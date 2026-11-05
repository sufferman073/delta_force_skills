package com.rzy.dealt_force_skills.mixin;

import com.rzy.dealt_force_skills.ai.operative.NativeManbaSkills;
import com.rzy.dealt_force_skills.entity.OperativeEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Forge's damage event runs before health changes and can still be canceled by later listeners. */
@Mixin(value=LivingEntity.class,remap=false)
public abstract class OperativeManbaDamageMixin {
    @Unique private final java.util.Deque<Float> dfs$manbaHealth = new java.util.ArrayDeque<>();

    @Inject(method="actuallyHurt",at=@At("HEAD"))
    private void dfs$beforeManbaDamage(DamageSource source,float amount,CallbackInfo ci) {
        dfs$manbaHealth.push(((LivingEntity)(Object)this).getHealth());
    }

    @Inject(method="actuallyHurt",at=@At("RETURN"))
    private void dfs$afterManbaDamage(DamageSource source,float amount,CallbackInfo ci) {
        float before=dfs$manbaHealth.pop();
        var target=(LivingEntity)(Object)this;
        float lost=Math.max(0,before-target.getHealth());
        if(!target.level().isClientSide && target instanceof com.rzy.dealt_force_skills.ai.operative.OperativeHost victim)
            com.rzy.dealt_force_skills.ai.operative.NativeGrowthEvents.damaged(victim,lost);
        if(!target.level().isClientSide && source.getEntity() instanceof com.rzy.dealt_force_skills.ai.operative.OperativeHost bot) {
            com.rzy.dealt_force_skills.ai.operative.NativeGrowthEvents.damagedTarget(bot,target,lost);
            if(bot.skills() instanceof NativeManbaSkills skills)skills.damagedTarget(bot,target,lost);
            if(bot.skills() instanceof com.rzy.dealt_force_skills.ai.operative.NativeUndeadSkills skills)skills.damagedTarget(bot,target,lost);
            if(bot.skills() instanceof com.rzy.dealt_force_skills.ai.operative.NativeLexSkills skills)skills.damagedTarget(bot,target,lost);
        }
    }
}
