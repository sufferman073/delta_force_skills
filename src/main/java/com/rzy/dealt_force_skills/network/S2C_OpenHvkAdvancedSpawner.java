package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.block.HvkAdvancedSpawnerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record S2C_OpenHvkAdvancedSpawner(
        BlockPos pos, String entityId, int spawnInterval, int spawnCount,
        int livingLimit, int totalLimit, int randomRange, int activityRange
) implements CustomPacketPayload {
    public static final Type<S2C_OpenHvkAdvancedSpawner> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "s2c_open_hvk_advanced_spawner"));
    public static final StreamCodec<FriendlyByteBuf, S2C_OpenHvkAdvancedSpawner> STREAM_CODEC =
            StreamCodec.of((buf, msg) -> encode(msg, buf), S2C_OpenHvkAdvancedSpawner::decode);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void encode(S2C_OpenHvkAdvancedSpawner msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos); buf.writeUtf(msg.entityId, HvkAdvancedSpawnerBlockEntity.MAX_ENTITY_ID_LENGTH);
        buf.writeInt(msg.spawnInterval); buf.writeInt(msg.spawnCount);
        buf.writeInt(msg.livingLimit); buf.writeInt(msg.totalLimit); buf.writeInt(msg.randomRange); buf.writeInt(msg.activityRange);
    }

    public static S2C_OpenHvkAdvancedSpawner decode(FriendlyByteBuf buf) {
        return new S2C_OpenHvkAdvancedSpawner(buf.readBlockPos(),
                buf.readUtf(HvkAdvancedSpawnerBlockEntity.MAX_ENTITY_ID_LENGTH), buf.readInt(),
                buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt());
    }

    public static void handle(S2C_OpenHvkAdvancedSpawner msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> HvkAdvancedSpawnerScreen.open(msg));
    }
}
