package com.rzy.dealt_force_skills.mixin;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.util.TargetingUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerSelfSkillDamageMixin {
    @WrapOperation(method = "hurt", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayer;canHarmPlayer(Lnet/minecraft/world/entity/player/Player;)Z"))
    private boolean dfs$allowSelfSkillDamage(ServerPlayer target, Player attacker, Operation<Boolean> operation, DamageSource source, float amount) {
        if (target == attacker && source instanceof com.rzy.dealt_force_skills.character.wildfire.WildfireDamageSource wildfire && !wildfire.core()) return true;
        // Vanilla applies the scoreboard/PvP gate to self before the mod's damage events run.
        if (TargetingUtil.isSelf(attacker, target)
                && TargetingUtil.isSelfOrHostilePlayerFor(attacker, target)
                && source.typeHolder().unwrapKey()
                .map(key -> DealtForceSkillsMod.MODID.equals(key.location().getNamespace())).orElse(false)) {
            return true;
        }
        return operation.call(target, attacker);
    }
}
