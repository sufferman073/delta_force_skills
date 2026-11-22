package com.rzy.dealt_force_skills.raid.rules;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** One server-authoritative state machine per raid instance; time is supplied in server ticks. */
public final class ExtractionSwitchRules {
    public static final int UNLIMITED = -1;

    public record Gate(String id, int maxUses) {
        public Gate {
            requireId(id);
            if (maxUses != UNLIMITED && maxUses < 1) throw new IllegalArgumentException("Invalid maxUses");
        }
        public static Gate fromSingleUse(String id, boolean singleUse) {
            return new Gate(id, singleUse ? 1 : UNLIMITED);
        }
    }

    public record Window(String id, List<String> gateIds, int requiredSwitchCount, long durationTicks) {
        public Window {
            requireId(id);
            gateIds = List.copyOf(gateIds);
            if (gateIds.isEmpty() || new HashSet<>(gateIds).size() != gateIds.size()
                    || requiredSwitchCount < 1 || requiredSwitchCount > gateIds.size() || durationTicks < 1)
                throw new IllegalArgumentException("Invalid extraction window: " + id);
        }
    }

    public enum PullStatus { PULLED, ALREADY_PULLED, WINDOW_ACTIVE, EXHAUSTED }
    public record Cycle(String windowId, long number, long endsAtTick) { }
    public record PullResult(PullStatus status, List<Cycle> started) {
        public PullResult { started = List.copyOf(started); }
    }
    public record GateSnapshot(long uses, boolean pulled, Set<String> pendingWindows) {
        public GateSnapshot { pendingWindows = Set.copyOf(pendingWindows); }
    }
    public record WindowSnapshot(long cycle, long endsAtTick, Set<String> activatedGateIds) {
        public WindowSnapshot { activatedGateIds = Set.copyOf(activatedGateIds); }
        public boolean active() { return endsAtTick >= 0; }
    }
    public record Snapshot(Map<String, GateSnapshot> gates, Map<String, WindowSnapshot> windows) {
        public Snapshot { gates = Map.copyOf(gates); windows = Map.copyOf(windows); }
    }

    private final Map<String, Gate> gates = new LinkedHashMap<>();
    private final Map<String, Window> windows = new LinkedHashMap<>();
    private final Map<String, GateState> gateStates = new LinkedHashMap<>();
    private final Map<String, WindowState> windowStates = new LinkedHashMap<>();
    private final Map<String, List<String>> links = new LinkedHashMap<>();

    private static final class GateState {
        long uses;
        boolean pulled;
        final Set<String> pending = new LinkedHashSet<>();
    }
    private static final class WindowState {
        long cycle;
        long endsAt = -1;
        final Set<String> activated = new LinkedHashSet<>();
    }

    public ExtractionSwitchRules(List<Gate> gateDefinitions, List<Window> windowDefinitions) {
        for (Gate gate : gateDefinitions) {
            if (gates.putIfAbsent(gate.id(), gate) != null) throw new IllegalArgumentException("Duplicate gate");
            gateStates.put(gate.id(), new GateState());
            links.put(gate.id(), new ArrayList<>());
        }
        for (Window window : windowDefinitions) {
            if (windows.putIfAbsent(window.id(), window) != null) throw new IllegalArgumentException("Duplicate window");
            windowStates.put(window.id(), new WindowState());
            for (String id : window.gateIds()) {
                if (!gates.containsKey(id)) throw new IllegalArgumentException("Unknown gate: " + id);
                links.get(id).add(window.id());
            }
        }
        if (links.values().stream().anyMatch(List::isEmpty)) throw new IllegalArgumentException("Unlinked gate");
    }

    /** Atomically latches the gate, records one use and starts newly satisfied windows. */
    public synchronized PullResult pull(String gateId, long now) {
        if (now < 0) throw new IllegalArgumentException("Negative tick");
        Gate gate = gate(gateId);
        GateState state = gateStates.get(gateId);
        if (state.pulled) return new PullResult(PullStatus.ALREADY_PULLED, List.of());
        if (exhausted(gate, state)) return new PullResult(PullStatus.EXHAUSTED, List.of());
        // A spare linked switch cannot spend a use or restart a window already running.
        if (links.get(gateId).stream().anyMatch(id -> windowStates.get(id).endsAt >= 0))
            return new PullResult(PullStatus.WINDOW_ACTIVE, List.of());
        List<Cycle> started = new ArrayList<>();
        // Validate arithmetic before mutating anything, including partial multi-window activation.
        Math.incrementExact(state.uses);
        for (String id : links.get(gateId)) {
            WindowState ws = windowStates.get(id);
            if (ws.activated.size() + 1 >= windows.get(id).requiredSwitchCount()) {
                Math.addExact(now, windows.get(id).durationTicks());
                Math.incrementExact(ws.cycle);
            }
        }
        state.uses++;
        state.pulled = true;
        for (String id : links.get(gateId)) {
            state.pending.add(id);
            WindowState ws = windowStates.get(id);
            ws.activated.add(gateId);
            if (ws.activated.size() >= windows.get(id).requiredSwitchCount()) {
                ws.endsAt = now + windows.get(id).durationTicks();
                ws.cycle++;
                started.add(new Cycle(id, ws.cycle, ws.endsAt));
            }
        }
        return new PullResult(PullStatus.PULLED, started);
    }

    /** Due cycles remain available after reload until the caller acknowledges their settlement. */
    public synchronized List<Cycle> due(long now) {
        List<Cycle> result = new ArrayList<>();
        for (var entry : windowStates.entrySet()) {
            WindowState state = entry.getValue();
            if (state.endsAt >= 0 && now >= state.endsAt)
                result.add(new Cycle(entry.getKey(), state.cycle, state.endsAt));
        }
        return List.copyOf(result);
    }

    /**
     * Call only after settling the actual occupants. Persist the cycle token with settlement receipts.
     * A gate linked to several windows resets only after all windows using that pull have completed.
     */
    public synchronized boolean complete(String windowId, long cycle, long now) {
        WindowState ws = windowState(windowId);
        if (ws.endsAt < 0 || now < ws.endsAt || ws.cycle != cycle) return false;
        for (String id : ws.activated) {
            GateState state = gateStates.get(id);
            state.pending.remove(windowId);
            if (state.pending.isEmpty() && !exhausted(gates.get(id), state)) state.pulled = false;
        }
        ws.activated.clear();
        ws.endsAt = -1;
        return true;
    }

    public synchronized long remainingUses(String gateId) {
        Gate gate = gate(gateId);
        return gate.maxUses() == UNLIMITED ? UNLIMITED : gate.maxUses() - gateStates.get(gateId).uses;
    }

    public synchronized boolean isPulled(String gateId) { gate(gateId); return gateStates.get(gateId).pulled; }

    public synchronized Snapshot snapshot() {
        Map<String, GateSnapshot> gs = new LinkedHashMap<>();
        Map<String, WindowSnapshot> ws = new LinkedHashMap<>();
        gateStates.forEach((id, state) -> gs.put(id, new GateSnapshot(state.uses, state.pulled, state.pending)));
        windowStates.forEach((id, state) -> ws.put(id, new WindowSnapshot(state.cycle, state.endsAt, state.activated)));
        return new Snapshot(gs, ws);
    }

    /** Rejects mismatched/corrupt snapshots instead of silently creating fresh countdowns. */
    public static ExtractionSwitchRules restore(List<Gate> gates, List<Window> windows, Snapshot snapshot) {
        Objects.requireNonNull(snapshot);
        ExtractionSwitchRules restored = new ExtractionSwitchRules(gates, windows);
        if (!snapshot.gates().keySet().equals(restored.gates.keySet())
                || !snapshot.windows().keySet().equals(restored.windows.keySet()))
            throw new IllegalArgumentException("Snapshot definitions changed");
        snapshot.gates().forEach((id, saved) -> {
            Gate gate = restored.gates.get(id);
            if (saved.uses() < 0 || (gate.maxUses() != UNLIMITED && saved.uses() > gate.maxUses())
                    || !restored.links.get(id).containsAll(saved.pendingWindows())
                    || (!saved.pulled() && !saved.pendingWindows().isEmpty())
                    || (saved.pulled() && saved.uses() == 0)
                    || (saved.pulled() && saved.pendingWindows().isEmpty()
                        && (gate.maxUses() == UNLIMITED || saved.uses() < gate.maxUses()))
                    || (!saved.pulled() && gate.maxUses() != UNLIMITED && saved.uses() == gate.maxUses()))
                throw new IllegalArgumentException("Invalid gate snapshot: " + id);
            GateState state = restored.gateStates.get(id);
            state.uses = saved.uses();
            state.pulled = saved.pulled();
            state.pending.addAll(saved.pendingWindows());
        });
        snapshot.windows().forEach((id, saved) -> {
            Window window = restored.windows.get(id);
            if (saved.cycle() < 0 || saved.endsAtTick() < -1
                    || !window.gateIds().containsAll(saved.activatedGateIds())
                    || saved.active() != (saved.activatedGateIds().size() >= window.requiredSwitchCount())
                    || (saved.active() && saved.cycle() == 0))
                throw new IllegalArgumentException("Invalid window snapshot: " + id);
            WindowState state = restored.windowStates.get(id);
            state.cycle = saved.cycle();
            state.endsAt = saved.endsAtTick();
            state.activated.addAll(saved.activatedGateIds());
        });
        for (var entry : restored.links.entrySet()) {
            for (String windowId : entry.getValue()) {
                if (restored.gateStates.get(entry.getKey()).pending.contains(windowId)
                        != restored.windowStates.get(windowId).activated.contains(entry.getKey()))
                    throw new IllegalArgumentException("Snapshot gate/window mismatch");
            }
        }
        return restored;
    }

    private Gate gate(String id) {
        Gate gate = gates.get(id);
        if (gate == null) throw new IllegalArgumentException("Unknown gate: " + id);
        return gate;
    }
    private WindowState windowState(String id) {
        WindowState state = windowStates.get(id);
        if (state == null) throw new IllegalArgumentException("Unknown window: " + id);
        return state;
    }
    private static boolean exhausted(Gate gate, GateState state) {
        return gate.maxUses() != UNLIMITED && state.uses >= gate.maxUses();
    }
    private static void requireId(String id) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("Empty ID");
    }
}
