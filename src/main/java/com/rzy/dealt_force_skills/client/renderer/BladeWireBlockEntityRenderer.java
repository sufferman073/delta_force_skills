package com.rzy.dealt_force_skills.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.block.BladeWireBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public final class BladeWireBlockEntityRenderer implements BlockEntityRenderer<BladeWireBlockEntity> {
    private static final ResourceLocation CORE =
            ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "sineva_blade_wire_core");
    private static final ResourceLocation WIRE =
            ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "sineva_perimeter_barbed_wire");

    public BladeWireBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
            BladeWireBlockEntity blockEntity,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay
    ) {
        boolean core = blockEntity.isCore();
        poseStack.pushPose();
        poseStack.translate(0.5D, 0.0D, 0.5D);
        boolean rendered = BlockbenchAnimatedModelRenderer.render(
                core ? CORE : WIRE,
                core ? "deploy" : null,
                core ? blockEntity.clientAgeSeconds(partialTick) : 0.0F,
                poseStack,
                buffer,
                packedLight
        );
        poseStack.popPose();

        if (!rendered) {
            renderFallback(core, poseStack, buffer);
        }
    }

    private static void renderFallback(boolean core, PoseStack poseStack, MultiBufferSource buffer) {
        float inset = core ? 0.08F : 0.02F;
        float top = core ? 0.32F : 0.10F;
        int r = core ? 44 : 74;
        int g = core ? 50 : 78;
        int b = core ? 58 : 84;
        VertexConsumer consumer = buffer.getBuffer(RenderType.debugQuads());

        poseStack.pushPose();
        box(poseStack, consumer, inset, 0.0F, inset, 1.0F - inset, top, 1.0F - inset, r, g, b);
        poseStack.popPose();
    }

    private static void box(
            PoseStack poseStack,
            VertexConsumer consumer,
            float x1,
            float y1,
            float z1,
            float x2,
            float y2,
            float z2,
            int r,
            int g,
            int b
    ) {
        quad(poseStack, consumer, x1, y1, z1, x2, y1, z1, x2, y2, z1, x1, y2, z1, r, g, b);
        quad(poseStack, consumer, x2, y1, z2, x1, y1, z2, x1, y2, z2, x2, y2, z2, r, g, b);
        quad(poseStack, consumer, x1, y1, z2, x1, y1, z1, x1, y2, z1, x1, y2, z2, r, g, b);
        quad(poseStack, consumer, x2, y1, z1, x2, y1, z2, x2, y2, z2, x2, y2, z1, r, g, b);
        quad(poseStack, consumer, x1, y2, z1, x2, y2, z1, x2, y2, z2, x1, y2, z2, r, g, b);
        quad(poseStack, consumer, x1, y1, z2, x2, y1, z2, x2, y1, z1, x1, y1, z1, r, g, b);
    }

    private static void quad(
            PoseStack poseStack,
            VertexConsumer consumer,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float x3, float y3, float z3,
            float x4, float y4, float z4,
            int r, int g, int b
    ) {
        consumer.addVertex(poseStack.last().pose(), x1, y1, z1).setColor(r, g, b, 255);
        consumer.addVertex(poseStack.last().pose(), x2, y2, z2).setColor(r, g, b, 255);
        consumer.addVertex(poseStack.last().pose(), x3, y3, z3).setColor(r, g, b, 255);
        consumer.addVertex(poseStack.last().pose(), x4, y4, z4).setColor(r, g, b, 255);
    }
}
