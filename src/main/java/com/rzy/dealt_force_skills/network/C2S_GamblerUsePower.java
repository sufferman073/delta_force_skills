package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.gambler.GamblerStateManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class C2S_GamblerUsePower implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<C2S_GamblerUsePower> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "c2s_gambler_use_power"));
    public static final StreamCodec<FriendlyByteBuf, C2S_GamblerUsePower> STREAM_CODEC =
            StreamCodec.of((buf, msg) -> encode(msg, buf), C2S_GamblerUsePower::decode);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final int powerOrdinal;

    public C2S_GamblerUsePower(int powerOrdinal) {
        this.powerOrdinal = powerOrdinal;
    }

    public static void encode(C2S_GamblerUsePower msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.powerOrdinal);
    }

    public static C2S_GamblerUsePower decode(FriendlyByteBuf buf) {
        return new C2S_GamblerUsePower(buf.readVarInt());
    }

    public static void handle(C2S_GamblerUsePower msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer player && !player.isSpectator()) {
                GamblerStateManager.useStoredPower(player, msg.powerOrdinal);
            }
        });
    }
}
