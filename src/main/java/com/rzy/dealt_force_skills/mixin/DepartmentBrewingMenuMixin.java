package com.rzy.dealt_force_skills.mixin;

import com.rzy.dealt_force_skills.department.DepartmentBrewing;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=AbstractContainerMenu.class,remap=false)
public abstract class DepartmentBrewingMenuMixin {
    @Inject(method="clicked",at=@At("HEAD"))
    private void dfs$beforeBrewClick(int slot,int button,ClickType type,Player player,CallbackInfo ci) {
        DepartmentBrewing.beforeClick((AbstractContainerMenu)(Object)this,player);
    }
    @Inject(method="clicked",at=@At("RETURN"))
    private void dfs$afterBrewClick(int slot,int button,ClickType type,Player player,CallbackInfo ci) {
        DepartmentBrewing.afterClick((AbstractContainerMenu)(Object)this,player);
    }
}
