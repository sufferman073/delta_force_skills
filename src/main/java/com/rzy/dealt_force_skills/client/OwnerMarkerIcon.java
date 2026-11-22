package com.rzy.dealt_force_skills.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

final class OwnerMarkerIcon {
    private OwnerMarkerIcon() {}
    static void draw(PoseStack pose, MultiBufferSource buffers, String skill, double distance) {
        float half = (float)Math.max(5, Math.min(48, distance * .8));
        var vertices = buffers.getBuffer(RenderType.textSeeThrough(SkillIcons.skill(skill, SkillIcons.State.NORMAL)));
        corner(vertices, pose, -half, 11, 0, 0);
        corner(vertices, pose, -half, 11 + 2 * half, 0, 1);
        corner(vertices, pose, half, 11 + 2 * half, 1, 1);
        corner(vertices, pose, half, 11, 1, 0);
    }
    private static void corner(VertexConsumer vertices, PoseStack pose, float x, float y, float u, float v) {
        vertices.vertex(pose.last().pose(), x, y, 0).color(255,255,255,255).uv(u,v).uv2(LightTexture.FULL_BRIGHT).endVertex();
    }
}
