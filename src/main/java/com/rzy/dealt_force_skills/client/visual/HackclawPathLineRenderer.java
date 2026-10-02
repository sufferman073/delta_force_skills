package com.rzy.dealt_force_skills.client.visual;

import com.rzy.dealt_force_skills.config.DealtForceConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.hackclaw.HackclawTool;
import com.rzy.dealt_force_skills.client.character.ClientHackclawHudState;
import com.rzy.dealt_force_skills.network.S2C_HackclawPathLines;
import com.rzy.dealt_force_skills.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT)
public final class HackclawPathLineRenderer {
    private static volatile double MAX_LOCK_RANGE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("MAX_LOCK_RANGE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("client.visual.hackclaw_path_line_renderer.max_lock_range", 62.0));
    private static volatile double MIN_LOCK_ALIGNMENT = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("MIN_LOCK_ALIGNMENT", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("client.visual.hackclaw_path_line_renderer.min_lock_alignment", 0.93));
    private static volatile double MAX_LOCK_OFF_AXIS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("MAX_LOCK_OFF_AXIS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("client.visual.hackclaw_path_line_renderer.max_lock_off_axis", 0.9));
    private static final int LOCK_SAMPLES = 18;
    private static final double MAX_LINE_LENGTH = 128.0D;
    private static final double MAX_LINE_LENGTH_SQR = MAX_LINE_LENGTH * MAX_LINE_LENGTH;
    private static final List<ClientLine> LINES = new ArrayList<>();
    private static int highlightedTargetId = -1;

    private HackclawPathLineRenderer() {
    }

    public static void setLines(List<S2C_HackclawPathLines.Line> lines) {
        LINES.clear();
        highlightedTargetId = -1;
        for (S2C_HackclawPathLines.Line line : lines) {
            if (LINES.size() >= S2C_HackclawPathLines.MAX_LINES) {
                break;
            }
            if (line == null || !isFinite(line.from()) || !isFinite(line.to())) {
                continue;
            }
            LINES.add(new ClientLine(line.sourceEntityId(), line.targetEntityId(), line.from(), line.to(), line.primary(),
                    Math.min(S2C_HackclawPathLines.MAX_LINE_TICKS, Math.max(1, line.ticks()))));
        }
    }

    public static void clear() {
        LINES.clear();
        highlightedTargetId = -1;
    }

    public static void tick(Minecraft minecraft) {
        if (LINES.isEmpty()) {
            highlightedTargetId = -1;
            return;
        }
        Iterator<ClientLine> iterator = LINES.iterator();
        while (iterator.hasNext()) {
            ClientLine line = iterator.next();
            line.ticks--;
            if (line.ticks <= 0) {
                iterator.remove();
            }
        }
        int previous = highlightedTargetId;
        highlightedTargetId = findHighlightedTarget(minecraft);
        if (highlightedTargetId >= 0 && highlightedTargetId != previous && minecraft.player != null) {
            minecraft.player.playSound(ModSounds.HACKCLAW_FLASH_DRONE_PATH_LOCK.get(), 0.55f, 1.0f);
        }
    }

    public static int highlightedTargetId() {
        return highlightedTargetId;
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || LINES.isEmpty()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.screen != null
                || !ClientHackclawHudState.shouldRender()) {
            return;
        }

        MultiBufferSource.BufferSource buffer = minecraft.renderBuffers().bufferSource();
        VertexConsumer consumer = buffer.getBuffer(RenderType.lines());
        PoseStack.Pose pose = event.getPoseStack().last();
        Vec3 camera = event.getCamera().getPosition();

        for (ClientLine line : LINES) {
            int red;
            int green;
            int blue;
            if (line.targetEntityId == highlightedTargetId) {
                red = 255;
                green = 132;
                blue = 46;
            } else if (line.primary) {
                red = 255;
                green = 62;
                blue = 62;
            } else {
                red = 255;
                green = 224;
                blue = 86;
            }
            Vec3 from = line.from(minecraft);
            Vec3 to = line.to(minecraft);
            if (isRenderableLine(camera, from, to)) {
                renderLine(consumer, pose, camera, from, to, red, green, blue, 225);
            }
        }
        buffer.endBatch(RenderType.lines());
    }

    private static int findHighlightedTarget(Minecraft minecraft) {
        if (minecraft.player == null || !ClientHackclawHudState.shouldRender()
                || ClientHackclawHudState.equippedTool() != HackclawTool.FLASH_DRONE) {
            return -1;
        }

        Vec3 eye = minecraft.player.getEyePosition();
        Vec3 look = minecraft.player.getLookAngle().normalize();
        int bestTarget = -1;
        double bestOffAxis = Double.MAX_VALUE;
        for (ClientLine line : LINES) {
            double offAxis = sampledRayDistance(eye, look, line.from(minecraft), line.to(minecraft));
            if (offAxis < bestOffAxis) {
                bestOffAxis = offAxis;
                bestTarget = line.targetEntityId;
            }
        }
        return bestOffAxis <= MAX_LOCK_OFF_AXIS ? bestTarget : -1;
    }

    private static double sampledRayDistance(Vec3 eye, Vec3 look, Vec3 from, Vec3 to) {
        if (!isFinite(eye) || !isFinite(look) || !isRenderableLine(Vec3.ZERO, from, to)) {
            return Double.MAX_VALUE;
        }
        double best = Double.MAX_VALUE;
        for (int i = 0; i <= LOCK_SAMPLES; i++) {
            double t = i / (double) LOCK_SAMPLES;
            Vec3 point = from.lerp(to, t);
            Vec3 offset = point.subtract(eye);
            double distance = offset.length();
            if (distance <= 0.001D || distance > MAX_LOCK_RANGE) {
                continue;
            }
            double alignment = look.dot(offset.scale(1.0D / distance));
            if (alignment < MIN_LOCK_ALIGNMENT) {
                continue;
            }
            double offAxis = Math.sqrt(Math.max(0.0D, 1.0D - alignment * alignment)) * distance;
            best = Math.min(best, offAxis);
        }
        return best;
    }

    private static void renderLine(VertexConsumer consumer, PoseStack.Pose pose, Vec3 camera,
                                   Vec3 from, Vec3 to, int red, int green, int blue, int alpha) {
        Vec3 a = from.subtract(camera);
        Vec3 b = to.subtract(camera);
        Vec3 normal = b.subtract(a);
        if (normal.lengthSqr() < 1.0E-6D) {
            normal = new Vec3(0.0D, 1.0D, 0.0D);
        } else {
            normal = normal.normalize();
        }
        consumer.addVertex(pose.pose(), (float) a.x, (float) a.y, (float) a.z)
                .setColor(red, green, blue, alpha)
                .setNormal(pose, (float) normal.x, (float) normal.y, (float) normal.z)
                ;
        consumer.addVertex(pose.pose(), (float) b.x, (float) b.y, (float) b.z)
                .setColor(red, green, blue, alpha)
                .setNormal(pose, (float) normal.x, (float) normal.y, (float) normal.z)
                ;
    }

    private static boolean isRenderableLine(Vec3 camera, Vec3 from, Vec3 to) {
        return isFinite(camera)
                && isFinite(from)
                && isFinite(to)
                && from.distanceToSqr(to) <= MAX_LINE_LENGTH_SQR;
    }

    private static boolean isFinite(Vec3 vec) {
        return vec != null
                && Double.isFinite(vec.x)
                && Double.isFinite(vec.y)
                && Double.isFinite(vec.z);
    }

    private static final class ClientLine {
        private final int sourceEntityId;
        private final int targetEntityId;
        private final Vec3 fallbackFrom;
        private final Vec3 fallbackTo;
        private final boolean primary;
        private int ticks;

        private ClientLine(int sourceEntityId, int targetEntityId, Vec3 from, Vec3 to, boolean primary, int ticks) {
            this.sourceEntityId = sourceEntityId;
            this.targetEntityId = targetEntityId;
            this.fallbackFrom = from;
            this.fallbackTo = to;
            this.primary = primary;
            this.ticks = ticks;
        }

        private Vec3 from(Minecraft minecraft) {
            Entity entity = minecraft.level == null ? null : minecraft.level.getEntity(sourceEntityId);
            return entity == null ? fallbackFrom : entity.position().add(0.0D, entity.getBbHeight() * 0.55D, 0.0D);
        }

        private Vec3 to(Minecraft minecraft) {
            Entity entity = minecraft.level == null ? null : minecraft.level.getEntity(targetEntityId);
            return entity == null ? fallbackTo : entity.position().add(0.0D, entity.getBbHeight() * 0.55D, 0.0D);
        }
    }
}
