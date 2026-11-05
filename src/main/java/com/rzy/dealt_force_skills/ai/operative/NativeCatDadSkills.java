package com.rzy.dealt_force_skills.ai.operative;

import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.character.SkillSlot;
import com.rzy.dealt_force_skills.character.catdad.CatDadStateManager;
import com.rzy.dealt_force_skills.config.DealtForceConfig;
import com.rzy.dealt_force_skills.entity.CatDadRoadTruckEntity;
import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.skill.SkillDamageHelper;
import com.rzy.dealt_force_skills.skill.SkillModelVisual;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Three-step hiss, delayed claw, refundable guard and the existing road truck. */
public final class NativeCatDadSkills implements OperativeSkills {
    private static final String HISS_OWNER = "dealt_force_skills.native_catdad_hiss_owner";
    private int hissStage, hissWindow, hissCooldown, strikeTicks, coreCooldown;
    private int blockCharges = CatDadStateManager.BLOCK_MAX_CHARGES, blockRecharge, blockTicks;
    private int downedTicks, rescueProgress, fatalUses;
    private float snapshotHealth = 1, healthBeforeHit = 1;
    private final Set<UUID> hissTargets = new HashSet<>();

    @Override public void tick(OperativeHost host) {
        if (hissCooldown > 0) hissCooldown--;
        if (coreCooldown > 0) coreCooldown--;
        if (blockTicks > 0) blockTicks--;
        blockCharges = Math.min(blockCharges, CatDadStateManager.BLOCK_MAX_CHARGES);
        if (blockCharges < CatDadStateManager.BLOCK_MAX_CHARGES && --blockRecharge <= 0) {
            blockCharges++; blockRecharge = CatDadStateManager.BLOCK_RECHARGE_TICKS;
        }
        if (downed()) { tickDowned(host); return; }
        if (!NativeRaptorSkills.available(host)) { cancel(host); return; }
        if (hissWindow > 0 && --hissWindow == 0) endHiss(host);
        if (strikeTicks > 0) {
            if (blocked(host, SkillSlot.ACTIVE_1)) { strikeTicks = 0; return; }
            if (--strikeTicks == 0) strike(host);
        }
    }
    private static boolean blocked(OperativeHost host, SkillSlot slot) {
        return OperativeElectronics.blocked(host, ModCharacters.CATDAD_ID, slot);
    }
    private static boolean valid(OperativeHost host, LivingEntity target, double range) {
        return target != null && target.isAlive() && target.level() == host.body().level()
                && host.isEnemy(target) && host.withinCombatRange(target.position())
                && host.body().distanceToSqr(target) <= range * range && host.canSee(target);
    }
    private static double range() {
        return DealtForceConfig.doubleValue("characters.catdad.cat_dad_state_manager.hiss_range", 14);
    }
    @Override public double score(OperativeHost host, SkillSlot slot, LivingEntity target) {
        if (downed() || busy() || !NativeRaptorSkills.available(host) || blocked(host, slot)) return 0;
        return switch (slot) {
            case ACTIVE_1 -> hissCooldown == 0 && valid(host, target, range()) ? 100 + hissStage * 5 : 0;
            case ACTIVE_2 -> blockCharges > 0 && blockTicks == 0 && valid(host, target, range())
                    && (host.body().distanceToSqr(target) <= 36 || host.body().getLastHurtByMob() == target) ? 130 : 0;
            case CORE -> coreCooldown == 0 && valid(host, target, 24) ? 90 : 0;
            case PASSIVE -> 0;
        };
    }
    @Override public boolean start(OperativeHost host, SkillSlot slot, LivingEntity target) {
        if (score(host, slot, target) <= 0) return false;
        host.aimAt(target);
        if (slot == SkillSlot.ACTIVE_2) {
            if (blockCharges == CatDadStateManager.BLOCK_MAX_CHARGES) blockRecharge = CatDadStateManager.BLOCK_RECHARGE_TICKS;
            blockCharges--; blockTicks = CatDadStateManager.BLOCK_WINDOW_TICKS;
            sound(host, ModSounds.CATDAD_BLOCK_START.get()); return true;
        }
        if (slot == SkillSlot.CORE) {
            Vec3 direction = horizontal(host);
            Vec3 start = ground(host, host.body().position().subtract(direction.scale(33)));
            var truck = new CatDadRoadTruckEntity(host.serverLevel(), host.body(), start, direction, 66, 16, false);
            if (!host.serverLevel().addFreshEntity(truck)) return false;
            snapshotHealth = Math.max(1, host.body().getHealth()); coreCooldown = CatDadStateManager.CORE_COOLDOWN_TICKS;
            sound(host, ModSounds.CATDAD_ROAD_START.get()); return true;
        }
        if (hissStage == 3) {
            endHiss(host); strikeTicks = Math.max(1, SkillModelVisual.CATDAD_CLAW.impactTick()); return true;
        }
        hissStage++;
        hissWindow = (7 - hissStage) * 20;
        if (hissStage < 3) {
            String effect = hissStage == 1 ? "catdad_hiss_slow.1" : "catdad_armor_reduced.2";
            target.addEffect(new MobEffectInstance(hissStage == 1 ? ModEffects.CATDAD_HISS_SLOW : ModEffects.CATDAD_ARMOR_REDUCED,
                    DealtForceConfig.intValue("characters.catdad.cat_dad_state_manager.effect." + effect + ".duration_ticks", hissStage == 1 ? 300 : 180),
                    DealtForceConfig.intValue("characters.catdad.cat_dad_state_manager.effect." + effect + ".amplifier", 0), false, true, true), host.body());
            target.getPersistentData().putUUID(HISS_OWNER, host.body().getUUID()); hissTargets.add(target.getUUID());
        } else target.lookAt(EntityAnchorArgument.Anchor.EYES, host.body().getEyePosition());
        sound(host, ModSounds.CATDAD_HISS.get()); return true;
    }
    private void strike(OperativeHost host) {
        Vec3 look = horizontal(host);
        double range = DealtForceConfig.doubleValue("characters.catdad.cat_dad_state_manager.empowered_strike_range", 14);
        var search = host.body().getBoundingBox().expandTowards(look.scale(range)).inflate(1, 1.2, 1);
        double attack = Math.max(1, host.body().getAttributeValue(Attributes.ATTACK_DAMAGE));
        for (LivingEntity target : host.serverLevel().getEntitiesOfClass(LivingEntity.class, search, t -> t.isAlive() && host.isEnemy(t) && host.withinCombatRange(t.position()) && host.canSee(t))) {
            Vec3 center = target.getBoundingBox().getCenter(), offset = center.subtract(host.body().getEyePosition());
            double forward = offset.x * look.x + offset.z * look.z;
            if (forward < 0 || forward > range || center.subtract(host.body().getEyePosition().add(look.scale(forward))).horizontalDistance() > 0.65
                    || Math.abs(offset.y) > 1.35) continue;
            double attenuation = forward <= 1 ? 1 : Math.max(0.25, 1 - (Math.floor(forward) - 1) * 0.1);
            SkillDamageHelper.hurt(target, SkillDamageHelper.catDadStrike(host.serverLevel(), host.body(), host.body()), host.body(), (float) (attack * 5 * attenuation));
        }
        sound(host, ModSounds.CATDAD_POWER_STRIKE.get());
    }
    private void endHiss(OperativeHost host) {
        if (hissStage > 0) hissCooldown = CatDadStateManager.HISS_COOLDOWN_TICKS;
        hissStage = hissWindow = 0;
        for (UUID id : hissTargets) if (host.serverLevel().getEntity(id) instanceof LivingEntity target
                && target.getPersistentData().hasUUID(HISS_OWNER) && host.body().getUUID().equals(target.getPersistentData().getUUID(HISS_OWNER))) {
            target.removeEffect(ModEffects.CATDAD_HISS_SLOW); target.removeEffect(ModEffects.CATDAD_ARMOR_REDUCED);
            target.getPersistentData().remove(HISS_OWNER);
        }
        hissTargets.clear();
    }
    public float incoming(OperativeHost host, DamageSource source, float amount) {
        if (amount <= 0 || !host.body().isAlive() || host.body().isInvulnerableTo(source) || host.body().level().isClientSide) return amount;
        healthBeforeHit = Math.max(1, host.body().getHealth());
        if (source.is(DamageTypes.GENERIC_KILL) || source.is(DamageTypes.FELL_OUT_OF_WORLD)) { clearDowned(host); return amount; }
        if (downed()) return 0;
        if (blockTicks > 0) {
            blockTicks = 0; blockCharges = Math.min(CatDadStateManager.BLOCK_MAX_CHARGES, blockCharges + 1);
            if (blockCharges == CatDadStateManager.BLOCK_MAX_CHARGES) blockRecharge = 0;
            sound(host, ModSounds.CATDAD_BLOCK_SUCCESS.get()); return 0;
        }
        if (!source.is(SkillDamageHelper.CATDAD_REFLECT) && source.getEntity() instanceof LivingEntity attacker
                && host.isEnemy(attacker) && host.body().getY() > attacker.getY()) {
            SkillDamageHelper.hurtUnscaled(attacker, SkillDamageHelper.catDadReflect(host.serverLevel(), host.body(), host.body()), amount * 0.5F);
            sound(host, ModSounds.CATDAD_REFLECT.get());
        }
        return amount;
    }
    public boolean preventDeath(OperativeHost host, DamageSource source) {
        if (source.is(DamageTypes.GENERIC_KILL) || source.is(DamageTypes.FELL_OUT_OF_WORLD)) return false;
        if (downed()) { host.body().setHealth(1); return true; }
        if (fatalUses >= CatDadStateManager.FATAL_DOWNED_MAX_USES) return false;
        fatalUses++; snapshotHealth = healthBeforeHit; enterRoadDowned(host); return true;
    }
    public void enterRoadDowned(OperativeHost host) {
        if (downed()) return;
        cancel(host); downedTicks = Math.max(1, CatDadStateManager.DOWNED_TICKS); rescueProgress = 0;
        host.body().setHealth(1); host.body().stopUsingItem(); stop(host);
        host.body().addEffect(new MobEffectInstance(ModEffects.CATDAD_DOWNED, downedTicks, 0, false, true, true));
        sound(host, ModSounds.CATDAD_TRUCK_SELF_DOWNED.get());
    }
    private void tickDowned(OperativeHost host) {
        host.body().stopUsingItem(); stop(host);
        if (--downedTicks <= 0) { clearDowned(host); host.body().hurt(host.body().damageSources().genericKill(), Float.MAX_VALUE); return; }
        if (!host.body().hasEffect(ModEffects.CATDAD_DOWNED)) host.body().addEffect(new MobEffectInstance(ModEffects.CATDAD_DOWNED, downedTicks, 0, false, true, true));
        if (NativeRaptorSkills.available(host)) {
            if (++rescueProgress >= Math.max(1, CatDadStateManager.SELF_RESCUE_TICKS)) revive(host);
        } else rescueProgress = 0;
    }
    public boolean downed() { return downedTicks > 0; }
    public void clearDowned(OperativeHost host) { downedTicks = rescueProgress = 0; host.body().removeEffect(ModEffects.CATDAD_DOWNED); }
    public void revive(OperativeHost host) {
        clearDowned(host); host.body().setHealth(Math.max(1, Math.min(host.body().getMaxHealth(), Math.max(snapshotHealth, host.body().getMaxHealth() * 0.3F))));
        sound(host, ModSounds.CATDAD_SELF_RESCUE_COMPLETE.get());
    }
    private static void stop(OperativeHost host) {
        host.body().setSprinting(false); host.body().setDeltaMovement(0, host.body().getDeltaMovement().y, 0);
        if (host.body() instanceof Mob mob) { mob.setTarget(null); mob.getNavigation().stop(); }
    }
    private static Vec3 horizontal(OperativeHost host) {
        Vec3 look = host.body().getLookAngle(), flat = new Vec3(look.x, 0, look.z);
        return flat.lengthSqr() < 0.0001 ? new Vec3(0, 0, 1) : flat.normalize();
    }
    private static Vec3 ground(OperativeHost host, Vec3 point) {
        BlockPos p = BlockPos.containing(point);
        for (int y = p.getY() + 3; y >= Math.max(host.serverLevel().getMinBuildHeight(), p.getY() - 10); y--) {
            BlockPos block = new BlockPos(p.getX(), y, p.getZ());
            var shape = host.serverLevel().getBlockState(block).getCollisionShape(host.serverLevel(), block);
            if (!shape.isEmpty()) return new Vec3(point.x, y + shape.max(Direction.Axis.Y), point.z);
        }
        return point;
    }
    @Override public boolean busy() { return strikeTicks > 0 || downed(); }
    @Override public void cancel(OperativeHost host) { endHiss(host); strikeTicks = blockTicks = 0; }
    @Override public CompoundTag save() {
        var tag = new CompoundTag();
        tag.putInt("HissStage", hissStage); tag.putInt("HissWindow", hissWindow); tag.putInt("HissCooldown", hissCooldown); tag.putInt("StrikeTicks", strikeTicks);
        tag.putInt("BlockCharges", blockCharges); tag.putInt("BlockRecharge", blockRecharge); tag.putInt("BlockTicks", blockTicks); tag.putInt("CoreCooldown", coreCooldown);
        tag.putInt("DownedTicks", downedTicks); tag.putInt("RescueProgress", rescueProgress); tag.putInt("FatalUses", fatalUses); tag.putFloat("SnapshotHealth", snapshotHealth);
        var targets = new ListTag(); hissTargets.forEach(id -> targets.add(NbtUtils.createUUID(id))); tag.put("HissTargets", targets); return tag;
    }
    @Override public void load(CompoundTag tag) {
        hissStage = bound(tag, "HissStage", 3); hissWindow = bound(tag, "HissWindow", 120); hissCooldown = bound(tag, "HissCooldown", CatDadStateManager.HISS_COOLDOWN_TICKS);
        strikeTicks = bound(tag, "StrikeTicks", SkillModelVisual.CATDAD_CLAW.impactTick()); coreCooldown = bound(tag, "CoreCooldown", CatDadStateManager.CORE_COOLDOWN_TICKS);
        blockCharges = tag.contains("BlockCharges") ? bound(tag, "BlockCharges", CatDadStateManager.BLOCK_MAX_CHARGES) : Math.max(0, CatDadStateManager.BLOCK_MAX_CHARGES); blockRecharge = bound(tag, "BlockRecharge", CatDadStateManager.BLOCK_RECHARGE_TICKS);
        blockTicks = bound(tag, "BlockTicks", CatDadStateManager.BLOCK_WINDOW_TICKS); downedTicks = bound(tag, "DownedTicks", CatDadStateManager.DOWNED_TICKS);
        rescueProgress = bound(tag, "RescueProgress", CatDadStateManager.SELF_RESCUE_TICKS); fatalUses = bound(tag, "FatalUses", CatDadStateManager.FATAL_DOWNED_MAX_USES);
        snapshotHealth = Float.isFinite(tag.getFloat("SnapshotHealth")) ? Math.max(1, tag.getFloat("SnapshotHealth")) : 1;
        hissTargets.clear(); var targets = tag.getList("HissTargets", Tag.TAG_INT_ARRAY);
        for (int i = 0; i < Math.min(128, targets.size()); i++) if (targets.getIntArray(i).length == 4) hissTargets.add(NbtUtils.loadUUID(targets.get(i)));
        if (hissStage > 0 && hissWindow == 0) { hissWindow = 1; hissCooldown = Math.max(hissCooldown, CatDadStateManager.HISS_COOLDOWN_TICKS); }
    }
    private static int bound(CompoundTag tag, String key, int max) { return Math.max(0, Math.min(Math.max(0, max), tag.getInt(key))); }
    private static void sound(OperativeHost host, SoundEvent sound) { host.serverLevel().playSound(null, host.body().blockPosition(), sound, SoundSource.NEUTRAL, 0.85F, 1); }
    @Override public String describe() { return "hiss="+hissStage+", block="+blockCharges+", core="+coreCooldown+", downed="+downedTicks+", lives="+(CatDadStateManager.FATAL_DOWNED_MAX_USES-fatalUses); }
    @Override public void refresh(OperativeHost host) {
        var fresh = new NativeCatDadSkills();
        blockCharges = fresh.blockCharges;
        blockRecharge = fresh.blockRecharge;
        coreCooldown = fresh.coreCooldown;
        hissCooldown = fresh.hissCooldown;
    }
}
