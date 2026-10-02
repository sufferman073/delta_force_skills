package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.character.stinger.StingerSkills;
import com.rzy.dealt_force_skills.character.stinger.StingerStateManager;
import com.rzy.dealt_force_skills.character.stinger.StingerToolAction;
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

public class C2S_StingerToolAction implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<C2S_StingerToolAction> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "c2s_stinger_tool_action"));

    public static final StreamCodec<FriendlyByteBuf, C2S_StingerToolAction> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    C2S_StingerToolAction::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final StingerToolAction action;
    private final boolean alternate;

    public C2S_StingerToolAction(StingerToolAction action) {
        this(action, false);
    }

    public C2S_StingerToolAction(StingerToolAction action, boolean alternate) {
        this.action = action;
        this.alternate = alternate;
    }

    public static void encode(C2S_StingerToolAction msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.action.ordinal());
        buf.writeBoolean(msg.alternate);
    }

    public static C2S_StingerToolAction decode(FriendlyByteBuf buf) {
        int ordinal = buf.readVarInt();
        StingerToolAction[] values = StingerToolAction.values();
        StingerToolAction action = ordinal >= 0 && ordinal < values.length ? values[ordinal] : StingerToolAction.STOW_TOOL;
        return new C2S_StingerToolAction(action, buf.readBoolean());
    }

    public static void handle(C2S_StingerToolAction msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player == null || player.isSpectator() || UluruLoiteringMissileEntity.isPlayerControlling(player)) {
                return;
            }
            if (player.hasEffect(ModEffects.STUN)
                    || player.hasEffect(ModEffects.WEBBED)
                    || StingerStateManager.isDowned(player)
                    || RoundStartFreezeManager.isFrozen(player)) {
                return;
            }
            StingerSkills.handleToolAction(player, msg.action, msg.alternate);
            StingerStateManager.syncToClient(player);
        });
    }
}
