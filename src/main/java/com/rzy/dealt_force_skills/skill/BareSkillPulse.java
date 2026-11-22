package com.rzy.dealt_force_skills.skill;

import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.S2C_SkillModelVisual;
import net.minecraft.server.level.ServerPlayer;

public enum BareSkillPulse {
    OVERLOAD(0x100, 9),
    THRUST(0x101, 6),
    TUCK_ROLL(0x102, 7),
    ANCHOR_SET(0x103, 8),
    RECALL_PULL(0x104, 7),
    ADRENALINE(0x105, 10),
    EVADE(0x106, 6),
    WARM_ENFORCEMENT(0x107, 9),
    BATON_BRACE(0x108, 7),
    STARS_ORBIT(0x109, 11),
    JUSTICE_SEAL(0x10a, 8),
    HIGH_NOON(0x10b, 10),
    HAKKO_DRAW(0x10c, 7),
    CORPS_CHALLENGE(0x10d, 9),
    DUEL_BECKON(0x10e, 10),
    ROAD_SIGNAL(0x10f, 10),
    CORRECTION(0x110, 7),
    TIME_ANCHOR(0x111, 10),
    ERASE_ERROR(0x112, 8),
    HEAT_RELEASE(0x113, 6),
    COUNTDOWN(0x114, 10),
    BLOODLUST(0x115, 8),
    BLOOD_RELOAD(0x116, 7),
    SPACE_OPEN(0x117, 10),
    ARENA_DEAL(0x118, 9),
    SCHOLAR_RECORD(0x119, 9),
    ANCHOR_BLINK(0x11a, 6),
    HUMMINGBIRD_MARK(0x11b, 7),
    HEALING_DUST(0x11c, 8);

    private final int wireId;
    public final int ticks;

    BareSkillPulse(int wireId, int ticks) {
        this.wireId = wireId;
        this.ticks = ticks;
    }

    public void play(ServerPlayer player) {
        NetworkHandler.sendToTrackingAndSelf(new S2C_SkillModelVisual(player.getId(), wireId, ticks), player);
    }

    public static boolean isBareId(int id) {
        return id >= 0x100 && id < 0x200;
    }

    public static BareSkillPulse byId(int id) {
        for (BareSkillPulse pulse : values()) if (pulse.wireId == id) return pulse;
        return null;
    }
}
