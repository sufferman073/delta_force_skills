package com.rzy.dealt_force_skills.client.visual;

import com.mojang.blaze3d.vertex.PoseStack;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.client.character.ClientNoxHudState;
import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.motion.StealthTransition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Short skin afterimage; authoritative invisibility, equipment hiding and targeting remain untouched. */
@EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT)
public final class StealthFadeVisuals {
    private static final Map<UUID, StealthTransition> FADES = new HashMap<>();
    private static Object level;
    private static final Map<PlayerModel<?>, PlayerModel<AbstractClientPlayer>> MODELS = new java.util.WeakHashMap<>();
    private StealthFadeVisuals() { }

    private static boolean hidden(Player p) {
        boolean undead = p.hasEffect(ModEffects.UNDEAD_TRUE_INVISIBILITY);
        boolean revealed = ClientNoxHudState.isEntityRevealed(p.getId());
        StealthTransition previous = FADES.get(p.getUUID());
        return undead || !revealed && (p.hasEffect(ModEffects.NOX_STEALTH)
                // Marker and entity metadata may arrive in different packets.
                || previous != null && previous.hidden() && p.isInvisible());
    }

    private static void checkLevel() {
        var world = Minecraft.getInstance().level;
        if (world != level) { FADES.clear(); MODELS.clear(); level = world; }
    }

    private static StealthTransition observe(Player player, float now) {
        boolean hide = hidden(player);
        StealthTransition fade = FADES.computeIfAbsent(player.getUUID(), id -> new StealthTransition(hide, now));
        fade.observe(hide, now);
        return fade;
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        checkLevel();
        var world = Minecraft.getInstance().level;
        if (world == null) return;
        for (Player p : world.players()) {
            observe(p, p.tickCount);
        }
        FADES.keySet().removeIf(id -> world.getPlayerByUUID(id) == null);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void render(RenderPlayerEvent.Pre event) {
        if (!(event.getEntity() instanceof AbstractClientPlayer player) || !player.isAlive()) return;
        checkLevel();
        if (player.hasEffect(ModEffects.DEPARTMENT_CONCEALMENT)
                || player.hasEffect(MobEffects.INVISIBILITY)) return;
        // A replacement renderer owns its mesh. Never reveal an invisible YSM model via a vanilla duplicate.
        if (ModList.get().getMods().stream().anyMatch(m -> {
            String id = m.getModId(); return id.contains("yes_steve") || id.contains("yessteve") || id.equals("ysm");
        })) return;
        float partial = com.rzy.dealt_force_skills.client.ClientRenderCompat.partialTick(event.getPartialTick());
        float now = player.tickCount + partial;
        // Observe before deciding who renders, including packet changes between client ticks.
        StealthTransition f = observe(player, now);
        float alpha = f.alpha(now);
        if (!f.ownsRender(now)) return;
        event.setCanceled(true);
        if (alpha <= 0) return;
        boolean slim = player.getSkin().model() == net.minecraft.client.resources.PlayerSkin.Model.SLIM;
        PlayerModel<AbstractClientPlayer> model = MODELS.computeIfAbsent(event.getRenderer().getModel(), key ->
                new PlayerModel<>(Minecraft.getInstance().getEntityModels()
                        .bakeLayer(slim ? ModelLayers.PLAYER_SLIM : ModelLayers.PLAYER), slim));
        model.attackTime = player.getAttackAnim(partial);
        model.crouching = player.isCrouching();
        model.hat.visible = player.isModelPartShown(PlayerModelPart.HAT);
        model.jacket.visible = player.isModelPartShown(PlayerModelPart.JACKET);
        model.leftSleeve.visible = player.isModelPartShown(PlayerModelPart.LEFT_SLEEVE);
        model.rightSleeve.visible = player.isModelPartShown(PlayerModelPart.RIGHT_SLEEVE);
        model.leftPants.visible = player.isModelPartShown(PlayerModelPart.LEFT_PANTS_LEG);
        model.rightPants.visible = player.isModelPartShown(PlayerModelPart.RIGHT_PANTS_LEG);
        float walk = player.walkAnimation.position(partial), speed = player.walkAnimation.speed(partial);
        model.prepareMobModel(player, walk, speed, partial);
        float body = Mth.rotLerp(partial, player.yBodyRotO, player.yBodyRot);
        float head = Mth.rotLerp(partial, player.yHeadRotO, player.yHeadRot);
        model.setupAnim(player, walk, speed, player.tickCount + partial, head - body, player.getViewXRot(partial));
        PoseStack stack = event.getPoseStack();
        stack.pushPose();
        TacticalPlayerPose.modelSpace(player, stack, partial);
        model.renderToBuffer(stack, event.getMultiBufferSource().getBuffer(
                RenderType.entityTranslucent(event.getRenderer().getTextureLocation(player))),
                event.getPackedLight(), OverlayTexture.NO_OVERLAY, ((int)(alpha * 255) << 24) | 0xFFFFFF);
        stack.popPose();
    }

}
