package com.rzy.dealt_force_skills.character.sineva;

import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.S2C_SinevaKnockdown;
import com.rzy.dealt_force_skills.registry.ModEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

/** A horizontal body, with the same geometry on the server and observing clients. */
public final class SinevaKnockdownState {
    private static volatile double KNOCKBACK_HORIZONTAL = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("KNOCKBACK_HORIZONTAL", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("characters.sineva.sineva_knockdown_state.knockback_horizontal", 0.85));
    private static volatile double KNOCKBACK_VERTICAL = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("KNOCKBACK_VERTICAL", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("characters.sineva.sineva_knockdown_state.knockback_vertical", 0.18));
    // Entity IDs are shared by the integrated server/client; use instance identity.
    private static final Map<LivingEntity, KnockdownData> ACTIVE =
            Collections.synchronizedMap(new IdentityHashMap<>());
    private SinevaKnockdownState() { }

    public static void apply(LivingEntity attacker, LivingEntity target, int ticks) {
        if (target.level().isClientSide || ticks <= 0 || !target.isAlive()
                || target.hasEffect(ModEffects.SHAKEHANDS)) return;
        float yaw = yawToward(attacker, target);
        begin(target, ticks, yaw);
        // Keep the existing player knockback. Mob knockback belongs to the calling skill.
        if (target instanceof ServerPlayer) {
            Vec3 away = horizontalAway(attacker, target);
            target.setDeltaMovement(away.x * KNOCKBACK_HORIZONTAL,
                    Math.max(target.getDeltaMovement().y, KNOCKBACK_VERTICAL), away.z * KNOCKBACK_HORIZONTAL);
            target.hurtMarked = true;
        }
        NetworkHandler.sendToTrackingAndSelf(new S2C_SinevaKnockdown(target.getId(), ticks, yaw), target);
    }

    public static void begin(LivingEntity target, int ticks, float yaw) {
        if (ticks <= 0 || !Float.isFinite(yaw)) { clear(target); return; }
        KnockdownData previous = ACTIVE.get(target);
        EntityDimensions standing = target.getDimensions(Pose.STANDING);
        float width = previous == null ? standing.width() : previous.width;
        float length = previous == null ? standing.height() : previous.length;
        Pose restore = previous == null && target instanceof Player player ? player.getForcedPose()
                : previous == null ? null : previous.restorePose;
        ACTIVE.put(target, new KnockdownData(target.level(), target.level().getGameTime(), ticks,
                yaw, width, length, restore));
        if (target instanceof Player player) player.setForcedPose(Pose.STANDING);
        target.setPose(Pose.STANDING);
        target.refreshDimensions();
        lockYaw(target, yaw);
    }

    public static boolean active(LivingEntity entity) { return ACTIVE.containsKey(entity); }
    public static float yaw(LivingEntity entity) { return ACTIVE.get(entity).yaw; }
    public static float bodyWidth(LivingEntity entity) { return ACTIVE.get(entity).width; }
    public static float bodyLength(LivingEntity entity) { return ACTIVE.get(entity).length; }

    public static EntityDimensions dimensions(LivingEntity entity) {
        KnockdownData data = ACTIVE.get(entity);
        return data == null ? null : EntityDimensions.scalable(data.width, data.width).withEyeHeight(data.width * .8F);
    }

    /** Vanilla attacks and projectile ray casts use this actual collision box. */
    public static AABB boundingBox(LivingEntity entity) {
        KnockdownData data = ACTIVE.get(entity);
        if (data == null) return null;
        double a = Math.toRadians(data.yaw), c = Math.abs(Math.cos(a)), s = Math.abs(Math.sin(a));
        double x = (data.width * c + data.length * s) * .5;
        double z = (data.width * s + data.length * c) * .5;
        Vec3 p = entity.position();
        return new AABB(p.x-x, p.y, p.z-z, p.x+x, p.y+data.width, p.z+z);
    }

    public static void tick(LivingEntity entity) {
        KnockdownData data = ACTIVE.get(entity);
        if (data == null) return;
        long now = entity.level().getGameTime();
        // A short client grace tolerates an effect packet arriving after the pose packet.
        boolean missingStun = !entity.hasEffect(ModEffects.STUN)
                && (!entity.level().isClientSide || now - data.started >= 3);
        if (!entity.isAlive() || entity.isRemoved() || entity.level() != data.level
                || entity.isPassenger() || now >= data.until || missingStun) { clear(entity); return; }
        lockYaw(entity, data.yaw);
    }

    public static void clear(LivingEntity entity) {
        KnockdownData data = ACTIVE.remove(entity);
        if (data == null) return;
        if (entity instanceof Player player && player.getForcedPose() == Pose.STANDING) {
            player.setForcedPose(data.restorePose);
            if (data.restorePose != null) player.setPose(data.restorePose);
        }
        entity.refreshDimensions();
        if (!entity.level().isClientSide)
            NetworkHandler.sendToTrackingAndSelf(new S2C_SinevaKnockdown(entity.getId(), 0, 0), entity);
    }

    public static void tracking(ServerPlayer viewer, LivingEntity target) {
        KnockdownData data = ACTIVE.get(target);
        if (data != null && data.until > target.level().getGameTime())
            NetworkHandler.sendToPlayer(new S2C_SinevaKnockdown(target.getId(),
                    (int)(data.until-target.level().getGameTime()), data.yaw), viewer);
    }

    public static void clearSide(boolean client) {
        LivingEntity[] entities;
        synchronized (ACTIVE) { entities = ACTIVE.keySet().toArray(LivingEntity[]::new); }
        for (LivingEntity entity : entities) if (entity.level().isClientSide == client) clear(entity);
    }

    private static void lockYaw(LivingEntity entity, float yaw) {
        entity.setYRot(yaw); entity.yRotO = yaw;
        entity.setYHeadRot(yaw); entity.yHeadRotO = yaw;
        entity.setYBodyRot(yaw); entity.yBodyRotO = yaw;
    }

    private static Vec3 horizontalAway(LivingEntity attacker, LivingEntity target) {
        Vec3 away = target.position().subtract(attacker.position());
        away = new Vec3(away.x, 0.0D, away.z);
        if (away.lengthSqr() < 0.0001D) {
            Vec3 look = attacker.getLookAngle();
            away = new Vec3(look.x, 0.0D, look.z);
        }
        if (away.lengthSqr() < 0.0001D) {
            return new Vec3(0.0D, 0.0D, 1.0D);
        }
        return away.normalize();
    }

    private static float yawToward(LivingEntity attacker, LivingEntity target) {
        Vec3 toward = attacker.position().subtract(target.position());
        toward = new Vec3(toward.x, 0.0D, toward.z);
        if (toward.lengthSqr() < 0.0001D) {
            Vec3 look = attacker.getLookAngle();
            toward = new Vec3(-look.x, 0.0D, -look.z);
        }
        if (toward.lengthSqr() < 0.0001D) {
            toward = new Vec3(0.0D, 0.0D, 1.0D);
        }
        toward = toward.normalize();
        return (float) (Mth.atan2(toward.z, toward.x) * Mth.RAD_TO_DEG - 90.0D);
    }

    private static final class KnockdownData {
        final Object level;
        final long started, until;
        final float yaw, width, length;
        final Pose restorePose;
        KnockdownData(Object level, long started, int ticks, float yaw, float width, float length, Pose restorePose) {
            this.level=level; this.started=started; this.until=started+ticks; this.yaw=yaw;
            this.width=width; this.length=length; this.restorePose=restorePose;
        }
    }
}
