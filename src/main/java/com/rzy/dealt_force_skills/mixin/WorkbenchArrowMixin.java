package com.rzy.dealt_force_skills.mixin;
import net.minecraft.world.entity.projectile.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(value=AbstractArrow.class,remap=false)
public interface WorkbenchArrowMixin {
    @Invoker("setPierceLevel") void dfs$pierce(byte value);
}
