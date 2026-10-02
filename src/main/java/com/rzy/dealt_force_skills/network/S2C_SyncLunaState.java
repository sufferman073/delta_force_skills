package com.rzy.dealt_force_skills.network;

import net.minecraft.network.FriendlyByteBuf;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class S2C_SyncLunaState implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<S2C_SyncLunaState> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "s2c_sync_luna_state"));

    public static final StreamCodec<FriendlyByteBuf, S2C_SyncLunaState> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    S2C_SyncLunaState::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final int shockCharges;
    private final int shockMaxCharges;
    private final int shockRechargeTicks;
    private final int grenadeCharges;
    private final int grenadeMaxCharges;
    private final int grenadeRechargeTicks;
    private final int coreCooldownTicks;
    private final int equippedToolOrdinal;
    private final int grenadeCookTicks;
    private final boolean shockBounceEnabled;

    public S2C_SyncLunaState(
            int shockCharges,
            int shockMaxCharges,
            int shockRechargeTicks,
            int grenadeCharges,
            int grenadeMaxCharges,
            int grenadeRechargeTicks,
            int coreCooldownTicks,
            int equippedToolOrdinal,
            int grenadeCookTicks,
            boolean shockBounceEnabled
    ) {
        this.shockCharges = shockCharges;
        this.shockMaxCharges = shockMaxCharges;
        this.shockRechargeTicks = shockRechargeTicks;
        this.grenadeCharges = grenadeCharges;
        this.grenadeMaxCharges = grenadeMaxCharges;
        this.grenadeRechargeTicks = grenadeRechargeTicks;
        this.coreCooldownTicks = coreCooldownTicks;
        this.equippedToolOrdinal = equippedToolOrdinal;
        this.grenadeCookTicks = grenadeCookTicks;
        this.shockBounceEnabled = shockBounceEnabled;
    }

    public static void encode(S2C_SyncLunaState msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.shockCharges);
        buf.writeVarInt(msg.shockMaxCharges);
        buf.writeVarInt(msg.shockRechargeTicks);
        buf.writeVarInt(msg.grenadeCharges);
        buf.writeVarInt(msg.grenadeMaxCharges);
        buf.writeVarInt(msg.grenadeRechargeTicks);
        buf.writeVarInt(msg.coreCooldownTicks);
        buf.writeVarInt(msg.equippedToolOrdinal);
        buf.writeVarInt(msg.grenadeCookTicks);
        buf.writeBoolean(msg.shockBounceEnabled);
    }

    public static S2C_SyncLunaState decode(FriendlyByteBuf buf) {
        return new S2C_SyncLunaState(
                buf.readVarInt(),
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

    public static void handle(S2C_SyncLunaState msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientLunaHudState.sync(
                        msg.shockCharges,
                        msg.shockMaxCharges,
                        msg.shockRechargeTicks,
                        msg.grenadeCharges,
                        msg.grenadeMaxCharges,
                        msg.grenadeRechargeTicks,
                        msg.coreCooldownTicks,
                        msg.equippedToolOrdinal,
                        msg.grenadeCookTicks,
                        msg.shockBounceEnabled
                ));
    }
}
