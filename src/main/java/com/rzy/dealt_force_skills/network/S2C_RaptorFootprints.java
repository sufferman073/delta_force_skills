package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.character.raptor.RaptorFootprintMarker;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class S2C_RaptorFootprints implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<S2C_RaptorFootprints> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "s2c_raptor_footprints"));

    public static final StreamCodec<FriendlyByteBuf, S2C_RaptorFootprints> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    S2C_RaptorFootprints::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final List<RaptorFootprintMarker> markers;

    public S2C_RaptorFootprints(List<RaptorFootprintMarker> markers) {
        this.markers = List.copyOf(markers);
    }

    public static void encode(S2C_RaptorFootprints msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.markers.size());
        for (RaptorFootprintMarker marker : msg.markers) {
            buf.writeVarInt(marker.id());
            buf.writeDouble(marker.position().x);
            buf.writeDouble(marker.position().y);
            buf.writeDouble(marker.position().z);
            buf.writeVarInt(marker.ageTicks());
            buf.writeUtf(marker.ownerName(), 64);
            buf.writeUtf(marker.equipmentSummary(), 160);
            buf.writeBoolean(marker.scanned());
        }
    }

    public static S2C_RaptorFootprints decode(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        List<RaptorFootprintMarker> markers = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            markers.add(new RaptorFootprintMarker(
                    buf.readVarInt(),
                    new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()),
                    buf.readVarInt(),
                    buf.readUtf(64),
                    buf.readUtf(160),
                    buf.readBoolean()
            ));
        }
        return new S2C_RaptorFootprints(markers);
    }

    public static void handle(S2C_RaptorFootprints msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientRaptorHudState.syncFootprints(msg.markers));
    }
}
