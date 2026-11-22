package com.rzy.dealt_force_skills.mapeditor;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.command.MapEditorCommands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Marker;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DealtForceSkillsMod.MODID)
public final class MapEditorEvents {
    private MapEditorEvents() { }
    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.HIGH)
    public static void toolClick(net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getItemStack().getItem() instanceof MapEditorToolItem tool)) return;
        event.setCanceled(true);
        event.setCancellationResult(tool.useOn(new net.minecraft.world.item.context.UseOnContext(
                event.getEntity(), event.getHand(), event.getHitVec())));
    }
    @SubscribeEvent public static void commands(RegisterCommandsEvent event) { MapEditorCommands.register(event.getDispatcher()); }
    @SubscribeEvent public static void join(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide && event.getEntity() instanceof Marker marker) MapMarkerService.observe(marker);
    }
    @SubscribeEvent public static void leave(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide || !(event.getEntity() instanceof Marker marker)) return;
        if (marker.getRemovalReason() == Entity.RemovalReason.KILLED || marker.getRemovalReason() == Entity.RemovalReason.DISCARDED)
            MapMarkerData.get(((net.minecraft.server.level.ServerLevel) event.getLevel()).getServer()).remove(marker.getUUID());
        else MapMarkerService.observe(marker);
    }
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player) MapEditorView.tick(player);
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) MapEditorView.clear(player);
    }
    @SubscribeEvent public static void stop(ServerStoppedEvent event) { MapEditorView.clearAll(); }
}
