package com.rzy.dealt_force_skills.network;

import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import java.util.List;
import java.util.UUID;

public record S2C_OpenMaidSelection(UUID token, String current, List<String> available) implements CustomPacketPayload {
    public static final Type<S2C_OpenMaidSelection> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("dealt_force_skills", "s2c_openmaidselection"));
    public static final StreamCodec<FriendlyByteBuf, S2C_OpenMaidSelection> STREAM_CODEC = StreamCodec.of((buf, msg) -> encode(msg, buf), S2C_OpenMaidSelection::decode);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void encode(S2C_OpenMaidSelection msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.token); buf.writeUtf(msg.current, 256);
        buf.writeCollection(msg.available, (b, id) -> b.writeUtf(id, 256));
    }
    public static S2C_OpenMaidSelection decode(FriendlyByteBuf buf) {
        return new S2C_OpenMaidSelection(buf.readUUID(), buf.readUtf(256), buf.readList(b -> b.readUtf(256)));
    }
    public static void handle(S2C_OpenMaidSelection msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.screen.MaidCharacterSelectionScreen", "open",
                        msg.token, msg.current, msg.available));
    }
}
