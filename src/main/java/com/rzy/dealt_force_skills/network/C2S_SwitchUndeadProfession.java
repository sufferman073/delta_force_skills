package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.character.undead.UndeadProfession;
import com.rzy.dealt_force_skills.character.undead.UndeadStateManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record C2S_SwitchUndeadProfession(int professionOrdinal) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<C2S_SwitchUndeadProfession> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "c2s_switch_undead_profession"));

    public static final StreamCodec<FriendlyByteBuf, C2S_SwitchUndeadProfession> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    C2S_SwitchUndeadProfession::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(C2S_SwitchUndeadProfession msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.professionOrdinal);
    }

    public static C2S_SwitchUndeadProfession decode(FriendlyByteBuf buf) {
        return new C2S_SwitchUndeadProfession(buf.readVarInt());
    }

    public static void handle(C2S_SwitchUndeadProfession msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player != null && !player.isSpectator() && UndeadStateManager.isUndead(player)) {
                UndeadStateManager.switchProfession(player,
                        UndeadProfession.byOrdinal(msg.professionOrdinal));
            }
        });
    }
}
