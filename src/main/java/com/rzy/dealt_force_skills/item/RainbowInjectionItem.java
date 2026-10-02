package com.rzy.dealt_force_skills.item;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.function.Supplier;

public class RainbowInjectionItem extends DfsUseItem {
    private static final String IRONS_ASCENSION_ID = "irons_spellbooks:ascension";

    public RainbowInjectionItem(Properties properties,
                                DfsItemQuality quality,
                                String tooltipKey,
                                int useTicks,
                                Supplier<SoundEvent> startSound,
                                Supplier<SoundEvent> finishSound,
                                String startMessageKey,
                                String finishMessageKey) {
        super(properties, quality, tooltipKey, useTicks, startSound, finishSound, startMessageKey, finishMessageKey);
    }

    @Override
    protected boolean applyUseEffect(ItemStack stack, Level level, ServerPlayer player) {
        if (player.getRandom().nextBoolean()) {
            for (MobEffect effect : BuiltInRegistries.MOB_EFFECT) {
                Holder<MobEffect> holder = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect);
                if (effect.getCategory() == MobEffectCategory.BENEFICIAL
                        && !holder.is(MobEffects.LEVITATION)
                        && !isExcludedModEffect(effect)) {
                    player.addEffect(new MobEffectInstance(holder, com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("items.rainbow_injection_item.effect.custom.0.duration_ticks", 360 * 20), com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("items.rainbow_injection_item.effect.custom.0.amplifier", 3), false, true, true), player);
                }
            }
        } else {
            player.hurt(player.damageSources().genericKill(), Float.MAX_VALUE);
        }
        return true;
    }

    private static boolean isExcludedModEffect(MobEffect effect) {
        var id = BuiltInRegistries.MOB_EFFECT.getKey(effect);
        return id != null && IRONS_ASCENSION_ID.equals(id.toString());
    }
}
