package com.rzy.dealt_force_skills.ai.operative;

import com.rzy.dealt_force_skills.character.SkillSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/** Shared melee/ranged skill decisions; each host retains its native weapon and navigation AI. */
public final class OperativeSkillStrategy {
    private OperativeSkillStrategy() { }
    public static double score(OperativeHost host, OperativeSkills skills, String character,
            SkillSlot slot, LivingEntity target, boolean ranged, double preferred, boolean reloading, boolean threatened) {
        if (skills instanceof NativeManbaSkills && slot == SkillSlot.ACTIVE_2) return 0;
        double score = skills.score(host, slot, target);
        if (score <= 0 || target == null) return score;
        String role = character.substring(character.lastIndexOf('/') + 1);
        LivingEntity body = host.body();
        boolean hurt = body.getHealth() < body.getMaxHealth() * .6;
        if (role.equals("catdad") && slot == SkillSlot.ACTIVE_2 && body.hurtTime == 0 && !target.swinging
                && !(target instanceof Mob mob && mob.getTarget() == body)) return 0;
        if (slot == SkillSlot.ACTIVE_1 && (role.equals("vyron") || role.equals("undead"))) {
            if (threatened || ranged && body.distanceTo(target) >= preferred && !reloading) return 0;
        }
        boolean explosive = slot == SkillSlot.ACTIVE_2 && (role.equals("vyron") || role.equals("luna")
                || role.equals("uluru") || role.equals("shepherd"))
                || slot == SkillSlot.ACTIVE_1 && role.equals("d_wolf")
                || slot == SkillSlot.CORE && (role.equals("vyron") || role.equals("uluru"));
        if (explosive) {
            long group = host.serverLevel().getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(5), host::isEnemy).size();
            if (ranged && group < 2 && target.getHealth() < 4 && !hurt) return 0;
            score += Math.min(3, group - 1) * 15 + (hurt ? 10 : 0);
        }
        return score;
    }
}
