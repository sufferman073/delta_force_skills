package com.rzy.dealt_force_skills.ai.operative;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Actual server actions keep instantaneous throws and shots audible between perception ticks. */
@Mod.EventBusSubscriber(modid = "dealt_force_skills")
public final class OperativeHearingEvents {
    private OperativeHearingEvents() { }

    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player
                && (player.isSprinting() || player.isUsingItem() || player.swinging)) OperativeHearing.recordAction(player);
    }
    @SubscribeEvent(priority = EventPriority.LOWEST) public static void use(LivingEntityUseItemEvent.Start event) {
        OperativeHearing.recordAction(event.getEntity());
    }
    @SubscribeEvent(priority = EventPriority.LOWEST) public static void interact(PlayerInteractEvent event) {
        if (event instanceof PlayerInteractEvent.RightClickItem || event instanceof PlayerInteractEvent.RightClickBlock
                || event instanceof PlayerInteractEvent.EntityInteract || event instanceof PlayerInteractEvent.EntityInteractSpecific
                || event instanceof PlayerInteractEvent.LeftClickBlock) OperativeHearing.recordAction(event.getEntity());
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
