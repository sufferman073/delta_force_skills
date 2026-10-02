package com.rzy.dealt_force_skills.network;

import net.minecraft.network.FriendlyByteBuf;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class S2C_DWolfStaminaRestore implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<S2C_DWolfStaminaRestore> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "s2c_d_wolf_stamina_restore"));

    public static final StreamCodec<FriendlyByteBuf, S2C_DWolfStaminaRestore> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    S2C_DWolfStaminaRestore::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final int percent;

    public S2C_DWolfStaminaRestore(int percent) {
        this.percent = percent;
    }

    public static void encode(S2C_DWolfStaminaRestore msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.percent);
    }

    public static S2C_DWolfStaminaRestore decode(FriendlyByteBuf buf) {
        return new S2C_DWolfStaminaRestore(buf.readVarInt());
    }

    public static void handle(S2C_DWolfStaminaRestore msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientDWolfHudState.restoreStaminaPercent(msg.percent));
    }
}
