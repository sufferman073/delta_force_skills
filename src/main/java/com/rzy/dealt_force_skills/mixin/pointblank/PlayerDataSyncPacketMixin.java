package com.rzy.dealt_force_skills.mixin.pointblank;

import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/** Isolate queued packet data from subsequent server-thread player changes. */
@Pseudo
@Mixin(targets = "com.vicmatskiv.pointblank.network.ClientBoundPlayerDataSyncPacket", remap = false)
public abstract class PlayerDataSyncPacketMixin {
    @Shadow private CompoundTag playerData;
    @Shadow private List<Integer> enabledItems;

    @Inject(method = "<init>(Lnet/minecraft/nbt/CompoundTag;Ljava/util/List;)V", at = @At("RETURN"))
    private void dfs$snapshot(CompoundTag data, List<Integer> items, CallbackInfo ci) {
        playerData = data == null ? null : data.copy();
        enabledItems = new ArrayList<>(items);
    }
}
