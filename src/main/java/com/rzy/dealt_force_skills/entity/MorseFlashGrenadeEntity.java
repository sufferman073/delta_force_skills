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
import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.skill.SkillDamageHelper;
import com.rzy.dealt_force_skills.util.RangedSoundHelper;
import com.rzy.dealt_force_skills.util.TargetingUtil;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import java.util.UUID;

public class MorseFlashGrenadeEntity extends Projectile implements ItemSupplier {
    private static volatile int DEFAULT_FUSE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("DEFAULT_FUSE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.morseflashgrenadeentity.default_fuse_ticks", 80));
    private static volatile int BOUNCE_FUSE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("BOUNCE_FUSE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.morseflashgrenadeentity.bounce_fuse_ticks", 12));
    private static volatile double BOUNCE_FACTOR = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("BOUNCE_FACTOR", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.morseflashgrenadeentity.bounce_factor", 0.66));
    private static volatile double RADIUS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("RADIUS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.morseflashgrenadeentity.radius", 24.0));
    private static volatile int MAX_FLASH_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("MAX_FLASH_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.morseflashgrenadeentity.max_flash_ticks", 240));
    private static volatile int MIN_FLASH_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("MIN_FLASH_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.morseflashgrenadeentity.min_flash_ticks", 8));
    private static volatile double MAX_FLASH_ANGLE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("MAX_FLASH_ANGLE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.morse_flash_grenade_entity.max_flash_angle", 90.0));
    private final OperativeAllegiance nativeAllegiance = new OperativeAllegiance();
    private UUID ownerId;
    private int fuseRemaining = DEFAULT_FUSE_TICKS;
    private boolean bounced;
    public void setNativeFuse(int ticks) {
        if (nativeAllegiance.active()) fuseRemaining = Math.max(2, Math.min(DEFAULT_FUSE_TICKS, ticks));
    }

    public MorseFlashGrenadeEntity(EntityType<? extends MorseFlashGrenadeEntity> type, Level level) {
        super(type, level);
    }

    public MorseFlashGrenadeEntity(EntityType<? extends MorseFlashGrenadeEntity> type, Level level, LivingEntity owner) {
        super(type, level);
        setOwner(owner);
        ownerId = owner.getUUID();
        nativeAllegiance.capture(owner);
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(Items.GLOWSTONE_DUST);
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    public void tick() {
        super.tick();
        if (nativeAllegiance.active() && level() instanceof ServerLevel level && fuseRemaining <= 3) NativeMorseSkills.turnAwayFromFlash(owner(level), position());
        if (!level().isClientSide && --fuseRemaining <= 0) {
            explode(position());
            return;
        }

        Vec3 motion = getDeltaMovement();
        Vec3 next = position().add(motion);
        HitResult hit = level().clip(new ClipContext(position(), next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (hit.getType() == HitResult.Type.BLOCK) {
            handleBlockHit((BlockHitResult) hit);
            spawnClientTrail();
            return;
        }

        setPos(next.x, next.y, next.z);
        setDeltaMovement(motion.add(0.0D, -0.045D, 0.0D).scale(0.985D));
        checkInsideBlocks();
        spawnClientTrail();
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        int nativeAge = nativeAllegiance.load(tag, 201);
        if (nativeAllegiance.active()) tickCount = nativeAge;
        ownerId = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        fuseRemaining = tag.getInt("FuseRemaining");
        if (nativeAllegiance.active()) fuseRemaining = Mth.clamp(fuseRemaining, 1, Math.max(1, DEFAULT_FUSE_TICKS));
        else if (fuseRemaining <= 0) {
            fuseRemaining = DEFAULT_FUSE_TICKS;
        }
        bounced = tag.getBoolean("Bounced");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        nativeAllegiance.save(tag, tickCount);
        if (ownerId != null) {
            tag.putUUID("Owner", ownerId);
        }
        tag.putInt("FuseRemaining", fuseRemaining);
        tag.putBoolean("Bounced", bounced);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    private void handleBlockHit(BlockHitResult hit) {
        Direction direction = hit.getDirection();
        Vec3 normal = Vec3.atLowerCornerOf(direction.getNormal());
        setPos(hit.getLocation().x + normal.x * 0.04D,
                hit.getLocation().y + normal.y * 0.04D,
                hit.getLocation().z + normal.z * 0.04D);
        setDeltaMovement(bounce(direction, getDeltaMovement()));
        if (!bounced && level() instanceof ServerLevel level) {
            RangedSoundHelper.playThrottled(level, hit.getLocation(), ModSounds.MORSE_FLASH_BOUNCE.get(),
                    SoundSource.PLAYERS, 0.55f, 1.0f, 14.0D, 6, 3.0D);
        }
        bounced = true;
        if (!level().isClientSide && fuseRemaining > BOUNCE_FUSE_TICKS) {
            fuseRemaining = BOUNCE_FUSE_TICKS;
        }
    }

    private Vec3 bounce(Direction direction, Vec3 motion) {
        return com.rzy.dealt_force_skills.util.ProjectileBouncePhysics.reflect(
                direction, motion, BOUNCE_FACTOR, 0.45D, 0.80D);
    }

    private void explode(Vec3 center) {
        if (!(level() instanceof ServerLevel level)) {
            discard();
            return;
        }
        LivingEntity owner = owner(level);
        RangedSoundHelper.playThrottled(level, center, ModSounds.MORSE_FLASH_EXPLODE.get(),
                SoundSource.PLAYERS, 1.2f, 1.0f, 30.0D, 3, 4.0D);
        level.sendParticles(ParticleTypes.FLASH, center.x, center.y + 0.2D, center.z,
                1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(ParticleTypes.END_ROD, center.x, center.y + 0.2D, center.z,
                80, 1.8D, 0.7D, 1.8D, 0.05D);

        AABB box = new AABB(center, center).inflate(RADIUS);
        int flashedTargets = 0;
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            // Self-harm skill: flash can hit thrower; teammates skipped.
            if (!(nativeAllegiance.active() ? nativeAllegiance.isCaster(target) || nativeAllegiance.isEnemy(level, target) : TargetingUtil.isSelfOrHostileLivingFor(owner, target))) {
                continue;
            }
            int flashTicks = flashDuration(center, target);
            if (flashTicks <= 0) {
                continue;
            }
            Vec3 before = target.getDeltaMovement();
            target.invulnerableTime = 0;
            SkillDamageHelper.hurtUnscaled(target, SkillDamageHelper.trueDamage(level, this, owner), 1.0F);
            target.setDeltaMovement(before);
            target.hurtMarked = true;
            boolean questApplied = target.addEffect(new MobEffectInstance(ModEffects.MORSE_FLASH_BLIND.get(),
                    flashTicks, com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.morse_flash_grenade_entity.effect.morse_flash_blind.0.amplifier", 0), false, false, true), owner);
            target.level().playSound(null, target.blockPosition(), ModSounds.MORSE_FLASH_HIT.get(),
                    SoundSource.PLAYERS, 0.7f, 1.0f);
            target.level().playSound(null, target.blockPosition(), ModSounds.MORSE_TINNITUS.get(),
                    SoundSource.PLAYERS, 0.55f, 1.0f);
            if (questApplied) com.rzy.dealt_force_skills.department.DepartmentRecon.morseAffected(owner,target,bounced,true);
            flashedTargets++;
        }
        if (owner instanceof ServerPlayer ownerPlayer) {
            DfsAchievements.recordMorseFlashTargets(ownerPlayer, flashedTargets);
        }
        discard();
    }

    private int flashDuration(Vec3 center, LivingEntity target) {
        Vec3 flashPos = center.add(0.0D, 0.65D, 0.0D);
        Vec3 eyes = target.getEyePosition(1.0F);
        Vec3 toFlash = flashPos.subtract(eyes);
        double distance = toFlash.length();
        if (distance > RADIUS || distance < 0.001D || !hasLineOfSight(eyes, flashPos)) {
            return 0;
        }

        Vec3 view = target.getViewVector(1.0F).normalize();
        double dot = Mth.clamp(view.dot(toFlash.normalize()), -1.0D, 1.0D);
        double angle = Math.toDegrees(Math.acos(dot));
        if (angle >= MAX_FLASH_ANGLE) {
            return MIN_FLASH_TICKS;
        }
        double angleFactor = 1.0D - angle / MAX_FLASH_ANGLE;
        return Math.max(MIN_FLASH_TICKS,
                (int) Math.round(MIN_FLASH_TICKS + (MAX_FLASH_TICKS - MIN_FLASH_TICKS) * angleFactor));
    }

    private boolean hasLineOfSight(Vec3 start, Vec3 end) {
        HitResult result = level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        return result.getType() == HitResult.Type.MISS || result.getLocation().distanceToSqr(end) < 0.35D;
    }

    private LivingEntity owner(ServerLevel level) {
        if (getOwner() instanceof LivingEntity living) {
            return living;
        }
        Entity owner = ownerId == null ? null : level.getEntity(ownerId);
        return owner instanceof LivingEntity living ? living : null;
    }

    private void spawnClientTrail() {
        if (level().isClientSide && tickCount % 2 == 0) {
            level().addParticle(ParticleTypes.END_ROD, getX(), getY() + 0.05D, getZ(), 0.0D, 0.01D, 0.0D);
        }
    }

    public static double nativeRadius() { return Math.max(0, RADIUS); }
    /** Same discrete fuse, face reflection and drag as tick; no world entities are created. */
    public static Vec3 nativeImpact(ServerLevel level, Vec3 position, Vec3 motion) {
        int fuse = Math.max(1, DEFAULT_FUSE_TICKS);
        for (int age = 0; age <= Math.max(1, DEFAULT_FUSE_TICKS); age++) {
            if (--fuse <= 0) return position;
            if (!level.hasChunkAt(net.minecraft.core.BlockPos.containing(position))) return null;
            var hit = level.clip(new ClipContext(position, position.add(motion), ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, (Entity) null));
            if (hit.getType() == HitResult.Type.BLOCK) {
                Vec3 normal = Vec3.atLowerCornerOf(hit.getDirection().getNormal());
                position = hit.getLocation().add(normal.scale(0.04D));
                motion = com.rzy.dealt_force_skills.util.ProjectileBouncePhysics.reflect(hit.getDirection(), motion, BOUNCE_FACTOR, 0.45D, 0.80D);
                fuse = Math.min(fuse, Math.max(1, BOUNCE_FUSE_TICKS));
            } else {
                position = position.add(motion);
                motion = motion.add(0, -0.045D, 0).scale(0.985D);
            }
        }
        return null;
    }
}
