package com.rzy.dealt_force_skills.mixin;

import com.rzy.dealt_force_skills.department.DepartmentTasks;
import com.rzy.dealt_force_skills.department.DepartmentTracker;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityDepartmentHealthMixin {
    @Unique private java.util.ArrayDeque<Float> dfs$healthWrites;

    @Inject(method="setHealth",at=@At("HEAD"))
    private void dfs$beforeHealth(float health,CallbackInfo ci) {
        if(!((Object)this instanceof ServerPlayer player))return;
        if(dfs$healthWrites==null)dfs$healthWrites=new java.util.ArrayDeque<>();
        dfs$healthWrites.push(player.server!=null&&player.getPersistentData().contains(DepartmentTracker.KEY)
                ?player.getHealth():Float.NaN);
    }
    @Inject(method="setHealth",at=@At("RETURN"))
    private void dfs$afterHealth(float health,CallbackInfo ci) {
        if(!((Object)this instanceof ServerPlayer player)||dfs$healthWrites==null||dfs$healthWrites.isEmpty())return;
        float before=dfs$healthWrites.pop();
        if(Float.isFinite(before)&&player.getHealth()>before)DepartmentTasks.tracker().fail(player,"heal");
    }
}
