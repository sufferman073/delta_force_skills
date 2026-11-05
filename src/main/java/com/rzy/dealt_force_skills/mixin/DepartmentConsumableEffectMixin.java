package com.rzy.dealt_force_skills.mixin;

import com.rzy.dealt_force_skills.department.DepartmentConsumables;
import com.rzy.dealt_force_skills.department.DepartmentEffectSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MobEffectInstance.class)
public abstract class DepartmentConsumableEffectMixin implements DepartmentEffectSource {
    @Unique private DepartmentConsumables.Source dfs$source;
    @Unique private boolean dfs$replaceSource;
    @Unique private com.rzy.dealt_force_skills.department.DepartmentManba.Silence dfs$silence;
    public com.rzy.dealt_force_skills.department.DepartmentManba.Silence dfs$silence(){return dfs$silence;}
    public void dfs$silence(com.rzy.dealt_force_skills.department.DepartmentManba.Silence silence){dfs$silence=silence;}
    public DepartmentConsumables.Source dfs$consumableSource(){return dfs$source;}
    public void dfs$consumableSource(DepartmentConsumables.Source source){dfs$source=source;}
    @Inject(method="setDetailsFrom",at=@At("RETURN"))
    private void dfs$copyConsumableSource(MobEffectInstance other,CallbackInfo ci) {
        dfs$source=((DepartmentEffectSource)other).dfs$consumableSource();
        dfs$silence=((DepartmentEffectSource)other).dfs$silence();
    }
    @Inject(method="update",at=@At("HEAD"))
    private void dfs$beforeConsumableMerge(MobEffectInstance other,CallbackInfoReturnable<Boolean> ci) {
        var self=(MobEffectInstance)(Object)this;
        dfs$replaceSource=other.getAmplifier()>self.getAmplifier()||(other.getAmplifier()==self.getAmplifier()
                &&!self.isInfiniteDuration()&&(other.isInfiniteDuration()||other.getDuration()>self.getDuration()));
    }
    @Inject(method="update",at=@At("RETURN"))
    private void dfs$afterConsumableMerge(MobEffectInstance other,CallbackInfoReturnable<Boolean> ci) {
        if(dfs$replaceSource)dfs$source=((DepartmentEffectSource)other).dfs$consumableSource();
        if(dfs$replaceSource)dfs$silence=com.rzy.dealt_force_skills.department.DepartmentManba.merge(dfs$silence,((DepartmentEffectSource)other).dfs$silence());
    }
    @Inject(method="tick",at=@At("HEAD"))
    private void dfs$countSilenceTick(net.minecraft.world.entity.LivingEntity target,Runnable onExpire,CallbackInfoReturnable<Boolean> ci) {
        com.rzy.dealt_force_skills.department.DepartmentManba.tickSilence((MobEffectInstance)(Object)this,target);
    }
    @Inject(method="save",at=@At("RETURN"))
    private void dfs$saveConsumableSource(CompoundTag tag,CallbackInfoReturnable<CompoundTag> ci) {
        DepartmentConsumables.save((MobEffectInstance)(Object)this,ci.getReturnValue());
    }
    @Inject(method="load",at=@At("RETURN"))
    private static void dfs$loadConsumableSource(CompoundTag tag,CallbackInfoReturnable<MobEffectInstance> ci) {
        if(ci.getReturnValue()!=null)DepartmentConsumables.load(ci.getReturnValue(),tag);
    }
}
