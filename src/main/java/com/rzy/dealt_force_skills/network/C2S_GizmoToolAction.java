package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.character.gizmo.GizmoSkills;
import com.rzy.dealt_force_skills.character.gizmo.GizmoStateManager;
import com.rzy.dealt_force_skills.character.gizmo.GizmoToolAction;
import com.rzy.dealt_force_skills.character.stinger.StingerStateManager;
import com.rzy.dealt_force_skills.entity.UluruLoiteringMissileEntity;
import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.team.RoundStartFreezeManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class C2S_GizmoToolAction implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<C2S_GizmoToolAction> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "c2s_gizmo_tool_action"));

    public static final StreamCodec<FriendlyByteBuf, C2S_GizmoToolAction> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    C2S_GizmoToolAction::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final GizmoToolAction action;
    private final int targetEntityId;

    public C2S_GizmoToolAction(GizmoToolAction action) {
        this(action, -1);
    }

    public C2S_GizmoToolAction(GizmoToolAction action, int targetEntityId) {
        this.action = action;
        this.targetEntityId = targetEntityId;
    }

    public static void encode(C2S_GizmoToolAction msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.action.ordinal());
        buf.writeVarInt(msg.targetEntityId);
    }

    public static C2S_GizmoToolAction decode(FriendlyByteBuf buf) {
        int ordinal = buf.readVarInt();
        GizmoToolAction[] values = GizmoToolAction.values();
        GizmoToolAction action = ordinal >= 0 && ordinal < values.length ? values[ordinal] : GizmoToolAction.STOW_TOOL;
        return new C2S_GizmoToolAction(action, buf.readVarInt());
    }

    public static void handle(C2S_GizmoToolAction msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (com.rzy.dealt_force_skills.climb.HvkZiplineManager.blocksToolAction(player, msg.action)) return;
            if (player == null || player.isSpectator() || UluruLoiteringMissileEntity.isPlayerControlling(player)) {
                return;
            }
            if (player.hasEffect(ModEffects.STUN) || player.hasEffect(ModEffects.WEBBED) || StingerStateManager.isDowned(player) || RoundStartFreezeManager.isFrozen(player)) {
                return;
            }
            GizmoSkills.handleToolAction(player, msg.action, msg.targetEntityId);
            GizmoStateManager.syncToClient(player);
        });
    }
}
