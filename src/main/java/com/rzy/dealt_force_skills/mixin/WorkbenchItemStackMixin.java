package com.rzy.dealt_force_skills.mixin;

import com.rzy.dealt_force_skills.workbench.WorkbenchData;
import com.rzy.dealt_force_skills.workbench.WorkbenchStat;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value=ItemStack.class,remap=false)
public abstract class WorkbenchItemStackMixin {
    @Inject(method="getMaxDamage",at=@At("RETURN"),cancellable=true)
    private void dfs$workbenchDurability(CallbackInfoReturnable<Integer> cir) {
        ItemStack stack=(ItemStack)(Object)this;
        int original=cir.getReturnValue();
        if(original<=0||WorkbenchData.tag(stack).isEmpty())return;
        int steps=WorkbenchData.steps(stack,WorkbenchStat.DURABILITY);
        if(steps!=0)cir.setReturnValue((int)Math.max(1,Math.min(Integer.MAX_VALUE,Math.round(original*(1+.01*steps)))));
    }
    @Inject(method="getDestroySpeed",at=@At("RETURN"),cancellable=true)
    private void dfs$workbenchMining(net.minecraft.world.level.block.state.BlockState state,CallbackInfoReturnable<Float> cir) {
        ItemStack stack=(ItemStack)(Object)this;
        if(WorkbenchData.steps(stack,WorkbenchStat.MINING_SPEED)!=0)
            cir.setReturnValue((float)(cir.getReturnValue()*WorkbenchData.factor(stack,WorkbenchStat.MINING_SPEED)));
    }
    @Inject(method="isCorrectToolForDrops",at=@At("RETURN"),cancellable=true)
    private void dfs$workbenchHarvest(net.minecraft.world.level.block.state.BlockState state,CallbackInfoReturnable<Boolean> cir) {
        ItemStack stack=(ItemStack)(Object)this;
        if(WorkbenchData.steps(stack,WorkbenchStat.HARVEST_LEVEL)!=0)
            cir.setReturnValue(com.rzy.dealt_force_skills.workbench.WorkbenchHarvest.correct(stack,state,cir.getReturnValue()));
    }
}
