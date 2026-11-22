package com.rzy.dealt_force_skills.client.visual;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.client.renderer.BlockbenchAnimatedModelRenderer;
import net.minecraft.resources.ResourceLocation;


public final class OE2CombatStimulantVisuals {
    static final ResourceLocation MODEL = new ResourceLocation(
            DealtForceSkillsMod.MODID, "consumable/oe2_combat_stimulant");

    private OE2CombatStimulantVisuals() {}

    static boolean available() {
        return BlockbenchAnimatedModelRenderer.animationLength(MODEL, "use") > 0;
    }

}
