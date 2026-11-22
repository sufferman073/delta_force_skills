package com.rzy.dealt_force_skills.client.visual;

import org.joml.Matrix4f;
import org.joml.Vector3f;

final class ConsumableStage {
    static Vector3f shoulder(int side) { return new Vector3f(side*.235F, -.22F, .065F); }

    static Matrix4f arm(int side, Vector3f palm, float along) {
        Vector3f back = shoulder(side).sub(palm);
        float length = back.length();
        return new Matrix4f().translation(palm).rotateTowards(back.div(Math.max(.001F, length)), new Vector3f(0, 1, 0))
                .scale(1, 1, length/(along*.75F*ConsumableContact.ARM_SCALE));
    }

    static Vector3f receive(float phase, Vector3f mouth) {
        Vector3f palm = new Vector3f(-.145F, -.215F, -.405F);
        float lift = ConsumableMotion.ramp(phase, .61F, .795F)*(1-ConsumableMotion.ramp(phase, .83F, .985F));
        return palm.lerp(mouth, lift);
    }

    static Vector3f bottleLip(String item) {
        return new Vector3f(0, (item.equals("dve_painkiller") ? 2.55F : 3.55F)/16, 0);
    }

    static Matrix4f bottle(String item, float phase, float equip) {
        float pour = ConsumableMotion.ramp(phase, .30F, .465F)*(1-ConsumableMotion.ramp(phase, .58F, .72F));
        Matrix4f base = new Matrix4f().translation(.16F, -.245F-.30F*equip, -.47F).rotateZ(2.17F*pour).scale(.48F);
        Vector3f lip = base.transformPosition(bottleLip(item));
        Vector3f shift = new Vector3f(-.145F, -.119F, -.405F).sub(lip).mul(pour);
        return base.setTranslation(base.getTranslation(new Vector3f()).add(shift));
    }

    static Vector3f tablet(Vector3f lip, Vector3f palm, float phase) {
        float drop = ConsumableMotion.ramp(phase, .465F, .565F);
        return new Vector3f(lip).lerp(palm, drop);
    }

    static Matrix4f catchArm(int side, Vector3f surface) {
        Vector3f center = new Vector3f(surface);
        for (int index = 0; index < 5; index++) {
            Vector3f up = arm(side, center, .93F).transformDirection(0, ConsumableContact.HALF_DEPTH, 0, new Vector3f());
            center.set(surface).sub(up);
        }
        return arm(side, center, .93F);
    }

    static float plateExposure(float phase, Vector3f top, Vector3f slot) {
        return phase < .70F ? ConsumableMotion.ramp(phase, 0, .09F)
                : Math.max(0, Math.min(1, (top.y-slot.y)/(.48F*5.35F/16)));
    }

    static Matrix4f caseFrame() { return new Matrix4f().translation(-.145F, -.28F, -.46F).scale(.48F); }

    static Vector3f plateTop(float phase, Vector3f slot) {
        Vector3f port = caseFrame().transformPosition(0, .11F, 0, new Vector3f());
        Vector3f raised = new Vector3f(slot).add(.035F, .166F, -.085F);
        return port.lerp(new Vector3f(port).add(0, .165F, 0), ConsumableMotion.ramp(phase, .06F, .29F))
                .lerp(raised, ConsumableMotion.ramp(phase, .30F, .60F))
                .lerp(new Vector3f(slot).add(0, .166F, 0), ConsumableMotion.ramp(phase, .60F, .70F))
                .lerp(slot, ConsumableMotion.ramp(phase, .70F, .90F));
    }

    static Vector3f orbit(float phase, float width) {
        float angle = ConsumableContact.turns(phase);
        Vector3f edge = ConsumableDressing.surface(angle, width);
        float cosine = (float)Math.cos(angle), sine = (float)Math.sin(angle);
        return ConsumableContact.forearm(1, phase).transformPosition(edge.add(.095F*cosine, .095F*sine, 0));
    }

    private ConsumableStage() {}
}
