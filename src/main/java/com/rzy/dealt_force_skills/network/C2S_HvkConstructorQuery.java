package com.rzy.dealt_force_skills.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.block.HvkAdvancedDisassemblyBeamEmitterBlockEntity;
import com.rzy.dealt_force_skills.block.HvkAdvancedStandardTemplateConstructorBlockEntity;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class C2S_HvkConstructorQuery implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<C2S_HvkConstructorQuery> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "c2s_hvk_constructor_query"));

    public static final StreamCodec<FriendlyByteBuf, C2S_HvkConstructorQuery> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    C2S_HvkConstructorQuery::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final BlockPos pos;
    private final String query;
    private final int page;

    public C2S_HvkConstructorQuery(BlockPos pos, String query, int page) {
        this.pos = pos;
        this.query = query == null ? "" : query;
        this.page = Math.max(0, page);
    }

    public static void encode(C2S_HvkConstructorQuery msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeUtf(msg.query);
        buf.writeVarInt(msg.page);
    }

    public static C2S_HvkConstructorQuery decode(FriendlyByteBuf buf) {
        return new C2S_HvkConstructorQuery(buf.readBlockPos(), buf.readUtf(128), buf.readVarInt());
    }

    public static void handle(C2S_HvkConstructorQuery msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player == null) {
                return;
            }
            BlockEntity blockEntity = player.level().getBlockEntity(msg.pos);
            if (blockEntity instanceof HvkAdvancedStandardTemplateConstructorBlockEntity constructor) {
                constructor.sendRecipes(player, msg.query, msg.page);
            } else if (blockEntity instanceof HvkAdvancedDisassemblyBeamEmitterBlockEntity emitter) {
                emitter.sendRecipes(player, msg.query, msg.page);
            }
        });
    }
}
