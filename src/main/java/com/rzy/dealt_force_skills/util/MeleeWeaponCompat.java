package com.rzy.dealt_force_skills.util;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import java.util.Locale;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.core.registries.BuiltInRegistries;

public final class MeleeWeaponCompat {
    public static final TagKey<Item> MELEE_WEAPONS = TagKey.create(
            Registries.ITEM, ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "melee_weapons"));
    private static final String[] LES_RAISINS_NAMESPACE_PARTS = {
            "lrtactical", "lesraisins", "raisins", "tactical_equipement", "tactical_equipment", "lrte"
    };
    private static final String[] MELEE_KEYWORDS = {
            "sword", "axe", "knife", "dagger", "blade", "machete", "katana", "saber", "sabre",
            "bayonet", "baton", "hammer", "club", "rapier", "halberd", "spear", "lance", "trident", "crowbar",
            "melee", "karambit", "kukri", "khukuri", "tomahawk", "hatchet", "shovel", "pickaxe", "m9"
    };

    private MeleeWeaponCompat() {
    }

    public static boolean isMeleeWeapon(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (stack.is(MELEE_WEAPONS) || stack.is(ItemTags.SWORDS) || stack.is(ItemTags.AXES) || stack.is(Items.TRIDENT)) {
            return true;
        }
        Item item = stack.getItem();
        if (item instanceof SwordItem || item instanceof AxeItem || item instanceof TridentItem || item instanceof TieredItem) {
            return true;
        }
        return isKnownLesRaisinsMelee(item);
    }

    public static boolean isLesRaisinsMelee(ItemStack stack) {
        return stack != null && !stack.isEmpty() && isKnownLesRaisinsMelee(stack.getItem());
    }

    private static boolean isKnownLesRaisinsMelee(Item item) {
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(item);
        if (key == null || !containsAny(key.getNamespace().toLowerCase(Locale.ROOT), LES_RAISINS_NAMESPACE_PARTS)) {
            return false;
        }
        String path = key.getPath().toLowerCase(Locale.ROOT);
        String className = item.getClass().getName().toLowerCase(Locale.ROOT);
        return containsAny(path, MELEE_KEYWORDS) || containsAny(className, MELEE_KEYWORDS);
    }

    private static boolean containsAny(String value, String[] parts) {
        for (String part : parts) {
            if (value.contains(part)) {
                return true;
            }
        }
        return false;
    }
}
