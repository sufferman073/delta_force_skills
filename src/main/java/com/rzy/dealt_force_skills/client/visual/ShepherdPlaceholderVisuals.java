package com.rzy.dealt_force_skills.client.visual;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.shepherd.ShepherdTool;
import com.rzy.dealt_force_skills.client.character.ClientShepherdHudState;
import com.rzy.dealt_force_skills.client.renderer.BlockbenchAnimatedModelRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderArmEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT)
public final class ShepherdPlaceholderVisuals {
    private static final ResourceLocation SONIC_TRAP_MODEL = model("shepherd_sonic_trap");
    private static final ResourceLocation GRENADE_MODEL = model("shared_hand_grenade");

    private ShepherdPlaceholderVisuals() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderHand(RenderHandEvent event) {
        boolean grenadeAction = ClientToolReleaseAction.isActive(
                ClientToolReleaseAction.Action.SHEPHERD_GRENADE);
        if (!ClientShepherdHudState.hasEquippedTool() && com.rzy.dealt_force_skills.client.WildfireClient.tool() != 2 && !grenadeAction) {
            return;
        }
        event.setCanceled(true);
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        ShepherdTool tool = grenadeAction || com.rzy.dealt_force_skills.client.WildfireClient.tool() == 2 ? ShepherdTool.FRAG_GRENADE : ClientShepherdHudState.equippedTool();
        if (tool == ShepherdTool.SONIC_TRAP) {
            poseStack.translate(0.38D, -0.18D, -0.58D);
            poseStack.mulPose(Axis.YP.rotationDegrees(-18.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(-18.0F));
            poseStack.scale(0.34F, 0.34F, 0.34F);
        } else {
            poseStack.translate(0.36D, -0.20D, -0.54D);
            poseStack.mulPose(Axis.YP.rotationDegrees(-16.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(-20.0F));
            poseStack.scale(0.82F, 0.82F, 0.82F);
        }

        Minecraft minecraft = Minecraft.getInstance();
        float seconds = (minecraft.player.tickCount + event.getPartialTick()) / 20.0F;
        ClientToolModelAnimationState.AnimationFrame frame = grenadeAction
                ? ClientToolReleaseAction.frame(
                        ClientToolReleaseAction.Action.SHEPHERD_GRENADE, "idle", seconds)
                : new ClientToolModelAnimationState.AnimationFrame("idle", seconds);
        BlockbenchAnimatedModelRenderer.render(modelFor(tool), frame.animation(), frame.seconds(),
                poseStack, event.getMultiBufferSource(), event.getPackedLight());
        poseStack.popPose();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderArm(RenderArmEvent event) {
        if (ClientShepherdHudState.hasEquippedTool() || com.rzy.dealt_force_skills.client.WildfireClient.tool() == 2
                || ClientToolReleaseAction.isActive(ClientToolReleaseAction.Action.SHEPHERD_GRENADE)) {
            event.setCanceled(true);
        }
    }

    private static ResourceLocation modelFor(ShepherdTool tool) {
        return switch (tool) {
            case SONIC_TRAP, NONE -> SONIC_TRAP_MODEL;
            case FRAG_GRENADE -> GRENADE_MODEL;
        };
    }

    private static ResourceLocation model(String path) {
        return new ResourceLocation(DealtForceSkillsMod.MODID, path);
    }
}