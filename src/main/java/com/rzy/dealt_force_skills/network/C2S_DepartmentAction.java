package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.department.DepartmentTasks;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record C2S_DepartmentAction(int action,String department,int page,String task) {
    public static C2S_DepartmentAction open(String department,int page,String task) { return new C2S_DepartmentAction(0,department,page,task); }
    public static void encode(C2S_DepartmentAction msg,FriendlyByteBuf buf) {
        buf.writeByte(msg.action);buf.writeUtf(msg.department,32);buf.writeVarInt(msg.page);buf.writeUtf(msg.task,64);
    }
    public static C2S_DepartmentAction decode(FriendlyByteBuf buf) {
        return new C2S_DepartmentAction(buf.readUnsignedByte(),buf.readUtf(32),buf.readVarInt(),buf.readUtf(64));
    }
    public static void handle(C2S_DepartmentAction msg,Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(()->{
            var player=ctx.get().getSender();
            if(player==null||!player.isAlive()||player.isSpectator()||msg.action<0||msg.action>5
                    ||!DepartmentTasks.allowRequest(player,msg.action!=0))return;
            var tracker=DepartmentTasks.tracker();
            tracker.poll(player);
            if(msg.action==1&&!tracker.accept(player,msg.task)||msg.action==2&&!tracker.submit(player,msg.task)||msg.action==3&&!tracker.abandon(player,msg.task)
                    ||msg.action==4&&!com.rzy.dealt_force_skills.department.CollectorBoard.lock(player,tracker,msg.task)
                    ||msg.action==5&&!com.rzy.dealt_force_skills.department.CollectorBoard.refreshCategory(player,tracker,msg.task))
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.dealt_force_skills.department.unavailable"),true);
            NetworkHandler.sendToPlayer(new S2C_DepartmentSnapshot(tracker.page(player,msg.department,msg.page,msg.task)),player);
        });
        ctx.get().setPacketHandled(true);
    }
}
