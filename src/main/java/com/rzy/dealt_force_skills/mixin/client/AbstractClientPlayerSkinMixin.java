package com.rzy.dealt_force_skills.mixin.client;

import com.rzy.dealt_force_skills.client.character.ClientCharacterSkinState;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerSkinMixin {
    @Inject(method = "getSkin", at = @At("RETURN"), cancellable = true, remap = false)
    private void dealt_force_skills$useSelectedCharacterSkin(CallbackInfoReturnable<PlayerSkin> cir) {
        AbstractClientPlayer player = (AbstractClientPlayer) (Object) this;
        ClientCharacterSkinState.skinFor(player).ifPresent(texture -> {
            PlayerSkin original = cir.getReturnValue();
            cir.setReturnValue(new PlayerSkin(
                    texture,
                    original.textureUrl(),
                    original.capeTexture(),
                    original.elytraTexture(),
                    ClientCharacterSkinState.modelFor(player)
                            .map(model -> "slim".equals(model) ? PlayerSkin.Model.SLIM : PlayerSkin.Model.WIDE)
                            .orElse(original.model()),
                    original.secure()
            ));
        });
    }
}
