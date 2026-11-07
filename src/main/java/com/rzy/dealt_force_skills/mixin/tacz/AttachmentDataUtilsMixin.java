package com.rzy.dealt_force_skills.mixin.tacz;

import com.rzy.dealt_force_skills.character.ghroth.GhrothTaczEnhancement;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = com.tacz.guns.util.AttachmentDataUtils.class, remap = false)
public final class AttachmentDataUtilsMixin {
    private AttachmentDataUtilsMixin() {
    }

    @Inject(method = "getAmmoCountWithAttachment", at = @At("RETURN"), cancellable = true, remap = false)
    private static void dfs$ghrothEnhancedMagazine(ItemStack gunItem, GunData gunData,
                                                   CallbackInfoReturnable<Integer> cir) {
        int baseCapacity = Math.max(0, cir.getReturnValueI());
        int enhancedCapacity = GhrothTaczEnhancement.enhancedMagazineCapacity(gunItem, baseCapacity);
        if (enhancedCapacity > baseCapacity) {
            cir.setReturnValue(enhancedCapacity);
        }
    }
}
