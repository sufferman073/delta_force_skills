package com.rzy.dealt_force_skills.mixin.tacz;

import com.rzy.dealt_force_skills.client.HelmetVisionRenderer;
import com.rzy.dealt_force_skills.entity.SmokeCloudModelState;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.tacz.guns.client.renderer.scope.ScopeThermalRenderer;
import com.tacz.guns.compat.oculus.OculusCompat;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ScopeThermalRenderer.class, remap = false)
public abstract class HelmetThermalCaptureMixin {
    @Shadow
    private static ShaderInstance shader;

    @Inject(method = "enabled", at = @At("HEAD"), cancellable = true)
    private static void dealtForceSkills$helmetHeatCapture(CallbackInfoReturnable<Boolean> cir) {
        if (HelmetVisionRenderer.thermalCaptureActive()) {
            // A helmet sensor does not require a gun, aiming, a visible HUD or enabled scope effects.
            cir.setReturnValue(shader != null && !OculusCompat.isRenderShadow());
        }
    }

    @Inject(method = "wrap", at = @At("HEAD"), cancellable = true)
    private static void dealtForceSkills$excludeHiddenHeat(MultiBufferSource source, Entity entity,
                                                          CallbackInfoReturnable<MultiBufferSource> cir) {
        if (HelmetVisionRenderer.thermalCaptureActive() && entity instanceof SmokeCloudModelState) {
            cir.setReturnValue(source);
        }
    }

    @WrapOperation(method = "wrap", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;isInvisible()Z", remap = true))
    private static boolean dealtForceSkills$invisibleHeat(Entity entity, Operation<Boolean> original) {
        return !(entity instanceof LivingEntity living && HelmetVisionRenderer.reveals(living))
                && original.call(entity);
    }
}
