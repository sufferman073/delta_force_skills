package com.rzy.dealt_force_skills.mixin;
import com.rzy.dealt_force_skills.workbench.WorkbenchFishing;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(FishingHook.class)
public abstract class WorkbenchFishingHookMixin implements WorkbenchFishing {
    @Unique private double dfs$pull=1;
    @Unique private boolean dfs$customLure;
    @Shadow private int timeUntilLured;
    @Override public void dfs$configureFishing(double pull,boolean customLure) {
        dfs$pull=Double.isFinite(pull)?Math.max(.05,Math.min(100,pull)):1;dfs$customLure=customLure;
    }
    @ModifyArg(method="pullEntity",at=@At(value="INVOKE",target="Lnet/minecraft/world/phys/Vec3;scale(D)Lnet/minecraft/world/phys/Vec3;"))
    private double dfs$pullImpulse(double scale) {return scale*dfs$pull;}
    @Inject(method="catchingFish",at=@At("RETURN"))
    private void dfs$positiveWait(BlockPos pos,CallbackInfo ci) {
        if(dfs$customLure&&timeUntilLured<0)timeUntilLured=1;
    }
    @Inject(method="addAdditionalSaveData",at=@At("TAIL"))
    private void dfs$save(CompoundTag tag,CallbackInfo ci) {
        if(dfs$pull!=1||dfs$customLure){tag.putDouble("DfsWorkbenchPull",dfs$pull);tag.putBoolean("DfsWorkbenchLure",dfs$customLure);}
    }
    @Inject(method="readAdditionalSaveData",at=@At("TAIL"))
    private void dfs$load(CompoundTag tag,CallbackInfo ci) {
        if(tag.contains("DfsWorkbenchPull"))dfs$configureFishing(tag.getDouble("DfsWorkbenchPull"),tag.getBoolean("DfsWorkbenchLure"));
    }
}
