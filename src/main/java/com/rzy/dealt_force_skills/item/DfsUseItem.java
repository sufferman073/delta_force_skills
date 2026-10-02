package com.rzy.dealt_force_skills.item;

import com.rzy.dealt_force_skills.config.DealtForceConfig;
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
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.function.Supplier;

public abstract class DfsUseItem extends QualityTooltipItem {
    private final int useTicks;
    private final String configKey;
    private final Supplier<SoundEvent> startSound;
    private final Supplier<SoundEvent> finishSound;
    private final String startMessageKey;
    private final String finishMessageKey;

    protected DfsUseItem(Properties properties,
                         DfsItemQuality quality,
                         String tooltipKey,
                         int useTicks,
                         Supplier<SoundEvent> startSound,
                         Supplier<SoundEvent> finishSound,
                         String startMessageKey,
                         String finishMessageKey) {
        super(properties, quality, tooltipKey);
        String id = tooltipKey.substring(tooltipKey.lastIndexOf('.') + 1);
        this.configKey = "consumables." + id;
        this.useTicks = DealtForceConfig.intValue(configKey + ".use_ticks", useTicks);
        this.startSound = startSound;
        this.finishSound = finishSound;
        this.startMessageKey = startMessageKey;
        this.finishMessageKey = finishMessageKey;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!canStartUse(level, player, stack)) {
            return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(hand);
        if (!level.isClientSide) {
            play(level, player, startSound);
            message(player, startMessageKey);
        }
        return InteractionResultHolder.consume(stack);
    }

    protected boolean canStartUse(Level level, Player player, ItemStack stack) {
        return true;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide && entity instanceof ServerPlayer player) {
            if (applyUseEffect(stack, level, player)) {
                if (stack.is(com.rzy.dealt_force_skills.registry.ModItems.OE2_COMBAT_STIMULANT.get())) {
                    com.rzy.dealt_force_skills.stamina.StaminaManager.fill(player);
                }
                consumeOneUse(stack, player);
                play(level, player, finishSound);
                message(player, finishMessageKey);
            }
        }
        return stack;
    }

    protected abstract boolean applyUseEffect(ItemStack stack, Level level, ServerPlayer player);

    protected final String configKey() {
        return configKey;
    }

    protected void consumeOneUse(ItemStack stack, ServerPlayer player) {
        if (player.getAbilities().instabuild) {
            return;
        }
        if (stack.isDamageableItem()) {
            InteractionHand hand = player.getUsedItemHand();
            stack.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        } else {
            stack.shrink(1);
        }
    }

    protected static void play(Level level, Player player, Supplier<SoundEvent> sound) {
        if (sound != null) {
            level.playSound(null, player.blockPosition(), sound.get(), SoundSource.PLAYERS, 0.75F, 1.0F);
        }
    }

    protected static void message(Player player, String key) {
        if (key != null && !key.isBlank()) {
            player.displayClientMessage(Component.translatable(key), true);
        }
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return useTicks;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        if (level.isClientSide || !(entity instanceof ServerPlayer player) || useTicks <= 0) {
            return;
        }
        int remainingTicks = Math.max(0, remainingUseDuration);
        int effectiveUseTicks = Math.max(1, Math.min(useTicks, entity.getTicksUsingItem() + remainingTicks));
        int elapsedTicks = Math.max(0, effectiveUseTicks - remainingTicks);
        if (elapsedTicks <= 0 || (elapsedTicks % 5 != 0 && remainingUseDuration > 1)) {
            return;
        }
        int percent = Math.min(100, Math.round(elapsedTicks * 100.0F / effectiveUseTicks));
        player.displayClientMessage(Component.translatable("message.dealt_force_skills.item.use_progress",
                progressBar(elapsedTicks, effectiveUseTicks), percent), true);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return stack.isDamageableItem() ? super.isBarVisible(stack) : false;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return super.getBarWidth(stack);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return super.getBarColor(stack);
    }

    @Override
    protected void addExtraTooltip(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.dealt_force_skills.use_time",
                String.format(java.util.Locale.ROOT, "%.1f", useTicks / 20.0D)));
    }

    private static String progressBar(int ticks, int required) {
        int width = 10;
        int filled = Math.min(width, Math.max(0, Math.round(width * (ticks / (float) Math.max(1, required)))));
        return "[" + "#".repeat(filled) + ".".repeat(width - filled) + "]";
    }
}
