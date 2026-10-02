package com.rzy.dealt_force_skills.character.chamber;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.core.registries.BuiltInRegistries;

public final class ChamberTaczEquipment {
    private static final ResourceLocation TACZ_GUN_ITEM = ResourceLocation.fromNamespaceAndPath("tacz", "modern_kinetic_gun");
    private static final String GUN_ID = "GunId";
    private static final String FIRE_MODE = "GunFireMode";
    private static final String AMMO = "GunCurrentAmmoCount";
    private static final String BARREL = "HasBulletInBarrel";

    private ChamberTaczEquipment() {
    }

    public static ItemStack create(ChamberGunKind kind) {
        Item item = BuiltInRegistries.ITEM.get(TACZ_GUN_ITEM);
        if (item == null || item == Items.AIR) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = new ItemStack(item);
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (kind == ChamberGunKind.HEADHUNTER) {
            tag.putString(GUN_ID, "tacz:deagle_golden");
            tag.putInt(AMMO, 7);
            tag.putBoolean(BARREL, true);
            tag.putString(FIRE_MODE, "SEMI");
            tag.put("AttachmentEXTENDED_MAG", attachment("tacz:ammo_mod_fmj"));
            tag.put("AttachmentLASER", attachment("tacz:laser_compact"));
            tag.put("AttachmentMUZZLE", attachment("tacz:deagle_golden_long_barrel"));
            tag.put("AttachmentSCOPE", attachment("tacz:sight_t1"));
        } else if (kind == ChamberGunKind.TOUR_DE_FORCE) {
            tag.putString(GUN_ID, "tacz:m95");
            tag.putInt(AMMO, 4);
            tag.putBoolean(BARREL, true);
            tag.putString(FIRE_MODE, "SEMI");
            tag.put("AttachmentEXTENDED_MAG", attachment("tacz:ammo_mod_fmj"));
            tag.put("AttachmentMUZZLE", attachment("tacz:muzzle_silencer_vulture"));
            tag.put("AttachmentSCOPE", attachment("tacz:scope_mk5hd"));
        } else {
            return ItemStack.EMPTY;
        }
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return ChamberTaczEnhancement.mark(stack, kind);
    }

    private static CompoundTag attachment(String id) {
        CompoundTag stack = new CompoundTag();
        stack.putInt("count", 1);
        stack.putString("id", "tacz:attachment");
        CompoundTag components = new CompoundTag();
        CompoundTag customData = new CompoundTag();
        customData.putString("AttachmentId", id);
        components.put("minecraft:custom_data", customData);
        stack.put("components", components);
        return stack;
    }
}
