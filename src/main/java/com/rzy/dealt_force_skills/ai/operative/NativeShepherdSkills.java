package com.rzy.dealt_force_skills.ai.operative;

import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.character.SkillSlot;
import com.rzy.dealt_force_skills.character.shepherd.ShepherdPlacementHelper;
import com.rzy.dealt_force_skills.character.shepherd.ShepherdStateManager;
import com.rzy.dealt_force_skills.entity.ShepherdDroneEntity;
import com.rzy.dealt_force_skills.entity.ShepherdFragGrenadeEntity;
import com.rzy.dealt_force_skills.entity.ShepherdSonicTrapEntity;
import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.registry.ModEntities;
import com.rzy.dealt_force_skills.registry.ModSounds;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import java.util.List;
import java.util.UUID;

/** Native base Shepherd: bounded floor traps, bouncing frag and tap/guard sonic drone. */
public final class NativeShepherdSkills implements OperativeSkills {
    private int traps = Math.max(0, ShepherdStateManager.SONIC_TRAP_MAX_CHARGES);
    private int frags = Math.max(0, ShepherdStateManager.FRAG_GRENADE_MAX_CHARGES);
    private int trapRecharge, fragRecharge, coreCooldown, trapDelay;
    private long shotTime = Long.MIN_VALUE;
    private UUID shotTarget;
    private Vec3 shotOrigin, targetPosition;
    private OperativeBallistics.Shot cachedShot;
    private UUID cookingTarget;
    private int cookedTicks;

    @Override public void tick(OperativeHost host) {
        if (coreCooldown > 0) coreCooldown--;
        if (trapDelay > 0) trapDelay--;
        int allowed = Math.max(0, ShepherdStateManager.SONIC_TRAP_MAX_CHARGES - activeTraps(host).size());
        traps = Math.min(traps, allowed);
        if (traps >= allowed) trapRecharge = 0;
        else if (trapRecharge <= 0) trapRecharge = Math.max(1, ShepherdStateManager.SONIC_TRAP_RECHARGE_TICKS);
        else if (--trapRecharge == 0) traps++;
        frags = Math.min(frags, Math.max(0, ShepherdStateManager.FRAG_GRENADE_MAX_CHARGES));
        if (frags < Math.max(0, ShepherdStateManager.FRAG_GRENADE_MAX_CHARGES) && --fragRecharge <= 0) {
            frags++; fragRecharge = Math.max(1, ShepherdStateManager.FRAG_GRENADE_RECHARGE_TICKS);
        }
        if (cookingTarget == null) return;
        LivingEntity target = host.serverLevel().getEntity(cookingTarget) instanceof LivingEntity living ? living : null;
        if (!NativeRaptorSkills.available(host) || target == null || !host.isEnemy(target) 
                || !host.withinCombatRange(target.position()) || OperativeElectronics.blocked(host, ModCharacters.SHEPHERD_ID, SkillSlot.ACTIVE_2)) {
            cancel(host); return;
        }
        host.aimAt(target);
        int remaining = ShepherdStateManager.FRAG_GRENADE_FUSE_TICKS - ++cookedTicks;
        var shot = fragShot(host, target);
        if (!safeFrag(host, shot) || remaining < shot.flightTicks()) { cancel(host); return; }
        if (remaining > shot.flightTicks()) return;
        if (throwFrag(host, shot, remaining))
            host.serverLevel().playSound(null, host.body().blockPosition(), ModSounds.SHEPHERD_FRAG_GRENADE_THROW.get(), SoundSource.NEUTRAL, .8F, 1);
        cookingTarget = null; cookedTicks = 0;
    }

    private boolean throwFrag(OperativeHost host, OperativeBallistics.Shot shot, int fuse) {
        var grenade = new ShepherdFragGrenadeEntity(ModEntities.SHEPHERD_FRAG_GRENADE.get(), host.serverLevel(), host.body(), fuse);
        grenade.setPos(host.body().getEyePosition().add(shot.direction().scale(.7)).add(0, -.12, 0));
        grenade.setDeltaMovement(shot.direction().scale(1.45).add(0, .18, 0));
        grenade.setYRot((float) (Math.toDegrees(Math.atan2(shot.direction().z, shot.direction().x)) - 90));
        grenade.setXRot((float) -Math.toDegrees(Math.asin(shot.direction().y)));
        if (!host.serverLevel().addFreshEntity(grenade)) return false;
        if (frags == Math.max(0, ShepherdStateManager.FRAG_GRENADE_MAX_CHARGES))
            fragRecharge = Math.max(1, ShepherdStateManager.FRAG_GRENADE_RECHARGE_TICKS);
        frags--; return true;
    }

    @Override public double score(OperativeHost host, SkillSlot slot, LivingEntity target) {
        if (busy() || !host.body().isAlive() || host.body().isRemoved() || !host.withinOrders(host.body().position())
                || host.body().hasEffect(ModEffects.STUN.get()) || host.body().hasEffect(ModEffects.WEBBED.get())
                || target == null || !host.isEnemy(target) || !host.withinCombatRange(target.position())
                || slot == SkillSlot.ACTIVE_1 && !host.canSee(target) || host.body().distanceToSqr(target) > (slot == SkillSlot.ACTIVE_2 ? 40 * 40 : 24 * 24)
                || OperativeElectronics.blocked(host, ModCharacters.SHEPHERD_ID, slot)) return 0;
        return switch (slot) {
            case ACTIVE_1 -> traps > 0 && trapDelay == 0
                    && activeTraps(host).size() < Math.max(0, ShepherdStateManager.SONIC_TRAP_ACTIVE_LIMIT)
                    && trapPosition(host, target) != null ? 75 : 0;
            case ACTIVE_2 -> frags > 0 && safeFrag(host, fragShot(host, target)) ? 108 : 0;
            case CORE -> coreCooldown == 0 && droneFits(host) ? 100 : 0;
            default -> 0;
        };
    }

    @Override public boolean start(OperativeHost host, SkillSlot slot, LivingEntity target) {
        if (score(host, slot, target) <= 0) return false;
        host.aimAt(target);
        net.minecraft.sounds.SoundEvent sound;
        if (slot == SkillSlot.ACTIVE_1) {
            Vec3 position = trapPosition(host, target);
            if (position == null) return false;
            var trap = new ShepherdSonicTrapEntity(ModEntities.SHEPHERD_SONIC_TRAP.get(), host.serverLevel(), host.body(), Direction.UP);
            trap.setPos(position);
            if (!host.serverLevel().addFreshEntity(trap)) return false;
            traps--; trapDelay = 100;
            sound = ModSounds.SHEPHERD_SONIC_TRAP_DEPLOY.get();
        } else if (slot == SkillSlot.ACTIVE_2) {
            var shot = fragShot(host, target);
            if (!safeFrag(host, shot)) return false;
            if (host.parallelNativeAi()) return throwFrag(host, shot, shot.flightTicks());
            cookingTarget = target.getUUID(); cookedTicks = 0;
            return true;
        } else {
            boolean patrol = host.body() instanceof com.rzy.dealt_force_skills.entity.OperativeEntity bot && bot.isGuarding();
            var drone = new ShepherdDroneEntity(ModEntities.SHEPHERD_DRONE.get(), host.serverLevel(), host.body(), patrol);
            drone.setPos(droneStart(host));
            if (!host.serverLevel().addFreshEntity(drone)) return false;
            coreCooldown = Math.max(1, ShepherdStateManager.CORE_COOLDOWN_TICKS);
            sound = ModSounds.SHEPHERD_DRONE_TAKEOFF.get();
        }
        host.serverLevel().playSound(null, host.body().blockPosition(), sound, SoundSource.NEUTRAL, 0.8F, 1);
        return true;
    }

    private static List<ShepherdSonicTrapEntity> activeTraps(OperativeHost host) {
        return host.serverLevel().getEntitiesOfClass(ShepherdSonicTrapEntity.class,
                host.body().getBoundingBox().inflate(100), e -> !e.isRemoved() && e.isOwnedBy(host.body().getUUID()));
    }

    private static Vec3 trapPosition(OperativeHost host, LivingEntity target) {
        Vec3 toward = target.position().subtract(host.body().position()).multiply(1, 0, 1);
        if (toward.lengthSqr() < 4) return null;
        double max = Math.min(6, Math.min(ShepherdPlacementHelper.SONIC_TRAP_RANGE - 1, toward.length() - 1.5D));
        for (double distance = max; distance >= 1; distance -= 2) {
            Vec3 point = host.body().position().add(toward.normalize().scale(distance));
            BlockHitResult hit = host.serverLevel().clip(new ClipContext(point.add(0, 1.5D, 0), point.add(0, -1.5D, 0),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, host.body()));
            if (hit.getType() != HitResult.Type.BLOCK || hit.getDirection() != Direction.UP) continue;
            Vec3 position = hit.getLocation().add(0, 0.025D, 0);
            if (!host.withinOrders(position) || position.distanceToSqr(host.body().getEyePosition()) > ShepherdPlacementHelper.SONIC_TRAP_RANGE * ShepherdPlacementHelper.SONIC_TRAP_RANGE) continue;
            if (host.serverLevel().clip(new ClipContext(host.body().getEyePosition(), position.add(0, 0.1D, 0),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, host.body())).getType() != HitResult.Type.MISS) continue;
            AABB bounds = new AABB(position.x - 0.19D, position.y, position.z - 0.19D, position.x + 0.19D, position.y + 0.22D, position.z + 0.19D);
            if (!host.serverLevel().noCollision(bounds) || !host.serverLevel().getEntitiesOfClass(LivingEntity.class, bounds.inflate(0.1D), LivingEntity::isAlive).isEmpty()) continue;
            if (!host.serverLevel().getEntitiesOfClass(ShepherdSonicTrapEntity.class, bounds.inflate(1)).isEmpty()) continue;
            return position;
        }
        return null;
    }

    /** Cache geometry within one world tick; allegiance is rechecked on every score/start. */
    private OperativeBallistics.Shot fragShot(OperativeHost host, LivingEntity target) {
        if (shotTime == host.serverLevel().getGameTime() && target.getUUID().equals(shotTarget)
                && host.body().position().equals(shotOrigin) && target.position().equals(targetPosition)) return cachedShot;
        shotTime = host.serverLevel().getGameTime(); shotTarget = target.getUUID();
        shotOrigin = host.body().position(); targetPosition = target.position(); cachedShot = null;
        Vec3 horizontal = target.position().subtract(host.body().position()).multiply(1, 0, 1).normalize();
        double best = Math.pow(Math.max(0.1D, ShepherdFragGrenadeEntity.nativeRadius() * 0.4D), 2);
        for (int degrees = -80; degrees <= 85; degrees += 3) {
            double angle = Math.toRadians(degrees);
            Vec3 direction = horizontal.scale(Math.cos(angle)).add(0, Math.sin(angle), 0);
            var shot = ShepherdFragGrenadeEntity.previewCookedShot(host, direction, target.getBoundingBox().getCenter());
            if (!safeFrag(host, shot)) continue;
            double error = shot.impact().distanceToSqr(target.getBoundingBox().getCenter());
            // Once inside the useful blast radius, prefer a quick throw over a needless high lob.
            if (cachedShot == null || shot.flightTicks() < cachedShot.flightTicks()
                    || shot.flightTicks() == cachedShot.flightTicks() && error < best) {
                best = error; cachedShot = shot;
            }
        }
        return cachedShot;
    }

    private static boolean safeFrag(OperativeHost host, OperativeBallistics.Shot shot) {
        if (shot == null || !host.withinOrders(shot.impact())) return false;
        double radius = ShepherdFragGrenadeEntity.nativeRadius() + 1;
        if (shot.impact().distanceToSqr(host.body().position()) <= radius * radius) return false;
        return host.serverLevel().getEntitiesOfClass(LivingEntity.class, new AABB(shot.impact(), shot.impact()).inflate(radius),
                e -> e != host.body() && e.isAlive() && !host.isEnemy(e)).isEmpty();
    }

    private static Vec3 droneStart(OperativeHost host) { return host.body().getEyePosition().add(0, 0.2D, 0); }
    private static boolean droneFits(OperativeHost host) {
        Vec3 p = droneStart(host);
        return host.serverLevel().noCollision(new AABB(p.x - 0.3D, p.y, p.z - 0.3D, p.x + 0.3D, p.y + 0.6D, p.z + 0.3D));
    }

    @Override public boolean busy() { return cookingTarget != null; }
    @Override public boolean locksWeapon() { return busy(); }
    @Override public void cancel(OperativeHost host) { cookingTarget = null; cookedTicks = 0; shotTime = Long.MIN_VALUE; cachedShot = null; }
    @Override public CompoundTag save() {
        var tag = new CompoundTag();
        tag.putInt("TrapCharges", traps); tag.putInt("FragCharges", frags);
        tag.putInt("TrapRecharge", trapRecharge); tag.putInt("FragRecharge", fragRecharge);
        tag.putInt("CoreCooldown", coreCooldown); tag.putInt("TrapDelay", trapDelay);
        return tag;
    }
    @Override public void load(CompoundTag tag) {
        traps = bound(tag, "TrapCharges", ShepherdStateManager.SONIC_TRAP_MAX_CHARGES);
        frags = bound(tag, "FragCharges", ShepherdStateManager.FRAG_GRENADE_MAX_CHARGES);
        trapRecharge = bound(tag, "TrapRecharge", ShepherdStateManager.SONIC_TRAP_RECHARGE_TICKS);
        fragRecharge = bound(tag, "FragRecharge", ShepherdStateManager.FRAG_GRENADE_RECHARGE_TICKS);
        coreCooldown = bound(tag, "CoreCooldown", ShepherdStateManager.CORE_COOLDOWN_TICKS);
        trapDelay = bound(tag, "TrapDelay", 100);
        cookingTarget = null; cookedTicks = 0; shotTime = Long.MIN_VALUE; cachedShot = null;
    }
    private static int bound(CompoundTag tag, String key, int max) { return Math.max(0, Math.min(Math.max(0, max), tag.getInt(key))); }
    @Override public String describe() { return "traps=" + traps + ", frags=" + frags + ", drone=" + coreCooldown; }

    @Override public void refresh(OperativeHost host) {
        var fresh = new NativeShepherdSkills();
        coreCooldown = fresh.coreCooldown;
        fragRecharge = fresh.fragRecharge;
        frags = fresh.frags;
        trapRecharge = fresh.trapRecharge;
        traps = fresh.traps;
    }
}
