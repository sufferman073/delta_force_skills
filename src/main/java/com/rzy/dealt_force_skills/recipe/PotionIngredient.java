package com.rzy.dealt_force_skills.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.rzy.dealt_force_skills.registry.ModRecipeSerializers;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;

import java.util.Optional;
import java.util.stream.Stream;

public record PotionIngredient(Optional<MobEffectCategory> category, boolean allContainers) implements ICustomIngredient {
    public static final MapCodec<PotionIngredient> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.comapFlatMap(PotionIngredient::parseCategory, MobEffectCategory::name)
                    .optionalFieldOf("category").forGetter(PotionIngredient::category),
            Codec.BOOL.optionalFieldOf("all_containers", false).forGetter(PotionIngredient::allContainers)
    ).apply(instance, PotionIngredient::new));

    private static DataResult<MobEffectCategory> parseCategory(String name) {
        try {
            return DataResult.success(MobEffectCategory.valueOf(name));
        } catch (IllegalArgumentException exception) {
            return DataResult.error(() -> "Unknown potion effect category: " + name);
        }
    }

    @Override
    public boolean test(ItemStack stack) {
        if (!stack.is(Items.POTION) && !(allContainers
                && (stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION)))) {
            return false;
        }
        if (category.isEmpty()) {
            return true;
        }
        boolean matches = false;
        for (var effect : stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).getAllEffects()) {
            // The harmful brewing recipe has always taken precedence for mixed-effect potions.
            if (category.get() == MobEffectCategory.BENEFICIAL
                    && effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
                return false;
            }
            if (effect.getEffect().value().getCategory() == category.get()) {
                matches = true;
            }
        }
        return matches;
    }

    @Override
    public Stream<ItemStack> getItems() {
        Stream<Item> containers = allContainers
                ? Stream.of(Items.POTION, Items.SPLASH_POTION, Items.LINGERING_POTION) : Stream.of(Items.POTION);
        // Match arbitrary custom effects at runtime, but give browsers concrete registered potions to display.
        return containers.flatMap(item -> BuiltInRegistries.POTION.holders().map(potion -> {
            ItemStack stack = new ItemStack(item);
            stack.set(DataComponents.POTION_CONTENTS, new PotionContents(potion));
            return stack;
        })).filter(this::test);
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    public IngredientType<?> getType() {
        return ModRecipeSerializers.POTION_INGREDIENT.get();
    }
}
