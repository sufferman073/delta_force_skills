package com.rzy.dealt_force_skills.client.visual;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.vlinder.VlinderTool;
import com.rzy.dealt_force_skills.client.character.ClientVlinderHudState;
import com.rzy.dealt_force_skills.client.renderer.BlockbenchAnimatedModelRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderArmEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT)
public final class VlinderPlaceholderVisuals {
    private static final ResourceLocation MEDICAL_DRONE_MODEL = model("vlinder_medical_drone");

    private VlinderPlaceholderVisuals() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderHand(RenderHandEvent event) {
        if (!ClientVlinderHudState.hasEquippedTool()) {
            return;
        }
        event.setCanceled(true);
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(0.42D, -0.17D, -0.58D);
        poseStack.mulPose(Axis.YP.rotationDegrees(-16.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(-20.0F));
        poseStack.scale(0.74F, 0.74F, 0.74F);

        Minecraft minecraft = Minecraft.getInstance();
        float seconds = (minecraft.player.tickCount + com.rzy.dealt_force_skills.client.ClientRenderCompat.partialTick(event.getPartialTick())) / 20.0F;
        String animation = ClientVlinderHudState.equippedTool() == VlinderTool.MEDICAL_DRONE ? "idle_hover" : null;
        BlockbenchAnimatedModelRenderer.render(MEDICAL_DRONE_MODEL, animation, seconds,
                poseStack, event.getMultiBufferSource(), event.getPackedLight());
        poseStack.popPose();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderArm(RenderArmEvent event) {
        if (ClientVlinderHudState.hasEquippedTool()) {
            event.setCanceled(true);
        }
    }

    private static ResourceLocation model(String path) {
        return ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, path);
    }
}
