package com.rzy.dealt_force_skills.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;

public final class HvkLoreBlockItem extends BlockItem {
    public enum Kind { ASARA_ARENA, ENCHANT_INFUSION, ADVANCED_SPAWNER }

    private final Kind kind;

    public HvkLoreBlockItem(Block block, Properties properties, Kind kind) {
        super(block, properties);
        this.kind = kind;
    }

    @Override
    public Component getName(ItemStack stack) {
        Component name = super.getName(stack);
        return kind == Kind.ASARA_ARENA ? name.copy().withStyle(ChatFormatting.RED) : name;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        if (kind == Kind.ASARA_ARENA) {
            addRange(tooltip, "hvk_asara_arena", 1, 6, ChatFormatting.WHITE);
            tooltip.add(Component.empty());
            addRange(tooltip, "hvk_asara_arena", 7, 15, ChatFormatting.WHITE);
            tooltip.add(Component.empty());
            addRange(tooltip, "hvk_asara_arena", 16, 16, ChatFormatting.RED);
        } else if (kind == Kind.ENCHANT_INFUSION) {
            addRange(tooltip, "hvk_enchant_infusion", 1, 2, ChatFormatting.GRAY);
            addRange(tooltip, "hvk_enchant_infusion", 3, 5, ChatFormatting.WHITE);
            addRange(tooltip, "hvk_enchant_infusion", 6, 6, ChatFormatting.GRAY);
        } else {
            tooltip.add(Component.translatable("tooltip.dealt_force_skills.hvk_advanced_spawner.obtain")
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    private static void addRange(List<Component> tooltip, String key, int first, int last, ChatFormatting color) {
        for (int line = first; line <= last; line++) {
            tooltip.add(Component.translatable("tooltip.dealt_force_skills." + key + "." + line).withStyle(color));
        }
    }
}
