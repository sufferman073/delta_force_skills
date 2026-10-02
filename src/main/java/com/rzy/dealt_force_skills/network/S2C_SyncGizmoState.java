package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.character.gizmo.GizmoTrapMarker;
import com.rzy.dealt_force_skills.character.gizmo.GizmoTrapType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class S2C_SyncGizmoState implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<S2C_SyncGizmoState> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "s2c_sync_gizmo_state"));

    public static final StreamCodec<FriendlyByteBuf, S2C_SyncGizmoState> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    S2C_SyncGizmoState::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final int smokeCharges;
    private final int smokeMaxCharges;
    private final int smokeRechargeTicks;
    private final int spiderCharges;
    private final int spiderMaxCharges;
    private final int spiderRechargeTicks;
    private final int coreCooldownTicks;
    private final int equippedToolOrdinal;
    private final boolean passiveBoost;
    private final int webEscapeTicks;
    private final List<GizmoTrapMarker> trapMarkers;

    public S2C_SyncGizmoState(
            int smokeCharges,
            int smokeMaxCharges,
            int smokeRechargeTicks,
            int spiderCharges,
            int spiderMaxCharges,
            int spiderRechargeTicks,
            int coreCooldownTicks,
            int equippedToolOrdinal,
            boolean passiveBoost,
            int webEscapeTicks,
            List<GizmoTrapMarker> trapMarkers
    ) {
        this.smokeCharges = smokeCharges;
        this.smokeMaxCharges = smokeMaxCharges;
        this.smokeRechargeTicks = smokeRechargeTicks;
        this.spiderCharges = spiderCharges;
        this.spiderMaxCharges = spiderMaxCharges;
        this.spiderRechargeTicks = spiderRechargeTicks;
        this.coreCooldownTicks = coreCooldownTicks;
        this.equippedToolOrdinal = equippedToolOrdinal;
        this.passiveBoost = passiveBoost;
        this.webEscapeTicks = webEscapeTicks;
        this.trapMarkers = List.copyOf(trapMarkers);
    }

    public static void encode(S2C_SyncGizmoState msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.smokeCharges);
        buf.writeVarInt(msg.smokeMaxCharges);
        buf.writeVarInt(msg.smokeRechargeTicks);
        buf.writeVarInt(msg.spiderCharges);
        buf.writeVarInt(msg.spiderMaxCharges);
        buf.writeVarInt(msg.spiderRechargeTicks);
        buf.writeVarInt(msg.coreCooldownTicks);
        buf.writeVarInt(msg.equippedToolOrdinal);
        buf.writeBoolean(msg.passiveBoost);
        buf.writeVarInt(msg.webEscapeTicks);
        buf.writeVarInt(msg.trapMarkers.size());
        for (GizmoTrapMarker marker : msg.trapMarkers) {
            buf.writeVarInt(marker.entityId());
            buf.writeVarInt(marker.type().ordinal());
            buf.writeDouble(marker.position().x);
            buf.writeDouble(marker.position().y);
            buf.writeDouble(marker.position().z);
        }
    }

    public static S2C_SyncGizmoState decode(FriendlyByteBuf buf) {
        int smokeCharges = buf.readVarInt();
        int smokeMaxCharges = buf.readVarInt();
        int smokeRechargeTicks = buf.readVarInt();
        int spiderCharges = buf.readVarInt();
        int spiderMaxCharges = buf.readVarInt();
        int spiderRechargeTicks = buf.readVarInt();
        int coreCooldownTicks = buf.readVarInt();
        int equippedToolOrdinal = buf.readVarInt();
        boolean passiveBoost = buf.readBoolean();
        int webEscapeTicks = buf.readVarInt();
        int count = buf.readVarInt();
        List<GizmoTrapMarker> markers = new ArrayList<>();
        GizmoTrapType[] types = GizmoTrapType.values();
        for (int i = 0; i < count; i++) {
            int entityId = buf.readVarInt();
            int typeOrdinal = buf.readVarInt();
            GizmoTrapType type = typeOrdinal >= 0 && typeOrdinal < types.length ? types[typeOrdinal] : GizmoTrapType.SMOKE;
            markers.add(new GizmoTrapMarker(entityId, type, new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble())));
        }
        return new S2C_SyncGizmoState(smokeCharges, smokeMaxCharges, smokeRechargeTicks,
                spiderCharges, spiderMaxCharges, spiderRechargeTicks, coreCooldownTicks,
                equippedToolOrdinal, passiveBoost, webEscapeTicks, markers);
    }

    public static void handle(S2C_SyncGizmoState msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientGizmoHudState.sync(
                        msg.smokeCharges,
                        msg.smokeMaxCharges,
                        msg.smokeRechargeTicks,
                        msg.spiderCharges,
                        msg.spiderMaxCharges,
                        msg.spiderRechargeTicks,
                        msg.coreCooldownTicks,
                        msg.equippedToolOrdinal,
                        msg.passiveBoost,
                        msg.webEscapeTicks,
                        msg.trapMarkers
                ));
    }
}
