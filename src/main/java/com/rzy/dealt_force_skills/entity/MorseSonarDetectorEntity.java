package com.rzy.dealt_force_skills.entity;

import com.rzy.dealt_force_skills.ai.operative.OperativeAllegiance;
import com.rzy.dealt_force_skills.ai.operative.OperativeHost;
import com.rzy.dealt_force_skills.ai.operative.OperativeRecon;
import com.rzy.dealt_force_skills.ai.operative.NativeMorseSkills;
import com.rzy.dealt_force_skills.ai.operative.OperativeElectronics;
import com.rzy.dealt_force_skills.ai.operative.OperativeSkillSource;
import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.character.SkillSlot;
import com.rzy.dealt_force_skills.advancement.DfsAchievements;
import com.rzy.dealt_force_skills.character.morse.MorseMarkerType;
import com.rzy.dealt_force_skills.character.morse.MorseStateManager;
import com.rzy.dealt_force_skills.character.morse.MorseWorldMarker;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.S2C_MorseMarkers;
import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.util.ReconRevealThrottle;
import com.rzy.dealt_force_skills.util.TargetingUtil;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MorseSonarDetectorEntity extends Entity implements ItemSupplier, BlockbenchModelPoseProvider, OperativeSkillSource {
    private static final EntityDataAccessor<Float> DATA_FACING_X =
            SynchedEntityData.defineId(MorseSonarDetectorEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_FACING_Z =
            SynchedEntityData.defineId(MorseSonarDetectorEntity.class, EntityDataSerializers.FLOAT);
    public static volatile double RANGE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("RANGE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.morsesonardetectorentity.range", 75.0));
    private static final double HALF_ANGLE_COS = Math.cos(Math.toRadians(
            com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue(
                    "summons.morse_sonar_detector.half_angle_degrees", 55.0D)));
    private static volatile int SCAN_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("SCAN_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.morsesonardetectorentity.scan_ticks", 80));
    private static volatile int IDLE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("IDLE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.morsesonardetectorentity.idle_ticks", 120));
    private static volatile int CYCLE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("CYCLE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.morsesonardetectorentity.cycle_ticks", SCAN_TICKS + IDLE_TICKS));
    private static volatile int TOTAL_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("TOTAL_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.morsesonardetectorentity.total_ticks", SCAN_TICKS * 3 + IDLE_TICKS * 2));
    private static volatile int REVEAL_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("REVEAL_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.morsesonardetectorentity.reveal_ticks", 45));
    private static volatile int DETECTOR_MARKER_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("DETECTOR_MARKER_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.morsesonardetectorentity.detector_marker_ticks", 18));
    private static final DustParticleOptions SONAR_DUST = new DustParticleOptions(new Vector3f(0.28f, 0.72f, 1.0f), 1.0f);
    private static final Map<UUID, MorseSonarDetectorEntity> ACTIVE = new HashMap<>();

    private final OperativeAllegiance nativeAllegiance = new OperativeAllegiance();
    private int missingOwnerTicks;
    private UUID ownerId;
    private Vec3 facing = new Vec3(0.0D, 0.0D, 1.0D);
    private int elapsedTicks;
    private boolean finished;

    public MorseSonarDetectorEntity(EntityType<? extends MorseSonarDetectorEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public MorseSonarDetectorEntity(EntityType<? extends MorseSonarDetectorEntity> type, Level level, LivingEntity owner, Vec3 facing) {
        this(type, level);
        ownerId = owner.getUUID();
        nativeAllegiance.capture(owner);
        setFacing(facing);
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(Items.SCULK_SENSOR);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(DATA_FACING_X, 0.0F);
        entityData.define(DATA_FACING_Z, 1.0F);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && nativeAllegiance.active()) { nativeTick(); return; }
        if (level().isClientSide) {
            if (tickCount % 6 == 0) {
                level().addParticle(SONAR_DUST, getX(), getY() + 0.18D, getZ(), 0.0D, 0.03D, 0.0D);
            }
            return;
        }
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        ServerPlayer owner = owner(level);
        if (owner == null || !owner.isAlive()) {
            discard();
            return;
        }
        ACTIVE.put(owner.getUUID(), this);
        elapsedTicks++;
        boolean scanning = isScanning();
        int remaining = phaseRemainingTicks();
        int targetCount = countTargets(level, owner, scanning);
        MorseStateManager.updateSonarHud(owner, scanning, remaining, targetCount);

        if (scanning && elapsedTicks % 20 == 1) {
            level.playSound(null, blockPosition(), ModSounds.MORSE_SONAR_SCAN_START.get(),
                    SoundSource.PLAYERS, 0.45f, 0.95f + Math.min(0.2f, elapsedTicks / 500.0f));
        } else if (!scanning && elapsedTicks % 40 == 0) {
            level.playSound(null, blockPosition(), ModSounds.MORSE_SONAR_IDLE_TICK.get(),
                    SoundSource.PLAYERS, 0.35f, 1.0f);
        }
        if (elapsedTicks % 10 == 0) {
            broadcastDetectorMarker(level);
            revealNonPlayers(level, owner);
            notifyPlayersInCone(level, owner, scanning, remaining);
        }
        if (elapsedTicks >= TOTAL_TICKS) {
            destroy(false);
        }
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (nativeAllegiance.active() && level() instanceof ServerLevel level
                && source.getEntity() != null && nativeAllegiance.isAlly(level, source.getEntity())) return false;
        if (!level().isClientSide) {
            destroy(true);
        }
        return true;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        nativeAllegiance.load(tag, Math.max(1, TOTAL_TICKS));
        missingOwnerTicks = 0;
        ownerId = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        setFacing(new Vec3(tag.getDouble("FacingX"), 0.0D, tag.getDouble("FacingZ")));
        elapsedTicks = tag.getInt("ElapsedTicks");
        if (nativeAllegiance.active()) elapsedTicks = Math.max(0, Math.min(Math.max(1, TOTAL_TICKS), elapsedTicks));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        nativeAllegiance.save(tag, elapsedTicks);
        if (ownerId != null) {
            tag.putUUID("Owner", ownerId);
        }
        tag.putDouble("FacingX", facing.x);
        tag.putDouble("FacingZ", facing.z);
        tag.putInt("ElapsedTicks", elapsedTicks);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public void remove(RemovalReason reason) {
        if (nativeAllegiance.active()) {
            if (ownerId != null) ACTIVE.remove(ownerId, this);
            if (!level().isClientSide && reason.shouldDestroy()) nativeFinish();
            super.remove(reason);
            return;
        }
        if (!level().isClientSide && ownerId != null) {
            MorseSonarDetectorEntity active = ACTIVE.get(ownerId);
            if (active == this) {
                ACTIVE.remove(ownerId);
            }
            if (!finished && level() instanceof ServerLevel level) {
                MorseStateManager.finishSonar(owner(level));
            }
        }
        super.remove(reason);
    }

    public void destroyByInterference() {
        destroy(true);
    }

    public int remainingLifeTicks() {
        return Math.max(0, TOTAL_TICKS - elapsedTicks);
    }

    @Override
    public Vec3 blockbenchModelForward(float partialTick) {
        return horizontalFacing(new Vec3(entityData.get(DATA_FACING_X), 0.0D, entityData.get(DATA_FACING_Z)));
    }

    public static MorseSonarDetectorEntity activeFor(ServerPlayer owner) {
        MorseSonarDetectorEntity detector = ACTIVE.get(owner.getUUID());
        if (detector == null || detector.isRemoved()) {
            ACTIVE.remove(owner.getUUID());
            return null;
        }
        return detector;
    }

    public static void discardFor(ServerPlayer owner) {
        MorseSonarDetectorEntity detector = activeFor(owner);
        if (detector != null) {
            detector.discard();
        }
    }

    public static void revealActingPlayer(ServerPlayer actor) {
        revealNativeActingPlayer(actor);
        if (!(actor.level() instanceof ServerLevel level) || ACTIVE.isEmpty() || !TargetingUtil.isTargetablePlayer(actor)) {
            return;
        }
        for (MorseSonarDetectorEntity detector : List.copyOf(ACTIVE.values())) {
            if (detector == null || detector.isRemoved() || detector.level() != level || !detector.isScanning()) {
                continue;
            }
            ServerPlayer owner = detector.owner(level);
            if (owner == null || owner == actor || !detector.inCone(actor)) {
                continue;
            }
            // Recon must not treat teammates as revealed hostiles.
            if (!com.rzy.dealt_force_skills.util.PositionRevealHelper.isValidReconTarget(owner, actor)) {
                continue;
            }
            if (!ReconRevealThrottle.tryStart(actor, REVEAL_TICKS)) {
                continue;
            }
            com.rzy.dealt_force_skills.util.PositionRevealHelper.sendToCasterAndTeammates(owner,
                    new S2C_MorseMarkers(List.of(
                            new MorseWorldMarker(MorseMarkerType.SONAR_REVEAL, actor.getId(), actor.position(), REVEAL_TICKS)
                    )));
            actor.addEffect(new MobEffectInstance(ModEffects.MORSE_SONAR_REVEALED.get(),
                    com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.morse_sonar_detector_entity.effect.morse_sonar_revealed.0.duration_ticks", 2 * 20), com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.morse_sonar_detector_entity.effect.morse_sonar_revealed.0.amplifier", 0), false, true, true), owner);
            DfsAchievements.recordMorseSonarActionReveal(owner, actor);
            owner.displayClientMessage(Component.translatable("message.dealt_force_skills.morse.sonar_action_owner",
                    actor.getDisplayName()), true);
            actor.displayClientMessage(Component.translatable("message.dealt_force_skills.morse.sonar_action_target"), true);
            level.playSound(null, detector.blockPosition(), ModSounds.MORSE_SONAR_ACTION_REVEAL.get(),
                    SoundSource.PLAYERS, 0.7f, 1.0f);
        }
    }

    private void destroy(boolean playSound) {
        if (nativeAllegiance.active()) {
            if (playSound && level() instanceof ServerLevel level) level.playSound(null, blockPosition(), ModSounds.MORSE_SONAR_DESTROYED.get(), SoundSource.NEUTRAL, 0.8F, 1);
            discard();
            return;
        }
        if (level() instanceof ServerLevel level && playSound) {
            level.playSound(null, blockPosition(), ModSounds.MORSE_SONAR_DESTROYED.get(),
                    SoundSource.PLAYERS, 0.8f, 1.0f);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + 0.2D, getZ(),
                    24, 0.35D, 0.18D, 0.35D, 0.08D);
        }
        finished = true;
        if (level() instanceof ServerLevel level) {
            MorseStateManager.finishSonar(owner(level));
        }
        discard();
    }

    private void revealNonPlayers(ServerLevel level, ServerPlayer owner) {
        if (!isScanning()) {
            return;
        }
        AABB box = new AABB(position(), position()).inflate(RANGE);
        List<MorseWorldMarker> markers = level.getEntitiesOfClass(LivingEntity.class, box,
                        target -> !(target instanceof Player) && TargetingUtil.isTargetableLiving(target) && inCone(target))
                .stream()
                .filter(target -> ReconRevealThrottle.tryStart(target, REVEAL_TICKS))
                .map(target -> new MorseWorldMarker(MorseMarkerType.SONAR_REVEAL, target.getId(), target.position(), REVEAL_TICKS))
                .toList();
        if (!markers.isEmpty()) {
            com.rzy.dealt_force_skills.department.DepartmentTasks.signal(owner,"morse.sonar.detect");
            com.rzy.dealt_force_skills.util.PositionRevealHelper.sendToCasterAndTeammates(
                    owner, new S2C_MorseMarkers(markers));
        }
    }

    private void broadcastDetectorMarker(ServerLevel level) {
        S2C_MorseMarkers packet = new S2C_MorseMarkers(List.of(
                new MorseWorldMarker(MorseMarkerType.SONAR_DETECTOR, -1, position(), DETECTOR_MARKER_TICKS)
        ));
        for (ServerPlayer player : level.players()) {
            NetworkHandler.sendToPlayer(packet, player);
        }
    }

    private int countTargets(ServerLevel level, ServerPlayer owner, boolean scanning) {
        if (!scanning) {
            return 0;
        }
        AABB box = new AABB(position(), position()).inflate(RANGE);
        return level.getEntitiesOfClass(LivingEntity.class, box,
                target -> target != owner && TargetingUtil.isTargetableLiving(target) && inCone(target)).size();
    }

    private void notifyPlayersInCone(ServerLevel level, ServerPlayer owner, boolean scanning, int remaining) {
        AABB box = new AABB(position(), position()).inflate(RANGE);
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, box,
                target -> target != owner && TargetingUtil.isTargetablePlayer(target) && inCone(target))) {
            player.displayClientMessage(Component.translatable(scanning
                            ? "message.dealt_force_skills.morse.sonar_target_scanning"
                            : "message.dealt_force_skills.morse.sonar_target_idle",
                    Math.max(1, (remaining + 19) / 20)), true);
        }
    }

    private boolean isScanning() {
        if (elapsedTicks < SCAN_TICKS) {
            return true;
        }
        if (elapsedTicks >= CYCLE_TICKS && elapsedTicks < CYCLE_TICKS + SCAN_TICKS) {
            return true;
        }
        return elapsedTicks >= CYCLE_TICKS * 2 && elapsedTicks < CYCLE_TICKS * 2 + SCAN_TICKS;
    }

    private int phaseRemainingTicks() {
        if (elapsedTicks < SCAN_TICKS) {
            return SCAN_TICKS - elapsedTicks;
        }
        if (elapsedTicks < CYCLE_TICKS) {
            return CYCLE_TICKS - elapsedTicks;
        }
        if (elapsedTicks < CYCLE_TICKS + SCAN_TICKS) {
            return CYCLE_TICKS + SCAN_TICKS - elapsedTicks;
        }
        if (elapsedTicks < CYCLE_TICKS * 2) {
            return CYCLE_TICKS * 2 - elapsedTicks;
        }
        return Math.max(0, TOTAL_TICKS - elapsedTicks);
    }

    private boolean inCone(LivingEntity target) {
        Vec3 center = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
        Vec3 toTarget = center.subtract(position());
        double distance = toTarget.length();
        if (distance > RANGE || distance < 0.001D) {
            return false;
        }
        Vec3 horizontal = new Vec3(toTarget.x, 0.0D, toTarget.z);
        if (horizontal.lengthSqr() < 0.001D) {
            return true;
        }
        return facing.dot(horizontal.normalize()) >= HALF_ANGLE_COS;
    }

    private ServerPlayer owner(ServerLevel level) {
        if (ownerId == null) {
            return null;
        }
        return level.getServer().getPlayerList().getPlayer(ownerId);
    }

    private static Vec3 horizontalFacing(Vec3 input) {
        Vec3 horizontal = new Vec3(input.x, 0.0D, input.z);
        return horizontal.lengthSqr() < 0.0001D ? new Vec3(0.0D, 0.0D, 1.0D) : horizontal.normalize();
    }

    private void setFacing(Vec3 input) {
        facing = horizontalFacing(input);
        entityData.set(DATA_FACING_X, (float) facing.x);
        entityData.set(DATA_FACING_Z, (float) facing.z);
    }

    @Override public UUID operativeOwnerUUID() { return nativeAllegiance.owner(); }
    public boolean isNativeOwnedBy(LivingEntity owner) { return nativeAllegiance.active() && nativeAllegiance.isCaster(owner); }
    private OperativeHost nativeHost(ServerLevel level) {
        if (ownerId == null) return null;
        if (!(level.getEntity(ownerId) instanceof OperativeHost host)
                || !(com.rzy.dealt_force_skills.ai.operative.NativeCommanderSkills.inherited(host.skills(), NativeMorseSkills.class) instanceof NativeMorseSkills skills)
                || !skills.ownsSonar(getUUID()) || !NativeMorseSkills.available(host)
                || !host.withinOrders(position()) || OperativeElectronics.blocked(host, ModCharacters.MORSE_ID, SkillSlot.CORE)) return null;
        return host;
    }
    private void nativeTick() {
        if (!(level() instanceof ServerLevel level)) return;
        OperativeHost host = nativeHost(level);
        if (host == null) {
            // Loading order may temporarily hide the caster; do not force-load its chunk.
            if (ownerId == null || level.getEntity(ownerId) != null || ++missingOwnerTicks > 40) { destroy(false); return; }
        } else missingOwnerTicks = 0;
        ACTIVE.put(ownerId, this);
        if (++elapsedTicks >= Math.max(1, TOTAL_TICKS)) { destroy(false); return; }
        if (host == null) return;
        boolean scanning = isScanning();
        if (scanning && elapsedTicks % 20 == 1) level.playSound(null, blockPosition(), ModSounds.MORSE_SONAR_SCAN_START.get(), SoundSource.NEUTRAL, 0.45F, 1);
        else if (!scanning && elapsedTicks % 40 == 0) level.playSound(null, blockPosition(), ModSounds.MORSE_SONAR_IDLE_TICK.get(), SoundSource.NEUTRAL, 0.35F, 1);
        if (elapsedTicks % 10 != 0) return;
        broadcastDetectorMarker(level);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(position(), position()).inflate(RANGE))) {
            if (!host.isEnemy(target) || !host.withinOrders(target.position()) || !inCone(target)) continue;
            if (target instanceof ServerPlayer player) {
                player.displayClientMessage(Component.translatable(scanning ? "message.dealt_force_skills.morse.sonar_target_scanning"
                        : "message.dealt_force_skills.morse.sonar_target_idle", Math.max(1, (phaseRemainingTicks() + 19) / 20)), true);
            } else if (scanning) OperativeRecon.reveal(host, target, REVEAL_TICKS);
        }
    }
    private void nativeFinish() {
        if (!(level() instanceof ServerLevel level) || ownerId == null) return;
        if (level.getEntity(ownerId) instanceof com.rzy.dealt_force_skills.ai.operative.OperativeHost bot && com.rzy.dealt_force_skills.ai.operative.NativeCommanderSkills.inherited(bot.skills(), NativeMorseSkills.class) instanceof NativeMorseSkills skills) skills.finishSonar(getUUID());
    }
    private static void revealNativeActingPlayer(ServerPlayer actor) {
        if (!(actor.level() instanceof ServerLevel level) || !TargetingUtil.isTargetablePlayer(actor)) return;
        for (var detector : List.copyOf(ACTIVE.values())) {
            if (detector.isRemoved() || detector.level() != level || !detector.nativeAllegiance.active()
                    || !detector.isScanning() || !detector.inCone(actor)) continue;
            var host = detector.nativeHost(level);
            if (host == null || !host.isEnemy(actor) || !host.withinOrders(actor.position())) continue;
            OperativeRecon.reveal(host, actor, REVEAL_TICKS);
            actor.addEffect(new MobEffectInstance(ModEffects.MORSE_SONAR_REVEALED.get(),
                    com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.morse_sonar_detector_entity.effect.morse_sonar_revealed.0.duration_ticks", 40),
                    com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.morse_sonar_detector_entity.effect.morse_sonar_revealed.0.amplifier", 0), false, true, true), host.body());
            level.playSound(null, detector.blockPosition(), ModSounds.MORSE_SONAR_ACTION_REVEAL.get(), SoundSource.NEUTRAL, 0.7F, 1);
        }
    }
}
