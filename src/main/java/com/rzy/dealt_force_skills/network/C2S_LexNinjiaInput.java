package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.character.lexninjia.LexNinjiaInputAction;
import com.rzy.dealt_force_skills.character.lexninjia.LexNinjiaStateManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record C2S_LexNinjiaInput(LexNinjiaInputAction action) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<C2S_LexNinjiaInput> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "c2s_lex_ninjia_input"));

    public static final StreamCodec<FriendlyByteBuf, C2S_LexNinjiaInput> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    C2S_LexNinjiaInput::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(C2S_LexNinjiaInput msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.action.ordinal());
    }

    public static C2S_LexNinjiaInput decode(FriendlyByteBuf buf) {
        int ordinal = buf.readVarInt();
        LexNinjiaInputAction[] values = LexNinjiaInputAction.values();
        LexNinjiaInputAction action = ordinal >= 0 && ordinal < values.length
                ? values[ordinal]
                : LexNinjiaInputAction.SNEAK_RELEASE;
        return new C2S_LexNinjiaInput(action);
    }

    public static void handle(C2S_LexNinjiaInput msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player != null && !player.isSpectator()) {
                LexNinjiaStateManager.handleInput(player, msg.action);
            }
        });
    }
}
