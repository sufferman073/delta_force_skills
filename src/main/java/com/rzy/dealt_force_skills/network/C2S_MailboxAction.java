package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.mail.MailboxManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import java.util.UUID;


public record C2S_MailboxAction(int action, UUID id, int page,String recipient,String title,String body,long coins,int experience,boolean heldAttachment, java.util.List<net.minecraft.world.item.ItemStack> attachments) implements CustomPacketPayload {
    public static final Type<C2S_MailboxAction> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("dealt_force_skills","c2s_mailboxaction"));
    public static final StreamCodec<RegistryFriendlyByteBuf,C2S_MailboxAction> STREAM_CODEC=StreamCodec.of((buf,msg)->encode(msg,buf),C2S_MailboxAction::decode);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public C2S_MailboxAction(int action,UUID id,int page,String recipient,String title,String body,long coins,int experience,boolean heldAttachment) {
        this(action,id,page,recipient,title,body,coins,experience,heldAttachment,java.util.List.of());
    }
    public C2S_MailboxAction(boolean claim,UUID id,int page){this(claim?1:0,id,page,"","","",0,0,false);}
    public static C2S_MailboxAction open(int page) { return new C2S_MailboxAction(false,new UUID(0,0),page); }
    public static C2S_MailboxAction read(UUID id,int page){return new C2S_MailboxAction(2,id,page,"","","",0,0,false);}
    public static void encode(C2S_MailboxAction msg,RegistryFriendlyByteBuf buf) {
        buf.writeByte(msg.action);buf.writeUUID(msg.id);buf.writeVarInt(msg.page);
        buf.writeUtf(msg.recipient,32);buf.writeUtf(msg.title,128);buf.writeUtf(msg.body,4096);
        buf.writeLong(msg.coins);buf.writeVarInt(msg.experience);buf.writeBoolean(msg.heldAttachment);
        buf.writeVarInt(msg.attachments.size());
        for(var stack:msg.attachments)net.minecraft.world.item.ItemStack.OPTIONAL_STREAM_CODEC.encode(buf,stack);
    }
    public static C2S_MailboxAction decode(RegistryFriendlyByteBuf buf) {
        if(buf.readableBytes()>262144)throw new io.netty.handler.codec.DecoderException("Mail action payload exceeds 256 KiB");
        int action=buf.readUnsignedByte();UUID id=buf.readUUID();int page=buf.readVarInt();
        String recipient=buf.readUtf(32),title=buf.readUtf(128),body=buf.readUtf(4096);
        long coins=buf.readLong();int experience=buf.readVarInt();boolean held=buf.readBoolean();
        int count=buf.readVarInt();
        if(count<0||count>MailboxManager.MAX_ATTACHMENTS)throw new io.netty.handler.codec.DecoderException("Invalid mail attachment count");
        var items=new java.util.ArrayList<net.minecraft.world.item.ItemStack>();
        for(int i=0;i<count;i++)items.add(net.minecraft.world.item.ItemStack.OPTIONAL_STREAM_CODEC.decode(buf));
        return new C2S_MailboxAction(action,id,page,recipient,title,body,coins,experience,held,java.util.List.copyOf(items));
    }
    public static void handle(C2S_MailboxAction msg,IPayloadContext ctx) {
        ctx.enqueueWork(()->{
            var player=ctx.player() instanceof net.minecraft.server.level.ServerPlayer server ? server : null;
            if(player==null || !player.isAlive() || player.isSpectator() || !MailboxManager.allowRequest(player)) return;
            if(msg.action<0||msg.action>3)return;
            if(msg.action==2)MailboxManager.read(player,msg.id);
            if(msg.action==3 && !com.rzy.dealt_force_skills.mail.ServerMailService.send(player,msg.recipient,msg.title,msg.body,msg.coins,msg.experience,msg.heldAttachment?java.util.List.of(player.getMainHandItem().copy()):msg.attachments))
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.dealt_force_skills.mail.send_denied"),true);
            if(msg.action==1 && !MailboxManager.claim(player,msg.id)) player.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable("message.dealt_force_skills.mail.no_space"),true);
            NetworkHandler.sendToPlayer(new S2C_MailboxSnapshot(MailboxManager.page(player,msg.page)),player);
        });
    }
}
