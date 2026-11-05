package com.rzy.dealt_force_skills.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record S2C_WorkbenchSnapshot(int request,CompoundTag snapshot) implements CustomPacketPayload {
    public static final Type<S2C_WorkbenchSnapshot> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("dealt_force_skills","s2cworkbenchsnapshot"));
    public static final StreamCodec<FriendlyByteBuf,S2C_WorkbenchSnapshot> STREAM_CODEC=StreamCodec.of((buf,msg)->encode(msg,buf),S2C_WorkbenchSnapshot::decode);
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    private static final int MAX_BYTES=524288;
    public static void encode(S2C_WorkbenchSnapshot msg,FriendlyByteBuf buf) {
        buf.writeInt(msg.request);
        var payload=new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        try {
            payload.writeNbt(msg.snapshot);
            if(payload.readableBytes()>MAX_BYTES)throw new io.netty.handler.codec.EncoderException("Workbench snapshot too large");
            buf.writeByteArray(io.netty.buffer.ByteBufUtil.getBytes(payload));
        } finally {payload.release();}
    }
    public static S2C_WorkbenchSnapshot decode(FriendlyByteBuf buf) {
        int request=buf.readInt();var payload=new FriendlyByteBuf(io.netty.buffer.Unpooled.wrappedBuffer(buf.readByteArray(MAX_BYTES)));
        try {
            var tag=payload.readNbt();
            if(tag==null||payload.isReadable())throw new io.netty.handler.codec.DecoderException("Invalid workbench snapshot");
            return new S2C_WorkbenchSnapshot(request,tag);
        } finally {payload.release();}
    }
    public static void handle(S2C_WorkbenchSnapshot msg,IPayloadContext ctx) {
        ctx.enqueueWork(()->com.rzy.dealt_force_skills.client.screen.WorkbenchScreen.accept(msg.request,msg.snapshot));
    }
}
