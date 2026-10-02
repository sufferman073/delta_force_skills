package com.rzy.dealt_force_skills.client;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT)
public final class ClientTempestRollVisuals {
    private static final int CAMERA_DIP_TICKS = 9;
    private static int cameraDipTicks;

    private ClientTempestRollVisuals() {
    }

    public static void startRoll() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        cameraDipTicks = CAMERA_DIP_TICKS;
        player.setForcedPose(net.minecraft.world.entity.Pose.SWIMMING);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (cameraDipTicks <= 0) {
            return;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            cameraDipTicks = 0;
            return;
        }

        float partialTick = (float) com.rzy.dealt_force_skills.client.ClientRenderCompat.partialTick(event.getPartialTick());
        float elapsed = Mth.clamp(CAMERA_DIP_TICKS - cameraDipTicks + partialTick, 0.0F, (float) CAMERA_DIP_TICKS);
        float progress = elapsed / (float) CAMERA_DIP_TICKS;
        float dip = Mth.sin(progress * (float) Math.PI) * 10.0F;
        event.setYaw(player.getViewYRot(partialTick));
        event.setPitch(Mth.clamp(player.getViewXRot(partialTick) + dip, -89.9F, 89.9F));
        event.setRoll(0.0F);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (cameraDipTicks <= 0) {
            return;
        }
        if (Minecraft.getInstance().player == null) {
            cameraDipTicks = 0;
            return;
        }
        cameraDipTicks--;
        if (cameraDipTicks == 0 && Minecraft.getInstance().player.getForcedPose() == net.minecraft.world.entity.Pose.SWIMMING) {
            Minecraft.getInstance().player.setForcedPose(null);
        }
    }
}
