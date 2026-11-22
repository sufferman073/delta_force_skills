package com.rzy.dealt_force_skills.raid.world;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.rzy.dealt_force_skills.mapeditor.MarkerRules;
import com.rzy.dealt_force_skills.raid.RaidTemplate;
import com.rzy.dealt_force_skills.raid.rules.ExtractionSwitchRules;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/** Server-thread world state owned and persisted by exactly one raid instance. */
public final class RaidWorldState {
    public record Actor(UUID id, Vec3 position, Entity entity) { }
    public interface Callbacks {
        Collection<Actor> actors();
        void extract(UUID actor, String receipt);
        boolean pay(ServerPlayer player, long amount, UUID transaction);
        default boolean canPay(ServerPlayer player, long amount) { return false; }
        double loadRatio(Entity entity);
        void onExtractionOpened(Vec3 extractionPosition, long endsAtTick);
        void broadcast(Component message);
        boolean requireParticipant(ServerPlayer player);
        /** Persist the owning ledger, including this world state, before returning. */
        void changed();
    }
    private record Marker(String id, String kind, Vec3 position, JsonObject config, List<BlockPos> targets) { }
    private record Storage(BlockPos canonical, Container container, List<BlockEntity> blocks) { }
    private record PendingCycle(long number, List<UUID> occupants) {
        private PendingCycle { occupants = List.copyOf(occupants); }
    }
    private record DoorQuote(String marker, long tick) {}
    private final Map<UUID, DoorQuote> quotes = new HashMap<>();
    private static final String LOOT_STAMP = "DFS_RaidLoot";
    private static final int PREPARE_BATCH = 4;
    private final ServerLevel level;
    private final UUID instance;
    private final String mapId;
    private final BlockPos origin;
    private final Map<String, Marker> markers = new LinkedHashMap<>();
    private final Map<BlockPos, String> interactions = new HashMap<>();
    private final List<Marker> loot = new ArrayList<>();
    private final List<ExtractionSwitchRules.Gate> gateDefs = new ArrayList<>();
    private final List<ExtractionSwitchRules.Window> windowDefs = new ArrayList<>();
    private ExtractionSwitchRules gates;
    private final Set<String> preparedMarkers = new LinkedHashSet<>();
    private final Map<Long, CompoundTag> generated = new LinkedHashMap<>();
    private final Map<Long, String> containerDefinitions = new HashMap<>();
    private final Set<String> paidDoors = new LinkedHashSet<>();
    private final Map<String, UUID> doorPayers = new LinkedHashMap<>();
    private final Set<UUID> settledActors = new LinkedHashSet<>();
    private final Map<String, PendingCycle> pendingCycles = new LinkedHashMap<>();
    private boolean prepared;
    private long nextPhysicalSync;

    public RaidWorldState(ServerLevel level, UUID instanceId, String mapId, BlockPos origin) {
        this.level = Objects.requireNonNull(level);
        this.instance = Objects.requireNonNull(instanceId);
        this.mapId = Objects.requireNonNull(mapId);
        this.origin = origin.immutable();
        RaidTemplate template = RaidTemplate.load(mapId);
        for (JsonObject json : template.markers()) {
            String id = json.get("id").getAsString(), kind = json.get("kind").getAsString();
            JsonArray local = json.getAsJsonArray("localPos");
            Vec3 position = new Vec3(origin.getX() + local.get(0).getAsDouble(),
                    origin.getY() + local.get(1).getAsDouble(), origin.getZ() + local.get(2).getAsDouble());
            JsonObject config = json.getAsJsonObject("config");
            Marker marker = new Marker(id, kind, position, config, targets(position, config));
            require(markers.putIfAbsent(id, marker) == null, "Duplicate marker: " + id);
            if (kind.startsWith("loot.")) {
                RaidWorldLogic.Tier.marker(kind);
                require(marker.targets.size() == 1, "Loot marker needs exactly one container: " + id);
                require("each_instance".equals(string(config, "refresh", "each_instance")), "Unsupported loot refresh: " + id);
                double chance = number(config, "spawnChance", 1);
                require(Double.isFinite(chance) && chance >= 0 && chance <= 1, "Invalid loot chance: " + id);
                loot.add(marker);
            } else if (kind.equals("switch.evac")) {
                require("right_click_block".equals(string(config, "interactMode", "right_click_block"))
                        && bool(config, "startCountdownOnInteract", true), "Unsupported switch interaction: " + id);
                int uses = config.has("maxUses") ? config.get("maxUses").getAsInt()
                        : bool(config, "singleUsePerInstance", false) ? 1 : ExtractionSwitchRules.UNLIMITED;
                gateDefs.add(new ExtractionSwitchRules.Gate(id, uses));
                addInteractions(marker);
            } else if (kind.equals("interact.paid_door")) {
                require("right_click_block".equals(string(config, "interactMode", "right_click_block"))
                        && "per_instance".equals(string(config, "unlockMode", "per_instance")), "Unsupported paid door: " + id);
                price(config.getAsJsonObject("price"));
                addInteractions(marker);
            } else if (kind.startsWith("extract.")) {
                zone(marker, position); // Validate shape and dimensions without needing any actor.
                if (kind.equals("extract.timed")) {
                    require("countdown_end".equals(string(config, "extractAt", "countdown_end")), "Unsupported extraction timing: " + id);
                    windowDefs.add(new ExtractionSwitchRules.Window(id, MarkerRules.switchIds(config),
                            MarkerRules.requiredSwitchCount(config), config.get("durationTicks").getAsLong()));
                } else if (kind.equals("extract.conditional")) {
                    require("player_confirm".equals(string(config, "interaction", "player_confirm")), "Unsupported conditional interaction: " + id);
                    String mode = string(config, "conditionMode", "all");
                    require(mode.equals("all") || mode.equals("any"), "Unknown conditionMode: " + id);
                    JsonObject conditions = config.getAsJsonObject("conditions");
                    require(conditions != null && !conditions.entrySet().isEmpty(), "Missing extraction conditions: " + id);
                    for (var entry : conditions.entrySet()) require(Set.of("maxLoadRatio", "payment").contains(entry.getKey()),
                            "Unsupported extraction condition: " + id + "/" + entry.getKey());
                    if (conditions.has("maxLoadRatio")) require(Double.isFinite(conditions.get("maxLoadRatio").getAsDouble())
                            && conditions.get("maxLoadRatio").getAsDouble() >= 0, "Invalid maxLoadRatio: " + id);
                    if (conditions.has("payment")) price(conditions.getAsJsonObject("payment"));
                } else throw new IllegalArgumentException("Unknown extraction kind: " + kind);
            }
            for (BlockPos target : marker.targets) require(target.getX() >= origin.getX() && target.getX() < origin.getX() + template.sizeX()
                    && target.getY() >= origin.getY() && target.getY() < origin.getY() + template.sizeY()
                    && target.getZ() >= origin.getZ() && target.getZ() < origin.getZ() + template.sizeZ(), "Marker target outside instance: " + id);
        }
        for (ExtractionSwitchRules.Gate gate : gateDefs) {
            Set<String> declared = new LinkedHashSet<>(strings(markers.get(gate.id()).config.getAsJsonArray("extractionIds")));
            Set<String> actual = new LinkedHashSet<>();
            for (ExtractionSwitchRules.Window window : windowDefs) if (window.gateIds().contains(gate.id())) actual.add(window.id());
            require(declared.equals(actual), "Nonreciprocal switch/extraction links: " + gate.id());
        }
        gates = new ExtractionSwitchRules(gateDefs, windowDefs);
    }

    /** Call after template placement, before admission. At most four marked containers per invocation. */
    public boolean prepareStep() {
        if (prepared) return true;
        int count = 0;
        for (Marker marker : loot) {
            if (preparedMarkers.contains(marker.id)) continue;
            prepareContainer(marker);
            preparedMarkers.add(marker.id);
            if (++count == PREPARE_BATCH) break;
        }
        if (preparedMarkers.size() == loot.size()) {
            syncPhysical(true);
            prepared = true;
        }
        return prepared;
    }
    public boolean prepared() { return prepared; }
    public int generationCount() { return generated.size(); }
    public List<String> conditionalMarkerIds() {
        return markers.values().stream().filter(m -> m.kind.equals("extract.conditional")).map(Marker::id).toList();
    }

    public void tick(long now, Callbacks callbacks) {
        if (!prepared) return;
        if (now % 20 == 0) signalExtractions(now, callbacks);
        for (ExtractionSwitchRules.Cycle cycle : gates.due(now)) {
            Marker marker = markers.get(cycle.windowId());
            PendingCycle pending = pendingCycles.get(cycle.windowId());
            if (pending == null) {
                // Freeze the deadline's occupants before side effects. A failed settlement retry
                // must not admit late arrivals or disqualify a player who has since moved away.
                List<UUID> occupants = callbacks.actors().stream().filter(a -> !settledActors.contains(a.id)
                        && zone(marker, a.position)).map(Actor::id).distinct().toList();
                pending = new PendingCycle(cycle.number(), occupants);
                pendingCycles.put(cycle.windowId(), pending);
                callbacks.changed();
            }
            require(pending.number == cycle.number(), "Extraction cycle changed before settlement");
            for (UUID actor : pending.occupants) {
                if (!settledActors.contains(actor)) settle(actor, "timed/" + marker.id + "/" + cycle.number(), callbacks);
            }
            if (gates.complete(cycle.windowId(), cycle.number(), now)) {
                pendingCycles.remove(cycle.windowId());
                callbacks.changed();
                syncPhysical(false);
                callbacks.broadcast(Component.literal("撤离窗口已结束。"));
            }
        }
        if (now >= nextPhysicalSync) {
            syncPhysical(false);
            nextPhysicalSync = now + 20;
        }
    }

    private void signalExtractions(long now, Callbacks callbacks) {
        var windows = gates.snapshot().windows();
        var dust = new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(.18F,.92F,.31F), 2F);
        for (Actor actor : callbacks.actors()) if (actor.entity() instanceof ServerPlayer player) {
            Marker nearest = null; double distance = Double.POSITIVE_INFINITY; boolean within = false;
            for (Marker marker : markers.values()) if (marker.kind.startsWith("extract.")) {
                var window = windows.get(marker.id);
                if (marker.kind.equals("extract.conditional")) {
                    JsonObject conditions = marker.config.getAsJsonObject("conditions");
                    boolean load = conditions.has("maxLoadRatio"), fee = conditions.has("payment");
                    double ratio = load ? callbacks.loadRatio(player) : 0;
                    boolean light = load && Double.isFinite(ratio) && ratio >= 0 && ratio <= conditions.get("maxLoadRatio").getAsDouble();
                    boolean funded = fee && callbacks.canPay(player, price(conditions.getAsJsonObject("payment")));
                    if ("all".equals(string(marker.config, "conditionMode", "all")) ? (load && !light || fee && !funded) : (!light && !funded)) continue;
                }
                if (window != null && (!window.active() || window.endsAtTick() < now)) continue;
                if (window != null) {
                    JsonObject zone = marker.config.getAsJsonObject("zone");
                    CompoundTag outline = new CompoundTag(); outline.putString("Kind", "raid_exit");
                    outline.putString("Dimension", level.dimension().location().toString()); outline.putString("Id", marker.id);
                    outline.putDouble("X", marker.position.x); outline.putDouble("Y", marker.position.y); outline.putDouble("Z", marker.position.z);
                    outline.putDouble("Radius", zone.get("radius").getAsDouble()); outline.putDouble("Height", zone.get("halfHeight").getAsDouble());
                    outline.putBoolean("Round", "cylinder".equals(zone.get("shape").getAsString())); outline.putLong("Ticks", Math.min(30, window.endsAtTick() - now));
                    com.rzy.dealt_force_skills.network.NetworkHandler.sendToPlayer(new com.rzy.dealt_force_skills.network.S2C_SkillFeedback(outline), player);
                }
                double range = player.distanceToSqr(marker.position);
                if (range < 160*160) level.sendParticles(player, dust, true, marker.position.x, marker.position.y+1.5, marker.position.z, 20, .45, 1.1, .45, .04);
                boolean inside = zone(marker, actor.position);
                if (zone(marker, marker.position.add(actor.position.subtract(marker.position).scale(1/1.5)))
                        && (nearest == null || inside && !within || inside == within && range < distance)) {
                    nearest = marker; within = inside; distance = range;
                }
            }
            if (nearest != null) {
                var window = windows.get(nearest.id);
                String status = window == null ? "条件撤离 · /raid extract " + nearest.id : "撤离倒计时 " + Math.max(0,(window.endsAtTick()-now+19)/20) + " 秒";
                player.displayClientMessage(Component.literal(status).withStyle(within ? net.minecraft.ChatFormatting.GREEN : net.minecraft.ChatFormatting.RED), true);
            }
        }
    }

    /** True means this is an instance-owned interaction and vanilla interaction must be cancelled. */
    public boolean interact(ServerPlayer player, BlockPos pos, long now, Callbacks callbacks) {
        String id = interactions.get(pos);
        if (id == null) return false;
        if (!prepared || !callbacks.requireParticipant(player)) return true;
        Marker marker = markers.get(id);
        // Check the actual targets before spending a use or a player's currency.
        syncMarker(marker, true);
        if (marker.kind.equals("switch.evac")) {
            ExtractionSwitchRules.PullResult result = gates.pull(id, now);
            if (result.status() == ExtractionSwitchRules.PullStatus.PULLED) {
                // Save the latch and spent use before announcing or allowing another interaction.
                callbacks.changed();
                syncMarker(marker, true);
                callbacks.broadcast(Component.literal("已拉闸：" + id));
                for (ExtractionSwitchRules.Cycle cycle : result.started()) {
                    callbacks.onExtractionOpened(markers.get(cycle.windowId()).position, cycle.endsAtTick());
                    callbacks.broadcast(Component.literal("撤离倒计时已开始：" + (cycle.endsAtTick() - now) / 20 + " 秒。"));
                }
                if (!result.started().isEmpty()) { callbacks.changed(); signalExtractions(now, callbacks); }
            } else {
                syncMarker(marker, true);
                player.sendSystemMessage(Component.literal("已拉闸，当前不可再次使用。"));
            }
        } else {
            if (!paidDoors.contains(id)) {
                long quotedPrice = price(marker.config.getAsJsonObject("price"));
                DoorQuote quote = quotes.get(player.getUUID());
                if (!player.isShiftKeyDown() || quote == null || !quote.marker().equals(id) || now - quote.tick() >= 120) {
                    quotes.put(player.getUUID(), new DoorQuote(id, now));
                    player.displayClientMessage(Component.literal("解锁此门需要 " + quotedPrice + " 哈夫币。是否解锁？6 秒内潜行并右键确认；不操作不会扣费。"), true);
                    return true;
                }
                if (now == quote.tick()) return true;
                quotes.remove(player.getUUID());
                UUID payer = doorPayers.get(id);
                if (payer != null && !payer.equals(player.getUUID())) {
                    player.sendSystemMessage(Component.literal("此门正在处理另一名玩家的付款，请稍后重试。"));
                    return true;
                }
                // Persist the payer as well as the instance/door transaction. An interrupted debit
                // must not be retried against another player's wallet.
                if (payer == null) { doorPayers.put(id, player.getUUID()); callbacks.changed(); }
                long amount = price(marker.config.getAsJsonObject("price"));
                if (!callbacks.pay(player, amount, RaidWorldLogic.transaction(instance, "door/" + id))) {
                    doorPayers.remove(id);
                    callbacks.changed();
                    player.sendSystemMessage(Component.literal("哈夫币不足，无法开启此门。"));
                    return true;
                }
                paidDoors.add(id);
                callbacks.changed();
                callbacks.broadcast(Component.literal("付费门已开启：" + id));
            }
            syncMarker(marker, true);
        }
        return true;
    }

    public boolean confirmConditional(ServerPlayer player, String markerId, Callbacks callbacks) {
        Marker marker = markers.get(markerId);
        if (marker == null || !marker.kind.equals("extract.conditional")) return false;
        if (!prepared || !callbacks.requireParticipant(player) || settledActors.contains(player.getUUID())) return true;
        Actor actor = callbacks.actors().stream().filter(a -> a.id.equals(player.getUUID())).findFirst().orElse(null);
        if (actor == null || !zone(marker, actor.position)) {
            player.sendSystemMessage(Component.literal("需要站在该撤离区域内。"));
            return true;
        }
        JsonObject conditions = marker.config.getAsJsonObject("conditions");
        boolean hasLoad = conditions.has("maxLoadRatio"), hasPayment = conditions.has("payment");
        double ratio = hasLoad ? callbacks.loadRatio(actor.entity) : 0;
        boolean loadPass = hasLoad && Double.isFinite(ratio) && ratio >= 0 && ratio <= conditions.get("maxLoadRatio").getAsDouble();
        boolean all = "all".equals(string(marker.config, "conditionMode", "all"));
        // In "any" mode a passing free condition must never cause an unnecessary debit.
        if (all && hasLoad && !loadPass || !all && !loadPass && !hasPayment) {
            player.sendSystemMessage(Component.literal("当前物品占格比例不满足撤离条件。"));
            return true;
        }
        if (hasPayment && (all || !loadPass)
                && !callbacks.pay(player, price(conditions.getAsJsonObject("payment")),
                RaidWorldLogic.transaction(instance, "conditional/" + markerId + "/" + actor.id))) {
            player.sendSystemMessage(Component.literal("哈夫币不足，无法撤离。"));
            return true;
        }
        settle(actor.id, "conditional/" + markerId, callbacks);
        return true;
    }

    private void settle(UUID actor, String reason, Callbacks callbacks) {
        callbacks.extract(actor, instance + "/" + reason + "/" + actor);
        settledActors.add(actor);
        callbacks.changed();
    }

    private void prepareContainer(Marker marker) {
        Storage storage = storage(marker.targets.get(0));
        long key = storage.canonical.asLong();
        String definition = marker.kind + "/" + number(marker.config, "spawnChance", 1);
        String previous = containerDefinitions.putIfAbsent(key, definition);
        require(previous == null || previous.equals(definition), "Conflicting loot markers for " + storage.canonical);
        CompoundTag receipt = generated.get(key);
        for (BlockEntity block : storage.blocks) {
            CompoundTag data = block.getPersistentData();
            if (!data.contains(LOOT_STAMP, Tag.TAG_COMPOUND)) continue;
            CompoundTag stamp = data.getCompound(LOOT_STAMP);
            require(stamp.hasUUID("Instance") && stamp.getUUID("Instance").equals(instance)
                    && stamp.getLong("Position") == key && definition.equals(stamp.getString("Definition")),
                    "Container belongs to another generation: " + block.getBlockPos());
            if (receipt == null) receipt = stamp.copy();
            else require(receipt.equals(stamp), "Conflicting container generation receipts");
        }
        if (receipt != null) {
            require(definition.equals(receipt.getString("Definition")), "Changed loot tier/chance after generation");
            generated.put(key, receipt);
            stamp(storage, receipt);
            return;
        }
        require(storage.container.isEmpty(), "Refusing to overwrite nonempty template container: " + storage.canonical);
        long seed = RaidWorldLogic.containerSeed(instance, key);
        RaidLootTables.Roll roll = RaidLootTables.roll(level, Vec3.atCenterOf(storage.canonical),
                RaidWorldLogic.Tier.marker(marker.kind), seed);
        List<ItemStack> stacks = roll.stacks();
        require(stacks.size() <= storage.container.getContainerSize(), "Loot exceeds marked container capacity");
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < storage.container.getContainerSize(); i++) slots.add(i);
        Collections.shuffle(slots, new Random(seed ^ 0x6a09e667f3bcc909L));
        for (int i = 0; i < stacks.size(); i++) storage.container.setItem(slots.get(i), stacks.get(i));
        storage.container.setChanged();
        receipt = new CompoundTag();
        receipt.putUUID("Instance", instance);
        receipt.putLong("Position", key);
        receipt.putString("Definition", definition);
        receipt.putString("Marker", marker.id);
        receipt.putLong("Seed", seed);
        receipt.putInt("Stacks", stacks.size());
        receipt.putBoolean("Skipped", false);
        receipt.put("Tables", stringTags(roll.tables()));
        generated.put(key, receipt);
        stamp(storage, receipt);
    }
    private static void stamp(Storage storage, CompoundTag receipt) {
        for (BlockEntity block : storage.blocks) {
            block.getPersistentData().put(LOOT_STAMP, receipt.copy());
            block.setChanged();
        }
    }
    private Storage storage(BlockPos target) {
        BlockState state = level.getBlockState(target);
        BlockEntity block = level.getBlockEntity(target);
        require(block instanceof Container, "Marked container is missing at " + target);
        if (state.getBlock() instanceof ChestBlock chest && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
            BlockPos other = target.relative(ChestBlock.getConnectedDirection(state));
            BlockEntity second = level.getBlockEntity(other);
            require(second instanceof Container && level.getBlockState(other).is(state.getBlock()), "Broken double chest at " + target);
            Container combined = ChestBlock.getContainer(chest, state, level, target, true);
            require(combined != null && combined.getContainerSize() == ((Container) block).getContainerSize() + ((Container) second).getContainerSize(),
                    "Incomplete double chest at " + target);
            BlockPos canonical = target.compareTo(other) <= 0 ? target : other;
            return new Storage(canonical, combined, List.of(block, second));
        }
        return new Storage(target, (Container) block, List.of(block));
    }

    private void syncPhysical(boolean strict) {
        for (Marker marker : markers.values())
            if (marker.kind.equals("switch.evac") || marker.kind.equals("interact.paid_door")) syncMarker(marker, strict);
    }
    private void syncMarker(Marker marker, boolean strict) {
        boolean gate = marker.kind.equals("switch.evac");
        boolean enabled = gate ? gates.isPulled(marker.id) : paidDoors.contains(marker.id);
        for (BlockPos target : marker.targets) {
            if (!strict && !level.hasChunkAt(target)) continue;
            BlockState state = level.getBlockState(target);
            var property = gate ? BlockStateProperties.POWERED : BlockStateProperties.OPEN;
            require(state.hasProperty(property), "Missing " + marker.kind + " target at " + target);
            if (state.getValue(property) != enabled) level.setBlock(target, state.setValue(property, enabled), 3);
        }
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Version", 1);
        tag.putUUID("Instance", instance);
        tag.putString("Map", mapId);
        tag.putLong("Origin", origin.asLong());
        tag.putBoolean("Prepared", prepared);
        tag.put("PreparedMarkers", stringTags(preparedMarkers));
        tag.put("PaidDoors", stringTags(paidDoors));
        CompoundTag payers = new CompoundTag();
        doorPayers.forEach(payers::putUUID);
        tag.put("DoorPayers", payers);
        tag.put("SettledActors", stringTags(settledActors.stream().map(UUID::toString).toList()));
        CompoundTag pending = new CompoundTag();
        pendingCycles.forEach((id, cycle) -> {
            CompoundTag value = new CompoundTag();
            value.putLong("Cycle", cycle.number);
            value.put("Occupants", stringTags(cycle.occupants.stream().map(UUID::toString).toList()));
            pending.put(id, value);
        });
        tag.put("PendingCycles", pending);
        ListTag receipts = new ListTag();
        generated.values().forEach(value -> receipts.add(value.copy()));
        tag.put("Generated", receipts);
        ExtractionSwitchRules.Snapshot snapshot = gates.snapshot();
        CompoundTag savedGates = new CompoundTag(), windows = new CompoundTag();
        snapshot.gates().forEach((id, state) -> {
            CompoundTag value = new CompoundTag();
            value.putLong("Uses", state.uses()); value.putBoolean("Pulled", state.pulled());
            value.put("Pending", stringTags(state.pendingWindows())); savedGates.put(id, value);
        });
        snapshot.windows().forEach((id, state) -> {
            CompoundTag value = new CompoundTag();
            value.putLong("Cycle", state.cycle()); value.putLong("EndsAt", state.endsAtTick());
            value.put("Activated", stringTags(state.activatedGateIds())); windows.put(id, value);
        });
        tag.put("Gates", savedGates); tag.put("Windows", windows);
        return tag;
    }
    public static RaidWorldState load(ServerLevel level, CompoundTag tag) {
        require(tag.getInt("Version") == 1, "Unknown raid world-state version");
        RaidWorldState result = new RaidWorldState(level, tag.getUUID("Instance"), tag.getString("Map"), BlockPos.of(tag.getLong("Origin")));
        result.prepared = tag.getBoolean("Prepared");
        result.preparedMarkers.addAll(strings(tag.getList("PreparedMarkers", Tag.TAG_STRING)));
        Set<String> lootIds = new HashSet<>(result.loot.stream().map(Marker::id).toList());
        require(lootIds.containsAll(result.preparedMarkers) && (!result.prepared || lootIds.equals(result.preparedMarkers)), "Invalid prepared marker state");
        result.paidDoors.addAll(strings(tag.getList("PaidDoors", Tag.TAG_STRING)));
        for (String id : result.paidDoors) require(result.isDoor(id), "Unknown paid door: " + id);
        CompoundTag payers = tag.getCompound("DoorPayers");
        for (String id : payers.getAllKeys()) {
            require(result.isDoor(id), "Unknown door payer: " + id);
            result.doorPayers.put(id, payers.getUUID(id));
        }
        for (String id : strings(tag.getList("SettledActors", Tag.TAG_STRING))) result.settledActors.add(UUID.fromString(id));
        for (Tag value : tag.getList("Generated", Tag.TAG_COMPOUND)) {
            CompoundTag receipt = (CompoundTag) value;
            require(receipt.hasUUID("Instance") && result.instance.equals(receipt.getUUID("Instance")), "Wrong generation instance");
            long position = receipt.getLong("Position");
            require(result.generated.putIfAbsent(position, receipt.copy()) == null, "Duplicate generation receipt");
            result.containerDefinitions.put(position, receipt.getString("Definition"));
        }
        Map<String, ExtractionSwitchRules.GateSnapshot> gates = new LinkedHashMap<>();
        CompoundTag gs = tag.getCompound("Gates");
        for (String id : gs.getAllKeys()) {
            CompoundTag state = gs.getCompound(id);
            gates.put(id, new ExtractionSwitchRules.GateSnapshot(state.getLong("Uses"), state.getBoolean("Pulled"),
                    new LinkedHashSet<>(strings(state.getList("Pending", Tag.TAG_STRING)))));
        }
        Map<String, ExtractionSwitchRules.WindowSnapshot> windows = new LinkedHashMap<>();
        CompoundTag ws = tag.getCompound("Windows");
        for (String id : ws.getAllKeys()) {
            CompoundTag state = ws.getCompound(id);
            windows.put(id, new ExtractionSwitchRules.WindowSnapshot(state.getLong("Cycle"), state.getLong("EndsAt"),
                    new LinkedHashSet<>(strings(state.getList("Activated", Tag.TAG_STRING)))));
        }
        result.gates = ExtractionSwitchRules.restore(result.gateDefs, result.windowDefs, new ExtractionSwitchRules.Snapshot(gates, windows));
        CompoundTag pending = tag.getCompound("PendingCycles");
        for (String id : pending.getAllKeys()) {
            CompoundTag value = pending.getCompound(id);
            long cycle = value.getLong("Cycle");
            ExtractionSwitchRules.WindowSnapshot window = windows.get(id);
            require(window != null && window.active() && window.cycle() == cycle, "Invalid pending extraction cycle");
            List<UUID> occupants = strings(value.getList("Occupants", Tag.TAG_STRING)).stream().map(UUID::fromString).toList();
            require(new HashSet<>(occupants).size() == occupants.size(), "Duplicate pending extraction actor");
            result.pendingCycles.put(id, new PendingCycle(cycle, occupants));
        }
        return result;
    }
    public boolean lockedDoor(BlockPos pos) {
        String id=interactions.get(pos); return isDoor(id) && !paidDoors.contains(id);
    }
    private boolean isDoor(String id) { return markers.containsKey(id) && markers.get(id).kind.equals("interact.paid_door"); }
    private void addInteractions(Marker marker) {
        for (BlockPos target : marker.targets)
            require(interactions.putIfAbsent(target, marker.id) == null, "Overlapping interaction targets: " + target);
    }
    private static boolean zone(Marker marker, Vec3 point) {
        JsonObject zone = marker.config.getAsJsonObject("zone");
        require(zone != null, "Missing extraction zone: " + marker.id);
        return RaidWorldLogic.inZone(zone.get("shape").getAsString(), zone.get("radius").getAsDouble(),
                zone.get("halfHeight").getAsDouble(), point.x - marker.position.x,
                point.y - marker.position.y, point.z - marker.position.z);
    }
    private static List<BlockPos> targets(Vec3 position, JsonObject config) {
        BlockPos base = BlockPos.containing(position);
        Set<BlockPos> result = new LinkedHashSet<>();
        if (config.has("targetBlockOffset")) result.add(offset(base, config.getAsJsonArray("targetBlockOffset")));
        if (config.has("targetBlockOffsets")) for (JsonElement value : config.getAsJsonArray("targetBlockOffsets")) result.add(offset(base, value.getAsJsonArray()));
        if (result.isEmpty()) result.add(base);
        return List.copyOf(result);
    }
    private static BlockPos offset(BlockPos base, JsonArray a) {
        require(a.size() == 3, "Invalid target offset");
        return base.offset(a.get(0).getAsInt(), a.get(1).getAsInt(), a.get(2).getAsInt());
    }
    private static long price(JsonObject value) {
        require(value != null && "havoc_coin".equals(string(value, "currencyKey", "")), "Unsupported/missing raid currency");
        long amount = value.get("amount").getAsLong();
        require(amount >= 0, "Negative raid price");
        return amount;
    }
    private static String string(JsonObject object, String key, String fallback) { return object.has(key) ? object.get(key).getAsString() : fallback; }
    private static boolean bool(JsonObject object, String key, boolean fallback) { return object.has(key) ? object.get(key).getAsBoolean() : fallback; }
    private static double number(JsonObject object, String key, double fallback) { return object.has(key) ? object.get(key).getAsDouble() : fallback; }
    private static List<String> strings(JsonArray array) {
        require(array != null, "Missing marker links");
        List<String> values = new ArrayList<>();
        array.forEach(value -> values.add(value.getAsString()));
        require(new HashSet<>(values).size() == values.size(), "Duplicate marker links");
        return values;
    }
    private static List<String> strings(ListTag list) {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) result.add(list.getString(i));
        return result;
    }
    private static ListTag stringTags(Collection<String> values) {
        ListTag list = new ListTag();
        values.forEach(value -> list.add(StringTag.valueOf(value)));
        return list;
    }
    private static void require(boolean condition, String message) { if (!condition) throw new IllegalArgumentException(message); }
}
