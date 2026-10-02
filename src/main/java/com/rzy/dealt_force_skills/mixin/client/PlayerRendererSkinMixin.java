package com.rzy.dealt_force_skills.mixin.client;

import com.rzy.dealt_force_skills.client.character.ClientCharacterSkinState;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererSkinMixin {
    @Inject(method = "getTextureLocation(Lnet/minecraft/client/player/AbstractClientPlayer;)Lnet/minecraft/resources/ResourceLocation;", at = @At("HEAD"), cancellable = true, remap = false)
    private void dealt_force_skills$useSelectedCharacterSkin(AbstractClientPlayer player, CallbackInfoReturnable<ResourceLocation> cir) {
        ClientCharacterSkinState.skinFor(player).ifPresent(cir::setReturnValue);
    }
}
