package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.character.toxik.ToxikStateManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class C2S_ToxikPullout implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<C2S_ToxikPullout> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "c2s_toxik_pullout"));

    public static final StreamCodec<FriendlyByteBuf, C2S_ToxikPullout> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    C2S_ToxikPullout::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final boolean active;

    public C2S_ToxikPullout(boolean active) {
        this.active = active;
    }

    public static void encode(C2S_ToxikPullout msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.active);
    }

    public static C2S_ToxikPullout decode(FriendlyByteBuf buf) {
        return new C2S_ToxikPullout(buf.readBoolean());
    }

    public static void handle(C2S_ToxikPullout msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player != null && !player.isSpectator()) {
                ToxikStateManager.setFireflyPulloutActive(player, msg.active);
            }
        });
    }
}
