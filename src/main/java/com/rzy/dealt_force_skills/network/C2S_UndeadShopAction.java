package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.shop.UndeadShopAction;
import com.rzy.dealt_force_skills.shop.UndeadShopManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record C2S_UndeadShopAction(String entryId, UndeadShopAction action) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<C2S_UndeadShopAction> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "c2s_undead_shop_action"));

    public static final StreamCodec<FriendlyByteBuf, C2S_UndeadShopAction> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    C2S_UndeadShopAction::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(C2S_UndeadShopAction msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.entryId, 64);
        buf.writeVarInt(msg.action.ordinal());
    }

    public static C2S_UndeadShopAction decode(FriendlyByteBuf buf) {
        String entryId = buf.readUtf(64);
        int ordinal = buf.readVarInt();
        UndeadShopAction[] values = UndeadShopAction.values();
        UndeadShopAction action = ordinal >= 0 && ordinal < values.length
                ? values[ordinal]
                : UndeadShopAction.BUY;
        return new C2S_UndeadShopAction(entryId, action);
    }

    public static void handle(C2S_UndeadShopAction msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player != null) {
                UndeadShopManager.handleAction(player, msg.entryId, msg.action);
            }
        });
    }
}
