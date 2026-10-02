package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.config.DealtItemsHavocConfig;
import com.rzy.dealt_force_skills.shop.SpecialOperationsBaseManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.ArrayList;
import java.util.List;
import io.netty.handler.codec.DecoderException;

public record S2C_SpecialOperationsBaseSnapshot(
        long havocCoins, int warehousePage, int warehousePages, int warehouseSlotsPerPage,
        List<FacilityView> facilities, List<ItemStack> warehouse, List<ItemStack> inventory,
        List<ItemStack> container, List<BuybackView> buyback) implements CustomPacketPayload {
    private static final int MAX_FACILITIES = 64;
    private static final int MAX_MATERIALS_PER_FACILITY = 64;
    private static final int MAX_STACKS = 4096;
    private static final int MAX_BUYBACK = 4096;
    public static final Type<S2C_SpecialOperationsBaseSnapshot> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "s2c_special_operations_base_snapshot"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2C_SpecialOperationsBaseSnapshot> STREAM_CODEC =
            StreamCodec.of((buf, msg) -> encode(msg, buf), S2C_SpecialOperationsBaseSnapshot::decode);

    public record MaterialView(String itemId, int count, int missingCount) {
    }

    public record FacilityView(String id, int level, int maxLevel, int nextLevel, long nextCost,
                               int requiredWarehouseLevel, boolean allowed, List<MaterialView> materials) {
    }

    public record BuybackView(int soldIndex, ItemStack stack, long price) {
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static S2C_SpecialOperationsBaseSnapshot capture(ServerPlayer player, int requestedPage) {
        SpecialOperationsBaseManager.synchronizeTeamSoldStacks(player);
        int pages = SpecialOperationsBaseManager.warehousePages(player);
        int page = pages <= 0 ? 0 : Math.max(0, Math.min(pages - 1, requestedPage));
        List<FacilityView> facilities = new ArrayList<>();
        for (SpecialOperationsBaseManager.Facility facility : SpecialOperationsBaseManager.Facility.values()) {
            SpecialOperationsBaseManager.UpgradeQuote quote = SpecialOperationsBaseManager.quote(player, facility);
            List<MaterialView> materials = quote.materials().stream().map(cost -> {
                ResourceLocation id = BuiltInRegistries.ITEM.getKey(cost.item());
                int available = SpecialOperationsBaseManager.availableMaterialCount(player, cost.item());
                return new MaterialView(id.toString(), cost.count(), Math.max(0, cost.count() - available));
            }).toList();
            facilities.add(new FacilityView(facility.id(), quote.currentLevel(), facility.maxLevel(),
                    quote.nextLevel(), quote.havocCoins(), quote.requiredWarehouseLevel(), quote.levelAllowed(), materials));
        }
        List<BuybackView> buyback = new ArrayList<>();
        int commandLevel = SpecialOperationsBaseManager.effectiveLevel(player,
                SpecialOperationsBaseManager.Facility.COMMAND_CENTER);
        double multiplier = Math.max(1.0D, DealtItemsHavocConfig.buybackMultiplier() - commandLevel * 0.1D);
        List<ItemStack> sold = SpecialOperationsBaseManager.soldStacks(player);
        for (int index = 0; index < sold.size(); index++) {
            ItemStack stack = sold.get(index);
            Item item = stack.getItem();
            if (!stack.isEmpty() && DealtItemsHavocConfig.isSellable(player, stack)) {
                buyback.add(new BuybackView(index, stack.copy(),
                        Math.max(1L, (long) Math.ceil(DealtItemsHavocConfig.price(player, stack) * multiplier))));
            }
        }
        // 特勤处内部仓库已删除，传空列表（但保留页码信息用于设施联动）
        return new S2C_SpecialOperationsBaseSnapshot(SpecialOperationsBaseManager.havocCoins(player), page, pages,
                SpecialOperationsBaseManager.warehouseSlotsPerPage(player), facilities,
                List.of(),
                SpecialOperationsBaseManager.sellableInventory(player),
                SpecialOperationsBaseManager.sellableContainer(player), buyback);
    }

    private static void encode(S2C_SpecialOperationsBaseSnapshot msg, RegistryFriendlyByteBuf buf) {
        buf.writeLong(msg.havocCoins);
        buf.writeVarInt(msg.warehousePage);
        buf.writeVarInt(msg.warehousePages);
        buf.writeVarInt(msg.warehouseSlotsPerPage);
        buf.writeVarInt(msg.facilities.size());
        for (FacilityView view : msg.facilities) {
            buf.writeUtf(view.id, 64);
            buf.writeVarInt(view.level);
            buf.writeVarInt(view.maxLevel);
            buf.writeVarInt(view.nextLevel);
            buf.writeLong(view.nextCost);
            buf.writeVarInt(view.requiredWarehouseLevel);
            buf.writeBoolean(view.allowed);
            buf.writeVarInt(view.materials.size());
            for (MaterialView material : view.materials) {
                buf.writeUtf(material.itemId, 256);
                buf.writeVarInt(material.count);
                buf.writeVarInt(material.missingCount);
            }
        }
        writeStacks(buf, msg.warehouse);
        writeStacks(buf, msg.inventory);
        writeStacks(buf, msg.container);
        buf.writeVarInt(msg.buyback.size());
        for (BuybackView view : msg.buyback) {
            buf.writeVarInt(view.soldIndex);
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, view.stack);
            buf.writeLong(view.price);
        }
    }

    private static S2C_SpecialOperationsBaseSnapshot decode(RegistryFriendlyByteBuf buf) {
        long havocCoins = buf.readLong();
        int page = buf.readVarInt();
        int pages = buf.readVarInt();
        int pageSize = buf.readVarInt();
        int facilityCount = buf.readVarInt();
        if (page < 0 || pages < 0 || pages > 64 || pageSize < 0 || pageSize > MAX_STACKS
                || facilityCount < 0 || facilityCount > MAX_FACILITIES) {
            throw new DecoderException("Invalid special operations snapshot header");
        }
        List<FacilityView> facilities = new ArrayList<>(facilityCount);
        for (int i = 0; i < facilityCount; i++) {
            String id = buf.readUtf(64);
            int level = buf.readVarInt();
            int max = buf.readVarInt();
            int next = buf.readVarInt();
            long cost = buf.readLong();
            int requiredWarehouseLevel = buf.readVarInt();
            boolean allowed = buf.readBoolean();
            int materialCount = buf.readVarInt();
            if (materialCount < 0 || materialCount > MAX_MATERIALS_PER_FACILITY) {
                throw new DecoderException("Invalid material count: " + materialCount);
            }
            List<MaterialView> materials = new ArrayList<>(materialCount);
            for (int j = 0; j < materialCount; j++) {
                materials.add(new MaterialView(buf.readUtf(256), buf.readVarInt(), buf.readVarInt()));
            }
            facilities.add(new FacilityView(id, level, max, next, cost, requiredWarehouseLevel,
                    allowed, List.copyOf(materials)));
        }
        List<ItemStack> warehouse = readStacks(buf);
        List<ItemStack> inventory = readStacks(buf);
        List<ItemStack> container = readStacks(buf);
        int buybackCount = buf.readVarInt();
        if (buybackCount < 0 || buybackCount > MAX_BUYBACK) {
            throw new DecoderException("Invalid buyback count: " + buybackCount);
        }
        List<BuybackView> buyback = new ArrayList<>(buybackCount);
        for (int i = 0; i < buybackCount; i++) {
            buyback.add(new BuybackView(buf.readVarInt(), ItemStack.OPTIONAL_STREAM_CODEC.decode(buf), buf.readLong()));
        }
        return new S2C_SpecialOperationsBaseSnapshot(havocCoins, page, pages, pageSize,
                List.copyOf(facilities), warehouse, inventory, container, List.copyOf(buyback));
    }

    public static void handle(S2C_SpecialOperationsBaseSnapshot msg, IPayloadContext context) {
        context.enqueueWork(() -> ClientPacketHandlers.openSpecialOperationsBase(msg));
    }

    private static void writeStacks(RegistryFriendlyByteBuf buf, List<ItemStack> stacks) {
        buf.writeVarInt(stacks.size());
        for (ItemStack stack : stacks) {
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, stack);
        }
    }

    private static List<ItemStack> readStacks(RegistryFriendlyByteBuf buf) {
        int size = buf.readVarInt();
        if (size < 0 || size > MAX_STACKS) {
            throw new DecoderException("Invalid stack count: " + size);
        }
        List<ItemStack> stacks = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            stacks.add(ItemStack.OPTIONAL_STREAM_CODEC.decode(buf));
        }
        return List.copyOf(stacks);
    }
}
