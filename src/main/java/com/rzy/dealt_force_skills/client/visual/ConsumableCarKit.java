package com.rzy.dealt_force_skills.client.visual;

import com.mojang.blaze3d.vertex.PoseStack;
import com.rzy.dealt_force_skills.client.renderer.BlockbenchAnimatedModelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Set;

final class ConsumableCarKit {
    private static final ResourceLocation LINE_SET = new ResourceLocation("dealt_force_skills", "consumable_vehicle_line_set");
    private static final Set<String> HIDE_LINE = Set.of("feed_link"), ONLY_LINE = Set.of("instrument");

    static float lift(float phase) {
        return ConsumableMotion.ramp(phase, .16F, .365F)*(1-ConsumableMotion.ramp(phase, .77F, .95F));
    }

    static Vector3f port(String item, Matrix4f bag) {
        Vector3f seam = switch (item) {
            case "field_first_aid_kit" -> new Vector3f(-2.9F, 3.4F, -.15F);
            case "outdoor_medical_kit" -> new Vector3f(-3.1F, -.08F, 3.62F);
            case "battlefield_medical_kit" -> new Vector3f(-3, 3.26F, -.18F);
            case "car_first_aid_kit" -> new Vector3f(-1.75F, 4.27F, .55F);
            default -> throw new IllegalArgumentException("No transfusion kit: " + item);
        };
        return bag.transformPosition(seam.div(16));
    }

    static Matrix4f syringe(Vector3f port, float phase, float width) {
        float work = lift(phase);
        Vector3f skin = ConsumableContact.forearm(1, phase).transformPosition(width+.002F, .035F, -.080F, new Vector3f());
        Vector3f tip = new Vector3f(port).lerp(skin, work);
        tip.y += .075F*4*work*(1-work);
        return new Matrix4f().translation(tip).rotateZ(1.16F-.96F*work).rotateY(-.13F*work)
                .scale(.52F*ConsumableDressing.reveal(phase));
    }

    static Vector3f grip(Matrix4f instrument) {
        return instrument.transformPosition(6.15F/16, -1.35F/16, .35F/16, new Vector3f());
    }

    static void atForearm(String item, Matrix4f bag, float phase, float width, int side, PoseStack pose, MultiBufferSource buffers,
                            int light, Vector3f holder) {
        float visible = ConsumableDressing.reveal(phase);
        if (visible < .001F) return;
        Vector3f port = port(item, bag);
        Matrix4f instrument = syringe(port, phase, width);
        draw(instrument, port, phase, .060F*visible, pose, buffers, light);
        holder.lerp(grip(instrument).mul(side, 1, 1), visible);
    }

    static void thirdPerson(String item, float phase, Vector3f skin, PoseStack pose, MultiBufferSource buffers, int light) {
        float visible = ConsumableDressing.reveal(phase);
        if (visible < .001F) return;
        float work = lift(phase);
        Vector3f port = port(item, new Matrix4f());
        Vector3f tip = new Vector3f(port).lerp(skin, work);
        Matrix4f instrument = new Matrix4f().translation(tip).rotateZ(1.2F-1.42F*work).scale(.92F*visible);
        draw(instrument, port, phase, .085F*visible, pose, buffers, light);
    }

    private static void draw(Matrix4f instrument, Vector3f port, float phase, float sag, PoseStack pose,
                             MultiBufferSource buffers, int light) {
        pose.pushPose();
        ConsumableContact.apply(pose, instrument);
        BlockbenchAnimatedModelRenderer.render(LINE_SET, "use", phase, pose, buffers, light, HIDE_LINE);
        pose.popPose();
        Vector3f hub = instrument.transformPosition(3.65F/16, -1.05F/16, .25F/16, new Vector3f());
        Vector3f start = port;
        for (int link = 1; link <= 6; link++) {
            float along = link/6F, slack = 4*along*(1-along)*sag;
            Vector3f end = new Vector3f(port).lerp(hub, along).add(-slack*.40F, -slack, slack*.52F);
            Vector3f axis = new Vector3f(end).sub(start);
            float length = axis.length();
            if (length > .0001F) {
                axis.div(length);
                pose.pushPose();
                pose.translate(start.x, start.y, start.z);
                pose.mulPose(new Quaternionf().rotationTo(1, 0, 0, axis.x, axis.y, axis.z));
                pose.scale(length+.0005F, 1, 1);
                BlockbenchAnimatedModelRenderer.render(LINE_SET, null, 0, pose, buffers, light, ONLY_LINE);
                pose.popPose();
            }
            start = end;
        }
    }

    private ConsumableCarKit() {}
}
