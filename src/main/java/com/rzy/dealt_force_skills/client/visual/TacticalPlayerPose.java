package com.rzy.dealt_force_skills.client.visual;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.rzy.dealt_force_skills.motion.TacticalMotion;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class TacticalPlayerPose {
    private static final float RAD = Mth.DEG_TO_RAD;
    private TacticalPlayerPose() { }
    private record ArmFrame(Quaternionf torso, float rootPitch, float resetWeight) { }
    private static final java.util.Map<ModelPart, ArmFrame> GUN_ARMS = new java.util.WeakHashMap<>();

    /** Applied after all pose writers, for both drawing arms and translateToHand attachments. */
    public static void armRoot(ModelPart arm, PoseStack stack) {
        ArmFrame frame = GUN_ARMS.get(arm);
        if (frame == null) return;
        var initial = arm.getInitialPose();
        Vector3f pivot = new Vector3f(Mth.lerp(frame.resetWeight, arm.x, initial.x),
                Mth.lerp(frame.resetWeight, arm.y, initial.y) - 12,
                Mth.lerp(frame.resetWeight, arm.z, initial.z)).rotate(frame.torso);
        stack.translate(pivot.x / 16F, (pivot.y + 12) / 16F, pivot.z / 16F);
        stack.mulPose(Axis.XP.rotationDegrees(-frame.rootPitch));
        stack.translate(-arm.x / 16F, -arm.y / 16F, -arm.z / 16F);
    }

    public static void apply(PlayerModel<?> model, Player player, float ageInTicks) {
        if (ageInTicks <= 0) {
            apply(model, player, ageInTicks, null, false, false);
            return;
        }
        apply(model, player, ageInTicks, ClientTacticalMotion.frame(player, Mth.clamp(ageInTicks - player.tickCount, 0, 1)),
                TacticalGunCompat.holdingGun(player), com.rzy.dealt_force_skills.client.character.ClientHeldToolVisualState.visual(player)
                != com.rzy.dealt_force_skills.skill.HeldToolVisual.NONE);
    }

    public static void apply(PlayerModel<?> model, net.minecraft.world.entity.LivingEntity player, float ageInTicks,
                             ClientTacticalMotion.Frame frame, boolean holdingGun, boolean holdingTool) {
        for (ModelPart arm : new ModelPart[]{model.leftArm, model.rightArm, model.leftSleeve, model.rightSleeve})
            GUN_ARMS.remove(arm);
        if (com.rzy.dealt_force_skills.character.sineva.SinevaKnockdownState.active(player)) return;
        if (ageInTicks <= 0) return; // Vanilla first-person hand setup has no world animation time.
        float partial = Mth.clamp(ageInTicks - player.tickCount, 0, 1);
        if (frame == null) return;
        var p = frame.pose(); float w = p.weight;
        boolean gun = frame.kind().keepsGunPose() && holdingGun && !holdingTool;
        if (frame.kind().sustained() && !frame.kind().upperBodyOnly()) {
            // Vanilla swimming/crouching also shifts pivots. Own both the pivots and rotations.
            for (ModelPart part : new ModelPart[]{model.body, model.head, model.leftArm, model.rightArm,
                    model.leftLeg, model.rightLeg}) {
                if (gun && (part == model.leftArm || part == model.rightArm)) continue;
                var initial = part.getInitialPose();
                part.setPos(Mth.lerp(w, part.x, initial.x), Mth.lerp(w, part.y, initial.y),
                        Mth.lerp(w, part.z, initial.z));
            }
        }
        // Six-part fallback: do not invent elbow/knee bones or alter the player's mesh.
        if (!frame.kind().upperBodyOnly()) rotate(model.body, 0, 0, 0, w);
        rotate(model.head, p.headPitch, (frame.kind() == TacticalMotion.ROLL || frame.kind() == TacticalMotion.JET) ? 0 : model.head.yRot / RAD, 0, w);
        if (!frame.kind().upperBodyOnly()) {
            rotate(model.leftLeg, p.leftLeg, 0, p.leftLegRoll, w);
            rotate(model.rightLeg, p.rightLeg, 0, p.rightLegRoll, w);
        }
        boolean protectArms = gun || player.isUsingItem() && frame.kind().looping();
        if (!protectArms) {
            boolean mirror = (frame.kind() == TacticalMotion.HEAVY_SWING || frame.kind() == TacticalMotion.LIGHT_SLASH
                    || frame.kind() == TacticalMotion.THROW) && player.getMainArm() == HumanoidArm.LEFT;
            var arms = frame.kind().looping()
                    && holdingTool
                    ? TacticalMotion.HOLD_TOOL.sample(4, 20, 0, 0, 1, 0) : p;
            rotate(model.leftArm, mirror ? arms.rightArm : arms.leftArm,
                    mirror ? -arms.rightArmYaw : arms.leftArmYaw, mirror ? -arms.rightArmRoll : arms.leftArmRoll, arms == p ? w : 1);
            rotate(model.rightArm, mirror ? arms.leftArm : arms.rightArm,
                    mirror ? -arms.leftArmYaw : arms.rightArmYaw, mirror ? -arms.leftArmRoll : arms.rightArmRoll, arms == p ? w : 1);
        }
        // Vanilla player parts are siblings; explicitly carry head/shoulders with the torso.
        Quaternionf torso = new Quaternionf().rotationXYZ(p.bodyPitch * RAD * w, p.bodyYaw * RAD * w, 0);
        carry(model.body, torso);
        carry(model.head, torso);
        if (gun) {
            ArmFrame grip = new ArmFrame(torso, p.rootPitch, frame.kind().sustained() ? w : 0);
            for (ModelPart arm : new ModelPart[]{model.leftArm, model.rightArm, model.leftSleeve, model.rightSleeve})
                GUN_ARMS.put(arm, grip);
        } else if (frame.kind() == TacticalMotion.SLIDE) {
            // Ordinary slide arms point forward in level model space.
            carryLevelArm(model.leftArm, torso, p.rootPitch * RAD);
            carryLevelArm(model.rightArm, torso, p.rootPitch * RAD);
        } else if (!protectArms) { carry(model.leftArm, torso); carry(model.rightArm, torso); }
        model.hat.copyFrom(model.head);
        model.jacket.copyFrom(model.body);
        model.leftSleeve.copyFrom(model.leftArm); model.rightSleeve.copyFrom(model.rightArm);
        model.leftPants.copyFrom(model.leftLeg); model.rightPants.copyFrom(model.rightLeg);
    }

    private static void rotate(ModelPart part, float x, float y, float z, float w) {
        part.xRot = part.xRot + w * Mth.wrapDegrees(x - part.xRot / RAD) * RAD;
        part.yRot = part.yRot + w * Mth.wrapDegrees(y - part.yRot / RAD) * RAD;
        part.zRot = part.zRot + w * Mth.wrapDegrees(z - part.zRot / RAD) * RAD;
    }

    private static void carry(ModelPart part, Quaternionf torso) {
        Vector3f pivot = new Vector3f(part.x, part.y - 12, part.z).rotate(torso);
        part.setPos(pivot.x, pivot.y + 12, pivot.z);
        Vector3f angles = new Quaternionf(torso).mul(new Quaternionf().rotationXYZ(part.xRot, part.yRot, part.zRot))
                .getEulerAnglesXYZ(new Vector3f());
        part.xRot = angles.x; part.yRot = angles.y; part.zRot = angles.z;
    }

    private static void carryLevelArm(ModelPart arm, Quaternionf torso, float rootPitch) {
        Vector3f pivot = new Vector3f(arm.x, arm.y - 12, arm.z).rotate(torso);
        arm.setPos(pivot.x, pivot.y + 12, pivot.z);
        Quaternionf rotation = new Quaternionf().rotationX(-rootPitch)
                // ModelPart renders Z-Y-X; using XYZ here can flip a tilted gun behind the body.
                .mul(new Quaternionf().rotationZYX(arm.zRot, arm.yRot, arm.xRot));
        // Extract Rz * Ry * Rx directly; retain all grip axes, including yaw and roll.
        double x = rotation.x, y = rotation.y, z = rotation.z, w = rotation.w;
        double s = 2 / (x * x + y * y + z * z + w * w);
        double r00 = 1 - s * (y * y + z * z), r10 = s * (x * y + w * z);
        double cosY = Math.hypot(r00, r10);
        arm.yRot = (float)Math.atan2(s * (w * y - x * z), cosY);
        if (cosY > 1.0E-6) {
            arm.xRot = (float)Math.atan2(s * (w * x + y * z), 1 - s * (x * x + y * y));
            arm.zRot = (float)Math.atan2(r10, r00);
        } else {
            // At Y = +/-90 degrees, X and Z share an axis; choose Z = 0.
            arm.xRot = (float)Math.atan2(s * (w * x - y * z), 1 - s * (x * x + z * z));
            arm.zRot = 0;
        }
    }

    public static void root(Player player, PoseStack stack, float bodyYaw, float partial) {
        root(player, stack, bodyYaw, partial, ClientTacticalMotion.frame(player, partial));
    }
    public static void root(net.minecraft.world.entity.LivingEntity player, PoseStack stack, float bodyYaw, float partial,
                            ClientTacticalMotion.Frame frame) {
        if (frame == null) return;
        var p = frame.pose();
        if (frame.kind().ownsRoot() && !frame.kind().looping()) {
            float facing = frame.kind() == TacticalMotion.JET ? player.getViewYRot(partial) : frame.yaw();
            stack.mulPose(Axis.YP.rotationDegrees(Mth.wrapDegrees(bodyYaw - facing) * p.weight));
        }
        // World-up space before vanilla's model-axis flip: negative X tips the face forward.
        float clearance = player.onGround() && (frame.kind() == TacticalMotion.ROLL
                || frame.kind() == TacticalMotion.SLIDE || frame.kind() == TacticalMotion.DIVE)
                ? TacticalMotion.groundLift(p) : 0;
        stack.translate(0, .75F + p.height + clearance, 0);
        stack.mulPose(Axis.XP.rotationDegrees(-p.rootPitch));
        stack.mulPose(Axis.ZP.rotationDegrees(p.rootRoll));
        stack.translate(0, -.75F, 0);
    }

    /** RenderPlayerEvent.Post runs outside LivingEntityRenderer's model-space push/pop. */
    public static void modelSpace(Player player, PoseStack stack, float partial) {
        float yaw = Mth.rotLerp(partial, player.yBodyRotO, player.yBodyRot);
        if (!KnockdownPlayerPose.root(player, stack, 1)) {
            stack.mulPose(Axis.YP.rotationDegrees(180 - yaw));
            root(player, stack, yaw, partial);
        }
        stack.scale(-.9375F, -.9375F, .9375F);
        stack.translate(0, -1.501F, 0);
    }
}
