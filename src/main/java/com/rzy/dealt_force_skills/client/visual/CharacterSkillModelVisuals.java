package com.rzy.dealt_force_skills.client.visual;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.client.character.ClientManbaHudState;
import com.rzy.dealt_force_skills.client.character.ClientSkillModelVisualState;
import com.rzy.dealt_force_skills.client.renderer.BlockbenchAnimatedModelRenderer;
import com.rzy.dealt_force_skills.skill.SkillModelVisual;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderArmEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.Set;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT)
public final class CharacterSkillModelVisuals {
    private static final ResourceLocation MANBA_FLASHLIGHT = model("manba_flashlight");

    private CharacterSkillModelVisuals() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderHand(RenderHandEvent event) {
        Player player = Minecraft.getInstance().player;
        SkillModelVisual visual = ClientSkillModelVisualState.visual(player);
        ModelSpec spec = specFor(visual, player, com.rzy.dealt_force_skills.client.ClientRenderCompat.partialTick(event.getPartialTick()));
        if (spec == null && ClientManbaHudState.flashlightActive()) {
            spec = new ModelSpec(MANBA_FLASHLIGHT, "idle_hold",
                    (player.tickCount + com.rzy.dealt_force_skills.client.ClientRenderCompat.partialTick(event.getPartialTick())) / 20.0F,
                    Placement.FLASHLIGHT, 0.96F, 0xFFFFFFFF, Set.of());
        }
        if (spec == null || isNikaidouVisual(visual) || !rendersInFirstPerson(spec.placement())) {
            return;
        }

        event.setCanceled(true);
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        applyFirstPersonTransform(poseStack, spec.placement(), spec.scale(), spec.seconds());
        BlockbenchAnimatedModelRenderer.render(
                spec.model(), spec.animation(), spec.seconds(),
                poseStack, event.getMultiBufferSource(), event.getPackedLight(),
                spec.hiddenGroups(), spec.argb());
        poseStack.popPose();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderArm(RenderArmEvent event) {
        Player player = Minecraft.getInstance().player;
        SkillModelVisual visual = ClientSkillModelVisualState.visual(player);
        ModelSpec spec = specFor(visual, player, com.rzy.dealt_force_skills.client.ClientRenderCompat.frameTime(Minecraft.getInstance()));
        if ((spec != null && !isNikaidouVisual(visual) && rendersInFirstPerson(spec.placement()))
                || ClientManbaHudState.flashlightActive()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        Player player = event.getEntity();
        Minecraft minecraft = Minecraft.getInstance();
        if (player.isInvisible()
                || player == minecraft.player && minecraft.options.getCameraType().isFirstPerson()) {
            return;
        }

        SkillModelVisual visual = ClientSkillModelVisualState.visual(player);
        ModelSpec spec = specFor(visual, player, com.rzy.dealt_force_skills.client.ClientRenderCompat.partialTick(event.getPartialTick()));
        if (spec == null || isNikaidouVisual(visual)) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        applyThirdPersonTransform(event, poseStack, spec.placement(), spec.scale());
        BlockbenchAnimatedModelRenderer.render(
                spec.model(), spec.animation(), spec.seconds(),
                poseStack, event.getMultiBufferSource(), event.getPackedLight(),
                spec.hiddenGroups(), spec.argb());
        poseStack.popPose();
        if (visual == SkillModelVisual.CATDAD_CLAW) {
            renderCatDadStrikeGhost(event, player);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderLivingPost(RenderLivingEvent.Post<?, ?> event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof Player || entity.isInvisible()) {
            return;
        }
        SkillModelVisual visual = ClientSkillModelVisualState.visual(entity);
        if (visual != SkillModelVisual.UNDEAD_SCHOLAR_FIBERS) {
            return;
        }
        float partialTick = com.rzy.dealt_force_skills.client.ClientRenderCompat.partialTick(event.getPartialTick());
        float ageSeconds = ClientSkillModelVisualState.ageSeconds(entity, partialTick);
        int remainingTicks = ClientSkillModelVisualState.remainingTicks(entity);
        String animation = remainingTicks <= 20 ? "fibers_redden"
                : ageSeconds < 0.55F ? "attach_manifest" : "attached_idle";
        float seconds = remainingTicks <= 20 ? 1.0F - remainingTicks / 20.0F
                : ageSeconds < 0.55F ? ageSeconds : ageSeconds - 0.55F;

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        applyFiberCoverTransform(entity, poseStack, partialTick);
        float scale = adaptiveFiberScale(entity);
        poseStack.scale(scale, scale, scale);
        BlockbenchAnimatedModelRenderer.render(
                model("undead_scholar_stitched_fibers"), animation, seconds,
                poseStack, event.getMultiBufferSource(), event.getPackedLight(),
                Set.of(), 0xA0FFB8B8);
        poseStack.popPose();
    }

    private static ModelSpec specFor(SkillModelVisual visual, Player player, float partialTick) {
        return specFor(visual,
                ClientSkillModelVisualState.animationSeconds(player, partialTick),
                ClientSkillModelVisualState.ageSeconds(player, partialTick),
                ClientSkillModelVisualState.remainingTicks(player));
    }

    private static ModelSpec specFor(SkillModelVisual visual, int entityId, float partialTick) {
        return specFor(visual,
                ClientSkillModelVisualState.animationSeconds(entityId, partialTick),
                ClientSkillModelVisualState.ageSeconds(entityId, partialTick),
                ClientSkillModelVisualState.remainingTicks(entityId));
    }

    private static ModelSpec specFor(SkillModelVisual visual, float seconds, float ageSeconds, int remainingTicks) {
        if (visual == null) {
            return null;
        }
        return switch (visual) {
            case MANBA_FLASHLIGHT_TOGGLE ->
                    spec("manba_flashlight", "toggle_light", seconds, Placement.FLASHLIGHT, 0.96F, 0xFFFFFFFF);
            case MANBA_ELBOW ->
                    spec("manba_elbow", "elbow_strike", seconds, Placement.HAND, 0.76F, 0xFFFFFFFF);
            case NIKAIDOU_HOT_IRON ->
                    spec("nikaidou_hiro_hot_iron", "upswing_attack", seconds, Placement.HAND, 0.82F, 0xFFFFFFFF);
            case NIKAIDOU_RITUAL_SWORD ->
                    spec("nikaidou_hiro_ritual_sword", "slash_left_to_right", seconds, Placement.HAND, 0.92F, 0xFFFFFFFF);
            case NIKAIDOU_HOT_IRON_OVERHEAD ->
                    spec("nikaidou_hiro_hot_iron", "overhead_slam", seconds, Placement.HAND, 0.86F, 0xFFFFFFFF);
            case NIKAIDOU_RITUAL_SWORD_RIGHT_TO_LEFT ->
                    spec("nikaidou_hiro_ritual_sword", "slash_right_to_left", seconds, Placement.HAND, 0.94F, 0xFFFFFFFF);
            case NIKAIDOU_RITUAL_SWORD_DIAGONAL ->
                    spec("nikaidou_hiro_ritual_sword", "diagonal_right_high_to_left_low", seconds, Placement.HAND, 0.94F, 0xFFFFFFFF);
            case CATDAD_GUARD ->
                    spec("catdad_hiss_guard_ghost",
                            ageSeconds < 0.5F ? "guard_manifest" : "guard_hold",
                            ageSeconds < 0.5F ? ageSeconds : ageSeconds - 0.5F,
                            Placement.BACK_GHOST, 2.0F, 0x88FFFFFF);
            case CATDAD_CLAW ->
                    spec("catdad_hiss_attack_claw", "forward_downward_scratch", seconds, Placement.CLAW, 0.78F, 0xAAFFFFFF);
            case UNDEAD_KNIGHT_CHARGE ->
                    spec("undead_knight_cross_shield_effects", "impact_charge", seconds,
                            Placement.KNIGHT_IMPACT_SHIELD, 0.62F, 0xCCFFFFFF,
                            Set.of("parry_barrier", "wall_orbit"));
            case UNDEAD_KNIGHT_BARRIER ->
                    spec("undead_knight_cross_shield_effects", "parry_barrier_deploy", seconds,
                            Placement.KNIGHT_RING_SHIELDS, 0.72F, 0xAAFFFFFF,
                            Set.of("impact_shield", "wall_orbit"));
            case UNDEAD_KNIGHT_WALL ->
                    spec("undead_knight_cross_shield_effects", "wall_shield_orbit", ageSeconds,
                            Placement.KNIGHT_RING_SHIELDS, 0.82F, 0x99FFFFFF,
                            Set.of("impact_shield", "parry_barrier"));
            case UNDEAD_WARRIOR_AXE ->
                    spec("undead_warrior_great_axe", "slam_right_high_to_left_low", seconds, Placement.FULL_SCREEN, 0.86F, 0xFFFFFFFF);
            case UNDEAD_EXPLORER_LANTERN ->
                    spec("undead_explorer_mining_lantern",
                            ageSeconds < 0.75F ? "explore_raise" : "searchlight_idle",
                            ageSeconds < 0.75F ? ageSeconds : ageSeconds - 0.75F,
                            Placement.HAND, 0.76F, 0xFFFFFFFF);
            case UNDEAD_EXPLORER_LANTERN_HOLD ->
                    spec("undead_explorer_mining_lantern", "searchlight_idle",
                            ageSeconds, Placement.HAND, 0.76F, 0xFFFFFFFF);
            case UNDEAD_ROGUE_BATON ->
                    spec("undead_rogue_baton", "overhead_stun_strike", seconds, Placement.HAND, 0.78F, 0xFFFFFFFF);
            case UNDEAD_SCHOLAR_STITCH ->
                    spec("undead_scholar_stitching_set", "stitch_use", seconds, Placement.HAND, 0.76F, 0xFFFFFFFF);
            case UNDEAD_SCHOLAR_FIBERS ->
                    spec("undead_scholar_stitched_fibers",
                            remainingTicks <= 20 ? "fibers_redden"
                                    : ageSeconds < 0.55F ? "attach_manifest" : "attached_idle",
                            remainingTicks <= 20 ? 1.0F - remainingTicks / 20.0F
                                    : ageSeconds < 0.55F ? ageSeconds : ageSeconds - 0.55F,
                            Placement.FIBER_COVER, 0.68F, 0xA0FFB8B8);
            case UNDEAD_SCHOLAR_RITUAL ->
                    spec("undead_scholar_ritual_dance",
                            ageSeconds < 0.6F ? "ritual_start" : "ritual_dance",
                            ageSeconds < 0.6F ? ageSeconds : ageSeconds - 0.6F,
                            Placement.HEAD_TOP, 0.95F, 0xB8FFFFFF);
            case LEX_HAND_REACH ->
                    spec("lex_ninjia_hand", "reach_forward", seconds, Placement.HAND_REACH_FRONT, 0.95F, 0xCCFFFFFF);
            case LEX_HAM_MANIFEST ->
                    spec("lex_ninjia_ham", "manifest", seconds, Placement.BACK_GHOST, 4.0F, 0x66FFFFFF);
            case LEX_HAM_SURGE ->
                    spec("lex_ninjia_ham", "tendril_surge", seconds, Placement.BACK_GHOST, 4.0F, 0x77FFFFFF);
            case LEX_HAM_PRESENCE ->
                    spec("lex_ninjia_ham", "spectral_idle", ageSeconds, Placement.BACK_GHOST, 4.0F, 0x66FFFFFF);
        };
    }

    public static boolean renderSpectatorFirstPerson(GuiGraphics graphics, int targetEntityId, float partialTick) {
        SkillModelVisual visual = ClientSkillModelVisualState.visual(targetEntityId);
        ModelSpec spec = specFor(visual, targetEntityId, partialTick);
        if (spec == null || !rendersInFirstPerson(spec.placement())) {
            return false;
        }

        Minecraft minecraft = Minecraft.getInstance();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();
        poseStack.translate(graphics.guiWidth() / 2.0D + 70.0D, graphics.guiHeight() - 8.0D, 220.0D);
        poseStack.scale(48.0F, -48.0F, 48.0F);
        applyFirstPersonTransform(poseStack, spec.placement(), spec.scale(), spec.seconds());
        BlockbenchAnimatedModelRenderer.render(spec.model(), spec.animation(), spec.seconds(),
                poseStack, buffers, 0x00F000F0, spec.hiddenGroups(), spec.argb());
        buffers.endBatch();
        poseStack.popPose();
        return true;
    }

    private static void applyFirstPersonTransform(
            PoseStack poseStack,
            Placement placement,
            float scale,
            float seconds
    ) {
        if (placement == Placement.BODY || placement == Placement.SHIELD_FRONT
                || placement == Placement.KNIGHT_IMPACT_SHIELD
                || placement == Placement.KNIGHT_RING_SHIELDS) {
            poseStack.translate(0.0D, -0.36D, -1.08D);
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            poseStack.scale(scale, scale, scale);
            return;
        }
        if (placement == Placement.CLAW) {
            poseStack.translate(0.36D, -0.28D, -0.82D);
            poseStack.mulPose(Axis.XP.rotationDegrees(-8.0F));
            poseStack.scale(scale, scale, scale);
            return;
        }
        if (placement == Placement.FULL_SCREEN) {
            float progress = Mth.clamp(seconds / 0.70F, 0.0F, 1.0F);
            float arc = Mth.sin(progress * Mth.PI);
            poseStack.translate(Mth.lerp(progress, 0.68F, -0.48F),
                    Mth.lerp(progress, -0.58F, 0.22F) - arc * 0.22F, -0.82D);
            poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(progress, -38.0F, 34.0F)));
            poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(progress, -48.0F, 18.0F)));
            poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(progress, 42.0F, -54.0F)));
            poseStack.scale(scale, scale, scale);
            return;
        }
        if (placement == Placement.HAND_REACH_FRONT) {
            poseStack.translate(0.34D, -0.14D, -0.72D);
            poseStack.mulPose(Axis.XP.rotationDegrees(-8.0F));
            poseStack.scale(scale, scale, scale);
            return;
        }
        if (placement == Placement.FLASHLIGHT) {
            poseStack.translate(0.58D, -0.58D, -0.96D);
            poseStack.mulPose(Axis.YP.rotationDegrees(28.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(12.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(-8.0F));
            poseStack.scale(scale, scale, scale);
            return;
        }
        poseStack.translate(0.43D, -0.24D, -0.64D);
        poseStack.mulPose(Axis.YP.rotationDegrees(-24.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(-24.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(10.0F));
        poseStack.scale(scale, scale, scale);
    }

    private static void applyThirdPersonTransform(
            RenderPlayerEvent.Post event,
            PoseStack poseStack,
            Placement placement,
            float scale
    ) {
        float finalScale = scale;
        if (placement == Placement.HAND
                || placement == Placement.FULL_SCREEN) {
            applyRightHandAnchor(event, poseStack);
        } else if (placement == Placement.FLASHLIGHT) {
            applyRightHandAnchor(event, poseStack);
            poseStack.translate(0.00D, 0.36D, -0.04D);
        } else if (placement == Placement.HAND_REACH_FRONT) {
            event.getRenderer().getModel().body.translateAndRotate(poseStack);
            poseStack.translate(0.0D, 0.72D, -0.92D);
            poseStack.mulPose(Axis.XP.rotationDegrees(-8.0F));
        } else if (placement == Placement.CLAW) {
            event.getRenderer().getModel().body.translateAndRotate(poseStack);
            poseStack.translate(-0.34D, 0.48D, -0.34D);
        } else if (placement == Placement.HEAD_TOP) {
            event.getRenderer().getModel().head.translateAndRotate(poseStack);
            poseStack.translate(0.0D, 1.82D, 0.0D);
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        } else if (placement == Placement.FIBER_COVER) {
            applyPlayerFiberCoverTransform(event, poseStack);
            finalScale = adaptiveFiberScale(event.getEntity());
        } else if (placement == Placement.KNIGHT_IMPACT_SHIELD) {
            applyKnightImpactShieldTransform(event, poseStack);
            finalScale = scale * 4.8F;
        } else if (placement == Placement.KNIGHT_RING_SHIELDS) {
            event.getRenderer().getModel().body.translateAndRotate(poseStack);
            poseStack.translate(0.0D, 0.54D, 0.0D);
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            finalScale = scale * 3.2F;
        } else if (placement == Placement.BACK_GHOST) {
            applyBackGhostTransform(event, poseStack);
        } else {
            event.getRenderer().getModel().body.translateAndRotate(poseStack);
            double z = placement == Placement.SHIELD_FRONT ? -0.52D : 0.02D;
            poseStack.translate(0.0D, 0.54D, z);
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        }
        poseStack.scale(finalScale, finalScale, finalScale);
    }

    private static void applyRightHandAnchor(RenderPlayerEvent.Post event, PoseStack poseStack) {
        event.getRenderer().getModel().rightArm.translateAndRotate(poseStack);
        poseStack.translate(-0.06D, 0.18D, -0.08D);
        poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(-90.0F));
    }

    private static void applyBackGhostTransform(RenderPlayerEvent.Post event, PoseStack poseStack) {
        applyBodyYawBackTransform(event, poseStack, 0.24D, 0.96D);
    }

    private static void applyKnightImpactShieldTransform(RenderPlayerEvent.Post event, PoseStack poseStack) {
        Player player = event.getEntity();
        float headYaw = Mth.rotLerp(com.rzy.dealt_force_skills.client.ClientRenderCompat.partialTick(event.getPartialTick()), player.yHeadRotO, player.yHeadRot);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - headYaw));
        event.getRenderer().getModel().body.translateAndRotate(poseStack);
        poseStack.translate(0.0D, 0.44D, 0.42D);
    }

    private static void applyBodyYawBackTransform(RenderPlayerEvent.Post event, PoseStack poseStack, double y, double z) {
        Player player = event.getEntity();
        float bodyYaw = Mth.rotLerp(com.rzy.dealt_force_skills.client.ClientRenderCompat.partialTick(event.getPartialTick()), player.yBodyRotO, player.yBodyRot);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw));
        event.getRenderer().getModel().body.translateAndRotate(poseStack);
        poseStack.translate(0.0D, y, z);
    }

    private static void applyPlayerFiberCoverTransform(RenderPlayerEvent.Post event, PoseStack poseStack) {
        event.getRenderer().getModel().head.translateAndRotate(poseStack);
        poseStack.translate(0.0D, 0.03D, -0.54D);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
    }

    private static void applyFiberCoverTransform(LivingEntity entity, PoseStack poseStack, float partialTick) {
        poseStack.translate(0.0D, entity.getBbHeight() * 0.86D, 0.0D);
        float headYaw = Mth.rotLerp(partialTick, entity.yHeadRotO, entity.yHeadRot);
        poseStack.mulPose(Axis.YP.rotationDegrees(-headYaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(partialTick, entity.xRotO, entity.getXRot())));
        poseStack.translate(0.0D, -entity.getBbHeight() * 0.04D,
                Math.max(0.46D, entity.getBbWidth() * 0.78D));
    }

    private static boolean isNikaidouVisual(SkillModelVisual visual) {
        return visual == SkillModelVisual.NIKAIDOU_HOT_IRON
                || visual == SkillModelVisual.NIKAIDOU_RITUAL_SWORD
                || visual == SkillModelVisual.NIKAIDOU_HOT_IRON_OVERHEAD
                || visual == SkillModelVisual.NIKAIDOU_RITUAL_SWORD_RIGHT_TO_LEFT
                || visual == SkillModelVisual.NIKAIDOU_RITUAL_SWORD_DIAGONAL;
    }

    private static boolean rendersInFirstPerson(Placement placement) {
        return placement != Placement.BACK_GHOST
                && placement != Placement.HEAD_TOP
                && placement != Placement.FIBER_COVER;
    }

    private static void renderCatDadStrikeGhost(RenderPlayerEvent.Post event, Player player) {
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        applyBackGhostTransform(event, poseStack);
        poseStack.scale(2.0F, 2.0F, 2.0F);
        BlockbenchAnimatedModelRenderer.render(
                model("catdad_hiss_guard_ghost"), "guard_manifest",
                ClientSkillModelVisualState.animationSeconds(player, com.rzy.dealt_force_skills.client.ClientRenderCompat.partialTick(event.getPartialTick())),
                poseStack, event.getMultiBufferSource(), event.getPackedLight(), 0x88FFFFFF);
        poseStack.popPose();
    }

    private static ModelSpec spec(
            String model,
            String animation,
            float seconds,
            Placement placement,
            float scale,
            int argb
    ) {
        return spec(model, animation, seconds, placement, scale, argb, Set.of());
    }

    private static ModelSpec spec(
            String model,
            String animation,
            float seconds,
            Placement placement,
            float scale,
            int argb,
            Set<String> hiddenGroups
    ) {
        return new ModelSpec(model(model), animation, seconds, placement, scale, argb, hiddenGroups);
    }

    private static ResourceLocation model(String path) {
        return ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, path);
    }

    private static float adaptiveFiberScale(LivingEntity entity) {
        float widthScale = entity.getBbWidth() * 2.4F;
        float heightScale = entity.getBbHeight() / 1.25F;
        return Mth.clamp(Math.max(1.35F, Math.max(widthScale, heightScale)), 1.35F, 5.0F);
    }

    private enum Placement {
        HAND,
        BODY,
        BACK_GHOST,
        CLAW,
        SHIELD_FRONT,
        KNIGHT_IMPACT_SHIELD,
        KNIGHT_RING_SHIELDS,
        FULL_SCREEN,
        HEAD_TOP,
        HAND_REACH,
        HAND_REACH_FRONT,
        FIBER_COVER,
        FLASHLIGHT
    }

    private record ModelSpec(
            ResourceLocation model,
            String animation,
            float seconds,
            Placement placement,
            float scale,
            int argb,
            Set<String> hiddenGroups
    ) {
    }
}
