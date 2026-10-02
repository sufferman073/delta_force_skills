package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class S2C_OpenCommanderScreen implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<S2C_OpenCommanderScreen> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "s2c_open_commander_screen"));

    public static final StreamCodec<FriendlyByteBuf, S2C_OpenCommanderScreen> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    S2C_OpenCommanderScreen::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final boolean support;

    public S2C_OpenCommanderScreen(boolean support) {
        this.support = support;
    }

    public static void encode(S2C_OpenCommanderScreen msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.support);
    }

    public static S2C_OpenCommanderScreen decode(FriendlyByteBuf buf) {
        return new S2C_OpenCommanderScreen(buf.readBoolean());
    }

    public static void handle(S2C_OpenCommanderScreen msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientPacketHandlers.openCommanderScreen(msg.support));
    }
}
