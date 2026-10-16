package com.rzy.dealt_force_skills.compat.jei;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.registry.ModItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapelessRecipe;

import java.util.List;

@JeiPlugin
public final class DfsJeiPlugin implements IModPlugin {
    @Override public void onRuntimeAvailable(mezz.jei.api.runtime.IJeiRuntime runtime) { HvkJeiRecipes.available(runtime); }
    @Override public void onRuntimeUnavailable() { HvkJeiRecipes.unavailable(); }

    @Override
    public ResourceLocation getPluginUid() {
        return id("jei");
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registerInfusionCrafting(registration);
        for (Item item : List.of(
                ModItems.SPECIAL_EQUIPMENT_SUPPLY.get(),
                ModItems.BLUEPRINT.get(),
                ModItems.DARK_ZONE_RAINBOW_INJECTION.get(),
                ModItems.ANIMAL_GLAND.get(),
                ModItems.PH_REGULATOR.get(),
                ModItems.COARSE_SALT.get(),
                ModItems.PORTABLE_ADS_DEVICE.get(),
                ModItems.RESPAWN_BEACON.get(),
                ModItems.INTERDIMENSIONAL_BLOCK.get(),
                ModItems.GTI_SPECIAL_OPERATIONS.get(),
                ModItems.RAVEN_SHADOW_EXTRACT.get(),
                ModItems.EXPERIMENTAL_IRON_CURTAIN_CATALYST.get(),
                ModItems.PROTOTYPE_MADNESS_COMPOUND.get(),
                ModItems.HVK_ASARA_ARENA.get(),
                ModItems.HVK_ENCHANT_INFUSION.get(),
                ModItems.HVK_ADVANCED_STANDARD_TEMPLATE_CONSTRUCTOR.get(),
                ModItems.HVK_ADVANCED_DISASSEMBLY_BEAM_EMITTER.get(),
                ModItems.HVK_ADVANCED_SPAWNER.get(),
                ModItems.HVK_CLONE_PROTOTYPE.get(),
                ModItems.HVK_ADVANCED_TREASURE_COMPASS.get(),
                ModItems.HVK_TREASURE_CHEST_BOX.get(),
                ModItems.HOOK_AUTHOR_BLESSING.get(),
                ModItems.ETERNAL_LOVE_BLESSING.get())) {
            String key = "jei." + DealtForceSkillsMod.MODID + "." + BuiltInRegistries.ITEM.getKey(item).getPath();
            registration.addItemStackInfo(new ItemStack(item), Component.translatable(key));
        }
    }

    private static void registerInfusionCrafting(IRecipeRegistration registration) {
        var level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        var enchantments = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        NonNullList<Ingredient> books = NonNullList.create();
        // These nine books are examples; the actual recipe accepts any nine distinct item data sets.
        for (String name : List.of("protection", "fire_protection", "feather_falling", "blast_protection",
                "projectile_protection", "respiration", "aqua_affinity", "thorns", "depth_strider")) {
            var enchantment = enchantments.getHolder(ResourceLocation.withDefaultNamespace(name)).orElseThrow();
            books.add(Ingredient.of(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(enchantment, 1))));
        }
        var recipe = new ShapelessRecipe( "",
                CraftingBookCategory.MISC, new ItemStack(ModItems.HVK_ENCHANT_INFUSION.get()), books);
        registration.addRecipes(RecipeTypes.CRAFTING, List.of(new net.minecraft.world.item.crafting.RecipeHolder<>(id("hvk_enchant_infusion"), recipe)));
    }


    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, path);
    }
}
