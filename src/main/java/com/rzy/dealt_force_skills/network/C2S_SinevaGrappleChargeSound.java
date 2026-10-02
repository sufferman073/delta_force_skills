package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.sineva.SinevaSkills;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class C2S_SinevaGrappleChargeSound implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<C2S_SinevaGrappleChargeSound> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "c2s_sineva_grapple_charge_sound"));

    public static final StreamCodec<FriendlyByteBuf, C2S_SinevaGrappleChargeSound> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    C2S_SinevaGrappleChargeSound::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public enum Action {
        START,
        READY,
        RELEASE,
        CANCEL
    }

    private final Action action;

    public C2S_SinevaGrappleChargeSound(Action action) {
        this.action = action;
    }

    public static void encode(C2S_SinevaGrappleChargeSound message, FriendlyByteBuf buffer) {
        buffer.writeEnum(message.action);
    }

    public static C2S_SinevaGrappleChargeSound decode(FriendlyByteBuf buffer) {
        return new C2S_SinevaGrappleChargeSound(buffer.readEnum(Action.class));
    }

    public static void handle(C2S_SinevaGrappleChargeSound msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player.isSpectator()) {
                return;
            }
            switch (msg.action) {
                case START -> SinevaSkills.beginGrappleChargeSound(player);
                case READY -> SinevaSkills.markGrappleChargeSoundReady(player);
                case RELEASE -> SinevaSkills.finishGrappleChargeSound(player);
                case CANCEL -> SinevaSkills.cancelGrappleChargeSound(player);
            }
        });
    }
}
