package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class S2C_OpenGamblerDuelInvite implements CustomPacketPayload {
    public static final Type<S2C_OpenGamblerDuelInvite> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "s2c_open_gambler_duel_invite"));
    public static final StreamCodec<FriendlyByteBuf, S2C_OpenGamblerDuelInvite> STREAM_CODEC =
            StreamCodec.of((buffer, payload) -> {}, buffer -> new S2C_OpenGamblerDuelInvite());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(S2C_OpenGamblerDuelInvite msg, IPayloadContext ctx) {
        ctx.enqueueWork(GamblerDuelInviteScreen::open);
    }
}
