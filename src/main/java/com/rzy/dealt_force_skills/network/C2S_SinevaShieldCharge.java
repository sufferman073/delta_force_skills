package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.character.sineva.SinevaSkills;
import com.rzy.dealt_force_skills.character.sineva.SinevaStateManager;
import com.rzy.dealt_force_skills.character.stinger.StingerStateManager;
import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.team.RoundStartFreezeManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class C2S_SinevaShieldCharge implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<C2S_SinevaShieldCharge> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "c2s_sineva_shield_charge"));

    public static final StreamCodec<FriendlyByteBuf, C2S_SinevaShieldCharge> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    C2S_SinevaShieldCharge::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(C2S_SinevaShieldCharge msg, FriendlyByteBuf buf) {
    }

    public static C2S_SinevaShieldCharge decode(FriendlyByteBuf buf) {
        return new C2S_SinevaShieldCharge();
    }

    public static void handle(C2S_SinevaShieldCharge msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player.isSpectator()) return;
            if (player.hasEffect(ModEffects.STUN) || player.hasEffect(ModEffects.WEBBED) || StingerStateManager.isDowned(player) || RoundStartFreezeManager.isFrozen(player)) return;
            SinevaSkills.tryShieldCharge(player);
            SinevaStateManager.syncToClient(player);
        });
    }
}
