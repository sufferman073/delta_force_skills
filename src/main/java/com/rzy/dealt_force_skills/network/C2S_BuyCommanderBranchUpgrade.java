package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.commander.CommanderBranchUpgrade;
import com.rzy.dealt_force_skills.character.commander.CommanderStateManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class C2S_BuyCommanderBranchUpgrade implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<C2S_BuyCommanderBranchUpgrade> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "c2s_buy_commander_branch_upgrade"));

    public static final StreamCodec<FriendlyByteBuf, C2S_BuyCommanderBranchUpgrade> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    C2S_BuyCommanderBranchUpgrade::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final CommanderBranchUpgrade upgrade;

    public C2S_BuyCommanderBranchUpgrade(CommanderBranchUpgrade upgrade) {
        this.upgrade = upgrade;
    }

    public static void encode(C2S_BuyCommanderBranchUpgrade msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.upgrade.ordinal());
    }

    public static C2S_BuyCommanderBranchUpgrade decode(FriendlyByteBuf buf) {
        int ordinal = buf.readVarInt();
        CommanderBranchUpgrade[] values = CommanderBranchUpgrade.values();
        return new C2S_BuyCommanderBranchUpgrade(
                ordinal >= 0 && ordinal < values.length ? values[ordinal] : values[0]);
    }

    public static void handle(C2S_BuyCommanderBranchUpgrade msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player != null && CommanderStateManager.isCommander(player)) {
                CommanderStateManager.buyBranchUpgrade(player, msg.upgrade);
            }
        });
    }
}