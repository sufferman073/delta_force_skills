package com.rzy.dealt_force_skills.character.undead;

import com.rzy.dealt_force_skills.skill.SkillCooldownHelper;

import com.rzy.dealt_force_skills.shop.UndeadShopEntry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceLocation;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;

import java.util.UUID;
import java.util.HashSet;
import java.util.Set;

public final class UndeadUpgradeManager {
    public static volatile int BASE_MAX_TALENT_POINTS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("BASE_MAX_TALENT_POINTS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.undead.undead_upgrade_manager.base_max_talent_points", 27));
    private static final String UPGRADES = "Upgrades";
    private static final String LEVELS = "Levels";
    private static final String EQUIPPED = "EquippedBracelets";
    private static final String PINK_COOLDOWN_UNTIL = "PinkBraceletCooldownUntil";
    private static final String PINK_INVULNERABLE_UNTIL = "PinkBraceletInvulnerableUntil";
    private static final Set<UUID> EFFECT_RESCALE_GUARD = new HashSet<>();

    private static final ResourceLocation MAX_HEALTH_UUID = ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "undead_upgrade_max_health");
    private static final ResourceLocation MOVEMENT_UUID = ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "undead_upgrade_movement");
    private static final ResourceLocation ARMOR_UUID = ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "undead_upgrade_armor");
    private static final ResourceLocation TOUGHNESS_UUID = ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "undead_upgrade_toughness");
    private static final ResourceLocation EXPLORER_REACH_UUID = ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "undead_upgrade_explorer_reach");

    private UndeadUpgradeManager() {
    }

    public static int level(Player player, UndeadShopEntry entry) {
        return Math.max(0, levels(player).getInt(entry.id()));
    }

    public static boolean has(Player player, UndeadShopEntry entry) {
        return level(player, entry) > 0;
    }

    public static void setLevel(Player player, UndeadShopEntry entry, int value) {
        levels(player).putInt(entry.id(), Math.max(0, Math.min(entry.maxLevel(), value)));
    }

    public static boolean isEquipped(Player player, UndeadShopEntry bracelet) {
        return bracelet.isBracelet() && equipped(player).getBoolean(bracelet.id());
    }

    public static int equippedBraceletCount(Player player) {
        int count = 0;
        for (UndeadShopEntry entry : UndeadShopEntry.values()) {
            if (isEquipped(player, entry)) {
                count++;
            }
        }
        return count;
    }

    public static boolean setEquipped(Player player, UndeadShopEntry bracelet, boolean value) {
        if (!bracelet.isBracelet() || !has(player, bracelet)) {
            return false;
        }
        equipped(player).putBoolean(bracelet.id(), value);
        return true;
    }

    public static int totalTalentPoints(Player player) {
        int total = 0;
        for (UndeadShopEntry entry : UndeadShopEntry.values()) {
            if (entry.category() == com.rzy.dealt_force_skills.shop.UndeadShopCategory.TALENT) {
                total += level(player, entry);
            }
        }
        return total;
    }

    public static int maxTalentPoints(Player player) {
        return BASE_MAX_TALENT_POINTS + level(player, UndeadShopEntry.TALENT_CAPACITY);
    }

    public static boolean allTalentAttributesMaxed(Player player) {
        return level(player, UndeadShopEntry.STRENGTH) >= UndeadShopEntry.STRENGTH.maxLevel()
                && level(player, UndeadShopEntry.AGILITY) >= UndeadShopEntry.AGILITY.maxLevel()
                && level(player, UndeadShopEntry.INTELLIGENCE) >= UndeadShopEntry.INTELLIGENCE.maxLevel()
                && level(player, UndeadShopEntry.WILL) >= UndeadShopEntry.WILL.maxLevel()
                && level(player, UndeadShopEntry.VITALITY) >= UndeadShopEntry.VITALITY.maxLevel()
                && level(player, UndeadShopEntry.CRAFT) >= UndeadShopEntry.CRAFT.maxLevel();
    }

    public static CompoundTag snapshot(Player player) {
        return upgradeData(player).copy();
    }

    public static void clearPurchaseProgress(Player player) {
        CompoundTag root = UndeadStateManager.rootData(player);
        root.remove(UPGRADES);
        clearAttributes(player);
        if (player instanceof ServerPlayer serverPlayer && UndeadStateManager.isUndead(serverPlayer)) {
            applyAttributes(serverPlayer);
            UndeadStateManager.syncToClient(serverPlayer);
        }
    }

    public static float maxEnergy(Player player) {
        float value = UndeadStateManager.baseMaxEnergy();
        if (isEquipped(player, UndeadShopEntry.PURPLE_LITHIUM_BRACELET)) {
            value += 80.0F;
        }
        value += level(player, UndeadShopEntry.INTELLIGENCE) * 10.0F;
        value += level(player, UndeadShopEntry.THOUSAND_TEMPERING) * 10.0F;
        return value;
    }

    public static float energyRegenMultiplier(Player player) {
        float multiplier = 1.0F + level(player, UndeadShopEntry.INTELLIGENCE) * 0.02F;
        if (isEquipped(player, UndeadShopEntry.EMERALD_BRACELET)) {
            multiplier += 0.25F;
        }
        if (isEquipped(player, UndeadShopEntry.PURPLE_LITHIUM_BRACELET)) {
            multiplier += 0.50F;
        }
        return multiplier;
    }

    public static float energyCostMultiplier(Player player) {
        return Math.max(0.0F, 1.0F
                - level(player, UndeadShopEntry.CRAFT) * 0.02F
                - level(player, UndeadShopEntry.THOUSAND_TEMPERING) * 0.01F);
    }

    public static int cooldownTicks(Player player, int baseTicks) {
        double multiplier = Math.max(0.0D, 1.0D - level(player, UndeadShopEntry.CRAFT) * 0.05D);
        return Math.max(1, (int) Math.ceil(baseTicks * multiplier));
    }

    public static float outgoingDamageMultiplier(Player player) {
        return 1.0F
                + level(player, UndeadShopEntry.WEAPON_POLISH) * 0.10F
                + level(player, UndeadShopEntry.STRENGTH) * 0.04F
                + level(player, UndeadShopEntry.THOUSAND_TEMPERING) * 0.04F;
    }

    public static float incomingDamageMultiplier(Player player, DamageSource source) {
        float multiplier = 1.0F - level(player, UndeadShopEntry.ARMOR_FORGING) * 0.03F;
        if (isEquipped(player, UndeadShopEntry.BLUE_ZIRCON_BRACELET)
                && source.is(DamageTypeTags.IS_PROJECTILE)) {
            multiplier *= 0.70F;
        }
        if (isEquipped(player, UndeadShopEntry.OBSIDIAN_BRACELET)) {
            if (source.is(DamageTypeTags.IS_EXPLOSION)) {
                multiplier *= 0.50F;
            }
            boolean skillDamage = source.typeHolder().unwrapKey()
                    .map(key -> "dealt_force_skills".equals(key.location().getNamespace()))
                    .orElse(false);
            if (skillDamage) {
                multiplier *= 0.70F;
            }
        }
        return Math.max(0.0F, multiplier);
    }

    public static float dodgeChance(Player player) {
        float chance = level(player, UndeadShopEntry.AGILITY) * 0.005F;
        if (isEquipped(player, UndeadShopEntry.TOURMALINE_BRACELET)) {
            chance += 0.10F;
        }
        return Math.min(0.95F, chance);
    }

    public static float criticalChance(Player player) {
        return isEquipped(player, UndeadShopEntry.YELLOW_DIAMOND_BRACELET) ? 0.25F : 0.0F;
    }

    public static float healingMultiplier(Player player) {
        float multiplier = 1.0F + level(player, UndeadShopEntry.VITALITY) * 0.01F;
        if (isEquipped(player, UndeadShopEntry.RUBY_BRACELET)) {
            multiplier += 0.10F;
        }
        if (isEquipped(player, UndeadShopEntry.PINK_DIAMOND_BRACELET)) {
            multiplier += 0.20F;
        }
        return multiplier;
    }

    public static float negativeDurationMultiplier(Player player) {
        float multiplier = 1.0F - level(player, UndeadShopEntry.WILL) * 0.05F;
        if (isEquipped(player, UndeadShopEntry.EMERALD_BRACELET)) {
            multiplier *= 0.50F;
        }
        return Math.max(0.05F, multiplier);
    }

    public static float interactionSpeedMultiplier(Player player) {
        if (player == null || !UndeadStateManager.isUndead(player)) {
            return 1.0F;
        }
        return 1.0F
                + level(player, UndeadShopEntry.WILL) * 0.02F
                + level(player, UndeadShopEntry.THOUSAND_TEMPERING) * 0.02F;
    }

    public static boolean tryPinkBraceletFatalGuard(ServerPlayer player, float incomingDamage) {
        if (!isEquipped(player, UndeadShopEntry.PINK_DIAMOND_BRACELET)
                || incomingDamage < player.getHealth()) {
            return false;
        }
        CompoundTag tag = upgradeData(player);
        long now = SkillCooldownHelper.now(player);
        if (now < tag.getLong(PINK_COOLDOWN_UNTIL)) {
            return false;
        }
        tag.putLong(PINK_COOLDOWN_UNTIL, now + 120L * 20L);
        tag.putLong(PINK_INVULNERABLE_UNTIL, now + 20L);
        player.invulnerableTime = Math.max(player.invulnerableTime, 20);
        return true;
    }

    public static boolean isPinkBraceletInvulnerable(ServerPlayer player) {
        return SkillCooldownHelper.now(player) < upgradeData(player).getLong(PINK_INVULNERABLE_UNTIL);
    }

    public static boolean tryRescaleAddedEffect(
            LivingEntity target,
            MobEffectInstance added,
            Entity effectSource
    ) {
        if (target.level().isClientSide || added == null || EFFECT_RESCALE_GUARD.contains(target.getUUID())) {
            return false;
        }
        double multiplier = 1.0D;
        if (target instanceof ServerPlayer player
                && UndeadStateManager.isUndead(player)
                && added.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
            multiplier *= negativeDurationMultiplier(player);
        }
        if (effectSource instanceof ServerPlayer source && UndeadStateManager.isUndead(source)) {
            if (UndeadStateManager.profession(source) == UndeadProfession.SCHOLAR
                    && has(source, UndeadShopEntry.SCHOLAR_RETURNED)) {
                multiplier *= 2.0D;
            }
            if (UndeadStateManager.profession(source) == UndeadProfession.ROGUE
                    && has(source, UndeadShopEntry.ROGUE_HEAVY_BLOW)
                    && added.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
                multiplier *= 2.0D;
            }
        }
        int duration = Math.max(1, (int) Math.round(added.getDuration() * multiplier));
        if (duration == added.getDuration()) {
            return false;
        }
        MobEffectInstance scaled = new MobEffectInstance(
                added.getEffect(),
                duration,
                added.getAmplifier(),
                added.isAmbient(),
                added.isVisible(),
                added.showIcon()
        );
        EFFECT_RESCALE_GUARD.add(target.getUUID());
        try {
            target.removeEffect(added.getEffect());
            return target.addEffect(scaled, effectSource);
        } finally {
            EFFECT_RESCALE_GUARD.remove(target.getUUID());
        }
    }

    public static void applyAttributes(Player player) {
        if (!UndeadStateManager.isUndead(player)) {
            clearAttributes(player);
            return;
        }
        double health = level(player, UndeadShopEntry.VITALITY) * 0.04D
                + level(player, UndeadShopEntry.THOUSAND_TEMPERING) * 0.04D;
        if (isEquipped(player, UndeadShopEntry.RUBY_BRACELET)) {
            health += 0.25D;
        }
        double movement = level(player, UndeadShopEntry.AGILITY) * 0.02D;
        if (isEquipped(player, UndeadShopEntry.TOURMALINE_BRACELET)) {
            movement += 0.15D;
        }
        double armor = level(player, UndeadShopEntry.ARMOR_FORGING) * 0.05D;
        if (isEquipped(player, UndeadShopEntry.BLUE_ZIRCON_BRACELET)) {
            armor += 0.15D;
        }
        applyModifier(player.getAttribute(Attributes.MAX_HEALTH), MAX_HEALTH_UUID, health);
        applyModifier(player.getAttribute(Attributes.MOVEMENT_SPEED), MOVEMENT_UUID, movement);
        applyModifier(player.getAttribute(Attributes.ARMOR), ARMOR_UUID, armor);
        applyModifier(player.getAttribute(Attributes.ARMOR_TOUGHNESS), TOUGHNESS_UUID, armor);
        applyModifier(player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE), EXPLORER_REACH_UUID,
                UndeadStateManager.profession(player) == UndeadProfession.EXPLORER ? 0.50D : 0.0D);
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    public static void clearAttributes(Player player) {
        removeModifier(player.getAttribute(Attributes.MAX_HEALTH), MAX_HEALTH_UUID);
        removeModifier(player.getAttribute(Attributes.MOVEMENT_SPEED), MOVEMENT_UUID);
        removeModifier(player.getAttribute(Attributes.ARMOR), ARMOR_UUID);
        removeModifier(player.getAttribute(Attributes.ARMOR_TOUGHNESS), TOUGHNESS_UUID);
        removeModifier(player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE), EXPLORER_REACH_UUID);
    }

    private static void applyModifier(
            AttributeInstance attribute,
            ResourceLocation id,
            double amount
    ) {
        if (attribute == null) {
            return;
        }
        AttributeModifier existing = attribute.getModifier(id);
        if (existing != null && Math.abs(existing.amount() - amount) < 1.0E-6D) {
            return;
        }
        if (existing != null) {
            attribute.removeModifier(id);
        }
        if (amount > 0.0D) {
            attribute.addTransientModifier(new AttributeModifier(
                    id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    private static void removeModifier(AttributeInstance attribute, ResourceLocation id) {
        if (attribute != null) {
            attribute.removeModifier(id);
        }
    }

    private static CompoundTag upgradeData(Player player) {
        CompoundTag root = UndeadStateManager.rootData(player);
        if (!root.contains(UPGRADES, Tag.TAG_COMPOUND)) {
            root.put(UPGRADES, new CompoundTag());
        }
        return root.getCompound(UPGRADES);
    }

    private static CompoundTag levels(Player player) {
        CompoundTag upgrades = upgradeData(player);
        if (!upgrades.contains(LEVELS, Tag.TAG_COMPOUND)) {
            upgrades.put(LEVELS, new CompoundTag());
        }
        return upgrades.getCompound(LEVELS);
    }

    private static CompoundTag equipped(Player player) {
        CompoundTag upgrades = upgradeData(player);
        if (!upgrades.contains(EQUIPPED, Tag.TAG_COMPOUND)) {
            upgrades.put(EQUIPPED, new CompoundTag());
        }
        return upgrades.getCompound(EQUIPPED);
    }
}
