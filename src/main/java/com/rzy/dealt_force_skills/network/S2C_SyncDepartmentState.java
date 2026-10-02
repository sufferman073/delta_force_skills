package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.character.department.DepartmentTrapMarker;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class S2C_SyncDepartmentState implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<S2C_SyncDepartmentState> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "s2c_sync_department_state"));

    public static final StreamCodec<FriendlyByteBuf, S2C_SyncDepartmentState> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    S2C_SyncDepartmentState::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final int laserCooldownTicks;
    private final int trapCooldownTicks;
    private final int coreCooldownTicks;
    private final int coreCountdownTicks;
    private final int coreAscendTicks;
    private final int equippedToolOrdinal;
    private final int reductionSteps;
    private final int vulnerabilityStacks;
    private final boolean concealed;
    private final int calibrationSuccesses;
    private final int calibrationWindow;
    private final int calibrationWindowStart;
    private final int trapCharges;
    private final List<DepartmentTrapMarker> trapMarkers;

    public S2C_SyncDepartmentState(
            int laserCooldownTicks,
            int trapCooldownTicks,
            int coreCooldownTicks,
            int coreCountdownTicks,
            int coreAscendTicks,
            int equippedToolOrdinal,
            int reductionSteps,
            int vulnerabilityStacks,
            boolean concealed,
            int calibrationSuccesses,
            int calibrationWindow,
            int calibrationWindowStart,
            int trapCharges,
            List<DepartmentTrapMarker> trapMarkers
    ) {
        this.laserCooldownTicks = laserCooldownTicks;
        this.trapCooldownTicks = trapCooldownTicks;
        this.coreCooldownTicks = coreCooldownTicks;
        this.coreCountdownTicks = coreCountdownTicks;
        this.coreAscendTicks = coreAscendTicks;
        this.equippedToolOrdinal = equippedToolOrdinal;
        this.reductionSteps = reductionSteps;
        this.vulnerabilityStacks = vulnerabilityStacks;
        this.concealed = concealed;
        this.calibrationSuccesses = calibrationSuccesses;
        this.calibrationWindow = calibrationWindow;
        this.calibrationWindowStart = calibrationWindowStart;
        this.trapCharges = trapCharges;
        this.trapMarkers = List.copyOf(trapMarkers);
    }

    public static void encode(S2C_SyncDepartmentState msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.laserCooldownTicks);
        buf.writeVarInt(msg.trapCooldownTicks);
        buf.writeVarInt(msg.coreCooldownTicks);
        buf.writeVarInt(msg.coreCountdownTicks);
        buf.writeVarInt(msg.coreAscendTicks);
        buf.writeVarInt(msg.equippedToolOrdinal);
        buf.writeVarInt(msg.reductionSteps);
        buf.writeVarInt(msg.vulnerabilityStacks);
        buf.writeBoolean(msg.concealed);
        buf.writeVarInt(msg.calibrationSuccesses);
        buf.writeVarInt(msg.calibrationWindow);
        buf.writeVarInt(msg.calibrationWindowStart);
        buf.writeVarInt(msg.trapCharges);
        buf.writeVarInt(msg.trapMarkers.size());
        for (DepartmentTrapMarker marker : msg.trapMarkers) {
            buf.writeVarInt(marker.entityId());
            buf.writeDouble(marker.position().x);
            buf.writeDouble(marker.position().y);
            buf.writeDouble(marker.position().z);
        }
    }

    public static S2C_SyncDepartmentState decode(FriendlyByteBuf buf) {
        int laserCooldownTicks = buf.readVarInt();
        int trapCooldownTicks = buf.readVarInt();
        int coreCooldownTicks = buf.readVarInt();
        int coreCountdownTicks = buf.readVarInt();
        int coreAscendTicks = buf.readVarInt();
        int equippedToolOrdinal = buf.readVarInt();
        int reductionSteps = buf.readVarInt();
        int vulnerabilityStacks = buf.readVarInt();
        boolean concealed = buf.readBoolean();
        int calibrationSuccesses = buf.readVarInt();
        int calibrationWindow = buf.readVarInt();
        int calibrationWindowStart = buf.readVarInt();
        int trapCharges = buf.readVarInt();
        int count = buf.readVarInt();
        List<DepartmentTrapMarker> markers = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            markers.add(new DepartmentTrapMarker(buf.readVarInt(),
                    new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble())));
        }
        return new S2C_SyncDepartmentState(laserCooldownTicks, trapCooldownTicks, coreCooldownTicks,
                coreCountdownTicks, coreAscendTicks, equippedToolOrdinal, reductionSteps,
                vulnerabilityStacks, concealed, calibrationSuccesses, calibrationWindow, calibrationWindowStart,
                trapCharges, markers);
    }

    public static void handle(S2C_SyncDepartmentState msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientDepartmentHudState.sync(
                        msg.laserCooldownTicks,
                        msg.trapCooldownTicks,
                        msg.coreCooldownTicks,
                        msg.coreCountdownTicks,
                        msg.coreAscendTicks,
                        msg.equippedToolOrdinal,
                        msg.reductionSteps,
                        msg.vulnerabilityStacks,
                        msg.concealed,
                        msg.calibrationSuccesses,
                        msg.calibrationWindow,
                        msg.calibrationWindowStart,
                        msg.trapCharges,
                        msg.trapMarkers
                ));
    }
}
