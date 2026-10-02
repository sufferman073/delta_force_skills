package com.rzy.dealt_force_skills.shop;

import com.rzy.dealt_force_skills.character.ghroth.GhrothStateManager;
import com.rzy.dealt_force_skills.advancement.DfsAchievements;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.S2C_OpenGhrothArmory;
import com.rzy.dealt_force_skills.registry.ModSounds;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.List;

public final class GhrothArmoryManager {
    private GhrothArmoryManager() {
    }

    public static boolean shouldOpen(ServerPlayer player) {
        return GhrothStateManager.isGhroth(player) && HaffCoinManager.canUseShop(player);
    }

    public static void open(ServerPlayer player) {
        if (!shouldOpen(player)) {
            return;
        }
        List<GhrothArmoryCatalog.Entry> entries = GhrothArmoryCatalog.entries();
        NetworkHandler.sendToPlayer(new S2C_OpenGhrothArmory(HaffCoinManager.get(player), entries), player);
    }

    public static void buy(ServerPlayer player, String entryId) {
        if (!shouldOpen(player)) {
            HaffCoinManager.sync(player);
            return;
        }
        GhrothArmoryCatalog.Entry entry = GhrothArmoryCatalog.find(entryId).orElse(null);
        if (entry == null) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.shop.invalid_item"), true);
            HaffCoinManager.sync(player);
            return;
        }
        if (!HaffCoinManager.trySpend(player, entry.price())) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.shop.not_enough_coins"), true);
            return;
        }

        ItemStack purchased = GhrothArmoryCatalog.buildPurchasedStack(entry.id());
        if (purchased.isEmpty()) {
            HaffCoinManager.grant(player, entry.price());
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.shop.invalid_item"), true);
            return;
        }
        ItemStack purchasedForMessage = purchased.copy();
        boolean inserted = player.getInventory().add(purchased);
        if (!inserted && !purchased.isEmpty()) {
            player.drop(purchased, false);
        }
        player.displayClientMessage(Component.translatable("message.dealt_force_skills.shop.purchased",
                purchasedForMessage.getHoverName(), entry.price()), true);
        playPurchaseSound(player);
        DfsAchievements.onShopPurchase(player, "ghroth", entry.id());
        HaffCoinManager.sync(player);
    }

    private static void playPurchaseSound(ServerPlayer player) {
        player.level().playSound(null, player.blockPosition(), randomPurchaseSound(player).get(),
                SoundSource.PLAYERS, 0.9F, 1.0F);
    }

    private static DeferredHolder<SoundEvent, SoundEvent> randomPurchaseSound(ServerPlayer player) {
        return switch (player.getRandom().nextInt(7)) {
            case 0 -> ModSounds.GHROTH_PURCHASE_SUCCESS_1;
            case 1 -> ModSounds.GHROTH_PURCHASE_SUCCESS_2;
            case 2 -> ModSounds.GHROTH_PURCHASE_SUCCESS_3;
            case 3 -> ModSounds.GHROTH_PURCHASE_SUCCESS_4;
            case 4 -> ModSounds.GHROTH_PURCHASE_SUCCESS_5;
            case 5 -> ModSounds.GHROTH_PURCHASE_SUCCESS_6;
            default -> ModSounds.GHROTH_PURCHASE_SUCCESS_7;
        };
    }
}
