package com.rzy.dealt_force_skills.client.config;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.client.screen.DealtForceConfigScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/**
 * Registers the mod-list Config button for NeoForge 1.21.1.
 */
@EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ModConfigScreens {
    private ModConfigScreens() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            if (com.rzy.dealt_force_skills.compat.SophisticatedBackpackCompat.isLoaded()) {
                com.rzy.dealt_force_skills.compat.SophisticatedBackpackCompat.registerClient();
            }
        });
        event.enqueueWork(() -> ModLoadingContext.get().registerExtensionPoint(
                IConfigScreenFactory.class,
                () -> (minecraft, parent) -> new DealtForceConfigScreen(parent)));
    }
}
