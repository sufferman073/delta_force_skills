package com.rzy.dealt_force_skills.recipe;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.registry.ModItems;
import com.rzy.dealt_force_skills.registry.ModRecipeSerializers;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID)
public final class HvkEnchantInfusionRecipe extends CustomRecipe {
    public HvkEnchantInfusionRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        List<ItemStack> distinct = new ArrayList<>();
        for (int index = 0; index < input.size(); index++) {
            ItemStack stack = input.getItem(index);
            if (stack.isEmpty()) {
                continue;
            }
            if (!stack.is(Items.ENCHANTED_BOOK)
                    || distinct.stream().anyMatch(existing -> ItemStack.isSameItemSameComponents(existing, stack))) {
                return false;
            }
            distinct.add(stack);
        }
        return distinct.size() == 9;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        return new ItemStack(ModItems.HVK_ENCHANT_INFUSION.get());
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 9;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.withSize(9, Ingredient.of(Items.ENCHANTED_BOOK));
    }

    @Override
    public boolean isSpecial() {
        return false;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeSerializers.HVK_ENCHANT_INFUSION.get();
    }

    @SubscribeEvent
    public static void onCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (!event.getCrafting().is(ModItems.HVK_ENCHANT_INFUSION.get())) {
            return;
        }
        float luck = Math.max(0.0F, event.getEntity().getLuck());
        float failureChance = Math.max(0.04F, 0.99F - luck * 0.05F);
        if (event.getEntity().getRandom().nextFloat() < failureChance) {
            event.getCrafting().setCount(0);
            event.getEntity().displayClientMessage(Component.translatable(
                    "message.dealt_force_skills.hvk_enchant_infusion.craft_failed"), true);
        }
    }
}
