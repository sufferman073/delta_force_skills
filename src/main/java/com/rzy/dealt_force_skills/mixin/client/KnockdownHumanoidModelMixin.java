package com.rzy.dealt_force_skills.mixin.client;
import com.rzy.dealt_force_skills.client.visual.KnockdownPlayerPose;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value = HumanoidModel.class)
public abstract class KnockdownHumanoidModelMixin {
    @Inject(method = "prepareMobModel(Lnet/minecraft/world/entity/LivingEntity;FFF)V", at = @At("HEAD"))
    private void dfs$prepare(LivingEntity entity, float swing, float amount, float partial, CallbackInfo ci) {
        if ((Object)this instanceof com.rzy.dealt_force_skills.client.visual.TacticalPoseRestorer restorer)
            restorer.dfs$restoreTacticalPose();
    }
    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void dfs$stillLimbs(LivingEntity entity, float a, float b, float c, float d, float e, CallbackInfo ci) {
        KnockdownPlayerPose.apply((HumanoidModel<?>)(Object)this, entity);
    }
}
