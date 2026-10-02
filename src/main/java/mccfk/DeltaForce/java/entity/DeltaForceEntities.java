package mccfk.DeltaForce.java.entity;

import mccfk.DeltaForce.java.DeltaForce;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * DeltaForce实体注册
 */
public class DeltaForceEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
        DeferredRegister.create(Registries.ENTITY_TYPE, DeltaForce.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<GroundItemEntity>> GROUND_ITEM =
        ENTITY_TYPES.register("df_item_e", () -> EntityType.Builder.<GroundItemEntity>of(
                GroundItemEntity::new, MobCategory.MISC)
            .sized(0.5F, 0.25F)
            .clientTrackingRange(6)
            .updateInterval(20)
            .build(ResourceLocation.fromNamespaceAndPath(DeltaForce.MODID, "df_item_e").toString()));
}
