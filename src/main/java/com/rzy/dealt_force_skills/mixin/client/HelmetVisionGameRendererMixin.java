package com.rzy.dealt_force_skills.mixin.client;

import com.rzy.dealt_force_skills.client.HelmetVisionRenderer;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GameRenderer.class, remap = false)
public abstract class HelmetVisionGameRendererMixin {
    @Unique
    private boolean dealtForceSkills$worldRendered;

    @Inject(method = "render", at = @At("HEAD"))
    private void dealtForceSkills$beginFrame(CallbackInfo ci) {
        dealtForceSkills$worldRendered = false;
    }

    @Inject(method = "renderLevel", at = @At("RETURN"))
    private void dealtForceSkills$worldReady(CallbackInfo ci) {
        dealtForceSkills$worldRendered = true;
    }

    // After world/hand/shader-pack composites, before the first GUI depth clear, including F1.
    @Inject(method = "render", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/RenderSystem;clear(IZ)V", ordinal = 0, remap = false))
    private void dealtForceSkills$helmetImaging(CallbackInfo ci) {
        if (dealtForceSkills$worldRendered) {
            HelmetVisionRenderer.render();
            dealtForceSkills$worldRendered = false;
        }
    }
}
