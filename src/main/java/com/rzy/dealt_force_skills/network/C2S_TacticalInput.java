package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.motion.TacticalMovementInput;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record C2S_TacticalInput(int forward, int left, boolean sprint) implements CustomPacketPayload {
    public static final Type<C2S_TacticalInput> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "c2s_tactical_input"));
    public static final StreamCodec<FriendlyByteBuf, C2S_TacticalInput> STREAM_CODEC =
            StreamCodec.of((buf, msg) -> encode(msg, buf), C2S_TacticalInput::decode);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void encode(C2S_TacticalInput msg, FriendlyByteBuf buf) {
        buf.writeByte(msg.forward); buf.writeByte(msg.left); buf.writeBoolean(msg.sprint);
    }
    public static C2S_TacticalInput decode(FriendlyByteBuf buf) {
        return new C2S_TacticalInput(buf.readByte(), buf.readByte(), buf.readBoolean());
    }
    public static void handle(C2S_TacticalInput msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> TacticalMovementInput.update((ServerPlayer) ctx.player(), msg.forward, msg.left, msg.sprint));
    }
}
