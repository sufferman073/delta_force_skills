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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT)
public final class HeldToolThirdPersonVisuals {
    private HeldToolThirdPersonVisuals() {
    }

    public static boolean renderSpectatorFirstPerson(GuiGraphics graphics, int targetEntityId, float partialTick) {
        ToolSpec spec = visualSpec(ClientHeldToolVisualState.visual(targetEntityId), partialTick);
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

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = event.getEntity();
        if (player.isInvisible()
                || player == minecraft.player && minecraft.options.getCameraType().isFirstPerson()) {
            return;
        }
        if (isNikaidouAttackVisual(ClientSkillModelVisualState.visual(player))) {
            return;
        }
        ToolSpec spec = player == minecraft.player
                ? currentLocalSpec(player, com.rzy.dealt_force_skills.client.ClientRenderCompat.partialTick(event.getPartialTick()))
                : visualSpec(ClientHeldToolVisualState.visual(player), com.rzy.dealt_force_skills.client.ClientRenderCompat.partialTick(event.getPartialTick()));
        if (spec == null) {
            return;
        }
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        applyRightHandAnchor(event, poseStack);
        poseStack.translate(spec.x(), spec.y(), spec.z());
        poseStack.mulPose(Axis.XP.rotationDegrees(180.0F + spec.pitch()));
        poseStack.mulPose(Axis.YP.rotationDegrees(spec.yaw()));
        poseStack.mulPose(Axis.ZP.rotationDegrees(-90.0F + spec.roll()));
        poseStack.scale(spec.scale(), spec.scale(), spec.scale());
        BlockbenchAnimatedModelRenderer.render(spec.model(), spec.animation(), spec.seconds(),
                poseStack, event.getMultiBufferSource(), event.getPackedLight());
        poseStack.popPose();
    }

    private static ToolSpec currentLocalSpec(Player player, float partialTick) {
        float seconds = (player.tickCount + partialTick) / 20.0F;
        int roverTool = com.rzy.dealt_force_skills.client.RoverClient.tool();
        if (roverTool > 0) return stinger(roverTool == 1 ? StingerTool.SMOKE_GRENADE : StingerTool.STIM_GUN, seconds);
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
            ClientVyronToolAnimationState.observeEquipped(tool);
            ClientVyronToolAnimationState.AnimationFrame frame =
                    ClientVyronToolAnimationState.frame(tool, seconds);
            return tool == VyronTool.TIGER_CANNON
                    ? spec("vyron_tiger_cannon_launcher", frame.animation(), frame.seconds(),
                    0.38F, 90.0F, -8.0F, 0.0F)
                    : spec("vyron_magnetic_bomb", frame.animation(), frame.seconds(), 0.52F);
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
            case NONE -> null;
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
            case VYRON_TIGER_CANNON -> spec("vyron_tiger_cannon_launcher", "idle", seconds,
                    0.38F, 90.0F, -8.0F, 0.0F);
            case DEPARTMENT_EXPLOSIVE_TRAP -> spec("department_explosive_trap", "idle_hold", seconds, 0.52F);
        };
    }

    private static void applyRightHandAnchor(RenderPlayerEvent.Post event, PoseStack poseStack) {
        event.getRenderer().getModel().rightArm.translateAndRotate(poseStack);
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
        return new ToolSpec(action.model(), frame.animation(), frame.seconds(), scale,
                -0.06D, 0.18D, -0.08D, 0.0F, 0.0F, 0.0F);
    }

    private static ToolSpec nikaidou(NikaidouHiroTool tool, float seconds) {
        return switch (tool) {
            case HOT_IRON -> spec("nikaidou_hiro_hot_iron", null, seconds, 0.46F);
            case RITUAL_SWORD -> spec("nikaidou_hiro_ritual_sword", null, seconds, 0.56F);
            case NONE -> null;
        };
    }

    private static ToolSpec flashlight(float seconds) {
        return new ToolSpec(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "manba_flashlight"),
                "idle_hold", seconds, 0.64F,
                -0.02D, 0.54D, -0.06D, 0.0F, -6.0F, 0.0F);
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
            case DEWAR_CANISTER -> spec("shared_hand_grenade", "idle", seconds, 0.44F);
            case CONDENSER_LAUNCHER -> spec("uluru_missile_launcher", null, seconds, 0.38F,
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
        return new ToolSpec(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, model),
                animation, seconds, scale, -0.06D, 0.18D, -0.08D, yaw, pitch, roll);
    }

    private static boolean isNikaidouAttackVisual(SkillModelVisual visual) {
        return visual == SkillModelVisual.NIKAIDOU_HOT_IRON
                || visual == SkillModelVisual.NIKAIDOU_RITUAL_SWORD
                || visual == SkillModelVisual.NIKAIDOU_HOT_IRON_OVERHEAD
                || visual == SkillModelVisual.NIKAIDOU_RITUAL_SWORD_RIGHT_TO_LEFT
                || visual == SkillModelVisual.NIKAIDOU_RITUAL_SWORD_DIAGONAL;
    }

    private record ToolSpec(
            ResourceLocation model,
            String animation,
            float seconds,
            float scale,
            double x,
            double y,
            double z,
            float yaw,
            float pitch,
            float roll
    ) {
    }
}
