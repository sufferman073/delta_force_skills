package com.rzy.dealt_force_skills.ai.operative;

import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.entity.OperativeEntity;
import com.rzy.dealt_force_skills.registry.ModEffects;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import java.util.HashSet;
import java.util.UUID;

/** Toxik's native effect policy; explicit casts and the effect event share one recursion guard. */
public final class NativeToxikEffects {
    private static final java.util.Set<UUID> SCALING = new HashSet<>();
    private NativeToxikEffects() { }

    public static boolean isToxik(Entity entity) {
        return entity instanceof OperativeHost host && com.rzy.dealt_force_skills.ai.operative.NativeCommanderSkills.inherited(host.skills(), NativeToxikSkills.class) instanceof NativeToxikSkills;
    }

    public static boolean tryRescale(LivingEntity target, MobEffectInstance effect, Entity source) {
        if (target.level().isClientSide || effect == null || SCALING.contains(target.getUUID())
                || effect.getEffect() == ModEffects.CHARACTER_FRAMEWORK) return false;
        if (!isToxik(source) && !(source == null && isToxik(target)
                && effect.getEffect().value().getCategory() == MobEffectCategory.BENEFICIAL)) return false;
        // Added fires before vanilla inserts or merges this same instance.
        // Re-entering addEffect here would be overwritten by that outer insertion.
        var scaled = scaled(source == null ? target : source, target, effect);
        var incoming = (com.rzy.dealt_force_skills.mixin.OperativeEffectAccessor) effect;
        incoming.dealt_force_skills$setDuration(scaled.getDuration());
        incoming.dealt_force_skills$setAmplifier(scaled.getAmplifier());
        return true;
    }

    public static boolean add(Entity source, LivingEntity target, MobEffectInstance original) {
        var effect = scaled(source, target, original);
        SCALING.add(target.getUUID());
        try { return target.addEffect(effect, source); }
        finally { SCALING.remove(target.getUUID()); }
    }

    private static MobEffectInstance scaled(Entity source, LivingEntity target, MobEffectInstance original) {
        boolean self = com.rzy.dealt_force_skills.util.TargetingUtil.isSelf(source, target);
        boolean positive = original.getEffect().value().getCategory() == MobEffectCategory.BENEFICIAL;
        int duration = original.isInfiniteDuration() ? -1 : Math.max(1, original.getDuration());
        int amplifier = Math.max(0, original.getAmplifier());
        int multiplier = positive ? (self || OperativeSupport.ally(source, target) ? 10 : 1)
                : self ? 1 : target instanceof Player ? 5 : 20;
        if (self && !positive) { duration = duration == -1 ? -1 : Math.max(1, duration / 100); amplifier = 0; }
        else {
            duration = duration == -1 ? -1 : (int) Math.min(Integer.MAX_VALUE, (long) duration * multiplier);
            amplifier = (int) Math.min(Integer.MAX_VALUE, ((long) amplifier + 1) * multiplier - 1);
        }
        return new MobEffectInstance(original.getEffect(), duration, amplifier,
                original.isAmbient(), original.isVisible(), original.showIcon());
    }

    public static void adrenaline(Entity source, LivingEntity target, int baseTicks) {
        for (var active : java.util.List.copyOf(target.getActiveEffects()))
            if (active.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) target.removeEffect(active.getEffect());
        if (OperativeGrowth.commander(source)) {
            SCALING.add(target.getUUID());
            try {
                target.addEffect(new MobEffectInstance(ModEffects.TOXIK_ADRENALINE,
                        (int)Math.min(Integer.MAX_VALUE, Math.max(1L, (long)baseTicks * 26)), 29, false, true, true), source);
            } finally { SCALING.remove(target.getUUID()); }
            return;
        }
        add(source, target, new MobEffectInstance(ModEffects.TOXIK_ADRENALINE, Math.max(1, baseTicks),
                com.rzy.dealt_force_skills.config.DealtForceConfig.intValue(
                        "characters.toxik.toxik_state_manager.effect.toxik_adrenaline.0.amplifier", 0), false, true, true));
    }

}
