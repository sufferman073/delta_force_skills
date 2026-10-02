package mccfk.DeltaForce.java.item;

import mccfk.DeltaForce.java.DeltaForce;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * DeltaForce物品注册 - 所有三角洲行动(DF)相关物品
 * 物品品质等级由描述文件descriptions/*.txt第一行的.level(X);决定
 */
public class DeltaForceItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, DeltaForce.MODID);

    // 临时测试物品
    public static final DeferredHolder<Item, Item> TEMP_DF = plainItem("temp_df");

    public static final DeferredHolder<Item, Item> LOGO_DF = plainItem("logo_df");

    // 非洲之星
    public static final DeferredHolder<Item, Item> AFRICAN_STAR = plainItem("african_star");

    // 海洋之泪
    public static final DeferredHolder<Item, Item> OCEAN_TEAR = plainItem("ocean_tear");

    // 实验数据
    public static final DeferredHolder<Item, Item> EXPERIMENTAL_DATA = plainItem("experimental_data");

    // 碳纤维板
    public static final DeferredHolder<Item, Item> CARBON_FIBER_PLATE = plainItem("carbon_fiber_plate");

    // 金条
    public static final DeferredHolder<Item, Item> GOLD_BAR = plainItem("gold_bar");

    // 高速磁盘阵列
    public static final DeferredHolder<Item, Item> HIGH_SPEED_DISK_ARRAY = plainItem("high_speed_disk_array");

    // 金币
    public static final DeferredHolder<Item, Item> GOLD_COIN = plainItem("gold_coin");

    // 印象派画作
    public static final DeferredHolder<Item, Item> IMPRESSIONIST_PAINTING = plainItem("impressionist_painting");

    // 阿萨拉机密文件
    public static final DeferredHolder<Item, Item> ASARA_CLASSIFIED_DOCS = plainItem("asara_classified_docs");

    // 糖三角
    public static final DeferredHolder<Item, Item> SUGAR_TRIANGLE = plainItem("sugar_triangle");

    // 银币
    public static final DeferredHolder<Item, Item> SILVER_COIN = plainItem("silver_coin");

    // 铜币
    public static final DeferredHolder<Item, Item> COPPER_COIN = plainItem("copper_coin");

    // 小丑
    public static final DeferredHolder<Item, Item> LITTLE_JOKER = plainItem("little_joker");

    // ===== 调酒原材料 =====

    // 杜松灵
    public static final DeferredHolder<Item, Item> JUNIPER_ESSENCE = plainItem("juniper_essence");

    // 清逸饮
    public static final DeferredHolder<Item, Item> REFRESHING_DRINK = plainItem("refreshing_drink");

    // 琥珀露
    public static final DeferredHolder<Item, Item> AMBER_DEW = plainItem("amber_dew");

    // 蜜糖汁
    public static final DeferredHolder<Item, Item> HONEY_JUICE = plainItem("honey_juice");

    // 蔓越莓汁
    public static final DeferredHolder<Item, Item> CRANBERRY_JUICE = plainItem("cranberry_juice");

    // 柠檬汁
    public static final DeferredHolder<Item, Item> LEMON_JUICE = plainItem("lemon_juice");

    // 橙汁
    public static final DeferredHolder<Item, Item> ORANGE_JUICE = plainItem("orange_juice");

    // 可乐
    public static final DeferredHolder<Item, Item> COLA = plainItem("cola");

    // 糖浆
    public static final DeferredHolder<Item, Item> SYRUP = plainItem("syrup");

    // 苏打水
    public static final DeferredHolder<Item, Item> SODA_WATER = plainItem("soda_water");

    // 汤力水
    public static final DeferredHolder<Item, Item> TONIC_WATER = plainItem("tonic_water");

    private static DeferredHolder<Item, Item> plainItem(String name) {
        return ITEMS.register(name, () -> new Item(new Item.Properties()));
    }

    /**
     * 判断是否为调酒原材料（仅掉落物形态时使用 modelsnull 模型）
     */
    public static boolean isRawMaterial(ItemStack stack) {
        return stack.is(JUNIPER_ESSENCE.get())
            || stack.is(REFRESHING_DRINK.get())
            || stack.is(AMBER_DEW.get())
            || stack.is(HONEY_JUICE.get())
            || stack.is(CRANBERRY_JUICE.get())
            || stack.is(LEMON_JUICE.get())
            || stack.is(ORANGE_JUICE.get())
            || stack.is(COLA.get())
            || stack.is(SYRUP.get())
            || stack.is(SODA_WATER.get())
            || stack.is(TONIC_WATER.get());
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}