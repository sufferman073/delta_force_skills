package com.rzy.dealt_force_skills.skill;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.SkillSlot;
import com.rzy.dealt_force_skills.registry.ModSounds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class SkillSoundFeedback {
    private static final String DATA_KEY = "DealtForceSkillSoundFeedback";
    private static final String CHARGE_SNAPSHOT_KEY = "__ChargeSnapshot";
    private static final String KEY_SEPARATOR = "|";
    private static final Map<UUID, Map<String, Long>> ATTEMPT_COOLDOWNS = new HashMap<>();
    private static final Map<UUID, Boolean> COOLDOWN_DENIALS = new HashMap<>();

    private SkillSoundFeedback() {
    }

    public static void beginAttempt(ServerPlayer player) {
        COOLDOWN_DENIALS.remove(player.getUUID());
        ATTEMPT_COOLDOWNS.put(player.getUUID(), collectCooldownDeadlines(player));
    }

    public static void clear(ServerPlayer player) {
        if (player == null) {
            return;
        }
        UUID playerId = player.getUUID();
        ATTEMPT_COOLDOWNS.remove(playerId);
        COOLDOWN_DENIALS.remove(playerId);
    }

    public static void clearAllRuntimeCaches() {
        ATTEMPT_COOLDOWNS.clear();
        COOLDOWN_DENIALS.clear();
    }

    public static void cooldown(ServerPlayer player, Component message) {
        COOLDOWN_DENIALS.put(player.getUUID(), Boolean.TRUE);
        player.displayClientMessage(message, true);
        player.playNotifySound(ModSounds.SKILLS_CD.get(), SoundSource.PLAYERS, 0.7F, 1.0F);
    }

    public static void onSkillResult(ServerPlayer player, String characterId, SkillSlot slot, boolean handled) {
        CompoundTag data = data(player);
        String key = key(characterId, slot);
        long now = player.level().getGameTime();
        Map<String, Long> before = ATTEMPT_COOLDOWNS.remove(player.getUUID());
        Map<String, Long> after = collectCooldownDeadlines(player);
        long changedReadyAt = changedCooldownDeadline(before, after, now, characterId);
        long trackedReadyAt = data.getLong(key);
        boolean cooldownDenied = COOLDOWN_DENIALS.remove(player.getUUID()) != null;
        if (cooldownDenied) {
            long readyAt = trackedReadyAt;
            if (readyAt <= now) {
                readyAt = Math.max(
                        latestRelevantCooldownDeadline(before, characterId, now),
                        latestRelevantCooldownDeadline(after, characterId, now));
            }
            if (readyAt > now) {
                data.putLong(key, readyAt);
            } else {
                data.remove(key);
            }
            return;
        }
        if (!handled) {
            return;
        }

        long readyAt = changedReadyAt;
        if (readyAt > now) {
            data.putLong(key, readyAt);
        } else {
            data.remove(key);
        }
    }

    public static void tick(ServerPlayer player, String selectedCharacterId) {
        if (player.isSpectator()) {
            return;
        }
        CompoundTag data = data(player);
        long now = player.level().getGameTime();
        String selectedPrefix = selectedCharacterId + KEY_SEPARATOR;
        boolean selectedSkillReady = false;
        for (String key : new ArrayList<>(data.getAllKeys())) {
            if (CHARGE_SNAPSHOT_KEY.equals(key)) {
                continue;
            }
            long readyAt = data.getLong(key);
            if (readyAt <= 0L || now < readyAt) {
                continue;
            }
            selectedSkillReady |= key.startsWith(selectedPrefix);
            data.remove(key);
        }
        boolean chargeRestored = updateChargeSnapshot(player, data, selectedCharacterId);
        if (selectedSkillReady || chargeRestored) {
            player.playNotifySound(ModSounds.SKILLS_READY.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
        }
    }

    private static CompoundTag data(ServerPlayer player) {
        CompoundTag persistentData = player.getPersistentData();
        if (!persistentData.contains(DATA_KEY, Tag.TAG_COMPOUND)) {
            persistentData.put(DATA_KEY, new CompoundTag());
        }
        return persistentData.getCompound(DATA_KEY);
    }

    private static String key(String characterId, SkillSlot slot) {
        return characterId + KEY_SEPARATOR + slot.name();
    }

    private static long changedCooldownDeadline(
            Map<String, Long> before,
            Map<String, Long> after,
            long now,
            String characterId) {
        long latest = 0L;
        for (Map.Entry<String, Long> entry : after.entrySet()) {
            if (!isRelevantCooldownPath(entry.getKey(), characterId)) {
                continue;
            }
            long previous = before == null ? 0L : before.getOrDefault(entry.getKey(), 0L);
            if (entry.getValue() > now && entry.getValue() > previous) {
                latest = Math.max(latest, entry.getValue());
            }
        }
        return latest;
    }

    private static long latestRelevantCooldownDeadline(
            Map<String, Long> deadlines,
            String characterId,
            long now) {
        if (deadlines == null || deadlines.isEmpty()) {
            return 0L;
        }
        long latest = 0L;
        for (Map.Entry<String, Long> entry : deadlines.entrySet()) {
            if (isRelevantCooldownPath(entry.getKey(), characterId) && entry.getValue() > now) {
                latest = Math.max(latest, entry.getValue());
            }
        }
        return latest;
    }

    private static boolean isRelevantCooldownPath(String path, String characterId) {
        return path.startsWith(DealtForceSkillsMod.MODID + "." + stateId(characterId) + "/");
    }

    private static String stateId(String characterId) {
        int separator = Math.max(characterId.lastIndexOf('/'), characterId.lastIndexOf(':'));
        return separator >= 0 ? characterId.substring(separator + 1) : characterId;
    }

    private static Map<String, Long> collectCooldownDeadlines(ServerPlayer player) {
        Map<String, Long> deadlines = new HashMap<>();
        collectCooldownDeadlines(player.getPersistentData(), "", player.level().getGameTime(), deadlines);
        return deadlines;
    }

    private static void collectCooldownDeadlines(
            CompoundTag compound,
            String path,
            long now,
            Map<String, Long> deadlines) {
        for (String key : compound.getAllKeys()) {
            Tag value = compound.get(key);
            String childPath = path.isEmpty() ? key : path + "/" + key;
            if (value instanceof CompoundTag child) {
                collectCooldownDeadlines(child, childPath, now, deadlines);
                continue;
            }
            String normalized = key.toLowerCase(Locale.ROOT);
            if (!(value instanceof NumericTag number)
                    || (!normalized.contains("cooldown") && !normalized.contains("recharge"))) {
                continue;
            }
            long raw = number.getAsLong();
            if (raw <= 0L) {
                continue;
            }
            boolean absolute = normalized.contains("until") || normalized.contains("next");
            deadlines.put(childPath, absolute ? raw : now + raw);
        }
    }

    private static boolean updateChargeSnapshot(ServerPlayer player, CompoundTag data, String selectedCharacterId) {
        CompoundTag current = new CompoundTag();
        collectChargeCounts(player.getPersistentData(), "", selectedCharacterId, current);
        CompoundTag previous = data.contains(CHARGE_SNAPSHOT_KEY, Tag.TAG_COMPOUND)
                ? data.getCompound(CHARGE_SNAPSHOT_KEY)
                : null;
        boolean restored = false;
        if (previous != null) {
            for (String key : current.getAllKeys()) {
                if (previous.contains(key, Tag.TAG_ANY_NUMERIC) && current.getInt(key) > previous.getInt(key)) {
                    restored = true;
                    break;
                }
            }
        }
        data.put(CHARGE_SNAPSHOT_KEY, current);
        return restored;
    }

    private static void collectChargeCounts(
            CompoundTag compound,
            String path,
            String characterId,
            CompoundTag output) {
        for (String key : compound.getAllKeys()) {
            Tag value = compound.get(key);
            String childPath = path.isEmpty() ? key : path + "/" + key;
            if (value instanceof CompoundTag child) {
                collectChargeCounts(child, childPath, characterId, output);
                continue;
            }
            String normalized = key.toLowerCase(Locale.ROOT);
            if (!(value instanceof NumericTag number)
                    || (!normalized.endsWith("charges") && !normalized.endsWith("ammo"))
                    || !isRelevantCooldownPath(childPath, characterId)) {
                continue;
            }
            output.putInt(childPath, number.getAsInt());
        }
    }
}
