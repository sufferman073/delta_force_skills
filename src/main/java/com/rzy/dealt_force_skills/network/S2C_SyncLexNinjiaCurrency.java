package com.rzy.dealt_force_skills.network;

import net.minecraft.network.FriendlyByteBuf;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record S2C_SyncLexNinjiaCurrency(long lotusBoxes) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<S2C_SyncLexNinjiaCurrency> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "s2c_sync_lex_ninjia_currency"));

    public static final StreamCodec<FriendlyByteBuf, S2C_SyncLexNinjiaCurrency> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    S2C_SyncLexNinjiaCurrency::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(S2C_SyncLexNinjiaCurrency msg, FriendlyByteBuf buf) {
        buf.writeLong(msg.lotusBoxes);
    }

    public static S2C_SyncLexNinjiaCurrency decode(FriendlyByteBuf buf) {
        return new S2C_SyncLexNinjiaCurrency(buf.readLong());
    }

    public static void handle(S2C_SyncLexNinjiaCurrency msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientLexNinjiaHudState.syncLotusBoxes(msg.lotusBoxes));
    }
}
