package com.rzy.dealt_force_skills.compat.maid;

import com.mojang.logging.LogUtils;
import com.rzy.dealt_force_skills.ai.operative.*;
import com.rzy.dealt_force_skills.character.SkillSlot;
import com.rzy.dealt_force_skills.util.TargetingUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.Vec3;
import java.util.Arrays;
import java.util.Comparator;

/** Per-maid state, never a player's selection or a global UUID cache. */
public final class MaidSkillController {
    public static final String KEY = "dealt_force_skills.MaidRole";
    private static final String CORPS_RETURN = KEY + ".CorpsReturn";
    private static final SkillSlot[] SLOTS = {SkillSlot.ACTIVE_1, SkillSlot.ACTIVE_2, SkillSlot.CORE};
    private final MaidSkillHost host;
    private CompoundTag saved = new CompoundTag();
    private OperativeSkills skills;
    private boolean loaded, failed;
    private int nextSkillTick;
    private int lastVisualState = -2;
    private boolean hasSkillAim;
    private float skillYaw, skillPitch;
    public static final double NATIVE_FREEDOM = 128;

    public MaidSkillController(MaidSkillHost host) { this.host = host; }
    public String characterId() { return saved.getString("Character"); }
    public boolean failed() { return failed; }
    public OperativeSkills skills() {
        if (host.body().level().isClientSide) return null;
        try { load(); } catch (RuntimeException | LinkageError error) { fail(error); }
        return failed ? null : skills;
    }
    public boolean weaponLocked() {
        var executor = skills();
        return executor != null && OperativeSupport.downed(host.body());
    }

    /** Capability checks also cover ports that no longer use the vanilla tameable contract. */
    public boolean supported() { return host.body() instanceof TamableAnimal; }
    public void read(CompoundTag tag) {
        saved = tag.getCompound(KEY).copy();
        // Film revival bypasses Entity.load, including the loader's persistent-data restoration.
        if (tag.contains(CORPS_RETURN, 10)) host.body().getPersistentData().put(NativeCorpsArena.KEY, tag.getCompound(CORPS_RETURN).copy());
        skills = null; loaded = false; failed = false; nextSkillTick = 0; hasSkillAim = false;
    }
    public void write(CompoundTag tag) {
        try {
            if (skills != null && !failed) saved.put("Skills", skills.save());
        } catch (RuntimeException | LinkageError error) { fail(error); }
        if (!saved.isEmpty()) tag.put(KEY, saved.copy());
        else tag.remove(KEY);
        if (NativeCorpsArena.hasTicket(host.body())) tag.put(CORPS_RETURN, host.body().getPersistentData().getCompound(NativeCorpsArena.KEY).copy());
        else tag.remove(CORPS_RETURN);
    }
    private void load() {
        if (loaded || failed || !supported()) return;
        loaded = true;
        if (saved.isEmpty() || saved.getInt("Format") != 1 || !NativeOperativeSkills.supports(characterId())) return;
        skills = NativeOperativeSkills.create(characterId());
        skills.load(saved.getCompound("Skills"));
    }
    public boolean select(String id) {
        if (!supported() || failed || !NativeOperativeSkills.supports(id)) return false;
        try {
            load();
            if (id.equals(characterId()) && skills != null) return true; // No cooldown reset by reselecting.
            OperativeSkills replacement = NativeOperativeSkills.create(id);
            clear();
            if (failed) return false;
            saved.putInt("Format", 1); saved.putString("Character", id);
            skills = replacement; loaded = true;
            syncVisuals();
            write(new CompoundTag());
            return true;
        } catch (RuntimeException | LinkageError error) { fail(error); return false; }
    }
    public boolean clear() {
        if (failed) return false;
        try {
            load();
            if (skills != null) {
                skills.cancel(host);
                if (NativeCommanderSkills.inherited(skills, NativeTempestSkills.class) instanceof NativeTempestSkills s) s.clearDowned(host);
                if (skills instanceof NativeCatDadSkills s) s.clearDowned(host);
                if (skills instanceof NativeNikaidouSkills s) s.clear(host);
                if (skills instanceof NativeWitchSkills s) s.clear(host);
                if (skills instanceof NativeCorpsSkills s) s.clear(host);
                if (skills instanceof NativeSaeedSkills s) s.clear(host);
                if (skills instanceof NativeChamberSkills s) s.clear(host);
                if (skills instanceof NativeManbaSkills s) s.clear(host);
                if (skills instanceof NativeLexSkills s) s.clear(host);
            }
            OperativeGrowth.clear(host.body());
            skills = null; saved = new CompoundTag(); loaded = true; hasSkillAim = false;
            syncVisuals();
            return true;
        } catch (RuntimeException | LinkageError error) { fail(error); return false; }
    }
    public void tick() {
        if (failed || !supported() || host.body().level().isClientSide || !host.body().isAlive()) return;
        LivingEntity body = host.body();
        float yaw = body.getYRot(), pitch = body.getXRot(), head = body.getYHeadRot(), torso = body.yBodyRot;
        try {
            load();
            if (skills == null) return;
            // Skills retain their own facing across ticks without steering the native gun/look AI.
            if (hasSkillAim) { body.setYRot(skillYaw); body.setYHeadRot(skillYaw); body.setXRot(skillPitch); }
            skills.tick(host);
            syncVisuals();
            if (!NativeRaptorSkills.available(host) || OperativeSupport.downed(host.body())) return;
            Mob maid = (Mob)host.body();
            if (((TamableAnimal)maid).isInSittingPose() || skills.busy() || (maid.tickCount + maid.getId()) % 5 != 0) return;
            LivingEntity target = maid.getTarget();
            if (target != null && (!isEnemy(target) || !host.withinCombatRange(target.position()))) target = null;
            if (skills instanceof NativeLexSkills lex && maid.tickCount >= nextSkillTick && lex.plan(host, target)) {
                NativeLexEvents.action(maid); nextSkillTick = maid.tickCount + 4; return;
            }
            LivingEntity chosen = target;
            for (SkillSlot slot : Arrays.stream(SLOTS).sorted(Comparator.comparingDouble(
                    (SkillSlot value) -> utility(value, chosen)).reversed()).toList()) {
                double score = utility(slot, chosen);
                boolean emergency = score >= 100 && (maid.getHealth() < maid.getMaxHealth() * .5 || chosen == null
                        || chosen instanceof Mob mob && mob.getTarget() == maid);
                if ((maid.tickCount >= nextSkillTick || emergency) && score > 0 && skills.start(host, slot, chosen)) {
                    NativeLexEvents.action(maid);
                    nextSkillTick = maid.tickCount + (emergency ? 10 : 12 + maid.getRandom().nextInt(9));
                    break;
                }
            }
        } catch (RuntimeException | LinkageError error) { fail(error); }
        finally {
            skillYaw = body.getYRot(); skillPitch = body.getXRot(); hasSkillAim = skills != null;
            body.setYRot(yaw); body.setXRot(pitch); body.setYHeadRot(head); body.yBodyRot = torso;
        }
    }
    private double utility(SkillSlot slot, LivingEntity target) {
        if (skills instanceof NativeManbaSkills && slot == SkillSlot.ACTIVE_2) return 0;
        return OperativeSkillStrategy.score(host, skills, characterId(), slot, target, rangedStrategy(),
                OperativeGun.preferredRange(host.body()),
                com.tacz.guns.api.entity.IGunOperator.fromLivingEntity(host.body()).getSynReloadState().getStateType().isReloading(),
                host.body().hurtTime > 0);
    }
    public boolean withinOrders(Vec3 point) {
        return !failed && supported()
                && point != null && Double.isFinite(point.x) && Double.isFinite(point.y) && Double.isFinite(point.z);
    }
    /** Task selection, not the held item: every native task except melee uses the ranged policy. */
    public boolean rangedStrategy() { return !MaidNativeTasks.melee(host.body()); }

    /** Only relax native return checks. This is never an additional skill/order boundary. */
    public boolean freeToActAt(Vec3 point) {
        if (skills() == null || point == null) return false;
        if (NativeCorpsArena.hasTicket(host.body())) return true;
        Mob maid = (Mob)host.body();
        if (MaidNativeTasks.home(maid)) {
            double radius = Math.max(NATIVE_FREEDOM, maid.getRestrictRadius());
            return Vec3.atCenterOf(maid.getRestrictCenter()).distanceToSqr(point) <= radius * radius;
        }
        LivingEntity owner = ((TamableAnimal)maid).getOwner();
        return owner != null && owner.level() == maid.level() && owner.position().distanceToSqr(point) <= NATIVE_FREEDOM * NATIVE_FREEDOM;
    }
    public int sinevaRenderState() {
        if (!(skills() instanceof NativeSinevaSkills s)) return -1;
        return (s.shieldFront() ? 1 : 0) | (s.save().getDouble("Viewport") <= 0 ? 2 : 0) | (s.suitActive() ? 4 : 0);
    }
    public com.rzy.dealt_force_skills.network.S2C_SyncSinevaRenderState renderPacket() {
        int state = sinevaRenderState();
        return new com.rzy.dealt_force_skills.network.S2C_SyncSinevaRenderState(host.body().getId(), state >= 0,
                (state & 4) != 0, (state & 1) != 0, (state & 2) != 0);
    }
    private void syncVisuals() {
        int state = sinevaRenderState();
        if (state == lastVisualState) return;
        lastVisualState = state;
        com.rzy.dealt_force_skills.network.NetworkHandler.sendToTrackingAndSelf(renderPacket(), host.body());
    }

    public boolean isEnemy(LivingEntity target) {
        if (failed || !supported() || target == null || target == host.body() || target.level() != host.body().level()
                || !TargetingUtil.isTargetableLiving(target) || TargetingUtil.shouldSkipFriendlyControl(host.body(), target)
                || host.body().isAlliedTo(target)) return false;
        TamableAnimal maid = (TamableAnimal)host.body();
        LivingEntity owner = maid.getOwner();
        return NativeCorpsArena.selectedOpponent(maid, target) || target == maid.getTarget() || target == maid.getLastHurtByMob()
                || owner != null && (target == owner.getLastHurtByMob() || target == owner.getLastHurtMob())
                || target instanceof Enemy;
    }

    /** The few operative entity overrides that a maid does not inherit. */
    public float incoming(net.minecraft.world.damagesource.DamageSource source, float amount) {
        if (com.rzy.dealt_force_skills.team.TeamCombatRules.shouldCancelFriendlyDamage(host.body(), source)) return 0;
        var executor = skills();
        if (executor == null) return amount;
        try {
            if (NativeCommanderSkills.inherited(executor, NativeTempestSkills.class) instanceof NativeTempestSkills s && (s.recalling() || s.downed())
                    && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) return 0;
            if (NativeCommanderSkills.inherited(executor, NativeShepherdSkills.class) instanceof NativeShepherdSkills && source.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION)) amount *= .7F;
            if (NativeCommanderSkills.inherited(executor, NativeSinevaSkills.class) instanceof NativeSinevaSkills s) amount = s.incoming(host, source, amount);
            if (executor instanceof NativeCatDadSkills s) amount = s.incoming(host, source, amount);
            if (executor instanceof NativeGhrothSkills s) amount = s.incoming(host, source, amount);
            return amount;
        } catch (RuntimeException | LinkageError error) { fail(error); return amount; }
    }
    public void damaged() {
        try { if (host.body().isAlive() && skills() instanceof NativeTempestSkills s) s.triggerSpine(host); }
        catch (RuntimeException | LinkageError error) { fail(error); }
    }
    public boolean preventDeath(net.minecraft.world.damagesource.DamageSource source) {
        try {
            var executor = skills();
            return NativeCommanderSkills.inherited(executor, NativeTempestSkills.class) instanceof NativeTempestSkills s && s.preventDeath(host, source)
                    || executor instanceof NativeCatDadSkills c && c.preventDeath(host, source);
        } catch (RuntimeException | LinkageError error) { fail(error); return false; }
    }
    /** Restore carried equipment before the maid moves her inventory into a tombstone and saves a film. */
    public void prepareDeath() {
        try { if (skills() instanceof NativeChamberSkills chamber) chamber.stow(host); }
        catch (RuntimeException | LinkageError error) { fail(error); }
    }
    public float fall(float distance, float multiplier) {
        try {
            var executor = skills();
            if (NativeCommanderSkills.inherited(executor, NativeTempestSkills.class) instanceof NativeTempestSkills s && s.consumeFallProtection(host)) return 0;
            if (NativeCommanderSkills.inherited(executor, NativeVyronSkills.class) instanceof NativeVyronSkills s) {
                boolean propulsion = com.rzy.dealt_force_skills.ai.operative.OperativeGrowth.commander(host.body());
                s.onLanding(distance, propulsion); return propulsion ? 0 : multiplier * .64F;
            }
            return NativeCommanderSkills.inherited(executor, NativeUluruSkills.class) instanceof NativeUluruSkills ? multiplier * .64F : multiplier;
        } catch (RuntimeException | LinkageError error) { fail(error); return multiplier; }
    }
    /** null lets the maid's normal melee continue. */
    public Boolean attack(net.minecraft.world.entity.Entity target) {
        if (host.body().level().isClientSide) return null;
        LivingEntity body = host.body();
        float yaw = body.getYRot(), pitch = body.getXRot(), head = body.getYHeadRot(), torso = body.yBodyRot;
        boolean started = false;
        try {
            var executor = skills();
            if (executor instanceof NativeNikaidouSkills s && s.weaponActive())
                return started = target instanceof LivingEntity living && s.attack(host, living);
            if (executor instanceof NativeWitchSkills s && s.weaponActive())
                return started = target instanceof LivingEntity living && s.attack(host, living);
            return weaponLocked() ? Boolean.FALSE : null;
        } catch (RuntimeException | LinkageError error) { fail(error); return null; }
        finally {
            // Rejected native repeat attacks must retain the pending strike's own facing.
            if (started) { skillYaw = body.getYRot(); skillPitch = body.getXRot(); hasSkillAim = true; }
            body.setYRot(yaw); body.setXRot(pitch); body.setYHeadRot(head); body.yBodyRot = torso;
        }
    }

    public void fail(Throwable error) {
        if (failed) return;
        failed = true;
        // Retain the last readable state; one broken optional integration must not break entity saving/ticking.
        LogUtils.getLogger().warn("Skipping maid skills for {} ({})", host.body().getUUID(), characterId(), error);
    }
}
