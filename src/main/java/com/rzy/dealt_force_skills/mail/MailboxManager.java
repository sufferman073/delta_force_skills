package com.rzy.dealt_force_skills.mail;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Server-owned mailbox saved alongside the player's inventory; attachments never expire. */
public final class MailboxManager {
    public static final String KEY = "dealt_force_skills.mailbox";
    public static final int PAGE_SIZE = 5;
    public static final int MAX_ATTACHMENTS = 27;
    private MailboxManager() { }

    public record Entry(UUID id, String title, List<ItemStack> items, long coins, int experience, List<String> unlocks,
                        String sender,String body,boolean unread) {
        public Entry(UUID id,String title,List<ItemStack> items,long coins,int experience,List<String> unlocks) {
            this(id,title,items,coins,experience,unlocks,"","",true);
        }
    }
    public record Page(int page, int pages, List<Entry> entries,int total,int unread,boolean administrator) {
        public Page(int page,int pages,List<Entry> entries){this(page,pages,entries,entries.size(),entries.size(),false);}
    }
    public static void notifyChange(ServerPlayer player) {
        com.rzy.dealt_force_skills.network.NetworkHandler.sendToPlayer(
                new com.rzy.dealt_force_skills.network.S2C_MailboxSnapshot(page(player,0)),player);
    }
    public static void read(ServerPlayer player,UUID id) {
        for(var tag:messages(player)) {
            var mail=(CompoundTag)tag;if(mail.hasUUID("Id")&&mail.getUUID("Id").equals(id)){mail.putBoolean("Read",true);return;}
        }
    }
    public static void letter(ServerPlayer player,String key,String sender,String title,String body,List<ItemStack> items,long coins,int experience) {
        if(!sendRewardOnce(player,"servermail/"+key,title,items,coins,experience,List.of()))return;
        // Attach metadata only to this operation's newly appended batch (at most MAX_ATTACHMENTS validated stacks).
        var list=messages(player);var mail=list.getCompound(list.size()-1);
        mail.putString("Sender",sender);mail.putString("Body",body);notifyChange(player);
    }

    private static ListTag messages(Player player) {
        CompoundTag data=player.getPersistentData();
        if(!data.contains(KEY,Tag.TAG_LIST)) data.put(KEY,new ListTag());
        return data.getList(KEY,Tag.TAG_COMPOUND);
    }

    /** Takes ownership of the supplied stack, inserting what fits and mailing the exact remainder. */
    public static void deliver(Player player, ItemStack stack) {
        if (!(player instanceof ServerPlayer server) || stack.isEmpty()) return;
        server.getInventory().add(stack);
        if(!stack.isEmpty()) {
            send(server,"物品栏溢出物资",List.of(stack));
            stack.setCount(0);
        }
        server.getInventory().setChanged();
        server.containerMenu.broadcastChanges();
    }

    /** Copies trusted server-produced attachments; callers retain ownership of their templates. */
    public static void send(ServerPlayer player,String title,List<ItemStack> items) {
        ListTag batch=new ListTag();
        for(ItemStack original:items) {
            if(original.isEmpty()) continue;
            ItemStack remainder=original.copy();
            while(!remainder.isEmpty()) {
                ItemStack part=remainder.split(Math.max(1,Math.min(remainder.getCount(),remainder.getMaxStackSize())));
                batch.add(part.save(new CompoundTag()));
                if(batch.size()==MAX_ATTACHMENTS) { append(player,title,batch); batch=new ListTag(); }
            }
        }
        if(!batch.isEmpty()) append(player,title,batch);
        notifyChange(player);
    }

    private static CompoundTag append(ServerPlayer player,String title,ListTag items) {
        CompoundTag mail=new CompoundTag();
        mail.putUUID("Id",UUID.randomUUID());
        mail.putString("Title",boundedTitle(title));
        mail.put("Items",items);
        messages(player).add(mail);
        player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.dealt_force_skills.mail.received"),true);
        return mail;
    }

    private static String boundedTitle(String title) { return title.substring(0,Math.min(128,title.length())); }

    /** Stable server-only delivery keys prevent retries from issuing a second task reward. */
    public static boolean sendRewardOnce(ServerPlayer player,String deliveryKey,String title,List<ItemStack> items,
                                         long coins,int experience,List<String> unlocks) {
        if (deliveryKey.isBlank() || deliveryKey.length()>160 || coins<0 || experience<0
                || !com.rzy.dealt_force_skills.character.TaskCharacterUnlocks.REWARDED.containsAll(unlocks))
            throw new IllegalArgumentException("Invalid server reward");
        CompoundTag data=player.getPersistentData();
        String receiptsKey=KEY+".receipts";
        if(!data.contains(receiptsKey,Tag.TAG_COMPOUND))data.put(receiptsKey,new CompoundTag());
        CompoundTag receipts=data.getCompound(receiptsKey);
        if(receipts.getBoolean(deliveryKey))return false;
        // Encode everything first. A serialization failure must not consume the delivery key.
        List<ListTag> batches=new ArrayList<>();ListTag batch=new ListTag();
        for(ItemStack template:items) {
            ItemStack stack=template.copy();
            while(!stack.isEmpty()) {
                ItemStack part=stack.split(Math.max(1,Math.min(stack.getCount(),stack.getMaxStackSize())));
                batch.add(part.save(new CompoundTag()));
                if(batch.size()==MAX_ATTACHMENTS){batches.add(batch);batch=new ListTag();}
            }
        }
        if(!batch.isEmpty()||batches.isEmpty())batches.add(batch);
        ListTag rewardUnlocks=new ListTag();
        unlocks.stream().distinct().forEach(id->rewardUnlocks.add(net.minecraft.nbt.StringTag.valueOf(id)));
        for(int i=0;i<batches.size();i++) {
            CompoundTag mail=append(player,title,batches.get(i));
            if(i==0){mail.putLong("Coins",coins);mail.putInt("Experience",experience);mail.put("Unlocks",rewardUnlocks);}
        }
        receipts.putBoolean(deliveryKey,true);
        notifyChange(player);
        return true;
    }

    private static List<String> unlocks(CompoundTag mail) {
        List<String> ids=new ArrayList<>();ListTag list=mail.getList("Unlocks",Tag.TAG_STRING);
        for(int i=0;i<list.size();i++)if(com.rzy.dealt_force_skills.character.TaskCharacterUnlocks.REWARDED.contains(list.getString(i)))ids.add(list.getString(i));
        return List.copyOf(ids);
    }

    /** A small per-player burst budget bounds snapshot serialization from custom clients. */
    public static boolean allowRequest(ServerPlayer player) {
        CompoundTag data=player.getPersistentData();long now=player.serverLevel().getGameTime();
        long last=data.getLong(KEY+".requestTick");
        int count=now==last?data.getInt(KEY+".requestCount"):0;
        if(count>=4)return false;
        data.putLong(KEY+".requestTick",now);data.putInt(KEY+".requestCount",count+1);return true;
    }

    /** Only the sender's own mailbox is searched. Partial claims retain all uninserted items. */
    public static boolean claim(ServerPlayer player,UUID id) {
        ListTag messages=messages(player);
        for(int i=0;i<messages.size();i++) {
            CompoundTag mail=messages.getCompound(i);
            if(!mail.hasUUID("Id") || !mail.getUUID("Id").equals(id)) continue;
            ListTag remaining=new ListTag();
            ListTag attachments=mail.getList("Items",Tag.TAG_COMPOUND);
            long coins=Math.max(0,mail.getLong("Coins"));int experience=Math.max(0,mail.getInt("Experience"));
            List<String> unlocks=unlocks(mail);
            // Remove non-item attachments before callbacks; a repeated/recursive claim cannot grant them again.
            boolean coinsClaimed=coins>0 && mccfk.DeltaForce.java.Eeconomy.EeconomyManager.getInstance()
                    .creditMail(player.getGameProfile().getName(),id,coins);
            if(coinsClaimed)mail.remove("Coins");
            mail.remove("Experience");mail.remove("Unlocks");
            boolean changed=coinsClaimed||experience>0||!unlocks.isEmpty();
            if(experience>0)player.giveExperiencePoints(experience);
            for(String character:unlocks)com.rzy.dealt_force_skills.character.TaskCharacterUnlocks.unlock(player,character);
            if(!unlocks.isEmpty())com.rzy.dealt_force_skills.character.CharacterAvailability.syncToClient(player);
            for(int j=0;j<attachments.size();j++) {
                CompoundTag encoded=attachments.getCompound(j);
                ItemStack stack=ItemStack.of(encoded);
                // Preserve unknown mod items in storage until their registry is available again.
                if(stack.isEmpty()) { remaining.add(encoded.copy()); continue; }
                int before=stack.getCount();
                player.getInventory().add(stack);
                changed |= stack.getCount()!=before;
                if(!stack.isEmpty()) remaining.add(stack.save(new CompoundTag()));
            }
            if(remaining.isEmpty() && mail.getLong("Coins")<=0) {messages.remove(i);changed=true;} else mail.put("Items",remaining);
            player.getInventory().setChanged();player.containerMenu.broadcastChanges();
            notifyChange(player);
            return changed;
        }
        return false;
    }

    public static Page page(ServerPlayer player,int requested) {
        ListTag messages=messages(player);
        int pages=Math.max(1,(messages.size()+PAGE_SIZE-1)/PAGE_SIZE);
        int page=Math.max(0,Math.min(requested,pages-1));
        List<Entry> result=new ArrayList<>();
        for(int i=page*PAGE_SIZE;i<Math.min(messages.size(),(page+1)*PAGE_SIZE);i++) {
            CompoundTag mail=messages.getCompound(i);
            if(!mail.hasUUID("Id")) continue;
            List<ItemStack> items=new ArrayList<>();
            ListTag attachments=mail.getList("Items",Tag.TAG_COMPOUND);
            for(int j=0;j<Math.min(MAX_ATTACHMENTS,attachments.size());j++) items.add(ItemStack.of(attachments.getCompound(j)));
            result.add(new Entry(mail.getUUID("Id"),boundedTitle(mail.getString("Title")),List.copyOf(items),Math.max(0,mail.getLong("Coins")),Math.max(0,mail.getInt("Experience")),unlocks(mail),
                    mail.getString("Sender"),mail.getString("Body"),!mail.getBoolean("Read")));
        }
        return new Page(page,pages,List.copyOf(result),messages.size(),
                (int)messages.stream().filter(tag->!((CompoundTag)tag).getBoolean("Read")).count(),player.hasPermissions(2));
    }

    public static void copy(Player from,Player to) {
        if(from.getPersistentData().contains(KEY,Tag.TAG_LIST)) to.getPersistentData().put(KEY,messages(from).copy());
        if(from.getPersistentData().contains(KEY+".receipts",Tag.TAG_COMPOUND))to.getPersistentData().put(KEY+".receipts",from.getPersistentData().getCompound(KEY+".receipts").copy());
    }
}
