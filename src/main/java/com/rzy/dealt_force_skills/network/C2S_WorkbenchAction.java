package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.workbench.WorkbenchService;
import com.rzy.dealt_force_skills.workbench.WorkbenchStat;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import java.util.UUID;

/** Requests contain indices and final allocations only; all item data comes from the server. */
public record C2S_WorkbenchAction(int request,int action,int slot,UUID revision,int[] allocation,int choice) implements CustomPacketPayload {
    public static final Type<C2S_WorkbenchAction> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("dealt_force_skills","c2sworkbenchaction"));
    public static final StreamCodec<FriendlyByteBuf,C2S_WorkbenchAction> STREAM_CODEC=StreamCodec.of((buf,msg)->encode(msg,buf),C2S_WorkbenchAction::decode);
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    public static C2S_WorkbenchAction open(int request,int slot) {
        return new C2S_WorkbenchAction(request,0,slot,new UUID(0,0),new int[0],0);
    }
    public static void encode(C2S_WorkbenchAction msg,FriendlyByteBuf buf) {
        buf.writeInt(msg.request);buf.writeByte(msg.action);buf.writeVarInt(msg.slot);
        if(msg.action!=0)buf.writeUUID(msg.revision);
        if(msg.action==1)buf.writeVarIntArray(msg.allocation);
        if(msg.action==3)buf.writeVarInt(msg.choice);
    }
    public static C2S_WorkbenchAction decode(FriendlyByteBuf buf) {
        int request=buf.readInt(),action=buf.readUnsignedByte(),slot=buf.readVarInt();
        if(action>3)throw new io.netty.handler.codec.DecoderException("Invalid workbench action");
        UUID revision=action==0?new UUID(0,0):buf.readUUID();
        int[] values=action==1?buf.readVarIntArray(WorkbenchStat.values().length):new int[0];
        if(action==1&&values.length!=WorkbenchStat.values().length)
            throw new io.netty.handler.codec.DecoderException("Invalid workbench allocation size");
        return new C2S_WorkbenchAction(request,action,slot,revision,values,action==3?buf.readVarInt():0);
    }
    public static void execute(C2S_WorkbenchAction msg,ServerPlayer player) {
        if(player==null||msg.action<0||msg.action>3||!WorkbenchService.allowRequest(player))return;
        var snapshot=switch(msg.action) {
            case 0->WorkbenchService.open(player,msg.slot);
            case 1->WorkbenchService.apply(player,msg.revision,msg.allocation);
            case 2->WorkbenchService.repair(player,msg.revision);
            default->WorkbenchService.attach(player,msg.revision,msg.choice);
        };
        NetworkHandler.sendToPlayer(new S2C_WorkbenchSnapshot(msg.request,snapshot),player);
    }
    public static void handle(C2S_WorkbenchAction msg,IPayloadContext ctx) {
        ctx.enqueueWork(()->execute(msg,ctx.player() instanceof ServerPlayer player?player:null));
    }
}
