package com.rzy.dealt_force_skills.compat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.core.registries.BuiltInRegistries;

public final class TrainingTargetCompat {
    private static final ResourceLocation DUMMMMMMY_TARGET =
            ResourceLocation.fromNamespaceAndPath("dummmmmmy", "target_dummy");

    private TrainingTargetCompat() {
    }

    public static boolean recordsDamageWithoutDying(LivingEntity entity) {
        return entity != null && DUMMMMMMY_TARGET.equals(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()));
    }
}