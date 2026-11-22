package com.rzy.dealt_force_skills.client.visual;

import com.rzy.dealt_force_skills.skill.SkillModelVisual;
import com.rzy.dealt_force_skills.skill.ToolHandCue;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;

final class ThirdPersonSkillPose {
    private ThirdPersonSkillPose() {}

    static boolean owns(SkillModelVisual action) {
        return action == SkillModelVisual.MANBA_ELBOW || action == SkillModelVisual.MANBA_FLASHLIGHT_TOGGLE
                || action == SkillModelVisual.UNDEAD_ROGUE_BATON || HeldToolThirdPersonVisuals.isNikaidouAttackVisual(action);
    }

    static boolean action(PlayerModel<?> model, HumanoidArm main, SkillModelVisual action, float ticks) {
        ModelPart arm = main == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
        int side = main == HumanoidArm.RIGHT ? 1 : -1;
        if (action == SkillModelVisual.MANBA_FLASHLIGHT_TOGGLE) {
            aimLight(model, arm);
            return true;
        }
        float impact = action.impactTick();
        float wind = impact * .40F;
        float raise = smooth(ticks / wind);
        float hit = smooth((ticks - wind) / (impact - wind));
        float settle = smooth((ticks - impact) / (action.durationTicks() - impact));
        float pitch, yaw, roll;
        if (action == SkillModelVisual.MANBA_ELBOW) {
            pitch = -.7F - .88F * raise + .55F * settle;
            yaw = .60F * raise - 1.55F * hit + .68F * settle;
            roll = .70F * raise - .95F * hit + .22F * settle;
        } else if (action == SkillModelVisual.UNDEAD_ROGUE_BATON || action == SkillModelVisual.NIKAIDOU_HOT_IRON_OVERHEAD) {
            pitch = -.48F - 2.28F * raise + 2.43F * hit - .14F * settle;
            yaw = -.11F; roll = -.08F;
        } else if (action == SkillModelVisual.NIKAIDOU_HOT_IRON) {
            pitch = -.4F + .23F * raise - 1.93F * hit + 1.22F * settle;
            yaw = .18F - .54F * hit; roll = .18F * raise;
        } else {
            boolean reverse = action == SkillModelVisual.NIKAIDOU_RITUAL_SWORD;
            pitch = -.62F - (action == SkillModelVisual.NIKAIDOU_RITUAL_SWORD_DIAGONAL ? 1.43F : .62F) * raise
                    + (action == SkillModelVisual.NIKAIDOU_RITUAL_SWORD_DIAGONAL ? 1.38F : .16F) * hit;
            yaw = (reverse ? -1 : 1) * (.93F * raise - 1.88F * hit + .77F * settle);
            roll = -.16F * raise;
        }
        arm.setRotation(pitch, side * yaw, side * roll);
        return true;
    }

    static void aimLight(PlayerModel<?> model, ModelPart arm) {
        arm.setRotation(-Mth.HALF_PI + model.head.xRot, model.head.yRot, 0);
    }

    static boolean pullsPin(ClientToolHandMotion.Frame frame) {
        return (frame.cue() == ToolHandCue.LUNA_COMPOSITE_GRENADE || frame.cue() == ToolHandCue.SHEPHERD_GRENADE)
                && frame.release() < .38F;
    }

    static boolean tool(PlayerModel<?> model, HumanoidArm main, ClientToolHandMotion.Frame frame) {
        if (frame == null) return false;
        ModelPart arm = main == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
        float side = main == HumanoidArm.RIGHT ? 1 : -1;
        switch (frame.cue()) {
            case ULURU_COVER -> {
                float wind = frame.release() < 0 ? frame.wind() : 1 - smooth(frame.release() / .38F);
                float toss = frame.release() < 0 ? 0 : smooth(frame.release() / .38F) * (1 - smooth((frame.release() - .38F) / .62F));
                arm.setRotation(-.70F + .45F * wind - .96F * toss, side * (.08F + .22F * wind), side * .12F);
            }
            case LUNA_COMPOSITE_GRENADE, SHEPHERD_GRENADE -> {
                if (!pullsPin(frame)) {
                    arm.setRotation(-1.65F + .95F * smooth((frame.release() - .38F) / .62F), 0, 0);
                } else ThirdPersonToolGrip.grenade(model, main, frame.draw(), frame.release());
            }
            case SINEVA_HOOK, LUNA_BOW -> { }
            default -> { return false; }
        }
        return true;
    }

    static float smooth(float value) {
        float clamped = Mth.clamp(value, 0, 1);
        return clamped * clamped * (3 - 2 * clamped);
    }
}
