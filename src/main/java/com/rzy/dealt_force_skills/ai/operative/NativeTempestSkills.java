package com.rzy.dealt_force_skills.ai.operative;

import com.rzy.dealt_force_skills.character.SkillSlot;
import com.rzy.dealt_force_skills.character.tempest.TempestStateManager;
import com.rzy.dealt_force_skills.config.DealtForceConfig;
import com.rzy.dealt_force_skills.entity.OperativeEntity;
import com.rzy.dealt_force_skills.entity.TempestRecallAnchorEntity;
import com.rzy.dealt_force_skills.entity.TempestWallDrillStingerEntity;
import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.registry.ModEntities;
import com.rzy.dealt_force_skills.registry.ModSounds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Base Tempest kit. Recorded paths are local to one loaded host and dimension. */
public final class NativeTempestSkills implements OperativeSkills {
    private int wallCharges = Math.max(0, TempestStateManager.WALL_MAX_CHARGES);
    private int wallRecharge, rollCooldown, coreCooldown, rollTicks, fallProtection;
    private int recallTicks, recallIndex, downedTicks, rescueProgress;
    private boolean pendingRoll, recalling, automatic, suspended;
    private Vec3 rollDirection = Vec3.ZERO;
    private UUID anchorId;
    private String dimension;
    private final List<Vec3> path = new ArrayList<>();
    private double ropeLength, fallStartY;
    private final Map<UUID, Long> nearMisses = new HashMap<>();

    @Override public void tick(OperativeHost host) {
        if (rollCooldown > 0) rollCooldown--;
        if (coreCooldown > 0) coreCooldown--;
        if (fallProtection > 0) fallProtection--;
        int capacity = Math.max(0, TempestStateManager.WALL_MAX_CHARGES);
        wallCharges = Math.min(wallCharges, capacity);
        if (wallCharges >= capacity) wallRecharge = 0;
        else if (wallRecharge <= 0) wallRecharge = Math.max(1, TempestStateManager.WALL_RECHARGE_TICKS);
        else if (--wallRecharge == 0) wallCharges++;
        if (downed()) { tickDowned(host); return; }
        if (!host.body().isAlive() || host.body().isRemoved() || host.body().hasEffect(ModEffects.STUN)
                || host.body().hasEffect(ModEffects.WEBBED)) { cancel(host); return; }
        if (host.body() instanceof OperativeEntity bot && bot.medical().downed()) { cancel(host); return; }
        nearMiss(host);
        if (ropeActive()) {
            if (!host.body().level().dimension().location().toString().equals(dimension)
                    || !host.withinOrders(host.body().position())
                    || !(host.serverLevel().getEntity(anchorId) instanceof TempestRecallAnchorEntity)) { cancel(host); return; }
            if (recalling) { tickRecall(host); return; }
            recordPath(host);
            if (recalling || !ropeActive()) return;
            if (!host.body().onGround()) {
                fallStartY = Math.max(fallStartY, host.body().getY());
                if (host.body().fallDistance > 11 || suspended) {
                    suspended = true;
                    Vec3 pos = host.body().position();
                    Vec3 held = new Vec3(pos.x, Math.max(pos.y, fallStartY - 10), pos.z);
                    if (canMove(host, held)) host.body().teleportTo(held.x, held.y, held.z);
                    host.body().setDeltaMovement(Vec3.ZERO); host.body().fallDistance = 0;
                }
            } else { fallStartY = host.body().getY(); suspended = false; }
        }
        if (pendingRoll && (host.body().onGround() || host.body().isInWaterOrBubble())) {
            if (host.body().isInWaterOrBubble()) pendingRoll = false;
            else consumeFallProtection(host);
        }
        if (rollTicks > 0) {
            double speed = number("roll_boost_speed_per_tick", 2.75);
            Vec3 next = host.body().position().add(rollDirection.scale(speed));
            if (host.body().horizontalCollision || host.body().isInWaterOrBubble()
                    || !safeRoll(host, next)) stopRoll(host);
            else {
                host.body().setPose(Pose.SWIMMING); host.body().refreshDimensions();
                host.body().setDeltaMovement(rollDirection.scale(speed).add(0, host.body().getDeltaMovement().y, 0));
                host.body().fallDistance = 0; host.body().hurtMarked = true; rollTicks--;
            }
        }
    }

    @Override public double score(OperativeHost host, SkillSlot slot, LivingEntity enemy) {
        if (busy() || !host.body().isAlive() || host.body().isRemoved() || OperativeSupport.downed(host.body())
                || host.body().hasEffect(ModEffects.STUN) || host.body().hasEffect(ModEffects.WEBBED)
                || !host.withinOrders(host.body().position())
                || OperativeElectronics.blocked(host, com.rzy.dealt_force_skills.character.ModCharacters.TEMPEST_ID, slot)) return 0;
        boolean threat = enemy != null && host.isEnemy(enemy) && host.withinOrders(enemy.position());
        return switch (slot) {
            case ACTIVE_1 -> rollCooldown == 0 && !pendingRoll && !host.body().isInWaterOrBubble()
                    && (host.body().isOnFire() || threat && host.body().distanceToSqr(enemy) >= 36
                    || !host.body().onGround() && host.body().fallDistance > 3)
                    && (!host.body().onGround() || safeRoll(host, host.body().position()
                    .add(rollVector(host, enemy).scale(number("roll_boost_speed_per_tick", 2.75))))) ? 65 : 0;
            case ACTIVE_2 -> wallCharges > 0 && threat && !enemy.hasEffect(ModEffects.TEMPEST_DISARMED)
                    && drillShot(host, enemy) != null ? 90 : 0;
            case CORE -> ropeActive() ? (host.body().getHealth() <= host.body().getMaxHealth() * 0.5F
                    || !threat && host.body().position().distanceToSqr(path.get(0)) > 4 ? 130 : 0)
                    : coreCooldown == 0 && threat && host.body().onGround()
                    && host.body().getHealth() > host.body().getMaxHealth() * 0.5F ? 110 : 0;
            case PASSIVE -> 0;
        };
    }

    @Override public boolean start(OperativeHost host, SkillSlot slot, LivingEntity enemy) {
        if (score(host, slot, enemy) <= 0) return false;
        if (slot == SkillSlot.ACTIVE_1) {
            rollDirection = rollVector(host, enemy); host.body().clearFire();
            rollCooldown = Math.max(1, TempestStateManager.ROLL_COOLDOWN_TICKS);
            if (host.body().onGround()) beginRoll(); else pendingRoll = true;
            sound(host, ModSounds.TEMPEST_TACTICAL_ROLL_START.get());
        } else if (slot == SkillSlot.ACTIVE_2) {
            var shot = drillShot(host, enemy);
            if (shot == null) return false;
            var drill = new TempestWallDrillStingerEntity(ModEntities.TEMPEST_WALL_DRILL_STINGER.get(), host.body().level(), host.body());
            drill.setPos(host.body().getEyePosition().add(shot.direction().scale(0.62)).add(0, -0.08, 0));
            drill.setDeltaMovement(shot.direction().scale(1.05).add(0, 0.08, 0));
            if (!host.serverLevel().addFreshEntity(drill)) return false;
            host.aimAt(enemy);
            if (wallCharges == Math.max(0, TempestStateManager.WALL_MAX_CHARGES))
                wallRecharge = Math.max(1, TempestStateManager.WALL_RECHARGE_TICKS);
            wallCharges--; sound(host, ModSounds.TEMPEST_WALL_DRILL_THROW.get());
        } else if (slot == SkillSlot.CORE) {
            if (ropeActive()) startRecall(host, false);
            else {
                var anchor = new TempestRecallAnchorEntity(ModEntities.TEMPEST_RECALL_ANCHOR.get(), host.body().level(), host.body());
                anchor.setPos(host.body().position().add(0, 0.04, 0));
                if (!host.serverLevel().addFreshEntity(anchor)) return false;
                anchorId = anchor.getUUID(); dimension = host.body().level().dimension().location().toString();
                path.add(host.body().position()); ropeLength = 0; fallStartY = host.body().getY();
                sound(host, ModSounds.TEMPEST_RECALL_ANCHOR_PLACE.get());
            }
        } else return false;
        triggerSpine(host); return true;
    }

    private static Vec3 rollVector(OperativeHost host, LivingEntity enemy) {
        return OperativeTactics.displacement(host, enemy, number("roll_boost_speed_per_tick", 2.75) * Math.max(1, integer("roll_boost_ticks_total", 9)));
    }
    private static boolean safeRoll(OperativeHost host, Vec3 next) {
        return next.distanceToSqr(host.body().position()) > .25D && canMove(host, next) && !host.body().level().noCollision(host.body(),
                host.body().getBoundingBox().move(next.subtract(host.body().position())).move(0, -1, 0));
    }
    private void beginRoll() {
        pendingRoll = false; fallProtection = 8;
        rollTicks = Math.max(1, integer("roll_boost_ticks_total", 9));
    }
    public boolean consumeFallProtection(OperativeHost host) {
        if (!pendingRoll && fallProtection <= 0) return false;
        host.body().clearFire(); host.body().fallDistance = 0;
        if (pendingRoll) beginRoll();
        return true;
    }
    private void stopRoll(OperativeHost host) {
        if (rollTicks > 0) host.body().setDeltaMovement(0, host.body().getDeltaMovement().y, 0);
        rollTicks = 0;
        if (host.body() instanceof OperativeEntity bot) bot.tryStanding();
    }

    private static OperativeBallistics.Shot drillShot(OperativeHost host, LivingEntity enemy) {
        if (host.body().distanceToSqr(enemy) > 24 * 24 || host.body().distanceToSqr(enemy) < 4) return null;
        var direct = OperativeBallistics.direct(host, enemy, 1.05, 0.08, 0.62, 0.08, 0.035, 0.988, 80);
        if (direct != null) return direct;
        // The real drill intentionally emits its directional pulse through a wall.
        Vec3 direction = enemy.getBoundingBox().getCenter().subtract(host.body().getEyePosition()).normalize();
        Vec3 pos = host.body().getEyePosition().add(direction.scale(0.62)).add(0, -0.08, 0);
        Vec3 motion = direction.scale(1.05).add(0, 0.08, 0);
        for (int tick = 0; tick < 80; tick++) {
            Vec3 next = pos.add(motion);
            var hit = host.body().level().clip(new ClipContext(pos, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, host.body()));
            if (hit.getType() == HitResult.Type.BLOCK) {
                Vec3 toEnemy = enemy.getBoundingBox().getCenter().subtract(hit.getLocation());
                Vec3 release = motion.normalize(); double along = toEnemy.dot(release);
                double range = DealtForceConfig.doubleValue("summons.tempestwalldrillstingerentity.release_range", 10);
                double width = DealtForceConfig.doubleValue("summons.tempest_wall_drill_stinger.release_half_width", 2.5);
                return along >= 0 && along <= range && toEnemy.lengthSqr() - along * along <= width * width
                        && host.withinOrders(hit.getLocation()) ? new OperativeBallistics.Shot(direction, hit.getLocation(), tick + 1) : null;
            }
            pos = next; motion = motion.add(0, -0.035, 0).scale(0.988);
        }
        return null;
    }

    public boolean ropeActive() { return anchorId != null && !path.isEmpty(); }
    public boolean isAnchor(UUID id) { return ropeActive() && anchorId.equals(id); }
    public boolean recalling() { return recalling; }
    public boolean downed() { return downedTicks > 0; }
    private void recordPath(OperativeHost host) {
        Vec3 last = path.get(path.size() - 1), current = host.body().position();
        double distance = last.distanceTo(current);
        if (distance < Math.max(0.05, number("path_node_distance", 1))) return;
        if (ropeLength + distance > Math.max(0, TempestStateManager.ROPE_MAX_LENGTH) * (OperativeGrowth.commander(host.body()) ? 3 : 1) || path.size() >= 4096) {
            startRecall(host, false); return;
        }
        path.add(current); ropeLength += distance;
    }
    private void startRecall(OperativeHost host, boolean emergency) {
        stopRoll(host); pendingRoll = false;
        if (path.get(path.size() - 1).distanceToSqr(host.body().position()) > 0.0001) path.add(host.body().position());
        recalling = true; automatic = emergency; recallTicks = 0; recallIndex = path.size() - 2;
        if (!host.parallelNativeAi()) { host.body().stopUsingItem(); host.body().setSprinting(false); }
        if (host.body() instanceof OperativeEntity bot) { bot.setTarget(null); bot.getNavigation().stop(); }
        sound(host, emergency ? ModSounds.TEMPEST_RECALL_LETHAL_TRIGGER.get() : ModSounds.TEMPEST_RECALL_ACTIVE.get());
    }
    private void tickRecall(OperativeHost host) {
        if (++recallTicks > Math.max(1, integer("recall_max_ticks", 600)) || recallIndex < 0) { endRecall(host); return; }
        Vec3 target = path.get(recallIndex), current = host.body().position(), delta = target.subtract(current);
        double speed = Math.max(0.05, TempestStateManager.RECALL_SPEED_PER_TICK);
        Vec3 next = delta.length() <= speed ? target : current.add(delta.normalize().scale(speed));
        if (!canMove(host, next)) { endRecall(host); return; }
        host.body().teleportTo(next.x, next.y, next.z);
        host.body().setDeltaMovement(Vec3.ZERO); host.body().fallDistance = 0; host.body().hurtMarked = true;
        if (next.distanceToSqr(target) < 0.0001 && --recallIndex < 0) endRecall(host);
    }
    private static boolean canMove(OperativeHost host, Vec3 next) {
        return Double.isFinite(next.x) && Double.isFinite(next.y) && Double.isFinite(next.z)
                && host.withinOrders(next) && host.body().level().hasChunkAt(net.minecraft.core.BlockPos.containing(next))
                && host.body().level().getWorldBorder().isWithinBounds(net.minecraft.core.BlockPos.containing(next))
                && host.body().level().noCollision(host.body(), host.body().getBoundingBox().expandTowards(next.subtract(host.body().position())));
    }
    private void endRecall(OperativeHost host) {
        boolean emergency = automatic; clearRope(host);
        if (emergency && host.body().isAlive()) {
            downedTicks = 1200; rescueProgress = 0; host.body().setHealth(Math.max(1, host.body().getHealth()));
            host.body().addEffect(new MobEffectInstance(ModEffects.TEMPEST_EMERGENCY_DOWNED, downedTicks, 0, false, true, true));
        }
        sound(host, ModSounds.TEMPEST_RECALL_END.get());
    }
    private void clearRope(OperativeHost host) {
        if (anchorId != null) {
            for (var level : host.serverLevel().getServer().getAllLevels()) {
                if (level.getEntity(anchorId) instanceof TempestRecallAnchorEntity anchor) anchor.discard();
            }
            coreCooldown = Math.max(coreCooldown, Math.max(1, TempestStateManager.CORE_COOLDOWN_TICKS));
        }
        anchorId = null; dimension = null; path.clear(); ropeLength = 0; recalling = automatic = suspended = false;
    }
    public boolean preventDeath(OperativeHost host, DamageSource source) {
        if (!ropeActive() || recalling || downed() || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
        if (OperativeGrowth.commander(host.body())) {
            Vec3 destination = path.get(0);
            if (canMove(host, destination)) host.body().teleportTo(destination.x, destination.y, destination.z);
            clearRope(host);
            host.body().setDeltaMovement(Vec3.ZERO); host.body().fallDistance = 0;
            host.body().setHealth(host.body().getMaxHealth());
            for (var slot : net.minecraft.world.entity.EquipmentSlot.values()) {
                var item = host.body().getItemBySlot(slot);
                if (item.isDamageableItem()) item.setDamageValue(0);
            }
            return true;
        }
        host.body().setHealth(1); startRecall(host, true); return true;
    }
    private void tickDowned(OperativeHost host) {
        host.body().stopUsingItem(); host.body().setDeltaMovement(0, host.body().getDeltaMovement().y, 0);
        if (host.body() instanceof OperativeEntity bot) { bot.setTarget(null); bot.getNavigation().stop(); }
        if (--downedTicks == 0) { clearDowned(host); host.body().hurt(host.body().damageSources().genericKill(), Float.MAX_VALUE); return; }
        if (!host.body().hasEffect(ModEffects.TEMPEST_EMERGENCY_DOWNED))
            host.body().addEffect(new MobEffectInstance(ModEffects.TEMPEST_EMERGENCY_DOWNED, downedTicks, 0, false, true, true));
        if (host.body() instanceof net.minecraft.world.entity.Mob mob && !mob.isNoAi()) {
            if (++rescueProgress >= Math.max(1, TempestStateManager.DOWNED_SELF_RESCUE_TICKS)) revive(host, false);
        } else rescueProgress = 0;
    }
    public void clearDowned(OperativeHost host) {
        downedTicks = rescueProgress = 0; host.body().removeEffect(ModEffects.TEMPEST_EMERGENCY_DOWNED);
    }
    public void revive(OperativeHost host, boolean full) {
        clearDowned(host); host.body().setHealth(full ? host.body().getMaxHealth()
                : Math.max(host.body().getHealth(), Math.min(host.body().getMaxHealth(), 6))); host.body().clearFire();
        sound(host, ModSounds.TEMPEST_RECALL_SELF_RESCUE_COMPLETE.get());
    }
    public void onKill() { rollCooldown = 0; }
    public void triggerSpine(OperativeHost host) {
        host.body().addEffect(new MobEffectInstance(ModEffects.TEMPEST_EXPLOSIVE_SPINE,
                Math.max(1, TempestStateManager.EXPLOSIVE_SPINE_TICKS) * (OperativeGrowth.commander(host.body()) ? 3 : 1),
                DealtForceConfig.intValue("characters.tempest.tempest_state_manager.effect.tempest_explosive_spine.0.amplifier", 0), false, true, true));
    }
    private void nearMiss(OperativeHost host) {
        long now = host.serverLevel().getGameTime(); nearMisses.entrySet().removeIf(entry -> entry.getValue() <= now);
        double radius = Math.max(0, number("near_miss_radius", 2));
        Vec3 center = host.body().position().add(0, host.body().getBbHeight() * 0.55, 0);
        for (Entity entity : host.serverLevel().getEntities(host.body(), host.body().getBoundingBox().inflate(radius + 0.35),
                TempestStateManager::isDangerousIncomingEntity)) {
            if (nearMisses.containsKey(entity.getUUID()) || entity instanceof Projectile projectile
                    && (projectile.getOwner() == host.body() || projectile.getOwner() instanceof LivingEntity owner
                    && OperativeSupport.ally(host.body(), owner))) continue;
            Vec3 motion = entity.getDeltaMovement();
            if (motion.lengthSqr() >= 0.0025 && TempestStateManager.distancePointToSegment(center,
                    entity.position().subtract(motion), entity.position()) <= radius) {
                if (nearMisses.size() >= 256) nearMisses.clear();
                nearMisses.put(entity.getUUID(), now + 20); triggerSpine(host); break;
            }
        }
    }
    @Override public boolean busy() { return rollTicks > 0 || recalling || downed(); }
    @Override public void cancel(OperativeHost host) {
        stopRoll(host); pendingRoll = false; fallProtection = 0;
        if (automatic) endRecall(host); else clearRope(host);
        nearMisses.clear(); host.body().removeEffect(ModEffects.TEMPEST_EXPLOSIVE_SPINE);
    }
    @Override public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("WallCharges", wallCharges); tag.putInt("WallRecharge", wallRecharge);
        tag.putInt("RollCooldown", rollCooldown); tag.putInt("CoreCooldown", coreCooldown);
        tag.putInt("DownedTicks", downedTicks); tag.putBoolean("RopeActive", ropeActive());
        tag.putBoolean("AutomaticRecall", automatic); return tag;
    }
    @Override public void load(CompoundTag tag) {
        wallCharges = bounded(tag, "WallCharges", TempestStateManager.WALL_MAX_CHARGES);
        wallRecharge = bounded(tag, "WallRecharge", TempestStateManager.WALL_RECHARGE_TICKS);
        rollCooldown = bounded(tag, "RollCooldown", TempestStateManager.ROLL_COOLDOWN_TICKS);
        coreCooldown = tag.getBoolean("RopeActive") ? Math.max(1, TempestStateManager.CORE_COOLDOWN_TICKS)
                : bounded(tag, "CoreCooldown", TempestStateManager.CORE_COOLDOWN_TICKS);
        downedTicks = tag.getBoolean("AutomaticRecall") ? 1200 : bounded(tag, "DownedTicks", 1200);
        anchorId = null; dimension = null; path.clear(); ropeLength = 0;
        recalling = automatic = suspended = pendingRoll = false; rollTicks = fallProtection = rescueProgress = 0; nearMisses.clear();
    }
    private static int bounded(CompoundTag tag, String key, int max) { return Math.max(0, Math.min(Math.max(0, max), tag.getInt(key))); }
    private static int integer(String key, int fallback) { return DealtForceConfig.intValue("characters.tempest.tempest_state_manager." + key, fallback); }
    private static double number(String key, double fallback) { return DealtForceConfig.doubleValue("characters.tempest.tempest_state_manager." + key, fallback); }
    private static void sound(OperativeHost host, net.minecraft.sounds.SoundEvent sound) {
        host.serverLevel().playSound(null, host.body().blockPosition(), sound, SoundSource.NEUTRAL, 0.85F, 1);
    }
    @Override public String describe() { return "walls=" + wallCharges + ", roll=" + rollCooldown + ", core=" + coreCooldown
            + ", rope=" + ropeActive() + ", length=" + ropeLength + ", recalling=" + recalling + ", downed=" + downedTicks; }
    @Override public void refresh(OperativeHost host) {
        var fresh = new NativeTempestSkills();
        coreCooldown = fresh.coreCooldown;
        rollCooldown = fresh.rollCooldown;
        wallCharges = fresh.wallCharges;
        wallRecharge = fresh.wallRecharge;
    }
    @Override public com.rzy.dealt_force_skills.motion.TacticalMotion motion() {
        return rollTicks > 0 ? com.rzy.dealt_force_skills.motion.TacticalMotion.ROLL : com.rzy.dealt_force_skills.motion.TacticalMotion.NONE;
    }
    @Override public int motionDuration() { return Math.max(1, integer("roll_boost_ticks_total", 9)); }
    @Override public Vec3 motionDirection() { return rollDirection; }

}
