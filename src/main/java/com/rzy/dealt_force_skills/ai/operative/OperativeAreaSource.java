package com.rzy.dealt_force_skills.ai.operative;

/** Area attacks retain their native caster and commander after unloading. */
public interface OperativeAreaSource extends OperativeSkillSource {
    OperativeAllegiance operativeAllegiance();

    @Override default java.util.UUID operativeOwnerUUID() { return operativeAllegiance().owner(); }
}
