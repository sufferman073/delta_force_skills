package com.rzy.dealt_force_skills.ai.operative;

import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.character.SkillSlot;
import com.rzy.dealt_force_skills.character.rover.RoverConfig;
import com.rzy.dealt_force_skills.character.rover.RoverSounds;
import com.rzy.dealt_force_skills.entity.*;
import com.rzy.dealt_force_skills.registry.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Real aerosol trajectories, allied cleansing, rescue and an independently saved dog. */
public final class NativeRoverSkills implements OperativeSkills {
    private int smokes = Math.max(0, RoverConfig.SMOKE_CHARGES), smokeRecharge;
    private int injectors = Math.max(0, RoverConfig.INJECTOR_CHARGES), injectorRecharge, coreCooldown;
    private UUID dog, rescueTarget;
    private Vec3 dogPosition;
    private long dogExpires;
    private int missingTicks, rescueTicks;
    private final Map<UUID, Integer> revealed = new HashMap<>();

    public static boolean available(OperativeHost host) { return NativeRaptorSkills.available(host); }
    private static boolean blocked(OperativeHost host, SkillSlot slot) {
        return OperativeElectronics.blocked(host, ModCharacters.ROVER_ID, slot);
    }
    public static boolean dogAvailable(LivingEntity source, Vec3 point, UUID id) {
        return source instanceof OperativeHost bot && com.rzy.dealt_force_skills.ai.operative.NativeCommanderSkills.inherited(bot.skills(), NativeRoverSkills.class) instanceof NativeRoverSkills skills
                && skills.ownsDog(id) && available(bot) && !blocked(bot, SkillSlot.CORE) && bot.withinOrders(point);
    }
    public boolean ownsDog(UUID id) { return id != null && id.equals(dog); }
    public void finishDog(UUID id) {
        if (!ownsDog(id)) return;
        dog = null; dogPosition = null; dogExpires = 0; missingTicks = 0;
        coreCooldown = Math.max(1, RoverConfig.CORE_COOLDOWN);
    }
    @Override public void tick(OperativeHost host) {
        if (coreCooldown > 0) coreCooldown--;
        smokes = Math.min(smokes, Math.max(0, RoverConfig.SMOKE_CHARGES));
        injectors = Math.min(injectors, Math.max(0, RoverConfig.INJECTOR_CHARGES));
        if (smokes < Math.max(0, RoverConfig.SMOKE_CHARGES) && --smokeRecharge <= 0) {
            smokes++; smokeRecharge = Math.max(1, RoverConfig.SMOKE_COOLDOWN);
        }
        if (injectors < Math.max(0, RoverConfig.INJECTOR_CHARGES) && --injectorRecharge <= 0) {
            injectors++; injectorRecharge = Math.max(1, RoverConfig.INJECTOR_COOLDOWN);
        }
        revealed.replaceAll((id, ticks) -> ticks - 1); revealed.values().removeIf(ticks -> ticks <= 0);
        if (!available(host)) { cancel(host); return; }
        if (dog != null) {
            var entity = host.serverLevel().getEntity(dog);
            if (entity instanceof RoverWolfEntity wolf && wolf.isNativeOwnedBy(host.body())) {
                dogPosition = wolf.position(); missingTicks = 0;
                if (blocked(host, SkillSlot.CORE) || !host.withinOrders(wolf.position())) wolf.discard();
            } else if (host.serverLevel().getGameTime() >= dogExpires
                    || (dogPosition == null || host.serverLevel().hasChunkAt(net.minecraft.core.BlockPos.containing(dogPosition))) && ++missingTicks > 40) {
                finishDog(dog);
            }
        }
        tickRescue(host);
    }
    private static boolean valid(OperativeHost host, LivingEntity target, boolean ally) {
        return target != null && target.isAlive() && target.level() == host.body().level()
                && host.withinCombatRange(target.position()) && host.body().distanceToSqr(target) <= 24 * 24
                && (target == host.body() || host.canSee(target))
                && (ally ? OperativeSupport.ally(host.body(), target) : host.isEnemy(target));
    }
    private LivingEntity patient(OperativeHost host) {
        return host.serverLevel().getEntitiesOfClass(LivingEntity.class, host.body().getBoundingBox().inflate(24),
                entity -> valid(host, entity, true) && !OperativeSupport.downed(entity)
                        && entity.getActiveEffects().stream().anyMatch(effect -> effect.getEffect().value().getCategory() == net.minecraft.world.effect.MobEffectCategory.HARMFUL))
                .stream().min(Comparator.comparingDouble(host.body()::distanceToSqr)).orElse(null);
    }
    private OperativeBallistics.Shot shot(OperativeHost host, LivingEntity target, boolean smoke, boolean ally) {
        if (!valid(host, target, ally)) return null;
        if (smoke || ally) return OperativeBallistics.solve(host, target.position(),
                smoke ? RoverConfig.SMOKE_HIGH_SPEED : RoverConfig.INJECTOR_SPEED, smoke ? RoverConfig.SMOKE_HIGH_LIFT : 0,
                0.35, 0.1, smoke ? 0.04 : 0.015, 0.99, 200, smoke ? RoverConfig.SMOKE_RADIUS * 0.7 : RoverConfig.INJECTOR_RADIUS * 0.7, true);
        return OperativeBallistics.direct(host, target, RoverConfig.INJECTOR_SPEED, 0.35, 0.1, 0.015, 0.99, 200);
    }
    private boolean covered(OperativeHost host, LivingEntity target, boolean smoke) {
        return !host.serverLevel().getEntitiesOfClass(RoverFieldEntity.class, target.getBoundingBox().inflate(6),
                field -> field.shouldRenderSmokeModel() == smoke && field.isNativeOwnedBy(host.body())
                        && field.position().distanceToSqr(target.position()) < field.radius() * field.radius()).isEmpty()
                || !host.serverLevel().getEntitiesOfClass(RoverProjectileEntity.class, host.body().getBoundingBox().inflate(40),
                    projectile -> projectile.isNativeFor(host.body(), target, smoke)).isEmpty();
    }
    @Override public double score(OperativeHost host, SkillSlot slot, LivingEntity target) {
        if (!available(host) || busy() || blocked(host, slot)) return 0;
        return switch (slot) {
            case ACTIVE_1 -> smokes > 0 && valid(host, target, false) && !covered(host, target, true) && shot(host, target, true, false) != null ? 90 : 0;
            case ACTIVE_2 -> {
                LivingEntity patient = patient(host);
                LivingEntity destination = patient == null ? target : patient;
                yield injectors > 0 && destination != null && !covered(host, destination, false)
                        && (destination == host.body() || shot(host, destination, false, patient != null) != null) ? patient != null ? 125 : 100 : 0;
            }
            case CORE -> dog == null && coreCooldown == 0 && valid(host, target, false) ? 110 : 0;
            case PASSIVE -> 0;
        };
    }
    @Override public boolean start(OperativeHost host, SkillSlot slot, LivingEntity target) {
        if (score(host, slot, target) <= 0) return false;
        if (slot == SkillSlot.CORE) {
            var wolf = new RoverWolfEntity(ModEntities.ROVER_WOLF.get(), host.serverLevel());
            wolf.configureNative(host.body()); wolf.setPos(host.body().position());
            wolf.getAttribute(Attributes.MAX_HEALTH).setBaseValue(Math.max(1, host.body().getMaxHealth() * RoverConfig.DOG_HEALTH_MULTIPLIER));
            wolf.setHealth(wolf.getMaxHealth());
            if (!host.serverLevel().addFreshEntity(wolf)) return false;
            dog = wolf.getUUID(); dogPosition = wolf.position(); missingTicks = 0;
            dogExpires = host.serverLevel().getGameTime() + Math.max(1, RoverConfig.CORE_DURATION);
            wolf.setTarget(target); sound(host, RoverSounds.DOG_SUMMON); return true;
        }
        boolean smoke = slot == SkillSlot.ACTIVE_1;
        LivingEntity patient = smoke ? null : patient(host);
        LivingEntity destination = patient == null ? target : patient;
        if (destination == host.body()) {
            var field = new RoverFieldEntity(ModEntities.ROVER_FIELD.get(), host.serverLevel());
            field.configureNative(host.body(), false); field.setPos(host.body().position());
            if (!host.serverLevel().addFreshEntity(field)) return false;
        } else {
            var shot = shot(host, destination, smoke, patient != null); if (shot == null) return false;
            var projectile = new RoverProjectileEntity(ModEntities.ROVER_PROJECTILE.get(), host.serverLevel());
            projectile.configureNative(host.body(), destination, smoke);
            projectile.setPos(host.body().getEyePosition().add(shot.direction().scale(0.35)).add(0, -0.1, 0));
            projectile.setDeltaMovement(shot.direction().scale(smoke ? RoverConfig.SMOKE_HIGH_SPEED : RoverConfig.INJECTOR_SPEED).add(0, smoke ? RoverConfig.SMOKE_HIGH_LIFT : 0, 0));
            if (!host.serverLevel().addFreshEntity(projectile)) return false;
        }
        if (smoke) { if (smokes == Math.max(0, RoverConfig.SMOKE_CHARGES)) smokeRecharge = Math.max(1, RoverConfig.SMOKE_COOLDOWN); smokes--; }
        else { if (injectors == Math.max(0, RoverConfig.INJECTOR_CHARGES)) injectorRecharge = Math.max(1, RoverConfig.INJECTOR_COOLDOWN); injectors--; }
        sound(host, smoke ? RoverSounds.SMOKE_BURST : RoverSounds.INJECTOR_BURST); return true;
    }
    private void tickRescue(OperativeHost host) {
        if (!(host.body() instanceof Mob mob)) return;
        LivingEntity target = rescueTarget != null && host.serverLevel().getEntity(rescueTarget) instanceof LivingEntity living ? living : null;
        boolean committed = host.parallelNativeAi() && rescueTicks > 0;
        if (!(committed ? committedRescue(host, target) : rescuable(host, target))) {
            resetRescue();
            target = host.serverLevel().getEntitiesOfClass(LivingEntity.class, host.body().getBoundingBox().inflate(20),
                    entity -> rescuable(host, entity)).stream().min(Comparator.comparingDouble(host.body()::distanceToSqr)).orElse(null);
            if (target == null) return;
            rescueTarget = target.getUUID();
        }
        host.aimAt(target);
        double radius = Math.max(0.1, RoverConfig.RESCUE_RADIUS);
        if (!committed && host.body().distanceToSqr(target) > radius * radius) { rescueTicks = 0; if (!host.parallelNativeAi()) mob.getNavigation().moveTo(target, 1.1); return; }
        if (!host.parallelNativeAi()) mob.getNavigation().stop();
        if (++rescueTicks >= Math.max(1, RoverConfig.RESCUE_TICKS)) { OperativeSupport.revive(target); resetRescue(); }
    }
    private boolean rescuable(OperativeHost host, LivingEntity entity) { return entity != host.body() && valid(host, entity, true) && OperativeSupport.downed(entity); }
    private static boolean committedRescue(OperativeHost host, LivingEntity target) {
        return target != null && target != host.body() && target.isAlive() && target.level() == host.body().level()
                && OperativeSupport.ally(host.body(), target) && OperativeSupport.downed(target);
    }
    private void resetRescue() { rescueTarget = null; rescueTicks = 0; }
    @Override public void remember(LivingEntity target, int ticks) { revealed.put(target.getUUID(), Math.max(1, ticks)); }
    @Override public boolean isRevealed(LivingEntity target) { return revealed.getOrDefault(target.getUUID(), 0) > 0; }
    @Override public boolean busy() { return rescueTarget != null; }
    @Override public void cancel(OperativeHost host) {
        if (dog != null && host.serverLevel().getEntity(dog) instanceof RoverWolfEntity wolf && wolf.isNativeOwnedBy(host.body())) wolf.discard();
        if (dog != null) finishDog(dog);
        resetRescue();
    }
    @Override public CompoundTag save() {
        var tag = new CompoundTag();
        tag.putInt("SmokeCharges", smokes); tag.putInt("SmokeRecharge", smokeRecharge);
        tag.putInt("InjectorCharges", injectors); tag.putInt("InjectorRecharge", injectorRecharge); tag.putInt("CoreCooldown", coreCooldown);
        if (dog != null) tag.putUUID("Dog", dog); tag.putLong("DogExpires", dogExpires);
        if (dogPosition != null) { tag.putDouble("DogX", dogPosition.x); tag.putDouble("DogY", dogPosition.y); tag.putDouble("DogZ", dogPosition.z); }
        return tag;
    }
    @Override public void load(CompoundTag tag) {
        smokes = bounded(tag,"SmokeCharges",RoverConfig.SMOKE_CHARGES); smokeRecharge = bounded(tag,"SmokeRecharge",RoverConfig.SMOKE_COOLDOWN);
        injectors = bounded(tag,"InjectorCharges",RoverConfig.INJECTOR_CHARGES); injectorRecharge = bounded(tag,"InjectorRecharge",RoverConfig.INJECTOR_COOLDOWN);
        coreCooldown = bounded(tag,"CoreCooldown",RoverConfig.CORE_COOLDOWN); dog = tag.hasUUID("Dog") ? tag.getUUID("Dog") : null;
        dogExpires = Math.max(0, tag.getLong("DogExpires"));
        dogPosition = dog != null && tag.contains("DogX") ? new Vec3(tag.getDouble("DogX"),tag.getDouble("DogY"),tag.getDouble("DogZ")) : null;
        missingTicks = 0; resetRescue(); revealed.clear();
    }
    private static int bounded(CompoundTag tag, String key, int max) { return Math.max(0,Math.min(Math.max(0,max),tag.getInt(key))); }
    private static void sound(OperativeHost host, net.minecraft.sounds.SoundEvent sound) {
        host.serverLevel().playSound(null, host.body().blockPosition(), sound, net.minecraft.sounds.SoundSource.NEUTRAL, 0.8F, 1);
    }
    @Override public String describe() { return "smoke="+smokes+", injector="+injectors+", dog="+dog+", cooldown="+coreCooldown+", rescue="+rescueTicks; }
    @Override public void refresh(OperativeHost host) {
        var fresh = new NativeRoverSkills();
        coreCooldown = fresh.coreCooldown;
        injectorRecharge = fresh.injectorRecharge;
        injectors = fresh.injectors;
        smokeRecharge = fresh.smokeRecharge;
        smokes = fresh.smokes;
    }
}
