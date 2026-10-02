package com.rzy.dealt_force_skills.client.visual;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.client.renderer.BlockbenchAnimatedModelRenderer;
import com.rzy.dealt_force_skills.entity.TempestRecallAnchorEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT)
public final class TempestRecallVisuals {
    private static final ResourceLocation DEVICE =
            ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "tempest_emergency_recall_device");

    private TempestRecallVisuals() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        Player player = event.getEntity();
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null
                || player.isInvisible()
                || player == minecraft.player && minecraft.options.getCameraType().isFirstPerson()) {
            return;
        }
        TempestRecallAnchorEntity anchor = minecraft.level.getEntitiesOfClass(
                        TempestRecallAnchorEntity.class,
                        player.getBoundingBox().inflate(320.0D),
                        candidate -> candidate.ownerEntityId() == player.getId())
                .stream()
                .findFirst()
                .orElse(null);
        if (anchor == null) {
            return;
        }

        String animation = anchor.isRecalling() ? "recall_winch" : "idle";
        float seconds = (anchor.tickCount + com.rzy.dealt_force_skills.client.ClientRenderCompat.partialTick(event.getPartialTick())) / 20.0F;
        renderDevice(event, -0.24D, false, animation, seconds);
        renderDevice(event, 0.24D, true, animation, seconds);
    }

    private static void renderDevice(
            RenderPlayerEvent.Post event,
            double xOffset,
            boolean mirror,
            String animation,
            float seconds
    ) {
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        event.getRenderer().getModel().body.translateAndRotate(poseStack);
        poseStack.translate(xOffset, 0.56D, 0.10D);
        poseStack.mulPose(Axis.YP.rotationDegrees(mirror ? 90.0F : -90.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
        poseStack.scale(0.22F, 0.22F, 0.22F);
        BlockbenchAnimatedModelRenderer.render(
                DEVICE, animation, seconds, poseStack,
                event.getMultiBufferSource(), event.getPackedLight());
        poseStack.popPose();
    }
}
