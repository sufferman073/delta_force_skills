package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.character.manba.ManbaSkills;
import com.rzy.dealt_force_skills.character.manba.ManbaStateManager;
import com.rzy.dealt_force_skills.character.manba.ManbaToolAction;
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

public class C2S_ManbaToolAction implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<C2S_ManbaToolAction> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "c2s_manba_tool_action"));

    public static final StreamCodec<FriendlyByteBuf, C2S_ManbaToolAction> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    C2S_ManbaToolAction::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final ManbaToolAction action;
    private final int targetEntityId;

    public C2S_ManbaToolAction(ManbaToolAction action) {
        this(action, -1);
    }

    public C2S_ManbaToolAction(ManbaToolAction action, int targetEntityId) {
        this.action = action;
        this.targetEntityId = targetEntityId;
    }

    public static void encode(C2S_ManbaToolAction msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.action.ordinal());
        buf.writeVarInt(msg.targetEntityId);
    }

    public static C2S_ManbaToolAction decode(FriendlyByteBuf buf) {
        int ordinal = buf.readVarInt();
        ManbaToolAction[] values = ManbaToolAction.values();
        ManbaToolAction action = ordinal >= 0 && ordinal < values.length ? values[ordinal] : ManbaToolAction.STOP_FLASHLIGHT;
        return new C2S_ManbaToolAction(action, buf.readVarInt());
    }

    public static void handle(C2S_ManbaToolAction msg, IPayloadContext ctx) {
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
            ManbaSkills.handleToolAction(player, msg.action, msg.targetEntityId);
            ManbaStateManager.syncToClient(player);
        });
    }
}
