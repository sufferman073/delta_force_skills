package com.rzy.dealt_force_skills.block;

import com.mojang.serialization.MapCodec;
import com.rzy.dealt_force_skills.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
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

public class HvkAdvancedTreasureCompassBlock extends BaseEntityBlock {
    public static final MapCodec<HvkAdvancedTreasureCompassBlock> CODEC =
            simpleCodec(p -> new HvkAdvancedTreasureCompassBlock());

    public HvkAdvancedTreasureCompassBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .requiresCorrectToolForDrops()
                .strength(4.0F, 6.0F)
                .sound(SoundType.METAL));
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
        return ModBlockEntities.HVK_ADVANCED_TREASURE_COMPASS.get().create(pos, state);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        if (SpecialWorkBlockConfig.shouldPreventPlacement(context,
                SpecialWorkBlockConfig.isHvkAdvancedTreasureCompassEnabled())) {
            return null;
        }
        return super.getStateForPlacement(context);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (SpecialWorkBlockConfig.denyUse(level, player,
                SpecialWorkBlockConfig.isHvkAdvancedTreasureCompassEnabled())) {
            return InteractionResult.CONSUME;
        }
        if (level.getBlockEntity(pos) instanceof HvkAdvancedTreasureCompassBlockEntity compass) {
            return compass.use(player);
        }
        return InteractionResult.PASS;
    }

}
