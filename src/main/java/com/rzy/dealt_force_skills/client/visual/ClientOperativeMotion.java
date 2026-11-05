package com.rzy.dealt_force_skills.client.visual;

import com.rzy.dealt_force_skills.entity.OperativeEntity;
import com.rzy.dealt_force_skills.motion.TacticalMotion;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** The player's pose sampler, driven by native snapshots and actual client displacement. */
public final class ClientOperativeMotion {
    private static final java.util.Map<OperativeEntity, Entry> STATES = new java.util.WeakHashMap<>();
    private ClientOperativeMotion() { }
    public static ClientTacticalMotion.Frame frame(OperativeEntity bot, float partial) {
        if (!bot.isAlive() || bot.isRemoved()) { STATES.remove(bot); return null; }
        TacticalMotion kind = bot.tacticalMotion();
        double now = bot.level().getGameTime() + (double)partial;
        Entry e = STATES.get(bot);
        Vec3 position = new Vec3(Mth.lerp(partial, bot.xo, bot.getX()),
                Mth.lerp(partial, bot.yo, bot.getY()), Mth.lerp(partial, bot.zo, bot.getZ()));
        if (kind != TacticalMotion.NONE && (e == null || e.kind != kind || e.start != bot.tacticalStart())) {
            e = new Entry(); e.kind = kind; e.start = bot.tacticalStart(); e.duration = bot.tacticalDuration();
            e.yaw = bot.tacticalYaw(); e.position = position; STATES.put(bot, e);
        }
        if (e == null) return null;
        if (kind == TacticalMotion.NONE && e.stopped < 0) {
            e.stopped = now;
            e.frozenAge = (float)Math.max(0, now - e.start);
            var outgoing = e.kind.sample(e.frozenAge, e.duration, 0, e.distance, e.strength, 0);
            e.recovery = TacticalMotion.recoveryTicks(e.kind, outgoing);
        }
        if (e.stopped >= 0 && now - e.stopped >= e.recovery) { STATES.remove(bot); return null; }
        if (e.stopped < 0) {
            Vec3 delta = position.subtract(e.position);
            double travel = e.kind == TacticalMotion.SWIM ? delta.length() : delta.horizontalDistance();
            if (travel < 4) e.distance += (float)travel;
            e.position = position;
            Vec3 tick = bot.position().subtract(bot.xo, bot.yo, bot.zo);
            e.strength = Mth.clamp((float)(e.kind == TacticalMotion.SWIM ? tick.length()/.12 : tick.horizontalDistance()/.18), 0, 1);
        }
        float age = e.stopped >= 0 ? e.frozenAge : (float)Math.max(0, now - e.start);
        var pose = e.kind.sample(age, e.duration, 0, e.distance, e.strength, 0);
        if (e.stopped >= 0) TacticalMotion.release(e.kind, pose, (float)((now - e.stopped) / e.recovery));
        return new ClientTacticalMotion.Frame(e.kind, pose, e.yaw);
    }
    private static final class Entry {
        TacticalMotion kind;
        long start;
        int duration, recovery;
        float distance, strength, yaw, frozenAge;
        double stopped = -1;
        Vec3 position;
    }
}
