package com.rzy.dealt_force_skills.mixin.client;

import com.rzy.dealt_force_skills.client.HelmetVisionClient;
import net.minecraft.client.particle.ParticleEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ParticleEngine.class, remap = false)
public abstract class HelmetThermalParticleMixin {
    // Target the loader's render overload used by LevelRenderer, including its frustum argument.
    @Inject(method = "render(Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;Ljava/util/function/Predicate;)V",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void dealtForceSkills$hideThermalParticles(CallbackInfo ci) {
        if (HelmetVisionClient.isThermalVisionActive()) {
            ci.cancel();
        }
    }
}
