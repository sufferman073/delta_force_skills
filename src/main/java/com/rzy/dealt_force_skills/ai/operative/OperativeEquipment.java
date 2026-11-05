package com.rzy.dealt_force_skills.ai.operative;

import com.rzy.dealt_force_skills.entity.OperativeEntity;
import com.rzy.dealt_force_skills.item.DfsUseItem;
import com.rzy.dealt_force_skills.util.MeleeWeaponCompat;
import com.tacz.guns.api.item.IGun;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;

public final class OperativeEquipment {
    private OperativeEquipment() { }
    public static InteractionResult interact(OperativeEntity bot, Player player, InteractionHand hand) {
        if (!player.getUUID().equals(bot.getOwnerUUID()) || !bot.isAlive() || OperativeSupport.downed(bot)) return InteractionResult.PASS;
        ItemStack held = player.getItemInHand(hand);
        EquipmentSlot slot = held.getItem() instanceof ArmorItem armor ? armor.getEquipmentSlot()
                : IGun.getIGunOrNull(held) != null || MeleeWeaponCompat.isMeleeWeapon(held) ? EquipmentSlot.MAINHAND : null;
        if (slot != null) {
            if (slot == EquipmentSlot.MAINHAND && bot.skills() instanceof NativeChamberSkills chamber
                    && chamber.equipped() != com.rzy.dealt_force_skills.character.chamber.ChamberGunKind.NONE) return InteractionResult.FAIL;
            if (slot == EquipmentSlot.MAINHAND && IGun.getIGunOrNull(held) != null
                    && (bot.skills() instanceof NativeUndeadSkills || bot.skills() instanceof NativeLexSkills)) return InteractionResult.FAIL;
            if (!bot.level().isClientSide) {
                bot.skills().cancel(bot); bot.gun().cancel(bot);
                ItemStack old = bot.getItemBySlot(slot);
                OperativeGun.preparePlayerTransfer(old);
                bot.setItemSlot(slot, held.copyWithCount(1));
                if (!player.getAbilities().instabuild) held.shrink(1);
                if (!old.isEmpty() && !player.getInventory().add(old)) player.drop(old, false);
            }
            return InteractionResult.sidedSuccess(bot.level().isClientSide);
        }
        if (held.getItem() instanceof DfsUseItem medicine) {
            if (bot.level().isClientSide) return InteractionResult.SUCCESS;
            ItemStack dose = held.copy();
            if (!medicine.administerToOperative(dose, bot)) return InteractionResult.FAIL;
            if (!player.getAbilities().instabuild) player.setItemInHand(hand, dose);
            return InteractionResult.CONSUME;
        }
        if (held.getFoodProperties(bot) != null || held.getItem() instanceof PotionItem || held.getItem() instanceof MilkBucketItem) {
            if (!bot.level().isClientSide) {
                ItemStack serving = held.copyWithCount(1);
                var food = held.getFoodProperties(bot);
                ItemStack remainder = serving.finishUsingItem(bot.level(), bot);
                // Vanilla player-only container handling does not consume mob servings.
                if (held.getItem() instanceof MilkBucketItem) remainder = new ItemStack(Items.BUCKET);
                else if (held.getItem() instanceof PotionItem) remainder = new ItemStack(Items.GLASS_BOTTLE);
                else if (remainder.is(held.getItem())) remainder = ItemStack.EMPTY;
                if (food != null) bot.heal(Math.max(1, food.nutrition() * .5F));
                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                    if (!remainder.isEmpty() && !player.getInventory().add(remainder)) player.drop(remainder, false);
                }
            }
            return InteractionResult.sidedSuccess(bot.level().isClientSide);
        }
        return InteractionResult.PASS;
    }
}
