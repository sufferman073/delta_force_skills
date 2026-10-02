package com.rzy.dealt_force_skills.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record S2C_OpenSaeedMonitorScreen(CompoundTag data) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<S2C_OpenSaeedMonitorScreen> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "s2c_open_saeed_monitor_screen"));

    public static final StreamCodec<FriendlyByteBuf, S2C_OpenSaeedMonitorScreen> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    S2C_OpenSaeedMonitorScreen::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public S2C_OpenSaeedMonitorScreen {
        data = data == null ? new CompoundTag() : data.copy();
    }

    public static void encode(S2C_OpenSaeedMonitorScreen msg, FriendlyByteBuf buf) {
        buf.writeNbt(msg.data);
    }

    public static S2C_OpenSaeedMonitorScreen decode(FriendlyByteBuf buf) {
        return new S2C_OpenSaeedMonitorScreen(buf.readNbt());
    }

    public static void handle(S2C_OpenSaeedMonitorScreen msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> SaeedMonitorScreen.open(msg.data));
    }
}
