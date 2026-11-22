package com.rzy.dealt_force_skills.client.visual;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.rzy.dealt_force_skills.client.renderer.BlockbenchAnimatedModelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import org.joml.Quaternionf;
import org.joml.Vector3f;

final class ThirdPersonToolGrip {
    static final float TOP_GRIP_X = -5F, TOP_GRIP_Y = 4.8F;

    static Vector3f launcherSideGrip() { return new Vector3f(3.5F,.2F,-5F); }

    private ThirdPersonToolGrip() {}

    private static HumanoidArm carrier(String id, HumanoidArm main) {
        return switch (id) {
            case "sineva_grapple_gun", "luna_composite_bow" -> main.getOpposite();
            default -> main;
        };
    }

    private static boolean paired(String id) {
        return switch (id) {
            case "uluru_missile_launcher", "n_two_condensation_focus", "morse_sonar_detector",
                    "hackclaw_hacking_terminal", "luna_composite_bow" -> true;
            default -> false;
        };
    }

    static boolean occupies(String id, HumanoidArm main, HumanoidArm query) {
        return !id.equals("wildfire_side_launcher") && (paired(id) || carrier(id, main) == query);
    }

    private static float size(HeldToolThirdPersonVisuals.ToolSpec spec) {
        return switch (spec.model().getPath()) {
            case "uluru_missile_launcher" -> 1.05F;
            case "n_two_condensation_focus" -> .76F;
            case "morse_sonar_detector" -> 1F / .9375F;
            case "nikaidou_hiro_hot_iron" -> .72F;
            case "nikaidou_hiro_ritual_sword" -> .78F;
            case "rover_dog_whistle" -> .49F;
            default -> spec.scale();
        };
    }

    private static Vector3f primary(String id, HumanoidArm arm) {
        float side = arm == HumanoidArm.RIGHT ? 1 : -1;
        return switch (id) {
            case "uluru_missile_launcher" -> new Vector3f(-1.53F, -4.44F, 0);
            case "n_two_condensation_focus" -> new Vector3f(TOP_GRIP_X, TOP_GRIP_Y, 0);
            case "vyron_tiger_cannon_launcher" -> new Vector3f(1.8F,-3.5F,0);
            case "morse_sonar_detector" -> new Vector3f(side * 5.3F, .2F, 2.8F);
            case "hackclaw_hacking_terminal" -> new Vector3f(side * 8.8F,0,.3F);
            case "luna_composite_bow" -> new Vector3f(.68F,0,0);
            case "sineva_grapple_gun" -> new Vector3f(-5.75F,-4.5F,0);
            case "rover_aerosol_injector" -> new Vector3f(0,-2.8F,3.5F);
            case "stinger_stim_gun" -> new Vector3f(0,-3.44F,4.4F);
            case "manba_flashlight" -> new Vector3f(0,0,-.7F);
            case "undead_rogue_baton" -> new Vector3f(0,-5,0);
            case "saeed_ember_crossbow" -> new Vector3f(0,-.74F,1.32F);
            case "stinger_smoke_drone" -> new Vector3f(0,-4.45F,-.6F);
            case "tempest_wall_drill_stinger" -> new Vector3f(0,0,8.9F);
            case "nikaidou_hiro_ritual_sword", "nikaidou_hiro_hot_iron" -> new Vector3f(0,-.75F,0);
            case "hackclaw_knife" -> new Vector3f(0,-10.05F,0);
            case "d_wolf_hand_cannon" -> new Vector3f(-1.2F,0,0);
            case "vlinder_medical_drone" -> new Vector3f(0,-2.1F,0);
            case "gizmo_smoke_trap", "gizmo_spider_nest_trap", "gizmo_t_boy", "shepherd_sonic_trap", "sineva_blade_wire_core" -> new Vector3f(0,-2.7F,0);
            case "nox_rotor" -> new Vector3f(0,0,3.2F);
            default -> new Vector3f();
        };
    }


    private static Quaternionf upright(String id, HumanoidArm arm) {
        float turn = switch (id) {
            case "uluru_missile_launcher" -> -90 + (arm == HumanoidArm.RIGHT ? -7 : 7);
            case "n_two_condensation_focus" -> -90;
            case "vyron_tiger_cannon_launcher", "sineva_grapple_gun", "d_wolf_hand_cannon" -> -90;
            default -> 0;
        };
        return new Quaternionf().rotateY(turn * Mth.DEG_TO_RAD).rotateZ(Mth.PI);
    }

    private static PoseStack hand(PlayerModel<?> model, HumanoidArm arm) {
        PoseStack stack = new PoseStack();
        model.translateToHand(arm, stack);
        return stack;
    }

    private static Vector3f offset(HumanoidArm arm) { return new Vector3f(arm == HumanoidArm.RIGHT ? -1 : 1, 10, -2); }

    static Vector3f pivot(PlayerModel<?> model, HumanoidArm arm) {
        return hand(model, arm).last().pose().getTranslation(new Vector3f()).mul(16);
    }

    private static Vector3f palm(PlayerModel<?> model, HumanoidArm arm) {
        return hand(model, arm).last().pose().transformPosition(offset(arm).div(16)).mul(16);
    }

    private static Vector3f mouth(PlayerModel<?> model) {
        PoseStack head = new PoseStack();
        model.head.translateAndRotate(head);
        return head.last().pose().transformPosition(new Vector3f(0,-2,-4.1F).div(16)).mul(16);
    }

    private static Vector3f contact(Vector3f origin, float radius, Vector3f other, float otherRadius, Vector3f preferred) {
        Vector3f axis = new Vector3f(other).sub(origin);
        float distance = axis.length();
        if (distance < .001F) return new Vector3f(origin).add(0,0,-radius);
        axis.div(distance);
        float along = Mth.clamp((radius * radius - otherRadius * otherRadius + distance * distance) / (2 * distance), -radius, radius);
        Vector3f center = new Vector3f(origin).fma(along, axis);
        Vector3f radial = new Vector3f(preferred).sub(center);
        radial.fma(-radial.dot(axis), axis);
        if (radial.lengthSquared() < .00001F) radial.set(0,1,0).cross(axis);
        if (radial.lengthSquared() < .00001F) radial.set(1,0,0).cross(axis);
        return center.fma((float)Math.sqrt(Math.max(0,radius * radius - along * along)), radial.normalize());
    }

    private static void reach(PlayerModel<?> model, HumanoidArm arm, Vector3f target) {
        ModelPart part = arm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
        Quaternionf current = new Quaternionf().rotationZYX(part.zRot, part.yRot, part.xRot);
        Quaternionf parent = hand(model, arm).last().pose().getUnnormalizedRotation(new Quaternionf()).mul(current.invert());
        Vector3f direction = new Vector3f(target).sub(pivot(model, arm)).rotate(parent.invert());
        if (direction.lengthSquared() < .00001F) return;
        Quaternionf targetRotation = new Quaternionf().rotationTo(offset(arm).normalize(), direction.normalize()).normalize();
        float pitch = (float)Math.atan2(2 * (targetRotation.w * targetRotation.x + targetRotation.y * targetRotation.z),
                1 - 2 * (targetRotation.x * targetRotation.x + targetRotation.y * targetRotation.y));
        float yaw = (float)Math.asin(Mth.clamp(2 * (targetRotation.w * targetRotation.y - targetRotation.z * targetRotation.x), -1, 1));
        float roll = (float)Math.atan2(2 * (targetRotation.w * targetRotation.z + targetRotation.x * targetRotation.y),
                1 - 2 * (targetRotation.y * targetRotation.y + targetRotation.z * targetRotation.z));
        part.setRotation(pitch, yaw, roll);
    }

    static boolean keepContactsOnRelease(String id) {
        return paired(id) || id.equals("rover_dog_whistle") || id.equals("sineva_grapple_gun");
    }

    static void grenade(PlayerModel<?> model, HumanoidArm arm, float draw, float release) {
        float side = arm == HumanoidArm.RIGHT ? 1 : -1;
        Vector3f shoulder = pivot(model, arm);
        float pull = pinTravel(release);
        float toss = ThirdPersonSkillPose.smooth((release - .16F) / .22F);
        Vector3f ringSide = new Vector3f(side * (3.9F + pull), -2.5F, .5F + 3F * toss);
        float radius = offset(arm).length();
        Vector3f grip = contact(shoulder, radius, pivot(model, arm.getOpposite()).sub(ringSide), radius,
                new Vector3f(shoulder).add(side * 3, 6 - 7 * toss, -8 - 2 * toss));
        Vector3f outer = new Vector3f(grip).add(ringSide);
        outer.lerp(palm(model,arm.getOpposite()), 1 - draw);
        reach(model, arm, grip);
        reach(model, arm.getOpposite(), outer);
    }

    static float pinTravel(float release) {
        return 3.1F * ThirdPersonSkillPose.smooth(release / .25F);
    }

    static void drawGrenade(HeldToolThirdPersonVisuals.ToolSpec spec, float release, PoseStack stack,
                            MultiBufferSource buffers, int light) {
        BlockbenchAnimatedModelRenderer.render(spec.model(), null, 0, stack, buffers, light,
                java.util.Set.of("safety_pin", "pull_ring"), 0xFFFFFFFF);
        if (release >= .25F) return;
        stack.pushPose();
        try {
            stack.translate(pinTravel(release) / (16F * size(spec)), 0, 0);
            BlockbenchAnimatedModelRenderer.render(spec.model(), null, 0, stack, buffers, light,
                    java.util.Set.of("static_body", "safety_lever", "fuse_striker"), 0xFFFFFFFF);
        } finally {
            stack.popPose();
        }
    }

    static boolean pose(HeldToolThirdPersonVisuals.ToolSpec spec, HumanoidArm main, PlayerModel<?> model,
                        float deploy, float draw, float pulse, boolean animated) {
        String id = spec.model().getPath();
        HumanoidArm arm = carrier(id, main);
        if (id.equals("sineva_grapple_gun")) {
            ModelPart forearm = arm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
            forearm.setRotation(-.65F, 0, arm == HumanoidArm.RIGHT ? .12F : -.12F);
            return true;
        }
        if (id.equals("manba_flashlight")) {
            ThirdPersonSkillPose.aimLight(model, arm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm);
            return true;
        }
        if (id.equals("rover_dog_whistle")) {
            Vector3f lip = mouth(model);
            Vector3f goal = contact(pivot(model, arm), offset(arm).length(), lip, (8.9F - .5F) * size(spec), new Vector3f(lip).add(0,3,-5));
            goal.lerp(palm(model,arm), 1 - draw);
            reach(model, arm, goal);
            return true;
        }
        if (paired(id)) {
            Vector3f shoulder = pivot(model, arm);
            if (id.equals("n_two_condensation_focus")) {
                reach(model, arm, new Vector3f(shoulder).add(arm == HumanoidArm.RIGHT ? 1.3F : -1.3F, 8.3F + .25F*pulse, -6.4F));
                return true;
            }
            float radius = offset(arm).length();
            Vector3f grip = primary(id, arm);
            Vector3f support = switch (id) {
                case "uluru_missile_launcher" -> new Vector3f(6,-4.4F,0);
                case "luna_composite_bow" -> bowDrawHand(spec.animation(), spec.seconds());
                default -> new Vector3f(-grip.x,grip.y,grip.z);
            };
            Vector3f delta = support.sub(grip).mul(mirror(id, arm), 1, 1).mul(size(spec)).rotate(upright(id, arm));
            float lower = id.equals("morse_sonar_detector") ? 6F * Mth.clamp(deploy,0,1) : 0;
            Vector3f preferred = new Vector3f(shoulder).add(0, (id.equals("morse_sonar_detector") ? 4 : 0) + lower + (1-draw)*5 - pulse*3.6F, -12 + pulse*3);
            if (id.equals("uluru_missile_launcher")) {
                Vector3f shoulderPad = new Vector3f(-12,-1.8F,0).sub(grip).mul(size(spec)).rotate(upright(id, arm));
                preferred.set(shoulder).add(0,-1 + (1-draw)*4 - pulse*2.5F,pulse*2.8F).sub(shoulderPad);
            }
            Vector3f goal = contact(shoulder, radius, pivot(model,arm.getOpposite()).sub(delta), radius, preferred);
            reach(model, arm, goal);
            Vector3f supportGoal = new Vector3f(goal).add(delta);
            reach(model, arm.getOpposite(), supportGoal);
            return true;
        }
        if (animated) return false;
        ModelPart part = arm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
        switch (id) {
            case "d_wolf_hand_cannon", "saeed_ember_crossbow", "stinger_stim_gun", "rover_aerosol_injector" -> part.xRot = Math.min(part.xRot, -1.43F);
            case "vlinder_medical_drone", "gizmo_smoke_trap", "gizmo_spider_nest_trap", "gizmo_t_boy", "shepherd_sonic_trap", "stinger_smoke_drone", "nox_rotor", "sineva_blade_wire_core" -> part.xRot = Math.min(part.xRot, -.98F);
            default -> { return false; }
        }
        return true;
    }

    static Vector3f bowDrawHand(String clip, float seconds) {
        float draw = "release".equals(clip) ? 1 : "draw".equals(clip) ? Mth.clamp(seconds/.8F,0,1) : 0;
        float loose = "release".equals(clip) ? ThirdPersonSkillPose.smooth(seconds/.22F) : 0;
        return new Vector3f(-.8F - 2*loose, loose, 3.8F + 6.4F*draw + 1.5F*loose);
    }

    private static int mirror(String id, HumanoidArm arm) {
        if (id.equals("luna_composite_bow")) return arm == HumanoidArm.LEFT ? -1 : 1;
        return id.equals("shared_hand_grenade") && arm == HumanoidArm.RIGHT ? -1 : 1;
    }

    static boolean place(HeldToolThirdPersonVisuals.ToolSpec spec, PlayerModel<?> model, HumanoidArm main, PoseStack stack) {
        String id = spec.model().getPath();
        HumanoidArm arm = carrier(id, main);
        Vector3f grip = primary(id, arm);
        Quaternionf rotation = upright(id, arm);
        if (id.equals("wildfire_side_launcher")) {
            model.body.translateAndRotate(stack);
            stack.translate(7.1/16, 5.4/16, -4.1/16);
            stack.mulPose(rotation);
        } else if (id.equals("rover_dog_whistle")) {
            Vector3f lip = mouth(model);
            Vector3f axis = new Vector3f(lip).sub(palm(model,arm));
            if (axis.lengthSquared() < .00001F) axis.set(0,-1,0);
            Vector3f hand = palm(model,arm);
            stack.translate(hand.x/16,hand.y/16,hand.z/16);
            stack.mulPose(new Quaternionf().rotationTo(new Vector3f(0,1,0),axis.normalize()));
            grip.set(0,.5F,0);
        } else {
            switch (id) {
                case "nikaidou_hiro_hot_iron", "nikaidou_hiro_ritual_sword", "undead_rogue_baton" -> {
                    model.translateToHand(arm,stack);
                    Vector3f socket = offset(arm).div(16);
                    stack.translate(socket.x,socket.y,socket.z);
                    rotation.rotationX(-62 * Mth.DEG_TO_RAD);
                }
                case "d_wolf_hand_cannon", "saeed_ember_crossbow", "stinger_stim_gun", "rover_aerosol_injector", "manba_flashlight" -> {
                    model.translateToHand(arm,stack);
                    stack.translate((arm == HumanoidArm.RIGHT ? -1 : 1)/16.0, (id.equals("d_wolf_hand_cannon") ? 6.3 : 10)/16, -2/16.0);
                    stack.mulPose(Axis.XP.rotationDegrees(90));
                }
                case "uluru_missile_launcher", "n_two_condensation_focus", "morse_sonar_detector", "luna_composite_bow", "hackclaw_hacking_terminal",
                        "sineva_grapple_gun", "shared_hand_grenade", "hackclaw_knife", "d_wolf_smoke_grenade", "vyron_tiger_cannon_launcher",
                        "tempest_wall_drill_stinger", "stinger_smoke_drone", "nox_rotor", "vlinder_medical_drone", "shepherd_sonic_trap",
                        "gizmo_t_boy", "gizmo_spider_nest_trap", "gizmo_smoke_trap", "sineva_blade_wire_core" -> {
                    Vector3f at = palm(model,arm);
                    if (id.equals("tempest_wall_drill_stinger")) rotation.rotationX(Mth.HALF_PI);
                    if (id.equals("vlinder_medical_drone") || id.startsWith("gizmo_") || id.equals("shepherd_sonic_trap")) at.y -= 2.7F;
                    stack.translate(at.x/16, at.y/16, at.z/16);
                }
                default -> { return false; }
            }
            stack.mulPose(rotation);
        }
        float scale = size(spec);
        stack.scale(scale * mirror(id, arm),scale, id.equals("n_two_condensation_focus") && arm == HumanoidArm.LEFT ? -scale : scale);
        stack.translate(-grip.x/16, -grip.y/16, -grip.z/16);
        return true;
    }
}
