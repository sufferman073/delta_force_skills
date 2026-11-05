package com.rzy.dealt_force_skills.shop;

import com.rzy.dealt_force_skills.advancement.DfsAchievements;
import com.rzy.dealt_force_skills.character.chamber.ChamberStateManager;
import com.rzy.dealt_force_skills.character.corps.CorpsStateManager;
import com.rzy.dealt_force_skills.character.ghroth.GhrothStateManager;
import com.rzy.dealt_force_skills.character.lexninjia.LexNinjiaStateManager;
import com.rzy.dealt_force_skills.character.saeed.SaeedStateManager;
import com.rzy.dealt_force_skills.config.DealtShopSetConfig;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.S2C_OpenHaffShop;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class HaffShopManager {
    private HaffShopManager() {
    }

    public static boolean shouldOpenShop(ServerPlayer player) {
        return shouldOpenShop(player, false);
    }

    public static boolean shouldOpenShop(ServerPlayer player, boolean allowSpecialCharacters) {
        return DealtShopAccessManager.isAllowed(DealtShopAccessManager.Service.SHOP, player)
                && (allowSpecialCharacters || (!LexNinjiaStateManager.isLexNinjia(player)
                && !GhrothStateManager.isGhroth(player)
                && !SaeedStateManager.isSaeed(player)))
                && HaffCoinManager.canUseShop(player);
    }

    public static void openShop(ServerPlayer player) {
        openShop(player, false);
    }

    public static void openShop(ServerPlayer player, boolean allowSpecialCharacters) {
        if (!shouldOpenShop(player, allowSpecialCharacters)) {
            return;
        }
        NetworkHandler.sendToPlayer(new S2C_OpenHaffShop(HaffCoinManager.get(player), DfsShopCatalog.entries()), player);
    }

    public static void buy(ServerPlayer player, String entryId) {
        if (!shouldOpenShop(player, SaeedStateManager.isSaeed(player))) {
            HaffCoinManager.sync(player);
            return;
        }
        DfsShopCatalog.Entry entry = DfsShopCatalog.find(entryId).orElse(null);
        if (entry == null) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.shop.invalid_item"), true);
            HaffCoinManager.sync(player);
            return;
        }
        if (!HaffCoinManager.trySpend(player, entry.price())) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.shop.not_enough_coins"), true);
            return;
        }
        if (entry.category() == DfsShopCatalog.Category.LOADOUT) {
            buyLoadout(player, entry);
            return;
        }

        ItemStack purchased = DfsShopCatalog.buildPurchasedStack(entry.id());
        if (purchased.isEmpty()) {
            HaffCoinManager.grant(player, entry.price());
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.shop.invalid_item"), true);
            return;
        }
        ItemStack purchasedForMessage = purchased.copy();
        boolean inserted = player.getInventory().add(purchased);
        if (!inserted && !purchased.isEmpty()) {
            com.rzy.dealt_force_skills.mail.MailboxManager.deliver(player, purchased);
        }
        player.displayClientMessage(Component.translatable("message.dealt_force_skills.shop.purchased",
                purchasedForMessage.getHoverName(), entry.price()), true);
        DfsAchievements.onShopPurchase(player, "haff", entry.id());
        CorpsStateManager.shareShopPurchase(player, List.of(purchasedForMessage));
        HaffCoinManager.sync(player);
    }

    private static void buyLoadout(ServerPlayer player, DfsShopCatalog.Entry entry) {
        DealtShopSetConfig.LoadoutSet loadout = DealtShopSetConfig.findByEntryId(entry.id()).orElse(null);
        if (loadout == null || loadout.items().isEmpty()) {
            HaffCoinManager.grant(player, entry.price());
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.shop.invalid_item"), true);
            HaffCoinManager.sync(player);
            return;
        }
        List<ItemStack> purchasedStacks = new ArrayList<>();
        for (ItemStack configured : loadout.items()) {
            ItemStack stack = configured.copy();
            purchasedStacks.add(stack.copy());
            boolean inserted = player.getInventory().add(stack);
            if (!inserted && !stack.isEmpty()) {
                com.rzy.dealt_force_skills.mail.MailboxManager.deliver(player, stack);
            }
        }
        player.displayClientMessage(Component.translatable("message.dealt_force_skills.shop.loadout_purchased",
                Component.literal(loadout.name()), entry.price()), true);
        DfsAchievements.onShopPurchase(player, "haff", entry.id());
        CorpsStateManager.shareShopPurchase(player, purchasedStacks);
        HaffCoinManager.sync(player);
    }
}
