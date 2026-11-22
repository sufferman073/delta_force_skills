package com.rzy.dealt_force_skills.client.visual;

import com.mojang.blaze3d.vertex.PoseStack;
import com.rzy.dealt_force_skills.client.renderer.BlockbenchAnimatedModelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.Set;

final class ConsumableDressing {
    private static final ResourceLocation CLOTH = new ResourceLocation("dealt_force_skills", "consumable_contact_cloth");
    private static final Set<String> STRIP = Set.of("roll", "pad"), ROLL = Set.of("strip", "pad"), PAD = Set.of("strip", "roll");
    private static final float STEP = (float)(Math.PI / 4);

    static Vector3f surface(float angle, float width) {
        float cosine = (float)Math.cos(angle), sine = (float)Math.sin(angle);
        float edge = Math.max(Math.abs(cosine), Math.abs(sine));
        return new Vector3f(cosine*(width+.005F)/edge, sine*(ConsumableContact.HALF_DEPTH+.005F)/edge,
                .042F - .092F*angle/(float)(Math.PI*3.5));
    }

    static float reveal(float fraction) {
        return ConsumableMotion.ramp(fraction, .12F, .235F)*(1-ConsumableMotion.ramp(fraction, .865F, .97F));
    }

    static Vector3f extracted(ConsumableMotion action, Matrix4f bag, float fraction, float width) {
        Vector3f pocket = bag.transformPosition(-.065F, .055F, .11F, new Vector3f());
        Vector3f destination = action == ConsumableMotion.SURGERY ? ConsumableStage.orbit(fraction, width)
                : ConsumableContact.forearm(1, fraction).transformPosition(0, ConsumableContact.HALF_DEPTH+.009F, -.01F, new Vector3f());
        float transfer = ConsumableMotion.ramp(fraction, .14F, .37F)*(1-ConsumableMotion.ramp(fraction, .79F, .955F));
        return pocket.lerp(destination, transfer);
    }

    static void draw(ConsumableMotion action, int side, float fraction, Matrix4f base, float width, PoseStack pose,
                     MultiBufferSource buffers, int light, Vector3f grip) {
        boolean looseRoll = action == ConsumableMotion.SURGERY;
        float reveal = reveal(fraction);
        Vector3f carrier = action == ConsumableMotion.WRAP ? base.getTranslation(new Vector3f()) : extracted(action, base, fraction, width);
        pose.pushPose();
        pose.scale(side, 1, 1);
        if (action != ConsumableMotion.WRAP && reveal > .001F) {
            pose.pushPose();
            pose.translate(carrier.x, carrier.y, carrier.z);
            if (looseRoll) pose.mulPose(com.mojang.math.Axis.XP.rotation(ConsumableContact.turns(fraction)));
            pose.scale(.61F*reveal, .61F*reveal, .61F*reveal);
            model(pose, buffers, light, looseRoll ? ROLL : PAD);
            pose.popPose();
            if (!looseRoll) grip.lerp(new Vector3f(side*carrier.x, carrier.y, carrier.z), reveal);
        }
        float angle = ConsumableContact.turns(fraction);
        if (looseRoll || action == ConsumableMotion.WRAP) {
            float take = looseRoll ? reveal : 1;
            if (take > .001F) {
                Vector3f end = new Vector3f(carrier).add(.038F, 0, .014F);
                grip.lerp(end.mul(side, 1, 1), take);
            }
        }
        if (angle > .001F && action != ConsumableMotion.FIRST_AID) {
            Matrix4f frame = ConsumableContact.forearm(1, fraction);
            Vector3f previous = frame.transformPosition(surface(0, width));
            Vector3f axis = frame.transformDirection(new Vector3f(0, 0, 1));
            for (int index = 1; (index-1)*STEP < angle; index++) {
                Vector3f next = frame.transformPosition(surface(Math.min(index*STEP, angle), width));
                strip(previous, next, axis, pose, buffers, light);
                previous = next;
            }
            if (fraction < .825F) {
                Vector3f local = new Matrix4f(frame).invert().transformPosition(new Vector3f(carrier));
                Vector3f edge = surface(angle, width);
                for (int corner = (int)Math.floor((angle+STEP)/(2*STEP)); corner < 8; corner++) {
                    boolean clearX = Math.abs(edge.x) >= width+.0049F && edge.x*(local.x-edge.x) >= 0;
                    boolean clearY = Math.abs(edge.y) >= ConsumableContact.HALF_DEPTH+.0049F && edge.y*(local.y-edge.y) >= 0;
                    if (clearX || clearY) break;
                    edge = surface((corner*2+1)*STEP, width);
                    Vector3f next = frame.transformPosition(new Vector3f(edge));
                    strip(previous, next, axis, pose, buffers, light);
                    previous = next;
                }
                strip(previous, carrier, axis, pose, buffers, light);
            }
        }
        pose.popPose();
    }


    private static void strip(Vector3f start, Vector3f end, Vector3f axis, PoseStack pose, MultiBufferSource buffers, int light) {
        Vector3f tangent = new Vector3f(end).sub(start);
        if (tangent.lengthSquared() < .000001F) return;
        Vector3f normal = new Vector3f(axis).cross(tangent);
        if (normal.lengthSquared() < .0000001F) return;
        normal.normalize();
        Vector3f across = new Vector3f(tangent).cross(normal).normalize().mul(.048F);
        Matrix4f span = new Matrix4f().set(tangent.x, tangent.y, tangent.z, 0, normal.x*.065F, normal.y*.065F, normal.z*.065F, 0,
                across.x, across.y, across.z, 0, start.x, start.y, start.z, 1);
        pose.pushPose();
        ConsumableContact.apply(pose, span);
        model(pose, buffers, light, STRIP);
        pose.popPose();
    }

    private static void model(PoseStack pose, MultiBufferSource buffers, int light, Set<String> hidden) {
        BlockbenchAnimatedModelRenderer.render(CLOTH, null, 0, pose, buffers, light, hidden);
    }

    private ConsumableDressing() {}
}
