package com.rzy.dealt_force_skills.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.rzy.dealt_force_skills.character.SkillSlot;
import com.rzy.dealt_force_skills.client.character.ClientCharacterSelectionState;
import com.rzy.dealt_force_skills.client.character.ClientUndeadHudState;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Keeps the existing button transforms while separating icon, timer and key rows. */
public final class SkillIconHud {
    private static int right, rowY, rowSize;
    private record Previous(int charges, int cooldown, long pulseUntil) {}
    private static final java.util.Map<String, Previous> PREVIOUS = new java.util.HashMap<>();
    private static Object levelIdentity;
    private static String characterIdentity = "";
    private SkillIconHud() {}

    static void beginFrame() {
        right = Integer.MIN_VALUE;
        var level = Minecraft.getInstance().level;
        String character = ClientCharacterSelectionState.displayedCharacterId();
        if (levelIdentity != level || !characterIdentity.equals(character)) {
            PREVIOUS.clear(); levelIdentity = level; characterIdentity = character;
        }
    }

    public static void draw(GuiGraphics graphics, Font font, int x, int y, int size,
                            KeyMapping key, Component name, int cooldown, String detail) {
        if (KeybindRegister.reversedActives() && key != null) {
            if (key == KeybindRegister.ACTIVE_SKILL_1) { x -= size + 4; key = KeybindRegister.ACTIVE_SKILL_2; }
            else if (key == KeybindRegister.ACTIVE_SKILL_2) { x += size + 4; key = KeybindRegister.ACTIVE_SKILL_1; }
        }
        String label = key == null ? "?" : key.getTranslatedKeyMessage().getString();
        if (SkillIcons.key(name).equals("chamber.skill.tour_de_force")) label = sneakKey(label);
        draw(graphics, font, x, y, size, label, name, cooldown, detail);
    }

    public static void draw(GuiGraphics graphics, Font font, int x, int y, int size,
                            String key, Component name, int cooldown, String detail) {
        if (x + size > right) { right = x + size; rowY = y; rowSize = size; }
        String id = SkillIcons.key(name);
        SkillIconState state = SkillIconState.resolve(id, cooldown);
        long tick = Minecraft.getInstance().level == null ? 0 : Minecraft.getInstance().level.getGameTime();
        Previous previous = PREVIOUS.get(id);
        long pulse = previous == null ? 0 : previous.pulseUntil;
        if (previous != null && (state.charges() < previous.charges || state.rechargeTicks() > previous.cooldown + 2)) pulse = tick + 12;
        PREVIOUS.put(id, new Previous(state.charges(), state.rechargeTicks(), pulse));
        SkillIcons.State appearance = state.active() || ClientSkillFeedback.active(id) ? SkillIcons.State.ACTIVE
                : state.available() && ClientSkillFeedback.available(id) && canAct()
                ? (tick < pulse ? SkillIcons.State.ACTIVE : SkillIcons.State.NORMAL) : SkillIcons.State.DISABLED;
        int iconSize = size - 18;
        try (var ignored = ClientHudLayout.scaleButton(graphics, x, y, size)) {
            plate(graphics, x + (size - iconSize) / 2, y, iconSize, appearance);
            icon(graphics, SkillIcons.skill(id, appearance), x + (size - iconSize) / 2, y, iconSize, 0xFFFFFFFF);
            String count = Integer.toString(Math.max(0, state.charges()));
            smallText(graphics, font, count, x + (size + iconSize) / 2.0F - font.width(count) * 0.65F,
                    y + 1, 0.65F, 0xFFFFFFFF);
            centered(graphics, font, state.statusText(id), x + size / 2.0F, y - 9, size - 2, 0.65F, 0xFFFFE6A6);
            String time = state.timerText(id, ClientSkillFeedback.remaining(id));
            centered(graphics, font, time, x + size / 2.0F, y + iconSize + 1, size - 4, 0.65F, 0xFFFFE6A6);
            centered(graphics, font, key, x + size / 2.0F, y + size - 8, size - 4, 0.8F, 0xFFFFFFFF);
        }
    }

    private static boolean canAct() {
        var player = Minecraft.getInstance().player;
        if (player == null) return true;
        return player.isAlive() && !player.isSpectator()
                && !player.hasEffect(com.rzy.dealt_force_skills.registry.ModEffects.STUN)
                && !player.hasEffect(com.rzy.dealt_force_skills.registry.ModEffects.WEBBED)
                && !player.hasEffect(com.rzy.dealt_force_skills.registry.ModEffects.N_TWO_FROZEN)
                && !player.hasEffect(com.rzy.dealt_force_skills.registry.ModEffects.TEMPEST_DISARMED)
                && !player.hasEffect(com.rzy.dealt_force_skills.registry.ModEffects.STINGER_DOWNED)
                && !player.hasEffect(com.rzy.dealt_force_skills.registry.ModEffects.VLINDER_VITAL_DOWNED)
                && !player.hasEffect(com.rzy.dealt_force_skills.registry.ModEffects.ROVER_DOWNED)
                && !player.hasEffect(com.rzy.dealt_force_skills.registry.ModEffects.CATDAD_DOWNED);
    }

    static void endFrame(GuiGraphics graphics) {
        if (right == Integer.MIN_VALUE) return;
        var character = ClientCharacterSelectionState.catalogCharacter(ClientCharacterSelectionState.displayedCharacterId()).orElse(null);
        if (character == null) return;
        var passive = character.skill(SkillSlot.PASSIVE).orElse(null);
        if (passive == null) return;
        String key = SkillIcons.key(passive.displayName());
        if (key.equals("undead.skill.many_faces")) key = "undead." + ClientUndeadHudState.profession().id() + ".skill.passive";
        float iconSize = (rowSize - 18) * 0.75F;
        int x = right + 4;
        String detail = PassiveIconState.detail();
        float scale = ClientHudLayout.buttonScale();
        float visualX = ClientHudLayout.x(8) + (x - ClientHudLayout.x(8)) * scale;
        float detailHeight = detail.isEmpty() ? 0 : (2 + Minecraft.getInstance().font.lineHeight * 0.6F) * scale;
        float bottom = rowY + rowSize + detailHeight;
        int hotbarLeft = graphics.guiWidth() / 2 - 91;
        boolean overlapsHotbar = visualX < hotbarLeft + 182 && visualX + iconSize * scale > hotbarLeft
                && bottom > graphics.guiHeight() - 22 && rowY + rowSize - iconSize * scale < graphics.guiHeight();
        float lift = overlapsHotbar ? (bottom - graphics.guiHeight() + 26) / scale : 0;
        try (var ignored = ClientHudLayout.scaleButton(graphics, x, rowY, rowSize)) {
            plate(graphics, x, rowY + rowSize - iconSize - lift, iconSize, ClientSkillFeedback.passiveState());
            icon(graphics, SkillIcons.skill(key, ClientSkillFeedback.passiveState()), x, rowY + rowSize - iconSize - lift,
                    iconSize, 0xFFFFFFFF);
            centered(graphics, Minecraft.getInstance().font, detail, x + iconSize / 2,
                    rowY + rowSize + 2 - lift, Math.max(24, (int)iconSize + 8), 0.6F, 0xFFFFE6A6);
        }
    }

    private static void plate(GuiGraphics graphics, float x, float y, float size, SkillIcons.State state) {
        int left = Math.round(x)-1, top = Math.round(y)-1, width = Math.round(size)+2;
        graphics.fill(left, top, left+width, top+width, switch (state) {
            case NORMAL -> 0x82787878; case ACTIVE -> 0xC22B3033; case DISABLED -> 0xB51C1E20;
        });
        graphics.renderOutline(left, top, width, width, state == SkillIcons.State.ACTIVE ? 0xFFDDE6E8 : 0xAAB6B6B6);
    }

    public static String sneakKey(String key) {
        return "[" + Minecraft.getInstance().options.keyShift.getTranslatedKeyMessage().getString() + "]+[" + key + "]";
    }

    public static void icon(GuiGraphics graphics, ResourceLocation texture, float x, float y, float size, int tint) {
        graphics.flush();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor((tint >> 16 & 255) / 255F, (tint >> 8 & 255) / 255F,
                (tint & 255) / 255F, (tint >>> 24) / 255F);
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(x, y, 0);
            graphics.pose().scale(size / 64F, size / 64F, 1);
            graphics.blit(texture, 0, 0, 0, 0, 64, 64, 64, 64);
            graphics.flush();
        } finally {
            graphics.pose().popPose();
            RenderSystem.setShaderColor(1, 1, 1, 1);
        }
    }

    private static void centered(GuiGraphics graphics, Font font, String text, float center, float y,
                                 int width, float scale, int color) {
        if (text.isEmpty()) return;
        float actual = Math.min(scale, width / (float) Math.max(1, font.width(text)));
        smallText(graphics, font, text, center - font.width(text) * actual / 2, y, actual, color);
    }

    private static void smallText(GuiGraphics graphics, Font font, String text, float x, float y, float scale, int color) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        graphics.pose().scale(scale, scale, 1);
        graphics.drawString(font, text, 0, 0, color, true);
        graphics.pose().popPose();
    }
}
