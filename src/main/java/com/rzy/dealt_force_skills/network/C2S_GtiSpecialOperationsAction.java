package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.shop.GtiTradingHallManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record C2S_GtiSpecialOperationsAction(Action action, int index, int count) implements CustomPacketPayload {
    public static final Type<C2S_GtiSpecialOperationsAction> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "c2s_gti_special_operations_action"));
    public static final StreamCodec<FriendlyByteBuf, C2S_GtiSpecialOperationsAction> STREAM_CODEC = StreamCodec.of(C2S_GtiSpecialOperationsAction::encode, C2S_GtiSpecialOperationsAction::decode);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public enum Action { OPEN_BASE, OPEN_MARKET, BUY }
    private static void encode(FriendlyByteBuf buf, C2S_GtiSpecialOperationsAction msg) { buf.writeEnum(msg.action); buf.writeVarInt(msg.index); buf.writeVarInt(msg.count); }
    private static C2S_GtiSpecialOperationsAction decode(FriendlyByteBuf buf) { return new C2S_GtiSpecialOperationsAction(buf.readEnum(Action.class), buf.readVarInt(), buf.readVarInt()); }
    public static void handle(C2S_GtiSpecialOperationsAction msg, IPayloadContext ctx) { ctx.enqueueWork(() -> { if (!(ctx.player() instanceof ServerPlayer player)) return; boolean gtiAccess = GtiTradingHallManager.hasBaseAccess(player); switch(msg.action) { case OPEN_BASE -> { if (!gtiAccess) GtiTradingHallManager.grantBaseAccess(player); NetworkHandler.sendToPlayer(S2C_SpecialOperationsBaseSnapshot.capture(player, 0), player); } case OPEN_MARKET -> { if (!gtiAccess) GtiTradingHallManager.grantBaseAccess(player); GtiTradingHallManager.open(player); } case BUY -> { if (gtiAccess) GtiTradingHallManager.buy(player, msg.index, msg.count); } } }); }
}
