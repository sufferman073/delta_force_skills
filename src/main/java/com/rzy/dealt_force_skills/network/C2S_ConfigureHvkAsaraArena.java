package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.block.HvkAsaraArenaBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record C2S_ConfigureHvkAsaraArena(BlockPos pos, String structureId, long modifierMask, boolean start)
        implements CustomPacketPayload {
    public static final Type<C2S_ConfigureHvkAsaraArena> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(
            DealtForceSkillsMod.MODID, "c2s_configure_hvk_asara_arena"));
    public static final StreamCodec<FriendlyByteBuf, C2S_ConfigureHvkAsaraArena> STREAM_CODEC =
            StreamCodec.of((buf, msg) -> encode(msg, buf), C2S_ConfigureHvkAsaraArena::decode);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void encode(C2S_ConfigureHvkAsaraArena msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeUtf(msg.structureId, HvkAsaraArenaBlockEntity.MAX_STRUCTURE_ID_LENGTH);
        buf.writeLong(msg.modifierMask);
        buf.writeBoolean(msg.start);
    }

    public static C2S_ConfigureHvkAsaraArena decode(FriendlyByteBuf buf) {
        return new C2S_ConfigureHvkAsaraArena(buf.readBlockPos(),
                buf.readUtf(HvkAsaraArenaBlockEntity.MAX_STRUCTURE_ID_LENGTH), buf.readLong(), buf.readBoolean());
    }

    public static void handle(C2S_ConfigureHvkAsaraArena msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer player
                    && player.level().getBlockEntity(msg.pos) instanceof HvkAsaraArenaBlockEntity arena) {
                arena.configure(player, msg.structureId, msg.modifierMask, msg.start);
            }
        });
    }
}
