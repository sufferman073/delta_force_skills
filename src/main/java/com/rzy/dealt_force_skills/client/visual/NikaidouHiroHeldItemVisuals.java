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
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.util.Mth;
import org.joml.Vector3f;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderArmEvent;
import net.minecraftforge.client.event.RenderHandEvent;

import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT)
public final class NikaidouHiroHeldItemVisuals {
    private NikaidouHiroHeldItemVisuals() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderHand(RenderHandEvent event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        SkillModelVisual visual = ClientSkillModelVisualState.visual(player);
        if (!ClientNikaidouHiroHudState.hasEquippedTool() && !isAttackVisual(visual)) {
            return;
        }
        event.setCanceled(true);
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        NikaidouHiroTool tool = toolFor(visual);
        if (tool == NikaidouHiroTool.NONE) {
            tool = ClientNikaidouHiroHudState.equippedTool();
        }
        float seconds = isAttackVisual(visual)
                ? ClientSkillModelVisualState.animationSeconds(player, event.getPartialTick())
                : 0.0F;
        HumanoidArm arm = player.getMainArm();
        int side = arm == HumanoidArm.RIGHT ? 1 : -1;
        boolean sword = tool == NikaidouHiroTool.RITUAL_SWORD;
        float size = sword ? .48F : .37F;
        float phase = isAttackVisual(visual) ? Mth.clamp(seconds/(visual.durationTicks()*.15F),0,1) : 0;
        float swing = Mth.sin(Mth.sqrt(phase)*Mth.PI), swingBack = Mth.sin(phase*phase*Mth.PI);
        boolean reverse = visual == SkillModelVisual.NIKAIDOU_RITUAL_SWORD_RIGHT_TO_LEFT;
        boolean overhead = visual == SkillModelVisual.NIKAIDOU_HOT_IRON_OVERHEAD;
        float sweep = reverse ? -swing : swing;
        Vector3f grip = new Vector3f(side*(.40F-.21F*sweep),-.32F+(overhead ? .20F : .09F)*swing,-.73F-.11F*swing);
        poseStack.pushPose();
        poseStack.translate(grip.x,grip.y,grip.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(side*(12-35*sweep)));
        poseStack.mulPose(Axis.ZP.rotationDegrees(side*(-12+(overhead ? 7 : 56)*sweep)));
        poseStack.mulPose(Axis.XP.rotationDegrees(-29+(overhead ? 49 : 20)*swing-64*swingBack));
        poseStack.scale(side*size,size,size);
        poseStack.translate(0,.8F/16,0);
        BlockbenchAnimatedModelRenderer.render(modelFor(tool),null,0,poseStack,event.getMultiBufferSource(),event.getPackedLight());
        poseStack.popPose();
        ClientToolFirstPerson.fixedArm(event,arm == HumanoidArm.RIGHT,grip,new Vector3f(side*.52F,-.42F,-.08F));
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderArm(RenderArmEvent event) {
        if (ClientNikaidouHiroHudState.hasEquippedTool()
                || isAttackVisual(ClientSkillModelVisualState.visual(Minecraft.getInstance().player))) {
            event.setCanceled(true);
        }
    }

    static HeldToolThirdPersonVisuals.ToolSpec attackSpec(SkillModelVisual visual, float seconds) {
        NikaidouHiroTool tool = toolFor(visual);
        return new HeldToolThirdPersonVisuals.ToolSpec(modelFor(tool), animationFor(visual), seconds,
                tool == NikaidouHiroTool.RITUAL_SWORD ? .78F : .72F, 0, 0, 0);
    }

    private static boolean isAttackVisual(SkillModelVisual visual) {
        return visual == SkillModelVisual.NIKAIDOU_HOT_IRON
                || visual == SkillModelVisual.NIKAIDOU_RITUAL_SWORD
                || visual == SkillModelVisual.NIKAIDOU_HOT_IRON_OVERHEAD
                || visual == SkillModelVisual.NIKAIDOU_RITUAL_SWORD_RIGHT_TO_LEFT
                || visual == SkillModelVisual.NIKAIDOU_RITUAL_SWORD_DIAGONAL;
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
        return new ResourceLocation(DealtForceSkillsMod.MODID,
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
