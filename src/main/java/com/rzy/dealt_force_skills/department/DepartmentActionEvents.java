package com.rzy.dealt_force_skills.department;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=DealtForceSkillsMod.MODID)
public final class DepartmentActionEvents {
    private DepartmentActionEvents() { }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void attack(AttackEntityEvent event) {
        if(event.getEntity() instanceof ServerPlayer p)DepartmentActions.action(p,"attack",0);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void useItem(PlayerInteractEvent.RightClickItem event) {
        if(event.getEntity() instanceof ServerPlayer p)DepartmentActions.action(p,"use",6);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void useBlock(PlayerInteractEvent.RightClickBlock event) {
        if(event.getEntity() instanceof ServerPlayer p)DepartmentActions.action(p,"use",6);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void useEntity(PlayerInteractEvent.EntityInteract event) {
        if(event.getEntity() instanceof ServerPlayer p)DepartmentActions.action(p,"use",6);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void useEntityAt(PlayerInteractEvent.EntityInteractSpecific event) {
        if(event.getEntity() instanceof ServerPlayer p)DepartmentActions.action(p,"use",6);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void startBreak(PlayerInteractEvent.LeftClickBlock event) {
        if(event.getEntity() instanceof ServerPlayer p)DepartmentActions.action(p,"break",6);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void place(BlockEvent.EntityPlaceEvent event) {
        if(event.getEntity() instanceof ServerPlayer p)DepartmentActions.action(p,"use",6);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void jump(LivingEvent.LivingJumpEvent event) {
        if(event.getEntity() instanceof ServerPlayer p)DepartmentActions.jumped(p);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void knockback(LivingKnockBackEvent event) {
        if(event.getEntity() instanceof ServerPlayer p)DepartmentActions.externalMotion(p);
    }
}
