package com.rzy.dealt_force_skills.music;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.registry.ModItems;
import com.rzy.dealt_force_skills.skill.SkillDamageHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID)
public final class FoxMusicEvents {
    private FoxMusicEvents() { }
    @SubscribeEvent public static void tick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) FoxMusicPlayerManager.tick(player, false);
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) FoxMusicPlayerManager.logout(player);
    }
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) FoxMusicPlayerManager.logout(player);
    }
    @SubscribeEvent public static void stop(ServerStoppedEvent event) { FoxMusicPlayerManager.clear(); }
    @SubscribeEvent public static void fox(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide || !(event.getEntity() instanceof Fox fox)) return;
        var data = fox.getPersistentData();
        if (data.getBoolean("dealt_force_skills.fox_music_checked")) return;
        data.putBoolean("dealt_force_skills.fox_music_checked", true);
        if (!event.loadedFromDisk() && fox.getRandom().nextInt(100) == 0) {
            fox.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.FOX_MUSIC_PLAYER.get()));
            fox.setDropChance(EquipmentSlot.MAINHAND, 2.0F);
        }
    }
    public static ServerPlayer owner(net.minecraft.world.damagesource.DamageSource source) {
        Entity entity = source.getEntity() != null ? source.getEntity() : source.getDirectEntity();
        if (entity instanceof ServerPlayer player) return player;
        if (entity instanceof Projectile projectile && projectile.getOwner() instanceof ServerPlayer player) return player;
        if (entity instanceof TamableAnimal pet && pet.getOwner() instanceof ServerPlayer player) return player;
        return SkillDamageHelper.activeSkillPlayer();
    }
    public static float multiplier(int discs, double roll) {
        if (discs <= 0) return 1.0F;
        double critical = roll < Math.min(1.0, discs * 0.01) ? 1.5 + discs * 0.02 : 1.0;
        return (float) ((1.0 + discs * 0.025) * critical);
    }
    @SubscribeEvent(priority = EventPriority.HIGH) public static void damage(LivingIncomingDamageEvent event) {
        ServerPlayer owner = owner(event.getSource());
        if (owner == null || event.getAmount() <= 0 || event.getEntity() == owner) return;
        int count = FoxMusicPlayerManager.discCount(owner);
        if (count > 0) event.setAmount(Math.min(Float.MAX_VALUE, event.getAmount() * multiplier(count, owner.getRandom().nextDouble())));
    }
    @SubscribeEvent(priority = EventPriority.HIGH) public static void heal(LivingHealEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getAmount() > 0) {
            int count = FoxMusicPlayerManager.discCount(player);
            if (count > 0) event.setAmount((float) Math.min(Float.MAX_VALUE, event.getAmount() * (1.0 + count * 0.1)));
        }
    }
}
