package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.block.HvkAdvancedSpawnerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record C2S_ConfigureHvkAdvancedSpawner(
        BlockPos pos, String entityId, int spawnInterval, int spawnCount,
        int livingLimit, int totalLimit, int randomRange, int activityRange
) implements CustomPacketPayload {
    public static final Type<C2S_ConfigureHvkAdvancedSpawner> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "c2s_configure_hvk_advanced_spawner"));
    public static final StreamCodec<FriendlyByteBuf, C2S_ConfigureHvkAdvancedSpawner> STREAM_CODEC =
            StreamCodec.of((buf, msg) -> encode(msg, buf), C2S_ConfigureHvkAdvancedSpawner::decode);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void encode(C2S_ConfigureHvkAdvancedSpawner msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos); buf.writeUtf(msg.entityId, HvkAdvancedSpawnerBlockEntity.MAX_ENTITY_ID_LENGTH);
        buf.writeInt(msg.spawnInterval); buf.writeInt(msg.spawnCount);
        buf.writeInt(msg.livingLimit); buf.writeInt(msg.totalLimit); buf.writeInt(msg.randomRange); buf.writeInt(msg.activityRange);
    }

    public static C2S_ConfigureHvkAdvancedSpawner decode(FriendlyByteBuf buf) {
        return new C2S_ConfigureHvkAdvancedSpawner(buf.readBlockPos(),
                buf.readUtf(HvkAdvancedSpawnerBlockEntity.MAX_ENTITY_ID_LENGTH), buf.readInt(),
                buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt());
    }

    public static void handle(C2S_ConfigureHvkAdvancedSpawner msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer player
                    && player.level().getBlockEntity(msg.pos) instanceof HvkAdvancedSpawnerBlockEntity spawner) {
                spawner.configure(player, msg.entityId, msg.spawnInterval, msg.spawnCount,
                        msg.livingLimit, msg.totalLimit, msg.randomRange, msg.activityRange);
            }
        });
    }
}
