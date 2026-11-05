package com.rzy.dealt_force_skills.mixin;
import com.rzy.dealt_force_skills.workbench.*;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=MaceItem.class,remap=false)
public abstract class WorkbenchMaceMixin {
    @Inject(method="getAttackDamageBonus",at=@At("RETURN"),cancellable=true)
    private void dfs$fallBonus(Entity target,float damage,DamageSource source,CallbackInfoReturnable<Float> cir) {
        if(source.getDirectEntity() instanceof LivingEntity attacker)
            cir.setReturnValue((float)(cir.getReturnValue()*WorkbenchData.factor(attacker.getMainHandItem(),WorkbenchStat.SMASH)));
    }
}
