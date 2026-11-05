package com.rzy.dealt_force_skills.workbench;

import net.minecraft.world.item.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.*;
import java.util.*;

public final class WorkbenchPlatform {
    private WorkbenchPlatform() { }

    public static int crossbowTicks(ItemStack stack) {
        var value=new org.apache.commons.lang3.mutable.MutableFloat(1.25f);
        net.minecraft.world.item.enchantment.EnchantmentHelper.runIterationOnItem(stack,(holder,level)->
            holder.value().modifyCrossbowChargeTime(net.minecraft.util.RandomSource.create(0L),level,value));
        return Math.max(0,net.minecraft.util.Mth.floor(value.floatValue()*20));
    }
    public static boolean same(ItemStack a,ItemStack b){return ItemStack.isSameItemSameComponents(a,b);}
    public record Stock(int slot,ItemStack stack) { }
    public static java.util.List<Stock> warehouse(net.minecraft.server.level.ServerPlayer player) {
        var items=mccfk.DeltaForce.java.data.DfWarehouseManager.loadWarehouse(player);var out=new java.util.ArrayList<Stock>();
        int capacity=com.rzy.dealt_force_skills.shop.SpecialOperationsBaseManager.warehouseCapacity(player);
        for(int i=0;i<Math.min(capacity,items.size());i++)if(!items.get(i).isEmpty())out.add(new Stock(i,items.get(i)));return out;
    }
    public static ItemStack extract(net.minecraft.server.level.ServerPlayer player,int slot,ItemStack wanted) {
        return mccfk.DeltaForce.java.data.DfWarehouseManager.extractMatching(player,slot,1,s->same(s,wanted));
    }
    public static java.util.List<net.minecraft.world.item.crafting.Ingredient> repairIngredients(net.minecraft.server.level.ServerPlayer player,ItemStack stack) {
        var recipes=new java.util.ArrayList<>(player.serverLevel().getRecipeManager().getRecipes());
        recipes.sort(java.util.Comparator.comparing(r->r.id().toString()));
        for(var holder:recipes) {
            var recipe=holder.value();
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
    public static boolean taczLoaded(){return net.neoforged.fml.ModList.get().isLoaded("tacz");}
    public static int enchantment(ItemStack stack,String name) {
        for(var entry:net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantmentsForCrafting(stack).entrySet())
            if(entry.getKey().unwrapKey().map(key->key.location().getPath().equals(name)).orElse(false))return entry.getIntValue();
        return 0;
    }
    public static int harvestLevel(ItemStack stack) {
        return WorkbenchHarvest.baseLevel(stack);
    }
    public static double attribute(ItemStack stack,WorkbenchStat stat) {
        net.minecraft.core.Holder<Attribute> attribute=switch(stat) {
            case DAMAGE -> Attributes.ATTACK_DAMAGE;
            case ATTACK_SPEED -> Attributes.ATTACK_SPEED;
            case ENTITY_REACH -> Attributes.ENTITY_INTERACTION_RANGE;
            case BLOCK_REACH -> Attributes.BLOCK_INTERACTION_RANGE;
            case ARMOR -> Attributes.ARMOR;
            case TOUGHNESS -> Attributes.ARMOR_TOUGHNESS;
            case KNOCKBACK -> Attributes.KNOCKBACK_RESISTANCE;
            default -> throw new IllegalArgumentException();
        };
        double base=switch(stat){case DAMAGE->1;case ATTACK_SPEED->4;case ENTITY_REACH->3;case BLOCK_REACH->4.5;default->0;};
        EquipmentSlot slot=stack.getItem() instanceof ArmorItem armor?armor.getType().getSlot():EquipmentSlot.MAINHAND;
        List<AttributeModifier> modifiers=new ArrayList<>();
        stack.getAttributeModifiers().forEach(slot,(type,mod)->{if(type.equals(attribute))modifiers.add(mod);});
        double value=base;
        for(var mod:modifiers)if(mod.operation()==AttributeModifier.Operation.ADD_VALUE)value+=mod.amount();
        double added=value;
        for(var mod:modifiers)if(mod.operation()==AttributeModifier.Operation.ADD_MULTIPLIED_BASE)added+=value*mod.amount();
        for(var mod:modifiers)if(mod.operation()==AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)added*=1+mod.amount();
        return Math.max(0,added);
    }
}
