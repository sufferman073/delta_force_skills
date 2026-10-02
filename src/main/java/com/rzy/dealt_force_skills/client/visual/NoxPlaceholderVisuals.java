package com.rzy.dealt_force_skills.client.visual;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.nox.NoxTool;
import com.rzy.dealt_force_skills.client.character.ClientNoxHudState;
import com.rzy.dealt_force_skills.client.renderer.BlockbenchAnimatedModelRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderArmEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT)
public final class NoxPlaceholderVisuals {
    private static final ResourceLocation ROTOR_MODEL =
            ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "nox_rotor");
    private static final ResourceLocation FLASH_GRENADE_MODEL =
            ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "nox_flash_grenade");

    private NoxPlaceholderVisuals() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderHand(RenderHandEvent event) {
        boolean flashAction = ClientToolReleaseAction.isActive(
                ClientToolReleaseAction.Action.NOX_FLASH_GRENADE);
        if (!ClientNoxHudState.hasEquippedTool() && !flashAction) {
            return;
        }
        event.setCanceled(true);
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        NoxTool tool = flashAction ? NoxTool.FLASH_GRENADE : ClientNoxHudState.equippedTool();
        if (tool == NoxTool.ROTOR) {
            poseStack.translate(0.42D, -0.16D, -0.62D);
            poseStack.mulPose(Axis.YP.rotationDegrees(-20.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(-22.0F));
            poseStack.scale(0.95F, 0.95F, 0.95F);
        } else {
            poseStack.translate(0.34D, -0.17D, -0.54D);
            poseStack.mulPose(Axis.YP.rotationDegrees(-16.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(-18.0F));
            poseStack.scale(0.82F, 0.82F, 0.82F);
        }

        Minecraft minecraft = Minecraft.getInstance();
        ClientToolModelAnimationState.AnimationFrame frame = flashAction
                ? ClientToolReleaseAction.frame(
                        ClientToolReleaseAction.Action.NOX_FLASH_GRENADE,
                        animationFor(tool),
                        (minecraft.level.getGameTime() + com.rzy.dealt_force_skills.client.ClientRenderCompat.partialTick(event.getPartialTick())) / 20.0F)
                : new ClientToolModelAnimationState.AnimationFrame(
                        animationFor(tool),
                        (minecraft.level.getGameTime() + com.rzy.dealt_force_skills.client.ClientRenderCompat.partialTick(event.getPartialTick())) / 20.0F
                                * ClientToolModelAnimationState.FAST_PLAYBACK_SPEED);
        boolean rendered = tool != NoxTool.NONE && BlockbenchAnimatedModelRenderer.render(
                modelFor(tool),
                frame.animation(),
                frame.seconds(),
                poseStack,
                event.getMultiBufferSource(),
                event.getPackedLight()
        );
        if (!rendered) {
            minecraft.getItemRenderer().renderStatic(
                    minecraft.player,
                    placeholderStack(tool),
                    ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,
                    false,
                    poseStack,
                    event.getMultiBufferSource(),
                    minecraft.level,
                    event.getPackedLight(),
                    OverlayTexture.NO_OVERLAY,
                    0
            );
        }
        poseStack.popPose();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderArm(RenderArmEvent event) {
        if (ClientNoxHudState.hasEquippedTool()
                || ClientToolReleaseAction.isActive(ClientToolReleaseAction.Action.NOX_FLASH_GRENADE)) {
            event.setCanceled(true);
        }
    }

    private static ItemStack placeholderStack(NoxTool tool) {
        return switch (tool) {
            case ROTOR -> new ItemStack(Items.BLAZE_ROD);
            case FLASH_GRENADE -> new ItemStack(Items.GLOWSTONE_DUST);
            case NONE -> ItemStack.EMPTY;
        };
    }

    private static ResourceLocation modelFor(NoxTool tool) {
        return switch (tool) {
            case ROTOR -> ROTOR_MODEL;
            case FLASH_GRENADE, NONE -> FLASH_GRENADE_MODEL;
        };
    }

    private static String animationFor(NoxTool tool) {
        return tool == NoxTool.ROTOR ? "idle" : "idle";
    }
}
