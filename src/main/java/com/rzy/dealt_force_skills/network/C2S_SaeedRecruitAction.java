package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.shop.SaeedRecruitAction;
import com.rzy.dealt_force_skills.shop.SaeedRecruitManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record C2S_SaeedRecruitAction(SaeedRecruitAction action, String guardTypeId) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<C2S_SaeedRecruitAction> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "c2s_saeed_recruit_action"));

    public static final StreamCodec<FriendlyByteBuf, C2S_SaeedRecruitAction> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    C2S_SaeedRecruitAction::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(C2S_SaeedRecruitAction msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.action.ordinal());
        buf.writeUtf(msg.guardTypeId == null ? "" : msg.guardTypeId, 64);
    }

    public static C2S_SaeedRecruitAction decode(FriendlyByteBuf buf) {
        int ordinal = buf.readVarInt();
        SaeedRecruitAction[] values = SaeedRecruitAction.values();
        SaeedRecruitAction action = ordinal >= 0 && ordinal < values.length ? values[ordinal] : SaeedRecruitAction.RECRUIT;
        return new C2S_SaeedRecruitAction(action, buf.readUtf(64));
    }

    public static void handle(C2S_SaeedRecruitAction msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player != null) {
                SaeedRecruitManager.handleAction(player, msg.action, msg.guardTypeId);
            }
        });
    }
}
