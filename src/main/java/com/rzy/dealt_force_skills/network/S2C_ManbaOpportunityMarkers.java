package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.character.manba.ManbaOpportunityMarker;
import com.rzy.dealt_force_skills.character.manba.ManbaOpportunityMarkerType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class S2C_ManbaOpportunityMarkers implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<S2C_ManbaOpportunityMarkers> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "s2c_manba_opportunity_markers"));

    public static final StreamCodec<FriendlyByteBuf, S2C_ManbaOpportunityMarkers> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    S2C_ManbaOpportunityMarkers::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final List<ManbaOpportunityMarker> markers;

    public S2C_ManbaOpportunityMarkers(List<ManbaOpportunityMarker> markers) {
        this.markers = markers == null ? List.of() : List.copyOf(markers);
    }

    public static void encode(S2C_ManbaOpportunityMarkers msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.markers.size());
        for (ManbaOpportunityMarker marker : msg.markers) {
            buf.writeVarInt(marker.type().ordinal());
            buf.writeDouble(marker.position().x);
            buf.writeDouble(marker.position().y);
            buf.writeDouble(marker.position().z);
            buf.writeUtf(marker.label(), 64);
        }
    }

    public static S2C_ManbaOpportunityMarkers decode(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        List<ManbaOpportunityMarker> markers = new ArrayList<>();
        ManbaOpportunityMarkerType[] types = ManbaOpportunityMarkerType.values();
        for (int i = 0; i < count; i++) {
            int typeOrdinal = buf.readVarInt();
            ManbaOpportunityMarkerType type = typeOrdinal >= 0 && typeOrdinal < types.length
                    ? types[typeOrdinal]
                    : ManbaOpportunityMarkerType.OPENED_OR_NORMAL;
            Vec3 position = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
            String label = buf.readUtf(64);
            markers.add(new ManbaOpportunityMarker(type, position, label));
        }
        return new S2C_ManbaOpportunityMarkers(markers);
    }

    public static void handle(S2C_ManbaOpportunityMarkers msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientManbaHudState.syncOpportunityMarkers(msg.markers));
    }
}
