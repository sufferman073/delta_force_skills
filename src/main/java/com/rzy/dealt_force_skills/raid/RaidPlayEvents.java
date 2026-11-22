package com.rzy.dealt_force_skills.raid;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.raid.ai.RaidAiAffinity;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.entity.EntityMobGriefingEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DealtForceSkillsMod.MODID)
public final class RaidPlayEvents {
    private RaidPlayEvents() { }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void death(LivingDeathEvent event) {
        if (RaidActors.defersDeath(event.getEntity()))
            event.setCanceled(true); // Fail closed before any disk write can throw.
        try { if (RaidMatches.death(event.getEntity(), event.getSource())) event.setCanceled(true); }
        catch (RuntimeException failure) {
            event.setCanceled(true);
            event.getEntity().setHealth(Math.max(1, event.getEntity().getHealth()));
            RaidMatches.retainDeathFailure(event.getEntity(), failure);
        }
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void drops(LivingDropsEvent event) {
        if (RaidAiAffinity.identity(event.getEntity()).getString("kind").equals("operative")
                || RaidActors.defersDeath(event.getEntity())) {
            event.getDrops().clear(); event.setCanceled(true);
        }
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void pickup(EntityItemPickupEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && (RaidMatches.assetsBlocked(p)
                || RaidInstances.crossInstanceDamage(event.getItem(), p))) event.setCanceled(true);
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void block(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (RaidMatches.assetsBlocked(player) || RaidMatches.belongs(player) && (!RaidMatches.participating(player)
                || !RaidAccess.canTeleport(player, player.serverLevel(), event.getPos().getX() + .5, event.getPos().getY() + .5, event.getPos().getZ() + .5))) {
            event.setCanceled(true); return;
        }
        if (RaidMatches.participating(player) && (RaidMatches.interact(player, event.getPos())
                || event.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND && RaidCables.use(player,event.getPos()))) {
            event.setCanceled(true); event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void entity(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (RaidMatches.assetsBlocked(player) || RaidInstances.crossInstanceDamage(player, event.getTarget())) { event.setCanceled(true); return; }
        if (event.getTarget() instanceof net.minecraft.world.entity.LivingEntity target && RaidActors.help(player, target)) {
            event.setCanceled(true); event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void breakBlock(BlockEvent.BreakEvent event) {
        if (event.getPlayer().level().dimension().equals(RaidInstances.DIMENSION)) event.setCanceled(true);
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void grief(EntityMobGriefingEvent event) {
        if (event.getEntity().level().dimension().equals(RaidInstances.DIMENSION)) event.setResult(Event.Result.DENY);
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void explosion(ExplosionEvent.Detonate event) {
        if (!event.getLevel().dimension().equals(RaidInstances.DIMENSION)) return;
        event.getAffectedBlocks().clear();
        if (event.getLevel() instanceof net.minecraft.server.level.ServerLevel level) {
            var instance = RaidInstances.instanceAt(level, event.getExplosion().getPosition());
            event.getAffectedEntities().removeIf(e -> instance == null || !instance.contains(e.position(), 0));
        }
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void command(CommandEvent event) {
        if (!(event.getParseResults().getContext().getSource().getEntity() instanceof ServerPlayer player)
                || !RaidMatches.belongs(player) && !RaidMatches.assetsBlocked(player)
                || RaidMatches.belongs(player) && RaidMatches.commandsAvailable(player)) return;
        String command = event.getParseResults().getReader().getString().trim();
        if (command.startsWith("/")) command = command.substring(1);
        String root = command.split("\\s+", 2)[0];
        if (root.equals("dealtcommandavailable") || root.equals("raid") || root.equals("msg") || root.equals("tell") || root.equals("w")) return;
        event.setCanceled(true);
        player.sendSystemMessage(Component.literal("绝密行动期间不能使用离场或其他局外指令。副本菜单：/raid"));
    }
}
