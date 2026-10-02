package com.rzy.dealt_force_skills.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record S2C_OpenLexNinjiaPresets(boolean configure, CompoundTag data) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<S2C_OpenLexNinjiaPresets> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "s2c_open_lex_ninjia_presets"));

    public static final StreamCodec<FriendlyByteBuf, S2C_OpenLexNinjiaPresets> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    S2C_OpenLexNinjiaPresets::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public S2C_OpenLexNinjiaPresets {
        data = data == null ? new CompoundTag() : data.copy();
    }

    public static void encode(S2C_OpenLexNinjiaPresets msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.configure);
        buf.writeNbt(msg.data);
    }

    public static S2C_OpenLexNinjiaPresets decode(FriendlyByteBuf buf) {
        return new S2C_OpenLexNinjiaPresets(buf.readBoolean(), buf.readNbt());
    }

    public static void handle(S2C_OpenLexNinjiaPresets msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> LexNinjiaPresetScreen.open(msg.configure, msg.data));
    }
}
