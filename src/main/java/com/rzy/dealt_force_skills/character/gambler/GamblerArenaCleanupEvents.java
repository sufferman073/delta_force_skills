package com.rzy.dealt_force_skills.character.gambler;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID)
public final class GamblerArenaCleanupEvents {
    private static final String ROUND_TAG = "DfsGamblerArenaRound";
    private static String round = java.util.UUID.randomUUID().toString();
    private static boolean resetting;

    private GamblerArenaCleanupEvents() {}

    static void beginReset() {
        round = java.util.UUID.randomUUID().toString();
        resetting = true;
    }

    static void endReset() { resetting = false; }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide || !event.getLevel().dimension().equals(GamblerArenaManager.ARENA_LEVEL)
                || event.getEntity() instanceof Player) return;
        // Chunk terrain can load before its saved entities. Reject late arrivals from old rounds.
        var data = event.getEntity().getPersistentData();
        if (resetting || (event.loadedFromDisk() && !round.equals(data.getString(ROUND_TAG)))) {
            event.setCanceled(true);
            return;
        }
        data.putString(ROUND_TAG, round);
    }
}
