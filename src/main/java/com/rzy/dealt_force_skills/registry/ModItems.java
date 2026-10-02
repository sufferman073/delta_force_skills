package com.rzy.dealt_force_skills.registry;

import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.config.DealtForceConfig;
import com.rzy.dealt_force_skills.item.DfsEquipmentItem;
import com.rzy.dealt_force_skills.item.AcceleratedTroopsSpawnEggItem;
import com.rzy.dealt_force_skills.item.DfsEquipmentItem.Faction;
import com.rzy.dealt_force_skills.item.DfsEquipmentItem.SpecialAbility;
import com.rzy.dealt_force_skills.item.DfsItemQuality;
import com.rzy.dealt_force_skills.item.DriftwoodItem;
import com.rzy.dealt_force_skills.item.EffectConsumableItem;
import com.rzy.dealt_force_skills.item.EternalLoveBlessingItem;
import com.rzy.dealt_force_skills.item.HarmfulCleanerItem;
import com.rzy.dealt_force_skills.item.HookAuthorBlessingItem;
import com.rzy.dealt_force_skills.item.HealingMedicineItem;
import com.rzy.dealt_force_skills.item.HvkAdvancedDisassemblyBeamEmitterItem;
import com.rzy.dealt_force_skills.item.HvkAdvancedStandardTemplateConstructorItem;
import com.rzy.dealt_force_skills.item.HvkAdvancedTreasureCompassItem;
import com.rzy.dealt_force_skills.item.HvkBrainUnitItem;
import com.rzy.dealt_force_skills.item.HvkClonePrototypeItem;
import com.rzy.dealt_force_skills.item.HvkLoreBlockItem;
import com.rzy.dealt_force_skills.item.HvkTreasureChestBoxItem;
import com.rzy.dealt_force_skills.item.HvkUniversalGlueItem;
import com.rzy.dealt_force_skills.item.InterdimensionalBlockItem;
import com.rzy.dealt_force_skills.item.LuckyCoinItem;
import com.rzy.dealt_force_skills.item.QualityTooltipItem;
import com.rzy.dealt_force_skills.item.PortableAdsDeviceItem;
import com.rzy.dealt_force_skills.item.RainbowInjectionItem;
import com.rzy.dealt_force_skills.item.RandomEffectConsumableItem;
import com.rzy.dealt_force_skills.item.RepairKitItem;
import com.rzy.dealt_force_skills.item.RespawnBeaconItem;
import com.rzy.dealt_force_skills.item.SpecialEquipmentSupplyItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, DealtForceSkillsMod.MODID);
    public static final DeferredHolder<Item, Item> AUTO_SELL_UPGRADE = ITEMS.register("auto_sell_upgrade",
            com.rzy.dealt_force_skills.compat.SophisticatedBackpackCompat::createUpgrade);

    public static final DeferredHolder<Item, Item> GTI_SPECIAL_OPERATIONS = ITEMS.register("gti_special_operations",
            () -> new BlockItem(ModBlocks.GTI_SPECIAL_OPERATIONS.get(), new Item.Properties()));

    public static final DeferredHolder<Item, Item> NYLON_BODY_ARMOR = ITEMS.register("nylon_body_armor",
            () -> armor("nylon_body_armor", DfsItemQuality.WHITE, Faction.GLOBAL_FORCES, 250, 3, 1.0D, "upper_torso",
                    0, 0, 0, 0, 0, 0, SpecialAbility.NEW_RECRUIT, 0));
    public static final DeferredHolder<Item, Item> LIGHT_BODY_ARMOR = ITEMS.register("light_body_armor",
            () -> armor("light_body_armor", DfsItemQuality.WHITE, Faction.GTI, 300, 3, 1.0D, "upper_torso",
                    0, 0, 0, 0, 0, 0, SpecialAbility.NEW_RECRUIT, 0));
    public static final DeferredHolder<Item, Item> SIMPLE_STAB_VEST = ITEMS.register("simple_stab_vest",
            () -> armor("simple_stab_vest", DfsItemQuality.GREEN, Faction.ASARA, 300, 4, 1.0D, "upper_torso",
                    0, 0, 0, 0, 0, 0, SpecialAbility.FRUGAL_DISASSEMBLY, 0));
    public static final DeferredHolder<Item, Item> UNIVERSAL_TACTICAL_VEST = ITEMS.register("universal_tactical_vest",
            () -> armor("universal_tactical_vest", DfsItemQuality.GREEN, Faction.GTI, 500, 4, 2.0D, "upper_torso",
                    0, 0, 0, 0, 0, 0, SpecialAbility.FRUGAL_DISASSEMBLY, 0));
    public static final DeferredHolder<Item, Item> STANDARD_BALLISTIC_VEST = ITEMS.register("standard_ballistic_vest",
            () -> armor("standard_ballistic_vest", DfsItemQuality.BLUE, Faction.ASARA, 500, 4, 2.0D, "upper_torso",
                    0.01D, 0, 0, 0, 0, 0, SpecialAbility.COUNTER_KICK, 0));
    public static final DeferredHolder<Item, Item> HVK_QUICK_RELEASE_BODY_ARMOR = ITEMS.register("hvk_quick_release_body_armor",
            () -> armor("hvk_quick_release_body_armor", DfsItemQuality.BLUE, Faction.HVK, 600, 5, 4.0D, "upper_torso",
                    0.01D, 0.01D, 0, 0, 0, 0, SpecialAbility.COUNTER_KICK, 0));
    public static final DeferredHolder<Item, Item> TG_H_BODY_ARMOR = ITEMS.register("tg_h_body_armor",
            () -> armor("tg_h_body_armor", DfsItemQuality.BLUE, Faction.GTI, 500, 5, 4.0D, "full_torso",
                    0.01D, 0.03D, 0.10D, 0.10D, 0, 0, SpecialAbility.LOOT_DUPLICATE, 0.45D));
    public static final DeferredHolder<Item, Item> MARKSMAN_TACTICAL_VEST = ITEMS.register("marksman_tactical_vest",
            () -> armor("marksman_tactical_vest", DfsItemQuality.BLUE, Faction.GLOBAL_FORCES, 850, 6, 4.0D, "full_torso",
                    0.02D, 0.04D, 0.20D, 0.15D, 0, 0, SpecialAbility.DURABLE, 0));
    public static final DeferredHolder<Item, Item> SAMURAI_BALLISTIC_VEST = ITEMS.register("samurai_ballistic_vest",
            () -> armor("samurai_ballistic_vest", DfsItemQuality.PURPLE, Faction.ASARA, 800, 6, 4.0D, "upper_torso",
                    0.05D, 0.05D, 0.15D, 0.15D, 0, 0, SpecialAbility.FRONT_IMMUNE, 0));
    public static final DeferredHolder<Item, Item> HMP_SPECIAL_DUTY_BODY_ARMOR = ITEMS.register("hmp_special_duty_body_armor",
            () -> armor("hmp_special_duty_body_armor", DfsItemQuality.PURPLE, Faction.HVK, 800, 7, 4.0D, "upper_torso",
                    0, 0, 0.10D, 0.10D, 0, 0, SpecialAbility.LOOT_DUPLICATE, 0.60D));
    public static final DeferredHolder<Item, Item> ASSAULT_BALLISTIC_VEST = ITEMS.register("assault_ballistic_vest",
            () -> armor("assault_ballistic_vest", DfsItemQuality.PURPLE, Faction.HVK, 900, 8, 4.0D, "full_torso",
                    0.01D, 0.02D, 0.20D, 0.15D, 0.05D, 0, SpecialAbility.RANDOM_F4, 0));
    public static final DeferredHolder<Item, Item> MK2_TACTICAL_VEST = ITEMS.register("mk2_tactical_vest",
            () -> armor("mk2_tactical_vest", DfsItemQuality.PURPLE, Faction.GTI, 1100, 9, 4.0D, "full_torso",
                    0.05D, 0.05D, 0.20D, 0.15D, 0.10D, 0, SpecialAbility.DURABILITY_RESTORE, 0));
    public static final DeferredHolder<Item, Item> DT_AVS_BODY_ARMOR = ITEMS.register("dt_avs_body_armor",
            () -> armor("dt_avs_body_armor", DfsItemQuality.PURPLE, Faction.GLOBAL_FORCES, 1000, 8, 4.0D, "full_torso",
                    0.03D, 0.04D, 0.20D, 0.18D, 0.07D, 0, SpecialAbility.FATAL_GUARD, 0));
    public static final DeferredHolder<Item, Item> ELITE_BALLISTIC_VEST = ITEMS.register("elite_ballistic_vest",
            () -> armor("elite_ballistic_vest", DfsItemQuality.GOLD, Faction.ASARA, 950, 9, 5.0D, "full_torso",
                    0, 0, 0.20D, 0, 0.10D, 0.10D, SpecialAbility.LONELY_DAMAGE, 0));
    public static final DeferredHolder<Item, Item> RED_OWL_HEAVY_ASSAULT_VEST = ITEMS.register("red_owl_heavy_assault_vest",
            () -> armor("red_owl_heavy_assault_vest", DfsItemQuality.GOLD, Faction.ASARA, 1050, 10, 6.0D, "full_torso",
                    0, 0.10D, 0.20D, 0, 0.15D, 0.20D, SpecialAbility.RED_OWL_REVENGE, 0));
    public static final DeferredHolder<Item, Item> HVK2_BODY_ARMOR = ITEMS.register("hvk2_body_armor",
            () -> armor("hvk2_body_armor", DfsItemQuality.GOLD, Faction.HVK, 1150, 10, 6.0D, "full_torso_arms",
                    0.07D, 0.08D, 0.20D, 0.17D, 0.18D, 0, SpecialAbility.SUMMON_REINFORCEMENTS, 0));
    public static final DeferredHolder<Item, Item> FS_COMPOSITE_BODY_ARMOR = ITEMS.register("fs_composite_body_armor",
            () -> armor("fs_composite_body_armor", DfsItemQuality.GOLD, Faction.GTI, 1050, 12, 6.0D, "full_torso",
                    0.03D, 0.05D, 0.20D, 0.15D, 0.30D, 0, SpecialAbility.STATIONARY_STACKS, 0));
    public static final DeferredHolder<Item, Item> HEAVY_ASSAULT_VEST = ITEMS.register("heavy_assault_vest",
            () -> armor("heavy_assault_vest", DfsItemQuality.GOLD, Faction.GLOBAL_FORCES, 1250, 10, 6.0D, "full_torso_arms",
                    0.05D, 0.10D, 0.40D, 0.175D, 0.15D, 0, SpecialAbility.ALLY_DEATH_SWAP, 0));
    public static final DeferredHolder<Item, Item> HA2_HEAVY_BODY_ARMOR = ITEMS.register("ha2_heavy_body_armor",
            () -> armor("ha2_heavy_body_armor", DfsItemQuality.RED, Faction.HVK, 1150, 12, 7.0D, "full_torso_arms",
                    0.03D, 0.01D, 0.50D, 0.20D, 0.20D, 0.05D, SpecialAbility.HEAVY_DODGE_SUMMON, 0));
    public static final DeferredHolder<Item, Item> TRICK_MAS2_ARMOR = ITEMS.register("trick_mas2_armor",
            () -> armor("trick_mas2_armor", DfsItemQuality.RED, Faction.GTI, 1250, 15, 7.0D, "full_torso_arms",
                    0.05D, 0.06D, 0.50D, 0.20D, 0.25D, 0, SpecialAbility.TRICK_ASSAULT, 0));
    public static final DeferredHolder<Item, Item> KING_KONG_BODY_ARMOR = ITEMS.register("king_kong_body_armor",
            () -> armor("king_kong_body_armor", DfsItemQuality.RED, Faction.ASARA, 1400, 16, 7.0D, "full_torso_arms",
                    0.07D, 0.08D, 0.45D, 0.20D, 0.20D, 0, SpecialAbility.KING_KONG_EXECUTION, 0));
    public static final DeferredHolder<Item, Item> TITAN_BALLISTIC_ARMOR = ITEMS.register("titan_ballistic_armor",
            () -> armor("titan_ballistic_armor", DfsItemQuality.RED, Faction.GLOBAL_FORCES, 1500, 20, 10.0D, "full_torso_arms",
                    0.08D, 0.12D, 1.0D, 0.20D, 0.40D, 0, SpecialAbility.ALLY_DEATH_SWAP, 0));

    public static final DeferredHolder<Item, Item> BOONIE_HAT = ITEMS.register("boonie_hat",
            () -> helmet("boonie_hat", DfsItemQuality.WHITE, Faction.ASARA, 100, 1, 0, 0, 0, 0, "none", false, false, SpecialAbility.VACATION, 0));
    public static final DeferredHolder<Item, Item> OUTDOOR_BASEBALL_CAP = ITEMS.register("outdoor_baseball_cap",
            () -> helmet("outdoor_baseball_cap", DfsItemQuality.WHITE, Faction.GTI, 120, 2, 0, 0, 0, 0.10D, "weak", false, false, SpecialAbility.NO_DURABILITY_LOSS, 0));
    public static final DeferredHolder<Item, Item> H01_TACTICAL_HELMET = ITEMS.register("h01_tactical_helmet",
            () -> helmet("h01_tactical_helmet", DfsItemQuality.GREEN, Faction.HVK, 120, 2, 1.0D, 0, 0, 0, "none", false, false, SpecialAbility.FRUGAL_DISASSEMBLY, 0));
    public static final DeferredHolder<Item, Item> MC_BALLISTIC_HELMET = ITEMS.register("mc_ballistic_helmet",
            () -> helmet("mc_ballistic_helmet", DfsItemQuality.GREEN, Faction.GLOBAL_FORCES, 200, 2, 1.0D, 0, 0, 0, "none", false, false, SpecialAbility.FRUGAL_DISASSEMBLY, 0));
    public static final DeferredHolder<Item, Item> DAS_BALLISTIC_HELMET = ITEMS.register("das_ballistic_helmet",
            () -> helmet("das_ballistic_helmet", DfsItemQuality.BLUE, Faction.GTI, 400, 3, 2.0D, 0, 0, 0, "none", false, false, SpecialAbility.LOOT_DUPLICATE, 0.45D));
    public static final DeferredHolder<Item, Item> H07_TACTICAL_HELMET = ITEMS.register("h07_tactical_helmet",
            () -> helmet("h07_tactical_helmet", DfsItemQuality.BLUE, Faction.HVK, 200, 3, 0, 0, 0, 0, "medium", false, false, SpecialAbility.COUNTER_KICK, 0));
    public static final DeferredHolder<Item, Item> MC201_BALLISTIC_HELMET = ITEMS.register("mc201_ballistic_helmet",
            () -> helmet("mc201_ballistic_helmet", DfsItemQuality.BLUE, Faction.GLOBAL_FORCES, 340, 3, 2.0D, 0.01D, 0.01D, 0.20D, "medium", true, false, SpecialAbility.DURABLE, 0));
    public static final DeferredHolder<Item, Item> RIOT_HELMET = ITEMS.register("riot_helmet",
            () -> helmet("riot_helmet", DfsItemQuality.BLUE, Faction.ASARA, 280, 3, 1.0D, 0.01D, 0.03D, -0.05D, "medium", false, false, SpecialAbility.COUNTER_KICK, 0));
    public static final DeferredHolder<Item, Item> D6_TACTICAL_HELMET = ITEMS.register("d6_tactical_helmet",
            () -> helmet("d6_tactical_helmet", DfsItemQuality.PURPLE, Faction.ASARA, 550, 4, 3.0D, 0.04D, 0.06D, -0.10D, "strong", false, false, SpecialAbility.ELBOW_SPIRIT, 0));
    public static final DeferredHolder<Item, Item> DICH_TRAINING_HELMET = ITEMS.register("dich_training_helmet",
            () -> helmet("dich_training_helmet", DfsItemQuality.PURPLE, Faction.GLOBAL_FORCES, 350, 5, 3.0D, 0.02D, 0, 0.50D, "strong", true, false, SpecialAbility.HEARING_SHARE, 0));
    public static final DeferredHolder<Item, Item> GT1_TACTICAL_HELMET = ITEMS.register("gt1_tactical_helmet",
            () -> helmet("gt1_tactical_helmet", DfsItemQuality.PURPLE, Faction.GTI, 480, 5, 3.0D, 0.04D, 0.04D, 0.20D, "medium", true, false, SpecialAbility.DURABILITY_RESTORE, 0));
    public static final DeferredHolder<Item, Item> MHS_TACTICAL_HELMET = ITEMS.register("mhs_tactical_helmet",
            () -> helmet("mhs_tactical_helmet", DfsItemQuality.PURPLE, Faction.HVK, 300, 6, 3.0D, 0.01D, 0.03D, 0.30D, "medium", true, false, SpecialAbility.MHS_FURY, 0));
    public static final DeferredHolder<Item, Item> GN_ENDURANCE_HEAVY_NIGHT_VISION_HELMET = ITEMS.register("gn_endurance_heavy_night_vision_helmet",
            () -> helmet("gn_endurance_heavy_night_vision_helmet", DfsItemQuality.GOLD, Faction.GLOBAL_FORCES, 200, 8, 5.0D, 0.03D, 0, 0.10D, "medium", true, true, SpecialAbility.ENDURANCE_TRANSFORM, 0));
    public static final DeferredHolder<Item, Item> MASK1_IRON_WALL_HELMET = ITEMS.register("mask1_iron_wall_helmet",
            () -> helmet("mask1_iron_wall_helmet", DfsItemQuality.GOLD, Faction.ASARA, 750, 7, 4.0D, 0.05D, 0.06D, -0.15D, "strong", false, false, SpecialAbility.MASK_LOCK_ON, 0));
    public static final DeferredHolder<Item, Item> DICH1_TACTICAL_HELMET = ITEMS.register("dich1_tactical_helmet",
            () -> helmet("dich1_tactical_helmet", DfsItemQuality.GOLD, Faction.GTI, 400, 7, 5.0D, 0.01D, 0.01D, 0.40D, "strong", true, false, SpecialAbility.STATIONARY_STACKS, 0));
    public static final DeferredHolder<Item, Item> GN_HEAVY_NIGHT_VISION_HELMET = ITEMS.register("gn_heavy_night_vision_helmet",
            () -> helmet("gn_heavy_night_vision_helmet", DfsItemQuality.GOLD, Faction.GLOBAL_FORCES, 500, 8, 5.0D, 0.03D, 0.04D, 0.10D, "medium", true, true, SpecialAbility.GN_HEAVY_NIGHT_VISION, 0));
    public static final DeferredHolder<Item, Item> GN_HEAVY_HELMET = ITEMS.register("gn_heavy_helmet",
            () -> helmet("gn_heavy_helmet", DfsItemQuality.GOLD, Faction.GLOBAL_FORCES, 500, 8, 5.0D, 0.03D, 0.04D, 0.10D, "medium", true, false, SpecialAbility.GN_HEAVY, 0));
    public static final DeferredHolder<Item, Item> H09_RIOT_HELMET = ITEMS.register("h09_riot_helmet",
            () -> helmet("h09_riot_helmet", DfsItemQuality.GOLD, Faction.HVK, 450, 7, 5.0D, 0.03D, 0.04D, 0.20D, "medium", true, false, SpecialAbility.H09_RIOT_DODGE, 0));
    public static final DeferredHolder<Item, Item> RED_OWL_ARMORED_MASK = ITEMS.register("red_owl_armored_mask",
            () -> helmet("red_owl_armored_mask", DfsItemQuality.GOLD, Faction.ASARA, 750, 7, 5.0D, 0, 0.05D, 0.40D, "strong", true, true, SpecialAbility.RED_OWL_MASK, 0));
    public static final DeferredHolder<Item, Item> H70_ELITE_HELMET = ITEMS.register("h70_elite_helmet",
            () -> helmet("h70_elite_helmet", DfsItemQuality.RED, Faction.HVK, 550, 10, 6.0D, 0.03D, 0.04D, 0.10D, "medium", true, false, SpecialAbility.H70_FOLLOWER_BOOST, 0));
    public static final DeferredHolder<Item, Item> H70_NIGHT_VISION_ELITE_HELMET = ITEMS.register("h70_night_vision_elite_helmet",
            () -> helmet("h70_night_vision_elite_helmet", DfsItemQuality.RED, Faction.HVK, 550, 10, 6.0D, 0.03D, 0.04D, 0.10D, "medium", true, true, SpecialAbility.H70_NIGHT_FOLLOWER_BOOST, 0));
    public static final DeferredHolder<Item, Item> DICH9_HEAVY_HELMET = ITEMS.register("dich9_heavy_helmet",
            () -> helmet("dich9_heavy_helmet", DfsItemQuality.RED, Faction.GTI, 500, 12, 6.0D, 0.01D, 0.03D, 0.40D, "medium", false, false, SpecialAbility.DICH9_ASSAULT, 0));
    public static final DeferredHolder<Item, Item> GT5_COMMANDER_HELMET = ITEMS.register("gt5_commander_helmet",
            () -> helmet("gt5_commander_helmet", DfsItemQuality.RED, Faction.GLOBAL_FORCES, 600, 14, 7.0D, 0.03D, 0.08D, 0.20D, "medium", true, false, SpecialAbility.GT5_STATIONARY_REDUCTION, 0));

    public static final DeferredHolder<Item, Item> SELF_MADE_ARMOR_REPAIR_KIT = ITEMS.register("self_made_armor_repair_kit",
            () -> armorRepairKit(DfsItemQuality.BLUE, 500, "self_made_armor_repair_kit"));
    public static final DeferredHolder<Item, Item> STANDARD_ARMOR_REPAIR_KIT = ITEMS.register("standard_armor_repair_kit",
            () -> armorRepairKit(DfsItemQuality.PURPLE, 750, "standard_armor_repair_kit"));
    public static final DeferredHolder<Item, Item> PRECISION_ARMOR_REPAIR_KIT = ITEMS.register("precision_armor_repair_kit",
            () -> armorRepairKit(DfsItemQuality.GOLD, 1200, "precision_armor_repair_kit"));
    public static final DeferredHolder<Item, Item> ADVANCED_ARMOR_REPAIR_KIT = ITEMS.register("advanced_armor_repair_kit",
            () -> armorRepairKit(DfsItemQuality.RED, 2000, "advanced_armor_repair_kit"));

    public static final DeferredHolder<Item, Item> SELF_MADE_HELMET_REPAIR_KIT = ITEMS.register("self_made_helmet_repair_kit",
            () -> helmetRepairKit(DfsItemQuality.BLUE, 300, "self_made_helmet_repair_kit"));
    public static final DeferredHolder<Item, Item> STANDARD_HELMET_REPAIR_KIT = ITEMS.register("standard_helmet_repair_kit",
            () -> helmetRepairKit(DfsItemQuality.PURPLE, 500, "standard_helmet_repair_kit"));
    public static final DeferredHolder<Item, Item> PRECISION_HELMET_REPAIR_KIT = ITEMS.register("precision_helmet_repair_kit",
            () -> helmetRepairKit(DfsItemQuality.GOLD, 750, "precision_helmet_repair_kit"));
    public static final DeferredHolder<Item, Item> ADVANCED_HELMET_REPAIR_KIT = ITEMS.register("advanced_helmet_repair_kit",
            () -> helmetRepairKit(DfsItemQuality.RED, 1000, "advanced_helmet_repair_kit"));

    public static final DeferredHolder<Item, Item> ELASTIC_BANDAGE = ITEMS.register("elastic_bandage",
            () -> new HarmfulCleanerItem(durableConsumable("elastic_bandage", 2), DfsItemQuality.WHITE, tooltip("elastic_bandage"),
                    3 * 20, 0, false,
                    ModSounds.ITEM_HEMOSTATIC_START, ModSounds.ITEM_HEMOSTATIC_FINISH,
                    message("elastic_bandage_start"), message("elastic_bandage_finish"), false, true));
    public static final DeferredHolder<Item, Item> CAT_TOURNIQUET = ITEMS.register("cat_tourniquet",
            () -> new HarmfulCleanerItem(durableConsumable("cat_tourniquet", 4), DfsItemQuality.WHITE, tooltip("cat_tourniquet"),
                    20, 1, false,
                    ModSounds.ITEM_HEMOSTATIC_START, ModSounds.ITEM_HEMOSTATIC_FINISH,
                    message("cat_tourniquet_start"), message("cat_tourniquet_finish"), false, true));

    public static final DeferredHolder<Item, Item> SUSTAINED_RELEASE_PAINKILLER = ITEMS.register("sustained_release_painkiller",
            () -> painReliefItem(durableConsumable("sustained_release_painkiller", 1), "sustained_release_painkiller", 200 * 20,
                    ModSounds.ITEM_SUSTAINED_RELEASE_PAINKILLER_START,
                    ModSounds.ITEM_SUSTAINED_RELEASE_PAINKILLER_FINISH));
    public static final DeferredHolder<Item, Item> BOTTLED_ANTIBIOTICS = ITEMS.register("bottled_antibiotics",
            () -> painReliefItem(durableConsumable("bottled_antibiotics", 3), "bottled_antibiotics", 240 * 20));
    public static final DeferredHolder<Item, Item> DVE_PAINKILLER = ITEMS.register("dve_painkiller",
            () -> painReliefItem(durableConsumable("dve_painkiller", 5), "dve_painkiller", 360 * 20));

    public static final DeferredHolder<Item, Item> SIMPLE_SURGICAL_PACK = ITEMS.register("simple_surgical_pack",
            () -> new HarmfulCleanerItem(durableConsumable("simple_surgical_pack", 2), DfsItemQuality.WHITE, tooltip("simple_surgical_pack"),
                    8 * 20, Integer.MAX_VALUE, false,
                    ModSounds.ITEM_SURGICAL_START, ModSounds.ITEM_SURGICAL_FINISH,
                    message("simple_surgical_pack_start"), message("simple_surgical_pack_finish")));
    public static final DeferredHolder<Item, Item> TACTICAL_QUICK_SURGICAL_PACK = ITEMS.register("tactical_quick_surgical_pack",
            () -> new HarmfulCleanerItem(durableConsumable("tactical_quick_surgical_pack", 4), DfsItemQuality.WHITE, tooltip("tactical_quick_surgical_pack"),
                    5 * 20, Integer.MAX_VALUE, true,
                    ModSounds.ITEM_SURGICAL_START, ModSounds.ITEM_SURGICAL_FINISH,
                    message("tactical_quick_surgical_pack_start"), message("tactical_quick_surgical_pack_finish"),
                    true));
    public static final DeferredHolder<Item, Item> DEK_FIELD_SURGICAL_PACK = ITEMS.register("dek_field_surgical_pack",
            () -> new HarmfulCleanerItem(durableConsumable("dek_field_surgical_pack", 7), DfsItemQuality.WHITE, tooltip("dek_field_surgical_pack"),
                    70, Integer.MAX_VALUE, true,
                    ModSounds.ITEM_SURGICAL_START, ModSounds.ITEM_SURGICAL_FINISH,
                    message("dek_field_surgical_pack_start"), message("dek_field_surgical_pack_finish"),
                    true));

    public static final DeferredHolder<Item, Item> PROTOTYPE_MADNESS_COMPOUND = ITEMS.register("prototype_madness_compound",
            () -> new EffectConsumableItem(new Item.Properties().stacksTo(16), DfsItemQuality.PURPLE,
                    tooltip("prototype_madness_compound"), 3 * 20,
                    ModSounds.ITEM_INJECTION_START, ModSounds.ITEM_INJECTION_FINISH,
                    message("injection_start"), message("prototype_madness_compound_finish"),
                    List.of(new EffectConsumableItem.EffectEntry(ModEffects.HELA, 120 * 20, 0))));
    public static final DeferredHolder<Item, Item> SPECIAL_EQUIPMENT_SUPPLY = ITEMS.register("special_equipment_supply",
            () -> new SpecialEquipmentSupplyItem(new Item.Properties().stacksTo(16), DfsItemQuality.GOLD,
                    tooltip("special_equipment_supply"), 5 * 20,
                    ModSounds.ITEM_SPECIAL_SUPPLY_START, ModSounds.ITEM_SPECIAL_SUPPLY_FINISH,
                    message("special_equipment_supply_start"), message("special_equipment_supply_finish")));
    public static final DeferredHolder<Item, Item> RAVEN_SHADOW_EXTRACT = ITEMS.register("raven_shadow_extract",
            () -> new RandomEffectConsumableItem(new Item.Properties().stacksTo(16), DfsItemQuality.PURPLE,
                    tooltip("raven_shadow_extract"), 3 * 20,
                    ModSounds.ITEM_INJECTION_START, ModSounds.ITEM_INJECTION_FINISH,
                    message("injection_start"), message("raven_shadow_extract_finish"),
                    List.of(
                            List.of(new EffectConsumableItem.EffectEntry(ModEffects.LAUGHING_MANIA_I, 360 * 20, 0),
                                    new EffectConsumableItem.EffectEntry(ModEffects.ITEM_WEAKNESS, 20 * 20, 0)),
                            List.of(new EffectConsumableItem.EffectEntry(ModEffects.LAUGHING_MANIA_II, 360 * 20, 0),
                                    new EffectConsumableItem.EffectEntry(ModEffects.ITEM_WEAKNESS, 360 * 20, 0),
                                    effect(ModEffects.BURNING_HEART, 360, 0))
                    )));
    public static final DeferredHolder<Item, Item> DARK_ZONE_RAINBOW_INJECTION = ITEMS.register("dark_zone_rainbow_injection",
            () -> new RainbowInjectionItem(new Item.Properties().stacksTo(16), DfsItemQuality.RED,
                    tooltip("dark_zone_rainbow_injection"), 3 * 20,
                    ModSounds.ITEM_INJECTION_START, ModSounds.ITEM_INJECTION_FINISH,
                    message("injection_start"), message("dark_zone_rainbow_injection_finish")));
    public static final DeferredHolder<Item, Item> EXPERIMENTAL_IRON_CURTAIN_CATALYST = ITEMS.register("experimental_iron_curtain_catalyst",
            () -> new EffectConsumableItem(new Item.Properties().stacksTo(16), DfsItemQuality.GOLD,
                    tooltip("experimental_iron_curtain_catalyst"), 3 * 20,
                    ModSounds.ITEM_INJECTION_START, ModSounds.ITEM_INJECTION_FINISH,
                    message("injection_start"), message("experimental_iron_curtain_catalyst_finish"),
                    List.of(new EffectConsumableItem.EffectEntry(ModEffects.LAUGHING_MANIA_III, 360 * 20, 0))));

    public static final DeferredHolder<Item, Item> GUANSHAN_NANYUE = ITEMS.register("guanshan_nanyue",
            () -> beverage("guanshan_nanyue", List.of(
                    effect(ModEffects.LARGE_INTERACTION_SPEED, 300, 0),
                    effect(ModEffects.ITEM_WEAKNESS, 300, 0))));
    public static final DeferredHolder<Item, Item> HIGHBALL = ITEMS.register("highball",
            () -> beverage("highball", List.of(
                    effect(ModEffects.SMALL_INTERACTION_SPEED, 300, 0),
                    effect(ModEffects.FATIGUE_REMOVAL, 300, 0))));
    public static final DeferredHolder<Item, Item> HEARTBEAT = ITEMS.register("heartbeat",
            () -> beverage("heartbeat", List.of(
                    effect(ModEffects.LARGE_RELOAD_SPEED, 300, 0),
                    effect(ModEffects.LARGE_ITEM_USE_SPEED, 300, 0))));
    public static final DeferredHolder<Item, Item> STARLIGHT_GLITTER = ITEMS.register("starlight_glitter",
            () -> beverage("starlight_glitter", List.of(
                    effect(ModEffects.LARGE_FIRE_RATE, 300, 0),
                    effect(ModEffects.LARGE_AIM_SPEED, 300, 0))));
    public static final DeferredHolder<Item, Item> LONG_GOODBYE = ITEMS.register("long_goodbye",
            () -> beverage("long_goodbye", List.of(
                    effect(ModEffects.STAMINA_BOOST, 200, 1),
                    effect(ModEffects.BURNING_HEART, 600, 0))));
    public static final DeferredHolder<Item, Item> MOUNTAIN_RAIN = ITEMS.register("mountain_rain",
            () -> beverage("mountain_rain", List.of(effect(ModEffects.STAMINA_BOOST, 300, 2))));
    public static final DeferredHolder<Item, Item> SKY_ISLAND = ITEMS.register("sky_island",
            () -> beverage("sky_island", List.of(
                    effect(ModEffects.MEDIUM_RELOAD_SPEED, 300, 0),
                    effect(ModEffects.SMALL_AIM_SPEED, 300, 0),
                    effect(ModEffects.SMALL_ITEM_USE_SPEED, 300, 0))));
    public static final DeferredHolder<Item, Item> CRANBERRY_SPECIAL = ITEMS.register("cranberry_special",
            () -> new RandomEffectConsumableItem(new Item.Properties().stacksTo(16), DfsItemQuality.RED,
                    tooltip("cranberry_special"), 3 * 20,
                    ModSounds.ITEM_BEVERAGE_START, ModSounds.ITEM_BEVERAGE_FINISH,
                    message("beverage_start"), message("cranberry_special_finish"),
                    List.of(
                            doubled(List.of(effect(ModEffects.LARGE_INTERACTION_SPEED, 300, 0), effect(ModEffects.ITEM_WEAKNESS, 300, 0))),
                            doubled(List.of(effect(ModEffects.SMALL_INTERACTION_SPEED, 300, 0), effect(ModEffects.FATIGUE_REMOVAL, 300, 0))),
                            doubled(List.of(effect(ModEffects.LARGE_RELOAD_SPEED, 300, 0), effect(ModEffects.LARGE_ITEM_USE_SPEED, 300, 0))),
                            doubled(List.of(effect(ModEffects.LARGE_FIRE_RATE, 300, 0), effect(ModEffects.LARGE_AIM_SPEED, 300, 0))),
                            doubled(List.of(effect(ModEffects.STAMINA_BOOST, 200, 1), effect(ModEffects.BURNING_HEART, 600, 0))),
                            doubled(List.of(effect(ModEffects.STAMINA_BOOST, 300, 2))),
                            doubled(List.of(effect(ModEffects.MEDIUM_RELOAD_SPEED, 300, 0), effect(ModEffects.SMALL_AIM_SPEED, 300, 0), effect(ModEffects.SMALL_ITEM_USE_SPEED, 300, 0))),
                            doubled(List.of(effect(ModEffects.CONTINUOUS_HEALING, 60, 0), effect(ModEffects.SEDATION, 180, 0))),
                            doubled(List.of(effect(ModEffects.CARRY_BOOST, 300, 1), effect(ModEffects.STAMINA_BOOST, 90, 0))),
                            doubled(List.of(effect(ModEffects.LARGE_INTERACTION_SPEED, 300, 0), effect(ModEffects.LARGE_AIM_PENALTY, 300, 0))),
                            doubled(List.of(effect(ModEffects.CONTINUOUS_HEALING, 120, 0), effect(ModEffects.SEDATION, 120, 0))),
                            doubled(List.of(effect(ModEffects.CONTINUOUS_HEALING, 180, 0), effect(ModEffects.SEDATION, 60, 0)))
                    )));
    public static final DeferredHolder<Item, Item> MIST_MORNING = ITEMS.register("mist_morning",
            () -> beverage("mist_morning", List.of(
                    effect(ModEffects.CONTINUOUS_HEALING, 60, 0),
                    effect(ModEffects.SEDATION, 180, 0))));
    public static final DeferredHolder<Item, Item> TULIP = ITEMS.register("tulip",
            () -> beverage("tulip", List.of(
                    effect(ModEffects.CARRY_BOOST, 300, 1),
                    effect(ModEffects.STAMINA_BOOST, 90, 0))));
    public static final DeferredHolder<Item, Item> WILD_ROSE = ITEMS.register("wild_rose",
            () -> beverage("wild_rose", List.of(
                    effect(ModEffects.LARGE_INTERACTION_SPEED, 300, 0),
                    effect(ModEffects.LARGE_AIM_PENALTY, 300, 0))));
    public static final DeferredHolder<Item, Item> ASARA_STYLE = ITEMS.register("asara_style",
            () -> beverage("asara_style", List.of(
                    effect(ModEffects.CONTINUOUS_HEALING, 120, 0),
                    effect(ModEffects.SEDATION, 120, 0))));
    public static final DeferredHolder<Item, Item> REIS_DANCE = ITEMS.register("reis_dance",
            () -> beverage("reis_dance", List.of(
                    effect(ModEffects.CONTINUOUS_HEALING, 180, 0),
                    effect(ModEffects.SEDATION, 60, 0))));

    public static final DeferredHolder<Item, Item> M1_MUSCLE_BOOSTER = ITEMS.register("m1_muscle_booster",
            () -> injection("m1_muscle_booster", 3 * 20, List.of(effect(ModEffects.CARRY_BOOST, 180, 0))));
    public static final DeferredHolder<Item, Item> M1_MUSCLE_INJECTION = ITEMS.register("m1_muscle_injection",
            () -> injection("m1_muscle_injection", DfsItemQuality.PURPLE, 3 * 20, List.of(effect(ModEffects.CARRY_BOOST, 360, 1))));
    public static final DeferredHolder<Item, Item> OE2_COMBAT_STIMULANT = ITEMS.register("oe2_combat_stimulant",
            () -> injection("oe2_combat_stimulant", 30, List.of(

                    effect(ModEffects.FATIGUE_REMOVAL, 30, 0))));
    public static final DeferredHolder<Item, Item> STAMINA_ACTIVATION_INJECTION = ITEMS.register("stamina_activation_injection",
            () -> injection("stamina_activation_injection", 3 * 20, List.of(effect(ModEffects.STAMINA_BOOST, 180, 0))));
    public static final DeferredHolder<Item, Item> STAMINA_ENHANCER = ITEMS.register("stamina_enhancer",
            () -> injection("stamina_enhancer", DfsItemQuality.PURPLE, 3 * 20, List.of(effect(ModEffects.STAMINA_BOOST, 360, 1))));
    public static final DeferredHolder<Item, Item> NOREPINEPHRINE = ITEMS.register("norepinephrine",
            () -> injection("norepinephrine", 3 * 20, List.of(effect(ModEffects.STAMINA_CAPACITY, 120, 0))));
    public static final DeferredHolder<Item, Item> PERCEPTION_ACTIVATION_INJECTION = ITEMS.register("perception_activation_injection",
            () -> injection("perception_activation_injection", 3 * 20, List.of(effect(ModEffects.HEARING_AMPLIFICATION, 180, 0))));
    public static final DeferredHolder<Item, Item> PERCEPTION_ACTIVATOR = ITEMS.register("perception_activator",
            () -> injection("perception_activator", DfsItemQuality.PURPLE, 3 * 20, List.of(effect(ModEffects.HEARING_AMPLIFICATION, 360, 1))));

    public static final DeferredHolder<Item, Item> CAR_FIRST_AID_KIT = ITEMS.register("car_first_aid_kit",
            () -> medicine("car_first_aid_kit", DfsItemQuality.WHITE, 90, 6 * 20, 6, 0.06D, 0));
    public static final DeferredHolder<Item, Item> FIELD_FIRST_AID_KIT = ITEMS.register("field_first_aid_kit",
            () -> medicine("field_first_aid_kit", DfsItemQuality.BLUE, 220, 6 * 20, 20, 0.20D, 0));
    public static final DeferredHolder<Item, Item> SIMPLE_INJECTOR = ITEMS.register("simple_injector",
            () -> medicine("simple_injector", DfsItemQuality.WHITE, 30, 5 * 20, 12, 0.12D, 0));
    public static final DeferredHolder<Item, Item> STRONG_INJECTOR = ITEMS.register("strong_injector",
            () -> medicine("strong_injector", DfsItemQuality.GREEN, 60, 3 * 20, 10, 0.12D, 0));
    public static final DeferredHolder<Item, Item> OUTDOOR_MEDICAL_KIT = ITEMS.register("outdoor_medical_kit",
            () -> medicine("outdoor_medical_kit", DfsItemQuality.PURPLE, 350, 70, 30, 0.30D, 25));
    public static final DeferredHolder<Item, Item> BATTLEFIELD_MEDICAL_KIT = ITEMS.register("battlefield_medical_kit",
            () -> medicine("battlefield_medical_kit", DfsItemQuality.GOLD, 800, 70, 40, 0.50D, 25));

    public static final DeferredHolder<Item, Item> BALLISTIC_CERAMIC = ITEMS.register("ballistic_ceramic",
            () -> material("ballistic_ceramic"));
    public static final DeferredHolder<Item, Item> SPECIAL_STEEL = ITEMS.register("special_steel",
            () -> material("special_steel"));
    public static final DeferredHolder<Item, Item> TITANIUM_ALLOY = ITEMS.register("titanium_alloy",
            () -> material("titanium_alloy"));
    public static final DeferredHolder<Item, Item> ANIMAL_GLAND = ITEMS.register("animal_gland",
            () -> material("animal_gland"));
    public static final DeferredHolder<Item, Item> RAW_WOOD_PLANK = ITEMS.register("raw_wood_plank",
            () -> material("raw_wood_plank"));
    public static final DeferredHolder<Item, Item> PH_REGULATOR = ITEMS.register("ph_regulator",
            () -> material("ph_regulator"));
    public static final DeferredHolder<Item, Item> GLASS_SYRINGE = ITEMS.register("glass_syringe",
            () -> material("glass_syringe"));
    public static final DeferredHolder<Item, Item> ANTIOXIDANT_STABILIZER = ITEMS.register("antioxidant_stabilizer",
            () -> material("antioxidant_stabilizer"));
    public static final DeferredHolder<Item, Item> STEPMOTHER_EARRING = ITEMS.register("stepmother_earring",
            () -> material("stepmother_earring"));
    public static final DeferredHolder<Item, Item> ELECTRIC_MOTOR = ITEMS.register("electric_motor",
            () -> material("electric_motor"));
    public static final DeferredHolder<Item, Item> STERILE_DRESSING_PACK = ITEMS.register("sterile_dressing_pack",
            () -> material("sterile_dressing_pack"));
    public static final DeferredHolder<Item, Item> COARSE_SALT = ITEMS.register("coarse_salt",
            () -> material("coarse_salt"));
    public static final DeferredHolder<Item, Item> STERILE_WATER = ITEMS.register("sterile_water",
            () -> material("sterile_water"));
    public static final DeferredHolder<Item, Item> MECHANICAL_WORKER_BEE_MODEL = ITEMS.register("mechanical_worker_bee_model",
            () -> material("mechanical_worker_bee_model"));
    public static final DeferredHolder<Item, Item> SLOW_RELEASE_STABILIZER = ITEMS.register("slow_release_stabilizer",
            () -> material("slow_release_stabilizer"));
    public static final DeferredHolder<Item, Item> POLYETHYLENE_FIBER = ITEMS.register("polyethylene_fiber",
            () -> material("polyethylene_fiber"));
    public static final DeferredHolder<Item, Item> GLAND_EXTRACT = ITEMS.register("gland_extract",
            () -> material("gland_extract"));
    public static final DeferredHolder<Item, Item> ARAMID_FIBER = ITEMS.register("aramid_fiber",
            () -> material("aramid_fiber"));
    public static final DeferredHolder<Item, Item> SCREWDRIVER = ITEMS.register("screwdriver",
            () -> material("screwdriver"));
    public static final DeferredHolder<Item, Item> DANCING_LADY = ITEMS.register("dancing_lady",
            () -> material("dancing_lady", DfsItemQuality.BLUE));
    public static final DeferredHolder<Item, Item> LENS = ITEMS.register("lens",
            () -> material("lens"));
    public static final DeferredHolder<Item, Item> BLUEPRINT = ITEMS.register("blueprint",
            () -> material("blueprint", DfsItemQuality.GOLD));
    public static final DeferredHolder<Item, Item> HIGH_OUTPUT_CRUSHING_PLIERS = ITEMS.register("high_output_crushing_pliers",
            () -> material("high_output_crushing_pliers"));
    public static final DeferredHolder<Item, Item> POLYMER_FABRIC = ITEMS.register("polymer_fabric",
            () -> material("polymer_fabric"));
    public static final DeferredHolder<Item, Item> DIGITAL_CALIPER = ITEMS.register("digital_caliper",
            () -> material("digital_caliper"));
    public static final DeferredHolder<Item, Item> PREMIUM_COFFEE_BEANS = ITEMS.register("premium_coffee_beans",
            () -> material("premium_coffee_beans", DfsItemQuality.RED));

    public static final DeferredHolder<Item, Item> HVK_BRAIN_UNIT = ITEMS.register("hvk_brain_unit",
            () -> new HvkBrainUnitItem(new Item.Properties().stacksTo(16), DfsItemQuality.RED, tooltip("hvk_brain_unit")));
    public static final DeferredHolder<Item, Item> BEACON_BRAIN_UNIT = ITEMS.register("beacon_brain_unit",
            () -> material("beacon_brain_unit", DfsItemQuality.RED));
    public static final DeferredHolder<Item, Item> BEACON_BOSS_SPAWN_EGG = ITEMS.register("beacon_boss_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.BEACON_BOSS, 0x1A0A24, 0x8B3DFF, new Item.Properties()));
    public static final DeferredHolder<Item, Item> ACCELERATED_TROOPS_SPAWN_EGG = ITEMS.register(
            "accelerated_troops_spawn_egg", () -> new AcceleratedTroopsSpawnEggItem(new Item.Properties()));
    public static final DeferredHolder<Item, Item> PORTABLE_ADS_DEVICE = ITEMS.register(
            "portable_ads_device", () -> new PortableAdsDeviceItem(new Item.Properties()));
    public static final DeferredHolder<Item, Item> RESPAWN_BEACON = ITEMS.register(
            "respawn_beacon", () -> new RespawnBeaconItem(new Item.Properties()));
    public static final DeferredHolder<Item, Item> ASARA_FLAMETHROWER_SPAWN_EGG = ITEMS.register("asara_flamethrower_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.ASARA_FLAMETHROWER, 0x4A3028, 0xE06A2B, new Item.Properties()));
    public static final DeferredHolder<Item, Item> ASARA_MACHINE_GUNNER_SPAWN_EGG = ITEMS.register("asara_machine_gunner_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.ASARA_MACHINE_GUNNER, 0x313A32, 0xA9A06C, new Item.Properties()));
    public static final DeferredHolder<Item, Item> ASARA_SHIELD_TROOPER_SPAWN_EGG = ITEMS.register("asara_shield_trooper_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.ASARA_SHIELD_TROOPER, 0x333A43, 0x6D879B, new Item.Properties()));
    public static final DeferredHolder<Item, Item> ASARA_ROCKET_TROOPER_SPAWN_EGG = ITEMS.register("asara_rocket_trooper_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.ASARA_ROCKET_TROOPER, 0x403629, 0xC1A354, new Item.Properties()));
    public static final DeferredHolder<Item, Item> ASARA_SNIPER_SPAWN_EGG = ITEMS.register("asara_sniper_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.ASARA_SNIPER, 0x283735, 0x7EAA8B, new Item.Properties()));
    public static final DeferredHolder<Item, Item> ASARA_SENTRY_SNIPER_SPAWN_EGG = ITEMS.register("asara_sentry_sniper_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.ASARA_SENTRY_SNIPER, 0x1B2726, 0xB8D8BE, new Item.Properties()));
    public static final DeferredHolder<Item, Item> ASARA_GUARD_SOLDIER_SPAWN_EGG = ITEMS.register("asara_guard_soldier_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.ASARA_GUARD_SOLDIER, 0x38433A, 0xA7B18C, new Item.Properties()));
    public static final DeferredHolder<Item, Item> PROGRAMMABLE_PROCESSOR = ITEMS.register("programmable_processor",
            () -> material("programmable_processor"));
    public static final DeferredHolder<Item, Item> HVK_ADVANCED_STANDARD_TEMPLATE_CONSTRUCTOR =
            ITEMS.register("hvk_advanced_standard_template_constructor",
                    () -> new HvkAdvancedStandardTemplateConstructorItem(ModBlocks.HVK_ADVANCED_STANDARD_TEMPLATE_CONSTRUCTOR.get(),
                            new Item.Properties()));
    public static final DeferredHolder<Item, Item> HVK_ADVANCED_DISASSEMBLY_BEAM_EMITTER =
            ITEMS.register("hvk_advanced_disassembly_beam_emitter",
                    () -> new HvkAdvancedDisassemblyBeamEmitterItem(ModBlocks.HVK_ADVANCED_DISASSEMBLY_BEAM_EMITTER.get(),
                            new Item.Properties()));
    public static final DeferredHolder<Item, Item> HVK_ADVANCED_SPAWNER =
            ITEMS.register("hvk_advanced_spawner",
                    () -> new HvkLoreBlockItem(ModBlocks.HVK_ADVANCED_SPAWNER.get(), new Item.Properties(),
                            HvkLoreBlockItem.Kind.ADVANCED_SPAWNER));
    public static final DeferredHolder<Item, Item> HVK_ASARA_ARENA =
            ITEMS.register("hvk_asara_arena",
                    () -> new HvkLoreBlockItem(ModBlocks.HVK_ASARA_ARENA.get(), new Item.Properties(),
                            HvkLoreBlockItem.Kind.ASARA_ARENA));
    public static final DeferredHolder<Item, Item> HVK_ENCHANT_INFUSION =
            ITEMS.register("hvk_enchant_infusion",
                    () -> new HvkLoreBlockItem(ModBlocks.HVK_ENCHANT_INFUSION.get(), new Item.Properties(),
                            HvkLoreBlockItem.Kind.ENCHANT_INFUSION));
    public static final DeferredHolder<Item, Item> HVK_CLONE_PROTOTYPE =
            ITEMS.register("hvk_clone_prototype",
                    () -> new HvkClonePrototypeItem(ModBlocks.HVK_CLONE_PROTOTYPE.get(),
                            new Item.Properties()));
    public static final DeferredHolder<Item, Item> HVK_ADVANCED_TREASURE_COMPASS =
            ITEMS.register("hvk_advanced_treasure_compass",
                    () -> new HvkAdvancedTreasureCompassItem(ModBlocks.HVK_ADVANCED_TREASURE_COMPASS.get(),
                            new Item.Properties()));
    public static final DeferredHolder<Item, Item> HVK_TREASURE_CHEST_BOX =
            ITEMS.register("hvk_treasure_chest_box",
                    () -> new HvkTreasureChestBoxItem(new Item.Properties()));
    public static final DeferredHolder<Item, Item> FOX_MUSIC_PLAYER = ITEMS.register("fox_music_player",
            () -> new com.rzy.dealt_force_skills.item.FoxMusicPlayerItem(new Item.Properties().stacksTo(1).fireResistant()));

    public static final DeferredHolder<Item, Item> ETERNAL_LOVE_BLESSING =
            ITEMS.register("eternal_love_blessing",
                    () -> new EternalLoveBlessingItem(new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<Item, Item> INTERDIMENSIONAL_BLOCK =
            ITEMS.register("interdimensional_block",
                    () -> new InterdimensionalBlockItem(ModBlocks.INTERDIMENSIONAL_BLOCK.get(),
                            new Item.Properties()));
    public static final DeferredHolder<Item, Item> HVK_UNIVERSAL_GLUE = ITEMS.register("hvk_universal_glue",
            () -> new HvkUniversalGlueItem(new Item.Properties().stacksTo(16)));
    public static final DeferredHolder<Item, Item> DRIFTWOOD = ITEMS.register("driftwood",
            () -> new DriftwoodItem(new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<Item, Item> LUCKY_COIN = ITEMS.register("lucky_coin",
            () -> new LuckyCoinItem(new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<Item, Item> HOOK_AUTHOR_BLESSING = ITEMS.register("hook_author_blessing",
            () -> new HookAuthorBlessingItem(new Item.Properties()));

    private static RepairKitItem armorRepairKit(DfsItemQuality quality, int durability, String id) {
        String key = "consumables." + id;
        return new RepairKitItem(durable(DealtForceConfig.intValue(key + ".durability", durability)), quality,
                tooltip(id), EquipmentSlot.CHEST, DealtForceConfig.intValue(key + ".use_ticks", 3 * 20),
                ModSounds.ITEM_ARMOR_REPAIR_START, ModSounds.ITEM_ARMOR_REPAIR_WORK, ModSounds.ITEM_ARMOR_REPAIR_FINISH,
                message("armor_repair_start"), message("armor_repair_work"), message("armor_repair_fail"));
    }

    private static RepairKitItem helmetRepairKit(DfsItemQuality quality, int durability, String id) {
        String key = "consumables." + id;
        return new RepairKitItem(durable(DealtForceConfig.intValue(key + ".durability", durability)), quality,
                tooltip(id), EquipmentSlot.HEAD, DealtForceConfig.intValue(key + ".use_ticks", 3 * 20),
                ModSounds.ITEM_HELMET_REPAIR_START, ModSounds.ITEM_HELMET_REPAIR_WORK, ModSounds.ITEM_HELMET_REPAIR_FINISH,
                message("helmet_repair_start"), message("helmet_repair_work"), message("helmet_repair_fail"));
    }

    private static EffectConsumableItem painReliefItem(Item.Properties properties, String id, int durationTicks) {
        return painReliefItem(properties, id, durationTicks,
                ModSounds.ITEM_PAINKILLER_START, ModSounds.ITEM_PAINKILLER_FINISH);
    }

    private static EffectConsumableItem painReliefItem(Item.Properties properties, String id, int durationTicks,
                                                       Supplier<SoundEvent> startSound,
                                                       Supplier<SoundEvent> finishSound) {
        String key = "consumables." + id;
        List<EffectConsumableItem.EffectEntry> effects = configuredEffects(key,
                List.of(new EffectConsumableItem.EffectEntry(ModEffects.PAIN_RELIEF, durationTicks, 0)));
        return new EffectConsumableItem(properties, DfsItemQuality.WHITE, tooltip(id),
                DealtForceConfig.intValue(key + ".use_ticks", 3 * 20),
                startSound, finishSound,
                message(id + "_start"), message(id + "_finish"),
                effects);
    }

    private static QualityTooltipItem material(String id) {
        return material(id, DfsItemQuality.WHITE);
    }

    private static QualityTooltipItem material(String id, DfsItemQuality quality) {
        return new QualityTooltipItem(new Item.Properties(), quality, tooltip(id));
    }

    private static EffectConsumableItem beverage(String id, List<EffectConsumableItem.EffectEntry> effects) {
        String key = "consumables." + id;
        return new EffectConsumableItem(new Item.Properties().stacksTo(16), DfsItemQuality.RED,
                tooltip(id), DealtForceConfig.intValue(key + ".use_ticks", 3 * 20),
                ModSounds.ITEM_BEVERAGE_START, ModSounds.ITEM_BEVERAGE_FINISH,
                message("beverage_start"), message(id + "_finish"), configuredEffects(key, effects));
    }

    private static EffectConsumableItem injection(String id, int useTicks, List<EffectConsumableItem.EffectEntry> effects) {
        return injection(id, DfsItemQuality.BLUE, useTicks, effects);
    }

    private static EffectConsumableItem injection(String id, DfsItemQuality quality, int useTicks,
                                                  List<EffectConsumableItem.EffectEntry> effects) {
        String key = "consumables." + id;
        return new EffectConsumableItem(new Item.Properties().stacksTo(16), quality,
                tooltip(id), DealtForceConfig.intValue(key + ".use_ticks", useTicks),
                ModSounds.ITEM_INJECTION_START, ModSounds.ITEM_INJECTION_FINISH,
                message("injection_start"), message(id + "_finish"), configuredEffects(key, effects));
    }

    private static HealingMedicineItem medicine(String id, DfsItemQuality quality, int durability, int useTicks,
                                                int durabilityPerSecond, double healPercent, int painReliefExtraCost) {
        String key = "consumables." + id;
        return new HealingMedicineItem(durable(DealtForceConfig.intValue(key + ".durability", durability)),
                quality, tooltip(id), DealtForceConfig.intValue(key + ".use_ticks", useTicks),
                ModSounds.ITEM_MEDICINE_START, ModSounds.ITEM_MEDICINE_WORK, ModSounds.ITEM_MEDICINE_FINISH,
                message(id + "_start"), message(id + "_finish"),
                DealtForceConfig.intValue(key + ".durability_per_second", durabilityPerSecond),
                DealtForceConfig.doubleValue(key + ".heal_percent", healPercent),
                DealtForceConfig.intValue(key + ".pain_relief_extra_cost", painReliefExtraCost));
    }

    private static DfsEquipmentItem armor(String id, DfsItemQuality quality, Faction faction, int durability,
                                          int defense, double toughness, String coverageKey,
                                          double movementLimit, double actionLimit, double knockbackResistance,
                                          double bluntResistance, double kineticAbsorption, double lightweight,
                                          SpecialAbility ability, double lootDuplicateChance) {
        return equipment(id, quality, EquipmentSlot.CHEST, faction, durability, defense, toughness, coverageKey,
                movementLimit, actionLimit, knockbackResistance, bluntResistance, kineticAbsorption, lightweight,
                0, "none", false, false, ability, lootDuplicateChance);
    }

    private static DfsEquipmentItem helmet(String id, DfsItemQuality quality, Faction faction, int durability,
                                           int defense, double toughness, double movementLimit, double actionLimit,
                                           double hearingBoost, String noiseReductionKey,
                                           boolean nightVision, boolean thermalVision,
                                           SpecialAbility ability, double lootDuplicateChance) {
        return equipment(id, quality, EquipmentSlot.HEAD, faction, durability, defense, toughness, "head",
                movementLimit, actionLimit, 0, 0, 0, 0,
                hearingBoost, noiseReductionKey, nightVision, thermalVision, ability, lootDuplicateChance);
    }

    private static DfsEquipmentItem equipment(String id, DfsItemQuality quality, EquipmentSlot slot, Faction faction,
                                              int durability, int defense, double toughness, String coverageKey,
                                              double movementLimit, double actionLimit, double knockbackResistance,
                                              double bluntResistance, double kineticAbsorption, double lightweight,
                                              double hearingBoost, String noiseReductionKey,
                                              boolean nightVision, boolean thermalVision,
                                              SpecialAbility ability, double lootDuplicateChance) {
        String key = "equipment." + id;
        DfsEquipmentItem.Profile profile = new DfsEquipmentItem.Profile(id, quality, slot, faction,
                DealtForceConfig.intValue(key + ".defense", defense),
                DealtForceConfig.doubleValue(key + ".toughness", toughness), coverageKey,
                DealtForceConfig.doubleValue(key + ".movement_limit", movementLimit),
                DealtForceConfig.doubleValue(key + ".action_limit", actionLimit),
                DealtForceConfig.doubleValue(key + ".knockback_resistance", knockbackResistance),
                DealtForceConfig.doubleValue(key + ".blunt_resistance", bluntResistance),
                DealtForceConfig.doubleValue(key + ".kinetic_absorption", kineticAbsorption),
                DealtForceConfig.doubleValue(key + ".lightweight", lightweight),
                DealtForceConfig.doubleValue(key + ".hearing_boost", hearingBoost), noiseReductionKey,
                DealtForceConfig.booleanValue(key + ".night_vision", nightVision),
                DealtForceConfig.booleanValue(key + ".thermal_vision", thermalVision), ability,
                DealtForceConfig.doubleValue(key + ".loot_duplicate_chance", lootDuplicateChance));
        int configuredDurability = DealtForceConfig.intValue(key + ".durability", durability);
        return new DfsEquipmentItem(new Item.Properties().durability(configuredDurability), profile, tooltip(id));
    }

    private static EffectConsumableItem.EffectEntry effect(Supplier<MobEffect> effect, int seconds, int amplifier) {
        return new EffectConsumableItem.EffectEntry(effect, seconds * 20, amplifier);
    }

    private static Supplier<MobEffect> externalEffect(String namespace, String path) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(namespace, path);
        return () -> BuiltInRegistries.MOB_EFFECT.get(id);
    }

    private static List<EffectConsumableItem.EffectEntry> doubled(List<EffectConsumableItem.EffectEntry> effects) {
        List<EffectConsumableItem.EffectEntry> result = new ArrayList<>();
        for (EffectConsumableItem.EffectEntry entry : effects) {
            int doubledLevelAmplifier = Math.max(0, (entry.amplifier() + 1) * 2 - 1);
            result.add(new EffectConsumableItem.EffectEntry(entry.effect(), entry.durationTicks() * 2, doubledLevelAmplifier));
        }
        return result;
    }

    private static List<EffectConsumableItem.EffectEntry> configuredEffects(
            String key,
            List<EffectConsumableItem.EffectEntry> effects
    ) {
        List<EffectConsumableItem.EffectEntry> result = new ArrayList<>();
        for (int i = 0; i < effects.size(); i++) {
            EffectConsumableItem.EffectEntry entry = effects.get(i);
            String effectKey = key + ".effects.effect_" + i;
            result.add(new EffectConsumableItem.EffectEntry(entry.effect(),
                    DealtForceConfig.intValue(effectKey + ".duration_ticks", entry.durationTicks()),
                    DealtForceConfig.intValue(effectKey + ".amplifier", entry.amplifier())));
        }
        return List.copyOf(result);
    }

    private static Item.Properties durable(int durability) {
        return new Item.Properties().durability(durability).setNoRepair();
    }

    private static Item.Properties durableConsumable(String id, int durability) {
        return durable(DealtForceConfig.intValue("consumables." + id + ".durability", durability));
    }

    private static String tooltip(String id) {
        return "tooltip.dealt_force_skills." + id;
    }

    private static String message(String id) {
        return "message.dealt_force_skills.item." + id;
    }

}
