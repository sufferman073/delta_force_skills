package com.rzy.dealt_force_skills.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record S2C_OpenLexNinjiaShop(long lotusBoxes, CompoundTag data) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<S2C_OpenLexNinjiaShop> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "s2c_open_lex_ninjia_shop"));

    public static final StreamCodec<FriendlyByteBuf, S2C_OpenLexNinjiaShop> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    S2C_OpenLexNinjiaShop::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public S2C_OpenLexNinjiaShop {
        lotusBoxes = Math.max(0L, lotusBoxes);
        data = data == null ? new CompoundTag() : data.copy();
    }

    public static void encode(S2C_OpenLexNinjiaShop msg, FriendlyByteBuf buf) {
        buf.writeLong(msg.lotusBoxes);
        buf.writeNbt(msg.data);
    }

    public static S2C_OpenLexNinjiaShop decode(FriendlyByteBuf buf) {
        long lotusBoxes = buf.readLong();
        CompoundTag data = buf.readNbt();
        return new S2C_OpenLexNinjiaShop(lotusBoxes, data);
    }

    public static void handle(S2C_OpenLexNinjiaShop msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> LexNinjiaShopScreen.open(msg.lotusBoxes, msg.data));
    }
}
