package com.rzy.dealt_force_skills.feedback;

import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.S2C_SkillFeedback;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/** Presentation hooks run after the gameplay code has chosen an affected target. */
public final class SkillFeedback {
    private static final Map<ServerPlayer, Map<String, Long>> LAST_NOTICE = new WeakHashMap<>();
    // Effect refreshes must not restart trap notices; entries expire with their source entity.
    private static final Map<Entity, Map<String, java.util.Set<java.util.UUID>>> TRIGGERED_NOTICES = new WeakHashMap<>();
    private static final Map<ServerPlayer, Map<Integer, Long>> OBSERVATIONS = new WeakHashMap<>();
    private SkillFeedback() {}

    public static void observe(ServerPlayer player, int observerId, int targetId) {
        if (player == null || !player.isAlive() || player.isSpectator()) return;
        var level = player.serverLevel();
        if (!(level.getEntity(targetId) instanceof ServerPlayer target) || target == player
                || !target.isAlive() || target.isSpectator()) return;
        long now = level.getGameTime();
        var recent = OBSERVATIONS.computeIfAbsent(player, ignored -> new HashMap<>());
        recent.entrySet().removeIf(e -> now - e.getValue() > 20);
        if (now - recent.getOrDefault(targetId, Long.MIN_VALUE / 2) < 10 || recent.size() >= 64) return;
        recent.put(targetId, now);
        if (observerId < 0) {
            double range = com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("client.client_events.mask1_lock_range", 64.0D);
            if (!com.rzy.dealt_force_skills.character.ghroth.GhrothStateManager.isGhroth(player)
                    || !(com.rzy.dealt_force_skills.character.ghroth.GhrothTaczEnhancement.isTaczGunStack(player.getMainHandItem())
                        || net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(player.getMainHandItem().getItem()).getNamespace().equals("superbwarfare"))
                    || !player.isShiftKeyDown()
                    || player.distanceToSqr(target) > range * range) return;
            // The existing lock chooses the nearest player and supports TaCZ and Superb Warfare.
            if (level.players().stream().anyMatch(other -> other != player && other != target && other.isAlive()
                    && !other.isSpectator() && player.distanceToSqr(other) < player.distanceToSqr(target))) return;
            notice(target, "locked"); passive(player, 1);
        } else if (level.getEntity(observerId) instanceof com.rzy.dealt_force_skills.entity.SaeedGuardEntity guard
                && guard.isAlive() && guard.isOwnedBy(player.getUUID())
                && com.rzy.dealt_force_skills.character.saeed.SaeedStateManager.isSaeed(player)
                && guard.distanceToSqr(target) <= 160 * 160 && guard.hasLineOfSight(target)) {
            var direction = target.getEyePosition().subtract(guard.getEyePosition()).normalize();
            if (direction.dot(player.getLookAngle()) >= .5D) notice(target, "guard");
        }
    }

    public static void notice(Entity target, String id) {
        notice(target, id, "", "");
    }

    public static void notice(Entity target, String id, String character, String icon) {
        if (!(target instanceof ServerPlayer player) || player.isSpectator()) return;
        SkillNotice notice = SkillNotice.find(id);
        if (notice == null) throw new IllegalArgumentException("Unknown skill notice: " + id);
        long now = player.serverLevel().getGameTime();
        var last = LAST_NOTICE.computeIfAbsent(player, ignored -> new HashMap<>());
        String token = id + character;
        if (now - last.getOrDefault(token, Long.MIN_VALUE / 2) < notice.interval()) return;
        last.put(token, now);
        CompoundTag tag = new CompoundTag();
        tag.putString("Kind", "notice");
        tag.putString("Notice", id);
        tag.putString("Character", character.isEmpty() ? notice.character() : character);
        tag.putString("Icon", icon.isEmpty() ? notice.icon() : icon);
        send(player, tag);
    }

    public static void countdown(Entity target, Entity source, String id, int remaining, int total) {
        timedNotice(target, source, id, remaining, total, remaining);
    }

    public static void phaseNotice(Entity target, Entity source, String id, int remaining, int total) {
        timedNotice(target, source, id, remaining, total, 14);
    }

    private static void timedNotice(Entity target, Entity source, String id, int remaining, int total, int ttl) {
        if (!(target instanceof ServerPlayer player) || !player.isAlive() || player.isSpectator() || remaining <= 0) return;
        SkillNotice spec = SkillNotice.find(id);
        if (spec == null) throw new IllegalArgumentException("Unknown skill notice: " + id);
        long now = player.serverLevel().getGameTime();
        String token = "timed:" + id + (source == null ? "" : source.getUUID());
        var recent = LAST_NOTICE.computeIfAbsent(player, ignored -> new HashMap<>());
        if (now - recent.getOrDefault(token, Long.MIN_VALUE / 2) < 2) return;
        recent.put(token, now);
        CompoundTag tag = new CompoundTag(); tag.putString("Kind", "notice"); tag.putString("Notice", id);
        tag.putString("Character", spec.character()); tag.putString("Icon", spec.icon());
        if (source != null) tag.putString("NoticeSource", source.getUUID().toString());
        tag.putInt("Remaining", remaining); tag.putInt("Total", Math.max(remaining, total)); tag.putInt("NoticeTtl", Math.max(1, ttl));
        send(player, tag);
    }

    /** Call only for visible hazards; traps must use triggered() at their actual effect site. */
    public static void hostileThreat(Entity source, String icon, double radius) {
        threat(source, icon, radius, radius + 3, player -> {
            Entity owner = source instanceof net.minecraft.world.entity.projectile.Projectile projectile ? projectile.getOwner() : source;
            return com.rzy.dealt_force_skills.util.TargetingUtil.isHostileLivingFor(owner, player);
        });
    }

    public static void threat(Entity source, String icon, double radius) {
        threat(source, icon, radius, radius + 3.0D);
    }

    public static void threat(Entity source, String icon, double radius, double warning) {
        threat(source, icon, radius, warning, player -> {
            Entity owner = source instanceof net.minecraft.world.entity.projectile.Projectile projectile ? projectile.getOwner() : source;
            return com.rzy.dealt_force_skills.util.TargetingUtil.isSelfOrHostileLivingFor(owner, player);
        });
    }

    public static void threat(Entity source, String icon, double radius, double warning,
                              java.util.function.Predicate<ServerPlayer> affected) {
        if (!(source.level() instanceof ServerLevel level) || source.isRemoved() || source.tickCount % 4 != 0) return;
        radius = Math.max(0, radius);
        warning = Math.max(radius, warning);
        for (ServerPlayer player : level.players()) {
            if (!player.isAlive() || player.isSpectator() || !affected.test(player)) continue;
            double distance = player.position().distanceTo(source.position());
            if (distance > warning) continue;
            threatTo(player, source, icon, radius, warning, false, 10);
            if (icon.equals("uluru.skill.loitering_missile")) notice(player, "missile");
            if (icon.equals("catdad.skill.highway")) notice(player, "truck");
        }
    }

    public static void triggered(LivingEntity target, Entity source, String icon, String notice) {
        if (!(target instanceof ServerPlayer player)) return;
        threatTo(player, source, icon, 0, 0, true, 50);
        if (!notice.isEmpty()) {
            var notified = TRIGGERED_NOTICES.computeIfAbsent(source, ignored -> new HashMap<>())
                    .computeIfAbsent(notice, ignored -> new java.util.HashSet<>());
            if (notified.add(player.getUUID())) notice(player, notice);
        }
    }

    /** Block traps have no tracked entity; keep a stable position identity and send only on contact. */
    public static void triggered(ServerPlayer player, net.minecraft.core.BlockPos position, String icon) {
        CompoundTag tag = new CompoundTag();
        tag.putString("Kind", "threat");
        String identity = player.level().dimension().location() + ":" + position.asLong();
        tag.putUUID("Source", java.util.UUID.nameUUIDFromBytes(identity.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        tag.putInt("Entity", -1); tag.putString("Icon", icon);
        tag.putDouble("X", position.getX() + .5); tag.putDouble("Y", position.getY() + .5); tag.putDouble("Z", position.getZ() + .5);
        tag.putBoolean("Triggered", true); tag.putInt("Ticks", 24);
        send(player, tag);
    }

    /** Directional or non-spherical hazards decide their geometry in the owning entity. */
    public static void shaped(ServerPlayer player, Entity source, String icon, boolean danger) {
        shaped(player, source, icon, danger, 6);
    }

    public static void shaped(ServerPlayer player, Entity source, String icon, boolean danger, int ticks) {
        if (!player.isAlive() || player.isSpectator()) return;
        threatTo(player, source, icon, danger ? Double.MAX_VALUE : 0, Double.MAX_VALUE, false, ticks);
    }

    private static void threatTo(ServerPlayer player, Entity source, String icon, double radius, double warning,
                                 boolean triggered, int ticks) {
        CompoundTag tag = new CompoundTag();
        tag.putString("Kind", "threat");
        tag.putUUID("Source", source.getUUID());
        tag.putInt("Entity", source.getId());
        tag.putString("Icon", icon);
        tag.putDouble("X", source.getX()); tag.putDouble("Y", source.getY()); tag.putDouble("Z", source.getZ());
        tag.putDouble("Radius", radius); tag.putDouble("Warning", warning);
        tag.putBoolean("Triggered", triggered); tag.putInt("Ticks", ticks);
        send(player, tag);
    }

    public static void active(Entity owner, String icon, int remaining) {
        if (!(owner instanceof ServerPlayer player) || player.tickCount % 4 != 0) return;
        CompoundTag tag = new CompoundTag(); tag.putString("Kind", "active");
        tag.putString("Icon", icon); tag.putInt("Remaining", Math.max(0, remaining));
        send(player, tag);
    }

    public static void talents(ServerPlayer player, int[] durations, int activeMask) {
        CompoundTag tag = new CompoundTag(); tag.putString("Kind", "talents");
        tag.putIntArray("Durations", durations); tag.putInt("ActiveMask", activeMask); send(player, tag);
    }

    public static void passive(ServerPlayer player, int state) {
        CompoundTag tag = new CompoundTag(); tag.putString("Kind", "passive"); tag.putInt("State", state);
        tag.putString("Character", com.rzy.dealt_force_skills.character.CharacterSelectionManager.getSelectedCharacterId(player).orElse(""));
        send(player, tag);
    }

    public static void availability(ServerPlayer player, String prefix, int mask) {
        CompoundTag tag = new CompoundTag(); tag.putString("Kind", "availability");
        tag.putString("Prefix", prefix); tag.putInt("Mask", mask); send(player, tag);
    }

    private static void send(ServerPlayer player, CompoundTag data) {
        data.putString("Dimension", player.level().dimension().location().toString());
        NetworkHandler.sendToPlayer(new S2C_SkillFeedback(data), player);
    }
}
