package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class S2C_ConfigApplyResult implements CustomPacketPayload {
    public static final Type<S2C_ConfigApplyResult> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "s2c_config_apply_result"));

    public static final StreamCodec<FriendlyByteBuf, S2C_ConfigApplyResult> STREAM_CODEC =
            StreamCodec.of((buf, msg) -> encode(msg, buf), S2C_ConfigApplyResult::decode);

    private final boolean success;
    private final int appliedCount;
    private final String code;

    public S2C_ConfigApplyResult(boolean success, int appliedCount, String code) {
        this.success = success;
        this.appliedCount = appliedCount;
        this.code = code == null ? "" : code;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(S2C_ConfigApplyResult msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.success);
        buf.writeVarInt(msg.appliedCount);
        buf.writeUtf(msg.code, 64);
    }

    public static S2C_ConfigApplyResult decode(FriendlyByteBuf buf) {
        return new S2C_ConfigApplyResult(buf.readBoolean(), buf.readVarInt(), buf.readUtf(64));
    }

    public static void handle(S2C_ConfigApplyResult msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> DealtForceConfigScreen.onApplyResult(msg.success, msg.appliedCount, msg.code));
    }
}
