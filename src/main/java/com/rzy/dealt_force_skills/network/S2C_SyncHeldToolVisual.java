package com.rzy.dealt_force_skills.network;

import net.minecraft.network.FriendlyByteBuf;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record S2C_SyncHeldToolVisual(int entityId, int visualId, int elapsedTicks, int durationTicks) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<S2C_SyncHeldToolVisual> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "s2c_sync_held_tool_visual"));

    public static final StreamCodec<FriendlyByteBuf, S2C_SyncHeldToolVisual> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    S2C_SyncHeldToolVisual::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

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

    public static void handle(S2C_SyncHeldToolVisual msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientHeldToolVisualState.sync(msg.entityId, msg.visualId, msg.elapsedTicks, msg.durationTicks));
    }
}
