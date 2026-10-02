package com.rzy.dealt_force_skills.event;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;

import com.rzy.dealt_force_skills.advancement.DfsAchievements;
import com.rzy.dealt_force_skills.block.BladeWireBlockEntity;
import com.rzy.dealt_force_skills.block.QuickCoverBlock;
import com.rzy.dealt_force_skills.block.QuickCoverBlockEntity;
import com.rzy.dealt_force_skills.block.SpecialWorkBlockConfig;
import com.rzy.dealt_force_skills.character.CharacterSelectionManager;
import com.rzy.dealt_force_skills.character.catdad.CatDadStateManager;
import com.rzy.dealt_force_skills.character.commander.CommanderBranchUpgrade;
import com.rzy.dealt_force_skills.character.commander.CommanderStateManager;
import com.rzy.dealt_force_skills.character.commander.CommanderSupportManager;
import com.rzy.dealt_force_skills.character.chamber.ChamberGunKind;
import com.rzy.dealt_force_skills.character.chamber.ChamberStateManager;
import com.rzy.dealt_force_skills.character.chamber.ChamberTaczEnhancement;
import com.rzy.dealt_force_skills.character.chamber.ChamberTool;
import com.rzy.dealt_force_skills.character.corps.CorpsStateManager;
import com.rzy.dealt_force_skills.character.department.DepartmentOfTransportationSkills;
import com.rzy.dealt_force_skills.character.department.DepartmentOfTransportationStateManager;
import com.rzy.dealt_force_skills.character.dwolf.DWolfSkills;
import com.rzy.dealt_force_skills.character.dwolf.DWolfStateManager;
import com.rzy.dealt_force_skills.character.dwolf.DWolfTool;
import com.rzy.dealt_force_skills.character.gambler.GamblerArenaManager;
import com.rzy.dealt_force_skills.character.gambler.GamblerStateManager;
import com.rzy.dealt_force_skills.character.ghroth.GhrothStateManager;
import com.rzy.dealt_force_skills.character.ghroth.GhrothTaczEnhancement;
import com.rzy.dealt_force_skills.character.gizmo.GizmoStateManager;
import com.rzy.dealt_force_skills.character.gizmo.GizmoTool;
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
import com.rzy.dealt_force_skills.character.ntwo.NTwoStateManager;
import com.rzy.dealt_force_skills.character.ntwo.NTwoTool;
import com.rzy.dealt_force_skills.character.raptor.RaptorStateManager;
import com.rzy.dealt_force_skills.character.raptor.RaptorTool;
import com.rzy.dealt_force_skills.character.saeed.SaeedGuardType;
import com.rzy.dealt_force_skills.character.saeed.SaeedStateManager;
import com.rzy.dealt_force_skills.character.shepherd.ShepherdSkills;
import com.rzy.dealt_force_skills.character.shepherd.ShepherdStateManager;
import com.rzy.dealt_force_skills.character.shepherd.ShepherdTool;
import com.rzy.dealt_force_skills.character.sineva.SinevaKnockdownState;
import com.rzy.dealt_force_skills.character.sineva.SinevaShieldGeometry;
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
import com.rzy.dealt_force_skills.character.uluru.UluruTool;
import com.rzy.dealt_force_skills.character.undead.UndeadProfession;
import com.rzy.dealt_force_skills.character.undead.UndeadStateManager;
import com.rzy.dealt_force_skills.character.undead.UndeadSupportManager;
import com.rzy.dealt_force_skills.character.undead.UndeadUpgradeManager;
import com.rzy.dealt_force_skills.character.vlinder.VlinderStateManager;
import com.rzy.dealt_force_skills.character.vlinder.VlinderTool;
import com.rzy.dealt_force_skills.character.vyron.VyronSkills;
import com.rzy.dealt_force_skills.character.vyron.VyronStateManager;
import com.rzy.dealt_force_skills.character.vyron.VyronTool;
import com.rzy.dealt_force_skills.climb.HvkZiplineManager;
import com.rzy.dealt_force_skills.compat.LesRaisinsTacticalCompat;
import com.rzy.dealt_force_skills.compat.PlayerReviveCompat;
import com.rzy.dealt_force_skills.compat.SuperbWarfareCompat;
import com.rzy.dealt_force_skills.compat.TrainingTargetCompat;
import com.rzy.dealt_force_skills.config.DealtForceConfig;
import com.rzy.dealt_force_skills.effect.HackclawFlashBlindEffect;
import com.rzy.dealt_force_skills.effect.InjuryManager;
import com.rzy.dealt_force_skills.effect.ManbaBlindedEffect;
import com.rzy.dealt_force_skills.effect.ModItemEffectHelper;
import com.rzy.dealt_force_skills.effect.MorseFlashedEffect;
import com.rzy.dealt_force_skills.effect.NoxDelayedWoundEffect;
import com.rzy.dealt_force_skills.effect.InjuryManager;
import com.rzy.dealt_force_skills.effect.NoxFlashedEffect;
import com.rzy.dealt_force_skills.effect.ShakehandsCombatHandler;
import com.rzy.dealt_force_skills.team.TeamCombatRules;
import com.rzy.dealt_force_skills.effect.ToxikFireflyInterferenceEffect;
import com.rzy.dealt_force_skills.effect.ToxikTearGasBlindEffect;
import com.rzy.dealt_force_skills.entity.LunaShockArrowEntity;
import com.rzy.dealt_force_skills.entity.MorseSonarDetectorEntity;
import com.rzy.dealt_force_skills.entity.RaptorFalconDroneEntity;
import com.rzy.dealt_force_skills.entity.AsaraTrooperEntity;
import com.rzy.dealt_force_skills.entity.AcceleratedTroopsEntity;
import com.rzy.dealt_force_skills.entity.SaeedGuardEntity;
import com.rzy.dealt_force_skills.entity.UluruLoiteringMissileEntity;
import com.rzy.dealt_force_skills.item.DfsEquipmentItem;
import com.rzy.dealt_force_skills.item.EternalLoveBlessingItem;
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
import com.rzy.dealt_force_skills.skill.SkillAnimationScheduler;
import com.rzy.dealt_force_skills.skill.SkillDamageHelper;
import com.rzy.dealt_force_skills.skill.SkillSoundFeedback;
import com.rzy.dealt_force_skills.team.DealtTeamManager;
import com.rzy.dealt_force_skills.team.RoundStartFreezeManager;
import com.rzy.dealt_force_skills.util.MeleeWeaponCompat;
import com.rzy.dealt_force_skills.util.EmptyHandExtinguishHandler;
import com.rzy.dealt_force_skills.util.ReconRevealThrottle;
import com.rzy.dealt_force_skills.util.TargetingUtil;
import com.rzy.dealt_force_skills.util.RangedSoundHelper;
import com.tacz.guns.api.event.common.AttachmentPropertyEvent;
import com.tacz.guns.api.event.common.GunFireEvent;
import com.tacz.guns.api.event.common.GunFireSelectEvent;
import com.tacz.guns.api.event.common.GunMeleeEvent;
import com.tacz.guns.api.event.common.GunReloadEvent;
import com.tacz.guns.api.event.common.GunShootEvent;
import com.tacz.guns.api.event.common.EntityHurtByGunEvent.Pre;
import com.tacz.guns.api.event.server.AmmoHitBlockEvent;
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
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
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
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades.ItemListing;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.CommandEvent;
import net.neoforged.neoforge.event.PlayLevelSoundEvent.AtEntity;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent.Finish;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent.Start;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent.Tick;
import net.neoforged.neoforge.event.entity.living.LivingEvent.LivingJumpEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent.Added;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent.Applicable;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent.Expired;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent.Remove;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent.Applicable.Result;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.BreakSpeed;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.ItemCraftedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.ItemSmeltedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteract;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteractSpecific;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickEmpty;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickEmpty;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickItem;
import net.neoforged.neoforge.event.level.BlockEvent.BreakEvent;
import net.neoforged.neoforge.event.level.BlockEvent.EntityPlaceEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent.Detonate;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent.Post;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;
import net.neoforged.neoforge.event.village.WandererTradesEvent;

@EventBusSubscriber(
   modid = "dealt_force_skills"
)
public class CommonEvents {
   private static final String TOUHOU_MAID_PICKUP_POST_EVENT =
      "com.github.tartaricacid.touhoulittlemaid.api.event.MaidPickupEvent$ItemResultPost";
   private static final String TOUHOU_MAID_PICKUP_PRE_EVENT =
      "com.github.tartaricacid.touhoulittlemaid.api.event.MaidPickupEvent$ItemResultPre";
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
   private static final int HVK_CONSTRUCTOR_REQUIRED_MOD_ADVANCEMENTS = 12;
   private static final int HVK_DISASSEMBLER_REQUIRED_PURPLE_ADVANCEMENTS = 7;
   private static volatile int EFFECT_REAPPLY_GRACE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("EFFECT_REAPPLY_GRACE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("events.common_events.effect_reapply_grace_ticks", 2));
   private static volatile double SHIELD_DEPLOYED_SLOW_AMOUNT = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("SHIELD_DEPLOYED_SLOW_AMOUNT", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("events.common_events.shield_deployed_slow_amount", -0.1));
   private static volatile double SHIELD_DAMAGE_SLOW_MAX_TOTAL = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("SHIELD_DAMAGE_SLOW_MAX_TOTAL", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("events.common_events.shield_damage_slow_max_total", 0.95));
   private static volatile double EQUIPMENT_PROJECTILE_NEAR_INFLATE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("EQUIPMENT_PROJECTILE_NEAR_INFLATE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("events.common_events.equipment_projectile_near_inflate", 0.75));
   private static volatile double EQUIPMENT_RANGED_TRACE_INFLATE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("EQUIPMENT_RANGED_TRACE_INFLATE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("events.common_events.equipment_ranged_trace_inflate", 0.25));
   private static volatile float LIMBS_UNPROTECTED_DAMAGE_REDUCTION = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("LIMBS_UNPROTECTED_DAMAGE_REDUCTION", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.floatValue("events.common_events.limbs_unprotected_damage_reduction", 0.35F));
   private static volatile double HELMET_PLAYER_SOUND_MAX_RANGE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("HELMET_PLAYER_SOUND_MAX_RANGE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("events.common_events.helmet_player_sound_max_range", 64.0));
   private static volatile double HELMET_PLAYER_SOUND_RANGE_PER_BOOST = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("HELMET_PLAYER_SOUND_RANGE_PER_BOOST", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue(
      "events.common_events.helmet_player_sound_range_per_boost", 100.0
   ));
   private static volatile double HELMET_PLAYER_SOUND_VOLUME_MULTIPLIER = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("HELMET_PLAYER_SOUND_VOLUME_MULTIPLIER", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue(
      "events.common_events.helmet_player_sound_volume_multiplier", 1.5
   ));
   private static final String EQUIPMENT_FATAL_GUARD_UNTIL = "dealt_force_skills.equipment_fatal_guard_until";
   private static final String ASARA_SET_FATAL_GUARD_UNTIL = "dealt_force_skills.asara_set_fatal_guard_until";
   private static final String GLOBAL_FORCES_LOCK_TARGET = "dealt_force_skills.global_forces_lock_target";
   private static final String GLOBAL_FORCES_LOCK_PROGRESS_TARGET = "dealt_force_skills.global_forces_lock_progress_target";
   private static final String GLOBAL_FORCES_LOCK_PROGRESS_TICKS = "dealt_force_skills.global_forces_lock_progress_ticks";
   private static final String RED_OWL_REVENGE_TYPE = "dealt_force_skills.red_owl_revenge_type";
   private static final String EQUIPMENT_RESTORE_UNTIL = "dealt_force_skills.equipment_restore_until";
   private static final String EQUIPMENT_SUMMON_UNTIL = "dealt_force_skills.equipment_summon_until";
   private static final String EQUIPMENT_HEAVY_DODGE_SUMMON_UNTIL = "dealt_force_skills.equipment_heavy_dodge_summon_until";
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
   private static final String MELEE_WIRE_CUT_TICK = "dealt_force_skills.melee_wire_cut_tick";
   private static volatile float H09_STATIONARY_MIN_SELF_DAMAGE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("H09_STATIONARY_MIN_SELF_DAMAGE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.floatValue("events.common_events.h09_stationary_min_self_damage", 0.5F));
   private static volatile float H09_STATIONARY_SELF_DAMAGE_MAX_HEALTH_FRACTION = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("H09_STATIONARY_SELF_DAMAGE_MAX_HEALTH_FRACTION", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.floatValue(
      "events.common_events.h09_stationary_self_damage_max_health_fraction", 0.01F
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
   private static final ResourceLocation EQUIPMENT_ASSAULT_MOVEMENT_UUID = ResourceLocation.fromNamespaceAndPath(
      "dealt_force_skills", "common_events.equipment_assault_movement"
   );
   private static final ResourceLocation EQUIPMENT_MHS_ROOT_UUID = ResourceLocation.fromNamespaceAndPath(
      "dealt_force_skills", "common_events.equipment_mhs_root"
   );
   private static final Map<UUID, Map<Holder<MobEffect>, CommonEvents.TrackedEffect>> ULURU_EFFECT_TRACKING = new HashMap<>();
   private static final Map<UUID, Integer> WEBBED_SELECTED_SLOTS = new HashMap<>();
   private static final Map<UUID, Integer> TEMPEST_ACTION_LOCKED_SELECTED_SLOTS = new HashMap<>();
   private static final Map<UUID, CommonEvents.LockedLook> WEBBED_LOOK_LOCKS = new HashMap<>();
   private static final Map<UUID, CommonEvents.TaczAmmoSnapshot> MHS_TACTICAL_AMMO_SNAPSHOTS = new HashMap<>();
   private static final Map<UUID, CommonEvents.GhrothDamageFloor> GHROTH_DAMAGE_FLOORS = new HashMap<>();
   private static final Map<UUID, LinkedList<CommonEvents.GhrothStarsPendingShot>> GHROTH_STARS_PENDING_SHOTS = new HashMap<>();
   private static final Map<UUID, LinkedList<CommonEvents.GhrothNoonPendingCopy>> GHROTH_NOON_PENDING_COPIES = new HashMap<>();
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
   private static final int[] BLUEPRINT_TRADE_LEVELS = new int[]{3, 4, 5};
   private static boolean taczReloadBridgeChecked;
   private static boolean taczReloadBridgeAvailable;
   private static boolean ghrothNoonCopyingDamage;
   private static boolean hackclawReflectingDamage;
   private static boolean commanderGeneConvertingDamage;
   private static Method taczFromLivingEntity;
   private static Method taczCancelReload;
   private static final String UNDEAD_EXPLORER_LOOT_CLAIMED = "dealt_force_skills.undead_explorer_loot_claimed";

   public static void onOptionalMaidPickup(Event event) {
      if (!TOUHOU_MAID_PICKUP_PRE_EVENT.equals(event.getClass().getName())) {
         return;
      }
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
            owner.sendSystemMessage(Component.translatable("message.dealt_force_skills.maid_lucky_coffee", maid.getDisplayName())
               .withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE));
         }
      } catch (ReflectiveOperationException ignored) {
      }
   }

   @SuppressWarnings("unchecked")
   public static void registerOptionalMaidPickupListener() {
      try {
         Class<?> eventClass = Class.forName(TOUHOU_MAID_PICKUP_PRE_EVENT);
         if (Event.class.isAssignableFrom(eventClass)) {
            registerOptionalMaidPickupListener((Class<? extends Event>)eventClass);
         }
      } catch (ClassNotFoundException ignored) {
      }
   }

   private static <T extends Event> void registerOptionalMaidPickupListener(Class<T> eventClass) {
      NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, false, eventClass, CommonEvents::onOptionalMaidPickup);
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
      int slots = ((Number)slotsMethod.invoke(inventory)).intValue();
      ItemStack remainder = stack;
      for (int slot = 0; slot < slots && !remainder.isEmpty(); slot++) {
         Object next = insertMethod.invoke(inventory, slot, remainder, false);
         if (next instanceof ItemStack nextStack) remainder = nextStack;
      }
      return remainder;
   }

   @SubscribeEvent
   public static void onPlayerLoggedInTeamSync(PlayerLoggedInEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         DealtTeamManager.handlePlayerLoggedIn(player);
      }
   }

   @SubscribeEvent
   public static void onPlayerLoggedOutControlCleanup(PlayerLoggedOutEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         DealtTeamManager.handlePlayerLoggedOut(player);
         UluruLoiteringMissileEntity.clearPlayerControlState(player);
         RaptorFalconDroneEntity.clearPlayerControlState(player);
         GamblerArenaManager.clearForPlayer(player);
         clearPlayerRuntimeCaches(player);
      }
   }

   private static void clearPlayerRuntimeCaches(ServerPlayer player) {
      UUID playerId = player.getUUID();
      SinevaSkills.clearForPlayer(player);
      DepartmentOfTransportationSkills.clearForPlayer(player);
      ShepherdSkills.clearForPlayer(player);
      SinevaKnockdownState.clear(player);
      MorseSonarDetectorEntity.discardFor(player);
      LunaShockArrowEntity.clearPullTicks(player);
      SkillSoundFeedback.clear(player);
      PlayerReviveCompat.clear(player);
      NoxStateManager.clearRuntimeOnLogout(player);
      ManbaStateManager.clearRuntimeOnLogout(player);
      DepartmentOfTransportationStateManager.clearRuntimeOnLogout(player);
      MorseStateManager.clearRuntimeOnLogout(player);
      RaptorStateManager.clearRuntimeOnLogout(player);
      LexNinjiaStateManager.clearRuntimeOnLogout(player);
      ULURU_EFFECT_TRACKING.remove(playerId);
      WEBBED_SELECTED_SLOTS.remove(playerId);
      TEMPEST_ACTION_LOCKED_SELECTED_SLOTS.remove(playerId);
      WEBBED_LOOK_LOCKS.remove(playerId);
      MHS_TACTICAL_AMMO_SNAPSHOTS.remove(playerId);
      GHROTH_DAMAGE_FLOORS.entrySet().removeIf(entry -> entry.getKey().equals(playerId) || entry.getValue().attackerId().equals(playerId));
      GHROTH_STARS_PENDING_SHOTS.remove(playerId);
      GHROTH_NOON_PENDING_COPIES.remove(playerId);
   }

   @SubscribeEvent
   public static void onServerStoppingRuntimeCleanup(ServerStoppingEvent event) {
      SinevaSkills.clearRuntimeCaches();
      DepartmentOfTransportationSkills.clearRuntimeCaches();
      ShepherdSkills.clearRuntimeCaches();
      ManbaStateManager.clearTransientRuntimeCaches();
      GhrothStateManager.clearRuntimeCaches();
      NoxDelayedWoundEffect.clearTrackedEntities();
      SkillAnimationScheduler.clearAllRuntimeCaches();
      SkillSoundFeedback.clearAllRuntimeCaches();
      GamblerArenaManager.clearRuntimeCaches();
      RangedSoundHelper.clearRuntimeCaches();
      ULURU_EFFECT_TRACKING.clear();
      WEBBED_SELECTED_SLOTS.clear();
      TEMPEST_ACTION_LOCKED_SELECTED_SLOTS.clear();
      WEBBED_LOOK_LOCKS.clear();
      MHS_TACTICAL_AMMO_SNAPSHOTS.clear();
      GHROTH_DAMAGE_FLOORS.clear();
      GHROTH_STARS_PENDING_SHOTS.clear();
      GHROTH_NOON_PENDING_COPIES.clear();
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingAttack(LivingIncomingDamageEvent event) {
        if (com.rzy.dealt_force_skills.character.rover.RoverStateManager.executingDeath(event.getEntity())) return;
      DamageSource source = event.getSource();
      if (AsaraTrooperEntity.shouldCancelFriendlyDamage(event.getEntity(), source)) {
         event.setCanceled(true);
         event.setAmount(0.0F);
         return;
      }
      // Team protection + shakehands must run before other combat modifiers.
      if (TeamCombatRules.shouldCancelFriendlyDamage(event.getEntity(), source)
         && !ManbaStateManager.arePlayerDuelPartners(
            damageSourceRootPlayer(source),
            event.getEntity() instanceof Player p ? p : null)) {
         event.setCanceled(true);
         event.setAmount(0.0F);
         return;
      }
      if (event.getEntity() instanceof ServerPlayer target
         && CommanderSupportManager.isAdsProtectedFor(target, damageSourceEntity(source), source)) {
         event.setCanceled(true);
         event.setAmount(0.0F);
         return;
      }
      if (event.getEntity() instanceof ServerPlayer target
         && UluruStateManager.tryRedirectDamageToCover(target, event.getAmount())) {
         event.setCanceled(true);
         event.setAmount(0.0F);
         return;
      }
      if (event.getEntity() instanceof ServerPlayer target
         && NikaidouHiroWitchificationStateManager.shouldBlockFinalAttack(target, source)) {
         event.setCanceled(true);
         event.setAmount(0.0F);
         return;
      }
      ShakehandsCombatHandler.handleLivingHurt(event);
      if (event.isCanceled() || event.getAmount() <= 0.0F) {
         return;
      }
      if (event.getEntity() instanceof ServerPlayer target
         && LexNinjiaStateManager.handleHandGuardAttack(target, source)) {
         event.setCanceled(true);
         event.setAmount(0.0F);
         return;
      }
      event.setAmount(NoxStateManager.handleOutgoingDamage(source, event.getAmount()));
      if (event.getAmount() <= 0.0F) return;
      AsaraTrooperEntity asaraSource = AsaraTrooperEntity.sourceTrooper(source);
      if (asaraSource != null) {
         event.setAmount(event.getAmount() * asaraSource.outgoingDamageMultiplier());
      }
      if (!CorpsStateManager.isExecutingLoyaltyTarget(event.getEntity())) {
         Player teamAttacker = damageSourceRootPlayer(source);
         if (teamAttacker != null
            && event.getEntity() instanceof Player target
            && DealtTeamManager.areTeammates(teamAttacker, target)
            && !ManbaStateManager.arePlayerDuelPartners(teamAttacker, target)) {
            event.setCanceled(true);
         } else if (UndeadSupportManager.shouldCancelFriendlyFire(event.getEntity(), source)) {
            event.setCanceled(true);
         } else {
            if (event.getEntity() instanceof Player targetPlayer
               && source.getEntity() instanceof Player attackerPlayer
               && GhrothStateManager.shouldCancelCeasefire(targetPlayer, attackerPlayer)
               && !ManbaStateManager.arePlayerDuelPartners(attackerPlayer, targetPlayer)) {
               event.setCanceled(true);
               return;
            }

            if (ManbaStateManager.shouldBlockPlayerDuelDamage(event.getEntity(), source)) {
               event.setCanceled(true);
            } else {
               if (event.getEntity() instanceof ServerPlayer target
                  && (GamblerStateManager.isFullyInvulnerable(target) || GamblerStateManager.isCoveredByOrangeShield(target))) {
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

               if (event.getEntity() instanceof ServerPlayer target && DepartmentOfTransportationStateManager.isFullyConcealed(target)) {
                  event.setCanceled(true);
                  return;
               }

               if (event.getEntity() instanceof ServerPlayer target && UndeadStateManager.blocksIncomingAttack(target)) {
                  event.setCanceled(true);
                  return;
               }

               if (!(event.getEntity() instanceof Player player)) {
                  return;
               }

               if (GhrothStateManager.isTacticalImmune(player) || GhrothStateManager.shouldDodge(player, source)) {
                  event.setCanceled(true);
               } else if (StingerStateManager.isDowned(player)
                  && !StingerStateManager.isExecutingDownedDeath(player)
                  && !ManbaStateManager.isExecutingDuelTarget(player)) {
                  event.setCanceled(true);
               } else if (VlinderStateManager.isDowned(player)
                  && !VlinderStateManager.isExecutingDownedDeath(player)
                  && !ManbaStateManager.isExecutingDuelTarget(player)) {
                  event.setCanceled(true);
               } else if (CatDadStateManager.isDowned(player) && !ManbaStateManager.isExecutingDuelTarget(player)) {
                  event.setCanceled(true);
               } else {
                  if (player instanceof ServerPlayer serverPlayer && CatDadStateManager.tryBlockIncoming(serverPlayer)) {
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

                  if (SinevaStateManager.isBombSuitActive(player)) {
                     if (source.is(DamageTypeTags.IS_FIRE)
                        || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)
                        || source.is(DamageTypeTags.WITCH_RESISTANT_TO)) {
                        event.setCanceled(true);
                        return;
                     }

                     if (source.getMsgId().equals("indirectMagic") || source.getMsgId().equals("magic") || source.is(DamageTypeTags.BYPASSES_EFFECTS)) {
                        event.setCanceled(true);
                        return;
                     }
                  }

                  Vec3 sourcePos = damageSourcePosition(source);
                  if (sourcePos != null) {
                     if (tryAbsorbFrontShield(player, source, sourcePos, event.getAmount())) {
                        if (event.getAmount() > 0.0F && isPlayerSourcedDamage(source)) {
                           SinevaStateManager.recordPlayerShieldDamage(player);
                        }

                        event.setCanceled(true);
                     } else {
                        Entity attackerx = damageSourceEntity(source);
                        if (attackerx != null) {
                           if (SinevaStateManager.isSineva(player)
                              && (CommanderStateManager.hasBranchUpgrade(player, CommanderBranchUpgrade.SINEVA_FULL_PROTECTION)
                                 || !SinevaStateManager.isShieldDeployed(player))
                              && Util.isFromBehind(player, sourcePos, 70.0F)
                              && Util.isUpperBodySource(player, attackerx)) {
                              event.setCanceled(true);
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   @SubscribeEvent
   public static void onHackclawMovementSound(AtEntity event) {
      if (event.getEntity() instanceof Player player && HackclawStateManager.isHackclaw(player)) {
         if (isMovementOrLandingSound(((SoundEvent)event.getSound().value()).getLocation())) {
            event.setNewVolume(event.getOriginalVolume() * 0.4F);
         }

         return;
      }
   }

   @SubscribeEvent(
      priority = EventPriority.LOWEST
   )
   public static void onHelmetPlayerSoundAmplification(AtEntity event) {
      if (event.getEntity() instanceof ServerPlayer source) {
         double multiplier = HELMET_PLAYER_SOUND_VOLUME_MULTIPLIER;
         if (Double.isFinite(multiplier) && !(multiplier <= 1.0)) {
            double maxRange = Math.max(0.0, HELMET_PLAYER_SOUND_MAX_RANGE);
            if (!(maxRange <= 0.0)) {
               float extraVolume = (float)((double)event.getOriginalVolume() * (multiplier - 1.0));
               if (Float.isFinite(extraVolume) && !(extraVolume <= 0.0F)) {
                  ServerLevel level = source.serverLevel();

                  for (ServerPlayer listener : level.players()) {
                     if (listener != source && !listener.isSpectator()) {
                        DfsEquipmentItem.Profile profile = DfsEquipmentItem.profile(listener.getItemBySlot(EquipmentSlot.HEAD));
                        double range = helmetPlayerSoundRange(profile, maxRange);
                        if (!(range <= 0.0) && !(listener.distanceToSqr(source) > range * range)) {
                           listener.connection
                              .send(
                                 new ClientboundSoundEntityPacket(
                                    event.getSound(), event.getSource(), source, extraVolume, event.getOriginalPitch(), level.getRandom().nextLong()
                                 )
                              );
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private static double helmetPlayerSoundRange(DfsEquipmentItem.Profile profile, double maxRange) {
      if (profile != null && !(profile.hearingBoost() <= 0.0)) {
         double range = profile.hearingBoost() * Math.max(0.0, HELMET_PLAYER_SOUND_RANGE_PER_BOOST);
         return Math.min(maxRange, Math.max(0.0, range));
      } else {
         return 0.0;
      }
   }

   @SubscribeEvent
   public static void onMorsePlayerSound(AtEntity event) {
      if (event.getEntity() instanceof ServerPlayer source) {
         MorseStateManager.onPlayerSound(source, source.position());
      }
   }

   @SubscribeEvent
    public static void onLivingHurt(LivingIncomingDamageEvent event) {
        if (com.rzy.dealt_force_skills.character.rover.RoverStateManager.executingDeath(event.getEntity())) return;
      LivingEntity hurtEntity = event.getEntity();
      DamageSource source = event.getSource();
      if (hackclawReflectingDamage) {
         return;
      }
      if (damageSourceRootPlayer(source) instanceof ServerPlayer attacker
         && attacker == hurtEntity
         && CommanderStateManager.isCommander(attacker)) {
         event.setCanceled(true);
         event.setAmount(0.0F);
         return;
      }
      if (SuperbWarfareCompat.isCommanderVehicle(hurtEntity,
         DealtForceSkillsMod.MODID + ".commander_vehicle")) {
         if (!source.is(DamageTypeTags.IS_EXPLOSION)) {
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
      if (GamblerArenaManager.shouldCancelAttack(hurtEntity, source) || GamblerArenaManager.handleLivingHurt(hurtEntity, source, event.getAmount())) {
         event.setCanceled(true);
         event.setAmount(0.0F);
      } else if (!CorpsStateManager.isExecutingLoyaltyTarget(hurtEntity)) {
         if (CorpsStateManager.tryConvertDuelAttack(source, hurtEntity)) {
            event.setAmount(0.0F);
         } else {
            Player teamAttacker = damageSourceRootPlayer(source);
            if (teamAttacker != null
               && hurtEntity instanceof Player target
               && DealtTeamManager.areTeammates(teamAttacker, target)
               && !ManbaStateManager.arePlayerDuelPartners(teamAttacker, target)) {
               event.setCanceled(true);
               event.setAmount(0.0F);
               return;
            }

            if (SaeedStateManager.shouldCancelTeamDamage(hurtEntity, source)) {
               event.setCanceled(true);
               event.setAmount(0.0F);
            } else {
               if (hurtEntity instanceof Player targetPlayer
                  && source.getEntity() instanceof Player attackerPlayer
                  && GhrothStateManager.shouldCancelCeasefire(targetPlayer, attackerPlayer)
                  && !ManbaStateManager.arePlayerDuelPartners(attackerPlayer, targetPlayer)) {
                  event.setCanceled(true);
                  event.setAmount(0.0F);
                  return;
               }

               if (ManbaStateManager.shouldBlockPlayerDuelDamage(hurtEntity, source)) {
                  event.setCanceled(true);
                  event.setAmount(0.0F);
               } else {
                  if (hurtEntity instanceof ServerPlayer targetPlayer && event.getAmount() > 0.0F) {
                     float redirected = CorpsStateManager.tryRedirectTeammateDamage(targetPlayer, source, event.getAmount());
                     if (redirected <= 0.0F) {
                        event.setAmount(0.0F);
                        return;
                     }

                     event.setAmount(redirected);
                  }

                  if (hurtEntity instanceof ServerPlayer targetPlayer && event.getAmount() > 0.0F) {
                     float adjusted = GamblerStateManager.handleIncomingHurt(targetPlayer, source, event.getAmount());
                     if (adjusted <= 0.0F) {
                        event.setCanceled(true);
                        event.setAmount(0.0F);
                        return;
                     }

                     event.setAmount(adjusted);
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

                  if (event.getAmount() > 0.0F) {
                     float guardMultiplier = SaeedStateManager.guardOutgoingDamageMultiplier(source, hurtEntity);
                     if (guardMultiplier != 1.0F) {
                        event.setAmount(event.getAmount() * guardMultiplier);
                     }
                  }

                  MobEffectInstance corrosion = activeEffectInstance(hurtEntity, ModEffects.CORROSION);
                  if (corrosion != null && event.getAmount() > 0.0F) {
                     int stacks = corrosion.getAmplifier() + 1;
                     event.setAmount(event.getAmount() * (1.0F + (float)stacks * 0.3F));
                  }

                  if (StingerStateManager.hasStimSuppression(hurtEntity)
                     && effectApplies(hurtEntity, ModEffects.STINGER_STIM_SUPPRESSION)
                     && event.getAmount() > 0.0F) {
                     event.setAmount(event.getAmount() * 1.15F);
                  }

                  if (VlinderStateManager.hasMedicalWasteInterference(hurtEntity)
                     && effectApplies(hurtEntity, ModEffects.VLINDER_MEDICAL_WASTE_INTERFERENCE)
                     && event.getAmount() > 0.0F) {
                     event.setAmount(event.getAmount() * 1.3F);
                  }

                  if (ModItemEffectHelper.hasLaughingManiaThree(hurtEntity) && event.getAmount() > 0.0F) {
                     event.setAmount(event.getAmount() * 0.6F);
                  }

                  event.setAmount(LunaStateManager.applyWeakPointDamage(hurtEntity, event.getAmount()));
                  LunaStateManager.handleDamageReveal(event);
                  if (NoxStateManager.handleDecoyHurt(hurtEntity, event.getSource(), event.getAmount())) {
                     event.setAmount(0.0F);
                  } else {
                     LexNinjiaStateManager.handleLivingHurt(event);
                     ShakehandsCombatHandler.handleLivingHurt(event);
                     if (!event.isCanceled()) {
                        if (event.getSource().getEntity() instanceof ServerPlayer attacker && event.getAmount() > 0.0F) {
                           DfsAchievements.onDamage(attacker, hurtEntity, event.getSource(), event.getAmount());
                        }

                        if (event.getSource().getEntity() instanceof ServerPlayer attacker && event.getAmount() > 0.0F) {
                           DepartmentOfTransportationStateManager.revealFromOffense(attacker);
                        }

                        if (event.getSource().getEntity() instanceof ServerPlayer attacker && ManbaStateManager.isManba(attacker) && event.getAmount() > 0.0F) {
                           event.setAmount(event.getAmount() * ManbaStateManager.outgoingDamageMultiplier(attacker, hurtEntity));
                        }

                        if (event.getSource().getEntity() instanceof ServerPlayer attacker
                           && NikaidouHiroWitchificationStateManager.isNikaidouHiroWitchification(attacker)
                           && event.getAmount() > 0.0F) {
                           event.setAmount(
                              NikaidouHiroWitchificationStateManager.handleOutgoingHurt(attacker, hurtEntity, event.getSource(), event.getAmount())
                           );
                        }

                        if (event.getSource().getEntity() instanceof ServerPlayer attacker
                           && event.getAmount() > 0.0F
                           && !event.getSource().is(SkillDamageHelper.TRUE_SKILL_DAMAGE)) {
                           event.setAmount(GamblerStateManager.handleOutgoingHurt(attacker, hurtEntity, event.getSource(), event.getAmount()));
                        }

                        if (event.getSource().getEntity() instanceof ServerPlayer attacker
                           && event.getAmount() > 0.0F
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
                              event.setAmount(event.getAmount()
                                 * CommanderSupportManager.markedDamageMultiplier(hurtEntity));
                           }
                        }

                        if (event.getSource().getEntity() instanceof ServerPlayer attacker && UndeadStateManager.isUndead(attacker) && event.getAmount() > 0.0F
                           )
                         {
                           event.setAmount(UndeadStateManager.handleOutgoingHurt(attacker, hurtEntity, event.getSource(), event.getAmount()));
                        }

                        if (event.getAmount() > 0.0F) {
                           SaeedStateManager.ownerFromDamageEntity(source.getEntity())
                              .filter(attacker -> SaeedStateManager.shouldExecutePassiveTarget(attacker, hurtEntity))
                              .ifPresent(
                                 attacker -> event.setAmount(Math.max(event.getAmount(), hurtEntity.getHealth() + hurtEntity.getAbsorptionAmount() + 1.0F))
                              );
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

                        if (hurtEntity instanceof SaeedGuardEntity guard && event.getAmount() > 0.0F) {
                           float adjusted = handleSaeedGuardInheritedDefense(guard, source, event.getAmount());
                           if (adjusted <= 0.0F) {
                              event.setAmount(0.0F);
                              return;
                           }

                           event.setAmount(adjusted);
                        }

                        if (hurtEntity instanceof Player player) {
                           if (StingerStateManager.isDowned(player)
                              && !StingerStateManager.isExecutingDownedDeath(player)
                              && !ManbaStateManager.isExecutingDuelTarget(player)) {
                              event.setAmount(0.0F);
                           } else if (VlinderStateManager.isDowned(player)
                              && !VlinderStateManager.isExecutingDownedDeath(player)
                              && !ManbaStateManager.isExecutingDuelTarget(player)) {
                              event.setAmount(0.0F);
                           } else if (CatDadStateManager.isDowned(player) && !ManbaStateManager.isExecutingDuelTarget(player)) {
                              event.setAmount(0.0F);
                           } else if (suppressesPotionDamage(player, source)) {
                              event.setAmount(0.0F);
                           } else {
                              if (player instanceof ServerPlayer serverPlayer && CatDadStateManager.tryBlockIncoming(serverPlayer)) {
                                 event.setAmount(0.0F);
                                 return;
                              }

                              if (player instanceof ServerPlayer serverPlayer
                                 && DepartmentOfTransportationStateManager.isDepartment(player)
                                 && event.getAmount() > 0.0F) {
                                 float adjustedAmount = DepartmentOfTransportationStateManager.handleIncomingHurt(serverPlayer, source, event.getAmount());
                                 if (adjustedAmount <= 0.0F) {
                                    event.setAmount(0.0F);
                                    return;
                                 }

                                 event.setAmount(adjustedAmount);
                              }

                              if (player instanceof ServerPlayer serverPlayer && UndeadStateManager.isUndead(player) && event.getAmount() > 0.0F) {
                                 float adjustedAmount = UndeadStateManager.handleIncomingHurt(serverPlayer, source, event.getAmount());
                                 if (adjustedAmount <= 0.0F) {
                                    event.setAmount(0.0F);
                                    return;
                                 }

                                 event.setAmount(adjustedAmount);
                              }

                              if (player instanceof ServerPlayer serverPlayer && NikaidouHiroStateManager.isNikaidouHiro(player) && event.getAmount() > 0.0F) {
                                 float adjustedAmount = source.is(SkillDamageHelper.NIKAIDOU_DECAY)
                                    ? event.getAmount()
                                    : event.getAmount() * NikaidouHiroStateManager.incomingDamageMultiplier(player);
                                 if (adjustedAmount <= 0.0F) {
                                    event.setAmount(0.0F);
                                    return;
                                 }

                                 if (NikaidouHiroStateManager.shouldPreventFatalDamage(serverPlayer, source, adjustedAmount)) {
                                    DfsAchievements.recordFatalAvoidance(serverPlayer);
                                    event.setAmount(0.0F);
                                    return;
                                 }

                                 event.setAmount(adjustedAmount);
                              }

                              if (player instanceof ServerPlayer serverPlayer
                                 && NikaidouHiroWitchificationStateManager.isNikaidouHiroWitchification(player)
                                 && event.getAmount() > 0.0F) {
                                 float incomingAmount = event.getAmount();
                                 if (NikaidouHiroWitchificationStateManager.tryRewindFatalRescue(serverPlayer, source, incomingAmount)) {
                                    event.setAmount(0.0F);
                                    return;
                                 }
                                 if (NikaidouHiroWitchificationStateManager.tryHandleFatalDamage(serverPlayer, source, incomingAmount)) {
                                    DfsAchievements.recordFatalAvoidance(serverPlayer);
                                    event.setAmount(0.0F);
                                    return;
                                 }
                                 float adjustedAmountx = NikaidouHiroWitchificationStateManager.adjustIncomingDamage(serverPlayer, source, incomingAmount);
                                 if (adjustedAmountx <= 0.0F) {
                                    DfsAchievements.recordFatalAvoidance(serverPlayer);
                                    event.setAmount(0.0F);
                                    return;
                                 }

                                 event.setAmount(adjustedAmountx);
                              }

                              if (player instanceof ServerPlayer serverPlayer && ManbaStateManager.isManba(player)) {
                                 if (ManbaStateManager.shouldIgnoreIncomingDamage(player, source)
                                    || ManbaStateManager.trySteelBody(serverPlayer, source, event.getAmount())) {
                                    event.setAmount(0.0F);
                                    return;
                                 }

                                 ManbaStateManager.tryTriggerDiversion(serverPlayer, source);
                                 if (event.getAmount() > 0.0F) {
                                    event.setAmount(event.getAmount() * ManbaStateManager.incomingDamageMultiplier(player, source));
                                 }
                              }

                              if (ShepherdStateManager.isShepherd(player) && isExplosionDamage(source)) {
                                 event.setAmount(event.getAmount() * 0.7F);
                              }

                              if (!SinevaStateManager.isBombSuitActive(player)
                                 || !source.is(DamageTypeTags.IS_FIRE)
                                    && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)
                                    && !source.is(DamageTypeTags.WITCH_RESISTANT_TO)) {
                                 Vec3 sourcePos = damageSourcePosition(source);
                                 if (sourcePos != null && tryAbsorbFrontShield(player, source, sourcePos, event.getAmount())) {
                                    if (event.getAmount() > 0.0F && isPlayerSourcedDamage(source)) {
                                       SinevaStateManager.recordPlayerShieldDamage(player);
                                    }

                                    event.setAmount(0.0F);
                                 } else {
                                    if (SinevaStateManager.isShieldDeployed(player) && event.getAmount() > 0.0F && isPlayerSourcedDamage(source)) {
                                       SinevaStateManager.recordShieldDamage(player);
                                    }

                                    if (SinevaStateManager.isBombSuitActive(player)) {
                                       event.setAmount(event.getAmount() * SinevaStateManager.BOMB_SUIT_DAMAGE_MULTIPLIER);
                                    }

                                    if (player.hasEffect(ModEffects.STINGER_STIM_HEAL) && event.getAmount() > 0.0F) {
                                       event.setAmount(event.getAmount() * 0.85F);
                                    }

                                    if (player instanceof ServerPlayer serverPlayer && event.getAmount() > 0.0F) {
                                       if (CommanderStateManager.isCommander(serverPlayer)) {
                                          if (CommanderStateManager.rollsDodge(serverPlayer)) {
                                             event.setAmount(0.0F);
                                             return;
                                          }
                                          event.setAmount(event.getAmount()
                                             * CommanderStateManager.incomingDamageMultiplier(serverPlayer));
                                          event.setAmount(CommanderStateManager.capIncomingDamage(serverPlayer, event.getAmount()));
                                          if (event.getAmount() <= 0.0F) {
                                             return;
                                          }
                                       }
                                       float equipmentAdjusted = handleEquipmentDefense(serverPlayer, source, event.getAmount());
                                       if (equipmentAdjusted <= 0.0F) {
                                          event.setAmount(0.0F);
                                          return;
                                       }

                                       float allyAdjusted = tryAbsorbGlobalForcesAllyDamage(serverPlayer, source, equipmentAdjusted);
                                       if (allyAdjusted <= 0.0F) {
                                          event.setAmount(0.0F);
                                          return;
                                       }

                                       event.setAmount(allyAdjusted);
                                    }

                                    if (player instanceof ServerPlayer serverPlayer && event.getAmount() > 0.0F) {
                                       event.setAmount(CorpsStateManager.adjustIncomingDuelDamage(serverPlayer, event.getAmount()));
                                    }

                                    if (event.getAmount() > 0.0F && ManbaStateManager.isManba(player)) {
                                       event.setAmount(ManbaStateManager.applyDuelMeleeIncomingDamageFloor(player, source, event.getAmount()));
                                    }

                                    if (player instanceof ServerPlayer serverPlayer && TempestStateManager.isTempest(player) && event.getAmount() > 0.0F) {
                                       TempestStateManager.triggerExplosiveSpine(serverPlayer, (SoundEvent)ModSounds.TEMPEST_EXPLOSIVE_SPINE_ACTIVATE.get());
                                       if (TempestStateManager.shouldPreventFatalDamage(serverPlayer, event.getAmount())) {
                                          event.setAmount(0.0F);
                                          DfsAchievements.recordFatalAvoidance(serverPlayer);
                                          TempestStateManager.triggerEmergencyRecall(serverPlayer);
                                          return;
                                       }
                                    }

                                    if (player instanceof ServerPlayer serverPlayerx
                                       && VlinderStateManager.shouldLetActiveDefenseAbsorbFatal(serverPlayerx, event.getAmount())) {
                                       DfsAchievements.recordVlinderActiveDefenseFatalSave(serverPlayerx, true);
                                       DfsAchievements.recordFatalAvoidance(serverPlayerx);
                                       return;
                                    }

                                    if (player instanceof ServerPlayer serverPlayerx
                                       && StingerStateManager.isHiveSmokeActive(serverPlayerx)
                                       && !source.is(SkillDamageHelper.NIKAIDOU_DECAY)
                                       && event.getAmount() > 0.0F) {
                                       event.setAmount(Math.min(event.getAmount(), Math.max(0.0F, serverPlayerx.getHealth() - 1.0F)));
                                    }

                                    if (player instanceof ServerPlayer serverPlayerx && StingerStateManager.shouldEnterDowned(serverPlayerx, event.getAmount())
                                       )
                                     {
                                       event.setAmount(0.0F);
                                       DfsAchievements.recordFatalAvoidance(serverPlayerx);
                                       StingerStateManager.enterDowned(serverPlayerx);
                                       return;
                                    }

                                    if (player instanceof ServerPlayer serverPlayerx && VlinderStateManager.shouldEnterDowned(serverPlayerx, event.getAmount())
                                       )
                                     {
                                       event.setAmount(0.0F);
                                       DfsAchievements.recordFatalAvoidance(serverPlayerx);
                                       VlinderStateManager.enterDowned(serverPlayerx);
                                       return;
                                    }

                                    if (player instanceof ServerPlayer serverPlayerx
                                       && CatDadStateManager.tryEnterFatalDowned(serverPlayerx, source, event.getAmount())) {
                                       event.setAmount(0.0F);
                                       return;
                                    }

                                    if (player instanceof ServerPlayer serverPlayerx
                                       && ManbaStateManager.tryPreventFatalDamage(serverPlayerx, source, event.getAmount())) {
                                       event.setAmount(0.0F);
                                       return;
                                    }

                                    if (player instanceof ServerPlayer serverPlayerx && event.getAmount() > 0.0F && ManbaStateManager.isManba(player)) {
                                       ManbaStateManager.recordDamageTaken(serverPlayerx);
                                    }

                                    if (player instanceof ServerPlayer serverPlayerx && event.getAmount() > 0.0F) {
                                       CorpsStateManager.onDamageTaken(serverPlayerx, event.getAmount());
                                       DfsAchievements.onDamageTaken(serverPlayerx, source, event.getAmount());
                                    }

                                    if (player instanceof ServerPlayer serverPlayerx && event.getAmount() > 0.0F) {
                                       CatDadStateManager.tryReflectDamage(serverPlayerx, source, event.getAmount());
                                       tryReflectHackclawExtremeHack(serverPlayerx, source, event.getAmount());
                                    }

                                    if (event.getAmount() > 0.0F) {
                                       ManbaStateManager.addAffectionForDamagedTarget(hurtEntity, source, event.getAmount());
                                       NoxStateManager.tryApplyDelayedWound(source, hurtEntity, event.getAmount());
                                    }
                                 }
                              } else {
                                 event.setAmount(0.0F);
                              }
                           }
                        } else {
                           if (event.getAmount() > 0.0F) {
                              ManbaStateManager.addAffectionForDamagedTarget(hurtEntity, source, event.getAmount());
                              NoxStateManager.tryApplyDelayedWound(source, hurtEntity, event.getAmount());
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   @SubscribeEvent(
      priority = EventPriority.LOWEST
   )
   public static void onLivingHurtGamblerPostFinalDamage(LivingIncomingDamageEvent event) {
      LivingEntity target = event.getEntity();
      if (!target.level().isClientSide && !(event.getAmount() <= 0.0F)) {
         if (damageSourceRootPlayer(event.getSource()) instanceof ServerPlayer attacker) {
            GamblerStateManager.handlePostFinalDamage(attacker, target, event.getSource(), event.getAmount());
         }
      }
   }

   @SubscribeEvent(
      priority = EventPriority.LOWEST
   )
   public static void onLivingHurtGhrothDamageFloor(LivingIncomingDamageEvent event) {
      LivingEntity target = event.getEntity();
      CommonEvents.GhrothDamageFloor floor = GHROTH_DAMAGE_FLOORS.get(target.getUUID());
      if (floor != null) {
         long now = target.level().getGameTime();
         if (now > floor.expiresAt()) {
            GHROTH_DAMAGE_FLOORS.remove(target.getUUID());
         } else {
            if (!(damageSourceRootPlayer(event.getSource()) instanceof ServerPlayer attacker) || !floor.attackerId().equals(attacker.getUUID())) {
               return;
            }

            if (!(event.getAmount() <= 0.0F)) {
               GHROTH_DAMAGE_FLOORS.remove(target.getUUID());
               if (event.getAmount() < floor.amount()) {
                  event.setAmount(floor.amount());
               }

               if (floor.starsBonus() && target.level().getServer() != null) {
                  float starsDamage = event.getAmount();
                  consumeGhrothStarsPendingShot(attacker);
                  target.level().getServer().execute(() -> GhrothStateManager.handleStarsTaczHit(attacker, target, starsDamage));
               }
            }
         }
      }
   }

   @SubscribeEvent(
      priority = EventPriority.LOWEST
   )
   public static void onLivingHurtGhrothNoonDamageCopy(LivingIncomingDamageEvent event) {
      LivingEntity target = event.getEntity();
      if (!target.level().isClientSide && !(event.getAmount() <= 0.0F) && !ghrothNoonCopyingDamage) {
         if (!(damageSourceRootPlayer(event.getSource()) instanceof ServerPlayer attacker) || attacker == target || !GhrothStateManager.isGhroth(attacker)) {
            return;
         }

         if (target instanceof Player targetPlayer && GhrothStateManager.shouldCancelCeasefire(attacker, targetPlayer)) {
            return;
         }

         float damage = event.getAmount();
         int copies = GhrothStateManager.consumeNoonDamageCopiesForHit(attacker);
         if (Float.isFinite(damage) && copies > 0) {
            startGhrothNoonDamageCopies(attacker, target, damage, copies);
         }
      }
   }

   @SubscribeEvent(
      priority = EventPriority.LOWEST
   )
   public static void onPlayerTickGhrothNoonDamageCopies(Post event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         tickGhrothNoonDamageCopyQueue(player);
      }
   }

   private static void startGhrothNoonDamageCopies(ServerPlayer attacker, LivingEntity target, float damage, int copies) {
      int remaining = Math.max(0, copies);
      if (GhrothStateManager.tryMarkNoonDamageCopyTick(attacker)) {
         applyGhrothNoonDamageCopy(attacker, target, damage);
         remaining--;
      }

      if (remaining > 0) {
         GHROTH_NOON_PENDING_COPIES.computeIfAbsent(attacker.getUUID(), ignored -> new LinkedList<>())
            .add(new CommonEvents.GhrothNoonPendingCopy(target.getId(), damage, remaining));
      }
   }

   private static void tickGhrothNoonDamageCopyQueue(ServerPlayer attacker) {
      LinkedList<CommonEvents.GhrothNoonPendingCopy> queue = GHROTH_NOON_PENDING_COPIES.get(attacker.getUUID());
      if (queue != null) {
         for (; !queue.isEmpty(); queue.poll()) {
            CommonEvents.GhrothNoonPendingCopy pending = queue.peek();
            Entity entity = attacker.level().getEntity(pending.targetId());
            if (entity instanceof LivingEntity) {
               LivingEntity target = (LivingEntity)entity;
               if (target != attacker && target.isAlive() && !target.isSpectator()) {
                  if (target instanceof Player) {
                     Player targetPlayer = (Player)target;
                     if (GhrothStateManager.shouldCancelCeasefire(attacker, targetPlayer)) {
                        continue;
                     }
                  }

                  if (!GhrothStateManager.tryMarkNoonDamageCopyTick(attacker)) {
                     return;
                  }

                  applyGhrothNoonDamageCopy(attacker, target, pending.damage());
                  queue.poll();
                  if (pending.copiesRemaining() > 1) {
                     queue.addFirst(new CommonEvents.GhrothNoonPendingCopy(pending.targetId(), pending.damage(), pending.copiesRemaining() - 1));
                  }
                  break;
               }
            }
         }

         if (queue.isEmpty()) {
            GHROTH_NOON_PENDING_COPIES.remove(attacker.getUUID());
         }
      }
   }

   private static void applyGhrothNoonDamageCopy(ServerPlayer attacker, LivingEntity target, float damage) {
      target.invulnerableTime = 0;
      ghrothNoonCopyingDamage = true;

      try {
         SkillDamageHelper.hurtUnscaled(target, SkillDamageHelper.trueDamage(attacker.serverLevel(), attacker, attacker), damage);
      } finally {
         ghrothNoonCopyingDamage = false;
      }
   }

   @SubscribeEvent(
      priority = EventPriority.HIGHEST
   )
   public static void onLivingHeal(LivingHealEvent event) {
      if (event.getEntity().hasEffect(ModEffects.NOX_DELAYED_WOUND)) {
         event.setCanceled(true);
      } else if (StingerStateManager.hasStimSuppression(event.getEntity())) {
         event.setCanceled(true);
      } else if (VlinderStateManager.hasMedicalWasteInterference(event.getEntity())) {
         event.setCanceled(true);
      } else if (NikaidouHiroWitchificationStateManager.shouldCancelHealing(event.getEntity())) {
         event.setCanceled(true);
      } else {
         if (event.getEntity() instanceof ServerPlayer player && UndeadStateManager.isUndead(player)) {
            event.setAmount(event.getAmount() * UndeadUpgradeManager.healingMultiplier(player));
         }
      }
   }

   @SubscribeEvent(
      priority = EventPriority.LOWEST
   )
   public static void onLivingUseItemFinish(Finish event) {
      LivingEntity entity = event.getEntity();
      if (!entity.level().isClientSide && entity.hasEffect(ModEffects.NOX_DELAYED_WOUND)) {
         NoxDelayedWoundEffect.enforceCaps(entity);
      }

      if (!entity.level().isClientSide) {
         ManbaStateManager.onUseItemFinished(entity);
         LexNinjiaStateManager.handleFoodFinished(entity, event.getItem());
      }
   }

   @SubscribeEvent(
      priority = EventPriority.HIGHEST
   )
   public static void onLivingUseItemStart(Start event) {
      LivingEntity entity = event.getEntity();
      if (!entity.level().isClientSide) {
         if (entity instanceof ServerPlayer player) {
            MorseStateManager.recordPlayerAction(player);
            LexNinjiaStateManager.onPlayerAction(player);
            if (GamblerArenaManager.blocksInteraction(player)) {
               cancelBlockedUseItem(event, player);
               return;
            }
         }

         if (entity instanceof Player playerx
            && (
               TempestStateManager.isActionLocked(playerx)
                  || CatDadStateManager.isDowned(playerx)
                  || NikaidouHiroStateManager.isWeaponActive(playerx)
                  || NikaidouHiroWitchificationStateManager.isWeaponActive(playerx)
                  || NikaidouHiroWitchificationStateManager.isActionLocked(playerx)
                  || DepartmentOfTransportationStateManager.hasEquippedTool(playerx)
                  || UndeadStateManager.isActionLocked(playerx)
                  || UndeadStateManager.isRitualDancing(playerx)
            )) {
            boolean tempestDisarmed = effectApplies(playerx, ModEffects.TEMPEST_DISARMED);
            if (tempestDisarmed) {
               cancelBlockedUseItem(event, playerx);
            } else {
               stopBlockedUseItem(event, playerx);
            }

            if (tempestDisarmed && playerx instanceof ServerPlayer serverPlayer) {
               cooldownTempestDisarmedHands(serverPlayer);
               cancelOptionalTaczReload(serverPlayer);
            }

            return;
         }

         if (shouldAdjustUseDuration(event.getItem()) || InjuryManager.armActionMultiplier(entity) < 0.9999) {
            double multiplier = ManbaStateManager.behaviorSpeedMultiplier(entity)
               * ToxikStateManager.behaviorSpeedMultiplier(entity)
               * ModItemEffectHelper.medicineUseSpeedMultiplier(entity)
               * equipmentActionSpeedMultiplier(entity)
               * (double)UndeadUpgradeManager.interactionSpeedMultiplier(entity instanceof Player playerx ? playerx : null)
               * (double)UndeadStateManager.frozenInteractionMultiplier(entity);
            multiplier = normalizedUseSpeedMultiplier(multiplier);
            if (multiplier > 0.0 && Math.abs(multiplier - 1.0) > 1.0E-4) {
               event.setDuration(Math.max(1, (int)Math.ceil((double)event.getDuration() / multiplier)));
            }
         }
      }
   }

   private static void stopBlockedUseItem(LivingEntityUseItemEvent event, LivingEntity entity) {
      entity.stopUsingItem();
      event.setDuration(0);
   }

   private static void cancelBlockedUseItem(LivingEntityUseItemEvent event, LivingEntity entity) {
      stopBlockedUseItem(event, entity);
      if (event instanceof Start start) {
         start.setCanceled(true);
      }
   }

   @SubscribeEvent(
      priority = EventPriority.HIGHEST
   )
   public static void onBreakSpeed(BreakSpeed event) {
      if (!TempestStateManager.isActionLocked(event.getEntity())
         && !CatDadStateManager.isDowned(event.getEntity())
         && !NikaidouHiroStateManager.isWeaponActive(event.getEntity())
         && !NikaidouHiroWitchificationStateManager.isWeaponActive(event.getEntity())
         && !NikaidouHiroWitchificationStateManager.isActionLocked(event.getEntity())
         && !DepartmentOfTransportationStateManager.hasEquippedTool(event.getEntity())) {
         double multiplier = ToxikStateManager.behaviorSpeedMultiplier(event.getEntity())
            * ModItemEffectHelper.medicineBreakSpeedMultiplier(event.getEntity())
            * equipmentActionSpeedMultiplier(event.getEntity())
            * (double)UndeadUpgradeManager.interactionSpeedMultiplier(event.getEntity())
            * (double)UndeadStateManager.frozenInteractionMultiplier(event.getEntity());
         if (multiplier > 0.0 && Math.abs(multiplier - 1.0) > 1.0E-4) {
            event.setNewSpeed((float)((double)event.getNewSpeed() * multiplier));
         }
      } else {
         event.setNewSpeed(0.0F);
      }
   }

   @SubscribeEvent(
      priority = EventPriority.HIGHEST
   )
   public static void onItemToss(ItemTossEvent event) {
      if (event.getPlayer() instanceof ServerPlayer serverPlayer
         && LexNinjiaStateManager.shouldPreventHandGuardToss(serverPlayer, event.getEntity().getItem())) {
         event.setCanceled(true);
         return;
      }

      if (event.getPlayer() instanceof ServerPlayer serverPlayer && ChamberStateManager.restoreCanceledGunToss(serverPlayer, event.getEntity().getItem())) {
         event.setCanceled(true);
         return;
      }

      if (event.getPlayer() instanceof ServerPlayer serverPlayer && GamblerArenaManager.blocksInteraction(serverPlayer)) {
         event.setCanceled(true);
         return;
      }

      if (TempestStateManager.isActionLocked(event.getPlayer())
         || CatDadStateManager.isDowned(event.getPlayer())
         || NikaidouHiroStateManager.isWeaponActive(event.getPlayer())
         || NikaidouHiroWitchificationStateManager.isWeaponActive(event.getPlayer())
         || NikaidouHiroWitchificationStateManager.isActionLocked(event.getPlayer())
         || DepartmentOfTransportationStateManager.hasEquippedTool(event.getPlayer())) {
         event.setCanceled(true);
      }
   }

   @SubscribeEvent(
      priority = EventPriority.LOWEST
   )
   public static void onLivingTickNTwoColdDecay(net.neoforged.neoforge.event.tick.EntityTickEvent.Post event) {
      if (event.getEntity() instanceof LivingEntity entity) {
         if (!entity.level().isClientSide) {
            NTwoStateManager.tickColdDecay(entity);
         }
      }
   }

   @SubscribeEvent(
      priority = EventPriority.LOWEST
   )
   public static void onLivingTickDelayedWound(net.neoforged.neoforge.event.tick.EntityTickEvent.Post event) {
      if (event.getEntity() instanceof LivingEntity entity) {
         if (!entity.level().isClientSide && entity.hasEffect(ModEffects.NOX_DELAYED_WOUND)) {
            NoxDelayedWoundEffect.enforceCaps(entity);
         }
      }
   }

   @SubscribeEvent(
      priority = EventPriority.LOWEST
   )
   public static void onLivingTickNoxFlashedAi(net.neoforged.neoforge.event.tick.EntityTickEvent.Post event) {
      if (event.getEntity() instanceof LivingEntity entity) {
         if (!entity.level().isClientSide && entity.hasEffect(ModEffects.NOX_FLASHED)) {
            NoxFlashedEffect.suppressHostileTargeting(entity);
         }
      }
   }

   @SubscribeEvent(
      priority = EventPriority.LOWEST
   )
   public static void onLivingTickMorseFlashedAi(net.neoforged.neoforge.event.tick.EntityTickEvent.Post event) {
      if (event.getEntity() instanceof LivingEntity entity) {
         if (!entity.level().isClientSide && entity.hasEffect(ModEffects.MORSE_FLASH_BLIND)) {
            MorseFlashedEffect.suppressHostileTargeting(entity);
         }
      }
   }

   @SubscribeEvent(
      priority = EventPriority.LOWEST
   )
   public static void onLivingTickManbaBlindedAi(net.neoforged.neoforge.event.tick.EntityTickEvent.Post event) {
      if (event.getEntity() instanceof LivingEntity entity) {
         if (!entity.level().isClientSide && entity.hasEffect(ModEffects.MANBA_BLINDED)) {
            ManbaBlindedEffect.suppressHostileTargeting(entity);
         }
      }
   }

   @SubscribeEvent(
      priority = EventPriority.LOWEST
   )
   public static void onLivingTickHackclawFlashBlindAi(net.neoforged.neoforge.event.tick.EntityTickEvent.Post event) {
      if (event.getEntity() instanceof LivingEntity entity) {
         if (!entity.level().isClientSide && entity.hasEffect(ModEffects.HACKCLAW_FLASH_BLIND)) {
            HackclawFlashBlindEffect.applySuppression(entity);
         }
      }
   }

   @SubscribeEvent(
      priority = EventPriority.LOWEST
   )
   public static void onLivingTickToxikTearGasBlindAi(net.neoforged.neoforge.event.tick.EntityTickEvent.Post event) {
      if (event.getEntity() instanceof LivingEntity entity) {
         if (!entity.level().isClientSide && entity.hasEffect(ModEffects.TOXIK_TEAR_GAS_BLIND)) {
            ToxikTearGasBlindEffect.suppressHostileTargeting(entity);
         }
      }
   }

   @SubscribeEvent(
      priority = EventPriority.LOWEST
   )
   public static void onLivingTickManbaDuelTaunt(net.neoforged.neoforge.event.tick.EntityTickEvent.Post event) {
      if (event.getEntity() instanceof LivingEntity entity) {
         if (!entity.level().isClientSide) {
            ManbaStateManager.enforceDuelTaunt(entity);
            DepartmentOfTransportationStateManager.suppressOwnedSummonTarget(entity);
            UndeadSupportManager.tickOwnedSummon(entity);
            UndeadStateManager.tickExternalEffects(entity);
            NikaidouHiroWitchificationStateManager.tickExternalEffects(entity);
         }
      }
   }

   @SubscribeEvent(
      priority = EventPriority.LOWEST
   )
   public static void onModLivingTickPeaceful(net.neoforged.neoforge.event.tick.EntityTickEvent.Post event) {
      if (!(event.getEntity() instanceof LivingEntity entity)
         || entity.level().isClientSide
         || entity.level().getDifficulty() != Difficulty.PEACEFUL) {
         return;
      }
      ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (id != null && DealtForceSkillsMod.MODID.equals(id.getNamespace())
                && !(entity instanceof AcceleratedTroopsEntity)
                && !(entity instanceof com.rzy.dealt_force_skills.entity.RoverWolfEntity)) {
            entity.discard();
        }
   }

   @SubscribeEvent(
      priority = EventPriority.LOWEST
   )
   public static void onPlayerTickDelayedWound(Post event) {
      Player player = event.getEntity();
      if (!player.level().isClientSide && player.hasEffect(ModEffects.NOX_DELAYED_WOUND)) {
         NoxDelayedWoundEffect.enforceCaps(player);
      }
   }

   @SubscribeEvent(
      priority = EventPriority.LOWEST
   )
   public static void onLevelTickDelayedWound(net.neoforged.neoforge.event.tick.LevelTickEvent.Post event) {
      if (event.getLevel() instanceof ServerLevel serverLevel) {
         NoxDelayedWoundEffect.enforceTracked(serverLevel);
      }
   }

   @SubscribeEvent(
      priority = EventPriority.LOWEST
   )
   public static void onCharacterHitFeedback(LivingIncomingDamageEvent event) {
      if (!event.getEntity().level().isClientSide && !(event.getAmount() <= 0.0F)) {
         Entity attacker = event.getSource().getEntity();
         if (attacker instanceof ServerPlayer player && attacker != event.getEntity() && CharacterSelectionManager.hasSelectedCharacter(player)) {
            NetworkHandler.sendToPlayer(new S2C_CharacterHitFeedback(event.getEntity().getId(), event.getAmount()), player);
            return;
         }
      }
   }

   @SubscribeEvent
   public static void onLivingFall(LivingFallEvent event) {
      if (event.getEntity() instanceof Player player && UndeadStateManager.isUndead(player) && UndeadStateManager.profession(player) == UndeadProfession.HUNTER
         )
       {
         event.setDamageMultiplier(0.0F);
         event.setCanceled(true);
         return;
      }

      if (event.getEntity() instanceof Player player && UluruStateManager.isUluru(player)) {
         event.setDamageMultiplier(event.getDamageMultiplier() * 0.64F);
      }

      if (event.getEntity() instanceof Player player && VyronStateManager.isVyron(player)) {
         boolean propulsionMod = player instanceof ServerPlayer serverPlayer
            && CommanderStateManager.hasBranchUpgrade(serverPlayer, CommanderBranchUpgrade.VYRON_PROPULSION_MOD);
         if (propulsionMod) {
            event.setDamageMultiplier(0.0F);
            event.setCanceled(true);
         } else {
            event.setDamageMultiplier(event.getDamageMultiplier() * 0.64F);
         }
         if ((propulsionMod || event.getDistance() > 3.5F) && player instanceof ServerPlayer serverPlayer) {
            VyronStateManager.grantPoweredOnLanding(serverPlayer);
         }
      }

      if (event.getEntity() instanceof ServerPlayer playerx && TempestStateManager.consumeFallProtection(playerx)) {
         event.setDamageMultiplier(0.0F);
         event.setCanceled(true);
      }
   }

   @SubscribeEvent
   public static void onMobEffectApplicable(Applicable event) {
      MobEffectInstance effect = event.getEffectInstance();
      if (com.rzy.dealt_force_skills.character.rover.RoverStateManager.isProtectedEffect(effect.getEffect())) return;
      if (event.getEntity() instanceof ServerPlayer player
         && com.rzy.dealt_force_skills.item.PortableAdsDeviceItem.isActive(player)) {
         event.setResult(Result.DO_NOT_APPLY);
         return;
      }
      if (event.getEntity() instanceof ServerPlayer player
         && CommanderSupportManager.isAdsEffectProtected(player)) {
         event.setResult(Result.DO_NOT_APPLY);
         return;
      }
      if (event.getEntity() instanceof Player player && CorpsStateManager.shouldDenyIncomingEffect(player, effect.getEffect())) {
         event.setResult(Result.DO_NOT_APPLY);
      }

      if (event.getEntity() instanceof Player player && NikaidouHiroWitchificationStateManager.shouldDenyIncomingEffect(player, effect.getEffect())) {
         event.setResult(Result.DO_NOT_APPLY);
      }

      if (event.getEntity() instanceof Player player && GamblerStateManager.shouldDenyHarmfulEffect(player, (MobEffect)effect.getEffect().value())) {
         event.setResult(Result.DO_NOT_APPLY);
      }

      if (NikaidouHiroWitchificationStateManager.shouldDenyHealingEffect(event.getEntity(), effect.getEffect())) {
         event.setResult(Result.DO_NOT_APPLY);
      }

      if (event.getEntity() instanceof Player player
         && (SinevaStateManager.isBombSuitActive(player) || DWolfStateManager.isOverloadActive(player))
         && !InjuryManager.isInjury(effect.getEffect())
         && ((MobEffect)effect.getEffect().value()).getCategory() == MobEffectCategory.HARMFUL) {
         event.setResult(Result.DO_NOT_APPLY);
      }

      if (event.getEntity() instanceof Player player
         && player.hasEffect(ModEffects.STINGER_STIM_HEAL)
         && ((MobEffect)effect.getEffect().value()).getCategory() == MobEffectCategory.HARMFUL) {
         event.setResult(Result.DO_NOT_APPLY);
      }

      if (event.getEntity() instanceof Player player
         && player.hasEffect(ModEffects.TOXIK_ADRENALINE)
         && ((MobEffect)effect.getEffect().value()).getCategory() == MobEffectCategory.HARMFUL) {
         event.setResult(Result.DO_NOT_APPLY);
      }

      if (event.getEntity() instanceof Player player
         && player.hasEffect(ModEffects.SEDATION)
         && ((MobEffect)effect.getEffect().value()).getCategory() == MobEffectCategory.HARMFUL) {
         event.setResult(Result.DO_NOT_APPLY);
      }

      if (ModItemEffectHelper.shouldSuppressHarmful(event.getEntity(), effect)
         && !InjuryManager.isInjury(effect.getEffect())
         && event.getEntity() instanceof Player player) {
         ModItemEffectHelper.suppressIncomingHarmful(player, effect);
      }

      if (event.getEntity() instanceof Player player
         && player.hasEffect(ModEffects.VLINDER_HEALING_DUST)
         && ((MobEffect)effect.getEffect().value()).getCategory() == MobEffectCategory.HARMFUL) {
         event.setResult(Result.DO_NOT_APPLY);
      }

      if (event.getEntity() instanceof ServerPlayer player
         && ((MobEffect)effect.getEffect().value()).getCategory() == MobEffectCategory.HARMFUL
         && CatDadStateManager.tryBlockIncoming(player)) {
         event.setResult(Result.DO_NOT_APPLY);
      }
   }

   @SubscribeEvent(
      priority = EventPriority.LOWEST
   )
   public static void onMobEffectAdded(Added event) {
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
      if (event.getEntity() instanceof ServerPlayer player
         && CommanderStateManager.isCommander(player)
         && event.getEffectSource() == player
         && event.getEffectInstance().getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
         player.removeEffect(event.getEffectInstance().getEffect());
         return;
      }
      if (!event.getEntity().level().isClientSide
         && TeamCombatRules.shouldBlockPlayerEffect(event.getEntity(), event.getEffectSource(), event.getEffectInstance())) {
         event.getEntity().removeEffect(event.getEffectInstance().getEffect());
         return;
      }
      if (event.getEntity().level().isClientSide || !ShakehandsCombatHandler.handleEffectAdded(event)) {
         boolean rescaled = !event.getEntity().level().isClientSide
            && ToxikStateManager.tryRescaleAddedEffect(event.getEntity(), event.getEffectInstance(), event.getEffectSource());
         if (!rescaled && !event.getEntity().level().isClientSide) {
            rescaled = UndeadUpgradeManager.tryRescaleAddedEffect(event.getEntity(), event.getEffectInstance(), event.getEffectSource());
         }

         if (!rescaled && !event.getEntity().level().isClientSide) {
            rescaled = NTwoStateManager.tryBoostAddedHarmfulEffect(
               event.getEntity(), event.getEffectInstance(), event.getEffectSource() instanceof LivingEntity living ? living : null
            );
         }

         if (!rescaled && !event.getEntity().level().isClientSide) {
            ManbaStateManager.addAffectionForControlledTarget(event.getEntity(), event.getEffectInstance());
         }

         if (event.getEntity() instanceof ServerPlayer player
            && event.getEffectSource() != player
            && CorpsStateManager.shouldDenyIncomingEffect(player, event.getEffectInstance().getEffect())) {
            player.removeEffect(event.getEffectInstance().getEffect());
         }

         if (event.getEntity() instanceof ServerPlayer player
            && NikaidouHiroWitchificationStateManager.shouldDenyIncomingEffect(player, event.getEffectInstance().getEffect())) {
            player.removeEffect(event.getEffectInstance().getEffect());
         }

         if (NikaidouHiroWitchificationStateManager.shouldDenyHealingEffect(event.getEntity(), event.getEffectInstance().getEffect())) {
            event.getEntity().removeEffect(event.getEffectInstance().getEffect());
         }

         if (!event.getEntity().level().isClientSide
            && (event.getEffectInstance().getEffect() == ModEffects.FATIGUE_REMOVAL || event.getEffectInstance().getEffect() == ModEffects.SEDATION)) {
            if (event.getEffectInstance().getEffect() == ModEffects.SEDATION) {
               ModItemEffectHelper.removeActiveNonInjuryHarmfulEffects(event.getEntity());
            } else {
               ModItemEffectHelper.removeActiveHarmfulEffects(event.getEntity());
            }
         }
      }
   }

   @SubscribeEvent(
      priority = EventPriority.LOWEST
   )
   public static void onMobEffectRemove(Remove event) {
      if (CorpsStateManager.shouldCancelDecayedSoldierRemoval(event.getEntity(), event.getEffect())) {
         event.setCanceled(true);
      } else {
         if (event.getEffect() == ModEffects.NOX_DELAYED_WOUND) {
            NoxDelayedWoundEffect.clearCaps(event.getEntity());
         } else if (event.getEffect() == ModEffects.TOXIK_FIREFLY_INTERFERENCE) {
            ToxikFireflyInterferenceEffect.clearCaps(event.getEntity());
         } else if (event.getEffect() == ModEffects.MANBA_BLINDED) {
            ManbaStateManager.restoreDuelTauntAfterBlind(event.getEntity());
         } else if (event.getEffect() == ModEffects.MORSE_FLASH_BLIND) {
            MorseFlashedEffect.restoreMobAi(event.getEntity());
         } else if (event.getEffect() == ModEffects.PAIN_RELIEF) {
            ModItemEffectHelper.restorePainReliefSuppression(event.getEntity());
         }
      }
   }

   @SubscribeEvent
   public static void onCommand(CommandEvent event) {
      CorpsStateManager.noteEffectClearCommand(event.getParseResults().getReader().getString());
   }

   @SubscribeEvent
   public static void onMobEffectExpired(Expired event) {
      MobEffectInstance effect = event.getEffectInstance();
      if (effect != null && effect.getEffect() == ModEffects.NOX_DELAYED_WOUND) {
         NoxDelayedWoundEffect.clearCaps(event.getEntity());
      } else if (effect != null && effect.getEffect() == ModEffects.TOXIK_FIREFLY_INTERFERENCE) {
         ToxikFireflyInterferenceEffect.clearCaps(event.getEntity());
      } else if (effect != null && effect.getEffect() == ModEffects.MANBA_BLINDED) {
         ManbaStateManager.restoreDuelTauntAfterBlind(event.getEntity());
      } else if (effect != null && effect.getEffect() == ModEffects.MORSE_FLASH_BLIND) {
         MorseFlashedEffect.restoreMobAi(event.getEntity());
      } else if (effect != null && effect.getEffect() == ModEffects.PAIN_RELIEF) {
         ModItemEffectHelper.restorePainReliefSuppression(event.getEntity());
      }
   }

   @SubscribeEvent
   public static void onPlayerTickUluruRestoreTerrain(Post event) {
      if (!event.getEntity().level().isClientSide) {
         if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            UluruLoiteringMissileEntity.tickRestoreTerrain(serverPlayer);
         }
      }
   }

   @SubscribeEvent
   public static void onPlayerTickSuitSlow(Post event) {
      Player player = event.getEntity();
      if (!player.level().isClientSide) {
         SinevaStateManager.updateBombSuitStepHeight(player, SinevaStateManager.isBombSuitActive(player));
         AttributeInstance attr = player.getAttribute(Attributes.MOVEMENT_SPEED);
         if (attr != null) {
            boolean shouldSlow = SinevaStateManager.isShieldDeployed(player);
            AttributeModifier existing = attr.getModifier(SinevaStateManager.BOMB_SUIT_SLOW_UUID);
            double damageSlow = shouldSlow ? SinevaStateManager.shieldDamageSlow(player) : 0.0;
            double targetSlowAmount = -Math.min(SHIELD_DAMAGE_SLOW_MAX_TOTAL, Math.max(0.0, -SHIELD_DEPLOYED_SLOW_AMOUNT) + damageSlow);
            if (damageSlow <= 0.0 && shouldSlow) {
               SinevaStateManager.clearShieldDamageSlow(player);
            }

            if (shouldSlow && (existing == null || Math.abs(existing.amount() - targetSlowAmount) > 1.0E-4)) {
               if (existing != null) {
                  attr.removeModifier(SinevaStateManager.BOMB_SUIT_SLOW_UUID);
               }

               attr.addTransientModifier(new AttributeModifier(SinevaStateManager.BOMB_SUIT_SLOW_UUID, targetSlowAmount, Operation.ADD_MULTIPLIED_TOTAL));
            }

            if (!shouldSlow && existing != null) {
               attr.removeModifier(SinevaStateManager.BOMB_SUIT_SLOW_UUID);
            }

            if (!shouldSlow && SinevaStateManager.isSineva(player)) {
               SinevaStateManager.clearShieldDamageSlow(player);
            }
         }
      }
   }

   @SubscribeEvent
   public static void onSinevaBombSuitJump(LivingJumpEvent event) {
      if (event.getEntity() instanceof Player player && SinevaStateManager.isBombSuitActive(player)) {
         Vec3 movement = player.getDeltaMovement();
         player.setDeltaMovement(movement.x, movement.y * SinevaStateManager.bombSuitJumpVelocityMultiplier(), movement.z);
         return;
      }
   }

   @SubscribeEvent
   public static void onPlayerTickDfsEquipment(Post event) {
      if (!event.getEntity().level().isClientSide) {
         if (event.getEntity() instanceof ServerPlayer player) {
            HaffCoinManager.awardSurvivalMinute(player);
            UndeadSoulManager.awardSurvivalMinute(player);
            LexNinjiaCurrencyManager.awardSurvivalMinute(player);
            EmptyHandExtinguishHandler.tick(player);
            DfsAchievements.tick(player);
            tickDfsEquipment(player);
         }
      }
   }

   @SubscribeEvent
   public static void onPlayerTickEternalLoveBlessing(Post event) {
      if (!event.getEntity().level().isClientSide && event.getEntity() instanceof ServerPlayer player) {
         EternalLoveBlessingItem.tickInventoryEffects(player);
      }
   }

   @SubscribeEvent
   public static void onPlayerTickSinevaEffectImmunity(Post event) {
      Player player = event.getEntity();
      if (!player.level().isClientSide && (SinevaStateManager.isBombSuitActive(player) || DWolfStateManager.isOverloadActive(player))) {
         for (MobEffectInstance effect : new ArrayList<>(player.getActiveEffects())) {
            if (((MobEffect)effect.getEffect().value()).getCategory() == MobEffectCategory.HARMFUL) {
               player.removeEffect(effect.getEffect());
            }
         }
      }
   }

   @SubscribeEvent(
      priority = EventPriority.HIGHEST
   )
   public static void onCatDadFatalBeforePlayerRevive(LivingDeathEvent event) {
      if (event.getEntity() instanceof ServerPlayer protectedPlayer
         && !StingerStateManager.isExecutingDownedDeath(protectedPlayer)
         && !VlinderStateManager.isExecutingDownedDeath(protectedPlayer)
         && !ManbaStateManager.isExecutingDuelTarget(protectedPlayer)
         && !CorpsStateManager.isExecutingLoyaltyTarget(protectedPlayer)
         && CatDadStateManager.tryEnterFatalDowned(protectedPlayer, event.getSource())) {
         event.setCanceled(true);
      }
   }

   @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingHurtHiveSmokeFloor(LivingIncomingDamageEvent event) {
        if (com.rzy.dealt_force_skills.character.rover.RoverStateManager.executingDeath(event.getEntity())) return;
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
    public static void onLivingDeath(LivingDeathEvent event) {
      LivingEntity entity = event.getEntity();
      boolean roverExecution = com.rzy.dealt_force_skills.character.rover.RoverStateManager.executingDeath(entity);
      if (entity instanceof ServerPlayer target
         && !roverExecution
         && CommanderSupportManager.tryPreventFatal(target, event.getSource(), Float.MAX_VALUE)) {
         event.setCanceled(true);
         return;
      }
      if (!roverExecution && GamblerArenaManager.handleLivingDeath(entity)) {
         event.setCanceled(true);
      } else {
         if (entity.level() instanceof ServerLevel serverLevel && DepartmentOfTransportationStateManager.isDepartmentChargedCreeper(entity)) {
            DepartmentOfTransportationStateManager.triggerChargedCreeperDeathExplosion(serverLevel, entity);
         }

         boolean corpsLoyaltyExecution = roverExecution || CorpsStateManager.isExecutingLoyaltyTarget(entity);
         if (entity instanceof ServerPlayer protectedPlayer
            && !corpsLoyaltyExecution
            && NikaidouHiroWitchificationStateManager.tryCancelDeath(protectedPlayer, event.getSource())) {
            event.setCanceled(true);
            return;
         }
         if (entity instanceof ServerPlayer protectedPlayer
            && !roverExecution
            && StingerStateManager.isHiveSmokeActive(protectedPlayer)
            && !event.getSource().is(SkillDamageHelper.NIKAIDOU_DECAY)) {
            event.setCanceled(true);
            protectedPlayer.setHealth(Math.max(1.0F, protectedPlayer.getHealth()));
            return;
         }

         if (entity instanceof ServerPlayer protectedPlayer && !corpsLoyaltyExecution && GamblerStateManager.shouldCancelDeath(protectedPlayer)) {
            event.setCanceled(true);
            return;
         }

         if (entity instanceof ServerPlayer protectedPlayer
            && !StingerStateManager.isExecutingDownedDeath(protectedPlayer)
            && !VlinderStateManager.isExecutingDownedDeath(protectedPlayer)
            && !ManbaStateManager.isExecutingDuelTarget(protectedPlayer)
            && !corpsLoyaltyExecution) {
            if (StingerStateManager.isDowned(protectedPlayer)) {
               event.setCanceled(true);
               protectedPlayer.setHealth(Math.max(1.0F, protectedPlayer.getHealth()));
               return;
            }

            if (VlinderStateManager.isDowned(protectedPlayer)) {
               event.setCanceled(true);
               protectedPlayer.setHealth(Math.max(1.0F, protectedPlayer.getHealth()));
               return;
            }

            if (CatDadStateManager.shouldCancelDeath(protectedPlayer)) {
               event.setCanceled(true);
               protectedPlayer.setHealth(Math.max(1.0F, protectedPlayer.getHealth()));
               return;
            }

            if (StingerStateManager.canEnterDowned(protectedPlayer)) {
               event.setCanceled(true);
               DfsAchievements.recordFatalAvoidance(protectedPlayer);
               StingerStateManager.enterDowned(protectedPlayer);
               return;
            }

            if (VlinderStateManager.canEnterDowned(protectedPlayer)) {
               event.setCanceled(true);
               DfsAchievements.recordFatalAvoidance(protectedPlayer);
               VlinderStateManager.enterDowned(protectedPlayer);
               return;
            }

            if (CatDadStateManager.tryEnterFatalDowned(protectedPlayer, event.getSource())) {
               event.setCanceled(true);
               return;
            }

            recordRedOwlRevengeOnDeath(protectedPlayer, event.getSource());
         }

         CorpsStateManager.onLivingDeath(entity);
         if (entity.level() instanceof ServerLevel serverLevel) {
            DfsAchievements.recordNoxDelayedWoundAssistDeath(serverLevel, entity, event.getSource());
         }

         if (entity instanceof ServerPlayer fireflyTarget
            && entity.level() instanceof ServerLevel serverLevel
            && entity.hasEffect(ModEffects.TOXIK_FIREFLY_INTERFERENCE)) {
            ToxikFireflyInterferenceEffect.owner(serverLevel, fireflyTarget)
               .ifPresent(owner -> DfsAchievements.recordToxikFireflyUnremovedDeath(owner, fireflyTarget));
         }

         if (entity instanceof ServerPlayer manbaPlayer) {
            ManbaStateManager.recordDuelDeathAchievement(manbaPlayer, event.getSource());
         }

         entity.removeEffect(ModEffects.STUN);
         entity.removeEffect(ModEffects.N_TWO_FROZEN);
         NTwoStateManager.syncFrozenVisual(entity, 0);
         entity.removeEffect(ModEffects.WEBBED);
         entity.removeEffect(ModEffects.CORROSION);
         entity.removeEffect(ModEffects.SONIC_SHOCK);
         entity.removeEffect(ModEffects.STINGER_DOWNED);
         entity.removeEffect(ModEffects.STINGER_STIM_HEAL);
         entity.removeEffect(ModEffects.STINGER_STIM_SUPPRESSION);
         entity.removeEffect(ModEffects.STINGER_SMOKE_REGEN);
         entity.removeEffect(ModEffects.NOX_DELAYED_WOUND);
         NoxDelayedWoundEffect.clearCaps(entity);
         entity.removeEffect(ModEffects.NOX_CRIPPLED);
         entity.removeEffect(ModEffects.NOX_FLASHED);
         entity.removeEffect(ModEffects.HACKCLAW_FLASH_BLIND);
         entity.removeEffect(ModEffects.NOX_STEALTH);
         entity.removeEffect(ModEffects.MANBA_BLINDED);
         MorseFlashedEffect.restoreMobAi(entity);
         entity.removeEffect(ModEffects.NIKAIDOU_RIFT_STACKS);
         entity.removeEffect(ModEffects.NIKAIDOU_CORRECTION);
         entity.removeEffect(ModEffects.NIKAIDOU_CORE);
         entity.removeEffect(ModEffects.NIKAIDOU_DOOMED);
         entity.removeEffect(ModEffects.CATDAD_HISS_SLOW);
         entity.removeEffect(ModEffects.CATDAD_ARMOR_REDUCED);
         entity.removeEffect(ModEffects.CATDAD_DOWNED);
         entity.removeEffect(ModEffects.DEPARTMENT_CALIBRATION);
         entity.removeEffect(ModEffects.DEPARTMENT_VULNERABLE);
         entity.removeEffect(ModEffects.DEPARTMENT_CONCEALMENT);
         entity.removeEffect(ModEffects.MORSE_STRONG_SHOCK);
         entity.removeEffect(ModEffects.MORSE_FLASH_BLIND);
         entity.removeEffect(ModEffects.MORSE_SONAR_REVEALED);
         entity.removeEffect(ModEffects.PAIN_RELIEF);
         entity.removeEffect(ModEffects.ITEM_WEAKNESS);
         entity.removeEffect(ModEffects.HELA);
         entity.removeEffect(ModEffects.LAUGHING_MANIA_I);
         entity.removeEffect(ModEffects.LAUGHING_MANIA_II);
         entity.removeEffect(ModEffects.LAUGHING_MANIA_III);
         entity.removeEffect(ModEffects.TOXIK_ADRENALINE);
         entity.removeEffect(ModEffects.TOXIK_TEAR_GAS_BLIND);
         ToxikFireflyInterferenceEffect.clearCaps(entity);
         entity.removeEffect(ModEffects.TOXIK_FIREFLY_INTERFERENCE);
         entity.removeEffect(ModEffects.RAPTOR_ACTION_PAUSE);
         entity.removeEffect(ModEffects.RAPTOR_ELECTROMAGNETIC_INTERFERENCE);
         entity.removeEffect(ModEffects.RAPTOR_HUMMINGBIRD_MARKED);
         entity.removeEffect(ModEffects.TEMPEST_EXPLOSIVE_SPINE);
         entity.removeEffect(ModEffects.TEMPEST_DISARMED);
         entity.removeEffect(ModEffects.TEMPEST_EMERGENCY_DOWNED);
         entity.removeEffect(ModEffects.CORPS_WARM_ENFORCEMENT);
         entity.removeEffect(ModEffects.CORPS_FORCEFUL_BATON);
         entity.removeEffect(ModEffects.CORPS_LOYALTY_BOOST);
         entity.removeEffect(ModEffects.VLINDER_HEALING_DUST);
         entity.removeEffect(ModEffects.VLINDER_MEDICAL_WASTE_INTERFERENCE);
         entity.removeEffect(ModEffects.VLINDER_VITAL_DOWNED);
         entity.removeEffect(ModEffects.VLINDER_RESCUE_PROTECTION);
         entity.removeEffect(ModEffects.VLINDER_PLASMA_INJECTED);
         if (entity instanceof ServerPlayer serverPlayer) {
            InjuryManager.clearAll(serverPlayer);
            for (MobEffectInstance instance : new ArrayList<>(serverPlayer.getActiveEffects())) {
               ResourceLocation id = BuiltInRegistries.MOB_EFFECT.getKey(instance.getEffect().value());
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
            GamblerStateManager.clearRuntimeOnDeath(serverPlayer);
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
            DfsAchievements.onDeath(deadPlayer, event.getSource());
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
         if (attacker instanceof Wolf wolf && wolf.getOwner() instanceof ServerPlayer owner) {
            LexNinjiaStateManager.onHamBeastKill(owner, wolf);
         }

         if (attacker instanceof SaeedGuardEntity guard) {
            guard.owner().ifPresent(owner -> {
               SaeedStateManager.awardKill(owner, entity);
               DfsAchievements.onSummonKill(owner, guard, entity);
            });
         }

         if (attacker instanceof ServerPlayer player && attacker != event.getEntity()) {
            DfsAchievements.onKill(player, entity, event.getSource());
            HaffCoinManager.awardKill(player, entity);
            UndeadSoulManager.awardKill(player, entity);
            LexNinjiaCurrencyManager.awardKill(player, entity);
            SaeedStateManager.awardKill(player, entity);
            if (GhrothStateManager.isGhroth(player)) {
               GhrothStateManager.recordJusticeKill(player, entity);
            }

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

            if (NikaidouHiroWitchificationStateManager.isNikaidouHiroWitchification(player)) {
               NikaidouHiroWitchificationStateManager.onKill(player, entity);
            }

            LexNinjiaStateManager.onKill(player);
            if (DepartmentOfTransportationStateManager.isDepartment(player)) {
               DepartmentOfTransportationStateManager.revealFromOffense(player);
            }

            return;
         }
      }
   }

   @SubscribeEvent
   public static void onLivingDrops(LivingDropsEvent event) {
      LivingEntity entity = event.getEntity();
      if (!entity.level().isClientSide) {
         if (!entity.level().getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)) {
            event.getDrops().clear();
            return;
         }
         if (entity instanceof Animal) {
            int looting = event.getSource().getEntity() instanceof LivingEntity killer
               ? Math.max(0, enchantmentLevel(killer.level(), Enchantments.LOOTING, killer.getMainHandItem()))
               : 0;
            float chance = Math.min(1.0F, 0.1F + 0.1F * (float)looting);
            if (entity.getRandom().nextFloat() < chance) {
               int count = 1 + entity.getRandom().nextInt(3);

               for (int i = 0; i < looting; i++) {
                  count += entity.getRandom().nextInt(2);
               }

               event.getDrops()
                  .add(new ItemEntity(entity.level(), entity.getX(), entity.getY(), entity.getZ(), new ItemStack((ItemLike)ModItems.ANIMAL_GLAND.get(), count)));
            }
         }

         if (event.getSource().getEntity() instanceof ServerPlayer killer) {
            duplicateDropsForEquipment(event, killer);
            int extraCopies = 0;
            if (UndeadStateManager.isUndead(killer) && UndeadStateManager.profession(killer) == UndeadProfession.EXPLORER) {
               extraCopies++;
            }

            if (UndeadStateManager.consumeRogueExecutionMarker(entity, killer)) {
               extraCopies += 4;
            }

            if (consumeHackclawBackstabMarker(entity, killer)) {
               extraCopies += 2;
            }

            extraCopies += GamblerStateManager.extraLootCopies(killer, entity);
            extraCopies += SaeedStateManager.extraLootCopies(killer, entity);
            duplicateLivingDrops(event, extraCopies);
            tryDropHumanoidEquipment(event, entity);
         } else {
            SaeedStateManager.ownerFromDamageEntity(event.getSource().getEntity())
               .ifPresent(owner -> duplicateLivingDrops(event, SaeedStateManager.extraLootCopies(owner, entity)));
         }

         if (entity.getType() != EntityType.VINDICATOR) {
            if (event.getSource().getEntity() instanceof ServerPlayer killer) {
               CorpsStateManager.shareLivingDrops(killer, event.getDrops());
            }
         } else {
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
               event.getDrops()
                  .add(
                     new ItemEntity(
                        entity.level(), entity.getX(), entity.getY(), entity.getZ(), new ItemStack((ItemLike)ModItems.DARK_ZONE_RAINBOW_INJECTION.get(), count)
                     )
                  );
            }

            if (event.getSource().getEntity() instanceof ServerPlayer killer) {
               CorpsStateManager.shareLivingDrops(killer, event.getDrops());
            }
         }
      }
   }

   @SubscribeEvent
   public static void onDfsItemCrafted(ItemCraftedEvent event) {
      if (!lacksHvkConstructorCraftRequirement(event)) {
         grantBonusOutput(event.getEntity(), event.getCrafting(), durableConsumableMultiplier(event.getCrafting().getItem()));
         if (event.getEntity() instanceof ServerPlayer player) {
            DfsAchievements.onCrafted(player, event.getCrafting());
         }
      }
   }

   @SubscribeEvent
   public static void onDfsItemSmelted(ItemSmeltedEvent event) {
      Item item = event.getSmelting().getItem();
      int multiplier = item != ModItems.STERILE_WATER.get() && item != ModItems.POLYETHYLENE_FIBER.get() ? 1 : 4;
      grantBonusOutput(event.getEntity(), event.getSmelting(), multiplier);
      if (event.getEntity() instanceof ServerPlayer player) {
         DfsAchievements.onCrafted(player, event.getSmelting());
      }
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
      } else if (item == ModItems.STANDARD_ARMOR_REPAIR_KIT.get()
         || item == ModItems.STANDARD_HELMET_REPAIR_KIT.get()
         || item == ModItems.CAT_TOURNIQUET.get()
         || item == ModItems.TACTICAL_QUICK_SURGICAL_PACK.get()
         || item == ModItems.BOTTLED_ANTIBIOTICS.get()
         || item == ModItems.FIELD_FIRST_AID_KIT.get()
         || item == ModItems.STRONG_INJECTOR.get()) {
         return 3;
      } else {
         return item != ModItems.PRECISION_ARMOR_REPAIR_KIT.get()
               && item != ModItems.ADVANCED_ARMOR_REPAIR_KIT.get()
               && item != ModItems.PRECISION_HELMET_REPAIR_KIT.get()
               && item != ModItems.ADVANCED_HELMET_REPAIR_KIT.get()
               && item != ModItems.DEK_FIELD_SURGICAL_PACK.get()
               && item != ModItems.DVE_PAINKILLER.get()
               && item != ModItems.OUTDOOR_MEDICAL_KIT.get()
               && item != ModItems.BATTLEFIELD_MEDICAL_KIT.get()
            ? 1
            : 2;
      }
   }

   private static void grantBonusOutput(Player player, ItemStack crafted, int multiplier) {
      if (!player.level().isClientSide && !crafted.isEmpty() && multiplier > 1) {
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
   }

   private static void tryDropHumanoidEquipment(LivingDropsEvent event, LivingEntity entity) {
      EntityType<?> type = entity.getType();
      boolean humanoid = type == EntityType.VILLAGER
         || type == EntityType.VINDICATOR
         || type == EntityType.PILLAGER
         || type == EntityType.WITCH
         || type == EntityType.EVOKER
         || type == EntityType.ILLUSIONER;
      if (humanoid && !(entity.getRandom().nextFloat() >= 0.02F)) {
         ItemStack drop = new ItemStack(
            entity.getRandom().nextBoolean() ? (ItemLike)ModItems.D6_TACTICAL_HELMET.get() : (ItemLike)ModItems.SAMURAI_BALLISTIC_VEST.get()
         );
         event.getDrops().add(new ItemEntity(entity.level(), entity.getX(), entity.getY(), entity.getZ(), drop));
      }
   }

   @SubscribeEvent
   public static void onOceanBlockBreak(BreakEvent event) {
      if (!(event.getLevel() instanceof Level level) || level.isClientSide) {
         return;
      }

      if (GamblerArenaManager.protectsArenaBlocks(level)) {
         event.setCanceled(true);
      } else {
         Player player = event.getPlayer();
         if (player instanceof ServerPlayer serverPlayer && GamblerArenaManager.blocksInteraction(serverPlayer)) {
            event.setCanceled(true);
            return;
         }

         int fortune = Math.max(0, enchantmentLevel(level, Enchantments.FORTUNE, player.getMainHandItem()));
         fortune = Math.max(fortune, GamblerStateManager.effectiveFortuneBonus(player));
         tryAwardCoarseSalt(player, level, event.getPos(), fortune);
         tryAwardPremiumCoffeeBeans(player, level, event.getState(), fortune);
         tryDuplicateNewRecruitMiningDrops(player, level, event.getState(), event.getPos());
         tryDuplicateUndeadExplorerMiningDrops(player, level, event.getState(), event.getPos());
         GamblerStateManager.tryDuplicateMiningDrops(player, level, event.getState(), event.getPos());
         if (player instanceof ServerPlayer serverPlayer && level instanceof ServerLevel serverLevel) {
            SaeedStateManager.onOwnerBreakBlock(serverPlayer, serverLevel, event.getPos(), event.getState());
         }
      }
   }

   @SubscribeEvent
   public static void onLexNinjiaBlockPlaced(EntityPlaceEvent event) {
      if (event.getLevel() instanceof Level level && GamblerArenaManager.protectsArenaBlocks(level)) {
         event.setCanceled(true);
         return;
      }

      if (event.getEntity() instanceof ServerPlayer serverPlayer && GamblerArenaManager.blocksInteraction(serverPlayer)) {
         event.setCanceled(true);
         return;
      }

      if (event.getEntity() instanceof ServerPlayer serverPlayer) {
         DfsAchievements.recordToxikAdrenalineAction(serverPlayer, "place");
      }

      LexNinjiaStateManager.handleBlockPlaced(event);
   }

   @SubscribeEvent
   public static void onItemCrafted(ItemCraftedEvent event) {
      if (!handleSpecialWorkBlockDisabledCraft(event)) {
         if (!handleHvkConstructorCraftGate(event)) {
            handleFrugalDisassemblyCraft(event);
            ItemStack crafted = event.getCrafting();
         }
      }
   }

   private static boolean lacksHvkConstructorCraftRequirement(ItemCraftedEvent event) {
      ItemStack crafted = event.getCrafting();
      if (crafted.isEmpty() || !(event.getEntity() instanceof ServerPlayer player)) {
         return false;
      } else if (SpecialWorkBlockConfig.isDisabledItem(crafted)) {
         return true;
      } else {
         return crafted.is((Item)ModItems.HVK_ADVANCED_STANDARD_TEMPLATE_CONSTRUCTOR.get())
            ? completedModAdvancements(player) < 12
            : crafted.is((Item)ModItems.HVK_ADVANCED_DISASSEMBLY_BEAM_EMITTER.get()) && completedPurpleAdvancements(player) < 7;
      }
   }

   private static boolean handleSpecialWorkBlockDisabledCraft(ItemCraftedEvent event) {
      ItemStack crafted = event.getCrafting();
      if (!crafted.isEmpty() && event.getEntity() instanceof ServerPlayer player && SpecialWorkBlockConfig.isDisabledItem(crafted)) {
         int craftedCount = Math.max(1, crafted.getCount());
         Item craftedItem = crafted.getItem();
         crafted.setCount(0);
         player.server.execute(() -> {
            removeItems(player, craftedItem, craftedCount);
            refundSpecialWorkBlockIngredients(player, craftedItem, craftedCount);
            SpecialWorkBlockConfig.notifyDisabled(player);
         });
         return true;
      } else {
         return false;
      }
   }

   private static boolean handleHvkConstructorCraftGate(ItemCraftedEvent event) {
      ItemStack crafted = event.getCrafting();
      if (!crafted.isEmpty() && event.getEntity() instanceof ServerPlayer player) {
         boolean var9 = crafted.is((Item)ModItems.HVK_ADVANCED_DISASSEMBLY_BEAM_EMITTER.get());
         boolean constructor = crafted.is((Item)ModItems.HVK_ADVANCED_STANDARD_TEMPLATE_CONSTRUCTOR.get());
         if (!constructor && !var9) {
            return false;
         } else {
            int completed = var9 ? completedPurpleAdvancements(player) : completedModAdvancements(player);
            int required = var9 ? 7 : 12;
            if (completed >= required) {
               return false;
            } else {
               int craftedCount = Math.max(1, crafted.getCount());
               Item craftedItem = crafted.getItem();
               crafted.setCount(0);
               player.server
                  .execute(
                     () -> {
                        removeItems(player, craftedItem, craftedCount);
                        if (var9) {
                           refundHvkDisassemblerIngredients(player, craftedCount);
                        } else {
                           refundHvkConstructorIngredients(player, craftedCount);
                        }

                        player.displayClientMessage(
                           Component.translatable(
                              var9
                                 ? "message.dealt_force_skills.hvk_disassembler.advancement_gate"
                                 : "message.dealt_force_skills.hvk_constructor.advancement_gate",
                              new Object[]{completed, required}
                           ),
                           true
                        );
                     }
                  );
               return true;
            }
         }
      } else {
         return false;
      }
   }

   private static int completedModAdvancements(ServerPlayer player) {
      int completed = 0;

      for (AdvancementHolder advancement : player.server.getAdvancements().getAllAdvancements()) {
         ResourceLocation id = advancement.id();
         if ("dealt_force_skills".equals(id.getNamespace())
            && !id.getPath().startsWith("recipes/")
            && player.getAdvancements().getOrStartProgress(advancement).isDone()) {
            completed++;
         }
      }

      return completed;
   }

   private static int completedPurpleAdvancements(ServerPlayer player) {
      int completed = 0;

      for (AdvancementHolder advancement : player.server.getAdvancements().getAllAdvancements()) {
         ResourceLocation id = advancement.id();
         if ("dealt_force_skills".equals(id.getNamespace())
            && id.getPath().startsWith("delta_force_skills/purple/")
            && player.getAdvancements().getOrStartProgress(advancement).isDone()) {
            completed++;
         }
      }

      return completed;
   }

   private static void refundHvkConstructorIngredients(Player player, int craftedCount) {
      giveOrDrop(player, new ItemStack((ItemLike)ModItems.PREMIUM_COFFEE_BEANS.get(), craftedCount));
      giveOrDrop(player, new ItemStack((ItemLike)ModItems.PROGRAMMABLE_PROCESSOR.get(), craftedCount * 2));
      giveOrDrop(player, new ItemStack((ItemLike)ModItems.SPECIAL_STEEL.get(), craftedCount * 3));
      giveOrDrop(player, new ItemStack((ItemLike)ModItems.TITANIUM_ALLOY.get(), craftedCount * 3));
   }

   private static void refundHvkDisassemblerIngredients(Player player, int craftedCount) {
      giveOrDrop(player, new ItemStack((ItemLike)ModItems.ARAMID_FIBER.get(), craftedCount * 3));
      giveOrDrop(player, new ItemStack((ItemLike)ModItems.POLYMER_FABRIC.get(), craftedCount * 3));
      giveOrDrop(player, new ItemStack((ItemLike)ModItems.BALLISTIC_CERAMIC.get(), craftedCount * 3));
   }

   private static void refundSpecialWorkBlockIngredients(Player player, Item craftedItem, int craftedCount) {
      if (craftedItem == ModItems.HVK_ADVANCED_STANDARD_TEMPLATE_CONSTRUCTOR.get()) {
         refundHvkConstructorIngredients(player, craftedCount);
      } else if (craftedItem == ModItems.HVK_ADVANCED_DISASSEMBLY_BEAM_EMITTER.get()) {
         refundHvkDisassemblerIngredients(player, craftedCount);
      } else if (craftedItem == ModItems.HVK_CLONE_PROTOTYPE.get()) {
         refundHvkClonePrototypeIngredients(player, craftedCount);
      } else if (craftedItem == ModItems.INTERDIMENSIONAL_BLOCK.get()) {
         refundInterdimensionalBlockIngredients(player, craftedCount);
      }
   }

   private static void refundHvkClonePrototypeIngredients(Player player, int craftedCount) {
      giveOrDrop(player, new ItemStack((ItemLike)ModItems.M1_MUSCLE_INJECTION.get(), craftedCount));
      giveOrDrop(player, new ItemStack((ItemLike)ModItems.PERCEPTION_ACTIVATOR.get(), craftedCount));
      giveOrDrop(player, new ItemStack((ItemLike)ModItems.STAMINA_ENHANCER.get(), craftedCount));
      giveOrDrop(player, new ItemStack((ItemLike)ModItems.DARK_ZONE_RAINBOW_INJECTION.get(), craftedCount));
      giveOrDrop(player, new ItemStack((ItemLike)ModItems.RAVEN_SHADOW_EXTRACT.get(), craftedCount));
      giveOrDrop(player, new ItemStack((ItemLike)ModItems.EXPERIMENTAL_IRON_CURTAIN_CATALYST.get(), craftedCount));
      giveOrDrop(player, new ItemStack((ItemLike)ModItems.PROTOTYPE_MADNESS_COMPOUND.get(), craftedCount));
      giveOrDrop(player, new ItemStack((ItemLike)ModItems.SPECIAL_EQUIPMENT_SUPPLY.get(), craftedCount));
      giveOrDrop(player, new ItemStack((ItemLike)ModItems.BATTLEFIELD_MEDICAL_KIT.get(), craftedCount));
   }

   private static void refundInterdimensionalBlockIngredients(Player player, int craftedCount) {
      giveOrDrop(player, new ItemStack(Items.HAY_BLOCK, craftedCount * 8));
      giveOrDrop(player, new ItemStack(Items.FEATHER, craftedCount));
   }

   private static int removeItems(ServerPlayer player, Item item, int max) {
      int removed = 0;
      ItemStack carried = player.containerMenu.getCarried();
      if (!carried.isEmpty() && carried.is(item)) {
         int take = Math.min(max, carried.getCount());
         carried.shrink(take);
         removed += take;
         player.containerMenu.setCarried(carried.isEmpty() ? ItemStack.EMPTY : carried);
      }

      for (int slot = 0; slot < player.getInventory().getContainerSize() && removed < max; slot++) {
         ItemStack stack = player.getInventory().getItem(slot);
         if (!stack.isEmpty() && stack.is(item)) {
            int take = Math.min(max - removed, stack.getCount());
            stack.shrink(take);
            removed += take;
         }
      }

      if (removed > 0) {
         player.getInventory().setChanged();
         player.containerMenu.broadcastChanges();
      }

      return removed;
   }

   @SubscribeEvent
   public static void onVillagerTrades(VillagerTradesEvent event) {
      if (BLUEPRINT_PROFESSIONS.contains(event.getType())) {
         ItemListing trade = CommonEvents::blueprintTradeOffer;

         for (int level : BLUEPRINT_TRADE_LEVELS) {
            ((List)event.getTrades().computeIfAbsent(level, ignored -> new ArrayList<>())).add(trade);
         }
      }
   }

   @SubscribeEvent
   public static void onWandererTrades(WandererTradesEvent event) {
      event.getRareTrades().add(CommonEvents::blueprintTradeOffer);
   }

   @SubscribeEvent
   public static void onOceanBucketUse(RightClickBlock event) {
      if (!event.getLevel().isClientSide) {
         if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            SaeedStateManager.onOwnerInteractBlock(serverPlayer, event.getPos());
         }

         tryDuplicateUndeadExplorerContainerLoot(event);
         if (event.getItemStack().is(Items.BUCKET)) {
            tryAwardCoarseSalt(event.getEntity(), event.getLevel(), event.getPos(), 0);
         }
      }
   }


   @SubscribeEvent(priority = EventPriority.HIGH)
   public static void onHvkBrainBeaconSummon(RightClickBlock event) {
      if (event.getLevel().isClientSide()) {
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
   public static void onCommanderServerTick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post event) {
      CommanderSupportManager.tickServer(event.getServer());
   }

   @SubscribeEvent
   public static void onCommanderRadiationSpawn(
      net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent event) {
      if (event.getEntity().level() instanceof ServerLevel level
         && CommanderSupportManager.shouldPreventSpawn(level, event.getEntity().blockPosition())) {
         event.setSpawnCancelled(true);
      }
   }

   @SubscribeEvent
   public static void onPlayerTickUluruEffectResistance(Post event) {
      Player player = event.getEntity();
      if (!player.level().isClientSide) {
         if (!UluruStateManager.isUluru(player)) {
            ULURU_EFFECT_TRACKING.remove(player.getUUID());
         } else {
            Map<Holder<MobEffect>, CommonEvents.TrackedEffect> tracked = ULURU_EFFECT_TRACKING.computeIfAbsent(player.getUUID(), id -> new HashMap<>());
            Set<Holder<MobEffect>> activeTypes = new HashSet<>();

            for (MobEffectInstance effect : new ArrayList<>(player.getActiveEffects())) {
               Holder<MobEffect> type = effect.getEffect();
               if (((MobEffect)type.value()).getCategory() == MobEffectCategory.HARMFUL && !effect.isInfiniteDuration()) {
                  activeTypes.add(type);
                  CommonEvents.TrackedEffect previous = tracked.get(type);
                  int currentDuration = effect.getDuration();
                  if (previous != null && previous.amplifier() == effect.getAmplifier() && currentDuration <= previous.duration() + EFFECT_REAPPLY_GRACE_TICKS) {
                     tracked.put(type, new CommonEvents.TrackedEffect(currentDuration, effect.getAmplifier()));
                  } else {
                     int reducedDuration = Math.max(1, currentDuration / 2);
                     MobEffectInstance reduced = new MobEffectInstance(
                        type, reducedDuration, effect.getAmplifier(), effect.isAmbient(), effect.isVisible(), effect.showIcon()
                     );
                     player.removeEffect(type);
                     player.addEffect(reduced);
                     tracked.put(type, new CommonEvents.TrackedEffect(reducedDuration, effect.getAmplifier()));
                  }
               }
            }

            tracked.keySet().removeIf(typex -> !activeTypes.contains(typex));
         }
      }
   }

   @SubscribeEvent(
      priority = EventPriority.HIGHEST
   )
   public static void onPlayerLeftClickBlock(LeftClickBlock event) {
      handlePlayerInteract(event);
   }

   @SubscribeEvent(
      priority = EventPriority.HIGHEST
   )
   public static void onPlayerLeftClickEmpty(LeftClickEmpty event) {
      handlePlayerInteract(event);
   }

   @SubscribeEvent(
      priority = EventPriority.HIGHEST
   )
   public static void onPlayerRightClickBlock(RightClickBlock event) {
      handlePlayerInteract(event);
   }

   @SubscribeEvent(
      priority = EventPriority.HIGHEST
   )
   public static void onPlayerRightClickEmpty(RightClickEmpty event) {
      handlePlayerInteract(event);
   }

   @SubscribeEvent(
      priority = EventPriority.HIGHEST
   )
   public static void onPlayerRightClickItem(RightClickItem event) {
      handlePlayerInteract(event);
   }

   @SubscribeEvent(
      priority = EventPriority.HIGHEST
   )
   public static void onPlayerEntityInteract(EntityInteract event) {
      handlePlayerInteract(event);
   }

   @SubscribeEvent(
      priority = EventPriority.HIGHEST
   )
   public static void onPlayerEntityInteractSpecific(EntityInteractSpecific event) {
      handlePlayerInteract(event);
   }

   private static void handlePlayerInteract(PlayerInteractEvent event) {
      Player player = event.getEntity();
      if (player instanceof ServerPlayer serverPlayer) {
         MorseStateManager.recordPlayerAction(serverPlayer);
         LexNinjiaStateManager.onPlayerAction(serverPlayer);
         if (GamblerArenaManager.blocksInteraction(serverPlayer)) {
            cancelInteraction(event, InteractionResult.FAIL);
            return;
         }
      }

      if (player instanceof ServerPlayer serverPlayerx
         && (UluruLoiteringMissileEntity.isPlayerControlling(serverPlayerx) || RaptorFalconDroneEntity.isPlayerControlling(serverPlayerx))) {
         cancelInteraction(event, InteractionResult.FAIL);
         return;
      }

      if (!(com.rzy.dealt_force_skills.character.rover.RoverStateManager.isDowned(player) || StingerStateManager.isDowned(player))
         && !VlinderStateManager.isDowned(player)
         && !CatDadStateManager.isDowned(player)
         && !TempestStateManager.isActionLocked(player)) {
         boolean webbedLeftClick = effectApplies(player, ModEffects.WEBBED) && event instanceof LeftClickBlock;
         if (effectApplies(player, ModEffects.MORSE_STRONG_SHOCK) && shouldMorseShockSkipAction(player)) {
            cancelInteraction(event, InteractionResult.FAIL);
         } else if (effectApplies(player, ModEffects.SONIC_SHOCK) && shouldSonicShockSkipAction(player)) {
            cancelInteraction(event, InteractionResult.FAIL);
         } else if (effectApplies(player, ModEffects.STUN)
            || effectApplies(player, ModEffects.RAPTOR_ACTION_PAUSE)
            || effectApplies(player, ModEffects.WEBBED) && !webbedLeftClick) {
            cancelInteraction(event, InteractionResult.FAIL);
         } else if (!tryCutTargetedBladeWire(event)) {
            if (isAnyToolEquipped(player)) {
               cancelInteraction(event, InteractionResult.FAIL);
            } else {
               if (player instanceof ServerPlayer serverPlayerx) {
                  DfsAchievements.recordToxikAdrenalineAction(serverPlayerx, "interact");
               }
            }
         }
      } else {
         cancelInteraction(event, InteractionResult.FAIL);
      }
   }

   @SubscribeEvent(
      priority = EventPriority.HIGHEST
   )
   public static void onAttackEntity(AttackEntityEvent event) {
      Player player = event.getEntity();
      if (player instanceof ServerPlayer serverPlayer) {
         MorseStateManager.recordPlayerAction(serverPlayer);
         if (GamblerArenaManager.blocksAttackEntity(serverPlayer, event.getTarget())) {
            event.setCanceled(true);
            return;
         }
      }

      if (player instanceof ServerPlayer serverPlayerx
         && (UluruLoiteringMissileEntity.isPlayerControlling(serverPlayerx) || RaptorFalconDroneEntity.isPlayerControlling(serverPlayerx))) {
         event.setCanceled(true);
         return;
      }

      if ((com.rzy.dealt_force_skills.character.rover.RoverStateManager.isDowned(player) || StingerStateManager.isDowned(player))
         || VlinderStateManager.isDowned(player)
         || CatDadStateManager.isDowned(player)
         || TempestStateManager.isActionLocked(player)
         || RoundStartFreezeManager.isFrozen(player)) {
         event.setCanceled(true);
      } else if (effectApplies(player, ModEffects.STUN) || effectApplies(player, ModEffects.RAPTOR_ACTION_PAUSE)) {
         event.setCanceled(true);
      } else if (effectApplies(player, ModEffects.MORSE_STRONG_SHOCK) && shouldMorseShockSkipAction(player)) {
         event.setCanceled(true);
      } else if (effectApplies(player, ModEffects.SONIC_SHOCK) && shouldSonicShockSkipAction(player)) {
         event.setCanceled(true);
      } else {
         if (player instanceof ServerPlayer serverPlayerx && event.getTarget() instanceof LivingEntity target) {
            LexNinjiaStateManager.handleAttackEntity(serverPlayerx, target);
         }

         if (isAnyToolEquipped(player)) {
            event.setCanceled(true);
         }
      }
   }

   @SubscribeEvent
   public static void onPlayerTickLesRaisinsMeleeWireCut(Post event) {
      if (event.getEntity() instanceof ServerPlayer serverPlayer) {
         LesRaisinsTacticalCompat.MeleeCutShape shape = LesRaisinsTacticalCompat.consumeMeleeAttackShape(serverPlayer);
         if (shape != null) {
            cutBladeWiresForLesRaisinsMeleeAttack(serverPlayer, shape);
         }
      }
   }

   private static void cutBladeWiresForLesRaisinsMeleeAttack(ServerPlayer player, LesRaisinsTacticalCompat.MeleeCutShape shape) {
      CompoundTag data = player.getPersistentData();
      long now = player.level().getGameTime();
      if (data.getLong("dealt_force_skills.melee_wire_cut_tick") != now) {
         data.putLong("dealt_force_skills.melee_wire_cut_tick", now);
         BladeWireBlockEntity.destroyWiresInMeleeArc(player, shape.range(), shape.halfWidth(), shape.height());
      }
   }

   private static boolean tryCutTargetedBladeWire(PlayerInteractEvent event) {
      if (event instanceof LeftClickBlock leftClick
         && leftClick.getEntity() instanceof ServerPlayer player
         && leftClick.getLevel() instanceof ServerLevel level
         && MeleeWeaponCompat.isMeleeWeapon(player.getMainHandItem())) {
         BlockPos pos = leftClick.getPos();
         if (!BladeWireBlockEntity.destroyWireBlock(level, pos, player)) {
            return false;
         }

         damageCuttingWeapon(player);
         leftClick.setCanceled(true);
         leftClick.setUseBlock(TriState.FALSE);
         leftClick.setUseItem(TriState.FALSE);
         return true;
      }

      return false;
   }

   private static void damageCuttingWeapon(ServerPlayer player) {
      ItemStack weapon = player.getMainHandItem();
      if (!weapon.isEmpty() && !player.getAbilities().instabuild) {
         weapon.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
      }
   }

   @SubscribeEvent(
      priority = EventPriority.HIGHEST
   )
   public static void onUseItemTick(Tick event) {
      if (event.getEntity() instanceof Player player
         && (
            (com.rzy.dealt_force_skills.character.rover.RoverStateManager.isDowned(player) || StingerStateManager.isDowned(player))
               || VlinderStateManager.isDowned(player)
               || CatDadStateManager.isDowned(player)
               || NikaidouHiroStateManager.isWeaponActive(player)
               || NikaidouHiroWitchificationStateManager.isWeaponActive(player)
               || NikaidouHiroWitchificationStateManager.isActionLocked(player)
               || DepartmentOfTransportationStateManager.hasEquippedTool(player)
               || TempestStateManager.isActionLocked(player)
               || UndeadStateManager.isActionLocked(player)
               || UndeadStateManager.isRitualDancing(player)
         )) {
         boolean tempestDisarmed = effectApplies(player, ModEffects.TEMPEST_DISARMED);
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

      if (event.getEntity() instanceof ServerPlayer player && RaptorFalconDroneEntity.isPlayerControlling(player)) {
         stopBlockedUseItem(event, player);
         return;
      }

      if (event.getEntity() instanceof ServerPlayer player) {
         MorseStateManager.recordPlayerAction(player);
         LexNinjiaStateManager.onPlayerAction(player);
         if (GamblerArenaManager.blocksInteraction(player)) {
            stopBlockedUseItem(event, player);
            return;
         }
      }

      if (event.getEntity() instanceof Player playerx && effectApplies(playerx, ModEffects.RAPTOR_ACTION_PAUSE)) {
         stopBlockedUseItem(event, playerx);
         return;
      }

      if (shouldAdjustUseDuration(event.getItem())) {
         if (event.getEntity() instanceof Player playerx && effectApplies(playerx, ModEffects.SONIC_SHOCK) && shouldSonicShockSkipAction(playerx)) {
            event.setDuration(event.getDuration() + 1);
         }

         if (event.getEntity() instanceof Player playerx && effectApplies(playerx, ModEffects.MORSE_STRONG_SHOCK) && shouldMorseShockSkipAction(playerx)) {
            event.setDuration(event.getDuration() + 1);
         }
      }
   }

   private static boolean isAnyToolEquipped(Player player) {
      if (SinevaSkills.canUseShieldAction(player)) {
         return true;
      } else if (UluruStateManager.isUluru(player) && UluruStateManager.equippedTool(player) != UluruTool.NONE) {
         return true;
      } else if (DWolfStateManager.isDWolf(player) && DWolfStateManager.equippedTool(player) != DWolfTool.NONE) {
         return true;
      } else if (GizmoStateManager.isGizmo(player) && GizmoStateManager.equippedTool(player) != GizmoTool.NONE) {
         return true;
      } else if (ShepherdStateManager.isShepherd(player) && ShepherdStateManager.equippedTool(player) != ShepherdTool.NONE) {
         return true;
      } else if (LunaStateManager.isLuna(player) && LunaStateManager.equippedTool(player) != LunaTool.NONE) {
         return true;
      } else if (VyronStateManager.isVyron(player) && VyronStateManager.equippedTool(player) != VyronTool.NONE) {
         return true;
      } else if (StingerStateManager.isStinger(player) && StingerStateManager.equippedTool(player) != StingerTool.NONE) {
         return true;
      } else if (ManbaStateManager.isManba(player) && ManbaStateManager.isFlashlightActive(player)) {
         return true;
      } else if (NikaidouHiroStateManager.isNikaidouHiro(player) && NikaidouHiroStateManager.equippedTool(player) != NikaidouHiroTool.NONE) {
         return true;
      } else if (NikaidouHiroWitchificationStateManager.isWeaponActive(player)) {
         return true;
      } else if (DepartmentOfTransportationStateManager.hasEquippedTool(player)) {
         return true;
      } else if (ChamberStateManager.isChamber(player) && ChamberStateManager.equippedTool(player) != ChamberTool.NONE) {
         return true;
      } else if (NTwoStateManager.isNTwo(player) && NTwoStateManager.equippedTool(player) != NTwoTool.NONE) {
         return true;
      } else if (MorseStateManager.isMorse(player) && MorseStateManager.equippedTool(player) != MorseTool.NONE) {
         return true;
      } else if (ToxikStateManager.isToxik(player) && ToxikStateManager.equippedTool(player) != ToxikTool.NONE) {
         return true;
      } else if (RaptorStateManager.isRaptor(player) && RaptorStateManager.equippedTool(player) != RaptorTool.NONE) {
         return true;
      } else {
         return VlinderStateManager.isVlinder(player) && VlinderStateManager.equippedTool(player) != VlinderTool.NONE
            ? true
            : TempestStateManager.isTempest(player) && TempestStateManager.equippedTool(player) != TempestTool.NONE;
      }
   }

   @SubscribeEvent
   public static void onPlayerTickRoundFreeze(Post event) {
      Player player = event.getEntity();
      if (!player.level().isClientSide && RoundStartFreezeManager.isFrozen(player)) {
         player.setDeltaMovement(Vec3.ZERO);
         player.setSprinting(false);
         player.fallDistance = 0.0F;
      }
   }

   @SubscribeEvent
   public static void onPlayerTickStun(Post event) {
      Player player = event.getEntity();
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
      CommonEvents.TaczSpeedMultipliers superbWarfareMultipliers = taczSpeedMultipliers(player);
      boolean optionalGunDisabled = (com.rzy.dealt_force_skills.character.rover.RoverStateManager.isDowned(player) || StingerStateManager.isDowned(player))
         || VlinderStateManager.isDowned(player)
         || CatDadStateManager.isDowned(player)
         || TempestStateManager.isActionLocked(player)
         || NikaidouHiroStateManager.isWeaponActive(player)
         || isAnyToolEquipped(player)
         || effectApplies(player, ModEffects.STUN)
         || effectApplies(player, ModEffects.RAPTOR_ACTION_PAUSE)
         || effectApplies(player, ModEffects.SONIC_SHOCK) && shouldSonicShockSkipAction(player)
         || effectApplies(player, ModEffects.MORSE_STRONG_SHOCK) && shouldMorseShockSkipAction(player);
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
      if (!(com.rzy.dealt_force_skills.character.rover.RoverStateManager.isDowned(player) || StingerStateManager.isDowned(player)) && !VlinderStateManager.isDowned(player) && !CatDadStateManager.isDowned(player)) {
         if (NikaidouHiroStateManager.isWeaponActive(player)) {
            player.stopUsingItem();
            if (player instanceof ServerPlayer nikaidouPlayer) {
               cancelOptionalTaczReload(nikaidouPlayer);
            }
         }

         if (NikaidouHiroWitchificationStateManager.isWeaponActive(player) || NikaidouHiroWitchificationStateManager.isActionLocked(player)) {
            player.stopUsingItem();
            player.setSprinting(false);
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

         if (!TempestStateManager.isActionLocked(player)) {
            if (player instanceof ServerPlayer serverPlayer) {
               TEMPEST_ACTION_LOCKED_SELECTED_SLOTS.remove(serverPlayer.getUUID());
            }
         } else {
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
         }

         if (effectApplies(player, ModEffects.STUN)) {
            Vec3 velocity = player.getDeltaMovement();
            player.setDeltaMovement(0.0, velocity.y, 0.0);
            player.stopUsingItem();
            if (!player.level().isClientSide && player.containerMenu != player.inventoryMenu) {
               player.closeContainer();
            }

            if (player instanceof ServerPlayer stunnedPlayer) {
               UluruLoiteringMissileEntity.stopPlayerControl(stunnedPlayer);
               RaptorFalconDroneEntity.stopPlayerControl(stunnedPlayer);
            }
         }

         if (effectApplies(player, ModEffects.RAPTOR_ACTION_PAUSE)) {
            Vec3 velocityx = player.getDeltaMovement();
            player.setDeltaMovement(0.0, velocityx.y, 0.0);
            player.stopUsingItem();
            if (!player.level().isClientSide && player.containerMenu != player.inventoryMenu) {
               player.closeContainer();
            }
         }

         MobEffectInstance morseStrongShock = activeEffectInstance(player, ModEffects.MORSE_STRONG_SHOCK);
         if (morseStrongShock != null) {
            Vec3 velocityx = player.getDeltaMovement();
            double factor = morseStrongShock.getAmplifier() >= 1 ? 0.25 : 0.55;
            player.setDeltaMovement(velocityx.x * factor, velocityx.y, velocityx.z * factor);
            player.setSprinting(false);
            player.hurtMarked = true;
         }

         if (effectApplies(player, ModEffects.WEBBED)) {
            Vec3 velocityx = player.getDeltaMovement();
            player.setDeltaMovement(0.0, Math.min(0.0, velocityx.y), 0.0);
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
      } else {
         player.stopUsingItem();
         player.setSprinting(false);
         if (!player.level().isClientSide && player.containerMenu != player.inventoryMenu) {
            player.closeContainer();
         }

         if (player instanceof ServerPlayer downedPlayer) {
            UluruLoiteringMissileEntity.stopPlayerControl(downedPlayer);
            RaptorFalconDroneEntity.stopPlayerControl(downedPlayer);
         }
      }
   }

   @SubscribeEvent
   public static void onPlayerTickSonicShockTimeFlow(Post event) {
      Player player = event.getEntity();
      MobEffectInstance sonicShock = activeEffectInstance(player, ModEffects.SONIC_SHOCK);
      if (sonicShock != null) {
         double factor = sonicShock.getAmplifier() >= 1 ? 0.25 : 0.55;
         Vec3 velocity = player.getDeltaMovement();
         player.setDeltaMovement(velocity.x * factor, velocity.y, velocity.z * factor);
         player.setSprinting(false);
         player.hurtMarked = true;
      }
   }

   @SubscribeEvent
   public static void onPlayerTickMissileControlLock(Post event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         boolean var4 = UluruLoiteringMissileEntity.isPlayerControlling(player);
         boolean raptorControl = RaptorFalconDroneEntity.isPlayerControlling(player);
         if (var4 || raptorControl) {
            if (var4) {
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
      }
   }

   @SubscribeEvent
   public static void onWorldTickCharge(net.neoforged.neoforge.event.tick.LevelTickEvent.Post event) {
      if (!event.getLevel().isClientSide) {
         SinevaSkills.tickChargingPlayers(event.getLevel());
         if (event.getLevel() instanceof ServerLevel serverLevel && serverLevel == serverLevel.getServer().overworld()) {
            com.rzy.dealt_force_skills.team.TeammateRevealSync.serverTick(serverLevel.getServer());
         }
         HvkZiplineManager.tick(event.getLevel());
      }
   }

   @SubscribeEvent
   public static void onExplosionDetonate(Detonate event) {
      if (event.getLevel() instanceof ServerLevel serverLevel) {
         boolean gamblerArena = GamblerArenaManager.protectsArenaBlocks(serverLevel);
         if (gamblerArena) {
            event.getAffectedBlocks().clear();
         }

         Entity directSource = event.getExplosion().getDirectSourceEntity();
         boolean departmentChargedCreeper = DepartmentOfTransportationStateManager.isDepartmentChargedCreeper(directSource);
         SaeedGuardEntity hakimRocketOwner = saeedHakimGuardFromRocketExplosion(directSource);
         boolean hakimRocket = hakimRocketOwner != null;
         if (!gamblerArena && !departmentChargedCreeper && !hakimRocket) {
            UluruExplosionHelper.destroyQuickCovers(serverLevel, event.getExplosion().center(), 6.0);
         }

         TempestStateManager.triggerExplosionSpineForNearby(serverLevel, event.getExplosion().center(), event.getAffectedEntities());
         if (departmentChargedCreeper) {
            removeDepartmentOwnerFromExplosionTargets(directSource, event.getAffectedEntities());
            event.getAffectedBlocks().clear();
            SuperbWarfareCompat.damageVehicles(
               serverLevel,
               event.getExplosion().center(),
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
               event.getExplosion().center(),
               SAEED_HAKIM_ROCKET_DAMAGE_RADIUS,
               SkillDamageHelper.uluruMissile(serverLevel, hakimRocketOwner, hakimRocketOwner),
               directSource,
               SAEED_HAKIM_ROCKET_DAMAGE / 100.0F,
               true
            );
            SuperbWarfareCompat.removeVehicles(event.getAffectedEntities());
            preventSaeedHakimRocketKnockback(serverLevel, hakimRocketOwner, event.getExplosion().center(), event.getAffectedEntities());
         }

         for (BlockPos pos : event.getAffectedBlocks()) {
            if (serverLevel.getBlockState(pos).is((Block)ModBlocks.QUICK_COVER.get())) {
               QuickCoverBlockEntity.destroyCoverAt(serverLevel, pos);
            }
         }
      }
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

   @SubscribeEvent(
      priority = EventPriority.HIGHEST
   )
   public static void onExplosionStart(net.neoforged.neoforge.event.level.ExplosionEvent.Start event) {
      if (event.getLevel() instanceof ServerLevel serverLevel) {
         Entity directSource = event.getExplosion().getDirectSourceEntity();
         if (UndeadStateManager.isHunterRetreatTnt(directSource)) {
            event.setCanceled(true);
            UndeadStateManager.handleHunterRetreatExplosion(serverLevel, directSource);
         }
      }
   }

   private static SaeedGuardEntity saeedHakimGuardFromRocketExplosion(Entity directSource) {
      if (directSource instanceof SaeedGuardEntity guard && guard.guardType() == SaeedGuardType.HAKIM) {
         return guard;
      }

      if (directSource instanceof Projectile projectile && projectile.getOwner() instanceof SaeedGuardEntity guard && guard.guardType() == SaeedGuardType.HAKIM
         )
       {
         return guard;
      }

      return null;
   }

   private static void preventSaeedHakimRocketKnockback(ServerLevel level, SaeedGuardEntity owner, Vec3 center, List<Entity> affectedEntities) {
      affectedEntities.removeIf(entity -> {
         if (!(entity instanceof ServerPlayer) && !(entity instanceof SaeedGuardEntity)) {
            return false;
         } else {
            if (entity instanceof LivingEntity living && living.isAlive()) {
               Vec3 entityCenter = living.position().add(0.0, (double)living.getBbHeight() * 0.5, 0.0);
               double distance = entityCenter.distanceTo(center);
               if (distance <= SAEED_HAKIM_ROCKET_DAMAGE_RADIUS && UluruExplosionHelper.hasExplosionLineOfSight(level, center, living)) {
                  float damage = SAEED_HAKIM_ROCKET_DAMAGE * Math.max(0.0F, 1.0F - (float)distance * 0.25F);
                  if (damage > 0.0F) {
                     living.invulnerableTime = 0;
                     SkillDamageHelper.hurt(living, SkillDamageHelper.uluruMissile(level, owner, owner), owner, damage);
                  }
               }

               living.setDeltaMovement(Vec3.ZERO);
               living.hurtMarked = true;
               return true;
            }

            return true;
         }
      });
   }

   @SubscribeEvent(
      priority = EventPriority.HIGHEST
   )
   public static void onTaczAttachmentProperty(AttachmentPropertyEvent event) {
      handleGhrothAttachmentProperty(event);
      handleChamberAttachmentProperty(event);
   }

   @SubscribeEvent(
      priority = EventPriority.HIGHEST
   )
   public static void onTaczPreDamage(Pre event) {
      if (!cancelManbaDuelTaczAction(event)) {
         if (cancelTeamTaczPreDamage(event)) {
            return;
         }
         if (!handleShakehandsTaczPreDamage(event)) {
            allowPortableAdsTaczHit(event);
            recordSaeedGuardTaczPreDamage(event);
            handleGhrothTaczPreDamage(event);
            handleChamberTaczPreDamage(event);
         }
      }
   }

   private static void allowPortableAdsTaczHit(Pre event) {
      try {
         Object attackerObject = event.getClass().getMethod("getAttacker").invoke(event);
         Object hurtObject = event.getClass().getMethod("getHurtEntity").invoke(event);
         if (hurtObject instanceof ServerPlayer player && attackerObject instanceof Entity attacker
            && com.rzy.dealt_force_skills.item.PortableAdsDeviceItem.isActive(player)) {
            com.rzy.dealt_force_skills.item.PortableAdsDeviceItem.allowNextTaczHit(player, attacker);
         }
      } catch (LinkageError | ReflectiveOperationException ignored) {
      }
   }

   private static boolean cancelTeamTaczPreDamage(Pre event) {
      try {
         Object attackerObject = event.getClass().getMethod("getAttacker").invoke(event);
         Object hurtObject = event.getClass().getMethod("getHurtEntity").invoke(event);
         if (!(attackerObject instanceof LivingEntity attacker) || !(hurtObject instanceof LivingEntity target)) {
            return false;
         }
         if (ManbaStateManager.arePlayerDuelPartners(
               attacker instanceof Player ap ? ap : null,
               target instanceof Player tp ? tp : null)) {
            return false;
         }
         if (!TeamCombatRules.shouldCancelFriendlyDamage(target, attacker)) {
            return false;
         }
         event.setCanceled(true);
         try {
            event.getClass().getMethod("setBaseAmount", float.class).invoke(event, 0.0F);
         } catch (ReflectiveOperationException ignored) {
         }
         return true;
      } catch (LinkageError | ReflectiveOperationException ignored) {
         return false;
      }
   }

   @SubscribeEvent(
      priority = EventPriority.HIGHEST
   )
   public static void onTaczGunReload(GunReloadEvent event) {
      recordMorseTaczAction(event);
      if (!cancelManbaDuelTaczAction(event)
         && !cancelLexNinjiaTaczAction(event)
         && !cancelUndeadTaczAction(event)
         && !cancelDownedTaczAction(event)
         && !cancelFrozenTaczAction(event)) {
         cancelTaczReloadEvent(event);
         if (throttleMorseShockTaczTimeEvent(event) || throttleSonicTaczTimeEvent(event)) {
            ;
         }
      }
   }

   @SubscribeEvent(
      priority = EventPriority.HIGHEST
   )
   public static void onTaczGunShoot(GunShootEvent event) {
      handleTaczSonicTimeEvent("com.tacz.guns.api.event.common.GunShootEvent", event);
   }

   @SubscribeEvent(
      priority = EventPriority.HIGHEST
   )
   public static void onTaczGunFire(GunFireEvent event) {
      handleTaczSonicTimeEvent("com.tacz.guns.api.event.common.GunFireEvent", event);
   }

   @SubscribeEvent(
      priority = EventPriority.HIGHEST
   )
   public static void onTaczGunMelee(GunMeleeEvent event) {
      handleTaczSonicTimeEvent("com.tacz.guns.api.event.common.GunMeleeEvent", event);
   }

   @SubscribeEvent(
      priority = EventPriority.HIGHEST
   )
   public static void onTaczGunFireSelect(GunFireSelectEvent event) {
      handleTaczSonicTimeEvent("com.tacz.guns.api.event.common.GunFireSelectEvent", event);
   }

   private static void handleTaczSonicTimeEvent(String eventName, Event event) {
      recordMorseTaczAction(event);
      if (!cancelManbaDuelTaczAction(event)
         && !cancelLexNinjiaTaczAction(event)
         && !cancelUndeadTaczAction(event)
         && !cancelDownedTaczAction(event)
         && !cancelFrozenTaczAction(event)
         && !throttleMorseShockTaczTimeEvent(event)
         && !throttleSonicTaczTimeEvent(event)) {
         accelerateTaczAdrenalineForEvent(eventName, event);
         snapshotMhsTaczAmmo(eventName, event);
         if ("com.tacz.guns.api.event.common.GunFireEvent".equals(eventName)) {
            handleGhrothStarsGunFire(event);
         }
      }
   }

   @SubscribeEvent(
      priority = EventPriority.HIGHEST
   )
   public static void onTaczAmmoHitBlock(AmmoHitBlockEvent event) {
      try {
         Object levelObject = event.getClass().getMethod("getLevel").invoke(event);
         Object hitObject = event.getClass().getMethod("getHitResult").invoke(event);
         Object ammoObject = event.getClass().getMethod("getAmmo").invoke(event);
         if (levelObject instanceof Level level && !level.isClientSide && hitObject instanceof BlockHitResult hit) {
            BlockPos pos = hit.getBlockPos();
            int damage = ammoObject instanceof Projectile projectile ? QuickCoverBlock.projectileDamage(projectile, hit.getLocation()) : 25;
            if (level.getBlockState(pos).is((Block)ModBlocks.QUICK_COVER.get())) {
               QuickCoverBlockEntity.damageCoverAt(level, pos, damage);
               return;
            }

            if (level.getBlockState(pos).is((Block)ModBlocks.BLADE_WIRE_CORE.get()) || level.getBlockState(pos).is((Block)ModBlocks.BLADE_WIRE.get())) {
               BladeWireBlockEntity.damageWireAt(level, pos, damage);
            }

            return;
         }
      } catch (ReflectiveOperationException var9) {
      }
   }

   private static void handleGhrothTaczPreDamage(Event event) {
      try {
         Object attackerObject = event.getClass().getMethod("getAttacker").invoke(event);
         Object hurtObject = event.getClass().getMethod("getHurtEntity").invoke(event);
         if (!(attackerObject instanceof ServerPlayer attacker) || !GhrothStateManager.isGhroth(attacker) || !(hurtObject instanceof LivingEntity target)) {
            return;
         }

         if (target instanceof Player targetPlayer && GhrothStateManager.shouldCancelCeasefire(attacker, targetPlayer)) {
            if (isEventCancelable(event)) {
               cancelEvent(event);
            }

            return;
         }

         ItemStack gun = attacker.getMainHandItem();
         if (isTaczGunStack(gun)) {
            ResourceLocation gunId = null;
            if (event.getClass().getMethod("getGunId").invoke(event) instanceof ResourceLocation resourceLocation) {
               gunId = resourceLocation;
            }

            float baseAmount = reflectedFloat(event, "getBaseAmount", 0.0F);
            float headshotMultiplier = reflectedFloat(event, "getHeadshotMultiplier", 1.0F);
            boolean headshot = reflectedBoolean(event, "isHeadShot", false);
            double newBaseAmountValue = (double)baseAmount * GhrothTaczEnhancement.levelDamageMultiplier(attacker);
            if (GhrothTaczEnhancement.isEnhancedGun(gun)) {
               newBaseAmountValue = Math.max(newBaseAmountValue, (double)GhrothTaczEnhancement.estimatedShotDamage(attacker, gun, gunId));
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
            if (GhrothStateManager.isNoonActive(attacker)) {
               DfsAchievements.recordGhrothNoonTaczDamage(attacker, target);
            }
         }
      } catch (LinkageError | ReflectiveOperationException var16) {
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
         event.getClass().getMethod("setHeadshotMultiplier", float.class).invoke(event, ChamberTaczEnhancement.headshotMultiplier(headshotMultiplier, gun));
         if (ChamberTaczEnhancement.kind(gun) == ChamberGunKind.TOUR_DE_FORCE
            && hurtObject instanceof LivingEntity target
            && reflectedFloat(event, "getBaseAmount", 0.0F) > 0.0F) {
            ChamberStateManager.tryTriggerTourSlowField(attacker, target);
         }
      } catch (LinkageError | ReflectiveOperationException var7) {
      }
   }

   private static boolean handleShakehandsTaczPreDamage(Pre event) {
      try {
         Object attackerObject = event.getClass().getMethod("getAttacker").invoke(event);
         Object hurtObject = event.getClass().getMethod("getHurtEntity").invoke(event);
         if (attackerObject instanceof LivingEntity attacker && hurtObject instanceof LivingEntity target && attacker != target) {
            float baseAmount = reflectedFloat(event, "getBaseAmount", 0.0F);
            float headshotMultiplier = reflectedFloat(event, "getHeadshotMultiplier", 1.0F);
            boolean headshot = reflectedBoolean(event, "isHeadShot", false);
            float amount = headshot ? baseAmount * Math.max(1.0F, headshotMultiplier) : baseAmount;
            if (amount <= 0.0F) {
               amount = Math.max(1.0F, baseAmount);
            }

            if (!ShakehandsCombatHandler.tryReflectFromAttacker(target, attacker, amount)) {
               return false;
            }

            event.setCanceled(true);

            try {
               event.getClass().getMethod("setBaseAmount", float.class).invoke(event, 0.0F);
            } catch (ReflectiveOperationException var10) {
            }

            return true;
         }

         return false;
      } catch (LinkageError | ReflectiveOperationException var11) {
         return false;
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
            return;
         }
      } catch (LinkageError | ReflectiveOperationException var4) {
      }
   }

   private static void handleGhrothAttachmentProperty(Event event) {
      try {
         if (!(event.getClass().getMethod("getGunItem").invoke(event) instanceof ItemStack gun)) {
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
            multiplyFloatCache(cache, "HEADSHOT_MULTIPLIER", (float)GhrothTaczEnhancement.enhancedHeadshotMultiplier(gun, gunId));
            multiplyFloatCache(cache, "EFFECTIVE_RANGE", 2.0F);
            multiplyFloatCache(cache, "AMMO_SPEED", 2.0F);
            addFloatCache(cache, "ARMOR_IGNORE", 1.0F);
            multiplyIntegerCache(cache, "ROUNDS_PER_MINUTE", 2.0);
            multiplyIntegerCache(cache, "PIERCE", 2.0);
            zeroFloatCache(cache, "ADS_TIME");
            zeroFloatCache(cache, "WEIGHT");
            zeroInaccuracyMapCache(cache, "INACCURACY");
            zeroInaccuracyMapCache(cache, "AIM_INACCURACY");
            maximizeRecoilControlCache(cache);
         }

         if (enhancedAttachmentCount > 0) {
            double multiplier = GhrothTaczEnhancement.enhancedAttachmentPropertyMultiplier(enhancedAttachmentCount);
            boostPositiveDamageCache(cache, multiplier);
            boostPositiveFloatCache(cache, "HEADSHOT_MULTIPLIER", multiplier, 1.0F, 1000000.0F);
            boostPositiveFloatCache(cache, "EFFECTIVE_RANGE", multiplier, 1.0F, 1000000.0F);
            boostPositiveFloatCache(cache, "ARMOR_IGNORE", multiplier, 1.0F, 100.0F);
            boostPositiveIntegerCache(cache, "ROUNDS_PER_MINUTE", multiplier, 1, 1000000);
            boostPositiveIntegerCache(cache, "PIERCE", multiplier, 1, 1000000);
            zeroFloatCache(cache, "ADS_TIME");
            zeroFloatCache(cache, "WEIGHT");
            zeroInaccuracyMapCache(cache, "INACCURACY");
            zeroInaccuracyMapCache(cache, "AIM_INACCURACY");
            maximizeRecoilControlCache(cache);
         }
      } catch (LinkageError | ReflectiveOperationException var8) {
      }
   }

   private static void handleChamberAttachmentProperty(Event event) {
      try {
         if (event.getClass().getMethod("getGunItem").invoke(event) instanceof ItemStack gun && ChamberTaczEnhancement.isChamberWeapon(gun)) {
            Object cache = event.getClass().getMethod("getCacheProperty").invoke(event);
            if (cache == null) {
               return;
            }

            multiplyFloatCache(cache, "HEADSHOT_MULTIPLIER", 5.0F);
            if (ChamberTaczEnhancement.ignoresArmor(gun)) {
               addFloatCache(cache, "ARMOR_IGNORE", 1.0F);
               multiplyIntegerCache(cache, "PIERCE", 2.0);
            }

            return;
         }
      } catch (LinkageError | ReflectiveOperationException var4) {
      }
   }

   private static void multiplyDamageCache(Object cache, double multiplier) throws ReflectiveOperationException {
      Object property = gunProperty("DAMAGE");
      if (cacheGet(cache, property) instanceof Iterable<?> pairs) {
         Class<?> pairClass = Class.forName("com.tacz.guns.resource.pojo.data.gun.ExtraDamage$DistanceDamagePair");
         Method getDistance = pairClass.getMethod("getDistance");
         Method getDamage = pairClass.getMethod("getDamage");
         LinkedList adjusted = new LinkedList();

         for (Object pair : pairs) {
            if (pair != null && pairClass.isAssignableFrom(pair.getClass())) {
               Object distanceValue = getDistance.invoke(pair);
               Object damageValue = getDamage.invoke(pair);
               if (distanceValue instanceof Number) {
                  Number distance = (Number)distanceValue;
                  if (damageValue instanceof Number damage) {
                     adjusted.add(
                        pairClass.getConstructor(float.class, float.class)
                           .newInstance(distance.floatValue(), GhrothTaczEnhancement.clampEnhancedDamage(damage.doubleValue() * multiplier))
                     );
                  }
               }
            }
         }

         if (!adjusted.isEmpty()) {
            cacheSet(cache, property, adjusted);
         }
      }
   }

   private static void multiplyFloatCache(Object cache, String propertyName, float multiplier) throws ReflectiveOperationException {
      Object property = gunProperty(propertyName);
      if (cacheGet(cache, property) instanceof Number number) {
         cacheSet(cache, property, number.floatValue() * multiplier);
      }
   }

   private static void boostPositiveDamageCache(Object cache, double multiplier) throws ReflectiveOperationException {
      Object property = gunProperty("DAMAGE");
      if (cacheGet(cache, property) instanceof Iterable<?> pairs) {
         Class<?> pairClass = Class.forName("com.tacz.guns.resource.pojo.data.gun.ExtraDamage$DistanceDamagePair");
         Method getDistance = pairClass.getMethod("getDistance");
         Method getDamage = pairClass.getMethod("getDamage");
         LinkedList adjusted = new LinkedList();

         for (Object pair : pairs) {
            if (pair != null && pairClass.isAssignableFrom(pair.getClass())) {
               Object distanceValue = getDistance.invoke(pair);
               Object damageValue = getDamage.invoke(pair);
               if (distanceValue instanceof Number) {
                  Number distance = (Number)distanceValue;
                  if (damageValue instanceof Number damage) {
                     adjusted.add(
                        pairClass.getConstructor(float.class, float.class)
                           .newInstance(
                              distance.floatValue(),
                              (float)positiveBoostValue(damage.doubleValue(), multiplier, 1.0, GhrothTaczEnhancement.ENHANCED_DAMAGE_LIMIT)
                           )
                     );
                  }
               }
            }
         }

         if (!adjusted.isEmpty()) {
            cacheSet(cache, property, adjusted);
         }
      }
   }

   private static void boostPositiveFloatCache(Object cache, String propertyName, double multiplier, float fallbackValue, float maxValue) throws ReflectiveOperationException {
      Object property = gunProperty(propertyName);
      if (cacheGet(cache, property) instanceof Number number) {
         cacheSet(cache, property, (float)positiveBoostValue(number.doubleValue(), multiplier, (double)fallbackValue, (double)maxValue));
      }
   }

   private static void boostPositiveIntegerCache(Object cache, String propertyName, double multiplier, int fallbackValue, int maxValue) throws ReflectiveOperationException {
      Object property = gunProperty(propertyName);
      if (cacheGet(cache, property) instanceof Number number) {
         double boosted = positiveBoostValue(number.doubleValue(), multiplier, (double)fallbackValue, (double)maxValue);
         cacheSet(cache, property, Math.max(fallbackValue, (int)Math.min((long)maxValue, Math.round(boosted))));
      }
   }

   private static double positiveBoostValue(double value, double multiplier, double fallbackValue, double maxValue) {
      double base = value > 0.0 && Double.isFinite(value) ? value : Math.max(fallbackValue, Math.abs(value));
      if (!Double.isFinite(base) || base <= 0.0) {
         base = fallbackValue;
      }

      double boosted = base * Math.max(1.0, multiplier);
      if (!Double.isFinite(boosted)) {
         boosted = maxValue;
      }

      return Math.max(0.0, Math.min(maxValue, boosted));
   }

   private static void addFloatCache(Object cache, String propertyName, float addend) throws ReflectiveOperationException {
      Object property = gunProperty(propertyName);
      if (cacheGet(cache, property) instanceof Number number) {
         cacheSet(cache, property, number.floatValue() + addend);
      }
   }

   private static void multiplyIntegerCache(Object cache, String propertyName, double multiplier) throws ReflectiveOperationException {
      Object property = gunProperty(propertyName);
      if (cacheGet(cache, property) instanceof Number number) {
         cacheSet(cache, property, Math.max(0, (int)Math.round(number.doubleValue() * multiplier)));
      }
   }

   private static void zeroFloatCache(Object cache, String propertyName) throws ReflectiveOperationException {
      cacheSet(cache, gunProperty(propertyName), 0.0F);
   }

   private static void zeroInaccuracyMapCache(Object cache, String propertyName) throws ReflectiveOperationException {
      Object property = gunProperty(propertyName);
      if (cacheGet(cache, property) instanceof Map<?, ?> map) {
         HashMap zeroed = new HashMap();

         for (Object key : map.keySet()) {
            zeroed.put(key, 0.0F);
         }

         cacheSet(cache, property, zeroed);
      }
   }

   private static void maximizeRecoilControlCache(Object cache) throws ReflectiveOperationException {
      try {
         Object property = gunProperty("RECOIL");
         Object current = cacheGet(cache, property);
         float pitchDefault = recoilDefaultValue(current, true);
         float yawDefault = recoilDefaultValue(current, false);
         List<Object> pitchModifiers = new ArrayList<>();
         List<Object> yawModifiers = new ArrayList<>();
         pitchModifiers.add(taczMultiplierModifier(0.0));
         yawModifiers.add(taczMultiplierModifier(0.0));
         Object pair = Class.forName("com.tacz.guns.api.modifier.ParameterizedCachePair")
            .getMethod("of", List.class, List.class, Object.class, Object.class)
            .invoke(null, pitchModifiers, yawModifiers, pitchDefault, yawDefault);
         cacheSet(cache, property, pair);
      } catch (LinkageError | ReflectiveOperationException var8) {
      }
   }

   private static float recoilDefaultValue(Object recoilPair, boolean pitch) throws ReflectiveOperationException {
      if (recoilPair == null) {
         return 0.0F;
      } else {
         Object parameterizedCache = recoilPair.getClass().getMethod(pitch ? "left" : "right").invoke(recoilPair);
         return parameterizedCache.getClass().getMethod("getDefaultValue").invoke(parameterizedCache) instanceof Number number ? number.floatValue() : 0.0F;
      }
   }

   private static Object taczMultiplierModifier(double multiplier) throws ReflectiveOperationException {
      Object modifier = Class.forName("com.tacz.guns.resource.pojo.data.attachment.Modifier").getConstructor().newInstance();
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
      // $VF: Couldn't be decompiled
      // Please report this to the Vineflower issue tracker, at https://github.com/Vineflower/vineflower/issues with a copy of the class file (if you have the rights to distribute it!)
      // java.lang.NullPointerException: Cannot invoke "org.jetbrains.java.decompiler.struct.gen.VarType.isGeneric()" because "newRet" is null
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.InvocationExprent.getInferredExprType(InvocationExprent.java:634)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.DoStatement.toJava(DoStatement.java:146)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:833)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.SequenceStatement.toJava(SequenceStatement.java:107)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:833)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.CatchStatement.toJava(CatchStatement.java:166)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:833)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.SequenceStatement.toJava(SequenceStatement.java:107)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.RootStatement.toJava(RootStatement.java:36)
      //   at org.jetbrains.java.decompiler.main.ClassWriter.writeMethod(ClassWriter.java:1283)
      //
      // Bytecode:
      // 00: aload 0
      // 01: invokevirtual java/lang/Object.getClass ()Ljava/lang/Class;
      // 04: ldc_w "getBullet"
      // 07: bipush 0
      // 08: anewarray 3094
      // 0b: invokevirtual java/lang/Class.getMethod (Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;
      // 0e: aload 0
      // 0f: bipush 0
      // 10: anewarray 4
      // 13: invokevirtual java/lang/reflect/Method.invoke (Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;
      // 16: astore 2
      // 17: aload 2
      // 18: instanceof net/minecraft/world/entity/Entity
      // 1b: ifeq 29
      // 1e: aload 2
      // 1f: checkcast net/minecraft/world/entity/Entity
      // 22: astore 4
      // 24: aload 4
      // 26: goto 2a
      // 29: aload 1
      // 2a: astore 3
      // 2b: aload 1
      // 2c: invokevirtual net/minecraft/server/level/ServerPlayer.serverLevel ()Lnet/minecraft/server/level/ServerLevel;
      // 2f: aload 3
      // 30: aload 1
      // 31: invokestatic com/rzy/dealt_force_skills/skill/SkillDamageHelper.trueDamage (Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;)Lnet/minecraft/world/damagesource/DamageSource;
      // 34: astore 4
      // 36: ldc_w "com.tacz.guns.api.event.common.GunDamageSourcePart"
      // 39: invokestatic java/lang/Class.forName (Ljava/lang/String;)Ljava/lang/Class;
      // 3c: astore 5
      // 3e: aload 0
      // 3f: invokevirtual java/lang/Object.getClass ()Ljava/lang/Class;
      // 42: ldc_w "setDamageSource"
      // 45: bipush 2
      // 46: anewarray 3094
      // 49: dup
      // 4a: bipush 0
      // 4b: aload 5
      // 4d: aastore
      // 4e: dup
      // 4f: bipush 1
      // 50: ldc net/minecraft/world/damagesource/DamageSource
      // 52: aastore
      // 53: invokevirtual java/lang/Class.getMethod (Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;
      // 56: astore 6
      // 58: aload 5
      // 5a: invokevirtual java/lang/Class.getEnumConstants ()[Ljava/lang/Object;
      // 5d: astore 7
      // 5f: aload 7
      // 61: arraylength
      // 62: istore 8
      // 64: bipush 0
      // 65: istore 9
      // 67: iload 9
      // 69: iload 8
      // 6b: if_icmpge 90
      // 6e: aload 7
      // 70: iload 9
      // 72: aaload
      // 73: astore 10
      // 75: aload 6
      // 77: aload 0
      // 78: bipush 2
      // 79: anewarray 4
      // 7c: dup
      // 7d: bipush 0
      // 7e: aload 10
      // 80: aastore
      // 81: dup
      // 82: bipush 1
      // 83: aload 4
      // 85: aastore
      // 86: invokevirtual java/lang/reflect/Method.invoke (Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;
      // 89: pop
      // 8a: iinc 9 1
      // 8d: goto 67
      // 90: goto 94
      // 93: astore 2
      // 94: return
   }

   private static void recordGhrothDamageFloor(LivingEntity target, ServerPlayer attacker, float amount, boolean starsBonus) {
      if (!(amount <= 0.0F) && Float.isFinite(amount)) {
         long expiresAt = target.level().getGameTime() + 2L;
         GHROTH_DAMAGE_FLOORS.put(target.getUUID(), new CommonEvents.GhrothDamageFloor(attacker.getUUID(), amount, expiresAt, starsBonus));
      }
   }

   private static void handleGhrothStarsGunFire(Event event) {
      if (taczEventEntity(event) instanceof ServerPlayer attacker && GhrothStateManager.isGhroth(attacker) && GhrothStateManager.isStarsActive(attacker)) {
         ItemStack gun = taczEventGunItem(event);
         if (!isTaczGunStack(gun)) {
            return;
         }

         Optional<LivingEntity> target = GhrothStateManager.findStarsAimTarget(attacker);
         if (target.isEmpty()) {
            return;
         }

         float damage = GhrothTaczEnhancement.estimatedShotDamage(attacker, gun);
         if (!(damage <= 0.0F) && Float.isFinite(damage)) {
            CommonEvents.GhrothStarsPendingShot shot = new CommonEvents.GhrothStarsPendingShot(
               target.get().getId(), damage, attacker.level().getGameTime() + 4L
            );
            GHROTH_STARS_PENDING_SHOTS.computeIfAbsent(attacker.getUUID(), ignored -> new LinkedList<>()).add(shot);
            attacker.serverLevel()
               .getServer()
               .tell(new TickTask(attacker.serverLevel().getServer().getTickCount() + 1, () -> resolveGhrothStarsPendingShot(attacker, shot)));
            return;
         }

         return;
      }
   }

   private static void consumeGhrothStarsPendingShot(ServerPlayer attacker) {
      LinkedList<CommonEvents.GhrothStarsPendingShot> shots = GHROTH_STARS_PENDING_SHOTS.get(attacker.getUUID());
      if (shots != null) {
         shots.poll();
         if (shots.isEmpty()) {
            GHROTH_STARS_PENDING_SHOTS.remove(attacker.getUUID());
         }
      }
   }

   private static void resolveGhrothStarsPendingShot(ServerPlayer attacker, CommonEvents.GhrothStarsPendingShot shot) {
      if (removeGhrothStarsPendingShot(attacker.getUUID(), shot)) {
         if (attacker.isAlive() && attacker.level().getGameTime() <= shot.expiresAt()) {
            if (attacker.level().getEntity(shot.targetId()) instanceof LivingEntity target) {
               GhrothStateManager.handleStarsGuaranteedShot(attacker, target, shot.damage());
            }
         }
      }
   }

   private static boolean removeGhrothStarsPendingShot(UUID attackerId, CommonEvents.GhrothStarsPendingShot shot) {
      LinkedList<CommonEvents.GhrothStarsPendingShot> shots = GHROTH_STARS_PENDING_SHOTS.get(attackerId);
      if (shots != null && shots.remove(shot)) {
         if (shots.isEmpty()) {
            GHROTH_STARS_PENDING_SHOTS.remove(attackerId);
         }

         return true;
      } else {
         return false;
      }
   }

   private static float reflectedFloat(Event event, String methodName, float fallback) throws ReflectiveOperationException {
      return event.getClass().getMethod(methodName).invoke(event) instanceof Number number ? number.floatValue() : fallback;
   }

   private static boolean reflectedBoolean(Event event, String methodName, boolean fallback) throws ReflectiveOperationException {
      return event.getClass().getMethod(methodName).invoke(event) instanceof Boolean bool ? bool : fallback;
   }

   private static boolean isEventCancelable(Event event) {
      try {
         event.getClass().getMethod("setCanceled", boolean.class);
         return true;
      } catch (NoSuchMethodException var2) {
         return false;
      }
   }

   private static boolean cancelEvent(Event event) {
      try {
         event.getClass().getMethod("setCanceled", boolean.class).invoke(event, true);
         return true;
      } catch (ReflectiveOperationException var2) {
         return false;
      }
   }

   private static void accelerateTaczAdrenalineTimers(Player player) {
      CommonEvents.TaczSpeedMultipliers multipliers = taczSpeedMultipliers(player);
      if (multipliers.hasChange()) {
         if (!player.level().isClientSide) {
            shiftTaczServerDataHolder(player, multipliers);
         }
      }
   }

   private static void accelerateTaczAdrenalineForEvent(String eventName, Event event) {
      if ("com.tacz.guns.api.event.common.GunShootEvent".equals(eventName) || "com.tacz.guns.api.event.common.GunFireEvent".equals(eventName)) {
         if (taczEventEntity(event) instanceof Player player) {
            CommonEvents.TaczSpeedMultipliers multipliers = taczSpeedMultipliers(player);
            if (multipliers.hasChange()) {
               if (player.level().isClientSide) {
                  shiftTaczClientDataHolder(player, multipliers);
               } else {
                  shiftTaczServerDataHolder(player, multipliers);
               }
            }
         }
      }
   }

   private static void snapshotMhsTaczAmmo(String eventName, Event event) {
      if ("com.tacz.guns.api.event.common.GunShootEvent".equals(eventName) || "com.tacz.guns.api.event.common.GunFireEvent".equals(eventName)) {
         if (taczEventEntity(event) instanceof ServerPlayer player && isMhsFuryActive(player)) {
            ItemStack gun = taczEventGunItem(event);
            if (isTaczGunStack(gun)) {
               rememberMhsTaczAmmo(player, gun, player.level().getGameTime() + 5L);
            }

            return;
         }
      }
   }

   private static CommonEvents.TaczSpeedMultipliers taczSpeedMultipliers(LivingEntity entity) {
      double adrenaline = ToxikStateManager.adrenalineSpeedMultiplier(entity);
      double adrenalineFireRate = ToxikStateManager.adrenalineFireRateSpeedMultiplier(entity);
      double equipment = equipmentTaczAssaultMultiplier(entity);
      double ghrothFireRate = GhrothTaczEnhancement.fireRateMultiplier(entity);
      double ghrothReload = GhrothTaczEnhancement.reloadMultiplier(entity);
      double ghrothAim = GhrothTaczEnhancement.aimMultiplier(entity);
      double ghrothBolt = GhrothTaczEnhancement.boltMultiplier(entity);
      return new CommonEvents.TaczSpeedMultipliers(
         adrenalineFireRate * equipment * ghrothFireRate * ModItemEffectHelper.medicineTaczFireRateMultiplier(entity),
         adrenaline * ghrothReload * ModItemEffectHelper.medicineTaczReloadMultiplier(entity),
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
      CommonEvents.TaczSpeedMultipliers multipliers = taczSpeedMultipliers(entity);
      return multipliers.aim() / multipliers.aimPenalty();
   }

   private static void shiftTaczServerDataHolder(LivingEntity entity, CommonEvents.TaczSpeedMultipliers multipliers) {
      try {
         Class<?> operatorClass = Class.forName("com.tacz.guns.api.entity.IGunOperator");
         Object operator = operatorClass.getMethod("fromLivingEntity", LivingEntity.class).invoke(null, entity);
         Object dataHolder = operatorClass.getMethod("getDataHolder").invoke(operator);
         long reloadExtraMillis = extraTaczMillis(multipliers.reload());
         long boltExtraMillis = extraTaczMillis(multipliers.bolt());
         shiftLongFieldIfNonNegative(dataHolder, "lockTimestamp", -Math.max(reloadExtraMillis, boltExtraMillis));

         if (booleanField(dataHolder, "isBolting")) {
            shiftLongFieldIfNonNegative(dataHolder, "boltTimestamp", -boltExtraMillis);
         }

         shiftLongFieldIfNonNegative(dataHolder, "drawTimestamp", -Math.max(0L, reloadExtraMillis / 2L));
      } catch (LinkageError | ReflectiveOperationException var15) {
      }
   }

   private static void shiftTaczClientDataHolder(Player player, CommonEvents.TaczSpeedMultipliers multipliers) {
      try {
         Class<?> clientOperatorClass = Class.forName("com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator");
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
         long reloadExtraMillis = extraTaczMillis(multipliers.reload());
         long boltExtraMillis = extraTaczMillis(multipliers.bolt());
         shiftLongFieldIfNonNegative(dataHolder, "lockTimestamp", -Math.max(reloadExtraMillis, boltExtraMillis));
         if (booleanField(dataHolder, "isBolting")) {
            shiftLongFieldIfNonNegative(dataHolder, "boltTimestamp", -boltExtraMillis);
         }

         if (booleanField(dataHolder, "isCharging")) {
            boostFloatField(
               dataHolder, "chargeProgress", adrenalineChargeProgressBoost(Math.max(multipliers.fireRate(), multipliers.aim())), 0.0F, Float.MAX_VALUE
            );
         }

      } catch (LinkageError | ReflectiveOperationException var16) {
      }
   }

   private static float adrenalineAimProgressBoost(double multiplier) {
      return (float)Math.min(0.5, Math.max(0.0, 0.08 * (multiplier - 1.0)));
   }

   private static float adrenalineChargeProgressBoost(double multiplier) {
      return (float)Math.max(0.0, 0.2 * (multiplier - 1.0));
   }

   private static long extraTaczMillis(double multiplier) {
      return multiplier <= 1.0001 ? 0L : Math.max(1L, Math.round(50.0 * (multiplier - 1.0)));
   }

   private static float taczAimPenaltyProgress(double multiplier) {
      return multiplier <= 1.0001 ? 0.0F : (float)Math.min(0.8, 0.08 * (multiplier - 1.0));
   }

   private static void shiftLongFieldIfNonNegative(Object target, String fieldName, long delta) throws ReflectiveOperationException {
      if (target != null) {
         Field field = fieldIfPresent(target, fieldName);
         if (field != null && field.getType() == long.class) {
            long value = field.getLong(target);
            if (value >= 0L) {
               field.setLong(target, value + delta);
            }
         }
      }
   }

   private static boolean booleanField(Object target, String fieldName) throws ReflectiveOperationException {
      if (target == null) {
         return false;
      } else {
         Field field = fieldIfPresent(target, fieldName);
         return field != null && field.getType() == boolean.class && field.getBoolean(target);
      }
   }

   private static void setBooleanFieldIfPresent(Object target, String fieldName, boolean value) throws ReflectiveOperationException {
      if (target != null) {
         Field field = fieldIfPresent(target, fieldName);
         if (field != null && field.getType() == boolean.class) {
            field.setBoolean(target, value);
         }
      }
   }

   private static void boostFloatField(Object target, String fieldName, float delta, float min, float max) throws ReflectiveOperationException {
      if (target != null && !(delta <= 0.0F)) {
         Field field = fieldIfPresent(target, fieldName);
         if (field != null && field.getType() == float.class) {
            float value = field.getFloat(target);
            if (Float.isFinite(value)) {
               float boosted = Math.max(min, Math.min(max, value + delta));
               field.setFloat(target, boosted);
            }
         }
      }
   }

   private static void reduceFloatField(Object target, String fieldName, float delta, float min, float max) throws ReflectiveOperationException {
      if (target != null && !(delta <= 0.0F)) {
         Field field = fieldIfPresent(target, fieldName);
         if (field != null && field.getType() == float.class) {
            float value = field.getFloat(target);
            if (Float.isFinite(value)) {
               field.setFloat(target, Math.max(min, Math.min(max, value - delta)));
            }
         }
      }
   }

   private static Field fieldIfPresent(Object target, String fieldName) {
      for (Class<?> type = target.getClass(); type != null; type = type.getSuperclass()) {
         try {
            Field field = type.getField(fieldName);
            field.setAccessible(true);
            return field;
         } catch (NoSuchFieldException var4) {
         }
      }

      return null;
   }

   private static boolean isTaczReloading(Object dataHolder) throws ReflectiveOperationException {
      if (dataHolder == null) {
         return false;
      } else {
         Field field = fieldIfPresent(dataHolder, "reloadStateType");
         if (field == null) {
            return false;
         } else {
            Object state = field.get(dataHolder);
            if (state == null) {
               return false;
            } else {
               try {
                  Method method = state.getClass().getMethod("isReloading");
                  if (method.invoke(state) instanceof Boolean b && b) {
                     return true;
                  }

                  return false;
               } catch (ReflectiveOperationException var6) {
                  return state.toString().contains("RELOAD");
               }
            }
         }
      }
   }

   private static boolean isTaczSonicTimeEvent(String eventName) {
      return "com.tacz.guns.api.event.common.GunShootEvent".equals(eventName)
         || "com.tacz.guns.api.event.common.GunFireEvent".equals(eventName)
         || "com.tacz.guns.api.event.common.GunMeleeEvent".equals(eventName)
         || "com.tacz.guns.api.event.common.GunFireSelectEvent".equals(eventName);
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
      if (taczEventEntity(event) instanceof ServerPlayer player) {
         MorseStateManager.recordPlayerAction(player);
      }
   }

   private static boolean cancelDownedTaczAction(Event event) {
      if (!isEventCancelable(event)) {
         return false;
      } else {
         LivingEntity entity = taczEventEntity(event);
         if (entity instanceof Player player
            && (
               (com.rzy.dealt_force_skills.character.rover.RoverStateManager.isDowned(player) || StingerStateManager.isDowned(player))
                  || VlinderStateManager.isDowned(player)
                  || CatDadStateManager.isDowned(player)
                  || NikaidouHiroStateManager.isWeaponActive(player)
                  || NikaidouHiroWitchificationStateManager.isWeaponActive(player)
                  || NikaidouHiroWitchificationStateManager.isActionLocked(player)
                  || DepartmentOfTransportationStateManager.hasEquippedTool(player)
                  || TempestStateManager.isActionLocked(player)
            )) {
            cancelEvent(event);
            cancelOptionalTaczReload(entity);
            return true;
         }

         return false;
      }
   }

   private static boolean cancelFrozenTaczAction(Event event) {
      if (isEventCancelable(event) && !"com.tacz.guns.api.event.common.GunFireSelectEvent".equals(event.getClass().getName())) {
         LivingEntity entity = taczEventEntity(event);
         if (entity instanceof Player player && RoundStartFreezeManager.isFrozen(player)) {
            cancelEvent(event);
            cancelOptionalTaczReload(entity);
            return true;
         }

         return false;
      } else {
         return false;
      }
   }

   private static boolean cancelManbaDuelTaczAction(Event event) {
      if (!isEventCancelable(event)) {
         return false;
      } else {
         if (taczEventEntity(event) instanceof ServerPlayer player && ManbaStateManager.isManba(player) && ManbaStateManager.isDuelActive(player)) {
            cancelEvent(event);
            if ("com.tacz.guns.api.event.common.GunReloadEvent".equals(event.getClass().getName())) {
               cancelOptionalTaczReload(player);
            }

            player.displayClientMessage(Component.translatable("message.dealt_force_skills.manba.tacz_forbidden_duel"), true);
            return true;
         }

         return false;
      }
   }

   private static boolean cancelLexNinjiaTaczAction(Event event) {
      if (!isEventCancelable(event)) {
         return false;
      } else {
         if (taczEventEntity(event) instanceof ServerPlayer player && LexNinjiaStateManager.isLexNinjia(player)) {
            cancelEvent(event);
            if ("com.tacz.guns.api.event.common.GunReloadEvent".equals(event.getClass().getName())) {
               cancelOptionalTaczReload(player);
            }

            player.displayClientMessage(Component.translatable("message.dealt_force_skills.lex_ninjia.tacz_forbidden"), true);
            return true;
         }

         return false;
      }
   }

   private static boolean cancelUndeadTaczAction(Event event) {
      if (!isEventCancelable(event)) {
         return false;
      } else {
         if (taczEventEntity(event) instanceof ServerPlayer player && UndeadStateManager.isUndead(player)) {
            cancelEvent(event);
            if ("com.tacz.guns.api.event.common.GunReloadEvent".equals(event.getClass().getName())) {
               cancelOptionalTaczReload(player);
            }

            player.displayClientMessage(Component.translatable("message.dealt_force_skills.undead.tacz_forbidden"), true);
            return true;
         }

         return false;
      }
   }

   private static boolean throttleSonicTaczTimeEvent(Event event) {
      if (!isEventCancelable(event)) {
         return false;
      } else {
         LivingEntity entity = taczEventEntity(event);
         if (entity != null && effectApplies(entity, ModEffects.SONIC_SHOCK) && shouldSonicShockSkipAction(entity)) {
            cancelEvent(event);
            if ("com.tacz.guns.api.event.common.GunReloadEvent".equals(event.getClass().getName())) {
               cancelOptionalTaczReload(entity);
            }

            return true;
         } else {
            return false;
         }
      }
   }

   private static boolean throttleMorseShockTaczTimeEvent(Event event) {
      if (!isEventCancelable(event)) {
         return false;
      } else {
         LivingEntity entity = taczEventEntity(event);
         if (entity != null && effectApplies(entity, ModEffects.MORSE_STRONG_SHOCK) && shouldMorseShockSkipAction(entity)) {
            cancelEvent(event);
            if ("com.tacz.guns.api.event.common.GunReloadEvent".equals(event.getClass().getName())) {
               cancelOptionalTaczReload(entity);
            }

            return true;
         } else {
            return false;
         }
      }
   }

   private static LivingEntity taczEventEntity(Event event) {
      for (String methodName : new String[]{"getEntity", "getShooter", "getAttacker"}) {
         try {
            Object entityObject = event.getClass().getMethod(methodName).invoke(event);
            if (entityObject instanceof LivingEntity) {
               return (LivingEntity)entityObject;
            }
         } catch (ReflectiveOperationException var7) {
         }
      }

      return null;
   }

   private static ItemStack taczEventGunItem(Event event) {
      try {
         Object stackObject = event.getClass().getMethod("getGunItemStack").invoke(event);
         if (stackObject instanceof ItemStack) {
            return (ItemStack)stackObject;
         }
      } catch (ReflectiveOperationException var3) {
      }

      return ItemStack.EMPTY;
   }

   private static void cancelTaczReloadEvent(Event event) {
      try {
         if (event.getClass().getMethod("getEntity").invoke(event) instanceof LivingEntity entity
            && (
               effectApplies(entity, ModEffects.WEBBED)
                  || effectApplies(entity, ModEffects.TEMPEST_DISARMED)
                  || ChamberTaczEnhancement.isChamberWeapon(entity.getMainHandItem())
            )) {
            cancelEvent(event);
         }
      } catch (ReflectiveOperationException var3) {
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
      int lockedSlot = TEMPEST_ACTION_LOCKED_SELECTED_SLOTS.computeIfAbsent(player.getUUID(), id -> player.getInventory().selected);
      if (player.getInventory().selected != lockedSlot) {
         player.getInventory().selected = lockedSlot;
         player.connection.send(new ClientboundSetCarriedItemPacket(lockedSlot));
      }
   }

   private static void cooldownTempestDisarmedHands(ServerPlayer player) {
      if (effectApplies(player, ModEffects.TEMPEST_DISARMED)) {
         cooldownHeldItem(player, player.getMainHandItem());
         cooldownHeldItem(player, player.getOffhandItem());
      }
   }

   private static void cooldownHeldItem(Player player, ItemStack stack) {
      if (!stack.isEmpty()) {
         player.getCooldowns().addCooldown(stack.getItem(), 3);
      }
   }

   private static void lockWebbedLook(ServerPlayer player) {
      CommonEvents.LockedLook locked = WEBBED_LOOK_LOCKS.computeIfAbsent(
         player.getUUID(), id -> new CommonEvents.LockedLook(player.getYRot(), player.getXRot())
      );
      player.setYRot(locked.yaw());
      player.setXRot(locked.pitch());
      player.setYHeadRot(locked.yaw());
      player.setYBodyRot(locked.yaw());
   }

   private static void cancelOptionalTaczReload(LivingEntity entity) {
      if (ensureTaczReloadBridge()) {
         try {
            Object operator = taczFromLivingEntity.invoke(null, entity);
            taczCancelReload.invoke(operator);
         } catch (RuntimeException | ReflectiveOperationException var2) {
         }
      }
   }

   private static boolean ensureTaczReloadBridge() {
      if (taczReloadBridgeChecked) {
         return taczReloadBridgeAvailable;
      } else {
         taczReloadBridgeChecked = true;

         try {
            Class<?> operatorClass = Class.forName("com.tacz.guns.api.entity.IGunOperator");
            taczFromLivingEntity = operatorClass.getMethod("fromLivingEntity", LivingEntity.class);
            taczCancelReload = operatorClass.getMethod("cancelReload");
            taczReloadBridgeAvailable = true;
         } catch (LinkageError | ReflectiveOperationException var1) {
            taczReloadBridgeAvailable = false;
         }

         return taczReloadBridgeAvailable;
      }
   }

   private static boolean tryAbsorbFrontShield(Player player, DamageSource source, Vec3 sourcePos, float amount) {
      if (!SinevaStateManager.isShieldDeployed(player)) {
         return false;
      } else {
         Vec3 forward = player.getLookAngle();
         Vec3 toSource = sourcePos.subtract(player.position().add(0.0, (double)player.getBbHeight() * 0.5, 0.0));
         boolean sourceInFront = toSource.lengthSqr() < 1.0E-4 || forward.dot(toSource.normalize()) > 0.0;
         if (!sourceInFront) {
            return false;
         } else {
            boolean viewportHit = isSinevaViewportHit(player, source, sourcePos);
            if (viewportHit && SinevaStateManager.viewportHealth(player) <= 0) {
               return false;
            } else {
               boolean shieldBroken = SinevaStateManager.damageShield(player, amount);
               boolean broken = viewportHit && SinevaStateManager.damageViewport(player, amount);
               if (player instanceof ServerPlayer serverPlayer) {
                  DfsAchievements.recordSinevaShieldBlock(serverPlayer, amount, viewportHit, broken);
               }

               if (!player.level().isClientSide) {
                  if (viewportHit) {
                     if (broken) {
                        player.level()
                           .playSound(
                              null,
                              player.getEyePosition().x,
                              player.getEyePosition().y,
                              player.getEyePosition().z,
                              (SoundEvent)ModSounds.SINEVA_VIEWPORT_BREAK.get(),
                              SoundSource.PLAYERS,
                              0.95F,
                              1.0F
                           );
                        player.level()
                           .playSound(null, player.blockPosition(), (SoundEvent)ModSounds.SINEVA_VIEWPORT_BREAK_VOICE.get(), SoundSource.VOICE, 1.0F, 1.0F);
                     } else {
                        SoundEvent glassSound = Blocks.GLASS.defaultBlockState().getSoundType().getHitSound();
                        player.level()
                           .playSound(
                              null,
                              player.getEyePosition().x,
                              player.getEyePosition().y,
                              player.getEyePosition().z,
                              glassSound,
                              SoundSource.PLAYERS,
                              0.6F,
                              1.4F
                           );
                     }

                     if (source.getEntity() instanceof ServerPlayer attacker && player instanceof ServerPlayer sp) {
                        NetworkHandler.sendToPlayer(new S2C_CharacterHitFeedback(sp.getId(), amount), attacker);
                     }
                  } else {
                     player.level().playSound(null, player.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.9F, 1.0F);
                  }

                  if (shieldBroken) {
                     player.level()
                        .playSound(
                           null,
                           player.getEyePosition().x,
                           player.getEyePosition().y,
                           player.getEyePosition().z,
                           (SoundEvent)ModSounds.SINEVA_VIEWPORT_BREAK.get(),
                           SoundSource.PLAYERS,
                           0.95F,
                           0.82F
                        );
                     player.level()
                        .playSound(null, player.blockPosition(), (SoundEvent)ModSounds.SINEVA_VIEWPORT_BREAK_VOICE.get(), SoundSource.VOICE, 1.0F, 1.0F);
                  }
               }

               if (player instanceof ServerPlayer serverPlayer && (broken || shieldBroken)) {
                  serverPlayer.displayClientMessage(
                     Component.translatable(
                        shieldBroken ? "message.dealt_force_skills.sineva.shield_broken" : "message.dealt_force_skills.sineva.viewport_broken"
                     ),
                     true
                  );
                  SinevaStateManager.syncToClient(serverPlayer);
                  SinevaStateManager.syncRenderStateToClients(serverPlayer);
                  if (broken && source.getEntity() instanceof ServerPlayer attacker) {
                     attacker.displayClientMessage(
                        Component.translatable("message.dealt_force_skills.sineva.viewport_broken_by_you", new Object[]{serverPlayer.getName()}), true
                     );
                  }
               }

               return true;
            }
         }
      }
   }

   private static boolean isSinevaViewportHit(Player player, DamageSource source, Vec3 sourcePos) {
      Vec3 forward = player.getLookAngle();
      if (forward.lengthSqr() < 1.0E-4) {
         return false;
      } else {
         forward = forward.normalize();
         Vec3 worldUp = new Vec3(0.0, 1.0, 0.0);
         Vec3 right = forward.cross(worldUp);
         if (right.lengthSqr() < 1.0E-4) {
            right = new Vec3(1.0, 0.0, 0.0);
         } else {
            right = right.normalize();
         }

         Vec3 up = right.cross(forward).normalize();
         Vec3 eye = player.getEyePosition().add(up.scale(SinevaShieldGeometry.VIEWPORT_CENTER_EYE_OFFSET));
         Vec3 viewportCenter = eye.add(forward.scale(SinevaShieldGeometry.VIEWPORT_FORWARD_OFFSET));
         if (!isInViewportRange(viewportCenter, forward, sourcePos)) {
            return false;
         } else {
            Entity direct = source.getDirectEntity();
            if (direct != null && direct != player) {
               Vec3 start = direct.position();
               Vec3 motion = direct.getDeltaMovement();
               if (motion.lengthSqr() > 1.0E-4) {
                  Vec3 end = start.add(motion);
                  if (lineHitsViewport(start, end, viewportCenter, forward, right, up)
                     || lineHitsViewport(start.subtract(motion), end, viewportCenter, forward, right, up)) {
                     return true;
                  }
               }

               Entity attacker = source.getEntity();
               return attacker != null && attacker != direct
                  ? lineHitsViewport(entityAimOrigin(attacker), direct.position(), viewportCenter, forward, right, up)
                  : lineHitsViewport(sourcePos, playerBodyCenter(player), viewportCenter, forward, right, up);
            } else {
               Entity attacker = source.getEntity();
               Vec3 start = attacker != null ? entityAimOrigin(attacker) : sourcePos;
               return lineHitsViewport(start, playerBodyCenter(player), viewportCenter, forward, right, up);
            }
         }
      }
   }

   private static boolean isInViewportRange(Vec3 viewportCenter, Vec3 forward, Vec3 sourcePos) {
      double distance = sourcePos.subtract(viewportCenter).dot(forward);
      return distance >= -0.25 && distance <= SinevaShieldGeometry.VIEWPORT_RAY_LENGTH;
   }

   private static boolean lineHitsViewport(Vec3 start, Vec3 end, Vec3 viewportCenter, Vec3 forward, Vec3 right, Vec3 up) {
      Vec3 line = end.subtract(start);
      double denom = line.dot(forward);
      if (Math.abs(denom) < 1.0E-4) {
         return false;
      } else {
         double t = viewportCenter.subtract(start).dot(forward) / denom;
         if (!(t < -0.05) && !(t > 1.05)) {
            Vec3 hit = start.add(line.scale(t));
            Vec3 local = hit.subtract(viewportCenter);
            return Math.abs(local.dot(right)) <= SinevaShieldGeometry.VIEWPORT_WORLD_WIDTH * 0.5
               && Math.abs(local.dot(up)) <= SinevaShieldGeometry.VIEWPORT_WORLD_HEIGHT * 0.5;
         } else {
            return false;
         }
      }
   }

   private static Vec3 entityAimOrigin(Entity entity) {
      return entity instanceof LivingEntity living ? living.getEyePosition() : entity.position().add(0.0, (double)entity.getBbHeight() * 0.5, 0.0);
   }

   private static Vec3 playerBodyCenter(Player player) {
      return player.position().add(0.0, (double)player.getBbHeight() * 0.5, 0.0);
   }

   private static boolean isSinevaViewportHitLegacy(Player player, Vec3 sourcePos) {
      double eyeY = player.getEyeY();
      if (Math.abs(sourcePos.y - eyeY) > 0.3) {
         return false;
      } else {
         Vec3 eye = player.getEyePosition();
         Vec3 toSource = sourcePos.subtract(eye);
         double dist = toSource.length();
         if (dist < 0.01) {
            return true;
         } else {
            Vec3 toSourceDir = toSource.scale(1.0 / dist);
            Vec3 look = player.getLookAngle();
            return look.dot(toSourceDir) > 0.97;
         }
      }
   }

   private static float equipmentOutgoingDamageMultiplier(ServerPlayer attacker, LivingEntity target, DamageSource source) {
      if (!ModGameRules.areArmorSpecialsEnabled(attacker)) {
         return 1.0F;
      } else {
         double multiplier = 1.0;
         ItemStack chest = attacker.getItemBySlot(EquipmentSlot.CHEST);
         ItemStack head = attacker.getItemBySlot(EquipmentSlot.HEAD);
         DfsEquipmentItem.Profile chestProfile = DfsEquipmentItem.profile(chest);
         DfsEquipmentItem.Profile headProfile = DfsEquipmentItem.profile(head);
         if (chestProfile != null
            && chestProfile.ability() == DfsEquipmentItem.SpecialAbility.LONELY_DAMAGE
            && attacker.level()
               .getEntitiesOfClass(Player.class, attacker.getBoundingBox().inflate(5.0), other -> other != attacker && other.isAlive())
               .isEmpty()) {
            multiplier *= 1.75;
            notifyEquipmentAbilityTrigger(attacker, chestProfile, ParticleTypes.CRIT);
         }

         if (chestProfile != null && chestProfile.ability() == DfsEquipmentItem.SpecialAbility.RED_OWL_REVENGE) {
            String revengeType = attacker.getPersistentData().getString("dealt_force_skills.red_owl_revenge_type");
            if (!revengeType.isBlank() && revengeType.equals(target.getType().getDescriptionId())) {
               multiplier *= 1.75;
               notifyEquipmentAbilityTrigger(attacker, chestProfile, ParticleTypes.SOUL_FIRE_FLAME);
            }
         }

         if (chestProfile != null && chestProfile.ability() == DfsEquipmentItem.SpecialAbility.STATIONARY_STACKS) {
            multiplier *= 1.0
               + (double)((CustomData)chest.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY))
                     .copyTag()
                     .getInt("dealt_force_skills.fs_stationary_stacks")
                  * 0.04;
         }

         if (headProfile != null && headProfile.ability() == DfsEquipmentItem.SpecialAbility.STATIONARY_STACKS) {
            multiplier *= 1.0
               + (double)((CustomData)head.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY))
                     .copyTag()
                     .getInt("dealt_force_skills.fs_stationary_stacks")
                  * 0.04;
         }

         if (headProfile != null && headProfile.ability() == DfsEquipmentItem.SpecialAbility.GN_HEAVY) {
            multiplier *= 1.1;
         }

         if (headProfile != null && headProfile.ability() == DfsEquipmentItem.SpecialAbility.GN_HEAVY_NIGHT_VISION) {
            multiplier *= 1.2;
         }

         if (headProfile != null && headProfile.ability() == DfsEquipmentItem.SpecialAbility.DICH9_ASSAULT) {
            addTimedEquipmentStacks(head, "dealt_force_skills.dich9_stacks", "dealt_force_skills.dich9_until", attacker.level().getGameTime(), 20, 140L);
            notifyEquipmentAbilityTrigger(attacker, headProfile, ParticleTypes.ELECTRIC_SPARK);
         }

         if (headProfile != null
            && headProfile.ability() == DfsEquipmentItem.SpecialAbility.ELBOW_SPIRIT
            && chestProfile != null
            && chestProfile.id().equals("samurai_ballistic_vest")
            && isMeleeDamage(source, attacker)) {
            multiplier *= 12.45;
            notifyEquipmentAbilityTrigger(attacker, headProfile, ParticleTypes.SWEEP_ATTACK);
         }

         return (float)multiplier;
      }
   }

   private static float handleEquipmentDefense(ServerPlayer player, DamageSource source, float amount) {
      if (isEquipmentBypassDamage(source)) {
         return amount;
      } else {
         ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
         ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
         DfsEquipmentItem.Profile chestProfile = DfsEquipmentItem.profile(chest);
         DfsEquipmentItem.Profile headProfile = DfsEquipmentItem.profile(head);
         if (chestProfile == null && headProfile == null) {
            return applyUnprotectedLimbDamageReduction(player, source, amount, false, false, false);
         } else if (tryRedOwlMaskDodge(player, source, headProfile)) {
            return 0.0F;
         } else {
            if (isMhsFuryActive(player)) {
               amount *= 0.05F;
               if (amount <= 0.0F) {
                  return 0.0F;
               }
            }

            boolean chestCovered = chestProfile != null && isEquipmentCoveredHit(player, source, chestProfile);
            boolean headCovered = headProfile != null && isEquipmentCoveredHit(player, source, headProfile);
            amount = applyUnprotectedLimbDamageReduction(player, source, amount, chestCovered, headCovered, true);
            if (!chestCovered && !headCovered) {
               return amount;
            } else if (tryEquipmentHurtTriggers(player, source, amount, chest, chestProfile, chestCovered, head, headProfile, headCovered)) {
               return 0.0F;
            } else {
               float adjusted = applyEquipmentPassiveReductions(player, source, amount, chestProfile, chestCovered, headProfile, headCovered);
               if (adjusted <= 0.0F) {
                  return 0.0F;
               } else {
                  if (headCovered) {
                     adjusted = absorbWithEquipment(player, head, headProfile, adjusted);
                  }

                  if (chestCovered) {
                     adjusted = absorbWithEquipment(player, chest, chestProfile, adjusted);
                  }

                  if (adjusted <= 0.0F) {
                     return 0.0F;
                  } else {
                     long now = player.level().getGameTime();
                     if (adjusted >= player.getHealth()) {
                        if (ModGameRules.areArmorSpecialsEnabled(player)
                           && chestCovered
                           && chestProfile.ability() == DfsEquipmentItem.SpecialAbility.FATAL_GUARD
                           && now >= player.getPersistentData().getLong("dealt_force_skills.equipment_fatal_guard_until")) {
                           player.getPersistentData().putLong("dealt_force_skills.equipment_fatal_guard_until", now + 12000L);
                           player.invulnerableTime = Math.max(player.invulnerableTime, 10);
                           notifyEquipmentAbilityTrigger(player, chestProfile, ParticleTypes.TOTEM_OF_UNDYING);
                           return 0.0F;
                        }

                        if ((chestCovered || headCovered)
                           && equipmentFactionCount(player, DfsEquipmentItem.Faction.ASARA) >= 2
                           && now >= player.getPersistentData().getLong("dealt_force_skills.asara_set_fatal_guard_until")) {
                           player.getPersistentData().putLong("dealt_force_skills.asara_set_fatal_guard_until", now + 7200L);
                           player.setHealth(player.getMaxHealth());
                           notifyEquipmentSetTrigger(player, DfsEquipmentItem.Faction.ASARA, ParticleTypes.HEART);
                           return 0.0F;
                        }
                     }

                     return adjusted;
                  }
               }
            }
         }
      }
   }

   private static float applyUnprotectedLimbDamageReduction(
      ServerPlayer player, DamageSource source, float amount, boolean chestCovered, boolean headCovered, boolean hasEquipmentProfile
   ) {
      if (!(amount <= 0.0F) && !chestCovered && !headCovered && ModGameRules.areLimbsDamageReductionsEnabled(player)) {
         Double ratio = estimateEquipmentHitHeightRatio(player, source);
         if (ratio == null) {
            return amount;
         } else if (!hasEquipmentProfile && ratio >= 0.36 && ratio < 0.8) {
            return amount;
         } else {
            float reduction = Mth.clamp(LIMBS_UNPROTECTED_DAMAGE_REDUCTION, 0.0F, 1.0F);
            return amount * (1.0F - reduction);
         }
      } else {
         return amount;
      }
   }

   private static float handleSaeedGuardInheritedDefense(SaeedGuardEntity guard, DamageSource source, float amount) {
      Optional<ServerPlayer> owner = guard.owner();
      if (!owner.isEmpty() && !isEquipmentBypassDamage(source)) {
         ServerPlayer player = owner.get();
         ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
         ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
         DfsEquipmentItem.Profile chestProfile = DfsEquipmentItem.profile(chest);
         DfsEquipmentItem.Profile headProfile = DfsEquipmentItem.profile(head);
         if (chestProfile == null && headProfile == null) {
            return amount;
         } else if (tryRedOwlMaskDodge(player, source, headProfile)) {
            return 0.0F;
         } else {
            if (isMhsFuryActive(player)) {
               amount *= 0.05F;
               if (amount <= 0.0F) {
                  return 0.0F;
               }
            }

            boolean chestCovered = chestProfile != null && isEquipmentCoveredHit(guard, source, chestProfile);
            boolean headCovered = headProfile != null && isEquipmentCoveredHit(guard, source, headProfile);
            if (!chestCovered && !headCovered) {
               return amount;
            } else {
               float adjusted = applyEquipmentPassiveReductions(player, source, amount, chestProfile, chestCovered, headProfile, headCovered);
               if (adjusted <= 0.0F) {
                  return 0.0F;
               } else {
                  if (headCovered) {
                     adjusted = absorbWithEquipment(player, head, headProfile, adjusted);
                  }

                  if (chestCovered) {
                     adjusted = absorbWithEquipment(player, chest, chestProfile, adjusted);
                  }

                  return Math.max(0.0F, adjusted);
               }
            }
         }
      } else {
         return amount;
      }
   }

   private static boolean tryEquipmentHurtTriggers(
      ServerPlayer player,
      DamageSource source,
      float amount,
      ItemStack chest,
      DfsEquipmentItem.Profile chestProfile,
      boolean chestCovered,
      ItemStack head,
      DfsEquipmentItem.Profile headProfile,
      boolean headCovered
   ) {
      if (!ModGameRules.areArmorSpecialsEnabled(player)) {
         return false;
      } else {
         LivingEntity livingAttacker = source.getEntity() instanceof LivingEntity living ? living : null;
         Vec3 sourcePos = damageSourcePosition(source);
         if (chestCovered
            && chestProfile.ability() == DfsEquipmentItem.SpecialAbility.FRONT_IMMUNE
            && sourcePos != null
            && Util.isFromFront(player, sourcePos, 70.0F)
            && player.getRandom().nextFloat() < 0.25F) {
            notifyEquipmentAbilityTrigger(player, chestProfile, ParticleTypes.CRIT);
            return true;
         } else {
            if (chestCovered && chestProfile.ability() == DfsEquipmentItem.SpecialAbility.RANDOM_F4) {
               int roll = player.getRandom().nextInt(4);
               notifyEquipmentAbilityTrigger(player, chestProfile, ParticleTypes.ENCHANT);
               if (roll == 0) {
                  return true;
               }

               if (roll == 1 && livingAttacker != null) {
                  livingAttacker.igniteForSeconds(5.0F);
               }

               if (roll == 2 && livingAttacker != null) {
                  livingAttacker.hurt(player.damageSources().playerAttack(player), amount);
               }

               if (roll == 3) {
                  player.addEffect(new MobEffectInstance(ModEffects.STUN, 10, 0, false, true, true));
                  return true;
               }
            }

            if (headCovered
               && headProfile.ability() == DfsEquipmentItem.SpecialAbility.H09_RIOT_DODGE
               && chestProfile != null
               && chestProfile.id().equals("elite_ballistic_vest")
               && !((CustomData)head.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY))
                  .copyTag()
                  .getBoolean("dealt_force_skills.equipment_stationary")
               && player.getRandom().nextFloat() < 0.75F) {
               notifyEquipmentAbilityTrigger(player, headProfile, ParticleTypes.CLOUD);
               return true;
            } else if (chestCovered
               && chestProfile.ability() == DfsEquipmentItem.SpecialAbility.HEAVY_DODGE_SUMMON
               && livingAttacker != null
               && livingAttacker != player
               && player.getRandom().nextFloat() < 0.5F) {
               CompoundTag tag = ((CustomData)chest.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)).copyTag();
               long now = player.level().getGameTime();
               if (now >= tag.getLong(EQUIPMENT_HEAVY_DODGE_SUMMON_UNTIL)) {
                  tag.putLong(EQUIPMENT_HEAVY_DODGE_SUMMON_UNTIL, now + 100L);
                  chest.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                  spawnEquipmentSupport(player, livingAttacker, 2, true, true);
                  notifyEquipmentAbilityTrigger(player, chestProfile, ParticleTypes.POOF);
               }
               return true;
            } else {
               if (chestCovered
                  && chestProfile.ability() == DfsEquipmentItem.SpecialAbility.KING_KONG_EXECUTION
                  && livingAttacker != null
                  && livingAttacker != player
                  && livingAttacker.distanceToSqr(player) <= 9.0) {
                  tryStartKingKongExecution(player, chest, livingAttacker);
               }

               if (chestCovered && chestProfile.ability() == DfsEquipmentItem.SpecialAbility.SUMMON_REINFORCEMENTS) {
                  CompoundTag tag = ((CustomData)chest.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)).copyTag();
                  long now = player.level().getGameTime();
                  if (now >= tag.getLong("dealt_force_skills.equipment_summon_until")) {
                     tag.putLong("dealt_force_skills.equipment_summon_until", now + 400L);
                     spawnEquipmentSupport(player, livingAttacker, 1 + player.getRandom().nextInt(4), false);
                     notifyEquipmentAbilityTrigger(player, chestProfile, ParticleTypes.HAPPY_VILLAGER);
                  }

                  chest.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
               }

               if (chestCovered && chestProfile.ability() == DfsEquipmentItem.SpecialAbility.TRICK_ASSAULT) {
                  addTimedEquipmentStacks(chest, "dealt_force_skills.trick_stacks", "dealt_force_skills.trick_until", player.level().getGameTime(), 20, 140L);
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
         }
      }
   }

   private static boolean tryRedOwlMaskDodge(ServerPlayer player, DamageSource source, DfsEquipmentItem.Profile headProfile) {
      if (ModGameRules.areArmorSpecialsEnabled(player)
         && headProfile != null
         && headProfile.ability() == DfsEquipmentItem.SpecialAbility.RED_OWL_MASK
         && !(player.getRandom().nextFloat() >= 0.5F)) {
         if (source.getEntity() instanceof LivingEntity livingAttacker && livingAttacker != player && isTaczGunStack(player.getMainHandItem())) {
            lookAtAim(player, livingAttacker);
         }

         notifyEquipmentAbilityTrigger(player, headProfile, ParticleTypes.CLOUD);

         return true;
      } else {
         return false;
      }
   }

   private static void triggerCounterKick(ServerPlayer player, DamageSource source, float amount, DfsEquipmentItem.Profile profile) {
      if (ModGameRules.areArmorSpecialsEnabled(player)
         && profile != null
         && profile.ability() == DfsEquipmentItem.SpecialAbility.COUNTER_KICK
         && isMeleeDamage(source, source.getEntity())) {
         if (source.getEntity() instanceof LivingEntity target && !(target.distanceToSqr(player) > 4.0) && !(player.getRandom().nextFloat() >= 0.1F)) {
            double attackDamage = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
            target.hurt(player.damageSources().playerAttack(player), (float)Math.max((double)amount, attackDamage * 3.0));
            notifyEquipmentAbilityTrigger(player, profile, ParticleTypes.SWEEP_ATTACK);
            return;
         }
      }
   }

   private static float applyEquipmentPassiveReductions(
      ServerPlayer player,
      DamageSource source,
      float amount,
      DfsEquipmentItem.Profile chestProfile,
      boolean chestCovered,
      DfsEquipmentItem.Profile headProfile,
      boolean headCovered
   ) {
      double multiplier = 1.0;
      if (chestCovered) {
         multiplier *= 1.0 - Math.min(0.95, chestProfile.kineticAbsorption());
         if (isMeleeDamage(source, source.getEntity())) {
            multiplier *= 1.0 - Math.min(0.95, chestProfile.bluntResistance());
         }
      }

      if (headCovered) {
         multiplier *= 1.0 - Math.min(0.95, headProfile.kineticAbsorption());
         if (isMeleeDamage(source, source.getEntity())) {
            multiplier *= 1.0 - Math.min(0.95, headProfile.bluntResistance());
         }
      }

      if (ModGameRules.areArmorSpecialsEnabled(player) && headCovered && headProfile.ability() == DfsEquipmentItem.SpecialAbility.GN_HEAVY) {
         multiplier *= 0.95;
      }

      if (ModGameRules.areArmorSpecialsEnabled(player) && headCovered && headProfile.ability() == DfsEquipmentItem.SpecialAbility.GN_HEAVY_NIGHT_VISION) {
         multiplier *= 0.9;
      }

      if (ModGameRules.areArmorSpecialsEnabled(player)
         && headCovered
         && headProfile.ability() == DfsEquipmentItem.SpecialAbility.GT5_STATIONARY_REDUCTION
         && ((CustomData)player.getItemBySlot(EquipmentSlot.HEAD).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY))
            .copyTag()
            .getBoolean("dealt_force_skills.equipment_stationary")) {
         multiplier *= 0.2;
         notifyEquipmentAbilityTrigger(player, headProfile, ParticleTypes.END_ROD);
      }

      return (float)((double)amount * Math.max(0.0, multiplier));
   }

   private static float absorbWithEquipment(ServerPlayer player, ItemStack stack, DfsEquipmentItem.Profile profile, float amount) {
      if (!(amount <= 0.0F) && !stack.isEmpty()) {
         boolean noDurabilityLoss = ModGameRules.areArmorSpecialsEnabled(player) && profile.ability() == DfsEquipmentItem.SpecialAbility.NO_DURABILITY_LOSS
            || stack.has(DataComponents.UNBREAKABLE);
         if (!noDurabilityLoss && !stack.isDamageableItem()) {
            return amount;
         } else {
            boolean broken = DfsEquipmentItem.isBroken(stack);
            boolean gtiOverload = canGtiOverloadEquipment(player, stack);
            if (!noDurabilityLoss && broken && !gtiOverload) {
               return amount;
            } else {
               double ratio = DfsEquipmentItem.qualityProtectionRatio(profile.quality());
               if (equipmentFactionCount(player, DfsEquipmentItem.Faction.HVK) >= 1) {
                  ratio += 0.04;
               }

               if (ModGameRules.areArmorSpecialsEnabled(player) && profile.ability() == DfsEquipmentItem.SpecialAbility.STATIONARY_STACKS) {
                  ratio += (double)((CustomData)stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY))
                        .copyTag()
                        .getInt("dealt_force_skills.fs_stationary_stacks")
                     * 0.04;
               }

               ratio = Math.min(0.98, ratio);
               double desiredAbsorb = (double)amount * ratio;
               if (desiredAbsorb <= 0.0) {
                  return amount;
               } else {
                  int durabilityCost = adjustedEquipmentDurabilityCost(player, profile);
                  double durabilityMultiplier = adjustedEquipmentDurabilityMultiplier(player, profile);
                  double effectiveCost = Math.max(0.0, (double)durabilityCost * durabilityMultiplier);
                  double absorbed = desiredAbsorb;
                  if (!noDurabilityLoss && effectiveCost > 0.0) {
                     int durabilityLimit = gtiOverload ? gtiOverloadLimit(stack) : DfsEquipmentItem.breakDamageLimit(stack);
                     absorbed = Math.min(desiredAbsorb, (double)(durabilityLimit - stack.getDamageValue()) / effectiveCost);
                  }

                  if (absorbed <= 0.0) {
                     return amount;
                  } else {
                     if (!noDurabilityLoss && effectiveCost > 0.0) {
                        if (shouldSkipEquipmentDurability(player)) {
                           notifyEquipmentSetTrigger(player, DfsEquipmentItem.Faction.HVK, ParticleTypes.WAX_ON);
                        } else {
                           int rawCost = Math.max(1, (int)Math.ceil(absorbed * effectiveCost));
                           damageEquipmentStack(player, stack, profile, rawCost);
                        }
                     } else if (noDurabilityLoss && profile.ability() == DfsEquipmentItem.SpecialAbility.NO_DURABILITY_LOSS) {
                        notifyEquipmentAbilityTrigger(player, profile, ParticleTypes.WAX_ON);
                     }

                     return (float)Math.max(0.0, (double)amount - absorbed);
                  }
               }
            }
         }
      } else {
         return amount;
      }
   }

   private static int adjustedEquipmentDurabilityCost(ServerPlayer player, DfsEquipmentItem.Profile profile) {
      int cost = DfsEquipmentItem.qualityDurabilityCost(profile.quality());
      if (equipmentFactionCount(player, DfsEquipmentItem.Faction.ASARA) >= 1) {
         cost--;
      }

      return Math.max(1, cost);
   }

   private static double adjustedEquipmentDurabilityMultiplier(ServerPlayer player, DfsEquipmentItem.Profile profile) {
      double multiplier = 1.0;
      if (equipmentFactionCount(player, DfsEquipmentItem.Faction.ASARA) >= 1) {
         multiplier *= 1.05;
      }

      if (equipmentFactionCount(player, DfsEquipmentItem.Faction.HVK) >= 1) {
         multiplier *= 0.9;
      }

      if (ModGameRules.areArmorSpecialsEnabled(player) && profile.ability() == DfsEquipmentItem.SpecialAbility.DURABLE) {
         multiplier *= 0.5;
      }

      if (ModGameRules.areArmorSpecialsEnabled(player) && profile.ability() == DfsEquipmentItem.SpecialAbility.GN_HEAVY) {
         multiplier *= 0.8;
      }

      if (ModGameRules.areArmorSpecialsEnabled(player) && profile.ability() == DfsEquipmentItem.SpecialAbility.GN_HEAVY_NIGHT_VISION) {
         multiplier *= 0.7;
      }

      return multiplier;
   }

   private static boolean shouldSkipEquipmentDurability(ServerPlayer player) {
      return equipmentFactionCount(player, DfsEquipmentItem.Faction.HVK) >= 2 && player.getRandom().nextFloat() < 0.25F;
   }

   private static boolean canGtiOverloadEquipment(ServerPlayer player, ItemStack stack) {
      return equipmentFactionCount(player, DfsEquipmentItem.Faction.GTI) >= 2
         && DfsEquipmentItem.profile(stack) != null
         && stack.isDamageableItem()
         && stack.getDamageValue() < gtiOverloadLimit(stack);
   }

   private static int gtiOverloadLimit(ItemStack stack) {
      return Math.max(stack.getMaxDamage() + 1, (int)Math.ceil((double)stack.getMaxDamage() * GTI_OVERLOAD_LIMIT_MULTIPLIER));
   }

   private static void damageEquipmentStack(ServerPlayer player, ItemStack stack, DfsEquipmentItem.Profile profile, int rawCost) {
      int unbreaking = Math.max(0, enchantmentLevel(player.level(), Enchantments.UNBREAKING, stack));
      int cost = Math.max(1, (int)Math.ceil((double)rawCost / (double)(unbreaking + 1)));
      boolean gtiOverload = canGtiOverloadEquipment(player, stack);
      int damageLimit = gtiOverload ? gtiOverloadLimit(stack) : DfsEquipmentItem.breakDamageLimit(stack);
      int oldDamage = stack.getDamageValue();
      int newDamage = Math.min(damageLimit, stack.getDamageValue() + cost);
      stack.setDamageValue(newDamage);
      if (gtiOverload && oldDamage < DfsEquipmentItem.breakDamageLimit(stack)
         && newDamage >= DfsEquipmentItem.breakDamageLimit(stack)) {
         notifyEquipmentSetTrigger(player, DfsEquipmentItem.Faction.GTI, ParticleTypes.ELECTRIC_SPARK);
      }
      if (gtiOverload && newDamage >= damageLimit) {
         stack.shrink(1);
      } else {
         if (ModGameRules.areArmorSpecialsEnabled(player)
            && newDamage >= DfsEquipmentItem.breakDamageLimit(stack)
            && profile.ability() == DfsEquipmentItem.SpecialAbility.DURABILITY_RESTORE) {
            CompoundTag tag = ((CustomData)stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)).copyTag();
            long now = player.level().getGameTime();
            if (now >= tag.getLong("dealt_force_skills.equipment_restore_until")) {
               stack.setDamageValue(0);
               tag.putLong("dealt_force_skills.equipment_restore_until", now + 36000L);
               notifyEquipmentAbilityTrigger(player, profile, ParticleTypes.HAPPY_VILLAGER);
            }

            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
         }
      }
   }

   private static void notifyEquipmentAbilityTrigger(ServerPlayer player, DfsEquipmentItem.Profile profile, ParticleOptions particle) {
      if (player != null && profile != null) {
         notifyEquipmentTrigger(player, "ability." + profile.id(),
            Component.translatable("message.dealt_force_skills.equipment.ability_trigger",
               Component.translatable("tooltip.dealt_force_skills." + profile.id() + ".ability")), particle);
      }
   }

   private static void notifyEquipmentSetTrigger(ServerPlayer player, DfsEquipmentItem.Faction faction, ParticleOptions particle) {
      if (player != null && faction != null) {
         notifyEquipmentTrigger(player, "set." + faction.key(),
            Component.translatable("message.dealt_force_skills.equipment.set_trigger",
               Component.translatable("faction.dealt_force_skills." + faction.key())), particle);
      }
   }

   private static void notifyEquipmentTrigger(ServerPlayer player, String id, Component message, ParticleOptions particle) {
      long now = player.level().getGameTime();
      String cooldownKey = "DfsEquipmentFeedbackUntil." + id;
      if (now >= player.getPersistentData().getLong(cooldownKey)) {
         player.getPersistentData().putLong(cooldownKey, now + 10L);
         player.displayClientMessage(message, true);
         ((ServerLevel)player.level()).sendParticles(particle, player.getX(), player.getY() + player.getBbHeight() * 0.55D,
            player.getZ(), 14, 0.35D, 0.45D, 0.35D, 0.03D);
      }
   }

   private static void tickHelmetVisionEffect(ServerPlayer player, ItemStack head) {
      int visionMode = DfsEquipmentItem.activeVisionMode(head);
      if (visionMode != 1) {
         MobEffectInstance current = player.getEffect(MobEffects.NIGHT_VISION);
         if (current != null && current.getAmplifier() == 0 && current.isAmbient() && !current.isVisible() && current.getDuration() <= 280) {
            player.removeEffect(MobEffects.NIGHT_VISION);
         }
      } else {
         MobEffectInstance current = player.getEffect(MobEffects.NIGHT_VISION);
         if (current == null || current.getDuration() < 220) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 260, 0, true, false, true), player);
         }
      }
   }

   private static void tickDfsEquipment(ServerPlayer player) {
      ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
      ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
      DfsEquipmentItem.Profile headProfile = DfsEquipmentItem.profile(head);
      DfsEquipmentItem.Profile chestProfile = DfsEquipmentItem.profile(chest);
      boolean specials = ModGameRules.areArmorSpecialsEnabled(player);
      tickHelmetVisionEffect(player, head);
      if (equipmentFactionCount(player, DfsEquipmentItem.Faction.GLOBAL_FORCES) >= 1 && player.tickCount % 100 == 0) {
         if (regenerateEquipmentDurability(head) | regenerateEquipmentDurability(chest)) {
            notifyEquipmentSetTrigger(player, DfsEquipmentItem.Faction.GLOBAL_FORCES, ParticleTypes.HAPPY_VILLAGER);
         }
      }

      tickGlobalForcesAllyLock(player);
      tickEquipmentStationaryFlag(player, head);
      tickEquipmentStationaryFlag(player, chest);
      if (specials && chestProfile != null && chestProfile.ability() == DfsEquipmentItem.SpecialAbility.STATIONARY_STACKS) {
         tickFsStationaryStacks(player, chest);
      }

      if (specials && headProfile != null && headProfile.ability() == DfsEquipmentItem.SpecialAbility.STATIONARY_STACKS) {
         tickFsStationaryStacks(player, head);
      }

      if (specials
         && headProfile != null
         && headProfile.ability() == DfsEquipmentItem.SpecialAbility.H09_RIOT_DODGE
         && chestProfile != null
         && chestProfile.id().equals("elite_ballistic_vest")) {
         tickH09StationaryDamage(player, head);
      }

      if (specials && headProfile != null && headProfile.ability() == DfsEquipmentItem.SpecialAbility.ENDURANCE_TRANSFORM) {
         tickEnduranceHelmetTransform(player, head);
      }

      if (specials
         && chestProfile != null
         && chestProfile.ability() == DfsEquipmentItem.SpecialAbility.TRICK_ASSAULT
         && player.level().getGameTime()
            > ((CustomData)chest.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)).copyTag().getLong("dealt_force_skills.trick_until")) {
         CompoundTag chestTag = ((CustomData)chest.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)).copyTag();
         chestTag.remove("dealt_force_skills.trick_stacks");
         chest.set(DataComponents.CUSTOM_DATA, CustomData.of(chestTag));
      }

      if (specials
         && headProfile != null
         && headProfile.ability() == DfsEquipmentItem.SpecialAbility.DICH9_ASSAULT
         && player.level().getGameTime()
            > ((CustomData)head.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)).copyTag().getLong("dealt_force_skills.dich9_until")) {
         CompoundTag headTag = ((CustomData)head.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)).copyTag();
         headTag.remove("dealt_force_skills.dich9_stacks");
         head.set(DataComponents.CUSTOM_DATA, CustomData.of(headTag));
      }

      tickMhsFury(player, head, specials ? headProfile : null);
      GhrothTaczEnhancement.tickEnhancedGunRuntime(player);
      updateEquipmentMovementBonus(player, chest, chestProfile, head, headProfile);
      tickKingKongExecution(player);
   }

   private static void tickGlobalForcesAllyLock(ServerPlayer player) {
      CompoundTag tag = player.getPersistentData();
      if (equipmentFactionCount(player, DfsEquipmentItem.Faction.GLOBAL_FORCES) < 2) {
         clearGlobalForcesLock(tag);
      } else {
         if (tag.hasUUID("dealt_force_skills.global_forces_lock_target")) {
            ServerPlayer locked = player.getServer().getPlayerList().getPlayer(tag.getUUID("dealt_force_skills.global_forces_lock_target"));
            if (!isValidGlobalForcesAlly(player, locked)) {
               tag.remove("dealt_force_skills.global_forces_lock_target");
            }
         }

         if (!player.isShiftKeyDown()) {
            clearGlobalForcesLockProgress(tag);
         } else {
            ServerPlayer target = findGlobalForcesLookTarget(player);
            if (target == null) {
               clearGlobalForcesLockProgress(tag);
            } else {
               UUID targetId = target.getUUID();
               int ticks = 1;
               if (tag.hasUUID("dealt_force_skills.global_forces_lock_progress_target")
                  && tag.getUUID("dealt_force_skills.global_forces_lock_progress_target").equals(targetId)) {
                  ticks = tag.getInt("dealt_force_skills.global_forces_lock_progress_ticks") + 1;
               } else {
                  tag.putUUID("dealt_force_skills.global_forces_lock_progress_target", targetId);
               }

               tag.putInt("dealt_force_skills.global_forces_lock_progress_ticks", ticks);
               if (ticks >= GLOBAL_FORCES_LOCK_TICKS_REQUIRED) {
                  boolean changedTarget = !tag.hasUUID("dealt_force_skills.global_forces_lock_target")
                     || !tag.getUUID("dealt_force_skills.global_forces_lock_target").equals(targetId);
                  tag.putUUID("dealt_force_skills.global_forces_lock_target", targetId);
                  clearGlobalForcesLockProgress(tag);
                  if (changedTarget) {
                     player.displayClientMessage(
                        Component.translatable("message.dealt_force_skills.global_forces.locked", new Object[]{target.getDisplayName()}), true
                     );
                  }
               }
            }
         }
      }
   }

   private static float tryAbsorbGlobalForcesAllyDamage(ServerPlayer victim, DamageSource source, float amount) {
      if (!(amount <= 0.0F) && !isEquipmentBypassDamage(source)) {
         float adjusted = amount;

         for (ServerPlayer protector : victim.serverLevel().players()) {
            if (isGlobalForcesProtecting(protector, victim)) {
               float before = adjusted;
               adjusted = absorbWithGlobalForcesAllyEquipment(protector, adjusted);
               if (adjusted < before) {
                  notifyEquipmentSetTrigger(protector, DfsEquipmentItem.Faction.GLOBAL_FORCES, ParticleTypes.HEART);
               }
               if (adjusted <= 0.0F) {
                  return 0.0F;
               }
            }
         }

         return adjusted;
      } else {
         return amount;
      }
   }

   private static boolean isGlobalForcesProtecting(ServerPlayer protector, ServerPlayer victim) {
      if (protector != victim && equipmentFactionCount(protector, DfsEquipmentItem.Faction.GLOBAL_FORCES) >= 2) {
         CompoundTag tag = protector.getPersistentData();
         return tag.hasUUID("dealt_force_skills.global_forces_lock_target")
            && tag.getUUID("dealt_force_skills.global_forces_lock_target").equals(victim.getUUID())
            && isValidGlobalForcesAlly(protector, victim);
      } else {
         return false;
      }
   }

   private static float absorbWithGlobalForcesAllyEquipment(ServerPlayer protector, float amount) {
      ItemStack head = protector.getItemBySlot(EquipmentSlot.HEAD);
      DfsEquipmentItem.Profile headProfile = DfsEquipmentItem.profile(head);
      if (headProfile != null) {
         amount = absorbWithEquipment(protector, head, headProfile, amount);
      }

      if (amount <= 0.0F) {
         return 0.0F;
      } else {
         ItemStack chest = protector.getItemBySlot(EquipmentSlot.CHEST);
         DfsEquipmentItem.Profile chestProfile = DfsEquipmentItem.profile(chest);
         if (chestProfile != null) {
            amount = absorbWithEquipment(protector, chest, chestProfile, amount);
         }

         return amount;
      }
   }

   private static ServerPlayer findGlobalForcesLookTarget(ServerPlayer player) {
      Vec3 eye = player.getEyePosition();
      Vec3 look = player.getLookAngle().normalize();
      Vec3 end = eye.add(look.scale(GLOBAL_FORCES_ALLY_RANGE));
      AABB search = player.getBoundingBox().expandTowards(look.scale(GLOBAL_FORCES_ALLY_RANGE)).inflate(GLOBAL_FORCES_LOCK_RAY_RADIUS);
      ServerPlayer best = null;
      double bestDistance = Double.MAX_VALUE;

      for (ServerPlayer target : player.serverLevel()
         .getEntitiesOfClass(ServerPlayer.class, search, targetx -> isValidGlobalForcesAlly(player, targetx) && player.hasLineOfSight(targetx))) {
         double distanceToRay = distanceToSegment(target.getBoundingBox().getCenter(), eye, end);
         if (!(distanceToRay > GLOBAL_FORCES_LOCK_RAY_RADIUS)) {
            double eyeDistance = eye.distanceToSqr(target.getEyePosition());
            if (eyeDistance < bestDistance) {
               bestDistance = eyeDistance;
               best = target;
            }
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
         && DealtTeamManager.areTeammates(player, target);
   }

   private static double distanceToSegment(Vec3 point, Vec3 start, Vec3 end) {
      Vec3 segment = end.subtract(start);
      double lengthSqr = segment.lengthSqr();
      if (lengthSqr < 1.0E-6) {
         return point.distanceTo(start);
      } else {
         double t = point.subtract(start).dot(segment) / lengthSqr;
         t = Math.max(0.0, Math.min(1.0, t));
         return point.distanceTo(start.add(segment.scale(t)));
      }
   }

   private static void clearGlobalForcesLock(CompoundTag tag) {
      tag.remove("dealt_force_skills.global_forces_lock_target");
      clearGlobalForcesLockProgress(tag);
   }

   private static void clearGlobalForcesLockProgress(CompoundTag tag) {
      tag.remove("dealt_force_skills.global_forces_lock_progress_target");
      tag.remove("dealt_force_skills.global_forces_lock_progress_ticks");
   }

   private static boolean regenerateEquipmentDurability(ItemStack stack) {
      if (!stack.isEmpty() && stack.isDamageableItem() && stack.isDamaged()) {
         int repair = Math.max(1, stack.getMaxDamage() / 100);
         stack.setDamageValue(Math.max(0, stack.getDamageValue() - repair));
         return true;
      }
      return false;
   }

   private static void tickFsStationaryStacks(ServerPlayer player, ItemStack stack) {
      CompoundTag tag = ((CustomData)stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)).copyTag();
      Vec3 position = player.position();
      if (!tag.contains("dealt_force_skills.fs_last_x")) {
         tag.putDouble("dealt_force_skills.fs_last_x", position.x);
         tag.putDouble("dealt_force_skills.fs_last_y", position.y);
         tag.putDouble("dealt_force_skills.fs_last_z", position.z);
         stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
      } else {
         Vec3 previous = new Vec3(
            tag.getDouble("dealt_force_skills.fs_last_x"), tag.getDouble("dealt_force_skills.fs_last_y"), tag.getDouble("dealt_force_skills.fs_last_z")
         );
         boolean stationary = previous.distanceToSqr(position) < 0.0025;
         tag.putDouble("dealt_force_skills.fs_last_x", position.x);
         tag.putDouble("dealt_force_skills.fs_last_y", position.y);
         tag.putDouble("dealt_force_skills.fs_last_z", position.z);
         if (stationary) {
            int stationaryTicks = tag.getInt("dealt_force_skills.fs_stationary_ticks") + 1;
            tag.putInt("dealt_force_skills.fs_stationary_ticks", stationaryTicks);
            tag.putInt("dealt_force_skills.fs_moving_ticks", 0);
            if (stationaryTicks >= 100) {
               int oldStacks = tag.getInt("dealt_force_skills.fs_stationary_stacks");
               tag.putInt("dealt_force_skills.fs_stationary_stacks", Math.min(8, oldStacks + 1));
               tag.putInt("dealt_force_skills.fs_stationary_ticks", 0);
               if (oldStacks < 8) {
                  notifyEquipmentAbilityTrigger(player, DfsEquipmentItem.profile(stack), ParticleTypes.END_ROD);
               }
            }
         } else {
            int movingTicks = tag.getInt("dealt_force_skills.fs_moving_ticks") + 1;
            tag.putInt("dealt_force_skills.fs_moving_ticks", movingTicks);
            tag.putInt("dealt_force_skills.fs_stationary_ticks", 0);
            if (movingTicks >= 200) {
               tag.putInt("dealt_force_skills.fs_stationary_stacks", Math.max(0, tag.getInt("dealt_force_skills.fs_stationary_stacks") - 1));
               tag.putInt("dealt_force_skills.fs_moving_ticks", 0);
            }
         }

         stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
      }
   }

   private static void tickEquipmentStationaryFlag(ServerPlayer player, ItemStack stack) {
      if (!stack.isEmpty()) {
         CompoundTag tag = ((CustomData)stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)).copyTag();
         Vec3 position = player.position();
         if (!tag.contains("dealt_force_skills.equipment_last_x")) {
            tag.putDouble("dealt_force_skills.equipment_last_x", position.x);
            tag.putDouble("dealt_force_skills.equipment_last_y", position.y);
            tag.putDouble("dealt_force_skills.equipment_last_z", position.z);
            tag.putBoolean("dealt_force_skills.equipment_stationary", true);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
         } else {
            Vec3 previous = new Vec3(
               tag.getDouble("dealt_force_skills.equipment_last_x"),
               tag.getDouble("dealt_force_skills.equipment_last_y"),
               tag.getDouble("dealt_force_skills.equipment_last_z")
            );
            tag.putDouble("dealt_force_skills.equipment_last_x", position.x);
            tag.putDouble("dealt_force_skills.equipment_last_y", position.y);
            tag.putDouble("dealt_force_skills.equipment_last_z", position.z);
            tag.putBoolean("dealt_force_skills.equipment_stationary", previous.distanceToSqr(position) < 0.0025);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
         }
      }
   }

   private static void tickH09StationaryDamage(ServerPlayer player, ItemStack head) {
      CompoundTag tag = ((CustomData)head.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)).copyTag();
      if (!tag.getBoolean("dealt_force_skills.equipment_stationary")) {
         tag.putInt("dealt_force_skills.equipment_stationary_damage_ticks", 0);
         head.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
      } else {
         int ticks = tag.getInt("dealt_force_skills.equipment_stationary_damage_ticks") + 1;
         if (ticks < 20) {
            tag.putInt("dealt_force_skills.equipment_stationary_damage_ticks", ticks);
            head.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
         } else {
            tag.putInt("dealt_force_skills.equipment_stationary_damage_ticks", 0);
            head.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            player.hurt(
               player.damageSources().generic(),
               Math.max(H09_STATIONARY_MIN_SELF_DAMAGE, player.getMaxHealth() * H09_STATIONARY_SELF_DAMAGE_MAX_HEALTH_FRACTION)
            );
         }
      }
   }

   private static void tickEnduranceHelmetTransform(ServerPlayer player, ItemStack head) {
      CompoundTag tag = ((CustomData)head.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)).copyTag();
      if (head.isDamaged()) {
         tag.putInt("dealt_force_skills.equipment_stationary_damage_ticks", 0);
         head.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
      } else {
         int wornTicks = tag.getInt("dealt_force_skills.equipment_stationary_damage_ticks") + 1;
         if (wornTicks < 3600) {
            tag.putInt("dealt_force_skills.equipment_stationary_damage_ticks", wornTicks);
            head.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
         } else {
            ItemStack replacement = new ItemStack((ItemLike)ModItems.GN_HEAVY_NIGHT_VISION_HELMET.get());
            CustomData headCd = (CustomData)head.get(DataComponents.CUSTOM_DATA);
            if (headCd != null) {
               replacement.set(DataComponents.CUSTOM_DATA, headCd);
               replacement.setDamageValue(0);
            }

            notifyEquipmentAbilityTrigger(player, DfsEquipmentItem.profile(head), ParticleTypes.TOTEM_OF_UNDYING);
            player.setItemSlot(EquipmentSlot.HEAD, replacement);
         }
      }
   }

   private static void tickMhsFury(ServerPlayer player, ItemStack head, DfsEquipmentItem.Profile headProfile) {
      long now = player.level().getGameTime();
      boolean active = headProfile != null
         && headProfile.ability() == DfsEquipmentItem.SpecialAbility.MHS_FURY
         && now <= ((CustomData)head.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)).copyTag().getLong("dealt_force_skills.mhs_fury_until");
      if (!active
         && headProfile != null
         && headProfile.ability() == DfsEquipmentItem.SpecialAbility.MHS_FURY
         && isTaczGunStack(player.getMainHandItem())
         && hasMhsFuryTriggerTarget(player)) {
         CompoundTag tag = ((CustomData)head.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)).copyTag();
         tag.putLong("dealt_force_skills.mhs_fury_until", now + (long)MHS_FURY_DURATION_TICKS);
         tag.putInt("dealt_force_skills.mhs_fury_slot", player.getInventory().selected);
         head.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
         active = true;
         notifyEquipmentAbilityTrigger(player, headProfile, ParticleTypes.ANGRY_VILLAGER);
      }

      if (!active) {
         MHS_TACTICAL_AMMO_SNAPSHOTS.remove(player.getUUID());
      } else {
         lockMhsFuryHotbar(player, head);
         Vec3 velocity = player.getDeltaMovement();
         player.setDeltaMovement(0.0, Math.min(velocity.y, 0.0), 0.0);
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
   }

   private static boolean hasMhsFuryTriggerTarget(ServerPlayer player) {
      AABB area = player.getBoundingBox().inflate(MHS_FURY_TRIGGER_RANGE);

      for (LivingEntity target : player.level()
         .getEntitiesOfClass(
            LivingEntity.class, area, entity -> entity != player && entity.isAlive() && !entity.isSpectator()
               && entity.getType() != EntityType.ARMOR_STAND && TargetingUtil.isHostileLivingFor(player, entity)
         )) {
         if (target.distanceToSqr(player) <= MHS_FURY_TRIGGER_RANGE * MHS_FURY_TRIGGER_RANGE) {
            return true;
         }
      }

      return false;
   }

   private static void lockMhsFuryHotbar(ServerPlayer player, ItemStack head) {
      CompoundTag tag = ((CustomData)head.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)).copyTag();
      int lockedSlot = tag.contains("dealt_force_skills.mhs_fury_slot") ? tag.getInt("dealt_force_skills.mhs_fury_slot") : player.getInventory().selected;
      if (lockedSlot < 0 || lockedSlot > 8) {
         lockedSlot = player.getInventory().selected;
         tag.putInt("dealt_force_skills.mhs_fury_slot", lockedSlot);
      }

      head.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
      if (player.getInventory().selected != lockedSlot) {
         player.getInventory().selected = lockedSlot;
         player.connection.send(new ClientboundSetCarriedItemPacket(lockedSlot));
      }
   }

   private static boolean isMhsFuryActive(Player player) {
      if (!ModGameRules.areArmorSpecialsEnabled(player)) {
         return false;
      } else {
         ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
         DfsEquipmentItem.Profile headProfile = DfsEquipmentItem.profile(head);
         return headProfile != null
            && headProfile.ability() == DfsEquipmentItem.SpecialAbility.MHS_FURY
            && player.level().getGameTime()
               <= ((CustomData)head.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)).copyTag().getLong("dealt_force_skills.mhs_fury_until");
      }
   }

   private static void rememberMhsTaczAmmo(ServerPlayer player, ItemStack gun, long expiresAt) {
      CompoundTag tag = ((CustomData)gun.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)).copyTag();
      String gunId = mhsGunId(gun);
      int ammoCount = tag.getInt("GunCurrentAmmoCount");
      boolean bulletInBarrel = tag.getBoolean("HasBulletInBarrel");
      CommonEvents.TaczAmmoSnapshot existing = MHS_TACTICAL_AMMO_SNAPSHOTS.get(player.getUUID());
      if (existing != null && existing.gunId().equals(gunId)) {
         if (existing.ammoCount() > ammoCount) {
            ammoCount = existing.ammoCount();
            bulletInBarrel = existing.bulletInBarrel();
         } else if (existing.ammoCount() == ammoCount) {
            bulletInBarrel = bulletInBarrel || existing.bulletInBarrel();
         }
      }

      MHS_TACTICAL_AMMO_SNAPSHOTS.put(player.getUUID(), new CommonEvents.TaczAmmoSnapshot(gunId, ammoCount, bulletInBarrel, expiresAt));
   }

   private static void restoreMhsTaczAmmo(ServerPlayer player, ItemStack gun, long now) {
      CommonEvents.TaczAmmoSnapshot snapshot = MHS_TACTICAL_AMMO_SNAPSHOTS.get(player.getUUID());
      if (snapshot != null) {
         if (now <= snapshot.expiresAt() && snapshot.gunId().equals(mhsGunId(gun))) {
            CompoundTag tag = ((CustomData)gun.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)).copyTag();
            if (tag.getInt("GunCurrentAmmoCount") < snapshot.ammoCount()) {
               tag.putInt("GunCurrentAmmoCount", snapshot.ammoCount());
            }

            if (snapshot.bulletInBarrel() && !tag.getBoolean("HasBulletInBarrel")) {
               tag.putBoolean("HasBulletInBarrel", true);
            }

            gun.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
         } else {
            MHS_TACTICAL_AMMO_SNAPSHOTS.remove(player.getUUID());
         }
      }
   }

   private static String mhsGunId(ItemStack gun) {
      CustomData cd = (CustomData)gun.get(DataComponents.CUSTOM_DATA);
      CompoundTag tag = cd != null ? cd.copyTag() : null;
      if (tag != null && tag.contains("GunId")) {
         String gunId = tag.getString("GunId");
         if (!gunId.isEmpty()) {
            return gunId;
         }
      }

      return gun.getItem().getClass().getName();
   }

   private static boolean isTaczGunStack(ItemStack stack) {
      if (stack.isEmpty()) {
         return false;
      } else {
         CustomData cd = (CustomData)stack.get(DataComponents.CUSTOM_DATA);
         CompoundTag tag = cd != null ? cd.copyTag() : null;
         return tag != null && tag.contains("GunId") ? true : stack.getItem().getClass().getName().startsWith("com.tacz.guns.");
      }
   }

   private static void updateEquipmentMovementBonus(
      ServerPlayer player, ItemStack chest, DfsEquipmentItem.Profile chestProfile, ItemStack head, DfsEquipmentItem.Profile headProfile
   ) {
      AttributeInstance attr = player.getAttribute(Attributes.MOVEMENT_SPEED);
      if (attr != null) {
         AttributeModifier existing = attr.getModifier(EQUIPMENT_ASSAULT_MOVEMENT_UUID);
         if (existing != null) {
            attr.removeModifier(EQUIPMENT_ASSAULT_MOVEMENT_UUID);
         }

         AttributeModifier existingRoot = attr.getModifier(EQUIPMENT_MHS_ROOT_UUID);
         if (existingRoot != null) {
            attr.removeModifier(EQUIPMENT_MHS_ROOT_UUID);
         }

         if (ModGameRules.areArmorSpecialsEnabled(player)) {
            double bonus = 0.0;
            long now = player.level().getGameTime();
            if (chestProfile != null
               && chestProfile.ability() == DfsEquipmentItem.SpecialAbility.TRICK_ASSAULT
               && now <= ((CustomData)chest.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)).copyTag().getLong("dealt_force_skills.trick_until")) {
               bonus += (double)((CustomData)chest.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY))
                     .copyTag()
                     .getInt("dealt_force_skills.trick_stacks")
                  * 0.05;
            }

            if (headProfile != null
               && headProfile.ability() == DfsEquipmentItem.SpecialAbility.DICH9_ASSAULT
               && now <= ((CustomData)head.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)).copyTag().getLong("dealt_force_skills.dich9_until")) {
               bonus += (double)((CustomData)head.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY))
                     .copyTag()
                     .getInt("dealt_force_skills.dich9_stacks")
                  * 0.04;
            }

            if (headProfile != null && headProfile.ability() == DfsEquipmentItem.SpecialAbility.GN_HEAVY) {
               bonus += 0.05;
            }

            if (headProfile != null && headProfile.ability() == DfsEquipmentItem.SpecialAbility.GN_HEAVY_NIGHT_VISION) {
               bonus += 0.1;
            }

            if (bonus > 1.0E-4) {
               attr.addTransientModifier(new AttributeModifier(EQUIPMENT_ASSAULT_MOVEMENT_UUID, bonus, Operation.ADD_MULTIPLIED_TOTAL));
            }

            if (isMhsFuryActive(player)) {
               attr.addTransientModifier(new AttributeModifier(EQUIPMENT_MHS_ROOT_UUID, -1.0, Operation.ADD_MULTIPLIED_TOTAL));
            }
         }
      }
   }

   private static void addTimedEquipmentStacks(ItemStack stack, String stacksKey, String untilKey, long now, int maxStacks, long durationTicks) {
      CompoundTag tag = ((CustomData)stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)).copyTag();
      int stacks = now <= tag.getLong(untilKey) ? tag.getInt(stacksKey) : 0;
      tag.putInt(stacksKey, Math.min(maxStacks, stacks + 1));
      tag.putLong(untilKey, now + durationTicks);
      stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
   }

   private static double equipmentTaczAssaultMultiplier(LivingEntity entity) {
      if (entity instanceof Player player) {
         if (!ModGameRules.areArmorSpecialsEnabled(player)) {
            return 1.0;
         } else {
            double multiplier = 1.0;
            long now = entity.level().getGameTime();
            ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
            DfsEquipmentItem.Profile chestProfile = DfsEquipmentItem.profile(chest);
            if (chestProfile != null
               && chestProfile.ability() == DfsEquipmentItem.SpecialAbility.TRICK_ASSAULT
               && now <= ((CustomData)chest.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)).copyTag().getLong("dealt_force_skills.trick_until")) {
               multiplier *= 1.0
                  + (double)((CustomData)chest.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)).copyTag().getInt("dealt_force_skills.trick_stacks")
                     * 0.04;
            }

            ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
            DfsEquipmentItem.Profile headProfile = DfsEquipmentItem.profile(head);
            if (headProfile != null
               && headProfile.ability() == DfsEquipmentItem.SpecialAbility.DICH9_ASSAULT
               && now <= ((CustomData)head.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)).copyTag().getLong("dealt_force_skills.dich9_until")) {
               multiplier *= 1.0
                  + (double)((CustomData)head.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)).copyTag().getInt("dealt_force_skills.dich9_stacks")
                     * 0.06;
            }

            if (isMhsFuryActive(player)) {
               multiplier *= 2.0;
            }

            return multiplier;
         }
      } else {
         return 1.0;
      }
   }

   private static void duplicateDropsForEquipment(LivingDropsEvent event, ServerPlayer killer) {
      double chance = equipmentLootDuplicateChance(killer);
      if (!(chance <= 0.0) && !(killer.getRandom().nextDouble() >= chance)) {
         boolean duplicated = false;
         for (ItemEntity original : new ArrayList<>(event.getDrops())) {
            ItemStack copy = original.getItem().copy();
            if (!copy.isEmpty()) {
               LivingEntity entity = event.getEntity();
               event.getDrops().add(new ItemEntity(entity.level(), entity.getX(), entity.getY(), entity.getZ(), copy));
               duplicated = true;
            }
         }

         if (duplicated) {
            DfsEquipmentItem.Profile profile = equippedProfileWithAbility(killer, DfsEquipmentItem.SpecialAbility.LOOT_DUPLICATE);
            if (profile != null) {
               notifyEquipmentAbilityTrigger(killer, profile, ParticleTypes.HAPPY_VILLAGER);
            } else {
               notifyEquipmentSetTrigger(killer, DfsEquipmentItem.Faction.GTI, ParticleTypes.HAPPY_VILLAGER);
            }
         }
      }
   }

   private static void duplicateLivingDrops(LivingDropsEvent event, int extraCopies) {
      if (extraCopies > 0) {
         LivingEntity entity = event.getEntity();
         List<ItemEntity> originals = new ArrayList<>(event.getDrops());

         for (int copyIndex = 0; copyIndex < extraCopies; copyIndex++) {
            for (ItemEntity original : originals) {
               ItemStack copy = original.getItem().copy();
               if (!copy.isEmpty()) {
                  event.getDrops().add(new ItemEntity(entity.level(), entity.getX(), entity.getY(), entity.getZ(), copy));
               }
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
      if (!ModGameRules.areArmorSpecialsEnabled(player)) {
         return 0.0;
      } else if (equipmentFactionCount(player, DfsEquipmentItem.Faction.GTI) >= 1) {
         return 1.0;
      } else {
         double chance = 0.0;
         DfsEquipmentItem.Profile chest = DfsEquipmentItem.profile(player.getItemBySlot(EquipmentSlot.CHEST));
         DfsEquipmentItem.Profile head = DfsEquipmentItem.profile(player.getItemBySlot(EquipmentSlot.HEAD));
         if (chest != null && chest.ability() == DfsEquipmentItem.SpecialAbility.LOOT_DUPLICATE) {
            chance = Math.max(chance, chest.lootDuplicateChance());
         }

         if (head != null && head.ability() == DfsEquipmentItem.SpecialAbility.LOOT_DUPLICATE) {
            chance = Math.max(chance, head.lootDuplicateChance());
         }

         return chance;
      }
   }

   private static void recordRedOwlRevengeOnDeath(ServerPlayer player, DamageSource source) {
      if (ModGameRules.areArmorSpecialsEnabled(player)) {
         DfsEquipmentItem.Profile chest = DfsEquipmentItem.profile(player.getItemBySlot(EquipmentSlot.CHEST));
         Entity attacker = source.getEntity();
         if (chest != null && chest.ability() == DfsEquipmentItem.SpecialAbility.RED_OWL_REVENGE && attacker != null) {
            player.getPersistentData().putString("dealt_force_skills.red_owl_revenge_type", attacker.getType().getDescriptionId());
         }
      }
   }

   private static double equipmentActionSpeedMultiplier(LivingEntity entity) {
      if (entity instanceof Player player) {
         if (!ModGameRules.areArmorSpecialsEnabled(player)) {
            return 1.0;
         } else {
            boolean hvkTwoPiece = equipmentFactionCount(player, DfsEquipmentItem.Faction.HVK) >= 2;
            double multiplier = 1.0;
            DfsEquipmentItem.Profile chest = DfsEquipmentItem.profile(player.getItemBySlot(EquipmentSlot.CHEST));
            DfsEquipmentItem.Profile head = DfsEquipmentItem.profile(player.getItemBySlot(EquipmentSlot.HEAD));
            if (chest != null) {
               multiplier *= chest.actionSpeedMultiplier(hvkTwoPiece);
            }

            if (head != null) {
               multiplier *= head.actionSpeedMultiplier(hvkTwoPiece);
            }

            return Math.max(0.1, multiplier);
         }
      } else {
         return 1.0;
      }
   }

   private static double normalizedUseSpeedMultiplier(double multiplier) {
      return !Double.isFinite(multiplier) ? 1.0 : Mth.clamp(multiplier, 0.05, 20.0);
   }

   private static boolean shouldAdjustUseDuration(ItemStack stack) {
      if (stack != null && !stack.isEmpty()) {
         ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
         return id == null ? false : "minecraft".equals(id.getNamespace()) || "dealt_force_skills".equals(id.getNamespace());
      } else {
         return false;
      }
   }

   private static int equipmentFactionCount(Player player, DfsEquipmentItem.Faction faction) {
      if (!ModGameRules.areArmorSpecialsEnabled(player)) {
         return 0;
      } else {
         int count = 0;
         DfsEquipmentItem.Profile chest = DfsEquipmentItem.profile(player.getItemBySlot(EquipmentSlot.CHEST));
         DfsEquipmentItem.Profile head = DfsEquipmentItem.profile(player.getItemBySlot(EquipmentSlot.HEAD));
         if (chest != null && chest.faction() == faction) {
            count++;
         }

         if (head != null && head.faction() == faction) {
            count++;
         }

         return count;
      }
   }

   private static boolean isEquipmentBypassDamage(DamageSource source) {
      return source.is(DamageTypes.FALL) || source.is(DamageTypes.FELL_OUT_OF_WORLD) || source.is(DamageTypes.MAGIC) || source.is(DamageTypes.INDIRECT_MAGIC);
   }

   private static boolean isEquipmentCoveredHit(LivingEntity entity, DamageSource source, DfsEquipmentItem.Profile profile) {
      if (profile == null) {
         return false;
      } else {
         Double ratio = estimateEquipmentHitHeightRatio(entity, source);
         if (ratio == null) {
            ratio = 0.55;
         }
         if (profile.isHelmet()) {
            return ratio >= 0.74;
         } else {
            String var4 = profile.coverageKey();

            return switch (var4) {
               case "upper_torso" -> ratio >= 0.42 && ratio < 0.78;
               case "full_torso" -> ratio >= 0.36 && ratio < 0.78;
               case "full_torso_arms" -> ratio >= 0.32 && ratio < 0.82;
               default -> ratio >= 0.36 && ratio < 0.78;
            };
         }
      }
   }

   static Double estimateEquipmentHitHeightRatio(LivingEntity entity, DamageSource source) {
      if (isMeleeDamage(source, source.getEntity())) {
         return 0.55;
      } else {
         Vec3 impact = source.getSourcePosition();
         if (impact != null && isNearPlayerHitbox(entity, impact, EQUIPMENT_PROJECTILE_NEAR_INFLATE)) {
            return playerHeightRatio(entity, impact.y);
         } else {
            Entity direct = source.getDirectEntity();
            if (direct instanceof Projectile && direct != entity) {
               Double ratio = estimateProjectileHitHeightRatio(entity, source, direct);
               if (ratio != null) {
                  return ratio;
               }
            }

            if (isLikelyRangedDamage(source)) {
               Double ratio = estimateRangedAttackerHitHeightRatio(entity, source);
               if (ratio != null) {
                  return ratio;
               }
            }

            return null;
         }
      }
   }

   private static Double estimateProjectileHitHeightRatio(LivingEntity entity, DamageSource source, Entity projectile) {
      Vec3 projectileCenter = projectile.getBoundingBox().getCenter();
      if (isNearPlayerHitbox(entity, projectileCenter, EQUIPMENT_PROJECTILE_NEAR_INFLATE)) {
         return playerHeightRatio(entity, projectileCenter.y);
      } else {
         Vec3 motion = projectile.getDeltaMovement();
         if (motion.lengthSqr() > 1.0E-6) {
            Double ratio = rayHitHeightRatio(entity, projectileCenter.subtract(motion), projectileCenter.add(motion), EQUIPMENT_RANGED_TRACE_INFLATE);
            if (ratio != null) {
               return ratio;
            }
         }

         Entity attacker = source.getEntity();
         return attacker != null && attacker != entity && attacker != projectile
            ? rayHitHeightRatio(entity, entityAimOrigin(attacker), projectileCenter, EQUIPMENT_RANGED_TRACE_INFLATE)
            : null;
      }
   }

   private static Double estimateRangedAttackerHitHeightRatio(LivingEntity entity, DamageSource source) {
      Entity attacker = source.getEntity();
      if (attacker instanceof LivingEntity && attacker != entity) {
         Vec3 from = entityAimOrigin(attacker);
         Vec3 look = attacker.getLookAngle();
         if (look.lengthSqr() > 1.0E-6) {
            double distance = Math.max(6.0, from.distanceTo(entity.getBoundingBox().getCenter()) + 2.0);
            Double ratio = rayHitHeightRatio(entity, from, from.add(look.normalize().scale(distance)), EQUIPMENT_RANGED_TRACE_INFLATE);
            if (ratio != null) {
               return ratio;
            }
         }

         Vec3 impact = source.getSourcePosition();
         return impact != null && impact.distanceToSqr(from) > 0.01 ? rayHitHeightRatio(entity, from, impact, EQUIPMENT_RANGED_TRACE_INFLATE) : null;
      } else {
         return null;
      }
   }

   private static boolean isNearPlayerHitbox(LivingEntity entity, Vec3 pos, double inflate) {
      return entity.getBoundingBox().inflate(inflate).contains(pos);
   }

   private static Double rayHitHeightRatio(LivingEntity entity, Vec3 from, Vec3 target) {
      return rayHitHeightRatio(entity, from, target, 0.08);
   }

   private static Double rayHitHeightRatio(LivingEntity entity, Vec3 from, Vec3 target, double inflate) {
      Vec3 delta = target.subtract(from);
      double length = delta.length();
      if (length < 1.0E-4) {
         return null;
      } else {
         Vec3 end = from.add(delta.scale((length + 1.0) / length));
         Optional<Vec3> hit = entity.getBoundingBox().inflate(inflate).clip(from, end);
         return hit.<Double>map(vec3 -> playerHeightRatio(entity, vec3.y)).orElse(null);
      }
   }

   private static double playerHeightRatio(LivingEntity entity, double y) {
      return Mth.clamp((y - entity.getY()) / Math.max(0.001, (double)entity.getBbHeight()), 0.0, 1.0);
   }

   private static boolean isLikelyHeadDamage(Player player, DamageSource source) {
      Double ratio = estimateEquipmentHitHeightRatio(player, source);
      return ratio != null && ratio >= 0.72;
   }

   private static boolean isMeleeDamage(DamageSource source, Entity expectedAttacker) {
      Entity attacker = source.getEntity();
      Entity direct = source.getDirectEntity();
      if (attacker instanceof com.rzy.dealt_force_skills.entity.BeaconBossEntity) {
         return false;
      }
      return attacker instanceof LivingEntity && !(direct instanceof Projectile) && !isLikelyRangedDamage(source)
         ? expectedAttacker == null || attacker == expectedAttacker
         : false;
   }

   private static boolean isLikelyRangedDamage(DamageSource source) {
      
      if (source.getEntity() instanceof com.rzy.dealt_force_skills.entity.BeaconBossEntity) {
         return true;
      }
Entity direct = source.getDirectEntity();
      if (!(direct instanceof Projectile) && !source.is(TACZ_BULLETS_TAG)) {
         String msgId = source.getMsgId();
         return msgId != null && (msgId.contains("bullet") || msgId.contains("projectile") || msgId.contains("arrow"));
      } else {
         return true;
      }
   }

   private static void tryStartKingKongExecution(ServerPlayer player, ItemStack chest, LivingEntity target) {
      CompoundTag chestTag = ((CustomData)chest.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)).copyTag();
      long now = player.level().getGameTime();
      if (now >= chestTag.getLong("dealt_force_skills.king_kong_cooldown_until")) {
         chestTag.putLong("dealt_force_skills.king_kong_cooldown_until", now + 1200L);
         chest.set(DataComponents.CUSTOM_DATA, CustomData.of(chestTag));
         CompoundTag playerTag = player.getPersistentData();
         playerTag.putInt("dealt_force_skills.king_kong_target_id", target.getId());
         playerTag.putLong("dealt_force_skills.king_kong_strike_tick", now + 20L);
         Vec3 direction = player.position().subtract(target.position());
         if (direction.lengthSqr() < 1.0E-4) {
            direction = target.getLookAngle().reverse();
         }

         Vec3 offset = direction.normalize().scale(1.3);
         Vec3 destination = target.position().add(offset);
         player.teleportTo(destination.x, target.getY(), destination.z);
         lookAt(player, target);
         lookAt(target, player);
         notifyEquipmentAbilityTrigger(player, DfsEquipmentItem.profile(chest), ParticleTypes.PORTAL);
      }
   }

   private static void tickKingKongExecution(ServerPlayer player) {
      CompoundTag tag = player.getPersistentData();
      long strikeTick = tag.getLong("dealt_force_skills.king_kong_strike_tick");
      if (strikeTick > 0L && player.level().getGameTime() >= strikeTick) {
         int targetId = tag.getInt("dealt_force_skills.king_kong_target_id");
         tag.remove("dealt_force_skills.king_kong_target_id");
         tag.remove("dealt_force_skills.king_kong_strike_tick");
         if (player.level() instanceof ServerLevel serverLevel) {
            if (serverLevel.getEntity(targetId) instanceof LivingEntity target
               && target.isAlive()
               && target != player
               && !(target.distanceToSqr(player) > 16.0)) {
               target.hurt(player.damageSources().playerAttack(player), Math.max(1.0F, target.getHealth() * 20.0F));
               return;
            }
         }
      }
   }

   private static void lookAt(LivingEntity entity, LivingEntity target) {
      double dx = target.getX() - entity.getX();
      double dz = target.getZ() - entity.getZ();
      float yaw = (float)(Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
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
      if (!(horizontal < 1.0E-4)) {
         float yaw = (float)(Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
         float pitch = (float)(-(Mth.atan2(dy, horizontal) * (180.0 / Math.PI)));
         entity.setYRot(yaw);
         entity.setXRot(Mth.clamp(pitch, -89.0F, 89.0F));
         entity.setYHeadRot(yaw);
         entity.setYBodyRot(yaw);
      }
   }

   private static void spawnEquipmentSupport(ServerPlayer player, LivingEntity target, int count, boolean boosted) {
      spawnEquipmentSupport(player, target, count, boosted, false);
   }

   private static void spawnEquipmentSupport(ServerPlayer player, LivingEntity target, int count, boolean boosted, boolean ironOnly) {
      if (player.level() instanceof ServerLevel serverLevel) {
         for (int var17 = 0; var17 < count; var17++) {
            Mob mob = !ironOnly && !player.getRandom().nextBoolean()
               ? (Mob)EntityType.SNOW_GOLEM.create(serverLevel)
               : (Mob)EntityType.IRON_GOLEM.create(serverLevel);
            if (mob != null) {
               double angle = player.getRandom().nextDouble() * Math.PI * 2.0;
               double radius = 1.5 + player.getRandom().nextDouble() * 2.0;
               mob.moveTo(
                  player.getX() + Math.cos(angle) * radius,
                  player.getY(),
                  player.getZ() + Math.sin(angle) * radius,
                  player.getRandom().nextFloat() * 360.0F,
                  0.0F
               );
               mob.getPersistentData().putUUID(UndeadSupportManager.SUMMON_OWNER, player.getUUID());
               if (boosted) {
                  AttributeInstance maxHealth = mob.getAttribute(Attributes.MAX_HEALTH);
                  if (maxHealth != null) {
                     maxHealth.setBaseValue(
                        Math.max(maxHealth.getBaseValue(), player.getAttributeValue(Attributes.MAX_HEALTH) * supportAttributeMultiplier(player))
                     );
                  }

                  AttributeInstance attackDamage = mob.getAttribute(Attributes.ATTACK_DAMAGE);
                  if (attackDamage != null) {
                     attackDamage.setBaseValue(
                        Math.max(attackDamage.getBaseValue(), player.getAttributeValue(Attributes.ATTACK_DAMAGE) * supportAttributeMultiplier(player))
                     );
                  }

                  AttributeInstance movementSpeed = mob.getAttribute(Attributes.MOVEMENT_SPEED);
                  if (movementSpeed != null) {
                     movementSpeed.setBaseValue(
                        Math.max(movementSpeed.getBaseValue(), player.getAttributeValue(Attributes.MOVEMENT_SPEED) * supportAttributeMultiplier(player))
                     );
                  }

                  AttributeInstance armor = mob.getAttribute(Attributes.ARMOR);
                  if (armor != null) {
                     armor.setBaseValue(Math.max(armor.getBaseValue(), player.getAttributeValue(Attributes.ARMOR) * supportAttributeMultiplier(player)));
                  }

                  AttributeInstance toughness = mob.getAttribute(Attributes.ARMOR_TOUGHNESS);
                  if (toughness != null) {
                     toughness.setBaseValue(
                        Math.max(toughness.getBaseValue(), player.getAttributeValue(Attributes.ARMOR_TOUGHNESS) * supportAttributeMultiplier(player))
                     );
                  }

                  mob.setHealth(mob.getMaxHealth());
               }

               if (target != null && target.isAlive() && target != player) {
                  mob.setTarget(target);
               }

               serverLevel.addFreshEntity(mob);
            }
         }
      }
   }

   private static double supportAttributeMultiplier(ServerPlayer player) {
      DfsEquipmentItem.Profile head = DfsEquipmentItem.profile(player.getItemBySlot(EquipmentSlot.HEAD));
      double multiplier = 5.0;
      if (!ModGameRules.areArmorSpecialsEnabled(player)) {
         return multiplier;
      } else {
         if (head != null && head.ability() == DfsEquipmentItem.SpecialAbility.H70_FOLLOWER_BOOST) {
            multiplier *= 2.0;
            notifyEquipmentAbilityTrigger(player, head, ParticleTypes.HAPPY_VILLAGER);
         } else if (head != null && head.ability() == DfsEquipmentItem.SpecialAbility.H70_NIGHT_FOLLOWER_BOOST) {
            multiplier *= 3.5;
            notifyEquipmentAbilityTrigger(player, head, ParticleTypes.ENCHANT);
         }

         return multiplier;
      }
   }

   private static Entity damageSourceEntity(DamageSource source) {
      Entity attacker = source.getDirectEntity();
      return attacker != null ? attacker : source.getEntity();
   }

   private static boolean tryConvertCommanderOrdinaryAttack(LivingIncomingDamageEvent event) {
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
      event.setAmount(0.0F);
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
      Entity player = source.getEntity();
      if (player instanceof Player) {
         return (Player)player;
      } else {
         player = source.getDirectEntity();
         if (player instanceof Player) {
            return (Player)player;
         } else {
            if (source.getDirectEntity() instanceof Projectile projectile) {
               Entity var8 = projectile.getOwner();
               if (var8 instanceof Player) {
                  return (Player)var8;
               }
            }

            return null;
         }
      }
   }

   private static boolean isPlayerSourcedDamage(DamageSource source) {
      if (source.getEntity() instanceof Player || source.getDirectEntity() instanceof Player) {
         return true;
      }
      if (source.getDirectEntity() instanceof Projectile projectile && projectile.getOwner() instanceof Player) {
         return true;
      }
      if (source.getEntity() instanceof com.rzy.dealt_force_skills.entity.BeaconBossEntity) {
         return true;
      }
      return source.getDirectEntity() instanceof Projectile projectile
         && projectile.getOwner() instanceof com.rzy.dealt_force_skills.entity.BeaconBossEntity;
   }

   private static boolean effectApplies(LivingEntity entity, Holder<MobEffect> effect) {
      return entity.hasEffect(effect) && !ModItemEffectHelper.shouldSuppressHarmfulImpact(entity, effect);
   }

   private static void cancelInteraction(PlayerInteractEvent event, InteractionResult result) {
      if (event instanceof LeftClickBlock leftClick) {
         leftClick.setCanceled(true);
         leftClick.setUseBlock(TriState.FALSE);
         leftClick.setUseItem(TriState.FALSE);
      } else if (event instanceof RightClickBlock rightClickBlock) {
         rightClickBlock.setCanceled(true);
         rightClickBlock.setCancellationResult(result);
      } else if (event instanceof RightClickItem rightClickItem) {
         rightClickItem.setCanceled(true);
         rightClickItem.setCancellationResult(result);
      } else if (event instanceof EntityInteract entityInteract) {
         entityInteract.setCanceled(true);
         entityInteract.setCancellationResult(result);
      } else if (event instanceof EntityInteractSpecific entityInteractSpecific) {
         entityInteractSpecific.setCanceled(true);
         entityInteractSpecific.setCancellationResult(result);
      }
   }

   private static MobEffectInstance activeEffectInstance(LivingEntity entity, Holder<MobEffect> effect) {
      MobEffectInstance instance = entity.getEffect(effect);
      return instance != null && !ModItemEffectHelper.shouldSuppressHarmfulImpact(entity, effect) ? instance : null;
   }

   private static int enchantmentLevel(Level level, ResourceKey<Enchantment> enchantment, ItemStack stack) {
      if (level != null && stack != null && !stack.isEmpty()) {
         Holder<Enchantment> holder = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(enchantment);
         return EnchantmentHelper.getItemEnchantmentLevel(holder, stack);
      } else {
         return 0;
      }
   }

   private static boolean suppressesPotionDamage(Player player, DamageSource source) {
      return player.hasEffect(ModEffects.PAIN_RELIEF)
         && (source.is(DamageTypes.MAGIC) || source.is(DamageTypes.INDIRECT_MAGIC) || source.is(DamageTypes.WITHER) || source.is(DamageTypes.STARVE));
   }

   private static boolean shouldSonicShockSkipAction(LivingEntity entity) {
      MobEffectInstance effect = activeEffectInstance(entity, ModEffects.SONIC_SHOCK);
      if (effect == null) {
         return false;
      } else {
         int period = effect.getAmplifier() >= 1 ? 4 : 2;
         return Math.floorMod(entity.tickCount, period) != 0;
      }
   }

   private static boolean shouldMorseShockSkipAction(LivingEntity entity) {
      MobEffectInstance effect = activeEffectInstance(entity, ModEffects.MORSE_STRONG_SHOCK);
      if (effect == null) {
         return false;
      } else {
         int period = effect.getAmplifier() >= 1 ? 4 : 2;
         return Math.floorMod(entity.tickCount, period) != 0;
      }
   }

   private static boolean isExplosionDamage(DamageSource source) {
      return source.is(DamageTypes.EXPLOSION) || source.is(DamageTypes.PLAYER_EXPLOSION);
   }

   private static void tryAwardCoarseSalt(Player player, Level level, BlockPos pos, int fortune) {
      if (player != null && level.getBiome(pos).is(BiomeTags.IS_OCEAN)) {
         float chance = Math.min(1.0F, 0.05F + 0.1F * (float)Math.max(0, fortune));
         if (!(player.getRandom().nextFloat() >= chance)) {
            ItemStack salt = new ItemStack((ItemLike)ModItems.COARSE_SALT.get());
            if (!player.addItem(salt)) {
               player.level().addFreshEntity(new ItemEntity(player.level(), player.getX(), player.getY(), player.getZ(), salt));
            }
         }
      }
   }

   private static void tryAwardPremiumCoffeeBeans(Player player, Level level, BlockState state, int fortune) {
      if (player != null && state.is(Blocks.COCOA) && (Integer)state.getValue(CocoaBlock.AGE) >= 2) {
         int bonusLevel = premiumCoffeeBonusLevel(player, fortune);
         double chance = Math.min(1.0, PREMIUM_COFFEE_DROP_BASE + (double)bonusLevel * PREMIUM_COFFEE_DROP_BONUS_PER_LEVEL);
         if (!(player.getRandom().nextDouble() >= chance)) {
            giveOrDrop(player, new ItemStack((ItemLike)ModItems.PREMIUM_COFFEE_BEANS.get()));
         }
      }
   }

   private static void tryDuplicateNewRecruitMiningDrops(Player player, Level level, BlockState state, BlockPos pos) {
      if (level instanceof ServerLevel serverLevel
         && player != null
         && !player.isCreative()
         && serverLevel.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS)
         && hasEquipmentAbility(player, DfsEquipmentItem.SpecialAbility.NEW_RECRUIT)
         && !(player.getRandom().nextDouble() >= NEW_RECRUIT_MINING_DUPLICATE_CHANCE)) {
         boolean duplicated = false;
         for (ItemStack stack : Block.getDrops(state, serverLevel, pos, serverLevel.getBlockEntity(pos), player, player.getMainHandItem())) {
            if (!stack.isEmpty()) {
               serverLevel.addFreshEntity(
                  new ItemEntity(serverLevel, (double)pos.getX() + 0.5, (double)pos.getY() + 0.5, (double)pos.getZ() + 0.5, stack.copy())
               );
               duplicated = true;
            }
         }

         if (duplicated && player instanceof ServerPlayer serverPlayer) {
            notifyEquipmentAbilityTrigger(
               serverPlayer, equippedProfileWithAbility(player, DfsEquipmentItem.SpecialAbility.NEW_RECRUIT), ParticleTypes.HAPPY_VILLAGER
            );
         }

         return;
      }
   }

   public static void grantHvkBlockLootBonuses(Player player, Level level, BlockPos pos, ItemStack stack) {
      if (level instanceof ServerLevel serverLevel
         && player != null
         && !player.isCreative()
         && stack != null
         && !stack.isEmpty()
         && serverLevel.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS)) {
         if (hasEquipmentAbility(player, DfsEquipmentItem.SpecialAbility.NEW_RECRUIT) && player.getRandom().nextDouble() < NEW_RECRUIT_MINING_DUPLICATE_CHANCE) {
            spawnHvkBlockLootBonus(serverLevel, pos, stack);
            if (player instanceof ServerPlayer serverPlayer) {
               notifyEquipmentAbilityTrigger(
                  serverPlayer, equippedProfileWithAbility(player, DfsEquipmentItem.SpecialAbility.NEW_RECRUIT), ParticleTypes.HAPPY_VILLAGER
               );
            }
         }

         if (player instanceof ServerPlayer
            && UndeadStateManager.isUndead(player)
            && UndeadStateManager.profession(player) == UndeadProfession.EXPLORER
            && serverLevel.getGameRules().getBoolean(ModGameRules.DEALT_UNDEAD_EXTRA_DROP)) {
            spawnHvkBlockLootBonus(serverLevel, pos, stack);
         }

         return;
      }
   }

   private static void spawnHvkBlockLootBonus(ServerLevel level, BlockPos pos, ItemStack stack) {
      if (!stack.isEmpty()) {
         level.addFreshEntity(new ItemEntity(level, (double)pos.getX() + 0.5, (double)pos.getY() + 0.5, (double)pos.getZ() + 0.5, stack.copy()));
      }
   }

   private static void tryDuplicateUndeadExplorerMiningDrops(Player player, Level level, BlockState state, BlockPos pos) {
      if (player instanceof ServerPlayer serverPlayer
         && level instanceof ServerLevel serverLevel
         && !player.isCreative()
         && UndeadStateManager.isUndead(player)
         && UndeadStateManager.profession(player) == UndeadProfession.EXPLORER
         && serverLevel.getGameRules().getBoolean(ModGameRules.DEALT_UNDEAD_EXTRA_DROP)
         && serverLevel.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS)) {
         for (ItemStack stack : Block.getDrops(state, serverLevel, pos, serverLevel.getBlockEntity(pos), serverPlayer, player.getMainHandItem())) {
            if (!stack.isEmpty()) {
               serverLevel.addFreshEntity(
                  new ItemEntity(serverLevel, (double)pos.getX() + 0.5, (double)pos.getY() + 0.5, (double)pos.getZ() + 0.5, stack.copy())
               );
            }
         }

         return;
      }
   }

   private static void tryDuplicateUndeadExplorerContainerLoot(RightClickBlock event) {
      if (event.getEntity() instanceof ServerPlayer player
         && UndeadStateManager.isUndead(player)
         && UndeadStateManager.profession(player) == UndeadProfession.EXPLORER
         && event.getLevel() instanceof ServerLevel level) {
         BlockEntity blockEntity = level.getBlockEntity(event.getPos());
         if (blockEntity instanceof RandomizableContainerBlockEntity && blockEntity instanceof Container) {
            CompoundTag saved = blockEntity.saveWithoutMetadata(level.registryAccess());
            CompoundTag persistent = blockEntity.getPersistentData();
            if (saved.contains("LootTable", 8) && !persistent.getBoolean("dealt_force_skills.undead_explorer_loot_claimed")) {
               persistent.putBoolean("dealt_force_skills.undead_explorer_loot_claimed", true);
               blockEntity.setChanged();
               BlockPos pos = event.getPos().immutable();
               level.getServer().tell(new TickTask(level.getServer().getTickCount() + 1, () -> {
                  if (level.getBlockEntity(pos) instanceof Container container) {
                     for (int slot = 0; slot < container.getContainerSize(); slot++) {
                        ItemStack stack = container.getItem(slot);
                        if (!stack.isEmpty()) {
                           ItemStack copy = stack.copy();
                           if (!player.getInventory().add(copy)) {
                              player.drop(copy, false);
                           }
                        }
                     }
                  }
               }));
               return;
            }

            return;
         }

         return;
      }
   }

   private static boolean hasEquipmentAbility(Player player, DfsEquipmentItem.SpecialAbility ability) {
      if (!ModGameRules.areArmorSpecialsEnabled(player)) {
         return false;
      } else {
         DfsEquipmentItem.Profile chestProfile = DfsEquipmentItem.profile(player.getItemBySlot(EquipmentSlot.CHEST));
         if (chestProfile != null && chestProfile.ability() == ability) {
            return true;
         } else {
            DfsEquipmentItem.Profile headProfile = DfsEquipmentItem.profile(player.getItemBySlot(EquipmentSlot.HEAD));
            return headProfile != null && headProfile.ability() == ability;
         }
      }
   }

   private static DfsEquipmentItem.Profile equippedProfileWithAbility(Player player, DfsEquipmentItem.SpecialAbility ability) {
      DfsEquipmentItem.Profile chestProfile = DfsEquipmentItem.profile(player.getItemBySlot(EquipmentSlot.CHEST));
      if (chestProfile != null && chestProfile.ability() == ability) {
         return chestProfile;
      }

      DfsEquipmentItem.Profile headProfile = DfsEquipmentItem.profile(player.getItemBySlot(EquipmentSlot.HEAD));
      return headProfile != null && headProfile.ability() == ability ? headProfile : null;
   }

   private static void handleFrugalDisassemblyCraft(ItemCraftedEvent event) {
      Player player = event.getEntity();
      if (!player.level().isClientSide) {
         Container inventory = event.getInventory();
         DfsEquipmentItem.Profile triggeredProfile = null;
         if (craftingContains(inventory, (Item)ModItems.SIMPLE_STAB_VEST.get())) {
            giveOrDrop(player, new ItemStack(Items.STRING, 3));
            giveOrDrop(player, new ItemStack(Items.WHITE_WOOL, 3));
            triggeredProfile = DfsEquipmentItem.profile(new ItemStack((ItemLike)ModItems.SIMPLE_STAB_VEST.get()));
         } else if (craftingContains(inventory, (Item)ModItems.UNIVERSAL_TACTICAL_VEST.get())) {
            giveOrDrop(player, new ItemStack(Items.WHITE_WOOL, 2));
            giveOrDrop(player, new ItemStack(Items.STRING, 2));
            triggeredProfile = DfsEquipmentItem.profile(new ItemStack((ItemLike)ModItems.UNIVERSAL_TACTICAL_VEST.get()));
         } else if (craftingContains(inventory, (Item)ModItems.H01_TACTICAL_HELMET.get())) {
            giveOrDrop(player, new ItemStack(Items.STRING, 2));
            giveOrDrop(player, new ItemStack((ItemLike)ModItems.RAW_WOOD_PLANK.get(), 2));
            triggeredProfile = DfsEquipmentItem.profile(new ItemStack((ItemLike)ModItems.H01_TACTICAL_HELMET.get()));
         } else if (craftingContains(inventory, (Item)ModItems.MC_BALLISTIC_HELMET.get())) {
            giveOrDrop(player, new ItemStack(Items.STRING, 2));
            giveOrDrop(player, new ItemStack((ItemLike)ModItems.RAW_WOOD_PLANK.get(), 3));
            triggeredProfile = DfsEquipmentItem.profile(new ItemStack((ItemLike)ModItems.MC_BALLISTIC_HELMET.get()));
         }

         if (triggeredProfile != null && player instanceof ServerPlayer serverPlayer) {
            notifyEquipmentAbilityTrigger(serverPlayer, triggeredProfile, ParticleTypes.HAPPY_VILLAGER);
         }
      }
   }

   private static boolean craftingContains(Container inventory, Item item) {
      if (inventory == null) {
         return false;
      } else {
         for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i).is(item)) {
               return true;
            }
         }

         return false;
      }
   }

   private static MerchantOffer blueprintTradeOffer(Entity trader, RandomSource random) {
      int emeralds = 30 + random.nextInt(35);
      int count = 1 + random.nextInt(2);
      return new MerchantOffer(new ItemCost(Items.EMERALD, emeralds), new ItemStack((ItemLike)ModItems.BLUEPRINT.get(), count), 12, 10, 0.05F);
   }

   private static int premiumCoffeeBonusLevel(Player player, int fortune) {
      int handFortune = Math.max(
         enchantmentLevel(player.level(), Enchantments.FORTUNE, player.getMainHandItem()),
         enchantmentLevel(player.level(), Enchantments.FORTUNE, player.getOffhandItem())
      );
      int luck = Math.max(0, Mth.floor(player.getLuck()));
      return Math.max(Math.max(0, fortune), Math.max(handFortune, luck));
   }

   private static void giveOrDrop(Player player, ItemStack stack) {
      if (!player.addItem(stack)) {
         player.level().addFreshEntity(new ItemEntity(player.level(), player.getX(), player.getY(), player.getZ(), stack));
      }
   }

   private static Vec3 damageSourcePosition(DamageSource source) {
      Entity direct = source.getDirectEntity();
      if (direct != null) {
         return direct.position();
      } else {
         Vec3 impact = source.getSourcePosition();
         if (impact != null) {
            return impact;
         } else {
            Entity attacker = source.getEntity();
            if (attacker instanceof LivingEntity living) {
               return living.position().add(0.0, (double)living.getBbHeight() * 0.5, 0.0);
            } else {
               return attacker != null ? attacker.position().add(0.0, (double)attacker.getBbHeight() * 0.5, 0.0) : null;
            }
         }
      }
   }

   private static record GhrothDamageFloor(UUID attackerId, float amount, long expiresAt, boolean starsBonus) {
   }

   private static record GhrothNoonPendingCopy(int targetId, float damage, int copiesRemaining) {
   }

   private static record GhrothStarsPendingShot(int targetId, float damage, long expiresAt) {
   }

   private static record LockedLook(float yaw, float pitch) {
   }

   private static record TaczAmmoSnapshot(String gunId, int ammoCount, boolean bulletInBarrel, long expiresAt) {
   }

   private static record TaczSpeedMultipliers(double fireRate, double reload, double aim, double bolt, double aimPenalty) {
      private TaczSpeedMultipliers {
         fireRate = validMultiplier(fireRate);
         reload = validMultiplier(reload);
         aim = validMultiplier(aim);
         bolt = validMultiplier(bolt);
         aimPenalty = validMultiplier(aimPenalty);
      }

      private static double validMultiplier(double value) {
         return Double.isFinite(value) && value >= 1.0 ? value : 1.0;
      }

      boolean hasChange() {
         return this.fireRate > 1.0001 || this.reload > 1.0001 || this.aim > 1.0001 || this.bolt > 1.0001 || this.aimPenalty > 1.0001;
      }
   }

   private static record TrackedEffect(int duration, int amplifier) {
   }
}
