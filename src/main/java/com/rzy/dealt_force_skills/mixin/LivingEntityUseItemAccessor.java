package com.rzy.dealt_force_skills.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LivingEntity.class)
public interface LivingEntityUseItemAccessor {
    @Accessor(value = "useItemRemaining", remap = false)
    void dealt_force_skills$setUseItemRemaining(int remainingTicks);
}
