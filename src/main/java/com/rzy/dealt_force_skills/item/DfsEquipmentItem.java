package com.rzy.dealt_force_skills.item;

import com.rzy.dealt_force_skills.registry.ModGameRules;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Consumer;

public class DfsEquipmentItem extends ArmorItem {
    public static final String TAG_VISION_MODE = "DfsVisionMode";
    public static final String TAG_INSTANCE_ID = "DfsEquipmentInstanceId";
    public static final int VISION_OFF = 0;
    public static final int VISION_NIGHT = 1;
    public static final int VISION_THERMAL = 2;

    private final Profile profile;
    private final DfsItemQuality quality;
    private final String tooltipKey;
    private final ItemAttributeModifiers defaultModifiers;

    public DfsEquipmentItem(Properties properties, Profile profile, String tooltipKey) {
        super(ArmorMaterials.IRON, armorType(profile.slot()), properties);
        this.profile = profile;
        this.quality = profile.quality();
        this.tooltipKey = tooltipKey;
        this.defaultModifiers = buildModifiers(profile);
    }

    public DfsItemQuality quality() {
        return quality;
    }

    @Override
    public Component getName(ItemStack stack) {
        Component name = Component.translatable(getDescriptionId(stack));
        return quality == DfsItemQuality.COLORFUL
                ? DfsItemQuality.rainbowText(name)
                : name.copy().withStyle(style -> style.withColor(quality.color()));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Component qualityLine = Component.translatable("tooltip.dealt_force_skills.quality",
                Component.translatable("quality.dealt_force_skills." + quality.key()));
        tooltip.add(quality == DfsItemQuality.COLORFUL
                ? DfsItemQuality.rainbowText(qualityLine)
                : qualityLine.copy().withStyle(ChatFormatting.GRAY));
        if (stack.isDamageableItem() && stack.isDamaged()) {
            tooltip.add(Component.translatable("tooltip.dealt_force_skills.durability",
                    displayedDurability(stack), stack.getMaxDamage()).withStyle(ChatFormatting.GRAY));
        }
        if (tooltipKey != null && !tooltipKey.isBlank()) {
            tooltip.add(Component.translatable(tooltipKey).withStyle(ChatFormatting.DARK_GRAY));
        }
        addExtraTooltip(stack, context, tooltip, flag);
    }

    public Profile profile() {
        return profile;
    }

    @Override
    public EquipmentSlot getEquipmentSlot() {
        return profile.slot();
    }

    @Override
    public ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, ArmorMaterial.Layer layer, boolean innerModel) {
        if (profile.slot() == EquipmentSlot.HEAD) {
            return ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "textures/models/armor/empty_layer_1.png");
        }
        return ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "textures/models/armor/" + profile.id() + "_layer_1.png");
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide) {
            ensureInstanceId(player.getItemInHand(hand));
        }
        return this.swapWithEquipmentSlot(this, level, player, hand);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (!level.isClientSide && entity instanceof Player) {
            ensureInstanceId(stack);
        }
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        return defaultModifiers;
    }

    @Override
    public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, T entity, Consumer<Item> onBroken) {
        if (amount <= 0) {
            return 0;
        }
        int cost = adjustedVanillaDamageCost(stack, amount);
        damageWithoutBreaking(stack, cost, entity);
        return 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        if (isBroken(stack)) {
            return 0;
        }
        return super.getBarWidth(stack);
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return stack.getCount() == 1;
    }

    @Override
    public int getEnchantmentValue() {
        return 12 + profile.quality().tier() * 2;
    }

    @Override
    public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
        ItemStack vanillaReference = profile.slot() == EquipmentSlot.HEAD
                ? new ItemStack(Items.IRON_HELMET)
                : new ItemStack(Items.IRON_CHESTPLATE);
        return enchantment.value().canEnchant(vanillaReference);
    }

    protected void addExtraTooltip(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.dealt_force_skills.equipment.faction",
                Component.translatable("faction.dealt_force_skills." + profile.faction().key())).withStyle(ChatFormatting.GRAY));
        addFactionSetTooltip(profile.faction(), tooltip);
        tooltip.add(Component.translatable("tooltip.dealt_force_skills.equipment.stats",
                profile.defense(),
                String.format(Locale.ROOT, "%.1f", profile.toughness())).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.dealt_force_skills.equipment.protection",
                Math.round(qualityProtectionRatio(profile.quality()) * 100.0D),
                qualityDurabilityCost(profile.quality())).withStyle(ChatFormatting.GRAY));
        if (!profile.coverageKey().isBlank()) {
            tooltip.add(Component.translatable("tooltip.dealt_force_skills.equipment.coverage",
                    Component.translatable("coverage.dealt_force_skills." + profile.coverageKey())).withStyle(ChatFormatting.GRAY));
        }
        if (profile.hasHelmetOptics()) {
            tooltip.add(Component.translatable("tooltip.dealt_force_skills.equipment.optics",
                    Component.translatable(valueKey(profile.nightVision())),
                    Component.translatable(valueKey(profile.thermalVision()))).withStyle(ChatFormatting.GRAY));
        }
        if (profile.hasHandlingStats()) {
            tooltip.add(Component.translatable("tooltip.dealt_force_skills.equipment.handling",
                    percent(profile.movementLimit()),
                    percent(profile.actionLimit()),
                    percent(profile.lightweight())).withStyle(ChatFormatting.GRAY));
        }
        if (profile.hasResistanceStats()) {
            tooltip.add(Component.translatable("tooltip.dealt_force_skills.equipment.resistance",
                    percent(profile.knockbackResistance()),
                    percent(profile.bluntResistance()),
                    percent(profile.kineticAbsorption())).withStyle(ChatFormatting.GRAY));
        }
        if (profile.hasHearingStats()) {
            tooltip.add(Component.translatable("tooltip.dealt_force_skills.equipment.hearing",
                    percent(profile.hearingBoost()),
                    Component.translatable("noise_reduction.dealt_force_skills." + profile.noiseReductionKey())).withStyle(ChatFormatting.GRAY));
        }
        if (profile.ability() != SpecialAbility.NONE) {
            tooltip.add(Component.translatable("tooltip.dealt_force_skills.equipment.ability",
                    Component.translatable("tooltip.dealt_force_skills." + profile.id() + ".ability")).withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    private static String percent(double value) {
        return String.format(Locale.ROOT, "%.0f%%", value * 100.0D);
    }

    private static String valueKey(boolean value) {
        return value ? "value.dealt_force_skills.yes" : "value.dealt_force_skills.no";
    }

    private static void addFactionSetTooltip(Faction faction, List<Component> tooltip) {
        String key = faction.key();
        tooltip.add(Component.translatable("tooltip.dealt_force_skills.equipment.faction_bonus.one",
                Component.translatable("tooltip.dealt_force_skills.equipment.faction." + key + ".one")).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.dealt_force_skills.equipment.faction_bonus.two",
                Component.translatable("tooltip.dealt_force_skills.equipment.faction." + key + ".two")).withStyle(ChatFormatting.GRAY));
    }

    private static Type armorType(EquipmentSlot slot) {
        return slot == EquipmentSlot.HEAD ? Type.HELMET : Type.CHESTPLATE;
    }

    private static ItemAttributeModifiers buildModifiers(Profile profile) {
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();
        EquipmentSlotGroup slotGroup = EquipmentSlotGroup.bySlot(profile.slot());
        builder.add(Attributes.ARMOR, modifier(profile, "armor", profile.defense(), AttributeModifier.Operation.ADD_VALUE), slotGroup);
        if (profile.toughness() > 0.0D) {
            builder.add(Attributes.ARMOR_TOUGHNESS, modifier(profile, "toughness", profile.toughness(), AttributeModifier.Operation.ADD_VALUE), slotGroup);
        }
        if (profile.knockbackResistance() > 0.0D) {
            builder.add(Attributes.KNOCKBACK_RESISTANCE, modifier(profile, "knockback", profile.knockbackResistance(), AttributeModifier.Operation.ADD_VALUE), slotGroup);
        }
        double movementModifier = profile.lightweight() - profile.movementLimit();
        if (Math.abs(movementModifier) > 0.0001D) {
            builder.add(Attributes.MOVEMENT_SPEED, modifier(profile, "movement", movementModifier, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL), slotGroup);
        }
        return builder.build();
    }

    private static AttributeModifier modifier(Profile profile, String key, double amount, AttributeModifier.Operation operation) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("dealt_force_skills", profile.id() + "_" + key);
        return new AttributeModifier(id, amount, operation);
    }

    public static Profile profile(ItemStack stack) {
        if (!stack.isEmpty() && stack.getItem() instanceof DfsEquipmentItem equipmentItem) {
            return equipmentItem.profile();
        }
        return null;
    }

    public static UUID ensureInstanceId(ItemStack stack) {
        if (profile(stack) == null) {
            return null;
        }
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!tag.hasUUID(TAG_INSTANCE_ID)) {
            tag.putUUID(TAG_INSTANCE_ID, UUID.randomUUID());
        }
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return tag.getUUID(TAG_INSTANCE_ID);
    }

    public static void removeDuplicatedEquippedHelmet(ServerPlayer player) {
        ItemStack equipped = player.getItemBySlot(EquipmentSlot.HEAD);
        UUID instanceId = ensureInstanceId(equipped);
        if (instanceId == null) {
            return;
        }
        for (ItemStack candidate : player.getInventory().items) {
            removeMatchingCopy(equipped, instanceId, candidate);
        }
        for (ItemStack candidate : player.getInventory().offhand) {
            removeMatchingCopy(equipped, instanceId, candidate);
        }
    }

    private static void removeMatchingCopy(ItemStack equipped, UUID instanceId, ItemStack candidate) {
        CompoundTag candidateTag = candidate.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (candidate.isEmpty() || candidate == equipped || candidate.getItem() != equipped.getItem()
                || !candidateTag.hasUUID(TAG_INSTANCE_ID)
                || !instanceId.equals(candidateTag.getUUID(TAG_INSTANCE_ID))) {
            return;
        }
        candidate.shrink(1);
    }

    public static boolean hasAbility(ItemStack stack, SpecialAbility ability) {
        Profile profile = profile(stack);
        return profile != null && profile.ability() == ability;
    }

    public static boolean isBroken(ItemStack stack) {
        return profile(stack) != null && stack.isDamageableItem() && stack.getDamageValue() >= breakDamageLimit(stack);
    }

    public static int breakDamageLimit(ItemStack stack) {
        return stack.isDamageableItem() ? Math.max(0, stack.getMaxDamage() - 1) : 0;
    }

    public static int remainingDurabilityBeforeBroken(ItemStack stack) {
        if (!stack.isDamageableItem()) {
            return 0;
        }
        return Math.max(0, breakDamageLimit(stack) - stack.getDamageValue());
    }

    public static boolean damageWithoutBreaking(ItemStack stack, int amount, LivingEntity wearer) {
        Profile profile = profile(stack);
        boolean noDurabilityLossEnabled = profile != null
                && profile.ability() == SpecialAbility.NO_DURABILITY_LOSS
                && (!(wearer instanceof Player player) || ModGameRules.areArmorSpecialsEnabled(player));
        if (profile == null || amount <= 0 || stack.isEmpty() || !stack.isDamageableItem()
                || noDurabilityLossEnabled) {
            return false;
        }
        int breakLimit = breakDamageLimit(stack);
        int oldDamage = stack.getDamageValue();
        if (oldDamage >= breakLimit) {
            return false;
        }
        int newDamage = Math.min(breakLimit, oldDamage + amount);
        stack.setDamageValue(newDamage);
        return newDamage >= breakLimit;
    }

    private static int displayedDurability(ItemStack stack) {
        if (!stack.isDamageableItem() || isBroken(stack)) {
            return 0;
        }
        return Math.max(0, stack.getMaxDamage() - stack.getDamageValue());
    }

    private static int adjustedVanillaDamageCost(ItemStack stack, int amount) {
        int cost = Math.max(0, amount);
        if (cost <= 0) {
            return 0;
        }
        int unbreaking = Math.max(0, enchantmentLevel(stack, Enchantments.UNBREAKING));
        return unbreaking <= 0 ? cost : Math.max(1, (int) Math.ceil(cost / (double) (unbreaking + 1)));
    }

    private static int enchantmentLevel(ItemStack stack, net.minecraft.resources.ResourceKey<Enchantment> key) {
        ItemEnchantments enchantments = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        int level = 0;
        for (var entry : enchantments.entrySet()) {
            if (entry.getKey().is(key)) {
                level = Math.max(level, entry.getIntValue());
            }
        }
        return level;
    }

    public static boolean hasUsableVision(ItemStack stack) {
        Profile profile = profile(stack);
        return profile != null && profile.hasHelmetOptics();
    }

    public static int activeVisionMode(LivingEntity entity) {
        if (entity == null) {
            return VISION_OFF;
        }
        return activeVisionMode(entity.getItemBySlot(EquipmentSlot.HEAD));
    }

    public static int activeVisionMode(ItemStack stack) {
        if (!hasUsableVision(stack)) {
            return VISION_OFF;
        }
        return sanitizeVisionMode(profile(stack), storedVisionMode(stack));
    }

    public static int storedVisionMode(ItemStack stack) {
        Profile profile = profile(stack);
        if (profile == null || !profile.hasHelmetOptics()) {
            return VISION_OFF;
        }
        CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = cd != null ? cd.copyTag() : null;
        if (tag == null || !tag.contains(TAG_VISION_MODE)) {
            return firstSupportedVisionMode(profile);
        }
        return sanitizeVisionMode(profile, tag.getInt(TAG_VISION_MODE));
    }

    public static void setVisionMode(ItemStack stack, int mode) {
        Profile profile = profile(stack);
        if (profile == null || !profile.hasHelmetOptics()) {
            return;
        }
        CompoundTag vmTag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        vmTag.putInt(TAG_VISION_MODE, sanitizeVisionMode(profile, mode));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(vmTag));
    }

    public static int firstSupportedVisionMode(Profile profile) {
        if (profile == null || !profile.hasHelmetOptics()) {
            return VISION_OFF;
        }
        if (profile.nightVision()) {
            return VISION_NIGHT;
        }
        return profile.thermalVision() ? VISION_THERMAL : VISION_OFF;
    }

    public static int nextVisionMode(Profile profile, int currentMode) {
        int current = sanitizeVisionMode(profile, currentMode);
        if (profile == null || !profile.hasHelmetOptics()) {
            return VISION_OFF;
        }
        if (profile.nightVision() && profile.thermalVision()) {
            return switch (current) {
                case VISION_NIGHT -> VISION_THERMAL;
                case VISION_THERMAL -> VISION_OFF;
                default -> VISION_NIGHT;
            };
        }
        if (profile.nightVision()) {
            return current == VISION_NIGHT ? VISION_OFF : VISION_NIGHT;
        }
        if (profile.thermalVision()) {
            return current == VISION_THERMAL ? VISION_OFF : VISION_THERMAL;
        }
        return VISION_OFF;
    }

    public static int sanitizeVisionMode(Profile profile, int mode) {
        if (profile == null || !profile.hasHelmetOptics()) {
            return VISION_OFF;
        }
        if (mode == VISION_NIGHT && profile.nightVision()) {
            return VISION_NIGHT;
        }
        if (mode == VISION_THERMAL && profile.thermalVision()) {
            return VISION_THERMAL;
        }
        return VISION_OFF;
    }

    public static double qualityProtectionRatio(DfsItemQuality quality) {
        return switch (quality) {
            case GRAY -> 0.10D;
            case WHITE -> 0.20D;
            case GREEN -> 0.30D;
            case BLUE -> 0.50D;
            case PURPLE -> 0.70D;
            case GOLD -> 0.90D;
            case RED -> 0.96D;
            case RED_7 -> 0.96D;
            case COLORFUL -> 0.96D;
        };
    }

    public static int qualityDurabilityCost(DfsItemQuality quality) {
        return switch (quality) {
            case GRAY -> 10;
            case WHITE -> 10;
            case GREEN -> 9;
            case BLUE -> 8;
            case PURPLE -> 7;
            case GOLD -> 6;
            case RED -> 5;
            case RED_7 -> 5;
            case COLORFUL -> 5;
        };
    }

    public enum Faction {
        ASARA("asara"),
        HVK("hvk"),
        GTI("gti"),
        GLOBAL_FORCES("global_forces");

        private final String key;

        Faction(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }
    }

    public enum SpecialAbility {
        NONE,
        NEW_RECRUIT,
        FRUGAL_DISASSEMBLY,
        COUNTER_KICK,
        LOOT_DUPLICATE,
        DURABLE,
        FRONT_IMMUNE,
        RANDOM_F4,
        DURABILITY_RESTORE,
        FATAL_GUARD,
        LONELY_DAMAGE,
        RED_OWL_REVENGE,
        SUMMON_REINFORCEMENTS,
        STATIONARY_STACKS,
        ALLY_DEATH_SWAP,
        HEAVY_DODGE_SUMMON,
        TRICK_ASSAULT,
        KING_KONG_EXECUTION,
        VACATION,
        NO_DURABILITY_LOSS,
        ELBOW_SPIRIT,
        HEARING_SHARE,
        MHS_FURY,
        ENDURANCE_TRANSFORM,
        MASK_LOCK_ON,
        GN_HEAVY,
        GN_HEAVY_NIGHT_VISION,
        DICH9_ASSAULT,
        GT5_STATIONARY_REDUCTION,
        H09_RIOT_DODGE,
        RED_OWL_MASK,
        H70_FOLLOWER_BOOST,
        H70_NIGHT_FOLLOWER_BOOST
    }

    public record Profile(
            String id,
            DfsItemQuality quality,
            EquipmentSlot slot,
            Faction faction,
            int defense,
            double toughness,
            String coverageKey,
            double movementLimit,
            double actionLimit,
            double knockbackResistance,
            double bluntResistance,
            double kineticAbsorption,
            double lightweight,
            double hearingBoost,
            String noiseReductionKey,
            boolean nightVision,
            boolean thermalVision,
            SpecialAbility ability,
            double lootDuplicateChance
    ) {
        public boolean isHelmet() {
            return slot == EquipmentSlot.HEAD;
        }

        public boolean isChestArmor() {
            return slot == EquipmentSlot.CHEST;
        }

        public boolean hasHandlingStats() {
            return movementLimit > 0.0D || actionLimit > 0.0D || lightweight > 0.0D;
        }

        public boolean hasResistanceStats() {
            return knockbackResistance > 0.0D || bluntResistance > 0.0D || kineticAbsorption > 0.0D;
        }

        public boolean hasHearingStats() {
            return Math.abs(hearingBoost) > 0.0001D || !noiseReductionKey.equals("none");
        }

        public boolean hasHelmetOptics() {
            return isHelmet() && (nightVision || thermalVision);
        }

        public double actionSpeedMultiplier(boolean hvkTwoPiece) {
            double penalty = actionLimit;
            double bonus = lightweight;
            if (hvkTwoPiece) {
                penalty *= 0.5D;
                bonus *= 1.5D;
            }
            return Math.max(0.1D, 1.0D - penalty + bonus);
        }
    }
}
