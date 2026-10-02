package com.rzy.dealt_force_skills.item;

import com.rzy.dealt_force_skills.character.CharacterAvailability;
import com.rzy.dealt_force_skills.character.CharacterSelectionManager;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.S2C_OpenCharacterSelection;
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

public class DriftwoodItem extends Item {
    public DriftwoodItem(Properties properties) {
        super(properties);
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

        if (!CharacterSelectionManager.hasSelectedCharacter(serverPlayer)) {
            if (CharacterSelectionManager.isNormalPlayer(serverPlayer)) {
                CharacterSelectionManager.clearNormalPlayer(serverPlayer);
            }
            CharacterAvailability.syncToClient(serverPlayer);
            NetworkHandler.sendToPlayer(new S2C_OpenCharacterSelection(), serverPlayer);
            return InteractionResultHolder.success(stack);
        }

        CharacterAvailability.syncToClient(serverPlayer);
        NetworkHandler.sendToPlayer(new S2C_OpenCharacterSelection(true), serverPlayer);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.dealt_force_skills.driftwood")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
