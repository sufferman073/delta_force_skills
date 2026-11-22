package com.rzy.dealt_force_skills.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record C2S_RaidShopSearch(int menu, String query, long[] matches) {
    public static void encode(C2S_RaidShopSearch msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.menu); buf.writeUtf(msg.query, 80); buf.writeLongArray(msg.matches);
    }
    public static C2S_RaidShopSearch decode(FriendlyByteBuf buf) {
        return new C2S_RaidShopSearch(buf.readVarInt(), buf.readUtf(80), buf.readLongArray(null, 4096));
    }
    public static void handle(C2S_RaidShopSearch msg, Supplier<NetworkEvent.Context> context) {
        var ctx = context.get();
        ctx.enqueueWork(() -> com.rzy.dealt_force_skills.raid.ui.RaidAssetMenus.search(ctx.getSender(), msg.menu, msg.query, java.util.BitSet.valueOf(msg.matches)));
        ctx.setPacketHandled(true);
    }
}
