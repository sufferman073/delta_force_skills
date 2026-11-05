package com.rzy.dealt_force_skills.motion;

/** Visual-only, six-part player poses. Angles are degrees; offsets are blocks. */
public enum TacticalMotion {
    NONE, SLIDE, ROLL, DIVE, JET, RUN, SWIM, HEAVY_SWING, CHARGE, VAULT,
    NINJA_CHARGE, LIGHT_SLASH, THROW, HOLD_TOOL;

    public boolean looping() { return this == RUN || this == SWIM; }
    /** These poses end on the server's stop snapshot, not at an estimated local deadline. */
    public boolean sustained() { return this == SLIDE || this == ROLL || this == JET || this == CHARGE
            || this == NINJA_CHARGE || this == HOLD_TOOL; }
    public boolean ownsRoot() { return this == SLIDE || this == ROLL || this == DIVE || this == JET
            || this == SWIM || this == CHARGE || this == VAULT; }

    public boolean keepsGunPose() { return this == SLIDE || this == JET || this == RUN; }
    public boolean upperBodyOnly() { return this == LIGHT_SLASH || this == THROW || this == HOLD_TOOL
            || this == NINJA_CHARGE; }

    public static TacticalMotion byId(int id) {
        return id >= 0 && id < values().length ? values()[id] : NONE;
    }

    public static final class Pose {
        public float rootPitch, rootRoll, height, bodyPitch, bodyYaw, headPitch;
        public float leftArm, rightArm, leftArmYaw, rightArmYaw, leftArmRoll, rightArmRoll;
        public float leftLeg, rightLeg, leftLegRoll, rightLegRoll, weight;
    }

    public static float clamp(float v, float lo, float hi) { return Math.max(lo, Math.min(hi, v)); }
    public static float smooth(float v) { v = clamp(v, 0, 1); return v * v * (3 - 2 * v); }
    public static float curve(float t, float... keys) {
        if (t <= keys[0]) return keys[1];
        for (int i = 2; i < keys.length; i += 2) {
            if (t <= keys[i]) {
                float u = smooth((t - keys[i - 2]) / (keys[i] - keys[i - 2]));
                return keys[i - 1] + (keys[i + 1] - keys[i - 1]) * u;
            }
        }
        return keys[keys.length - 1];
    }

    /** Unwrapped scalar interpolation is essential: 0 and 360 are NOT interpolated as quaternions. */
    public static float rollAngle(float t) {
        float[] keys = {0, 0, .125F, 35, .25F, 90, .5F, 185, .75F, 280, 1, 360};
        t = clamp(t, 0, 1);
        for (int i = 2; i < keys.length; i += 2) {
            if (t <= keys[i]) return keys[i - 1] + (keys[i + 1] - keys[i - 1])
                    * (t - keys[i - 2]) / (keys[i] - keys[i - 2]);
        }
        return 360;
    }

    /** phase=1 is server-confirmed dive landing. distance is actual travel, never target velocity. */
    public Pose sample(float age, int duration, int phase, float distance, float strength, int impactTick) {
        Pose p = new Pose();
        float t = clamp(age / Math.max(1, duration), 0, 1);
        p.weight = looping() ? smooth(age / 3) * clamp(strength, 0, 1)
                : sustained() ? smooth(age / Math.max(1, Math.min(2, duration * .15F)))
                : Math.min(smooth(t / .15F), smooth((1 - t) / .2F));
        float stride = (float)Math.sin(distance * Math.PI * 2 / (this == SWIM ? 3.2 : 2.8));
        switch (this) {
            case SLIDE -> {
                // Recline the entire body; a raised leg on an upright torso is not a slide.
                p.rootPitch = -64 * p.weight; p.height = -.43F * p.weight;
                // Raise the chest to about 35 degrees above ground without moving the legs.
                p.bodyPitch = 9; p.bodyYaw = -8; p.headPitch = 48;
                p.leftLeg = -8; p.rightLeg = -24; p.rightLegRoll = 18;
                // Hold forward in level model space; the slide renderer counters the root tilt.
                p.leftArm = -90; p.rightArm = -90;
            }
            case ROLL -> {
                // Keep an unwrapped turn across boost/coast and packet heartbeats.
                float turns = Math.max(0, age) / Math.max(1, duration);
                float whole = (float)Math.floor(turns);
                p.rootPitch = 360 * whole + rollAngle(turns - whole);
                p.height = -.20F * p.weight;
                p.bodyPitch = 22; p.headPitch = 25;
                p.leftLeg = -95; p.rightLeg = -100; p.leftArm = -100; p.rightArm = -105;
                p.leftArmRoll = -12; p.rightArmRoll = 12;
            }
            case DIVE -> {
                if (phase == 0) {
                    p.weight = smooth(age / Math.max(1, duration * .25F));
                    p.rootPitch = 82 * p.weight; p.headPitch = -55;
                    p.leftArm = -160; p.rightArm = -150; p.leftLeg = 8; p.rightLeg = 12;
                } else {
                    p.weight = 1 - smooth(t); p.rootPitch = 65 * p.weight;
                    p.height = -.2F * p.weight; p.headPitch = -40;
                    p.leftArm = -105; p.rightArm = -100; p.leftLeg = -45; p.rightLeg = -55;
                }
            }
            case JET -> {
                // Thrust pushes an upright stance forward, without a flying root rotation.
                p.bodyPitch = 14; p.headPitch = -12; p.height = -.025F * p.weight;
                p.leftArm = -104; p.rightArm = -104; p.leftLeg = -6; p.rightLeg = 9;
            }
            case RUN -> {
                p.bodyPitch = 20; p.headPitch = -20;
                p.leftLeg = 52 * stride; p.rightLeg = -p.leftLeg;
                p.leftArm = -45 * stride; p.rightArm = -p.leftArm;
                p.leftArmRoll = -8; p.rightArmRoll = 8;
                p.bodyYaw = 6 * stride;
            }
            case SWIM -> {
                p.rootPitch = 82; p.headPitch = -55; p.rootRoll = 8 * stride * p.weight;
                float cycle = distance / 3.2F;
                float left = cycle - (float)Math.floor(cycle);
                float right = (left + .5F) % 1;
                p.leftArm = swimArm(left); p.rightArm = swimArm(right);
                p.leftArmRoll = -curve(left, 0, 10, .25F, 25, .5F, 20, .75F, 75, 1, 10);
                p.rightArmRoll = curve(right, 0, 10, .25F, 25, .5F, 20, .75F, 75, 1, 10);
                p.leftLeg = 12 * (float)Math.sin(cycle * Math.PI * 6); p.rightLeg = -p.leftLeg;
            }
            case HEAVY_SWING -> {
                float hit = clamp(impactTick / (float)Math.max(1, duration), .2F, .85F);
                float windup = hit * .65F;
                p.bodyYaw = curve(t, 0, 0, windup, 45, hit, -50, 1, 0);
                p.bodyPitch = curve(t, 0, 0, windup, -8, hit, 25, 1, 0);
                p.rightArm = curve(t, 0, -20, windup, -140, hit, -65, 1, -20);
                p.leftArm = p.rightArm + 12;
                p.rightArmYaw = curve(t, 0, 0, windup, -40, hit, 55, 1, 0);
                p.leftArmYaw = p.rightArmYaw + 20;
                p.leftLeg = -15; p.rightLeg = 10; p.headPitch = -p.bodyPitch * .6F;
            }
            case CHARGE -> {
                p.bodyPitch = 30; p.bodyYaw = -20; p.headPitch = -20;
                p.leftArm = -65; p.rightArm = -80; p.rightArmYaw = -25;
                // A bounded, planted drive cadence; displacement can jump several blocks/tick.
                float drive = 18 * (float)Math.sin(age * Math.PI * 2 / 10) * clamp(strength, 0, 1);
                p.leftLeg = -8 + drive; p.rightLeg = -8 - drive;
            }
            case VAULT -> {
                p.rootPitch = 20 * p.weight; p.leftArm = -95; p.rightArm = -90;
                p.leftLeg = -80; p.rightLeg = -65; p.headPitch = -15;
            }
            case NINJA_CHARGE -> {
                p.bodyPitch = 5; p.headPitch = -5;
                p.leftArm = -85; p.rightArm = -85;
                p.leftArmYaw = 35; p.rightArmYaw = -35;
                p.leftArmRoll = -12; p.rightArmRoll = 12;
            }
            case LIGHT_SLASH -> {
                // A short one-handed cut. The existing skill owns damage timing.
                p.rightArm = curve(t, 0, -40, .2F, -125, .42F, -65, 1, -30);
                p.rightArmYaw = curve(t, 0, -15, .2F, -65, .42F, 65, 1, 0);
                p.rightArmRoll = curve(t, 0, 0, .2F, 25, .42F, -35, 1, 0);
                p.leftArm = -35; p.leftArmYaw = 15;
                p.bodyYaw = curve(t, 0, 0, .2F, 15, .42F, -18, 1, 0);
            }
            case THROW -> {
                p.weight = smooth((1-t)/.45F);
                p.rightArm = curve(t, 0, -100, .2F, -65, .55F, -30, 1, -10);
                p.rightArmYaw = curve(t, 0, 10, .3F, 25, 1, 0);
                p.leftArm = -45; p.leftArmYaw = 15;
                p.bodyYaw = curve(t, 0, -12, .3F, -6, 1, 0);
            }
            case HOLD_TOOL -> {
                p.rightArm = -65; p.rightArmYaw = -12;
                p.leftArm = -35; p.leftArmYaw = 15;
            }
            default -> p.weight = 0;
        }
        return p;
    }

    public static int recoveryTicks(TacticalMotion kind, Pose p) {
        if (kind != ROLL) return 4;
        float remainder = ((p.rootPitch % 360) + 360) % 360;
        return Math.max(4, (int)Math.ceil((remainder < .001F ? 0 : 360 - remainder) / 60));
    }

    /** Release a frozen outgoing pose; a roll completes forward instead of reversing. */
    public static void release(TacticalMotion kind, Pose p, float progress) {
        float blend = smooth(progress), keep = 1 - blend;
        if (kind == ROLL) {
            float remainder = ((p.rootPitch % 360) + 360) % 360;
            float target = p.rootPitch + (remainder < .001F ? 0 : 360 - remainder);
            p.rootPitch += (target - p.rootPitch) * blend;
        } else p.rootPitch *= keep;
        p.weight *= keep; p.height *= keep; p.rootRoll *= keep;
    }

    private static float swimArm(float phase) {
        return -curve(phase, 0, 160, .25F, 60, .5F, -20, .75F, 80, 1, 160);
    }

    /** Conservative six-part silhouette clearance; visual translation only, never entity collision. */
    public static float groundLift(Pose p) {
        float minimum = Float.POSITIVE_INFINITY;
        // origin x/y, box bounds, local Euler degrees. Upper parts rotate about the hip at model y=12.
        float[][] parts = {
                {0,0,-4,4,-8,0,-4,4,p.headPitch,0,0,1},
                {0,0,-4,4,0,12,-2,2,0,0,0,1},
                {5,2,-1,3,-2,10,-2,2,p.leftArm,p.leftArmYaw,p.leftArmRoll,1},
                {-5,2,-3,1,-2,10,-2,2,p.rightArm,p.rightArmYaw,p.rightArmRoll,1},
                {1.9F,12,-2,2,0,12,-2,2,p.leftLeg,0,p.leftLegRoll,0},
                {-1.9F,12,-2,2,0,12,-2,2,p.rightLeg,0,p.rightLegRoll,0}};
        for (float[] part : parts) for (int corner = 0; corner < 8; corner++) {
            float[] v = rotate(part[2 + (corner & 1)], part[4 + ((corner >> 1) & 1)],
                    part[6 + ((corner >> 2) & 1)], part[8]*p.weight, part[9]*p.weight, part[10]*p.weight);
            v[0] += part[0]; v[1] += part[1];
            if (part[11] == 1) {
                v = rotate(v[0], v[1]-12, v[2], p.bodyPitch*p.weight, p.bodyYaw*p.weight, 0);
                v[1] += 12;
            }
            float y = (1.501F - v[1]/16)*.9375F;
            float z = v[2]/16*.9375F;
            double angle = Math.toRadians(p.rootPitch);
            float height = .75F + p.height + (y-.75F)*(float)Math.cos(angle) + z*(float)Math.sin(angle);
            minimum = Math.min(minimum, height);
        }
        return Math.max(0, .04F*p.weight-minimum);
    }

    private static float[] rotate(float x, float y, float z, float pitch, float yaw, float roll) {
        double a=Math.toRadians(pitch), b=Math.toRadians(yaw), c=Math.toRadians(roll);
        double x1=x*Math.cos(c)-y*Math.sin(c), y1=x*Math.sin(c)+y*Math.cos(c);
        double x2=x1*Math.cos(b)+z*Math.sin(b), z2=-x1*Math.sin(b)+z*Math.cos(b);
        return new float[]{(float)x2,(float)(y1*Math.cos(a)-z2*Math.sin(a)),(float)(y1*Math.sin(a)+z2*Math.cos(a))};
    }
}
