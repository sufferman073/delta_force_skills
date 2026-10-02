package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.block.HvkClonePrototypeBlockEntity;
import com.rzy.dealt_force_skills.block.HvkAdvancedTreasureCompassBlockEntity;
import com.rzy.dealt_force_skills.block.InterdimensionalBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class C2S_AdvancedWorkBlockQuery implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<C2S_AdvancedWorkBlockQuery> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(
                    DealtForceSkillsMod.MODID, "c2s_advanced_work_block_query"));

    public static final StreamCodec<FriendlyByteBuf, C2S_AdvancedWorkBlockQuery> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    C2S_AdvancedWorkBlockQuery::decode
            );

    private final BlockPos pos;
    private final int blockType;
    private final int mode;
    private final String query;
    private final int page;

    public C2S_AdvancedWorkBlockQuery(BlockPos pos, int blockType, int mode, String query, int page) {
        this.pos = pos;
        this.blockType = blockType;
        this.mode = mode;
        this.query = query == null ? "" : query;
        this.page = Math.max(0, page);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(C2S_AdvancedWorkBlockQuery msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeVarInt(msg.blockType);
        buf.writeVarInt(msg.mode);
        buf.writeUtf(msg.query);
        buf.writeVarInt(msg.page);
    }

    public static C2S_AdvancedWorkBlockQuery decode(FriendlyByteBuf buf) {
        return new C2S_AdvancedWorkBlockQuery(buf.readBlockPos(), buf.readVarInt(), buf.readVarInt(),
                buf.readUtf(128), buf.readVarInt());
    }

    public static void handle(C2S_AdvancedWorkBlockQuery msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player == null) {
                return;
            }
            BlockEntity blockEntity = player.level().getBlockEntity(msg.pos);
            if (blockEntity instanceof HvkClonePrototypeBlockEntity clone) {
                clone.sendData(player, msg.mode, msg.query, msg.page);
            } else if (blockEntity instanceof HvkAdvancedTreasureCompassBlockEntity compass) {
                compass.sendData(player, msg.mode, msg.query, msg.page);
            } else if (blockEntity instanceof InterdimensionalBlockEntity interdimensional) {
                interdimensional.sendData(player, msg.mode, msg.query, msg.page);
            }
        });
    }
}
