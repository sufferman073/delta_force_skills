package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

public record S2C_OpenHvkEnchantInfusion(BlockPos pos, ItemStack stack, List<Entry> entries,
                                          boolean apotheosisAvailable, List<AffixEntry> affixes)
        implements CustomPacketPayload {
    public static final Type<S2C_OpenHvkEnchantInfusion> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(
            DealtForceSkillsMod.MODID, "s2c_open_hvk_enchant_infusion"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2C_OpenHvkEnchantInfusion> STREAM_CODEC =
            StreamCodec.of((buf, msg) -> encode(msg, buf), S2C_OpenHvkEnchantInfusion::decode);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static final int MAX_ENTRIES = 8192;
    public record Entry(String id, int currentLevel, int maxLevel) {}
    public record AffixEntry(String id, String name, int currentValue, int maxValue) {}

    public static void encode(S2C_OpenHvkEnchantInfusion msg, RegistryFriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, msg.stack);
        buf.writeVarInt(Math.min(MAX_ENTRIES, msg.entries.size()));
        for (int i = 0; i < Math.min(MAX_ENTRIES, msg.entries.size()); i++) {
            Entry entry = msg.entries.get(i);
            buf.writeUtf(entry.id, 256);
            buf.writeVarInt(entry.currentLevel);
            buf.writeVarInt(entry.maxLevel);
        }
        buf.writeBoolean(msg.apotheosisAvailable);
        buf.writeVarInt(Math.min(MAX_ENTRIES, msg.affixes.size()));
        for (int i = 0; i < Math.min(MAX_ENTRIES, msg.affixes.size()); i++) {
            AffixEntry entry = msg.affixes.get(i);
            buf.writeUtf(entry.id, 256);
            buf.writeUtf(entry.name, 512);
            buf.writeVarInt(entry.currentValue);
            buf.writeVarInt(entry.maxValue);
        }
    }

    public static S2C_OpenHvkEnchantInfusion decode(RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        ItemStack stack = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
        int count = Math.min(MAX_ENTRIES, Math.max(0, buf.readVarInt()));
        List<Entry> entries = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            entries.add(new Entry(buf.readUtf(256), buf.readVarInt(), buf.readVarInt()));
        }
        boolean apotheosisAvailable = buf.readBoolean();
        int affixCount = Math.min(MAX_ENTRIES, Math.max(0, buf.readVarInt()));
        List<AffixEntry> affixes = new ArrayList<>(affixCount);
        for (int i = 0; i < affixCount; i++) {
            affixes.add(new AffixEntry(buf.readUtf(256), buf.readUtf(512), buf.readVarInt(), buf.readVarInt()));
        }
        return new S2C_OpenHvkEnchantInfusion(pos, stack, List.copyOf(entries),
                apotheosisAvailable, List.copyOf(affixes));
    }

    public static void handle(S2C_OpenHvkEnchantInfusion msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> HvkEnchantInfusionScreen.open(msg));
    }
}
