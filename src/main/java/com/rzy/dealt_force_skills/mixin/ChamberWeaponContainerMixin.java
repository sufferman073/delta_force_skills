package com.rzy.dealt_force_skills.mixin;

import com.rzy.dealt_force_skills.character.chamber.ChamberTaczEnhancement;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AbstractContainerMenu.class, remap = false)
public abstract class ChamberWeaponContainerMixin {
    @Inject(method = "clicked", at = @At("HEAD"), cancellable = true)
    private void dfs$keepSummonedWeapon(int slot, int button, ClickType type, Player player, CallbackInfo callback) {
        AbstractContainerMenu menu = (AbstractContainerMenu) (Object) this;
        boolean cursorDrop = slot == -999 && type == ClickType.PICKUP
                && ChamberTaczEnhancement.isChamberWeapon(menu.getCarried());
        boolean slotDrop = type == ClickType.THROW && slot >= 0 && slot < menu.slots.size()
                && ChamberTaczEnhancement.isChamberWeapon(menu.getSlot(slot).getItem());
        if (cursorDrop || slotDrop) {
            menu.sendAllDataToRemote();
            callback.cancel();
        }
    }
}
