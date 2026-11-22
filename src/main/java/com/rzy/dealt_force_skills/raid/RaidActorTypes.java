package com.rzy.dealt_force_skills.raid;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class RaidActorTypes {
    public static EntityType<RaidOfflineBodyEntity> OFFLINE_BODY;
    private RaidActorTypes() { }
    @SubscribeEvent public static void register(RegisterEvent event) {
        event.register(Registries.ENTITY_TYPE, helper -> {
            OFFLINE_BODY = EntityType.Builder.of(RaidOfflineBodyEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.8F).clientTrackingRange(10).updateInterval(1)
                    .build("dealt_force_skills:raid_offline_body");
            helper.register(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("dealt_force_skills", "raid_offline_body"), OFFLINE_BODY);
        });
    }
    @SubscribeEvent public static void attributes(EntityAttributeCreationEvent event) {
        event.put(OFFLINE_BODY, RaidOfflineBodyEntity.attributes().build());
    }
}
