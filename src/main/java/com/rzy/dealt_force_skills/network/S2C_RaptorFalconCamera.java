package com.rzy.dealt_force_skills.network;

import net.minecraft.network.FriendlyByteBuf;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class S2C_RaptorFalconCamera implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<S2C_RaptorFalconCamera> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "s2c_raptor_falcon_camera"));

    public static final StreamCodec<FriendlyByteBuf, S2C_RaptorFalconCamera> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    S2C_RaptorFalconCamera::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final int entityId;
    private final boolean active;

    public S2C_RaptorFalconCamera(int entityId, boolean active) {
        this.entityId = entityId;
        this.active = active;
    }

    public static void encode(S2C_RaptorFalconCamera msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.entityId);
        buf.writeBoolean(msg.active);
    }

    public static S2C_RaptorFalconCamera decode(FriendlyByteBuf buf) {
        return new S2C_RaptorFalconCamera(buf.readVarInt(), buf.readBoolean());
    }

    public static void handle(S2C_RaptorFalconCamera msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
                    if (msg.active) {
                        RaptorFalconController.start(msg.entityId);
                    } else {
                        RaptorFalconController.stop(false);
                    }
                });
    }
}
