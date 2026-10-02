package com.rzy.dealt_force_skills.network;

import net.minecraft.network.FriendlyByteBuf;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class S2C_SyncHelmetVision implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<S2C_SyncHelmetVision> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "s2c_sync_helmet_vision"));

    public static final StreamCodec<FriendlyByteBuf, S2C_SyncHelmetVision> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    S2C_SyncHelmetVision::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final int mode;

    public S2C_SyncHelmetVision(int mode) {
        this.mode = mode;
    }

    public static void encode(S2C_SyncHelmetVision msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.mode);
    }

    public static S2C_SyncHelmetVision decode(FriendlyByteBuf buf) {
        return new S2C_SyncHelmetVision(buf.readVarInt());
    }

    public static void handle(S2C_SyncHelmetVision msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> HelmetVisionClient.syncLocalHelmetMode(msg.mode));
    }
}
