package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.shop.LexNinjiaShopAction;
import com.rzy.dealt_force_skills.shop.LexNinjiaShopManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record C2S_LexNinjiaShopAction(String artId, LexNinjiaShopAction action) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<C2S_LexNinjiaShopAction> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "c2s_lex_ninjia_shop_action"));

    public static final StreamCodec<FriendlyByteBuf, C2S_LexNinjiaShopAction> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    C2S_LexNinjiaShopAction::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(C2S_LexNinjiaShopAction msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.artId, 64);
        buf.writeVarInt(msg.action.ordinal());
    }

    public static C2S_LexNinjiaShopAction decode(FriendlyByteBuf buf) {
        String artId = buf.readUtf(64);
        int ordinal = buf.readVarInt();
        LexNinjiaShopAction[] values = LexNinjiaShopAction.values();
        LexNinjiaShopAction action = ordinal >= 0 && ordinal < values.length
                ? values[ordinal]
                : LexNinjiaShopAction.BUY;
        return new C2S_LexNinjiaShopAction(artId, action);
    }

    public static void handle(C2S_LexNinjiaShopAction msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player != null) {
                LexNinjiaShopManager.handleAction(player, msg.artId, msg.action);
            }
        });
    }
}
