package com.rzy.dealt_force_skills.character.luna;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.CharacterSelectionManager;
import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.character.commander.CommanderBranchUpgrade;
import com.rzy.dealt_force_skills.character.commander.CommanderStateManager;
import com.rzy.dealt_force_skills.entity.LunaCompositeGrenadeEntity;
import com.rzy.dealt_force_skills.entity.LunaShockArrowEntity;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.S2C_LunaRevealEntities;
import com.rzy.dealt_force_skills.network.S2C_SyncLunaState;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.skill.SkillCooldownHelper;
import com.rzy.dealt_force_skills.skill.SkillDamageHelper;
import com.rzy.dealt_force_skills.util.RangedSoundHelper;
import com.rzy.dealt_force_skills.util.ReconRevealThrottle;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import java.util.List;
import java.util.Optional;

public final class LunaStateManager {
    public static volatile int SHOCK_ARROW_MAX_CHARGES = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("SHOCK_ARROW_MAX_CHARGES", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.luna.luna_state_manager.shock_arrow_max_charges", 2));
    public static volatile int SHOCK_ARROW_RECHARGE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("SHOCK_ARROW_RECHARGE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.luna.luna_state_manager.shock_arrow_recharge_ticks", 600));
    public static volatile int COMPOSITE_GRENADE_MAX_CHARGES = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("COMPOSITE_GRENADE_MAX_CHARGES", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.luna.luna_state_manager.composite_grenade_max_charges", 2));
    public static volatile int COMPOSITE_GRENADE_RECHARGE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("COMPOSITE_GRENADE_RECHARGE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue(
      "characters.luna.luna_state_manager.composite_grenade_recharge_ticks", 600
   ));
    public static volatile int COMPOSITE_GRENADE_FUSE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("COMPOSITE_GRENADE_FUSE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.luna.luna_state_manager.composite_grenade_fuse_ticks", 100));
    public static volatile int RECON_ARROW_COOLDOWN_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("RECON_ARROW_COOLDOWN_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.luna.luna_state_manager.recon_arrow_cooldown_ticks", 900));
    public static volatile int MAX_BOW_CHARGE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("MAX_BOW_CHARGE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.luna.luna_state_manager.max_bow_charge_ticks", 14));
    public static volatile int SKILL_REVEAL_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("SKILL_REVEAL_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.luna.luna_state_manager.skill_reveal_ticks", 30));
    public static volatile int PASSIVE_REVEAL_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("PASSIVE_REVEAL_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.luna.luna_state_manager.passive_reveal_ticks", 20));
    public static volatile int RECON_REVEAL_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("RECON_REVEAL_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.luna.luna_state_manager.recon_reveal_ticks", 40));
    private static final String ROOT_TAG = DealtForceSkillsMod.MODID + ".luna";
    private static final String INITIALIZED = "Initialized";
    private static final String SHOCK_CHARGES = "ShockCharges";
    private static final String SHOCK_NEXT_RECHARGE = "ShockNextRecharge";
    private static final String GRENADE_CHARGES = "GrenadeCharges";
    private static final String GRENADE_NEXT_RECHARGE = "GrenadeNextRecharge";
    private static final String CORE_COOLDOWN_UNTIL = "CoreCooldownUntil";
    private static final String EQUIPPED_TOOL = "EquippedTool";
    private static final String GRENADE_COOK_START = "GrenadeCookStart";
    private static final String SHOCK_BOUNCE_ENABLED = "ShockBounceEnabled";
    private static final String WEAK_POINT_UNTIL = DealtForceSkillsMod.MODID + ".luna_weak_point_until";
    private static final String WEAK_POINT_STACKS = DealtForceSkillsMod.MODID + ".luna_weak_point_stacks";

    private LunaStateManager() {
    }

    public static boolean isLuna(Player player) {
        Optional<String> selected = CharacterSelectionManager.getSelectedCharacterId(player);
        return selected.isPresent() && ModCharacters.LUNA_ID.equals(selected.get())
                || com.rzy.dealt_force_skills.character.commander.CommanderStateManager
                .inheritsRuntimeSource(player, ModCharacters.LUNA_ID);
    }

    public static void initializeIfNeeded(ServerPlayer player) {
        if (!isLuna(player)) {
            return;
        }

        CompoundTag tag = data(player);
        if (tag.getBoolean(INITIALIZED)) {
            return;
        }

        tag.putBoolean(INITIALIZED, true);
        tag.putInt(SHOCK_CHARGES, shockCapacity(player));
        tag.putLong(SHOCK_NEXT_RECHARGE, 0L);
        tag.putInt(GRENADE_CHARGES, grenadeCapacity(player));
        tag.putLong(GRENADE_NEXT_RECHARGE, 0L);
        tag.putLong(CORE_COOLDOWN_UNTIL, 0L);
        tag.putInt(EQUIPPED_TOOL, LunaTool.NONE.ordinal());
        tag.putLong(GRENADE_COOK_START, 0L);
        tag.putBoolean(SHOCK_BOUNCE_ENABLED, false);
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
        if (!isLuna(player)) {
            return;
        }

        initializeIfNeeded(player);
        long now = SkillCooldownHelper.now(player);
        recharge(player, now, SHOCK_CHARGES, shockCapacity(player), SHOCK_NEXT_RECHARGE, SHOCK_ARROW_RECHARGE_TICKS);
        recharge(player, now, GRENADE_CHARGES, grenadeCapacity(player), GRENADE_NEXT_RECHARGE, COMPOSITE_GRENADE_RECHARGE_TICKS);
    }

    public static int shockCharges(Player player) {
        return Math.min(shockCapacity(player), data(player).getInt(SHOCK_CHARGES));
    }

    public static int shockRechargeRemainingTicks(Player player) {
        if (shockCharges(player) >= shockCapacity(player)) {
            return 0;
        }
        return remainingTicks(player, SHOCK_NEXT_RECHARGE);
    }

    public static boolean consumeShockCharge(ServerPlayer player) {
        return consumeCharge(player, SHOCK_CHARGES, shockCapacity(player), SHOCK_NEXT_RECHARGE, SHOCK_ARROW_RECHARGE_TICKS);
    }

    public static int grenadeCharges(Player player) {
        return Math.min(grenadeCapacity(player), data(player).getInt(GRENADE_CHARGES));
    }

    public static int grenadeRechargeRemainingTicks(Player player) {
        if (grenadeCharges(player) >= grenadeCapacity(player)) {
            return 0;
        }
        return remainingTicks(player, GRENADE_NEXT_RECHARGE);
    }

    public static boolean consumeGrenadeCharge(ServerPlayer player) {
        return consumeCharge(player, GRENADE_CHARGES, grenadeCapacity(player),
                GRENADE_NEXT_RECHARGE, COMPOSITE_GRENADE_RECHARGE_TICKS);
    }

    public static LunaTool equippedTool(Player player) {
        int ordinal = data(player).getInt(EQUIPPED_TOOL);
        LunaTool[] values = LunaTool.values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : LunaTool.NONE;
    }

    public static void setEquippedTool(Player player, LunaTool tool) {
        data(player).putInt(EQUIPPED_TOOL, tool.ordinal());
        if (tool != LunaTool.COMPOSITE_GRENADE) {
            clearGrenadeCook(player);
        }
    }

    public static boolean shockBounceEnabled(Player player) {
        return data(player).getBoolean(SHOCK_BOUNCE_ENABLED);
    }

    public static void toggleShockBounce(Player player) {
        data(player).putBoolean(SHOCK_BOUNCE_ENABLED, !shockBounceEnabled(player));
    }

    public static void startGrenadeCook(Player player) {
        CompoundTag tag = data(player);
        if (tag.getLong(GRENADE_COOK_START) <= 0L) {
            tag.putLong(GRENADE_COOK_START, SkillCooldownHelper.now(player));
        }
    }

    public static void clearGrenadeCook(Player player) {
        data(player).putLong(GRENADE_COOK_START, 0L);
    }

    public static int grenadeCookTicks(Player player) {
        long start = data(player).getLong(GRENADE_COOK_START);
        if (start <= 0L || equippedTool(player) != LunaTool.COMPOSITE_GRENADE) {
            return 0;
        }
        long cooked = SkillCooldownHelper.now(player) - start;
        return cooked > 0L ? (int) Math.min(Integer.MAX_VALUE, cooked) : 0;
    }

    public static boolean isCoreReady(Player player) {
        return SkillCooldownHelper.now(player) >= data(player).getLong(CORE_COOLDOWN_UNTIL);
    }

    public static int coreCooldownRemainingTicks(Player player) {
        return remainingTicks(player, CORE_COOLDOWN_UNTIL);
    }

    public static void setCoreCooldown(ServerPlayer player) {
        data(player).putLong(CORE_COOLDOWN_UNTIL,
                SkillCooldownHelper.until(player, SkillCooldownHelper.now(player), RECON_ARROW_COOLDOWN_TICKS));
    }

    public static void handleDamageReveal(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide || event.getAmount() <= 0.0f) {
            return;
        }

        Entity attacker = event.getSource().getEntity();
        if (!(attacker instanceof ServerPlayer player) || !isLuna(player) || attacker == event.getEntity()) {
            return;
        }

        boolean skillDamage = isLunaSkillDamage(event.getSource());
        if (revealToOwner(player, event.getEntity(), skillDamage ? SKILL_REVEAL_TICKS : PASSIVE_REVEAL_TICKS, true,
                CommanderStateManager.hasBranchUpgrade(player, CommanderBranchUpgrade.LUNA_ULTIMATE_RECON),
                CommanderStateManager.hasBranchUpgrade(player, CommanderBranchUpgrade.LUNA_WEAK_POINT), true))
            com.rzy.dealt_force_skills.department.DepartmentTasks.signal(player,"luna.passive.reveal");
    }

    public static void revealToOwner(ServerPlayer owner, LivingEntity target, int ticks, boolean voiceIfPlayer) {
        revealToOwner(owner, target, ticks, voiceIfPlayer,
                CommanderStateManager.hasBranchUpgrade(owner, CommanderBranchUpgrade.LUNA_ULTIMATE_RECON),
                CommanderStateManager.hasBranchUpgrade(owner, CommanderBranchUpgrade.LUNA_WEAK_POINT),
                true);
    }

    public static void revealToOwnerSnapshot(ServerPlayer owner, LivingEntity target, int ticks,
                                             boolean voiceIfPlayer, boolean ultimateRecon, boolean weakPoint) {
        revealToOwner(owner, target, ticks, voiceIfPlayer, ultimateRecon, weakPoint, false);
    }

    private static boolean revealToOwner(ServerPlayer owner, LivingEntity target, int ticks, boolean voiceIfPlayer,
                                      boolean ultimateRecon, boolean weakPoint, boolean requireActiveLuna) {
        if (owner == null || !owner.isAlive() || (requireActiveLuna && !isLuna(owner))
                || target == owner || ticks <= 0) {
            return false;
        }
        // Recon must not expose teammates (and never self).
        if (!com.rzy.dealt_force_skills.util.PositionRevealHelper.isValidReconTarget(owner, target)) {
            return false;
        }
        boolean alreadyRevealed = ReconRevealThrottle.isActive(target);
        int effectiveTicks = ultimateRecon ? Integer.MAX_VALUE : ticks;
        boolean revealStarted = ultimateRecon
                ? ReconRevealThrottle.tryStartOrExtend(target, effectiveTicks)
                : ReconRevealThrottle.tryStart(target, effectiveTicks);
        if (weakPoint && alreadyRevealed) {
            CompoundTag targetData = target.getPersistentData();
            int stacks = targetData.getInt(WEAK_POINT_STACKS);
            targetData.putInt(WEAK_POINT_STACKS, stacks == Integer.MAX_VALUE ? stacks : stacks + 1);
            targetData.putLong(WEAK_POINT_UNTIL, ReconRevealThrottle.activeUntil(target));
        }
        if (!revealStarted) {
            return false;
        }

        // Share position exposure with the caster's teammates.
        com.rzy.dealt_force_skills.util.PositionRevealHelper.sendToCasterAndTeammates(
                owner, new S2C_LunaRevealEntities(List.of(target.getId()), effectiveTicks));
        if (target instanceof ServerPlayer) {
            target.level().playSound(null, target.blockPosition(), ModSounds.LUNA_POSITION_REVEAL.get(),
                    SoundSource.PLAYERS, 0.85f, 1.0f);
            if (voiceIfPlayer) {
                RangedSoundHelper.playFollowingPlayer(owner, ModSounds.LUNA_REVEAL_VOICE.get(),
                        SoundSource.PLAYERS, 0.8f, 1.0f, 32.0D);
            }
        }
        return true;
    }

    public static float applyWeakPointDamage(LivingEntity target, float amount) {
        if (target == null || target.level().isClientSide || amount <= 0.0F) {
            return amount;
        }
        CompoundTag tag = target.getPersistentData();
        long now = target.level().getGameTime();
        if (tag.getLong(WEAK_POINT_UNTIL) <= now) {
            tag.remove(WEAK_POINT_UNTIL);
            tag.remove(WEAK_POINT_STACKS);
            return amount;
        }
        int stacks = tag.getInt(WEAK_POINT_STACKS);
        if (stacks <= 0) {
            return amount;
        }
        double adjusted = amount * (1.0D + stacks * 0.1D);
        return adjusted >= Float.MAX_VALUE ? Float.MAX_VALUE : (float) adjusted;
    }

    public static void syncToClient(ServerPlayer player) {
        if (!isLuna(player)) {
            return;
        }
        initializeIfNeeded(player);
        NetworkHandler.sendToPlayer(new S2C_SyncLunaState(
                shockCharges(player),
                shockCapacity(player),
                shockRechargeRemainingTicks(player),
                grenadeCharges(player),
                grenadeCapacity(player),
                grenadeRechargeRemainingTicks(player),
                coreCooldownRemainingTicks(player),
                equippedTool(player).ordinal(),
                grenadeCookTicks(player),
                shockBounceEnabled(player)
        ), player);
    }

    private static boolean isLunaSkillDamage(DamageSource source) {
        if (source.is(SkillDamageHelper.LUNA_SHOCK_ARROW)) {
            return true;
        }
        Entity direct = source.getDirectEntity();
        return direct instanceof LunaShockArrowEntity || direct instanceof LunaCompositeGrenadeEntity;
    }

    private static boolean consumeCharge(ServerPlayer player, String chargesKey, int maxCharges, String rechargeKey, int rechargeTicks) {
        CompoundTag tag = data(player);
        int capacity = Math.max(0, maxCharges);
        int charges = Math.min(capacity, Math.max(0, tag.getInt(chargesKey)));
        tag.putInt(chargesKey, charges);
        if (charges <= 0) {
            return false;
        }

        tag.putInt(chargesKey, charges - 1);
        if (charges == capacity) {
            tag.putLong(rechargeKey, SkillCooldownHelper.until(player, SkillCooldownHelper.now(player), rechargeTicks));
        }
        return true;
    }

    private static void recharge(ServerPlayer player, long now, String chargesKey, int maxCharges, String rechargeKey, int rechargeTicks) {
        CompoundTag tag = data(player);
        int capacity = Math.max(0, maxCharges);
        int charges = Math.min(capacity, Math.max(0, tag.getInt(chargesKey)));
        tag.putInt(chargesKey, charges);
        long nextRecharge = tag.getLong(rechargeKey);
        if (charges >= capacity) {
            tag.putLong(rechargeKey, 0L);
            return;
        }
        if (nextRecharge <= 0L) {
            tag.putLong(rechargeKey, SkillCooldownHelper.until(player, now, rechargeTicks));
            return;
        }
        if (now < nextRecharge) {
            return;
        }

        do {
            charges++;
            if (charges >= capacity) {
                tag.putInt(chargesKey, capacity);
                tag.putLong(rechargeKey, 0L);
                return;
            }
            nextRecharge += SkillCooldownHelper.ticks(player, rechargeTicks);
        } while (now >= nextRecharge);

        tag.putInt(chargesKey, charges);
        tag.putLong(rechargeKey, nextRecharge);
    }

    private static int remainingTicks(Player player, String key) {
        return SkillCooldownHelper.remainingTicks(player, data(player).getLong(key));
    }

    private static int shockCapacity(Player player) {
        return CommanderStateManager.skillCapacity(player, SHOCK_ARROW_MAX_CHARGES);
    }

    private static int grenadeCapacity(Player player) {
        return CommanderStateManager.skillCapacity(player, COMPOSITE_GRENADE_MAX_CHARGES);
    }

    private static CompoundTag data(Player player) {
        CompoundTag persistent = player.getPersistentData();
        if (!persistent.contains(ROOT_TAG, Tag.TAG_COMPOUND)) {
            persistent.put(ROOT_TAG, new CompoundTag());
        }
        CompoundTag tag = persistent.getCompound(ROOT_TAG);
        if (player instanceof ServerPlayer) {
            SkillCooldownHelper.clampCooldown(player, tag, SHOCK_NEXT_RECHARGE, SHOCK_ARROW_RECHARGE_TICKS);
            SkillCooldownHelper.clampCooldown(player, tag, GRENADE_NEXT_RECHARGE, COMPOSITE_GRENADE_RECHARGE_TICKS);
            SkillCooldownHelper.clampCooldown(player, tag, CORE_COOLDOWN_UNTIL, RECON_ARROW_COOLDOWN_TICKS);
        }
        return tag;
    }
}
