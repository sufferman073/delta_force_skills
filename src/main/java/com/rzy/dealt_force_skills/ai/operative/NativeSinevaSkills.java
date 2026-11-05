package com.rzy.dealt_force_skills.ai.operative;

import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.character.SkillSlot;
import com.rzy.dealt_force_skills.character.sineva.SinevaKnockdownState;
import com.rzy.dealt_force_skills.character.sineva.SinevaStateManager;
import com.rzy.dealt_force_skills.config.DealtForceConfig;
import com.rzy.dealt_force_skills.config.SkillBalance;
import com.rzy.dealt_force_skills.entity.BladeWireProjectileEntity;
import com.rzy.dealt_force_skills.entity.GrappleHookEntity;
import com.rzy.dealt_force_skills.entity.OperativeEntity;
import com.rzy.dealt_force_skills.registry.ModBlocks;
import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.registry.ModEntities;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.skill.SkillDamageHelper;
import com.rzy.dealt_force_skills.stamina.StaminaRules;
import com.rzy.dealt_force_skills.stamina.StaminaSettings;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Base Sineva: real wire, grapple, riot suit and shield actions with independent resources. */
public final class NativeSinevaSkills implements OperativeSkills {
    private static final net.minecraft.resources.ResourceLocation STEP_ID = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("dealt_force_skills", "operative_sineva_step");
    private int wires = Math.max(0, SinevaStateManager.BLADE_WIRE_MAX_CHARGES);
    private int wireRecharge, wireDelay, grappleCooldown, coreCooldown, equipTicks;
    private int bashCooldown, chargeCooldown, chargeTicks, staminaDelay, idleTicks, slowDelay;
    private boolean suit, front;
    private int gunWindow, defenseTicks, defenseCooldown;
    public boolean shieldFront() { return suit && front; }
    private double shield = Math.max(0, SinevaStateManager.SHIELD_MAX_DURABILITY);
    private double viewport = Math.max(0, SinevaStateManager.VIEWPORT_MAX_HEALTH);
    private long viewportWindow = Long.MIN_VALUE;
    private double viewportDamage;
    private double stamina = capacity(), damageSlow;
    private Vec3 chargeDirection = Vec3.ZERO;
    private Vec3 detachedChargePosition;
    private final Set<UUID> chargeHits = new HashSet<>(), reflected = new HashSet<>();

    @Override public void tick(OperativeHost host) {
        if (gunWindow > 0) gunWindow--;
        if (defenseTicks > 0) defenseTicks--;
        if (defenseCooldown > 0) defenseCooldown--;
        if (wireDelay > 0) wireDelay--;
        if (grappleCooldown > 0) grappleCooldown--;
        if (coreCooldown > 0) coreCooldown--;
        if (bashCooldown > 0) bashCooldown--;
        if (chargeCooldown > 0) chargeCooldown--;
        wires = Math.min(wires, Math.max(0, SinevaStateManager.BLADE_WIRE_MAX_CHARGES));
        if (wires < Math.max(0, SinevaStateManager.BLADE_WIRE_MAX_CHARGES) && --wireRecharge <= 0) {
            wires++; wireRecharge = Math.max(1, SinevaStateManager.BLADE_WIRE_RECHARGE_TICKS);
        }
        if (staminaDelay > 0) staminaDelay--;
        else stamina = Math.min(capacity(), stamina + StaminaRules.recoveryPerTick(0, 0));
        if (slowDelay > 0) slowDelay--;
        else damageSlow = Math.max(0, damageSlow - config("shield_damage_slow_max", 0.95D)
                / Math.max(1, config("shield_damage_slow_recovery_ticks", 80)));
        if (!usable(host)) { cancel(host); return; }
        if (equipTicks > 0 && --equipTicks == 0) {
            suit = front = true; defenseTicks = 16;
            shield = Math.max(0, SinevaStateManager.SHIELD_MAX_DURABILITY);
            viewport = Math.max(0, SinevaStateManager.VIEWPORT_MAX_HEALTH) * (OperativeGrowth.commander(host.body()) ? 3 : 1);
            sound(host, ModSounds.SINEVA_SHIELD_TOGGLE.get());
        }
        if (suit) {
            for (var effect : java.util.List.copyOf(host.body().getActiveEffects()))
                if (effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) host.body().removeEffect(effect.getEffect());
            LivingEntity target = host.body() instanceof Mob mob ? mob.getTarget() : null;
            if (chargeTicks > 0) {
                front = shield > 0;
            } else if (valid(host, target, 0, 24)) {
                idleTicks = 0;
                var gun = com.tacz.guns.api.item.IGun.getIGunOrNull(host.body().getMainHandItem());
                boolean firearm = gun != null && (host.body() instanceof OperativeEntity bot ? bot.gun().hasAmmunition(bot)
                        : gun.getCurrentAmmoCount(host.body().getMainHandItem()) > 0 || gun.hasBulletInBarrel(host.body().getMainHandItem()));
                if (firearm && defenseCooldown == 0 && imminent(host, target)) {
                    defenseTicks = 10; defenseCooldown = 30;
                }
                double distance = host.body().distanceToSqr(target);
                boolean controlReady = gunWindow == 0 && !host.body().hasEffect(ModEffects.STUN)
                        && (bashCooldown == 0 && distance <= 9 && StaminaRules.canSpend(stamina, StaminaSettings.bashCost(), 0, false)
                        || chargeCooldown == 0 && distance >= 25 && distance <= 100
                        && !host.body().hasEffect(ModEffects.WEBBED)
                        && StaminaRules.canSpend(stamina, StaminaSettings.chargeCost(), 0, false)
                        && safeChargeStep(host, target.position().subtract(host.body().position()).multiply(1, 0, 1).normalize()));
                front = shield > 0 && (chargeTicks > 0 || !firearm || defenseTicks > 0 || controlReady);
                host.aimAt(target);
                if (front && chargeTicks == 0 && gunWindow == 0 && target != null) {
                    if (host.body().distanceToSqr(target) <= 9) tryBash(host, target);
                    else if (host.body().distanceToSqr(target) >= 25 && host.body().distanceToSqr(target) <= 100)
                        tryCharge(host, target);
                }
            } else {
                front = false;
                if (++idleTicks >= 200) deactivate(host);
            }
        }
        if (chargeTicks > 0) {
            Vec3 position = detachedChargePosition == null ? host.body().position() : detachedChargePosition;
            if (!front || host.body().hasEffect(ModEffects.STUN) || detachedChargePosition == null && host.body().horizontalCollision
                    || !safeChargeStep(host, chargeDirection, position)) stopCharge(host);
            else {
                shieldStrike(host, chargeDirection, true);
                if (detachedChargePosition != null) detachedChargePosition = position.add(chargeDirection.scale(1.65D));
                else {
                    host.body().move(MoverType.SELF, chargeDirection.scale(1.65D));
                    host.body().setDeltaMovement(0, host.body().getDeltaMovement().y, 0);
                    host.body().hurtMarked = true;
                }
                if (--chargeTicks == 0) stopCharge(host);
            }
        }
        reconcile(host);
    }

    private static boolean imminent(OperativeHost host, LivingEntity target) {
        if (target.swinging && host.body().distanceToSqr(target) <= 16 && !target.hasEffect(ModEffects.STUN)) return true;
        Vec3 center = host.body().getBoundingBox().getCenter();
        var allegiance = new OperativeAllegiance(); allegiance.capture(host.body());
        for (Projectile p : host.serverLevel().getEntitiesOfClass(Projectile.class, host.body().getBoundingBox().inflate(6),
                p -> p.isAlive() && p.getOwner() != host.body() && !allegiance.isAlly(host.body(), p))) {
            Vec3 velocity = p.getDeltaMovement();
            if (velocity.dot(center.subtract(p.position())) <= 0 || !host.body().hasLineOfSight(p)) continue;
            if (host.body().getBoundingBox().inflate(.4).clip(p.position(), p.position().add(velocity.scale(4))).isPresent()) return true;
        }
        return false;
    }
    private static boolean usable(OperativeHost host) {
        return host.body().isAlive() && !host.body().isRemoved() && host.withinOrders(host.body().position());
    }
    private static boolean valid(OperativeHost host, LivingEntity target, double min, double max) {
        return usable(host) && target != null && host.isEnemy(target) && host.withinCombatRange(target.position())
                && host.canSee(target) && host.body().distanceToSqr(target) >= min * min
                && host.body().distanceToSqr(target) <= max * max;
    }
    @Override public double score(OperativeHost host, SkillSlot slot, LivingEntity target) {
        if (busy() || gunWindow > 0 || !valid(host, target, 0, 24) || host.body().hasEffect(ModEffects.STUN)
                || host.body().hasEffect(ModEffects.WEBBED)
                || OperativeElectronics.blocked(host, ModCharacters.SINEVA_ID, slot)) return 0;
        return switch (slot) {
            case CORE -> !suit && coreCooldown == 0 ? 100 : 0;
            case ACTIVE_1 -> wires > 0 && wireDelay == 0 && valid(host, target, 2, 24) && wireShot(host, target) != null ? 80 : 0;
            case ACTIVE_2 -> grappleCooldown == 0 && valid(host, target, 2, 24) && grappleClear(host, target) ? 90 : 0;
            default -> 0;
        };
    }
    @Override public boolean start(OperativeHost host, SkillSlot slot, LivingEntity target) {
        if (score(host, slot, target) <= 0) return false;
        host.aimAt(target);
        if (slot == SkillSlot.CORE) {
            equipTicks = Math.max(1, SinevaStateManager.BOMB_SUIT_EQUIP_TICKS);
            sound(host, ModSounds.SINEVA_RIOT_SUIT_PREPARE.get());
        } else if (slot == SkillSlot.ACTIVE_1) {
            var shot = wireShot(host, target);
            if (shot == null) return false;
            var wire = new BladeWireProjectileEntity(ModEntities.BLADE_WIRE_PROJECTILE.get(), host.serverLevel(), host.body());
            wire.setPos(host.body().getEyePosition().add(shot.direction().scale(0.55D)).add(0, -0.1D, 0));
            wire.setDeltaMovement(shot.direction().scale(2.25D).add(0, 0.14D, 0));
            if (!host.serverLevel().addFreshEntity(wire)) return false;
            if (wires == Math.max(0, SinevaStateManager.BLADE_WIRE_MAX_CHARGES)) wireRecharge = Math.max(1, SinevaStateManager.BLADE_WIRE_RECHARGE_TICKS);
            wires--; wireDelay = 100;
            sound(host, ModSounds.SINEVA_BARBED_WIRE_THROW.get());
        } else {
            var hook = new GrappleHookEntity(ModEntities.GRAPPLE_HOOK.get(), host.serverLevel(), host.body());
            Vec3 start = GrappleHookEntity.ropeOrigin(host.body(), 1);
            hook.setPos(start);
            hook.setDeltaMovement(target.getBoundingBox().getCenter().subtract(start).normalize().scale(2.2D));
            if (!host.serverLevel().addFreshEntity(hook)) return false;
            if (OperativeGrowth.commander(host.body()))
                for (var extra : host.serverLevel().getEntitiesOfClass(LivingEntity.class,
                        host.body().getBoundingBox().inflate(24), candidate -> candidate != target && valid(host, candidate, 2, 24) && grappleClear(host, candidate))) {
                    var additional = new GrappleHookEntity(ModEntities.GRAPPLE_HOOK.get(), host.serverLevel(), host.body());
                    additional.setPos(start);
                    additional.setDeltaMovement(extra.getBoundingBox().getCenter().subtract(start).normalize().scale(2.2D));
                    host.serverLevel().addFreshEntity(additional);
                }
            grappleCooldown = Math.max(1, SinevaStateManager.GRAPPLE_COOLDOWN_TICKS);
            sound(host, ModSounds.SINEVA_GRAPPLE_FIRE.get());
        }
        return true;
    }
    private static OperativeBallistics.Shot wireShot(OperativeHost host, LivingEntity target) {
        var shot = OperativeBallistics.solve(host, target.position().add(0, 0.05D, 0), 2.25D, 0.14D, 0.55D, 0.1D, 0.03D, 0.99D, 80, 1.2D, true);
        if (shot == null || !host.withinOrders(shot.impact())) return null;
        var p = net.minecraft.core.BlockPos.containing(shot.impact().add(0, 0.1D, 0));
        if (!host.serverLevel().getBlockState(p).canBeReplaced()) return null;
        // Avoid repeatedly carpeting an already deployed trap.
        for (var pos : net.minecraft.core.BlockPos.betweenClosed(p.offset(-2, -1, -2), p.offset(2, 1, 2)))
            if (host.serverLevel().getBlockState(pos).is(ModBlocks.BLADE_WIRE_CORE.get())) return null;
        return shot;
    }
    private static boolean grappleClear(OperativeHost host, LivingEntity target) {
        Vec3 start = GrappleHookEntity.ropeOrigin(host.body(), 1);
        Vec3 end = target.getBoundingBox().getCenter();
        if (host.serverLevel().clip(new net.minecraft.world.level.ClipContext(start, end,
                net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, host.body())).getType()
                != net.minecraft.world.phys.HitResult.Type.MISS) return false;
        return host.serverLevel().getEntitiesOfClass(LivingEntity.class, new AABB(start, end).inflate(0.5D),
                e -> e != host.body() && e != target && e.isAlive() && !host.isEnemy(e)
                        && e.getBoundingBox().inflate(0.3D).clip(start, end).isPresent()).isEmpty();
    }

    public boolean tryBash(OperativeHost host, LivingEntity target) {
        if (!front || busy() || bashCooldown > 0 || !valid(host, target, 0, 3) || host.body().hasEffect(ModEffects.STUN)
                || !StaminaRules.canSpend(stamina, StaminaSettings.bashCost(), 0, false)) return false;
        host.aimAt(target); spend(StaminaSettings.bashCost());
        bashCooldown = Math.max(1, DealtForceConfig.intValue("characters.sineva.sineva_skills.bash_cooldown_ticks", 10));
        shieldStrike(host, host.body().getLookAngle().normalize(), false);
        sound(host, ModSounds.SINEVA_SHIELD_BASH.get()); return true;
    }
    public boolean tryCharge(OperativeHost host, LivingEntity target) {
        if (!front || busy() || chargeCooldown > 0 || !valid(host, target, 4, 12) || host.body().hasEffect(ModEffects.STUN)
                || host.body().hasEffect(ModEffects.WEBBED)
                || !StaminaRules.canSpend(stamina, StaminaSettings.chargeCost(), 0, false)) return false;
        Vec3 direction = target.position().subtract(host.body().position()).multiply(1, 0, 1).normalize();
        if (!safeChargeStep(host, direction)) return false;
        spend(StaminaSettings.chargeCost()); host.aimAt(target);
        chargeDirection = direction; chargeTicks = 14; chargeHits.clear(); reflected.clear();
        detachedChargePosition = host.parallelNativeAi() ? host.body().position() : null;
        chargeCooldown = Math.max(1, DealtForceConfig.intValue("characters.sineva.sineva_skills.charge_cooldown_ticks", 10));
        sound(host, ModSounds.SINEVA_SHIELD_BASH_CHARGE.get()); return true;
    }
    private static boolean safeChargeStep(OperativeHost host, Vec3 direction) {
        return safeChargeStep(host, direction, host.body().position());
    }
    private static boolean safeChargeStep(OperativeHost host, Vec3 direction, Vec3 position) {
        Vec3 step = direction.scale(1.65D);
        AABB box = host.body().getBoundingBox().move(position.subtract(host.body().position()));
        return (host.parallelNativeAi() || host.body().onGround()) && host.withinOrders(position.add(step))
                && host.serverLevel().hasChunkAt(net.minecraft.core.BlockPos.containing(position.add(step)))
                && host.serverLevel().noCollision(host.body(), box.expandTowards(step).deflate(0.001D))
                && !host.serverLevel().noCollision(host.body(), box.move(step).move(0, -1, 0));
    }
    private void shieldStrike(OperativeHost host, Vec3 facing, boolean charge) {
        Vec3 offset = charge && detachedChargePosition != null ? detachedChargePosition.subtract(host.body().position()) : Vec3.ZERO;
        Vec3 eye = host.body().getEyePosition().add(offset);
        AABB bounds = host.body().getBoundingBox().move(offset).inflate(2, 1, 2);
        double dot = charge ? 0 : DealtForceConfig.doubleValue("characters.sineva.sineva_skills.front_dot", 0.5D);
        for (var target : host.serverLevel().getEntitiesOfClass(LivingEntity.class, bounds, host::isEnemy)) {
            Vec3 toward = target.getBoundingBox().getCenter().subtract(eye).normalize();
            if (!host.withinCombatRange(target.position()) || !(charge && detachedChargePosition != null ? strikeVisible(host, eye, target) : host.canSee(target)) || facing.dot(toward) < dot
                    || charge && !chargeHits.add(target.getUUID())) continue;
            float damage = SkillBalance.damage("characters.sineva.shield_" + (charge ? "charge" : "bash")
                    + (target instanceof Player ? ".player_damage" : ".non_player_damage"), target instanceof Player ? 5 : 50);
            SkillDamageHelper.hurt(target, SkillDamageHelper.sinevaShieldBash(host.serverLevel(), null, host.body()), host.body(), damage);
            if (target.isAlive()) {
                int duration = DealtForceConfig.intValue("characters.sineva.sineva_skills.silence_ticks", 60);
                target.addEffect(new MobEffectInstance(ModEffects.STUN, duration, 0, false, true));
                SinevaKnockdownState.apply(host.body(), target, duration);
                gunWindow = 40; defenseTicks = 0;
                if (!charge) { front = false; reconcile(host); }
            }
        }
        var allies = new OperativeAllegiance(); allies.capture(host.body());
        for (Projectile projectile : host.serverLevel().getEntitiesOfClass(Projectile.class, bounds,
                p -> p.isAlive() && p.getOwner() != host.body() && !allies.isAlly(host.body(), p))) {
            Vec3 toward = projectile.position().subtract(eye).normalize();
            if (facing.dot(toward) < dot || !host.withinOrders(projectile.position()) || !(charge && detachedChargePosition != null ? strikeVisible(host, eye, projectile) : host.body().hasLineOfSight(projectile))
                    || charge && !reflected.add(projectile.getUUID())) continue;
            double bonus = charge ? DealtForceConfig.doubleValue("characters.sineva.sineva_skills.projectile_charge_speed_bonus", 0.45D) : 0;
            double speed = Math.min(DealtForceConfig.doubleValue("characters.sineva.sineva_skills.projectile_reflect_max_speed", 3.8D),
                    Math.max(DealtForceConfig.doubleValue("characters.sineva.sineva_skills.projectile_reflect_min_speed", 1.4D), projectile.getDeltaMovement().length() + bonus));
            Vec3 motion = projectile.getDeltaMovement().lengthSqr() > 0.0001D ? projectile.getDeltaMovement().normalize().scale(-speed) : toward.scale(speed);
            if (motion.dot(toward) < 0.15D) motion = toward.scale(speed);
            projectile.setOwner(host.body()); projectile.setDeltaMovement(motion);
            projectile.setYRot((float) Math.toDegrees(Math.atan2(motion.x, motion.z)));
            projectile.setXRot((float) -Math.toDegrees(Math.atan2(motion.y, motion.horizontalDistance())));
            projectile.hasImpulse = projectile.hurtMarked = true;
            sound(host, ModSounds.SINEVA_PROJECTILE_REFLECT.get());
        }
    }

    /** Called before native hurt; shield wear occurs once, independently from player events. */
    public float incoming(OperativeHost host, DamageSource source, float amount) {
        if (!(amount > 0) || !Float.isFinite(amount)) return amount;
        if (suit && (source.is(DamageTypeTags.IS_FIRE) || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)
                || source.is(DamageTypeTags.WITCH_RESISTANT_TO) || source.is(DamageTypeTags.BYPASSES_EFFECTS)
                || source.getMsgId().equals("magic") || source.getMsgId().equals("indirectMagic"))) return 0;
        Vec3 position = source.getDirectEntity() != null ? source.getDirectEntity().position() : source.getSourcePosition();
        if (position == null && source.getEntity() != null) position = source.getEntity().getBoundingBox().getCenter();
        if (position != null) {
            Vec3 direction = position.subtract(host.body().getBoundingBox().getCenter());
            if (front && shield > 0 && (direction.lengthSqr() < 0.0001D || host.body().getLookAngle().dot(direction.normalize()) > 0)) {
                boolean glass = com.rzy.dealt_force_skills.event.CommonEvents.isSinevaViewportHit(host.body(), source, position);
                if (!glass || viewport > 0) {
                    if (glass) {
                        double damage = amount;
                        if (OperativeGrowth.commander(host.body())) {
                            long window = host.serverLevel().getGameTime() / 20;
                            if (viewportWindow != window) { viewportWindow = window; viewportDamage = 0; }
                            double cap = Math.max(0, SinevaStateManager.VIEWPORT_MAX_HEALTH) * 3 * .1;
                            damage = Math.min(damage, Math.max(0, cap - viewportDamage));
                            viewportDamage += damage;
                        }
                        viewport = Math.max(0, viewport - damage);
                    }
                    else if (!OperativeGrowth.commander(host.body())) shield = Math.max(0, shield - Math.min(amount, Math.max(0, SinevaStateManager.SHIELD_MAX_DAMAGE_PER_HIT)));
                    if (source.getEntity() instanceof Player) {
                        stamina = Math.max(Math.min(stamina, config("shield_block_stamina_floor_percent", 40) * 0.1D),
                                stamina - config("shield_block_stamina_cost_percent", 2) * 0.1D);
                        damageSlow = Math.min(config("shield_damage_slow_max", 0.95D), damageSlow + config("shield_damage_slow_per_hit", 0.04D));
                        slowDelay = (int) config("shield_damage_slow_recovery_delay_ticks", 20);
                        staminaDelay = StaminaRules.RECOVERY_DELAY;
                    }
                    front &= shield > 0; reconcile(host);
                    sound(host, glass && viewport == 0 ? net.minecraft.sounds.SoundEvents.GLASS_BREAK : ModSounds.SINEVA_SHIELD_HIT.get());
                    return 0;
                }
            }
            if ((!front || OperativeGrowth.commander(host.body())) && source.getEntity() != null && source.getEntity().getEyeY() >= host.body().getY() + 0.7D) {
                Vec3 forward = host.body().getLookAngle().multiply(1, 0, 1).normalize();
                Vec3 toward = direction.multiply(1, 0, 1).normalize();
                if (forward.dot(toward) < -Math.cos(Math.toRadians(70))) return 0;
            }
        }
        return suit ? amount * 0.5F : amount;
    }
    private static boolean strikeVisible(OperativeHost host, Vec3 eye, net.minecraft.world.entity.Entity target) {
        return host.serverLevel().clip(new net.minecraft.world.level.ClipContext(eye, target.getBoundingBox().getCenter(),
                net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, host.body()))
                .getType() == net.minecraft.world.phys.HitResult.Type.MISS;
    }
    public boolean suitActive() { return suit; }
    private void spend(double cost) { stamina = Math.max(0, stamina - cost); staminaDelay = StaminaRules.RECOVERY_DELAY; }
    private static double capacity() { return Math.max(1, DealtForceConfig.doubleValue("stamina.base_capacity", StaminaRules.BASE_CAPACITY)); }
    private static double config(String name, double fallback) { return DealtForceConfig.doubleValue("characters.sineva.sineva_state_manager." + name, fallback); }
    private static void sound(OperativeHost host, net.minecraft.sounds.SoundEvent sound) {
        host.serverLevel().playSound(null, host.body().blockPosition(), sound, SoundSource.NEUTRAL, 0.8F, 1);
    }
    private void reconcile(OperativeHost host) {
        var speed = host.body().getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(SinevaStateManager.BOMB_SUIT_SLOW_UUID);
            if (front && !host.parallelNativeAi()) speed.addTransientModifier(new AttributeModifier(SinevaStateManager.BOMB_SUIT_SLOW_UUID, -Math.min(0.99D, 0.4D + damageSlow), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
        var step = host.body().getAttribute(Attributes.STEP_HEIGHT);
        if (step != null) {
            step.removeModifier(STEP_ID);
            if (suit) step.addTransientModifier(new AttributeModifier(STEP_ID,
                    SinevaStateManager.bombSuitStepHeightBonus(), AttributeModifier.Operation.ADD_VALUE));
        }
        if (host.body() instanceof OperativeEntity bot) bot.setSinevaVisualState((front ? 1 : 0) | (viewport <= 0 ? 2 : 0) | (suit ? 4 : 0));
    }
    private void stopCharge(OperativeHost host) {
        if (chargeTicks > 0 && !host.parallelNativeAi()) host.body().setDeltaMovement(0, host.body().getDeltaMovement().y, 0);
        chargeTicks = 0; chargeDirection = Vec3.ZERO; detachedChargePosition = null; chargeHits.clear(); reflected.clear();
    }
    private void deactivate(OperativeHost host) {
        if (suit || equipTicks > 0) coreCooldown = Math.max(1, SinevaStateManager.BOMB_SUIT_COOLDOWN_TICKS);
        suit = front = false; equipTicks = idleTicks = 0; damageSlow = 0;
        gunWindow = defenseTicks = defenseCooldown = 0;
        stopCharge(host); reconcile(host);
    }
    @Override public boolean locksWeapon() { return front || equipTicks > 0 || chargeTicks > 0; }
    @Override public boolean busy() { return equipTicks > 0 || chargeTicks > 0; }
    @Override public void cancel(OperativeHost host) {
        deactivate(host);
        for (var hook : host.serverLevel().getEntitiesOfClass(GrappleHookEntity.class, host.body().getBoundingBox().inflate(100),
                e -> e.isOwnedBy(host.body().getUUID()))) hook.discard();
    }
    @Override public CompoundTag save() {
        var tag = new CompoundTag();
        tag.putInt("GunWindow", gunWindow); tag.putInt("DefenseTicks", defenseTicks); tag.putInt("DefenseCooldown", defenseCooldown);
        tag.putInt("WireCharges", wires); tag.putInt("WireRecharge", wireRecharge); tag.putInt("WireDelay", wireDelay);
        tag.putInt("GrappleCooldown", grappleCooldown); tag.putInt("CoreCooldown", coreCooldown);
        tag.putInt("EquipTicks", equipTicks); tag.putBoolean("Suit", suit); tag.putBoolean("Front", front);
        tag.putDouble("Shield", shield); tag.putDouble("Viewport", viewport); tag.putDouble("Stamina", stamina);
        tag.putInt("StaminaDelay", staminaDelay); tag.putInt("BashCooldown", bashCooldown); tag.putInt("ChargeCooldown", chargeCooldown);
        tag.putInt("IdleTicks", idleTicks); tag.putDouble("DamageSlow", damageSlow); tag.putInt("SlowDelay", slowDelay);
        return tag;
    }
    @Override public void load(CompoundTag tag) {
        gunWindow = bound(tag, "GunWindow", 40); defenseTicks = bound(tag, "DefenseTicks", 16); defenseCooldown = bound(tag, "DefenseCooldown", 30);
        wires = bound(tag, "WireCharges", SinevaStateManager.BLADE_WIRE_MAX_CHARGES);
        wireRecharge = bound(tag, "WireRecharge", SinevaStateManager.BLADE_WIRE_RECHARGE_TICKS); wireDelay = bound(tag, "WireDelay", 100);
        grappleCooldown = bound(tag, "GrappleCooldown", SinevaStateManager.GRAPPLE_COOLDOWN_TICKS);
        coreCooldown = bound(tag, "CoreCooldown", SinevaStateManager.BOMB_SUIT_COOLDOWN_TICKS);
        equipTicks = bound(tag, "EquipTicks", SinevaStateManager.BOMB_SUIT_EQUIP_TICKS);
        suit = tag.getBoolean("Suit"); front = suit && tag.getBoolean("Front");
        shield = finite(tag, "Shield", SinevaStateManager.SHIELD_MAX_DURABILITY);
        viewport = finite(tag, "Viewport", SinevaStateManager.VIEWPORT_MAX_HEALTH);
        front &= shield > 0; stamina = finite(tag, "Stamina", capacity());
        staminaDelay = bound(tag, "StaminaDelay", StaminaRules.RECOVERY_DELAY);
        bashCooldown = bound(tag, "BashCooldown", Math.max(1, DealtForceConfig.intValue("characters.sineva.sineva_skills.bash_cooldown_ticks", 10)));
        chargeCooldown = bound(tag, "ChargeCooldown", Math.max(1, DealtForceConfig.intValue("characters.sineva.sineva_skills.charge_cooldown_ticks", 10)));
        idleTicks = bound(tag, "IdleTicks", 200); damageSlow = finite(tag, "DamageSlow", config("shield_damage_slow_max", 0.95D));
        slowDelay = bound(tag, "SlowDelay", (int) config("shield_damage_slow_recovery_delay_ticks", 20));
        chargeTicks = 0; chargeDirection = Vec3.ZERO; detachedChargePosition = null; chargeHits.clear(); reflected.clear();
    }
    private static int bound(CompoundTag tag, String key, int max) { return Math.max(0, Math.min(Math.max(0, max), tag.getInt(key))); }
    private static double finite(CompoundTag tag, String key, double max) { double value = tag.getDouble(key); return Double.isFinite(value) ? Math.max(0, Math.min(Math.max(0, max), value)) : 0; }
    @Override public String describe() { return "wire=" + wires + ", grapple=" + grappleCooldown + ", suit=" + suit + ", equip=" + equipTicks
            + ", front=" + front + ", shield=" + shield + ", viewport=" + viewport + ", stamina=" + stamina; }
    @Override public void refresh(OperativeHost host) {
        var fresh = new NativeSinevaSkills();
        bashCooldown = fresh.bashCooldown;
        chargeCooldown = fresh.chargeCooldown;
        coreCooldown = fresh.coreCooldown;
        grappleCooldown = fresh.grappleCooldown;
        wireRecharge = fresh.wireRecharge;
        wires = fresh.wires;
    }
    @Override public com.rzy.dealt_force_skills.motion.TacticalMotion motion() {
        return chargeTicks > 0 ? com.rzy.dealt_force_skills.motion.TacticalMotion.CHARGE : com.rzy.dealt_force_skills.motion.TacticalMotion.NONE;
    }
    @Override public int motionDuration() { return 14; }
    @Override public Vec3 motionDirection() { return chargeDirection; }

}
