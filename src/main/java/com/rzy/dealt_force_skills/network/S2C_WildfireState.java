package com.rzy.dealt_force_skills.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record S2C_WildfireState(int entityId,CompoundTag state) implements CustomPacketPayload {
    public static final Type<S2C_WildfireState> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("dealt_force_skills","s2c_wildfirestate"));
    public static final StreamCodec<FriendlyByteBuf,S2C_WildfireState> STREAM_CODEC=StreamCodec.of((buf,msg)->encode(msg,buf),S2C_WildfireState::decode);
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    public static void encode(S2C_WildfireState msg,FriendlyByteBuf buf){buf.writeVarInt(msg.entityId);buf.writeNbt(msg.state);}
    public static S2C_WildfireState decode(FriendlyByteBuf buf){int id=buf.readVarInt();CompoundTag tag=buf.readNbt();return new S2C_WildfireState(id,tag==null?new CompoundTag():tag);}
    public static void handle(S2C_WildfireState msg,IPayloadContext ctx){
        ctx.enqueueWork(()->com.rzy.dealt_force_skills.client.WildfireClient.accept(msg.entityId,msg.state));
    }
}
