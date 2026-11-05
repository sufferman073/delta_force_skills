package com.rzy.dealt_force_skills.mixin;
import com.rzy.dealt_force_skills.workbench.*;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import java.util.List;
@Mixin(value=ProjectileWeaponItem.class,remap=false)
public abstract class WorkbenchProjectileWeaponMixin {
    @WrapOperation(method="draw",at=@At(value="INVOKE",target="Lnet/minecraft/world/item/enchantment/EnchantmentHelper;processProjectileCount(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/Entity;I)I"))
    private static int dfs$count(ServerLevel level,ItemStack weapon,Entity user,int base,Operation<Integer> operation) {
        int original=operation.call(level,weapon,user,base),steps=WorkbenchData.steps(weapon,WorkbenchStat.MULTISHOT);
        return steps==0?original:Math.max(1,Math.min(19,original+steps));
    }
    @WrapOperation(method="shoot",at=@At(value="INVOKE",target="Lnet/minecraft/server/level/ServerLevel;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean dfs$launch(ServerLevel world,Entity projectile,Operation<Boolean> operation,ServerLevel level,LivingEntity user,InteractionHand hand,ItemStack weapon,List<ItemStack> ammo,float velocity,float inaccuracy,boolean critical,LivingEntity target) {
        projectile.setDeltaMovement(projectile.getDeltaMovement().scale(WorkbenchData.factor(weapon,WorkbenchStat.PROJECTILE_SPEED)));
        if(projectile instanceof AbstractArrow arrow) {
            if(weapon.getItem() instanceof BowItem)arrow.setBaseDamage(arrow.getBaseDamage()*WorkbenchData.factor(weapon,WorkbenchStat.BOW_DAMAGE)*(arrow.isCritArrow()?WorkbenchData.factor(weapon,WorkbenchStat.BOW_FULL_DAMAGE):1));
            int steps=WorkbenchData.steps(weapon,WorkbenchStat.PIERCING);
            if(steps!=0)((WorkbenchArrowMixin)arrow).dfs$pierce((byte)Math.max(0,Math.min(127,arrow.getPierceLevel()+steps)));
        }
        return operation.call(world,projectile);
    }
}
