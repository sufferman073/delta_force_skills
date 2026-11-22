package com.rzy.dealt_force_skills.skill;

import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.S2C_SkillModelVisual;
import net.minecraft.world.entity.Entity;

public final class SkillModelVisualSync {
    private SkillModelVisualSync() {
    }

    public static void play(Entity entity, SkillModelVisual visual) {
        play(entity, visual, visual == null ? 0 : visual.durationTicks());
    }

    public static void play(Entity entity, SkillModelVisual visual, int durationTicks) {
        if (entity != null && visual != null) {
            if (visual != SkillModelVisual.UNDEAD_SCHOLAR_FIBERS && visual != SkillModelVisual.LEX_HAM_PRESENCE
                    && entity instanceof net.minecraft.world.entity.LivingEntity living)
                com.rzy.dealt_force_skills.ai.operative.OperativeHearing.recordAction(living);
            NetworkHandler.sendToTrackingAndSelf(new S2C_SkillModelVisual(
                    entity.getId(), visual.ordinal(), Math.max(1, durationTicks)), entity);
        }
    }
}
