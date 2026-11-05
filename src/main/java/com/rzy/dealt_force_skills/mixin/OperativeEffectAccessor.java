package com.rzy.dealt_force_skills.mixin;

import net.minecraft.world.effect.MobEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Edits an incoming effect before the effect event returns to vanilla's merge. */
@Mixin(MobEffectInstance.class)
public interface OperativeEffectAccessor {
    @Accessor("hiddenEffect")
    MobEffectInstance dfs$hiddenEffect();

    @Accessor("duration")
    void dealt_force_skills$setDuration(int ticks);

    @Accessor("amplifier")
    void dealt_force_skills$setAmplifier(int amplifier);
}
