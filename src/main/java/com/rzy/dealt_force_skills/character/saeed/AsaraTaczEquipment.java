package com.rzy.dealt_force_skills.character.saeed;

import com.rzy.dealt_force_skills.entity.AsaraTrooperRole;
import com.rzy.dealt_force_skills.shop.TaczShopCatalog;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.builder.AmmoItemBuilder;
import com.tacz.guns.resource.index.CommonGunIndex;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class AsaraTaczEquipment {
    private AsaraTaczEquipment() {
    }

    public static ItemStack randomGun(AsaraTrooperRole role, RandomSource random, HolderLookup.Provider registries) {
        List<TaczShopCatalog.Entry> matches = new ArrayList<>();
        for (TaczShopCatalog.Entry entry : TaczShopCatalog.entries()) {
            if (entry.type() == TaczShopCatalog.Type.GUN && matchesRole(role, entry.id())) {
                matches.add(entry);
            }
        }
        if (matches.isEmpty()) {
            return SaeedTaczEquipment.mainHand(role.guardType());
        }
        ItemStack gun = matches.get(random.nextInt(matches.size())).preview().copy();
        gun.setCount(1);
        installRandomAttachments(gun, random, registries);
        SaeedTaczEquipment.primeForShot(role.guardType(), gun);
        return gun;
    }

    public static List<ItemStack> matchingAmmo(ItemStack gun, int count) {
        ResourceLocation gunId = SaeedTaczEquipment.gunId(gun);
        Optional<CommonGunIndex> index = gunId == null ? Optional.empty() : TimelessAPI.getCommonGunIndex(gunId);
        if (index.isEmpty() || index.get().getGunData().getAmmoId() == null || count <= 0) {
            return List.of();
        }
        ResourceLocation ammoId = index.get().getGunData().getAmmoId();
        List<ItemStack> result = new ArrayList<>();
        int remaining = count;
        while (remaining > 0) {
            int stackCount = Math.min(64, remaining);
            ItemStack ammo = AmmoItemBuilder.create().setId(ammoId).setCount(stackCount).build();
            if (ammo.isEmpty()) {
                break;
            }
            result.add(ammo);
            remaining -= stackCount;
        }
        return List.copyOf(result);
    }

    public static void clearReserveAmmo(ItemStack gun) { SaeedTaczEquipment.clearReserveAmmo(gun); }

    private static void installRandomAttachments(ItemStack gun, RandomSource random, HolderLookup.Provider registries) {
        IGun gunApi = IGun.getIGunOrNull(gun);
        if (gunApi == null) {
            return;
        }
        List<ItemStack> candidates = new ArrayList<>();
        for (TaczShopCatalog.Entry entry : TaczShopCatalog.entries()) {
            if (entry.type() == TaczShopCatalog.Type.ATTACHMENT) {
                candidates.add(entry.preview().copy());
            }
        }
        int wanted = 1 + random.nextInt(4);
        for (int installed = 0; installed < wanted && !candidates.isEmpty();) {
            ItemStack attachment = candidates.remove(random.nextInt(candidates.size()));
            if (gunApi.allowAttachment(gun, attachment)) {
                gunApi.installAttachment(registries, gun, attachment);
                installed++;
            }
        }
    }

    private static boolean matchesRole(AsaraTrooperRole role, ResourceLocation id) {
        CommonGunIndex index = TimelessAPI.getCommonGunIndex(id).orElse(null);
        String type = index == null || index.getType() == null ? "" : index.getType().toLowerCase(Locale.ROOT);
        String value = (type + " " + id).toLowerCase(Locale.ROOT);
        return switch (role) {
            case MACHINE_GUNNER -> contains(value, "machine", "lmg", "mg", "m249", "minigun");
            case SNIPER, SENTRY_SNIPER -> contains(value, "sniper", "marksman", "m700", "m24", "awm", "awp", "svd", "m200", "barrett");
            case ROCKET_TROOPER -> contains(value, "rocket", "rpg", "launcher", "bazooka");
            case GUARD_SOLDIER -> contains(value, "shotgun", "scatter", "m870", "m1014", "aa12", "saiga");
            case SHIELD_TROOPER -> contains(value, "smg", "submachine", "pistol", "mp5", "ump", "vector");
            case FLAMETHROWER -> contains(value, "flame", "fire", "thrower", "pistol", "handgun");
        };
    }

    private static boolean contains(String value, String... needles) {
        for (String needle : needles) {
            if (value.contains(needle)) {
                return true;
            }
        }
        return false;
    }
}
