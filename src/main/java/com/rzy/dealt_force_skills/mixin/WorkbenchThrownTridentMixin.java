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
@Mixin(value=ThrownTrident.class)
public abstract class WorkbenchThrownTridentMixin {
    @Shadow private ItemStack tridentItem;

    @WrapOperation(method="onHitEntity",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean dfs$impaling(Entity target,DamageSource source,float damage,Operation<Boolean> operation) {

        double factor=WorkbenchData.factor(tridentItem,WorkbenchStat.IMPALING);
        if(factor!=1&&target instanceof LivingEntity living)
            damage+=(float)(Enchantments.IMPALING.getDamageBonus(WorkbenchPlatform.enchantment(tridentItem,"impaling"),living.getMobType())*(factor-1));
        return operation.call(target,source,damage);
    }
}
