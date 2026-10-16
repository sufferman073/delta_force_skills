package com.rzy.dealt_force_skills.character.manba;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.advancement.DfsAchievements;
import com.rzy.dealt_force_skills.character.CharacterSelectionManager;
import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.character.sineva.SinevaKnockdownState;
import com.rzy.dealt_force_skills.compat.PlayerReviveCompat;
import com.rzy.dealt_force_skills.effect.ManbaBlindedEffect;
import com.rzy.dealt_force_skills.effect.StunEffect;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.S2C_ManbaDuelView;
import com.rzy.dealt_force_skills.network.S2C_ManbaFlashlightBeam;
import com.rzy.dealt_force_skills.network.S2C_ManbaFlashlightProgress;
import com.rzy.dealt_force_skills.network.S2C_ManbaOpportunityMarkers;
import com.rzy.dealt_force_skills.network.S2C_SyncManbaState;
import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.skill.SkillCooldownHelper;
import com.rzy.dealt_force_skills.skill.SkillDamageHelper;
import com.rzy.dealt_force_skills.skill.SkillAnimationScheduler;
import com.rzy.dealt_force_skills.skill.SkillModelVisual;
import com.rzy.dealt_force_skills.skill.SkillModelVisualSync;
import com.rzy.dealt_force_skills.util.RangedSoundHelper;
import com.rzy.dealt_force_skills.util.TargetingUtil;
import net.minecraft.core.Holder;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class ManbaStateManager {
    public static volatile int ELBOW_MAX_CHARGES = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("ELBOW_MAX_CHARGES", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.manba.manba_state_manager.elbow_max_charges", 6));
    public static volatile int ELBOW_RECHARGE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("ELBOW_RECHARGE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.manba.manba_state_manager.elbow_recharge_ticks", 120));
    public static final int CORE_COOLDOWN_TICKS = Math.max(90 * 20,
            com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.manba.manba_state_manager.core_cooldown_ticks", 90 * 20));
    public static volatile int DUEL_DURATION_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("DUEL_DURATION_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.manba.manba_state_manager.duel_duration_ticks", 3660));
    public static volatile int DUEL_STALE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("DUEL_STALE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.manba.manba_state_manager.duel_stale_ticks", 600));
    private static volatile long DUEL_AFFECTION_DECAY_INTERVAL_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("DUEL_AFFECTION_DECAY_INTERVAL_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.longValue(
      "characters.manba.manba_state_manager.duel_affection_decay_interval_ticks", 40L
   ));
    private static volatile int DUEL_DAMAGE_AFFECTION_GAIN = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("DUEL_DAMAGE_AFFECTION_GAIN", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.manba.manba_state_manager.duel_damage_affection_gain", 2));
    private static volatile int DUEL_CONTROL_AFFECTION_COOLDOWN_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("DUEL_CONTROL_AFFECTION_COOLDOWN_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue(
      "characters.manba.manba_state_manager.duel_control_affection_cooldown_ticks", 20
   ));
    public static final ResourceLocation BASE_SLOW_UUID = ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "manba_state_manager.base_slow");
    public static final ResourceLocation LIGHT_WARRIOR_SPEED_UUID = ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "manba_state_manager.light_warrior_speed");
    public static final ResourceLocation UNYIELDING_SPEED_UUID = ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "manba_state_manager.unyielding_speed");
    public static final ResourceLocation DAMAGE_SPEED_UUID = ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "manba_state_manager.damage_speed");

    private static final String ROOT_TAG = DealtForceSkillsMod.MODID + ".manba";
    private static final String INITIALIZED = "Initialized";
    private static final String CONFIGURED = "Configured";
    private static final String TALENTS = "Talents";
    private static final String BULBS = "Bulbs";
    private static final String LENS = "Lens";
    private static final String BATTERIES = "Batteries";
    private static final String ELBOW_CHARGES = "ElbowCharges";
    private static final String ELBOW_NEXT_RECHARGE = "ElbowNextRecharge";
    private static final String FLASHLIGHT_DURABILITY = "FlashlightDurability";
    private static final String FLASHLIGHT_ACTIVE = "FlashlightActive";
    private static final String CORE_COOLDOWN_UNTIL = "CoreCooldownUntil";
    private static final String DUEL_TARGET = "DuelTarget";
    private static final String DUEL_END_TICK = "DuelEndTick";
    private static final String DUEL_AFFECTION = "DuelAffection";
    private static final String DUEL_LAST_CHANGE = "DuelLastChange";
    private static final String DUEL_NEXT_DECAY = "DuelNextDecay";
    private static final String DUEL_REVIVES_USED = "DuelRevivesUsed";
    private static final String STEEL_COOLDOWN_UNTIL = "SteelCooldownUntil";
    private static final String INDESTRUCTIBLE_COOLDOWN_UNTIL = "IndestructibleCooldownUntil";
    private static final String GOOD_REWARD_ACTIVE = "GoodRewardActive";
    private static final String SELF_TAUGHT_STACKS = "SelfTaughtStacks";
    private static final String SNEAK_TICKS = "SneakTicks";
    private static final String DURABLE_NEXT_TICK = "DurableNextTick";
    private static final String DIVERSION_VIEW_TICKS = "DiversionViewTicks";
    private static final String DIVERSION_READY = "DiversionReady";
    private static final String DIVERSION_LAST_HURT = "DiversionLastHurt";
    private static final String DAMAGE_SPEED_UNTIL = "DamageSpeedUntil";

    private static final int FLASHLIGHT_SCALE = 100;
    private static volatile int FLASHLIGHT_BLIND_COST = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("FLASHLIGHT_BLIND_COST", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.manba.manba_state_manager.flashlight_blind_cost", 1000));
    private static volatile int FLASHLIGHT_FULL_PROGRESS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("FLASHLIGHT_FULL_PROGRESS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.manba.manba_state_manager.flashlight_full_progress", 1000));
    private static volatile int FLASHLIGHT_BEAM_SYNC_INTERVAL_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("FLASHLIGHT_BEAM_SYNC_INTERVAL_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue(
      "characters.manba.manba_state_manager.flashlight_beam_sync_interval_ticks", 2
   ));
    private static volatile int FLASHLIGHT_BEAM_TTL_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("FLASHLIGHT_BEAM_TTL_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.manba.manba_state_manager.flashlight_beam_ttl_ticks", 6));
    private static volatile int FLASHLIGHT_LIGHT_INTERVAL_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("FLASHLIGHT_LIGHT_INTERVAL_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue(
      "characters.manba.manba_state_manager.flashlight_light_interval_ticks", 4
   ));
    private static volatile int FLASHLIGHT_LIGHT_LEVEL = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("FLASHLIGHT_LIGHT_LEVEL", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.manba.manba_state_manager.flashlight_light_level", 15));
    private static volatile int FLASHLIGHT_MAX_LIGHT_BLOCKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("FLASHLIGHT_MAX_LIGHT_BLOCKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.manba.manba_state_manager.flashlight_max_light_blocks", 120));
    private static volatile int STEEL_BODY_MIN_COOLDOWN_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("STEEL_BODY_MIN_COOLDOWN_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.manba.manba_state_manager.steel_body_min_cooldown_ticks", 60));
    private static volatile double DUEL_LOCK_RANGE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("DUEL_LOCK_RANGE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("characters.manba.manba_state_manager.duel_lock_range", 32.0));
    private static volatile double DUEL_BGM_TRACK_RADIUS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("DUEL_BGM_TRACK_RADIUS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("characters.manba.manba_state_manager.duel_bgm_track_radius", 96.0));
    private static volatile double DAMAGE_DECAY_START_DISTANCE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("DAMAGE_DECAY_START_DISTANCE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue(
      "characters.manba.manba_state_manager.damage_decay_start_distance", 4.0
   ));
    private static volatile double DAMAGE_DECAY_PER_BLOCK = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("DAMAGE_DECAY_PER_BLOCK", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("characters.manba.manba_state_manager.damage_decay_per_block", 0.12));
    private static volatile double ELBOW_RANGE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("ELBOW_RANGE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("characters.manba.manba_state_manager.elbow_range", 3.2));
    private static volatile double ELBOW_FRONT_DOT = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("ELBOW_FRONT_DOT", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue(
      "characters.manba.manba_state_manager.elbow_front_dot", Math.cos(Math.toRadians(65.0))
   ));
    private static volatile double ELBOW_KNOCKBACK_HORIZONTAL = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("ELBOW_KNOCKBACK_HORIZONTAL", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue(
      "characters.manba.manba_state_manager.elbow_knockback_horizontal", 2.65
   ));
    private static volatile double ELBOW_KNOCKBACK_VERTICAL = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("ELBOW_KNOCKBACK_VERTICAL", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("characters.manba.manba_state_manager.elbow_knockback_vertical", 0.42));
    private static volatile int ELBOW_KNOCKBACK_STUN_GRACE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("ELBOW_KNOCKBACK_STUN_GRACE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue(
      "characters.manba.manba_state_manager.elbow_knockback_stun_grace_ticks", 6
   ));
    private static volatile double BRAVE_DASH_RANGE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("BRAVE_DASH_RANGE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("characters.manba.manba_state_manager.brave_dash_range", 4.25));
    private static volatile double FLASHLIGHT_TARGET_LOOK_DOT = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("FLASHLIGHT_TARGET_LOOK_DOT", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue(
      "characters.manba.manba_state_manager.flashlight_target_look_dot", Math.cos(Math.toRadians(85.0))
   ));
    public static volatile double OPPORTUNITY_WINDOW_RANGE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("OPPORTUNITY_WINDOW_RANGE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("characters.manba.manba_state_manager.opportunity_window_range", 45.0));
    public static volatile int OPPORTUNITY_WINDOW_SYNC_INTERVAL_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("OPPORTUNITY_WINDOW_SYNC_INTERVAL_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue(
      "characters.manba.manba_state_manager.opportunity_window_sync_interval_ticks", 10
   ));
    private static volatile int OPPORTUNITY_WINDOW_MAX_MARKERS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("OPPORTUNITY_WINDOW_MAX_MARKERS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue(
      "characters.manba.manba_state_manager.opportunity_window_max_markers", 256
   ));
    private static final Map<UUID, Map<UUID, FlashProgress>> FLASH_PROGRESS = new HashMap<>();
    private static final Map<UUID, Set<BlockPos>> FLASHLIGHT_LIGHT_BLOCKS = new HashMap<>();
    private static final Map<UUID, UUID> DUEL_TARGET_TO_OWNER = new HashMap<>();
    private static final Map<UUID, WeakReference<LivingEntity>> DUEL_TARGET_ENTITIES = new HashMap<>();
    private static final Map<UUID, Set<UUID>> DUEL_BGM_LISTENERS = new HashMap<>();
    private static final Map<ControlAffectionKey, Long> DUEL_CONTROL_AFFECTION_READY_AT = new HashMap<>();
    private static final Set<UUID> EXECUTING_DUEL_TARGETS = new HashSet<>();
    private static final ResourceLocation DUEL_BGM_SOUND_FILE =
            ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "manba/duel_bgm");

    private ManbaStateManager() {
    }

    public static boolean isManba(Player player) {
        Optional<String> selected = CharacterSelectionManager.getSelectedCharacterId(player);
        return selected.isPresent() && ModCharacters.MANBA_ID.equals(selected.get());
    }

    public static void initializeIfNeeded(ServerPlayer player) {
        if (!isManba(player)) {
            return;
        }

        CompoundTag tag = data(player);
        if (!tag.getBoolean(INITIALIZED)) {
            tag.putBoolean(INITIALIZED, true);
            tag.putBoolean(CONFIGURED, false);
            writeOrdinals(tag, TALENTS, defaultTalents());
            writeOrdinals(tag, BULBS, Set.of(ManbaBulb.GREEN_BULB, ManbaBulb.PURPLE_BULB));
            tag.putInt(LENS, ManbaLens.PURPLE_LENS.ordinal());
            writeOrdinals(tag, BATTERIES, Set.of(ManbaBattery.YELLOW_BATTERY, ManbaBattery.WHITE_BATTERY));
            tag.putInt(ELBOW_CHARGES, ELBOW_MAX_CHARGES);
            tag.putLong(ELBOW_NEXT_RECHARGE, 0L);
            tag.putLong(CORE_COOLDOWN_UNTIL, 0L);
            tag.putInt(DUEL_AFFECTION, 0);
            tag.putInt(SELF_TAUGHT_STACKS, 0);
        }

        int maxDurability = flashlightStats(player).maxDurabilityScaled();
        if (!tag.contains(FLASHLIGHT_DURABILITY, Tag.TAG_INT)) {
            tag.putInt(FLASHLIGHT_DURABILITY, maxDurability);
        } else if (tag.getInt(FLASHLIGHT_DURABILITY) > maxDurability) {
            tag.putInt(FLASHLIGHT_DURABILITY, maxDurability);
        }
    }

    public static void copyState(Player original, Player target) {
        CompoundTag originalData = original.getPersistentData();
        if (originalData.contains(ROOT_TAG, Tag.TAG_COMPOUND)) {
            target.getPersistentData().put(ROOT_TAG, originalData.getCompound(ROOT_TAG).copy());
        }
    }

    public static void clearState(Player player) {
        removeTransientModifiers(player);
        FLASH_PROGRESS.remove(player.getUUID());
        if (player instanceof ServerPlayer serverPlayer) {
            clearFlashlightLightBlocks(serverPlayer);
        }
        if (player.getPersistentData().contains(ROOT_TAG, Tag.TAG_COMPOUND)) {
            CompoundTag tag = data(player);
            UUID targetId = tag.hasUUID(DUEL_TARGET) ? tag.getUUID(DUEL_TARGET) : null;
            if (player instanceof ServerPlayer serverPlayer) {
                LivingEntity target = targetId == null ? null : duelTarget(serverPlayer, targetId);
                stopDuelBgm(serverPlayer, target, targetId);
            }
            if (tag.hasUUID(DUEL_TARGET)) {
                DUEL_TARGET_TO_OWNER.remove(tag.getUUID(DUEL_TARGET));
            }
        }
        DUEL_TARGET_ENTITIES.remove(player.getUUID());
        DUEL_BGM_LISTENERS.remove(player.getUUID());
        clearControlAffectionCooldowns(player.getUUID());
        player.getPersistentData().remove(ROOT_TAG);
    }

    public static void removeTransientModifiers(Player player) {
        var attr = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attr == null) {
            return;
        }
        if (attr.getModifier(BASE_SLOW_UUID) != null) {
            attr.removeModifier(BASE_SLOW_UUID);
        }
        if (attr.getModifier(LIGHT_WARRIOR_SPEED_UUID) != null) {
            attr.removeModifier(LIGHT_WARRIOR_SPEED_UUID);
        }
        if (attr.getModifier(UNYIELDING_SPEED_UUID) != null) {
            attr.removeModifier(UNYIELDING_SPEED_UUID);
        }
        if (attr.getModifier(DAMAGE_SPEED_UUID) != null) {
            attr.removeModifier(DAMAGE_SPEED_UUID);
        }
    }

    public static void clearRuntimeOnDeath(ServerPlayer player) {
        endDuel(player, false, false);
        setFlashlightActive(player, false);
        FLASH_PROGRESS.remove(player.getUUID());
        clearFlashlightLightBlocks(player);
        data(player).putInt(SNEAK_TICKS, 0);
        removeTransientModifiers(player);
        syncToClient(player);
    }

    public static void clearRuntimeOnLogout(ServerPlayer player) {
        if (player == null) {
            return;
        }
        UUID playerId = player.getUUID();
        FLASH_PROGRESS.remove(playerId);
        removeTransientModifiers(player);
        clearFlashlightLightBlocks(player);
        if (player.getPersistentData().contains(ROOT_TAG, Tag.TAG_COMPOUND)) {
            endDuel(player, false, false);
            setFlashlightActive(player, false);
        }
        DUEL_TARGET_TO_OWNER.entrySet().removeIf(entry -> entry.getKey().equals(playerId) || entry.getValue().equals(playerId));
        DUEL_TARGET_ENTITIES.entrySet().removeIf(entry -> {
            LivingEntity target = entry.getValue() == null ? null : entry.getValue().get();
            return entry.getKey().equals(playerId)
                    || target == null
                    || target.getUUID().equals(playerId)
                    || target.isRemoved();
        });
        DUEL_BGM_LISTENERS.remove(playerId);
        DUEL_BGM_LISTENERS.values().removeIf(listeners -> {
            listeners.remove(playerId);
            return listeners.isEmpty();
        });
        EXECUTING_DUEL_TARGETS.remove(playerId);
        clearControlAffectionCooldowns(playerId);
    }

    public static void clearTransientRuntimeCaches() {
        FLASH_PROGRESS.clear();
        FLASHLIGHT_LIGHT_BLOCKS.clear();
        DUEL_TARGET_TO_OWNER.clear();
        DUEL_TARGET_ENTITIES.clear();
        DUEL_BGM_LISTENERS.clear();
        DUEL_CONTROL_AFFECTION_READY_AT.clear();
        EXECUTING_DUEL_TARGETS.clear();
    }

    public static void tick(ServerPlayer player) {
        if (!isManba(player)) {
            removeTransientModifiers(player);
            return;
        }

        initializeIfNeeded(player);
        long now = SkillCooldownHelper.now(player);
        applyMovementModifiers(player);
        rechargeElbow(player, now);
        tickFlashlight(player, now);
        tickDuel(player, now);
        tickDurableTalent(player, now);
        tickBraveForward(player);
        tickDiversionWatch(player, now);
        syncOpportunityWindow(player, now);
    }

    public static boolean configure(ServerPlayer player, int[] talentOrdinals, int[] bulbOrdinals,
                                    int lensOrdinal, int[] batteryOrdinals) {
        if (!isManba(player)) {
            return false;
        }
        initializeIfNeeded(player);
        EnumSet<ManbaTalent> talents = decodeSet(ManbaTalent.class, talentOrdinals);
        EnumSet<ManbaBulb> bulbs = decodeSet(ManbaBulb.class, bulbOrdinals);
        EnumSet<ManbaBattery> batteries = decodeSet(ManbaBattery.class, batteryOrdinals);
        ManbaLens[] lenses = ManbaLens.values();
        if (talents.size() != 4 || bulbs.size() != 2 || batteries.size() != 2
                || lensOrdinal < 0 || lensOrdinal >= lenses.length) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.manba.loadout_invalid"), true);
            return false;
        }

        CompoundTag tag = data(player);
        writeOrdinals(tag, TALENTS, talents);
        writeOrdinals(tag, BULBS, bulbs);
        tag.putInt(LENS, lensOrdinal);
        writeOrdinals(tag, BATTERIES, batteries);
        tag.putBoolean(CONFIGURED, true);
        tag.putInt(FLASHLIGHT_DURABILITY,
                Math.min(tag.getInt(FLASHLIGHT_DURABILITY), flashlightStats(player).maxDurabilityScaled()));
        player.displayClientMessage(Component.translatable("message.dealt_force_skills.manba.loadout_saved"), true);
        syncToClient(player);
        return true;
    }

    public static boolean isConfigured(Player player) {
        return data(player).getBoolean(CONFIGURED);
    }

    public static boolean hasTalent(Player player, ManbaTalent talent) {
        return selectedTalents(player).contains(talent);
    }

    public static EnumSet<ManbaTalent> selectedTalents(Player player) {
        return readEnumSet(data(player), TALENTS, ManbaTalent.class, defaultTalents());
    }

    public static EnumSet<ManbaBulb> selectedBulbs(Player player) {
        return readEnumSet(data(player), BULBS, ManbaBulb.class,
                EnumSet.of(ManbaBulb.GREEN_BULB, ManbaBulb.PURPLE_BULB));
    }

    public static ManbaLens selectedLens(Player player) {
        int ordinal = data(player).getInt(LENS);
        ManbaLens[] lenses = ManbaLens.values();
        return ordinal >= 0 && ordinal < lenses.length ? lenses[ordinal] : ManbaLens.PURPLE_LENS;
    }

    public static EnumSet<ManbaBattery> selectedBatteries(Player player) {
        return readEnumSet(data(player), BATTERIES, ManbaBattery.class,
                EnumSet.of(ManbaBattery.YELLOW_BATTERY, ManbaBattery.WHITE_BATTERY));
    }

    public static ManbaFlashlightStats flashlightStats(Player player) {
        return ManbaFlashlightStats.of(selectedBulbs(player), selectedLens(player), selectedBatteries(player));
    }

    public static int elbowCharges(Player player) {
        return Math.min(ELBOW_MAX_CHARGES, data(player).getInt(ELBOW_CHARGES));
    }

    public static int elbowRechargeRemainingTicks(Player player) {
        if (elbowCharges(player) >= ELBOW_MAX_CHARGES) {
            return 0;
        }
        return remainingTicks(player, ELBOW_NEXT_RECHARGE);
    }

    public static boolean consumeElbow(ServerPlayer player) {
        CompoundTag tag = data(player);
        int charges = elbowCharges(player);
        if (charges <= 0) {
            return false;
        }
        tag.putInt(ELBOW_CHARGES, charges - 1);
        if (charges == ELBOW_MAX_CHARGES) {
            tag.putLong(ELBOW_NEXT_RECHARGE,
                    SkillCooldownHelper.until(player, SkillCooldownHelper.now(player), ELBOW_RECHARGE_TICKS));
        }
        return true;
    }

    public static void useElbow(ServerPlayer player) {
        if (!consumeElbow(player)) {
            com.rzy.dealt_force_skills.skill.SkillCooldownHelper.notifyCooldown(player, Component.translatable("message.dealt_force_skills.manba.elbow_empty"));
            return;
        }

        player.level().playSound(null, player.blockPosition(), ModSounds.MANBA_ELBOW_START.get(),
                SoundSource.PLAYERS, 0.8f, 1.0f);
        SkillModelVisualSync.play(player, SkillModelVisual.MANBA_ELBOW);
        SkillAnimationScheduler.schedule(
                player,
                SkillModelVisual.MANBA_ELBOW.impactTick(),
                ManbaStateManager::performElbowImpact);
    }

    private static void performElbowImpact(ServerPlayer player) {
        Vec3 eye = player.getEyePosition();
        Vec3 forward = player.getLookAngle().normalize();
        AABB box = player.getBoundingBox().inflate(ELBOW_RANGE, 1.0D, ELBOW_RANGE);
        for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class, box,
                entity -> TargetingUtil.isHostileLivingFor(player, entity))) {
            Vec3 center = target.position().add(0.0D, target.getBbHeight() * 0.55D, 0.0D);
            Vec3 toTarget = center.subtract(eye);
            if (toTarget.length() > ELBOW_RANGE || toTarget.lengthSqr() < 0.0001D) {
                continue;
            }
            if (forward.dot(toTarget.normalize()) < ELBOW_FRONT_DOT) {
                continue;
            }

            SkillDamageHelper.hurt(target,
                    SkillDamageHelper.manbaElbow(player.serverLevel(), null, player),
                    player,
                    com.rzy.dealt_force_skills.config.DealtForceConfig.floatValue("characters.manba.manba_state_manager.skill_hurt.0.damage", 2.4f));
            StunEffect.allowHorizontalMovement(target, ELBOW_KNOCKBACK_STUN_GRACE_TICKS);
            target.addEffect(new MobEffectInstance(ModEffects.STUN, com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.manba.manba_state_manager.effect.stun.0.duration_ticks", 48), com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.manba.manba_state_manager.effect.stun.0.amplifier", 0), false, true, true), player);
            SinevaKnockdownState.apply(player, target, 48);
            applyElbowKnockback(player, target, forward);
            player.level().playSound(null, target.blockPosition(), ModSounds.MANBA_ELBOW_HIT.get(),
                    SoundSource.PLAYERS, 0.9f, 1.0f);
        }
    }

    private static void applyElbowKnockback(ServerPlayer player, LivingEntity target, Vec3 fallbackDirection) {
        Vec3 away = target.position().subtract(player.position());
        Vec3 knock = new Vec3(away.x, 0.0D, away.z);
        if (knock.lengthSqr() < 0.001D) {
            knock = new Vec3(fallbackDirection.x, 0.0D, fallbackDirection.z);
        }
        if (knock.lengthSqr() < 0.001D) {
            return;
        }
        Vec3 current = target.getDeltaMovement();
        target.setDeltaMovement(knock.normalize().scale(ELBOW_KNOCKBACK_HORIZONTAL)
                .add(0.0D, Math.max(current.y, ELBOW_KNOCKBACK_VERTICAL), 0.0D));
        target.hurtMarked = true;
    }

    public static void setFlashlightActive(ServerPlayer player, boolean active) {
        if (!isManba(player)) {
            return;
        }
        initializeIfNeeded(player);
        if (active && data(player).getInt(FLASHLIGHT_DURABILITY) <= 0) {
            com.rzy.dealt_force_skills.skill.SkillCooldownHelper.notifyCooldown(player, Component.translatable("message.dealt_force_skills.manba.flashlight_empty"));
            active = false;
        }
        boolean changed = data(player).getBoolean(FLASHLIGHT_ACTIVE) != active;
        data(player).putBoolean(FLASHLIGHT_ACTIVE, active);
        if (changed) {
            SkillModelVisualSync.play(player, SkillModelVisual.MANBA_FLASHLIGHT_TOGGLE);
        }
        if (!active) {
            clearFlashProgress(player);
            clearFlashlightLightBlocks(player);
        }
        syncToClient(player);
        syncFlashlightBeam(player, data(player).getBoolean(FLASHLIGHT_ACTIVE), flashlightStats(player));
    }

    public static boolean isFlashlightActive(Player player) {
        return data(player).getBoolean(FLASHLIGHT_ACTIVE);
    }

    public static int flashlightDurability(Player player) {
        return data(player).getInt(FLASHLIGHT_DURABILITY);
    }

    public static int coreCooldownRemainingTicks(Player player) {
        return remainingTicks(player, CORE_COOLDOWN_UNTIL);
    }

    public static boolean coreReady(Player player) {
        return coreCooldownRemainingTicks(player) <= 0 && !isDuelActive(player);
    }

    public static boolean isDuelActive(Player player) {
        return data(player).hasUUID(DUEL_TARGET) && data(player).getLong(DUEL_END_TICK) > SkillCooldownHelper.now(player);
    }

    public static int duelAffection(Player player) {
        return data(player).getInt(DUEL_AFFECTION);
    }

    public static int duelRemainingTicks(Player player) {
        return remainingTicks(player, DUEL_END_TICK);
    }

    public static boolean startDuel(ServerPlayer player, int targetEntityId) {
        if (!coreReady(player)) {
            SkillCooldownHelper.notifyCooldown(player,
                    Component.translatable("message.dealt_force_skills.manba.core_cooldown"));
            return true;
        }
        Entity entity = player.level().getEntity(targetEntityId);
        if (!(entity instanceof LivingEntity target)
                || target == player
                || !TargetingUtil.isHostileLivingFor(player, target)
                || target.distanceToSqr(player) > DUEL_LOCK_RANGE * DUEL_LOCK_RANGE
                || !player.hasLineOfSight(target)) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.manba.no_duel_target"), true);
            return true;
        }

        long now = SkillCooldownHelper.now(player);
        CompoundTag tag = data(player);
        tag.putUUID(DUEL_TARGET, target.getUUID());
        tag.putLong(DUEL_END_TICK, now + DUEL_DURATION_TICKS);
        tag.putInt(DUEL_AFFECTION, 20);
        tag.putLong(DUEL_LAST_CHANGE, now);
        tag.putLong(DUEL_NEXT_DECAY, now + DUEL_AFFECTION_DECAY_INTERVAL_TICKS);
        tag.putInt(DUEL_REVIVES_USED, 0);
        DUEL_TARGET_TO_OWNER.put(target.getUUID(), player.getUUID());
        DUEL_TARGET_ENTITIES.put(player.getUUID(), new WeakReference<>(target));
        target.setGlowingTag(true);
        player.level().playSound(null, player.blockPosition(), ModSounds.MANBA_DUEL_BGM.get(),
                SoundSource.PLAYERS, 0.8f, 1.0f);
        rememberDuelBgmListeners(player, target);
        player.displayClientMessage(Component.translatable("message.dealt_force_skills.manba.duel_started",
                target.getDisplayName()), true);
        syncToClient(player);
        syncDuelView(player, target, false);
        return true;
    }

    public static void cancelDuel(ServerPlayer player) {
        endDuel(player, false, true);
    }

    public static void addAffectionForControlledTarget(LivingEntity target, MobEffectInstance effectInstance) {
        if (target.level().isClientSide || effectInstance == null
                || !isControlOrNegativeEffect(effectInstance.getEffect())
                || isDuelDownedEffect(effectInstance.getEffect())) {
            return;
        }
        UUID ownerId = DUEL_TARGET_TO_OWNER.get(target.getUUID());
        if (ownerId == null || !(target.level() instanceof ServerLevel level)) {
            return;
        }
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ownerId);
        if (owner == null || !isManba(owner) || !isDuelActive(owner)) {
            return;
        }
        if (target instanceof Player targetPlayer && PlayerReviveCompat.isBleeding(targetPlayer)) {
            return;
        }
        LivingEntity currentTarget = duelTarget(owner);
        if (currentTarget == null || !currentTarget.getUUID().equals(target.getUUID())
                || !consumeControlAffectionCooldown(owner, target)) {
            return;
        }
        addDuelAffection(owner, 10);
        SoundEvent sound = randomControlSound(owner);
        owner.level().playSound(null, owner.blockPosition(), sound, SoundSource.PLAYERS, 0.75f, 1.0f);
        if (target instanceof ServerPlayer targetPlayer) {
            targetPlayer.level().playSound(null, targetPlayer.blockPosition(), sound, SoundSource.PLAYERS, 0.55f, 0.9f);
        }
    }

    public static void addAffectionForDamagedTarget(LivingEntity target, DamageSource source, float amount) {
        if (target.level().isClientSide || source == null || amount <= 0.0f) {
            return;
        }
        UUID ownerId = DUEL_TARGET_TO_OWNER.get(target.getUUID());
        if (ownerId == null || !(target.level() instanceof ServerLevel level)) {
            return;
        }
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ownerId);
        if (owner == null || !isManba(owner) || !isDuelActive(owner)) {
            return;
        }
        if (!sameUuid(source.getEntity(), ownerId) && !sameUuid(source.getDirectEntity(), ownerId)) {
            return;
        }
        LivingEntity currentTarget = duelTarget(owner);
        if (currentTarget == null || !currentTarget.getUUID().equals(target.getUUID())) {
            return;
        }
        addDuelAffection(owner, DUEL_DAMAGE_AFFECTION_GAIN);
    }

    public static boolean shouldIgnoreIncomingDamage(Player player, DamageSource source) {
        return isManba(player)
                && !isExecutingDuelTarget(player)
                && isDuelActive(player)
                && !isAllowedDuelIncomingDamage(player, source);
    }

    public static boolean trySteelBody(ServerPlayer player, DamageSource source, float amount) {
        if (!isManba(player) || isExecutingDuelTarget(player)
                || amount <= 0.0f || !hasTalent(player, ManbaTalent.STEEL_BODY)) {
            return false;
        }
        long now = SkillCooldownHelper.now(player);
        CompoundTag tag = data(player);
        if (now < tag.getLong(STEEL_COOLDOWN_UNTIL)) {
            return false;
        }
        int cooldownTicks = Math.max(STEEL_BODY_MIN_COOLDOWN_TICKS, SkillCooldownHelper.ticks(player, 60 * 20));
        tag.putLong(STEEL_COOLDOWN_UNTIL, now + cooldownTicks);
        player.invulnerableTime = Math.max(player.invulnerableTime, 10);
        player.level().playSound(null, player.blockPosition(), ModSounds.SHIELD_BASH.get(),
                SoundSource.PLAYERS, 0.8f, 0.75f);
        syncToClient(player);
        return true;
    }

    public static boolean tryTriggerDiversion(ServerPlayer player, DamageSource source) {
        if (!isManba(player) || !hasTalent(player, ManbaTalent.DIVERSION) || !data(player).getBoolean(DIVERSION_READY)) {
            recordDiversionHurt(player);
            return false;
        }
        Entity attacker = source.getEntity();
        if (!(attacker instanceof LivingEntity living) || attacker == player) {
            recordDiversionHurt(player);
            return false;
        }

        Vec3 back = new Vec3(living.getLookAngle().x, 0.0D, living.getLookAngle().z);
        if (back.lengthSqr() < 0.001D) {
            back = living.position().subtract(player.position());
        }
        back = back.lengthSqr() < 0.001D ? new Vec3(0.0D, 0.0D, 1.0D) : back.normalize();
        Vec3 pos = living.position().subtract(back.scale(1.25D));
        player.teleportTo(pos.x, living.getY(), pos.z);
        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.manba.manba_state_manager.effect.invisibility.1.duration_ticks", 3 * 20), com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.manba.manba_state_manager.effect.invisibility.1.amplifier", 0), false, false, false));
        data(player).putBoolean(DIVERSION_READY, false);
        data(player).putInt(DIVERSION_VIEW_TICKS, 0);
        recordDiversionHurt(player);
        syncToClient(player);
        return false;
    }

    public static boolean tryPreventFatalDamage(ServerPlayer player, DamageSource source, float amount) {
        if (!isManba(player) || isExecutingDuelTarget(player) || amount < player.getHealth()) {
            return false;
        }
        CompoundTag tag = data(player);
        if (tryDuelRevive(player, tag)) {
            return true;
        }
        if (hasTalent(player, ManbaTalent.GOOD_REWARD) && tag.getBoolean(GOOD_REWARD_ACTIVE)) {
            tag.putBoolean(GOOD_REWARD_ACTIVE, false);
            player.setHealth(Math.max(1.0f, player.getMaxHealth() * 0.5f));
            DfsAchievements.recordFatalAvoidance(player);
            syncToClient(player);
            return true;
        }
        if (hasTalent(player, ManbaTalent.INDESTRUCTIBLE)) {
            long now = SkillCooldownHelper.now(player);
            if (now >= tag.getLong(INDESTRUCTIBLE_COOLDOWN_UNTIL)) {
                tag.putLong(INDESTRUCTIBLE_COOLDOWN_UNTIL, SkillCooldownHelper.until(player, now, 120 * 20));
                player.setHealth(player.getMaxHealth());
                DfsAchievements.recordFatalAvoidance(player);
                player.level().playSound(null, player.blockPosition(), ModSounds.MANBA_DUEL_REVIVE.get(),
                        SoundSource.PLAYERS, 0.85f, 1.15f);
                syncToClient(player);
                return true;
            }
        }
        return false;
    }

    public static void recordDuelDeathAchievement(ServerPlayer player, DamageSource source) {
        if (!isManba(player) || !isDuelActive(player) || source == null) {
            return;
        }
        CompoundTag tag = data(player);
        if (!tag.hasUUID(DUEL_TARGET)) {
            return;
        }
        UUID targetId = tag.getUUID(DUEL_TARGET);
        Entity attacker = source.getEntity();
        ServerPlayer killer = attacker instanceof ServerPlayer attackerPlayer
                && attackerPlayer.getUUID().equals(targetId) ? attackerPlayer : null;
        if (killer == null) {
            Entity direct = source.getDirectEntity();
            if (direct instanceof ServerPlayer directPlayer && directPlayer.getUUID().equals(targetId)) {
                killer = directPlayer;
            }
        }
        if (killer == null) {
            return;
        }
        long remaining = Math.max(0L, tag.getLong(DUEL_END_TICK) - SkillCooldownHelper.now(player));
        long elapsed = Math.max(0L, DUEL_DURATION_TICKS - remaining);
        DfsAchievements.recordManbaDuelDeath(player, killer, elapsed);
    }

    public static float outgoingDamageMultiplier(ServerPlayer player, LivingEntity target) {
        if (!isManba(player) || EXECUTING_DUEL_TARGETS.contains(target.getUUID())) {
            return 1.0f;
        }
        return distanceDamageMultiplier(player.distanceTo(target));
    }

    public static float incomingDamageMultiplier(Player player, DamageSource source) {
        if (!isManba(player)) {
            return 1.0f;
        }
        Entity attacker = source.getEntity();
        if (attacker == null) {
            attacker = source.getDirectEntity();
        }
        if (attacker == null || attacker == player) {
            return 1.0f;
        }
        return distanceDamageMultiplier(attacker.distanceTo(player));
    }

    public static float applyDuelMeleeIncomingDamageFloor(Player player, DamageSource source, float amount) {
        if (!isManba(player) || !isDuelActive(player) || amount <= 0.0F || !isMeleeDamage(player, source)) {
            return amount;
        }
        return Math.max(amount, player.getMaxHealth() * 0.35F);
    }

    public static boolean shouldBlockPlayerDuelDamage(LivingEntity hurtEntity, DamageSource source) {
        if (hurtEntity == null || source == null) {
            return false;
        }
        Player attacker = damageSourcePlayer(source);
        if (attacker != null) {
            UUID attackerPartnerId = playerDuelPartnerId(attacker);
            if (attackerPartnerId != null && !hurtEntity.getUUID().equals(attackerPartnerId)) {
                return true;
            }
        }
        if (hurtEntity instanceof Player targetPlayer) {
            UUID targetPartnerId = playerDuelPartnerId(targetPlayer);
            return targetPartnerId != null
                    && attacker != null
                    && !attacker.getUUID().equals(targetPartnerId);
        }
        return false;
    }

    public static boolean arePlayerDuelPartners(Player first, Player second) {
        if (first == null || second == null) {
            return false;
        }
        UUID firstPartner = playerDuelPartnerId(first);
        if (firstPartner != null && firstPartner.equals(second.getUUID())) {
            return true;
        }
        UUID secondPartner = playerDuelPartnerId(second);
        return secondPartner != null && secondPartner.equals(first.getUUID());
    }

    public static void enforceDuelTaunt(LivingEntity entity) {
        enforceDuelTaunt(entity, false);
    }

    public static void restoreDuelTauntAfterBlind(LivingEntity entity) {
        enforceDuelTaunt(entity, true);
    }

    private static void enforceDuelTaunt(LivingEntity entity, boolean ignoreBlind) {
        if (entity.level().isClientSide || !(entity instanceof Mob mob) || !(mob instanceof Enemy)) {
            return;
        }
        if (!ignoreBlind && entity.hasEffect(ModEffects.MANBA_BLINDED)) {
            return;
        }
        UUID ownerId = DUEL_TARGET_TO_OWNER.get(entity.getUUID());
        if (ownerId == null || !(entity.level() instanceof ServerLevel level)) {
            return;
        }
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ownerId);
        if (owner == null || !isManba(owner) || !isDuelActive(owner) || !owner.isAlive()) {
            return;
        }
        if (mob.getTarget() != owner) {
            mob.setTarget(owner);
        }
    }

    public static boolean isActiveDuelTarget(LivingEntity entity) {
        UUID ownerId = DUEL_TARGET_TO_OWNER.get(entity.getUUID());
        if (ownerId == null || !(entity.level() instanceof ServerLevel level)) {
            return false;
        }
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ownerId);
        return owner != null && isManba(owner) && isDuelActive(owner);
    }

    public static boolean isExecutingDuelTarget(LivingEntity entity) {
        return EXECUTING_DUEL_TARGETS.contains(entity.getUUID());
    }

    public static void recordDamageTaken(ServerPlayer player) {
        if (!isManba(player)) {
            return;
        }
        data(player).putLong(DAMAGE_SPEED_UNTIL, SkillCooldownHelper.now(player) + 2L * 20L);
        syncToClient(player);
    }

    public static double behaviorSpeedMultiplier(LivingEntity entity) {
        double multiplier = 1.0D;
        if (entity instanceof Player player && isManba(player)) {
            multiplier += selfTaughtStacks(player) * 0.06D;
            if (hasTalent(player, ManbaTalent.UNYIELDING) && player.getHealth() < player.getMaxHealth()) {
                multiplier += 0.40D;
            }
        }
        if (hasNearbyWatcher(entity)) {
            multiplier += 0.40D;
        }
        return multiplier;
    }

    public static void onUseItemFinished(LivingEntity entity) {
        if (!(entity instanceof ServerPlayer player) || !isManba(player) || !hasTalent(player, ManbaTalent.SELF_TAUGHT)) {
            return;
        }
        CompoundTag tag = data(player);
        tag.putInt(SELF_TAUGHT_STACKS, Math.min(10, tag.getInt(SELF_TAUGHT_STACKS) + 1));
        syncToClient(player);
    }

    public static int selfTaughtStacks(Player player) {
        return Math.min(10, data(player).getInt(SELF_TAUGHT_STACKS));
    }

    public static void syncToClient(ServerPlayer player) {
        if (!isManba(player)) {
            return;
        }
        initializeIfNeeded(player);
        int[] talents = ordinals(selectedTalents(player));
        NetworkHandler.sendToPlayer(new S2C_SyncManbaState(
                elbowCharges(player),
                ELBOW_MAX_CHARGES,
                elbowRechargeRemainingTicks(player),
                flashlightDurability(player),
                flashlightStats(player).maxDurabilityScaled(),
                isFlashlightActive(player),
                coreCooldownRemainingTicks(player),
                isDuelActive(player),
                duelAffection(player),
                duelRemainingTicks(player),
                isConfigured(player),
                talents,
                talentCooldowns(player, talents),
                selfTaughtStacks(player),
                ordinals(selectedBulbs(player)),
                selectedLens(player).ordinal(),
                ordinals(selectedBatteries(player))
        ), player);
        LivingEntity target = duelTarget(player);
        if (target != null) {
            syncDuelView(player, target, false);
        }
    }

    private static void rechargeElbow(ServerPlayer player, long now) {
        CompoundTag tag = data(player);
        int charges = elbowCharges(player);
        if (charges >= ELBOW_MAX_CHARGES) {
            tag.putLong(ELBOW_NEXT_RECHARGE, 0L);
            return;
        }
        long next = tag.getLong(ELBOW_NEXT_RECHARGE);
        if (next <= 0L) {
            tag.putLong(ELBOW_NEXT_RECHARGE, SkillCooldownHelper.until(player, now, ELBOW_RECHARGE_TICKS));
            return;
        }
        while (charges < ELBOW_MAX_CHARGES && now >= next) {
            charges++;
            next += SkillCooldownHelper.ticks(player, ELBOW_RECHARGE_TICKS);
        }
        tag.putInt(ELBOW_CHARGES, charges);
        tag.putLong(ELBOW_NEXT_RECHARGE, charges >= ELBOW_MAX_CHARGES ? 0L : next);
    }

    private static void tickFlashlight(ServerPlayer player, long now) {
        CompoundTag tag = data(player);
        ManbaFlashlightStats stats = flashlightStats(player);
        int durability = tag.getInt(FLASHLIGHT_DURABILITY);
        boolean wasActive = tag.getBoolean(FLASHLIGHT_ACTIVE);
        if (hasTalent(player, ManbaTalent.DURABLE)) {
            durability += Math.max(1, FLASHLIGHT_SCALE / 20);
        }
        if (wasActive) {
            durability -= stats.consumePerTickScaled();
            if (durability <= 0) {
                durability = 0;
                tag.putBoolean(FLASHLIGHT_ACTIVE, false);
                clearFlashProgress(player);
                clearFlashlightLightBlocks(player);
                com.rzy.dealt_force_skills.skill.SkillCooldownHelper.notifyCooldown(player, Component.translatable("message.dealt_force_skills.manba.flashlight_empty"));
            } else {
                tickFlashlightTargets(player, stats);
                if (now % FLASHLIGHT_LIGHT_INTERVAL_TICKS == 0L) {
                    updateFlashlightLightBlocks(player, stats);
                }
            }
        } else {
            durability += stats.regenPerTickScaled();
            tickFlashlightDecayOnly(player, stats);
            clearFlashlightLightBlocks(player);
        }
        tag.putInt(FLASHLIGHT_DURABILITY, Math.min(stats.maxDurabilityScaled(), Math.max(0, durability)));
        boolean activeNow = tag.getBoolean(FLASHLIGHT_ACTIVE);
        if (activeNow && now % FLASHLIGHT_BEAM_SYNC_INTERVAL_TICKS == 0L) {
            syncFlashlightBeam(player, true, stats);
        } else if (wasActive && !activeNow) {
            syncFlashlightBeam(player, false, stats);
            syncToClient(player);
        }
    }

    private static void tickFlashlightTargets(ServerPlayer player, ManbaFlashlightStats stats) {
        Map<UUID, FlashProgress> progress = FLASH_PROGRESS.computeIfAbsent(player.getUUID(), id -> new HashMap<>());
        Set<UUID> illuminated = new HashSet<>();
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        double maxRange = stats.range();
        double minDot = Math.cos(Math.toRadians(stats.halfAngleDegrees()));
        AABB box = player.getBoundingBox().inflate(maxRange, maxRange * 0.5D, maxRange);

        for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class, box,
                entity -> TargetingUtil.isHostileLivingFor(player, entity))) {
            if (!isInFlashlightBeam(player, target, eye, look, maxRange, minDot)) {
                continue;
            }
            if (!isLookingAtCaster(target, player)) {
                continue;
            }
            FlashProgress entry = progress.computeIfAbsent(target.getUUID(), id -> new FlashProgress(target.getId(), 0));
            entry.entityId = target.getId();
            entry.progress = Math.min(FLASHLIGHT_FULL_PROGRESS, entry.progress + stats.progressPerTick());
            illuminated.add(target.getUUID());
            if (entry.progress >= FLASHLIGHT_FULL_PROGRESS) {
                applyFlashlightBlind(player, target, stats);
                entry.progress = 0;
            }
            sendFlashProgress(player, target, entry.progress);
        }

        decayUnlitFlashProgress(player, progress, illuminated, stats.decayPerTick());
    }

    private static void updateFlashlightLightBlocks(ServerPlayer player, ManbaFlashlightStats stats) {
        ServerLevel level = player.serverLevel();
        Set<BlockPos> previous = FLASHLIGHT_LIGHT_BLOCKS.getOrDefault(player.getUUID(), Set.of());
        Set<BlockPos> next = new HashSet<>();
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        if (look.lengthSqr() < 0.0001D) {
            return;
        }
        Vec3 right = look.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (right.lengthSqr() < 0.0001D) {
            right = new Vec3(1.0D, 0.0D, 0.0D);
        }
        right = right.normalize();
        Vec3 up = right.cross(look).normalize();
        double halfAngleTan = Math.tan(Math.toRadians(Math.max(2.0D, stats.halfAngleDegrees())));
        int placed = 0;
        for (double distance = 2.0D; distance <= stats.range() && placed < FLASHLIGHT_MAX_LIGHT_BLOCKS; distance += 2.0D) {
            double spread = Math.min(4.5D, Math.max(0.0D, distance * halfAngleTan));
            int rings = Math.max(0, Math.min(2, (int) Math.ceil(spread / 1.8D)));
            for (int sx = -rings; sx <= rings && placed < FLASHLIGHT_MAX_LIGHT_BLOCKS; sx++) {
                for (int sy = -rings; sy <= rings && placed < FLASHLIGHT_MAX_LIGHT_BLOCKS; sy++) {
                    Vec3 offset = right.scale(sx * 1.8D).add(up.scale(sy * 1.35D));
                    if (offset.length() > spread + 0.25D) {
                        continue;
                    }
                    Vec3 sample = eye.add(look.scale(distance)).add(offset);
                    if (!hasClearFlashlightPath(player, eye, sample)) {
                        continue;
                    }
                    BlockPos pos = BlockPos.containing(sample);
                    if (tryPlaceFlashlightLight(level, pos, previous, next)) {
                        placed++;
                    }
                }
            }
        }
        removeStaleFlashlightLightBlocks(level, previous, next);
        if (next.isEmpty()) {
            FLASHLIGHT_LIGHT_BLOCKS.remove(player.getUUID());
        } else {
            FLASHLIGHT_LIGHT_BLOCKS.put(player.getUUID(), next);
        }
    }

    private static boolean tryPlaceFlashlightLight(ServerLevel level, BlockPos pos, Set<BlockPos> previous, Set<BlockPos> next) {
        if (!level.isLoaded(pos) || next.contains(pos)) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        boolean owned = previous.contains(pos);
        if (owned && !state.isAir() && !state.is(Blocks.LIGHT)) {
            return false;
        }
        if (!owned && !state.isAir()) {
            return false;
        }
        if (owned || state.isAir()) {
            level.setBlock(pos, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, FLASHLIGHT_LIGHT_LEVEL), 3);
            next.add(pos.immutable());
            return true;
        }
        return false;
    }

    private static void clearFlashlightLightBlocks(ServerPlayer player) {
        Set<BlockPos> previous = FLASHLIGHT_LIGHT_BLOCKS.remove(player.getUUID());
        if (previous != null) {
            removeStaleFlashlightLightBlocks(player.serverLevel(), previous, Set.of());
        }
    }

    private static void removeStaleFlashlightLightBlocks(ServerLevel level, Set<BlockPos> previous, Set<BlockPos> next) {
        for (BlockPos pos : previous) {
            if (next.contains(pos) || !level.isLoaded(pos)) {
                continue;
            }
            if (level.getBlockState(pos).is(Blocks.LIGHT)) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            }
        }
    }

    private static void tickFlashlightDecayOnly(ServerPlayer player, ManbaFlashlightStats stats) {
        Map<UUID, FlashProgress> progress = FLASH_PROGRESS.get(player.getUUID());
        if (progress != null) {
            decayUnlitFlashProgress(player, progress, Set.of(), stats.decayPerTick());
        }
    }

    private static void decayUnlitFlashProgress(ServerPlayer player, Map<UUID, FlashProgress> progress,
                                                Set<UUID> illuminated, int decay) {
        Iterator<Map.Entry<UUID, FlashProgress>> iterator = progress.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, FlashProgress> entry = iterator.next();
            if (illuminated.contains(entry.getKey())) {
                continue;
            }
            FlashProgress value = entry.getValue();
            value.progress = Math.max(0, value.progress - decay);
            LivingEntity target = livingByUuid(player.serverLevel(), entry.getKey());
            if (target != null && value.progress > 0) {
                sendFlashProgress(player, target, value.progress);
            }
            if (value.progress <= 0) {
                if (target instanceof ServerPlayer targetPlayer) {
                    NetworkHandler.sendToPlayer(new S2C_ManbaFlashlightProgress(value.entityId, 0, 1), targetPlayer);
                }
                iterator.remove();
            }
        }
        if (progress.isEmpty()) {
            FLASH_PROGRESS.remove(player.getUUID());
        }
    }

    private static void applyFlashlightBlind(ServerPlayer player, LivingEntity target, ManbaFlashlightStats stats) {
        int amplifier = hasTalent(player, ManbaTalent.LIGHT_WARRIOR) ? 1 : 0;
        target.addEffect(new MobEffectInstance(ModEffects.MANBA_BLINDED,
                stats.blindTicks(), amplifier, false, true, true), player);
        if (!stats.noBlindCost()) {
            CompoundTag tag = data(player);
            tag.putInt(FLASHLIGHT_DURABILITY, Math.max(0, tag.getInt(FLASHLIGHT_DURABILITY) - FLASHLIGHT_BLIND_COST));
        }
        player.level().playSound(null, target.blockPosition(), ModSounds.MANBA_FLASHLIGHT_BLIND.get(),
                SoundSource.PLAYERS, 0.85f, 1.0f);
    }

    private static void sendFlashProgress(ServerPlayer player, LivingEntity target, int progress) {
        int percent = Math.max(0, Math.min(100, Math.round(progress / 10.0f)));
        NetworkHandler.sendToPlayer(new S2C_ManbaFlashlightProgress(target.getId(), percent, 10), player);
        if (target instanceof ServerPlayer targetPlayer) {
            NetworkHandler.sendToPlayer(new S2C_ManbaFlashlightProgress(target.getId(), percent, 10), targetPlayer);
        }
    }

    private static void syncFlashlightBeam(ServerPlayer player, boolean active, ManbaFlashlightStats stats) {
        NetworkHandler.sendToTrackingAndSelf(new S2C_ManbaFlashlightBeam(
                player.getId(),
                active,
                (float) stats.range(),
                (float) stats.halfAngleDegrees(),
                active ? FLASHLIGHT_BEAM_TTL_TICKS : 1
        ), player);
    }

    private static void clearFlashProgress(ServerPlayer player) {
        Map<UUID, FlashProgress> progress = FLASH_PROGRESS.remove(player.getUUID());
        if (progress == null) {
            return;
        }
        for (Map.Entry<UUID, FlashProgress> entry : progress.entrySet()) {
            LivingEntity target = livingByUuid(player.serverLevel(), entry.getKey());
            if (target instanceof ServerPlayer targetPlayer) {
                NetworkHandler.sendToPlayer(new S2C_ManbaFlashlightProgress(entry.getValue().entityId, 0, 1), targetPlayer);
            }
        }
    }

    private static void tickDuel(ServerPlayer player, long now) {
        CompoundTag tag = data(player);
        if (!tag.hasUUID(DUEL_TARGET)) {
            return;
        }
        LivingEntity target = duelTarget(player);
        if (now >= tag.getLong(DUEL_END_TICK)) {
            endDuel(player, true, true);
            return;
        }
        if (target == null || !target.isAlive() || !player.isAlive()) {
            endDuel(player, false, false);
            return;
        }
        if (now - tag.getLong(DUEL_LAST_CHANGE) >= DUEL_STALE_TICKS) {
            endDuel(player, false, true);
            return;
        }
        if (now >= tag.getLong(DUEL_NEXT_DECAY)) {
            addDuelAffection(player, -1);
            tag.putLong(DUEL_NEXT_DECAY, now + DUEL_AFFECTION_DECAY_INTERVAL_TICKS);
        }
        if (target.hasEffect(ModEffects.MANBA_BLINDED)) {
            ManbaBlindedEffect.suppressHostileTargeting(target);
            return;
        }
        enforceDuelTaunt(target);
    }

    private static void endDuel(ServerPlayer player, boolean executeIfWon, boolean notify) {
        CompoundTag tag = data(player);
        UUID targetId = tag.hasUUID(DUEL_TARGET) ? tag.getUUID(DUEL_TARGET) : null;
        LivingEntity target = targetId == null ? null : duelTarget(player, targetId);
        int affection = tag.getInt(DUEL_AFFECTION);
        boolean shouldExecute = affection > 100;
        stopDuelBgm(player, target, targetId);
        if (targetId != null) {
            DUEL_TARGET_TO_OWNER.remove(targetId);
        }
        clearControlAffectionCooldowns(player.getUUID());
        if (targetId != null) {
            clearControlAffectionCooldowns(targetId);
        }
        DUEL_TARGET_ENTITIES.remove(player.getUUID());
        if (target != null) {
            target.setGlowingTag(false);
            if (shouldExecute) {
                executeDuelTarget(player, target);
            }
            syncDuelView(player, target, true);
        }
        tag.remove(DUEL_TARGET);
        tag.putLong(DUEL_END_TICK, 0L);
        tag.putInt(DUEL_AFFECTION, 0);
        tag.putLong(DUEL_LAST_CHANGE, 0L);
        tag.putLong(DUEL_NEXT_DECAY, 0L);
        tag.putInt(DUEL_REVIVES_USED, 0);
        tag.putLong(CORE_COOLDOWN_UNTIL,
                SkillCooldownHelper.until(player, SkillCooldownHelper.now(player), CORE_COOLDOWN_TICKS));
        if (notify) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.manba.duel_ended"), true);
        }
        syncToClient(player);
    }

    private static void stopDuelBgm(ServerPlayer player, LivingEntity target, UUID targetId) {
        Set<UUID> listenerIds = new HashSet<>();
        Set<UUID> remembered = DUEL_BGM_LISTENERS.remove(player.getUUID());
        if (remembered != null) {
            listenerIds.addAll(remembered);
        }
        listenerIds.add(player.getUUID());
        if (targetId != null) {
            listenerIds.add(targetId);
        }
        ServerPlayer targetPlayer = target instanceof ServerPlayer directTarget
                ? directTarget
                : targetId == null ? null : player.server.getPlayerList().getPlayer(targetId);
        if (targetPlayer != null) {
            listenerIds.add(targetPlayer.getUUID());
        }
        double radiusSqr = DUEL_BGM_TRACK_RADIUS * DUEL_BGM_TRACK_RADIUS;
        for (ServerPlayer listener : player.server.getPlayerList().getPlayers()) {
            if (listener.level() != player.level() && (target == null || listener.level() != target.level())) {
                continue;
            }
            if (listener.distanceToSqr(player) <= radiusSqr
                    || (target != null && listener.distanceToSqr(target) <= radiusSqr)) {
                listenerIds.add(listener.getUUID());
            }
        }
        for (UUID listenerId : listenerIds) {
            ServerPlayer listener = player.server.getPlayerList().getPlayer(listenerId);
            if (listener != null) {
                sendDuelBgmStop(listener);
            }
        }
    }

    private static void rememberDuelBgmListeners(ServerPlayer player, LivingEntity target) {
        Set<UUID> listenerIds = new HashSet<>();
        double radiusSqr = DUEL_BGM_TRACK_RADIUS * DUEL_BGM_TRACK_RADIUS;
        for (ServerPlayer listener : player.server.getPlayerList().getPlayers()) {
            if (listener.level() == player.level()
                    && (listener.distanceToSqr(player) <= radiusSqr
                    || listener == player
                    || listener == target
                    || (target != null && listener.distanceToSqr(target) <= radiusSqr))) {
                listenerIds.add(listener.getUUID());
            }
        }
        listenerIds.add(player.getUUID());
        if (target instanceof ServerPlayer targetPlayer) {
            listenerIds.add(targetPlayer.getUUID());
        }
        DUEL_BGM_LISTENERS.put(player.getUUID(), listenerIds);
    }

    private static void sendDuelBgmStop(ServerPlayer listener) {
        ResourceLocation eventId = ModSounds.MANBA_DUEL_BGM.get().getLocation();
        listener.connection.send(new ClientboundStopSoundPacket(eventId, SoundSource.PLAYERS));
        listener.connection.send(new ClientboundStopSoundPacket(eventId, null));
        listener.connection.send(new ClientboundStopSoundPacket(DUEL_BGM_SOUND_FILE, SoundSource.PLAYERS));
        listener.connection.send(new ClientboundStopSoundPacket(DUEL_BGM_SOUND_FILE, null));
        listener.connection.send(new ClientboundStopSoundPacket(null, SoundSource.PLAYERS));
    }

    private static void executeDuelTarget(ServerPlayer player, LivingEntity target) {
        if (!com.rzy.dealt_force_skills.boss.BossCombatRules.canInstantKill(target)) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                    "message.dealt_force_skills.beacon_boss.immune_execute"), true);
            return;
        }
        EXECUTING_DUEL_TARGETS.add(target.getUUID());
        try {
            player.level().playSound(null, target.blockPosition(), ModSounds.MANBA_DUEL_EXECUTE.get(),
                    SoundSource.PLAYERS, 1.0f, 1.0f);
            DamageSource source = SkillDamageHelper.manbaDuelExecute(player.serverLevel(), null, player);
            target.hurt(source, Math.max(1000.0f, target.getMaxHealth() * 100.0f));
            target.kill();
            if (isStillAliveAfterDuelExecute(target)) {
                forceDuelExecuteDeath(source, target);
            }
        } finally {
            EXECUTING_DUEL_TARGETS.remove(target.getUUID());
        }
    }

    private static boolean isStillAliveAfterDuelExecute(LivingEntity target) {
        return !target.isRemoved() && target.isAlive() && target.getHealth() > 0.0f;
    }

    private static void forceDuelExecuteDeath(DamageSource source, LivingEntity target) {
        target.setHealth(0.0f);
        target.die(source);
        if (target instanceof ServerPlayer targetPlayer) {
            if (isStillAliveAfterDuelExecute(targetPlayer)) {
                targetPlayer.kill();
            }
            return;
        }
        if (isStillAliveAfterDuelExecute(target)) {
            target.kill();
        }
        if (isStillAliveAfterDuelExecute(target)) {
            target.remove(Entity.RemovalReason.KILLED);
        }
        if (isStillAliveAfterDuelExecute(target)) {
            target.discard();
        }
    }

    private static boolean tryDuelRevive(ServerPlayer player, CompoundTag tag) {
        if (!isDuelActive(player)) {
            return false;
        }
        int maxRevives = tag.getInt(DUEL_AFFECTION) > 200 ? 2 : 1;
        int used = tag.getInt(DUEL_REVIVES_USED);
        if (used >= maxRevives) {
            return false;
        }
        tag.putInt(DUEL_REVIVES_USED, used + 1);
        player.setHealth(player.getMaxHealth());
        DfsAchievements.recordFatalAvoidance(player);
        if (used == 1) {
            DfsAchievements.recordManbaFavorSecondDeathSave(player, tag.getInt(DUEL_AFFECTION));
        }
        player.level().playSound(null, player.blockPosition(), ModSounds.MANBA_DUEL_REVIVE.get(),
                SoundSource.PLAYERS, 0.9f, 1.0f);
        syncToClient(player);
        return true;
    }

    private static void addDuelAffection(ServerPlayer player, int delta) {
        if (!isDuelActive(player) || delta == 0) {
            return;
        }
        CompoundTag tag = data(player);
        int old = tag.getInt(DUEL_AFFECTION);
        int next = Math.max(0, Math.min(999, old + delta));
        tag.putInt(DUEL_AFFECTION, next);
        if (next != old) {
            tag.putLong(DUEL_LAST_CHANGE, SkillCooldownHelper.now(player));
        }
        syncToClient(player);
    }

    private static void syncDuelView(ServerPlayer player, LivingEntity target, boolean clear) {
        S2C_ManbaDuelView msg = new S2C_ManbaDuelView(!clear, player.getId(), target.getId(),
                player.getDisplayName().getString(), target.getDisplayName().getString(),
                duelAffection(player), duelRemainingTicks(player));
        NetworkHandler.sendToPlayer(msg, player);
        if (target instanceof ServerPlayer targetPlayer) {
            NetworkHandler.sendToPlayer(msg, targetPlayer);
        }
    }

    private static void tickDurableTalent(ServerPlayer player, long now) {
        if (!hasTalent(player, ManbaTalent.DURABLE)) {
            return;
        }
        CompoundTag tag = data(player);
        long next = tag.getLong(DURABLE_NEXT_TICK);
        if (next > now) {
            return;
        }
        tag.putLong(DURABLE_NEXT_TICK, now + 10L * 20L);
        repairInventoryItems(player);
    }

    private static void repairInventoryItems(ServerPlayer player) {
        for (ItemStack stack : player.getInventory().items) {
            repairStack(stack);
        }
        for (ItemStack stack : player.getInventory().armor) {
            repairStack(stack);
        }
        for (ItemStack stack : player.getInventory().offhand) {
            repairStack(stack);
        }
    }

    private static void repairStack(ItemStack stack) {
        if (stack.isEmpty() || !stack.isDamageableItem() || !stack.isDamaged()) {
            return;
        }
        int repair = Math.max(1, stack.getMaxDamage() / 100);
        stack.setDamageValue(Math.max(0, stack.getDamageValue() - repair));
    }

    private static void tickBraveForward(ServerPlayer player) {
        CompoundTag tag = data(player);
        if (!hasTalent(player, ManbaTalent.BRAVE_FORWARD)) {
            tag.putInt(SNEAK_TICKS, 0);
            return;
        }
        int previous = tag.getInt(SNEAK_TICKS);
        if (player.isShiftKeyDown()) {
            tag.putInt(SNEAK_TICKS, previous + 1);
            return;
        }
        if (previous >= 3 * 20) {
            performBraveDash(player);
        }
        tag.putInt(SNEAK_TICKS, 0);
    }

    private static void performBraveDash(ServerPlayer player) {
        Vec3 look = player.getLookAngle();
        Vec3 direction = new Vec3(look.x, 0.0D, look.z);
        if (direction.lengthSqr() < 0.001D) {
            return;
        }
        direction = direction.normalize();
        player.setDeltaMovement(direction.scale(1.45D).add(0.0D, Math.max(0.08D, player.getDeltaMovement().y), 0.0D));
        player.hurtMarked = true;

        AABB box = player.getBoundingBox().expandTowards(direction.scale(BRAVE_DASH_RANGE)).inflate(1.0D, 0.8D, 1.0D);
        for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class, box,
                entity -> TargetingUtil.isHostileLivingFor(player, entity))) {
            Vec3 toTarget = target.position().subtract(player.position());
            Vec3 horizontal = new Vec3(toTarget.x, 0.0D, toTarget.z);
            if (horizontal.lengthSqr() > 0.001D && direction.dot(horizontal.normalize()) < 0.15D) {
                continue;
            }
            target.addEffect(new MobEffectInstance(ModEffects.STUN, com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.manba.manba_state_manager.effect.stun.3.duration_ticks", 30), com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.manba.manba_state_manager.effect.stun.3.amplifier", 0), false, true, true), player);
            SinevaKnockdownState.apply(player, target, 30);
        }
        RangedSoundHelper.playFollowingPlayer(player, ModSounds.VYRON_DASH.get(),
                SoundSource.PLAYERS, 0.8f, 0.8f, 32.0D);
    }

    private static void tickDiversionWatch(ServerPlayer player, long now) {
        if (!hasTalent(player, ManbaTalent.DIVERSION) || data(player).getBoolean(DIVERSION_READY)) {
            return;
        }
        if (now - data(player).getLong(DIVERSION_LAST_HURT) < 10L * 20L) {
            data(player).putInt(DIVERSION_VIEW_TICKS, 0);
            return;
        }
        boolean seen = false;
        for (ServerPlayer other : player.server.getPlayerList().getPlayers()) {
            if (other == player || other.level() != player.level() || !TargetingUtil.isTargetablePlayer(other)) {
                continue;
            }
            if (other.distanceToSqr(player) > 32.0D * 32.0D || !other.hasLineOfSight(player)) {
                continue;
            }
            if (isLookingAtCaster(other, player)) {
                seen = true;
                break;
            }
        }
        int ticks = seen ? data(player).getInt(DIVERSION_VIEW_TICKS) + 1 : Math.max(0, data(player).getInt(DIVERSION_VIEW_TICKS) - 2);
        data(player).putInt(DIVERSION_VIEW_TICKS, ticks);
        if (ticks >= 10 * 20) {
            data(player).putBoolean(DIVERSION_READY, true);
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.manba.diversion_ready"), true);
            syncToClient(player);
        }
    }

    private static void recordDiversionHurt(ServerPlayer player) {
        data(player).putLong(DIVERSION_LAST_HURT, SkillCooldownHelper.now(player));
    }

    private static void syncOpportunityWindow(ServerPlayer player, long now) {
        if (now % OPPORTUNITY_WINDOW_SYNC_INTERVAL_TICKS != 0L) {
            return;
        }
        if (!hasTalent(player, ManbaTalent.OPPORTUNITY_WINDOW)) {
            NetworkHandler.sendToPlayer(new S2C_ManbaOpportunityMarkers(List.of()), player);
            return;
        }
        NetworkHandler.sendToPlayer(new S2C_ManbaOpportunityMarkers(opportunityMarkers(player)), player);
    }

    private static List<ManbaOpportunityMarker> opportunityMarkers(ServerPlayer player) {
        return opportunityMarkers(player, OPPORTUNITY_WINDOW_RANGE);
    }

    public static List<ManbaOpportunityMarker> opportunityMarkers(ServerPlayer player, double range) {
        List<ManbaOpportunityMarker> markers = new ArrayList<>();
        double clampedRange = Math.max(1.0D, range);
        double rangeSqr = clampedRange * clampedRange;
        ServerLevel level = player.serverLevel();
        Vec3 playerPos = player.position();
        for (ServerPlayer other : player.server.getPlayerList().getPlayers()) {
            if (markers.size() >= OPPORTUNITY_WINDOW_MAX_MARKERS) {
                return markers;
            }
            if (other == player || other.level() != level || !other.isAlive()) {
                continue;
            }
            if (other.distanceToSqr(player) <= rangeSqr) {
                markers.add(new ManbaOpportunityMarker(ManbaOpportunityMarkerType.PLAYER,
                        other.position(), other.getDisplayName().getString()));
            }
        }

        int minChunkX = (int) Math.floor((player.getX() - clampedRange) / 16.0D);
        int maxChunkX = (int) Math.floor((player.getX() + clampedRange) / 16.0D);
        int minChunkZ = (int) Math.floor((player.getZ() - clampedRange) / 16.0D);
        int maxChunkZ = (int) Math.floor((player.getZ() + clampedRange) / 16.0D);
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                LevelChunk chunk = level.getChunkSource().getChunk(chunkX, chunkZ, false);
                if (chunk == null) {
                    continue;
                }
                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    if (markers.size() >= OPPORTUNITY_WINDOW_MAX_MARKERS) {
                        return markers;
                    }
                    ManbaOpportunityMarkerType type = opportunityContainerType(blockEntity);
                    if (type == null) {
                        continue;
                    }
                    BlockPos pos = blockEntity.getBlockPos();
                    Vec3 center = Vec3.atCenterOf(pos);
                    if (center.distanceToSqr(playerPos) <= rangeSqr) {
                        markers.add(new ManbaOpportunityMarker(type, center, ""));
                    }
                }
            }
        }
        return markers;
    }

    private static ManbaOpportunityMarkerType opportunityContainerType(BlockEntity blockEntity) {
        if (blockEntity instanceof RandomizableContainerBlockEntity randomizable) {
            return hasUnopenedLoot(randomizable)
                    ? ManbaOpportunityMarkerType.UNOPENED_LOOT
                    : ManbaOpportunityMarkerType.OPENED_OR_NORMAL;
        }
        if (blockEntity instanceof Container
                || (blockEntity.getLevel() != null
                    && blockEntity.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, blockEntity.getBlockPos(), null) != null)) {
            return ManbaOpportunityMarkerType.OPENED_OR_NORMAL;
        }
        return null;
    }

    private static boolean hasUnopenedLoot(RandomizableContainerBlockEntity blockEntity) {
        return blockEntity.getLootTable() != null;
    }

    private static void applyMovementModifiers(ServerPlayer player) {
        var attr = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attr == null) {
            return;
        }
        if (attr.getModifier(BASE_SLOW_UUID) == null) {
            attr.addTransientModifier(new AttributeModifier(BASE_SLOW_UUID, -0.05D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
        boolean lightSpeed = hasTalent(player, ManbaTalent.LIGHT_WARRIOR) && isFlashlightActive(player);
        if (lightSpeed && attr.getModifier(LIGHT_WARRIOR_SPEED_UUID) == null) {
            attr.addTransientModifier(new AttributeModifier(LIGHT_WARRIOR_SPEED_UUID, 0.20D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        } else if (!lightSpeed && attr.getModifier(LIGHT_WARRIOR_SPEED_UUID) != null) {
            attr.removeModifier(LIGHT_WARRIOR_SPEED_UUID);
        }
        boolean unyielding = hasTalent(player, ManbaTalent.UNYIELDING) && player.getHealth() < player.getMaxHealth();
        if (unyielding && attr.getModifier(UNYIELDING_SPEED_UUID) == null) {
            attr.addTransientModifier(new AttributeModifier(UNYIELDING_SPEED_UUID, 0.40D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        } else if (!unyielding && attr.getModifier(UNYIELDING_SPEED_UUID) != null) {
            attr.removeModifier(UNYIELDING_SPEED_UUID);
        }
        boolean damageSpeed = data(player).getLong(DAMAGE_SPEED_UNTIL) > SkillCooldownHelper.now(player);
        if (damageSpeed && attr.getModifier(DAMAGE_SPEED_UUID) == null) {
            attr.addTransientModifier(new AttributeModifier(DAMAGE_SPEED_UUID, 0.50D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        } else if (!damageSpeed && attr.getModifier(DAMAGE_SPEED_UUID) != null) {
            attr.removeModifier(DAMAGE_SPEED_UUID);
        }
    }

    private static boolean isMeleeDamage(Player player, DamageSource source) {
        Entity attacker = source.getEntity();
        Entity direct = source.getDirectEntity();
        if (!(attacker instanceof LivingEntity) || attacker == player) {
            return false;
        }
        if (direct != null && direct != attacker) {
            return false;
        }
        if (direct instanceof Projectile) {
            return false;
        }
        return attacker.distanceToSqr(player) <= 5.0D * 5.0D;
    }

    private static UUID playerDuelPartnerId(Player player) {
        if (player instanceof ServerPlayer serverPlayer && isManba(player) && isDuelActive(player)) {
            LivingEntity target = duelTarget(serverPlayer);
            if (target instanceof Player targetPlayer) {
                return targetPlayer.getUUID();
            }
        }
        UUID ownerId = DUEL_TARGET_TO_OWNER.get(player.getUUID());
        if (ownerId == null || !(player.level() instanceof ServerLevel level)) {
            return null;
        }
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ownerId);
        if (owner == null || !isManba(owner) || !isDuelActive(owner)) {
            return null;
        }
        LivingEntity currentTarget = duelTarget(owner);
        return currentTarget instanceof Player targetPlayer && targetPlayer.getUUID().equals(player.getUUID())
                ? ownerId
                : null;
    }

    private static Player damageSourcePlayer(DamageSource source) {
        Entity attacker = source.getEntity();
        if (attacker instanceof Player player) {
            return player;
        }
        Entity direct = source.getDirectEntity();
        return direct instanceof Player player ? player : null;
    }

    private static boolean isAllowedDuelIncomingDamage(Player player, DamageSource source) {
        return isMeleeDamage(player, source)
                || isExplosionDamage(source)
                || isTrueSkillDamage(source);
    }

    private static boolean isExplosionDamage(DamageSource source) {
        return source.is(DamageTypes.EXPLOSION)
                || source.is(DamageTypes.PLAYER_EXPLOSION)
                || source.is(SkillDamageHelper.SHEPHERD_FRAG_GRENADE)
                || source.is(SkillDamageHelper.D_WOLF_HAND_CANNON)
                || source.is(SkillDamageHelper.ULURU_MISSILE)
                || source.is(SkillDamageHelper.ULURU_INCENDIARY)
                || source.is(SkillDamageHelper.VYRON_MAGNETIC_BOMB);
    }

    private static boolean isTrueSkillDamage(DamageSource source) {
        return source.is(SkillDamageHelper.TRUE_SKILL_DAMAGE)
                || source.is(SkillDamageHelper.SINEVA_BLADE_WIRE)
                || source.is(SkillDamageHelper.GIZMO_SPIDERLING)
                || source.is(SkillDamageHelper.MANBA_DUEL_EXECUTE)
                || source.is(SkillDamageHelper.MANBA_ELBOW)
                || source.is(SkillDamageHelper.NOX_ROTOR)
                || source.is(SkillDamageHelper.NOX_FLASH)
                || source.is(SkillDamageHelper.TOXIK_FIREFLY)
                || source.is(SkillDamageHelper.RAPTOR_PULSE);
    }

    private static float distanceDamageMultiplier(double distance) {
        if (distance <= DAMAGE_DECAY_START_DISTANCE) {
            return 1.0f;
        }
        double multiplier = 1.0D - (distance - DAMAGE_DECAY_START_DISTANCE) * DAMAGE_DECAY_PER_BLOCK;
        return (float) Math.max(0.0D, multiplier);
    }

    private static boolean isLookingAtCaster(LivingEntity target, LivingEntity caster) {
        Vec3 toCaster = caster.getEyePosition().subtract(target.getEyePosition());
        if (toCaster.lengthSqr() < 0.001D) {
            return true;
        }
        Vec3 targetLook = target.getLookAngle().normalize();
        Vec3 normalized = toCaster.normalize();
        if (targetLook.dot(normalized) >= FLASHLIGHT_TARGET_LOOK_DOT) {
            return true;
        }
        Vec3 flatLook = new Vec3(targetLook.x, 0.0D, targetLook.z);
        Vec3 flatToCaster = new Vec3(toCaster.x, 0.0D, toCaster.z);
        return flatLook.lengthSqr() > 0.001D
                && flatToCaster.lengthSqr() > 0.001D
                && flatLook.normalize().dot(flatToCaster.normalize()) >= FLASHLIGHT_TARGET_LOOK_DOT;
    }

    private static boolean isInFlashlightBeam(ServerPlayer player, LivingEntity target,
                                              Vec3 eye, Vec3 look, double maxRange, double minDot) {
        for (Vec3 sample : flashlightSamplePoints(target)) {
            Vec3 toSample = sample.subtract(eye);
            double distance = toSample.length();
            if (distance <= 0.001D || distance > maxRange) {
                continue;
            }
            if (look.dot(toSample.scale(1.0D / distance)) < minDot) {
                continue;
            }
            if (hasClearFlashlightPath(player, eye, sample)) {
                return true;
            }
        }
        return false;
    }

    private static Vec3[] flashlightSamplePoints(LivingEntity target) {
        double y = target.getY();
        double height = target.getBbHeight();
        return new Vec3[]{
                target.getEyePosition(),
                new Vec3(target.getX(), y + height * 0.60D, target.getZ()),
                new Vec3(target.getX(), y + height * 0.35D, target.getZ())
        };
    }

    private static boolean hasClearFlashlightPath(ServerPlayer player, Vec3 from, Vec3 to) {
        HitResult hit = player.level().clip(new ClipContext(from, to,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.MISS || hit.getLocation().distanceToSqr(to) <= 0.09D;
    }

    private static boolean hasNearbyWatcher(LivingEntity entity) {
        if (!(entity.level() instanceof ServerLevel level)) {
            return false;
        }
        AABB box = entity.getBoundingBox().inflate(16.0D);
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, box,
                other -> other != entity && isManba(other) && hasTalent(other, ManbaTalent.WATCHER))) {
            if (player.isAlive()) {
                return true;
            }
        }
        return false;
    }

    private static boolean isControlOrNegativeEffect(Holder<MobEffect> effect) {
        return effect.value().getCategory() == MobEffectCategory.HARMFUL
                || effect == ModEffects.STUN
                || effect == ModEffects.WEBBED
                || effect == ModEffects.SONIC_SHOCK
                || effect == ModEffects.NOX_FLASHED
                || effect == ModEffects.HACKCLAW_FLASH_BLIND
                || effect == ModEffects.NOX_DELAYED_WOUND
                || effect == ModEffects.NOX_CRIPPLED
                || effect == ModEffects.MANBA_BLINDED
                || effect == ModEffects.STINGER_STIM_SUPPRESSION
                || effect == ModEffects.CORROSION;
    }

    private static boolean isDuelDownedEffect(Holder<MobEffect> effect) {
        return effect == ModEffects.STINGER_DOWNED
                || effect == ModEffects.VLINDER_VITAL_DOWNED
                || effect == ModEffects.TEMPEST_EMERGENCY_DOWNED
                || effect == ModEffects.CATDAD_DOWNED;
    }

    private static boolean consumeControlAffectionCooldown(ServerPlayer owner, LivingEntity target) {
        long now = SkillCooldownHelper.now(owner);
        ControlAffectionKey key = new ControlAffectionKey(owner.getUUID(), target.getUUID());
        Long readyAt = DUEL_CONTROL_AFFECTION_READY_AT.get(key);
        if (readyAt != null && readyAt > now) {
            return false;
        }
        if (now % 200L == 0L) {
            DUEL_CONTROL_AFFECTION_READY_AT.entrySet().removeIf(entry -> entry.getValue() <= now);
        }
        DUEL_CONTROL_AFFECTION_READY_AT.put(key, now + Math.max(1, DUEL_CONTROL_AFFECTION_COOLDOWN_TICKS));
        return true;
    }

    private static void clearControlAffectionCooldowns(UUID playerId) {
        DUEL_CONTROL_AFFECTION_READY_AT.keySet().removeIf(key ->
                key.owner().equals(playerId) || key.target().equals(playerId));
    }

    private static boolean sameUuid(Entity entity, UUID uuid) {
        return entity != null && entity.getUUID().equals(uuid);
    }

    private static SoundEvent randomControlSound(ServerPlayer player) {
        @SuppressWarnings("unchecked")
        DeferredHolder<SoundEvent, SoundEvent>[] sounds = new DeferredHolder[]{
                ModSounds.MANBA_DUEL_CONTROL_1,
                ModSounds.MANBA_DUEL_CONTROL_2,
                ModSounds.MANBA_DUEL_CONTROL_3,
                ModSounds.MANBA_DUEL_CONTROL_4
        };
        return sounds[player.getRandom().nextInt(sounds.length)].get();
    }

    private static LivingEntity duelTarget(ServerPlayer player) {
        CompoundTag tag = data(player);
        if (!tag.hasUUID(DUEL_TARGET)) {
            return null;
        }
        return duelTarget(player, tag.getUUID(DUEL_TARGET));
    }

    private static LivingEntity duelTarget(ServerPlayer player, UUID uuid) {
        WeakReference<LivingEntity> rememberedReference = DUEL_TARGET_ENTITIES.get(player.getUUID());
        LivingEntity remembered = rememberedReference == null ? null : rememberedReference.get();
        if (rememberedReference != null && (remembered == null || !remembered.getUUID().equals(uuid))) {
            DUEL_TARGET_ENTITIES.remove(player.getUUID());
            remembered = null;
        }
        if (remembered != null && !remembered.isRemoved()) {
            return remembered;
        }
        LivingEntity currentLevelTarget = livingByUuid(player.serverLevel(), uuid);
        if (currentLevelTarget != null) {
            DUEL_TARGET_ENTITIES.put(player.getUUID(), new WeakReference<>(currentLevelTarget));
            return currentLevelTarget;
        }
        ServerPlayer playerTarget = player.server.getPlayerList().getPlayer(uuid);
        if (playerTarget != null) {
            DUEL_TARGET_ENTITIES.put(player.getUUID(), new WeakReference<>(playerTarget));
            return playerTarget;
        }
        for (ServerLevel level : player.server.getAllLevels()) {
            if (level == player.serverLevel()) {
                continue;
            }
            LivingEntity target = livingByUuid(level, uuid);
            if (target != null) {
                DUEL_TARGET_ENTITIES.put(player.getUUID(), new WeakReference<>(target));
                return target;
            }
        }
        DUEL_TARGET_ENTITIES.remove(player.getUUID());
        return null;
    }

    private static LivingEntity livingByUuid(ServerLevel level, UUID uuid) {
        Entity entity = level.getEntity(uuid);
        return entity instanceof LivingEntity living ? living : null;
    }

    private static int[] talentCooldowns(Player player, int[] talentOrdinals) {
        int[] cooldowns = new int[talentOrdinals.length];
        for (int i = 0; i < talentOrdinals.length; i++) {
            ManbaTalent[] values = ManbaTalent.values();
            if (talentOrdinals[i] < 0 || talentOrdinals[i] >= values.length) {
                cooldowns[i] = 0;
                continue;
            }
            ManbaTalent talent = values[talentOrdinals[i]];
            cooldowns[i] = switch (talent) {
                case STEEL_BODY -> remainingTicks(player, STEEL_COOLDOWN_UNTIL);
                case INDESTRUCTIBLE -> remainingTicks(player, INDESTRUCTIBLE_COOLDOWN_UNTIL);
                default -> 0;
            };
        }
        return cooldowns;
    }

    private static int remainingTicks(Player player, String key) {
        return SkillCooldownHelper.remainingTicks(player, data(player).getLong(key));
    }

    private static EnumSet<ManbaTalent> defaultTalents() {
        return EnumSet.of(ManbaTalent.SELF_TAUGHT, ManbaTalent.DURABLE,
                ManbaTalent.BRAVE_FORWARD, ManbaTalent.INDESTRUCTIBLE);
    }

    private static <E extends Enum<E>> EnumSet<E> decodeSet(Class<E> type, int[] ordinals) {
        EnumSet<E> set = EnumSet.noneOf(type);
        E[] values = type.getEnumConstants();
        for (int ordinal : ordinals) {
            if (ordinal >= 0 && ordinal < values.length) {
                set.add(values[ordinal]);
            }
        }
        return set;
    }

    private static <E extends Enum<E>> EnumSet<E> readEnumSet(CompoundTag tag, String key, Class<E> type, EnumSet<E> fallback) {
        if (!tag.contains(key, Tag.TAG_LIST)) {
            return EnumSet.copyOf(fallback);
        }
        EnumSet<E> set = EnumSet.noneOf(type);
        E[] values = type.getEnumConstants();
        ListTag list = tag.getList(key, Tag.TAG_INT);
        for (Tag entry : list) {
            int ordinal = ((IntTag) entry).getAsInt();
            if (ordinal >= 0 && ordinal < values.length) {
                set.add(values[ordinal]);
            }
        }
        return set.isEmpty() ? EnumSet.copyOf(fallback) : set;
    }

    private static <E extends Enum<E>> void writeOrdinals(CompoundTag tag, String key, Set<E> values) {
        ListTag list = new ListTag();
        for (E value : values) {
            list.add(IntTag.valueOf(value.ordinal()));
        }
        tag.put(key, list);
    }

    private static int[] ordinals(Set<? extends Enum<?>> values) {
        return values.stream().mapToInt(Enum::ordinal).toArray();
    }

    private static CompoundTag data(Player player) {
        CompoundTag persistent = player.getPersistentData();
        if (!persistent.contains(ROOT_TAG, Tag.TAG_COMPOUND)) {
            persistent.put(ROOT_TAG, new CompoundTag());
        }
        CompoundTag tag = persistent.getCompound(ROOT_TAG);
        if (player instanceof ServerPlayer) {
            SkillCooldownHelper.clampCooldown(player, tag, ELBOW_NEXT_RECHARGE, ELBOW_RECHARGE_TICKS);
            SkillCooldownHelper.clampCooldown(player, tag, CORE_COOLDOWN_UNTIL, CORE_COOLDOWN_TICKS);
            SkillCooldownHelper.clampDeadline(tag, STEEL_COOLDOWN_UNTIL, SkillCooldownHelper.now(player), Math.max(STEEL_BODY_MIN_COOLDOWN_TICKS, Math.max(1200, SkillCooldownHelper.ticks(player, 1200))));
            SkillCooldownHelper.clampCooldown(player, tag, INDESTRUCTIBLE_COOLDOWN_UNTIL, 120 * 20);
        }
        return tag;
    }

    private record ControlAffectionKey(UUID owner, UUID target) {
    }

    private static final class FlashProgress {
        private int entityId;
        private int progress;

        private FlashProgress(int entityId, int progress) {
            this.entityId = entityId;
            this.progress = progress;
        }
    }
}
