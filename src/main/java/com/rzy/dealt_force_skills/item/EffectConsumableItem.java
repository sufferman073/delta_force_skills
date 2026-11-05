package com.rzy.dealt_force_skills.item;

import com.rzy.dealt_force_skills.config.DealtForceConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.function.Supplier;

public class EffectConsumableItem extends DfsUseItem {
    private final List<EffectEntry> effects;

    public EffectConsumableItem(Properties properties,
                                DfsItemQuality quality,
                                String tooltipKey,
                                int useTicks,
                                Supplier<SoundEvent> startSound,
                                Supplier<SoundEvent> finishSound,
                                String startMessageKey,
                                String finishMessageKey,
                                List<EffectEntry> effects) {
        super(properties, quality, tooltipKey, useTicks, startSound, finishSound, startMessageKey, finishMessageKey);
        java.util.ArrayList<EffectEntry> configured = new java.util.ArrayList<>();
        for (int i = 0; i < effects.size(); i++) {
            EffectEntry entry = effects.get(i);
            String key = configKey() + ".effects.effect_" + i;
            configured.add(new EffectEntry(entry.effect(),
                    DealtForceConfig.intValue(key + ".duration_ticks", entry.durationTicks()),
                    DealtForceConfig.intValue(key + ".amplifier", entry.amplifier())));
        }
        this.effects = List.copyOf(configured);
    }

    @Override
    protected boolean applyUseEffect(ItemStack stack, Level level, ServerPlayer player) {
        for (EffectEntry entry : effects) {
            MobEffect effect = entry.effect().get();
            if (effect == null) {
                continue;
            }
            com.rzy.dealt_force_skills.department.DepartmentConsumables.addEffect(player,new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect),
                    entry.durationTicks(), entry.amplifier(),
                    false, true, true));
        }
        return true;
    }

    public List<EffectEntry> blessingPreviewEffects() {
        return effects;
    }

    public record EffectEntry(Supplier<MobEffect> effect, int durationTicks, int amplifier) {
    }
    @Override public boolean useOnOperative(ItemStack stack, com.rzy.dealt_force_skills.entity.OperativeEntity target) {
        for (EffectEntry entry : effects) if (entry.effect().get() != null) {
            MobEffect effect = entry.effect().get();
            if (effect.isInstantenous()) effect.applyInstantenousEffect(target, target, target, entry.amplifier(), 1);
            else target.addEffect(new MobEffectInstance(net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect), entry.durationTicks(), entry.amplifier()), target);
        }
        return true;
    }
}
