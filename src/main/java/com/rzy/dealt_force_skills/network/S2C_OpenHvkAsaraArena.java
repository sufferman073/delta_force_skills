package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.block.HvkAsaraArenaBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

public record S2C_OpenHvkAsaraArena(BlockPos pos, String selectedStructure, long modifierMask,
                                     List<String> structures) implements CustomPacketPayload {
    public static final Type<S2C_OpenHvkAsaraArena> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(
            DealtForceSkillsMod.MODID, "s2c_open_hvk_asara_arena"));
    public static final StreamCodec<FriendlyByteBuf, S2C_OpenHvkAsaraArena> STREAM_CODEC =
            StreamCodec.of((buf, msg) -> encode(msg, buf), S2C_OpenHvkAsaraArena::decode);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void encode(S2C_OpenHvkAsaraArena msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeUtf(msg.selectedStructure, HvkAsaraArenaBlockEntity.MAX_STRUCTURE_ID_LENGTH);
        buf.writeLong(msg.modifierMask);
        int count = Math.min(HvkAsaraArenaBlockEntity.MAX_STRUCTURES, msg.structures.size());
        buf.writeVarInt(count);
        for (int i = 0; i < count; i++) {
            buf.writeUtf(msg.structures.get(i), HvkAsaraArenaBlockEntity.MAX_STRUCTURE_ID_LENGTH);
        }
    }

    public static S2C_OpenHvkAsaraArena decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        String selected = buf.readUtf(HvkAsaraArenaBlockEntity.MAX_STRUCTURE_ID_LENGTH);
        long mask = buf.readLong();
        int count = Math.min(HvkAsaraArenaBlockEntity.MAX_STRUCTURES, Math.max(0, buf.readVarInt()));
        List<String> structures = new ArrayList<>(count);
        for (int i = 0; i < count; i++) structures.add(buf.readUtf(HvkAsaraArenaBlockEntity.MAX_STRUCTURE_ID_LENGTH));
        return new S2C_OpenHvkAsaraArena(pos, selected, mask, List.copyOf(structures));
    }

    public static void handle(S2C_OpenHvkAsaraArena msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> HvkAsaraArenaScreen.open(msg));
    }
}
