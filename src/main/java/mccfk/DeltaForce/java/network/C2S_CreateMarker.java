package mccfk.DeltaForce.java.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import io.netty.handler.codec.DecoderException;

import java.util.Optional;
import java.util.UUID;

/**
 * C2S: 客户端→服务端 - 创建标点请求包
 */
public record C2S_CreateMarker(Marker marker) implements CustomPacketPayload {
    public static final Type<C2S_CreateMarker> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath("deltaforce", "create_marker")
    );

    private static final StreamCodec<RegistryFriendlyByteBuf, MarkerType> MARKER_TYPE_CODEC = new StreamCodec<>() {
        @Override
        public MarkerType decode(RegistryFriendlyByteBuf buf) {
            int ordinal = buf.readUnsignedByte();
            MarkerType[] values = MarkerType.values();
            if (ordinal >= values.length) {
                throw new DecoderException("Unknown marker type: " + ordinal);
            }
            return values[ordinal];
        }
        @Override
        public void encode(RegistryFriendlyByteBuf buf, MarkerType val) {
            buf.writeByte(val.ordinal());
        }
    };

    private static final StreamCodec<RegistryFriendlyByteBuf, Optional<EntityType<?>>> ENTITY_TYPE_OPTIONAL =
        ByteBufCodecs.optional(ByteBufCodecs.registry(net.minecraft.core.registries.Registries.ENTITY_TYPE));

    public static final StreamCodec<RegistryFriendlyByteBuf, C2S_CreateMarker> CODEC = new StreamCodec<>() {
        @Override
        public C2S_CreateMarker decode(RegistryFriendlyByteBuf buf) {
            UUID owner = buf.readUUID();
            BlockPos pos = BlockPos.STREAM_CODEC.decode(buf);
            MarkerType type = MARKER_TYPE_CODEC.decode(buf);
            long createTime = buf.readLong();
            Optional<EntityType<?>> entityType = ENTITY_TYPE_OPTIONAL.decode(buf);
            Optional<BlockPos> containerPos = buf.readBoolean() ? Optional.of(BlockPos.STREAM_CODEC.decode(buf)) : Optional.empty();
            Optional<Vec3> precisePos = buf.readBoolean()
                ? Optional.of(new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()))
                : Optional.empty();
            return new C2S_CreateMarker(new Marker(owner, pos, type, createTime, entityType, containerPos, precisePos));
        }
        @Override
        public void encode(RegistryFriendlyByteBuf buf, C2S_CreateMarker packet) {
            Marker m = packet.marker();
            buf.writeUUID(m.owner());
            BlockPos.STREAM_CODEC.encode(buf, m.position());
            MARKER_TYPE_CODEC.encode(buf, m.type());
            buf.writeLong(m.createTime());
            ENTITY_TYPE_OPTIONAL.encode(buf, m.entityType());
            if (m.containerPos().isPresent()) {
                buf.writeBoolean(true);
                BlockPos.STREAM_CODEC.encode(buf, m.containerPos().get());
            } else {
                buf.writeBoolean(false);
            }
            if (m.precisePosition().isPresent()) {
                buf.writeBoolean(true);
                Vec3 v = m.precisePosition().get();
                buf.writeDouble(v.x);
                buf.writeDouble(v.y);
                buf.writeDouble(v.z);
            } else {
                buf.writeBoolean(false);
            }
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
