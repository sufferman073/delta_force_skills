package com.rzy.dealt_force_skills.client;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * Client half of the velocity-driven zipline.
 *
 * <p>The server owns the traversal and pushes one position sample per tick. Applying the sample directly
 * to the local player is what makes the ride smooth without a carrier entity: the camera never switches to
 * a vehicle transform and other mods (TACZ muzzle placement) keep reading the real player entity.</p>
 *
 * <p>Local velocity is intentionally zeroed: the server is the only mover, so the client must not add its
 * own prediction on top of the sample. The sample is re-applied at the end of the client tick as well,
 * which cancels the small drift produced by the local player's own walk input while the line is held.</p>
 */
@EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT)
public final class HvkZiplineMotionClient {
    private static boolean active;
    private static LocalPlayer rider;
    private static ClientLevel rideLevel;
    private static boolean previousNoGravity;
    private static boolean previousNoPhysics;
    private static double sampleX;
    private static double sampleY;
    private static double sampleZ;

    private HvkZiplineMotionClient() {
    }

    /** Packet callback: absolute authoritative position (or the stop notice when {@code active} is false). */
    public static void apply(boolean active, double x, double y, double z) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            clear();
            return;
        }
        if (rider != player || rideLevel != mc.level) clear();
        if (!active) {
            if (rider != player) return;
            player.setPos(x, y, z);
            player.setDeltaMovement(Vec3.ZERO);
            player.fallDistance = 0.0F;
            clear();
            return;
        }
        if (rider == null) {
            rider = player;
            rideLevel = mc.level;
            previousNoGravity = player.isNoGravity();
            previousNoPhysics = player.noPhysics;
        }
        HvkZiplineMotionClient.active = active;
        HvkZiplineMotionClient.sampleX = x;
        HvkZiplineMotionClient.sampleY = y;
        HvkZiplineMotionClient.sampleZ = z;
        player.setPos(x, y, z);
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0.0F;
        player.setNoGravity(active);
        player.noPhysics = active;
    }

    /** Keeps the local player pinned to the latest server sample instead of letting walk input drift it. */
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!active) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player != rider || mc.level != rideLevel) {
            clear();
            return;
        }
        player.setPos(sampleX, sampleY, sampleZ);
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0.0F;
        player.setNoGravity(true);
        player.noPhysics = true;
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        clear();
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() == rideLevel) clear();
    }

    private static void clear() {
        if (rider != null) {
            rider.setNoGravity(previousNoGravity);
            rider.noPhysics = previousNoPhysics || rider.isSpectator();
        }
        active = false;
        rider = null;
        rideLevel = null;
    }
}
