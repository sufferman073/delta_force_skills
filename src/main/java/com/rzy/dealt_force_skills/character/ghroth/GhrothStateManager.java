package com.rzy.dealt_force_skills.character.ghroth;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.advancement.DfsAchievements;
import com.rzy.dealt_force_skills.character.CharacterSelectionManager;
import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.stamina.StaminaManager;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.S2C_SyncGhrothState;
import com.rzy.dealt_force_skills.network.S2C_TempestStartRoll;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.skill.SkillCooldownHelper;
import com.rzy.dealt_force_skills.skill.SkillDamageHelper;
import com.rzy.dealt_force_skills.util.TargetingUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.registries.BuiltInRegistries;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class GhrothStateManager {
    private static final String ROOT_TAG = DealtForceSkillsMod.MODID + ".ghroth";
    private static final String INITIALIZED = "Initialized";
    private static final String STARS_COOLDOWN_UNTIL = "StarsCooldownUntil";
    private static final String STARS_ACTIVE_UNTIL = "StarsActiveUntil";
    private static final String JUSTICE_COOLDOWN_UNTIL = "JusticeCooldownUntil";
    private static final String JUSTICE_ACTIVE_UNTIL = "JusticeActiveUntil";
    private static final String NOON_COOLDOWN_UNTIL = "NoonCooldownUntil";
    private static final String NOON_ACTIVE_UNTIL = "NoonActiveUntil";
    private static final String NOON_GAZE_TARGET_ID = "NoonGazeTargetId";
    private static final String NOON_GAZE_TICKS = "NoonGazeTicks";
    private static final String NOON_GRANTED_SECONDS = "NoonGrantedSeconds";
    private static final String NOON_DAMAGE_COPIES = "NoonDamageCopies";
    private static final String NOON_LAST_COPY_TICK = "NoonLastCopyTick";
    private static final String TACTICAL_COOLDOWN_UNTIL = "TacticalCooldownUntil";
    private static final String TACTICAL_IMMUNE_UNTIL = "TacticalImmuneUntil";
    private static final String TACTICAL_BOOST_TICKS = "TacticalBoostTicks";
    private static final String TACTICAL_DIR_X = "TacticalDirX";
    private static final String TACTICAL_DIR_Z = "TacticalDirZ";
    private static final String TACTICAL_MODE = "TacticalMode";
    private static final String TACTICAL_MODE_ROLL = "roll";
    private static final String TACTICAL_MODE_LUNGE = "lunge";
    private static final String JUSTICE_MOB_STACKS = "JusticeMobStacks";
    private static final String CEASEFIRE = "Ceasefire";

    private static volatile int STARS_COOLDOWN_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("STARS_COOLDOWN_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.ghroth.ghroth_state_manager.stars_cooldown_ticks", 500));
    private static volatile int STARS_DURATION_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("STARS_DURATION_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.ghroth.ghroth_state_manager.stars_duration_ticks", 260));
    private static volatile int JUSTICE_COOLDOWN_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("JUSTICE_COOLDOWN_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.ghroth.ghroth_state_manager.justice_cooldown_ticks", 500));
    private static volatile int JUSTICE_DURATION_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("JUSTICE_DURATION_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.ghroth.ghroth_state_manager.justice_duration_ticks", 260));
    private static volatile int NOON_COOLDOWN_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("NOON_COOLDOWN_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.ghroth.ghroth_state_manager.noon_cooldown_ticks", 1000));
    private static volatile int NOON_GAZE_TOTAL_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("NOON_GAZE_TOTAL_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.ghroth.ghroth_state_manager.noon_gaze_total_ticks", 120));
    private static volatile int NOON_OUTPUT_WINDOW_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("NOON_OUTPUT_WINDOW_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.ghroth.ghroth_state_manager.noon_output_window_ticks", 60));
    private static volatile int NOON_DURATION_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("NOON_DURATION_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue(
      "characters.ghroth.ghroth_state_manager.noon_duration_ticks", NOON_GAZE_TOTAL_TICKS + NOON_OUTPUT_WINDOW_TICKS
   ));
    private static volatile int NOON_MAX_GRANTED_SECONDS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("NOON_MAX_GRANTED_SECONDS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.ghroth.ghroth_state_manager.noon_max_granted_seconds", 6));
    private static volatile int NOON_MAX_DAMAGE_COPIES = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("NOON_MAX_DAMAGE_COPIES", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.ghroth.ghroth_state_manager.noon_max_damage_copies", 12));
    private static volatile int TACTICAL_COOLDOWN_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("TACTICAL_COOLDOWN_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.ghroth.ghroth_state_manager.tactical_cooldown_ticks", 12));
    private static volatile int TACTICAL_BOOST_TOTAL_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("TACTICAL_BOOST_TOTAL_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.ghroth.ghroth_state_manager.tactical_boost_total_ticks", 8));
    private static volatile int TACTICAL_IMMUNE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("TACTICAL_IMMUNE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.ghroth.ghroth_state_manager.tactical_immune_ticks", 12));
    private static volatile int CEASEFIRE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("CEASEFIRE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.ghroth.ghroth_state_manager.ceasefire_ticks", 3600));
    private static volatile int JUSTICE_MAX_STACKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("JUSTICE_MAX_STACKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.ghroth.ghroth_state_manager.justice_max_stacks", 14));
    private static volatile double REVEAL_RANGE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("REVEAL_RANGE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("characters.ghroth.ghroth_state_manager.reveal_range", 30.0));
    private static volatile double STARS_TARGET_RANGE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("STARS_TARGET_RANGE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("characters.ghroth.ghroth_state_manager.stars_target_range", 96.0));
    private static volatile double NOON_TARGET_RANGE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("NOON_TARGET_RANGE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("characters.ghroth.ghroth_state_manager.noon_target_range", 96.0));
    private static volatile double NOON_FALLBACK_FREEZE_RANGE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("NOON_FALLBACK_FREEZE_RANGE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue(
      "characters.ghroth.ghroth_state_manager.noon_fallback_freeze_range", 128.0
   ));
    private static final Map<UUID, FrozenEntityState> FROZEN_ENTITIES = new HashMap<>();

    private GhrothStateManager() {
    }

    public static boolean isGhroth(Player player) {
        return CharacterSelectionManager.getSelectedCharacterId(player)
                .map(ModCharacters.GHROTH_ID::equals)
                .orElse(false);
    }

    public static void initializeIfNeeded(ServerPlayer player) {
        CompoundTag tag = data(player);
        if (tag.getBoolean(INITIALIZED)) {
            return;
        }
        tag.putBoolean(INITIALIZED, true);
    }

    public static void copyState(Player original, Player target) {
        CompoundTag from = original.getPersistentData();
        if (from.contains(ROOT_TAG, Tag.TAG_COMPOUND)) {
            target.getPersistentData().put(ROOT_TAG, from.getCompound(ROOT_TAG).copy());
        }
    }

    public static void clearRuntimeOnDeath(ServerPlayer player) {
        CompoundTag tag = data(player);
        tag.remove(STARS_ACTIVE_UNTIL);
        tag.remove(JUSTICE_ACTIVE_UNTIL);
        tag.remove(NOON_ACTIVE_UNTIL);
        tag.remove(NOON_DAMAGE_COPIES);
        tag.remove(NOON_LAST_COPY_TICK);
        clearNoonGaze(tag);
        tag.remove(TACTICAL_IMMUNE_UNTIL);
        tag.remove(TACTICAL_MODE);
        tag.putInt(TACTICAL_BOOST_TICKS, 0);
        syncToClient(player);
    }

    public static void clearState(ServerPlayer player) {
        clearRuntimeOnDeath(player);
    }

    public static void clearRuntimeCaches() {
        FROZEN_ENTITIES.clear();
    }

    public static void tick(ServerPlayer player) {
        if (!isGhroth(player)) {
            return;
        }
        initializeIfNeeded(player);
        tickPassiveReveal(player);
        tickTacticalInput(player);
        tickTacticalBoost(player);
        boolean noonActive = isNoonActive(player);
        if (noonActive) {
            if (isNoonCharging(player)) {
                tickNoonGaze(player);
            }
        } else {
            clearExpiredNoonState(data(player));
        }
        releaseExpiredFreezes(player.serverLevel(), SkillCooldownHelper.now(player));
        if (noonActive || player.tickCount % 5 == 0) {
            syncToClient(player);
        }
    }

    public static boolean useStars(ServerPlayer player) {
        initializeIfNeeded(player);
        long now = SkillCooldownHelper.now(player);
        CompoundTag tag = data(player);
        if (now < tag.getLong(STARS_COOLDOWN_UNTIL)) {
            SkillCooldownHelper.notifyCooldown(player,
                    Component.translatable("message.dealt_force_skills.ghroth.skill_cooldown"));
            return true;
        }
        tag.putLong(STARS_COOLDOWN_UNTIL, SkillCooldownHelper.until(player, now, STARS_COOLDOWN_TICKS));
        tag.putLong(STARS_ACTIVE_UNTIL, now + STARS_DURATION_TICKS);
        player.level().playSound(null, player.blockPosition(), ModSounds.GHROTH_STARS.get(), SoundSource.PLAYERS, 0.9F, 1.0F);
        syncToClient(player);
        return true;
    }

    public static boolean useJustice(ServerPlayer player) {
        initializeIfNeeded(player);
        long now = SkillCooldownHelper.now(player);
        CompoundTag tag = data(player);
        if (now < tag.getLong(JUSTICE_COOLDOWN_UNTIL)) {
            SkillCooldownHelper.notifyCooldown(player,
                    Component.translatable("message.dealt_force_skills.ghroth.skill_cooldown"));
            return true;
        }
        tag.putLong(JUSTICE_COOLDOWN_UNTIL, SkillCooldownHelper.until(player, now, JUSTICE_COOLDOWN_TICKS));
        tag.putLong(JUSTICE_ACTIVE_UNTIL, now + JUSTICE_DURATION_TICKS);
        player.level().playSound(null, player.blockPosition(), ModSounds.GHROTH_JUSTICE.get(), SoundSource.PLAYERS, 0.9F, 1.0F);
        syncToClient(player);
        return true;
    }

    public static boolean useNoon(ServerPlayer player) {
        initializeIfNeeded(player);
        long now = SkillCooldownHelper.now(player);
        CompoundTag tag = data(player);
        if (now < tag.getLong(NOON_COOLDOWN_UNTIL)) {
            SkillCooldownHelper.notifyCooldown(player,
                    Component.translatable("message.dealt_force_skills.ghroth.skill_cooldown"));
            return true;
        }
        tag.putLong(NOON_COOLDOWN_UNTIL, SkillCooldownHelper.until(player, now, NOON_COOLDOWN_TICKS));
        tag.putLong(NOON_ACTIVE_UNTIL, now + NOON_DURATION_TICKS);
        tag.remove(NOON_DAMAGE_COPIES);
        tag.remove(NOON_LAST_COPY_TICK);
        clearNoonGaze(tag);
        player.level().playSound(null, player.blockPosition(), ModSounds.GHROTH_NOON.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        syncToClient(player);
        return true;
    }

    public static boolean isStarsActive(ServerPlayer player) {
        return data(player).getLong(STARS_ACTIVE_UNTIL) > SkillCooldownHelper.now(player);
    }

    public static boolean isJusticeActive(ServerPlayer player) {
        return data(player).getLong(JUSTICE_ACTIVE_UNTIL) > SkillCooldownHelper.now(player);
    }

    public static boolean isNoonActive(ServerPlayer player) {
        return data(player).getLong(NOON_ACTIVE_UNTIL) > SkillCooldownHelper.now(player);
    }

    private static boolean isNoonCharging(ServerPlayer player) {
        return noonActiveRemainingTicks(player) > NOON_OUTPUT_WINDOW_TICKS;
    }

    public static boolean isTacticalImmune(Player player) {
        return isGhroth(player) && data(player).getLong(TACTICAL_IMMUNE_UNTIL) > SkillCooldownHelper.now(player);
    }

    public static boolean shouldDodge(Player player, DamageSource source) {
        return isGhroth(player)
                && !source.is(SkillDamageHelper.TRUE_SKILL_DAMAGE)
                && player.getRandom().nextFloat() < 0.3F;
    }

    public static void recordJusticeKill(ServerPlayer killer, LivingEntity victim) {
        if (!isJusticeActive(killer)) {
            return;
        }
        if (victim instanceof ServerPlayer killedPlayer) {
            long until = killer.level().getGameTime() + CEASEFIRE_TICKS;
            putCeasefire(killer, killedPlayer.getUUID(), until);
            putCeasefire(killedPlayer, killer.getUUID(), until);
            return;
        }
        String key = entityTypeKey(victim.getType());
        if (key.isBlank()) {
            return;
        }
        CompoundTag stacks = data(killer).getCompound(JUSTICE_MOB_STACKS);
        stacks.putInt(key, Mth.clamp(stacks.getInt(key) + 1, 0, JUSTICE_MAX_STACKS));
        data(killer).put(JUSTICE_MOB_STACKS, stacks);
        syncToClient(killer);
    }

    public static float incomingDamageMultiplier(Player player, Entity sourceEntity) {
        if (!isGhroth(player) || sourceEntity == null) {
            return 1.0F;
        }
        String key = entityTypeKey(sourceEntity.getType());
        if (key.isBlank()) {
            return 1.0F;
        }
        int stacks = Mth.clamp(data(player).getCompound(JUSTICE_MOB_STACKS).getInt(key), 0, JUSTICE_MAX_STACKS);
        return Math.max(0.0F, 1.0F - stacks * 0.07F);
    }

    public static boolean shouldCancelCeasefire(Player left, Player right) {
        if (left == null || right == null || left == right) {
            return false;
        }
        long now = left.level().getGameTime();
        return ceasefireUntil(left, right.getUUID()) > now || ceasefireUntil(right, left.getUUID()) > now;
    }

    public static Optional<LivingEntity> findStarsAimTarget(ServerPlayer player) {
        return findAimTarget(player);
    }

    public static void handleStarsTaczHit(ServerPlayer player, LivingEntity hitTarget, float damage) {
        if (!isStarsActive(player) || damage <= 0.0F || !Float.isFinite(damage)) {
            return;
        }
        LivingEntity target = findAimTarget(player).orElse(hitTarget);
        applyStarsDamage(player, target, damage);
    }

    public static void handleStarsGuaranteedShot(ServerPlayer player, LivingEntity target, float damage) {
        if (!isStarsActive(player) || damage <= 0.0F || !Float.isFinite(damage)) {
            return;
        }
        applyStarsDamage(player, target, damage);
    }

    private static void applyStarsDamage(ServerPlayer player, LivingEntity target, float damage) {
        if (target == null || !TargetingUtil.isHostileLivingFor(player, target)) {
            return;
        }
        if (target instanceof Player other && shouldCancelCeasefire(player, other)) {
            return;
        }
        target.invulnerableTime = 0;
        boolean damaged = SkillDamageHelper.hurtUnscaled(target,
                SkillDamageHelper.trueDamage(player.serverLevel(), player, player),
                damage);
        if (damaged) {
            DfsAchievements.recordGhrothStarsHit(player);
        }
    }

    public static int starsCooldownRemainingTicks(Player player) {
        return remainingTicks(player, STARS_COOLDOWN_UNTIL);
    }

    public static int justiceCooldownRemainingTicks(Player player) {
        return remainingTicks(player, JUSTICE_COOLDOWN_UNTIL);
    }

    public static int noonCooldownRemainingTicks(Player player) {
        return remainingTicks(player, NOON_COOLDOWN_UNTIL);
    }

    public static int starsActiveRemainingTicks(Player player) {
        return remainingTicks(player, STARS_ACTIVE_UNTIL);
    }

    public static int justiceActiveRemainingTicks(Player player) {
        return remainingTicks(player, JUSTICE_ACTIVE_UNTIL);
    }

    public static int noonActiveRemainingTicks(Player player) {
        return remainingTicks(player, NOON_ACTIVE_UNTIL);
    }

    public static int noonGazeTargetId(Player player) {
        return data(player).getInt(NOON_GAZE_TARGET_ID);
    }

    public static int noonGazeTicks(Player player) {
        return Mth.clamp(data(player).getInt(NOON_GAZE_TICKS), 0, NOON_GAZE_TOTAL_TICKS);
    }

    public static int noonDamageCopies(Player player) {
        return Mth.clamp(data(player).getInt(NOON_DAMAGE_COPIES), 0, NOON_MAX_DAMAGE_COPIES);
    }

    public static int consumeNoonDamageCopiesForHit(ServerPlayer player) {
        if (!isGhroth(player) || !isNoonActive(player)) {
            return 0;
        }
        CompoundTag tag = data(player);
        int copies = Mth.clamp(tag.getInt(NOON_DAMAGE_COPIES), 0, NOON_MAX_DAMAGE_COPIES);
        if (copies <= 0) {
            return 0;
        }
        tag.putInt(NOON_DAMAGE_COPIES, 0);
        syncToClient(player);
        return copies;
    }

    public static boolean tryMarkNoonDamageCopyTick(ServerPlayer player) {
        if (!isGhroth(player)) {
            return false;
        }
        CompoundTag tag = data(player);
        long now = SkillCooldownHelper.now(player);
        if (tag.getLong(NOON_LAST_COPY_TICK) == now) {
            return false;
        }
        tag.putLong(NOON_LAST_COPY_TICK, now);
        return true;
    }

    public static void syncToClient(ServerPlayer player) {
        if (!isGhroth(player)) {
            return;
        }
        initializeIfNeeded(player);
        NetworkHandler.sendToPlayer(new S2C_SyncGhrothState(
                starsCooldownRemainingTicks(player),
                justiceCooldownRemainingTicks(player),
                noonCooldownRemainingTicks(player),
                starsActiveRemainingTicks(player),
                justiceActiveRemainingTicks(player),
                noonActiveRemainingTicks(player),
                noonGazeTargetId(player),
                noonGazeTicks(player),
                noonDamageCopies(player),
                isTacticalImmune(player)
        ), player);
    }

    private static void tickPassiveReveal(ServerPlayer player) {
        if (player.tickCount % 10 != 0) {
            return;
        }
        AABB box = player.getBoundingBox().inflate(REVEAL_RANGE);
        int revealed = 0;
        for (LivingEntity target : player.serverLevel().getEntitiesOfClass(LivingEntity.class, box,
                entity -> entity != player && entity.isAlive() && !(entity instanceof Player))) {
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.ghroth.ghroth_state_manager.effect.glowing.0.duration_ticks", 14), com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.ghroth.ghroth_state_manager.effect.glowing.0.amplifier", 0), true, false));
            revealed++;
            DfsAchievements.recordGhrothPassiveRevealTarget(player, target, target.hasEffect(MobEffects.INVISIBILITY));
        }
        if (revealed > 0) {
            DfsAchievements.recordGhrothPassiveReveal(player, revealed);
        }
    }

    private static void tickTacticalInput(ServerPlayer player) {
        CompoundTag tag = data(player);
        long now = SkillCooldownHelper.now(player);
        if (now < tag.getLong(TACTICAL_COOLDOWN_UNTIL) || !player.isSprinting()) {
            return;
        }
        if (player.isShiftKeyDown()) {
            if (tryConsumeTacticalStamina(player)) {
                startTacticalBoost(player, 1.175D, TACTICAL_MODE_ROLL);
            }
            return;
        }
        if (!player.onGround() && player.getDeltaMovement().y > -0.08D) {
            if (tryConsumeTacticalStamina(player)) {
                startTacticalBoost(player, 0.775D, TACTICAL_MODE_LUNGE);
            }
        }
    }

    private static boolean tryConsumeTacticalStamina(ServerPlayer player) {
        StaminaManager.ConsumeResult result = StaminaManager.consume(player,
                com.rzy.dealt_force_skills.stamina.StaminaRules.GHROTH_TACTICAL_COST);
        if (result == StaminaManager.ConsumeResult.NOT_ENOUGH) {
            return false;
        }

        return true;
    }

    private static void startTacticalBoost(ServerPlayer player, double speed, String mode) {
        CompoundTag tag = data(player);
        long now = SkillCooldownHelper.now(player);
        Vec3 look = player.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0.0D, look.z);
        if (horizontal.lengthSqr() < 0.001D) {
            horizontal = new Vec3(player.getDeltaMovement().x, 0.0D, player.getDeltaMovement().z);
        }
        if (horizontal.lengthSqr() < 0.001D) {
            return;
        }
        horizontal = horizontal.normalize();
        tag.putLong(TACTICAL_COOLDOWN_UNTIL, now + TACTICAL_COOLDOWN_TICKS);
        tag.putLong(TACTICAL_IMMUNE_UNTIL, now + TACTICAL_IMMUNE_TICKS);
        tag.putInt(TACTICAL_BOOST_TICKS, TACTICAL_BOOST_TOTAL_TICKS);
        tag.putDouble(TACTICAL_DIR_X, horizontal.x * speed);
        tag.putDouble(TACTICAL_DIR_Z, horizontal.z * speed);
        tag.putString(TACTICAL_MODE, mode);
        if (TACTICAL_MODE_ROLL.equals(mode)) {
            NetworkHandler.sendToPlayer(new S2C_TempestStartRoll(), player);
        }
    }

    private static void tickTacticalBoost(ServerPlayer player) {
        CompoundTag tag = data(player);
        int ticks = tag.getInt(TACTICAL_BOOST_TICKS);
        if (ticks <= 0) {
            tag.remove(TACTICAL_MODE);
            return;
        }
        tag.putInt(TACTICAL_BOOST_TICKS, ticks - 1);
        Vec3 current = player.getDeltaMovement();
        double scale = ticks / (double) TACTICAL_BOOST_TOTAL_TICKS;
        player.setDeltaMovement(
                current.x + tag.getDouble(TACTICAL_DIR_X) * scale,
                Math.max(current.y, 0.08D),
                current.z + tag.getDouble(TACTICAL_DIR_Z) * scale
        );
        player.setPose(Pose.SWIMMING);
        player.refreshDimensions();
        if (ticks <= 1) {
            tag.remove(TACTICAL_MODE);
        }
        player.fallDistance = 0.0F;
        player.hurtMarked = true;
    }

    private static void tickNoonGaze(ServerPlayer player) {
        CompoundTag tag = data(player);
        Optional<LivingEntity> target = findNoonAimTarget(player);
        if (target.isEmpty()) {
            clearNoonGaze(tag);
            return;
        }
        int targetId = target.get().getId();
        if (tag.getInt(NOON_GAZE_TARGET_ID) != targetId) {
            tag.putInt(NOON_GAZE_TARGET_ID, targetId);
            tag.putInt(NOON_GAZE_TICKS, 1);
            tag.putInt(NOON_GRANTED_SECONDS, 0);
            return;
        }
        tag.putInt(NOON_GAZE_TICKS, Mth.clamp(tag.getInt(NOON_GAZE_TICKS) + 1, 0, NOON_GAZE_TOTAL_TICKS));
        grantNoonDamageCopies(tag);
    }

    private static void grantNoonDamageCopies(CompoundTag tag) {
        int readySeconds = Mth.clamp((tag.getInt(NOON_GAZE_TICKS) + 1) / 20, 0, NOON_MAX_GRANTED_SECONDS);
        int grantedSeconds = Mth.clamp(tag.getInt(NOON_GRANTED_SECONDS), 0, NOON_MAX_GRANTED_SECONDS);
        int copies = Mth.clamp(tag.getInt(NOON_DAMAGE_COPIES), 0, NOON_MAX_DAMAGE_COPIES);
        while (grantedSeconds < readySeconds) {
            grantedSeconds++;
            copies += grantedSeconds <= 3 ? 1 : 3;
        }
        tag.putInt(NOON_GRANTED_SECONDS, grantedSeconds);
        tag.putInt(NOON_DAMAGE_COPIES, Mth.clamp(copies, 0, NOON_MAX_DAMAGE_COPIES));
    }

    private static void clearNoonGaze(CompoundTag tag) {
        tag.remove(NOON_GAZE_TARGET_ID);
        tag.remove(NOON_GAZE_TICKS);
        tag.remove(NOON_GRANTED_SECONDS);
    }

    private static void clearExpiredNoonState(CompoundTag tag) {
        clearNoonGaze(tag);
        tag.remove(NOON_DAMAGE_COPIES);
        tag.remove(NOON_LAST_COPY_TICK);
    }

    private static void applyNoonSlow(ServerPlayer player) {
        Vec3 motion = player.getDeltaMovement();
        player.setDeltaMovement(motion.x * 0.25D, motion.y, motion.z * 0.25D);
        player.hurtMarked = true;
    }

    private static void freezeLoadedEntities(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        for (Entity entity : loadedEntities(level, player)) {
            if (entity == player || entity instanceof Player || entity.isRemoved()) {
                continue;
            }
            freezeEntity(entity, now);
        }
    }

    private static Iterable<Entity> loadedEntities(ServerLevel level, ServerPlayer fallbackCenter) {
        try {
            Method method = level.getClass().getMethod("getAllEntities");
            Object result = method.invoke(level);
            if (result instanceof Iterable<?> iterable) {
                return () -> new Iterator<>() {
                    private final Iterator<?> delegate = iterable.iterator();

                    @Override
                    public boolean hasNext() {
                        return delegate.hasNext();
                    }

                    @Override
                    public Entity next() {
                        return (Entity) delegate.next();
                    }
                };
            }
        } catch (ReflectiveOperationException | ClassCastException | LinkageError ignored) {
            // Fallback below keeps the skill usable on mappings without getAllEntities.
        }
        return level.getEntities(fallbackCenter, fallbackCenter.getBoundingBox().inflate(NOON_FALLBACK_FREEZE_RANGE));
    }

    private static void freezeEntity(Entity entity, long now) {
        FrozenEntityState state = FROZEN_ENTITIES.computeIfAbsent(entity.getUUID(), uuid ->
                new FrozenEntityState(
                        entity instanceof Mob mob && mob.isNoAi(),
                        entity.isNoGravity()
                ));
        state.lastTick = now;
        entity.setDeltaMovement(Vec3.ZERO);
        entity.setNoGravity(true);
        entity.fallDistance = 0.0F;
        entity.hurtMarked = true;
        if (entity instanceof Projectile projectile) {
            projectile.setDeltaMovement(Vec3.ZERO);
        }
        if (entity instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.ghroth.ghroth_state_manager.effect.movement_slowdown.1.duration_ticks", 4), com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.ghroth.ghroth_state_manager.effect.movement_slowdown.1.amplifier", 10), true, false));
        }
        if (entity instanceof Mob mob) {
            mob.setNoAi(true);
            mob.getNavigation().stop();
            mob.setTarget(null);
        }
    }

    private static void releaseExpiredFreezes(ServerLevel level, long now) {
        Iterator<Map.Entry<UUID, FrozenEntityState>> iterator = FROZEN_ENTITIES.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, FrozenEntityState> entry = iterator.next();
            FrozenEntityState state = entry.getValue();
            if (now - state.lastTick <= 2L) {
                continue;
            }
            Entity entity = level.getEntity(entry.getKey());
            if (entity != null) {
                entity.setNoGravity(state.oldNoGravity);
                if (entity instanceof Mob mob) {
                    mob.setNoAi(state.oldNoAi);
                }
            }
            iterator.remove();
        }
    }

    private static Optional<LivingEntity> findAimTarget(ServerPlayer player) {
        return findAimTarget(player, STARS_TARGET_RANGE, 4.0D);
    }

    private static Optional<LivingEntity> findNoonAimTarget(ServerPlayer player) {
        return findAimTarget(player, NOON_TARGET_RANGE, 0.75D);
    }

    private static Optional<LivingEntity> findAimTarget(ServerPlayer player, double range, double extraTolerance) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        AABB search = player.getBoundingBox().inflate(range);
        LivingEntity best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (LivingEntity target : player.serverLevel().getEntitiesOfClass(LivingEntity.class, search,
                target -> TargetingUtil.isHostileLivingFor(player, target))) {
            if (target instanceof Player other && shouldCancelCeasefire(player, other)) {
                continue;
            }
            Vec3 toTarget = target.getBoundingBox().getCenter().subtract(eye);
            double forward = toTarget.dot(look);
            if (forward <= 0.0D || forward > range) {
                continue;
            }
            double perpendicular = toTarget.subtract(look.scale(forward)).length();
            double tolerance = target.getBbWidth() * 0.5D + extraTolerance;
            if (perpendicular > tolerance) {
                continue;
            }
            double score = forward - perpendicular * 12.0D;
            if (score > bestScore) {
                bestScore = score;
                best = target;
            }
        }
        return Optional.ofNullable(best);
    }

    private static void putCeasefire(ServerPlayer player, UUID other, long until) {
        CompoundTag tag = data(player).getCompound(CEASEFIRE);
        tag.putLong(other.toString(), until);
        data(player).put(CEASEFIRE, tag);
    }

    private static long ceasefireUntil(Player player, UUID other) {
        return data(player).getCompound(CEASEFIRE).getLong(other.toString());
    }

    private static int remainingTicks(Player player, String key) {
        return SkillCooldownHelper.remainingTicks(player, data(player).getLong(key));
    }

    private static String entityTypeKey(EntityType<?> type) {
        ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        return key == null ? "" : key.toString();
    }

    private static CompoundTag data(Player player) {
        CompoundTag persistent = player.getPersistentData();
        if (!persistent.contains(ROOT_TAG, Tag.TAG_COMPOUND)) {
            persistent.put(ROOT_TAG, new CompoundTag());
        }
        return persistent.getCompound(ROOT_TAG);
    }

    private static final class FrozenEntityState {
        private final boolean oldNoAi;
        private final boolean oldNoGravity;
        private long lastTick;

        private FrozenEntityState(boolean oldNoAi, boolean oldNoGravity) {
            this.oldNoAi = oldNoAi;
            this.oldNoGravity = oldNoGravity;
        }
    }
}
