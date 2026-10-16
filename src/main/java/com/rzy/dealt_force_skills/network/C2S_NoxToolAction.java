package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.character.nox.NoxSkills;
import com.rzy.dealt_force_skills.character.nox.NoxStateManager;
import com.rzy.dealt_force_skills.character.nox.NoxToolAction;
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

public class C2S_NoxToolAction implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<C2S_NoxToolAction> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "c2s_nox_tool_action"));

    public static final StreamCodec<FriendlyByteBuf, C2S_NoxToolAction> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    C2S_NoxToolAction::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final NoxToolAction action;
    private final int targetEntityId;
    private final boolean alternate;

    public C2S_NoxToolAction(NoxToolAction action) {
        this(action, -1, false);
    }

    public C2S_NoxToolAction(NoxToolAction action, int targetEntityId, boolean alternate) {
        this.action = action;
        this.targetEntityId = targetEntityId;
        this.alternate = alternate;
    }

    public static void encode(C2S_NoxToolAction msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.action.ordinal());
        buf.writeVarInt(msg.targetEntityId);
        buf.writeBoolean(msg.alternate);
    }

    public static C2S_NoxToolAction decode(FriendlyByteBuf buf) {
        int ordinal = buf.readVarInt();
        NoxToolAction[] values = NoxToolAction.values();
        NoxToolAction action = ordinal >= 0 && ordinal < values.length ? values[ordinal] : NoxToolAction.STOW_TOOL;
        return new C2S_NoxToolAction(action, buf.readVarInt(), buf.readBoolean());
    }

    public static void handle(C2S_NoxToolAction msg, IPayloadContext ctx) {
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
            NoxSkills.handleToolAction(player, msg.action, msg.targetEntityId, msg.alternate);
            NoxStateManager.syncToClient(player);
        });
    }
}
