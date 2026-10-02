package com.rzy.dealt_force_skills.client.visual;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.client.character.ClientUndeadHudState;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT)
public final class UndeadExplorerSpaceVisuals {
    private UndeadExplorerSpaceVisuals() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderLivingPre(RenderLivingEvent.Pre<?, ?> event) {
        if (hidesOtherEntities() && event.getEntity() != Minecraft.getInstance().player) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderNameTag(RenderNameTagEvent event) {
        if (hidesOtherEntities() && event.getEntity() != Minecraft.getInstance().player) {
            event.setCanRender(TriState.FALSE);
        }
    }

    private static boolean hidesOtherEntities() {
        return ClientUndeadHudState.shouldRender()
                && ClientUndeadHudState.explorerSpaceTicks() > 0
                && !ClientUndeadHudState.explorerCanSeeEntities();
    }
}
