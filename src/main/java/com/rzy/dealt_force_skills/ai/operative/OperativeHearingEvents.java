package com.rzy.dealt_force_skills.ai.operative;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** Actual server actions keep instantaneous throws and shots audible between perception ticks. */
@EventBusSubscriber(modid = "dealt_force_skills")
public final class OperativeHearingEvents {
    private OperativeHearingEvents() { }

    @SubscribeEvent public static void tick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player
                && (player.isSprinting() || player.isUsingItem() || player.swinging)) OperativeHearing.recordAction(player);
    }
    @SubscribeEvent(priority = EventPriority.LOWEST) public static void use(LivingEntityUseItemEvent.Start event) {
        OperativeHearing.recordAction(event.getEntity());
    }
    @SubscribeEvent(priority = EventPriority.LOWEST) public static void useItem(PlayerInteractEvent.RightClickItem event) {
        OperativeHearing.recordAction(event.getEntity());
    }
    @SubscribeEvent(priority = EventPriority.LOWEST) public static void useBlock(PlayerInteractEvent.RightClickBlock event) {
        OperativeHearing.recordAction(event.getEntity());
    }
    @SubscribeEvent(priority = EventPriority.LOWEST) public static void useEntity(PlayerInteractEvent.EntityInteract event) {
        OperativeHearing.recordAction(event.getEntity());
    }
    @SubscribeEvent(priority = EventPriority.LOWEST) public static void useEntityAt(PlayerInteractEvent.EntityInteractSpecific event) {
        OperativeHearing.recordAction(event.getEntity());
    }
    @SubscribeEvent(priority = EventPriority.LOWEST) public static void breakBlock(PlayerInteractEvent.LeftClickBlock event) {
        OperativeHearing.recordAction(event.getEntity());
    }
    @SubscribeEvent(priority = EventPriority.LOWEST) public static void attack(AttackEntityEvent event) {
        OperativeHearing.recordAction(event.getEntity());
    }
    @SubscribeEvent public static void jump(LivingEvent.LivingJumpEvent event) {
        OperativeHearing.recordAction(event.getEntity());
    }
    @SubscribeEvent(priority = EventPriority.LOWEST) public static void toss(ItemTossEvent event) {
        OperativeHearing.recordAction(event.getPlayer());
    }
    @SubscribeEvent(priority = EventPriority.LOWEST) public static void projectile(EntityJoinLevelEvent event) {
        // TaCZ's kinetic bullet is a Projectile too; an actual shot is captured even without a hit.
        if (!event.getLevel().isClientSide && event.getEntity() instanceof Projectile projectile
                && projectile.getOwner() instanceof LivingEntity shooter) OperativeHearing.recordAction(shooter);
    }
}
