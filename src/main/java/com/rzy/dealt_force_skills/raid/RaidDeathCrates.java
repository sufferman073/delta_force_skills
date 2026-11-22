package com.rzy.dealt_force_skills.raid;

import com.rzy.dealt_force_skills.entity.DeathCrateEntity;
import com.rzy.dealt_force_skills.registry.ModEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

import java.nio.charset.StandardCharsets;
import java.util.*;

/** One receipt produces one entity; loading an old empty crate never refills it. */
public final class RaidDeathCrates {
    private static final TicketType<UUID> TICKET = TicketType.create("dfs_raid_death_crate", UUID::compareTo);
    private record Pending(UUID instance, UUID receipt, UUID entity, Vec3 position, List<ItemStack> items, ChunkPos chunk) { }
    private static final class State {
        final Map<UUID, Pending> pending = new LinkedHashMap<>();
        final Map<UUID, UUID> delivered = new HashMap<>();
    }
    private static final Map<ServerLevel, State> STATES = new WeakHashMap<>();
    private RaidDeathCrates() { }
    public static void create(ServerLevel level, UUID instance, UUID receipt, Vec3 position, List<ItemStack> items) {
        if (level == null) throw new IllegalStateException("Missing death crate dimension");
        State state = STATES.computeIfAbsent(level, ignored -> new State());
        if (state.delivered.containsKey(receipt) || state.pending.containsKey(receipt)) return;
        UUID entity = UUID.nameUUIDFromBytes(("dfs/raid/crate/" + instance + "/" + receipt).getBytes(StandardCharsets.UTF_8));
        ChunkPos chunk = new ChunkPos(net.minecraft.core.BlockPos.containing(position));
        Pending pending = new Pending(instance, receipt, entity, position, items.stream().filter(s -> !s.isEmpty()).map(ItemStack::copy).toList(), chunk);
        state.pending.put(receipt, pending);
        level.getChunkSource().addRegionTicket(TICKET, chunk, 2, receipt);
    }
    public static void tick(ServerLevel level) {
        State state = STATES.get(level); if (state == null) return;
        for (Pending pending : List.copyOf(state.pending.values())) {
            RaidInstanceData.Instance instance = RaidInstanceData.get(level.getServer()).instance(pending.instance);
            if (instance == null || instance.phase != RaidInstanceData.Phase.READY) { release(level, state, pending); continue; }
            level.getChunk(pending.chunk.x, pending.chunk.z);
            if (!level.areEntitiesLoaded(pending.chunk.toLong())) continue;
            Entity existing = level.getEntity(pending.entity);
            if (existing == null) {
                DeathCrateEntity crate = ModEntities.DEATH_CRATE.get().create(level);
                if (crate == null) throw new IllegalStateException("Death crate type could not be created");
                crate.setUUID(pending.entity); crate.setPos(pending.position);
                crate.configureRaid(pending.instance, pending.receipt, pending.items);
                if (!level.addFreshEntity(crate)) throw new IllegalStateException("Death crate entity rejected");
            } else if (!(existing instanceof DeathCrateEntity crate) || !pending.receipt.equals(crate.raidReceipt()))
                throw new IllegalStateException("Death crate receipt conflicts with existing entity");
            state.delivered.put(pending.receipt, pending.instance); release(level, state, pending);
        }
    }
    private static void release(ServerLevel level, State state, Pending pending) {
        level.getChunkSource().removeRegionTicket(TICKET, pending.chunk, 2, pending.receipt); state.pending.remove(pending.receipt);
    }
    public static void close(ServerLevel level, UUID instance) {
        State state = STATES.get(level); if (state == null) return;
        for (Pending pending : List.copyOf(state.pending.values())) if (pending.instance.equals(instance)) release(level, state, pending);
        state.delivered.values().removeIf(instance::equals);
    }
}
