package com.rzy.dealt_force_skills.character.sineva;

import com.rzy.dealt_force_skills.advancement.DfsAchievements;
import com.rzy.dealt_force_skills.character.SkillSlot;
import com.rzy.dealt_force_skills.character.commander.CommanderBranchUpgrade;
import com.rzy.dealt_force_skills.character.commander.CommanderStateManager;
import com.rzy.dealt_force_skills.stamina.StaminaManager;
import com.rzy.dealt_force_skills.stamina.StaminaRules;
import com.rzy.dealt_force_skills.effect.InjuryManager;
import com.rzy.dealt_force_skills.entity.BladeWireProjectileEntity;
import com.rzy.dealt_force_skills.entity.GrappleHookEntity;
import com.rzy.dealt_force_skills.registry.ModBlocks;
import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.registry.ModEntities;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.skill.SkillDamageHelper;
import com.rzy.dealt_force_skills.skill.SkillCooldownHelper;
import com.rzy.dealt_force_skills.util.RangedSoundHelper;
import com.rzy.dealt_force_skills.util.TargetingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SinevaSkills {
    private static volatile double FRONT_DOT = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("FRONT_DOT", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("characters.sineva.sineva_skills.front_dot", Math.cos(Math.toRadians(60.0))));
    private static volatile int BASH_COOLDOWN_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("BASH_COOLDOWN_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.sineva.sineva_skills.bash_cooldown_ticks", 10));
    private static volatile int CHARGE_COOLDOWN_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("CHARGE_COOLDOWN_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.sineva.sineva_skills.charge_cooldown_ticks", 20));
    private static volatile int BASH_ACTIVE_STAMINA_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("BASH_ACTIVE_STAMINA_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.sineva.sineva_skills.bash_active_stamina_ticks", 20));
    private static volatile int SILENCE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("SILENCE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.sineva.sineva_skills.silence_ticks", 60));
    private static volatile double PROJECTILE_REFLECT_MIN_SPEED = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("PROJECTILE_REFLECT_MIN_SPEED", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("characters.sineva.sineva_skills.projectile_reflect_min_speed", 1.4));
    private static volatile double PROJECTILE_REFLECT_MAX_SPEED = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("PROJECTILE_REFLECT_MAX_SPEED", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("characters.sineva.sineva_skills.projectile_reflect_max_speed", 3.8));
    private static volatile double PROJECTILE_CHARGE_SPEED_BONUS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("PROJECTILE_CHARGE_SPEED_BONUS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue(
      "characters.sineva.sineva_skills.projectile_charge_speed_bonus", 0.45
   ));
    private static final int GRAPPLE_CHARGE_SOUND_READY_TICKS = 25;
    private static final int GRAPPLE_CHARGE_SOUND_STALE_TICKS = 80;
    private static final ConcurrentHashMap<UUID, ChargeData> CHARGING_PLAYERS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, GrappleChargeSoundData> GRAPPLE_CHARGE_SOUNDS = new ConcurrentHashMap<>();

    private SinevaSkills() {
    }

    public static boolean useSkill(ServerPlayer player, SkillSlot slot, boolean alternate) {
        SinevaStateManager.initializeIfNeeded(player);
        return switch (slot) {
            case ACTIVE_1 -> useBladeWire(player, alternate);
            case ACTIVE_2 -> useGrapple(player, alternate);
            case CORE -> useBombSuit(player, alternate);
            case PASSIVE -> false;
        };
    }

    public static void deployBladeWire(ServerLevel level, BlockPos center, ServerPlayer owner) {
        BlockPos corePos = findGroundPlacement(level, center);
        if (corePos == null) {
            return;
        }

        level.setBlock(corePos, ModBlocks.BLADE_WIRE_CORE.get().defaultBlockState(), 3);
        if (level.getBlockEntity(corePos) instanceof com.rzy.dealt_force_skills.block.BladeWireBlockEntity core) {
            core.configureCore(owner.getUUID());
        }

        int placedWires = 0;
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                if (dx == 0 && dz == 0) continue;
                if (dx * dx + dz * dz > 16) continue;

                BlockPos wirePos = findGroundPlacement(level, corePos.offset(dx, 0, dz));
                if (wirePos == null || wirePos.equals(corePos) || !level.getBlockState(wirePos).canBeReplaced()) {
                    continue;
                }

                level.setBlock(wirePos, ModBlocks.BLADE_WIRE.get().defaultBlockState(), 3);
                if (level.getBlockEntity(wirePos) instanceof com.rzy.dealt_force_skills.block.BladeWireBlockEntity wire) {
                    wire.configureWire(owner.getUUID(), corePos);
                    placedWires++;
                }
            }
        }

        if (level.getBlockEntity(corePos) instanceof com.rzy.dealt_force_skills.block.BladeWireBlockEntity core) {
            core.setLinkedWireCount(placedWires);
        }

        level.playSound(null, corePos, ModSounds.SINEVA_BARBED_WIRE_LAND.get(), SoundSource.BLOCKS, 1.0f, 1.0f);
        damageBladeWireBurst(level, corePos, owner);
    }

    public static boolean tryShieldBash(ServerPlayer player) {
        if (!com.rzy.dealt_force_skills.stamina.StaminaFeature.SINEVA_BASH.enabled(player)) return false;
        if (!canUseShieldAction(player) || !SinevaStateManager.isBashReady(player)) {
            return false;
        }

        if (StaminaManager.consume(player, StaminaManager.skillCost(player,
                com.rzy.dealt_force_skills.stamina.StaminaSettings.bashCost()))
                != StaminaManager.ConsumeResult.SUCCESS) return false;
        SinevaStateManager.setBashCooldown(player, BASH_COOLDOWN_TICKS);

        RangedSoundHelper.playFollowingPlayer(player, ModSounds.SINEVA_SHIELD_BASH.get(),
                SoundSource.PLAYERS, 1.0f, 1.0f, 32.0D);

        Vec3 forward = player.getLookAngle().normalize();
        AABB box = player.getBoundingBox().inflate(2.0, 1.0, 2.0);
        reflectProjectiles(player, box, forward, FRONT_DOT, null, 0.0D);
        boolean controlVoicePlayed = false;
        int controlledTargets = 0;
        for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class, box,
                entity -> TargetingUtil.isHostileLivingFor(player, entity))) {
            Vec3 toTarget = target.position().add(0, target.getBbHeight() * 0.5, 0)
                    .subtract(player.getEyePosition()).normalize();
            if (forward.dot(toTarget) < FRONT_DOT) continue;

            float damage = target instanceof Player
                    ? com.rzy.dealt_force_skills.config.SkillBalance.damage("characters.sineva.shield_bash.player_damage", 5.0F)
                    : com.rzy.dealt_force_skills.config.SkillBalance.damage("characters.sineva.shield_bash.non_player_damage", 50.0F);
            SkillDamageHelper.hurt(target, SkillDamageHelper.sinevaShieldBash(player.serverLevel(), null, player), player, damage);
            playShieldBashHit(player, target);
            if (applyShieldBashControl(player, target) && !controlVoicePlayed) {
                controlledTargets++;
                playShieldBashControlVoice(player);
                controlVoicePlayed = true;
            } else if (target.hasEffect(ModEffects.STUN)) {
                controlledTargets++;
            }
        }
        DfsAchievements.recordSinevaShieldBashTargets(player, controlledTargets, true);
        return true;
    }

    public static boolean tryShieldCharge(ServerPlayer player) {
        if (!com.rzy.dealt_force_skills.stamina.StaminaFeature.SINEVA_CHARGE.enabled(player)) return false;
        if (!canUseShieldAction(player) || !SinevaStateManager.isChargeReady(player)) {
            return false;
        }
        if (CHARGING_PLAYERS.containsKey(player.getUUID())) {
            return true;
        }

        Vec3 look = player.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0.0, look.z);
        if (horizontal.lengthSqr() < 0.001) {
            return true;
        }

        if (StaminaManager.consume(player, StaminaManager.skillCost(player,
                com.rzy.dealt_force_skills.stamina.StaminaSettings.chargeCost()))
                != StaminaManager.ConsumeResult.SUCCESS) return false;
        SinevaStateManager.setChargeCooldown(player, CHARGE_COOLDOWN_TICKS);
        CHARGING_PLAYERS.put(player.getUUID(), new ChargeData(horizontal.normalize(), 14));
        RangedSoundHelper.playFollowingPlayer(player, ModSounds.SINEVA_SHIELD_BASH_CHARGE.get(),
                SoundSource.PLAYERS, 1.0f, 0.8f, 32.0D);
        return true;
    }

    public static void tickChargingPlayers(Level level) {
        if (level.isClientSide) return;

        var iterator = CHARGING_PLAYERS.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                iterator.remove();
                continue;
            }

            ChargeData data = entry.getValue();
            if (!canUseShieldAction(player) || data.ticksRemaining-- <= 0 || player.horizontalCollision) {
                iterator.remove();
                continue;
            }

            Vec3 motion = data.direction.scale(1.65);
            player.setDeltaMovement(motion.x, player.getDeltaMovement().y, motion.z);
            player.hurtMarked = true;

            AABB box = player.getBoundingBox().inflate(2.0, 1.0, 2.0);
            reflectProjectiles(player, box, data.direction, 0.0D, data.reflectedProjectiles, PROJECTILE_CHARGE_SPEED_BONUS);
            for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class, box,
                    entity -> entity != player && entity.isAlive() && !data.hitEntities.contains(entity)
                            && TargetingUtil.isHostileLivingFor(player, entity))) {
                Vec3 toTarget = target.position().subtract(player.position()).normalize();
                if (data.direction.dot(new Vec3(toTarget.x, 0.0, toTarget.z).normalize()) < 0.0) continue;

                float damage = target instanceof Player
                    ? com.rzy.dealt_force_skills.config.SkillBalance.damage("characters.sineva.shield_charge.player_damage", 5.0F)
                    : com.rzy.dealt_force_skills.config.SkillBalance.damage("characters.sineva.shield_charge.non_player_damage", 50.0F);
                SkillDamageHelper.hurt(target, SkillDamageHelper.sinevaShieldBash(player.serverLevel(), null, player), player, damage);
                playShieldBashHit(player, target);
                if (applyShieldBashControl(player, target) && !data.controlVoicePlayed) {
                    playShieldBashControlVoice(player);
                    data.controlVoicePlayed = true;
                }
                data.hitEntities.add(target);
                DfsAchievements.recordSinevaShieldBashTargets(player, data.hitEntities.size(), false);
            }
        }
    }

    private static boolean applyShieldBashControl(ServerPlayer player, LivingEntity target) {
        if (!target.isAlive()) {
            return false;
        }
        target.addEffect(new MobEffectInstance(ModEffects.STUN, SILENCE_TICKS, com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.sineva.sineva_skills.effect.stun.0.amplifier", 0), false, true));
        SinevaKnockdownState.apply(player, target, SILENCE_TICKS);
        return true;
    }

    private static void playShieldBashControlVoice(ServerPlayer player) {
        RangedSoundHelper.playFollowingPlayer(player, ModSounds.SINEVA_SHIELD_BASH_CONTROL_VOICE.get(),
                SoundSource.PLAYERS, 1.0f, 1.0f, 32.0D);
    }

    private static void playShieldBashHit(ServerPlayer player, LivingEntity target) {
        player.level().playSound(null, target.blockPosition(), ModSounds.SINEVA_SHIELD_HIT.get(),
                SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private static void reflectProjectiles(ServerPlayer player, AABB box, Vec3 facing, double minDot,
                                           Set<UUID> alreadyReflected, double speedBonus) {
        Vec3 shieldFacing = normalizeOrFallback(facing, player.getLookAngle());
        for (Projectile projectile : player.level().getEntitiesOfClass(Projectile.class, box, candidate ->
                candidate.isAlive() && candidate.getOwner() != player)) {
            UUID projectileId = projectile.getUUID();
            if (alreadyReflected != null && alreadyReflected.contains(projectileId)) {
                continue;
            }

            Vec3 toProjectile = projectile.position().add(0.0D, projectile.getBbHeight() * 0.5D, 0.0D)
                    .subtract(player.getEyePosition());
            Vec3 fallbackDirection = normalizeOrFallback(toProjectile, shieldFacing);
            if (shieldFacing.dot(fallbackDirection) < minDot) {
                continue;
            }

            reflectProjectile(player, projectile, fallbackDirection, speedBonus);
            if (alreadyReflected != null) {
                alreadyReflected.add(projectileId);
            }
        }
    }

    private static void reflectProjectile(ServerPlayer player, Projectile projectile, Vec3 fallbackDirection, double speedBonus) {
        Vec3 incomingMotion = projectile.getDeltaMovement();
        double incomingSpeed = incomingMotion.length();
        double reflectSpeed = Math.min(PROJECTILE_REFLECT_MAX_SPEED,
                Math.max(PROJECTILE_REFLECT_MIN_SPEED, incomingSpeed + speedBonus));
        Vec3 reflectedMotion = incomingSpeed > 1.0E-4D
                ? incomingMotion.normalize().scale(-reflectSpeed)
                : fallbackDirection.scale(reflectSpeed);
        if (reflectedMotion.dot(fallbackDirection) < 0.15D) {
            reflectedMotion = fallbackDirection.scale(reflectSpeed);
        }

        projectile.setOwner(player);
        projectile.setDeltaMovement(reflectedMotion);
        alignProjectileRotation(projectile, reflectedMotion);
        projectile.hasImpulse = true;
        projectile.hurtMarked = true;
        player.level().playSound(null, projectile.blockPosition(), ModSounds.SINEVA_PROJECTILE_REFLECT.get(),
                SoundSource.PLAYERS, 0.9f, 1.0f);
    }

    private static void alignProjectileRotation(Projectile projectile, Vec3 motion) {
        double horizontal = motion.horizontalDistance();
        projectile.setYRot((float) (Math.atan2(motion.x, motion.z) * (180.0D / Math.PI)));
        projectile.setXRot((float) (Math.atan2(motion.y, horizontal) * -(180.0D / Math.PI)));
        projectile.yRotO = projectile.getYRot();
        projectile.xRotO = projectile.getXRot();
    }

    private static Vec3 normalizeOrFallback(Vec3 vector, Vec3 fallback) {
        if (vector.lengthSqr() >= 1.0E-6D) {
            return vector.normalize();
        }
        return fallback.lengthSqr() >= 1.0E-6D ? fallback.normalize() : new Vec3(0.0D, 0.0D, 1.0D);
    }

    public static boolean canUseShieldAction(Player player) {
        return SinevaStateManager.isShieldDeployed(player) && !player.hasEffect(ModEffects.STUN);
    }

    public static void clearForPlayer(ServerPlayer player) {
        CHARGING_PLAYERS.remove(player.getUUID());
        GRAPPLE_CHARGE_SOUNDS.remove(player.getUUID());
    }

    public static void clearRuntimeCaches() {
        CHARGING_PLAYERS.clear();
        GRAPPLE_CHARGE_SOUNDS.clear();
    }

    public static void beginGrappleChargeSound(ServerPlayer player) {
        if (!SinevaStateManager.isSineva(player) || !SinevaStateManager.isGrappleReady(player)) {
            return;
        }
        long now = SkillCooldownHelper.now(player);
        GrappleChargeSoundData current = GRAPPLE_CHARGE_SOUNDS.get(player.getUUID());
        if (current != null && now - current.startedAt <= GRAPPLE_CHARGE_SOUND_STALE_TICKS) {
            return;
        }
        GRAPPLE_CHARGE_SOUNDS.put(player.getUUID(), new GrappleChargeSoundData(now));
        RangedSoundHelper.playFollowingPlayer(player, ModSounds.SINEVA_GRAPPLE_CHARGE.get(),
                SoundSource.PLAYERS, 0.85F, 1.0F, 32.0D);
    }

    public static void markGrappleChargeSoundReady(ServerPlayer player) {
        GrappleChargeSoundData data = GRAPPLE_CHARGE_SOUNDS.get(player.getUUID());
        long now = SkillCooldownHelper.now(player);
        if (data == null || data.readyPlayed || now - data.startedAt < GRAPPLE_CHARGE_SOUND_READY_TICKS) {
            return;
        }
        data.readyPlayed = true;
        RangedSoundHelper.playFollowingPlayer(player, ModSounds.SINEVA_GRAPPLE_CHARGE_READY.get(),
                SoundSource.PLAYERS, 0.9F, 1.0F, 32.0D);
    }

    public static void cancelGrappleChargeSound(ServerPlayer player) {
        if (GRAPPLE_CHARGE_SOUNDS.remove(player.getUUID()) == null) {
            return;
        }
        RangedSoundHelper.playFollowingPlayer(player, ModSounds.SINEVA_GRAPPLE_RETRACT.get(),
                SoundSource.PLAYERS, 0.8F, 1.0F, 32.0D);
    }

    public static void finishGrappleChargeSound(ServerPlayer player) {
        GRAPPLE_CHARGE_SOUNDS.remove(player.getUUID());
    }

    private static boolean useBladeWire(ServerPlayer player, boolean heldThrow) {
        if (!SinevaStateManager.consumeBladeWireCharge(player)) {
            com.rzy.dealt_force_skills.skill.SkillCooldownHelper.notifyCooldown(player, Component.translatable("message.dealt_force_skills.sineva.blade_wire_empty"));
            return true;
        }

        ServerLevel level = player.serverLevel();
        BladeWireProjectileEntity projectile = new BladeWireProjectileEntity(ModEntities.BLADE_WIRE_PROJECTILE.get(), level, player);
        Vec3 look = player.getLookAngle().normalize();
        Vec3 start = player.getEyePosition().add(look.scale(heldThrow ? 0.55D : 0.6D));
        projectile.setPos(start.x, start.y - 0.1, start.z);
        projectile.setDeltaMovement(heldThrow ? look.scale(2.25D).add(0.0D, 0.14D, 0.0D) : look.scale(1.25D));
        projectile.setYRot(player.getYRot());
        projectile.setXRot(player.getXRot());
        level.addFreshEntity(projectile);
        RangedSoundHelper.playFollowingPlayer(player, ModSounds.SINEVA_BARBED_WIRE_THROW.get(),
                SoundSource.PLAYERS, 0.9F, 1.0F, 32.0D);
        return true;
    }

    private static boolean useGrapple(ServerPlayer player, boolean lockTarget) {
        finishGrappleChargeSound(player);
        if (!SinevaStateManager.isGrappleReady(player)) {
            com.rzy.dealt_force_skills.skill.SkillCooldownHelper.notifyCooldown(player,
                    Component.translatable("message.dealt_force_skills.sineva.grapple_cooldown"));
            return true;
        }

        ServerLevel level = player.serverLevel();
        Vec3 start = GrappleHookEntity.ropeOrigin(player, 1.0F);
        boolean multiGrapple = CommanderStateManager.hasBranchUpgrade(
                player, CommanderBranchUpgrade.SINEVA_MULTI_GRAPPLE);
        LivingEntity singleTarget = lockTarget ? SinevaGrappleTargeting.findTarget(level, player) : null;
        List<LivingEntity> targets = multiGrapple
                ? SinevaGrappleTargeting.findTargets(level, player)
                : (singleTarget == null ? List.of() : List.of(singleTarget));
        if (targets.isEmpty()) {
            spawnGrapple(level, player, start, null);
        } else {
            for (LivingEntity target : targets) {
                spawnGrapple(level, player, start, target);
            }
        }
        RangedSoundHelper.playFollowingPlayer(player, ModSounds.SINEVA_GRAPPLE_FIRE.get(),
                SoundSource.PLAYERS, 1.0f, 1.0f, 32.0D);
        SinevaStateManager.setGrappleCooldown(player);
        return true;
    }

    private static void spawnGrapple(ServerLevel level, ServerPlayer player, Vec3 start, LivingEntity target) {
        GrappleHookEntity hook = new GrappleHookEntity(ModEntities.GRAPPLE_HOOK.get(), level, player);
        Vec3 look = target == null
                ? player.getLookAngle().normalize()
                : target.getBoundingBox().getCenter().subtract(start).normalize();
        hook.setPos(start.x, start.y, start.z);
        hook.setDeltaMovement(look.scale(2.2));
        alignGrappleRotation(hook, look, player);
        level.addFreshEntity(hook);
    }

    private static void alignGrappleRotation(GrappleHookEntity hook, Vec3 direction, ServerPlayer player) {
        if (direction.lengthSqr() < 1.0E-6D) {
            hook.setYRot(player.getYRot());
            hook.setXRot(player.getXRot());
            return;
        }
        double horizontal = direction.horizontalDistance();
        hook.setYRot((float) (Math.atan2(direction.x, direction.z) * (180.0D / Math.PI)));
        hook.setXRot((float) (Math.atan2(direction.y, horizontal) * -(180.0D / Math.PI)));
        hook.yRotO = hook.getYRot();
        hook.xRotO = hook.getXRot();
    }

    private static boolean useBombSuit(ServerPlayer player, boolean alternate) {
        if (SinevaStateManager.isBombSuitActive(player)) {
            if (alternate) {
                SinevaStateManager.deactivateBombSuit(player);
                player.displayClientMessage(Component.translatable("message.dealt_force_skills.sineva.bomb_suit_deactivated"), true);
            } else {
                if (SinevaStateManager.isShieldBroken(player)
                        && !CommanderStateManager.hasBranchUpgrade(
                        player, CommanderBranchUpgrade.SINEVA_ADVANCED_MATERIALS)) {
                    player.displayClientMessage(Component.translatable("message.dealt_force_skills.sineva.shield_broken"), true);
                    SinevaStateManager.syncToClient(player);
                    SinevaStateManager.syncRenderStateToClients(player);
                    return true;
                }
                boolean deployed = !SinevaStateManager.isShieldDeployed(player);
                SinevaStateManager.setShieldDeployed(player, deployed);
                RangedSoundHelper.playFollowingPlayer(player, ModSounds.SINEVA_SHIELD_TOGGLE.get(),
                        SoundSource.PLAYERS, 0.85f, deployed ? 1.08f : 0.82f, 32.0D);
                player.displayClientMessage(Component.translatable(deployed
                        ? "message.dealt_force_skills.sineva.shield_deployed"
                        : "message.dealt_force_skills.sineva.shield_stowed"), true);
            }
            return true;
        }

        if (SinevaStateManager.isBombSuitEquipping(player)) {
            return true;
        }

        if (!SinevaStateManager.isBombSuitCooldownReady(player)) {
            com.rzy.dealt_force_skills.skill.SkillCooldownHelper.notifyCooldown(player,
                    Component.translatable("message.dealt_force_skills.sineva.bomb_suit_cooldown"));
            return true;
        }

        SinevaStateManager.startBombSuitEquip(player);
        RangedSoundHelper.playFollowingPlayer(player, ModSounds.SINEVA_RIOT_SUIT_PREPARE.get(),
                SoundSource.PLAYERS, 0.9f, 0.95f, 32.0D);
        RangedSoundHelper.playFollowingPlayer(player, ModSounds.SINEVA_RIOT_SUIT_VOICE.get(),
                SoundSource.VOICE, 1.0f, 1.0f, 32.0D);
        player.displayClientMessage(Component.translatable("message.dealt_force_skills.sineva.bomb_suit_equipping"), true);
        return true;
    }

    private static void damageBladeWireBurst(ServerLevel level, BlockPos corePos, ServerPlayer owner) {
        Vec3 center = Vec3.atCenterOf(corePos);
        AABB box = new AABB(corePos).inflate(4.0);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box,
                entity -> entity != owner && entity.isAlive() && entity.distanceToSqr(center) <= 16.0)) {
            SkillDamageHelper.hurt(target, SkillDamageHelper.sinevaBladeWire(level, null, owner), owner, com.rzy.dealt_force_skills.config.DealtForceConfig.floatValue("characters.sineva.sineva_skills.skill_hurt.2.damage", 12.0f));
            if (target instanceof ServerPlayer player) {
                InjuryManager.applyBladeWireBurst(player);
            }
        }
        // Blade wire explosion should destroy Uluru quick covers in range
        com.rzy.dealt_force_skills.character.uluru.UluruExplosionHelper.destroyQuickCovers(level, center, 5.0);
    }

    private static BlockPos findGroundPlacement(ServerLevel level, BlockPos preferred) {
        for (int dy = 1; dy >= -8; dy--) {
            BlockPos pos = preferred.offset(0, dy, 0);
            if (level.getBlockState(pos).canBeReplaced() && hasGround(level, pos)) {
                return pos;
            }
        }
        return null;
    }

    private static boolean hasGround(ServerLevel level, BlockPos pos) {
        BlockPos below = pos.below();
        return Block.canSupportCenter(level, below, Direction.UP)
                || !level.getBlockState(below).getCollisionShape(level, below).isEmpty();
    }

    private static final class ChargeData {
        private final Vec3 direction;
        private final Set<LivingEntity> hitEntities = new HashSet<>();
        private final Set<UUID> reflectedProjectiles = new HashSet<>();
        private boolean controlVoicePlayed;
        private int ticksRemaining;

        private ChargeData(Vec3 direction, int ticksRemaining) {
            this.direction = direction;
            this.ticksRemaining = ticksRemaining;
        }
    }

    private static final class GrappleChargeSoundData {
        private final long startedAt;
        private boolean readyPlayed;

        private GrappleChargeSoundData(long startedAt) {
            this.startedAt = startedAt;
        }
    }
}
