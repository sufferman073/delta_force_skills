package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.item.DfsItemQuality;
import com.rzy.dealt_force_skills.shop.DfsShopCatalog;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class S2C_OpenHaffShop implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<S2C_OpenHaffShop> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "s2c_open_haff_shop"));

    public static final StreamCodec<RegistryFriendlyByteBuf, S2C_OpenHaffShop> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    S2C_OpenHaffShop::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final long coins;
    private final List<DfsShopCatalog.Entry> entries;

    public S2C_OpenHaffShop(long coins, List<DfsShopCatalog.Entry> entries) {
        this.coins = Math.max(0L, coins);
        this.entries = entries == null ? List.of() : List.copyOf(entries);
    }

    public static void encode(S2C_OpenHaffShop msg, RegistryFriendlyByteBuf buf) {
        buf.writeLong(msg.coins);
        buf.writeVarInt(msg.entries.size());
        for (DfsShopCatalog.Entry entry : msg.entries) {
            buf.writeUtf(entry.id());
            buf.writeUtf(entry.category().key());
            buf.writeUtf(entry.quality().key());
            buf.writeVarInt(Math.max(0, entry.price()));
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, entry.preview());
        }
    }

    public static S2C_OpenHaffShop decode(RegistryFriendlyByteBuf buf) {
        long coins = buf.readLong();
        int size = Math.max(0, buf.readVarInt());
        List<DfsShopCatalog.Entry> entries = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            String id = buf.readUtf();
            DfsShopCatalog.Category category = DfsShopCatalog.Category.byKey(buf.readUtf());
            DfsItemQuality quality = qualityByKey(buf.readUtf());
            int price = buf.readVarInt();
            ItemStack preview = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
            entries.add(new DfsShopCatalog.Entry(id, category, quality, price, preview));
        }
        return new S2C_OpenHaffShop(coins, entries);
    }

    public static void handle(S2C_OpenHaffShop msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> HaffShopScreen.open(msg.coins, msg.entries));
    }

    private static DfsItemQuality qualityByKey(String key) {
        for (DfsItemQuality quality : DfsItemQuality.values()) {
            if (quality.key().equals(key)) {
                return quality;
            }
        }
        return DfsItemQuality.WHITE;
    }
}
