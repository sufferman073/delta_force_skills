package com.rzy.dealt_force_skills.item;

import com.rzy.dealt_force_skills.config.DealtBossesConfig;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

public final class PortableAdsDeviceItem extends QualityTooltipItem {
    private static final String ACTIVE_UNTIL = "DealtForcePortableAdsUntil";
    private static final String COOLDOWN_UNTIL = "DealtForcePortableAdsCooldownUntil";
    private static final String TACZ_ALLOW_TICK = "DealtForcePortableAdsTaczAllowTick";
    private static final String TACZ_ALLOW_COUNT = "DealtForcePortableAdsTaczAllowCount";
    private static final String TACZ_ALLOW_ATTACKER = "DealtForcePortableAdsTaczAllowAttacker";

    public PortableAdsDeviceItem(Properties properties) {
        super(properties.stacksTo(1), DfsItemQuality.RED,
                "tooltip.dealt_force_skills.portable_ads_device");
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            long now = serverPlayer.serverLevel().getGameTime();
            CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            long cooldownUntil = tag.getLong(COOLDOWN_UNTIL);
            if (cooldownUntil > now) {
                serverPlayer.displayClientMessage(Component.translatable(
                        "message.dealt_force_skills.portable_ads_device.cooldown",
                        remainingSeconds(cooldownUntil - now)), true);
                return InteractionResultHolder.fail(stack);
            }
            serverPlayer.getPersistentData().putLong(ACTIVE_UNTIL, now + activeTicks());
            serverPlayer.removeAllEffects();
            tag.putLong(COOLDOWN_UNTIL, now + cooldownTicks());
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.dealt_force_skills.portable_ads_device.activated"), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    public static boolean isActive(ServerPlayer player) {
        return player != null && player.getPersistentData().getLong(ACTIVE_UNTIL)
                > player.serverLevel().getGameTime();
    }

    public static void allowNextTaczHit(ServerPlayer player, Entity attacker) {
        if (player == null || attacker == null) {
            return;
        }
        long now = player.serverLevel().getGameTime();
        var data = player.getPersistentData();
        boolean sameAttacker = data.hasUUID(TACZ_ALLOW_ATTACKER)
                && data.getUUID(TACZ_ALLOW_ATTACKER).equals(attacker.getUUID());
        if (data.getLong(TACZ_ALLOW_TICK) != now || !sameAttacker) {
            data.putLong(TACZ_ALLOW_TICK, now);
            data.putUUID(TACZ_ALLOW_ATTACKER, attacker.getUUID());
            data.putInt(TACZ_ALLOW_COUNT, 0);
        }
        data.putInt(TACZ_ALLOW_COUNT, Math.min(32, data.getInt(TACZ_ALLOW_COUNT) + 2));
    }

    public static boolean consumeTaczHitAllowance(ServerPlayer player, DamageSource source) {
        long now = player.serverLevel().getGameTime();
        var data = player.getPersistentData();
        Entity attacker = source.getEntity();
        if (attacker == null || data.getLong(TACZ_ALLOW_TICK) != now
                || !data.hasUUID(TACZ_ALLOW_ATTACKER)
                || !data.getUUID(TACZ_ALLOW_ATTACKER).equals(attacker.getUUID())) {
            data.remove(TACZ_ALLOW_COUNT);
            return false;
        }
        int remaining = data.getInt(TACZ_ALLOW_COUNT);
        if (remaining <= 0) {
            return false;
        }
        data.putInt(TACZ_ALLOW_COUNT, remaining - 1);
        return true;
    }

    public static void displayActiveStatus(ServerPlayer player) {
        long remaining = player.getPersistentData().getLong(ACTIVE_UNTIL)
                - player.serverLevel().getGameTime();
        if (remaining > 0L) {
            player.removeAllEffects();
        }
        if (remaining > 0L && player.tickCount % 20 == 0) {
            player.displayClientMessage(Component.translatable(
                    "message.dealt_force_skills.portable_ads_device.remaining",
                    remainingSeconds(remaining)), true);
        }
    }

    private static long remainingSeconds(long ticks) {
        return Math.max(1L, (ticks + 19L) / 20L);
    }

    private static int activeTicks() {
        return Math.max(1, DealtBossesConfig.intValue(
                "accelerated_troops.portable_ads.active_ticks", 15 * 20));
    }

    private static int cooldownTicks() {
        return Math.max(1, DealtBossesConfig.intValue(
                "accelerated_troops.portable_ads.cooldown_ticks", 60 * 20));
    }
}
