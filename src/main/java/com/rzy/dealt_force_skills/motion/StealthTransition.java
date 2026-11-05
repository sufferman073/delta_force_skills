package com.rzy.dealt_force_skills.motion;

/** Render-clock opacity with continuous reversals and an owned invisible endpoint. */
public final class StealthTransition {
    private boolean hidden;
    private float from, started;

    public StealthTransition(boolean hidden, float now) {
        this.hidden = hidden; this.from = hidden ? 0 : 1; this.started = now - 4;
    }

    public boolean hidden() { return hidden; }

    public float observe(boolean nextHidden, float now) {
        float alpha = alpha(now);
        if (nextHidden != hidden) {
            from = alpha; started = now; hidden = nextHidden;
        }
        return alpha;
    }

    public float alpha(float now) {
        float target = hidden ? 0 : 1;
        float progress = TacticalMotion.smooth((now - started) / 4);
        return from + (target - from) * progress;
    }

    public boolean ownsRender(float now) { return hidden || alpha(now) < 1; }
}
