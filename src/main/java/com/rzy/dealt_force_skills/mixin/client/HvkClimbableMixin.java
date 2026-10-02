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
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Client-side climbing boost for blocks glued by the HVK Universal Glue item. Registered client-only
 * (same reasoning as the mod's other movement mixins): player movement is client-predicted, so the
 * server never runs this and would desync if it did.
 * <p>
 * Reuses 100% of vanilla's own climb-up / sideways-shimmy physics by (1) making {@code onClimbable()}
 * report {@code true} whenever the entity is at or beside a glued position, then (2) rescaling the two
 * places vanilla derives climb speed from: the horizontal clamp inside {@code handleOnClimbable} (governs
 * sideways shimmy) and the fixed {@code 0.2} climb-up boost inside
 * {@code handleRelativeFrictionAndCalculateMovement} (governs vertical climb speed).
 */
@Mixin(LivingEntity.class)
public abstract class HvkClimbableMixin {

    @Inject(method = "onClimbable", at = @At("HEAD"), cancellable = true, remap = false)
    private void dealt_force_skills$forceClimbableWhenGlued(CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (dealt_force_skills$isNearGluedPosition(self)) {
            cir.setReturnValue(true);
        }
    }

    @Invoker(value = "handleOnClimbable", remap = false)
    abstract Vec3 dealt_force_skills$invokeHandleOnClimbable(Vec3 vec3);

    @Redirect(method = "handleRelativeFrictionAndCalculateMovement",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;handleOnClimbable(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;"),
            remap = false)
    private Vec3 dealt_force_skills$rescaleClimbShimmy(LivingEntity self, Vec3 deltaMovement) {
        // handleOnClimbable is private on LivingEntity, so it can't be called directly on `self` from
        // this mixin's source (private access doesn't cross class files, regardless of how Mixin later
        // merges the bytecode) - go through an @Invoker accessor instead, called on `this` since the
        // generated invoker method only exists on the mixin's own class body.
        Vec3 result = this.dealt_force_skills$invokeHandleOnClimbable(deltaMovement);
        if (!dealt_force_skills$isNearGluedPosition(self)) {
            return result;
        }
        double multiplier = dealt_force_skills$climbSpeedMultiplier();
        return new Vec3(result.x * multiplier, result.y, result.z * multiplier);
    }

    @ModifyConstant(method = "handleRelativeFrictionAndCalculateMovement",
            constant = @Constant(doubleValue = 0.2D), require = 0, expect = 1, remap = false)
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
