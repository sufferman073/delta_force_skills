package com.rzy.dealt_force_skills.advancement;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.CharacterSelectionManager;
import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.character.SkillSlot;
import com.rzy.dealt_force_skills.character.nikaidou.NikaidouHiroWitchificationStateManager;
import com.rzy.dealt_force_skills.character.saeed.SaeedGuardType;
import com.rzy.dealt_force_skills.character.saeed.SaeedStateManager;
import com.rzy.dealt_force_skills.character.undead.UndeadProfession;
import com.rzy.dealt_force_skills.entity.DWolfSmokeCloudEntity;
import com.rzy.dealt_force_skills.entity.GizmoSmokeCloudEntity;
import com.rzy.dealt_force_skills.entity.SaeedGuardEntity;
import com.rzy.dealt_force_skills.entity.StingerSmokeCloudEntity;
import com.rzy.dealt_force_skills.entity.ToxikTearGasCloudEntity;
import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.team.DealtTeamManager;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class DfsAchievements {
    private static final String DATA_KEY = DealtForceSkillsMod.MODID + ".achievements";
    private static final String FINAL = "final";
    private static final int LEX_COOK_RECIPE_COUNT = 11;
    private static final int LEX_TOTAL_ART_COUNT = 33;
    private static final int UNDEAD_LIMITED_SHOP_ENTRY_COUNT = 29;
    private static final int N_TWO_CONTINUOUS_FREEZE_TICKS = 15 * 20;
    private static final String NOX_FIRST_WOUND_OWNER = DATA_KEY + ".nox_first_wound_owner";
    private static final List<String> NORMAL_ADVANCEMENTS = buildNormalAdvancements();

    private DfsAchievements() {
    }

    public static void tick(ServerPlayer player) {
        if (player == null) {
            return;
        }
        CompoundTag data = data(player);
        long now = gameTime(player);
        trackNoHealWindow(player, data);
        finishDelayedAwards(player, data, now);
        checkNetherTourist(player);
        if (player.tickCount % 20 == 0) {
            tryAwardFinal(player);
        }
    }

    public static void onCharacterSelected(ServerPlayer player, String characterId) {
        if (player == null || characterId == null || characterId.isBlank()) {
            return;
        }
        data(player).putString("selected.current", characterId);
        if (ModCharacters.CHAMBER_ID.equals(characterId)) {
            data(player).putInt("chamber.clean_player_kills", 0);
        }
        tryAwardFinal(player);
    }

    public static void onSkillUsed(ServerPlayer player, String characterId, SkillSlot slot, boolean alternate) {
        if (player == null || characterId == null || slot == null) {
            return;
        }
        CompoundTag data = data(player);
        long now = gameTime(player);
        recordDWolfKillSkillSequence(player, characterId, slot, now);
        recordLunaToolCycle(player, characterId, slot, now);
        recordUndeadMechanismUse(player, characterId, slot, now);
        tryAwardFinal(player);
    }

    public static void onDamage(ServerPlayer attacker, LivingEntity target, DamageSource source, float amount) {
        if (attacker == null || target == null || source == null || amount <= 0.0F) {
            return;
        }
        long now = gameTime(attacker);
        String characterId = selectedCharacterId(attacker);
        CompoundTag data = data(attacker);
        String msg = source.getMsgId();

        recordDamageSourceMilestones(attacker, target, source, msg, amount, now, data, characterId);
        recordEffectBasedDamageMilestones(attacker, target, msg, now, data, characterId);
        recordStrongControlChain(attacker, target, now);
    }

    public static void onKill(ServerPlayer killer, LivingEntity victim, DamageSource source) {
        if (killer == null || victim == null) {
            return;
        }
        long now = gameTime(killer);
        CompoundTag data = data(killer);
        String characterId = selectedCharacterId(killer);
        String msg = source == null ? "" : source.getMsgId();

        recordKillSmokeMilestones(killer, victim, now, data, characterId);
        recordKillDamageSourceMilestones(killer, victim, source, msg, now, data, characterId);
        recordKillEffectMilestones(killer, victim, now, data, characterId);
        recordDWolfKillSkillSequenceStart(killer, characterId, now);
        recordDWolfOverloadKill(killer, characterId, now);
        recordVyronLowHealthKill(killer, victim, characterId, now);
        recordNoxStealthKill(killer, victim, characterId, now);
        recordStingerCombatViolation(killer, characterId);
        recordTempestEmergencyKill(killer, victim, characterId, now);
        recordNikaidouCoreKill(killer, characterId, now);
        recordGhrothNoonKill(killer, victim, characterId, now);
        recordUndeadLowHealthSwitchKill(killer, characterId, now);
        recordToxikAdrenalineAction(killer, "kill");
        recordNikaidouWitchPostRemnantKill(killer, victim, characterId, now);
        recordCorpsMaxBoostKill(killer, victim, characterId);
        recordChamberCleanPlayerKill(killer, victim, characterId);
        recordSelfExplosionOtherKill(killer, victim, source);
        recordNoHealKill(killer, data);
        if (victim instanceof ServerPlayer targetPlayer) {
            recordBaitPropLookKill(killer, targetPlayer);
        }
        tryAwardFinal(killer);
    }

    public static void recordNoxFirstDelayedWound(ServerPlayer owner, LivingEntity target, boolean firstWound) {
        if (owner == null || target == null || !firstWound || target.getHealth() <= 250.0F
                || !isSelected(owner, ModCharacters.NOX_ID)
                || !target.hasEffect(ModEffects.NOX_DELAYED_WOUND)) {
            return;
        }
        target.getPersistentData().putUUID(NOX_FIRST_WOUND_OWNER, owner.getUUID());
    }

    public static void recordNoxDelayedWoundAssistDeath(ServerLevel level, LivingEntity target, DamageSource source) {
        if (level == null || target == null || source == null
                || !target.hasEffect(ModEffects.NOX_DELAYED_WOUND)) {
            return;
        }
        CompoundTag targetData = target.getPersistentData();
        if (!targetData.hasUUID(NOX_FIRST_WOUND_OWNER)) {
            return;
        }
        UUID ownerId = targetData.getUUID(NOX_FIRST_WOUND_OWNER);
        Entity killer = source.getEntity();
        if (killer == null || killer.getUUID().equals(ownerId)) {
            return;
        }
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ownerId);
        if (owner != null && isSelected(owner, ModCharacters.NOX_ID)) {
            award(owner, green(6));
        }
    }

    public static void recordNoxFlashGroup(ServerPlayer owner, List<UUID> targets, int flashTicks) {
        if (owner == null || targets == null || !isSelected(owner, ModCharacters.NOX_ID)
                || targets.stream().distinct().count() < 4) {
            return;
        }
        long expiresAt = gameTime(owner) + Math.max(1, flashTicks);
        CompoundTag data = data(owner);
        targets.stream().distinct().forEach(targetId ->
                data.putLong("nox.flash_four." + targetId, expiresAt));
    }

    public static void onSummonKill(ServerPlayer owner, Entity summon, LivingEntity victim) {
        if (owner == null || summon == null || victim == null) {
            return;
        }
        if (summon instanceof SaeedGuardEntity guard && isSelected(owner, ModCharacters.SAEED_ID)) {
            recordSaeedGuardKill(owner, guard, victim);
        }
        tryAwardFinal(owner);
    }

    public static void onDeath(ServerPlayer player, DamageSource source) {
        if (player == null) {
            return;
        }
        CompoundTag data = data(player);
        long now = gameTime(player);
        data.putLong("last_death", now);
        String characterId = selectedCharacterId(player);
        if (ModCharacters.NIKAIDOU_HIRO_ID.equals(characterId) && data.getInt("nikaidou.core_kills") >= 6) {
            award(player, purple(37));
        }
        if (ModCharacters.LEX_NINJIA_ID.equals(characterId)
                && source != null
                && data.getBoolean("lex.forbidden_art_active")) {
            award(player, green(32));
        }
        recordSelfExplosionDeath(player, source);
        recordNikaidouWitchSkeletonTeammateDeath(player, source);
        clearNoDeathChains(data);
        tryAwardFinal(player);
    }

    public static void onDamageTaken(ServerPlayer player, DamageSource source, float amount) {
        if (player == null || amount <= 0.0F) {
            return;
        }
        if (isSelected(player, ModCharacters.CHAMBER_ID)) {
            data(player).putInt("chamber.clean_player_kills", 0);
        }
    }

    public static void onRescue(ServerPlayer rescuer, ServerPlayer target, boolean self, String source) {
        if (rescuer == null || source == null) {
            return;
        }
        long now = gameTime(rescuer);
        if ("stinger".equals(source)) {
            recordStingerRescue(rescuer, target, self, now);
        } else if ("vlinder".equals(source)) {
            recordVlinderRescue(rescuer, target, self, now);
        }
        recordToxikAdrenalineAction(rescuer, "rescue");
        recordTeamRescue(rescuer, target, self, now);
    }

    public static void onCrafted(ServerPlayer player, ItemStack stack) {
        if (player == null || stack == null || stack.isEmpty()) {
            return;
        }
        tryAwardFinal(player);
    }

    public static void onShopPurchase(ServerPlayer player, String shop, String entryId) {
        if (player == null || shop == null || entryId == null) {
            return;
        }
        CompoundTag data = data(player);
        String normalizedShop = normalizeId(shop);
        String normalizedEntry = normalizeId(entryId);
        int bought = markUnique(data, "shop." + normalizedShop, normalizedEntry);
        if ("undead".equals(normalizedShop)
                && isSelected(player, ModCharacters.UNDEAD_ID)
                && bought >= UNDEAD_LIMITED_SHOP_ENTRY_COUNT) {
            award(player, green(36));
        }
        tryAwardFinal(player);
    }

    public static void onLexArtEquipped(ServerPlayer player, String artId) {
        if (player == null || artId == null || !isSelected(player, ModCharacters.LEX_NINJIA_ID)) {
            return;
        }
        int equipped = markUnique(data(player), "lex.equipped_arts", normalizeId(artId));
        if (equipped >= LEX_TOTAL_ART_COUNT) {
            award(player, green(33));
        }
    }

    public static void onLexArtReleased(ServerPlayer player, String artId, boolean cookRecipe) {
        if (player == null || artId == null || !isSelected(player, ModCharacters.LEX_NINJIA_ID)) {
            return;
        }
        CompoundTag data = data(player);
        String normalized = normalizeId(artId);
        long now = gameTime(player);
        int released = windowUnique(data, "lex.art_release_window", normalized, now, 35 * 20);
        if (released >= 12) {
            award(player, purple(31));
        }
        if (cookRecipe) {
            int cookCount = markUnique(data, "lex.cook_recipes", normalized);
            if (cookCount >= LEX_COOK_RECIPE_COUNT) {
                award(player, green(31));
            }
        }
        if ("all_hands".equals(normalized)) {
            award(player, purple(30));
        }
        if (normalized.startsWith("ham_")) {
            data.putBoolean("lex.forbidden_art_active", true);
        }
    }

    public static void onElectronicInterference(ServerPlayer player, int affectedDevices) {
        if (player == null || affectedDevices <= 0) {
            return;
        }
        String characterId = selectedCharacterId(player);
        if (ModCharacters.HACKCLAW_ID.equals(characterId) && affectedDevices >= 2) {
            award(player, green(25));
        }
        if (ModCharacters.RAPTOR_ID.equals(characterId) && affectedDevices >= 3) {
            award(player, green(30));
        }
        recordElectronicDeathSetup(player, affectedDevices);
    }

    public static void onSaeedPrestige(ServerPlayer player, int prestige) {
        if (player != null && prestige >= 4 && isSelected(player, ModCharacters.SAEED_ID)) {
            award(player, green(46));
        }
    }

    public static void recordDWolfOverloadExtension(ServerPlayer player, int extensionTicks) {
        if (player == null || !isSelected(player, ModCharacters.D_WOLF_ID)) {
            return;
        }
        int total = increment(data(player), "d_wolf.overload_extension_ticks", Math.max(0, extensionTicks));
        if (total >= 125 * 20) {
            award(player, purple(1));
        }
    }

    public static void recordVyronDashWhilePowered(ServerPlayer player) {
        if (player != null && isSelected(player, ModCharacters.VYRON_ID)) {
            award(player, green(3));
        }
    }

    public static void recordVyronStickyBombKill(ServerPlayer player, LivingEntity target) {
        if (player != null && target instanceof ServerPlayer && isSelected(player, ModCharacters.VYRON_ID)) {
            award(player, green(4));
        }
    }

    public static void recordVyronTigerCannonExplosion(ServerPlayer player, int floorBounces, int knockedDownTargets) {
        if (player != null && isSelected(player, ModCharacters.VYRON_ID)
                && floorBounces >= 1 && knockedDownTargets >= 4) {
            award(player, purple(3));
        }
    }

    public static void recordNTwoFreeze(ServerPlayer owner, LivingEntity target, int freezeTicks) {
        if (owner == null || target == null || !isSelected(owner, ModCharacters.N_TWO_ID)) {
            return;
        }
        CompoundTag data = data(owner);
        long now = gameTime(owner);
        String base = "ntwo.freeze_continuous." + target.getUUID();
        long start = data.getLong(base + ".start");
        long until = data.getLong(base + ".until");
        if (start <= 0L || now > until + 5L) {
            start = now;
        }
        long newUntil = Math.max(until, now + Math.max(1, freezeTicks));
        data.putLong(base + ".start", start);
        data.putLong(base + ".until", newUntil);
        if (newUntil - start >= N_TWO_CONTINUOUS_FREEZE_TICKS) {
            award(owner, purple(52));
        }
    }

    public static void recordNTwoCondensedFreeze(ServerPlayer owner, String sourceId, UUID targetId) {
        if (owner == null || sourceId == null || sourceId.isBlank() || targetId == null
                || !isSelected(owner, ModCharacters.N_TWO_ID)) {
            return;
        }
        int frozen = markUnique(data(owner), "ntwo.condensed_freezes." + normalizeId(sourceId), targetId.toString());
        if (frozen >= 5) {
            award(owner, green(50));
        }
    }

    public static void recordNTwoDewarFreeze(ServerPlayer owner, String sourceId) {
        if (owner == null || sourceId == null || sourceId.isBlank() || !isSelected(owner, ModCharacters.N_TWO_ID)) {
            return;
        }
        int freezes = increment(data(owner), "ntwo.dewar_freezes." + normalizeId(sourceId), 1);
        if (freezes >= 12) {
            award(owner, purple(51));
        }
    }

    public static void recordNTwoColdDamageKill(ServerPlayer owner, LivingEntity target) {
        if (owner == null || target == null || target.getUUID().equals(owner.getUUID())
                || !isSelected(owner, ModCharacters.N_TWO_ID)) {
            return;
        }
        if (target instanceof ServerPlayer targetPlayer && DealtTeamManager.areTeammates(owner, targetPlayer)) {
            return;
        }
        award(owner, green(51));
    }

    public static void recordCorpsLoyaltyExecution(ServerPlayer owner, LivingEntity target) {
        if (owner != null && target instanceof ServerPlayer && isSelected(owner, ModCharacters.CORPS_ID)) {
            award(owner, green(53));
        }
    }

    public static void recordCorpsPassiveHealing(ServerPlayer player, float amount) {
        if (player == null || amount <= 0.0F || !isSelected(player, ModCharacters.CORPS_ID)) {
            return;
        }
        float total = data(player).getFloat("corps.passive_healing") + amount;
        data(player).putFloat("corps.passive_healing", total);
        if (total >= 1000.0F) {
            award(player, purple(53));
        }
    }

    public static void recordCorpsPassiveAbsorb(ServerPlayer player, float amount) {
        if (player == null || amount <= 0.0F || !isSelected(player, ModCharacters.CORPS_ID)) {
            return;
        }
        float total = data(player).getFloat("corps.passive_absorb") + amount;
        data(player).putFloat("corps.passive_absorb", total);
        if (total >= 2000.0F) {
            award(player, purple(54));
        }
    }

    public static void recordNikaidouWitchFinalEffect(ServerPlayer player) {
        if (player != null && isSelected(player, ModCharacters.NIKAIDOU_HIRO_WITCHIFICATION_ID)) {
            award(player, green(54));
        }
    }

    public static void recordChamberRoundTax(ServerPlayer player, long amount) {
        if (player == null || amount <= 0L || !isSelected(player, ModCharacters.CHAMBER_ID)) {
            return;
        }
        long total = data(player).getLong("chamber.round_tax") + amount;
        data(player).putLong("chamber.round_tax", total);
        if (total >= 25000L) {
            award(player, green(55));
        }
    }

    public static void recordSinevaShieldBlock(ServerPlayer player, float blockedDamage, boolean viewportHit, boolean viewportBroken) {
        if (player == null || !isSelected(player, ModCharacters.SINEVA_ID) || blockedDamage <= 0.0F) {
            return;
        }
        CompoundTag data = data(player);
        float total = data.getFloat("sineva.shield_block_damage") + blockedDamage;
        data.putFloat("sineva.shield_block_damage", total);
        if (total >= 400.0F) {
            award(player, green(9));
        }
        if (viewportHit) {
            float viewportTotal = data.getFloat("sineva.viewport_damage") + blockedDamage;
            data.putFloat("sineva.viewport_damage", viewportTotal);
            if (viewportBroken && viewportTotal >= 3500.0F) {
                award(player, purple(10));
            }
        }
    }

    public static void recordSinevaGrapplePull(ServerPlayer player, LivingEntity target, double startDistance, double endDistance) {
        if (player != null && target != null && isSelected(player, ModCharacters.SINEVA_ID)
                && startDistance >= 20.0D && endDistance <= 3.5D) {
            award(player, green(10));
        }
    }

    public static void recordSinevaShieldBashTargets(ServerPlayer player, int knockedDownTargets, boolean followUpBash) {
        if (player == null || !isSelected(player, ModCharacters.SINEVA_ID)) {
            return;
        }
        CompoundTag data = data(player);
        long now = gameTime(player);
        if (knockedDownTargets >= 4) {
            data.putLong("sineva.charge_knock4", now);
        }
        if (followUpBash && now - data.getLong("sineva.charge_knock4") <= 3L * 20L) {
            award(player, purple(9));
        }
    }

    public static void recordTempestSpineRefresh(ServerPlayer player) {
        if (player == null || !isSelected(player, ModCharacters.TEMPEST_ID)) {
            return;
        }
        int count = incrementWindow(data(player), "tempest.spine_refresh", gameTime(player), 10 * 20);
        if (count >= 5) {
            award(player, green(7));
        }
    }

    public static void recordTempestRecallSuccess(ServerPlayer player, double pathLength) {
        if (player != null && isSelected(player, ModCharacters.TEMPEST_ID) && pathLength >= 250.0D) {
            award(player, green(8));
        }
    }

    public static void recordTempestEmergencySelfSave(ServerPlayer player, boolean oneHealth, int survivalTicks) {
        if (player == null || !isSelected(player, ModCharacters.TEMPEST_ID)) {
            return;
        }
        CompoundTag data = data(player);
        if (oneHealth) {
            data.putLong("tempest.self_save_survive_until", gameTime(player) + Math.max(0, survivalTicks));
        }
        data.putLong("tempest.emergency_until", gameTime(player) + 8L * 20L);
    }

    public static void recordUluruFireFieldTargets(ServerPlayer player, int burningTargets) {
        if (player != null && isSelected(player, ModCharacters.ULURU_ID) && burningTargets >= 5) {
            award(player, green(11));
        }
    }

    public static void recordUluruMissileCombo(ServerPlayer player, boolean directHit, boolean bombletHit) {
        if (player == null || !isSelected(player, ModCharacters.ULURU_ID)) {
            return;
        }
        CompoundTag data = data(player);
        long now = gameTime(player);
        if (directHit) {
            data.putLong("uluru.missile_direct_until", now + 8L * 20L);
        }
        if (bombletHit) {
            data.putLong("uluru.missile_bomblet_until", now + 8L * 20L);
        }
        if (data.getLong("uluru.missile_direct_until") >= now
                && data.getLong("uluru.missile_bomblet_until") >= now) {
            award(player, green(12));
        }
    }

    public static void recordUluruQuickCoverProtection(ServerPlayer player, float damageTaken, boolean playerSurvived) {
        if (player == null || !isSelected(player, ModCharacters.ULURU_ID) || damageTaken <= 0.0F) {
            return;
        }
        float total = data(player).getFloat("uluru.cover_damage") + damageTaken;
        data(player).putFloat("uluru.cover_damage", total);
        if (playerSurvived && total >= 750.0F) {
            award(player, purple(12));
        }
    }

    public static void recordGizmoTrapKill(ServerPlayer player, LivingEntity victim) {
        if (player != null && victim instanceof ServerPlayer && isSelected(player, ModCharacters.GIZMO_ID)) {
            award(player, green(13));
        }
    }

    public static void recordGizmoTBoyWeb(ServerPlayer player, int webbedTargets) {
        if (player != null && isSelected(player, ModCharacters.GIZMO_ID) && webbedTargets >= 2) {
            award(player, green(14));
        }
    }

    public static void recordGizmoSpiderNestHits(ServerPlayer player, List<UUID> targets) {
        if (player == null || targets == null || !isSelected(player, ModCharacters.GIZMO_ID)) {
            return;
        }
        if (targets.stream().distinct().count() >= 3) {
            award(player, purple(13));
        }
    }

    public static void recordGizmoComboTarget(ServerPlayer player, LivingEntity target, String source) {
        if (player == null || target == null || source == null || !isSelected(player, ModCharacters.GIZMO_ID)) {
            return;
        }
        int unique = markUnique(data(player), "gizmo.combo." + target.getUUID(), source);
        if (unique >= 3) {
            award(player, purple(14));
        }
    }

    public static void recordShepherdSonicTrapHit(ServerPlayer player, LivingEntity target, boolean warned) {
        if (player != null && target != null && isSelected(player, ModCharacters.SHEPHERD_ID) && warned) {
            award(player, green(15));
        }
    }

    public static void recordShepherdDroneShock(ServerPlayer player, int targets) {
        if (player != null && isSelected(player, ModCharacters.SHEPHERD_ID) && targets >= 6) {
            award(player, green(16));
        }
    }

    public static void recordShepherdFragArmorBreak(ServerPlayer player, LivingEntity target, boolean bounced,
                                                    int armorPiecesBroken, boolean sonicShocked, boolean killed) {
        if (player == null || target == null || !isSelected(player, ModCharacters.SHEPHERD_ID)) {
            return;
        }
        if (bounced && armorPiecesBroken >= 2) {
            award(player, purple(15));
        }
        if (bounced && armorPiecesBroken >= 1 && sonicShocked && !killed) {
            data(player).putLong("shepherd.frag_survive_until", gameTime(player) + 30L * 20L);
        }
    }

    public static void recordStingerSmokeConvertedSave(ServerPlayer player, ServerPlayer target) {
        if (player != null && target != null && isSelected(player, ModCharacters.STINGER_ID)) {
            award(player, green(18));
        }
    }

    public static void recordToxikFireflyHit(ServerPlayer player, LivingEntity target, boolean lethalMode) {
        if (player == null || target == null || !isSelected(player, ModCharacters.TOXIK_ID)) {
            return;
        }
        if (!lethalMode) {
            return;
        }
        CompoundTag data = data(player);
        long now = gameTime(player);
        long start = data.getLong("toxik.firefly_lethal_targets.start");
        if (start <= 0L || now - start > 12L * 20L) {
            data.put("toxik.firefly_lethal_players", new CompoundTag());
        }
        int affectedTargets = windowUnique(data, "toxik.firefly_lethal_targets", target.getStringUUID(), now, 12 * 20);
        int affectedPlayers = data.contains("toxik.firefly_lethal_players", Tag.TAG_COMPOUND)
                ? data.getCompound("toxik.firefly_lethal_players").getAllKeys().size()
                : 0;
        if (target instanceof ServerPlayer) {
            affectedPlayers = windowUnique(data, "toxik.firefly_lethal_players", target.getStringUUID(), now, 12 * 20);
            data.putLong("toxik.firefly_unremoved." + target.getUUID(), now + 60L * 20L);
        }
        if (affectedTargets >= 5 && affectedPlayers >= 1) {
            data.putLong("toxik.firefly_lethal_swarm_until", now + 60L * 20L);
        }
    }

    public static void recordToxikFireflyMaxHealthLoss(ServerPlayer player, LivingEntity target, double lossRatio) {
        if (player != null && target != null && isSelected(player, ModCharacters.TOXIK_ID) && lossRatio >= 0.5D) {
            award(player, green(19));
        }
    }

    public static void recordToxikFireflyUnremovedDeath(ServerPlayer player, ServerPlayer target) {
        if (player == null || target == null || !isSelected(player, ModCharacters.TOXIK_ID)) {
            return;
        }
        CompoundTag data = data(player);
        long now = gameTime(player);
        if (data.getLong("toxik.firefly_lethal_swarm_until") >= now
                && data.getLong("toxik.firefly_unremoved." + target.getUUID()) >= now) {
            award(player, purple(19));
        }
    }

    public static void recordToxikFireflyTargets(ServerPlayer player, int affectedTargets, boolean includedPlayer,
                                                 boolean lethalMode, boolean unremovedPlayerDied) {
        if (player == null || !isSelected(player, ModCharacters.TOXIK_ID)) {
            return;
        }
        if (lethalMode && affectedTargets >= 5 && includedPlayer && unremovedPlayerDied) {
            award(player, purple(19));
        }
    }

    public static void recordToxikAdrenalineAction(ServerPlayer player, String action) {
        if (player == null || action == null || !isSelected(player, ModCharacters.TOXIK_ID)
                || !player.hasEffect(ModEffects.TOXIK_ADRENALINE)) {
            return;
        }
        int actions = windowUnique(data(player), "toxik.adrenaline_actions", normalizeId(action), gameTime(player), 20 * 20);
        if (actions >= 3) {
            award(player, green(20));
        }
    }

    public static void recordVlinderVitalProtection(ServerPlayer player, ServerPlayer protectedTarget) {
        if (player != null && protectedTarget != null && isSelected(player, ModCharacters.VLINDER_ID)) {
            award(player, green(21));
        }
    }

    public static void recordVlinderHealing(ServerPlayer player, float amount) {
        if (player == null || !isSelected(player, ModCharacters.VLINDER_ID) || amount <= 0.0F) {
            return;
        }
        float total = data(player).getFloat("vlinder.healing_done") + amount;
        data(player).putFloat("vlinder.healing_done", total);
        if (total >= 150.0F) {
            award(player, green(22));
        }
    }

    public static void recordVlinderActiveDefenseRescue(ServerPlayer player, ServerPlayer target,
                                                        boolean plasmaInjected, boolean fastRescue) {
        if (player != null && target != null && isSelected(player, ModCharacters.VLINDER_ID)
                && plasmaInjected && fastRescue) {
            award(player, purple(20));
        }
    }

    public static void recordVlinderActiveDefenseFatalSave(ServerPlayer player, boolean selfSaved) {
        if (player != null && isSelected(player, ModCharacters.VLINDER_ID) && selfSaved) {
            award(player, purple(21));
        }
    }

    public static void recordLunaReconReveal(ServerPlayer player, int revealedTargets) {
        if (player != null && isSelected(player, ModCharacters.LUNA_ID) && revealedTargets >= 6) {
            award(player, green(23));
        }
    }

    public static void recordLunaShockFixedKill(ServerPlayer player, LivingEntity target, boolean bounced, boolean fixed) {
        if (player != null && target instanceof ServerPlayer && isSelected(player, ModCharacters.LUNA_ID)
                && bounced && fixed) {
            data(player).putLong("luna.shock_fixed." + target.getUUID(), gameTime(player) + 10L * 20L);
        }
    }

    public static void recordLunaCompositeArmorBreak(ServerPlayer player, LivingEntity target, int armorPiecesBroken) {
        if (player == null || target == null || !isSelected(player, ModCharacters.LUNA_ID) || armorPiecesBroken <= 0) {
            return;
        }
        data(player).putLong("luna.armor_break." + target.getUUID(), gameTime(player));
    }

    public static void recordHackclawDestroyedFreshElectronic(ServerPlayer player) {
        if (player != null && isSelected(player, ModCharacters.HACKCLAW_ID)) {
            award(player, green(47));
        }
    }

    public static void recordHackclawFlashDroneTargets(ServerPlayer player, int flashedTargets) {
        if (player == null || !isSelected(player, ModCharacters.HACKCLAW_ID)) {
            return;
        }
        if (flashedTargets >= 2) {
            data(player).putLong("hackclaw.double_flash_until", gameTime(player) + 8L * 20L);
        }
        if (flashedTargets >= 5) {
            award(player, purple(25));
        }
    }

    public static void recordHackclawAdvancedPathFlash(ServerPlayer player, ServerPlayer target) {
        if (player != null && target != null && isSelected(player, ModCharacters.HACKCLAW_ID)) {
            award(player, purple(24));
        }
    }

    public static void recordMorseAlertDamage(ServerPlayer player, LivingEntity target) {
        if (player != null && target != null && isSelected(player, ModCharacters.MORSE_ID)) {
            award(player, green(27));
        }
    }

    public static void recordMorseFlashTargets(ServerPlayer player, int targets) {
        if (player != null && isSelected(player, ModCharacters.MORSE_ID) && targets >= 4) {
            award(player, green(28));
        }
    }

    public static void recordMorseSonarActionReveal(ServerPlayer player, ServerPlayer target) {
        if (player == null || target == null || !isSelected(player, ModCharacters.MORSE_ID)) {
            return;
        }
        int revealed = windowUnique(data(player), "morse.sonar_action_reveals", target.getStringUUID(), gameTime(player), 30 * 20);
        if (revealed >= 3) {
            award(player, purple(26));
        }
    }

    public static void recordMorseShockDeafTargets(ServerPlayer player, int targets) {
        if (player != null && isSelected(player, ModCharacters.MORSE_ID) && targets >= 6) {
            award(player, purple(27));
        }
    }

    public static void recordRaptorFalconObservation(ServerPlayer player, ServerPlayer target) {
        if (player != null && target != null && isSelected(player, ModCharacters.RAPTOR_ID)) {
            award(player, green(29));
        }
    }

    public static void recordRaptorHummingbirdKill(ServerPlayer player, ServerPlayer target) {
        if (player != null && target != null && isSelected(player, ModCharacters.RAPTOR_ID)) {
            award(player, purple(28));
        }
    }

    public static void recordRaptorPulseThenFalconKill(ServerPlayer player, ServerPlayer target) {
        if (player != null && target != null && isSelected(player, ModCharacters.RAPTOR_ID)) {
            award(player, purple(29));
        }
    }

    public static void recordUndeadRogueTheft(ServerPlayer player, ServerPlayer target) {
        if (player != null && target != null && isSelected(player, ModCharacters.UNDEAD_ID)) {
            data(player).putLong("undead.rogue_theft_until", gameTime(player) + 30L * 20L);
        }
    }

    public static void recordUndeadHunterScatterHit(ServerPlayer player, LivingEntity target) {
        if (player == null || target == null || !isSelected(player, ModCharacters.UNDEAD_ID)) {
            return;
        }
        int hits = incrementWindow(data(player), "undead.hunter_scatter_hits", gameTime(player), 20 * 20);
        if (hits >= 25) {
            award(player, green(35));
        }
    }

    public static void recordUndeadProfessionMechanism(ServerPlayer player, UndeadProfession profession) {
        if (player == null || profession == null || !isSelected(player, ModCharacters.UNDEAD_ID)) {
            return;
        }
        int mechanisms = windowUnique(data(player), "undead.profession_mechanisms", profession.name(), gameTime(player), 35 * 20);
        if (mechanisms >= 6) {
            award(player, purple(32));
        }
    }

    public static void recordUndeadLowHealthSwitch(ServerPlayer player, UndeadProfession profession) {
        if (player == null || profession == null || !isSelected(player, ModCharacters.UNDEAD_ID)
                || player.getHealth() > player.getMaxHealth() * 0.20F) {
            return;
        }
        int switches = windowUnique(data(player), "undead.low_health_switches", profession.name(), gameTime(player), 8 * 20);
        if (switches >= 3) {
            data(player).putLong("undead.low_health_switch_kill_until", gameTime(player) + 8L * 20L);
        }
    }

    public static void recordDepartmentCalibration(ServerPlayer player, LivingEntity target, int stacks) {
        if (player != null && target instanceof ServerPlayer && isSelected(player, ModCharacters.DEPARTMENT_OF_TRANSPORTATION_ID)
                && stacks >= 5) {
            award(player, green(37));
        }
    }

    public static void recordDepartmentCoreKill(ServerPlayer player, int killedPlayers) {
        if (player != null && isSelected(player, ModCharacters.DEPARTMENT_OF_TRANSPORTATION_ID) && killedPlayers >= 2) {
            award(player, purple(34));
        }
    }

    public static void recordCatDadTripleHissKill(ServerPlayer player, LivingEntity target, boolean fullThreeHits) {
        if (player != null && target != null && isSelected(player, ModCharacters.CATDAD_ID) && fullThreeHits) {
            award(player, green(38));
        }
    }

    public static void recordCatDadHissStageHit(ServerPlayer player, LivingEntity target, int stage) {
        if (player == null || target == null || !isSelected(player, ModCharacters.CATDAD_ID)) {
            return;
        }
        CompoundTag data = data(player);
        long now = gameTime(player);
        String id = target.getStringUUID();
        String maskKey = "catdad.hiss_mask." + id;
        String untilKey = "catdad.hiss_mask_until." + id;
        int mask = data.getLong(untilKey) >= now ? data.getInt(maskKey) : 0;
        if (stage == 1) {
            mask = 1;
        } else if (stage == 2 && (mask & 1) != 0) {
            mask |= 2;
        } else if (stage == 3 && (mask & 3) == 3) {
            mask |= 4;
        } else {
            return;
        }
        data.putInt(maskKey, mask);
        data.putLong(untilKey, now + 12L * 20L);
        if ((mask & 7) == 7) {
            data.putLong("catdad.full_three_hiss_until." + id, now + 15L * 20L);
        }
    }

    public static void recordCatDadTruckHits(ServerPlayer player, int hitsOrKills) {
        if (player != null && isSelected(player, ModCharacters.CATDAD_ID) && hitsOrKills >= 9) {
            award(player, purple(35));
        }
    }

    public static void recordManbaDuelDeath(ServerPlayer player, ServerPlayer killer, long duelTicks) {
        if (player != null && killer != null && isSelected(player, ModCharacters.MANBA_ID) && duelTicks >= 2L * 60L * 20L) {
            award(player, green(39));
        }
    }

    public static void recordManbaFavorSecondDeathSave(ServerPlayer player, int favor) {
        if (player != null && isSelected(player, ModCharacters.MANBA_ID) && favor > 300) {
            award(player, purple(36));
        }
    }

    public static void recordBaitPropLookKill(ServerPlayer player, ServerPlayer target) {
        if (player != null && target != null
                && data(player).getLong("global.bait_look." + target.getUUID()) >= gameTime(player)) {
            award(player, purple(44));
        }
    }

    public static void recordBaitPropLook(ServerPlayer player, ServerPlayer target) {
        if (player != null && target != null && player != target) {
            data(player).putLong("global.bait_look." + target.getUUID(), gameTime(player) + 20L * 20L);
        }
    }

    public static void recordGhrothPassiveReveal(ServerPlayer player, int targets) {
        if (player != null && isSelected(player, ModCharacters.GHROTH_ID) && targets >= 20) {
            award(player, green(41));
        }
    }

    public static void recordGhrothPassiveRevealTarget(ServerPlayer player, LivingEntity target, boolean invisible) {
        if (player == null || target == null || !invisible || !isSelected(player, ModCharacters.GHROTH_ID)) {
            return;
        }
        data(player).putLong("ghroth.invisible_reveal." + target.getUUID(), gameTime(player) + 30L * 20L);
    }

    public static void recordGhrothInvisibleRevealKill(ServerPlayer player, LivingEntity target) {
        if (player != null && target != null && isSelected(player, ModCharacters.GHROTH_ID)) {
            award(player, green(42));
        }
    }

    public static void recordGhrothNoonTaczDamage(ServerPlayer player, LivingEntity target) {
        if (player != null && target != null && isSelected(player, ModCharacters.GHROTH_ID)) {
            data(player).putLong("ghroth.noon_tacz_damage." + target.getUUID(), gameTime(player) + 6L * 20L);
        }
    }

    public static void recordGhrothStarsHit(ServerPlayer player) {
        if (player == null || !isSelected(player, ModCharacters.GHROTH_ID)) {
            return;
        }
        int hits = incrementWindow(data(player), "ghroth.stars_hits", gameTime(player), 13 * 20);
        if (hits >= 13) {
            award(player, purple(38));
        }
    }

    public static void recordSaeedIronRainHit(ServerPlayer player) {
        if (player == null || !isSelected(player, ModCharacters.SAEED_ID)) {
            return;
        }
        int hits = incrementWindow(data(player), "saeed.iron_rain_hits", gameTime(player), 12 * 20);
        if (hits >= 30) {
            award(player, green(43));
        }
    }

    public static void recordSaeedFireTargets(ServerPlayer player, int burningTargets) {
        if (player != null && isSelected(player, ModCharacters.SAEED_ID) && burningTargets >= 8) {
            award(player, green(44));
        }
    }

    public static void recordSaeedHakimMissileKill(ServerPlayer player, boolean aroundObstacle) {
        if (player != null && isSelected(player, ModCharacters.SAEED_ID) && aroundObstacle) {
            award(player, green(45));
        }
    }

    public static void recordSaeedEagleFarKill(ServerPlayer player, double distance) {
        if (player != null && isSelected(player, ModCharacters.SAEED_ID) && distance >= 64.0D) {
            award(player, purple(41));
        }
    }

    public static void recordFatalAvoidance(ServerPlayer player) {
        if (player != null) {
            data(player).putLong("global.fatal_avoidance_until", gameTime(player) + 6L * 20L);
        }
    }

    public static void recordSelfExplosionMutualKill(ServerPlayer player) {
        if (player != null) {
            award(player, green(49));
            award(player, purple(49));
        }
    }

    private static void recordSelfExplosionDeath(ServerPlayer player, DamageSource source) {
        if (player == null || source == null || sourcePlayer(source) != player || !isExplosiveSkillSource(source)) {
            return;
        }
        CompoundTag data = data(player);
        long now = gameTime(player);
        String key = selfExplosionKey(source);
        data.putLong("self_explosion.death." + key, now + 4L * 20L);
        int kills = data.getInt("self_explosion.kills." + key);
        if (kills >= 1) {
            award(player, green(49));
        }
        if (kills >= 2) {
            award(player, purple(49));
        }
    }

    private static void recordSelfExplosionOtherKill(ServerPlayer player, LivingEntity victim, DamageSource source) {
        if (player == null || victim == null || victim == player || source == null || sourcePlayer(source) != player
                || !isExplosiveSkillSource(source)) {
            return;
        }
        CompoundTag data = data(player);
        long now = gameTime(player);
        String key = selfExplosionKey(source);
        int kills = windowUnique(data, "self_explosion.unique_kills." + key, victim.getStringUUID(), now, 4 * 20);
        data.putInt("self_explosion.kills." + key, kills);
        if (data.getLong("self_explosion.death." + key) >= now) {
            award(player, green(49));
            if (kills >= 2) {
                award(player, purple(49));
            }
        }
    }

    private static ServerPlayer sourcePlayer(DamageSource source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            return player;
        }
        if (source.getDirectEntity() instanceof ServerPlayer player) {
            return player;
        }
        if (source.getDirectEntity() instanceof Projectile projectile && projectile.getOwner() instanceof ServerPlayer player) {
            return player;
        }
        return null;
    }

    private static boolean isExplosiveSkillSource(DamageSource source) {
        String msg = source.getMsgId();
        return isMsg(msg, "d_wolf_hand_cannon")
                || isMsg(msg, "vyron_magnetic_bomb")
                || isMsg(msg, "uluru_missile")
                || isMsg(msg, "uluru_incendiary")
                || isMsg(msg, "gizmo_spiderling")
                || isMsg(msg, "shepherd_frag_grenade")
                || isMsg(msg, "raptor_falcon")
                || isMsg(msg, "department_trap")
                || isMsg(msg, "department_trap_manual")
                || isMsg(msg, "department_core")
                || isMsg(msg, "department_passive_blast");
    }

    private static String selfExplosionKey(DamageSource source) {
        Entity sourceEntity = source.getDirectEntity() != null ? source.getDirectEntity() : source.getEntity();
        String msg = normalizeId(source.getMsgId()).replace(':', '_').replace('.', '_');
        return sourceEntity == null ? msg : msg + "." + sourceEntity.getStringUUID();
    }

    private static void recordDamageSourceMilestones(ServerPlayer attacker, LivingEntity target, DamageSource source,
                                                     String msg, float amount, long now, CompoundTag data, String characterId) {
        if (isMsg(msg, "d_wolf_hand_cannon") && ModCharacters.D_WOLF_ID.equals(characterId)) {
            int targets = windowUnique(data, "d_wolf.hand_cannon_targets", target.getStringUUID(), now, 8 * 20);
            if (targets >= 3) {
                award(attacker, purple(2));
            }
        }
        if ((isMsg(msg, "uluru_incendiary") || isMsg(msg, "uluru_fire_field")) && ModCharacters.ULURU_ID.equals(characterId)) {
            int targets = windowUnique(data, "uluru.fire_targets", target.getStringUUID(), now, 4 * 20);
            if (targets >= 5) {
                award(attacker, green(11));
            }
        }
        if (isMsg(msg, "gizmo_spiderling") && ModCharacters.GIZMO_ID.equals(characterId)) {
            int spiderTargets = windowUnique(data, "gizmo.spider_targets", target.getStringUUID(), now, 20 * 20);
            if (spiderTargets >= 3) {
                award(attacker, purple(13));
            }
            recordGizmoComboTarget(attacker, target, "spider");
        }
        if (isMsg(msg, "shepherd_sonic") && ModCharacters.SHEPHERD_ID.equals(characterId)) {
            award(attacker, green(15));
        }
        if (isMsg(msg, "toxik_firefly") && ModCharacters.TOXIK_ID.equals(characterId)) {
            recordToxikFireflyTargets(attacker, 1, target instanceof ServerPlayer, false, false);
        }
        if (isMsg(msg, "raptor_pulse") && ModCharacters.RAPTOR_ID.equals(characterId)) {
            data.putLong("raptor.pulsed." + target.getUUID(), now + 12L * 20L);
        }
        if (isMsg(msg, "department_laser") && ModCharacters.DEPARTMENT_OF_TRANSPORTATION_ID.equals(characterId)) {
            recordDepartmentLaserCalibration(attacker, target);
        }
    }

    private static void recordEffectBasedDamageMilestones(ServerPlayer attacker, LivingEntity target, String msg,
                                                          long now, CompoundTag data, String characterId) {
        if (has(target, ModEffects.MORSE_SONAR_REVEALED) && ModCharacters.MORSE_ID.equals(characterId)) {
            recordMorseAlertDamage(attacker, target);
        }
        if (has(target, ModEffects.HACKCLAW_FLASH_BLIND) && ModCharacters.HACKCLAW_ID.equals(characterId)) {
            data.putLong("hackclaw.flash_damaged." + target.getUUID(), now + 8L * 20L);
        }
        if (has(target, ModEffects.RAPTOR_HUMMINGBIRD_MARKED) && ModCharacters.RAPTOR_ID.equals(characterId)) {
            data.putLong("raptor.hummingbird_marked." + target.getUUID(), now + 10L * 20L);
        }
        if (has(target, ModEffects.NIKAIDOU_CORRECTION) && ModCharacters.NIKAIDOU_HIRO_ID.equals(characterId)) {
            data.putLong("nikaidou.corrected." + target.getUUID(), now + 10L * 20L);
        }
        if (has(target, ModEffects.VLINDER_PLASMA_INJECTED) && ModCharacters.VLINDER_ID.equals(characterId)) {
            data.putLong("vlinder.plasma." + target.getUUID(), now + 10L * 20L);
        }
        if (has(target, ModEffects.SONIC_SHOCK) && isMsg(msg, "shepherd_frag_grenade")
                && ModCharacters.SHEPHERD_ID.equals(characterId)) {
            data.putLong("shepherd.frag_sonic." + target.getUUID(), now + 30L * 20L);
        }
        if (has(target, ModEffects.NOX_DELAYED_WOUND) && ModCharacters.NOX_ID.equals(characterId)
                && target.getMaxHealth() > 250.0F) {
            data.putLong("nox.large_delayed_target." + target.getUUID(), now + 20L * 20L);
        }
    }

    private static void recordKillDamageSourceMilestones(ServerPlayer killer, LivingEntity victim, DamageSource source,
                                                         String msg, long now, CompoundTag data, String characterId) {
        if (isMsg(msg, "vyron_magnetic_bomb") && ModCharacters.VYRON_ID.equals(characterId) && victim instanceof ServerPlayer) {
            award(killer, green(4));
        }
        if (isMsg(msg, "uluru_missile") && ModCharacters.ULURU_ID.equals(characterId)) {
            if (data.getLong("uluru.high_kills_start") <= 0L) {
                data.putLong("uluru.high_kills_start", now);
            }
            if (killer.getY() - victim.getY() >= 16.0D) {
                int kills = increment(data, "uluru.high_kills", 1);
                if (kills >= 12) {
                    award(killer, purple(11));
                }
            }
        }
        if (isMsg(msg, "gizmo_spiderling") && ModCharacters.GIZMO_ID.equals(characterId)) {
            recordGizmoTrapKill(killer, victim);
        }
        if (isMsg(msg, "luna_shock_arrow") && ModCharacters.LUNA_ID.equals(characterId)
                && data.getLong("luna.shock_fixed." + victim.getUUID()) >= now) {
            award(killer, purple(22));
        }
        if (isMsg(msg, "raptor_falcon") && ModCharacters.RAPTOR_ID.equals(characterId)) {
            if (data.getLong("raptor.pulsed." + victim.getUUID()) >= now) {
                recordRaptorPulseThenFalconKill(killer, victim instanceof ServerPlayer player ? player : null);
            }
        }
        if (isMsg(msg, "department_core") && ModCharacters.DEPARTMENT_OF_TRANSPORTATION_ID.equals(characterId)) {
            return;
        }
        if (isMsg(msg, "catdad_reflect")) {
            award(killer, purple(48));
        }
        if (isMsg(msg, "catdad_truck") && ModCharacters.CATDAD_ID.equals(characterId)) {
            return;
        }
        if (isMsg(msg, "manba_duel_execute") && ModCharacters.MANBA_ID.equals(characterId)) {
            return;
        }
        if (isMsg(msg, "nikaidou_weapon") || isMsg(msg, "nikaidou_decay")) {
            if (ModCharacters.NIKAIDOU_HIRO_ID.equals(characterId) && has(killer, ModEffects.NIKAIDOU_CORE)) {
                recordNikaidouCoreKill(killer, characterId, now);
            }
        }
    }

    private static void recordKillEffectMilestones(ServerPlayer killer, LivingEntity victim, long now,
                                                   CompoundTag data, String characterId) {
        if (has(victim, ModEffects.HACKCLAW_FLASH_BLIND) && ModCharacters.HACKCLAW_ID.equals(characterId)
                && data.getLong("hackclaw.double_flash_until") >= now) {
            award(killer, green(26));
        }
        if (has(victim, ModEffects.NOX_FLASHED) && ModCharacters.NOX_ID.equals(characterId)
                && data.getLong("nox.flash_four." + victim.getUUID()) >= now) {
            award(killer, purple(6));
        }
        if (has(victim, ModEffects.HACKCLAW_FLASH_BLIND)
                || has(victim, ModEffects.MORSE_FLASH_BLIND)
                || has(victim, ModEffects.NOX_FLASHED)
                || has(victim, ModEffects.MANBA_BLINDED)
                || has(victim, ModEffects.TOXIK_TEAR_GAS_BLIND)) {
            markDeathDebuff(killer, victim, "blind", now);
        }
        if (has(victim, ModEffects.SONIC_SHOCK) || has(victim, ModEffects.MORSE_STRONG_SHOCK)) {
            markDeathDebuff(killer, victim, "deaf", now);
        }
        if (has(victim, ModEffects.HACKCLAW_INTERFERENCE)
                || has(victim, ModEffects.RAPTOR_ELECTROMAGNETIC_INTERFERENCE)
                || has(victim, ModEffects.TOXIK_FIREFLY_INTERFERENCE)
                || has(victim, ModEffects.VLINDER_MEDICAL_WASTE_INTERFERENCE)) {
            markDeathDebuff(killer, victim, "interference", now);
        }
        int deathDebuffs = countRecentDeathDebuffs(data, victim.getUUID(), now);
        if (deathDebuffs >= 3) {
            award(killer, purple(50));
        }
        if (has(victim, ModEffects.RAPTOR_HUMMINGBIRD_MARKED) && ModCharacters.RAPTOR_ID.equals(characterId)) {
            recordRaptorHummingbirdKill(killer, victim instanceof ServerPlayer player ? player : null);
        }
        if (data.getLong("luna.armor_break." + victim.getUUID()) + 5L * 20L >= now
                && ModCharacters.LUNA_ID.equals(characterId)) {
            award(killer, purple(23));
        }
        if (data.getLong("global.fatal_avoidance_until") >= now) {
            award(killer, purple(43));
        }
        if (ModCharacters.CATDAD_ID.equals(characterId)) {
            recordCatDadTripleHissKill(killer, victim,
                    data.getLong("catdad.full_three_hiss_until." + victim.getUUID()) >= now);
        }
        if (ModCharacters.GHROTH_ID.equals(characterId)
                && data.getLong("ghroth.invisible_reveal." + victim.getUUID()) >= now) {
            recordGhrothInvisibleRevealKill(killer, victim);
        }
    }

    private static void recordKillSmokeMilestones(ServerPlayer killer, LivingEntity victim, long now,
                                                  CompoundTag data, String characterId) {
        if (ModCharacters.D_WOLF_ID.equals(characterId) && isDwolfSmokeObscured(killer, victim)) {
            int smokeKills = increment(data, "d_wolf.smoke_kills", 1);
            if (smokeKills >= 5) {
                award(killer, green(1));
            }
        }
        if (isAnySmokeObscured(killer, victim)) {
            int kills = incrementWindow(data, "global.smoke_hidden_kills", now, 20 * 20);
            if (kills >= 3) {
                award(killer, purple(42));
            }
        }
    }

    private static void recordDWolfKillSkillSequenceStart(ServerPlayer killer, String characterId, long now) {
        if (!ModCharacters.D_WOLF_ID.equals(characterId)) {
            return;
        }
        CompoundTag data = data(killer);
        data.putInt("d_wolf.sequence_step", Math.max(1, data.getInt("d_wolf.sequence_step")));
        data.putLong("d_wolf.sequence_until", now + 90L * 20L);
    }

    private static void recordDWolfKillSkillSequence(ServerPlayer player, String characterId, SkillSlot slot, long now) {
        if (!ModCharacters.D_WOLF_ID.equals(characterId)) {
            return;
        }
        CompoundTag data = data(player);
        int step = data.getInt("d_wolf.sequence_step");
        long until = data.getLong("d_wolf.sequence_until");
        if (until <= 0L || now > until) {
            return;
        }
        if (step == 1 && slot == SkillSlot.CORE) {
            data.putInt("d_wolf.sequence_step", 2);
        } else if (step == 2 && slot == SkillSlot.ACTIVE_2) {
            data.putInt("d_wolf.sequence_step", 3);
        } else if (step == 3 && slot == SkillSlot.ACTIVE_1) {
            data.putInt("d_wolf.sequence_step", 4);
            data.putLong("d_wolf.sequence_award_at", now + 90L * 20L);
        }
    }

    private static void recordDWolfOverloadKill(ServerPlayer killer, String characterId, long now) {
        if (ModCharacters.D_WOLF_ID.equals(characterId)) {
            data(killer).putLong("d_wolf.last_overload_kill", now);
        }
    }

    private static void recordVyronLowHealthKill(ServerPlayer killer, LivingEntity victim, String characterId, long now) {
        if (ModCharacters.VYRON_ID.equals(characterId) && killer.getHealth() <= killer.getMaxHealth() * 0.25F && victim instanceof ServerPlayer) {
            int kills = incrementWindow(data(killer), "vyron.low_health_player_kills", now, 30 * 20);
            if (kills >= 3) {
                award(killer, purple(4));
            }
        }
    }

    private static void recordNoxStealthKill(ServerPlayer killer, LivingEntity victim, String characterId, long now) {
        if (!ModCharacters.NOX_ID.equals(characterId) || !has(killer, ModEffects.NOX_STEALTH)) {
            return;
        }
        if (isBehind(killer, victim)) {
            int backKills = incrementWindow(data(killer), "nox.stealth_back_kills", now, 35 * 20);
            if (backKills >= 3) {
                award(killer, green(5));
            }
        }
        if (victim instanceof ServerPlayer) {
            int playerKills = incrementWindow(data(killer), "nox.stealth_player_kills", now, 35 * 20);
            if (playerKills >= 2) {
                award(killer, purple(5));
            }
        }
    }

    private static void recordStingerCombatViolation(ServerPlayer killer, String characterId) {
        if (ModCharacters.STINGER_ID.equals(characterId)) {
            data(killer).putBoolean("stinger.rescue_chain_damaged", true);
        }
    }

    private static void recordTempestEmergencyKill(ServerPlayer killer, LivingEntity victim, String characterId, long now) {
        if (ModCharacters.TEMPEST_ID.equals(characterId)
                && data(killer).getLong("tempest.emergency_until") >= now
                && victim instanceof ServerPlayer) {
            int kills = incrementWindow(data(killer), "tempest.emergency_cooldown_kills", now, 8 * 20);
            if (kills >= 3) {
                award(killer, purple(8));
            }
        }
    }

    private static void recordCorpsMaxBoostKill(ServerPlayer killer, LivingEntity victim, String characterId) {
        if (!ModCharacters.CORPS_ID.equals(characterId) || victim == null) {
            return;
        }
        MobEffectInstance boost = killer.getEffect(ModEffects.CORPS_LOYALTY_BOOST);
        if (boost == null || boost.getAmplifier() < 19) {
            return;
        }
        int kills = increment(data(killer), "corps.max_boost_kills", 1);
        if (kills >= 3) {
            award(killer, green(52));
        }
    }

    private static void recordChamberCleanPlayerKill(ServerPlayer killer, LivingEntity victim, String characterId) {
        if (!ModCharacters.CHAMBER_ID.equals(characterId) || !(victim instanceof ServerPlayer)) {
            return;
        }
        int kills = increment(data(killer), "chamber.clean_player_kills", 1);
        if (kills >= 5) {
            award(killer, purple(56));
        }
    }

    private static void recordNikaidouCoreKill(ServerPlayer killer, String characterId, long now) {
        if (!ModCharacters.NIKAIDOU_HIRO_ID.equals(characterId)) {
            return;
        }
        award(killer, green(40));
        int kills = incrementWindow(data(killer), "nikaidou.core_kills", now, 20 * 20);
        if (kills >= 6) {
            data(killer).putInt("nikaidou.core_kills", kills);
        }
    }

    private static void recordNikaidouWitchPostRemnantKill(ServerPlayer killer, LivingEntity victim,
                                                           String characterId, long now) {
        if (!ModCharacters.NIKAIDOU_HIRO_WITCHIFICATION_ID.equals(characterId)
                || victim == null
                || !NikaidouHiroWitchificationStateManager.isPostRemnantPhase(killer)) {
            return;
        }
        data(killer).putLong("nikaidou_witch.post_remnant_kill_until", now + 5L * 60L * 20L);
    }

    private static void recordNikaidouWitchSkeletonTeammateDeath(ServerPlayer victim, DamageSource source) {
        if (victim == null || source == null || !(source.getEntity() instanceof WitherSkeleton skeleton)) {
            return;
        }
        UUID ownerId = NikaidouHiroWitchificationStateManager.witchSkeletonOwner(skeleton);
        if (ownerId == null || ownerId.equals(victim.getUUID())) {
            return;
        }
        ServerPlayer owner = victim.server.getPlayerList().getPlayer(ownerId);
        if (owner == null || !isSelected(owner, ModCharacters.NIKAIDOU_HIRO_WITCHIFICATION_ID)
                || !DealtTeamManager.areTeammates(owner, victim)) {
            return;
        }
        if (data(owner).getLong("nikaidou_witch.post_remnant_kill_until") >= gameTime(owner)) {
            award(owner, purple(55));
        }
    }

    private static void recordGhrothNoonKill(ServerPlayer killer, LivingEntity victim, String characterId, long now) {
        if (ModCharacters.GHROTH_ID.equals(characterId)
                && victim != null
                && data(killer).getLong("ghroth.noon_tacz_damage." + victim.getUUID()) >= now) {
            int kills = incrementWindow(data(killer), "ghroth.noon_kills", now, 50 * 20);
            if (kills >= 13) {
                award(killer, purple(39));
            }
        }
    }

    private static void recordStingerRescue(ServerPlayer rescuer, ServerPlayer target, boolean self, long now) {
        if (!isSelected(rescuer, ModCharacters.STINGER_ID)) {
            return;
        }
        CompoundTag data = data(rescuer);
        if (self) {
            data.putLong("stinger.self_rescue_at", now);
            return;
        }
        if (target == null) {
            return;
        }
        int rescues = increment(data, "stinger.rescues", 1);
        if (rescues >= 3) {
            award(rescuer, green(17));
        }
        long selfAt = data.getLong("stinger.self_rescue_at");
        if (selfAt > 0L && now - selfAt <= 10L * 20L) {
            award(rescuer, purple(17));
        }
        int chain = data.getBoolean("stinger.rescue_chain_damaged")
                ? 0
                : increment(data, "stinger.clean_rescue_chain", 1);
        if (chain >= 12) {
            award(rescuer, purple(18));
        }
    }

    private static void recordVlinderRescue(ServerPlayer rescuer, ServerPlayer target, boolean self, long now) {
        if (!isSelected(rescuer, ModCharacters.VLINDER_ID)) {
            return;
        }
        if (self) {
            data(rescuer).putLong("vlinder.self_rescue_at", now);
            return;
        }
        if (target != null && data(rescuer).getLong("vlinder.plasma." + target.getUUID()) >= now) {
            award(rescuer, purple(20));
        }
    }

    private static void recordTeamRescue(ServerPlayer rescuer, ServerPlayer target, boolean self, long now) {
        if (rescuer != null && target != null && !self
                && data(rescuer).getLong("global.interrupt_rescue_until") >= now) {
            award(rescuer, green(48));
        }
    }

    private static void recordLunaToolCycle(ServerPlayer player, String characterId, SkillSlot slot, long now) {
        if (!ModCharacters.LUNA_ID.equals(characterId)) {
            return;
        }
        int tools = windowUnique(data(player), "luna.tool_cycle", slot.name(), now, 25 * 20);
        if (tools >= 4) {
            award(player, green(24));
        }
    }

    private static void recordUndeadMechanismUse(ServerPlayer player, String characterId, SkillSlot slot, long now) {
        if (!ModCharacters.UNDEAD_ID.equals(characterId) || slot == SkillSlot.PASSIVE) {
            return;
        }
        int mechanisms = windowUnique(data(player), "undead.slot_mechanisms", slot.name(), now, 35 * 20);
        if (mechanisms >= 6) {
            award(player, purple(32));
        }
    }

    private static void recordUndeadLowHealthSwitchKill(ServerPlayer player, String characterId, long now) {
        if (!ModCharacters.UNDEAD_ID.equals(characterId)
                || data(player).getLong("undead.low_health_switch_kill_until") < now) {
            return;
        }
        award(player, purple(33));
    }

    private static void recordDepartmentLaserCalibration(ServerPlayer attacker, LivingEntity target) {
        if (target instanceof ServerPlayer && has(target, ModEffects.DEPARTMENT_CALIBRATION)) {
            int stacks = target.getEffect(ModEffects.DEPARTMENT_CALIBRATION).getAmplifier() + 1;
            recordDepartmentCalibration(attacker, target, stacks);
        }
    }

    private static void recordSaeedGuardKill(ServerPlayer owner, SaeedGuardEntity guard, LivingEntity victim) {
        SaeedGuardType type = guard.guardType();
        if (type == SaeedGuardType.SHARP_EAGLE) {
            recordSaeedEagleFarKill(owner, guard.distanceTo(owner));
        }
        int kills = incrementWindow(data(owner), "saeed.summon_kills", gameTime(owner), 30 * 20);
        if (SaeedStateManager.isCoreBoostActive(owner)
                && SaeedStateManager.activeGuardCountForAchievements(owner) >= 8
                && kills >= 45) {
            award(owner, purple(40));
        }
    }

    private static void recordElectronicDeathSetup(ServerPlayer player, int affectedDevices) {
        CompoundTag data = data(player);
        long until = gameTime(player) + 10L * 20L;
        data.putLong("global.electronic_effect_until", until);
        if (affectedDevices >= 2) {
            data.putLong("global.interrupt_rescue_until", until);
        }
    }

    private static void recordStrongControlChain(ServerPlayer attacker, LivingEntity target, long now) {
        CompoundTag data = data(attacker);
        UUID id = target.getUUID();
        if (has(target, ModEffects.WEBBED)) {
            markControl(data, id, "web", now);
        }
        if (has(target, ModEffects.STUN)) {
            markControl(data, id, "stun", now);
        }
        if (has(target, ModEffects.TEMPEST_DISARMED)) {
            markControl(data, id, "disarm", now);
        }
        if (has(target, ModEffects.TEMPEST_EMERGENCY_DOWNED)
                || has(target, ModEffects.STINGER_DOWNED)
                || has(target, ModEffects.VLINDER_VITAL_DOWNED)
                || has(target, ModEffects.CATDAD_DOWNED)) {
            markControl(data, id, "downed", now);
        }
        if (has(target, ModEffects.RAPTOR_ACTION_PAUSE)) {
            markControl(data, id, "pause", now);
        }
        if (recentControls(data, id, now) >= 3) {
            award(attacker, purple(47));
        }
    }

    private static void markControl(CompoundTag data, UUID id, String control, long now) {
        data.putLong("control." + id + "." + control, now + 10L * 20L);
    }

    private static int recentControls(CompoundTag data, UUID id, long now) {
        int count = 0;
        for (String control : List.of("web", "stun", "disarm", "downed", "pause")) {
            if (data.getLong("control." + id + "." + control) >= now) {
                count++;
            }
        }
        return count;
    }

    private static void markDeathDebuff(ServerPlayer killer, LivingEntity victim, String debuff, long now) {
        data(killer).putLong("death_debuff." + victim.getUUID() + "." + debuff, now + 10L * 20L);
    }

    private static int countRecentDeathDebuffs(CompoundTag data, UUID victim, long now) {
        int count = 0;
        for (String key : List.of("blind", "deaf", "interference")) {
            if (data.getLong("death_debuff." + victim + "." + key) >= now) {
                count++;
            }
        }
        return count;
    }

    private static void trackNoHealWindow(ServerPlayer player, CompoundTag data) {
        float previous = data.contains("no_heal.last_health") ? data.getFloat("no_heal.last_health") : player.getHealth();
        if (player.getHealth() > previous + 0.01F) {
            data.putInt("no_heal.kills", 0);
        }
        data.putFloat("no_heal.last_health", player.getHealth());
    }

    private static void recordNoHealKill(ServerPlayer killer, CompoundTag data) {
        int kills = data.getInt("no_heal.kills") + 1;
        data.putInt("no_heal.kills", kills);
        if (kills >= 2) {
            award(killer, purple(45));
        }
    }

    private static void finishDelayedAwards(ServerPlayer player, CompoundTag data, long now) {
        if (data.getLong("d_wolf.sequence_award_at") > 0L && now >= data.getLong("d_wolf.sequence_award_at")) {
            award(player, green(2));
            data.remove("d_wolf.sequence_award_at");
        }
        if (data.getLong("tempest.self_save_survive_until") > 0L
                && now >= data.getLong("tempest.self_save_survive_until")
                && player.isAlive()) {
            award(player, purple(7));
            data.remove("tempest.self_save_survive_until");
        }
        if (data.getLong("shepherd.frag_survive_until") > 0L && now >= data.getLong("shepherd.frag_survive_until")) {
            award(player, purple(16));
            data.remove("shepherd.frag_survive_until");
        }
        if (data.getLong("undead.rogue_theft_until") > 0L && now >= data.getLong("undead.rogue_theft_until")) {
            award(player, green(34));
            data.remove("undead.rogue_theft_until");
        }
    }

    private static void checkNetherTourist(ServerPlayer player) {
        if (!isSelected(player, ModCharacters.UNDEAD_ID) || !isNoEquipmentAndHandsBlocksOnly(player)) {
            return;
        }
        AdvancementHolder exploreNether = player.server.getAdvancements()
                .get(ResourceLocation.fromNamespaceAndPath("minecraft", "nether/explore_nether"));
        if (exploreNether != null && player.getAdvancements().getOrStartProgress(exploreNether).isDone()) {
            award(player, purple(46));
        }
    }

    private static boolean isNoEquipmentAndHandsBlocksOnly(ServerPlayer player) {
        for (ItemStack stack : player.getArmorSlots()) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return isEmptyOrBlock(player.getMainHandItem()) && isEmptyOrBlock(player.getOffhandItem());
    }

    private static boolean isEmptyOrBlock(ItemStack stack) {
        return stack.isEmpty() || stack.getItem() instanceof BlockItem || stack.is(Items.AIR);
    }

    private static void clearNoDeathChains(CompoundTag data) {
        data.putInt("stinger.clean_rescue_chain", 0);
        data.putBoolean("stinger.rescue_chain_damaged", false);
        data.remove("d_wolf.sequence_award_at");
        data.remove("d_wolf.sequence_until");
        data.putInt("d_wolf.sequence_step", 0);
    }

    private static boolean isDwolfSmokeObscured(ServerPlayer killer, LivingEntity victim) {
        if (!(killer.level() instanceof ServerLevel level)) {
            return false;
        }
        AABB search = new AABB(killer.getEyePosition(), victim.getEyePosition()).inflate(DWolfSmokeCloudEntity.RADIUS + 2.0D);
        for (DWolfSmokeCloudEntity cloud : level.getEntitiesOfClass(DWolfSmokeCloudEntity.class, search, Entity::isAlive)) {
            if (segmentIntersectsSphere(killer.getEyePosition(), victim.getEyePosition(), cloud.position(), DWolfSmokeCloudEntity.RADIUS)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isAnySmokeObscured(ServerPlayer killer, LivingEntity victim) {
        if (!(killer.level() instanceof ServerLevel level)) {
            return false;
        }
        Vec3 start = killer.getEyePosition();
        Vec3 end = victim.getEyePosition();
        AABB search = new AABB(start, end).inflate(10.0D);
        for (DWolfSmokeCloudEntity cloud : level.getEntitiesOfClass(DWolfSmokeCloudEntity.class, search, Entity::isAlive)) {
            if (segmentIntersectsSphere(start, end, cloud.position(), DWolfSmokeCloudEntity.RADIUS)) {
                return true;
            }
        }
        for (StingerSmokeCloudEntity cloud : level.getEntitiesOfClass(StingerSmokeCloudEntity.class, search, Entity::isAlive)) {
            if (segmentIntersectsSphere(start, end, cloud.position(), StingerSmokeCloudEntity.ENHANCED_RADIUS)) {
                return true;
            }
        }
        for (GizmoSmokeCloudEntity cloud : level.getEntitiesOfClass(GizmoSmokeCloudEntity.class, search, Entity::isAlive)) {
            if (segmentIntersectsSphere(start, end, cloud.position(), GizmoSmokeCloudEntity.RADIUS)) {
                return true;
            }
        }
        for (ToxikTearGasCloudEntity cloud : level.getEntitiesOfClass(ToxikTearGasCloudEntity.class, search, Entity::isAlive)) {
            if (segmentIntersectsSphere(start, end, cloud.position(), ToxikTearGasCloudEntity.RADIUS)) {
                return true;
            }
        }
        return false;
    }

    private static boolean segmentIntersectsSphere(Vec3 start, Vec3 end, Vec3 center, double radius) {
        Vec3 segment = end.subtract(start);
        double lengthSqr = segment.lengthSqr();
        if (lengthSqr < 0.0001D) {
            return start.distanceTo(center) <= radius;
        }
        double t = center.subtract(start).dot(segment) / lengthSqr;
        t = Math.max(0.0D, Math.min(1.0D, t));
        return start.add(segment.scale(t)).distanceTo(center) <= radius;
    }

    private static boolean isBehind(ServerPlayer attacker, LivingEntity victim) {
        Vec3 toAttacker = attacker.position().subtract(victim.position());
        Vec3 horizontal = new Vec3(toAttacker.x, 0.0D, toAttacker.z);
        if (horizontal.lengthSqr() < 0.0001D) {
            return false;
        }
        Vec3 victimLook = victim.getLookAngle();
        Vec3 victimHorizontal = new Vec3(victimLook.x, 0.0D, victimLook.z);
        if (victimHorizontal.lengthSqr() < 0.0001D) {
            return false;
        }
        return victimHorizontal.normalize().dot(horizontal.normalize()) < -0.45D;
    }

    private static boolean has(LivingEntity entity, Holder<MobEffect> effect) {
        return entity != null && effect != null && entity.hasEffect(effect);
    }

    private static boolean isMsg(String msg, String id) {
        return msg != null && msg.equals(DealtForceSkillsMod.MODID + "." + id);
    }

    private static long gameTime(ServerPlayer player) {
        return player.level().getGameTime();
    }

    private static void award(ServerPlayer player, String path) {
        if (player == null || path == null || path.isBlank()) {
            return;
        }
        AdvancementHolder advancement = advancement(player, path);
        if (advancement == null) {
            return;
        }
        boolean changed = player.getAdvancements().award(advancement, "unlock");
        if (changed && !FINAL.equals(path)) {
            tryAwardFinal(player);
        }
    }

    private static AdvancementHolder advancement(ServerPlayer player, String path) {
        return player.server.getAdvancements().get(
                ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "delta_force_skills/" + path));
    }

    private static boolean isDone(ServerPlayer player, String path) {
        AdvancementHolder advancement = advancement(player, path);
        return advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone();
    }

    private static void tryAwardFinal(ServerPlayer player) {
        AdvancementHolder finalAdvancement = advancement(player, FINAL);
        if (finalAdvancement == null || player.getAdvancements().getOrStartProgress(finalAdvancement).isDone()) {
            return;
        }
        for (String path : NORMAL_ADVANCEMENTS) {
            if (!isDone(player, path)) {
                return;
            }
        }
        player.getAdvancements().award(finalAdvancement, "unlock");
    }

    private static CompoundTag data(ServerPlayer player) {
        CompoundTag persistent = player.getPersistentData();
        if (!persistent.contains(DATA_KEY, Tag.TAG_COMPOUND)) {
            persistent.put(DATA_KEY, new CompoundTag());
        }
        return persistent.getCompound(DATA_KEY);
    }

    private static int increment(CompoundTag data, String key, int amount) {
        int value = data.getInt(key) + amount;
        data.putInt(key, value);
        return value;
    }

    private static int incrementWindow(CompoundTag data, String group, long now, int windowTicks) {
        String startKey = group + ".start";
        long start = data.getLong(startKey);
        if (start <= 0L || now - start > windowTicks) {
            data.putLong(startKey, now);
            data.putInt(group + ".count", 0);
        }
        int count = data.getInt(group + ".count") + 1;
        data.putInt(group + ".count", count);
        return count;
    }

    private static int markUnique(CompoundTag data, String group, String id) {
        if (!data.contains(group, Tag.TAG_COMPOUND)) {
            data.put(group, new CompoundTag());
        }
        CompoundTag flags = data.getCompound(group);
        flags.putBoolean(normalizeId(id), true);
        return flags.getAllKeys().size();
    }

    private static int windowUnique(CompoundTag data, String group, String id, long now, int windowTicks) {
        String startKey = group + ".start";
        long start = data.getLong(startKey);
        if (start <= 0L || now - start > windowTicks) {
            data.putLong(startKey, now);
            data.put(group, new CompoundTag());
        }
        return markUnique(data, group, id);
    }

    private static boolean isSelected(ServerPlayer player, String characterId) {
        return characterId.equals(selectedCharacterId(player));
    }

    private static String selectedCharacterId(ServerPlayer player) {
        return CharacterSelectionManager.getSelectedCharacter(player)
                .map(character -> character.id())
                .orElse("");
    }

    private static String normalizeId(String id) {
        return id == null ? "unknown" : id.toLowerCase(Locale.ROOT).replace(' ', '_');
    }

    private static String green(int number) {
        return numbered("green", number);
    }

    private static String purple(int number) {
        return numbered("purple", number);
    }

    private static String numbered(String group, int number) {
        if (number < 10) {
            return group + "/00" + number;
        }
        if (number < 100) {
            return group + "/0" + number;
        }
        return group + "/" + number;
    }

    private static List<String> buildNormalAdvancements() {
        List<String> paths = new ArrayList<>();
        for (int i = 1; i <= 55; i++) {
            paths.add(green(i));
        }
        for (int i = 1; i <= 56; i++) {
            paths.add(purple(i));
        }
        return List.copyOf(paths);
    }

    public static void awardAll(ServerPlayer player) {
        if (player == null) return;
        for (AdvancementHolder advancement : player.server.getAdvancements().getAllAdvancements()) {
            AdvancementProgress progress = player.getAdvancements().getOrStartProgress(advancement);
            List<String> remaining = new ArrayList<>();
            progress.getRemainingCriteria().forEach(remaining::add);
            for (String criterion : remaining) player.getAdvancements().award(advancement, criterion);
        }
    }
}
