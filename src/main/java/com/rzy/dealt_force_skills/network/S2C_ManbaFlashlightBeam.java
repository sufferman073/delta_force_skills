package com.rzy.dealt_force_skills.network;

import net.minecraft.network.FriendlyByteBuf;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class S2C_ManbaFlashlightBeam implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<S2C_ManbaFlashlightBeam> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "s2c_manba_flashlight_beam"));

    public static final StreamCodec<FriendlyByteBuf, S2C_ManbaFlashlightBeam> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    S2C_ManbaFlashlightBeam::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final int entityId;
    private final boolean active;
    private final float range;
    private final float halfAngleDegrees;
    private final int ticks;

    public S2C_ManbaFlashlightBeam(int entityId, boolean active, float range, float halfAngleDegrees, int ticks) {
        this.entityId = entityId;
        this.active = active;
        this.range = range;
        this.halfAngleDegrees = halfAngleDegrees;
        this.ticks = ticks;
    }

    public static void encode(S2C_ManbaFlashlightBeam msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.entityId);
        buf.writeBoolean(msg.active);
        buf.writeFloat(msg.range);
        buf.writeFloat(msg.halfAngleDegrees);
        buf.writeVarInt(msg.ticks);
    }

    public static S2C_ManbaFlashlightBeam decode(FriendlyByteBuf buf) {
        return new S2C_ManbaFlashlightBeam(
                buf.readVarInt(),
                buf.readBoolean(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readVarInt()
        );
    }

    public static void handle(S2C_ManbaFlashlightBeam msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ManbaFlashlightBeamRenderer.syncBeam(
                        msg.entityId,
                        msg.active,
                        msg.range,
                        msg.halfAngleDegrees,
                        msg.ticks
                ));
    }
}
