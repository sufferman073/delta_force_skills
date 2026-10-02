package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.character.stinger.StingerStateManager;
import com.rzy.dealt_force_skills.entity.LunaShockArrowEntity;
import com.rzy.dealt_force_skills.team.RoundStartFreezeManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class C2S_LunaShockArrowPull implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<C2S_LunaShockArrowPull> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "c2s_luna_shock_arrow_pull"));

    public static final StreamCodec<FriendlyByteBuf, C2S_LunaShockArrowPull> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    C2S_LunaShockArrowPull::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final boolean holding;

    public C2S_LunaShockArrowPull(boolean holding) {
        this.holding = holding;
    }

    public static void encode(C2S_LunaShockArrowPull msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.holding);
    }

    public static C2S_LunaShockArrowPull decode(FriendlyByteBuf buf) {
        return new C2S_LunaShockArrowPull(buf.readBoolean());
    }

    public static void handle(C2S_LunaShockArrowPull msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player != null && !player.isSpectator() && !StingerStateManager.isDowned(player)
                    && !RoundStartFreezeManager.isFrozen(player)) {
                LunaShockArrowEntity.reportPullHold(player, msg.holding);
            }
        });
    }
}
