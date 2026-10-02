package com.rzy.dealt_force_skills.character.lexninjia;

import com.rzy.dealt_force_skills.advancement.DfsAchievements;
import com.rzy.dealt_force_skills.entity.VisualSmokeCloudEntity;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.CharacterSelectionManager;
import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.S2C_SyncLexNinjiaState;
import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.registry.ModEntities;
import com.rzy.dealt_force_skills.registry.ModGameRules;
import com.rzy.dealt_force_skills.registry.ModParticles;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.skill.SkillAnimationScheduler;
import com.rzy.dealt_force_skills.skill.SkillCooldownHelper;
import com.rzy.dealt_force_skills.skill.SkillDamageHelper;
import com.rzy.dealt_force_skills.skill.SkillModelVisual;
import com.rzy.dealt_force_skills.skill.SkillModelVisualSync;
import com.rzy.dealt_force_skills.util.MeleeWeaponCompat;
import com.rzy.dealt_force_skills.util.RangedSoundHelper;
import com.rzy.dealt_force_skills.util.TargetingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;

public final class LexNinjiaStateManager {
    private static final String ART_CONFIG_ROOT = "characters.lex_ninjia.arts.";
    private static final String LEGACY_STATE_CONFIG_ROOT = "characters.lexninjia.lex_ninjia_state_manager.";
    public static volatile int BASE_MIND_CAPACITY = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("BASE_MIND_CAPACITY", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.lexninjia.lex_ninjia_state_manager.base_mind_capacity", 25));
    public static volatile int MAX_OVERLOAD_MIND = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("MAX_OVERLOAD_MIND", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.lexninjia.lex_ninjia_state_manager.max_overload_mind", 10));
    public static volatile int SCIENTIFIC_TOOL_MAX_LEVEL = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("SCIENTIFIC_TOOL_MAX_LEVEL", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.lexninjia.lex_ninjia_state_manager.scientific_tool_max_level", 7));
    public static volatile int SCIENTIFIC_TOOL_BASE_INPUTS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("SCIENTIFIC_TOOL_BASE_INPUTS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue(
      "characters.lexninjia.lex_ninjia_state_manager.scientific_tool_base_inputs", 5
   ));
    public static volatile int SCIENTIFIC_TOOL_BASE_PRESETS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("SCIENTIFIC_TOOL_BASE_PRESETS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue(
      "characters.lexninjia.lex_ninjia_state_manager.scientific_tool_base_presets", 3
   ));
    public static final int MAX_FORCED_MIND_EXPANSIONS = Math.max(0,
            totalEquippableMindCost() - BASE_MIND_CAPACITY - MAX_OVERLOAD_MIND);
    private static volatile int INPUT_EXPIRY_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("INPUT_EXPIRY_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.lexninjia.lex_ninjia_state_manager.input_expiry_ticks", 600));
    private static volatile int MAX_STORED_COMBO_INPUTS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("MAX_STORED_COMBO_INPUTS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.lexninjia.lex_ninjia_state_manager.max_stored_combo_inputs", 12));
    private static volatile int STACK_DURATION_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("STACK_DURATION_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.lexninjia.lex_ninjia_state_manager.stack_duration_ticks", 200));
    private static volatile int DEEP_FOCUS_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("DEEP_FOCUS_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.lexninjia.lex_ninjia_state_manager.deep_focus_ticks", 20));
    private static volatile int MAX_FOUNDATION_STACKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("MAX_FOUNDATION_STACKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.lexninjia.lex_ninjia_state_manager.max_foundation_stacks", 10));
    private static volatile float BASE_LEICRA_REGEN_PER_SECOND = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("BASE_LEICRA_REGEN_PER_SECOND", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.floatValue(
      "characters.lexninjia.lex_ninjia_state_manager.base_leicra_regen_per_second", 1.0F
   ));
    private static final float HAM_BERSERK_SELF_HEALTH_COST_FRACTION = artFloatValue(
            "ham_berserk", "self_health_cost_fraction", 0.20F, "ham_berserk_self_health_cost_fraction");
    private static final float HAM_BEAST_SELF_HEALTH_COST_FRACTION = artFloatValue(
            "ham_beast", "self_health_cost_fraction", 0.50F, "ham_beast_self_health_cost_fraction");
    private static final float HAM_SHADOW_KICK_RELEASE_SELF_HEALTH_COST_FRACTION = artFloatValue(
            "ham_shadow_kick", "release_self_health_cost_fraction", 0.05F, "ham_shadow_kick_release_self_health_cost_fraction");
    private static final float BIG_PORTION_SELF_HEALTH_COST_FRACTION = artFloatValue(
            "big_portion", "self_health_cost_fraction", 0.10F, "big_portion_self_health_cost_fraction");
    private static final float HAM_KILL_ALL_SELF_DAMAGE_MAX_HEALTH_FRACTION = artFloatValue(
            "ham_kill_all", "self_damage_max_health_fraction", 4.0F, "ham_kill_all_self_damage_max_health_fraction");
    private static final float HAM_SHADOW_KICK_TICK_SELF_HEALTH_COST_FRACTION = artFloatValue(
            "ham_shadow_kick", "tick_self_health_cost_fraction", 0.025F, "ham_shadow_kick_tick_self_health_cost_fraction");
    private static final String ROOT = DealtForceSkillsMod.MODID + ".lex_ninjia";
    private static final String LEICRA = ROOT + ".leicra";
    private static final String KNOWN = ROOT + ".known";
    private static final String EQUIPPED = ROOT + ".equipped";
    private static final String MIND_EXPANSIONS = ROOT + ".mind_expansions";
    private static final String SCIENTIFIC_TOOL_LEVEL = ROOT + ".scientific_tool_level";
    private static final String SCIENTIFIC_PRESETS = ROOT + ".scientific_presets";
    private static final String FOOD_ART = ROOT + ".food_art";
    private static final String FOOD_LEICRA_REGEN_UNTIL = ROOT + ".food_leicra_regen_until";
    private static final String REVIVE_DISABLED_UNTIL = ROOT + ".revive_disabled_until";
    private static final String TACZ_GUN_ID_TAG = "GunId";
    private static final ResourceLocation HAND_SPEED_UUID = ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "lex_ninjia_state_manager.hand_speed");
    private static final ResourceLocation SPIN_SLOW_UUID = ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "lex_ninjia_state_manager.spin_slow");
    private static final String HAND_GUARD_SHIELD_TAG = "dealt_force_skills.lex_ninjia.hand_guard_shield";
    private static final float SPIN_ION_CHARGE_FRACTION_PER_SECOND = 0.05F;
    private static final float SPIN_ION_BASE_RANGE = 4.0F;
    private static final float SPIN_ION_RANGE_PER_LEICRA = 0.05F;
    private static final float SPIN_ION_BASE_WIDTH = 2.0F;
    private static final float SPIN_ION_WIDTH_PER_LEICRA = 0.01F;
    private static final double MAX_FRONT_AREA_QUERY_RANGE = 128.0D;
    private static final int SPIN_ION_FEEDBACK_INTERVAL_TICKS = 10;
    private static final Set<LexNinjiaArt> HAND_DAMAGE_ARTS = EnumSet.of(
            LexNinjiaArt.FIRE_FIST,
            LexNinjiaArt.LUOHAN_HAND,
            LexNinjiaArt.STOP_HAND,
            LexNinjiaArt.ONE_DEATH_HAND
    );
    private static final Map<UUID, RuntimeState> RUNTIME = new HashMap<>();

    private LexNinjiaStateManager() {
    }

    private static float artFloatValue(String artId, String key, float defaultValue, String legacyKey) {
        return com.rzy.dealt_force_skills.config.DealtForceConfig.floatValue(
                ART_CONFIG_ROOT + artId + "." + key,
                defaultValue,
                LEGACY_STATE_CONFIG_ROOT + legacyKey);
    }

    private static int totalEquippableMindCost() {
        int total = 0;
        for (LexNinjiaArt art : LexNinjiaArt.values()) {
            if (!art.defaultKnown() && !art.cookRecipe()) {
                total += art.mindCost();
            }
        }
        return total;
    }

    public static boolean isLexNinjia(Player player) {
        return CharacterSelectionManager.getSelectedCharacterId(player)
                .map(ModCharacters.LEX_NINJIA_ID::equals)
                .orElse(false);
    }

    public static void initializeIfNeeded(ServerPlayer player) {
        CompoundTag tag = player.getPersistentData();
        CompoundTag known = tag.getCompound(KNOWN);
        boolean changed = false;
        for (LexNinjiaArt art : LexNinjiaArt.values()) {
            if (art.defaultKnown() && !known.getBoolean(art.id())) {
                known.putBoolean(art.id(), true);
                changed = true;
            }
        }
        if (changed || !tag.contains(KNOWN)) {
            tag.put(KNOWN, known);
        }
        CompoundTag equipped = tag.getCompound(EQUIPPED);
        for (LexNinjiaArt art : LexNinjiaArt.values()) {
            if (art.defaultKnown() && !equipped.getBoolean(art.id())) {
                equipped.putBoolean(art.id(), true);
            }
        }
        tag.put(EQUIPPED, equipped);
        if (!tag.contains(LEICRA)) {
            tag.putFloat(LEICRA, maxLeicra(player));
        }
    }

    public static void copyState(Player original, Player replacement) {
        CompoundTag from = original.getPersistentData();
        CompoundTag to = replacement.getPersistentData();
        if (from.contains(KNOWN, Tag.TAG_COMPOUND)) {
            to.put(KNOWN, from.getCompound(KNOWN).copy());
        }
        if (from.contains(EQUIPPED, Tag.TAG_COMPOUND)) {
            to.put(EQUIPPED, from.getCompound(EQUIPPED).copy());
        }
        to.putInt(MIND_EXPANSIONS, Mth.clamp(from.getInt(MIND_EXPANSIONS), 0, MAX_FORCED_MIND_EXPANSIONS));
        if (from.contains(SCIENTIFIC_TOOL_LEVEL, Tag.TAG_INT)) {
            to.putInt(SCIENTIFIC_TOOL_LEVEL,
                    Mth.clamp(from.getInt(SCIENTIFIC_TOOL_LEVEL), 0, SCIENTIFIC_TOOL_MAX_LEVEL));
        }
        if (from.contains(SCIENTIFIC_PRESETS, Tag.TAG_COMPOUND)) {
            to.put(SCIENTIFIC_PRESETS, from.getCompound(SCIENTIFIC_PRESETS).copy());
        }
        to.putFloat(LEICRA, Math.max(0.0F, from.getFloat(LEICRA)));
        RUNTIME.remove(original.getUUID());
    }

    public static void onDeselected(ServerPlayer player) {
        RuntimeState state = state(player);
        restoreShield(player, state);
        state.clearCombatRuntime(SkillCooldownHelper.now(player));
        player.getAttribute(Attributes.MOVEMENT_SPEED).removeModifier(HAND_SPEED_UUID);
        player.getAttribute(Attributes.MOVEMENT_SPEED).removeModifier(SPIN_SLOW_UUID);
        syncToClient(player);
    }

    public static void clearRuntimeOnDeath(ServerPlayer player) {
        RuntimeState state = state(player);
        restoreShield(player, state);
        state.clearAll();
        syncToClient(player);
    }

    public static void clearRuntimeOnLogout(ServerPlayer player) {
        if (player == null) {
            return;
        }
        RuntimeState state = RUNTIME.remove(player.getUUID());
        if (state != null) {
            state.noRetaliationUntil = 0L;
            restoreShield(player, state);
        }
        var movement = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movement != null) {
            movement.removeModifier(HAND_SPEED_UUID);
            movement.removeModifier(SPIN_SLOW_UUID);
        }
    }

    public static void tick(ServerPlayer player) {
        initializeIfNeeded(player);
        RuntimeState state = state(player);
        long now = SkillCooldownHelper.now(player);
        state.pruneInputs(now);
        regenerateLeicra(player, state, now);
        tickFoundationStacks(player, state, now);
        tickPersistentEffects(player, state, now);
        if (now % 10L == 0L && (state.hamPowerUntil > now
                || state.hamBerserkUntil > now
                || state.hamShadowKickUntil > now
                || state.hamKillReleaseTick > 0L)) {
            SkillModelVisualSync.play(player, SkillModelVisual.LEX_HAM_PRESENCE, 14);
        }
        state.prepared = matchPrepared(player, state, now).orElse(null);
        tickSpinIonCharge(player, state, now);
        syncToClient(player);
    }

    public static void onKill(ServerPlayer player) {
        if (!isLexNinjia(player)) {
            return;
        }
        RuntimeState state = state(player);
        long now = SkillCooldownHelper.now(player);
        if (state.hamBerserkUntil > now) {
            state.hamBerserkUntil += 5L * 20L;
            state.hamBerserkStacks = Math.min(8, state.hamBerserkStacks + 1);
        }
    }

    public static void onHamBeastKill(ServerPlayer owner, Wolf wolf) {
        if (!isLexNinjia(owner)) {
            return;
        }
        RuntimeState state = state(owner);
        if (state.hamBeastId == null || !state.hamBeastId.equals(wolf.getUUID())) {
            return;
        }
        state.hamBeastStacks = Math.min(8, state.hamBeastStacks + 1);
        int damageTicks = effectTicks(wolf, MobEffects.DAMAGE_BOOST) + 10 * 20;
        int speedTicks = effectTicks(wolf, MobEffects.MOVEMENT_SPEED) + 10 * 20;
        wolf.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, damageTicks,
                Math.min(127, 99 + state.hamBeastStacks), true, true));
        wolf.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, speedTicks,
                2 + state.hamBeastStacks / 2, true, true));
        if (wolf.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
            wolf.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(
                    wolf.getAttributeBaseValue(Attributes.ATTACK_DAMAGE) + 2.0D);
        }
    }

    private static int effectTicks(LivingEntity entity, net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect) {
        MobEffectInstance instance = entity.getEffect(effect);
        return instance == null ? 0 : instance.getDuration();
    }

    public static boolean useFoundationSkill(ServerPlayer player, LexNinjiaComboInput input, int cost) {
        initializeIfNeeded(player);
        if (isSleeping(player)) {
            return true;
        }
        RuntimeState state = state(player);
        long now = SkillCooldownHelper.now(player);
        if (!applyFoundationInput(player, state, input, cost, now)) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.lex_ninjia.not_enough_leicra"), true);
            syncToClient(player);
            return true;
        }
        syncToClient(player);
        return true;
    }

    private static boolean applyFoundationInput(ServerPlayer player, RuntimeState state,
                                                LexNinjiaComboInput input, int cost, long now) {
        if (!spendLeicra(player, cost)) {
            return false;
        }
        if (input == LexNinjiaComboInput.HAND) {
            state.handStacks = Math.min(MAX_FOUNDATION_STACKS, state.handStacks + 1);
            state.handLastTick = now;
            applyHandSpeed(player, state.handStacks);
        } else if (input == LexNinjiaComboInput.BLADE) {
            state.bladeStacks = Math.min(MAX_FOUNDATION_STACKS, state.bladeStacks + 1);
            state.bladeLastTick = now;
        } else if (input == LexNinjiaComboInput.HARMONY) {
            state.harmonyStacks = Math.min(MAX_FOUNDATION_STACKS, state.harmonyStacks + 1);
            state.harmonyLastTick = now;
        }
        addInput(player, state, input, now);
        state.prepared = matchPrepared(player, state, now).orElse(null);
        return true;
    }

    public static void handleInput(ServerPlayer player, LexNinjiaInputAction action) {
        if (!isLexNinjia(player)) {
            return;
        }
        RuntimeState state = state(player);
        long now = SkillCooldownHelper.now(player);
        if (state.sleepUntil > now) {
            syncToClient(player);
            return;
        }
        switch (action) {
            case SNEAK_PRESS -> {
                state.sneakPressTick = now;
                state.longSneakRecorded = false;
            }
            case SNEAK_RELEASE -> handleSneakRelease(player, state, now);
            case JUMP -> {
                addInput(player, state, LexNinjiaComboInput.JUMP, now);
                state.prepared = matchPrepared(player, state, now).orElse(null);
            }
            case RIGHT_PRESS -> {
                state.rightPressTick = Math.max(1L, now);
                state.rightChargeSpent = 0.0F;
            }
            case RIGHT_RELEASE -> handleRightRelease(player, state, now);
            case COOK -> craftCookArt(player, state);
        }
        syncToClient(player);
    }

    public static void handleAttackEntity(ServerPlayer player, LivingEntity target) {
        if (!isLexNinjia(player) || target == player || !target.isAlive()) {
            return;
        }
        RuntimeState state = state(player);
        long now = SkillCooldownHelper.now(player);
        state.prepared = matchPrepared(player, state, now).orElse(null);
        if (state.prepared != null && state.prepared.releaseTrigger() == LexNinjiaReleaseTrigger.LEFT_CLICK) {
            releasePrepared(player, state, state.prepared, target, now);
        } else {
            state.lastMeleeTarget = target.getUUID();
        }
        syncToClient(player);
    }

    public static void handleLivingHurt(LivingIncomingDamageEvent event) {
        if (event.getAmount() <= 0.0F || event.getSource().is(SkillDamageHelper.TRUE_SKILL_DAMAGE)) {
            return;
        }
        LivingEntity target = event.getEntity();
        Entity attackerEntity = event.getSource().getEntity();
        if (target instanceof ServerPlayer targetPlayer && isLexNinjia(targetPlayer)) {
            handleIncomingDamage(targetPlayer, event);
        }
        if (attackerEntity instanceof ServerPlayer attacker && isLexNinjia(attacker)) {
            handleOutgoingDamage(attacker, target, event);
        }
        if (attackerEntity instanceof LivingEntity attacker) {
            handleNoRetaliation(attacker, target, event);
        }
    }

    public static boolean handleHandGuardAttack(ServerPlayer player, DamageSource source) {
        RuntimeState state = RUNTIME.get(player.getUUID());
        long now = SkillCooldownHelper.now(player);
        if (state == null || state.shieldUntil <= now || !player.isUsingItem()
                || !isHandGuardShield(player.getUseItem())) {
            return false;
        }
        Entity attackerEntity = source.getEntity();
        if (!(attackerEntity instanceof LivingEntity attacker)
                || !(attacker.getMainHandItem().getItem() instanceof AxeItem)) {
            return false;
        }
        Vec3 sourcePosition = source.getSourcePosition();
        if (sourcePosition == null) {
            return false;
        }
        Vec3 sourceToPlayer = sourcePosition.vectorTo(player.position()).normalize();
        Vec3 horizontalIncoming = new Vec3(sourceToPlayer.x, 0.0D, sourceToPlayer.z);
        if (horizontalIncoming.lengthSqr() < 0.0001D
                || horizontalIncoming.normalize().dot(player.getViewVector(1.0F)) >= 0.0D) {
            return false;
        }
        player.getCooldowns().removeCooldown(Items.SHIELD);
        return true;
    }

    public static boolean shouldPreventHandGuardToss(ServerPlayer player, ItemStack stack) {
        RuntimeState state = RUNTIME.get(player.getUUID());
        return state != null && state.shieldUntil > SkillCooldownHelper.now(player) && isHandGuardShield(stack);
    }

    public static void handleFoodFinished(LivingEntity entity, ItemStack stack) {
        if (!(entity instanceof ServerPlayer player) || stack.isEmpty() || !stack.has(DataComponents.CUSTOM_DATA)) {
            return;
        }
        String artId = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString(FOOD_ART);
        if (artId.isBlank()) {
            return;
        }
        LexNinjiaArt art = LexNinjiaArt.byId(artId).orElse(null);
        if (art == null) {
            return;
        }
        applyCookFoodEffect(player, art);
        syncToClient(player);
    }

    public static void handleBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !isLexNinjia(player)
                || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        RuntimeState state = state(player);
        if (state.fertilizerUntil <= level.getGameTime()) {
            return;
        }
        growPlacedCrop(level, event.getPos());
    }

    public static void onPlayerAction(LivingEntity entity) {
        if (!entity.level().isClientSide) {
            triggerSnakePoison(entity);
        }
    }

    public static boolean isKnown(Player player, LexNinjiaArt art) {
        return art.defaultKnown() || player.getPersistentData().getCompound(KNOWN).getBoolean(art.id());
    }

    public static boolean isEquipped(Player player, LexNinjiaArt art) {
        return art.defaultKnown()
                || art.cookRecipe()
                || player.getPersistentData().getCompound(EQUIPPED).getBoolean(art.id());
    }

    public static boolean canDisplayInShop(Player player, LexNinjiaArt art) {
        if (art.defaultKnown()) {
            return true;
        }
        return !art.hamForbidden() || allNonHamArtsKnown(player);
    }

    public static void learnArt(ServerPlayer player, LexNinjiaArt art) {
        CompoundTag known = player.getPersistentData().getCompound(KNOWN);
        known.putBoolean(art.id(), true);
        player.getPersistentData().put(KNOWN, known);
        if (art == LexNinjiaArt.HAM_FRIEND) {
            setEquipped(player, art, true);
        }
        syncToClient(player);
    }

    public static boolean setEquipped(ServerPlayer player, LexNinjiaArt art, boolean equip) {
        if (art.defaultKnown() || art.cookRecipe()) {
            return true;
        }
        if (art == LexNinjiaArt.HAM_FRIEND && !equip) {
            return false;
        }
        CompoundTag equipped = player.getPersistentData().getCompound(EQUIPPED);
        if (!equip) {
            equipped.remove(art.id());
            player.getPersistentData().put(EQUIPPED, equipped);
            syncToClient(player);
            return true;
        }
        int used = mindUsed(player);
        if (!equipped.getBoolean(art.id())) {
            used += art.mindCost();
        }
        if (used > maxMindLoad(player)) {
            return false;
        }
        equipped.putBoolean(art.id(), true);
        player.getPersistentData().put(EQUIPPED, equipped);
        syncToClient(player);
        return true;
    }

    public static int forcedMindExpansions(Player player) {
        return Mth.clamp(player.getPersistentData().getInt(MIND_EXPANSIONS),
                0, MAX_FORCED_MIND_EXPANSIONS);
    }

    public static void expandMind(ServerPlayer player) {
        int current = forcedMindExpansions(player);
        if (current < MAX_FORCED_MIND_EXPANSIONS) {
            player.getPersistentData().putInt(MIND_EXPANSIONS, current + 1);
        }
        syncToClient(player);
    }

    public static int mindCapacity(Player player) {
        return BASE_MIND_CAPACITY + forcedMindExpansions(player);
    }

    public static int maxMindLoad(Player player) {
        return mindCapacity(player) + MAX_OVERLOAD_MIND;
    }

    public static int mindOverload(Player player) {
        return Math.max(0, mindUsed(player) - mindCapacity(player));
    }

    public static int mindUsed(Player player) {
        CompoundTag equipped = player.getPersistentData().getCompound(EQUIPPED);
        int used = 0;
        for (LexNinjiaArt art : LexNinjiaArt.values()) {
            if (!art.defaultKnown() && !art.cookRecipe() && equipped.getBoolean(art.id())) {
                used += art.mindCost();
            }
        }
        return used;
    }

    public static CompoundTag shopData(ServerPlayer player) {
        initializeIfNeeded(player);
        CompoundTag data = new CompoundTag();
        data.put(KNOWN, player.getPersistentData().getCompound(KNOWN).copy());
        data.put(EQUIPPED, player.getPersistentData().getCompound(EQUIPPED).copy());
        data.put("Known", player.getPersistentData().getCompound(KNOWN).copy());
        data.put("Equipped", player.getPersistentData().getCompound(EQUIPPED).copy());
        data.putInt("MindExpansions", forcedMindExpansions(player));
        data.putInt("MindExpansionMax", MAX_FORCED_MIND_EXPANSIONS);
        data.putInt("MindCapacity", mindCapacity(player));
        data.putInt("MindUsed", mindUsed(player));
        data.putBoolean("HamVisible", allNonHamArtsKnown(player));
        data.putInt("ScientificToolLevel", scientificToolLevel(player));
        data.putInt("ScientificMaxInputs", scientificMaxInputs(player));
        data.putInt("ScientificMaxPresets", scientificMaxPresets(player));
        data.put("ScientificPresets", scientificPresets(player).copy());
        return data;
    }

    public static void syncToClient(ServerPlayer player) {
        if (!isLexNinjia(player)) {
            return;
        }
        RuntimeState state = state(player);
        long now = SkillCooldownHelper.now(player);
        NetworkHandler.sendToPlayer(new S2C_SyncLexNinjiaState(
                leicra(player),
                maxLeicra(player),
                state.handStacks,
                state.bladeStacks,
                state.harmonyStacks,
                state.prepared == null ? "" : state.prepared.id(),
                state.prepared != null && canAffordRelease(player, state.prepared),
                comboText(state, now),
                mindUsed(player),
                mindCapacity(player),
                Math.max(0, (int) (state.deathFlameUntil - now)),
                state.deathFlameOverflow,
                Math.max(0, (int) (state.hamPowerUntil - now)),
                Math.max(0, (int) (state.hamBerserkUntil - now)),
                scientificToolLevel(player)
        ), player);
    }

    public static int scientificToolLevel(Player player) {
        CompoundTag tag = player.getPersistentData();
        return tag.contains(SCIENTIFIC_TOOL_LEVEL, Tag.TAG_INT)
                ? Mth.clamp(tag.getInt(SCIENTIFIC_TOOL_LEVEL), 0, SCIENTIFIC_TOOL_MAX_LEVEL)
                : -1;
    }

    public static boolean hasScientificTool(Player player) {
        return scientificToolLevel(player) >= 0;
    }

    public static void buyScientificTool(ServerPlayer player) {
        if (!hasScientificTool(player)) {
            player.getPersistentData().putInt(SCIENTIFIC_TOOL_LEVEL, 0);
        }
        syncToClient(player);
    }

    public static void clearPurchaseProgress(ServerPlayer player) {
        CompoundTag tag = player.getPersistentData();
        tag.remove(KNOWN);
        tag.remove(EQUIPPED);
        tag.remove(MIND_EXPANSIONS);
        tag.remove(SCIENTIFIC_TOOL_LEVEL);
        tag.remove(SCIENTIFIC_PRESETS);
        if (isLexNinjia(player)) {
            initializeIfNeeded(player);
            syncToClient(player);
        }
    }

    public static void upgradeScientificTool(ServerPlayer player) {
        int level = scientificToolLevel(player);
        if (level >= 0 && level < SCIENTIFIC_TOOL_MAX_LEVEL) {
            player.getPersistentData().putInt(SCIENTIFIC_TOOL_LEVEL, level + 1);
        }
        syncToClient(player);
    }

    public static int scientificMaxInputs(Player player) {
        return hasScientificTool(player)
                ? SCIENTIFIC_TOOL_BASE_INPUTS + scientificToolLevel(player)
                : 0;
    }

    public static int scientificMaxPresets(Player player) {
        return hasScientificTool(player)
                ? SCIENTIFIC_TOOL_BASE_PRESETS + scientificToolLevel(player)
                : 0;
    }

    public static CompoundTag scientificPresets(Player player) {
        return player.getPersistentData().getCompound(SCIENTIFIC_PRESETS);
    }

    public static void saveScientificPreset(ServerPlayer player, int slot, String name,
                                            List<LexNinjiaComboInput> inputs) {
        if (!hasScientificTool(player) || slot < 0 || slot >= scientificMaxPresets(player)) {
            return;
        }
        int maxInputs = scientificMaxInputs(player);
        List<LexNinjiaComboInput> sanitizedInputs = new ArrayList<>();
        for (LexNinjiaComboInput input : inputs) {
            if (input == null || sanitizedInputs.size() >= maxInputs) {
                break;
            }
            sanitizedInputs.add(input);
            if (input == LexNinjiaComboInput.LEFT_CLICK || input == LexNinjiaComboInput.RIGHT_RELEASE) {
                break;
            }
        }
        CompoundTag presets = scientificPresets(player);
        LexNinjiaPreset.write(presets, slot, new LexNinjiaPreset(name, sanitizedInputs), maxInputs);
        player.getPersistentData().put(SCIENTIFIC_PRESETS, presets);
    }

    public static boolean executeScientificPreset(ServerPlayer player, int slot) {
        if (!isLexNinjia(player) || player.isCreative() || !hasScientificTool(player)
                || slot < 0 || slot >= scientificMaxPresets(player)) {
            return false;
        }
        LexNinjiaPreset preset = LexNinjiaPreset.read(scientificPresets(player), slot, scientificMaxInputs(player));
        if (preset.inputs().isEmpty()) {
            return false;
        }
        RuntimeState state = state(player);
        long now = SkillCooldownHelper.now(player);
        state.inputs.clear();
        state.prepared = null;
        for (LexNinjiaComboInput input : preset.inputs()) {
            if (!applyPresetInput(player, state, input, now)) {
                syncToClient(player);
                return false;
            }
        }
        state.prepared = matchPrepared(player, state, now).orElse(null);
        if (state.prepared != null && state.prepared.releaseTrigger() == LexNinjiaReleaseTrigger.MANUAL) {
            releasePrepared(player, state, state.prepared, null, now);
        }
        state.presetDisplayText = preset.inputs().stream()
                .map(input -> input.name().toLowerCase(java.util.Locale.ROOT))
                .collect(java.util.stream.Collectors.joining(" + "));
        state.presetDisplayUntil = now + 60L;
        syncToClient(player);
        return true;
    }

    private static boolean applyPresetInput(ServerPlayer player, RuntimeState state,
                                            LexNinjiaComboInput input, long now) {
        return switch (input) {
            case HAND -> applyFoundationInput(player, state, input, 3, now);
            case BLADE -> applyFoundationInput(player, state, input, 3, now);
            case HARMONY -> applyFoundationInput(player, state, input, 5, now);
            case SNEAK, JUMP -> {
                addInput(player, state, input, now);
                yield true;
            }
            case RIGHT_RELEASE -> {
                state.prepared = matchPrepared(player, state, now).orElse(null);
                if (state.prepared == null
                        || state.prepared.releaseTrigger() != LexNinjiaReleaseTrigger.RIGHT_RELEASE) {
                    yield false;
                }
                releasePrepared(player, state, state.prepared, null, now);
                yield true;
            }
            case LEFT_CLICK -> {
                state.prepared = matchPrepared(player, state, now).orElse(null);
                LivingEntity target = findPresetTarget(player);
                if (state.prepared == null
                        || state.prepared.releaseTrigger() != LexNinjiaReleaseTrigger.LEFT_CLICK
                        || target == null) {
                    yield false;
                }
                releasePrepared(player, state, state.prepared, target, now);
                yield true;
            }
        };
    }

    private static LivingEntity findPresetTarget(ServerPlayer player) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        return player.level().getEntitiesOfClass(LivingEntity.class,
                        player.getBoundingBox().inflate(6.0D),
                        target -> TargetingUtil.isHostileLivingFor(player, target) && player.hasLineOfSight(target))
                .stream()
                .filter(target -> {
                    Vec3 direction = target.getEyePosition().subtract(eye);
                    return direction.lengthSqr() > 0.0001D
                            && direction.normalize().dot(look) >= 0.90D;
                })
                .min(Comparator.comparingDouble(target -> target.distanceToSqr(player)))
                .orElse(null);
    }

    public static float leicra(ServerPlayer player) {
        return Mth.clamp(player.getPersistentData().getFloat(LEICRA), 0.0F, maxLeicra(player));
    }

    public static float maxLeicra(ServerPlayer player) {
        float base = com.rzy.dealt_force_skills.config.DealtForceConfig.floatValue(
                "experience_growth.lex_ninjia.base_max_leicra", 200.0F);
        float growth = com.rzy.dealt_force_skills.config.DealtForceConfig.floatValue(
                "experience_growth.lex_ninjia.max_leicra_per_level", 10.0F);
        return base + ModGameRules.effectiveExperienceLevel(player) * growth;
    }

    public static boolean isTaczGunStack(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = cd != null ? cd.copyTag() : null;
        if (tag != null && tag.contains(TACZ_GUN_ID_TAG)) {
            return true;
        }
        return stack.getItem().getClass().getName().startsWith("com.tacz.guns.");
    }

    private static RuntimeState state(Player player) {
        return RUNTIME.computeIfAbsent(player.getUUID(), ignored -> new RuntimeState());
    }

    private static void regenerateLeicra(ServerPlayer player, RuntimeState state, long now) {
        float max = maxLeicra(player);
        float perTick = BASE_LEICRA_REGEN_PER_SECOND / 20.0F;
        if (isMovingForward(player, state)) {
            perTick += max * 0.02F / 20.0F;
        }
        if (player.isShiftKeyDown()) {
            if (state.sneakHeldSince <= 0L) {
                state.sneakHeldSince = now;
            }
            if (now - state.sneakHeldSince >= DEEP_FOCUS_TICKS) {
                perTick += max * 0.04F / 20.0F;
                if (!state.longSneakRecorded) {
                    addInput(player, state, LexNinjiaComboInput.SNEAK, now);
                    state.longSneakRecorded = true;
                }
            }
        } else {
            state.sneakHeldSince = 0L;
            state.longSneakRecorded = false;
        }
        if (now <= player.getPersistentData().getLong(FOOD_LEICRA_REGEN_UNTIL)) {
            perTick *= 3.0F;
        }
        setLeicra(player, Math.min(max, leicra(player) + perTick));
    }

    private static boolean isMovingForward(ServerPlayer player, RuntimeState state) {
        Vec3 look = new Vec3(player.getLookAngle().x, 0.0D, player.getLookAngle().z);
        Vec3 movement = player.position().subtract(state.lastPosition);
        state.lastPosition = player.position();
        if (look.lengthSqr() < 0.0001D || movement.horizontalDistanceSqr() < 0.00004D) {
            return false;
        }
        return movement.normalize().dot(look.normalize()) > 0.45D;
    }

    private static void tickFoundationStacks(ServerPlayer player, RuntimeState state, long now) {
        if (state.handStacks > 0 && now - state.handLastTick > STACK_DURATION_TICKS) {
            state.handStacks--;
            state.handLastTick = now;
            applyHandSpeed(player, state.handStacks);
        }
        if (state.bladeStacks > 0 && now - state.bladeLastTick > STACK_DURATION_TICKS) {
            state.bladeStacks--;
            state.bladeLastTick = now;
        }
        if (state.harmonyStacks > 0 && now - state.harmonyLastTick > STACK_DURATION_TICKS) {
            state.harmonyStacks--;
            state.harmonyLastTick = now;
        }
    }

    private static void tickPersistentEffects(ServerPlayer player, RuntimeState state, long now) {
        if (state.sleepUntil > now) {
            player.stopUsingItem();
            player.setSprinting(false);
            Vec3 movement = player.getDeltaMovement();
            player.setDeltaMovement(0.0D, Math.min(0.0D, movement.y), 0.0D);
            player.addEffect(new MobEffectInstance(ModEffects.STUN, com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.lexninjia.lex_ninjia_state_manager.effect.stun.2.duration_ticks", 5), com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.lexninjia.lex_ninjia_state_manager.effect.stun.2.amplifier", 0), true, false));
        } else if (state.sleepRewardPending) {
            state.sleepRewardPending = false;
            state.sleepUntil = 0L;
            player.heal(player.getMaxHealth());
            setLeicra(player, Math.min(maxLeicra(player), leicra(player) + maxLeicra(player) * 0.50F));
        }
        if (state.deathFlameUntil > now) {
            removeHarmfulEffects(player);
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.lexninjia.lex_ninjia_state_manager.effect.fire_resistance.3.duration_ticks", 30), com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.lexninjia.lex_ninjia_state_manager.effect.fire_resistance.3.amplifier", 0), true, false));
            player.serverLevel().sendParticles(ModParticles.D_WOLF_LARGE_SMOKE.get(),
                    player.getX(), player.getY() + 1.0D, player.getZ(), 1, 0.7D, 0.7D, 0.7D, 0.02D);
        } else if (state.deathFlameOverflow > 0.0F) {
            float damage = state.deathFlameOverflow + 1.0F;
            state.deathFlameOverflow = 0.0F;
            player.hurt(SkillDamageHelper.trueDamage(player.serverLevel(), player, player), damage);
        }
        if (state.returnHandUntil > now) {
            removeHarmfulEffects(player);
            if (now % 20L == 0L) {
                player.heal(Math.max(1.0F, player.getMaxHealth() * 0.04F));
            }
        }
        if (state.ironRainUntil > now && now % 6L == 0L) {
            rainIronSwords(player);
        }
        if (state.noRetaliationUntil > 0L && state.noRetaliationUntil <= now) {
            state.noRetaliationUntil = 0L;
        }
        if (state.shieldUntil > now) {
            enforceHandGuardShield(player);
            player.getCooldowns().removeCooldown(Items.SHIELD);
        } else if (state.shieldUntil > 0L) {
            restoreShield(player, state);
        }
        tickWallBlocks(player.serverLevel(), state, now);
        if (state.tenMeterReleaseTick > 0L) {
            tickTenMeterSword(player, state, now);
        }
        if (state.hamKillReleaseTick > 0L) {
            tickHamKillAll(player, state, now);
        }
        if (state.hamShadowKickUntil > now) {
            tickHamShadowKick(player, state, now);
        }
        if (state.hamBerserkUntil > now) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.lexninjia.lex_ninjia_state_manager.effect.movement_speed.4.duration_ticks", 30),
                    1 + state.hamBerserkStacks / 3, true, false));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.lexninjia.lex_ninjia_state_manager.effect.damage_boost.5.duration_ticks", 30),
                    2 + state.hamBerserkStacks, true, false));
        }
        if (state.shadowCloneUntil > now && now % 10L == 0L) {
            spawnShadowCloneVisuals(player, false);
        }
        tickSlashQueue(player, state, now);
    }

    private static void handleSneakRelease(ServerPlayer player, RuntimeState state, long now) {
        long held = state.sneakPressTick <= 0L ? 0L : now - state.sneakPressTick;
        state.sneakPressTick = 0L;
        if (held >= DEEP_FOCUS_TICKS) {
            state.longSneakRecorded = false;
            state.prepared = matchPrepared(player, state, now).orElse(null);
            return;
        }
        state.prepared = matchPrepared(player, state, now).orElse(null);
        if (state.prepared != null && state.prepared.releaseTrigger() == LexNinjiaReleaseTrigger.MANUAL) {
            releasePrepared(player, state, state.prepared, null, now);
            return;
        }
        addInput(player, state, LexNinjiaComboInput.SNEAK, now);
        state.prepared = matchPrepared(player, state, now).orElse(null);
    }

    private static void handleRightRelease(ServerPlayer player, RuntimeState state, long now) {
        state.prepared = matchPrepared(player, state, now).orElse(null);
        state.rightPressTick = 0L;
        if (state.prepared != null && state.prepared.releaseTrigger() == LexNinjiaReleaseTrigger.RIGHT_RELEASE) {
            boolean releasingSpinIon = state.prepared == LexNinjiaArt.SPIN_ION_HAND;
            releasePrepared(player, state, state.prepared, null, now);
            if (!releasingSpinIon) {
                state.rightChargeSpent = 0.0F;
            }
        } else {
            state.rightChargeSpent = 0.0F;
        }
        player.getAttribute(Attributes.MOVEMENT_SPEED).removeModifier(SPIN_SLOW_UUID);
    }

    private static void tickSpinIonCharge(ServerPlayer player, RuntimeState state, long now) {
        if (state.rightPressTick <= 0L || state.prepared != LexNinjiaArt.SPIN_ION_HAND) {
            return;
        }
        float remainingUsefulCharge = maxUsefulSpinIonCharge() - state.rightChargeSpent;
        float available = leicra(player);
        if (remainingUsefulCharge > 0.0F && available > 0.0F) {
            float requested = maxLeicra(player) * SPIN_ION_CHARGE_FRACTION_PER_SECOND / 20.0F;
            if (!Float.isFinite(requested) || requested <= 0.0F) {
                requested = Math.min(available, remainingUsefulCharge);
            }
            float spent = Math.min(Math.min(requested, available), remainingUsefulCharge);
            if (spent > 0.0F) {
                setLeicra(player, available - spent);
                state.rightChargeSpent += spent;
            }
        }
        if (now % SPIN_ION_FEEDBACK_INTERVAL_TICKS == 0L) {
            float distance = spinIonRange(state.rightChargeSpent);
            player.displayClientMessage(Component.translatable(
                    "message.dealt_force_skills.lex_ninjia.spin_ion_charging",
                    Math.round(state.rightChargeSpent * 10.0F) / 10.0F,
                    Math.round(distance * 10.0F) / 10.0F), true);
            float progress = Mth.clamp(state.rightChargeSpent / maxUsefulSpinIonCharge(), 0.0F, 1.0F);
            RangedSoundHelper.playFollowingPlayer(player, SoundEvents.EXPERIENCE_ORB_PICKUP,
                    SoundSource.PLAYERS, 0.35F, 0.7F + progress * 0.8F, 24.0D);
        }
    }

    private static void addInput(ServerPlayer player, RuntimeState state, LexNinjiaComboInput input, long now) {
        state.inputs.add(new InputEntry(input, now));
        state.pruneInputs(now);
        state.trimInputs();
        state.prepared = matchPrepared(player, state, now).orElse(null);
    }

    private static Optional<LexNinjiaArt> matchPrepared(ServerPlayer player, RuntimeState state, long now) {
        state.pruneInputs(now);
        List<LexNinjiaComboInput> inputs = state.inputs.stream().map(InputEntry::input).toList();
        LexNinjiaArt best = null;
        for (LexNinjiaArt art : LexNinjiaArt.values()) {
            List<LexNinjiaComboInput> releaseCombo = comboBeforeReleaseTrigger(art);
            if (releaseCombo.isEmpty() || !isKnown(player, art) || !isEquipped(player, art)) {
                continue;
            }
            if (art.hamForbidden() && !allNonHamArtsKnown(player)) {
                continue;
            }
            if (!endsWith(inputs, releaseCombo)) {
                continue;
            }
            if (best == null
                    || releaseCombo.size() > comboBeforeReleaseTrigger(best).size()
                    || (releaseCombo.size() == comboBeforeReleaseTrigger(best).size() && art.hamForbidden() && !best.hamForbidden())) {
                best = art;
            }
        }
        return Optional.ofNullable(best);
    }

    private static List<LexNinjiaComboInput> comboBeforeReleaseTrigger(LexNinjiaArt art) {
        List<LexNinjiaComboInput> combo = art.combo();
        if (combo.isEmpty()) {
            return combo;
        }
        LexNinjiaComboInput last = combo.get(combo.size() - 1);
        if ((art.releaseTrigger() == LexNinjiaReleaseTrigger.LEFT_CLICK && last == LexNinjiaComboInput.LEFT_CLICK)
                || (art.releaseTrigger() == LexNinjiaReleaseTrigger.RIGHT_RELEASE && last == LexNinjiaComboInput.RIGHT_RELEASE)) {
            return combo.subList(0, combo.size() - 1);
        }
        return combo;
    }

    private static boolean endsWith(List<LexNinjiaComboInput> inputs, List<LexNinjiaComboInput> combo) {
        if (combo.size() > inputs.size()) {
            return false;
        }
        int offset = inputs.size() - combo.size();
        for (int i = 0; i < combo.size(); i++) {
            if (inputs.get(offset + i) != combo.get(i)) {
                return false;
            }
        }
        return true;
    }

    private static void releasePrepared(ServerPlayer player, RuntimeState state, LexNinjiaArt art, LivingEntity target, long now) {
        if (!canRelease(player, state, art, target)) {
            state.prepared = art;
            return;
        }
        if (art.leicraCost() > 0 && !spendLeicra(player, art.leicraCost())) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.lex_ninjia.not_enough_leicra"), true);
            state.prepared = art;
            return;
        }
        if (!payHamCost(player, state, art, now)) {
            state.prepared = art;
            return;
        }
        playArtSound(player, art);
        playArtVisuals(player, art, target);
        SkillModelVisual visual = modelVisualFor(art);
        if (visual != null) {
            SkillModelVisualSync.play(player, visual);
        }
        int delayTicks = visual == null ? 0 : visual.impactTick();
        if (delayTicks > 0) {
            LivingEntity releaseTarget = target;
            SkillAnimationScheduler.schedule(player, delayTicks, delayedPlayer -> {
                LivingEntity resolvedTarget = releaseTarget != null
                        && releaseTarget.isAlive()
                        && releaseTarget.level() == delayedPlayer.level()
                        ? releaseTarget
                        : null;
                if (art.releaseTrigger() == LexNinjiaReleaseTrigger.LEFT_CLICK && resolvedTarget == null) {
                    return;
                }
                applyArt(delayedPlayer, state, art, resolvedTarget, SkillCooldownHelper.now(delayedPlayer));
                DfsAchievements.onLexArtReleased(delayedPlayer, art.name(), art.cookRecipe());
                if (art.hamForbidden()) {
                    playHamEcho(delayedPlayer);
                }
            });
        } else {
            applyArt(player, state, art, target, now);
            DfsAchievements.onLexArtReleased(player, art.name(), art.cookRecipe());
            if (art.hamForbidden()) {
                playHamEcho(player);
            }
        }
        state.inputs.clear();
        state.prepared = null;
    }

    private static SkillModelVisual modelVisualFor(LexNinjiaArt art) {
        if (art == LexNinjiaArt.HAM_FRIEND) {
            return SkillModelVisual.LEX_HAM_MANIFEST;
        }
        if (art.school() == LexNinjiaSchool.HAM) {
            return SkillModelVisual.LEX_HAM_SURGE;
        }
        if (art.school() == LexNinjiaSchool.HAND) {
            return SkillModelVisual.LEX_HAND_REACH;
        }
        return null;
    }

    private static boolean canRelease(ServerPlayer player, RuntimeState state, LexNinjiaArt art, LivingEntity target) {
        if (art.requiredMaxLeicra() > 0 && maxLeicra(player) < art.requiredMaxLeicra()) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.lex_ninjia.max_leicra_required",
                    art.requiredMaxLeicra()), true);
            return false;
        }
        if (art.hamForbidden() && art != LexNinjiaArt.HAM_FRIEND && state.hamPowerUntil <= SkillCooldownHelper.now(player)) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.lex_ninjia.ham_power_required"), true);
            return false;
        }
        if (art.school() == LexNinjiaSchool.BLADE && !hasMeleeWeapon(player)) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.lex_ninjia.requires_melee_weapon"), true);
            return false;
        }
        if (art.school() == LexNinjiaSchool.HAND && !player.getMainHandItem().isEmpty()) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.lex_ninjia.requires_empty_hand"), true);
            return false;
        }
        if (art.releaseTrigger() == LexNinjiaReleaseTrigger.LEFT_CLICK && target == null) {
            return false;
        }
        if (art == LexNinjiaArt.SHADOW_SMOKE && state.shadowBladeUntil <= SkillCooldownHelper.now(player)) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.lex_ninjia.shadow_blade_required"), true);
            return false;
        }
        if (art == LexNinjiaArt.NO_NAME_BLADE && (target == null || !target.getUUID().equals(state.lastDamager))) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.lex_ninjia.last_damager_required"), true);
            return false;
        }
        if (art == LexNinjiaArt.ONE_DEATH_HAND && (target == null || !target.getUUID().equals(state.handshakeTarget))) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.lex_ninjia.handshake_target_required"), true);
            return false;
        }
        return true;
    }

    private static boolean canAffordRelease(ServerPlayer player, LexNinjiaArt art) {
        return leicra(player) >= art.leicraCost()
                && (art.requiredMaxLeicra() <= 0 || maxLeicra(player) >= art.requiredMaxLeicra());
    }

    private static boolean payHamCost(ServerPlayer player, RuntimeState state, LexNinjiaArt art, long now) {
        if (!art.hamForbidden()) {
            return true;
        }
        if (art != LexNinjiaArt.HAM_FRIEND) {
            state.hamPowerUntil = 0L;
        }
        if (art == LexNinjiaArt.HAM_BERSERK) {
            hurtSelfPercent(player, HAM_BERSERK_SELF_HEALTH_COST_FRACTION);
        } else if (art == LexNinjiaArt.HAM_BEAST) {
            hurtSelfPercent(player, HAM_BEAST_SELF_HEALTH_COST_FRACTION);
        } else if (art == LexNinjiaArt.HAM_SHADOW_KICK) {
            hurtSelfPercent(player, HAM_SHADOW_KICK_RELEASE_SELF_HEALTH_COST_FRACTION);
        }
        return player.isAlive() || art == LexNinjiaArt.HAM_KILL_ALL;
    }

    private static void applyArt(ServerPlayer player, RuntimeState state, LexNinjiaArt art, LivingEntity target, long now) {
        switch (art) {
            case ONE_WORD_CUT -> strikeFrontArea(player, 3.0D, 1.0D, 1.0D, meleeDamage(player));
            case HANDSHAKE -> applyHandshake(player, state);
            case FLASH_CUT_HAND -> {
                if (canApplyControl(player, target)) {
                    target.addEffect(new MobEffectInstance(ModEffects.TEMPEST_DISARMED, com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.lexninjia.lex_ninjia_state_manager.effect.tempest_disarmed.6.duration_ticks", 70), com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.lexninjia.lex_ninjia_state_manager.effect.tempest_disarmed.6.amplifier", 0)));
                }
            }
            case ONE_BLADE_TAUNT -> tauntNearby(player);
            case BURNING_BLADE -> {
                state.burningBladeUntil = now + 5L * 20L;
                queueSlashes(state, target, now, 4, Math.max(1.0F, meleeDamage(player)), 6L, 6L, SlashVisual.BURNING, 0.0D);
            }
            case ARASHI_CUT -> state.arashiUntil = now + 15L * 20L;
            case DEATH_FLAME_SMOKE -> {
                state.deathFlameUntil = now + 10L * 20L;
                state.deathFlameOverflow = 0.0F;
            }
            case SHADOW_BLADE -> state.shadowBladeUntil = now + 12L * 20L;
            case SHADOW_SMOKE -> shadowSmoke(player);
            case CLIFF_FALL_BLADE -> {
                if (canApplyControl(player, target)) {
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.lexninjia.lex_ninjia_state_manager.effect.movement_slowdown.7.duration_ticks", 6 * 20), com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.lexninjia.lex_ninjia_state_manager.effect.movement_slowdown.7.amplifier", 10)));
                }
            }
            case NO_NAME_BLADE -> executeTarget(player, target);
            case SHARPEN -> state.sharpenStacks = com.rzy.dealt_force_skills.config.DealtForceConfig.intValue(
                    "experience_growth.lex_ninjia.sharpen_base_stacks", 20)
                    + ModGameRules.effectiveExperienceLevel(player)
                    * com.rzy.dealt_force_skills.config.DealtForceConfig.intValue(
                    "experience_growth.lex_ninjia.sharpen_stacks_per_level", 1);
            case SHADOW_CLONE_CROSS -> {
                state.shadowCloneUntil = now + 120L * 20L;
                spawnShadowCloneVisuals(player, true);
            }
            case TEN_METER_SWORD -> startTenMeterSword(player, state, now);
            case IRON_SWORD_RAIN -> state.ironRainUntil = now + 6L * 20L;
            case FD_HAND -> state.fdHandUntil = now + 2L * 20L;
            case FIRE_FIST -> {
                hurtTrue(player, target, 10.0F + target.getMaxHealth() * 0.01F);
                target.igniteForSeconds(3);
            }
            case LUOHAN_HAND -> hurtTrue(player, target, handDamage(player, 12.0F, state));
            case REFLECT_HAND -> {
                state.reflectStacks = 4;
                state.reflectUntil = now + 60L * 20L;
            }
            case STOP_HAND -> {
                if (canApplyControl(player, target)) {
                    target.addEffect(new MobEffectInstance(ModEffects.RAPTOR_ACTION_PAUSE, com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.lexninjia.lex_ninjia_state_manager.effect.raptor_action_pause.8.duration_ticks", 4 * 20), com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.lexninjia.lex_ninjia_state_manager.effect.raptor_action_pause.8.amplifier", 0)));
                    target.addEffect(new MobEffectInstance(ModEffects.NOX_DELAYED_WOUND, com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.lexninjia.lex_ninjia_state_manager.effect.nox_delayed_wound.9.duration_ticks", 10 * 20), com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.lexninjia.lex_ninjia_state_manager.effect.nox_delayed_wound.9.amplifier", 0)));
                }
            }
            case PEA_SHOOTER -> {
                state.peaStacks = Math.min(5, state.peaStacks + 1);
                state.peaUntil = now + 15L * 20L;
            }
            case BIG_PORTION -> {
                hurtSelfPercent(player, BIG_PORTION_SELF_HEALTH_COST_FRACTION);
                if (player.isAlive()) {
                    setLeicra(player, Math.min(maxLeicra(player), leicra(player) + maxLeicra(player) * 0.50F));
                    state.fertilizerUntil = now + 30L * 20L;
                }
            }
            case GOOD_SLEEP -> {
                state.sleepUntil = now + 10L * 20L;
                state.sleepRewardPending = true;
                player.stopUsingItem();
                player.setSprinting(false);
                player.addEffect(new MobEffectInstance(ModEffects.STUN, com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.lexninjia.lex_ninjia_state_manager.effect.stun.10.duration_ticks", 5), com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.lexninjia.lex_ninjia_state_manager.effect.stun.10.amplifier", 0), true, false));
            }
            case RETURN_HAND -> state.returnHandUntil = now + 14L * 20L;
            case DOUBLE_LUOHAN -> state.doubleLuohanReady = true;
            case ION_HAND -> pullLookTarget(player);
            case SPIN_ION_HAND -> releaseSpinIon(player, state);
            case WHITE_CRANE -> state.whiteCraneReady = true;
            case SHIELD_GUARD -> equipTemporaryShield(player, state, now);
            case SAND_WALL -> createSandWall(player, state, now);
            case NO_ONE_RETALIATES -> state.noRetaliationUntil = now + NO_ONE_RETALIATES_DURATION_TICKS;
            case ONE_DEATH_HAND -> executeTarget(player, target);
            case SNAKE_POISON_HAND -> state.snakePoisonUntil = now + 20L * 20L;
            case DEATH_GOD_HAND -> summonDeathGod(player);
            case ALL_HANDS -> releaseAllHands(player, state, now);
            case HAM_FRIEND -> {
                state.hamPowerUntil = now + 600L * 20L;
                state.hamFriendPact = true;
            }
            case HAM_BERSERK -> {
                state.hamBerserkUntil = now + 30L * 20L;
                state.hamBerserkStacks = 0;
            }
            case HAM_KILL_ALL -> {
                state.hamKillReleaseTick = now + 5L * 20L;
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.lexninjia.lex_ninjia_state_manager.effect.damage_resistance.11.duration_ticks", 10 * 20), com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.lexninjia.lex_ninjia_state_manager.effect.damage_resistance.11.amplifier", 4), true, false));
            }
            case HAM_BEAST -> summonHamBeast(player, state);
            case HAM_SHADOW_KICK -> state.hamShadowKickUntil = now + 15L * 20L;
            default -> {
            }
        }
    }

    private static void craftCookArt(ServerPlayer player, RuntimeState state) {
        if (!isLexNinjia(player) || player.isCreative()) {
            return;
        }
        List<LexNinjiaArt> candidates = new ArrayList<>();
        for (LexNinjiaArt art : LexNinjiaArt.values()) {
            if (art.cookRecipe() && isKnown(player, art)) {
                candidates.add(art);
            }
        }
        candidates.sort(LexNinjiaStateManager::compareCookRecipePriority);
        for (LexNinjiaArt art : candidates) {
            int cost = cookLeicraCost(art);
            if (leicra(player) < cost) {
                continue;
            }
            if (tryConsumeCookIngredients(player, art)) {
                spendLeicra(player, cost);
                if (art != LexNinjiaArt.SOLDIER_PILL) {
                    playArtSound(player, art);
                }
                playArtVisuals(player, art, null);
                giveOrDrop(player, cookFoodStack(art));
                state.inputs.clear();
                return;
            }
        }
        player.displayClientMessage(Component.translatable("message.dealt_force_skills.lex_ninjia.no_cook_recipe"), true);
    }

    private static int compareCookRecipePriority(LexNinjiaArt left, LexNinjiaArt right) {
        int byPrice = Long.compare(right.price(), left.price());
        if (byPrice != 0) {
            return byPrice;
        }
        int byComplexity = Integer.compare(cookRecipeComplexity(right), cookRecipeComplexity(left));
        if (byComplexity != 0) {
            return byComplexity;
        }
        int byCost = Integer.compare(cookLeicraCost(right), cookLeicraCost(left));
        if (byCost != 0) {
            return byCost;
        }
        return Integer.compare(left.ordinal(), right.ordinal());
    }

    private static int cookRecipeComplexity(LexNinjiaArt art) {
        return switch (art) {
            case HOT_DRINK -> 6;
            case MC_NUGGETS, LOTUS_BOX_FOOD, MILK_BEER -> 5;
            case HAMBURGER, MILK_FRUIT_SHAKE, ROAST_MEAT_RICE -> 3;
            case SOLDIER_PILL, NANO_SNICKERS, SHRIMP_HAND -> 2;
            case COLD_COPPER -> 1;
            default -> 0;
        };
    }

    private static int cookLeicraCost(LexNinjiaArt art) {
        return switch (art) {
            case HAMBURGER, MILK_FRUIT_SHAKE, SHRIMP_HAND, ROAST_MEAT_RICE, MC_NUGGETS, LOTUS_BOX_FOOD, MILK_BEER, HOT_DRINK -> 30;
            case SOLDIER_PILL, NANO_SNICKERS -> 20;
            default -> 10;
        };
    }

    private static boolean tryConsumeCookIngredients(ServerPlayer player, LexNinjiaArt art) {
        ItemStack hand = player.getMainHandItem();
        return switch (art) {
            case SOLDIER_PILL -> hand.has(net.minecraft.core.component.DataComponents.FOOD)
                    && canConsumeMainHand(hand, 1)
                    && canConsumeInventoryAfterMain(player, stack -> stack.has(net.minecraft.core.component.DataComponents.FOOD), 1, 1)
                    && consumeMainHand(player, 1)
                    && consumeInventory(player, stack -> stack.has(net.minecraft.core.component.DataComponents.FOOD), 1);
            case NANO_SNICKERS -> isSugarLike(hand)
                    && canConsumeMainHand(hand, 1)
                    && canConsumeInventoryAfterMain(player, LexNinjiaStateManager::isSugarLike, 1, 1)
                    && consumeMainHand(player, 1)
                    && consumeInventory(player, LexNinjiaStateManager::isSugarLike, 1);
            case HAMBURGER -> hand.is(Items.BREAD)
                    && canConsumeMainHand(hand, 1)
                    && canConsumeInventoryAfterMain(player, LexNinjiaStateManager::isMeatLike, 1, 1)
                    && canConsumeInventoryAfterMain(player, LexNinjiaStateManager::isPlantFoodLike, 1, 1)
                    && consumeMainHand(player, 1)
                    && consumeInventory(player, LexNinjiaStateManager::isMeatLike, 1)
                    && consumeInventory(player, LexNinjiaStateManager::isPlantFoodLike, 1);
            case MILK_FRUIT_SHAKE -> hand.is(Items.MILK_BUCKET)
                    && canConsumeMainHand(hand, 1)
                    && canConsumeInventoryAfterMain(player, LexNinjiaStateManager::isFruitLike, 2, 1)
                    && consumeMainHand(player, 1)
                    && consumeInventory(player, LexNinjiaStateManager::isFruitLike, 2);
            case SHRIMP_HAND -> isFishLike(hand)
                    && canConsumeMainHand(hand, 1)
                    && canConsumeInventoryAfterMain(player, LexNinjiaStateManager::isFishLike, 1, 1)
                    && consumeMainHand(player, 1)
                    && consumeInventory(player, LexNinjiaStateManager::isFishLike, 1);
            case ROAST_MEAT_RICE -> isCookedMeatLike(hand) && !hand.is(Items.COOKED_CHICKEN)
                    && canConsumeMainHand(hand, 1)
                    && canConsumeInventoryAfterMain(player, LexNinjiaStateManager::isPlantFoodLike, 2, 1)
                    && consumeMainHand(player, 1)
                    && consumeInventory(player, LexNinjiaStateManager::isPlantFoodLike, 2);
            case MC_NUGGETS -> hand.is(Items.COOKED_CHICKEN)
                    && canConsumeMainHand(hand, 3)
                    && canConsumeInventoryAfterMain(player, stack -> stack.is(Items.WHEAT), 2, 3)
                    && consumeMainHand(player, 3)
                    && consumeInventory(player, stack -> stack.is(Items.WHEAT), 2);
            case LOTUS_BOX_FOOD -> isPlantFoodLike(hand)
                    && canConsumeMainHand(hand, 5)
                    && consumeMainHand(player, 5);
            case MILK_BEER -> isPotion(hand, net.minecraft.world.item.alchemy.Potions.AWKWARD)
                    && canConsumeMainHand(hand, 1)
                    && canConsumeInventoryAfterMain(player, stack -> stack.is(Items.WHEAT), 3, 1)
                    && canConsumeInventoryAfterMain(player, stack -> stack.is(Items.MILK_BUCKET), 1, 1)
                    && consumeMainHand(player, 1)
                    && consumeInventory(player, stack -> stack.is(Items.WHEAT), 3)
                    && consumeInventory(player, stack -> stack.is(Items.MILK_BUCKET), 1);
            case COLD_COPPER -> hand.is(Items.COPPER_BLOCK)
                    && canConsumeMainHand(hand, 1)
                    && consumeMainHand(player, 1);
            case HOT_DRINK -> isPotion(hand, net.minecraft.world.item.alchemy.Potions.WATER)
                    && canConsumeMainHand(hand, 1)
                    && canConsumeInventoryAfterMain(player, LexNinjiaStateManager::isSugarLike, 5, 1)
                    && consumeMainHand(player, 1)
                    && consumeInventory(player, LexNinjiaStateManager::isSugarLike, 5);
            default -> false;
        };
    }

    private static ItemStack cookFoodStack(LexNinjiaArt art) {
        ItemStack stack = new ItemStack((art == LexNinjiaArt.MILK_BEER || art == LexNinjiaArt.HOT_DRINK)
                ? Items.HONEY_BOTTLE : Items.COOKIE, art == LexNinjiaArt.MC_NUGGETS ? 6 : art == LexNinjiaArt.SHRIMP_HAND ? 2 : art == LexNinjiaArt.MILK_BEER ? 3 : 1);
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, Component.translatable(art.nameKey()));
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putString(FOOD_ART, art.id());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    private static void applyCookFoodEffect(ServerPlayer player, LexNinjiaArt art) {
        switch (art) {
            case SOLDIER_PILL -> player.getFoodData().eat(16, 1.0F);
            case NANO_SNICKERS -> {
                player.getFoodData().eat(20, 1.0F);
                setLeicra(player, Math.min(maxLeicra(player), leicra(player) + maxLeicra(player) * 0.10F));
                player.addEffect(new MobEffectInstance(MobEffects.SATURATION, com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.lexninjia.lex_ninjia_state_manager.effect.saturation.12.duration_ticks", 25 * 20), com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.lexninjia.lex_ninjia_state_manager.effect.saturation.12.amplifier", 0)));
            }
            case HAMBURGER -> {
                player.getFoodData().eat(20, 1.0F);
                player.heal(player.getMaxHealth());
                player.addEffect(new MobEffectInstance(MobEffects.SATURATION, com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.lexninjia.lex_ninjia_state_manager.effect.saturation.13.duration_ticks", 10 * 20), com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.lexninjia.lex_ninjia_state_manager.effect.saturation.13.amplifier", 0)));
                player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.lexninjia.lex_ninjia_state_manager.effect.absorption.14.duration_ticks", 10 * 20), com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.lexninjia.lex_ninjia_state_manager.effect.absorption.14.amplifier", 3)));
            }
            case MILK_FRUIT_SHAKE -> {
                player.getFoodData().eat(16, 1.5F);
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.lexninjia.lex_ninjia_state_manager.effect.movement_speed.15.duration_ticks", 60 * 20), com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.lexninjia.lex_ninjia_state_manager.effect.movement_speed.15.amplifier", 0)));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.lexninjia.lex_ninjia_state_manager.effect.damage_resistance.16.duration_ticks", 60 * 20), com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.lexninjia.lex_ninjia_state_manager.effect.damage_resistance.16.amplifier", 0)));
            }
            case SHRIMP_HAND -> {
                player.getFoodData().eat(10, 0.9F);
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.lexninjia.lex_ninjia_state_manager.effect.damage_boost.17.duration_ticks", 10 * 20), com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.lexninjia.lex_ninjia_state_manager.effect.damage_boost.17.amplifier", 2)));
                player.getPersistentData().putLong(FOOD_LEICRA_REGEN_UNTIL, SkillCooldownHelper.now(player) + 10L * 20L);
            }
            case ROAST_MEAT_RICE -> {
                player.getFoodData().eat(100, 10.0F);
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.lexninjia.lex_ninjia_state_manager.effect.movement_speed.18.duration_ticks", 20 * 20), com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.lexninjia.lex_ninjia_state_manager.effect.movement_speed.18.amplifier", 0)));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.lexninjia.lex_ninjia_state_manager.effect.damage_boost.19.duration_ticks", 20 * 20), com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.lexninjia.lex_ninjia_state_manager.effect.damage_boost.19.amplifier", 2)));
            }
            case MC_NUGGETS -> {
                player.getFoodData().eat(9, 0.8F);
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.lexninjia.lex_ninjia_state_manager.effect.movement_speed.20.duration_ticks", 20 * 20), com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.lexninjia.lex_ninjia_state_manager.effect.movement_speed.20.amplifier", 0)));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.lexninjia.lex_ninjia_state_manager.effect.damage_boost.21.duration_ticks", 20 * 20), com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.lexninjia.lex_ninjia_state_manager.effect.damage_boost.21.amplifier", 0)));
                player.addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.lexninjia.lex_ninjia_state_manager.effect.health_boost.22.duration_ticks", 20 * 20), com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.lexninjia.lex_ninjia_state_manager.effect.health_boost.22.amplifier", 4)));
            }
            case LOTUS_BOX_FOOD -> {
                player.getFoodData().eat(20, 1.5F);
                setLeicra(player, Math.min(maxLeicra(player), leicra(player) + maxLeicra(player) * 0.25F));
                player.heal(com.rzy.dealt_force_skills.config.DealtForceConfig.floatValue("characters.lexninjia.lex_ninjia_state_manager.heal.3.heal_amount", 10.0F));
            }
            case MILK_BEER -> {
                removeHarmfulEffects(player);
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.lexninjia.lex_ninjia_state_manager.effect.confusion.23.duration_ticks", 3 * 20), com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.lexninjia.lex_ninjia_state_manager.effect.confusion.23.amplifier", 2)));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.lexninjia.lex_ninjia_state_manager.effect.damage_resistance.24.duration_ticks", 20 * 20), com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.lexninjia.lex_ninjia_state_manager.effect.damage_resistance.24.amplifier", 1)));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.lexninjia.lex_ninjia_state_manager.effect.damage_boost.25.duration_ticks", 20 * 20), com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.lexninjia.lex_ninjia_state_manager.effect.damage_boost.25.amplifier", 1)));
            }
            case COLD_COPPER -> {
                player.getFoodData().eat(1000, 100.0F);
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.lexninjia.lex_ninjia_state_manager.effect.movement_slowdown.26.duration_ticks", 60 * 20), com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.lexninjia.lex_ninjia_state_manager.effect.movement_slowdown.26.amplifier", 3)));
                player.addEffect(new MobEffectInstance(MobEffects.POISON, com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.lexninjia.lex_ninjia_state_manager.effect.poison.27.duration_ticks", 60 * 20), com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.lexninjia.lex_ninjia_state_manager.effect.poison.27.amplifier", 4)));
                player.addEffect(new MobEffectInstance(MobEffects.WITHER, com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.lexninjia.lex_ninjia_state_manager.effect.wither.28.duration_ticks", 60 * 20), com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.lexninjia.lex_ninjia_state_manager.effect.wither.28.amplifier", 4)));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.lexninjia.lex_ninjia_state_manager.effect.damage_resistance.29.duration_ticks", 60 * 20), com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.lexninjia.lex_ninjia_state_manager.effect.damage_resistance.29.amplifier", 2)));
                player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.lexninjia.lex_ninjia_state_manager.effect.absorption.30.duration_ticks", 60 * 20), com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.lexninjia.lex_ninjia_state_manager.effect.absorption.30.amplifier", 9)));
            }
            case HOT_DRINK -> {
                setLeicra(player, maxLeicra(player));
                player.getPersistentData().putLong(FOOD_LEICRA_REGEN_UNTIL, SkillCooldownHelper.now(player) + 120L * 20L);
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.lexninjia.lex_ninjia_state_manager.effect.damage_boost.31.duration_ticks", 120 * 20), com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.lexninjia.lex_ninjia_state_manager.effect.damage_boost.31.amplifier", 0)));
            }
            default -> {
            }
        }
    }

    private static void handleIncomingDamage(ServerPlayer player, LivingIncomingDamageEvent event) {
        RuntimeState state = state(player);
        long now = SkillCooldownHelper.now(player);
        Entity attacker = event.getSource().getEntity();
        if (attacker instanceof LivingEntity living) {
            state.lastDamager = living.getUUID();
        }
        if (state.fdHandUntil > now) {
            event.setAmount(0.0F);
            state.fdHandUntil = 0L;
            return;
        }
        if (state.sleepUntil > now) {
            event.setAmount(event.getAmount() * 0.20F);
        }
        if (state.reflectStacks > 0 && state.reflectUntil > now && event.getSource().getDirectEntity() instanceof Projectile projectile) {
            float reflectedDamage = Math.max(1.0F, event.getAmount());
            event.setAmount(0.0F);
            state.reflectStacks--;
            Entity owner = projectile.getOwner();
            if (owner instanceof LivingEntity living) {
                hurtTrue(player, living, reflectedDamage);
            }
            return;
        }
        float multiplier = Math.max(0.0F, 1.0F - state.harmonyStacks * 0.02F);
        if (state.hamFriendPact) {
            multiplier *= 0.75F;
        }
        if (state.hamBerserkUntil > now || state.hamShadowKickUntil > now) {
            multiplier *= 0.55F;
        }
        if (attacker != null && attacker.getUUID().equals(state.handshakeTarget)) {
            multiplier *= 0.70F;
        }
        int overload = mindOverload(player);
        if (overload > 0) {
            multiplier *= 1.0F + overload * 0.10F;
        }
        if (state.deathFlameUntil > now && event.getAmount() * multiplier >= player.getHealth()) {
            float overflow = event.getAmount() * multiplier - Math.max(0.0F, player.getHealth() - 1.0F);
            state.deathFlameOverflow += Math.max(0.0F, overflow);
            event.setAmount(Math.max(0.0F, player.getHealth() - 1.0F));
            return;
        }
        event.setAmount(event.getAmount() * multiplier);
    }

    private static void handleOutgoingDamage(ServerPlayer attacker, LivingEntity target, LivingIncomingDamageEvent event) {
        RuntimeState state = state(attacker);
        long now = SkillCooldownHelper.now(attacker);
        float multiplier = 1.0F + state.bladeStacks * 0.03F + state.sharpenStacks * 0.01F;
        if (state.hamFriendPact) {
            multiplier *= 1.50F;
        }
        if (state.hamBerserkUntil > now) {
            multiplier *= 3.0F;
            attacker.heal(Math.max(0.0F, event.getAmount()) * 0.50F);
        }
        if (target.getUUID().equals(state.handshakeTarget)) {
            multiplier *= 0.10F;
        }
        event.setAmount(event.getAmount() * multiplier);
        if (state.sharpenStacks > 0) {
            state.sharpenStacks--;
        }
        if (state.burningBladeUntil > now) {
            hurtTrue(attacker, target, target.getMaxHealth() * 0.025F);
            target.igniteForSeconds(3);
        }
        if (state.arashiUntil > now) {
            queueSlashes(state, target, now, 2, Math.max(1.0F, event.getAmount() * 0.60F), 4L, 4L, SlashVisual.ARASHI, 0.0D);
        }
        if (state.shadowBladeUntil > now) {
            hurtTrue(attacker, target, target.getMaxHealth() * 0.005F);
        }
        if (state.shadowCloneUntil > now) {
            float splitDamage = Math.max(1.0F, event.getAmount()) / 3.0F;
            event.setAmount(splitDamage);
            playMainShadowCloneSlash(attacker, target);
            queueShadowCloneFollowUps(state, target, now, splitDamage);
        }
        if (state.peaStacks > 0 && state.peaUntil > now) {
            float peaDamage = state.peaStacks;
            if (state.fertilizerUntil > now) {
                peaDamage *= 2.5F;
            }
            hurtTrue(attacker, target, peaDamage);
        }
        if (state.snakePoisonUntil > now) {
            addSnakePoison(target, attacker);
        }
    }

    private static volatile double NO_ONE_RETALIATES_RADIUS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("NO_ONE_RETALIATES_RADIUS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("characters.lex_ninjia.arts.no_one_retaliates.radius", 45.0));
    private static volatile int NO_ONE_RETALIATES_DURATION_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("NO_ONE_RETALIATES_DURATION_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.lex_ninjia.arts.no_one_retaliates.duration_ticks", 1200));
    private static volatile float NO_ONE_RETALIATES_REFLECT_MULTIPLIER = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("NO_ONE_RETALIATES_REFLECT_MULTIPLIER", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.floatValue(
      "characters.lex_ninjia.arts.no_one_retaliates.reflect_multiplier", 2.0F
   ));
    private static volatile int NO_ONE_RETALIATES_CASTER_ATTACK_PENALTY_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("NO_ONE_RETALIATES_CASTER_ATTACK_PENALTY_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue(
      "characters.lex_ninjia.arts.no_one_retaliates.caster_attack_duration_penalty_ticks", 200
   ));
    private static void handleNoRetaliation(LivingEntity attacker, LivingEntity target, LivingIncomingDamageEvent event) {
        if (event.isCanceled() || !(target.level() instanceof ServerLevel level)) {
            return;
        }
        var server = level.getServer();
        if (server == null) {
            return;
        }
        long now = SkillCooldownHelper.now(level);
        double radiusSq = NO_ONE_RETALIATES_RADIUS * NO_ONE_RETALIATES_RADIUS;
        for (Map.Entry<UUID, RuntimeState> entry : RUNTIME.entrySet()) {
            RuntimeState state = entry.getValue();
            if (state.noRetaliationUntil <= 0L) {
                continue;
            }
            if (state.noRetaliationUntil <= now) {
                state.noRetaliationUntil = 0L;
                continue;
            }
            ServerPlayer caster = server.getPlayerList().getPlayer(entry.getKey());
            if (caster == null || !caster.isAlive() || !isLexNinjia(caster)) {
                state.noRetaliationUntil = 0L;
                continue;
            }
            if (caster.level() != attacker.level() || caster.level() != target.level()) {
                continue;
            }
            if (attacker.distanceToSqr(caster) > radiusSq || target.distanceToSqr(caster) > radiusSq) {
                continue;
            }
            if (attacker == caster) {
                state.noRetaliationUntil = Math.max(now,
                        state.noRetaliationUntil - NO_ONE_RETALIATES_CASTER_ATTACK_PENALTY_TICKS);
                if (state.noRetaliationUntil <= now) {
                    state.noRetaliationUntil = 0L;
                }
                continue;
            }
            float reflectedDamage = Math.max(1.0F, event.getAmount()) * NO_ONE_RETALIATES_REFLECT_MULTIPLIER;
            event.setCanceled(true);
            event.setAmount(0.0F);
            hurtTrue(caster, attacker, reflectedDamage);
            break;
        }
    }

    private static void applyHandshake(ServerPlayer player, RuntimeState state) {
        nearestLookTarget(player, 8.0D, 0.92D).ifPresent(target -> {
            if (!canApplyControl(player, target)) {
                return;
            }
            state.handshakeTarget = target.getUUID();
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.lexninjia.lex_ninjia_state_manager.effect.glowing.32.duration_ticks", 15 * 20), com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.lexninjia.lex_ninjia_state_manager.effect.glowing.32.amplifier", 0)));
            player.addEffect(new MobEffectInstance(MobEffects.GLOWING, com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.lexninjia.lex_ninjia_state_manager.effect.glowing.33.duration_ticks", 15 * 20), com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.lexninjia.lex_ninjia_state_manager.effect.glowing.33.amplifier", 0)));
        });
    }

    private static void tauntNearby(ServerPlayer player) {
        for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(5.0D), entity -> entity != player && entity.isAlive())) {
            if (!canApplyControl(player, target)) {
                continue;
            }
            target.addEffect(new MobEffectInstance(ModEffects.STUN, com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.lexninjia.lex_ninjia_state_manager.effect.stun.34.duration_ticks", 30), com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.lexninjia.lex_ninjia_state_manager.effect.stun.34.amplifier", 0)));
            if (target instanceof Mob mob) {
                mob.setTarget(player);
            }
        }
    }

    private static void shadowSmoke(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        VisualSmokeCloudEntity smoke = new VisualSmokeCloudEntity(ModEntities.VISUAL_SMOKE_CLOUD.get(), level);
        smoke.setPos(player.getX(), player.getY(), player.getZ());
        level.addFreshEntity(smoke);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(6.0D), entity -> entity != player && entity.isAlive())) {
            if (!canApplyControl(player, target)) {
                continue;
            }
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.lexninjia.lex_ninjia_state_manager.effect.blindness.35.duration_ticks", 10 * 20), com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.lexninjia.lex_ninjia_state_manager.effect.blindness.35.amplifier", 0)));
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.lexninjia.lex_ninjia_state_manager.effect.glowing.36.duration_ticks", 10 * 20), com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.lexninjia.lex_ninjia_state_manager.effect.glowing.36.amplifier", 0)));
        }
    }

    private static void startTenMeterSword(ServerPlayer player, RuntimeState state, long now) {
        state.tenMeterReleaseTick = now + 3L * 20L;
        state.tenMeterOrigin = player.position();
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.lexninjia.lex_ninjia_state_manager.effect.movement_slowdown.37.duration_ticks", 3 * 20), com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.lexninjia.lex_ninjia_state_manager.effect.movement_slowdown.37.amplifier", 10), true, false));
        player.getAttribute(Attributes.MOVEMENT_SPEED).addTransientModifier(new AttributeModifier(SPIN_SLOW_UUID, -1.0D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        ));
    }

    private static void tickTenMeterSword(ServerPlayer player, RuntimeState state, long now) {
        if (player.hasEffect(ModEffects.STUN) || player.hasEffect(ModEffects.TEMPEST_DISARMED)
                || state.tenMeterOrigin.distanceToSqr(player.position()) > 0.25D) {
            state.tenMeterReleaseTick = 0L;
            player.getAttribute(Attributes.MOVEMENT_SPEED).removeModifier(SPIN_SLOW_UUID);
            return;
        }
        if (now < state.tenMeterReleaseTick) {
            return;
        }
        player.getAttribute(Attributes.MOVEMENT_SPEED).removeModifier(SPIN_SLOW_UUID);
        state.tenMeterReleaseTick = 0L;
        strikeFrontArea(player, 66.0D, 6.0D, 6.0D, meleeDamage(player) * 5.0F);
        RangedSoundHelper.playFollowingPlayer(player, SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS, 1.2F, 0.6F, 32.0D);
    }

    private static void releaseSpinIon(ServerPlayer player, RuntimeState state) {
        float spent = Float.isFinite(state.rightChargeSpent)
                ? Mth.clamp(state.rightChargeSpent, 0.0F, maxUsefulSpinIonCharge())
                : 0.0F;
        state.rightPressTick = 0L;
        state.rightChargeSpent = 0.0F;
        float distance = spinIonRange(spent);
        float width = SPIN_ION_BASE_WIDTH + spent * SPIN_ION_WIDTH_PER_LEICRA;
        strikeFrontArea(player, distance, width, 2.0D, 6.0F + spent * 0.4F);
        Vec3 dash = player.getLookAngle().normalize().scale(Math.min(2.5D, distance * 0.1D));
        player.push(dash.x, 0.1D, dash.z);
        RangedSoundHelper.playFollowingPlayer(player, SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS, 1.0F, 0.8F, 32.0D);
    }

    private static float spinIonRange(float spent) {
        return (float) Math.min(MAX_FRONT_AREA_QUERY_RANGE,
                SPIN_ION_BASE_RANGE + Math.max(0.0F, spent) * SPIN_ION_RANGE_PER_LEICRA);
    }

    private static float maxUsefulSpinIonCharge() {
        return (float) ((MAX_FRONT_AREA_QUERY_RANGE - SPIN_ION_BASE_RANGE) / SPIN_ION_RANGE_PER_LEICRA);
    }

    private static void pullLookTarget(ServerPlayer player) {
        nearestLookTarget(player, 24.0D, 0.94D).ifPresent(target -> {
            if (!canApplyControl(player, target)) {
                return;
            }
            Vec3 destination = player.position().add(player.getLookAngle().normalize().scale(1.5D));
            target.teleportTo(destination.x, player.getY(), destination.z);
            target.setDeltaMovement(player.position().subtract(target.position()).normalize().scale(1.2D));
        });
    }

    private static void createSandWall(ServerPlayer player, RuntimeState state, long now) {
        ServerLevel level = player.serverLevel();
        Vec3 look = new Vec3(player.getLookAngle().x, 0.0D, player.getLookAngle().z).normalize();
        Vec3 right = new Vec3(-look.z, 0.0D, look.x);
        BlockPos center = BlockPos.containing(player.position().add(look.scale(4.0D)));
        for (int x = -2; x <= 2; x++) {
            for (int y = 0; y < 5; y++) {
                BlockPos pos = BlockPos.containing(center.getX() + right.x * x, center.getY() + y, center.getZ() + right.z * x);
                if (level.isEmptyBlock(pos)) {
                    level.setBlockAndUpdate(pos, Blocks.SANDSTONE.defaultBlockState());
                    state.wallBlocks.add(new TimedBlock(pos.immutable(), now + 30L * 20L));
                }
            }
        }
    }

    private static void tickWallBlocks(ServerLevel level, RuntimeState state, long now) {
        Iterator<TimedBlock> iterator = state.wallBlocks.iterator();
        while (iterator.hasNext()) {
            TimedBlock block = iterator.next();
            if (now < block.expireTick()) {
                continue;
            }
            if (level.getBlockState(block.pos()).is(Blocks.SANDSTONE)) {
                level.removeBlock(block.pos(), false);
            }
            iterator.remove();
        }
    }

    private static void equipTemporaryShield(ServerPlayer player, RuntimeState state, long now) {
        if (state.shieldUntil > now) {
            state.shieldUntil = now + 30L * 20L;
            enforceHandGuardShield(player);
            return;
        }
        moveToInventoryOrDrop(player, player.getOffhandItem());
        player.setItemInHand(InteractionHand.OFF_HAND, createHandGuardShield());
        player.getCooldowns().removeCooldown(Items.SHIELD);
        state.shieldUntil = now + 30L * 20L;
    }

    private static ItemStack createHandGuardShield() {
        ItemStack shield = new ItemStack(Items.SHIELD);
        shield.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, Component.translatable("item.dealt_force_skills.lex_ninjia_guard_shield"));
        shield.set(net.minecraft.core.component.DataComponents.UNBREAKABLE, new net.minecraft.world.item.component.Unbreakable(true));
        CompoundTag tag = shield.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putBoolean(HAND_GUARD_SHIELD_TAG, true);
        shield.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return shield;
    }

    private static boolean isHandGuardShield(ItemStack stack) {
        return !stack.isEmpty() && stack.is(Items.SHIELD)
                && stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getBoolean(HAND_GUARD_SHIELD_TAG);
    }

    private static void enforceHandGuardShield(ServerPlayer player) {
        ItemStack current = player.getOffhandItem();
        if (isHandGuardShield(current)) {
            removeHandGuardShieldsFromInventory(player);
            return;
        }
        moveToInventoryOrDrop(player, current);
        ItemStack bound = takeHandGuardShieldFromInventory(player);
        player.setItemInHand(InteractionHand.OFF_HAND, bound.isEmpty() ? createHandGuardShield() : bound);
    }

    private static ItemStack takeHandGuardShieldFromInventory(ServerPlayer player) {
        for (int slot = 0; slot < player.getInventory().items.size(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!isHandGuardShield(stack)) {
                continue;
            }
            player.getInventory().setItem(slot, ItemStack.EMPTY);
            player.getInventory().setChanged();
            return stack.copy();
        }
        return ItemStack.EMPTY;
    }

    private static void removeHandGuardShieldsFromInventory(ServerPlayer player) {
        for (int slot = 0; slot < player.getInventory().items.size(); slot++) {
            if (isHandGuardShield(player.getInventory().getItem(slot))) {
                player.getInventory().setItem(slot, ItemStack.EMPTY);
            }
        }
        player.getInventory().setChanged();
    }

    private static void moveToInventoryOrDrop(ServerPlayer player, ItemStack stack) {
        ItemStack remaining = stack.copy();
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        player.getInventory().add(remaining);
        if (!remaining.isEmpty()) {
            player.level().addFreshEntity(new ItemEntity(
                    player.level(), player.getX(), player.getY(), player.getZ(), remaining));
        }
    }

    private static void restoreShield(ServerPlayer player, RuntimeState state) {
        if (state.shieldUntil <= 0L) {
            return;
        }
        ItemStack current = player.getOffhandItem();
        if (isHandGuardShield(current)) {
            player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        }
        removeHandGuardShieldsFromInventory(player);
        state.shieldUntil = 0L;
    }

    private static void playArtVisuals(ServerPlayer player, LexNinjiaArt art, LivingEntity target) {
        ServerLevel level = player.serverLevel();
        Vec3 center = player.position().add(0.0D, 1.0D, 0.0D);
        switch (art) {
            case DEATH_FLAME_SMOKE -> {
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME, center.x, center.y, center.z, 24, 0.9D, 0.7D, 0.9D, 0.04D);
                level.sendParticles(ModParticles.D_WOLF_LARGE_SMOKE.get(), center.x, center.y, center.z, 3, 1.2D, 0.8D, 1.2D, 0.03D);
            }
            case SHADOW_BLADE, SHADOW_SMOKE, SHADOW_CLONE_CROSS, HAM_SHADOW_KICK -> {
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.POOF, center.x, center.y, center.z, 28, 0.9D, 0.8D, 0.9D, 0.08D);
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.PORTAL, center.x, center.y + 0.2D, center.z, 18, 0.8D, 0.8D, 0.8D, 0.1D);
            }
            case GOOD_SLEEP -> level.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD, center.x, center.y, center.z, 20, 0.7D, 0.5D, 0.7D, 0.03D);
            case BIG_PORTION, PEA_SHOOTER, SNAKE_POISON_HAND -> level.sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER, center.x, center.y, center.z, 18, 0.8D, 0.6D, 0.8D, 0.04D);
            case SOLDIER_PILL, NANO_SNICKERS, HAMBURGER, MILK_FRUIT_SHAKE, SHRIMP_HAND, ROAST_MEAT_RICE, MC_NUGGETS, LOTUS_BOX_FOOD, MILK_BEER, COLD_COPPER, HOT_DRINK -> {
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER, center.x, center.y, center.z, 16, 0.6D, 0.5D, 0.6D, 0.03D);
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT, center.x, center.y + 0.2D, center.z, 10, 0.5D, 0.4D, 0.5D, 0.05D);
            }
            case HAM_FRIEND, HAM_BERSERK, HAM_KILL_ALL, HAM_BEAST -> {
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.WITCH, center.x, center.y, center.z, 30, 1.0D, 0.8D, 1.0D, 0.05D);
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.PORTAL, center.x, center.y + 0.2D, center.z, 24, 1.0D, 0.9D, 1.0D, 0.12D);
            }
            default -> {
                if (art.school() == LexNinjiaSchool.BLADE) {
                    Vec3 hit = target == null ? center.add(player.getLookAngle().normalize().scale(1.4D)) : target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
                    level.sendParticles(net.minecraft.core.particles.ParticleTypes.SWEEP_ATTACK, hit.x, hit.y, hit.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                    level.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT, hit.x, hit.y, hit.z, 12, 0.5D, 0.4D, 0.5D, 0.08D);
                } else if (art.school() == LexNinjiaSchool.HAND) {
                    Vec3 hit = target == null ? center.add(player.getLookAngle().normalize().scale(1.0D)) : target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
                    level.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT, hit.x, hit.y, hit.z, 14, 0.4D, 0.4D, 0.4D, 0.08D);
                    level.sendParticles(net.minecraft.core.particles.ParticleTypes.POOF, center.x, center.y, center.z, 8, 0.4D, 0.4D, 0.4D, 0.04D);
                }
            }
        }
    }

    private static void spawnShadowCloneVisuals(ServerPlayer player, boolean burst) {
        ServerLevel level = player.serverLevel();
        Vec3 look = new Vec3(player.getLookAngle().x, 0.0D, player.getLookAngle().z);
        if (look.lengthSqr() < 0.0001D) {
            look = new Vec3(0.0D, 0.0D, 1.0D);
        }
        look = look.normalize();
        Vec3 right = new Vec3(-look.z, 0.0D, look.x);
        int count = burst ? 28 : 4;
        for (double side : new double[]{-1.15D, 1.15D}) {
            Vec3 clone = player.position().add(right.scale(side)).add(0.0D, player.getBbHeight() * 0.5D, 0.0D);
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.POOF, clone.x, clone.y, clone.z, count, 0.22D, 0.65D, 0.22D, burst ? 0.08D : 0.01D);
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT, clone.x, clone.y + 0.2D, clone.z, Math.max(2, count / 3), 0.25D, 0.55D, 0.25D, 0.03D);
        }
    }

    private static void releaseAllHands(ServerPlayer player, RuntimeState state, long now) {
        LivingEntity target = nearestLookTarget(player, 16.0D, 0.85D).orElse(null);
        if (target != null) {
            if (!canApplyControl(player, target)) {
                target = null;
            }
        }
        if (target != null) {
            hurtTrue(player, target, 22.0F);
            target.addEffect(new MobEffectInstance(ModEffects.RAPTOR_ACTION_PAUSE, com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.lexninjia.lex_ninjia_state_manager.effect.raptor_action_pause.38.duration_ticks", 4 * 20), com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.lexninjia.lex_ninjia_state_manager.effect.raptor_action_pause.38.amplifier", 0)));
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.lexninjia.lex_ninjia_state_manager.effect.movement_slowdown.39.duration_ticks", 6 * 20), com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.lexninjia.lex_ninjia_state_manager.effect.movement_slowdown.39.amplifier", 10)));
            addSnakePoison(target, player);
        }
        state.fdHandUntil = now + 2L * 20L;
        state.peaStacks = 5;
        state.peaUntil = now + 15L * 20L;
        state.returnHandUntil = now + 14L * 20L;
        createSandWall(player, state, now);
    }

    private static void summonDeathGod(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Skeleton skeleton = EntityType.SKELETON.create(level);
        if (skeleton == null) {
            return;
        }
        skeleton.moveTo(player.getX() + 1.0D, player.getY(), player.getZ() + 1.0D, player.getYRot(), 0.0F);
        skeleton.setCustomName(Component.translatable("entity.dealt_force_skills.lex_ninjia.death_god"));
        skeleton.setCustomNameVisible(true);
        skeleton.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100.0D);
        skeleton.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(1000.0D);
        skeleton.setHealth(100.0F);
        skeleton.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_HOE));
        skeleton.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
        skeleton.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.LEATHER_CHESTPLATE));
        skeleton.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.LEATHER_LEGGINGS));
        skeleton.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.LEATHER_BOOTS));
        nearestLookTarget(player, 32.0D, 0.7D)
                .filter(target -> canApplyControl(player, target))
                .ifPresent(skeleton::setTarget);
        level.addFreshEntity(skeleton);
    }

    private static void summonHamBeast(ServerPlayer player, RuntimeState state) {
        ServerLevel level = player.serverLevel();
        Wolf wolf = EntityType.WOLF.create(level);
        if (wolf == null) {
            return;
        }
        wolf.moveTo(player.getX() + 1.0D, player.getY(), player.getZ() + 1.0D, player.getYRot(), 0.0F);
        wolf.tame(player);
        wolf.getAttribute(Attributes.MAX_HEALTH).setBaseValue(150.0D);
        wolf.setHealth(150.0F);
        wolf.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.lexninjia.lex_ninjia_state_manager.effect.damage_boost.40.duration_ticks", 75 * 20), com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.lexninjia.lex_ninjia_state_manager.effect.damage_boost.40.amplifier", 99)));
        wolf.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.lexninjia.lex_ninjia_state_manager.effect.movement_speed.41.duration_ticks", 75 * 20), com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.lexninjia.lex_ninjia_state_manager.effect.movement_speed.41.amplifier", 2)));
        level.addFreshEntity(wolf);
        state.hamBeastId = wolf.getUUID();
        state.hamBeastStacks = 0;
    }

    private static void tickHamKillAll(ServerPlayer player, RuntimeState state, long now) {
        if (now < state.hamKillReleaseTick) {
            player.setDeltaMovement(player.getDeltaMovement().x, 0.08D, player.getDeltaMovement().z);
            return;
        }
        for (LivingEntity target : player.serverLevel().getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(100.0D), entity -> entity != player && entity.isAlive())) {
            executeTarget(player, target);
        }
        state.hamKillReleaseTick = 0L;
        player.hurt(SkillDamageHelper.trueDamage(player.serverLevel(), player, player), player.getMaxHealth() * HAM_KILL_ALL_SELF_DAMAGE_MAX_HEALTH_FRACTION);
    }

    private static void tickHamShadowKick(ServerPlayer player, RuntimeState state, long now) {
        if (player.getDeltaMovement().horizontalDistanceSqr() <= 0.005D || now % 10L != 0L) {
            return;
        }
        hurtSelfPercent(player, HAM_SHADOW_KICK_TICK_SELF_HEALTH_COST_FRACTION);
        for (LivingEntity target : player.serverLevel().getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(7.0D), entity -> entity != player && entity.isAlive())) {
            if (!canApplyControl(player, target)) {
                continue;
            }
            hurtTrue(player, target, meleeDamage(player) * 0.7F);
            target.addEffect(new MobEffectInstance(ModEffects.STUN, com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.lexninjia.lex_ninjia_state_manager.effect.stun.42.duration_ticks", 20), com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.lexninjia.lex_ninjia_state_manager.effect.stun.42.amplifier", 0)));
        }
        breakSoftBlocksAround(player);
    }

    private static void breakSoftBlocksAround(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        if (!ModGameRules.areSkillBlockBreaksEnabled(level)) {
            return;
        }
        BlockPos center = player.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-2, -1, -2), center.offset(2, 1, 2))) {
            if (pos.equals(center.below())) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (!state.isAir() && state.getDestroySpeed(level, pos) >= 0.0F && state.getDestroySpeed(level, pos) <= 1.0F) {
                level.destroyBlock(pos, true, player);
            }
        }
    }

    private static void rainIronSwords(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        net.minecraft.core.particles.ItemParticleOption swordParticle =
                new net.minecraft.core.particles.ItemParticleOption(net.minecraft.core.particles.ParticleTypes.ITEM, new ItemStack(Items.IRON_SWORD));
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(40.0D), entity -> entity != player && entity.isAlive()
                        && level.canSeeSky(entity.blockPosition().above()))) {
            double hitX = target.getX();
            double hitY = target.getY() + target.getBbHeight() * 0.5D;
            double hitZ = target.getZ();
            level.sendParticles(swordParticle, hitX, target.getY() + target.getBbHeight() + 2.5D, hitZ, 8, 0.35D, 1.2D, 0.35D, 0.02D);
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT,
                    hitX, hitY, hitZ, 16, 0.45D, 0.35D, 0.45D, 0.12D);
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.SWEEP_ATTACK,
                    hitX, hitY, hitZ, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            RangedSoundHelper.playFollowingPlayer(player, SoundEvents.TRIDENT_HIT,
                    SoundSource.PLAYERS, 0.8F, 1.15F, 32.0D);
            hurtTrue(player, target, meleeDamage(player) * 0.9F);
        }
    }

    private static void queueSlashes(RuntimeState state, LivingEntity target, long now, int count, float damage, long firstDelay, long interval, SlashVisual visual, double sideOffset) {
        if (target == null || count <= 0 || damage <= 0.0F) {
            return;
        }
        for (int i = 0; i < count; i++) {
            state.slashQueue.add(new PendingSlash(target.getUUID(), now + firstDelay + i * interval, damage, visual, sideOffset));
        }
    }

    private static void queueShadowCloneFollowUps(RuntimeState state, LivingEntity target, long now, float damage) {
        if (target == null || damage <= 0.0F) {
            return;
        }
        state.slashQueue.add(new PendingSlash(target.getUUID(), now + 3L, damage, SlashVisual.CLONE, -1.15D));
        state.slashQueue.add(new PendingSlash(target.getUUID(), now + 6L, damage, SlashVisual.CLONE, 1.15D));
    }

    private static void playMainShadowCloneSlash(ServerPlayer player, LivingEntity target) {
        if (target == null) {
            return;
        }
        ServerLevel level = player.serverLevel();
        Vec3 hit = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.SWEEP_ATTACK, hit.x, hit.y, hit.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT, hit.x, hit.y, hit.z, 12, 0.35D, 0.35D, 0.35D, 0.06D);
    }

    private static void tickSlashQueue(ServerPlayer player, RuntimeState state, long now) {
        Iterator<PendingSlash> iterator = state.slashQueue.iterator();
        while (iterator.hasNext()) {
            PendingSlash slash = iterator.next();
            if (slash.tick() > now) {
                continue;
            }
            Entity entity = player.serverLevel().getEntity(slash.target());
            if (entity instanceof LivingEntity target && target.isAlive()) {
                playQueuedSlashVisual(player, target, slash);
                hurtTrue(player, target, slash.damage());
            }
            iterator.remove();
        }
    }

    private static void playQueuedSlashVisual(ServerPlayer player, LivingEntity target, PendingSlash slash) {
        ServerLevel level = player.serverLevel();
        Vec3 hit = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
        switch (slash.visual()) {
            case CLONE -> {
                Vec3 look = new Vec3(player.getLookAngle().x, 0.0D, player.getLookAngle().z);
                if (look.lengthSqr() < 0.0001D) {
                    look = new Vec3(0.0D, 0.0D, 1.0D);
                }
                look = look.normalize();
                Vec3 right = new Vec3(-look.z, 0.0D, look.x);
                Vec3 clone = player.position().add(right.scale(slash.sideOffset())).add(0.0D, player.getBbHeight() * 0.5D, 0.0D);
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.POOF, clone.x, clone.y, clone.z, 10, 0.20D, 0.45D, 0.20D, 0.04D);
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.SWEEP_ATTACK, hit.x, hit.y, hit.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                RangedSoundHelper.playFollowingPlayer(player, SoundEvents.PLAYER_ATTACK_SWEEP,
                        SoundSource.PLAYERS, 0.65F, slash.sideOffset() < 0.0D ? 1.25F : 0.85F, 32.0D);
            }
            case ARASHI -> {
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.SWEEP_ATTACK, hit.x, hit.y, hit.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT, hit.x, hit.y, hit.z, 14, 0.45D, 0.35D, 0.45D, 0.10D);
                RangedSoundHelper.playFollowingPlayer(player, SoundEvents.PLAYER_ATTACK_SWEEP,
                        SoundSource.PLAYERS, 0.75F, 1.35F, 32.0D);
            }
            case BURNING -> {
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME, hit.x, hit.y, hit.z, 12, 0.30D, 0.25D, 0.30D, 0.02D);
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT, hit.x, hit.y, hit.z, 10, 0.35D, 0.35D, 0.35D, 0.08D);
                RangedSoundHelper.playFollowingPlayer(player, SoundEvents.BLAZE_SHOOT,
                        SoundSource.PLAYERS, 0.55F, 1.5F, 32.0D);
            }
        }
    }

    private static void strikeFrontArea(ServerPlayer player, double range, double halfWidth, double height, float damage) {
        range = boundedFrontAreaDimension(range);
        halfWidth = boundedFrontAreaDimension(halfWidth);
        height = boundedFrontAreaDimension(height);
        if (range <= 0.0D || halfWidth <= 0.0D || height <= 0.0D) {
            return;
        }
        Vec3 eye = player.getEyePosition();
        Vec3 look = new Vec3(player.getLookAngle().x, 0.0D, player.getLookAngle().z);
        if (look.lengthSqr() < 0.0001D) {
            look = player.getLookAngle();
        }
        look = look.normalize();
        Vec3 right = new Vec3(-look.z, 0.0D, look.x);
        double horizontalQueryRange = Math.max(range, halfWidth);
        AABB box = player.getBoundingBox().inflate(horizontalQueryRange, height, horizontalQueryRange);
        for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class, box,
                entity -> entity != player && entity.isAlive())) {
            Vec3 delta = target.getBoundingBox().getCenter().subtract(eye);
            double forward = delta.dot(look);
            double side = Math.abs(delta.dot(right));
            if (forward > 0.0D && forward <= range && side <= halfWidth && Math.abs(delta.y) <= height) {
                hurtTrue(player, target, damage);
            }
        }
    }

    private static double boundedFrontAreaDimension(double value) {
        return Double.isFinite(value) ? Mth.clamp(value, 0.0D, MAX_FRONT_AREA_QUERY_RANGE) : 0.0D;
    }

    private static Optional<LivingEntity> nearestLookTarget(ServerPlayer player, double range, double minimumDot) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        return player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(range),
                        entity -> entity != player && entity.isAlive() && entity.isAttackable())
                .stream()
                .filter(target -> {
                    Vec3 direction = target.getBoundingBox().getCenter().subtract(eye);
                    return direction.lengthSqr() <= range * range && direction.normalize().dot(look) >= minimumDot;
                })
                .min(Comparator.comparingDouble(target -> target.distanceToSqr(player)));
    }

    private static boolean canApplyControl(ServerPlayer player, LivingEntity target) {
        return TargetingUtil.isHostileLivingFor(player, target);
    }

    private static void executeTarget(ServerPlayer player, LivingEntity target) {
        if (target == null || !TargetingUtil.isHostileLivingFor(player, target)) {
            return;
        }
        if (!com.rzy.dealt_force_skills.boss.BossCombatRules.canInstantKill(target) && !(target instanceof Player)) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                    "message.dealt_force_skills.beacon_boss.immune_execute"), true);
            return;
        }
        if (target instanceof ServerPlayer victim) {
            victim.getPersistentData().putLong(REVIVE_DISABLED_UNTIL, victim.level().getGameTime() + 7L * 20L);
        }
        hurtTrue(player, target, Math.max(target.getMaxHealth() * 20.0F, 1000.0F));
        if (target.isAlive() && !(target instanceof Player)
                && com.rzy.dealt_force_skills.boss.BossCombatRules.canInstantKill(target)) {
            target.kill();
        }
    }

    private static void triggerSnakePoison(LivingEntity entity) {
        CompoundTag tag = entity.getPersistentData();
        int stacks = tag.getInt(ROOT + ".snake_poison_stacks");
        if (stacks <= 0) {
            return;
        }
        float damage = entity.getMaxHealth() * 0.02F * stacks;
        tag.putInt(ROOT + ".snake_poison_stacks", Math.max(0, stacks - 1));
        if (entity.level() instanceof ServerLevel level) {
            entity.hurt(SkillDamageHelper.trueDamage(level, entity, null), damage);
        }
    }

    private static void addSnakePoison(LivingEntity target, ServerPlayer attacker) {
        CompoundTag tag = target.getPersistentData();
        tag.putInt(ROOT + ".snake_poison_stacks", Math.min(9, tag.getInt(ROOT + ".snake_poison_stacks") + 3));
    }

    private static void growPlacedCrop(ServerLevel level, BlockPos pos) {
        for (int i = 0; i < 4; i++) {
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof BonemealableBlock bonemealable)
                    || !bonemealable.isValidBonemealTarget(level, pos, state)
                    || !bonemealable.isBonemealSuccess(level, level.random, pos, state)) {
                return;
            }
            bonemealable.performBonemeal(level, level.random, pos, state);
        }
    }

    private static boolean isSleeping(ServerPlayer player) {
        return state(player).sleepUntil > SkillCooldownHelper.now(player);
    }

    private static void hurtTrue(ServerPlayer source, LivingEntity target, float amount) {
        if (target == null || amount <= 0.0F || !(source.level() instanceof ServerLevel level)) {
            return;
        }
        SkillDamageHelper.hurtUnscaled(target, SkillDamageHelper.trueDamage(level, source, source), SkillDamageHelper.scale(source, amount));
    }

    private static void hurtSelfPercent(ServerPlayer player, float percent) {
        player.hurt(SkillDamageHelper.trueDamage(player.serverLevel(), player, player), player.getMaxHealth() * percent);
    }

    private static float meleeDamage(ServerPlayer player) {
        return (float) Math.max(2.0D, player.getAttributeValue(Attributes.ATTACK_DAMAGE));
    }

    private static float handDamage(ServerPlayer player, float base, RuntimeState state) {
        if (!state.doubleLuohanReady) {
            return base;
        }
        state.doubleLuohanReady = false;
        return base * 2.0F;
    }

    private static boolean hasMeleeWeapon(ServerPlayer player) {
        return MeleeWeaponCompat.isMeleeWeapon(player.getMainHandItem());
    }

    private static void setLeicra(ServerPlayer player, float amount) {
        player.getPersistentData().putFloat(LEICRA, Mth.clamp(amount, 0.0F, maxLeicra(player)));
    }

    private static boolean spendLeicra(ServerPlayer player, float amount) {
        if (amount <= 0.0F) {
            return true;
        }
        float current = leicra(player);
        if (current + 0.001F < amount) {
            return false;
        }
        setLeicra(player, current - amount);
        return true;
    }

    private static void applyHandSpeed(ServerPlayer player, int stacks) {
        var attribute = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attribute == null) {
            return;
        }
        attribute.removeModifier(HAND_SPEED_UUID);
        if (stacks > 0) {
            attribute.addTransientModifier(new AttributeModifier(HAND_SPEED_UUID, stacks * 0.02D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
            ));
        }
    }

    private static void removeHarmfulEffects(LivingEntity entity) {
        for (MobEffectInstance effect : new ArrayList<>(entity.getActiveEffects())) {
            if (effect.getEffect().value().getCategory() == net.minecraft.world.effect.MobEffectCategory.HARMFUL) {
                entity.removeEffect(effect.getEffect());
            }
        }
    }

    private static void playArtSound(ServerPlayer player, LexNinjiaArt art) {
        SoundEvent sound = ModSounds.lexNinjiaSound(art.soundId());
        if (sound != null) {
            RangedSoundHelper.playFollowingPlayer(player, sound, SoundSource.PLAYERS, 1.0F, 1.0F, 32.0D);
        }
    }

    private static void playHamEcho(ServerPlayer player) {
        SoundEvent sound = ModSounds.lexNinjiaSound("lex_ninjia_ham_echo");
        if (sound != null) {
            RangedSoundHelper.playFollowingPlayer(player, sound, SoundSource.PLAYERS, 0.85F, 1.0F, 32.0D);
        }
    }

    private static String comboText(RuntimeState state, long now) {
        if (state.inputs.isEmpty() && state.presetDisplayUntil > now && !state.presetDisplayText.isBlank()) {
            return state.presetDisplayText;
        }
        state.pruneInputs(now);
        StringBuilder builder = new StringBuilder();
        for (InputEntry entry : state.inputs) {
            if (!builder.isEmpty()) {
                builder.append(" + ");
            }
            builder.append(entry.input().name().toLowerCase(java.util.Locale.ROOT));
        }
        return builder.toString();
    }

    private static boolean allNonHamArtsKnown(Player player) {
        for (LexNinjiaArt art : LexNinjiaArt.values()) {
            if (!art.hamForbidden() && !art.defaultKnown() && !isKnown(player, art)) {
                return false;
            }
        }
        return true;
    }

    private static void giveOrDrop(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        if (!player.getInventory().add(stack.copy())) {
            player.level().addFreshEntity(new ItemEntity(player.level(), player.getX(), player.getY(), player.getZ(), stack.copy()));
        }
    }

    private static boolean canConsumeMainHand(ItemStack hand, int count) {
        return !hand.isEmpty() && hand.getCount() >= count;
    }

    private static boolean canConsumeInventoryAfterMain(ServerPlayer player, java.util.function.Predicate<ItemStack> predicate, int count, int reservedMainHandCount) {
        int available = 0;
        ItemStack hand = player.getMainHandItem();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && predicate.test(stack) && !stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().contains(FOOD_ART)) {
                available += stack.getCount();
            }
        }
        if (!hand.isEmpty() && predicate.test(hand) && !hand.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().contains(FOOD_ART)) {
            available -= Math.min(reservedMainHandCount, hand.getCount());
        }
        return available >= count;
    }

    private static boolean consumeMainHand(ServerPlayer player, int count) {
        ItemStack hand = player.getMainHandItem();
        if (!canConsumeMainHand(hand, count)) {
            return false;
        }
        hand.shrink(count);
        return true;
    }

    private static boolean consumeInventory(ServerPlayer player, java.util.function.Predicate<ItemStack> predicate, int count) {
        int remaining = count;
        for (int i = 0; i < player.getInventory().getContainerSize() && remaining > 0; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && predicate.test(stack) && !stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().contains(FOOD_ART)) {
                int take = Math.min(remaining, stack.getCount());
                stack.shrink(take);
                remaining -= take;
            }
        }
        return remaining <= 0;
    }

    private static boolean isSugarLike(ItemStack stack) {
        return stack.is(Items.SUGAR) || stack.is(Items.COOKIE) || stack.is(Items.HONEY_BOTTLE);
    }

    private static boolean isPlantFoodLike(ItemStack stack) {
        return stack.is(Items.WHEAT) || stack.is(Items.CARROT) || stack.is(Items.POTATO) || stack.is(Items.BEETROOT)
                || stack.is(Items.APPLE) || stack.is(Items.MELON_SLICE) || stack.is(Items.SWEET_BERRIES)
                || stack.is(Items.GLOW_BERRIES) || stack.is(Items.BREAD);
    }

    private static boolean isFruitLike(ItemStack stack) {
        return stack.is(Items.APPLE) || stack.is(Items.MELON_SLICE) || stack.is(Items.SWEET_BERRIES)
                || stack.is(Items.GLOW_BERRIES) || stack.is(Items.CHORUS_FRUIT);
    }

    private static boolean isMeatLike(ItemStack stack) {
        return stack.is(Items.BEEF) || stack.is(Items.COOKED_BEEF) || stack.is(Items.PORKCHOP)
                || stack.is(Items.COOKED_PORKCHOP) || stack.is(Items.MUTTON) || stack.is(Items.COOKED_MUTTON)
                || stack.is(Items.CHICKEN) || stack.is(Items.COOKED_CHICKEN) || stack.is(Items.RABBIT)
                || stack.is(Items.COOKED_RABBIT);
    }

    private static boolean isCookedMeatLike(ItemStack stack) {
        return stack.is(Items.COOKED_BEEF) || stack.is(Items.COOKED_PORKCHOP) || stack.is(Items.COOKED_MUTTON)
                || stack.is(Items.COOKED_CHICKEN) || stack.is(Items.COOKED_RABBIT);
    }

    private static boolean isFishLike(ItemStack stack) {
        return stack.is(Items.COD) || stack.is(Items.COOKED_COD) || stack.is(Items.SALMON)
                || stack.is(Items.COOKED_SALMON) || stack.is(Items.TROPICAL_FISH) || stack.is(Items.PUFFERFISH);
    }

    private static boolean isPotion(ItemStack stack, net.minecraft.core.Holder<net.minecraft.world.item.alchemy.Potion> potion) {
        if (!stack.is(Items.POTION)) return false;
        net.minecraft.world.item.alchemy.PotionContents contents = stack.get(net.minecraft.core.component.DataComponents.POTION_CONTENTS);
        return contents != null && contents.potion().filter(p -> p.is(potion)).isPresent();
    }

    private record InputEntry(LexNinjiaComboInput input, long tick) {
    }

    private record TimedBlock(BlockPos pos, long expireTick) {
    }

    private record PendingSlash(UUID target, long tick, float damage, SlashVisual visual, double sideOffset) {
    }

    private enum SlashVisual {
        BURNING,
        ARASHI,
        CLONE
    }

    private static final class RuntimeState {
        private final List<InputEntry> inputs = new ArrayList<>();
        private final List<TimedBlock> wallBlocks = new ArrayList<>();
        private final List<PendingSlash> slashQueue = new ArrayList<>();
        private LexNinjiaArt prepared;
        private int handStacks;
        private int bladeStacks;
        private int harmonyStacks;
        private long handLastTick;
        private long bladeLastTick;
        private long harmonyLastTick;
        private long sneakPressTick;
        private long sneakHeldSince;
        private boolean longSneakRecorded;
        private long rightPressTick;
        private float rightChargeSpent;
        private Vec3 lastPosition = Vec3.ZERO;
        private UUID lastMeleeTarget;
        private UUID lastDamager;
        private UUID handshakeTarget;
        private long burningBladeUntil;
        private long arashiUntil;
        private long deathFlameUntil;
        private float deathFlameOverflow;
        private long shadowBladeUntil;
        private long shadowCloneUntil;
        private long ironRainUntil;
        private long fdHandUntil;
        private long reflectUntil;
        private int reflectStacks;
        private int sharpenStacks;
        private int peaStacks;
        private long peaUntil;
        private long fertilizerUntil;
        private long sleepUntil;
        private boolean sleepRewardPending;
        private long returnHandUntil;
        private boolean doubleLuohanReady;
        private boolean whiteCraneReady;
        private long noRetaliationUntil;
        private long snakePoisonUntil;
        private long shieldUntil;
        private long tenMeterReleaseTick;
        private Vec3 tenMeterOrigin = Vec3.ZERO;
        private boolean hamFriendPact;
        private long hamPowerUntil;
        private long hamBerserkUntil;
        private int hamBerserkStacks;
        private long hamKillReleaseTick;
        private long hamShadowKickUntil;
        private UUID hamBeastId;
        private int hamBeastStacks;
        private String presetDisplayText = "";
        private long presetDisplayUntil;

        private void pruneInputs(long now) {
            inputs.removeIf(entry -> now - entry.tick() > INPUT_EXPIRY_TICKS);
            trimInputs();
        }

        private void trimInputs() {
            while (inputs.size() > MAX_STORED_COMBO_INPUTS) {
                inputs.remove(0);
            }
        }

        private void clearCombatRuntime(long now) {
            inputs.clear();
            prepared = null;
            presetDisplayText = "";
            presetDisplayUntil = 0L;
            burningBladeUntil = 0L;
            arashiUntil = 0L;
            deathFlameUntil = 0L;
            deathFlameOverflow = 0.0F;
            shadowBladeUntil = 0L;
            shadowCloneUntil = 0L;
            ironRainUntil = 0L;
            fdHandUntil = 0L;
            reflectUntil = 0L;
            reflectStacks = 0;
            peaUntil = 0L;
            peaStacks = 0;
            fertilizerUntil = 0L;
            sleepUntil = 0L;
            sleepRewardPending = false;
            returnHandUntil = 0L;
            noRetaliationUntil = 0L;
            snakePoisonUntil = 0L;
            tenMeterReleaseTick = 0L;
            hamBerserkUntil = 0L;
            hamBerserkStacks = 0;
            hamKillReleaseTick = 0L;
            hamShadowKickUntil = 0L;
            hamPowerUntil = 0L;
            hamBeastId = null;
            hamBeastStacks = 0;
            slashQueue.clear();
        }

        private void clearAll() {
            clearCombatRuntime(0L);
            inputs.clear();
            wallBlocks.clear();
            slashQueue.clear();
            prepared = null;
            presetDisplayText = "";
            presetDisplayUntil = 0L;
            handStacks = 0;
            bladeStacks = 0;
            harmonyStacks = 0;
            deathFlameOverflow = 0.0F;
            shieldUntil = 0L;
            handshakeTarget = null;
            lastDamager = null;
            lastMeleeTarget = null;
        }
    }
}
