package com.rzy.dealt_force_skills.client.visual;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.motion.TacticalMotion;
import com.rzy.dealt_force_skills.network.S2C_TacticalMotion;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT)
public final class ClientTacticalMotion {
    private static final Map<UUID, Entry> STATES = new HashMap<>();
    private static Object level;
    private ClientTacticalMotion() { }

    public static void receive(S2C_TacticalMotion m) {
        checkLevel();
        if (level == null) return;
        long now = Minecraft.getInstance().level.getGameTime();
        Entry old = STATES.get(m.playerId());
        if (old != null && m.sequence() < old.sequence) return;
        TacticalMotion kind = TacticalMotion.byId(m.action());
        if (kind == TacticalMotion.NONE) {
            if (Minecraft.getInstance().player != null && Minecraft.getInstance().player.getUUID().equals(m.playerId()))
                com.rzy.dealt_force_skills.client.ClientTempestRollVisuals.stopRoll();
            if (old != null && old.stopped < 0) {
                old.age = old.age(now, 0); old.received = now; old.stopped = now;
                old.recovery = TacticalMotion.recoveryTicks(old.kind,
                        old.kind.sample(old.age, old.duration, old.phase, old.distance, 1, old.impact));
                old.sequence = m.sequence();
            }
            return;
        }
        if (old != null && old.sequence == m.sequence() && old.kind == kind) {
            old.age = Math.max(old.age(now, 0), Math.max(0, m.age()));
            old.received = now; old.yaw = m.yaw(); old.lastPacket = now;
            old.duration = Math.max(1, m.duration()); old.phase = m.phase(); old.impact = m.impactTick();
            return;
        }
        if (Minecraft.getInstance().player != null && Minecraft.getInstance().player.getUUID().equals(m.playerId())) {
            if (kind == TacticalMotion.ROLL && m.age() < m.duration())
                com.rzy.dealt_force_skills.client.ClientTempestRollVisuals.startRoll(m.duration() - m.age());
            else com.rzy.dealt_force_skills.client.ClientTempestRollVisuals.stopRoll();
        }
        Entry e = new Entry(); e.kind = kind; e.sequence = m.sequence();
        e.age = Math.max(0, m.age()); e.duration = Math.max(1, m.duration());
        e.phase = m.phase(); e.yaw = m.yaw(); e.impact = m.impactTick();
        e.received = now; e.lastPacket = now;
        Player current = Minecraft.getInstance().level.getPlayerByUUID(m.playerId());
        e.position = current != null ? current.position() : old == null ? null : old.position;
        STATES.put(m.playerId(), e);
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        checkLevel();
        if (level == null) return;
        var world = Minecraft.getInstance().level;
        long now = world.getGameTime();
        STATES.entrySet().removeIf(item -> {
            Entry e = item.getValue(); Player p = world.getPlayerByUUID(item.getKey());
            if (p == null) return now - e.lastPacket > 40;
            if (!valid(p) || now - e.lastPacket > 40 || (e.stopped >= 0 && now - e.stopped >= e.recovery)) return true;
            if (e.stopped >= 0) return false; // Freeze the outgoing limbs as well as its animation age.
            Vec3 position = p.position();
            if (e.position != null) {
                Vec3 delta = position.subtract(e.position);
                float distance = (float)(e.kind == TacticalMotion.SWIM ? delta.length() : delta.horizontalDistance());
                e.step = distance < 4 ? distance : 0; // Teleports are not strides.
                e.distance += e.step;
            }
            e.position = position;
            return false;
        });
    }

    private static void checkLevel() {
        Object current = Minecraft.getInstance().level;
        if (current != level) {
            com.rzy.dealt_force_skills.client.ClientTempestRollVisuals.stopRoll();
            STATES.clear(); level = current;
        }
    }

    public static Frame frame(Player player, float partial) {
        checkLevel();
        Entry e = STATES.get(player.getUUID());
        if (!valid(player) || com.rzy.dealt_force_skills.character.sineva.SinevaKnockdownState.active(player)) return null;
        if (e == null) return heldTool(player);
        long now = player.level().getGameTime();
        if (now - e.lastPacket > 40 || e.stopped >= 0 && now-e.stopped+partial >= e.recovery) return heldTool(player);
        float age = e.age(now, partial);
        if (!e.kind.looping() && !e.kind.sustained() && !(e.kind == TacticalMotion.DIVE && e.phase == 0)
                && age > e.duration + 4) return heldTool(player);
        float strength = TacticalMotion.clamp(e.step / (e.kind == TacticalMotion.SWIM ? .12F : .18F), 0, 1);
        TacticalMotion.Pose pose = e.kind.sample(age, e.duration, e.phase,
                e.distance + e.step * partial, strength, e.impact);
        if (e.stopped >= 0) {
            TacticalMotion.release(e.kind, pose, (now - e.stopped + partial) / e.recovery);
        }
        return new Frame(e.kind, pose, e.yaw);
    }

    private static Frame heldTool(Player player) {
        return com.rzy.dealt_force_skills.client.character.ClientHeldToolVisualState.visual(player)
                == com.rzy.dealt_force_skills.skill.HeldToolVisual.NONE ? null
                : new Frame(TacticalMotion.HOLD_TOOL, TacticalMotion.HOLD_TOOL.sample(4, 20, 0, 0, 1, 0), player.getYRot());
    }

    private static boolean valid(Player p) {
        return p.isAlive() && !p.isRemoved() && !p.isPassenger() && !p.isSleeping()
                && !p.isSpectator() && !p.isFallFlying();
    }

    public record Frame(TacticalMotion kind, TacticalMotion.Pose pose, float yaw) { }
    private static final class Entry {
        TacticalMotion kind;
        long sequence, received, lastPacket, stopped = -1;
        float age, yaw, distance, step;
        int duration, phase, impact, recovery = 4;
        Vec3 position;
        float age(long now, float partial) { return age + (stopped < 0 ? now - received + partial : 0); }
    }
}
