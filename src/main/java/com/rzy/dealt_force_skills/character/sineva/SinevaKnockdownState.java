package com.rzy.dealt_force_skills.character.sineva;

import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.S2C_SinevaKnockdown;
import com.rzy.dealt_force_skills.registry.ModEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class SinevaKnockdownState {
    private static volatile double KNOCKBACK_HORIZONTAL = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("KNOCKBACK_HORIZONTAL", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("characters.sineva.sineva_knockdown_state.knockback_horizontal", 0.85));
    private static volatile double KNOCKBACK_VERTICAL = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("KNOCKBACK_VERTICAL", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("characters.sineva.sineva_knockdown_state.knockback_vertical", 0.18));
    private static final Map<UUID, KnockdownData> ACTIVE = new HashMap<>();

    private SinevaKnockdownState() {
    }

    public static void apply(ServerPlayer attacker, LivingEntity target, int ticks) {
        if (!(target instanceof ServerPlayer player) || ticks <= 0 || !player.isAlive()) {
            return;
        }
        if (player.hasEffect(ModEffects.SHAKEHANDS)) {
            return;
        }

        Vec3 away = horizontalAway(attacker, player);
        float yaw = yawToward(attacker, player);
        ACTIVE.put(player.getUUID(), new KnockdownData(ticks, yaw));

        player.setForcedPose(Pose.SLEEPING);
        player.setPose(Pose.SLEEPING);
        player.refreshDimensions();
        player.setDeltaMovement(
                away.x * KNOCKBACK_HORIZONTAL,
                Math.max(player.getDeltaMovement().y, KNOCKBACK_VERTICAL),
                away.z * KNOCKBACK_HORIZONTAL
        );
        player.hurtMarked = true;

        NetworkHandler.sendToTrackingAndSelf(new S2C_SinevaKnockdown(player.getId(), ticks, yaw), player);
    }

    public static void tick(ServerPlayer player) {
        KnockdownData data = ACTIVE.get(player.getUUID());
        if (data == null) {
            return;
        }

        if (!player.isAlive() || !player.hasEffect(ModEffects.STUN) || data.remainingTicks <= 0) {
            clear(player);
            return;
        }

        player.setForcedPose(Pose.SLEEPING);
        player.setPose(Pose.SLEEPING);
        player.setYRot(data.yaw);
        player.yRotO = data.yaw;
        player.setYHeadRot(data.yaw);
        player.yHeadRotO = data.yaw;
        player.setYBodyRot(data.yaw);
        player.yBodyRotO = data.yaw;
        player.refreshDimensions();

        data.remainingTicks--;
        if (data.remainingTicks <= 0) {
            clear(player);
        }
    }

    public static void clear(ServerPlayer player) {
        if (ACTIVE.remove(player.getUUID()) == null) {
            return;
        }

        player.setForcedPose(null);
        player.refreshDimensions();
        NetworkHandler.sendToTrackingAndSelf(new S2C_SinevaKnockdown(player.getId(), 0, 0.0F), player);
    }

    private static Vec3 horizontalAway(ServerPlayer attacker, ServerPlayer target) {
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

    private static float yawToward(ServerPlayer attacker, ServerPlayer target) {
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
        private int remainingTicks;
        private final float yaw;

        private KnockdownData(int remainingTicks, float yaw) {
            this.remainingTicks = remainingTicks;
            this.yaw = yaw;
        }
    }
}
