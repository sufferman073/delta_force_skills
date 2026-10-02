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
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;

/**
 * 地面物品事件处理器 - 处理DF物品的地面显示
 */
@EventBusSubscriber(modid = DeltaForce.MODID)
public class GroundItemEvents {

    /**
     * 地面物品标签 - 带有此标签的物品掉落时会变成地面物品实体
     */
    public static final TagKey<Item> GROUND_ITEM_TAG = TagKey.create(
        Registries.ITEM,
        ResourceLocation.fromNamespaceAndPath(DeltaForce.MODID, "df_item_e")
    );

    /**
     * DF物品标签 - 带有此标签的物品也会生成自定义地面掉落物实体
     */
    public static final TagKey<Item> DF_ITEM_TAG = TagKey.create(
        Registries.ITEM,
        ResourceLocation.fromNamespaceAndPath(DeltaForce.MODID, "df_item")
    );

    /**
     * 检查物品是否应该转换为地面物品实体。
     * 说明：需求为"模组全部物品都视为带 #deltaforce:df_item 标签"，使模组物品掉落时生成自定义地面物品实体。
     * 仅转换本模组（deltaforce / dealt_force_skills）注册的物品；原版与其他模组物品保持原样掉落。
     */
    private static boolean shouldConvertToGroundItem(ItemStack itemStack) {
        if (itemStack.isEmpty()) {
            return false;
        }
        // 本模组（deltaforce / dealt_force_skills）注册的物品全部转换
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(itemStack.getItem());
        if (id != null) {
            String namespace = id.getNamespace();
            if (namespace.equals(DeltaForce.MODID) || namespace.equals(DealtForceSkillsMod.MODID)) {
                return true;
            }
        }
        // 带 df_item / df_item_e 标签的物品也转换
        return itemStack.is(GROUND_ITEM_TAG) || itemStack.is(DF_ITEM_TAG);
    }

    @SubscribeEvent
    public static void onItemToss(ItemTossEvent event) {
        ItemStack itemStack = event.getEntity().getItem();

        if (shouldConvertToGroundItem(itemStack)) {
            event.setCanceled(true);

            var level = event.getEntity().level();
            var player = event.getPlayer();

            if (!level.isClientSide()) {
                double x = player.getX();
                double y = player.getY();
                double z = player.getZ();

                GroundItemEntity groundItem = new GroundItemEntity(
                    level,
                    x,
                    y,
                    z,
                    itemStack
                );
                groundItem.setDeltaMovement(event.getEntity().getDeltaMovement());
                level.addFreshEntity(groundItem);
            }
        }
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof ItemEntity itemEntity && !itemEntity.getItem().isEmpty()) {
            ItemStack itemStack = itemEntity.getItem();

            if (shouldConvertToGroundItem(itemStack)) {
                Level level = event.getEntity().level();

                if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
                    event.setCanceled(true);

                    GroundItemEntity groundItem = new GroundItemEntity(
                        serverLevel,
                        itemEntity.getX(),
                        itemEntity.getY(),
                        itemEntity.getZ(),
                        itemStack.copy()
                    );
                    groundItem.setDeltaMovement(itemEntity.getDeltaMovement());
                    serverLevel.addFreshEntity(groundItem);
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
