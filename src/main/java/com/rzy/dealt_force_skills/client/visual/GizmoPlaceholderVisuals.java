package com.rzy.dealt_force_skills.client.visual;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.gizmo.GizmoTool;
import com.rzy.dealt_force_skills.client.GizmoInputHandler;
import com.rzy.dealt_force_skills.client.character.ClientGizmoHudState;
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
public final class GizmoPlaceholderVisuals {
    private static final ResourceLocation SMOKE_TRAP_MODEL = model("gizmo_smoke_trap");
    private static final ResourceLocation SPIDER_NEST_MODEL = model("gizmo_spider_nest_trap");
    private static final ResourceLocation T_BOY_MODEL = model("gizmo_t_boy");

    private GizmoPlaceholderVisuals() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderHand(RenderHandEvent event) {
        if (!GizmoInputHandler.shouldRenderPlaceholder()) {
            return;
        }
        event.setCanceled(true);
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        GizmoTool tool = ClientGizmoHudState.equippedTool();
        poseStack.translate(0.38D, -0.16D, -0.58D);
        poseStack.mulPose(Axis.YP.rotationDegrees(-18.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(-18.0F));
        float scale = switch (tool) {
            case SMOKE_TRAP, NONE -> 0.32F;
            case SPIDER_NEST -> 0.54F;
            case T_BOY -> 0.70F;
        };
        poseStack.scale(scale, scale, scale);

        Minecraft minecraft = Minecraft.getInstance();
        float seconds = (minecraft.player.tickCount + com.rzy.dealt_force_skills.client.ClientRenderCompat.partialTick(event.getPartialTick())) / 20.0F;
        BlockbenchAnimatedModelRenderer.render(modelFor(tool), "idle", seconds,
                poseStack, event.getMultiBufferSource(), event.getPackedLight());
        poseStack.popPose();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderArm(RenderArmEvent event) {
        if (GizmoInputHandler.shouldRenderPlaceholder()) {
            event.setCanceled(true);
        }
    }

    private static ResourceLocation modelFor(GizmoTool tool) {
        return switch (tool) {
            case SMOKE_TRAP, NONE -> SMOKE_TRAP_MODEL;
            case SPIDER_NEST -> SPIDER_NEST_MODEL;
            case T_BOY -> T_BOY_MODEL;
        };
    }

    private static ResourceLocation model(String path) {
        return ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, path);
    }
}
