package com.rzy.dealt_force_skills.boss;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;

/**
 * Beacon boss M1911 loadout and TACZ magazine priming (1.21 DataComponents).
 */
public final class BeaconTaczEquipment {
    private static final ResourceLocation TACZ_GUN_ITEM =
            ResourceLocation.fromNamespaceAndPath("tacz", "modern_kinetic_gun");
    private static final String GUN_ID_TAG = "GunId";
    private static final String GUN_FIRE_MODE_TAG = "GunFireMode";
    private static final String GUN_CURRENT_AMMO_COUNT_TAG = "GunCurrentAmmoCount";
    private static final String GUN_HAS_BULLET_IN_BARREL_TAG = "HasBulletInBarrel";
    private static final String GUN_DUMMY_AMMO_TAG = "DummyAmmo";
    private static final String GUN_MAX_DUMMY_AMMO_TAG = "MaxDummyAmmo";
    private static final int MAGAZINE_SIZE = 12;

    private BeaconTaczEquipment() {
    }

    public static ItemStack createM1911() {
        return buildM1911Manually();
    }

    public static boolean isTaczGun(ItemStack stack) {
        CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = cd != null ? cd.copyTag() : null;
        return tag != null && tag.contains(GUN_ID_TAG);
    }

    public static ResourceLocation gunId(ItemStack stack) {
        CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = cd != null ? cd.copyTag() : null;
        return tag == null ? null : ResourceLocation.tryParse(tag.getString(GUN_ID_TAG));
    }

    public static void primeForShot(ItemStack stack) {
        if (stack.isEmpty() || !isTaczGun(stack)) {
            return;
        }
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putInt(GUN_CURRENT_AMMO_COUNT_TAG, Math.max(tag.getInt(GUN_CURRENT_AMMO_COUNT_TAG), MAGAZINE_SIZE));
        tag.putBoolean(GUN_HAS_BULLET_IN_BARREL_TAG, true);
        tag.putInt(GUN_DUMMY_AMMO_TAG, Math.max(tag.getInt(GUN_DUMMY_AMMO_TAG), 9999));
        tag.putInt(GUN_MAX_DUMMY_AMMO_TAG, Math.max(tag.getInt(GUN_MAX_DUMMY_AMMO_TAG), 9999));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private static ItemStack buildM1911Manually() {
        Item item = BuiltInRegistries.ITEM.get(TACZ_GUN_ITEM);
        if (item == null || item == Items.AIR) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = new ItemStack(item);
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putString(GUN_ID_TAG, "tacz:m1911");
        tag.putInt(GUN_CURRENT_AMMO_COUNT_TAG, MAGAZINE_SIZE);
        tag.putBoolean(GUN_HAS_BULLET_IN_BARREL_TAG, true);
        tag.putString(GUN_FIRE_MODE_TAG, "SEMI");
        tag.putInt(GUN_DUMMY_AMMO_TAG, 9999);
        tag.putInt(GUN_MAX_DUMMY_AMMO_TAG, 9999);
        tag.put("AttachmentEXTENDED_MAG", attachment("tacz:light_extended_mag_3"));
        tag.put("AttachmentMUZZLE", attachment("tacz:muzzle_silencer_mirage"));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    private static CompoundTag attachment(String attachmentId) {
        CompoundTag stack = new CompoundTag();
        stack.putInt("count", 1);
        stack.putString("id", "tacz:attachment");
        CompoundTag components = new CompoundTag();
        CompoundTag customData = new CompoundTag();
        customData.putString("AttachmentId", attachmentId);
        components.put("minecraft:custom_data", customData);
        stack.put("components", components);
        return stack;
    }
}