package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.character.catdad.CatDadStateManager;
import com.rzy.dealt_force_skills.character.nikaidou.NikaidouHiroSkills;
import com.rzy.dealt_force_skills.character.nikaidou.NikaidouHiroStateManager;
import com.rzy.dealt_force_skills.character.nikaidou.NikaidouHiroToolAction;
import com.rzy.dealt_force_skills.character.nikaidou.NikaidouHiroWitchificationSkills;
import com.rzy.dealt_force_skills.character.nikaidou.NikaidouHiroWitchificationStateManager;
import com.rzy.dealt_force_skills.character.stinger.StingerStateManager;
import com.rzy.dealt_force_skills.character.tempest.TempestStateManager;
import com.rzy.dealt_force_skills.character.vlinder.VlinderStateManager;
import com.rzy.dealt_force_skills.entity.RaptorFalconDroneEntity;
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

public class C2S_NikaidouHiroToolAction implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<C2S_NikaidouHiroToolAction> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "c2s_nikaidou_hiro_tool_action"));

    public static final StreamCodec<FriendlyByteBuf, C2S_NikaidouHiroToolAction> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    C2S_NikaidouHiroToolAction::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final NikaidouHiroToolAction action;

    public C2S_NikaidouHiroToolAction(NikaidouHiroToolAction action) {
        this.action = action;
    }

    public static void encode(C2S_NikaidouHiroToolAction msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.action.ordinal());
    }

    public static C2S_NikaidouHiroToolAction decode(FriendlyByteBuf buf) {
        int ordinal = buf.readVarInt();
        NikaidouHiroToolAction[] values = NikaidouHiroToolAction.values();
        return new C2S_NikaidouHiroToolAction(ordinal >= 0 && ordinal < values.length
                ? values[ordinal]
                : NikaidouHiroToolAction.ATTACK);
    }

    public static void handle(C2S_NikaidouHiroToolAction msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (com.rzy.dealt_force_skills.climb.HvkZiplineManager.blocksToolAction(player, msg.action)) return;
            if (player == null
                    || player.isSpectator()
                    || UluruLoiteringMissileEntity.isPlayerControlling(player)
                    || RaptorFalconDroneEntity.isPlayerControlling(player)
                    || player.hasEffect(ModEffects.STUN)
                    || player.hasEffect(ModEffects.WEBBED)
                    || player.hasEffect(ModEffects.TEMPEST_DISARMED)
                    || StingerStateManager.isDowned(player)
                    || VlinderStateManager.isDowned(player)
                    || TempestStateManager.isActionLocked(player)
                    || NikaidouHiroWitchificationStateManager.isActionLocked(player)
                    || CatDadStateManager.isDowned(player)
                    || RoundStartFreezeManager.isFrozen(player)) {
                return;
            }
            if (NikaidouHiroWitchificationStateManager.isNikaidouHiroWitchification(player)) {
                NikaidouHiroWitchificationSkills.handleToolAction(player, msg.action);
                NikaidouHiroWitchificationStateManager.syncToClient(player);
            } else {
                NikaidouHiroSkills.handleToolAction(player, msg.action);
                NikaidouHiroStateManager.syncToClient(player);
            }
        });
    }
}
