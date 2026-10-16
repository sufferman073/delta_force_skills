package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.character.catdad.CatDadStateManager;
import com.rzy.dealt_force_skills.character.saeed.SaeedFireArrowAction;
import com.rzy.dealt_force_skills.character.saeed.SaeedStateManager;
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

public class C2S_SaeedFireArrowAction implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<C2S_SaeedFireArrowAction> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "c2s_saeed_fire_arrow_action"));

    public static final StreamCodec<FriendlyByteBuf, C2S_SaeedFireArrowAction> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    C2S_SaeedFireArrowAction::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final SaeedFireArrowAction action;

    public C2S_SaeedFireArrowAction(SaeedFireArrowAction action) {
        this.action = action;
    }

    public static void encode(C2S_SaeedFireArrowAction msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.action.ordinal());
    }

    public static C2S_SaeedFireArrowAction decode(FriendlyByteBuf buf) {
        int ordinal = buf.readVarInt();
        SaeedFireArrowAction[] values = SaeedFireArrowAction.values();
        SaeedFireArrowAction action = ordinal >= 0 && ordinal < values.length
                ? values[ordinal]
                : SaeedFireArrowAction.STOW;
        return new C2S_SaeedFireArrowAction(action);
    }

    public static void handle(C2S_SaeedFireArrowAction msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (com.rzy.dealt_force_skills.climb.HvkZiplineManager.blocksToolAction(player, msg.action)) return;
            if (player == null
                    || player.isSpectator()
                    || !SaeedStateManager.isSaeed(player)
                    || UluruLoiteringMissileEntity.isPlayerControlling(player)
                    || RaptorFalconDroneEntity.isPlayerControlling(player)) {
                return;
            }
            if (player.hasEffect(ModEffects.STUN)
                    || player.hasEffect(ModEffects.WEBBED)
                    || StingerStateManager.isDowned(player)
                    || VlinderStateManager.isDowned(player)
                    || CatDadStateManager.isDowned(player)
                    || TempestStateManager.isActionLocked(player)
                    || RoundStartFreezeManager.isFrozen(player)) {
                return;
            }
            switch (msg.action) {
                case FIRE -> SaeedStateManager.fireEquippedCrossbow(player);
                case TOGGLE_BOUNCE -> SaeedStateManager.toggleFireBounce(player);
                case STOW -> SaeedStateManager.stowFireCrossbow(player);
            }
            SaeedStateManager.syncToClient(player);
        });
    }
}
