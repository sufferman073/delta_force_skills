package com.rzy.dealt_force_skills.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.rzy.dealt_force_skills.client.visual.ClientTacticalMotion;
import com.rzy.dealt_force_skills.client.visual.TacticalPlayerPose;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerRenderer.class)
public abstract class TacticalPlayerRendererMixin {
    @ModifyExpressionValue(method = "setupRotations", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/AbstractClientPlayer;getSwimAmount(F)F"))
    private float dfs$avoidDoubleSwim(float vanilla, AbstractClientPlayer player, PoseStack stack, float age, float yaw, float partial) {
        if (com.rzy.dealt_force_skills.character.sineva.SinevaKnockdownState.active(player)) return 0;
        var frame = ClientTacticalMotion.frame(player, partial);
        return frame != null && frame.kind().ownsRoot() ? vanilla * (1 - frame.pose().weight) : vanilla;
    }
    @Inject(method = "setupRotations", at = @At("TAIL"))
    private void dfs$root(AbstractClientPlayer player, PoseStack stack, float age, float yaw, float partial, CallbackInfo ci) {
        TacticalPlayerPose.root(player, stack, yaw, partial);
    }
}
