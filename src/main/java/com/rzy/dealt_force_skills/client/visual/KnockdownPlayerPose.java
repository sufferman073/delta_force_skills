package com.rzy.dealt_force_skills.client.visual;
import com.rzy.dealt_force_skills.character.sineva.SinevaKnockdownState;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;
public final class KnockdownPlayerPose {
    private KnockdownPlayerPose() { }
    public static boolean root(LivingEntity entity, com.mojang.blaze3d.vertex.PoseStack stack, float scale) {
        if (!SinevaKnockdownState.active(entity)) return false;
        stack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180-SinevaKnockdownState.yaw(entity)));
        stack.translate(0, SinevaKnockdownState.bodyWidth(entity)*.5/scale,
                -SinevaKnockdownState.bodyLength(entity)*.5/scale);
        stack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90));
        return true;
    }
    public static void apply(HumanoidModel<?> model, LivingEntity entity) {
        if (!SinevaKnockdownState.active(entity)) return;
        for (ModelPart part : new ModelPart[]{model.head, model.hat, model.body, model.leftArm,
                model.rightArm, model.leftLeg, model.rightLeg}) part.loadPose(part.getInitialPose());
        model.crouching=false;
        if (model instanceof PlayerModel<?> player) {
            player.jacket.copyFrom(model.body); player.hat.copyFrom(model.head);
            player.leftSleeve.copyFrom(model.leftArm); player.rightSleeve.copyFrom(model.rightArm);
            player.leftPants.copyFrom(model.leftLeg); player.rightPants.copyFrom(model.rightLeg);
        }
    }
}
