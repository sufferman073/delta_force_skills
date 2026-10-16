package com.rzy.dealt_force_skills.character.chamber;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.advancement.DfsAchievements;
import com.rzy.dealt_force_skills.character.CharacterSelectionManager;
import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.entity.ChamberSlowFieldEntity;
import com.rzy.dealt_force_skills.entity.ChamberTeleportAnchorEntity;
import com.rzy.dealt_force_skills.entity.ChamberTripTrapEntity;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.S2C_SyncChamberState;
import com.rzy.dealt_force_skills.registry.ModEntities;
import com.rzy.dealt_force_skills.shop.HaffCoinManager;
import com.rzy.dealt_force_skills.skill.SkillCooldownHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class ChamberStateManager {
    public static final int TELEPORT_COOLDOWN_TICKS = 30 * 20;
    public static final int TRAP_COOLDOWN_TICKS = 30 * 20;
    public static final int HEADHUNTER_COOLDOWN_TICKS = 60 * 20;
    public static final int TOUR_DE_FORCE_COOLDOWN_TICKS = 120 * 20;
    public static final int TOUR_SLOW_FIELD_TRIGGER_INTERVAL_TICKS = 5 * 20;
    public static final double TELEPORT_RADIUS = 30.0D;
    private static final double MARKER_SEARCH_RANGE = 180.0D;
    private static final long ROUND_TAX = 3000L;
    private static final double HAFF_INCOME_MULTIPLIER = 1.25D;

    private static final String ROOT_TAG = DealtForceSkillsMod.MODID + ".chamber";
    private static final String INITIALIZED = "Initialized";
    private static final String TELEPORT_COOLDOWN_UNTIL = "TeleportCooldownUntil";
    private static final String TRAP_COOLDOWN_UNTIL = "TrapCooldownUntil";
    private static final String HEADHUNTER_COOLDOWN_UNTIL = "HeadhunterCooldownUntil";
    private static final String TOUR_DE_FORCE_COOLDOWN_UNTIL = "TourDeForceCooldownUntil";
    private static final String TOUR_SLOW_FIELD_COOLDOWN_UNTIL = "TourSlowFieldCooldownUntil";
    private static final String EQUIPPED_TOOL = "EquippedTool";
    private static final String EQUIPPED_GUN = "EquippedGun";
    private static final String EQUIPPED_GUN_SLOT = "EquippedGunSlot";

    private ChamberStateManager() {
    }

    public static boolean isChamber(Player player) {
        Optional<String> selected = CharacterSelectionManager.getSelectedCharacterId(player);
        return selected.isPresent() && ModCharacters.CHAMBER_ID.equals(selected.get());
    }

    public static boolean hasHaffPassive(ServerPlayer player) {
        return isChamber(player);
    }

    public static long scaleHaffIncome(ServerPlayer player, long amount) {
        if (amount <= 0L || !hasHaffPassive(player)) {
            return amount;
        }
        return Math.max(amount, Math.round(amount * HAFF_INCOME_MULTIPLIER));
    }

    public static void initializeIfNeeded(ServerPlayer player) {
        if (!isChamber(player)) {
            return;
        }
        CompoundTag tag = data(player);
        if (tag.getBoolean(INITIALIZED)) {
            return;
        }
        tag.putBoolean(INITIALIZED, true);
        tag.putLong(TELEPORT_COOLDOWN_UNTIL, 0L);
        tag.putLong(TRAP_COOLDOWN_UNTIL, 0L);
        tag.putLong(HEADHUNTER_COOLDOWN_UNTIL, 0L);
        tag.putLong(TOUR_DE_FORCE_COOLDOWN_UNTIL, 0L);
        tag.putLong(TOUR_SLOW_FIELD_COOLDOWN_UNTIL, 0L);
        tag.putInt(EQUIPPED_TOOL, ChamberTool.NONE.ordinal());
        tag.putInt(EQUIPPED_GUN, ChamberGunKind.NONE.ordinal());
        tag.putInt(EQUIPPED_GUN_SLOT, -1);
    }

    public static void copyState(Player original, Player target) {
        CompoundTag originalData = original.getPersistentData();
        if (originalData.contains(ROOT_TAG, Tag.TAG_COMPOUND)) {
            target.getPersistentData().put(ROOT_TAG, originalData.getCompound(ROOT_TAG).copy());
        }
    }

    public static void tick(ServerPlayer player) {
        if (!isChamber(player)) {
            setEquippedTool(player, ChamberTool.NONE);
            removeAllChamberGuns(player);
            clearEquippedGun(player);
            return;
        }
        initializeIfNeeded(player);
        if (hasEquippedGun(player)) {
            lockEquippedGunSlot(player);
            return;
        }
        ChamberGunKind mainHandKind = ChamberTaczEnhancement.kind(player.getMainHandItem());
        if (mainHandKind != ChamberGunKind.NONE) {
            rememberEquippedGun(player, mainHandKind);
        }
    }

    public static ChamberTool equippedTool(Player player) {
        int ordinal = data(player).getInt(EQUIPPED_TOOL);
        ChamberTool[] values = ChamberTool.values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : ChamberTool.NONE;
    }

    public static void setEquippedTool(Player player, ChamberTool tool) {
        data(player).putInt(EQUIPPED_TOOL, tool.ordinal());
    }

    public static ChamberGunKind equippedGun(Player player) {
        int ordinal = data(player).getInt(EQUIPPED_GUN);
        ChamberGunKind[] values = ChamberGunKind.values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : ChamberGunKind.NONE;
    }

    public static boolean hasEquippedGun(Player player) {
        return equippedGun(player) != ChamberGunKind.NONE;
    }

    public static int teleportCooldownRemainingTicks(Player player) {
        return remainingTicks(player, TELEPORT_COOLDOWN_UNTIL);
    }

    public static int trapCooldownRemainingTicks(Player player) {
        if (player instanceof ServerPlayer serverPlayer && hasActiveTrap(serverPlayer)) {
            return 0;
        }
        return remainingTicks(player, TRAP_COOLDOWN_UNTIL);
    }

    public static int headhunterCooldownRemainingTicks(Player player) {
        return remainingTicks(player, HEADHUNTER_COOLDOWN_UNTIL);
    }

    public static int tourDeForceCooldownRemainingTicks(Player player) {
        return remainingTicks(player, TOUR_DE_FORCE_COOLDOWN_UNTIL);
    }

    public static void setTeleportCooldown(ServerPlayer player) {
        setCooldown(player, TELEPORT_COOLDOWN_UNTIL, TELEPORT_COOLDOWN_TICKS);
    }

    public static void setTrapCooldown(ServerPlayer player) {
        setCooldown(player, TRAP_COOLDOWN_UNTIL, TRAP_COOLDOWN_TICKS);
    }

    public static void setGunCooldown(ServerPlayer player, ChamberGunKind kind) {
        if (kind == ChamberGunKind.HEADHUNTER) {
            setCooldown(player, HEADHUNTER_COOLDOWN_UNTIL, HEADHUNTER_COOLDOWN_TICKS);
        } else if (kind == ChamberGunKind.TOUR_DE_FORCE) {
            setCooldown(player, TOUR_DE_FORCE_COOLDOWN_UNTIL, TOUR_DE_FORCE_COOLDOWN_TICKS);
        }
    }

    public static void equipGun(ServerPlayer player, ChamberGunKind kind) {
        rememberEquippedGun(player, kind);
    }

    public static void clearEquippedGun(Player player) {
        CompoundTag tag = data(player);
        tag.putInt(EQUIPPED_GUN, ChamberGunKind.NONE.ordinal());
        tag.putInt(EQUIPPED_GUN_SLOT, -1);
    }

    public static void lockEquippedGunSlot(ServerPlayer player) {
        ChamberGunKind kind = equippedGun(player);
        if (kind == ChamberGunKind.NONE) {
            return;
        }
        CompoundTag tag = data(player);
        int lockedSlot = tag.getInt(EQUIPPED_GUN_SLOT);
        if (!isHotbarSlot(lockedSlot)) {
            lockedSlot = player.getInventory().selected;
            tag.putInt(EQUIPPED_GUN_SLOT, lockedSlot);
        }
        if (ChamberTaczEnhancement.kind(player.getInventory().items.get(lockedSlot)) != kind
                && !restoreGunToLockedSlot(player, kind, lockedSlot)) {
            lockedSlot = findHotbarGunSlot(player, kind);
            if (lockedSlot < 0) {
                clearEquippedGun(player);
                return;
            }
            tag.putInt(EQUIPPED_GUN_SLOT, lockedSlot);
        }
        purgeDuplicateChamberGuns(player, kind, lockedSlot);
        if (player.getInventory().selected != lockedSlot) {
            player.getInventory().selected = lockedSlot;
            player.connection.send(new ClientboundSetCarriedItemPacket(lockedSlot));
        }
        if (player.containerMenu != player.inventoryMenu) {
            player.closeContainer();
        }
    }

    public static boolean restoreCanceledGunToss(ServerPlayer player, ItemStack tossedStack) {
        if (!isChamber(player) || tossedStack == null) {
            return false;
        }
        ChamberGunKind tossedKind = ChamberTaczEnhancement.kind(tossedStack);
        ChamberGunKind equippedKind = equippedGun(player);
        ChamberGunKind kind = tossedKind != ChamberGunKind.NONE ? tossedKind : equippedKind;
        if (kind == ChamberGunKind.NONE) {
            return false;
        }
        if (equippedKind != ChamberGunKind.NONE && equippedKind != kind) {
            return false;
        }

        CompoundTag tag = data(player);
        int existingSlot = findHotbarGunSlot(player, kind);
        if (existingSlot >= 0) {
            tag.putInt(EQUIPPED_GUN, kind.ordinal());
            tag.putInt(EQUIPPED_GUN_SLOT, existingSlot);
            lockEquippedGunSlot(player);
            return true;
        }
        if (tossedKind == ChamberGunKind.NONE) {
            return false;
        }

        ItemStack restored = tossedStack.copy();
        int lockedSlot = tag.getInt(EQUIPPED_GUN_SLOT);
        if (!isHotbarSlot(lockedSlot)) {
            lockedSlot = player.getInventory().selected;
        }
        int restoredSlot = placeInEmptyHotbarSlot(player, lockedSlot, restored);
        if (restoredSlot < 0) {
            restoredSlot = placeInEmptyHotbarSlot(player, player.getInventory().selected, restored);
        }
        if (restoredSlot < 0) {
            restoredSlot = placeInEmptyHotbarSlot(player, findEmptyHotbarSlot(player), restored);
        }
        if (restoredSlot < 0) {
            return false;
        }

        tag.putInt(EQUIPPED_GUN, kind.ordinal());
        tag.putInt(EQUIPPED_GUN_SLOT, restoredSlot);
        player.getInventory().setChanged();
        lockEquippedGunSlot(player);
        return true;
    }

    public static boolean canUseHeadhunter(Player player) {
        return headhunterCooldownRemainingTicks(player) <= 0;
    }

    public static boolean canUseTourDeForce(Player player) {
        return tourDeForceCooldownRemainingTicks(player) <= 0;
    }

    public static Optional<ChamberTeleportAnchorEntity> nearestAnchor(ServerPlayer player) {
        return player.serverLevel()
                .getEntitiesOfClass(ChamberTeleportAnchorEntity.class, markerSearchBox(player),
                        anchor -> anchor.isOwnedBy(player.getUUID()))
                .stream()
                .min(Comparator.comparingDouble(anchor -> anchor.distanceToSqr(player)));
    }

    public static boolean hasActiveTrap(ServerPlayer player) {
        return !player.serverLevel()
                .getEntitiesOfClass(ChamberTripTrapEntity.class, markerSearchBox(player),
                        trap -> trap.isOwnedBy(player.getUUID()))
                .isEmpty();
    }

    public static List<ChamberMarker> activeMarkers(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        List<ChamberMarker> markers = new ArrayList<>();
        level.getEntitiesOfClass(ChamberTeleportAnchorEntity.class, markerSearchBox(player),
                        anchor -> anchor.isOwnedBy(player.getUUID()))
                .forEach(anchor -> markers.add(new ChamberMarker(anchor.getId(), ChamberMarkerType.TELEPORT_ANCHOR, anchor.position())));
        level.getEntitiesOfClass(ChamberTripTrapEntity.class, markerSearchBox(player),
                        trap -> trap.isOwnedBy(player.getUUID()))
                .forEach(trap -> markers.add(new ChamberMarker(trap.getId(), ChamberMarkerType.TRAP, trap.position())));
        markers.sort(Comparator.comparingDouble(marker -> marker.position().distanceToSqr(player.position())));
        return List.copyOf(markers);
    }

    public static void syncToClient(ServerPlayer player) {
        if (!isChamber(player)) {
            return;
        }
        initializeIfNeeded(player);
        NetworkHandler.sendToPlayer(new S2C_SyncChamberState(
                teleportCooldownRemainingTicks(player),
                trapCooldownRemainingTicks(player),
                headhunterCooldownRemainingTicks(player),
                tourDeForceCooldownRemainingTicks(player),
                equippedTool(player).ordinal(),
                equippedGun(player).ordinal(),
                activeMarkers(player)
        ), player);
    }

    public static void collectRoundTax(ServerPlayer chamber, List<ServerPlayer> teammates) {
        if (!isChamber(chamber) || teammates == null || teammates.isEmpty()) {
            return;
        }
        long collected = 0L;
        for (ServerPlayer teammate : teammates) {
            if (teammate == null || teammate.getUUID().equals(chamber.getUUID())) {
                continue;
            }
            long available = HaffCoinManager.get(teammate);
            long taken = Math.min(ROUND_TAX, available);
            if (taken <= 0L) {
                continue;
            }
            HaffCoinManager.set(teammate, available - taken);
            collected += taken;
        }
        if (collected > 0L) {
            HaffCoinManager.grant(chamber, collected);
            DfsAchievements.recordChamberRoundTax(chamber, collected);
            chamber.displayClientMessage(Component.translatable(
                    "message.dealt_force_skills.chamber.round_tax", collected), true);
        }
    }

    public static boolean tryTriggerTourSlowField(ServerPlayer player, LivingEntity target) {
        if (target == null || !target.isAlive() || target.level().isClientSide) {
            return false;
        }
        CompoundTag tag = data(player);
        long now = SkillCooldownHelper.now(player);
        if (tag.getLong(TOUR_SLOW_FIELD_COOLDOWN_UNTIL) > now) {
            return false;
        }
        tag.putLong(TOUR_SLOW_FIELD_COOLDOWN_UNTIL,
                SkillCooldownHelper.until(player, now, TOUR_SLOW_FIELD_TRIGGER_INTERVAL_TICKS));
        ServerLevel level = player.serverLevel();
        ChamberSlowFieldEntity field = new ChamberSlowFieldEntity(ModEntities.CHAMBER_SLOW_FIELD.get(), level, player);
        Vec3 center = target.position();
        field.setPos(center.x, center.y, center.z);
        level.addFreshEntity(field);
        return true;
    }

    private static void rememberEquippedGun(ServerPlayer player, ChamberGunKind kind) {
        CompoundTag tag = data(player);
        tag.putInt(EQUIPPED_GUN, kind.ordinal());
        tag.putInt(EQUIPPED_GUN_SLOT, player.getInventory().selected);
    }

    private static int findHotbarGunSlot(ServerPlayer player, ChamberGunKind kind) {
        for (int slot = 0; slot < 9; slot++) {
            if (ChamberTaczEnhancement.kind(player.getInventory().items.get(slot)) == kind) {
                return slot;
            }
        }
        return -1;
    }

    private static int findInventoryGunSlot(ServerPlayer player, ChamberGunKind kind) {
        for (int slot = 0; slot < player.getInventory().items.size(); slot++) {
            if (ChamberTaczEnhancement.kind(player.getInventory().items.get(slot)) == kind) {
                return slot;
            }
        }
        return -1;
    }

    private static boolean restoreGunToLockedSlot(ServerPlayer player, ChamberGunKind kind, int lockedSlot) {
        if (!isHotbarSlot(lockedSlot)) {
            return false;
        }
        ItemStack lockedStack = player.getInventory().items.get(lockedSlot);
        if (ChamberTaczEnhancement.kind(lockedStack) == kind) {
            return true;
        }
        ItemStack carried = player.containerMenu.getCarried();
        if (ChamberTaczEnhancement.kind(carried) == kind) {
            player.getInventory().items.set(lockedSlot, carried);
            player.containerMenu.setCarried(lockedStack.isEmpty() ? ItemStack.EMPTY : lockedStack);
            player.getInventory().setChanged();
            player.containerMenu.broadcastChanges();
            return true;
        }
        int foundSlot = findInventoryGunSlot(player, kind);
        if (foundSlot < 0) {
            return false;
        }
        ItemStack foundStack = player.getInventory().items.get(foundSlot);
        player.getInventory().items.set(foundSlot, lockedStack);
        player.getInventory().items.set(lockedSlot, foundStack);
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        return true;
    }

    private static void purgeDuplicateChamberGuns(ServerPlayer player, ChamberGunKind kind, int lockedSlot) {
        boolean changed = false;
        for (int slot = 0; slot < player.getInventory().items.size(); slot++) {
            ItemStack stack = player.getInventory().items.get(slot);
            ChamberGunKind slotKind = ChamberTaczEnhancement.kind(stack);
            if (slotKind != ChamberGunKind.NONE && (slot != lockedSlot || slotKind != kind)) {
                player.getInventory().items.set(slot, ItemStack.EMPTY);
                changed = true;
            }
        }
        ItemStack carried = player.containerMenu.getCarried();
        if (ChamberTaczEnhancement.kind(carried) != ChamberGunKind.NONE) {
            if (isHotbarSlot(lockedSlot)
                    && ChamberTaczEnhancement.kind(player.getInventory().items.get(lockedSlot)) != kind
                    && ChamberTaczEnhancement.kind(carried) == kind) {
                player.getInventory().items.set(lockedSlot, carried.copy());
            }
            player.containerMenu.setCarried(ItemStack.EMPTY);
            changed = true;
        }
        if (changed) {
            player.getInventory().setChanged();
            player.containerMenu.broadcastChanges();
        }
    }

    private static void removeAllChamberGuns(ServerPlayer player) {
        boolean changed = false;
        for (int slot = 0; slot < player.getInventory().items.size(); slot++) {
            if (ChamberTaczEnhancement.kind(player.getInventory().items.get(slot)) != ChamberGunKind.NONE) {
                player.getInventory().items.set(slot, ItemStack.EMPTY);
                changed = true;
            }
        }
        if (ChamberTaczEnhancement.kind(player.containerMenu.getCarried()) != ChamberGunKind.NONE) {
            player.containerMenu.setCarried(ItemStack.EMPTY);
            changed = true;
        }
        if (changed) {
            player.getInventory().setChanged();
            player.containerMenu.broadcastChanges();
        }
    }

    private static int findEmptyHotbarSlot(ServerPlayer player) {
        for (int slot = 0; slot < 9; slot++) {
            if (player.getInventory().items.get(slot).isEmpty()) {
                return slot;
            }
        }
        return -1;
    }

    private static int placeInEmptyHotbarSlot(ServerPlayer player, int slot, ItemStack stack) {
        if (!isHotbarSlot(slot) || !player.getInventory().items.get(slot).isEmpty()) {
            return -1;
        }
        player.getInventory().items.set(slot, stack.copy());
        return slot;
    }

    private static boolean isHotbarSlot(int slot) {
        return slot >= 0 && slot < 9;
    }

    private static void setCooldown(ServerPlayer player, String key, int ticks) {
        data(player).putLong(key, SkillCooldownHelper.until(player, SkillCooldownHelper.now(player), ticks));
    }

    private static int remainingTicks(Player player, String key) {
        return SkillCooldownHelper.remainingTicks(player, data(player).getLong(key));
    }

    private static AABB markerSearchBox(ServerPlayer player) {
        return player.getBoundingBox().inflate(MARKER_SEARCH_RANGE);
    }

    private static CompoundTag data(Player player) {
        CompoundTag persistent = player.getPersistentData();
        if (!persistent.contains(ROOT_TAG, Tag.TAG_COMPOUND)) {
            persistent.put(ROOT_TAG, new CompoundTag());
        }
        CompoundTag tag = persistent.getCompound(ROOT_TAG);
        if (player instanceof ServerPlayer) {
            SkillCooldownHelper.clampCooldown(player, tag, TELEPORT_COOLDOWN_UNTIL, TELEPORT_COOLDOWN_TICKS);
            SkillCooldownHelper.clampCooldown(player, tag, TRAP_COOLDOWN_UNTIL, TRAP_COOLDOWN_TICKS);
            SkillCooldownHelper.clampCooldown(player, tag, HEADHUNTER_COOLDOWN_UNTIL, HEADHUNTER_COOLDOWN_TICKS);
            SkillCooldownHelper.clampCooldown(player, tag, TOUR_DE_FORCE_COOLDOWN_UNTIL, TOUR_DE_FORCE_COOLDOWN_TICKS);
            SkillCooldownHelper.clampCooldown(player, tag, TOUR_SLOW_FIELD_COOLDOWN_UNTIL, TOUR_SLOW_FIELD_TRIGGER_INTERVAL_TICKS);
        }
        return tag;
    }
}
