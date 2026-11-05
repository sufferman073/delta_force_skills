package com.rzy.dealt_force_skills.home;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID)
public final class HomeEvents {
    private HomeEvents() { }
    @SubscribeEvent public static void starting(ServerStartingEvent event) { HomeDimensions.restore(event.getServer()); }
    @SubscribeEvent public static void tick(ServerTickEvent.Post event) {
        HomeManager.maintain(event.getServer());
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            syncBorder(player);
            var occupied = HomeData.get(player.server).homes.get(HomeDimensions.owner(player.level().dimension()));
            if (occupied != null && occupied.initialized) HomeDimensions.initialize(player.serverLevel(), occupied);
            var home = HomeData.get(player.server).homes.get(player.getUUID());
            if (home != null) { home.name = player.getGameProfile().getName(); HomeData.get(player.server).setDirty(); }
        }
    }
    private static void syncBorder(ServerPlayer player) {
        if (HomeDimensions.isHome(player.level())) player.connection.send(new net.minecraft.network.protocol.game.ClientboundInitializeBorderPacket(player.serverLevel().getWorldBorder()));
    }
    @SubscribeEvent public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) syncBorder(player);
    }
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) syncBorder(player);
    }
    @SubscribeEvent public static void chunkLoad(net.neoforged.neoforge.event.level.ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof net.minecraft.server.level.ServerLevel level)
                || !(event.getChunk() instanceof net.minecraft.world.level.chunk.LevelChunk chunk)) return;
        var owner = HomeDimensions.owner(level.dimension());
        if (owner == null) return;
        // Always defer until a server tick: execute() can run inline before the chunk is FULL.
        HomeTerrain.schedule(chunk, event.isNewChunk());
    }
    @SubscribeEvent public static void chunkDataLoad(net.neoforged.neoforge.event.level.ChunkDataEvent.Load event) {
        HomeTerrain.readMarker(event.getChunk(), event.getData());
    }
    @SubscribeEvent public static void chunkDataSave(net.neoforged.neoforge.event.level.ChunkDataEvent.Save event) {
        HomeTerrain.writeMarker(event.getChunk(), event.getData());
    }
    @SubscribeEvent public static void travel(EntityTravelToDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer traveller && HomeManager.authorizedTravel(traveller)) return;
        if (HomeMobTransport.authorized(event.getEntity())) return;
        var owner = HomeDimensions.owner(event.getDimension());
        if (owner != null && (!(event.getEntity() instanceof ServerPlayer player) || !HomeManager.allowed(player.server, player.getUUID(), owner))) event.setCanceled(true);
        // Portals cannot manufacture linked overworld portals from a private dimension.
        if (HomeDimensions.isHome(event.getEntity().level()) && owner == null) event.setCanceled(true);
    }
    @SubscribeEvent public static void breakBlock(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player && !HomeManager.canInteract(player, event.getPos())) event.setCanceled(true);
    }
    @SubscribeEvent public static void place(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !HomeManager.canInteract(player, event.getPos())) event.setCanceled(true);
    }
    @SubscribeEvent public static void interact(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer player && (!HomeManager.canInteract(player, event.getPos())
                || !HomeManager.canInteract(player, event.getPos().relative(event.getFace() == null ? net.minecraft.core.Direction.UP : event.getFace())))) event.setCanceled(true);
    }
    @SubscribeEvent public static void useItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getEntity() instanceof ServerPlayer player && !HomeManager.canInteract(player, player.blockPosition())) event.setCanceled(true);
    }
}
