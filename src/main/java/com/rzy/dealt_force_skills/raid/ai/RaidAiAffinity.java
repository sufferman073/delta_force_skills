package com.rzy.dealt_force_skills.raid.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import java.util.UUID;

/** Persistent instance/team identity shared by actors and native projectile allegiance snapshots. */
public final class RaidAiAffinity {
    public static final String IDENTITY = "DfsRaidIdentity";
    private static final String ORDER = "DfsRaidOrder";
    private RaidAiAffinity() { }

    public static void bindParticipant(Entity actor, UUID instance, UUID team, BlockPos origin, int x, int y, int z) {
        bind(actor, instance, team, "human", origin, x, y, z);
    }
    public static void bindAi(Entity actor, UUID instance, UUID team, boolean ordinaryNpc, BlockPos origin, int x, int y, int z) {
        bind(actor, instance, team, ordinaryNpc ? "npc" : "operative", origin, x, y, z);
    }
    private static void bind(Entity actor, UUID instance, UUID team, String kind, BlockPos origin, int x, int y, int z) {
        if (x < 1 || y < 1 || z < 1) throw new IllegalArgumentException("Invalid raid bounds");
        CompoundTag tag = new CompoundTag();
        tag.putUUID("instance", instance); tag.putUUID("team", team); tag.putString("kind", kind);
        tag.putString("dimension", actor.level().dimension().location().toString());
        tag.putIntArray("bounds", new int[]{origin.getX(), origin.getY(), origin.getZ(), x, y, z});
        actor.getPersistentData().put(IDENTITY, tag);
    }
    public static void clear(Entity actor) {
        actor.getPersistentData().remove(IDENTITY); actor.getPersistentData().remove(ORDER);
        actor.getPersistentData().remove("DfsRaidRally"); actor.getPersistentData().remove("DfsRaidFocus");
        actor.getPersistentData().remove("DfsRaidPursuing");
    }
    public static CompoundTag identity(Entity actor) { return identity(actor, 0); }
    private static CompoundTag identity(Entity actor, int depth) {
        if (actor == null || depth > 4) return new CompoundTag();
        CompoundTag own = identity(actor.getPersistentData());
        if (own.hasUUID("instance")) return own;
        if (actor instanceof com.rzy.dealt_force_skills.ai.operative.OperativeAreaSource area) {
            CompoundTag inherited = area.operativeAllegiance().raidIdentity();
            if (inherited.hasUUID("instance")) return inherited;
        }
        if (actor instanceof com.rzy.dealt_force_skills.ai.operative.NativeSaeedGuardEntity guard) {
            CompoundTag inherited = guard.allegiance().raidIdentity();
            if (inherited.hasUUID("instance")) return inherited;
        }
        if (com.rzy.dealt_force_skills.ai.operative.NativeDepartmentSkills.nativeSummon(actor)) {
            CompoundTag inherited = identity(actor.getPersistentData().getCompound(com.rzy.dealt_force_skills.ai.operative.NativeDepartmentSkills.SUMMON));
            if (inherited.hasUUID("instance")) return inherited;
        }
        Entity owner = actor instanceof Projectile projectile ? projectile.getOwner()
                : actor instanceof OwnableEntity owned ? owned.getOwner() : null;
        return owner == actor ? own : identity(owner, depth + 1);
    }
    public static CompoundTag identity(CompoundTag savedRoot) { return savedRoot.getCompound(IDENTITY).copy(); }
    public static void copyIdentity(Entity source, CompoundTag targetRoot) {
        CompoundTag context = identity(source);
        if (context.hasUUID("instance")) targetRoot.put(IDENTITY, context);
        else targetRoot.remove(IDENTITY);
    }
    public static UUID instance(Entity actor) {
        CompoundTag context = identity(actor);
        return context.hasUUID("instance") ? context.getUUID("instance") : null;
    }
    public static boolean isAi(Entity actor) { return isAi(identity(actor)); }
    private static boolean isAi(CompoundTag context) {
        return context.getString("kind").equals("operative") || context.getString("kind").equals("npc");
    }

    /** null means ordinary DFS allegiance remains authoritative. False also blocks cross-instance damage. */
    public static Boolean hostility(Entity source, LivingEntity target) {
        if (target == null) return false;
        CompoundTag context = identity(source);
        if (source != null && isAi(context) && pursuing(source)) {
            var focus = source.getPersistentData().getList("DfsRaidFocus", net.minecraft.nbt.Tag.TAG_INT_ARRAY);
            boolean included = false;
            for (var value : focus) if (net.minecraft.nbt.NbtUtils.loadUUID(value).equals(target.getUUID())) { included = true; break; }
            if (!included) return false;
        }
        return hostility(context, target);
    }
    public static Boolean hostility(CompoundTag sourceIdentity, Entity target) {
        CompoundTag other = identity(target);
        boolean fromRaid = sourceIdentity.hasUUID("instance"), toRaid = other.hasUUID("instance");
        if (!fromRaid && !toRaid) return null;
        if (!fromRaid || !toRaid || target == null || !sourceIdentity.getUUID("instance").equals(other.getUUID("instance"))
                || !sourceIdentity.getString("dimension").equals(target.level().dimension().location().toString())) return false;
        if (isAi(sourceIdentity) && isAi(other)) return false;
        return !sourceIdentity.hasUUID("team") || !other.hasUUID("team")
                || !sourceIdentity.getUUID("team").equals(other.getUUID("team"));
    }
    /** An ally is in the same instance, unlike a harmless actor across an instance boundary. */
    public static boolean allies(Entity source, Entity target) { return allies(identity(source), target); }
    public static boolean allies(CompoundTag context, Entity target) {
        CompoundTag other = identity(target);
        return context.hasUUID("instance") && other.hasUUID("instance")
                && context.getUUID("instance").equals(other.getUUID("instance"))
                && (isAi(context) && isAi(other) || context.hasUUID("team") && other.hasUUID("team")
                && context.getUUID("team").equals(other.getUUID("team")));
    }
    public static boolean inside(Entity actor, Vec3 position) {
        CompoundTag context = identity(actor);
        return inside(context, actor.level().dimension().location().toString(), position);
    }
    public static boolean inside(CompoundTag context, String dimension, Vec3 position) {
        if (!context.hasUUID("instance")) return true;
        int[] bounds = context.getIntArray("bounds");
        return bounds.length == 6 && context.getString("dimension").equals(dimension)
                && position.x >= bounds[0] && position.x < bounds[0] + bounds[3]
                && position.y >= bounds[1] && position.y < bounds[1] + bounds[4]
                && position.z >= bounds[2] && position.z < bounds[2] + bounds[5];
    }
    public static Vec3 order(Entity actor) {
        if (!isAi(actor) || !actor.getPersistentData().contains(ORDER)) return null;
        CompoundTag order = actor.getPersistentData().getCompound(ORDER);
        Vec3 point = new Vec3(order.getDouble("x"), order.getDouble("y"), order.getDouble("z"));
        return inside(actor, point) ? point : null;
    }
    public static void order(Entity actor, Vec3 point, boolean rally) {
        if (!isAi(actor) || !inside(actor, point)) throw new IllegalArgumentException("Order outside raid instance");
        CompoundTag order = new CompoundTag(); order.putDouble("x", point.x); order.putDouble("y", point.y); order.putDouble("z", point.z);
        actor.getPersistentData().put(ORDER, order); actor.getPersistentData().putBoolean("DfsRaidRally", rally);
    }
    public static boolean pursuing(Entity actor) { return actor.getPersistentData().getBoolean("DfsRaidPursuing") || actor.getPersistentData().getBoolean("DfsRaidRally"); }
    public static void focus(Entity actor, java.util.List<UUID> targets) {
        var list = new net.minecraft.nbt.ListTag();
        for (UUID id : targets) list.add(net.minecraft.nbt.NbtUtils.createUUID(id));
        actor.getPersistentData().put("DfsRaidFocus", list);
        actor.getPersistentData().putBoolean("DfsRaidPursuing", !targets.isEmpty());
    }
    /** Cache inherited allegiance before the caster disappears; called when new skill entities join. */
    public static void inherit(Entity entity) {
        CompoundTag context = identity(entity);
        // Legacy native devices expose their snapshot through entity NBT rather than a getter.
        // Resolve only once at join; ordinary hostility queries do not serialize entities.
        if (!context.hasUUID("instance") && entity instanceof com.rzy.dealt_force_skills.ai.operative.OperativeSkillSource) {
            CompoundTag saved = new CompoundTag(); entity.saveWithoutId(saved); context = identity(saved);
        }
        if (context.hasUUID("instance")) entity.getPersistentData().put(IDENTITY, context);
    }
}
