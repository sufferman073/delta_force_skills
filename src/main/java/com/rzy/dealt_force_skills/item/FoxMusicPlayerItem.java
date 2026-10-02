package com.rzy.dealt_force_skills.item;

import com.rzy.dealt_force_skills.music.FoxMusicData;
import com.rzy.dealt_force_skills.music.FoxMusicPlayerManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import java.util.List;

public final class FoxMusicPlayerItem extends QualityTooltipItem {
    public FoxMusicPlayerItem(Properties properties) { super(properties, DfsItemQuality.RED, null); }

    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer server) FoxMusicPlayerManager.open(server, stack, 0, "");
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override protected void addExtraTooltip(ItemStack stack, net.minecraft.world.item.Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.dealt_force_skills.fox_music_player.description").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.dealt_force_skills.fox_music_player.discs", FoxMusicData.count(stack), FoxMusicData.CAPACITY).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.dealt_force_skills.fox_music_player.bonuses").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.dealt_force_skills.fox_music_player.use").withStyle(ChatFormatting.GRAY));
    }
}
