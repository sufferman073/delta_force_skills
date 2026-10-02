package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

public class S2C_OpenEternalLoveBlessing implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<S2C_OpenEternalLoveBlessing> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(
                    DealtForceSkillsMod.MODID, "s2c_open_eternal_love_blessing"));

    public static final StreamCodec<FriendlyByteBuf, S2C_OpenEternalLoveBlessing> STREAM_CODEC =
            StreamCodec.of(S2C_OpenEternalLoveBlessing::encode, S2C_OpenEternalLoveBlessing::decode);

    private static final int MAX_ENTRIES = 256;

    private final List<Entry> entries;

    public S2C_OpenEternalLoveBlessing(List<Entry> entries) {
        this.entries = entries == null ? List.of() : List.copyOf(entries);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(FriendlyByteBuf buf, S2C_OpenEternalLoveBlessing msg) {
        buf.writeVarInt(Math.min(MAX_ENTRIES, msg.entries.size()));
        for (int i = 0; i < msg.entries.size() && i < MAX_ENTRIES; i++) {
            Entry entry = msg.entries.get(i);
            buf.writeUtf(entry.effectId(), 512);
            buf.writeUtf(entry.name(), 256);
            buf.writeVarInt(entry.level());
        }
    }

    private static S2C_OpenEternalLoveBlessing decode(FriendlyByteBuf buf) {
        int count = Math.max(0, Math.min(MAX_ENTRIES, buf.readVarInt()));
        List<Entry> entries = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            entries.add(new Entry(buf.readUtf(512), buf.readUtf(256), buf.readVarInt()));
        }
        return new S2C_OpenEternalLoveBlessing(entries);
    }

    public static void handle(S2C_OpenEternalLoveBlessing msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> EternalLoveBlessingScreen.open(msg.entries));
    }

    public record Entry(String effectId, String name, int level) {
    }
}
