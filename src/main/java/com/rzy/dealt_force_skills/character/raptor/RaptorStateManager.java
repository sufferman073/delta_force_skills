package com.rzy.dealt_force_skills.character.raptor;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.CharacterSelectionManager;
import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.character.commander.CommanderStateManager;
import com.rzy.dealt_force_skills.effect.RaptorHummingbirdMarkedEffect;
import com.rzy.dealt_force_skills.entity.RaptorFalconDroneEntity;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.S2C_RaptorFootprints;
import com.rzy.dealt_force_skills.network.S2C_RaptorRevealEntities;
import com.rzy.dealt_force_skills.network.S2C_SyncRaptorState;
import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.util.RangedSoundHelper;
import com.rzy.dealt_force_skills.skill.SkillCooldownHelper;
import com.rzy.dealt_force_skills.team.TeamCombatRules;
import com.rzy.dealt_force_skills.util.ReconRevealThrottle;
import com.rzy.dealt_force_skills.util.TargetingUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetCameraPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class RaptorStateManager {
    public static volatile int FALCON_COOLDOWN_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("FALCON_COOLDOWN_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.raptor.raptor_state_manager.falcon_cooldown_ticks", 900));
    public static volatile int PULSE_MAX_CHARGES = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("PULSE_MAX_CHARGES", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.raptor.raptor_state_manager.pulse_max_charges", 2));
    public static volatile int PULSE_RECHARGE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("PULSE_RECHARGE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.raptor.raptor_state_manager.pulse_recharge_ticks", 800));
    public static volatile int HUMMINGBIRD_COOLDOWN_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("HUMMINGBIRD_COOLDOWN_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.raptor.raptor_state_manager.hummingbird_cooldown_ticks", 900));
    public static volatile int HUMMINGBIRD_ATTACH_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("HUMMINGBIRD_ATTACH_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.raptor.raptor_state_manager.hummingbird_attach_ticks", 20));
    public static volatile int HUMMINGBIRD_DURATION_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("HUMMINGBIRD_DURATION_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.raptor.raptor_state_manager.hummingbird_duration_ticks", 600));
    public static volatile int HUMMINGBIRD_REVEAL_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("HUMMINGBIRD_REVEAL_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.raptor.raptor_state_manager.hummingbird_reveal_ticks", 45));
    public static volatile double HUMMINGBIRD_RANGE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("HUMMINGBIRD_RANGE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("characters.raptor.raptor_state_manager.hummingbird_range", 90.0));
    private static final int HUMMINGBIRD_LOOP_REPLAY_TICKS = 180;
    private static volatile int FOOTPRINT_INTERVAL_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("FOOTPRINT_INTERVAL_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.raptor.raptor_state_manager.footprint_interval_ticks", 20));
    /** Footprint lifetime: 60 seconds (was 5 minutes; reduced for performance). */
    private static volatile int FOOTPRINT_LIFE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("FOOTPRINT_LIFE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.raptor.raptor_state_manager.footprint_life_ticks", 1200));
    private static final int FOOTPRINT_SYNC_LIMIT = 400;
    private static final int FOOTPRINT_STORAGE_LIMIT = 2048;
    private static volatile double FOOTPRINT_SYNC_RANGE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("FOOTPRINT_SYNC_RANGE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("characters.raptor.raptor_state_manager.footprint_sync_range", 128.0));
    private static volatile double FOOTPRINT_READ_RANGE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("FOOTPRINT_READ_RANGE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("characters.raptor.raptor_state_manager.footprint_read_range", 32.0));
    private static volatile double FOOTPRINT_READ_DISTANCE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("FOOTPRINT_READ_DISTANCE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("characters.raptor.raptor_state_manager.footprint_read_distance", 0.95));
    /** When a footprint is fully read and the owner is within this radius, expose position briefly. */
    private static volatile double FOOTPRINT_READ_REVEAL_RANGE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("FOOTPRINT_READ_REVEAL_RANGE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue(
      "characters.raptor.raptor_state_manager.footprint_read_reveal_range", 35.0
   ));
    private static volatile int FOOTPRINT_READ_REVEAL_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("FOOTPRINT_READ_REVEAL_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.raptor.raptor_state_manager.footprint_read_reveal_ticks", 60));
    private static final String ROOT_TAG = DealtForceSkillsMod.MODID + ".raptor";
    private static final String INITIALIZED = "Initialized";
    private static final String FALCON_COOLDOWN_UNTIL = "FalconCooldownUntil";
    private static final String PULSE_CHARGES = "PulseCharges";
    private static final String PULSE_NEXT_RECHARGE = "PulseNextRecharge";
    private static final String HUMMINGBIRD_COOLDOWN_UNTIL = "HummingbirdCooldownUntil";
    private static final String EQUIPPED_TOOL = "EquippedTool";
    private static final String HUMMINGBIRD_PENDING_TARGET = "HummingbirdPendingTarget";
    private static final String HUMMINGBIRD_PENDING_TICKS = "HummingbirdPendingTicks";
    private static final String LAST_FOOTPRINT_TICK = "LastFootprintTick";
    private static final String LAST_FOOTPRINT_X = "LastFootprintX";
    private static final String LAST_FOOTPRINT_Y = "LastFootprintY";
    private static final String LAST_FOOTPRINT_Z = "LastFootprintZ";
    private static final String READ_FOOTPRINT_ID = "ReadFootprintId";
    private static final String READ_FOOTPRINT_TICKS = "ReadFootprintTicks";

    private static final Map<ResourceKey<Level>, List<FootprintState>> FOOTPRINTS = new HashMap<>();
    private static final Map<java.util.UUID, Integer> ACTIVE_HUMMINGBIRD_TARGETS = new HashMap<>();
    private static final java.util.Set<java.util.UUID> HUMMINGBIRD_VIEWERS = new java.util.HashSet<>();
    private static final Map<java.util.UUID, java.util.Set<Integer>> SCANNED_FOOTPRINTS = new HashMap<>();
    private static int nextFootprintId = 1;

    private RaptorStateManager() {
    }

    public static boolean isRaptor(Player player) {
        Optional<String> selected = CharacterSelectionManager.getSelectedCharacterId(player);
        return selected.isPresent() && ModCharacters.RAPTOR_ID.equals(selected.get())
                || com.rzy.dealt_force_skills.character.commander.CommanderStateManager
                .inheritsRuntimeSource(player, ModCharacters.RAPTOR_ID);
    }

    public static void initializeIfNeeded(ServerPlayer player) {
        if (!isRaptor(player)) {
            return;
        }
        CompoundTag tag = data(player);
        if (tag.getBoolean(INITIALIZED)) {
            return;
        }
        tag.putBoolean(INITIALIZED, true);
        tag.putLong(FALCON_COOLDOWN_UNTIL, 0L);
        tag.putInt(PULSE_CHARGES, pulseCapacity(player));
        tag.putLong(PULSE_NEXT_RECHARGE, 0L);
        tag.putLong(HUMMINGBIRD_COOLDOWN_UNTIL, 0L);
        tag.putInt(EQUIPPED_TOOL, RaptorTool.NONE.ordinal());
        tag.putInt(HUMMINGBIRD_PENDING_TARGET, -1);
    }

    public static void copyState(Player original, Player target) {
        CompoundTag originalData = original.getPersistentData();
        if (originalData.contains(ROOT_TAG, Tag.TAG_COMPOUND)) {
            target.getPersistentData().put(ROOT_TAG, originalData.getCompound(ROOT_TAG).copy());
        }
    }

    public static void clearState(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            stopHummingbirdLoop(serverPlayer);
        }
        player.getPersistentData().remove(ROOT_TAG);
    }

    public static void recordFootprint(ServerPlayer player) {
        if (!TargetingUtil.isTargetablePlayer(player) || player.isPassenger()) {
            return;
        }
        CompoundTag tag = data(player);
        long now = SkillCooldownHelper.now(player);
        if (now - tag.getLong(LAST_FOOTPRINT_TICK) < FOOTPRINT_INTERVAL_TICKS) {
            return;
        }
        Vec3 pos = player.position();
        Vec3 last = new Vec3(tag.getDouble(LAST_FOOTPRINT_X), tag.getDouble(LAST_FOOTPRINT_Y), tag.getDouble(LAST_FOOTPRINT_Z));
        if (tag.contains(LAST_FOOTPRINT_TICK) && pos.distanceToSqr(last) < 1.0D) {
            return;
        }
        tag.putLong(LAST_FOOTPRINT_TICK, now);
        tag.putDouble(LAST_FOOTPRINT_X, pos.x);
        tag.putDouble(LAST_FOOTPRINT_Y, pos.y);
        tag.putDouble(LAST_FOOTPRINT_Z, pos.z);
        List<FootprintState> list = FOOTPRINTS.computeIfAbsent(player.level().dimension(), key -> new ArrayList<>());
        list.add(new FootprintState(nextFootprintId++, player.getUUID().toString(), player.getGameProfile().getName(),
                equipmentSummary(player), pos, now));
        cleanupFootprints(player.serverLevel(), now);
    }

    public static void tick(ServerPlayer player) {
        if (!isRaptor(player)) {
            return;
        }
        initializeIfNeeded(player);
        long now = SkillCooldownHelper.now(player);
        rechargePulse(player, now);
        tickHummingbirdAttach(player);
        tickActiveHummingbird(player);
        tickFootprintRead(player);
        if (player.tickCount % 20 == 0) {
            syncFootprints(player);
        }
    }

    public static void revealFromHummingbird(LivingEntity marked) {
        if (!(marked.level() instanceof ServerLevel level)) {
            return;
        }
        var nativeOwner = RaptorHummingbirdMarkedEffect.nativeOwner(level, marked).orElse(null);
        if (nativeOwner != null) {
            com.rzy.dealt_force_skills.ai.operative.NativeRaptorSkills.revealFromHummingbird(nativeOwner, marked);
            return;
        }
        ServerPlayer owner = RaptorHummingbirdMarkedEffect.owner(level, marked).orElse(null);
        if (owner == null || !owner.isAlive()) {
            return;
        }
        if (!ReconRevealThrottle.tryStart(marked, HUMMINGBIRD_REVEAL_TICKS)) {
            return;
        }
        List<RaptorRevealMarker> markers = new ArrayList<>();
        markers.add(new RaptorRevealMarker(marked.getId(), marked.position(), HUMMINGBIRD_REVEAL_TICKS));
        Vec3 eye = marked.getEyePosition();
        Vec3 look = marked.getLookAngle().normalize();
        AABB box = marked.getBoundingBox().inflate(32.0D);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (target == marked || target == owner || !TargetingUtil.isTargetableLiving(target)
                    || !com.rzy.dealt_force_skills.util.PositionRevealHelper.isValidReconTarget(owner, target)) {
                continue;
            }
            Vec3 center = target.position().add(0.0D, target.getBbHeight() * 0.55D, 0.0D);
            Vec3 toTarget = center.subtract(eye);
            double distance = toTarget.length();
            if (distance > 32.0D || distance < 0.001D || look.dot(toTarget.scale(1.0D / distance)) < 0.35D) {
                continue;
            }
            HitResult result = level.clip(new ClipContext(eye, center, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, marked));
            if ((result.getType() == HitResult.Type.MISS || result.getLocation().distanceToSqr(center) < 0.6D)
                    && ReconRevealThrottle.tryStart(target, HUMMINGBIRD_REVEAL_TICKS)) {
                markers.add(new RaptorRevealMarker(target.getId(), target.position(), HUMMINGBIRD_REVEAL_TICKS));
            }
        }
        com.rzy.dealt_force_skills.util.PositionRevealHelper.sendToCasterAndTeammates(
                owner, new S2C_RaptorRevealEntities(markers));
        RangedSoundHelper.playFollowingPlayer(owner, ModSounds.RAPTOR_HUMMINGBIRD_REVEAL.get(),
                SoundSource.PLAYERS, 0.45f, 1.0f, 32.0D);
    }

    public static void revealEntities(ServerPlayer owner, List<? extends LivingEntity> targets, int ticks) {
        if (targets.isEmpty()) {
            return;
        }
        List<RaptorRevealMarker> markers = targets.stream()
                .filter(LivingEntity::isAlive)
                .filter(target -> com.rzy.dealt_force_skills.util.PositionRevealHelper.isValidReconTarget(owner, target))
                .filter(target -> ReconRevealThrottle.tryStart(target, ticks))
                .map(target -> new RaptorRevealMarker(target.getId(), target.position(), ticks))
                .toList();
        if (markers.isEmpty()) {
            return;
        }
        com.rzy.dealt_force_skills.util.PositionRevealHelper.sendToCasterAndTeammates(
                owner, new S2C_RaptorRevealEntities(markers));
    }

    public static void applyFalconHummingbird(ServerPlayer owner, List<? extends LivingEntity> targets,
                                              boolean hummingbirdFalcon) {
        if (owner == null || !hummingbirdFalcon) {
            return;
        }
        int duration = CommanderStateManager.skillDuration(owner,
                Math.max(HUMMINGBIRD_DURATION_TICKS, HUMMINGBIRD_REVEAL_TICKS));
        int amplifier = hummingbirdAmplifier(owner);
        for (LivingEntity target : targets) {
            if (!target.isAlive()
                    || !com.rzy.dealt_force_skills.util.PositionRevealHelper.isValidReconTarget(owner, target)) {
                continue;
            }
            RaptorHummingbirdMarkedEffect.setOwner(target, owner);
            target.addEffect(new MobEffectInstance(ModEffects.RAPTOR_HUMMINGBIRD_MARKED.get(),
                    duration, amplifier,
                    false, true, true), owner);
        }
    }

    public static boolean consumeFalcon(ServerPlayer player) {
        long now = SkillCooldownHelper.now(player);
        if (now < data(player).getLong(FALCON_COOLDOWN_UNTIL)) {
            return false;
        }
        data(player).putLong(FALCON_COOLDOWN_UNTIL,
                SkillCooldownHelper.until(player, now, FALCON_COOLDOWN_TICKS));
        return true;
    }

    public static boolean consumePulseCharge(ServerPlayer player) {
        CompoundTag tag = data(player);
        int capacity = pulseCapacity(player);
        int charges = Math.min(capacity, tag.getInt(PULSE_CHARGES));
        if (charges <= 0) {
            return false;
        }
        tag.putInt(PULSE_CHARGES, charges - 1);
        if (charges - 1 < capacity && tag.getLong(PULSE_NEXT_RECHARGE) <= 0L) {
            tag.putLong(PULSE_NEXT_RECHARGE,
                    SkillCooldownHelper.until(player, SkillCooldownHelper.now(player), PULSE_RECHARGE_TICKS));
        }
        return true;
    }

    public static boolean consumeHummingbird(ServerPlayer player) {
        long now = SkillCooldownHelper.now(player);
        if (now < data(player).getLong(HUMMINGBIRD_COOLDOWN_UNTIL)) {
            return false;
        }
        data(player).putLong(HUMMINGBIRD_COOLDOWN_UNTIL,
                SkillCooldownHelper.until(player, now, HUMMINGBIRD_COOLDOWN_TICKS));
        return true;
    }

    public static RaptorTool equippedTool(Player player) {
        int ordinal = data(player).getInt(EQUIPPED_TOOL);
        RaptorTool[] tools = RaptorTool.values();
        return ordinal >= 0 && ordinal < tools.length ? tools[ordinal] : RaptorTool.NONE;
    }

    public static void setEquippedTool(Player player, RaptorTool tool) {
        data(player).putInt(EQUIPPED_TOOL, tool.ordinal());
    }

    public static void beginHummingbirdAttach(ServerPlayer player) {
        if (stopHummingbirdView(player)) {
            return;
        }
        LivingEntity active = activeHummingbirdTarget(player).orElse(null);
        if (active != null) {
            startHummingbirdView(player, active);
            return;
        }
        if (data(player).getInt(HUMMINGBIRD_PENDING_TARGET) >= 0) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.raptor.hummingbird_attaching",
                    progressBar(data(player).getInt(HUMMINGBIRD_PENDING_TICKS), HUMMINGBIRD_ATTACH_TICKS)), true);
            return;
        }
        if (hummingbirdCooldownRemainingTicks(player) > 0) {
            SkillCooldownHelper.notifyCooldown(player,
                    Component.translatable("message.dealt_force_skills.raptor.hummingbird_cooldown"));
            return;
        }
        LivingEntity target = findHummingbirdTarget(player).orElse(null);
        if (target == null) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.raptor.hummingbird_no_target"), true);
            return;
        }
        data(player).putInt(HUMMINGBIRD_PENDING_TARGET, target.getId());
        data(player).putInt(HUMMINGBIRD_PENDING_TICKS, 0);
        player.level().playSound(null, player.blockPosition(), ModSounds.RAPTOR_HUMMINGBIRD_TARGET_AVAILABLE.get(),
                SoundSource.PLAYERS, 0.7f, 1.0f);
    }

    public static int falconCooldownRemainingTicks(Player player) {
        return remainingTicks(player, FALCON_COOLDOWN_UNTIL);
    }

    public static int pulseCharges(Player player) {
        return Math.min(pulseCapacity(player), data(player).getInt(PULSE_CHARGES));
    }

    public static int pulseRechargeRemainingTicks(Player player) {
        if (pulseCharges(player) >= pulseCapacity(player)) {
            return 0;
        }
        return remainingTicks(player, PULSE_NEXT_RECHARGE);
    }

    public static int hummingbirdCooldownRemainingTicks(Player player) {
        return remainingTicks(player, HUMMINGBIRD_COOLDOWN_UNTIL);
    }

    public static int hummingbirdPendingTicks(Player player) {
        return data(player).getInt(HUMMINGBIRD_PENDING_TICKS);
    }

    public static int activeFalconRemainingTicks(ServerPlayer player) {
        RaptorFalconDroneEntity drone = RaptorFalconDroneEntity.activeFor(player);
        return drone == null ? 0 : drone.remainingLifeTicks();
    }

    public static void clearRuntimeOnDeath(ServerPlayer player) {
        setEquippedTool(player, RaptorTool.NONE);
        data(player).putInt(HUMMINGBIRD_PENDING_TARGET, -1);
        data(player).putInt(HUMMINGBIRD_PENDING_TICKS, 0);
        stopHummingbirdView(player);
        stopHummingbirdLoop(player);
        ACTIVE_HUMMINGBIRD_TARGETS.remove(player.getUUID());
        SCANNED_FOOTPRINTS.remove(player.getUUID());
        RaptorFalconDroneEntity.discardFor(player);
    }

    public static void clearRuntimeOnLogout(ServerPlayer player) {
        if (player == null) {
            return;
        }
        java.util.UUID playerId = player.getUUID();
        stopHummingbirdView(player);
        stopHummingbirdLoop(player);
        ACTIVE_HUMMINGBIRD_TARGETS.remove(playerId);
        SCANNED_FOOTPRINTS.remove(playerId);
        RaptorFalconDroneEntity.discardFor(player);
        if (player.getPersistentData().contains(ROOT_TAG, Tag.TAG_COMPOUND)) {
            setEquippedTool(player, RaptorTool.NONE);
            data(player).putInt(HUMMINGBIRD_PENDING_TARGET, -1);
            data(player).putInt(HUMMINGBIRD_PENDING_TICKS, 0);
        }
    }

    public static void syncToClient(ServerPlayer player) {
        if (!isRaptor(player)) {
            return;
        }
        initializeIfNeeded(player);
        NetworkHandler.sendToPlayer(new S2C_SyncRaptorState(
                falconCooldownRemainingTicks(player),
                pulseCharges(player),
                pulseCapacity(player),
                pulseRechargeRemainingTicks(player),
                hummingbirdCooldownRemainingTicks(player),
                equippedTool(player).ordinal(),
                activeFalconRemainingTicks(player),
                hummingbirdPendingTicks(player),
                HUMMINGBIRD_ATTACH_TICKS
        ), player);
    }

    private static void tickHummingbirdAttach(ServerPlayer player) {
        CompoundTag tag = data(player);
        int targetId = tag.getInt(HUMMINGBIRD_PENDING_TARGET);
        if (targetId < 0) {
            return;
        }
        EntityLookup lookup = entityById(player.serverLevel(), targetId);
        double range = hummingbirdRange(player);
        if (!(lookup.entity() instanceof LivingEntity target)
                || !target.isAlive()
                || target.distanceTo(player) > range + 4.0D) {
            tag.putInt(HUMMINGBIRD_PENDING_TARGET, -1);
            tag.putInt(HUMMINGBIRD_PENDING_TICKS, 0);
            player.level().playSound(null, player.blockPosition(), ModSounds.RAPTOR_HUMMINGBIRD_LOST.get(),
                    SoundSource.PLAYERS, 0.75f, 1.0f);
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.raptor.hummingbird_lost"), true);
            return;
        }
        int ticks = tag.getInt(HUMMINGBIRD_PENDING_TICKS) + 1;
        tag.putInt(HUMMINGBIRD_PENDING_TICKS, ticks);
        player.displayClientMessage(Component.translatable("message.dealt_force_skills.raptor.hummingbird_attaching",
                progressBar(ticks, HUMMINGBIRD_ATTACH_TICKS)), true);
        if (ticks < HUMMINGBIRD_ATTACH_TICKS) {
            return;
        }
        RaptorHummingbirdMarkedEffect.setOwner(target, player);
        boolean questAttached = target.addEffect(new MobEffectInstance(ModEffects.RAPTOR_HUMMINGBIRD_MARKED.get(),
                CommanderStateManager.skillDuration(player, HUMMINGBIRD_DURATION_TICKS),
                hummingbirdAmplifier(player), false, true, true), player);
        if (questAttached) com.rzy.dealt_force_skills.department.DepartmentTasks.activated(player,"raptor.hummingbird.observed_target");
        ACTIVE_HUMMINGBIRD_TARGETS.put(player.getUUID(), target.getId());
        if (ReconRevealThrottle.tryStart(target, HUMMINGBIRD_REVEAL_TICKS)) {
            NetworkHandler.sendToPlayer(new S2C_RaptorRevealEntities(List.of(
                    new RaptorRevealMarker(target.getId(), target.position(), HUMMINGBIRD_REVEAL_TICKS))), player);
        }
        player.level().playSound(null, target.blockPosition(), ModSounds.RAPTOR_HUMMINGBIRD_ATTACH.get(),
                SoundSource.PLAYERS, 0.75f, 1.0f);
        player.playNotifySound(ModSounds.RAPTOR_HUMMINGBIRD_SUCCESS.get(), SoundSource.PLAYERS, 0.7f, 1.0f);
        startHummingbirdLoop(player, target);
        tag.putInt(HUMMINGBIRD_PENDING_TARGET, -1);
        tag.putInt(HUMMINGBIRD_PENDING_TICKS, 0);
    }


    private static void tickActiveHummingbird(ServerPlayer player) {
        Integer targetId = ACTIVE_HUMMINGBIRD_TARGETS.get(player.getUUID());
        if (targetId == null) {
            return;
        }
        EntityLookup lookup = entityById(player.serverLevel(), targetId);
        Entity entity = lookup.entity();
        LivingEntity activeTarget = entity instanceof LivingEntity living ? living : null;
        boolean destroyed = activeTarget == null || !activeTarget.isAlive();
        boolean valid = !destroyed
                && activeTarget.hasEffect(ModEffects.RAPTOR_HUMMINGBIRD_MARKED.get())
                && RaptorHummingbirdMarkedEffect.owner(player.serverLevel(), activeTarget).filter(owner -> owner == player).isPresent();
        if (!valid) {
            ACTIVE_HUMMINGBIRD_TARGETS.remove(player.getUUID());
            stopHummingbirdView(player);
            stopHummingbirdLoop(player);
            if (destroyed) {
                playHummingbirdDestroyed(player, entity);
            } else {
                player.playNotifySound(ModSounds.RAPTOR_HUMMINGBIRD_END.get(), SoundSource.PLAYERS, 0.65f, 1.0f);
            }
            consumeHummingbird(player);
            syncToClient(player);
            return;
        }
        if (SkillCooldownHelper.now(player) % HUMMINGBIRD_LOOP_REPLAY_TICKS == 0L) {
            startHummingbirdLoop(player, activeTarget);
        }
    }

    private static Optional<LivingEntity> activeHummingbirdTarget(ServerPlayer player) {
        Integer activeId = ACTIVE_HUMMINGBIRD_TARGETS.get(player.getUUID());
        if (activeId == null) {
            return Optional.empty();
        }
        EntityLookup lookup = entityById(player.serverLevel(), activeId);
        if (lookup.entity() instanceof LivingEntity target
                && target.isAlive()
                && target.hasEffect(ModEffects.RAPTOR_HUMMINGBIRD_MARKED.get())
                && RaptorHummingbirdMarkedEffect.owner(player.serverLevel(), target).filter(owner -> owner == player).isPresent()) {
            return Optional.of(target);
        }
        ACTIVE_HUMMINGBIRD_TARGETS.remove(player.getUUID());
        stopHummingbirdLoop(player);
        return Optional.empty();
    }

    private static void startHummingbirdLoop(ServerPlayer player, LivingEntity target) {
        RangedSoundHelper.playFollowingEntity(player.serverLevel(), target, ModSounds.RAPTOR_HUMMINGBIRD_ACTIVE_LOOP.get(),
                SoundSource.PLAYERS, 0.5f, 1.0f, 32.0D);
    }

    private static void stopHummingbirdLoop(ServerPlayer player) {
        RangedSoundHelper.stop(player.serverLevel(), ModSounds.RAPTOR_HUMMINGBIRD_ACTIVE_LOOP.get(), SoundSource.PLAYERS);
    }

    private static void playHummingbirdDestroyed(ServerPlayer player, Entity entity) {
        if (entity instanceof LivingEntity target) {
            player.level().playSound(null, target.blockPosition(), ModSounds.RAPTOR_HUMMINGBIRD_DESTROYED.get(),
                    SoundSource.PLAYERS, 0.7f, 1.0f);
        } else {
            player.playNotifySound(ModSounds.RAPTOR_HUMMINGBIRD_DESTROYED.get(), SoundSource.PLAYERS, 0.65f, 1.0f);
        }
    }

    private static void startHummingbirdView(ServerPlayer player, LivingEntity target) {
        if (!target.isAlive()) {
            return;
        }
        player.connection.send(new ClientboundSetCameraPacket(target));
        HUMMINGBIRD_VIEWERS.add(player.getUUID());
        player.level().playSound(null, target.blockPosition(), ModSounds.RAPTOR_HUMMINGBIRD_VIEW_ENTER.get(),
                SoundSource.PLAYERS, 0.65f, 1.0f);
    }

    public static boolean stopHummingbirdView(ServerPlayer player) {
        if (!HUMMINGBIRD_VIEWERS.remove(player.getUUID())) {
            return false;
        }
        player.connection.send(new ClientboundSetCameraPacket(player));
        player.level().playSound(null, player.blockPosition(), ModSounds.RAPTOR_HUMMINGBIRD_VIEW_EXIT.get(),
                SoundSource.PLAYERS, 0.65f, 1.0f);
        return true;
    }

    private static void tickFootprintRead(ServerPlayer player) {
        FootprintState footprint = lookedFootprint(player).orElse(null);
        CompoundTag tag = data(player);
        if (footprint == null) {
            tag.putInt(READ_FOOTPRINT_ID, -1);
            tag.putInt(READ_FOOTPRINT_TICKS, 0);
            return;
        }
        int ticks = tag.getInt(READ_FOOTPRINT_ID) == footprint.id ? tag.getInt(READ_FOOTPRINT_TICKS) + 1 : 1;
        tag.putInt(READ_FOOTPRINT_ID, footprint.id);
        tag.putInt(READ_FOOTPRINT_TICKS, ticks);
        if (ticks == 1) {
            player.playNotifySound(ModSounds.RAPTOR_FOOTPRINT_READ_START.get(), SoundSource.PLAYERS, 0.45f, 1.0f);
        }
        if (ticks < 20) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.raptor.footprint_reading",
                    progressBar(ticks, 20)), true);
            return;
        }
        int age = (int) Math.max(0, SkillCooldownHelper.now(player) - footprint.gameTime);
        player.displayClientMessage(Component.translatable("message.dealt_force_skills.raptor.footprint_info",
                footprint.ownerName, secondsText(age), footprint.equipmentSummary), true);
        scannedFootprints(player).add(footprint.id);
        if (ticks == 20) {
            player.playNotifySound(ModSounds.RAPTOR_FOOTPRINT_INFO.get(), SoundSource.PLAYERS, 0.55f, 1.0f);
            // Compensation for shorter footprint TTL: if owner is near, reveal for 3s to Raptor + teammates.
            revealFootprintOwnerIfNearby(player, footprint);
        }
    }

    /**
     * After a successful footprint read, if the owner is within {@link #FOOTPRINT_READ_REVEAL_RANGE}
     * blocks of the Raptor, expose their position for {@link #FOOTPRINT_READ_REVEAL_TICKS} ticks
     * to the Raptor and their teammates (same pipeline as other recon reveals).
     */
    private static void revealFootprintOwnerIfNearby(ServerPlayer raptor, FootprintState footprint) {
        if (raptor == null || footprint == null || footprint.ownerUuid == null) {
            return;
        }
        ServerPlayer owner = null;
        try {
            owner = raptor.server.getPlayerList().getPlayer(java.util.UUID.fromString(footprint.ownerUuid));
        } catch (IllegalArgumentException ignored) {
            return;
        }
        if (owner == null || !owner.isAlive() || owner.level() != raptor.level()) {
            return;
        }
        if (!TargetingUtil.isTargetablePlayer(owner)
                || !com.rzy.dealt_force_skills.util.PositionRevealHelper.isValidReconTarget(raptor, owner)) {
            return;
        }
        double range = FOOTPRINT_READ_REVEAL_RANGE;
        if (raptor.distanceToSqr(owner) > range * range) {
            return;
        }
        revealEntities(raptor, List.of(owner), FOOTPRINT_READ_REVEAL_TICKS);
    }

    private static void syncFootprints(ServerPlayer player) {
        List<FootprintState> list = FOOTPRINTS.getOrDefault(player.level().dimension(), List.of());
        long now = SkillCooldownHelper.now(player);
        String viewerId = player.getUUID().toString();
        java.util.Set<Integer> scanned = scannedFootprints(player);
        scanned.removeIf(id -> list.stream()
                .noneMatch(footprint -> footprint.id == id && now - footprint.gameTime <= FOOTPRINT_LIFE_TICKS));
        List<RaptorFootprintMarker> markers = list.stream()
                .filter(footprint -> now - footprint.gameTime <= FOOTPRINT_LIFE_TICKS)
                .filter(footprint -> !footprint.ownerUuid.equals(viewerId))
                .filter(footprint -> !TeamCombatRules.isTeammateFootprintOwner(player, footprint.ownerUuid))
                .filter(footprint -> footprint.position.distanceToSqr(player.position()) <= FOOTPRINT_SYNC_RANGE * FOOTPRINT_SYNC_RANGE)
                .sorted(Comparator.comparingDouble(footprint -> footprint.position.distanceToSqr(player.position())))
                .map(footprint -> new RaptorFootprintMarker(footprint.id, footprint.position,
                        (int) (now - footprint.gameTime), footprint.ownerName, footprint.equipmentSummary,
                        scanned.contains(footprint.id)))
                .limit(FOOTPRINT_SYNC_LIMIT)
                .toList();
        NetworkHandler.sendToPlayer(new S2C_RaptorFootprints(markers), player);
    }

    private static java.util.Set<Integer> scannedFootprints(ServerPlayer player) {
        return SCANNED_FOOTPRINTS.computeIfAbsent(player.getUUID(), ignored -> new java.util.HashSet<>());
    }

    private static Optional<FootprintState> lookedFootprint(ServerPlayer player) {
        List<FootprintState> list = FOOTPRINTS.getOrDefault(player.level().dimension(), List.of());
        if (list.isEmpty()) {
            return Optional.empty();
        }
        long now = SkillCooldownHelper.now(player);
        String viewerId = player.getUUID().toString();
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        Vec3 end = eye.add(look.scale(FOOTPRINT_READ_RANGE));
        return list.stream()
                .filter(footprint -> now - footprint.gameTime <= FOOTPRINT_LIFE_TICKS)
                .filter(footprint -> !footprint.ownerUuid.equals(viewerId))
                .filter(footprint -> !TeamCombatRules.isTeammateFootprintOwner(player, footprint.ownerUuid))
                .filter(footprint -> footprint.position.distanceToSqr(player.position()) <= FOOTPRINT_READ_RANGE * FOOTPRINT_READ_RANGE)
                .min(Comparator.comparingDouble(footprint -> distanceToSegmentSqr(footprint.position.add(0.0D, 0.1D, 0.0D), eye, end)))
                .filter(footprint -> distanceToSegmentSqr(footprint.position.add(0.0D, 0.1D, 0.0D), eye, end)
                        <= FOOTPRINT_READ_DISTANCE * FOOTPRINT_READ_DISTANCE);
    }


    private static Optional<LivingEntity> findHummingbirdTarget(ServerPlayer player) {
        double range = hummingbirdRange(player);
        Optional<LivingEntity> lookTarget = findLookTarget(player, range);
        if (lookTarget.isPresent()) {
            return lookTarget;
        }
        Optional<LivingEntity> revealed = findNearestRevealedTarget(player, range);
        if (revealed.isPresent()) {
            return revealed;
        }
        return readFootprintTarget(player);
    }

    private static Optional<LivingEntity> readFootprintTarget(ServerPlayer player) {
        int footprintId = data(player).getInt(READ_FOOTPRINT_ID);
        if (footprintId < 0 || data(player).getInt(READ_FOOTPRINT_TICKS) < 20) {
            return Optional.empty();
        }
        long now = SkillCooldownHelper.now(player);
        return FOOTPRINTS.getOrDefault(player.level().dimension(), List.of()).stream()
                .filter(footprint -> footprint.id == footprintId)
                .filter(footprint -> now - footprint.gameTime <= FOOTPRINT_LIFE_TICKS)
                .findFirst()
                .flatMap(footprint -> player.server.getPlayerList().getPlayers().stream()
                        .filter(candidate -> candidate.level() == player.level())
                        .filter(candidate -> candidate.getUUID().toString().equals(footprint.ownerUuid))
                        .filter(candidate -> candidate.isAlive() && TargetingUtil.isTargetablePlayer(candidate))
                        .map(candidate -> (LivingEntity) candidate)
                        .findFirst());
    }

    private static Optional<LivingEntity> findNearestRevealedTarget(ServerPlayer player, double range) {
        AABB box = player.getBoundingBox().inflate(range);
        return player.serverLevel().getEntitiesOfClass(LivingEntity.class, box,
                        target -> target != player
                                && target.isAlive()
                                && TargetingUtil.isTargetableLiving(target)
                                && target.isCurrentlyGlowing())
                .stream()
                .min(Comparator.comparingDouble(player::distanceToSqr));
    }

    private static int hummingbirdAmplifier(Player player) {
        int baseAmplifier = com.rzy.dealt_force_skills.config.DealtForceConfig.intValue(
                "characters.raptor.raptor_state_manager.effect.raptor_hummingbird_marked.0.amplifier", 0);
        return CommanderStateManager.skillEffectAmplifier(player, baseAmplifier);
    }

    private static double hummingbirdRange(Player player) {
        return CommanderStateManager.skillRadius(player, HUMMINGBIRD_RANGE);
    }

    private static Optional<LivingEntity> findLookTarget(ServerPlayer player, double range) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        Vec3 end = eye.add(look.scale(range));
        AABB box = player.getBoundingBox().expandTowards(look.scale(range)).inflate(2.0D);
        return player.serverLevel().getEntitiesOfClass(LivingEntity.class, box,
                        target -> target != player && TargetingUtil.isTargetableLiving(target))
                .stream()
                .filter(target -> {
                    Vec3 center = target.position().add(0.0D, target.getBbHeight() * 0.55D, 0.0D);
                    if (distanceToSegmentSqr(center, eye, end) > 1.15D * 1.15D) {
                        return false;
                    }
                    HitResult result = player.serverLevel().clip(new ClipContext(eye, center, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
                    return result.getType() == HitResult.Type.MISS || result.getLocation().distanceToSqr(center) < 0.6D;
                })
                .min(Comparator.comparingDouble(player::distanceToSqr));
    }

    private static void rechargePulse(ServerPlayer player, long now) {
        CompoundTag tag = data(player);
        int capacity = pulseCapacity(player);
        int charges = Math.min(capacity, Math.max(0, tag.getInt(PULSE_CHARGES)));
        tag.putInt(PULSE_CHARGES, charges);
        if (charges >= capacity) {
            tag.putLong(PULSE_NEXT_RECHARGE, 0L);
            return;
        }
        long next = tag.getLong(PULSE_NEXT_RECHARGE);
        if (next <= 0L) {
            tag.putLong(PULSE_NEXT_RECHARGE, SkillCooldownHelper.until(player, now, PULSE_RECHARGE_TICKS));
            return;
        }
        while (charges < capacity && now >= next) {
            charges++;
            next += SkillCooldownHelper.ticks(player, PULSE_RECHARGE_TICKS);
        }
        tag.putInt(PULSE_CHARGES, charges);
        tag.putLong(PULSE_NEXT_RECHARGE, charges >= capacity ? 0L : next);
    }

    private static void cleanupFootprints(ServerLevel level, long now) {
        List<FootprintState> list = FOOTPRINTS.get(level.dimension());
        if (list == null) {
            return;
        }
        Iterator<FootprintState> iterator = list.iterator();
        while (iterator.hasNext()) {
            if (now - iterator.next().gameTime > FOOTPRINT_LIFE_TICKS) {
                iterator.remove();
            }
        }
        while (list.size() > FOOTPRINT_STORAGE_LIMIT) {
            list.remove(0);
        }
        if (list.isEmpty()) {
            FOOTPRINTS.remove(level.dimension());
        }
    }

    private static String equipmentSummary(ServerPlayer player) {
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        String mainName = main.isEmpty() ? "空手" : main.getHoverName().getString();
        String offName = off.isEmpty() ? "空副手" : off.getHoverName().getString();
        int armor = 0;
        for (ItemStack stack : player.getArmorSlots()) {
            if (!stack.isEmpty()) {
                armor++;
            }
        }
        return mainName + " / " + offName + " / 护甲" + armor;
    }

    private static double distanceToSegmentSqr(Vec3 point, Vec3 start, Vec3 end) {
        Vec3 segment = end.subtract(start);
        double lengthSqr = segment.lengthSqr();
        if (lengthSqr < 0.0001D) {
            return point.distanceToSqr(start);
        }
        double t = point.subtract(start).dot(segment) / lengthSqr;
        t = Math.max(0.0D, Math.min(1.0D, t));
        return point.distanceToSqr(start.add(segment.scale(t)));
    }

    private static int remainingTicks(Player player, String key) {
        return SkillCooldownHelper.remainingTicks(player, data(player).getLong(key));
    }

    private static int pulseCapacity(Player player) {
        return CommanderStateManager.skillCapacity(player, PULSE_MAX_CHARGES);
    }

    private static String secondsText(int ticks) {
        return Math.max(0, ticks / 20) + "s";
    }

    private static String progressBar(int ticks, int required) {
        int filled = Math.max(0, Math.min(10, ticks * 10 / Math.max(1, required)));
        return "[" + "#".repeat(filled) + ".".repeat(10 - filled) + "]";
    }

    private static EntityLookup entityById(ServerLevel level, int entityId) {
        return new EntityLookup(level.getEntity(entityId));
    }

    private static CompoundTag data(Player player) {
        CompoundTag persistent = player.getPersistentData();
        if (!persistent.contains(ROOT_TAG, Tag.TAG_COMPOUND)) {
            persistent.put(ROOT_TAG, new CompoundTag());
        }
        CompoundTag tag = persistent.getCompound(ROOT_TAG);
        if (player instanceof ServerPlayer) {
            SkillCooldownHelper.clampCooldown(player, tag, FALCON_COOLDOWN_UNTIL, FALCON_COOLDOWN_TICKS);
            SkillCooldownHelper.clampCooldown(player, tag, PULSE_NEXT_RECHARGE, PULSE_RECHARGE_TICKS);
            SkillCooldownHelper.clampCooldown(player, tag, HUMMINGBIRD_COOLDOWN_UNTIL, HUMMINGBIRD_COOLDOWN_TICKS);
        }
        return tag;
    }

    private record FootprintState(int id, String ownerUuid, String ownerName, String equipmentSummary, Vec3 position, long gameTime) {
    }

    private record EntityLookup(net.minecraft.world.entity.Entity entity) {
    }

    public record NativeFootprint(int id, java.util.UUID owner, Vec3 position) { }
    public static double nativeFootprintRevealRange() { return FOOTPRINT_READ_REVEAL_RANGE; }
    public static int nativeFootprintRevealTicks() { return FOOTPRINT_READ_REVEAL_TICKS; }

    /** Read the same finite moving-player records as the player passive. */
    public static Optional<NativeFootprint> nativeFootprint(com.rzy.dealt_force_skills.ai.operative.OperativeHost host) {
        long now = SkillCooldownHelper.now(host.body());
        return FOOTPRINTS.getOrDefault(host.body().level().dimension(), List.of()).stream()
                .filter(f -> now >= f.gameTime && now - f.gameTime <= FOOTPRINT_LIFE_TICKS)
                .filter(f -> f.position.distanceToSqr(host.body().position()) <= FOOTPRINT_READ_RANGE * FOOTPRINT_READ_RANGE
                        && host.withinOrders(f.position))
                .filter(f -> {
                    try {
                        return host.serverLevel().getEntity(java.util.UUID.fromString(f.ownerUuid)) instanceof LivingEntity target
                                && host.isEnemy(target) && host.withinOrders(target.position());
                    } catch (IllegalArgumentException ignored) { return false; }
                })
                .filter(f -> host.serverLevel().clip(new ClipContext(host.body().getEyePosition(), f.position.add(0, 0.1D, 0),
                        ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, host.body())).getType() == HitResult.Type.MISS)
                .min(Comparator.comparingDouble(f -> f.position.distanceToSqr(host.body().position())))
                .map(f -> new NativeFootprint(f.id, java.util.UUID.fromString(f.ownerUuid), f.position));
    }

}
