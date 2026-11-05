package com.rzy.dealt_force_skills.mixin.client;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value = LivingEntityRenderer.class, remap = false)
public abstract class KnockdownLivingRendererMixin {
    @Inject(method = "setupRotations", at = @At("HEAD"), cancellable = true)
    private void dfs$lieDown(LivingEntity entity, PoseStack stack, float age, float yaw, float partial, float scale, CallbackInfo ci) {
        if (com.rzy.dealt_force_skills.client.visual.KnockdownPlayerPose.root(entity, stack, scale)) ci.cancel();
    }
}
