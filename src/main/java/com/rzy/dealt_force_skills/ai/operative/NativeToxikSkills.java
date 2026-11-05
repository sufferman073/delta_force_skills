package com.rzy.dealt_force_skills.ai.operative;

import com.rzy.dealt_force_skills.character.SkillSlot;
import com.rzy.dealt_force_skills.character.toxik.ToxikFireflyMode;
import com.rzy.dealt_force_skills.character.toxik.ToxikStateManager;
import com.rzy.dealt_force_skills.entity.ToxikTearGasGrenadeEntity;
import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.registry.ModEntities;
import com.rzy.dealt_force_skills.registry.ModSounds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import java.util.Comparator;
import java.util.UUID;

/** Toxik cleanses allies, covers threats with gas, and selects lethal or amplifying fireflies. */
public final class NativeToxikSkills implements OperativeSkills {
    private int gas = Math.max(0, ToxikStateManager.TEAR_GAS_MAX_CHARGES);
    private int gasRecharge, adrenalineCooldown, fireflyCooldown, windup;
    private SkillSlot prepared;
    private UUID preparedTarget;
    private boolean amplify;

    @Override public void tick(OperativeHost host) {
        if (adrenalineCooldown > 0) adrenalineCooldown--;
        if (fireflyCooldown > 0) fireflyCooldown--;
        int capacity = Math.max(0, ToxikStateManager.TEAR_GAS_MAX_CHARGES);
        gas = Math.min(gas, capacity);
        if (gas < capacity && --gasRecharge <= 0) { gas++; gasRecharge = Math.max(1, ToxikStateManager.TEAR_GAS_RECHARGE_TICKS); }
        if (prepared == null) return;
        LivingEntity target = host.serverLevel().getEntity(preparedTarget) instanceof LivingEntity living ? living : null;
        if (!host.body().isAlive() || OperativeSupport.downed(host.body())
                || target == null || !target.isAlive() || target.level() != host.body().level()
                || !host.parallelNativeAi() && !valid(host, target, prepared == SkillSlot.ACTIVE_1 || amplify,
                    prepared == SkillSlot.ACTIVE_1 ? ToxikStateManager.ADRENALINE_RADIUS
                        : prepared == SkillSlot.ACTIVE_2 ? 24 : ToxikStateManager.FIREFLY_RANGE)) {
            clear(); return;
        }
        host.aimAt(target);
        if (--windup > 0) return;
        switch (prepared) {
            case ACTIVE_1 -> releaseAdrenaline(host);
            case ACTIVE_2 -> throwGas(host, target);
            case CORE -> releaseFirefly(host, target);
            default -> { }
        }
        clear();
    }

    @Override public double score(OperativeHost host, SkillSlot slot, LivingEntity enemy) {
        if (busy() || !host.withinOrders(host.body().position()) || OperativeSupport.downed(host.body())) return 0;
        return switch (slot) {
            case ACTIVE_1 -> adrenalineCooldown == 0 && injectionTarget(host, enemy) != null ? 115 : 0;
            case ACTIVE_2 -> gas > 0 && valid(host, enemy, false, 24) && !enemy.hasEffect(ModEffects.TOXIK_TEAR_GAS_BLIND)
                    && gasShot(host, enemy) != null ? 80 : 0;
            case CORE -> fireflyCooldown > 0 ? 0 : supportTarget(host, enemy) != null ? 105
                    : valid(host, enemy, false, ToxikStateManager.FIREFLY_RANGE)
                        && !enemy.hasEffect(ModEffects.TOXIK_FIREFLY_INTERFERENCE) ? 95 : 0;
            case PASSIVE -> 0;
        };
    }

    @Override public boolean start(OperativeHost host, SkillSlot slot, LivingEntity enemy) {
        if (score(host, slot, enemy) <= 0) return false;
        LivingEntity support = slot == SkillSlot.CORE ? supportTarget(host, enemy) : null;
        LivingEntity target = slot == SkillSlot.ACTIVE_1 ? injectionTarget(host, enemy) : support != null ? support : enemy;
        if (target == null) return false;
        amplify = support != null;
        if (host.parallelNativeAi()) {
            switch (slot) {
                case ACTIVE_1 -> releaseAdrenaline(host);
                case ACTIVE_2 -> throwGas(host, target);
                case CORE -> releaseFirefly(host, target);
                default -> { return false; }
            }
            amplify = false;
            return true;
        }
        prepared = slot; preparedTarget = target.getUUID();
        int baseWindup = slot == SkillSlot.CORE ? ToxikStateManager.FIREFLY_PULLOUT_TICKS : 5;
        windup = Math.max(1, (int) Math.ceil(baseWindup / Math.max(0.1, ToxikStateManager.behaviorSpeedMultiplier(host.body()))));
        host.aimAt(target); return true;
    }

    private boolean valid(OperativeHost host, LivingEntity target, boolean ally, double range) {
        return target != null && target.isAlive() && !OperativeSupport.downed(target)
                && host.withinOrders(host.body().position()) && host.withinCombatRange(target.position())
                && host.body().distanceToSqr(target) <= Math.max(0, range) * Math.max(0, range)
                && (target == host.body() || host.canSee(target))
                && (ally ? OperativeSupport.ally(host.body(), target) : host.isEnemy(target));
    }
    private boolean harmful(LivingEntity target) {
        return target.getActiveEffects().stream().anyMatch(effect -> effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL);
    }
    private boolean alliedCombat(OperativeHost host,LivingEntity ally){
        LivingEntity threat=ally instanceof net.minecraft.world.entity.Mob mob?mob.getTarget():null;
        if(threat==null || !threat.isAlive())threat=ally.getLastHurtByMob();
        return threat!=null && threat.isAlive() && !OperativeSupport.ally(host.body(),threat)
                && ally.distanceToSqr(threat)<=24*24;
    }
    private LivingEntity injectionTarget(OperativeHost host, LivingEntity enemy) {
        double radius = Math.max(0, ToxikStateManager.ADRENALINE_RADIUS);
        return host.serverLevel().getEntitiesOfClass(LivingEntity.class, host.body().getBoundingBox().inflate(radius),
                target -> valid(host, target, true, radius) && (harmful(target)
                        || (valid(host, enemy, false, 24) || alliedCombat(host,target)) && !target.hasEffect(ModEffects.TOXIK_ADRENALINE)))
                .stream().min(Comparator.comparingDouble(target -> harmful(target) ? -1 : host.body().distanceToSqr(target))).orElse(null);
    }
    private LivingEntity supportTarget(OperativeHost host, LivingEntity enemy) {
        double range = Math.max(0, Math.min(24, ToxikStateManager.FIREFLY_RANGE));
        return host.serverLevel().getEntitiesOfClass(LivingEntity.class, host.body().getBoundingBox().inflate(range),
                target -> target != host.body() && valid(host, target, true, range)
                        && (harmful(target) || (valid(host, enemy, false, 24) || alliedCombat(host,target)) && !target.hasEffect(ModEffects.TOXIK_ADRENALINE)))
                .stream().min(Comparator.comparingDouble(target -> harmful(target) ? -1 : host.body().distanceToSqr(target))).orElse(null);
    }
    private void releaseAdrenaline(OperativeHost host) {
        if (adrenalineCooldown > 0) return;
        double radius = Math.max(0, ToxikStateManager.ADRENALINE_RADIUS);
        boolean applied = false;
        for (LivingEntity target : host.serverLevel().getEntitiesOfClass(LivingEntity.class,
                host.body().getBoundingBox().inflate(radius), target -> valid(host, target, true, radius))) {
            NativeToxikEffects.adrenaline(host.body(), target, ToxikStateManager.ADRENALINE_BASE_DURATION_TICKS); applied = true;
        }
        if (applied) { adrenalineCooldown = Math.max(1, ToxikStateManager.ADRENALINE_COOLDOWN_TICKS); sound(host, ModSounds.TOXIK_ADRENALINE_RELEASE.get()); }
    }
    private OperativeBallistics.Shot gasShot(OperativeHost host, LivingEntity target) {
        return OperativeBallistics.solve(host, target.position().add(0, 0.1, 0), 1.08, 0.08, 0.58, 0.1,
                0.045, 0.985, 80, 1.5, true);
    }
    private void throwGas(OperativeHost host, LivingEntity target) {
        var shot = gasShot(host, target);
        if (gas <= 0 || shot == null) return;
        var grenade = new ToxikTearGasGrenadeEntity(ModEntities.TOXIK_TEAR_GAS_GRENADE.get(), host.serverLevel(), host.body());
        grenade.setPos(host.body().getEyePosition().add(shot.direction().scale(0.58)).add(0, -0.1, 0));
        grenade.setDeltaMovement(shot.direction().scale(1.08).add(0, 0.08, 0));
        if (!host.serverLevel().addFreshEntity(grenade)) return;
        if (gas == Math.max(0, ToxikStateManager.TEAR_GAS_MAX_CHARGES)) gasRecharge = Math.max(1, ToxikStateManager.TEAR_GAS_RECHARGE_TICKS);
        gas--; sound(host, ModSounds.TOXIK_TEAR_GAS_THROW.get());
    }
    private void releaseFirefly(OperativeHost host, LivingEntity target) {
        if (fireflyCooldown > 0 || amplify && target.hasEffect(ModEffects.TOXIK_ADRENALINE) && !harmful(target)
                || !amplify && target.hasEffect(ModEffects.TOXIK_FIREFLY_INTERFERENCE)) return;
        Vec3 direction = target.position().add(0, target.getBbHeight() * 0.58, 0).subtract(host.body().getEyePosition()).normalize();
        ToxikStateManager.spawnFireflySwarm(host.serverLevel(), host.body(),
                amplify ? ToxikFireflyMode.AMPLIFY : ToxikFireflyMode.LETHAL, host.body().getEyePosition(), direction);
        fireflyCooldown = Math.max(1, ToxikStateManager.FIREFLY_COOLDOWN_TICKS);
        sound(host, ModSounds.TOXIK_FIREFLY_RELEASE.get());
    }
    private void clear() { prepared = null; preparedTarget = null; windup = 0; amplify = false; }
    @Override public boolean busy() { return prepared != null; }
    @Override public void cancel(OperativeHost host) { clear(); }
    @Override public CompoundTag save() {
        var tag = new CompoundTag(); tag.putInt("GasCharges", gas); tag.putInt("GasRecharge", gasRecharge);
        tag.putInt("AdrenalineCooldown", adrenalineCooldown); tag.putInt("FireflyCooldown", fireflyCooldown); return tag;
    }
    @Override public void load(CompoundTag tag) {
        gas = bounded(tag, "GasCharges", ToxikStateManager.TEAR_GAS_MAX_CHARGES);
        gasRecharge = bounded(tag, "GasRecharge", ToxikStateManager.TEAR_GAS_RECHARGE_TICKS);
        adrenalineCooldown = bounded(tag, "AdrenalineCooldown", ToxikStateManager.ADRENALINE_COOLDOWN_TICKS);
        fireflyCooldown = bounded(tag, "FireflyCooldown", ToxikStateManager.FIREFLY_COOLDOWN_TICKS); clear();
    }
    private static int bounded(CompoundTag tag, String key, int max) { return Math.max(0, Math.min(Math.max(0, max), tag.getInt(key))); }
    private static void sound(OperativeHost host, net.minecraft.sounds.SoundEvent sound) {
        host.serverLevel().playSound(null, host.body().blockPosition(), sound, SoundSource.PLAYERS, 0.85F, 1);
    }
    @Override public String describe() { return "gas=" + gas + ", injection=" + adrenalineCooldown + ", firefly=" + fireflyCooldown + ", preparing=" + prepared; }
    @Override public void refresh(OperativeHost host) {
        var fresh = new NativeToxikSkills();
        adrenalineCooldown = fresh.adrenalineCooldown;
        fireflyCooldown = fresh.fireflyCooldown;
        gas = fresh.gas;
        gasRecharge = fresh.gasRecharge;
    }
    @Override public boolean locksMovement() { return false; }
}
