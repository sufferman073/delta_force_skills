package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.character.department.DepartmentOfTransportationSkills;
import com.rzy.dealt_force_skills.character.department.DepartmentOfTransportationStateManager;
import com.rzy.dealt_force_skills.character.department.DepartmentToolAction;
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

public class C2S_DepartmentToolAction implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<C2S_DepartmentToolAction> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "c2s_department_tool_action"));

    public static final StreamCodec<FriendlyByteBuf, C2S_DepartmentToolAction> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    C2S_DepartmentToolAction::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final DepartmentToolAction action;
    private final int targetEntityId;

    public C2S_DepartmentToolAction(DepartmentToolAction action) {
        this(action, -1);
    }

    public C2S_DepartmentToolAction(DepartmentToolAction action, int targetEntityId) {
        this.action = action;
        this.targetEntityId = targetEntityId;
    }

    public static void encode(C2S_DepartmentToolAction msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.action.ordinal());
        buf.writeVarInt(msg.targetEntityId);
    }

    public static C2S_DepartmentToolAction decode(FriendlyByteBuf buf) {
        int ordinal = buf.readVarInt();
        DepartmentToolAction[] values = DepartmentToolAction.values();
        DepartmentToolAction action = ordinal >= 0 && ordinal < values.length ? values[ordinal] : DepartmentToolAction.STOW_TOOL;
        return new C2S_DepartmentToolAction(action, buf.readVarInt());
    }

    public static void handle(C2S_DepartmentToolAction msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (com.rzy.dealt_force_skills.climb.HvkZiplineManager.blocksToolAction(player, msg.action)) return;
            if (player == null || player.isSpectator() || UluruLoiteringMissileEntity.isPlayerControlling(player)) {
                return;
            }
            if (msg.action != DepartmentToolAction.CALIBRATION_RELEASE
                    && (player.hasEffect(ModEffects.STUN)
                    || player.hasEffect(ModEffects.WEBBED)
                    || StingerStateManager.isDowned(player)
                    || RoundStartFreezeManager.isFrozen(player))) {
                return;
            }
            DepartmentOfTransportationSkills.handleToolAction(player, msg.action, msg.targetEntityId);
            if (DepartmentOfTransportationStateManager.isDepartment(player)) {
                DepartmentOfTransportationStateManager.syncToClient(player);
            }
        });
    }
}
