package com.rzy.dealt_force_skills.mixin.client;

import com.rzy.dealt_force_skills.client.HvkGlueClientCache;
import com.rzy.dealt_force_skills.config.DealtForceConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 客户端攀爬加速，适用于被HVK万能胶水粘合的方块。仅注册客户端侧。
 * 通过(1)使实体在粘合位置旁时{@code onClimbable()}返回{@code true}，
 * 然后(2)缩放原版的爬升/侧移速度来实现加速效果。
 */
@Mixin(LivingEntity.class)
public abstract class HvkClimbableMixin {

    @Inject(method = "onClimbable", at = @At("HEAD"), cancellable = true)
    private void dealt_force_skills$forceClimbableWhenGlued(CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (dealt_force_skills$isNearGluedPosition(self)) {
            cir.setReturnValue(true);
        }
    }

    @ModifyExpressionValue(method = "handleRelativeFrictionAndCalculateMovement",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;handleOnClimbable(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 dealt_force_skills$rescaleClimbShimmy(Vec3 result) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!dealt_force_skills$isNearGluedPosition(self)) {
            return result;
        }
        double multiplier = dealt_force_skills$climbSpeedMultiplier();
        return new Vec3(result.x * multiplier, result.y, result.z * multiplier);
    }

    @ModifyConstant(method = "handleRelativeFrictionAndCalculateMovement",
            constant = @Constant(doubleValue = 0.2D), require = 0, expect = 1)
    private double dealt_force_skills$rescaleClimbUpBoost(double original) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!dealt_force_skills$isNearGluedPosition(self)) {
            return original;
        }
        return original * dealt_force_skills$climbSpeedMultiplier();
    }

    private static double dealt_force_skills$climbSpeedMultiplier() {
        return DealtForceConfig.doubleValue("items.hvk_universal_glue.climb_speed_multiplier", 4.0D);
    }

    private static boolean dealt_force_skills$isNearGluedPosition(LivingEntity entity) {
        Level level = entity.level();
        ResourceKey<Level> dimension = level.dimension();
        BlockPos base = entity.blockPosition();
        if (HvkGlueClientCache.isGlued(dimension, base)) {
            return true;
        }
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (HvkGlueClientCache.isGlued(dimension, base.relative(direction))) {
                return true;
            }
        }
        return false;
    }
}