package com.rzy.dealt_force_skills.network;

import net.minecraft.network.FriendlyByteBuf;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class S2C_SyncVyronState implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<S2C_SyncVyronState> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "s2c_sync_vyron_state"));

    public static final StreamCodec<FriendlyByteBuf, S2C_SyncVyronState> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    S2C_SyncVyronState::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final int dashCooldownTicks;
    private final int dashTicks;
    private final int poweredTicks;
    private final int bombCharges;
    private final int bombMaxCharges;
    private final int bombRechargeTicks;
    private final int coreCooldownTicks;
    private final int equippedToolOrdinal;

    public S2C_SyncVyronState(
            int dashCooldownTicks,
            int dashTicks,
            int poweredTicks,
            int bombCharges,
            int bombMaxCharges,
            int bombRechargeTicks,
            int coreCooldownTicks,
            int equippedToolOrdinal
    ) {
        this.dashCooldownTicks = dashCooldownTicks;
        this.dashTicks = dashTicks;
        this.poweredTicks = poweredTicks;
        this.bombCharges = bombCharges;
        this.bombMaxCharges = bombMaxCharges;
        this.bombRechargeTicks = bombRechargeTicks;
        this.coreCooldownTicks = coreCooldownTicks;
        this.equippedToolOrdinal = equippedToolOrdinal;
    }

    public static void encode(S2C_SyncVyronState msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.dashCooldownTicks);
        buf.writeVarInt(msg.dashTicks);
        buf.writeVarInt(msg.poweredTicks);
        buf.writeVarInt(msg.bombCharges);
        buf.writeVarInt(msg.bombMaxCharges);
        buf.writeVarInt(msg.bombRechargeTicks);
        buf.writeVarInt(msg.coreCooldownTicks);
        buf.writeVarInt(msg.equippedToolOrdinal);
    }

    public static S2C_SyncVyronState decode(FriendlyByteBuf buf) {
        return new S2C_SyncVyronState(
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

    public static void handle(S2C_SyncVyronState msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientVyronHudState.sync(
                        msg.dashCooldownTicks,
                        msg.dashTicks,
                        msg.poweredTicks,
                        msg.bombCharges,
                        msg.bombMaxCharges,
                        msg.bombRechargeTicks,
                        msg.coreCooldownTicks,
                        msg.equippedToolOrdinal
                ));
    }
}
