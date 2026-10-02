package com.rzy.dealt_force_skills.event;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.entity.DeathCrateEntity;
import com.rzy.dealt_force_skills.registry.ModEntities;
import com.rzy.dealt_force_skills.registry.ModGameRules;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.GameRules;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID)
public final class DeathCrateEvents {
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || player.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)
                || !player.level().getGameRules().getBoolean(ModGameRules.DEALT_CHEST_DROPABLE)
                || event.getDrops().isEmpty() || event.getDrops().size() > 54) return;
        DeathCrateEntity crate = new DeathCrateEntity(ModEntities.DEATH_CRATE.get(), player.level());
        crate.setOwnerUuid(player.getUUID());
        for (ItemEntity drop : event.getDrops()) if (!crate.addDrop(drop.getItem())) return;
        crate.setPos(player.getX(), player.getY() + 0.05D, player.getZ());
        if (player.level().addFreshEntity(crate)) event.setCanceled(true);
    }
}
