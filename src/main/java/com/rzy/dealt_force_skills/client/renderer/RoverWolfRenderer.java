package com.rzy.dealt_force_skills.client.renderer;

import net.minecraft.client.renderer.entity.WolfRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.WolfCollarLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.model.WolfModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Wolf;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;

public final class RoverWolfRenderer extends WolfRenderer {
    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath("dealt_force_skills", "textures/entity/rover_wolf/" + name + ".png");
    }
    private static final ResourceLocation NORMAL = texture("wolf");
    private static final ResourceLocation TAME = texture("wolf_tame");
    private static final ResourceLocation ANGRY = texture("wolf_angry");
    private static final ResourceLocation COLLAR = texture("wolf_collar");

    public RoverWolfRenderer(EntityRendererProvider.Context context) {
        super(context);
        layers.removeIf(layer -> layer instanceof WolfCollarLayer);
        addLayer(new RenderLayer<Wolf, WolfModel<Wolf>>(this) {
            @Override public void render(PoseStack pose, MultiBufferSource buffers, int light, Wolf wolf,
                    float limb, float limbAmount, float partialTick, float age, float yaw, float pitch) {
                if (wolf.isTame() && !wolf.isInvisible()) {
                    var vertices = buffers.getBuffer(net.minecraft.client.renderer.RenderType.entityCutoutNoCull(COLLAR));
                    getParentModel().renderToBuffer(pose, vertices, light, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
                            wolf.getCollarColor().getTextureDiffuseColor());
                }
            }
        });
    }

    @Override public ResourceLocation getTextureLocation(Wolf wolf) {
        return wolf.isAngry() ? ANGRY : wolf.isTame() ? TAME : NORMAL;
    }
}
