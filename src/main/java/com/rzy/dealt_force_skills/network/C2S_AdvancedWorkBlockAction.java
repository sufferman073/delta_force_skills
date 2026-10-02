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

public class C2S_AdvancedWorkBlockAction implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<C2S_AdvancedWorkBlockAction> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(
                    DealtForceSkillsMod.MODID, "c2s_advanced_work_block_action"));

    public static final StreamCodec<FriendlyByteBuf, C2S_AdvancedWorkBlockAction> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    C2S_AdvancedWorkBlockAction::decode
            );

    private final BlockPos pos;
    private final int blockType;
    private final int mode;
    private final ResourceLocation id;
    private final int action;
    private final int count;

    public C2S_AdvancedWorkBlockAction(BlockPos pos, int blockType, int mode, ResourceLocation id, int action,
                                       int count) {
        this.pos = pos;
        this.blockType = blockType;
        this.mode = mode;
        this.id = id;
        this.action = action;
        this.count = Math.max(1, count);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(C2S_AdvancedWorkBlockAction msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeVarInt(msg.blockType);
        buf.writeVarInt(msg.mode);
        buf.writeUtf(msg.id == null ? "minecraft:air" : msg.id.toString());
        buf.writeVarInt(msg.action);
        buf.writeVarInt(msg.count);
    }

    public static C2S_AdvancedWorkBlockAction decode(FriendlyByteBuf buf) {
        return new C2S_AdvancedWorkBlockAction(buf.readBlockPos(), buf.readVarInt(), buf.readVarInt(),
                S2C_OpenAdvancedWorkBlock.readResourceLocation(buf), buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(C2S_AdvancedWorkBlockAction msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player == null) {
                return;
            }
            BlockEntity blockEntity = player.level().getBlockEntity(msg.pos);
            if (blockEntity instanceof HvkClonePrototypeBlockEntity clone) {
                clone.performAction(player, msg.mode, msg.id, msg.action, msg.count);
            } else if (blockEntity instanceof HvkAdvancedTreasureCompassBlockEntity compass) {
                compass.performAction(player, msg.mode, msg.id, msg.action, msg.count);
            } else if (blockEntity instanceof InterdimensionalBlockEntity interdimensional) {
                interdimensional.performAction(player, msg.mode, msg.id, msg.action, msg.count);
            }
        });
    }
}
