package com.rzy.dealt_force_skills.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;

public class HvkAdvancedStandardTemplateConstructorItem extends BlockItem {
    public HvkAdvancedStandardTemplateConstructorItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        addLine(tooltip, 1, ChatFormatting.RED);
        addLine(tooltip, 2, ChatFormatting.DARK_RED);
        addLine(tooltip, 3, ChatFormatting.DARK_RED);
        addLine(tooltip, 4, ChatFormatting.GOLD);
        addLine(tooltip, 5, ChatFormatting.DARK_RED);
        addLine(tooltip, 6, ChatFormatting.GOLD);
        addLine(tooltip, 7, ChatFormatting.DARK_RED);
        addLine(tooltip, 8, ChatFormatting.GOLD);
        addLine(tooltip, 9, ChatFormatting.DARK_RED);
        addLine(tooltip, 10, ChatFormatting.GOLD);
        addLine(tooltip, 11, ChatFormatting.DARK_RED);
        addLine(tooltip, 12, ChatFormatting.GOLD);
    }

    private static void addLine(List<Component> tooltip, int line, ChatFormatting color) {
        tooltip.add(Component.translatable("tooltip.dealt_force_skills.hvk_constructor." + line).withStyle(color));
    }
}
