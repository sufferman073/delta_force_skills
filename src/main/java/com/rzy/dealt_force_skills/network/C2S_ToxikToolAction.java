package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.character.stinger.StingerStateManager;
import com.rzy.dealt_force_skills.character.toxik.ToxikSkills;
import com.rzy.dealt_force_skills.character.toxik.ToxikStateManager;
import com.rzy.dealt_force_skills.character.toxik.ToxikToolAction;
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

public class C2S_ToxikToolAction implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<C2S_ToxikToolAction> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "c2s_toxik_tool_action"));

    public static final StreamCodec<FriendlyByteBuf, C2S_ToxikToolAction> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    C2S_ToxikToolAction::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final ToxikToolAction action;
    private final boolean alternate;

    public C2S_ToxikToolAction(ToxikToolAction action) {
        this(action, false);
    }

    public C2S_ToxikToolAction(ToxikToolAction action, boolean alternate) {
        this.action = action;
        this.alternate = alternate;
    }

    public static void encode(C2S_ToxikToolAction msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.action.ordinal());
        buf.writeBoolean(msg.alternate);
    }

    public static C2S_ToxikToolAction decode(FriendlyByteBuf buf) {
        int ordinal = buf.readVarInt();
        ToxikToolAction[] values = ToxikToolAction.values();
        ToxikToolAction action = ordinal >= 0 && ordinal < values.length ? values[ordinal] : ToxikToolAction.STOW_TOOL;
        return new C2S_ToxikToolAction(action, buf.readBoolean());
    }

    public static void handle(C2S_ToxikToolAction msg, IPayloadContext ctx) {
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
            ToxikSkills.handleToolAction(player, msg.action, msg.alternate);
            ToxikStateManager.syncToClient(player);
        });
    }
}
