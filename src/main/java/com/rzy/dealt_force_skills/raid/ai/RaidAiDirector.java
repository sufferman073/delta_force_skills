package com.rzy.dealt_force_skills.raid.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.rzy.dealt_force_skills.entity.OperativeEntity;
import com.rzy.dealt_force_skills.raid.RaidTemplate;
import com.rzy.dealt_force_skills.raid.rules.*;
import com.rzy.dealt_force_skills.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;

/** One director per instance. The owner ticks it on the server thread and journals save(). */
public final class RaidAiDirector {
    private static final TicketType<UUID> ACTOR_TICKET = TicketType.create("dfs_raid_actor", Comparator.comparing(UUID::toString));
    public record HumanTarget(UUID player, UUID team, Vec3 position, boolean active) { }
    /** highLoot must cache by receipt; crate must be idempotent by receipt. checkpoint saves the whole raid journal. */
    public interface DeathLoot {
        List<ItemStack> highLoot(UUID receipt, Vec3 position);
        void checkpoint(CompoundTag director);
        void crate(UUID receipt, Vec3 position, List<ItemStack> equipmentAndLoot);
    }
    private final ServerLevel level;
    private final UUID instance;
    private final RaidTemplate template;
    private final BlockPos origin;
    private final Map<String, JsonObject> markers = new LinkedHashMap<>();
    private final List<Squad> squads = new ArrayList<>();
    private final List<NpcPoint> npcs = new ArrayList<>();
    private final Map<UUID, PendingCrate> pending = new LinkedHashMap<>();
    private final Map<UUID, BlockPos> actorPositions = new LinkedHashMap<>();
    private final Set<ChunkPos> tickets = new HashSet<>();
    private final Random random;
    private DeathLoot deathLoot;
    private Function<UUID, LivingEntity> targetResolver;
    private boolean spawned;
    private boolean closed;
    private Vec3 rally;
    private long nextDecision;
    private final Set<ChunkPos> mapTickets = new HashSet<>();
    private int mapTicketCursor, readyCursor, warmedCursor = -1;
    private long rallyUntil;
    public boolean holdMap() {
        level.resetEmptyTime();
        int columns = ((origin.getX() + template.sizeX() - 1) >> 4) - (origin.getX() >> 4) + 1;
        int rows = ((origin.getZ() + template.sizeZ() - 1) >> 4) - (origin.getZ() >> 4) + 1;
        for (int batch = 0; batch < 4 && mapTicketCursor < columns*rows; batch++, mapTicketCursor++) {
            ChunkPos pos = new ChunkPos((origin.getX()>>4) + mapTicketCursor%columns, (origin.getZ()>>4) + mapTicketCursor/columns);
            level.getChunkSource().addRegionTicket(MAP_TICKET, pos, 2, instance, true); mapTickets.add(pos);
        }
        while (readyCursor < mapTicketCursor) {
            ChunkPos next = new ChunkPos((origin.getX() >> 4) + readyCursor % columns, (origin.getZ() >> 4) + readyCursor / columns);
            if (!level.isPositionEntityTicking(next.getWorldPosition())) {
                if (warmedCursor != readyCursor) {
                    warmedCursor = readyCursor;
                    for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
                        int chunkX = next.x + dx, chunkZ = next.z + dz;
                        if (level.getChunkSource().getChunkNow(chunkX, chunkZ) == null) level.getChunk(chunkX, chunkZ);
                    }
                }
                break;
            }
            readyCursor++;
        }
        return readyCursor == columns * rows;
    }
    private static final TicketType<UUID> MAP_TICKET = TicketType.create("dfs_raid_map", Comparator.comparing(UUID::toString));
    private static final class Member {
        final UUID id; final RaidAiRules.Member rule; boolean spawned, dead;
        Member(UUID id, RaidAiRules.Member rule) { this.id = id; this.rule = rule; }
    }
    private static final class Squad {
        final UUID id; final String spawn; final Vec3 home; final RaidAiRules.Team rule; final List<Member> members = new ArrayList<>();
        UUID target; long repairAt, cooldownUntil, progressAt, patrolAt; double bestDistance = Double.MAX_VALUE; Vec3 waypoint;
        Squad(UUID id, String spawn, Vec3 home, RaidAiRules.Team rule) { this.id = id; this.spawn = spawn; this.home = home; this.rule = rule; }
    }
    private static final class NpcPoint {
        final JsonObject marker; final Set<UUID> living = new LinkedHashSet<>(); long nextWave; boolean fired;
        NpcPoint(JsonObject marker) { this.marker = marker; }
    }
    private record Cluster(UUID team, List<HumanTarget> people) { Vec3 position() { return people.get(0).position(); } }
    private record PendingCrate(UUID receipt, Vec3 position, List<ItemStack> items) { }

    public RaidAiDirector(ServerLevel level, UUID instance, String mapId, BlockPos origin, RaidSpawnRules.Plan plan) {
        this.level = Objects.requireNonNull(level); this.instance = Objects.requireNonNull(instance);
        this.template = RaidTemplate.load(mapId); this.origin = origin.immutable();
        this.random = new Random(instance.getMostSignificantBits() ^ instance.getLeastSignificantBits());
        this.targetResolver = id -> level.getEntity(id) instanceof LivingEntity e ? e : null;
        for (JsonObject marker : template.markers()) {
            if (markers.putIfAbsent(marker.get("id").getAsString(), marker) != null) throw new IllegalArgumentException("Duplicate marker");
            String kind = marker.get("kind").getAsString();
            if (mapId.equals("zero_dam") && (kind.equals("spawn.continuous") || kind.equals("spawn.special_once"))) npcs.add(new NpcPoint(marker));
        }
        Set<String> occupied = new HashSet<>();
        for (RaidSpawnRules.HumanAssignment human : plan.humans()) {
            JsonObject marker = spawnMarker(human.spawn().id());
            String side = marker.getAsJsonObject("config").get("side").getAsString();
            if (!side.equalsIgnoreCase("player") || !occupied.add(human.spawn().id())) throw new IllegalArgumentException("Human spawn eligibility mismatch");
        }
        for (RaidSpawnRules.AiAssignment assigned : plan.ai()) {
            JsonObject marker = spawnMarker(assigned.spawn().id());
            if (!occupied.add(assigned.spawn().id())) throw new IllegalArgumentException("Spawn reused by two squads");
            RaidAiRules.Team rule = RaidAiRules.select(assigned.members(), random);
            UUID team = uuid("team/" + assigned.spawn().id());
            Squad squad = new Squad(team, assigned.spawn().id(), position(marker), rule);
            for (int i = 0; i < rule.members().size(); i++) squad.members.add(new Member(uuid("member/" + assigned.spawn().id() + "/" + i), rule.members().get(i)));
            squads.add(squad);
        }
    }
    public void deathLoot(DeathLoot callback) { this.deathLoot = Objects.requireNonNull(callback); }
    /** Offline mannequins resolve from the original participant UUID through this hook. */
    public void targetResolver(Function<UUID, LivingEntity> resolver) { this.targetResolver = Objects.requireNonNull(resolver); }
    private UUID uuid(String suffix) { return UUID.nameUUIDFromBytes((instance + "/" + suffix).getBytes(StandardCharsets.UTF_8)); }
    private JsonObject spawnMarker(String id) {
        JsonObject marker = markers.get(id);
        if (marker == null || !marker.get("kind").getAsString().equals("spawn.team")) throw new IllegalArgumentException("Missing team spawn " + id);
        return marker;
    }
    private Vec3 position(JsonObject marker) {
        JsonArray p = marker.getAsJsonArray("localPos");
        Vec3 result = new Vec3(origin.getX() + p.get(0).getAsDouble(), origin.getY() + p.get(1).getAsDouble(), origin.getZ() + p.get(2).getAsDouble());
        if (!inside(result)) throw new IllegalArgumentException("Marker outside raid bounds: " + marker.get("id"));
        return result;
    }
    private boolean inside(Vec3 p) { return p.x >= origin.getX() && p.x < origin.getX() + template.sizeX()
            && p.y >= origin.getY() && p.y < origin.getY() + template.sizeY() && p.z >= origin.getZ() && p.z < origin.getZ() + template.sizeZ(); }
    private void bind(Entity entity, UUID team, boolean npc) { RaidAiAffinity.bindAi(entity, instance, team, npc, origin, template.sizeX(), template.sizeY(), template.sizeZ()); }

    /** Prepares all actors before publishing any; no unsupported character is silently rerolled. */
    public void spawnAll() {
        if (closed) throw new IllegalStateException("Raid director is closed");
        if (spawned) return;
        Objects.requireNonNull(deathLoot, "Raid death loot callback must be installed before spawning");
        Map<Member, OperativeEntity> prepared = new LinkedHashMap<>();
        for (Squad squad : squads) for (Member member : squad.members) {
            if (member.dead || member.spawned) continue;
            if (level.getEntity(member.id) != null) throw new IllegalStateException("Duplicate operative UUID " + member.id);
            OperativeEntity actor = ModEntities.OPERATIVE.get().create(level);
            if (actor == null) throw new IllegalStateException("Cannot create raid operative");
            actor.setUUID(member.id); actor.moveTo(squad.home.x, squad.home.y, squad.home.z, 0, 0);
            bind(actor, squad.id, false);
            String character = RaidAiRules.characterId(member.rule.authoredName()).orElseThrow();
            if (!actor.setCharacter(character)) throw new IllegalStateException("Missing native executor " + character);
            actor.setOwnerUUID(null); actor.setGuarding(true); actor.setPersistenceRequired();
            com.rzy.dealt_force_skills.ai.operative.OperativeSettings.apply(actor);
            actor.setHealth(actor.getMaxHealth());
            RaidAiAffinity.order(actor, squad.home, false);
            RaidAiEquipment.equip(actor, member.rule.equipment(), new Random(member.id.getLeastSignificantBits()));
            RaidAiEquipment.refreshEffects(actor, squad.rule.persistentEffects()); prepared.put(member, actor);
        }
        for (OperativeEntity actor : prepared.values()) actorPositions.put(actor.getUUID(), actor.blockPosition());
        updateTickets();
        for (ChunkPos chunk : tickets) level.getChunk(chunk.x, chunk.z);
        List<OperativeEntity> added = new ArrayList<>();
        try {
            for (var entry : prepared.entrySet()) {
                if (!level.addFreshEntity(entry.getValue())) throw new IllegalStateException("Raid operative spawn rejected");
                added.add(entry.getValue());
            }
        } catch (RuntimeException failure) { added.forEach(Entity::discard); prepared.values().forEach(a -> actorPositions.remove(a.getUUID())); updateTickets(); throw failure; }
        for (Member member : prepared.keySet()) member.spawned = true;
        spawned = true; deathLoot.checkpoint(save());
    }
    public void rally(Vec3 extraction, long endsAtTick) {
        if (!inside(extraction)) throw new IllegalArgumentException("Rally point outside instance");
        rally = extraction; rallyUntil = endsAtTick; nextDecision = 0;
        for (Squad squad : squads) { squad.target = null; for (OperativeEntity actor : loaded(squad)) { RaidAiAffinity.focus(actor, List.of()); RaidAiAffinity.order(actor, rally, true); actor.setTarget(null); } }
    }
    public void tick(List<HumanTarget> targets, long now) {
        if (!spawned || closed) return;
        updateTickets();
        flushCrates();
        List<HumanTarget> active = targets.stream().filter(t -> t.active() && inside(t.position())).sorted(Comparator.comparing(t -> t.player().toString())).toList();
        // Region tickets load actors, but an empty dimension still sleeps after 300 ticks.
        // Only an active roster in this live instance keeps natural entity ticks running.
        var owner = com.rzy.dealt_force_skills.raid.RaidInstanceData.get(level.getServer()).instance(instance);
        if (!active.isEmpty() && owner != null && owner.phase == com.rzy.dealt_force_skills.raid.RaidInstanceData.Phase.READY
                && owner.error.isEmpty() && level.dimension().equals(com.rzy.dealt_force_skills.raid.RaidInstances.DIMENSION)) level.resetEmptyTime();
        for (Squad squad : squads) {
            for (OperativeEntity actor : loaded(squad)) {
                if (!actor.isAlive()) { onDeath(actor); continue; }
                if (now % 20 == 0) RaidAiEquipment.refreshEffects(actor, squad.rule.persistentEffects());
                if (now >= squad.repairAt) RaidAiEquipment.repairWornArmor(actor, squad.rule.wornArmorRepairFraction());
            }
            if (now >= squad.repairAt) squad.repairAt = now + squad.rule.wornArmorRepairIntervalTicks();
        }
        if (now >= nextDecision) { nextDecision = now + 20; decisions(active, now); tickNpcs(active, now); }
    }
    private List<OperativeEntity> loaded(Squad squad) {
        List<OperativeEntity> result = new ArrayList<>();
        for (Member member : squad.members) if (!member.dead && level.getEntity(member.id) instanceof OperativeEntity actor) result.add(actor);
        return result;
    }
    private List<Cluster> clusters(List<HumanTarget> active) {
        List<Cluster> result = new ArrayList<>();
        for (HumanTarget human : active) {
            Cluster near = result.stream().filter(c -> c.team().equals(human.team()) && c.position().distanceToSqr(human.position()) <= 48 * 48).findFirst().orElse(null);
            if (near == null) result.add(new Cluster(human.team(), new ArrayList<>(List.of(human)))); else near.people().add(human);
        }
        return result;
    }
    private void decisions(List<HumanTarget> active, long now) {
        if (rally != null && now >= rallyUntil) rally = null;
        if (rally != null) {
            for (Squad squad : squads) for (OperativeEntity actor : loaded(squad)) {
                boolean moving = actor.position().distanceToSqr(rally) > 4 * 4;
                var defenders = active.stream().filter(target -> target.position().distanceToSqr(rally) <= 32 * 32)
                        .map(target -> targetResolver.apply(target.player())).filter(Objects::nonNull).filter(LivingEntity::isAlive).toList();
                RaidAiAffinity.focus(actor, defenders.stream().map(Entity::getUUID).toList());
                LivingEntity contact = defenders.stream().filter(actor::isEnemy).min(Comparator.comparingDouble(actor::distanceToSqr)).orElse(null);
                RaidAiAffinity.order(actor, rally, moving && contact == null);
                actor.setTarget(contact);
                if (moving && contact == null) navigate(actor, rally);
            }
            return;
        }
        List<Cluster> groups = clusters(active); Map<Squad, Cluster> assignments = new LinkedHashMap<>(); Set<Cluster> occupied = new HashSet<>();
        // Retain a whole squad's current side when a human team splits.
        for (Squad squad : squads) if (squad.target != null && now >= squad.cooldownUntil && !loaded(squad).isEmpty()) {
            Cluster group = groups.stream().filter(c -> c.people().stream().anyMatch(h -> h.player().equals(squad.target))).findFirst().orElse(null);
            if (group != null && !makingProgress(squad, group, now)) squad.cooldownUntil = now + 200;
        }
        for (Cluster group : groups) if (!occupied.contains(group)) {
            Squad closest = squads.stream().filter(s -> !assignments.containsKey(s) && !loaded(s).isEmpty())
                    .min(Comparator.<Squad, Boolean>comparing(s -> now < s.cooldownUntil).thenComparingDouble(s -> loaded(s).stream().mapToDouble(actor -> actor.distanceToSqr(group.position())).min().orElse(Double.MAX_VALUE))).orElse(null);
            if (closest != null) { UUID target = group.people().get(0).player(); if (!target.equals(closest.target)) { closest.progressAt = now; closest.bestDistance = Double.MAX_VALUE; } closest.target = target; assignments.put(closest, group); occupied.add(group); }
        }
        for (Squad squad : squads) {
            Cluster group = assignments.get(squad);
            if (group == null) { if (squad.target != null) loaded(squad).forEach(actor -> actor.setTarget(null)); squad.target = null; patrol(squad, now); continue; }
            LivingEntity contact = targetResolver.apply(squad.target);
            List<UUID> focus = group.people().stream().map(h -> targetResolver.apply(h.player())).filter(Objects::nonNull).map(Entity::getUUID).toList();
            for (OperativeEntity actor : loaded(squad)) {
                RaidAiAffinity.focus(actor, focus); RaidAiAffinity.order(actor, group.position(), false);
                if (contact != null && contact.isAlive() && actor.isEnemy(contact)) actor.setTarget(contact); else actor.setTarget(null);
            }
        }
    }
    private boolean makingProgress(Squad squad, Cluster group, long now) {
        List<OperativeEntity> actors = loaded(squad);
        double distance = actors.stream().mapToDouble(a -> a.position().distanceToSqr(group.position())).min().orElse(Double.MAX_VALUE);
        boolean contact = actors.stream().anyMatch(a -> { LivingEntity e = targetResolver.apply(squad.target); return e != null && a.hasLineOfSight(e) && a.distanceToSqr(e) < 64 * 64; });
        if (contact || distance + 9 < squad.bestDistance) { squad.bestDistance = distance; squad.progressAt = now; }
        return now - squad.progressAt < 600; // Thirty seconds without progress or contact releases the whole team.
    }
    private void patrol(Squad squad, long now) {
        List<OperativeEntity> actors = loaded(squad); if (actors.isEmpty()) return;
        if (squad.waypoint == null || now >= squad.patrolAt) {
            squad.patrolAt = now + 160; squad.waypoint = squad.home;
            for (int attempt = 0; attempt < 8; attempt++) {
                Vec3 point = squad.home.add(random.nextInt(25) - 12, 0, random.nextInt(25) - 12);
                if (inside(point) && actors.get(0).getNavigation().createPath(BlockPos.containing(point), 2) != null) { squad.waypoint = point; break; }
            }
        }
        for (OperativeEntity actor : actors) { RaidAiAffinity.focus(actor, List.of()); RaidAiAffinity.order(actor, squad.waypoint, false); if (actor.getTarget() == null) navigate(actor, squad.waypoint); }
    }
    private void navigate(OperativeEntity actor, Vec3 destination) {
        destination = com.rzy.dealt_force_skills.raid.RaidCables.route(actor, destination);
        Vec3 delta = destination.subtract(actor.position());
        Vec3 step = delta.lengthSqr() > 48 * 48 ? actor.position().add(delta.normalize().scale(48)) : destination;
        if (inside(step)) actor.getNavigation().moveTo(step.x, step.y, step.z, 1.05);
    }
    private void tickNpcs(List<HumanTarget> active, long now) {
        for (NpcPoint point : npcs) {
            JsonObject marker = point.marker, config = marker.getAsJsonObject("config");
            boolean once = marker.get("kind").getAsString().equals("spawn.special_once");
            Vec3 pos = position(marker); double radius = config.get("activationRadius").getAsDouble();
            if (once && point.fired || now < point.nextWave || active.stream().noneMatch(t -> t.position().distanceToSqr(pos) <= radius * radius)) continue;
            int maximum = once ? config.get("count").getAsInt() : config.get("maxAlive").getAsInt();
            int wave = once ? maximum : config.get("countPerWave").getAsInt();
            int count = Math.min(wave, maximum - point.living.size()); if (count <= 0) continue;
            ResourceLocation id = ResourceLocation.tryParse(config.get("entity").getAsString());
            if (id == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(id)) throw new IllegalStateException("Missing marked NPC " + config.get("entity"));
            List<Mob> prepared = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                Entity entity = BuiltInRegistries.ENTITY_TYPE.get(id).create(level);
                if (!(entity instanceof Mob mob)) throw new IllegalStateException("NPC marker is not a mob: " + id);
                mob.moveTo(pos.x, pos.y, pos.z, 0, 0);
                mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()), MobSpawnType.EVENT, null, null);
                bind(mob, uuid("npc/" + marker.get("id").getAsString()), true); mob.setPersistenceRequired(); prepared.add(mob);
            }
            for (Mob mob : prepared) actorPositions.put(mob.getUUID(), mob.blockPosition());
            updateTickets();
            level.getChunkAt(BlockPos.containing(pos));
            List<Mob> added = new ArrayList<>();
            try { for (Mob mob : prepared) { if (!level.addFreshEntity(mob)) throw new IllegalStateException("NPC spawn rejected"); added.add(mob); } }
            catch (RuntimeException error) { added.forEach(Entity::discard); prepared.forEach(m -> actorPositions.remove(m.getUUID())); updateTickets(); throw error; }
            for (Mob mob : added) point.living.add(mob.getUUID());
            point.fired = true; point.nextWave = once ? Long.MAX_VALUE : now + Math.max(1, config.get("intervalTicks").getAsInt());
            deathLoot.checkpoint(save());
        }
    }
    /** Call only after all revive/death-cancellation handlers accept final death. */
    public boolean onDeath(LivingEntity entity) {
        if (!instance.equals(RaidAiAffinity.instance(entity))) return false;
        for (NpcPoint point : npcs) if (point.living.remove(entity.getUUID())) { actorPositions.remove(entity.getUUID()); updateTickets(); deathLoot.checkpoint(save()); return false; }
        for (Squad squad : squads) for (Member member : squad.members) if (member.id.equals(entity.getUUID())) {
            if (member.dead) return true;
            if (!(entity instanceof OperativeEntity actor)) throw new IllegalStateException("Raid operative UUID changed entity type");
            UUID receipt = uuid("death/" + member.id); List<ItemStack> contents = new ArrayList<>(RaidAiEquipment.copyDeathEquipment(actor));
            contents.addAll(deathLoot.highLoot(receipt, actor.position()).stream().map(ItemStack::copy).toList());
            member.dead = true; actorPositions.remove(entity.getUUID()); updateTickets(); pending.put(receipt, new PendingCrate(receipt, actor.position(), List.copyOf(contents)));
            deathLoot.checkpoint(save());
            for (EquipmentSlot slot : EquipmentSlot.values()) actor.setItemSlot(slot, ItemStack.EMPTY);
            flushCrates(); return true;
        }
        return false;
    }
    private void flushCrates() {
        for (PendingCrate crate : new ArrayList<>(pending.values())) {
            deathLoot.crate(crate.receipt(), crate.position(), crate.items().stream().map(ItemStack::copy).toList());
            pending.remove(crate.receipt()); deathLoot.checkpoint(save());
        }
    }
    /** Separate region-ticket keys preserve overlapping tickets owned by other live instances. */
    private void updateTickets() {
        Set<ChunkPos> needed = new HashSet<>();
        for (var entry : actorPositions.entrySet()) {
            Entity actor = level.getEntity(entry.getKey());
            if (actor != null && !actor.isRemoved()) entry.setValue(actor.blockPosition());
            // Null means unloaded, not dead. Keep its saved location ticket until the death event.
            needed.add(new ChunkPos(entry.getValue()));
        }
        for (ChunkPos chunk : needed) if (tickets.add(chunk)) level.getChunkSource().addRegionTicket(ACTOR_TICKET, chunk, 2, instance);
        for (ChunkPos chunk : new HashSet<>(tickets)) if (!needed.contains(chunk)) {
            level.getChunkSource().removeRegionTicket(ACTOR_TICKET, chunk, 2, instance); tickets.remove(chunk);
        }
    }
    /** Invoke before region recycling, after the match has finished all participant settlements. */
    public void close() {
        closed = true;
        for (ChunkPos pos : mapTickets) level.getChunkSource().removeRegionTicket(MAP_TICKET, pos, 2, instance, true);
        mapTickets.clear();
        for (ChunkPos chunk : tickets) level.getChunkSource().removeRegionTicket(ACTOR_TICKET, chunk, 2, instance);
        tickets.clear();
    }
    public int heldTicketCount() { return tickets.size(); }
    public CompoundTag save() {
        CompoundTag root = new CompoundTag(); root.putInt("schema", 1); root.putUUID("instance", instance);
        root.putString("map", template.id()); root.putIntArray("origin", new int[]{origin.getX(), origin.getY(), origin.getZ()}); root.putBoolean("spawned", spawned);
        if (rally != null) { root.put("rally", vector(rally)); root.putLong("rallyUntil", rallyUntil); }
        ListTag teams = new ListTag();
        for (Squad squad : squads) {
            CompoundTag tag = new CompoundTag(); tag.putUUID("id", squad.id); tag.putString("spawn", squad.spawn);
            if (squad.target != null) tag.putUUID("target", squad.target); tag.putLong("repairAt", squad.repairAt); tag.putLong("cooldown", squad.cooldownUntil);
            tag.putLong("progressAt", squad.progressAt); tag.putDouble("distance", squad.bestDistance);
            ListTag members = new ListTag();
            for (Member member : squad.members) { CompoundTag m = new CompoundTag(); m.putUUID("id", member.id); m.putString("name", member.rule.authoredName()); m.putBoolean("spawned", member.spawned); m.putBoolean("dead", member.dead); members.add(m); }
            tag.put("members", members); teams.add(tag);
        }
        root.put("squads", teams); ListTag npcTags = new ListTag();
        for (NpcPoint point : npcs) { CompoundTag tag = new CompoundTag(); tag.putString("id", point.marker.get("id").getAsString()); tag.putBoolean("fired", point.fired); tag.putLong("next", point.nextWave); ListTag ids = new ListTag(); point.living.forEach(id -> ids.add(NbtUtils.createUUID(id))); tag.put("living", ids); npcTags.add(tag); }
        root.put("npcs", npcTags); ListTag crates = new ListTag();
        for (PendingCrate crate : pending.values()) { CompoundTag tag = new CompoundTag(); tag.putUUID("id", crate.receipt()); tag.put("pos", vector(crate.position())); ListTag items = new ListTag(); crate.items().forEach(item -> items.add(item.save(new CompoundTag()))); tag.put("items", items); crates.add(tag); }
        root.put("pendingCrates", crates);
        ListTag positions = new ListTag();
        for (var entry : actorPositions.entrySet()) { CompoundTag tag = new CompoundTag(); tag.putUUID("id", entry.getKey()); tag.putLong("pos", entry.getValue().asLong()); positions.add(tag); }
        root.put("actorPositions", positions); return root;
    }
    /** Construct from the same immutable Plan, then load. Unloaded members are never treated as dead or respawned. */
    public void load(CompoundTag root) {
        if (root.getInt("schema") != 1 || !root.hasUUID("instance") || !root.getUUID("instance").equals(instance) || !root.getString("map").equals(template.id())
                || !Arrays.equals(root.getIntArray("origin"), new int[]{origin.getX(), origin.getY(), origin.getZ()})) throw new IllegalArgumentException("Director journal identity mismatch");
        ListTag teams = root.getList("squads", Tag.TAG_COMPOUND);
        if (teams.size() != squads.size()) throw new IllegalArgumentException("Director squad count changed");
        for (int i = 0; i < teams.size(); i++) {
            CompoundTag tag = teams.getCompound(i); Squad squad = squads.get(i);
            if (!squad.id.equals(tag.getUUID("id")) || !squad.spawn.equals(tag.getString("spawn"))) throw new IllegalArgumentException("Director roster changed");
            ListTag members = tag.getList("members", Tag.TAG_COMPOUND); if (members.size() != squad.members.size()) throw new IllegalArgumentException("Director roster size changed");
            for (int j = 0; j < members.size(); j++) { CompoundTag m = members.getCompound(j); Member member = squad.members.get(j); if (!member.id.equals(m.getUUID("id")) || !member.rule.authoredName().equals(m.getString("name"))) throw new IllegalArgumentException("Director member changed"); member.dead = m.getBoolean("dead"); member.spawned = m.getBoolean("spawned"); }
            squad.target = tag.hasUUID("target") ? tag.getUUID("target") : null; squad.repairAt = tag.getLong("repairAt"); squad.cooldownUntil = tag.getLong("cooldown"); squad.progressAt = tag.getLong("progressAt"); squad.bestDistance = tag.getDouble("distance");
        }
        for (Tag value : root.getList("npcs", Tag.TAG_COMPOUND)) { CompoundTag tag = (CompoundTag) value; NpcPoint point = npcs.stream().filter(p -> p.marker.get("id").getAsString().equals(tag.getString("id"))).findFirst().orElseThrow(); point.fired = tag.getBoolean("fired"); point.nextWave = tag.getLong("next"); point.living.clear(); for (Tag id : tag.getList("living", Tag.TAG_INT_ARRAY)) point.living.add(NbtUtils.loadUUID(id)); }
        pending.clear(); for (Tag value : root.getList("pendingCrates", Tag.TAG_COMPOUND)) { CompoundTag tag = (CompoundTag) value; UUID receipt = tag.getUUID("id"); List<ItemStack> items = new ArrayList<>(); for (Tag item : tag.getList("items", Tag.TAG_COMPOUND)) items.add(ItemStack.of((CompoundTag) item)); pending.put(receipt, new PendingCrate(receipt, vector(tag.getCompound("pos")), List.copyOf(items))); }
        rallyUntil = root.getLong("rallyUntil"); spawned = root.getBoolean("spawned"); rally = root.contains("rally") ? vector(root.getCompound("rally")) : null;
        actorPositions.clear(); for (Tag value : root.getList("actorPositions", Tag.TAG_COMPOUND)) { CompoundTag tag = (CompoundTag) value; actorPositions.put(tag.getUUID("id"), BlockPos.of(tag.getLong("pos"))); }
        updateTickets();
    }
    private static CompoundTag vector(Vec3 p) { CompoundTag tag = new CompoundTag(); tag.putDouble("x", p.x); tag.putDouble("y", p.y); tag.putDouble("z", p.z); return tag; }
    private static Vec3 vector(CompoundTag tag) { return new Vec3(tag.getDouble("x"), tag.getDouble("y"), tag.getDouble("z")); }
}
