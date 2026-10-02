package com.rzy.dealt_force_skills.client.character;

import com.rzy.dealt_force_skills.character.gambler.GamblerSuperpower;
import com.rzy.dealt_force_skills.character.gambler.GamblerTargetMode;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;

public final class ClientGamblerTargetingState {
    private static final double TARGET_RANGE = 90.0D;
    private static final double MIN_TARGET_DOT = 0.965D;
    private static final int LOCK_MARKER_TICKS = 45;

    private static GamblerSuperpower pendingPower;
    private static int lockedTargetId = -1;
    private static int lockedTicks;

    private ClientGamblerTargetingState() {
    }

    public static void begin(GamblerSuperpower power) {
        pendingPower = power;
    }

    public static boolean hasPendingTargetPower() {
        return pendingPower != null;
    }

    public static GamblerSuperpower pendingPower() {
        return pendingPower;
    }

    public static void clearPending() {
        pendingPower = null;
    }

    public static void clearAll() {
        clearPending();
        lockedTargetId = -1;
        lockedTicks = 0;
    }

    public static void tick(Minecraft minecraft) {
        if (lockedTicks > 0) {
            lockedTicks--;
            if (lockedTicks <= 0) {
                lockedTargetId = -1;
            }
        }
        if (minecraft.player == null || !ClientGamblerHudState.shouldRender()) {
            clearAll();
        }
    }

    public static LivingEntity lockedTarget(Minecraft minecraft) {
        if (minecraft.level == null || lockedTargetId < 0 || lockedTicks <= 0) {
            return null;
        }
        Entity entity = minecraft.level.getEntity(lockedTargetId);
        return entity instanceof LivingEntity living && living.isAlive() ? living : null;
    }

    public static void lock(LivingEntity target) {
        lockedTargetId = target.getId();
        lockedTicks = LOCK_MARKER_TICKS;
    }

    public static LivingEntity findLookTarget(Minecraft minecraft, GamblerSuperpower power) {
        if (minecraft.player == null || minecraft.level == null || power == null) {
            return null;
        }
        Vec3 eye = minecraft.player.getEyePosition();
        Vec3 look = minecraft.player.getLookAngle().normalize();
        return minecraft.level.getEntitiesOfClass(LivingEntity.class,
                        minecraft.player.getBoundingBox().inflate(TARGET_RANGE),
                        target -> target != minecraft.player && validTarget(minecraft.player, target, power.targetMode()))
                .stream()
                .map(target -> new TargetCandidate(target, alignmentScore(eye, look, target)))
                .filter(candidate -> candidate.score() >= 0.0D)
                .max(Comparator.comparingDouble(TargetCandidate::score))
                .map(TargetCandidate::target)
                .orElse(null);
    }

    public static Component pendingPrompt(GamblerSuperpower power) {
        return Component.translatable("message.dealt_force_skills.gambler.select_target",
                Component.translatable(power.nameKey()));
    }

    private static boolean validTarget(Player player, LivingEntity target, GamblerTargetMode mode) {
        if (!target.isAlive()) {
            return false;
        }
        if (target instanceof Player targetPlayer && (targetPlayer.isSpectator() || targetPlayer.getAbilities().instabuild)) {
            return false;
        }
        if (mode == GamblerTargetMode.PLAYER && !(target instanceof Player)) {
            return false;
        }
        if (mode == GamblerTargetMode.NON_PLAYER && target instanceof Player) {
            return false;
        }
        return player.hasLineOfSight(target);
    }

    private static double alignmentScore(Vec3 eye, Vec3 look, LivingEntity target) {
        Vec3 center = target.position().add(0.0D, target.getBbHeight() * 0.55D, 0.0D);
        Vec3 toTarget = center.subtract(eye);
        double distance = toTarget.length();
        if (distance <= 0.001D || distance > TARGET_RANGE) {
            return -1.0D;
        }
        double dot = look.dot(toTarget.scale(1.0D / distance));
        if (dot < MIN_TARGET_DOT) {
            return -1.0D;
        }
        return dot * 1000.0D - distance;
    }

    private record TargetCandidate(LivingEntity target, double score) {
    }
}
