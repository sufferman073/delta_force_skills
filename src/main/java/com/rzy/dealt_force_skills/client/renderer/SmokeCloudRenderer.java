package com.rzy.dealt_force_skills.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.rzy.dealt_force_skills.entity.SmokeCloudModelState;
import com.rzy.dealt_force_skills.util.ClientVisionHooks;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import net.minecraftforge.client.model.pipeline.VertexConsumerWrapper;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class SmokeCloudRenderer<T extends Entity & SmokeCloudModelState> extends EntityRenderer<T> {
    private static final int BURST_TICKS = 13;
    private static final int END_TICKS = 12;
    private static final float BILLOW_PERIOD_SECONDS = 4.0F;
    private static final String BURST_ANIMATION = "\u70b8\u5f00";
    private static final String SUSTAIN_ANIMATION = "\u6301\u7eed";
    private static final String END_ANIMATION = "\u7ed3\u675f";
    private static final int CANOPY_COUNT = 8;
    private static final int BANK_COUNT = 20;
    private static final String[] SHEET_GROUPS = sheetGroups();
    private static final Set<String> HIDE_SHEETS = Set.of("vapor_sheets");
    private static final Set<String> HIDE_CORE = Set.of("occluding_body");
    private final ResourceLocation model;
    private final float scale;

    private static String[] sheetGroups() {
        String[] names = new String[CANOPY_COUNT + BANK_COUNT];
        for (int index = 0; index < names.length; index++) {
            names[index] = index < CANOPY_COUNT ? "smoke_puff_uv" + index : "smoke_bank_" + (index - CANOPY_COUNT);
        }
        return names;
    }

    public SmokeCloudRenderer(EntityRendererProvider.Context context, ResourceLocation model, float scale) {
        super(context);
        this.model = model;
        this.scale = scale;
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(T entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        if (com.rzy.dealt_force_skills.client.SmokeWorldRenderer.drawInEntityPass(entity)) {
            renderWorld(entity, partialTick, poseStack, buffer);
        }
    }

    public void renderWorld(Entity entity, float partialTick, PoseStack poseStack, MultiBufferSource buffer) {
        if (!(entity instanceof SmokeCloudModelState smoke)
                || ClientVisionHooks.isThermalVisionActive() || !smoke.shouldRenderSmokeModel()) {
            return;
        }
        float ageTicks = entity.tickCount + partialTick;
        int lifetime = Math.max(1, smoke.smokeLifetimeTicks());
        float burstTicks = Math.min(BURST_TICKS, lifetime * (BURST_TICKS / (float) (BURST_TICKS + END_TICKS)));
        float endTicks = Math.min(END_TICKS, lifetime - burstTicks);
        float endStart = lifetime - endTicks;
        String animation;
        float seconds;
        if (ageTicks < burstTicks) {
            animation = BURST_ANIMATION;
            seconds = ageTicks / burstTicks * (BURST_TICKS / 20.0F);
        } else if (ageTicks >= endStart) {
            animation = END_ANIMATION;
            seconds = (ageTicks - endStart) / endTicks * (END_TICKS / 20.0F);
        } else {
            animation = SUSTAIN_ANIMATION;
            float sustainTicks = endStart - burstTicks;
            float progress = (ageTicks - burstTicks) / sustainTicks;
            float envelope = 4.0F * progress * (1.0F - progress);
            float lane = ((entity.getId() * 5 & 15) - 7.5F) / 7.5F;
            int cycles = Math.round(sustainTicks / (20.0F * BILLOW_PERIOD_SECONDS));
            seconds = cycles == 0
                    ? (0.25F + 0.10F * lane) * sustainTicks / 20.0F * envelope
                    : (cycles * progress + 0.20F * lane * envelope * envelope) * BILLOW_PERIOD_SECONDS;
        }

        poseStack.pushPose();
        poseStack.scale(scale, scale, scale);
        try {
            var atlas = BlockbenchAnimatedModelRenderer.textureLocation(model);
            VertexConsumer core = new VertexConsumerWrapper(buffer.getBuffer(RenderType.entityCutoutNoCull(atlas))) {
                @Override
                public VertexConsumer normal(float x, float y, float z) {
                    return super.normal(0.0F, 1.0F, 0.0F);
                }
            };
            BlockbenchAnimatedModelRenderer.renderWithGroupReplacements(model, animation, seconds,
                    poseStack, ignored -> core, LightTexture.FULL_BRIGHT, HIDE_SHEETS, Map.of());

            VertexConsumer vapor = buffer.getBuffer(RenderType.entityTranslucent(atlas));
            var camera = entityRenderDispatcher.cameraOrientation();
            Map<String, Runnable> puffs = new HashMap<>(40);
            for (int index = 0; index < SHEET_GROUPS.length; index++) {
                int tile = index < CANOPY_COUNT ? index : CANOPY_COUNT + (index - CANOPY_COUNT) % 4;
                puffs.put(SHEET_GROUPS[index], () -> {
                    poseStack.mulPose(camera);
                    drawPuff(poseStack.last(), vapor, tile);
                });
            }
            BlockbenchAnimatedModelRenderer.renderWithGroupReplacements(model, animation, seconds,
                    poseStack, buffer, LightTexture.FULL_BRIGHT, HIDE_CORE, puffs);
        } finally {
            poseStack.popPose();
        }
    }

    private static void drawPuff(PoseStack.Pose pose, VertexConsumer vertices, int slot) {
        float left = (slot % 4) * 0.25F;
        float top = (1 + slot / 4) * 0.25F;
        float radius = 1.0F / 16.0F;
        puffVertex(pose, vertices, -radius, -radius, left, top + 0.25F);
        puffVertex(pose, vertices, radius, -radius, left + 0.25F, top + 0.25F);
        puffVertex(pose, vertices, radius, radius, left + 0.25F, top);
        puffVertex(pose, vertices, -radius, radius, left, top);
    }

    private static void puffVertex(PoseStack.Pose pose, VertexConsumer vertices,
                                   float x, float y, float u, float v) {
        vertices.vertex(pose.pose(), x, y, 0.0F).color(255, 255, 255, 255).uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                .normal(0.0F, 1.0F, 0.0F).endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return BlockbenchAnimatedModelRenderer.textureLocation(model);
    }
}
