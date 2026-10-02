package com.rzy.dealt_force_skills.shop;

import com.rzy.dealt_force_skills.character.lexninjia.LexNinjiaStateManager;
import com.rzy.dealt_force_skills.character.commander.CommanderStateManager;
import com.rzy.dealt_force_skills.character.saeed.SaeedStateManager;
import com.rzy.dealt_force_skills.character.undead.UndeadStateManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.ToLongFunction;
import java.util.function.Predicate;

public final class DealtCurrencyRegistry {
    private static final List<DealtCurrencyProvider> PROVIDERS = new ArrayList<>();

    static {
        registerBuiltin(new SimpleProvider(
                "lex_ninjia_lotus_boxes",
                "currency.dealt_force_skills.lotus_boxes",
                LexNinjiaStateManager::isLexNinjia,
                LexNinjiaCurrencyManager::get,
                LexNinjiaCurrencyManager::grant,
                LexNinjiaCurrencyManager::set
        ));
        registerBuiltin(new SimpleProvider(
                "undead_souls",
                "currency.dealt_force_skills.souls",
                UndeadStateManager::isUndead,
                UndeadSoulManager::get,
                UndeadSoulManager::grant,
                UndeadSoulManager::set
        ));
        registerBuiltin(new SimpleProvider(
                "saeed_tactical_points",
                "currency.dealt_force_skills.tactical_points",
                SaeedStateManager::isSaeed,
                SaeedStateManager::tacticalPoints,
                SaeedStateManager::grantTacticalPoints,
                SaeedStateManager::setTacticalPoints
        ));
        registerBuiltin(new SimpleProvider(
                "commander_tactical_points",
                "currency.dealt_force_skills.commander_tactical_points",
                CommanderStateManager::isCommander,
                CommanderStateManager::tacticalPoints,
                CommanderStateManager::grantTacticalPoints,
                CommanderStateManager::setTacticalPoints
        ));
        registerBuiltin(new SimpleProvider(
                "haff_coins",
                "currency.dealt_force_skills.haff_coins",
                player -> !LexNinjiaStateManager.isLexNinjia(player)
                        && !UndeadStateManager.isUndead(player)
                        && !SaeedStateManager.isSaeed(player),
                HaffCoinManager::get,
                HaffCoinManager::grant,
                HaffCoinManager::set
        ));
        registerBuiltin(new SimpleProvider(
                "havoc_coin",
                "currency.dealt_force_skills.havoc_coin",
                player -> true,
                SpecialOperationsBaseManager::havocCoins,
                SpecialOperationsBaseManager::grantHavocCoins,
                SpecialOperationsBaseManager::setHavocCoins
        ));
    }

    private DealtCurrencyRegistry() {
    }

    public static synchronized void register(DealtCurrencyProvider provider) {
        DealtCurrencyProvider safe = Objects.requireNonNull(provider, "provider");
        ensureUniqueId(safe.id());
        PROVIDERS.add(0, safe);
    }

    public static synchronized Optional<DealtCurrencyProvider> find(ServerPlayer player) {
        return PROVIDERS.stream().filter(provider -> provider.supports(player)).findFirst();
    }

    public static synchronized Optional<DealtCurrencyProvider> findById(String id) {
        return PROVIDERS.stream().filter(provider -> provider.id().equals(id)).findFirst();
    }

    public static synchronized Optional<DealtCurrencyProvider> findActiveSpecial(ServerPlayer player) {
        return PROVIDERS.stream()
                .filter(provider -> !provider.id().equals("haff_coins") && !provider.id().equals("havoc_coin"))
                .filter(provider -> provider.supports(player))
                .findFirst();
    }

    public static synchronized List<DealtCurrencyProvider> providers() {
        return List.copyOf(PROVIDERS);
    }

    private static void registerBuiltin(DealtCurrencyProvider provider) {
        ensureUniqueId(provider.id());
        PROVIDERS.add(provider);
    }

    private static void ensureUniqueId(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Currency provider ID cannot be blank");
        }
        if (PROVIDERS.stream().anyMatch(provider -> provider.id().equals(id))) {
            throw new IllegalArgumentException("Duplicate currency provider ID: " + id);
        }
    }

    private record SimpleProvider(
            String id,
            String nameKey,
            Predicate<ServerPlayer> supports,
            ToLongFunction<ServerPlayer> balance,
            BiFunction<ServerPlayer, Long, Long> grant,
            BiFunction<ServerPlayer, Long, Long> set
    ) implements DealtCurrencyProvider {
        @Override
        public Component displayName() {
            return Component.translatable(nameKey);
        }

        @Override
        public boolean supports(ServerPlayer player) {
            return supports.test(player);
        }

        @Override
        public long balance(ServerPlayer player) {
            return balance.applyAsLong(player);
        }

        @Override
        public long grant(ServerPlayer player, long amount) {
            return grant.apply(player, amount);
        }

        @Override
        public long set(ServerPlayer player, long amount) {
            return set.apply(player, amount);
        }
    }
}
