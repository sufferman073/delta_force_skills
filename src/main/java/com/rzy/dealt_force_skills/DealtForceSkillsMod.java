package com.rzy.dealt_force_skills;

import com.rzy.dealt_force_skills.config.DealtBossesConfig;
import com.rzy.dealt_force_skills.config.DealtBaseConfig;
import com.rzy.dealt_force_skills.config.DealtForceConfig;
import com.rzy.dealt_force_skills.config.DealtForcePlayerConfig;
import com.rzy.dealt_force_skills.config.DealtForceShopConfig;
import com.rzy.dealt_force_skills.config.DealtShopSetConfig;
import com.rzy.dealt_force_skills.config.GamblerArenaConfig;
import com.rzy.dealt_force_skills.config.DealtItemsHavocConfig;
import com.rzy.dealt_force_skills.compat.RarityCoreCompat;
import com.rzy.dealt_force_skills.compat.TamarindLootConfig;
import com.rzy.dealt_force_skills.character.CharacterBranchPackManager;
import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.character.electronics.ElectronicInterferenceManager;
import com.rzy.dealt_force_skills.entity.BeaconBossEntity;
import com.rzy.dealt_force_skills.entity.AsaraTrooperEntity;
import com.rzy.dealt_force_skills.entity.AcceleratedTroopsEntity;
import com.rzy.dealt_force_skills.entity.NoxDecoyEntity;
import com.rzy.dealt_force_skills.entity.SaeedGuardEntity;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.registry.ModBlockEntities;
import com.rzy.dealt_force_skills.registry.ModBlocks;
import com.rzy.dealt_force_skills.registry.ModBrewingRecipes;
import com.rzy.dealt_force_skills.registry.ModCreativeTabs;
import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.registry.ModEntities;
import com.rzy.dealt_force_skills.registry.ModGameRules;
import com.rzy.dealt_force_skills.registry.ModItems;
import com.rzy.dealt_force_skills.registry.ModLootModifiers;
import com.rzy.dealt_force_skills.registry.ModParticles;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.registry.ModRecipeSerializers;
import com.rzy.dealt_force_skills.shop.DfsShopCatalog;
import com.rzy.dealt_force_skills.shop.GhrothArmoryCatalog;
import com.rzy.dealt_force_skills.shop.UndeadShopEntry;
import com.rzy.dealt_force_skills.shop.SpecialOperationsBaseManager;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(DealtForceSkillsMod.MODID)
public class DealtForceSkillsMod {
    public static final String MODID = "dealt_force_skills";

    public DealtForceSkillsMod() {
        this(FMLJavaModLoadingContext.get().getModEventBus());
    }

    public DealtForceSkillsMod(IEventBus modBus) {
        DealtForceConfig.bootstrap();
        DealtBaseConfig.bootstrap();
        DealtBossesConfig.bootstrap();
        DealtForcePlayerConfig.bootstrap();
        DealtForceShopConfig.bootstrap();
        DealtShopSetConfig.bootstrap();
        GamblerArenaConfig.bootstrap();
        DealtItemsHavocConfig.bootstrap();
        TamarindLootConfig.bootstrap();
        CharacterBranchPackManager.bootstrapDefaults();
        CharacterBranchPackManager.reload();

        ModSounds.SOUNDS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModLootModifiers.LOOT_MODIFIER_SERIALIZERS.register(modBus);
        ModCreativeTabs.CREATIVE_TABS.register(modBus);
        ModBlocks.BLOCKS.register(modBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modBus);
        ModEffects.EFFECTS.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        ModParticles.PARTICLES.register(modBus);
        com.rzy.dealt_force_skills.recipe.PotionIngredient.register();
        ModRecipeSerializers.RECIPE_SERIALIZERS.register(modBus);
        ModGameRules.register();

        modBus.addListener(NetworkHandler::onCommonSetup);
        modBus.addListener(this::onCommonSetup);
        modBus.addListener(this::registerEntityAttributes);
        DealtForceConfig.flush();
        DealtBaseConfig.flush();
        DealtBossesConfig.flush();
        DealtForcePlayerConfig.flush();
        DealtForceShopConfig.flush();
        DealtShopSetConfig.flush();
        GamblerArenaConfig.flush();
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            ModBrewingRecipes.register();
            DealtForceConfig.populateGameplayDefaults();
            DealtBossesConfig.populateDefaults();
            ModCharacters.all();
            CharacterBranchPackManager.reload();
            ElectronicInterferenceManager.populateConfigDefaults();
            DfsShopCatalog.entries();
            GhrothArmoryCatalog.entries();
            UndeadShopEntry.values();
            SpecialOperationsBaseManager.populateConfigDefaults();
            com.rzy.dealt_force_skills.compat.SophisticatedBackpackCompat.register();
            RarityCoreCompat.registerRarities();
            DealtForceConfig.finishInitialPopulation();
            DealtBaseConfig.flush();
            DealtBossesConfig.finishInitialPopulation();
            DealtForcePlayerConfig.finishInitialPopulation();
            DealtForceShopConfig.finishInitialPopulation();
            DealtShopSetConfig.flush();
            GamblerArenaConfig.flush();
        });
    }

    private void registerEntityAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.ROVER_WOLF.get(), net.minecraft.world.entity.animal.Wolf.createAttributes().build());
        event.put(ModEntities.NOX_DECOY.get(), NoxDecoyEntity.createAttributes().build());
        event.put(ModEntities.OPERATIVE.get(), com.rzy.dealt_force_skills.entity.OperativeEntity.createAttributes().build());
        event.put(ModEntities.SAEED_GUARD.get(), SaeedGuardEntity.createAttributes().build());
        event.put(ModEntities.OPERATIVE_SAEED_GUARD.get(), SaeedGuardEntity.createAttributes().build());
        event.put(ModEntities.ASARA_FLAMETHROWER.get(), AsaraTrooperEntity.createAttributes().build());
        event.put(ModEntities.ASARA_MACHINE_GUNNER.get(), AsaraTrooperEntity.createAttributes().build());
        event.put(ModEntities.ASARA_SHIELD_TROOPER.get(), AsaraTrooperEntity.createAttributes().build());
        event.put(ModEntities.ASARA_ROCKET_TROOPER.get(), AsaraTrooperEntity.createAttributes().build());
        event.put(ModEntities.ASARA_SNIPER.get(), AsaraTrooperEntity.createAttributes().build());
        event.put(ModEntities.ASARA_SENTRY_SNIPER.get(), AsaraTrooperEntity.createAttributes().build());
        event.put(ModEntities.ASARA_GUARD_SOLDIER.get(), AsaraTrooperEntity.createAttributes().build());
        event.put(ModEntities.ACCELERATED_TROOPS.get(), AcceleratedTroopsEntity.createAttributes().build());
        event.put(ModEntities.BEACON_BOSS.get(), BeaconBossEntity.createAttributes().build());
    }
}