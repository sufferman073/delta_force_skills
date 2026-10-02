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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * S2C: 服务端→客户端 - 同步所有标点包
 */
public record S2C_SyncMarkers(List<Marker> markers) implements CustomPacketPayload {
    public static final int MAX_MARKERS = 1024;
    public static final Type<S2C_SyncMarkers> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath("deltaforce", "sync_markers")
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

    private static final StreamCodec<RegistryFriendlyByteBuf, Marker> MARKER_CODEC = new StreamCodec<>() {
        @Override
        public Marker decode(RegistryFriendlyByteBuf buf) {
            UUID owner = buf.readUUID();
            BlockPos pos = BlockPos.STREAM_CODEC.decode(buf);
            MarkerType type = MARKER_TYPE_CODEC.decode(buf);
            long createTime = buf.readLong();
            Optional<EntityType<?>> entityType = ENTITY_TYPE_OPTIONAL.decode(buf);
            Optional<BlockPos> containerPos = buf.readBoolean() ? Optional.of(BlockPos.STREAM_CODEC.decode(buf)) : Optional.empty();
            Optional<Vec3> precisePos = buf.readBoolean()
                ? Optional.of(new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()))
                : Optional.empty();
            return new Marker(owner, pos, type, createTime, entityType, containerPos, precisePos);
        }
        @Override
        public void encode(RegistryFriendlyByteBuf buf, Marker m) {
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

    public static final StreamCodec<RegistryFriendlyByteBuf, S2C_SyncMarkers> CODEC = new StreamCodec<>() {
        @Override
        public S2C_SyncMarkers decode(RegistryFriendlyByteBuf buf) {
            int count = buf.readVarInt();
            if (count < 0 || count > MAX_MARKERS) {
                throw new DecoderException("Invalid marker count: " + count);
            }
            List<Marker> list = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                list.add(MARKER_CODEC.decode(buf));
            }
            return new S2C_SyncMarkers(list);
        }
        @Override
        public void encode(RegistryFriendlyByteBuf buf, S2C_SyncMarkers packet) {
            if (packet.markers().size() > MAX_MARKERS) {
                throw new IllegalArgumentException("Too many markers: " + packet.markers().size());
            }
            buf.writeVarInt(packet.markers().size());
            for (Marker m : packet.markers()) {
                MARKER_CODEC.encode(buf, m);
            }
        }
    };

    public S2C_SyncMarkers {
        markers = markers == null ? List.of() : List.copyOf(markers);
        if (markers.size() > MAX_MARKERS) {
            throw new IllegalArgumentException("Too many markers: " + markers.size());
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
