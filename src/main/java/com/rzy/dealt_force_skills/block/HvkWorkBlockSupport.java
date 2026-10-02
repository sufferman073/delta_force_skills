package com.rzy.dealt_force_skills.block;

import com.rzy.dealt_force_skills.registry.ModBlocks;
import com.rzy.dealt_force_skills.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

final class HvkWorkBlockSupport {
    private static final int EXCLUSION_RADIUS = 6;

    private HvkWorkBlockSupport() {
    }

    static boolean shouldPreventConstructorPlacement(Level level, BlockPos pos, LivingEntity placer) {
        if (level == null || level.isClientSide || !hasEmitterNearby(level, pos)) {
            return false;
        }
        if (placer instanceof ServerPlayer player) {
            player.displayClientMessage(Component.translatable(
                    "message.dealt_force_skills.hvk_constructor.repelled_by_disassembler"), true);
        }
        return true;
    }

    static void repelConstructorsAroundEmitter(Level level, BlockPos emitterPos, LivingEntity placer) {
        if (level == null || level.isClientSide) {
            return;
        }
        boolean removedAny = false;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = emitterPos.getX() - EXCLUSION_RADIUS; x <= emitterPos.getX() + EXCLUSION_RADIUS; x++) {
            for (int y = emitterPos.getY() - EXCLUSION_RADIUS; y <= emitterPos.getY() + EXCLUSION_RADIUS; y++) {
                for (int z = emitterPos.getZ() - EXCLUSION_RADIUS; z <= emitterPos.getZ() + EXCLUSION_RADIUS; z++) {
                    cursor.set(x, y, z);
                    if (level.getBlockState(cursor).is(ModBlocks.HVK_ADVANCED_STANDARD_TEMPLATE_CONSTRUCTOR.get())) {
                        level.destroyBlock(cursor, true);
                        removedAny = true;
                    }
                }
            }
        }
        if (removedAny && placer instanceof ServerPlayer player) {
            player.displayClientMessage(Component.translatable(
                    "message.dealt_force_skills.hvk_constructor.ran_away"), true);
            player.playNotifySound(ModSounds.HVK_DISASSEMBLER_REPEL.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }

    static boolean hasSilkTouch(Level level, ItemStack tool) {
        if (level == null || tool == null || tool.isEmpty()) {
            return false;
        }
        return EnchantmentHelper.getItemEnchantmentLevel(
                level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH),
                tool) > 0;
    }

    private static boolean hasEmitterNearby(Level level, BlockPos center) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = center.getX() - EXCLUSION_RADIUS; x <= center.getX() + EXCLUSION_RADIUS; x++) {
            for (int y = center.getY() - EXCLUSION_RADIUS; y <= center.getY() + EXCLUSION_RADIUS; y++) {
                for (int z = center.getZ() - EXCLUSION_RADIUS; z <= center.getZ() + EXCLUSION_RADIUS; z++) {
                    cursor.set(x, y, z);
                    BlockState state = level.getBlockState(cursor);
                    if (state.is(ModBlocks.HVK_ADVANCED_DISASSEMBLY_BEAM_EMITTER.get())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
