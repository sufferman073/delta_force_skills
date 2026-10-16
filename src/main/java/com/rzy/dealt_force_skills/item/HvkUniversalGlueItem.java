package com.rzy.dealt_force_skills.item;

import com.rzy.dealt_force_skills.climb.HvkClimbGlueManager;
import com.rzy.dealt_force_skills.climb.HvkZiplineManager;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.S2C_SyncGluedPositions;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import java.util.List;

public class HvkUniversalGlueItem extends Item {
    private static final String PENDING = "dealt_force_skills.glue_start";
    public HvkUniversalGlueItem(Properties properties) { super(properties); }

    @Override public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().isClientSide()) return InteractionResult.SUCCESS;
        if (!(context.getLevel() instanceof ServerLevel level) || !(context.getPlayer() instanceof ServerPlayer player)
                || player.isSpectator() || !player.mayBuild()) return InteractionResult.FAIL;
        BlockPos pos = context.getClickedPos();
        if (HvkClimbGlueManager.isGlued(level, pos)) {
            return HvkZiplineManager.beginRide(player, level, pos) ? InteractionResult.CONSUME : InteractionResult.FAIL;
        }
        if (!HvkClimbGlueManager.isEligible(level.getBlockState(pos))) return feedback(player, "ineligible", InteractionResult.FAIL);
        CompoundTag saved = player.getPersistentData().getCompound(PENDING);
        String dimension = level.dimension().location().toString();
        if (!saved.contains("Pos") || !dimension.equals(saved.getString("Dimension"))) {
            mark(player, pos, dimension);
            return feedback(player, "start", InteractionResult.CONSUME);
        }
        BlockPos start = BlockPos.of(saved.getLong("Pos"));
        if (start.equals(pos)) {
            player.getPersistentData().remove(PENDING);
            return feedback(player, "cancelled", InteractionResult.CONSUME);
        }
        if (level.hasChunkAt(start) && (!HvkClimbGlueManager.isEligible(level.getBlockState(start)) || HvkClimbGlueManager.isGlued(level, start))) {
            mark(player, pos, dimension);
            return feedback(player, "start", InteractionResult.CONSUME);
        }
        if (!HvkClimbGlueManager.connect(level, start, pos, player.isShiftKeyDown())) return feedback(player, "link_failed", InteractionResult.FAIL);
        player.getPersistentData().remove(PENDING);
        if (!player.getAbilities().instabuild) context.getItemInHand().shrink(1);
        NetworkHandler.sendToDimension(new S2C_SyncGluedPositions(level.dimension(), HvkClimbGlueManager.allGluedPositions(level), HvkClimbGlueManager.skillBlockedLinks(level)), level);
        return feedback(player, player.isShiftKeyDown() ? "applied_restricted" : "applied", InteractionResult.CONSUME);
    }
    private static void mark(ServerPlayer player, BlockPos pos, String dimension) {
        CompoundTag saved = new CompoundTag();
        saved.putLong("Pos", pos.asLong()); saved.putString("Dimension", dimension);
        player.getPersistentData().put(PENDING, saved);
    }
    private static InteractionResult feedback(ServerPlayer player, String key, InteractionResult result) {
        player.displayClientMessage(Component.translatable("message.dealt_force_skills.hvk_universal_glue." + key), true);
        return result;
    }
    @Override public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.dealt_force_skills.hvk_universal_glue").withStyle(ChatFormatting.GRAY));
    }
}
