package com.rzy.dealt_force_skills.raid;

import com.rzy.dealt_force_skills.raid.assets.RaidItemCodec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import java.util.List;
import java.util.UUID;

/** Stationary living proxy. Full non-equipment inventory remains escrowed, never a vanilla loot source. */
public final class RaidOfflineBodyEntity extends PathfinderMob {
    private UUID instance, player;
    private CompoundTag actual = new CompoundTag(), state = new CompoundTag();
    private Vec3 anchor = Vec3.ZERO;
    private int selected;
    private boolean anchored;
    public RaidOfflineBodyEntity(EntityType<? extends RaidOfflineBodyEntity> type, Level level) {
        super(type, level); setNoAi(true); setNoGravity(true); setPersistenceRequired();
        for (EquipmentSlot slot : EquipmentSlot.values()) setDropChance(slot, 0);
    }
    public static AttributeSupplier.Builder attributes() {
        return Player.createAttributes().add(Attributes.FOLLOW_RANGE, 0).add(Attributes.ATTACK_DAMAGE, 1);
    }
    public UUID instanceId() { return instance; }
    public UUID playerId() { return player; }
    void initialize(UUID instance, RaidMatchLedger.Participant participant) {
        this.instance = instance; this.player = participant.id; this.state = participant.bodyState.copy();
        this.actual = participant.actual.copy(); this.selected = Math.max(0, Math.min(8, state.getInt("Selected")));
        anchor = participant.position; anchored = true;
        moveTo(anchor.x, anchor.y, anchor.z, participant.yaw, participant.pitch);
        setCustomName(Component.literal(participant.name + "（离线）")); setCustomNameVisible(true);
        RaidActors.restoreLife(this, state);
        applyEquipment();
    }
    void applyEquipment() {
        if (!(level() instanceof ServerLevel level) || actual.isEmpty()) return;
        List<ItemStack> inventory = RaidItemCodec.readList(level.getServer(), actual.getList("Inventory", Tag.TAG_COMPOUND), 41);
        setItemSlot(EquipmentSlot.MAINHAND, inventory.get(selected).copy());
        setItemSlot(EquipmentSlot.OFFHAND, inventory.get(40).copy());
        setItemSlot(EquipmentSlot.FEET, inventory.get(36).copy()); setItemSlot(EquipmentSlot.LEGS, inventory.get(37).copy());
        setItemSlot(EquipmentSlot.CHEST, inventory.get(38).copy()); setItemSlot(EquipmentSlot.HEAD, inventory.get(39).copy());
    }
    public CompoundTag actual() {
        if (!(level() instanceof ServerLevel level)) return actual.copy();
        List<ItemStack> inventory = RaidItemCodec.readList(level.getServer(), actual.getList("Inventory", Tag.TAG_COMPOUND), 41);
        inventory.set(selected, getMainHandItem().copy()); inventory.set(40, getOffhandItem().copy());
        inventory.set(36, getItemBySlot(EquipmentSlot.FEET).copy()); inventory.set(37, getItemBySlot(EquipmentSlot.LEGS).copy());
        inventory.set(38, getItemBySlot(EquipmentSlot.CHEST).copy()); inventory.set(39, getItemBySlot(EquipmentSlot.HEAD).copy());
        actual.put("Inventory", RaidItemCodec.writeList(level.getServer(), inventory)); return actual.copy();
    }
    CompoundTag bodyState() {
        CompoundTag latest = RaidActors.captureLife(this);
        // Player-specific state has no living-mob equivalent and remains in the durable proxy record.
        for (String key : new String[]{"Attributes", "Selected", "Food", "NativeDowned", "DownKind", "GenericDowned", "ReviveProgress",
                "FinalDeathEffectsClaimed", "FinalDeathSource", "FinalDeathAttacker"})
            if (state.contains(key)) latest.put(key, state.get(key).copy());
        return latest;
    }
    void remember(CompoundTag latest) { state = latest.copy(); }
    public boolean tryDown(DamageSource source) { return RaidActors.tryBodyDown(this, source); }
    @Override public void tick() {
        if (anchored) { setPos(anchor); setDeltaMovement(Vec3.ZERO); }
        super.tick();
        if (anchored) { setPos(anchor); setDeltaMovement(Vec3.ZERO); }
        if (!level().isClientSide) setPose(state.getBoolean("GenericDowned") ? Pose.SWIMMING : Pose.STANDING);
    }
    @Override public void travel(Vec3 input) { setDeltaMovement(Vec3.ZERO); }
    @Override public void push(Entity entity) { }
    @Override public void push(double x, double y, double z) { }
    @Override public boolean isPushable() { return false; }
    @Override protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer helper && hand == InteractionHand.MAIN_HAND) {
            return RaidActors.help(helper, this) ? InteractionResult.CONSUME : InteractionResult.PASS;
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }
    @Override protected void dropAllDeathLoot(DamageSource source) { }
    @Override protected void hurtArmor(DamageSource source, float amount) {
        if (source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR)) return;
        int wear = Math.max(1, (int) (amount / 4));
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD}) {
            ItemStack item = getItemBySlot(slot);
            if (!item.isEmpty()) item.hurtAndBreak(wear, this, actor -> actor.broadcastBreakEvent(slot));
        }
    }
    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (instance != null) tag.putUUID("RaidInstance", instance);
        if (player != null) tag.putUUID("RaidPlayer", player);
        tag.put("RaidActual", actual()); tag.put("RaidBodyState", bodyState()); tag.putInt("RaidSelected", selected);
        tag.putDouble("AnchorX", anchor.x); tag.putDouble("AnchorY", anchor.y); tag.putDouble("AnchorZ", anchor.z);
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        instance = tag.hasUUID("RaidInstance") ? tag.getUUID("RaidInstance") : null;
        player = tag.hasUUID("RaidPlayer") ? tag.getUUID("RaidPlayer") : null;
        actual = tag.getCompound("RaidActual").copy(); state = tag.getCompound("RaidBodyState").copy();
        selected = Math.max(0, Math.min(8, tag.getInt("RaidSelected")));
        anchor = new Vec3(tag.getDouble("AnchorX"), tag.getDouble("AnchorY"), tag.getDouble("AnchorZ")); anchored = true;
        setNoAi(true); setNoGravity(true); setPersistenceRequired();
        RaidActors.restoreLife(this, state);
        applyEquipment();
    }
}
