package com.rzy.dealt_force_skills.mixin;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** NeoForge moves unpackLootTable to a default interface method; observe its completed fill. */
@Mixin(value=LootTable.class,remap=false)
public abstract class DepartmentContainerLootMixin {
    @Inject(method="fill",at=@At("RETURN"))
    private void dfs$generatedDepartmentLoot(Container container,LootParams params,long seed,CallbackInfo ci) {
        if(params.getParamOrNull(LootContextParams.THIS_ENTITY) instanceof Player player) {
            com.rzy.dealt_force_skills.department.DepartmentLoot.containerLoot(container,player);
            com.rzy.dealt_force_skills.department.DepartmentUndead.containerLoot(container,player);
        }
    }
}
