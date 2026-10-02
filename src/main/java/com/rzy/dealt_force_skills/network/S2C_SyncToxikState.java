package com.rzy.dealt_force_skills.network;

import net.minecraft.network.FriendlyByteBuf;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class S2C_SyncToxikState implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<S2C_SyncToxikState> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "s2c_sync_toxik_state"));

    public static final StreamCodec<FriendlyByteBuf, S2C_SyncToxikState> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    S2C_SyncToxikState::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final int adrenalineCooldownTicks;
    private final int tearGasCharges;
    private final int tearGasMaxCharges;
    private final int tearGasRechargeTicks;
    private final int fireflyCooldownTicks;
    private final int equippedToolOrdinal;
    private final int fireflyModeOrdinal;
    private final int pulloutTicks;
    private final int pulloutRequiredTicks;

    public S2C_SyncToxikState(
            int adrenalineCooldownTicks,
            int tearGasCharges,
            int tearGasMaxCharges,
            int tearGasRechargeTicks,
            int fireflyCooldownTicks,
            int equippedToolOrdinal,
            int fireflyModeOrdinal,
            int pulloutTicks,
            int pulloutRequiredTicks
    ) {
        this.adrenalineCooldownTicks = adrenalineCooldownTicks;
        this.tearGasCharges = tearGasCharges;
        this.tearGasMaxCharges = tearGasMaxCharges;
        this.tearGasRechargeTicks = tearGasRechargeTicks;
        this.fireflyCooldownTicks = fireflyCooldownTicks;
        this.equippedToolOrdinal = equippedToolOrdinal;
        this.fireflyModeOrdinal = fireflyModeOrdinal;
        this.pulloutTicks = pulloutTicks;
        this.pulloutRequiredTicks = pulloutRequiredTicks;
    }

    public static void encode(S2C_SyncToxikState msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.adrenalineCooldownTicks);
        buf.writeVarInt(msg.tearGasCharges);
        buf.writeVarInt(msg.tearGasMaxCharges);
        buf.writeVarInt(msg.tearGasRechargeTicks);
        buf.writeVarInt(msg.fireflyCooldownTicks);
        buf.writeVarInt(msg.equippedToolOrdinal);
        buf.writeVarInt(msg.fireflyModeOrdinal);
        buf.writeVarInt(msg.pulloutTicks);
        buf.writeVarInt(msg.pulloutRequiredTicks);
    }

    public static S2C_SyncToxikState decode(FriendlyByteBuf buf) {
        return new S2C_SyncToxikState(
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt()
        );
    }

    public static void handle(S2C_SyncToxikState msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientToxikHudState.sync(
                        msg.adrenalineCooldownTicks,
                        msg.tearGasCharges,
                        msg.tearGasMaxCharges,
                        msg.tearGasRechargeTicks,
                        msg.fireflyCooldownTicks,
                        msg.equippedToolOrdinal,
                        msg.fireflyModeOrdinal,
                        msg.pulloutTicks,
                        msg.pulloutRequiredTicks
                ));
    }
}
