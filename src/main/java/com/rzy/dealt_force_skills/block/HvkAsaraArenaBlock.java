package com.rzy.dealt_force_skills.block;

import com.mojang.serialization.MapCodec;
import com.rzy.dealt_force_skills.registry.ModBlockEntities;
import com.rzy.dealt_force_skills.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public final class HvkAsaraArenaBlock extends BaseEntityBlock {
    public static final MapCodec<HvkAsaraArenaBlock> CODEC = simpleCodec(properties -> new HvkAsaraArenaBlock());

    public HvkAsaraArenaBlock() {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).requiresCorrectToolForDrops()
                .strength(5.0F, 1200.0F).sound(SoundType.METAL));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.HVK_ASARA_ARENA.get().create(pos, state);
    }

    @Nullable @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return SpecialWorkBlockConfig.shouldPreventPlacement(context, SpecialWorkBlockConfig.isHvkAsaraArenaEnabled())
                ? null : super.getStateForPlacement(context);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (SpecialWorkBlockConfig.denyUse(level, player, SpecialWorkBlockConfig.isHvkAsaraArenaEnabled())) {
            return InteractionResult.CONSUME;
        }
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            HvkAsaraArenaBlockEntity arena = getOrCreateArena(level, pos, state);
            if (arena == null) {
                return InteractionResult.FAIL;
            }
            level.playSound(null, pos, ModSounds.HVK_ASARA_ARENA_OPEN.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            arena.openConfiguration(serverPlayer);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private HvkAsaraArenaBlockEntity getOrCreateArena(Level level, BlockPos pos, BlockState state) {
        if (level.getBlockEntity(pos) instanceof HvkAsaraArenaBlockEntity arena) {
            return arena;
        }
        BlockEntity created = newBlockEntity(pos, state);
        if (!(created instanceof HvkAsaraArenaBlockEntity arena)) {
            return null;
        }
        level.setBlockEntity(arena);
        arena.setChanged();
        return arena;
    }
}
