package com.rzy.dealt_force_skills.workbench;

import com.tacz.guns.api.GunProperties;
import com.tacz.guns.api.GunProperty;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.resource.modifier.AttachmentCacheProperty;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;
import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.rzy.dealt_force_skills.shop.SpecialOperationsBaseManager;
import java.util.*;

/** Loaded only behind the optional TaCZ presence check; uses its public API. */
public final class WorkbenchTacz {
    private WorkbenchTacz() { }
    public static boolean isGun(ItemStack stack){return IGun.getIGunOrNull(stack)!=null;}
    private static boolean compatible(ItemStack gunStack,ItemStack attachment) {
        var gun=IGun.getIGunOrNull(gunStack);var part=IAttachment.getIAttachmentOrNull(attachment);
        return gun!=null&&part!=null&&!gun.hasAttachmentLock(gunStack)&&part.getType(attachment)!=AttachmentType.NONE
                &&gun.allowAttachmentType(gunStack,part.getType(attachment))&&gun.allowAttachment(gunStack,attachment);
    }
    public static List<WorkbenchService.AttachmentChoice> find(ServerPlayer player,ItemStack gun) {
        if(!isGun(gun))return List.of();
        List<WorkbenchService.AttachmentChoice> out=new ArrayList<>();
        for(int i=0;i<player.getInventory().getContainerSize();i++)add(out,gun,player.getInventory().getItem(i),0,i,0);
        for(var entry:WorkbenchPlatform.warehouse(player))add(out,gun,entry.stack(),1,entry.slot(),0);
        for(var stack:SpecialOperationsBaseManager.soldStacks(player))if(compatible(gun,stack)) {
            var template=stack.copy();template.setCount(1);
            long cost=SpecialOperationsBaseManager.redemptionCostMatching(player,s->WorkbenchPlatform.same(s,template),1);
            if(cost>=0&&cost<=SpecialOperationsBaseManager.havocCoins(player))add(out,gun,template,2,0,cost);
        }
        return List.copyOf(out);
    }
    private static void add(List<WorkbenchService.AttachmentChoice> out,ItemStack gun,ItemStack stack,int source,int slot,long cost) {
        if(out.size()>=512||stack.isEmpty()||!compatible(gun,stack))return;
        if(out.stream().anyMatch(c->c.source()==source&&WorkbenchPlatform.same(c.stack(),stack)))return;
        var copy=stack.copy();copy.setCount(1);
        out.add(new WorkbenchService.AttachmentChoice(source,slot,copy,cost,IAttachment.getIAttachmentOrNull(copy).getType(copy).name()));
    }
    public static net.minecraft.nbt.ListTag slotViews(ServerPlayer player,ItemStack stack) {
        var result=new net.minecraft.nbt.ListTag();var gun=IGun.getIGunOrNull(stack);
        if(gun==null||gun.hasAttachmentLock(stack))return result;
        for(var type:AttachmentType.values())if(type!=AttachmentType.NONE&&gun.allowAttachmentType(stack,type)) {
            var row=new net.minecraft.nbt.CompoundTag();row.putString("Type",type.name());
            var installed=gun.getAttachment(player.registryAccess(),stack,type);
            if(installed.isEmpty())installed=gun.getBuiltinAttachment(stack,type);
            if(!installed.isEmpty())row.put("Item",com.rzy.dealt_force_skills.department.DepartmentPlatform.save(player,installed));
            result.add(row);
        }
        return result;
    }
    public static String slots(ServerPlayer player,ItemStack stack) {
        var gun=IGun.getIGunOrNull(stack);if(gun==null)return "";
        List<String> slots=new ArrayList<>();
        for(var type:AttachmentType.values())if(type!=AttachmentType.NONE&&gun.allowAttachmentType(stack,type)) {
            var installed=gun.getAttachment(player.registryAccess(),stack,type);
            if(installed.isEmpty())installed=gun.getBuiltinAttachment(stack,type);
            slots.add(type.name()+": "+(installed.isEmpty()?"—":installed.getHoverName().getString()));
        }
        return String.join(" | ",slots)+(gun.hasAttachmentLock(stack)?" [locked]":"");
    }
    public static boolean install(ServerPlayer player,int gunSlot,ItemStack original,WorkbenchService.AttachmentChoice choice) {
        ItemStack offered=choice.stack();if(!compatible(original,offered))return false;
        ItemStack current=ItemStack.EMPTY;
        if(choice.source()==0) {
            if(choice.slot()==gunSlot||choice.slot()<0||choice.slot()>=player.getInventory().getContainerSize())return false;
            current=player.getInventory().getItem(choice.slot());
        } else if(choice.source()==1) {
            for(var entry:WorkbenchPlatform.warehouse(player))if(entry.slot()==choice.slot()){current=entry.stack();break;}
        } else if(choice.source()==2) {
            long cost=SpecialOperationsBaseManager.redemptionCostMatching(player,s->WorkbenchPlatform.same(s,offered),1);
            if(cost!=choice.price()||cost<0||SpecialOperationsBaseManager.maxRedeemableCountMatching(player,s->WorkbenchPlatform.same(s,offered),1)!=1)return false;
            current=offered;
        } else return false;
        if(current.isEmpty()||!WorkbenchPlatform.same(current,offered))return false;
        var gun=IGun.getIGunOrNull(original);var part=IAttachment.getIAttachmentOrNull(offered);var type=part.getType(offered);
        ItemStack old=gun.getAttachment(player.registryAccess(),original,type).copy(),changed=original.copy();
        gun.installAttachment(player.registryAccess(),changed,offered.copy());
        var installed=gun.getAttachment(player.registryAccess(),changed,type);var installedPart=IAttachment.getIAttachmentOrNull(installed);
        if(installedPart==null||!part.getAttachmentId(offered).equals(installedPart.getAttachmentId(installed))
                ||!WorkbenchData.valid(changed,WorkbenchData.allocation(original)))return false;
        ItemStack taken=switch(choice.source()) {
            case 0->player.getInventory().removeItem(choice.slot(),1);
            case 1->WorkbenchPlatform.extract(player,choice.slot(),offered);
            default->SpecialOperationsBaseManager.redeemCheapestMatching(player,s->WorkbenchPlatform.same(s,offered),1);
        };
        if(taken.isEmpty())return false;
        player.getInventory().setItem(gunSlot,changed);
        if(!old.isEmpty())com.rzy.dealt_force_skills.mail.MailboxManager.deliver(player,old);
        return true;
    }
    public static void refresh(ServerPlayer player) {
        var stack=player.getMainHandItem();var gun=IGun.getIGunOrNull(stack);if(gun==null)return;
        com.tacz.guns.resource.modifier.AttachmentPropertyManager.postChangeEvent(player,stack);
    }
    @SuppressWarnings({"rawtypes","unchecked"})
    public static void adjust(Object event) {
        if(!(event instanceof com.tacz.guns.api.event.common.AttachmentPropertyEvent e))return;
        var stack=e.getGunItem();var allocation=WorkbenchData.allocation(stack);var cache=e.getCacheProperty();
        int damage=allocation[WorkbenchStat.GUN_DAMAGE.ordinal()];
        if(damage!=0) {
            var pairs=cache.getCache(GunProperties.DAMAGE);
            if(pairs!=null){var changed=new LinkedList<com.tacz.guns.resource.pojo.data.gun.ExtraDamage.DistanceDamagePair>();
                for(var pair:pairs)changed.add(new com.tacz.guns.resource.pojo.data.gun.ExtraDamage.DistanceDamagePair(pair.getDistance(),(float)Math.max(0,pair.getDamage()+damage*.5)));
                cache.setCache(GunProperties.DAMAGE,changed);}
        }
        for(var stat:WorkbenchStat.values()) {
            int steps=allocation[stat.ordinal()];if(steps==0)continue;
            GunProperty property=property(stat);if(property==null)continue;
            Object value=cache.getCache(property);if(!(value instanceof Number number))continue;
            double changed=Math.max(0,number.doubleValue()+stat.change(number.doubleValue(),steps));
            if(stat==WorkbenchStat.GUN_RPM)cache.setCache(property,(int)Math.max(1,Math.min(6000,Math.round(changed))));
            else cache.setCache(property,(float)(stat==WorkbenchStat.GUN_ARMOR?Math.min(1,changed):changed));
        }
    }
    public static double baseline(ItemStack stack,WorkbenchStat stat) {
        var gun=IGun.getIGunOrNull(stack);if(gun==null)return 0;
        var index=TimelessAPI.getCommonGunIndex(gun.getGunId(stack));if(index.isEmpty())return 0;
        var cache=new AttachmentCacheProperty();cache.eval(stack,index.get().getGunData());
        if(stat==WorkbenchStat.GUN_DAMAGE){var pairs=cache.getCache(GunProperties.DAMAGE);return pairs==null||pairs.isEmpty()?0:pairs.getFirst().getDamage();}
        var property=property(stat);Object value=property==null?null:cache.getCache(property);return value instanceof Number n?n.doubleValue():0;
    }
    private static GunProperty<?> property(WorkbenchStat stat) {
        return switch(stat) {
            case GUN_RPM->GunProperties.ROUNDS_PER_MINUTE;case GUN_RANGE->GunProperties.EFFECTIVE_RANGE;
            case GUN_SPEED->GunProperties.AMMO_SPEED;case GUN_WEIGHT->GunProperties.WEIGHT;
            case GUN_ADS->GunProperties.ADS_TIME;case GUN_HEADSHOT->GunProperties.HEADSHOT_MULTIPLIER;
            case GUN_ARMOR->GunProperties.ARMOR_IGNORE;default->null;
        };
    }
}
