package mccfk.DeltaForce.java.network;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import mccfk.DeltaForce.java.DeltaForce;

/**
 * 网络包注册器
 */
@EventBusSubscriber(modid = DeltaForce.MODID)
public class DeltaForceNetwork {

    @SubscribeEvent
    public static void registerPackets(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(DeltaForce.MODID).versioned("2");

        // C2S: 客户端→服务端 - 创建标点
        registrar.playToServer(
            C2S_CreateMarker.TYPE,
            C2S_CreateMarker.CODEC,
            (payload, context) -> {
                context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) {
                        MarkerManager.validateRequest(player, payload.marker()).ifPresent(marker -> {
                            if (MarkerManager.addMarker(player.getUUID(), marker)) {
                                broadcastMarkers(player.getServer());
                            }
                        });
                    }
                });
            }
        );

        // S2C: 服务端→客户端 - 同步标点
        registrar.playToClient(
            S2C_SyncMarkers.TYPE,
            S2C_SyncMarkers.CODEC,
            (payload, context) -> {
                // 更新客户端存储
                ClientMarkerStorage.updateMarkers(payload.markers());
            }
        );

        // C2S: 客户端→服务端 - 仓库操作（打开/存入/取出）
        registrar.playToServer(
            C2S_DfWarehouseAction.TYPE,
            C2S_DfWarehouseAction.STREAM_CODEC,
            (payload, context) -> C2S_DfWarehouseAction.handle(payload, context)
        );

        // S2C: 服务端→客户端 - 仓库快照
        registrar.playToClient(
            S2C_DfWarehouseSnapshot.TYPE,
            S2C_DfWarehouseSnapshot.STREAM_CODEC,
            (payload, context) -> S2C_DfWarehouseSnapshot.handle(payload, context)
        );

    }

    public static void syncMarkers(ServerPlayer player) {
        player.connection.send(new S2C_SyncMarkers(java.util.List.copyOf(MarkerManager.getAllMarkers())));
    }

    public static void broadcastMarkers(MinecraftServer server) {
        if (server == null) {
            return;
        }
        var syncPacket = new S2C_SyncMarkers(java.util.List.copyOf(MarkerManager.getAllMarkers()));
        server.getPlayerList().getPlayers().forEach(player -> player.connection.send(syncPacket));
    }

}