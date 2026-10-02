package com.rzy.dealt_force_skills.character.commander;

import com.rzy.dealt_force_skills.character.SkillSlot;

public enum CommanderSkillChoice {
    PASSIVE_1(SkillSlot.PASSIVE),
    PASSIVE_2(SkillSlot.PASSIVE),
    PASSIVE_3(SkillSlot.PASSIVE),
    PASSIVE_4(SkillSlot.PASSIVE),
    ACTIVE_1(SkillSlot.ACTIVE_1),
    ACTIVE_2(SkillSlot.ACTIVE_2),
    CORE_1(SkillSlot.CORE),
    CORE_2(SkillSlot.CORE);

    private final SkillSlot skillSlot;

    CommanderSkillChoice(SkillSlot skillSlot) {
        this.skillSlot = skillSlot;
    }

    public SkillSlot skillSlot() {
        return skillSlot;
    }

    public String translationKey() {
        return "screen.dealt_force_skills.commander.choice." + name().toLowerCase(java.util.Locale.ROOT);
    }
}