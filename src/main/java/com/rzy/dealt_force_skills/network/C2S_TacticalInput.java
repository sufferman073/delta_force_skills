package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.motion.TacticalMovementInput;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record C2S_TacticalInput(int forward, int left, boolean sprint) {

    public static void encode(C2S_TacticalInput msg, FriendlyByteBuf buf) {
        buf.writeByte(msg.forward); buf.writeByte(msg.left); buf.writeBoolean(msg.sprint);
    }
    public static C2S_TacticalInput decode(FriendlyByteBuf buf) {
        return new C2S_TacticalInput(buf.readByte(), buf.readByte(), buf.readBoolean());
    }
    public static void handle(C2S_TacticalInput msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) TacticalMovementInput.update(player, msg.forward, msg.left, msg.sprint);
        });
        ctx.get().setPacketHandled(true);
    }
}
