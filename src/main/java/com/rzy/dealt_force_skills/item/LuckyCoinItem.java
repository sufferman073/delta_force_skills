package com.rzy.dealt_force_skills.item;

import com.rzy.dealt_force_skills.character.gambler.GamblerStateManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class LuckyCoinItem extends QualityTooltipItem {
    public LuckyCoinItem(Properties properties) {
        super(properties, DfsItemQuality.GOLD, "tooltip.dealt_force_skills.lucky_coin");
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return InteractionResultHolder.success(stack);
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.pass(stack);
        }
        if (!GamblerStateManager.isGambler(serverPlayer)) {
            serverPlayer.displayClientMessage(Component.translatable("message.dealt_force_skills.gambler.lucky_coin_not_gambler"), true);
            return InteractionResultHolder.success(stack);
        }
        GamblerStateManager.grantChips(serverPlayer, 999);
        serverPlayer.displayClientMessage(Component.translatable("message.dealt_force_skills.gambler.lucky_coin_used", 999)
                .withStyle(ChatFormatting.GOLD), false);
        return InteractionResultHolder.success(stack);
    }

    @Override
    protected void addExtraTooltip(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.dealt_force_skills.lucky_coin.test").withStyle(ChatFormatting.GOLD));
    }
}
