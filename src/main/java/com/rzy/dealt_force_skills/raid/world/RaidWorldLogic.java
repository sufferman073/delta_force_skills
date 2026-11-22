package com.rzy.dealt_force_skills.raid.world;

import java.nio.charset.StandardCharsets;
import java.util.*;

/** Pure rules shared by the two loaders; no game registries or player assets are accessed here. */
public final class RaidWorldLogic {
    public enum Tier {
        LOW(1, 5), NORMAL(3, 12), HIGH(5, 20);
        public final int minStacks, maxStacks;
        Tier(int minStacks, int maxStacks) { this.minStacks = minStacks; this.maxStacks = maxStacks; }
        public static Tier marker(String kind) {
            return switch (kind) {
                case "loot.common" -> LOW;
                case "loot.rare" -> NORMAL;
                case "loot.high" -> HIGH;
                default -> throw new IllegalArgumentException("Unknown loot tier: " + kind);
            };
        }
    }
    private static final Set<String> OVERWORLD = Set.of(
            "abandoned_mineshaft", "ancient_city", "ancient_city_ice_box", "buried_treasure",
            "desert_pyramid", "igloo_chest", "jungle_temple", "jungle_temple_dispenser",
            "pillager_outpost", "ruined_portal", "shipwreck_map", "shipwreck_supply", "shipwreck_treasure",
            "simple_dungeon", "spawn_bonus_chest", "stronghold_corridor", "stronghold_crossing",
            "stronghold_library", "underwater_ruin_big", "underwater_ruin_small", "woodland_mansion");
    private static final Set<String> NETHER = Set.of("nether_bridge", "bastion_treasure", "bastion_other",
            "bastion_bridge", "bastion_hoglin_stable", "ruined_portal");
    private static final Set<String> END = Set.of("end_city_treasure");

    public record Pools(List<String> overworld, List<String> nether, List<String> end, List<String> all,
                        List<String> unclassified) {
        public Pools {
            overworld = List.copyOf(overworld); nether = List.copyOf(nether); end = List.copyOf(end);
            all = List.copyOf(all); unclassified = List.copyOf(unclassified);
        }
    }
    public static Pools classify(Collection<String> registeredIds) {
        List<String> ow = new ArrayList<>(), nether = new ArrayList<>(), end = new ArrayList<>(), all = new ArrayList<>(), other = new ArrayList<>();
        for (String id : new TreeSet<>(registeredIds)) {
            int colon = id.indexOf(':');
            if (colon < 1 || !id.substring(colon + 1).startsWith("chests/")) continue;
            all.add(id);
            if (!id.startsWith("minecraft:chests/")) { other.add(id); continue; }
            String path = id.substring("minecraft:chests/".length());
            boolean classified = false;
            if (OVERWORLD.contains(path) || path.startsWith("village/") || path.startsWith("trial_chambers/")) { ow.add(id); classified = true; }
            if (NETHER.contains(path)) { nether.add(id); classified = true; }
            if (END.contains(path)) { end.add(id); classified = true; }
            if (!classified) other.add(id);
        }
        return new Pools(ow, nether, end, all, other);
    }
    public static List<String> chooseTables(Pools pools, Tier tier, Random random) {
        List<String> result = new ArrayList<>();
        List<String> ow = new ArrayList<>(pools.overworld());
        int needed = tier == Tier.NORMAL ? 2 : 1;
        if (ow.size() < needed) throw new IllegalStateException("Not enough registered overworld chest loot tables");
        Collections.shuffle(ow, random);
        result.addAll(ow.subList(0, needed));
        if (tier != Tier.LOW) result.add(choose(pools.nether(), random, "Nether"));
        if (tier == Tier.HIGH) {
            result.add(choose(pools.end(), random, "End"));
            result.add(choose(pools.all(), random, "all dimensions/mods"));
        }
        return List.copyOf(result);
    }
    private static String choose(List<String> pool, Random random, String label) {
        if (pool.isEmpty()) throw new IllegalStateException("No registered " + label + " chest loot tables");
        return pool.get(random.nextInt(pool.size()));
    }
    public static int stackCount(Tier tier, Random random) {
        return tier.minStacks + random.nextInt(tier.maxStacks - tier.minStacks + 1);
    }
    /** Samples stack occurrences, never groups by item ID or merges distinct item data. */
    public static <T> List<T> sampleOccurrences(List<T> occurrences, int count, Random random) {
        if (count < 0 || count > occurrences.size()) throw new IllegalArgumentException("Insufficient rolled stack occurrences");
        List<T> shuffled = new ArrayList<>(occurrences);
        Collections.shuffle(shuffled, random);
        return List.copyOf(shuffled.subList(0, count));
    }
    public static boolean spawn(double chance, Random random) {
        if (!Double.isFinite(chance) || chance < 0 || chance > 1) throw new IllegalArgumentException("Invalid spawnChance");
        return chance == 1 || (chance > 0 && random.nextDouble() < chance);
    }
    public static long containerSeed(UUID instance, long canonicalPosition) {
        long value = instance.getMostSignificantBits() ^ Long.rotateLeft(instance.getLeastSignificantBits(), 23) ^ canonicalPosition;
        value = (value ^ value >>> 30) * 0xbf58476d1ce4e5b9L;
        value = (value ^ value >>> 27) * 0x94d049bb133111ebL;
        return value ^ value >>> 31;
    }
    public static UUID transaction(UUID instance, String purpose) {
        return UUID.nameUUIDFromBytes(("dfs:raid:" + instance + ":" + purpose).getBytes(StandardCharsets.UTF_8));
    }
    public static boolean inZone(String shape, double radius, double halfHeight,
                                 double dx, double dy, double dz) {
        if (!Double.isFinite(radius) || !Double.isFinite(halfHeight) || radius <= 0 || halfHeight <= 0)
            throw new IllegalArgumentException("Invalid extraction zone");
        if (!Double.isFinite(dx) || !Double.isFinite(dy) || !Double.isFinite(dz)) return false;
        if (Math.abs(dy) > halfHeight) return false;
        return switch (shape) {
            case "cylinder" -> dx * dx + dz * dz <= radius * radius;
            case "box" -> Math.abs(dx) <= radius && Math.abs(dz) <= radius;
            default -> throw new IllegalArgumentException("Unknown extraction shape: " + shape);
        };
    }
    private RaidWorldLogic() { }
}
