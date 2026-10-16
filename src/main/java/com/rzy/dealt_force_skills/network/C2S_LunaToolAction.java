package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.character.luna.LunaSkills;
import com.rzy.dealt_force_skills.character.luna.LunaStateManager;
import com.rzy.dealt_force_skills.character.luna.LunaToolAction;
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

public class C2S_LunaToolAction implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<C2S_LunaToolAction> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "c2s_luna_tool_action"));

    public static final StreamCodec<FriendlyByteBuf, C2S_LunaToolAction> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    C2S_LunaToolAction::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final LunaToolAction action;
    private final int chargeTicks;

    public C2S_LunaToolAction(LunaToolAction action) {
        this(action, 0);
    }

    public C2S_LunaToolAction(LunaToolAction action, int chargeTicks) {
        this.action = action;
        this.chargeTicks = chargeTicks;
    }

    public static void encode(C2S_LunaToolAction msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.action.ordinal());
        buf.writeVarInt(msg.chargeTicks);
    }

    public static C2S_LunaToolAction decode(FriendlyByteBuf buf) {
        int ordinal = buf.readVarInt();
        LunaToolAction[] values = LunaToolAction.values();
        LunaToolAction action = ordinal >= 0 && ordinal < values.length ? values[ordinal] : LunaToolAction.STOW_TOOL;
        return new C2S_LunaToolAction(action, buf.readVarInt());
    }

    public static void handle(C2S_LunaToolAction msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (com.rzy.dealt_force_skills.climb.HvkZiplineManager.blocksToolAction(player, msg.action)) return;
            if (player == null || player.isSpectator() || UluruLoiteringMissileEntity.isPlayerControlling(player)) {
                return;
            }
            if (player.hasEffect(ModEffects.STUN) || player.hasEffect(ModEffects.WEBBED) || StingerStateManager.isDowned(player) || RoundStartFreezeManager.isFrozen(player)) {
                return;
            }
            LunaSkills.handleToolAction(player, msg.action, msg.chargeTicks);
            LunaStateManager.syncToClient(player);
        });
    }
}
