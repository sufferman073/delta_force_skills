package com.rzy.dealt_force_skills.character.chamber;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class ChamberTaczEnhancement {
    private static final String WEAPON_TAG = "dealt_force_skills.chamber_weapon";
    private static final String KIND_TAG = "dealt_force_skills.chamber_weapon_kind";
    private static final String GUN_ID_TAG = "GunId";
    private static final String ATTACHMENT_LOCK = "AttachmentLock";
    private static final double HEADSHOT_MULTIPLIER = 5.0D;

    private ChamberTaczEnhancement() {
    }

    public static ItemStack mark(ItemStack stack, ChamberGunKind kind) {
        if (stack.isEmpty()) {
            return stack;
        }
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putBoolean(WEAPON_TAG, true);
        tag.putString(KIND_TAG, kind.name());
        tag.putBoolean(ATTACHMENT_LOCK, true);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    public static boolean isChamberWeapon(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBoolean(WEAPON_TAG);
    }

    public static ChamberGunKind kind(ItemStack stack) {
        if (!isChamberWeapon(stack)) {
            return ChamberGunKind.NONE;
        }
        try {
            return ChamberGunKind.valueOf(stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString(KIND_TAG));
        } catch (RuntimeException ignored) {
            return ChamberGunKind.NONE;
        }
    }

    public static float headshotMultiplier(float original, ItemStack stack) {
        return isChamberWeapon(stack) ? (float) (original * HEADSHOT_MULTIPLIER) : original;
    }

    public static boolean ignoresArmor(ItemStack stack) {
        return kind(stack) == ChamberGunKind.TOUR_DE_FORCE;
    }

    public static ResourceLocation gunId(ItemStack stack) {
        CustomData customData = stack == null ? null : stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = customData != null ? customData.copyTag() : null;
        return tag == null ? null : ResourceLocation.tryParse(tag.getString(GUN_ID_TAG));
    }
}
