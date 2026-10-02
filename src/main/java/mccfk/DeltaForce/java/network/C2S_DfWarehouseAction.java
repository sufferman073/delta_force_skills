package mccfk.DeltaForce.java.network;

import mccfk.DeltaForce.java.DeltaForce;
import mccfk.DeltaForce.java.data.DfWarehouseManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import io.netty.handler.codec.DecoderException;

/**
 * C2S: 客户端→服务端 - 仓库操作
 * OPEN：请求仓库快照；DEPOSIT：物品栏→仓库精确格；WITHDRAW：仓库→物品栏精确格；
 * EQUIP：物品栏→装备槽；UNEQUIP：装备槽→物品栏；EQUIP_WH：仓库→装备槽；UNEQUIP_WH：装备槽→仓库
 * 参数：index=源格索引，target=目标格索引，count=数量
 */
public record C2S_DfWarehouseAction(Action action, int index, int target, int count) implements CustomPacketPayload {
    private static final int MAX_TRANSFER_COUNT = 4096;
    public static final Type<C2S_DfWarehouseAction> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(DeltaForce.MODID, "c2s_df_warehouse_action"));
    public static final StreamCodec<FriendlyByteBuf, C2S_DfWarehouseAction> STREAM_CODEC = StreamCodec.of(
            C2S_DfWarehouseAction::encode, C2S_DfWarehouseAction::decode);

    public enum Action { OPEN, DEPOSIT, WITHDRAW, EQUIP, UNEQUIP, EQUIP_WH, UNEQUIP_WH, MOVE_INV, MOVE_WH, DEPOSIT_AUTO, WITHDRAW_AUTO }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(FriendlyByteBuf buf, C2S_DfWarehouseAction msg) {
        buf.writeEnum(msg.action);
        buf.writeVarInt(msg.index);
        buf.writeVarInt(msg.target);
        buf.writeVarInt(msg.count);
    }

    private static C2S_DfWarehouseAction decode(FriendlyByteBuf buf) {
        int ordinal = buf.readVarInt();
        Action[] actions = Action.values();
        if (ordinal < 0 || ordinal >= actions.length) {
            throw new DecoderException("Unknown warehouse action: " + ordinal);
        }
        return new C2S_DfWarehouseAction(actions[ordinal], buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(C2S_DfWarehouseAction msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) {
                return;
            }
            if (msg.action() != Action.OPEN && (msg.count() < 0 || msg.count() > MAX_TRANSFER_COUNT)) {
                return;
            }
            switch (msg.action()) {
                case OPEN -> sendSnapshot(player);
                case DEPOSIT -> {
                    if (DfWarehouseManager.depositTo(player, msg.index(), msg.target(), msg.count())) {
                        sendSnapshot(player);
                    }
                }
                case WITHDRAW -> {
                    if (DfWarehouseManager.withdrawTo(player, msg.index(), msg.target(), msg.count())) {
                        sendSnapshot(player);
                    }
                }
                case EQUIP -> {
                    if (DfWarehouseManager.equipFromInventory(player, msg.index(), msg.target(), msg.count())) {
                        sendSnapshot(player);
                    }
                }
                case UNEQUIP -> {
                    if (DfWarehouseManager.unequipToInventory(player, msg.index(), msg.target())) {
                        sendSnapshot(player);
                    }
                }
                case EQUIP_WH -> {
                    if (DfWarehouseManager.equipFromWarehouse(player, msg.index(), msg.target(), msg.count())) {
                        sendSnapshot(player);
                    }
                }
                case UNEQUIP_WH -> {
                    if (DfWarehouseManager.unequipToWarehouse(player, msg.index(), msg.target())) {
                        sendSnapshot(player);
                    }
                }
                case MOVE_INV -> {
                    if (DfWarehouseManager.moveInInventory(player, msg.index(), msg.target(), msg.count())) {
                        sendSnapshot(player);
                    }
                }
                case MOVE_WH -> {
                    if (DfWarehouseManager.moveInWarehouse(player, msg.index(), msg.target(), msg.count())) {
                        sendSnapshot(player);
                    }
                }
                case DEPOSIT_AUTO -> {
                    if (DfWarehouseManager.deposit(player, msg.index(), msg.count())) {
                        sendSnapshot(player);
                    }
                }
                case WITHDRAW_AUTO -> {
                    if (DfWarehouseManager.withdraw(player, msg.index(), msg.count())) {
                        sendSnapshot(player);
                    }
                }
            }
        });
    }

    private static void sendSnapshot(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new S2C_DfWarehouseSnapshot(
                DfWarehouseManager.warehousePage(player),
                DfWarehouseManager.warehouseContainer(player),
                DfWarehouseManager.totalSlots(player),
                DfWarehouseManager.loadWarehouse(player)));
    }
}
