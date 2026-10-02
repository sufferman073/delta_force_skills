package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.character.uluru.UluruSkills;
import com.rzy.dealt_force_skills.character.uluru.UluruStateManager;
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

public class C2S_UluruToolAction implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<C2S_UluruToolAction> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "c2s_uluru_tool_action"));

    public static final StreamCodec<FriendlyByteBuf, C2S_UluruToolAction> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    C2S_UluruToolAction::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final boolean secondary;
    private final boolean guidedLaunch;

    public C2S_UluruToolAction(boolean secondary, boolean guidedLaunch) {
        this.secondary = secondary;
        this.guidedLaunch = guidedLaunch;
    }

    public static void encode(C2S_UluruToolAction msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.secondary);
        buf.writeBoolean(msg.guidedLaunch);
    }

    public static C2S_UluruToolAction decode(FriendlyByteBuf buf) {
        return new C2S_UluruToolAction(buf.readBoolean(), buf.readBoolean());
    }

    public static void handle(C2S_UluruToolAction msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player == null || player.isSpectator()) {
                return;
            }
            if (player.hasEffect(ModEffects.STUN) || player.hasEffect(ModEffects.WEBBED) || StingerStateManager.isDowned(player) || RoundStartFreezeManager.isFrozen(player)) {
                return;
            }
            if (UluruLoiteringMissileEntity.isPlayerControlling(player)) {
                return;
            }
            UluruSkills.handleToolAction(player, msg.secondary, msg.guidedLaunch);
            UluruStateManager.syncToClient(player);
        });
    }
}
