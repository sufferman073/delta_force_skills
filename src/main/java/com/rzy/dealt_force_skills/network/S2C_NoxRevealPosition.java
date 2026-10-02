package com.rzy.dealt_force_skills.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class S2C_NoxRevealPosition implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<S2C_NoxRevealPosition> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "s2c_nox_reveal_position"));

    public static final StreamCodec<FriendlyByteBuf, S2C_NoxRevealPosition> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    S2C_NoxRevealPosition::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final int entityId;
    private final Vec3 position;
    private final int ticks;

    public S2C_NoxRevealPosition(int entityId, Vec3 position, int ticks) {
        this.entityId = entityId;
        this.position = position;
        this.ticks = ticks;
    }

    public static void encode(S2C_NoxRevealPosition msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.entityId);
        buf.writeDouble(msg.position.x);
        buf.writeDouble(msg.position.y);
        buf.writeDouble(msg.position.z);
        buf.writeVarInt(msg.ticks);
    }

    public static S2C_NoxRevealPosition decode(FriendlyByteBuf buf) {
        return new S2C_NoxRevealPosition(
                buf.readVarInt(),
                new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()),
                buf.readVarInt()
        );
    }

    public static void handle(S2C_NoxRevealPosition msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientNoxHudState.revealEntity(msg.entityId, msg.position, msg.ticks));
    }
}
