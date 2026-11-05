package com.rzy.dealt_force_skills.ai.operative;

import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.character.SkillSlot;
import com.rzy.dealt_force_skills.character.ntwo.NTwoStateManager;
import com.rzy.dealt_force_skills.entity.*;
import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.registry.ModEntities;
import com.rzy.dealt_force_skills.registry.ModSounds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Native tracking, discrete canister arcs and the real six-shot condenser launcher. */
public final class NativeNTwoSkills implements OperativeSkills {
    private int trackingCooldown, dewarCharges = NTwoStateManager.DEWAR_MAX_CHARGES, dewarRecharge;
    private int coreCooldown, coreTicks, ammo, shotDelay;
    private UUID coreTarget;
    private OperativeBallistics.Shot detachedShot;
    private net.minecraft.world.phys.Vec3 detachedOrigin;
    private final Map<UUID, Integer> revealed = new HashMap<>();

    private static boolean blocked(OperativeHost host, SkillSlot slot) {
        return OperativeElectronics.blocked(host, ModCharacters.N_TWO_ID, slot);
    }
    private static boolean valid(OperativeHost host, LivingEntity target) {
        return target != null && target.isAlive() && target.level() == host.body().level()
                && host.isEnemy(target) && host.withinCombatRange(target.position())
                && host.body().distanceToSqr(target) <= 24 * 24 && host.canSee(target);
    }
    private static OperativeBallistics.Shot floorShot(OperativeHost host, LivingEntity target, boolean core) {
        if (!valid(host, target)) return null;
        var shot = OperativeBallistics.solve(host, target.position(), core ? 3.25 : 1.1, core ? 0.18 : 0.22,
                core ? 0.75 : 0.7, 0.12, core ? 0.012 : 0.045, core ? 0.996 : 0.986,
                core ? 180 : 240, core ? 2 : 4, true);
        double safe = core ? 4 : 7;
        return shot != null && host.body().position().distanceToSqr(shot.impact()) > safe * safe ? shot : null;
    }
    private static OperativeBallistics.Shot trackingShot(OperativeHost host, LivingEntity target) {
        return valid(host, target) ? OperativeBallistics.direct(host, target, 0.72, 0.7, 0.12, 0, 1, 240) : null;
    }
    private boolean covered(OperativeHost host, LivingEntity target) {
        return !host.serverLevel().getEntitiesOfClass(NTwoIceFieldEntity.class, target.getBoundingBox().inflate(6),
                field -> field.isNativeOwnedBy(host.body()) && field.covers(target)).isEmpty()
                || !host.serverLevel().getEntitiesOfClass(NTwoDewarCanisterEntity.class, host.body().getBoundingBox().inflate(32),
                    canister -> canister.isNativeOwnedBy(host.body())).isEmpty();
    }
    @Override public void tick(OperativeHost host) {
        if (trackingCooldown > 0) trackingCooldown--;
        if (coreCooldown > 0) coreCooldown--;
        if (shotDelay > 0) shotDelay--;
        if (dewarCharges < NTwoStateManager.DEWAR_MAX_CHARGES && --dewarRecharge <= 0) {
            dewarCharges++; dewarRecharge = NTwoStateManager.DEWAR_RECHARGE_TICKS;
        }
        revealed.replaceAll((id, ticks) -> ticks - 1); revealed.values().removeIf(ticks -> ticks <= 0);
        if (!NativeRaptorSkills.available(host) || blocked(host, SkillSlot.CORE)) { cancel(host); return; }
        if (coreTicks <= 0) return;
        if (--coreTicks <= 0 || ammo <= 0) { endCore(); return; }
        LivingEntity target = coreTarget != null && host.serverLevel().getEntity(coreTarget) instanceof LivingEntity living ? living : null;
        if (!(host.parallelNativeAi() ? NativeRaptorSkills.committedTarget(host, target) : valid(host, target))) {
            target = host.body() instanceof net.minecraft.world.entity.Mob mob ? mob.getTarget() : null;
            if (!valid(host, target)) { endCore(); return; }
            coreTarget = target.getUUID();
        }
        if (shotDelay > 0) return;
        var shot = detachedShot != null ? detachedShot : floorShot(host, target, true);
        if (shot == null) { endCore(); return; }
        host.aimAt(target);
        var grenade = new NTwoCondensedGrenadeEntity(ModEntities.N_TWO_CONDENSED_GRENADE.get(), host.serverLevel(), host.body());
        grenade.setPos((detachedOrigin != null ? detachedOrigin : host.body().getEyePosition()).add(shot.direction().scale(0.75)).add(0, -0.12, 0));
        grenade.setDeltaMovement(shot.direction().scale(3.25).add(0, 0.18, 0));
        if (!host.serverLevel().addFreshEntity(grenade)) { endCore(); return; }
        shotDelay = NTwoStateManager.CORE_SHOT_INTERVAL_TICKS; ammo--;
        sound(host, ModSounds.N_TWO_CONDENSED_FIRE.get());
        if (ammo <= 0) endCore();
    }
    @Override public double score(OperativeHost host, SkillSlot slot, LivingEntity target) {
        if (!NativeRaptorSkills.available(host) || busy() || blocked(host, slot) || !valid(host, target)) return 0;
        return switch (slot) {
            case ACTIVE_1 -> trackingCooldown == 0 && !target.hasEffect(ModEffects.N_TWO_DISRUPTED) && trackingShot(host, target) != null ? 100 : 0;
            case ACTIVE_2 -> dewarCharges > 0 && !covered(host, target) && floorShot(host, target, false) != null ? 110 : 0;
            case CORE -> coreCooldown == 0 && floorShot(host, target, true) != null ? 120 : 0;
            case PASSIVE -> 0;
        };
    }
    @Override public boolean start(OperativeHost host, SkillSlot slot, LivingEntity target) {
        if (score(host, slot, target) <= 0) return false;
        host.aimAt(target);
        if (slot == SkillSlot.CORE) {
            detachedShot = host.parallelNativeAi() ? floorShot(host, target, true) : null;
            detachedOrigin = detachedShot != null ? host.body().getEyePosition() : null;
            coreTicks = NTwoStateManager.CORE_ACTIVE_TICKS;
            ammo = OperativeGrowth.commander(host.body()) ? (int)Math.ceil(18 * OperativeGrowth.commanderEffect()) : NTwoStateManager.CORE_MAX_AMMO;
            shotDelay = 0; coreTarget = target.getUUID(); sound(host, ModSounds.N_TWO_CONDENSED_LAUNCHER.get()); return true;
        }
        var shot = slot == SkillSlot.ACTIVE_1 ? trackingShot(host, target) : floorShot(host, target, false);
        if (shot == null) return false;
        net.minecraft.world.entity.projectile.Projectile projectile;
        if (slot == SkillSlot.ACTIVE_1) {
            projectile = new NTwoTrackingStunGrenadeEntity(ModEntities.N_TWO_TRACKING_GRENADE.get(), host.serverLevel(), host.body());
            projectile.setDeltaMovement(shot.direction().scale(0.72));
        } else {
            projectile = new NTwoDewarCanisterEntity(ModEntities.N_TWO_DEWAR_CANISTER.get(), host.serverLevel(), host.body());
            projectile.setDeltaMovement(shot.direction().scale(1.1).add(0, 0.22, 0));
        }
        projectile.setPos(host.body().getEyePosition().add(shot.direction().scale(0.7)).add(0, -0.12, 0));
        if (!host.serverLevel().addFreshEntity(projectile)) return false;
        if (slot == SkillSlot.ACTIVE_1) trackingCooldown = NTwoStateManager.TRACKING_GRENADE_COOLDOWN_TICKS;
        else { if (dewarCharges == NTwoStateManager.DEWAR_MAX_CHARGES) dewarRecharge = NTwoStateManager.DEWAR_RECHARGE_TICKS; dewarCharges--; }
        sound(host, slot == SkillSlot.ACTIVE_1 ? ModSounds.N_TWO_TRACKING_STUN_THROW.get() : ModSounds.N_TWO_DEWAR_THROW.get());
        return true;
    }
    private void endCore() {
        if (coreTicks > 0 || ammo > 0) coreCooldown = NTwoStateManager.CORE_COOLDOWN_TICKS;
        coreTicks = ammo = shotDelay = 0; coreTarget = null; detachedShot = null; detachedOrigin = null;
    }
    @Override public boolean busy() { return coreTicks > 0; }
    @Override public void cancel(OperativeHost host) { endCore(); }
    @Override public void remember(LivingEntity target, int ticks) { if (ticks > 0) revealed.merge(target.getUUID(), ticks, Math::max); }
    @Override public boolean isRevealed(LivingEntity target) { return target != null && revealed.getOrDefault(target.getUUID(), 0) > 0; }
    @Override public CompoundTag save() {
        var tag = new CompoundTag();
        tag.putInt("TrackingCooldown", trackingCooldown); tag.putInt("DewarCharges", dewarCharges); tag.putInt("DewarRecharge", dewarRecharge);
        tag.putInt("CoreCooldown", coreCooldown); tag.putInt("CoreTicks", coreTicks); tag.putInt("Ammo", ammo); tag.putInt("ShotDelay", shotDelay);
        if (coreTarget != null) tag.putUUID("CoreTarget", coreTarget); return tag;
    }
    @Override public void load(CompoundTag tag) {
        detachedShot = null; detachedOrigin = null;
        trackingCooldown = bound(tag, "TrackingCooldown", NTwoStateManager.TRACKING_GRENADE_COOLDOWN_TICKS);
        dewarCharges = bound(tag, "DewarCharges", NTwoStateManager.DEWAR_MAX_CHARGES); dewarRecharge = bound(tag, "DewarRecharge", NTwoStateManager.DEWAR_RECHARGE_TICKS);
        coreCooldown = bound(tag, "CoreCooldown", NTwoStateManager.CORE_COOLDOWN_TICKS);
        coreTicks = bound(tag, "CoreTicks", NTwoStateManager.CORE_ACTIVE_TICKS); ammo = bound(tag, "Ammo", (int)Math.ceil(18 * OperativeGrowth.commanderEffect()));
        shotDelay = bound(tag, "ShotDelay", NTwoStateManager.CORE_SHOT_INTERVAL_TICKS);
        coreTarget = tag.hasUUID("CoreTarget") ? tag.getUUID("CoreTarget") : null;
        if (coreTicks <= 0 || ammo <= 0 || coreTarget == null) endCore(); revealed.clear();
    }
    private static int bound(CompoundTag tag, String key, int max) { return Math.max(0, Math.min(max, tag.getInt(key))); }
    private static void sound(OperativeHost host, net.minecraft.sounds.SoundEvent sound) {
        host.serverLevel().playSound(null, host.body().blockPosition(), sound, net.minecraft.sounds.SoundSource.NEUTRAL, 0.8F, 1);
    }
    @Override public String describe() { return "tracking="+trackingCooldown+", dewar="+dewarCharges+", core="+coreTicks+", ammo="+ammo+", cooldown="+coreCooldown; }
    @Override public void refresh(OperativeHost host) {
        var fresh = new NativeNTwoSkills();
        coreCooldown = fresh.coreCooldown;
        dewarCharges = fresh.dewarCharges;
        dewarRecharge = fresh.dewarRecharge;
        trackingCooldown = fresh.trackingCooldown;
    }
    @Override public boolean locksMovement() { return false; }
}
