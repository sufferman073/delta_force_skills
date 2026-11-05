package com.rzy.dealt_force_skills.department;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;

/** Server-resolved display data. Counts travel separately from the single-item preview stack. */
public final class TaskDisplay {
    public record Item(ItemStack stack, int count) {
        public Item { stack=stack.copyWithCount(1); }
    }
    public record Rewards(List<Item> items,long coins,int experience,List<String> unlocks) {
        public static final Rewards EMPTY=new Rewards(List.of(),0,0,List.of());
    }
    private TaskDisplay() { }
    public static List<Item> objective(DepartmentCatalog.Objective goal) {
        List<Item> result=new ArrayList<>();
        if(goal.group())for(var child:goal.children())result.addAll(objective(child));
        else if(List.of("inventory","craft","consume").contains(goal.kind())) {
            var stack=DepartmentRewards.create(goal.key(),1);
            if(!stack.isEmpty())result.add(new Item(stack,(int)Math.min(65536,Math.ceil(goal.target()))));
        }
        return List.copyOf(result);
    }
    public static Rewards reward(DepartmentCatalog.Reward reward) {
        List<Item> items=new ArrayList<>();
        for(var part:reward.items()) {
            var stack=DepartmentRewards.create(part.item(),1);
            if(!stack.isEmpty())items.add(new Item(stack,part.count()));
        }
        return new Rewards(List.copyOf(items),reward.coins(),reward.experience(),reward.unlocks());
    }
    public static Rewards offer(CollectorOffer offer) {
        return new Rewards(offer.rewards().stream().map(p->new Item(p.template(),p.count())).toList(),offer.coins(),offer.experience(),List.of());
    }
    private static int count(RegistryFriendlyByteBuf buf,int max) {
        int n=buf.readVarInt();
        if(n<0||n>max)throw new io.netty.handler.codec.DecoderException("Invalid task display size");
        return n;
    }
    public static void writeItems(RegistryFriendlyByteBuf buf,List<Item> items) {
        buf.writeVarInt(items.size());
        for(var item:items){ItemStack.OPTIONAL_STREAM_CODEC.encode(buf,item.stack());buf.writeVarInt(item.count());}
    }
    public static List<Item> readItems(RegistryFriendlyByteBuf buf) {
        int size=count(buf,128);List<Item> items=new ArrayList<>();
        for(int i=0;i<size;i++) {
            var stack=ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);int amount=count(buf,65536);
            if(stack.isEmpty()||amount==0)throw new io.netty.handler.codec.DecoderException("Invalid task display item");
            items.add(new Item(stack,amount));
        }
        return List.copyOf(items);
    }
    public static void writeRewards(RegistryFriendlyByteBuf buf,Rewards rewards) {
        writeItems(buf,rewards.items());buf.writeLong(rewards.coins());buf.writeVarInt(rewards.experience());
        buf.writeVarInt(rewards.unlocks().size());for(String id:rewards.unlocks())buf.writeUtf(id,160);
    }
    public static Rewards readRewards(RegistryFriendlyByteBuf buf) {
        var items=readItems(buf);long coins=Math.max(0,buf.readLong());int experience=count(buf,Integer.MAX_VALUE);
        int size=count(buf,32);List<String> unlocks=new ArrayList<>();for(int i=0;i<size;i++)unlocks.add(buf.readUtf(160));
        return new Rewards(items,coins,experience,List.copyOf(unlocks));
    }
}
