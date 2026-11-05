package com.rzy.dealt_force_skills.ai.operative;

import com.rzy.dealt_force_skills.character.SkillSlot;
import com.rzy.dealt_force_skills.character.vyron.VyronProjectiles;
import com.rzy.dealt_force_skills.character.vyron.VyronStateManager;
import com.rzy.dealt_force_skills.entity.VyronMagneticBombEntity;
import com.rzy.dealt_force_skills.entity.VyronTigerCannonEntity;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.util.RangedSoundHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/** Native Vyron kit with inherited Commander propulsion upgrades. */
public final class NativeVyronSkills implements OperativeSkills {
    private int dashCooldown;
    private int coreCooldown;
    private int bombCharges = Math.max(0, VyronStateManager.MAGNETIC_BOMB_MAX_CHARGES);
    private int bombRecharge;
    private int poweredTicks;
    private boolean propulsionDescending;
    private int dashTicks;
    private Vec3 dashDirection = Vec3.ZERO;
    private int cannonWindup;
    private UUID cannonTarget;

    @Override
    public void tick(OperativeHost host) {
        if (dashCooldown > 0) dashCooldown--;
        if (coreCooldown > 0) coreCooldown--;
        int capacity = Math.max(0, VyronStateManager.MAGNETIC_BOMB_MAX_CHARGES);
        bombCharges = Math.min(bombCharges, capacity);
        if (bombCharges < capacity && --bombRecharge <= 0) {
            bombCharges++;
            bombRecharge = Math.max(1, VyronStateManager.MAGNETIC_BOMB_RECHARGE_TICKS);
        }
        if (poweredTicks > 0) poweredTicks--;
        if (OperativeGrowth.commander(host.body())) {
            if (host.body().onGround()) {
                if (propulsionDescending) power();
                propulsionDescending = false;
            } else if (!host.body().isPassenger() && !host.body().isInWaterOrBubble()
                    && !host.body().onClimbable() && host.body().getDeltaMovement().y < -.01) propulsionDescending = true;
        }
        updatePowered(host);
        if (dashTicks > 0) {
            if (host.body().horizontalCollision || !safeDash(host, dashDirection)) {
                dashTicks = 0;
            } else {
                Vec3 motion = dashDirection.scale((VyronStateManager.dashSpeed() * 0.5D));
                host.body().setDeltaMovement(motion.x, 0.0D, motion.z);
                host.body().fallDistance = 0;
                host.body().hurtMarked = true;
                dashTicks--;
            }
        }
        if (cannonWindup > 0) {
            LivingEntity target = host.serverLevel().getEntity(cannonTarget) instanceof LivingEntity living ? living : null;
            if (!validTarget(host, target, 4.0D, 24.0D)) {
                cannonWindup = 0;
                cannonTarget = null;
                return;
            }
            host.aimAt(target);
            if (--cannonWindup == 0) {
                var shot = cannonShot(host, target);
                if (shot != null && safeBlast(host, shot.impact(), VyronTigerCannonEntity.blastRadius())) {
                    aim(host, shot.direction());
                    if (VyronProjectiles.fireTigerCannon(host.serverLevel(), host.body())) {
                    coreCooldown = Math.max(1, VyronStateManager.TIGER_CANNON_COOLDOWN_TICKS);
                    power();
                    sound(host, ModSounds.VYRON_TIGER_CANNON_FIRE.get());
                    }
                }
                cannonTarget = null;
            }
        }
    }

    @Override
    public double score(OperativeHost host, SkillSlot slot, LivingEntity target) {
        if (busy()) return 0;
        return switch (slot) {
            case ACTIVE_1 -> dashCooldown == 0 && validTarget(host, target, 3, 56)
                    && OperativeTactics.displacement(host, target, (VyronStateManager.dashSpeed() * 0.5D) * Math.max(1, VyronStateManager.DASH_DURATION_TICKS)).lengthSqr() > .5 ? 45 : 0;
            case ACTIVE_2 -> bombCharges > 0 && validTarget(host, target, 9, 18)
                    && safeShot(host, bombShot(host, target), VyronMagneticBombEntity.blastRadius()) ? 80 : 0;
            case CORE -> coreCooldown == 0 && validTarget(host, target, 5, 24)
                    && safeShot(host, cannonShot(host, target), VyronTigerCannonEntity.blastRadius()) ? 65 : 0;
            case PASSIVE -> 0;
        };
    }

    @Override
    public boolean start(OperativeHost host, SkillSlot slot, LivingEntity target) {
        if (score(host, slot, target) <= 0) return false;
        host.aimAt(target);
        switch (slot) {
            case ACTIVE_1 -> {
                dashDirection = OperativeTactics.displacement(host, target, (VyronStateManager.dashSpeed() * 0.5D) * Math.max(1, VyronStateManager.DASH_DURATION_TICKS));
                if(!host.parallelNativeAi() && host.body() instanceof net.minecraft.world.entity.Mob mob){
                    mob.getNavigation().stop();mob.setZza(0);mob.setXxa(0);
                }
                dashTicks = Math.max(1, VyronStateManager.DASH_DURATION_TICKS);
                dashCooldown = Math.max(1, VyronStateManager.DASH_COOLDOWN_TICKS);
                power();
                sound(host, ModSounds.VYRON_DASH.get());
            }
            case ACTIVE_2 -> {
                var shot = bombShot(host, target);
                if (!safeShot(host, shot, VyronMagneticBombEntity.blastRadius())) return false;
                aim(host, shot.direction());
                if (!VyronProjectiles.throwMagneticBomb(host.serverLevel(), host.body(), false)) return false;
                if (bombCharges == Math.max(0, VyronStateManager.MAGNETIC_BOMB_MAX_CHARGES)) {
                    bombRecharge = Math.max(1, VyronStateManager.MAGNETIC_BOMB_RECHARGE_TICKS);
                }
                bombCharges--;
                power();
                sound(host, ModSounds.VYRON_MAGNETIC_BOMB_THROW.get());
            }
            case CORE -> {
                if (host.parallelNativeAi()) {
                    var shot = cannonShot(host, target);
                    if (shot == null || !safeBlast(host, shot.impact(), VyronTigerCannonEntity.blastRadius())) return false;
                    aim(host, shot.direction());
                    if (!VyronProjectiles.fireTigerCannon(host.serverLevel(), host.body())) return false;
                    coreCooldown = Math.max(1, VyronStateManager.TIGER_CANNON_COOLDOWN_TICKS);
                    power(); sound(host, ModSounds.VYRON_TIGER_CANNON_FIRE.get());
                    return true;
                }
                cannonTarget = target.getUUID();
                cannonWindup = 10;
                sound(host, ModSounds.VYRON_TIGER_CANNON_EQUIP.get());
            }
            default -> { return false; }
        }
        return true;
    }

    private static void aim(OperativeHost host, Vec3 offset) {
        host.body().setYRot((float) (Math.toDegrees(Math.atan2(offset.z, offset.x)) - 90.0D));
        host.body().setYHeadRot(host.body().getYRot());
        host.body().setXRot((float) -Math.toDegrees(Math.atan2(offset.y, offset.horizontalDistance())));
    }

    private static boolean validTarget(OperativeHost host, LivingEntity target, double min, double max) {
        if (target == null || !host.isEnemy(target) || !host.canSee(target)
                || !host.withinCombatRange(target.position()) || !host.withinOrders(host.body().position())) return false;
        double distance = host.body().distanceToSqr(target);
        return distance >= min * min && distance <= max * max;
    }

    private static OperativeBallistics.Shot bombShot(OperativeHost host, LivingEntity target) {
        return OperativeBallistics.solve(host, target.position().add(0, 0.1D, 0),
                1.08D, 0.06D, 0.62D, 0.1D, 0.04D, 0.99D, 80,
                VyronMagneticBombEntity.blastRadius() * 0.5D, false);
    }

    private static OperativeBallistics.Shot cannonShot(OperativeHost host, LivingEntity target) {
        return OperativeBallistics.solve(host, target.position().add(0, 0.1D, 0),
                1.9D, 0.08D, 0.8D, 0.08D, 0.045D, 0.992D, 55,
                VyronTigerCannonEntity.blastRadius() * 0.5D, true);
    }

    private static boolean safeShot(OperativeHost host, OperativeBallistics.Shot shot, double radius) {
        return shot != null && safeBlast(host, shot.impact(), radius);
    }

    private static boolean safeBlast(OperativeHost host, Vec3 impact, double radius) {
        if (host.body().position().distanceToSqr(impact) <= (radius + 1) * (radius + 1)) return false;
        return host.serverLevel().getEntitiesOfClass(LivingEntity.class, new AABB(impact, impact).inflate(radius),
                candidate -> candidate != host.body() && candidate.isAlive() && !host.isEnemy(candidate)).isEmpty();
    }

    private static boolean safeDash(OperativeHost host, Vec3 direction) {
        Vec3 horizontal = new Vec3(direction.x, 0, direction.z).normalize();
        if (horizontal.lengthSqr() < 0.01D) return false;
        Vec3 next = host.body().position().add(horizontal.scale((VyronStateManager.dashSpeed() * 0.5D)));
        AABB destination = host.body().getBoundingBox().move(next.subtract(host.body().position()));
        return host.withinOrders(next) && host.serverLevel().hasChunkAt(net.minecraft.core.BlockPos.containing(next))
                && host.serverLevel().getWorldBorder().isWithinBounds(destination)
                && host.body().level().noCollision(host.body(), destination)
                && !host.body().level().noCollision(host.body(), destination.move(0, -1, 0));
    }

    private void power() {
        poweredTicks = Math.max(1, VyronStateManager.POWERED_DURATION_TICKS);
    }

    private void updatePowered(OperativeHost host) {
        var speed = host.body().getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) return;
        var existing = speed.getModifier(VyronStateManager.POWERED_SPEED_UUID);
        if (poweredTicks > 0 && existing == null) {
            speed.addTransientModifier(new AttributeModifier(VyronStateManager.POWERED_SPEED_UUID,
                    0.5D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        } else if (poweredTicks <= 0 && existing != null) {
            speed.removeModifier(VyronStateManager.POWERED_SPEED_UUID);
        }
    }

    private static void sound(OperativeHost host, net.minecraft.sounds.SoundEvent sound) {
        RangedSoundHelper.playThrottled(host.serverLevel(), host.body().position(), sound,
                SoundSource.NEUTRAL, 1.0F, 1.0F, 24.0D, 4, 2.0D);
    }

    /** Vyron's passive resets the dash cooldown after a confirmed kill. */
    public void onKill() { dashCooldown = 0; }

    public void onLanding(float distance, boolean propulsion) {
        if (distance > 3.5F || propulsion) power();
    }

    @Override
    public boolean busy() { return dashTicks > 0 || cannonWindup > 0; }

    @Override
    public void cancel(OperativeHost host) {
        if (dashTicks > 0) {
            var motion = host.body().getDeltaMovement();
            host.body().setDeltaMovement(0, motion.y, 0);
        }
        dashTicks = cannonWindup = poweredTicks = 0;
        propulsionDescending = false;
        cannonTarget = null;
        updatePowered(host);
    }

    @Override
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("DashCooldown", dashCooldown);
        tag.putInt("CoreCooldown", coreCooldown);
        tag.putInt("BombCharges", bombCharges);
        tag.putInt("BombRecharge", bombRecharge);
        tag.putInt("PoweredTicks", poweredTicks);
        // Movement and uncommitted windups are cancelled on load, without spending a core charge.
        return tag;
    }

    @Override
    public void load(CompoundTag tag) {
        dashCooldown = remaining(tag, "DashCooldown", VyronStateManager.DASH_COOLDOWN_TICKS);
        coreCooldown = remaining(tag, "CoreCooldown", VyronStateManager.TIGER_CANNON_COOLDOWN_TICKS);
        bombCharges = Math.max(0, Math.min(VyronStateManager.MAGNETIC_BOMB_MAX_CHARGES, tag.getInt("BombCharges")));
        bombRecharge = Math.max(1, remaining(tag, "BombRecharge", VyronStateManager.MAGNETIC_BOMB_RECHARGE_TICKS));
        poweredTicks = remaining(tag, "PoweredTicks", VyronStateManager.POWERED_DURATION_TICKS);
        dashTicks = cannonWindup = 0;
        cannonTarget = null;
    }

    @Override
    public String describe() {
        return "dash=" + dashCooldown + ", bombs=" + bombCharges + ", recharge=" + bombRecharge
                + ", core=" + coreCooldown + ", windup=" + cannonWindup + ", powered=" + poweredTicks;
    }

    private static int remaining(CompoundTag tag, String key, int limit) {
        return Math.max(0, Math.min(Math.max(0, limit), tag.getInt(key)));
    }
    @Override public void refresh(OperativeHost host) {
        var fresh = new NativeVyronSkills();
        bombCharges = fresh.bombCharges;
        bombRecharge = fresh.bombRecharge;
        coreCooldown = fresh.coreCooldown;
        dashCooldown = fresh.dashCooldown;
    }
    @Override public com.rzy.dealt_force_skills.motion.TacticalMotion motion() {
        return dashTicks > 0 ? com.rzy.dealt_force_skills.motion.TacticalMotion.JET : com.rzy.dealt_force_skills.motion.TacticalMotion.NONE;
    }
    @Override public int motionDuration() { return Math.max(1, VyronStateManager.DASH_DURATION_TICKS); }
    @Override public Vec3 motionDirection() { return Vec3.ZERO; }

    @Override public boolean locksMovement() { return dashTicks > 0; }
}
