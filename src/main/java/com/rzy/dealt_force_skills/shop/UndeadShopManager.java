package com.rzy.dealt_force_skills.shop;

import com.rzy.dealt_force_skills.character.undead.UndeadStateManager;
import com.rzy.dealt_force_skills.advancement.DfsAchievements;
import com.rzy.dealt_force_skills.character.undead.UndeadSupportManager;
import com.rzy.dealt_force_skills.character.undead.UndeadUpgradeManager;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.S2C_OpenUndeadShop;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class UndeadShopManager {
    private UndeadShopManager() {
    }

    public static boolean shouldOpen(ServerPlayer player) {
        return UndeadStateManager.isUndead(player) && !player.isCreative() && !player.isSpectator();
    }

    public static void open(ServerPlayer player) {
        if (shouldOpen(player)) {
            NetworkHandler.sendToPlayer(new S2C_OpenUndeadShop(
                    UndeadSoulManager.get(player),
                    UndeadStateManager.profession(player).ordinal(),
                    UndeadUpgradeManager.snapshot(player)
            ), player);
        }
    }

    public static void handleAction(ServerPlayer player, String entryId, UndeadShopAction action) {
        if (!shouldOpen(player)) {
            return;
        }
        UndeadShopEntry.byId(entryId).ifPresent(entry -> {
            switch (action) {
                case BUY -> buy(player, entry);
                case REFUND -> refund(player, entry);
                case TOGGLE -> toggle(player, entry);
            }
            UndeadUpgradeManager.applyAttributes(player);
            UndeadStateManager.syncToClient(player);
            open(player);
        });
    }

    private static void buy(ServerPlayer player, UndeadShopEntry entry) {
        int current = UndeadUpgradeManager.level(player, entry);
        if (entry.requiresAllTalentAttributesMaxed()
                && !UndeadUpgradeManager.allTalentAttributesMaxed(player)) {
            message(player, "message.dealt_force_skills.undead_shop.locked");
            return;
        }
        if (!entry.isSupport() && !entry.isUnlimited() && current >= entry.maxLevel()) {
            message(player, "message.dealt_force_skills.undead_shop.max_level");
            return;
        }
        if (entry.category() == UndeadShopCategory.TALENT
                && UndeadUpgradeManager.totalTalentPoints(player) >= UndeadUpgradeManager.maxTalentPoints(player)) {
            message(player, "message.dealt_force_skills.undead_shop.talent_limit");
            return;
        }
        long price = entry.priceForLevel(current);
        if (!UndeadSoulManager.trySpend(player, price)) {
            message(player, "message.dealt_force_skills.undead_shop.not_enough_souls");
            return;
        }
        if (entry.isSupport()) {
            if (!UndeadSupportManager.deploy(player, entry)) {
                UndeadSoulManager.add(player, price);
                message(player, "message.dealt_force_skills.undead_shop.deploy_failed");
            } else com.rzy.dealt_force_skills.department.DepartmentTasks.signal(player,"undead.buy.support");
            return;
        }
        UndeadUpgradeManager.setLevel(player, entry, current + 1);
        if (entry.isBracelet()) {
            UndeadUpgradeManager.setEquipped(player, entry, true);
        }
        String questPurchase=switch(entry.category()) {
            case TRAIT -> "undead.buy.character";
            case BRACELET -> "undead.buy.accessory";
            case SERVICE -> "undead.buy.service";
            default -> "";
        };
        if(!questPurchase.isEmpty())com.rzy.dealt_force_skills.department.DepartmentTasks.signal(player,questPurchase);
        if(entry.category()==UndeadShopCategory.TALENT&&current+1>=entry.maxLevel())
            com.rzy.dealt_force_skills.department.DepartmentTasks.signal(player,"undead.talent.maxed");
        if(entry==UndeadShopEntry.THOUSAND_TEMPERING)
            com.rzy.dealt_force_skills.department.DepartmentTasks.signal(player,"undead.buy.thousand_hits");
        DfsAchievements.onShopPurchase(player, "undead", entry.id());
        message(player, "message.dealt_force_skills.undead_shop.purchased");
    }

    private static void refund(ServerPlayer player, UndeadShopEntry entry) {
        int current = UndeadUpgradeManager.level(player, entry);
        if (!entry.refundable() || current <= 0) {
            return;
        }
        UndeadUpgradeManager.setLevel(player, entry, current - 1);
        UndeadSoulManager.add(player, entry.priceForLevel(current - 1));
        message(player, "message.dealt_force_skills.undead_shop.refunded");
    }

    private static void toggle(ServerPlayer player, UndeadShopEntry entry) {
        if (!entry.isBracelet() || !UndeadUpgradeManager.has(player, entry)) {
            return;
        }
        boolean next = !UndeadUpgradeManager.isEquipped(player, entry);
        UndeadUpgradeManager.setEquipped(player, entry, next);
    }

    private static void message(ServerPlayer player, String key) {
        player.displayClientMessage(Component.translatable(key), true);
    }
}
