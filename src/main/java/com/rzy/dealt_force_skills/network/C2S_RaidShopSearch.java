package com.rzy.dealt_force_skills.network;

import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record C2S_RaidShopSearch(int menu, String query, long[] matches) implements CustomPacketPayload {
    public static final Type<C2S_RaidShopSearch> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("dealt_force_skills", "raid_shop_search"));
    public static final StreamCodec<FriendlyByteBuf, C2S_RaidShopSearch> STREAM_CODEC = StreamCodec.of((buf, msg) -> encode(msg, buf), C2S_RaidShopSearch::decode);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void encode(C2S_RaidShopSearch msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.menu); buf.writeUtf(msg.query, 80); buf.writeLongArray(msg.matches);
    }
    public static C2S_RaidShopSearch decode(FriendlyByteBuf buf) {
        return new C2S_RaidShopSearch(buf.readVarInt(), buf.readUtf(80), buf.readLongArray(null, 4096));
    }
    public static void handle(C2S_RaidShopSearch msg, IPayloadContext ctx) {
        
        ctx.enqueueWork(() -> com.rzy.dealt_force_skills.raid.ui.RaidAssetMenus.search(ctx.player() instanceof net.minecraft.server.level.ServerPlayer player ? player : null, msg.menu, msg.query, java.util.BitSet.valueOf(msg.matches)));
        
    }
}
