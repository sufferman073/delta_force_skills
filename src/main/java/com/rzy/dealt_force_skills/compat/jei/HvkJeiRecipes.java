package com.rzy.dealt_force_skills.compat.jei;

import com.rzy.dealt_force_skills.compat.HvkJeiRecipeCatalog;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Client-only JEI access. No JEI classes occur in the server catalog or either work block. */
final class HvkJeiRecipes {
    private static volatile int generation;
    private HvkJeiRecipes() { }

    static void available(IJeiRuntime runtime) {
        int token = ++generation;
        Minecraft client = Minecraft.getInstance();
        var server = client.getSingleplayerServer();
        if (server == null) return;
        client.execute(() -> {
            if (token != generation || client.getSingleplayerServer() != server) return;
            List<HvkJeiRecipeCatalog.Entry> entries = new ArrayList<>();
            try (var categories = runtime.getRecipeManager().createRecipeCategoryLookup().includeHidden().get()) {
                categories.forEach(category -> collect(runtime, category, entries));
            } catch (RuntimeException | LinkageError ignored) {
                // JEI is optional; keep usable recipes from categories that completed successfully.
            }
            List<HvkJeiRecipeCatalog.Entry> snapshot = List.copyOf(entries);
            com.mojang.logging.LogUtils.getLogger().info("HVK collected {} JEI item recipes for the integrated server", snapshot.size());
            server.execute(() -> {
                if (token == generation) HvkJeiRecipeCatalog.replace(server, snapshot);
            });
        });
    }

    static void unavailable() {
        ++generation;
        var server = Minecraft.getInstance().getSingleplayerServer();
        if (server != null) server.execute(() -> HvkJeiRecipeCatalog.replace(server, List.of()));
    }

    private static <T> void collect(IJeiRuntime runtime, IRecipeCategory<T> category,
                                    List<HvkJeiRecipeCatalog.Entry> entries) {
        try (var recipes = runtime.getRecipeManager().createRecipeLookup(category.getRecipeType()).includeHidden().get()) {
            var iterator = recipes.iterator();
            while (iterator.hasNext() && entries.size() < 50000) {
                T recipe = iterator.next();
                try {
                    var layout = runtime.getRecipeManager().createRecipeLayoutDrawable(category, recipe,
                            runtime.getJeiHelpers().getFocusFactory().getEmptyFocusGroup());
                    if (layout.isEmpty()) continue;
                    List<HvkJeiRecipeCatalog.Input> inputs = new ArrayList<>();
                    ItemStack output = ItemStack.EMPTY;
                    boolean supported = true;
                    for (var slot : layout.get().getRecipeSlotsView().getSlotViews()) {
                        if (slot.getRole() != RecipeIngredientRole.INPUT && slot.getRole() != RecipeIngredientRole.OUTPUT) continue;
                        List<ItemStack> choices = slot.getItemStacks().filter(stack -> !stack.isEmpty()).map(ItemStack::copy).toList();
                        if (choices.isEmpty()) {
                            if (!slot.isEmpty()) supported = false; // Do not turn fluids into free item recipes.
                            continue;
                        }
                        if (slot.getRole() == RecipeIngredientRole.OUTPUT) {
                            if (!output.isEmpty() || choices.size() != 1) { supported = false; break; }
                            output = choices.get(0);
                        } else {
                            int count = choices.get(0).getCount();
                            // The existing constructor plans inventory by item, so reject data-sensitive costs.
                            if (count <= 0 || choices.stream().anyMatch(stack -> stack.getCount() != count || !stack.getComponentsPatch().isEmpty())) {
                                supported = false; break;
                            }
                            inputs.add(new HvkJeiRecipeCatalog.Input(choices, count));
                        }
                    }
                    if (!supported || inputs.isEmpty() || output.isEmpty()) continue;
                    ResourceLocation id = category.getRegistryName(recipe);
                    if (id == null) {
                        String signature = category.getRecipeType().getUid() + "|" + output.save(Minecraft.getInstance().level.registryAccess()) + "|" + inputs;
                        id = ResourceLocation.fromNamespaceAndPath("dealt_force_skills", "jei/" + UUID.nameUUIDFromBytes(signature.getBytes(StandardCharsets.UTF_8)));
                    }
                    entries.add(new HvkJeiRecipeCatalog.Entry(id, output, inputs));
                } catch (RuntimeException | LinkageError ignored) {
                    // A broken third-party category must not prevent native recipe access.
                }
            }
        } catch (RuntimeException | LinkageError ignored) { }
    }
}
