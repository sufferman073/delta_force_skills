package com.rzy.dealt_force_skills.client.visual;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.nikaidou.NikaidouHiroTool;
import com.rzy.dealt_force_skills.client.character.ClientNikaidouHiroHudState;
import com.rzy.dealt_force_skills.client.character.ClientSkillModelVisualState;
import com.rzy.dealt_force_skills.client.renderer.BlockbenchAnimatedModelRenderer;
import com.rzy.dealt_force_skills.skill.SkillModelVisual;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderArmEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT)
public final class NikaidouHiroHeldItemVisuals {
    private NikaidouHiroHeldItemVisuals() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderHand(RenderHandEvent event) {
        Player player = Minecraft.getInstance().player;
        SkillModelVisual visual = ClientSkillModelVisualState.visual(player);
        if (!ClientNikaidouHiroHudState.hasEquippedTool() && !isAttackVisual(visual)) {
            return;
        }
        event.setCanceled(true);
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        NikaidouHiroTool tool = toolFor(visual);
        if (tool == NikaidouHiroTool.NONE) {
            tool = ClientNikaidouHiroHudState.equippedTool();
        }
        float seconds = isAttackVisual(visual)
                ? ClientSkillModelVisualState.animationSeconds(player, com.rzy.dealt_force_skills.client.ClientRenderCompat.partialTick(event.getPartialTick()))
                : 0.0F;
        applyScreenAction(poseStack, visual, seconds);
        if (tool == NikaidouHiroTool.RITUAL_SWORD) {
            poseStack.translate(0.42D, -0.30D, -0.68D);
            poseStack.mulPose(Axis.YP.rotationDegrees(-28.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(-34.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(18.0F));
            poseStack.scale(1.12F, 1.12F, 1.12F);
        } else {
            poseStack.translate(0.18D, -0.26D, -0.66D);
            poseStack.mulPose(Axis.YP.rotationDegrees(-18.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(-22.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(8.0F));
            poseStack.scale(0.74F, 0.74F, 0.74F);
        }

        String animation = animationFor(visual);
        BlockbenchAnimatedModelRenderer.render(
                modelFor(tool), animation, seconds,
                poseStack, event.getMultiBufferSource(), event.getPackedLight());
        poseStack.popPose();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderArm(RenderArmEvent event) {
        if (ClientNikaidouHiroHudState.hasEquippedTool()
                || isAttackVisual(ClientSkillModelVisualState.visual(Minecraft.getInstance().player))) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        Player player = event.getEntity();
        SkillModelVisual visual = ClientSkillModelVisualState.visual(player);
        if (!isAttackVisual(visual)
                || player.isInvisible()
                || player == Minecraft.getInstance().player
                && Minecraft.getInstance().options.getCameraType().isFirstPerson()) {
            return;
        }
        NikaidouHiroTool tool = toolFor(visual);
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        event.getRenderer().getModel().rightArm.translateAndRotate(poseStack);
        poseStack.translate(-0.06D, 0.18D, -0.08D);
        poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(-90.0F));
        float seconds = ClientSkillModelVisualState.animationSeconds(player, com.rzy.dealt_force_skills.client.ClientRenderCompat.partialTick(event.getPartialTick()));
        applyThirdPersonAction(poseStack, visual, seconds);
        float scale = tool == NikaidouHiroTool.RITUAL_SWORD ? 0.56F : 0.46F;
        poseStack.scale(scale, scale, scale);
        BlockbenchAnimatedModelRenderer.render(
                modelFor(tool), animationFor(visual),
                seconds,
                poseStack, event.getMultiBufferSource(), event.getPackedLight());
        poseStack.popPose();
    }

    private static boolean isAttackVisual(SkillModelVisual visual) {
        return visual == SkillModelVisual.NIKAIDOU_HOT_IRON
                || visual == SkillModelVisual.NIKAIDOU_RITUAL_SWORD
                || visual == SkillModelVisual.NIKAIDOU_HOT_IRON_OVERHEAD
                || visual == SkillModelVisual.NIKAIDOU_RITUAL_SWORD_RIGHT_TO_LEFT
                || visual == SkillModelVisual.NIKAIDOU_RITUAL_SWORD_DIAGONAL;
    }

    private static void applyScreenAction(PoseStack poseStack, SkillModelVisual visual, float seconds) {
        if (!isAttackVisual(visual)) {
            return;
        }
        float length = Math.max(0.05F, visual.durationTicks() / 20.0F);
        float progress = Mth.clamp(seconds / length, 0.0F, 1.0F);
        float arc = Mth.sin(progress * Mth.PI);
        if (visual == SkillModelVisual.NIKAIDOU_RITUAL_SWORD) {
            poseStack.translate(Mth.lerp(progress, 1.05F, -1.02F),
                    Mth.lerp(progress, -0.78F, 0.46F) - arc * 0.34F,
                    Mth.lerp(progress, -0.12F, -0.42F));
            poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(progress, -48.0F, 54.0F)));
            poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(progress, -58.0F, 24.0F)));
            poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(progress, 72.0F, -80.0F)));
        } else if (visual == SkillModelVisual.NIKAIDOU_RITUAL_SWORD_RIGHT_TO_LEFT) {
            poseStack.translate(Mth.lerp(progress, -1.00F, 1.02F),
                    Mth.lerp(progress, -0.72F, 0.38F) - arc * 0.30F,
                    Mth.lerp(progress, -0.12F, -0.40F));
            poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(progress, 48.0F, -56.0F)));
            poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(progress, -54.0F, 20.0F)));
            poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(progress, -70.0F, 78.0F)));
        } else if (visual == SkillModelVisual.NIKAIDOU_RITUAL_SWORD_DIAGONAL) {
            poseStack.translate(Mth.lerp(progress, 0.88F, -0.86F),
                    Mth.lerp(progress, -0.98F, 0.58F) - arc * 0.38F,
                    Mth.lerp(progress, -0.16F, -0.46F));
            poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(progress, -38.0F, 44.0F)));
            poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(progress, -72.0F, 30.0F)));
            poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(progress, 42.0F, -96.0F)));
        } else if (visual == SkillModelVisual.NIKAIDOU_HOT_IRON_OVERHEAD) {
            poseStack.translate(Mth.lerp(progress, 0.04F, -0.04F),
                    Mth.lerp(progress, -0.22F, -0.04F) - arc * 0.08F,
                    Mth.lerp(progress, -0.18F, -0.34F));
            poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(progress, -4.0F, 8.0F)));
            poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(progress, -42.0F, 16.0F)));
            poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(progress, 7.0F, -7.0F)));
        } else {
            poseStack.translate(Mth.lerp(progress, 0.10F, -0.10F),
                    Mth.lerp(progress, -0.20F, -0.08F) - arc * 0.08F,
                    Mth.lerp(progress, -0.24F, -0.40F));
            poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(progress, -14.0F, 12.0F)));
            poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(progress, -34.0F, 10.0F)));
            poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(progress, 26.0F, -22.0F)));
        }
    }

    private static void applyThirdPersonAction(PoseStack poseStack, SkillModelVisual visual, float seconds) {
        float length = Math.max(0.05F, visual.durationTicks() / 20.0F);
        float progress = Mth.clamp(seconds / length, 0.0F, 1.0F);
        float arc = Mth.sin(progress * Mth.PI);
        if (visual == SkillModelVisual.NIKAIDOU_RITUAL_SWORD_RIGHT_TO_LEFT) {
            poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(progress, -28.0F, 36.0F)));
            poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(progress, -44.0F, 48.0F)));
        } else if (visual == SkillModelVisual.NIKAIDOU_RITUAL_SWORD_DIAGONAL) {
            poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(progress, -38.0F, 34.0F)));
            poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(progress, 30.0F, -54.0F)));
        } else if (visual == SkillModelVisual.NIKAIDOU_HOT_IRON_OVERHEAD) {
            poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(progress, -62.0F, 28.0F)));
            poseStack.translate(0.0D, -arc * 0.16D, -arc * 0.10D);
        } else {
            poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(progress, 26.0F, -32.0F)));
            poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(progress, 42.0F, -42.0F)));
        }
    }

    private static NikaidouHiroTool toolFor(SkillModelVisual visual) {
        if (visual == null) {
            return NikaidouHiroTool.NONE;
        }
        return switch (visual) {
            case NIKAIDOU_HOT_IRON, NIKAIDOU_HOT_IRON_OVERHEAD -> NikaidouHiroTool.HOT_IRON;
            case NIKAIDOU_RITUAL_SWORD, NIKAIDOU_RITUAL_SWORD_RIGHT_TO_LEFT,
                    NIKAIDOU_RITUAL_SWORD_DIAGONAL -> NikaidouHiroTool.RITUAL_SWORD;
            default -> NikaidouHiroTool.NONE;
        };
    }

    private static ResourceLocation modelFor(NikaidouHiroTool tool) {
        return ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID,
                tool == NikaidouHiroTool.RITUAL_SWORD
                        ? "nikaidou_hiro_ritual_sword"
                        : "nikaidou_hiro_hot_iron");
    }

    private static String animationFor(SkillModelVisual visual) {
        if (visual == SkillModelVisual.NIKAIDOU_HOT_IRON) {
            return "upswing_attack";
        }
        if (visual == SkillModelVisual.NIKAIDOU_HOT_IRON_OVERHEAD) {
            return "overhead_slam";
        }
        if (visual == SkillModelVisual.NIKAIDOU_RITUAL_SWORD) {
            return "slash_left_to_right";
        }
        if (visual == SkillModelVisual.NIKAIDOU_RITUAL_SWORD_RIGHT_TO_LEFT) {
            return "slash_right_to_left";
        }
        if (visual == SkillModelVisual.NIKAIDOU_RITUAL_SWORD_DIAGONAL) {
            return "diagonal_right_high_to_left_low";
        }
        return null;
    }
}
