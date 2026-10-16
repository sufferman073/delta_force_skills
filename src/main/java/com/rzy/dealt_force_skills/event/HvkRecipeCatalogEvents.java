package com.rzy.dealt_force_skills.event;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.block.HvkAdvancedDisassemblyBeamEmitterBlockEntity;
import com.rzy.dealt_force_skills.block.HvkAdvancedStandardTemplateConstructorBlockEntity;
import com.rzy.dealt_force_skills.block.WorkBlockQueryCatalog;
import com.rzy.dealt_force_skills.block.InterdimensionalBlockEntity;
import com.rzy.dealt_force_skills.block.HvkAdvancedTreasureCompassBlockEntity;
import com.rzy.dealt_force_skills.block.HvkClonePrototypeBlockEntity;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID)
public final class HvkRecipeCatalogEvents {
    private HvkRecipeCatalogEvents() {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        WorkBlockQueryCatalog.tick(event.getServer());
        HvkAdvancedStandardTemplateConstructorBlockEntity.tickPlanningJobs(event.getServer());
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        warm(event.getServer(), true);
    }



    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        if (event.getPlayer() == null) {
            warm(event.getPlayerList().getServer(), true);
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        com.rzy.dealt_force_skills.compat.HvkJeiRecipeCatalog.clear(event.getServer());
        WorkBlockQueryCatalog.clear();
        HvkAdvancedStandardTemplateConstructorBlockEntity.clearRecipeCatalog(event.getServer());
        HvkAdvancedDisassemblyBeamEmitterBlockEntity.clearRecipeCatalog(event.getServer());
    }

    private static void warm(MinecraftServer server, boolean clearFirst) {
        if (server == null) {
            return;
        }
        if (clearFirst) {
            com.rzy.dealt_force_skills.compat.HvkJeiRecipeCatalog.clear(server);
            WorkBlockQueryCatalog.clear();
            HvkAdvancedStandardTemplateConstructorBlockEntity.clearRecipeCatalog(server);
            HvkAdvancedDisassemblyBeamEmitterBlockEntity.clearRecipeCatalog(server);
        }
        ServerLevel level = server.overworld();
        if (level == null) return;
        HvkAdvancedStandardTemplateConstructorBlockEntity.warmRecipeCatalog(level);
        HvkAdvancedDisassemblyBeamEmitterBlockEntity.warmRecipeCatalog(level);
        InterdimensionalBlockEntity.warmQueryCatalog(level);
        HvkAdvancedTreasureCompassBlockEntity.warmQueryCatalog(level);
        HvkClonePrototypeBlockEntity.warmQueryCatalog(level);
    }
}
