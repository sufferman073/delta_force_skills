package com.rzy.dealt_force_skills.ai.operative;

import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.character.SkillSlot;
import com.rzy.dealt_force_skills.character.electronics.ElectronicInterferenceManager;
import com.rzy.dealt_force_skills.entity.AcceleratedTroopsEntity;
import com.rzy.dealt_force_skills.entity.HackclawInterferenceFieldEntity;
import com.rzy.dealt_force_skills.registry.ModEffects;

/** Native counterpart of the existing player electronic-skill restriction. No charge is spent. */
public final class OperativeElectronics {
    private OperativeElectronics() { }
    public static boolean blocked(OperativeHost host, String role, SkillSlot slot) {
        if (ModCharacters.D_WOLF_ID.equals(role) && OperativeGrowth.commander(host.body())) return false;
        var definition = ModCharacters.get(role).orElse(null);
        if (!ElectronicInterferenceManager.isElectronicSkill(definition, slot)) return false;
        var body = host.body();
        if (body.hasEffect(ModEffects.RAPTOR_ELECTROMAGNETIC_INTERFERENCE.get())
                || body.hasEffect(ModEffects.HACKCLAW_INTERFERENCE.get())
                || body.hasEffect(ModEffects.VLINDER_MEDICAL_WASTE_INTERFERENCE.get())) return true;
        for (var field : host.serverLevel().getEntitiesOfClass(HackclawInterferenceFieldEntity.class,
                body.getBoundingBox().inflate(HackclawInterferenceFieldEntity.RADIUS), HackclawInterferenceFieldEntity::isActive)) {
            if (body.position().distanceToSqr(field.position()) <= field.radius() * field.radius()
                    && !field.protects(body) && (field.ownerPlayer(host.serverLevel()) == null
                    || !OperativeSupport.ally(field.ownerPlayer(host.serverLevel()), body))) return true;
        }
        double radius = Math.max(0, com.rzy.dealt_force_skills.config.DealtBossesConfig.doubleValue("accelerated_troops.interference_radius", 20));
        return radius > 0 && !host.serverLevel().getEntitiesOfClass(AcceleratedTroopsEntity.class,
                body.getBoundingBox().inflate(radius), enemy -> enemy.isAlive() && enemy.distanceToSqr(body) <= radius * radius).isEmpty();
    }
}
