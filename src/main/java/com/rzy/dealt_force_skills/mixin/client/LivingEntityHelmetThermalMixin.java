package com.rzy.dealt_force_skills.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.rzy.dealt_force_skills.client.HelmetVisionRenderer;
import com.tacz.guns.client.renderer.scope.ScopeThermalRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Some render optimizers call the living renderer directly, bypassing TaCZ's dispatcher hook. */
@Mixin(value = LivingEntityRenderer.class, remap = false)
public abstract class LivingEntityHelmetThermalMixin {
    @Inject(method = "isBodyVisible", at = @At("RETURN"), cancellable = true)
    private void dealtForceSkills$visibleHeat(LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
        if (HelmetVisionRenderer.reveals(entity)) cir.setReturnValue(true);
    }

    @ModifyVariable(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"), argsOnly = true)
    private MultiBufferSource dealtForceSkills$helmetHeat(MultiBufferSource source, LivingEntity entity,
                                                         float yaw, float partialTick, PoseStack pose,
                                                         MultiBufferSource original, int light) {
        return HelmetVisionRenderer.thermalCaptureActive() ? ScopeThermalRenderer.wrap(source, entity) : source;
    }
}
