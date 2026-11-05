package com.rzy.dealt_force_skills.mixin;
import com.rzy.dealt_force_skills.workbench.*;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(value=Player.class,remap=false)
public abstract class WorkbenchPlayerMixin {
    @ModifyArg(method="disableShield",at=@At(value="INVOKE",target="Lnet/minecraft/world/item/ItemCooldowns;addCooldown(Lnet/minecraft/world/item/Item;I)V"))
    private int dfs$shieldCooldown(int ticks) {
        return Math.max(1,(int)Math.round(ticks*WorkbenchData.factor(((Player)(Object)this).getUseItem(),WorkbenchStat.SHIELD_COOLDOWN)));
    }
    // Match the unique damage float even when another transformer prepends the receiver.
    @ModifyArg(method="attack",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/LivingEntity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private float dfs$sweepDamage(float damage) {
        return (float)(damage*WorkbenchData.factor(((Player)(Object)this).getMainHandItem(),WorkbenchStat.SWEEP));
    }
}

