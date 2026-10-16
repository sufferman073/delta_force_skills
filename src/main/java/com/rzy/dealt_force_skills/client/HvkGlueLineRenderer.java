package com.rzy.dealt_force_skills.client;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT)
public final class HvkGlueLineRenderer {
    private HvkGlueLineRenderer() { }
    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        Minecraft client = Minecraft.getInstance();
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || client.level == null) return;
        long[] pairs = HvkGlueClientCache.links(client.level.dimension());
        if (pairs.length == 0) return;
        var buffers = client.renderBuffers().bufferSource();
        var vertices = buffers.getBuffer(RenderType.lines());
        var pose = event.getPoseStack().last();
        Vec3 camera = event.getCamera().getPosition();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            BlockPos start = BlockPos.of(pairs[i]), end = BlockPos.of(pairs[i + 1]);
            boolean restricted = HvkGlueClientCache.blocksSkills(client.level.dimension(), start);
            int greenBlue = restricted ? 166 : 220;
            Vec3 a = Vec3.atCenterOf(start).subtract(camera);
            Vec3 b = Vec3.atCenterOf(end).subtract(camera);
            Vec3 normal = b.subtract(a).normalize();
            vertices.addVertex(pose.pose(), (float) a.x, (float) a.y, (float) a.z).setColor(220, greenBlue, greenBlue, 255)
                    .setNormal(pose, (float) normal.x, (float) normal.y, (float) normal.z);
            vertices.addVertex(pose.pose(), (float) b.x, (float) b.y, (float) b.z).setColor(220, greenBlue, greenBlue, 255)
                    .setNormal(pose, (float) normal.x, (float) normal.y, (float) normal.z);
        }
        buffers.endBatch(RenderType.lines());
    }
}
