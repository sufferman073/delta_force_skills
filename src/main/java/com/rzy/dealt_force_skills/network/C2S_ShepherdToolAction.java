package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.character.shepherd.ShepherdSkills;
import com.rzy.dealt_force_skills.character.shepherd.ShepherdStateManager;
import com.rzy.dealt_force_skills.character.shepherd.ShepherdToolAction;
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

public class C2S_ShepherdToolAction implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<C2S_ShepherdToolAction> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "c2s_shepherd_tool_action"));

    public static final StreamCodec<FriendlyByteBuf, C2S_ShepherdToolAction> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    C2S_ShepherdToolAction::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final ShepherdToolAction action;
    private final int targetEntityId;

    public C2S_ShepherdToolAction(ShepherdToolAction action) {
        this(action, -1);
    }

    public C2S_ShepherdToolAction(ShepherdToolAction action, int targetEntityId) {
        this.action = action;
        this.targetEntityId = targetEntityId;
    }

    public static void encode(C2S_ShepherdToolAction msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.action.ordinal());
        buf.writeVarInt(msg.targetEntityId);
    }

    public static C2S_ShepherdToolAction decode(FriendlyByteBuf buf) {
        int ordinal = buf.readVarInt();
        ShepherdToolAction[] values = ShepherdToolAction.values();
        ShepherdToolAction action = ordinal >= 0 && ordinal < values.length ? values[ordinal] : ShepherdToolAction.STOW_TOOL;
        return new C2S_ShepherdToolAction(action, buf.readVarInt());
    }

    public static void handle(C2S_ShepherdToolAction msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (com.rzy.dealt_force_skills.climb.HvkZiplineManager.blocksToolAction(player, msg.action)) return;
            if (player == null || player.isSpectator() || UluruLoiteringMissileEntity.isPlayerControlling(player)) {
                return;
            }
            if (player.hasEffect(ModEffects.STUN) || player.hasEffect(ModEffects.WEBBED) || StingerStateManager.isDowned(player) || RoundStartFreezeManager.isFrozen(player)) {
                return;
            }
            ShepherdSkills.handleToolAction(player, msg.action, msg.targetEntityId);
            ShepherdStateManager.syncToClient(player);
        });
    }
}
