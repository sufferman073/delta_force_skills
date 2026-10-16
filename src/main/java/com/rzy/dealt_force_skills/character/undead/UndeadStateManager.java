package com.rzy.dealt_force_skills.character.undead;

import com.rzy.dealt_force_skills.advancement.DfsAchievements;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.CharacterSelectionManager;
import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.character.manba.ManbaStateManager;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.S2C_ManbaOpportunityMarkers;
import com.rzy.dealt_force_skills.network.S2C_SyncUndeadState;
import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.skill.SkillCooldownHelper;
import com.rzy.dealt_force_skills.skill.SkillDamageHelper;
import com.rzy.dealt_force_skills.util.TargetingUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;

public final class UndeadStateManager {
    public static float baseMaxEnergy() {
        return com.rzy.dealt_force_skills.config.DealtForceConfig.floatValue("characters.undead.undead_state_manager.base_max_energy", 100.0F);
    }

    public static float baseEnergyRegenPerTick() {
        return com.rzy.dealt_force_skills.config.DealtForceConfig.floatValue("characters.undead.undead_state_manager.base_energy_regen_per_tick", 4.0F / 20.0F);
    }
    public static volatile double HUNTER_WALL_CLIMB_SPEED_PER_TICK = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("HUNTER_WALL_CLIMB_SPEED_PER_TICK", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue(
      "characters.undead.undead_state_manager.hunter_wall_climb_speed_per_tick", 0.8
   ));
    private static volatile float HUNTER_HORIZONTAL_DRAIN_PER_TICK = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("HUNTER_HORIZONTAL_DRAIN_PER_TICK", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.floatValue(
      "characters.undead.undead_state_manager.hunter_horizontal_drain_per_tick", 0.15F
   ));
    private static volatile float HUNTER_VERTICAL_DRAIN_PER_TICK = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("HUNTER_VERTICAL_DRAIN_PER_TICK", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.floatValue(
      "characters.undead.undead_state_manager.hunter_vertical_drain_per_tick", 0.25F
   ));
    private static volatile double HUNTER_MOVEMENT_THRESHOLD_SQR = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("HUNTER_MOVEMENT_THRESHOLD_SQR", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue(
      "characters.undead.undead_state_manager.hunter_movement_threshold_sqr", 0.0025
   ));
    private static volatile double HUNTER_MAX_TRACKED_DISPLACEMENT_SQR = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("HUNTER_MAX_TRACKED_DISPLACEMENT_SQR", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue(
      "characters.undead.undead_state_manager.hunter_max_tracked_displacement_sqr", 4.0
   ));
    private static volatile float WARRIOR_BLOODLUST_SELF_HEALTH_COST_FRACTION = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("WARRIOR_BLOODLUST_SELF_HEALTH_COST_FRACTION", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.floatValue(
      "characters.undead.undead_state_manager.warrior_bloodlust_self_health_cost_fraction", 0.04F
   ));
    private static volatile double EXPLORER_SPIRIT_OPPORTUNITY_RANGE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("EXPLORER_SPIRIT_OPPORTUNITY_RANGE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue(
      "characters.undead.undead_state_manager.explorer_spirit_opportunity_range", ManbaStateManager.OPPORTUNITY_WINDOW_RANGE * 2.0
   ));
    private static final String ROOT_TAG = DealtForceSkillsMod.MODID + ".undead";
    private static final String INITIALIZED = "Initialized";
    private static final String PROFESSION = "Profession";
    private static final String ENERGY = "Energy";
    private static final String DISORIENTED_UNTIL = "DisorientedUntil";
    private static final String KNIGHT_CHARGE_UNTIL = "KnightChargeUntil";
    private static final String KNIGHT_CHARGE_TARGET = "KnightChargeTarget";
    private static final String KNIGHT_PARRY_UNTIL = "KnightParryUntil";
    private static final String KNIGHT_SHIELD_UNTIL = "KnightShieldUntil";
    private static final String KNIGHT_SHIELD = "KnightShield";
    private static final String WARRIOR_MIGHT = "WarriorMight";
    private static final String WARRIOR_BLOODLUST_UNTIL = "WarriorBloodlustUntil";
    private static final String EXPLORER_MEDITATING_UNTIL = "ExplorerMeditatingUntil";
    private static final String EXPLORER_SPACE_UNTIL = "ExplorerSpaceUntil";
    private static final String ROGUE_INVISIBLE_UNTIL = "RogueInvisibleUntil";
    private static final String ROGUE_INVULNERABLE_UNTIL = "RogueInvulnerableUntil";
    private static final String ROGUE_SPEED_UNTIL = "RogueSpeedUntil";
    private static final String ROGUE_PASSIVE_READY_AT = "RoguePassiveReadyAt";
    private static final String SCHOLAR_RITUAL_UNTIL = "ScholarRitualUntil";
    private static final String SCHOLAR_RECORDS = "ScholarRecords";
    private static final String HUNTER_SCATTER = "HunterScatter";
    private static final String HUNTER_SCATTER_YAW = "HunterScatterYaw";
    private static final String HUNTER_EXHAUSTED_UNTIL = "HunterExhaustedUntil";
    private static final String HUNTER_RELOAD_READY_AT = "HunterReloadReadyAt";
    private static final String HUNTER_POSITION_TRACKED = "HunterPositionTracked";
    private static final String HUNTER_LAST_X = "HunterLastX";
    private static final String HUNTER_LAST_Y = "HunterLastY";
    private static final String HUNTER_LAST_Z = "HunterLastZ";
    private static final String RUNTIME_INVISIBLE = "RuntimeInvisible";
    private static final String CORE_COOLDOWN_PREFIX = "CoreCooldown_";
    private static final String INPUT_STARTED_PREFIX = "InputStarted_";
    private static final String INPUT_RESOLVED_PREFIX = "InputResolved_";

    private static final String RUPTURE_UNTIL = DealtForceSkillsMod.MODID + ".undead_rupture_until";
    private static final String RUPTURE_NEXT = DealtForceSkillsMod.MODID + ".undead_rupture_next";
    private static final String RITUAL_DANCE_UNTIL = DealtForceSkillsMod.MODID + ".undead_ritual_dance_until";
    private static final String ROGUE_EXECUTION_OWNER = DealtForceSkillsMod.MODID + ".undead_rogue_execution_owner";
    private static final String OPAL_BURN_UNTIL = DealtForceSkillsMod.MODID + ".undead_opal_burn_until";
    private static final String OPAL_BURN_NEXT = DealtForceSkillsMod.MODID + ".undead_opal_burn_next";
    private static final String FROZEN_BLADE_STACKS = DealtForceSkillsMod.MODID + ".undead_frozen_blade_stacks";
    private static final String FROZEN_BLADE_UNTIL = DealtForceSkillsMod.MODID + ".undead_frozen_blade_until";
    public static final String HUNTER_RETREAT_OWNER = DealtForceSkillsMod.MODID + ".undead_hunter_retreat_owner";
    private static final ResourceLocation FROZEN_BLADE_SPEED_UUID = ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "undead_state_manager.frozen_blade_speed");

    private static final ResourceLocation HUNTER_SPEED_UUID = ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "undead_state_manager.hunter_speed");
    private static final ResourceLocation HUNTER_SCATTER_SLOW_UUID = ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "undead_state_manager.hunter_scatter_slow");
    private static final AttributeModifier HUNTER_SPEED = new AttributeModifier(HUNTER_SPEED_UUID, 0.5D,
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
    );

    private UndeadStateManager() {
    }

    public static boolean isUndead(Player player) {
        Optional<String> selected = CharacterSelectionManager.getSelectedCharacterId(player);
        return selected.isPresent() && ModCharacters.UNDEAD_ID.equals(selected.get());
    }

    public static UndeadProfession profession(Player player) {
        return UndeadProfession.byOrdinal(data(player).getInt(PROFESSION));
    }

    public static void initializeIfNeeded(ServerPlayer player) {
        if (!isUndead(player)) {
            return;
        }
        CompoundTag tag = data(player);
        if (tag.getBoolean(INITIALIZED)) {
            return;
        }
        tag.putBoolean(INITIALIZED, true);
        tag.putInt(PROFESSION, UndeadProfession.KNIGHT.ordinal());
        tag.putFloat(ENERGY, baseMaxEnergy());
        tag.putLong(DISORIENTED_UNTIL, 0L);
        for (UndeadProfession profession : UndeadProfession.values()) {
            tag.putLong(coreCooldownKey(profession), 0L);
        }
        syncToClient(player);
    }

    public static void copyState(Player original, Player replacement) {
        CompoundTag from = original.getPersistentData();
        if (from.contains(ROOT_TAG, Tag.TAG_COMPOUND)) {
            replacement.getPersistentData().put(ROOT_TAG, from.getCompound(ROOT_TAG).copy());
            clearTransientState(replacement);
        }
    }

    public static void clearRuntime(Player player) {
        clearHunterSpeed(player);
        clearHunterScatterSlow(player);
        clearRuntimeInvisibility(player);
        player.removeEffect(ModEffects.UNDEAD_HUNTER_SCATTER);
        UndeadUpgradeManager.clearAttributes(player);
    }

    public static void clearRoundTransientState(ServerPlayer player) {
        CompoundTag persistent = player.getPersistentData();
        if (!persistent.contains(ROOT_TAG, Tag.TAG_COMPOUND)) {
            clearRuntime(player);
            return;
        }
        clearTransientState(player);
        if (isUndead(player)) {
            UndeadUpgradeManager.applyAttributes(player);
            syncToClient(player);
        }
    }

    public static void onDeselected(Player player) {
        CompoundTag persistent = player.getPersistentData();
        if (!persistent.contains(ROOT_TAG, Tag.TAG_COMPOUND)) {
            return;
        }
        clearTransientState(player);
    }

    public static void tick(ServerPlayer player) {
        if (!isUndead(player)) {
            clearRuntime(player);
            return;
        }
        initializeIfNeeded(player);
        CompoundTag tag = data(player);
        long now = SkillCooldownHelper.now(player);
        UndeadUpgradeManager.applyAttributes(player);
        UndeadSupportManager.tick(player);

        if (now < tag.getLong(HUNTER_EXHAUSTED_UNTIL)) {
            player.setDeltaMovement(Vec3.ZERO);
        }

        tickKnight(player, tag, now);
        tickWarrior(player, tag, now);
        tickExplorer(player, tag, now);
        syncExplorerOpportunityWindow(player, tag, now);
        tickRogue(player, tag, now);
        tickScholar(player, tag, now);
        tickHunter(player, tag, now);
        UndeadSkills.tickHeldInputs(player);
        updateRuntimeInvisibility(player, now);
        updateHunterScatterVisual(player, tag);
        if (now >= tag.getLong(HUNTER_EXHAUSTED_UNTIL)) {
            addEnergy(player, baseEnergyRegenPerTick()
                    * UndeadUpgradeManager.energyRegenMultiplier(player));
        }
        syncToClient(player);
    }

    public static void switchProfession(ServerPlayer player, UndeadProfession next) {
        if (!isUndead(player)) {
            return;
        }
        initializeIfNeeded(player);
        CompoundTag tag = data(player);
        if (profession(player) == next) {
            return;
        }
        tag.putInt(PROFESSION, next.ordinal());
        DfsAchievements.recordUndeadLowHealthSwitch(player, next);
        tag.putLong(DISORIENTED_UNTIL, SkillCooldownHelper.now(player) + 2L * 20L);
        player.displayClientMessage(Component.translatable(
                "message.dealt_force_skills.undead.profession_changed",
                Component.translatable(next.translationKey())), true);
        player.level().playSound(null, player.blockPosition(), SoundEvents.ARMOR_EQUIP_CHAIN.value(),
                SoundSource.PLAYERS, 0.7F, 1.15F);
        syncToClient(player);
    }

    public static float energy(Player player) {
        return Math.max(0.0F, Math.min(UndeadUpgradeManager.maxEnergy(player), data(player).getFloat(ENERGY)));
    }

    public static void addEnergy(ServerPlayer player, float amount) {
        if (amount <= 0.0F) {
            return;
        }
        setEnergy(player, energy(player) + amount);
    }

    public static boolean consumeEnergy(ServerPlayer player, float baseCost) {
        initializeIfNeeded(player);
        float cost = effectiveEnergyCost(player, baseCost);
        if (energy(player) + 0.0001F < cost) {
            player.displayClientMessage(Component.translatable(
                    "message.dealt_force_skills.undead.not_enough_energy",
                    Math.round(cost)), true);
            return false;
        }
        setEnergy(player, energy(player) - cost);
        return true;
    }

    public static boolean isActionLocked(Player player) {
        if (!isUndead(player)) {
            return false;
        }
        long now = SkillCooldownHelper.now(player);
        CompoundTag tag = data(player);
        return now < tag.getLong(KNIGHT_PARRY_UNTIL)
                || now < tag.getLong(EXPLORER_MEDITATING_UNTIL)
                || now < tag.getLong(SCHOLAR_RITUAL_UNTIL)
                || now < tag.getLong(HUNTER_EXHAUSTED_UNTIL);
    }

    public static boolean startCoreCooldown(ServerPlayer player, UndeadProfession profession, int ticks) {
        long now = SkillCooldownHelper.now(player);
        String key = coreCooldownKey(profession);
        CompoundTag tag = data(player);
        if (now < tag.getLong(key)) {
            SkillCooldownHelper.notifyCooldown(player, Component.translatable(
                    "message.dealt_force_skills.undead.core_cooldown"));
            return false;
        }
        tag.putLong(key, now + UndeadUpgradeManager.cooldownTicks(player, ticks));
        return true;
    }

    public static int coreCooldownRemaining(Player player, UndeadProfession profession) {
        return remainingTicks(player, coreCooldownKey(profession));
    }

    public static void startKnightCharge(ServerPlayer player) {
        data(player).putInt(KNIGHT_CHARGE_TARGET, -1);
        data(player).putLong(KNIGHT_CHARGE_UNTIL, SkillCooldownHelper.now(player) + 12L);
    }

    public static void startKnightCharge(ServerPlayer player, LivingEntity target) {
        data(player).putInt(KNIGHT_CHARGE_TARGET, target.getId());
        data(player).putLong(KNIGHT_CHARGE_UNTIL, SkillCooldownHelper.now(player) + 30L);
    }

    public static void startKnightParry(ServerPlayer player) {
        data(player).putLong(KNIGHT_PARRY_UNTIL, SkillCooldownHelper.now(player) + 16L);
    }

    public static void startKnightShield(ServerPlayer player) {
        CompoundTag tag = data(player);
        tag.putLong(KNIGHT_SHIELD_UNTIL, SkillCooldownHelper.now(player) + 20L * 20L);
        tag.putFloat(KNIGHT_SHIELD, 50.0F);
    }

    public static void toggleWarriorMight(ServerPlayer player) {
        CompoundTag tag = data(player);
        tag.putBoolean(WARRIOR_MIGHT, !tag.getBoolean(WARRIOR_MIGHT));
    }

    public static boolean warriorMightActive(Player player) {
        return data(player).getBoolean(WARRIOR_MIGHT);
    }

    public static void startWarriorBloodlust(ServerPlayer player) {
        data(player).putLong(WARRIOR_BLOODLUST_UNTIL, SkillCooldownHelper.now(player) + 15L * 20L);
    }

    public static void toggleExplorerMeditation(ServerPlayer player) {
        CompoundTag tag = data(player);
        long now = SkillCooldownHelper.now(player);
        if (now < tag.getLong(EXPLORER_MEDITATING_UNTIL)) {
            tag.putLong(EXPLORER_MEDITATING_UNTIL, 0L);
        } else {
            tag.putLong(EXPLORER_MEDITATING_UNTIL, now + 10L * 20L);
            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.undead.undead_state_manager.effect.absorption.0.duration_ticks", 10 * 20), com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.undead.undead_state_manager.effect.absorption.0.amplifier", 1), false, true, true));
        }
    }

    public static boolean startExplorerMeditation(ServerPlayer player) {
        if (explorerMeditating(player) || !consumeEnergy(player, 10.0F)) {
            return false;
        }
        data(player).putLong(EXPLORER_MEDITATING_UNTIL, SkillCooldownHelper.now(player) + 10L * 20L);
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.undead.undead_state_manager.effect.absorption.1.duration_ticks", 10 * 20), com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.undead.undead_state_manager.effect.absorption.1.amplifier", 1),
                false, true, true));
        return true;
    }

    public static void stopExplorerMeditation(ServerPlayer player) {
        data(player).putLong(EXPLORER_MEDITATING_UNTIL, 0L);
    }

    public static boolean explorerMeditating(Player player) {
        return SkillCooldownHelper.now(player) < data(player).getLong(EXPLORER_MEDITATING_UNTIL);
    }

    public static void startExplorerSpace(ServerPlayer player) {
        data(player).putLong(EXPLORER_SPACE_UNTIL, SkillCooldownHelper.now(player) + 30L * 20L);
    }

    public static boolean explorerSpaceActive(Player player) {
        return SkillCooldownHelper.now(player) < data(player).getLong(EXPLORER_SPACE_UNTIL);
    }

    public static void stopExplorerSpace(ServerPlayer player) {
        data(player).putLong(EXPLORER_SPACE_UNTIL, 0L);
        clearRuntimeInvisibility(player);
        NetworkHandler.sendToPlayer(new S2C_ManbaOpportunityMarkers(List.of()), player);
    }

    public static void startRogueStealth(ServerPlayer player) {
        int duration = UndeadUpgradeManager.has(player, com.rzy.dealt_force_skills.shop.UndeadShopEntry.ROGUE_SILENT)
                ? 4 * 20
                : 2 * 20;
        long until = SkillCooldownHelper.now(player) + duration;
        CompoundTag tag = data(player);
        tag.putLong(ROGUE_INVISIBLE_UNTIL, until);
        tag.putLong(ROGUE_INVULNERABLE_UNTIL, until);
        tag.putLong(ROGUE_SPEED_UNTIL, until);
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,
                duration,
                com.rzy.dealt_force_skills.config.DealtForceConfig.intValue(
                        "characters.undead.undead_state_manager.effect.movement_speed.4.amplifier", 1),
                false, false, true));
    }

    public static boolean isRogueInvisible(Player player) {
        return isUndead(player)
                && profession(player) == UndeadProfession.ROGUE
                && SkillCooldownHelper.now(player) < data(player).getLong(ROGUE_INVISIBLE_UNTIL);
    }

    public static void startScholarRitual(ServerPlayer player) {
        long duration = UndeadUpgradeManager.has(
                player, com.rzy.dealt_force_skills.shop.UndeadShopEntry.SCHOLAR_RETURNED) ? 100L : 50L;
        data(player).putLong(SCHOLAR_RITUAL_UNTIL, SkillCooldownHelper.now(player) + duration);
    }

    public static boolean recordScholarTarget(ServerPlayer player, LivingEntity target) {
        String id = target instanceof Player
                ? "minecraft:player"
                : String.valueOf(BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()));
        CompoundTag records = scholarRecords(player);
        int current = Math.max(0, records.getInt(id));
        if (current >= 2) {
            return false;
        }
        int typeLimit = UndeadUpgradeManager.has(
                player, com.rzy.dealt_force_skills.shop.UndeadShopEntry.SCHOLAR_ANCIENT_SCROLL) ? 10 : 5;
        if (!records.contains(id) && records.getAllKeys().size() >= typeLimit) {
            return false;
        }
        records.putInt(id, current + 1);
        return true;
    }

    public static int scholarRecordCount(Player player, LivingEntity target) {
        String id = target instanceof Player
                ? "minecraft:player"
                : String.valueOf(BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()));
        return Math.max(0, scholarRecords(player).getInt(id));
    }

    public static int scholarRecordedTypes(Player player) {
        return scholarRecords(player).getAllKeys().size();
    }

    public static int scholarRecordTypeLimit(Player player) {
        return UndeadUpgradeManager.has(
                player, com.rzy.dealt_force_skills.shop.UndeadShopEntry.SCHOLAR_ANCIENT_SCROLL) ? 10 : 5;
    }

    public static void scheduleHunterBlast(ServerPlayer player) {
        PrimedTnt tnt = new PrimedTnt(player.level(),
                player.getX(), player.getEyeY() - 0.25D, player.getZ(), player);
        tnt.setFuse(2 * 20);
        tnt.getPersistentData().putUUID(HUNTER_RETREAT_OWNER, player.getUUID());
        Vec3 velocity = player.getLookAngle().normalize().scale(0.9D).add(0.0D, 0.2D, 0.0D);
        tnt.setDeltaMovement(velocity);
        player.level().addFreshEntity(tnt);
        player.serverLevel().sendParticles(ParticleTypes.SMOKE,
                player.getX(), player.getEyeY() - 0.25D, player.getZ(),
                12, 0.2D, 0.15D, 0.2D, 0.02D);
    }

    public static boolean isHunterRetreatTnt(Entity entity) {
        return entity instanceof PrimedTnt
                && entity.getPersistentData().hasUUID(HUNTER_RETREAT_OWNER);
    }

    public static void handleHunterRetreatExplosion(ServerLevel level, Entity tnt) {
        if (!isHunterRetreatTnt(tnt)) {
            return;
        }
        UUID ownerId = tnt.getPersistentData().getUUID(HUNTER_RETREAT_OWNER);
        Entity ownerEntity = level.getEntity(ownerId);
        ServerPlayer owner = ownerEntity instanceof ServerPlayer player ? player : null;
        Vec3 center = tnt.position();
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                center.x, center.y + 0.2D, center.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(ParticleTypes.CLOUD,
                center.x, center.y + 0.2D, center.z, 55, 1.2D, 0.45D, 1.2D, 0.08D);
        level.playSound(null, center.x, center.y, center.z,
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.9F, 1.35F);
        blastHunterArea(level, owner, center);
    }

    public static boolean hunterReloadReady(Player player) {
        return SkillCooldownHelper.now(player) >= data(player).getLong(HUNTER_RELOAD_READY_AT);
    }

    public static void useHunterReload(ServerPlayer player) {
        data(player).putLong(HUNTER_RELOAD_READY_AT, SkillCooldownHelper.now(player)
                + UndeadUpgradeManager.cooldownTicks(player, 2 * 20));
        addEnergy(player, 50.0F);
    }

    public static void toggleHunterScatter(ServerPlayer player) {
        CompoundTag tag = data(player);
        boolean enabled = !tag.getBoolean(HUNTER_SCATTER);
        tag.putBoolean(HUNTER_SCATTER, enabled);
        if (enabled) {
            tag.putFloat(HUNTER_SCATTER_YAW, player.getYRot());
        }
    }

    public static boolean hunterScatterActive(Player player) {
        return data(player).getBoolean(HUNTER_SCATTER);
    }

    public static boolean isRitualDancing(LivingEntity entity) {
        return entity.level().getGameTime() < entity.getPersistentData().getLong(RITUAL_DANCE_UNTIL);
    }

    public static void beginSkillInput(ServerPlayer player, com.rzy.dealt_force_skills.character.SkillSlot slot) {
        CompoundTag tag = data(player);
        tag.remove(INPUT_RESOLVED_PREFIX + slot.name());
        tag.putLong(INPUT_STARTED_PREFIX + slot.name(), SkillCooldownHelper.now(player));
    }

    public static int heldSkillInputTicks(ServerPlayer player, com.rzy.dealt_force_skills.character.SkillSlot slot) {
        String key = INPUT_STARTED_PREFIX + slot.name();
        CompoundTag tag = data(player);
        if (!tag.contains(key, Tag.TAG_LONG)) {
            return 0;
        }
        long started = tag.getLong(key);
        return (int) Math.max(0L, Math.min(20L * 30L, SkillCooldownHelper.now(player) - started));
    }

    public static void clearSkillInput(ServerPlayer player, com.rzy.dealt_force_skills.character.SkillSlot slot) {
        data(player).remove(INPUT_STARTED_PREFIX + slot.name());
    }

    public static void markSkillInputResolved(ServerPlayer player, com.rzy.dealt_force_skills.character.SkillSlot slot) {
        data(player).putBoolean(INPUT_RESOLVED_PREFIX + slot.name(), true);
    }

    public static boolean consumeSkillInputResolved(
            ServerPlayer player,
            com.rzy.dealt_force_skills.character.SkillSlot slot
    ) {
        CompoundTag tag = data(player);
        String key = INPUT_RESOLVED_PREFIX + slot.name();
        boolean resolved = tag.getBoolean(key);
        tag.remove(key);
        return resolved;
    }

    public static void markRogueExecution(LivingEntity victim, ServerPlayer owner) {
        victim.getPersistentData().putUUID(ROGUE_EXECUTION_OWNER, owner.getUUID());
    }

    public static void executeRogueNonPlayer(ServerPlayer owner, LivingEntity victim) {
        if (!com.rzy.dealt_force_skills.boss.BossCombatRules.canInstantKill(victim)) {
            return;
        }
        markRogueExecution(victim, owner);
        DamageSource source = SkillDamageHelper.trueDamage(owner.serverLevel(), owner, owner);
        victim.invulnerableTime = 0;
        victim.hurt(source, Math.max(1000.0F, victim.getMaxHealth() * 100.0F));
        victim.kill();
        if (isStillAliveAfterRogueExecution(victim)) {
            forceRogueExecutionDeath(source, victim);
        }
    }

    public static boolean consumeRogueExecutionMarker(LivingEntity victim, ServerPlayer owner) {
        CompoundTag tag = victim.getPersistentData();
        if (!tag.hasUUID(ROGUE_EXECUTION_OWNER)
                || !owner.getUUID().equals(tag.getUUID(ROGUE_EXECUTION_OWNER))) {
            return false;
        }
        tag.remove(ROGUE_EXECUTION_OWNER);
        return true;
    }

    private static boolean isStillAliveAfterRogueExecution(LivingEntity victim) {
        return !victim.isRemoved() && victim.isAlive() && victim.getHealth() > 0.0F;
    }

    private static void forceRogueExecutionDeath(DamageSource source, LivingEntity victim) {
        victim.setHealth(0.0F);
        victim.die(source);
        if (isStillAliveAfterRogueExecution(victim)) {
            victim.kill();
        }
        if (isStillAliveAfterRogueExecution(victim)) {
            victim.remove(Entity.RemovalReason.KILLED);
        }
        if (isStillAliveAfterRogueExecution(victim)) {
            victim.discard();
        }
    }

    public static boolean blocksIncomingAttack(ServerPlayer player) {
        if (!isUndead(player)) {
            return false;
        }
        long now = SkillCooldownHelper.now(player);
        CompoundTag tag = data(player);
        return now < tag.getLong(EXPLORER_SPACE_UNTIL)
                || UndeadUpgradeManager.isPinkBraceletInvulnerable(player)
                || now < tag.getLong(ROGUE_INVULNERABLE_UNTIL);
    }

    public static boolean blocksOutgoingAttack(ServerPlayer player) {
        if (!isUndead(player)) {
            return false;
        }
        if (isActionLocked(player)
                || SkillCooldownHelper.now(player) < data(player).getLong(EXPLORER_SPACE_UNTIL)) {
            return true;
        }
        revealRogueFromOffense(player);
        return false;
    }

    public static float handleIncomingHurt(ServerPlayer player, DamageSource source, float amount) {
        if (!isUndead(player) || amount <= 0.0F) {
            return amount;
        }
        CompoundTag tag = data(player);
        long now = SkillCooldownHelper.now(player);
        UndeadProfession profession = profession(player);

        if (UndeadUpgradeManager.isPinkBraceletInvulnerable(player)
                || player.getRandom().nextFloat() < UndeadUpgradeManager.dodgeChance(player)) {
            return 0.0F;
        }

        if (now < tag.getLong(KNIGHT_PARRY_UNTIL)) {
            tag.putLong(KNIGHT_PARRY_UNTIL, 0L);
            addEnergy(player, 15.0F);
            player.serverLevel().sendParticles(ParticleTypes.FLASH,
                    player.getX(), player.getY() + 1.0D, player.getZ(),
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
            player.serverLevel().sendParticles(ParticleTypes.ENCHANTED_HIT,
                    player.getX(), player.getY() + 1.0D, player.getZ(),
                    22, 0.65D, 0.75D, 0.65D, 0.08D);
            player.serverLevel().playSound(null, player.blockPosition(),
                    SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0F, 0.65F);
            if (source.getEntity() instanceof LivingEntity attacker) {
                attacker.addEffect(new MobEffectInstance(ModEffects.STUN, com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.undead.undead_state_manager.effect.stun.2.duration_ticks", 3 * 20), com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.undead.undead_state_manager.effect.stun.2.amplifier", 0),
                        false, true, true), player);
                player.serverLevel().sendParticles(ParticleTypes.CRIT,
                        attacker.getX(), attacker.getY() + attacker.getBbHeight() * 0.5D, attacker.getZ(),
                        18, 0.35D, 0.35D, 0.35D, 0.08D);
            }
            return 0.0F;
        }

        if (tag.getFloat(KNIGHT_SHIELD) > 0.0F && now < tag.getLong(KNIGHT_SHIELD_UNTIL)) {
            float remaining = Math.max(0.0F, tag.getFloat(KNIGHT_SHIELD) - amount);
            tag.putFloat(KNIGHT_SHIELD, remaining);
            player.serverLevel().sendParticles(ParticleTypes.ENCHANTED_HIT,
                    player.getX(), player.getY() + 1.0D, player.getZ(),
                    12, 0.65D, 0.75D, 0.65D, 0.04D);
            if (remaining <= 0.0F) {
                tag.putLong(KNIGHT_SHIELD_UNTIL, 0L);
                player.serverLevel().sendParticles(ParticleTypes.POOF,
                        player.getX(), player.getY() + 1.0D, player.getZ(),
                        22, 0.75D, 0.85D, 0.75D, 0.08D);
                player.serverLevel().playSound(null, player.blockPosition(),
                        SoundEvents.SHIELD_BREAK, SoundSource.PLAYERS, 0.9F, 0.9F);
            }
            return 0.0F;
        }

        if (profession == UndeadProfession.KNIGHT) {
            amount = Math.min(amount * 0.7F, player.getMaxHealth() * 0.45F);
            if (UndeadUpgradeManager.has(
                    player, com.rzy.dealt_force_skills.shop.UndeadShopEntry.KNIGHT_INDESTRUCTIBLE)) {
                float missingRatio = 1.0F - player.getHealth() / Math.max(1.0F, player.getMaxHealth());
                amount *= 1.0F - 0.15F * missingRatio;
            }
        } else if (profession == UndeadProfession.SCHOLAR) {
            amount *= isScholarExceptionalDamage(source) ? 2.0F : 0.5F;
        } else if (profession == UndeadProfession.HUNTER && source.is(DamageTypes.FALL)) {
            amount = 0.0F;
        } else if (profession == UndeadProfession.ROGUE
                && now >= tag.getLong(ROGUE_PASSIVE_READY_AT)) {
            long duration = UndeadUpgradeManager.has(
                    player, com.rzy.dealt_force_skills.shop.UndeadShopEntry.ROGUE_SILENT) ? 60L : 30L;
            tag.putLong(ROGUE_INVISIBLE_UNTIL, now + duration);
            tag.putLong(ROGUE_PASSIVE_READY_AT, now + 10L * 20L);
            player.serverLevel().sendParticles(ParticleTypes.POOF,
                    player.getX(), player.getY() + 0.9D, player.getZ(),
                    18, 0.55D, 0.7D, 0.55D, 0.04D);
        }
        if (source.getEntity() instanceof LivingEntity attacker) {
            int records = scholarRecordCount(player, attacker);
            float reduction = attacker instanceof Player ? 0.15F : 0.49F;
            amount *= Math.max(0.0F, 1.0F - reduction * records);
        }
        amount *= UndeadUpgradeManager.incomingDamageMultiplier(player, source);
        if (UndeadUpgradeManager.tryPinkBraceletFatalGuard(player, amount)) {
            DfsAchievements.recordFatalAvoidance(player);
            return 0.0F;
        }
        return amount;
    }

    public static float handleOutgoingHurt(ServerPlayer player, LivingEntity target, DamageSource source, float amount) {
        if (!isUndead(player) || amount <= 0.0F || source.is(SkillDamageHelper.TRUE_SKILL_DAMAGE)) {
            return amount;
        }
        UndeadProfession profession = profession(player);
        CompoundTag tag = data(player);
        long now = SkillCooldownHelper.now(player);
        float adjusted = amount * UndeadUpgradeManager.outgoingDamageMultiplier(player);
        if (player.getRandom().nextFloat() < UndeadUpgradeManager.criticalChance(player)) {
            adjusted *= 1.5F;
        }

        if (profession == UndeadProfession.WARRIOR) {
            if (tag.getBoolean(WARRIOR_MIGHT)) {
                adjusted *= 1.25F;
            }
            boolean bloodlust = now < tag.getLong(WARRIOR_BLOODLUST_UNTIL);
            if (bloodlust) {
                boolean potential = UndeadUpgradeManager.has(
                        player, com.rzy.dealt_force_skills.shop.UndeadShopEntry.WARRIOR_INNER_POTENTIAL);
                adjusted *= potential ? 2.0F : 1.4F;
            }
            player.heal(adjusted * (bloodlust ? 0.30F : 0.10F));
            if (UndeadUpgradeManager.has(
                    player, com.rzy.dealt_force_skills.shop.UndeadShopEntry.WARRIOR_FROZEN_BLADE)
                    && source.getDirectEntity() == player) {
                applyFrozenBlade(target);
            }
        } else if (profession == UndeadProfession.ROGUE) {
            revealRogueFromOffense(player);
            if (UndeadUpgradeManager.has(
                    player, com.rzy.dealt_force_skills.shop.UndeadShopEntry.ROGUE_SILENT)
                    && isBehindTarget(player, target)) {
                adjusted *= 2.0F;
            }
            if (target.hasEffect(ModEffects.STUN)) {
                adjusted *= 3.0F;
                target.removeEffect(ModEffects.STUN);
            }
        } else if (profession == UndeadProfession.SCHOLAR) {
            int records = scholarRecordCount(player, target);
            if (target instanceof Player) {
                adjusted *= 1.0F + records * 0.15F;
            }
        }
        if (UndeadUpgradeManager.isEquipped(
                player, com.rzy.dealt_force_skills.shop.UndeadShopEntry.OPAL_BRACELET)) {
            applyOpalBurn(target);
        }
        if (profession == UndeadProfession.WARRIOR && tag.getBoolean(WARRIOR_MIGHT)) {
            target.invulnerableTime = 0;
            target.hurt(SkillDamageHelper.trueDamage(player.serverLevel(), player, player), adjusted * 0.5F);
            adjusted *= 0.5F;
        }
        return adjusted;
    }

    public static void applyRupture(LivingEntity target) {
        applyRupture(null, target);
    }

    public static void applyRupture(ServerPlayer owner, LivingEntity target) {
        long now = SkillCooldownHelper.now(target);
        long duration = owner != null && UndeadUpgradeManager.has(
                owner, com.rzy.dealt_force_skills.shop.UndeadShopEntry.SCHOLAR_RETURNED)
                ? 8L * 20L + 1L
                : 4L * 20L + 1L;
        target.getPersistentData().putLong(RUPTURE_UNTIL, now + duration);
        target.getPersistentData().putLong(RUPTURE_NEXT, now + 20L);
    }

    public static void tickExternalEffects(LivingEntity entity) {
        if (entity.level().isClientSide || !entity.isAlive()) {
            return;
        }
        CompoundTag tag = entity.getPersistentData();
        long now = entity.level().getGameTime();
        long ruptureUntil = tag.getLong(RUPTURE_UNTIL);
        if (ruptureUntil > 0L) {
            if (now >= ruptureUntil) {
                tag.remove(RUPTURE_UNTIL);
                tag.remove(RUPTURE_NEXT);
            } else if (now >= tag.getLong(RUPTURE_NEXT)) {
                tag.putLong(RUPTURE_NEXT, now + 20L);
                if (now < tag.getLong(RITUAL_DANCE_UNTIL)) {
                    entity.heal(entity.getMaxHealth() * 0.06F);
                } else {
                    entity.hurt(entity.damageSources().magic(), entity.getMaxHealth() * 0.20F);
                }
            }
        }
        long burnUntil = tag.getLong(OPAL_BURN_UNTIL);
        if (burnUntil > 0L) {
            if (now >= burnUntil) {
                tag.remove(OPAL_BURN_UNTIL);
                tag.remove(OPAL_BURN_NEXT);
            } else if (now >= tag.getLong(OPAL_BURN_NEXT)) {
                tag.putLong(OPAL_BURN_NEXT, now + 20L);
                entity.hurt(entity.damageSources().onFire(), entity.getMaxHealth() * 0.02F);
            }
        }
        tickFrozenBlade(entity, tag, now);
    }

    public static void syncToClient(ServerPlayer player) {
        if (!isUndead(player)) {
            return;
        }
        CompoundTag tag = data(player);
        NetworkHandler.sendToPlayer(new S2C_SyncUndeadState(
                profession(player).ordinal(),
                energy(player),
                UndeadUpgradeManager.maxEnergy(player),
                remainingTicks(player, DISORIENTED_UNTIL),
                coreCooldownRemaining(player, profession(player)),
                remainingTicks(player, KNIGHT_SHIELD_UNTIL),
                Math.max(0.0F, tag.getFloat(KNIGHT_SHIELD)),
                tag.getBoolean(WARRIOR_MIGHT),
                remainingTicks(player, WARRIOR_BLOODLUST_UNTIL),
                remainingTicks(player, EXPLORER_MEDITATING_UNTIL),
                remainingTicks(player, EXPLORER_SPACE_UNTIL),
                UndeadUpgradeManager.has(
                        player, com.rzy.dealt_force_skills.shop.UndeadShopEntry.EXPLORER_SPIRIT),
                UndeadUpgradeManager.has(
                        player, com.rzy.dealt_force_skills.shop.UndeadShopEntry.EXPLORER_THIRST_FOR_KNOWLEDGE),
                remainingTicks(player, ROGUE_INVISIBLE_UNTIL),
                remainingTicks(player, SCHOLAR_RITUAL_UNTIL),
                tag.getBoolean(HUNTER_SCATTER),
                remainingTicks(player, HUNTER_EXHAUSTED_UNTIL)
        ), player);
    }

    private static void tickKnight(ServerPlayer player, CompoundTag tag, long now) {
        if (profession(player) != UndeadProfession.KNIGHT) {
            return;
        }
        if (now >= tag.getLong(KNIGHT_SHIELD_UNTIL)) {
            tag.putFloat(KNIGHT_SHIELD, 0.0F);
        } else if (tag.getFloat(KNIGHT_SHIELD) > 0.0F && player.tickCount % 10 == 0) {
            player.serverLevel().sendParticles(ParticleTypes.ENCHANTED_HIT,
                    player.getX(), player.getY() + 1.0D, player.getZ(),
                    8, 0.55D, 0.8D, 0.55D, 0.02D);
        }
        if (now < tag.getLong(KNIGHT_PARRY_UNTIL)) {
            player.setDeltaMovement(Vec3.ZERO);
        }
        if (now >= tag.getLong(KNIGHT_CHARGE_UNTIL)) {
            return;
        }
        Vec3 look = player.getLookAngle();
        int targetId = tag.getInt(KNIGHT_CHARGE_TARGET);
        Entity chargeTarget = targetId >= 0 ? player.serverLevel().getEntity(targetId) : null;
        if (chargeTarget instanceof LivingEntity living && living.isAlive()) {
            look = living.getBoundingBox().getCenter().subtract(player.position());
        }
        Vec3 horizontal = new Vec3(look.x, 0.0D, look.z);
        if (horizontal.lengthSqr() < 1.0E-5D) {
            horizontal = new Vec3(0.0D, 0.0D, 1.0D);
        }
        player.setDeltaMovement(horizontal.normalize().scale(1.25D).add(0.0D, 0.05D, 0.0D));
        player.hurtMarked = true;
        List<LivingEntity> hits = player.serverLevel().getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(0.65D),
                target -> target != player && TargetingUtil.isTargetableLiving(target)
        );
        if (!hits.isEmpty()) {
            LivingEntity target = hits.get(0);
            target.invulnerableTime = 0;
            float damage = UndeadUpgradeManager.has(
                    player, com.rzy.dealt_force_skills.shop.UndeadShopEntry.KNIGHT_FANATIC_CHARGE)
                    ? 8.4F
                    : 6.0F;
            SkillDamageHelper.hurt(target, player.damageSources().playerAttack(player), player, damage);
            target.addEffect(new MobEffectInstance(ModEffects.STUN, com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.undead.undead_state_manager.effect.stun.3.duration_ticks", 30), com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.undead.undead_state_manager.effect.stun.3.amplifier", 0),
                    false, true, true), player);
            player.serverLevel().sendParticles(ParticleTypes.CRIT,
                    target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(),
                    18, 0.35D, 0.35D, 0.35D, 0.08D);
            tag.putLong(KNIGHT_CHARGE_UNTIL, 0L);
            tag.putInt(KNIGHT_CHARGE_TARGET, -1);
        } else if (player.horizontalCollision) {
            player.serverLevel().sendParticles(ParticleTypes.POOF,
                    player.getX(), player.getY() + 0.8D, player.getZ(),
                    16, 0.45D, 0.55D, 0.45D, 0.08D);
            player.serverLevel().playSound(null, player.blockPosition(),
                    SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.55F, 1.35F);
            tag.putLong(KNIGHT_CHARGE_UNTIL, 0L);
            tag.putInt(KNIGHT_CHARGE_TARGET, -1);
        } else {
            player.serverLevel().sendParticles(ParticleTypes.END_ROD,
                    player.getX(), player.getY() + 0.8D, player.getZ(),
                    2, 0.2D, 0.25D, 0.2D, 0.0D);
        }
    }

    private static void tickWarrior(ServerPlayer player, CompoundTag tag, long now) {
        if (profession(player) != UndeadProfession.WARRIOR) {
            return;
        }
        if (tag.getBoolean(WARRIOR_MIGHT) && !drainContinuousEnergy(player, 5.0F / 20.0F)) {
            tag.putBoolean(WARRIOR_MIGHT, false);
        }
        if (tag.getBoolean(WARRIOR_MIGHT) && player.tickCount % 10 == 0) {
            player.serverLevel().sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                    player.getX(), player.getY() + 0.9D, player.getZ(),
                    4, 0.45D, 0.55D, 0.45D, 0.02D);
        }
        if (now < tag.getLong(WARRIOR_BLOODLUST_UNTIL)
                && UndeadUpgradeManager.has(
                player, com.rzy.dealt_force_skills.shop.UndeadShopEntry.WARRIOR_INNER_POTENTIAL)
                && player.tickCount % 20 == 0) {
            player.hurt(player.damageSources().magic(), player.getMaxHealth() * WARRIOR_BLOODLUST_SELF_HEALTH_COST_FRACTION);
        }
        if (now < tag.getLong(WARRIOR_BLOODLUST_UNTIL) && player.tickCount % 10 == 0) {
            player.serverLevel().sendParticles(ParticleTypes.FLAME,
                    player.getX(), player.getY() + 0.8D, player.getZ(),
                    7, 0.45D, 0.65D, 0.45D, 0.01D);
        }
    }

    private static void tickExplorer(ServerPlayer player, CompoundTag tag, long now) {
        if (profession(player) != UndeadProfession.EXPLORER) {
            return;
        }
        if (now < tag.getLong(EXPLORER_MEDITATING_UNTIL)) {
            player.setDeltaMovement(Vec3.ZERO);
            player.heal(player.getMaxHealth() * 0.05F / 20.0F);
            addEnergy(player, 10.0F / 20.0F);
            if (player.tickCount % 5 == 0) {
                player.serverLevel().sendParticles(ParticleTypes.ENCHANT,
                        player.getX(), player.getY() + 0.9D, player.getZ(),
                        5, 0.45D, 0.55D, 0.45D, 0.01D);
            }
        }
        if (now < tag.getLong(EXPLORER_SPACE_UNTIL) && player.tickCount % 10 == 0) {
            player.serverLevel().sendParticles(ParticleTypes.REVERSE_PORTAL,
                    player.getX(), player.getY() + 0.9D, player.getZ(),
                    8, 0.5D, 0.75D, 0.5D, 0.02D);
        }
    }

    private static void syncExplorerOpportunityWindow(ServerPlayer player, CompoundTag tag, long now) {
        if (now % ManbaStateManager.OPPORTUNITY_WINDOW_SYNC_INTERVAL_TICKS != 0L) {
            return;
        }
        boolean active = profession(player) == UndeadProfession.EXPLORER
                && now < tag.getLong(EXPLORER_SPACE_UNTIL)
                && UndeadUpgradeManager.has(player,
                com.rzy.dealt_force_skills.shop.UndeadShopEntry.EXPLORER_SPIRIT);
        NetworkHandler.sendToPlayer(new S2C_ManbaOpportunityMarkers(active
                ? ManbaStateManager.opportunityMarkers(player, EXPLORER_SPIRIT_OPPORTUNITY_RANGE)
                : List.of()), player);
    }

    private static void tickRogue(ServerPlayer player, CompoundTag tag, long now) {
        if (profession(player) != UndeadProfession.ROGUE) {
            return;
        }
        if (now < tag.getLong(ROGUE_INVISIBLE_UNTIL) && player.tickCount % 10 == 0) {
            player.serverLevel().sendParticles(ParticleTypes.SMOKE,
                    player.getX(), player.getY() + 0.7D, player.getZ(),
                    5, 0.35D, 0.55D, 0.35D, 0.01D);
        }
    }

    private static void tickScholar(ServerPlayer player, CompoundTag tag, long now) {
        if (profession(player) != UndeadProfession.SCHOLAR || now >= tag.getLong(SCHOLAR_RITUAL_UNTIL)) {
            return;
        }
        player.getPersistentData().putLong(RITUAL_DANCE_UNTIL, now + 10L);
        if (player.tickCount % 5 != 0) {
            return;
        }
        player.serverLevel().sendParticles(ParticleTypes.ENCHANT,
                player.getX(), player.getY() + 1.0D, player.getZ(),
                10, 1.4D, 0.7D, 1.4D, 0.02D);
        if (UndeadUpgradeManager.has(
                player, com.rzy.dealt_force_skills.shop.UndeadShopEntry.SCHOLAR_RETURNED)
                && player.tickCount % 20 == 0) {
            player.heal(player.getMaxHealth() * 0.06F);
        }
        AABB area = player.getBoundingBox().inflate(3.0D);
        for (LivingEntity target : player.serverLevel().getEntitiesOfClass(
                LivingEntity.class, area, target -> target != player && TargetingUtil.isTargetableLiving(target))) {
            Vec3 pull = player.position().subtract(target.position());
            if (pull.lengthSqr() > 1.0E-5D) {
                target.setDeltaMovement(target.getDeltaMovement().add(pull.normalize().scale(0.08D)));
                target.hurtMarked = true;
            }
            target.getPersistentData().putLong(RITUAL_DANCE_UNTIL, now + 10L);
            target.addEffect(new MobEffectInstance(ModEffects.RAPTOR_ELECTROMAGNETIC_INTERFERENCE,
                    com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.undead.undead_state_manager.effect.raptor_electromagnetic_interference.5.duration_ticks", 12), com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.undead.undead_state_manager.effect.raptor_electromagnetic_interference.5.amplifier", 0), false, true, true), player);
            if (player.tickCount % 20 == 0) {
                target.invulnerableTime = 0;
                SkillDamageHelper.hurt(target, player.damageSources().magic(), player, com.rzy.dealt_force_skills.config.DealtForceConfig.floatValue("characters.undead.undead_state_manager.skill_hurt.1.damage", 4.0F));
            }
        }
    }

    private static void tickHunter(ServerPlayer player, CompoundTag tag, long now) {
        if (profession(player) != UndeadProfession.HUNTER) {
            clearHunterMovementTracking(tag);
            clearHunterSpeed(player);
            clearHunterScatterSlow(player);
            return;
        }
        applyHunterSpeed(player);
        long exhaustedUntil = tag.getLong(HUNTER_EXHAUSTED_UNTIL);
        if (now < exhaustedUntil) {
            clearHunterMovementTracking(tag);
            return;
        }
        if (exhaustedUntil > 0L) {
            tag.putLong(HUNTER_EXHAUSTED_UNTIL, 0L);
            setEnergy(player, UndeadUpgradeManager.maxEnergy(player));
        }

        Vec3 movement = player.getDeltaMovement();
        Vec3 trackedMovement = trackHunterMovement(player, tag);
        boolean movingVertically = !player.onGround() && Math.abs(trackedMovement.y) > 0.05D;
        boolean movingHorizontally = trackedMovement.x * trackedMovement.x
                + trackedMovement.z * trackedMovement.z > HUNTER_MOVEMENT_THRESHOLD_SQR;
        float drain = movingVertically
                ? HUNTER_VERTICAL_DRAIN_PER_TICK
                : movingHorizontally ? HUNTER_HORIZONTAL_DRAIN_PER_TICK : 0.0F;
        boolean durableFuel = UndeadUpgradeManager.has(
                player, com.rzy.dealt_force_skills.shop.UndeadShopEntry.HUNTER_DURABLE_FUEL);
        if (!durableFuel && drain > 0.0F && !drainContinuousEnergy(player, drain)) {
            exhaustHunter(player, tag, now);
            return;
        }
        if (player.horizontalCollision) {
            player.setDeltaMovement(movement.x,
                    Math.max(HUNTER_WALL_CLIMB_SPEED_PER_TICK, movement.y), movement.z);
            player.hurtMarked = true;
        }

        if (tag.getBoolean(HUNTER_SCATTER)) {
            applyHunterScatterSlow(player);
            rotateHunterScatter(player, tag);
            if (!drainContinuousEnergy(player, 5.0F / 20.0F)) {
                tag.putBoolean(HUNTER_SCATTER, false);
                clearHunterScatterSlow(player);
                return;
            }
            if (player.tickCount % 5 == 0) {
                player.serverLevel().sendParticles(ParticleTypes.CRIT,
                        player.getX(), player.getY() + 1.0D, player.getZ(),
                        12, 1.5D, 0.5D, 1.5D, 0.08D);
                for (LivingEntity target : player.serverLevel().getEntitiesOfClass(
                        LivingEntity.class,
                        player.getBoundingBox().inflate(4.0D),
                        target -> target != player && TargetingUtil.isTargetableLiving(target))) {
                    target.invulnerableTime = 0;
                    float damage = UndeadUpgradeManager.has(
                            player, com.rzy.dealt_force_skills.shop.UndeadShopEntry.HUNTER_HIGH_ENERGY_POWDER)
                            ? 4.0F
                            : 2.0F;
                    SkillDamageHelper.hurt(target, player.damageSources().playerAttack(player), player, damage);
                    DfsAchievements.recordUndeadHunterScatterHit(player, target);
                }
            }
        } else {
            clearHunterScatterSlow(player);
        }
    }

    private static void blastHunterArea(ServerLevel level, ServerPlayer player, Vec3 center) {
        for (LivingEntity target : level.getEntitiesOfClass(
                LivingEntity.class,
                new AABB(center, center).inflate(5.0D),
                target -> target == player || TargetingUtil.isTargetableLiving(target))) {
            Vec3 away = target.position().subtract(center);
            if (away.lengthSqr() < 1.0E-5D) {
                away = new Vec3(0.0D, 0.2D, 1.0D);
            }
            target.setDeltaMovement(target.getDeltaMovement().add(away.normalize().scale(2.2D)).add(0.0D, 0.8D, 0.0D));
            target.hurtMarked = true;
        }
    }

    private static void exhaustHunter(ServerPlayer player, CompoundTag tag, long now) {
        setEnergy(player, 0.0F);
        tag.putBoolean(HUNTER_SCATTER, false);
        tag.putLong(HUNTER_EXHAUSTED_UNTIL, now + 5L * 20L);
        player.addEffect(new MobEffectInstance(ModEffects.STUN, com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.undead.undead_state_manager.effect.stun.6.duration_ticks", 5 * 20), com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.undead.undead_state_manager.effect.stun.6.amplifier", 0),
                false, true, true));
    }

    private static void updateRuntimeInvisibility(ServerPlayer player, long now) {
        CompoundTag tag = data(player);
        boolean invisible = now < tag.getLong(EXPLORER_SPACE_UNTIL)
                || now < tag.getLong(ROGUE_INVISIBLE_UNTIL)
                || UndeadSupportManager.isTombstoneHidden(player);
        if (invisible) {
            tag.putBoolean(RUNTIME_INVISIBLE, true);
            player.setInvisible(true);
            refreshVisualMarker(player, ModEffects.UNDEAD_TRUE_INVISIBILITY);
            clearMobTargets(player);
        } else {
            clearRuntimeInvisibility(player);
        }
    }

    private static void updateHunterScatterVisual(ServerPlayer player, CompoundTag tag) {
        if (profession(player) == UndeadProfession.HUNTER && tag.getBoolean(HUNTER_SCATTER)) {
            refreshVisualMarker(player, ModEffects.UNDEAD_HUNTER_SCATTER);
        } else {
            player.removeEffect(ModEffects.UNDEAD_HUNTER_SCATTER);
        }
    }

    private static void refreshVisualMarker(ServerPlayer player, Holder<MobEffect> effect) {
        MobEffectInstance current = player.getEffect(effect);
        if (current == null || current.getDuration() <= 5) {
            player.addEffect(new MobEffectInstance(effect, com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.undead.undead_state_manager.effect.custom.7.duration_ticks", 10), com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.undead.undead_state_manager.effect.custom.7.amplifier", 0),
                    false, false, false));
        }
    }

    private static void clearRuntimeInvisibility(Player player) {
        CompoundTag tag = data(player);
        if (tag.getBoolean(RUNTIME_INVISIBLE)) {
            tag.putBoolean(RUNTIME_INVISIBLE, false);
            player.setInvisible(false);
        }
        player.removeEffect(ModEffects.UNDEAD_TRUE_INVISIBILITY);
    }

    private static void clearMobTargets(ServerPlayer player) {
        for (Mob mob : player.serverLevel().getEntitiesOfClass(
                Mob.class, player.getBoundingBox().inflate(64.0D), Mob::isAlive)) {
            if (mob.getTarget() == player) {
                mob.setTarget(null);
            }
        }
    }

    private static void revealRogueFromOffense(ServerPlayer player) {
        if (!isUndead(player) || profession(player) != UndeadProfession.ROGUE) {
            return;
        }
        CompoundTag tag = data(player);
        tag.putLong(ROGUE_INVISIBLE_UNTIL, 0L);
        tag.putLong(ROGUE_INVULNERABLE_UNTIL, 0L);
        tag.putLong(ROGUE_SPEED_UNTIL, 0L);
        clearRuntimeInvisibility(player);
    }

    private static float effectiveEnergyCost(Player player, float baseCost) {
        float cost = Math.max(0.0F, baseCost);
        long now = SkillCooldownHelper.now(player);
        CompoundTag tag = data(player);
        if (now < tag.getLong(DISORIENTED_UNTIL)) {
            cost *= 2.0F;
        }
        if (profession(player) == UndeadProfession.WARRIOR
                && now < tag.getLong(WARRIOR_BLOODLUST_UNTIL)) {
            cost *= 0.5F;
        }
        return cost * UndeadUpgradeManager.energyCostMultiplier(player);
    }

    private static boolean drainContinuousEnergy(ServerPlayer player, float baseCost) {
        float cost = effectiveEnergyCost(player, baseCost);
        if (energy(player) + 0.0001F < cost) {
            return false;
        }
        setEnergy(player, energy(player) - cost);
        return true;
    }

    private static void setEnergy(Player player, float value) {
        data(player).putFloat(ENERGY, Math.max(0.0F,
                Math.min(UndeadUpgradeManager.maxEnergy(player), value)));
    }

    public static float frozenInteractionMultiplier(LivingEntity entity) {
        int stacks = dataValue(entity, FROZEN_BLADE_STACKS);
        return Math.max(0.1F, 1.0F - stacks * 0.10F);
    }

    private static void applyOpalBurn(LivingEntity target) {
        long now = SkillCooldownHelper.now(target);
        CompoundTag tag = target.getPersistentData();
        tag.putLong(OPAL_BURN_UNTIL, now + 2L * 20L + 1L);
        tag.putLong(OPAL_BURN_NEXT, now + 20L);
        target.igniteForSeconds(2);
    }

    private static void applyFrozenBlade(LivingEntity target) {
        CompoundTag tag = target.getPersistentData();
        int stacks = Math.min(6, tag.getInt(FROZEN_BLADE_STACKS) + 1);
        tag.putInt(FROZEN_BLADE_STACKS, stacks);
        tag.putLong(FROZEN_BLADE_UNTIL, SkillCooldownHelper.now(target) + 5L * 20L);
    }

    private static void tickFrozenBlade(LivingEntity entity, CompoundTag tag, long now) {
        int stacks = tag.getInt(FROZEN_BLADE_STACKS);
        var attribute = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (stacks <= 0 || now >= tag.getLong(FROZEN_BLADE_UNTIL)) {
            tag.remove(FROZEN_BLADE_STACKS);
            tag.remove(FROZEN_BLADE_UNTIL);
            if (attribute != null) {
                attribute.removeModifier(FROZEN_BLADE_SPEED_UUID);
            }
            return;
        }
        if (attribute == null) {
            return;
        }
        AttributeModifier existing = attribute.getModifier(FROZEN_BLADE_SPEED_UUID);
        double amount = -0.10D * stacks;
        if (existing == null || Math.abs(existing.amount() - amount) > 1.0E-6D) {
            attribute.removeModifier(FROZEN_BLADE_SPEED_UUID);
            attribute.addTransientModifier(new AttributeModifier(FROZEN_BLADE_SPEED_UUID, amount,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
            ));
        }
    }

    private static int dataValue(LivingEntity entity, String key) {
        CompoundTag tag = entity.getPersistentData();
        if (entity.level().getGameTime() >= tag.getLong(FROZEN_BLADE_UNTIL)) {
            return 0;
        }
        return Math.max(0, tag.getInt(key));
    }

    private static boolean isBehindTarget(ServerPlayer attacker, LivingEntity target) {
        Vec3 direction = attacker.position().subtract(target.position());
        if (direction.lengthSqr() < 1.0E-5D) {
            return false;
        }
        return target.getLookAngle().normalize().dot(direction.normalize()) < -0.5D;
    }

    private static boolean isScholarExceptionalDamage(DamageSource source) {
        boolean skillDamage = source.typeHolder().unwrapKey()
                .map(key -> DealtForceSkillsMod.MODID.equals(key.location().getNamespace()))
                .orElse(false);
        return skillDamage
                || source.is(DamageTypeTags.IS_EXPLOSION)
                || source.is(DamageTypeTags.WITCH_RESISTANT_TO)
                || source.is(DamageTypeTags.BYPASSES_EFFECTS)
                || "magic".equals(source.getMsgId())
                || "indirectMagic".equals(source.getMsgId());
    }

    private static void clearProfessionToggles(Player player, CompoundTag tag) {
        tag.putLong(KNIGHT_CHARGE_UNTIL, 0L);
        tag.putLong(KNIGHT_PARRY_UNTIL, 0L);
        tag.putBoolean(WARRIOR_MIGHT, false);
        tag.putLong(EXPLORER_MEDITATING_UNTIL, 0L);
        tag.putLong(EXPLORER_SPACE_UNTIL, 0L);
        tag.putLong(ROGUE_INVISIBLE_UNTIL, 0L);
        tag.putLong(ROGUE_INVULNERABLE_UNTIL, 0L);
        tag.putLong(ROGUE_SPEED_UNTIL, 0L);
        tag.putLong(SCHOLAR_RITUAL_UNTIL, 0L);
        tag.putBoolean(HUNTER_SCATTER, false);
        clearRuntime(player);
    }

    private static void clearTransientState(Player player) {
        CompoundTag tag = data(player);
        clearProfessionToggles(player, tag);
        tag.putLong(HUNTER_EXHAUSTED_UNTIL, 0L);
        clearHunterMovementTracking(tag);
    }

    private static Vec3 trackHunterMovement(Player player, CompoundTag tag) {
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();
        if (!tag.getBoolean(HUNTER_POSITION_TRACKED)) {
            storeHunterPosition(tag, x, y, z);
            return Vec3.ZERO;
        }

        Vec3 movement = new Vec3(
                x - tag.getDouble(HUNTER_LAST_X),
                y - tag.getDouble(HUNTER_LAST_Y),
                z - tag.getDouble(HUNTER_LAST_Z));
        storeHunterPosition(tag, x, y, z);
        return movement.lengthSqr() <= HUNTER_MAX_TRACKED_DISPLACEMENT_SQR ? movement : Vec3.ZERO;
    }

    private static void storeHunterPosition(CompoundTag tag, double x, double y, double z) {
        tag.putBoolean(HUNTER_POSITION_TRACKED, true);
        tag.putDouble(HUNTER_LAST_X, x);
        tag.putDouble(HUNTER_LAST_Y, y);
        tag.putDouble(HUNTER_LAST_Z, z);
    }

    private static void clearHunterMovementTracking(CompoundTag tag) {
        tag.remove(HUNTER_POSITION_TRACKED);
        tag.remove(HUNTER_LAST_X);
        tag.remove(HUNTER_LAST_Y);
        tag.remove(HUNTER_LAST_Z);
    }

    private static void applyHunterSpeed(Player player) {
        var attribute = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attribute != null && attribute.getModifier(HUNTER_SPEED_UUID) == null) {
            attribute.addTransientModifier(HUNTER_SPEED);
        }
    }

    private static void clearHunterSpeed(Player player) {
        var attribute = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attribute != null) {
            attribute.removeModifier(HUNTER_SPEED_UUID);
        }
    }

    private static void applyHunterScatterSlow(Player player) {
        var attribute = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attribute != null && attribute.getModifier(HUNTER_SCATTER_SLOW_UUID) == null) {
            attribute.addTransientModifier(new AttributeModifier(HUNTER_SCATTER_SLOW_UUID, -0.50D,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
            ));
        }
    }

    private static void rotateHunterScatter(ServerPlayer player, CompoundTag tag) {
        float yaw = Mth.wrapDegrees(tag.getFloat(HUNTER_SCATTER_YAW) + 18.0F);
        tag.putFloat(HUNTER_SCATTER_YAW, yaw);
        player.setYRot(yaw);
        player.setYHeadRot(yaw);
        player.setYBodyRot(yaw);
    }

    private static void clearHunterScatterSlow(Player player) {
        var attribute = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attribute != null) {
            attribute.removeModifier(HUNTER_SCATTER_SLOW_UUID);
        }
    }

    private static CompoundTag scholarRecords(Player player) {
        CompoundTag root = data(player);
        if (!root.contains(SCHOLAR_RECORDS, Tag.TAG_COMPOUND)) {
            root.put(SCHOLAR_RECORDS, new CompoundTag());
        }
        return root.getCompound(SCHOLAR_RECORDS);
    }

    private static String coreCooldownKey(UndeadProfession profession) {
        return CORE_COOLDOWN_PREFIX + profession.name();
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
            SkillCooldownHelper.clampDeadline(tag, HUNTER_RELOAD_READY_AT, SkillCooldownHelper.now(player), 2 * 20);
            SkillCooldownHelper.clampDeadline(tag, ROGUE_PASSIVE_READY_AT, SkillCooldownHelper.now(player), 10 * 20);
            SkillCooldownHelper.clampDeadline(tag, coreCooldownKey(UndeadProfession.KNIGHT),
                    SkillCooldownHelper.now(player), Math.max(1, com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.undead.undead_skills.cooldown.0.cooldown_ticks", 1800)));
            SkillCooldownHelper.clampDeadline(tag, coreCooldownKey(UndeadProfession.WARRIOR),
                    SkillCooldownHelper.now(player), Math.max(1, com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.undead.undead_skills.cooldown.1.cooldown_ticks", 1200)));
            SkillCooldownHelper.clampDeadline(tag, coreCooldownKey(UndeadProfession.EXPLORER),
                    SkillCooldownHelper.now(player), Math.max(1, com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.undead.undead_skills.cooldown.2.cooldown_ticks", 1)));
            SkillCooldownHelper.clampDeadline(tag, coreCooldownKey(UndeadProfession.ROGUE),
                    SkillCooldownHelper.now(player), Math.max(1, com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.undead.undead_skills.cooldown.3.cooldown_ticks", 800)));
        }
        return tag;
    }

    static CompoundTag rootData(Player player) {
        return data(player);
    }
}
