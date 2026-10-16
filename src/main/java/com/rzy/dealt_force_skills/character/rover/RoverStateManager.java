package com.rzy.dealt_force_skills.character.rover;

import com.rzy.dealt_force_skills.character.CharacterSelectionManager;
import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.character.SkillSlot;
import com.rzy.dealt_force_skills.compat.PlayerReviveCompat;
import com.rzy.dealt_force_skills.effect.NoxDelayedWoundEffect;
import com.rzy.dealt_force_skills.entity.RoverProjectileEntity;
import com.rzy.dealt_force_skills.entity.RoverWolfEntity;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.S2C_RoverState;
import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.registry.ModEntities;
import com.rzy.dealt_force_skills.skill.SkillCooldownHelper;
import com.rzy.dealt_force_skills.skill.SkillDamageHelper;
import com.rzy.dealt_force_skills.team.DealtTeamManager;
import com.rzy.dealt_force_skills.util.TargetingUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.UUID;

import static com.rzy.dealt_force_skills.character.rover.RoverConfig.*;

public final class RoverStateManager {
    public static final String ROOT = "dealt_force_skills.rover";
    private static final net.minecraft.resources.ResourceLocation HEALTH_MODIFIER = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("dealt_force_skills", "rover_health");
    private static final net.minecraft.resources.ResourceLocation MOVE_MODIFIER = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("dealt_force_skills", "rover_movement");

    private RoverStateManager() {}

    public static CompoundTag data(LivingEntity entity) {
        CompoundTag root = entity.getPersistentData();
        if (!root.contains(ROOT)) root.put(ROOT, new CompoundTag());
        CompoundTag tag = root.getCompound(ROOT);
        if (entity instanceof ServerPlayer player) {
            SkillCooldownHelper.clampCooldown(player, tag, "SmokeNext", SMOKE_COOLDOWN);
            SkillCooldownHelper.clampCooldown(player, tag, "InjectorNext", INJECTOR_COOLDOWN);
            SkillCooldownHelper.clampCooldown(player, tag, "CoreUntil", CORE_COOLDOWN);
        }
        return tag;
    }

    public static boolean isRover(Player player) {
        return CharacterSelectionManager.getSelectedCharacter(player)
                .map(character -> ModCharacters.ROVER_ID.equals(character.id())).orElse(false)
                || com.rzy.dealt_force_skills.character.commander.CommanderStateManager
                .inheritsRuntimeSource(player, ModCharacters.ROVER_ID);
    }

    public static void initialize(ServerPlayer player) {
        CompoundTag tag = data(player);
        if (!tag.getBoolean("Initialized")) {
            tag.putBoolean("Initialized", true);
            tag.putInt("Smoke", Math.max(1, SMOKE_CHARGES));
            tag.putInt("Injector", Math.max(1, INJECTOR_CHARGES));
        }
    }

    public static void refreshSkills(ServerPlayer player, boolean fillCharges) {
        if (!isRover(player) && !player.getPersistentData().contains(ROOT)) return;
        initialize(player);
        refreshCharges(player, "Smoke", SMOKE_CHARGES, SMOKE_COOLDOWN, fillCharges);
        refreshCharges(player, "Injector", INJECTOR_CHARGES, INJECTOR_COOLDOWN, fillCharges);
        data(player).putLong("CoreUntil", 0);
        sync(player);
    }

    private static void refreshCharges(ServerPlayer player, String key, int capacity, int cooldown, boolean fill) {
        CompoundTag tag = data(player);
        capacity = Math.max(1, capacity);
        int charges = fill ? capacity : Math.min(capacity, Math.max(0, tag.getInt(key)) + 1);
        tag.putInt(key, charges);
        tag.putLong(key + "Next", charges < capacity ? SkillCooldownHelper.until(player, cooldown) : 0);
    }

    public static boolean isDowned(LivingEntity entity) {
        return entity.hasEffect(ModEffects.ROVER_DOWNED) || entity.getPersistentData().getCompound(ROOT).getLong("DownedUntil") > 0;
    }

    public static boolean executingDeath(LivingEntity entity) {
        return data(entity).getBoolean("ExecutingDeath");
    }

    public static boolean isProtectedEffect(net.minecraft.core.Holder<MobEffect> effect) {
        return effect == ModEffects.ROVER_DOWNED || effect == ModEffects.ROVER_PURIFIED
                || effect == ModEffects.ROVER_SECOND_CHANCE;
    }

    public static boolean hasProtection(LivingEntity entity) {
        return entity.hasEffect(ModEffects.ROVER_PURIFIED) || entity.hasEffect(ModEffects.ROVER_SECOND_CHANCE);
    }

    public static void tick(ServerPlayer player) {
        if (isRover(player)) {
            initialize(player);
            recharge(player, "Smoke", SMOKE_CHARGES, SMOKE_COOLDOWN);
            recharge(player, "Injector", INJECTOR_CHARGES, INJECTOR_COOLDOWN);
            tickDog(player);
            tickRescue(player);
        }
        if (isDowned(player)) tickDowned(player);
        if (isRover(player) || isDowned(player) || data(player).getBoolean("Synced")) {
            if (player.tickCount % 2 == 0) sync(player);
        }
    }

    private static void recharge(ServerPlayer player, String key, int capacity, int cooldown) {
        CompoundTag tag = data(player);
        capacity = Math.max(1, capacity);
        int charges = Math.min(capacity, tag.getInt(key));
        long deadline = tag.getLong(key + "Next");
        if (charges < capacity && deadline <= SkillCooldownHelper.now(player)) {
            charges++;
            tag.putLong(key + "Next", charges < capacity ? SkillCooldownHelper.until(player, cooldown) : 0);
        }
        tag.putInt(key, charges);
    }

    private static boolean consume(ServerPlayer player, String key, int cooldown) {
        CompoundTag tag = data(player);
        int count = tag.getInt(key);
        if (count <= 0) {
            SkillCooldownHelper.notifyCooldown(player, Component.translatable("message.dealt_force_skills.rover.empty"));
            return false;
        }
        tag.putInt(key, count - 1);
        if (tag.getLong(key + "Next") <= 0) tag.putLong(key + "Next", SkillCooldownHelper.until(player, cooldown));
        return true;
    }

    public static boolean useSkill(ServerPlayer player, SkillSlot slot, boolean alternate) {
        initialize(player);
        switch (slot) {
            case ACTIVE_1 -> { if (alternate) equip(player, 1); else throwSmoke(player, false); }
            case ACTIVE_2 -> equip(player, data(player).getInt("Tool") == 2 ? 0 : 2);
            case CORE -> core(player, alternate);
            default -> { return false; }
        }
        sync(player);
        return true;
    }

    public static void toolAction(ServerPlayer player, int action) {
        if (!isRover(player)) return;
        int tool = data(player).getInt("Tool");
        if (action == 0) equip(player, 0);
        if (action == 1 && tool == 1) throwSmoke(player, true);
        if (action == 2 && tool == 2 && consume(player, "Injector", INJECTOR_COOLDOWN)) {
            spawnProjectile(player, false, data(player).getBoolean("Bounce"), INJECTOR_SPEED, 0);
            sound(player, RoverSounds.INJECTOR_FIRE);
        }
        if (action == 3 && tool == 2) {
            data(player).putBoolean("Bounce", !data(player).getBoolean("Bounce"));
            sound(player, RoverSounds.INJECTOR_MODE);
        }
        sync(player);
    }

    public static void equip(ServerPlayer player, int tool) {
        CompoundTag tag = data(player);
        int previous = tag.getInt("Tool");
        if (previous == tool) return;
        if (tool != 0 && tag.getInt(tool == 1 ? "Smoke" : "Injector") <= 0) {
            SkillCooldownHelper.notifyCooldown(player, Component.translatable("message.dealt_force_skills.rover.empty"));
            return;
        }
        tag.putInt("Tool", tool);
        sound(player, tool == 1 ? RoverSounds.SMOKE_EQUIP : tool == 2 ? RoverSounds.INJECTOR_EQUIP
                : previous == 1 ? RoverSounds.SMOKE_STOW : RoverSounds.INJECTOR_STOW);
    }

    private static void throwSmoke(ServerPlayer player, boolean high) {
        if (!consume(player, "Smoke", SMOKE_COOLDOWN)) return;
        spawnProjectile(player, true, true, high ? SMOKE_HIGH_SPEED : SMOKE_LOW_SPEED,
                high ? SMOKE_HIGH_LIFT : SMOKE_LOW_LIFT);
        data(player).putInt("Tool", 0);
        sound(player, RoverSounds.SMOKE_THROW);
    }

    private static void spawnProjectile(ServerPlayer player, boolean smoke, boolean bounce, double speed, double lift) {
        RoverProjectileEntity projectile = new RoverProjectileEntity(ModEntities.ROVER_PROJECTILE.get(), player.level());
        projectile.setOwner(player);
        projectile.configure(smoke, bounce);
        Vec3 look = player.getLookAngle();
        projectile.setPos(player.getEyePosition().add(look.scale(0.35)).add(0, -0.1, 0));
        projectile.setDeltaMovement(look.scale(Math.max(0.1, speed)).add(0, lift, 0));
        player.serverLevel().addFreshEntity(projectile);
    }

    public static RoverWolfEntity dog(ServerPlayer player) {
        CompoundTag tag = data(player);
        if (!tag.hasUUID("Dog")) return null;
        return player.serverLevel().getEntity(tag.getUUID("Dog")) instanceof RoverWolfEntity wolf && wolf.isAlive() ? wolf : null;
    }

    private static void core(ServerPlayer player, boolean recall) {
        RoverWolfEntity wolf = dog(player);
        if (wolf == null && data(player).getBoolean("DogActive")) return;
        if (wolf != null) {
            if (recall) {
                data(player).remove("Waypoint");
                sound(player, RoverSounds.DOG_RECALL);
            } else {
                Vec3 from = player.getEyePosition();
                Vec3 end = from.add(player.getLookAngle().scale(Math.max(1, COMMAND_RANGE)));
                Vec3 point = player.level().clip(new ClipContext(from, end, ClipContext.Block.COLLIDER,
                        ClipContext.Fluid.NONE, player)).getLocation();
                CompoundTag waypoint = new CompoundTag();
                waypoint.putDouble("X", point.x); waypoint.putDouble("Y", point.y); waypoint.putDouble("Z", point.z);
                data(player).put("Waypoint", waypoint);
                sound(player, RoverSounds.DOG_COMMAND);
            }
            return;
        }
        if (remaining(player, "CoreUntil") > 0) {
            SkillCooldownHelper.notifyCooldown(player, Component.translatable("message.dealt_force_skills.rover.empty"));
            return;
        }
        wolf = new RoverWolfEntity(ModEntities.ROVER_WOLF.get(), player.level());
        wolf.tame(player);
        wolf.setPos(player.position());
        wolf.setCustomName(Component.translatable("entity.dealt_force_skills.rover_wolf"));
        wolf.setCustomNameVisible(true);
        wolf.getAttribute(Attributes.MAX_HEALTH).setBaseValue(Math.max(1, player.getMaxHealth() * DOG_HEALTH_MULTIPLIER));
        wolf.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(player.getAttributeValue(Attributes.MOVEMENT_SPEED) * 4.0);
        wolf.setHealth(wolf.getMaxHealth());
        wolf.getPersistentData().putLong("RoverExpires", SkillCooldownHelper.now(player) + Math.max(1, CORE_DURATION));
        if (player.serverLevel().addFreshEntity(wolf)) {
            data(player).putUUID("Dog", wolf.getUUID());
            data(player).putBoolean("DogActive", true);
            data(player).putLong("DogUntil", wolf.getPersistentData().getLong("RoverExpires"));
            data(player).remove("Waypoint");
            sound(player, RoverSounds.DOG_SUMMON);
            player.level().playSound(null, wolf.blockPosition(), RoverSounds.DOG_SPAWN, SoundSource.PLAYERS, 0.85F, 1.0F);
        }
    }

    public static Vec3 waypoint(ServerPlayer player) {
        if (!data(player).contains("Waypoint")) return null;
        CompoundTag point = data(player).getCompound("Waypoint");
        return new Vec3(point.getDouble("X"), point.getDouble("Y"), point.getDouble("Z"));
    }

    private static void tickDog(ServerPlayer player) {
        if (data(player).getBoolean("DogActive") && dog(player) == null
                && remaining(player, "DogUntil") <= 0) finishDog(player);
    }

    public static void finishDog(ServerPlayer player) {
        if (data(player).getBoolean("DogActive")) {
            data(player).putBoolean("DogActive", false);
            data(player).remove("Dog");
            data(player).remove("DogUntil");
            data(player).remove("Waypoint");
            data(player).putLong("CoreUntil", SkillCooldownHelper.until(player, CORE_COOLDOWN));

        }
    }

    public static void directHit(ServerPlayer owner, LivingEntity target, RoverProjectileEntity projectile) {
        if (!TargetingUtil.isHostileLivingFor(owner, target)) return;
        SkillDamageHelper.hurt(target, SkillDamageHelper.trueDamage(owner.serverLevel(), projectile, owner), owner,
                (float) Math.max(0, INJECTOR_DAMAGE));
        if (target.isAlive()) reduceHealth(target, INJECTOR_DIRECT_REDUCTION);
        damageFeedback(owner);
    }

    public static void applyAerosol(ServerPlayer owner, LivingEntity target, boolean pulse) {
        if (DealtTeamManager.isSelfOrTeammate(owner, target)) {
            int cleansed = 0;
            for (MobEffectInstance effect : new ArrayList<>(target.getActiveEffects())) {
                if (effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL && target.removeEffect(effect.getEffect())) cleansed++;
            }
            if (cleansed > 0) {
                if (target instanceof ServerPlayer listener) sound(listener, RoverSounds.CLEANSE);
                MobEffectInstance old = target.getEffect(ModEffects.ROVER_SECOND_CHANCE);
                int stacks = Math.min(255, cleansed + (old == null ? 0 : old.getAmplifier() + 1));
                target.addEffect(new MobEffectInstance(ModEffects.ROVER_SECOND_CHANCE, Math.max(1, PROTECTION_DURATION),
                        stacks - 1, false, true, true), owner);
            }
            target.addEffect(new MobEffectInstance(ModEffects.ROVER_PURIFIED, 3, 0, false, true, true), owner);
        } else if (TargetingUtil.isHostileLivingFor(owner, target)) {
            int duration = Math.max(2, INJECTOR_RESTORE_DELAY + 1);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, Math.max(0, INJECTOR_SLOW_AMPLIFIER)), owner);
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, duration, Math.max(0, WITHER_AMPLIFIER)), owner);
            target.addEffect(new MobEffectInstance(ModEffects.NOX_DELAYED_WOUND, duration), owner);
            NoxDelayedWoundEffect.resetCaps(target, target.getHealth());
            data(target).putLong("RestoreAt", SkillCooldownHelper.now(target) + Math.max(1, INJECTOR_RESTORE_DELAY));
            if (pulse) {
                reduceHealth(target, INJECTOR_TICK_REDUCTION);
                damageFeedback(owner);
            }
        }
    }

    public static void reduceHealth(LivingEntity target, double fraction) {
        var attribute = target.getAttribute(Attributes.MAX_HEALTH);
        if (attribute == null || fraction <= 0) return;
        CompoundTag tag = data(target);
        if (!tag.contains("HealthMinimum")) tag.putFloat("HealthMinimum", target.getHealth());
        double reduction = Math.min(0.95, Math.max(0, tag.getDouble("HealthReduction") + fraction));
        tag.putDouble("HealthReduction", reduction);
        tag.putLong("RestoreAt", SkillCooldownHelper.now(target) + Math.max(1, INJECTOR_RESTORE_DELAY));
        attribute.removeModifier(HEALTH_MODIFIER);
        attribute.addTransientModifier(new AttributeModifier(HEALTH_MODIFIER, -reduction, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        target.setHealth(Math.min(target.getHealth(), target.getMaxHealth()));
        tag.putFloat("HealthMinimum", Math.min(tag.getFloat("HealthMinimum"), target.getHealth()));
    }

    public static void tickHealth(LivingEntity target) {
        if (!target.getPersistentData().contains(ROOT)) return;
        CompoundTag tag = data(target);
        if (!tag.contains("HealthMinimum")) return;
        var health = target.getAttribute(Attributes.MAX_HEALTH);
        if (health != null && health.getModifier(HEALTH_MODIFIER) == null
                && SkillCooldownHelper.now(target) < tag.getLong("RestoreAt")) {
            double reduction = Math.max(0, Math.min(0.95, tag.getDouble("HealthReduction")));
            health.addTransientModifier(new AttributeModifier(HEALTH_MODIFIER, -reduction, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            target.setHealth(Math.min(target.getHealth(), target.getMaxHealth()));
        }
        float minimum = Math.min(tag.getFloat("HealthMinimum"), target.getHealth());
        tag.putFloat("HealthMinimum", minimum);
        if (SkillCooldownHelper.now(target) >= tag.getLong("RestoreAt")) {
            var attribute = target.getAttribute(Attributes.MAX_HEALTH);
            if (attribute != null) attribute.removeModifier(HEALTH_MODIFIER);
            tag.remove("HealthMinimum"); tag.remove("HealthReduction"); tag.remove("RestoreAt");
        }
        if (target.getHealth() > minimum) target.setHealth(minimum);
    }

    public static boolean preventDeath(LivingEntity entity, DamageSource source) {
        if (executingDeath(entity) || source.is(net.minecraft.world.damagesource.DamageTypes.GENERIC_KILL)) return false;
        MobEffectInstance protection = entity.getEffect(ModEffects.ROVER_SECOND_CHANCE);
        if (!isDowned(entity) && protection != null) {
            int remaining = protection.getAmplifier();
            int duration = protection.getDuration();
            entity.removeEffect(ModEffects.ROVER_SECOND_CHANCE);
            if (remaining > 0) entity.addEffect(new MobEffectInstance(ModEffects.ROVER_SECOND_CHANCE, duration, remaining - 1));
            entity.setHealth((float) Math.min(entity.getMaxHealth(), Math.max(1, TOTEM_HEALTH)));
            entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, Math.max(1, TOTEM_REGEN_DURATION), 1));
            entity.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, Math.max(1, TOTEM_ABSORPTION_DURATION), 1));
            entity.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, Math.max(1, TOTEM_FIRE_RESISTANCE_DURATION)));
            entity.level().broadcastEntityEvent(entity, (byte) 35);
            return true;
        }
        if (entity instanceof ServerPlayer player && !isDowned(player) && hasProtection(player)) {
            data(player).putLong("DownedUntil", SkillCooldownHelper.now(player) + Math.max(1, DOWNED_DURATION));
            data(player).putInt("Tool", 0);
            player.setHealth(Math.max(1, player.getMaxHealth()));
            tickDowned(player);
            sync(player);
            return true;
        }
        return false;
    }

    private static void tickDowned(ServerPlayer player) {
        CompoundTag tag = data(player);
        if (tag.getLong("DownedUntil") <= 0) tag.putLong("DownedUntil", SkillCooldownHelper.now(player) + Math.max(1, DOWNED_DURATION));
        int remaining = remaining(player, "DownedUntil");
        if (remaining <= 0) {
            tag.putBoolean("ExecutingDeath", true);
            tag.remove("DownedUntil");
            player.removeEffect(ModEffects.ROVER_DOWNED);
            player.hurt(player.damageSources().genericKill(), Float.MAX_VALUE);
            return;
        }
        if (!player.hasEffect(ModEffects.ROVER_DOWNED)) player.addEffect(new MobEffectInstance(ModEffects.ROVER_DOWNED, remaining));
        var move = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (move != null && move.getModifier(MOVE_MODIFIER) == null) move.addTransientModifier(new AttributeModifier(
                MOVE_MODIFIER, -Math.max(0, Math.min(1, DOWNED_MOVE_REDUCTION)), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        player.stopUsingItem(); player.setSprinting(false);
        if (player.containerMenu != player.inventoryMenu) player.closeContainer();
    }

    public static void clearDowned(ServerPlayer player, boolean heal) {
        data(player).remove("DownedUntil");
        data(player).remove("RescueTicks");
        player.removeEffect(ModEffects.ROVER_DOWNED);
        var move = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (move != null) move.removeModifier(MOVE_MODIFIER);
        if (heal) { player.clearFire(); player.setHealth(player.getMaxHealth()); }
        sync(player);
    }

    private static void tickRescue(ServerPlayer player) {
        CompoundTag tag = data(player);
        ServerPlayer target = isDowned(player) ? player : player.serverLevel().players().stream()
                .filter(other -> other != player && isDowned(other) && DealtTeamManager.canRescue(player, other))
                .filter(other -> other.distanceToSqr(player) <= RESCUE_RADIUS * RESCUE_RADIUS)
                .min(Comparator.comparingDouble(player::distanceToSqr)).orElse(null);
        if (!player.isShiftKeyDown() || target == null || !player.isAlive()) {
            tag.putInt("RescueTicks", 0); tag.remove("RescueTarget"); return;
        }
        if (!tag.hasUUID("RescueTarget") || !tag.getUUID("RescueTarget").equals(target.getUUID())) {
            tag.putInt("RescueTicks", 0); tag.putUUID("RescueTarget", target.getUUID());
        }
        int ticks = tag.getInt("RescueTicks") + 1;
        tag.putInt("RescueTicks", ticks);
        if (ticks >= Math.max(1, target == player ? SELF_RESCUE_TICKS : RESCUE_TICKS)) {
            clearDowned(target, true); tag.putInt("RescueTicks", 0);
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.rover.rescued"), true);
        }
    }

    public static void cleanup(ServerPlayer player) {
        PlayerReviveCompat.clearRoverBleeding(player);
        RoverWolfEntity wolf = dog(player);
        if (wolf != null) wolf.discard();
        clearDowned(player, false);
        var health = player.getAttribute(Attributes.MAX_HEALTH);
        if (health != null) health.removeModifier(HEALTH_MODIFIER);
        player.removeEffect(ModEffects.ROVER_PURIFIED);
        player.removeEffect(ModEffects.ROVER_SECOND_CHANCE);
        player.removeEffect(ModEffects.ROVER_VULNERABLE);
        player.getPersistentData().remove(ROOT);
        sync(player);
    }

    public static int remaining(LivingEntity entity, String key) {
        return SkillCooldownHelper.remainingTicks(data(entity).getLong(key), SkillCooldownHelper.now(entity));
    }

    public static void damageFeedback(ServerPlayer owner) {
        long now = owner.level().getGameTime();
        CompoundTag tag = data(owner);
        if (!tag.contains("DamageSoundAt") || now >= tag.getLong("DamageSoundAt")) {
            tag.putLong("DamageSoundAt", now + 4);
            sound(owner, RoverSounds.DAMAGE);
        }
    }

    public static void sound(ServerPlayer player, SoundEvent sound) {
        com.rzy.dealt_force_skills.util.RangedSoundHelper.playForPlayer(player, sound, SoundSource.PLAYERS, 0.85F, 1.0F);
    }

    public static void sync(ServerPlayer player) {
        CompoundTag tag = data(player);
        CompoundTag snapshot = new CompoundTag();
        snapshot.putInt("Tool", tag.getInt("Tool")); snapshot.putBoolean("Bounce", tag.getBoolean("Bounce"));
        snapshot.putInt("Smoke", tag.getInt("Smoke")); snapshot.putInt("Injector", tag.getInt("Injector"));
        snapshot.putInt("SmokeMax", Math.max(1, SMOKE_CHARGES)); snapshot.putInt("InjectorMax", Math.max(1, INJECTOR_CHARGES));
        snapshot.putInt("SmokeCooldown", remaining(player, "SmokeNext")); snapshot.putInt("InjectorCooldown", remaining(player, "InjectorNext"));
        snapshot.putInt("CoreCooldown", remaining(player, "CoreUntil")); snapshot.putInt("Downed", remaining(player, "DownedUntil"));
        snapshot.putInt("Rescue", tag.getInt("RescueTicks"));
        snapshot.putInt("RescueRequired", isDowned(player) ? Math.max(1, SELF_RESCUE_TICKS) : Math.max(1, RESCUE_TICKS));
        snapshot.putInt("DogTicks", tag.getBoolean("DogActive") ? remaining(player, "DogUntil") : 0);
        if (tag.contains("Waypoint")) snapshot.put("Waypoint", tag.getCompound("Waypoint").copy());
        net.minecraft.nbt.ListTag markers = new net.minecraft.nbt.ListTag();
        if (isRover(player)) {
            for (ServerPlayer other : player.serverLevel().players()) {
                if (other != player && isDowned(other) && DealtTeamManager.canRescue(player, other)) {
                    markers.add(marker(other.position(), "downed"));
                }
            }
        }
        snapshot.put("Markers", markers);
        NetworkHandler.sendToPlayer(new S2C_RoverState(snapshot), player);
        com.rzy.dealt_force_skills.skill.HeldToolVisualSync.sync(player);
        tag.putBoolean("Synced", isRover(player) || isDowned(player));
    }

    private static CompoundTag marker(Vec3 position, String kind) {
        CompoundTag tag = new CompoundTag();
        tag.putDouble("X", position.x); tag.putDouble("Y", position.y); tag.putDouble("Z", position.z);
        tag.putString("Kind", kind);
        return tag;
    }
}
