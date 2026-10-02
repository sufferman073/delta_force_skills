package com.rzy.dealt_force_skills.boss;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;

public final class AcceleratedTroopsEquipment {
    private static final ResourceLocation TACZ_GUN_ITEM =
            ResourceLocation.fromNamespaceAndPath("tacz", "modern_kinetic_gun");
    private static final ResourceLocation TACZ_AMMO_BOX_ITEM =
            ResourceLocation.fromNamespaceAndPath("tacz", "ammo_box");
    private AcceleratedTroopsEquipment() {
    }

    public static ItemStack createM4A1() {
        return createM4A1(true);
    }

    public static ItemStack createDroppedM4A1() {
        return createM4A1(false);
    }

    private static ItemStack createM4A1(boolean unlimitedReserve) {
        Item item = BuiltInRegistries.ITEM.get(TACZ_GUN_ITEM);
        if (item == null || item == Items.AIR) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = new ItemStack(item);
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putString("GunId", "tacz:m4a1");
        tag.putString("GunFireMode", "AUTO");
        putAttachment(tag, "AttachmentEXTENDED_MAG", "tacz:ammo_mod_i");
        putAttachment(tag, "AttachmentGRIP", "tacz:grip_vertical_ranger");
        putAttachment(tag, "AttachmentLASER", "tacz:laser_lopro");
        putAttachment(tag, "AttachmentMUZZLE", "tacz:muzzle_brake_cyclone_d2");
        putAttachment(tag, "AttachmentSCOPE", "tacz:sight_coyote");
        putAttachment(tag, "AttachmentSTOCK", "tacz:stock_carbon_bone_c5");
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        prepareM4A1(stack, unlimitedReserve);
        return stack;
    }

    public static ItemStack createAmmoBox() {
        Item item = BuiltInRegistries.ITEM.get(TACZ_AMMO_BOX_ITEM);
        if (item == null || item == Items.AIR) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = new ItemStack(item);
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putInt("AmmoCount", 114);
        tag.putString("AmmoId", "tacz:556x45");
        tag.putInt("Level", 2);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    public static void primeM4A1(ItemStack stack) {
        prepareM4A1(stack, true);
    }

    private static void prepareM4A1(ItemStack stack, boolean unlimitedReserve) {
        if (stack.isEmpty()) {
            return;
        }
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putString("GunId", "tacz:m4a1");
        tag.putString("GunFireMode", "AUTO");
        tag.putInt("GunCurrentAmmoCount", Math.max(60, tag.getInt("GunCurrentAmmoCount")));
        tag.putBoolean("HasBulletInBarrel", true);
        if (unlimitedReserve) {
            tag.putInt("DummyAmmo", Math.max(9999, tag.getInt("DummyAmmo")));
            tag.putInt("MaxDummyAmmo", Math.max(9999, tag.getInt("MaxDummyAmmo")));
        } else {
            tag.putInt("DummyAmmo", 114);
            tag.putInt("MaxDummyAmmo", 114);
        }
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private static void putAttachment(CompoundTag gunTag, String slotKey, String attachmentId) {
        CompoundTag attachment = new CompoundTag();
        attachment.putInt("count", 1);
        attachment.putString("id", "tacz:attachment");
        CompoundTag components = new CompoundTag();
        CompoundTag customData = new CompoundTag();
        customData.putString("AttachmentId", attachmentId);
        components.put("minecraft:custom_data", customData);
        attachment.put("components", components);
        gunTag.put(slotKey, attachment);
    }
}
