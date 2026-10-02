package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.character.ntwo.NTwoColdMarker;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class S2C_SyncNTwoState implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<S2C_SyncNTwoState> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "s2c_sync_n_two_state"));

    public static final StreamCodec<FriendlyByteBuf, S2C_SyncNTwoState> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    S2C_SyncNTwoState::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final int trackingCooldownTicks;
    private final int dewarCharges;
    private final int dewarMaxCharges;
    private final int dewarRechargeTicks;
    private final int coreCooldownTicks;
    private final int coreActiveTicks;
    private final int coreAmmo;
    private final int equippedToolOrdinal;
    private final List<NTwoColdMarker> coldMarkers;

    public S2C_SyncNTwoState(
            int trackingCooldownTicks,
            int dewarCharges,
            int dewarMaxCharges,
            int dewarRechargeTicks,
            int coreCooldownTicks,
            int coreActiveTicks,
            int coreAmmo,
            int equippedToolOrdinal,
            List<NTwoColdMarker> coldMarkers
    ) {
        this.trackingCooldownTicks = trackingCooldownTicks;
        this.dewarCharges = dewarCharges;
        this.dewarMaxCharges = dewarMaxCharges;
        this.dewarRechargeTicks = dewarRechargeTicks;
        this.coreCooldownTicks = coreCooldownTicks;
        this.coreActiveTicks = coreActiveTicks;
        this.coreAmmo = coreAmmo;
        this.equippedToolOrdinal = equippedToolOrdinal;
        this.coldMarkers = List.copyOf(coldMarkers);
    }

    public static void encode(S2C_SyncNTwoState msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.trackingCooldownTicks);
        buf.writeVarInt(msg.dewarCharges);
        buf.writeVarInt(msg.dewarMaxCharges);
        buf.writeVarInt(msg.dewarRechargeTicks);
        buf.writeVarInt(msg.coreCooldownTicks);
        buf.writeVarInt(msg.coreActiveTicks);
        buf.writeVarInt(msg.coreAmmo);
        buf.writeVarInt(msg.equippedToolOrdinal);
        buf.writeVarInt(msg.coldMarkers.size());
        for (NTwoColdMarker marker : msg.coldMarkers) {
            buf.writeVarInt(marker.entityId());
            buf.writeUtf(marker.name(), 64);
            buf.writeVarInt(marker.cold());
            buf.writeVarInt(marker.frozenTicks());
        }
    }

    public static S2C_SyncNTwoState decode(FriendlyByteBuf buf) {
        int trackingCooldownTicks = buf.readVarInt();
        int dewarCharges = buf.readVarInt();
        int dewarMaxCharges = buf.readVarInt();
        int dewarRechargeTicks = buf.readVarInt();
        int coreCooldownTicks = buf.readVarInt();
        int coreActiveTicks = buf.readVarInt();
        int coreAmmo = buf.readVarInt();
        int equippedToolOrdinal = buf.readVarInt();
        int count = buf.readVarInt();
        List<NTwoColdMarker> markers = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            markers.add(new NTwoColdMarker(buf.readVarInt(), buf.readUtf(64), buf.readVarInt(), buf.readVarInt()));
        }
        return new S2C_SyncNTwoState(trackingCooldownTicks, dewarCharges, dewarMaxCharges,
                dewarRechargeTicks, coreCooldownTicks, coreActiveTicks, coreAmmo, equippedToolOrdinal, markers);
    }

    public static void handle(S2C_SyncNTwoState msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientNTwoHudState.sync(
                        msg.trackingCooldownTicks,
                        msg.dewarCharges,
                        msg.dewarMaxCharges,
                        msg.dewarRechargeTicks,
                        msg.coreCooldownTicks,
                        msg.coreActiveTicks,
                        msg.coreAmmo,
                        msg.equippedToolOrdinal,
                        msg.coldMarkers
                ));
    }
}
