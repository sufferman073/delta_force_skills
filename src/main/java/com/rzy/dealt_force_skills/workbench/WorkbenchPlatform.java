package com.rzy.dealt_force_skills.workbench;

import net.minecraft.world.item.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.*;
import java.util.*;

public final class WorkbenchPlatform {
    private WorkbenchPlatform() { }

    public static int crossbowTicks(ItemStack stack) {
        return Math.max(0,25-5*enchantment(stack,"quick_charge"));
    }
    public static boolean same(ItemStack a,ItemStack b){return ItemStack.isSameItemSameTags(a,b);}
    public record Stock(int slot,ItemStack stack) { }
    public static java.util.List<Stock> warehouse(net.minecraft.server.level.ServerPlayer player) {
        return com.rzy.dealt_force_skills.shop.SpecialOperationsBaseManager.warehouseEntries(player).stream().map(e->new Stock(e.slot(),e.stack())).toList();
    }
    public static ItemStack extract(net.minecraft.server.level.ServerPlayer player,int slot,ItemStack wanted) {
        return com.rzy.dealt_force_skills.shop.SpecialOperationsBaseManager.extractWarehouse(player,slot,1);
    }
    public static java.util.List<net.minecraft.world.item.crafting.Ingredient> repairIngredients(net.minecraft.server.level.ServerPlayer player,ItemStack stack) {
        var recipes=new java.util.ArrayList<>(player.serverLevel().getRecipeManager().getRecipes());
        recipes.sort(java.util.Comparator.comparing(r->r.getId().toString()));
        for(var recipe:recipes) {
            if(recipe.getType()!=net.minecraft.world.item.crafting.RecipeType.CRAFTING&&recipe.getType()!=net.minecraft.world.item.crafting.RecipeType.SMITHING)continue;
            if(!recipe.getResultItem(player.level().registryAccess()).is(stack.getItem()))continue;
            var result=recipe.getIngredients().stream().filter(i->!i.isEmpty()&&!i.test(stack)).toList();
            if(!result.isEmpty())return result;
        }
        java.util.List<ItemStack> materials=new java.util.ArrayList<>();
        for(var item:net.minecraft.core.registries.BuiltInRegistries.ITEM) {
            var material=new ItemStack(item);if(stack.getItem().isValidRepairItem(stack,material))materials.add(material);
        }
        return materials.isEmpty()?java.util.List.of():java.util.List.of(net.minecraft.world.item.crafting.Ingredient.of(materials.stream()));
    }
    public static boolean taczLoaded(){return net.minecraftforge.fml.ModList.get().isLoaded("tacz");}
    public static int enchantment(ItemStack stack,String name) {
        for(var entry:net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantments(stack).entrySet())
            if(net.minecraft.core.registries.BuiltInRegistries.ENCHANTMENT.getKey(entry.getKey()).getPath().equals(name))return entry.getValue();
        return 0;
    }
    public static int harvestLevel(ItemStack stack) {
        return WorkbenchHarvest.baseLevel(stack);
    }
    public static double attribute(ItemStack stack,WorkbenchStat stat) {
        Attribute attribute=switch(stat) {
            case DAMAGE -> Attributes.ATTACK_DAMAGE;
            case ATTACK_SPEED -> Attributes.ATTACK_SPEED;
            case ENTITY_REACH -> net.minecraftforge.common.ForgeMod.ENTITY_REACH.get();
            case BLOCK_REACH -> net.minecraftforge.common.ForgeMod.BLOCK_REACH.get();
            case ARMOR -> Attributes.ARMOR;
            case TOUGHNESS -> Attributes.ARMOR_TOUGHNESS;
            case KNOCKBACK -> Attributes.KNOCKBACK_RESISTANCE;
            default -> throw new IllegalArgumentException();
        };
        double base=switch(stat){case DAMAGE->1;case ATTACK_SPEED->4;case ENTITY_REACH->3;case BLOCK_REACH->4.5;default->0;};
        EquipmentSlot slot=stack.getItem() instanceof ArmorItem armor?armor.getType().getSlot():EquipmentSlot.MAINHAND;
        var modifiers=stack.getAttributeModifiers(slot).get(attribute);
        double value=base;
        for(var mod:modifiers)if(mod.getOperation()==AttributeModifier.Operation.ADDITION)value+=mod.getAmount();
        double added=value;
        for(var mod:modifiers)if(mod.getOperation()==AttributeModifier.Operation.MULTIPLY_BASE)added+=value*mod.getAmount();
        for(var mod:modifiers)if(mod.getOperation()==AttributeModifier.Operation.MULTIPLY_TOTAL)added*=1+mod.getAmount();
        return Math.max(0,added);
    }
}
