package com.rzy.dealt_force_skills.ai.operative;

import com.rzy.dealt_force_skills.network.S2C_LunaRevealEntities;
import com.rzy.dealt_force_skills.util.PositionRevealHelper;
import com.rzy.dealt_force_skills.util.ReconRevealThrottle;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;

import java.util.List;

/** Native recon informs the living brain and shares the existing outline with its commander. */
public final class OperativeRecon {
    private OperativeRecon() { }

    public static void reveal(OperativeHost host, LivingEntity target, int ticks) {
        if (ticks <= 0 || !host.isEnemy(target)) return;
        if (host.skills() != null) host.skills().remember(target, ticks);
        if (!ReconRevealThrottle.tryStart(target, ticks)) return;
        if (host.body() instanceof OwnableEntity owned && owned.getOwnerUUID() != null
                && host.serverLevel().getPlayerByUUID(owned.getOwnerUUID()) instanceof ServerPlayer commander) {
            PositionRevealHelper.sendToCasterAndTeammates(commander,
                    new S2C_LunaRevealEntities(List.of(target.getId()), ticks));
        }
    }
}
