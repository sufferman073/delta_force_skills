package com.rzy.dealt_force_skills.network;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.List;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class S2C_UluruGhostEntities implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<S2C_UluruGhostEntities> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "s2c_uluru_ghost_entities"));

    public static final StreamCodec<FriendlyByteBuf, S2C_UluruGhostEntities> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    S2C_UluruGhostEntities::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final List<Entry> entries;

    public S2C_UluruGhostEntities(List<Entry> entries) {
        this.entries = entries;
    }

    public static S2C_UluruGhostEntities fromEntities(List<Entity> entities) {
        List<Entry> entries = new ArrayList<>(entities.size());
        for (Entity entity : entities) {
            ResourceLocation typeId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
            if (typeId == null) {
                continue;
            }
            entries.add(new Entry(
                    entity.getId(),
                    typeId,
                    entity.getX(),
                    entity.getY(),
                    entity.getZ(),
                    entity.getYRot(),
                    entity.getXRot(),
                    entity.getYHeadRot()
            ));
        }
        return new S2C_UluruGhostEntities(entries);
    }

    public static void encode(S2C_UluruGhostEntities msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.entries.size());
        for (Entry entry : msg.entries) {
            buf.writeVarInt(entry.sourceId());
            buf.writeResourceLocation(entry.typeId());
            buf.writeDouble(entry.x());
            buf.writeDouble(entry.y());
            buf.writeDouble(entry.z());
            buf.writeFloat(entry.yRot());
            buf.writeFloat(entry.xRot());
            buf.writeFloat(entry.headYaw());
        }
    }

    public static S2C_UluruGhostEntities decode(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<Entry> entries = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            entries.add(new Entry(
                    buf.readVarInt(),
                    buf.readResourceLocation(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readFloat()
            ));
        }
        return new S2C_UluruGhostEntities(entries);
    }

    public static void handle(S2C_UluruGhostEntities msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> UluruGhostEntityManager.update(msg.entries));
    }

    public record Entry(int sourceId, ResourceLocation typeId, double x, double y, double z,
                        float yRot, float xRot, float headYaw) {
    }
}
