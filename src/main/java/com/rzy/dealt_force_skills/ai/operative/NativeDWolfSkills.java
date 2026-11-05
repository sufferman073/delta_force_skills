package com.rzy.dealt_force_skills.ai.operative;

import com.rzy.dealt_force_skills.character.SkillSlot;
import com.rzy.dealt_force_skills.character.dwolf.DWolfStateManager;
import com.rzy.dealt_force_skills.config.DealtForceConfig;
import com.rzy.dealt_force_skills.entity.DWolfHandCannonGrenadeEntity;
import com.rzy.dealt_force_skills.entity.DWolfSmokeCloudEntity;
import com.rzy.dealt_force_skills.entity.DWolfSmokeGrenadeEntity;
import com.rzy.dealt_force_skills.registry.ModEntities;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.stamina.StaminaRules;
import com.rzy.dealt_force_skills.stamina.StaminaSettings;
import com.rzy.dealt_force_skills.util.RangedSoundHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/** Base D-Wolf: a three-round hand cannon, defensive smoke, overload and tactical slide. */
public final class NativeDWolfSkills implements OperativeSkills {
    private final double capacityScale;
    private int cannon, smoke;

    public NativeDWolfSkills() { this(1); }
    NativeDWolfSkills(double capacityScale) {
        this.capacityScale = capacityScale;
        cannon = cannonCapacity(); smoke = smokeCapacity();
    }
    private int cannonCapacity() { return capacity(DWolfStateManager.HAND_CANNON_MAX_CHARGES); }
    private int smokeCapacity() { return capacity(DWolfStateManager.SMOKE_MAX_CHARGES); }
    private int capacity(int base) { return (int) Math.min(Integer.MAX_VALUE, Math.ceil(Math.max(0, base) * capacityScale)); }
    private int cannonRecharge, smokeRecharge, coreCooldown, activeTicks;
    private int healTicks, slideTicks, slideCooldown, staminaDelay;
    private float healing;
    private double stamina = staminaCapacity();
    private SkillSlot prepared;
    private UUID preparedTarget;
    private int windup, burstShots;
    private boolean burstCommitted;
    private OperativeBallistics.Shot detachedShot;
    private Vec3 detachedOrigin;
    private Vec3 slideDirection = Vec3.ZERO;

    @Override
    public void tick(OperativeHost host) { tick(host, true); }

    void tick(OperativeHost host, boolean allowSlide) {
        if (coreCooldown > 0) coreCooldown = Math.max(0, coreCooldown - OperativeGrowth.cooldownStep(host));
        if (slideCooldown > 0) slideCooldown = Math.max(0, slideCooldown - OperativeGrowth.cooldownStep(host));
        cannon = Math.min(cannon, cannonCapacity());
        if (cannon < cannonCapacity() && (cannonRecharge -= OperativeGrowth.cooldownStep(host)) <= 0) {
            cannon++; cannonRecharge = Math.max(1, DWolfStateManager.HAND_CANNON_RECHARGE_TICKS);
        }
        smoke = Math.min(smoke, smokeCapacity());
        if (smoke < smokeCapacity() && (smokeRecharge -= OperativeGrowth.cooldownStep(host)) <= 0) {
            smoke++; smokeRecharge = Math.max(1, DWolfStateManager.SMOKE_RECHARGE_TICKS);
        }
        if (staminaDelay > 0) staminaDelay--;
        else stamina = Math.min(staminaCapacity(), stamina + StaminaRules.recoveryPerTick(0, 0));
        if (healTicks > 0) {
            float portion = healing / healTicks;
            host.body().heal(portion); healing = Math.max(0, healing - portion); healTicks--;
        }
        if (activeTicks > 0 && --activeTicks == 0) {
            coreCooldown = Math.max(1, DWolfStateManager.OVERLOAD_COOLDOWN_TICKS);
            sound(host, ModSounds.D_WOLF_OVERLOAD_END.get());
        }
        updateSpeed(host);
        if (slideTicks > 0) {
            if (host.body().horizontalCollision || !safeStep(host, slideDirection)) stopSlide(host);
            else {
                Vec3 motion = slideDirection.scale(slideSpeed());
                host.body().setDeltaMovement(motion.x, host.body().getDeltaMovement().y, motion.z);
                host.body().hurtMarked = true;
                if (--slideTicks == 0) stopSlide(host);
            }
            return;
        }
        if (prepared == null) {
            if (allowSlide && host.body() instanceof Mob mob && !mob.isNoAi()) trySlide(host, mob.getTarget());
            return;
        }
        LivingEntity target = host.serverLevel().getEntity(preparedTarget) instanceof LivingEntity living ? living : null;
        if (!NativeRaptorSkills.committedTarget(host, target) || !host.parallelNativeAi() && !valid(host, target, prepared == SkillSlot.ACTIVE_1 ? 8 : 2, 24)) {
            clearPreparation(); return;
        }
        host.aimAt(target);
        if (--windup > 0) return;
        if (prepared == SkillSlot.CORE) {
            activeTicks = Math.max(1, DWolfStateManager.OVERLOAD_DURATION_TICKS);
            stamina = Math.min(staminaCapacity(), stamina + staminaCapacity() * 0.5D);
            updateSpeed(host); sound(host, ModSounds.D_WOLF_OVERLOAD_ACTIVE.get()); clearPreparation();
        } else if (prepared == SkillSlot.ACTIVE_1) {
            if (!fireCannon(host, target)) { clearPreparation(); return; }
            if (--burstShots <= 0) clearPreparation();
            else windup = 5;
        } else {
            throwSmoke(host, target); clearPreparation();
        }
    }

    @Override
    public double score(OperativeHost host, SkillSlot slot, LivingEntity target) {
        if (busy() || !valid(host, target, slot == SkillSlot.ACTIVE_1 ? 8 : 2, 24)) return 0;
        return switch (slot) {
            case ACTIVE_1 -> cannon > 0 && safeCannon(host, cannonShot(host, target)) ? 80 : 0;
            case ACTIVE_2 -> smoke > 0 && needsSmoke(host) && smokeShot(host, target) != null ? 95 : 0;
            case CORE -> coreCooldown == 0 && activeTicks == 0 ? 90 : 0;
            case PASSIVE -> 0;
        };
    }

    @Override
    public boolean start(OperativeHost host, SkillSlot slot, LivingEntity target) {
        if (score(host, slot, target) <= 0) return false;
        detachedShot = host.parallelNativeAi() && slot == SkillSlot.ACTIVE_1 ? cannonShot(host, target) : null;
        detachedOrigin = detachedShot != null ? host.body().getEyePosition() : null;
        if (host.parallelNativeAi() && slot == SkillSlot.ACTIVE_2) { throwSmoke(host, target); return true; }
        prepared = slot; preparedTarget = target.getUUID();
        burstShots = slot == SkillSlot.ACTIVE_1 ? 3 : 0; burstCommitted = false;
        windup = slot == SkillSlot.CORE ? Math.max(1, DWolfStateManager.OVERLOAD_STARTUP_TICKS) : 5;
        host.aimAt(target);
        sound(host, slot == SkillSlot.CORE ? ModSounds.D_WOLF_OVERLOAD_START.get()
                : slot == SkillSlot.ACTIVE_1 ? ModSounds.D_WOLF_HAND_CANNON_EQUIP.get() : ModSounds.D_WOLF_SMOKE_EQUIP.get());
        return true;
    }

    private boolean fireCannon(OperativeHost host, LivingEntity target) {
        var shot = detachedShot != null ? detachedShot : cannonShot(host, target);
        if ((!burstCommitted && cannon <= 0) || shot == null || detachedShot == null && !safeCannon(host, shot)) return false;
        var projectile = new DWolfHandCannonGrenadeEntity(ModEntities.D_WOLF_HAND_CANNON_GRENADE.get(), host.serverLevel(), host.body());
        launch(host, projectile, shot.direction(), 2.05D, 0.05D, 0.75D, 0.12D);
        if (detachedOrigin != null) projectile.setPos(detachedOrigin.add(shot.direction().scale(.75)).add(0, -.12, 0));
        if (!host.serverLevel().addFreshEntity(projectile)) return false;
        if (!burstCommitted) {
            if (cannon == cannonCapacity())
                cannonRecharge = Math.max(1, DWolfStateManager.HAND_CANNON_RECHARGE_TICKS);
            cannon--; burstCommitted = true;
        }
        sound(host, ModSounds.D_WOLF_HAND_CANNON_FIRE.get());
        return true;
    }

    private void throwSmoke(OperativeHost host, LivingEntity target) {
        var shot = smokeShot(host, target);
        if (smoke <= 0 || shot == null || !needsSmoke(host)) return;
        var projectile = new DWolfSmokeGrenadeEntity(ModEntities.D_WOLF_SMOKE_GRENADE.get(), host.serverLevel(), host.body());
        launch(host, projectile, shot.direction(), 1.05D, 0.08D, 0.58D, 0.1D);
        if (!host.serverLevel().addFreshEntity(projectile)) return;
        if (smoke == smokeCapacity())
            smokeRecharge = Math.max(1, DWolfStateManager.SMOKE_RECHARGE_TICKS);
        smoke--; sound(host, ModSounds.D_WOLF_SMOKE_THROW.get());
    }

    private static void launch(OperativeHost host, Projectile projectile, Vec3 direction, double speed, double lift, double forward, double down) {
        projectile.setPos(host.body().getEyePosition().add(direction.scale(forward)).add(0, -down, 0));
        projectile.setDeltaMovement(direction.scale(speed).add(0, lift, 0));
    }

    private static boolean valid(OperativeHost host, LivingEntity target, double min, double max) {
        if (target == null || !host.isEnemy(target) || !host.canSee(target)
                || !host.withinCombatRange(target.position()) || !host.withinOrders(host.body().position())) return false;
        double distance = host.body().distanceToSqr(target);
        return distance >= min * min && distance <= max * max;
    }

    private static OperativeBallistics.Shot cannonShot(OperativeHost host, LivingEntity target) {
        var shot = OperativeBallistics.solve(host, target.position().add(0, 0.1D, 0),
                2.05D, 0.05D, 0.75D, 0.12D, 0.045D, 0.985D, 55, 1.5D, true);
        return shot != null && shot.flightTicks() < DWolfHandCannonGrenadeEntity.fuseTicks() ? shot : null;
    }

    private static boolean safeCannon(OperativeHost host, OperativeBallistics.Shot shot) {
        if (shot == null) return false;
        double radius = OperativeGrowth.damageRadius(host.body(), DWolfHandCannonGrenadeEntity.blastRadius(), "summons.dwolfhandcannongrenadeentity") + 1;
        if (host.body().position().distanceToSqr(shot.impact()) <= radius * radius) return false;
        return host.serverLevel().getEntitiesOfClass(LivingEntity.class, new AABB(shot.impact(), shot.impact()).inflate(radius),
                candidate -> candidate != host.body() && candidate.isAlive() && !host.isEnemy(candidate)).isEmpty();
    }

    private static OperativeBallistics.Shot smokeShot(OperativeHost host, LivingEntity target) {
        Vec3 direction = target.position().subtract(host.body().position()).normalize();
        Vec3 screen = host.body().position().add(direction.scale(Math.min(4, host.body().distanceTo(target) * 0.5D)));
        return OperativeBallistics.solve(host, screen.add(0, 0.1D, 0), 1.05D, 0.08D, 0.58D, 0.1D, 0.045D, 0.985D, 60, 1, true);
    }

    private static boolean needsSmoke(OperativeHost host) {
        if (!host.serverLevel().getEntitiesOfClass(DWolfSmokeCloudEntity.class, host.body().getBoundingBox().inflate(5)).isEmpty()) return false;
        return host.body().getHealth() <= host.body().getMaxHealth() * 0.6F
                || host.serverLevel().getEntitiesOfClass(Mob.class, host.body().getBoundingBox().inflate(16),
                    mob -> host.isEnemy(mob) && mob.getTarget() == host.body()).size() >= 2;
    }

    /** Native mobs keep their own base stamina; no player training/commander bonuses are granted. */
    public boolean trySlide(OperativeHost host, LivingEntity target) {
        if (busy() || slideCooldown > 0 || !valid(host, target, 5, 14) || !host.body().onGround()
                || host.body().getDeltaMovement().horizontalDistanceSqr() < 0.001D
                || !OperativeGrowth.commander(host.body()) && !StaminaRules.canSpend(stamina, StaminaSettings.slideCost(), 0, false)) return false;
        Vec3 direction = OperativeTactics.displacement(host, target, slideSpeed() * Math.max(1, DWolfStateManager.SLIDE_TICKS));
        if (!safeStep(host, direction)) return false;
        if (!OperativeGrowth.commander(host.body())) {
            stamina = Math.max(0, stamina - StaminaSettings.slideCost()); staminaDelay = StaminaRules.RECOVERY_DELAY;
        }
        slideDirection = direction; slideTicks = Math.max(1, DWolfStateManager.SLIDE_TICKS);
        slideCooldown = Math.max(1, DWolfStateManager.SLIDE_COOLDOWN_TICKS);
        host.body().setPose(Pose.SWIMMING); host.body().refreshDimensions();
        return true;
    }

    private static double slideSpeed() { return Math.max(0, DealtForceConfig.doubleValue("characters.dwolf.d_wolf_state_manager.slide_speed", 0.9D)); }
    private static double staminaCapacity() { return Math.max(1, DealtForceConfig.doubleValue("stamina.base_capacity", StaminaRules.BASE_CAPACITY)); }

    private static boolean safeStep(OperativeHost host, Vec3 direction) {
        Vec3 next = host.body().position().add(direction.scale(slideSpeed()));
        AABB origin = host.body().getDimensions(Pose.SWIMMING).makeBoundingBox(host.body().position());
        AABB destination = origin.move(next.subtract(host.body().position()));
        return host.body().onGround() && host.withinOrders(next)
                && host.body().level().noCollision(host.body(), origin.expandTowards(next.subtract(host.body().position())).deflate(0.0001D))
                && !host.body().level().noCollision(host.body(), destination.move(0, -1, 0));
    }

    private void stopSlide(OperativeHost host) {
        slideTicks = 0; slideDirection = Vec3.ZERO;
        host.body().setDeltaMovement(0, host.body().getDeltaMovement().y, 0);
        if (host.body() instanceof com.rzy.dealt_force_skills.entity.OperativeEntity operative) operative.tryStanding();
        else if (host.body().level().noCollision(host.body(), host.body().getDimensions(Pose.STANDING).makeBoundingBox(host.body().position()))) {
            host.body().setPose(Pose.STANDING); host.body().refreshDimensions();
        }
    }

    private void updateSpeed(OperativeHost host) {
        var speed = host.body().getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) return;
        if (activeTicks > 0 && speed.getModifier(DWolfStateManager.OVERLOAD_SPEED_UUID) == null)
            speed.addTransientModifier(new AttributeModifier(DWolfStateManager.OVERLOAD_SPEED_UUID,
                    "operative_dwolf_overload", 0.5D, AttributeModifier.Operation.MULTIPLY_TOTAL));
        else if (activeTicks == 0) speed.removeModifier(DWolfStateManager.OVERLOAD_SPEED_UUID);
    }

    public void onKill(OperativeHost host) {
        if (activeTicks <= 0) return;
        int reward = OperativeGrowth.commander(host.body()) ? 2 : 1;
        activeTicks = (int) Math.min(Integer.MAX_VALUE, (long) activeTicks + OperativeGrowth.duration(host.body(), Math.max(0, DWolfStateManager.OVERLOAD_EXTENSION_TICKS) * reward));
        healing = host.body().getMaxHealth() * Math.max(0, DWolfStateManager.OVERLOAD_KILL_HEAL_FRACTION) * reward;
        healTicks = Math.max(1, DWolfStateManager.OVERLOAD_REGEN_TICKS);
        stamina = Math.min(staminaCapacity(), stamina + staminaCapacity() * 0.5D * reward);
        sound(host, ModSounds.D_WOLF_OVERLOAD_KILL_EXTENSION.get());
    }

    private void clearPreparation() { prepared = null; preparedTarget = null; windup = burstShots = 0; burstCommitted = false; detachedShot = null; detachedOrigin = null; }
    @Override public boolean locksMovement() { return slideTicks > 0; }
    @Override public boolean busy() { return prepared != null || slideTicks > 0; }
    @Override public void cancel(OperativeHost host) {
        clearPreparation();
        if (slideTicks > 0) stopSlide(host);
        if (activeTicks > 0) coreCooldown = Math.max(1, DWolfStateManager.OVERLOAD_COOLDOWN_TICKS);
        activeTicks = healTicks = 0; healing = 0; updateSpeed(host);
    }
    @Override public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("CannonCharges", cannon); tag.putInt("CannonRecharge", cannonRecharge);
        tag.putInt("SmokeCharges", smoke); tag.putInt("SmokeRecharge", smokeRecharge);
        tag.putInt("CoreCooldown", coreCooldown); tag.putInt("ActiveTicks", activeTicks);
        tag.putInt("HealTicks", healTicks); tag.putFloat("Healing", healing);
        tag.putInt("SlideCooldown", slideCooldown); tag.putDouble("Stamina", stamina); tag.putInt("StaminaDelay", staminaDelay);
        return tag;
    }
    @Override public void load(CompoundTag tag) {
        cannon = bounded(tag, "CannonCharges", cannonCapacity());
        smoke = bounded(tag, "SmokeCharges", smokeCapacity());
        cannonRecharge = bounded(tag, "CannonRecharge", DWolfStateManager.HAND_CANNON_RECHARGE_TICKS);
        smokeRecharge = bounded(tag, "SmokeRecharge", DWolfStateManager.SMOKE_RECHARGE_TICKS);
        coreCooldown = bounded(tag, "CoreCooldown", DWolfStateManager.OVERLOAD_COOLDOWN_TICKS);
        activeTicks = bounded(tag, "ActiveTicks", Integer.MAX_VALUE);
        healTicks = bounded(tag, "HealTicks", DWolfStateManager.OVERLOAD_REGEN_TICKS);
        healing = Float.isFinite(tag.getFloat("Healing")) ? Math.max(0, tag.getFloat("Healing")) : 0;
        slideCooldown = bounded(tag, "SlideCooldown", DWolfStateManager.SLIDE_COOLDOWN_TICKS);
        stamina = Double.isFinite(tag.getDouble("Stamina")) ? Math.max(0, Math.min(staminaCapacity(), tag.getDouble("Stamina"))) : 0;
        staminaDelay = bounded(tag, "StaminaDelay", StaminaRules.RECOVERY_DELAY);
        clearPreparation(); slideTicks = 0; slideDirection = Vec3.ZERO;
    }
    private static int bounded(CompoundTag tag, String key, int max) { return Math.max(0, Math.min(Math.max(0, max), tag.getInt(key))); }
    private static void sound(OperativeHost host, net.minecraft.sounds.SoundEvent sound) {
        RangedSoundHelper.playThrottled(host.serverLevel(), host.body().position(), sound, SoundSource.NEUTRAL, 0.9F, 1, 24, 4, 3);
    }
    @Override public String describe() { return "cannon=" + cannon + ", smoke=" + smoke + ", core=" + coreCooldown
            + ", active=" + activeTicks + ", slide=" + slideTicks + ", stamina=" + stamina + ", preparing=" + prepared; }

    @Override public void refresh(OperativeHost host) {
        var fresh = new NativeDWolfSkills(capacityScale);
        cannon = fresh.cannon;
        cannonRecharge = fresh.cannonRecharge;
        coreCooldown = fresh.coreCooldown;
        slideCooldown = fresh.slideCooldown;
        smoke = fresh.smoke;
        smokeRecharge = fresh.smokeRecharge;
    }
    @Override public com.rzy.dealt_force_skills.motion.TacticalMotion motion() {
        return slideTicks > 0 ? com.rzy.dealt_force_skills.motion.TacticalMotion.SLIDE : com.rzy.dealt_force_skills.motion.TacticalMotion.NONE;
    }
    @Override public int motionDuration() { return Math.max(1, DWolfStateManager.SLIDE_TICKS); }
    @Override public Vec3 motionDirection() { return slideDirection; }

}
