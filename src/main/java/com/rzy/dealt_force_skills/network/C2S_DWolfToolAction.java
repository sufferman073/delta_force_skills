package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.character.dwolf.DWolfSkills;
import com.rzy.dealt_force_skills.character.dwolf.DWolfStateManager;
import com.rzy.dealt_force_skills.character.dwolf.DWolfToolAction;
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

public class C2S_DWolfToolAction implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<C2S_DWolfToolAction> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "c2s_d_wolf_tool_action"));

    public static final StreamCodec<FriendlyByteBuf, C2S_DWolfToolAction> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    C2S_DWolfToolAction::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final DWolfToolAction action;

    public C2S_DWolfToolAction(DWolfToolAction action) {
        this.action = action;
    }

    public static void encode(C2S_DWolfToolAction msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.action.ordinal());
    }

    public static C2S_DWolfToolAction decode(FriendlyByteBuf buf) {
        int ordinal = buf.readVarInt();
        DWolfToolAction[] values = DWolfToolAction.values();
        DWolfToolAction action = ordinal >= 0 && ordinal < values.length ? values[ordinal] : DWolfToolAction.STOW_TOOL;
        return new C2S_DWolfToolAction(action);
    }

    public static void handle(C2S_DWolfToolAction msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (com.rzy.dealt_force_skills.climb.HvkZiplineManager.blocksToolAction(player, msg.action)) return;
            if (player == null || player.isSpectator() || UluruLoiteringMissileEntity.isPlayerControlling(player)) {
                return;
            }
            if (player.hasEffect(ModEffects.STUN) || player.hasEffect(ModEffects.WEBBED) || StingerStateManager.isDowned(player) || RoundStartFreezeManager.isFrozen(player)) {
                return;
            }
            DWolfSkills.handleToolAction(player, msg.action);
            DWolfStateManager.syncToClient(player);
        });
    }
}
