package com.rzy.dealt_force_skills.client.visual;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.rzy.dealt_force_skills.client.character.ClientCharacterSkinState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.PlayerModelPart;
import org.joml.Matrix4f;
import org.joml.Vector3f;

final class ConsumableContact {
    static final float ARM_SCALE = .59F;
    static final float HALF_DEPTH = ARM_SCALE * 2 / 16;
    static final float HALF_LENGTH = ARM_SCALE * 6 / 16;

    static float hold(float fraction) {
        return ConsumableMotion.ramp(fraction, .10F, .34F) * (1 - ConsumableMotion.ramp(fraction, .755F, .96F));
    }

    static boolean onForearm(ConsumableMotion action) {
        return action == ConsumableMotion.INJECT || action == ConsumableMotion.OE2 || action == ConsumableMotion.WRAP
                || action == ConsumableMotion.TOURNIQUET || action == ConsumableMotion.SURGERY || action == ConsumableMotion.FIRST_AID;
    }

    static Matrix4f forearm(int side, float fraction) {
        float reach = ConsumableMotion.ramp(fraction, .035F, .32F) * (1 - ConsumableMotion.ramp(fraction, .80F, 1));
        Vector3f palm = new Vector3f(-.177F+.037F*reach, -.234F+.025F*reach, -.388F-.035F*reach);
        return new Matrix4f().scaling(side, 1, 1).mul(ConsumableStage.arm(-1, palm, .77F));
    }

    static Vector3f station(ConsumableMotion action, float fraction) {
        float thread = .28F*(1 - ConsumableMotion.ramp(fraction, .09F, .34F) + ConsumableMotion.ramp(fraction, .82F, .98F));
        return forearm(1, fraction).transformPosition(0, 0, action == ConsumableMotion.TOURNIQUET ? -thread : 0, new Vector3f());
    }

    static float turns(float fraction) {
        return ConsumableMotion.ramp(fraction, .265F, .80F) * (float)(Math.PI*3.5);
    }


    static float armWidth(net.minecraft.world.entity.player.Player player, HumanoidArm arm) {
        if (!(player instanceof net.minecraft.client.player.AbstractClientPlayer client)
                || !(Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(client) instanceof PlayerRenderer renderer)) return HALF_DEPTH;
        ModelPart part = arm == HumanoidArm.RIGHT ? renderer.getModel().rightArm : renderer.getModel().leftArm;
        float[] width = {HALF_DEPTH};
        part.visit(new PoseStack(), (ignored, path, index, cube) -> {
            if (path.isEmpty() && index == 0) width[0] = (cube.maxX-cube.minX)*ARM_SCALE/32F;
        });
        return width[0];
    }

    static void apply(PoseStack pose, Matrix4f transform) {
        pose.mulPose(transform);
    }

    static Vector3f rim(String path) {
        return switch (path) {
            case "asara_style" -> new Vector3f(0.036882F, 0.221875F, 0.137644F);
            case "cranberry_special" -> new Vector3f(0.022108F, 0.144375F, 0.167926F);
            case "guanshan_nanyue" -> new Vector3f(0.028308F, 0.012500F, 0.215020F);
            case "heartbeat" -> new Vector3f(-0.005655F, 0.273750F, 0.151144F);
            case "highball" -> new Vector3f(0.043190F, 0.161250F, 0.161189F);
            case "long_goodbye" -> new Vector3f(0.036955F, 0.154375F, 0.280703F);
            case "mist_morning" -> new Vector3f(0.032795F, 0.147500F, 0.249101F);
            case "mountain_rain" -> new Vector3f(-0.005048F, 0.230000F, 0.134906F);
            case "reis_dance" -> new Vector3f(0.012037F, 0.006875F, 0.229685F);
            case "sky_island" -> new Vector3f(-0.004721F, 0.200000F, 0.126162F);
            case "starlight_glitter" -> new Vector3f(0.040764F, 0.210625F, 0.152133F);
            case "tulip" -> new Vector3f(-0.043514F, 0.115625F, 0.162396F);
            case "wild_rose" -> new Vector3f(0.058720F, 0.041875F, 0.219144F);
            default -> throw new IllegalArgumentException("Unknown drinking glass: " + path);
        };
    }

    static Vector3f needleTip(String path) {
        return new Vector3f(0, switch (path) {
            case "dark_zone_rainbow_injection" -> -8.39F;
            case "experimental_iron_curtain_catalyst" -> -7.91F;
            case "m1_muscle_booster" -> -8.15F;
            case "m1_muscle_injection" -> -8.08F;
            case "norepinephrine" -> -7.9F;
            case "oe2_combat_stimulant" -> -6.52F;
            case "perception_activation_injection" -> -8.07F;
            case "perception_activator" -> -7.89F;
            case "prototype_madness_compound" -> -7.97F;
            case "raven_shadow_extract" -> -7.83F;
            case "simple_injector" -> -8.15F;
            case "stamina_activation_injection" -> -7.63F;
            case "stamina_enhancer" -> -8.04F;
            case "strong_injector" -> -8.24F;
            default -> throw new IllegalArgumentException("Unknown applicator: " + path);
        } / 16F, 0);
    }

    static Matrix4f base(ConsumableMotion action, String item, float fraction, float ticks, float equip, boolean assist) {
        ConsumableMotion.Pose move = action.sample(fraction, ticks);
        float contact = hold(fraction);
        float pitch = move.pitch(), roll = move.roll();
        Vector3f socket = new Vector3f(), target = null;
        float scale = .52F;
        if (action == ConsumableMotion.DRINK) {
            socket = rim(item);
            target = new Vector3f(.008F, -.109F, -.174F);
            pitch = 73*contact;
            scale -= .13F*contact;
        } else if (assist && action == ConsumableMotion.TOURNIQUET) {
            Vector3f center = station(action, fraction);
            return forearm(1, fraction).normalize3x3().setTranslation(center).scale(.58F);
        } else if (assist && (action == ConsumableMotion.INJECT || action == ConsumableMotion.OE2)) {
            socket = needleTip(item);
            target = forearm(1, fraction).transformPosition(.020F, HALF_DEPTH-.003F, 0, new Vector3f());
            roll = -40*contact;
        } else if (assist && action == ConsumableMotion.WRAP) {
            target = ConsumableStage.orbit(fraction, HALF_DEPTH);
            scale -= .26F*contact;
            roll = -18*contact;
        } else if (assist && (action == ConsumableMotion.SURGERY || action == ConsumableMotion.FIRST_AID)) {
            target = new Vector3f(.145F, -.325F, -.665F);
            pitch = 14*contact;
            roll = -4*contact;
            scale -= .11F*contact;
        }
        Matrix4f transform = new Matrix4f().translation(.22F-move.inward(), -.215F+move.lift()-.30F*equip, -.53F+move.nearer())
                .rotateZ((float)Math.toRadians(roll)).rotateX((float)Math.toRadians(pitch)).rotateY(-.21F).scale(scale);
        if (target != null) {
            Vector3f correction = target.sub(transform.transformPosition(socket, new Vector3f())).mul(contact);
            transform.m30(transform.m30()+correction.x).m31(transform.m31()+correction.y).m32(transform.m32()+correction.z);
        }
        return transform;
    }

    static void arm(LocalPlayer player, HumanoidArm which, Matrix4f frame, float along, PoseStack pose, MultiBufferSource buffers, int light) {
        if (!(Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player) instanceof PlayerRenderer renderer)) return;
        boolean right = which == HumanoidArm.RIGHT;
        var skin = ClientCharacterSkinState.skinFor(player).orElseGet(() -> renderer.getTextureLocation(player));
        pose.pushPose();
        apply(pose, frame);
        if (frame.determinant3x3() < 0) pose.scale(-1, 1, 1);
        pose.mulPose(Axis.XP.rotationDegrees(-90));
        pose.scale(ARM_SCALE, ARM_SCALE, ARM_SCALE);
        ModelPart limb = right ? renderer.getModel().rightArm : renderer.getModel().leftArm;
        ModelPart sleeve = right ? renderer.getModel().rightSleeve : renderer.getModel().leftSleeve;
        limb.visit(new PoseStack(), (ignored, path, index, cube) -> {
            if (!path.isEmpty() || index != 0) return;
            pose.translate(-(cube.minX+cube.maxX)/32F, -(cube.minY+along*(cube.maxY-cube.minY))/16F, -(cube.minZ+cube.maxZ)/32F);
            emit(cube, pose, buffers.getBuffer(RenderType.entitySolid(skin)), light);
            if (player.isModelPartShown(right ? PlayerModelPart.RIGHT_SLEEVE : PlayerModelPart.LEFT_SLEEVE))
                sleeve.visit(new PoseStack(), (unused, child, ordinal, overlay) -> {
                    if (child.isEmpty() && ordinal == 0) emit(overlay, pose, buffers.getBuffer(RenderType.entityTranslucent(skin)), light);
                });
        });
        pose.popPose();
    }

    private static void emit(ModelPart.Cube cube, PoseStack pose, VertexConsumer consumer, int light) {
        cube.compile(pose.last(), consumer, light, OverlayTexture.NO_OVERLAY, -1);
    }

    private ConsumableContact() {}
}
