package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.shop.SaeedRecruitManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class C2S_OpenSaeedMonitor implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<C2S_OpenSaeedMonitor> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "c2s_open_saeed_monitor"));

    public static final StreamCodec<FriendlyByteBuf, C2S_OpenSaeedMonitor> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    C2S_OpenSaeedMonitor::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(C2S_OpenSaeedMonitor msg, FriendlyByteBuf buf) {
    }

    public static C2S_OpenSaeedMonitor decode(FriendlyByteBuf buf) {
        return new C2S_OpenSaeedMonitor();
    }

    public static void handle(C2S_OpenSaeedMonitor msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player != null) {
                SaeedRecruitManager.openMonitor(player);
            }
        });
    }
}
