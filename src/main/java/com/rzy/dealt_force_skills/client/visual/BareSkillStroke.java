package com.rzy.dealt_force_skills.client.visual;

import com.rzy.dealt_force_skills.skill.BareSkillPulse;

record BareSkillStroke(float lift, float inward, float reach, float roll, float weight) {
    private static final BareSkillStroke REST = new BareSkillStroke(0, 0, 0, 0, 0);
    private record Path(float readyAt, BareSkillStroke ready, float accentAt, BareSkillStroke accent) {}

    private static final Path OVERLOAD = new Path(.30F, pose(.66F,.40F,.24F,-34), .53F, pose(.75F,.94F,.37F,34));
    private static final Path THRUST = new Path(.20F, pose(.52F,.12F,.09F,-12), .48F, pose(.31F,-.43F,.99F,-27));
    private static final Path TUCK = new Path(.22F, pose(.82F,.30F,.48F,12), .57F, pose(.96F,.90F,.12F,49));
    private static final Path PLANT = new Path(.31F, pose(.75F,.36F,.24F,-8), .57F, pose(.27F,.68F,.83F,29));
    private static final Path PULL = new Path(.23F, pose(.48F,.28F,.97F,-20), .58F, pose(.76F,.56F,.04F,31));
    private static final Path FAN = new Path(.33F, pose(.52F,.88F,.34F,38), .65F, pose(.69F,-.40F,.69F,-46));
    private static final Path PARRY = new Path(.24F, pose(.34F,-.30F,.58F,-31), .51F, pose(.89F,.36F,.54F,9));

    private static final Path WARM_ENFORCEMENT = new Path(.31F, pose(.30F, .79F, .17F, 37), .64F, pose(.68F, -.17F, .57F, -18));
    private static final Path BATON_BRACE = new Path(.21F, pose(.85F, -.23F, .14F, -33), .53F, pose(.48F, .57F, .71F, 24));
    private static final Path STARS_ORBIT = new Path(.35F, pose(.93F, -.36F, .42F, -49), .67F, pose(.57F, .67F, .73F, 42));
    private static final Path JUSTICE_SEAL = new Path(.36F, pose(.86F, .25F, .32F, 4), .54F, pose(.28F, .25F, .85F, 7));
    private static final Path HIGH_NOON = new Path(.29F, pose(.96F, .65F, .12F, 32), .70F, pose(.86F, -.10F, .70F, -4));
    private static final Path HAKKO_DRAW = new Path(.32F, pose(.51F, .94F, .14F, 48), .50F, pose(.53F, -.21F, .83F, -53));
    private static final Path CORPS_CHALLENGE = new Path(.32F, pose(.60F, .77F, .10F, 12), .58F, pose(.59F, .05F, .96F, -8));
    private static final Path DUEL_BECKON = new Path(.32F, pose(.68F, .08F, .87F, -18), .66F, pose(.73F, .66F, .22F, 36));
    private static final Path ROAD_SIGNAL = new Path(.31F, pose(.94F, -.28F, .26F, -28), .62F, pose(.44F, .83F, .64F, 37));
    private static final Path CORRECTION = new Path(.26F, pose(.62F, .08F, .29F, -6), .54F, pose(.70F, .56F, .76F, -24));
    private static final Path TIME_ANCHOR = new Path(.36F, pose(.80F, .77F, .38F, 46), .64F, pose(.45F, .66F, .68F, -27));
    private static final Path ERASE_ERROR = new Path(.28F, pose(.81F, -.22F, .34F, -39), .53F, pose(.53F, .95F, .65F, 21));
    private static final Path HEAT_RELEASE = new Path(.21F, pose(.38F, .46F, .13F, -9), .46F, pose(.60F, .14F, .98F, -14));
    private static final Path COUNTDOWN = new Path(.30F, pose(.94F, .24F, .24F, -31), .65F, pose(.54F, .82F, .22F, 45));
    private static final Path BLOODLUST = new Path(.27F, pose(.36F, -.29F, .38F, -44), .62F, pose(.85F, .70F, .14F, 28));
    private static final Path BLOOD_RELOAD = new Path(.32F, pose(.42F, .76F, .30F, 46), .53F, pose(.76F, .47F, .16F, -28));
    private static final Path SPACE_OPEN = new Path(.34F, pose(.61F, .94F, .42F, -15), .69F, pose(.94F, -.49F, .47F, -50));
    private static final Path ARENA_DEAL = new Path(.28F, pose(.45F, .73F, .18F, 26), .64F, pose(.42F, -.44F, .89F, -36));
    private static final Path SCHOLAR_RECORD = new Path(.34F, pose(.63F, -.22F, .66F, -12), .72F, pose(.59F, .77F, .55F, 24));

    private static final Path ANCHOR_BLINK = new Path(.24F, pose(.37F, -.14F, .53F, -37), .48F, pose(.82F, .84F, .13F, 46));
    private static final Path HUMMINGBIRD_MARK = new Path(.38F, pose(.61F, .26F, .16F, 20), .57F, pose(.74F, .11F, .82F, -31));

    private static final Path HEALING_DUST = new Path(.31F, pose(.84F, .86F, .17F, 40), .65F, pose(.37F, .58F, .31F, -19));

    private static BareSkillStroke pose(float lift, float inward, float reach, float roll) {
        return new BareSkillStroke(lift, inward, reach, roll, 1);
    }

    static BareSkillStroke sample(BareSkillPulse pulse, float phase) {
        if (phase <= 0 || phase >= 1) return REST;
        Path path = switch (pulse) {
            case OVERLOAD -> OVERLOAD;
            case THRUST -> THRUST;
            case TUCK_ROLL -> TUCK;
            case ANCHOR_SET -> PLANT;
            case RECALL_PULL -> PULL;
            case ADRENALINE -> FAN;
            case EVADE -> PARRY;
            case WARM_ENFORCEMENT -> WARM_ENFORCEMENT;
            case BATON_BRACE -> BATON_BRACE;
            case STARS_ORBIT -> STARS_ORBIT;
            case JUSTICE_SEAL -> JUSTICE_SEAL;
            case HIGH_NOON -> HIGH_NOON;
            case HAKKO_DRAW -> HAKKO_DRAW;
            case CORPS_CHALLENGE -> CORPS_CHALLENGE;
            case DUEL_BECKON -> DUEL_BECKON;
            case ROAD_SIGNAL -> ROAD_SIGNAL;
            case CORRECTION -> CORRECTION;
            case TIME_ANCHOR -> TIME_ANCHOR;
            case ERASE_ERROR -> ERASE_ERROR;
            case HEAT_RELEASE -> HEAT_RELEASE;
            case COUNTDOWN -> COUNTDOWN;
            case BLOODLUST -> BLOODLUST;
            case BLOOD_RELOAD -> BLOOD_RELOAD;
            case SPACE_OPEN -> SPACE_OPEN;
            case ARENA_DEAL -> ARENA_DEAL;
            case SCHOLAR_RECORD -> SCHOLAR_RECORD;
            case ANCHOR_BLINK -> ANCHOR_BLINK;
            case HUMMINGBIRD_MARK -> HUMMINGBIRD_MARK;
            case HEALING_DUST -> HEALING_DUST;
        };
        if (phase < path.readyAt) return blend(REST, path.ready, phase / path.readyAt);
        if (phase < path.accentAt) return blend(path.ready, path.accent, (phase - path.readyAt) / (path.accentAt - path.readyAt));
        return blend(path.accent, REST, (phase - path.accentAt) / (1 - path.accentAt));
    }

    private static BareSkillStroke blend(BareSkillStroke from, BareSkillStroke to, float phase) {
        float mix = phase * phase * (3 - 2 * phase);
        return new BareSkillStroke(from.lift + mix * (to.lift - from.lift), from.inward + mix * (to.inward - from.inward),
                from.reach + mix * (to.reach - from.reach), from.roll + mix * (to.roll - from.roll), from.weight + mix * (to.weight - from.weight));
    }
}
