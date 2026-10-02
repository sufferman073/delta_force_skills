package com.rzy.dealt_force_skills.mixin.client;

import com.rzy.dealt_force_skills.client.VaultCameraMotion;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Camera.class, remap = false)
public abstract class VaultCameraMixin {
    @Shadow public abstract Vec3 getPosition();
    @Shadow protected abstract void setPosition(Vec3 position);

    @Inject(method = "setup", at = @At("TAIL"))
    private void dfs$interpolateVault(BlockGetter level, Entity entity, boolean detached,
                                     boolean reverse, float partialTick, CallbackInfo ci) {
        if (entity == Minecraft.getInstance().player) {
            setPosition(getPosition().add(VaultCameraMotion.offset(partialTick)));
        }
    }
}
