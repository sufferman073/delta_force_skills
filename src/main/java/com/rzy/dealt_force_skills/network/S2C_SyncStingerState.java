package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.character.stinger.StingerDownedMarker;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class S2C_SyncStingerState implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<S2C_SyncStingerState> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "s2c_sync_stinger_state"));

    public static final StreamCodec<FriendlyByteBuf, S2C_SyncStingerState> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    S2C_SyncStingerState::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final int smokeCooldownTicks;
    private final int droneCooldownTicks;
    private final int stimCharges;
    private final int stimMaxCharges;
    private final int stimRechargeTicks;
    private final int equippedToolOrdinal;
    private final int stimModeOrdinal;
    private final int rescueTicks;
    private final int rescueRequiredTicks;
    private final int rescueCooldownTicks;
    private final int selfDownedTicks;
    private final List<StingerDownedMarker> downedMarkers;

    public S2C_SyncStingerState(
            int smokeCooldownTicks,
            int droneCooldownTicks,
            int stimCharges,
            int stimMaxCharges,
            int stimRechargeTicks,
            int equippedToolOrdinal,
            int stimModeOrdinal,
            int rescueTicks,
            int rescueRequiredTicks,
            int rescueCooldownTicks,
            int selfDownedTicks,
            List<StingerDownedMarker> downedMarkers
    ) {
        this.smokeCooldownTicks = smokeCooldownTicks;
        this.droneCooldownTicks = droneCooldownTicks;
        this.stimCharges = stimCharges;
        this.stimMaxCharges = stimMaxCharges;
        this.stimRechargeTicks = stimRechargeTicks;
        this.equippedToolOrdinal = equippedToolOrdinal;
        this.stimModeOrdinal = stimModeOrdinal;
        this.rescueTicks = rescueTicks;
        this.rescueRequiredTicks = rescueRequiredTicks;
        this.rescueCooldownTicks = rescueCooldownTicks;
        this.selfDownedTicks = selfDownedTicks;
        this.downedMarkers = List.copyOf(downedMarkers);
    }

    public static void encode(S2C_SyncStingerState msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.smokeCooldownTicks);
        buf.writeVarInt(msg.droneCooldownTicks);
        buf.writeVarInt(msg.stimCharges);
        buf.writeVarInt(msg.stimMaxCharges);
        buf.writeVarInt(msg.stimRechargeTicks);
        buf.writeVarInt(msg.equippedToolOrdinal);
        buf.writeVarInt(msg.stimModeOrdinal);
        buf.writeVarInt(msg.rescueTicks);
        buf.writeVarInt(msg.rescueRequiredTicks);
        buf.writeVarInt(msg.rescueCooldownTicks);
        buf.writeVarInt(msg.selfDownedTicks);
        buf.writeVarInt(msg.downedMarkers.size());
        for (StingerDownedMarker marker : msg.downedMarkers) {
            buf.writeVarInt(marker.entityId());
            buf.writeDouble(marker.position().x);
            buf.writeDouble(marker.position().y);
            buf.writeDouble(marker.position().z);
            buf.writeVarInt(marker.remainingTicks());
        }
    }

    public static S2C_SyncStingerState decode(FriendlyByteBuf buf) {
        int smokeCooldownTicks = buf.readVarInt();
        int droneCooldownTicks = buf.readVarInt();
        int stimCharges = buf.readVarInt();
        int stimMaxCharges = buf.readVarInt();
        int stimRechargeTicks = buf.readVarInt();
        int equippedToolOrdinal = buf.readVarInt();
        int stimModeOrdinal = buf.readVarInt();
        int rescueTicks = buf.readVarInt();
        int rescueRequiredTicks = buf.readVarInt();
        int rescueCooldownTicks = buf.readVarInt();
        int selfDownedTicks = buf.readVarInt();
        int count = buf.readVarInt();
        List<StingerDownedMarker> markers = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            markers.add(new StingerDownedMarker(
                    buf.readVarInt(),
                    new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()),
                    buf.readVarInt()
            ));
        }
        return new S2C_SyncStingerState(smokeCooldownTicks, droneCooldownTicks, stimCharges, stimMaxCharges,
                stimRechargeTicks, equippedToolOrdinal, stimModeOrdinal, rescueTicks, rescueRequiredTicks,
                rescueCooldownTicks, selfDownedTicks, markers);
    }

    public static void handle(S2C_SyncStingerState msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientStingerHudState.sync(
                        msg.smokeCooldownTicks,
                        msg.droneCooldownTicks,
                        msg.stimCharges,
                        msg.stimMaxCharges,
                        msg.stimRechargeTicks,
                        msg.equippedToolOrdinal,
                        msg.stimModeOrdinal,
                        msg.rescueTicks,
                        msg.rescueRequiredTicks,
                        msg.rescueCooldownTicks,
                        msg.selfDownedTicks,
                        msg.downedMarkers
                ));
    }
}
