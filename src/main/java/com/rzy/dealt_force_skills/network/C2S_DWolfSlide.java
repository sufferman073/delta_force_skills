package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.character.dwolf.DWolfSkills;
import com.rzy.dealt_force_skills.character.dwolf.DWolfStateManager;
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

public class C2S_DWolfSlide implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<C2S_DWolfSlide> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "c2s_d_wolf_slide"));

    public static final StreamCodec<FriendlyByteBuf, C2S_DWolfSlide> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    C2S_DWolfSlide::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public C2S_DWolfSlide() {
    }

    public static void encode(C2S_DWolfSlide msg, FriendlyByteBuf buf) {
    }

    public static C2S_DWolfSlide decode(FriendlyByteBuf buf) {
        return new C2S_DWolfSlide();
    }

    public static void handle(C2S_DWolfSlide msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player == null || player.isSpectator() || UluruLoiteringMissileEntity.isPlayerControlling(player)) {
                return;
            }
            if (player.hasEffect(ModEffects.STUN) || player.hasEffect(ModEffects.WEBBED) || StingerStateManager.isDowned(player) || RoundStartFreezeManager.isFrozen(player)) {
                return;
            }
            boolean accepted = DWolfSkills.tryTacticalSlide(player);
            if (accepted) {
                NetworkHandler.sendToPlayer(new S2C_DWolfSlideAccepted(), player);
            }
            DWolfStateManager.syncToClient(player);
        });
    }
}
