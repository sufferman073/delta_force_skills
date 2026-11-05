package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.home.HomeManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.UUID;
import java.util.function.Supplier;

public record C2S_HomeAction(int action, UUID target, int amount) {
    public static void encode(C2S_HomeAction msg, FriendlyByteBuf buf) { buf.writeByte(msg.action); buf.writeUUID(msg.target); buf.writeVarInt(msg.amount); }
    public static C2S_HomeAction decode(FriendlyByteBuf buf) { return new C2S_HomeAction(buf.readUnsignedByte(), buf.readUUID(), buf.readVarInt()); }
    public static void handle(C2S_HomeAction msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> { var player = ctx.get().getSender(); if (player != null) HomeManager.handle(player, msg.action, msg.target, msg.amount); });
        ctx.get().setPacketHandled(true);
    }
}
