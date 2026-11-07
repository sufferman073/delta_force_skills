package com.rzy.dealt_force_skills.client;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.rzy.dealt_force_skills.item.DfsEquipmentItem;
import com.rzy.dealt_force_skills.mixin.tacz.ScopeLensShaderAccessor;
import com.tacz.guns.client.renderer.scope.ScopeThermalRenderer;
import com.tacz.guns.client.renderer.scope.ScopeVisionMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

/** Full-screen adapter for the shader owned and reloaded by TaCZ. */
public final class HelmetVisionRenderer {
    private static final long START_TIME = System.nanoTime();
    private static TextureTarget scene;

    private HelmetVisionRenderer() {
    }

    private static int mode() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null && mc.player != null && mc.getCameraEntity() == mc.player
                ? DfsEquipmentItem.activeVisionMode(mc.player) : DfsEquipmentItem.VISION_OFF;
    }

    public static boolean thermalCaptureActive() {
        return mode() == DfsEquipmentItem.VISION_THERMAL && ScopeLensShaderAccessor.dealtForceSkills$shader() != null;
    }

    /** Only the viewing client reveals heat; authoritative invisibility stays untouched. */
    public static boolean reveals(net.minecraft.world.entity.LivingEntity entity) {
        return entity.isAlive() && !entity.isSpectator()
                && entity != Minecraft.getInstance().getCameraEntity() && thermalCaptureActive();
    }

    public static void close() {
        if (scene == null) return;
        if (!RenderSystem.isOnRenderThread()) {
            RenderSystem.recordRenderCall(HelmetVisionRenderer::close);
            return;
        }
        try (PassState ignored = new PassState()) {
            scene.destroyBuffers();
            scene = null;
        }
    }

    public static void render() {
        int mode = mode();
        if (mode != DfsEquipmentItem.VISION_NIGHT && mode != DfsEquipmentItem.VISION_THERMAL) {
            close();
            return;
        }
        ShaderInstance shader = ScopeLensShaderAccessor.dealtForceSkills$shader();
        if (shader == null) return;
        Minecraft mc = Minecraft.getInstance();
        var main = mc.getMainRenderTarget();
        int width = main.width;
        int height = main.height;
        if (width <= 0 || height <= 0) return;

        try (PassState ignored = new PassState()) {
            GlStateManager._activeTexture(GL13.GL_TEXTURE0);
            if (scene == null) scene = new TextureTarget(width, height, false, Minecraft.ON_OSX);
            else if (scene.width != width || scene.height != height) scene.resize(width, height, Minecraft.ON_OSX);

            // Sample a separate texture: never read from the colour attachment being written.
            RenderSystem.disableScissor();
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, main.frameBufferId);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, scene.frameBufferId);
            GL30.glBlitFramebuffer(0, 0, width, height, 0, 0, width, height, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
            main.bindWrite(true);
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.disableBlend();
            RenderSystem.disableCull();
            GL11.glDisable(GL11.GL_STENCIL_TEST);
            RenderSystem.colorMask(true, true, true, true);
            RenderSystem.setShader(() -> shader);
            RenderSystem.setShaderTexture(0, scene.getColorTextureId());
            RenderSystem.setShaderTexture(1, ScopeThermalRenderer.textureOr(scene.getColorTextureId(), width, height));
            RenderSystem.setShaderTexture(2, scene.getColorTextureId());
            shader.safeGetUniform("ScreenSize").set((float) width, (float) height);
            shader.safeGetUniform("LensCenter").set(0.5F, 0.5F);
            shader.safeGetUniform("LensRadius").set(0.5F, 0.5F);
            shader.safeGetUniform("ContourReady").set(0);
            shader.safeGetUniform("Distortion").set(0.0F);
            shader.safeGetUniform("Aberration").set(0.0F);
            shader.safeGetUniform("EdgeWidth").set(0.0F);
            shader.safeGetUniform("VignetteStrength").set(0.0F);
            shader.safeGetUniform("VignetteWidth").set(0.0F);
            shader.safeGetUniform("LensTint").set(1.0F, 1.0F, 1.0F);
            shader.safeGetUniform("VisionMode").set((mode == DfsEquipmentItem.VISION_NIGHT
                    ? ScopeVisionMode.NIGHT_VISION_GREEN : ScopeVisionMode.THERMAL_WHITE_HOT).ordinal());
            shader.safeGetUniform("ThermalReady").set(ScopeThermalRenderer.isReady(width, height) ? 1 : 0);
            shader.safeGetUniform("VisionGain").set(3.0F);
            shader.safeGetUniform("VisionNoise").set(0.025F);
            shader.safeGetUniform("SensorTime").set((System.nanoTime() - START_TIME) / 1_000_000_000.0F);

            RenderSystem.backupProjectionMatrix();
            var modelView = RenderSystem.getModelViewStack();
            modelView.pushPose();
            try {
                RenderSystem.setProjectionMatrix(new Matrix4f(), VertexSorting.ORTHOGRAPHIC_Z);
                modelView.setIdentity();
                RenderSystem.applyModelViewMatrix();
                BufferBuilder vertices = Tesselator.getInstance().getBuilder();
                vertices.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
                vertices.vertex(-1, -1, 0).endVertex();
                vertices.vertex(1, -1, 0).endVertex();
                vertices.vertex(1, 1, 0).endVertex();
                vertices.vertex(-1, 1, 0).endVertex();
                BufferUploader.drawWithShader(vertices.end());
            } finally {
                modelView.popPose();
                RenderSystem.applyModelViewMatrix();
                RenderSystem.restoreProjectionMatrix();
            }
        }
    }

    /** Restore only the GL state touched by this pass, including Minecraft's texture/shader caches. */
    private static final class PassState implements AutoCloseable {
        private final int readFramebuffer = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        private final int drawFramebuffer = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        private final int program = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        private final int activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        private final int[] viewport = new int[4];
        private final int[] scissorBox = new int[4];
        private final int[] textures = new int[3];
        private final int[] shaderTextures = new int[3];
        private final int[] colorMask = new int[4];
        private final boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        private final boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
        private final boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        private final boolean scissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        private final boolean stencil = GL11.glIsEnabled(GL11.GL_STENCIL_TEST);
        private final boolean depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        private final ShaderInstance previousShader = RenderSystem.getShader();

        private PassState() {
            GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
            GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX, scissorBox);
            GL11.glGetIntegerv(GL11.GL_COLOR_WRITEMASK, colorMask);
            for (int i = 0; i < textures.length; i++) {
                shaderTextures[i] = RenderSystem.getShaderTexture(i);
                GlStateManager._activeTexture(GL13.GL_TEXTURE0 + i);
                textures[i] = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            }
            GlStateManager._activeTexture(activeTexture);
        }

        @Override
        public void close() {
            // ShaderInstance caches blend modes separately from GL. Prime the previous mode
            // before restoring GL so the first HUD shader preserves vignette blending.
            if (previousShader != null) {
                previousShader.apply();
                previousShader.clear();
            }
            RenderSystem.setShader(() -> previousShader);
            GlStateManager._glUseProgram(program);
            for (int i = 0; i < textures.length; i++) {
                RenderSystem.setShaderTexture(i, shaderTextures[i]);
                GlStateManager._activeTexture(GL13.GL_TEXTURE0 + i);
                GlStateManager._bindTexture(textures[i]);
            }
            GlStateManager._activeTexture(activeTexture);
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readFramebuffer);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, drawFramebuffer);
            RenderSystem.viewport(viewport[0], viewport[1], viewport[2], viewport[3]);
            if (depth) RenderSystem.enableDepthTest(); else RenderSystem.disableDepthTest();
            if (blend) RenderSystem.enableBlend(); else RenderSystem.disableBlend();
            if (cull) RenderSystem.enableCull(); else RenderSystem.disableCull();
            if (scissor) RenderSystem.enableScissor(scissorBox[0], scissorBox[1], scissorBox[2], scissorBox[3]);
            else RenderSystem.disableScissor();
            if (stencil) GL11.glEnable(GL11.GL_STENCIL_TEST); else GL11.glDisable(GL11.GL_STENCIL_TEST);
            RenderSystem.depthMask(depthMask);
            RenderSystem.colorMask(colorMask[0] != 0, colorMask[1] != 0, colorMask[2] != 0, colorMask[3] != 0);
        }
    }
}
