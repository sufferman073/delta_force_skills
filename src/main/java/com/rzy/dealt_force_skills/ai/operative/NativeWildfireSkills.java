package com.rzy.dealt_force_skills.ai.operative;

import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.character.SkillSlot;
import com.rzy.dealt_force_skills.character.wildfire.WildfireConfig;
import com.rzy.dealt_force_skills.character.wildfire.WildfireStateManager;
import com.rzy.dealt_force_skills.entity.OperativeEntity;
import com.rzy.dealt_force_skills.entity.WildfireDeviceEntity;
import com.rzy.dealt_force_skills.motion.TacticalMotion;
import com.rzy.dealt_force_skills.registry.ModEntities;
import com.rzy.dealt_force_skills.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import java.util.UUID;

import static com.rzy.dealt_force_skills.ai.operative.BehaviorNode.Status.*;

/** Native Wildfire utility decisions and a prepare/charge/leap behavior tree. */
public final class NativeWildfireSkills implements OperativeSkills {
    private static final net.minecraft.resources.ResourceLocation SPEED = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("dealt_force_skills", "operative_wildfire_speed");
    private static final net.minecraft.resources.ResourceLocation STEP = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("dealt_force_skills", "operative_wildfire_step");
    private int smoke = WildfireConfig.maxCharges(true), grenades = WildfireConfig.maxCharges(false);
    private int smokeRecharge, grenadeRecharge, coreCooldown, smokeDelay, grenadeDelay;
    private int phase, prepare, age, leapAge;
    private long passiveUntil;
    private UUID targetId;
    private Vec3 direction = Vec3.ZERO;
    private BehaviorNode coreTree;
    private long shotTick = Long.MIN_VALUE;
    private UUID shotTarget;
    private Vec3 shotOrigin, shotDestination;
    private ThrowPlan cachedShot;
    private record ThrowPlan(Vec3 direction, Vec3 impact, double speed, double lift) { }

    @Override public void tick(OperativeHost host) {
        smoke = Math.min(smoke, WildfireConfig.maxCharges(true));
        grenades = Math.min(grenades, WildfireConfig.maxCharges(false));
        if (smoke < WildfireConfig.maxCharges(true) && --smokeRecharge <= 0) {
            smoke++; smokeRecharge = smoke < WildfireConfig.maxCharges(true) ? WildfireConfig.cooldown(true) : 0;
        }
        if (grenades < WildfireConfig.maxCharges(false) && --grenadeRecharge <= 0) {
            grenades++; grenadeRecharge = grenades < WildfireConfig.maxCharges(false) ? WildfireConfig.cooldown(false) : 0;
        }
        if (smokeDelay > 0) smokeDelay--;
        if (grenadeDelay > 0) grenadeDelay--;
        if (phase == 0 && coreCooldown > 0) coreCooldown--;
        if (!NativeRaptorSkills.available(host) || host.body().isPassenger()
                || phase != 0 && OperativeElectronics.blocked(host, ModCharacters.WILDFIRE_ID, SkillSlot.CORE)) {
            cancel(host); return;
        }
        if (coreTree == null) coreTree = BehaviorNode.selector(
                BehaviorNode.sequence(() -> phase == 1 ? SUCCESS : FAILURE, () -> prepare(host)),
                BehaviorNode.sequence(() -> phase == 2 ? SUCCESS : FAILURE, () -> charge(host)),
                BehaviorNode.sequence(() -> phase == 3 ? SUCCESS : FAILURE, () -> land(host)));
        if (phase != 0) coreTree.tick();
        attributes(host);
    }

    public boolean passiveActive(OperativeHost host) {
        return !OperativeElectronics.blocked(host, ModCharacters.WILDFIRE_ID, SkillSlot.PASSIVE)
                && (host.body().getHealth() < host.body().getMaxHealth() * WildfireConfig.fraction("passive.health_threshold", .45)
                    || host.serverLevel().getGameTime() < passiveUntil);
    }

    public void onDamaged(OperativeHost host) {
        passiveUntil = host.serverLevel().getGameTime() + WildfireConfig.ticks("passive.duration_ticks", 60);
        attributes(host);
    }

    public boolean shieldActive() { return phase >= 2; }

    @Override public double score(OperativeHost host, SkillSlot slot, LivingEntity target) {
        if (busy() || !NativeRaptorSkills.available(host) || OperativeSupport.downed(host.body())
                || OperativeElectronics.blocked(host, ModCharacters.WILDFIRE_ID, slot)) return 0;
        boolean enemy = NativeRaptorSkills.committedTarget(host, target) && host.withinCombatRange(target.position());
        boolean hurt = host.body().getHealth() < host.body().getMaxHealth() * .5F;
        return switch (slot) {
            case ACTIVE_1 -> smoke > 0 && smokeDelay == 0 && (enemy || host.body().hurtTime > 0)
                    && (hurt || host.body() instanceof OperativeEntity bot && bot.gun().reloading(bot))
                    && host.serverLevel().getEntitiesOfClass(WildfireDeviceEntity.class,
                        host.body().getBoundingBox().inflate(16), e -> host.body().getUUID().equals(e.ownerId())
                            && e.mode() >= WildfireDeviceEntity.CLOUD).isEmpty() ? 125 : 0;
            case ACTIVE_2 -> enemy && grenades > 0 && grenadeDelay == 0 && host.body().distanceToSqr(target) <= 40 * 40
                    && grenadeShot(host, target) != null ? (host.canSee(target) ? 90 : 120) : 0;
            case CORE -> enemy && coreCooldown == 0 && !host.parallelNativeAi() && !host.body().isPassenger()
                    && host.body().onGround() && host.body().distanceToSqr(target) >= 9
                    && host.body().distanceToSqr(target) <= 24 * 24 && chargeLane(host, target) ? (hurt ? 100 : 75) : 0;
            default -> 0;
        };
    }

    @Override public boolean start(OperativeHost host, SkillSlot slot, LivingEntity target) {
        if (score(host, slot, target) <= 0) return false;
        if (slot == SkillSlot.ACTIVE_1) {
            WildfireDeviceEntity field = WildfireDeviceEntity.smoke(host.body(), host.body().position(), true);
            if (host.serverLevel().getEntity(field.getUUID()) != field) return false;
            smoke--; if (smokeRecharge <= 0) smokeRecharge = WildfireConfig.cooldown(true);
            smokeDelay = WildfireConfig.ticks("smoke.duration_ticks", 160);
            sound(host, ModSounds.WILDFIRE_SMOKE_RING.get());
        } else if (slot == SkillSlot.ACTIVE_2) {
            ThrowPlan plan = grenadeShot(host, target);
            if (plan == null) return false;
            var device = new WildfireDeviceEntity(ModEntities.WILDFIRE_DEVICE.get(), host.serverLevel());
            device.configure(host.body(), WildfireDeviceEntity.HE_GRENADE);
            device.setPos(host.body().getEyePosition().add(plan.direction.scale(.6)).add(0, -.1, 0));
            device.setDeltaMovement(plan.direction.scale(plan.speed).add(0, plan.lift, 0));
            if (!host.serverLevel().addFreshEntity(device)) return false;
            grenades--; if (grenadeRecharge <= 0) grenadeRecharge = WildfireConfig.cooldown(false);
            grenadeDelay = WildfireConfig.ticks("grenade.fuse_ticks", 60) + 10;
            host.aimAt(target); sound(host, ModSounds.WILDFIRE_GRENADE_THROW.get());
        } else if (slot == SkillSlot.CORE) {
            phase = 1; prepare = WildfireConfig.ticks("core.prepare_ticks", 40); age = 0;
            coreCooldown = WildfireConfig.coreCooldown(); targetId = target.getUUID();
            host.aimAt(target); host.body().stopUsingItem();
            if (host.body() instanceof Mob mob) mob.getNavigation().stop();
            sound(host, ModSounds.WILDFIRE_CORE_START.get());
        } else return false;
        return true;
    }

    private LivingEntity target(OperativeHost host) {
        return targetId != null && host.serverLevel().getEntity(targetId) instanceof LivingEntity living
                && NativeRaptorSkills.committedTarget(host, living) && host.withinOrders(living.position()) ? living : null;
    }

    private BehaviorNode.Status prepare(OperativeHost host) {
        LivingEntity enemy = target(host);
        if (enemy == null || !chargeLane(host, enemy)) { cancel(host); return FAILURE; }
        host.aimAt(enemy);
        if (--prepare <= 0) { phase = 2; age = 0; }
        return RUNNING;
    }

    private BehaviorNode.Status charge(OperativeHost host) {
        LivingEntity enemy = target(host);
        if (enemy == null || ++age > 200) { endCore(host); return FAILURE; }
        host.aimAt(enemy);
        direction = enemy.position().subtract(host.body().position()).multiply(1, 0, 1).normalize();
        double distance = host.body().distanceTo(enemy);
        double impact = Math.min(64, WildfireConfig.number("core.impact_radius", 4));
        // Leap into a group only with a clear landing lane and headroom; single targets get a bash.
        if (distance <= impact && distance >= 2 && host.body().onGround()
                && host.serverLevel().getEntitiesOfClass(LivingEntity.class, enemy.getBoundingBox().inflate(impact), host::isEnemy).size() >= 2
                && OperativeTactics.safeDisplacement(host, direction, Math.max(.5, distance - 1.5))
                && host.serverLevel().noCollision(host.body(), host.body().getBoundingBox().expandTowards(0, WildfireConfig.number("core.leap_height", 3), 0))) {
            phase = 3; leapAge = 0;
            host.body().setDeltaMovement(direction.x * .18, WildfireStateManager.leapVelocity(WildfireConfig.number("core.leap_height", 3)), direction.z * .18);
            host.body().hurtMarked = true; host.body().fallDistance = 0;
            sound(host, ModSounds.WILDFIRE_CORE_LEAP.get()); return RUNNING;
        }
        if (distance <= 1.8) { impact(host, true); return SUCCESS; }
        double speed = WildfireConfig.number("core.base_forward_speed", .28) * (1 + WildfireStateManager.speedBonus(age))
                * (passiveActive(host) ? 1 + WildfireConfig.number("passive.movement_bonus", .25) : 1);
        double lookahead = Math.max(.5, speed + .3);
        if (!OperativeTactics.safeDisplacement(host, direction, lookahead)) {
            // A real wall collision can breach doors; unsafe terrain never propels the host forward.
            if (!host.serverLevel().noCollision(host.body(), host.body().getBoundingBox().move(direction.scale(lookahead)).deflate(.02)))
                impact(host, true);
            else endCore(host);
            return SUCCESS;
        }
        host.body().setDeltaMovement(direction.x * speed, host.body().getDeltaMovement().y, direction.z * speed);
        host.body().hurtMarked = true;
        return RUNNING;
    }

    private BehaviorNode.Status land(OperativeHost host) {
        host.body().fallDistance = 0;
        // Stop horizontal steering on the ascent; do not follow a moving enemy through a wall.
        host.body().setDeltaMovement(0, host.body().getDeltaMovement().y, 0);
        if (++leapAge > 2 && host.body().onGround()) { impact(host, false); return SUCCESS; }
        if (leapAge > 100) endCore(host);
        return RUNNING;
    }

    private void impact(OperativeHost host, boolean front) {
        WildfireStateManager.impact(host.body(), front);
        endCore(host);
    }

    private static boolean chargeLane(OperativeHost host, LivingEntity target) {
        Vec3 offset = target.position().subtract(host.body().position()).multiply(1, 0, 1);
        double travel = Math.max(.5, offset.length() - 1.5);
        Vec3 direction = offset.normalize();
        var hit = host.serverLevel().clip(new ClipContext(host.body().getEyePosition(), target.getEyePosition(),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, host.body()));
        if (hit.getType() == HitResult.Type.BLOCK) {
            var block = host.serverLevel().getBlockState(hit.getBlockPos()).getBlock();
            if (!(block instanceof DoorBlock || block instanceof TrapDoorBlock)) return false;
            travel = Math.max(.5, host.body().position().distanceTo(hit.getLocation()) - 1);
        }
        return OperativeTactics.safeDisplacement(host, direction, travel);
    }

    /** Preview the real grenade's bounce, floor rest, drag-before-gravity and fuse. */
    private ThrowPlan grenadeShot(OperativeHost host, LivingEntity target) {
        long now = host.serverLevel().getGameTime();
        if (now == shotTick && target.getUUID().equals(shotTarget) && host.body().position().equals(shotOrigin)
                && target.position().equals(shotDestination)) return safeBlast(host, cachedShot) ? cachedShot : null;
        shotTick = now; shotTarget = target.getUUID(); shotOrigin = host.body().position(); shotDestination = target.position();
        cachedShot = null;
        Vec3 horizontal = target.position().subtract(shotOrigin).multiply(1, 0, 1).normalize();
        double radius = Math.min(64, WildfireConfig.number("grenade.radius", 7));
        double best = Math.pow(Math.max(.5, radius * .65), 2);
        for (boolean high : new boolean[]{false, true}) {
            double speed = WildfireConfig.number(high ? "projectile.high_speed" : "projectile.low_speed", high ? 1.55 : 1.05);
            double lift = WildfireConfig.number(high ? "projectile.high_lift" : "projectile.low_lift", high ? .3 : .08);
            for (int degrees = -30; degrees <= 80; degrees += 5) {
                double angle = Math.toRadians(degrees);
                Vec3 direction = horizontal.scale(Math.cos(angle)).add(0, Math.sin(angle), 0);
                Vec3 impact = preview(host, direction, speed, lift);
                if (impact == null) continue;
                double error = impact.distanceToSqr(target.position());
                if (error >= best) continue;
                ThrowPlan plan = new ThrowPlan(direction, impact, speed, lift);
                if (safeBlast(host, plan)) { cachedShot = plan; best = error; }
            }
        }
        return cachedShot;
    }

    private static Vec3 preview(OperativeHost host, Vec3 direction, double speed, double lift) {
        Vec3 point = host.body().getEyePosition().add(direction.scale(.6)).add(0, -.1, 0);
        if (host.serverLevel().clip(new ClipContext(host.body().getEyePosition(), point, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, host.body())).getType() != HitResult.Type.MISS) return null;
        Vec3 motion = direction.scale(speed).add(0, lift, 0);
        int fuse = WildfireConfig.ticks("grenade.fuse_ticks", 60);
        for (int tick = 1; tick < Math.min(fuse, 240); tick++) {
            Vec3 next = point.add(motion);
            if (!host.withinOrders(next) || !host.serverLevel().hasChunkAt(BlockPos.containing(next))) return null;
            var hit = host.serverLevel().clip(new ClipContext(point, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, host.body()));
            if (hit.getType() == HitResult.Type.BLOCK) {
                Vec3 normal = Vec3.atLowerCornerOf(hit.getDirection().getNormal());
                point = hit.getLocation().add(normal.scale(.04));
                if (hit.getDirection() == Direction.UP) return point;
                motion = motion.subtract(normal.scale(2 * motion.dot(normal))).scale(WildfireConfig.fraction("projectile.bounce_factor", .7));
            } else {
                point = next;
                motion = motion.scale(WildfireConfig.fraction("projectile.drag", .985)).add(0, -WildfireConfig.number("projectile.gravity", .045), 0);
            }
        }
        return fuse <= 240 ? point : null;
    }

    private static boolean safeBlast(OperativeHost host, ThrowPlan plan) {
        if (plan == null || !host.withinOrders(plan.impact)) return false;
        double radius = Math.min(64, WildfireConfig.number("grenade.radius", 7)) + 1;
        if (plan.impact.distanceToSqr(host.body().position()) <= radius * radius) return false;
        return host.serverLevel().getEntitiesOfClass(LivingEntity.class, new AABB(plan.impact, plan.impact).inflate(radius),
                e -> e.isAlive() && !host.isEnemy(e) && e.position().distanceToSqr(plan.impact) <= radius * radius).isEmpty();
    }

    private void attributes(OperativeHost host) {
        modifier(host.body().getAttribute(Attributes.MOVEMENT_SPEED), SPEED,
                passiveActive(host) ? WildfireConfig.number("passive.movement_bonus", .25) : 0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        modifier(host.body().getAttribute(Attributes.STEP_HEIGHT), STEP,
                phase >= 2 ? WildfireConfig.number("core.step_height_bonus", 1) : 0, AttributeModifier.Operation.ADD_VALUE);
    }
    private static void modifier(AttributeInstance attribute, net.minecraft.resources.ResourceLocation id, double amount, AttributeModifier.Operation operation) {
        if (attribute == null) return;
        var old = attribute.getModifier(id);
        if (old != null && old.amount() == amount) return;
        if (old != null) attribute.removeModifier(id);
        if (amount != 0) attribute.addTransientModifier(new AttributeModifier(id, amount, operation));
    }
    private void endCore(OperativeHost host) {
        boolean moving = phase >= 2;
        phase = prepare = age = leapAge = 0; targetId = null; direction = Vec3.ZERO;
        if (moving) host.body().setDeltaMovement(0, host.body().getDeltaMovement().y, 0);
        attributes(host);
    }
    @Override public void cancel(OperativeHost host) {
        if (phase == 1) coreCooldown = (int)Math.ceil(WildfireConfig.coreCooldown() * (1 - WildfireConfig.fraction("core.cancel_refund", .9)));
        endCore(host);
        modifier(host.body().getAttribute(Attributes.MOVEMENT_SPEED), SPEED, 0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }
    @Override public boolean busy() { return phase != 0; }
    @Override public TacticalMotion motion() { return phase == 1 ? TacticalMotion.HOLD_TOOL : phase >= 2 ? TacticalMotion.CHARGE : TacticalMotion.NONE; }
    @Override public Vec3 motionDirection() { return direction; }
    @Override public CompoundTag save() {
        var tag = new CompoundTag(); tag.putInt("Smoke", smoke); tag.putInt("Grenades", grenades);
        tag.putInt("SmokeRecharge", smokeRecharge); tag.putInt("GrenadeRecharge", grenadeRecharge);
        tag.putInt("CoreCooldown", coreCooldown); tag.putInt("SmokeDelay", smokeDelay); tag.putInt("GrenadeDelay", grenadeDelay); return tag;
    }
    @Override public void load(CompoundTag tag) {
        smoke = bounded(tag, "Smoke", WildfireConfig.maxCharges(true)); grenades = bounded(tag, "Grenades", WildfireConfig.maxCharges(false));
        smokeRecharge = bounded(tag, "SmokeRecharge", WildfireConfig.cooldown(true)); grenadeRecharge = bounded(tag, "GrenadeRecharge", WildfireConfig.cooldown(false));
        coreCooldown = bounded(tag, "CoreCooldown", WildfireConfig.coreCooldown()); smokeDelay = bounded(tag, "SmokeDelay", 72000); grenadeDelay = bounded(tag, "GrenadeDelay", 72010);
        phase = prepare = age = leapAge = 0; targetId = null; passiveUntil = 0; direction = Vec3.ZERO; shotTick = Long.MIN_VALUE;
    }
    private static int bounded(CompoundTag tag, String key, int max) { return Math.max(0, Math.min(max, tag.getInt(key))); }
    @Override public void refresh(OperativeHost host) {
        smoke = WildfireConfig.maxCharges(true); grenades = WildfireConfig.maxCharges(false);
        smokeRecharge = grenadeRecharge = coreCooldown = 0;
    }
    @Override public String describe() { return "smoke=" + smoke + ",grenades=" + grenades + ",core=" + coreCooldown + ",phase=" + phase; }
    private static void sound(OperativeHost host, SoundEvent sound) { host.serverLevel().playSound(null, host.body().blockPosition(), sound, SoundSource.NEUTRAL, .85F, 1); }
}
