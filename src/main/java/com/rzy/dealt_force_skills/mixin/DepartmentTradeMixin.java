package com.rzy.dealt_force_skills.mixin;

import com.rzy.dealt_force_skills.department.DepartmentTasks;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantContainer;
import net.minecraft.world.inventory.MerchantResultSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.Merchant;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Reached only after MerchantOffer.take actually consumed the payment. */
@Mixin(value=MerchantResultSlot.class,remap=false)
public abstract class DepartmentTradeMixin {
    @Shadow @Final private MerchantContainer slots;
    @Shadow @Final private Merchant merchant;
    @Inject(method="onTake",at=@At(value="INVOKE",target="Lnet/minecraft/world/item/trading/Merchant;notifyTrade(Lnet/minecraft/world/item/trading/MerchantOffer;)V"))
    private void dfs$paidTrade(Player player,ItemStack stack,CallbackInfo ci) {
        if(!(player instanceof ServerPlayer server)||!(merchant instanceof net.minecraft.world.entity.npc.AbstractVillager))return;
        var offer=slots.getActiveOffer();
        if(offer!=null&&offer.getResult().is(Items.EMERALD))
            DepartmentTasks.signal(server,"trade.emerald",offer.getResult().getCount());
    }
}
