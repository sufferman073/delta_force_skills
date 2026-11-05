package com.rzy.dealt_force_skills.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.rzy.dealt_force_skills.client.visual.SlideRenderState;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

@Mixin(value = EntityRenderDispatcher.class, remap = false)
public abstract class SlideRenderDispatcherMixin {
    @WrapOperation(method = "render", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/client/renderer/entity/EntityRenderer;render(Lnet/minecraft/world/entity/Entity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"))
    private <E extends Entity> void dfs$slideAnimationInputs(EntityRenderer<E> renderer, E entity, float yaw,
            float partial, PoseStack stack, MultiBufferSource buffer, int light, Operation<Void> operation) {
        SlideRenderState.render(entity, partial, () -> operation.call(renderer, entity, yaw, partial, stack, buffer, light));
    }
}
