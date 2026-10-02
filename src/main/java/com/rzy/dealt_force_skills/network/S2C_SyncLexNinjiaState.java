package com.rzy.dealt_force_skills.network;

import net.minecraft.network.FriendlyByteBuf;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record S2C_SyncLexNinjiaState(
        float leicra,
        float maxLeicra,
        int handStacks,
        int bladeStacks,
        int harmonyStacks,
        String preparedArtId,
        boolean preparedAffordable,
        String comboText,
        int mindUsed,
        int mindCapacity,
        int deathFlameTicks,
        float deathFlameOverflow,
        int hamPowerTicks,
        int hamBerserkTicks,
        int scientificToolLevel
) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<S2C_SyncLexNinjiaState> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "s2c_sync_lex_ninjia_state"));

    public static final StreamCodec<FriendlyByteBuf, S2C_SyncLexNinjiaState> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    S2C_SyncLexNinjiaState::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(S2C_SyncLexNinjiaState msg, FriendlyByteBuf buf) {
        buf.writeFloat(msg.leicra);
        buf.writeFloat(msg.maxLeicra);
        buf.writeVarInt(msg.handStacks);
        buf.writeVarInt(msg.bladeStacks);
        buf.writeVarInt(msg.harmonyStacks);
        buf.writeUtf(msg.preparedArtId, 64);
        buf.writeBoolean(msg.preparedAffordable);
        buf.writeUtf(msg.comboText, 256);
        buf.writeVarInt(msg.mindUsed);
        buf.writeVarInt(msg.mindCapacity);
        buf.writeVarInt(msg.deathFlameTicks);
        buf.writeFloat(msg.deathFlameOverflow);
        buf.writeVarInt(msg.hamPowerTicks);
        buf.writeVarInt(msg.hamBerserkTicks);
        buf.writeVarInt(msg.scientificToolLevel);
    }

    public static S2C_SyncLexNinjiaState decode(FriendlyByteBuf buf) {
        return new S2C_SyncLexNinjiaState(
                buf.readFloat(),
                buf.readFloat(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readUtf(64),
                buf.readBoolean(),
                buf.readUtf(256),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readFloat(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt()
        );
    }

    public static void handle(S2C_SyncLexNinjiaState msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientLexNinjiaHudState.sync(
                        msg.leicra,
                        msg.maxLeicra,
                        msg.handStacks,
                        msg.bladeStacks,
                        msg.harmonyStacks,
                        msg.preparedArtId,
                        msg.preparedAffordable,
                        msg.comboText,
                        msg.mindUsed,
                        msg.mindCapacity,
                        msg.deathFlameTicks,
                        msg.deathFlameOverflow,
                        msg.hamPowerTicks,
                        msg.hamBerserkTicks,
                        msg.scientificToolLevel
                ));
    }
}
