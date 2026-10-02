package com.rzy.dealt_force_skills.network;

import net.minecraft.network.FriendlyByteBuf;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class S2C_DWolfSlideAccepted implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<S2C_DWolfSlideAccepted> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "s2c_d_wolf_slide_accepted"));

    public static final StreamCodec<FriendlyByteBuf, S2C_DWolfSlideAccepted> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    S2C_DWolfSlideAccepted::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public S2C_DWolfSlideAccepted() {
    }

    public static void encode(S2C_DWolfSlideAccepted msg, FriendlyByteBuf buf) {
    }

    public static S2C_DWolfSlideAccepted decode(FriendlyByteBuf buf) {
        return new S2C_DWolfSlideAccepted();
    }

    public static void handle(S2C_DWolfSlideAccepted msg, IPayloadContext ctx) {
        ctx.enqueueWork(DWolfInputHandler::acceptSlide);
    }
}
