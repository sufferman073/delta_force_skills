package com.rzy.dealt_force_skills.mixin;
import com.rzy.dealt_force_skills.character.sineva.SinevaKnockdownState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value = Entity.class, remap = false)
public abstract class KnockdownEntityBoundsMixin {
    @Inject(method = "makeBoundingBox", at = @At("HEAD"), cancellable = true)
    private void dfs$horizontalBox(CallbackInfoReturnable<AABB> cir) {
        if ((Object)this instanceof LivingEntity entity) {
            AABB box = SinevaKnockdownState.boundingBox(entity);
            if (box != null) cir.setReturnValue(box);
        }
    }
}
