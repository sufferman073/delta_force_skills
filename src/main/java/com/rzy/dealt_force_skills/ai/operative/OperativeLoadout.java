package com.rzy.dealt_force_skills.ai.operative;

import com.rzy.dealt_force_skills.entity.OperativeEntity;
import com.rzy.dealt_force_skills.item.DfsEquipmentItem;
import com.rzy.dealt_force_skills.registry.ModItems;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.api.item.builder.GunItemBuilder;
import com.tacz.guns.api.item.builder.AttachmentItemBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Draws from live TaCZ common indices so server gun packs participate without hardcoded IDs. */
public final class OperativeLoadout {
    private OperativeLoadout() { }
    public static void equip(OperativeEntity bot) {
        OperativeGrowth.initialize(bot);
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST}) {
            List<Item> armor = BuiltInRegistries.ITEM.stream().filter(i -> i instanceof DfsEquipmentItem a && a.getEquipmentSlot() == slot).toList();
            if (!armor.isEmpty() && bot.getItemBySlot(slot).isEmpty()) bot.setItemSlot(slot, new ItemStack(armor.get(bot.getRandom().nextInt(armor.size()))));
        }
        if (!bot.getMainHandItem().isEmpty()) return;
        ItemStack weapon = bot.skills() instanceof NativeUndeadSkills || bot.skills() instanceof NativeLexSkills ? melee(bot) : gun(bot);
        if (weapon.isEmpty()) weapon = melee(bot);
        bot.setItemSlot(EquipmentSlot.MAINHAND, weapon);
    }
    private static ItemStack melee(OperativeEntity bot) {
        Item[] choices = {Items.IRON_SWORD, Items.DIAMOND_SWORD, Items.IRON_AXE, Items.DIAMOND_AXE};
        return new ItemStack(choices[bot.getRandom().nextInt(choices.length)]);
    }
    public static ItemStack gun(OperativeEntity bot) {
        var guns = new ArrayList<>(TimelessAPI.getAllCommonGunIndex());
        guns.removeIf(e -> e.getValue().getGunData() == null || e.getValue().getGunData().getAmmoAmount() <= 0
                || e.getValue().getGunData().getFireModeSet() == null || e.getValue().getGunData().getFireModeSet().isEmpty());
        guns.sort(Comparator.comparing(e -> e.getKey().toString()));
        if (guns.isEmpty()) return ItemStack.EMPTY;
        var selected = guns.get(bot.getRandom().nextInt(guns.size()));
        var data = selected.getValue().getGunData();
        ItemStack stack = GunItemBuilder.create().setId(selected.getKey()).setAmmoCount(data.getAmmoAmount())
                .setAmmoInBarrel(true).setFireMode(data.getFireModeSet().get(0)).build(bot.registryAccess());
        IGun gun = IGun.getIGunOrNull(stack);
        if (gun == null) return ItemStack.EMPTY;
        if (bot.skills() instanceof NativeGhrothSkills)
            com.rzy.dealt_force_skills.character.ghroth.GhrothTaczEnhancement.markArmoryStack(stack, "gun");
        OperativeGun.supplyReserve(stack);
        var attachments = new ArrayList<>(TimelessAPI.getAllCommonAttachmentIndex());
        attachments.sort(Comparator.comparing(e -> e.getKey().toString()));
        boolean installed = false;
        for (AttachmentType type : AttachmentType.values()) {
            if (type == AttachmentType.NONE || !gun.allowAttachmentType(stack, type)) continue;
            if (installed && !bot.getRandom().nextBoolean()) continue;
            List<ItemStack> compatible = new ArrayList<>();
            for (var entry : attachments) {
                ItemStack attachment = AttachmentItemBuilder.create().setId(entry.getKey()).build();
                if (bot.skills() instanceof NativeGhrothSkills)
                    com.rzy.dealt_force_skills.character.ghroth.GhrothTaczEnhancement.markArmoryStack(attachment, "attachment");
                IAttachment api = IAttachment.getIAttachmentOrNull(attachment);
                if (api != null && api.getType(attachment) == type && gun.allowAttachment(stack, attachment)) compatible.add(attachment);
            }
            if (!compatible.isEmpty()) {
                gun.installAttachment(bot.registryAccess(), stack, compatible.get(bot.getRandom().nextInt(compatible.size())));
                installed = true;
            }
        }
        return stack;
    }
}
