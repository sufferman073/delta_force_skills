package com.rzy.dealt_force_skills.character.stinger;

import com.rzy.dealt_force_skills.advancement.DfsAchievements;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.CharacterSelectionManager;
import com.rzy.dealt_force_skills.character.commander.CommanderBranchUpgrade;
import com.rzy.dealt_force_skills.character.commander.CommanderStateManager;
import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.compat.PlayerReviveCompat;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.S2C_SyncStingerState;
import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.skill.SkillCooldownHelper;
import com.rzy.dealt_force_skills.team.DealtTeamManager;
import com.rzy.dealt_force_skills.util.TargetingUtil;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class StingerStateManager {
    public static volatile int SMOKE_COOLDOWN_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("SMOKE_COOLDOWN_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.stinger.stinger_state_manager.smoke_cooldown_ticks", 800));
    public static volatile int DRONE_COOLDOWN_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("DRONE_COOLDOWN_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.stinger.stinger_state_manager.drone_cooldown_ticks", 1100));
    public static volatile int STIM_MAX_CHARGES = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("STIM_MAX_CHARGES", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.stinger.stinger_state_manager.stim_max_charges", 6));
    public static volatile int STIM_RECHARGE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("STIM_RECHARGE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.stinger.stinger_state_manager.stim_recharge_ticks", 500));
    public static volatile int STIM_DURATION_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("STIM_DURATION_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.stinger.stinger_state_manager.stim_duration_ticks", 400));
    public static volatile int DOWNED_DURATION_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("DOWNED_DURATION_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.stinger.stinger_state_manager.downed_duration_ticks", 400));
    public static volatile float STIM_INSTANT_HEAL_FRACTION = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("STIM_INSTANT_HEAL_FRACTION", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.floatValue(
      "characters.stinger.stinger_state_manager.stim_instant_heal_fraction", 0.2F
   ));
    public static volatile float STIM_HEAL_PER_SECOND_FRACTION = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("STIM_HEAL_PER_SECOND_FRACTION", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.floatValue(
      "characters.stinger.stinger_state_manager.stim_heal_per_second_fraction", 0.05F
   ));
    /** Self-rescue becomes available on a fixed 60-second server-time cooldown. */
    public static final int DOWNED_COOLDOWN_TICKS = 60 * 20;
    public static volatile int REVIVE_OTHER_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("REVIVE_OTHER_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.stinger.stinger_state_manager.revive_other_ticks", 60));
    public static volatile int REVIVE_SELF_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("REVIVE_SELF_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.stinger.stinger_state_manager.revive_self_ticks", 20));
    public static volatile double REVIVE_RANGE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("REVIVE_RANGE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("characters.stinger.stinger_state_manager.revive_range", 1.5));
    private static final DustParticleOptions DOWNED_DUST = new DustParticleOptions(new Vector3f(0.28f, 0.86f, 1.0f), 1.25f);

    private static final String ROOT_TAG = DealtForceSkillsMod.MODID + ".stinger";
    private static final String INITIALIZED = "Initialized";
    private static final String SMOKE_COOLDOWN_UNTIL = "SmokeCooldownUntil";
    private static final String DRONE_COOLDOWN_UNTIL = "DroneCooldownUntil";
    private static final String STIM_CHARGES = "StimCharges";
    private static final String STIM_NEXT_RECHARGE = "StimNextRecharge";
    private static final String EQUIPPED_TOOL = "EquippedTool";
    private static final String STIM_MODE = "StimMode";
    private static final String RESCUE_TARGET_ID = "RescueTargetId";
    private static final String RESCUE_TICKS = "RescueTicks";
    private static final String SELF_RESCUE_TICKS = "SelfRescueTicks";
    private static final String RESCUE_COOLDOWN_UNTIL = "RescueCooldownUntil";
    private static final String DOWNED_UNTIL = "DownedUntil";
    private static final String DOWNED_COOLDOWN_UNTIL = "DownedCooldownUntil";
    private static final String EXECUTING_DOWNED_DEATH = "ExecutingDownedDeath";
    private static final String SMOKE_REGEN_OWNER = "SmokeRegenOwner";
    private static final String HIVE_SMOKE_UNTIL = "HiveSmokeUntil";
    private static final String HIVE_SMOKE_LAST_HEAL = "HiveSmokeLastHeal";

    private StingerStateManager() {
    }

    public static boolean isStinger(Player player) {
        Optional<String> selected = CharacterSelectionManager.getSelectedCharacterId(player);
        return selected.isPresent() && ModCharacters.STINGER_ID.equals(selected.get())
                || com.rzy.dealt_force_skills.character.commander.CommanderStateManager
                .inheritsRuntimeSource(player, ModCharacters.STINGER_ID);
    }

    public static void initializeIfNeeded(ServerPlayer player) {
        if (!isStinger(player)) {
            return;
        }

        CompoundTag tag = data(player);
        if (tag.getBoolean(INITIALIZED)) {
            return;
        }

        tag.putBoolean(INITIALIZED, true);
        tag.putLong(SMOKE_COOLDOWN_UNTIL, 0L);
        tag.putLong(DRONE_COOLDOWN_UNTIL, 0L);
        tag.putInt(STIM_CHARGES, stimCapacity(player));
        tag.putLong(STIM_NEXT_RECHARGE, 0L);
        tag.putInt(EQUIPPED_TOOL, StingerTool.NONE.ordinal());
        tag.putInt(STIM_MODE, StingerStimMode.HEAL.ordinal());
        tag.putInt(RESCUE_TARGET_ID, -1);
        tag.putInt(RESCUE_TICKS, 0);
        tag.putInt(SELF_RESCUE_TICKS, 0);
        tag.putLong(RESCUE_COOLDOWN_UNTIL, 0L);
    }

    public static void copyState(Player original, Player target) {
        CompoundTag originalData = original.getPersistentData();
        if (originalData.contains(ROOT_TAG, Tag.TAG_COMPOUND)) {
            target.getPersistentData().put(ROOT_TAG, originalData.getCompound(ROOT_TAG).copy());
        }
    }

    public static void clearState(Player player) {
        player.getPersistentData().remove(ROOT_TAG);
    }

    public static void tick(ServerPlayer player) {
        if (!isStinger(player)) {
            return;
        }
        initializeIfNeeded(player);
        long now = SkillCooldownHelper.now(player);
        rechargeStim(player, now);
        tickRescue(player);
        tickDowned(player);
    }

    public static void tickDowned(ServerPlayer player) {
        CompoundTag tag = data(player);
        long until = tag.getLong(DOWNED_UNTIL);
        if (until <= 0L) {
            if (player.hasEffect(ModEffects.STINGER_DOWNED.get())) {
                player.removeEffect(ModEffects.STINGER_DOWNED.get());
            }
            return;
        }

        int remaining = (int) Math.min(Integer.MAX_VALUE, until - SkillCooldownHelper.now(player));
        if (remaining <= 0) {
            expireDowned(player);
            return;
        }

        MobEffectInstance current = player.getEffect(ModEffects.STINGER_DOWNED.get());
        if (current == null || current.getDuration() < remaining - 5) {
            player.addEffect(new MobEffectInstance(ModEffects.STINGER_DOWNED.get(),
                    remaining, com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.stinger.stinger_state_manager.effect.stinger_downed.0.amplifier", 0), false, true, true));
        }
        if (player.getHealth() < 1.0f) {
            player.setHealth(1.0f);
        }
        player.setSprinting(false);
        player.stopUsingItem();
        if (player.containerMenu != player.inventoryMenu) {
            player.closeContainer();
        }
        spawnDownedParticles(player);
    }

    public static void tickSynchronizedDowned(ServerPlayer player) {
        tickDowned(player);
        tickRescue(player);
    }

    public static boolean shouldEnterDowned(ServerPlayer player, float incomingDamage) {
        return incomingDamage >= player.getHealth() && canEnterDowned(player);
    }

    public static boolean canEnterDowned(ServerPlayer player) {
        return !isDowned(player)
                && !isExecutingDownedDeath(player)
                && downedCooldownReady(player)
                && hasStingerProtection(player);
    }

    public static void enterDowned(ServerPlayer player) {
        CompoundTag tag = data(player);
        long now = SkillCooldownHelper.now(player);
        long until = now + DOWNED_DURATION_TICKS;
        tag.putLong(DOWNED_UNTIL, until);
        tag.putLong(DOWNED_COOLDOWN_UNTIL, now + DOWNED_COOLDOWN_TICKS);
        tag.putInt(SELF_RESCUE_TICKS, 0);
        tag.putInt(RESCUE_TARGET_ID, -1);
        tag.putInt(RESCUE_TICKS, 0);
        player.setHealth(Math.max(1.0f, Math.min(player.getHealth(), player.getMaxHealth())));
        player.clearFire();
        player.addEffect(new MobEffectInstance(ModEffects.STINGER_DOWNED.get(),
                DOWNED_DURATION_TICKS, com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.stinger.stinger_state_manager.effect.stinger_downed.1.amplifier", 0), false, true, true));
        player.displayClientMessage(Component.translatable("message.dealt_force_skills.stinger.downed"), true);
        ServerPlayer smokeOwner = smokeRegenOwner(player);
        if (smokeOwner != null && player.hasEffect(ModEffects.STINGER_SMOKE_REGEN.get())) {
            DfsAchievements.recordStingerSmokeConvertedSave(smokeOwner, player);
        }
    }

    public static void clearDowned(ServerPlayer player, boolean fullHeal) {
        CompoundTag tag = data(player);
        tag.putLong(DOWNED_UNTIL, 0L);
        tag.putLong(DOWNED_COOLDOWN_UNTIL,
                SkillCooldownHelper.now(player) + DOWNED_COOLDOWN_TICKS);
        tag.putInt(SELF_RESCUE_TICKS, 0);
        tag.putInt(RESCUE_TARGET_ID, -1);
        tag.putInt(RESCUE_TICKS, 0);
        player.removeEffect(ModEffects.STINGER_DOWNED.get());
        if (fullHeal) {
            player.setHealth(player.getMaxHealth());
            player.clearFire();
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.stinger.stinger_state_manager.effect.regeneration.2.duration_ticks", 4 * 20), com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.stinger.stinger_state_manager.effect.regeneration.2.amplifier", 0), false, true, true));
        }
        player.displayClientMessage(Component.translatable("message.dealt_force_skills.stinger.revived"), true);
    }

    public static void clearDownedOnDeath(ServerPlayer player) {
        CompoundTag tag = data(player);
        tag.putLong(DOWNED_UNTIL, 0L);
        tag.putInt(SELF_RESCUE_TICKS, 0);
        tag.putInt(RESCUE_TARGET_ID, -1);
        tag.putInt(RESCUE_TICKS, 0);
        player.removeEffect(ModEffects.STINGER_DOWNED.get());
    }

    public static boolean isDowned(Player player) {
        return data(player).getLong(DOWNED_UNTIL) > SkillCooldownHelper.now(player);
    }

    public static boolean isExecutingDownedDeath(Player player) {
        return data(player).getBoolean(EXECUTING_DOWNED_DEATH);
    }

    public static int downedRemainingTicks(Player player) {
        long remaining = data(player).getLong(DOWNED_UNTIL) - SkillCooldownHelper.now(player);
        return remaining > 0L ? (int) Math.min(Integer.MAX_VALUE, remaining) : 0;
    }

    public static boolean hasStingerProtection(Player player) {
        return player.hasEffect(ModEffects.STINGER_STIM_HEAL.get())
                || player.hasEffect(ModEffects.STINGER_SMOKE_REGEN.get())
                || CommanderStateManager.hasBranchUpgrade(player, CommanderBranchUpgrade.TOXIK_SYNC_RESCUE)
                && !player.getActiveEffects().isEmpty();
    }

    public static boolean smokeReady(Player player) {
        return SkillCooldownHelper.now(player) >= data(player).getLong(SMOKE_COOLDOWN_UNTIL);
    }

    public static int smokeCooldownRemainingTicks(Player player) {
        return remainingTicks(player, SMOKE_COOLDOWN_UNTIL);
    }

    public static boolean consumeSmoke(ServerPlayer player) {
        CompoundTag tag = data(player);
        long now = SkillCooldownHelper.now(player);
        if (now < tag.getLong(SMOKE_COOLDOWN_UNTIL)) {
            return false;
        }
        tag.putLong(SMOKE_COOLDOWN_UNTIL, SkillCooldownHelper.until(player, now, SMOKE_COOLDOWN_TICKS));
        return true;
    }

    public static boolean droneReady(Player player) {
        return SkillCooldownHelper.now(player) >= data(player).getLong(DRONE_COOLDOWN_UNTIL);
    }

    public static int droneCooldownRemainingTicks(Player player) {
        return remainingTicks(player, DRONE_COOLDOWN_UNTIL);
    }

    public static boolean consumeDrone(ServerPlayer player) {
        CompoundTag tag = data(player);
        long now = SkillCooldownHelper.now(player);
        if (now < tag.getLong(DRONE_COOLDOWN_UNTIL)) {
            return false;
        }
        tag.putLong(DRONE_COOLDOWN_UNTIL, SkillCooldownHelper.until(player, now, DRONE_COOLDOWN_TICKS));
        return true;
    }

    public static int stimCharges(Player player) {
        return Math.min(stimCapacity(player), data(player).getInt(STIM_CHARGES));
    }

    public static int stimRechargeRemainingTicks(Player player) {
        if (stimCharges(player) >= stimCapacity(player)) {
            return 0;
        }
        return remainingTicks(player, STIM_NEXT_RECHARGE);
    }

    public static boolean consumeStimCharge(ServerPlayer player) {
        CompoundTag tag = data(player);
        int capacity = stimCapacity(player);
        int charges = Math.min(capacity, tag.getInt(STIM_CHARGES));
        if (charges <= 0) {
            return false;
        }
        tag.putInt(STIM_CHARGES, charges - 1);
        if (charges - 1 < capacity && tag.getLong(STIM_NEXT_RECHARGE) <= 0L) {
            tag.putLong(STIM_NEXT_RECHARGE,
                    SkillCooldownHelper.until(player, SkillCooldownHelper.now(player), STIM_RECHARGE_TICKS));
        }
        return true;
    }

    public static StingerTool equippedTool(Player player) {
        int ordinal = data(player).getInt(EQUIPPED_TOOL);
        StingerTool[] tools = StingerTool.values();
        return ordinal >= 0 && ordinal < tools.length ? tools[ordinal] : StingerTool.NONE;
    }

    public static void setEquippedTool(Player player, StingerTool tool) {
        data(player).putInt(EQUIPPED_TOOL, tool.ordinal());
    }

    public static StingerStimMode stimMode(Player player) {
        int ordinal = data(player).getInt(STIM_MODE);
        StingerStimMode[] modes = StingerStimMode.values();
        return ordinal >= 0 && ordinal < modes.length ? modes[ordinal] : StingerStimMode.HEAL;
    }

    public static void toggleStimMode(Player player) {
        StingerStimMode next = stimMode(player) == StingerStimMode.HEAL
                ? StingerStimMode.SUPPRESS
                : StingerStimMode.HEAL;
        data(player).putInt(STIM_MODE, next.ordinal());
    }

    public static int rescueTicks(Player player) {
        return Math.max(data(player).getInt(RESCUE_TICKS), data(player).getInt(SELF_RESCUE_TICKS));
    }

    public static int rescueRequiredTicks(Player player) {
        if (isDowned(player)) {
            return CommanderStateManager.hasBranchUpgrade(player, CommanderBranchUpgrade.STINGER_EXTREME_EFFICIENCY)
                    ? 1 : REVIVE_SELF_TICKS;
        }
        return data(player).getInt(RESCUE_TICKS) > 0
                ? (CommanderStateManager.hasBranchUpgrade(player, CommanderBranchUpgrade.STINGER_EXTREME_EFFICIENCY)
                ? Math.max(1, REVIVE_OTHER_TICKS / 2) : REVIVE_OTHER_TICKS) : 0;
    }

    public static int rescueCooldownRemainingTicks(Player player) {
        return remainingTicks(player, RESCUE_COOLDOWN_UNTIL);
    }

    public static boolean rescueCooldownReady(Player player) {
        return SkillCooldownHelper.now(player) >= data(player).getLong(RESCUE_COOLDOWN_UNTIL);
    }

    public static void markRescueCooldown(ServerPlayer player) {
        if (player != null) {
            data(player).putLong(RESCUE_COOLDOWN_UNTIL,
                    SkillCooldownHelper.now(player) + DOWNED_COOLDOWN_TICKS);
        }
    }

    public static void applyStimHeal(LivingEntity target) {
        applyStimHeal(target, false);
    }

    public static void applyStimHeal(LivingEntity target, boolean doubled) {
        applyStimHeal(null, target, doubled);
    }

    public static void applyStimHeal(LivingEntity source, LivingEntity target, boolean doubled) {
        // Instant 20% max-health restore, then ongoing 5%/s via STINGER_STIM_HEAL effect.
        int effectMultiplier = doubled ? 2 : 1;
        LivingEntity departmentSource=source==null?target:source;
        com.rzy.dealt_force_skills.department.DepartmentHealing.source(target,"stinger_stim",departmentSource);
        if (!target.level().isClientSide && target.getHealth() > 0.0F) {
            com.rzy.dealt_force_skills.department.DepartmentHealing.heal(departmentSource,target,target.getMaxHealth() * Math.max(0.0F, STIM_INSTANT_HEAL_FRACTION) * effectMultiplier
                    * (float)(com.rzy.dealt_force_skills.ai.operative.OperativeGrowth.commander((net.minecraft.world.entity.Entity) source)
                        ? com.rzy.dealt_force_skills.ai.operative.OperativeGrowth.commanderEffect() : 1));
        }
        int healAmplifier = com.rzy.dealt_force_skills.config.DealtForceConfig.intValue(
                "characters.stinger.stinger_state_manager.effect.stinger_stim_heal.3.amplifier", 0);
        int absorptionAmplifier = com.rzy.dealt_force_skills.config.DealtForceConfig.intValue(
                "characters.stinger.stinger_state_manager.effect.absorption.5.amplifier", 1);
        target.addEffect(new MobEffectInstance(ModEffects.STINGER_STIM_HEAL.get(),
                com.rzy.dealt_force_skills.ai.operative.OperativeGrowth.duration(source, STIM_DURATION_TICKS * effectMultiplier),
                com.rzy.dealt_force_skills.ai.operative.OperativeGrowth.amplifier(source, (healAmplifier + 1) * effectMultiplier - 1), false, true, true));
        target.addEffect(new MobEffectInstance(MobEffects.ABSORPTION,
                com.rzy.dealt_force_skills.ai.operative.OperativeGrowth.duration(source, STIM_DURATION_TICKS * effectMultiplier),
                com.rzy.dealt_force_skills.ai.operative.OperativeGrowth.amplifier(source, (absorptionAmplifier + 1) * effectMultiplier - 1), false, true, true));
        for (MobEffectInstance effect : new ArrayList<>(target.getActiveEffects())) {
            if (effect.getEffect().getCategory() == MobEffectCategory.HARMFUL) {
                target.removeEffect(effect.getEffect());
            }
        }
    }

    public static void applySmokeRegen(ServerPlayer target) {
        applySmokeRegen(null, target, false);
    }

    public static void applySmokeRegen(ServerPlayer owner, ServerPlayer target) {
        applySmokeRegen(owner, target, false);
    }

    public static void applySmokeRegen(ServerPlayer owner, ServerPlayer target, boolean hiveSmoke) {
        if (owner != null) {
            data(target).putUUID(SMOKE_REGEN_OWNER, owner.getUUID());
        }
        if (hiveSmoke) {
            CompoundTag targetData = data(target);
            long now = SkillCooldownHelper.now(target);
            targetData.putLong(HIVE_SMOKE_UNTIL, now + 40L);
            if (now >= targetData.getLong(HIVE_SMOKE_LAST_HEAL) + 20L) {
                com.rzy.dealt_force_skills.department.DepartmentHealing.heal(owner,target,Math.max(1.0F, target.getMaxHealth() * 0.10F));
                targetData.putLong(HIVE_SMOKE_LAST_HEAL, now);
            }
        }
        int effectDuration = hiveSmoke ? 40 : com.rzy.dealt_force_skills.config.DealtForceConfig.intValue(
                "characters.stinger.stinger_state_manager.effect.stinger_smoke_regen.6.duration_ticks", 30);
        int effectAmplifier = com.rzy.dealt_force_skills.config.DealtForceConfig.intValue(
                "characters.stinger.stinger_state_manager.effect.stinger_smoke_regen.6.amplifier", 0);
        if (owner != null) {
            effectDuration = CommanderStateManager.skillDuration(owner, effectDuration);
            effectAmplifier = CommanderStateManager.skillEffectAmplifier(owner, effectAmplifier);
        }
        target.addEffect(new MobEffectInstance(ModEffects.STINGER_SMOKE_REGEN.get(),
                effectDuration, effectAmplifier, false, true, true));
        if (!hiveSmoke) {
            int regenerationDuration = com.rzy.dealt_force_skills.config.DealtForceConfig.intValue(
                    "characters.stinger.stinger_state_manager.effect.regeneration.7.duration_ticks", 30);
            int regenerationAmplifier = com.rzy.dealt_force_skills.config.DealtForceConfig.intValue(
                    "characters.stinger.stinger_state_manager.effect.regeneration.7.amplifier", 0);
            if (owner != null) {
                regenerationDuration = CommanderStateManager.skillDuration(owner, regenerationDuration);
                regenerationAmplifier = CommanderStateManager.skillEffectAmplifier(owner, regenerationAmplifier);
            }
            target.addEffect(new MobEffectInstance(MobEffects.REGENERATION,
                    regenerationDuration, regenerationAmplifier, false, true, true));
        } else {
            int hiveDuration = owner == null ? 40 : CommanderStateManager.skillDuration(owner, 40);
            int adrenalineAmplifier = owner == null ? 4 : CommanderStateManager.skillEffectAmplifier(owner, 4);
            int damageAmplifier = owner == null ? 9 : CommanderStateManager.skillEffectAmplifier(owner, 9);
            target.addEffect(new MobEffectInstance(ModEffects.TOXIK_ADRENALINE.get(),
                    hiveDuration, adrenalineAmplifier, false, true, true));
            target.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST,
                    hiveDuration, damageAmplifier, false, true, true));
        }
    }

    public static boolean isHiveSmokeActive(ServerPlayer player) {
        long until = player == null ? 0L : data(player).getLong(HIVE_SMOKE_UNTIL);
        return player != null && until > 0L && SkillCooldownHelper.now(player) <= until;
    }

    private static ServerPlayer smokeRegenOwner(ServerPlayer target) {
        CompoundTag tag = data(target);
        if (!tag.hasUUID(SMOKE_REGEN_OWNER)) {
            return null;
        }
        return target.server.getPlayerList().getPlayer(tag.getUUID(SMOKE_REGEN_OWNER));
    }

    public static void applyStimSuppression(LivingEntity target) {
        applyStimSuppression(target, false);
    }

    public static void applyStimSuppression(LivingEntity target, boolean doubled) {
        applyStimSuppression(null, target, doubled);
    }

    public static void applyStimSuppression(LivingEntity source, LivingEntity target, boolean doubled) {
        int effectMultiplier = doubled ? 2 : 1;
        int amplifier = com.rzy.dealt_force_skills.config.DealtForceConfig.intValue(
                "characters.stinger.stinger_state_manager.effect.stinger_stim_suppression.8.amplifier", 0);
        target.addEffect(new MobEffectInstance(ModEffects.STINGER_STIM_SUPPRESSION.get(),
                com.rzy.dealt_force_skills.ai.operative.OperativeGrowth.duration(source, STIM_DURATION_TICKS * effectMultiplier),
                com.rzy.dealt_force_skills.ai.operative.OperativeGrowth.amplifier(source, (amplifier + 1) * effectMultiplier - 1), false, true, true));
    }

    public static boolean hasStimSuppression(LivingEntity target) {
        return target.hasEffect(ModEffects.STINGER_STIM_SUPPRESSION.get());
    }

    public static void syncToClient(ServerPlayer player) {
        if (!isStinger(player)) {
            return;
        }
        initializeIfNeeded(player);
        NetworkHandler.sendToPlayer(new S2C_SyncStingerState(
                smokeCooldownRemainingTicks(player),
                droneCooldownRemainingTicks(player),
                stimCharges(player),
                stimCapacity(player),
                stimRechargeRemainingTicks(player),
                equippedTool(player).ordinal(),
                stimMode(player).ordinal(),
                rescueTicks(player),
                rescueRequiredTicks(player),
                rescueCooldownRemainingTicks(player),
                downedRemainingTicks(player),
                downedMarkers(player)
        ), player);
    }

    private static void rechargeStim(ServerPlayer player, long now) {
        CompoundTag tag = data(player);
        int capacity = stimCapacity(player);
        int charges = Math.min(capacity, Math.max(0, tag.getInt(STIM_CHARGES)));
        tag.putInt(STIM_CHARGES, charges);
        if (charges >= capacity) {
            tag.putLong(STIM_NEXT_RECHARGE, 0L);
            return;
        }
        long next = tag.getLong(STIM_NEXT_RECHARGE);
        if (next <= 0L) {
            tag.putLong(STIM_NEXT_RECHARGE, SkillCooldownHelper.until(player, now, STIM_RECHARGE_TICKS));
            return;
        }
        while (charges < capacity && now >= next) {
            charges++;
            next += SkillCooldownHelper.ticks(player, STIM_RECHARGE_TICKS);
        }
        tag.putInt(STIM_CHARGES, charges);
        tag.putLong(STIM_NEXT_RECHARGE, charges >= capacity ? 0L : next);
    }

    private static int stimCapacity(Player player) {
        return CommanderStateManager.skillCapacity(player, STIM_MAX_CHARGES);
    }

    private static void tickRescue(ServerPlayer player) {
        CompoundTag tag = data(player);
        if (!rescueCooldownReady(player)) {
            resetRescue(tag);
            return;
        }
        if (!player.isShiftKeyDown()) {
            resetRescue(tag);
            return;
        }

        if (isDowned(player)) {
            int ticks = tag.getInt(SELF_RESCUE_TICKS) + 1;
            tag.putInt(SELF_RESCUE_TICKS, ticks);
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.stinger.self_rescue",
                    progressBar(ticks, REVIVE_SELF_TICKS)), true);
            if (ticks >= REVIVE_SELF_TICKS) {
                clearDowned(player, true);
                markRescueCooldown(player);
                DfsAchievements.onRescue(player, player, true, "stinger");
            }
            return;
        }

        ServerPlayer target = nearestDownedPlayer(player);
        if (target == null) {
            resetRescue(tag);
            return;
        }

        int targetId = target.getId();
        int ticks = tag.getInt(RESCUE_TARGET_ID) == targetId ? tag.getInt(RESCUE_TICKS) + 1 : 1;
        tag.putInt(RESCUE_TARGET_ID, targetId);
        tag.putInt(RESCUE_TICKS, ticks);
        player.displayClientMessage(Component.translatable("message.dealt_force_skills.stinger.rescuing",
                target.getDisplayName(), progressBar(ticks, REVIVE_OTHER_TICKS)), true);
        if (ticks >= REVIVE_OTHER_TICKS) {
            boolean revived = false;
            if (PlayerReviveCompat.isBleeding(target)) {
                revived = PlayerReviveCompat.revive(target, true);
            }
            if (isDowned(target)) {
                clearDowned(target, true);
                revived = true;
            }
            if (revived) {
                markRescueCooldown(player);
                markRescueCooldown(target);
                DfsAchievements.onRescue(player, target, false, "stinger");
                resetRescue(tag);
                player.displayClientMessage(Component.translatable("message.dealt_force_skills.stinger.rescue_done",
                        target.getDisplayName()), true);
            }
        }
    }

    private static ServerPlayer nearestDownedPlayer(ServerPlayer player) {
        double rangeSqr = REVIVE_RANGE * REVIVE_RANGE;
        return player.server.getPlayerList().getPlayers().stream()
                .filter(target -> target != player && target.level() == player.level())
                .filter(target -> DealtTeamManager.areTeammates(player, target))
                .filter(target -> TargetingUtil.isTargetablePlayer(target) && isRescuableDowned(target))
                .filter(target -> target.distanceToSqr(player) <= rangeSqr)
                .min(Comparator.comparingDouble(player::distanceToSqr))
                .orElse(null);
    }

    private static List<StingerDownedMarker> downedMarkers(ServerPlayer stinger) {
        List<StingerDownedMarker> markers = new ArrayList<>();
        for (ServerPlayer player : stinger.server.getPlayerList().getPlayers()) {
            if (player.level() == stinger.level() && DealtTeamManager.areTeammates(stinger, player) && isRescuableDowned(player)) {
                markers.add(new StingerDownedMarker(player.getId(), player.position(), downedRemainingTicksForMarker(player)));
            }
        }
        markers.sort(Comparator.comparingDouble(marker -> marker.position().distanceToSqr(stinger.position())));
        return markers;
    }

    private static boolean isRescuableDowned(ServerPlayer player) {
        return isDowned(player) || PlayerReviveCompat.isBleeding(player);
    }

    private static int downedRemainingTicksForMarker(ServerPlayer player) {
        return Math.max(downedRemainingTicks(player), PlayerReviveCompat.downedRemainingTicks(player));
    }

    private static boolean downedCooldownReady(Player player) {
        return SkillCooldownHelper.now(player) >= data(player).getLong(DOWNED_COOLDOWN_UNTIL);
    }

    private static void expireDowned(ServerPlayer player) {
        CompoundTag tag = data(player);
        tag.putLong(DOWNED_UNTIL, 0L);
        tag.putBoolean(EXECUTING_DOWNED_DEATH, true);
        player.removeEffect(ModEffects.STINGER_DOWNED.get());
        try {
            player.hurt(player.damageSources().genericKill(), Float.MAX_VALUE);
        } finally {
            tag.remove(EXECUTING_DOWNED_DEATH);
        }
    }

    private static void spawnDownedParticles(ServerPlayer player) {
        if (player.tickCount % 4 != 0 || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        level.sendParticles(DOWNED_DUST,
                player.getX(),
                player.getY() + 1.05D,
                player.getZ(),
                8,
                0.42D,
                0.55D,
                0.42D,
                0.0D);
    }

    private static void resetRescue(CompoundTag tag) {
        tag.putInt(RESCUE_TARGET_ID, -1);
        tag.putInt(RESCUE_TICKS, 0);
        tag.putInt(SELF_RESCUE_TICKS, 0);
    }

    private static String progressBar(int ticks, int required) {
        int filled = Math.max(0, Math.min(10, ticks * 10 / Math.max(1, required)));
        return "[" + "#".repeat(filled) + ".".repeat(10 - filled) + "]";
    }

    private static int remainingTicks(Player player, String key) {
        return SkillCooldownHelper.remainingTicks(player, data(player).getLong(key));
    }

    private static CompoundTag data(Player player) {
        CompoundTag persistent = player.getPersistentData();
        if (!persistent.contains(ROOT_TAG, Tag.TAG_COMPOUND)) {
            persistent.put(ROOT_TAG, new CompoundTag());
        }
        CompoundTag tag = persistent.getCompound(ROOT_TAG);
        if (player instanceof ServerPlayer) {
            SkillCooldownHelper.clampCooldown(player, tag, SMOKE_COOLDOWN_UNTIL, SMOKE_COOLDOWN_TICKS);
            SkillCooldownHelper.clampCooldown(player, tag, DRONE_COOLDOWN_UNTIL, DRONE_COOLDOWN_TICKS);
            SkillCooldownHelper.clampCooldown(player, tag, STIM_NEXT_RECHARGE, STIM_RECHARGE_TICKS);
            SkillCooldownHelper.clampDeadline(tag, RESCUE_COOLDOWN_UNTIL, SkillCooldownHelper.now(player), DOWNED_COOLDOWN_TICKS);
            SkillCooldownHelper.clampDeadline(tag, DOWNED_COOLDOWN_UNTIL, SkillCooldownHelper.now(player), DOWNED_COOLDOWN_TICKS);
        }
        return tag;
    }
}
