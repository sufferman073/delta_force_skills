package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.character.gizmo.GizmoStateManager;
import com.rzy.dealt_force_skills.character.stinger.StingerStateManager;
import com.rzy.dealt_force_skills.registry.ModEffects;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class C2S_GizmoWebEscape implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<C2S_GizmoWebEscape> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "c2s_gizmo_web_escape"));

    public static final StreamCodec<FriendlyByteBuf, C2S_GizmoWebEscape> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    C2S_GizmoWebEscape::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final boolean holdingSpace;

    public C2S_GizmoWebEscape(boolean holdingSpace) {
        this.holdingSpace = holdingSpace;
    }

    public static void encode(C2S_GizmoWebEscape msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.holdingSpace);
    }

    public static C2S_GizmoWebEscape decode(FriendlyByteBuf buf) {
        return new C2S_GizmoWebEscape(buf.readBoolean());
    }

    public static void handle(C2S_GizmoWebEscape msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player != null && player.hasEffect(ModEffects.WEBBED) && !StingerStateManager.isDowned(player)) {
                GizmoStateManager.setWebEscapeHolding(player, msg.holdingSpace);
            }
        });
    }
}
