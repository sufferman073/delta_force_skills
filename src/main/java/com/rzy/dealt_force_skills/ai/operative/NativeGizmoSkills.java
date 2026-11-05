package com.rzy.dealt_force_skills.ai.operative;

import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.character.SkillSlot;
import com.rzy.dealt_force_skills.character.gizmo.GizmoPlacementHelper;
import com.rzy.dealt_force_skills.character.gizmo.GizmoStateManager;
import com.rzy.dealt_force_skills.entity.GizmoSmokeTrapEntity;
import com.rzy.dealt_force_skills.entity.GizmoSpiderNestTrapEntity;
import com.rzy.dealt_force_skills.entity.GizmoTBoyEntity;
import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.registry.ModEntities;
import com.rzy.dealt_force_skills.registry.ModSounds;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Base Gizmo uses the actual traps and crawlers, without Commander branch upgrades. */
public final class NativeGizmoSkills implements OperativeSkills {
    private final double capacityScale;
    private int smoke, nests;

    public NativeGizmoSkills() { this(1); }
    NativeGizmoSkills(double capacityScale) {
        this.capacityScale = Math.max(1.0D, capacityScale);
        smoke = smokeCapacity();
        nests = nestCapacity();
    }
    private int capacity(int base) {
        return (int) Math.min(Integer.MAX_VALUE, Math.ceil(Math.max(0, base) * capacityScale));
    }
    private int smokeCapacity() { return capacity(GizmoStateManager.SMOKE_MAX_CHARGES); }
    private int nestCapacity() { return capacity(GizmoStateManager.SPIDER_MAX_CHARGES); }
    private int smokeActiveLimit() { return capacity(GizmoStateManager.SMOKE_ACTIVE_LIMIT); }
    private int nestActiveLimit() { return capacity(GizmoStateManager.SPIDER_ACTIVE_LIMIT); }
    private int smokeRecharge, nestRecharge, coreCooldown, trapDelay;
    private final Map<UUID, Integer> revealed = new HashMap<>();

    @Override public void tick(OperativeHost host) {
        if (coreCooldown > 0) coreCooldown--;
        if (trapDelay > 0) trapDelay--;
        int smokeAllowed = Math.max(0, smokeCapacity() - smokeCount(host));
        smoke = Math.min(smoke, smokeAllowed);
        if (smoke >= smokeAllowed) smokeRecharge = 0;
        else if (smokeRecharge <= 0) smokeRecharge = Math.max(1, GizmoStateManager.SMOKE_RECHARGE_TICKS);
        else if (--smokeRecharge == 0) smoke++;
        int nestAllowed = Math.max(0, nestCapacity() - nestCount(host));
        nests = Math.min(nests, nestAllowed);
        if (nests >= nestAllowed) nestRecharge = 0;
        else if (nestRecharge <= 0) nestRecharge = Math.max(1, GizmoStateManager.SPIDER_RECHARGE_TICKS);
        else if (--nestRecharge == 0) nests++;
        revealed.replaceAll((id, ticks) -> ticks - 1);
        revealed.values().removeIf(ticks -> ticks <= 0);
        updatePassive(host, host.body().isAlive() && !host.body().isRemoved() && nearOwnTrap(host));
    }

    @Override public double score(OperativeHost host, SkillSlot slot, LivingEntity target) {
        if (!host.body().isAlive() || host.body().isRemoved() || !host.withinOrders(host.body().position())
                || host.body().hasEffect(ModEffects.STUN) || host.body().hasEffect(ModEffects.WEBBED)
                || target == null || !host.isEnemy(target) || !host.withinCombatRange(target.position())
                || !host.canSee(target) || host.body().distanceToSqr(target) > 24 * 24
                || OperativeElectronics.blocked(host, ModCharacters.GIZMO_ID, slot)) return 0;
        return switch (slot) {
            case ACTIVE_1 -> host.body().distanceToSqr(target) <= 10 * 10 && smoke > 0 && trapDelay == 0 && smokeCount(host) < smokeActiveLimit()
                    && trapPosition(host, target, false) != null ? 75 : 0;
            case ACTIVE_2 -> nests > 0 && trapDelay == 0 && nestCount(host) < nestActiveLimit()
                    && trapPosition(host, target, true) != null ? 85 : 0;
            case CORE -> coreCooldown == 0 && crawlerPosition(host, target) != null ? 100 : 0;
            default -> 0;
        };
    }

    @Override public boolean start(OperativeHost host, SkillSlot slot, LivingEntity target) {
        if (score(host, slot, target) <= 0) return false;
        host.aimAt(target);
        net.minecraft.sounds.SoundEvent sound;
        if (slot == SkillSlot.ACTIVE_1) {
            Vec3 p = trapPosition(host, target, false);
            if (p == null) return false;
            var trap = new GizmoSmokeTrapEntity(ModEntities.GIZMO_SMOKE_TRAP.get(), host.serverLevel(), host.body(), Direction.UP);
            trap.setPos(p);
            if (!host.serverLevel().addFreshEntity(trap)) return false;
            smoke--; trapDelay = 60;
            sound = ModSounds.GIZMO_SMOKE_TRAP_DEPLOY.get();
        } else if (slot == SkillSlot.ACTIVE_2) {
            Vec3 p = trapPosition(host, target, true);
            if (p == null) return false;
            var trap = new GizmoSpiderNestTrapEntity(ModEntities.GIZMO_SPIDER_NEST_TRAP.get(), host.serverLevel(), host.body());
            trap.setPos(p);
            if (!host.serverLevel().addFreshEntity(trap)) return false;
            nests--; trapDelay = 60;
            sound = ModSounds.GIZMO_SPIDER_NEST_DEPLOY.get();
        } else {
            Vec3 p = crawlerPosition(host, target);
            if (p == null) return false;
            var crawler = new GizmoTBoyEntity(ModEntities.GIZMO_T_BOY.get(), host.serverLevel(), host.body());
            crawler.setPos(p);
            crawler.setTravelDirection(target.position().subtract(p));
            if (!host.serverLevel().addFreshEntity(crawler)) return false;
            coreCooldown = Math.max(1, GizmoStateManager.CORE_COOLDOWN_TICKS);
            sound = ModSounds.GIZMO_T_BOY_DEPLOY.get();
        }
        host.serverLevel().playSound(null, host.body().blockPosition(), sound, SoundSource.NEUTRAL, 0.9F, 1);
        return true;
    }

    private static int smokeCount(OperativeHost host) {
        return host.serverLevel().getEntitiesOfClass(GizmoSmokeTrapEntity.class, host.body().getBoundingBox().inflate(100),
                e -> !e.isRemoved() && e.isOwnedBy(host.body().getUUID())).size();
    }
    private static int nestCount(OperativeHost host) {
        return host.serverLevel().getEntitiesOfClass(GizmoSpiderNestTrapEntity.class, host.body().getBoundingBox().inflate(100),
                e -> !e.isRemoved() && e.isOwnedBy(host.body().getUUID())).size();
    }
    private static boolean nearOwnTrap(OperativeHost host) {
        double radius = Math.max(0, GizmoStateManager.PASSIVE_RADIUS);
        var box = host.body().getBoundingBox().inflate(radius);
        return !host.serverLevel().getEntitiesOfClass(GizmoSmokeTrapEntity.class, box,
                e -> !e.isRemoved() && e.isOwnedBy(host.body().getUUID()) && e.distanceToSqr(host.body()) <= radius * radius).isEmpty()
                || !host.serverLevel().getEntitiesOfClass(GizmoSpiderNestTrapEntity.class, box,
                e -> !e.isRemoved() && e.isOwnedBy(host.body().getUUID()) && e.distanceToSqr(host.body()) <= radius * radius).isEmpty();
    }

    private static Vec3 trapPosition(OperativeHost host, LivingEntity target, boolean nest) {
        Vec3 toward = target.position().subtract(host.body().position()).multiply(1, 0, 1);
        if (toward.lengthSqr() < 2.25D) return null;
        double range = nest ? GizmoPlacementHelper.SPIDER_RANGE : GizmoPlacementHelper.SMOKE_RANGE;
        double trigger = nest ? GizmoSpiderNestTrapEntity.nativeTriggerRadius() : GizmoSmokeTrapEntity.nativeTriggerRadius();
        for (double d = Math.min(nest ? range - 0.75D : 2.5D, toward.length() - 1); d >= 0.75D; d -= 0.75D) {
            Vec3 point = host.body().position().add(toward.normalize().scale(d));
            var hit = host.serverLevel().clip(new ClipContext(point.add(0, 1.25D, 0), point.add(0, -1.25D, 0),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, host.body()));
            if (hit.getType() != HitResult.Type.BLOCK || hit.getDirection() != Direction.UP) continue;
            Vec3 p = hit.getLocation().add(0, 0.025D, 0);
            if (!host.withinOrders(p) || nest && p.distanceToSqr(target.position()) > trigger * trigger
                    || p.distanceToSqr(host.body().getEyePosition()) > range * range) continue;
            if (host.serverLevel().clip(new ClipContext(host.body().getEyePosition(), p.add(0, 0.1D, 0),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, host.body())).getType() != HitResult.Type.MISS) continue;
            AABB bounds = new AABB(p.x - 0.22D, p.y, p.z - 0.22D, p.x + 0.22D, p.y + 0.25D, p.z + 0.22D);
            if (!host.serverLevel().noCollision(bounds) || !host.serverLevel().getEntitiesOfClass(LivingEntity.class, bounds.inflate(0.1D), LivingEntity::isAlive).isEmpty()) continue;
            if (!host.serverLevel().getEntitiesOfClass(GizmoSmokeTrapEntity.class, bounds.inflate(0.6D)).isEmpty()
                    || !host.serverLevel().getEntitiesOfClass(GizmoSpiderNestTrapEntity.class, bounds.inflate(0.6D)).isEmpty()) continue;
            return p;
        }
        return null;
    }

    private static Vec3 crawlerPosition(OperativeHost host, LivingEntity target) {
        Vec3 direction = target.position().subtract(host.body().position()).multiply(1, 0, 1).normalize();
        if (direction.lengthSqr() < 0.1D) return null;
        Vec3 p = host.body().position().add(direction.scale(0.85D)).add(0, 0.05D, 0);
        AABB box = new AABB(p.x - 0.18D, p.y, p.z - 0.18D, p.x + 0.18D, p.y + 0.3D, p.z + 0.18D);
        return host.withinOrders(p) && host.serverLevel().noCollision(box)
                && host.serverLevel().getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive).isEmpty() ? p : null;
    }

    private static void updatePassive(OperativeHost host, boolean active) {
        var move = host.body().getAttribute(Attributes.MOVEMENT_SPEED);
        var attack = host.body().getAttribute(Attributes.ATTACK_SPEED);
        if (move != null) {
            if (active && move.getModifier(GizmoStateManager.PASSIVE_MOVE_UUID) == null)
                move.addTransientModifier(new AttributeModifier(GizmoStateManager.PASSIVE_MOVE_UUID, 0.1D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            else if (!active) move.removeModifier(GizmoStateManager.PASSIVE_MOVE_UUID);
        }
        if (attack != null) {
            if (active && attack.getModifier(GizmoStateManager.PASSIVE_ATTACK_UUID) == null)
                attack.addTransientModifier(new AttributeModifier(GizmoStateManager.PASSIVE_ATTACK_UUID, 0.1D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            else if (!active) attack.removeModifier(GizmoStateManager.PASSIVE_ATTACK_UUID);
        }
    }
    @Override public double meleeIntervalTicks(OperativeHost host) {
        return 20 / Math.max(0.1D, host.body().getAttributeValue(Attributes.ATTACK_SPEED));
    }
    @Override public void remember(LivingEntity target, int ticks) {
        if (target != null && revealed.size() < 64) revealed.merge(target.getUUID(), Math.max(1, Math.min(200, ticks)), Math::max);
    }
    @Override public boolean isRevealed(LivingEntity target) { return target != null && revealed.getOrDefault(target.getUUID(), 0) > 0; }
    @Override public boolean busy() { return false; }
    @Override public void cancel(OperativeHost host) { updatePassive(host, false); revealed.clear(); }
    @Override public CompoundTag save() {
        var tag = new CompoundTag();
        tag.putInt("SmokeCharges", smoke); tag.putInt("NestCharges", nests);
        tag.putInt("SmokeRecharge", smokeRecharge); tag.putInt("NestRecharge", nestRecharge);
        tag.putInt("CoreCooldown", coreCooldown); tag.putInt("TrapDelay", trapDelay);
        return tag;
    }
    @Override public void load(CompoundTag tag) {
        smoke = bound(tag, "SmokeCharges", smokeCapacity());
        nests = bound(tag, "NestCharges", nestCapacity());
        smokeRecharge = bound(tag, "SmokeRecharge", GizmoStateManager.SMOKE_RECHARGE_TICKS);
        nestRecharge = bound(tag, "NestRecharge", GizmoStateManager.SPIDER_RECHARGE_TICKS);
        coreCooldown = bound(tag, "CoreCooldown", GizmoStateManager.CORE_COOLDOWN_TICKS);
        trapDelay = bound(tag, "TrapDelay", 60);
        revealed.clear();
    }
    private static int bound(CompoundTag tag, String key, int max) { return Math.max(0, Math.min(Math.max(0, max), tag.getInt(key))); }
    @Override public String describe() { return "smoke=" + smoke + ", nests=" + nests + ", tboy=" + coreCooldown; }
    @Override public void refresh(OperativeHost host) {
        var fresh = new NativeGizmoSkills(capacityScale);
        coreCooldown = fresh.coreCooldown;
        nestRecharge = fresh.nestRecharge;
        nests = fresh.nests;
        smoke = fresh.smoke;
        smokeRecharge = fresh.smokeRecharge;
    }
}
