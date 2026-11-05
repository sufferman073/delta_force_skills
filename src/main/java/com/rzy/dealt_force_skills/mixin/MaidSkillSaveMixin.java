package com.rzy.dealt_force_skills.mixin;

import com.rzy.dealt_force_skills.compat.maid.MaidSkillHost;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Film resurrection calls additional-data reading directly, bypassing Entity.load/ForgeData. */
@Mixin(LivingEntity.class)
public abstract class MaidSkillSaveMixin {
    @Inject(method="addAdditionalSaveData", at=@At("TAIL"))
    private void dfs$saveMaidSkills(CompoundTag tag, CallbackInfo ci) {
        if ((Object)this instanceof MaidSkillHost host) host.dealtForceMaidSkills().write(tag);
    }
    @Inject(method="readAdditionalSaveData", at=@At("TAIL"))
    private void dfs$readMaidSkills(CompoundTag tag, CallbackInfo ci) {
        if ((Object)this instanceof MaidSkillHost host) host.dealtForceMaidSkills().read(tag);
    }
}
