package com.rzy.dealt_force_skills.client.screen;

import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.RecipesUpdatedEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.api.distmarker.Dist;

/** Invalidate screen-local metadata after the server synchronizes a datapack reload. */
@EventBusSubscriber(modid="dealt_force_skills", value=Dist.CLIENT)
public final class ClientRecipeCatalogEvents {
    private ClientRecipeCatalogEvents() { }

    @SubscribeEvent
    public static void recipesUpdated(RecipesUpdatedEvent event) {
        var screen = Minecraft.getInstance().screen;
        if (screen instanceof HvkConstructorScreen constructor) constructor.reloadCatalog();
        if (screen instanceof AdvancedWorkBlockScreen work) work.reloadCatalog();
    }
}
