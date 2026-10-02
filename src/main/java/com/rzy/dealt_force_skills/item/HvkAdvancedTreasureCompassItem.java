package com.rzy.dealt_force_skills.item;

import com.rzy.dealt_force_skills.loot.HvkTreasureProgress;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.List;

public class HvkAdvancedTreasureCompassItem extends BlockItem {
    private static final String TOOLTIP_PREFIX = "tooltip.dealt_force_skills.hvk_advanced_treasure_compass.";
    private static final TextColor ORANGE_RED = TextColor.fromRgb(0xFF6A2A);
    private static final TextColor PALE_RED = TextColor.fromRgb(0xFF9A9A);
    private static final TextColor CALL_RED = TextColor.fromRgb(0xFF3030);
    private static final TextColor WHITE = TextColor.fromRgb(0xFFFFFF);

    public HvkAdvancedTreasureCompassItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip,
                                TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        addLine(tooltip, 1, ORANGE_RED);
        addLine(tooltip, 2, PALE_RED);
        addLine(tooltip, 3, ORANGE_RED);
        addLine(tooltip, 4, PALE_RED);
        addLine(tooltip, 5, ORANGE_RED);
        addLine(tooltip, 6, PALE_RED);
        addLine(tooltip, 7, ORANGE_RED);
        addLine(tooltip, 8, PALE_RED);
        addLine(tooltip, 9, ORANGE_RED);
        addLine(tooltip, 10, PALE_RED);
        addLine(tooltip, 11, ORANGE_RED);
        addLine(tooltip, 12, PALE_RED);
        addLine(tooltip, 13, ORANGE_RED);
        addLine(tooltip, 14, WHITE);
        addLine(tooltip, 15, CALL_RED);
        addLine(tooltip, 16, ORANGE_RED);
        addLine(tooltip, 17, CALL_RED);
        addLine(tooltip, 18, PALE_RED);
        addLine(tooltip, 19, ORANGE_RED);
        addLine(tooltip, 20, ORANGE_RED);
        addLine(tooltip, 21, ORANGE_RED);
        addLine(tooltip, 22, PALE_RED);
        addLine(tooltip, 23, ORANGE_RED);
        addLine(tooltip, 24, PALE_RED);
        addLine(tooltip, 25, ORANGE_RED);
        addLine(tooltip, 26, PALE_RED);
        addLine(tooltip, 27, ORANGE_RED);
        addLine(tooltip, 28, PALE_RED);
        addLine(tooltip, 29, ORANGE_RED);
    }

    private static void addLine(List<Component> tooltip, int line, TextColor color) {
        tooltip.add(Component.translatable(TOOLTIP_PREFIX + line).withStyle(style -> style.withColor(color)));
    }

    @Override
    public void onCraftedBy(ItemStack stack, Level level, Player player) {
        super.onCraftedBy(stack, level, player);
        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer)
                || player.getAbilities().instabuild || HvkTreasureProgress.canCraftCompass(player)) {
            return;
        }
        int crafted = Math.max(1, stack.getCount());
        stack.shrink(crafted);
        refund(serverPlayer, Items.COPPER_BLOCK, crafted);
        refund(serverPlayer, Items.IRON_BLOCK, crafted);
        refund(serverPlayer, Items.COAL_BLOCK, crafted);
        refund(serverPlayer, Items.LAPIS_BLOCK, crafted);
        refund(serverPlayer, Items.REDSTONE_BLOCK, crafted);
        refund(serverPlayer, Items.DIAMOND_BLOCK, crafted);
        refund(serverPlayer, Items.QUARTZ_BLOCK, crafted);
        refund(serverPlayer, Items.NETHERITE_BLOCK, crafted);
        player.displayClientMessage(Component.translatable(
                "message.dealt_force_skills.hvk_treasure.compass_craft_locked",
                HvkTreasureProgress.openedChestLootCount(player),
                HvkTreasureProgress.requiredOpenedChestLootForCompass()), true);
    }

    private static void refund(ServerPlayer player, Item item, int count) {
        ItemStack stack = new ItemStack(item, count);
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }
}
