package com.rzy.dealt_force_skills.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.rzy.dealt_force_skills.client.visual.TacticalPlayerPose;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ModelPart.class, remap = false)
public abstract class TacticalArmTransformMixin {
    @Inject(method = "translateAndRotate", at = @At("HEAD"))
    private void dfs$levelGrip(PoseStack stack, CallbackInfo ci) {
        TacticalPlayerPose.armRoot((ModelPart)(Object)this, stack);
    }
}
