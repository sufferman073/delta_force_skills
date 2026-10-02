package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.shop.DealtShopAccessManager;
import com.rzy.dealt_force_skills.shop.GtiTradingHallManager;
import com.rzy.dealt_force_skills.shop.SpecialOperationsBaseManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record C2S_SpecialOperationsBaseAction(Action action, String target, int index, int count,
                                               long amount, int page) implements CustomPacketPayload {
    public static final Type<C2S_SpecialOperationsBaseAction> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "c2s_special_operations_base_action"));
    public static final StreamCodec<FriendlyByteBuf, C2S_SpecialOperationsBaseAction> STREAM_CODEC =
            StreamCodec.of((buf, msg) -> encode(msg, buf), C2S_SpecialOperationsBaseAction::decode);

    public enum Action {
        OPEN, PAGE, UPGRADE, SELL, REDEEM, DEPOSIT, WITHDRAW, EXCHANGE
    }

    public static C2S_SpecialOperationsBaseAction open() {
        return new C2S_SpecialOperationsBaseAction(Action.OPEN, "", 0, 0, 0L, 0);
    }

    public static C2S_SpecialOperationsBaseAction page(int page) {
        return new C2S_SpecialOperationsBaseAction(Action.PAGE, "", 0, 0, 0L, page);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(C2S_SpecialOperationsBaseAction msg, FriendlyByteBuf buf) {
        buf.writeEnum(msg.action);
        buf.writeUtf(msg.target == null ? "" : msg.target);
        buf.writeVarInt(msg.index);
        buf.writeVarInt(msg.count);
        buf.writeLong(msg.amount);
        buf.writeVarInt(msg.page);
    }

    private static C2S_SpecialOperationsBaseAction decode(FriendlyByteBuf buf) {
        return new C2S_SpecialOperationsBaseAction(buf.readEnum(Action.class), buf.readUtf(256),
                buf.readVarInt(), buf.readVarInt(), buf.readLong(), buf.readVarInt());
    }

    public static void handle(C2S_SpecialOperationsBaseAction msg, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            if (player == null) return;
            boolean gtiAccess = GtiTradingHallManager.hasBaseAccess(player);
            if (!gtiAccess && !DealtShopAccessManager.isAllowed(DealtShopAccessManager.Service.BASE, player)) return;
            boolean success = false;
            switch (msg.action) {
                case OPEN, PAGE -> { }
                case UPGRADE -> {
                    var facility = SpecialOperationsBaseManager.Facility.byId(msg.target);
                    if (facility != null) success = SpecialOperationsBaseManager.upgrade(player, facility);
                }
                case SELL -> {
                    try { success = SpecialOperationsBaseManager.sell(player,
                            SpecialOperationsBaseManager.ItemSource.valueOf(msg.target), msg.index, msg.count) > 0L; }
                    catch (IllegalArgumentException ignored) { }
                }
                case REDEEM -> success = SpecialOperationsBaseManager.redeem(player, msg.index, msg.count, gtiAccess);
                // This port uses the shared warehouse menu; legacy in-base storage actions stay unsupported.
                case DEPOSIT, WITHDRAW -> { }
                case EXCHANGE -> success = SpecialOperationsBaseManager.exchange(player, msg.target, msg.amount);
            }
            if (msg.action != Action.OPEN && msg.action != Action.PAGE && msg.action != Action.UPGRADE) {
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable(success
                        ? "message.dealt_force_skills.transaction.success" : "message.dealt_force_skills.transaction.failed"), true);
            }
            NetworkHandler.sendToPlayer(S2C_SpecialOperationsBaseSnapshot.capture(player, msg.page), player);
        });
    }
}
