package com.rzy.dealt_force_skills.event;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.block.HvkAdvancedSpawnerBlockEntity;
import com.rzy.dealt_force_skills.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID)
public final class HvkAdvancedSpawnerEvents {
    private HvkAdvancedSpawnerEvents() {
    }

    @SubscribeEvent
    public static void onSpawnerBreak(BlockEvent.BreakEvent event) {
        if (event.getPlayer().isCreative() || !event.getState().is(Blocks.SPAWNER)) return;
        ItemStack tool = event.getPlayer().getMainHandItem();
        int silkTouch = EnchantmentHelper.getItemEnchantmentLevel(
                event.getPlayer().level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                        .getOrThrow(Enchantments.SILK_TOUCH), tool);
        if (silkTouch > 0 && event.getPlayer().getRandom().nextFloat() < 0.20F) {
            Block.popResource(event.getPlayer().level(), event.getPos(),
                    new ItemStack(ModItems.HVK_ADVANCED_SPAWNER.get()));
        }
    }

    @SubscribeEvent
    public static void onLivingTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Mob mob) || mob.level().isClientSide || mob.tickCount % 20 != 0) {
            return;
        }
        CompoundTag tag = mob.getPersistentData();
        if (!tag.contains(HvkAdvancedSpawnerBlockEntity.SPAWNER_DIMENSION)) {
            return;
        }
        ResourceLocation dimension = ResourceLocation.tryParse(
                tag.getString(HvkAdvancedSpawnerBlockEntity.SPAWNER_DIMENSION));
        if (dimension == null || !dimension.equals(mob.level().dimension().location())) {
            return;
        }
        int range = Math.max(0, tag.getInt(HvkAdvancedSpawnerBlockEntity.ACTIVITY_RANGE));
        BlockPos origin = new BlockPos(tag.getInt(HvkAdvancedSpawnerBlockEntity.SPAWNER_X),
                tag.getInt(HvkAdvancedSpawnerBlockEntity.SPAWNER_Y),
                tag.getInt(HvkAdvancedSpawnerBlockEntity.SPAWNER_Z));
        double x = origin.getX() + 0.5D;
        double y = origin.getY() + 1.0D;
        double z = origin.getZ() + 0.5D;
        if (mob.distanceToSqr(x, y, z) > (double) range * range) {
            mob.getNavigation().moveTo(x, y, z, 1.15D);
        }
    }
}
