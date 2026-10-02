package com.rzy.dealt_force_skills.block;

import com.rzy.dealt_force_skills.registry.ModBlockEntities;
import com.rzy.dealt_force_skills.util.MeleeWeaponCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class BladeWireBlock extends Block implements EntityBlock {
    private static final VoxelShape WIRE_SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 2.0, 16.0);
    private static final VoxelShape CORE_SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 5.0, 15.0);

    private final boolean core;

    public BladeWireBlock(boolean core) {
        super(Properties.of()
                .strength(core ? 40.0f : 1.0f, core ? 60.0f : 20.0f)
                .requiresCorrectToolForDrops()
                .noCollission()
                .noOcclusion());
        this.core = core;
    }

    public boolean isCore() {
        return core;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.BLADE_WIRE.get().create(pos, state);
    }

    @Override
    public void attack(BlockState state, Level level, BlockPos pos, Player player) {
        ItemStack weapon = player.getMainHandItem();
        if (!level.isClientSide && canCutWire(weapon)) {
            level.destroyBlock(pos, false, player);
            if (!player.getAbilities().instabuild) {
                weapon.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
            }
            return;
        }
        super.attack(state, level, pos, player);
    }

    @Override
    public void onProjectileHit(Level level, BlockState state, BlockHitResult hit, Projectile projectile) {
        if (!level.isClientSide) {
            BladeWireBlockEntity.damageWireAt(level, hit.getBlockPos(),
                    QuickCoverBlock.projectileDamage(projectile, hit.getLocation()));
        }
        super.onProjectileHit(level, state, hit, projectile);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return core ? CORE_SHAPE : WIRE_SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return core ? CORE_SHAPE : Shapes.empty();
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return true;
    }

    private static boolean canCutWire(ItemStack stack) {
        return MeleeWeaponCompat.isMeleeWeapon(stack);
    }

    @Override
    public void onRemove(BlockState oldState, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (oldState.getBlock() != newState.getBlock()
                && level.getBlockEntity(pos) instanceof BladeWireBlockEntity wire) {
            wire.onRemoved();
        }
        super.onRemove(oldState, level, pos, newState, moving);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == ModBlockEntities.BLADE_WIRE.get()
                ? (lvl, p, st, be) -> BladeWireBlockEntity.tick(lvl, p, st, (BladeWireBlockEntity) be)
                : null;
    }
}
