package com.rzy.dealt_force_skills.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record S2C_SyncHeldToolVisual(int entityId, int visualId, int elapsedTicks, int durationTicks) {
    public S2C_SyncHeldToolVisual(int entityId, int visualId) {
        this(entityId, visualId, -1, 0);
    }

    public static void encode(S2C_SyncHeldToolVisual msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.entityId);
        buf.writeVarInt(msg.visualId);
        if (msg.visualId >= 0 && msg.elapsedTicks >= 0) {
            buf.writeVarInt(msg.elapsedTicks);
            buf.writeVarInt(msg.durationTicks);
        }
    }

    public static S2C_SyncHeldToolVisual decode(FriendlyByteBuf buf) {
        int entityId = buf.readVarInt(), visualId = buf.readVarInt();
        return visualId >= 0 && buf.isReadable()
                ? new S2C_SyncHeldToolVisual(entityId, visualId, buf.readVarInt(), buf.readVarInt())
                : new S2C_SyncHeldToolVisual(entityId, visualId);
    }

    public static void handle(S2C_SyncHeldToolVisual msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientHeldToolVisualState.sync(msg.entityId, msg.visualId, msg.elapsedTicks, msg.durationTicks)
        ));
        ctx.get().setPacketHandled(true);
    }
}
