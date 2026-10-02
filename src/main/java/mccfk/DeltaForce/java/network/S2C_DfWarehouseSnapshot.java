package mccfk.DeltaForce.java.network;

import mccfk.DeltaForce.java.DeltaForce;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import io.netty.handler.codec.DecoderException;

import java.util.ArrayList;
import java.util.List;

/**
 * S2C: 服务端→客户端 - 仓库快照（页码 + 页容器名 + 页总格数 + 仓库内容）
 */
public record S2C_DfWarehouseSnapshot(int page, String container, int pageSlots, List<ItemStack> warehouse) implements CustomPacketPayload {
    private static final int MAX_WAREHOUSE_SLOTS = 4096;
    public static final Type<S2C_DfWarehouseSnapshot> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(DeltaForce.MODID, "s2c_df_warehouse_snapshot"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2C_DfWarehouseSnapshot> STREAM_CODEC = StreamCodec.of(
            S2C_DfWarehouseSnapshot::encode, S2C_DfWarehouseSnapshot::decode);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(RegistryFriendlyByteBuf buf, S2C_DfWarehouseSnapshot msg) {
        buf.writeVarInt(msg.page());
        buf.writeUtf(msg.container(), 64);
        buf.writeVarInt(msg.pageSlots());
        buf.writeVarInt(msg.warehouse().size());
        for (ItemStack stack : msg.warehouse()) {
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, stack);
        }
    }

    private static S2C_DfWarehouseSnapshot decode(RegistryFriendlyByteBuf buf) {
        int page = buf.readVarInt();
        String container = buf.readUtf(64);
        int pageSlots = buf.readVarInt();
        int size = buf.readVarInt();
        if (page < 0 || pageSlots < 0 || pageSlots > MAX_WAREHOUSE_SLOTS
                || size < 0 || size > MAX_WAREHOUSE_SLOTS || size > pageSlots) {
            throw new DecoderException("Invalid warehouse snapshot sizes: " + pageSlots + "/" + size);
        }
        List<ItemStack> stacks = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            stacks.add(ItemStack.OPTIONAL_STREAM_CODEC.decode(buf));
        }
        return new S2C_DfWarehouseSnapshot(page, container, pageSlots, stacks);
    }

    public S2C_DfWarehouseSnapshot {
        container = container == null ? "" : container;
        warehouse = warehouse == null ? List.of() : List.copyOf(warehouse);
        if (page < 0 || pageSlots < 0 || pageSlots > MAX_WAREHOUSE_SLOTS
                || warehouse.size() > pageSlots || warehouse.size() > MAX_WAREHOUSE_SLOTS) {
            throw new IllegalArgumentException("Invalid warehouse snapshot sizes");
        }
    }

    public static void handle(S2C_DfWarehouseSnapshot msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientPacketBridge.openWarehouse(msg));
    }
}