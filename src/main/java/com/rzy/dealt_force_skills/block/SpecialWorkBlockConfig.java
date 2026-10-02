package com.rzy.dealt_force_skills.block;

import com.rzy.dealt_force_skills.config.DealtForceConfig;
import com.rzy.dealt_force_skills.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;

public final class SpecialWorkBlockConfig {
    private static final String HVK_CONSTRUCTOR = "work_blocks.hvk_advanced_standard_template_constructor.enabled";
    private static final String HVK_DISASSEMBLER = "work_blocks.hvk_advanced_disassembly_beam_emitter.enabled";
    private static final String HVK_CLONE_PROTOTYPE = "work_blocks.hvk_clone_prototype.enabled";
    private static final String HVK_ADVANCED_TREASURE_COMPASS = "work_blocks.hvk_advanced_treasure_compass.enabled";
    private static final String INTERDIMENSIONAL_BLOCK = "work_blocks.interdimensional_block.enabled";
    private static final String HVK_ASARA_ARENA = "work_blocks.hvk_asara_arena.enabled";
    private static final String HVK_ENCHANT_INFUSION = "work_blocks.hvk_enchant_infusion.enabled";

    private SpecialWorkBlockConfig() {
    }

    public static boolean isHvkConstructorEnabled() {
        return DealtForceConfig.booleanValue(HVK_CONSTRUCTOR, true);
    }

    public static boolean isHvkDisassemblerEnabled() {
        return DealtForceConfig.booleanValue(HVK_DISASSEMBLER, true);
    }

    public static boolean isHvkClonePrototypeEnabled() {
        return DealtForceConfig.booleanValue(HVK_CLONE_PROTOTYPE, true);
    }

    public static boolean isHvkAdvancedTreasureCompassEnabled() {
        return DealtForceConfig.booleanValue(HVK_ADVANCED_TREASURE_COMPASS, true);
    }

    public static boolean isInterdimensionalBlockEnabled() {
        return DealtForceConfig.booleanValue(INTERDIMENSIONAL_BLOCK, true);
    }

    public static boolean isHvkAsaraArenaEnabled() { return DealtForceConfig.booleanValue(HVK_ASARA_ARENA, true); }
    public static boolean isHvkEnchantInfusionEnabled() { return DealtForceConfig.booleanValue(HVK_ENCHANT_INFUSION, true); }

    public static boolean isDisabledItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (stack.is(ModItems.HVK_ADVANCED_STANDARD_TEMPLATE_CONSTRUCTOR.get())) {
            return !isHvkConstructorEnabled();
        }
        if (stack.is(ModItems.HVK_ADVANCED_DISASSEMBLY_BEAM_EMITTER.get())) {
            return !isHvkDisassemblerEnabled();
        }
        if (stack.is(ModItems.HVK_ASARA_ARENA.get())) return !isHvkAsaraArenaEnabled();
        if (stack.is(ModItems.HVK_ENCHANT_INFUSION.get())) return !isHvkEnchantInfusionEnabled();
        if (stack.is(ModItems.HVK_CLONE_PROTOTYPE.get())) {
            return !isHvkClonePrototypeEnabled();
        }
        if (stack.is(ModItems.HVK_ADVANCED_TREASURE_COMPASS.get())) {
            return !isHvkAdvancedTreasureCompassEnabled();
        }
        return stack.is(ModItems.INTERDIMENSIONAL_BLOCK.get()) && !isInterdimensionalBlockEnabled();
    }

    public static boolean shouldPreventPlacement(BlockPlaceContext context, boolean enabled) {
        if (enabled) {
            return false;
        }
        notifyDisabled(context.getLevel(), context.getPlayer());
        return true;
    }

    public static boolean denyUse(Level level, Player player, boolean enabled) {
        if (enabled) {
            return false;
        }
        notifyDisabled(level, player);
        return true;
    }

    public static void notifyDisabled(Level level, Player player) {
        if (player != null && (level == null || !level.isClientSide)) {
            notifyDisabled(player);
        }
    }

    public static void notifyDisabled(Player player) {
        if (player != null) {
            player.displayClientMessage(Component.translatable(
                    "message.dealt_force_skills.special_work_block.disabled"), true);
        }
    }
}
