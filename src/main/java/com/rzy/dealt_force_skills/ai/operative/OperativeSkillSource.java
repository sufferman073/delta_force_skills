package com.rzy.dealt_force_skills.ai.operative;

import java.util.UUID;

/** Keeps companion allegiance available after the living caster unloads or dies. */
public interface OperativeSkillSource {
    UUID operativeOwnerUUID();
}
