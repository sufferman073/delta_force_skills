package com.rzy.dealt_force_skills.mixin.client;

import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundEngine.class)
public abstract class HookAuthorChargeSoundPitchMixin {
    private static final float MAX_CHARGE_PITCH = 20.0F;

    @Inject(method = "calculatePitch", at = @At("HEAD"), cancellable = true, remap = false)
    private void dealt_force_skills$allowHighChargePitch(SoundInstance sound,
                                                          CallbackInfoReturnable<Float> cir) {
        ResourceLocation location = sound.getLocation();
        if ("dealt_force_skills".equals(location.getNamespace())
                && "hook_author_charge_step".equals(location.getPath())) {
            cir.setReturnValue(Math.max(0.5F, Math.min(MAX_CHARGE_PITCH, sound.getPitch())));
        }
    }
}
