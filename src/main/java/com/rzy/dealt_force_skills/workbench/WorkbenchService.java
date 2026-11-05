package com.rzy.dealt_force_skills.workbench;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import java.util.*;

/** Every transaction runs on the server thread against the owner's exact, unmodified stack. */
public final class WorkbenchService {
    private WorkbenchService() { }
    public record AttachmentChoice(int source,int slot,ItemStack stack,long price,String type) { }
    private record Session(UUID revision,int slot,ItemStack original,Ingredient repair,List<AttachmentChoice> attachments,long expires) { }
    private static final Map<ServerPlayer,Session> SESSIONS=new WeakHashMap<>();
    private static final Map<ServerPlayer,Long> REQUESTS=new WeakHashMap<>();
    public static boolean allowRequest(ServerPlayer player) {
        long now=System.nanoTime();Long last=REQUESTS.get(player);
        if(last!=null&&now-last<50_000_000L)return false;
        REQUESTS.put(player,now);return true;
    }
    private static boolean usable(ServerPlayer player,int slot) {
        if(!com.rzy.dealt_force_skills.config.DealtBaseConfig.workbenchEnabled())return false;
        if(!player.isAlive()||player.isSpectator()||slot<0||slot>=player.getInventory().getContainerSize())return false;
        var stack=player.getInventory().getItem(slot);
        return stack.getCount()==1&&!WorkbenchData.types(stack).isEmpty()&&(!player.isUsingItem()||player.getUseItem()!=stack);
    }
    public static CompoundTag open(ServerPlayer player,int slot) {
        SESSIONS.remove(player);
        if(!com.rzy.dealt_force_skills.config.DealtBaseConfig.workbenchEnabled())return error("disabled");
        if(!usable(player,slot))return error("invalid_item");
        ItemStack stack=player.getInventory().getItem(slot);
        var options=WorkbenchPlatform.repairIngredients(player,WorkbenchData.clean(stack));
        Ingredient repair=options.isEmpty()?Ingredient.EMPTY:options.get(player.getRandom().nextInt(options.size()));
        var attachments=WorkbenchPlatform.taczLoaded()&&com.rzy.dealt_force_skills.config.DealtBaseConfig.workbenchAttachmentsEnabled()?WorkbenchTacz.find(player,stack):List.<AttachmentChoice>of();
        Session session=new Session(UUID.randomUUID(),slot,stack.copy(),repair,attachments,player.serverLevel().getGameTime()+1200);
        SESSIONS.put(player,session);return snapshot(player,session,"ready");
    }
    private static Session current(ServerPlayer player,UUID revision) {
        Session s=SESSIONS.get(player);
        if(s==null||!s.revision.equals(revision)||player.serverLevel().getGameTime()>s.expires||!usable(player,s.slot)
                ||!ItemStack.matches(s.original,player.getInventory().getItem(s.slot)))return null;
        return s;
    }
    public static CompoundTag apply(ServerPlayer player,UUID revision,int[] allocation) {
        Session s=current(player,revision);
        if(s==null)return error("stale");
        if(!WorkbenchData.valid(s.original,allocation))return snapshot(player,s,"invalid_allocation");
        int[] old=WorkbenchData.allocation(s.original);
        for(var stat:WorkbenchStat.values())if(!com.rzy.dealt_force_skills.config.DealtBaseConfig.workbenchStatEnabled(stat)
                &&allocation[stat.ordinal()]!=old[stat.ordinal()])return snapshot(player,s,"disabled_property");
        ItemStack changed=s.original.copy();int oldMax=changed.getMaxDamage(),oldDamage=changed.getDamageValue();
        WorkbenchData.write(changed,allocation);
        if(oldMax>0)changed.setDamageValue((int)Math.min(Integer.MAX_VALUE,Math.ceil((double)oldDamage*changed.getMaxDamage()/oldMax)));
        player.getInventory().setItem(s.slot,changed);sync(player);return open(player,s.slot);
    }
    public static CompoundTag repair(ServerPlayer player,UUID revision) {
        if(!com.rzy.dealt_force_skills.config.DealtBaseConfig.workbenchRepairEnabled())return error("disabled");
        Session s=current(player,revision);
        if(s==null)return error("stale");
        if(!s.original.isDamaged()||s.repair.isEmpty())return snapshot(player,s,"no_repair");
        int material=-1;
        for(int i=0;i<player.getInventory().getContainerSize();i++)
            if(i!=s.slot&&s.repair.test(player.getInventory().getItem(i))){material=i;break;}
        if(material<0)return snapshot(player,s,"missing_material");
        ItemStack changed=s.original.copy();changed.setDamageValue(0);
        var consumed=player.getInventory().removeItem(material,1);
        if(consumed.isEmpty())return snapshot(player,s,"missing_material");
        var remainder=consumed.getCraftingRemainingItem();
        player.getInventory().setItem(s.slot,changed);
        if(!remainder.isEmpty())com.rzy.dealt_force_skills.mail.MailboxManager.deliver(player,remainder);
        sync(player);return open(player,s.slot);
    }
    public static CompoundTag attach(ServerPlayer player,UUID revision,int choice) {
        if(!com.rzy.dealt_force_skills.config.DealtBaseConfig.workbenchAttachmentsEnabled())return error("disabled");
        Session s=current(player,revision);if(s==null)return error("stale");
        if(!WorkbenchPlatform.taczLoaded())return snapshot(player,s,"invalid_attachment");
        if(choice<0||choice>=s.attachments.size()||!WorkbenchTacz.install(player,s.slot,s.original,s.attachments.get(choice)))return snapshot(player,s,"invalid_attachment");
        sync(player);
        var result=open(player,s.slot);result.putString("Status","attachment_installed");
        result.putString("AttachmentName",bounded(s.attachments.get(choice).stack().getHoverName().getString(),128));
        return result;
    }
    private static void sync(ServerPlayer player) {
        player.getInventory().setChanged();player.inventoryMenu.broadcastChanges();player.containerMenu.broadcastChanges();
        if(WorkbenchPlatform.taczLoaded())WorkbenchTacz.refresh(player);
    }
    private static String bounded(String text,int max){return text.substring(0,Math.min(text.length(),max));}
    private static CompoundTag error(String status){var out=new CompoundTag();out.putString("Status",status);return out;}
    private static CompoundTag snapshot(ServerPlayer player,Session s,String status) {
        CompoundTag out=error(status);out.putUUID("Revision",s.revision);out.putInt("Slot",s.slot);
        out.putString("Name",bounded(s.original.getHoverName().getString(),128));
        out.putString("Item",net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(s.original.getItem()).toString());
        var allocations=WorkbenchData.allocation(s.original);out.putIntArray("Allocation",allocations);
        ListTag stats=new ListTag();
        for(var stat:WorkbenchStat.values())if(WorkbenchData.available(s.original,stat)&&com.rzy.dealt_force_skills.config.DealtBaseConfig.workbenchStatEnabled(stat)) {
            double base=WorkbenchData.baseline(s.original,stat);if(!Double.isFinite(base)||base<0)continue;
            var row=new CompoundTag();row.putInt("Id",stat.ordinal());row.putDouble("Base",base);
            row.putInt("Min",-stat.sacrificeLimit(base));row.putInt("Max",stat.improvementLimit(base));stats.add(row);
        }
        out.put("Stats",stats);
        out.putBoolean("RepairEnabled",com.rzy.dealt_force_skills.config.DealtBaseConfig.workbenchRepairEnabled());
        out.putBoolean("AttachmentsEnabled",com.rzy.dealt_force_skills.config.DealtBaseConfig.workbenchAttachmentsEnabled());
        out.putBoolean("Damaged",s.original.isDamaged());
        out.putString("Repair",bounded(Arrays.stream(s.repair.getItems()).limit(8).map(st->st.getHoverName().getString()).reduce((a,b)->a+" / "+b).orElse(""),512));
        ListTag attachments=new ListTag();
        for(var choice:s.attachments){var row=new CompoundTag();row.putString("Name",bounded(choice.stack.getHoverName().getString(),128));
            row.putInt("Source",choice.source);row.putString("Type",choice.type);row.putLong("Price",choice.price);attachments.add(row);}
        out.put("Attachments",attachments);
        boolean gun=WorkbenchPlatform.taczLoaded()&&WorkbenchTacz.isGun(s.original);
        out.putBoolean("TaczGun",gun);
        if(gun) {
            out.putString("Slots",bounded(WorkbenchTacz.slots(player,s.original),2048));
            out.put("AttachmentSlots",WorkbenchTacz.slotViews(player,s.original));
        }
        return out;
    }
}
