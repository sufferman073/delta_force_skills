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
import com.mojang.blaze3d.systems.RenderSystem;
import com.rzy.dealt_force_skills.client.renderer.BlockbenchAnimatedModelRenderer;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Vector3f;

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

    private static final Map<Integer, Matrix4f> WORLD_LENSES = new HashMap<>();
    private static HandJoin handJoin;
    private static final float LENS_FACE = -.32F / 16;
    private record HandJoin(int entityId, Matrix4f worldToClip, Vec3 center, Vec3 width, Vec3 height) {}

    static void renderLamp(Player player, ResourceLocation model, String clip, float seconds, PoseStack stack,
                           MultiBufferSource buffers, int light, boolean firstPerson) {
        if (!isActive(player)) {
            BlockbenchAnimatedModelRenderer.render(model,clip,seconds,stack,buffers,light);
            return;
        }
        BlockbenchAnimatedModelRenderer.renderWithGroupPlacement(model,clip,seconds,stack,buffers,light,"lens_core", () -> {
            if (firstPerson) {
                if (handJoin != null && handJoin.entityId == player.getId()) connectLens(stack,buffers,handJoin);
            } else WORLD_LENSES.put(player.getId(),new Matrix4f(stack.last().pose()));
        });
    }

    static Matrix4f joinToLens(Matrix4f handToClip, Matrix4f worldToClip) {
        return new Matrix4f(handToClip).invert().mul(worldToClip);
    }

    private static void connectLens(PoseStack stack, MultiBufferSource buffers, HandJoin join) {
        Matrix4f lens = stack.last().pose();
        Matrix4f toLens = joinToLens(new Matrix4f(RenderSystem.getProjectionMatrix())
                .mul(RenderSystem.getModelViewMatrix()).mul(lens), join.worldToClip);
        Vec3 start = new Vec3(0,0,LENS_FACE);
        var sheets = buffers.getBuffer(DfsRenderTypes.untexturedQuads());
        for (int index = 0; index < 2; index++) {
            Vec3 far = index == 0 ? join.width : join.height;
            Vec3 near = index == 0 ? new Vec3(.048,0,0) : new Vec3(0,.048,0);
            Vec3 outer = project(toLens,join.center.add(far)), inner = project(toLens,join.center.subtract(far));
            renderQuad(sheets,lens,start.add(near),outer,inner,start.subtract(near),index == 0 ? 42 : 36);
            renderQuad(sheets,lens,start.subtract(near),inner,outer,start.add(near),index == 0 ? 42 : 36);
        }
        renderLine(buffers.getBuffer(RenderType.lines()),stack.last(),Vec3.ZERO,start,project(toLens,join.center),205);
    }

    private static Vec3 project(Matrix4f matrix, Vec3 point) {
        Vector3f result = matrix.transformProject(point.toVector3f());
        return new Vec3(result.x,result.y,result.z);
    }

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
        WORLD_LENSES.clear(); handJoin = null;
    }

    public static boolean isActive(Player player) {
        return player != null && BEAMS.containsKey(player.getId());
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY) {
            WORLD_LENSES.clear(); handJoin = null;
        }
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || BEAMS.isEmpty()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }

        MultiBufferSource.BufferSource buffer = minecraft.renderBuffers().bufferSource();
        PoseStack.Pose pose = event.getPoseStack().last();
        Matrix4f matrix = pose.pose();
        Vec3 camera = event.getCamera().getPosition();
        Matrix4f worldToClip = new Matrix4f(RenderSystem.getProjectionMatrix()).mul(RenderSystem.getModelViewMatrix()).mul(matrix);
        Matrix4f toWorld = new Matrix4f(matrix).invert();
        var visible = new java.util.ArrayList<BeamGeometry>();
        for (var entry : BEAMS.entrySet()) {
            Player player = playerForBeam(minecraft, entry.getKey());
            if (player == null) continue;
            Beam beam = entry.getValue();
            float partial = com.rzy.dealt_force_skills.client.ClientRenderCompat.partialTick(event.getPartialTick());
            boolean hand = player == minecraft.getCameraEntity() && minecraft.options.getCameraType().isFirstPerson();
            Vec3 start, look;
            double range = beam.range;
            if (hand) {
                look = player.getViewVector(partial).normalize();
                double bridge = Math.min(1.65,range*.35);
                Vec3 right = rightVector(look,new Vec3(0,1,0));
                start = player.getEyePosition(partial).add(look.scale(bridge))
                        .add(right.scale(player.getMainArm() == HumanoidArm.LEFT ? .26 : -.26))
                        .add(look.cross(right).scale(-.19));
                range -= bridge;
            } else {
                Matrix4f lens = WORLD_LENSES.get(player.getId());
                if (lens == null) continue;
                Matrix4f placed = new Matrix4f(toWorld).mul(lens);
                start = camera.add(project(placed,new Vec3(0,0,LENS_FACE)));
                Vector3f direction = placed.transformDirection(0,0,-1,new Vector3f()).normalize();
                look = new Vec3(direction.x,direction.y,direction.z);
            }
            Vec3 right = rightVector(look,new Vec3(0,1,0)), up = look.cross(right).normalize();
            double radius = Math.tan(Math.toRadians(beam.halfAngleDegrees))*beam.range;
            double near = hand ? Math.min(.15,radius*.10) : .035;
            visible.add(new BeamGeometry(start,start.add(look.scale(range)),right,up,radius,near));
            if (hand) handJoin = new HandJoin(player.getId(),worldToClip,start.subtract(camera),right.scale(-near),up.scale(near));
        }
        var sheets = buffer.getBuffer(DfsRenderTypes.untexturedQuads());
        for (BeamGeometry geometry : visible) renderBeamSheets(geometry,matrix,camera,sheets);
        buffer.endBatch(DfsRenderTypes.untexturedQuads());
        var lines = buffer.getBuffer(RenderType.lines());
        for (BeamGeometry geometry : visible) renderBeamLines(geometry,pose,camera,lines);
        buffer.endBatch(RenderType.lines());
    }

    private static Player playerForBeam(Minecraft minecraft, int entityId) {
        if (minecraft.level == null) {
            return null;
        }
        Entity entity = minecraft.level.getEntity(entityId);
        return entity instanceof Player player ? player : null;
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
