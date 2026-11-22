package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.shop.GtiTradingHallManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record C2S_GtiSpecialOperationsAction(Action action, int index, int count) {
    public enum Action { OPEN_BASE, OPEN_MARKET, BUY }

    public static void encode(C2S_GtiSpecialOperationsAction msg, FriendlyByteBuf buf) {
        buf.writeEnum(msg.action); buf.writeVarInt(msg.index); buf.writeVarInt(msg.count);
    }
    public static C2S_GtiSpecialOperationsAction decode(FriendlyByteBuf buf) {
        return new C2S_GtiSpecialOperationsAction(buf.readEnum(Action.class), buf.readVarInt(), buf.readVarInt());
    }
    public static void handle(C2S_GtiSpecialOperationsAction msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null && com.rzy.dealt_force_skills.raid.RaidAccess.externalItemsBlocked(player)) return;
            if (player == null) return;
            boolean gtiAccess = GtiTradingHallManager.hasBaseAccess(player);
            switch (msg.action) {
                case OPEN_BASE -> {
                    if (gtiAccess) {
                        NetworkHandler.sendToPlayer(S2C_SpecialOperationsBaseSnapshot.capture(player, 0), player);
                    }
                }
                case OPEN_MARKET -> GtiTradingHallManager.open(player);
                case BUY -> GtiTradingHallManager.buy(player, msg.index, msg.count);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
