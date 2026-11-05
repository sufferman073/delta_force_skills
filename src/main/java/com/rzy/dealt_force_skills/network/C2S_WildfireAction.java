package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.character.wildfire.WildfireStateManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record C2S_WildfireAction(int action){
    public static void encode(C2S_WildfireAction msg,FriendlyByteBuf buf){buf.writeVarInt(msg.action);}
    public static C2S_WildfireAction decode(FriendlyByteBuf buf){return new C2S_WildfireAction(buf.readVarInt());}
    public static void handle(C2S_WildfireAction msg,Supplier<NetworkEvent.Context> ctx){
        ctx.get().enqueueWork(()->{var player=ctx.get().getSender();if(player!=null)WildfireStateManager.toolAction(player,msg.action);});ctx.get().setPacketHandled(true);
    }
}
