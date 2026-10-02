package com.rzy.dealt_force_skills.character.commander;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.CharacterDefinition;
import com.rzy.dealt_force_skills.character.CharacterRole;
import com.rzy.dealt_force_skills.character.CharacterSelectionManager;
import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.character.SkillSlot;
import com.rzy.dealt_force_skills.compat.TrainingTargetCompat;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.skill.SkillDamageHelper;
import com.rzy.dealt_force_skills.team.DealtTeamManager;
import com.rzy.dealt_force_skills.util.TargetingUtil;
import com.rzy.dealt_force_skills.network.S2C_CommanderSnapshot;
import com.rzy.dealt_force_skills.skill.SkillCooldownHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;

import net.minecraft.resources.ResourceLocation;

import java.util.EnumSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class CommanderStateManager {
    private static final String ROOT_TAG = DealtForceSkillsMod.MODID + ".commander";
    private static final String INITIALIZED = "Initialized";
    private static final String TACTICAL_POINTS = "TacticalPoints";
    private static final String LAST_SURVIVAL_REWARD = "LastSurvivalReward";
    private static final String EFFICIENCY_LEVEL = "EfficiencyLevel";
    private static final String STRENGTH_LEVEL = "StrengthLevel";
    private static final String GENE_LEVEL = "GeneLevel";
    private static final String SUPPORT_COOLDOWNS = "SupportCooldowns";
    private static final String BRANCH_UPGRADES = "BranchUpgrades";
    private static final String ENABLED_PASSIVES = "EnabledPassives";
    private static final String ENABLED_PASSIVES_INITIALIZED = "EnabledPassivesInitialized";
    private static final String MEDICAL_SUPPORT_UNTIL = "MedicalSupportUntil";
    private static final String ADS_COVER_UNTIL = "AdsCoverUntil";
    private static final String QUICK_REVIVE_UNTIL = "QuickReviveUntil";
    private static final String DAMAGE_WINDOW_TICK = "DamageWindowTick";
    private static final String DAMAGE_WINDOW_TOTAL = "DamageWindowTotal";

    private static final ResourceLocation MAX_HEALTH_MODIFIER = ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "commander_max_health");
    private static final ResourceLocation MOVEMENT_MODIFIER = ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "commander_movement");
    private static final ResourceLocation LUCK_MODIFIER = ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "commander_luck");
    private static final ResourceLocation STEP_HEIGHT_MODIFIER = ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "commander_step_height");
    private static final double STEP_HEIGHT_BONUS = 0.5D;
    private static final Set<CharacterRole> INHERITABLE_ROLES = EnumSet.of(
            CharacterRole.ASSAULT, CharacterRole.SUPPORT, CharacterRole.ENGINEER, CharacterRole.RECON);

    private CommanderStateManager() {
    }

    public static boolean isCommander(Player player) {
        // Inherited skill execution temporarily exposes the source character through
        // getSelectedCharacterId(); Commander identity must use the stored selection.
        return player != null && CharacterSelectionManager.getStoredSelectedCharacterId(player)
                .map(ModCharacters.COMMANDER_ID::equals)
                .orElse(false);
    }

    public static double stepHeightBonus() {
        return STEP_HEIGHT_BONUS;
    }

    public static void initializeIfNeeded(ServerPlayer player) {
        if (!isCommander(player)) {
            return;
        }
        CompoundTag tag = data(player);
        if (!tag.getBoolean(INITIALIZED)) {
            tag.putBoolean(INITIALIZED, true);
            tag.putLong(TACTICAL_POINTS, 0L);
            tag.putLong(LAST_SURVIVAL_REWARD, SkillCooldownHelper.now(player));
            tag.putInt(EFFICIENCY_LEVEL, 0);
            tag.putInt(STRENGTH_LEVEL, 0);
            tag.putInt(GENE_LEVEL, 0);
            putChoice(tag, CommanderSkillChoice.PASSIVE_1, ModCharacters.D_WOLF_ID);
            putChoice(tag, CommanderSkillChoice.PASSIVE_2, ModCharacters.STINGER_ID);
            putChoice(tag, CommanderSkillChoice.PASSIVE_3, ModCharacters.GIZMO_ID);
            putChoice(tag, CommanderSkillChoice.PASSIVE_4, ModCharacters.LUNA_ID);
            putChoice(tag, CommanderSkillChoice.ACTIVE_1, ModCharacters.D_WOLF_ID);
            putChoice(tag, CommanderSkillChoice.ACTIVE_2, ModCharacters.LUNA_ID);
            putChoice(tag, CommanderSkillChoice.CORE_1, ModCharacters.ULURU_ID);
            putChoice(tag, CommanderSkillChoice.CORE_2, ModCharacters.STINGER_ID);
        }
        repairChoices(tag);
        if (geneLevel(player) >= CommanderUpgrade.GENE.maxLevel()) {
            initializeEnabledPassives(tag);
        }
        refreshAttributes(player);
        if (hasBranchUpgrade(player, CommanderBranchUpgrade.MORSE_SOLO_DETECTION)
                && player.tickCount % 10 == 0) {
            for (LivingEntity target : player.serverLevel().getEntitiesOfClass(LivingEntity.class,
                    player.getBoundingBox().inflate(50.0D),
                    entity -> TargetingUtil.isHostileLivingFor(player, entity))) {
                target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 20, 0, false, true, true));
            }
        }
    }

    public static void tick(ServerPlayer player) {
        if (!isCommander(player)) {
            removeAttributeModifiers(player);
            return;
        }
        initializeIfNeeded(player);
        CompoundTag tag = data(player);
        long now = SkillCooldownHelper.now(player);
        long last = tag.getLong(LAST_SURVIVAL_REWARD);
        if (last <= 0L || now < last) {
            tag.putLong(LAST_SURVIVAL_REWARD, now);
        } else if (now - last >= 1_200L) {
            long rewards = (now - last) / 1_200L;
            addTacticalPoints(player, rewards * 200L);
            tag.putLong(LAST_SURVIVAL_REWARD, last + rewards * 1_200L);
        }
        int gene = geneLevel(player);
        if (gene > 0 && player.tickCount % 20 == 0 && player.getHealth() > 0.0F
                && player.getHealth() < player.getMaxHealth()) {
            player.heal(player.getMaxHealth() * 0.005F * gene);
        }
        refreshAttributes(player);
    }

    public static void copyState(Player original, Player target) {
        CompoundTag source = original.getPersistentData();
        if (source.contains(ROOT_TAG, Tag.TAG_COMPOUND)) {
            target.getPersistentData().put(ROOT_TAG, source.getCompound(ROOT_TAG).copy());
        }
    }

    public static void onDeselected(ServerPlayer player) {
        removeAttributeModifiers(player);
    }

    /**
     * Removes stale Commander-owned modifiers even when the normal Commander
     * dispatcher is no longer running for this player (for example after a
     * character switch or respawn clone).
     */
    public static void cleanupIfNotCommander(ServerPlayer player) {
        if (player != null && !isCommander(player) && hasCommanderAttributeState(player)) {
            removeAttributeModifiers(player);
        }
    }

    /** Keeps stale client-side role state from suppressing vanilla half-block traversal. */
    public static void ensureVanillaStepHeight(Player player) {
        if (player != null && player.maxUpStep() < 0.6F) {
            AttributeInstance stepAttr = player.getAttribute(Attributes.STEP_HEIGHT);
            if (stepAttr != null && stepAttr.getBaseValue() < 0.6D) {
                stepAttr.setBaseValue(0.6D);
            }
        }
    }

    private static boolean hasCommanderAttributeState(ServerPlayer player) {
        if (player.getPersistentData().contains(ROOT_TAG, Tag.TAG_COMPOUND)) {
            return true;
        }
        return hasModifier(player.getAttribute(Attributes.MAX_HEALTH), MAX_HEALTH_MODIFIER)
                || hasModifier(player.getAttribute(Attributes.MOVEMENT_SPEED), MOVEMENT_MODIFIER)
                || hasModifier(player.getAttribute(Attributes.LUCK), LUCK_MODIFIER)
                || hasModifier(player.getAttribute(Attributes.STEP_HEIGHT), STEP_HEIGHT_MODIFIER);
    }

    private static boolean hasModifier(AttributeInstance attribute, ResourceLocation id) {
        return attribute != null && attribute.getModifier(id) != null;
    }

    public static long tacticalPoints(Player player) {
        return Math.max(0L, data(player).getLong(TACTICAL_POINTS));
    }

    public static void addTacticalPoints(ServerPlayer player, long amount) {
        if (!isCommander(player) || amount <= 0L) {
            return;
        }
        CompoundTag tag = data(player);
        long current = tacticalPoints(player);
        long updated = amount > Long.MAX_VALUE - current ? Long.MAX_VALUE : current + amount;
        tag.putLong(TACTICAL_POINTS, updated);
        syncToClient(player);
    }

    public static long grantTacticalPoints(ServerPlayer player, long amount) {
        if (amount > 0L) {
            addTacticalPoints(player, amount);
        }
        return tacticalPoints(player);
    }

    public static long setTacticalPoints(ServerPlayer player, long amount) {
        if (!isCommander(player)) {
            return tacticalPoints(player);
        }
        data(player).putLong(TACTICAL_POINTS, Math.max(0L, amount));
        syncToClient(player);
        return tacticalPoints(player);
    }

    public static boolean spendTacticalPoints(ServerPlayer player, long amount) {
        if (!isCommander(player) || amount < 0L || tacticalPoints(player) < amount) {
            return false;
        }
        data(player).putLong(TACTICAL_POINTS, tacticalPoints(player) - amount);
        syncToClient(player);
        return true;
    }

    public static void refundTacticalPoints(ServerPlayer player, long amount) {
        addTacticalPoints(player, amount);
    }

    public static int upgradeLevel(Player player, CommanderUpgrade upgrade) {
        return switch (upgrade) {
            case EFFICIENCY -> data(player).getInt(EFFICIENCY_LEVEL);
            case STRENGTH -> data(player).getInt(STRENGTH_LEVEL);
            case GENE -> data(player).getInt(GENE_LEVEL);
        };
    }

    public static boolean buyUpgrade(ServerPlayer player, CommanderUpgrade upgrade) {
        initializeIfNeeded(player);
        int level = upgradeLevel(player, upgrade);
        long cost = upgrade.cost(level);
        if (cost < 0L || !spendTacticalPoints(player, cost)) {
            return false;
        }
        switch (upgrade) {
            case EFFICIENCY -> data(player).putInt(EFFICIENCY_LEVEL, level + 1);
            case STRENGTH -> data(player).putInt(STRENGTH_LEVEL, level + 1);
            case GENE -> data(player).putInt(GENE_LEVEL, level + 1);
        }
        if (upgrade == CommanderUpgrade.GENE && geneLevel(player) >= CommanderUpgrade.GENE.maxLevel()) {
            initializeEnabledPassives(data(player));
        }
        refreshAttributes(player);
        syncToClient(player);
        return true;
    }

    public static int efficiencyLevel(Player player) {
        return Math.min(CommanderUpgrade.EFFICIENCY.maxLevel(), upgradeLevel(player, CommanderUpgrade.EFFICIENCY));
    }

    public static int strengthLevel(Player player) {
        return Math.min(CommanderUpgrade.STRENGTH.maxLevel(), upgradeLevel(player, CommanderUpgrade.STRENGTH));
    }

    public static int geneLevel(Player player) {
        return Math.min(CommanderUpgrade.GENE.maxLevel(), upgradeLevel(player, CommanderUpgrade.GENE));
    }

    public static boolean ownsBranchUpgrade(Player player, CommanderBranchUpgrade upgrade) {
        return data(player).getCompound(BRANCH_UPGRADES).getBoolean(upgrade.name());
    }

    public static boolean hasBranchUpgrade(Player player, CommanderBranchUpgrade upgrade) {
        return isCommander(player) && ownsBranchUpgrade(player, upgrade)
                && inheritedRuntimeSources(player).contains(upgrade.characterId());
    }

    public static boolean buyBranchUpgrade(ServerPlayer player, CommanderBranchUpgrade upgrade) {
        initializeIfNeeded(player);
        if (ownsBranchUpgrade(player, upgrade)
                || !spendTacticalPoints(player, upgrade.cost())) {
            return false;
        }
        CompoundTag upgrades = data(player).getCompound(BRANCH_UPGRADES);
        upgrades.putBoolean(upgrade.name(), true);
        data(player).put(BRANCH_UPGRADES, upgrades);
        syncToClient(player);
        return true;
    }

    public static double skillCooldownMultiplier(Player player) {
        if (!isCommander(player)) {
            return 1.0D;
        }
        return Math.max(0.10D, 0.50D - efficiencyLevel(player) * 0.10D);
    }

    public static double skillStrengthMultiplier(Player player) {
        if (!isCommander(player)) {
            return 1.0D;
        }
        return skillEffectMultiplier(player) * (1.0D + geneLevel(player) * 0.10D);
    }

    public static double skillEffectMultiplier(Player player) {
        if (!isCommander(player)) {
            return 1.0D;
        }
        return 2.0D * Math.pow(1.25D, strengthLevel(player));
    }

    public static int skillDuration(Player player, int baseTicks) {
        return scalePositiveInt(baseTicks, skillEffectMultiplier(player));
    }

    public static int skillEffectAmplifier(Player player, int baseAmplifier) {
        if (baseAmplifier < 0) {
            return baseAmplifier;
        }
        return Math.max(0, scalePositiveInt(baseAmplifier + 1, skillEffectMultiplier(player)) - 1);
    }

    public static int skillCapacity(Player player, int baseCapacity) {
        return scalePositiveInt(baseCapacity, skillEffectMultiplier(player));
    }

    public static int skillCount(Player player, int baseCount) {
        return scalePositiveInt(baseCount, skillEffectMultiplier(player));
    }

    public static double skillRadius(Player player, double baseRadius) {
        return baseRadius <= 0.0D ? baseRadius : baseRadius * skillEffectMultiplier(player);
    }

    public static float skillCost(Player player, float baseCost) {
        return isCommander(player) && baseCost > 0.0F ? baseCost * 0.5F : baseCost;
    }

    public static int skillCost(Player player, int baseCost) {
        if (!isCommander(player) || baseCost <= 0) {
            return baseCost;
        }
        return Math.max(1, (int) Math.ceil(baseCost * 0.5D));
    }

    public static double skillDamageFalloff(Player player, double baseFalloff) {
        return isCommander(player) && baseFalloff > 0.0D ? baseFalloff * 0.5D : baseFalloff;
    }

    public static float ordinaryDamageMultiplier(Player player) {
        return isCommander(player) ? 1.0F + geneLevel(player) * 0.10F : 1.0F;
    }

    public static Optional<String> selectedSource(Player player, CommanderSkillChoice choice) {
        String id = data(player).getString(choiceKey(choice));
        return isValidSource(id, choice) ? Optional.of(id) : Optional.empty();
    }

    public static Optional<String> sourceForSkill(Player player, SkillSlot slot, boolean alternate) {
        CommanderSkillChoice choice = switch (slot) {
            case ACTIVE_1 -> CommanderSkillChoice.ACTIVE_1;
            case ACTIVE_2 -> CommanderSkillChoice.ACTIVE_2;
            case CORE -> alternate ? CommanderSkillChoice.CORE_2 : CommanderSkillChoice.CORE_1;
            default -> null;
        };
        return choice == null ? Optional.empty() : selectedSource(player, choice);
    }

    public static boolean setSelectedSource(ServerPlayer player, CommanderSkillChoice choice, String characterId) {
        initializeIfNeeded(player);
        if (choice.skillSlot() == SkillSlot.PASSIVE
                && geneLevel(player) >= CommanderUpgrade.GENE.maxLevel()) {
            return false;
        }
        if (!isValidSource(characterId, choice)) {
            return false;
        }
        for (CommanderSkillChoice other : CommanderSkillChoice.values()) {
            if (other == choice || other.skillSlot() != choice.skillSlot()) {
                continue;
            }
            if (selectedSource(player, other).filter(characterId::equals).isPresent()) {
                return false;
            }
        }
        putChoice(data(player), choice, characterId);
        syncToClient(player);
        return true;
    }

    public static List<CharacterDefinition> availableSources(CommanderSkillChoice choice) {
        return ModCharacters.all().stream()
                .filter(CharacterDefinition::selectable)
                .filter(character -> INHERITABLE_ROLES.contains(character.role()))
                .filter(character -> character.skill(choice.skillSlot()).map(skill -> skill.implemented()).orElse(false))
                .toList();
    }

    public static boolean hasInheritedPassive(Player player, String characterId) {
        if (!isCommander(player) || characterId == null) {
            return false;
        }
        if (geneLevel(player) >= CommanderUpgrade.GENE.maxLevel()) {
            return enabledPassives(player).contains(characterId);
        }
        for (int index = 0; index < 4; index++) {
            CommanderSkillChoice choice = CommanderSkillChoice.values()[index];
            if (selectedSource(player, choice).filter(characterId::equals).isPresent()) {
                return true;
            }
        }
        return false;
    }

    public static List<String> enabledPassives(Player player) {
        CompoundTag tag = data(player);
        ListTag list = tag.getList(ENABLED_PASSIVES, Tag.TAG_STRING);
        List<String> enabled = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            String id = list.getString(i);
            if (isValidSource(id, CommanderSkillChoice.PASSIVE_1) && !enabled.contains(id)) {
                enabled.add(id);
            }
        }
        return List.copyOf(enabled);
    }

    public static boolean setPassiveEnabled(ServerPlayer player, String characterId, boolean enabled) {
        initializeIfNeeded(player);
        if (!isValidSource(characterId, CommanderSkillChoice.PASSIVE_1)) {
            syncToClient(player);
            return false;
        }
        if (geneLevel(player) >= CommanderUpgrade.GENE.maxLevel()) {
            CompoundTag tag = data(player);
            initializeEnabledPassives(tag);
            List<String> passives = new ArrayList<>(enabledPassives(player));
            boolean changed = enabled ? !passives.contains(characterId) && passives.add(characterId)
                    : passives.remove(characterId);
            if (changed) {
                putEnabledPassives(tag, passives);
            }
            syncToClient(player);
            return true;
        }
        CommanderSkillChoice[] passiveChoices = passiveChoices();
        for (CommanderSkillChoice choice : passiveChoices) {
            if (selectedSource(player, choice).filter(characterId::equals).isPresent()) {
                if (!enabled) {
                    putChoice(data(player), choice, "");
                    syncToClient(player);
                }
                return true;
            }
        }
        if (!enabled) {
            syncToClient(player);
            return true;
        }
        for (CommanderSkillChoice choice : passiveChoices) {
            if (selectedSource(player, choice).isEmpty()) {
                putChoice(data(player), choice, characterId);
                syncToClient(player);
                return true;
            }
        }
        syncToClient(player);
        return false;
    }

    public static List<String> inheritedRuntimeSources(Player player) {
        List<String> sources = new ArrayList<>();
        for (CommanderSkillChoice choice : CommanderSkillChoice.values()) {
            if (choice.skillSlot() != SkillSlot.PASSIVE) {
                selectedSource(player, choice).ifPresent(sources::add);
            }
        }
        if (geneLevel(player) >= CommanderUpgrade.GENE.maxLevel()) {
            sources.addAll(enabledPassives(player));
        } else {
            for (CommanderSkillChoice choice : passiveChoices()) {
                selectedSource(player, choice).ifPresent(sources::add);
            }
        }
        return sources.stream().distinct().toList();
    }

    public static boolean inheritsRuntimeSource(Player player, String characterId) {
        return characterId != null && isCommander(player)
                && inheritedRuntimeSources(player).contains(characterId);
    }

    public static int supportCooldownTicks(Player player, CommanderSupport support) {
        long until = data(player).getCompound(SUPPORT_COOLDOWNS).getLong(support.name());
        return SkillCooldownHelper.remainingTicks(player, until);
    }

    public static boolean beginSupportCooldown(ServerPlayer player, CommanderSupport support) {
        if (supportCooldownTicks(player, support) > 0) {
            return false;
        }
        CompoundTag cooldowns = data(player).getCompound(SUPPORT_COOLDOWNS);
        cooldowns.putLong(support.name(), SkillCooldownHelper.now(player) + support.cooldownTicks());
        data(player).put(SUPPORT_COOLDOWNS, cooldowns);
        return true;
    }

    public static void cancelSupportCooldown(ServerPlayer player, CommanderSupport support) {
        CompoundTag cooldowns = data(player).getCompound(SUPPORT_COOLDOWNS);
        cooldowns.remove(support.name());
        data(player).put(SUPPORT_COOLDOWNS, cooldowns);
    }

    public static long medicalSupportUntil(Player player) {
        return data(player).getLong(MEDICAL_SUPPORT_UNTIL);
    }

    public static void setMedicalSupportUntil(Player player, long until) {
        data(player).putLong(MEDICAL_SUPPORT_UNTIL, until);
    }

    public static long adsCoverUntil(Player player) {
        return data(player).getLong(ADS_COVER_UNTIL);
    }

    public static void setAdsCoverUntil(Player player, long until) {
        data(player).putLong(ADS_COVER_UNTIL, until);
    }

    public static long quickReviveUntil(Player player) {
        return data(player).getLong(QUICK_REVIVE_UNTIL);
    }

    public static void setQuickReviveUntil(Player player, long until) {
        data(player).putLong(QUICK_REVIVE_UNTIL, until);
    }

    public static float capIncomingDamage(ServerPlayer player, float amount) {
        int gene = geneLevel(player);
        if (hasBranchUpgrade(player, CommanderBranchUpgrade.VLINDER_SAVE_ANOTHER)
                && DealtTeamManager.onlineTeammates(player).stream()
                .anyMatch(teammate -> teammate.hasEffect(ModEffects.STINGER_DOWNED)
                        || teammate.hasEffect(ModEffects.VLINDER_VITAL_DOWNED))) {
            return Math.min(amount, Math.max(0.0F, player.getHealth() - 1.0F));
        }
        if (gene < 5 || amount <= 0.0F) {
            return amount;
        }
        CompoundTag tag = data(player);
        long window = SkillCooldownHelper.now(player) / 20L;
        if (tag.getLong(DAMAGE_WINDOW_TICK) != window) {
            tag.putLong(DAMAGE_WINDOW_TICK, window);
            tag.putFloat(DAMAGE_WINDOW_TOTAL, 0.0F);
        }
        float maxHealth = player.getMaxHealth();
        float perHit = maxHealth * 0.20F;
        float remaining = Math.max(0.0F, maxHealth * 0.30F - tag.getFloat(DAMAGE_WINDOW_TOTAL));
        float accepted = Math.min(amount, Math.min(perHit, remaining));
        tag.putFloat(DAMAGE_WINDOW_TOTAL, tag.getFloat(DAMAGE_WINDOW_TOTAL) + accepted);
        return accepted;
    }

    public static boolean rollsDodge(ServerPlayer player) {
        int gene = geneLevel(player);
        return gene > 0 && player.getRandom().nextFloat() < Math.min(0.40F, gene * 0.02F);
    }

    public static float incomingDamageMultiplier(Player player) {
        return isCommander(player) ? Math.max(0.20F, 1.0F - geneLevel(player) * 0.04F) : 1.0F;
    }

    public static float criticalDamageMultiplier(ServerPlayer player) {
        int gene = geneLevel(player);
        return gene > 0 && player.getRandom().nextFloat() < Math.min(1.0F, gene * 0.05F)
                ? 1.0F + gene * 0.50F : 1.0F;
    }

    /** Gene level 15 changes the first skill damage instance to void damage. */
    public static boolean convertsSkillDamage(ServerPlayer player) {
        return isCommander(player) && geneLevel(player) >= 15;
    }

    public static void onSkillDamage(ServerPlayer attacker, LivingEntity target,
                                     DamageSource primarySource, float scaledAmount, boolean applied) {
        if (!applied || !isCommander(attacker) || target == attacker || !target.isAlive()) {
            return;
        }
        applyOutgoingDamageEffects(attacker, target);
        if (!target.isAlive()) {
            return;
        }
        int gene = geneLevel(attacker);
        if (gene < 15 || TrainingTargetCompat.recordsDamageWithoutDying(target)
                || !(attacker.level() instanceof ServerLevel level) || scaledAmount <= 0.0F) {
            return;
        }
        float extra = scaledAmount;
        target.invulnerableTime = 0;
        target.hurt(level.damageSources().playerAttack(attacker), extra);
        target.invulnerableTime = 0;
        target.hurt(level.damageSources().magic(), extra);
        target.invulnerableTime = 0;
        target.hurt(level.damageSources().wither(), extra);
        target.invulnerableTime = 0;
        target.hurt(level.damageSources().onFire(), extra);
        target.invulnerableTime = 0;
        target.hurt(level.damageSources().lightningBolt(), extra);
        if (target.isAlive()) {
            target.invulnerableTime = 0;
            target.hurt(level.damageSources().explosion(attacker, attacker), extra);
        }
    }

    public static void onOrdinaryDamage(ServerPlayer attacker, LivingEntity target) {
        if (!isCommander(attacker) || target == attacker || !target.isAlive()) {
            return;
        }
        applyOutgoingDamageEffects(attacker, target);
    }

    private static void applyOutgoingDamageEffects(ServerPlayer attacker, LivingEntity target) {
        int gene = geneLevel(attacker);
        if (gene >= 10) {
            target.addEffect(new MobEffectInstance(ModEffects.STUN, 10, 0, false, true, true));
        }
        if (hasBranchUpgrade(attacker, CommanderBranchUpgrade.SHEPHERD_RESONANT_SONIC)
                || hasBranchUpgrade(attacker, CommanderBranchUpgrade.RAPTOR_COMPOSITE_PULSE)) {
            target.addEffect(new MobEffectInstance(ModEffects.SONIC_SHOCK, 10, 0, false, true, true));
        }
        if (hasBranchUpgrade(attacker, CommanderBranchUpgrade.MORSE_AUTO_FLASH)) {
            target.addEffect(new MobEffectInstance(ModEffects.MORSE_FLASH_BLIND, 25 * 20, 0, false, true, true));
        }
        if (hasBranchUpgrade(attacker, CommanderBranchUpgrade.HACKCLAW_STRONG_INTERFERENCE)) {
            target.addEffect(new MobEffectInstance(ModEffects.HACKCLAW_INTERFERENCE, 8 * 20, 0, false, true, true));
        }
        if (hasBranchUpgrade(attacker, CommanderBranchUpgrade.TOXIK_FIREFLY_AURA)) {
            target.addEffect(new MobEffectInstance(ModEffects.TOXIK_FIREFLY_INTERFERENCE, 60 * 20, 0, false, true, true));
            for (ServerPlayer teammate : DealtTeamManager.onlineTeammates(attacker)) {
                if (teammate.distanceToSqr(attacker) <= 40.0D * 40.0D) {
                    teammate.addEffect(new MobEffectInstance(ModEffects.TOXIK_ADRENALINE, 10 * 20, 0,
                            false, true, true));
                }
            }
            attacker.addEffect(new MobEffectInstance(ModEffects.TOXIK_ADRENALINE, 10 * 20, 0,
                    false, true, true));
        }
        if (hasBranchUpgrade(attacker, CommanderBranchUpgrade.VLINDER_DUST_AURA)) {
            target.addEffect(new MobEffectInstance(ModEffects.VLINDER_MEDICAL_WASTE_INTERFERENCE, 15 * 20,
                    0, false, true, true));
            for (ServerPlayer teammate : DealtTeamManager.onlineTeammates(attacker)) {
                if (teammate.distanceToSqr(attacker) <= 40.0D * 40.0D) {
                    teammate.addEffect(new MobEffectInstance(ModEffects.VLINDER_HEALING_DUST, 15 * 20, 0,
                            false, true, true));
                }
            }
            attacker.addEffect(new MobEffectInstance(ModEffects.VLINDER_HEALING_DUST, 15 * 20, 0,
                    false, true, true));
        }
        if (hasBranchUpgrade(attacker, CommanderBranchUpgrade.RAPTOR_VETERAN_EXPERIENCE)
                && !TrainingTargetCompat.recordsDamageWithoutDying(target)) {
            CompoundTag data = target.getPersistentData();
            int insight = data.getInt("CommanderRaptorInsight") + 1 + attacker.getRandom().nextInt(3);
            if (insight >= 1_000) {
                if (target.isAlive() && attacker.level() instanceof ServerLevel level) {
                    target.invulnerableTime = 0;
                    target.hurt(SkillDamageHelper.source(level,
                            net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK,
                            attacker, attacker), Float.MAX_VALUE);
                }
                data.remove("CommanderRaptorInsight");
            } else {
                data.putInt("CommanderRaptorInsight", insight);
            }
        }
    }

    public static void syncToClient(ServerPlayer player) {
        if (!isCommander(player)) {
            return;
        }
        NetworkHandler.sendToPlayer(S2C_CommanderSnapshot.from(player), player);
    }

    private static void repairChoices(CompoundTag tag) {
        String[] defaults = {
                ModCharacters.D_WOLF_ID, ModCharacters.STINGER_ID, ModCharacters.GIZMO_ID, ModCharacters.LUNA_ID,
                ModCharacters.D_WOLF_ID, ModCharacters.LUNA_ID, ModCharacters.ULURU_ID, ModCharacters.STINGER_ID
        };
        CommanderSkillChoice[] choices = CommanderSkillChoice.values();
        for (int i = 0; i < choices.length; i++) {
            String current = tag.getString(choiceKey(choices[i]));
            if (!isValidSource(current, choices[i])) {
                putChoice(tag, choices[i], choices[i].skillSlot() == SkillSlot.PASSIVE && current.isBlank()
                        ? "" : defaults[i]);
            }
        }
    }

    private static void initializeEnabledPassives(CompoundTag tag) {
        if (tag.getBoolean(ENABLED_PASSIVES_INITIALIZED)) {
            return;
        }
        List<String> initial = new ArrayList<>();
        for (CommanderSkillChoice choice : passiveChoices()) {
            String id = tag.getString(choiceKey(choice));
            if (isValidSource(id, choice) && !initial.contains(id)) {
                initial.add(id);
            }
        }
        putEnabledPassives(tag, initial);
        tag.putBoolean(ENABLED_PASSIVES_INITIALIZED, true);
    }

    private static void putEnabledPassives(CompoundTag tag, List<String> passives) {
        ListTag list = new ListTag();
        passives.stream()
                .filter(id -> isValidSource(id, CommanderSkillChoice.PASSIVE_1))
                .distinct()
                .forEach(id -> list.add(StringTag.valueOf(id)));
        tag.put(ENABLED_PASSIVES, list);
    }

    private static CommanderSkillChoice[] passiveChoices() {
        return new CommanderSkillChoice[] {
                CommanderSkillChoice.PASSIVE_1, CommanderSkillChoice.PASSIVE_2,
                CommanderSkillChoice.PASSIVE_3, CommanderSkillChoice.PASSIVE_4
        };
    }

    private static boolean isValidSource(String id, CommanderSkillChoice choice) {
        if (id == null || id.isBlank()) {
            return false;
        }
        return ModCharacters.get(id)
                .filter(character -> INHERITABLE_ROLES.contains(character.role()))
                .flatMap(character -> character.skill(choice.skillSlot()))
                .map(skill -> skill.implemented())
                .orElse(false);
    }

    private static void putChoice(CompoundTag tag, CommanderSkillChoice choice, String id) {
        tag.putString(choiceKey(choice), id);
    }

    private static String choiceKey(CommanderSkillChoice choice) {
        return "Choice_" + choice.name();
    }

    private static void refreshAttributes(ServerPlayer player) {
        int gene = geneLevel(player);
        normalizeNegativeStepHeightBase(player);
        replaceModifier(player.getAttribute(Attributes.MAX_HEALTH), MAX_HEALTH_MODIFIER,
                "Commander gene max health", gene * 0.20D, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        replaceModifier(player.getAttribute(Attributes.MOVEMENT_SPEED), MOVEMENT_MODIFIER,
                "Commander gene movement", gene * 0.04D, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        replaceModifier(player.getAttribute(Attributes.LUCK), LUCK_MODIFIER,
                "Commander gene luck", gene, AttributeModifier.Operation.ADD_VALUE);
        replaceModifier(player.getAttribute(Attributes.STEP_HEIGHT), STEP_HEIGHT_MODIFIER,
                "Commander step height", STEP_HEIGHT_BONUS, AttributeModifier.Operation.ADD_VALUE);
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    private static void replaceModifier(AttributeInstance attribute, ResourceLocation id, String name, double amount,
                                        AttributeModifier.Operation operation) {
        if (attribute == null) {
            return;
        }
        attribute.removeModifier(id);
        if (amount != 0.0D) {
            attribute.addTransientModifier(new AttributeModifier(id, amount, operation));
        }
    }

    private static void removeAttributeModifiers(ServerPlayer player) {
        removeModifier(player.getAttribute(Attributes.MAX_HEALTH), MAX_HEALTH_MODIFIER);
        removeModifier(player.getAttribute(Attributes.MOVEMENT_SPEED), MOVEMENT_MODIFIER);
        removeModifier(player.getAttribute(Attributes.LUCK), LUCK_MODIFIER);
        removeModifier(player.getAttribute(Attributes.STEP_HEIGHT), STEP_HEIGHT_MODIFIER);
        normalizeNegativeStepHeightBase(player);
        // Attribute removal does not clamp LivingEntity health automatically. A
        // role switch must therefore reduce a Commander-era health value to the
        // new role's actual maximum immediately.
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    private static void removeModifier(AttributeInstance attribute, ResourceLocation id) {
        if (attribute != null) {
            attribute.removeModifier(id);
        }
    }

    private static void normalizeNegativeStepHeightBase(Player player) {
        AttributeInstance stepHeight = player.getAttribute(Attributes.STEP_HEIGHT);
        if (stepHeight != null && stepHeight.getBaseValue() < 0.0D) {
            stepHeight.setBaseValue(0.0D);
        }
        ensureVanillaStepHeight(player);
    }

    private static CompoundTag data(Player player) {
        CompoundTag persistent = player.getPersistentData();
        if (!persistent.contains(ROOT_TAG, Tag.TAG_COMPOUND)) {
            persistent.put(ROOT_TAG, new CompoundTag());
        }
        return persistent.getCompound(ROOT_TAG);
    }

    private static int scalePositiveInt(int base, double multiplier) {
        if (base <= 0) {
            return base;
        }
        double scaled = Math.ceil(base * multiplier);
        return scaled >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) scaled;
    }
}
