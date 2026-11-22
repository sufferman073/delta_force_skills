package com.rzy.dealt_force_skills.home;

import com.rzy.dealt_force_skills.shop.SpecialOperationsBaseManager;
import net.minecraft.server.level.ServerPlayer;
import java.math.BigInteger;

final class HomeFillBudget {
    private HomeFillBudget() { }

    private static final String CREDIT = "DfsHomeEnergyCoinCredit";
    private static final BigInteger COIN = BigInteger.TEN.pow(18), FULL_TANK = COIN.multiply(BigInteger.valueOf(10000));
    private static long credit(ServerPlayer user) {
        long value = user.getPersistentData().getCompound("PlayerPersisted").getLong(CREDIT);
        return value >= 0 && value < COIN.longValueExact() ? value : 0;
    }
    static long offer(long capacity, ServerPlayer user) {
        if (capacity <= 0) return 0;
        long balance = SpecialOperationsBaseManager.havocCoins(user);
        if (balance >= 10000) return capacity;
        BigInteger funds = COIN.multiply(BigInteger.valueOf(Math.min(10000, balance))).add(BigInteger.valueOf(credit(user)));
        return funds.multiply(BigInteger.valueOf(capacity)).divide(FULL_TANK).longValueExact();
    }
    static boolean bill(long capacity, long offered, long sent, ServerPlayer user) {
        if (sent < 0 || sent > offered) throw new IllegalStateException("Invalid accepted fill: " + sent);
        if (sent == 0) return false;
        long oldCredit = credit(user);
        BigInteger net = FULL_TANK.multiply(BigInteger.valueOf(sent)).divide(BigInteger.valueOf(capacity)).subtract(BigInteger.valueOf(oldCredit));
        long coins = net.signum() <= 0 ? 0 : net.add(COIN).subtract(BigInteger.ONE).divide(COIN).longValueExact();
        if (!SpecialOperationsBaseManager.spendHavocCoins(user, coins)) throw new IllegalStateException("Fill payment balance changed during insertion");
        long remaining = COIN.multiply(BigInteger.valueOf(coins)).subtract(net).longValueExact();
        if (remaining != oldCredit) {
            var persisted = user.getPersistentData().getCompound("PlayerPersisted");
            if (remaining == 0) persisted.remove(CREDIT); else persisted.putLong(CREDIT, remaining);
            user.getPersistentData().put("PlayerPersisted", persisted);
        }
        return true;
    }
}
