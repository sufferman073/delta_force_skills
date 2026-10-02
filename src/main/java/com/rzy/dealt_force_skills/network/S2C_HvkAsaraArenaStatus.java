package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

public class S2C_HvkAsaraArenaStatus implements CustomPacketPayload {
    public static final Type<S2C_HvkAsaraArenaStatus> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "s2c_hvk_asara_arena_status"));
    public static final StreamCodec<FriendlyByteBuf, S2C_HvkAsaraArenaStatus> STREAM_CODEC = StreamCodec.of(
            (buf, msg) -> encode(msg, buf), S2C_HvkAsaraArenaStatus::decode);

    private final boolean active;
    private final int remaining;
    private final List<BlockPos> positions;

    public S2C_HvkAsaraArenaStatus(int remaining, List<BlockPos> positions) {
        this(true, remaining, positions);
    }

    private S2C_HvkAsaraArenaStatus(boolean active, int remaining, List<BlockPos> positions) {
        this.active = active;
        this.remaining = Math.max(0, remaining);
        this.positions = positions.stream().limit(7).toList();
    }

    public static S2C_HvkAsaraArenaStatus inactive() {
        return new S2C_HvkAsaraArenaStatus(false, 0, List.of());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(S2C_HvkAsaraArenaStatus msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.active);
        buf.writeVarInt(msg.remaining);
        buf.writeVarInt(msg.positions.size());
        msg.positions.forEach(buf::writeBlockPos);
    }

    public static S2C_HvkAsaraArenaStatus decode(FriendlyByteBuf buf) {
        boolean active = buf.readBoolean();
        int remaining = buf.readVarInt();
        int size = Math.min(7, buf.readVarInt());
        List<BlockPos> positions = new ArrayList<>(size);
        for (int index = 0; index < size; index++) positions.add(buf.readBlockPos());
        return new S2C_HvkAsaraArenaStatus(active, remaining, positions);
    }

    public static void handle(S2C_HvkAsaraArenaStatus msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientHvkAsaraArenaState.sync(msg.active, msg.remaining, msg.positions));
    }
}
