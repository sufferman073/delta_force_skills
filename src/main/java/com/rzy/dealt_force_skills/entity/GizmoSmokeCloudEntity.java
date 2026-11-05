package com.rzy.dealt_force_skills.entity;

import com.rzy.dealt_force_skills.advancement.DfsAchievements;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.S2C_GizmoRevealEntities;
import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.util.TargetingUtil;
import com.rzy.dealt_force_skills.ai.operative.OperativeAllegiance;
import com.rzy.dealt_force_skills.ai.operative.OperativeAreaSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GizmoSmokeCloudEntity extends Entity implements OperativeAreaSource, ItemSupplier, SmokeCloudModelState {
    public static volatile int LIFE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("LIFE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.gizmosmokecloudentity.life_ticks", 300));
    public static volatile double RADIUS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("RADIUS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.gizmosmokecloudentity.radius", 4.5));
    private static final int PARTICLE_REFRESH_TICKS = 15;

    private final OperativeAllegiance allegiance = new OperativeAllegiance();
    @Override public OperativeAllegiance operativeAllegiance() { return allegiance; }
    public static int nativeLifetime() { return Math.max(1, LIFE_TICKS); }
    private UUID ownerId;

    public GizmoSmokeCloudEntity(EntityType<? extends GizmoSmokeCloudEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        noCulling = true;
    }

    public GizmoSmokeCloudEntity(EntityType<? extends GizmoSmokeCloudEntity> type, Level level, UUID ownerId) {
        this(type, level);
        this.ownerId = ownerId;
    }

    @Override
    public int smokeLifetimeTicks() {
        return LIFE_TICKS;
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(Items.YELLOW_DYE);
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && allegiance.active() && tickCount >= nativeLifetime()) { discard(); return; }
        if (level().isClientSide) {
            return;
        }

        if (level() instanceof ServerLevel serverLevel) {
            applySmokeEffects(serverLevel);
            suppressTargetsThroughSmoke(serverLevel);
        }
        if (tickCount >= LIFE_TICKS) {
            discard();
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        ownerId = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        if (tag.getBoolean("OperativeSource")) tickCount = allegiance.load(tag, nativeLifetime());
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        allegiance.save(tag, tickCount);
        if (ownerId != null) {
            tag.putUUID("Owner", ownerId);
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        double renderDistance = 512.0D;
        return distance < renderDistance * renderDistance;
    }

    private void applySmokeEffects(ServerLevel level) {
        AABB box = new AABB(position(), position()).inflate(RADIUS);
        List<Integer> revealIds = new ArrayList<>();
        Entity smokeOwner = ownerId == null ? null : level.getEntity(ownerId);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (!(allegiance.active() ? allegiance.isEnemy(level, target) : TargetingUtil.isHostileLivingFor(smokeOwner, target))
                    || (ownerId != null && target.getUUID().equals(ownerId))) {
                continue;
            }
            if (target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D).distanceTo(position()) > RADIUS) {
                continue;
            }

            boolean smokeApplied = target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.gizmo_smoke_cloud_entity.effect.movement_slowdown.0.duration_ticks", 20), com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.gizmo_smoke_cloud_entity.effect.movement_slowdown.0.amplifier", 0), false, true, true));
            if (ownerId != null && level.getEntity(ownerId) instanceof ServerPlayer owner) {
                DfsAchievements.recordGizmoComboTarget(owner, target, "smoke");
                if (smokeApplied) com.rzy.dealt_force_skills.department.DepartmentTasks.signal(owner,"gizmo.smoke.affected");
            }
            if (target.hasEffect(ModEffects.CORROSION.get())) {
                revealIds.add(target.getId());
                if (allegiance.active() && smokeOwner instanceof com.rzy.dealt_force_skills.ai.operative.OperativeHost bot && bot.skills() != null) bot.skills().remember(target, 20);
            }
        }

        if (!revealIds.isEmpty() && tickCount % 5 == 0 && ownerId != null && level.getEntity(ownerId) instanceof ServerPlayer owner) {
            com.rzy.dealt_force_skills.department.DepartmentTasks.signal(owner,"gizmo.smoke.corroded_reveal");
            // Corrosion smoke reveal is shared with the caster's teammates.
            com.rzy.dealt_force_skills.util.PositionRevealHelper.sendToCasterAndTeammates(
                    owner, new S2C_GizmoRevealEntities(revealIds));
        }
    }

    private void suppressTargetsThroughSmoke(ServerLevel level) {
        if (ownerId == null || !(level.getEntity(ownerId) instanceof LivingEntity owner) || !owner.isAlive()) {
            return;
        }

        AABB box = new AABB(position(), position()).inflate(40.0D);
        for (Mob mob : level.getEntitiesOfClass(Mob.class, box, mob -> mob.isAlive() && mob.getTarget() == owner)) {
            if (segmentIntersectsSmoke(mob.getEyePosition(), owner.getEyePosition())) {
                mob.setTarget(null);
                mob.getNavigation().stop();
            }
        }
    }

    private boolean segmentIntersectsSmoke(Vec3 start, Vec3 end) {
        Vec3 segment = end.subtract(start);
        double lengthSqr = segment.lengthSqr();
        if (lengthSqr < 0.0001D) {
            return start.distanceTo(position()) <= RADIUS;
        }

        double t = position().subtract(start).dot(segment) / lengthSqr;
        t = Math.max(0.0D, Math.min(1.0D, t));
        Vec3 closest = start.add(segment.scale(t));
        return closest.distanceTo(position()) <= RADIUS;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }
}
