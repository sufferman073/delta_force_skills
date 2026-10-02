package com.rzy.dealt_force_skills.shop;

import com.rzy.dealt_force_skills.character.ghroth.GhrothTaczEnhancement;
import com.rzy.dealt_force_skills.config.DealtShopSetConfig;
import com.rzy.dealt_force_skills.config.DealtForceShopConfig;
import com.rzy.dealt_force_skills.item.DfsItemQuality;
import com.rzy.dealt_force_skills.registry.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class DfsShopCatalog {
    private static final List<Spec> BASE_ENTRIES = List.of(
            entry("boonie_hat", ModItems.BOONIE_HAT, Category.HELMET, DfsItemQuality.WHITE, 700),
            entry("outdoor_baseball_cap", ModItems.OUTDOOR_BASEBALL_CAP, Category.HELMET, DfsItemQuality.WHITE, 900),
            entry("h01_tactical_helmet", ModItems.H01_TACTICAL_HELMET, Category.HELMET, DfsItemQuality.GREEN, 1300),
            entry("mc_ballistic_helmet", ModItems.MC_BALLISTIC_HELMET, Category.HELMET, DfsItemQuality.GREEN, 1500),
            entry("das_ballistic_helmet", ModItems.DAS_BALLISTIC_HELMET, Category.HELMET, DfsItemQuality.BLUE, 3200),
            entry("h07_tactical_helmet", ModItems.H07_TACTICAL_HELMET, Category.HELMET, DfsItemQuality.BLUE, 3000),
            entry("mc201_ballistic_helmet", ModItems.MC201_BALLISTIC_HELMET, Category.HELMET, DfsItemQuality.BLUE, 3600),
            entry("riot_helmet", ModItems.RIOT_HELMET, Category.HELMET, DfsItemQuality.BLUE, 3400),
            entry("d6_tactical_helmet", ModItems.D6_TACTICAL_HELMET, Category.HELMET, DfsItemQuality.PURPLE, 9000),
            entry("dich_training_helmet", ModItems.DICH_TRAINING_HELMET, Category.HELMET, DfsItemQuality.PURPLE, 8800),
            entry("gt1_tactical_helmet", ModItems.GT1_TACTICAL_HELMET, Category.HELMET, DfsItemQuality.PURPLE, 9600),
            entry("mhs_tactical_helmet", ModItems.MHS_TACTICAL_HELMET, Category.HELMET, DfsItemQuality.PURPLE, 10000),
            entry("gn_endurance_heavy_night_vision_helmet", ModItems.GN_ENDURANCE_HEAVY_NIGHT_VISION_HELMET, Category.HELMET, DfsItemQuality.GOLD, 14500),
            entry("mask1_iron_wall_helmet", ModItems.MASK1_IRON_WALL_HELMET, Category.HELMET, DfsItemQuality.GOLD, 18000),
            entry("dich1_tactical_helmet", ModItems.DICH1_TACTICAL_HELMET, Category.HELMET, DfsItemQuality.GOLD, 17000),
            entry("gn_heavy_night_vision_helmet", ModItems.GN_HEAVY_NIGHT_VISION_HELMET, Category.HELMET, DfsItemQuality.GOLD, 22000),
            entry("gn_heavy_helmet", ModItems.GN_HEAVY_HELMET, Category.HELMET, DfsItemQuality.GOLD, 19000),
            entry("h09_riot_helmet", ModItems.H09_RIOT_HELMET, Category.HELMET, DfsItemQuality.GOLD, 18000),
            entry("red_owl_armored_mask", ModItems.RED_OWL_ARMORED_MASK, Category.HELMET, DfsItemQuality.GOLD, 23000),
            entry("h70_elite_helmet", ModItems.H70_ELITE_HELMET, Category.HELMET, DfsItemQuality.RED, 34000),
            entry("h70_night_vision_elite_helmet", ModItems.H70_NIGHT_VISION_ELITE_HELMET, Category.HELMET, DfsItemQuality.RED, 39000),
            entry("dich9_heavy_helmet", ModItems.DICH9_HEAVY_HELMET, Category.HELMET, DfsItemQuality.RED, 38000),
            entry("gt5_commander_helmet", ModItems.GT5_COMMANDER_HELMET, Category.HELMET, DfsItemQuality.RED, 42000),

            entry("nylon_body_armor", ModItems.NYLON_BODY_ARMOR, Category.ARMOR, DfsItemQuality.WHITE, 1200),
            entry("light_body_armor", ModItems.LIGHT_BODY_ARMOR, Category.ARMOR, DfsItemQuality.WHITE, 1400),
            entry("simple_stab_vest", ModItems.SIMPLE_STAB_VEST, Category.ARMOR, DfsItemQuality.GREEN, 2200),
            entry("universal_tactical_vest", ModItems.UNIVERSAL_TACTICAL_VEST, Category.ARMOR, DfsItemQuality.GREEN, 2700),
            entry("standard_ballistic_vest", ModItems.STANDARD_BALLISTIC_VEST, Category.ARMOR, DfsItemQuality.BLUE, 4200),
            entry("hvk_quick_release_body_armor", ModItems.HVK_QUICK_RELEASE_BODY_ARMOR, Category.ARMOR, DfsItemQuality.BLUE, 5200),
            entry("tg_h_body_armor", ModItems.TG_H_BODY_ARMOR, Category.ARMOR, DfsItemQuality.BLUE, 5400),
            entry("marksman_tactical_vest", ModItems.MARKSMAN_TACTICAL_VEST, Category.ARMOR, DfsItemQuality.BLUE, 6200),
            entry("samurai_ballistic_vest", ModItems.SAMURAI_BALLISTIC_VEST, Category.ARMOR, DfsItemQuality.PURPLE, 9000),
            entry("hmp_special_duty_body_armor", ModItems.HMP_SPECIAL_DUTY_BODY_ARMOR, Category.ARMOR, DfsItemQuality.PURPLE, 10000),
            entry("assault_ballistic_vest", ModItems.ASSAULT_BALLISTIC_VEST, Category.ARMOR, DfsItemQuality.PURPLE, 11000),
            entry("mk2_tactical_vest", ModItems.MK2_TACTICAL_VEST, Category.ARMOR, DfsItemQuality.PURPLE, 12000),
            entry("dt_avs_body_armor", ModItems.DT_AVS_BODY_ARMOR, Category.ARMOR, DfsItemQuality.PURPLE, 11500),
            entry("elite_ballistic_vest", ModItems.ELITE_BALLISTIC_VEST, Category.ARMOR, DfsItemQuality.GOLD, 18000),
            entry("red_owl_heavy_assault_vest", ModItems.RED_OWL_HEAVY_ASSAULT_VEST, Category.ARMOR, DfsItemQuality.GOLD, 21000),
            entry("hvk2_body_armor", ModItems.HVK2_BODY_ARMOR, Category.ARMOR, DfsItemQuality.GOLD, 22000),
            entry("fs_composite_body_armor", ModItems.FS_COMPOSITE_BODY_ARMOR, Category.ARMOR, DfsItemQuality.GOLD, 23000),
            entry("heavy_assault_vest", ModItems.HEAVY_ASSAULT_VEST, Category.ARMOR, DfsItemQuality.GOLD, 24000),
            entry("ha2_heavy_body_armor", ModItems.HA2_HEAVY_BODY_ARMOR, Category.ARMOR, DfsItemQuality.RED, 36000),
            entry("trick_mas2_armor", ModItems.TRICK_MAS2_ARMOR, Category.ARMOR, DfsItemQuality.RED, 42000),
            entry("king_kong_body_armor", ModItems.KING_KONG_BODY_ARMOR, Category.ARMOR, DfsItemQuality.RED, 45000),
            entry("titan_ballistic_armor", ModItems.TITAN_BALLISTIC_ARMOR, Category.ARMOR, DfsItemQuality.RED, 52000),

            entry("car_first_aid_kit", ModItems.CAR_FIRST_AID_KIT, Category.MEDICINE, DfsItemQuality.WHITE, 900),
            entry("simple_injector", ModItems.SIMPLE_INJECTOR, Category.MEDICINE, DfsItemQuality.WHITE, 700),
            entry("elastic_bandage", ModItems.ELASTIC_BANDAGE, Category.MEDICINE, DfsItemQuality.WHITE, 350),
            entry("cat_tourniquet", ModItems.CAT_TOURNIQUET, Category.MEDICINE, DfsItemQuality.WHITE, 550),
            entry("sustained_release_painkiller", ModItems.SUSTAINED_RELEASE_PAINKILLER, Category.MEDICINE, DfsItemQuality.WHITE, 700),
            entry("bottled_antibiotics", ModItems.BOTTLED_ANTIBIOTICS, Category.MEDICINE, DfsItemQuality.WHITE, 950),
            entry("dve_painkiller", ModItems.DVE_PAINKILLER, Category.MEDICINE, DfsItemQuality.WHITE, 1400),
            entry("simple_surgical_pack", ModItems.SIMPLE_SURGICAL_PACK, Category.MEDICINE, DfsItemQuality.WHITE, 1200),
            entry("tactical_quick_surgical_pack", ModItems.TACTICAL_QUICK_SURGICAL_PACK, Category.MEDICINE, DfsItemQuality.WHITE, 1800),
            entry("dek_field_surgical_pack", ModItems.DEK_FIELD_SURGICAL_PACK, Category.MEDICINE, DfsItemQuality.WHITE, 2500),
            entry("strong_injector", ModItems.STRONG_INJECTOR, Category.MEDICINE, DfsItemQuality.GREEN, 1300),
            entry("field_first_aid_kit", ModItems.FIELD_FIRST_AID_KIT, Category.MEDICINE, DfsItemQuality.BLUE, 2600),
            entry("outdoor_medical_kit", ModItems.OUTDOOR_MEDICAL_KIT, Category.MEDICINE, DfsItemQuality.PURPLE, 5200),
            entry("battlefield_medical_kit", ModItems.BATTLEFIELD_MEDICAL_KIT, Category.MEDICINE, DfsItemQuality.GOLD, 9000),

            entry("m1_muscle_booster", ModItems.M1_MUSCLE_BOOSTER, Category.INJECTION_REPAIR, DfsItemQuality.BLUE, 1800),
            entry("oe2_combat_stimulant", ModItems.OE2_COMBAT_STIMULANT, Category.INJECTION_REPAIR, DfsItemQuality.BLUE, 1800),
            entry("stamina_activation_injection", ModItems.STAMINA_ACTIVATION_INJECTION, Category.INJECTION_REPAIR, DfsItemQuality.BLUE, 1800),
            entry("norepinephrine", ModItems.NOREPINEPHRINE, Category.INJECTION_REPAIR, DfsItemQuality.BLUE, 2200),
            entry("perception_activation_injection", ModItems.PERCEPTION_ACTIVATION_INJECTION, Category.INJECTION_REPAIR, DfsItemQuality.BLUE, 1800),
            entry("self_made_armor_repair_kit", ModItems.SELF_MADE_ARMOR_REPAIR_KIT, Category.INJECTION_REPAIR, DfsItemQuality.BLUE, 4000),
            entry("self_made_helmet_repair_kit", ModItems.SELF_MADE_HELMET_REPAIR_KIT, Category.INJECTION_REPAIR, DfsItemQuality.BLUE, 2600),
            entry("m1_muscle_injection", ModItems.M1_MUSCLE_INJECTION, Category.INJECTION_REPAIR, DfsItemQuality.PURPLE, 3600),
            entry("stamina_enhancer", ModItems.STAMINA_ENHANCER, Category.INJECTION_REPAIR, DfsItemQuality.PURPLE, 3600),
            entry("perception_activator", ModItems.PERCEPTION_ACTIVATOR, Category.INJECTION_REPAIR, DfsItemQuality.PURPLE, 3600),
            entry("standard_armor_repair_kit", ModItems.STANDARD_ARMOR_REPAIR_KIT, Category.INJECTION_REPAIR, DfsItemQuality.PURPLE, 7000),
            entry("standard_helmet_repair_kit", ModItems.STANDARD_HELMET_REPAIR_KIT, Category.INJECTION_REPAIR, DfsItemQuality.PURPLE, 4500),
            entry("precision_armor_repair_kit", ModItems.PRECISION_ARMOR_REPAIR_KIT, Category.INJECTION_REPAIR, DfsItemQuality.GOLD, 12000),
            entry("precision_helmet_repair_kit", ModItems.PRECISION_HELMET_REPAIR_KIT, Category.INJECTION_REPAIR, DfsItemQuality.GOLD, 8000),
            entry("advanced_armor_repair_kit", ModItems.ADVANCED_ARMOR_REPAIR_KIT, Category.INJECTION_REPAIR, DfsItemQuality.RED, 18000),
            entry("advanced_helmet_repair_kit", ModItems.ADVANCED_HELMET_REPAIR_KIT, Category.INJECTION_REPAIR, DfsItemQuality.RED, 12000),

            entry("prototype_madness_compound", ModItems.PROTOTYPE_MADNESS_COMPOUND, Category.SPECIAL, DfsItemQuality.PURPLE, 6500),
            entry("raven_shadow_extract", ModItems.RAVEN_SHADOW_EXTRACT, Category.SPECIAL, DfsItemQuality.PURPLE, 7000),
            entry("special_equipment_supply", ModItems.SPECIAL_EQUIPMENT_SUPPLY, Category.SPECIAL, DfsItemQuality.GOLD, 10000),
            entry("experimental_iron_curtain_catalyst", ModItems.EXPERIMENTAL_IRON_CURTAIN_CATALYST, Category.SPECIAL, DfsItemQuality.GOLD, 12000),
            entry("dark_zone_rainbow_injection", ModItems.DARK_ZONE_RAINBOW_INJECTION, Category.SPECIAL, DfsItemQuality.RED, 26000),
            entry("guanshan_nanyue", ModItems.GUANSHAN_NANYUE, Category.SPECIAL, DfsItemQuality.RED, 3500),
            entry("highball", ModItems.HIGHBALL, Category.SPECIAL, DfsItemQuality.RED, 3500),
            entry("heartbeat", ModItems.HEARTBEAT, Category.SPECIAL, DfsItemQuality.RED, 3500),
            entry("starlight_glitter", ModItems.STARLIGHT_GLITTER, Category.SPECIAL, DfsItemQuality.RED, 3500),
            entry("long_goodbye", ModItems.LONG_GOODBYE, Category.SPECIAL, DfsItemQuality.RED, 3500),
            entry("mountain_rain", ModItems.MOUNTAIN_RAIN, Category.SPECIAL, DfsItemQuality.RED, 3500),
            entry("sky_island", ModItems.SKY_ISLAND, Category.SPECIAL, DfsItemQuality.RED, 3500),
            entry("cranberry_special", ModItems.CRANBERRY_SPECIAL, Category.SPECIAL, DfsItemQuality.RED, 9000),
            entry("mist_morning", ModItems.MIST_MORNING, Category.SPECIAL, DfsItemQuality.RED, 3500),
            entry("tulip", ModItems.TULIP, Category.SPECIAL, DfsItemQuality.RED, 3500),
            entry("wild_rose", ModItems.WILD_ROSE, Category.SPECIAL, DfsItemQuality.RED, 3500),
            entry("asara_style", ModItems.ASARA_STYLE, Category.SPECIAL, DfsItemQuality.RED, 3500),
            entry("reis_dance", ModItems.REIS_DANCE, Category.SPECIAL, DfsItemQuality.RED, 3500)
    );
    private DfsShopCatalog() {
    }

    public static List<Entry> entries() {
        List<Entry> entries = new ArrayList<>(baseEntries(true));
        addTaczEntries(entries);
        addLoadoutEntries(entries);
        return sorted(entries);
    }

    static List<Entry> baseEntriesForArmory() {
        return sorted(baseEntries(false));
    }

    private static List<Entry> baseEntries(boolean honorEnabled) {
        List<Entry> entries = new ArrayList<>();
        for (Spec spec : BASE_ENTRIES) {
            spec.toEntry(honorEnabled).ifPresent(entries::add);
        }
        return entries;
    }

    private static List<Entry> sorted(List<Entry> entries) {
        return entries.stream()
                .sorted(Comparator.comparing(Entry::category)
                        .thenComparing(Entry::quality)
                        .thenComparingInt(Entry::price)
                        .thenComparing(Entry::id))
                .toList();
    }

    public static List<Entry> entries(Category category) {
        return entries().stream()
                .filter(entry -> entry.category() == category)
                .sorted(Comparator.comparing(Entry::quality).thenComparingInt(Entry::price).thenComparing(Entry::id))
                .toList();
    }

    public static Optional<Entry> find(String id) {
        return entries().stream()
                .filter(entry -> entry.id().equals(id))
                .findFirst();
    }

    public static Optional<HaffPriceUpdate> setHaffPrice(ItemStack stack, int price) {
        if (stack == null || stack.isEmpty()) {
            return Optional.empty();
        }
        for (Spec spec : BASE_ENTRIES) {
            if (stack.is(spec.item().get())) {
                DealtForceShopConfig.setIntValue("haff_shop.items." + spec.id() + ".price", price);
                return Optional.of(new HaffPriceUpdate(spec.id(), price));
            }
        }
        return TaczShopCatalog.find(stack).map(entry -> {
            String path = "haff_shop.tacz." + entry.type().configSection()
                    + "." + TaczShopCatalog.configKey(entry.id()) + ".price";
            DealtForceShopConfig.setIntValue(path, price);
            return new HaffPriceUpdate(entry.entryId(), price);
        });
    }

    public static ItemStack buildPurchasedStack(String entryId) {
        Optional<Entry> found = find(entryId);
        return found.map(entry -> entry.preview().copy()).orElse(ItemStack.EMPTY);
    }

    private static Spec entry(String id, DeferredHolder<Item, Item> item, Category category, DfsItemQuality quality, int price) {
        return new Spec(id, item, category, quality, price);
    }

    private static void addTaczEntries(List<Entry> entries) {
        for (TaczShopCatalog.Entry tacz : TaczShopCatalog.entries()) {
            Category category = switch (tacz.type()) {
                case GUN -> Category.TACZ_GUNS;
                case ATTACHMENT -> Category.TACZ_ATTACHMENTS;
                case AMMO -> Category.TACZ_AMMO;
            };
            DfsItemQuality quality = switch (tacz.type()) {
                case GUN -> DfsItemQuality.BLUE;
                case ATTACHMENT -> DfsItemQuality.GREEN;
                case AMMO -> DfsItemQuality.WHITE;
            };
            int price = taczPrice(tacz);
            if (!taczEnabled(tacz)) {
                continue;
            }
            entries.add(new Entry(
                    tacz.entryId(),
                    category,
                    quality,
                    price,
                    tacz.preview().copy()
            ));
        }
    }

    private static void addLoadoutEntries(List<Entry> entries) {
        for (DealtShopSetConfig.LoadoutSet set : DealtShopSetConfig.enabledSets()) {
            entries.add(new Entry(
                    set.entryId(),
                    Category.LOADOUT,
                    DfsItemQuality.GOLD,
                    set.price(),
                    DealtShopSetConfig.preview(set)
            ));
        }
    }

    private static int taczPrice(TaczShopCatalog.Entry entry) {
        int calculated;
        boolean addon = !"tacz".equals(entry.id().getNamespace());
        if (entry.type() == TaczShopCatalog.Type.GUN) {
            int base = GhrothTaczEnhancement.isLikelySniper(entry.id()) ? 22_000 : 12_000;
            calculated = Math.min(90_000, base + (addon ? 3_000 : 0) + entry.sort() * 2);
        } else if (entry.type() == TaczShopCatalog.Type.ATTACHMENT) {
            calculated = Math.min(30_000, 4_500 + (addon ? 1_500 : 0) + entry.sort());
        } else {
            calculated = Math.min(8_000, 900 + (addon ? 200 : 0) + Math.max(0, entry.sort() / 10));
        }
        return DealtForceShopConfig.intValue("haff_shop.tacz." + entry.type().configSection()
                + "." + TaczShopCatalog.configKey(entry.id()) + ".price", calculated);
    }

    private static boolean taczEnabled(TaczShopCatalog.Entry entry) {
        return DealtForceShopConfig.booleanValue("haff_shop.tacz." + entry.type().configSection()
                + "." + TaczShopCatalog.configKey(entry.id()) + ".enabled", true);
    }

    public enum Category {
        HELMET("helmet"),
        ARMOR("armor"),
        MEDICINE("medicine"),
        INJECTION_REPAIR("injection_repair"),
        SPECIAL("special"),
        TACZ_GUNS("tacz_guns"),
        TACZ_ATTACHMENTS("tacz_attachments"),
        TACZ_AMMO("tacz_ammo"),
        LOADOUT("loadout");

        private final String key;

        Category(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }

        public static Category byKey(String key) {
            for (Category category : values()) {
                if (category.key.equals(key)) {
                    return category;
                }
            }
            return HELMET;
        }
    }

    private record Spec(String id, DeferredHolder<Item, Item> item, Category category, DfsItemQuality quality, int price) {
        Optional<Entry> toEntry(boolean honorEnabled) {
            int configuredPrice = DealtForceShopConfig.intValue("haff_shop.items." + id + ".price", price);
            boolean enabled = DealtForceShopConfig.booleanValue("haff_shop.items." + id + ".enabled", true);
            if (honorEnabled && !enabled) {
                return Optional.empty();
            }
            return Optional.of(new Entry(id, category, quality, configuredPrice, new ItemStack(item.get())));
        }
    }

    public record Entry(String id, Category category, DfsItemQuality quality, int price, ItemStack preview) {
    }

    public record HaffPriceUpdate(String entryId, int price) {
    }
}
