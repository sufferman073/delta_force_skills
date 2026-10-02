package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.shop.GhrothArmoryCatalog;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class S2C_OpenGhrothArmory implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<S2C_OpenGhrothArmory> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "s2c_open_ghroth_armory"));

    public static final StreamCodec<RegistryFriendlyByteBuf, S2C_OpenGhrothArmory> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    S2C_OpenGhrothArmory::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final long coins;
    private final List<GhrothArmoryCatalog.Entry> entries;

    public S2C_OpenGhrothArmory(long coins, List<GhrothArmoryCatalog.Entry> entries) {
        this.coins = Math.max(0L, coins);
        this.entries = entries == null ? List.of() : List.copyOf(entries);
    }

    public static void encode(S2C_OpenGhrothArmory msg, RegistryFriendlyByteBuf buf) {
        buf.writeLong(msg.coins);
        buf.writeVarInt(msg.entries.size());
        for (GhrothArmoryCatalog.Entry entry : msg.entries) {
            buf.writeUtf(entry.id());
            buf.writeUtf(entry.categoryKey());
            buf.writeVarInt(Math.max(0, entry.price()));
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, entry.preview());
        }
    }

    public static S2C_OpenGhrothArmory decode(RegistryFriendlyByteBuf buf) {
        long coins = buf.readLong();
        int size = Math.max(0, buf.readVarInt());
        List<GhrothArmoryCatalog.Entry> entries = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            String id = buf.readUtf();
            String category = buf.readUtf();
            int price = buf.readVarInt();
            ItemStack preview = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
            entries.add(new GhrothArmoryCatalog.Entry(id, category, price, preview));
        }
        return new S2C_OpenGhrothArmory(coins, entries);
    }

    public static void handle(S2C_OpenGhrothArmory msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> GhrothArmoryScreen.open(msg.coins, msg.entries));
    }
}
