package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.character.chamber.ChamberSkills;
import com.rzy.dealt_force_skills.character.chamber.ChamberStateManager;
import com.rzy.dealt_force_skills.character.chamber.ChamberToolAction;
import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.team.RoundStartFreezeManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class C2S_ChamberToolAction implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<C2S_ChamberToolAction> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "c2s_chamber_tool_action"));

    public static final StreamCodec<FriendlyByteBuf, C2S_ChamberToolAction> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    C2S_ChamberToolAction::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final ChamberToolAction action;
    private final int targetEntityId;

    public C2S_ChamberToolAction(ChamberToolAction action) {
        this(action, -1);
    }

    public C2S_ChamberToolAction(ChamberToolAction action, int targetEntityId) {
        this.action = action;
        this.targetEntityId = targetEntityId;
    }

    public static void encode(C2S_ChamberToolAction msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.action.ordinal());
        buf.writeVarInt(msg.targetEntityId);
    }

    public static C2S_ChamberToolAction decode(FriendlyByteBuf buf) {
        int ordinal = buf.readVarInt();
        ChamberToolAction[] values = ChamberToolAction.values();
        ChamberToolAction action = ordinal >= 0 && ordinal < values.length ? values[ordinal] : ChamberToolAction.STOW_TOOL;
        return new C2S_ChamberToolAction(action, buf.readVarInt());
    }

    public static void handle(C2S_ChamberToolAction msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (com.rzy.dealt_force_skills.climb.HvkZiplineManager.blocksToolAction(player, msg.action)) return;
            if (player == null || player.isSpectator()
                    || player.hasEffect(ModEffects.STUN)
                    || player.hasEffect(ModEffects.WEBBED)
                    || player.hasEffect(ModEffects.N_TWO_FROZEN)
                    || RoundStartFreezeManager.isFrozen(player)) {
                return;
            }
            ChamberSkills.handleToolAction(player, msg.action, msg.targetEntityId);
            ChamberStateManager.syncToClient(player);
        });
    }
}
