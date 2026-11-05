package com.rzy.dealt_force_skills.mixin;
import com.rzy.dealt_force_skills.workbench.*;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
@Mixin(value=TridentItem.class,remap=false)
public abstract class WorkbenchTridentMixin {
    @ModifyVariable(method="releaseUsing",at=@At("HEAD"),argsOnly=true)
    private int dfs$charge(int remaining,ItemStack stack,Level world,LivingEntity user,int originalRemaining) {
        int duration=stack.getUseDuration(user);
        return duration-WorkbenchCharge.elapsed(stack,duration-remaining,10);
    }

    @WrapOperation(method="releaseUsing",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean dfs$launch(Level world,Entity projectile,Operation<Boolean> operation,ItemStack weapon,Level level,LivingEntity user,int remaining) {
        projectile.setDeltaMovement(projectile.getDeltaMovement().scale(WorkbenchData.factor(weapon,WorkbenchStat.PROJECTILE_SPEED)));
        return operation.call(world,projectile);
    }
}
