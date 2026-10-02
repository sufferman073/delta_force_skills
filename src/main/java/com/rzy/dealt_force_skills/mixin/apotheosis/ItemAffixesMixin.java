package com.rzy.dealt_force_skills.mixin.apotheosis;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Pseudo
@Mixin(targets = "dev.shadowsoffire.apotheosis.affix.ItemAffixes", remap = false)
public abstract class ItemAffixesMixin {
    private static final float HVK_MAX_AFFIX_LEVEL = 6.0F;

    @ModifyConstant(method = "<init>", constant = @Constant(floatValue = 2.0F), require = 1)
    private float dealtForceSkills$allowHvkTripleAffixCap(float original) {
        return HVK_MAX_AFFIX_LEVEL;
    }

    @ModifyConstant(method = "<clinit>", constant = @Constant(floatValue = 2.0F), require = 1)
    private static float dealtForceSkills$allowHvkTripleAffixCodecCap(float original) {
        return HVK_MAX_AFFIX_LEVEL;
    }
}
