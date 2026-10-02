package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.config.ConfigEntryData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

public final class S2C_ConfigSnapshot implements CustomPacketPayload {
    public static final Type<S2C_ConfigSnapshot> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "s2c_config_snapshot"));

    public static final StreamCodec<FriendlyByteBuf, S2C_ConfigSnapshot> STREAM_CODEC =
            StreamCodec.of((buf, msg) -> encode(msg, buf), S2C_ConfigSnapshot::decode);

    private final int chunkIndex;
    private final int totalChunks;
    private final boolean canEdit;
    private final boolean lastChunk;
    private final List<ConfigEntryData> entries;

    public S2C_ConfigSnapshot(
            int chunkIndex,
            int totalChunks,
            boolean canEdit,
            boolean lastChunk,
            List<ConfigEntryData> entries) {
        this.chunkIndex = chunkIndex;
        this.totalChunks = totalChunks;
        this.canEdit = canEdit;
        this.lastChunk = lastChunk;
        this.entries = entries == null ? List.of() : List.copyOf(entries);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(S2C_ConfigSnapshot msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.chunkIndex);
        buf.writeVarInt(msg.totalChunks);
        buf.writeBoolean(msg.canEdit);
        buf.writeBoolean(msg.lastChunk);
        buf.writeVarInt(msg.entries.size());
        for (ConfigEntryData entry : msg.entries) {
            entry.encode(buf);
        }
    }

    public static S2C_ConfigSnapshot decode(FriendlyByteBuf buf) {
        int chunkIndex = buf.readVarInt();
        int totalChunks = buf.readVarInt();
        boolean canEdit = buf.readBoolean();
        boolean lastChunk = buf.readBoolean();
        int size = buf.readVarInt();
        List<ConfigEntryData> entries = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            entries.add(ConfigEntryData.decode(buf));
        }
        return new S2C_ConfigSnapshot(chunkIndex, totalChunks, canEdit, lastChunk, entries);
    }

    public static void handle(S2C_ConfigSnapshot msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> DealtForceConfigScreen.acceptServerChunk(
                msg.chunkIndex, msg.totalChunks, msg.canEdit, msg.lastChunk, msg.entries));
    }
}
