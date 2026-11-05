package com.rzy.dealt_force_skills.character;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.LinkedHashSet;
import java.util.Set;

public final class CharacterBanManager {
    private static final String PLAYER_BANS_TAG = DealtForceSkillsMod.MODID + ".banned_characters";
    private static final String PLAYER_ALLOWS_TAG = DealtForceSkillsMod.MODID + ".allowed_characters";
    private static final String DATA_NAME = DealtForceSkillsMod.MODID + "_character_bans";
    private static final String SERVER_BANS_TAG = "ServerBans";
    private static final String SERVER_ALLOWS_TAG = "ServerAllows";

    private CharacterBanManager() {
    }

    public static boolean banPlayer(ServerPlayer player, String characterId) {
        CompoundTag data = player.getPersistentData();
        ListTag bans = data.contains(PLAYER_BANS_TAG, Tag.TAG_LIST)
                ? data.getList(PLAYER_BANS_TAG, Tag.TAG_STRING)
                : new ListTag();
        boolean allowRemoved = removeFromList(data, PLAYER_ALLOWS_TAG, characterId);
        if (contains(bans, characterId)) {
            return allowRemoved;
        }
        bans.add(StringTag.valueOf(characterId));
        data.put(PLAYER_BANS_TAG, bans);
        return true;
    }

    public static boolean unbanPlayer(ServerPlayer player, String characterId) {
        CompoundTag data = player.getPersistentData();
        boolean banRemoved = removeFromList(data, PLAYER_BANS_TAG, characterId);
        ListTag allows = data.contains(PLAYER_ALLOWS_TAG, Tag.TAG_LIST)
                ? data.getList(PLAYER_ALLOWS_TAG, Tag.TAG_STRING)
                : new ListTag();
        if (contains(allows, characterId)) {
            return banRemoved;
        }
        allows.add(StringTag.valueOf(characterId));
        data.put(PLAYER_ALLOWS_TAG, allows);
        return true;
    }

    public static boolean isPlayerAllowed(ServerPlayer player, String characterId) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(PLAYER_ALLOWS_TAG, Tag.TAG_LIST)) {
            return false;
        }
        return contains(data.getList(PLAYER_ALLOWS_TAG, Tag.TAG_STRING), characterId);
    }

    public static boolean isPlayerBanned(ServerPlayer player, String characterId) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(PLAYER_BANS_TAG, Tag.TAG_LIST)) {
            return false;
        }
        return contains(data.getList(PLAYER_BANS_TAG, Tag.TAG_STRING), characterId);
    }

    public static boolean banServer(MinecraftServer server, String characterId) {
        ServerBanData data = serverData(server);
        boolean changed = data.bannedCharacters.add(characterId);
        changed |= data.allowedCharacters.remove(characterId);
        if (changed) {
            data.setDirty();
        }
        return changed;
    }

    public static boolean unbanServer(MinecraftServer server, String characterId) {
        ServerBanData data = serverData(server);
        boolean changed = data.bannedCharacters.remove(characterId);
        changed |= data.allowedCharacters.add(characterId);
        if (changed) {
            data.setDirty();
        }
        return changed;
    }

    public static boolean isServerBanned(MinecraftServer server, String characterId) {
        return serverData(server).bannedCharacters.contains(characterId);
    }

    public static boolean isServerAllowed(MinecraftServer server, String characterId) {
        return server != null && serverData(server).allowedCharacters.contains(characterId);
    }

    public static void copy(net.minecraft.world.entity.player.Player from, net.minecraft.world.entity.player.Player to) {
        for (String key : java.util.List.of(PLAYER_BANS_TAG, PLAYER_ALLOWS_TAG)) {
            if (from.getPersistentData().contains(key, Tag.TAG_LIST))
                to.getPersistentData().put(key, from.getPersistentData().getList(key, Tag.TAG_STRING).copy());
        }
    }

    private static boolean contains(ListTag list, String characterId) {
        for (int i = 0; i < list.size(); i++) {
            if (characterId.equals(list.getString(i))) {
                return true;
            }
        }
        return false;
    }

    private static boolean removeFromList(CompoundTag data, String tagName, String characterId) {
        if (!data.contains(tagName, Tag.TAG_LIST)) {
            return false;
        }
        ListTag values = data.getList(tagName, Tag.TAG_STRING);
        ListTag kept = new ListTag();
        boolean changed = false;
        for (int i = 0; i < values.size(); i++) {
            String value = values.getString(i);
            if (characterId.equals(value)) {
                changed = true;
            } else {
                kept.add(StringTag.valueOf(value));
            }
        }
        if (!changed) {
            return false;
        }
        if (kept.isEmpty()) {
            data.remove(tagName);
        } else {
            data.put(tagName, kept);
        }
        return true;
    }

    private static ServerBanData serverData(MinecraftServer server) {
        return server.overworld().getDataStorage()
                .computeIfAbsent(ServerBanData::load, ServerBanData::new, DATA_NAME);
    }

    private static final class ServerBanData extends SavedData {
        private final Set<String> bannedCharacters = new LinkedHashSet<>();
        private final Set<String> allowedCharacters = new LinkedHashSet<>();

        private static ServerBanData load(CompoundTag tag) {
            ServerBanData data = new ServerBanData();
            ListTag bans = tag.getList(SERVER_BANS_TAG, Tag.TAG_STRING);
            for (int i = 0; i < bans.size(); i++) {
                data.bannedCharacters.add(bans.getString(i));
            }
            ListTag allows = tag.getList(SERVER_ALLOWS_TAG, Tag.TAG_STRING);
            for (int i = 0; i < allows.size(); i++) data.allowedCharacters.add(allows.getString(i));
            return data;
        }

        @Override
        public CompoundTag save(CompoundTag tag) {
            ListTag bans = new ListTag();
            dataCopy().forEach(characterId -> bans.add(StringTag.valueOf(characterId)));
            tag.put(SERVER_BANS_TAG, bans);
            ListTag allows = new ListTag();
            allowedCharacters.forEach(id -> allows.add(StringTag.valueOf(id)));
            tag.put(SERVER_ALLOWS_TAG, allows);
            return tag;
        }

        private Set<String> dataCopy() {
            return new LinkedHashSet<>(bannedCharacters);
        }
    }
}
