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
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class C2S_LunaToolAction {
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

    public static void handle(C2S_LunaToolAction msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null && (msg.action == LunaToolAction.FIRE_SHOCK_ARROW || msg.action == LunaToolAction.FIRE_RECON_ARROW)) {
                LunaSkills.endDraw(player);
            }
            if (com.rzy.dealt_force_skills.climb.HvkZiplineManager.blocksToolAction(player, msg.action)) return;
            if (player == null || player.isSpectator() || UluruLoiteringMissileEntity.isPlayerControlling(player)) {
                return;
            }
            if (player.hasEffect(ModEffects.STUN.get()) || player.hasEffect(ModEffects.WEBBED.get()) || StingerStateManager.isDowned(player) || RoundStartFreezeManager.isFrozen(player)) {
                return;
            }
            LunaSkills.handleToolAction(player, msg.action, msg.chargeTicks);
            LunaStateManager.syncToClient(player);
        });
        ctx.get().setPacketHandled(true);
    }
}
