package com.rzy.dealt_force_skills.mixin;

import com.rzy.dealt_force_skills.department.DepartmentConsumption;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class DepartmentItemUseMixin {
    @Unique private java.util.ArrayDeque<DepartmentConsumption.Observation> dfs$departmentUses;
    @Inject(method="finishUsingItem",at=@At("HEAD"))
    private void dfs$beforeUse(Level level,LivingEntity user,CallbackInfoReturnable<ItemStack> cir) {
        ItemStack item=(ItemStack)(Object)this;
        if(!(user instanceof ServerPlayer player)||item.getItem() instanceof com.rzy.dealt_force_skills.item.DfsUseItem)return;
        UseAnim animation=item.getUseAnimation();
        if(animation!=UseAnim.EAT&&animation!=UseAnim.DRINK)return;
        if(dfs$departmentUses==null)dfs$departmentUses=new java.util.ArrayDeque<>();
        dfs$departmentUses.push(DepartmentConsumption.before(player,item));
    }
    @Inject(method="finishUsingItem",at=@At("RETURN"))
    private void dfs$afterUse(Level level,LivingEntity user,CallbackInfoReturnable<ItemStack> cir) {
        if(user instanceof ServerPlayer player&&dfs$departmentUses!=null&&!dfs$departmentUses.isEmpty()) {
            com.rzy.dealt_force_skills.department.DepartmentUseSpeed.finished(player,(ItemStack)(Object)this);
            DepartmentConsumption.finished(player,dfs$departmentUses.pop());
        }
    }
}
