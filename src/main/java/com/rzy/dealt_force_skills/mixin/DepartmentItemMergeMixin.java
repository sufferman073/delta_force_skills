package com.rzy.dealt_force_skills.mixin;

import com.rzy.dealt_force_skills.department.DepartmentLoot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=ItemEntity.class,remap=false)
public abstract class DepartmentItemMergeMixin {
    @Inject(method="merge(Lnet/minecraft/world/entity/item/ItemEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/item/ItemEntity;Lnet/minecraft/world/item/ItemStack;)V",at=@At("HEAD"))
    private static void dfs$mergeLoot(ItemEntity destination,ItemStack destinationStack,ItemEntity origin,ItemStack originStack,CallbackInfo ci) {
        DepartmentLoot.merging(destination,destinationStack,origin,originStack);
    }
    /** Inventory.add can move a partial stack while returning false, so observe its mutation directly. */
    @WrapOperation(method="playerTouch",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/player/Inventory;add(Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean dfs$actualPickup(Inventory inventory,ItemStack stack,Operation<Boolean> original,Player player) {
        int before=stack.getCount();
        boolean result=original.call(inventory,stack);
        if(player instanceof ServerPlayer serverPlayer)
            DepartmentLoot.pickedUp(serverPlayer,(ItemEntity)(Object)this,Math.max(0,before-stack.getCount()));
        return result;
    }
}

