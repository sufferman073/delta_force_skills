package com.rzy.dealt_force_skills.department;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;

/** Credit the player supplying inputs only when the server consumes reagent and changes output. */
public final class DepartmentBrewing {
    private static final String OWNER="dealt_force_skills.department_brewer";
    private static final java.util.Map<AbstractContainerMenu,Interaction> INTERACTIONS=new java.util.WeakHashMap<>();
    private static final java.util.Map<BrewingStandBlockEntity,Batch> BATCHES=new java.util.WeakHashMap<>();
    private record Interaction(BrewingStandBlockEntity stand,java.util.List<ItemStack> before,java.util.UUID owner) { }
    private record Batch(java.util.UUID owner,java.util.List<ItemStack> before) { }
    private DepartmentBrewing() { }
    private static java.util.List<ItemStack> inputs(BrewingStandBlockEntity stand) {
        var result=new java.util.ArrayList<ItemStack>();for(int i=0;i<4;i++)result.add(stand.getItem(i).copy());return result;
    }
    public static void beforeClick(AbstractContainerMenu menu,Player player) {
        if(player instanceof ServerPlayer&&menu instanceof BrewingStandMenu&&menu.getSlot(0).container instanceof BrewingStandBlockEntity stand)
            INTERACTIONS.put(menu,new Interaction(stand,inputs(stand),stand.getPersistentData().hasUUID(OWNER)?stand.getPersistentData().getUUID(OWNER):null));
    }
    public static void afterClick(AbstractContainerMenu menu,Player player) {
        var observation=INTERACTIONS.remove(menu);if(observation==null||!(player instanceof ServerPlayer))return;
        boolean changed=false,placed=false;
        for(int i=0;i<4;i++) {
            ItemStack before=observation.before().get(i),after=observation.stand().getItem(i);
            changed|=!ItemStack.matches(before,after);
            placed|=!after.isEmpty()&&(!ItemStack.isSameItemSameTags(before,after)||after.getCount()>before.getCount());
        }
        if(changed) {
            invalidate(observation.stand());
            if(placed)observation.stand().getPersistentData().putUUID(OWNER,player.getUUID());
            observation.stand().setChanged();
        } else if(observation.owner()!=null)observation.stand().getPersistentData().putUUID(OWNER,observation.owner());
    }
    public static void invalidate(Object container) {
        if(container instanceof BrewingStandBlockEntity stand) {
            stand.getPersistentData().remove(OWNER);stand.setChanged();
        }
    }
    public static void beforeBrew(net.minecraft.world.level.Level level,net.minecraft.core.BlockPos pos) {
        if(!level.isClientSide&&level.getBlockEntity(pos) instanceof BrewingStandBlockEntity stand) {
            var data=stand.getPersistentData();
            BATCHES.put(stand,new Batch(data.hasUUID(OWNER)?data.getUUID(OWNER):null,inputs(stand)));
        }
    }
    public static void afterBrew(net.minecraft.world.level.Level level,net.minecraft.core.BlockPos pos) {
        if(level.isClientSide||!(level.getBlockEntity(pos) instanceof BrewingStandBlockEntity stand))return;
        var batch=BATCHES.remove(stand);if(batch==null||ItemStack.matches(batch.before().get(3),stand.getItem(3)))return;
        invalidate(stand);
        ServerPlayer owner=batch.owner()==null?null:level.getServer().getPlayerList().getPlayer(batch.owner());
        if(owner==null)return;
        for(int i=0;i<3;i++) {
            var result=stand.getItem(i);
            if(!result.isEmpty()&&!ItemStack.matches(batch.before().get(i),result))DepartmentCrafting.crafted(owner,result);
        }
    }
}
