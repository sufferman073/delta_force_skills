package com.rzy.dealt_force_skills.boss;

import com.rzy.dealt_force_skills.config.DealtBossesConfig;
import com.rzy.dealt_force_skills.entity.BeaconBossEntity;
import com.rzy.dealt_force_skills.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeaconBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class BeaconSummonManager {
    private static volatile int SUMMON_DELAY_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("SUMMON_DELAY_TICKS", () -> com.rzy.dealt_force_skills.config.DealtBossesConfig.intValue("beacon.summon.delay_ticks", 40));
    private static volatile int SUMMON_COOLDOWN_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("SUMMON_COOLDOWN_TICKS", () -> com.rzy.dealt_force_skills.config.DealtBossesConfig.intValue("beacon.summon.cooldown_ticks", 1200));
    private static final Map<String, Long> COOLDOWNS = new HashMap<>();
    private static final Map<String, PendingSummon> PENDING = new HashMap<>();

    private BeaconSummonManager() {
    }

    public static boolean tryStartSummon(ServerPlayer player, ServerLevel level, BlockPos pos) {
        if (player == null || level == null || pos == null) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof BeaconBlock)) {
            player.displayClientMessage(Component.translatable(
                    "message.dealt_force_skills.beacon_boss.summon_not_beacon"), true);
            return false;
        }
        if (!isActivatedBeacon(level, pos)) {
            player.displayClientMessage(Component.translatable(
                    "message.dealt_force_skills.beacon_boss.summon_inactive"), true);
            return false;
        }
        String key = key(level.dimension(), pos);
        long now = level.getGameTime();
        Long readyAt = COOLDOWNS.get(key);
        if (readyAt != null && readyAt > now) {
            long remainSec = Math.max(1L, (readyAt - now + 19L) / 20L);
            player.displayClientMessage(Component.translatable(
                    "message.dealt_force_skills.beacon_boss.summon_cooldown", remainSec), true);
            return false;
        }
        if (PENDING.containsKey(key)) {
            player.displayClientMessage(Component.translatable(
                    "message.dealt_force_skills.beacon_boss.summon_pending"), true);
            return false;
        }

        PENDING.put(key, new PendingSummon(level.dimension(), pos.immutable(), now + SUMMON_DELAY_TICKS));
        COOLDOWNS.put(key, now + SUMMON_COOLDOWN_TICKS);
        level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.4F, 0.8F);
        level.playSound(null, pos, SoundEvents.BEACON_AMBIENT, SoundSource.BLOCKS, 1.0F, 1.2F);
        spawnChargeParticles(level, pos);
        player.displayClientMessage(Component.translatable(
                "message.dealt_force_skills.beacon_boss.summon_start"), true);
        return true;
    }

    public static void serverTick(ServerLevel level) {
        if (level == null || PENDING.isEmpty()) {
            return;
        }
        long now = level.getGameTime();
        Iterator<Map.Entry<String, PendingSummon>> it = PENDING.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, PendingSummon> entry = it.next();
            PendingSummon pending = entry.getValue();
            if (!pending.dimension.equals(level.dimension())) {
                continue;
            }
            spawnChargeParticles(level, pending.pos);
            if (now < pending.spawnAt) {
                continue;
            }
            it.remove();
            spawnBoss(level, pending.pos);
        }
        COOLDOWNS.entrySet().removeIf(e -> e.getValue() <= now - 20L * 60L * 10L);
    }

    public static boolean isActivatedBeacon(Level level, BlockPos pos) {
        if (level == null || pos == null) {
            return false;
        }
        if (!(level.getBlockState(pos).getBlock() instanceof BeaconBlock)) {
            return false;
        }
        // At least one full mineral pyramid layer (3x3 under the beacon).
        BlockPos base = pos.below();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (!level.getBlockState(base.offset(dx, 0, dz)).is(BlockTags.BEACON_BASE_BLOCKS)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static void spawnBoss(ServerLevel level, BlockPos pos) {
        BeaconBossEntity boss = ModEntities.BEACON_BOSS.get().create(level);
        if (boss == null) {
            return;
        }
        boss.moveTo(pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D, level.random.nextFloat() * 360.0F, 0.0F);
        boss.setPersistenceRequired();
        level.addFreshEntity(boss);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                pos.getX() + 0.5D, pos.getY() + 1.2D, pos.getZ() + 0.5D,
                1, 0.0D, 0.0D, 0.0D, 0.0D);
        for (int i = 0; i < 40; i++) {
            level.sendParticles(ParticleTypes.END_ROD,
                    pos.getX() + 0.5D, pos.getY() + 1.0D + level.random.nextDouble() * 2.0D, pos.getZ() + 0.5D,
                    1,
                    (level.random.nextDouble() - 0.5D) * 1.2D,
                    level.random.nextDouble() * 0.6D,
                    (level.random.nextDouble() - 0.5D) * 1.2D,
                    0.05D);
        }
        level.playSound(null, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D,
                SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.2F, 0.9F);
        level.playSound(null, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D,
                SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 0.7F, 1.1F);
    }

    private static void spawnChargeParticles(ServerLevel level, BlockPos pos) {
        for (int i = 0; i < 8; i++) {
            level.sendParticles(ParticleTypes.END_ROD,
                    pos.getX() + 0.5D, pos.getY() + 1.0D + level.random.nextDouble() * 2.5D, pos.getZ() + 0.5D,
                    1, 0.15D, 0.35D, 0.15D, 0.01D);
            level.sendParticles(ParticleTypes.FIREWORK,
                    pos.getX() + 0.5D, pos.getY() + 1.2D, pos.getZ() + 0.5D,
                    2, 0.25D, 0.5D, 0.25D, 0.01D);
        }
    }

    private static String key(ResourceKey<Level> dimension, BlockPos pos) {
        return dimension.location() + "|" + pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }

    private record PendingSummon(ResourceKey<Level> dimension, BlockPos pos, long spawnAt) {
    }
}
