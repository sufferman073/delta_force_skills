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
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderArmEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.Map;
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
        boolean right = player.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT;
        if (visual == SkillModelVisual.MANBA_ELBOW) {
            drawElbow(Minecraft.getInstance().player,right,spec.seconds(),event.getPoseStack(),event.getMultiBufferSource(),event.getPackedLight());
            return;
        }
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        boolean baton = visual == SkillModelVisual.UNDEAD_ROGUE_BATON;
        boolean contact = visual == SkillModelVisual.LEX_HAND_REACH || baton || isLantern(visual) || spec.placement() == Placement.FLASHLIGHT;
        Matrix4f toView = contact ? new Matrix4f(poseStack.last().pose()).invert() : null;
        Vector3f socket = null;
        if (contact) {
            applyDedicatedHandPose(event, poseStack, visual, spec, right);
        } else {
            applyFirstPersonTransform(poseStack, spec.placement(), spec.scale(), spec.seconds());
        }
        if (contact) {
            socket = spec.placement() == Placement.FLASHLIGHT ? new Vector3f(0,-.9F/16,1.3F/16)
                    : baton ? new Vector3f(0, -5F / 16, 0)
                    : animatedSocket(visual, spec, event.getMultiBufferSource(), event.getPackedLight());
            toView.mul(poseStack.last().pose()).transformPosition(socket);
        } else if (spec.placement() == Placement.HAND || spec.placement() == Placement.FULL_SCREEN) {
            float gripY = visual == SkillModelVisual.UNDEAD_WARRIOR_AXE ? -6.5F : 0;
            ClientToolFirstPerson.attachedHand(event,right,0,gripY,0);
            if (visual == SkillModelVisual.UNDEAD_WARRIOR_AXE) ClientToolFirstPerson.attachedHand(event,!right,0,-.5F,0);
        }
        if (spec.placement() == Placement.FLASHLIGHT) {
            ManbaFlashlightBeamRenderer.renderLamp(player, spec.model(), spec.animation(), spec.seconds(),
                    poseStack, event.getMultiBufferSource(), event.getPackedLight(), true);
        } else BlockbenchAnimatedModelRenderer.render(
                spec.model(), baton ? null : spec.animation(), spec.seconds(),
                poseStack, event.getMultiBufferSource(), event.getPackedLight(),
                spec.hiddenGroups(), spec.argb());
        poseStack.popPose();
        var local = Minecraft.getInstance().player;
        if (socket != null && socket.isFinite() && local != null && !local.isInvisible() && !local.isSpectator()) {
            if (baton || spec.placement() == Placement.FLASHLIGHT) {
                ClientToolFirstPerson.fixedArm(event,right,socket,shoulder(right));
                return;
            }
            float along = visual == SkillModelVisual.LEX_HAND_REACH ? 1F : .88F;
            Vector3f toShoulder = new Vector3f(right ? .57F : -.57F, -.49F, -.50F).sub(socket);
            float reach = toShoulder.length() / (2 * ConsumableContact.HALF_LENGTH * along);
            Matrix4f palm = new Matrix4f().translation(socket).rotateTowards(toShoulder, new Vector3f(0, 1, 0)).scale(1, 1, reach);
            ConsumableContact.arm(local, local.getMainArm(), palm, along, poseStack, event.getMultiBufferSource(), event.getPackedLight());
        }
    }

    private static boolean isLantern(SkillModelVisual visual) {
        return visual == SkillModelVisual.UNDEAD_EXPLORER_LANTERN || visual == SkillModelVisual.UNDEAD_EXPLORER_LANTERN_HOLD;
    }

    private static Vector3f shoulder(boolean right) {
        return new Vector3f(right ? .50F : -.50F,-.36F,-.08F);
    }

    private static float batonSweep(float seconds) {
        float phase = Mth.clamp(seconds/.75F,0,1);
        return 8 + 38*ClientToolHandMotion.smooth(phase/.28F)
                - 81*ClientToolHandMotion.smooth((phase-.28F)/.52F) + 12*ClientToolHandMotion.smooth((phase-.80F)/.20F);
    }

    private static void drawElbow(net.minecraft.client.player.AbstractClientPlayer player, boolean right, float seconds,
                                  PoseStack pose, MultiBufferSource buffers, int light) {
        float phase = Mth.clamp(seconds/.75F,0,1), side = right ? 1 : -1;
        float strike = ClientToolHandMotion.smooth(phase/.40F)*(1-ClientToolHandMotion.smooth((phase-.50F)/.50F));
        Vector3f elbow = new Vector3f(side*(.65F-.84F*strike),-.36F+.18F*strike,-.51F-.19F*strike);
        Vector3f fist = new Vector3f(elbow).add(elbowToFist(right,strike));
        ClientToolFirstPerson.foldedArm(player,right,shoulder(right),elbow,fist,pose,buffers,light,.63F);
    }

    static Vector3f elbowToFist(boolean right, float strike) {
        return new Vector3f(-.24F-.06F*strike, .21F+.04F*strike, right ? .16F : -.10F).normalize().mul(.36F);
    }

    private static Vector3f animatedSocket(SkillModelVisual visual, ModelSpec spec, MultiBufferSource buffers, int light) {
        boolean lantern = isLantern(visual);
        String bone = lantern ? "carry_handle" : visual == SkillModelVisual.MANBA_ELBOW ? "arm_rig"
                : visual == SkillModelVisual.LEX_HAND_REACH ? "hand_rig" : "baton_rig";
        float offsetY = lantern ? 4.3F : visual == SkillModelVisual.LEX_HAND_REACH ? -1.6F : 0;
        PoseStack probe = new PoseStack();
        Vector3f socket = new Vector3f(Float.NaN);
        BlockbenchAnimatedModelRenderer.renderWithGroupReplacements(spec.model(), spec.animation(), spec.seconds(),
                probe, buffers, light, lantern ? Set.of("fuel_body", "glass_chamber", "top_vent") : Set.of(),
                Map.of(bone, () -> probe.last().pose().transformPosition(0, offsetY / 16, 0, socket)));
        return socket;
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

    private static void applyDedicatedHandPose(RenderHandEvent event, PoseStack pose, SkillModelVisual visual,
                                               ModelSpec spec, boolean right) {
        int side = right ? 1 : -1;
        if (visual == SkillModelVisual.LEX_HAND_REACH) {
            pose.translate(side * 0.32F, -0.105F, -0.86F);
            pose.mulPose(Axis.XP.rotationDegrees(-8));
            pose.scale(side * 0.87F, 0.87F, 0.87F);
        } else if (visual == SkillModelVisual.UNDEAD_ROGUE_BATON) {
            Vector3f pivot = shoulder(right);
            pose.translate(pivot.x,pivot.y,pivot.z);
            pose.mulPose(Axis.XP.rotationDegrees(batonSweep(spec.seconds())));
            pose.translate(-side*.13F,.04F,-.51F);
            pose.mulPose(Axis.XP.rotationDegrees(-43));
            pose.scale(side * .64F, .64F, .64F);
            pose.translate(0, 5F / 16, 0);
        } else if (isLantern(visual)) {
            pose.translate(side * .33F, .025F, -.94F);
            pose.scale(side * .49F, .49F, .49F);
            pose.translate(0, -14.3F / 16, 0);
        } else {
            float press = visual == SkillModelVisual.MANBA_FLASHLIGHT_TOGGLE
                    ? Mth.sin(Mth.PI * Mth.clamp(spec.seconds() / 0.45F, 0, 1)) : 0;
            pose.translate(side * .31F,-.24F,-.67F);
            pose.mulPose(Axis.YP.rotationDegrees(side * 4));
            pose.mulPose(Axis.XP.rotationDegrees(2 + 3 * press));
            pose.scale(0.80F, 0.80F, 0.80F);

        }
    }

    public static void renderPlayerLayer(net.minecraft.client.model.PlayerModel<?> model,
                                         net.minecraft.client.player.AbstractClientPlayer player, PoseStack pose,
                                         MultiBufferSource buffers, int light, float partialTick) {
        SkillModelVisual visual = ClientSkillModelVisualState.visual(player);
        ModelSpec spec = specFor(visual, player, partialTick);
        if (spec == null || isNikaidouVisual(visual) || player.isInvisible()) return;
        pose.pushPose();
        float size = spec.scale();
        float gripY = 0;
        boolean left = player.getMainArm() == net.minecraft.world.entity.HumanoidArm.LEFT;
        switch (spec.placement()) {
            case HAND, FULL_SCREEN, FLASHLIGHT, CLAW, HAND_REACH_FRONT, HAND_REACH -> {
                var arm = left ? model.leftArm : model.rightArm;
                arm.translateAndRotate(pose);
                pose.translate(left ? .0625 : -.0625, .595, -.035);
                if (isLantern(visual)) {
                    pose.mulPose(new Quaternionf().rotationZYX(arm.zRot, arm.yRot, arm.xRot).conjugate());
                    size = .49F;
                }
                pose.mulPose(Axis.XP.rotationDegrees(180));
                if (visual == SkillModelVisual.MANBA_ELBOW) pose.mulPose(Axis.XP.rotationDegrees(90));
                if (visual == SkillModelVisual.UNDEAD_WARRIOR_AXE) gripY = -6.5F;
                if (visual == SkillModelVisual.UNDEAD_ROGUE_BATON) gripY = -5;
                if (visual == SkillModelVisual.LEX_HAND_REACH) gripY = -3.5F;
            }
            case BACK_GHOST -> { model.body.translateAndRotate(pose); pose.translate(0,.24,.66); pose.mulPose(Axis.ZP.rotationDegrees(180)); }
            case HEAD_TOP, FIBER_COVER -> { model.head.translateAndRotate(pose); pose.translate(0,-.54,0); pose.mulPose(Axis.ZP.rotationDegrees(180)); if (spec.placement() == Placement.FIBER_COVER) size = adaptiveFiberScale(player); }
            default -> { model.body.translateAndRotate(pose); pose.translate(0,.49,spec.placement() == Placement.KNIGHT_RING_SHIELDS ? 0 : -.45); pose.mulPose(Axis.ZP.rotationDegrees(180)); if (spec.placement() == Placement.KNIGHT_RING_SHIELDS) size *= 3.2F; if (spec.placement() == Placement.KNIGHT_IMPACT_SHIELD) size *= 4.8F; }
        }
        pose.scale(size,size,size);
        if (isLantern(visual)) {
            Vector3f handle = animatedSocket(visual, spec, buffers, light);
            if (handle.isFinite()) pose.translate(-handle.x, -handle.y, -handle.z);
        } else {
            pose.translate(0,-gripY/16,visual == SkillModelVisual.MANBA_ELBOW ? -7.5/16 : 0);
        }
        BlockbenchAnimatedModelRenderer.render(spec.model(),spec.animation(),spec.seconds(),pose,buffers,light,spec.hiddenGroups(),spec.argb());
        pose.popPose();
        if (visual == SkillModelVisual.CATDAD_CLAW) {
            pose.pushPose(); model.body.translateAndRotate(pose); pose.translate(0,.24,.66); pose.mulPose(Axis.ZP.rotationDegrees(180)); pose.scale(2,2,2);
            BlockbenchAnimatedModelRenderer.render(model("catdad_hiss_guard_ghost"),"guard_manifest",spec.seconds(),pose,buffers,light,0x88FFFFFF);
            pose.popPose();
        }
    }

    public static boolean replacesHand(Player player) {
        SkillModelVisual visual = ClientSkillModelVisualState.visual(player);
        if (isNikaidouVisual(visual)) return true;
        ModelSpec spec = specFor(visual, player, 0);
        if (spec == null) return false;
        return switch (spec.placement()) {
            case HAND, FULL_SCREEN, FLASHLIGHT, CLAW, HAND_REACH_FRONT, HAND_REACH -> true;
            default -> false;
        };
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderLivingPost(RenderLivingEvent.Post<?, ?> event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof Player || entity.isInvisible()) {
            return;
        }
        if (entity instanceof com.rzy.dealt_force_skills.compat.maid.MaidSkillHost) renderMaidProps(event);
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

    /** Model-independent anchors also work with a maid's custom model. Post starts at world feet. */
    private static void renderMaidProps(RenderLivingEvent.Post<?, ?> event) {
        LivingEntity maid = event.getEntity();
        float partial = com.rzy.dealt_force_skills.client.ClientRenderCompat.partialTick(event.getPartialTick());
        PoseStack poses = event.getPoseStack();
        float bodyYaw = Mth.rotLerp(partial, maid.yBodyRotO, maid.yBodyRot);
        float scale = Mth.clamp(maid.getBbHeight() / 1.8F, .5F, 1.5F);
        if (com.rzy.dealt_force_skills.client.character.ClientSinevaRenderState.isSineva(maid)) {
            poses.pushPose();
            poses.mulPose(Axis.YP.rotationDegrees(180 - bodyYaw));
            poses.scale(-scale, -scale, scale);
            poses.translate(0, -1.501, 0);
            SinevaPlaceholderVisuals.renderOperativeShield(poses, event.getMultiBufferSource(), event.getPackedLight(),
                    com.rzy.dealt_force_skills.client.character.ClientSinevaRenderState.isShieldDeployed(maid),
                    com.rzy.dealt_force_skills.client.character.ClientSinevaRenderState.isViewportBroken(maid),
                    Mth.wrapDegrees(Mth.rotLerp(partial, maid.yHeadRotO, maid.yHeadRot) - bodyYaw));
            poses.popPose();
        }
        SkillModelVisual visual = ClientSkillModelVisualState.visual(maid);
        if (visual == SkillModelVisual.UNDEAD_SCHOLAR_FIBERS) return; // Existing adaptive cover below.
        ModelSpec spec = specFor(visual, maid.getId(), partial);
        if (spec == null) return;
        poses.pushPose();
        poses.mulPose(Axis.YP.rotationDegrees(-bodyYaw));
        boolean back = spec.placement() == Placement.BACK_GHOST;
        boolean ring = spec.placement() == Placement.KNIGHT_RING_SHIELDS;
        boolean head = spec.placement() == Placement.HEAD_TOP;
        poses.translate(back || ring || head ? 0 : -.32 * scale,
                maid.getBbHeight() * (head ? 1.08 : .64), back ? -.4 : ring ? 0 : .42);
        float size = spec.scale() * scale * (ring ? 3.2F : 1);
        poses.scale(size, size, size);
        BlockbenchAnimatedModelRenderer.render(spec.model(), spec.animation(), spec.seconds(), poses,
                event.getMultiBufferSource(), event.getPackedLight(), spec.hiddenGroups(), spec.argb());
        poses.popPose();
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
        if (visual == SkillModelVisual.MANBA_ELBOW) {
            if (minecraft.level != null && minecraft.level.getEntity(targetEntityId) instanceof net.minecraft.client.player.AbstractClientPlayer target)
                drawElbow(target,target.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT,spec.seconds(),poseStack,buffers,0x00F000F0);
        } else {
            applyFirstPersonTransform(poseStack, spec.placement(), spec.scale(), spec.seconds());
            BlockbenchAnimatedModelRenderer.render(spec.model(), spec.animation(), spec.seconds(),
                    poseStack, buffers, 0x00F000F0, spec.hiddenGroups(), spec.argb());
        }
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
