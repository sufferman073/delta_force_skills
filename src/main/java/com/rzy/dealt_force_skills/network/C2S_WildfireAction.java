package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.character.wildfire.WildfireStateManager;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record C2S_WildfireAction(int action) implements CustomPacketPayload {
    public static final Type<C2S_WildfireAction> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("dealt_force_skills","c2s_wildfireaction"));
    public static final StreamCodec<FriendlyByteBuf,C2S_WildfireAction> STREAM_CODEC=StreamCodec.of((buf,msg)->encode(msg,buf),C2S_WildfireAction::decode);
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    public static void encode(C2S_WildfireAction msg,FriendlyByteBuf buf){buf.writeVarInt(msg.action);}
    public static C2S_WildfireAction decode(FriendlyByteBuf buf){return new C2S_WildfireAction(buf.readVarInt());}
    public static void handle(C2S_WildfireAction msg,IPayloadContext ctx){
        ctx.enqueueWork(()->{var player=ctx.player() instanceof net.minecraft.server.level.ServerPlayer p?p:null;if(player!=null)WildfireStateManager.toolAction(player,msg.action);});
    }
}
