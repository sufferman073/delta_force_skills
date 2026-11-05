package com.rzy.dealt_force_skills.ai.operative;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.OwnableEntity;
import java.util.UUID;

/** Snapshot used by native support projectiles after their caster leaves the world. */
public final class OperativeAllegiance {
    private boolean active;
    private UUID owner;
    private UUID caster;

    public void capture(Entity caster) {
        if (caster instanceof NativeSaeedGuardEntity guard) { copyFrom(guard.allegiance()); return; }
        active = caster instanceof OperativeHost;
        // Keep damage attribution after any caster unloads, without enabling native gadget AI.
        owner = caster instanceof net.minecraft.world.entity.player.Player ? caster.getUUID()
                : caster instanceof OwnableEntity ownable ? ownable.getOwnerUUID() : null;
        this.caster = caster == null ? null : caster.getUUID();
    }

    /** Resolve live owners when available and keep commander/team identity after unloading. */
    public boolean isAlly(net.minecraft.server.level.ServerLevel level, Entity other) {
        Entity source = caster == null ? null : level.getEntity(caster);
        if (source == null && owner != null) source = level.getEntity(owner);
        return isAlly(source, other, level.getServer());
    }

    public boolean isAlly(Entity source, Entity other) {
        return isAlly(source, other, source.getServer());
    }

    private boolean isAlly(Entity source, Entity other, net.minecraft.server.MinecraftServer server) {
        if (!active || other == null) return false;
        if (isCaster(other)) return true;
        if (other instanceof NativeSaeedGuardEntity guard && caster!=null && caster.equals(guard.allegiance().caster())) return true;
        if (NativeDepartmentSkills.nativeSummon(other)) {
            var summon = other.getPersistentData().getCompound(NativeDepartmentSkills.SUMMON);
            if (caster != null && summon.hasUUID("OperativeCaster") && caster.equals(summon.getUUID("OperativeCaster"))) return true;
            if (owner != null && summon.hasUUID("OperativeOwner")) {
                UUID commander = summon.getUUID("OperativeOwner");
                if (owner.equals(commander) || com.rzy.dealt_force_skills.team.DealtTeamManager.areTeammatesById(owner, commander, server)) return true;
            }
        }
        if (other instanceof net.minecraft.world.entity.projectile.Projectile projectile && projectile.getOwner() != null)
            return isAlly(source, projectile.getOwner(), server);
        if (source != null && other instanceof net.minecraft.world.entity.LivingEntity living
                && com.rzy.dealt_force_skills.util.TargetingUtil.shouldSkipFriendlyControl(source, living)) return true;
        UUID otherOwner = other instanceof OperativeSkillSource skill ? skill.operativeOwnerUUID()
                : other instanceof OwnableEntity ownable ? ownable.getOwnerUUID()
                : other instanceof net.minecraft.world.entity.player.Player ? other.getUUID() : null;
        if (otherOwner == null) {
            CompoundTag data = new CompoundTag(); other.saveWithoutId(data);
            otherOwner = data.hasUUID("Owner") ? data.getUUID("Owner") : null;
        }
        if (otherOwner != null && otherOwner.equals(caster)) return true;
        return owner != null && (owner.equals(otherOwner) || owner.equals(other.getUUID())
                || otherOwner != null && com.rzy.dealt_force_skills.team.DealtTeamManager.areTeammatesById(owner, otherOwner, server));
    }

    /** Covers retain commander identity even when their operative owner is unloaded. */
    public boolean isAllyOwner(net.minecraft.server.level.ServerLevel level, UUID candidate) {
        if (!active || candidate == null) return false;
        if (candidate.equals(caster) || candidate.equals(owner)) return true;
        Entity live = level.getEntity(candidate);
        return live != null && isAlly(level, live)
                || owner != null && com.rzy.dealt_force_skills.team.DealtTeamManager.areTeammatesById(owner, candidate, level.getServer());
    }

    public void copyFrom(OperativeAllegiance source) { active = source.active; owner = source.owner; caster = source.caster; }
    /** Native gadgets use the host's threat policy, retaining conservative orphan behavior. */
    public boolean isEnemy(net.minecraft.server.level.ServerLevel level, net.minecraft.world.entity.LivingEntity target) {
        if (!active || !com.rzy.dealt_force_skills.util.TargetingUtil.isTargetableLiving(target) || isAlly(level, target)) return false;
        Entity source = caster == null ? null : level.getEntity(caster);
        return source instanceof OperativeHost host ? host.isEnemy(target) && host.withinCombatRange(target.position())
                : target instanceof net.minecraft.world.entity.monster.Enemy;
    }

    public boolean withinOrders(net.minecraft.server.level.ServerLevel level, net.minecraft.world.phys.Vec3 position) {
        Entity source = caster == null ? null : level.getEntity(caster);
        return !(source instanceof OperativeHost host) || host.withinOrders(position);
    }

    public boolean active() { return active; }
    public UUID owner() { return owner; }
    public UUID caster() { return caster; }
    public boolean isCaster(Entity target) { return target != null && target.getUUID().equals(caster); }
    public void save(CompoundTag tag, int age) {
        tag.putBoolean("OperativeSource", active);
        if (owner != null) tag.putUUID("OperativeOwner", owner);
        if (caster != null) tag.putUUID("OperativeCaster", caster);
        if (active) tag.putInt("OperativeAge", Math.max(0, age));
    }
    public int load(CompoundTag tag, int maxAge) {
        active = tag.getBoolean("OperativeSource");
        owner = tag.hasUUID("OperativeOwner") ? tag.getUUID("OperativeOwner") : null;
        caster = tag.hasUUID("OperativeCaster") ? tag.getUUID("OperativeCaster") : null;
        return Math.max(0, Math.min(Math.max(1, maxAge) + 1, tag.getInt("OperativeAge")));
    }
}
