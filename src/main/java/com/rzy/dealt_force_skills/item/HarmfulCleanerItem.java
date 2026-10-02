package com.rzy.dealt_force_skills.item;

import com.rzy.dealt_force_skills.effect.ModItemEffectHelper;
import com.rzy.dealt_force_skills.effect.InjuryManager;
import com.rzy.dealt_force_skills.config.DealtForceConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.component.CustomData;

import java.util.List;
import java.util.function.Supplier;

public class HarmfulCleanerItem extends DfsUseItem {
    private static final String SELECTED_EFFECT = "DfsSelectedHarmfulEffect";

    private final int maxAmplifierInclusive;
    private final boolean strongest;
    private final boolean selectable;
    private final boolean woundsOnly;

    public HarmfulCleanerItem(Properties properties,
                              DfsItemQuality quality,
                              String tooltipKey,
                              int useTicks,
                              int maxAmplifierInclusive,
                              boolean strongest,
                              Supplier<SoundEvent> startSound,
                              Supplier<SoundEvent> finishSound,
                              String startMessageKey,
                              String finishMessageKey) {
        this(properties, quality, tooltipKey, useTicks, maxAmplifierInclusive, strongest,
                startSound, finishSound, startMessageKey, finishMessageKey, false, false);
    }

    public HarmfulCleanerItem(Properties properties,
                              DfsItemQuality quality,
                              String tooltipKey,
                              int useTicks,
                              int maxAmplifierInclusive,
                              boolean strongest,
                              Supplier<SoundEvent> startSound,
                              Supplier<SoundEvent> finishSound,
                              String startMessageKey,
                              String finishMessageKey,
                              boolean selectable) {
        this(properties, quality, tooltipKey, useTicks, maxAmplifierInclusive, strongest,
                startSound, finishSound, startMessageKey, finishMessageKey, selectable, false);
    }

    public HarmfulCleanerItem(Properties properties,
                              DfsItemQuality quality,
                              String tooltipKey,
                              int useTicks,
                              int maxAmplifierInclusive,
                              boolean strongest,
                              Supplier<SoundEvent> startSound,
                              Supplier<SoundEvent> finishSound,
                              String startMessageKey,
                              String finishMessageKey,
                              boolean selectable,
                              boolean woundsOnly) {
        super(properties, quality, tooltipKey, useTicks, startSound, finishSound, startMessageKey, finishMessageKey);
        this.maxAmplifierInclusive = DealtForceConfig.intValue(
                configKey() + ".max_removed_effect_amplifier", maxAmplifierInclusive);
        this.strongest = DealtForceConfig.booleanValue(configKey() + ".remove_strongest_first", strongest);
        this.selectable = DealtForceConfig.booleanValue(configKey() + ".selectable_effect", selectable);
        this.woundsOnly = woundsOnly;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (selectable && player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                cycleSelectedEffect(player, stack);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        return super.use(level, player, hand);
    }

    @Override
    protected boolean canStartUse(Level level, Player player, ItemStack stack) {
        if (!selectable || level.isClientSide) {
            return true;
        }
        return canUseSelectedOrDefault(player, stack);
    }

    @Override
    protected boolean applyUseEffect(ItemStack stack, Level level, ServerPlayer player) {
        if (woundsOnly) {
            return InjuryManager.removeRandomWound(player);
        }
        if (selectable) {
            Holder<MobEffect> selected = selectedEffect(stack);
            if (selected != null) {
                boolean removed = ModItemEffectHelper.removeHarmfulEffect(player, selected, maxAmplifierInclusive);
                if (removed) {
                    clearSelectedEffect(stack);
                }
                return removed;
            }
        }
        return ModItemEffectHelper.removeHarmfulEffectPreferNonWound(player, maxAmplifierInclusive, strongest);
    }

    @Override
    protected void addExtraTooltip(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.addExtraTooltip(stack, context, tooltip, flag);
        if (!selectable) {
            return;
        }
        tooltip.add(Component.translatable("tooltip.dealt_force_skills.select_harmful_effect")
                .withStyle(ChatFormatting.GRAY));
        Holder<MobEffect> selected = selectedEffect(stack);
        if (selected != null) {
            tooltip.add(Component.translatable("tooltip.dealt_force_skills.selected_harmful_effect",
                    selected.value().getDisplayName()).withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    private boolean canUseSelectedOrDefault(Player player, ItemStack stack) {
        List<ModItemEffectHelper.HarmfulEffectChoice> choices =
                ModItemEffectHelper.harmfulEffectChoices(player, maxAmplifierInclusive);
        if (choices.isEmpty()) {
            clearSelectedEffect(stack);
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.item.no_harmful_effect"), true);
            return false;
        }
        Holder<MobEffect> selected = selectedEffect(stack);
        for (ModItemEffectHelper.HarmfulEffectChoice choice : choices) {
            if (choice.effect().equals(selected)) {
                return true;
            }
        }
        clearSelectedEffect(stack);
        return true;
    }

    private void cycleSelectedEffect(Player player, ItemStack stack) {
        List<ModItemEffectHelper.HarmfulEffectChoice> choices =
                ModItemEffectHelper.harmfulEffectChoices(player, maxAmplifierInclusive);
        if (choices.isEmpty()) {
            clearSelectedEffect(stack);
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.item.no_harmful_effect"), true);
            return;
        }
        Holder<MobEffect> selected = selectedEffect(stack);
        int current = -1;
        for (int i = 0; i < choices.size(); i++) {
            if (choices.get(i).effect().equals(selected)) {
                current = i;
                break;
            }
        }
        int next = (current + 1) % choices.size();
        ModItemEffectHelper.HarmfulEffectChoice choice = choices.get(next);
        setSelectedEffect(stack, choice.effect());
        displaySelected(player, choice, next + 1, choices.size());
    }

    private static void displaySelected(Player player, ModItemEffectHelper.HarmfulEffectChoice choice, int index, int total) {
        player.displayClientMessage(Component.translatable("message.dealt_force_skills.item.selected_harmful_effect",
                choice.effect().value().getDisplayName(), choice.amplifier() + 1,
                Math.max(0, choice.duration() / 20), index, total), true);
    }

    private static Holder<MobEffect> selectedEffect(ItemStack stack) {
        CompoundTag selTag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!selTag.contains(SELECTED_EFFECT)) {
            return null;
        }
        ResourceLocation id = ResourceLocation.tryParse(selTag.getString(SELECTED_EFFECT));
        return id == null ? null : BuiltInRegistries.MOB_EFFECT.getHolder(id).orElse(null);
    }

    private static void setSelectedEffect(ItemStack stack, Holder<MobEffect> effect) {
        ResourceLocation id = BuiltInRegistries.MOB_EFFECT.getKey(effect.value());
        if (id == null) {
            clearSelectedEffect(stack);
        } else {
            CompoundTag writeTag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            writeTag.putString(SELECTED_EFFECT, id.toString());
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(writeTag));
        }
    }

    private static void clearSelectedEffect(ItemStack stack) {
        CompoundTag removeTag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (removeTag.contains(SELECTED_EFFECT)) {
            removeTag.remove(SELECTED_EFFECT);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(removeTag));
        }
    }
}
