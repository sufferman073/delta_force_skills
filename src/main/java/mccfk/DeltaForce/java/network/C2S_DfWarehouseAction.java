package mccfk.DeltaForce.java.network;

import com.rzy.dealt_force_skills.network.NetworkHandler;
import io.netty.handler.codec.DecoderException;
import mccfk.DeltaForce.java.data.DfWarehouseManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record C2S_DfWarehouseAction(Action action, int index, int target, int count) {
    private static final int MAX_TRANSFER_COUNT = 4096;
    public enum Action { OPEN, DEPOSIT, WITHDRAW, EQUIP, UNEQUIP, EQUIP_WH, UNEQUIP_WH, MOVE_INV, MOVE_WH, DEPOSIT_AUTO, WITHDRAW_AUTO }

    public static void encode(C2S_DfWarehouseAction message, FriendlyByteBuf buffer) {
        buffer.writeVarInt(message.action.ordinal());
        buffer.writeVarInt(message.index);
        buffer.writeVarInt(message.target);
        buffer.writeVarInt(message.count);
    }

    public static C2S_DfWarehouseAction decode(FriendlyByteBuf buffer) {
        int ordinal = buffer.readVarInt();
        Action[] values = Action.values();
        if (ordinal < 0 || ordinal >= values.length) throw new DecoderException("Unknown warehouse action: " + ordinal);
        return new C2S_DfWarehouseAction(values[ordinal], buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt());
    }

    public static void handle(C2S_DfWarehouseAction message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null && com.rzy.dealt_force_skills.raid.RaidAccess.externalItemsBlocked(player)) return;
            if (player == null || message.action != Action.OPEN
                    && (message.count < 0 || message.count > MAX_TRANSFER_COUNT)) return;
            boolean changed = switch (message.action) {
                case OPEN -> true;
                case DEPOSIT -> DfWarehouseManager.depositTo(player, message.index, message.target, message.count);
                case WITHDRAW -> DfWarehouseManager.withdrawTo(player, message.index, message.target, message.count);
                case EQUIP -> DfWarehouseManager.equipFromInventory(player, message.index, message.target, message.count);
                case UNEQUIP -> DfWarehouseManager.unequipToInventory(player, message.index, message.target);
                case EQUIP_WH -> DfWarehouseManager.equipFromWarehouse(player, message.index, message.target, message.count);
                case UNEQUIP_WH -> DfWarehouseManager.unequipToWarehouse(player, message.index, message.target);
                case MOVE_INV -> DfWarehouseManager.moveInInventory(player, message.index, message.target, message.count);
                case MOVE_WH -> DfWarehouseManager.moveInWarehouse(player, message.index, message.target, message.count);
                case DEPOSIT_AUTO -> DfWarehouseManager.deposit(player, message.index, message.count);
                case WITHDRAW_AUTO -> DfWarehouseManager.withdraw(player, message.index, message.count);
            };
            if (changed) sendSnapshot(player);
        });
        context.setPacketHandled(true);
    }

    private static void sendSnapshot(ServerPlayer player) {
        NetworkHandler.sendToPlayer(new S2C_DfWarehouseSnapshot(DfWarehouseManager.warehousePage(player),
                DfWarehouseManager.warehouseContainer(player), DfWarehouseManager.totalSlots(player),
                DfWarehouseManager.loadWarehouse(player)), player);
    }
}
