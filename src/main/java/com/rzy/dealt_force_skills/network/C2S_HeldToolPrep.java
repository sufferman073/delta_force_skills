package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.skill.HeldToolVisualSync;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record C2S_HeldToolPrep(int kind) {
    public static void encode(C2S_HeldToolPrep message, FriendlyByteBuf buffer) {
        buffer.writeByte(message.kind);
    }

    public static C2S_HeldToolPrep decode(FriendlyByteBuf buffer) {
        return new C2S_HeldToolPrep(buffer.readUnsignedByte());
    }

    public static void handle(C2S_HeldToolPrep message, Supplier<NetworkEvent.Context> context) {
        ServerPlayer player = context.get().getSender();
        if (player != null) HeldToolVisualSync.queueInput(player, message.kind);
        context.get().setPacketHandled(true);
    }
}
