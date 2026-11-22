package com.rzy.dealt_force_skills.skill;

import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.S2C_SyncHeldToolVisual;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public final class ToolHandRelease {
    private ToolHandRelease() {}

    public static boolean spawned(boolean spawned, LivingEntity caster, ToolHandCue cue) {
        if (spawned && caster instanceof ServerPlayer player) {
            NetworkHandler.sendToTrackingAndSelf(new S2C_SyncHeldToolVisual(player.getId(), cue.releaseId()), player);
        }
        return spawned;
    }
}
