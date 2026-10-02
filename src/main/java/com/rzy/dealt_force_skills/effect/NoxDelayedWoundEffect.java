package com.rzy.dealt_force_skills.effect;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.registry.ModEffects;
import net.minecraft.resources.ResourceKey;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class NoxDelayedWoundEffect extends MobEffect {
    private static final String ROOT_TAG = DealtForceSkillsMod.MODID + ".nox_delayed_wound";
    private static final String HEALTH_CAP = "HealthCap";
    private static final String FOOD_CAP = "FoodCap";
    private static final String SATURATION_CAP = "SaturationCap";
    private static final Map<ResourceKey<Level>, Set<UUID>> TRACKED_ENTITIES = new HashMap<>();

    public NoxDelayedWoundEffect() {
        super(MobEffectCategory.NEUTRAL, 0xFFFFFF);
    }

    public static void resetCaps(LivingEntity entity, float healthCap) {
        if (entity.level().isClientSide) {
            return;
        }
        track(entity);
        CompoundTag tag = data(entity);
        float currentHealth = entity.getHealth();
        float newCap = Math.max(0.0f, Math.min(entity.getMaxHealth(), Math.min(currentHealth, healthCap)));
        if (tag.contains(HEALTH_CAP)) {
            newCap = Math.min(newCap, tag.getFloat(HEALTH_CAP));
        }
        tag.putFloat(HEALTH_CAP, newCap);
        if (entity instanceof Player player) {
            FoodData food = player.getFoodData();
            int foodCap = food.getFoodLevel();
            float saturationCap = food.getSaturationLevel();
            if (tag.contains(FOOD_CAP)) {
                foodCap = Math.min(foodCap, tag.getInt(FOOD_CAP));
            }
            if (tag.contains(SATURATION_CAP)) {
                saturationCap = Math.min(saturationCap, tag.getFloat(SATURATION_CAP));
            }
            tag.putInt(FOOD_CAP, Math.max(0, foodCap));
            tag.putFloat(SATURATION_CAP, Math.max(0.0f, saturationCap));
        }
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        enforceCaps(entity);
        return true;
    }

    public static void enforceCaps(LivingEntity entity) {
        if (entity.level().isClientSide) {
            return;
        }

        track(entity);
        CompoundTag tag = data(entity);
        float currentHealth = entity.getHealth();
        float healthCap = tag.contains(HEALTH_CAP) ? tag.getFloat(HEALTH_CAP) : currentHealth;
        healthCap = Math.max(0.0f, Math.min(entity.getMaxHealth(), healthCap));
        if (currentHealth > healthCap) {
            entity.setHealth(healthCap);
        } else if (currentHealth < healthCap) {
            tag.putFloat(HEALTH_CAP, currentHealth);
        }

        if (!(entity instanceof Player player)) {
            return;
        }

        FoodData food = player.getFoodData();
        int currentFood = food.getFoodLevel();
        float currentSaturation = food.getSaturationLevel();
        int foodCap = tag.contains(FOOD_CAP) ? tag.getInt(FOOD_CAP) : currentFood;
        float saturationCap = tag.contains(SATURATION_CAP) ? tag.getFloat(SATURATION_CAP) : currentSaturation;

        if (currentFood > foodCap) {
            food.setFoodLevel(Math.max(0, foodCap));
        } else if (currentFood < foodCap) {
            tag.putInt(FOOD_CAP, currentFood);
        }

        if (currentSaturation > saturationCap) {
            food.setSaturation(Math.max(0.0f, saturationCap));
        } else if (currentSaturation < saturationCap) {
            tag.putFloat(SATURATION_CAP, currentSaturation);
        }
    }

    public static void enforceTracked(ServerLevel level) {
        Set<UUID> tracked = TRACKED_ENTITIES.get(level.dimension());
        if (tracked == null || tracked.isEmpty()) {
            return;
        }

        Iterator<UUID> iterator = tracked.iterator();
        while (iterator.hasNext()) {
            UUID id = iterator.next();
            Entity entity = level.getEntity(id);
            if (!(entity instanceof LivingEntity living)
                    || !living.isAlive()
                    || !living.hasEffect(ModEffects.NOX_DELAYED_WOUND)) {
                if (entity instanceof LivingEntity staleLiving) {
                    staleLiving.getPersistentData().remove(ROOT_TAG);
                }
                iterator.remove();
                continue;
            }
            enforceCaps(living);
        }

        if (tracked.isEmpty()) {
            TRACKED_ENTITIES.remove(level.dimension());
        }
    }

    public static void clearCaps(LivingEntity entity) {
        entity.getPersistentData().remove(ROOT_TAG);
        untrack(entity);
    }

    public static void clearTrackedEntities() {
        TRACKED_ENTITIES.clear();
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    private static CompoundTag data(LivingEntity entity) {
        CompoundTag persistent = entity.getPersistentData();
        if (!persistent.contains(ROOT_TAG, Tag.TAG_COMPOUND)) {
            persistent.put(ROOT_TAG, new CompoundTag());
        }
        return persistent.getCompound(ROOT_TAG);
    }

    private static void track(LivingEntity entity) {
        TRACKED_ENTITIES.computeIfAbsent(entity.level().dimension(), key -> new HashSet<>()).add(entity.getUUID());
    }

    private static void untrack(LivingEntity entity) {
        Set<UUID> tracked = TRACKED_ENTITIES.get(entity.level().dimension());
        if (tracked == null) {
            return;
        }
        tracked.remove(entity.getUUID());
        if (tracked.isEmpty()) {
            TRACKED_ENTITIES.remove(entity.level().dimension());
        }
    }
}
