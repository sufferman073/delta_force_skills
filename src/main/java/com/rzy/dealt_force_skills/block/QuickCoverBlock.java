package com.rzy.dealt_force_skills.block;

import com.rzy.dealt_force_skills.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;

public class QuickCoverBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<QuickCoverBlock> CODEC =
            simpleCodec(p -> new QuickCoverBlock());
    public static final BooleanProperty HALF = BooleanProperty.create("half");

    private static final VoxelShape NORTH_SOUTH_SHAPE = box(0.0, 0.0, 6.0, 16.0, 16.0, 10.0);
    private static final VoxelShape NORTH_SOUTH_HALF_SHAPE = box(0.0, 0.0, 6.0, 16.0, 8.0, 10.0);
    private static final VoxelShape EAST_WEST_SHAPE = box(6.0, 0.0, 0.0, 10.0, 16.0, 16.0);
    private static final VoxelShape EAST_WEST_HALF_SHAPE = box(6.0, 0.0, 0.0, 10.0, 8.0, 16.0);

    public QuickCoverBlock() {
        super(Properties.of()
                .strength(500.0f, 1200.0f)
                .noOcclusion());
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(HALF, false));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.QUICK_COVER.get().create(pos, state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING, HALF);
    }

    @Override
    public void attack(BlockState state, Level level, BlockPos pos, Player player) {
        if (!level.isClientSide) {
            QuickCoverBlockEntity.destroyCoverAt(level, pos);
            ItemStack weapon = player.getMainHandItem();
            if (!weapon.isEmpty() && !player.getAbilities().instabuild) {
                weapon.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
            }
            return;
        }
        super.attack(state, level, pos, player);
    }

    @Override
    public void onProjectileHit(Level level, BlockState state, BlockHitResult hit, Projectile projectile) {
        if (!level.isClientSide) {
            QuickCoverBlockEntity.damageCoverAt(level, hit.getBlockPos(), projectileDamage(projectile, hit.getLocation()));
        }
        super.onProjectileHit(level, state, hit, projectile);
    }

    @Override
    public void onBlockExploded(BlockState state, Level level, BlockPos pos, Explosion explosion) {
        if (!level.isClientSide) {
            QuickCoverBlockEntity.destroyCoverAt(level, pos);
        }
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        boolean half = state.getValue(HALF);
        if (state.getValue(FACING).getAxis() == Direction.Axis.X) {
            return half ? EAST_WEST_HALF_SHAPE : EAST_WEST_SHAPE;
        }
        return half ? NORTH_SOUTH_HALF_SHAPE : NORTH_SOUTH_SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        entity.makeStuckInBlock(state, new Vec3(0.25, 0.25, 0.25));
    }

    @Override
    public void onRemove(BlockState oldState, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (oldState.getBlock() != newState.getBlock()
                && level.getBlockEntity(pos) instanceof QuickCoverBlockEntity cover) {
            cover.onRemoved();
        }
        super.onRemove(oldState, level, pos, newState, moving);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == ModBlockEntities.QUICK_COVER.get()
                ? (lvl, p, st, be) -> QuickCoverBlockEntity.tick(lvl, p, st, (QuickCoverBlockEntity) be)
                : null;
    }

    public static int projectileDamage(Projectile projectile, Vec3 hitLocation) {
        try {
            Method getDamage = projectile.getClass().getMethod("getDamage", Vec3.class);
            Object result = getDamage.invoke(projectile, hitLocation);
            if (result instanceof Number number) {
                return Math.max(1, (int) Math.ceil(number.doubleValue()));
            }
        } catch (ReflectiveOperationException ignored) {
            // Non-TACZ projectiles use the fallback structural damage below.
        }
        return 25;
    }
}
