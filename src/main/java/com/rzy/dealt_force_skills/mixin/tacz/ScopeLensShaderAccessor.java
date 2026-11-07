package com.rzy.dealt_force_skills.mixin.tacz;

import com.tacz.guns.client.renderer.scope.ScopeLensRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Borrow TaCZ's reload-managed shader; no shader source or program is bundled here. */
@Mixin(value = ScopeLensRenderer.class, remap = false)
public interface ScopeLensShaderAccessor {
    @Accessor("shader")
    static ShaderInstance dealtForceSkills$shader() {
        throw new AssertionError("Mixin accessor was not applied");
    }
}
