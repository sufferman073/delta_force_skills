package com.rzy.dealt_force_skills.client.visual;

import com.mojang.math.Axis;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.client.character.ClientNoxHudState;
import com.rzy.dealt_force_skills.registry.ModEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.ModList;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT)
public final class UndeadPlayerVisuals {
    private static final EquipmentSlot[] HIDDEN_EQUIPMENT_SLOTS = {
            EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND, EquipmentSlot.HEAD,
            EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };
    private static final Set<Integer> ROTATED_PLAYERS = new HashSet<>();
    private static final Map<UUID, Map<EquipmentSlot, ItemStackSnapshot>> YSM_HIDDEN_EQUIPMENT = new HashMap<>();

    private UndeadPlayerVisuals() {
    }

    public static void resetTransientState() {
        restoreYsmEquipment(Minecraft.getInstance());
        ROTATED_PLAYERS.clear();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onHidePlayerPre(RenderPlayerEvent.Pre event) {
        if (shouldHideCompletely(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        if (player == Minecraft.getInstance().player
                || !player.hasEffect(ModEffects.UNDEAD_HUNTER_SCATTER)) {
            return;
        }
        event.getPoseStack().pushPose();
        float angle = (player.tickCount + com.rzy.dealt_force_skills.client.ClientRenderCompat.partialTick(event.getPartialTick())) * 18.0F;
        event.getPoseStack().mulPose(Axis.YP.rotationDegrees(angle));
        ROTATED_PLAYERS.add(player.getId());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        if (ROTATED_PLAYERS.remove(event.getEntity().getId())) {
            event.getPoseStack().popPose();
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderLivingPre(RenderLivingEvent.Pre<?, ?> event) {
        if (event.getEntity() instanceof Player player && shouldHideCompletely(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderNameTag(RenderNameTagEvent event) {
        if (event.getEntity() instanceof Player player && shouldHideCompletely(player)) {
            event.setCanRender(TriState.FALSE);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderHand(RenderHandEvent event) {
        Player player = Minecraft.getInstance().player;
        if (player != null && shouldHideCompletely(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderLevelStart(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY && isYesSteveModelLoaded()) {
            hideYsmEquipment(Minecraft.getInstance());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderLevelEnd(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            restoreYsmEquipment(Minecraft.getInstance());
        }
    }

    private static void hideYsmEquipment(Minecraft minecraft) {
        restoreYsmEquipment(minecraft);
        if (minecraft.level == null) {
            return;
        }
        for (Player player : minecraft.level.players()) {
            if (!shouldHideCompletely(player)) {
                continue;
            }
            EnumMap<EquipmentSlot, ItemStackSnapshot> snapshot = new EnumMap<>(EquipmentSlot.class);
            for (EquipmentSlot slot : HIDDEN_EQUIPMENT_SLOTS) {
                net.minecraft.world.item.ItemStack stack = player.getItemBySlot(slot);
                if (!stack.isEmpty()) {
                    snapshot.put(slot, new ItemStackSnapshot(stack));
                    player.setItemSlot(slot, net.minecraft.world.item.ItemStack.EMPTY);
                }
            }
            if (!snapshot.isEmpty()) {
                YSM_HIDDEN_EQUIPMENT.put(player.getUUID(), snapshot);
            }
        }
    }

    private static void restoreYsmEquipment(Minecraft minecraft) {
        if (YSM_HIDDEN_EQUIPMENT.isEmpty()) {
            return;
        }
        if (minecraft.level == null) {
            YSM_HIDDEN_EQUIPMENT.clear();
            return;
        }
        for (Map.Entry<UUID, Map<EquipmentSlot, ItemStackSnapshot>> entry
                : new HashMap<>(YSM_HIDDEN_EQUIPMENT).entrySet()) {
            Player player = minecraft.level.getPlayerByUUID(entry.getKey());
            if (player != null) {
                entry.getValue().forEach((slot, snapshot) -> player.setItemSlot(slot, snapshot.stack()));
            }
            YSM_HIDDEN_EQUIPMENT.remove(entry.getKey());
        }
    }

    private static boolean isYesSteveModelLoaded() {
        return ModList.get().getMods().stream().anyMatch(modInfo -> {
            String id = modInfo.getModId().toLowerCase(Locale.ROOT);
            return id.equals("yes_steve_model") || id.equals("yesstevemodel") || id.equals("ysm")
                    || id.contains("yes_steve") || id.contains("yessteve");
        });
    }

    private static boolean shouldHideCompletely(Player player) {
        return player.hasEffect(ModEffects.UNDEAD_TRUE_INVISIBILITY)
                || player.hasEffect(ModEffects.DEPARTMENT_CONCEALMENT)
                || (player.hasEffect(ModEffects.NOX_STEALTH)
                && !ClientNoxHudState.isEntityRevealed(player.getId()));
    }

    private record ItemStackSnapshot(net.minecraft.world.item.ItemStack stack) {
    }
}
