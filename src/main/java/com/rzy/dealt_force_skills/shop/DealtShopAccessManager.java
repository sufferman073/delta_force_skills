package com.rzy.dealt_force_skills.shop;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class DealtShopAccessManager {
    private static final String DATA_NAME = DealtForceSkillsMod.MODID + "_shop_access";
    private static final String PLAYER_TAG_PREFIX = DealtForceSkillsMod.MODID + ".access.";

    public enum Service {
        SHOP("shop", false),
        BASE("base", true),
        BACKTP("backtp", true);

        private final String id;
        private final boolean defaultEnabled;

        Service(String id, boolean defaultEnabled) {
            this.id = id;
            this.defaultEnabled = defaultEnabled;
        }

        public String id() {
            return id;
        }

        public boolean defaultEnabled() {
            return defaultEnabled;
        }
    }

    private DealtShopAccessManager() {
    }

    public static boolean isAllowed(Service service, ServerPlayer player) {
        if (player == null || player.getServer() == null) {
            return service.defaultEnabled();
        }
        AccessData data = data(player.getServer());
        Boolean playerOverride = data.playerOverrides.get(service).get(player.getUUID());
        if (playerOverride != null) {
            return playerOverride;
        }
        String dimension = player.serverLevel().dimension().location().toString();
        BlockPos pos = player.blockPosition();
        List<AreaRule> rules = data.areas.get(service);
        for (int i = rules.size() - 1; i >= 0; i--) {
            AreaRule rule = rules.get(i);
            if (rule.contains(dimension, pos)) {
                return rule.enabled;
            }
        }
        return data.serverDefaults.get(service);
    }

    public static void setServerDefault(MinecraftServer server, Service service, boolean enabled) {
        AccessData data = data(server);
        data.serverDefaults.put(service, enabled);
        data.setDirty();
    }

    public static void setPlayerOverride(ServerPlayer player, Service service, boolean enabled) {
        if (player.getServer() == null) {
            return;
        }
        AccessData data = data(player.getServer());
        data.playerOverrides.get(service).put(player.getUUID(), enabled);
        player.getPersistentData().putBoolean(PLAYER_TAG_PREFIX + service.id(), enabled);
        data.setDirty();
    }

    public static void addAreaRule(MinecraftServer server, Service service, ResourceLocation dimension,
                                   BlockPos first, BlockPos second, boolean enabled) {
        AccessData data = data(server);
        data.areas.get(service).add(new AreaRule(
                dimension.toString(),
                Math.min(first.getX(), second.getX()), Math.min(first.getY(), second.getY()), Math.min(first.getZ(), second.getZ()),
                Math.max(first.getX(), second.getX()), Math.max(first.getY(), second.getY()), Math.max(first.getZ(), second.getZ()),
                enabled));
        data.setDirty();
    }

    public static void handlePlayerLogin(ServerPlayer player) {
        if (player.getServer() == null) {
            return;
        }
        AccessData data = data(player.getServer());
        for (Service service : Service.values()) {
            Boolean override = data.playerOverrides.get(service).get(player.getUUID());
            if (override != null) {
                player.getPersistentData().putBoolean(PLAYER_TAG_PREFIX + service.id(), override);
            }
        }
    }

    private static AccessData data(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(AccessData::new, AccessData::load), DATA_NAME);
    }

    private record AreaRule(String dimension, int minX, int minY, int minZ,
                            int maxX, int maxY, int maxZ, boolean enabled) {
        private boolean contains(String currentDimension, BlockPos pos) {
            return dimension.equals(currentDimension)
                    && pos.getX() >= minX && pos.getX() <= maxX
                    && pos.getY() >= minY && pos.getY() <= maxY
                    && pos.getZ() >= minZ && pos.getZ() <= maxZ;
        }

        private CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putString("Dimension", dimension);
            tag.putInt("MinX", minX);
            tag.putInt("MinY", minY);
            tag.putInt("MinZ", minZ);
            tag.putInt("MaxX", maxX);
            tag.putInt("MaxY", maxY);
            tag.putInt("MaxZ", maxZ);
            tag.putBoolean("Enabled", enabled);
            return tag;
        }

        private static AreaRule load(CompoundTag tag) {
            return new AreaRule(tag.getString("Dimension"), tag.getInt("MinX"), tag.getInt("MinY"), tag.getInt("MinZ"),
                    tag.getInt("MaxX"), tag.getInt("MaxY"), tag.getInt("MaxZ"), tag.getBoolean("Enabled"));
        }
    }

    private static final class AccessData extends SavedData {
        private final EnumMap<Service, Boolean> serverDefaults = new EnumMap<>(Service.class);
        private final EnumMap<Service, LinkedHashMap<UUID, Boolean>> playerOverrides = new EnumMap<>(Service.class);
        private final EnumMap<Service, List<AreaRule>> areas = new EnumMap<>(Service.class);

        private AccessData() {
            for (Service service : Service.values()) {
                serverDefaults.put(service, service.defaultEnabled());
                playerOverrides.put(service, new LinkedHashMap<>());
                areas.put(service, new ArrayList<>());
            }
        }

        private static AccessData load(CompoundTag tag, HolderLookup.Provider provider) {
            AccessData data = new AccessData();
            for (Service service : Service.values()) {
                String prefix = service.id();
                if (tag.contains(prefix + "Server")) {
                    data.serverDefaults.put(service, tag.getBoolean(prefix + "Server"));
                }
                ListTag players = tag.getList(prefix + "Players", Tag.TAG_COMPOUND);
                for (int i = 0; i < players.size(); i++) {
                    CompoundTag entry = players.getCompound(i);
                    if (entry.hasUUID("Player")) {
                        data.playerOverrides.get(service).put(entry.getUUID("Player"), entry.getBoolean("Enabled"));
                    }
                }
                ListTag regions = tag.getList(prefix + "Areas", Tag.TAG_COMPOUND);
                for (int i = 0; i < regions.size(); i++) {
                    data.areas.get(service).add(AreaRule.load(regions.getCompound(i)));
                }
            }
            return data;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
            for (Service service : Service.values()) {
                String prefix = service.id();
                tag.putBoolean(prefix + "Server", serverDefaults.get(service));
                ListTag players = new ListTag();
                for (Map.Entry<UUID, Boolean> entry : playerOverrides.get(service).entrySet()) {
                    CompoundTag playerTag = new CompoundTag();
                    playerTag.putUUID("Player", entry.getKey());
                    playerTag.putBoolean("Enabled", entry.getValue());
                    players.add(playerTag);
                }
                tag.put(prefix + "Players", players);
                ListTag regions = new ListTag();
                for (AreaRule rule : areas.get(service)) {
                    regions.add(rule.save());
                }
                tag.put(prefix + "Areas", regions);
            }
            return tag;
        }
    }
}
