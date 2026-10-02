package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Full-replace sync of every position glued (by HVK Universal Glue) in one dimension. Sent on a
 * successful glue (to everyone currently in that dimension) and on player join / dimension change
 * (to bring the client's {@link HvkGlueClientCache} up to date).
 */
public class S2C_SyncGluedPositions implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<S2C_SyncGluedPositions> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "s2c_sync_glued_positions"));

    public static final StreamCodec<FriendlyByteBuf, S2C_SyncGluedPositions> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    S2C_SyncGluedPositions::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final ResourceKey<Level> dimension;
    private final long[] positions;

    public S2C_SyncGluedPositions(ResourceKey<Level> dimension, long[] positions) {
        this.dimension = dimension;
        this.positions = positions;
    }

    public static void encode(S2C_SyncGluedPositions msg, FriendlyByteBuf buf) {
        buf.writeResourceLocation(msg.dimension.location());
        buf.writeVarInt(msg.positions.length);
        for (long pos : msg.positions) {
            buf.writeLong(pos);
        }
    }

    public static S2C_SyncGluedPositions decode(FriendlyByteBuf buf) {
        ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, buf.readResourceLocation());
        int size = buf.readVarInt();
        long[] positions = new long[size];
        for (int i = 0; i < size; i++) {
            positions[i] = buf.readLong();
        }
        return new S2C_SyncGluedPositions(dimension, positions);
    }

    public static void handle(S2C_SyncGluedPositions msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> HvkGlueClientCache.replaceAll(msg.dimension, msg.positions));
    }
}
