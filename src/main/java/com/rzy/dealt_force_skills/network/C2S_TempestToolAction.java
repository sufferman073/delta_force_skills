package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.character.stinger.StingerStateManager;
import com.rzy.dealt_force_skills.character.tempest.TempestSkills;
import com.rzy.dealt_force_skills.character.tempest.TempestStateManager;
import com.rzy.dealt_force_skills.character.tempest.TempestToolAction;
import com.rzy.dealt_force_skills.character.vlinder.VlinderStateManager;
import com.rzy.dealt_force_skills.entity.RaptorFalconDroneEntity;
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

public class C2S_TempestToolAction implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<C2S_TempestToolAction> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "c2s_tempest_tool_action"));

    public static final StreamCodec<FriendlyByteBuf, C2S_TempestToolAction> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    C2S_TempestToolAction::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final TempestToolAction action;
    private final boolean highThrow;

    public C2S_TempestToolAction(TempestToolAction action) {
        this(action, false);
    }

    public C2S_TempestToolAction(TempestToolAction action, boolean highThrow) {
        this.action = action;
        this.highThrow = highThrow;
    }

    public static void encode(C2S_TempestToolAction msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.action.ordinal());
        buf.writeBoolean(msg.highThrow);
    }

    public static C2S_TempestToolAction decode(FriendlyByteBuf buf) {
        int ordinal = buf.readVarInt();
        TempestToolAction[] values = TempestToolAction.values();
        TempestToolAction action = ordinal >= 0 && ordinal < values.length ? values[ordinal] : TempestToolAction.STOW_TOOL;
        return new C2S_TempestToolAction(action, buf.readBoolean());
    }

    public static void handle(C2S_TempestToolAction msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player == null
                    || player.isSpectator()
                    || UluruLoiteringMissileEntity.isPlayerControlling(player)
                    || RaptorFalconDroneEntity.isPlayerControlling(player)) {
                return;
            }
            if (player.hasEffect(ModEffects.STUN)
                    || player.hasEffect(ModEffects.WEBBED)
                    || player.hasEffect(ModEffects.TEMPEST_DISARMED)
                    || StingerStateManager.isDowned(player)
                    || VlinderStateManager.isDowned(player)
                    || TempestStateManager.isActionLocked(player)
                    || RoundStartFreezeManager.isFrozen(player)) {
                return;
            }
            TempestSkills.handleToolAction(player, msg.action, msg.highThrow);
            TempestStateManager.syncToClient(player);
        });
    }
}
