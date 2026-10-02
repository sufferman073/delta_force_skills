package com.rzy.dealt_force_skills.effect;

import com.rzy.dealt_force_skills.registry.ModEffects;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Hand-curated taxonomy of "control effects" and "special negative effects" used by the
 * shakehands redirect logic. The codebase has no pre-existing classification for these two
 * buckets (only the vanilla {@link MobEffectCategory#HARMFUL} split exists), so this list was
 * assembled by reviewing every registered {@link ModEffects} entry. Adjust freely if a
 * character's effect should or should not be redirected back onto an attacker.
 *
 * <p>Lazily built on first access so it only touches {@link ModEffects} fields after the
 * registry has finished populating its {@code DeferredHolder} instances.
 */
public final class EffectRedirectCategories {
    private static Set<MobEffect> controlEffects;
    private static Set<MobEffect> specialNegativeEffects;

    private EffectRedirectCategories() {
    }

    public static boolean isRedirectable(MobEffect effect) {
        if (effect == null) {
            return false;
        }
        return effect.getCategory() == MobEffectCategory.HARMFUL
                || controlEffects().contains(effect)
                || specialNegativeEffects().contains(effect);
    }

    private static Set<MobEffect> controlEffects() {
        if (controlEffects == null) {
            Set<MobEffect> set = new HashSet<>();
            add(set, ModEffects.STUN);
            add(set, ModEffects.N_TWO_FROZEN);
            add(set, ModEffects.N_TWO_DISRUPTED);
            add(set, ModEffects.WEBBED);
            add(set, ModEffects.TEMPEST_DISARMED);
            add(set, ModEffects.RAPTOR_ACTION_PAUSE);
            add(set, ModEffects.MANBA_BLINDED);
            add(set, ModEffects.HACKCLAW_FLASH_BLIND);
            add(set, ModEffects.MORSE_FLASH_BLIND);
            add(set, ModEffects.NOX_FLASHED);
            add(set, ModEffects.TOXIK_TEAR_GAS_BLIND);
            add(set, ModEffects.CATDAD_HISS_SLOW);
            controlEffects = Collections.unmodifiableSet(set);
        }
        return controlEffects;
    }

    private static Set<MobEffect> specialNegativeEffects() {
        if (specialNegativeEffects == null) {
            Set<MobEffect> set = new HashSet<>();
            add(set, ModEffects.CORROSION);
            add(set, ModEffects.NOX_DELAYED_WOUND);
            add(set, ModEffects.NOX_CRIPPLED);
            add(set, ModEffects.NIKAIDOU_DOOMED);
            add(set, ModEffects.NIKAIDOU_RIFT_STACKS);
            add(set, ModEffects.DEPARTMENT_VULNERABLE);
            add(set, ModEffects.CATDAD_ARMOR_REDUCED);
            add(set, ModEffects.ITEM_WEAKNESS);
            add(set, ModEffects.TEMPEST_EXPLOSIVE_SPINE);
            add(set, ModEffects.TOXIK_FIREFLY_INTERFERENCE);
            add(set, ModEffects.VLINDER_MEDICAL_WASTE_INTERFERENCE);
            add(set, ModEffects.STINGER_STIM_SUPPRESSION);
            add(set, ModEffects.MORSE_STRONG_SHOCK);
            add(set, ModEffects.SONIC_SHOCK);
            add(set, ModEffects.LAUGHING_MANIA_I);
            add(set, ModEffects.LAUGHING_MANIA_II);
            add(set, ModEffects.LAUGHING_MANIA_III);
            add(set, ModEffects.HELA);
            add(set, ModEffects.HACKCLAW_INTERFERENCE);
            add(set, ModEffects.LEFT_LEG_FRACTURE);
            add(set, ModEffects.RIGHT_LEG_FRACTURE);
            add(set, ModEffects.LEFT_ARM_FRACTURE);
            add(set, ModEffects.RIGHT_ARM_FRACTURE);
            add(set, ModEffects.ABDOMEN_INJURY);
            add(set, ModEffects.CHEST_INJURY);
            add(set, ModEffects.HEAD_INJURY);
            add(set, ModEffects.LEFT_LEG_WOUND);
            add(set, ModEffects.RIGHT_LEG_WOUND);
            add(set, ModEffects.LEFT_ARM_WOUND);
            add(set, ModEffects.RIGHT_ARM_WOUND);
            add(set, ModEffects.ABDOMEN_WOUND);
            add(set, ModEffects.CHEST_WOUND);
            add(set, ModEffects.HEAD_WOUND);
            specialNegativeEffects = Collections.unmodifiableSet(set);
        }
        return specialNegativeEffects;
    }

    private static void add(Set<MobEffect> set, DeferredHolder<MobEffect, MobEffect> holder) {
        if (holder != null && holder.isBound()) {
            set.add(holder.get());
        }
    }
}
