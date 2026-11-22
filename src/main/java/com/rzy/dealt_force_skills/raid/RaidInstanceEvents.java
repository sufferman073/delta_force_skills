package com.rzy.dealt_force_skills.raid;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.command.RaidCommands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DealtForceSkillsMod.MODID)
public final class RaidInstanceEvents {
    private RaidInstanceEvents() { }
    @SubscribeEvent public static void commands(RegisterCommandsEvent event) { RaidCommands.register(event.getDispatcher()); RaidPlayCommands.register(event.getDispatcher()); }
    @SubscribeEvent public static void started(ServerStartedEvent event) { RaidMatches.starting(event.getServer()); RaidInstances.starting(event.getServer()); }
    @SubscribeEvent public static void stopping(ServerStoppingEvent event) { RaidMatches.stopping(event.getServer()); RaidInstances.stopping(event.getServer()); }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) { RaidInstances.tick(event.getServer()); RaidMatches.tick(event.getServer());
            var level = event.getServer().getLevel(RaidInstances.DIMENSION); if (level != null) RaidDeathCrates.tick(level); }
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) { RaidMatches.loggedIn(player); RaidInstances.loggedIn(player); }
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) { RaidMatches.loggedOut(player); RaidInstances.loggedOut(player); }
    }
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && RaidInstances.visiting(player)) RaidInstances.leave(player);
    }
    @SubscribeEvent public static void join(EntityJoinLevelEvent event) { RaidInstances.trackEntity(event.getEntity()); }
    @SubscribeEvent public static void travel(EntityTravelToDimensionEvent event) {
        if (RaidInstances.authorizedTravel(event.getEntity())) return;
        if (event.getEntity().level().dimension().equals(RaidInstances.DIMENSION) || event.getDimension().equals(RaidInstances.DIMENSION))
            event.setCanceled(true);
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void hurt(LivingHurtEvent event) {
        if (RaidInstances.crossInstanceDamage(event.getSource().getEntity(), event.getEntity())
                || RaidInstances.crossInstanceDamage(event.getSource().getDirectEntity(), event.getEntity())) event.setCanceled(true);
        else if (event.getEntity().level().dimension().equals(RaidInstances.DIMENSION)
                && event.getEntity() instanceof com.rzy.dealt_force_skills.entity.AsaraTrooperEntity
                && "npc".equals(com.rzy.dealt_force_skills.raid.ai.RaidAiAffinity.identity(event.getEntity()).getString("kind"))) {
            event.setAmount(event.getAmount() * 4F);
        }
    }
    @SubscribeEvent public static void command(CommandEvent event) {
        if (!(event.getParseResults().getContext().getSource().getEntity() instanceof ServerPlayer player)
                || !RaidInstances.visiting(player)) return;
        String command = event.getParseResults().getReader().getString().trim();
        if (command.startsWith("/")) command = command.substring(1);
        String[] words = command.split("\\s+");
        // CommandEvent also receives parse failures: a visitor can leave after losing OP.
        // Keep the existing dfs command-tree permission requirement unchanged.
        if (words.length == 3 && words[0].equals("dfs") && words[1].equals("raid") && words[2].equals("leave")) {
            event.setCanceled(true);
            boolean restored = RaidInstances.leave(player);
            player.sendSystemMessage(Component.literal(restored ? "已离开地图预览并恢复原位置和模式。" : "恢复失败，已保留返回记录。"));
            return;
        }
        if (words.length >= 2 && words[0].equals("dfs") && words[1].equals("raid")) return;
        event.setCanceled(true);
        player.sendSystemMessage(Component.literal("地图预览中仅可使用 /dfs raid 命令；使用 /dfs raid leave 返回后再执行其他命令。"));
    }
}
