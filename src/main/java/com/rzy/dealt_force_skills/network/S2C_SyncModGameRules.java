package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.registry.ModGameRules;
import com.rzy.dealt_force_skills.config.DealtItemsHavocConfig;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

public final class S2C_SyncModGameRules implements CustomPacketPayload {
    public static final Type<S2C_SyncModGameRules> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "s2c_sync_mod_game_rules"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2C_SyncModGameRules> STREAM_CODEC = StreamCodec.of(
            S2C_SyncModGameRules::encode, S2C_SyncModGameRules::decode);

    private final boolean armorSpecialsEnabled;
    private final boolean rebuyEnabled;
    private final boolean havocValueVisible;
    private final long[] havocPrices;
    private final List<DealtItemsHavocConfig.VariantPrice> havocVariantPrices;

    public S2C_SyncModGameRules(boolean armorSpecialsEnabled, boolean rebuyEnabled, boolean havocValueVisible,
                                long[] havocPrices,
                                List<DealtItemsHavocConfig.VariantPrice> havocVariantPrices) {
        this.armorSpecialsEnabled = armorSpecialsEnabled;
        this.rebuyEnabled = rebuyEnabled;
        this.havocValueVisible = havocValueVisible;
        this.havocPrices = havocPrices.clone();
        this.havocVariantPrices = List.copyOf(havocVariantPrices);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(RegistryFriendlyByteBuf buf, S2C_SyncModGameRules msg) {
        buf.writeBoolean(msg.armorSpecialsEnabled);
        buf.writeBoolean(msg.rebuyEnabled);
        buf.writeBoolean(msg.havocValueVisible);
        buf.writeVarInt(msg.havocPrices.length);
        for (long price : msg.havocPrices) buf.writeVarLong(price);
        buf.writeVarInt(msg.havocVariantPrices.size());
        for (DealtItemsHavocConfig.VariantPrice variant : msg.havocVariantPrices) {
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, variant.stack());
            buf.writeVarLong(variant.price());
        }
    }

    private static S2C_SyncModGameRules decode(RegistryFriendlyByteBuf buf) {
        boolean armorSpecialsEnabled = buf.readBoolean();
        boolean rebuyEnabled = buf.readBoolean();
        boolean havocValueVisible = buf.readBoolean();
        int size = buf.readVarInt();
        if (size < 0 || size > 1_000_000) throw new IllegalArgumentException("Invalid Havoc price count: " + size);
        long[] havocPrices = new long[size];
        for (int i = 0; i < size; i++) havocPrices[i] = buf.readVarLong();
        int variantCount = buf.readVarInt();
        if (variantCount < 0 || variantCount > 10_000) {
            throw new IllegalArgumentException("Invalid Havoc variant price count: " + variantCount);
        }
        List<DealtItemsHavocConfig.VariantPrice> variants = new ArrayList<>(variantCount);
        for (int i = 0; i < variantCount; i++) {
            ItemStack stack = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
            variants.add(new DealtItemsHavocConfig.VariantPrice(stack, buf.readVarLong()));
        }
        return new S2C_SyncModGameRules(armorSpecialsEnabled, rebuyEnabled, havocValueVisible, havocPrices, variants);
    }

    public static void handle(S2C_SyncModGameRules msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ModGameRules.acceptClientSync(msg.armorSpecialsEnabled, msg.rebuyEnabled, msg.havocValueVisible);
            DealtItemsHavocConfig.acceptClientPrices(msg.havocPrices, msg.havocVariantPrices);
        });
    }
}
