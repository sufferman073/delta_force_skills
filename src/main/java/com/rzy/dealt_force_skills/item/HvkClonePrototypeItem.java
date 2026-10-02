package com.rzy.dealt_force_skills.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;

public class HvkClonePrototypeItem extends BlockItem {
    public HvkClonePrototypeItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip,
                                TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        addLine(tooltip, 1, ChatFormatting.GREEN);
        addLine(tooltip, 2, ChatFormatting.GRAY);
        addLine(tooltip, 3, ChatFormatting.GREEN);
        addLine(tooltip, 4, ChatFormatting.GRAY);
        addLine(tooltip, 5, ChatFormatting.GREEN);
    }

    private static void addLine(List<Component> tooltip, int line, ChatFormatting color) {
        tooltip.add(Component.translatable("tooltip.dealt_force_skills.hvk_clone_prototype." + line).withStyle(color));
    }
}
