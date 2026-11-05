package com.rzy.dealt_force_skills.network;

import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import java.util.UUID;

/** Relative server age avoids subtracting independently advancing client/server clocks. */
public record S2C_TacticalMotion(UUID playerId, int action, long sequence, int age, int duration,
                                int phase, float yaw, int impactTick) implements CustomPacketPayload {
    public static final Type<S2C_TacticalMotion> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("dealt_force_skills", "tactical_motion"));
    public static final StreamCodec<FriendlyByteBuf, S2C_TacticalMotion> STREAM_CODEC = StreamCodec.of((b, m) -> encode(m, b), S2C_TacticalMotion::decode);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void encode(S2C_TacticalMotion m, FriendlyByteBuf b) {
        b.writeUUID(m.playerId); b.writeVarInt(m.action); b.writeVarLong(m.sequence);
        b.writeVarInt(m.age); b.writeVarInt(m.duration); b.writeVarInt(m.phase);
        b.writeFloat(m.yaw); b.writeVarInt(m.impactTick);
    }
    public static S2C_TacticalMotion decode(FriendlyByteBuf b) {
        return new S2C_TacticalMotion(b.readUUID(), b.readVarInt(), b.readVarLong(), b.readVarInt(),
                b.readVarInt(), b.readVarInt(), b.readFloat(), b.readVarInt());
    }
    public static void handle(S2C_TacticalMotion m, IPayloadContext c) {
        c.enqueueWork(() -> ClientPacketBridge.invoke(
                "com.rzy.dealt_force_skills.client.visual.ClientTacticalMotion", "receive", m));
    }
}
