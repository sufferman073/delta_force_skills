package com.rzy.dealt_force_skills.effect;

import com.rzy.dealt_force_skills.mixin.LivingEntityUseItemAccessor;
import com.rzy.dealt_force_skills.config.DealtForceConfig;
import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.registry.ModGameRules;
import com.rzy.dealt_force_skills.skill.SkillDamageHelper;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class InjuryManager {
    private static final int INFINITE_DURATION = -1;
    private static final String DATA_ROOT = "DealtForceInjuryRuntime";
    private static final String FOOD_LEVEL = "FoodLevel";
    private static final String SATURATION = "Saturation";
    private static final String HEAD_BLINDNESS = "HeadBlindness";
    private static volatile float WOUND_ROLL_MULTIPLIER = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("WOUND_ROLL_MULTIPLIER", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.floatValue("effects.injury_manager.wound_probability_multiplier", 0.3F));
    private InjuryManager() {
    }

    public enum Type {
        LEFT_LEG_FRACTURE, RIGHT_LEG_FRACTURE, LEFT_ARM_FRACTURE, RIGHT_ARM_FRACTURE,
        ABDOMEN_INJURY, CHEST_INJURY, HEAD_INJURY,
        LEFT_LEG_WOUND, RIGHT_LEG_WOUND, LEFT_ARM_WOUND, RIGHT_ARM_WOUND,
        ABDOMEN_WOUND, CHEST_WOUND, HEAD_WOUND
    }

    public static void onDamage(ServerPlayer player, DamageSource source, Double estimatedHitHeightRatio) {
        if (!ModGameRules.areWoundsEnabled(player) || source == null) {
            return;
        }
        if (isExplosion(source)) {
            if (roll(player, 0.25F)) apply(player, random(player, Type.LEFT_LEG_FRACTURE, Type.RIGHT_LEG_FRACTURE,
                    Type.LEFT_ARM_FRACTURE, Type.RIGHT_ARM_FRACTURE));
            if (roll(player, 0.25F)) apply(player, random(player, Type.ABDOMEN_INJURY, Type.CHEST_INJURY, Type.HEAD_INJURY));
            if (roll(player, 0.40F)) apply(player, random(player, Type.LEFT_LEG_WOUND, Type.RIGHT_LEG_WOUND,
                    Type.LEFT_ARM_WOUND, Type.RIGHT_ARM_WOUND, Type.ABDOMEN_WOUND, Type.CHEST_WOUND, Type.HEAD_WOUND));
        }
        if (source.is(SkillDamageHelper.SINEVA_GRAPPLE)) {
            apply(player, Type.ABDOMEN_INJURY);
        }
        if (source.is(SkillDamageHelper.LUNA_SHOCK_ARROW) && roll(player, 0.30F)) {
            apply(player, Type.ABDOMEN_INJURY);
        }

        double ratio = estimatedHitHeightRatio == null ? -1.0D : estimatedHitHeightRatio;
        if (ratio >= 0.78D) {
            if (roll(player, 0.75F)) apply(player, Type.HEAD_INJURY);
            if (roll(player, 0.50F)) apply(player, Type.HEAD_WOUND);
        } else if (ratio >= 0.58D) {
            if (roll(player, 0.20F)) apply(player, Type.CHEST_INJURY);
            if (roll(player, 0.25F)) apply(player, Type.CHEST_WOUND);
            if (roll(player, 0.04F)) apply(player, random(player, Type.LEFT_ARM_FRACTURE, Type.RIGHT_ARM_FRACTURE));
            if (roll(player, 0.08F)) apply(player, random(player, Type.LEFT_ARM_WOUND, Type.RIGHT_ARM_WOUND));
        } else if (ratio >= 0.38D) {
            if (roll(player, 0.20F)) apply(player, Type.ABDOMEN_INJURY);
            if (roll(player, 0.25F)) apply(player, Type.ABDOMEN_WOUND);
            if (roll(player, 0.04F)) apply(player, random(player, Type.LEFT_ARM_FRACTURE, Type.RIGHT_ARM_FRACTURE));
            if (roll(player, 0.08F)) apply(player, random(player, Type.LEFT_ARM_WOUND, Type.RIGHT_ARM_WOUND));
        } else if (ratio >= 0.0D) {
            if (roll(player, 0.04F)) apply(player, random(player, Type.LEFT_LEG_FRACTURE, Type.RIGHT_LEG_FRACTURE));
            if (roll(player, 0.08F)) apply(player, random(player, Type.LEFT_LEG_WOUND, Type.RIGHT_LEG_WOUND));
        }
        if (isMelee(source) && roll(player, 0.30F)) {
            apply(player, random(player, Type.LEFT_ARM_FRACTURE, Type.RIGHT_ARM_FRACTURE));
        }
    }

    public static void onFall(ServerPlayer player) {
        if (ModGameRules.areWoundsEnabled(player) && roll(player, 0.30F)) {
            apply(player, random(player, Type.LEFT_LEG_FRACTURE, Type.RIGHT_LEG_FRACTURE));
        }
    }

    public static void onJump(ServerPlayer player) {
        if (isSuppressed(player)) {
            return;
        }
        int fractures = (has(player, Type.LEFT_LEG_FRACTURE) ? 1 : 0)
                + (has(player, Type.RIGHT_LEG_FRACTURE) ? 1 : 0);
        if (fractures > 0) {
            applyTrueDamage(player, 4.0F * fractures);
        }
    }

    public static void tick(ServerPlayer player) {
        if (!ModGameRules.areWoundsEnabled(player)) {
            clearAll(player);
            return;
        }
        if (player.tickCount % 20 == 0) {
            repairDisplayFlags(player);
        }
        boolean suppressed = isSuppressed(player);
        if (!suppressed && hasLegFracture(player)) {
            player.setSprinting(false);
        }
        if (!suppressed && player.tickCount % 80 == 0) {
            int wounds = woundCount(player);
            if (wounds > 0) {
                applyWoundTickDamage(player, wounds);
            }
        }
        tickHeadInjury(player, suppressed);
        tickFoodRecovery(player, suppressed);
    }

    public static boolean apply(ServerPlayer player, Type type) {
        if (!ModGameRules.areWoundsEnabled(player)) {
            return false;
        }
        return applyForced(player, type);
    }

    /**
     * Apply injury ignoring the dealtwound gamerule (bosses / scripted sources).
     */
    public static boolean applyForced(ServerPlayer player, Type type) {
        if (player == null || type == null || !ModGameRules.areWoundsEnabled(player)) {
            return false;
        }
        Holder<MobEffect> effect = effect(type);
        MobEffectInstance current = player.getEffect(effect);
        if (current != null) {
            if (!current.isVisible() || !current.showIcon()) {
                replaceWithVisible(player, current);
                return true;
            }
            return false;
        }
        return player.addEffect(new MobEffectInstance(effect, INFINITE_DURATION, 0, false, true, true));
    }

    public static void applyBladeWireBurst(ServerPlayer player) {
        apply(player, Type.LEFT_LEG_WOUND);
        apply(player, Type.RIGHT_LEG_WOUND);
        if (roll(player, 0.40F)) apply(player, Type.LEFT_ARM_WOUND);
        if (roll(player, 0.40F)) apply(player, Type.RIGHT_ARM_WOUND);
        if (roll(player, 0.60F)) apply(player, Type.ABDOMEN_WOUND);
        if (roll(player, 0.20F)) apply(player, Type.CHEST_WOUND);
    }

    public static void applyBladeWireContact(ServerPlayer player) {
        if (roll(player, 0.40F)) apply(player, Type.LEFT_LEG_WOUND);
        if (roll(player, 0.40F)) apply(player, Type.RIGHT_LEG_WOUND);
    }

    public static boolean removeRandomWound(ServerPlayer player) {
        List<Holder<MobEffect>> present = new ArrayList<>();
        for (Type type : Type.values()) {
            if (isWound(type) && has(player, type)) {
                present.add(effect(type));
            }
        }
        if (present.isEmpty()) {
            return false;
        }
        return player.removeEffect(present.get(player.getRandom().nextInt(present.size())));
    }

    public static void copyInjuries(LivingEntity original, LivingEntity replacement) {
        if (replacement instanceof net.minecraft.world.entity.player.Player player
                && !ModGameRules.areWoundsEnabled(player)) {
            return;
        }
        for (Type type : Type.values()) {
            Holder<MobEffect> effect = effect(type);
            if (original.hasEffect(effect)) {
                replacement.addEffect(new MobEffectInstance(effect, INFINITE_DURATION, 0, false, true, true));
            }
        }
    }

    public static boolean isInjury(Holder<MobEffect> effect) {
        return effect != null && isInjury(effect.value());
    }

    public static boolean isInjury(MobEffect effect) {
        if (effect == null) return false;
        for (Type type : Type.values()) {
            if (effect(type).get() == effect) return true;
        }
        return false;
    }

    public static boolean isWound(Holder<MobEffect> effect) {
        return effect != null && isWound(effect.value());
    }

    public static boolean isWound(MobEffect effect) {
        if (effect == null) return false;
        for (Type type : Type.values()) {
            if (isWound(type) && effect(type).get() == effect) return true;
        }
        return false;
    }

    public static double armActionMultiplier(LivingEntity entity) {
        if (isSuppressed(entity)) return 1.0D;
        double value = 1.0D;
        if (has(entity, Type.LEFT_ARM_FRACTURE)) value *= 0.65D;
        if (has(entity, Type.RIGHT_ARM_FRACTURE)) value *= 0.65D;
        return value;
    }

    public static boolean shouldPreventSprinting(LivingEntity entity) {
        return !isSuppressed(entity) && hasLegFracture(entity);
    }

    public static boolean isSuppressed(LivingEntity entity) {
        return entity != null && (entity.hasEffect(ModEffects.PAIN_RELIEF)
                || entity.hasEffect(ModEffects.SEDATION));
    }

    private static void tickHeadInjury(ServerPlayer player, boolean suppressed) {
        CompoundTag data = data(player);
        if (has(player, Type.HEAD_INJURY) && !suppressed) {
            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 30, 0, true, false, false));
            data.putBoolean(HEAD_BLINDNESS, true);
        } else if (data.getBoolean(HEAD_BLINDNESS)) {
            MobEffectInstance blindness = player.getEffect(MobEffects.BLINDNESS);
            if (blindness != null && blindness.getAmplifier() == 0 && blindness.isAmbient()
                    && !blindness.isVisible() && blindness.getDuration() <= 35) {
                player.removeEffect(MobEffects.BLINDNESS);
            }
            data.remove(HEAD_BLINDNESS);
        }
    }

    private static void tickFoodRecovery(ServerPlayer player, boolean suppressed) {
        CompoundTag data = data(player);
        FoodData food = player.getFoodData();
        int currentFood = food.getFoodLevel();
        float currentSaturation = food.getSaturationLevel();
        if (data.contains(FOOD_LEVEL) && has(player, Type.ABDOMEN_INJURY) && !suppressed) {
            int previousFood = data.getInt(FOOD_LEVEL);
            float previousSaturation = data.getFloat(SATURATION);
            if (currentFood > previousFood) {
                currentFood = previousFood + Math.max(1, (int) Math.ceil((currentFood - previousFood) * 0.25D));
                food.setFoodLevel(currentFood);
            }
            if (currentSaturation > previousSaturation) {
                currentSaturation = previousSaturation + (currentSaturation - previousSaturation) * 0.25F;
                food.setSaturation(currentSaturation);
            }
        }
        data.putInt(FOOD_LEVEL, currentFood);
        data.putFloat(SATURATION, currentSaturation);
    }

    private static void applyTrueDamage(ServerPlayer player, float amount) {
        float health = player.getHealth() - Math.max(0.0F, amount);
        player.setHealth(Math.max(0.0F, health));
        if (health <= 0.0F) {
            player.die(player.damageSources().generic());
        }
    }

    private static void applyWoundTickDamage(ServerPlayer player, float amount) {
        boolean wasUsingItem = player.isUsingItem();
        InteractionHand usedHand = wasUsingItem ? player.getUsedItemHand() : InteractionHand.MAIN_HAND;
        ItemStack usedStack = wasUsingItem ? player.getUseItem().copy() : ItemStack.EMPTY;
        int remainingTicks = wasUsingItem ? player.getUseItemRemainingTicks() : 0;

        applyTrueDamage(player, amount);

        if (wasUsingItem && player.isAlive() && !player.isUsingItem()) {
            restoreItemUse(player, usedHand, usedStack, remainingTicks);
        }
    }

    private static void restoreItemUse(ServerPlayer player, InteractionHand usedHand, ItemStack usedStack, int remainingTicks) {
        if (remainingTicks <= 0 || usedStack.isEmpty()) {
            return;
        }
        ItemStack currentStack = player.getItemInHand(usedHand);
        if (!isSameUseStack(currentStack, usedStack)) {
            return;
        }
        player.startUsingItem(usedHand);
        if (player.isUsingItem()) {
            ((LivingEntityUseItemAccessor) player).dealt_force_skills$setUseItemRemaining(remainingTicks);
        }
    }

    private static boolean isSameUseStack(ItemStack currentStack, ItemStack previousStack) {
        return !currentStack.isEmpty()
                && currentStack.getItem() == previousStack.getItem()
                && currentStack.getDamageValue() == previousStack.getDamageValue()
                && Objects.equals(
                    currentStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY),
                    previousStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY));
    }

    private static int woundCount(LivingEntity entity) {
        int count = 0;
        for (Type type : Type.values()) if (isWound(type) && has(entity, type)) count++;
        return count;
    }

    private static void repairDisplayFlags(ServerPlayer player) {
        for (Type type : Type.values()) {
            MobEffectInstance current = player.getEffect(effect(type));
            if (current != null && (!current.isVisible() || !current.showIcon())) {
                replaceWithVisible(player, current);
            }
        }
    }

    private static void replaceWithVisible(ServerPlayer player, MobEffectInstance current) {
        Holder<MobEffect> effect = current.getEffect();
        MobEffectInstance visible = new MobEffectInstance(effect, current.getDuration(), current.getAmplifier(),
                current.isAmbient(), true, true);
        player.removeEffect(effect);
        player.addEffect(visible);
    }

    public static void clearAll(ServerPlayer player) {
        CompoundTag persistent = player.getPersistentData();
        if (!persistent.contains(DATA_ROOT, Tag.TAG_COMPOUND)
                && player.getActiveEffects().stream().noneMatch(instance -> isInjury(instance.getEffect()))) {
            return;
        }
        for (Type type : Type.values()) {
            player.removeEffect(effect(type));
        }
        persistent.remove(DATA_ROOT);
    }

    private static boolean hasLegFracture(LivingEntity entity) {
        return has(entity, Type.LEFT_LEG_FRACTURE) || has(entity, Type.RIGHT_LEG_FRACTURE);
    }

    private static boolean has(LivingEntity entity, Type type) {
        return entity != null && entity.hasEffect(effect(type));
    }

    private static boolean isWound(Type type) {
        return type.ordinal() >= Type.LEFT_LEG_WOUND.ordinal();
    }

    private static boolean roll(ServerPlayer player, float chance) {
        return player.getRandom().nextFloat() < Math.max(0.0F, chance * WOUND_ROLL_MULTIPLIER);
    }

    private static Type random(ServerPlayer player, Type... values) {
        return values[player.getRandom().nextInt(values.length)];
    }

    private static boolean isMelee(DamageSource source) {
        Entity attacker = source.getEntity();
        return attacker instanceof LivingEntity && source.getDirectEntity() == attacker;
    }

    private static boolean isExplosion(DamageSource source) {
        return source.is(DamageTypeTags.IS_EXPLOSION)
                || source.is(SkillDamageHelper.SHEPHERD_FRAG_GRENADE)
                || source.is(SkillDamageHelper.D_WOLF_HAND_CANNON)
                || source.is(SkillDamageHelper.ULURU_MISSILE)
                || source.is(SkillDamageHelper.ULURU_INCENDIARY)
                || source.is(SkillDamageHelper.GIZMO_SPIDERLING)
                || source.is(SkillDamageHelper.VYRON_MAGNETIC_BOMB)
                || source.is(SkillDamageHelper.DEPARTMENT_TRAP)
                || source.is(SkillDamageHelper.DEPARTMENT_TRAP_MANUAL)
                || source.is(SkillDamageHelper.DEPARTMENT_CORE)
                || source.is(SkillDamageHelper.DEPARTMENT_PASSIVE_BLAST);
    }

    private static CompoundTag data(ServerPlayer player) {
        CompoundTag root = player.getPersistentData();
        if (!root.contains(DATA_ROOT, Tag.TAG_COMPOUND)) {
            root.put(DATA_ROOT, new CompoundTag());
        }
        return root.getCompound(DATA_ROOT);
    }

    private static DeferredHolder<MobEffect, MobEffect> effect(Type type) {
        return switch (type) {
            case LEFT_LEG_FRACTURE -> ModEffects.LEFT_LEG_FRACTURE;
            case RIGHT_LEG_FRACTURE -> ModEffects.RIGHT_LEG_FRACTURE;
            case LEFT_ARM_FRACTURE -> ModEffects.LEFT_ARM_FRACTURE;
            case RIGHT_ARM_FRACTURE -> ModEffects.RIGHT_ARM_FRACTURE;
            case ABDOMEN_INJURY -> ModEffects.ABDOMEN_INJURY;
            case CHEST_INJURY -> ModEffects.CHEST_INJURY;
            case HEAD_INJURY -> ModEffects.HEAD_INJURY;
            case LEFT_LEG_WOUND -> ModEffects.LEFT_LEG_WOUND;
            case RIGHT_LEG_WOUND -> ModEffects.RIGHT_LEG_WOUND;
            case LEFT_ARM_WOUND -> ModEffects.LEFT_ARM_WOUND;
            case RIGHT_ARM_WOUND -> ModEffects.RIGHT_ARM_WOUND;
            case ABDOMEN_WOUND -> ModEffects.ABDOMEN_WOUND;
            case CHEST_WOUND -> ModEffects.CHEST_WOUND;
            case HEAD_WOUND -> ModEffects.HEAD_WOUND;
        };
    }
}
