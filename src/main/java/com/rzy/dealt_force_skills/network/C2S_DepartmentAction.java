package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.department.DepartmentTasks;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record C2S_DepartmentAction(int action,String department,int page,String task) implements CustomPacketPayload {
    public static final Type<C2S_DepartmentAction> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("dealt_force_skills","c2s_departmentaction"));
    public static final StreamCodec<FriendlyByteBuf,C2S_DepartmentAction> STREAM_CODEC=StreamCodec.of((buf,msg)->encode(msg,buf),C2S_DepartmentAction::decode);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static C2S_DepartmentAction open(String department,int page,String task) { return new C2S_DepartmentAction(0,department,page,task); }
    public static void encode(C2S_DepartmentAction msg,FriendlyByteBuf buf) {
        buf.writeByte(msg.action);buf.writeUtf(msg.department,32);buf.writeVarInt(msg.page);buf.writeUtf(msg.task,64);
    }
    public static C2S_DepartmentAction decode(FriendlyByteBuf buf) {
        return new C2S_DepartmentAction(buf.readUnsignedByte(),buf.readUtf(32),buf.readVarInt(),buf.readUtf(64));
    }
    public static void handle(C2S_DepartmentAction msg,IPayloadContext ctx) {
        ctx.enqueueWork(()->{
            var player=ctx.player() instanceof net.minecraft.server.level.ServerPlayer server ? server : null;
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
    }
}
