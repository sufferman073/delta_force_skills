package com.rzy.dealt_force_skills.client.visual;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.rzy.dealt_force_skills.client.renderer.BlockbenchAnimatedModelRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.Map;
import java.util.Set;

final class ConsumableUseRig {
    private static final Set<String> UNHELD = Set.of("linen_roll", "compress", "coaster", "ratchet", "seal_tab");

    private static final Set<String> WRAPPED = Set.of("linen_end");
    private static final Set<String> NO_COASTER = Set.of("coaster");
    private static final Set<String> COASTER_ONLY = Set.of("held_item");

    private static ResourceLocation id(ConsumableMotion action, ItemStack stack) {
        if (action == ConsumableMotion.OE2) return OE2CombatStimulantVisuals.MODEL;
        ResourceLocation item = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return item.withPath("consumable/" + action.rig(item.getPath()));
    }

    static boolean available(ConsumableMotion action, ItemStack stack) {
        return BlockbenchAnimatedModelRenderer.animationLength(id(action, stack), "use") > 0;
    }

    interface Arms { void place(HumanoidArm arm, Matrix4f frame, float along); }

    static boolean firstPerson(ConsumableMotion action, ItemStack stack, LocalPlayer player, HumanoidArm arm,
                               float fraction, float ticks, boolean assist, float equip, PoseStack pose,
                               MultiBufferSource buffers, int light) {
        boolean workView = action == ConsumableMotion.BOTTLE || action == ConsumableMotion.OE2
                || action == ConsumableMotion.WRAP || action == ConsumableMotion.REPAIR || action == ConsumableMotion.FIRST_AID;
        pose.pushPose();
        if (workView) {
            Vector3f pivot = ConsumableStage.shoulder(1);
            pose.translate(0, pivot.y, pivot.z);
            pose.scale(1, 1, 1.62F);
            pose.mulPose(Axis.XP.rotationDegrees(action == ConsumableMotion.BOTTLE ? -6 : 9));
            pose.translate(0, -pivot.y, -pivot.z);
        }
        float handWidth = workView ? ConsumableContact.armWidth(player, arm) : 0;
        boolean drawn = render(action, stack, player, arm, fraction, ticks, assist, equip, pose, buffers, light,
                new Vector3f(-.024F, -.079F, -.145F), new Vector3f(.035F, -.282F, -.135F),
                (which, frame, along) -> {
                    if (workView && which == arm) frame = grasp(which, frame.getTranslation(new Vector3f()), along, handWidth);
                    ConsumableContact.arm(player, which, frame, along, pose, buffers, light);
                }, workView);
        pose.popPose();
        return drawn;
    }

    private static Matrix4f grasp(HumanoidArm arm, Vector3f surface, float along, float width) {
        int side = arm == HumanoidArm.RIGHT ? 1 : -1;
        Vector3f center = new Vector3f(surface), offset = new Vector3f();
        for (int index = 0; index < 8; index++) {
            ConsumableStage.arm(side, center, along).transformDirection(-side * width * .92F, ConsumableContact.HALF_DEPTH, 0, offset);
            center.set(surface).sub(offset);
        }
        return ConsumableStage.arm(side, center, along);
    }

    static boolean scene(ConsumableMotion action, ItemStack stack, Player player, HumanoidArm arm,
                         float fraction, float ticks, boolean assist, float equip, PoseStack pose,
                         MultiBufferSource buffers, int light, Vector3f mouth, Vector3f chest, Arms arms) {
        if (action == ConsumableMotion.BLISTER) return ConsumableHandVisuals.blister(arm, true, 3*fraction, 0, 0,
                assist, true, true, mouth, pose, buffers, light, arms);
        return render(action, stack, player, arm, fraction, ticks, assist, equip, pose, buffers, light, mouth, chest, arms, false);
    }

    private static boolean render(ConsumableMotion action, ItemStack stack, Player player, HumanoidArm arm,
                                  float fraction, float ticks, boolean assist, float equip, PoseStack pose,
                                  MultiBufferSource buffers, int light, Vector3f mouth, Vector3f chest, Arms arms, boolean workView) {
        if (!available(action, stack)) return false;
        int side = arm == HumanoidArm.RIGHT ? 1 : -1;
        Matrix4f toView = new Matrix4f(pose.last().pose()).invert();
        Vector3f grip = new Vector3f(Float.NaN), tool = new Vector3f(Float.NaN);
        String item = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        Matrix4f view = new Matrix4f(pose.last().pose());
        float width = ConsumableContact.armWidth(player, arm.getOpposite());
        if (action == ConsumableMotion.BOTTLE && assist) {
            Matrix4f bottle = ConsumableStage.bottle(item, fraction, equip);
            if (workView) bottle.rotateY((float)Math.PI);
            Vector3f palm = ConsumableStage.receive(fraction, mouth);
            Vector3f lip = bottle.transformPosition(ConsumableStage.bottleLip(item));
            Vector3f pill = ConsumableStage.tablet(lip, palm, fraction);
            Vector3f cap = bottle.transformPosition(0, .31F, 0, new Vector3f());
            float twist = ConsumableMotion.ramp(fraction, .015F, .13F)*(1-ConsumableMotion.ramp(fraction, .28F, .44F));
            palm.lerp(cap, twist);
            pose.pushPose();
            ConsumableContact.apply(pose, new Matrix4f().scaling(side, 1, 1).mul(bottle));
            boolean drawn = BlockbenchAnimatedModelRenderer.renderWithGroupPlacement(id(action, stack), "use", fraction,
                    pose, buffers, light, "tablet", () -> pose.last().pose().setTranslation(view.transformPosition(
                            new Vector3f(pill).mul(side, 1, 1))));
            pose.popPose();
            if (drawn && !player.isInvisible()) {
                Vector3f hold = workView ? new Vector3f(0, (item.equals("dve_painkiller") ? -6.57F : -6.91F)/16, 0)
                        : new Vector3f(2F/16, -2.3F/16, 1.5F/16);
                bottle.transformPosition(hold).mul(side, 1, 1);
                arms.place(arm, ConsumableStage.arm(side, hold, .94F), .94F);
                arms.place(arm.getOpposite(), ConsumableStage.catchArm(-side, palm.mul(side, 1, 1)), .93F);
            }
            return drawn;
        }
        if (action == ConsumableMotion.REPAIR) {
            float phase = action.clipTime(fraction, ticks);
            Matrix4f bag = ConsumableStage.caseFrame();
            pose.pushPose();
            pose.scale(side, 1, 1);
            pose.pushPose();
            ConsumableContact.apply(pose, bag);
            boolean drawn = draw(action, stack, player, pose, buffers, light, fraction, ticks, Set.of("plate"), IGNORE, IGNORE);
            pose.popPose();
            Vector3f top = ConsumableStage.plateTop(phase, chest);
            float visible = ConsumableStage.plateExposure(phase, top, chest);
            if (visible > .001F) {
                pose.pushPose();
                pose.translate(top.x, top.y, top.z);
                pose.scale(.48F, .48F*visible, .48F);
                BlockbenchAnimatedModelRenderer.render(id(action, stack), null, 0, pose, buffers, light, Set.of("held_item"));
                pose.popPose();
            }
            pose.popPose();
            if (drawn && !player.isInvisible()) {
                Vector3f handle = new Vector3f(top).add(.032F, -.008F, -.012F);
                handle.lerp(bag.transformPosition(.05F, .11F, 0, new Vector3f()), ConsumableMotion.ramp(phase, .90F, 1));
                arms.place(arm, ConsumableStage.arm(side, handle.mul(side, 1, 1), .98F), .98F);
                if (assist) arms.place(arm.getOpposite(), ConsumableStage.arm(-side,
                        bag.transformPosition(-.24F, -.05F, .09F, new Vector3f()).mul(side, 1, 1), .95F), .95F);
            }
            return drawn;
        }
        pose.pushPose();
        boolean braced = assist && ConsumableContact.onForearm(action);
        boolean dressing = braced && (action == ConsumableMotion.SURGERY || action == ConsumableMotion.FIRST_AID);
        Matrix4f base = ConsumableContact.base(action, item, fraction, ticks, equip, braced);
        if (action == ConsumableMotion.SUPPLY) base.rotateY((float)Math.PI);
        if (braced && action == ConsumableMotion.WRAP) base.setTranslation(ConsumableStage.orbit(fraction, width));
        ConsumableContact.apply(pose, new Matrix4f().scaling(side, 1, 1).mul(base));
        Set<String> hidden = !assist || dressing ? UNHELD : action == ConsumableMotion.WRAP ? WRAPPED
                : action == ConsumableMotion.DRINK ? NO_COASTER : Set.of();
        boolean drawn = draw(action, stack, player, pose, buffers, light, fraction, ticks, hidden,
                () -> toView.transformPosition(pose.last().pose().getTranslation(grip)),
                () -> toView.transformPosition(pose.last().pose().getTranslation(tool)));
        pose.popPose();
        if (!drawn || player.isInvisible()) return drawn;
        if (assist && action == ConsumableMotion.DRINK) {
            pose.pushPose();
            pose.translate(side*(.11F-.33F*ConsumableContact.hold(fraction)), -.14F, -.63F);
            pose.scale(side*.43F, .43F, .43F);
            BlockbenchAnimatedModelRenderer.renderWithGroupReplacements(id(action, stack), "use", 0, pose, buffers, light,
                    COASTER_ONLY, Map.of("contact", () -> toView.transformPosition(pose.last().pose().getTranslation(tool))));
            pose.popPose();
        }
        if (braced) {
            HumanoidArm support = arm.getOpposite();
            arms.place(support, ConsumableContact.forearm(side, fraction), .77F);
            if (action == ConsumableMotion.FIRST_AID) {
                pose.pushPose();
                pose.scale(side, 1, 1);
                ConsumableCarKit.atForearm(item, base, fraction, ConsumableContact.armWidth(player, support), side, pose, buffers, light, grip);
                pose.popPose();
            } else if (action == ConsumableMotion.WRAP || dressing)
                ConsumableDressing.draw(action, side, fraction, base, ConsumableContact.armWidth(player, support),
                        pose, buffers, light, grip);
            if (action == ConsumableMotion.TOURNIQUET && tool.isFinite()) grip.lerp(tool, ConsumableContact.hold(fraction));
        }
        if (grip.isFinite()) {
            Matrix4f hand = !workView && braced && action == ConsumableMotion.FIRST_AID
                    ? grasp(arm, grip, .96F, ConsumableContact.armWidth(player, arm)) : ConsumableStage.arm(side, grip, .96F);
            arms.place(arm, hand, .96F);
        }
        if (assist && !braced && tool.isFinite()) {
            float reach = action == ConsumableMotion.REPAIR ? ConsumableMotion.ramp(ticks, 0, 5)
                    : ConsumableMotion.ramp(fraction, 0, .18F) * (1 - ConsumableMotion.ramp(fraction, .84F, 1));
            Vector3f receiving = new Vector3f(-side*.38F, -.48F, -.42F).lerp(tool, reach);
            arms.place(arm.getOpposite(), ConsumableStage.arm(-side, receiving, .94F), .94F);
        }
        return true;
    }

    static boolean thirdPerson(ConsumableMotion action, ItemStack stack, Player player, PlayerModel<?> model,
                               HumanoidArm arm, float fraction, float ticks, PoseStack pose, MultiBufferSource buffers, int light) {
        if (!available(action, stack)) return false;
        int side = arm == HumanoidArm.RIGHT ? 1 : -1;
        boolean assist = (arm == player.getMainArm() ? player.getOffhandItem() : player.getMainHandItem()).isEmpty();
        boolean transfusion = assist && action == ConsumableMotion.FIRST_AID;
        Vector3f skin = transfusion ? ConsumableHandVisuals.supportSurface(model, arm.getOpposite(), pose, false) : null;
        pose.pushPose();
        model.translateToHand(arm, pose);
        pose.mulPose(Axis.XP.rotationDegrees(-90));
        pose.mulPose(Axis.YP.rotationDegrees(180));
        pose.translate(side/16F, .125F, -.625F);
        pose.scale(side*.46F, .46F, .46F);
        ConsumableMotion.Pose move = action.sample(fraction, ticks);
        pose.mulPose(Axis.XP.rotationDegrees(48 + move.pitch()*.55F));
        pose.mulPose(Axis.ZP.rotationDegrees(move.roll()*.55F));
        pose.translate(-2F/16, 2F/16, -1F/16);
        boolean drawn = draw(action, stack, player, pose, buffers, light, fraction, ticks,
                assist && !transfusion ? Set.of() : UNHELD, IGNORE, IGNORE);
        if (drawn && transfusion) {
            new Matrix4f(pose.last().pose()).invert().transformPosition(skin);
            ConsumableCarKit.thirdPerson(BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath(), fraction, skin, pose, buffers, light);
        }
        pose.popPose();
        return drawn;
    }

    private static final Runnable IGNORE = () -> {};

    private static boolean draw(ConsumableMotion action, ItemStack stack, Player player, PoseStack pose,
                                MultiBufferSource buffers, int light, float fraction, float ticks, Set<String> hidden,
                                Runnable grip, Runnable tool) {
        Runnable item = () -> {
            if (action != ConsumableMotion.OE2) Minecraft.getInstance().getEntityRenderDispatcher().getItemInHandRenderer().renderItem(player, stack,
                    action == ConsumableMotion.REPAIR ? ItemDisplayContext.FIXED : ItemDisplayContext.NONE,
                    false, pose, buffers, light);
        };
        return BlockbenchAnimatedModelRenderer.renderWithGroupReplacements(id(action, stack), "use",
                action.clipTime(fraction, ticks), pose, buffers, light, hidden,
                Map.of("grip", grip, "contact", tool, "held_item", item));
    }

    private ConsumableUseRig() {}
}
