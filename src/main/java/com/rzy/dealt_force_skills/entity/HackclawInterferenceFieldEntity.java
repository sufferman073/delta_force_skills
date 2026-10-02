package com.rzy.dealt_force_skills.entity;

import com.rzy.dealt_force_skills.advancement.DfsAchievements;
import com.rzy.dealt_force_skills.character.electronics.ElectronicInterferenceManager;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.util.RangedSoundHelper;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.UUID;

public class HackclawInterferenceFieldEntity extends Entity implements ItemSupplier, BlockbenchModelPoseProvider {
    private static final EntityDataAccessor<Integer> ATTACHED_FACE =
            SynchedEntityData.defineId(HackclawInterferenceFieldEntity.class, EntityDataSerializers.INT);
    public static volatile double RADIUS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("RADIUS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.hackclawinterferencefieldentity.radius", 10.0));
    private static volatile int DURATION_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("DURATION_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.hackclawinterferencefieldentity.duration_ticks", 200));
    private static volatile int DEVICE_DISRUPT_INTERVAL_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("DEVICE_DISRUPT_INTERVAL_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue(
      "summons.hackclawinterferencefieldentity.device_disrupt_interval_ticks", 10
   ));
    private UUID ownerId;
    private int age;
    private boolean feedbackTriggered;

    public HackclawInterferenceFieldEntity(EntityType<? extends HackclawInterferenceFieldEntity> type, Level level) {
        super(type, level);
    }

    public HackclawInterferenceFieldEntity(
            EntityType<? extends HackclawInterferenceFieldEntity> type,
            Level level,
            UUID ownerId,
            Direction attachedFace
    ) {
        super(type, level);
        this.ownerId = ownerId;
        entityData.set(ATTACHED_FACE, attachedFace.get3DDataValue());
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(Items.REDSTONE);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(ATTACHED_FACE, Direction.UP.get3DDataValue());
    }

    @Override
    public void tick() {
        super.tick();
        age++;

        if (level().isClientSide) {
            spawnClientParticles();
            return;
        }

        if (age >= DURATION_TICKS) {
            discard();
            return;
        }

        if (age % DEVICE_DISRUPT_INTERVAL_TICKS == 0) {
            int destroyed = ElectronicInterferenceManager.disruptElectronicDevices(this);
            if (destroyed > 0 && level() instanceof ServerLevel level) {
                RangedSoundHelper.playThrottled(level, position(),
                        ModSounds.HACKCLAW_ELECTRONIC_DEVICE_DESTROYED.get(),
                        SoundSource.PLAYERS, 0.9f, 1.0f, 18.0D, 5, 3.0D);
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        ownerId = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        age = tag.getInt("Age");
        feedbackTriggered = tag.getBoolean("FeedbackTriggered");
        entityData.set(ATTACHED_FACE, tag.getInt("AttachedFace"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (ownerId != null) {
            tag.putUUID("Owner", ownerId);
        }
        tag.putInt("Age", age);
        tag.putBoolean("FeedbackTriggered", feedbackTriggered);
        tag.putInt("AttachedFace", entityData.get(ATTACHED_FACE));
    }

    @Override
    public Direction blockbenchAttachedFace() {
        return Direction.from3DDataValue(entityData.get(ATTACHED_FACE));
    }

    public double radius() {
        return RADIUS;
    }

    public boolean isActive() {
        return !isRemoved() && age < DURATION_TICKS;
    }

    public void markSuccessfulInterference() {
        markSuccessfulInterference(1);
    }

    public void markSuccessfulInterference(int affectedDevices) {
        if (feedbackTriggered || !(level() instanceof ServerLevel level)) {
            return;
        }

        feedbackTriggered = true;
        ServerPlayer owner = owner(level);
        if (owner != null) {
            owner.displayClientMessage(Component.translatable(
                    "message.dealt_force_skills.hackclaw.interference_success"), true);
            DfsAchievements.onElectronicInterference(owner, Math.max(1, affectedDevices));
            owner.level().playSound(null, owner.blockPosition(), ModSounds.HACKCLAW_HACK_SUCCESS.get(),
                    SoundSource.PLAYERS, 0.85f, 1.0f);
        }
    }

    public ServerPlayer ownerPlayer(ServerLevel level) {
        return owner(level);
    }

    private ServerPlayer owner(ServerLevel level) {
        return ownerId == null ? null : level.getServer().getPlayerList().getPlayer(ownerId);
    }

    private void spawnClientParticles() {
        if (tickCount % 2 != 0) {
            return;
        }

        double angle = random.nextDouble() * Math.PI * 2.0D;
        double distance = random.nextDouble() * RADIUS;
        double x = getX() + Math.cos(angle) * distance;
        double z = getZ() + Math.sin(angle) * distance;
        double y = getY() + 0.15D + random.nextDouble() * 1.6D;
        level().addParticle(ParticleTypes.ELECTRIC_SPARK, x, y, z, 0.0D, 0.01D, 0.0D);
    }
}
