package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.item.EternalLoveBlessingItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record C2S_EternalLoveBlessingCapture(boolean offhandSource) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<C2S_EternalLoveBlessingCapture> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(
                    DealtForceSkillsMod.MODID, "c2s_eternal_love_blessing_capture"));

    public static final StreamCodec<FriendlyByteBuf, C2S_EternalLoveBlessingCapture> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> buf.writeBoolean(msg.offhandSource),
                    buf -> new C2S_EternalLoveBlessingCapture(buf.readBoolean())
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(C2S_EternalLoveBlessingCapture msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer player) {
                EternalLoveBlessingItem.handleCapture(player, msg.offhandSource);
            }
        });
    }
}
