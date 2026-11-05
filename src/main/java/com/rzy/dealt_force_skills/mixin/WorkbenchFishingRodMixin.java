package com.rzy.dealt_force_skills.mixin;
import com.rzy.dealt_force_skills.workbench.*;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.level.Level;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
@Mixin(value=FishingRodItem.class,remap=false)
public abstract class WorkbenchFishingRodMixin {
    @WrapOperation(method="use",at=@At(value="NEW",target="net/minecraft/world/entity/projectile/FishingHook"))
    private FishingHook dfs$createHook(Player owner,Level world,int luck,int lure,Operation<FishingHook> operation,Level usedWorld,Player player,InteractionHand hand) {
        var rod=player.getItemInHand(hand);
        int extraLure=WorkbenchData.steps(rod,WorkbenchStat.FISH_LURE);
        var hook=operation.call(owner,world,Math.max(0,luck+WorkbenchData.steps(rod,WorkbenchStat.FISH_LUCK)),Math.max(0,lure+extraLure*100));
        ((WorkbenchFishing)hook).dfs$configureFishing(WorkbenchData.factor(rod,WorkbenchStat.FISH_PULL),extraLure>0);
        return hook;
    }
}
