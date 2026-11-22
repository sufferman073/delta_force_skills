package com.rzy.dealt_force_skills.effect;

import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.skill.SkillDamageHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;

/**
 * Combat logic for the {@link ModEffects#SHAKEHANDS} effect: while an entity holds it, an
 * attack against it deals no damage (the attacker instead takes reflected true damage), and
 * any negative/control/special-negative effect the attack would have applied to the holder is
 * redirected back onto the attacker instead.
 *
 * <p>Interception uses {@link LivingAttackEvent} as the primary path so knockback, hurt sound,
 * red-flash animation and invulnerability timer never apply to the holder. {@link LivingHurtEvent}
 * is a secondary safety net for damage paths that skip the attack stage. TaCZ gun hits are also
 * cancelled on {@code EntityHurtByGunEvent$Pre} via {@link #tryReflectFromAttacker}.</p>
 *
 * <p>Infinite dual-holder loops are prevented by (1) ignoring {@code shakehands_reflect} damage
 * types entirely, and (2) a re-entrancy flag around the reflect {@code hurt} call.</p>
 */
public final class ShakehandsCombatHandler {
    /**
     * Guards against infinite ping-pong when both the holder and the attacker have shakehands
     * (redirecting an effect back onto the attacker would otherwise trigger this handler again
     * for the attacker's own shakehands, and so on). Event callbacks run synchronously on the
     * server thread, so a simple static flag is sufficient.
     */
    private static boolean redirecting;

    /**
     * Guards against infinite recursion when reflected damage is itself dealt to an entity that
     * also holds shakehands. Combined with the {@code shakehands_reflect} damage-type filter.
     */
    private static boolean reflecting;

    private ShakehandsCombatHandler() {
    }

    public static void handleLivingAttack(LivingAttackEvent event) {
        if (event.isCanceled() || reflecting) {
            return;
        }
        float amount = event.getAmount();
        if (amount <= 0.0F) {
            return;
        }
        LivingEntity target = event.getEntity();
        if (!tryReflectIncoming(target, event.getSource(), amount)) {
            return;
        }
        event.setCanceled(true);
    }

    /**
     * Secondary path for damage that reaches hurt-stage without having been cancelled earlier.
     * Does not re-apply if already cancelled, or if the source is our own reflect type.
     */
    public static void handleLivingHurt(LivingHurtEvent event) {
        if (event.isCanceled() || reflecting) {
            return;
        }
        float amount = event.getAmount();
        if (amount <= 0.0F) {
            return;
        }
        LivingEntity target = event.getEntity();
        if (!tryReflectIncoming(target, event.getSource(), amount)) {
            return;
        }
        event.setCanceled(true);
        event.setAmount(0.0F);
    }

    /**
     * Shared entry for TaCZ {@code EntityHurtByGunEvent$Pre} (and any other external caller that
     * already resolved the attacker + damage amount). Cancels nothing itself — the caller must
     * cancel its own event when this returns {@code true}.
     */
    public static boolean tryReflectFromAttacker(LivingEntity target, LivingEntity attacker, float amount) {
        if (reflecting || target == null || attacker == null || attacker == target || amount <= 0.0F) {
            return false;
        }
        if (!(target.level() instanceof ServerLevel level)) {
            return false;
        }
        if (!target.hasEffect(ModEffects.SHAKEHANDS.get())) {
            return false;
        }
        applyReflect(level, target, attacker, amount);
        return true;
    }

    /**
     * @return {@code true} if the incoming hit was fully nullified and reflected (caller should
     * cancel the original damage event).
     */
    public static boolean tryReflectIncoming(LivingEntity target, DamageSource source, float amount) {
        if (reflecting || target == null || source == null || amount <= 0.0F) {
            return false;
        }
        if (isShakehandsReflect(source)) {
            // Own reflect damage must always be allowed through, even if the victim also holds
            // shakehands — otherwise dual holders would bounce forever / stack-overflow.
            return false;
        }
        if (!(target.level() instanceof ServerLevel level)) {
            return false;
        }
        if (!target.hasEffect(ModEffects.SHAKEHANDS.get())) {
            return false;
        }
        LivingEntity attacker = resolveAttacker(source, target);
        if (attacker == null) {
            return false;
        }
        applyReflect(level, target, attacker, amount);
        return true;
    }

    private static void applyReflect(ServerLevel level, LivingEntity target, LivingEntity attacker, float amount) {
        float reflected = Math.max(1.0F, amount) * 2.0F;
        com.rzy.dealt_force_skills.feedback.SkillFeedback.notice(attacker, "backlash");
        reflecting = true;
        try {
            // Clear i-frames so the reflected hit is never absorbed by recent vanilla invuln.
            attacker.invulnerableTime = 0;
            attacker.hurtTime = 0;
            boolean dealt = SkillDamageHelper.hurtUnscaled(attacker,
                    SkillDamageHelper.shakehandsReflect(level, target, target), reflected);
            // Fallback: if the unscaled helper rejected the hit for any reason, force vanilla hurt.
            if (!dealt && attacker.isAlive()) {
                attacker.invulnerableTime = 0;
                attacker.hurt(SkillDamageHelper.shakehandsReflect(level, target, target), reflected);
            }
        } finally {
            reflecting = false;
        }
    }

    /**
     * Resolves the responsible living attacker from a damage source. Handles melee, projectiles,
     * TaCZ-style bullet entities (non-living direct entity with living owner), and cases where only
     * {@code getDirectEntity()} is populated.
     */
    public static LivingEntity resolveAttacker(DamageSource source, LivingEntity target) {
        if (source == null) {
            return null;
        }
        LivingEntity fromEntity = livingAttacker(source.getEntity(), target);
        if (fromEntity != null) {
            return fromEntity;
        }
        return livingAttacker(source.getDirectEntity(), target);
    }

    private static LivingEntity livingAttacker(Entity candidate, LivingEntity target) {
        if (candidate == null || candidate == target) {
            return null;
        }
        if (candidate instanceof LivingEntity living) {
            return living;
        }
        if (candidate instanceof Projectile projectile
                && projectile.getOwner() instanceof LivingEntity owner
                && owner != target) {
            return owner;
        }
        // TaCZ / other gun mods may use a non-Projectile bullet entity that still exposes getOwner().
        try {
            var method = candidate.getClass().getMethod("getOwner");
            Object owner = method.invoke(candidate);
            if (owner instanceof LivingEntity living && living != target) {
                return living;
            }
        } catch (ReflectiveOperationException | LinkageError ignored) {
            // Optional owner bridge for non-vanilla projectiles.
        }
        return null;
    }

    private static boolean isShakehandsReflect(DamageSource source) {
        return source.is(SkillDamageHelper.SHAKEHANDS_REFLECT);
    }

    /**
     * @return {@code true} if the incoming effect was redirected back onto its source (in which
     * case the caller should stop processing this {@code MobEffectEvent.Added} any further,
     * since the effect instance has already been removed from the holder).
     */
    public static boolean handleEffectAdded(MobEffectEvent.Added event) {
        if (redirecting) {
            return false;
        }
        LivingEntity holder = event.getEntity();
        if (!holder.hasEffect(ModEffects.SHAKEHANDS.get())) {
            return false;
        }
        Entity source = event.getEffectSource();
        MobEffectInstance instance = event.getEffectInstance();
        MobEffect effect = instance.getEffect();
        if (effect == ModEffects.SHAKEHANDS.get() || source == holder) {
            return false;
        }
        // Shakehands rejects every externally sourced effect. Existing negative/control effects
        // are still redirected when a living source exists; beneficial or source-less effects
        // are simply removed from the holder.
        holder.removeEffect(effect);
        if (source instanceof LivingEntity attacker && EffectRedirectCategories.isRedirectable(effect)) {
            redirecting = true;
            try {
                attacker.addEffect(new MobEffectInstance(effect, instance.getDuration(), instance.getAmplifier(),
                        instance.isAmbient(), instance.isVisible(), instance.showIcon()), holder);
            } finally {
                redirecting = false;
            }
        }
        return true;
    }

    public static boolean blocksExternalSpecial(LivingEntity holder, Entity source) {
        return holder != null && holder.hasEffect(ModEffects.SHAKEHANDS.get()) && source != holder;
    }
}
