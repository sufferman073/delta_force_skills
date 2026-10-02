package com.rzy.dealt_force_skills.client.visual;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.client.renderer.BlockbenchAnimatedModelRenderer;
import com.rzy.dealt_force_skills.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.Map;
import java.util.Set;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT)
public final class OE2CombatStimulantVisuals {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(
            DealtForceSkillsMod.MODID, "oe2_combat_stimulant");
    private static final Set<String> HIDDEN_FIRST_PERSON_GROUPS = Set.of("thirdperson_hand", "camera");
    private static final Set<String> HIDDEN_THIRD_PERSON_GROUPS = Set.of(
            "lefthand", "righthand", "thirdperson_hand", "camera");
    private static final double IDLE_VIEW_X = -2.75D;
    private static final double IDLE_VIEW_Y = 14.2875D;
    private static final double IDLE_VIEW_Z = 14.0D;

    private OE2CombatStimulantVisuals() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderHand(RenderHandEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        InteractionHand modelHand = player == null ? null : displayedStimulantHand(player);
        if (player == null || modelHand == null) {
            return;
        }
        event.setCanceled(true);
        if (event.getHand() == modelHand) {
            renderFirstPerson(player, modelHand, event.getPartialTick(), event.getPoseStack(),
                    event.getMultiBufferSource(), event.getPackedLight());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        Player player = event.getEntity();
        Minecraft minecraft = Minecraft.getInstance();
        if (player.isInvisible() || player == minecraft.player
                && minecraft.options.getCameraType().isFirstPerson()) {
            return;
        }
        renderThirdPersonHand(event, player, InteractionHand.MAIN_HAND);
        renderThirdPersonHand(event, player, InteractionHand.OFF_HAND);
    }

    private static void renderThirdPersonHand(RenderPlayerEvent.Post event, Player player, InteractionHand hand) {
        if (!isStimulant(player.getItemInHand(hand))) {
            return;
        }
        boolean rightArm = isRightArm(player, hand);
        float side = rightArm ? 1.0F : -1.0F;
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        if (rightArm) {
            event.getRenderer().getModel().rightArm.translateAndRotate(poseStack);
        } else {
            event.getRenderer().getModel().leftArm.translateAndRotate(poseStack);
        }
        poseStack.translate(-0.06D * side, 0.18D, -0.08D);
        poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(-90.0F * side));
        poseStack.scale(0.46F, 0.46F, 0.46F);
        renderModel(player, hand, event.getPartialTick(), poseStack, event.getMultiBufferSource(),
                event.getPackedLight(), HIDDEN_THIRD_PERSON_GROUPS);
        poseStack.popPose();
    }

    private static void renderModel(Player player, InteractionHand hand, float partialTick, PoseStack poseStack,
                                    net.minecraft.client.renderer.MultiBufferSource buffer,
                                    int packedLight, Set<String> hiddenGroups) {
        boolean using = player.isUsingItem() && player.getUsedItemHand() == hand
                && isStimulant(player.getUseItem());
        float seconds = using
                ? Mth.clamp((player.getTicksUsingItem() + partialTick) / 20.0F, 0.0F, 1.5F)
                : 0.0F;
        BlockbenchAnimatedModelRenderer.render(MODEL, using ? "use" : "idle", seconds,
                poseStack, buffer, packedLight, hiddenGroups);
    }

    private static boolean isStimulant(ItemStack stack) {
        return stack != null && stack.is(ModItems.OE2_COMBAT_STIMULANT.get());
    }

    private static boolean isRightArm(Player player, InteractionHand hand) {
        return (hand == InteractionHand.MAIN_HAND)
                == (player.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT);
    }

    private static InteractionHand displayedStimulantHand(LocalPlayer player) {
        if (player.isUsingItem() && isStimulant(player.getUseItem())) {
            return player.getUsedItemHand();
        }
        if (isStimulant(player.getMainHandItem())) {
            return InteractionHand.MAIN_HAND;
        }
        return isStimulant(player.getOffhandItem()) ? InteractionHand.OFF_HAND : null;
    }

    private static void renderFirstPerson(
            LocalPlayer player,
            InteractionHand hand,
            float partialTick,
            PoseStack poseStack,
            net.minecraft.client.renderer.MultiBufferSource buffer,
            int packedLight
    ) {
        boolean using = player.isUsingItem() && player.getUsedItemHand() == hand
                && isStimulant(player.getUseItem());
        float seconds = using
                ? Mth.clamp((player.getTicksUsingItem() + partialTick) / 20.0F, 0.0F, 1.5F)
                : 0.0F;

        poseStack.pushPose();
        poseStack.translate(-IDLE_VIEW_X / 16.0D, -IDLE_VIEW_Y / 16.0D, -IDLE_VIEW_Z / 16.0D);
        Map<String, Runnable> armReplacements = Map.of(
                "lefthand_pos", () -> renderAnimatedArm(
                        player, HumanoidArm.LEFT, poseStack, buffer, packedLight),
                "righthand_pos", () -> renderAnimatedArm(
                        player, HumanoidArm.RIGHT, poseStack, buffer, packedLight)
        );
        BlockbenchAnimatedModelRenderer.renderWithGroupReplacements(
                MODEL, using ? "use" : "idle", seconds, poseStack, buffer, packedLight,
                HIDDEN_FIRST_PERSON_GROUPS, armReplacements);
        poseStack.popPose();
    }

    private static void renderAnimatedArm(
            LocalPlayer player,
            HumanoidArm arm,
            PoseStack poseStack,
            net.minecraft.client.renderer.MultiBufferSource buffer,
            int packedLight
    ) {
        poseStack.pushPose();
        if (arm == HumanoidArm.RIGHT) {
            poseStack.translate(-8.0D / 16.0D, -8.0D / 16.0D, 10.0D / 16.0D);
            poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
            poseStack.translate(9.0D / 16.0D, 6.5D / 16.0D, 8.0D / 16.0D);
            CharacterSkinOverrideVisuals.renderRightHand(poseStack, buffer, packedLight, player);
        } else {
            poseStack.translate(-8.25D / 16.0D, -7.1875D / 16.0D, 9.375D / 16.0D);
            poseStack.mulPose(Axis.ZP.rotationDegrees(-9.97296F));
            poseStack.mulPose(Axis.YP.rotationDegrees(6.22373F));
            poseStack.mulPose(Axis.XP.rotationDegrees(-83.47775F));
            poseStack.translate(8.0D / 16.0D, 6.5D / 16.0D, 8.0D / 16.0D);
            CharacterSkinOverrideVisuals.renderLeftHand(poseStack, buffer, packedLight, player);
        }
        poseStack.popPose();
    }
}