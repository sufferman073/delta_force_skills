package com.rzy.dealt_force_skills.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.entity.CatDadRoadTruckEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Matrix4f;

public class CatDadRoadTruckRenderer extends EntityRenderer<CatDadRoadTruckEntity> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "textures/entity/catdad_dayun.png");
    private static final double ROAD_SEGMENT_LENGTH = 4.0D;
    private static final double ROAD_LIFT = 0.09D;
    private static final double ROAD_EDGE_WIDTH = 0.35D;
    private static final double ROAD_CENTER_WIDTH = 0.28D;
    private static final double TRUCK_IMAGE_WIDTH = 16.0D;
    private static final double TRUCK_IMAGE_HEIGHT = 16.0D;
    private static final double TRUCK_IMAGE_BOTTOM = 0.05D;

    public CatDadRoadTruckRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(CatDadRoadTruckEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        VertexConsumer roadConsumer = buffer.getBuffer(RenderType.debugQuads());
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        Vec3 dir = entity.direction();
        Vec3 side = new Vec3(-dir.z, 0.0D, dir.x);
        Vec3 current = entity.position();
        renderRoad(roadConsumer, matrix, entity, current, dir, side);

        if (entity.roadAge() >= CatDadRoadTruckEntity.WARNING_TICKS) {
            VertexConsumer truckConsumer = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
            renderTruck(truckConsumer, matrix, pose, packedLight, dir, side);
        }
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(CatDadRoadTruckEntity entity) {
        return TEXTURE;
    }

    private static void renderTruck(VertexConsumer consumer, Matrix4f matrix, PoseStack.Pose pose,
                                    int packedLight, Vec3 dir, Vec3 side) {
        double halfWidth = TRUCK_IMAGE_WIDTH * 0.5D;
        double bottom = TRUCK_IMAGE_BOTTOM;
        double top = TRUCK_IMAGE_BOTTOM + TRUCK_IMAGE_HEIGHT;
        Vec3 leftBottom = side.scale(halfWidth).add(0.0D, bottom, 0.0D);
        Vec3 rightBottom = side.scale(-halfWidth).add(0.0D, bottom, 0.0D);
        Vec3 rightTop = side.scale(-halfWidth).add(0.0D, top, 0.0D);
        Vec3 leftTop = side.scale(halfWidth).add(0.0D, top, 0.0D);
        texturedQuad(consumer, matrix, pose, leftBottom, rightBottom, rightTop, leftTop,
                (float) dir.x, 0.0F, (float) dir.z, packedLight);
    }

    private static void renderRoad(VertexConsumer consumer, Matrix4f matrix, CatDadRoadTruckEntity entity,
                                   Vec3 current, Vec3 dir, Vec3 side) {
        boolean warning = entity.roadAge() < CatDadRoadTruckEntity.WARNING_TICKS;
        double pulse = warning ? (Math.sin(entity.roadAge() * 0.35D) + 1.0D) * 0.5D : 0.0D;
        int baseAlpha = warning ? 220 + (int) (28.0D * pulse) : 190;
        int markAlpha = warning ? 228 + (int) (27.0D * pulse) : 205;
        double halfWidth = entity.roadWidth() * 0.5D;
        int segments = Math.max(1, (int) Math.ceil(entity.length() / ROAD_SEGMENT_LENGTH));
        double segmentLength = entity.length() / segments;
        for (int i = 0; i < segments; i++) {
            double from = i * segmentLength;
            double to = Math.min(entity.length(), from + segmentLength);
            roadQuad(consumer, matrix, entity, current, dir, side, from, to,
                    -halfWidth, halfWidth, 36, 36, 32, baseAlpha);
            roadQuad(consumer, matrix, entity, current, dir, side, from, to,
                    -halfWidth, -halfWidth + ROAD_EDGE_WIDTH, 255, 205, 48, markAlpha);
            roadQuad(consumer, matrix, entity, current, dir, side, from, to,
                    halfWidth - ROAD_EDGE_WIDTH, halfWidth, 255, 205, 48, markAlpha);
            if (warning || i % 2 == 0) {
                roadQuad(consumer, matrix, entity, current, dir, side, from, to,
                        -ROAD_CENTER_WIDTH * 0.5D, ROAD_CENTER_WIDTH * 0.5D,
                        warning ? 255 : 230, warning ? 60 : 230, warning ? 38 : 210, markAlpha);
            }
        }
    }

    private static void roadQuad(VertexConsumer consumer, Matrix4f matrix, CatDadRoadTruckEntity entity,
                                 Vec3 current, Vec3 dir, Vec3 side,
                                 double from, double to, double left, double right,
                                 int r, int g, int bl, int alpha) {
        Vec3 a = roadPoint(entity, current, dir, side, from, left);
        Vec3 b = roadPoint(entity, current, dir, side, to, left);
        Vec3 c = roadPoint(entity, current, dir, side, to, right);
        Vec3 d = roadPoint(entity, current, dir, side, from, right);
        quad(consumer, matrix, a, b, c, d, r, g, bl, alpha);
    }

    private static Vec3 roadPoint(CatDadRoadTruckEntity entity, Vec3 current, Vec3 dir, Vec3 side,
                                  double along, double lateral) {
        Vec3 world = entity.start().add(dir.scale(along)).add(side.scale(lateral));
        double groundY = findGroundY(entity, world.x, world.z);
        return new Vec3(world.x - current.x, groundY - current.y, world.z - current.z);
    }

    private static double findGroundY(CatDadRoadTruckEntity entity, double x, double z) {
        Level level = entity.level();
        double baseY = entity.start().y;
        int startY = (int) Math.floor(baseY) + 4;
        int minY = Math.max(level.getMinBuildHeight(), (int) Math.floor(baseY) - 10);
        int blockX = (int) Math.floor(x);
        int blockZ = (int) Math.floor(z);
        for (int y = startY; y >= minY; y--) {
            BlockPos pos = new BlockPos(blockX, y, blockZ);
            VoxelShape shape = level.getBlockState(pos).getCollisionShape(level, pos);
            if (!shape.isEmpty()) {
                return pos.getY() + shape.max(Direction.Axis.Y) + ROAD_LIFT;
            }
        }
        return baseY + ROAD_LIFT;
    }

    private static void quad(VertexConsumer consumer, Matrix4f matrix, Vec3 a, Vec3 b, Vec3 c, Vec3 d,
                             int r, int g, int bl, int alpha) {
        consumer.addVertex(matrix, (float) a.x, (float) a.y, (float) a.z).setColor(r, g, bl, alpha);
        consumer.addVertex(matrix, (float) b.x, (float) b.y, (float) b.z).setColor(r, g, bl, alpha);
        consumer.addVertex(matrix, (float) c.x, (float) c.y, (float) c.z).setColor(r, g, bl, alpha);
        consumer.addVertex(matrix, (float) d.x, (float) d.y, (float) d.z).setColor(r, g, bl, alpha);
        consumer.addVertex(matrix, (float) d.x, (float) d.y, (float) d.z).setColor(r, g, bl, alpha);
        consumer.addVertex(matrix, (float) c.x, (float) c.y, (float) c.z).setColor(r, g, bl, alpha);
        consumer.addVertex(matrix, (float) b.x, (float) b.y, (float) b.z).setColor(r, g, bl, alpha);
        consumer.addVertex(matrix, (float) a.x, (float) a.y, (float) a.z).setColor(r, g, bl, alpha);
    }

    private static void texturedQuad(VertexConsumer consumer, Matrix4f matrix, PoseStack.Pose pose,
                                     Vec3 a, Vec3 b, Vec3 c, Vec3 d,
                                     float nx, float ny, float nz, int packedLight) {
        texturedVertex(consumer, matrix, pose, a, 0.0F, 1.0F, nx, ny, nz, packedLight);
        texturedVertex(consumer, matrix, pose, b, 1.0F, 1.0F, nx, ny, nz, packedLight);
        texturedVertex(consumer, matrix, pose, c, 1.0F, 0.0F, nx, ny, nz, packedLight);
        texturedVertex(consumer, matrix, pose, d, 0.0F, 0.0F, nx, ny, nz, packedLight);
    }

    private static void texturedVertex(VertexConsumer consumer, Matrix4f matrix, PoseStack.Pose pose,
                                       Vec3 point, float u, float v,
                                       float nx, float ny, float nz, int packedLight) {
        consumer.addVertex(matrix, (float) point.x, (float) point.y, (float) point.z)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(pose, nx, ny, nz)
                ;
    }
}
