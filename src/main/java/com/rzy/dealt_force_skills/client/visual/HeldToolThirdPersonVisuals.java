package com.rzy.dealt_force_skills.client.visual;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.chamber.ChamberTool;
import com.rzy.dealt_force_skills.character.gizmo.GizmoTool;
import com.rzy.dealt_force_skills.character.hackclaw.HackclawTool;
import com.rzy.dealt_force_skills.character.luna.LunaTool;
import com.rzy.dealt_force_skills.character.morse.MorseTool;
import com.rzy.dealt_force_skills.character.nikaidou.NikaidouHiroTool;
import com.rzy.dealt_force_skills.character.nox.NoxTool;
import com.rzy.dealt_force_skills.character.ntwo.NTwoTool;
import com.rzy.dealt_force_skills.character.raptor.RaptorTool;
import com.rzy.dealt_force_skills.character.shepherd.ShepherdTool;
import com.rzy.dealt_force_skills.character.stinger.StingerTool;
import com.rzy.dealt_force_skills.character.toxik.ToxikTool;
import com.rzy.dealt_force_skills.character.uluru.UluruTool;
import com.rzy.dealt_force_skills.character.vyron.VyronTool;
import com.rzy.dealt_force_skills.client.DWolfInputHandler;
import com.rzy.dealt_force_skills.client.SinevaInputHandler;
import com.rzy.dealt_force_skills.client.character.ClientDWolfHudState;
import com.rzy.dealt_force_skills.client.character.ClientLunaBowVisualState;
import com.rzy.dealt_force_skills.client.character.ClientChamberHudState;
import com.rzy.dealt_force_skills.client.character.ClientDepartmentHudState;
import com.rzy.dealt_force_skills.client.character.ClientGizmoHudState;
import com.rzy.dealt_force_skills.client.character.ClientHackclawHudState;
import com.rzy.dealt_force_skills.client.character.ClientHeldToolVisualState;
import com.rzy.dealt_force_skills.client.character.ClientLunaHudState;
import com.rzy.dealt_force_skills.client.character.ClientManbaHudState;
import com.rzy.dealt_force_skills.client.character.ClientMorseHudState;
import com.rzy.dealt_force_skills.client.character.ClientNikaidouHiroHudState;
import com.rzy.dealt_force_skills.client.character.ClientNoxHudState;
import com.rzy.dealt_force_skills.client.character.ClientNTwoHudState;
import com.rzy.dealt_force_skills.client.character.ClientRaptorHudState;
import com.rzy.dealt_force_skills.client.character.ClientShepherdHudState;
import com.rzy.dealt_force_skills.client.character.ClientSkillModelVisualState;
import com.rzy.dealt_force_skills.client.character.ClientStingerHudState;
import com.rzy.dealt_force_skills.client.character.ClientTempestHudState;
import com.rzy.dealt_force_skills.client.character.ClientToxikHudState;
import com.rzy.dealt_force_skills.client.character.ClientUluruHudState;
import com.rzy.dealt_force_skills.client.character.ClientVlinderHudState;
import com.rzy.dealt_force_skills.client.character.ClientVyronHudState;
import com.rzy.dealt_force_skills.client.renderer.BlockbenchAnimatedModelRenderer;
import com.rzy.dealt_force_skills.skill.HeldToolVisual;
import com.rzy.dealt_force_skills.skill.SkillModelVisual;
import com.rzy.dealt_force_skills.skill.ToolHandCue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import org.joml.Vector3f;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class HeldToolThirdPersonVisuals extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private HeldToolThirdPersonVisuals(PlayerRenderer renderer) {
        super(renderer);
    }

    @SubscribeEvent
    public static void addLayers(EntityRenderersEvent.AddLayers event) {
        for (var skin : event.getSkins()) {
            PlayerRenderer renderer = event.getSkin(skin);
            if (renderer != null) renderer.addLayer(new HeldToolThirdPersonVisuals(renderer));
        }
    }

    public static boolean renderSpectatorFirstPerson(GuiGraphics graphics, int targetEntityId, float partialTick) {
        ToolSpec spec = wildfireSpec(targetEntityId, partialTick);
        if (spec == null) spec = visualSpec(ClientHeldToolVisualState.visual(targetEntityId), partialTick);
        if (spec == null) {
            return false;
        }
        Minecraft minecraft = Minecraft.getInstance();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();
        poseStack.translate(graphics.guiWidth() / 2.0D + 70.0D, graphics.guiHeight() - 8.0D, 210.0D);
        poseStack.scale(58.0F, -58.0F, 58.0F);
        poseStack.translate(0.43D, -0.24D, -0.64D);
        poseStack.mulPose(Axis.YP.rotationDegrees(-24.0F + spec.yaw()));
        poseStack.mulPose(Axis.XP.rotationDegrees(-24.0F + spec.pitch()));
        poseStack.mulPose(Axis.ZP.rotationDegrees(10.0F + spec.roll()));
        poseStack.scale(spec.scale(), spec.scale(), spec.scale());
        BlockbenchAnimatedModelRenderer.render(spec.model(), spec.animation(), spec.seconds(),
                poseStack, buffers, 0x00F000F0);
        buffers.endBatch();
        poseStack.popPose();
        return true;
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffers, int packedLight, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        if (!visible(player)) return;
        TempestRecallVisuals.render(getParentModel(), player, partialTick, poseStack, buffers, packedLight);
        SkillModelVisual action = ClientSkillModelVisualState.visual(player);
        if (action == SkillModelVisual.MANBA_ELBOW) {
            var model = getParentModel();
            HumanoidArm arm = player.getMainArm();
            boolean right = arm == HumanoidArm.RIGHT;
            var shoulder = ThirdPersonToolGrip.pivot(model, arm).div(16);
            var handFrame = new PoseStack();
            model.translateToHand(arm, handFrame);
            var elbow = handFrame.last().pose().transformPosition(0,.39F,0,new Vector3f());
            float phase = ClientSkillModelVisualState.ageSeconds(player, partialTick)*20 / action.durationTicks();
            var forearm = CharacterSkillModelVisuals.elbowToFist(right, ThirdPersonSkillPose.smooth(phase/.4F)).mul(-1,-1,1);
            ClientToolFirstPerson.foldedArm(player,right,shoulder,elbow,new Vector3f(elbow).add(forearm),poseStack,buffers,packedLight,1);
            return;
        }
        if (!ThirdPersonSkillPose.owns(action))
            CharacterSkillModelVisuals.renderPlayerLayer(getParentModel(), player, poseStack, buffers, packedLight, partialTick);
        var motion = ClientToolHandMotion.frame(player, partialTick);
        ToolSpec spec = forPlayer(player, partialTick, motion);
        if (spec == null || !ThirdPersonSkillPose.owns(action) && motion != null && motion.hidden()) return;
        if (spec.model().getPath().equals("n_two_condensation_focus")) {
            var local = new PoseStack();
            ThirdPersonToolGrip.place(spec, getParentModel(), player.getMainArm(), local);
            var sideGrip = local.last().pose().transformPosition(ThirdPersonToolGrip.launcherSideGrip().div(16));
            var support = player.getMainArm().getOpposite();
            var shoulder = ThirdPersonToolGrip.pivot(getParentModel(), support).div(16);
            ClientToolFirstPerson.foldedArm(player,support == HumanoidArm.RIGHT,shoulder,launcherSupportElbow(shoulder,sideGrip),sideGrip,poseStack,buffers,packedLight,1);
        }
        poseStack.pushPose();
        try {
            if (!ThirdPersonToolGrip.place(spec, getParentModel(), player.getMainArm(), poseStack))
                spec.applyGrip(getParentModel(), player.getMainArm(), poseStack);
            if (spec.model().getPath().equals("hackclaw_hacking_terminal")) {
                HackclawTerminalVisuals.renderTerminal(player, partialTick, poseStack, buffers, packedLight);
            } else if (spec.model().getPath().equals("shared_hand_grenade") && motion != null && motion.release() >= 0) {
                ThirdPersonToolGrip.drawGrenade(spec, motion.release(), poseStack, buffers, packedLight);
            } else if (spec.model().getPath().equals("manba_flashlight")) {
                ManbaFlashlightBeamRenderer.renderLamp(player,spec.model(),spec.animation(),spec.seconds(),poseStack,buffers,packedLight,false);
            } else if (spec.model().getPath().equals("luna_composite_bow")) {
                LunaTool tool = ClientLunaBowVisualState.isActive(player) ? ClientLunaBowVisualState.tool(player)
                        : player == Minecraft.getInstance().player ? ClientLunaHudState.equippedTool()
                        : ClientHeldToolVisualState.visual(player) == HeldToolVisual.LUNA_RECON_BOW ? LunaTool.RECON_BOW : LunaTool.SHOCK_BOW;
                ClientToolFirstPerson.drawBow(tool, spec.animation(), spec.seconds(), poseStack, buffers, packedLight);
            } else {
                BlockbenchAnimatedModelRenderer.render(spec.model(), spec.animation(), spec.seconds(), poseStack, buffers, packedLight);
            }
        } finally {
            poseStack.popPose();
        }
    }

    static Vector3f launcherSupportElbow(Vector3f shoulder, Vector3f grip) {
        return new Vector3f(shoulder.x * 1.18F, grip.y - .04F, (shoulder.z + grip.z) * .5F);
    }

    public static void withSegmentedArm(AbstractClientPlayer player, PlayerModel<?> model, float partial, Runnable render) {
        if (!visible(player)) { render.run(); return; }
        boolean elbow = ClientSkillModelVisualState.visual(player) == SkillModelVisual.MANBA_ELBOW;
        if (!elbow) {
            var motion = ClientToolHandMotion.frame(player, partial);
            if (motion == null || motion.cue() != com.rzy.dealt_force_skills.skill.ToolHandCue.N_TWO_CONDENSER_LAUNCHER || motion.hidden()) {
                if (ClientHeldToolVisualState.visual(player) != HeldToolVisual.N_TWO_CONDENSER_LAUNCHER) { render.run(); return; }
            }
            var spec = forPlayer(player, partial, motion);
            if (spec == null || !spec.model().getPath().equals("n_two_condensation_focus") || motion != null && motion.hidden()) { render.run(); return; }
        }
        boolean right = (elbow ? player.getMainArm() : player.getMainArm().getOpposite()) == HumanoidArm.RIGHT;
        var arm = right ? model.rightArm : model.leftArm;
        var sleeve = right ? model.rightSleeve : model.leftSleeve;
        boolean armSkipped = arm.skipDraw, sleeveSkipped = sleeve.skipDraw;
        try {
            arm.skipDraw = sleeve.skipDraw = true;
            render.run();
        } finally {
            arm.skipDraw = armSkipped; sleeve.skipDraw = sleeveSkipped;
        }
    }

    private static boolean visible(Player player) {
        Minecraft minecraft = Minecraft.getInstance();
        return !player.isInvisible() && !player.isSpectator() && !((player == minecraft.player || player == minecraft.getCameraEntity())
                && minecraft.options.getCameraType().isFirstPerson());
    }

    public static boolean replacesHand(Player player, HumanoidArm arm) {
        if (!visible(player)) return false;
        if (ThirdPersonSkillPose.owns(ClientSkillModelVisualState.visual(player))) return arm == player.getMainArm();
        if (arm == player.getMainArm() && CharacterSkillModelVisuals.replacesHand(player)) return true;
        var motion = ClientToolHandMotion.frame(player, 0);
        if (motion != null && ThirdPersonSkillPose.pullsPin(motion) && arm != player.getMainArm()) return true;
        ToolSpec spec = forPlayer(player, 0, motion);
        return spec != null && ThirdPersonToolGrip.occupies(spec.model().getPath(), player.getMainArm(), arm);
    }

    public static void poseArms(Player player, PlayerModel<?> model, float partial) {
        if (!visible(player)) return;
        SkillModelVisual action = ClientSkillModelVisualState.visual(player);
        var motion = ClientToolHandMotion.frame(player, partial);
        boolean dedicated = ThirdPersonSkillPose.owns(action);
        boolean posed = dedicated ? ThirdPersonSkillPose.action(model, player.getMainArm(), action,
                ClientSkillModelVisualState.ageSeconds(player, partial) * 20F)
                : ThirdPersonSkillPose.tool(model, player.getMainArm(), motion) || ClientToolHandMotion.poseThirdPerson(player, model, partial);
        ToolSpec spec = forPlayer(player, partial, motion);
        if (!dedicated && spec != null && (motion == null || !motion.hidden())) {
            boolean timeline = motion != null && motion.release() >= 0;
            if (!timeline || ThirdPersonToolGrip.keepContactsOnRelease(spec.model().getPath())) {
                float deploy = ClientHeldToolVisualState.deployProgress(player, partial);
                posed |= ThirdPersonToolGrip.pose(spec, player.getMainArm(), model, deploy,
                        motion == null ? 1 : motion.draw(), poseWeight(motion), posed);
            }
        }
        if (posed) {
            model.leftSleeve.copyFrom(model.leftArm);
            model.rightSleeve.copyFrom(model.rightArm);
        }
    }

    private static float poseWeight(ClientToolHandMotion.Frame motion) {
        return motion == null || motion.release() < 0 ? 0 : (float)Math.sin(Math.PI * ClientToolHandMotion.smooth(motion.release()));
    }

    private static ToolSpec forPlayer(Player player, float partial, ClientToolHandMotion.Frame motion) {
        float seconds = (player.tickCount + partial) / 20F;
        boolean local = player == Minecraft.getInstance().player;
        SkillModelVisual action = ClientSkillModelVisualState.visual(player);
        if (action == SkillModelVisual.MANBA_ELBOW) return null;
        if (action == SkillModelVisual.MANBA_FLASHLIGHT_TOGGLE) return flashlight(seconds);
        if (action == SkillModelVisual.UNDEAD_ROGUE_BATON) return spec("undead_rogue_baton", null, 0, .78F);
        if (isNikaidouAttackVisual(action)) {
            boolean iron = action == SkillModelVisual.NIKAIDOU_HOT_IRON || action == SkillModelVisual.NIKAIDOU_HOT_IRON_OVERHEAD;
            return nikaidou(iron ? NikaidouHiroTool.HOT_IRON : NikaidouHiroTool.RITUAL_SWORD, seconds);
        }
        if (CharacterSkillModelVisuals.replacesHand(player) || ClientToolHandMotion.commandOwnsHands(player)) return null;
        if (motion != null && motion.cue() == ToolHandCue.VYRON_TIGER_CANNON)
            return spec(motion.cue().model, motion.clip(), motion.seconds(), .38F, 90F, -8F, 0F);
        if (motion != null && motion.release() >= 0 && motion.cue() != com.rzy.dealt_force_skills.skill.ToolHandCue.LUNA_BOW
                && motion.cue() != com.rzy.dealt_force_skills.skill.ToolHandCue.HACKCLAW_TERMINAL) {
            ToolSpec equipped = visualSpec(ClientHeldToolVisualState.visual(player), partial);
            String id = motion.cue().model;
            if (equipped != null && equipped.model().getPath().equals(id))
                return new ToolSpec(equipped.model(), motion.clip(), motion.seconds(), equipped.scale(), equipped.yaw(), equipped.pitch(), equipped.roll());
            float scale = switch (id) {
                case "d_wolf_hand_cannon" -> .86F;
                case "d_wolf_smoke_grenade" -> .83F;
                case "wildfire_side_launcher" -> .75F;
                case "shared_hand_grenade" -> .44F;
                case "uluru_quick_cover_package" -> .42F;
                case "hackclaw_knife" -> .54F;
                case "sineva_grapple_gun" -> .62F;
                case "saeed_ember_crossbow" -> .79F;
                default -> .48F;
            };
            return spec(id, motion.clip(), motion.seconds(), scale);
        }
        if (HackclawTerminalVisuals.visible(player)) return spec("hackclaw_hacking_terminal", null, seconds, .60F);
        if (ClientLunaBowVisualState.isActive(player)) return spec("luna_composite_bow",
                ClientLunaBowVisualState.phase(player) == ClientLunaBowVisualState.PHASE_RELEASE ? "release" : "draw",
                ClientLunaBowVisualState.animationSeconds(player, partial), .76F);
        if (local && ClientLunaHudState.hasEquippedTool() && ClientLunaHudState.equippedTool() != LunaTool.COMPOSITE_GRENADE)
            return spec("luna_composite_bow", "idle", seconds, .76F);
        if (local && (DWolfInputHandler.isSmokeHeldForVisual() || ClientToolModelAnimationState.isActive("d_wolf_smoke_grenade")))
            return animatedLocal("d_wolf_smoke_grenade", null, seconds, .83F);
        if (local && (ClientDWolfHudState.hasHandCannonEquipped() || ClientToolModelAnimationState.isActive("d_wolf_hand_cannon")))
            return animatedLocal("d_wolf_hand_cannon", "idle", seconds, .86F);
        if ((local && (SinevaInputHandler.isGrappleCharging() || ClientToolModelAnimationState.isActive("sineva_grapple_gun")))
                || TempestRecallVisuals.hasGrapple(player))
            return local ? animatedLocal("sineva_grapple_gun", null, seconds, .62F) : spec("sineva_grapple_gun", null, seconds, .62F);
        if (local && SinevaInputHandler.isBladeWireHeld()) return spec("sineva_blade_wire_core", "idle", seconds, .48F);
        if (com.rzy.dealt_force_skills.client.WildfireClient.trackedTool(player.getId()) == 1)
            return spec("wildfire_side_launcher", "idle", seconds, .75F);
        ToolSpec spec = wildfireSpec(player.getId(), partial);
        if (spec != null) return spec;
        return local ? currentLocalSpec(player, partial) : visualSpec(ClientHeldToolVisualState.visual(player), partial);
    }

    private static ToolSpec animatedLocal(String model, String idle, float seconds, float scale) {
        var frame = ClientToolModelAnimationState.frame(model, id(model), idle, seconds);
        return spec(model, frame.animation(), frame.seconds(), scale);
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation(DealtForceSkillsMod.MODID, path);
    }

    private static ToolSpec wildfireSpec(int entityId, float partialTick) {
        int tool = com.rzy.dealt_force_skills.client.WildfireClient.trackedTool(entityId);
        if (tool != 1 && tool != 2) return null;
        var level = Minecraft.getInstance().level;
        var target = level == null ? null : level.getEntity(entityId);
        if (target == null || !target.isAlive()) return null;
        return new ToolSpec(com.rzy.dealt_force_skills.client.WildfireClient.toolModel(tool), "idle",
                (target.tickCount + partialTick) / 20.0F, tool == 1 ? 0.69F : 0.76F, 0.0F, 0.0F, 90.0F);
    }

    private static ToolSpec currentLocalSpec(Player player, float partialTick) {
        float seconds = (player.tickCount + partialTick) / 20.0F;
        String saeedClip = SaeedPlaceholderVisuals.crossbowClip();
        if (saeedClip != null) return new ToolSpec(SaeedPlaceholderVisuals.HAND_MODEL, saeedClip,
                seconds, 0.79F, 180.0F, -6.0F, 90.0F);
        int roverTool = com.rzy.dealt_force_skills.client.RoverClient.tool();
        if (ClientHeldToolVisualState.visual(player) == HeldToolVisual.ROVER_DOG_WHISTLE) return visualSpec(HeldToolVisual.ROVER_DOG_WHISTLE, partialTick);
        if (roverTool == 1) return visualSpec(HeldToolVisual.ROVER_IRRITANT_SMOKE, partialTick);
        if (roverTool == 2) return visualSpec(HeldToolVisual.ROVER_AEROSOL_INJECTOR, partialTick);
        ClientToolReleaseAction.Action releaseAction = activeReleaseAction();
        if (releaseAction != null) {
            return release(releaseAction, seconds);
        }
        if (ClientManbaHudState.flashlightActive()) {
            return flashlight(seconds);
        }
        if (ClientNikaidouHiroHudState.hasEquippedTool()) {
            return nikaidou(ClientNikaidouHiroHudState.equippedTool(), seconds);
        }
        if (ClientGizmoHudState.hasEquippedTool()) {
            return gizmo(ClientGizmoHudState.equippedTool(), seconds);
        }
        if (ClientChamberHudState.hasEquippedTool()) {
            return chamber(ClientChamberHudState.equippedTool(), seconds);
        }
        if (ClientNTwoHudState.hasEquippedTool()) {
            return ntwo(ClientNTwoHudState.equippedTool(), seconds);
        }
        if (ClientMorseHudState.hasEquippedTool()) {
            return morse(ClientMorseHudState.equippedTool(), seconds);
        }
        if (ClientShepherdHudState.hasEquippedTool()) {
            ShepherdTool tool = ClientShepherdHudState.equippedTool();
            return tool == ShepherdTool.SONIC_TRAP
                    ? spec("shepherd_sonic_trap", "idle", seconds, 0.25F)
                    : spec("shared_hand_grenade", "idle", seconds, 0.44F);
        }
        if (ClientRaptorHudState.hasEquippedTool()) {
            RaptorTool tool = ClientRaptorHudState.equippedTool();
            return tool == RaptorTool.FALCON_DRONE
                    ? spec("raptor_falcon_drone", "idle_flight", seconds, 0.46F)
                    : spec("raptor_pulse_grenade", "idle_hold", seconds, 0.42F);
        }
        if (ClientNoxHudState.hasEquippedTool()) {
            NoxTool tool = ClientNoxHudState.equippedTool();
            return tool == NoxTool.ROTOR
                    ? spec("nox_rotor", "idle", seconds, 0.52F)
                    : spec("nox_flash_grenade", "idle", seconds, 0.44F);
        }
        if (ClientToxikHudState.hasEquippedTool()) {
            ToxikTool tool = ClientToxikHudState.equippedTool();
            return tool == ToxikTool.FIREFLY_SWARM
                    ? spec("toxik_firefly_swarm", "idle_hover", seconds, 0.48F)
                    : spec("toxik_tear_gas_grenade", "idle", seconds, 0.44F);
        }
        if (ClientStingerHudState.hasEquippedTool()) {
            return stinger(ClientStingerHudState.equippedTool(), seconds);
        }
        if (ClientHackclawHudState.hasEquippedTool()) {
            HackclawTool tool = ClientHackclawHudState.equippedTool();
            return tool == HackclawTool.FLASH_DRONE
                    ? spec("hackclaw_flash_drone", "idle_closed", seconds, 0.44F)
                    : spec("hackclaw_knife", "idle_charge", seconds, 0.54F);
        }
        if (ClientTempestHudState.hasEquippedTool()) {
            return spec("tempest_wall_drill_stinger", "idle", seconds, 0.48F,
                    180.0F, 0.0F, 0.0F);
        }
        if (ClientLunaHudState.hasEquippedTool()
                && ClientLunaHudState.equippedTool() == LunaTool.COMPOSITE_GRENADE) {
            return spec("shared_hand_grenade", "idle", seconds, 0.44F);
        }
        if (ClientUluruHudState.hasEquippedTool()) {
            return uluru(ClientUluruHudState.equippedTool(), seconds);
        }
        if (ClientVlinderHudState.hasEquippedTool()) {
            return spec("vlinder_medical_drone", "idle_hover", seconds, 0.42F);
        }
        if (ClientVyronHudState.hasEquippedTool()) {
            VyronTool tool = ClientVyronHudState.equippedTool();
            if (tool == VyronTool.TIGER_CANNON) return visualSpec(HeldToolVisual.VYRON_TIGER_CANNON, partialTick);
            ClientVyronToolAnimationState.observeEquipped(tool);
            ClientVyronToolAnimationState.AnimationFrame frame =
                    ClientVyronToolAnimationState.frame(tool, seconds);
            return spec("vyron_magnetic_bomb", frame.animation(), frame.seconds(), 0.52F);
        }
        if (ClientDepartmentHudState.hasEquippedTool()) {
            return spec("department_explosive_trap", "idle_hold", seconds, 0.52F);
        }
        return null;
    }

    private static ToolSpec visualSpec(HeldToolVisual visual, float partialTick) {
        float seconds = (Minecraft.getInstance().level == null
                ? 0.0F : (Minecraft.getInstance().level.getGameTime() + partialTick) / 20.0F);
        return switch (visual) {
            case NONE, SAEED_COMMAND -> null;
            case D_WOLF_HAND_CANNON -> spec("d_wolf_hand_cannon", "idle", seconds, .86F);
            case D_WOLF_SMOKE -> spec("d_wolf_smoke_grenade", null, seconds, .83F);
            case SINEVA_WIRE -> spec("sineva_blade_wire_core", "idle", seconds, .48F);
            case SINEVA_HOOK -> spec("sineva_grapple_gun", null, seconds, .62F);
            case SAEED_CROSSBOW, SAEED_CROSSBOW_RICOCHET, SAEED_CROSSBOW_SPENT -> new ToolSpec(
                    SaeedPlaceholderVisuals.HAND_MODEL, visual == HeldToolVisual.SAEED_CROSSBOW_SPENT ? "spent"
                    : visual == HeldToolVisual.SAEED_CROSSBOW_RICOCHET ? "ricochet" : "direct", seconds, .79F, 180F, -6F, 90F);
            case LUNA_SHOCK_BOW, LUNA_RECON_BOW -> spec("luna_composite_bow", "idle", seconds, .76F);
            case ROVER_AEROSOL_INJECTOR -> spec("rover_aerosol_injector", null, seconds, 0.48F, 0.0F, -8.0F, 0.0F);
            case ROVER_IRRITANT_SMOKE -> spec("rover_irritant_smoke", null, seconds, 0.52F, 8F, 0F, 80F);
            case ROVER_DOG_WHISTLE -> spec("rover_dog_whistle", null, seconds, 0.39F);
            case MANBA_FLASHLIGHT -> flashlight(seconds);
            case NIKAIDOU_HOT_IRON -> nikaidou(NikaidouHiroTool.HOT_IRON, seconds);
            case NIKAIDOU_RITUAL_SWORD -> nikaidou(NikaidouHiroTool.RITUAL_SWORD, seconds);
            case GIZMO_SMOKE_TRAP -> gizmo(GizmoTool.SMOKE_TRAP, seconds);
            case GIZMO_SPIDER_NEST -> gizmo(GizmoTool.SPIDER_NEST, seconds);
            case GIZMO_T_BOY -> gizmo(GizmoTool.T_BOY, seconds);
            case CHAMBER_TELEPORT_CARD -> chamber(ChamberTool.TELEPORT_CARD, seconds);
            case CHAMBER_TRAP_CARD -> chamber(ChamberTool.TRAP_CARD, seconds);
            case N_TWO_DEWAR_CANISTER -> ntwo(NTwoTool.DEWAR_CANISTER, seconds);
            case N_TWO_CONDENSER_LAUNCHER -> ntwo(NTwoTool.CONDENSER_LAUNCHER, seconds);
            case MORSE_SHOCK_ORB -> morse(MorseTool.SHOCK_ORB, seconds);
            case MORSE_FLASH_GRENADE -> morse(MorseTool.FLASH_GRENADE, seconds);
            case MORSE_SONAR_DETECTOR -> morse(MorseTool.SONAR_DETECTOR, seconds);
            case SHEPHERD_SONIC_TRAP -> spec("shepherd_sonic_trap", "idle", seconds, 0.25F);
            case SHEPHERD_GRENADE -> spec("shared_hand_grenade", "idle", seconds, 0.44F);
            case RAPTOR_FALCON_DRONE -> spec("raptor_falcon_drone", "idle_flight", seconds, 0.46F);
            case RAPTOR_PULSE_GRENADE -> spec("raptor_pulse_grenade", "idle_hold", seconds, 0.42F);
            case NOX_ROTOR -> spec("nox_rotor", "idle", seconds, 0.52F);
            case NOX_FLASH_GRENADE -> spec("nox_flash_grenade", "idle", seconds, 0.44F);
            case TOXIK_TEAR_GAS -> spec("toxik_tear_gas_grenade", "idle", seconds, 0.44F);
            case TOXIK_FIREFLY_SWARM -> spec("toxik_firefly_swarm", "idle_hover", seconds, 0.48F);
            case STINGER_SMOKE_GRENADE -> stinger(StingerTool.SMOKE_GRENADE, seconds);
            case STINGER_SMOKE_DRONE -> stinger(StingerTool.SMOKE_DRONE, seconds);
            case STINGER_STIM_GUN -> stinger(StingerTool.STIM_GUN, seconds);
            case HACKCLAW_HACKING_KNIFE -> spec("hackclaw_knife", "idle_charge", seconds, 0.54F);
            case HACKCLAW_FLASH_DRONE -> spec("hackclaw_flash_drone", "idle_closed", seconds, 0.44F);
            case TEMPEST_WALL_DRILL_STINGER -> spec("tempest_wall_drill_stinger", "idle", seconds, 0.48F,
                    180.0F, 0.0F, 0.0F);
            case LUNA_COMPOSITE_GRENADE -> spec("shared_hand_grenade", "idle", seconds, 0.44F);
            case ULURU_INCENDIARY -> uluru(UluruTool.INCENDIARY, seconds);
            case ULURU_COVER -> uluru(UluruTool.COVER, seconds);
            case ULURU_MISSILE -> uluru(UluruTool.MISSILE, seconds);
            case VLINDER_MEDICAL_DRONE -> spec("vlinder_medical_drone", "idle_hover", seconds, 0.42F);
            case VYRON_MAGNETIC_BOMB -> spec("vyron_magnetic_bomb", "idle", seconds, 0.52F);
            case VYRON_TIGER_CANNON -> spec("vyron_tiger_cannon_launcher", null, seconds,
                    0.38F, 90.0F, -8.0F, 0.0F);
            case DEPARTMENT_EXPLOSIVE_TRAP -> spec("department_explosive_trap", "idle_hold", seconds, 0.52F);
        };
    }

    private static ClientToolReleaseAction.Action activeReleaseAction() {
        for (ClientToolReleaseAction.Action action : ClientToolReleaseAction.Action.values()) {
            if (ClientToolReleaseAction.isActive(action)) {
                return action;
            }
        }
        return null;
    }

    private static ToolSpec release(ClientToolReleaseAction.Action action, float seconds) {
        ClientToolModelAnimationState.AnimationFrame frame =
                ClientToolReleaseAction.frame(action, action.animation(), seconds);
        float scale = switch (action) {
            case HACKCLAW_KNIFE -> 0.54F;
            case HACKCLAW_FLASH_DRONE, NOX_FLASH_GRENADE, MORSE_FLASH_GRENADE,
                    SHEPHERD_GRENADE, LUNA_GRENADE, STINGER_SMOKE_GRENADE,
                    TOXIK_TEAR_GAS -> 0.44F;
            case STINGER_STIM_PRIME, STINGER_STIM_FIRE -> 0.50F;
            case TOXIK_FIREFLY -> 0.48F;
            case RAPTOR_PULSE_GRENADE -> 0.42F;
        };
        return new ToolSpec(action.model(), frame.animation(), frame.seconds(), scale, 0.0F, 0.0F, 0.0F);
    }

    private static ToolSpec nikaidou(NikaidouHiroTool tool, float seconds) {
        return switch (tool) {
            case HOT_IRON -> spec("nikaidou_hiro_hot_iron", null, seconds, 0.46F);
            case RITUAL_SWORD -> spec("nikaidou_hiro_ritual_sword", null, seconds, 0.56F);
            case NONE -> null;
        };
    }

    private static ToolSpec flashlight(float seconds) {
        return new ToolSpec(new ResourceLocation(DealtForceSkillsMod.MODID, "manba_flashlight"),
                "idle_hold", seconds, 0.64F, 0.0F, -6.0F, 0.0F);
    }

    private static ToolSpec gizmo(GizmoTool tool, float seconds) {
        return switch (tool) {
            case SMOKE_TRAP -> spec("gizmo_smoke_trap", "idle", seconds, 0.22F);
            case SPIDER_NEST -> spec("gizmo_spider_nest_trap", "idle", seconds, 0.34F);
            case T_BOY -> spec("gizmo_t_boy", "idle", seconds, 0.42F);
            case NONE -> null;
        };
    }

    private static ToolSpec chamber(ChamberTool tool, float seconds) {
        return null;
    }

    private static ToolSpec ntwo(NTwoTool tool, float seconds) {
        return switch (tool) {
            case DEWAR_CANISTER -> spec("n_two_dewar_canister", "idle", seconds, 0.64F);
            case CONDENSER_LAUNCHER -> spec("n_two_condensation_focus", "aurora", seconds, 0.38F,
                    90.0F, -8.0F, 0.0F);
            case NONE -> null;
        };
    }

    private static ToolSpec morse(MorseTool tool, float seconds) {
        return switch (tool) {
            case SHOCK_ORB -> spec("morse_shock_orb", "idle_hold", seconds, 0.42F);
            case FLASH_GRENADE -> spec("morse_flash_grenade", "flash_prepare", seconds, 0.42F);
            case SONAR_DETECTOR -> spec("morse_sonar_detector", "deploy_ready", seconds, 0.42F);
            case NONE -> null;
        };
    }

    private static ToolSpec stinger(StingerTool tool, float seconds) {
        return switch (tool) {
            case SMOKE_GRENADE -> spec("stinger_smoke_grenade", "idle", seconds, 0.44F);
            case SMOKE_DRONE -> spec("stinger_smoke_drone", "idle", seconds, 0.46F);
            case STIM_GUN -> spec("stinger_stim_gun", "idle", seconds, 0.50F,
                    0.0F, -8.0F, 0.0F);
            case NONE -> null;
        };
    }

    private static ToolSpec uluru(UluruTool tool, float seconds) {
        return switch (tool) {
            case INCENDIARY -> spec("uluru_incendiary_grenade", null, seconds, 0.44F);
            case COVER -> spec("uluru_quick_cover_package", null, seconds, 0.42F);
            case MISSILE -> spec("uluru_missile_launcher", null, seconds, 0.38F,
                    90.0F, -8.0F, 0.0F);
            case NONE -> null;
        };
    }

    private static ToolSpec spec(String model, String animation, float seconds, float scale) {
        return spec(model, animation, seconds, scale, 0.0F, 0.0F, 0.0F);
    }

    private static ToolSpec spec(
            String model,
            String animation,
            float seconds,
            float scale,
            float yaw,
            float pitch,
            float roll
    ) {
        return new ToolSpec(new ResourceLocation(DealtForceSkillsMod.MODID, model),
                animation, seconds, scale, yaw, pitch, roll);
    }

    static boolean isNikaidouAttackVisual(SkillModelVisual visual) {
        return visual == SkillModelVisual.NIKAIDOU_HOT_IRON
                || visual == SkillModelVisual.NIKAIDOU_RITUAL_SWORD
                || visual == SkillModelVisual.NIKAIDOU_HOT_IRON_OVERHEAD
                || visual == SkillModelVisual.NIKAIDOU_RITUAL_SWORD_RIGHT_TO_LEFT
                || visual == SkillModelVisual.NIKAIDOU_RITUAL_SWORD_DIAGONAL;
    }

    record ToolSpec(
            ResourceLocation model,
            String animation,
            float seconds,
            float scale,
            float yaw,
            float pitch,
            float roll
    ) {
        void applyGrip(PlayerModel<?> parent, HumanoidArm mainArm, PoseStack stack) {
            String id = model.getPath();
            HumanoidArm arm = mainArm;
            if (parent.young) {
                stack.translate(0, 0.75F, 0);
                stack.scale(0.5F, 0.5F, 0.5F);
            }
            parent.translateToHand(arm, stack);
            stack.translate((arm == HumanoidArm.RIGHT ? -1 : 1) / 16.0F, 10 / 16.0F, -2 / 16.0F);
            stack.mulPose(Axis.XP.rotationDegrees(180.0F + pitch));
            stack.mulPose(Axis.YP.rotationDegrees(yaw));
            stack.mulPose(Axis.ZP.rotationDegrees(-90.0F + roll));
            stack.scale(scale, scale, scale);
            switch (id) {
                case "nox_flash_grenade" -> centerGrip(stack, 0, 5.5, 0);
                case "rover_irritant_smoke" -> centerGrip(stack, 0, -1.3, 0);
                case "uluru_quick_cover_package" -> centerGrip(stack, 0, -2.4, 1.775);
                case "vyron_magnetic_bomb" -> centerGrip(stack, 0, 7, 0);
            }
        }

        private static void centerGrip(PoseStack stack, double gripX, double gripY, double gripZ) {
            stack.translate(-gripX / 16, -gripY / 16, -gripZ / 16);
        }
    }
}
