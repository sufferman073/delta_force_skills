package com.rzy.dealt_force_skills.department;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid=DealtForceSkillsMod.MODID)
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
