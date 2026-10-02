package com.rzy.dealt_force_skills.network;

import net.minecraft.network.FriendlyByteBuf;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class S2C_CharacterHitFeedback implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<S2C_CharacterHitFeedback> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "s2c_character_hit_feedback"));

    public static final StreamCodec<FriendlyByteBuf, S2C_CharacterHitFeedback> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    S2C_CharacterHitFeedback::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final int hurtEntityId;
    private final float amount;

    public S2C_CharacterHitFeedback(int hurtEntityId, float amount) {
        this.hurtEntityId = hurtEntityId;
        this.amount = amount;
    }

    public static void encode(S2C_CharacterHitFeedback msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.hurtEntityId);
        buf.writeFloat(msg.amount);
    }

    public static S2C_CharacterHitFeedback decode(FriendlyByteBuf buf) {
        return new S2C_CharacterHitFeedback(buf.readVarInt(), buf.readFloat());
    }

    public static void handle(S2C_CharacterHitFeedback msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientPacketHandlers.characterHitFeedback(msg.hurtEntityId, msg.amount));
    }
}
