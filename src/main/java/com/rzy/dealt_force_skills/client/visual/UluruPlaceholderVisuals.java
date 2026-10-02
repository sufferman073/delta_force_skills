package com.rzy.dealt_force_skills.client.visual;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.uluru.UluruTool;
import com.rzy.dealt_force_skills.client.UluruMissileController;
import com.rzy.dealt_force_skills.client.character.ClientUluruHudState;
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
public final class UluruPlaceholderVisuals {
    private static final ResourceLocation INCENDIARY_MODEL =
            ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "uluru_incendiary_grenade");
    private static final ResourceLocation COVER_MODEL =
            ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "uluru_quick_cover_package");
    private static final ResourceLocation MISSILE_MODEL =
            ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "uluru_missile_launcher");
    private static UluruTool lastAnimatedTool = UluruTool.NONE;

    private UluruPlaceholderVisuals() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderHand(RenderHandEvent event) {
        if (UluruMissileController.isControlling()) {
            event.setCanceled(true);
            return;
        }
        if (!shouldRenderTool()) {
            return;
        }
        event.setCanceled(true);
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        UluruTool tool = displayedTool();
        applyTransform(poseStack, tool);

        Minecraft minecraft = Minecraft.getInstance();
        ResourceLocation model = modelFor(tool);
        ClientToolModelAnimationState.AnimationFrame frame = ClientToolModelAnimationState.frame(
                keyFor(tool), model, null, 0.0F);
        boolean rendered = BlockbenchAnimatedModelRenderer.render(
                model, frame.animation(), frame.seconds(), poseStack,
                event.getMultiBufferSource(), event.getPackedLight());
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
        if (UluruMissileController.isControlling() || shouldRenderTool()) {
            event.setCanceled(true);
        }
    }

    public static void startUseAnimation(UluruTool tool) {
        if (tool == UluruTool.NONE) {
            return;
        }
        lastAnimatedTool = tool;
        ClientToolModelAnimationState.start(keyFor(tool), modelFor(tool),
                tool == UluruTool.MISSILE ? "fire" : "arm",
                switch (tool) {
                    case INCENDIARY -> 0.6F;
                    case COVER -> 0.8F;
                    case MISSILE -> 0.65F;
                    case NONE -> 0.5F;
                },
                ClientToolModelAnimationState.FAST_PLAYBACK_SPEED);
    }

    public static int useAnimationTicks(UluruTool tool) {
        return switch (tool) {
            case INCENDIARY -> 4;
            case COVER -> 6;
            case MISSILE -> 5;
            case NONE -> 0;
        };
    }

    private static boolean shouldRenderTool() {
        return ClientUluruHudState.hasEquippedTool()
                || lastAnimatedTool != UluruTool.NONE
                && ClientToolModelAnimationState.isActive(keyFor(lastAnimatedTool));
    }

    private static UluruTool displayedTool() {
        return ClientUluruHudState.hasEquippedTool()
                ? ClientUluruHudState.equippedTool()
                : lastAnimatedTool;
    }

    private static void applyTransform(PoseStack poseStack, UluruTool tool) {
        poseStack.translate(0.42D, tool == UluruTool.COVER ? -0.38D : -0.24D,
                tool == UluruTool.MISSILE ? -0.76D : -0.62D);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(tool == UluruTool.COVER ? -18.0F : -10.0F));
        float scale = tool == UluruTool.MISSILE ? 0.55F : 0.75F;
        poseStack.scale(scale, scale, scale);
    }

    private static ResourceLocation modelFor(UluruTool tool) {
        return switch (tool) {
            case INCENDIARY -> INCENDIARY_MODEL;
            case COVER -> COVER_MODEL;
            case MISSILE -> MISSILE_MODEL;
            case NONE -> INCENDIARY_MODEL;
        };
    }

    private static String keyFor(UluruTool tool) {
        return "uluru_" + tool.name().toLowerCase(java.util.Locale.ROOT);
    }

    private static ItemStack placeholderStack(UluruTool tool) {
        return switch (tool) {
            case INCENDIARY -> new ItemStack(Items.FIRE_CHARGE);
            case COVER -> new ItemStack(Items.SCAFFOLDING);
            case MISSILE -> new ItemStack(Items.CROSSBOW);
            case NONE -> ItemStack.EMPTY;
        };
    }
}
