package com.rzy.dealt_force_skills.item;

import com.rzy.dealt_force_skills.effect.ModItemEffectHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.function.Supplier;

public class SpecialEquipmentSupplyItem extends DfsUseItem {
    public SpecialEquipmentSupplyItem(Properties properties,
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
        ModItemEffectHelper.refreshSkillCooldowns(player);
        return true;
    }
    @Override public boolean useOnOperative(ItemStack stack, com.rzy.dealt_force_skills.entity.OperativeEntity target) {
        target.skills().refresh(target);
        return true;
    }
}
