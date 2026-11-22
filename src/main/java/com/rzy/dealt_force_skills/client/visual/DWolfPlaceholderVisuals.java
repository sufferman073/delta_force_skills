package com.rzy.dealt_force_skills.client.visual;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.client.DWolfInputHandler;
import com.rzy.dealt_force_skills.client.character.ClientDWolfHudState;
import com.rzy.dealt_force_skills.client.renderer.BlockbenchAnimatedModelRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
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
public final class DWolfPlaceholderVisuals {
    public static final ResourceLocation HAND_CANNON_MODEL =
            ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "d_wolf_hand_cannon");
    public static final ResourceLocation SMOKE_GRENADE_MODEL =
            ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "d_wolf_smoke_grenade");
    private static final String HAND_CANNON_KEY = "d_wolf_hand_cannon";
    private static final String SMOKE_GRENADE_KEY = "d_wolf_smoke_grenade";

    private DWolfPlaceholderVisuals() {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRenderHand(RenderHandEvent event) {
        if (!shouldRenderPlaceholder()) {
            return;
        }
        event.setCanceled(true);
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        boolean handCannon = ClientDWolfHudState.hasHandCannonEquipped()
                || ClientToolModelAnimationState.isActive(HAND_CANNON_KEY);
        ResourceLocation model = handCannon ? HAND_CANNON_MODEL : SMOKE_GRENADE_MODEL;
        float worldSeconds = (minecraft.level.getGameTime() + com.rzy.dealt_force_skills.client.ClientRenderCompat.partialTick(event.getPartialTick())) / 20.0F
                * ClientToolModelAnimationState.FAST_PLAYBACK_SPEED;
        ClientToolModelAnimationState.AnimationFrame frame = ClientToolModelAnimationState.frame(
                handCannon ? HAND_CANNON_KEY : SMOKE_GRENADE_KEY,
                model,
                handCannon ? "idle" : null,
                worldSeconds
        );
        float extension = handCannon && "fire".equals(frame.animation())
                ? Mth.sin(Mth.clamp(frame.seconds() / 0.45F, 0.0F, 1.0F) * Mth.PI) * 0.24F
                : 0.0F;

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        if (handCannon) {
            poseStack.translate(0.47D, -0.47D, -0.82D - extension);
            poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(-7.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(-4.0F));
            poseStack.scale(0.70F, 0.70F, 0.70F);
        } else {
            poseStack.translate(0.36D, -0.30D, -0.58D);
            poseStack.mulPose(Axis.YP.rotationDegrees(-16.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(-18.0F));
            poseStack.scale(0.82F, 0.82F, 0.82F);
        }

        boolean rendered = BlockbenchAnimatedModelRenderer.render(
                model,
                frame.animation(),
                frame.seconds(),
                poseStack,
                event.getMultiBufferSource(),
                event.getPackedLight()
        );
        if (!rendered) {
            minecraft.getItemRenderer().renderStatic(
                    minecraft.player,
                    handCannon ? new ItemStack(Items.CROSSBOW) : new ItemStack(Items.GRAY_DYE),
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
        if (shouldRenderPlaceholder()) {
            event.setCanceled(true);
        }
    }



    private static boolean shouldRenderPlaceholder() {
        return ClientDWolfHudState.hasHandCannonEquipped()
                || DWolfInputHandler.isSmokeHeldForVisual()
                || ClientToolModelAnimationState.isActive(HAND_CANNON_KEY)
                || ClientToolModelAnimationState.isActive(SMOKE_GRENADE_KEY);
    }

    public static void startHandCannonFire() {
        ClientToolModelAnimationState.start(
                HAND_CANNON_KEY, HAND_CANNON_MODEL, "fire", 0.45F,
                ClientToolModelAnimationState.FAST_PLAYBACK_SPEED);
    }

    public static void cancelSmokeQuickTrigger() {
        ClientToolModelAnimationState.reset(SMOKE_GRENADE_KEY);
    }

    public static void startSmokeQuickTrigger() {
        ClientToolModelAnimationState.start(
                SMOKE_GRENADE_KEY, SMOKE_GRENADE_MODEL, "quick_trigger", 0.75F,
                ClientToolModelAnimationState.FAST_PLAYBACK_SPEED);
    }
}
