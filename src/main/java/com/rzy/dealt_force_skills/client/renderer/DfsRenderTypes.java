package com.rzy.dealt_force_skills.client.renderer;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;

import java.util.OptionalDouble;

public final class DfsRenderTypes extends RenderType {
    private static final RenderType UNTEXTURED_QUADS = RenderType.create(
            "dfs_untextured_quads",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS,
            256,
            false,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(POSITION_COLOR_SHADER)
                    .setTextureState(NO_TEXTURE)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .createCompositeState(false)
    );
    private static final RenderType TRAJECTORY_QUADS = RenderType.create(
            "dfs_trajectory_quads", DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS,
            4096, false, true, RenderType.CompositeState.builder()
                    .setShaderState(POSITION_COLOR_SHADER).setTextureState(NO_TEXTURE)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY).setCullState(NO_CULL)
                    .setWriteMaskState(COLOR_WRITE).createCompositeState(false));

    public static RenderType trajectoryQuads() { return TRAJECTORY_QUADS; }

    static RenderType glassPane(net.minecraft.resources.ResourceLocation texture) {
        return create("dfs_glass_pane", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, false, true,
                CompositeState.builder().setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
                        .setTextureState(new TextureStateShard(texture, false, false))
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY).setCullState(NO_CULL)
                        .setLightmapState(LIGHTMAP).setOverlayState(OVERLAY).setWriteMaskState(COLOR_WRITE)
                        .createCompositeState(false));
    }

    private static final RenderType EQUIPMENT_SOLID_QUADS = RenderType.create(
            "dfs_equipment_solid_quads",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS,
            256,
            false,
            false,
            RenderType.CompositeState.builder()
                    .setShaderState(POSITION_COLOR_SHADER)
                    .setTextureState(NO_TEXTURE)
                    .setTransparencyState(NO_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .createCompositeState(false)
    );
    private static final RenderType XRAY_LINES = RenderType.create(
            "dfs_xray_lines",
            DefaultVertexFormat.POSITION_COLOR_NORMAL,
            VertexFormat.Mode.LINES,
            RenderType.TRANSIENT_BUFFER_SIZE,
            false,
            false,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderType.RENDERTYPE_LINES_SHADER)
                    .setLineState(new RenderType.LineStateShard(OptionalDouble.empty()))
                    .setLayeringState(RenderType.VIEW_OFFSET_Z_LAYERING)
                    .setTransparencyState(RenderType.TRANSLUCENT_TRANSPARENCY)
                    .setDepthTestState(RenderType.NO_DEPTH_TEST)
                    .setWriteMaskState(RenderType.COLOR_WRITE)
                    .setCullState(RenderType.NO_CULL)
                    .createCompositeState(false)
    );

    private DfsRenderTypes(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize,
                           boolean affectsCrumbling, boolean sortOnUpload,
                           Runnable setupState, Runnable clearState) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
    }

    public static RenderType untexturedQuads() {
        return UNTEXTURED_QUADS;
    }

    public static RenderType equipmentSolidQuads() {
        return EQUIPMENT_SOLID_QUADS;
    }

    public static RenderType xrayLines() {
        return XRAY_LINES;
    }
}
