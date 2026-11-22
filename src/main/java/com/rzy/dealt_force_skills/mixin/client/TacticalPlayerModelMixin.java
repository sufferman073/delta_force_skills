package com.rzy.dealt_force_skills.mixin.client;

import com.rzy.dealt_force_skills.client.visual.TacticalPlayerPose;
import com.rzy.dealt_force_skills.client.visual.TacticalPoseRestorer;
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

@Mixin(value = PlayerModel.class, remap = false)
public abstract class TacticalPlayerModelMixin implements TacticalPoseRestorer {
    @Unique private boolean dfs$handPass;
    @Unique private PartPose[] dfs$base;
    @Unique private PartPose[] dfs$applied;
    @Unique private ModelPart[] dfs$parts() {
        PlayerModel<?> m = (PlayerModel<?>)(Object)this;
        return new ModelPart[]{m.head, m.hat, m.body, m.leftArm, m.rightArm, m.leftLeg, m.rightLeg,
                m.jacket, m.leftSleeve, m.rightSleeve, m.leftPants, m.rightPants};
    }
    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("HEAD"))
    private void dfs$restore(LivingEntity entity, float a, float b, float c, float d, float e, CallbackInfo ci) {
        // First-person hand renderers can call setupAnim without prepareMobModel.
        dfs$restoreTacticalPose();
    }
    @Override
    public void dfs$renderIsolated(Runnable draw, boolean hand) {
        ModelPart[] parts = dfs$parts();
        PartPose[] entry = new PartPose[parts.length];
        for (int index = 0; index < parts.length; index++) entry[index] = parts[index].storePose();
        PartPose[] base = dfs$base, applied = dfs$applied;
        boolean previous = dfs$handPass;
        dfs$handPass = hand;
        try { dfs$restoreTacticalPose(); draw.run(); }
        finally {
            for (int index = 0; index < parts.length; index++) {
                PartPose pose = entry[index];
                parts[index].setPos(pose.x, pose.y, pose.z);
                parts[index].setRotation(pose.xRot, pose.yRot, pose.zRot);
            }
            TacticalPlayerPose.clearArmRoots((PlayerModel<?>)(Object)this);
            dfs$base = base; dfs$applied = applied; dfs$handPass = previous;
        }
    }
    @Override
    public void dfs$restoreTacticalPose() {
        TacticalPlayerPose.clearArmRoots((PlayerModel<?>)(Object)this);
        if (dfs$base != null) {
            ModelPart[] parts = dfs$parts();
            for (int i = 0; i < parts.length; i++) {
                ModelPart part = parts[i];
                PartPose before = dfs$base[i], after = dfs$applied[i];
                // Undo only our writes that are still present. Preserve fresh third-party poses.
                if (before.x != after.x && part.x == after.x) part.x = before.x;
                if (before.y != after.y && part.y == after.y) part.y = before.y;
                if (before.z != after.z && part.z == after.z) part.z = before.z;
                if (before.xRot != after.xRot && part.xRot == after.xRot) part.xRot = before.xRot;
                if (before.yRot != after.yRot && part.yRot == after.yRot) part.yRot = before.yRot;
                if (before.zRot != after.zRot && part.zRot == after.zRot) part.zRot = before.zRot;
            }
            dfs$base = null;
            dfs$applied = null;
        }
    }
    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void dfs$animate(LivingEntity entity, float a, float b, float age, float d, float e, CallbackInfo ci) {
        if (dfs$handPass || !(entity instanceof Player player)) return;
        ModelPart[] parts = dfs$parts(); dfs$base = new PartPose[parts.length];
        for (int i = 0; i < parts.length; i++) dfs$base[i] = parts[i].storePose();
        TacticalPlayerPose.apply((PlayerModel<?>)(Object)this, player, age);
        com.rzy.dealt_force_skills.client.visual.HeldToolThirdPersonVisuals.poseArms(player, (PlayerModel<?>)(Object)this,
                net.minecraft.util.Mth.clamp(age - player.tickCount, 0, 1));
        com.rzy.dealt_force_skills.client.visual.BareSkillHandVisuals.poseArms(player, (PlayerModel<?>)(Object)this,
                net.minecraft.util.Mth.clamp(age - player.tickCount, 0, 1));
        com.rzy.dealt_force_skills.client.visual.KnockdownPlayerPose.apply((PlayerModel<?>)(Object)this, player);
        dfs$applied = new PartPose[parts.length];
        for (int i = 0; i < parts.length; i++) dfs$applied[i] = parts[i].storePose();
    }
}
