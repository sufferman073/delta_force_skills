package com.rzy.dealt_force_skills.mail;

import com.rzy.dealt_force_skills.department.DepartmentPlatform;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.List;
import java.util.UUID;

/** Server outbox also delivers to offline recipients when they next join. Broadcast includes future joins. */
public final class ServerMailService {
    private static Outbox data(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(Outbox::new,Outbox::load),"dealt_force_skills_server_mail");
    }
    public static boolean send(ServerPlayer sender,String recipient,String title,String body,long coins,int experience,boolean attachment) {
        return send(sender,recipient,title,body,coins,experience,attachment&&!sender.getMainHandItem().isEmpty()?List.of(sender.getMainHandItem().copy()):List.of());
    }
    public static boolean send(ServerPlayer sender,String recipient,String title,String body,long coins,int experience,List<net.minecraft.world.item.ItemStack> attachments) {
        if(!sender.hasPermissions(2)||title.isBlank()||MailText.parse(title).getString().isBlank()||title.length()>128||body.length()>4096||recipient.length()>32
                ||coins<0||coins>1000000000L||experience<0||experience>1000000
                ||!(recipient.equals("*")||recipient.matches("[A-Za-z0-9_]{1,16}")))return false;
        if(attachments==null||attachments.size()>MailboxManager.MAX_ATTACHMENTS)return false;
        ListTag encoded=new ListTag();int encodedSize=0;
        for(var stack:attachments) {
            if(stack.isEmpty()||stack.getCount()<1||stack.getCount()>stack.getMaxStackSize())return false;
            CompoundTag tag=DepartmentPlatform.save(sender,stack.copy());
            encodedSize+=tag.toString().length();
            if(encodedSize>65536)return false;
            encoded.add(tag);
        }
        var box=data(sender.server);var mail=new CompoundTag();
        mail.putString("Id",UUID.randomUUID().toString());mail.putString("Recipient",recipient);
        mail.putString("Sender",sender.getGameProfile().getName());mail.putString("Title",title);mail.putString("Body",body);
        mail.putLong("Coins",coins);mail.putInt("Experience",experience);
        mail.put("Items",encoded);
        box.letters.add(mail);box.setDirty();
        for(var player:sender.server.getPlayerList().getPlayers())deliverPending(player);
        sender.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.dealt_force_skills.mail.sent"),true);return true;
    }
    public static void deliverPending(ServerPlayer player) {
        for(var tag:data(player.server).letters) {
            var mail=(CompoundTag)tag;String target=mail.getString("Recipient");
            if(!target.equals("*")&&!target.equalsIgnoreCase(player.getGameProfile().getName()))continue;
            List<net.minecraft.world.item.ItemStack> items=new java.util.ArrayList<>();
            boolean available=true;
            if(mail.contains("Items",Tag.TAG_LIST)) {
                for(var encoded:mail.getList("Items",Tag.TAG_COMPOUND)) {
                    var stack=DepartmentPlatform.load(player,(CompoundTag)encoded);
                    if(stack.isEmpty()){available=false;break;}
                    items.add(stack);
                }
            } else if(mail.contains("Item")) {
                var stack=DepartmentPlatform.load(player,mail.getCompound("Item"));
                if(stack.isEmpty())available=false;else items.add(stack);
            }
            if(!available)continue;
            MailboxManager.letter(player,mail.getString("Id"),mail.getString("Sender"),mail.getString("Title"),mail.getString("Body"),
                    items,mail.getLong("Coins"),mail.getInt("Experience"));
        }
    }
    private static final class Outbox extends SavedData {
        private ListTag letters=new ListTag();
        private static Outbox load(CompoundTag tag,net.minecraft.core.HolderLookup.Provider registries){var result=new Outbox();result.letters=tag.getList("Letters",Tag.TAG_COMPOUND).copy();return result;}
        @Override public CompoundTag save(CompoundTag tag,net.minecraft.core.HolderLookup.Provider registries){tag.put("Letters",letters.copy());return tag;}
    }
}
