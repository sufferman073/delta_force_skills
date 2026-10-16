package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.character.hackclaw.HackclawSkills;
import com.rzy.dealt_force_skills.character.hackclaw.HackclawStateManager;
import com.rzy.dealt_force_skills.character.hackclaw.HackclawToolAction;
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

public class C2S_HackclawToolAction implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<C2S_HackclawToolAction> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "c2s_hackclaw_tool_action"));

    public static final StreamCodec<FriendlyByteBuf, C2S_HackclawToolAction> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    C2S_HackclawToolAction::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final HackclawToolAction action;
    private final int targetEntityId;

    public C2S_HackclawToolAction(HackclawToolAction action) {
        this(action, -1);
    }

    public C2S_HackclawToolAction(HackclawToolAction action, int targetEntityId) {
        this.action = action;
        this.targetEntityId = targetEntityId;
    }

    public static void encode(C2S_HackclawToolAction msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.action.ordinal());
        buf.writeVarInt(msg.targetEntityId);
    }

    public static C2S_HackclawToolAction decode(FriendlyByteBuf buf) {
        int ordinal = buf.readVarInt();
        HackclawToolAction[] values = HackclawToolAction.values();
        HackclawToolAction action = ordinal >= 0 && ordinal < values.length
                ? values[ordinal]
                : HackclawToolAction.STOW_TOOL;
        return new C2S_HackclawToolAction(action, buf.readVarInt());
    }

    public static void handle(C2S_HackclawToolAction msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (com.rzy.dealt_force_skills.climb.HvkZiplineManager.blocksToolAction(player, msg.action)) return;
            if (player == null || player.isSpectator() || UluruLoiteringMissileEntity.isPlayerControlling(player)) {
                return;
            }
            if (player.hasEffect(ModEffects.STUN)
                    || player.hasEffect(ModEffects.WEBBED)
                    || StingerStateManager.isDowned(player)
                    || RoundStartFreezeManager.isFrozen(player)) {
                return;
            }
            HackclawSkills.handleToolAction(player, msg.action, msg.targetEntityId);
            HackclawStateManager.syncToClient(player);
        });
    }
}
