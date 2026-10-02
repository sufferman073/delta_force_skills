package com.rzy.dealt_force_skills.compat;

import com.rzy.dealt_force_skills.team.DealtTeamManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteract;
import net.neoforged.neoforge.event.tick.PlayerTickEvent.Pre;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = "dealt_force_skills")
public final class PlayerReviveCompatEvents {
   private PlayerReviveCompatEvents() {
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public static void onPlayerReviveInteract(EntityInteract event) {
      if (!PlayerReviveCompat.isLoaded() || !(event.getEntity() instanceof ServerPlayer helper)) {
         return;
      }
      if (!(event.getTarget() instanceof ServerPlayer target) || !PlayerReviveCompat.isBleeding(target)) {
         return;
      }
      if (helper == target || DealtTeamManager.canRescue(helper, target)) {
         return;
      }
      event.setCanceled(true);
      PlayerReviveCompat.removePlayerAsHelper(helper);
      helper.displayClientMessage(Component.translatable("message.dealt_force_skills.playerrevive.not_teammate_helper"), true);
      target.displayClientMessage(
         Component.translatable("message.dealt_force_skills.playerrevive.not_teammate_target", helper.getDisplayName()), true
      );
   }

   @SubscribeEvent(priority = EventPriority.LOWEST)
   public static void onPlayerReviveTick(Pre event) {
      if (event.getEntity().level().isClientSide) {
         return;
      }
      if (event.getEntity() instanceof ServerPlayer player) {
         PlayerReviveCompat.finishNativeReviveBonuses(player);
         PlayerReviveCompat.boostReviveProgress(player);
      }
   }

   @SubscribeEvent
   public static void onServerTickTeamWipe(ServerTickEvent.Post event) {
      if (event.getServer() != null) {
         PlayerReviveCompat.tickTeamWipe(event.getServer());
      }
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public static void onPlayerReviveTickBefore(Pre event) {
      if (event.getEntity().level().isClientSide) {
         return;
      }
      if (event.getEntity() instanceof ServerPlayer player) {
         PlayerReviveCompat.trackNativeReviveHelpers(player);
            PlayerReviveCompat.extendRoverBleeding(player);
      }
   }
}
