package com.rzy.dealt_force_skills.mixin;

import com.rzy.dealt_force_skills.compat.maid.MaidSkillHost;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Pause ordinary melee only while a maid skill owns the weapon controls. */
@Mixin(Mob.class)
public abstract class MaidMeleeMixin {
    @Inject(method="doHurtTarget", at=@At("HEAD"), cancellable=true)
    private void dfs$pauseMaidMelee(Entity target, CallbackInfoReturnable<Boolean> cir) {
        if ((Object)this instanceof MaidSkillHost host) {
            Boolean result = host.dealtForceMaidSkills().attack(target);
            if (result != null) cir.setReturnValue(result);
        }
    }
}
