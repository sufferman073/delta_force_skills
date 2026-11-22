package com.rzy.dealt_force_skills.raid.rules;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.random.RandomGenerator;

/** Complete authored squads. Entity creation, weapon pools and candidate weights are external. */
public final class RaidAiRules {
    private static final String MOD = "dealt_force_skills:";
    private RaidAiRules() { }

    private static final Map<Integer, List<List<String>>> REGULAR = Map.of(
            1, List.of(List.of("深蓝"), List.of("红狼"), List.of("威龙"), List.of("露娜"), List.of("乌鲁鲁")),
            2, List.of(List.of("银翼", "乌鲁鲁"), List.of("回响", "疾风"), List.of("威龙", "红狼"),
                    List.of("蜂医", "蝶"), List.of("深蓝", "深蓝"), List.of("银翼", "牧羊人"), List.of("麦晓雯", "威龙")),
            3, List.of(List.of("银翼", "乌鲁鲁", "露娜"), List.of("回响", "疾风", "旅人"),
                    List.of("威龙", "红狼", "露娜"), List.of("蜂医", "蝶", "蛊"), List.of("深蓝", "深蓝", "蜂医"),
                    List.of("深蓝", "牧羊人", "乌鲁鲁"), List.of("深蓝", "牧羊人", "比特"),
                    List.of("乌鲁鲁", "牧羊人", "比特"), List.of("液氮", "乌鲁鲁", "比特"),
                    List.of("旅人", "旅人", "旅人"), List.of("银翼", "乌鲁鲁", "疾风"),
                    List.of("疾风", "疾风", "乌鲁鲁"), List.of("无名", "无名", "无名"),
                    List.of("无名", "旅人", "乌鲁鲁"), List.of("麦晓雯", "蜂医", "威龙")));

    private static final Map<Integer, List<List<String>>> SPECIAL = Map.of(
            1, List.of(List.of("蕾忍"), List.of("不死人")),
            2, List.of(List.of("二阶堂希罗", "二阶堂希罗·魔女化"), List.of("交通亭-量产型", "交通亭-量产型"),
                    List.of("尚博勒", "赌徒")),
            3, List.of(List.of("耄耋", "耄耋", "耄耋"), List.of("牢大", "耄耋", "交通亭-量产型"),
                    List.of("牢大", "牢大", "牢大"), List.of("交通亭-量产型", "交通亭-量产型", "交通亭-量产型")));

    // Verified against ModCharacters and zh_cn; the author confirmed 麦晓雯 = Hackclaw.
    private static final Map<String, String> CHARACTERS = Map.ofEntries(
            Map.entry("深蓝", "engineer/sineva"), Map.entry("红狼", "assault/d_wolf"),
            Map.entry("威龙", "assault/vyron"), Map.entry("露娜", "recon/luna"), Map.entry("乌鲁鲁", "engineer/uluru"),
            Map.entry("银翼", "recon/raptor"), Map.entry("回响", "recon/morse"), Map.entry("疾风", "assault/tempest"),
            Map.entry("蜂医", "support/stinger"), Map.entry("蝶", "support/vlinder"), Map.entry("牧羊人", "engineer/shepherd"),
            Map.entry("旅人", "support/rover"), Map.entry("蛊", "support/toxik"), Map.entry("比特", "engineer/gizmo"),
            Map.entry("液氮", "engineer/n_two"), Map.entry("无名", "assault/nox"), Map.entry("蕾忍", "special/lex_ninjia"),
            Map.entry("不死人", "special/undead"), Map.entry("二阶堂希罗", "special/nikaidou_hiro"),
            Map.entry("二阶堂希罗·魔女化", "special/nikaidou_hiro_witchification"),
            Map.entry("交通亭-量产型", "special/department_of_transportation"), Map.entry("尚博勒", "valorant/chamber"),
            Map.entry("赌徒", "special/gambler"), Map.entry("耄耋", "special/catdad"), Map.entry("牢大", "special/manba"),
            Map.entry("麦晓雯", "recon/hackclaw"));

    public enum WeaponRole { RIFLE_OR_SMG, SNIPER, SHOTGUN, PISTOL, NETHERITE_AXE }
    /** amplifier is Minecraft's zero-based amplifier: 0 = I, 1 = II. */
    public record Effect(String id, int amplifier) { }
    public record Equipment(int helmetTier, int chestTier, boolean fullNetheriteArmor,
                            WeaponRole weapon, boolean fullAttachments, boolean allApplicableEnchantments,
                            boolean allowConflictingEnchantments) { }
    public record Member(String authoredName, Equipment equipment) { }
    public record Team(boolean special, List<Member> members, List<Effect> persistentEffects,
                       int wornArmorRepairIntervalTicks, double wornArmorRepairFraction) {
        public Team { members = List.copyOf(members); persistentEffects = List.copyOf(persistentEffects); }
    }

    /** Explicit weighting hook for future map configuration. */
    @FunctionalInterface
    public interface CandidateSelector {
        int select(int size, boolean special, List<List<String>> candidates, RandomGenerator random);
    }

    public static List<List<String>> candidates(int size, boolean special) {
        requireSize(size);
        return (special ? SPECIAL : REGULAR).get(size);
    }

    public static Optional<String> characterId(String authoredName) {
        return Optional.ofNullable(CHARACTERS.get(authoredName)).map(path -> MOD + path);
    }

    /** Author-approved equal weighting within each regular/special pool, after the 10% team roll. */
    public static Team select(int size, RandomGenerator random) {
        return select(size, random, (count, special, candidates, rng) -> rng.nextInt(candidates.size()));
    }

    public static Team select(int size, RandomGenerator random, CandidateSelector selector) {
        requireSize(size);
        Objects.requireNonNull(random);
        Objects.requireNonNull(selector);
        boolean special = random.nextDouble() < 0.10; // Exactly one special roll per complete team.
        List<List<String>> candidates = candidates(size, special);
        int index = selector.select(size, special, candidates, random);
        if (index < 0 || index >= candidates.size()) throw new IllegalArgumentException("Candidate index out of range");
        List<String> names = candidates.get(index);
        List<Member> members = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            boolean melee = special && size == 1;
            WeaponRole role = melee ? WeaponRole.NETHERITE_AXE : i == 0 ? WeaponRole.RIFLE_OR_SMG
                    : i == 1 ? WeaponRole.SNIPER : random.nextDouble() < 0.85 ? WeaponRole.SHOTGUN : WeaponRole.PISTOL;
            Equipment equipment = new Equipment(melee ? 0 : armorTier(size, random),
                    melee ? 0 : armorTier(size, random), melee, role, !melee, melee, melee);
            members.add(new Member(names.get(i), equipment));
        }
        return new Team(special, members, effects(size), size == 1 ? 100 : size == 2 ? 200 : 400, 0.04);
    }

    /** Each armor piece rolls separately; three-person teams use the specified two-stage upgrade. */
    public static int armorTier(int teamSize, RandomGenerator random) {
        requireSize(teamSize);
        Objects.requireNonNull(random);
        if (teamSize == 1) return 5;
        if (teamSize == 2) return random.nextDouble() < 0.30 ? 5 : 4;
        if (random.nextDouble() >= 0.65) return 3;
        return random.nextDouble() < 0.20 ? 5 : 4;
    }

    private static List<Effect> effects(int size) {
        if (size == 1) return List.of(new Effect("minecraft:regeneration", 1), new Effect(MOD + "pain_relief", 0),
                new Effect(MOD + "stamina_boost", 0), new Effect(MOD + "carry_boost", 0),
                new Effect(MOD + "continuous_healing", 0));
        if (size == 2) return List.of(new Effect("minecraft:regeneration", 0), new Effect(MOD + "pain_relief", 0));
        return List.of(new Effect("minecraft:regeneration", 0));
    }

    private static void requireSize(int size) {
        if (size < 1 || size > 3) throw new IllegalArgumentException("AI team size must be 1-3");
    }
}
