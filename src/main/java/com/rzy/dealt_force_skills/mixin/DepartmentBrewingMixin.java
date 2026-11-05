package com.rzy.dealt_force_skills.mixin;

import com.rzy.dealt_force_skills.department.DepartmentBrewing;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BrewingStandBlockEntity.class)
public abstract class DepartmentBrewingMixin {
    @Inject(method="doBrew",at=@At("HEAD"))
    private static void dfs$beforeDepartmentBrew(Level level,BlockPos pos,NonNullList<ItemStack> items,CallbackInfo ci) {
        DepartmentBrewing.beforeBrew(level,pos);
    }
    @Inject(method="doBrew",at=@At("RETURN"))
    private static void dfs$afterDepartmentBrew(Level level,BlockPos pos,NonNullList<ItemStack> items,CallbackInfo ci) {
        DepartmentBrewing.afterBrew(level,pos);
    }
}
