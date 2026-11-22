package com.rzy.dealt_force_skills.raid.assets;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

/** Write-ahead asset journal. Failed reads/writes never become an empty account. */
final class RaidAssetJournal {
    private static final String SAVE_RECEIPT = "dfsRaidAssetSaveReceipt";
    private RaidAssetJournal() { }
    static CompoundTag read(MinecraftServer server, UUID player) {
        requireThread(server);
        Path file = file(server, player);
        if (!Files.exists(file)) { CompoundTag fresh = new CompoundTag(); fresh.putInt("Schema", 1); return fresh; }
        try (var stream = Files.newInputStream(file)) {
            CompoundTag root = NbtIo.readCompressed(stream);
            if (root.getInt("Schema") != 1) throw new IOException("Unsupported raid asset journal schema");
            return root;
        } catch (IOException e) { throw new IllegalStateException("Cannot read raid assets for " + player, e); }
    }
    static void write(MinecraftServer server, UUID player, CompoundTag root) {
        requireThread(server);
        Path file = file(server, player);
        Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
        try {
            Files.createDirectories(file.getParent());
            try (var stream = Files.newOutputStream(temporary)) { NbtIo.writeCompressed(root, stream); }
            try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE)) { channel.force(true); }
            Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) { throw new IllegalStateException("Cannot persist raid assets for " + player, e); }
    }
    /** Vanilla's save method logs IO failures. Verify our receipt in the actual saved player file. */
    static void savePlayer(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        UUID receipt = UUID.randomUUID();
        player.getPersistentData().putUUID(SAVE_RECEIPT, receipt);
        server.getPlayerList().saveAll();
        Path file = server.getWorldPath(LevelResource.PLAYER_DATA_DIR).resolve(player.getUUID() + ".dat");
        try (var stream = Files.newInputStream(file)) {
            if (!hasReceipt(NbtIo.readCompressed(stream), receipt)) throw new IOException("Player save receipt not persisted");
        } catch (IOException e) { throw new IllegalStateException("Cannot confirm saved raid player " + player.getUUID(), e); }
    }
    private static boolean hasReceipt(CompoundTag tag, UUID receipt) {
        if (tag.hasUUID(SAVE_RECEIPT) && tag.getUUID(SAVE_RECEIPT).equals(receipt)) return true;
        for (String key : tag.getAllKeys()) if (tag.get(key) instanceof CompoundTag child && hasReceipt(child, receipt)) return true;
        return false;
    }
    private static Path file(MinecraftServer server, UUID player) {
        return server.getWorldPath(LevelResource.ROOT).resolve("data/dfs_raid_assets").resolve(player + ".dat");
    }
    static void requireThread(MinecraftServer server) {
        if (!server.isSameThread()) throw new IllegalStateException("Raid assets must run on the server thread");
    }
}
