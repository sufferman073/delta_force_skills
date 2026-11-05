package com.rzy.dealt_force_skills.ai.operative;

import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.character.SkillSlot;
import com.rzy.dealt_force_skills.character.nox.NoxStateManager;
import com.rzy.dealt_force_skills.effect.NoxDelayedWoundEffect;
import com.rzy.dealt_force_skills.entity.NoxDecoyEntity;
import com.rzy.dealt_force_skills.entity.NoxFlashGrenadeEntity;
import com.rzy.dealt_force_skills.entity.NoxRotorDroneEntity;
import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.registry.ModEntities;
import com.rzy.dealt_force_skills.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

/** Unupgraded Nox, using the real rotor, directional flash and stealth projections. */
public final class NativeNoxSkills implements OperativeSkills {
    private int rotorCooldown, flash = Math.max(0, NoxStateManager.FLASH_MAX_CHARGES), flashRecharge;
    private int coreCooldown, preparation, stealth, nextDecoy, reducedDamage;
    private boolean runtime, previousSilent, previousInvisible;
    private final java.util.Set<java.util.UUID> warned = new java.util.HashSet<>();

    @Override public void tick(OperativeHost host) {
        if (rotorCooldown > 0) rotorCooldown--;
        if (coreCooldown > 0) coreCooldown--;
        if (reducedDamage > 0) reducedDamage--;
        if (flash >= Math.max(0, NoxStateManager.FLASH_MAX_CHARGES)) flashRecharge = 0;
        else if (flashRecharge <= 0) flashRecharge = Math.max(1, NoxStateManager.FLASH_RECHARGE_TICKS);
        else if (--flashRecharge == 0) flash++;
        if (!host.body().isAlive() || !host.withinOrders(host.body().position())
                || host.body().hasEffect(ModEffects.STUN.get()) || host.body().hasEffect(ModEffects.WEBBED.get())) {
            cancel(host); return;
        }
        if (preparation > 0 && --preparation == 0) {
            stealth = Math.max(1, NoxStateManager.CORE_DURATION_TICKS);
            nextDecoy = 1;
            applyRuntime(host);
        } else if (stealth > 0 && --stealth == 0) endStealth(host);
        if (stealth <= 0) {
            if (runtime) endStealth(host);
            return;
        }
        applyRuntime(host);
        warnNearby(host);
        for (Mob mob : host.serverLevel().getEntitiesOfClass(Mob.class, host.body().getBoundingBox().inflate(32), Mob::isAlive))
            if (mob.getTarget() == host.body()) mob.setTarget(null);
        if (--nextDecoy <= 0) {
            spawnDecoy(host);
            nextDecoy = Math.max(1, NoxStateManager.DECOY_INTERVAL_TICKS);
        }
        if (host.serverLevel().getGameTime() % NoxStateManager.STEALTH_PARTICLE_INTERVAL_TICKS == 0)
            host.serverLevel().sendParticles(ParticleTypes.SMOKE, host.body().getX(), host.body().getY() + 1,
                    host.body().getZ(), 8, 0.32D, 0.55D, 0.32D, 0.01D);
    }

    private static boolean allowed(OperativeHost host, LivingEntity target) {
        return host.body().isAlive() && !host.body().isRemoved() && host.withinOrders(host.body().position())
                && !host.body().hasEffect(ModEffects.STUN.get()) && !host.body().hasEffect(ModEffects.WEBBED.get())
                && target != null && host.isEnemy(target) && host.withinCombatRange(target.position())
                && host.canSee(target) && host.body().distanceToSqr(target) <= 24 * 24;
    }
    @Override public double score(OperativeHost host, SkillSlot slot, LivingEntity target) {
        if (preparation > 0 || stealth > 0 || !allowed(host, target)
                || OperativeElectronics.blocked(host, ModCharacters.NOX_ID, slot)) return 0;
        return switch (slot) {
            case ACTIVE_1 -> rotorCooldown == 0 && rotorShot(host, target) != null ? 100 : 0;
            case ACTIVE_2 -> flash > 0 && !target.hasEffect(ModEffects.NOX_FLASHED.get())
                    && flashShot(host, target) != null ? 110 : 0;
            case CORE -> coreCooldown == 0 && host.body().distanceToSqr(target) > 5
                    && (rotorCooldown > 0 || host.body().getHealth() < host.body().getMaxHealth() * 0.6F) ? 95 : 0;
            default -> 0;
        };
    }
    @Override public boolean start(OperativeHost host, SkillSlot slot, LivingEntity target) {
        if (slot == SkillSlot.PASSIVE || preparation > 0 || !allowed(host, target)
                || OperativeElectronics.blocked(host, ModCharacters.NOX_ID, slot)) return false;
        if (stealth > 0) { endStealth(host); return true; }
        if (score(host, slot, target) <= 0) return false;
        host.aimAt(target);
        if (slot == SkillSlot.CORE) {
            preparation = Math.max(1, NoxStateManager.CORE_PREP_TICKS);
            host.serverLevel().playSound(null, host.body().blockPosition(), ModSounds.NOX_STEALTH_START.get(), SoundSource.NEUTRAL, 0.8F, 1);
            return true;
        }
        if (slot == SkillSlot.ACTIVE_1) {
            var shot = rotorShot(host, target);
            if (shot == null) return false;
            Vec3 p = host.body().getEyePosition().add(shot.direction().scale(0.72D)).add(0, -0.08D, 0);
            var rotor = new NoxRotorDroneEntity(ModEntities.NOX_ROTOR_DRONE.get(), host.serverLevel(), host.body(), target.getUUID());
            rotor.setPos(p); rotor.setDeltaMovement(shot.direction().scale(1.75D));
            if (!host.serverLevel().addFreshEntity(rotor)) return false;
            rotorCooldown = Math.max(1, NoxStateManager.ROTOR_COOLDOWN_TICKS);
            host.serverLevel().playSound(null, host.body().blockPosition(), ModSounds.NOX_ROTOR_THROW.get(), SoundSource.NEUTRAL, 0.95F, 0.92F);
            return true;
        }
        var shot = flashShot(host, target);
        if (shot == null) return false;
        Vec3 p = host.body().getEyePosition().add(shot.direction().scale(0.58D)).add(0, -0.1D, 0);
        var grenade = new NoxFlashGrenadeEntity(ModEntities.NOX_FLASH_GRENADE.get(), host.serverLevel(), host.body());
        grenade.setPos(p); grenade.setDeltaMovement(shot.direction().scale(1.55D).add(0, 0.28D, 0));
        grenade.setNativeFuse(shot.flightTicks() + 1);
        if (!host.serverLevel().addFreshEntity(grenade)) return false;
        flash--; if (flashRecharge == 0) flashRecharge = Math.max(1, NoxStateManager.FLASH_RECHARGE_TICKS);
        host.serverLevel().playSound(null, host.body().blockPosition(), ModSounds.NOX_FLASH_THROW.get(), SoundSource.NEUTRAL, 0.95F, 0.95F);
        return true;
    }

    private static OperativeBallistics.Shot rotorShot(OperativeHost host, LivingEntity target) {
        if (host.body().distanceToSqr(target) < 9) return null;
        return OperativeBallistics.direct(host, target, 1.75D, 0.72D, 0.08D, 0, 0.995D, 40);
    }
    private static OperativeBallistics.Shot flashShot(OperativeHost host, LivingEntity target) {
        if (host.body().distanceToSqr(target) < 9) return null;
        return OperativeFlashTactics.shot(host, target, 25);
    }
    public boolean stealthed() { return stealth > 0; }
    private void warnNearby(OperativeHost host) {
        var nearby = new java.util.HashSet<java.util.UUID>();
        double range = Math.max(0, NoxStateManager.STEALTH_WARNING_RANGE);
        for (var player : host.serverLevel().players()) {
            if (!com.rzy.dealt_force_skills.util.TargetingUtil.isTargetablePlayer(player)
                    || player.distanceToSqr(host.body()) > range * range) continue;
            nearby.add(player.getUUID());
            if (warned.add(player.getUUID())) host.serverLevel().playSound(null, player.blockPosition(),
                    ModSounds.NOX_STEALTH_WARNING.get(), SoundSource.PLAYERS, 0.7F, 1);
            if (host.serverLevel().getGameTime() % 40 == 0) player.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable("message.dealt_force_skills.nox.stealth_nearby"), true);
        }
        warned.retainAll(nearby);
    }
    public float outgoing(OperativeHost host, float amount) {
        if (amount <= 0) return amount;
        if (OperativeGrowth.commander(host.body())) return amount;
        boolean reduced = stealth > 0 || reducedDamage > 0;
        if (stealth > 0) endStealth(host);
        return reduced ? amount * 0.5F : amount;
    }
    public static void wound(OperativeHost host, LivingEntity target, float amount) {
        if (amount <= 0 || !host.isEnemy(target) || !host.withinCombatRange(target.position())) return;
        if (OperativeGrowth.commander(host.body()))
            for (var effect : java.util.List.copyOf(target.getActiveEffects()))
                if (effect.getEffect().getCategory() == net.minecraft.world.effect.MobEffectCategory.BENEFICIAL)
                    target.removeEffect(effect.getEffect());
        boolean applied = target.addEffect(new MobEffectInstance(ModEffects.NOX_DELAYED_WOUND.get(),
                Math.max(1, NoxStateManager.DELAYED_WOUND_TICKS), 0, false, true, true), host.body());
        if (applied || target.hasEffect(ModEffects.NOX_DELAYED_WOUND.get()))
            NoxDelayedWoundEffect.resetCaps(target, target.getHealth() - amount);
    }
    private void applyRuntime(OperativeHost host) {
        if (!runtime) {
            previousSilent = host.body().isSilent(); previousInvisible = host.body().isInvisible(); runtime = true;
        }
        var speed = host.body().getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null && speed.getModifier(NoxStateManager.STEALTH_SPEED_UUID) == null)
            speed.addTransientModifier(new AttributeModifier(NoxStateManager.STEALTH_SPEED_UUID, "nox_stealth_speed", 0.25D, AttributeModifier.Operation.MULTIPLY_TOTAL));
        host.body().setSilent(true); host.body().setInvisible(true);
        host.body().addEffect(new MobEffectInstance(ModEffects.NOX_STEALTH.get(), 40, 0, false, false, false));
    }
    private void endStealth(OperativeHost host) {
        if (stealth > 0 || runtime) {
            coreCooldown = Math.max(1, NoxStateManager.CORE_COOLDOWN_TICKS); reducedDamage = 20;
        }
        preparation = 0; stealth = 0; nextDecoy = 0;
        var speed = host.body().getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.removeModifier(NoxStateManager.STEALTH_SPEED_UUID);
        if (runtime) {
            host.body().setSilent(previousSilent); host.body().setInvisible(previousInvisible);
            host.body().removeEffect(ModEffects.NOX_STEALTH.get()); runtime = false;
        }
        for (var decoy : decoys(host)) decoy.discard();
        warned.clear();
    }
    private static java.util.List<NoxDecoyEntity> decoys(OperativeHost host) {
        return host.serverLevel().getEntitiesOfClass(NoxDecoyEntity.class, host.body().getBoundingBox().inflate(128),
                e -> !e.isRemoved() && host.body().getUUID().equals(e.ownerUuid()));
    }
    private void spawnDecoy(OperativeHost host) {
        int limit = Math.max(0, Math.min(16, NoxStateManager.MAX_ACTIVE_DECOYS));
        if (limit == 0) return;
        var active = decoys(host);
        active.sort(java.util.Comparator.comparingInt(e -> -e.nativeAge()));
        for (int i = 0; i <= active.size() - limit; i++) active.get(i).discard();
        Vec3 look = host.body().getLookAngle().multiply(1, 0, 1).normalize();
        Vec3 p = host.body().position().subtract(look.scale(1.35D));
        if (!host.withinOrders(p)) return;
        var decoy = new NoxDecoyEntity(ModEntities.NOX_DECOY.get(), host.serverLevel(), host.body());
        decoy.setPos(p);
        if (!host.serverLevel().noCollision(decoy, decoy.getBoundingBox())) return;
        decoy.setYRot(host.body().getYRot()); decoy.setYHeadRot(host.body().getYHeadRot()); decoy.setYBodyRot(host.body().getYRot());
        host.serverLevel().addFreshEntity(decoy);
    }
    @Override public boolean locksMovement() { return false; }
    @Override public boolean busy() { return preparation > 0; }
    @Override public void cancel(OperativeHost host) { endStealth(host); }
    @Override public CompoundTag save() {
        var tag = new CompoundTag();
        tag.putInt("RotorCooldown", rotorCooldown); tag.putInt("FlashCharges", flash); tag.putInt("FlashRecharge", flashRecharge);
        tag.putInt("CoreCooldown", coreCooldown); tag.putInt("Preparation", preparation); tag.putInt("Stealth", stealth);
        tag.putInt("NextDecoy", nextDecoy); tag.putInt("ReducedDamage", reducedDamage);
        tag.putBoolean("Runtime", runtime); tag.putBoolean("PreviousSilent", previousSilent); tag.putBoolean("PreviousInvisible", previousInvisible);
        return tag;
    }
    @Override public void load(CompoundTag tag) {
        rotorCooldown = bound(tag, "RotorCooldown", NoxStateManager.ROTOR_COOLDOWN_TICKS);
        flash = bound(tag, "FlashCharges", NoxStateManager.FLASH_MAX_CHARGES);
        flashRecharge = bound(tag, "FlashRecharge", NoxStateManager.FLASH_RECHARGE_TICKS);
        coreCooldown = bound(tag, "CoreCooldown", NoxStateManager.CORE_COOLDOWN_TICKS);
        preparation = bound(tag, "Preparation", NoxStateManager.CORE_PREP_TICKS);
        stealth = bound(tag, "Stealth", NoxStateManager.CORE_DURATION_TICKS);
        if (stealth > 0) preparation = 0;
        nextDecoy = bound(tag, "NextDecoy", NoxStateManager.DECOY_INTERVAL_TICKS);
        reducedDamage = bound(tag, "ReducedDamage", 20);
        runtime = tag.getBoolean("Runtime"); previousSilent = tag.getBoolean("PreviousSilent"); previousInvisible = tag.getBoolean("PreviousInvisible");
        warned.clear();
    }
    private static int bound(CompoundTag tag, String key, int max) { return Math.max(0, Math.min(Math.max(0, max), tag.getInt(key))); }
    @Override public String describe() { return "rotor=" + rotorCooldown + ", flash=" + flash + ", prep=" + preparation + ", stealth=" + stealth + ", core=" + coreCooldown; }

    @Override public void refresh(OperativeHost host) {
        var fresh = new NativeNoxSkills();
        coreCooldown = fresh.coreCooldown;
        flash = fresh.flash;
        flashRecharge = fresh.flashRecharge;
        rotorCooldown = fresh.rotorCooldown;
    }
}
