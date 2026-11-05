package com.rzy.dealt_force_skills.mixin;

import com.rzy.dealt_force_skills.compat.maid.MaidSkillHost;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets={"com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.task.MaidFollowOwnerTask", "com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.task.MaidFollowOwnerVehicleTask"}, remap=false)
public abstract class TouhouMaidFollowMixin {
    @Inject(method="checkExtraStartConditions(Lnet/minecraft/server/level/ServerLevel;Lcom/github/tartaricacid/touhoulittlemaid/entity/passive/EntityMaid;)Z", at=@At("HEAD"), cancellable=true, require=0)
    private void dfs$freedom(ServerLevel level, @Coerce LivingEntity maid, CallbackInfoReturnable<Boolean> cir) {
        if (maid instanceof MaidSkillHost host && host.dealtForceMaidSkills().freeToActAt(maid.position())) cir.setReturnValue(false);
    }
}
