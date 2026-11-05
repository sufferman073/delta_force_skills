package com.rzy.dealt_force_skills.mixin;
import com.rzy.dealt_force_skills.workbench.*;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.level.Level;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import java.util.List;
@Mixin(CrossbowItem.class)
public abstract class WorkbenchCrossbowMixin {
    @Shadow private static List<ItemStack> getChargedProjectiles(ItemStack stack){throw new AssertionError();}
    @Shadow private static void shootProjectile(Level level,LivingEntity user,InteractionHand hand,ItemStack weapon,ItemStack ammo,float pitch,boolean creative,float velocity,float inaccuracy,float angle){throw new AssertionError();}
    @Inject(method="getChargeDuration",at=@At("RETURN"),cancellable=true)
    private static void dfs$charge(ItemStack stack,CallbackInfoReturnable<Integer> ci) {
        int steps=WorkbenchData.steps(stack,WorkbenchStat.CHARGE_TIME);
        if(steps!=0)ci.setReturnValue(Math.max(1,ci.getReturnValue()-steps*2));
    }
    // Both branches of vanilla's multishot count; unrelated local variables are immaterial.
    @ModifyExpressionValue(method="tryLoadProjectiles",
        at={@At(value="CONSTANT",args="intValue=1"),@At(value="CONSTANT",args="intValue=3")},
        slice=@Slice(from=@At(value="INVOKE",target="Lnet/minecraft/world/item/enchantment/EnchantmentHelper;getItemEnchantmentLevel(Lnet/minecraft/world/item/enchantment/Enchantment;Lnet/minecraft/world/item/ItemStack;)I"),
            to=@At(value="INVOKE",target="Lnet/minecraft/world/entity/player/Player;getAbilities()Lnet/minecraft/world/entity/player/Abilities;")))
    private static int dfs$count(int original,LivingEntity user,ItemStack stack) {
        int steps=WorkbenchData.steps(stack,WorkbenchStat.MULTISHOT);
        return steps==0?original:Math.max(1,Math.min(19,original+steps));
    }
    @ModifyVariable(method="shootProjectile",at=@At("HEAD"),argsOnly=true,ordinal=1)
    private static float dfs$velocity(float velocity,Level world,LivingEntity user,InteractionHand hand,ItemStack weapon,ItemStack ammo,float pitch,boolean creative,float original,float inaccuracy,float angle) {
        return (float)(velocity*WorkbenchData.factor(weapon,WorkbenchStat.PROJECTILE_SPEED));
    }
    @Inject(method="getArrow",at=@At("RETURN"))
    private static void dfs$pierce(Level world,LivingEntity user,ItemStack weapon,ItemStack ammo,CallbackInfoReturnable<AbstractArrow> ci) {
        int steps=WorkbenchData.steps(weapon,WorkbenchStat.PIERCING);
        if(steps!=0){var arrow=ci.getReturnValue();arrow.setPierceLevel((byte)Math.max(0,Math.min(127,arrow.getPierceLevel()+steps)));}
    }
    @Inject(method="performShooting",at=@At(value="INVOKE",target="Lnet/minecraft/world/item/CrossbowItem;onCrossbowShot(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;)V"))
    private static void dfs$extraShots(Level world,LivingEntity user,InteractionHand hand,ItemStack weapon,float velocity,float inaccuracy,CallbackInfo ci) {
        int steps=WorkbenchData.steps(weapon,WorkbenchStat.MULTISHOT);
        if(steps<=0)return;
        var ammo=getChargedProjectiles(weapon);
        int count=Math.min(ammo.size(),Math.min(19,3+steps));
        for(int i=3;i<count&&!weapon.isEmpty();i++) {
            // Every additional arrow has a nonzero angle, retaining vanilla CREATIVE_ONLY pickup.
            float angle=(i%2==1?-1:1)*10f*((i+1)/2);
            if(!ammo.get(i).isEmpty())shootProjectile(world,user,hand,weapon,ammo.get(i),1f,false,velocity,inaccuracy,angle);
        }
    }
}
