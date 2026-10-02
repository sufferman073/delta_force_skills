package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class S2C_SyncFearStacks implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<S2C_SyncFearStacks> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "s2c_sync_fear_stacks"));

    public static final StreamCodec<FriendlyByteBuf, S2C_SyncFearStacks> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    S2C_SyncFearStacks::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final int stacks;

    public S2C_SyncFearStacks(int stacks) {
        this.stacks = stacks;
    }

    public static void encode(S2C_SyncFearStacks msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.stacks);
    }

    public static S2C_SyncFearStacks decode(FriendlyByteBuf buf) {
        return new S2C_SyncFearStacks(buf.readVarInt());
    }

    public static void handle(S2C_SyncFearStacks msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientFearState.setStacks(msg.stacks));
    }
}
