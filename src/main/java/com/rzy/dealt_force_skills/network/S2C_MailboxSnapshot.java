package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.mail.MailboxManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record S2C_MailboxSnapshot(MailboxManager.Page page) {
    public static void encode(S2C_MailboxSnapshot msg,FriendlyByteBuf buf) {
        buf.writeVarInt(msg.page.total());buf.writeVarInt(msg.page.unread());buf.writeBoolean(msg.page.administrator());
        buf.writeVarInt(msg.page.page());buf.writeVarInt(msg.page.pages());buf.writeVarInt(msg.page.entries().size());
        for(var entry:msg.page.entries()) {
            buf.writeUUID(entry.id());buf.writeUtf(entry.title(),128);buf.writeVarInt(entry.items().size());
            for(var stack:entry.items()) buf.writeItem(stack);
            buf.writeLong(entry.coins());buf.writeVarInt(entry.experience());buf.writeVarInt(entry.unlocks().size());
            for(String id:entry.unlocks())buf.writeUtf(id,160);
            buf.writeUtf(entry.sender(),128);buf.writeUtf(entry.body(),4096);buf.writeBoolean(entry.unread());
        }
    }
    private static int count(FriendlyByteBuf buf,int max) {
        int n=buf.readVarInt();if(n<0||n>max)throw new io.netty.handler.codec.DecoderException("Invalid mailbox count");return n;
    }
    public static S2C_MailboxSnapshot decode(FriendlyByteBuf buf) {
        int total=count(buf,Integer.MAX_VALUE),unread=count(buf,Integer.MAX_VALUE);boolean administrator=buf.readBoolean();
        int page=count(buf,Integer.MAX_VALUE),pages=count(buf,Integer.MAX_VALUE),size=count(buf,MailboxManager.PAGE_SIZE);
        List<MailboxManager.Entry> entries=new ArrayList<>();
        for(int i=0;i<size;i++) {
            var id=buf.readUUID();var title=buf.readUtf(128);int n=count(buf,MailboxManager.MAX_ATTACHMENTS);
            List<ItemStack> items=new ArrayList<>();for(int j=0;j<n;j++)items.add(buf.readItem());
            long coins=Math.max(0,buf.readLong());int experience=count(buf,Integer.MAX_VALUE);int unlockCount=count(buf,32);
            List<String> unlocks=new ArrayList<>();for(int j=0;j<unlockCount;j++)unlocks.add(buf.readUtf(160));
            entries.add(new MailboxManager.Entry(id,title,List.copyOf(items),coins,experience,List.copyOf(unlocks),buf.readUtf(128),buf.readUtf(4096),buf.readBoolean()));
        }
        return new S2C_MailboxSnapshot(new MailboxManager.Page(page,Math.max(1,pages),List.copyOf(entries),total,unread,administrator));
    }
    public static void handle(S2C_MailboxSnapshot msg,Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(()->net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                ()->()->com.rzy.dealt_force_skills.client.screen.MailboxScreen.accept(msg.page)));
        ctx.get().setPacketHandled(true);
    }
}
