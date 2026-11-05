package com.rzy.dealt_force_skills.ai.operative;

import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.character.SkillSlot;
import com.rzy.dealt_force_skills.character.morse.MorseStateManager;
import com.rzy.dealt_force_skills.entity.MorseFlashGrenadeEntity;
import com.rzy.dealt_force_skills.entity.MorseShockOrbEntity;
import com.rzy.dealt_force_skills.entity.MorseSonarDetectorEntity;
import com.rzy.dealt_force_skills.entity.OperativeEntity;
import com.rzy.dealt_force_skills.registry.ModEntities;
import com.rzy.dealt_force_skills.registry.ModSounds;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Real Morse devices; hearing stores an approximate place, never an enemy outline. */
public final class NativeMorseSkills implements OperativeSkills {
    private int shocks = MorseStateManager.SHOCK_MAX_CHARGES, shockRecharge;
    private int flashes = MorseStateManager.FLASH_MAX_CHARGES, flashRecharge;
    private int sonarCooldown, deployTicks, missingTicks;
    private UUID sonar;
    private Vec3 sonarPosition, deployStart, deployPosition, deployFacing;
    private final Map<UUID, Integer> revealed = new HashMap<>();
    private final Map<UUID, Long> heardAt = new HashMap<>();
    private Vec3 heardPosition;
    private int hearingTicks;

    @Override public void tick(OperativeHost host) {
        if (sonarCooldown > 0) sonarCooldown--;
        if (shocks < Math.max(0, MorseStateManager.SHOCK_MAX_CHARGES)) {
            if (--shockRecharge <= 0) { shocks++; shockRecharge = Math.max(1, MorseStateManager.SHOCK_RECHARGE_TICKS); }
        }
        if (flashes < Math.max(0, MorseStateManager.FLASH_MAX_CHARGES)) {
            if (--flashRecharge <= 0) { flashes++; flashRecharge = Math.max(1, MorseStateManager.FLASH_RECHARGE_TICKS); }
        }
        revealed.replaceAll((id, ticks) -> ticks - 1); revealed.values().removeIf(ticks -> ticks <= 0);
        long now = host.serverLevel().getGameTime();
        heardAt.values().removeIf(time -> now - time >= MorseStateManager.nativeSoundCooldown());
        if (hearingTicks > 0 && --hearingTicks == 0) heardPosition = null;
        if (!available(host)) { cancel(host); return; }
        if (sonar != null) {
            var device = host.serverLevel().getEntity(sonar);
            if (device instanceof MorseSonarDetectorEntity detector && detector.isNativeOwnedBy(host.body())) missingTicks = 0;
            else if (sonarPosition == null || host.serverLevel().hasChunkAt(net.minecraft.core.BlockPos.containing(sonarPosition))) {
                // Allow owner and gadget to register in either order after a chunk reload.
                if (++missingTicks > 40) finishSonar(sonar);
            }
        }
        if (deployPosition == null) return;
        if (!host.parallelNativeAi() && host.body().position().distanceToSqr(deployStart) > 0.36D
                || !safeDeploy(host, deployPosition) || blocked(host, SkillSlot.CORE)) { clearDeploy(); return; }
        if (++deployTicks < Math.max(1, MorseStateManager.SONAR_DEPLOY_TICKS)) return;
        var device = new MorseSonarDetectorEntity(ModEntities.MORSE_SONAR_DETECTOR.get(), host.serverLevel(), host.body(), deployFacing);
        device.setPos(deployPosition);
        if (host.serverLevel().addFreshEntity(device)) {
            sonar = device.getUUID(); sonarPosition = deployPosition; missingTicks = 0;
            sound(host, ModSounds.MORSE_SONAR_DEPLOY_COMPLETE.get());
        }
        clearDeploy();
    }

    public static boolean available(OperativeHost host) { return NativeRaptorSkills.available(host); }
    private static boolean blocked(OperativeHost host, SkillSlot slot) { return OperativeElectronics.blocked(host, ModCharacters.MORSE_ID, slot); }
    private static boolean valid(OperativeHost host, LivingEntity target) {
        return target != null && target.level() == host.body().level() && host.isEnemy(target)
                && host.withinCombatRange(target.position()) && host.body().distanceToSqr(target) <= MorseSonarDetectorEntity.RANGE * MorseSonarDetectorEntity.RANGE;
    }
    @Override public double score(OperativeHost host, SkillSlot slot, LivingEntity target) {
        if (!available(host) || busy() || !valid(host, target) || blocked(host, slot)) return 0;
        return switch (slot) {
            case CORE -> sonar == null && sonarCooldown == 0 && (host.canSee(target) || isRevealed(target)) && deployment(host, target) != null ? 100 : 0;
            case ACTIVE_1 -> shocks > 0 && host.canSee(target) && !target.hasEffect(com.rzy.dealt_force_skills.registry.ModEffects.MORSE_STRONG_SHOCK.get())
                    && shockPlan(host, target) != null ? 90 : 0;
            case ACTIVE_2 -> flashes > 0 && host.body().getHealth() > 1 && host.canSee(target) && !target.hasEffect(com.rzy.dealt_force_skills.registry.ModEffects.MORSE_FLASH_BLIND.get())
                    && flashPlan(host, target) != null ? 110 : 0;
            case PASSIVE -> 0;
        };
    }
    @Override public boolean start(OperativeHost host, SkillSlot slot, LivingEntity target) {
        if (score(host, slot, target) <= 0) return false;
        host.aimAt(target);
        switch (slot) {
            case CORE -> {
                deployPosition = deployment(host, target); deployStart = host.body().position();
                deployFacing = target.position().subtract(deployPosition); deployTicks = 0;
                if (!host.parallelNativeAi() && host.body() instanceof net.minecraft.world.entity.Mob mob) mob.getNavigation().stop();
            }
            case ACTIVE_1 -> {
                var plan = shockPlan(host, target); if (plan == null) return false;
                var orb = new MorseShockOrbEntity(ModEntities.MORSE_SHOCK_ORB.get(), host.serverLevel(), host.body());
                orb.setPos(plan.start()); orb.setDeltaMovement(plan.motion());
                if (!host.serverLevel().addFreshEntity(orb)) return false;
                if (shocks == Math.max(0, MorseStateManager.SHOCK_MAX_CHARGES)) shockRecharge = Math.max(1, MorseStateManager.SHOCK_RECHARGE_TICKS);
                shocks--; sound(host, ModSounds.MORSE_SHOCK_ORB_THROW.get());
            }
            case ACTIVE_2 -> {
                var plan = flashPlan(host, target); if (plan == null) return false;
                var flash = new MorseFlashGrenadeEntity(ModEntities.MORSE_FLASH_GRENADE.get(), host.serverLevel(), host.body());
                flash.setPos(plan.start()); flash.setDeltaMovement(plan.motion()); flash.setNativeFuse(plan.fuse());
                if (!host.serverLevel().addFreshEntity(flash)) return false;
                if (flashes == Math.max(0, MorseStateManager.FLASH_MAX_CHARGES)) flashRecharge = Math.max(1, MorseStateManager.FLASH_RECHARGE_TICKS);
                flashes--; sound(host, ModSounds.MORSE_FLASH_THROW.get());
            }
            default -> { return false; }
        }
        return true;
    }

    public record ThrowPlan(Vec3 start, Vec3 motion, Vec3 impact, int fuse) { }
    public static ThrowPlan shockPlan(OperativeHost host, LivingEntity target) {
        var shot = OperativeBallistics.direct(host, target, 1.45D, 0.18D, 0.7D, 0.12D, 0.045D, 0.985D, 200);
        if (shot == null || !safeArea(host, target.getBoundingBox().getCenter(), MorseShockOrbEntity.nativeRadius() + 1, false)) return null;
        Vec3 start = host.body().getEyePosition().add(shot.direction().scale(0.7D)).add(0, -0.12D, 0);
        if (!clearStart(host, start)) return null;
        return new ThrowPlan(start, shot.direction().scale(1.45D).add(0, 0.18D, 0), shot.impact(), 0);
    }
    public static ThrowPlan flashPlan(OperativeHost host, LivingEntity target) {
        var shot = OperativeFlashTactics.shot(host, target, MorseFlashGrenadeEntity.nativeRadius() + 1);
        if (shot == null) return null;
        Vec3 start = host.body().getEyePosition().add(shot.direction().scale(0.58D)).add(0, -0.1D, 0);
        return new ThrowPlan(start, shot.direction().scale(1.55D).add(0, 0.28D, 0), shot.impact(), shot.flightTicks() + 1);
    }
    private static boolean clearStart(OperativeHost host, Vec3 start) {
        return host.serverLevel().clip(new ClipContext(host.body().getEyePosition(), start, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, host.body())).getType() == HitResult.Type.MISS;
    }
    private static boolean safeArea(OperativeHost host, Vec3 impact, double radius, boolean selfHarm) {
        if (selfHarm && host.body().getBoundingBox().getCenter().distanceToSqr(impact) <= radius * radius) return false;
        return host.serverLevel().getEntitiesOfClass(LivingEntity.class, new AABB(impact, impact).inflate(radius),
                entity -> entity != host.body() && entity.isAlive() && !host.isEnemy(entity)
                        && entity.getBoundingBox().getCenter().distanceToSqr(impact) <= radius * radius).isEmpty();
    }
    private static Vec3 deployment(OperativeHost host, LivingEntity target) {
        Vec3 direction = target.position().subtract(host.body().position());
        direction = new Vec3(direction.x, 0, direction.z).normalize();
        Vec3 start = host.body().position().add(direction.scale(1.2D)).add(0, 1, 0);
        var hit = host.serverLevel().clip(new ClipContext(start, start.add(0, -3, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, host.body()));
        if (hit.getType() != HitResult.Type.BLOCK || hit.getDirection() != Direction.UP) return null;
        Vec3 place = hit.getLocation().add(0, 0.05D, 0);
        return safeDeploy(host, place) && clearStart(host, place.add(0, 0.2D, 0)) ? place : null;
    }
    private static boolean safeDeploy(OperativeHost host, Vec3 place) {
        return (host.parallelNativeAi() || host.withinOrders(place)) && host.serverLevel().hasChunkAt(net.minecraft.core.BlockPos.containing(place))
                && host.serverLevel().noCollision(new AABB(place.add(-0.2D, 0, -0.2D), place.add(0.2D, 0.4D, 0.2D)))
                && host.serverLevel().getBlockState(net.minecraft.core.BlockPos.containing(place.add(0, -0.1D, 0))).isFaceSturdy(
                host.serverLevel(), net.minecraft.core.BlockPos.containing(place.add(0, -0.1D, 0)), Direction.UP);
    }
    /** Look away during the last three fuse ticks; base self damage and minimum blindness still apply. */
    public static void turnAwayFromFlash(LivingEntity owner, Vec3 flash) {
        if (!(owner instanceof OperativeHost bot) || !(com.rzy.dealt_force_skills.ai.operative.NativeCommanderSkills.inherited(bot.skills(), NativeMorseSkills.class) instanceof NativeMorseSkills)
                || !available(bot) || owner.distanceToSqr(flash) > Math.pow(MorseFlashGrenadeEntity.nativeRadius(), 2)) return;
        Vec3 away = owner.getEyePosition().subtract(flash.add(0, 0.65D, 0));
        float yaw = (float) (Math.toDegrees(Math.atan2(away.z, away.x)) - 90);
        owner.setYRot(yaw); owner.setYHeadRot(yaw);
        owner.setXRot((float) -Math.toDegrees(Math.atan2(away.y, away.horizontalDistance())));
    }
    public boolean ownsSonar(UUID id) { return id != null && id.equals(sonar); }
    public void finishSonar(UUID id) {
        if (!ownsSonar(id)) return;
        sonar = null; sonarPosition = null; missingTicks = 0;
        sonarCooldown = Math.max(1, MorseStateManager.SONAR_COOLDOWN_TICKS);
    }
    public void hear(OperativeHost host, LivingEntity source, Vec3 approximatePosition) {
        if (!available(host) || !host.isEnemy(source) || !host.withinOrders(approximatePosition)) return;
        long now = host.serverLevel().getGameTime();
        Long last = heardAt.get(source.getUUID());
        if (last != null && now - last < MorseStateManager.nativeSoundCooldown()) return;
        heardAt.put(source.getUUID(), now); heardPosition = approximatePosition;
        hearingTicks = Math.max(1, MorseStateManager.nativeSoundTicks());
    }
    @Override public Vec3 investigation() { return hearingTicks > 0 ? heardPosition : null; }
    @Override public void remember(LivingEntity target, int ticks) { if (ticks > 0) revealed.merge(target.getUUID(), ticks, Math::max); }
    @Override public boolean isRevealed(LivingEntity target) { return target != null && revealed.getOrDefault(target.getUUID(), 0) > 0; }
    @Override public boolean locksMovement() { return deployPosition != null; }
    @Override public boolean busy() { return deployPosition != null; }
    private void clearDeploy() { deployStart = deployPosition = deployFacing = null; deployTicks = 0; }
    @Override public void cancel(OperativeHost host) {
        clearDeploy(); heardAt.clear(); heardPosition = null; hearingTicks = 0;
        if (sonar != null) {
            UUID id = sonar;
            if (host.serverLevel().getEntity(id) instanceof MorseSonarDetectorEntity detector && detector.isNativeOwnedBy(host.body())) detector.discard();
            finishSonar(id);
        }
    }
    @Override public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("ShockCharges", shocks); tag.putInt("ShockRecharge", shockRecharge);
        tag.putInt("FlashCharges", flashes); tag.putInt("FlashRecharge", flashRecharge); tag.putInt("SonarCooldown", sonarCooldown);
        if (sonar != null) tag.putUUID("Sonar", sonar);
        if (sonarPosition != null) { tag.putDouble("SonarX", sonarPosition.x); tag.putDouble("SonarY", sonarPosition.y); tag.putDouble("SonarZ", sonarPosition.z); }
        return tag;
    }
    @Override public void load(CompoundTag tag) {
        shocks = bounded(tag, "ShockCharges", MorseStateManager.SHOCK_MAX_CHARGES); shockRecharge = bounded(tag, "ShockRecharge", MorseStateManager.SHOCK_RECHARGE_TICKS);
        flashes = bounded(tag, "FlashCharges", MorseStateManager.FLASH_MAX_CHARGES); flashRecharge = bounded(tag, "FlashRecharge", MorseStateManager.FLASH_RECHARGE_TICKS);
        sonarCooldown = bounded(tag, "SonarCooldown", MorseStateManager.SONAR_COOLDOWN_TICKS);
        sonar = tag.hasUUID("Sonar") ? tag.getUUID("Sonar") : null;
        sonarPosition = sonar != null && tag.contains("SonarX") ? new Vec3(tag.getDouble("SonarX"), tag.getDouble("SonarY"), tag.getDouble("SonarZ")) : null;
        clearDeploy(); missingTicks = 0; revealed.clear(); heardAt.clear(); heardPosition = null; hearingTicks = 0;
    }
    private static int bounded(CompoundTag tag, String key, int max) { return Math.max(0, Math.min(Math.max(0, max), tag.getInt(key))); }
    private static void sound(OperativeHost host, net.minecraft.sounds.SoundEvent sound) { host.serverLevel().playSound(null, host.body().blockPosition(), sound, SoundSource.NEUTRAL, 0.75F, 1); }
    @Override public String describe() { return "shock=" + shocks + ", flash=" + flashes + ", sonar=" + sonarCooldown + ", deploying=" + deployTicks + ", device=" + sonar; }

    @Override public void refresh(OperativeHost host) {
        var fresh = new NativeMorseSkills();
        flashRecharge = fresh.flashRecharge;
        flashes = fresh.flashes;
        shockRecharge = fresh.shockRecharge;
        shocks = fresh.shocks;
        sonarCooldown = fresh.sonarCooldown;
    }
}
