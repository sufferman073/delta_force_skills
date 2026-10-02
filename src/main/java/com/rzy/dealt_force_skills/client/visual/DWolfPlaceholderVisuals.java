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
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
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
    private static boolean savedThirdPersonArm;
    private static float savedRightArmX;
    private static float savedRightArmY;
    private static float savedRightArmZ;

    private DWolfPlaceholderVisuals() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
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

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (event.getEntity() != minecraft.player || !isHandCannonVisible()) {
            return;
        }
        var rightArm = event.getRenderer().getModel().rightArm;
        savedRightArmX = rightArm.xRot;
        savedRightArmY = rightArm.yRot;
        savedRightArmZ = rightArm.zRot;
        savedThirdPersonArm = true;
        ClientToolModelAnimationState.AnimationFrame frame = handCannonFrame(com.rzy.dealt_force_skills.client.ClientRenderCompat.partialTick(event.getPartialTick()));
        if ("fire".equals(frame.animation())) {
            float thrust = Mth.sin(Mth.clamp(frame.seconds() / 0.45F, 0.0F, 1.0F) * Mth.PI);
            rightArm.xRot = -1.45F - thrust * 0.18F;
            rightArm.yRot = -0.10F;
            rightArm.zRot = -0.04F;
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!savedThirdPersonArm || event.getEntity() != minecraft.player) {
            return;
        }
        var rightArm = event.getRenderer().getModel().rightArm;
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        rightArm.translateAndRotate(poseStack);
        poseStack.translate(-0.04D, 0.43D, 0.0D);
        poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
        poseStack.scale(0.55F, 0.55F, 0.55F);
        ClientToolModelAnimationState.AnimationFrame frame = handCannonFrame(com.rzy.dealt_force_skills.client.ClientRenderCompat.partialTick(event.getPartialTick()));
        BlockbenchAnimatedModelRenderer.render(
                HAND_CANNON_MODEL, frame.animation(), frame.seconds(), poseStack,
                event.getMultiBufferSource(), event.getPackedLight());
        poseStack.popPose();
        rightArm.xRot = savedRightArmX;
        rightArm.yRot = savedRightArmY;
        rightArm.zRot = savedRightArmZ;
        savedThirdPersonArm = false;
    }

    private static boolean shouldRenderPlaceholder() {
        return ClientDWolfHudState.hasHandCannonEquipped()
                || DWolfInputHandler.isSmokeHeldForVisual()
                || ClientToolModelAnimationState.isActive(HAND_CANNON_KEY)
                || ClientToolModelAnimationState.isActive(SMOKE_GRENADE_KEY);
    }

    private static boolean isHandCannonVisible() {
        return ClientDWolfHudState.hasHandCannonEquipped()
                || ClientToolModelAnimationState.isActive(HAND_CANNON_KEY);
    }

    private static ClientToolModelAnimationState.AnimationFrame handCannonFrame(float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        float worldSeconds = minecraft.level == null ? 0.0F
                : (minecraft.level.getGameTime() + partialTick) / 20.0F
                * ClientToolModelAnimationState.FAST_PLAYBACK_SPEED;
        return ClientToolModelAnimationState.frame(
                HAND_CANNON_KEY, HAND_CANNON_MODEL, "idle", worldSeconds);
    }

    public static void startHandCannonFire() {
        ClientToolModelAnimationState.start(
                HAND_CANNON_KEY, HAND_CANNON_MODEL, "fire", 0.45F,
                ClientToolModelAnimationState.FAST_PLAYBACK_SPEED);
    }

    public static void startSmokeQuickTrigger() {
        ClientToolModelAnimationState.start(
                SMOKE_GRENADE_KEY, SMOKE_GRENADE_MODEL, "quick_trigger", 0.75F,
                ClientToolModelAnimationState.FAST_PLAYBACK_SPEED);
    }
}
