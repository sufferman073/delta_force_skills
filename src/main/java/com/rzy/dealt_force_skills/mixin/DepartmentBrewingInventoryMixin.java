package com.rzy.dealt_force_skills.mixin;

import com.rzy.dealt_force_skills.department.DepartmentBrewing;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value=BaseContainerBlockEntity.class,remap=false)
public abstract class DepartmentBrewingInventoryMixin {
    @Inject(method="setItem",at=@At("HEAD"))
    private void dfs$changedBrewInput(int slot,ItemStack stack,CallbackInfo ci) {
        if(slot>=0&&slot<4)DepartmentBrewing.invalidate(this);
    }
    @Inject(method="removeItem",at=@At("HEAD"))
    private void dfs$removedBrewInput(int slot,int amount,CallbackInfoReturnable<ItemStack> ci) {
        if(slot>=0&&slot<4&&amount>0)DepartmentBrewing.invalidate(this);
    }
    @Inject(method="removeItemNoUpdate",at=@At("HEAD"))
    private void dfs$takenBrewInput(int slot,CallbackInfoReturnable<ItemStack> ci) {
        if(slot>=0&&slot<4)DepartmentBrewing.invalidate(this);
    }
    @Inject(method="clearContent",at=@At("HEAD"))
    private void dfs$clearedBrewInputs(CallbackInfo ci) { DepartmentBrewing.invalidate(this); }
}
