package com.rzy.dealt_force_skills.entity;

import com.rzy.dealt_force_skills.character.rover.RoverConfig;
import com.rzy.dealt_force_skills.character.rover.RoverSounds;
import com.rzy.dealt_force_skills.character.rover.RoverStateManager;
import com.rzy.dealt_force_skills.registry.ModEntities;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public final class RoverProjectileEntity extends ThrowableItemProjectile {
    private final com.rzy.dealt_force_skills.ai.operative.OperativeAllegiance allegiance = new com.rzy.dealt_force_skills.ai.operative.OperativeAllegiance();
    private java.util.UUID destination;
    public void configureNative(LivingEntity owner, LivingEntity target, boolean smoke) {
        setOwner(owner); allegiance.capture(owner); destination = target.getUUID(); configure(smoke, false);
    }
    public boolean isNativeFor(LivingEntity owner, LivingEntity target, boolean smoke) {
        return allegiance.active() && allegiance.isCaster(owner) && target.getUUID().equals(destination) && this.smoke == smoke;
    }
    private boolean smoke;
    private boolean bounce;
    private int bounces;

    public RoverProjectileEntity(EntityType<? extends RoverProjectileEntity> type, Level level) { super(type, level); }

    public void configure(boolean smoke, boolean bounce) {
        this.smoke = smoke; this.bounce = bounce;
        setItem(new net.minecraft.world.item.ItemStack(smoke ? Items.GRAY_DYE : Items.LIME_DYE));
    }

    @Override protected Item getDefaultItem() { return Items.LIME_DYE; }
    @Override protected double getDefaultGravity() { return smoke ? 0.04D : 0.015D; }

    @Override public void tick() {
        if (!level().isClientSide && allegiance.active() && tickCount >= 200) { discard(); return; }
        super.tick();
        if (!level().isClientSide && tickCount > 200) { if (allegiance.active()) discard(); else burst(); }
    }

    @Override protected boolean canHitEntity(net.minecraft.world.entity.Entity entity) {
        return !(allegiance.active() && smoke) && super.canHitEntity(entity);
    }

    @Override protected void onHitEntity(EntityHitResult hit) {
        if (level().isClientSide) return;
        if (smoke) return;
        if (allegiance.active() && level() instanceof net.minecraft.server.level.ServerLevel level
                && hit.getEntity() instanceof LivingEntity target && allegiance.isEnemy(level, target)) {
            LivingEntity owner = getOwner() instanceof LivingEntity living ? living : null;
            com.rzy.dealt_force_skills.skill.SkillDamageHelper.hurt(target,
                    com.rzy.dealt_force_skills.skill.SkillDamageHelper.trueDamage(level, this, owner), owner, (float) Math.max(0, RoverConfig.INJECTOR_DAMAGE));
            if (target.isAlive()) RoverStateManager.reduceHealth(target, RoverConfig.INJECTOR_DIRECT_REDUCTION);
        }
        if (getOwner() instanceof ServerPlayer owner && hit.getEntity() instanceof LivingEntity target) {
            RoverStateManager.directHit(owner, target, this);
        }
        setPos(hit.getLocation());
        burst();
    }

    @Override protected void onHitBlock(BlockHitResult hit) {
        if (level().isClientSide) return;
        if (smoke && hit.getDirection() == Direction.UP || !smoke && (!bounce || bounces >= 1)) {
            if (smoke) level().playSound(null, hit.getBlockPos(), RoverSounds.SMOKE_LAND, SoundSource.PLAYERS, 0.7F, 1.0F);
            setPos(hit.getLocation()); burst(); return;
        }
        bounces++;
        Vec3 normal = Vec3.atLowerCornerOf(hit.getDirection().getNormal());
        Vec3 velocity = getDeltaMovement();
        setDeltaMovement(velocity.subtract(normal.scale(2 * velocity.dot(normal))).scale(0.75));
        setPos(hit.getLocation().add(normal.scale(0.06)));
        hasImpulse = true;
        if (smoke) level().playSound(null, blockPosition(), RoverSounds.SMOKE_BOUNCE, SoundSource.PLAYERS, 0.65F, 1.1F);
    }

    private void burst() {
        if (isRemoved()) return;
        if (allegiance.active()) {
            RoverFieldEntity field = new RoverFieldEntity(ModEntities.ROVER_FIELD.get(), level());
            field.configureNative(allegiance, smoke); field.setPos(position()); level().addFreshEntity(field);
        } else if (getOwner() instanceof ServerPlayer owner && owner.isAlive()) {
            RoverFieldEntity field = new RoverFieldEntity(ModEntities.ROVER_FIELD.get(), level());
            field.configure(owner, smoke);
            field.setPos(position());
            level().addFreshEntity(field);
            level().playSound(null, blockPosition(), smoke ? RoverSounds.SMOKE_BURST : RoverSounds.INJECTOR_BURST,
                    SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        discard();
    }

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        allegiance.save(tag, tickCount);
        if (destination != null) tag.putUUID("NativeDestination", destination);
        tag.putBoolean("Smoke", smoke); tag.putBoolean("Bounce", bounce); tag.putInt("Bounces", bounces);
    }

    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        configure(tag.getBoolean("Smoke"), tag.getBoolean("Bounce")); bounces = tag.getInt("Bounces");
        int age = allegiance.load(tag, 200); if (allegiance.active()) tickCount = age;
        destination = tag.hasUUID("NativeDestination") ? tag.getUUID("NativeDestination") : null;
    }
}
