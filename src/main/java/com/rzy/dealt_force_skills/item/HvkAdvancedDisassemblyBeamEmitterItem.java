package com.rzy.dealt_force_skills.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;

public class HvkAdvancedDisassemblyBeamEmitterItem extends BlockItem {
    public HvkAdvancedDisassemblyBeamEmitterItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        addLine(tooltip, 1, ChatFormatting.RED);
        addLine(tooltip, 2, ChatFormatting.DARK_RED);
        addLine(tooltip, 3, ChatFormatting.GOLD);
        addLine(tooltip, 4, ChatFormatting.DARK_RED);
        addLine(tooltip, 5, ChatFormatting.GOLD);
        addLine(tooltip, 6, ChatFormatting.DARK_RED);
        addLine(tooltip, 7, ChatFormatting.GOLD);
        addLine(tooltip, 8, ChatFormatting.DARK_RED);
        addLine(tooltip, 9, ChatFormatting.GOLD);
    }

    private static void addLine(List<Component> tooltip, int line, ChatFormatting color) {
        tooltip.add(Component.translatable("tooltip.dealt_force_skills.hvk_disassembler." + line).withStyle(color));
    }
}
