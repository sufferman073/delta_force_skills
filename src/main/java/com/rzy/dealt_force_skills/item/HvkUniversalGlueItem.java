package com.rzy.dealt_force_skills.item;

import com.rzy.dealt_force_skills.climb.HvkClimbGlueManager;
import com.rzy.dealt_force_skills.climb.HvkZiplineManager;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.S2C_SyncGluedPositions;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class HvkUniversalGlueItem extends Item {
    public HvkUniversalGlueItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();

        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.PASS;
        }

        if (player instanceof ServerPlayer serverPlayer && HvkClimbGlueManager.isGlued(serverLevel, pos)) {
            return HvkZiplineManager.beginRide(serverPlayer, serverLevel, pos)
                    ? InteractionResult.CONSUME
                    : InteractionResult.FAIL;
        }

        BlockState state = serverLevel.getBlockState(pos);
        if (!HvkClimbGlueManager.isEligible(state)) {
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.displayClientMessage(
                        Component.translatable("message.dealt_force_skills.hvk_universal_glue.ineligible"), true);
            }
            return InteractionResult.FAIL;
        }

        HvkClimbGlueManager.GlueResult result = HvkClimbGlueManager.glueChain(serverLevel, pos);
        if (result.newlyGlued() == 0) {
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.displayClientMessage(
                        Component.translatable("message.dealt_force_skills.hvk_universal_glue.already_glued"), true);
            }
            return InteractionResult.FAIL;
        }

        NetworkHandler.sendToDimension(
                new S2C_SyncGluedPositions(serverLevel.dimension(), HvkClimbGlueManager.allGluedPositions(serverLevel)),
                serverLevel);

        if (player instanceof ServerPlayer serverPlayer) {
            if (!serverPlayer.getAbilities().instabuild) {
                context.getItemInHand().shrink(1);
            }
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.dealt_force_skills.hvk_universal_glue.applied", result.newlyGlued()), true);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.dealt_force_skills.hvk_universal_glue")
                .withStyle(ChatFormatting.GRAY));
    }
}
