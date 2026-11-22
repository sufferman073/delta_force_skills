package com.rzy.dealt_force_skills.ai.operative;

import com.rzy.dealt_force_skills.character.undead.UndeadStateManager;
import com.rzy.dealt_force_skills.entity.OperativeEntity;
import com.rzy.dealt_force_skills.registry.ModEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/** Server perception only: it never casts skills, marks enemies or owns movement controls. */
public final class OperativeHearing {
    public static final int NOISE_TICKS = 40;
    private record Noise(Level level, long until) { }
    private static final Map<Player, Noise> NOISE = new WeakHashMap<>();
    private UUID tracked;
    private Vec3 lastHeard;
    private long investigateUntil;

    public static void recordAction(LivingEntity entity) {
        if (entity instanceof Player player && !player.level().isClientSide && player.isAlive())
            NOISE.put(player, new Noise(player.level(), player.level().getGameTime() + NOISE_TICKS));
    }

    public static boolean concealed(LivingEntity entity) {
        return entity.isInvisible() || entity.hasEffect(ModEffects.NOX_STEALTH)
                || entity instanceof Player player && UndeadStateManager.isUndead(player)
                    && UndeadStateManager.explorerSpaceActive(player);
    }

    public static boolean audible(LivingEntity entity) {
        if (!(entity instanceof Player player)) return true;
        // Action noise wins over crouching and partial invisibility, including Nox's muted audio.
        if (player.isSprinting() || player.isUsingItem() || player.swinging) return true;
        Noise noise = NOISE.get(player);
        if (noise != null) {
            if (noise.level == player.level() && noise.until > player.level().getGameTime()) return true;
            NOISE.remove(player);
        }
        return !player.isShiftKeyDown() && !player.isCrouching() && !concealed(player);
    }

    public static boolean visible(OperativeEntity host, LivingEntity target) {
        return !concealed(target) && host.canSee(target);
    }

    /** Area tools may use an acquired sound contact; this does not grant sight to guns or melee. */
    public static boolean knownContact(OperativeHost host, LivingEntity target) {
        if (target == null || !target.isAlive() || target.isRemoved() || target.level() != host.body().level()
                || !host.isEnemy(target) || !host.withinCombatRange(target.position())) return false;
        if (!concealed(target) && host.canSee(target) || host.skills() != null && host.skills().isRevealed(target)) return true;
        return host.body() instanceof OperativeEntity bot && bot.getTarget() == target
                && host.withinOrders(target.position()) && bot.distanceToSqr(target) <= Math.pow(bot.acquisitionRange(), 2)
                && audible(target);
    }

    public boolean tracks(LivingEntity target) { return target != null && target.getUUID().equals(tracked); }

    public boolean validContact(OperativeEntity host, LivingEntity target) {
        return target != null && target.isAlive() && !target.isRemoved() && target.level() == host.level()
                && (host.isEnemy(target) || tracks(target) && host.isHearingEnemy(target) && audible(target));
    }

    public LivingEntity acquire(OperativeEntity host) {
        double range = host.acquisitionRange();
        LivingEntity target = host.serverLevel().getEntitiesOfClass(LivingEntity.class,
                host.getBoundingBox().inflate(range), e -> e.level() == host.level() && !e.isRemoved()
                        && host.isHearingEnemy(e) && host.distanceToSqr(e) <= range * range
                        && host.withinOrders(e.position()) && host.withinCombatRange(e.position()) && audible(e))
                .stream().min(Comparator.comparingInt((LivingEntity e) -> e instanceof Player ? 0 : 1)
                        .thenComparingDouble(host::distanceToSqr).thenComparingInt(LivingEntity::getId)).orElse(null);
        if (target != null) {
            tracked = target.getUUID();
            lastHeard = target.position();
            investigateUntil = host.level().getGameTime() + 60;
        }
        return target;
    }

    /** A quiet contact leaves a fixed investigation point, never a live position feed. */
    public Vec3 investigation(OperativeEntity host) {
        return lastHeard != null && host.level().getGameTime() < investigateUntil
                && host.withinOrders(lastHeard) ? lastHeard : null;
    }

    public void clear() { tracked = null; lastHeard = null; investigateUntil = 0; }
}
