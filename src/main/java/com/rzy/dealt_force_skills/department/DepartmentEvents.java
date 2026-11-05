package com.rzy.dealt_force_skills.department;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=DealtForceSkillsMod.MODID)
public final class DepartmentEvents {
    private DepartmentEvents() { }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void projectile(net.minecraftforge.event.entity.EntityJoinLevelEvent event) {
        DepartmentGunCombat.spawned(event.getEntity());
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void gunEvent(net.minecraftforge.eventbus.api.Event event) {
        DepartmentGunReload.event(event);
        if(event.getClass().getName().equals("com.tacz.guns.api.event.common.GunFireEvent"))DepartmentGunCombat.firing(event);
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if(event.getEntity() instanceof ServerPlayer player) {
            DepartmentTaskAdminManager.applyPending(player);
            DepartmentRecon.clear(player);
        }
    }
    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if(event.getEntity() instanceof ServerPlayer player) {
            DepartmentRecon.clear(player);DepartmentTaskConditions.clear(player);
        }
    }
    @SubscribeEvent
    public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if(event.getEntity() instanceof ServerPlayer player)DepartmentRecon.clear(player);
    }

    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void tick(TickEvent.PlayerTickEvent event) {
        if(event.phase!=TickEvent.Phase.END||!(event.player instanceof ServerPlayer player)||player.tickCount%20!=0)return;
        DepartmentTaskAdminManager.applyPending(player);
        DepartmentTasks.tracker().poll(player);
        DepartmentTasks.signal(player,"travel.dimension",1,player.level().dimension().location().toString());
        player.level().getBiome(player.blockPosition()).unwrapKey().ifPresent(key->DepartmentTasks.signal(player,"travel.biome",1,key.location().toString()));
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void crafted(PlayerEvent.ItemCraftedEvent event) {
        if(event.getEntity() instanceof ServerPlayer player&&!event.getCrafting().isEmpty())
            DepartmentCrafting.crafted(player,event.getCrafting());
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void drops(net.minecraftforge.event.entity.living.LivingDropsEvent event) {
        DepartmentLoot.drops(event.getEntity(),event.getDrops());
    }
}
