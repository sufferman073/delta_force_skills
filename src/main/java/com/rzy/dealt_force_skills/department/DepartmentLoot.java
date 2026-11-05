package com.rzy.dealt_force_skills.department;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Loot provenance stays on dropped entities, never on items a player can throw again. */
public final class DepartmentLoot {
    private DepartmentLoot() { }
    private static final String RECEIPTS="dealt_force_skills.department_loot";
    private static final String CHEST="dealt_force_skills.department_loot_chest";
    private static final java.util.Map<LivingEntity,java.util.Collection<ItemEntity>> DROPS=new java.util.WeakHashMap<>();

    public static void drops(LivingEntity victim,java.util.Collection<ItemEntity> drops) {
        if(!victim.level().isClientSide&&!(victim instanceof Player))DROPS.put(victim,drops);
    }
    public static void died(LivingEntity victim) {
        var drops=DROPS.remove(victim);
        if(drops==null||victim.isAlive()||!(victim.level() instanceof ServerLevel level))return;
        for(var drop:drops)if(!drop.getItem().isEmpty()&&level.getEntity(drop.getUUID())==drop) {
            var receipts=new CompoundTag();receipts.putInt("mob/"+victim.getUUID(),drop.getItem().getCount());
            drop.getPersistentData().put(RECEIPTS,receipts);
        }
    }
    /** Runs only after the original loot table fills a natural container. */
    public static void containerLoot(Container container,Player opener) {
        if(!(opener instanceof ServerPlayer player)||!player.isAlive()||player.isSpectator()
                ||!(container instanceof net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity block))return;
        boolean any=false;for(int i=0;i<container.getContainerSize();i++)if(!container.getItem(i).isEmpty()){any=true;break;}
        if(!any)return;
        var data=block.getPersistentData();
        if(!data.hasUUID(CHEST)){data.putUUID(CHEST,java.util.UUID.randomUUID());block.setChanged();}
        DepartmentTasks.signal(player,"loot.chest_or_entity",1,"chest/"+data.getUUID(CHEST));
    }
    /** Only successful loader pickup callbacks supply the actual removed count. */
    public static void pickedUp(ServerPlayer player,ItemEntity drop,int count) {
        if(count<=0||drop.level().isClientSide)return;
        var receipts=drop.getPersistentData().getCompound(RECEIPTS);
        int remaining=count;
        for(String token:java.util.List.copyOf(receipts.getAllKeys())) {
            int available=Math.max(0,receipts.getInt(token)),taken=Math.min(available,remaining);
            if(taken<=0)continue;
            remaining-=taken;
            if(taken==available)receipts.remove(token);else receipts.putInt(token,available-taken);
            DepartmentTasks.signal(player,"loot.chest_or_entity",1,token);
            if(remaining==0)break;
        }
        if(receipts.isEmpty())drop.getPersistentData().remove(RECEIPTS);
    }
    /** Mirrors the private vanilla merge's exact count before it mutates either stack. */
    public static void merging(ItemEntity destination,ItemStack destinationStack,ItemEntity origin,ItemStack originStack) {
        if(destination.level().isClientSide)return;
        int moved=Math.min(Math.min(destinationStack.getMaxStackSize(),64)-destinationStack.getCount(),originStack.getCount());
        if(moved<=0)return;
        var from=origin.getPersistentData().getCompound(RECEIPTS);
        var into=destination.getPersistentData().getCompound(RECEIPTS);
        for(String token:java.util.List.copyOf(from.getAllKeys())) {
            int available=Math.max(0,from.getInt(token)),taken=Math.min(available,moved);
            if(taken<=0)continue;
            into.putInt(token,into.getInt(token)+taken);moved-=taken;
            if(taken==available)from.remove(token);else from.putInt(token,available-taken);
            if(moved==0)break;
        }
        if(!into.isEmpty())destination.getPersistentData().put(RECEIPTS,into);
        if(from.isEmpty())origin.getPersistentData().remove(RECEIPTS);
    }
}
