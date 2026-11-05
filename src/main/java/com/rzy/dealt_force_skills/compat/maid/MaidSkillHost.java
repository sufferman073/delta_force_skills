package com.rzy.dealt_force_skills.compat.maid;

import com.rzy.dealt_force_skills.ai.operative.OperativeHost;
import com.rzy.dealt_force_skills.ai.operative.OperativeSkills;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Added only to the optional maid class; no external API appears in our class signatures. */
public interface MaidSkillHost extends OperativeHost {
    MaidSkillController dealtForceMaidSkills();
    @Override default boolean parallelNativeAi() { return true; }
    @Override default OperativeSkills skills() { return dealtForceMaidSkills().skills(); }
    @Override default boolean isEnemy(LivingEntity target) { return dealtForceMaidSkills().isEnemy(target); }
    @Override default boolean withinOrders(Vec3 point) { return dealtForceMaidSkills().withinOrders(point); }
}
