package com.rzy.dealt_force_skills.character.morse;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.CharacterSelectionManager;
import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.character.commander.CommanderStateManager;
import com.rzy.dealt_force_skills.entity.MorseSonarDetectorEntity;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.S2C_MorseMarkers;
import com.rzy.dealt_force_skills.network.S2C_SyncMorseState;
import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.skill.SkillCooldownHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class MorseStateManager {
    public static volatile int SHOCK_MAX_CHARGES = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("SHOCK_MAX_CHARGES", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.morse.morse_state_manager.shock_max_charges", 2));
    public static volatile int SHOCK_RECHARGE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("SHOCK_RECHARGE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.morse.morse_state_manager.shock_recharge_ticks", 800));
    public static volatile int FLASH_MAX_CHARGES = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("FLASH_MAX_CHARGES", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.morse.morse_state_manager.flash_max_charges", 2));
    public static volatile int FLASH_RECHARGE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("FLASH_RECHARGE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.morse.morse_state_manager.flash_recharge_ticks", 800));
    public static volatile int SONAR_COOLDOWN_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("SONAR_COOLDOWN_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.morse.morse_state_manager.sonar_cooldown_ticks", 1500));
    public static volatile int SONAR_DEPLOY_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("SONAR_DEPLOY_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.morse.morse_state_manager.sonar_deploy_ticks", 20));
    private static volatile int SOUND_MARK_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("SOUND_MARK_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.morse.passive.sound_mark_ticks", 40));
    private static volatile int SOUND_MARK_COOLDOWN_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("SOUND_MARK_COOLDOWN_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.morse.passive.sound_mark_cooldown_ticks", 200));
    private static volatile double SOUND_MARK_RANGE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("SOUND_MARK_RANGE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("characters.morse.passive.sound_mark_range", 50.0));
    private static volatile double DEPLOY_RANGE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("DEPLOY_RANGE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("characters.morse.morse_state_manager.deploy_range", 5.0));
    private static volatile double DEPLOY_CANCEL_DISTANCE_SQR = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("DEPLOY_CANCEL_DISTANCE_SQR", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue(
      "characters.morse.morse_state_manager.deploy_cancel_distance_sqr", 2.25
   ));
    private static final String ROOT_TAG = DealtForceSkillsMod.MODID + ".morse";
    private static final String INITIALIZED = "Initialized";
    private static final String SHOCK_CHARGES = "ShockCharges";
    private static final String SHOCK_NEXT_RECHARGE = "ShockNextRecharge";
    private static final String FLASH_CHARGES = "FlashCharges";
    private static final String FLASH_NEXT_RECHARGE = "FlashNextRecharge";
    private static final String SONAR_COOLDOWN_UNTIL = "SonarCooldownUntil";
    private static final String EQUIPPED_TOOL = "EquippedTool";
    private static final String DEPLOYING = "Deploying";
    private static final String DEPLOY_TICKS = "DeployTicks";
    private static final String DEPLOY_START_X = "DeployStartX";
    private static final String DEPLOY_START_Y = "DeployStartY";
    private static final String DEPLOY_START_Z = "DeployStartZ";
    private static final String DEPLOY_TARGET_X = "DeployTargetX";
    private static final String DEPLOY_TARGET_Y = "DeployTargetY";
    private static final String DEPLOY_TARGET_Z = "DeployTargetZ";
    private static final String DEPLOY_YAW = "DeployYaw";
    private static final String SONAR_HUD_ACTIVE = "SonarHudActive";
    private static final String SONAR_HUD_SCANNING = "SonarHudScanning";
    private static final String SONAR_HUD_REMAINING = "SonarHudRemaining";
    private static final String SONAR_HUD_COUNT = "SonarHudCount";
    private static final String SONAR_HUD_EXPIRES = "SonarHudExpires";

    private static final Map<SoundExposureKey, Long> LAST_SOUND_MARK = new HashMap<>();
    private static final Map<UUID, ActionSnapshot> ACTION_SNAPSHOTS = new HashMap<>();

    private MorseStateManager() {
    }

    public static boolean isMorse(Player player) {
        Optional<String> selected = CharacterSelectionManager.getSelectedCharacterId(player);
        return selected.isPresent() && ModCharacters.MORSE_ID.equals(selected.get())
                || CommanderStateManager.inheritsRuntimeSource(player, ModCharacters.MORSE_ID);
    }

    public static void initializeIfNeeded(ServerPlayer player) {
        if (!isMorse(player)) {
            return;
        }
        CompoundTag tag = data(player);
        if (tag.getBoolean(INITIALIZED)) {
            return;
        }
        tag.putBoolean(INITIALIZED, true);
        tag.putInt(SHOCK_CHARGES, shockCapacity(player));
        tag.putLong(SHOCK_NEXT_RECHARGE, 0L);
        tag.putInt(FLASH_CHARGES, flashCapacity(player));
        tag.putLong(FLASH_NEXT_RECHARGE, 0L);
        tag.putLong(SONAR_COOLDOWN_UNTIL, 0L);
        tag.putInt(EQUIPPED_TOOL, MorseTool.NONE.ordinal());
    }

    public static void copyState(Player original, Player target) {
        CompoundTag originalData = original.getPersistentData();
        if (originalData.contains(ROOT_TAG, Tag.TAG_COMPOUND)) {
            target.getPersistentData().put(ROOT_TAG, originalData.getCompound(ROOT_TAG).copy());
        }
    }

    public static void clearState(Player player) {
        player.getPersistentData().remove(ROOT_TAG);
        ACTION_SNAPSHOTS.remove(player.getUUID());
        clearSoundExposureCooldowns(player.getUUID());
    }

    public static void tick(ServerPlayer player) {
        if (!isMorse(player)) {
            return;
        }
        initializeIfNeeded(player);
        long now = SkillCooldownHelper.now(player);
        recharge(player, now, SHOCK_CHARGES, SHOCK_NEXT_RECHARGE, shockCapacity(player), SHOCK_RECHARGE_TICKS);
        recharge(player, now, FLASH_CHARGES, FLASH_NEXT_RECHARGE, flashCapacity(player), FLASH_RECHARGE_TICKS);
        tickSonarDeploy(player, now);
        clearExpiredSonarHud(player, now);
    }

    public static void recordObservedPlayerActivity(ServerPlayer player) {
        Vec3 pos = player.position();
        ActionSnapshot previous = ACTION_SNAPSHOTS.put(player.getUUID(), new ActionSnapshot(
                pos,
                player.getInventory().selected,
                player.isUsingItem(),
                player.isSprinting(),
                player.isCrouching(),
                player.containerMenu != player.inventoryMenu
        ));
        if (previous == null) {
            return;
        }
        boolean moved = previous.position.distanceToSqr(pos) > 0.0064D;
        boolean changed = previous.selectedSlot != player.getInventory().selected
                || previous.usingItem != player.isUsingItem()
                || previous.sprinting != player.isSprinting()
                || previous.crouching != player.isCrouching()
                || previous.containerOpen != (player.containerMenu != player.inventoryMenu);
        if (moved || changed) {
            recordPlayerAction(player);
        }
    }

    public static void recordPlayerAction(ServerPlayer player) {
        onPlayerSound(player, player.position());
        MorseSonarDetectorEntity.revealActingPlayer(player);
    }

    public static void onPlayerSound(ServerPlayer source, Vec3 position) {
        if (!(source.level() instanceof ServerLevel level)) {
            return;
        }
        long now = level.getGameTime();
        pruneSoundExposureCooldowns(now);
        double rangeSqr = SOUND_MARK_RANGE * SOUND_MARK_RANGE;
        for (ServerPlayer viewer : level.players()) {
            if (viewer == source || !isMorse(viewer) || viewer.distanceToSqr(position) > rangeSqr) {
                continue;
            }
            SoundExposureKey cooldownKey = new SoundExposureKey(source.getUUID(), viewer.getUUID());
            Long last = LAST_SOUND_MARK.get(cooldownKey);
            if (last != null && now - last < SOUND_MARK_COOLDOWN_TICKS) {
                continue;
            }
            LAST_SOUND_MARK.put(cooldownKey, now);
            Vec3 markerPos = noisySoundPosition(level, position);
            NetworkHandler.sendToPlayer(new S2C_MorseMarkers(List.of(
                    new MorseWorldMarker(MorseMarkerType.SOUND_SOURCE, -1, markerPos, SOUND_MARK_TICKS)
            )), viewer);
            viewer.displayClientMessage(Component.translatable("message.dealt_force_skills.morse.sound_direction",
                    roughSoundDirection(viewer, markerPos)), true);
            viewer.connection.send(new ClientboundSoundPacket(
                    Holder.direct(ModSounds.MORSE_ALERT_HEARING_PING.get()),
                    SoundSource.PLAYERS,
                    markerPos.x,
                    markerPos.y,
                    markerPos.z,
                    0.55f,
                    1.0f,
                    level.getRandom().nextLong()
            ));
        }
    }

    public static MorseTool equippedTool(Player player) {
        int ordinal = data(player).getInt(EQUIPPED_TOOL);
        MorseTool[] tools = MorseTool.values();
        return ordinal >= 0 && ordinal < tools.length ? tools[ordinal] : MorseTool.NONE;
    }

    public static void setEquippedTool(Player player, MorseTool tool) {
        data(player).putInt(EQUIPPED_TOOL, tool.ordinal());
        if (tool != MorseTool.SONAR_DETECTOR) {
            cancelSonarDeploy(player);
        }
    }

    public static boolean consumeShockCharge(ServerPlayer player) {
        return consumeCharge(player, SHOCK_CHARGES, SHOCK_NEXT_RECHARGE, shockCapacity(player), SHOCK_RECHARGE_TICKS);
    }

    public static boolean consumeFlashCharge(ServerPlayer player) {
        return consumeCharge(player, FLASH_CHARGES, FLASH_NEXT_RECHARGE, flashCapacity(player), FLASH_RECHARGE_TICKS);
    }

    public static boolean beginSonarDeploy(ServerPlayer player) {
        if (equippedTool(player) != MorseTool.SONAR_DETECTOR) {
            return false;
        }
        if (sonarCooldownRemainingTicks(player) > 0 || MorseSonarDetectorEntity.activeFor(player) != null) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.morse.sonar_unavailable"), true);
            return true;
        }
        DeployTarget target = findDeployTarget(player);
        if (target == null) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.morse.sonar_bad_surface"), true);
            return true;
        }
        CompoundTag tag = data(player);
        tag.putBoolean(DEPLOYING, true);
        tag.putInt(DEPLOY_TICKS, 0);
        Vec3 start = player.position();
        tag.putDouble(DEPLOY_START_X, start.x);
        tag.putDouble(DEPLOY_START_Y, start.y);
        tag.putDouble(DEPLOY_START_Z, start.z);
        tag.putDouble(DEPLOY_TARGET_X, target.position.x);
        tag.putDouble(DEPLOY_TARGET_Y, target.position.y);
        tag.putDouble(DEPLOY_TARGET_Z, target.position.z);
        tag.putFloat(DEPLOY_YAW, player.getYRot());
        player.level().playSound(null, player.blockPosition(), ModSounds.MORSE_SONAR_DEPLOYING.get(),
                SoundSource.PLAYERS, 0.7f, 1.0f);
        return true;
    }

    public static void finishSonar(ServerPlayer owner) {
        if (owner == null) {
            return;
        }
        long now = SkillCooldownHelper.now(owner);
        CompoundTag tag = data(owner);
        tag.putLong(SONAR_COOLDOWN_UNTIL, SkillCooldownHelper.until(owner, now, SONAR_COOLDOWN_TICKS));
        tag.putBoolean(SONAR_HUD_ACTIVE, false);
        tag.putBoolean(SONAR_HUD_SCANNING, false);
        tag.putInt(SONAR_HUD_REMAINING, 0);
        tag.putInt(SONAR_HUD_COUNT, 0);
        tag.putLong(SONAR_HUD_EXPIRES, 0L);
        if (equippedTool(owner) == MorseTool.SONAR_DETECTOR) {
            setEquippedTool(owner, MorseTool.NONE);
        }
        syncToClient(owner);
    }

    public static void updateSonarHud(ServerPlayer owner, boolean scanning, int remainingTicks, int targetCount) {
        CompoundTag tag = data(owner);
        tag.putBoolean(SONAR_HUD_ACTIVE, true);
        tag.putBoolean(SONAR_HUD_SCANNING, scanning);
        tag.putInt(SONAR_HUD_REMAINING, Math.max(0, remainingTicks));
        tag.putInt(SONAR_HUD_COUNT, Math.max(0, targetCount));
        tag.putLong(SONAR_HUD_EXPIRES, SkillCooldownHelper.now(owner) + 15L);
    }

    public static int shockCharges(Player player) {
        return Math.min(shockCapacity(player), data(player).getInt(SHOCK_CHARGES));
    }

    public static int shockRechargeRemainingTicks(Player player) {
        return shockCharges(player) >= shockCapacity(player) ? 0 : remainingTicks(player, SHOCK_NEXT_RECHARGE);
    }

    public static int flashCharges(Player player) {
        return Math.min(flashCapacity(player), data(player).getInt(FLASH_CHARGES));
    }

    public static int flashRechargeRemainingTicks(Player player) {
        return flashCharges(player) >= flashCapacity(player) ? 0 : remainingTicks(player, FLASH_NEXT_RECHARGE);
    }

    public static int sonarCooldownRemainingTicks(Player player) {
        return remainingTicks(player, SONAR_COOLDOWN_UNTIL);
    }

    public static void clearRuntimeOnDeath(ServerPlayer player) {
        cancelSonarDeploy(player);
        setEquippedTool(player, MorseTool.NONE);
        MorseSonarDetectorEntity.discardFor(player);
        ACTION_SNAPSHOTS.remove(player.getUUID());
        clearSoundExposureCooldowns(player.getUUID());
    }

    public static void clearRuntimeOnLogout(ServerPlayer player) {
        if (player == null) {
            return;
        }
        MorseSonarDetectorEntity.discardFor(player);
        ACTION_SNAPSHOTS.remove(player.getUUID());
        clearSoundExposureCooldowns(player.getUUID());
        if (player.getPersistentData().contains(ROOT_TAG, Tag.TAG_COMPOUND)) {
            cancelSonarDeploy(player);
            setEquippedTool(player, MorseTool.NONE);
        }
    }

    public static void syncToClient(ServerPlayer player) {
        if (!isMorse(player)) {
            return;
        }
        initializeIfNeeded(player);
        CompoundTag tag = data(player);
        NetworkHandler.sendToPlayer(new S2C_SyncMorseState(
                shockCharges(player),
                shockCapacity(player),
                shockRechargeRemainingTicks(player),
                flashCharges(player),
                flashCapacity(player),
                flashRechargeRemainingTicks(player),
                sonarCooldownRemainingTicks(player),
                equippedTool(player).ordinal(),
                tag.getBoolean(DEPLOYING) ? tag.getInt(DEPLOY_TICKS) : 0,
                SONAR_DEPLOY_TICKS,
                tag.getBoolean(SONAR_HUD_ACTIVE),
                tag.getBoolean(SONAR_HUD_SCANNING),
                tag.getInt(SONAR_HUD_REMAINING),
                tag.getInt(SONAR_HUD_COUNT)
        ), player);
    }

    private static void tickSonarDeploy(ServerPlayer player, long now) {
        CompoundTag tag = data(player);
        if (!tag.getBoolean(DEPLOYING)) {
            return;
        }
        if (equippedTool(player) != MorseTool.SONAR_DETECTOR
                || player.hasEffect(ModEffects.STUN)
                || player.hasEffect(ModEffects.WEBBED)
                || player.position().distanceToSqr(new Vec3(tag.getDouble(DEPLOY_START_X),
                tag.getDouble(DEPLOY_START_Y), tag.getDouble(DEPLOY_START_Z))) > DEPLOY_CANCEL_DISTANCE_SQR) {
            cancelSonarDeploy(player);
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.morse.sonar_deploy_cancelled"), true);
            return;
        }
        int ticks = tag.getInt(DEPLOY_TICKS) + 1;
        tag.putInt(DEPLOY_TICKS, ticks);
        player.displayClientMessage(Component.translatable("message.dealt_force_skills.morse.sonar_deploying",
                progressBar(ticks, SONAR_DEPLOY_TICKS)), true);
        if (ticks < SONAR_DEPLOY_TICKS) {
            return;
        }
        ServerLevel level = player.serverLevel();
        Vec3 target = new Vec3(tag.getDouble(DEPLOY_TARGET_X), tag.getDouble(DEPLOY_TARGET_Y), tag.getDouble(DEPLOY_TARGET_Z));
        Vec3 facing = Vec3.directionFromRotation(0.0F, tag.getFloat(DEPLOY_YAW));
        MorseSonarDetectorEntity detector = new MorseSonarDetectorEntity(
                com.rzy.dealt_force_skills.registry.ModEntities.MORSE_SONAR_DETECTOR.get(), level, player, facing);
        detector.setPos(target.x, target.y, target.z);
        detector.setYRot(tag.getFloat(DEPLOY_YAW));
        level.addFreshEntity(detector);
        tag.putBoolean(DEPLOYING, false);
        tag.putInt(DEPLOY_TICKS, 0);
        setEquippedTool(player, MorseTool.NONE);
        level.playSound(null, target.x, target.y, target.z, ModSounds.MORSE_SONAR_DEPLOY_COMPLETE.get(),
                SoundSource.PLAYERS, 0.9f, 1.0f);
    }

    private static void cancelSonarDeploy(Player player) {
        CompoundTag tag = data(player);
        tag.putBoolean(DEPLOYING, false);
        tag.putInt(DEPLOY_TICKS, 0);
    }

    private static DeployTarget findDeployTarget(ServerPlayer player) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(DEPLOY_RANGE));
        HitResult hit = player.serverLevel().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK || blockHit.getDirection() != Direction.UP) {
            return null;
        }
        BlockPos ground = blockHit.getBlockPos();
        BlockPos place = ground.above();
        BlockState placeState = player.serverLevel().getBlockState(place);
        if (!placeState.isAir()) {
            return null;
        }
        return new DeployTarget(Vec3.atBottomCenterOf(place).add(0.0D, 0.05D, 0.0D));
    }

    private static void clearExpiredSonarHud(ServerPlayer player, long now) {
        CompoundTag tag = data(player);
        if (tag.getBoolean(SONAR_HUD_ACTIVE) && now > tag.getLong(SONAR_HUD_EXPIRES)) {
            tag.putBoolean(SONAR_HUD_ACTIVE, false);
            tag.putBoolean(SONAR_HUD_SCANNING, false);
            tag.putInt(SONAR_HUD_REMAINING, 0);
            tag.putInt(SONAR_HUD_COUNT, 0);
        }
    }

    private static boolean consumeCharge(ServerPlayer player, String chargeKey, String rechargeKey, int maxCharges, int rechargeTicks) {
        CompoundTag tag = data(player);
        int charges = tag.getInt(chargeKey);
        if (charges <= 0) {
            return false;
        }
        tag.putInt(chargeKey, charges - 1);
        if (charges - 1 < maxCharges && tag.getLong(rechargeKey) <= 0L) {
            tag.putLong(rechargeKey, SkillCooldownHelper.until(player, SkillCooldownHelper.now(player), rechargeTicks));
        }
        return true;
    }

    private static void recharge(ServerPlayer player, long now, String chargeKey, String rechargeKey, int maxCharges, int rechargeTicks) {
        CompoundTag tag = data(player);
        int charges = tag.getInt(chargeKey);
        if (charges >= maxCharges) {
            tag.putLong(rechargeKey, 0L);
            return;
        }
        long next = tag.getLong(rechargeKey);
        if (next <= 0L) {
            tag.putLong(rechargeKey, SkillCooldownHelper.until(player, now, rechargeTicks));
            return;
        }
        while (charges < maxCharges && now >= next) {
            charges++;
            next += SkillCooldownHelper.ticks(player, rechargeTicks);
        }
        tag.putInt(chargeKey, charges);
        tag.putLong(rechargeKey, charges >= maxCharges ? 0L : next);
    }

    private static int remainingTicks(Player player, String key) {
        return SkillCooldownHelper.remainingTicks(player, data(player).getLong(key));
    }

    private static int shockCapacity(Player player) {
        return CommanderStateManager.skillCapacity(player, SHOCK_MAX_CHARGES);
    }

    private static int flashCapacity(Player player) {
        return CommanderStateManager.skillCapacity(player, FLASH_MAX_CHARGES);
    }

    private static Vec3 noisySoundPosition(ServerLevel level, Vec3 position) {
        double distance = 1.0D + level.getRandom().nextDouble() * 2.0D;
        double angle = level.getRandom().nextDouble() * Math.PI * 2.0D;
        double y = (level.getRandom().nextDouble() - 0.5D) * 0.75D;
        return position.add(Math.cos(angle) * distance, y, Math.sin(angle) * distance);
    }

    private static Component roughSoundDirection(ServerPlayer viewer, Vec3 position) {
        Vec3 delta = position.subtract(viewer.position());
        Vec3 horizontal = new Vec3(delta.x, 0.0D, delta.z);
        if (horizontal.lengthSqr() < 0.0001D) {
            return Component.translatable("message.dealt_force_skills.morse.direction.near");
        }
        horizontal = horizontal.normalize();
        Vec3 look = viewer.getLookAngle();
        Vec3 forward = new Vec3(look.x, 0.0D, look.z);
        if (forward.lengthSqr() < 0.0001D) {
            forward = Vec3.directionFromRotation(0.0F, viewer.getYRot());
            forward = new Vec3(forward.x, 0.0D, forward.z);
        }
        forward = forward.normalize();
        Vec3 right = new Vec3(-forward.z, 0.0D, forward.x);
        double front = horizontal.dot(forward);
        double side = horizontal.dot(right);
        String suffix;
        if (front > 0.72D) {
            suffix = side > 0.38D ? "front_right" : side < -0.38D ? "front_left" : "front";
        } else if (front < -0.72D) {
            suffix = side > 0.38D ? "back_right" : side < -0.38D ? "back_left" : "back";
        } else {
            suffix = side >= 0.0D ? "right" : "left";
        }
        int distance = Math.max(1, Mth.floor(Math.sqrt(delta.x * delta.x + delta.z * delta.z)));
        return Component.translatable("message.dealt_force_skills.morse.direction." + suffix, distance);
    }

    private static void clearSoundExposureCooldowns(UUID playerId) {
        LAST_SOUND_MARK.keySet().removeIf(key -> key.source().equals(playerId) || key.viewer().equals(playerId));
    }

    private static void pruneSoundExposureCooldowns(long now) {
        LAST_SOUND_MARK.entrySet().removeIf(entry -> now - entry.getValue() >= SOUND_MARK_COOLDOWN_TICKS);
    }

    private static String progressBar(int ticks, int required) {
        int filled = Math.max(0, Math.min(10, ticks * 10 / Math.max(1, required)));
        return "[" + "#".repeat(filled) + ".".repeat(10 - filled) + "]";
    }

    private static CompoundTag data(Player player) {
        CompoundTag persistent = player.getPersistentData();
        if (!persistent.contains(ROOT_TAG, Tag.TAG_COMPOUND)) {
            persistent.put(ROOT_TAG, new CompoundTag());
        }
        return persistent.getCompound(ROOT_TAG);
    }

    private record ActionSnapshot(Vec3 position, int selectedSlot, boolean usingItem, boolean sprinting,
                                  boolean crouching, boolean containerOpen) {
    }

    private record SoundExposureKey(UUID source, UUID viewer) {
    }

    private record DeployTarget(Vec3 position) {
    }
}
