package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.util.EmptyHandExtinguishHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record C2S_EmptyHandExtinguish(boolean holding) implements CustomPacketPayload {
    public static final Type<C2S_EmptyHandExtinguish> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(
            DealtForceSkillsMod.MODID, "c2s_empty_hand_extinguish"));
    public static final StreamCodec<FriendlyByteBuf, C2S_EmptyHandExtinguish> STREAM_CODEC =
            StreamCodec.of(C2S_EmptyHandExtinguish::encode, C2S_EmptyHandExtinguish::decode);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(FriendlyByteBuf buffer, C2S_EmptyHandExtinguish message) {
        buffer.writeBoolean(message.holding);
    }

    public static C2S_EmptyHandExtinguish decode(FriendlyByteBuf buffer) {
        return new C2S_EmptyHandExtinguish(buffer.readBoolean());
    }

    public static void handle(C2S_EmptyHandExtinguish message, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                EmptyHandExtinguishHandler.signal(player, message.holding);
            }
        });
    }
}
