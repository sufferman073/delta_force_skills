package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.shop.DealtShopAccessManager;
import com.rzy.dealt_force_skills.shop.GtiTradingHallManager;
import com.rzy.dealt_force_skills.shop.SpecialOperationsBaseManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record C2S_SpecialOperationsBaseAction(Action action, String target, int index, int count, long amount, int page) {
    public enum Action {
        OPEN,
        PAGE,
        UPGRADE,
        SELL,
        REDEEM,
        DEPOSIT,
        WITHDRAW,
        EXCHANGE
    }

    public static C2S_SpecialOperationsBaseAction open() {
        return new C2S_SpecialOperationsBaseAction(Action.OPEN, "", 0, 0, 0L, 0);
    }

    public static C2S_SpecialOperationsBaseAction page(int page) {
        return new C2S_SpecialOperationsBaseAction(Action.PAGE, "", 0, 0, 0L, page);
    }

    public static void encode(C2S_SpecialOperationsBaseAction msg, FriendlyByteBuf buf) {
        buf.writeEnum(msg.action);
        buf.writeUtf(msg.target == null ? "" : msg.target);
        buf.writeVarInt(msg.index);
        buf.writeVarInt(msg.count);
        buf.writeLong(msg.amount);
        buf.writeVarInt(msg.page);
    }

    public static C2S_SpecialOperationsBaseAction decode(FriendlyByteBuf buf) {
        return new C2S_SpecialOperationsBaseAction(buf.readEnum(Action.class), buf.readUtf(256),
                buf.readVarInt(), buf.readVarInt(), buf.readLong(), buf.readVarInt());
    }

    public static void handle(C2S_SpecialOperationsBaseAction msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
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
                case DEPOSIT -> success = SpecialOperationsBaseManager.depositInventory(player, msg.index, msg.count);
                case WITHDRAW -> success = SpecialOperationsBaseManager.withdrawToInventory(player, msg.index, msg.count);
                case EXCHANGE -> success = SpecialOperationsBaseManager.exchange(player, msg.target, msg.amount);
            }
            if (msg.action == Action.OPEN) com.rzy.dealt_force_skills.department.DepartmentTasks.signal(player,"base.open");
            if (success) {
                switch(msg.action) {
                    case UPGRADE -> com.rzy.dealt_force_skills.department.DepartmentTasks.signal(player,"base.upgrade."+msg.target);
                    case SELL -> com.rzy.dealt_force_skills.department.DepartmentTasks.signal(player,"base.sell");
                    case REDEEM -> com.rzy.dealt_force_skills.department.DepartmentTasks.signal(player,"base.redeem");
                    case DEPOSIT, WITHDRAW -> com.rzy.dealt_force_skills.department.DepartmentTasks.signal(player,"base.warehouse.transfer");
                    default -> { }
                }
            }
            if (msg.action != Action.OPEN && msg.action != Action.PAGE && msg.action != Action.UPGRADE) {
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable(success
                        ? "message.dealt_force_skills.transaction.success" : "message.dealt_force_skills.transaction.failed"), true);
            }
            NetworkHandler.sendToPlayer(S2C_SpecialOperationsBaseSnapshot.capture(player, msg.page), player);
        });
        ctx.get().setPacketHandled(true);
    }
}
