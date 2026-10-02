package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.character.vlinder.VlinderMarkerType;
import com.rzy.dealt_force_skills.character.vlinder.VlinderWorldMarker;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class S2C_SyncVlinderState implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<S2C_SyncVlinderState> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "s2c_sync_vlinder_state"));

    public static final StreamCodec<FriendlyByteBuf, S2C_SyncVlinderState> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    S2C_SyncVlinderState::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final int medicalCharges;
    private final int medicalMaxCharges;
    private final int medicalRechargeTicks;
    private final int smokeCharges;
    private final int smokeMaxCharges;
    private final int smokeRechargeTicks;
    private final int coreCooldownTicks;
    private final int equippedToolOrdinal;
    private final int droneModeOrdinal;
    private final int lockedTargetId;
    private final int rescueTicks;
    private final int rescueRequiredTicks;
    private final int selfDownedTicks;
    private final int rescueProtectionTicks;
    private final int activeDefenseTicks;
    private final int injectionTicks;
    private final int injectionRequiredTicks;
    private final List<VlinderWorldMarker> markers;

    public S2C_SyncVlinderState(
            int medicalCharges,
            int medicalMaxCharges,
            int medicalRechargeTicks,
            int smokeCharges,
            int smokeMaxCharges,
            int smokeRechargeTicks,
            int coreCooldownTicks,
            int equippedToolOrdinal,
            int droneModeOrdinal,
            int lockedTargetId,
            int rescueTicks,
            int rescueRequiredTicks,
            int selfDownedTicks,
            int rescueProtectionTicks,
            int activeDefenseTicks,
            int injectionTicks,
            int injectionRequiredTicks,
            List<VlinderWorldMarker> markers
    ) {
        this.medicalCharges = medicalCharges;
        this.medicalMaxCharges = medicalMaxCharges;
        this.medicalRechargeTicks = medicalRechargeTicks;
        this.smokeCharges = smokeCharges;
        this.smokeMaxCharges = smokeMaxCharges;
        this.smokeRechargeTicks = smokeRechargeTicks;
        this.coreCooldownTicks = coreCooldownTicks;
        this.equippedToolOrdinal = equippedToolOrdinal;
        this.droneModeOrdinal = droneModeOrdinal;
        this.lockedTargetId = lockedTargetId;
        this.rescueTicks = rescueTicks;
        this.rescueRequiredTicks = rescueRequiredTicks;
        this.selfDownedTicks = selfDownedTicks;
        this.rescueProtectionTicks = rescueProtectionTicks;
        this.activeDefenseTicks = activeDefenseTicks;
        this.injectionTicks = injectionTicks;
        this.injectionRequiredTicks = injectionRequiredTicks;
        this.markers = List.copyOf(markers);
    }

    public static void encode(S2C_SyncVlinderState msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.medicalCharges);
        buf.writeVarInt(msg.medicalMaxCharges);
        buf.writeVarInt(msg.medicalRechargeTicks);
        buf.writeVarInt(msg.smokeCharges);
        buf.writeVarInt(msg.smokeMaxCharges);
        buf.writeVarInt(msg.smokeRechargeTicks);
        buf.writeVarInt(msg.coreCooldownTicks);
        buf.writeVarInt(msg.equippedToolOrdinal);
        buf.writeVarInt(msg.droneModeOrdinal);
        buf.writeVarInt(msg.lockedTargetId);
        buf.writeVarInt(msg.rescueTicks);
        buf.writeVarInt(msg.rescueRequiredTicks);
        buf.writeVarInt(msg.selfDownedTicks);
        buf.writeVarInt(msg.rescueProtectionTicks);
        buf.writeVarInt(msg.activeDefenseTicks);
        buf.writeVarInt(msg.injectionTicks);
        buf.writeVarInt(msg.injectionRequiredTicks);
        buf.writeVarInt(msg.markers.size());
        for (VlinderWorldMarker marker : msg.markers) {
            buf.writeVarInt(marker.type().ordinal());
            buf.writeVarInt(marker.entityId());
            buf.writeDouble(marker.position().x);
            buf.writeDouble(marker.position().y);
            buf.writeDouble(marker.position().z);
            buf.writeVarInt(marker.remainingTicks());
            buf.writeVarInt(marker.progressTicks());
            buf.writeVarInt(marker.requiredTicks());
        }
    }

    public static S2C_SyncVlinderState decode(FriendlyByteBuf buf) {
        int medicalCharges = buf.readVarInt();
        int medicalMaxCharges = buf.readVarInt();
        int medicalRechargeTicks = buf.readVarInt();
        int smokeCharges = buf.readVarInt();
        int smokeMaxCharges = buf.readVarInt();
        int smokeRechargeTicks = buf.readVarInt();
        int coreCooldownTicks = buf.readVarInt();
        int equippedToolOrdinal = buf.readVarInt();
        int droneModeOrdinal = buf.readVarInt();
        int lockedTargetId = buf.readVarInt();
        int rescueTicks = buf.readVarInt();
        int rescueRequiredTicks = buf.readVarInt();
        int selfDownedTicks = buf.readVarInt();
        int rescueProtectionTicks = buf.readVarInt();
        int activeDefenseTicks = buf.readVarInt();
        int injectionTicks = buf.readVarInt();
        int injectionRequiredTicks = buf.readVarInt();
        int count = buf.readVarInt();
        List<VlinderWorldMarker> markers = new ArrayList<>();
        VlinderMarkerType[] types = VlinderMarkerType.values();
        for (int i = 0; i < count; i++) {
            int typeOrdinal = buf.readVarInt();
            VlinderMarkerType type = typeOrdinal >= 0 && typeOrdinal < types.length ? types[typeOrdinal] : VlinderMarkerType.PLAYER;
            markers.add(new VlinderWorldMarker(
                    type,
                    buf.readVarInt(),
                    new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()),
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readVarInt()
            ));
        }
        return new S2C_SyncVlinderState(medicalCharges, medicalMaxCharges, medicalRechargeTicks,
                smokeCharges, smokeMaxCharges, smokeRechargeTicks, coreCooldownTicks, equippedToolOrdinal,
                droneModeOrdinal, lockedTargetId, rescueTicks, rescueRequiredTicks, selfDownedTicks,
                rescueProtectionTicks, activeDefenseTicks, injectionTicks, injectionRequiredTicks, markers);
    }

    public static void handle(S2C_SyncVlinderState msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientVlinderHudState.sync(
                        msg.medicalCharges,
                        msg.medicalMaxCharges,
                        msg.medicalRechargeTicks,
                        msg.smokeCharges,
                        msg.smokeMaxCharges,
                        msg.smokeRechargeTicks,
                        msg.coreCooldownTicks,
                        msg.equippedToolOrdinal,
                        msg.droneModeOrdinal,
                        msg.lockedTargetId,
                        msg.rescueTicks,
                        msg.rescueRequiredTicks,
                        msg.selfDownedTicks,
                        msg.rescueProtectionTicks,
                        msg.activeDefenseTicks,
                        msg.injectionTicks,
                        msg.injectionRequiredTicks,
                        msg.markers
                ));
    }
}
