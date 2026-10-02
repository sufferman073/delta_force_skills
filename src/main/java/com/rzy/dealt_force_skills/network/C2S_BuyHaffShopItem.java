package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.shop.HaffShopManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class C2S_BuyHaffShopItem implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<C2S_BuyHaffShopItem> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "c2s_buy_haff_shop_item"));

    public static final StreamCodec<FriendlyByteBuf, C2S_BuyHaffShopItem> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    C2S_BuyHaffShopItem::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final String entryId;

    public C2S_BuyHaffShopItem(String entryId) {
        this.entryId = entryId == null ? "" : entryId;
    }

    public static void encode(C2S_BuyHaffShopItem msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.entryId);
    }

    public static C2S_BuyHaffShopItem decode(FriendlyByteBuf buf) {
        return new C2S_BuyHaffShopItem(buf.readUtf(256));
    }

    public static void handle(C2S_BuyHaffShopItem msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player != null) {
                HaffShopManager.buy(player, msg.entryId);
            }
        });
    }
}
