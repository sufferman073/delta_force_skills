package com.rzy.dealt_force_skills.mixin;
import com.rzy.dealt_force_skills.workbench.*;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(value=BowItem.class)
public abstract class WorkbenchBowMixin {
    @ModifyVariable(method="releaseUsing",at=@At("HEAD"),argsOnly=true)
    private int dfs$charge(int remaining,ItemStack stack,Level world,LivingEntity user,int originalRemaining) {
        int duration=stack.getUseDuration();
        return duration-WorkbenchCharge.elapsed(stack,duration-remaining,20);
    }

    @WrapOperation(method="releaseUsing",at=@At(value="INVOKE",target="Lnet/minecraft/world/item/BowItem;customArrow(Lnet/minecraft/world/entity/projectile/AbstractArrow;)Lnet/minecraft/world/entity/projectile/AbstractArrow;"))
    private net.minecraft.world.entity.projectile.AbstractArrow dfs$snapshot(BowItem item,net.minecraft.world.entity.projectile.AbstractArrow arrow,Operation<net.minecraft.world.entity.projectile.AbstractArrow> operation,ItemStack weapon,Level level,LivingEntity user,int remaining) {
        var result=operation.call(item,arrow);
        ((WorkbenchArrowSnapshot)result).dfs$weapon(weapon.copy());
        return result;
    }
    @WrapOperation(method="releaseUsing",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean dfs$launch(Level world,Entity projectile,Operation<Boolean> operation) {
        var arrow=(net.minecraft.world.entity.projectile.AbstractArrow)projectile;
        ItemStack weapon=((WorkbenchArrowSnapshot)arrow).dfs$takeWeapon();
        arrow.setBaseDamage(arrow.getBaseDamage()*WorkbenchData.factor(weapon,WorkbenchStat.BOW_DAMAGE)*(arrow.isCritArrow()?WorkbenchData.factor(weapon,WorkbenchStat.BOW_FULL_DAMAGE):1));
        arrow.setDeltaMovement(arrow.getDeltaMovement().scale(WorkbenchData.factor(weapon,WorkbenchStat.PROJECTILE_SPEED)));
        return operation.call(world,arrow);
    }
}
