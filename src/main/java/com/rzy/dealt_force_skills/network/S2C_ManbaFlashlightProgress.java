package com.rzy.dealt_force_skills.network;

import net.minecraft.network.FriendlyByteBuf;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class S2C_ManbaFlashlightProgress implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<S2C_ManbaFlashlightProgress> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "s2c_manba_flashlight_progress"));

    public static final StreamCodec<FriendlyByteBuf, S2C_ManbaFlashlightProgress> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    S2C_ManbaFlashlightProgress::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final int entityId;
    private final int progressPercent;
    private final int ticks;

    public S2C_ManbaFlashlightProgress(int entityId, int progressPercent, int ticks) {
        this.entityId = entityId;
        this.progressPercent = progressPercent;
        this.ticks = ticks;
    }

    public static void encode(S2C_ManbaFlashlightProgress msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.entityId);
        buf.writeVarInt(msg.progressPercent);
        buf.writeVarInt(msg.ticks);
    }

    public static S2C_ManbaFlashlightProgress decode(FriendlyByteBuf buf) {
        return new S2C_ManbaFlashlightProgress(buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(S2C_ManbaFlashlightProgress msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientManbaHudState.updateFlashlightProgress(msg.entityId, msg.progressPercent, msg.ticks));
    }
}
