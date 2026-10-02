package com.rzy.dealt_force_skills.client.visual;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.client.renderer.DfsRenderTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT)
public final class ManbaFlashlightBeamRenderer {
    private static final int RING_SEGMENTS = 32;
    private static final int RED = 255;
    private static final int GREEN = 235;
    private static final int BLUE = 155;
    private static final Map<Integer, Beam> BEAMS = new HashMap<>();

    private ManbaFlashlightBeamRenderer() {
    }

    public static void syncBeam(int entityId, boolean active, float range, float halfAngleDegrees, int ticks) {
        if (!active || ticks <= 0 || range <= 0.0F || !Float.isFinite(range) || !Float.isFinite(halfAngleDegrees)) {
            BEAMS.remove(entityId);
            return;
        }
        BEAMS.put(entityId, new Beam(range, halfAngleDegrees, ticks));
    }

    public static void tick(Minecraft minecraft) {
        if (minecraft.level == null) {
            BEAMS.clear();
            return;
        }
        Iterator<Map.Entry<Integer, Beam>> iterator = BEAMS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, Beam> entry = iterator.next();
            entry.getValue().ticks--;
            if (entry.getValue().ticks <= 0 || !(minecraft.level.getEntity(entry.getKey()) instanceof Player)) {
                iterator.remove();
            }
        }
    }

    public static void reset() {
        BEAMS.clear();
    }

    public static boolean isActive(Player player) {
        return player != null && BEAMS.containsKey(player.getId());
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || BEAMS.isEmpty()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }

        MultiBufferSource.BufferSource buffer = minecraft.renderBuffers().bufferSource();
        VertexConsumer sheetConsumer = buffer.getBuffer(DfsRenderTypes.untexturedQuads());
        PoseStack.Pose pose = event.getPoseStack().last();
        Matrix4f matrix = pose.pose();
        Vec3 camera = event.getCamera().getPosition();

        for (Map.Entry<Integer, Beam> entry : BEAMS.entrySet()) {
            Player player = playerForBeam(minecraft, entry.getKey());
            if (player != null) {
                BeamGeometry geometry = beamGeometry(player, entry.getValue(), com.rzy.dealt_force_skills.client.ClientRenderCompat.partialTick(event.getPartialTick()));
                if (geometry != null) {
                    renderBeamSheets(geometry, matrix, camera, sheetConsumer);
                }
            }
        }
        buffer.endBatch(DfsRenderTypes.untexturedQuads());

        VertexConsumer lineConsumer = buffer.getBuffer(RenderType.lines());
        for (Map.Entry<Integer, Beam> entry : BEAMS.entrySet()) {
            Player player = playerForBeam(minecraft, entry.getKey());
            if (player != null) {
                BeamGeometry geometry = beamGeometry(player, entry.getValue(), com.rzy.dealt_force_skills.client.ClientRenderCompat.partialTick(event.getPartialTick()));
                if (geometry != null) {
                    renderBeamLines(geometry, pose, camera, lineConsumer);
                }
            }
        }
        buffer.endBatch(RenderType.lines());
    }

    private static Player playerForBeam(Minecraft minecraft, int entityId) {
        if (minecraft.level == null) {
            return null;
        }
        Entity entity = minecraft.level.getEntity(entityId);
        return entity instanceof Player player ? player : null;
    }

    private static BeamGeometry beamGeometry(Player player, Beam beam, float partialTick) {
        if (beam.range <= 0.0F || !Float.isFinite(beam.range) || !Float.isFinite(beam.halfAngleDegrees)) {
            return null;
        }
        Vec3 look = player.getViewVector(partialTick).normalize();
        if (look.lengthSqr() < 1.0E-6D) {
            return null;
        }

        Vec3 up = new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 right = rightVector(look, up);
        Vec3 vertical = look.cross(right).normalize();
        Vec3 start = muzzle(player, partialTick);
        Vec3 end = start.add(look.scale(beam.range));
        double radius = Math.tan(Math.toRadians(beam.halfAngleDegrees)) * beam.range;
        if (!Double.isFinite(radius)) {
            return null;
        }
        double nearRadius = Math.max(0.08D, radius * 0.035D);
        if (!Double.isFinite(nearRadius)) {
            return null;
        }
        return new BeamGeometry(start, end, right, vertical, radius, nearRadius);
    }

    private static void renderBeamSheets(BeamGeometry geometry, Matrix4f matrix, Vec3 camera,
                                         VertexConsumer sheetConsumer) {
        double radius = geometry.radius();
        double nearRadius = geometry.nearRadius();

        renderSheet(sheetConsumer, matrix, camera, geometry.start(), geometry.end(),
                geometry.right(), nearRadius, radius, 42);
        renderSheet(sheetConsumer, matrix, camera, geometry.start(), geometry.end(),
                geometry.vertical(), nearRadius, radius, 36);
    }

    private static void renderBeamLines(BeamGeometry geometry, PoseStack.Pose pose, Vec3 camera,
                                        VertexConsumer lineConsumer) {
        Vec3 start = geometry.start();
        Vec3 end = geometry.end();
        Vec3 right = geometry.right();
        Vec3 vertical = geometry.vertical();
        double radius = geometry.radius();
        renderLine(lineConsumer, pose, camera, start, end, 205);
        Vec3 previous = ringPoint(end, right, vertical, radius, 0.0D);
        for (int i = 1; i <= RING_SEGMENTS; i++) {
            double angle = Math.PI * 2.0D * i / RING_SEGMENTS;
            Vec3 next = ringPoint(end, right, vertical, radius, angle);
            renderLine(lineConsumer, pose, camera, previous, next, 125);
            if (i % 4 == 0) {
                renderLine(lineConsumer, pose, camera, start, next, 95);
            }
            previous = next;
        }
        renderLine(lineConsumer, pose, camera, start, end.add(right.scale(radius)), 120);
        renderLine(lineConsumer, pose, camera, start, end.add(right.scale(-radius)), 120);
        renderLine(lineConsumer, pose, camera, start, end.add(vertical.scale(radius)), 120);
        renderLine(lineConsumer, pose, camera, start, end.add(vertical.scale(-radius)), 120);
    }

    public static Vec3 muzzle(Player player, float partialTick) {
        Vec3 look = player.getViewVector(partialTick).normalize();
        if (look.lengthSqr() < 1.0E-6D) {
            look = new Vec3(0.0D, 0.0D, 1.0D);
        }
        Vec3 up = new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 right = rightVector(look, up);
        Vec3 vertical = look.cross(right).normalize();
        double handSide = player.getMainArm() == HumanoidArm.LEFT ? -1.0D : 1.0D;
        return player.getEyePosition(partialTick)
                .add(look.scale(0.50D))
                .add(right.scale(-0.48D * handSide))
                .add(vertical.scale(-0.62D));
    }

    private static Vec3 rightVector(Vec3 look, Vec3 up) {
        Vec3 right = up.cross(look);
        if (right.lengthSqr() < 1.0E-6D) {
            return new Vec3(1.0D, 0.0D, 0.0D);
        }
        return right.normalize();
    }

    private static Vec3 ringPoint(Vec3 center, Vec3 right, Vec3 vertical, double radius, double angle) {
        return center.add(right.scale(Math.cos(angle) * radius)).add(vertical.scale(Math.sin(angle) * radius));
    }

    private static void renderSheet(VertexConsumer consumer, Matrix4f matrix, Vec3 camera,
                                    Vec3 start, Vec3 end, Vec3 axis, double nearRadius,
                                    double farRadius, int alpha) {
        Vec3 startA = start.add(axis.scale(nearRadius)).subtract(camera);
        Vec3 startB = start.add(axis.scale(-nearRadius)).subtract(camera);
        Vec3 endA = end.add(axis.scale(farRadius)).subtract(camera);
        Vec3 endB = end.add(axis.scale(-farRadius)).subtract(camera);
        renderQuad(consumer, matrix, startA, endA, endB, startB, alpha);
        renderQuad(consumer, matrix, startB, endB, endA, startA, alpha);
    }

    private static void renderQuad(VertexConsumer consumer, Matrix4f matrix,
                                   Vec3 a, Vec3 b, Vec3 c, Vec3 d, int alpha) {
        consumer.addVertex(matrix, (float) a.x, (float) a.y, (float) a.z).setColor(RED, GREEN, BLUE, alpha);
        consumer.addVertex(matrix, (float) b.x, (float) b.y, (float) b.z).setColor(RED, GREEN, BLUE, alpha);
        consumer.addVertex(matrix, (float) c.x, (float) c.y, (float) c.z).setColor(RED, GREEN, BLUE, alpha);
        consumer.addVertex(matrix, (float) d.x, (float) d.y, (float) d.z).setColor(RED, GREEN, BLUE, alpha);
    }

    private static void renderLine(VertexConsumer consumer, PoseStack.Pose pose, Vec3 camera,
                                   Vec3 from, Vec3 to, int alpha) {
        Vec3 a = from.subtract(camera);
        Vec3 b = to.subtract(camera);
        Vec3 normal = b.subtract(a);
        if (normal.lengthSqr() < 1.0E-6D) {
            normal = new Vec3(0.0D, 1.0D, 0.0D);
        } else {
            normal = normal.normalize();
        }
        consumer.addVertex(pose.pose(), (float) a.x, (float) a.y, (float) a.z)
                .setColor(RED, GREEN, BLUE, alpha)
                .setNormal(pose, (float) normal.x, (float) normal.y, (float) normal.z)
                ;
        consumer.addVertex(pose.pose(), (float) b.x, (float) b.y, (float) b.z)
                .setColor(RED, GREEN, BLUE, alpha)
                .setNormal(pose, (float) normal.x, (float) normal.y, (float) normal.z)
                ;
    }

    private static final class Beam {
        private final float range;
        private final float halfAngleDegrees;
        private int ticks;

        private Beam(float range, float halfAngleDegrees, int ticks) {
            this.range = range;
            this.halfAngleDegrees = halfAngleDegrees;
            this.ticks = ticks;
        }
    }

    private record BeamGeometry(Vec3 start, Vec3 end, Vec3 right, Vec3 vertical, double radius, double nearRadius) {
    }
}
