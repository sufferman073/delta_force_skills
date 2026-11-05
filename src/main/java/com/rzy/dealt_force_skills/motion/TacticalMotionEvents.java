package com.rzy.dealt_force_skills.motion;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DealtForceSkillsMod.MODID)
public final class TacticalMotionEvents {
    private TacticalMotionEvents() { }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer p) TacticalMotionSync.tick(p);
    }
    @SubscribeEvent
    public static void track(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer viewer && event.getTarget() instanceof ServerPlayer target)
            TacticalMotionSync.tracking(viewer, target);
    }
    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) TacticalMotionSync.clear(p);
    }
    @SubscribeEvent
    public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) TacticalMotionSync.clear(p);
    }
    @SubscribeEvent
    public static void clone(PlayerEvent.Clone event) {
        if (event.getOriginal() instanceof ServerPlayer p) TacticalMotionSync.clear(p);
    }
    @SubscribeEvent
    public static void stop(ServerStoppedEvent event) { TacticalMotionSync.clearAll(); }
}
