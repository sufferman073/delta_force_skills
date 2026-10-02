package com.rzy.dealt_force_skills.network;

import net.minecraft.network.FriendlyByteBuf;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class S2C_SinevaShieldStaminaConsume implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<S2C_SinevaShieldStaminaConsume> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "s2c_sineva_shield_stamina_consume"));

    public static final StreamCodec<FriendlyByteBuf, S2C_SinevaShieldStaminaConsume> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    S2C_SinevaShieldStaminaConsume::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final int percent;
    private final int floorPercent;

    public S2C_SinevaShieldStaminaConsume(int percent, int floorPercent) {
        this.percent = percent;
        this.floorPercent = floorPercent;
    }

    public static void encode(S2C_SinevaShieldStaminaConsume msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.percent);
        buf.writeVarInt(msg.floorPercent);
    }

    public static S2C_SinevaShieldStaminaConsume decode(FriendlyByteBuf buf) {
        return new S2C_SinevaShieldStaminaConsume(buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(S2C_SinevaShieldStaminaConsume msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientSinevaHudState.consumeShieldBlockStamina(msg.percent, msg.floorPercent));
    }
}
