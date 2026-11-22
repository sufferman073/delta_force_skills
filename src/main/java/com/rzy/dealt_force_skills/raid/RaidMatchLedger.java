package com.rzy.dealt_force_skills.raid;

import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.Vec3;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Strict per-match write-ahead record. A malformed record blocks allocation instead of losing assets. */
public final class RaidMatchLedger {
    public enum Phase { PREPARING, ACTIVE, ENDING, CLOSED }
    public enum Status { WAITING, ENTERING, ACTIVE, DEAD, EXTRACTED, FAILED, LOST, INTERRUPTED, CANCELLED }
    public static final class Participant {
        public final UUID id, team;
        public final String spawn;
        public String name, character = "";
        public Status status = Status.WAITING;
        public RaidInstanceData.ReturnPoint returnPoint;
        public Vec3 position = Vec3.ZERO;
        public float yaw, pitch;
        public boolean returned, assetApplied, observing, settled;
        public UUID body;
        public long downedUntil;
        public CompoundTag actual = new CompoundTag(), bodyState = new CompoundTag(), originalVitals = new CompoundTag();
        public Participant(UUID id, UUID team, String spawn, String name) {
            this.id = Objects.requireNonNull(id); this.team = Objects.requireNonNull(team);
            this.spawn = Objects.requireNonNull(spawn); this.name = Objects.requireNonNull(name);
        }
        public boolean eligible() { return status == Status.ACTIVE; }
        public boolean hasAssets() { return status != Status.WAITING && status != Status.CANCELLED; }
        public boolean terminal() { return status != Status.WAITING && status != Status.ENTERING && status != Status.ACTIVE; }
        CompoundTag save() {
            CompoundTag t = new CompoundTag(); t.putUUID("Id", id); t.putUUID("Team", team); t.putString("Spawn", spawn);
            t.putString("Name", name); t.putString("Character", character); t.putString("Status", status.name());
            if (returnPoint != null) t.put("Return", returnPoint.save());
            t.putDouble("X", position.x); t.putDouble("Y", position.y); t.putDouble("Z", position.z); t.putFloat("Yaw", yaw); t.putFloat("Pitch", pitch);
            t.putBoolean("Settled", settled); t.putBoolean("Returned", returned); t.putBoolean("AssetApplied", assetApplied); t.putBoolean("Observing", observing);
            if (body != null) t.putUUID("Body", body);
            t.putLong("DownedUntil", downedUntil); t.put("Actual", actual.copy()); t.put("BodyState", bodyState.copy());
            t.put("OriginalVitals", originalVitals.copy());
            return t;
        }
        static Participant load(CompoundTag t) {
            Participant p = new Participant(t.getUUID("Id"), t.getUUID("Team"), t.getString("Spawn"), t.getString("Name"));
            p.character = t.getString("Character"); p.status = Status.valueOf(t.getString("Status"));
            if (t.contains("Return")) p.returnPoint = RaidInstanceData.ReturnPoint.load(t.getCompound("Return"));
            p.position = new Vec3(t.getDouble("X"), t.getDouble("Y"), t.getDouble("Z"));
            if (!Double.isFinite(p.position.x) || !Double.isFinite(p.position.y) || !Double.isFinite(p.position.z))
                throw new IllegalArgumentException("Nonfinite participant location");
            p.yaw = t.getFloat("Yaw"); p.pitch = t.getFloat("Pitch"); p.returned = t.getBoolean("Returned");
            p.settled = t.getBoolean("Settled"); p.assetApplied = t.getBoolean("AssetApplied"); p.observing = t.getBoolean("Observing");
            p.body = t.hasUUID("Body") ? t.getUUID("Body") : null; p.downedUntil = t.getLong("DownedUntil");
            p.actual = t.getCompound("Actual").copy(); p.bodyState = t.getCompound("BodyState").copy();
            p.originalVitals = t.getCompound("OriginalVitals").copy();
            if (p.hasAssets() && p.returnPoint == null) throw new IllegalArgumentException("Missing participant return record");
            return p;
        }
    }
    public static final class Match {
        public final UUID id;
        public final String map;
        public Phase phase = Phase.PREPARING;
        public long ends;
        public final LinkedHashMap<UUID, Participant> players = new LinkedHashMap<>();
        public CompoundTag world = new CompoundTag(), ai = new CompoundTag();
        public final LinkedHashMap<String, Integer> aiSpawns = new LinkedHashMap<>();
        public boolean aiStarted;
        public String error = "";
        public Match(UUID id, String map) { this.id = Objects.requireNonNull(id); this.map = Objects.requireNonNull(map); }
        CompoundTag save() {
            CompoundTag tag = new CompoundTag(); tag.putInt("Schema", 1); tag.putUUID("Id", id); tag.putString("Map", map);
            tag.putString("Phase", phase.name()); tag.putLong("Ends", ends); tag.putBoolean("AiStarted", aiStarted);
            tag.putString("Error", error); tag.put("World", world.copy()); tag.put("Ai", ai.copy());
            ListTag list = new ListTag(); players.values().forEach(p -> list.add(p.save())); tag.put("Players", list);
            CompoundTag spawns = new CompoundTag(); aiSpawns.forEach(spawns::putInt); tag.put("AiSpawns", spawns);
            return tag;
        }
        static Match load(CompoundTag tag) {
            if (tag.getInt("Schema") != 1) throw new IllegalArgumentException("Unsupported raid match schema");
            Match m = new Match(tag.getUUID("Id"), tag.getString("Map")); m.phase = Phase.valueOf(tag.getString("Phase"));
            m.ends = tag.getLong("Ends"); m.aiStarted = tag.getBoolean("AiStarted"); m.error = tag.getString("Error");
            m.world = tag.getCompound("World").copy(); m.ai = tag.getCompound("Ai").copy();
            for (Tag raw : tag.getList("Players", Tag.TAG_COMPOUND)) {
                Participant p = Participant.load((CompoundTag) raw);
                if (m.players.putIfAbsent(p.id, p) != null) throw new IllegalArgumentException("Duplicate raid player");
            }
            CompoundTag spawns = tag.getCompound("AiSpawns");
            for (String spawn : spawns.getAllKeys()) {
                int count = spawns.getInt(spawn); if (count < 1 || count > 3) throw new IllegalArgumentException("Invalid AI team size");
                m.aiSpawns.put(spawn, count);
            }
            return m;
        }
    }
    private RaidMatchLedger() { }
    private static Path directory(MinecraftServer server) { return server.getWorldPath(LevelResource.ROOT).resolve("data/dfs_raid_matches"); }
    public static List<Match> load(MinecraftServer server) {
        Path directory = directory(server);
        if (!Files.exists(directory)) return List.of();
        List<Match> result = new ArrayList<>();
        try (var files = Files.list(directory)) {
            for (Path path : files.filter(p -> p.getFileName().toString().endsWith(".dat")).sorted().toList()) {
                Match match;
                try (InputStream input = Files.newInputStream(path)) { match = Match.load(NbtIo.readCompressed(input, NbtAccounter.unlimitedHeap())); }
                if (!path.getFileName().toString().equals(match.id + ".dat")) throw new IOException("Raid record/file identity mismatch: " + path);
                result.add(match);
            }
        } catch (IOException | RuntimeException failure) { throw new IllegalStateException("Cannot safely load raid match ledger", failure); }
        return result;
    }
    public static void save(MinecraftServer server, Match match) {
        if (!server.isSameThread()) throw new IllegalStateException("Raid ledger writes must run on server thread");
        Path directory = directory(server), temporary = null;
        try {
            Files.createDirectories(directory); temporary = Files.createTempFile(directory, "raid-", ".tmp");
            try (FileOutputStream output = new FileOutputStream(temporary.toFile())) {
                NbtIo.writeCompressed(match.save(), output);
                // NbtIo closes its gzip stream; open the finished file before forcing the durable bytes.
            }
            try (var channel = java.nio.channels.FileChannel.open(temporary, StandardOpenOption.WRITE)) { channel.force(true); }
            Path destination = directory.resolve(match.id + ".dat");
            try { Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException unsupported) { Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING); }
        } catch (IOException failure) { throw new IllegalStateException("Cannot persist raid match " + match.id, failure); }
        finally { if (temporary != null) try { Files.deleteIfExists(temporary); } catch (IOException ignored) { } }
    }
}
