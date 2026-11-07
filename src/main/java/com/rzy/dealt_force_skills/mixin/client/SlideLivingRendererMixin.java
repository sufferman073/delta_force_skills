package com.rzy.dealt_force_skills.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.rzy.dealt_force_skills.client.visual.SlideRenderState;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;

/** Also protect pose inputs when a render optimizer bypasses EntityRenderDispatcher. */
@Mixin(LivingEntityRenderer.class)
public abstract class SlideLivingRendererMixin {
    @WrapMethod(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V")
    private void dfs$directSlideRender(LivingEntity entity, float yaw, float partial, PoseStack stack,
                                      MultiBufferSource buffers, int light, Operation<Void> original) {
        SlideRenderState.render(entity, partial, () -> original.call(entity, yaw, partial, stack, buffers, light));
    }
}
