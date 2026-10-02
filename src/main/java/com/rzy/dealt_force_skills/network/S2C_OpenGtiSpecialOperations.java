package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record S2C_OpenGtiSpecialOperations() implements CustomPacketPayload {
    public static final Type<S2C_OpenGtiSpecialOperations> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "s2c_open_gti_special_operations"));
    public static final StreamCodec<FriendlyByteBuf, S2C_OpenGtiSpecialOperations> STREAM_CODEC = StreamCodec.of((buf,msg)->{}, buf -> new S2C_OpenGtiSpecialOperations());
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void handle(S2C_OpenGtiSpecialOperations msg, IPayloadContext ctx) { ctx.enqueueWork(GtiSpecialOperationsScreen::open); }
}
