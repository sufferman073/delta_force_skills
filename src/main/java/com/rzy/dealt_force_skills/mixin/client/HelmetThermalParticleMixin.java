package com.rzy.dealt_force_skills.mixin.client;

import com.rzy.dealt_force_skills.client.HelmetVisionClient;
import net.minecraft.client.particle.ParticleEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ParticleEngine.class)
public abstract class HelmetThermalParticleMixin {
    // Target the loader's render overload used by LevelRenderer, including its frustum argument.
    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;)V",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void dealtForceSkills$hideThermalParticles(CallbackInfo ci) {
        if (HelmetVisionClient.isThermalVisionActive()) {
            ci.cancel();
        }
    }
}
