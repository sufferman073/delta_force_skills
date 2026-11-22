package com.rzy.dealt_force_skills.entity;

import com.rzy.dealt_force_skills.character.saeed.AsaraTaczEquipment;
import com.rzy.dealt_force_skills.character.saeed.SaeedGuardType;
import com.rzy.dealt_force_skills.character.saeed.SaeedStateManager;
import com.rzy.dealt_force_skills.compat.TamarindCompatEvents;
import com.rzy.dealt_force_skills.registry.ModItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

public final class AsaraTrooperEntity extends SaeedGuardEntity {
    private static final String DROPPED_LOOT = "AsaraTrooperDroppedLoot";
    private final AsaraTrooperRole role;

    public AsaraTrooperEntity(EntityType<? extends SaeedGuardEntity> type, Level level, AsaraTrooperRole role) {
        super(type, level);
        this.role = role;
        configureStandalone(role.guardType());
        setItemSlot(EquipmentSlot.MAINHAND, AsaraTaczEquipment.randomGun(role, random));
        if (role == AsaraTrooperRole.GUARD_SOLDIER) {
            setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        }
    }

    @Override
    protected boolean isStandaloneTrooper() {
        return true;
    }

    public AsaraTrooperRole role() {
        return role;
    }

    @Override
    protected double standaloneAttributeMultiplier() {
        return role.attributeMultiplier();
    }

    @Override
    protected void applyStandaloneAttributes(SaeedGuardType type) {
        super.applyStandaloneAttributes(type);
        if (getAttribute(Attributes.MOVEMENT_SPEED) != null) {
            getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(type.speed() * role.speedMultiplier());
        }
        if (getAttribute(Attributes.FOLLOW_RANGE) != null) {
            getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(
                    Math.max(35.0D, type.targetRange() + 6.0D) * role.targetRangeMultiplier());
        }
    }

    public float outgoingDamageMultiplier() {
        return 0.6F * (float) role.damageMultiplier();
    }

    public static AsaraTrooperEntity sourceTrooper(DamageSource source) {
        if (source == null) {
            return null;
        }
        if (source.getEntity() instanceof AsaraTrooperEntity trooper) {
            return trooper;
        }
        return source.getDirectEntity() instanceof AsaraTrooperEntity trooper ? trooper : null;
    }

    public static boolean isAsaraAlly(Entity entity) {
        if (entity instanceof AsaraTrooperEntity || entity instanceof BeaconBossEntity) {
            return true;
        }
        if (entity instanceof SaeedGuardEntity guard) {
            return !guard.isStandaloneTrooper();
        }
        return entity instanceof Player player && SaeedStateManager.isSaeed(player);
    }

    public static boolean sourceIsAsara(DamageSource source) {
        return source != null && (source.getEntity() instanceof AsaraTrooperEntity
                || source.getDirectEntity() instanceof AsaraTrooperEntity);
    }

    public static boolean shouldCancelFriendlyDamage(Entity target, DamageSource source) {
        if (source == null) {
            return false;
        }
        Entity attacker = source.getEntity() != null ? source.getEntity() : source.getDirectEntity();
        Boolean raid = attacker == null ? null : com.rzy.dealt_force_skills.raid.ai.RaidAiAffinity.hostility(com.rzy.dealt_force_skills.raid.ai.RaidAiAffinity.identity(attacker), target);
        if (raid != null) return !raid;
        return sourceIsAsara(source) && isAsaraAlly(target)
                || target instanceof AsaraTrooperEntity
                && (isAsaraAlly(source.getEntity()) || isAsaraAlly(source.getDirectEntity()));
    }

    @Override
    public boolean isAlliedTo(Entity entity) {
        if (com.rzy.dealt_force_skills.raid.ai.RaidAiAffinity.instance(this) != null || com.rzy.dealt_force_skills.raid.ai.RaidAiAffinity.instance(entity) != null) return com.rzy.dealt_force_skills.raid.ai.RaidAiAffinity.allies(this, entity);
        return isAsaraAlly(entity) || super.isAlliedTo(entity);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        Entity attacker = source.getEntity() != null ? source.getEntity() : source.getDirectEntity();
        Boolean raid = attacker == null ? null : com.rzy.dealt_force_skills.raid.ai.RaidAiAffinity.hostility(com.rzy.dealt_force_skills.raid.ai.RaidAiAffinity.identity(attacker), this);
        if (raid != null) return raid && super.hurt(source, amount);
        if (isAsaraAlly(source.getEntity()) || isAsaraAlly(source.getDirectEntity())) {
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    protected void dropStandaloneDeathLoot() {
        if (!(level() instanceof ServerLevel serverLevel) || getPersistentData().getBoolean(DROPPED_LOOT)) {
            return;
        }
        getPersistentData().putBoolean(DROPPED_LOOT, true);
        ItemStack weapon = getMainHandItem().copy();
        if (!weapon.isEmpty()) {
            weapon.setCount(1);
            AsaraTaczEquipment.clearReserveAmmo(weapon);
            spawnAtLocation(weapon);
            for (ItemStack ammo : AsaraTaczEquipment.matchingAmmo(weapon, 30 + random.nextInt(61))) {
                spawnAtLocation(ammo);
            }
        }
        switch (guardType()) {
            case FIREEYE -> dropRepairKits();
            case IRON_RAIN -> spawnAtLocation(new ItemStack(ModItems.MASK1_IRON_WALL_HELMET.get()));
            case THUNDER -> {
                spawnAtLocation(new ItemStack(ModItems.D6_TACTICAL_HELMET.get()));
                spawnAtLocation(new ItemStack(ModItems.SAMURAI_BALLISTIC_VEST.get()));
            }
            default -> {
            }
        }
        for (ItemStack stack : TamarindCompatEvents.createBirdNestLoot(serverLevel, 1, 4)) {
            spawnAtLocation(stack);
        }
    }

    private void dropRepairKits() {
        List<Item> kits = List.of(
                ModItems.STANDARD_ARMOR_REPAIR_KIT.get(),
                ModItems.STANDARD_HELMET_REPAIR_KIT.get(),
                ModItems.PRECISION_ARMOR_REPAIR_KIT.get(),
                ModItems.PRECISION_HELMET_REPAIR_KIT.get(),
                ModItems.ADVANCED_ARMOR_REPAIR_KIT.get(),
                ModItems.ADVANCED_HELMET_REPAIR_KIT.get());
        int count = 1 + random.nextInt(2);
        for (int i = 0; i < count; i++) {
            spawnAtLocation(new ItemStack(kits.get(random.nextInt(kits.size()))));
        }
    }

}
