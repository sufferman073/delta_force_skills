package com.rzy.dealt_force_skills.ai.operative;

import com.rzy.dealt_force_skills.config.DealtForceConfig;
import com.rzy.dealt_force_skills.entity.OperativeEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ai.attributes.Attributes;
import java.util.Map;

/** Server-owned bot attributes. Role growth and equipment modifiers remain separate. */
public final class OperativeSettings {
    private static final Map<OperativeEntity, Map<String, Double>> APPLIED = new java.util.WeakHashMap<>();
    private record Setting(double initial, double raidInitial, double min, double max) {
        Setting(double initial, double min, double max) { this(initial, initial, min, max); }
    }
    private static final Map<String, Setting> VALUES = Map.ofEntries(
            Map.entry("max_health", new Setting(20, 1, 1000000)),
            Map.entry("armor", new Setting(0, 0, 30)),
            Map.entry("armor_toughness", new Setting(0, 0, 20)),
            Map.entry("damage_reduction", new Setting(0, 0, 1)),
            Map.entry("damage_bonus", new Setting(0, 0, 100)),
            Map.entry("melee_damage_bonus", new Setting(0, 0, 100)),
            Map.entry("tacz_damage_bonus", new Setting(0, 0, 100)),
            Map.entry("head_aim_chance", new Setting(0.5, 0.21, 0, 1)),
            Map.entry("attack_damage", new Setting(4, 0, 10000)),
            Map.entry("movement_speed", new Setting(.28, .2, 0, 4)),
            Map.entry("follow_range", new Setting(80, 24, 1, 256)),
            Map.entry("knockback_resistance", new Setting(0, 0, 1)),
            Map.entry("commander_speed_multiplier", new Setting(.4, 0, 10)));
    private static final String NORMAL = "operatives.attributes.", RAID = "operatives.raid_attributes.";
    static { VALUES.keySet().forEach(key -> { read(NORMAL, key); read(RAID, key); }); }
    private OperativeSettings() { }

    public static boolean valid(String path, Object value) {
        String prefix = path.startsWith(RAID) ? RAID : NORMAL;
        if (!path.startsWith(prefix)) return false;
        var setting = VALUES.get(path.substring(prefix.length()));
        return setting != null && value instanceof Number number && Double.isFinite(number.doubleValue())
                && number.doubleValue() >= setting.min && number.doubleValue() <= setting.max;
    }
    public static double value(String key) { return read(NORMAL, key); }
    public static double value(net.minecraft.world.entity.LivingEntity actor, String key) {
        if (actor instanceof OperativeEntity) {
            var identity = actor.getPersistentData().getCompound(com.rzy.dealt_force_skills.raid.ai.RaidAiAffinity.IDENTITY);
            if (identity.hasUUID("instance") && "operative".equals(identity.getString("kind"))) return read(RAID, key);
        }
        return read(NORMAL, key);
    }
    private static double read(String prefix, String key) {
        var setting = VALUES.get(key);
        double initial = RAID.equals(prefix) ? setting.raidInitial : setting.initial;
        double configured = DealtForceConfig.doubleValue(prefix + key, initial);
        return Double.isFinite(configured) ? Math.max(setting.min, Math.min(setting.max, configured)) : initial;
    }
    public static boolean rollHeadAim(net.minecraft.util.RandomSource random) {
        return random.nextDouble() < value("head_aim_chance");
    }
    public static void apply(OperativeEntity bot) {
        var applied = APPLIED.computeIfAbsent(bot, ignored -> new java.util.HashMap<>());
        apply(bot, applied, "max_health", Attributes.MAX_HEALTH);
        apply(bot, applied, "armor", Attributes.ARMOR);
        apply(bot, applied, "armor_toughness", Attributes.ARMOR_TOUGHNESS);
        apply(bot, applied, "attack_damage", Attributes.ATTACK_DAMAGE);
        apply(bot, applied, "movement_speed", Attributes.MOVEMENT_SPEED);
        apply(bot, applied, "follow_range", Attributes.FOLLOW_RANGE);
        apply(bot, applied, "knockback_resistance", Attributes.KNOCKBACK_RESISTANCE);
        if (bot.getHealth() > bot.getMaxHealth()) bot.setHealth(bot.getMaxHealth());
    }
    private static void apply(OperativeEntity bot, Map<String, Double> applied, String key,
                              net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute) {
        double configured = value(bot, key);
        if (!java.util.Objects.equals(applied.put(key, configured), configured))
            bot.getAttribute(attribute).setBaseValue(configured);
    }
    public static float damage(DamageSource source, float amount) {
        if (source.getEntity() instanceof OperativeEntity bot) {
            double multiplier = 1 + value(bot, "damage_bonus");
            if ("tacz.bullet".equals(source.getMsgId()) || "tacz".equals(source.typeHolder().unwrapKey()
                    .map(key -> key.location().getNamespace()).orElse(""))) multiplier *= 1 + value(bot, "tacz_damage_bonus");
            else if (source.is(net.minecraft.world.damagesource.DamageTypes.MOB_ATTACK)) multiplier *= 1 + value(bot, "melee_damage_bonus");
            return (float)Math.min(Float.MAX_VALUE, amount * multiplier);
        }
        return amount;
    }
}
