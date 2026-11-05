package com.rzy.dealt_force_skills.ai.operative;

import com.rzy.dealt_force_skills.character.SkillSlot;
import com.rzy.dealt_force_skills.character.luna.LunaStateManager;
import com.rzy.dealt_force_skills.entity.LunaCompositeGrenadeEntity;
import com.rzy.dealt_force_skills.entity.LunaReconArrowEntity;
import com.rzy.dealt_force_skills.entity.LunaShockArrowEntity;
import com.rzy.dealt_force_skills.registry.ModEntities;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.util.RangedSoundHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Base Luna: draw/release arrows, time a cooked grenade, and use hit/recon exposure. */
public final class NativeLunaSkills implements OperativeSkills {
    private final double capacityScale;
    private int shocks, grenades;

    public NativeLunaSkills() { this(1); }
    NativeLunaSkills(double capacityScale) {
        this.capacityScale = capacityScale;
        shocks = shockCapacity(); grenades = grenadeCapacity();
    }
    private int shockCapacity() { return capacity(LunaStateManager.SHOCK_ARROW_MAX_CHARGES); }
    private int grenadeCapacity() { return capacity(LunaStateManager.COMPOSITE_GRENADE_MAX_CHARGES); }
    private int capacity(int base) { return (int) Math.min(Integer.MAX_VALUE, Math.ceil(Math.max(0, base) * capacityScale)); }
    private int shockRecharge, grenadeRecharge, coreCooldown;
    private SkillSlot prepared;
    private UUID preparedTarget;
    private int windup, cookedTicks;
    private final Map<UUID, Integer> revealed = new HashMap<>();

    @Override
    public void tick(OperativeHost host) {
        if (coreCooldown > 0) coreCooldown = Math.max(0, coreCooldown - OperativeGrowth.cooldownStep(host));
        int shockCapacity = shockCapacity();
        shocks = Math.min(shocks, shockCapacity);
        if (shocks < shockCapacity && (shockRecharge -= OperativeGrowth.cooldownStep(host)) <= 0) {
            shocks++; shockRecharge = Math.max(1, LunaStateManager.SHOCK_ARROW_RECHARGE_TICKS);
        }
        int grenadeCapacity = grenadeCapacity();
        grenades = Math.min(grenades, grenadeCapacity);
        if (grenades < grenadeCapacity && (grenadeRecharge -= OperativeGrowth.cooldownStep(host)) <= 0) {
            grenades++; grenadeRecharge = Math.max(1, LunaStateManager.COMPOSITE_GRENADE_RECHARGE_TICKS);
        }
        revealed.replaceAll((id, ticks) -> ticks - 1);
        revealed.values().removeIf(ticks -> ticks <= 0);
        if (!busy()) return;
        LivingEntity target = host.serverLevel().getEntity(preparedTarget) instanceof LivingEntity living ? living : null;
        if (!valid(host, target, 2, prepared == SkillSlot.CORE ? 80 : 40)) {
            cancel(host); return;
        }
        host.aimAt(target);
        if (prepared == SkillSlot.ACTIVE_2) {
            cookedTicks++;
            var shot = grenadeShot(host, target);
            int remaining = LunaStateManager.COMPOSITE_GRENADE_FUSE_TICKS - cookedTicks;
            // This is independent preparation, not a live grenade occupying the firing hand.
            // Wait out transiently unsafe lanes instead of aborting every time a target moves.
            if (!safeGrenade(host, shot)) {
                if (cookedTicks >= LunaStateManager.COMPOSITE_GRENADE_FUSE_TICKS) cancel(host);
                return;
            }
            windup = Math.max(0, remaining - shot.flightTicks() - 1);
            if (windup > 0) return;
        } else if (--windup > 0) return;
        SkillSlot releasing = prepared;
        prepared = null; preparedTarget = null;
        release(host, releasing, target);
    }

    @Override
    public double score(OperativeHost host, SkillSlot slot, LivingEntity target) {
        if (busy() || !valid(host, target, 2, slot == SkillSlot.CORE ? 80 : 40)) return 0;
        return switch (slot) {
            case ACTIVE_1 -> shocks > 0 && arrowShot(host, target) != null ? 80 : 0;
            case ACTIVE_2 -> grenades > 0 && safeGrenade(host, grenadeShot(host, target)) ? 120 : 0;
            case CORE -> coreCooldown == 0 && !isRevealed(target) && reconShot(host, target) != null ? 100 : 0;
            case PASSIVE -> 0;
        };
    }

    @Override
    public boolean start(OperativeHost host, SkillSlot slot, LivingEntity target) {
        if (score(host, slot, target) <= 0) return false;
        if (host.parallelNativeAi()) {
            release(host, slot, target);
            return true;
        }
        prepared = slot; preparedTarget = target.getUUID(); cookedTicks = 0;
        if (slot == SkillSlot.ACTIVE_2) {
            var shot = grenadeShot(host, target);
            windup = Math.max(1, LunaStateManager.COMPOSITE_GRENADE_FUSE_TICKS - shot.flightTicks() - 1);
            sound(host, ModSounds.LUNA_GRENADE_PIN.get());
        } else windup = Math.max(1, LunaStateManager.MAX_BOW_CHARGE_TICKS) + 2;
        host.aimAt(target);
        return true;
    }

    private void release(OperativeHost host, SkillSlot slot, LivingEntity target) {
        Projectile projectile;
        Vec3 direction;
        double speed, lift = 0, forward = 0.65D, down = 0.05D;
        switch (slot) {
            case ACTIVE_1 -> {
                var shot = arrowShot(host, target);
                if (shot == null || shocks <= 0) return;
                projectile = new LunaShockArrowEntity(ModEntities.LUNA_SHOCK_ARROW.get(), host.serverLevel(), host.body(), false);
                direction = shot.direction(); speed = 3.1D;
            }
            case ACTIVE_2 -> {
                var shot = grenadeShot(host, target);
                if (!safeGrenade(host, shot) || grenades <= 0) return;
                // Recompute the off-hand cook and launch together so a moving target cannot miss
                // a one-tick release window. The physical grenade is created only at release.
                int remainingFuse = Math.max(1, Math.min(LunaStateManager.COMPOSITE_GRENADE_FUSE_TICKS, shot.flightTicks() + 1));
                projectile = new LunaCompositeGrenadeEntity(ModEntities.LUNA_COMPOSITE_GRENADE.get(),
                        host.serverLevel(), host.body(), remainingFuse);
                direction = shot.direction(); speed = 1.55D; lift = 0.18D; forward = 0.7D; down = 0.12D;
            }
            case CORE -> {
                if (coreCooldown > 0) return;
                projectile = new LunaReconArrowEntity(ModEntities.LUNA_RECON_ARROW.get(), host.serverLevel(), host.body());
                var shot = reconShot(host, target); if (shot == null) return;
                direction = shot.direction(); speed = 3.3D;
            }
            default -> { return; }
        }
        Vec3 start = host.body().getEyePosition().add(direction.scale(forward)).add(0, -down, 0);
        projectile.setPos(start); projectile.setDeltaMovement(direction.scale(speed).add(0, lift, 0));
        if (!host.serverLevel().addFreshEntity(projectile)) return;
        switch (slot) {
            case ACTIVE_1 -> {
                if (shocks == shockCapacity())
                    shockRecharge = Math.max(1, LunaStateManager.SHOCK_ARROW_RECHARGE_TICKS);
                shocks--;
            }
            case ACTIVE_2 -> {
                if (grenades == grenadeCapacity())
                    grenadeRecharge = Math.max(1, LunaStateManager.COMPOSITE_GRENADE_RECHARGE_TICKS);
                grenades--;
            }
            case CORE -> coreCooldown = Math.max(1, LunaStateManager.RECON_ARROW_COOLDOWN_TICKS);
            default -> { }
        }
        sound(host, slot == SkillSlot.ACTIVE_2 ? ModSounds.LUNA_GRENADE_THROW.get() : ModSounds.LUNA_ARROW_RELEASE.get());
    }

    private static boolean valid(OperativeHost host, LivingEntity target, double min, double max) {
        if (target == null || !host.isEnemy(target) || !host.canSee(target)
                || !host.withinCombatRange(target.position()) || !host.withinOrders(host.body().position())) return false;
        double distance = host.body().distanceToSqr(target);
        return distance >= min * min && distance <= max * max;
    }

    private static OperativeBallistics.Shot reconShot(OperativeHost host, LivingEntity target) {
        return OperativeBallistics.direct(host, target, 3.3D, 0.65D, 0.05D, 0.025D, 0.995D, 100);
    }

    private static OperativeBallistics.Shot arrowShot(OperativeHost host, LivingEntity target) {
        return OperativeBallistics.direct(host, target, 3.1D, 0.65D, 0.05D, 0.035D, 0.99D, 30);
    }

    private static OperativeBallistics.Shot grenadeShot(OperativeHost host, LivingEntity target) {
        var shot = OperativeBallistics.solve(host, target.position().add(0, 0.1D, 0),
                1.55D, 0.18D, 0.7D, 0.12D, 0.045D, 0.986D, 55, 1.5D, true);
        return shot != null && shot.flightTicks() + 1 < LunaStateManager.COMPOSITE_GRENADE_FUSE_TICKS ? shot : null;
    }

    private static boolean safeGrenade(OperativeHost host, OperativeBallistics.Shot shot) {
        if (shot == null) return false;
        double radius = OperativeGrowth.damageRadius(host.body(), LunaCompositeGrenadeEntity.blastRadius(), "summons.lunacompositegrenadeentity") + 1;
        if (host.body().position().distanceToSqr(shot.impact()) <= radius * radius) return false;
        return host.serverLevel().getEntitiesOfClass(LivingEntity.class, new AABB(shot.impact(), shot.impact()).inflate(radius),
                candidate -> candidate != host.body() && candidate.isAlive() && !host.isEnemy(candidate)).isEmpty();
    }

    public void remember(LivingEntity target, int ticks) { revealed.merge(target.getUUID(), ticks, Math::max); }
    public boolean isRevealed(LivingEntity target) { return target != null && revealed.getOrDefault(target.getUUID(), 0) > 0; }

    @Override public boolean locksMovement() { return false; }
    @Override public boolean locksWeapon() { return prepared != null && prepared != SkillSlot.ACTIVE_2; }
    @Override public boolean busy() { return prepared != null; }
    @Override public void cancel(OperativeHost host) { prepared = null; preparedTarget = null; windup = 0; }

    @Override
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("ShockCharges", shocks); tag.putInt("ShockRecharge", shockRecharge);
        tag.putInt("GrenadeCharges", grenades); tag.putInt("GrenadeRecharge", grenadeRecharge);
        tag.putInt("CoreCooldown", coreCooldown);
        return tag;
    }

    @Override
    public void load(CompoundTag tag) {
        shocks = bounded(tag, "ShockCharges", shockCapacity());
        grenades = bounded(tag, "GrenadeCharges", grenadeCapacity());
        shockRecharge = bounded(tag, "ShockRecharge", LunaStateManager.SHOCK_ARROW_RECHARGE_TICKS);
        grenadeRecharge = bounded(tag, "GrenadeRecharge", LunaStateManager.COMPOSITE_GRENADE_RECHARGE_TICKS);
        coreCooldown = bounded(tag, "CoreCooldown", LunaStateManager.RECON_ARROW_COOLDOWN_TICKS);
        prepared = null; preparedTarget = null; windup = 0; revealed.clear();
    }

    private static int bounded(CompoundTag tag, String key, int max) { return Math.max(0, Math.min(Math.max(0, max), tag.getInt(key))); }
    private static void sound(OperativeHost host, net.minecraft.sounds.SoundEvent sound) {
        RangedSoundHelper.playThrottled(host.serverLevel(), host.body().position(), sound, SoundSource.NEUTRAL, 0.9F, 1.0F, 20, 4, 3);
    }
    @Override public String describe() {
        return "shock=" + shocks + ", shockRecharge=" + shockRecharge + ", grenades=" + grenades
                + ", grenadeRecharge=" + grenadeRecharge + ", core=" + coreCooldown + ", preparing=" + prepared + ", ticks=" + windup;
    }

    @Override public void refresh(OperativeHost host) {
        var fresh = new NativeLunaSkills(capacityScale);
        coreCooldown = fresh.coreCooldown;
        grenadeRecharge = fresh.grenadeRecharge;
        grenades = fresh.grenades;
        shockRecharge = fresh.shockRecharge;
        shocks = fresh.shocks;
    }
}
