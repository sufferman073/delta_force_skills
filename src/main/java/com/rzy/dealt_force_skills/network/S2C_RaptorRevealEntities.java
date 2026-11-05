package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.character.raptor.RaptorRevealMarker;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class S2C_RaptorRevealEntities implements com.rzy.dealt_force_skills.department.DepartmentRecon.Packet {
    private final List<RaptorRevealMarker> markers;

    public S2C_RaptorRevealEntities(List<RaptorRevealMarker> markers) {
        this.markers = List.copyOf(markers);
    }

    @Override public void recordDepartmentReveal(net.minecraft.server.level.ServerPlayer viewer) {
        for(var marker:markers) com.rzy.dealt_force_skills.department.DepartmentRecon.mark(viewer,marker.entityId(),marker.ticks());
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

    public static void handle(S2C_RaptorRevealEntities msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientRaptorHudState.revealEntities(msg.markers)
        ));
        ctx.get().setPacketHandled(true);
    }
}
