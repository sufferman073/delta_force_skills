package com.rzy.dealt_force_skills.ai.operative;

import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.character.SkillSlot;
import com.rzy.dealt_force_skills.character.uluru.UluruStateManager;
import com.rzy.dealt_force_skills.entity.UluruBombletEntity;
import com.rzy.dealt_force_skills.entity.UluruFireFieldEntity;
import com.rzy.dealt_force_skills.entity.UluruIncendiaryGrenadeEntity;
import com.rzy.dealt_force_skills.entity.UluruLoiteringMissileEntity;
import com.rzy.dealt_force_skills.entity.UluruQuickCoverPackageEntity;
import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.registry.ModEntities;
import com.rzy.dealt_force_skills.registry.ModSounds;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/** Native base Uluru: floor fire, defensive cover and a directly launched cluster missile. */
public final class NativeUluruSkills implements OperativeSkills {
    private final double capacityScale;
    private int incendiaries, covers;

    public NativeUluruSkills() { this(1); }
    NativeUluruSkills(double capacityScale) {
        this.capacityScale = Math.max(1.0D, capacityScale);
        incendiaries = incendiaryCapacity();
        covers = coverCapacity();
    }
    private int capacity(int base) {
        return (int) Math.min(Integer.MAX_VALUE, Math.ceil(Math.max(0, base) * capacityScale));
    }
    private int incendiaryCapacity() { return capacity(UluruStateManager.INCENDIARY_MAX_CHARGES); }
    private int coverCapacity() { return capacity(UluruStateManager.COVER_MAX_CHARGES); }
    private int incendiaryRecharge, coverRecharge, missileCooldown, coverDelay;
    private record Tracked(int duration, int amplifier) { }
    private final Map<String, Tracked> effects = new HashMap<>();

    @Override public void tick(OperativeHost host) { tick(host, true); }

    void tick(OperativeHost host, boolean passive) {
        if (missileCooldown > 0) missileCooldown = Math.max(0, missileCooldown - OperativeGrowth.cooldownStep(host));
        if (coverDelay > 0) coverDelay--;
        int incendiaryCapacity = incendiaryCapacity();
        int coverCapacity = coverCapacity();
        incendiaries = Math.min(incendiaries, incendiaryCapacity);
        covers = Math.min(covers, coverCapacity);
        if (incendiaries < incendiaryCapacity && (incendiaryRecharge -= OperativeGrowth.cooldownStep(host)) <= 0) {
            incendiaries++; incendiaryRecharge = Math.max(1, UluruStateManager.INCENDIARY_RECHARGE_TICKS);
        }
        if (covers < coverCapacity && (coverRecharge -= OperativeGrowth.cooldownStep(host)) <= 0) {
            covers++; coverRecharge = Math.max(1, UluruStateManager.COVER_RECHARGE_TICKS);
        }
        if (!passive) return;
        var active = new java.util.HashSet<String>();
        int grace = com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("events.common_events.effect_reapply_grace_ticks", 2);
        for (var effect : new ArrayList<>(host.body().getActiveEffects())) {
            var type = effect.getEffect();
            if (type.value().getCategory() != MobEffectCategory.HARMFUL || effect.isInfiniteDuration()) continue;
            String key = net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.getKey(type.value()).toString();
            active.add(key);
            var previous = effects.get(key);
            int duration = effect.getDuration();
            if (previous == null || previous.amplifier() != effect.getAmplifier() || duration > previous.duration() + grace) {
                duration = Math.max(1, duration / 2);
                ((com.rzy.dealt_force_skills.mixin.OperativeEffectAccessor)(Object)effect)
                        .dealt_force_skills$setDuration(duration);
                host.body().forceAddEffect(effect, null);
            }
            effects.put(key, new Tracked(duration, effect.getAmplifier()));
        }
        effects.keySet().retainAll(active);
    }

    @Override public double score(OperativeHost host, SkillSlot slot, LivingEntity target) {
        if (!host.body().isAlive() || host.body().isRemoved() || !host.withinOrders(host.body().position())
                || host.body().hasEffect(ModEffects.STUN) || host.body().hasEffect(ModEffects.WEBBED)
                || target == null || !host.isEnemy(target) || !host.withinCombatRange(target.position())
                || !host.canSee(target) || host.body().distanceToSqr(target) > (slot == SkillSlot.CORE ? 80 * 80 : 32 * 32)
                || OperativeElectronics.blocked(host, ModCharacters.ULURU_ID, slot)) return 0;
        return switch (slot) {
            case ACTIVE_1 -> incendiaries > 0 && safe(host, fireShot(host, target), UluruFireFieldEntity.nativeRadius() + 1) ? 80 : 0;
            case ACTIVE_2 -> covers > 0 && coverDelay == 0 && host.body().getHealth() < host.body().getMaxHealth() * 0.65F
                    && coverShot(host, target) != null ? 100 : 0;
            case CORE -> missileCooldown == 0 && safeMissile(host, UluruLoiteringMissileEntity.nativeShot(host, target)) ? 90 : 0;
            default -> 0;
        };
    }

    @Override public boolean start(OperativeHost host, SkillSlot slot, LivingEntity target) {
        if (score(host, slot, target) <= 0) return false;
        host.aimAt(target);
        Projectile projectile;
        OperativeBallistics.Shot shot;
        double speed, lift, forward;
        net.minecraft.sounds.SoundEvent sound;
        if (slot == SkillSlot.ACTIVE_1) {
            shot = fireShot(host, target);
            if (!safe(host, shot, UluruFireFieldEntity.nativeRadius() + 1)) return false;
            projectile = new UluruIncendiaryGrenadeEntity(ModEntities.ULURU_INCENDIARY_GRENADE.get(), host.serverLevel(), host.body());
            speed = 2.25D; lift = 0.14D; forward = 0.55D; sound = ModSounds.INCENDIARY_THROW.get();
        } else if (slot == SkillSlot.ACTIVE_2) {
            shot = coverShot(host, target);
            if (shot == null) return false;
            Direction facing = Direction.getNearest(target.getX() - host.body().getX(), 0, target.getZ() - host.body().getZ());
            projectile = new UluruQuickCoverPackageEntity(ModEntities.ULURU_QUICK_COVER_PACKAGE.get(), host.serverLevel(), host.body(), facing);
            speed = 1.35D; lift = 0.04D; forward = 0.55D; sound = ModSounds.QUICK_COVER_THROW.get();
        } else {
            shot = UluruLoiteringMissileEntity.nativeShot(host, target);
            if (!safeMissile(host, shot)) return false;
            projectile = new UluruLoiteringMissileEntity(ModEntities.ULURU_LOITERING_MISSILE.get(), host.serverLevel(), host.body(), false);
            speed = 0; lift = 0; forward = 0.9D; sound = ModSounds.MISSILE_FIRE.get();
        }
        Vec3 direction = shot.direction();
        projectile.setPos(host.body().getEyePosition().add(direction.scale(forward)).add(0, -0.1D, 0));
        projectile.setDeltaMovement(direction.scale(speed).add(0, lift, 0));
        projectile.setYRot((float) (Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90));
        projectile.setXRot((float) -Math.toDegrees(Math.atan2(direction.y, direction.horizontalDistance())));
        if (!host.serverLevel().addFreshEntity(projectile)) return false;
        if (slot == SkillSlot.ACTIVE_1) {
            if (incendiaries == incendiaryCapacity()) incendiaryRecharge = Math.max(1, UluruStateManager.INCENDIARY_RECHARGE_TICKS);
            incendiaries--;
        } else if (slot == SkillSlot.ACTIVE_2) {
            if (covers == coverCapacity()) coverRecharge = Math.max(1, UluruStateManager.COVER_RECHARGE_TICKS);
            covers--; coverDelay = 800;
        } else missileCooldown = Math.max(1, UluruStateManager.MISSILE_COOLDOWN_TICKS);
        host.serverLevel().playSound(null, host.body().blockPosition(), sound, SoundSource.NEUTRAL, 1, 1);
        return true;
    }

    private static OperativeBallistics.Shot fireShot(OperativeHost host, LivingEntity target) {
        return OperativeBallistics.solve(host, target.position(), 2.25D, 0.14D, 0.55D, 0.1D, 0.04D, 0.98D,
                80, Math.max(0.1D, UluruFireFieldEntity.nativeRadius() * 0.5D), true);
    }
    private static OperativeBallistics.Shot coverShot(OperativeHost host, LivingEntity target) {
        Vec3 offset = target.position().subtract(host.body().position()).multiply(1, 0, 1);
        if (offset.lengthSqr() < 36) return null;
        Vec3 destination = host.body().position().add(offset.normalize().scale(3));
        if (!host.withinOrders(destination)) return null;
        return OperativeBallistics.solve(host, destination, 1.35D, 0.04D, 0.55D, 0.1D, 0.035D, 0.985D, 60, 0.8D, true);
    }
    private static boolean safe(OperativeHost host, OperativeBallistics.Shot shot, double radius) {
        if (shot == null || !host.withinOrders(shot.impact()) || host.body().position().distanceToSqr(shot.impact()) <= radius * radius) return false;
        return host.serverLevel().getEntitiesOfClass(LivingEntity.class, new AABB(shot.impact(), shot.impact()).inflate(radius),
                e -> e != host.body() && e.isAlive() && !host.isEnemy(e)).isEmpty();
    }
    private static boolean safeMissile(OperativeHost host, OperativeBallistics.Shot shot) {
        if (!safe(host, shot, UluruLoiteringMissileEntity.nativeRadius(host.body()) + 1)) return false;
        // Predict each of the six real bomblets, including its ground landing and fuse.
        for (int i = 0; i < 6; i++) {
            double angle = Math.PI * 2 * i / 6;
            Vec3 motion = new Vec3(Math.cos(angle), 0.18D, Math.sin(angle)).normalize().scale(0.72D);
            Vec3 position = shot.impact().add(0, 0.2D, 0);
            for (int tick = 1; tick <= UluruBombletEntity.nativeFlightLimit(); tick++) {
                Vec3 next = position.add(motion);
                var hit = host.serverLevel().clip(new ClipContext(position, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, host.body()));
                if (hit.getType() == HitResult.Type.BLOCK) { position = hit.getLocation().subtract(motion.normalize().scale(0.05D)); break; }
                position = next;
                if (tick >= UluruBombletEntity.nativeArmingTicks()
                        && !safe(host, new OperativeBallistics.Shot(shot.direction(), position, tick), UluruBombletEntity.nativeRadius(host.body()) + motion.length() + 1)) return false;
                motion = motion.add(0, -0.035D, 0).scale(0.97D);
            }
            if (!safe(host, new OperativeBallistics.Shot(shot.direction(), position, 1), UluruBombletEntity.nativeRadius(host.body()) + 1)) return false;
        }
        return true;
    }

    @Override public boolean busy() { return false; }
    @Override public void cancel(OperativeHost host) { }
    @Override public CompoundTag save() {
        var tag = new CompoundTag();
        tag.putInt("IncendiaryCharges", incendiaries); tag.putInt("CoverCharges", covers);
        tag.putInt("IncendiaryRecharge", incendiaryRecharge); tag.putInt("CoverRecharge", coverRecharge);
        tag.putInt("MissileCooldown", missileCooldown); tag.putInt("CoverDelay", coverDelay);
        var tracked = new CompoundTag();
        effects.forEach((key, value) -> {
            var entry = new CompoundTag(); entry.putInt("Duration", value.duration()); entry.putInt("Amplifier", value.amplifier());
            tracked.put(key, entry);
        });
        tag.put("TrackedEffects", tracked);
        return tag;
    }
    @Override public void load(CompoundTag tag) {
        incendiaries = bound(tag, "IncendiaryCharges", incendiaryCapacity());
        covers = bound(tag, "CoverCharges", coverCapacity());
        incendiaryRecharge = bound(tag, "IncendiaryRecharge", UluruStateManager.INCENDIARY_RECHARGE_TICKS);
        coverRecharge = bound(tag, "CoverRecharge", UluruStateManager.COVER_RECHARGE_TICKS);
        missileCooldown = bound(tag, "MissileCooldown", UluruStateManager.MISSILE_COOLDOWN_TICKS);
        coverDelay = bound(tag, "CoverDelay", 800); effects.clear();
        var tracked = tag.getCompound("TrackedEffects");
        for (String key : tracked.getAllKeys()) {
            var entry = tracked.getCompound(key);
            effects.put(key, new Tracked(Math.max(0, entry.getInt("Duration")), Math.max(0, entry.getInt("Amplifier"))));
        }
    }
    private static int bound(CompoundTag tag, String key, int max) { return Math.max(0, Math.min(Math.max(0, max), tag.getInt(key))); }
    @Override public String describe() { return "incendiaries=" + incendiaries + ", covers=" + covers + ", missile=" + missileCooldown; }

    @Override public void refresh(OperativeHost host) {
        var fresh = new NativeUluruSkills(capacityScale);
        coverRecharge = fresh.coverRecharge;
        covers = fresh.covers;
        incendiaries = fresh.incendiaries;
        incendiaryRecharge = fresh.incendiaryRecharge;
        missileCooldown = fresh.missileCooldown;
    }
}
