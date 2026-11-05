package com.rzy.dealt_force_skills.event;

import com.rzy.dealt_force_skills.config.DealtForceConfig;
import com.rzy.dealt_force_skills.arena.HvkAsaraArenaManager;
import com.rzy.dealt_force_skills.advancement.DfsAchievements;
import com.rzy.dealt_force_skills.block.BladeWireBlockEntity;
import com.rzy.dealt_force_skills.block.QuickCoverBlock;
import com.rzy.dealt_force_skills.block.QuickCoverBlockEntity;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.chamber.ChamberGunKind;
import com.rzy.dealt_force_skills.character.chamber.ChamberStateManager;
import com.rzy.dealt_force_skills.character.chamber.ChamberTaczEnhancement;
import com.rzy.dealt_force_skills.character.CharacterSelectionManager;
import com.rzy.dealt_force_skills.character.commander.CommanderBranchUpgrade;
import com.rzy.dealt_force_skills.character.commander.CommanderStateManager;
import com.rzy.dealt_force_skills.character.commander.CommanderSupportManager;
import com.rzy.dealt_force_skills.compat.SuperbWarfareCompat;
import com.rzy.dealt_force_skills.compat.TrainingTargetCompat;
import com.rzy.dealt_force_skills.character.catdad.CatDadStateManager;
import com.rzy.dealt_force_skills.character.corps.CorpsStateManager;
import com.rzy.dealt_force_skills.character.department.DepartmentOfTransportationStateManager;
import com.rzy.dealt_force_skills.character.dwolf.DWolfSkills;
import com.rzy.dealt_force_skills.character.dwolf.DWolfStateManager;
import com.rzy.dealt_force_skills.character.dwolf.DWolfTool;
import com.rzy.dealt_force_skills.character.gizmo.GizmoStateManager;
import com.rzy.dealt_force_skills.character.gambler.GamblerArenaManager;
import com.rzy.dealt_force_skills.character.gambler.GamblerStateManager;
import com.rzy.dealt_force_skills.character.gizmo.GizmoTool;
import com.rzy.dealt_force_skills.character.ghroth.GhrothStateManager;
import com.rzy.dealt_force_skills.character.ghroth.GhrothTaczEnhancement;
import com.rzy.dealt_force_skills.character.hackclaw.HackclawStateManager;
import com.rzy.dealt_force_skills.character.lexninjia.LexNinjiaStateManager;
import com.rzy.dealt_force_skills.character.luna.LunaStateManager;
import com.rzy.dealt_force_skills.character.luna.LunaTool;
import com.rzy.dealt_force_skills.character.manba.ManbaStateManager;
import com.rzy.dealt_force_skills.character.morse.MorseStateManager;
import com.rzy.dealt_force_skills.character.morse.MorseTool;
import com.rzy.dealt_force_skills.character.nikaidou.NikaidouHiroStateManager;
import com.rzy.dealt_force_skills.character.nikaidou.NikaidouHiroTool;
import com.rzy.dealt_force_skills.character.nikaidou.NikaidouHiroWitchificationStateManager;
import com.rzy.dealt_force_skills.character.nox.NoxStateManager;
import com.rzy.dealt_force_skills.character.raptor.RaptorStateManager;
import com.rzy.dealt_force_skills.character.raptor.RaptorTool;
import com.rzy.dealt_force_skills.character.saeed.SaeedGuardType;
import com.rzy.dealt_force_skills.character.saeed.SaeedStateManager;
import com.rzy.dealt_force_skills.character.shepherd.ShepherdStateManager;
import com.rzy.dealt_force_skills.character.shepherd.ShepherdTool;
import com.rzy.dealt_force_skills.character.uluru.UluruTool;
import com.rzy.dealt_force_skills.character.sineva.SinevaShieldGeometry;
import com.rzy.dealt_force_skills.character.sineva.SinevaKnockdownState;
import com.rzy.dealt_force_skills.character.sineva.SinevaSkills;
import com.rzy.dealt_force_skills.character.sineva.SinevaStateManager;
import com.rzy.dealt_force_skills.character.stinger.StingerStateManager;
import com.rzy.dealt_force_skills.character.stinger.StingerTool;
import com.rzy.dealt_force_skills.character.tempest.TempestStateManager;
import com.rzy.dealt_force_skills.character.tempest.TempestTool;
import com.rzy.dealt_force_skills.character.toxik.ToxikStateManager;
import com.rzy.dealt_force_skills.character.toxik.ToxikTool;
import com.rzy.dealt_force_skills.character.uluru.UluruExplosionHelper;
import com.rzy.dealt_force_skills.character.uluru.UluruStateManager;
import com.rzy.dealt_force_skills.character.undead.UndeadProfession;
import com.rzy.dealt_force_skills.character.undead.UndeadStateManager;
import com.rzy.dealt_force_skills.character.undead.UndeadSupportManager;
import com.rzy.dealt_force_skills.character.undead.UndeadUpgradeManager;
import com.rzy.dealt_force_skills.character.vlinder.VlinderStateManager;
import com.rzy.dealt_force_skills.character.vlinder.VlinderTool;
import com.rzy.dealt_force_skills.character.vyron.VyronSkills;
import com.rzy.dealt_force_skills.character.vyron.VyronStateManager;
import com.rzy.dealt_force_skills.character.vyron.VyronTool;
import com.rzy.dealt_force_skills.effect.NoxDelayedWoundEffect;
import com.rzy.dealt_force_skills.effect.InjuryManager;
import com.rzy.dealt_force_skills.effect.NoxFlashedEffect;
import com.rzy.dealt_force_skills.effect.ModItemEffectHelper;
import com.rzy.dealt_force_skills.effect.ManbaBlindedEffect;
import com.rzy.dealt_force_skills.effect.HackclawFlashBlindEffect;
import com.rzy.dealt_force_skills.effect.MorseFlashedEffect;
import com.rzy.dealt_force_skills.effect.ShakehandsCombatHandler;
import com.rzy.dealt_force_skills.effect.ToxikTearGasBlindEffect;
import com.rzy.dealt_force_skills.effect.ToxikFireflyInterferenceEffect;
import com.rzy.dealt_force_skills.team.RoundStartFreezeManager;
import com.rzy.dealt_force_skills.team.TeamCombatRules;
import com.rzy.dealt_force_skills.util.EmptyHandExtinguishHandler;
import com.rzy.dealt_force_skills.util.MeleeWeaponCompat;
import com.rzy.dealt_force_skills.util.ReconRevealThrottle;
import com.rzy.dealt_force_skills.util.TargetingUtil;
import com.rzy.dealt_force_skills.entity.RaptorFalconDroneEntity;
import com.rzy.dealt_force_skills.entity.AsaraTrooperEntity;
import com.rzy.dealt_force_skills.entity.AcceleratedTroopsEntity;
import com.rzy.dealt_force_skills.entity.SaeedGuardEntity;
import com.rzy.dealt_force_skills.entity.UluruLoiteringMissileEntity;
import com.rzy.dealt_force_skills.item.DfsEquipmentItem;
import com.rzy.dealt_force_skills.item.EternalLoveBlessingItem;
import com.rzy.dealt_force_skills.item.DfsEquipmentItem.Faction;
import com.rzy.dealt_force_skills.item.DfsEquipmentItem.Profile;
import com.rzy.dealt_force_skills.item.DfsEquipmentItem.SpecialAbility;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.S2C_CharacterHitFeedback;
import com.rzy.dealt_force_skills.registry.ModBlocks;
import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.registry.ModGameRules;
import com.rzy.dealt_force_skills.registry.ModItems;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.shop.HaffCoinManager;
import com.rzy.dealt_force_skills.shop.LexNinjiaCurrencyManager;
import com.rzy.dealt_force_skills.shop.UndeadSoulManager;
import com.rzy.dealt_force_skills.skill.SkillCooldownHelper;
import com.rzy.dealt_force_skills.skill.SkillDamageHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import com.rzy.dealt_force_skills.util.ItemStackTagReads;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.PlayLevelSoundEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.event.village.WandererTradesEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.world.phys.BlockHitResult;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = DealtForceSkillsMod.MODID)
public class CommonEvents {
    private static final String TACZ_AMMO_HIT_BLOCK_EVENT = "com.tacz.guns.api.event.server.AmmoHitBlockEvent";
    private static final String TACZ_GUN_RELOAD_EVENT = "com.tacz.guns.api.event.common.GunReloadEvent";
    private static final String TACZ_GUN_SHOOT_EVENT = "com.tacz.guns.api.event.common.GunShootEvent";
    private static final String TACZ_GUN_FIRE_EVENT = "com.tacz.guns.api.event.common.GunFireEvent";
    private static final String TACZ_ENTITY_HURT_BY_GUN_PRE_EVENT = "com.tacz.guns.api.event.common.EntityHurtByGunEvent$Pre";
    private static final String TACZ_ATTACHMENT_PROPERTY_EVENT = "com.tacz.guns.api.event.common.AttachmentPropertyEvent";
    private static final String TACZ_GUN_MELEE_EVENT = "com.tacz.guns.api.event.common.GunMeleeEvent";
    private static final String TACZ_GUN_FIRE_SELECT_EVENT = "com.tacz.guns.api.event.common.GunFireSelectEvent";
    private static final String TACZ_GUN_OPERATOR = "com.tacz.guns.api.entity.IGunOperator";
    private static final String TACZ_CLIENT_GUN_OPERATOR = "com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator";
    private static final TagKey<DamageType> TACZ_BULLETS_TAG = TagKey.create(Registries.DAMAGE_TYPE, ResourceLocation.tryBuild("tacz", "bullets"));
    private static volatile int EFFECT_REAPPLY_GRACE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("EFFECT_REAPPLY_GRACE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("events.common_events.effect_reapply_grace_ticks", 2));
    private static volatile double SHIELD_DEPLOYED_SLOW_AMOUNT = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("SHIELD_DEPLOYED_SLOW_AMOUNT", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("events.common_events.shield_deployed_slow_amount", -0.1));
    private static volatile double SHIELD_DAMAGE_SLOW_MAX_TOTAL = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("SHIELD_DAMAGE_SLOW_MAX_TOTAL", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("events.common_events.shield_damage_slow_max_total", 0.95));
    private static volatile double EQUIPMENT_PROJECTILE_NEAR_INFLATE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("EQUIPMENT_PROJECTILE_NEAR_INFLATE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("events.common_events.equipment_projectile_near_inflate", 0.75));
    private static volatile double EQUIPMENT_RANGED_TRACE_INFLATE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("EQUIPMENT_RANGED_TRACE_INFLATE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("events.common_events.equipment_ranged_trace_inflate", 0.25));
    private static final String EQUIPMENT_FATAL_GUARD_UNTIL = "dealt_force_skills.equipment_fatal_guard_until";
    private static final String ASARA_SET_FATAL_GUARD_UNTIL = "dealt_force_skills.asara_set_fatal_guard_until";
    private static final String GLOBAL_FORCES_LOCK_TARGET = "dealt_force_skills.global_forces_lock_target";
    private static final String GLOBAL_FORCES_LOCK_PROGRESS_TARGET = "dealt_force_skills.global_forces_lock_progress_target";
    private static final String GLOBAL_FORCES_LOCK_PROGRESS_TICKS = "dealt_force_skills.global_forces_lock_progress_ticks";
    private static final String RED_OWL_REVENGE_TYPE = "dealt_force_skills.red_owl_revenge_type";
    private static final String EQUIPMENT_RESTORE_UNTIL = "dealt_force_skills.equipment_restore_until";
    private static final String EQUIPMENT_SUMMON_UNTIL = "dealt_force_skills.equipment_summon_until";
    private static final String EQUIPMENT_HEAVY_DODGE_SUMMON_UNTIL =
            "dealt_force_skills.equipment_heavy_dodge_summon_until";
    private static final String FS_STATIONARY_STACKS = "dealt_force_skills.fs_stationary_stacks";
    private static final String FS_STATIONARY_TICKS = "dealt_force_skills.fs_stationary_ticks";
    private static final String FS_MOVING_TICKS = "dealt_force_skills.fs_moving_ticks";
    private static final String FS_LAST_X = "dealt_force_skills.fs_last_x";
    private static final String FS_LAST_Y = "dealt_force_skills.fs_last_y";
    private static final String FS_LAST_Z = "dealt_force_skills.fs_last_z";
    private static final String TRICK_STACKS = "dealt_force_skills.trick_stacks";
    private static final String TRICK_UNTIL = "dealt_force_skills.trick_until";
    private static final String DICH9_STACKS = "dealt_force_skills.dich9_stacks";
    private static final String DICH9_UNTIL = "dealt_force_skills.dich9_until";
    private static final String MHS_FURY_UNTIL = "dealt_force_skills.mhs_fury_until";
    private static final String MHS_FURY_SLOT = "dealt_force_skills.mhs_fury_slot";
    private static final String TACZ_GUN_ID_TAG = "GunId";
    private static final String TACZ_GUN_CURRENT_AMMO_COUNT_TAG = "GunCurrentAmmoCount";
    private static final String TACZ_GUN_HAS_BULLET_IN_BARREL_TAG = "HasBulletInBarrel";
    private static final String EQUIPMENT_STATIONARY = "dealt_force_skills.equipment_stationary";
    private static final String EQUIPMENT_LAST_X = "dealt_force_skills.equipment_last_x";
    private static final String EQUIPMENT_LAST_Y = "dealt_force_skills.equipment_last_y";
    private static final String EQUIPMENT_LAST_Z = "dealt_force_skills.equipment_last_z";
    private static final String EQUIPMENT_STATIONARY_DAMAGE_TICKS = "dealt_force_skills.equipment_stationary_damage_ticks";
    private static volatile float H09_STATIONARY_MIN_SELF_DAMAGE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("H09_STATIONARY_MIN_SELF_DAMAGE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.floatValue("events.common_events.h09_stationary_min_self_damage", 0.5F));
    private static volatile float H09_STATIONARY_SELF_DAMAGE_MAX_HEALTH_FRACTION = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("H09_STATIONARY_SELF_DAMAGE_MAX_HEALTH_FRACTION", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.floatValue(
      "events.common_events.h09_stationary_self_damage_max_health_fraction", 0.01F
   ));
    /** Multiplier applied to unprotected limb hits when {@code dealtlimbsdamage} is on. */
    private static volatile float LIMBS_UNPROTECTED_DAMAGE_REDUCTION = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("LIMBS_UNPROTECTED_DAMAGE_REDUCTION", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.floatValue("events.common_events.limbs_unprotected_damage_reduction", 0.35F));
    /** When hit height cannot be estimated, prefer torso absorption over a full miss. */
    private static volatile double EQUIPMENT_UNKNOWN_HIT_HEIGHT_RATIO = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("EQUIPMENT_UNKNOWN_HIT_HEIGHT_RATIO", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue(
      "events.common_events.equipment_unknown_hit_height_ratio", 0.55
   ));
    private static volatile double PREMIUM_COFFEE_FAILURE_BASE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("PREMIUM_COFFEE_FAILURE_BASE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("events.common_events.premium_coffee_failure_base", 0.95));
    private static volatile double PREMIUM_COFFEE_FAILURE_REDUCTION_PER_LEVEL = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("PREMIUM_COFFEE_FAILURE_REDUCTION_PER_LEVEL", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue(
      "events.common_events.premium_coffee_failure_reduction_per_level", 0.15
   ));
    private static volatile double PREMIUM_COFFEE_DROP_BASE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("PREMIUM_COFFEE_DROP_BASE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("events.common_events.premium_coffee_drop_base", 0.01));
    private static volatile double PREMIUM_COFFEE_DROP_BONUS_PER_LEVEL = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("PREMIUM_COFFEE_DROP_BONUS_PER_LEVEL", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue(
      "events.common_events.premium_coffee_drop_bonus_per_level", 0.01
   ));
    private static volatile double NEW_RECRUIT_MINING_DUPLICATE_CHANCE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("NEW_RECRUIT_MINING_DUPLICATE_CHANCE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue(
      "events.common_events.new_recruit_mining_duplicate_chance", 0.25
   ));
    private static volatile int GLOBAL_FORCES_LOCK_TICKS_REQUIRED = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("GLOBAL_FORCES_LOCK_TICKS_REQUIRED", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("events.common_events.global_forces_lock_ticks_required", 80));
    private static volatile double GLOBAL_FORCES_ALLY_RANGE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("GLOBAL_FORCES_ALLY_RANGE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("events.common_events.global_forces_ally_range", 20.0));
    private static volatile double GLOBAL_FORCES_LOCK_RAY_RADIUS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("GLOBAL_FORCES_LOCK_RAY_RADIUS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("events.common_events.global_forces_lock_ray_radius", 1.0));
    private static volatile double SAEED_HAKIM_ROCKET_DAMAGE_RADIUS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("SAEED_HAKIM_ROCKET_DAMAGE_RADIUS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("events.common_events.saeed_hakim_rocket_damage_radius", 3.0));
    private static volatile float SAEED_HAKIM_ROCKET_DAMAGE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("SAEED_HAKIM_ROCKET_DAMAGE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.floatValue("events.common_events.saeed_hakim_rocket_damage", 40.0F));
    private static volatile double GTI_OVERLOAD_LIMIT_MULTIPLIER = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("GTI_OVERLOAD_LIMIT_MULTIPLIER", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("events.common_events.gti_overload_limit_multiplier", 2.0));
    private static volatile int MHS_FURY_DURATION_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("MHS_FURY_DURATION_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("events.common_events.mhs_fury_duration_ticks", 300));
    private static volatile double MHS_FURY_TRIGGER_RANGE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("MHS_FURY_TRIGGER_RANGE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("events.common_events.mhs_fury_trigger_range", 2.0));
    private static final String KING_KONG_COOLDOWN_UNTIL = "dealt_force_skills.king_kong_cooldown_until";
    private static final String KING_KONG_TARGET_ID = "dealt_force_skills.king_kong_target_id";
    private static final String KING_KONG_STRIKE_TICK = "dealt_force_skills.king_kong_strike_tick";
    private static final String HACKCLAW_BACKSTAB_OWNER = "dealt_force_skills.hackclaw_backstab_owner";
    private static final String HACKCLAW_BACKSTAB_UNTIL = "dealt_force_skills.hackclaw_backstab_until";
    private static final UUID EQUIPMENT_ASSAULT_MOVEMENT_UUID = UUID.fromString("ddf4b7a1-9809-46d6-8e8f-7d78cd7e1101");
    private static final UUID EQUIPMENT_MHS_ROOT_UUID = UUID.fromString("82b98383-88a5-4b72-935b-49b6b1d7f001");
    private static final Map<UUID, Map<MobEffect, TrackedEffect>> ULURU_EFFECT_TRACKING = new HashMap<>();
    private static final Map<UUID, Integer> WEBBED_SELECTED_SLOTS = new HashMap<>();
    private static final Map<UUID, Integer> TEMPEST_ACTION_LOCKED_SELECTED_SLOTS = new HashMap<>();
    private static final Map<UUID, LockedLook> WEBBED_LOOK_LOCKS = new HashMap<>();
    private static final Map<UUID, TaczAmmoSnapshot> MHS_TACTICAL_AMMO_SNAPSHOTS = new HashMap<>();
    private static final Map<UUID, GhrothDamageFloor> GHROTH_DAMAGE_FLOORS = new HashMap<>();
    private static final Map<UUID, LinkedList<GhrothStarsPendingShot>> GHROTH_STARS_PENDING_SHOTS = new HashMap<>();
    private static final Map<UUID, LinkedList<GhrothNoonPendingCopy>> GHROTH_NOON_PENDING_COPIES = new HashMap<>();
    private static final Set<VillagerProfession> BLUEPRINT_PROFESSIONS = Set.of(
            VillagerProfession.ARMORER,
            VillagerProfession.CARTOGRAPHER,
            VillagerProfession.FLETCHER,
            VillagerProfession.LEATHERWORKER,
            VillagerProfession.LIBRARIAN,
            VillagerProfession.MASON,
            VillagerProfession.WEAPONSMITH,
            VillagerProfession.TOOLSMITH
    );
    private static final int[] BLUEPRINT_TRADE_LEVELS = {3, 4, 5};
    private static boolean taczReloadBridgeChecked;
    private static boolean taczReloadBridgeAvailable;
    private static boolean ghrothNoonCopyingDamage;
    private static boolean hackclawReflectingDamage;
    private static boolean commanderGeneConvertingDamage;
    private static Method taczFromLivingEntity;
    private static Method taczCancelReload;

    private static final String TOUHOU_MAID_PICKUP_POST_EVENT =
            "com.github.tartaricacid.touhoulittlemaid.api.event.MaidPickupEvent$ItemResultPost";
    private static final String TOUHOU_MAID_PICKUP_PRE_EVENT =
            "com.github.tartaricacid.touhoulittlemaid.api.event.MaidPickupEvent$ItemResultPre";
    private static final String EQUIPMENT_FEEDBACK_UNTIL = "DfsEquipmentFeedbackUntil";

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingAttack(LivingAttackEvent event) {
        if (com.rzy.dealt_force_skills.character.rover.RoverStateManager.executingDeath(event.getEntity())
                || CorpsStateManager.isExecutingLoyaltyTarget(event.getEntity())) return;
        DamageSource source = event.getSource();
        if (AsaraTrooperEntity.shouldCancelFriendlyDamage(event.getEntity(), source)) {
            event.setCanceled(true);
            return;
        }
        if (TeamCombatRules.shouldCancelFriendlyDamage(event.getEntity(), source)) {
            event.setCanceled(true);
            return;
        }
        if (event.getEntity() instanceof ServerPlayer target
                && CommanderSupportManager.isAdsProtectedFor(target, damageSourceEntity(source), source)) {
            event.setCanceled(true);
            return;
        }
        if (event.getEntity() instanceof ServerPlayer target
                && NikaidouHiroWitchificationStateManager.shouldBlockFinalAttack(target, source)) {
            event.setCanceled(true);
            return;
        }
        ShakehandsCombatHandler.handleLivingAttack(event);
        if (event.isCanceled()) {
            return;
        }
        if (event.getEntity() instanceof ServerPlayer target
                && UluruStateManager.tryRedirectDamageToCover(target, event.getAmount())) {
            event.setCanceled(true);
            return;
        }
        if (event.getEntity() instanceof ServerPlayer target
                && LexNinjiaStateManager.handleHandGuardAttack(target, source)) {
            event.setCanceled(true);
            return;
        }
        if (UndeadSupportManager.shouldCancelFriendlyFire(event.getEntity(), source)) {
            event.setCanceled(true);
            return;
        }
        if (event.getEntity() instanceof Player targetPlayer
                && source.getEntity() instanceof Player attackerPlayer
                && GhrothStateManager.shouldCancelCeasefire(targetPlayer, attackerPlayer)) {
            event.setCanceled(true);
            return;
        }
        if (source.getEntity() instanceof ServerPlayer attacker) {
            DepartmentOfTransportationStateManager.revealFromOffense(attacker);
            if (UndeadStateManager.blocksOutgoingAttack(attacker)) {
                event.setCanceled(true);
                return;
            }
        }
        if (tryConvertCommanderOrdinaryAttack(event)) {
            return;
        }
        if (event.getEntity() instanceof ServerPlayer target
                && DepartmentOfTransportationStateManager.isFullyConcealed(target)) {
            event.setCanceled(true);
            return;
        }
        if (event.getEntity() instanceof ServerPlayer target
                && UndeadStateManager.blocksIncomingAttack(target)) {
            if(event.getAmount()>0&&UndeadStateManager.explorerSpaceActive(target))
                com.rzy.dealt_force_skills.department.DepartmentTasks.signal(target,"undead.explorer.immune");
            event.setCanceled(true);
            return;
        }
        if (!(event.getEntity() instanceof Player player)) return;

        if (GhrothStateManager.isTacticalImmune(player)) {
            event.setCanceled(true);
            return;
        }
        if (Float.isFinite(event.getAmount()) && event.getAmount()>0.0F && GhrothStateManager.shouldDodge(player, source)) {
            if(player instanceof ServerPlayer owner)
                com.rzy.dealt_force_skills.department.DepartmentGhroth.dodged(owner,event::isCanceled);
            event.setCanceled(true);
            return;
        }

        if (StingerStateManager.isDowned(player)
                && !StingerStateManager.isExecutingDownedDeath(player)
                && !ManbaStateManager.isExecutingDuelTarget(player)) {
            event.setCanceled(true);
            return;
        }

        if (VlinderStateManager.isDowned(player)
                && !VlinderStateManager.isExecutingDownedDeath(player)
                && !ManbaStateManager.isExecutingDuelTarget(player)) {
            event.setCanceled(true);
            return;
        }

        if (CatDadStateManager.isDowned(player)
                && !ManbaStateManager.isExecutingDuelTarget(player)) {
            event.setCanceled(true);
            return;
        }

        if (player instanceof ServerPlayer serverPlayer
                && CatDadStateManager.tryBlockDamage(serverPlayer, event.getAmount())) {
            event.setCanceled(true);
            return;
        }

        if (player instanceof ServerPlayer serverPlayer && ManbaStateManager.isManba(player)) {
            ManbaStateManager.recordDamageTaken(serverPlayer);
            if (ManbaStateManager.shouldIgnoreIncomingDamage(player, source)
                    || ManbaStateManager.trySteelBody(serverPlayer, source, event.getAmount())) {
                event.setCanceled(true);
                return;
            }
            ManbaStateManager.tryTriggerDiversion(serverPlayer, source);
        }

        // Bomb suit immunities
        if (SinevaStateManager.isBombSuitActive(player)) {
            if (source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE)
                    || source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)
                    || source.is(net.minecraft.tags.DamageTypeTags.WITCH_RESISTANT_TO)) {
                // Fire / void / magic — fully immune
                event.setCanceled(true);
                return;
            }
            // Generic magic damage (wither, dragon breath, etc.)
            if (source.getMsgId().equals("indirectMagic") || source.getMsgId().equals("magic")
                    || source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_EFFECTS)) {
                event.setCanceled(true);
                return;
            }
        }

        Vec3 sourcePos = damageSourcePosition(source);
        if (sourcePos == null) return;

        if (tryAbsorbFrontShield(player, source, sourcePos, event.getAmount())) {
            if (event.getAmount() > 0.0f && isPlayerSourcedDamage(source)) {
                SinevaStateManager.recordPlayerShieldDamage(player);
            }
            event.setCanceled(true);
            return;
        }

        Entity attacker = damageSourceEntity(source);
        if (attacker == null) return;

        if (SinevaStateManager.isSineva(player)
                && (CommanderStateManager.hasBranchUpgrade(player, CommanderBranchUpgrade.SINEVA_FULL_PROTECTION)
                || !SinevaStateManager.isShieldDeployed(player))
                && Util.isFromBehind(player, sourcePos, 70.0f)
                && Util.isUpperBodySource(player, attacker)) {
            com.rzy.dealt_force_skills.department.DepartmentTasks.signal(player,"sineva.passive.absorbed",event.getAmount());
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onHackclawMovementSound(PlayLevelSoundEvent.AtEntity event) {
        if (!(event.getEntity() instanceof Player player) || !HackclawStateManager.isHackclaw(player)) {
            return;
        }
        if (isMovementOrLandingSound(event.getSound().value().getLocation())) {
            event.setNewVolume(event.getOriginalVolume() * 0.4f);
        }
    }

    @SubscribeEvent
    public static void onMorsePlayerSound(PlayLevelSoundEvent.AtEntity event) {
        if (!(event.getEntity() instanceof ServerPlayer source)) {
            return;
        }
        MorseStateManager.onPlayerSound(source, source.position());
    }

    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.LOWEST)
    public static void recordOwnerAttack(net.minecraftforge.event.entity.living.LivingDamageEvent event) {
        if (event.getAmount() > 0 && event.getSource().getEntity() instanceof ServerPlayer owner
                && event.getEntity() != owner && !TeamCombatRules.shouldCancelFriendlyDamage(event.getEntity(), event.getSource()))
            owner.setLastHurtMob(event.getEntity());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingHurt(LivingHurtEvent event) {
        if (com.rzy.dealt_force_skills.character.rover.RoverStateManager.executingDeath(event.getEntity())
                || CorpsStateManager.isExecutingLoyaltyTarget(event.getEntity())) return;
        LivingEntity hurtEntity = event.getEntity();
        com.rzy.dealt_force_skills.department.DepartmentEquipment.resolved(hurtEntity,()->!event.isCanceled());
        DamageSource source = event.getSource();
        if (TeamCombatRules.shouldCancelFriendlyDamage(hurtEntity, source)) {
            event.setCanceled(true);
            event.setAmount(0.0f);
            return;
        }
        ShakehandsCombatHandler.handleLivingHurt(event);
        if (event.isCanceled() || event.getAmount() <= 0.0f) {
            return;
        }
        if (hackclawReflectingDamage) {
            return;
        }
        if (CorpsStateManager.tryConvertDuelAttack(source, hurtEntity)) {
            event.setAmount(0.0F);
            return;
        }
        if (damageSourceRootPlayer(source) instanceof ServerPlayer attacker
                && attacker == hurtEntity
                && CommanderStateManager.isCommander(attacker)) {
            // Commander-owned blasts/fields and inherited skills must never damage or
            // control the caster, including direct damage calls that bypass SkillDamageHelper.
            event.setCanceled(true);
            event.setAmount(0.0F);
            return;
        }
        if (SuperbWarfareCompat.isCommanderVehicle(hurtEntity,
                com.rzy.dealt_force_skills.DealtForceSkillsMod.MODID + ".commander_vehicle")) {
            if (!source.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION)) {
                event.setCanceled(true);
                event.setAmount(0.0F);
            } else {
                event.setAmount(event.getAmount() * 0.10F);
            }
            return;
        }
        if (hurtEntity instanceof ServerPlayer target
                && CommanderSupportManager.isAdsProtectedFor(target, damageSourceEntity(source), source)) {
            event.setCanceled(true);
            event.setAmount(0.0F);
            return;
        }
        if (hurtEntity instanceof ServerPlayer target
                && CommanderSupportManager.tryPreventFatal(target, source, event.getAmount())) {
            event.setAmount(0.0F);
            return;
        }
        event.setAmount(NoxStateManager.handleOutgoingDamage(source, event.getAmount()));
        if (event.getAmount() <= 0.0f) {
            return;
        }
        AsaraTrooperEntity asaraSource = AsaraTrooperEntity.sourceTrooper(source);
        if (asaraSource != null) {
            event.setAmount(event.getAmount() * asaraSource.outgoingDamageMultiplier());
        }
        if (source.getEntity() instanceof LivingEntity arenaAttacker) {
            float amount = event.getAmount() * HvkAsaraArenaManager.outgoingDamageMultiplier(
                    arenaAttacker, hurtEntity, source);
            event.setAmount(HvkAsaraArenaManager.capPlayerDamage(arenaAttacker, hurtEntity, source, amount));
        }
        if (SaeedStateManager.shouldCancelTeamDamage(hurtEntity, source)) {
            event.setCanceled(true);
            event.setAmount(0.0f);
            return;
        }
        if (hurtEntity instanceof Player targetPlayer
                && source.getEntity() instanceof Player attackerPlayer
                && GhrothStateManager.shouldCancelCeasefire(targetPlayer, attackerPlayer)) {
            event.setCanceled(true);
            event.setAmount(0.0F);
            return;
        }
        if (hurtEntity instanceof Player player && GhrothStateManager.isTacticalImmune(player)) {
            event.setCanceled(true);
            event.setAmount(0.0F);
            return;
        }
        if (hurtEntity instanceof Player player && event.getAmount() > 0.0F) {
            float ghrothMultiplier = GhrothStateManager.incomingDamageMultiplier(player, damageSourceEntity(source));
            if (ghrothMultiplier <= 0.0F) {
                event.setCanceled(true);
                event.setAmount(0.0F);
                return;
            }
            if (ghrothMultiplier < 1.0F) {
                event.setAmount(event.getAmount() * ghrothMultiplier);
            }
        }
        if (event.getAmount() > 0.0f) {
            float guardMultiplier = SaeedStateManager.guardOutgoingDamageMultiplier(source, hurtEntity);
            if (guardMultiplier != 1.0F) {
                event.setAmount(event.getAmount() * guardMultiplier);
            }
        }
        MobEffectInstance corrosion = activeEffectInstance(hurtEntity, ModEffects.CORROSION.get());
        if (corrosion != null && event.getAmount() > 0.0f) {
            int stacks = corrosion.getAmplifier() + 1;
            event.setAmount(event.getAmount() * (1.0f + stacks * 0.3f));
        }
        if (StingerStateManager.hasStimSuppression(hurtEntity)
                && effectApplies(hurtEntity, ModEffects.STINGER_STIM_SUPPRESSION.get())
                && event.getAmount() > 0.0f) {
            event.setAmount(event.getAmount() * 1.15f);
        }
        if (VlinderStateManager.hasMedicalWasteInterference(hurtEntity)
                && effectApplies(hurtEntity, ModEffects.VLINDER_MEDICAL_WASTE_INTERFERENCE.get())
                && event.getAmount() > 0.0f) {
            event.setAmount(event.getAmount() * 1.3f);
        }
        if (ModItemEffectHelper.hasLaughingManiaThree(hurtEntity) && event.getAmount() > 0.0f) {
            event.setAmount(event.getAmount() * 0.6f);
        }
        event.setAmount(LunaStateManager.applyWeakPointDamage(hurtEntity, event.getAmount()));
        LunaStateManager.handleDamageReveal(event);
        if (NoxStateManager.handleDecoyHurt(hurtEntity, event.getSource(), event.getAmount())) {
            event.setAmount(0.0f);
            return;
        }
        LexNinjiaStateManager.handleLivingHurt(event);

        if (event.getSource().getEntity() instanceof ServerPlayer attacker
                && event.getAmount() > 0.0f) {
            DepartmentOfTransportationStateManager.revealFromOffense(attacker);
        }

        if (event.getSource().getEntity() instanceof ServerPlayer attacker
                && ManbaStateManager.isManba(attacker)
                && event.getAmount() > 0.0f) {
            float beforeManba=event.getAmount();
            event.setAmount(beforeManba * ManbaStateManager.outgoingDamageMultiplier(attacker, hurtEntity));
            com.rzy.dealt_force_skills.department.DepartmentManba.reduced(hurtEntity,attacker,beforeManba,event.getAmount(),()->!event.isCanceled());
        }
        if (event.getSource().getEntity() instanceof ServerPlayer attacker
                && event.getAmount() > 0.0f
                && !event.getSource().is(SkillDamageHelper.TRUE_SKILL_DAMAGE)) {
            event.setAmount(event.getAmount() * equipmentOutgoingDamageMultiplier(attacker, hurtEntity, event.getSource()));
        }
        if (event.getAmount() > 0.0F) {
            if (damageSourceRootPlayer(source) instanceof ServerPlayer attacker
                    && CommanderStateManager.isCommander(attacker)) {
                if (!SkillDamageHelper.isApplyingCommanderSkillDamage()
                        && !commanderGeneConvertingDamage) {
                    event.setAmount(event.getAmount()
                            * CommanderStateManager.ordinaryDamageMultiplier(attacker));
                }
                if (!commanderGeneConvertingDamage) {
                    event.setAmount(event.getAmount()
                            * CommanderStateManager.criticalDamageMultiplier(attacker));
                }
            }
            if (CommanderSupportManager.isMarked(hurtEntity)) {
                event.setAmount(event.getAmount() * CommanderSupportManager.markedDamageMultiplier(hurtEntity));
            }
        }
        if (event.getSource().getEntity() instanceof ServerPlayer attacker
                && UndeadStateManager.isUndead(attacker)
                && event.getAmount() > 0.0f) {
            event.setAmount(UndeadStateManager.handleOutgoingHurt(
                    attacker, hurtEntity, event.getSource(), event.getAmount()));
        }
        if (event.getAmount() > 0.0f) {
            SaeedStateManager.ownerFromDamageEntity(source.getEntity())
                    .filter(attacker -> SaeedStateManager.shouldExecutePassiveTarget(attacker, hurtEntity))
                    .ifPresent(attacker -> event.setAmount(Math.max(
                            event.getAmount(), hurtEntity.getHealth() + hurtEntity.getAbsorptionAmount() + 1.0F)));
        }
        event.setAmount(NoxStateManager.capDeepWoundDamage(source, hurtEntity, event.getAmount()));
        if (event.getAmount() > 0.0F
                && damageSourceRootPlayer(source) instanceof ServerPlayer attacker
                && CommanderStateManager.hasBranchUpgrade(
                        attacker, CommanderBranchUpgrade.HACKCLAW_BACKSTAB_REPUTATION)
                && isPassiveOrNeutralMob(hurtEntity)
                && !TrainingTargetCompat.recordsDamageWithoutDying(hurtEntity)) {
            markHackclawBackstab(hurtEntity, attacker);
            event.setAmount(Math.max(event.getAmount(), hurtEntity.getMaxHealth() * 0.99F));
        }
        if (event.getAmount() > 0.0F
                && !SkillDamageHelper.isApplyingCommanderSkillDamage()
                && damageSourceRootPlayer(source) instanceof ServerPlayer attacker
                && CommanderStateManager.isCommander(attacker)) {
            CommanderStateManager.onOrdinaryDamage(attacker, hurtEntity);
        }

        if (hurtEntity instanceof SaeedGuardEntity guard && event.getAmount() > 0.0f) {
            float adjusted = handleSaeedGuardInheritedDefense(guard, source, event.getAmount());
            if (adjusted <= 0.0f) {
                event.setAmount(0.0f);
                return;
            }
            event.setAmount(adjusted);
        }

        if (!(hurtEntity instanceof Player player)) {
            if (event.getAmount() > 0.0f) {
                ManbaStateManager.addAffectionForDamagedTarget(hurtEntity, source, event.getAmount());
                NoxStateManager.tryApplyDelayedWound(source, hurtEntity, event.getAmount());
            }
            return;
        }

        if (player instanceof ServerPlayer serverPlayer && event.getAmount() > 0.0F
                && GamblerStateManager.isGambler(serverPlayer)) {
            float adjustedAmount = GamblerStateManager.handleIncomingHurt(
                    serverPlayer, source, event.getAmount());
            com.rzy.dealt_force_skills.department.DepartmentGambler.resolved(serverPlayer,()->!event.isCanceled()&&event.getAmount()==0.0F);
            if (adjustedAmount <= 0.0F) {
                event.setAmount(0.0F);
                return;
            }
            event.setAmount(adjustedAmount);
        }

        if (StingerStateManager.isDowned(player)
                && !StingerStateManager.isExecutingDownedDeath(player)
                && !ManbaStateManager.isExecutingDuelTarget(player)) {
            event.setAmount(0.0f);
            return;
        }

        if (VlinderStateManager.isDowned(player)
                && !VlinderStateManager.isExecutingDownedDeath(player)
                && !ManbaStateManager.isExecutingDuelTarget(player)) {
            event.setAmount(0.0f);
            return;
        }

        if (CatDadStateManager.isDowned(player)
                && !ManbaStateManager.isExecutingDuelTarget(player)) {
            event.setAmount(0.0f);
            return;
        }

        if (suppressesPotionDamage(player, source)) {
            event.setAmount(0.0f);
            return;
        }
        if (player instanceof ServerPlayer serverPlayer && CatDadStateManager.tryBlockDamage(serverPlayer, event.getAmount())) {
            event.setAmount(0.0f);
            return;
        }
        if (player instanceof ServerPlayer serverPlayer
                && DepartmentOfTransportationStateManager.isDepartment(player)
                && event.getAmount() > 0.0f) {
            float adjustedAmount = DepartmentOfTransportationStateManager.handleIncomingHurt(serverPlayer, source, event.getAmount());
            if (adjustedAmount <= 0.0f) {
                event.setAmount(0.0f);
                return;
            }
            event.setAmount(adjustedAmount);
        }
        if (player instanceof ServerPlayer serverPlayer
                && UndeadStateManager.isUndead(player)
                && event.getAmount() > 0.0f) {
            float adjustedAmount = UndeadStateManager.handleIncomingHurt(
                    serverPlayer, source, event.getAmount());
            if (adjustedAmount <= 0.0F) {
                event.setAmount(0.0F);
                return;
            }
            event.setAmount(adjustedAmount);
        }
        if (player instanceof ServerPlayer serverPlayer
                && NikaidouHiroWitchificationStateManager.isNikaidouHiroWitchification(player)
                && event.getAmount() > 0.0f) {
            float incomingAmount = event.getAmount();
            if (NikaidouHiroWitchificationStateManager.tryRewindFatalRescue(
                    serverPlayer, source, incomingAmount)) {
                event.setAmount(0.0f);
                return;
            }
            if (NikaidouHiroWitchificationStateManager.tryHandleFatalDamage(
                    serverPlayer, source, incomingAmount)) {
                event.setAmount(0.0f);
                return;
            }
            float adjustedAmount = NikaidouHiroWitchificationStateManager.adjustIncomingDamage(
                    serverPlayer, source, incomingAmount);
            if (adjustedAmount <= 0.0f) {
                event.setAmount(0.0f);
                return;
            }
            event.setAmount(adjustedAmount);
        }
        if (source.getEntity() instanceof ServerPlayer attacker
                && NikaidouHiroWitchificationStateManager.isNikaidouHiroWitchification(attacker)
                && event.getAmount() > 0.0f) {
            event.setAmount(NikaidouHiroWitchificationStateManager.handleOutgoingHurt(
                    attacker, hurtEntity, source, event.getAmount()));
        }
        if (player instanceof ServerPlayer serverPlayer
                && NikaidouHiroStateManager.isNikaidouHiro(player)
                && event.getAmount() > 0.0f) {
            float adjustedAmount = source.is(SkillDamageHelper.NIKAIDOU_DECAY)
                    ? event.getAmount()
                    : event.getAmount() * NikaidouHiroStateManager.incomingDamageMultiplier(player);
            if (adjustedAmount <= 0.0f) {
                event.setAmount(0.0f);
                return;
            }
            if (NikaidouHiroStateManager.shouldPreventFatalDamage(serverPlayer, source, adjustedAmount)) {
                event.setAmount(0.0f);
                return;
            }
            event.setAmount(adjustedAmount);
        }
        if (player instanceof ServerPlayer serverPlayer) {
            event.setAmount(CorpsStateManager.adjustIncomingDuelDamage(serverPlayer,event.getAmount()));
        }
        if (player instanceof ServerPlayer serverPlayer && ManbaStateManager.isManba(player)) {
            if (ManbaStateManager.shouldIgnoreIncomingDamage(player, source)
                    || ManbaStateManager.trySteelBody(serverPlayer, source, event.getAmount())) {
                event.setAmount(0.0f);
                return;
            }
            ManbaStateManager.tryTriggerDiversion(serverPlayer, source);
            if (event.getAmount() > 0.0f) {
                float beforeManba=event.getAmount();
                event.setAmount(beforeManba * ManbaStateManager.incomingDamageMultiplier(player, source));
                com.rzy.dealt_force_skills.department.DepartmentManba.reduced(hurtEntity,serverPlayer,beforeManba,event.getAmount(),()->!event.isCanceled());
            }
        }

        if (ShepherdStateManager.isShepherd(player) && isExplosionDamage(source)) {
            event.setAmount(event.getAmount() * 0.7f);
        }

        if (SinevaStateManager.isBombSuitActive(player)) {
            if (source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE)
                    || source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)
                    || source.is(net.minecraft.tags.DamageTypeTags.WITCH_RESISTANT_TO)) {
                event.setAmount(0.0f);
                return;
            }
        }

        Vec3 sourcePos = damageSourcePosition(source);
        if (sourcePos != null && tryAbsorbFrontShield(player, source, sourcePos, event.getAmount())) {
            if (event.getAmount() > 0.0f && isPlayerSourcedDamage(source)) {
                SinevaStateManager.recordPlayerShieldDamage(player);
            }
            event.setAmount(0.0f);
            return;
        }

        if (SinevaStateManager.isShieldDeployed(player) && event.getAmount() > 0.0f && isPlayerSourcedDamage(source)) {
            SinevaStateManager.recordShieldDamage(player);
        }

        if (SinevaStateManager.isBombSuitActive(player)) {
            event.setAmount(event.getAmount() * 0.5f);
        }

        if (player.hasEffect(ModEffects.STINGER_STIM_HEAL.get()) && event.getAmount() > 0.0f) {
            event.setAmount(event.getAmount() * 0.85f);
        }

        if (player instanceof ServerPlayer serverPlayer && event.getAmount() > 0.0f) {
            if (CommanderStateManager.isCommander(serverPlayer)) {
                if (CommanderStateManager.rollsDodge(serverPlayer)) {
                    event.setAmount(0.0F);
                    return;
                }
                event.setAmount(event.getAmount() * CommanderStateManager.incomingDamageMultiplier(serverPlayer));
                event.setAmount(CommanderStateManager.capIncomingDamage(serverPlayer, event.getAmount()));
                if (event.getAmount() <= 0.0F) {
                    return;
                }
            }
            event.setAmount(com.rzy.dealt_force_skills.character.wildfire.WildfireStateManager.defend(serverPlayer, source, event.getAmount()));
            float equipmentAdjusted = handleEquipmentDefense(serverPlayer, source, event.getAmount());
            if (equipmentAdjusted <= 0.0f) {
                event.setAmount(0.0f);
                return;
            }
            float allyAdjusted = tryAbsorbGlobalForcesAllyDamage(serverPlayer, source, equipmentAdjusted);
            if (allyAdjusted <= 0.0f) {
                event.setAmount(0.0f);
                return;
            }
            event.setAmount(allyAdjusted);
        }

        if (player instanceof ServerPlayer serverPlayer
                && TempestStateManager.isTempest(player)
                && event.getAmount() > 0.0f) {
            TempestStateManager.triggerExplosiveSpine(serverPlayer, ModSounds.TEMPEST_EXPLOSIVE_SPINE_ACTIVATE.get());
            if (TempestStateManager.shouldPreventFatalDamage(serverPlayer, event.getAmount())) {
                event.setAmount(0.0f);
                TempestStateManager.triggerEmergencyRecall(serverPlayer);
                return;
            }
        }

        // Hive technology smoke keeps protected players alive at one health. Apply
        // this before the generic Stinger downed interception so the smoke does not
        // accidentally convert lethal damage into a downed state.
        if (player instanceof ServerPlayer serverPlayer
                && StingerStateManager.isHiveSmokeActive(serverPlayer)
                && !event.getSource().is(SkillDamageHelper.NIKAIDOU_DECAY)
                && event.getAmount() > 0.0F) {
            event.setAmount(Math.min(event.getAmount(), Math.max(0.0F, serverPlayer.getHealth() - 1.0F)));
        }

        if (player instanceof ServerPlayer serverPlayer && StingerStateManager.shouldEnterDowned(serverPlayer, event.getAmount())) {
            event.setAmount(0.0f);
            StingerStateManager.enterDowned(serverPlayer);
            return;
        }

        if (player instanceof ServerPlayer serverPlayer && VlinderStateManager.shouldEnterDowned(serverPlayer, event.getAmount())) {
            event.setAmount(0.0f);
            VlinderStateManager.enterDowned(serverPlayer);
            return;
        }

        if (player instanceof ServerPlayer serverPlayer
                && CatDadStateManager.tryEnterFatalDowned(serverPlayer, source, event.getAmount())) {
            event.setAmount(0.0f);
            return;
        }

        if (player instanceof ServerPlayer serverPlayer
                && ManbaStateManager.tryPreventFatalDamage(serverPlayer, source, event.getAmount())) {
            event.setAmount(0.0f);
            return;
        }

        if (player instanceof ServerPlayer serverPlayer && event.getAmount() > 0.0f && ManbaStateManager.isManba(player)) {
            ManbaStateManager.recordDamageTaken(serverPlayer);
        }

        if (player instanceof ServerPlayer serverPlayer && event.getAmount() > 0.0f) {
            CatDadStateManager.tryReflectDamage(serverPlayer, source, event.getAmount());
            tryReflectHackclawExtremeHack(serverPlayer, source, event.getAmount());
        }

        if (event.getAmount() > 0.0f) {
            ManbaStateManager.addAffectionForDamagedTarget(hurtEntity, source, event.getAmount());
            NoxStateManager.tryApplyDelayedWound(source, hurtEntity, event.getAmount());
        }

    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingHurtGamblerPostFinalDamage(LivingHurtEvent event) {
        LivingEntity target = event.getEntity();
        if (!target.level().isClientSide && event.getAmount() > 0.0F) {
            if (damageSourceRootPlayer(event.getSource()) instanceof ServerPlayer attacker) {
                GamblerStateManager.handlePostFinalDamage(attacker, target, event.getSource(), event.getAmount());
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingHurtGhrothDamageFloor(LivingHurtEvent event) {
        LivingEntity target = event.getEntity();
        GhrothDamageFloor floor = GHROTH_DAMAGE_FLOORS.get(target.getUUID());
        if (floor == null) {
            return;
        }
        long now = target.level().getGameTime();
        if (now > floor.expiresAt()) {
            GHROTH_DAMAGE_FLOORS.remove(target.getUUID());
            return;
        }
        if (!(damageSourceRootPlayer(event.getSource()) instanceof ServerPlayer attacker)
                || !floor.attackerId().equals(attacker.getUUID())) {
            return;
        }
        if (event.getAmount() <= 0.0F) {
            return;
        }
        GHROTH_DAMAGE_FLOORS.remove(target.getUUID());
        if (event.getAmount() < floor.amount()) {
            event.setAmount(floor.amount());
        }
        if (floor.starsBonus() && target.level().getServer() != null) {
            float starsDamage = event.getAmount();
            consumeGhrothStarsPendingShot(attacker);
            target.level().getServer().execute(
                    () -> GhrothStateManager.handleStarsTaczHit(attacker, target, starsDamage));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingHurtGhrothNoonDamageCopy(LivingHurtEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide || event.getAmount() <= 0.0F || ghrothNoonCopyingDamage) {
            return;
        }
        if (!(damageSourceRootPlayer(event.getSource()) instanceof ServerPlayer attacker)
                || attacker == target
                || !GhrothStateManager.isGhroth(attacker)) {
            return;
        }
        if (target instanceof Player targetPlayer && GhrothStateManager.shouldCancelCeasefire(attacker, targetPlayer)) {
            return;
        }
        float damage = event.getAmount();
        if(Float.isFinite(damage)&&damage>0)
            com.rzy.dealt_force_skills.department.DepartmentGhroth.noon(target,attacker,
                    ()->!event.isCanceled()&&event.getAmount()>0,
                    copies->startGhrothNoonDamageCopies(attacker,target,event.getAmount(),copies));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerTickGhrothNoonDamageCopies(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        tickGhrothNoonDamageCopyQueue(player);
    }

    private static void startGhrothNoonDamageCopies(ServerPlayer attacker, LivingEntity target, float damage, int copies) {
        int remaining = Math.max(0, copies);
        if (GhrothStateManager.tryMarkNoonDamageCopyTick(attacker)) {
            applyGhrothNoonDamageCopy(attacker, target, damage);
            remaining--;
        }
        if (remaining > 0) {
            GHROTH_NOON_PENDING_COPIES
                    .computeIfAbsent(attacker.getUUID(), ignored -> new LinkedList<>())
                    .add(new GhrothNoonPendingCopy(target.getId(), damage, remaining));
        }
    }

    private static void tickGhrothNoonDamageCopyQueue(ServerPlayer attacker) {
        LinkedList<GhrothNoonPendingCopy> queue = GHROTH_NOON_PENDING_COPIES.get(attacker.getUUID());
        if (queue == null) {
            return;
        }
        while (!queue.isEmpty()) {
            GhrothNoonPendingCopy pending = queue.peek();
            Entity entity = attacker.level().getEntity(pending.targetId());
            if (!(entity instanceof LivingEntity target)
                    || target == attacker
                    || !target.isAlive()
                    || target.isSpectator()
                    || (target instanceof Player targetPlayer && GhrothStateManager.shouldCancelCeasefire(attacker, targetPlayer))) {
                queue.poll();
                continue;
            }
            if (!GhrothStateManager.tryMarkNoonDamageCopyTick(attacker)) {
                return;
            }
            applyGhrothNoonDamageCopy(attacker, target, pending.damage());
            queue.poll();
            if (pending.copiesRemaining() > 1) {
                queue.addFirst(new GhrothNoonPendingCopy(pending.targetId(), pending.damage(), pending.copiesRemaining() - 1));
            }
            break;
        }
        if (queue.isEmpty()) {
            GHROTH_NOON_PENDING_COPIES.remove(attacker.getUUID());
        }
    }

    private static void applyGhrothNoonDamageCopy(ServerPlayer attacker, LivingEntity target, float damage) {
        target.invulnerableTime = 0;
        ghrothNoonCopyingDamage = true;
        try {
            SkillDamageHelper.hurtUnscaled(target,
                    SkillDamageHelper.trueDamage(attacker.serverLevel(), attacker, attacker),
                    damage);
        } finally {
            ghrothNoonCopyingDamage = false;
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingHeal(LivingHealEvent event) {
        if (event.getEntity().hasEffect(ModEffects.NOX_DELAYED_WOUND.get())) {
            com.rzy.dealt_force_skills.department.DepartmentHealing.prevented(event.getEntity(),event.getAmount());
            event.setCanceled(true);
            return;
        }
        if (StingerStateManager.hasStimSuppression(event.getEntity())) {
            event.setCanceled(true);
            return;
        }
        if (VlinderStateManager.hasMedicalWasteInterference(event.getEntity())) {
            event.setCanceled(true);
            return;
        }
        if (NikaidouHiroWitchificationStateManager.shouldCancelHealing(event.getEntity())) {
            event.setCanceled(true);
            return;
        }
        if (event.getEntity() instanceof ServerPlayer player
                && UndeadStateManager.isUndead(player)) {
            event.setAmount(event.getAmount() * UndeadUpgradeManager.healingMultiplier(player));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingUseItemFinish(LivingEntityUseItemEvent.Finish event) {
        LivingEntity entity = event.getEntity();
        if (!entity.level().isClientSide && entity.hasEffect(ModEffects.NOX_DELAYED_WOUND.get())) {
            NoxDelayedWoundEffect.enforceCaps(entity);
        }
        if (!entity.level().isClientSide) {
            ManbaStateManager.onUseItemFinished(entity);
            LexNinjiaStateManager.handleFoodFinished(entity, event.getItem());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingUseItemStart(LivingEntityUseItemEvent.Start event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) {
            return;
        }
        if (entity instanceof ServerPlayer player) {
            MorseStateManager.recordPlayerAction(player);
            LexNinjiaStateManager.onPlayerAction(player);
        }
        if (entity instanceof Player player
                && (TempestStateManager.isActionLocked(player)
                || CatDadStateManager.isDowned(player)
                || NikaidouHiroStateManager.isWeaponActive(player)
                || DepartmentOfTransportationStateManager.hasEquippedTool(player)
                || UndeadStateManager.isActionLocked(player)
                || UndeadStateManager.isRitualDancing(player))) {
            boolean tempestDisarmed = effectApplies(player, ModEffects.TEMPEST_DISARMED.get());
            if (tempestDisarmed) {
                cancelBlockedUseItem(event, player);
            } else {
                stopBlockedUseItem(event, player);
            }
            if (tempestDisarmed && player instanceof ServerPlayer serverPlayer) {
                cooldownTempestDisarmedHands(serverPlayer);
                cancelOptionalTaczReload(serverPlayer);
            }
            return;
        }
        if (shouldAdjustUseDuration(event.getItem())) {
            double multiplier = ManbaStateManager.behaviorSpeedMultiplier(entity)
                    * ToxikStateManager.behaviorSpeedMultiplier(entity)
                    * ModItemEffectHelper.medicineUseSpeedMultiplier(entity)
                    * equipmentActionSpeedMultiplier(entity)
                    * UndeadUpgradeManager.interactionSpeedMultiplier(entity instanceof Player player ? player : null)
                    * UndeadStateManager.frozenInteractionMultiplier(entity);
            com.rzy.dealt_force_skills.department.DepartmentUseSpeed.start(entity,event.getItem(),event.getDuration(),multiplier);
            multiplier = normalizedUseSpeedMultiplier(multiplier);
            if (multiplier > 0.0D && Math.abs(multiplier - 1.0D) > 0.0001D) {
                event.setDuration(Math.max(1, (int) Math.ceil(event.getDuration() / multiplier)));
            }
        }
    }

    private static void stopBlockedUseItem(LivingEntityUseItemEvent event, LivingEntity entity) {
        entity.stopUsingItem();
        event.setDuration(0);
    }

    private static void cancelBlockedUseItem(LivingEntityUseItemEvent event, LivingEntity entity) {
        stopBlockedUseItem(event, entity);
        if (event.isCancelable()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (TempestStateManager.isActionLocked(event.getEntity())
                || CatDadStateManager.isDowned(event.getEntity())
                || NikaidouHiroStateManager.isWeaponActive(event.getEntity())
                || DepartmentOfTransportationStateManager.hasEquippedTool(event.getEntity())) {
            event.setNewSpeed(0.0F);
            return;
        }
        double multiplier = ToxikStateManager.behaviorSpeedMultiplier(event.getEntity())
                * ModItemEffectHelper.medicineBreakSpeedMultiplier(event.getEntity())
                * equipmentActionSpeedMultiplier(event.getEntity())
                * UndeadUpgradeManager.interactionSpeedMultiplier(event.getEntity())
                * UndeadStateManager.frozenInteractionMultiplier(event.getEntity());
        if (multiplier > 0.0D && Math.abs(multiplier - 1.0D) > 0.0001D) {
            event.setNewSpeed((float) (event.getNewSpeed() * multiplier));
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onItemToss(ItemTossEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player
                && LexNinjiaStateManager.shouldPreventHandGuardToss(player, event.getEntity().getItem())) {
            event.setCanceled(true);
            return;
        }
        if (TempestStateManager.isActionLocked(event.getPlayer())
                || CatDadStateManager.isDowned(event.getPlayer())
                || NikaidouHiroStateManager.isWeaponActive(event.getPlayer())
                || DepartmentOfTransportationStateManager.hasEquippedTool(event.getPlayer())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingTickNTwoColdDecay(LivingEvent.LivingTickEvent event) {
        if (!event.getEntity().level().isClientSide) {
            com.rzy.dealt_force_skills.character.ntwo.NTwoStateManager.tickColdDecay(event.getEntity());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingTickDelayedWound(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (!entity.level().isClientSide && entity.hasEffect(ModEffects.NOX_DELAYED_WOUND.get())) {
            NoxDelayedWoundEffect.enforceCaps(entity);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingTickNoxFlashedAi(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (!entity.level().isClientSide && entity.hasEffect(ModEffects.NOX_FLASHED.get())) {
            NoxFlashedEffect.suppressHostileTargeting(entity);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingTickMorseFlashedAi(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (!entity.level().isClientSide && entity.hasEffect(ModEffects.MORSE_FLASH_BLIND.get())) {
            MorseFlashedEffect.suppressHostileTargeting(entity);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingTickManbaBlindedAi(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (!entity.level().isClientSide && entity.hasEffect(ModEffects.MANBA_BLINDED.get())) {
            ManbaBlindedEffect.suppressHostileTargeting(entity);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingTickHackclawFlashBlindAi(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (!entity.level().isClientSide && entity.hasEffect(ModEffects.HACKCLAW_FLASH_BLIND.get())) {
            HackclawFlashBlindEffect.applySuppression(entity);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingTickToxikTearGasBlindAi(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (!entity.level().isClientSide && entity.hasEffect(ModEffects.TOXIK_TEAR_GAS_BLIND.get())) {
            ToxikTearGasBlindEffect.suppressHostileTargeting(entity);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingTickManbaDuelTaunt(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (!entity.level().isClientSide) {
            ManbaStateManager.enforceDuelTaunt(entity);
            DepartmentOfTransportationStateManager.suppressOwnedSummonTarget(entity);
            UndeadSupportManager.tickOwnedSummon(entity);
            UndeadStateManager.tickExternalEffects(entity);
            NikaidouHiroWitchificationStateManager.tickExternalEffects(entity);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerTickDelayedWound(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Player player = event.player;
        if (!player.level().isClientSide && player.hasEffect(ModEffects.NOX_DELAYED_WOUND.get())) {
            NoxDelayedWoundEffect.enforceCaps(player);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLevelTickDelayedWound(TickEvent.LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.level instanceof ServerLevel serverLevel) {
            NoxDelayedWoundEffect.enforceTracked(serverLevel);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onCharacterHitFeedback(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide || event.getAmount() <= 0.0f) {
            return;
        }

        Entity attacker = event.getSource().getEntity();
        if (!(attacker instanceof ServerPlayer player)
                || attacker == event.getEntity()
                || !CharacterSelectionManager.hasSelectedCharacter(player)) {
            return;
        }

        NetworkHandler.sendToPlayer(new S2C_CharacterHitFeedback(event.getEntity().getId(), event.getAmount()), player);
    }

    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        if (event.getEntity() instanceof Player player
                && UndeadStateManager.isUndead(player)
                && UndeadStateManager.profession(player) == UndeadProfession.HUNTER) {
            event.setDamageMultiplier(0.0F);
            event.setCanceled(true);
            return;
        }
        if (event.getEntity() instanceof Player player && UluruStateManager.isUluru(player)) {
            event.setDamageMultiplier(event.getDamageMultiplier() * 0.64f);
        }
        if (event.getEntity() instanceof Player player && VyronStateManager.isVyron(player)) {
            boolean propulsionMod = player instanceof ServerPlayer serverPlayer
                    && CommanderStateManager.hasBranchUpgrade(serverPlayer,
                    CommanderBranchUpgrade.VYRON_PROPULSION_MOD);
            if (propulsionMod) {
                event.setDamageMultiplier(0.0F);
                event.setCanceled(true);
            } else {
                event.setDamageMultiplier(event.getDamageMultiplier() * 0.64f);
            }
            if ((propulsionMod || event.getDistance() > 3.5f) && player instanceof ServerPlayer serverPlayer) {
                VyronStateManager.grantPowered(serverPlayer, true);
                VyronStateManager.syncToClient(serverPlayer);
            }
        }
        if (event.getEntity() instanceof ServerPlayer player && TempestStateManager.consumeFallProtection(player)) {
            event.setDamageMultiplier(0.0f);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMobEffectApplicable(MobEffectEvent.Applicable event) {
        MobEffectInstance effect = event.getEffectInstance();
        if (com.rzy.dealt_force_skills.character.rover.RoverStateManager.isProtectedEffect(effect.getEffect())) return;
        if (event.getEntity() instanceof ServerPlayer player
                && com.rzy.dealt_force_skills.item.PortableAdsDeviceItem.isActive(player)) {
            event.setResult(Event.Result.DENY);
            return;
        }
        if (event.getEntity() instanceof ServerPlayer player
                && CommanderSupportManager.isAdsEffectProtected(player)) {
            event.setResult(Event.Result.DENY);
            return;
        }
        if (event.getEntity() instanceof Player player
                && CorpsStateManager.shouldDenyIncomingEffect(player,effect.getEffect())) {
            event.setResult(Event.Result.DENY);
            return;
        }
        // Team rules: deny teammate debuffs / enemy buffs before the effect is applied.
        // Note: Applicable has no effect-source API in all Forge versions; Added still strips
        // with source. When source is unavailable here, only same-team harmful self-checks
        // cannot run — source-aware blocking remains in onMobEffectAddedTeamRules.
        if (event.getEntity() instanceof Player player
                && (SinevaStateManager.isBombSuitActive(player) || DWolfStateManager.isOverloadActive(player))
                && effect.getEffect().getCategory() == MobEffectCategory.HARMFUL) {
            event.setResult(Event.Result.DENY);
        }
        if (event.getEntity() instanceof Player player
                && player.hasEffect(ModEffects.STINGER_STIM_HEAL.get())
                && effect.getEffect().getCategory() == MobEffectCategory.HARMFUL) {
            event.setResult(Event.Result.DENY);
        }
        if (event.getEntity() instanceof Player player
                && player.hasEffect(ModEffects.TOXIK_ADRENALINE.get())
                && effect.getEffect().getCategory() == MobEffectCategory.HARMFUL) {
            event.setResult(Event.Result.DENY);
        }
        if (event.getEntity() instanceof Player player
                && player.hasEffect(ModEffects.SEDATION.get())
                && effect.getEffect().getCategory() == MobEffectCategory.HARMFUL) {
            event.setResult(Event.Result.DENY);
        }
        if (ModItemEffectHelper.shouldSuppressHarmful(event.getEntity(), effect)) {
            if (event.getEntity() instanceof Player player) {
                ModItemEffectHelper.suppressIncomingHarmful(player, effect);
            }
        }
        if (event.getEntity() instanceof Player player
                && player.hasEffect(ModEffects.VLINDER_HEALING_DUST.get())
                && effect.getEffect().getCategory() == MobEffectCategory.HARMFUL) {
            event.setResult(Event.Result.DENY);
        }
        if (event.getEntity() instanceof ServerPlayer player
                && effect.getEffect().getCategory() == MobEffectCategory.HARMFUL
                && CatDadStateManager.tryBlockIncoming(player)) {
            event.setResult(Event.Result.DENY);
        }
        if (NikaidouHiroWitchificationStateManager.shouldDenyHealingEffect(
                event.getEntity(), effect.getEffect())) {
            event.setResult(Event.Result.DENY);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMobEffectAddedTeamRules(MobEffectEvent.Added event) {
        if (event.getEffectInstance().getEffect() == ModEffects.ROVER_DOWNED.get()) return;
        if (event.getEntity().level().isClientSide) {
            return;
        }
        if (event.getEntity() instanceof ServerPlayer player
                && com.rzy.dealt_force_skills.item.PortableAdsDeviceItem.isActive(player)) {
            player.removeEffect(event.getEffectInstance().getEffect());
            return;
        }
        if (event.getEntity() instanceof ServerPlayer player
                && CommanderSupportManager.isAdsEffectProtected(player)) {
            player.removeEffect(event.getEffectInstance().getEffect());
            return;
        }
        if (event.getEntity() instanceof ServerPlayer player
                && CommanderStateManager.isCommander(player)
                && event.getEffectSource() == player
                && event.getEffectInstance().getEffect().getCategory() == MobEffectCategory.HARMFUL) {
            player.removeEffect(event.getEffectInstance().getEffect());
            return;
        }
        if (TeamCombatRules.shouldBlockPlayerEffect(
                event.getEntity(), event.getEffectSource(), event.getEffectInstance())) {
            MobEffect effect = event.getEffectInstance().getEffect();
            // Effect is already applied when Added fires; strip it immediately.
            event.getEntity().removeEffect(effect);
            return;
        }
        if (ShakehandsCombatHandler.handleEffectAdded(event)) {
            return;
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onMobEffectAdded(MobEffectEvent.Added event) {
        if (com.rzy.dealt_force_skills.character.rover.RoverStateManager.isProtectedEffect(event.getEffectInstance().getEffect())) return;
        if (event.getEntity() instanceof ServerPlayer player
                && com.rzy.dealt_force_skills.item.PortableAdsDeviceItem.isActive(player)) {
            player.removeEffect(event.getEffectInstance().getEffect());
            return;
        }
        if (event.getEntity() instanceof ServerPlayer player
                && CommanderSupportManager.isAdsEffectProtected(player)) {
            player.removeEffect(event.getEffectInstance().getEffect());
            return;
        }
        if (NikaidouHiroWitchificationStateManager.shouldDenyHealingEffect(
                event.getEntity(), event.getEffectInstance().getEffect())) {
            event.getEntity().removeEffect(event.getEffectInstance().getEffect());
            return;
        }
        boolean incomingIsActive = event.getEntity().getEffect(event.getEffectInstance().getEffect()) == event.getEffectInstance();
        if (!incomingIsActive) com.rzy.dealt_force_skills.character.ntwo.NTwoStateManager.boostNativeAddedHarmfulEffect(
                event.getEntity(), event.getEffectInstance());
        boolean rescaled = !incomingIsActive && !event.getEntity().level().isClientSide
                && ToxikStateManager.tryRescaleAddedEffect(
                event.getEntity(), event.getEffectInstance(), event.getEffectSource());
        if (!incomingIsActive && !rescaled && !event.getEntity().level().isClientSide) {
            rescaled = UndeadUpgradeManager.tryRescaleAddedEffect(
                    event.getEntity(), event.getEffectInstance(), event.getEffectSource());
        }
        if (!incomingIsActive && !rescaled && !event.getEntity().level().isClientSide) {
            ManbaStateManager.addAffectionForControlledTarget(event.getEntity(), event.getEffectInstance());
        }
        if (!event.getEntity().level().isClientSide
                && (event.getEffectInstance().getEffect() == ModEffects.FATIGUE_REMOVAL.get()
                || event.getEffectInstance().getEffect() == ModEffects.SEDATION.get())) {
            ModItemEffectHelper.removeActiveHarmfulEffects(event.getEntity());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onMobEffectRemove(MobEffectEvent.Remove event) {
        if (event.getEffect() == ModEffects.NOX_DELAYED_WOUND.get()) {
            NoxDelayedWoundEffect.clearCaps(event.getEntity());
        } else if (event.getEffect() == ModEffects.TOXIK_FIREFLY_INTERFERENCE.get()) {
            ToxikFireflyInterferenceEffect.clearCaps(event.getEntity());
        } else if (event.getEffect() == ModEffects.MANBA_BLINDED.get()) {
            ManbaStateManager.restoreDuelTauntAfterBlind(event.getEntity());
        } else if (event.getEffect() == ModEffects.MORSE_FLASH_BLIND.get()) {
            MorseFlashedEffect.restoreMobAi(event.getEntity());
        } else if (event.getEffect() == ModEffects.PAIN_RELIEF.get()) {
            ModItemEffectHelper.restorePainReliefSuppression(event.getEntity());
        }
    }

    @SubscribeEvent
    public static void onMobEffectExpired(MobEffectEvent.Expired event) {
        MobEffectInstance effect = event.getEffectInstance();
        if (effect != null && effect.getEffect() == ModEffects.NOX_DELAYED_WOUND.get()) {
            NoxDelayedWoundEffect.clearCaps(event.getEntity());
        } else if (effect != null && effect.getEffect() == ModEffects.TOXIK_FIREFLY_INTERFERENCE.get()) {
            ToxikFireflyInterferenceEffect.clearCaps(event.getEntity());
        } else if (effect != null && effect.getEffect() == ModEffects.MANBA_BLINDED.get()) {
            ManbaStateManager.restoreDuelTauntAfterBlind(event.getEntity());
        } else if (effect != null && effect.getEffect() == ModEffects.MORSE_FLASH_BLIND.get()) {
            MorseFlashedEffect.restoreMobAi(event.getEntity());
        } else if (effect != null && effect.getEffect() == ModEffects.PAIN_RELIEF.get()) {
            ModItemEffectHelper.restorePainReliefSuppression(event.getEntity());
        }
    }


    @SubscribeEvent
    public static void onPlayerTickUluruRestoreTerrain(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) {
            return;
        }
        if (event.player instanceof ServerPlayer serverPlayer) {
            UluruLoiteringMissileEntity.tickRestoreTerrain(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerTickSuitSlow(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player player = event.player;
        if (player.level().isClientSide) return;

        var attr = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attr == null) return;

        boolean shouldSlow = SinevaStateManager.isShieldDeployed(player);
        AttributeModifier existing = attr.getModifier(SinevaStateManager.BOMB_SUIT_SLOW_UUID);

        double damageSlow = shouldSlow ? SinevaStateManager.shieldDamageSlow(player) : 0.0D;
        double targetSlowAmount = -Math.min(SHIELD_DAMAGE_SLOW_MAX_TOTAL,
                Math.max(0.0D, -SHIELD_DEPLOYED_SLOW_AMOUNT) + damageSlow);
        if (damageSlow <= 0.0D && shouldSlow) {
            SinevaStateManager.clearShieldDamageSlow(player);
        }

        if (shouldSlow && (existing == null || Math.abs(existing.getAmount() - targetSlowAmount) > 0.0001)) {
            if (existing != null) {
                attr.removeModifier(SinevaStateManager.BOMB_SUIT_SLOW_UUID);
            }
            attr.addTransientModifier(new AttributeModifier(
                    SinevaStateManager.BOMB_SUIT_SLOW_UUID, "shield_deployed_slow", targetSlowAmount, AttributeModifier.Operation.MULTIPLY_TOTAL
            ));
        }
        if (!shouldSlow && existing != null) {
            attr.removeModifier(SinevaStateManager.BOMB_SUIT_SLOW_UUID);
        }
        if (!shouldSlow && SinevaStateManager.isSineva(player)) {
            SinevaStateManager.clearShieldDamageSlow(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerTickDfsEquipment(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) {
            return;
        }
        if (event.player instanceof ServerPlayer player) {
            HaffCoinManager.awardSurvivalMinute(player);
            UndeadSoulManager.awardSurvivalMinute(player);
            LexNinjiaCurrencyManager.awardSurvivalMinute(player);
            EmptyHandExtinguishHandler.tick(player);
            tickDfsEquipment(player);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingHurtHiveSmokeFloor(LivingHurtEvent event) {
        if (com.rzy.dealt_force_skills.character.rover.RoverStateManager.executingDeath(event.getEntity())
                || CorpsStateManager.isExecutingLoyaltyTarget(event.getEntity())) return;
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !StingerStateManager.isHiveSmokeActive(player)
                || event.getSource().is(SkillDamageHelper.NIKAIDOU_DECAY)
                || event.getAmount() <= 0.0F) {
            return;
        }
        float allowed = Math.max(0.0F, player.getHealth() - 1.0F);
        if (event.getAmount() > allowed) {
            event.setAmount(allowed);
        }
    }

    @SubscribeEvent
    public static void onPlayerTickEternalLoveBlessing(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player) {
            EternalLoveBlessingItem.tickInventoryEffects(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerTickSinevaEffectImmunity(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player player = event.player;
        if (player.level().isClientSide
                || (!SinevaStateManager.isBombSuitActive(player) && !DWolfStateManager.isOverloadActive(player))) {
            return;
        }

        for (MobEffectInstance effect : new ArrayList<>(player.getActiveEffects())) {
            if (effect.getEffect().getCategory() == MobEffectCategory.HARMFUL) {
                player.removeEffect(effect.getEffect());
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (!com.rzy.dealt_force_skills.character.rover.RoverStateManager.executingDeath(entity)
                && !CorpsStateManager.isExecutingLoyaltyTarget(entity)) {
        if (entity instanceof ServerPlayer target
                && CommanderSupportManager.tryPreventFatal(target, event.getSource(), Float.MAX_VALUE)) {
            event.setCanceled(true);
            return;
        }
        if (entity.level() instanceof ServerLevel serverLevel
                && DepartmentOfTransportationStateManager.isDepartmentChargedCreeper(entity)) {
            DepartmentOfTransportationStateManager.triggerChargedCreeperDeathExplosion(serverLevel, entity);
        }
        if (entity instanceof ServerPlayer protectedPlayer
                && NikaidouHiroWitchificationStateManager.tryCancelDeath(protectedPlayer, event.getSource())) {
            event.setCanceled(true);
            return;
        }
        if (entity instanceof ServerPlayer protectedPlayer
                && GamblerStateManager.shouldCancelDeath(protectedPlayer)) {
            event.setCanceled(true);
            protectedPlayer.setHealth(Math.max(1.0F, protectedPlayer.getHealth()));
            return;
        }
        if (entity instanceof ServerPlayer protectedPlayer
                && StingerStateManager.isHiveSmokeActive(protectedPlayer)
                && !event.getSource().is(SkillDamageHelper.NIKAIDOU_DECAY)) {
            event.setCanceled(true);
            protectedPlayer.setHealth(Math.max(1.0F, protectedPlayer.getHealth()));
            return;
        }
        if (entity instanceof ServerPlayer protectedPlayer
                && !StingerStateManager.isExecutingDownedDeath(protectedPlayer)
                && !VlinderStateManager.isExecutingDownedDeath(protectedPlayer)
                && !ManbaStateManager.isExecutingDuelTarget(protectedPlayer)) {
            if (StingerStateManager.isDowned(protectedPlayer)) {
                event.setCanceled(true);
                protectedPlayer.setHealth(Math.max(1.0f, protectedPlayer.getHealth()));
                return;
            }
            if (VlinderStateManager.isDowned(protectedPlayer)) {
                event.setCanceled(true);
                protectedPlayer.setHealth(Math.max(1.0f, protectedPlayer.getHealth()));
                return;
            }
            if (CatDadStateManager.shouldCancelDeath(protectedPlayer)) {
                event.setCanceled(true);
                protectedPlayer.setHealth(Math.max(1.0f, protectedPlayer.getHealth()));
                return;
            }
            if (StingerStateManager.canEnterDowned(protectedPlayer)) {
                event.setCanceled(true);
                StingerStateManager.enterDowned(protectedPlayer);
                return;
            }
            if (VlinderStateManager.canEnterDowned(protectedPlayer)) {
                event.setCanceled(true);
                VlinderStateManager.enterDowned(protectedPlayer);
                return;
            }
            if (CatDadStateManager.tryEnterFatalDowned(protectedPlayer, event.getSource())) {
                event.setCanceled(true);
                return;
            }
            recordRedOwlRevengeOnDeath(protectedPlayer, event.getSource());
        }
        // Clear control effects on death so players can click Respawn and move after respawn.
        }
        entity.removeEffect(ModEffects.STUN.get());
        entity.removeEffect(ModEffects.WEBBED.get());
        entity.removeEffect(ModEffects.CORROSION.get());
        entity.removeEffect(ModEffects.SONIC_SHOCK.get());
        entity.removeEffect(ModEffects.STINGER_DOWNED.get());
        entity.removeEffect(ModEffects.STINGER_STIM_HEAL.get());
        entity.removeEffect(ModEffects.STINGER_STIM_SUPPRESSION.get());
        entity.removeEffect(ModEffects.STINGER_SMOKE_REGEN.get());
        entity.removeEffect(ModEffects.NOX_DELAYED_WOUND.get());
        NoxDelayedWoundEffect.clearCaps(entity);
        entity.removeEffect(ModEffects.NOX_CRIPPLED.get());
        entity.removeEffect(ModEffects.NOX_FLASHED.get());
        entity.removeEffect(ModEffects.HACKCLAW_FLASH_BLIND.get());
        entity.removeEffect(ModEffects.NOX_STEALTH.get());
        entity.removeEffect(ModEffects.MANBA_BLINDED.get());
        MorseFlashedEffect.restoreMobAi(entity);
        entity.removeEffect(ModEffects.NIKAIDOU_RIFT_STACKS.get());
        entity.removeEffect(ModEffects.NIKAIDOU_CORRECTION.get());
        entity.removeEffect(ModEffects.NIKAIDOU_CORE.get());
        entity.removeEffect(ModEffects.NIKAIDOU_DOOMED.get());
        entity.removeEffect(ModEffects.CATDAD_HISS_SLOW.get());
        entity.removeEffect(ModEffects.CATDAD_ARMOR_REDUCED.get());
        CorpsStateManager.onLivingDeath(entity);
        entity.removeEffect(ModEffects.CATDAD_DOWNED.get());
        entity.removeEffect(ModEffects.DEPARTMENT_CALIBRATION.get());
        entity.removeEffect(ModEffects.DEPARTMENT_VULNERABLE.get());
        entity.removeEffect(ModEffects.DEPARTMENT_CONCEALMENT.get());
        entity.removeEffect(ModEffects.MORSE_STRONG_SHOCK.get());
        entity.removeEffect(ModEffects.MORSE_FLASH_BLIND.get());
        entity.removeEffect(ModEffects.MORSE_SONAR_REVEALED.get());
        entity.removeEffect(ModEffects.PAIN_RELIEF.get());
        entity.removeEffect(ModEffects.ITEM_WEAKNESS.get());
        entity.removeEffect(ModEffects.HELA.get());
        entity.removeEffect(ModEffects.LAUGHING_MANIA_I.get());
        entity.removeEffect(ModEffects.LAUGHING_MANIA_II.get());
        entity.removeEffect(ModEffects.LAUGHING_MANIA_III.get());
        entity.removeEffect(ModEffects.TOXIK_ADRENALINE.get());
        entity.removeEffect(ModEffects.TOXIK_TEAR_GAS_BLIND.get());
        ToxikFireflyInterferenceEffect.clearCaps(entity);
        entity.removeEffect(ModEffects.TOXIK_FIREFLY_INTERFERENCE.get());
        entity.removeEffect(ModEffects.RAPTOR_ACTION_PAUSE.get());
        entity.removeEffect(ModEffects.RAPTOR_ELECTROMAGNETIC_INTERFERENCE.get());
        entity.removeEffect(ModEffects.RAPTOR_HUMMINGBIRD_MARKED.get());
        entity.removeEffect(ModEffects.TEMPEST_EXPLOSIVE_SPINE.get());
        entity.removeEffect(ModEffects.TEMPEST_DISARMED.get());
        entity.removeEffect(ModEffects.TEMPEST_EMERGENCY_DOWNED.get());
        entity.removeEffect(ModEffects.VLINDER_HEALING_DUST.get());
        entity.removeEffect(ModEffects.VLINDER_MEDICAL_WASTE_INTERFERENCE.get());
        entity.removeEffect(ModEffects.VLINDER_VITAL_DOWNED.get());
        entity.removeEffect(ModEffects.VLINDER_RESCUE_PROTECTION.get());
        entity.removeEffect(ModEffects.VLINDER_PLASMA_INJECTED.get());
        if (entity instanceof ServerPlayer serverPlayer) {
            InjuryManager.clearAll(serverPlayer);
            for (MobEffectInstance instance : new ArrayList<>(serverPlayer.getActiveEffects())) {
                ResourceLocation id = ForgeRegistries.MOB_EFFECTS.getKey(instance.getEffect());
                if (id != null && DealtForceSkillsMod.MODID.equals(id.getNamespace())) {
                    serverPlayer.removeEffect(instance.getEffect());
                }
            }
            SinevaSkills.clearForPlayer(serverPlayer);
            SinevaKnockdownState.clear(serverPlayer);
            UluruLoiteringMissileEntity.stopPlayerControl(serverPlayer);
            RaptorFalconDroneEntity.stopPlayerControl(serverPlayer);
            GizmoStateManager.clearWebControl(serverPlayer);
            NoxStateManager.clearRuntimeOnDeath(serverPlayer);
            StingerStateManager.clearDownedOnDeath(serverPlayer);
            ManbaStateManager.clearRuntimeOnDeath(serverPlayer);
            NikaidouHiroStateManager.clearRuntimeOnDeath(serverPlayer);
            NikaidouHiroWitchificationStateManager.clearRuntimeOnDeath(serverPlayer);
            CatDadStateManager.clearRuntimeOnDeath(serverPlayer);
            DepartmentOfTransportationStateManager.clearRuntimeOnDeath(serverPlayer);
            MorseStateManager.clearRuntimeOnDeath(serverPlayer);
            ToxikStateManager.clearRuntimeOnDeath(serverPlayer);
            RaptorStateManager.clearRuntimeOnDeath(serverPlayer);
            VlinderStateManager.clearRuntimeOnDeath(serverPlayer);
            TempestStateManager.clearRuntimeOnDeath(serverPlayer);
            LexNinjiaStateManager.clearRuntimeOnDeath(serverPlayer);
            SaeedStateManager.clearRuntimeOnDeath(serverPlayer);
            GhrothStateManager.clearRuntimeOnDeath(serverPlayer);
            GHROTH_NOON_PENDING_COPIES.remove(serverPlayer.getUUID());
            WEBBED_SELECTED_SLOTS.remove(serverPlayer.getUUID());
            TEMPEST_ACTION_LOCKED_SELECTED_SLOTS.remove(serverPlayer.getUUID());
            WEBBED_LOOK_LOCKS.remove(serverPlayer.getUUID());
        }

        ModItemEffectHelper.handleLivingDeath(entity, event.getSource());
        if (entity instanceof ServerPlayer deadPlayer) {
            HaffCoinManager.awardDeath(deadPlayer);
            UndeadSoulManager.awardDeath(deadPlayer);
            LexNinjiaCurrencyManager.awardDeath(deadPlayer);
        }

        Entity attacker = event.getSource().getEntity();
        if (damageSourceRootPlayer(event.getSource()) instanceof ServerPlayer commanderKiller
                && CommanderStateManager.isCommander(commanderKiller)
                && commanderKiller != entity) {
            CommanderStateManager.addTacticalPoints(commanderKiller,
                    entity instanceof Player ? 1_000L : Math.max(1L, Math.round(entity.getMaxHealth() * 20.0D)));
            CommanderSupportManager.onCommanderKill(commanderKiller, entity);
        }
        if (entity instanceof ServerPlayer commanderVictim
                && CommanderStateManager.isCommander(commanderVictim)) {
            CommanderStateManager.addTacticalPoints(commanderVictim, 1_000L);
        }
        if (attacker instanceof net.minecraft.world.entity.animal.Wolf wolf
                && wolf.getOwner() instanceof ServerPlayer owner) {
            LexNinjiaStateManager.onHamBeastKill(owner, wolf);
        }
        if (attacker instanceof SaeedGuardEntity guard) {
            guard.owner().ifPresent(owner -> SaeedStateManager.awardKill(owner, entity));
        }
        if (!(attacker instanceof ServerPlayer player) || attacker == event.getEntity()) {
            return;
        }
        HaffCoinManager.awardKill(player, entity);
        UndeadSoulManager.awardKill(player, entity);
        LexNinjiaCurrencyManager.awardKill(player, entity);
        SaeedStateManager.awardKill(player, entity);
        if (DWolfStateManager.isOverloadActive(player)) {
            DWolfSkills.handleOverloadKill(player);
        }
        if (VyronStateManager.isVyron(player)) {
            VyronSkills.handleKill(player);
            VyronStateManager.syncToClient(player);
        }
        if (TempestStateManager.isTempest(player)) {
            TempestStateManager.resetRollCooldownOnKill(player);
        }
        NoxStateManager.onKill(player);
        if (NikaidouHiroStateManager.isNikaidouHiro(player)) {
            NikaidouHiroStateManager.onKill(player);
        }

        if (DepartmentOfTransportationStateManager.isDepartment(player)) {
            DepartmentOfTransportationStateManager.revealFromOffense(player);
        }
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) {
            return;
        }
        if (!entity.level().getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)) {
            event.getDrops().clear();
            return;
        }
        if (entity instanceof Animal) {
            int looting = Math.max(0, event.getLootingLevel());
            float chance = Math.min(1.0F, 0.10F + 0.10F * looting);
            if (entity.getRandom().nextFloat() < chance) {
                int count = 1 + entity.getRandom().nextInt(3);
                for (int i = 0; i < looting; i++) {
                    count += entity.getRandom().nextInt(2);
                }
                event.getDrops().add(new ItemEntity(entity.level(), entity.getX(), entity.getY(), entity.getZ(),
                        new ItemStack(ModItems.ANIMAL_GLAND.get(), count)));
            }
        }
        if (event.getSource().getEntity() instanceof ServerPlayer killer) {
            duplicateDropsForEquipment(event, killer);
            int extraCopies = 0;
            if (UndeadStateManager.isUndead(killer)
                    && UndeadStateManager.profession(killer) == UndeadProfession.EXPLORER) {
                extraCopies++;
            }
            if (UndeadStateManager.consumeRogueExecutionMarker(entity, killer)) {
                extraCopies += 4;
            }
            if (consumeHackclawBackstabMarker(entity, killer)) {
                extraCopies += 2;
            }
            extraCopies += SaeedStateManager.extraLootCopies(killer, entity);
            duplicateLivingDrops(event, extraCopies);
            tryDropHumanoidEquipment(event, entity);
        } else {
            SaeedStateManager.ownerFromDamageEntity(event.getSource().getEntity())
                    .ifPresent(owner -> duplicateLivingDrops(event,
                            SaeedStateManager.extraLootCopies(owner, entity)));
        }
        if (entity.getType() != EntityType.VINDICATOR) {
            return;
        }
        boolean rolf = entity.hasCustomName()
                && ("罗尔夫".equals(entity.getCustomName().getString())
                || "Rolf".equalsIgnoreCase(entity.getCustomName().getString()));
        int count = 0;
        if (rolf) {
            count = 1 + entity.getRandom().nextInt(3);
        } else if (entity.getRandom().nextFloat() < 0.05F) {
            count = 1;
        }
        if (count > 0) {
            event.getDrops().add(new ItemEntity(entity.level(), entity.getX(), entity.getY(), entity.getZ(),
                    new ItemStack(ModItems.DARK_ZONE_RAINBOW_INJECTION.get(), count)));
        }
    }

    @SubscribeEvent
    public static void onCommanderServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            CommanderSupportManager.tickServer(event.getServer());
        }
    }

    @SubscribeEvent
    public static void onCommanderRadiationSpawn(MobSpawnEvent.FinalizeSpawn event) {
        if (event.getEntity().level() instanceof ServerLevel level
                && CommanderSupportManager.shouldPreventSpawn(level, event.getEntity().blockPosition())) {
            event.setSpawnCancelled(true);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onModLivingTickPeaceful(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide || entity.level().getDifficulty() != Difficulty.PEACEFUL) {
            return;
        }
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (id != null && DealtForceSkillsMod.MODID.equals(id.getNamespace())
                && !(entity instanceof AcceleratedTroopsEntity)
                && !(entity instanceof com.rzy.dealt_force_skills.entity.RoverWolfEntity)
                && !(entity instanceof com.rzy.dealt_force_skills.ai.operative.OperativeHost)) {
            entity.discard();
        }
    }

    @SubscribeEvent
    public static void onDfsItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (event.getCrafting().is(ModItems.HVK_ENCHANT_INFUSION.get())) {
            double failureChance = Math.max(4.0D, 99.0D - event.getEntity().getAttributeValue(Attributes.LUCK) * 5.0D);
            if (event.getEntity().getRandom().nextDouble() * 100.0D < failureChance) {
                event.getCrafting().setCount(0);
                event.getEntity().displayClientMessage(Component.translatable(
                        "message.dealt_force_skills.hvk_enchant_infusion.craft_failed"), true);
                return;
            }
        }
        grantBonusOutput(event.getEntity(), event.getCrafting(), durableConsumableMultiplier(event.getCrafting().getItem()));
    }

    @SubscribeEvent
    public static void onDfsItemSmelted(PlayerEvent.ItemSmeltedEvent event) {
        Item item = event.getSmelting().getItem();
        int multiplier = item == ModItems.STERILE_WATER.get() || item == ModItems.POLYETHYLENE_FIBER.get()
                ? 4
                : 1;
        grantBonusOutput(event.getEntity(), event.getSmelting(), multiplier);
    }

    private static int durableConsumableMultiplier(Item item) {
        if (item == ModItems.SELF_MADE_ARMOR_REPAIR_KIT.get()
                || item == ModItems.SELF_MADE_HELMET_REPAIR_KIT.get()
                || item == ModItems.ELASTIC_BANDAGE.get()
                || item == ModItems.SIMPLE_SURGICAL_PACK.get()
                || item == ModItems.SUSTAINED_RELEASE_PAINKILLER.get()
                || item == ModItems.CAR_FIRST_AID_KIT.get()
                || item == ModItems.SIMPLE_INJECTOR.get()) {
            return 4;
        }
        if (item == ModItems.STANDARD_ARMOR_REPAIR_KIT.get()
                || item == ModItems.STANDARD_HELMET_REPAIR_KIT.get()
                || item == ModItems.CAT_TOURNIQUET.get()
                || item == ModItems.TACTICAL_QUICK_SURGICAL_PACK.get()
                || item == ModItems.BOTTLED_ANTIBIOTICS.get()
                || item == ModItems.FIELD_FIRST_AID_KIT.get()
                || item == ModItems.STRONG_INJECTOR.get()) {
            return 3;
        }
        if (item == ModItems.PRECISION_ARMOR_REPAIR_KIT.get()
                || item == ModItems.ADVANCED_ARMOR_REPAIR_KIT.get()
                || item == ModItems.PRECISION_HELMET_REPAIR_KIT.get()
                || item == ModItems.ADVANCED_HELMET_REPAIR_KIT.get()
                || item == ModItems.DEK_FIELD_SURGICAL_PACK.get()
                || item == ModItems.DVE_PAINKILLER.get()
                || item == ModItems.OUTDOOR_MEDICAL_KIT.get()
                || item == ModItems.BATTLEFIELD_MEDICAL_KIT.get()) {
            return 2;
        }
        return 1;
    }

    private static void grantBonusOutput(Player player, ItemStack crafted, int multiplier) {
        if (player.level().isClientSide || crafted.isEmpty() || multiplier <= 1) {
            return;
        }
        int remaining = crafted.getCount() * (multiplier - 1);
        int maxStack = Math.max(1, crafted.getMaxStackSize());
        while (remaining > 0) {
            ItemStack bonus = crafted.copy();
            int count = Math.min(maxStack, remaining);
            bonus.setCount(count);
            remaining -= count;
            if (!player.getInventory().add(bonus)) {
                player.drop(bonus, false);
            }
        }
    }

    private static void tryDropHumanoidEquipment(LivingDropsEvent event, LivingEntity entity) {
        EntityType<?> type = entity.getType();
        boolean humanoid = type == EntityType.VILLAGER
                || type == EntityType.VINDICATOR
                || type == EntityType.PILLAGER
                || type == EntityType.WITCH
                || type == EntityType.EVOKER
                || type == EntityType.ILLUSIONER;
        if (!humanoid || entity.getRandom().nextFloat() >= 0.02F) {
            return;
        }
        ItemStack drop = new ItemStack(entity.getRandom().nextBoolean()
                ? ModItems.D6_TACTICAL_HELMET.get()
                : ModItems.SAMURAI_BALLISTIC_VEST.get());
        event.getDrops().add(new ItemEntity(entity.level(), entity.getX(), entity.getY(), entity.getZ(), drop));
    }

    @SubscribeEvent
    public static void onOceanBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof Level level) || level.isClientSide) {
            return;
        }
        Player player = event.getPlayer();
        int fortune = Math.max(0, EnchantmentHelper.getItemEnchantmentLevel(Enchantments.BLOCK_FORTUNE,
                player.getMainHandItem()));
        tryAwardCoarseSalt(player, level, event.getPos(), fortune);
        tryAwardPremiumCoffeeBeans(player, level, event.getState(), fortune);
        tryDuplicateNewRecruitMiningDrops(player, level, event.getState(), event.getPos());
        tryDuplicateUndeadExplorerMiningDrops(player, level, event.getState(), event.getPos());
        if (player instanceof ServerPlayer serverPlayer && level instanceof ServerLevel serverLevel) {
            SaeedStateManager.onOwnerBreakBlock(serverPlayer, serverLevel, event.getPos(), event.getState());
        }
    }

    @SubscribeEvent
    public static void onLexNinjiaBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        LexNinjiaStateManager.handleBlockPlaced(event);
    }

    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        handleFrugalDisassemblyCraft(event);
        // Premium coffee beans crafting recipe removed; boss drop / cocoa chance only.
    }

    @SubscribeEvent
    public static void onVillagerTrades(VillagerTradesEvent event) {
        if (!BLUEPRINT_PROFESSIONS.contains(event.getType())) {
            return;
        }
        VillagerTrades.ItemListing trade = CommonEvents::blueprintTradeOffer;
        for (int level : BLUEPRINT_TRADE_LEVELS) {
            event.getTrades().computeIfAbsent(level, ignored -> new ArrayList<>()).add(trade);
        }
    }

    @SubscribeEvent
    public static void onWandererTrades(WandererTradesEvent event) {
        event.getRareTrades().add(CommonEvents::blueprintTradeOffer);
    }

    @SubscribeEvent
    public static void onOceanBucketUse(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide) {
            return;
        }
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            SaeedStateManager.onOwnerInteractBlock(serverPlayer, event.getPos());
        }

        if (!event.getItemStack().is(Items.BUCKET)) {
            return;
        }
        tryAwardCoarseSalt(event.getEntity(), event.getLevel(), event.getPos(), 0);
    }

    /**
     * Adventure-mode safe HVK brain summon: sneak + right-click activated beacon.
     * Server-only so the interact packet always reaches the server; cancel after
     * success so the beacon GUI cannot consume the action.
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onHvkBrainBeaconSummon(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide) {
            return;
        }
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        if (player == null || stack.isEmpty() || !player.isShiftKeyDown()) {
            return;
        }
        if (!(stack.getItem() instanceof com.rzy.dealt_force_skills.item.HvkBrainUnitItem)) {
            return;
        }
        if (!com.rzy.dealt_force_skills.item.HvkBrainUnitItem.tryActivateFromEvent(
                event.getLevel(), player, event.getHand(), stack, event.getPos())) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    @SubscribeEvent
    public static void onPlayerTickUluruEffectResistance(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player player = event.player;
        if (player.level().isClientSide) return;

        if (!UluruStateManager.isUluru(player)) {
            ULURU_EFFECT_TRACKING.remove(player.getUUID());
            return;
        }

        Map<MobEffect, TrackedEffect> tracked = ULURU_EFFECT_TRACKING.computeIfAbsent(player.getUUID(), id -> new HashMap<>());
        Set<MobEffect> activeTypes = new HashSet<>();
        for (MobEffectInstance effect : new ArrayList<>(player.getActiveEffects())) {
            MobEffect type = effect.getEffect();
            if (type.getCategory() != MobEffectCategory.HARMFUL || effect.isInfiniteDuration()) {
                continue;
            }

            activeTypes.add(type);
            TrackedEffect previous = tracked.get(type);
            int currentDuration = effect.getDuration();
            if (previous != null
                    && previous.amplifier() == effect.getAmplifier()
                    && currentDuration <= previous.duration() + EFFECT_REAPPLY_GRACE_TICKS) {
                tracked.put(type, new TrackedEffect(currentDuration, effect.getAmplifier()));
                continue;
            }

            int reducedDuration = Math.max(1, currentDuration / 2);
            // Shorten the existing chain; remove/add would discard hidden infinite effects.
            ((com.rzy.dealt_force_skills.mixin.OperativeEffectAccessor)effect).dealt_force_skills$setDuration(reducedDuration);
            player.forceAddEffect(effect,null);
            com.rzy.dealt_force_skills.department.DepartmentTasks.signal(player,"uluru.passive.duration_saved",currentDuration-reducedDuration);
            tracked.put(type, new TrackedEffect(reducedDuration, effect.getAmplifier()));
        }
        tracked.keySet().removeIf(type -> !activeTypes.contains(type));
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getEntity();
        if (player instanceof ServerPlayer serverPlayer) {
            MorseStateManager.recordPlayerAction(serverPlayer);
            LexNinjiaStateManager.onPlayerAction(serverPlayer);
            if (GamblerArenaManager.blocksInteraction(serverPlayer)) {
                cancelPlayerInteract(event);
                return;
            }
        }
        if (player instanceof ServerPlayer serverPlayer
                && (UluruLoiteringMissileEntity.isPlayerControlling(serverPlayer)
                || RaptorFalconDroneEntity.isPlayerControlling(serverPlayer))) {
            cancelPlayerInteract(event);
            return;
        }

        if ((com.rzy.dealt_force_skills.character.rover.RoverStateManager.isDowned(player) || StingerStateManager.isDowned(player))
                || VlinderStateManager.isDowned(player)
                || CatDadStateManager.isDowned(player)
                || TempestStateManager.isActionLocked(player)) {
            cancelPlayerInteract(event);
            return;
        }

        boolean webbedLeftClick = effectApplies(player, ModEffects.WEBBED.get())
                && event instanceof PlayerInteractEvent.LeftClickBlock;
        if (effectApplies(player, ModEffects.MORSE_STRONG_SHOCK.get()) && shouldMorseShockSkipAction(player)) {
            cancelPlayerInteract(event);
            return;
        }
        if (effectApplies(player, ModEffects.SONIC_SHOCK.get()) && shouldSonicShockSkipAction(player)) {
            cancelPlayerInteract(event);
            return;
        }
        if (effectApplies(player, ModEffects.STUN.get())
                || effectApplies(player, ModEffects.RAPTOR_ACTION_PAUSE.get())
                || (effectApplies(player, ModEffects.WEBBED.get()) && !webbedLeftClick)) {
            cancelPlayerInteract(event);
            return;
        }

        if (!tryCutTargetedBladeWire(event)) {
            if (isAnyToolEquipped(player)) {
                cancelPlayerInteract(event);
            } else {
                if (player instanceof ServerPlayer serverPlayerx) {
                    DfsAchievements.recordToxikAdrenalineAction(serverPlayerx, "interact");
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAttackEntity(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (event.getTarget() instanceof com.rzy.dealt_force_skills.entity.OperativeEntity bot
                && com.rzy.dealt_force_skills.item.OperativeSpawnEggItem.collect(player, bot)) {
            event.setCanceled(true); return;
        }
        if (event.getTarget() instanceof LivingEntity target
                && TeamCombatRules.shouldCancelFriendlyDamage(target, player)) {
            event.setCanceled(true);
            return;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            MorseStateManager.recordPlayerAction(serverPlayer);
        }
        if (player instanceof ServerPlayer serverPlayer
                && (UluruLoiteringMissileEntity.isPlayerControlling(serverPlayer)
                || RaptorFalconDroneEntity.isPlayerControlling(serverPlayer))) {
            event.setCanceled(true);
            return;
        }

        if ((com.rzy.dealt_force_skills.character.rover.RoverStateManager.isDowned(player) || StingerStateManager.isDowned(player))
                || VlinderStateManager.isDowned(player)
                || CatDadStateManager.isDowned(player)
                || TempestStateManager.isActionLocked(player)) {
            event.setCanceled(true);
            return;
        }

        if (effectApplies(player, ModEffects.STUN.get()) || effectApplies(player, ModEffects.RAPTOR_ACTION_PAUSE.get())) {
            event.setCanceled(true);
            return;
        }

        if (effectApplies(player, ModEffects.MORSE_STRONG_SHOCK.get()) && shouldMorseShockSkipAction(player)) {
            event.setCanceled(true);
            return;
        }

        if (effectApplies(player, ModEffects.SONIC_SHOCK.get()) && shouldSonicShockSkipAction(player)) {
            event.setCanceled(true);
            return;
        }

        if (player instanceof ServerPlayer serverPlayer && event.getTarget() instanceof LivingEntity target) {
            LexNinjiaStateManager.handleAttackEntity(serverPlayer, target);
        }

        if (isAnyToolEquipped(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onUseItemTick(net.minecraftforge.event.entity.living.LivingEntityUseItemEvent.Tick event) {
        if (event.getEntity() instanceof Player player
                && ((com.rzy.dealt_force_skills.character.rover.RoverStateManager.isDowned(player) || StingerStateManager.isDowned(player))
                || VlinderStateManager.isDowned(player)
                || CatDadStateManager.isDowned(player)
                || NikaidouHiroStateManager.isWeaponActive(player)
                || DepartmentOfTransportationStateManager.hasEquippedTool(player)
                || TempestStateManager.isActionLocked(player)
                || UndeadStateManager.isActionLocked(player)
                || UndeadStateManager.isRitualDancing(player))) {
            boolean tempestDisarmed = effectApplies(player, ModEffects.TEMPEST_DISARMED.get());
            if (tempestDisarmed) {
                cancelBlockedUseItem(event, player);
            } else {
                stopBlockedUseItem(event, player);
            }
            if (tempestDisarmed && player instanceof ServerPlayer serverPlayer) {
                cooldownTempestDisarmedHands(serverPlayer);
                cancelOptionalTaczReload(serverPlayer);
            }
            return;
        }
        if (event.getEntity() instanceof ServerPlayer player
                && RaptorFalconDroneEntity.isPlayerControlling(player)) {
            stopBlockedUseItem(event, player);
            return;
        }
        if (event.getEntity() instanceof ServerPlayer player) {
            MorseStateManager.recordPlayerAction(player);
            LexNinjiaStateManager.onPlayerAction(player);
        }
        if (event.getEntity() instanceof Player player && effectApplies(player, ModEffects.RAPTOR_ACTION_PAUSE.get())) {
            stopBlockedUseItem(event, player);
            return;
        }
        if (!shouldAdjustUseDuration(event.getItem())) {
            return;
        }
        if (event.getEntity() instanceof Player player
                && effectApplies(player, ModEffects.SONIC_SHOCK.get())
                && shouldSonicShockSkipAction(player)) {
            event.setDuration(event.getDuration() + 1);
        }
        if (event.getEntity() instanceof Player player
                && effectApplies(player, ModEffects.MORSE_STRONG_SHOCK.get())
                && shouldMorseShockSkipAction(player)) {
            event.setDuration(event.getDuration() + 1);
        }
    }

    /**
     * Returns true if the player has any character pseudo-tool equipped that should block
     * vanilla left/right click interactions (attack, use item, interact).
     */
    private static boolean isAnyToolEquipped(Player player) {
        if (com.rzy.dealt_force_skills.character.wildfire.WildfireStateManager.blocksItems(player)) return true;
        // Sineva: shield deployed
        if (SinevaSkills.canUseShieldAction(player)) {
            return true;
        }
        // Uluru: any tool equipped (incendiary, cover, missile)
        if (UluruStateManager.isUluru(player) && UluruStateManager.equippedTool(player) != UluruTool.NONE) {
            return true;
        }
        // D-Wolf: hand cannon equipped
        if (DWolfStateManager.isDWolf(player) && DWolfStateManager.equippedTool(player) != DWolfTool.NONE) {
            return true;
        }
        // Gizmo: any trap or T-boy pseudo-tool equipped
        if (GizmoStateManager.isGizmo(player) && GizmoStateManager.equippedTool(player) != GizmoTool.NONE) {
            return true;
        }
        // Shepherd: sonic trap or frag grenade equipped
        if (ShepherdStateManager.isShepherd(player) && ShepherdStateManager.equippedTool(player) != ShepherdTool.NONE) {
            return true;
        }
        // Luna: shock bow, composite grenade, or recon bow equipped
        if (LunaStateManager.isLuna(player) && LunaStateManager.equippedTool(player) != LunaTool.NONE) {
            return true;
        }
        // Vyron: magnetic bomb or Tiger Crouching Cannon equipped
        if (VyronStateManager.isVyron(player) && VyronStateManager.equippedTool(player) != VyronTool.NONE) {
            return true;
        }
        // Stinger: smoke grenade, smoke drone, or stim gun equipped
        if (StingerStateManager.isStinger(player) && StingerStateManager.equippedTool(player) != StingerTool.NONE) {
            return true;
        }
        if (ManbaStateManager.isManba(player) && ManbaStateManager.isFlashlightActive(player)) {
            return true;
        }
        if (NikaidouHiroStateManager.isNikaidouHiro(player) && NikaidouHiroStateManager.equippedTool(player) != NikaidouHiroTool.NONE) {
            return true;
        }
        if (DepartmentOfTransportationStateManager.hasEquippedTool(player)) {
            return true;
        }
        if (MorseStateManager.isMorse(player) && MorseStateManager.equippedTool(player) != MorseTool.NONE) {
            return true;
        }
        if (ToxikStateManager.isToxik(player) && ToxikStateManager.equippedTool(player) != ToxikTool.NONE) {
            return true;
        }
        if (RaptorStateManager.isRaptor(player) && RaptorStateManager.equippedTool(player) != RaptorTool.NONE) {
            return true;
        }
        if (VlinderStateManager.isVlinder(player) && VlinderStateManager.equippedTool(player) != VlinderTool.NONE) {
            return true;
        }
        if (TempestStateManager.isTempest(player) && TempestStateManager.equippedTool(player) != TempestTool.NONE) {
            return true;
        }
        return false;
    }

    @SubscribeEvent
    public static void onPlayerTickStun(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player player = event.player;
        if (player instanceof ServerPlayer serverPlayer) {
            SinevaKnockdownState.tick(serverPlayer);
            GizmoStateManager.tickWebbedEscape(serverPlayer);
            StingerStateManager.tickDowned(serverPlayer);
            VlinderStateManager.tickDowned(serverPlayer);
            ToxikStateManager.tickFireflyPullout(serverPlayer);
            ToxikStateManager.accelerateCooldowns(player);
            ModItemEffectHelper.tickItemEffects(serverPlayer);
        }
        accelerateTaczAdrenalineTimers(player);
        TaczSpeedMultipliers superbWarfareMultipliers = taczSpeedMultipliers(player);
        boolean optionalGunDisabled = (com.rzy.dealt_force_skills.character.rover.RoverStateManager.isDowned(player) || StingerStateManager.isDowned(player))
                || VlinderStateManager.isDowned(player)
                || CatDadStateManager.isDowned(player)
                || TempestStateManager.isActionLocked(player)
                || NikaidouHiroStateManager.isWeaponActive(player)
                || isAnyToolEquipped(player)
                || effectApplies(player, ModEffects.STUN.get())
                || effectApplies(player, ModEffects.RAPTOR_ACTION_PAUSE.get())
                // TaCZ cancels these periodic action windows through its own
                // events. Apply the same window to Superb Warfare weapons.
                || (effectApplies(player, ModEffects.SONIC_SHOCK.get())
                && shouldSonicShockSkipAction(player))
                || (effectApplies(player, ModEffects.MORSE_STRONG_SHOCK.get())
                && shouldMorseShockSkipAction(player));
        SuperbWarfareCompat.applyPlayerWeaponModifiers(player,
                superbWarfareMultipliers.fireRate(), superbWarfareMultipliers.reload(),
                superbWarfareMultipliers.aim(), superbWarfareMultipliers.aimPenalty(),
                superbWarfareMultipliers.bolt(), optionalGunDisabled);
        if (player.getVehicle() != null) {
            SuperbWarfareCompat.applyWeaponSpeedModifiers(player.getVehicle(),
                    superbWarfareMultipliers.fireRate(), superbWarfareMultipliers.reload(),
                    superbWarfareMultipliers.aim(), superbWarfareMultipliers.aimPenalty(),
                    superbWarfareMultipliers.bolt());
            SuperbWarfareCompat.setVehicleWeaponDisabled(player.getVehicle(), optionalGunDisabled);
        }
        if ((com.rzy.dealt_force_skills.character.rover.RoverStateManager.isDowned(player) || StingerStateManager.isDowned(player)) || VlinderStateManager.isDowned(player) || CatDadStateManager.isDowned(player)) {
            player.stopUsingItem();
            player.setSprinting(false);
            if (!player.level().isClientSide && player.containerMenu != player.inventoryMenu) {
                player.closeContainer();
            }
            if (player instanceof ServerPlayer downedPlayer) {
                UluruLoiteringMissileEntity.stopPlayerControl(downedPlayer);
                RaptorFalconDroneEntity.stopPlayerControl(downedPlayer);
            }
            return;
        }
        if (NikaidouHiroStateManager.isWeaponActive(player)) {
            player.stopUsingItem();
            if (player instanceof ServerPlayer nikaidouPlayer) {
                cancelOptionalTaczReload(nikaidouPlayer);
            }
        }
        if (DepartmentOfTransportationStateManager.hasEquippedTool(player)) {
            player.stopUsingItem();
            if (player instanceof ServerPlayer departmentPlayer) {
                cancelOptionalTaczReload(departmentPlayer);
            }
        }
        if (TempestStateManager.isActionLocked(player)) {
            player.stopUsingItem();
            if (TempestStateManager.isDowned(player) || TempestStateManager.isRecalling(player)) {
                player.setSprinting(false);
            }
            if (player instanceof ServerPlayer tempestLockedPlayer) {
                TempestStateManager.setEquippedTool(tempestLockedPlayer, TempestTool.NONE);
                lockTempestActionHotbar(tempestLockedPlayer);
                cooldownTempestDisarmedHands(tempestLockedPlayer);
                cancelOptionalTaczReload(tempestLockedPlayer);
                if (tempestLockedPlayer.containerMenu != tempestLockedPlayer.inventoryMenu) {
                    tempestLockedPlayer.closeContainer();
                }
                if (TempestStateManager.isDowned(tempestLockedPlayer) || TempestStateManager.isRecalling(tempestLockedPlayer)) {
                    UluruLoiteringMissileEntity.stopPlayerControl(tempestLockedPlayer);
                    RaptorFalconDroneEntity.stopPlayerControl(tempestLockedPlayer);
                    return;
                }
            }
        } else if (player instanceof ServerPlayer serverPlayer) {
            TEMPEST_ACTION_LOCKED_SELECTED_SLOTS.remove(serverPlayer.getUUID());
        }
        if (effectApplies(player, ModEffects.STUN.get())) {
            Vec3 velocity = player.getDeltaMovement();
            player.setDeltaMovement(0, velocity.y, 0);
            player.stopUsingItem();
            if (!player.level().isClientSide && player.containerMenu != player.inventoryMenu) {
                player.closeContainer();
            }
            if (player instanceof ServerPlayer stunnedPlayer) {
                UluruLoiteringMissileEntity.stopPlayerControl(stunnedPlayer);
                RaptorFalconDroneEntity.stopPlayerControl(stunnedPlayer);
            }
        }
        if (effectApplies(player, ModEffects.RAPTOR_ACTION_PAUSE.get())) {
            Vec3 velocity = player.getDeltaMovement();
            player.setDeltaMovement(0.0D, velocity.y, 0.0D);
            player.stopUsingItem();
            if (!player.level().isClientSide && player.containerMenu != player.inventoryMenu) {
                player.closeContainer();
            }
        }
        MobEffectInstance morseStrongShock = activeEffectInstance(player, ModEffects.MORSE_STRONG_SHOCK.get());
        if (morseStrongShock != null) {
            Vec3 velocity = player.getDeltaMovement();
            double factor = morseStrongShock.getAmplifier() >= 1 ? 0.25D : 0.55D;
            player.setDeltaMovement(velocity.x * factor, velocity.y, velocity.z * factor);
            player.setSprinting(false);
            player.hurtMarked = true;
        }
        if (effectApplies(player, ModEffects.WEBBED.get())) {
            Vec3 velocity = player.getDeltaMovement();
            player.setDeltaMovement(0.0D, Math.min(0.0D, velocity.y), 0.0D);
            player.stopUsingItem();
            if (!player.level().isClientSide && player.containerMenu != player.inventoryMenu) {
                player.closeContainer();
            }
            if (player instanceof ServerPlayer webbedPlayer) {
                lockWebbedHotbar(webbedPlayer);
                lockWebbedLook(webbedPlayer);
                cancelOptionalTaczReload(webbedPlayer);
                UluruLoiteringMissileEntity.stopPlayerControl(webbedPlayer);
                RaptorFalconDroneEntity.stopPlayerControl(webbedPlayer);
            }
        } else if (player instanceof ServerPlayer serverPlayer) {
            WEBBED_SELECTED_SLOTS.remove(serverPlayer.getUUID());
            WEBBED_LOOK_LOCKS.remove(serverPlayer.getUUID());
        }
    }

    @SubscribeEvent
    public static void onPlayerTickSonicShockTimeFlow(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Player player = event.player;
        MobEffectInstance sonicShock = activeEffectInstance(player, ModEffects.SONIC_SHOCK.get());
        if (sonicShock == null) {
            return;
        }

        double factor = sonicShock.getAmplifier() >= 1 ? 0.25D : 0.55D;
        Vec3 velocity = player.getDeltaMovement();
        player.setDeltaMovement(velocity.x * factor, velocity.y, velocity.z * factor);
        player.setSprinting(false);
        player.hurtMarked = true;

    }

    @SubscribeEvent
    public static void onPlayerTickMissileControlLock(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        boolean uluruControl = UluruLoiteringMissileEntity.isPlayerControlling(player);
        boolean raptorControl = RaptorFalconDroneEntity.isPlayerControlling(player);
        if (!uluruControl && !raptorControl) {
            return;
        }

        if (uluruControl) {
            UluruLoiteringMissileEntity.lockControllingPlayer(player);
        }
        if (raptorControl) {
            RaptorFalconDroneEntity.lockControllingPlayer(player);
        }
        player.stopUsingItem();
        if (player.containerMenu != player.inventoryMenu) {
            player.closeContainer();
        }
    }

    @SubscribeEvent
    public static void onWorldTickCharge(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.level.isClientSide) return;
        SinevaSkills.tickChargingPlayers(event.level);
        if (event.level instanceof ServerLevel serverLevel && serverLevel == serverLevel.getServer().overworld()) {
            com.rzy.dealt_force_skills.team.TeammateRevealSync.serverTick(serverLevel.getServer());
        }
    }

    @SubscribeEvent
    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        Entity directSource = event.getExplosion().getDirectSourceEntity();
        boolean departmentChargedCreeper =
                DepartmentOfTransportationStateManager.isDepartmentChargedCreeper(directSource);
        SaeedGuardEntity hakimRocketOwner = saeedHakimGuardFromRocketExplosion(directSource);
        boolean hakimRocket = hakimRocketOwner != null;
        if (!departmentChargedCreeper && !hakimRocket) {
            UluruExplosionHelper.destroyQuickCovers(serverLevel, event.getExplosion().getPosition(), 6.0);
        }
        TempestStateManager.triggerExplosionSpineForNearby(serverLevel,
                event.getExplosion().getPosition(), event.getAffectedEntities());
        if (departmentChargedCreeper) {
            removeDepartmentOwnerFromExplosionTargets(directSource, event.getAffectedEntities());
            event.getAffectedBlocks().clear();
            if (com.rzy.dealt_force_skills.ai.operative.NativeDepartmentSkills.nativeSummon(directSource)) {
                var allegiance = com.rzy.dealt_force_skills.ai.operative.NativeDepartmentSkills.summonAllegiance(directSource);
                event.getAffectedEntities().removeIf(entity -> !(entity instanceof LivingEntity living) || !allegiance.isEnemy(serverLevel, living));
            }
            if (!com.rzy.dealt_force_skills.ai.operative.NativeDepartmentSkills.nativeSummon(directSource)) SuperbWarfareCompat.damageVehicles(
                    serverLevel,
                    event.getExplosion().getPosition(),
                    6.0,
                    SkillDamageHelper.departmentTrap(serverLevel, directSource, directSource),
                    directSource,
                    0.16F,
                    false
            );
            SuperbWarfareCompat.removeVehicles(event.getAffectedEntities());
            DepartmentOfTransportationStateManager.handleDepartmentChargedCreeperExplosion(serverLevel, directSource);
        } else if (hakimRocket) {
            event.getAffectedBlocks().clear();
            SuperbWarfareCompat.damageVehicles(
                    serverLevel,
                    event.getExplosion().getPosition(),
                    SAEED_HAKIM_ROCKET_DAMAGE_RADIUS,
                    SkillDamageHelper.uluruMissile(serverLevel, hakimRocketOwner, hakimRocketOwner),
                    directSource,
                    SAEED_HAKIM_ROCKET_DAMAGE / 100.0F,
                    true
            );
            SuperbWarfareCompat.removeVehicles(event.getAffectedEntities());
            preventSaeedHakimRocketKnockback(serverLevel, hakimRocketOwner,
                    event.getExplosion().getPosition(), event.getAffectedEntities());
        }
        for (BlockPos pos : event.getAffectedBlocks()) {
            if (serverLevel.getBlockState(pos).is(ModBlocks.QUICK_COVER.get())) {
                QuickCoverBlockEntity.destroyCoverAt(serverLevel, pos);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onExplosionStart(ExplosionEvent.Start event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }
        Entity directSource = event.getExplosion().getDirectSourceEntity();
        if (UndeadStateManager.isHunterRetreatTnt(directSource)) {
            event.setCanceled(true);
            UndeadStateManager.handleHunterRetreatExplosion(serverLevel, directSource);
        }
    }

    private static SaeedGuardEntity saeedHakimGuardFromRocketExplosion(Entity directSource) {
        if (directSource instanceof SaeedGuardEntity guard && guard.guardType() == SaeedGuardType.HAKIM) {
            return guard;
        }
        if (directSource instanceof Projectile projectile
                && projectile.getOwner() instanceof SaeedGuardEntity guard
                && guard.guardType() == SaeedGuardType.HAKIM) {
            return guard;
        }
        return null;
    }

    private static void removeDepartmentOwnerFromExplosionTargets(Entity source, List<Entity> affectedEntities) {
        if (source != null && !affectedEntities.isEmpty()) {
            CompoundTag tag = source.getPersistentData();
            if (tag.hasUUID("DepartmentOwner")) {
                UUID ownerId = tag.getUUID("DepartmentOwner");
                affectedEntities.removeIf(entity -> entity != null && ownerId.equals(entity.getUUID()));
            }
        }
    }

    private static void preventSaeedHakimRocketKnockback(ServerLevel level, SaeedGuardEntity owner, Vec3 center,
                                                         List<Entity> affectedEntities) {
        affectedEntities.removeIf(entity -> {
            if (!(entity instanceof ServerPlayer || entity instanceof SaeedGuardEntity)) {
                return false;
            }
            if (!(entity instanceof LivingEntity living) || !living.isAlive()) {
                return true;
            }
            Vec3 entityCenter = living.position().add(0.0D, living.getBbHeight() * 0.5D, 0.0D);
            double distance = entityCenter.distanceTo(center);
            if (distance <= SAEED_HAKIM_ROCKET_DAMAGE_RADIUS
                    && UluruExplosionHelper.hasExplosionLineOfSight(level, center, living)) {
                float damage = SAEED_HAKIM_ROCKET_DAMAGE * Math.max(0.0F, 1.0F - (float) distance * 0.25F);
                if (damage > 0.0F) {
                    living.invulnerableTime = 0;
                    SkillDamageHelper.hurt(living, SkillDamageHelper.uluruMissile(level, owner, owner), owner, damage);
                }
            }
            living.setDeltaMovement(Vec3.ZERO);
            living.hurtMarked = true;
            return true;
        });
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAnyForgeEvent(Event event) {
        String eventName = event.getClass().getName();
        if (TOUHOU_MAID_PICKUP_PRE_EVENT.equals(eventName)) {
            handleTouhouMaidCoffeePickup(event);
            return;
        }
        if (TACZ_ATTACHMENT_PROPERTY_EVENT.equals(eventName)) {
            handleGhrothAttachmentProperty(event);
            com.rzy.dealt_force_skills.workbench.WorkbenchTacz.adjust(event);
            return;
        }
        if (TACZ_ENTITY_HURT_BY_GUN_PRE_EVENT.equals(eventName)) {
            if (cancelTeamTaczPreDamage(event)) {
                return;
            }
            if (handleShakehandsTaczPreDamage(event)) {
                return;
            }
            allowPortableAdsTaczHit(event);
            handleGhrothTaczPreDamage(event);
            handleChamberTaczPreDamage(event);
            recordSaeedGuardTaczPreDamage(event);
            return;
        }
        if (TACZ_GUN_RELOAD_EVENT.equals(eventName)) {
            recordMorseTaczAction(event);
            if (cancelLexNinjiaTaczAction(event) || cancelUndeadTaczAction(event) || cancelDownedTaczAction(event)
                    || cancelFrozenTaczAction(event) || cancelManbaDuelTaczAction(event)) {
                return;
            }
            cancelTaczReloadEvent(event);
            if (throttleMorseShockTaczTimeEvent(event) || throttleSonicTaczTimeEvent(event)) {
                return;
            }
            return;
        }

        if (isTaczSonicTimeEvent(eventName)) {
            recordMorseTaczAction(event);
            accelerateTaczAdrenalineForEvent(eventName, event);
            if (cancelLexNinjiaTaczAction(event)
                    || cancelUndeadTaczAction(event)
                    || cancelDownedTaczAction(event)
                    || cancelFrozenTaczAction(event)
                    || cancelManbaDuelTaczAction(event)
                    || throttleMorseShockTaczTimeEvent(event)
                    || throttleSonicTaczTimeEvent(event)) {
                return;
            }
            snapshotMhsTaczAmmo(eventName, event);
            if (TACZ_GUN_FIRE_EVENT.equals(eventName)) {
                handleGhrothStarsGunFire(event);
            }
        }

        if (!TACZ_AMMO_HIT_BLOCK_EVENT.equals(eventName)) {
            return;
        }

        try {
            Object levelObject = event.getClass().getMethod("getLevel").invoke(event);
            Object hitObject = event.getClass().getMethod("getHitResult").invoke(event);
            Object ammoObject = event.getClass().getMethod("getAmmo").invoke(event);
            if (!(levelObject instanceof Level level) || level.isClientSide
                    || !(hitObject instanceof BlockHitResult hit)) {
                return;
            }

            BlockPos pos = hit.getBlockPos();
            int damage = ammoObject instanceof Projectile projectile
                    ? QuickCoverBlock.projectileDamage(projectile, hit.getLocation())
                    : 25;
            if (level.getBlockState(pos).is(ModBlocks.QUICK_COVER.get())) {
                QuickCoverBlockEntity.damageCoverAt(level, pos, damage);
                return;
            }
            if (level.getBlockState(pos).is(ModBlocks.BLADE_WIRE_CORE.get())
                    || level.getBlockState(pos).is(ModBlocks.BLADE_WIRE.get())) {
                BladeWireBlockEntity.damageWireAt(level, pos, damage);
            }
        } catch (ReflectiveOperationException ignored) {
            // TACZ is optional; reflection keeps this mod loadable without a hard dependency.
        }
    }

    private static boolean cancelPlayerInteract(PlayerInteractEvent event) {
        if (!event.isCancelable()) {
            return false;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.FAIL);
        return true;
    }

    private static boolean tryCutTargetedBladeWire(PlayerInteractEvent event) {
        if (event instanceof PlayerInteractEvent.LeftClickBlock leftClick
                && leftClick.getEntity() instanceof ServerPlayer player
                && leftClick.getLevel() instanceof ServerLevel level
                && MeleeWeaponCompat.isMeleeWeapon(player.getMainHandItem())) {
            BlockPos pos = leftClick.getPos();
            if (!BladeWireBlockEntity.destroyWireBlock(level, pos, player)) {
                return false;
            }

            damageCuttingWeapon(player);
            leftClick.setCanceled(true);
            leftClick.setUseBlock(Event.Result.DENY);
            leftClick.setUseItem(Event.Result.DENY);
            return true;
        }

        return false;
    }

    private static void damageCuttingWeapon(ServerPlayer player) {
        ItemStack weapon = player.getMainHandItem();
        if (!weapon.isEmpty() && !player.getAbilities().instabuild) {
            weapon.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(EquipmentSlot.MAINHAND));
        }
    }

    private static void handleTouhouMaidCoffeePickup(Event event) {
        try {
            Object simulateObject = event.getClass().getMethod("isSimulate").invoke(event);
            Object itemObject = event.getClass().getMethod("getEntityItem").invoke(event);
            Object maidObject = event.getClass().getMethod("getMaid").invoke(event);
            if (!(simulateObject instanceof Boolean simulate) || simulate
                    || !(itemObject instanceof ItemEntity itemEntity)
                    || !itemEntity.getItem().is(Items.COCOA_BEANS)
                    || !(maidObject instanceof LivingEntity maid) || maid.level().isClientSide
                    || maid.getRandom().nextFloat() >= 0.20F) {
                return;
            }
            ItemStack remainder = insertTouhouMaidInventory(maid,
                    new ItemStack(ModItems.PREMIUM_COFFEE_BEANS.get()));
            if (!remainder.isEmpty()) {
                maid.level().addFreshEntity(new ItemEntity(maid.level(), maid.getX(), maid.getY(), maid.getZ(), remainder));
            }
            maid.level().playSound(null, maid.blockPosition(), net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP,
                    net.minecraft.sounds.SoundSource.NEUTRAL, 0.8F, 1.15F);
            if (maid instanceof net.minecraft.world.entity.OwnableEntity ownable
                    && ownable.getOwner() instanceof ServerPlayer owner) {
                owner.sendSystemMessage(Component.translatable(
                        "message.dealt_force_skills.maid_lucky_coffee", maid.getDisplayName())
                        .withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE));
            }
        } catch (ReflectiveOperationException ignored) {
            // Optional Touhou Little Maid versions without this public event shape are left untouched.
        }
    }

    private static ItemStack insertTouhouMaidInventory(LivingEntity maid, ItemStack stack)
            throws ReflectiveOperationException {
        Object inventory;
        try {
            inventory = maid.getClass().getMethod("getAvailableBackpackInv").invoke(maid);
        } catch (NoSuchMethodException ignored) {
            inventory = maid.getClass().getMethod("getAvailableInv", boolean.class).invoke(maid, false);
        }
        Method slotsMethod = inventory.getClass().getMethod("getSlots");
        Method insertMethod = inventory.getClass().getMethod("insertItem", int.class, ItemStack.class, boolean.class);
        int slots = ((Number) slotsMethod.invoke(inventory)).intValue();
        ItemStack remainder = stack;
        for (int slot = 0; slot < slots && !remainder.isEmpty(); slot++) {
            Object next = insertMethod.invoke(inventory, slot, remainder, false);
            if (next instanceof ItemStack nextStack) remainder = nextStack;
        }
        return remainder;
    }


    private static boolean cancelTeamTaczPreDamage(Event event) {
        try {
            Object attackerObject = event.getClass().getMethod("getAttacker").invoke(event);
            Object hurtObject = event.getClass().getMethod("getHurtEntity").invoke(event);
            if (!(attackerObject instanceof LivingEntity attacker)
                    || !(hurtObject instanceof LivingEntity target)) {
                return false;
            }
            if (!TeamCombatRules.shouldCancelFriendlyDamage(target, attacker)) {
                return false;
            }
            if (event.isCancelable()) {
                event.setCanceled(true);
            }
            return true;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return false;
        }
    }

    private static void allowPortableAdsTaczHit(Event event) {
        try {
            Object attackerObject = event.getClass().getMethod("getAttacker").invoke(event);
            Object hurtObject = event.getClass().getMethod("getHurtEntity").invoke(event);
            if (hurtObject instanceof ServerPlayer player && attackerObject instanceof Entity attacker
                    && com.rzy.dealt_force_skills.item.PortableAdsDeviceItem.isActive(player)) {
                com.rzy.dealt_force_skills.item.PortableAdsDeviceItem.allowNextTaczHit(player, attacker);
            }
        } catch (ReflectiveOperationException | LinkageError ignored) {
        }
    }

    private static boolean handleShakehandsTaczPreDamage(Event event) {
        try {
            Object attackerObject = event.getClass().getMethod("getAttacker").invoke(event);
            Object hurtObject = event.getClass().getMethod("getHurtEntity").invoke(event);
            if (!(attackerObject instanceof LivingEntity attacker)
                    || !(hurtObject instanceof LivingEntity target)) {
                return false;
            }
            float amount = reflectedFloat(event, "getBaseAmount", 0.0F);
            float headshotMultiplier = reflectedFloat(event, "getHeadshotMultiplier", 1.0F);
            boolean headshot = reflectedBoolean(event, "isHeadShot", false);
            float total = headshot ? amount * Math.max(1.0F, headshotMultiplier) : amount;
            if (!ShakehandsCombatHandler.tryReflectFromAttacker(target, attacker, Math.max(1.0F, total))) {
                return false;
            }
            if (event.isCancelable()) {
                event.setCanceled(true);
            }
            return true;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return false;
        }
    }

    private static void handleGhrothTaczPreDamage(Event event) {
        try {
            Object attackerObject = event.getClass().getMethod("getAttacker").invoke(event);
            Object hurtObject = event.getClass().getMethod("getHurtEntity").invoke(event);
            if (!(attackerObject instanceof ServerPlayer attacker)
                    || !GhrothStateManager.isGhroth(attacker)
                    || !(hurtObject instanceof LivingEntity target)) {
                return;
            }
            if (target instanceof Player targetPlayer && GhrothStateManager.shouldCancelCeasefire(attacker, targetPlayer)) {
                if (event.isCancelable()) {
                    event.setCanceled(true);
                }
                return;
            }
            ItemStack gun = attacker.getMainHandItem();
            if (!isTaczGunStack(gun)) {
                return;
            }
            ResourceLocation gunId = null;
            Object gunIdObject = event.getClass().getMethod("getGunId").invoke(event);
            if (gunIdObject instanceof ResourceLocation resourceLocation) {
                gunId = resourceLocation;
            }
            float baseAmount = reflectedFloat(event, "getBaseAmount", 0.0F);
            float headshotMultiplier = reflectedFloat(event, "getHeadshotMultiplier", 1.0F);
            boolean headshot = reflectedBoolean(event, "isHeadShot", false);
            double newBaseAmountValue = baseAmount * GhrothTaczEnhancement.levelDamageMultiplier(attacker);
            if (GhrothTaczEnhancement.isEnhancedGun(gun)) {
                newBaseAmountValue = Math.max(newBaseAmountValue,
                        GhrothTaczEnhancement.estimatedShotDamage(attacker, gun, gunId));
            }
            float newBaseAmount = GhrothTaczEnhancement.clampEnhancedDamage(newBaseAmountValue);
            float newHeadshotMultiplier = GhrothTaczEnhancement.headshotMultiplier(headshotMultiplier, gun, gunId);
            event.getClass().getMethod("setBaseAmount", float.class).invoke(event, newBaseAmount);
            event.getClass().getMethod("setHeadshotMultiplier", float.class).invoke(event, newHeadshotMultiplier);
            if (GhrothTaczEnhancement.isEnhancedGun(gun)) {
                replaceGhrothTaczDamageSources(event, attacker);
            }
            float floor = headshot ? newBaseAmount * newHeadshotMultiplier : newBaseAmount;
            recordGhrothDamageFloor(target, attacker, floor, GhrothStateManager.isStarsActive(attacker));
        } catch (ReflectiveOperationException | LinkageError ignored) {
            // TaCZ event internals differ between versions; Ghroth falls back to LivingHurt handling.
        }
    }

    private static void handleChamberTaczPreDamage(Event event) {
        try {
            if (!(event.getClass().getMethod("getAttacker").invoke(event) instanceof ServerPlayer attacker)) {
                return;
            }

            ItemStack gun = attacker.getMainHandItem();
            if (!ChamberTaczEnhancement.isChamberWeapon(gun)) {
                return;
            }

            Object hurtObject = event.getClass().getMethod("getHurtEntity").invoke(event);
            float headshotMultiplier = reflectedFloat(event, "getHeadshotMultiplier", 1.0F);
            event.getClass().getMethod("setHeadshotMultiplier", float.class).invoke(event,
                    ChamberTaczEnhancement.headshotMultiplier(headshotMultiplier, gun));
            if (ChamberTaczEnhancement.kind(gun) == ChamberGunKind.TOUR_DE_FORCE
                    && hurtObject instanceof LivingEntity target
                    && reflectedFloat(event, "getBaseAmount", 0.0F) > 0.0F) {
                ChamberStateManager.tryTriggerTourSlowField(attacker, target);
            }
        } catch (LinkageError | ReflectiveOperationException ignored) {
            // Chamber Tacz enhancement is optional.
        }
    }

    private static void recordSaeedGuardTaczPreDamage(Event event) {
        try {
            Object attackerObject = event.getClass().getMethod("getAttacker").invoke(event);
            Object hurtObject = event.getClass().getMethod("getHurtEntity").invoke(event);
            if (attackerObject instanceof SaeedGuardEntity guard
                    && guard.guardType() == SaeedGuardType.IRON_RAIN
                    && hurtObject instanceof LivingEntity
                    && !(reflectedFloat(event, "getBaseAmount", 0.0F) <= 0.0F)) {
                guard.owner().ifPresent(DfsAchievements::recordSaeedIronRainHit);
            }
        } catch (LinkageError | ReflectiveOperationException ignored) {
            // Saeed guard Tacz tracking is optional.
        }
    }

    private static void handleGhrothAttachmentProperty(Event event) {
        try {
            Object gunObject = event.getClass().getMethod("getGunItem").invoke(event);
            if (!(gunObject instanceof ItemStack gun)) {
                return;
            }
            boolean enhancedGun = GhrothTaczEnhancement.isEnhancedGun(gun);
            int enhancedAttachmentCount = GhrothTaczEnhancement.enhancedAttachmentCountInstalled(gun);
            if (!enhancedGun && enhancedAttachmentCount <= 0) {
                return;
            }
            Object cache = event.getClass().getMethod("getCacheProperty").invoke(event);
            if (cache == null) {
                return;
            }
            if (enhancedGun) {
                ResourceLocation gunId = GhrothTaczEnhancement.gunId(gun).orElse(null);
                multiplyDamageCache(cache, GhrothTaczEnhancement.enhancedGunDamageMultiplier(gun, gunId));
                multiplyFloatCache(cache, "HEADSHOT_MULTIPLIER",
                        (float) GhrothTaczEnhancement.enhancedHeadshotMultiplier(gun, gunId));
                multiplyFloatCache(cache, "EFFECTIVE_RANGE", 2.0F);
                multiplyFloatCache(cache, "AMMO_SPEED", 2.0F);
                addFloatCache(cache, "ARMOR_IGNORE", 1.0F);
                multiplyIntegerCache(cache, "ROUNDS_PER_MINUTE", 2.0D);
                multiplyIntegerCache(cache, "PIERCE", 2.0D);
                zeroFloatCache(cache, "ADS_TIME");
                zeroFloatCache(cache, "WEIGHT");
                zeroInaccuracyMapCache(cache, "INACCURACY");
                zeroInaccuracyMapCache(cache, "AIM_INACCURACY");
                maximizeRecoilControlCache(cache);
            }
            if (enhancedAttachmentCount > 0) {
                double multiplier = GhrothTaczEnhancement.enhancedAttachmentPropertyMultiplier(enhancedAttachmentCount);
                boostPositiveDamageCache(cache, multiplier);
                boostPositiveFloatCache(cache, "HEADSHOT_MULTIPLIER", multiplier, 1.0F, 1_000_000.0F);
                boostPositiveFloatCache(cache, "EFFECTIVE_RANGE", multiplier, 1.0F, 1_000_000.0F);
                boostPositiveFloatCache(cache, "ARMOR_IGNORE", multiplier, 1.0F, 100.0F);
                boostPositiveIntegerCache(cache, "ROUNDS_PER_MINUTE", multiplier, 1, 1_000_000);
                boostPositiveIntegerCache(cache, "PIERCE", multiplier, 1, 1_000_000);
                zeroFloatCache(cache, "ADS_TIME");
                zeroFloatCache(cache, "WEIGHT");
                zeroInaccuracyMapCache(cache, "INACCURACY");
                zeroInaccuracyMapCache(cache, "AIM_INACCURACY");
                maximizeRecoilControlCache(cache);
            }
        } catch (ReflectiveOperationException | LinkageError ignored) {
            // TaCZ is optional; changed property internals should not break the mod.
        }
    }

    private static void multiplyDamageCache(Object cache, double multiplier) throws ReflectiveOperationException {
        Object property = gunProperty("DAMAGE");
        Object value = cacheGet(cache, property);
        if (!(value instanceof Iterable<?> pairs)) {
            return;
        }
        Class<?> pairClass = Class.forName("com.tacz.guns.resource.pojo.data.gun.ExtraDamage$DistanceDamagePair");
        Method getDistance = pairClass.getMethod("getDistance");
        Method getDamage = pairClass.getMethod("getDamage");
        LinkedList<Object> adjusted = new LinkedList<>();
        for (Object pair : pairs) {
            if (pair == null || !pairClass.isAssignableFrom(pair.getClass())) {
                continue;
            }
            Object distanceValue = getDistance.invoke(pair);
            Object damageValue = getDamage.invoke(pair);
            if (!(distanceValue instanceof Number distance) || !(damageValue instanceof Number damage)) {
                continue;
            }
            adjusted.add(pairClass.getConstructor(float.class, float.class).newInstance(
                    distance.floatValue(),
                    GhrothTaczEnhancement.clampEnhancedDamage(damage.doubleValue() * multiplier)
            ));
        }
        if (!adjusted.isEmpty()) {
            cacheSet(cache, property, adjusted);
        }
    }

    private static void multiplyFloatCache(Object cache, String propertyName, float multiplier) throws ReflectiveOperationException {
        Object property = gunProperty(propertyName);
        Object value = cacheGet(cache, property);
        if (value instanceof Number number) {
            cacheSet(cache, property, number.floatValue() * multiplier);
        }
    }

    private static void boostPositiveDamageCache(Object cache, double multiplier) throws ReflectiveOperationException {
        Object property = gunProperty("DAMAGE");
        Object value = cacheGet(cache, property);
        if (!(value instanceof Iterable<?> pairs)) {
            return;
        }
        Class<?> pairClass = Class.forName("com.tacz.guns.resource.pojo.data.gun.ExtraDamage$DistanceDamagePair");
        Method getDistance = pairClass.getMethod("getDistance");
        Method getDamage = pairClass.getMethod("getDamage");
        LinkedList<Object> adjusted = new LinkedList<>();
        for (Object pair : pairs) {
            if (pair == null || !pairClass.isAssignableFrom(pair.getClass())) {
                continue;
            }
            Object distanceValue = getDistance.invoke(pair);
            Object damageValue = getDamage.invoke(pair);
            if (!(distanceValue instanceof Number distance) || !(damageValue instanceof Number damage)) {
                continue;
            }
            adjusted.add(pairClass.getConstructor(float.class, float.class).newInstance(
                    distance.floatValue(),
                    (float) positiveBoostValue(damage.doubleValue(), multiplier, 1.0D,
                            GhrothTaczEnhancement.ENHANCED_DAMAGE_LIMIT)
            ));
        }
        if (!adjusted.isEmpty()) {
            cacheSet(cache, property, adjusted);
        }
    }

    private static void boostPositiveFloatCache(Object cache, String propertyName, double multiplier,
                                                float fallbackValue, float maxValue) throws ReflectiveOperationException {
        Object property = gunProperty(propertyName);
        Object value = cacheGet(cache, property);
        if (value instanceof Number number) {
            cacheSet(cache, property, (float) positiveBoostValue(number.doubleValue(), multiplier, fallbackValue, maxValue));
        }
    }

    private static void boostPositiveIntegerCache(Object cache, String propertyName, double multiplier,
                                                  int fallbackValue, int maxValue) throws ReflectiveOperationException {
        Object property = gunProperty(propertyName);
        Object value = cacheGet(cache, property);
        if (value instanceof Number number) {
            double boosted = positiveBoostValue(number.doubleValue(), multiplier, fallbackValue, maxValue);
            cacheSet(cache, property, Math.max(fallbackValue, (int) Math.min(maxValue, Math.round(boosted))));
        }
    }

    private static double positiveBoostValue(double value, double multiplier, double fallbackValue, double maxValue) {
        double base = value > 0.0D && Double.isFinite(value) ? value : Math.max(fallbackValue, Math.abs(value));
        if (!Double.isFinite(base) || base <= 0.0D) {
            base = fallbackValue;
        }
        double boosted = base * Math.max(1.0D, multiplier);
        if (!Double.isFinite(boosted)) {
            boosted = maxValue;
        }
        return Math.max(0.0D, Math.min(maxValue, boosted));
    }

    private static void addFloatCache(Object cache, String propertyName, float addend) throws ReflectiveOperationException {
        Object property = gunProperty(propertyName);
        Object value = cacheGet(cache, property);
        if (value instanceof Number number) {
            cacheSet(cache, property, number.floatValue() + addend);
        }
    }

    private static void multiplyIntegerCache(Object cache, String propertyName, double multiplier) throws ReflectiveOperationException {
        Object property = gunProperty(propertyName);
        Object value = cacheGet(cache, property);
        if (value instanceof Number number) {
            cacheSet(cache, property, Math.max(0, (int) Math.round(number.doubleValue() * multiplier)));
        }
    }

    private static void zeroFloatCache(Object cache, String propertyName) throws ReflectiveOperationException {
        cacheSet(cache, gunProperty(propertyName), 0.0F);
    }

    private static void zeroInaccuracyMapCache(Object cache, String propertyName) throws ReflectiveOperationException {
        Object property = gunProperty(propertyName);
        Object value = cacheGet(cache, property);
        if (!(value instanceof Map<?, ?> map)) {
            return;
        }
        Map<Object, Float> zeroed = new HashMap<>();
        for (Object key : map.keySet()) {
            zeroed.put(key, 0.0F);
        }
        cacheSet(cache, property, zeroed);
    }

    private static void maximizeRecoilControlCache(Object cache) throws ReflectiveOperationException {
        try {
            Object property = gunProperty("RECOIL");
            Object current = cacheGet(cache, property);
            float pitchDefault = recoilDefaultValue(current, true);
            float yawDefault = recoilDefaultValue(current, false);
            List<Object> pitchModifiers = new ArrayList<>();
            List<Object> yawModifiers = new ArrayList<>();
            pitchModifiers.add(taczMultiplierModifier(0.0D));
            yawModifiers.add(taczMultiplierModifier(0.0D));
            Object pair = Class.forName("com.tacz.guns.api.modifier.ParameterizedCachePair")
                    .getMethod("of", List.class, List.class, Object.class, Object.class)
                    .invoke(null, pitchModifiers, yawModifiers, pitchDefault, yawDefault);
            cacheSet(cache, property, pair);
        } catch (ReflectiveOperationException | LinkageError ignored) {
            // Recoil cache type is version-sensitive; other property boosts remain valid.
        }
    }

    private static float recoilDefaultValue(Object recoilPair, boolean pitch) throws ReflectiveOperationException {
        if (recoilPair == null) {
            return 0.0F;
        }
        Object parameterizedCache = recoilPair.getClass().getMethod(pitch ? "left" : "right").invoke(recoilPair);
        Object value = parameterizedCache.getClass().getMethod("getDefaultValue").invoke(parameterizedCache);
        return value instanceof Number number ? number.floatValue() : 0.0F;
    }

    private static Object taczMultiplierModifier(double multiplier) throws ReflectiveOperationException {
        Object modifier = Class.forName("com.tacz.guns.resource.pojo.data.attachment.Modifier")
                .getConstructor()
                .newInstance();
        Field multiplierField = modifier.getClass().getDeclaredField("multiplier");
        multiplierField.setAccessible(true);
        multiplierField.setDouble(modifier, multiplier);
        return modifier;
    }

    private static Object gunProperty(String name) throws ReflectiveOperationException {
        return Class.forName("com.tacz.guns.api.GunProperties").getField(name).get(null);
    }

    private static Object cacheGet(Object cache, Object property) throws ReflectiveOperationException {
        Class<?> gunPropertyClass = Class.forName("com.tacz.guns.api.GunProperty");
        return cache.getClass().getMethod("getCache", gunPropertyClass).invoke(cache, property);
    }

    private static void cacheSet(Object cache, Object property, Object value) throws ReflectiveOperationException {
        Class<?> gunPropertyClass = Class.forName("com.tacz.guns.api.GunProperty");
        cache.getClass().getMethod("setCache", gunPropertyClass, Object.class).invoke(cache, property, value);
    }

    private static void replaceGhrothTaczDamageSources(Event event, ServerPlayer attacker) {
        try {
            Object bulletObject = event.getClass().getMethod("getBullet").invoke(event);
            Entity bullet = bulletObject instanceof Entity entity ? entity : attacker;
            DamageSource trueSource = SkillDamageHelper.trueDamage(attacker.serverLevel(), bullet, attacker);
            Class<?> partClass = Class.forName("com.tacz.guns.api.event.common.GunDamageSourcePart");
            Method setDamageSource = event.getClass().getMethod("setDamageSource", partClass, DamageSource.class);
            for (Object part : partClass.getEnumConstants()) {
                setDamageSource.invoke(event, part, trueSource);
            }
        } catch (ReflectiveOperationException | LinkageError ignored) {
            // DamageSource replacement is best-effort; base damage and floor compensation still apply.
        }
    }

    private static void recordGhrothDamageFloor(LivingEntity target, ServerPlayer attacker, float amount, boolean starsBonus) {
        if (amount <= 0.0F || !Float.isFinite(amount)) {
            return;
        }
        long expiresAt = target.level().getGameTime() + 2L;
        GHROTH_DAMAGE_FLOORS.put(target.getUUID(),
                new GhrothDamageFloor(attacker.getUUID(), amount, expiresAt, starsBonus));
    }

    private static void handleGhrothStarsGunFire(Event event) {
        LivingEntity shooter = taczEventEntity(event);
        if (!(shooter instanceof ServerPlayer attacker)
                || !GhrothStateManager.isGhroth(attacker)
                || !GhrothStateManager.isStarsActive(attacker)) {
            return;
        }
        ItemStack gun = taczEventGunItem(event);
        if (!isTaczGunStack(gun)) {
            return;
        }
        Optional<LivingEntity> target = GhrothStateManager.findStarsAimTarget(attacker);
        if (target.isEmpty()) {
            return;
        }
        float damage = GhrothTaczEnhancement.estimatedShotDamage(attacker, gun);
        if (damage <= 0.0F || !Float.isFinite(damage)) {
            return;
        }
        GhrothStarsPendingShot shot = new GhrothStarsPendingShot(
                target.get().getId(),
                damage,
                attacker.level().getGameTime() + 4L);
        GHROTH_STARS_PENDING_SHOTS
                .computeIfAbsent(attacker.getUUID(), ignored -> new LinkedList<>())
                .add(shot);
        attacker.serverLevel().getServer().tell(new TickTask(attacker.serverLevel().getServer().getTickCount() + 1,
                () -> resolveGhrothStarsPendingShot(attacker, shot)));
    }

    private static void consumeGhrothStarsPendingShot(ServerPlayer attacker) {
        LinkedList<GhrothStarsPendingShot> shots = GHROTH_STARS_PENDING_SHOTS.get(attacker.getUUID());
        if (shots == null) {
            return;
        }
        shots.poll();
        if (shots.isEmpty()) {
            GHROTH_STARS_PENDING_SHOTS.remove(attacker.getUUID());
        }
    }

    private static void resolveGhrothStarsPendingShot(ServerPlayer attacker, GhrothStarsPendingShot shot) {
        if (!removeGhrothStarsPendingShot(attacker.getUUID(), shot)) {
            return;
        }
        if (!attacker.isAlive() || attacker.level().getGameTime() > shot.expiresAt()) {
            return;
        }
        Entity targetEntity = attacker.level().getEntity(shot.targetId());
        if (targetEntity instanceof LivingEntity target) {
            GhrothStateManager.handleStarsGuaranteedShot(attacker, target, shot.damage());
        }
    }

    private static boolean removeGhrothStarsPendingShot(UUID attackerId, GhrothStarsPendingShot shot) {
        LinkedList<GhrothStarsPendingShot> shots = GHROTH_STARS_PENDING_SHOTS.get(attackerId);
        if (shots == null || !shots.remove(shot)) {
            return false;
        }
        if (shots.isEmpty()) {
            GHROTH_STARS_PENDING_SHOTS.remove(attackerId);
        }
        return true;
    }

    private static float reflectedFloat(Event event, String methodName, float fallback) throws ReflectiveOperationException {
        Object value = event.getClass().getMethod(methodName).invoke(event);
        return value instanceof Number number ? number.floatValue() : fallback;
    }

    private static boolean reflectedBoolean(Event event, String methodName, boolean fallback) throws ReflectiveOperationException {
        Object value = event.getClass().getMethod(methodName).invoke(event);
        return value instanceof Boolean bool ? bool : fallback;
    }

    private static void accelerateTaczAdrenalineTimers(Player player) {
        TaczSpeedMultipliers multipliers = taczSpeedMultipliers(player);
        if (!multipliers.hasChange()) {
            return;
        }
        if (player.level().isClientSide) {
            return;
        }
        shiftTaczServerDataHolder(player, multipliers);
    }

    private static void accelerateTaczAdrenalineForEvent(String eventName, Event event) {
        if (!TACZ_GUN_SHOOT_EVENT.equals(eventName) && !TACZ_GUN_FIRE_EVENT.equals(eventName)) {
            return;
        }
        LivingEntity entity = taczEventEntity(event);
        if (!(entity instanceof Player player)) {
            return;
        }
        TaczSpeedMultipliers multipliers = taczSpeedMultipliers(player);
        if (!multipliers.hasChange()) {
            return;
        }
        if (player.level().isClientSide) {
            shiftTaczClientDataHolder(player, multipliers);
        } else {
            shiftTaczServerDataHolder(player, multipliers);
        }
    }

    private static void snapshotMhsTaczAmmo(String eventName, Event event) {
        if (!TACZ_GUN_SHOOT_EVENT.equals(eventName) && !TACZ_GUN_FIRE_EVENT.equals(eventName)) {
            return;
        }
        LivingEntity entity = taczEventEntity(event);
        if (!(entity instanceof ServerPlayer player) || !isMhsFuryActive(player)) {
            return;
        }
        ItemStack gun = taczEventGunItem(event);
        if (isTaczGunStack(gun)) {
            rememberMhsTaczAmmo(player, gun, player.level().getGameTime() + 5L);
        }
    }

    private static TaczSpeedMultipliers taczSpeedMultipliers(LivingEntity entity) {
        double adrenaline = ToxikStateManager.adrenalineSpeedMultiplier(entity);
        double adrenalineFireRate = ToxikStateManager.adrenalineFireRateSpeedMultiplier(entity);
        double equipment = equipmentTaczAssaultMultiplier(entity);
        double ghrothFireRate = GhrothTaczEnhancement.fireRateMultiplier(entity);
        double ghrothReload = GhrothTaczEnhancement.reloadMultiplier(entity);
        double ghrothAim = GhrothTaczEnhancement.aimMultiplier(entity);
        double ghrothBolt = GhrothTaczEnhancement.boltMultiplier(entity);
        return new TaczSpeedMultipliers(
                adrenalineFireRate * equipment * ghrothFireRate * ModItemEffectHelper.medicineTaczFireRateMultiplier(entity),
                adrenaline * ghrothReload * ModItemEffectHelper.medicineTaczReloadMultiplier(entity)
                        * com.rzy.dealt_force_skills.character.wildfire.WildfireStateManager.reloadMultiplier(entity),
                adrenaline * equipment * ghrothAim * ModItemEffectHelper.medicineTaczAimSpeedMultiplier(entity),
                adrenaline * ghrothBolt,
                ModItemEffectHelper.medicineTaczAimPenaltyMultiplier(entity)
        );
    }

    public static double effectiveTaczFireRateMultiplier(LivingEntity entity) {
        return taczSpeedMultipliers(entity).fireRate();
    }

    public static double effectiveTaczReloadMultiplier(LivingEntity entity) {
        return taczSpeedMultipliers(entity).reload();
    }

    public static double effectiveTaczAimMultiplier(LivingEntity entity) {
        TaczSpeedMultipliers multipliers = taczSpeedMultipliers(entity);
        return multipliers.aim() / multipliers.aimPenalty();
    }

    private static void shiftTaczServerDataHolder(LivingEntity entity, TaczSpeedMultipliers multipliers) {
        try {
            Class<?> operatorClass = Class.forName(TACZ_GUN_OPERATOR);
            Object operator = operatorClass.getMethod("fromLivingEntity", LivingEntity.class).invoke(null, entity);
            Object dataHolder = operatorClass.getMethod("getDataHolder").invoke(operator);
            long fireExtraMillis = extraTaczMillis(multipliers.fireRate());
            long reloadExtraMillis = extraTaczMillis(multipliers.reload());
            long aimExtraMillis = extraTaczMillis(multipliers.aim());
            long boltExtraMillis = extraTaczMillis(multipliers.bolt());
            long aimPenaltyMillis = extraTaczMillis(multipliers.aimPenalty());
            // 射速：TaCZ 用 shootTimestamp 计算冷却，时间戳往过去推等价于冷却更快结束。
            shiftLongFieldIfNonNegative(dataHolder, "shootTimestamp", -fireExtraMillis);
            shiftLongFieldIfNonNegative(dataHolder, "lastShootTimestamp", -fireExtraMillis);
            shiftLongFieldIfNonNegative(dataHolder, "lockTimestamp", -Math.max(fireExtraMillis, Math.max(reloadExtraMillis, boltExtraMillis)));
            // 换弹/拉栓/切枪：这些也是时间戳驱动，正在进行时加速推进。
            if (isTaczReloading(dataHolder)) {
                shiftLongFieldIfNonNegative(dataHolder, "reloadTimestamp", -reloadExtraMillis);
            }
            if (booleanField(dataHolder, "isBolting")) {
                shiftLongFieldIfNonNegative(dataHolder, "boltTimestamp", -boltExtraMillis);
            }
            shiftLongFieldIfNonNegative(dataHolder, "drawTimestamp", -Math.max(0L, reloadExtraMillis / 2L));
            // 开镜：只改时间戳在部分事件顺序下体感不明显，所以额外直接推进 aimingProgress。
            if (booleanField(dataHolder, "isAiming")) {
                shiftLongFieldIfNonNegative(dataHolder, "aimingTimestamp", -aimExtraMillis + aimPenaltyMillis);
                boostFloatField(dataHolder, "aimingProgress", adrenalineAimProgressBoost(multipliers.aim()), 0.0F, 1.0F);
                reduceFloatField(dataHolder, "aimingProgress", taczAimPenaltyProgress(multipliers.aimPenalty()), 0.0F, 1.0F);
            }
        } catch (ReflectiveOperationException | LinkageError ignored) {
            // TACZ is optional; missing or changed APIs should not break this mod.
        }
    }

    private static void shiftTaczClientDataHolder(Player player, TaczSpeedMultipliers multipliers) {
        try {
            Class<?> clientOperatorClass = Class.forName(TACZ_CLIENT_GUN_OPERATOR);
            Method fromLocalPlayer = null;
            for (Method method : clientOperatorClass.getMethods()) {
                if ("fromLocalPlayer".equals(method.getName()) && method.getParameterCount() == 1) {
                    fromLocalPlayer = method;
                    break;
                }
            }
            if (fromLocalPlayer == null) {
                return;
            }
            Object operator = fromLocalPlayer.invoke(null, player);
            Object dataHolder = clientOperatorClass.getMethod("getDataHolder").invoke(operator);
            long fireExtraMillis = extraTaczMillis(multipliers.fireRate());
            long reloadExtraMillis = extraTaczMillis(multipliers.reload());
            long aimExtraMillis = extraTaczMillis(multipliers.aim());
            long boltExtraMillis = extraTaczMillis(multipliers.bolt());
            long aimPenaltyMillis = extraTaczMillis(multipliers.aimPenalty());
            // 客户端射速判定读取 clientShootTimestamp；服务端读取 shootTimestamp，两边都要推。
            shiftLongFieldIfNonNegative(dataHolder, "clientShootTimestamp", -fireExtraMillis);
            shiftLongFieldIfNonNegative(dataHolder, "clientLastShootTimestamp", -fireExtraMillis);
            // 客户端状态锁会挡住连射/换弹表现，也要同步加速释放。
            shiftLongFieldIfNonNegative(dataHolder, "lockTimestamp", -Math.max(fireExtraMillis, Math.max(reloadExtraMillis, boltExtraMillis)));
            if (booleanField(dataHolder, "isBolting")) {
                shiftLongFieldIfNonNegative(dataHolder, "boltTimestamp", -boltExtraMillis);
            }
            // 开镜表现读取 clientAimingProgress。参考牧羊人“周期性打断开镜”的反向思路，
            // 这里直接推进进度，避免只改 timestamp 但视觉仍然慢。
            if (booleanField(dataHolder, "clientIsAiming")) {
                shiftLongFieldIfNonNegative(dataHolder, "clientAimingTimestamp", -aimExtraMillis + aimPenaltyMillis);
                boostFloatField(dataHolder, "clientAimingProgress", adrenalineAimProgressBoost(multipliers.aim()), 0.0F, 1.0F);
                reduceFloatField(dataHolder, "clientAimingProgress", taczAimPenaltyProgress(multipliers.aimPenalty()), 0.0F, 1.0F);
            }
            // 蓄力武器也属于“枪械行为速度”，给充能进度补一份额外推进。
            if (booleanField(dataHolder, "isCharging")) {
                boostFloatField(dataHolder, "chargeProgress", adrenalineChargeProgressBoost(Math.max(multipliers.fireRate(), multipliers.aim())), 0.0F, Float.MAX_VALUE);
            }
            // 如果 TaCZ 正在等待异步射击记录，肾上腺素下不要让这个锁成为额外射速瓶颈。
            setBooleanFieldIfPresent(dataHolder, "isShootRecorded", true);
        } catch (ReflectiveOperationException | LinkageError ignored) {
            // Client-side TACZ bridge is best-effort and safe when TACZ is absent.
        }
    }

    private static float adrenalineAimProgressBoost(double multiplier) {
        // 20 TPS 下：基础 1 级约每 tick 额外 +1%，蛊自身 20 级约每 tick 额外 +20%。
        return (float) Math.min(0.5D, Math.max(0.0D, 0.08D * (multiplier - 1.0D)));
    }

    private static float adrenalineChargeProgressBoost(double multiplier) {
        return (float) Math.max(0.0D, 0.2D * (multiplier - 1.0D));
    }

    private static long extraTaczMillis(double multiplier) {
        return multiplier <= 1.0001D ? 0L : Math.max(1L, Math.round(50.0D * (multiplier - 1.0D)));
    }

    private static float taczAimPenaltyProgress(double multiplier) {
        return multiplier <= 1.0001D ? 0.0F : (float) Math.min(0.8D, 0.08D * (multiplier - 1.0D));
    }

    private record TaczSpeedMultipliers(double fireRate, double reload, double aim, double bolt, double aimPenalty) {
        private TaczSpeedMultipliers {
            fireRate = validMultiplier(fireRate);
            reload = validMultiplier(reload);
            aim = validMultiplier(aim);
            bolt = validMultiplier(bolt);
            aimPenalty = validMultiplier(aimPenalty);
        }

        private static double validMultiplier(double value) {
            return Double.isFinite(value) && value >= 1.0D ? value : 1.0D;
        }

        boolean hasChange() {
            return fireRate > 1.0001D || reload > 1.0001D || aim > 1.0001D || bolt > 1.0001D || aimPenalty > 1.0001D;
        }
    }

    private record TaczAmmoSnapshot(String gunId, int ammoCount, boolean bulletInBarrel, long expiresAt) {
    }

    private record GhrothDamageFloor(UUID attackerId, float amount, long expiresAt, boolean starsBonus) {
    }

    private record GhrothStarsPendingShot(int targetId, float damage, long expiresAt) {
    }

    private record GhrothNoonPendingCopy(int targetId, float damage, int copiesRemaining) {
    }

    private static void shiftLongFieldIfNonNegative(Object target, String fieldName, long delta) throws ReflectiveOperationException {
        if (target == null) {
            return;
        }
        Field field = fieldIfPresent(target, fieldName);
        if (field == null || field.getType() != long.class) {
            return;
        }
        long value = field.getLong(target);
        if (value >= 0L) {
            field.setLong(target, value + delta);
        }
    }

    private static boolean booleanField(Object target, String fieldName) throws ReflectiveOperationException {
        if (target == null) {
            return false;
        }
        Field field = fieldIfPresent(target, fieldName);
        return field != null && field.getType() == boolean.class && field.getBoolean(target);
    }

    private static void setBooleanFieldIfPresent(Object target, String fieldName, boolean value) throws ReflectiveOperationException {
        if (target == null) {
            return;
        }
        Field field = fieldIfPresent(target, fieldName);
        if (field != null && field.getType() == boolean.class) {
            field.setBoolean(target, value);
        }
    }

    private static void boostFloatField(Object target, String fieldName, float delta, float min, float max) throws ReflectiveOperationException {
        if (target == null || delta <= 0.0F) {
            return;
        }
        Field field = fieldIfPresent(target, fieldName);
        if (field == null || field.getType() != float.class) {
            return;
        }
        float value = field.getFloat(target);
        if (!Float.isFinite(value)) {
            return;
        }
        float boosted = Math.max(min, Math.min(max, value + delta));
        field.setFloat(target, boosted);
    }

    private static void reduceFloatField(Object target, String fieldName, float delta, float min, float max) throws ReflectiveOperationException {
        if (target == null || delta <= 0.0F) {
            return;
        }
        Field field = fieldIfPresent(target, fieldName);
        if (field == null || field.getType() != float.class) {
            return;
        }
        float value = field.getFloat(target);
        if (!Float.isFinite(value)) {
            return;
        }
        field.setFloat(target, Math.max(min, Math.min(max, value - delta)));
    }

    private static Field fieldIfPresent(Object target, String fieldName) {
        Class<?> type = target.getClass();
        while (type != null) {
            try {
                Field field = type.getField(fieldName);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            }
        }
        return null;
    }

    private static boolean isTaczReloading(Object dataHolder) throws ReflectiveOperationException {
        if (dataHolder == null) {
            return false;
        }
        Field field = fieldIfPresent(dataHolder, "reloadStateType");
        if (field == null) {
            return false;
        }
        Object state = field.get(dataHolder);
        if (state == null) {
            return false;
        }
        try {
            Method method = state.getClass().getMethod("isReloading");
            Object result = method.invoke(state);
            return result instanceof Boolean b && b;
        } catch (ReflectiveOperationException ignored) {
            return state.toString().contains("RELOAD");
        }
    }

    private static boolean isTaczSonicTimeEvent(String eventName) {
        return TACZ_GUN_SHOOT_EVENT.equals(eventName)
                || TACZ_GUN_FIRE_EVENT.equals(eventName)
                || TACZ_GUN_MELEE_EVENT.equals(eventName)
                || TACZ_GUN_FIRE_SELECT_EVENT.equals(eventName);
    }

    private static boolean isMovementOrLandingSound(ResourceLocation soundId) {
        String path = soundId.getPath();
        return path.endsWith(".step")
                || path.endsWith(".fall")
                || path.contains(".step.")
                || path.contains(".fall.")
                || path.contains("small_fall")
                || path.contains("big_fall")
                || path.contains("splash")
                || path.contains("swim");
    }

    private static void recordMorseTaczAction(Event event) {
        LivingEntity entity = taczEventEntity(event);
        if (entity instanceof ServerPlayer player) {
            MorseStateManager.recordPlayerAction(player);
        }
    }

    private static boolean cancelDownedTaczAction(Event event) {
        if (!event.isCancelable()) {
            return false;
        }
        LivingEntity entity = taczEventEntity(event);
        if (entity instanceof Player player
                && ((com.rzy.dealt_force_skills.character.rover.RoverStateManager.isDowned(player) || StingerStateManager.isDowned(player))
                || VlinderStateManager.isDowned(player)
                || CatDadStateManager.isDowned(player)
                || NikaidouHiroStateManager.isWeaponActive(player)
                || com.rzy.dealt_force_skills.character.wildfire.WildfireStateManager.blocksItems(player)
                || DepartmentOfTransportationStateManager.hasEquippedTool(player)
                || TempestStateManager.isActionLocked(player))) {
            event.setCanceled(true);
            cancelOptionalTaczReload(entity);
            return true;
        }
        return false;
    }

    private static boolean cancelLexNinjiaTaczAction(Event event) {
        if (!event.isCancelable()) {
            return false;
        }
        LivingEntity entity = taczEventEntity(event);
        if (!(entity instanceof ServerPlayer player) || !LexNinjiaStateManager.isLexNinjia(player)) {
            return false;
        }
        event.setCanceled(true);
        if (TACZ_GUN_RELOAD_EVENT.equals(event.getClass().getName())) {
            cancelOptionalTaczReload(player);
        }
        player.displayClientMessage(Component.translatable(
                "message.dealt_force_skills.lex_ninjia.tacz_forbidden"), true);
        return true;
    }

    private static boolean cancelUndeadTaczAction(Event event) {
        if (!event.isCancelable()) {
            return false;
        }
        LivingEntity entity = taczEventEntity(event);
        if (!(entity instanceof ServerPlayer player) || !UndeadStateManager.isUndead(player)) {
            return false;
        }
        event.setCanceled(true);
        if (TACZ_GUN_RELOAD_EVENT.equals(event.getClass().getName())) {
            cancelOptionalTaczReload(player);
        }
        player.displayClientMessage(Component.translatable(
                "message.dealt_force_skills.undead.tacz_forbidden"), true);
        return true;
    }

    private static boolean cancelFrozenTaczAction(Event event) {
        if (!event.isCancelable() || TACZ_GUN_FIRE_SELECT_EVENT.equals(event.getClass().getName())) {
            return false;
        }
        LivingEntity entity = taczEventEntity(event);
        if (entity instanceof Player player && RoundStartFreezeManager.isFrozen(player)) {
            event.setCanceled(true);
            cancelOptionalTaczReload(entity);
            return true;
        }
        return false;
    }

    private static boolean cancelManbaDuelTaczAction(Event event) {
        if (!event.isCancelable()) {
            return false;
        }

        LivingEntity maidCaster = taczEventEntity(event);
        if (isTaczSonicTimeEvent(event.getClass().getName())
                && maidCaster instanceof com.rzy.dealt_force_skills.compat.maid.MaidSkillHost maid
                && maid.dealtForceMaidSkills().weaponLocked()) {
            event.setCanceled(true);
            return true;
        }
        LivingEntity entity = taczEventEntity(event);
        if (entity instanceof ServerPlayer player
                && ManbaStateManager.isManba(player)
                && ManbaStateManager.isDuelActive(player)) {
            event.setCanceled(true);
            if (TACZ_GUN_RELOAD_EVENT.equals(event.getClass().getName())) {
                cancelOptionalTaczReload(player);
            }
            player.displayClientMessage(Component.translatable(
                    "message.dealt_force_skills.manba.tacz_forbidden_duel"), true);
            return true;
        }
        return false;
    }

    private static boolean throttleSonicTaczTimeEvent(Event event) {
        if (!event.isCancelable()) {
            return false;
        }
        LivingEntity entity = taczEventEntity(event);
        if (entity != null && effectApplies(entity, ModEffects.SONIC_SHOCK.get()) && shouldSonicShockSkipAction(entity)) {
            event.setCanceled(true);
            if (TACZ_GUN_RELOAD_EVENT.equals(event.getClass().getName())) {
                cancelOptionalTaczReload(entity);
            }
            return true;
        }
        return false;
    }

    private static boolean throttleMorseShockTaczTimeEvent(Event event) {
        if (!event.isCancelable()) {
            return false;
        }
        LivingEntity entity = taczEventEntity(event);
        if (entity != null && effectApplies(entity, ModEffects.MORSE_STRONG_SHOCK.get()) && shouldMorseShockSkipAction(entity)) {
            event.setCanceled(true);
            if (TACZ_GUN_RELOAD_EVENT.equals(event.getClass().getName())) {
                cancelOptionalTaczReload(entity);
            }
            return true;
        }
        return false;
    }

    private static LivingEntity taczEventEntity(Event event) {
        for (String methodName : new String[]{"getEntity", "getShooter"}) {
            try {
                Object entityObject = event.getClass().getMethod(methodName).invoke(event);
                if (entityObject instanceof LivingEntity entity) {
                    return entity;
                }
            } catch (ReflectiveOperationException ignored) {
                // Try the next TACZ event accessor.
            }
        }
        return null;
    }

    private static ItemStack taczEventGunItem(Event event) {
        try {
            Object stackObject = event.getClass().getMethod("getGunItemStack").invoke(event);
            if (stackObject instanceof ItemStack stack) {
                return stack;
            }
        } catch (ReflectiveOperationException ignored) {
            // TACZ is optional and may change event internals between versions.
        }
        return ItemStack.EMPTY;
    }

    private static void cancelTaczReloadEvent(Event event) {
        if (ChamberTaczEnhancement.isChamberWeapon(taczEventGunItem(event))) {
            event.setCanceled(true);
            return;
        }
        try {
            Object entityObject = event.getClass().getMethod("getEntity").invoke(event);
            if (entityObject instanceof LivingEntity entity
                    && (effectApplies(entity, ModEffects.WEBBED.get())
                    || effectApplies(entity, ModEffects.TEMPEST_DISARMED.get()))) {
                event.setCanceled(true);
            }
        } catch (ReflectiveOperationException ignored) {
            // TACZ is optional and may change event internals between versions.
        }
    }

    private static void lockWebbedHotbar(ServerPlayer player) {
        int lockedSlot = WEBBED_SELECTED_SLOTS.computeIfAbsent(player.getUUID(), id -> player.getInventory().selected);
        if (player.getInventory().selected != lockedSlot) {
            player.getInventory().selected = lockedSlot;
            player.connection.send(new ClientboundSetCarriedItemPacket(lockedSlot));
        }
    }

    private static void lockTempestActionHotbar(ServerPlayer player) {
        int lockedSlot = TEMPEST_ACTION_LOCKED_SELECTED_SLOTS.computeIfAbsent(player.getUUID(),
                id -> player.getInventory().selected);
        if (player.getInventory().selected != lockedSlot) {
            player.getInventory().selected = lockedSlot;
            player.connection.send(new ClientboundSetCarriedItemPacket(lockedSlot));
        }
    }

    private static void cooldownTempestDisarmedHands(ServerPlayer player) {
        if (!effectApplies(player, ModEffects.TEMPEST_DISARMED.get())) {
            return;
        }
        cooldownHeldItem(player, player.getMainHandItem());
        cooldownHeldItem(player, player.getOffhandItem());
    }

    private static void cooldownHeldItem(Player player, ItemStack stack) {
        if (!stack.isEmpty()) {
            player.getCooldowns().addCooldown(stack.getItem(), 3);
        }
    }

    private static void lockWebbedLook(ServerPlayer player) {
        LockedLook locked = WEBBED_LOOK_LOCKS.computeIfAbsent(player.getUUID(),
                id -> new LockedLook(player.getYRot(), player.getXRot()));
        player.setYRot(locked.yaw());
        player.setXRot(locked.pitch());
        player.setYHeadRot(locked.yaw());
        player.setYBodyRot(locked.yaw());
    }

    private static void cancelOptionalTaczReload(LivingEntity entity) {
        if (!ensureTaczReloadBridge()) {
            return;
        }
        try {
            Object operator = taczFromLivingEntity.invoke(null, entity);
            taczCancelReload.invoke(operator);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // Keep the optional TACZ bridge best-effort and non-fatal.
        }
    }

    private static boolean ensureTaczReloadBridge() {
        if (taczReloadBridgeChecked) {
            return taczReloadBridgeAvailable;
        }
        taczReloadBridgeChecked = true;
        try {
            Class<?> operatorClass = Class.forName(TACZ_GUN_OPERATOR);
            taczFromLivingEntity = operatorClass.getMethod("fromLivingEntity", LivingEntity.class);
            taczCancelReload = operatorClass.getMethod("cancelReload");
            taczReloadBridgeAvailable = true;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            taczReloadBridgeAvailable = false;
        }
        return taczReloadBridgeAvailable;
    }

    private static boolean tryAbsorbFrontShield(Player player, DamageSource source, Vec3 sourcePos, float amount) {
        if (!SinevaStateManager.isShieldDeployed(player)) {
            return false;
        }
        // Hemisphere check: absorb any damage whose source is in front of the player
        Vec3 forward = player.getLookAngle();
        Vec3 toSource = sourcePos.subtract(player.position().add(0, player.getBbHeight() * 0.5, 0));
        if (toSource.lengthSqr() < 0.0001D) {
            return true;
        }
        if (forward.dot(toSource.normalize()) <= 0.0D) {
            return false;
        }

        boolean viewportHit = isSinevaViewportHit(player, source, sourcePos);
        if (viewportHit && SinevaStateManager.viewportHealth(player) <= 0) {
            return false;
        }

        // Viewport hits wear glass; body hits wear shield durability (was previously never applied).
        boolean broken = viewportHit
                ? SinevaStateManager.damageViewport(player, amount)
                : SinevaStateManager.damageShield(player, amount);
        if (!player.level().isClientSide) {
            if (viewportHit) {
                SoundEvent glassSound = broken
                        ? SoundEvents.GLASS_BREAK
                        : net.minecraft.world.level.block.Blocks.GLASS.defaultBlockState()
                                .getSoundType().getHitSound();
                player.level().playSound(null, player.getEyePosition().x, player.getEyePosition().y,
                        player.getEyePosition().z, glassSound, SoundSource.PLAYERS,
                        broken ? 0.9f : 0.6f, broken ? 1.0f : 1.4f);
                // Hit feedback: notify attacker they hit the viewport
                if (source.getEntity() instanceof ServerPlayer attacker && player instanceof ServerPlayer sp) {
                    NetworkHandler.sendToPlayer(new S2C_CharacterHitFeedback(sp.getId(), amount), attacker);
                }
            } else {
                player.level().playSound(null, player.blockPosition(), ModSounds.SINEVA_SHIELD_HIT.get(),
                        SoundSource.PLAYERS, 0.9f, 1.0f);
                if (broken && player instanceof ServerPlayer serverPlayer) {
                    SinevaStateManager.syncToClient(serverPlayer);
                    SinevaStateManager.syncRenderStateToClients(serverPlayer);
                }
            }
        }
        if (viewportHit && broken && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.displayClientMessage(Component.translatable("message.dealt_force_skills.sineva.viewport_broken"), true);
            SinevaStateManager.syncToClient(serverPlayer);
            SinevaStateManager.syncRenderStateToClients(serverPlayer);
            // Notify the attacker who broke the viewport
            if (source.getEntity() instanceof ServerPlayer attacker) {
                attacker.displayClientMessage(
                        Component.translatable("message.dealt_force_skills.sineva.viewport_broken_by_you",
                                serverPlayer.getName()), true);
            }
        }
        return true;
    }

    @SubscribeEvent
    public static void onNativeSinevaJump(net.minecraftforge.event.entity.living.LivingEvent.LivingJumpEvent event) {
        if (event.getEntity() instanceof com.rzy.dealt_force_skills.ai.operative.OperativeHost bot
                && com.rzy.dealt_force_skills.ai.operative.NativeCommanderSkills.inherited(bot.skills(), com.rzy.dealt_force_skills.ai.operative.NativeSinevaSkills.class) instanceof com.rzy.dealt_force_skills.ai.operative.NativeSinevaSkills sineva
                && sineva.suitActive()) {
            Vec3 motion = bot.body().getDeltaMovement();
            bot.body().setDeltaMovement(motion.x, motion.y * SinevaStateManager.bombSuitJumpVelocityMultiplier(), motion.z);
        }
    }

    public static boolean isSinevaViewportHit(LivingEntity player, DamageSource source, Vec3 sourcePos) {
        Vec3 forward = player.getLookAngle();
        if (forward.lengthSqr() < 0.0001D) {
            return false;
        }
        forward = forward.normalize();

        Vec3 worldUp = new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 right = forward.cross(worldUp);
        if (right.lengthSqr() < 0.0001D) {
            right = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            right = right.normalize();
        }
        Vec3 up = right.cross(forward).normalize();

        Vec3 eye = player.getEyePosition().add(up.scale(SinevaShieldGeometry.VIEWPORT_CENTER_EYE_OFFSET));
        Vec3 viewportCenter = eye.add(forward.scale(SinevaShieldGeometry.VIEWPORT_FORWARD_OFFSET));

        if (!isInViewportRange(viewportCenter, forward, sourcePos)) {
            return false;
        }

        Entity direct = source.getDirectEntity();
        if (direct != null && direct != player) {
            Vec3 start = direct.position();
            Vec3 motion = direct.getDeltaMovement();
            if (motion.lengthSqr() > 0.0001D) {
                Vec3 end = start.add(motion);
                if (lineHitsViewport(start, end, viewportCenter, forward, right, up)
                        || lineHitsViewport(start.subtract(motion), end, viewportCenter, forward, right, up)) {
                    return true;
                }
            }

            Entity attacker = source.getEntity();
            if (attacker != null && attacker != direct) {
                return lineHitsViewport(entityAimOrigin(attacker), direct.position(), viewportCenter, forward, right, up);
            }
            return lineHitsViewport(sourcePos, playerBodyCenter(player), viewportCenter, forward, right, up);
        }

        Entity attacker = source.getEntity();
        Vec3 start = attacker != null ? entityAimOrigin(attacker) : sourcePos;
        return lineHitsViewport(start, playerBodyCenter(player), viewportCenter, forward, right, up);
    }

    private static boolean isInViewportRange(Vec3 viewportCenter, Vec3 forward, Vec3 sourcePos) {
        double distance = sourcePos.subtract(viewportCenter).dot(forward);
        return distance >= -0.25D && distance <= SinevaShieldGeometry.VIEWPORT_RAY_LENGTH;
    }

    private static boolean lineHitsViewport(
            Vec3 start,
            Vec3 end,
            Vec3 viewportCenter,
            Vec3 forward,
            Vec3 right,
            Vec3 up
    ) {
        Vec3 line = end.subtract(start);
        double denom = line.dot(forward);
        if (Math.abs(denom) < 0.0001D) {
            return false;
        }

        double t = viewportCenter.subtract(start).dot(forward) / denom;
        if (t < -0.05D || t > 1.05D) {
            return false;
        }

        Vec3 hit = start.add(line.scale(t));
        Vec3 local = hit.subtract(viewportCenter);
        return Math.abs(local.dot(right)) <= SinevaShieldGeometry.VIEWPORT_WORLD_WIDTH * 0.5D
                && Math.abs(local.dot(up)) <= SinevaShieldGeometry.VIEWPORT_WORLD_HEIGHT * 0.5D;
    }

    private static Vec3 entityAimOrigin(Entity entity) {
        if (entity instanceof LivingEntity living) {
            return living.getEyePosition();
        }
        return entity.position().add(0.0D, entity.getBbHeight() * 0.5D, 0.0D);
    }

    private static Vec3 playerBodyCenter(LivingEntity player) {
        return player.position().add(0.0D, player.getBbHeight() * 0.5D, 0.0D);
    }

    @SuppressWarnings("unused")
    private static boolean isSinevaViewportHitLegacy(Player player, Vec3 sourcePos) {
        // Hit only counts as "viewport" when the impact is at head height AND
        // the damage direction aligns tightly with the player's gaze.
        double eyeY = player.getEyeY();
        if (Math.abs(sourcePos.y - eyeY) > 0.3D) return false; // not at head level
        Vec3 eye = player.getEyePosition();
        Vec3 toSource = sourcePos.subtract(eye);
        double dist = toSource.length();
        if (dist < 0.01D) return true;
        Vec3 toSourceDir = toSource.scale(1.0 / dist);
        Vec3 look = player.getLookAngle();
        return look.dot(toSourceDir) > 0.97D; // ~14° cone
    }

    /** SpecialAbility only; night vision / thermal vision are NOT special abilities. */
    private static boolean armorSpecialsEnabled(Player player) {
        return ModGameRules.areArmorSpecialsEnabled(player);
    }

    private static SpecialAbility equipmentAbility(Player player, Profile profile) {
        if (profile == null || !armorSpecialsEnabled(player)) {
            return SpecialAbility.NONE;
        }
        return profile.ability();
    }

    private static float equipmentOutgoingDamageMultiplier(ServerPlayer attacker, LivingEntity target, DamageSource source) {
        if (!armorSpecialsEnabled(attacker)) {
            return 1.0F;
        }
        double multiplier = 1.0D;
        ItemStack chest = attacker.getItemBySlot(EquipmentSlot.CHEST);
        ItemStack head = attacker.getItemBySlot(EquipmentSlot.HEAD);
        Profile chestProfile = DfsEquipmentItem.profile(chest);
        Profile headProfile = DfsEquipmentItem.profile(head);

        if (equipmentAbility(attacker, chestProfile) == SpecialAbility.LONELY_DAMAGE
                && attacker.level().getEntitiesOfClass(Player.class,
                attacker.getBoundingBox().inflate(5.0D),
                other -> other != attacker && other.isAlive()).isEmpty()) {
            multiplier *= 1.75D;
            notifyEquipmentAbilityTrigger(attacker, chestProfile, ParticleTypes.CRIT);
        }
        if (equipmentAbility(attacker, chestProfile) == SpecialAbility.RED_OWL_REVENGE) {
            String revengeType = attacker.getPersistentData().getString(RED_OWL_REVENGE_TYPE);
            if (!revengeType.isBlank() && revengeType.equals(target.getType().getDescriptionId())) {
                multiplier *= 1.75D;
                notifyEquipmentAbilityTrigger(attacker, chestProfile, ParticleTypes.SOUL_FIRE_FLAME);
            }
        }
        if (equipmentAbility(attacker, chestProfile) == SpecialAbility.STATIONARY_STACKS) {
            multiplier *= 1.0D + ItemStackTagReads.getInt(chest, FS_STATIONARY_STACKS) * 0.04D;
        }
        if (equipmentAbility(attacker, headProfile) == SpecialAbility.STATIONARY_STACKS) {
            multiplier *= 1.0D + ItemStackTagReads.getInt(head, FS_STATIONARY_STACKS) * 0.04D;
        }
        if (equipmentAbility(attacker, headProfile) == SpecialAbility.GN_HEAVY) {
            multiplier *= 1.10D;
        }
        if (equipmentAbility(attacker, headProfile) == SpecialAbility.GN_HEAVY_NIGHT_VISION) {
            multiplier *= 1.20D;
        }
        if (equipmentAbility(attacker, headProfile) == SpecialAbility.DICH9_ASSAULT) {
            addTimedEquipmentStacks(head, DICH9_STACKS, DICH9_UNTIL, attacker.level().getGameTime(), 20, 7 * 20L);
            notifyEquipmentAbilityTrigger(attacker, headProfile, ParticleTypes.ELECTRIC_SPARK);
        }
        if (equipmentAbility(attacker, headProfile) == SpecialAbility.ELBOW_SPIRIT
                && chestProfile != null && chestProfile.id().equals("samurai_ballistic_vest")
                && isMeleeDamage(source, attacker)) {
            multiplier *= 12.45D;
            notifyEquipmentAbilityTrigger(attacker, headProfile, ParticleTypes.SWEEP_ATTACK);
        }
        return (float) multiplier;
    }

    private static float handleEquipmentDefense(ServerPlayer player, DamageSource source, float amount) {
        if (source instanceof com.rzy.dealt_force_skills.character.wildfire.WildfireDamageSource wildfire) {
            float bypass = amount * (float) wildfire.equipmentPenetration();
            return bypass + handleWildfireProtectedEquipmentDamage(player, source, amount - bypass);
        }
        return handleWildfireProtectedEquipmentDamage(player, source, amount);
    }

    private static float handleWildfireProtectedEquipmentDamage(ServerPlayer player, DamageSource source, float amount) {
        if (amount <= 0) return 0;
        // Fall, void and magic-like damage are not physical impacts.  They must not be
        // converted into equipment durability loss or prevented by the equipment layer.
        if (isEquipmentBypassDamage(source)) {
            return amount;
        }

        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        Profile chestProfile = DfsEquipmentItem.profile(chest);
        Profile headProfile = DfsEquipmentItem.profile(head);
        if (chestProfile == null && headProfile == null) {
            return amount;
        }

        if (tryRedOwlMaskDodge(player, source, headProfile)) {
            return 0.0F;
        }

        if (isMhsFuryActive(player)) {
            amount *= 0.05F;
            if (amount <= 0.0F) {
                return 0.0F;
            }
        }

        Double hitRatio = resolveEquipmentHitHeightRatio(player, source);
        boolean chestCovered = chestProfile != null && isEquipmentCoveredByRatio(chestProfile, hitRatio);
        boolean headCovered = headProfile != null && isEquipmentCoveredByRatio(headProfile, hitRatio);
        if (!chestCovered && !headCovered) {
            return applyLimbUnprotectedReduction(player, amount, hitRatio);
        }

        if (tryEquipmentHurtTriggers(player, source, amount, chest, chestProfile, chestCovered,
                head, headProfile, headCovered)) {
            return 0.0F;
        }

        float adjusted = applyEquipmentPassiveReductions(player, source, amount, chestProfile, chestCovered,
                headProfile, headCovered);
        if (adjusted <= 0.0F) {
            return 0.0F;
        }

        // Prefer absorb first (helmet then chest) so protection resolves before later modifiers starve durability.
        if (headCovered) {
            adjusted = absorbWithEquipment(player, head, headProfile, adjusted);
        }
        if (chestCovered) {
            adjusted = absorbWithEquipment(player, chest, chestProfile, adjusted);
        }
        if (adjusted <= 0.0F) {
            return 0.0F;
        }

        long now = player.level().getGameTime();
        if (adjusted >= player.getHealth()) {
            if (chestCovered && equipmentAbility(player, chestProfile) == SpecialAbility.FATAL_GUARD
                    && now >= player.getPersistentData().getLong(EQUIPMENT_FATAL_GUARD_UNTIL)) {
                player.getPersistentData().putLong(EQUIPMENT_FATAL_GUARD_UNTIL, now + 600L * 20L);
                player.invulnerableTime = Math.max(player.invulnerableTime, 10);
                notifyEquipmentAbilityTrigger(player, chestProfile, ParticleTypes.TOTEM_OF_UNDYING);
                return 0.0F;
            }
            if ((chestCovered || headCovered) && equipmentFactionCount(player, Faction.ASARA) >= 2
                    && now >= player.getPersistentData().getLong(ASARA_SET_FATAL_GUARD_UNTIL)) {
                player.getPersistentData().putLong(ASARA_SET_FATAL_GUARD_UNTIL, now + 360L * 20L);
                player.setHealth(player.getMaxHealth());
                com.rzy.dealt_force_skills.department.DepartmentEquipment.record(player,"armor.asara.inheritance",1);
                notifyEquipmentSetTrigger(player, Faction.ASARA, ParticleTypes.HEART);
                return 0.0F;
            }
        }
        return adjusted;
    }

    private static float handleSaeedGuardInheritedDefense(SaeedGuardEntity guard, DamageSource source, float amount) {
        Optional<ServerPlayer> owner = guard.owner();
        if (owner.isEmpty() || isEquipmentBypassDamage(source)) {
            return amount;
        }

        ServerPlayer player = owner.get();
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        Profile chestProfile = DfsEquipmentItem.profile(chest);
        Profile headProfile = DfsEquipmentItem.profile(head);
        if (chestProfile == null && headProfile == null) {
            return amount;
        }

        if (tryRedOwlMaskDodge(player, source, headProfile)) {
            return 0.0F;
        }

        if (isMhsFuryActive(player)) {
            amount *= 0.05F;
            if (amount <= 0.0F) {
                return 0.0F;
            }
        }

        Double hitRatio = resolveEquipmentHitHeightRatio(guard, source);
        boolean chestCovered = chestProfile != null && isEquipmentCoveredByRatio(chestProfile, hitRatio);
        boolean headCovered = headProfile != null && isEquipmentCoveredByRatio(headProfile, hitRatio);
        if (!chestCovered && !headCovered) {
            return applyLimbUnprotectedReduction(player, amount, hitRatio);
        }

        float adjusted = applyEquipmentPassiveReductions(player, source, amount, chestProfile, chestCovered,
                headProfile, headCovered);
        if (adjusted <= 0.0F) {
            return 0.0F;
        }

        if (headCovered) {
            adjusted = absorbWithEquipment(player, head, headProfile, adjusted);
        }
        if (chestCovered) {
            adjusted = absorbWithEquipment(player, chest, chestProfile, adjusted);
        }
        return Math.max(0.0F, adjusted);
    }

    private static boolean tryEquipmentHurtTriggers(ServerPlayer player, DamageSource source, float amount,
                                                    ItemStack chest, Profile chestProfile, boolean chestCovered,
                                                    ItemStack head, Profile headProfile, boolean headCovered) {
        if (!armorSpecialsEnabled(player)) {
            return false;
        }
        Entity attackerEntity = source.getEntity();
        LivingEntity livingAttacker = attackerEntity instanceof LivingEntity living ? living : null;
        Vec3 sourcePos = damageSourcePosition(source);

        if (chestCovered && equipmentAbility(player, chestProfile) == SpecialAbility.FRONT_IMMUNE
                && sourcePos != null && Util.isFromFront(player, sourcePos, 70.0F)
                && player.getRandom().nextFloat() < 0.25F) {
            notifyEquipmentAbilityTrigger(player, chestProfile, ParticleTypes.CRIT);
            return true;
        }
        if (chestCovered && equipmentAbility(player, chestProfile) == SpecialAbility.RANDOM_F4) {
            int roll = player.getRandom().nextInt(4);
            notifyEquipmentAbilityTrigger(player, chestProfile, ParticleTypes.ENCHANT);
            if (roll == 0) {
                return true;
            }
            if (roll == 1 && livingAttacker != null) {
                livingAttacker.setSecondsOnFire(5);
            }
            if (roll == 2 && livingAttacker != null) {
                livingAttacker.hurt(player.damageSources().playerAttack(player), amount);
            }
            if (roll == 3) {
                player.addEffect(new MobEffectInstance(ModEffects.STUN.get(), 10, 0, false, true, true));
                return true;
            }
        }
        if (headCovered && equipmentAbility(player, headProfile) == SpecialAbility.H09_RIOT_DODGE
                && chestProfile != null && chestProfile.id().equals("elite_ballistic_vest")
                && !ItemStackTagReads.getBoolean(head, EQUIPMENT_STATIONARY)
                && player.getRandom().nextFloat() < 0.75F) {
            notifyEquipmentAbilityTrigger(player, headProfile, ParticleTypes.CLOUD);
            return true;
        }
        if (chestCovered && equipmentAbility(player, chestProfile) == SpecialAbility.HEAVY_DODGE_SUMMON
                && livingAttacker != null && livingAttacker != player
                && player.getRandom().nextFloat() < 0.50F) {
            CompoundTag tag = chest.getOrCreateTag();
            long now = player.level().getGameTime();
            if (now >= tag.getLong(EQUIPMENT_HEAVY_DODGE_SUMMON_UNTIL)) {
                tag.putLong(EQUIPMENT_HEAVY_DODGE_SUMMON_UNTIL, now + 5L * 20L);
                spawnEquipmentSupport(player, livingAttacker, 2, true, true);
                notifyEquipmentAbilityTrigger(player, chestProfile, ParticleTypes.POOF);
            }
            return true;
        }
        if (chestCovered && equipmentAbility(player, chestProfile) == SpecialAbility.KING_KONG_EXECUTION
                && livingAttacker != null && livingAttacker != player
                && livingAttacker.distanceToSqr(player) <= 9.0D) {
            tryStartKingKongExecution(player, chest, livingAttacker);
        }
        if (chestCovered && equipmentAbility(player, chestProfile) == SpecialAbility.SUMMON_REINFORCEMENTS) {
            CompoundTag tag = chest.getOrCreateTag();
            long now = player.level().getGameTime();
            if (now >= tag.getLong(EQUIPMENT_SUMMON_UNTIL)) {
                tag.putLong(EQUIPMENT_SUMMON_UNTIL, now + 20L * 20L);
                spawnEquipmentSupport(player, livingAttacker, 1 + player.getRandom().nextInt(4), false);
                notifyEquipmentAbilityTrigger(player, chestProfile, ParticleTypes.HAPPY_VILLAGER);
            }
        }
        if (chestCovered && equipmentAbility(player, chestProfile) == SpecialAbility.TRICK_ASSAULT) {
            addTimedEquipmentStacks(chest, TRICK_STACKS, TRICK_UNTIL, player.level().getGameTime(), 20, 7 * 20L);
            notifyEquipmentAbilityTrigger(player, chestProfile, ParticleTypes.ELECTRIC_SPARK);
        }
        if (chestCovered) {
            triggerCounterKick(player, source, amount, chestProfile);
        }
        if (headCovered) {
            triggerCounterKick(player, source, amount, headProfile);
        }
        return false;
    }

    private static boolean tryRedOwlMaskDodge(ServerPlayer player, DamageSource source, Profile headProfile) {
        if (equipmentAbility(player, headProfile) != SpecialAbility.RED_OWL_MASK
                || player.getRandom().nextFloat() >= 0.50F) {
            return false;
        }
        Entity attackerEntity = source.getEntity();
        if (attackerEntity instanceof LivingEntity livingAttacker
                && livingAttacker != player && isTaczGunStack(player.getMainHandItem())) {
            lookAtAim(player, livingAttacker);
        }
        notifyEquipmentAbilityTrigger(player, headProfile, ParticleTypes.CLOUD);
        return true;
    }

    private static void triggerCounterKick(ServerPlayer player, DamageSource source, float amount, Profile profile) {
        if (equipmentAbility(player, profile) != SpecialAbility.COUNTER_KICK || !isMeleeDamage(source, source.getEntity())) {
            return;
        }
        Entity attacker = source.getEntity();
        if (!(attacker instanceof LivingEntity target) || target.distanceToSqr(player) > 4.0D
                || player.getRandom().nextFloat() >= 0.10F) {
            return;
        }
        double attackDamage = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        target.hurt(player.damageSources().playerAttack(player), (float) Math.max(amount, attackDamage * 3.0D));
        notifyEquipmentAbilityTrigger(player, profile, ParticleTypes.SWEEP_ATTACK);
    }

    private static float applyEquipmentPassiveReductions(ServerPlayer player, DamageSource source, float amount,
                                                         Profile chestProfile, boolean chestCovered,
                                                         Profile headProfile, boolean headCovered) {
        double multiplier = 1.0D;
        if (chestCovered) {
            multiplier *= 1.0D - Math.min(0.95D, chestProfile.kineticAbsorption());
            if (isMeleeDamage(source, source.getEntity())) {
                multiplier *= 1.0D - Math.min(0.95D, chestProfile.bluntResistance());
            }
        }
        if (headCovered) {
            multiplier *= 1.0D - Math.min(0.95D, headProfile.kineticAbsorption());
            if (isMeleeDamage(source, source.getEntity())) {
                multiplier *= 1.0D - Math.min(0.95D, headProfile.bluntResistance());
            }
        }
        if (headCovered && equipmentAbility(player, headProfile) == SpecialAbility.GN_HEAVY) {
            multiplier *= 0.95D;
        }
        if (headCovered && equipmentAbility(player, headProfile) == SpecialAbility.GN_HEAVY_NIGHT_VISION) {
            multiplier *= 0.90D;
        }
        if (headCovered && equipmentAbility(player, headProfile) == SpecialAbility.GT5_STATIONARY_REDUCTION
                && ItemStackTagReads.getBoolean(player.getItemBySlot(EquipmentSlot.HEAD), EQUIPMENT_STATIONARY)) {
            multiplier *= 0.20D;
            notifyEquipmentAbilityTrigger(player, headProfile, ParticleTypes.END_ROD);
        }
        return (float) (amount * Math.max(0.0D, multiplier));
    }

    private static float absorbWithEquipment(ServerPlayer player, ItemStack stack, Profile profile, float amount) {
        if (!Float.isFinite(amount) || amount <= 0.0F || stack.isEmpty()) return amount;
        boolean noDurabilityLoss = equipmentAbility(player, profile) == SpecialAbility.NO_DURABILITY_LOSS || stack.hasTag() && stack.getTag().getBoolean("Unbreakable");
        if (!noDurabilityLoss && !stack.isDamageableItem()) return amount;
        boolean broken = DfsEquipmentItem.isBroken(stack);
        boolean gtiOverload = canGtiOverloadEquipment(player, stack);
        if (!noDurabilityLoss && broken && !gtiOverload) return amount;
        boolean hvk = equipmentFactionCount(player, Faction.HVK) >= 1;
        double baseRatio = DfsEquipmentItem.qualityProtectionRatio(profile.quality());
        if (equipmentAbility(player, profile) == SpecialAbility.STATIONARY_STACKS) baseRatio += ItemStackTagReads.getInt(stack, FS_STATIONARY_STACKS) * 0.04D;
        double ratio = Math.min(0.98D, baseRatio + (hvk ? 0.04D : 0.0D));
        double desiredAbsorb = amount * ratio;
        if (desiredAbsorb <= 0.0D) return amount;
        int durabilityCost = adjustedEquipmentDurabilityCost(player, profile);
        double durabilityMultiplier = adjustedEquipmentDurabilityMultiplier(player, profile);
        double effectiveCost = Math.max(0.0D, durabilityCost * durabilityMultiplier);
        int durabilityLimit = gtiOverload ? gtiOverloadLimit(stack) : DfsEquipmentItem.breakDamageLimit(stack);
        int remaining = Math.max(0, durabilityLimit - stack.getDamageValue());
        double capacity = !noDurabilityLoss && effectiveCost > 0.0D ? remaining / effectiveCost : Double.POSITIVE_INFINITY;
        double absorbed = Math.min(desiredAbsorb, capacity);
        if (absorbed <= 0.0D) return amount;
        if (hvk) com.rzy.dealt_force_skills.department.DepartmentEquipment.record(player,"armor.hvk.absorbed",
                Math.max(0, absorbed - Math.min(amount * Math.min(0.98D,baseRatio),capacity)));
        if (!noDurabilityLoss && effectiveCost > 0.0D) {
            int rawCost = Math.max(1, (int)Math.ceil(absorbed * effectiveCost));
            int unbreaking = Math.max(0, EnchantmentHelper.getItemEnchantmentLevel(Enchantments.UNBREAKING,stack));
            int actualCost = Math.min(remaining, Math.max(1, (int)Math.ceil(rawCost / (double)(unbreaking + 1))));
            boolean skipped = shouldSkipEquipmentDurability(player);
            if (hvk) {
                int baselineRaw = Math.max(1, (int)Math.ceil(absorbed * (effectiveCost / 0.90D)));
                int baselineCost = Math.min(remaining, Math.max(1, (int)Math.ceil(baselineRaw / (double)(unbreaking + 1))));
                com.rzy.dealt_force_skills.department.DepartmentEquipment.record(player,"armor.hvk.durability_saved",
                        Math.max(0,baselineCost-(skipped?0:actualCost)));
            }
            if (skipped) {
                com.rzy.dealt_force_skills.department.DepartmentEquipment.record(player,"armor.hvk.durability_prevented",1);
                notifyEquipmentSetTrigger(player,Faction.HVK,ParticleTypes.WAX_ON);
            } else damageEquipmentStack(player,stack,profile,rawCost);
        } else if (noDurabilityLoss && equipmentAbility(player, profile) == SpecialAbility.NO_DURABILITY_LOSS) notifyEquipmentAbilityTrigger(player,profile,ParticleTypes.WAX_ON);
        return (float)Math.max(0.0D,amount-absorbed);
    }

    private static int adjustedEquipmentDurabilityCost(ServerPlayer player, Profile profile) {
        int cost = DfsEquipmentItem.qualityDurabilityCost(profile.quality());
        if (equipmentFactionCount(player, Faction.ASARA) >= 1) {
            cost -= 1;
        }
        return Math.max(1, cost);
    }

    private static double adjustedEquipmentDurabilityMultiplier(ServerPlayer player, Profile profile) {
        double multiplier = 1.0D;
        if (equipmentFactionCount(player, Faction.ASARA) >= 1) {
            multiplier *= 1.05D;
        }
        if (equipmentFactionCount(player, Faction.HVK) >= 1) {
            multiplier *= 0.90D;
        }
        if (equipmentAbility(player, profile) == SpecialAbility.DURABLE) {
            multiplier *= 0.50D;
        }
        if (equipmentAbility(player, profile) == SpecialAbility.GN_HEAVY) {
            multiplier *= 0.80D;
        }
        if (equipmentAbility(player, profile) == SpecialAbility.GN_HEAVY_NIGHT_VISION) {
            multiplier *= 0.70D;
        }
        return multiplier;
    }

    private static boolean shouldSkipEquipmentDurability(ServerPlayer player) {
        return equipmentFactionCount(player, Faction.HVK) >= 2 && player.getRandom().nextFloat() < 0.25F;
    }

    private static boolean canGtiOverloadEquipment(ServerPlayer player, ItemStack stack) {
        return equipmentFactionCount(player, Faction.GTI) >= 2
                && DfsEquipmentItem.profile(stack) != null
                && stack.isDamageableItem()
                && stack.getDamageValue() < gtiOverloadLimit(stack);
    }

    private static int gtiOverloadLimit(ItemStack stack) {
        return Math.max(stack.getMaxDamage() + 1, (int) Math.ceil(stack.getMaxDamage() * GTI_OVERLOAD_LIMIT_MULTIPLIER));
    }

    private static void damageEquipmentStack(ServerPlayer player, ItemStack stack, Profile profile, int rawCost) {
        int unbreaking = Math.max(0, EnchantmentHelper.getItemEnchantmentLevel(Enchantments.UNBREAKING, stack));
        int cost = Math.max(1, (int) Math.ceil(rawCost / (double) (unbreaking + 1)));
        boolean gtiOverload = canGtiOverloadEquipment(player, stack);
        int damageLimit = gtiOverload ? gtiOverloadLimit(stack) : DfsEquipmentItem.breakDamageLimit(stack);
        int oldDamage = stack.getDamageValue();
        int newDamage = Math.min(damageLimit, stack.getDamageValue() + cost);
        stack.setDamageValue(newDamage);
        if (gtiOverload && oldDamage < DfsEquipmentItem.breakDamageLimit(stack)
                && newDamage >= DfsEquipmentItem.breakDamageLimit(stack)) {
            notifyEquipmentSetTrigger(player, Faction.GTI, ParticleTypes.ELECTRIC_SPARK);
        }
        if (gtiOverload && newDamage >= damageLimit) {
            com.rzy.dealt_force_skills.department.DepartmentEquipment.record(player,"armor.gti.destroyed_absorbing",1);
            stack.shrink(1);
            return;
        }
        if (newDamage >= DfsEquipmentItem.breakDamageLimit(stack)
                && equipmentAbility(player, profile) == SpecialAbility.DURABILITY_RESTORE) {
            CompoundTag tag = stack.getOrCreateTag();
            long now = player.level().getGameTime();
            if (now >= tag.getLong(EQUIPMENT_RESTORE_UNTIL)) {
                stack.setDamageValue(0);
                tag.putLong(EQUIPMENT_RESTORE_UNTIL, now + 1800L * 20L);
                notifyEquipmentAbilityTrigger(player, profile, ParticleTypes.HAPPY_VILLAGER);
            }
        }
    }

    private static void notifyEquipmentAbilityTrigger(ServerPlayer player, Profile profile,
                                                       ParticleOptions particle) {
        if (player == null || profile == null) {
            return;
        }
        notifyEquipmentTrigger(player, "ability." + profile.id(),
                Component.translatable("message.dealt_force_skills.equipment.ability_trigger",
                        Component.translatable("tooltip.dealt_force_skills." + profile.id() + ".ability")), particle);
    }

    private static void notifyEquipmentSetTrigger(ServerPlayer player, Faction faction, ParticleOptions particle) {
        if (player == null || faction == null) {
            return;
        }
        notifyEquipmentTrigger(player, "set." + faction.key(),
                Component.translatable("message.dealt_force_skills.equipment.set_trigger",
                        Component.translatable("faction.dealt_force_skills." + faction.key())), particle);
    }

    private static void notifyEquipmentTrigger(ServerPlayer player, String id, Component message,
                                               ParticleOptions particle) {
        long now = player.level().getGameTime();
        String cooldownKey = EQUIPMENT_FEEDBACK_UNTIL + "." + id;
        if (now < player.getPersistentData().getLong(cooldownKey)) {
            return;
        }
        player.getPersistentData().putLong(cooldownKey, now + 10L);
        player.displayClientMessage(message, true);
        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(particle, player.getX(), player.getY() + player.getBbHeight() * 0.55D,
                    player.getZ(), 14, 0.35D, 0.45D, 0.35D, 0.03D);
        }
    }

    private static void tickHelmetVisionEffect(ServerPlayer player, ItemStack head) {
        int visionMode = DfsEquipmentItem.activeVisionMode(head);
        if (visionMode == DfsEquipmentItem.VISION_NIGHT) {
            MobEffectInstance current = player.getEffect(MobEffects.NIGHT_VISION);
            if (current == null || current.getDuration() < 220) {
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 260, 0, true, false, true), player);
            }
            return;
        }

        MobEffectInstance current = player.getEffect(MobEffects.NIGHT_VISION);
        if (current != null && current.getAmplifier() == 0 && current.isAmbient()
                && !current.isVisible() && current.getDuration() <= 280) {
            player.removeEffect(MobEffects.NIGHT_VISION);
        }
    }

    private static void tickDfsEquipment(ServerPlayer player) {
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        Profile headProfile = DfsEquipmentItem.profile(head);
        Profile chestProfile = DfsEquipmentItem.profile(chest);

        // Helmet night vision / thermal vision are NOT special abilities — always tick.
        tickHelmetVisionEffect(player, head);
        if (equipmentFactionCount(player, Faction.GLOBAL_FORCES) >= 1 && player.tickCount % 100 == 0) {
            int beforeRepair = head.getDamageValue() + chest.getDamageValue();
            if (regenerateEquipmentDurability(head) | regenerateEquipmentDurability(chest)) {
                com.rzy.dealt_force_skills.department.DepartmentTasks.signal(player,"armor.global.restored",
                        Math.max(0,beforeRepair-head.getDamageValue()-chest.getDamageValue()));
                notifyEquipmentSetTrigger(player, Faction.GLOBAL_FORCES, ParticleTypes.HAPPY_VILLAGER);
            }
        }
        // Base protection and handling stay; abilities and faction set bonuses respect dealtarmorspecial.
        tickGlobalForcesAllyLock(player);
        if (armorSpecialsEnabled(player)) {
            tickEquipmentStationaryFlag(player, head);
            tickEquipmentStationaryFlag(player, chest);
            if (equipmentAbility(player, chestProfile) == SpecialAbility.STATIONARY_STACKS) {
                tickFsStationaryStacks(player, chest);
            }
            if (equipmentAbility(player, headProfile) == SpecialAbility.STATIONARY_STACKS) {
                tickFsStationaryStacks(player, head);
            }
            if (equipmentAbility(player, headProfile) == SpecialAbility.H09_RIOT_DODGE
                    && chestProfile != null && chestProfile.id().equals("elite_ballistic_vest")) {
                tickH09StationaryDamage(player, head);
            }
            if (equipmentAbility(player, headProfile) == SpecialAbility.ENDURANCE_TRANSFORM) {
                tickEnduranceHelmetTransform(player, head);
            }
            if (equipmentAbility(player, chestProfile) == SpecialAbility.TRICK_ASSAULT
                    && player.level().getGameTime() > ItemStackTagReads.getLong(chest, TRICK_UNTIL)
                    && chest.getTag() != null) {
                chest.getTag().remove(TRICK_STACKS);
            }
            if (equipmentAbility(player, headProfile) == SpecialAbility.DICH9_ASSAULT
                    && player.level().getGameTime() > ItemStackTagReads.getLong(head, DICH9_UNTIL)
                    && head.getTag() != null) {
                head.getTag().remove(DICH9_STACKS);
            }
            tickMhsFury(player, head, headProfile);
            updateEquipmentMovementBonus(player, chest, chestProfile, head, headProfile);
            tickKingKongExecution(player);
        } else {
            // Clear ability-sourced movement modifiers when specials are disabled.
            updateEquipmentMovementBonus(player, chest, null, head, null);
        }
        GhrothTaczEnhancement.tickEnhancedGunRuntime(player);
    }

    private static void tickGlobalForcesAllyLock(ServerPlayer player) {
        CompoundTag tag = player.getPersistentData();
        if (equipmentFactionCount(player, Faction.GLOBAL_FORCES) < 2) {
            clearGlobalForcesLock(tag);
            return;
        }

        if (tag.hasUUID(GLOBAL_FORCES_LOCK_TARGET)) {
            ServerPlayer locked = player.getServer().getPlayerList().getPlayer(tag.getUUID(GLOBAL_FORCES_LOCK_TARGET));
            if (!isValidGlobalForcesAlly(player, locked)) {
                tag.remove(GLOBAL_FORCES_LOCK_TARGET);
            }
        }

        if (!player.isShiftKeyDown()) {
            clearGlobalForcesLockProgress(tag);
            return;
        }

        ServerPlayer target = findGlobalForcesLookTarget(player);
        if (target == null) {
            clearGlobalForcesLockProgress(tag);
            return;
        }

        UUID targetId = target.getUUID();
        int ticks = 1;
        if (tag.hasUUID(GLOBAL_FORCES_LOCK_PROGRESS_TARGET)
                && tag.getUUID(GLOBAL_FORCES_LOCK_PROGRESS_TARGET).equals(targetId)) {
            ticks = tag.getInt(GLOBAL_FORCES_LOCK_PROGRESS_TICKS) + 1;
        } else {
            tag.putUUID(GLOBAL_FORCES_LOCK_PROGRESS_TARGET, targetId);
        }
        tag.putInt(GLOBAL_FORCES_LOCK_PROGRESS_TICKS, ticks);

        if (ticks >= GLOBAL_FORCES_LOCK_TICKS_REQUIRED) {
            boolean changedTarget = !tag.hasUUID(GLOBAL_FORCES_LOCK_TARGET)
                    || !tag.getUUID(GLOBAL_FORCES_LOCK_TARGET).equals(targetId);
            tag.putUUID(GLOBAL_FORCES_LOCK_TARGET, targetId);
            clearGlobalForcesLockProgress(tag);
            if (changedTarget) {
                player.displayClientMessage(Component.translatable(
                        "message.dealt_force_skills.global_forces.locked", target.getDisplayName()), true);
            }
        }
    }

    private static float tryAbsorbGlobalForcesAllyDamage(ServerPlayer victim, DamageSource source, float amount) {
        if (amount <= 0.0F || isEquipmentBypassDamage(source)) {
            return amount;
        }
        float adjusted = amount;
        for (ServerPlayer protector : victim.serverLevel().players()) {
            if (!isGlobalForcesProtecting(protector, victim)) {
                continue;
            }
            float before = adjusted;
            adjusted = absorbWithGlobalForcesAllyEquipment(protector, adjusted);
            if (adjusted < before) {
                notifyEquipmentSetTrigger(protector, Faction.GLOBAL_FORCES, ParticleTypes.HEART);
            }
            if (adjusted <= 0.0F) {
                return 0.0F;
            }
        }
        return adjusted;
    }

    private static boolean isGlobalForcesProtecting(ServerPlayer protector, ServerPlayer victim) {
        if (protector == victim || equipmentFactionCount(protector, Faction.GLOBAL_FORCES) < 2) {
            return false;
        }
        CompoundTag tag = protector.getPersistentData();
        return tag.hasUUID(GLOBAL_FORCES_LOCK_TARGET)
                && tag.getUUID(GLOBAL_FORCES_LOCK_TARGET).equals(victim.getUUID())
                && isValidGlobalForcesAlly(protector, victim);
    }

    private static float absorbWithGlobalForcesAllyEquipment(ServerPlayer protector, float amount) {
        ItemStack head = protector.getItemBySlot(EquipmentSlot.HEAD);
        Profile headProfile = DfsEquipmentItem.profile(head);
        if (headProfile != null) {
            amount = absorbWithEquipment(protector, head, headProfile, amount);
        }
        if (amount <= 0.0F) {
            return 0.0F;
        }
        ItemStack chest = protector.getItemBySlot(EquipmentSlot.CHEST);
        Profile chestProfile = DfsEquipmentItem.profile(chest);
        if (chestProfile != null) {
            amount = absorbWithEquipment(protector, chest, chestProfile, amount);
        }
        return amount;
    }

    private static ServerPlayer findGlobalForcesLookTarget(ServerPlayer player) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        Vec3 end = eye.add(look.scale(GLOBAL_FORCES_ALLY_RANGE));
        AABB search = player.getBoundingBox().expandTowards(look.scale(GLOBAL_FORCES_ALLY_RANGE))
                .inflate(GLOBAL_FORCES_LOCK_RAY_RADIUS);
        ServerPlayer best = null;
        double bestDistance = Double.MAX_VALUE;
        for (ServerPlayer target : player.serverLevel().getEntitiesOfClass(ServerPlayer.class, search,
                target -> isValidGlobalForcesAlly(player, target) && player.hasLineOfSight(target))) {
            double distanceToRay = distanceToSegment(target.getBoundingBox().getCenter(), eye, end);
            if (distanceToRay > GLOBAL_FORCES_LOCK_RAY_RADIUS) {
                continue;
            }
            double eyeDistance = eye.distanceToSqr(target.getEyePosition());
            if (eyeDistance < bestDistance) {
                bestDistance = eyeDistance;
                best = target;
            }
        }
        return best;
    }

    private static boolean isValidGlobalForcesAlly(ServerPlayer player, ServerPlayer target) {
        return target != null
                && target != player
                && target.isAlive()
                && !target.isSpectator()
                && target.level() == player.level()
                && player.distanceToSqr(target) <= GLOBAL_FORCES_ALLY_RANGE * GLOBAL_FORCES_ALLY_RANGE
                && player.isAlliedTo(target);
    }

    private static double distanceToSegment(Vec3 point, Vec3 start, Vec3 end) {
        Vec3 segment = end.subtract(start);
        double lengthSqr = segment.lengthSqr();
        if (lengthSqr < 1.0E-6D) {
            return point.distanceTo(start);
        }
        double t = point.subtract(start).dot(segment) / lengthSqr;
        t = Math.max(0.0D, Math.min(1.0D, t));
        return point.distanceTo(start.add(segment.scale(t)));
    }

    private static void clearGlobalForcesLock(CompoundTag tag) {
        tag.remove(GLOBAL_FORCES_LOCK_TARGET);
        clearGlobalForcesLockProgress(tag);
    }

    private static void clearGlobalForcesLockProgress(CompoundTag tag) {
        tag.remove(GLOBAL_FORCES_LOCK_PROGRESS_TARGET);
        tag.remove(GLOBAL_FORCES_LOCK_PROGRESS_TICKS);
    }

    private static boolean regenerateEquipmentDurability(ItemStack stack) {
        if (stack.isEmpty() || !stack.isDamageableItem() || !stack.isDamaged()) {
            return false;
        }
        int repair = Math.max(1, stack.getMaxDamage() / 100);
        stack.setDamageValue(Math.max(0, stack.getDamageValue() - repair));
        return true;
    }

    private static void tickFsStationaryStacks(ServerPlayer player, ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        Vec3 position = player.position();
        if (!tag.contains(FS_LAST_X)) {
            tag.putDouble(FS_LAST_X, position.x);
            tag.putDouble(FS_LAST_Y, position.y);
            tag.putDouble(FS_LAST_Z, position.z);
            return;
        }

        Vec3 previous = new Vec3(tag.getDouble(FS_LAST_X), tag.getDouble(FS_LAST_Y), tag.getDouble(FS_LAST_Z));
        boolean stationary = previous.distanceToSqr(position) < 0.0025D;
        tag.putDouble(FS_LAST_X, position.x);
        tag.putDouble(FS_LAST_Y, position.y);
        tag.putDouble(FS_LAST_Z, position.z);

        if (stationary) {
            int stationaryTicks = tag.getInt(FS_STATIONARY_TICKS) + 1;
            tag.putInt(FS_STATIONARY_TICKS, stationaryTicks);
            tag.putInt(FS_MOVING_TICKS, 0);
            if (stationaryTicks >= 5 * 20) {
                int oldStacks = tag.getInt(FS_STATIONARY_STACKS);
                tag.putInt(FS_STATIONARY_STACKS, Math.min(8, oldStacks + 1));
                tag.putInt(FS_STATIONARY_TICKS, 0);
                if (oldStacks < 8) {
                    notifyEquipmentAbilityTrigger(player, DfsEquipmentItem.profile(stack), ParticleTypes.END_ROD);
                }
            }
        } else {
            int movingTicks = tag.getInt(FS_MOVING_TICKS) + 1;
            tag.putInt(FS_MOVING_TICKS, movingTicks);
            tag.putInt(FS_STATIONARY_TICKS, 0);
            if (movingTicks >= 10 * 20) {
                tag.putInt(FS_STATIONARY_STACKS, Math.max(0, tag.getInt(FS_STATIONARY_STACKS) - 1));
                tag.putInt(FS_MOVING_TICKS, 0);
            }
        }
    }

    private static void tickEquipmentStationaryFlag(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        CompoundTag tag = stack.getOrCreateTag();
        Vec3 position = player.position();
        if (!tag.contains(EQUIPMENT_LAST_X)) {
            tag.putDouble(EQUIPMENT_LAST_X, position.x);
            tag.putDouble(EQUIPMENT_LAST_Y, position.y);
            tag.putDouble(EQUIPMENT_LAST_Z, position.z);
            tag.putBoolean(EQUIPMENT_STATIONARY, true);
            return;
        }
        Vec3 previous = new Vec3(tag.getDouble(EQUIPMENT_LAST_X), tag.getDouble(EQUIPMENT_LAST_Y), tag.getDouble(EQUIPMENT_LAST_Z));
        tag.putDouble(EQUIPMENT_LAST_X, position.x);
        tag.putDouble(EQUIPMENT_LAST_Y, position.y);
        tag.putDouble(EQUIPMENT_LAST_Z, position.z);
        tag.putBoolean(EQUIPMENT_STATIONARY, previous.distanceToSqr(position) < 0.0025D);
    }

    private static void tickH09StationaryDamage(ServerPlayer player, ItemStack head) {
        CompoundTag tag = head.getOrCreateTag();
        if (!tag.getBoolean(EQUIPMENT_STATIONARY)) {
            tag.putInt(EQUIPMENT_STATIONARY_DAMAGE_TICKS, 0);
            return;
        }
        int ticks = tag.getInt(EQUIPMENT_STATIONARY_DAMAGE_TICKS) + 1;
        if (ticks < 20) {
            tag.putInt(EQUIPMENT_STATIONARY_DAMAGE_TICKS, ticks);
            return;
        }
        tag.putInt(EQUIPMENT_STATIONARY_DAMAGE_TICKS, 0);
        player.hurt(player.damageSources().generic(), Math.max(H09_STATIONARY_MIN_SELF_DAMAGE,
                player.getMaxHealth() * H09_STATIONARY_SELF_DAMAGE_MAX_HEALTH_FRACTION));
    }

    private static void tickEnduranceHelmetTransform(ServerPlayer player, ItemStack head) {
        CompoundTag tag = head.getOrCreateTag();
        if (head.isDamaged()) {
            tag.putInt(EQUIPMENT_STATIONARY_DAMAGE_TICKS, 0);
            return;
        }
        int wornTicks = tag.getInt(EQUIPMENT_STATIONARY_DAMAGE_TICKS) + 1;
        if (wornTicks < 180 * 20) {
            tag.putInt(EQUIPMENT_STATIONARY_DAMAGE_TICKS, wornTicks);
            return;
        }
        ItemStack replacement = new ItemStack(ModItems.GN_HEAVY_NIGHT_VISION_HELMET.get());
        if (head.hasTag()) {
            replacement.setTag(head.getTag().copy());
            replacement.setDamageValue(0);
        }
        notifyEquipmentAbilityTrigger(player, DfsEquipmentItem.profile(head), ParticleTypes.TOTEM_OF_UNDYING);
        player.setItemSlot(EquipmentSlot.HEAD, replacement);
    }

    private static void tickMhsFury(ServerPlayer player, ItemStack head, Profile headProfile) {
        if (!armorSpecialsEnabled(player)) {
            MHS_TACTICAL_AMMO_SNAPSHOTS.remove(player.getUUID());
            return;
        }
        long now = player.level().getGameTime();
        boolean active = equipmentAbility(player, headProfile) == SpecialAbility.MHS_FURY
                && now <= ItemStackTagReads.getLong(head, MHS_FURY_UNTIL);

        if (!active && equipmentAbility(player, headProfile) == SpecialAbility.MHS_FURY
                && isTaczGunStack(player.getMainHandItem()) && hasMhsFuryTriggerTarget(player)) {
            CompoundTag tag = head.getOrCreateTag();
            tag.putLong(MHS_FURY_UNTIL, now + MHS_FURY_DURATION_TICKS);
            tag.putInt(MHS_FURY_SLOT, player.getInventory().selected);
            active = true;
            notifyEquipmentAbilityTrigger(player, headProfile, ParticleTypes.ANGRY_VILLAGER);
        }

        if (!active) {
            MHS_TACTICAL_AMMO_SNAPSHOTS.remove(player.getUUID());
            return;
        }

        lockMhsFuryHotbar(player, head);
        Vec3 velocity = player.getDeltaMovement();
        player.setDeltaMovement(0.0D, Math.min(velocity.y, 0.0D), 0.0D);
        player.setSprinting(false);
        player.hurtMarked = true;
        if (player.containerMenu != player.inventoryMenu) {
            player.closeContainer();
        }

        ItemStack gun = player.getMainHandItem();
        if (isTaczGunStack(gun)) {
            rememberMhsTaczAmmo(player, gun, now + 5L);
            restoreMhsTaczAmmo(player, gun, now);
        }
    }

    private static boolean hasMhsFuryTriggerTarget(ServerPlayer player) {
        AABB area = player.getBoundingBox().inflate(MHS_FURY_TRIGGER_RANGE);
        for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != player && entity.isAlive() && !entity.isSpectator()
                        && entity.getType() != EntityType.ARMOR_STAND
                        && TargetingUtil.isHostileLivingFor(player, entity))) {
            if (target.distanceToSqr(player) <= MHS_FURY_TRIGGER_RANGE * MHS_FURY_TRIGGER_RANGE) {
                return true;
            }
        }
        return false;
    }

    private static void lockMhsFuryHotbar(ServerPlayer player, ItemStack head) {
        CompoundTag tag = head.getTag();
        int lockedSlot = tag != null && tag.contains(MHS_FURY_SLOT) ? tag.getInt(MHS_FURY_SLOT) : player.getInventory().selected;
        if (lockedSlot < 0 || lockedSlot > 8) {
            lockedSlot = player.getInventory().selected;
            head.getOrCreateTag().putInt(MHS_FURY_SLOT, lockedSlot);
        }
        if (player.getInventory().selected != lockedSlot) {
            player.getInventory().selected = lockedSlot;
            player.connection.send(new ClientboundSetCarriedItemPacket(lockedSlot));
        }
    }

    private static boolean isMhsFuryActive(Player player) {
        if (!armorSpecialsEnabled(player)) {
            return false;
        }
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        Profile headProfile = DfsEquipmentItem.profile(head);
        return equipmentAbility(player, headProfile) == SpecialAbility.MHS_FURY
                && player.level().getGameTime() <= ItemStackTagReads.getLong(head, MHS_FURY_UNTIL);
    }

    private static void rememberMhsTaczAmmo(ServerPlayer player, ItemStack gun, long expiresAt) {
        CompoundTag tag = gun.getTag();
        String gunId = mhsGunId(gun);
        int ammoCount = tag == null ? 0 : tag.getInt(TACZ_GUN_CURRENT_AMMO_COUNT_TAG);
        boolean bulletInBarrel = tag != null && tag.getBoolean(TACZ_GUN_HAS_BULLET_IN_BARREL_TAG);
        TaczAmmoSnapshot existing = MHS_TACTICAL_AMMO_SNAPSHOTS.get(player.getUUID());
        if (existing != null && existing.gunId().equals(gunId)) {
            if (existing.ammoCount() > ammoCount) {
                ammoCount = existing.ammoCount();
                bulletInBarrel = existing.bulletInBarrel();
            } else if (existing.ammoCount() == ammoCount) {
                bulletInBarrel = bulletInBarrel || existing.bulletInBarrel();
            }
        }
        MHS_TACTICAL_AMMO_SNAPSHOTS.put(player.getUUID(),
                new TaczAmmoSnapshot(gunId, ammoCount, bulletInBarrel, expiresAt));
    }

    private static void restoreMhsTaczAmmo(ServerPlayer player, ItemStack gun, long now) {
        TaczAmmoSnapshot snapshot = MHS_TACTICAL_AMMO_SNAPSHOTS.get(player.getUUID());
        if (snapshot == null) {
            return;
        }
        if (now > snapshot.expiresAt() || !snapshot.gunId().equals(mhsGunId(gun))) {
            MHS_TACTICAL_AMMO_SNAPSHOTS.remove(player.getUUID());
            return;
        }
        CompoundTag tag = gun.getTag();
        if ((tag == null ? 0 : tag.getInt(TACZ_GUN_CURRENT_AMMO_COUNT_TAG)) < snapshot.ammoCount()) {
            tag = gun.getOrCreateTag();
            tag.putInt(TACZ_GUN_CURRENT_AMMO_COUNT_TAG, snapshot.ammoCount());
        }
        if (snapshot.bulletInBarrel() && (tag == null || !tag.getBoolean(TACZ_GUN_HAS_BULLET_IN_BARREL_TAG))) {
            gun.getOrCreateTag().putBoolean(TACZ_GUN_HAS_BULLET_IN_BARREL_TAG, true);
        }
    }

    private static String mhsGunId(ItemStack gun) {
        CompoundTag tag = gun.getTag();
        if (tag != null && tag.contains(TACZ_GUN_ID_TAG)) {
            String gunId = tag.getString(TACZ_GUN_ID_TAG);
            if (!gunId.isEmpty()) {
                return gunId;
            }
        }
        return gun.getItem().getClass().getName();
    }

    private static boolean isTaczGunStack(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(TACZ_GUN_ID_TAG)) {
            return true;
        }
        return stack.getItem().getClass().getName().startsWith("com.tacz.guns.");
    }

    private static void updateEquipmentMovementBonus(ServerPlayer player, ItemStack chest, Profile chestProfile,
                                                     ItemStack head, Profile headProfile) {
        var attr = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attr == null) {
            return;
        }
        AttributeModifier existing = attr.getModifier(EQUIPMENT_ASSAULT_MOVEMENT_UUID);
        if (existing != null) {
            attr.removeModifier(EQUIPMENT_ASSAULT_MOVEMENT_UUID);
        }
        AttributeModifier existingRoot = attr.getModifier(EQUIPMENT_MHS_ROOT_UUID);
        if (existingRoot != null) {
            attr.removeModifier(EQUIPMENT_MHS_ROOT_UUID);
        }

        double bonus = 0.0D;
        long now = player.level().getGameTime();
        if (chest != null && !chest.isEmpty()
                && equipmentAbility(player, chestProfile) == SpecialAbility.TRICK_ASSAULT
                && now <= ItemStackTagReads.getLong(chest, TRICK_UNTIL)) {
            bonus += ItemStackTagReads.getInt(chest, TRICK_STACKS) * 0.05D;
        }
        if (head != null && !head.isEmpty()
                && equipmentAbility(player, headProfile) == SpecialAbility.DICH9_ASSAULT
                && now <= ItemStackTagReads.getLong(head, DICH9_UNTIL)) {
            bonus += ItemStackTagReads.getInt(head, DICH9_STACKS) * 0.04D;
        }
        if (equipmentAbility(player, headProfile) == SpecialAbility.GN_HEAVY) {
            bonus += 0.05D;
        }
        if (equipmentAbility(player, headProfile) == SpecialAbility.GN_HEAVY_NIGHT_VISION) {
            bonus += 0.10D;
        }
        if (bonus > 0.0001D) {
            attr.addTransientModifier(new AttributeModifier(
                    EQUIPMENT_ASSAULT_MOVEMENT_UUID, "dfs_equipment_assault_movement", bonus, AttributeModifier.Operation.MULTIPLY_TOTAL
            ));
        }
        if (isMhsFuryActive(player)) {
            attr.addTransientModifier(new AttributeModifier(
                    EQUIPMENT_MHS_ROOT_UUID, "dfs_mhs_fury_root", -1.0D, AttributeModifier.Operation.MULTIPLY_TOTAL
            ));
        }
    }

    private static void addTimedEquipmentStacks(ItemStack stack, String stacksKey, String untilKey,
                                                long now, int maxStacks, long durationTicks) {
        CompoundTag tag = stack.getOrCreateTag();
        int stacks = now <= tag.getLong(untilKey) ? tag.getInt(stacksKey) : 0;
        tag.putInt(stacksKey, Math.min(maxStacks, stacks + 1));
        tag.putLong(untilKey, now + durationTicks);
    }

    private static double equipmentTaczAssaultMultiplier(LivingEntity entity) {
        if (!(entity instanceof Player player) || !armorSpecialsEnabled(player)) {
            return 1.0D;
        }
        double multiplier = 1.0D;
        long now = entity.level().getGameTime();
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        Profile chestProfile = DfsEquipmentItem.profile(chest);
        if (equipmentAbility(player, chestProfile) == SpecialAbility.TRICK_ASSAULT
                && now <= ItemStackTagReads.getLong(chest, TRICK_UNTIL)) {
            multiplier *= 1.0D + ItemStackTagReads.getInt(chest, TRICK_STACKS) * 0.04D;
        }
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        Profile headProfile = DfsEquipmentItem.profile(head);
        if (equipmentAbility(player, headProfile) == SpecialAbility.DICH9_ASSAULT
                && now <= ItemStackTagReads.getLong(head, DICH9_UNTIL)) {
            multiplier *= 1.0D + ItemStackTagReads.getInt(head, DICH9_STACKS) * 0.06D;
        }
        if (isMhsFuryActive(player)) {
            multiplier *= 2.0D;
        }
        return multiplier;
    }

    private static void duplicateDropsForEquipment(LivingDropsEvent event, ServerPlayer killer) {
        double chance = equipmentLootDuplicateChance(killer);
        if (chance <= 0.0D || killer.getRandom().nextDouble() >= chance) {
            return;
        }
        boolean duplicated = false;
        for (ItemEntity original : new ArrayList<>(event.getDrops())) {
            ItemStack copy = original.getItem().copy();
            if (!copy.isEmpty()) {
                LivingEntity entity = event.getEntity();
                ItemEntity bonus = new ItemEntity(entity.level(),entity.getX(),entity.getY(),entity.getZ(),copy);
                event.getDrops().add(bonus);
                if(equipmentFactionCount(killer,Faction.GTI)>=1)
                    com.rzy.dealt_force_skills.department.DepartmentEquipment.bonus(entity,killer,bonus);
                duplicated = true;
            }
        }
        if (duplicated) {
            Profile profile = equippedProfileWithAbility(killer, SpecialAbility.LOOT_DUPLICATE);
            if (profile != null) {
                notifyEquipmentAbilityTrigger(killer, profile, ParticleTypes.HAPPY_VILLAGER);
            } else {
                notifyEquipmentSetTrigger(killer, Faction.GTI, ParticleTypes.HAPPY_VILLAGER);
            }
        }
    }

    private static void duplicateLivingDrops(LivingDropsEvent event, int extraCopies) {
        if (extraCopies <= 0) {
            return;
        }
        LivingEntity entity = event.getEntity();
        List<ItemEntity> originals = new ArrayList<>(event.getDrops());
        for (int copyIndex = 0; copyIndex < extraCopies; copyIndex++) {
            for (ItemEntity original : originals) {
                ItemStack copy = original.getItem().copy();
                if (!copy.isEmpty()) {
                    if(copyIndex==0&&event.getSource().getEntity() instanceof ServerPlayer owner
                            &&UndeadStateManager.isUndead(owner)&&UndeadStateManager.profession(owner)==UndeadProfession.EXPLORER)
                        com.rzy.dealt_force_skills.department.DepartmentTasks.signal(owner,"undead.explorer.bonus_loot",copy.getCount());
                    event.getDrops().add(new ItemEntity(
                            entity.level(), entity.getX(), entity.getY(), entity.getZ(), copy));
                }
            }
        }
    }

    private static boolean isPassiveOrNeutralMob(LivingEntity entity) {
        return entity instanceof Mob && (!(entity instanceof Enemy) || entity instanceof NeutralMob);
    }

    private static void markHackclawBackstab(LivingEntity target, ServerPlayer attacker) {
        CompoundTag tag = target.getPersistentData();
        tag.putUUID(HACKCLAW_BACKSTAB_OWNER, attacker.getUUID());
        tag.putLong(HACKCLAW_BACKSTAB_UNTIL, target.level().getGameTime() + 5L);
    }

    private static boolean consumeHackclawBackstabMarker(LivingEntity target, ServerPlayer killer) {
        CompoundTag tag = target.getPersistentData();
        long until = tag.getLong(HACKCLAW_BACKSTAB_UNTIL);
        boolean matches = until >= target.level().getGameTime()
                && tag.hasUUID(HACKCLAW_BACKSTAB_OWNER)
                && killer.getUUID().equals(tag.getUUID(HACKCLAW_BACKSTAB_OWNER));
        tag.remove(HACKCLAW_BACKSTAB_OWNER);
        tag.remove(HACKCLAW_BACKSTAB_UNTIL);
        return matches;
    }

    private static void tryReflectHackclawExtremeHack(ServerPlayer player, DamageSource source, float amount) {
        if (amount <= 0.0F
                || !CommanderStateManager.hasBranchUpgrade(player, CommanderBranchUpgrade.HACKCLAW_EXTREME_HACK)) {
            return;
        }
        Entity attacker = source.getEntity();
        if (!(attacker instanceof LivingEntity) && source.getDirectEntity() instanceof Projectile projectile) {
            attacker = projectile.getOwner();
        }
        if (!(attacker instanceof LivingEntity livingAttacker)
                || attacker == player
                || !livingAttacker.isAlive()
                || !ReconRevealThrottle.isActive(livingAttacker)) {
            return;
        }
        float reflected = amount >= Float.MAX_VALUE / 2.0F ? Float.MAX_VALUE : amount * 2.0F;
        hackclawReflectingDamage = true;
        try {
            livingAttacker.hurt(player.damageSources().playerAttack(player), reflected);
        } finally {
            hackclawReflectingDamage = false;
        }
    }

    private static double equipmentLootDuplicateChance(ServerPlayer player) {
        if (equipmentFactionCount(player, Faction.GTI) >= 1) {
            return 1.0D;
        }
        if (!armorSpecialsEnabled(player)) {
            return 0.0D;
        }
        double chance = 0.0D;
        Profile chest = DfsEquipmentItem.profile(player.getItemBySlot(EquipmentSlot.CHEST));
        Profile head = DfsEquipmentItem.profile(player.getItemBySlot(EquipmentSlot.HEAD));
        if (equipmentAbility(player, chest) == SpecialAbility.LOOT_DUPLICATE) {
            chance = Math.max(chance, chest.lootDuplicateChance());
        }
        if (equipmentAbility(player, head) == SpecialAbility.LOOT_DUPLICATE) {
            chance = Math.max(chance, head.lootDuplicateChance());
        }
        return chance;
    }

    private static void recordRedOwlRevengeOnDeath(ServerPlayer player, DamageSource source) {
        if (!armorSpecialsEnabled(player)) {
            return;
        }
        Profile chest = DfsEquipmentItem.profile(player.getItemBySlot(EquipmentSlot.CHEST));
        Entity attacker = source.getEntity();
        if (equipmentAbility(player, chest) == SpecialAbility.RED_OWL_REVENGE && attacker != null) {
            player.getPersistentData().putString(RED_OWL_REVENGE_TYPE, attacker.getType().getDescriptionId());
        }
    }

    private static double equipmentActionSpeedMultiplier(LivingEntity entity) {
        if (!(entity instanceof Player player)) {
            return 1.0D;
        }
        boolean hvkTwoPiece = equipmentFactionCount(player, Faction.HVK) >= 2;
        double multiplier = 1.0D;
        Profile chest = DfsEquipmentItem.profile(player.getItemBySlot(EquipmentSlot.CHEST));
        Profile head = DfsEquipmentItem.profile(player.getItemBySlot(EquipmentSlot.HEAD));
        if (chest != null) {
            multiplier *= chest.actionSpeedMultiplier(hvkTwoPiece);
        }
        if (head != null) {
            multiplier *= head.actionSpeedMultiplier(hvkTwoPiece);
        }
        return Math.max(0.1D, multiplier);
    }

    private static double normalizedUseSpeedMultiplier(double multiplier) {
        if (!Double.isFinite(multiplier)) {
            return 1.0D;
        }
        return Mth.clamp(multiplier, 0.05D, 20.0D);
    }

    private static boolean shouldAdjustUseDuration(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id == null) {
            return false;
        }
        return "minecraft".equals(id.getNamespace()) || DealtForceSkillsMod.MODID.equals(id.getNamespace());
    }

    private static int equipmentFactionCount(Player player, Faction faction) {
        if (!armorSpecialsEnabled(player)) {
            return 0;
        }
        int count = 0;
        Profile chest = DfsEquipmentItem.profile(player.getItemBySlot(EquipmentSlot.CHEST));
        Profile head = DfsEquipmentItem.profile(player.getItemBySlot(EquipmentSlot.HEAD));
        if (chest != null && chest.faction() == faction) {
            count++;
        }
        if (head != null && head.faction() == faction) {
            count++;
        }
        return count;
    }

    private static boolean isEquipmentBypassDamage(DamageSource source) {
        return source.is(DamageTypes.FALL)
                || source.is(DamageTypes.FELL_OUT_OF_WORLD)
                || source.is(DamageTypes.MAGIC)
                || source.is(DamageTypes.INDIRECT_MAGIC);
    }

    private static boolean isEquipmentCoveredHit(LivingEntity entity, DamageSource source, Profile profile) {
        return isEquipmentCoveredByRatio(profile, resolveEquipmentHitHeightRatio(entity, source));
    }

    /**
     * Prefer equipment absorb: widen zones slightly and fall back to torso when height is unknown
     * (explosions, skill AOE, self-sourced blasts) so helmet/armor still get a chance to protect.
     */
    private static Double resolveEquipmentHitHeightRatio(LivingEntity entity, DamageSource source) {
        Double ratio = estimateEquipmentHitHeightRatio(entity, source);
        return ratio != null ? ratio : EQUIPMENT_UNKNOWN_HIT_HEIGHT_RATIO;
    }

    private static boolean isEquipmentCoveredByRatio(Profile profile, Double ratio) {
        if (profile == null || ratio == null) {
            return false;
        }
        if (profile.isHelmet()) {
            // Slightly lower threshold so more upper-body hits count as helmet protection.
            return ratio >= 0.74D;
        }
        return switch (profile.coverageKey()) {
            case "upper_torso" -> ratio >= 0.42D && ratio < 0.78D;
            case "full_torso" -> ratio >= 0.36D && ratio < 0.78D;
            case "full_torso_arms" -> ratio >= 0.32D && ratio < 0.82D;
            default -> ratio >= 0.36D && ratio < 0.78D;
        };
    }

    private static boolean isLimbHitRatio(double ratio) {
        // Below lowest torso/arm coverage band = legs / unprotected limbs.
        return ratio < 0.32D;
    }

    private static float applyLimbUnprotectedReduction(ServerPlayer player, float amount, Double hitRatio) {
        if (amount <= 0.0F
                || hitRatio == null
                || !isLimbHitRatio(hitRatio)
                || !ModGameRules.areLimbsDamageReductionsEnabled(player)) {
            return amount;
        }
        // Config is a reduction fraction: 0.35 => keep 65% of limb damage.
        float reduction = Math.max(0.0F, Math.min(1.0F, LIMBS_UNPROTECTED_DAMAGE_REDUCTION));
        return amount * (1.0F - reduction);
    }

    public static Double estimateEquipmentHitHeightRatio(LivingEntity entity, DamageSource source) {
        if (isMeleeDamage(source, source.getEntity())) {
            return 0.55D;
        }

        Vec3 impact = source.getSourcePosition();
        if (impact != null && isNearPlayerHitbox(entity, impact, EQUIPMENT_PROJECTILE_NEAR_INFLATE)) {
            return playerHeightRatio(entity, impact.y);
        }

        Entity direct = source.getDirectEntity();
        if (direct instanceof Projectile && direct != entity) {
            Double ratio = estimateProjectileHitHeightRatio(entity, source, direct);
            if (ratio != null) {
                return ratio;
            }
        }

        // Skill/explosion entities often sit near the player without being Projectiles.
        if (direct != null && direct != entity
                && isNearPlayerHitbox(entity, direct.position(), EQUIPMENT_PROJECTILE_NEAR_INFLATE)) {
            return playerHeightRatio(entity, direct.getBoundingBox().getCenter().y);
        }

        if (isLikelyRangedDamage(source)) {
            Double ratio = estimateRangedAttackerHitHeightRatio(entity, source);
            if (ratio != null) {
                return ratio;
            }
        }

        // Explosion / area damage with a known blast origin near the player.
        if (impact != null && isNearPlayerHitbox(entity, impact, EQUIPMENT_PROJECTILE_NEAR_INFLATE * 2.0D)) {
            return playerHeightRatio(entity, impact.y);
        }
        return null;
    }

    private static Double estimateProjectileHitHeightRatio(LivingEntity entity, DamageSource source, Entity projectile) {
        Vec3 projectileCenter = projectile.getBoundingBox().getCenter();
        if (isNearPlayerHitbox(entity, projectileCenter, EQUIPMENT_PROJECTILE_NEAR_INFLATE)) {
            return playerHeightRatio(entity, projectileCenter.y);
        }

        Vec3 motion = projectile.getDeltaMovement();
        if (motion.lengthSqr() > 0.000001D) {
            Double ratio = rayHitHeightRatio(entity, projectileCenter.subtract(motion), projectileCenter.add(motion), EQUIPMENT_RANGED_TRACE_INFLATE);
            if (ratio != null) {
                return ratio;
            }
        }

        Entity attacker = source.getEntity();
        if (attacker != null && attacker != entity && attacker != projectile) {
            return rayHitHeightRatio(entity, entityAimOrigin(attacker), projectileCenter, EQUIPMENT_RANGED_TRACE_INFLATE);
        }
        return null;
    }

    private static Double estimateRangedAttackerHitHeightRatio(LivingEntity entity, DamageSource source) {
        Entity attacker = source.getEntity();
        if (!(attacker instanceof LivingEntity) || attacker == entity) {
            return null;
        }

        Vec3 from = entityAimOrigin(attacker);
        Vec3 look = attacker.getLookAngle();
        if (look.lengthSqr() > 0.000001D) {
            double distance = Math.max(6.0D, from.distanceTo(entity.getBoundingBox().getCenter()) + 2.0D);
            Double ratio = rayHitHeightRatio(entity, from, from.add(look.normalize().scale(distance)), EQUIPMENT_RANGED_TRACE_INFLATE);
            if (ratio != null) {
                return ratio;
            }
        }

        Vec3 impact = source.getSourcePosition();
        if (impact != null && impact.distanceToSqr(from) > 0.01D) {
            return rayHitHeightRatio(entity, from, impact, EQUIPMENT_RANGED_TRACE_INFLATE);
        }
        return null;
    }

    private static boolean isNearPlayerHitbox(LivingEntity entity, Vec3 pos, double inflate) {
        return entity.getBoundingBox().inflate(inflate).contains(pos);
    }

    private static Double rayHitHeightRatio(LivingEntity entity, Vec3 from, Vec3 target) {
        return rayHitHeightRatio(entity, from, target, 0.08D);
    }

    private static Double rayHitHeightRatio(LivingEntity entity, Vec3 from, Vec3 target, double inflate) {
        Vec3 delta = target.subtract(from);
        double length = delta.length();
        if (length < 0.0001D) {
            return null;
        }
        Vec3 end = from.add(delta.scale((length + 1.0D) / length));
        Optional<Vec3> hit = entity.getBoundingBox().inflate(inflate).clip(from, end);
        return hit.map(vec3 -> playerHeightRatio(entity, vec3.y)).orElse(null);
    }

    private static double playerHeightRatio(LivingEntity entity, double y) {
        return Mth.clamp((y - entity.getY()) / Math.max(0.001D, entity.getBbHeight()), 0.0D, 1.0D);
    }

    private static boolean isLikelyHeadDamage(Player player, DamageSource source) {
        Double ratio = estimateEquipmentHitHeightRatio(player, source);
        return ratio != null && ratio >= 0.72D;
    }

    private static boolean isMeleeDamage(DamageSource source, Entity expectedAttacker) {
        Entity attacker = source.getEntity();
        Entity direct = source.getDirectEntity();
        // Beacon boss is a gunfighter; never treat its hits as melee for equipment/height.
        if (attacker instanceof com.rzy.dealt_force_skills.entity.BeaconBossEntity) {
            return false;
        }
        if (!(attacker instanceof LivingEntity) || direct instanceof Projectile || isLikelyRangedDamage(source)) {
            return false;
        }
        return expectedAttacker == null || attacker == expectedAttacker;
    }

    private static boolean isLikelyRangedDamage(DamageSource source) {
        Entity direct = source.getDirectEntity();
        if (direct instanceof Projectile || source.is(TACZ_BULLETS_TAG)) {
            return true;
        }
        if (source.getEntity() instanceof com.rzy.dealt_force_skills.entity.BeaconBossEntity) {
            return true;
        }
        String msgId = source.getMsgId();
        return msgId != null
                && (msgId.contains("bullet") || msgId.contains("projectile") || msgId.contains("arrow"));
    }

    private static void tryStartKingKongExecution(ServerPlayer player, ItemStack chest, LivingEntity target) {
        CompoundTag chestTag = chest.getOrCreateTag();
        long now = SkillCooldownHelper.now(player);
        if (now < chestTag.getLong(KING_KONG_COOLDOWN_UNTIL)) {
            return;
        }
        chestTag.putLong(KING_KONG_COOLDOWN_UNTIL, now + 60L * 20L);
        CompoundTag playerTag = player.getPersistentData();
        playerTag.putInt(KING_KONG_TARGET_ID, target.getId());
        playerTag.putLong(KING_KONG_STRIKE_TICK, now + 20L);

        Vec3 direction = player.position().subtract(target.position());
        if (direction.lengthSqr() < 0.0001D) {
            direction = target.getLookAngle().reverse();
        }
        Vec3 offset = direction.normalize().scale(1.3D);
        Vec3 destination = target.position().add(offset);
        player.teleportTo(destination.x, target.getY(), destination.z);
        lookAt(player, target);
        lookAt(target, player);
        notifyEquipmentAbilityTrigger(player, DfsEquipmentItem.profile(chest), ParticleTypes.PORTAL);
    }

    private static void tickKingKongExecution(ServerPlayer player) {
        CompoundTag tag = player.getPersistentData();
        long strikeTick = tag.getLong(KING_KONG_STRIKE_TICK);
        if (strikeTick <= 0L || player.level().getGameTime() < strikeTick) {
            return;
        }
        int targetId = tag.getInt(KING_KONG_TARGET_ID);
        tag.remove(KING_KONG_TARGET_ID);
        tag.remove(KING_KONG_STRIKE_TICK);
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Entity entity = serverLevel.getEntity(targetId);
        if (!(entity instanceof LivingEntity target) || !target.isAlive() || target == player
                || target.distanceToSqr(player) > 16.0D) {
            return;
        }
        target.hurt(player.damageSources().playerAttack(player), Math.max(1.0F, target.getHealth() * 20.0F));
    }

    private static void lookAt(LivingEntity entity, LivingEntity target) {
        double dx = target.getX() - entity.getX();
        double dz = target.getZ() - entity.getZ();
        float yaw = (float) (Mth.atan2(dz, dx) * (180F / Math.PI)) - 90.0F;
        entity.setYRot(yaw);
        entity.setYHeadRot(yaw);
        entity.setYBodyRot(yaw);
    }

    private static void lookAtAim(LivingEntity entity, LivingEntity target) {
        Vec3 eye = entity.getEyePosition();
        Vec3 targetEye = target.getEyePosition();
        double dx = targetEye.x - eye.x;
        double dy = targetEye.y - eye.y;
        double dz = targetEye.z - eye.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        if (horizontal < 0.0001D) {
            return;
        }
        float yaw = (float) (Mth.atan2(dz, dx) * (180F / Math.PI)) - 90.0F;
        float pitch = (float) -(Mth.atan2(dy, horizontal) * (180F / Math.PI));
        entity.setYRot(yaw);
        entity.setXRot(Mth.clamp(pitch, -89.0F, 89.0F));
        entity.setYHeadRot(yaw);
        entity.setYBodyRot(yaw);
    }

    private static void spawnEquipmentSupport(ServerPlayer player, LivingEntity target, int count, boolean boosted) {
        spawnEquipmentSupport(player, target, count, boosted, false);
    }

    private static void spawnEquipmentSupport(ServerPlayer player, LivingEntity target, int count, boolean boosted, boolean ironOnly) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        for (int i = 0; i < count; i++) {
            Mob mob = ironOnly || player.getRandom().nextBoolean()
                    ? EntityType.IRON_GOLEM.create(serverLevel)
                    : EntityType.SNOW_GOLEM.create(serverLevel);
            if (mob == null) {
                continue;
            }
            double angle = player.getRandom().nextDouble() * Math.PI * 2.0D;
            double radius = 1.5D + player.getRandom().nextDouble() * 2.0D;
            mob.moveTo(player.getX() + Math.cos(angle) * radius, player.getY(), player.getZ() + Math.sin(angle) * radius,
                    player.getRandom().nextFloat() * 360.0F, 0.0F);
            mob.getPersistentData().putUUID(UndeadSupportManager.SUMMON_OWNER, player.getUUID());
            if (boosted) {
                var maxHealth = mob.getAttribute(Attributes.MAX_HEALTH);
                if (maxHealth != null) {
                    maxHealth.setBaseValue(Math.max(maxHealth.getBaseValue(), player.getAttributeValue(Attributes.MAX_HEALTH) * supportAttributeMultiplier(player)));
                }
                var attackDamage = mob.getAttribute(Attributes.ATTACK_DAMAGE);
                if (attackDamage != null) {
                    attackDamage.setBaseValue(Math.max(attackDamage.getBaseValue(), player.getAttributeValue(Attributes.ATTACK_DAMAGE) * supportAttributeMultiplier(player)));
                }
                var movementSpeed = mob.getAttribute(Attributes.MOVEMENT_SPEED);
                if (movementSpeed != null) {
                    movementSpeed.setBaseValue(Math.max(movementSpeed.getBaseValue(), player.getAttributeValue(Attributes.MOVEMENT_SPEED) * supportAttributeMultiplier(player)));
                }
                var armor = mob.getAttribute(Attributes.ARMOR);
                if (armor != null) {
                    armor.setBaseValue(Math.max(armor.getBaseValue(), player.getAttributeValue(Attributes.ARMOR) * supportAttributeMultiplier(player)));
                }
                var toughness = mob.getAttribute(Attributes.ARMOR_TOUGHNESS);
                if (toughness != null) {
                    toughness.setBaseValue(Math.max(toughness.getBaseValue(), player.getAttributeValue(Attributes.ARMOR_TOUGHNESS) * supportAttributeMultiplier(player)));
                }
                mob.setHealth(mob.getMaxHealth());
            }
            if (target != null && target.isAlive() && target != player) {
                mob.setTarget(target);
            }
            serverLevel.addFreshEntity(mob);
        }
    }

    private static double supportAttributeMultiplier(ServerPlayer player) {
        Profile head = DfsEquipmentItem.profile(player.getItemBySlot(EquipmentSlot.HEAD));
        double multiplier = 5.0D;
        if (equipmentAbility(player, head) == SpecialAbility.H70_FOLLOWER_BOOST) {
            multiplier *= 2.0D;
            notifyEquipmentAbilityTrigger(player, head, ParticleTypes.HAPPY_VILLAGER);
        } else if (equipmentAbility(player, head) == SpecialAbility.H70_NIGHT_FOLLOWER_BOOST) {
            multiplier *= 3.5D;
            notifyEquipmentAbilityTrigger(player, head, ParticleTypes.ENCHANT);
        }
        return multiplier;
    }


    private static Entity damageSourceEntity(DamageSource source) {
        Entity attacker = source.getDirectEntity();
        return attacker != null ? attacker : source.getEntity();
    }

    private static boolean tryConvertCommanderOrdinaryAttack(LivingAttackEvent event) {
        LivingEntity target = event.getEntity();
        Player root = damageSourceRootPlayer(event.getSource());
        if (commanderGeneConvertingDamage || SkillDamageHelper.isApplyingCommanderSkillDamage()
                || !(root instanceof ServerPlayer attacker) || target == attacker
                || !CommanderStateManager.convertsSkillDamage(attacker)
                || TrainingTargetCompat.recordsDamageWithoutDying(target)
                || !(target.level() instanceof ServerLevel level) || event.getAmount() <= 0.0F) {
            return false;
        }

        float amount = event.getAmount()
                * CommanderStateManager.ordinaryDamageMultiplier(attacker)
                * CommanderStateManager.criticalDamageMultiplier(attacker);
        event.setCanceled(true);
        commanderGeneConvertingDamage = true;
        try {
            applyCommanderGeneDamage(target,
                    SkillDamageHelper.source(level, DamageTypes.FELL_OUT_OF_WORLD, attacker, attacker), amount);
            applyCommanderGeneDamage(target,
                    SkillDamageHelper.source(level, DamageTypes.PLAYER_ATTACK, attacker, attacker), amount);
            applyCommanderGeneDamage(target,
                    SkillDamageHelper.source(level, DamageTypes.MAGIC, attacker, attacker), amount);
            applyCommanderGeneDamage(target,
                    SkillDamageHelper.source(level, DamageTypes.WITHER, attacker, attacker), amount);
            applyCommanderGeneDamage(target,
                    SkillDamageHelper.source(level, DamageTypes.ON_FIRE, attacker, attacker), amount);
            applyCommanderGeneDamage(target,
                    SkillDamageHelper.source(level, DamageTypes.LIGHTNING_BOLT, attacker, attacker), amount);
            applyCommanderGeneDamage(target, level.damageSources().explosion(attacker, attacker), amount);
        } finally {
            commanderGeneConvertingDamage = false;
        }
        return true;
    }

    private static void applyCommanderGeneDamage(LivingEntity target, DamageSource source, float amount) {
        if (!target.isAlive() || amount <= 0.0F) {
            return;
        }
        target.invulnerableTime = 0;
        target.hurt(source, amount);
    }

    private static Player damageSourceRootPlayer(DamageSource source) {
        if (source.getEntity() instanceof Player player) {
            return player;
        }
        if (source.getDirectEntity() instanceof Player player) {
            return player;
        }
        if (source.getDirectEntity() instanceof Projectile projectile && projectile.getOwner() instanceof Player player) {
            return player;
        }
        return null;
    }

    private static boolean isPlayerSourcedDamage(DamageSource source) {
        if (source.getEntity() instanceof Player || source.getDirectEntity() instanceof Player) {
            return true;
        }
        if (source.getDirectEntity() instanceof Projectile projectile && projectile.getOwner() instanceof Player) {
            return true;
        }
        // Treat Beacon boss gunfire like player-sourced for Sineva bomb-suit shield stamina/slow bookkeeping.
        if (source.getEntity() instanceof com.rzy.dealt_force_skills.entity.BeaconBossEntity) {
            return true;
        }
        return source.getDirectEntity() instanceof Projectile projectile
                && projectile.getOwner() instanceof com.rzy.dealt_force_skills.entity.BeaconBossEntity;
    }

    private static boolean effectApplies(LivingEntity entity, MobEffect effect) {
        return entity.hasEffect(effect) && !ModItemEffectHelper.shouldSuppressHarmfulImpact(entity, effect);
    }

    private static MobEffectInstance activeEffectInstance(LivingEntity entity, MobEffect effect) {
        MobEffectInstance instance = entity.getEffect(effect);
        return instance != null && !ModItemEffectHelper.shouldSuppressHarmfulImpact(entity, effect) ? instance : null;
    }

    private static boolean suppressesPotionDamage(Player player, DamageSource source) {
        return player.hasEffect(ModEffects.PAIN_RELIEF.get())
                && (source.is(DamageTypes.MAGIC)
                || source.is(DamageTypes.INDIRECT_MAGIC)
                || source.is(DamageTypes.WITHER)
                || source.is(DamageTypes.STARVE));
    }

    private static boolean shouldSonicShockSkipAction(LivingEntity entity) {
        MobEffectInstance effect = activeEffectInstance(entity, ModEffects.SONIC_SHOCK.get());
        if (effect == null) {
            return false;
        }
        int period = effect.getAmplifier() >= 1 ? 4 : 2;
        return Math.floorMod(entity.tickCount, period) != 0;
    }

    private static boolean shouldMorseShockSkipAction(LivingEntity entity) {
        MobEffectInstance effect = activeEffectInstance(entity, ModEffects.MORSE_STRONG_SHOCK.get());
        if (effect == null) {
            return false;
        }
        int period = effect.getAmplifier() >= 1 ? 4 : 2;
        return Math.floorMod(entity.tickCount, period) != 0;
    }

    private static boolean isExplosionDamage(DamageSource source) {
        return source.is(DamageTypes.EXPLOSION) || source.is(DamageTypes.PLAYER_EXPLOSION);
    }

    private static void tryAwardCoarseSalt(Player player, Level level, BlockPos pos, int fortune) {
        if (player == null || !level.getBiome(pos).is(BiomeTags.IS_OCEAN)) {
            return;
        }
        float chance = Math.min(1.0F, 0.05F + 0.10F * Math.max(0, fortune));
        if (player.getRandom().nextFloat() >= chance) {
            return;
        }
        ItemStack salt = new ItemStack(ModItems.COARSE_SALT.get());
        if (!player.addItem(salt)) {
            player.level().addFreshEntity(new ItemEntity(player.level(), player.getX(), player.getY(), player.getZ(), salt));
        }
    }

    private static void tryAwardPremiumCoffeeBeans(Player player, Level level, BlockState state, int fortune) {
        if (player == null || !state.is(Blocks.COCOA) || state.getValue(CocoaBlock.AGE) < 2) {
            return;
        }
        int bonusLevel = premiumCoffeeBonusLevel(player, fortune);
        double chance = Math.min(1.0D, PREMIUM_COFFEE_DROP_BASE + bonusLevel * PREMIUM_COFFEE_DROP_BONUS_PER_LEVEL);
        if (player.getRandom().nextDouble() >= chance) {
            return;
        }
        giveOrDrop(player, new ItemStack(ModItems.PREMIUM_COFFEE_BEANS.get()));
    }

    private static void tryDuplicateNewRecruitMiningDrops(Player player, Level level, BlockState state, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel) || player == null || player.isCreative()
                || !serverLevel.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS)
                || !hasEquipmentAbility(player, SpecialAbility.NEW_RECRUIT)
                || player.getRandom().nextDouble() >= NEW_RECRUIT_MINING_DUPLICATE_CHANCE) {
            return;
        }
        var drops = Block.getDrops(state, serverLevel, pos, serverLevel.getBlockEntity(pos), player, player.getMainHandItem());
        boolean duplicated = false;
        for (ItemStack stack : drops) {
            if (!stack.isEmpty()) {
                serverLevel.addFreshEntity(new ItemEntity(serverLevel,
                        pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, stack.copy()));
                duplicated = true;
            }
        }
        if (duplicated && player instanceof ServerPlayer serverPlayer) {
            notifyEquipmentAbilityTrigger(serverPlayer,
                    equippedProfileWithAbility(player, SpecialAbility.NEW_RECRUIT), ParticleTypes.HAPPY_VILLAGER);
        }
    }

    private static void tryDuplicateUndeadExplorerMiningDrops(
            Player player,
            Level level,
            BlockState state,
            BlockPos pos
    ) {
        if (!(player instanceof ServerPlayer serverPlayer)
                || !(level instanceof ServerLevel serverLevel)
                || player.isCreative()
                || !UndeadStateManager.isUndead(player)
                || UndeadStateManager.profession(player) != UndeadProfession.EXPLORER
                || !serverLevel.getGameRules().getBoolean(ModGameRules.DEALT_UNDEAD_EXTRA_DROP)
                || !serverLevel.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS)) {
            return;
        }
        for (ItemStack stack : Block.getDrops(
                state,
                serverLevel,
                pos,
                serverLevel.getBlockEntity(pos),
                serverPlayer,
                player.getMainHandItem())) {
            if (!stack.isEmpty()) {
                boolean questSpawned=serverLevel.addFreshEntity(new ItemEntity(
                        serverLevel,
                        pos.getX() + 0.5D,
                        pos.getY() + 0.5D,
                        pos.getZ() + 0.5D,
                        stack.copy()
                ));
                if(questSpawned)com.rzy.dealt_force_skills.department.DepartmentTasks.signal(serverPlayer,"undead.explorer.bonus_loot",stack.getCount());
            }
        }
    }



    private static boolean hasEquipmentAbility(Player player, SpecialAbility ability) {
        if (!armorSpecialsEnabled(player) || ability == null || ability == SpecialAbility.NONE) {
            return false;
        }
        Profile chestProfile = DfsEquipmentItem.profile(player.getItemBySlot(EquipmentSlot.CHEST));
        if (equipmentAbility(player, chestProfile) == ability) {
            return true;
        }
        Profile headProfile = DfsEquipmentItem.profile(player.getItemBySlot(EquipmentSlot.HEAD));
        return equipmentAbility(player, headProfile) == ability;
    }

    private static Profile equippedProfileWithAbility(Player player, SpecialAbility ability) {
        Profile chestProfile = DfsEquipmentItem.profile(player.getItemBySlot(EquipmentSlot.CHEST));
        if (equipmentAbility(player, chestProfile) == ability) {
            return chestProfile;
        }
        Profile headProfile = DfsEquipmentItem.profile(player.getItemBySlot(EquipmentSlot.HEAD));
        return equipmentAbility(player, headProfile) == ability ? headProfile : null;
    }

    private static void handleFrugalDisassemblyCraft(PlayerEvent.ItemCraftedEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) {
            return;
        }
        Container inventory = event.getInventory();
        Profile triggeredProfile = null;
        if (craftingContains(inventory, ModItems.SIMPLE_STAB_VEST.get())) {
            giveOrDrop(player, new ItemStack(Items.STRING, 3));
            giveOrDrop(player, new ItemStack(Items.WHITE_WOOL, 3));
            triggeredProfile = DfsEquipmentItem.profile(new ItemStack(ModItems.SIMPLE_STAB_VEST.get()));
        } else if (craftingContains(inventory, ModItems.UNIVERSAL_TACTICAL_VEST.get())) {
            giveOrDrop(player, new ItemStack(Items.WHITE_WOOL, 2));
            giveOrDrop(player, new ItemStack(Items.STRING, 2));
            triggeredProfile = DfsEquipmentItem.profile(new ItemStack(ModItems.UNIVERSAL_TACTICAL_VEST.get()));
        } else if (craftingContains(inventory, ModItems.H01_TACTICAL_HELMET.get())) {
            giveOrDrop(player, new ItemStack(Items.STRING, 2));
            giveOrDrop(player, new ItemStack(ModItems.RAW_WOOD_PLANK.get(), 2));
            triggeredProfile = DfsEquipmentItem.profile(new ItemStack(ModItems.H01_TACTICAL_HELMET.get()));
        } else if (craftingContains(inventory, ModItems.MC_BALLISTIC_HELMET.get())) {
            giveOrDrop(player, new ItemStack(Items.STRING, 2));
            giveOrDrop(player, new ItemStack(ModItems.RAW_WOOD_PLANK.get(), 3));
            triggeredProfile = DfsEquipmentItem.profile(new ItemStack(ModItems.MC_BALLISTIC_HELMET.get()));
        }
        if (triggeredProfile != null && player instanceof ServerPlayer serverPlayer) {
            notifyEquipmentAbilityTrigger(serverPlayer, triggeredProfile, ParticleTypes.HAPPY_VILLAGER);
        }
    }

    private static boolean craftingContains(Container inventory, Item item) {
        if (inventory == null) {
            return false;
        }
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i).is(item)) {
                return true;
            }
        }
        return false;
    }

    private static MerchantOffer blueprintTradeOffer(Entity trader, RandomSource random) {
        int emeralds = 30 + random.nextInt(35);
        int count = 1 + random.nextInt(2);
        return new MerchantOffer(new ItemStack(Items.EMERALD, emeralds),
                new ItemStack(ModItems.BLUEPRINT.get(), count), 12, 10, 0.05F);
    }

    private static int premiumCoffeeBonusLevel(Player player, int fortune) {
        int handFortune = Math.max(
                EnchantmentHelper.getItemEnchantmentLevel(Enchantments.BLOCK_FORTUNE, player.getMainHandItem()),
                EnchantmentHelper.getItemEnchantmentLevel(Enchantments.BLOCK_FORTUNE, player.getOffhandItem()));
        int luck = Math.max(0, Mth.floor(player.getLuck()));
        return Math.max(Math.max(0, fortune), Math.max(handFortune, luck));
    }

    private static void giveOrDrop(Player player, ItemStack stack) {
        if (!player.addItem(stack)) {
            player.level().addFreshEntity(new ItemEntity(player.level(), player.getX(), player.getY(), player.getZ(), stack));
        }
    }

    /**
     * Shared HVK work-block loot bonuses (new-recruit equipment + undead explorer gamerule).
     * Called by advanced work-block entities after they produce a bonus stack.
     */
    public static void grantHvkBlockLootBonuses(Player player, Level level, BlockPos pos, ItemStack stack) {
        if (!(level instanceof ServerLevel serverLevel)
                || player == null
                || player.isCreative()
                || stack == null
                || stack.isEmpty()
                || !serverLevel.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS)) {
            return;
        }
        if (hasEquipmentAbility(player, SpecialAbility.NEW_RECRUIT)
                && player.getRandom().nextDouble() < NEW_RECRUIT_MINING_DUPLICATE_CHANCE) {
            spawnHvkBlockLootBonus(serverLevel, pos, stack);
            if (player instanceof ServerPlayer serverPlayer) {
                notifyEquipmentAbilityTrigger(serverPlayer,
                        equippedProfileWithAbility(player, SpecialAbility.NEW_RECRUIT), ParticleTypes.HAPPY_VILLAGER);
            }
        }
        if (player instanceof ServerPlayer
                && UndeadStateManager.isUndead(player)
                && UndeadStateManager.profession(player) == UndeadProfession.EXPLORER
                && serverLevel.getGameRules().getBoolean(ModGameRules.DEALT_UNDEAD_EXTRA_DROP)) {
            spawnHvkBlockLootBonus(serverLevel, pos, stack);
        }
    }

    private static void spawnHvkBlockLootBonus(ServerLevel level, BlockPos pos, ItemStack stack) {
        if (!stack.isEmpty()) {
            level.addFreshEntity(new ItemEntity(level,
                    pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, stack.copy()));
        }
    }

    private static Vec3 damageSourcePosition(DamageSource source) {
        // 1. Direct entity (bullet/projectile) position = actual impact point
        Entity direct = source.getDirectEntity();
        if (direct != null) return direct.position();
        // 2. Source position (may be impact point or shooter depending on damage type)
        Vec3 impact = source.getSourcePosition();
        if (impact != null) return impact;
        // 3. Fallback: attacker position
        Entity attacker = source.getEntity();
        if (attacker instanceof LivingEntity living) {
            return living.position().add(0.0D, living.getBbHeight() * 0.5D, 0.0D);
        }
        if (attacker != null) {
            return attacker.position().add(0.0D, attacker.getBbHeight() * 0.5D, 0.0D);
        }
        return null;
    }

    private record TrackedEffect(int duration, int amplifier) {
    }

    private record LockedLook(float yaw, float pitch) {
    }

}
