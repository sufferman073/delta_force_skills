package com.rzy.dealt_force_skills.client.visual;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.luna.LunaTool;
import com.rzy.dealt_force_skills.character.hackclaw.HackclawTool;
import com.rzy.dealt_force_skills.character.morse.MorseTool;
import com.rzy.dealt_force_skills.character.nox.NoxTool;
import com.rzy.dealt_force_skills.character.raptor.RaptorTool;
import com.rzy.dealt_force_skills.character.shepherd.ShepherdTool;
import com.rzy.dealt_force_skills.character.tempest.TempestTool;
import com.rzy.dealt_force_skills.character.uluru.UluruTool;
import com.rzy.dealt_force_skills.character.vyron.VyronTool;
import com.rzy.dealt_force_skills.client.DWolfInputHandler;
import com.rzy.dealt_force_skills.client.SinevaInputHandler;
import com.rzy.dealt_force_skills.client.VyronInputHandler;
import com.rzy.dealt_force_skills.client.character.ClientDWolfHudState;
import com.rzy.dealt_force_skills.client.character.ClientHackclawHudState;
import com.rzy.dealt_force_skills.client.character.ClientLunaHudState;
import com.rzy.dealt_force_skills.client.character.ClientMorseHudState;
import com.rzy.dealt_force_skills.client.character.ClientNoxHudState;
import com.rzy.dealt_force_skills.client.character.ClientRaptorHudState;
import com.rzy.dealt_force_skills.client.character.ClientShepherdHudState;
import com.rzy.dealt_force_skills.client.character.ClientTempestHudState;
import com.rzy.dealt_force_skills.client.character.ClientUluruHudState;
import com.rzy.dealt_force_skills.client.character.ClientVyronHudState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT)
public final class ThrowTrajectoryPreview {
    private static final int MAX_POINTS = 96;
    private static final int TUBE_SIDES = 12;
    private static final double TUBE_RADIUS = 0.035;
    private static final int TUBE_ALPHA = 48;
    private static final double LANDING_RADIUS = 0.11;
    private static float renderPartialTick;

    private ThrowTrajectoryPreview() {
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.screen != null) {
            return;
        }

        renderPartialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Preview preview = currentPreview(minecraft.player);
        if (preview == null) {
            return;
        }

        Trajectory trajectory = simulate(minecraft.level, minecraft.player, preview);
        if (trajectory.points().size() < 2) {
            return;
        }

        MultiBufferSource.BufferSource buffer = minecraft.renderBuffers().bufferSource();
        VertexConsumer consumer = buffer.getBuffer(com.rzy.dealt_force_skills.client.renderer.DfsRenderTypes.trajectoryQuads());
        PoseStack poseStack = event.getPoseStack();
        PoseStack.Pose pose = poseStack.last();
        Vec3 camera = event.getCamera().getPosition();

        renderTube(consumer, pose, camera, trajectory.points(), preview.red(), preview.green(), preview.blue());
        Vec3 sphereCenter = trajectory.hit()
                ? trajectory.end().add(trajectory.normal().scale(LANDING_RADIUS + 0.008)) : trajectory.end();
        renderLandingSphere(consumer, pose, camera, sphereCenter,
                preview.red(), preview.green(), preview.blue(), trajectory.hit() ? 56 : 34);
        renderLandingMark(consumer, pose, camera, trajectory.end(), trajectory.normal(),
                trajectory.hit() ? 50 : 28, preview.red(), preview.green(), preview.blue());
        buffer.endBatch(com.rzy.dealt_force_skills.client.renderer.DfsRenderTypes.trajectoryQuads());
    }

    private static Preview currentPreview(Player player) {
        if (ClientShepherdHudState.shouldRender() && ClientShepherdHudState.equippedTool() == ShepherdTool.FRAG_GRENADE) {
            return preview(player, 0.70D, -0.12D, 1.45D, 0.18D, 0.045D, 0.985D, 80, 255, 175, 64);
        }
        if (ClientLunaHudState.shouldRender() && ClientLunaHudState.equippedTool() == LunaTool.COMPOSITE_GRENADE) {
            return preview(player, 0.70D, -0.12D, 1.55D, 0.18D, 0.045D, 0.986D, 80, 112, 210, 255);
        }
        if (ClientHackclawHudState.shouldRender() && ClientHackclawHudState.equippedTool() == HackclawTool.HACKING_KNIFE) {
            return preview(player, 0.62D, -0.08D, 1.55D, 0.30D, 0.040D, 0.988D, 80, 98, 223, 164);
        }
        if (ClientHackclawHudState.shouldRender() && ClientHackclawHudState.equippedTool() == HackclawTool.FLASH_DRONE) {
            return preview(player, 0.68D, -0.04D, 1.45D, 0.10D, 0.012D, 0.995D, 65, 255, 227, 143);
        }
        if (ClientVyronHudState.shouldRender()
                && (ClientVyronHudState.equippedTool() == VyronTool.MAGNETIC_BOMB || VyronInputHandler.isMagneticBombHeldForVisual())) {
            return preview(player, 0.62D, -0.10D, 1.75D, 0.32D, 0.040D, 0.990D, 80, 150, 255, 150);
        }
        if (ClientVyronHudState.shouldRender() && ClientVyronHudState.equippedTool() == VyronTool.TIGER_CANNON) {
            return preview(player, 0.80D, -0.08D, 1.90D, 0.08D, 0.045D, 0.992D, 90, 255, 136, 64);
        }
        if (ClientNoxHudState.shouldRender() && ClientNoxHudState.equippedTool() == NoxTool.FLASH_GRENADE) {
            return preview(player, 0.58D, -0.10D, 1.55D, 0.28D, 0.045D, 0.985D, 80, 245, 245, 180);
        }
        if (ClientMorseHudState.shouldRender() && ClientMorseHudState.equippedTool() == MorseTool.SHOCK_ORB) {
            return preview(player, 0.70D, -0.12D, 1.45D, 0.18D, 0.045D, 0.985D, 80, 242, 200, 75);
        }
        if (ClientMorseHudState.shouldRender() && ClientMorseHudState.equippedTool() == MorseTool.FLASH_GRENADE) {
            return preview(player, 0.58D, -0.10D, 1.55D, 0.28D, 0.045D, 0.985D, 80, 225, 232, 255);
        }
        if (ClientRaptorHudState.shouldRender() && ClientRaptorHudState.equippedTool() == RaptorTool.PULSE_GRENADE) {
            return preview(player, 0.58D, -0.10D, 1.75D, 0.34D, 0.045D, 0.985D, 75, 96, 168, 255);
        }
        if (ClientTempestHudState.shouldRender() && ClientTempestHudState.equippedTool() == TempestTool.WALL_DRILL_STINGER) {
            return preview(player, 0.62D, -0.08D, 1.55D, 0.30D, 0.035D, 0.988D, 80, 124, 255, 91);
        }
        if (ClientDWolfHudState.hasHandCannonEquipped()) {
            return preview(player, 0.75D, -0.12D, 2.05D, 0.05D, 0.045D, 0.985D, 70, 255, 88, 72);
        }
        if (ClientUluruHudState.shouldRender() && ClientUluruHudState.equippedTool() == UluruTool.INCENDIARY) {
            return preview(player, 0.55D, -0.10D, 2.25D, 0.14D, 0.040D, 0.980D, 80, 255, 105, 48);
        }
        if (ClientUluruHudState.shouldRender() && ClientUluruHudState.equippedTool() == UluruTool.COVER) {
            return preview(player, 0.55D, -0.10D, 1.35D, 0.04D, 0.035D, 0.985D, 70, 110, 205, 255);
        }
        if (SinevaInputHandler.isBladeWireHeld()) {
            return preview(player, 0.55D, -0.10D, 2.25D, 0.14D, 0.030D, 0.990D, 80, 190, 190, 210);
        }
        if (DWolfInputHandler.isSmokeHeldForVisual()) {
            boolean high = DWolfInputHandler.isSmokeHighThrowPreview();
            return preview(player, 0.58D, -0.10D, high ? 1.80D : 1.05D, high ? 0.34D : 0.08D,
                    0.045D, 0.985D, 75, 170, 170, 170);
        }
        return null;
    }

    private static Preview preview(Player player, double forwardOffset, double yOffset, double speed, double lift,
                                   double gravity, double drag, int maxTicks, int red, int green, int blue) {
        Vec3 look = player.getViewVector(renderPartialTick).normalize();
        Vec3 start = player.getEyePosition(renderPartialTick).add(look.scale(forwardOffset)).add(0.0D, yOffset, 0.0D);
        Vec3 velocity = look.scale(speed).add(0.0D, lift, 0.0D);
        return new Preview(start, velocity, gravity, drag, Math.min(maxTicks, MAX_POINTS), red, green, blue);
    }

    private static Trajectory simulate(Level level, Player player, Preview preview) {
        List<Vec3> points = new ArrayList<>();
        Vec3 pos = preview.start();
        Vec3 motion = preview.velocity();
        points.add(pos);

        for (int i = 0; i < preview.maxTicks(); i++) {
            Vec3 next = pos.add(motion);
            net.minecraft.world.phys.BlockHitResult hit = level.clip(new ClipContext(pos, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            if (hit.getType() != HitResult.Type.MISS) {
                Vec3 end = hit.getLocation();
                points.add(end);
                return new Trajectory(points, end, true, Vec3.atLowerCornerOf(hit.getDirection().getNormal()));
            }

            points.add(next);
            pos = next;
            motion = motion.add(0.0D, -preview.gravity(), 0.0D).scale(preview.drag());
        }

        return new Trajectory(points, points.get(points.size() - 1), false, new Vec3(0, 1, 0));
    }

    private static void renderLandingMark(VertexConsumer consumer, PoseStack.Pose pose, Vec3 camera, Vec3 center,
                                          Vec3 normal, int alpha, int red, int green, int blue) {
        Vec3 axis = Math.abs(normal.y) > 0.9 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 u = normal.cross(axis).normalize().scale(0.38);
        Vec3 v = normal.cross(u).normalize().scale(0.38);
        Vec3 origin = center.add(normal.scale(0.008));
        for (int i = 0; i < 32; i++) {
            double a = i * Math.PI / 16.0, b = (i + 1) * Math.PI / 16.0;
            Vec3 p = origin.add(u.scale(Math.cos(a))).add(v.scale(Math.sin(a)));
            Vec3 q = origin.add(u.scale(Math.cos(b))).add(v.scale(Math.sin(b)));
            quad(consumer, pose, camera, origin, p, q, origin, red, green, blue, alpha);
        }
    }

    private static void renderTube(VertexConsumer consumer, PoseStack.Pose pose, Vec3 camera,
                                   List<Vec3> points, int red, int green, int blue) {
        Vec3[] previousRing = null;
        Vec3 side = null;
        for (int i = 0; i < points.size(); i++) {
            Vec3 center = points.get(i);
            Vec3 tangent = points.get(Math.min(i + 1, points.size() - 1))
                    .subtract(points.get(Math.max(0, i - 1)));
            if (tangent.lengthSqr() < 1.0E-10) {
                tangent = center.subtract(points.get(Math.max(0, i - 1)));
                if (tangent.lengthSqr() < 1.0E-10) continue;
            }
            tangent = tangent.normalize();
            // Transport the circular frame along the curve, independent of the camera.
            if (side != null) side = side.subtract(tangent.scale(side.dot(tangent)));
            if (side == null || side.lengthSqr() < 1.0E-10) {
                Vec3 axis = Math.abs(tangent.y) > 0.9 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
                side = tangent.cross(axis);
            }
            side = side.normalize();
            Vec3 up = tangent.cross(side).normalize();
            Vec3[] ring = new Vec3[TUBE_SIDES];
            for (int j = 0; j < TUBE_SIDES; j++) {
                double angle = j * Math.PI * 2.0 / TUBE_SIDES;
                ring[j] = center.add(side.scale(Math.cos(angle) * TUBE_RADIUS))
                        .add(up.scale(Math.sin(angle) * TUBE_RADIUS));
            }
            if (previousRing != null) {
                for (int j = 0; j < TUBE_SIDES; j++) {
                    int next = (j + 1) % TUBE_SIDES;
                    quad(consumer, pose, camera, previousRing[j], ring[j], ring[next], previousRing[next],
                            red, green, blue, TUBE_ALPHA);
                }
            }
            previousRing = ring;
        }
    }

    private static void renderLandingSphere(VertexConsumer consumer, PoseStack.Pose pose, Vec3 camera,
                                            Vec3 center, int red, int green, int blue, int alpha) {
        for (int latitude = 0; latitude < 8; latitude++) {
            double a = -Math.PI / 2.0 + latitude * Math.PI / 8.0;
            double b = -Math.PI / 2.0 + (latitude + 1) * Math.PI / 8.0;
            for (int longitude = 0; longitude < 16; longitude++) {
                double u = longitude * Math.PI / 8.0;
                double v = (longitude + 1) * Math.PI / 8.0;
                quad(consumer, pose, camera, spherePoint(center, a, u), spherePoint(center, b, u),
                        spherePoint(center, b, v), spherePoint(center, a, v), red, green, blue, alpha);
            }
        }
    }

    private static Vec3 spherePoint(Vec3 center, double latitude, double longitude) {
        double radius = Math.cos(latitude) * LANDING_RADIUS;
        return center.add(radius * Math.cos(longitude), Math.sin(latitude) * LANDING_RADIUS,
                radius * Math.sin(longitude));
    }

    private static void quad(VertexConsumer consumer, PoseStack.Pose pose, Vec3 camera,
                             Vec3 a, Vec3 b, Vec3 c, Vec3 d, int red, int green, int blue, int alpha) {
        vertex(consumer, pose, a.subtract(camera), red, green, blue, alpha);
        vertex(consumer, pose, b.subtract(camera), red, green, blue, alpha);
        vertex(consumer, pose, c.subtract(camera), red, green, blue, alpha);
        vertex(consumer, pose, d.subtract(camera), red, green, blue, alpha);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, Vec3 point,
                               int red, int green, int blue, int alpha) {
        consumer.addVertex(pose.pose(), (float) point.x, (float) point.y, (float) point.z)
                .setColor(red, green, blue, alpha);
    }

    private record Preview(Vec3 start, Vec3 velocity, double gravity, double drag, int maxTicks, int red, int green, int blue) {
    }

    private record Trajectory(List<Vec3> points, Vec3 end, boolean hit, Vec3 normal) {
    }
}
