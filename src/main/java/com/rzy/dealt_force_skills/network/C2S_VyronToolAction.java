package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.character.vyron.VyronSkills;
import com.rzy.dealt_force_skills.character.vyron.VyronStateManager;
import com.rzy.dealt_force_skills.character.vyron.VyronToolAction;
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

public class C2S_VyronToolAction implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<C2S_VyronToolAction> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "c2s_vyron_tool_action"));

    public static final StreamCodec<FriendlyByteBuf, C2S_VyronToolAction> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    C2S_VyronToolAction::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final VyronToolAction action;
    private final boolean highThrow;

    public C2S_VyronToolAction(VyronToolAction action) {
        this(action, false);
    }

    public C2S_VyronToolAction(VyronToolAction action, boolean highThrow) {
        this.action = action;
        this.highThrow = highThrow;
    }

    public static void encode(C2S_VyronToolAction msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.action.ordinal());
        buf.writeBoolean(msg.highThrow);
    }

    public static C2S_VyronToolAction decode(FriendlyByteBuf buf) {
        int ordinal = buf.readVarInt();
        VyronToolAction[] values = VyronToolAction.values();
        VyronToolAction action = ordinal >= 0 && ordinal < values.length ? values[ordinal] : VyronToolAction.STOW_TOOL;
        return new C2S_VyronToolAction(action, buf.readBoolean());
    }

    public static void handle(C2S_VyronToolAction msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player == null || player.isSpectator() || UluruLoiteringMissileEntity.isPlayerControlling(player)) {
                return;
            }
            if (player.hasEffect(ModEffects.STUN) || player.hasEffect(ModEffects.WEBBED) || StingerStateManager.isDowned(player) || RoundStartFreezeManager.isFrozen(player)) {
                return;
            }
            VyronSkills.handleToolAction(player, msg.action, msg.highThrow);
            VyronStateManager.syncToClient(player);
        });
    }
}
