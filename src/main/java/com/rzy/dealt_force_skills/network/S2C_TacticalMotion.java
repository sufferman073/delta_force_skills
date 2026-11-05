package com.rzy.dealt_force_skills.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.UUID;
import java.util.function.Supplier;

/** Relative server age avoids subtracting independently advancing client/server clocks. */
public record S2C_TacticalMotion(UUID playerId, int action, long sequence, int age, int duration,
                                int phase, float yaw, int impactTick) {
    public static void encode(S2C_TacticalMotion m, FriendlyByteBuf b) {
        b.writeUUID(m.playerId); b.writeVarInt(m.action); b.writeVarLong(m.sequence);
        b.writeVarInt(m.age); b.writeVarInt(m.duration); b.writeVarInt(m.phase);
        b.writeFloat(m.yaw); b.writeVarInt(m.impactTick);
    }
    public static S2C_TacticalMotion decode(FriendlyByteBuf b) {
        return new S2C_TacticalMotion(b.readUUID(), b.readVarInt(), b.readVarLong(), b.readVarInt(),
                b.readVarInt(), b.readVarInt(), b.readFloat(), b.readVarInt());
    }
    public static void handle(S2C_TacticalMotion m, Supplier<NetworkEvent.Context> c) {
        c.get().enqueueWork(() -> ClientPacketBridge.invoke(
                "com.rzy.dealt_force_skills.client.visual.ClientTacticalMotion", "receive", m));
        c.get().setPacketHandled(true);
    }
}
