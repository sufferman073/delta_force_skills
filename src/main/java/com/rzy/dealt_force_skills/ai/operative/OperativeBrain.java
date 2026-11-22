package com.rzy.dealt_force_skills.ai.operative;

import com.rzy.dealt_force_skills.character.SkillSlot;
import com.rzy.dealt_force_skills.entity.OperativeEntity;
import net.minecraft.world.entity.LivingEntity;

import java.util.Arrays;
import java.util.Comparator;

import static com.rzy.dealt_force_skills.ai.operative.BehaviorNode.Status.*;

/** Bounded perception, utility ranking, then reactive behavior execution. */
public final class OperativeBrain {
    private static final SkillSlot[] ACTIVE_SLOTS = {SkillSlot.ACTIVE_1, SkillSlot.ACTIVE_2, SkillSlot.CORE};
    private final OperativeEntity host;
    private final BehaviorNode tree;
    private final OperativeTactics tactics;
    private final OperativeHearing hearing = new OperativeHearing();
    private int nextSkillTick, nextRaidPathTick;
    private String lastSkill = "none";
    private double meleeCooldown;
    private String action = "idle";

    public OperativeBrain(OperativeEntity host) {
        this.host = host;
        tactics = new OperativeTactics(host);
        tree = BehaviorNode.selector(
                this::enforceOrders,
                BehaviorNode.sequence(this::hasTarget, this::fight),
                this::returnToOwnerOrPost,
                () -> { action = "idle"; return SUCCESS; });
    }

    public void tick() {
        if (meleeCooldown > 0) meleeCooldown--;
        // Perception arbitrates owner orders against immediate self-defence.
        host.orderPosition(); // Retain the last loaded owner position for disconnects or dimension changes.
        if (host.getTarget() != null && (!hearing.validContact(host, host.getTarget())
                || !host.withinCombatRange(host.getTarget().position()))) host.setTarget(null);
        if ((host.tickCount + host.getId()) % 10 == 0) perceive();
        if ((host.tickCount + host.getId()) % 5 == 0 || ("melee".equals(action) && meleeCooldown <= 0)) {
            tactics.perceive();
            if (host.withinLeash(host.position())) chooseSkill();
            if (host.getTarget() == null && tactics.threatened() && tactics.move(null)) action = tactics.describe();
            else tree.tick();
        }
    }

    private void perceive() {
        if (host.skills() instanceof NativeCorpsSkills corps) {
            LivingEntity opponent = corps.duelTarget(host);
            if (opponent != null && opponent.isAlive() && host.isEnemy(opponent)) {
                host.setTarget(opponent);
                return;
            }
        }
        if(host.skills() instanceof NativeManbaSkills manba && manba.dueling()){
            LivingEntity opponent=manba.duelTarget(host);
            host.setTarget(opponent!=null && opponent.isAlive() && host.isEnemy(opponent) && host.withinLeash(opponent.position())?opponent:null);
            return;
        }
        LivingEntity urgent = host.level().getEntitiesOfClass(LivingEntity.class,
                host.getBoundingBox().inflate(8), candidate -> host.isEnemy(candidate)
                        && host.withinCombatRange(candidate.position()) && OperativeHearing.visible(host, candidate)).stream()
                .min(Comparator.comparingDouble(candidate -> host.distanceToSqr(candidate)
                        - (candidate instanceof net.minecraft.world.entity.Mob mob && mob.getTarget() == host ? 12 : 0)))
                .orElse(null);
        LivingEntity current = host.getTarget();
        if (urgent != null) {
            // Retain an equally close current threat; owner focus cannot override self-defence.
            if (current == null || !host.isEnemy(current) || !OperativeHearing.visible(host, current)
                    || host.distanceToSqr(current) > 64 || host.distanceToSqr(urgent) + 9 < host.distanceToSqr(current))
                host.setTarget(urgent);
            hearing.clear(); return;
        }
        LivingEntity assisted = host.assistedTarget();
        if (assisted != null && host.isEnemy(assisted) && host.withinCombatRange(assisted.position())
                && (OperativeHearing.visible(host, assisted) || host.skills().isRevealed(assisted) || OperativeHearing.audible(assisted))) {
            host.setTarget(assisted); hearing.clear(); return;
        }
        LivingEntity retained = host.getTarget();
        if (retained != null && host.isEnemy(retained) && host.withinCombatRange(retained.position())
                && (OperativeHearing.visible(host, retained) || host.skills().isRevealed(retained))) {
            hearing.clear(); return;
        }
        LivingEntity owner = host.getOwner();
        LivingEntity threat = owner == null ? null : owner.getLastHurtByMob();
        LivingEntity previous = host.getTarget();
        double range = host.acquisitionRange();
        LivingEntity seen = host.level().getEntitiesOfClass(LivingEntity.class,
                        host.getBoundingBox().inflate(range), candidate -> host.isEnemy(candidate) && host.distanceToSqr(candidate) <= range * range
                                && host.withinCombatRange(candidate.position()) && (OperativeHearing.visible(host, candidate)
                                || host.skills().isRevealed(candidate))).stream()
                .min(Comparator.comparingDouble(candidate -> host.distanceToSqr(candidate)
                        - (candidate == threat ? 600.0D : 0.0D)
                        - (candidate == host.getLastHurtByMob() ? 300.0D : 0.0D)
                        - (candidate == previous ? 30.0D : 0.0D)))
                .orElse(null);
        if (seen != null) { host.setTarget(seen); hearing.clear(); return; }
        boolean previouslyHeard = hearing.tracks(retained);
        LivingEntity heard = hearing.acquire(host);
        // Keep a previously observed combat contact outside acquisition range, as before.
        if (heard == null && !previouslyHeard && retained != null && host.isEnemy(retained)
                && host.withinCombatRange(retained.position()) && OperativeHearing.audible(retained)) host.setTarget(retained);
        else host.setTarget(heard);
    }

    private BehaviorNode.Status enforceOrders() {
        if (host.withinLeash(host.position())) return FAILURE;
        host.skills().cancel(host);
        hearing.clear();
        host.setTarget(null);
        return returnToOwnerOrPost();
    }

    private BehaviorNode.Status hasTarget() {
        return hearing.validContact(host, host.getTarget()) ? SUCCESS : FAILURE;
    }

    private BehaviorNode.Status chooseSkill() {
        if (host.skills().busy()) return FAILURE;
        LivingEntity target = host.getTarget();
        if(host.skills() instanceof NativeLexSkills lex && host.tickCount>=nextSkillTick && lex.plan(host,target)) {
            NativeLexEvents.action(host);
            nextSkillTick=host.tickCount+4;lastSkill="ninjutsu";return SUCCESS;
        }
        for (SkillSlot slot : Arrays.stream(ACTIVE_SLOTS)
                .sorted(Comparator.comparingDouble((SkillSlot value) -> utility(value, target)).reversed())
                .toList()) {
            double score = utility(slot, target);
            boolean emergency = score >= 100 && (tactics.threatened() || host.getHealth() < host.getMaxHealth() * .5 || target == null);
            if ((host.tickCount >= nextSkillTick || emergency) && score > 0 && host.skills().start(host, slot, target)) {
                NativeLexEvents.action(host);
                if (!host.skills().busy()) host.showSkillUse();
                nextSkillTick = host.tickCount + (emergency ? 10 : 12 + host.getRandom().nextInt(9));
                lastSkill = slot.name().toLowerCase(java.util.Locale.ROOT);
                return SUCCESS;
            }
        }
        return FAILURE;
    }

    private double utility(SkillSlot slot, LivingEntity target) {
        return OperativeSkillStrategy.score(host, host.skills(), host.characterId(), slot, target,
                OperativeGun.equipped(host) && host.gun().hasAmmunition(host), tactics.preferredDistance(),
                host.gun().reloading(host), tactics.threatened());
    }

    private BehaviorNode.Status fight() {
        LivingEntity target = host.getTarget();
        if (com.rzy.dealt_force_skills.raid.ai.RaidAiAffinity.pursuing(host) && !tactics.threatened()) {
            double range = OperativeGun.equipped(host) && host.gun().hasAmmunition(host)
                    ? Math.min(64, OperativeGun.effectiveRange(host)) : 12;
            if (host.distanceToSqr(target) > range * range || !host.hasLineOfSight(target) && !host.skills().isRevealed(target))
                return returnToOwnerOrPost();
        }
        if (!host.withinCombatRange(target.position())) {
            host.setTarget(null);
            return FAILURE;
        }
        host.getLookControl().setLookAt(target, 30.0F, 30.0F);
        if (host.skills().locksMovement()) { host.getNavigation().stop(); action = "skill:" + lastSkill; return RUNNING; }
        if (tactics.move(target)) { action = tactics.describe(); return RUNNING; }
        if (!host.isEnemy(target)) {
            tactics.navigate(target.position(), 1.1D);
            action = "investigate_contact";
            return RUNNING;
        }
        if (OperativeGun.equipped(host) && host.gun().hasAmmunition(host) && host.distanceToSqr(target) <= Math.pow(OperativeGun.effectiveRange(host), 2) && OperativeGun.clearShot(host, target)) {
            host.getNavigation().stop();
            action = OperativeGun.clearShot(host, target) ? "shoot" : "hold_fire";
            return RUNNING;
        }
        if (host.distanceToSqr(target) <= 5.0D && host.canSee(target)) {
            host.getNavigation().stop();
            if (meleeCooldown <= 0 && !host.skills().locksWeapon()) {
                host.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                host.doHurtTarget(target);
                meleeCooldown = Math.max(-1, meleeCooldown) + Math.max(1, host.skills().meleeIntervalTicks(host));
            }
            action = "melee";
        } else {
            var path = host.getNavigation().createPath(target, 0);
            if (path != null && tactics.safePath(path, tactics.danger(host.position()))) host.getNavigation().moveTo(path, 1.1D);
            else host.getNavigation().stop();
            action = "approach";
        }
        return RUNNING;
    }

    private BehaviorNode.Status returnToOwnerOrPost() {
        if (host.skills().locksMovement()) return RUNNING;
        if (com.rzy.dealt_force_skills.raid.ai.RaidAiAffinity.pursuing(host)) {
            var destination = host.orderPosition();
            if (destination != null && host.distanceToSqr(destination) > 9) {
                if (host.tickCount >= nextRaidPathTick) {
                    nextRaidPathTick = host.tickCount + 20;
                    tactics.marchToward(destination);
                }
                action = "raid_pursuit";
                return RUNNING;
            }
        }
        var contact = hearing.investigation(host);
        if (contact == null) contact = host.skills().investigation();
        if (contact != null && host.withinOrders(contact) && host.position().distanceToSqr(contact) > 4) {
            tactics.navigate(contact, 1.0D);
            action = "investigate";
            return RUNNING;
        }
        var destination = host.orderPosition();
        if (destination == null || host.position().distanceToSqr(destination) <= 9.0D) {
            host.getNavigation().stop();
            return FAILURE;
        }
        tactics.navigate(destination, 1.0D);
        action = host.isGuarding() ? "return_to_post" : "follow";
        return RUNNING;
    }

    public void resetPerception() { hearing.clear(); }

    public String describe() { return action + ",last_skill=" + lastSkill + ",hearing=" + hearing.tracks(host.getTarget()); }
}
