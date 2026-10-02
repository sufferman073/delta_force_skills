package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.character.morse.MorseMarkerType;
import com.rzy.dealt_force_skills.character.morse.MorseWorldMarker;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class S2C_MorseMarkers implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<S2C_MorseMarkers> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "s2c_morse_markers"));

    public static final StreamCodec<FriendlyByteBuf, S2C_MorseMarkers> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    S2C_MorseMarkers::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final List<MorseWorldMarker> markers;

    public S2C_MorseMarkers(List<MorseWorldMarker> markers) {
        this.markers = List.copyOf(markers);
    }

    public static void encode(S2C_MorseMarkers msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.markers.size());
        for (MorseWorldMarker marker : msg.markers) {
            buf.writeVarInt(marker.type().ordinal());
            buf.writeVarInt(marker.entityId());
            buf.writeDouble(marker.position().x);
            buf.writeDouble(marker.position().y);
            buf.writeDouble(marker.position().z);
            buf.writeVarInt(marker.ticks());
        }
    }

    public static S2C_MorseMarkers decode(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        MorseMarkerType[] types = MorseMarkerType.values();
        List<MorseWorldMarker> markers = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            int ordinal = buf.readVarInt();
            MorseMarkerType type = ordinal >= 0 && ordinal < types.length ? types[ordinal] : MorseMarkerType.SOUND_SOURCE;
            markers.add(new MorseWorldMarker(
                    type,
                    buf.readVarInt(),
                    new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()),
                    buf.readVarInt()
            ));
        }
        return new S2C_MorseMarkers(markers);
    }

    public static void handle(S2C_MorseMarkers msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientMorseHudState.addMarkers(msg.markers));
    }
}
