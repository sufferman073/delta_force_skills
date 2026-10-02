package com.rzy.dealt_force_skills.mixin.client;

import com.rzy.dealt_force_skills.util.HearingRangeHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(value = SoundEngine.class, remap = false)
public abstract class HearingSoundEngineMixin {
    @Shadow @Final private Map<SoundInstance, ChannelAccess.ChannelHandle> instanceToChannel;
    @Inject(method = "tick(Z)V", at = @At("TAIL"))
    private void dfs$pauseIndividualMusic(boolean gamePaused, CallbackInfo ci) {
        instanceToChannel.forEach((sound, channel) -> {
            if (sound instanceof com.rzy.dealt_force_skills.client.FoxMusicClient.FollowingSound music) {
                boolean pause = gamePaused || music.paused();
                channel.execute(handle -> { if (pause) handle.pause(); else handle.unpause(); });
            }
        });

    }

    @Unique private double dealt_force_skills$lastHearingMultiplier = Double.NaN;

    @ModifyVariable(method = "play", at = @At("STORE"), ordinal = 1)
    private float dealt_force_skills$hearingDistance(float distance) {
        return (float) (distance * HearingRangeHelper.multiplier(Minecraft.getInstance().player));
    }

    @Inject(method = "tick(Z)V", at = @At("TAIL"))
    private void dealt_force_skills$updatePlayingSounds(boolean paused, CallbackInfo ci) {
        double multiplier = HearingRangeHelper.multiplier(Minecraft.getInstance().player);
        if (multiplier == dealt_force_skills$lastHearingMultiplier) {
            return;
        }
        dealt_force_skills$lastHearingMultiplier = multiplier;
        // Existing loops must also respond when the effect changes or expires.
        instanceToChannel.forEach((sound, channel) -> {
            if (!sound.isRelative() && sound.getAttenuation() == SoundInstance.Attenuation.LINEAR) {
                float distance = (float) (Math.max(sound.getVolume(), 1.0F)
                        * sound.getSound().getAttenuationDistance() * multiplier);
                channel.execute(handle -> handle.linearAttenuation(distance));
            }
        });
    }
}
