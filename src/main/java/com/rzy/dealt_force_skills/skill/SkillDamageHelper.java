package com.rzy.dealt_force_skills.skill;

import com.rzy.dealt_force_skills.boss.BossCombatRules;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.commander.CommanderStateManager;
import com.rzy.dealt_force_skills.compat.TrainingTargetCompat;
import com.rzy.dealt_force_skills.registry.ModGameRules;
import com.rzy.dealt_force_skills.team.DealtTeamManager;
import com.rzy.dealt_force_skills.util.TargetingUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.List;

public final class SkillDamageHelper {
    private static final ThreadLocal<net.minecraft.server.level.ServerPlayer> ACTIVE_SKILL_PLAYER = new ThreadLocal<>();
    public static net.minecraft.server.level.ServerPlayer activeSkillPlayer() { return ACTIVE_SKILL_PLAYER.get(); }
    private static final ThreadLocal<Boolean> COMMANDER_SKILL_DAMAGE =
            ThreadLocal.withInitial(() -> false);
    private static volatile float BONUS_PER_EXPERIENCE_LEVEL = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("BONUS_PER_EXPERIENCE_LEVEL", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.floatValue("experience_growth.general.skill_damage_per_level", 0.005F));
    private static volatile float NON_PLAYER_TARGET_MULTIPLIER = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("NON_PLAYER_TARGET_MULTIPLIER", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.floatValue("general.skilldamagehelper.non_player_target_multiplier", 5.0F));
    public static final ResourceKey<DamageType> TRUE_SKILL_DAMAGE = key("true_skill");
    public static final ResourceKey<DamageType> SINEVA_BLADE_WIRE = key("sineva_blade_wire");
    public static final ResourceKey<DamageType> SINEVA_GRAPPLE = key("sineva_grapple");
    public static final ResourceKey<DamageType> SINEVA_SHIELD_BASH = key("sineva_shield_bash");
    public static final ResourceKey<DamageType> SHEPHERD_SONIC = key("shepherd_sonic");
    public static final ResourceKey<DamageType> SHEPHERD_FRAG_GRENADE = key("shepherd_frag_grenade");
    public static final ResourceKey<DamageType> D_WOLF_HAND_CANNON = key("d_wolf_hand_cannon");
    public static final ResourceKey<DamageType> D_WOLF_SELF_REWARD = key("d_wolf_self_reward");
    public static final ResourceKey<DamageType> ULURU_MISSILE = key("uluru_missile");
    public static final ResourceKey<DamageType> ULURU_INCENDIARY = key("uluru_incendiary");
    public static final ResourceKey<DamageType> ULURU_FIRE_FIELD = key("uluru_fire_field");
    public static final ResourceKey<DamageType> GIZMO_SPIDERLING = key("gizmo_spiderling");
    public static final ResourceKey<DamageType> LUNA_SHOCK_ARROW = key("luna_shock_arrow");
    public static final ResourceKey<DamageType> VYRON_MAGNETIC_BOMB = key("vyron_magnetic_bomb");
    public static final ResourceKey<DamageType> MANBA_DUEL_EXECUTE = key("manba_duel_execute");
    public static final ResourceKey<DamageType> MANBA_ELBOW = key("manba_elbow");
    public static final ResourceKey<DamageType> NOX_ROTOR = key("nox_rotor");
    public static final ResourceKey<DamageType> NOX_FLASH = key("nox_flash");
    public static final ResourceKey<DamageType> TOXIK_FIREFLY = key("toxik_firefly");
    public static final ResourceKey<DamageType> RAPTOR_PULSE = key("raptor_pulse");
    public static final ResourceKey<DamageType> RAPTOR_FALCON = key("raptor_falcon");
    public static final ResourceKey<DamageType> NIKAIDOU_WEAPON = key("nikaidou_weapon");
    public static final ResourceKey<DamageType> NIKAIDOU_DECAY = key("nikaidou_decay");
    public static final ResourceKey<DamageType> CATDAD_REFLECT = key("catdad_reflect");
    public static final ResourceKey<DamageType> CATDAD_STRIKE = key("catdad_strike");
    public static final ResourceKey<DamageType> CATDAD_TRUCK = key("catdad_truck");
    public static final ResourceKey<DamageType> DEPARTMENT_LASER = key("department_laser");
    public static final ResourceKey<DamageType> DEPARTMENT_TRAP = key("department_trap");
    public static final ResourceKey<DamageType> DEPARTMENT_TRAP_MANUAL = key("department_trap_manual");
    public static final ResourceKey<DamageType> DEPARTMENT_CORE = key("department_core");
    public static final ResourceKey<DamageType> DEPARTMENT_PASSIVE_BLAST = key("department_passive_blast");
    public static final ResourceKey<DamageType> SHAKEHANDS_REFLECT = key("shakehands_reflect");

    private SkillDamageHelper() {
    }

    private static ResourceKey<DamageType> key(String id) {
        return ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, id));
    }

    public static float scale(LivingEntity source, float amount) {
        if (!(source instanceof Player player) || amount <= 0.0f) {
            return amount;
        }
        return (float) (amount * (1.0f + ModGameRules.effectiveExperienceLevel(player) * BONUS_PER_EXPERIENCE_LEVEL)
                * CommanderStateManager.skillStrengthMultiplier(player));
    }

    public static boolean hurt(LivingEntity target, DamageSource source, LivingEntity sourceEntity, float amount) {
        if (shouldSkipTeammateDamage(target, source, sourceEntity)) {
            return false;
        }
        float scaled = scaleForTarget(target, sourceEntity, amount);
        if (sourceEntity instanceof net.minecraft.server.level.ServerPlayer commander) {
            DamageSource appliedSource = source;
            boolean convert = CommanderStateManager.convertsSkillDamage(commander)
                    && !TrainingTargetCompat.recordsDamageWithoutDying(target)
                    && target.level() instanceof ServerLevel;
            if (convert) {
                ServerLevel level = (ServerLevel) target.level();
                appliedSource = level.damageSources().fellOutOfWorld();
            }
            net.minecraft.server.level.ServerPlayer previousSkillPlayer = ACTIVE_SKILL_PLAYER.get();
            ACTIVE_SKILL_PLAYER.set(commander);
            COMMANDER_SKILL_DAMAGE.set(true);
            try {
                boolean applied = target.hurt(appliedSource, scaled);
                CommanderStateManager.onSkillDamage(commander, target, appliedSource, scaled, applied);
                return applied;
            } finally {
                COMMANDER_SKILL_DAMAGE.remove();
                if (previousSkillPlayer == null) ACTIVE_SKILL_PLAYER.remove(); else ACTIVE_SKILL_PLAYER.set(previousSkillPlayer);
            }
        }
        return target.hurt(source, scaled);
    }

    public static boolean isApplyingCommanderSkillDamage() {
        return COMMANDER_SKILL_DAMAGE.get();
    }

    public static boolean hurtUnscaled(LivingEntity target, DamageSource source, float amount) {
        // Reflect damage must never be blocked by teammate filters (source entity is the shield holder).
        if (!isReflectDamage(source) && shouldSkipTeammateDamage(target, source, null)) {
            return false;
        }
        return target.hurt(source, amount);
    }

    private static boolean isReflectDamage(DamageSource source) {
        return source != null && (source.is(SHAKEHANDS_REFLECT) || source.is(CATDAD_REFLECT));
    }

    public static float scaleForTarget(LivingEntity target, LivingEntity sourceEntity, float amount) {
        float scaled = scale(sourceEntity, amount);
        if (BossCombatRules.receivesNonPlayerSkillMultiplier(target)) {
            scaled *= NON_PLAYER_TARGET_MULTIPLIER;
        }
        return scaled;
    }

    private static boolean shouldSkipTeammateDamage(LivingEntity target, DamageSource source, LivingEntity sourceEntity) {
        Entity attacker = sourceEntity != null ? sourceEntity : source.getEntity();
        if (target instanceof Player player && !TargetingUtil.isTargetablePlayer(player)) {
            return true;
        }
        return sourceEntity instanceof Player player
                && target == player
                && CommanderStateManager.isCommander(player)
                || TargetingUtil.isPlayerOwnedSummonOf(attacker, target)
                || attacker instanceof Player && target instanceof Player
                && DealtTeamManager.areTeammates(attacker, target);
    }

    public static DamageSource trueDamage(ServerLevel level, Entity directEntity, Entity causingEntity) {
        return source(level, TRUE_SKILL_DAMAGE, directEntity, causingEntity);
    }

    public static List<ResourceKey<DamageType>> allTypes(ServerLevel level) {
        return level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).entrySet().stream()
                .map(java.util.Map.Entry::getKey)
                .toList();
    }

    public static DamageSource sinevaBladeWire(ServerLevel level, Entity directEntity, Entity causingEntity) {
        return source(level, SINEVA_BLADE_WIRE, directEntity, causingEntity);
    }

    public static DamageSource sinevaGrapple(ServerLevel level, Entity directEntity, Entity causingEntity) {
        return source(level, SINEVA_GRAPPLE, directEntity, causingEntity);
    }

    public static DamageSource sinevaShieldBash(ServerLevel level, Entity directEntity, Entity causingEntity) {
        return source(level, SINEVA_SHIELD_BASH, directEntity, causingEntity);
    }

    public static DamageSource shepherdSonic(ServerLevel level, Entity directEntity, Entity causingEntity) {
        return source(level, SHEPHERD_SONIC, directEntity, causingEntity);
    }

    public static DamageSource shepherdFragGrenade(ServerLevel level, Entity directEntity, Entity causingEntity) {
        return source(level, SHEPHERD_FRAG_GRENADE, directEntity, causingEntity);
    }

    public static DamageSource dWolfHandCannon(ServerLevel level, Entity directEntity, Entity causingEntity) {
        return source(level, D_WOLF_HAND_CANNON, directEntity, causingEntity);
    }

    public static DamageSource dWolfSelfReward(ServerLevel level, Entity directEntity, Entity causingEntity) {
        return source(level, D_WOLF_SELF_REWARD, directEntity, causingEntity);
    }

    public static DamageSource uluruMissile(ServerLevel level, Entity directEntity, Entity causingEntity) {
        return source(level, ULURU_MISSILE, directEntity, causingEntity);
    }

    public static DamageSource uluruIncendiary(ServerLevel level, Entity directEntity, Entity causingEntity) {
        return source(level, ULURU_INCENDIARY, directEntity, causingEntity);
    }

    public static DamageSource uluruFireField(ServerLevel level, Entity directEntity) {
        return source(level, ULURU_FIRE_FIELD, directEntity, null);
    }

    public static DamageSource gizmoSpiderling(ServerLevel level, Entity directEntity, Entity causingEntity) {
        return source(level, GIZMO_SPIDERLING, directEntity, causingEntity);
    }

    public static DamageSource lunaShockArrow(ServerLevel level, Entity directEntity, Entity causingEntity) {
        return source(level, LUNA_SHOCK_ARROW, directEntity, causingEntity);
    }

    public static DamageSource vyronMagneticBomb(ServerLevel level, Entity directEntity, Entity causingEntity) {
        return source(level, VYRON_MAGNETIC_BOMB, directEntity, causingEntity);
    }

    public static DamageSource manbaDuelExecute(ServerLevel level, Entity directEntity, Entity causingEntity) {
        return source(level, MANBA_DUEL_EXECUTE, directEntity, causingEntity);
    }

    public static DamageSource manbaElbow(ServerLevel level, Entity directEntity, Entity causingEntity) {
        return source(level, MANBA_ELBOW, directEntity, causingEntity);
    }

    public static DamageSource noxRotor(ServerLevel level, Entity directEntity, Entity causingEntity) {
        return source(level, NOX_ROTOR, directEntity, causingEntity);
    }

    public static DamageSource noxFlash(ServerLevel level, Entity directEntity, Entity causingEntity) {
        return source(level, NOX_FLASH, directEntity, causingEntity);
    }

    public static DamageSource toxikFirefly(ServerLevel level, Entity directEntity, Entity causingEntity) {
        return source(level, TOXIK_FIREFLY, directEntity, causingEntity);
    }

    public static DamageSource raptorPulse(ServerLevel level, Entity directEntity, Entity causingEntity) {
        return source(level, RAPTOR_PULSE, directEntity, causingEntity);
    }

    public static DamageSource raptorFalcon(ServerLevel level, Entity directEntity, Entity causingEntity) {
        return source(level, RAPTOR_FALCON, directEntity, causingEntity);
    }

    public static DamageSource nikaidouWeapon(ServerLevel level, Entity directEntity, Entity causingEntity) {
        return source(level, NIKAIDOU_WEAPON, directEntity, causingEntity);
    }

    public static DamageSource nikaidouDecay(ServerLevel level, Entity directEntity, Entity causingEntity) {
        return source(level, NIKAIDOU_DECAY, directEntity, causingEntity);
    }

    public static DamageSource catDadReflect(ServerLevel level, Entity directEntity, Entity causingEntity) {
        return source(level, CATDAD_REFLECT, directEntity, causingEntity);
    }

    public static DamageSource catDadStrike(ServerLevel level, Entity directEntity, Entity causingEntity) {
        return source(level, CATDAD_STRIKE, directEntity, causingEntity);
    }

    public static DamageSource catDadTruck(ServerLevel level, Entity directEntity, Entity causingEntity) {
        return source(level, CATDAD_TRUCK, directEntity, causingEntity);
    }

    public static DamageSource departmentLaser(ServerLevel level, Entity directEntity, Entity causingEntity) {
        return source(level, DEPARTMENT_LASER, directEntity, causingEntity);
    }

    public static DamageSource departmentTrap(ServerLevel level, Entity directEntity, Entity causingEntity) {
        return source(level, DEPARTMENT_TRAP, directEntity, causingEntity);
    }

    public static DamageSource departmentTrapManual(ServerLevel level, Entity directEntity, Entity causingEntity) {
        return source(level, DEPARTMENT_TRAP_MANUAL, directEntity, causingEntity);
    }

    public static DamageSource departmentCore(ServerLevel level, Entity directEntity, Entity causingEntity) {
        return source(level, DEPARTMENT_CORE, directEntity, causingEntity);
    }

    public static DamageSource departmentPassiveBlast(ServerLevel level, Entity directEntity, Entity causingEntity) {
        return source(level, DEPARTMENT_PASSIVE_BLAST, directEntity, causingEntity);
    }

    public static DamageSource shakehandsReflect(ServerLevel level, Entity directEntity, Entity causingEntity) {
        return source(level, SHAKEHANDS_REFLECT, directEntity, causingEntity);
    }

    public static DamageSource source(ServerLevel level, ResourceKey<DamageType> type, Entity directEntity, Entity causingEntity) {
        return new DamageSource(
                level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(type),
                directEntity,
                causingEntity
        );
    }
}
