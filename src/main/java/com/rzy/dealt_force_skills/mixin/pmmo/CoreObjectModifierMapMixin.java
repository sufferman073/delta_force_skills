package com.rzy.dealt_force_skills.mixin.pmmo;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Pseudo
@Mixin(targets = "harmonised.pmmo.core.Core", remap = false)
public abstract class CoreObjectModifierMapMixin {
    private static final Logger DFS$LOGGER =
            LoggerFactory.getLogger(DealtForceSkillsMod.MODID + "/PMMO Compat");
    private static final AtomicBoolean DFS$WARNED = new AtomicBoolean();

    @ModifyArg(
            method = "getObjectModifierMap",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/HashMap;<init>(Ljava/util/Map;)V",
                    ordinal = 0,
                    remap = false
            ),
            index = 0,
            require = 1,
            remap = false
    )
    private Map<?, ?> dfs$replaceNullModifierMap(Map<?, ?> source) {
        if (source != null) {
            return source;
        }
        if (DFS$WARNED.compareAndSet(false, true)) {
            DFS$LOGGER.warn("Prevented PMMO null modifier-map crash. Null bonus map was treated as empty.");
        }
        return Collections.emptyMap();
    }
}