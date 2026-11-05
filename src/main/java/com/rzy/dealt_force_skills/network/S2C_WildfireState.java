package com.rzy.dealt_force_skills.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record S2C_WildfireState(int entityId,CompoundTag state){
    public static void encode(S2C_WildfireState msg,FriendlyByteBuf buf){buf.writeVarInt(msg.entityId);buf.writeNbt(msg.state);}
    public static S2C_WildfireState decode(FriendlyByteBuf buf){int id=buf.readVarInt();CompoundTag tag=buf.readNbt();return new S2C_WildfireState(id,tag==null?new CompoundTag():tag);}
    public static void handle(S2C_WildfireState msg,Supplier<NetworkEvent.Context> ctx){
        ctx.get().enqueueWork(()->com.rzy.dealt_force_skills.client.WildfireClient.accept(msg.entityId,msg.state));ctx.get().setPacketHandled(true);
    }
}
