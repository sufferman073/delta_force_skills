package com.rzy.dealt_force_skills.mixin.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class ScreenBackgroundBlurMixin {
    @Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true, remap = false)
    private void dealtForceSkills$skipProjectScreenBlur(GuiGraphics graphics, int mouseX, int mouseY,
                                                        float partialTick, CallbackInfo callback) {
        String screenClass = getClass().getName();
        if (screenClass.startsWith("com.rzy.dealt_force_skills.")
                || screenClass.startsWith("mccfk.DeltaForce.")) {
            callback.cancel();
        }
    }
}
