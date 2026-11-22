package com.rzy.dealt_force_skills.home;

import com.rzy.dealt_force_skills.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class PersonalDimensionBlock extends BaseEntityBlock {
    final int tier;
    public PersonalDimensionBlock(int tier) {
        super(tier == 0 ? Properties.of().strength(2, 3600000).sound(SoundType.WOOD) : Properties.of().strength(3, 3600000).sound(SoundType.METAL).requiresCorrectToolForDrops());
        this.tier = tier;
    }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new HomeDevice(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide || tier == 0 ? null : createTickerHelper(type, ModBlockEntities.PERSONAL_DIMENSION.get(), HomeDevice::tick);
    }
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        if (player instanceof ServerPlayer user && HomeManager.canInteract(user, pos)) {
            if (player.isShiftKeyDown()) {
                if (level.getBlockEntity(pos) instanceof HomeDevice device) HomeDeviceMenu.open(user, device);
            } else if (!(HomeDimensions.isHome(level) ? HomeManager.leave(user) : HomeManager.enter(user, user.getUUID())))
                user.displayClientMessage(net.minecraft.network.chat.Component.literal("当前无法进出家园"), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
