package com.rzy.dealt_force_skills.department;

import com.rzy.dealt_force_skills.mail.MailboxManager;
import mccfk.DeltaForce.java.Eeconomy.CurrencyType;
import mccfk.DeltaForce.java.Eeconomy.EeconomyManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Per-player persistent exchange board; locks protect offer IDs and the exact rolled terms. */
public final class CollectorBoard {
    public static final String KEY=DepartmentTracker.KEY+".collector";
    public static final long REFRESH_TICKS=20400, REFRESH_COST=100000;
    public static final int MAX_OFFERS=50;
    private CollectorBoard() { }
    private static long now(ServerPlayer player){return player.server.overworld().getGameTime();}
    private static CompoundTag root(ServerPlayer player) {
        var data=player.getPersistentData();
        if(!data.contains(KEY,Tag.TAG_COMPOUND))data.put(KEY,new CompoundTag());
        return data.getCompound(KEY);
    }
    private static ListTag offers(ServerPlayer player){return root(player).getList("Offers",Tag.TAG_COMPOUND);}
    private static List<DepartmentCatalog.Definition> categories(DepartmentTracker tracker) {
        return tracker.definitions().values().stream().filter(DepartmentCatalog.Definition::repeatable).toList();
    }
    private static boolean limited(DepartmentCatalog.Definition task) {
        return java.util.Set.of("entity","chest","plant").contains(task.goals().get(0).key());
    }
    private static int count(ListTag offers,String category){return (int)offers.stream().map(tag->(CompoundTag)tag).filter(tag->tag.getString("Category").equals(category)).count();}
    private static CompoundTag generate(ServerPlayer player,DepartmentCatalog.Definition task) {
        String kind=task.goals().get(0).key();int variants=CollectorGenerator.variants(kind),start=player.getRandom().nextInt(variants);
        // A missing optional mod removes that variant, never the entire category.
        for(int i=0;i<variants;i++) {
            var offer=CollectorGenerator.generate(player,kind,1+(start+i)%variants);
            if(offer==null)continue;
            var entry=new CompoundTag();entry.putString("Id",UUID.randomUUID().toString());
            entry.putString("Category",task.id());entry.put("Offer",offer.save(player));return entry;
        }
        return null;
    }
    private static void fill(ServerPlayer player,DepartmentCatalog.Definition task,ListTag list,int target) {
        int missing=Math.min(target-count(list,task.id()),MAX_OFFERS-list.size());
        for(int i=0;i<missing;i++) {var next=generate(player,task);if(next==null)break;list.add(next);}
    }
    public static void ensure(ServerPlayer player,DepartmentTracker tracker) {
        var data=root(player);long tick=now(player);
        if(!data.contains("Offers")) {
            ListTag initial=new ListTag();
            // Preserve existing accepted offers when upgrading from the old one-off collector implementation.
            var legacy=player.getPersistentData().getCompound(DepartmentTracker.KEY);
            for(var task:categories(tracker)) {
                var old=legacy.getCompound(task.id());
                if(old.getBoolean("Active") && !old.getBoolean("Completed") && CollectorOffer.load(player,old.getCompound("Offer"))!=null) {
                    var entry=new CompoundTag();entry.putString("Id",UUID.randomUUID().toString());entry.putString("Category",task.id());
                    entry.put("Offer",old.getCompound("Offer").copy());entry.putBoolean("Locked",true);initial.add(entry);
                }
                old.putBoolean("Active",false);
            }
            data.put("Offers",initial);
        }
        long next=data.getLong("NextRefresh");
        if(next==0 || tick>=next || next-tick>REFRESH_TICKS) {
            DepartmentNaturalPools.inBatch(()->{
                ListTag kept=new ListTag();
                for(var tag:offers(player)) {
                    var entry=(CompoundTag)tag;
                    if(entry.getBoolean("Locked") && !entry.getBoolean("Fulfilled"))kept.add(entry.copy());
                }
                Map<DepartmentCatalog.Definition,Integer> targets=new LinkedHashMap<>();
                int total=0;
                for(var task:categories(tracker))if(tracker.eligible(player,task.id())) {
                    int target=Math.max(count(kept,task.id()),limited(task)?2:8);targets.put(task,target);total+=target;
                }
                List<DepartmentCatalog.Definition> grow=new ArrayList<>(targets.keySet());
                int desired=Math.min(MAX_OFFERS,total+player.getRandom().nextInt(Math.max(1,MAX_OFFERS-total+1)));
                while(total<desired && !grow.isEmpty()) {
                    var task=grow.get(player.getRandom().nextInt(grow.size()));int target=targets.get(task);
                    if(target>= (limited(task)?4:12)){grow.remove(task);continue;}
                    targets.put(task,target+1);total++;
                }
                targets.forEach((task,target)->fill(player,task,kept,target));
                data.put("Offers",kept);data.putLong("NextRefresh",tick+REFRESH_TICKS);return null;
            });
        }
    }
    private static CompoundTag find(ServerPlayer player,String id) {
        for(var tag:offers(player))if(((CompoundTag)tag).getString("Id").equals(id))return (CompoundTag)tag;
        return null;
    }
    public static boolean lock(ServerPlayer player,DepartmentTracker tracker,String id) {
        ensure(player,tracker);var entry=find(player,id);
        if(entry==null || entry.getBoolean("Fulfilled"))return false;
        entry.putBoolean("Locked",!entry.getBoolean("Locked"));return true;
    }
    public static boolean refreshCategory(ServerPlayer player,DepartmentTracker tracker,String category) {
        ensure(player,tracker);var task=tracker.definitions().get(category);
        if(task==null || !task.repeatable() || !tracker.eligible(player,category))return false;
        var previous=offers(player);var next=new ListTag();int unlocked=0;
        for(var tag:previous) {
            var entry=(CompoundTag)tag;
            if(!entry.getString("Category").equals(category) || entry.getBoolean("Locked"))next.add(entry.copy());else unlocked++;
        }
        if(unlocked==0)return false;
        var economy=EeconomyManager.getInstance();String name=player.getGameProfile().getName();
        if(economy.getBalance(name,CurrencyType.HAVOC_COIN)<REFRESH_COST)return false;
        int target=Math.min(count(previous,category)+player.getRandom().nextInt(3)-1,limited(task)?4:12);
        target=Math.max(target,Math.max(count(next,category),limited(task)?2:8));final int desired=target;
        DepartmentNaturalPools.inBatch(()->{fill(player,task,next,desired);return null;});
        if(count(next,category)<(limited(task)?2:8))return false;
        economy.subtractBalance(name,CurrencyType.HAVOC_COIN,REFRESH_COST);
        root(player).put("Offers",next);return true;
    }
    public static boolean submit(ServerPlayer player,DepartmentTracker tracker,String id) {
        if(!player.isAlive()||player.isSpectator())return false;
        ensure(player,tracker);var entry=find(player,id);
        if(entry==null||entry.getBoolean("Fulfilled")||!tracker.eligible(player,entry.getString("Category")))return false;
        var offer=CollectorOffer.load(player,entry.getCompound("Offer"));
        if(offer==null || !offer.consume(player))return false;
        entry.putBoolean("Fulfilled",true);
        var task=tracker.definitions().get(entry.getString("Category"));
        MailboxManager.sendRewardOnce(player,"collector/"+id,task.title(),offer.rewards().stream().map(CollectorOffer.Part::stack).toList(),
                offer.coins(),offer.experience(),List.of());
        // Keep the receipt's offer until refresh; duplicate submission cannot withdraw twice.
        entry.putBoolean("Locked",false);return true;
    }
    public static DepartmentTracker.Page page(ServerPlayer player,DepartmentTracker tracker,String department,String selected) {
        ensure(player,tracker);var list=offers(player);List<DepartmentTracker.Row> rows=new ArrayList<>();
        for(var task:categories(tracker)) {
            int index=0;
            for(var tag:list) {
                var entry=(CompoundTag)tag;if(!entry.getString("Category").equals(task.id()))continue;
                rows.add(new DepartmentTracker.Row(entry.getString("Id"),task.title(),task.title()+" · "+(++index),
                        entry.getBoolean("Fulfilled")?"COMPLETED":entry.getBoolean("Locked")?"PINNED":"ACTIVE"));
            }
        }
        var entry=find(player,selected);if(entry==null && !rows.isEmpty())entry=find(player,rows.get(0).id());
        DepartmentTracker.Detail detail=null;
        if(entry!=null) {
            var task=tracker.definitions().get(entry.getString("Category"));var offer=CollectorOffer.load(player,entry.getCompound("Offer"));
            if(task!=null && offer!=null) {
                List<DepartmentTracker.GoalView> goals=new ArrayList<>();
                for(var part:offer.requirements()) {
                    int held=0;for(var stack:player.getInventory().items)if(CollectorOffer.matches(player,stack,part))held+=stack.getCount();
                    goals.add(new DepartmentTracker.GoalView(DepartmentPlatform.itemName(player,part.template()),Math.min(held,part.count()),part.count(),List.of(new TaskDisplay.Item(part.template(),part.count()))));
                }
                var reward=Component.translatable("screen.dealt_force_skills.department.exchange_reward",offer.coins(),offer.experience());
                for(var part:offer.rewards())reward.append("\n").append(DepartmentPlatform.itemName(player,part.template())).append(" ×"+part.count());
                detail=new DepartmentTracker.Detail(entry.getString("Id"),task.title(),task.brief(),reward,List.of(),
                        entry.getBoolean("Fulfilled")?"COMPLETED":"ACTIVE",0,true,List.copyOf(goals),
                        List.of(new DepartmentTracker.Row(task.id(),task.department(),task.title(),"CATEGORY")),
                        entry.getBoolean("Locked"),Math.max(0,root(player).getLong("NextRefresh")-now(player)),TaskDisplay.offer(offer));
            }
        }
        return new DepartmentTracker.Page(department,0,1,List.copyOf(rows),detail);
    }
}
