package com.rzy.dealt_force_skills.raid;

import com.rzy.dealt_force_skills.compat.PlayerReviveCompat;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;

/** Optional calls are limited to public PlayerRevive APIs and public configuration fields. */
final class RaidReviveBridge {
    record Settings(int bleedTicks, float bleedingHealth, int progress, float progressPerPlayer,
                    double distance, float restoredHealth, boolean pauseWhileHelped) { }
    private RaidReviveBridge() { }
    static Settings settings() {
        return new Settings((int) number("bleeding", "bleedTime", 1200), (float) number("bleeding", "bleedingHealth", 10),
                (int) number("revive", "requiredReviveProgress", 100), (float) number("revive", "progressPerPlayer", 1),
                number("revive", "maxDistance", 3), (float) number("revive", "healthAfter", 2), bool("revive", "haltBleedTime", true));
    }
    static void start(ServerPlayer player, DamageSource source) {
        if (PlayerReviveCompat.isBleeding(player)) return;
        try {
            Class.forName("team.creative.playerrevive.server.PlayerReviveServer")
                    .getMethod("startBleeding", Player.class, DamageSource.class).invoke(null, player, source);
            if (!PlayerReviveCompat.isBleeding(player)) throw new IllegalStateException("PlayerRevive declined raid downed entry");
        } catch (ReflectiveOperationException | LinkageError failure) {
            throw new IllegalStateException("Installed PlayerRevive public API unavailable; raid recovery retained", failure);
        }
    }
    private static Object field(String section, String name) throws ReflectiveOperationException {
        Class<?> api = Class.forName("team.creative.playerrevive.PlayerRevive");
        Object config = api.getField("CONFIG").get(null);
        Object group = config.getClass().getField(section).get(config);
        return group.getClass().getField(name).get(group);
    }
    private static double number(String section, String name, double fallback) {
        if (!PlayerReviveCompat.isLoaded()) return fallback;
        try { Object value = field(section, name); return value instanceof Number number && number.doubleValue() > 0 ? number.doubleValue() : fallback; }
        catch (ReflectiveOperationException | LinkageError ignored) { return fallback; }
    }
    private static boolean bool(String section, String name, boolean fallback) {
        if (!PlayerReviveCompat.isLoaded()) return fallback;
        try { Object value = field(section, name); return value instanceof Boolean bool ? bool : fallback; }
        catch (ReflectiveOperationException | LinkageError ignored) { return fallback; }
    }
}
