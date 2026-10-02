package com.rzy.dealt_force_skills.shop;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.lexninjia.LexNinjiaStateManager;
import com.rzy.dealt_force_skills.character.saeed.SaeedStateManager;
import com.rzy.dealt_force_skills.character.undead.UndeadUpgradeManager;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Controls whether character shop purchase progress and special currencies are kept across
 * {@code /dealtmatch start} and player rejoin.
 *
 * <p>Default is {@code true} (keep forever). Server-wide default is stored in world SavedData;
 * per-player overrides take priority over the server default.
 */
public final class DealtKeepBoundsManager {
    private static final String DATA_NAME = DealtForceSkillsMod.MODID + "_keep_bounds";
    private static final String PLAYER_OVERRIDE_TAG = DealtForceSkillsMod.MODID + ".keep_bounds";

    private DealtKeepBoundsManager() {
    }

    public static boolean shouldKeep(ServerPlayer player) {
        if (player == null) {
            return true;
        }
        CompoundTag tag = player.getPersistentData();
        if (tag.contains(PLAYER_OVERRIDE_TAG)) {
            return tag.getBoolean(PLAYER_OVERRIDE_TAG);
        }
        MinecraftServer server = player.getServer();
        if (server == null) {
            return true;
        }
        return data(server).serverDefaultKeep;
    }

    public static boolean serverDefaultKeep(MinecraftServer server) {
        return data(server).serverDefaultKeep;
    }

    public static void setServerDefault(MinecraftServer server, boolean keep) {
        KeepBoundsData data = data(server);
        data.serverDefaultKeep = keep;
        data.setDirty();
    }

    public static void setPlayerOverride(ServerPlayer player, boolean keep) {
        player.getPersistentData().putBoolean(PLAYER_OVERRIDE_TAG, keep);
        MinecraftServer server = player.getServer();
        if (server != null) {
            KeepBoundsData data = data(server);
            data.playerOverrides.put(player.getUUID(), keep);
            data.setDirty();
        }
    }

    public static void clearIfNeeded(ServerPlayer player) {
        if (player == null || shouldKeep(player)) {
            return;
        }
        clearPurchaseAndCurrency(player);
    }

    public static void clearPurchaseAndCurrency(ServerPlayer player) {
        HaffCoinManager.set(player, 0L);
        UndeadSoulManager.set(player, 0L);
        LexNinjiaCurrencyManager.set(player, 0L);
        SaeedStateManager.setTacticalPoints(player, 0L);

        UndeadUpgradeManager.clearPurchaseProgress(player);
        LexNinjiaStateManager.clearPurchaseProgress(player);
        SaeedStateManager.clearPurchaseProgress(player);
    }

    public static void handlePlayerLoggedIn(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server != null) {
            KeepBoundsData data = data(server);
            Boolean override = data.playerOverrides.get(player.getUUID());
            if (override != null) {
                player.getPersistentData().putBoolean(PLAYER_OVERRIDE_TAG, override);
            }
        }
        clearIfNeeded(player);
    }

    private static KeepBoundsData data(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(KeepBoundsData::new, KeepBoundsData::load), DATA_NAME);
    }

    private static final class KeepBoundsData extends SavedData {
        private boolean serverDefaultKeep = true;
        private final LinkedHashMap<UUID, Boolean> playerOverrides = new LinkedHashMap<>();

        private static KeepBoundsData load(CompoundTag tag, HolderLookup.Provider provider) {
            KeepBoundsData data = new KeepBoundsData();
            data.serverDefaultKeep = !tag.contains("ServerDefaultKeep") || tag.getBoolean("ServerDefaultKeep");
            ListTag overrides = tag.getList("PlayerOverrides", Tag.TAG_COMPOUND);
            for (int i = 0; i < overrides.size(); i++) {
                CompoundTag entry = overrides.getCompound(i);
                if (entry.hasUUID("Player") && entry.contains("Keep")) {
                    data.playerOverrides.put(entry.getUUID("Player"), entry.getBoolean("Keep"));
                }
            }
            return data;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
            tag.putBoolean("ServerDefaultKeep", serverDefaultKeep);
            ListTag overrides = new ListTag();
            for (Map.Entry<UUID, Boolean> entry : playerOverrides.entrySet()) {
                CompoundTag entryTag = new CompoundTag();
                entryTag.putUUID("Player", entry.getKey());
                entryTag.putBoolean("Keep", entry.getValue());
                overrides.add(entryTag);
            }
            tag.put("PlayerOverrides", overrides);
            return tag;
        }
    }
}
