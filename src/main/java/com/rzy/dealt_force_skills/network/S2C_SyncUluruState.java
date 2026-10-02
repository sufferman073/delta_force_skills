package com.rzy.dealt_force_skills.network;

import net.minecraft.network.FriendlyByteBuf;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class S2C_SyncUluruState implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<S2C_SyncUluruState> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "s2c_sync_uluru_state"));

    public static final StreamCodec<FriendlyByteBuf, S2C_SyncUluruState> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    S2C_SyncUluruState::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final int incendiaryCharges;
    private final int incendiaryMaxCharges;
    private final int incendiaryRechargeTicks;
    private final int coverCharges;
    private final int coverMaxCharges;
    private final int coverRechargeTicks;
    private final int missileCooldownTicks;
    private final int equippedToolOrdinal;
    private final boolean coverPerpendicular;

    public S2C_SyncUluruState(
            int incendiaryCharges,
            int incendiaryMaxCharges,
            int incendiaryRechargeTicks,
            int coverCharges,
            int coverMaxCharges,
            int coverRechargeTicks,
            int missileCooldownTicks,
            int equippedToolOrdinal,
            boolean coverPerpendicular
    ) {
        this.incendiaryCharges = incendiaryCharges;
        this.incendiaryMaxCharges = incendiaryMaxCharges;
        this.incendiaryRechargeTicks = incendiaryRechargeTicks;
        this.coverCharges = coverCharges;
        this.coverMaxCharges = coverMaxCharges;
        this.coverRechargeTicks = coverRechargeTicks;
        this.missileCooldownTicks = missileCooldownTicks;
        this.equippedToolOrdinal = equippedToolOrdinal;
        this.coverPerpendicular = coverPerpendicular;
    }

    public static void encode(S2C_SyncUluruState msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.incendiaryCharges);
        buf.writeVarInt(msg.incendiaryMaxCharges);
        buf.writeVarInt(msg.incendiaryRechargeTicks);
        buf.writeVarInt(msg.coverCharges);
        buf.writeVarInt(msg.coverMaxCharges);
        buf.writeVarInt(msg.coverRechargeTicks);
        buf.writeVarInt(msg.missileCooldownTicks);
        buf.writeVarInt(msg.equippedToolOrdinal);
        buf.writeBoolean(msg.coverPerpendicular);
    }

    public static S2C_SyncUluruState decode(FriendlyByteBuf buf) {
        return new S2C_SyncUluruState(
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readBoolean()
        );
    }

    public static void handle(S2C_SyncUluruState msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientUluruHudState.sync(
                        msg.incendiaryCharges,
                        msg.incendiaryMaxCharges,
                        msg.incendiaryRechargeTicks,
                        msg.coverCharges,
                        msg.coverMaxCharges,
                        msg.coverRechargeTicks,
                        msg.missileCooldownTicks,
                        msg.equippedToolOrdinal,
                        msg.coverPerpendicular
                ));
    }
}
