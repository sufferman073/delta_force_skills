package com.rzy.dealt_force_skills.item;

import com.rzy.dealt_force_skills.config.DealtForceConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.function.Supplier;

public class RandomEffectConsumableItem extends DfsUseItem {
    private final List<List<EffectConsumableItem.EffectEntry>> outcomes;

    public RandomEffectConsumableItem(Properties properties,
                                      DfsItemQuality quality,
                                      String tooltipKey,
                                      int useTicks,
                                      Supplier<SoundEvent> startSound,
                                      Supplier<SoundEvent> finishSound,
                                      String startMessageKey,
                                      String finishMessageKey,
                                      List<List<EffectConsumableItem.EffectEntry>> outcomes) {
        super(properties, quality, tooltipKey, useTicks, startSound, finishSound, startMessageKey, finishMessageKey);
        java.util.ArrayList<List<EffectConsumableItem.EffectEntry>> configuredOutcomes = new java.util.ArrayList<>();
        for (int outcomeIndex = 0; outcomeIndex < outcomes.size(); outcomeIndex++) {
            List<EffectConsumableItem.EffectEntry> outcome = outcomes.get(outcomeIndex);
            java.util.ArrayList<EffectConsumableItem.EffectEntry> configuredEffects = new java.util.ArrayList<>();
            for (int effectIndex = 0; effectIndex < outcome.size(); effectIndex++) {
                EffectConsumableItem.EffectEntry entry = outcome.get(effectIndex);
                String key = configKey() + ".outcomes.outcome_" + outcomeIndex + ".effect_" + effectIndex;
                configuredEffects.add(new EffectConsumableItem.EffectEntry(entry.effect(),
                        DealtForceConfig.intValue(key + ".duration_ticks", entry.durationTicks()),
                        DealtForceConfig.intValue(key + ".amplifier", entry.amplifier())));
            }
            configuredOutcomes.add(List.copyOf(configuredEffects));
        }
        this.outcomes = List.copyOf(configuredOutcomes);
    }

    @Override
    protected boolean applyUseEffect(ItemStack stack, Level level, ServerPlayer player) {
        if (outcomes.isEmpty()) {
            return false;
        }
        List<EffectConsumableItem.EffectEntry> selected = outcomes.get(player.getRandom().nextInt(outcomes.size()));
        for (EffectConsumableItem.EffectEntry entry : selected) {
            MobEffect effect = entry.effect().get();
            if (effect == null) {
                continue;
            }
            player.addEffect(new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect),
                    entry.durationTicks(), entry.amplifier(),
                    false, true, true), player);
        }
        return true;
    }

    public List<EffectConsumableItem.EffectEntry> blessingPreviewOutcome(RandomSource random) {
        if (outcomes.isEmpty()) {
            return List.of();
        }
        RandomSource source = random == null ? RandomSource.create() : random;
        return outcomes.get(source.nextInt(outcomes.size()));
    }
}
