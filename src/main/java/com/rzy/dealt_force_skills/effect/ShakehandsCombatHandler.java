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
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

/**
 * Combat logic for the {@link ModEffects#SHAKEHANDS} effect: while an entity holds it, an
 * attack against it deals no damage (the attacker instead takes reflected true damage), and
 * any negative/control/special-negative effect the attack would have applied to the holder is
 * redirected back onto the attacker instead.
 *
 * <p>{@link LivingIncomingDamageEvent} is NeoForge's unified replacement for old Forge's split
 * LivingAttack/LivingHurt events. TaCZ gun hits are also cancelled on
 * {@code EntityHurtByGunEvent.Pre} via {@link #tryReflectFromAttacker}.</p>
 *
 * <p>Infinite dual-holder loops are prevented by (1) ignoring {@code shakehands_reflect} damage
 * types entirely, and (2) a re-entrancy flag around the reflect {@code hurt} call.</p>
 */
public final class ShakehandsCombatHandler {
    private static boolean redirecting;
    private static boolean reflecting;

    private ShakehandsCombatHandler() {
    }

    public static void handleLivingHurt(LivingIncomingDamageEvent event) {
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

    public static boolean tryReflectFromAttacker(LivingEntity target, LivingEntity attacker, float amount) {
        if (reflecting || target == null || attacker == null || attacker == target || amount <= 0.0F) {
            return false;
        }
        if (!(target.level() instanceof ServerLevel level)) {
            return false;
        }
        if (!target.hasEffect(ModEffects.SHAKEHANDS)) {
            return false;
        }
        applyReflect(level, target, attacker, amount);
        return true;
    }

    public static boolean tryReflectIncoming(LivingEntity target, DamageSource source, float amount) {
        if (reflecting || target == null || source == null || amount <= 0.0F) {
            return false;
        }
        if (isShakehandsReflect(source)) {
            return false;
        }
        if (!(target.level() instanceof ServerLevel level)) {
            return false;
        }
        if (!target.hasEffect(ModEffects.SHAKEHANDS)) {
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
        if (attacker instanceof ServerPlayer attackerPlayer) {
            attackerPlayer.displayClientMessage(Component.translatable(
                    "message.dealt_force_skills.shakehands.reflect"), true);
        }
        reflecting = true;
        try {
            attacker.invulnerableTime = 0;
            attacker.hurtTime = 0;
            boolean dealt = SkillDamageHelper.hurtUnscaled(attacker,
                    SkillDamageHelper.shakehandsReflect(level, target, target), reflected);
            if (!dealt && attacker.isAlive()) {
                attacker.invulnerableTime = 0;
                attacker.hurt(SkillDamageHelper.shakehandsReflect(level, target, target), reflected);
            }
        } finally {
            reflecting = false;
        }
    }

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

    public static boolean handleEffectAdded(MobEffectEvent.Added event) {
        if (redirecting) {
            return false;
        }
        LivingEntity holder = event.getEntity();
        if (!holder.hasEffect(ModEffects.SHAKEHANDS)) {
            return false;
        }
        Entity source = event.getEffectSource();
        MobEffectInstance instance = event.getEffectInstance();
        MobEffect effect = instance.getEffect().value();
        if (effect == ModEffects.SHAKEHANDS.value() || source == holder) {
            return false;
        }
        holder.removeEffect(instance.getEffect());
        if (source instanceof LivingEntity attacker && EffectRedirectCategories.isRedirectable(effect)) {
            redirecting = true;
            try {
                attacker.addEffect(new MobEffectInstance(instance.getEffect(), instance.getDuration(), instance.getAmplifier(),
                        instance.isAmbient(), instance.isVisible(), instance.showIcon()), holder);
            } finally {
                redirecting = false;
            }
        }
        return true;
    }

    public static boolean blocksExternalSpecial(LivingEntity holder, Entity source) {
        return holder != null && holder.hasEffect(ModEffects.SHAKEHANDS) && source != holder;
    }
}
