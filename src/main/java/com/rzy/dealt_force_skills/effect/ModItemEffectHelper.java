package com.rzy.dealt_force_skills.effect;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.commander.CommanderStateManager;
import com.rzy.dealt_force_skills.registry.ModEffects;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ModItemEffectHelper {
    private static final int MAX_SPEED_EFFECT_LEVEL = 100;
    private static final String ROOT_TAG = DealtForceSkillsMod.MODID + ".item_effects";
    private static final String SUPPRESSED_EFFECTS = "SuppressedHarmfulEffects";
    private static final String ATTRIBUTE_SUPPRESSED_EFFECTS = "AttributeSuppressedHarmfulEffects";
    private static final String EFFECT_ID = "Id";
    private static final String DURATION = "Duration";
    private static final String AMPLIFIER = "Amplifier";
    private static final String AMBIENT = "Ambient";
    private static final String VISIBLE = "Visible";
    private static final String SHOW_ICON = "ShowIcon";
    private static final Map<String, Integer> CHARGE_MAXIMUMS = Map.ofEntries(
            Map.entry("BladeWireCharges", 2),
            Map.entry("BlockCharges", 3),
            Map.entry("BombCharges", 2),
            Map.entry("CoverCharges", 2),
            Map.entry("DewarCharges", 2),
            Map.entry("ElbowCharges", 6),
            Map.entry("FlashCharges", 2),
            Map.entry("FlashDroneCharges", 2),
            Map.entry("FragCharges", 2),
            Map.entry("GrenadeCharges", 2),
            Map.entry("HandCannonCharges", 2),
            Map.entry("IncendiaryCharges", 2),
            Map.entry("KnifeCharges", 2),
            Map.entry("MedicalCharges", 2),
            Map.entry("PulseCharges", 2),
            Map.entry("ShockCharges", 2),
            Map.entry("SmokeCharges", 2),
            Map.entry("SpiderCharges", 1),
            Map.entry("StimCharges", 6),
            Map.entry("TearGasCharges", 2),
            Map.entry("TrapCharges", 2),
            Map.entry("WallCharges", 2)
    );

    private ModItemEffectHelper() {
    }

    public static boolean shouldSuppressHarmful(LivingEntity entity, MobEffectInstance effect) {
        return shouldSuppressHarmfulImpact(entity, effect);
    }

    public static boolean shouldSuppressHarmfulImpact(LivingEntity entity, MobEffectInstance effect) {
        return effect != null && shouldSuppressHarmfulImpact(entity, effect.getEffect());
    }

    public static boolean shouldSuppressHarmfulImpact(LivingEntity entity, Holder<MobEffect> effect) {
        return effect != null && shouldSuppressHarmfulImpact(entity, effect.value());
    }

    public static boolean shouldSuppressHarmfulImpact(LivingEntity entity, MobEffect effect) {
        return entity instanceof Player player
                && (player.hasEffect(ModEffects.PAIN_RELIEF) || player.hasEffect(ModEffects.SEDATION))
                && effect != null
                && effect.getCategory() == MobEffectCategory.HARMFUL;
    }

    public static void suppressIncomingHarmful(Player player, MobEffectInstance effect) {
        if (!player.level().isClientSide && shouldSuppressHarmfulImpact(player, effect)) {
            suppressActiveHarmfulAttributes(player);
        }
    }

    public static void tickPainRelief(ServerPlayer player) {
        restoreSuppressedEffects(player);
        if (player.hasEffect(ModEffects.PAIN_RELIEF) || player.hasEffect(ModEffects.SEDATION)) {
            suppressActiveHarmfulAttributes(player);
        } else {
            restoreSuppressedHarmfulAttributes(player);
        }
    }

    public static void tickItemEffects(ServerPlayer player) {
        tickPainRelief(player);
        if (player.hasEffect(ModEffects.SEDATION)) {
            removeActiveNonInjuryHarmfulEffects(player);
        }
    }

    public static void restorePainReliefSuppression(LivingEntity entity) {
        restoreSuppressedEffects(entity);
        restoreSuppressedHarmfulAttributes(entity);
    }

    public static void restoreSuppressedEffects(LivingEntity entity) {
        if (!(entity instanceof Player player) || player.level().isClientSide) {
            return;
        }
        ListTag list = suppressedList(player);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            Holder<MobEffect> effect = effectFromTag(tag);
            int duration = tag.getInt(DURATION);
            if (effect != null && duration > 0) {
                player.addEffect(new MobEffectInstance(effect, duration, tag.getInt(AMPLIFIER),
                        tag.getBoolean(AMBIENT), tag.getBoolean(VISIBLE), tag.getBoolean(SHOW_ICON)));
            }
        }
        list.clear();
    }

    public static boolean removeHarmfulEffect(Player player, int maxAmplifierInclusive, boolean strongest) {
        List<HarmfulRef> candidates = harmfulRefs(player, maxAmplifierInclusive);
        if (candidates.isEmpty()) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                    "message.dealt_force_skills.item.no_harmful_effect"), true);
            return false;
        }
        HarmfulRef selected = selectHarmfulRef(player, candidates, strongest);
        removeHarmfulRef(player, selected);
        return true;
    }

    public static boolean removeHarmfulEffectPreferNonWound(Player player, int maxAmplifierInclusive, boolean strongest) {
        List<HarmfulRef> candidates = harmfulRefs(player, maxAmplifierInclusive);
        if (candidates.isEmpty()) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                    "message.dealt_force_skills.item.no_harmful_effect"), true);
            return false;
        }
        List<HarmfulRef> preferred = new ArrayList<>();
        for (HarmfulRef candidate : candidates) {
            if (!InjuryManager.isWound(candidate.effect().value())) {
                preferred.add(candidate);
            }
        }
        HarmfulRef selected = selectHarmfulRef(player, preferred.isEmpty() ? candidates : preferred, strongest);
        removeHarmfulRef(player, selected);
        return true;
    }

    private static HarmfulRef selectHarmfulRef(Player player, List<HarmfulRef> candidates, boolean strongest) {
        if (strongest) {
            return candidates.stream()
                    .max(Comparator.comparingInt(HarmfulRef::amplifier).thenComparingInt(HarmfulRef::duration))
                    .orElse(candidates.get(0));
        }
        return candidates.get(player.getRandom().nextInt(candidates.size()));
    }

    private static void removeHarmfulRef(Player player, HarmfulRef selected) {
        player.removeEffect(selected.effect());
        removeSuppressed(player, selected.effect());
        player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                "message.dealt_force_skills.item.cleaned_effect",
                selected.effect().value().getDisplayName()), true);
    }

    public static void removeActiveHarmfulEffects(LivingEntity entity) {
        for (MobEffectInstance active : new ArrayList<>(entity.getActiveEffects())) {
            if (active.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
                entity.removeEffect(active.getEffect());
                if (entity instanceof Player player) {
                    removeSuppressed(player, active.getEffect());
                }
            }
        }
    }

    public static void removeActiveNonInjuryHarmfulEffects(LivingEntity entity) {
        for (MobEffectInstance active : new ArrayList<>(entity.getActiveEffects())) {
            if (active.getEffect().value().getCategory() == MobEffectCategory.HARMFUL
                    && !InjuryManager.isInjury(active.getEffect())) {
                entity.removeEffect(active.getEffect());
                if (entity instanceof Player player) {
                    removeSuppressed(player, active.getEffect());
                }
            }
        }
    }

    public static List<HarmfulEffectChoice> harmfulEffectChoices(Player player, int maxAmplifierInclusive) {
        Map<Holder<MobEffect>, HarmfulEffectChoice> choices = new LinkedHashMap<>();
        for (HarmfulRef ref : harmfulRefs(player, maxAmplifierInclusive)) {
            HarmfulEffectChoice existing = choices.get(ref.effect());
            if (existing == null
                    || ref.amplifier() > existing.amplifier()
                    || (ref.amplifier() == existing.amplifier() && ref.duration() > existing.duration())) {
                choices.put(ref.effect(), new HarmfulEffectChoice(ref.effect(), ref.amplifier(), ref.duration()));
            }
        }
        return new ArrayList<>(choices.values());
    }

    public static boolean removeHarmfulEffect(Player player, Holder<MobEffect> selectedEffect, int maxAmplifierInclusive) {
        if (selectedEffect == null) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                    "message.dealt_force_skills.item.no_harmful_effect"), true);
            return false;
        }
        boolean canRemoveActive = false;
        for (MobEffectInstance active : player.getActiveEffects()) {
            if (active.getEffect().equals(selectedEffect)
                    && active.getEffect().value().getCategory() == MobEffectCategory.HARMFUL
                    && active.getAmplifier() <= maxAmplifierInclusive) {
                canRemoveActive = true;
                break;
            }
        }
        boolean removed = canRemoveActive && player.removeEffect(selectedEffect);
        removed |= removeSuppressed(player, selectedEffect);
        if (!removed) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                    "message.dealt_force_skills.item.no_harmful_effect"), true);
            return false;
        }
        player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                "message.dealt_force_skills.item.cleaned_effect",
                selectedEffect.value().getDisplayName()), true);
        return true;
    }

    public static double medicineBehaviorSpeedMultiplier(LivingEntity entity) {
        return medicineUseSpeedMultiplier(entity);
    }

    public static double medicineUseSpeedMultiplier(LivingEntity entity) {
        double multiplier = 1.0D;
        multiplier *= InjuryManager.armActionMultiplier(entity);
        multiplier *= effectMultiplier(entity, ModEffects.LAUGHING_MANIA_I, 0.25D);
        multiplier *= effectMultiplier(entity, ModEffects.STAMINA_BOOST, 0.30D);
        multiplier *= effectMultiplier(entity, ModEffects.CARRY_BOOST, 0.25D);
        multiplier *= effectMultiplier(entity, ModEffects.FATIGUE_REMOVAL, 0.10D);
        multiplier *= effectMultiplier(entity, ModEffects.SMALL_INTERACTION_SPEED, 0.50D);
        multiplier *= effectMultiplier(entity, ModEffects.LARGE_INTERACTION_SPEED, 2.00D);
        multiplier *= effectMultiplier(entity, ModEffects.SMALL_ITEM_USE_SPEED, 0.50D);
        multiplier *= effectMultiplier(entity, ModEffects.LARGE_ITEM_USE_SPEED, 1.00D);
        if (entity.hasEffect(ModEffects.ROUND_START_FREEZE)) {
            multiplier *= 2.00D;
        }
        return multiplier;
    }

    public static double medicineBreakSpeedMultiplier(LivingEntity entity) {
        double multiplier = 1.0D;
        multiplier *= InjuryManager.armActionMultiplier(entity);
        multiplier *= effectMultiplier(entity, ModEffects.LAUGHING_MANIA_I, 0.25D);
        multiplier *= effectMultiplier(entity, ModEffects.STAMINA_BOOST, 0.30D);
        multiplier *= effectMultiplier(entity, ModEffects.CARRY_BOOST, 0.10D);
        multiplier *= effectMultiplier(entity, ModEffects.FATIGUE_REMOVAL, 0.10D);
        multiplier *= effectMultiplier(entity, ModEffects.SMALL_INTERACTION_SPEED, 0.50D);
        multiplier *= effectMultiplier(entity, ModEffects.LARGE_INTERACTION_SPEED, 2.00D);
        return multiplier;
    }

    public static double medicineTaczSpeedMultiplier(LivingEntity entity) {
        return medicineTaczFireRateMultiplier(entity)
                * medicineTaczReloadMultiplier(entity)
                * medicineTaczAimSpeedMultiplier(entity);
    }

    public static double medicineTaczFireRateMultiplier(LivingEntity entity) {
        double multiplier = 1.0D;
        multiplier *= effectMultiplier(entity, ModEffects.LAUGHING_MANIA_I, 0.25D);
        multiplier *= effectMultiplier(entity, ModEffects.LARGE_FIRE_RATE, 2.00D);
        return multiplier;
    }

    public static double medicineTaczReloadMultiplier(LivingEntity entity) {
        double multiplier = 1.0D;
        multiplier *= effectMultiplier(entity, ModEffects.LAUGHING_MANIA_I, 0.25D);
        multiplier *= effectMultiplier(entity, ModEffects.MEDIUM_RELOAD_SPEED, 0.75D);
        multiplier *= effectMultiplier(entity, ModEffects.LARGE_RELOAD_SPEED, 1.00D);
        return multiplier;
    }

    public static double medicineTaczAimSpeedMultiplier(LivingEntity entity) {
        double multiplier = 1.0D;
        multiplier *= InjuryManager.armActionMultiplier(entity);
        multiplier *= effectMultiplier(entity, ModEffects.LAUGHING_MANIA_I, 0.25D);
        multiplier *= effectMultiplier(entity, ModEffects.SMALL_AIM_SPEED, 0.50D);
        multiplier *= effectMultiplier(entity, ModEffects.LARGE_AIM_SPEED, 19.00D);
        return multiplier;
    }

    public static double medicineTaczAimPenaltyMultiplier(LivingEntity entity) {
        return effectMultiplier(entity, ModEffects.LARGE_AIM_PENALTY, 19.00D);
    }

    private static double effectMultiplier(LivingEntity entity, Holder<MobEffect> effect, double perLevelBonus) {
        MobEffectInstance instance = entity.getEffect(effect);
        if (instance == null || shouldSuppressHarmfulImpact(entity, effect)) {
            return 1.0D;
        }
        int level = Math.min(MAX_SPEED_EFFECT_LEVEL - 1, Math.max(0, instance.getAmplifier())) + 1;
        return 1.0D + perLevelBonus * level;
    }

    public static void handleLivingDeath(LivingEntity entity, net.minecraft.world.damagesource.DamageSource source) {
        if (source.getEntity() instanceof ServerPlayer player && player.hasEffect(ModEffects.HELA)) {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("effects.mod_item_effect_helper.effect.regeneration.1.duration_ticks", 5 * 20), com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("effects.mod_item_effect_helper.effect.regeneration.1.amplifier", 2), false, true, true), player);
        }
    }

    public static void refreshSkillCooldowns(ServerPlayer player) {
        refreshSkillCooldowns(player, false);
    }

    public static void refreshSkillCooldowns(ServerPlayer player, boolean fillRoverCharges) {
        CompoundTag persistent = player.getPersistentData();
        for (String key : new ArrayList<>(persistent.getAllKeys())) {
            if (key.startsWith(DealtForceSkillsMod.MODID + ".") && persistent.contains(key, Tag.TAG_COMPOUND)) {
                refreshCooldownFields(persistent.getCompound(key), player);
            }
        }
        com.rzy.dealt_force_skills.character.rover.RoverStateManager.refreshSkills(player, fillRoverCharges);
        player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                "message.dealt_force_skills.item.skill_cooldowns_refreshed"), true);
    }

    public static boolean hasLaughingManiaThree(LivingEntity entity) {
        return entity.hasEffect(ModEffects.LAUGHING_MANIA_III);
    }

    private static void refreshCooldownFields(CompoundTag tag, ServerPlayer player) {
        for (Map.Entry<String, Integer> entry : CHARGE_MAXIMUMS.entrySet()) {
            String chargeKey = entry.getKey();
            if (!tag.contains(chargeKey, Tag.TAG_INT)) {
                continue;
            }
            tag.putInt(chargeKey, CommanderStateManager.skillCapacity(player, entry.getValue()));
            String rechargeKey = chargeKey.replace("Charges", "NextRecharge");
            if (tag.contains(rechargeKey, Tag.TAG_LONG) || tag.contains(rechargeKey, Tag.TAG_INT)) {
                tag.putLong(rechargeKey, 0L);
            }
        }
        for (String key : new ArrayList<>(tag.getAllKeys())) {
            if (tag.contains(key, Tag.TAG_COMPOUND)) {
                refreshCooldownFields(tag.getCompound(key), player);
                continue;
            }
            if (key.contains("NextRecharge") && restoreOneCharge(tag, key, player)) {
                continue;
            }
            if (key.contains("CooldownUntil")
                    && (tag.contains(key, Tag.TAG_LONG) || tag.contains(key, Tag.TAG_INT))) {
                tag.putLong(key, 0L);
            }
        }
    }

    private static boolean restoreOneCharge(CompoundTag tag, String rechargeKey, ServerPlayer player) {
        String chargeKey = rechargeKey.replace("NextRecharge", "Charges");
        Integer baseMaxCharges = CHARGE_MAXIMUMS.get(chargeKey);
        if (baseMaxCharges == null || !tag.contains(chargeKey, Tag.TAG_INT)) {
            if (tag.contains(rechargeKey, Tag.TAG_LONG) || tag.contains(rechargeKey, Tag.TAG_INT)) {
                tag.putLong(rechargeKey, 0L);
            }
            return true;
        }
        int maxCharges = CommanderStateManager.skillCapacity(player, baseMaxCharges);
        tag.putInt(chargeKey, maxCharges);
        tag.putLong(rechargeKey, 0L);
        return true;
    }

    public static void suppressActiveHarmfulAttributes(Player player) {
        if (player.level().isClientSide) {
            return;
        }
        AttributeMap attributes = player.getAttributes();
        ListTag list = attributeSuppressedList(player);
        Set<String> activeKeys = new HashSet<>();
        for (MobEffectInstance active : new ArrayList<>(player.getActiveEffects())) {
            Holder<MobEffect> effect = active.getEffect();
            if (effect.value().getCategory() != MobEffectCategory.HARMFUL) {
                continue;
            }
            ResourceLocation id = BuiltInRegistries.MOB_EFFECT.getKey(effect.value());
            if (id == null) {
                continue;
            }
            effect.value().removeAttributeModifiers(attributes);
            activeKeys.add(id.toString());
            upsertAttributeSuppressed(list, id, active.getAmplifier());
        }
        for (int i = list.size() - 1; i >= 0; i--) {
            if (!activeKeys.contains(list.getCompound(i).getString(EFFECT_ID))) {
                list.remove(i);
            }
        }
    }

    private static void restoreSuppressedHarmfulAttributes(LivingEntity entity) {
        if (!(entity instanceof Player player) || player.level().isClientSide) {
            return;
        }
        ListTag list = attributeSuppressedList(player);
        AttributeMap attributes = player.getAttributes();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            Holder<MobEffect> effect = effectFromTag(tag);
            MobEffectInstance active = effect == null ? null : player.getEffect(effect);
            if (effect != null && active != null && effect.value().getCategory() == MobEffectCategory.HARMFUL) {
                effect.value().addAttributeModifiers(attributes, active.getAmplifier());
            }
        }
        list.clear();
    }

    private static void upsertAttributeSuppressed(ListTag list, ResourceLocation id, int amplifier) {
        String key = id.toString();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            if (key.equals(tag.getString(EFFECT_ID))) {
                tag.putInt(AMPLIFIER, amplifier);
                return;
            }
        }
        CompoundTag tag = new CompoundTag();
        tag.putString(EFFECT_ID, key);
        tag.putInt(AMPLIFIER, amplifier);
        list.add(tag);
    }

    private static void storeSuppressedEffect(Player player, MobEffectInstance effect) {
        ResourceLocation id = BuiltInRegistries.MOB_EFFECT.getKey(effect.getEffect().value());
        if (id == null || effect.isInfiniteDuration()) {
            return;
        }
        ListTag list = suppressedList(player);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag existing = list.getCompound(i);
            if (id.toString().equals(existing.getString(EFFECT_ID))) {
                if (effect.getAmplifier() > existing.getInt(AMPLIFIER)
                        || effect.getDuration() > existing.getInt(DURATION)) {
                    writeEffect(existing, id, effect);
                }
                return;
            }
        }
        CompoundTag tag = new CompoundTag();
        writeEffect(tag, id, effect);
        list.add(tag);
    }

    private static void writeEffect(CompoundTag tag, ResourceLocation id, MobEffectInstance effect) {
        tag.putString(EFFECT_ID, id.toString());
        tag.putInt(DURATION, effect.getDuration());
        tag.putInt(AMPLIFIER, effect.getAmplifier());
        tag.putBoolean(AMBIENT, effect.isAmbient());
        tag.putBoolean(VISIBLE, effect.isVisible());
        tag.putBoolean(SHOW_ICON, effect.showIcon());
    }

    private static List<HarmfulRef> harmfulRefs(Player player, int maxAmplifierInclusive) {
        List<HarmfulRef> candidates = new ArrayList<>();
        for (MobEffectInstance active : player.getActiveEffects()) {
            if (active.getEffect().value().getCategory() == MobEffectCategory.HARMFUL
                    && active.getAmplifier() <= maxAmplifierInclusive) {
                candidates.add(HarmfulRef.active(active));
            }
        }
        ListTag suppressed = suppressedList(player);
        for (int i = 0; i < suppressed.size(); i++) {
            CompoundTag tag = suppressed.getCompound(i);
            Holder<MobEffect> effect = effectFromTag(tag);
            if (effect != null
                    && effect.value().getCategory() == MobEffectCategory.HARMFUL
                    && tag.getInt(AMPLIFIER) <= maxAmplifierInclusive
                    && tag.getInt(DURATION) > 0) {
                candidates.add(HarmfulRef.suppressed(effect, tag.getInt(AMPLIFIER), tag.getInt(DURATION)));
            }
        }
        return candidates;
    }

    private static boolean removeSuppressed(Player player, Holder<MobEffect> effect) {
        ResourceLocation id = BuiltInRegistries.MOB_EFFECT.getKey(effect.value());
        if (id == null) {
            return false;
        }
        boolean removed = false;
        ListTag list = suppressedList(player);
        for (int i = list.size() - 1; i >= 0; i--) {
            if (id.toString().equals(list.getCompound(i).getString(EFFECT_ID))) {
                list.remove(i);
                removed = true;
            }
        }
        return removed;
    }

    private static Holder<MobEffect> effectFromTag(CompoundTag tag) {
        ResourceLocation id = ResourceLocation.tryParse(tag.getString(EFFECT_ID));
        return id == null ? null : BuiltInRegistries.MOB_EFFECT.getHolder(id).orElse(null);
    }

    private static ListTag suppressedList(Player player) {
        CompoundTag root = root(player);
        if (!root.contains(SUPPRESSED_EFFECTS, Tag.TAG_LIST)) {
            root.put(SUPPRESSED_EFFECTS, new ListTag());
        }
        return root.getList(SUPPRESSED_EFFECTS, Tag.TAG_COMPOUND);
    }

    private static ListTag attributeSuppressedList(Player player) {
        CompoundTag root = root(player);
        if (!root.contains(ATTRIBUTE_SUPPRESSED_EFFECTS, Tag.TAG_LIST)) {
            root.put(ATTRIBUTE_SUPPRESSED_EFFECTS, new ListTag());
        }
        return root.getList(ATTRIBUTE_SUPPRESSED_EFFECTS, Tag.TAG_COMPOUND);
    }

    private static CompoundTag root(Player player) {
        CompoundTag persistent = player.getPersistentData();
        if (!persistent.contains(ROOT_TAG, Tag.TAG_COMPOUND)) {
            persistent.put(ROOT_TAG, new CompoundTag());
        }
        return persistent.getCompound(ROOT_TAG);
    }

    private record HarmfulRef(Holder<MobEffect> effect, int amplifier, int duration) {
        static HarmfulRef active(MobEffectInstance instance) {
            return new HarmfulRef(instance.getEffect(), instance.getAmplifier(), instance.getDuration());
        }

        static HarmfulRef suppressed(Holder<MobEffect> effect, int amplifier, int duration) {
            return new HarmfulRef(effect, amplifier, duration);
        }
    }

    public record HarmfulEffectChoice(Holder<MobEffect> effect, int amplifier, int duration) {
    }
}
