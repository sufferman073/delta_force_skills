package com.rzy.dealt_force_skills.mixin.client;

import com.rzy.dealt_force_skills.client.visual.TacticalPlayerPose;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerModel.class)
public abstract class TacticalPlayerModelMixin {
    @Unique private PartPose[] dfs$base;
    @Unique private ModelPart[] dfs$parts() {
        PlayerModel<?> m = (PlayerModel<?>)(Object)this;
        return new ModelPart[]{m.head, m.hat, m.body, m.leftArm, m.rightArm, m.leftLeg, m.rightLeg,
                m.jacket, m.leftSleeve, m.rightSleeve, m.leftPants, m.rightPants};
    }
    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("HEAD"))
    private void dfs$restore(LivingEntity entity, float a, float b, float c, float d, float e, CallbackInfo ci) {
        if (dfs$base != null) {
            ModelPart[] parts = dfs$parts();
            for (int i = 0; i < parts.length; i++)
                if (dfs$base[i] != null) parts[i].loadPose(dfs$base[i]);
            dfs$base = null;
        }
    }
    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void dfs$animate(LivingEntity entity, float a, float b, float age, float d, float e, CallbackInfo ci) {
        if (!(entity instanceof Player player)) return;
        ModelPart[] parts = dfs$parts(); dfs$base = new PartPose[parts.length];
        for (int i = 0; i < parts.length; i++) dfs$base[i] = parts[i].storePose();
        TacticalPlayerPose.apply((PlayerModel<?>)(Object)this, player, age);
        com.rzy.dealt_force_skills.client.visual.KnockdownPlayerPose.apply((PlayerModel<?>)(Object)this, player);
        // Restore only what we changed; never feed a previous gun animation back into its owner.
        for (int i = 0; i < parts.length; i++) {
            PartPose before = dfs$base[i], after = parts[i].storePose();
            if (before.x == after.x && before.y == after.y && before.z == after.z
                    && before.xRot == after.xRot && before.yRot == after.yRot && before.zRot == after.zRot)
                dfs$base[i] = null;
        }
    }
}
