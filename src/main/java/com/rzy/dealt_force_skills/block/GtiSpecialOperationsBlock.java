package com.rzy.dealt_force_skills.block;

import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.S2C_OpenGtiSpecialOperations;
import com.rzy.dealt_force_skills.registry.ModBlockEntities;
import com.rzy.dealt_force_skills.shop.GtiTradingHallManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import com.mojang.serialization.MapCodec;
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

public final class GtiSpecialOperationsBlock extends BaseEntityBlock {
    public static final MapCodec<GtiSpecialOperationsBlock> CODEC = simpleCodec(properties -> new GtiSpecialOperationsBlock());
    public GtiSpecialOperationsBlock() {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).requiresCorrectToolForDrops()
                .strength(5.0F, 1200.0F).sound(SoundType.METAL));
    }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.GTI_SPECIAL_OPERATIONS.get().create(pos, state);
    }

    @Nullable @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return SpecialWorkBlockConfig.shouldPreventPlacement(context, true)
                ? null : super.getStateForPlacement(context);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;
        GtiTradingHallManager.grantBaseAccess(serverPlayer);
        NetworkHandler.sendToPlayer(new S2C_OpenGtiSpecialOperations(), serverPlayer);
        return InteractionResult.CONSUME;
    }

    @Override
    public void attack(BlockState state, Level level, BlockPos pos, Player player) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer && player.isShiftKeyDown()) {
            GtiTradingHallManager.tryRandomReward(serverPlayer);
        }
        super.attack(state, level, pos, player);
    }

}
