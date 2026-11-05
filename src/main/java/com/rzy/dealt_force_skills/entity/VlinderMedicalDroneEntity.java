package com.rzy.dealt_force_skills.entity;

import com.rzy.dealt_force_skills.character.vlinder.VlinderDroneMode;
import com.rzy.dealt_force_skills.character.vlinder.VlinderStateManager;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.team.DealtTeamManager;
import com.rzy.dealt_force_skills.util.TargetingUtil;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import org.joml.Vector3f;

import java.util.UUID;
import com.rzy.dealt_force_skills.ai.operative.*;
import com.rzy.dealt_force_skills.character.SkillSlot;

public class VlinderMedicalDroneEntity extends Entity implements ItemSupplier {
    private static volatile int LIFE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("LIFE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.vlindermedicaldroneentity.life_ticks", 200));
    private static volatile double SPEED = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("SPEED", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.vlindermedicaldroneentity.speed", 0.4));
    private static volatile double HIT_DISTANCE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("HIT_DISTANCE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.vlindermedicaldroneentity.hit_distance", 1.5));
    private static volatile double MAX_OWNER_DISTANCE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("MAX_OWNER_DISTANCE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.vlindermedicaldroneentity.max_owner_distance", 128.0));
    private static final DustParticleOptions HEAL_DUST = new DustParticleOptions(new Vector3f(0.45f, 1.0f, 0.68f), 1.0f);
    private static final DustParticleOptions WASTE_DUST = new DustParticleOptions(new Vector3f(0.55f, 0.35f, 1.0f), 1.0f);


    private final OperativeAllegiance allegiance = new OperativeAllegiance();
    private int missingTicks;
    private UUID ownerId;
    private UUID targetId;
    private VlinderDroneMode mode = VlinderDroneMode.HEAL;
    private float health = 6.0F;

    public VlinderMedicalDroneEntity(EntityType<? extends VlinderMedicalDroneEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public VlinderMedicalDroneEntity(
            EntityType<? extends VlinderMedicalDroneEntity> type,
            Level level,
            LivingEntity owner,
            ServerPlayer target,
            VlinderDroneMode mode
    ) {
        this(type, level);
        ownerId = owner.getUUID();
        targetId = target.getUUID();
        this.mode = mode;
        allegiance.capture(owner);
    }


    public VlinderMedicalDroneEntity(EntityType<? extends VlinderMedicalDroneEntity> type, Level level,
            LivingEntity owner, LivingEntity target, VlinderDroneMode mode) {
        this(type, level);
        ownerId = owner.getUUID(); targetId = target.getUUID(); this.mode = mode;
        allegiance.capture(owner);
    }
    public boolean isNativeFor(LivingEntity owner, LivingEntity target) {
        return isAlive() && allegiance.active() && owner.getUUID().equals(ownerId) && target.getUUID().equals(targetId);
    }
    private void tickNative(ServerLevel level) {
        LivingEntity owner = owner(level);
        Entity found = targetId == null ? null : level.getEntity(targetId);
        if (tickCount > LIFE_TICKS || health <= 0) { discard(); return; }
        if (owner == null || found == null) { if (++missingTicks > 40) discard(); return; }
        missingTicks = 0;
        if (!(found instanceof LivingEntity target) || !TargetingUtil.isTargetableLiving(target)
                || !NativeVlinderSkills.deviceAvailable(owner, SkillSlot.ACTIVE_1, target.position())
                || owner.distanceToSqr(this) > MAX_OWNER_DISTANCE * MAX_OWNER_DISTANCE
                || (mode == VlinderDroneMode.HEAL ? !OperativeSupport.ally(owner, target) || OperativeSupport.downed(target)
                    : !allegiance.isEnemy(level, target)) || !owner.hasLineOfSight(target)) { discard(); return; }
        Vec3 destination = target.position().add(0, target.getBbHeight() * 0.58, 0);
        Vec3 delta = destination.subtract(position());
        if (delta.lengthSqr() <= HIT_DISTANCE * HIT_DISTANCE) {
            if (mode == VlinderDroneMode.HEAL) VlinderStateManager.applyHealingDust(owner, target);
            else VlinderStateManager.applyMedicalWaste(owner, target);
            level.playSound(null, target.blockPosition(), ModSounds.VLINDER_MEDICAL_DRONE_HIT.get(), SoundSource.PLAYERS, 0.75F, 1);
            discard(); return;
        }
        Vec3 motion = delta.normalize().scale(Math.min(Math.max(0.01, SPEED), delta.length()));
        if (level.clip(new net.minecraft.world.level.ClipContext(position(), position().add(motion),
                net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, this)).getType()
                != net.minecraft.world.phys.HitResult.Type.MISS) { discard(); return; }
        setDeltaMovement(motion); move(MoverType.SELF, motion);
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(mode == VlinderDroneMode.HEAL ? Items.HONEYCOMB : Items.PURPLE_DYE);
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            spawnClientParticles();
            return;
        }
        if (!(level() instanceof ServerLevel level)) {
            discard();
            return;
        }
        if (allegiance.active()) { tickNative(level); return; }
        LivingEntity owner = owner(level);
        ServerPlayer target = target(level);
        if (owner == null || target == null || tickCount > LIFE_TICKS
                || owner.distanceToSqr(this) > MAX_OWNER_DISTANCE * MAX_OWNER_DISTANCE) {
            discard();
            return;
        }
        if (mode == VlinderDroneMode.HEAL
                && (!(owner instanceof ServerPlayer player) || !DealtTeamManager.areTeammates(player, target))) {
            discard();
            return;
        }
        if (mode != VlinderDroneMode.HEAL
                && (owner instanceof ServerPlayer player && DealtTeamManager.areTeammates(player, target)
                || owner instanceof AcceleratedTroopsEntity troops && !troops.isValidEnemy(target))) {
            discard();
            return;
        }

        Vec3 targetCenter = target.position().add(0.0D, target.getBbHeight() * 0.58D, 0.0D);
        Vec3 toTarget = targetCenter.subtract(position());
        if (toTarget.length() <= HIT_DISTANCE) {
            applyToTarget(owner, target, level);
            discard();
            return;
        }
        Vec3 motion = toTarget.lengthSqr() < 0.0001D ? Vec3.ZERO : toTarget.normalize().scale(SPEED);
        setDeltaMovement(motion);
        move(MoverType.SELF, motion);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        ownerId = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        int nativeAge = allegiance.load(tag, LIFE_TICKS);
        if (allegiance.active()) tickCount = nativeAge;
        missingTicks = 0;
        targetId = tag.hasUUID("Target") ? tag.getUUID("Target") : null;
        int ordinal = tag.getInt("Mode");
        VlinderDroneMode[] modes = VlinderDroneMode.values();
        mode = ordinal >= 0 && ordinal < modes.length ? modes[ordinal] : VlinderDroneMode.HEAL;
        health = tag.contains("Health") ? tag.getFloat("Health") : 6.0F;
        if (allegiance.active()) health = Float.isFinite(health) ? Math.max(0, Math.min(6, health)) : 0;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        allegiance.save(tag, tickCount);
        if (ownerId != null) {
            tag.putUUID("Owner", ownerId);
        }
        if (targetId != null) {
            tag.putUUID("Target", targetId);
        }
        tag.putInt("Mode", mode.ordinal());
        tag.putFloat("Health", health);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide || amount <= 0.0F) {
            return true;
        }
        if (allegiance.active() && level() instanceof ServerLevel level
                && allegiance.isAlly(level, source.getEntity())) return false;
        health -= amount;
        if (health <= 0.0F) {
            destroyByInterference();
        }
        return true;
    }

    public void destroyByInterference() {
        if (level() instanceof ServerLevel level) {
            level.playSound(null, blockPosition(), ModSounds.VLINDER_MEDICAL_DRONE_DESTROYED.get(),
                    SoundSource.PLAYERS, 0.75F, 1.0F);
        }
        discard();
    }

    private void applyToTarget(LivingEntity owner, ServerPlayer target, ServerLevel level) {
        if (mode == VlinderDroneMode.HEAL && owner instanceof ServerPlayer player) {
            VlinderStateManager.applyHealingDust(player, target);
            level.playSound(null, target.blockPosition(), ModSounds.VLINDER_HEALING_DUST_APPLY.get(),
                    SoundSource.PLAYERS, 0.85F, 1.08F);
        } else {
            VlinderStateManager.applyMedicalWaste(owner, target);
            level.playSound(null, target.blockPosition(), ModSounds.VLINDER_MEDICAL_WASTE_APPLY.get(),
                    SoundSource.PLAYERS, 0.85F, 0.86F);
        }
        level.playSound(null, target.blockPosition(), ModSounds.VLINDER_MEDICAL_DRONE_HIT.get(),
                SoundSource.PLAYERS, 0.75F, 1.0F);
    }

    private LivingEntity owner(ServerLevel level) {
        Entity entity = ownerId == null ? null : level.getEntity(ownerId);
        return entity instanceof LivingEntity living && living.isAlive() ? living : null;
    }

    private ServerPlayer target(ServerLevel level) {
        Entity entity = targetId == null ? null : level.getEntity(targetId);
        return entity instanceof ServerPlayer player && TargetingUtil.isTargetablePlayer(player) ? player : null;
    }

    private void spawnClientParticles() {
        level().addParticle(mode == VlinderDroneMode.HEAL ? HEAL_DUST : WASTE_DUST,
                getX(), getY() + 0.05D, getZ(), 0.0D, 0.0D, 0.0D);
        if (tickCount % 4 == 0) {
            level().addParticle(mode == VlinderDroneMode.HEAL ? ParticleTypes.HAPPY_VILLAGER : ParticleTypes.CRIT,
                    getX(), getY(), getZ(), 0.0D, 0.01D, 0.0D);
        }
    }
}
