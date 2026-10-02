package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.commander.CommanderStateManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class C2S_OpenCommanderScreen implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<C2S_OpenCommanderScreen> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "c2s_open_commander_screen"));

    public static final StreamCodec<FriendlyByteBuf, C2S_OpenCommanderScreen> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    C2S_OpenCommanderScreen::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final boolean support;

    public C2S_OpenCommanderScreen(boolean support) {
        this.support = support;
    }

    public static void encode(C2S_OpenCommanderScreen msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.support);
    }

    public static C2S_OpenCommanderScreen decode(FriendlyByteBuf buf) {
        return new C2S_OpenCommanderScreen(buf.readBoolean());
    }

    public static void handle(C2S_OpenCommanderScreen msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player != null && CommanderStateManager.isCommander(player)) {
                CommanderStateManager.initializeIfNeeded(player);
                CommanderStateManager.syncToClient(player);
                NetworkHandler.sendToPlayer(new S2C_OpenCommanderScreen(msg.support), player);
            }
        });
    }
}