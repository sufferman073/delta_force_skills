package com.rzy.dealt_force_skills.network;

import net.minecraft.network.FriendlyByteBuf;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class S2C_SinevaKnockdown implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<S2C_SinevaKnockdown> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "s2c_sineva_knockdown"));

    public static final StreamCodec<FriendlyByteBuf, S2C_SinevaKnockdown> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    S2C_SinevaKnockdown::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final int entityId;
    private final int ticks;
    private final float yaw;

    public S2C_SinevaKnockdown(int entityId, int ticks, float yaw) {
        this.entityId = entityId;
        this.ticks = ticks;
        this.yaw = yaw;
    }

    public static void encode(S2C_SinevaKnockdown msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.entityId);
        buf.writeVarInt(msg.ticks);
        buf.writeFloat(msg.yaw);
    }

    public static S2C_SinevaKnockdown decode(FriendlyByteBuf buf) {
        return new S2C_SinevaKnockdown(buf.readVarInt(), buf.readVarInt(), buf.readFloat());
    }

    public static void handle(S2C_SinevaKnockdown msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientSinevaKnockdownState.sync(msg.entityId, msg.ticks, msg.yaw));
    }
}
