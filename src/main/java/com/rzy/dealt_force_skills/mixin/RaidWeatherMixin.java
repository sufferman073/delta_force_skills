package com.rzy.dealt_force_skills.mixin;

import com.rzy.dealt_force_skills.raid.RaidInstances;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Level.class, remap = false)
abstract class RaidWeatherMixin {
    @Inject(method = {"getRainLevel", "getThunderLevel"}, at = @At("HEAD"), cancellable = true)
    private void dfs$raidClear(float partial, CallbackInfoReturnable<Float> result) {
        if (((Level)(Object)this).dimension().equals(RaidInstances.DIMENSION)) result.setReturnValue(0F);
    }
}
