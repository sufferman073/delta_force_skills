package mccfk.DeltaForce.java.item.creative;

import com.rzy.dealt_force_skills.registry.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

import mccfk.DeltaForce.java.DeltaForce;
import mccfk.DeltaForce.java.item.DeltaForceItems;

/**
 * DeltaForce创造模式标签页 - 三角洲行动物品专属标签页
 */
public class DeltaForceCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, DeltaForce.MODID);

    // 三角洲行动创造模式标签页
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> DELTAFORCE_TAB = CREATIVE_MODE_TABS.register("deltaforce",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("tab.deltaforce"))
                    .icon(() -> DeltaForceItems.LOGO_DF.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(DeltaForceItems.TEMP_DF.get());

                        output.accept(DeltaForceItems.AFRICAN_STAR.get());
                        output.accept(DeltaForceItems.OCEAN_TEAR.get());
                        output.accept(DeltaForceItems.EXPERIMENTAL_DATA.get());
                        output.accept(DeltaForceItems.CARBON_FIBER_PLATE.get());
                        output.accept(DeltaForceItems.GOLD_BAR.get());
                        output.accept(DeltaForceItems.HIGH_SPEED_DISK_ARRAY.get());

                        output.accept(ModItems.BLUEPRINT.get());
                        output.accept(DeltaForceItems.GOLD_COIN.get());

                        output.accept(DeltaForceItems.IMPRESSIONIST_PAINTING.get());
                        output.accept(DeltaForceItems.ASARA_CLASSIFIED_DOCS.get());

                        output.accept(DeltaForceItems.SUGAR_TRIANGLE.get());
                        output.accept(ModItems.DANCING_LADY.get());
                        output.accept(DeltaForceItems.SILVER_COIN.get());

                        output.accept(DeltaForceItems.COPPER_COIN.get());

                        output.accept(DeltaForceItems.LITTLE_JOKER.get());

                        // 调酒原材料
                        output.accept(DeltaForceItems.JUNIPER_ESSENCE.get());
                        output.accept(DeltaForceItems.REFRESHING_DRINK.get());
                        output.accept(DeltaForceItems.AMBER_DEW.get());
                        output.accept(DeltaForceItems.HONEY_JUICE.get());
                        output.accept(DeltaForceItems.CRANBERRY_JUICE.get());
                        output.accept(DeltaForceItems.LEMON_JUICE.get());
                        output.accept(DeltaForceItems.ORANGE_JUICE.get());
                        output.accept(DeltaForceItems.COLA.get());
                        output.accept(DeltaForceItems.SYRUP.get());
                        output.accept(DeltaForceItems.SODA_WATER.get());
                        output.accept(DeltaForceItems.TONIC_WATER.get());
                    })
                    .build());

    public static void register(IEventBus modEventBus) {
        CREATIVE_MODE_TABS.register(modEventBus);
    }
}
