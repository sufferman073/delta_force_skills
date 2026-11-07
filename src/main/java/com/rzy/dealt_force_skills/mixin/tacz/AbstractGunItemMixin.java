package com.rzy.dealt_force_skills.mixin.tacz;

import com.rzy.dealt_force_skills.character.ghroth.GhrothTaczEnhancement;
import com.tacz.guns.api.item.attachment.AttachmentType;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = com.tacz.guns.api.item.gun.AbstractGunItem.class, remap = false)
public abstract class AbstractGunItemMixin {
    @Inject(method = "allowAttachment", at = @At("RETURN"), cancellable = true, remap = false)
    private void dfs$allowGhrothEnhancedAttachment(ItemStack gun, ItemStack attachmentItem,
                                                  CallbackInfoReturnable<Boolean> cir) {
        if (Boolean.FALSE.equals(cir.getReturnValue())
                && GhrothTaczEnhancement.shouldForceAllowAttachment(gun, attachmentItem)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "allowAttachmentType", at = @At("RETURN"), cancellable = true, remap = false)
    private void dfs$allowGhrothEnhancedAttachmentType(ItemStack gun, AttachmentType type,
                                                      CallbackInfoReturnable<Boolean> cir) {
        if (Boolean.FALSE.equals(cir.getReturnValue())
                && GhrothTaczEnhancement.shouldForceAllowAttachmentType(gun, type)) {
            cir.setReturnValue(true);
        }
    }
}
