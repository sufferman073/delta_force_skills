package com.rzy.dealt_force_skills.entity;

import com.rzy.dealt_force_skills.advancement.DfsAchievements;
import com.rzy.dealt_force_skills.character.commander.CommanderBranchUpgrade;
import com.rzy.dealt_force_skills.character.commander.CommanderStateManager;
import com.rzy.dealt_force_skills.compat.SuperbWarfareCompat;
import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.item.DfsEquipmentItem;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.skill.SkillDamageHelper;
import com.rzy.dealt_force_skills.util.RangedSoundHelper;
import com.rzy.dealt_force_skills.util.TargetingUtil;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.network.syncher.SynchedEntityData;

public class ShepherdFragGrenadeEntity extends Projectile implements ItemSupplier {
    private static final double DENTURES_HOME_RADIUS = 13.0D;
    private static final double DENTURES_HOMING_WEIGHT = 0.20D;
    private static volatile int DEFAULT_FUSE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("DEFAULT_FUSE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.shepherdfraggrenadeentity.default_fuse_ticks", 70));
    private static volatile double RADIUS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("RADIUS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.shepherdfraggrenadeentity.radius", 8.0));
    private static volatile double WALL_BOUNCE_FACTOR = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("WALL_BOUNCE_FACTOR", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.shepherdfraggrenadeentity.wall_bounce_factor", 0.5));
    private static volatile double GROUND_ROLL_FACTOR = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("GROUND_ROLL_FACTOR", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.shepherdfraggrenadeentity.ground_roll_factor", 0.3));
    private static final int MAX_PHYSICS_BOUNCES = 3;
    private static volatile int MAX_AUDIBLE_BOUNCES = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("MAX_AUDIBLE_BOUNCES", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.shepherd_frag_grenade_entity.max_audible_bounces", 1));
    private static volatile int MAX_ROLL_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("MAX_ROLL_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.shepherdfraggrenadeentity.max_roll_ticks", 8));
    private static volatile double SETTLE_SPEED_SQR = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("SETTLE_SPEED_SQR", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.shepherdfraggrenadeentity.settle_speed_sqr", 0.018));
    private static volatile double ROLL_STOP_SPEED_SQR = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("ROLL_STOP_SPEED_SQR", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.shepherdfraggrenadeentity.roll_stop_speed_sqr", 0.004));
    private static volatile float ARMOR_DAMAGE_SHARE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("ARMOR_DAMAGE_SHARE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.floatValue("summons.shepherdfraggrenadeentity.armor_damage_share", 0.65F));
    private static volatile float HEALTH_DAMAGE_SHARE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("HEALTH_DAMAGE_SHARE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.floatValue(
      "summons.shepherdfraggrenadeentity.health_damage_share", 1.0F - ARMOR_DAMAGE_SHARE
   ));
    private static volatile float ARMOR_DURABILITY_DAMAGE_PER_POINT = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("ARMOR_DURABILITY_DAMAGE_PER_POINT", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.floatValue(
      "summons.shepherdfraggrenadeentity.armor_durability_damage_per_point", 10.0F
   ));
    private int fuseRemaining = DEFAULT_FUSE_TICKS;
    private int audibleBounces;
    private int blockImpacts;
    private int rollingTicks;
    private boolean settled;
    private boolean rolling;
    private boolean myDentures;

    public ShepherdFragGrenadeEntity(EntityType<? extends ShepherdFragGrenadeEntity> type, Level level) {
        super(type, level);
    }

    public ShepherdFragGrenadeEntity(EntityType<? extends ShepherdFragGrenadeEntity> type, Level level, LivingEntity owner, int fuseTicks) {
        super(type, level);
        setOwner(owner);
        fuseRemaining = Math.max(1, fuseTicks);
        myDentures = owner instanceof ServerPlayer player
                && CommanderStateManager.hasBranchUpgrade(player, CommanderBranchUpgrade.SHEPHERD_MY_DENTURES);
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(Items.IRON_NUGGET);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();

        if (!level().isClientSide) {
            fuseRemaining--;
            if (fuseRemaining <= 0) {
                explode(position());
                return;
            }
        }

        if (settled) {
            setDeltaMovement(Vec3.ZERO);
            spawnClientTrail();
            return;
        }

        if (rolling) {
            tickRolling();
            spawnClientTrail();
            return;
        }

        if (!level().isClientSide && myDentures) {
            LivingEntity contact = findDenturesContact();
            if (contact != null) {
                explode(contact.position().add(0.0D, contact.getBbHeight() * 0.5D, 0.0D));
                return;
            }
            homeTowardDenturesTarget();
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
        setDeltaMovement(motion.add(0.0D, -0.045D, 0.0D).multiply(0.985D, 0.985D, 0.985D));
        checkInsideBlocks();
        spawnClientTrail();
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        fuseRemaining = tag.getInt("FuseRemaining");
        if (fuseRemaining <= 0) {
            fuseRemaining = DEFAULT_FUSE_TICKS;
        }
        audibleBounces = tag.getInt("AudibleBounces");
        blockImpacts = tag.getInt("BlockImpacts");
        rollingTicks = tag.getInt("RollingTicks");
        settled = tag.getBoolean("Settled");
        rolling = tag.getBoolean("Rolling");
        myDentures = tag.getBoolean("MyDentures");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("FuseRemaining", fuseRemaining);
        tag.putInt("AudibleBounces", audibleBounces);
        tag.putInt("BlockImpacts", blockImpacts);
        tag.putInt("RollingTicks", rollingTicks);
        tag.putBoolean("Settled", settled);
        tag.putBoolean("Rolling", rolling);
        tag.putBoolean("MyDentures", myDentures);
    }

    private void tickRolling() {
        Vec3 motion = getDeltaMovement();
        Vec3 horizontal = new Vec3(motion.x, 0.0D, motion.z);
        if (rollingTicks++ >= MAX_ROLL_TICKS || horizontal.horizontalDistanceSqr() <= ROLL_STOP_SPEED_SQR) {
            settle();
            return;
        }

        Vec3 next = position().add(horizontal);
        HitResult hit = level().clip(new ClipContext(position(), next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (hit.getType() == HitResult.Type.BLOCK) {
            Direction direction = ((BlockHitResult) hit).getDirection();
            Vec3 normal = Vec3.atLowerCornerOf(direction.getNormal());
            setPos(hit.getLocation().x + normal.x * 0.035D,
                    hit.getLocation().y + normal.y * 0.035D,
                    hit.getLocation().z + normal.z * 0.035D);
            settle();
            return;
        }

        setPos(next.x, next.y, next.z);
        setDeltaMovement(horizontal.scale(0.55D));
        checkInsideBlocks();
    }

    private void handleBlockHit(BlockHitResult hit) {
        Direction direction = hit.getDirection();
        Vec3 motion = getDeltaMovement();
        Vec3 normal = Vec3.atLowerCornerOf(direction.getNormal());
        setPos(hit.getLocation().x + normal.x * 0.04D,
                hit.getLocation().y + normal.y * 0.04D,
                hit.getLocation().z + normal.z * 0.04D);

        blockImpacts++;
        if (blockImpacts == 1) {
            playBounceSoundOnce(hit, motion);
        }
        Vec3 bounced = bounce(direction, motion);
        if (blockImpacts <= MAX_PHYSICS_BOUNCES
                && bounced.lengthSqr() > SETTLE_SPEED_SQR
                && (direction != Direction.UP || bounced.y > 0.07D)) {
            setDeltaMovement(bounced);
            return;
        }

        startRollingOrSettle(direction == Direction.UP ? motion : bounced);
    }

    private void startRollingOrSettle(Vec3 motion) {
        Vec3 roll = new Vec3(motion.x * GROUND_ROLL_FACTOR, 0.0D, motion.z * GROUND_ROLL_FACTOR);
        if (roll.horizontalDistanceSqr() <= ROLL_STOP_SPEED_SQR) {
            settle();
            return;
        }
        rolling = true;
        rollingTicks = 0;
        setDeltaMovement(roll);
    }

    private void settle() {
        rolling = false;
        settled = true;
        setDeltaMovement(Vec3.ZERO);
    }

    private void playBounceSoundOnce(BlockHitResult hit, Vec3 impactMotion) {
        if (audibleBounces >= MAX_AUDIBLE_BOUNCES || impactMotion.lengthSqr() <= 0.035D) {
            return;
        }
        audibleBounces++;
        if (level() instanceof ServerLevel serverLevel) {
            RangedSoundHelper.playThrottled(serverLevel, hit.getLocation(),
                    ModSounds.SHEPHERD_FRAG_GRENADE_BOUNCE.get(), SoundSource.PLAYERS, 0.45f,
                    0.9f + random.nextFloat() * 0.2f, 10.0D, 12, 3.0D);
        }
    }

    private Vec3 bounce(Direction direction, Vec3 motion) {
        return com.rzy.dealt_force_skills.util.ProjectileBouncePhysics.reflect(
                direction, motion, WALL_BOUNCE_FACTOR, 0.35D, 0.70D);
    }

    private void explode(Vec3 center) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            discard();
            return;
        }

        LivingEntity owner = getOwner() instanceof LivingEntity living ? living : null;
        ServerPlayer ownerPlayer = owner instanceof ServerPlayer player ? player : null;
        double radius = CommanderStateManager.skillRadius(ownerPlayer, RADIUS);
        double falloff = CommanderStateManager.skillDamageFalloff(ownerPlayer, com.rzy.dealt_force_skills.config.SkillBalance.falloffPerBlock("summons.shepherdfraggrenadeentity", 0.1D));
        boolean bounced = blockImpacts > 0 || audibleBounces > 0;
        RangedSoundHelper.playThrottled(serverLevel, center, ModSounds.SHEPHERD_FRAG_GRENADE_EXPLODE.get(),
                SoundSource.PLAYERS, 1.25f, 1.0f, 24.0D, 3, 4.0D);
        serverLevel.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y + 0.2D, center.z,
                12, 0.75D, 0.3D, 0.75D, 0.02D);
        serverLevel.sendParticles(ParticleTypes.CLOUD, center.x, center.y + 0.2D, center.z,
                72, radius * 0.65D, 0.45D, radius * 0.65D, 0.05D);

        AABB box = new AABB(center, center).inflate(radius);
        for (LivingEntity target : serverLevel.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (!TargetingUtil.isSelfOrHostileLivingFor(owner, target)) {
                continue;
            }
            double distance = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D).distanceTo(center);
            if (distance > radius) {
                continue;
            }

            float baseDamage = com.rzy.dealt_force_skills.config.SkillBalance.damage("summons.shepherdfraggrenadeentity.base_damage", 100.0F) * com.rzy.dealt_force_skills.config.SkillBalance.falloff("summons.shepherdfraggrenadeentity", distance, falloff);
            if (baseDamage <= 0.0f) {
                continue;
            }

            boolean wearingArmor = hasWornArmor(target);
            ArmorDamageResult armorResult = myDentures && wearingArmor
                    ? destroyArmorDurability(target, owner)
                    : damageArmor(target, baseDamage * ARMOR_DAMAGE_SHARE, owner);
            float healthDamage = baseDamage * HEALTH_DAMAGE_SHARE + armorResult.overflowHealthDamage;
            if (!armorResult.hadDamageableArmor) {
                // If there is no damageable armor item to grind, the armor-wear share must not disappear.
                // This also fixes attribute-only armor from curios/trinkets or mobs with built-in armor values.
                healthDamage += baseDamage * ARMOR_DAMAGE_SHARE;
            }
            if (myDentures && !wearingArmor) {
                healthDamage *= 12.45F;
            }
            if (healthDamage <= 0.0f) {
                continue;
            }

            float healthBefore = target.getHealth();
            Vec3 before = target.getDeltaMovement();
            target.invulnerableTime = 0;
            boolean armoredTarget = armorResult.hadDamageableArmor || target.getArmorValue() > 0;
            boolean damaged = SkillDamageHelper.hurt(target,
                    SkillDamageHelper.shepherdFragGrenade(serverLevel, this, owner),
                    owner,
                    healthDamage);
            if (ownerPlayer != null) {
                DfsAchievements.recordShepherdFragArmorBreak(ownerPlayer, target, bounced,
                        armorResult.brokenPieces, target.hasEffect(ModEffects.SONIC_SHOCK), !target.isAlive());
            }
            target.setDeltaMovement(before);
            target.hurtMarked = true;
            if (damaged && healthBefore > target.getMaxHealth() * 0.5f && target.getHealth() <= target.getMaxHealth() * 0.5f) {
                playHitByBlast(serverLevel, target, owner);
            }
        }
        SuperbWarfareCompat.damageVehicles(serverLevel, center, RADIUS,
                SkillDamageHelper.shepherdFragGrenade(serverLevel, this, owner), this, 1.0F, false);
        discard();
    }

    private ArmorDamageResult damageArmor(LivingEntity target, float armorDamage, LivingEntity owner) {
        List<EquipmentSlot> slots = new ArrayList<>();
        int totalRemainingDurability = 0;
        for (EquipmentSlot slot : armorSlots()) {
            ItemStack stack = target.getItemBySlot(slot);
            if (!stack.isEmpty() && stack.isDamageableItem()) {
                int remaining = remainingArmorDurability(stack);
                if (remaining > 0) {
                    slots.add(slot);
                    totalRemainingDurability += remaining;
                }
            }
        }
        if (slots.isEmpty()) {
            return new ArmorDamageResult(false, 0.0f, 0);
        }

        int requestedLoss = Math.max(1, Mth.ceil(armorDamage * ARMOR_DURABILITY_DAMAGE_PER_POINT));
        int overflowLoss = Math.max(0, requestedLoss - totalRemainingDurability);
        int totalLoss = Math.min(requestedLoss, totalRemainingDurability);
        int brokenPieces = 0;
        int remainingSlots = slots.size();
        for (EquipmentSlot slot : slots) {
            ItemStack stack = target.getItemBySlot(slot);
            if (stack.isEmpty() || !stack.isDamageableItem()) {
                remainingSlots--;
                continue;
            }
            int available = remainingArmorDurability(stack);
            if (available <= 0) {
                remainingSlots--;
                continue;
            }
            int loss = Math.min(available, Math.max(1, Mth.ceil(totalLoss / (float) remainingSlots)));
            totalLoss -= loss;
            remainingSlots--;
            if (damageArmorStack(target, slot, stack, loss)) {
                brokenPieces++;
            }
        }

        if (brokenPieces > 0) {
            playArmorBreak(serverLevel(), target, owner);
        }
        return new ArmorDamageResult(true, overflowLoss / ARMOR_DURABILITY_DAMAGE_PER_POINT, brokenPieces);
    }

    private boolean hasWornArmor(LivingEntity target) {
        for (EquipmentSlot slot : armorSlots()) {
            if (!target.getItemBySlot(slot).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private ArmorDamageResult destroyArmorDurability(LivingEntity target, LivingEntity owner) {
        boolean hadDamageableArmor = false;
        int brokenPieces = 0;
        for (EquipmentSlot slot : armorSlots()) {
            ItemStack stack = target.getItemBySlot(slot);
            if (stack.isEmpty() || !stack.isDamageableItem()) {
                continue;
            }
            hadDamageableArmor = true;
            int breakLimit = DfsEquipmentItem.profile(stack) != null
                    ? DfsEquipmentItem.breakDamageLimit(stack)
                    : stack.getMaxDamage();
            if (stack.getDamageValue() < breakLimit) {
                stack.setDamageValue(breakLimit);
                brokenPieces++;
            }
        }
        if (brokenPieces > 0) {
            playArmorBreak(serverLevel(), target, owner);
        }
        return new ArmorDamageResult(hadDamageableArmor, 0.0F, brokenPieces);
    }

    private void homeTowardDenturesTarget() {
        LivingEntity owner = getOwner() instanceof LivingEntity living ? living : null;
        if (owner == null) {
            return;
        }
        LivingEntity target = level().getEntitiesOfClass(LivingEntity.class,
                        getBoundingBox().inflate(DENTURES_HOME_RADIUS),
                        candidate -> TargetingUtil.isHostileLivingFor(owner, candidate))
                .stream()
                .min(Comparator.comparingDouble(candidate -> candidate.distanceToSqr(this)))
                .orElse(null);
        if (target == null) {
            return;
        }
        Vec3 toward = target.getEyePosition().subtract(position());
        if (toward.lengthSqr() < 0.0001D) {
            return;
        }
        double speed = Math.max(0.3D, getDeltaMovement().length());
        Vec3 desired = toward.normalize().scale(speed);
        Vec3 steered = getDeltaMovement().scale(1.0D - DENTURES_HOMING_WEIGHT)
                .add(desired.scale(DENTURES_HOMING_WEIGHT));
        if (steered.lengthSqr() > 0.0001D) {
            setDeltaMovement(steered.normalize().scale(speed));
        }
    }

    private LivingEntity findDenturesContact() {
        LivingEntity owner = getOwner() instanceof LivingEntity living ? living : null;
        if (owner == null) {
            return null;
        }
        return level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(0.35D),
                        target -> TargetingUtil.isHostileLivingFor(owner, target))
                .stream()
                .findFirst()
                .orElse(null);
    }

    private int remainingArmorDurability(ItemStack stack) {
        if (DfsEquipmentItem.profile(stack) != null) {
            return DfsEquipmentItem.remainingDurabilityBeforeBroken(stack);
        }
        return Math.max(1, stack.getMaxDamage() - stack.getDamageValue());
    }

    private boolean damageArmorStack(LivingEntity target, EquipmentSlot slot, ItemStack stack, int loss) {
        if (DfsEquipmentItem.profile(stack) != null) {
            return DfsEquipmentItem.damageWithoutBreaking(stack, loss, target);
        }
        int beforeCount = stack.getCount();
        stack.hurtAndBreak(loss, target, slot);
        return beforeCount > 0 && stack.isEmpty();
    }

    private ServerLevel serverLevel() {
        return (ServerLevel) level();
    }

    private void playArmorBreak(ServerLevel level, LivingEntity target, LivingEntity owner) {
        playImportantFeedback(level, target, owner, ModSounds.SHEPHERD_ARMOR_BREAK.get(), 4.0f,
                0.86f + target.getRandom().nextFloat() * 0.28f);
    }

    private void playHitByBlast(ServerLevel level, LivingEntity target, LivingEntity owner) {
        playImportantFeedback(level, target, owner, ModSounds.SHEPHERD_FRAG_GRENADE_HIT.get(), 4.2f,
                0.84f + target.getRandom().nextFloat() * 0.32f);
    }

    private void playImportantFeedback(ServerLevel level, LivingEntity target, LivingEntity owner, SoundEvent sound, float volume, float pitch) {
        RangedSoundHelper.playThrottled(level, target.position(), sound, SoundSource.MASTER, volume, pitch,
                40.0D, 8, 8.0D);
        if (owner != null && owner != target && owner.distanceToSqr(target) > 40.0D * 40.0D) {
            RangedSoundHelper.playThrottled(level, owner.position(), sound, SoundSource.MASTER, volume * 0.8f,
                    0.84f + owner.getRandom().nextFloat() * 0.32f, 10.0D, 8, 8.0D);
        }
    }

    private EquipmentSlot[] armorSlots() {
        return new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    }

    private void spawnClientTrail() {
        if (level().isClientSide && tickCount % 2 == 0) {
            level().addParticle(ParticleTypes.SMOKE, getX(), getY() + 0.04D, getZ(), 0.0D, 0.01D, 0.0D);
        }
    }

    private static final class ArmorDamageResult {
        private final boolean hadDamageableArmor;
        private final float overflowHealthDamage;
        private final int brokenPieces;

        private ArmorDamageResult(boolean hadDamageableArmor, float overflowHealthDamage, int brokenPieces) {
            this.hadDamageableArmor = hadDamageableArmor;
            this.overflowHealthDamage = overflowHealthDamage;
            this.brokenPieces = brokenPieces;
        }
    }
}
