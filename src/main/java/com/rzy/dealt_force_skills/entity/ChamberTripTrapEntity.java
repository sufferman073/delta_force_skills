package com.rzy.dealt_force_skills.entity;

import com.rzy.dealt_force_skills.character.chamber.ChamberStateManager;
import com.rzy.dealt_force_skills.registry.ModEntities;
import com.rzy.dealt_force_skills.util.TargetingUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.network.syncher.SynchedEntityData;

public class ChamberTripTrapEntity extends Entity implements ItemSupplier {
    private static final double TRIGGER_RADIUS = 4.0D;
    private static final double MAX_OWNER_DISTANCE = 150.0D;
    private UUID ownerId;

    public ChamberTripTrapEntity(EntityType<? extends ChamberTripTrapEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    public ChamberTripTrapEntity(EntityType<? extends ChamberTripTrapEntity> type, Level level, ServerPlayer owner) {
        this(type, level);
        ownerId = owner.getUUID();
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(Items.LIGHT_WEIGHTED_PRESSURE_PLATE);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        ServerPlayer owner = owner(level);
        if (owner == null || owner.level() != level || !ChamberStateManager.isChamber(owner)
                || owner.distanceToSqr(this) > MAX_OWNER_DISTANCE * MAX_OWNER_DISTANCE) {
            if (owner != null && ChamberStateManager.isChamber(owner)) {
                ChamberStateManager.setTrapCooldown(owner);
            }
            discard();
            return;
        }
        if (tickCount % 5 == 0) {
            findTriggerTarget(level, owner).ifPresent(target -> trigger(level, owner, target));
        }
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide && amount > 0.0F) {
            ServerPlayer owner = level() instanceof ServerLevel level ? owner(level) : null;
            if (owner != null) {
                ChamberStateManager.setTrapCooldown(owner);
                owner.displayClientMessage(Component.translatable("message.dealt_force_skills.chamber.trap_destroyed"), true);
            }
            discard();
        }
        return true;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        ownerId = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (ownerId != null) {
            tag.putUUID("Owner", ownerId);
        }
    }

    public boolean isOwnedBy(UUID owner) {
        return ownerId != null && ownerId.equals(owner);
    }

    private Optional<LivingEntity> findTriggerTarget(ServerLevel level, ServerPlayer owner) {
        AABB box = new AABB(position(), position()).inflate(TRIGGER_RADIUS);
        return level.getEntitiesOfClass(LivingEntity.class, box, entity -> canTrigger(owner, entity))
                .stream()
                .filter(this::hasLineOfSightTo)
                .min(Comparator.comparingDouble(target -> target.distanceToSqr(this)));
    }

    private boolean canTrigger(ServerPlayer owner, LivingEntity entity) {
        return entity.isAlive()
                && TargetingUtil.isHostileLivingFor(owner, entity)
                && (entity instanceof Player || entity instanceof Enemy);
    }

    private void trigger(ServerLevel level, ServerPlayer owner, LivingEntity target) {
        ChamberSlowFieldEntity field = new ChamberSlowFieldEntity(ModEntities.CHAMBER_SLOW_FIELD.get(), level, owner);
        Vec3 center = target.position();
        field.setPos(center.x, center.y, center.z);
        level.addFreshEntity(field);
        ChamberStateManager.setTrapCooldown(owner);
        owner.displayClientMessage(Component.translatable("message.dealt_force_skills.chamber.trap_triggered"), true);
        discard();
    }

    private boolean hasLineOfSightTo(LivingEntity target) {
        Vec3 start = position().add(0.0D, getBbHeight() * 0.5D, 0.0D);
        Vec3 end = target.getEyePosition();
        return level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)).getType()
                == HitResult.Type.MISS;
    }

    private ServerPlayer owner(ServerLevel level) {
        return ownerId == null ? null : level.getServer().getPlayerList().getPlayer(ownerId);
    }
}
