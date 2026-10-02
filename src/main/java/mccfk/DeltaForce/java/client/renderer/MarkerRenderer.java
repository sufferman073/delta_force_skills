package mccfk.DeltaForce.java.client.renderer;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.OptionalDouble;

import mccfk.DeltaForce.java.compat.SubLevelProjectionHelper;
import mccfk.DeltaForce.java.network.ClientMarkerStorage;
import mccfk.DeltaForce.java.network.Marker;
import mccfk.DeltaForce.java.network.MarkerType;

/**
 * 标点渲染器 - 在 RenderLevelStageEvent 中调用
 */
public class MarkerRenderer {

    private static final int ENTITY_LINE_COLOR = 0xFFFF0000;   // 红色（普通实体）
    private static final int CONTAINER_LINE_COLOR = 0xFFFFFF00; // 黄色
    private static final int POSITION_LINE_COLOR = 0xFF00FF00; // 绿色
    private static final float CIRCLE_RADIUS = 0.25F; // 小圆圈标记
    private static final double MARKER_HEIGHT = 3.5; // 3.5格高

    // 深度测试关闭的发光线条RenderType，穿透方块显示
    private static final RenderType GLOWING_LINES = RenderType.create(
        "glowing_lines",
        DefaultVertexFormat.POSITION_COLOR_NORMAL,
        VertexFormat.Mode.LINES,
        RenderType.TRANSIENT_BUFFER_SIZE,
        false,
        false,
        RenderType.CompositeState.builder()
            .setShaderState(RenderStateShard.RENDERTYPE_LINES_SHADER)
            .setDepthTestState(RenderStateShard.NO_DEPTH_TEST)
            .setTextureState(RenderStateShard.NO_TEXTURE)
            .setCullState(RenderStateShard.NO_CULL)
            .setLightmapState(RenderStateShard.NO_LIGHTMAP)
            .setWriteMaskState(RenderStateShard.COLOR_WRITE)
            .setLineState(new RenderStateShard.LineStateShard(OptionalDouble.of(5.0)))
            .createCompositeState(false)
    );

    /**
     * 渲染所有标点
     */
    public static void renderAllMarkers(PoseStack poseStack, Camera camera, float partialTick) {
        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer lines = bufferSource.getBuffer(GLOWING_LINES);
        for (Marker marker : ClientMarkerStorage.getAllMarkers()) {
            renderMarker(poseStack, camera, marker, lines);
        }
        bufferSource.endBatch(GLOWING_LINES);
    }

    private static void renderMarker(PoseStack poseStack, Camera camera, Marker marker, VertexConsumer lines) {
        BlockPos pos = marker.position();
        Vec3 cameraPos = camera.getPosition();

        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        switch (marker.type()) {
            case ENTITY -> renderEntityMarker(poseStack, marker, lines);
            case CONTAINER -> renderContainerMarker(poseStack, marker, lines);
            case POSITION -> renderPositionMarker(poseStack, pos, marker.precisePosition(), lines);
        }

        poseStack.popPose();
    }

    /**
     * 渲染实体标点：线 + 底部圆圈，优先使用精确实体坐标
     */
    private static void renderEntityMarker(PoseStack poseStack, Marker marker, VertexConsumer lines) {
        BlockPos pos = marker.position();
        double x, y, z;
        if (marker.precisePosition().isPresent()) {
            Vec3 p = marker.precisePosition().get();
            x = p.x;
            y = p.y;
            z = p.z;
        } else {
            x = pos.getX() + 0.5;
            y = pos.getY();
            z = pos.getZ() + 0.5;
        }

        // 绘制竖线
        drawVerticalLine(poseStack, lines, x, y, z, MARKER_HEIGHT, ENTITY_LINE_COLOR);
        // 绘制底部圆圈
        drawCircle(poseStack, lines, x, y + 0.01, z, CIRCLE_RADIUS, ENTITY_LINE_COLOR);
    }

    /**
     * 渲染容器标点：黄色AABB轮廓
     */
    private static void renderContainerMarker(PoseStack poseStack, Marker marker, VertexConsumer lines) {
        BlockPos pos = marker.position();

        // 检查是否为子世界容器：containerPos存在且不等于position，说明是plotgrid原始坐标
        if (marker.containerPos().isPresent() && !marker.containerPos().get().equals(pos)) {
            // 子世界容器：用plotgrid原始坐标精确计算世界空间AABB（处理旋转/分数偏移）
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null) {
                BlockPos plotgridPos = marker.containerPos().get();
                AABB aabb = SubLevelProjectionHelper.projectBlockAABBToGlobal(mc.level, plotgridPos);
                drawAABBOutline(poseStack, lines, aabb, CONTAINER_LINE_COLOR);
                return;
            }
        }

        // 普通容器：直接使用BlockPos绘制AABB
        AABB aabb = new AABB(pos);
        drawAABBOutline(poseStack, lines, aabb, CONTAINER_LINE_COLOR);
    }

    /**
     * 渲染位置标点：线 + 底部圆圈
     */
    private static void renderPositionMarker(PoseStack poseStack, BlockPos pos, java.util.Optional<Vec3> precisePos,
                                             VertexConsumer lines) {
        double x, y, z;
        if (precisePos.isPresent()) {
            Vec3 p = precisePos.get();
            x = p.x;
            y = p.y;
            z = p.z;
        } else {
            x = pos.getX() + 0.5;
            y = pos.getY();
            z = pos.getZ() + 0.5;
        }

        // 绘制竖线
        drawVerticalLine(poseStack, lines, x, y, z, MARKER_HEIGHT, POSITION_LINE_COLOR);
        // 绘制底部圆圈
        drawCircle(poseStack, lines, x, y + 0.01, z, CIRCLE_RADIUS, POSITION_LINE_COLOR);
    }

    /**
     * 绘制竖线
     */
    private static void drawVerticalLine(PoseStack poseStack, VertexConsumer consumer, double x, double y, double z,
                                         double height, int color) {
        Matrix4f matrix = poseStack.last().pose();
        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;
        float a = 1.0F;

        // LINES RenderType 需要 POSITION_COLOR_NORMAL 格式，必须提供法向量
        consumer.addVertex(matrix, (float) x, (float) y, (float) z).setColor(r, g, b, a).setNormal(0, 1, 0);
        consumer.addVertex(matrix, (float) x, (float) (y + height), (float) z).setColor(r, g, b, a).setNormal(0, 1, 0);

    }

    /**
     * 绘制水平圆圈
     */
    private static void drawCircle(PoseStack poseStack, VertexConsumer consumer, double centerX, double centerY,
                                   double centerZ, double radius, int color) {
        Matrix4f matrix = poseStack.last().pose();
        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;

        int segments = 32;
        for (int i = 0; i < segments; i++) {
            double angle1 = 2 * Math.PI * i / segments;
            double angle2 = 2 * Math.PI * (i + 1) / segments;
            addLine(consumer, matrix,
                    centerX + radius * Math.cos(angle1), centerY, centerZ + radius * Math.sin(angle1),
                    centerX + radius * Math.cos(angle2), centerY, centerZ + radius * Math.sin(angle2), r, g, b);
        }
    }

    /**
     * 绘制AABB线框
     */
    private static void drawAABBOutline(PoseStack poseStack, VertexConsumer consumer, AABB aabb, int color) {
        Matrix4f matrix = poseStack.last().pose();
        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;

        // 12条边
        addLine(consumer, matrix, aabb.minX, aabb.minY, aabb.minZ, aabb.maxX, aabb.minY, aabb.minZ, r, g, b);
        addLine(consumer, matrix, aabb.minX, aabb.minY, aabb.maxZ, aabb.maxX, aabb.minY, aabb.maxZ, r, g, b);
        addLine(consumer, matrix, aabb.minX, aabb.maxY, aabb.minZ, aabb.maxX, aabb.maxY, aabb.minZ, r, g, b);
        addLine(consumer, matrix, aabb.minX, aabb.maxY, aabb.maxZ, aabb.maxX, aabb.maxY, aabb.maxZ, r, g, b);
        addLine(consumer, matrix, aabb.minX, aabb.minY, aabb.minZ, aabb.minX, aabb.maxY, aabb.minZ, r, g, b);
        addLine(consumer, matrix, aabb.maxX, aabb.minY, aabb.minZ, aabb.maxX, aabb.maxY, aabb.minZ, r, g, b);
        addLine(consumer, matrix, aabb.minX, aabb.minY, aabb.maxZ, aabb.minX, aabb.maxY, aabb.maxZ, r, g, b);
        addLine(consumer, matrix, aabb.maxX, aabb.minY, aabb.maxZ, aabb.maxX, aabb.maxY, aabb.maxZ, r, g, b);
        addLine(consumer, matrix, aabb.minX, aabb.minY, aabb.minZ, aabb.minX, aabb.minY, aabb.maxZ, r, g, b);
        addLine(consumer, matrix, aabb.maxX, aabb.minY, aabb.minZ, aabb.maxX, aabb.minY, aabb.maxZ, r, g, b);
        addLine(consumer, matrix, aabb.minX, aabb.maxY, aabb.minZ, aabb.minX, aabb.maxY, aabb.maxZ, r, g, b);
        addLine(consumer, matrix, aabb.maxX, aabb.maxY, aabb.minZ, aabb.maxX, aabb.maxY, aabb.maxZ, r, g, b);

    }

    private static void addLine(VertexConsumer consumer, Matrix4f matrix, double x1, double y1, double z1, 
                                double x2, double y2, double z2, float r, float g, float b) {
        consumer.addVertex(matrix, (float) x1, (float) y1, (float) z1).setColor(r, g, b, 1.0F).setNormal(0, 1, 0);
        consumer.addVertex(matrix, (float) x2, (float) y2, (float) z2).setColor(r, g, b, 1.0F).setNormal(0, 1, 0);
    }
}