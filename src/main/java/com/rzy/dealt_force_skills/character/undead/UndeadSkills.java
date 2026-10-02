package com.rzy.dealt_force_skills.character.undead;

import com.rzy.dealt_force_skills.advancement.DfsAchievements;
import com.rzy.dealt_force_skills.character.SkillSlot;
import com.rzy.dealt_force_skills.registry.ModGameRules;
import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.skill.SkillCooldownHelper;
import com.rzy.dealt_force_skills.skill.SkillDamageHelper;
import com.rzy.dealt_force_skills.skill.SkillAnimationScheduler;
import com.rzy.dealt_force_skills.skill.SkillModelVisual;
import com.rzy.dealt_force_skills.skill.SkillModelVisualSync;
import com.rzy.dealt_force_skills.util.TargetingUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class UndeadSkills {
    public static volatile int LONG_HOLD_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("LONG_HOLD_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.undead.undead_skills.long_hold_ticks", 5));
    private static volatile double KNIGHT_LOCK_RANGE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("KNIGHT_LOCK_RANGE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("characters.undead.undead_skills.knight_lock_range", 12.0));
    private static volatile double WARRIOR_LOCK_RANGE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("WARRIOR_LOCK_RANGE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("characters.undead.undead_skills.warrior_lock_range", 3.0));
    private static volatile double ROGUE_SUBDUE_LOCK_RANGE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("ROGUE_SUBDUE_LOCK_RANGE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("characters.undead.undead_skills.rogue_subdue_lock_range", 4.0));
    private static volatile double ROGUE_CORE_LOCK_RANGE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("ROGUE_CORE_LOCK_RANGE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("characters.undead.undead_skills.rogue_core_lock_range", 2.5));
    private static volatile double SCHOLAR_STITCH_LOCK_RANGE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("SCHOLAR_STITCH_LOCK_RANGE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("characters.undead.undead_skills.scholar_stitch_lock_range", 2.0));
    private static volatile double SCHOLAR_RECORD_LOCK_RANGE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("SCHOLAR_RECORD_LOCK_RANGE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("characters.undead.undead_skills.scholar_record_lock_range", 6.0));
    private static volatile double KNIGHT_NON_PLAYER_LOCK_MULTIPLIER = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("KNIGHT_NON_PLAYER_LOCK_MULTIPLIER", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue(
      "characters.undead.undead_skills.knight_non_player_lock_multiplier", 1.5
   ));
    private static volatile double WARRIOR_NON_PLAYER_LOCK_MULTIPLIER = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("WARRIOR_NON_PLAYER_LOCK_MULTIPLIER", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue(
      "characters.undead.undead_skills.warrior_non_player_lock_multiplier", 2.0
   ));
    private static volatile double ROGUE_NON_PLAYER_LOCK_MULTIPLIER = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("ROGUE_NON_PLAYER_LOCK_MULTIPLIER", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue(
      "characters.undead.undead_skills.rogue_non_player_lock_multiplier", 3.0
   ));
    private static volatile double SCHOLAR_NON_PLAYER_LOCK_MULTIPLIER = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("SCHOLAR_NON_PLAYER_LOCK_MULTIPLIER", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue(
      "characters.undead.undead_skills.scholar_non_player_lock_multiplier", 3.0
   ));
    private static volatile float HUNTER_RELOAD_SELF_HEALTH_COST_FRACTION = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("HUNTER_RELOAD_SELF_HEALTH_COST_FRACTION", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.floatValue(
      "characters.undead.undead_skills.hunter_reload_self_health_cost_fraction", 0.05F
   ));
    private UndeadSkills() {
    }

    public static boolean handleInput(
            ServerPlayer player,
            SkillSlot slot,
            UndeadSkillInputAction action,
            int clientHeldTicks
    ) {
        if (!UndeadStateManager.isUndead(player)
                || slot == SkillSlot.PASSIVE) {
            return false;
        }
        UndeadStateManager.initializeIfNeeded(player);
        if (action == UndeadSkillInputAction.PRESS) {
            UndeadStateManager.beginSkillInput(player, slot);
            return true;
        }
        int heldTicks = Math.max(clientHeldTicks, UndeadStateManager.heldSkillInputTicks(player, slot));
        if (action == UndeadSkillInputAction.HOLD) {
            if (slot == SkillSlot.ACTIVE_2
                    && UndeadStateManager.profession(player) == UndeadProfession.EXPLORER
                    && heldTicks >= LONG_HOLD_TICKS) {
                if (UndeadStateManager.startExplorerMeditation(player)) {
                    particles(player, ParticleTypes.ENCHANT, 20, 0.65D, 0.9D);
                    play(player, SoundEvents.AMETHYST_BLOCK_RESONATE, 0.65F, 0.8F);
                }
            }
            return true;
        }
        if (UndeadStateManager.consumeSkillInputResolved(player, slot)) {
            UndeadStateManager.clearSkillInput(player, slot);
            return true;
        }
        UndeadStateManager.clearSkillInput(player, slot);
        if (slot == SkillSlot.ACTIVE_2
                && UndeadStateManager.profession(player) == UndeadProfession.EXPLORER) {
            if (UndeadStateManager.explorerMeditating(player)) {
                UndeadStateManager.stopExplorerMeditation(player);
                particles(player, ParticleTypes.HAPPY_VILLAGER, 12, 0.5D, 0.7D);
            }
            return true;
        }
        if (UndeadStateManager.isActionLocked(player)) {
            player.displayClientMessage(Component.translatable(
                    "message.dealt_force_skills.undead.action_locked"), true);
            return true;
        }
        return useReleasedSkill(player, slot, heldTicks);
    }

    public static void tickHeldInputs(ServerPlayer player) {
        if (!UndeadStateManager.isUndead(player)
                || UndeadStateManager.profession(player) != UndeadProfession.EXPLORER
                || UndeadStateManager.isActionLocked(player)) {
            return;
        }
        int heldTicks = UndeadStateManager.heldSkillInputTicks(player, SkillSlot.ACTIVE_1);
        int maximumTicks = UndeadUpgradeManager.has(
                player, com.rzy.dealt_force_skills.shop.UndeadShopEntry.EXPLORER_THIRST_FOR_KNOWLEDGE)
                ? 120
                : 60;
        if (heldTicks > 0 && player.tickCount % 5 == 0) {
            SkillModelVisualSync.play(player, SkillModelVisual.UNDEAD_EXPLORER_LANTERN_HOLD, 8);
            renderExplorerGuidance(player, heldTicks, maximumTicks);
        }
        if (heldTicks < maximumTicks) {
            return;
        }
        UndeadStateManager.clearSkillInput(player, SkillSlot.ACTIVE_1);
        UndeadStateManager.markSkillInputResolved(player, SkillSlot.ACTIVE_1);
        useExplorer(player, SkillSlot.ACTIVE_1, maximumTicks);
    }

    public static boolean useSkill(ServerPlayer player, SkillSlot slot, boolean alternate) {
        UndeadStateManager.initializeIfNeeded(player);
        if (UndeadStateManager.isActionLocked(player)) {
            player.displayClientMessage(Component.translatable(
                    "message.dealt_force_skills.undead.action_locked"), true);
            return true;
        }
        return useReleasedSkill(player, slot, alternate ? LONG_HOLD_TICKS : 0);
    }

    private static boolean useReleasedSkill(ServerPlayer player, SkillSlot slot, int heldTicks) {
        return switch (UndeadStateManager.profession(player)) {
            case KNIGHT -> useKnight(player, slot, heldTicks);
            case WARRIOR -> useWarrior(player, slot, heldTicks);
            case EXPLORER -> useExplorer(player, slot, heldTicks);
            case ROGUE -> useRogue(player, slot, heldTicks);
            case SCHOLAR -> useScholar(player, slot, heldTicks);
            case HUNTER -> useHunter(player, slot);
        };
    }

    private static boolean useKnight(ServerPlayer player, SkillSlot slot, int heldTicks) {
        if (slot == SkillSlot.ACTIVE_1) {
            float cost = UndeadUpgradeManager.has(
                    player, com.rzy.dealt_force_skills.shop.UndeadShopEntry.KNIGHT_FANATIC_CHARGE)
                    ? 20.0F * 0.93F
                    : 20.0F;
            Optional<LivingEntity> target = heldTicks >= LONG_HOLD_TICKS
                    ? nearestLockTarget(player, UndeadProfession.KNIGHT, SkillSlot.ACTIVE_1)
                    : Optional.empty();
            if (heldTicks >= LONG_HOLD_TICKS && target.isEmpty()) {
                noTarget(player);
                return true;
            }
            if (!UndeadStateManager.consumeEnergy(player, cost)) {
                return true;
            }
            if (target.isPresent()) {
                UndeadStateManager.startKnightCharge(player, target.get());
            } else {
                UndeadStateManager.startKnightCharge(player);
            }
            SkillModelVisualSync.play(player, SkillModelVisual.UNDEAD_KNIGHT_CHARGE,
                    target.isPresent() ? 33 : 15);
            particles(player, ParticleTypes.END_ROD, 18, 0.55D, 0.4D);
            play(player, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 0.7F);
            return true;
        }
        if (slot == SkillSlot.ACTIVE_2) {
            if (!UndeadStateManager.consumeEnergy(player, com.rzy.dealt_force_skills.config.DealtForceConfig.floatValue("characters.undead.undead_skills.energy.1.energy_cost", 10.0F))) {
                return true;
            }
            SkillModelVisualSync.play(player, SkillModelVisual.UNDEAD_KNIGHT_BARRIER,
                    SkillModelVisual.UNDEAD_KNIGHT_BARRIER.impactTick() + 16);
            SkillAnimationScheduler.schedule(
                    player,
                    SkillModelVisual.UNDEAD_KNIGHT_BARRIER.impactTick(),
                    UndeadStateManager::startKnightParry);
            particles(player, ParticleTypes.ENCHANTED_HIT, 18, 0.55D, 0.8D);
            play(player, SoundEvents.SHIELD_BLOCK, 0.9F, 1.15F);
            return true;
        }
        if (slot == SkillSlot.CORE) {
            if (!coreReady(player) || !UndeadStateManager.consumeEnergy(player, com.rzy.dealt_force_skills.config.DealtForceConfig.floatValue("characters.undead.undead_skills.energy.2.energy_cost", 60.0F))) {
                return true;
            }
            UndeadStateManager.startCoreCooldown(player, UndeadProfession.KNIGHT, com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.undead.undead_skills.cooldown.0.cooldown_ticks", 90 * 20));
            DfsAchievements.recordUndeadProfessionMechanism(player, UndeadProfession.KNIGHT);
            SkillModelVisualSync.play(player, SkillModelVisual.UNDEAD_KNIGHT_WALL,
                    SkillModelVisual.UNDEAD_KNIGHT_WALL.impactTick() + 20 * 20);
            SkillAnimationScheduler.schedule(
                    player,
                    SkillModelVisual.UNDEAD_KNIGHT_WALL.impactTick(),
                    UndeadStateManager::startKnightShield);
            particles(player, ParticleTypes.TOTEM_OF_UNDYING, 35, 0.8D, 1.0D);
            play(player, SoundEvents.BEACON_ACTIVATE, 0.9F, 0.75F);
            return true;
        }
        return false;
    }

    private static boolean useWarrior(ServerPlayer player, SkillSlot slot, int heldTicks) {
        if (slot == SkillSlot.ACTIVE_1) {
            Optional<LivingEntity> lockedTarget = heldTicks >= LONG_HOLD_TICKS
                    ? nearestLockTarget(player, UndeadProfession.WARRIOR, SkillSlot.ACTIVE_1)
                    : Optional.empty();
            if (heldTicks >= LONG_HOLD_TICKS && lockedTarget.isEmpty()) {
                noTarget(player);
                return true;
            }
            if (!UndeadStateManager.consumeEnergy(player, com.rzy.dealt_force_skills.config.DealtForceConfig.floatValue("characters.undead.undead_skills.energy.3.energy_cost", 30.0F))) {
                return true;
            }
            LivingEntity locked = lockedTarget.orElse(null);
            SkillModelVisualSync.play(player, SkillModelVisual.UNDEAD_WARRIOR_AXE);
            SkillAnimationScheduler.schedule(
                    player,
                    SkillModelVisual.UNDEAD_WARRIOR_AXE.impactTick(),
                    delayedPlayer -> {
                        renderSlashArc(delayedPlayer, 3.0D, 110.0D);
                        if (locked != null && locked.isAlive() && locked.level() == delayedPlayer.level()) {
                            locked.invulnerableTime = 0;
                            SkillDamageHelper.hurt(locked, delayedPlayer.damageSources().playerAttack(delayedPlayer),
                                    delayedPlayer, com.rzy.dealt_force_skills.config.DealtForceConfig.floatValue("characters.undead.undead_skills.skill_hurt.0.damage", 15.0F));
                            particlesAt(delayedPlayer, locked, ParticleTypes.CRIT, 24, 0.45D);
                        } else {
                            for (LivingEntity target : targetsInCone(delayedPlayer, 3.0D, 110.0D)) {
                                target.invulnerableTime = 0;
                                SkillDamageHelper.hurt(target,
                                        delayedPlayer.damageSources().playerAttack(delayedPlayer),
                                        delayedPlayer, com.rzy.dealt_force_skills.config.DealtForceConfig.floatValue("characters.undead.undead_skills.skill_hurt.1.damage", 9.0F));
                                particlesAt(delayedPlayer, target, ParticleTypes.SWEEP_ATTACK, 3, 0.2D);
                            }
                        }
                        play(delayedPlayer, SoundEvents.PLAYER_ATTACK_STRONG, 1.0F, 0.75F);
                    });
            return true;
        }
        if (slot == SkillSlot.ACTIVE_2) {
            UndeadStateManager.toggleWarriorMight(player);
            particles(player, ParticleTypes.ANGRY_VILLAGER, 10, 0.5D, 0.8D);
            player.displayClientMessage(Component.translatable(
                    UndeadStateManager.warriorMightActive(player)
                            ? "message.dealt_force_skills.undead.toggle_on"
                            : "message.dealt_force_skills.undead.toggle_off"), true);
            return true;
        }
        if (slot == SkillSlot.CORE) {
            if (!coreReady(player) || !UndeadStateManager.consumeEnergy(player, com.rzy.dealt_force_skills.config.DealtForceConfig.floatValue("characters.undead.undead_skills.energy.4.energy_cost", 50.0F))) {
                return true;
            }
            UndeadStateManager.startCoreCooldown(player, UndeadProfession.WARRIOR, com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.undead.undead_skills.cooldown.1.cooldown_ticks", 60 * 20));
            UndeadStateManager.startWarriorBloodlust(player);
            DfsAchievements.recordUndeadProfessionMechanism(player, UndeadProfession.WARRIOR);
            particles(player, ParticleTypes.FLAME, 36, 0.8D, 1.0D);
            play(player, SoundEvents.RAVAGER_ROAR, 0.8F, 1.25F);
            return true;
        }
        return false;
    }

    private static boolean useExplorer(ServerPlayer player, SkillSlot slot, int heldTicks) {
        if (slot == SkillSlot.ACTIVE_1) {
            if (!UndeadStateManager.consumeEnergy(player, com.rzy.dealt_force_skills.config.DealtForceConfig.floatValue("characters.undead.undead_skills.energy.5.energy_cost", 20.0F))) {
                return true;
            }
            HitResult hit = player.pick(20.0D, 0.0F, false);
            Vec3 center = hit.getType() == HitResult.Type.MISS
                    ? player.getEyePosition().add(player.getLookAngle().scale(20.0D))
                    : hit.getLocation();
            boolean knowledge = UndeadUpgradeManager.has(
                    player, com.rzy.dealt_force_skills.shop.UndeadShopEntry.EXPLORER_THIRST_FOR_KNOWLEDGE);
            int maximumTicks = knowledge ? 120 : 60;
            int halfSeconds = Math.max(0, Math.min(maximumTicks, heldTicks)) / 10;
            double radius = 2.0D + halfSeconds * (knowledge ? 1.0D : 0.5D);
            float damage = knowledge ? 4.0F : 1.0F;
            SkillModelVisualSync.play(player, SkillModelVisual.UNDEAD_EXPLORER_LANTERN);
            SkillAnimationScheduler.schedule(
                    player,
                    SkillModelVisual.UNDEAD_EXPLORER_LANTERN.impactTick(),
                    delayedPlayer -> {
                        AABB area = new AABB(center, center).inflate(radius);
                        for (LivingEntity target : delayedPlayer.serverLevel().getEntitiesOfClass(
                                LivingEntity.class, area,
                                target -> target != delayedPlayer && TargetingUtil.isTargetableLiving(target))) {
                            target.invulnerableTime = 0;
                            SkillDamageHelper.hurt(target, delayedPlayer.damageSources().magic(), delayedPlayer, damage);
                            target.addEffect(new MobEffectInstance(
                                    ModEffects.RAPTOR_ELECTROMAGNETIC_INTERFERENCE,
                                    com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.undead.undead_skills.effect.raptor_electromagnetic_interference.0.duration_ticks", 8 * 20), com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.undead.undead_skills.effect.raptor_electromagnetic_interference.0.amplifier", 0), false, true, true), delayedPlayer);
                            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.undead.undead_skills.effect.glowing.1.duration_ticks", 8 * 20), com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.undead.undead_skills.effect.glowing.1.amplifier", 0),
                                    false, false, true), delayedPlayer);
                        }
                        delayedPlayer.serverLevel().sendParticles(ParticleTypes.ELECTRIC_SPARK,
                                center.x, center.y, center.z, Math.max(18, (int) (radius * 10.0D)),
                                radius * 0.45D, radius * 0.25D, radius * 0.45D, 0.02D);
                        renderHorizontalRing(delayedPlayer, center, radius, ParticleTypes.ELECTRIC_SPARK, 28);
                        play(delayedPlayer, SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 0.85F);
                    });
            return true;
        }
        if (slot == SkillSlot.ACTIVE_2) {
            holdRequired(player);
            return true;
        }
        if (slot == SkillSlot.CORE) {
            if (UndeadStateManager.explorerSpaceActive(player)) {
                UndeadStateManager.stopExplorerSpace(player);
                particles(player, ParticleTypes.REVERSE_PORTAL, 30, 0.7D, 1.0D);
                play(player, SoundEvents.PORTAL_TRIGGER, 0.45F, 1.6F);
                return true;
            }
            if (!coreReady(player) || !UndeadStateManager.consumeEnergy(player, com.rzy.dealt_force_skills.config.DealtForceConfig.floatValue("characters.undead.undead_skills.energy.6.energy_cost", 100.0F))) {
                return true;
            }
            UndeadStateManager.startCoreCooldown(player, UndeadProfession.EXPLORER, com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.undead.undead_skills.cooldown.2.cooldown_ticks", 1));
            UndeadStateManager.startExplorerSpace(player);
            DfsAchievements.recordUndeadProfessionMechanism(player, UndeadProfession.EXPLORER);
            particles(player, ParticleTypes.PORTAL, 45, 0.8D, 1.0D);
            play(player, SoundEvents.PORTAL_TRAVEL, 0.45F, 1.5F);
            return true;
        }
        return false;
    }

    private static boolean useRogue(ServerPlayer player, SkillSlot slot, int heldTicks) {
        if (slot == SkillSlot.ACTIVE_1) {
            if (!UndeadStateManager.consumeEnergy(player, com.rzy.dealt_force_skills.config.DealtForceConfig.floatValue("characters.undead.undead_skills.energy.7.energy_cost", 15.0F))) {
                return true;
            }
            UndeadStateManager.startRogueStealth(player);
            particles(player, ParticleTypes.POOF, 28, 0.7D, 1.0D);
            play(player, SoundEvents.ENDERMAN_TELEPORT, 0.6F, 1.6F);
            return true;
        }
        if (slot == SkillSlot.ACTIVE_2) {
            if (heldTicks < LONG_HOLD_TICKS) {
                holdRequired(player);
                return true;
            }
            Optional<LivingEntity> target = nearestLockTarget(player, UndeadProfession.ROGUE, SkillSlot.ACTIVE_2);
            if (target.isEmpty()) {
                noTarget(player);
                return true;
            }
            if (!UndeadStateManager.consumeEnergy(player, com.rzy.dealt_force_skills.config.DealtForceConfig.floatValue("characters.undead.undead_skills.energy.8.energy_cost", 30.0F))) {
                return true;
            }
            LivingEntity victim = target.get();
            SkillModelVisualSync.play(player, SkillModelVisual.UNDEAD_ROGUE_BATON);
            SkillAnimationScheduler.schedule(
                    player,
                    SkillModelVisual.UNDEAD_ROGUE_BATON.impactTick(),
                    delayedPlayer -> {
                        if (!victim.isAlive() || victim.level() != delayedPlayer.level()) {
                            return;
                        }
                        victim.invulnerableTime = 0;
                        SkillDamageHelper.hurt(victim, delayedPlayer.damageSources().playerAttack(delayedPlayer),
                                delayedPlayer, com.rzy.dealt_force_skills.config.DealtForceConfig.floatValue("characters.undead.undead_skills.skill_hurt.3.damage", 1.0F));
                        victim.addEffect(new MobEffectInstance(ModEffects.STUN, com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.undead.undead_skills.effect.stun.2.duration_ticks", 5 * 20), com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.undead.undead_skills.effect.stun.2.amplifier", 0),
                                false, true, true), delayedPlayer);
                        particlesAt(delayedPlayer, victim, ParticleTypes.ELECTRIC_SPARK, 28, 0.45D);
                    });
            return true;
        }
        if (slot == SkillSlot.CORE) {
            if (heldTicks < LONG_HOLD_TICKS) {
                holdRequired(player);
                return true;
            }
            Optional<LivingEntity> target = nearestLockTarget(player, UndeadProfession.ROGUE, SkillSlot.CORE);
            if (target.isEmpty()) {
                player.displayClientMessage(Component.translatable(
                        "message.dealt_force_skills.undead.no_target"), true);
                return true;
            }
            LivingEntity victim = target.get();
            if (!UndeadStateManager.isRogueInvisible(player)
                    && !victim.hasEffect(ModEffects.STUN)) {
                player.displayClientMessage(Component.translatable(
                        "message.dealt_force_skills.undead.rogue_core_condition"), true);
                return true;
            }
            if (!coreReady(player) || !UndeadStateManager.consumeEnergy(player, com.rzy.dealt_force_skills.config.DealtForceConfig.floatValue("characters.undead.undead_skills.energy.9.energy_cost", 50.0F))) {
                return true;
            }
            UndeadStateManager.startCoreCooldown(player, UndeadProfession.ROGUE, com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.undead.undead_skills.cooldown.3.cooldown_ticks", 40 * 20));
            DfsAchievements.recordUndeadProfessionMechanism(player, UndeadProfession.ROGUE);
            SkillModelVisualSync.play(player, SkillModelVisual.UNDEAD_ROGUE_BATON);
            SkillAnimationScheduler.schedule(
                    player,
                    SkillModelVisual.UNDEAD_ROGUE_BATON.impactTick(),
                    delayedPlayer -> {
                        if (!victim.isAlive() || victim.level() != delayedPlayer.level()) {
                            return;
                        }
                        if (victim instanceof Player victimPlayer) {
                            SkillDamageHelper.hurt(victim,
                                    delayedPlayer.damageSources().playerAttack(delayedPlayer),
                                    delayedPlayer, com.rzy.dealt_force_skills.config.DealtForceConfig.floatValue("characters.undead.undead_skills.skill_hurt.4.damage", 10.0F));
                            stealEquipment(delayedPlayer, victimPlayer);
                        } else if (!com.rzy.dealt_force_skills.boss.BossCombatRules.canInstantKill(victim)) {
                            delayedPlayer.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                                    "message.dealt_force_skills.beacon_boss.immune_execute"), true);
                        } else {
                            UndeadStateManager.executeRogueNonPlayer(delayedPlayer, victim);
                        }
                        particlesAt(delayedPlayer, victim, ParticleTypes.TOTEM_OF_UNDYING, 30, 0.5D);
                    });
            return true;
        }
        return false;
    }

    private static boolean useScholar(ServerPlayer player, SkillSlot slot, int heldTicks) {
        if (slot == SkillSlot.ACTIVE_1) {
            Optional<LivingEntity> selected = heldTicks >= LONG_HOLD_TICKS
                    ? nearestLockTarget(player, UndeadProfession.SCHOLAR, SkillSlot.ACTIVE_1)
                    : Optional.of(player);
            if (selected.isEmpty()) {
                noTarget(player);
                return true;
            }
            if (!UndeadStateManager.consumeEnergy(player, com.rzy.dealt_force_skills.config.DealtForceConfig.floatValue("characters.undead.undead_skills.energy.10.energy_cost", 20.0F))) {
                return true;
            }
            LivingEntity target = selected.get();
            int ruptureVisualTicks = UndeadUpgradeManager.has(
                    player, com.rzy.dealt_force_skills.shop.UndeadShopEntry.SCHOLAR_RETURNED)
                    ? 8 * 20 + 1
                    : 4 * 20 + 1;
            SkillModelVisualSync.play(player, SkillModelVisual.UNDEAD_SCHOLAR_STITCH);
            SkillAnimationScheduler.schedule(
                    player,
                    SkillModelVisual.UNDEAD_SCHOLAR_FIBERS.impactTick(),
                    delayedPlayer -> {
                        if (target.isAlive()
                                && target.level() == delayedPlayer.level()) {
                            SkillModelVisualSync.play(target, SkillModelVisual.UNDEAD_SCHOLAR_FIBERS,
                                    ruptureVisualTicks);
                        }
                    });
            SkillAnimationScheduler.schedule(
                    player,
                    SkillModelVisual.UNDEAD_SCHOLAR_STITCH.impactTick(),
                    delayedPlayer -> {
                        if (!target.isAlive() || target.level() != delayedPlayer.level()) {
                            return;
                        }
                        target.heal(target.getMaxHealth() * 0.75F);
                        boolean selfProtected = target == delayedPlayer && UndeadUpgradeManager.has(
                                delayedPlayer,
                                com.rzy.dealt_force_skills.shop.UndeadShopEntry.SCHOLAR_RETURNED);
                        if (!selfProtected && delayedPlayer.getRandom().nextFloat() < 0.99F) {
                            UndeadStateManager.applyRupture(delayedPlayer, target);
                        }
                        particlesAt(delayedPlayer, target, ParticleTypes.HEART, 20, 0.45D);
                        play(delayedPlayer, SoundEvents.ZOMBIE_VILLAGER_CURE, 0.55F, 1.35F);
                    });
            return true;
        }
        if (slot == SkillSlot.ACTIVE_2) {
            if (!UndeadStateManager.consumeEnergy(player, com.rzy.dealt_force_skills.config.DealtForceConfig.floatValue("characters.undead.undead_skills.energy.11.energy_cost", 20.0F))) {
                return true;
            }
            int ritualTicks = UndeadUpgradeManager.has(
                    player, com.rzy.dealt_force_skills.shop.UndeadShopEntry.SCHOLAR_RETURNED) ? 100 : 50;
            SkillModelVisualSync.play(player, SkillModelVisual.UNDEAD_SCHOLAR_RITUAL,
                    SkillModelVisual.UNDEAD_SCHOLAR_RITUAL.impactTick() + ritualTicks);
            SkillAnimationScheduler.schedule(
                    player,
                    SkillModelVisual.UNDEAD_SCHOLAR_RITUAL.impactTick(),
                    UndeadStateManager::startScholarRitual);
            particles(player, ParticleTypes.ENCHANT, 32, 1.1D, 1.0D);
            play(player, SoundEvents.ENCHANTMENT_TABLE_USE, 0.8F, 0.75F);
            return true;
        }
        if (slot == SkillSlot.CORE) {
            Optional<LivingEntity> target = nearestLockTarget(player, UndeadProfession.SCHOLAR, SkillSlot.CORE);
            if (target.isEmpty()) {
                player.displayClientMessage(Component.translatable(
                        "message.dealt_force_skills.undead.no_target"), true);
                return true;
            }
            float cost = UndeadUpgradeManager.has(
                    player, com.rzy.dealt_force_skills.shop.UndeadShopEntry.SCHOLAR_ANCIENT_SCROLL)
                    ? 37.5F
                    : 75.0F;
            if (!UndeadStateManager.consumeEnergy(player, cost)) {
                return true;
            }
            boolean recorded = UndeadStateManager.recordScholarTarget(player, target.get());
            int targetRecords = UndeadStateManager.scholarRecordCount(player, target.get());
            int recordedTypes = UndeadStateManager.scholarRecordedTypes(player);
            int typeLimit = UndeadStateManager.scholarRecordTypeLimit(player);
            if (recorded) {
                DfsAchievements.recordUndeadProfessionMechanism(player, UndeadProfession.SCHOLAR);
            }
            player.displayClientMessage(Component.translatable(recorded
                            ? "message.dealt_force_skills.undead.recorded"
                            : "message.dealt_force_skills.undead.record_limit",
                    targetRecords, recordedTypes, typeLimit), true);
            particlesAt(player, target.get(), ParticleTypes.ENCHANT, 24, 0.5D);
            return true;
        }
        return false;
    }

    private static boolean useHunter(ServerPlayer player, SkillSlot slot) {
        if (slot == SkillSlot.ACTIVE_1) {
            if (!UndeadStateManager.consumeEnergy(player, com.rzy.dealt_force_skills.config.DealtForceConfig.floatValue("characters.undead.undead_skills.energy.13.energy_cost", 20.0F))) {
                return true;
            }
            UndeadStateManager.scheduleHunterBlast(player);
            play(player, SoundEvents.TNT_PRIMED, 0.8F, 1.35F);
            return true;
        }
        if (slot == SkillSlot.ACTIVE_2) {
            if (!UndeadStateManager.hunterReloadReady(player)) {
                SkillCooldownHelper.notifyCooldown(player, Component.translatable(
                        "message.dealt_force_skills.undead.reload_cooldown"));
                return true;
            }
            player.hurt(player.damageSources().magic(), player.getMaxHealth() * HUNTER_RELOAD_SELF_HEALTH_COST_FRACTION);
            UndeadStateManager.useHunterReload(player);
            particles(player, ParticleTypes.HAPPY_VILLAGER, 18, 0.55D, 0.8D);
            return true;
        }
        if (slot == SkillSlot.CORE) {
            UndeadStateManager.toggleHunterScatter(player);
            if (UndeadStateManager.hunterScatterActive(player)) {
                DfsAchievements.recordUndeadProfessionMechanism(player, UndeadProfession.HUNTER);
            }
            particles(player, ParticleTypes.CRIT, 24, 0.8D, 1.0D);
            player.displayClientMessage(Component.translatable(
                    UndeadStateManager.hunterScatterActive(player)
                            ? "message.dealt_force_skills.undead.toggle_on"
                            : "message.dealt_force_skills.undead.toggle_off"), true);
            return true;
        }
        return false;
    }

    private static boolean coreReady(ServerPlayer player) {
        if (UndeadStateManager.coreCooldownRemaining(player, UndeadStateManager.profession(player)) > 0) {
            SkillCooldownHelper.notifyCooldown(player, Component.translatable(
                    "message.dealt_force_skills.undead.core_cooldown"));
            return false;
        }
        return true;
    }

    private static List<LivingEntity> targetsInCone(ServerPlayer player, double range, double angleDegrees) {
        Vec3 look = player.getLookAngle().normalize();
        double minimumDot = Math.cos(Math.toRadians(angleDegrees * 0.5D));
        return player.serverLevel().getEntitiesOfClass(
                        LivingEntity.class,
                        player.getBoundingBox().inflate(range),
                        target -> target != player && TargetingUtil.isTargetableLiving(target))
                .stream()
                .filter(target -> {
                    Vec3 direction = target.getBoundingBox().getCenter().subtract(player.getEyePosition());
                    return direction.lengthSqr() <= range * range
                            && direction.normalize().dot(look) >= minimumDot;
                })
                .toList();
    }

    private static Optional<LivingEntity> nearestLockTarget(
            ServerPlayer player,
            UndeadProfession profession,
            SkillSlot slot
    ) {
        double maxRange = maxLockRange(profession, slot);
        double angleDegrees = lockAngleDegrees(profession, slot);
        if (maxRange <= 0.0D || angleDegrees <= 0.0D) {
            return Optional.empty();
        }
        Vec3 look = player.getLookAngle().normalize();
        double minimumDot = Math.cos(Math.toRadians(angleDegrees));
        return player.serverLevel().getEntitiesOfClass(
                        LivingEntity.class,
                        player.getBoundingBox().inflate(maxRange),
                        target -> target != player && TargetingUtil.isTargetableLiving(target))
                .stream()
                .filter(target -> {
                    double range = lockRangeForTarget(profession, slot, target);
                    Vec3 direction = target.getBoundingBox().getCenter().subtract(player.getEyePosition());
                    return direction.lengthSqr() <= range * range
                            && direction.normalize().dot(look) >= minimumDot;
                })
                .min(Comparator.comparingDouble(target -> target.distanceToSqr(player)));
    }

    public static boolean isLockSkill(UndeadProfession profession, SkillSlot slot) {
        return baseLockRange(profession, slot) > 0.0D;
    }

    public static double maxLockRange(UndeadProfession profession, SkillSlot slot) {
        double base = baseLockRange(profession, slot);
        if (base <= 0.0D) {
            return 0.0D;
        }
        return base * nonPlayerLockMultiplier(profession);
    }

    public static double lockRangeForTarget(UndeadProfession profession, SkillSlot slot, LivingEntity target) {
        double base = baseLockRange(profession, slot);
        if (base <= 0.0D) {
            return 0.0D;
        }
        return target instanceof Player ? base : base * nonPlayerLockMultiplier(profession);
    }

    public static double lockAngleDegrees(UndeadProfession profession, SkillSlot slot) {
        if (profession == UndeadProfession.ROGUE && slot == SkillSlot.CORE) {
            return 40.0D;
        }
        if (profession == UndeadProfession.SCHOLAR && slot == SkillSlot.ACTIVE_1) {
            return 45.0D;
        }
        return isLockSkill(profession, slot) ? 35.0D : 0.0D;
    }

    private static double baseLockRange(UndeadProfession profession, SkillSlot slot) {
        if (profession == UndeadProfession.KNIGHT && slot == SkillSlot.ACTIVE_1) {
            return KNIGHT_LOCK_RANGE;
        }
        if (profession == UndeadProfession.WARRIOR && slot == SkillSlot.ACTIVE_1) {
            return WARRIOR_LOCK_RANGE;
        }
        if (profession == UndeadProfession.ROGUE && slot == SkillSlot.ACTIVE_2) {
            return ROGUE_SUBDUE_LOCK_RANGE;
        }
        if (profession == UndeadProfession.ROGUE && slot == SkillSlot.CORE) {
            return ROGUE_CORE_LOCK_RANGE;
        }
        if (profession == UndeadProfession.SCHOLAR && slot == SkillSlot.ACTIVE_1) {
            return SCHOLAR_STITCH_LOCK_RANGE;
        }
        if (profession == UndeadProfession.SCHOLAR && slot == SkillSlot.CORE) {
            return SCHOLAR_RECORD_LOCK_RANGE;
        }
        return 0.0D;
    }

    private static double nonPlayerLockMultiplier(UndeadProfession profession) {
        return switch (profession) {
            case KNIGHT -> KNIGHT_NON_PLAYER_LOCK_MULTIPLIER;
            case WARRIOR -> WARRIOR_NON_PLAYER_LOCK_MULTIPLIER;
            case ROGUE -> ROGUE_NON_PLAYER_LOCK_MULTIPLIER;
            case SCHOLAR -> SCHOLAR_NON_PLAYER_LOCK_MULTIPLIER;
            default -> 1.0D;
        };
    }

    private static void renderSlashArc(ServerPlayer player, double range, double angleDegrees) {
        Vec3 forward = new Vec3(player.getLookAngle().x, 0.0D, player.getLookAngle().z);
        if (forward.lengthSqr() < 1.0E-5D) {
            forward = new Vec3(0.0D, 0.0D, 1.0D);
        }
        forward = forward.normalize();
        double baseAngle = Math.atan2(forward.z, forward.x);
        double halfAngle = Math.toRadians(angleDegrees * 0.5D);
        Vec3 origin = player.position().add(0.0D, player.getBbHeight() * 0.58D, 0.0D);
        int points = 13;
        for (int index = 0; index < points; index++) {
            double progress = index / (double) (points - 1);
            double angle = baseAngle - halfAngle + angleDegrees * Math.PI / 180.0D * progress;
            double distance = range * (0.70D + 0.30D * Math.sin(Math.PI * progress));
            Vec3 point = origin.add(Math.cos(angle) * distance, 0.12D * Math.sin(Math.PI * progress),
                    Math.sin(angle) * distance);
            player.serverLevel().sendParticles(ParticleTypes.SWEEP_ATTACK,
                    point.x, point.y, point.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    private static void renderExplorerGuidance(ServerPlayer player, int heldTicks, int maximumTicks) {
        HitResult hit = player.pick(20.0D, 0.0F, false);
        Vec3 center = hit.getType() == HitResult.Type.MISS
                ? player.getEyePosition().add(player.getLookAngle().scale(20.0D))
                : hit.getLocation();
        boolean knowledge = maximumTicks > 60;
        int halfSeconds = Math.max(0, Math.min(maximumTicks, heldTicks)) / 10;
        double radius = 2.0D + halfSeconds * (knowledge ? 1.0D : 0.5D);
        renderHorizontalRing(player, center, radius, ParticleTypes.END_ROD, 20);

        Vec3 start = player.getEyePosition().add(player.getLookAngle().scale(0.8D));
        Vec3 step = center.subtract(start).scale(1.0D / 7.0D);
        for (int index = 1; index <= 7; index++) {
            Vec3 point = start.add(step.scale(index));
            player.serverLevel().sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    point.x, point.y, point.z, 1, 0.03D, 0.03D, 0.03D, 0.0D);
        }
    }

    private static void renderHorizontalRing(
            ServerPlayer player,
            Vec3 center,
            double radius,
            net.minecraft.core.particles.ParticleOptions particle,
            int points
    ) {
        for (int index = 0; index < points; index++) {
            double angle = Math.PI * 2.0D * index / points;
            player.serverLevel().sendParticles(particle,
                    center.x + Math.cos(angle) * radius,
                    center.y + 0.12D,
                    center.z + Math.sin(angle) * radius,
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    private static void play(ServerPlayer player, net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
        player.level().playSound(null, player.blockPosition(), sound, SoundSource.PLAYERS, volume, pitch);
    }

    private static void noTarget(ServerPlayer player) {
        player.displayClientMessage(Component.translatable(
                "message.dealt_force_skills.undead.no_target"), true);
    }

    private static void holdRequired(ServerPlayer player) {
        player.displayClientMessage(Component.translatable(
                "message.dealt_force_skills.undead.hold_required"), true);
    }

    private static void particles(
            ServerPlayer player,
            net.minecraft.core.particles.ParticleOptions particle,
            int count,
            double spread,
            double verticalSpread
    ) {
        player.serverLevel().sendParticles(particle,
                player.getX(), player.getY() + player.getBbHeight() * 0.5D, player.getZ(),
                count, spread, verticalSpread, spread, 0.03D);
    }

    private static void particlesAt(
            ServerPlayer player,
            LivingEntity target,
            net.minecraft.core.particles.ParticleOptions particle,
            int count,
            double spread
    ) {
        player.serverLevel().sendParticles(particle,
                target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(),
                count, spread, spread, spread, 0.04D);
    }

    private static void stealEquipment(ServerPlayer thief, Player victim) {
        boolean directToThief = thief.serverLevel().getGameRules()
                .getBoolean(ModGameRules.DEALT_UNDEAD_STEAL);
        EquipmentSlot[] slots = {
                EquipmentSlot.HEAD,
                EquipmentSlot.CHEST,
                EquipmentSlot.LEGS,
                EquipmentSlot.FEET,
                EquipmentSlot.MAINHAND,
                EquipmentSlot.OFFHAND
        };
        for (EquipmentSlot slot : slots) {
            ItemStack stack = victim.getItemBySlot(slot);
            if (stack.isEmpty() || thief.getRandom().nextFloat() >= 0.50F) {
                continue;
            }
            ItemStack stolen = stack.copy();
            victim.setItemSlot(slot, ItemStack.EMPTY);
            DfsAchievements.recordUndeadRogueTheft(thief, victim instanceof ServerPlayer player ? player : null);
            if (directToThief) {
                if (!thief.getInventory().add(stolen)) {
                    thief.drop(stolen, false);
                }
            } else {
                victim.level().addFreshEntity(new ItemEntity(
                        victim.level(), victim.getX(), victim.getY(), victim.getZ(), stolen));
            }
        }
    }
}
