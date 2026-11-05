package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.character.morse.MorseMarkerType;
import com.rzy.dealt_force_skills.character.morse.MorseWorldMarker;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class S2C_MorseMarkers implements com.rzy.dealt_force_skills.department.DepartmentRecon.Packet {
    private final List<MorseWorldMarker> markers;

    public S2C_MorseMarkers(List<MorseWorldMarker> markers) {
        this.markers = List.copyOf(markers);
    }

    @Override public void recordDepartmentReveal(net.minecraft.server.level.ServerPlayer viewer) {
        for(var marker:markers) if(marker.type()==MorseMarkerType.SONAR_REVEAL) com.rzy.dealt_force_skills.department.DepartmentRecon.mark(viewer,marker.entityId(),marker.ticks());
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

    public static void handle(S2C_MorseMarkers msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientMorseHudState.addMarkers(msg.markers)
        ));
        ctx.get().setPacketHandled(true);
    }
}
