package com.rzy.dealt_force_skills.raid.ai;

import com.rzy.dealt_force_skills.ai.operative.*;
import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.character.SkillSlot;
import com.rzy.dealt_force_skills.character.gambler.*;
import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.skill.SkillDamageHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Native raid gambler: passive, core lottery, Hakko and applicable same-dimension powers.
 * Player-only powers remain in the saved inventory; they are neither rerolled nor charged.
 * Final Bet and return-to-spawn are unavailable inside a raid. No synthetic player is used. */
public final class NativeRaidGamblerSkills implements OperativeSkills {
    private static final Set<GamblerSuperpower> UNAVAILABLE = EnumSet.of(
            GamblerSuperpower.RIDE_THE_WIND, GamblerSuperpower.DOLPHIN_HURRICANE);
    private static final String COPY = "DfsRaidGamblerCopy";
    private static final net.minecraft.resources.ResourceLocation SPEED = net.minecraft.resources.ResourceLocation.parse("dealt_force_skills:raid_gambler_jackpot");
    private CompoundTag state = new CompoundTag();
    private final Random random = new Random();
    private boolean reflecting;
    public static Set<GamblerSuperpower> unavailablePowers() { return Set.copyOf(UNAVAILABLE); }
    private static long now(OperativeHost host) { return host.serverLevel().getGameTime(); }
    private boolean live(String key, long now) { return state.getLong(key) > now; }
    private int chips() { return state.getInt("chips"); }
    private void chips(int value) { state.putInt("chips", Math.max(0, Math.min(999999, value))); }
    private void charge(String key, int amount) { state.putInt(key, Math.max(0, state.getInt(key) + amount)); }
    private boolean consume(String key) { if (state.getInt(key) <= 0) return false; charge(key, -1); return true; }
    private static double luck(OperativeHost host) {
        var effect = host.body().getEffect(MobEffects.LUCK);
        var bad = host.body().getEffect(MobEffects.UNLUCK);
        return (effect == null ? 0 : effect.getAmplifier() + 1) - (bad == null ? 0 : bad.getAmplifier() + 1);
    }
    private static boolean available(OperativeHost host, SkillSlot slot) {
        return NativeRaptorSkills.available(host) && !OperativeSupport.downed(host.body())
                && !OperativeElectronics.blocked(host, ModCharacters.GAMBLER_ID, slot);
    }
    private static boolean enemy(OperativeHost host, LivingEntity target) {
        return target != null && target.isAlive() && target.level() == host.body().level()
                && RaidAiAffinity.inside(host.body(), target.position()) && host.isEnemy(target);
    }
    @Override public double score(OperativeHost host, SkillSlot slot, LivingEntity target) {
        if (!available(host, slot)) return 0;
        long now = now(host);
        return switch (slot) {
            case ACTIVE_2 -> enemy(host, target) && chips() >= 4 && !live("drawReady", now)
                    && !live("drawNext", now) ? 90 : 0;
            case CORE -> !state.getBoolean("core") && chips() > 0 && !live("jackpot", now)
                    && host.body().getHealth() < host.body().getMaxHealth() * .7F ? 100 : 0;
            default -> 0; // Final Bet requires the forbidden arena transition.
        };
    }
    @Override public boolean start(OperativeHost host, SkillSlot slot, LivingEntity target) {
        if (score(host, slot, target) <= 0) return false;
        long now = now(host);
        if (slot == SkillSlot.CORE) { state.putBoolean("core", true); state.putLong("coreNext", now + 20); return true; }
        if (state.getInt("draws") <= 0 || !live("drawExpiry", now)) {
            state.putInt("draws", 8); state.putInt("startChips", chips()); state.putInt("spent", 0);
            state.putInt("noHakko", 0); state.putBoolean("free", false);
        }
        draw(host, true, 0); state.putLong("drawNext", now + 20); return true;
    }
    @Override public void tick(OperativeHost host) {
        long now = now(host);
        if (state.getBoolean("clearShield") && !live("invulnerable", now)) {
            state.putInt("shield", 0); state.putBoolean("clearShield", false);
        }
        if (state.getInt("draws") > 0 && !live("drawExpiry", now)) {
            state.putInt("draws", 0); state.putLong("drawReady", now + 2400);
        }
        if (live("jackpot", now)) {
            host.body().setHealth(host.body().getMaxHealth());
            jackpotSpeed(host, true);
        } else jackpotSpeed(host, false);
        if (state.getBoolean("core") && now >= state.getLong("coreNext") && available(host, SkillSlot.CORE)) {
            if (chips() <= 0) state.putBoolean("core", false);
            else { chips(chips() - 1); state.putLong("coreNext", now + 20); coreDraw(host); }
        }
        if (host.body().tickCount % 20 == 0 && available(host, SkillSlot.ACTIVE_2)) usePower(host);
    }
    /** Called once by the shared damage event before armor, never also from Entity.hurt. */
    public float incoming(OperativeHost host, DamageSource source, float amount) {
        if (!Float.isFinite(amount) || amount <= 0 || reflecting) return amount;
        long now = now(host);
        if (live("invulnerable", now) || live("unbreakable", now)) return 0;
        if (consume("potato")) { reflect(host, source, amount + 60); return 0; }
        if (consume("reflect")) { reflect(host, source, amount * 1.5F); return 0; }
        if (consume("ignore")) return 0;
        if (consume("reduce")) amount *= .5F;
        int gain = shieldGain(luck(host)); chips(chips() + gain); charge("shield", gain);
        if (state.getInt("shield") >= 100) {
            state.putInt("shield", 100); state.putLong("invulnerable", now + 40);
            state.putBoolean("clearShield", true); addRandomPower(host); return 0;
        }
        int pity = state.getInt("incomingPity");
        boolean best = pity >= 49 || random.nextDouble() < Math.min(1, .02 + pity * .03);
        state.putInt("incomingPity", best ? 0 : pity + 1);
        amount *= best ? .1F : .1F + random.nextFloat() * 1.9F;
        return live("jackpot", now) ? Math.min(amount, host.body().getHealth() * .5F) : amount;
    }
    public float outgoing(OperativeHost host, LivingEntity target, DamageSource source, float amount) {
        if (amount <= 0 || source.is(SkillDamageHelper.TRUE_SKILL_DAMAGE) || !enemy(host, target)) return amount;
        int pity = state.getInt("outgoingPity");
        boolean best = pity >= 49 || random.nextDouble() < Math.min(1, .02 + pity * .04);
        state.putInt("outgoingPity", best ? 0 : pity + 1);
        amount *= best ? 2.5F : .5F + random.nextFloat() * 2;
        long now = now(host);
        if (live("boostUntil", now)) amount *= 1 + state.getInt("boost") / 1000F;
        if (live("spentUntil", now)) amount *= 1 + state.getInt("spentDamage") * .04F;
        if (live("stink", now) && target instanceof Player) amount *= 2;
        else if (live("stink", now) && amount > 3) forceKill(host, target);
        if (live("ice", now)) target.addEffect(new MobEffectInstance(ModEffects.N_TWO_FROZEN, 40, 0));
        return amount;
    }
    public static float adjustIncoming(LivingEntity victim, DamageSource source, float amount) {
        if (victim.level() instanceof net.minecraft.server.level.ServerLevel level && RaidAiAffinity.instance(victim) != null) {
            for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class, victim.getBoundingBox().inflate(6)))
                if (other != victim && other.distanceToSqr(victim) <= 36 && sameTeam(other, victim)
                        && other instanceof OperativeHost host && host.skills() instanceof NativeRaidGamblerSkills gambler
                        && gambler.live("orange", level.getGameTime())) return 0;
        }
        return victim instanceof OperativeHost host && host.skills() instanceof NativeRaidGamblerSkills gambler
                ? gambler.incoming(host, source, amount) : amount;
    }
    public static float adjustOutgoing(LivingEntity attacker, LivingEntity victim, DamageSource source, float amount) {
        return attacker instanceof OperativeHost host && host.skills() instanceof NativeRaidGamblerSkills gambler
                ? gambler.outgoing(host, victim, source, amount) : amount;
    }
    /** Invoke on final applied non-true damage. Copies carry true damage to prevent recursion. */
    public static void afterDamage(LivingEntity attacker, LivingEntity victim, DamageSource source, float amount) {
        if (amount <= 0 || !Float.isFinite(amount) || source.is(SkillDamageHelper.TRUE_SKILL_DAMAGE)
                || !(attacker instanceof OperativeHost host) || !enemy(host, victim)) return;
        long now = now(host);
        if (host.skills() instanceof NativeRaidGamblerSkills gambler) {
            if (gambler.live("familiar", now) && now >= gambler.state.getLong("familiarNext")) {
                damage(host, victim, 2); gambler.addRandomPower(host); gambler.state.putLong("familiarNext", now + 100);
            }
            if (gambler.live("ice", now)) {
                Vec3 behind = victim.position().subtract(victim.getLookAngle().normalize().scale(2));
                for (LivingEntity other : host.serverLevel().getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(behind, behind).inflate(1.5), e -> e != victim && enemy(host, e))) damage(host, other, amount);
            }
        }
        CompoundTag copy = attacker.getPersistentData().getCompound(COPY);
        int count = (copy.getLong("triple") > now ? 2 : 0) + (copy.getLong("radiant") > now ? 1 : 0) + (copy.getLong("viral") > now ? 1 : 0);
        for (int i = 0; i < count && victim.isAlive(); i++) { victim.invulnerableTime = 0; damage(host, victim, amount); }
        if (copy.getLong("possession") > now && copy.hasUUID("owner") && victim.isAlive()
                && host.serverLevel().getEntity(copy.getUUID("owner")) instanceof LivingEntity owner
                && owner.isAlive() && sameTeam(owner, attacker) && owner instanceof OperativeHost ownerHost) {
            victim.invulnerableTime = 0; damage(ownerHost, victim, amount * .5F);
        }
    }
    public boolean preventsDeath(OperativeHost host) {
        long now = now(host);
        if (!live("invulnerable", now) && !live("unbreakable", now) && !live("jackpot", now)) return false;
        host.body().setHealth(host.body().getMaxHealth()); return true;
    }
    public boolean deniesHarmfulEffect(OperativeHost host) { return live("jackpot", now(host)); }
    private void reflect(OperativeHost host, DamageSource source, float amount) {
        if (!(source.getEntity() instanceof LivingEntity attacker) || !enemy(host, attacker)) return;
        reflecting = true;
        try { damage(host, attacker, amount); } finally { reflecting = false; }
    }
    private int shieldGain(double luck) {
        double[] weights = new double[7]; double rest = 0;
        for (int i = 0; i < 7; i++) if (i != 3) { weights[i] = Math.max(0, 1D / 7 + (i < 3 ? -.03 : .03) * luck); rest += weights[i]; }
        weights[3] = 1D / 7;
        for (int i = 0; i < 7; i++) if (i != 3) weights[i] = weights[i] / rest * (6D / 7);
        double roll = random.nextDouble(); for (int i = 0; i < 7; i++) if ((roll -= weights[i]) <= 0) return i + 1;
        return 4;
    }
    private void draw(OperativeHost host, boolean counted, int depth) {
        if (depth > 6 || counted && !consume("draws")) return;
        long now = now(host);
        GamblerDrawEffect effect = GamblerDrawEffect.roll(random, luck(host), state.getInt("noHakko") >= 7);
        boolean free = state.getBoolean("free"); state.putBoolean("free", false);
        if (!free && chips() < effect.cost()) { state.putInt("draws", 0); state.putLong("drawReady", now + 2400); return; }
        if (!free) chips(chips() - effect.cost());
        charge("spent", effect.cost()); state.putInt("noHakko", effect.isHakko() ? 0 : state.getInt("noHakko") + 1);
        state.putLong("drawExpiry", now + 600);
        switch (effect) {
            case LAND_SEA_AIR -> { loseFivePercent(); draw(host, false, depth + 1); }
            case MILITARISM -> { charge("ignore", 1); loseFivePercent(); }
            case FIFTH_INFANTRY -> { state.putLong("spentUntil", now + 600); state.putInt("spentDamage", Math.max(1, state.getInt("spent"))); loseFivePercent(); }
            case FORTIETH_CAVALRY -> { state.putBoolean("free", true); loseFivePercent(); }
            case WIDE_LAND -> { int gain = Math.max(0, state.getInt("startChips") - chips()); chips(chips() + gain); if (gain > 0) { state.putLong("boostUntil", now + 600); charge("boost", gain * 20); } }
            case DESPERATE_PLAN -> { enemies(host, 30).forEach(e -> damage(host, e, 10)); loseFivePercent(); }
            case IMPERIAL_POWER -> { enemies(host, 30).forEach(e -> { e.removeAllEffects(); damage(host, e, 8); }); }
            case VIRAL_SPREAD -> { charge("draws", 1); teamBuff(host, 600, 0); for (LivingEntity ally : allies(host, 30)) copyUntil(ally, "viral", now + 600); }
            case THINKING_ZOMBIE -> { addRandomPower(host); addRandomPower(host); }
            case CROWDFUND_REVIVAL -> charge("draws", 2);
            case LECTURE_ZOMBIE -> state.putBoolean("freePower", true);
            case HAKKO_ICHIIU -> { draw(host, false, depth + 1); draw(host, false, depth + 1); addRandomPower(host); addRandomPower(host); chips(chips() + state.getInt("spent") * 2); }
        }
        if (state.getInt("draws") <= 0) state.putLong("drawReady", now + 2400);
    }
    private void loseFivePercent() { chips(chips() - (int) Math.ceil(chips() * .05)); }
    private void coreDraw(OperativeHost host) {
        int pity = state.getInt("corePity"); double luck = Math.max(0, Math.min(1, luck(host) / 256));
        double none = Math.max(0, 25 * (1 - .9 * luck)), small = Math.max(0, 50 * (1 - .35 * luck));
        double medium = 15 + 35 * luck, big = 9.996 + 40 * luck;
        double jackpot = .004 + pity * (.004 + luck * .010) + 12 * luck * luck;
        double roll = random.nextDouble() * (none + small + medium + big + jackpot);
        if (pity < Math.max(40, 238 - (int) Math.floor(luck * 180))) {
            charge("corePity", 1);
            if ((roll -= none) < 0) return;
            if ((roll -= small) < 0) { heal(host.body(), .06F); return; }
            if ((roll -= medium) < 0) { heal(host.body(), .12F); charge("ignore", 1); return; }
            if ((roll -= big) < 0) { heal(host.body(), .20F); chips(chips() + 2); return; }
        }
        state.putBoolean("core", false); state.putInt("corePity", 0); state.putLong("jackpot", now(host) + 3900);
        host.body().setHealth(host.body().getMaxHealth());
        host.body().addEffect(new MobEffectInstance(ModEffects.BURNING_HEART, 3900, 0));
        host.body().addEffect(new MobEffectInstance(MobEffects.LUCK, 4100, 9));
    }
    private void addRandomPower(OperativeHost host) {
        double red = Math.max(0, Math.min(1, .15 + luck(host) * .05));
        double purple = Math.max(0, Math.min(1 - red, .5 - luck(host) * .05));
        double roll = random.nextDouble();
        addPower(GamblerSuperpower.randomByRarity(random, roll < red ? GamblerRarity.RED : roll < 1 - purple ? GamblerRarity.GOLD : GamblerRarity.PURPLE));
    }
    private void addPower(GamblerSuperpower power) { charge("power." + power.id(), 1); }
    private void usePower(OperativeHost host) {
        if (!state.getBoolean("freePower") && chips() < 1) return;
        LivingEntity enemyTarget = host.body() instanceof Mob mob ? mob.getTarget() : null;
        for (GamblerSuperpower power : GamblerSuperpower.values()) {
            LivingEntity target = enemyTarget;
            if (UNAVAILABLE.contains(power) || state.getInt("power." + power.id()) <= 0) continue;
            if (power.targetMode() == GamblerTargetMode.LIVING || power.targetMode() == GamblerTargetMode.NON_PLAYER) {
                if (!enemy(host, target) || host.body().distanceToSqr(target) > 90 * 90 || !host.canSee(target)) continue;
                if (power.targetMode() == GamblerTargetMode.NON_PLAYER && target instanceof Player) continue;
            }
            if (power == GamblerSuperpower.TOMBSTONE_CODE && (!(target instanceof Player) || !enemy(host, target) || !host.canSee(target))) continue;
            if (power == GamblerSuperpower.BUBBLES || power == GamblerSuperpower.POSSESSION || power == GamblerSuperpower.RADIANT) {
                target = allies(host, 90).stream().filter(e -> e != host.body()).findFirst().orElse(null);
                if (target == null) continue;
            }
            if (!state.getBoolean("freePower")) chips(chips() - 1);
            consume("power." + power.id()); applyPower(host, power, target); return;
        }
    }
    private void applyPower(OperativeHost host, GamblerSuperpower power, LivingEntity target) {
        LivingEntity self = host.body(); long now = now(host);
        switch (power) {
            case EARTHSHAKING_SLAM -> { if (target instanceof Player) target.addEffect(new MobEffectInstance(ModEffects.STUN, 120, 0)); else forceKill(host, target); }
            case TRIPLE_THREAT -> copyUntil(self, "triple", now + 700);
            case SHRINK_RAY -> target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 1200, target instanceof Player ? 1 : 4));
            case VITALITY_BURST -> { damage(host, target, 30); heal(self, .3F); }
            case FROZEN_TUNDRA -> cone(host).forEach(e -> e.addEffect(new MobEffectInstance(ModEffects.N_TWO_FROZEN, 200, 0)));
            case INSPIRATION -> { for (int i = 0; i < 3; i++) addPower(GamblerSuperpower.randomAtLeastGold(random)); }
            case WITCH_FAMILIAR -> { state.putLong("familiar", now + 600); state.putLong("familiarNext", now); }
            case MISSILE_BARRAGE -> missiles(host, target, 30, 0);
            case OCTO_THROWER -> charge("reflect", 2);
            case ICE_MOON -> state.putLong("ice", now + 600);
            case SCARY_SHAPER_1000 -> { addRandomPower(host); addRandomPower(host); state.putBoolean("freePower", true); }
            case BISECT -> { if (attribute(target, Attributes.ATTACK_DAMAGE) > attribute(self, Attributes.ATTACK_DAMAGE) || attribute(target, Attributes.ATTACK_DAMAGE) > 250) forceKill(host, target); }
            case TELEPATHY -> { addRandomPower(host); addRandomPower(host); }
            case STINK_CLOUD -> state.putLong("stink", now + 300);
            case HEROIC_HEALING -> heal(self, .5F);
            case POSSESSION -> { buff(target, 600, 1); copyUntil(target, "possession", now + 600); target.getPersistentData().getCompound(COPY).putUUID("owner", self.getUUID()); }
            case ZOMBIE_MORALE -> buff(self, 1200, 1);
            case TOMBSTONE_CODE -> { target.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 4)); target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 9)); target.addEffect(new MobEffectInstance(ModEffects.STUN, 200, 0)); }
            case BRUTE_FORCE -> self.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 1200, 0));
            case STONE_WALL -> self.addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, 1200, 2));
            case DANCE_OFF -> charge("ignore", 2);
            case EVAPORATE -> { if (target.getHealth() < target.getMaxHealth()) forceKill(host, target); }
            case LIGHTNING_ARROW -> damage(host, target, 25);
            case ACID_RAIN -> cone(host).forEach(e -> { e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 1200, 0)); e.addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, 1200, -1)); });
            case SUMMONING -> { addPower(GamblerSuperpower.randomByRarity(random, GamblerRarity.PURPLE)); addPower(GamblerSuperpower.randomByRarity(random, GamblerRarity.PURPLE)); }
            case PRECISION_STRIKE -> damage(host, target, 50);
            case SOLAR_BURN -> { damage(host, target, 20); chips(chips() + 20); }
            case UNBREAKABLE -> state.putLong("unbreakable", now + 600);
            case DEVOUR -> { if (target.getHealth() < self.getHealth() * 15) forceKill(host, target); }
            case THROW_POTATO -> charge("potato", 1);
            case ORANGE_PEEL_SHIELD -> state.putLong("orange", now + 400);
            case POWER_PUNCH -> cone(host).forEach(e -> damage(host, e, 20));
            case INFLATING_MUSHROOM -> { damage(host, target, 20); state.putLong("boostUntil", now + 600); state.putInt("boost", Math.max(400, state.getInt("boost"))); }
            case SHEEPIFY -> weaken(target, true);
            case BLAZING_BARK -> self.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 600, 3));
            case GENE_AMPLIFICATION -> { buff(self, 1200, 1); self.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 1200, 0)); self.addEffect(new MobEffectInstance(MobEffects.CONDUIT_POWER, 1200, 0)); }
            case UPROOT_FLAG -> swap(host, target);
            case LIEUTENANT_CARROT -> { addPower(GamblerSuperpower.randomByRarity(random, GamblerRarity.PURPLE)); charge("ignore", 1); }
            case LIGHTSPEED_SEED -> { addPower(GamblerSuperpower.randomByRarity(random, GamblerRarity.GOLD)); addPower(GamblerSuperpower.randomByRarity(random, GamblerRarity.GOLD)); }
            case METEOR_IMPACT -> { damage(host, target, 20); target.igniteForSeconds(20); }
            case GIGANTIFY -> { self.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 600, 0)); self.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 600, 0)); }
            case RADIANT -> copyUntil(target, "radiant", now + 600);
            case POLYMORPH -> weaken(target, false);
            case FREEZE -> target.addEffect(new MobEffectInstance(ModEffects.N_TWO_FROZEN, 100, 0));
            case WEED_ATTACK -> { target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 1200, 0)); target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 1200, 0)); }
            case SPORE_OVERLOAD -> state.putInt("reduce", 6);
            case RAINSTORM -> teamBuff(host, 2400, 0);
            case HOLOGRAM_FLOWER -> addPower(GamblerSuperpower.randomByRarity(random, GamblerRarity.RED));
            case ROOTING_WALL -> target.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 600, 4));
            case BUBBLES -> { swap(host, target); target.addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, 1200, 2)); }
            case SCORCHED_EARTH -> cone(host).forEach(e -> e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 1200, 0)));
            case FOUNTAIN -> allies(host, 6).forEach(e -> heal(e, .4F));
            default -> throw new IllegalStateException("Unavailable native gambler power selected: " + power);
        }
    }
    private static double attribute(LivingEntity entity, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute) {
        var instance = entity.getAttribute(attribute); return instance == null ? 0 : instance.getValue();
    }
    private static void weaken(LivingEntity target, boolean floor) {
        if (target instanceof Player) return;
        for (var type : List.of(Attributes.MAX_HEALTH, Attributes.ATTACK_DAMAGE)) {
            var attribute = target.getAttribute(type);
            if (attribute != null) attribute.setBaseValue(floor ? 1 : Math.max(1, attribute.getBaseValue() * .25));
        }
        target.setHealth(Math.min(target.getHealth(), target.getMaxHealth()));
    }
    private static void swap(OperativeHost host, LivingEntity target) {
        if (target == null || target.level() != host.body().level() || !RaidAiAffinity.inside(host.body(), target.position())
                || !Objects.equals(RaidAiAffinity.instance(host.body()), RaidAiAffinity.instance(target))) return;
        Vec3 from = host.body().position(), to = target.position();
        host.body().teleportTo(to.x, to.y, to.z); target.teleportTo(from.x, from.y, from.z);
        host.body().fallDistance = 0; target.fallDistance = 0;
    }
    private static List<LivingEntity> enemies(OperativeHost host, double radius) {
        return host.serverLevel().getEntitiesOfClass(LivingEntity.class, host.body().getBoundingBox().inflate(radius),
                e -> enemy(host, e) && e.distanceToSqr(host.body()) <= radius * radius);
    }
    private static List<LivingEntity> cone(OperativeHost host) {
        Vec3 direction = host.body().getLookAngle();
        return enemies(host, 90).stream().filter(e -> e.position().subtract(host.body().position()).normalize().dot(direction) >= Math.cos(Math.toRadians(70))).toList();
    }
    private static List<LivingEntity> allies(OperativeHost host, double radius) {
        CompoundTag self = RaidAiAffinity.identity(host.body());
        return host.serverLevel().getEntitiesOfClass(LivingEntity.class, host.body().getBoundingBox().inflate(radius), e -> {
            CompoundTag other = RaidAiAffinity.identity(e);
            return e.isAlive() && e.distanceToSqr(host.body()) <= radius * radius && (e == host.body()
                    || RaidAiAffinity.allies(host.body(), e) && self.hasUUID("team") && other.hasUUID("team") && self.getUUID("team").equals(other.getUUID("team")));
        });
    }
    private static void teamBuff(OperativeHost host, int ticks, int amplifier) { allies(host, 30).forEach(e -> buff(e, ticks, amplifier)); }
    private static void buff(LivingEntity target, int ticks, int amplifier) { target.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, ticks, amplifier)); target.addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, ticks, amplifier)); }
    private static void heal(LivingEntity target, float fraction) { target.heal(target.getMaxHealth() * fraction); }
    private static void damage(OperativeHost host, LivingEntity target, float amount) {
        if (enemy(host, target)) SkillDamageHelper.hurtUnscaled(target, SkillDamageHelper.trueDamage(host.serverLevel(), host.body(), host.body()), amount);
    }
    private static void missiles(OperativeHost host, LivingEntity target, float amount, int depth) {
        if (target == null || amount <= 1 || depth > 5) return;
        damage(host, target, amount);
        for (LivingEntity other : host.serverLevel().getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(6), e -> e != target && enemy(host, e))) missiles(host, other, amount * .5F, depth + 1);
    }
    private static void copyUntil(LivingEntity target, String key, long until) {
        CompoundTag tag = target.getPersistentData().getCompound(COPY); tag.putLong(key, Math.max(tag.getLong(key), until)); target.getPersistentData().put(COPY, tag);
    }
    private static boolean sameTeam(LivingEntity a, LivingEntity b) {
        CompoundTag left = RaidAiAffinity.identity(a), right = RaidAiAffinity.identity(b);
        return RaidAiAffinity.allies(a, b) && left.hasUUID("team") && right.hasUUID("team") && left.getUUID("team").equals(right.getUUID("team"));
    }
    private static void jackpotSpeed(OperativeHost host, boolean active) {
        var attribute = host.body().getAttribute(Attributes.MOVEMENT_SPEED); if (attribute == null) return;
        if (active && attribute.getModifier(SPEED) == null) attribute.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(SPEED, .25, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        else if (!active) attribute.removeModifier(SPEED);
    }
    public void clear(OperativeHost host) { jackpotSpeed(host, false); host.body().getPersistentData().remove(COPY); state = new CompoundTag(); }
    private static void forceKill(OperativeHost host, LivingEntity target) {
        if (!(target instanceof Player) && enemy(host, target) && com.rzy.dealt_force_skills.boss.BossCombatRules.canInstantKill(target))
            damage(host, target, target.getHealth() + target.getAbsorptionAmount() + 1000);
    }
    @Override public boolean busy() { return false; }
    @Override public void cancel(OperativeHost host) { state.putBoolean("core", false); }
    @Override public CompoundTag save() { return state.copy(); }
    @Override public void load(CompoundTag tag) { state = tag.copy(); }
    @Override public String describe() { return "raid gambler: passive/core/Hakko; " + (GamblerSuperpower.values().length - UNAVAILABLE.size()) + " native powers, " + UNAVAILABLE.size() + " retained player-only powers"; }
}
