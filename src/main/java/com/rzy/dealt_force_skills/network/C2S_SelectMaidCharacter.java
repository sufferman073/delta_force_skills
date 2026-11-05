package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.compat.maid.MaidSelection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.UUID;
import java.util.function.Supplier;

public record C2S_SelectMaidCharacter(UUID token, String characterId) {
    public static void encode(C2S_SelectMaidCharacter msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.token); buf.writeUtf(msg.characterId, 256);
    }
    public static C2S_SelectMaidCharacter decode(FriendlyByteBuf buf) {
        return new C2S_SelectMaidCharacter(buf.readUUID(), buf.readUtf(256));
    }
    public static void handle(C2S_SelectMaidCharacter msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            var player = ctx.get().getSender();
            if (player != null) MaidSelection.submit(player, msg.token, msg.characterId);
        });
        ctx.get().setPacketHandled(true);
    }
}
