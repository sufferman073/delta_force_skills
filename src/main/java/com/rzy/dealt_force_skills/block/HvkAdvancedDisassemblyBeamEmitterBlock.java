package com.rzy.dealt_force_skills.block;

import com.rzy.dealt_force_skills.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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

public class HvkAdvancedDisassemblyBeamEmitterBlock extends BaseEntityBlock {
    public static final MapCodec<HvkAdvancedDisassemblyBeamEmitterBlock> CODEC =
            simpleCodec(p -> new HvkAdvancedDisassemblyBeamEmitterBlock());

    public HvkAdvancedDisassemblyBeamEmitterBlock() {
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
        if (SpecialWorkBlockConfig.shouldPreventPlacement(context, SpecialWorkBlockConfig.isHvkDisassemblerEnabled())) {
            return null;
        }
        return super.getStateForPlacement(context);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.HVK_ADVANCED_DISASSEMBLY_BEAM_EMITTER.get().create(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer,
                            ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        HvkWorkBlockSupport.repelConstructorsAroundEmitter(level, pos, placer);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (SpecialWorkBlockConfig.denyUse(level, player, SpecialWorkBlockConfig.isHvkDisassemblerEnabled())) {
            return InteractionResult.CONSUME;
        }
        if (level.getBlockEntity(pos) instanceof HvkAdvancedDisassemblyBeamEmitterBlockEntity emitter) {
            return emitter.use(player);
        }
        return InteractionResult.PASS;
    }

}
