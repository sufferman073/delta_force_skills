package com.rzy.dealt_force_skills.item;

import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.S2C_OpenEternalLoveBlessing;
import com.rzy.dealt_force_skills.registry.ModItems;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.util.RangedSoundHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class EternalLoveBlessingItem extends QualityTooltipItem {
    private static final String RECORDS_TAG = "EternalLoveEffects";
    private static final String HELD_SOUND_TAG = "dealt_force_skills.eternal_love_blessing_held";
    private static final String LAST_OBTAIN_SOUND_TICK_TAG = "dealt_force_skills.eternal_love_blessing_last_obtain_sound";
    private static final long OBTAIN_SOUND_RETRIGGER_GRACE_TICKS = 5L;
    private static final int MAX_RECORD_LEVEL = 25;
    private static final int EFFECT_DURATION_TICKS = 13 * 20;

    public EternalLoveBlessingItem(Properties properties) {
        super(properties, DfsItemQuality.RED, null);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, net.minecraft.world.entity.player.Player player,
                                                  InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.consume(stack);
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.pass(stack);
        }
        if (player.isShiftKeyDown()) {
            clearRecords(stack);
            playRecordSound(serverPlayer);
            player.displayClientMessage(Component.translatable(
                    "message.dealt_force_skills.eternal_love_blessing.clear_all"), true);
        } else {
            openRecordsScreen(serverPlayer, stack);
        }
        return InteractionResultHolder.consume(stack);
    }

    @Override
    protected void addExtraTooltip(ItemStack stack, Item.TooltipContext context, List<Component> tooltip,
                                   TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.dealt_force_skills.eternal_love_blessing.1")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        for (String control : List.of("capture", "offhand", "upgrade", "manage", "clear")) {
            tooltip.add(Component.translatable("tooltip.dealt_force_skills.eternal_love_blessing." + control)
                    .withStyle(ChatFormatting.GRAY));
        }
        Map<ResourceLocation, Integer> records = records(stack);
        if (!records.isEmpty()) {
            tooltip.add(Component.translatable("tooltip.dealt_force_skills.eternal_love_blessing.records",
                    records.size()).withStyle(ChatFormatting.AQUA));
        }
    }

    public static void handleCapture(ServerPlayer player, boolean offhandSource) {
        ItemStack blessing = findUsableBlessing(player);
        if (blessing.isEmpty()) {
            return;
        }
        List<MobEffectInstance> effects = offhandSource
                ? collectItemEffects(player.getOffhandItem(), player)
                : new ArrayList<>(player.getActiveEffects());
        if (effects.isEmpty()) {
            player.displayClientMessage(Component.translatable(offhandSource
                    ? "message.dealt_force_skills.eternal_love_blessing.no_offhand_effects"
                    : "message.dealt_force_skills.eternal_love_blessing.no_active_effects"), true);
            return;
        }

        int changed = 0;
        for (MobEffectInstance effect : effects) {
            if (effect == null || effect.getEffect() == null) {
                continue;
            }
            changed += recordEffect(blessing, effect.getEffect(), effect.getAmplifier() + 1, offhandSource) ? 1 : 0;
        }
        if (changed <= 0) {
            player.displayClientMessage(Component.translatable(
                    "message.dealt_force_skills.eternal_love_blessing.no_valid_effects"), true);
            return;
        }
        if (offhandSource) {
            consumeOffhand(player);
        }
        playRecordSound(player);
        player.displayClientMessage(Component.translatable(
                "message.dealt_force_skills.eternal_love_blessing.recorded", changed), true);
    }

    public static void removeRecordFromAnyBlessing(ServerPlayer player, ResourceLocation effectId) {
        if (player == null || effectId == null) {
            return;
        }
        ItemStack stack = findAnyBlessing(player);
        if (stack.isEmpty()) {
            return;
        }
        CompoundTag tag = customData(stack);
        CompoundTag records = tag.getCompound(RECORDS_TAG);
        records.remove(effectId.toString());
        if (records.isEmpty()) {
            tag.remove(RECORDS_TAG);
        } else {
            tag.put(RECORDS_TAG, records);
        }
        setCustomData(stack, tag);
        playRecordSound(player);
        openRecordsScreen(player, stack);
    }

    public static void openRecordsScreen(ServerPlayer player, ItemStack stack) {
        List<S2C_OpenEternalLoveBlessing.Entry> entries = records(stack).entrySet().stream()
                .map(entry -> {
                    Holder<MobEffect> effect = effectHolder(entry.getKey());
                    Component name = effect == null
                            ? Component.literal(entry.getKey().toString())
                            : effect.value().getDisplayName();
                    return new S2C_OpenEternalLoveBlessing.Entry(entry.getKey().toString(),
                            name.getString(), entry.getValue());
                })
                .sorted(Comparator.comparing(S2C_OpenEternalLoveBlessing.Entry::name)
                        .thenComparing(S2C_OpenEternalLoveBlessing.Entry::effectId))
                .toList();
        NetworkHandler.sendToPlayer(new S2C_OpenEternalLoveBlessing(entries), player);
    }

    public static void tickInventoryEffects(ServerPlayer player) {
        tickHeldSound(player);
        Map<Holder<MobEffect>, Integer> merged = new LinkedHashMap<>();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.is(ModItems.ETERNAL_LOVE_BLESSING.get())) {
                continue;
            }
            for (Map.Entry<ResourceLocation, Integer> entry : records(stack).entrySet()) {
                Holder<MobEffect> effect = effectHolder(entry.getKey());
                if (effect != null) {
                    merged.merge(effect, entry.getValue(), Math::max);
                }
            }
        }
        for (Map.Entry<Holder<MobEffect>, Integer> entry : merged.entrySet()) {
            int amplifier = Math.max(0, Math.min(MAX_RECORD_LEVEL, entry.getValue()) - 1);
            MobEffectInstance current = player.getEffect(entry.getKey());
            if (current == null || current.getAmplifier() < amplifier || current.getDuration() < 220) {
                player.addEffect(new MobEffectInstance(entry.getKey(), EFFECT_DURATION_TICKS, amplifier,
                        true, false, true), player);
            }
        }
    }

    public static Map<ResourceLocation, Integer> records(ItemStack stack) {
        Map<ResourceLocation, Integer> result = new LinkedHashMap<>();
        if (stack.isEmpty()) {
            return result;
        }
        CompoundTag tag = customData(stack);
        if (!tag.contains(RECORDS_TAG, Tag.TAG_COMPOUND)) {
            return result;
        }
        CompoundTag records = tag.getCompound(RECORDS_TAG);
        for (String key : records.getAllKeys()) {
            try {
                ResourceLocation id = ResourceLocation.parse(key);
                int level = Math.max(1, Math.min(MAX_RECORD_LEVEL, records.getInt(key)));
                result.put(id, level);
            } catch (RuntimeException ignored) {
            }
        }
        return result;
    }

    private static boolean recordEffect(ItemStack stack, Holder<MobEffect> effect, int level,
                                        boolean incrementExisting) {
        if (effect == null) {
            return false;
        }
        ResourceLocation id = BuiltInRegistries.MOB_EFFECT.getKey(effect.value());
        if (id == null) {
            return false;
        }
        CompoundTag tag = customData(stack);
        CompoundTag records = tag.getCompound(RECORDS_TAG);
        String key = id.toString();
        int old = Math.max(0, records.getInt(key));
        int next = incrementExisting && old > 0 ? old + 1 : Math.max(old, level);
        records.putInt(key, Math.max(1, Math.min(MAX_RECORD_LEVEL, next)));
        tag.put(RECORDS_TAG, records);
        setCustomData(stack, tag);
        return true;
    }

    private static void clearRecords(ItemStack stack) {
        CompoundTag tag = customData(stack);
        tag.remove(RECORDS_TAG);
        setCustomData(stack, tag);
    }

    private static List<MobEffectInstance> collectItemEffects(ItemStack stack, ServerPlayer player) {
        List<MobEffectInstance> result = new ArrayList<>();
        if (stack.isEmpty()) {
            return result;
        }
        PotionContents potion = stack.get(DataComponents.POTION_CONTENTS);
        if (potion != null) {
            for (MobEffectInstance instance : potion.getAllEffects()) {
                result.add(instance);
            }
        }
        FoodProperties food = stack.get(DataComponents.FOOD);
        if (food != null) {
            for (Object possible : food.effects()) {
                MobEffectInstance instance = foodEffectInstance(possible);
                if (instance != null) {
                    result.add(instance);
                }
            }
        }
        if (stack.getItem() instanceof EffectConsumableItem item) {
            for (EffectConsumableItem.EffectEntry entry : item.blessingPreviewEffects()) {
                MobEffect effect = entry.effect().get();
                if (effect != null) {
                    result.add(new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect),
                            entry.durationTicks(), entry.amplifier()));
                }
            }
        }
        if (stack.getItem() instanceof RandomEffectConsumableItem item) {
            for (EffectConsumableItem.EffectEntry entry : item.blessingPreviewOutcome(player.getRandom())) {
                MobEffect effect = entry.effect().get();
                if (effect != null) {
                    result.add(new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect),
                            entry.durationTicks(), entry.amplifier()));
                }
            }
        }
        return result;
    }

    private static MobEffectInstance foodEffectInstance(Object value) {
        if (value instanceof MobEffectInstance instance) {
            return instance;
        }
        if (value instanceof Supplier<?> supplier && supplier.get() instanceof MobEffectInstance instance) {
            return instance;
        }
        if (value == null) {
            return null;
        }
        for (String methodName : List.of("effect", "getEffect")) {
            try {
                Method method = value.getClass().getMethod(methodName);
                return foodEffectInstance(method.invoke(value));
            } catch (ReflectiveOperationException | ClassCastException | LinkageError ignored) {
            }
        }
        return null;
    }

    private static ItemStack findUsableBlessing(ServerPlayer player) {
        ItemStack main = player.getMainHandItem();
        return main.is(ModItems.ETERNAL_LOVE_BLESSING.get()) ? main : ItemStack.EMPTY;
    }

    private static ItemStack findAnyBlessing(ServerPlayer player) {
        ItemStack main = player.getMainHandItem();
        if (main.is(ModItems.ETERNAL_LOVE_BLESSING.get())) {
            return main;
        }
        ItemStack offhand = player.getOffhandItem();
        if (offhand.is(ModItems.ETERNAL_LOVE_BLESSING.get())) {
            return offhand;
        }
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(ModItems.ETERNAL_LOVE_BLESSING.get())) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static void consumeOffhand(ServerPlayer player) {
        if (player.getAbilities().instabuild) {
            return;
        }
        ItemStack offhand = player.getOffhandItem();
        if (!offhand.isEmpty()) {
            offhand.shrink(1);
        }
    }

    private static Holder<MobEffect> effectHolder(ResourceLocation id) {
        MobEffect effect = BuiltInRegistries.MOB_EFFECT.get(id);
        return effect == null ? null : BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect);
    }

    private static CompoundTag customData(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    private static void setCustomData(ItemStack stack, CompoundTag tag) {
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static void playObtainSound(ServerPlayer player) {
        markObtainSoundTick(player);
        playInterruptingSound(player, ModSounds.ETERNAL_LOVE_BLESSING_OBTAIN.get(), 0.85F);
    }

    private static void playRecordSound(ServerPlayer player) {
        playInterruptingSound(player, ModSounds.ETERNAL_LOVE_BLESSING_RECORD.get(), 0.75F);
    }

    private static void playInterruptingSound(ServerPlayer player, SoundEvent sound, float volume) {
        ServerLevel level = player.serverLevel();
        RangedSoundHelper.stop(level, player.position(), ModSounds.ETERNAL_LOVE_BLESSING_RECORD.get(),
                SoundSource.PLAYERS, 32.0D);
        RangedSoundHelper.stop(level, player.position(), ModSounds.ETERNAL_LOVE_BLESSING_OBTAIN.get(),
                SoundSource.PLAYERS, 32.0D);
        RangedSoundHelper.play(level, player.position(), sound, SoundSource.PLAYERS, volume, 1.0F, 32.0D);
    }

    private static void tickHeldSound(ServerPlayer player) {
        boolean held = player.getMainHandItem().is(ModItems.ETERNAL_LOVE_BLESSING.get())
                || player.getOffhandItem().is(ModItems.ETERNAL_LOVE_BLESSING.get());
        CompoundTag data = player.getPersistentData();
        boolean wasHeld = data.getBoolean(HELD_SOUND_TAG);
        if (held && !wasHeld && canPlayObtainSound(player)) {
            playObtainSound(player);
        }
        data.putBoolean(HELD_SOUND_TAG, held);
    }

    private static boolean canPlayObtainSound(ServerPlayer player) {
        long now = player.level().getGameTime();
        long last = player.getPersistentData().getLong(LAST_OBTAIN_SOUND_TICK_TAG);
        return now - last > OBTAIN_SOUND_RETRIGGER_GRACE_TICKS;
    }

    private static void markObtainSoundTick(ServerPlayer player) {
        player.getPersistentData().putLong(LAST_OBTAIN_SOUND_TICK_TAG, player.level().getGameTime());
    }
}
