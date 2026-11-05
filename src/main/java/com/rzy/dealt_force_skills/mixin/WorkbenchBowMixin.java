package com.rzy.dealt_force_skills.mixin;
import com.rzy.dealt_force_skills.workbench.*;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(value=BowItem.class,remap=false)
public abstract class WorkbenchBowMixin {
    @ModifyVariable(method="releaseUsing",at=@At("HEAD"),argsOnly=true)
    private int dfs$charge(int remaining,ItemStack stack,Level world,LivingEntity user,int originalRemaining) {
        int duration=stack.getUseDuration(user);
        return duration-WorkbenchCharge.elapsed(stack,duration-remaining,20);
    }
}
