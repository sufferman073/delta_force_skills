package com.rzy.dealt_force_skills.block;

import com.mojang.serialization.MapCodec;
import com.rzy.dealt_force_skills.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
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

public final class HvkEnchantInfusionBlock extends BaseEntityBlock {
    public static final MapCodec<HvkEnchantInfusionBlock> CODEC = simpleCodec(properties -> new HvkEnchantInfusionBlock());

    public HvkEnchantInfusionBlock() {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).requiresCorrectToolForDrops()
                .strength(5.0F, 1200.0F).sound(SoundType.METAL));
    }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.HVK_ENCHANT_INFUSION.get().create(pos, state);
    }

    @Nullable @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return SpecialWorkBlockConfig.shouldPreventPlacement(context, SpecialWorkBlockConfig.isHvkEnchantInfusionEnabled())
                ? null : super.getStateForPlacement(context);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (SpecialWorkBlockConfig.denyUse(level, player, SpecialWorkBlockConfig.isHvkEnchantInfusionEnabled())) {
            return InteractionResult.CONSUME;
        }
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof HvkEnchantInfusionBlockEntity infusion) {
            infusion.open(serverPlayer);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!state.is(next.getBlock()) && level.getBlockEntity(pos) instanceof HvkEnchantInfusionBlockEntity infusion) {
            infusion.dropStoredItem();
        }
        super.onRemove(state, level, pos, next, moving);
    }
}
