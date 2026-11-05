package com.rzy.dealt_force_skills.mixin;

import com.rzy.dealt_force_skills.compat.maid.MaidSkillController;
import com.rzy.dealt_force_skills.compat.maid.MaidSkillHost;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;

/** Optional host bridge and native return-policy adjustments for assigned maids. */
@Pseudo
@Mixin(targets="com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid", remap=false)
public abstract class TouhouMaidHostMixin implements MaidSkillHost {
    @org.spongepowered.asm.mixin.injection.Inject(method="teleportToOwner", at=@org.spongepowered.asm.mixin.injection.At("HEAD"), cancellable=true, require=0)
    private void dfs$noEarlyReturn(LivingEntity owner, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> cir) {
        if (dealtForceMaidSkills().freeToActAt(body().position())) cir.setReturnValue(false);
    }
    @org.spongepowered.asm.mixin.injection.Inject(method="isWithinRestriction(Lnet/minecraft/core/BlockPos;)Z", at=@org.spongepowered.asm.mixin.injection.At("HEAD"), cancellable=true, require=0, remap=false)
    private void dfs$homeFreedom(net.minecraft.core.BlockPos pos, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> cir) {
        if (dealtForceMaidSkills().freeToActAt(net.minecraft.world.phys.Vec3.atCenterOf(pos))) cir.setReturnValue(true);
    }
    @org.spongepowered.asm.mixin.injection.Inject(method="isWithinRestriction()Z", at=@org.spongepowered.asm.mixin.injection.At("HEAD"), cancellable=true, require=0, remap=false)
    private void dfs$currentHomeFreedom(org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> cir) {
        if (dealtForceMaidSkills().freeToActAt(body().position())) cir.setReturnValue(true);
    }
    @Unique private MaidSkillController dfs$maidSkills;
    @Override public LivingEntity body() { return (LivingEntity)(Object)this; }
    @Override public MaidSkillController dealtForceMaidSkills() {
        if (dfs$maidSkills == null) dfs$maidSkills = new MaidSkillController(this);
        return dfs$maidSkills;
    }
}
