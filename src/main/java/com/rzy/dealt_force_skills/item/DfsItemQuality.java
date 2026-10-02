package com.rzy.dealt_force_skills.item;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

public enum DfsItemQuality {
    GRAY("gray", 0, 0x888888),
    WHITE("white", 1, 0xFFFFFF),
    GREEN("green", 2, 0x55FF55),
    BLUE("blue", 3, 0x55AAFF),
    PURPLE("purple", 4, 0xAA55FF),
    GOLD("gold", 5, 0xFFD34D),
    RED("red", 6, 0xFF5555),
    RED_7("red", 7, 0xFF5555),
    COLORFUL("colorful", 8, 0xFF55FF);

    private static final long RAINBOW_CYCLE_MILLIS = 1_200L;
    private static final float RAINBOW_HUE_STEP = 0.08F;

    private final String key;
    private final int tier;
    private final int color;

    DfsItemQuality(String key, int tier, int color) {
        this.key = key;
        this.tier = tier;
        this.color = color;
    }

    public String key() {
        return key;
    }

    public int tier() {
        return tier;
    }

    public int color() {
        return color;
    }

    public static float rainbowPhase() {
        return (System.currentTimeMillis() % RAINBOW_CYCLE_MILLIS)
                / (float) RAINBOW_CYCLE_MILLIS;
    }

    public static int rainbowColor(float phase, int glyphIndex) {
        float hue = (phase + glyphIndex * RAINBOW_HUE_STEP) % 1.0F;
        return Mth.hsvToRgb(hue, 0.85F, 1.0F);
    }

    public static int rainbowColor(float phase, int glyphIndex, int alpha) {
        return Mth.clamp(alpha, 0, 255) << 24 | rainbowColor(phase, glyphIndex);
    }

    public static MutableComponent rainbowText(Component component) {
        return rainbowText(component, rainbowPhase());
    }

    public static MutableComponent rainbowText(Component component, float phase) {
        String text = component.getString();
        MutableComponent rainbow = Component.empty();
        int glyphIndex = 0;
        for (int offset = 0; offset < text.length();) {
            int codePoint = text.codePointAt(offset);
            String glyph = new String(Character.toChars(codePoint));
            int color = rainbowColor(phase, glyphIndex);
            rainbow.append(Component.literal(glyph).withStyle(style -> style.withColor(color)));
            glyphIndex++;
            offset += Character.charCount(codePoint);
        }
        return rainbow;
    }

    private static final Map<ResourceLocation, Integer> TAG_QUALITY_OVERRIDES = new HashMap<>();

    public static void applyTagQualityOverride(ResourceLocation itemId, int tagAge) {
        DfsItemQuality[] values = values();
        if (tagAge >= 0 && tagAge < values.length) {
            TAG_QUALITY_OVERRIDES.put(itemId, values[tagAge].tier);
        }
    }

    public static void clearTagQualityOverrides() {
        TAG_QUALITY_OVERRIDES.clear();
    }

    public static DfsItemQuality of(ItemStack stack) {
        if (stack.isEmpty()) {
            return WHITE;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id != null) {
            Integer overrideTier = TAG_QUALITY_OVERRIDES.get(id);
            if (overrideTier != null) {
                for (DfsItemQuality q : values()) {
                    if (q.tier == overrideTier) {
                        return q;
                    }
                }
            }
        }
        if (stack.getItem() instanceof QualityTooltipItem qualityItem) {
            return qualityItem.quality();
        }
        if (stack.getItem() instanceof DfsEquipmentItem equipmentItem) {
            return equipmentItem.profile().quality();
        }
        return WHITE;
    }
}
