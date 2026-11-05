package com.rzy.dealt_force_skills.mixin;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity.class)
public abstract class DepartmentContainerLootMixin {
    @Inject(method="unpackLootTable",at=@At(value="INVOKE",
            target="Lnet/minecraft/world/level/storage/loot/LootTable;fill(Lnet/minecraft/world/Container;Lnet/minecraft/world/level/storage/loot/LootParams;J)V",shift=At.Shift.AFTER))
    private void dfs$generatedDepartmentLoot(Player player,CallbackInfo ci) {
        com.rzy.dealt_force_skills.department.DepartmentLoot.containerLoot((Container)(Object)this,player);
        com.rzy.dealt_force_skills.department.DepartmentUndead.containerLoot((Container)(Object)this,player);
    }
}
