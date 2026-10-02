package com.rzy.dealt_force_skills.client;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.character.SkillSlot;
import com.rzy.dealt_force_skills.character.rover.RoverConfig;
import com.rzy.dealt_force_skills.client.character.ClientCharacterSelectionState;
import com.rzy.dealt_force_skills.client.character.ClientCommanderState;
import com.rzy.dealt_force_skills.network.C2S_RoverAction;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.registry.ModItems;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.LevelRenderer;
import com.rzy.dealt_force_skills.client.renderer.DfsRenderTypes;
import net.minecraft.world.phys.AABB;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT)
public final class RoverClient {
    private static CompoundTag state = new CompoundTag();
    private static int smokeHeld;
    private static int coreHeld;
    private static boolean equipped;
    private static boolean recalled;
    private RoverClient() {}
    public static void sync(CompoundTag snapshot) { state = snapshot.copy(); }
    public static boolean selected() { return ClientCharacterSelectionState.isSelectedCharacter(ModCharacters.ROVER_ID); }
    public static int tool() { return selected() || ClientCommanderState.inherits(ModCharacters.ROVER_ID) ? state.getInt("Tool") : 0; }
    public static boolean ownsCoreSkill() {
        var player = Minecraft.getInstance().player;
        return selected() || coreHeld > 0 || player != null && ClientCommanderState.handlesSkill(
                ModCharacters.ROVER_ID, SkillSlot.CORE, player.isShiftKeyDown());
    }
    public static int cooldown(SkillSlot slot) {
        return switch (slot) {
            case ACTIVE_1 -> state.getInt("Smoke") > 0 ? 0 : state.getInt("SmokeCooldown");
            case ACTIVE_2 -> state.getInt("Injector") > 0 ? 0 : state.getInt("InjectorCooldown");
            case CORE -> state.getInt("CoreCooldown");
            default -> 0;
        };
    }
    public static void reset() { state = new CompoundTag(); smokeHeld = coreHeld = 0; equipped = recalled = false; }

    public static void tick(Minecraft mc) {
        if (mc.player == null) { reset(); return; }
        for (String key : new String[]{"SmokeCooldown", "InjectorCooldown", "CoreCooldown", "DogTicks", "Downed"}) {
            if (state.getInt(key) > 0) state.putInt(key, state.getInt(key) - 1);
        }
        boolean smokeOwned = selected() || ClientCommanderState.handlesSkill(ModCharacters.ROVER_ID, SkillSlot.ACTIVE_1);
        boolean injectorOwned = selected() || ClientCommanderState.handlesSkill(ModCharacters.ROVER_ID, SkillSlot.ACTIVE_2);
        boolean coreOwned = ownsCoreSkill();
        if (mc.screen != null || mc.player.hasEffect(ModEffects.ROVER_DOWNED)) {
            if (coreOwned) drain(KeybindRegister.CORE_SKILL);
            smokeHeld = coreHeld = 0; equipped = recalled = false; return;
        }
        if (smokeOwned && KeybindRegister.ACTIVE_SKILL_1.isDown()) {
            drain(KeybindRegister.ACTIVE_SKILL_1);
            if (++smokeHeld >= Math.max(1, RoverConfig.HOLD_TICKS) && !equipped) {
                equipped = true; ClientCommanderState.useNativeOrInherited(ModCharacters.ROVER_ID, SkillSlot.ACTIVE_1, true);
            }
        } else if (smokeHeld > 0) {
            if (smokeOwned && !equipped) ClientCommanderState.useNativeOrInherited(ModCharacters.ROVER_ID, SkillSlot.ACTIVE_1, false);
            smokeHeld = 0; equipped = false;
        }
        while (injectorOwned && KeybindRegister.ACTIVE_SKILL_2.consumeClick())
            ClientCommanderState.useNativeOrInherited(ModCharacters.ROVER_ID, SkillSlot.ACTIVE_2, false);
        if (coreOwned && KeybindRegister.CORE_SKILL.isDown()) {
            drain(KeybindRegister.CORE_SKILL);
            if (++coreHeld >= Math.max(1, RoverConfig.HOLD_TICKS) && !recalled) {
                recalled = true; ClientCommanderState.useNativeOrInherited(ModCharacters.ROVER_ID, SkillSlot.CORE, true);
            }
        } else if (coreHeld > 0) {
            drain(KeybindRegister.CORE_SKILL);
            if (!recalled) ClientCommanderState.useNativeOrInherited(ModCharacters.ROVER_ID, SkillSlot.CORE, false);
            coreHeld = 0; recalled = false;
        } else if (coreOwned) {
            // A quick press/release can finish between client ticks and leave only a queued click.
            while (KeybindRegister.CORE_SKILL.consumeClick())
                ClientCommanderState.useNativeOrInherited(ModCharacters.ROVER_ID, SkillSlot.CORE, false);
        }
        if (tool() > 0 && !refreshItemHasPriority(mc)) {
            drain(mc.options.keyAttack); drain(mc.options.keyUse); drain(mc.options.keyDrop); drain(mc.options.keySwapOffhand);
        }
    }

    private static void drain(KeyMapping key) { while (key.consumeClick()) {} }

    private static boolean refreshItemHasPriority(Minecraft mc) {
        return mc.player != null
                && (mc.player.getMainHandItem().is(ModItems.SPECIAL_EQUIPMENT_SUPPLY.get())
                || mc.player.getOffhandItem().is(ModItems.SPECIAL_EQUIPMENT_SUPPLY.get())
                || mc.player.getMainHandItem().is(ModItems.HOOK_AUTHOR_BLESSING.get()));
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST) public static void mouse(InputEvent.MouseButton.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (tool() == 0 || mc.screen != null || mc.player == null) return;
        if (!mc.player.hasEffect(ModEffects.ROVER_DOWNED) && refreshItemHasPriority(mc)) return;
        if (event.getButton() != GLFW.GLFW_MOUSE_BUTTON_LEFT && event.getButton() != GLFW.GLFW_MOUSE_BUTTON_RIGHT) return;
        event.setCanceled(true);
        if (event.getAction() != GLFW.GLFW_PRESS) return;
        boolean left = event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT;
        int action = state.getInt("Tool") == 1 ? (left ? 1 : 0) : (left ? 2 : 3);
        NetworkHandler.sendToServer(new C2S_RoverAction(action));
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST) public static void interaction(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && (mc.player.hasEffect(ModEffects.ROVER_DOWNED)
                || tool() > 0 && !refreshItemHasPriority(mc))) {
            event.setCanceled(true); event.setSwingHand(false);
        }
    }

    @SubscribeEvent public static void hud(RenderGuiLayerEvent.Post event) {
        if (!SkillHudRenderCompat.shouldRender(event)) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        GuiGraphics graphics = event.getGuiGraphics();
        if (ClientCharacterSelectionState.isDisplayedCharacter(ModCharacters.ROVER_ID)) {
            int x = ClientHudLayout.x(8), y = ClientHudLayout.y(Math.max(8, graphics.guiHeight() - 68));
            slot(graphics, x, y, "dog", KeybindRegister.CORE_SKILL,
                    state.getInt("CoreCooldown"), state.getInt("DogTicks") > 0 ? seconds(state.getInt("DogTicks")) : "", 0xFFE0CB83);
            slot(graphics, x + 50, y, "injector", KeybindRegister.ACTIVE_SKILL_2,
                    state.getInt("Injector") == 0 ? state.getInt("InjectorCooldown") : 0,
                    state.getInt("Injector") + "/" + state.getInt("InjectorMax"), 0xFF9AD68C);
            slot(graphics, x + 100, y, "smoke", KeybindRegister.ACTIVE_SKILL_1,
                    state.getInt("Smoke") == 0 ? state.getInt("SmokeCooldown") : 0,
                    state.getInt("Smoke") + "/" + state.getInt("SmokeMax"), 0xFFBAC0C3);
            if (state.getInt("Tool") == 2) graphics.drawCenteredString(mc.font,
                    Component.translatable(state.getBoolean("Bounce") ? "hud.dealt_force_skills.rover.bounce" : "hud.dealt_force_skills.rover.impact"),
                    graphics.guiWidth() / 2, graphics.guiHeight() / 2 + 18, 0xFFA8DE92);
        }
        if (state.getInt("Downed") > 0) graphics.drawCenteredString(mc.font,
                Component.translatable("hud.dealt_force_skills.rover.downed", seconds(state.getInt("Downed"))), graphics.guiWidth() / 2, 26, 0xFFE9B881);
        if (state.getInt("Rescue") > 0) {
            int x = graphics.guiWidth() / 2 - 60;
            graphics.fill(x, 43, x + 120, 50, 0xAA111111);
            graphics.fill(x, 43, x + Math.min(120, 120 * state.getInt("Rescue") / Math.max(1, state.getInt("RescueRequired"))), 50, 0xFF9AD68C);
        }
    }

    private static void slot(GuiGraphics graphics, int x, int y, String name, KeyMapping key, int cooldown, String detail, int color) {
        StingerHudOverlay.drawSlot(graphics, Minecraft.getInstance().font, x, y, color, key,
                Component.translatable("character.dealt_force_skills.rover.skill." + name), cooldown, detail);
    }

    private static String seconds(int ticks) { return Math.max(1, (ticks + 19) / 20) + "s"; }

    @SubscribeEvent public static void markers(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL
                || !selected() && !ClientCommanderState.inherits(ModCharacters.ROVER_ID)) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        var camera = mc.gameRenderer.getMainCamera();
        var buffers = mc.renderBuffers().bufferSource();
        // WorldRenderMatrixCompat already restores AFTER_LEVEL's world model-view.
        var pose = event.getPoseStack();
        var markers = state.getList("Markers", Tag.TAG_COMPOUND).copy();
        if (state.contains("Waypoint")) {
            CompoundTag waypoint = state.getCompound("Waypoint").copy(); waypoint.putString("Kind", "waypoint"); markers.add(waypoint);
        }
        for (Tag value : markers) {
            CompoundTag marker = (CompoundTag) value;
            Vec3 base = new Vec3(marker.getDouble("X"), marker.getDouble("Y"), marker.getDouble("Z"));
            Vec3 cameraPos = camera.getPosition();
            var lines = buffers.getBuffer(DfsRenderTypes.xrayLines());
            LevelRenderer.renderLineBox(pose, lines,
                    new AABB(base.x - 0.4, base.y, base.z - 0.4, base.x + 0.4, base.y + 0.15, base.z + 0.4)
                            .move(-cameraPos.x, -cameraPos.y, -cameraPos.z), 0.66F, 0.87F, 0.57F, 1.0F);
            LevelRenderer.renderLineBox(pose, lines,
                    new AABB(base.x - 0.06, base.y, base.z - 0.06, base.x + 0.06, base.y + 1.5, base.z + 0.06)
                            .move(-cameraPos.x, -cameraPos.y, -cameraPos.z), 0.66F, 0.87F, 0.57F, 1.0F);
            buffers.endBatch(DfsRenderTypes.xrayLines());
            Vec3 pos = new Vec3(marker.getDouble("X"), marker.getDouble("Y") + 1, marker.getDouble("Z"));
            pose.pushPose();
            Vec3 relative = pos.subtract(camera.getPosition());
            pose.translate(relative.x, relative.y, relative.z); pose.mulPose(camera.rotation()); pose.scale(-0.025F, -0.025F, 0.025F);
            String text = Component.translatable("hud.dealt_force_skills.rover.marker." + marker.getString("Kind")).getString()
                    + " " + Math.round(pos.distanceTo(mc.player.position())) + "m";
            mc.font.drawInBatch(text, -mc.font.width(text) / 2F, 0, 0xFFA8DE92, false, pose.last().pose(), buffers,
                    Font.DisplayMode.SEE_THROUGH, 0x66000000, LightTexture.FULL_BRIGHT);
            pose.popPose();
        }
        buffers.endBatch();
    }
}
