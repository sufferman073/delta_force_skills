package com.rzy.dealt_force_skills.mixin;

import net.minecraft.world.effect.MobEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Edits an incoming effect before the effect event returns to vanilla's merge. */
@Mixin(MobEffectInstance.class)
public interface OperativeEffectAccessor {
    @Accessor(value = "hiddenEffect", remap = false)
    MobEffectInstance dfs$hiddenEffect();

    @Accessor(value = "duration", remap = false)
    void dealt_force_skills$setDuration(int ticks);

    @Accessor(value = "amplifier", remap = false)
    void dealt_force_skills$setAmplifier(int amplifier);
}
