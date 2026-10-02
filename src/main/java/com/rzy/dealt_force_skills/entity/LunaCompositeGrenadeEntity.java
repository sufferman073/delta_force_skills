package com.rzy.dealt_force_skills.entity;

import com.rzy.dealt_force_skills.advancement.DfsAchievements;
import com.rzy.dealt_force_skills.character.commander.CommanderStateManager;
import com.rzy.dealt_force_skills.compat.SuperbWarfareCompat;
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
import net.minecraft.world.entity.player.Player;
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
import java.util.List;
import net.minecraft.network.syncher.SynchedEntityData;

public class LunaCompositeGrenadeEntity extends Projectile implements ItemSupplier {
    private static volatile int DEFAULT_FUSE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("DEFAULT_FUSE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.lunacompositegrenadeentity.default_fuse_ticks", 100));
    private static volatile double RADIUS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("RADIUS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.lunacompositegrenadeentity.radius", 8.0));
    private static volatile double WALL_BOUNCE_FACTOR = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("WALL_BOUNCE_FACTOR", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.lunacompositegrenadeentity.wall_bounce_factor", 0.55));
    private static volatile double GROUND_ROLL_FACTOR = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("GROUND_ROLL_FACTOR", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.lunacompositegrenadeentity.ground_roll_factor", 0.36));
    private static volatile double ROLL_STOP_SPEED_SQR = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("ROLL_STOP_SPEED_SQR", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.lunacompositegrenadeentity.roll_stop_speed_sqr", 0.004));
    private static volatile int MAX_ROLL_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("MAX_ROLL_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.lunacompositegrenadeentity.max_roll_ticks", 12));
    private static volatile float ARMOR_DAMAGE_SHARE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("ARMOR_DAMAGE_SHARE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.floatValue("summons.lunacompositegrenadeentity.armor_damage_share", 0.5F));
    private static volatile float ARMOR_DURABILITY_DAMAGE_PER_POINT = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("ARMOR_DURABILITY_DAMAGE_PER_POINT", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.floatValue(
      "summons.lunacompositegrenadeentity.armor_durability_damage_per_point", 10.0F
   ));
    private int fuseRemaining = DEFAULT_FUSE_TICKS;
    private int rollingTicks;
    private boolean rolling;
    private boolean settled;

    public LunaCompositeGrenadeEntity(EntityType<? extends LunaCompositeGrenadeEntity> type, Level level) {
        super(type, level);
    }

    public LunaCompositeGrenadeEntity(EntityType<? extends LunaCompositeGrenadeEntity> type, Level level, LivingEntity owner, int fuseTicks) {
        super(type, level);
        setOwner(owner);
        fuseRemaining = Math.max(1, fuseTicks);
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

        if (!level().isClientSide && --fuseRemaining <= 0) {
            explode(position());
            return;
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

        Vec3 motion = getDeltaMovement();
        Vec3 next = position().add(motion);
        HitResult hit = level().clip(new ClipContext(position(), next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (hit.getType() == HitResult.Type.BLOCK) {
            handleBlockHit((BlockHitResult) hit);
            spawnClientTrail();
            return;
        }

        setPos(next.x, next.y, next.z);
        setDeltaMovement(motion.add(0.0D, -0.045D, 0.0D).multiply(0.986D, 0.986D, 0.986D));
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
        rollingTicks = tag.getInt("RollingTicks");
        rolling = tag.getBoolean("Rolling");
        settled = tag.getBoolean("Settled");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("FuseRemaining", fuseRemaining);
        tag.putInt("RollingTicks", rollingTicks);
        tag.putBoolean("Rolling", rolling);
        tag.putBoolean("Settled", settled);
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
            settle();
            return;
        }

        setPos(next.x, next.y, next.z);
        setDeltaMovement(horizontal.scale(0.62D));
    }

    private void handleBlockHit(BlockHitResult hit) {
        Direction direction = hit.getDirection();
        Vec3 motion = getDeltaMovement();
        Vec3 normal = Vec3.atLowerCornerOf(direction.getNormal());
        setPos(hit.getLocation().x + normal.x * 0.045D,
                hit.getLocation().y + normal.y * 0.045D,
                hit.getLocation().z + normal.z * 0.045D);

        if (motion.lengthSqr() > 0.04D && level() instanceof ServerLevel serverLevel) {
            RangedSoundHelper.playThrottled(serverLevel, hit.getLocation(), ModSounds.LUNA_GRENADE_BOUNCE.get(),
                    SoundSource.PLAYERS, 0.45f, 0.9f + random.nextFloat() * 0.2f, 10.0D, 10, 3.0D);
        }

        Vec3 bounced = bounce(direction, motion);
        if (bounced.lengthSqr() > 0.02D
                && (direction != Direction.UP || bounced.y > 0.07D)) {
            setDeltaMovement(bounced);
            return;
        }
        Vec3 roll = new Vec3(motion.x * GROUND_ROLL_FACTOR, 0.0D, motion.z * GROUND_ROLL_FACTOR);
        if (roll.horizontalDistanceSqr() <= ROLL_STOP_SPEED_SQR) {
            settle();
        } else {
            rolling = true;
            rollingTicks = 0;
            setDeltaMovement(roll);
        }
    }

    private Vec3 bounce(Direction direction, Vec3 motion) {
        return com.rzy.dealt_force_skills.util.ProjectileBouncePhysics.reflect(
                direction, motion, WALL_BOUNCE_FACTOR, 0.35D, 0.75D);
    }

    private void settle() {
        rolling = false;
        settled = true;
        setDeltaMovement(Vec3.ZERO);
    }

    private void explode(Vec3 center) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            discard();
            return;
        }

        LivingEntity owner = getOwner() instanceof LivingEntity living ? living : null;
        ServerPlayer ownerPlayer = owner instanceof ServerPlayer player ? player : null;
        double radius = CommanderStateManager.skillRadius(ownerPlayer, RADIUS);
        double falloff = CommanderStateManager.skillDamageFalloff(ownerPlayer, 0.1D);
        RangedSoundHelper.playThrottled(serverLevel, center, ModSounds.LUNA_GRENADE_EXPLODE.get(),
                SoundSource.PLAYERS, 1.25f, 1.0f, 26.0D, 3, 4.0D);
        serverLevel.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y + 0.2D, center.z,
                14, 0.8D, 0.35D, 0.8D, 0.02D);
        serverLevel.sendParticles(ParticleTypes.CLOUD, center.x, center.y + 0.2D, center.z,
                90, radius * 0.68D, 0.5D, radius * 0.68D, 0.05D);

        AABB box = new AABB(center, center).inflate(radius);
        for (LivingEntity target : serverLevel.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (!TargetingUtil.isSelfOrHostileLivingFor(owner, target)) {
                continue;
            }
            Vec3 targetCenter = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
            double distance = targetCenter.distanceTo(center);
            if (distance > radius || !hasLineOfSight(center, targetCenter)) {
                continue;
            }

            float baseDamage = 100.0f * Math.max(0.0f, 1.0f - (float) distance * (float) falloff);
            if (baseDamage <= 0.0f) {
                continue;
            }

            ArmorDamageResult armorResult = damageArmor(target, baseDamage * ARMOR_DAMAGE_SHARE, owner);
            if (ownerPlayer != null) {
                DfsAchievements.recordLunaCompositeArmorBreak(ownerPlayer, target, armorResult.brokenPieces);
            }
            float healthDamage = baseDamage * (1.0f - ARMOR_DAMAGE_SHARE) + armorResult.overflowHealthDamage;
            if (!armorResult.hadDamageableArmor) {
                healthDamage += baseDamage * ARMOR_DAMAGE_SHARE;
            }

            Vec3 before = target.getDeltaMovement();
            float healthBefore = target.getHealth();
            target.invulnerableTime = 0;
            boolean damaged = SkillDamageHelper.hurt(target,
                    SkillDamageHelper.shepherdFragGrenade(serverLevel, this, owner),
                    owner,
                    healthDamage);
            target.setDeltaMovement(before);
            target.hurtMarked = true;
            if (damaged && target instanceof Player && healthBefore - target.getHealth() > target.getMaxHealth() * 0.5f) {
                playImportantFeedback(serverLevel, target, owner, ModSounds.LUNA_GRENADE_HIT.get(), 4.2f,
                        0.84f + target.getRandom().nextFloat() * 0.32f);
            }
        }
        SuperbWarfareCompat.damageVehicles(serverLevel, center, radius,
                SkillDamageHelper.shepherdFragGrenade(serverLevel, this, owner), this, 1.0F, true);
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

        if (brokenPieces > 0 && level() instanceof ServerLevel serverLevel) {
            playImportantFeedback(serverLevel, target, owner, ModSounds.LUNA_ARMOR_BREAK.get(), 4.0f,
                    0.86f + target.getRandom().nextFloat() * 0.28f);
        }
        return new ArmorDamageResult(true, overflowLoss / ARMOR_DURABILITY_DAMAGE_PER_POINT, brokenPieces);
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

    private boolean hasLineOfSight(Vec3 center, Vec3 targetCenter) {
        HitResult result = level().clip(new ClipContext(center, targetCenter, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        return result.getType() == HitResult.Type.MISS || result.getLocation().distanceToSqr(targetCenter) < 0.25D;
    }

    private void playImportantFeedback(ServerLevel level, LivingEntity target, LivingEntity owner, SoundEvent sound, float volume, float pitch) {
        RangedSoundHelper.playThrottled(level, target.position(), sound, SoundSource.MASTER, volume, pitch,
                40.0D, 8, 8.0D);
        if (owner != null && owner != target && owner.distanceToSqr(target) > 40.0D * 40.0D) {
            RangedSoundHelper.playThrottled(level, owner.position(), sound, SoundSource.MASTER, volume * 0.8f,
                    pitch, 10.0D, 8, 8.0D);
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
