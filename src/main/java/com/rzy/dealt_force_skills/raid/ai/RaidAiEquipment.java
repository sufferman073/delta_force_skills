package com.rzy.dealt_force_skills.raid.ai;

import com.rzy.dealt_force_skills.ai.operative.OperativeGun;
import com.rzy.dealt_force_skills.entity.OperativeEntity;
import com.rzy.dealt_force_skills.item.DfsEquipmentItem;
import com.rzy.dealt_force_skills.raid.rules.RaidAiRules;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.api.item.builder.AttachmentItemBuilder;
import com.tacz.guns.api.item.builder.GunItemBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.*;
import java.util.random.RandomGenerator;

/** Raid-only equipment selection; normal companion loadouts are untouched. */
public final class RaidAiEquipment {
    private RaidAiEquipment() { }

    public static void equip(OperativeEntity actor, RaidAiRules.Equipment plan, RandomGenerator random) {
        Map<EquipmentSlot, ItemStack> prepared = new EnumMap<>(EquipmentSlot.class);
        if (plan.fullNetheriteArmor()) {
            prepared.put(EquipmentSlot.HEAD, fullyEnchanted(actor, Items.NETHERITE_HELMET));
            prepared.put(EquipmentSlot.CHEST, fullyEnchanted(actor, Items.NETHERITE_CHESTPLATE));
            prepared.put(EquipmentSlot.LEGS, fullyEnchanted(actor, Items.NETHERITE_LEGGINGS));
            prepared.put(EquipmentSlot.FEET, fullyEnchanted(actor, Items.NETHERITE_BOOTS));
            prepared.put(EquipmentSlot.MAINHAND, fullyEnchanted(actor, Items.NETHERITE_AXE));
        } else {
            prepared.put(EquipmentSlot.HEAD, armor(EquipmentSlot.HEAD, plan.helmetTier(), random));
            prepared.put(EquipmentSlot.CHEST, armor(EquipmentSlot.CHEST, plan.chestTier(), random));
            prepared.put(EquipmentSlot.MAINHAND, gun(actor, plan.weapon(), random));
        }
        // Prepare every item first: a missing gun category must not leave a partially equipped actor.
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            actor.setItemSlot(slot, prepared.getOrDefault(slot, ItemStack.EMPTY));
            actor.setDropChance(slot, 0.0F); // The raid death crate owns the complete equipment transfer.
        }
    }

    private static ItemStack armor(EquipmentSlot slot, int tier, RandomGenerator random) {
        List<Item> candidates = BuiltInRegistries.ITEM.stream().filter(item -> item instanceof DfsEquipmentItem armor
                && armor.getEquipmentSlot() == slot && armor.profile().quality().tier() == tier)
                .sorted(Comparator.comparing(item -> BuiltInRegistries.ITEM.getKey(item).toString())).toList();
        if (candidates.isEmpty()) throw new IllegalStateException("Missing DFS armor tier " + tier + " for " + slot);
        return new ItemStack(candidates.get(random.nextInt(candidates.size())));
    }

    public static ItemStack gun(OperativeEntity actor, RaidAiRules.WeaponRole role, RandomGenerator random) {
        var candidates = new ArrayList<>(TimelessAPI.getAllCommonGunIndex());
        candidates.removeIf(entry -> !matches(role, entry.getValue().getType()) || entry.getValue().getGunData() == null
                || entry.getValue().getGunData().getAmmoAmount() <= 0 || entry.getValue().getGunData().getFireModeSet() == null
                || entry.getValue().getGunData().getFireModeSet().isEmpty());
        candidates.sort(Comparator.comparing(entry -> entry.getKey().toString()));
        if (candidates.isEmpty()) throw new IllegalStateException("No usable TaCZ gun in required raid category " + role);
        var selected = candidates.get(random.nextInt(candidates.size()));
        var data = selected.getValue().getGunData();
        ItemStack stack = GunItemBuilder.create().setId(selected.getKey()).setAmmoCount(data.getAmmoAmount())
                .setAmmoInBarrel(true).setFireMode(data.getFireModeSet().get(0)).build(actor.registryAccess());
        IGun gun = IGun.getIGunOrNull(stack);
        if (gun == null) throw new IllegalStateException("TaCZ builder returned no gun for " + selected.getKey());
        var attachments = new ArrayList<>(TimelessAPI.getAllCommonAttachmentIndex());
        attachments.sort(Comparator.comparing(entry -> entry.getKey().toString()));
        for (AttachmentType type : AttachmentType.values()) {
            if (type == AttachmentType.NONE || !gun.allowAttachmentType(stack, type)) continue;
            List<ItemStack> compatible = new ArrayList<>();
            for (var entry : attachments) {
                ItemStack candidate = AttachmentItemBuilder.create().setId(entry.getKey()).build();
                IAttachment api = IAttachment.getIAttachmentOrNull(candidate);
                if (api != null && api.getType(candidate) == type && gun.allowAttachment(stack, candidate)) compatible.add(candidate);
            }
            if (!compatible.isEmpty()) {
                ItemStack selectedAttachment = compatible.get(random.nextInt(compatible.size()));
                gun.installAttachment(actor.registryAccess(), stack, selectedAttachment);
                if (gun.getAttachment(actor.registryAccess(), stack, type).isEmpty()) throw new IllegalStateException("Attachment installation failed: " + type);
            }
        }
        gun.setCurrentAmmoCount(stack, com.tacz.guns.util.AttachmentDataUtils.getAmmoCountWithAttachment(stack, data));
        OperativeGun.supplyReserve(stack);
        return stack;
    }

    public static boolean matches(RaidAiRules.WeaponRole role, String type) {
        String normalized = type == null ? "" : type.toLowerCase(Locale.ROOT);
        return switch (role) {
            case RIFLE_OR_SMG -> normalized.equals("rifle") || normalized.equals("smg");
            case SNIPER -> normalized.equals("sniper");
            case SHOTGUN -> normalized.equals("shotgun");
            case PISTOL -> normalized.equals("pistol");
            case NETHERITE_AXE -> false;
        };
    }

    private static ItemStack fullyEnchanted(OperativeEntity actor, Item item) {
        ItemStack stack = new ItemStack(item);
        // Compatibility between enchantments is deliberately not queried: the author allows coexistence.
        for (var enchantment : actor.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).holders().toList())
            if (enchantment.value().canEnchant(stack)) stack.enchant(enchantment, enchantment.value().getMaxLevel());
        return stack;
    }

    public static void refreshEffects(OperativeEntity actor, List<RaidAiRules.Effect> effects) {
        for (RaidAiRules.Effect effect : effects) {
            ResourceLocation id = ResourceLocation.tryParse(effect.id());
            if (id == null || !BuiltInRegistries.MOB_EFFECT.containsKey(id)) throw new IllegalStateException("Missing effect " + effect.id());
            var resolved = BuiltInRegistries.MOB_EFFECT.getHolder(id).orElseThrow();
            MobEffectInstance existing = actor.getEffect(resolved);
            if (existing == null || existing.getDuration() < 100 || existing.getAmplifier() < effect.amplifier())
                actor.addEffect(new MobEffectInstance(resolved, 220, effect.amplifier(), true, false));
        }
    }

    /** Fractional durability is carried per equipped item; never rounds every small repair upward. */
    public static void repairWornArmor(OperativeEntity actor, double fraction) {
        for (EquipmentSlot slot : List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)) {
            ItemStack stack = actor.getItemBySlot(slot);
            if (stack.isEmpty() || !stack.isDamageableItem() || !stack.isDamaged()) continue;
            String key = "DfsRaidRepair." + slot.getName() + "." + BuiltInRegistries.ITEM.getKey(stack.getItem());
            double amount = stack.getMaxDamage() * fraction + actor.getPersistentData().getDouble(key);
            int repair = (int) Math.floor(amount);
            actor.getPersistentData().putDouble(key, amount - repair);
            if (repair > 0) stack.setDamageValue(Math.max(0, stack.getDamageValue() - repair));
        }
    }

    public static List<ItemStack> copyDeathEquipment(OperativeEntity actor) {
        List<ItemStack> result = new ArrayList<>();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = actor.getItemBySlot(slot).copy();
            if (!stack.isEmpty()) { OperativeGun.preparePlayerTransfer(stack); result.add(stack); }
        }
        return result;
    }
}
