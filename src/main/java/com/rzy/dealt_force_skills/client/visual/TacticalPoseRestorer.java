package com.rzy.dealt_force_skills.client.visual;

/** Restores our previous frame before vanilla or another animator prepares the next one. */
public interface TacticalPoseRestorer {
    void dfs$restoreTacticalPose();
    void dfs$renderIsolated(Runnable draw, boolean hand);
}
