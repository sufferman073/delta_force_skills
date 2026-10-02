package com.rzy.dealt_force_skills.registry;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, DealtForceSkillsMod.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> EQUIPMENT = CREATIVE_TABS.register("equipment",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.dealt_force_skills.equipment"))
                    .icon(() -> new ItemStack(ModItems.TITAN_BALLISTIC_ARMOR.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.NYLON_BODY_ARMOR.get());
                        output.accept(ModItems.LIGHT_BODY_ARMOR.get());
                        output.accept(ModItems.SIMPLE_STAB_VEST.get());
                        output.accept(ModItems.UNIVERSAL_TACTICAL_VEST.get());
                        output.accept(ModItems.STANDARD_BALLISTIC_VEST.get());
                        output.accept(ModItems.HVK_QUICK_RELEASE_BODY_ARMOR.get());
                        output.accept(ModItems.TG_H_BODY_ARMOR.get());
                        output.accept(ModItems.MARKSMAN_TACTICAL_VEST.get());
                        output.accept(ModItems.SAMURAI_BALLISTIC_VEST.get());
                        output.accept(ModItems.HMP_SPECIAL_DUTY_BODY_ARMOR.get());
                        output.accept(ModItems.ASSAULT_BALLISTIC_VEST.get());
                        output.accept(ModItems.MK2_TACTICAL_VEST.get());
                        output.accept(ModItems.DT_AVS_BODY_ARMOR.get());
                        output.accept(ModItems.ELITE_BALLISTIC_VEST.get());
                        output.accept(ModItems.RED_OWL_HEAVY_ASSAULT_VEST.get());
                        output.accept(ModItems.HVK2_BODY_ARMOR.get());
                        output.accept(ModItems.FS_COMPOSITE_BODY_ARMOR.get());
                        output.accept(ModItems.HEAVY_ASSAULT_VEST.get());
                        output.accept(ModItems.HA2_HEAVY_BODY_ARMOR.get());
                        output.accept(ModItems.TRICK_MAS2_ARMOR.get());
                        output.accept(ModItems.KING_KONG_BODY_ARMOR.get());
                        output.accept(ModItems.TITAN_BALLISTIC_ARMOR.get());
                        output.accept(ModItems.BOONIE_HAT.get());
                        output.accept(ModItems.OUTDOOR_BASEBALL_CAP.get());
                        output.accept(ModItems.H01_TACTICAL_HELMET.get());
                        output.accept(ModItems.MC_BALLISTIC_HELMET.get());
                        output.accept(ModItems.DAS_BALLISTIC_HELMET.get());
                        output.accept(ModItems.H07_TACTICAL_HELMET.get());
                        output.accept(ModItems.MC201_BALLISTIC_HELMET.get());
                        output.accept(ModItems.RIOT_HELMET.get());
                        output.accept(ModItems.D6_TACTICAL_HELMET.get());
                        output.accept(ModItems.DICH_TRAINING_HELMET.get());
                        output.accept(ModItems.GT1_TACTICAL_HELMET.get());
                        output.accept(ModItems.MHS_TACTICAL_HELMET.get());
                        output.accept(ModItems.GN_ENDURANCE_HEAVY_NIGHT_VISION_HELMET.get());
                        output.accept(ModItems.MASK1_IRON_WALL_HELMET.get());
                        output.accept(ModItems.DICH1_TACTICAL_HELMET.get());
                        output.accept(ModItems.GN_HEAVY_NIGHT_VISION_HELMET.get());
                        output.accept(ModItems.GN_HEAVY_HELMET.get());
                        output.accept(ModItems.H09_RIOT_HELMET.get());
                        output.accept(ModItems.RED_OWL_ARMORED_MASK.get());
                        output.accept(ModItems.H70_ELITE_HELMET.get());
                        output.accept(ModItems.H70_NIGHT_VISION_ELITE_HELMET.get());
                        output.accept(ModItems.DICH9_HEAVY_HELMET.get());
                        output.accept(ModItems.GT5_COMMANDER_HELMET.get());
                    })
                    .build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CONSUMABLES = CREATIVE_TABS.register("consumables",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.dealt_force_skills.consumables"))
                    .icon(() -> new ItemStack(ModItems.SPECIAL_EQUIPMENT_SUPPLY.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.SELF_MADE_ARMOR_REPAIR_KIT.get());
                        output.accept(ModItems.STANDARD_ARMOR_REPAIR_KIT.get());
                        output.accept(ModItems.PRECISION_ARMOR_REPAIR_KIT.get());
                        output.accept(ModItems.ADVANCED_ARMOR_REPAIR_KIT.get());
                        output.accept(ModItems.SELF_MADE_HELMET_REPAIR_KIT.get());
                        output.accept(ModItems.STANDARD_HELMET_REPAIR_KIT.get());
                        output.accept(ModItems.PRECISION_HELMET_REPAIR_KIT.get());
                        output.accept(ModItems.ADVANCED_HELMET_REPAIR_KIT.get());
                        output.accept(ModItems.ELASTIC_BANDAGE.get());
                        output.accept(ModItems.CAT_TOURNIQUET.get());
                        output.accept(ModItems.SUSTAINED_RELEASE_PAINKILLER.get());
                        output.accept(ModItems.BOTTLED_ANTIBIOTICS.get());
                        output.accept(ModItems.DVE_PAINKILLER.get());
                        output.accept(ModItems.SIMPLE_SURGICAL_PACK.get());
                        output.accept(ModItems.TACTICAL_QUICK_SURGICAL_PACK.get());
                        output.accept(ModItems.DEK_FIELD_SURGICAL_PACK.get());
                        output.accept(ModItems.PROTOTYPE_MADNESS_COMPOUND.get());
                        output.accept(ModItems.SPECIAL_EQUIPMENT_SUPPLY.get());
                        output.accept(ModItems.FOX_MUSIC_PLAYER.get());
            output.accept(ModItems.ETERNAL_LOVE_BLESSING.get());
                        output.accept(ModItems.DRIFTWOOD.get());
               output.accept(ModItems.LUCKY_COIN.get());
               output.accept(ModItems.HOOK_AUTHOR_BLESSING.get());
                        output.accept(ModItems.RAVEN_SHADOW_EXTRACT.get());
                        output.accept(ModItems.DARK_ZONE_RAINBOW_INJECTION.get());
                        output.accept(ModItems.EXPERIMENTAL_IRON_CURTAIN_CATALYST.get());
                        output.accept(ModItems.GUANSHAN_NANYUE.get());
                        output.accept(ModItems.HIGHBALL.get());
                        output.accept(ModItems.HEARTBEAT.get());
                        output.accept(ModItems.STARLIGHT_GLITTER.get());
                        output.accept(ModItems.LONG_GOODBYE.get());
                        output.accept(ModItems.MOUNTAIN_RAIN.get());
                        output.accept(ModItems.SKY_ISLAND.get());
                        output.accept(ModItems.CRANBERRY_SPECIAL.get());
                        output.accept(ModItems.MIST_MORNING.get());
                        output.accept(ModItems.TULIP.get());
                        output.accept(ModItems.WILD_ROSE.get());
                        output.accept(ModItems.ASARA_STYLE.get());
                        output.accept(ModItems.REIS_DANCE.get());
                        output.accept(ModItems.M1_MUSCLE_BOOSTER.get());
                        output.accept(ModItems.M1_MUSCLE_INJECTION.get());
                        output.accept(ModItems.OE2_COMBAT_STIMULANT.get());
                        output.accept(ModItems.STAMINA_ACTIVATION_INJECTION.get());
                        output.accept(ModItems.STAMINA_ENHANCER.get());
                        output.accept(ModItems.NOREPINEPHRINE.get());
                        output.accept(ModItems.PERCEPTION_ACTIVATION_INJECTION.get());
                        output.accept(ModItems.PERCEPTION_ACTIVATOR.get());
                        output.accept(ModItems.CAR_FIRST_AID_KIT.get());
                        output.accept(ModItems.FIELD_FIRST_AID_KIT.get());
                        output.accept(ModItems.SIMPLE_INJECTOR.get());
                        output.accept(ModItems.STRONG_INJECTOR.get());
                        output.accept(ModItems.OUTDOOR_MEDICAL_KIT.get());
                        output.accept(ModItems.BATTLEFIELD_MEDICAL_KIT.get());
                    })
                    .build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MATERIALS = CREATIVE_TABS.register("materials",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.dealt_force_skills.materials"))
                    .icon(() -> new ItemStack(ModItems.BALLISTIC_CERAMIC.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.BALLISTIC_CERAMIC.get());
                        output.accept(ModItems.SPECIAL_STEEL.get());
                        output.accept(ModItems.TITANIUM_ALLOY.get());
                        output.accept(ModItems.ANIMAL_GLAND.get());
                        output.accept(ModItems.RAW_WOOD_PLANK.get());
                        output.accept(ModItems.PH_REGULATOR.get());
                        output.accept(ModItems.GLASS_SYRINGE.get());
                        output.accept(ModItems.ANTIOXIDANT_STABILIZER.get());
                        output.accept(ModItems.STEPMOTHER_EARRING.get());
                        output.accept(ModItems.ELECTRIC_MOTOR.get());
                        output.accept(ModItems.STERILE_DRESSING_PACK.get());
                        output.accept(ModItems.COARSE_SALT.get());
                        output.accept(ModItems.STERILE_WATER.get());
                        output.accept(ModItems.MECHANICAL_WORKER_BEE_MODEL.get());
                        output.accept(ModItems.SLOW_RELEASE_STABILIZER.get());
                        output.accept(ModItems.POLYETHYLENE_FIBER.get());
                        output.accept(ModItems.GLAND_EXTRACT.get());
                        output.accept(ModItems.ARAMID_FIBER.get());
                        output.accept(ModItems.SCREWDRIVER.get());
                        output.accept(ModItems.DANCING_LADY.get());
                        output.accept(ModItems.LENS.get());
                        output.accept(ModItems.BLUEPRINT.get());
                        output.accept(ModItems.HIGH_OUTPUT_CRUSHING_PLIERS.get());
                        output.accept(ModItems.POLYMER_FABRIC.get());
                        output.accept(ModItems.DIGITAL_CALIPER.get());
                        output.accept(ModItems.PREMIUM_COFFEE_BEANS.get());
                        output.accept(ModItems.HVK_BRAIN_UNIT.get());
                        output.accept(ModItems.BEACON_BRAIN_UNIT.get());
                        output.accept(ModItems.BEACON_BOSS_SPAWN_EGG.get());
                        output.accept(ModItems.ACCELERATED_TROOPS_SPAWN_EGG.get());
                        output.accept(ModItems.PORTABLE_ADS_DEVICE.get());
                        output.accept(ModItems.RESPAWN_BEACON.get());
                        output.accept(ModItems.ASARA_FLAMETHROWER_SPAWN_EGG.get());
                        output.accept(ModItems.ASARA_MACHINE_GUNNER_SPAWN_EGG.get());
                        output.accept(ModItems.ASARA_SHIELD_TROOPER_SPAWN_EGG.get());
                        output.accept(ModItems.ASARA_ROCKET_TROOPER_SPAWN_EGG.get());
                        output.accept(ModItems.ASARA_SNIPER_SPAWN_EGG.get());
                        output.accept(ModItems.ASARA_SENTRY_SNIPER_SPAWN_EGG.get());
                        output.accept(ModItems.ASARA_GUARD_SOLDIER_SPAWN_EGG.get());
                        output.accept(ModItems.PROGRAMMABLE_PROCESSOR.get());
                        output.accept(ModItems.HVK_ADVANCED_STANDARD_TEMPLATE_CONSTRUCTOR.get());
                        output.accept(ModItems.HVK_ADVANCED_DISASSEMBLY_BEAM_EMITTER.get());
                        output.accept(ModItems.HVK_ADVANCED_SPAWNER.get());
                        output.accept(ModItems.HVK_ASARA_ARENA.get());
                        output.accept(ModItems.HVK_ENCHANT_INFUSION.get());
                        output.accept(ModItems.HVK_CLONE_PROTOTYPE.get());
                        output.accept(ModItems.HVK_ADVANCED_TREASURE_COMPASS.get());
                        output.accept(ModItems.HVK_TREASURE_CHEST_BOX.get());
                        output.accept(ModItems.HVK_UNIVERSAL_GLUE.get());
                        output.accept(ModItems.INTERDIMENSIONAL_BLOCK.get());
                        output.accept(ModItems.GTI_SPECIAL_OPERATIONS.get());
                        if (com.rzy.dealt_force_skills.compat.SophisticatedBackpackCompat.isLoaded()) {
                            output.accept(ModItems.AUTO_SELL_UPGRADE.get());
                        }
                    })
                    .build());
}
