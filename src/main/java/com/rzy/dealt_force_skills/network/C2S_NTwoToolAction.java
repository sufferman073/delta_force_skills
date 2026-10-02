package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.character.ntwo.NTwoSkills;
import com.rzy.dealt_force_skills.character.ntwo.NTwoStateManager;
import com.rzy.dealt_force_skills.character.ntwo.NTwoToolAction;
import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.team.RoundStartFreezeManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class C2S_NTwoToolAction implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<C2S_NTwoToolAction> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "c2s_n_two_tool_action"));

    public static final StreamCodec<FriendlyByteBuf, C2S_NTwoToolAction> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    C2S_NTwoToolAction::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final NTwoToolAction action;

    public C2S_NTwoToolAction(NTwoToolAction action) {
        this.action = action;
    }

    public static void encode(C2S_NTwoToolAction msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.action.ordinal());
    }

    public static C2S_NTwoToolAction decode(FriendlyByteBuf buf) {
        int ordinal = buf.readVarInt();
        NTwoToolAction[] values = NTwoToolAction.values();
        return new C2S_NTwoToolAction(ordinal >= 0 && ordinal < values.length ? values[ordinal] : NTwoToolAction.STOW_TOOL);
    }

    public static void handle(C2S_NTwoToolAction msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player == null || player.isSpectator()
                    || player.hasEffect(ModEffects.STUN)
                    || player.hasEffect(ModEffects.WEBBED)
                    || player.hasEffect(ModEffects.N_TWO_FROZEN)
                    || RoundStartFreezeManager.isFrozen(player)) {
                return;
            }
            NTwoSkills.handleToolAction(player, msg.action);
            NTwoStateManager.syncToClient(player);
        });
    }
}
