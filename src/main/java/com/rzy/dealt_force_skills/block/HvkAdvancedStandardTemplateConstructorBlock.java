package com.rzy.dealt_force_skills.block;

import com.rzy.dealt_force_skills.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class HvkAdvancedStandardTemplateConstructorBlock extends BaseEntityBlock {
    public static final MapCodec<HvkAdvancedStandardTemplateConstructorBlock> CODEC =
            simpleCodec(p -> new HvkAdvancedStandardTemplateConstructorBlock());

    public HvkAdvancedStandardTemplateConstructorBlock() {
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
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return SpecialWorkBlockConfig.shouldPreventPlacement(context, SpecialWorkBlockConfig.isHvkConstructorEnabled())
                || HvkWorkBlockSupport.shouldPreventConstructorPlacement(
                context.getLevel(), context.getClickedPos(), context.getPlayer())
                ? null
                : super.getStateForPlacement(context);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.HVK_ADVANCED_STANDARD_TEMPLATE_CONSTRUCTOR.get().create(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                 BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (SpecialWorkBlockConfig.denyUse(level, player, SpecialWorkBlockConfig.isHvkConstructorEnabled())) {
            return InteractionResult.CONSUME;
        }
        if (level.getBlockEntity(pos) instanceof HvkAdvancedStandardTemplateConstructorBlockEntity constructor) {
            return constructor.use(player, InteractionHand.MAIN_HAND);
        }
        return InteractionResult.PASS;
    }

    @Override
    public void onRemove(BlockState oldState, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (oldState.getBlock() != newState.getBlock()
                && level.getBlockEntity(pos) instanceof HvkAdvancedStandardTemplateConstructorBlockEntity constructor) {
            constructor.dropStoredContents();
        }
        super.onRemove(oldState, level, pos, newState, moving);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == ModBlockEntities.HVK_ADVANCED_STANDARD_TEMPLATE_CONSTRUCTOR.get()
                ? (tickerLevel, pos, tickerState, blockEntity) ->
                HvkAdvancedStandardTemplateConstructorBlockEntity.tick(tickerLevel, pos, tickerState,
                        (HvkAdvancedStandardTemplateConstructorBlockEntity) blockEntity)
                : null;
    }
}
