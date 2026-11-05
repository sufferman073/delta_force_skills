package com.rzy.dealt_force_skills.ai.operative;

import com.rzy.dealt_force_skills.entity.OperativeEntity;
import com.tacz.guns.api.item.IGun;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;

/** Gun item interaction can consume the click before Mob.mobInteract is reached. */
@net.neoforged.fml.common.EventBusSubscriber(modid="dealt_force_skills")
public final class OperativeEquipmentEvents {
    private OperativeEquipmentEvents() { }
    @SubscribeEvent(priority=EventPriority.LOWEST, receiveCanceled=true)
    public static void interact(PlayerInteractEvent.EntityInteract event) {
        if (event.isCanceled() && !event.getCancellationResult().consumesAction()) return;
        var result = equip(event, event.getTarget());
        if (result != net.minecraft.world.InteractionResult.PASS) {
            event.setCancellationResult(result); event.setCanceled(true);
        }
    }
    @SubscribeEvent(priority=EventPriority.LOWEST, receiveCanceled=true)
    public static void interactAt(PlayerInteractEvent.EntityInteractSpecific event) {
        if (event.isCanceled() && !event.getCancellationResult().consumesAction()) return;
        var result = equip(event, event.getTarget());
        if (result != net.minecraft.world.InteractionResult.PASS) {
            event.setCancellationResult(result); event.setCanceled(true);
        }
    }
    private static net.minecraft.world.InteractionResult equip(PlayerInteractEvent event, net.minecraft.world.entity.Entity target) {
        if (!(target instanceof OperativeEntity bot) || IGun.getIGunOrNull(event.getItemStack()) == null
                || !event.getEntity().getUUID().equals(bot.getOwnerUUID())) return net.minecraft.world.InteractionResult.PASS;
        return OperativeEquipment.interact(bot, event.getEntity(), event.getHand());
    }
}
