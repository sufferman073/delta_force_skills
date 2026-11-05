package com.rzy.dealt_force_skills.entity;

import com.rzy.dealt_force_skills.character.vlinder.VlinderStateManager;
import com.rzy.dealt_force_skills.character.commander.CommanderBranchUpgrade;
import com.rzy.dealt_force_skills.character.commander.CommanderStateManager;
import com.rzy.dealt_force_skills.compat.PlayerReviveCompat;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import org.joml.Vector3f;

import java.util.Comparator;
import java.util.UUID;
import com.rzy.dealt_force_skills.ai.operative.*;
import com.rzy.dealt_force_skills.character.SkillSlot;

public class VlinderActiveDefenseDroneEntity extends Entity implements ItemSupplier {
    public static volatile int LIFE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("LIFE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.vlinderactivedefensedroneentity.life_ticks", 700));
    public static volatile int INJECTION_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("INJECTION_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.vlinderactivedefensedroneentity.injection_ticks", 120));
    private static volatile int ABSORB_INTERVAL_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("ABSORB_INTERVAL_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.vlinderactivedefensedroneentity.absorb_interval_ticks", 100));
    private static volatile double INJECTION_RANGE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("INJECTION_RANGE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.vlinderactivedefensedroneentity.injection_range", 20.0));
    private static volatile double FOLLOW_SPEED = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("FOLLOW_SPEED", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.vlinderactivedefensedroneentity.follow_speed", 0.32));
    private static final DustParticleOptions DRONE_DUST = new DustParticleOptions(new Vector3f(1.0f, 0.78f, 0.2f), 1.0f);


    private final OperativeAllegiance allegiance = new OperativeAllegiance();
    private int missingTicks;
    private UUID injectionTargetUUID;
    private UUID ownerId;
    private int usedTicks;
    private float health = 20.0F;
    private int injectionTargetId = -1;
    private int injectionTicks;
    private double injectionX;
    private double injectionY;
    private double injectionZ;

    public VlinderActiveDefenseDroneEntity(EntityType<? extends VlinderActiveDefenseDroneEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public VlinderActiveDefenseDroneEntity(EntityType<? extends VlinderActiveDefenseDroneEntity> type, Level level, ServerPlayer owner) {
        this(type, level);
        ownerId = owner.getUUID();
    }


    public VlinderActiveDefenseDroneEntity(EntityType<? extends VlinderActiveDefenseDroneEntity> type, Level level, LivingEntity owner) {
        this(type, level); ownerId = owner.getUUID(); allegiance.capture(owner);
    }
    public boolean isNativeOwnedBy(LivingEntity owner) { return allegiance.active() && owner.getUUID().equals(ownerId); }
    private void tickNative(ServerLevel level) {
        if (usedTicks++ >= LIFE_TICKS || health <= 0) { discard(); return; }
        Entity found = ownerId == null ? null : level.getEntity(ownerId);
        if (!(found instanceof LivingEntity owner)) { if (++missingTicks > 40) discard(); return; }
        missingTicks = 0;
        if (!NativeVlinderSkills.deviceAvailable(owner, SkillSlot.CORE, position())
                || !(owner instanceof com.rzy.dealt_force_skills.ai.operative.OperativeHost bot) || !(com.rzy.dealt_force_skills.ai.operative.NativeCommanderSkills.inherited(bot.skills(), NativeVlinderSkills.class) instanceof NativeVlinderSkills skills)
                || !skills.ownsDefense(getUUID())) { discard(); return; }
        Vec3 desired = owner.getEyePosition().add(owner.getLookAngle().scale(-0.55)).add(0, 0.85, 0);
        Vec3 delta = desired.subtract(position());
        Vec3 motion = delta.lengthSqr() < 0.04 ? Vec3.ZERO : delta.normalize().scale(Math.min(Math.max(0.01, FOLLOW_SPEED), delta.length()));
        setDeltaMovement(motion); move(MoverType.SELF, motion);
        if (usedTicks == 1 || usedTicks % Math.max(1, ABSORB_INTERVAL_TICKS) == 0) {
            owner.setAbsorptionAmount(Math.max(owner.getAbsorptionAmount(), 40));
            owner.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, Math.max(1, ABSORB_INTERVAL_TICKS) + 20,
                    com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.vlinder_active_defense_drone_entity.effect.absorption.0.amplifier", 9), false, true, true), owner);
            if (bot instanceof OperativeEntity operative) operative.medical().protectVlinder(Math.max(1, ABSORB_INTERVAL_TICKS) + 20);
            level.playSound(null, owner.blockPosition(), ModSounds.VLINDER_ABSORPTION_REFRESH.get(), SoundSource.PLAYERS, 0.7F, 1);
        }
        LivingEntity target = injectionTargetUUID != null && level.getEntity(injectionTargetUUID) instanceof LivingEntity living ? living : null;
        if (!nativeInjectable(bot, target)) {
            resetInjection(); injectionTargetUUID = null;
            target = level.getEntitiesOfClass(LivingEntity.class, owner.getBoundingBox().inflate(INJECTION_RANGE),
                    candidate -> nativeInjectable(bot, candidate)).stream()
                    .min(Comparator.comparingDouble(owner::distanceToSqr)).orElse(null);
            if (target == null) return;
            injectionTargetUUID = target.getUUID();
            level.playSound(null, target.blockPosition(), ModSounds.VLINDER_PLASMA_INJECTION_START.get(), SoundSource.PLAYERS, 0.7F, 1);
        }
        injectionTargetId = target.getId();
        injectionX = target.getX(); injectionY = target.getY(); injectionZ = target.getZ();
        if (++injectionTicks >= Math.max(1, INJECTION_TICKS)) {
            int duration = Math.max(20, VlinderStateManager.DOWNED_DURATION_TICKS);
            target.addEffect(new MobEffectInstance(com.rzy.dealt_force_skills.registry.ModEffects.VLINDER_PLASMA_INJECTED.get(), duration,
                    com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.vlinder.vlinder_state_manager.effect.vlinder_plasma_injected.6.amplifier", 0), false, true, true), owner);
            level.playSound(null, target.blockPosition(), ModSounds.VLINDER_PLASMA_INJECTION_COMPLETE.get(), SoundSource.PLAYERS, 0.85F, 1);
            resetInjection(); injectionTargetUUID = null;
        }
    }
    private boolean nativeInjectable(com.rzy.dealt_force_skills.ai.operative.OperativeHost owner, LivingEntity target) {
        return target != null && target != owner.body() && target.isAlive() && target.level() == level()
                && OperativeSupport.ally(owner.body(), target) && OperativeSupport.downed(target)
                && !target.hasEffect(com.rzy.dealt_force_skills.registry.ModEffects.VLINDER_PLASMA_INJECTED.get())
                && owner.body().distanceToSqr(target) <= INJECTION_RANGE * INJECTION_RANGE
                && owner.withinOrders(target.position()) && owner.body().hasLineOfSight(target);
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(Items.GOLDEN_APPLE);
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
        ServerPlayer owner = owner(level);
        if (owner == null || usedTicks >= LIFE_TICKS) {
            if(owner!=null && owner.isAlive() && usedTicks>=LIFE_TICKS && getPersistentData().getBoolean("DepartmentSurvivedDamage"))
                com.rzy.dealt_force_skills.department.DepartmentTasks.signal(owner,"vlinder.protection.survived_damage");
            discard();
            return;
        }
        usedTicks++;
        followOwner(owner);
        if (usedTicks == 1 || usedTicks % ABSORB_INTERVAL_TICKS == 0) {
            refreshAbsorption(owner, level);
        }
        tickInjection(owner, level);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        ownerId = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        int nativeAge = allegiance.load(tag, LIFE_TICKS);
        if (allegiance.active()) tickCount = nativeAge;
        missingTicks = 0;
        usedTicks = tag.getInt("UsedTicks");
        if (allegiance.active()) usedTicks = Math.max(0, Math.min(Math.max(1, LIFE_TICKS) + 1, usedTicks));
        health = tag.contains("Health") ? tag.getFloat("Health") : 20.0F;
        if (allegiance.active()) health = Float.isFinite(health) ? Math.max(0, Math.min(20, health)) : 0;
        injectionTargetId = tag.contains("InjectionTargetId") ? tag.getInt("InjectionTargetId") : -1;
        injectionTicks = tag.getInt("InjectionTicks");
        injectionTargetUUID = tag.hasUUID("NativeInjectionTarget") ? tag.getUUID("NativeInjectionTarget") : null;
        if (allegiance.active()) {
            injectionTicks = Math.max(0, Math.min(Math.max(1, INJECTION_TICKS), injectionTicks));
            injectionTargetId = -1;
        }
        injectionX = tag.getDouble("InjectionX");
        injectionY = tag.getDouble("InjectionY");
        injectionZ = tag.getDouble("InjectionZ");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        allegiance.save(tag, tickCount);
        if (ownerId != null) {
            tag.putUUID("Owner", ownerId);
        }
        tag.putInt("UsedTicks", usedTicks);
        tag.putFloat("Health", health);
        tag.putInt("InjectionTargetId", injectionTargetId);
        tag.putInt("InjectionTicks", injectionTicks);
        if (injectionTargetUUID != null) tag.putUUID("NativeInjectionTarget", injectionTargetUUID);
        tag.putDouble("InjectionX", injectionX);
        tag.putDouble("InjectionY", injectionY);
        tag.putDouble("InjectionZ", injectionZ);
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

    public static VlinderActiveDefenseDroneEntity activeFor(ServerPlayer owner) {
        if (!(owner.level() instanceof ServerLevel level)) {
            return null;
        }
        UUID ownerUuid = owner.getUUID();
        AABB search = owner.getBoundingBox().inflate(160.0D);
        return level.getEntitiesOfClass(VlinderActiveDefenseDroneEntity.class, search,
                        drone -> drone.isAlive() && ownerUuid.equals(drone.ownerId))
                .stream()
                .min(Comparator.comparingDouble(owner::distanceToSqr))
                .orElse(null);
    }

    public static void discardFor(ServerPlayer owner) {
        VlinderActiveDefenseDroneEntity drone = activeFor(owner);
        if (drone != null) {
            drone.discard();
        }
    }

    public void destroyByInterference() {
        if (level() instanceof ServerLevel level) {
            level.playSound(null, blockPosition(), ModSounds.VLINDER_ACTIVE_DEFENSE_DESTROYED.get(),
                    SoundSource.PLAYERS, 0.85F, 1.0F);
        }
        discard();
    }

    public int remainingTicks() {
        return Math.max(0, LIFE_TICKS - usedTicks);
    }

    public int injectionTargetId() {
        return injectionTargetId;
    }

    public int injectionTicks() {
        return injectionTicks;
    }

    public int injectionRequiredTicks() {
        if (level() instanceof ServerLevel serverLevel) {
            LivingEntity owner = ownerId != null && serverLevel.getEntity(ownerId) instanceof LivingEntity living ? living : null;
            if (owner != null && com.rzy.dealt_force_skills.ai.operative.OperativeGrowth.branch(
                    owner, CommanderBranchUpgrade.VLINDER_EXTREME_DEFENSE)) {
                return Math.max(1, INJECTION_TICKS / 2);
            }
        }
        return INJECTION_TICKS;
    }

    public Vec3 injectionPosition() {
        return new Vec3(injectionX, injectionY, injectionZ);
    }

    private void followOwner(ServerPlayer owner) {
        Vec3 look = owner.getLookAngle().normalize();
        Vec3 desired = owner.getEyePosition().add(look.scale(-0.55D)).add(0.0D, 0.85D, 0.0D);
        Vec3 delta = desired.subtract(position());
        Vec3 motion = delta.lengthSqr() < 0.04D ? Vec3.ZERO : delta.normalize().scale(Math.min(FOLLOW_SPEED, delta.length()));
        setDeltaMovement(motion);
        move(MoverType.SELF, motion);
    }

    private void refreshAbsorption(ServerPlayer owner, ServerLevel level) {
        owner.setAbsorptionAmount(Math.max(owner.getAbsorptionAmount(), 40.0F));
        owner.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, ABSORB_INTERVAL_TICKS + 20, com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.vlinder_active_defense_drone_entity.effect.absorption.0.amplifier", 9), false, true, true), owner);
        VlinderStateManager.markVlinderSuppliedBuff(owner, ABSORB_INTERVAL_TICKS + 20, owner);
        level.playSound(null, owner.blockPosition(), ModSounds.VLINDER_ABSORPTION_REFRESH.get(),
                SoundSource.PLAYERS, 0.7F, 1.0F);
    }

    private void tickInjection(ServerPlayer owner, ServerLevel level) {
        ServerPlayer target = currentInjectionTarget(owner);
        if (target == null) {
            target = nearestDowned(owner, level);
            if (target == null) {
                resetInjection();
                return;
            }
            injectionTargetId = target.getId();
            injectionTicks = 0;
            level.playSound(null, target.blockPosition(), ModSounds.VLINDER_DOWNED_PLAYER_FOUND.get(),
                    SoundSource.PLAYERS, 0.75F, 1.0F);
            level.playSound(null, blockPosition(), ModSounds.VLINDER_PLASMA_INJECTION_START.get(),
                    SoundSource.PLAYERS, 0.7F, 1.0F);
        }

        injectionX = target.getX();
        injectionY = target.getY();
        injectionZ = target.getZ();
        injectionTicks++;
        if (injectionTicks >= injectionRequiredTicks()) {
            VlinderStateManager.markPlasmaInjected(owner, target);
            level.playSound(null, target.blockPosition(), ModSounds.VLINDER_PLASMA_INJECTION_COMPLETE.get(),
                    SoundSource.PLAYERS, 0.85F, 1.0F);
            resetInjection();
        }
    }

    private ServerPlayer currentInjectionTarget(ServerPlayer owner) {
        if (injectionTargetId < 0) {
            return null;
        }
        for (ServerPlayer target : owner.server.getPlayerList().getPlayers()) {
            if (target.getId() == injectionTargetId
                    && target.level() == owner.level()
                    && DealtTeamManager.areTeammates(owner, target)
                    && TargetingUtil.isTargetablePlayer(target)
                    && isInjectableDowned(target)
                    && !target.hasEffect(com.rzy.dealt_force_skills.registry.ModEffects.VLINDER_PLASMA_INJECTED.get())
                    && target.distanceToSqr(owner) <= INJECTION_RANGE * INJECTION_RANGE) {
                return target;
            }
        }
        return null;
    }

    private ServerPlayer nearestDowned(ServerPlayer owner, ServerLevel level) {
        return level.getEntitiesOfClass(ServerPlayer.class, owner.getBoundingBox().inflate(INJECTION_RANGE),
                        target -> target != owner
                                && DealtTeamManager.areTeammates(owner, target)
                                && TargetingUtil.isTargetablePlayer(target)
                                && isInjectableDowned(target)
                                && !target.hasEffect(com.rzy.dealt_force_skills.registry.ModEffects.VLINDER_PLASMA_INJECTED.get())
                                && target.distanceToSqr(owner) <= INJECTION_RANGE * INJECTION_RANGE)
                .stream()
                .min(Comparator.comparingDouble(owner::distanceToSqr))
                .orElse(null);
    }

    private static boolean isInjectableDowned(ServerPlayer target) {
        return VlinderStateManager.isDowned(target) || PlayerReviveCompat.isBleeding(target);
    }

    private void resetInjection() {
        injectionTargetId = -1;
        injectionTicks = 0;
        injectionX = getX();
        injectionY = getY();
        injectionZ = getZ();
    }

    private ServerPlayer owner(ServerLevel level) {
        Entity entity = ownerId == null ? null : level.getEntity(ownerId);
        return entity instanceof ServerPlayer player && player.isAlive() ? player : null;
    }

    private void spawnClientParticles() {
        level().addParticle(DRONE_DUST, getX(), getY(), getZ(), 0.0D, 0.0D, 0.0D);
        if (injectionTargetId >= 0 && tickCount % 3 == 0) {
            level().addParticle(ParticleTypes.HAPPY_VILLAGER, getX(), getY(), getZ(), 0.0D, 0.02D, 0.0D);
        }
    }
}
