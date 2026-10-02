package com.rzy.dealt_force_skills.event;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.boss.BeaconSummonManager;
import com.rzy.dealt_force_skills.boss.FearManager;
import com.rzy.dealt_force_skills.entity.BeaconBossEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.CanPlayerSleepEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID)
public final class BeaconBossEvents {
    private BeaconBossEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }
        if (event.getEntity() instanceof ServerPlayer player) {
            FearManager.tick(player);
        }
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel().isClientSide) {
            return;
        }
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            BeaconSummonManager.serverTick(serverLevel);
        }
    }

    /**
     * Fear / injury apply on attack connect, even if later damage is fully absorbed (shield / armor).
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingAttack(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide) {
            return;
        }
        BeaconBossEntity boss = beaconAttacker(event.getSource());
        if (boss != null && boss.isAlive()) {
            boss.onAttackConnected(player);
        }
    }

    private static BeaconBossEntity beaconAttacker(DamageSource source) {
        if (source == null) {
            return null;
        }
        Entity entity = source.getEntity();
        if (entity instanceof BeaconBossEntity boss) {
            return boss;
        }
        Entity direct = source.getDirectEntity();
        if (direct instanceof BeaconBossEntity boss) {
            return boss;
        }
        if (direct instanceof Projectile projectile && projectile.getOwner() instanceof BeaconBossEntity boss) {
            return boss;
        }
        return null;
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            FearManager.onPlayerDeath(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!(event.getOriginal() instanceof ServerPlayer original)
                || !(event.getEntity() instanceof ServerPlayer clone)) {
            return;
        }
        if (event.isWasDeath()) {
            FearManager.copyOnClone(original, clone);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            FearManager.setStacks(player, FearManager.getStacks(player), false);
        }
    }

    @SubscribeEvent
    public static void onCanPlayerSleep(CanPlayerSleepEvent event) {
        if (FearManager.blocksSleep(event.getEntity())) {
            event.setProblem(Player.BedSleepingProblem.OTHER_PROBLEM);
            event.getEntity().displayClientMessage(Component.translatable(
                    "message.dealt_force_skills.beacon_boss.fear_sleep"), true);
        }
    }
}