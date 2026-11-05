package com.rzy.dealt_force_skills.mixin;
import com.rzy.dealt_force_skills.workbench.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
@Mixin(value=ThrownTrident.class,remap=false)
public abstract class WorkbenchThrownTridentMixin {

    @WrapOperation(method="onHitEntity",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean dfs$impaling(Entity target,DamageSource source,float damage,Operation<Boolean> operation) {

        var self=(ThrownTrident)(Object)this;ItemStack weapon=self.getWeaponItem();
        double factor=WorkbenchData.factor(weapon,WorkbenchStat.IMPALING);
        if(factor!=1&&self.level() instanceof net.minecraft.server.level.ServerLevel level) {
            var holder=level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.IMPALING);
            int rank=EnchantmentHelper.getItemEnchantmentLevel(holder,weapon);
            var amount=new org.apache.commons.lang3.mutable.MutableFloat(8f);
            holder.value().modifyDamage(level,rank,weapon,target,source,amount);
            damage+=(float)((amount.floatValue()-8)*(factor-1));
        }
        return operation.call(target,source,damage);
    }
}
