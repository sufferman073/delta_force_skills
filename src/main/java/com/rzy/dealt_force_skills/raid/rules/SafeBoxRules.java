package com.rzy.dealt_force_skills.raid.rules;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/** Pure settlement calculation; inventory copies, payment and mail delivery belong to the caller. */
public final class SafeBoxRules {
    public static final int PURCHASE_USES = 10;
    private SafeBoxRules() { }

    public enum Tier {
        PERMANENT(2, 0), FOUR(4, 500_000), SIX(6, 2_000_000), NINE(9, 5_000_000);
        private final int slots;
        private final long cost;
        Tier(int slots, long cost) { this.slots = slots; this.cost = cost; }
        public int slots() { return slots; }
        public long cost() { return cost; }
    }

    public enum Outcome { EXTRACTED, FAILED, LOST, SERVER_RESTART }

    public record State(Tier tier, int remainingUses) {
        public State {
            Objects.requireNonNull(tier);
            if (tier == Tier.PERMANENT ? remainingUses != 0 : remainingUses < 1 || remainingUses > PURCHASE_USES)
                throw new IllegalArgumentException("Invalid protection entitlement");
        }
        public static State permanent() { return new State(Tier.PERMANENT, 0); }
        public boolean covers(Tier requested) { return remainingUses > 0 && tier.slots() >= requested.slots(); }
    }

    public record Settlement(State nextState, List<Integer> protectedSlots, boolean consumedUse) {
        public Settlement { protectedSlots = List.copyOf(protectedSlots); }
    }

    /** Only invoke after the external currency transaction has committed. Does not charge money. */
    public static State purchased(Tier tier) {
        if (tier == Tier.PERMANENT) throw new IllegalArgumentException("Permanent slots are free");
        return new State(tier, PURCHASE_USES);
    }

    public static boolean acceptsItem(int maximumStackSize) { return maximumStackSize > 1; }

    /** Slot indexes are zero-based; occupancy counts nonempty slots, independent of their position. */
    public static Settlement settle(State state, Outcome outcome, List<Integer> occupiedSlots) {
        Objects.requireNonNull(state);
        Objects.requireNonNull(outcome);
        List<Integer> occupied = List.copyOf(occupiedSlots);
        if (new HashSet<>(occupied).size() != occupied.size()
                || occupied.stream().anyMatch(slot -> slot < 0 || slot >= state.tier().slots()))
            throw new IllegalArgumentException("Invalid occupied slots");
        boolean consumed = (outcome == Outcome.FAILED || outcome == Outcome.LOST)
                && state.tier() != Tier.PERMANENT && occupied.size() > Tier.PERMANENT.slots();
        State next = !consumed ? state : state.remainingUses() == 1 ? State.permanent()
                : new State(state.tier(), state.remainingUses() - 1);
        // Capture all current contents before returning the smaller entitlement on the last use.
        return new Settlement(next, occupied, consumed);
    }
}
