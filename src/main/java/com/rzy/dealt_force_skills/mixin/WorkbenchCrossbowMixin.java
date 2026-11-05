package com.rzy.dealt_force_skills.mixin;
import com.rzy.dealt_force_skills.workbench.*;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(value=CrossbowItem.class,remap=false)
public abstract class WorkbenchCrossbowMixin {
    @Inject(method="getChargeDuration",at=@At("RETURN"),cancellable=true)
    private static void dfs$charge(ItemStack stack,LivingEntity user,CallbackInfoReturnable<Integer> ci) {
        int steps=WorkbenchData.steps(stack,WorkbenchStat.CHARGE_TIME);
        if(steps!=0)ci.setReturnValue(Math.max(1,ci.getReturnValue()-steps*2));
    }
}
