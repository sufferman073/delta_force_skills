package com.rzy.dealt_force_skills.item;

import com.rzy.dealt_force_skills.effect.ModItemEffectHelper;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.function.Supplier;

public class RepairKitItem extends QualityTooltipItem {
    private static volatile int USE_DURATION = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("USE_DURATION", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("items.repair_kit_item.use_duration", 72000));
    private static volatile int REPAIR_INTERVAL_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("REPAIR_INTERVAL_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("items.repair_kit_item.repair_interval_ticks", 20));
    private static volatile int BASE_COST = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("BASE_COST", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("items.repair_kit_item.base_cost", 100));
    private static volatile int BASE_REPAIR = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("BASE_REPAIR", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("items.repair_kit_item.base_repair", 100));
    private static final String TAG_USE_PROGRESS = "DfsRepairUseProgress";
    private static final String TAG_LAST_PROGRESS_TICK = "DfsRepairLastProgressTick";
    private static final String TAG_NEXT_REPAIR_PROGRESS = "DfsRepairNextProgress";

    private final EquipmentSlot targetSlot;
    private final int windupTicks;
    private final Supplier<SoundEvent> startSound;
    private final Supplier<SoundEvent> workSound;
    private final Supplier<SoundEvent> finishSound;
    private final String startMessageKey;
    private final String workMessageKey;
    private final String failMessageKey;

    public RepairKitItem(Properties properties,
                         DfsItemQuality quality,
                         String tooltipKey,
                         EquipmentSlot targetSlot,
                         int windupTicks,
                         Supplier<SoundEvent> startSound,
                         Supplier<SoundEvent> workSound,
                         Supplier<SoundEvent> finishSound,
                         String startMessageKey,
                         String workMessageKey,
                         String failMessageKey) {
        super(properties, quality, tooltipKey);
        this.targetSlot = targetSlot;
        this.windupTicks = windupTicks;
        this.startSound = startSound;
        this.workSound = workSound;
        this.finishSound = finishSound;
        this.startMessageKey = startMessageKey;
        this.workMessageKey = workMessageKey;
        this.failMessageKey = failMessageKey;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!canRepair(player)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable(failMessageKey), true);
            }
            return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(hand);
        if (!level.isClientSide) {
            CompoundTag startTag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            startTag.putDouble(TAG_USE_PROGRESS, 0.0D);
            startTag.putLong(TAG_LAST_PROGRESS_TICK, level.getGameTime());
            startTag.putInt(TAG_NEXT_REPAIR_PROGRESS, Math.max(0, windupTicks));
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(startTag));
            level.playSound(null, player.blockPosition(), startSound.get(), SoundSource.PLAYERS, 0.75F, 1.0F);
            player.displayClientMessage(Component.translatable(startMessageKey), true);
        }
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        if (level.isClientSide || !(entity instanceof ServerPlayer player)) {
            return;
        }
        int elapsed = advanceUseProgress(level, entity, stack);
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        int nextRepairProgress = tag.contains(TAG_NEXT_REPAIR_PROGRESS)
                ? tag.getInt(TAG_NEXT_REPAIR_PROGRESS)
                : Math.max(0, windupTicks);
        if (elapsed < nextRepairProgress) {
            return;
        }
        if (!repairOnePulse(player, stack)) {
            player.stopUsingItem();
            return;
        }
        tag.putInt(TAG_NEXT_REPAIR_PROGRESS, nextRepairProgress + Math.max(1, REPAIR_INTERVAL_TICKS));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeCharged) {
        if (!level.isClientSide && entity instanceof ServerPlayer player) {
            clearUseStart(stack);
            level.playSound(null, player.blockPosition(), finishSound.get(), SoundSource.PLAYERS, 0.65F, 1.0F);
        }
    }

    private int advanceUseProgress(Level level, LivingEntity entity, ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        long now = level.getGameTime();
        long last = tag.contains(TAG_LAST_PROGRESS_TICK) ? tag.getLong(TAG_LAST_PROGRESS_TICK) : now;
        long elapsed = Math.max(0L, now - last);
        double progress = Math.max(0.0D, tag.getDouble(TAG_USE_PROGRESS));
        progress += elapsed * ModItemEffectHelper.medicineUseSpeedMultiplier(entity);
        tag.putDouble(TAG_USE_PROGRESS, progress);
        tag.putLong(TAG_LAST_PROGRESS_TICK, now);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return (int) Math.min(Integer.MAX_VALUE, Math.floor(progress));
    }

    private void clearUseStart(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (tag.contains(TAG_USE_PROGRESS) || tag.contains(TAG_LAST_PROGRESS_TICK)
                || tag.contains(TAG_NEXT_REPAIR_PROGRESS)) {
            tag.remove(TAG_USE_PROGRESS);
            tag.remove(TAG_LAST_PROGRESS_TICK);
            tag.remove(TAG_NEXT_REPAIR_PROGRESS);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
    }

    private boolean canRepair(Player player) {
        ItemStack target = player.getItemBySlot(targetSlot);
        return !target.isEmpty() && target.isDamageableItem() && target.isDamaged();
    }

    private boolean repairOnePulse(ServerPlayer player, ItemStack kit) {
        ItemStack target = player.getItemBySlot(targetSlot);
        if (target.isEmpty() || !target.isDamageableItem() || !target.isDamaged()) {
            player.displayClientMessage(Component.translatable(failMessageKey), true);
            return false;
        }
        int cost = adjustedCost(target);
        int repair = adjustedRepair(target);
        if (!player.getAbilities().instabuild) {
            int available = kit.getMaxDamage() - kit.getDamageValue();
            if (available <= 0) {
                return false;
            }
            int actualCost = Math.min(cost, available);
            repair = Math.max(1, Math.round(repair * (actualCost / (float) cost)));
            kit.hurtAndBreak(actualCost, player, player.getUsedItemHand() == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        }
        target.setDamageValue(Math.max(0, target.getDamageValue() - repair));
        player.level().playSound(null, player.blockPosition(), workSound.get(), SoundSource.PLAYERS, 0.65F, 1.0F);
        player.displayClientMessage(Component.translatable(workMessageKey, repair), true);
        return !kit.isEmpty() && target.isDamaged();
    }

    private int adjustedCost(ItemStack target) {
        int delta = quality().tier() - DfsItemQuality.of(target).tier();
        if (delta <= 0) {
            return BASE_COST;
        }
        return Math.max(1, Math.round(BASE_COST * Math.max(0.2F, 1.0F - delta * 0.2F)));
    }

    private int adjustedRepair(ItemStack target) {
        int delta = DfsItemQuality.of(target).tier() - quality().tier();
        if (delta <= 0) {
            return BASE_REPAIR;
        }
        return Math.max(1, Math.round((float) (BASE_REPAIR * Math.pow(0.7D, delta))));
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return USE_DURATION;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    protected void addExtraTooltip(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.dealt_force_skills.repair_windup",
                String.format(java.util.Locale.ROOT, "%.1f", windupTicks / 20.0D)));
    }
}
