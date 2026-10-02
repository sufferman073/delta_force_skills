package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.commander.CommanderStateManager;
import com.rzy.dealt_force_skills.character.commander.CommanderUpgrade;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class C2S_BuyCommanderUpgrade implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<C2S_BuyCommanderUpgrade> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "c2s_buy_commander_upgrade"));

    public static final StreamCodec<FriendlyByteBuf, C2S_BuyCommanderUpgrade> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    C2S_BuyCommanderUpgrade::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final CommanderUpgrade upgrade;

    public C2S_BuyCommanderUpgrade(CommanderUpgrade upgrade) {
        this.upgrade = upgrade;
    }

    public static void encode(C2S_BuyCommanderUpgrade msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.upgrade.ordinal());
    }

    public static C2S_BuyCommanderUpgrade decode(FriendlyByteBuf buf) {
        int ordinal = buf.readVarInt();
        CommanderUpgrade[] values = CommanderUpgrade.values();
        return new C2S_BuyCommanderUpgrade(ordinal >= 0 && ordinal < values.length ? values[ordinal] : values[0]);
    }

    public static void handle(C2S_BuyCommanderUpgrade msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player != null && CommanderStateManager.isCommander(player)) {
                CommanderStateManager.buyUpgrade(player, msg.upgrade);
            }
        });
    }
}