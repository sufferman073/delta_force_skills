package com.rzy.dealt_force_skills.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;

public class InterdimensionalBlockItem extends BlockItem {
    public InterdimensionalBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip,
                                TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        addLine(tooltip, 1, ChatFormatting.GRAY);
        addLine(tooltip, 2, ChatFormatting.AQUA);
        addLine(tooltip, 3, ChatFormatting.LIGHT_PURPLE);
        addLine(tooltip, 4, ChatFormatting.AQUA);
        addLine(tooltip, 5, ChatFormatting.LIGHT_PURPLE);
        addLine(tooltip, 6, ChatFormatting.LIGHT_PURPLE);
        addLine(tooltip, 7, ChatFormatting.AQUA);
        addLine(tooltip, 8, ChatFormatting.LIGHT_PURPLE);
        addLine(tooltip, 9, ChatFormatting.AQUA);
        addLine(tooltip, 10, ChatFormatting.LIGHT_PURPLE);
        addLine(tooltip, 11, ChatFormatting.AQUA);
    }

    private static void addLine(List<Component> tooltip, int line, ChatFormatting color) {
        tooltip.add(Component.translatable("tooltip.dealt_force_skills.interdimensional_block." + line).withStyle(color));
    }
}
