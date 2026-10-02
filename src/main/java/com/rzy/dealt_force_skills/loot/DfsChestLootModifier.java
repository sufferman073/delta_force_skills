package com.rzy.dealt_force_skills.loot;

import com.rzy.dealt_force_skills.config.DealtForceConfig;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.rzy.dealt_force_skills.item.DfsEquipmentItem;
import com.rzy.dealt_force_skills.item.DfsEquipmentItem.Faction;
import com.rzy.dealt_force_skills.item.EternalLoveBlessingItem;
import com.rzy.dealt_force_skills.registry.ModItems;
import com.rzy.dealt_force_skills.registry.ModGameRules;
import com.rzy.dealt_force_skills.registry.ModLootModifiers;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.LootModifier;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class DfsChestLootModifier extends LootModifier {
    public static final MapCodec<DfsChestLootModifier> CODEC = RecordCodecBuilder.mapCodec(instance ->
            codecStart(instance).apply(instance, DfsChestLootModifier::new));

    private static volatile float SPECIAL_EQUIPMENT_SUPPLY_CHANCE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("SPECIAL_EQUIPMENT_SUPPLY_CHANCE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.floatValue(
      "loot.dfs_chest_loot_modifier.special_equipment_supply_chance", 0.03F
   ));
    private static volatile float PROGRAMMABLE_PROCESSOR_BASE_CHANCE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("PROGRAMMABLE_PROCESSOR_BASE_CHANCE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.floatValue(
      "loot.dfs_chest_loot_modifier.programmable_processor_base_chance", 0.06F
   ));
    private static volatile float PROGRAMMABLE_PROCESSOR_LUCK_BONUS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("PROGRAMMABLE_PROCESSOR_LUCK_BONUS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.floatValue(
      "loot.dfs_chest_loot_modifier.programmable_processor_luck_bonus", 0.04F
   ));
    private static volatile float ETERNAL_LOVE_BLESSING_CHANCE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("ETERNAL_LOVE_BLESSING_CHANCE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.floatValue("loot.dfs_chest_loot_modifier.eternal_love_blessing_chance", 1.0E-4F));
    public DfsChestLootModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected @NotNull ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        ResourceLocation lootTableId = context.getQueriedLootTableId();
        if (lootTableId == null || !lootTableId.getPath().startsWith("chests/")) {
            return generatedLoot;
        }

        Entity entity = context.getParamOrNull(LootContextParams.THIS_ENTITY);
        if (entity instanceof Player player) {
            HvkTreasureProgress.recordOpenedChestLoot(player);
            if (hasGtiEquipment(player)) {
                for (ItemStack stack : new ArrayList<>(generatedLoot)) {
                    if (!stack.isEmpty()) {
                        generatedLoot.add(stack.copy());
                    }
                }
            }
        }

        if (context.getRandom().nextFloat() < SPECIAL_EQUIPMENT_SUPPLY_CHANCE) {
            generatedLoot.add(new ItemStack(ModItems.SPECIAL_EQUIPMENT_SUPPLY.get()));
        }

        float processorChance = Math.min(1.0F, PROGRAMMABLE_PROCESSOR_BASE_CHANCE
                + Math.max(0, Mth.floor(context.getLuck())) * PROGRAMMABLE_PROCESSOR_LUCK_BONUS);
        if (context.getRandom().nextFloat() < processorChance) {
            generatedLoot.add(new ItemStack(ModItems.PROGRAMMABLE_PROCESSOR.get(), 1 + context.getRandom().nextInt(3)));
        }

        if (context.getRandom().nextFloat() < ETERNAL_LOVE_BLESSING_CHANCE) {
            generatedLoot.add(new ItemStack(ModItems.ETERNAL_LOVE_BLESSING.get()));
            if (entity instanceof ServerPlayer player) {
                EternalLoveBlessingItem.playObtainSound(player);
            }
        }

        // Independent roll, sharing the supply probability and its one-item yield.
        if (context.getRandom().nextFloat() < SPECIAL_EQUIPMENT_SUPPLY_CHANCE) {
            generatedLoot.add(new ItemStack(ModItems.BLUEPRINT.get()));
        }

        return generatedLoot;
    }

    private static boolean hasGtiEquipment(Player player) {
        if (!ModGameRules.areArmorSpecialsEnabled(player)) {
            return false;
        }
        DfsEquipmentItem.Profile chest = DfsEquipmentItem.profile(player.getItemBySlot(EquipmentSlot.CHEST));
        if (chest != null && chest.faction() == Faction.GTI) {
            return true;
        }
        DfsEquipmentItem.Profile head = DfsEquipmentItem.profile(player.getItemBySlot(EquipmentSlot.HEAD));
        return head != null && head.faction() == Faction.GTI;
    }

    @Override
    public MapCodec<DfsChestLootModifier> codec() {
        return ModLootModifiers.CHEST_LOOT.get();
    }
}
