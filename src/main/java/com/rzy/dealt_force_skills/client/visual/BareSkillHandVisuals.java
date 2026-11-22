package com.rzy.dealt_force_skills.client.visual;

import com.mojang.math.Axis;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.sineva.SinevaKnockdownState;
import com.rzy.dealt_force_skills.client.RaptorFalconController;
import com.rzy.dealt_force_skills.client.UluruMissileController;
import com.rzy.dealt_force_skills.client.character.ClientHeldToolVisualState;
import com.rzy.dealt_force_skills.skill.BareSkillPulse;
import com.rzy.dealt_force_skills.skill.HeldToolVisual;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

import java.util.Map;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT)
public final class BareSkillHandVisuals {
    private record Active(BareSkillPulse pulse, InteractionHand hand, int start) {}
    private static final Map<Player, Active> ACTIVE = new WeakHashMap<>();

    private BareSkillHandVisuals() {}

    public static void receive(int entityId, int visualId, int duration) {
        var level = Minecraft.getInstance().level;
        BareSkillPulse pulse = BareSkillPulse.byId(visualId);
        if (pulse == null || duration != pulse.ticks || level == null
                || !(level.getEntity(entityId) instanceof Player player)) return;
        InteractionHand hand = player.getOffhandItem().isEmpty() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        if (player.getItemInHand(hand).isEmpty() && !occupied(player, 0))
            ACTIVE.put(player, new Active(pulse, hand, player.tickCount));
        else ACTIVE.remove(player);
    }

    private static boolean occupied(Player player, float partial) {
        return !player.isAlive() || player.isInvisible() || player.isSpectator() || player.isSleeping()
                || player.isUsingItem() || SinevaKnockdownState.active(player)
                || player == Minecraft.getInstance().player && (RaptorFalconController.isControlling() || UluruMissileController.isControlling())
                || ClientHeldToolVisualState.visual(player) != HeldToolVisual.NONE
                || CharacterSkillModelVisuals.replacesHand(player) || ClientToolHandMotion.frame(player, partial) != null;
    }

    private static Active active(Player player, float partial) {
        Active active = ACTIVE.get(player);
        if (active != null && (player.tickCount - active.start >= active.pulse.ticks
                || !player.getItemInHand(active.hand).isEmpty() || occupied(player, partial))) {
            ACTIVE.remove(player);
            return null;
        }
        return active;
    }

    private static HumanoidArm arm(Player player, Active active) {
        return active.hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
    }

    private static BareSkillStroke stroke(Player player, Active active, float partial) {
        return BareSkillStroke.sample(active.pulse, (player.tickCount - active.start + partial) / active.pulse.ticks);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onHand(RenderHandEvent event) {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (player == null || minecraft.getCameraEntity() != player || !minecraft.options.getCameraType().isFirstPerson()) return;
        Active active = active(player, event.getPartialTick());
        if (active == null || event.getHand() != active.hand || !event.getItemStack().isEmpty()) return;
        BareSkillStroke stroke = stroke(player, active, event.getPartialTick());
        boolean right = arm(player, active) == HumanoidArm.RIGHT;
        int side = right ? 1 : -1;
        var poses = event.getPoseStack();
        event.setCanceled(true);
        poses.pushPose();
        try {
            poses.translate(side * (.41F - .27F * stroke.inward()), -.78F + .54F * stroke.lift(), -.40F - .39F * stroke.reach());
            poses.mulPose(Axis.ZP.rotationDegrees(side * stroke.roll()));
            ClientToolFirstPerson.hand(event, player, right, new Vector3f(), 48 + 38 * stroke.reach());
        } finally {
            poses.popPose();
        }
    }

    public static void poseArms(Player player, PlayerModel<?> model, float partial) {
        var minecraft = Minecraft.getInstance();
        if (player == minecraft.getCameraEntity() && minecraft.options.getCameraType().isFirstPerson()) return;
        Active active = active(player, partial);
        if (active == null || HeldToolThirdPersonVisuals.replacesHand(player, HumanoidArm.LEFT)
                || HeldToolThirdPersonVisuals.replacesHand(player, HumanoidArm.RIGHT)) return;
        BareSkillStroke stroke = stroke(player, active, partial);
        boolean right = arm(player, active) == HumanoidArm.RIGHT;
        int side = right ? 1 : -1;
        var part = right ? model.rightArm : model.leftArm;
        float weight = stroke.weight();
        part.setRotation(Mth.lerp(weight, part.xRot, -.25F - 1.1F * stroke.lift() - .65F * stroke.reach()),
                Mth.lerp(weight, part.yRot, -side * .85F * stroke.inward()),
                Mth.lerp(weight, part.zRot, side * stroke.roll() * Mth.DEG_TO_RAD));
        (right ? model.rightSleeve : model.leftSleeve).copyFrom(part);
    }
}
