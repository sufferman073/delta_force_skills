package com.rzy.dealt_force_skills.boss;

import com.rzy.dealt_force_skills.config.DealtBossesConfig;
import com.rzy.dealt_force_skills.entity.BeaconBossEntity;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.S2C_SyncFearStacks;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class FearManager {
    private static final String ROOT = "DealtForceFear";
    private static final String STACKS = "Stacks";
    private static final String LAST_INCREASE = "LastIncreaseGameTime";
    private static final String CROSSED_50 = "Crossed50";
    private static final String CROSSED_80 = "Crossed80";
    private static final String LAST_SOURCE = "LastSourceUuid";
    private static final net.minecraft.resources.ResourceLocation SPEED_MODIFIER_ID =
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("dealt_force_skills", "fear_slow");

    // Live config reads for /reload hot-apply.
    private static int decayIdleTicks() {
        return DealtBossesConfig.intValue("beacon.fear.decay_idle_ticks", 5 * 20);
    }

    private static int decayPerSecond() {
        return DealtBossesConfig.intValue("beacon.fear.decay_per_second", 3);
    }

    /** Every N fear stacks reduces movement speed by speed_penalty_per_percent. */
    private static int speedStacksPerPercent() {
        return Math.max(1, DealtBossesConfig.intValue("beacon.fear.speed_stacks_per_percent", 5));
    }

    private static double speedPenaltyPerPercent() {
        return DealtBossesConfig.doubleValue("beacon.fear.speed_penalty_per_percent", 0.01D);
    }

    private static double deathRetainFraction() {
        return DealtBossesConfig.doubleValue("beacon.fear.death_retain_fraction", 0.75D);
    }

    private static int executeThreshold() {
        return DealtBossesConfig.intValue("beacon.fear.execute_threshold", 100);
    }

    private static int executeResetStacks() {
        return DealtBossesConfig.intValue("beacon.fear.execute_reset_stacks", 50);
    }

    private static int splashRadius() {
        return DealtBossesConfig.intValue("beacon.fear.kill_splash_radius", 10);
    }

    private static int splashAmount() {
        return DealtBossesConfig.intValue("beacon.fear.kill_splash_amount", 25);
    }

    private FearManager() {
    }

    public static int getStacks(Player player) {
        if (player == null) {
            return 0;
        }
        return Math.max(0, data(player).getInt(STACKS));
    }

    public static void setStacks(ServerPlayer player, int stacks) {
        setStacks(player, stacks, false);
    }

    public static void setStacks(ServerPlayer player, int stacks, boolean fromIncrease) {
        if (player == null) {
            return;
        }
        int clamped = Math.max(0, stacks);
        CompoundTag tag = data(player);
        int previous = tag.getInt(STACKS);
        tag.putInt(STACKS, clamped);
        if (fromIncrease && clamped > previous) {
            tag.putLong(LAST_INCREASE, player.level().getGameTime());
        }
        if (clamped < 50) {
            tag.putBoolean(CROSSED_50, false);
        }
        if (clamped < 80) {
            tag.putBoolean(CROSSED_80, false);
        }
        applySpeedModifier(player, clamped);
        applyThresholdEffects(player, previous, clamped);
        NetworkHandler.sendToPlayer(new S2C_SyncFearStacks(clamped), player);
        if (clamped >= executeThreshold()) {
            tryExecute(player);
        }
    }

    public static void addStacks(ServerPlayer player, int amount, LivingEntity source) {
        if (player == null || amount == 0 || player.isCreative() || player.isSpectator()) {
            return;
        }
        if (source != null) {
            data(player).putUUID(LAST_SOURCE, source.getUUID());
        }
        setStacks(player, getStacks(player) + amount, amount > 0);
    }

    public static void tick(ServerPlayer player) {
        if (player == null || player.level().isClientSide) {
            return;
        }
        int stacks = getStacks(player);
        if (stacks <= 0) {
            clearSpeedModifier(player);
            return;
        }
        applySpeedModifier(player, stacks);
        applyPersistentEffects(player, stacks);

        CompoundTag tag = data(player);
        long lastIncrease = tag.getLong(LAST_INCREASE);
        long now = player.level().getGameTime();
        if (now - lastIncrease >= decayIdleTicks() && player.tickCount % 20 == 0) {
            setStacks(player, stacks - decayPerSecond(), false);
        }
    }

    public static void onPlayerDeath(ServerPlayer player) {
        if (player == null) {
            return;
        }
        int stacks = getStacks(player);
        if (stacks <= 0) {
            return;
        }
        int retained = (int) Math.floor(stacks * deathRetainFraction());
        CompoundTag tag = data(player);
        tag.putInt(STACKS, Math.max(0, retained));
        if (retained < 50) {
            tag.putBoolean(CROSSED_50, false);
        }
        if (retained < 80) {
            tag.putBoolean(CROSSED_80, false);
        }
    }

    public static void copyOnClone(ServerPlayer original, ServerPlayer clone) {
        if (original == null || clone == null) {
            return;
        }
        CompoundTag from = data(original);
        CompoundTag to = data(clone);
        to.putInt(STACKS, from.getInt(STACKS));
        to.putLong(LAST_INCREASE, from.getLong(LAST_INCREASE));
        to.putBoolean(CROSSED_50, from.getBoolean(CROSSED_50));
        to.putBoolean(CROSSED_80, from.getBoolean(CROSSED_80));
        if (from.hasUUID(LAST_SOURCE)) {
            to.putUUID(LAST_SOURCE, from.getUUID(LAST_SOURCE));
        }
        int stacks = to.getInt(STACKS);
        applySpeedModifier(clone, stacks);
        NetworkHandler.sendToPlayer(new S2C_SyncFearStacks(stacks), clone);
    }

    public static void splashOnBeaconKill(ServerLevel level, Vec3 origin, ServerPlayer deadPlayer) {
        if (level == null || origin == null) {
            return;
        }
        AABB box = new AABB(origin, origin).inflate(splashRadius());
        List<ServerPlayer> players = level.getEntitiesOfClass(ServerPlayer.class, box,
                p -> p.isAlive() && p != deadPlayer && !p.isSpectator() && !p.isCreative());
        for (ServerPlayer player : players) {
            addStacks(player, splashAmount(), null);
        }
    }

    public static boolean blocksSleep(Player player) {
        return getStacks(player) > 0;
    }

    private static void tryExecute(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level) || !player.isAlive()) {
            return;
        }
        BeaconBossEntity boss = findExecutionBoss(level, player);
        if (boss == null || !boss.isAlive()) {
            setStacks(player, executeResetStacks(), false);
            return;
        }
        Vec3 behind = player.position().subtract(player.getLookAngle().normalize().scale(1.5D));
        boss.teleportTo(behind.x, player.getY(), behind.z);
        boss.setYRot(player.getYRot());
        boss.setXRot(player.getXRot());

        // Reset before death so LivingDeathEvent retains floor(reset * death_retain).
        setStacks(player, executeResetStacks(), false);
        player.sendSystemMessage(Component.translatable(
                "message.dealt_force_skills.beacon_boss.fear_execute", player.getGameProfile().getName()));
        player.hurt(boss.damageSources().mobAttack(boss), Math.max(1000.0F, player.getMaxHealth() * 100.0F));
        if (player.isAlive()) {
            player.kill();
        }
        boss.onFearExecuteKill(player);
    }

    private static BeaconBossEntity findExecutionBoss(ServerLevel level, ServerPlayer player) {
        CompoundTag tag = data(player);
        if (tag.hasUUID(LAST_SOURCE)) {
            var entity = level.getEntity(tag.getUUID(LAST_SOURCE));
            if (entity instanceof BeaconBossEntity boss && boss.isAlive()) {
                return boss;
            }
        }
        return level.getEntitiesOfClass(BeaconBossEntity.class, player.getBoundingBox().inflate(96.0D),
                        LivingEntity::isAlive).stream()
                .min((a, b) -> Double.compare(a.distanceToSqr(player), b.distanceToSqr(player)))
                .orElse(null);
    }

    private static void applyThresholdEffects(ServerPlayer player, int previous, int stacks) {
        CompoundTag tag = data(player);
        if (stacks >= 50 && previous < 50 && !tag.getBoolean(CROSSED_50)) {
            tag.putBoolean(CROSSED_50, true);
            player.displayClientMessage(Component.translatable(
                    "message.dealt_force_skills.beacon_boss.fear_50"), false);
        }
        if (stacks >= 80 && previous < 80 && !tag.getBoolean(CROSSED_80)) {
            tag.putBoolean(CROSSED_80, true);
            player.displayClientMessage(Component.translatable(
                    "message.dealt_force_skills.beacon_boss.fear_80"), false);
        }
        applyPersistentEffects(player, stacks);
    }

    private static void applyPersistentEffects(ServerPlayer player, int stacks) {
        if (stacks >= 50) {
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 40, 0, false, false, true));
        }
        if (stacks >= 80) {
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 40, 0, false, false, true));
        }
    }

    private static void applySpeedModifier(ServerPlayer player, int stacks) {
        AttributeInstance movement = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movement == null) {
            return;
        }
        movement.removeModifier(SPEED_MODIFIER_ID);
        if (stacks <= 0) {
            return;
        }
        int percents = stacks / speedStacksPerPercent();
        if (percents <= 0) {
            return;
        }
        double amount = -speedPenaltyPerPercent() * percents;
        movement.addTransientModifier(new AttributeModifier(
                SPEED_MODIFIER_ID, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    private static void clearSpeedModifier(ServerPlayer player) {
        AttributeInstance movement = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movement != null) {
            movement.removeModifier(SPEED_MODIFIER_ID);
        }
    }

    private static CompoundTag data(Player player) {
        CompoundTag root = player.getPersistentData();
        if (!root.contains(ROOT, CompoundTag.TAG_COMPOUND)) {
            root.put(ROOT, new CompoundTag());
        }
        return root.getCompound(ROOT);
    }
}
