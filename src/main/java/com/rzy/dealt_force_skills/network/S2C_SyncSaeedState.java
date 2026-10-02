package com.rzy.dealt_force_skills.network;

import net.minecraft.network.FriendlyByteBuf;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class S2C_SyncSaeedState implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<S2C_SyncSaeedState> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "s2c_sync_saeed_state"));

    public static final StreamCodec<FriendlyByteBuf, S2C_SyncSaeedState> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    S2C_SyncSaeedState::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final int fireAmmo;
    private final int fireMaxAmmo;
    private final int fireRechargeTicks;
    private final boolean crossbowEquipped;
    private final boolean fireBounce;
    private final int rollCooldownTicks;
    private final int coreCooldownTicks;
    private final int coreActiveTicks;
    private final int commandVisualTicks;

    public S2C_SyncSaeedState(
            int fireAmmo,
            int fireMaxAmmo,
            int fireRechargeTicks,
            boolean crossbowEquipped,
            boolean fireBounce,
            int rollCooldownTicks,
            int coreCooldownTicks,
            int coreActiveTicks,
            int commandVisualTicks
    ) {
        this.fireAmmo = fireAmmo;
        this.fireMaxAmmo = fireMaxAmmo;
        this.fireRechargeTicks = fireRechargeTicks;
        this.crossbowEquipped = crossbowEquipped;
        this.fireBounce = fireBounce;
        this.rollCooldownTicks = rollCooldownTicks;
        this.coreCooldownTicks = coreCooldownTicks;
        this.coreActiveTicks = coreActiveTicks;
        this.commandVisualTicks = commandVisualTicks;
    }

    public static void encode(S2C_SyncSaeedState msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.fireAmmo);
        buf.writeVarInt(msg.fireMaxAmmo);
        buf.writeVarInt(msg.fireRechargeTicks);
        buf.writeBoolean(msg.crossbowEquipped);
        buf.writeBoolean(msg.fireBounce);
        buf.writeVarInt(msg.rollCooldownTicks);
        buf.writeVarInt(msg.coreCooldownTicks);
        buf.writeVarInt(msg.coreActiveTicks);
        buf.writeVarInt(msg.commandVisualTicks);
    }

    public static S2C_SyncSaeedState decode(FriendlyByteBuf buf) {
        return new S2C_SyncSaeedState(
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt()
        );
    }

    public static void handle(S2C_SyncSaeedState msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientSaeedHudState.sync(
                        msg.fireAmmo,
                        msg.fireMaxAmmo,
                        msg.fireRechargeTicks,
                        msg.crossbowEquipped,
                        msg.fireBounce,
                        msg.rollCooldownTicks,
                        msg.coreCooldownTicks,
                        msg.coreActiveTicks,
                        msg.commandVisualTicks
                ));
    }
}
