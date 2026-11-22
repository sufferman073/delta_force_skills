package com.rzy.dealt_force_skills.client.visual;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.client.ClientRenderCompat;
import com.rzy.dealt_force_skills.client.renderer.BlockbenchAnimatedModelRenderer;
import com.rzy.dealt_force_skills.item.DfsUseItem;
import com.rzy.dealt_force_skills.item.RepairKitItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;


@EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT)
public final class ConsumableHandVisuals {
    private static final ResourceLocation BLISTER = ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "sustained_release_painkiller");
    private static final float CLIP_SECONDS = 3;

    private ConsumableHandVisuals() {}

    private static ConsumableMotion motion(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof DfsUseItem || stack.getItem() instanceof RepairKitItem)) return null;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return DealtForceSkillsMod.MODID.equals(id.getNamespace()) ? ConsumableMotion.forPath(id.getPath()) : null;
    }

    private static boolean using(Player player, InteractionHand hand) {
        return player.isUsingItem() && player.getUsedItemHand() == hand && !player.getUseItem().isEmpty()
                && player.getItemInHand(hand).is(player.getUseItem().getItem());
    }

    public static boolean replacesUsePose(Player player, InteractionHand hand) {
        return using(player, hand) && motion(player.getUseItem()) != null;
    }

    private static float progress(Player player, float partial) {
        return ConsumableMotion.progress(player.getUseItem().getUseDuration(player), player.getUseItemRemainingTicks(), partial);
    }

    private static InteractionHand other(InteractionHand hand) {
        return hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
    }

    private static HumanoidArm arm(Player player, InteractionHand hand) {
        return hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void renderHand(RenderHandEvent event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || player.isSpectator() || player.isScoping()) return;
        InteractionHand hand = event.getHand();
        ItemStack displayed = event.getItemStack();
        if (displayed.isEmpty()) {
            if (hand == InteractionHand.MAIN_HAND && player.getMainHandItem().isEmpty() && using(player, InteractionHand.OFF_HAND)) {
                ConsumableMotion assisting = motion(player.getUseItem());
                if (assisting == ConsumableMotion.BLISTER ? BlockbenchAnimatedModelRenderer.animationLength(BLISTER, "use") > 0
                        : assisting != null && ConsumableUseRig.available(assisting, player.getUseItem())) event.setCanceled(true);
            }
            return;
        }
        if (!player.getItemInHand(hand).is(displayed.getItem())) return;
        ConsumableMotion action = motion(displayed);
        if (action == null) return;
        boolean active = using(player, hand);
        float partial = event.getPartialTick();
        HumanoidArm holder = arm(player, hand);
        if (action == ConsumableMotion.BLISTER) {
            if (firstPersonBlister(player, holder, event, active, active ? CLIP_SECONDS * progress(player, partial) : 0))
                event.setCanceled(true);
        } else if (active || action == ConsumableMotion.OE2) {
            float fraction = active ? progress(player, partial) : 0;
            float ticks = active ? player.getTicksUsingItem() + partial : 0;
            boolean assist = active && player.getItemInHand(other(hand)).isEmpty();
            if (ConsumableUseRig.firstPerson(action, displayed, player, holder, fraction, ticks, assist,
                    event.getEquipProgress(), event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight()))
                event.setCanceled(true);
        }
    }

    private static boolean firstPersonBlister(LocalPlayer player, HumanoidArm holder, RenderHandEvent event,
                                              boolean active, float seconds) {
        boolean assist = active && player.getItemInHand(other(event.getHand())).isEmpty();
        PoseStack pose = event.getPoseStack();
        return blister(holder, active, seconds, event.getEquipProgress(), event.getSwingProgress(), assist, !player.isInvisible(), false,
                new Vector3f(-.025F, -.065F, -.255F), pose, event.getMultiBufferSource(), event.getPackedLight(),
                (arm, frame, along) -> ConsumableContact.arm(player, arm, frame, along, pose, event.getMultiBufferSource(), event.getPackedLight()));
    }

    static boolean blister(HumanoidArm holder, boolean active, float seconds, float equip, float swingProgress,
                           boolean assist, boolean drawArms, boolean bodyView, Vector3f mouth, PoseStack pose, MultiBufferSource buffers,
                           int light, ConsumableUseRig.Arms arms) {
        int side = holder == HumanoidArm.RIGHT ? 1 : -1;
        float settle = ConsumableMotion.ramp(seconds, 0, .70F) * (1 - ConsumableMotion.ramp(seconds, 2.29F, 3));
        float carry = ConsumableMotion.carry(seconds);
        float swing = active ? 0 : Mth.sin(Mth.sqrt(swingProgress) * Mth.PI);
        float gripX = .27F - .13F * settle - .055F * carry - .055F * swing;
        float gripY = -.37F + .04F * settle + .035F * carry - .48F * equip;
        float gripZ = -.64F + .085F * carry - .05F * swing;
        float release = ConsumableMotion.ramp(seconds, 1.02F, 1.45F);
        if (bodyView) {
            gripX = .205F + .095F * release;
            gripY = -.435F - .028F * release;
            gripZ = -.39F + .07F * release;
        }
        Matrix4f view = new Matrix4f(pose.last().pose());
        Vector3f bite = view.transformPosition(mouth.mul(side, 1, 1, new Vector3f()));
        Matrix4f toView = new Matrix4f(view).invert();
        Vector3f doseCenter = new Vector3f(Float.NaN);
        Matrix4f tabletFrame = bodyView && assist ? new Matrix4f() : null;
        pose.pushPose();
        pose.scale(side, 1, 1);
        pose.translate(gripX, gripY, gripZ);
        pose.mulPose(Axis.YP.rotationDegrees(bodyView ? 110 - 37 * release : 158));
        pose.mulPose(Axis.ZP.rotationDegrees(8));
        float size = bodyView ? .44F : .50F;
        pose.scale(size, size, size);
        pose.translate(0, -1.5F / 16, 0);
        boolean drawn = active ? BlockbenchAnimatedModelRenderer.renderWithGroupPlacement(BLISTER, "use", seconds,
                pose, buffers, light, "dose", () -> {
                    if (bodyView) {
                        bodyDose(pose, view, toView, side, mouth, seconds, release, doseCenter);
                        if (tabletFrame != null) toView.mul(pose.last().pose(), tabletFrame);
                    } else toView.transformPosition(carryDose(pose, bite, seconds), doseCenter);
                })
                : BlockbenchAnimatedModelRenderer.render(BLISTER, null, 0, pose, buffers, light);
        pose.popPose();
        if (!drawn || !drawArms) return drawn;
        Vector3f holding = bodyView ? new Vector3f(side*(gripX+.027F), gripY-.04F, gripZ+.012F)
                : new Vector3f(side*(gripX+.095F), gripY-.035F, gripZ-.012F);
        arms.place(holder, ConsumableStage.arm(side, holding, .93F), .93F);
        if (assist && doseCenter.isFinite()) {
            float reach = ConsumableMotion.ramp(seconds, .12F, .82F) * (1 - ConsumableMotion.ramp(seconds, 2.29F, 3));
            Vector3f rest = new Vector3f(-side*.43F, -.54F, -.42F);
            Vector3f palm = dosePalm(-side, doseCenter, tabletFrame);
            arms.place(holder.getOpposite(), ConsumableStage.arm(-side, rest.lerp(palm, reach), .93F), .93F);
        }
        return drawn;
    }

    private static void bodyDose(PoseStack pose, Matrix4f view, Matrix4f toView, int side, Vector3f mouth,
                                 float seconds, float release, Vector3f center) {
        Matrix4f dose = pose.last().pose();
        Vector3f localCenter = new Vector3f(0, 0, -.65F/16);
        toView.transformPosition(dose.transformPosition(localCenter, new Vector3f()), center);
        if (release <= 0) return;
        float lift = ConsumableMotion.ramp(seconds, 1.45F, 1.97F);
        float approach = ConsumableMotion.ramp(seconds, 1.94F, 2.17F);
        float swallow = ConsumableMotion.ramp(seconds, 2.17F, 2.29F);
        Vector3f destination = new Vector3f(-.155F, -.29F, -.44F)
                .lerp(new Vector3f(mouth).add(-.125F, -.06F, -.105F), lift)
                .lerp(new Vector3f(mouth).add(-.044F, -.018F, -.047F), approach)
                .lerp(mouth, swallow).mul(side, 1, 1);
        center.lerp(destination, release);
        Quaternionf orientation = new Matrix4f().scaling(side, 1, 1).mul(toView).mul(dose)
                .getUnnormalizedRotation(new Quaternionf());
        Quaternionf turn = orientation.conjugate().mul(new Quaternionf().rotationXYZ(.16F, .12F, .52F));
        pose.mulPose(new Quaternionf().slerp(turn, release));
        float expose = 1 + .40F * release;
        pose.scale(expose, expose, expose);
        Vector3f correction = view.transformPosition(center, new Vector3f()).sub(dose.transformPosition(localCenter));
        dose.setTranslation(dose.getTranslation(new Vector3f()).add(correction));
    }

    private static Vector3f dosePalm(int side, Vector3f center, Matrix4f tabletFrame) {
        Vector3f palm = new Vector3f(center), offset = new Vector3f();
        for (int pass = 0; pass < 8; pass++) {
            Matrix4f hand = ConsumableStage.arm(side, palm, .93F);
            float support = .046F;
            if (tabletFrame != null) {
                Vector3f up = hand.transformDirection(0, 1, 0, offset);
                float axisX = up.x*tabletFrame.m00()+up.y*tabletFrame.m01()+up.z*tabletFrame.m02();
                float axisY = up.x*tabletFrame.m10()+up.y*tabletFrame.m11()+up.z*tabletFrame.m12();
                float axisZ = up.x*tabletFrame.m20()+up.y*tabletFrame.m21()+up.z*tabletFrame.m22();
                float spanX = Math.abs(.731354F*axisX - .681998F*axisY);
                float spanY = Math.abs(.681998F*axisX + .731354F*axisY);
                float waist = 1.14F*spanX + .41F*spanY + .24F*Math.abs(axisZ) - .06F*axisZ;
                float crown = .88F*spanX + .53F*spanY + .28F*Math.abs(axisZ) + .02F*axisZ;
                support = (ConsumableContact.ARM_SCALE*.25F + Math.max(waist, crown))/16;
            }
            hand.transformDirection(-side*.040F, ConsumableContact.HALF_DEPTH+support, 0, offset);
            palm.set(center).sub(offset);
        }
        return palm;
    }

    private static Vector3f carryDose(PoseStack pose, Vector3f mouth, float seconds) {
        Matrix4f dose = pose.last().pose();
        Vector3f center = dose.transformPosition(0, 0, -.65F/16, new Vector3f());
        Vector3f shift = new Vector3f(mouth).sub(center).mul(ConsumableMotion.ramp(seconds, 1.45F, 2.17F));
        dose.setTranslation(dose.getTranslation(new Vector3f()).add(shift));
        return center.add(shift);
    }

    static Vector3f supportSurface(PlayerModel<?> model, HumanoidArm arm, PoseStack pose, boolean tablet) {
        boolean right = arm == HumanoidArm.RIGHT;
        ModelPart limb = right ? model.rightArm : model.leftArm;
        Vector3f target = new Vector3f();
        pose.pushPose();
        limb.translateAndRotate(pose);
        limb.visit(new PoseStack(), (ignored, path, index, cube) -> {
            if (path.isEmpty() && index == 0) pose.last().pose().transformPosition(
                    ((right ? cube.maxX : cube.minX) + (right ? 1 : -1)*(tablet ? .55F : .20F))/16,
                    (cube.minY + (tablet ? .92F : .65F)*(cube.maxY-cube.minY))/16,
                    (tablet ? cube.minZ-.45F : (cube.minZ+cube.maxZ)*.5F)/16, target);
        });
        pose.popPose();
        return target;
    }

    public static boolean thirdPersonItem(Player player, PlayerModel<?> model, ItemStack displayed, HumanoidArm holder,
                                          PoseStack pose, MultiBufferSource buffer, int light) {
        if (ConsumableBody.owns(model, player, holder)) return true;
        ConsumableMotion action = motion(displayed);
        if (action == null) return false;
        InteractionHand hand = player.getMainArm() == holder ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        if (!player.getItemInHand(hand).is(displayed.getItem())) return false;
        boolean active = using(player, hand);
        if (action != ConsumableMotion.BLISTER) {
            if (!active && action != ConsumableMotion.OE2) return false;
            float partial = ClientRenderCompat.frameTime(Minecraft.getInstance());
            return ConsumableUseRig.thirdPerson(action, displayed, player, model, holder, active ? progress(player, partial) : 0,
                    active ? player.getTicksUsingItem() + partial : 0, pose, buffer, light);
        }
        float seconds = active ? CLIP_SECONDS * progress(player, ClientRenderCompat.frameTime(Minecraft.getInstance())) : 0;
        Vector3f mouth = new Vector3f();
        Vector3f receiver = active && player.getItemInHand(other(hand)).isEmpty()
                ? supportSurface(model, holder.getOpposite(), pose, true) : null;
        if (active) {
            pose.pushPose();
            model.head.translateAndRotate(pose);
            pose.last().pose().transformPosition(0, -.12F, -.28F, mouth);
            pose.popPose();
        }
        pose.pushPose();
        model.translateToHand(holder, pose);
        pose.mulPose(Axis.XP.rotationDegrees(-90));
        pose.mulPose(Axis.YP.rotationDegrees(180));
        pose.translate((holder == HumanoidArm.RIGHT ? 1 : -1) / 16F, .125F, -.625F);
        pose.mulPose(Axis.XP.rotationDegrees(active ? 33 : 78));
        pose.mulPose(Axis.YP.rotationDegrees((holder == HumanoidArm.RIGHT ? 1 : -1) * (active ? 66 : 18)));
        pose.scale(.48F, .48F, .48F);
        pose.translate(0, -1.5F / 16, 0);
        boolean drawn = active ? BlockbenchAnimatedModelRenderer.renderWithGroupPlacement(BLISTER, "use", seconds,
                pose, buffer, light, "dose", () -> {
                    if (receiver == null) carryDose(pose, mouth, seconds);
                    else {
                        Vector3f target = new Vector3f(receiver).lerp(mouth, ConsumableMotion.ramp(seconds, 2.04F, 2.17F));
                        Matrix4f dose = pose.last().pose();
                        Vector3f center = dose.transformPosition(0, 0, -.65F/16, new Vector3f());
                        dose.setTranslation(dose.getTranslation(new Vector3f()).add(target.sub(center)
                                .mul(ConsumableMotion.ramp(seconds, 1.06F, 1.45F))));
                    }
                })
                : BlockbenchAnimatedModelRenderer.render(BLISTER, null, 0, pose, buffer, light);
        pose.popPose();
        return drawn;
    }

    public static void endThirdPerson(Player player, PlayerModel<?> model) { ConsumableBody.finish(model, player); }

    public static void thirdPersonArms(Player player, PlayerModel<?> model, float partial, PoseStack pose, MultiBufferSource buffer, int light) {
        if (HeldToolThirdPersonVisuals.replacesHand(player, HumanoidArm.LEFT)
                || HeldToolThirdPersonVisuals.replacesHand(player, HumanoidArm.RIGHT)) return;
        if (!player.isUsingItem()) return;
        InteractionHand hand = player.getUsedItemHand();
        ConsumableMotion action = motion(player.getUseItem());
        if (action == null || !using(player, hand)) return;
        HumanoidArm holder = arm(player, hand);
        float fraction = progress(player, partial);
        if (ConsumableBody.draw(model, player, action, player.getUseItem(), holder, fraction,
                player.getTicksUsingItem()+partial, player.getItemInHand(other(hand)).isEmpty(), pose, buffer, light)) return;
        if (action == ConsumableMotion.BLISTER) {
            float seconds = CLIP_SECONDS * fraction;
            float carry = ConsumableMotion.carry(seconds);
            poseArm(model, holder, 47 - 5 * carry, -13, 1);
            if (player.getItemInHand(other(hand)).isEmpty()) {
                float reach = ConsumableMotion.ramp(seconds, 0, .82F) * (1 - ConsumableMotion.ramp(seconds, 2.29F, 3));
                poseArm(model, holder.getOpposite(), 79 + 57 * carry, 35 - 5 * carry, reach);
            }
        } else {
            ConsumableMotion.Pose move = action.sample(fraction, player.getTicksUsingItem() + partial);
            poseArm(model, holder, 24 + move.armPitch(), move.armInward(), 1);
            if (player.getItemInHand(other(hand)).isEmpty()) {
                float reach = action == ConsumableMotion.REPAIR ? ConsumableMotion.ramp(player.getTicksUsingItem()+partial, 0, 6)
                        : ConsumableMotion.ramp(fraction, .03F, .21F) * (1 - ConsumableMotion.ramp(fraction, .81F, 1));
                poseArm(model, holder.getOpposite(), 48 + .42F*move.armPitch(), 38 - .16F*move.armInward(), reach);
            }
        }
    }

    private static void poseArm(PlayerModel<?> model, HumanoidArm arm, float pitch, float inward, float weight) {
        boolean right = arm == HumanoidArm.RIGHT;
        ModelPart part = right ? model.rightArm : model.leftArm;
        part.xRot = Mth.lerp(weight, part.xRot, -pitch * Mth.DEG_TO_RAD);
        part.yRot = Mth.lerp(weight, part.yRot, (right ? -1 : 1) * inward * Mth.DEG_TO_RAD);
        (right ? model.rightSleeve : model.leftSleeve).copyFrom(part);
    }
}
