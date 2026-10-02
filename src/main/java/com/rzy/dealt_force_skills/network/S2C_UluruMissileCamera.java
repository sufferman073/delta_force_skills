package com.rzy.dealt_force_skills.network;

import net.minecraft.network.FriendlyByteBuf;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class S2C_UluruMissileCamera implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<S2C_UluruMissileCamera> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "s2c_uluru_missile_camera"));

    public static final StreamCodec<FriendlyByteBuf, S2C_UluruMissileCamera> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    S2C_UluruMissileCamera::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final int entityId;
    private final boolean active;

    public S2C_UluruMissileCamera(int entityId, boolean active) {
        this.entityId = entityId;
        this.active = active;
    }

    public static void encode(S2C_UluruMissileCamera msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.entityId);
        buf.writeBoolean(msg.active);
    }

    public static S2C_UluruMissileCamera decode(FriendlyByteBuf buf) {
        return new S2C_UluruMissileCamera(buf.readVarInt(), buf.readBoolean());
    }

    public static void handle(S2C_UluruMissileCamera msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
                    if (msg.active) {
                        UluruMissileController.start(msg.entityId);
                    } else {
                        UluruMissileController.stop(false);
                    }
                });
    }
}
