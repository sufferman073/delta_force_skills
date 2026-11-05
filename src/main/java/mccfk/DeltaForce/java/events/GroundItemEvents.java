package mccfk.DeltaForce.java.events;

import mccfk.DeltaForce.java.DeltaForce;
import mccfk.DeltaForce.java.entity.GroundItemEntity;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 地面物品事件处理器 - 处理DF物品的地面显示
 */
@Mod.EventBusSubscriber(modid = DeltaForce.MODID)
public class GroundItemEvents {

    /**
     * 地面物品标签 - 带有此标签的物品掉落时会变成地面物品实体
     */
    public static final TagKey<Item> GROUND_ITEM_TAG = TagKey.create(
        Registries.ITEM,
        new ResourceLocation(DeltaForce.MODID, "df_item_e")
    );

    /**
     * DF物品标签 - 带有此标签的物品也会生成自定义地面掉落物实体
     */
    public static final TagKey<Item> DF_ITEM_TAG = TagKey.create(
        Registries.ITEM,
        new ResourceLocation(DeltaForce.MODID, "df_item")
    );

    /**
     * 检查物品是否应该转换为地面物品实体
     */
    private static boolean shouldConvertToGroundItem(ItemStack itemStack) {
        if (!com.rzy.dealt_force_skills.config.DealtBaseConfig.customDropsEnabled() || itemStack.isEmpty()) {
            return false;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(itemStack.getItem());
        if (id != null && (DeltaForce.MODID.equals(id.getNamespace())
                || DealtForceSkillsMod.MODID.equals(id.getNamespace()))) {
            return true;
        }
        return itemStack.is(GROUND_ITEM_TAG) || itemStack.is(DF_ITEM_TAG);
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof ItemEntity itemEntity && !itemEntity.getItem().isEmpty()) {
            ItemStack itemStack = itemEntity.getItem();

            if (shouldConvertToGroundItem(itemStack)) {
                Level level = event.getEntity().level();

                if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
                    GroundItemEntity groundItem = new GroundItemEntity(
                        serverLevel,
                        itemEntity.getX(),
                        itemEntity.getY(),
                        itemEntity.getZ(),
                        itemStack.copy()
                    );
                    groundItem.setVanillaItem(itemEntity);
                    groundItem.setDeltaMovement(itemEntity.getDeltaMovement());
                    if(serverLevel.addFreshEntity(groundItem))event.setCanceled(true);
                }
            }
        }
    }

    /**
     * 工具方法:将普通物品掉落转换为地面物品
     */
    public static void convertToGroundItem(ServerLevel level, double x, double y, double z, ItemStack itemStack) {
        if (shouldConvertToGroundItem(itemStack)) {
            GroundItemEntity groundItem = new GroundItemEntity(level, x, y, z, itemStack);
            level.addFreshEntity(groundItem);
        } else {
            ItemEntity itemEntity = new ItemEntity(level, x, y, z, itemStack);
            level.addFreshEntity(itemEntity);
        }
    }
}