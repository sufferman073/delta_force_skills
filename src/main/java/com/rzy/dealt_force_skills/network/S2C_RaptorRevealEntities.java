package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.character.raptor.RaptorRevealMarker;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class S2C_RaptorRevealEntities implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<S2C_RaptorRevealEntities> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "s2c_raptor_reveal_entities"));

    public static final StreamCodec<FriendlyByteBuf, S2C_RaptorRevealEntities> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    S2C_RaptorRevealEntities::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final List<RaptorRevealMarker> markers;

    public S2C_RaptorRevealEntities(List<RaptorRevealMarker> markers) {
        this.markers = List.copyOf(markers);
    }

    public static void encode(S2C_RaptorRevealEntities msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.markers.size());
        for (RaptorRevealMarker marker : msg.markers) {
            buf.writeVarInt(marker.entityId());
            buf.writeDouble(marker.position().x);
            buf.writeDouble(marker.position().y);
            buf.writeDouble(marker.position().z);
            buf.writeVarInt(marker.ticks());
        }
    }

    public static S2C_RaptorRevealEntities decode(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        List<RaptorRevealMarker> markers = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            markers.add(new RaptorRevealMarker(
                    buf.readVarInt(),
                    new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()),
                    buf.readVarInt()
            ));
        }
        return new S2C_RaptorRevealEntities(markers);
    }

    public static void handle(S2C_RaptorRevealEntities msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientRaptorHudState.revealEntities(msg.markers));
    }
}
