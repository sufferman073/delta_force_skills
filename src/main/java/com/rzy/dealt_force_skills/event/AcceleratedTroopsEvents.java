package com.rzy.dealt_force_skills.event;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.boss.AcceleratedTroopsSquadManager;
import com.rzy.dealt_force_skills.entity.RespawnBeaconMarkerEntity;
import com.rzy.dealt_force_skills.item.PortableAdsDeviceItem;
import com.rzy.dealt_force_skills.item.RespawnBeaconItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID)
public final class AcceleratedTroopsEvents {
    private static final TagKey<DamageType> TACZ_BULLETS = TagKey.create(
            Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath("tacz", "bullets"));

    private AcceleratedTroopsEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingHurt(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !PortableAdsDeviceItem.isActive(player)) {
            return;
        }
        if (shouldBlock(player, event.getSource())) {
            event.setCanceled(true);
            event.setAmount(0.0F);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        RespawnBeaconMarkerEntity marker = RespawnBeaconItem.marker(player);
        if (marker == null || !(marker.level() instanceof ServerLevel markerLevel)) {
            return;
        }
        event.setCanceled(true);
        player.teleportTo(markerLevel, marker.getX(), marker.getY(), marker.getZ(),
                player.getYRot(), player.getXRot());
        player.setHealth(player.getMaxHealth());
        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(20.0F);
        player.removeAllEffects();
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.isDamageableItem()) {
                stack.setDamageValue(0);
            }
        }
        RespawnBeaconItem.consumeMarker(player, marker);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!event.getEntity().level().isClientSide && event.getEntity() instanceof ServerPlayer player) {
            RespawnBeaconItem.displayMarkerStatus(player);
            PortableAdsDeviceItem.displayActiveStatus(player);
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        AcceleratedTroopsSquadManager.tickServer(event.getServer());
    }

    private static boolean isMelee(DamageSource source) {
        Entity attacker = source.getEntity();
        String messageId = source.getMsgId();
        return attacker instanceof LivingEntity && source.getDirectEntity() == attacker
                && ("mob".equals(messageId) || "player".equals(messageId));
    }

    private static boolean shouldBlock(ServerPlayer player, DamageSource source) {
        return !isMelee(source) && !(source.is(TACZ_BULLETS)
                && PortableAdsDeviceItem.consumeTaczHitAllowance(player, source));
    }
}
