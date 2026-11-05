package com.rzy.dealt_force_skills.item;

import com.rzy.dealt_force_skills.registry.ModEffects;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.function.Supplier;

public class HealingMedicineItem extends DfsUseItem {
    private static final String MEDICINE_WORK_TICKS = "dealt_force_skills.medicine_work_ticks";

    private final int durabilityPerSecond;
    private final double healPercent;
    private final int painReliefExtraCost;

    public HealingMedicineItem(Properties properties,
                               DfsItemQuality quality,
                               String tooltipKey,
                               int useTicks,
                               Supplier<SoundEvent> startSound,
                               Supplier<SoundEvent> workSound,
                               Supplier<SoundEvent> finishSound,
                               String startMessageKey,
                               String finishMessageKey,
                               int durabilityPerSecond,
                               double healPercent,
                               int painReliefExtraCost) {
        super(properties, quality, tooltipKey, useTicks, startSound, finishSound, startMessageKey, finishMessageKey);
        this.durabilityPerSecond = durabilityPerSecond;
        this.healPercent = healPercent;
        this.painReliefExtraCost = painReliefExtraCost;
        this.workSound = workSound;
    }

    private final Supplier<SoundEvent> workSound;

    @Override
    protected boolean canStartUse(Level level, net.minecraft.world.entity.player.Player player, ItemStack stack) {
        boolean canStart = stack.isDamageableItem() && stack.getDamageValue() < stack.getMaxDamage() && needsHealing(player);
        if (canStart && !level.isClientSide) {
            stack.getOrCreateTag().putInt(MEDICINE_WORK_TICKS, 0);
        }
        return canStart;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        super.onUseTick(level, entity, stack, remainingUseDuration);
        if (level.isClientSide || !(entity instanceof ServerPlayer player)) {
            return;
        }
        int elapsedTicks = Math.max(0, getUseDuration(stack) - Math.max(0, remainingUseDuration));
        if (elapsedTicks <= 0 || elapsedTicks % 20 != 0) {
            return;
        }
        if (!performMedicineWork(level, player, stack, true)) {
            player.stopUsingItem();
            return;
        }
        if (stack.isEmpty() || stack.getDamageValue() >= stack.getMaxDamage() || !needsHealing(player)) {
            player.stopUsingItem();
        }
    }

    @Override
    protected boolean applyUseEffect(ItemStack stack, Level level, ServerPlayer player) {
        int expectedWorkTicks = expectedMedicineWorkTicks(stack);
        while (medicineWorkTicks(stack) < expectedWorkTicks
                && performMedicineWork(level, player, stack, false)) {
            if (stack.isEmpty() || stack.getDamageValue() >= stack.getMaxDamage() || !needsHealing(player)) {
                break;
            }
        }
        clearMedicineWorkTicks(stack);
        if (painReliefExtraCost > 0 && !player.hasEffect(ModEffects.PAIN_RELIEF.get())) {
            int remainingDurability = stack.getMaxDamage() - stack.getDamageValue();
            if (remainingDurability > painReliefExtraCost) {
                stack.hurtAndBreak(painReliefExtraCost, player,
                        broken -> broken.broadcastBreakEvent(player.getUsedItemHand()));
                com.rzy.dealt_force_skills.department.DepartmentConsumables.addEffect(player,new MobEffectInstance(ModEffects.PAIN_RELIEF.get(), com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("items.healing_medicine_item.effect.pain_relief.0.duration_ticks", 30 * 20), com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("items.healing_medicine_item.effect.pain_relief.0.amplifier", 0),
                        false, true, true));
            }
        }
        return true;
    }

    @Override
    protected void consumeOneUse(ItemStack stack, ServerPlayer player) {
        // Medicine durability is consumed during the channel.
    }

    private boolean performMedicineWork(Level level, ServerPlayer player, ItemStack stack, boolean feedback) {
        if (!needsHealing(player) || stack.getDamageValue() >= stack.getMaxDamage()) {
            return false;
        }
        int remainingDurability = stack.getMaxDamage() - stack.getDamageValue();
        int cost = Math.min(durabilityPerSecond, remainingDurability);
        if (cost <= 0) {
            return false;
        }
        ItemStack departmentDose = stack.copy();
        float departmentHealth = player.getHealth();
        stack.hurtAndBreak(cost, player, broken -> broken.broadcastBreakEvent(player.getUsedItemHand()));
        player.heal((float) (player.getMaxHealth() * healPercent));
        com.rzy.dealt_force_skills.department.DepartmentConsumption.healed(player, departmentDose, departmentHealth);
        com.rzy.dealt_force_skills.department.DepartmentTasks.tracker().fail(player, "consume");
        incrementMedicineWorkTicks(stack);
        if (feedback) {
            play(level, player, workSound);
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.item.medicine_work"), true);
        }
        return true;
    }

    private int expectedMedicineWorkTicks(ItemStack stack) {
        return Math.max(1, getUseDuration(stack) / 20);
    }

    private static int medicineWorkTicks(ItemStack stack) {
        return stack.hasTag() ? stack.getOrCreateTag().getInt(MEDICINE_WORK_TICKS) : 0;
    }

    private static void incrementMedicineWorkTicks(ItemStack stack) {
        stack.getOrCreateTag().putInt(MEDICINE_WORK_TICKS, medicineWorkTicks(stack) + 1);
    }

    private static void clearMedicineWorkTicks(ItemStack stack) {
        if (stack.hasTag()) {
            stack.getOrCreateTag().remove(MEDICINE_WORK_TICKS);
        }
    }

    private static boolean needsHealing(LivingEntity entity) {
        return entity.getHealth() < entity.getMaxHealth();
    }

    @Override public boolean useOnOperative(ItemStack stack, com.rzy.dealt_force_skills.entity.OperativeEntity target) {
        if (!needsHealing(target) || stack.getDamageValue() >= stack.getMaxDamage()) return false;
        // One administration completes this item's normal course, bounded by its remaining durability.
        int damage = stack.getDamageValue();
        for (int i = 0; i < expectedMedicineWorkTicks(stack) && needsHealing(target) && damage < stack.getMaxDamage(); i++) {
            target.heal((float) (target.getMaxHealth() * healPercent));
            damage += Math.min(Math.max(1, durabilityPerSecond), stack.getMaxDamage() - damage);
        }
        if (painReliefExtraCost > 0 && stack.getMaxDamage() - damage > painReliefExtraCost
                && !target.hasEffect(ModEffects.PAIN_RELIEF.get())) {
            damage += painReliefExtraCost;
            target.addEffect(new MobEffectInstance(ModEffects.PAIN_RELIEF.get(),
                    com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("items.healing_medicine_item.effect.pain_relief.0.duration_ticks", 600),
                    com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("items.healing_medicine_item.effect.pain_relief.0.amplifier", 0)), target);
        }
        if (damage >= stack.getMaxDamage()) stack.shrink(1); else stack.setDamageValue(damage);
        return true;
    }

    @Override protected void consumeOperativeUse(ItemStack stack) { /* Course already charged above. */ }
}
