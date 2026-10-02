package com.rzy.dealt_force_skills.arena;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.gambler.GamblerArenaManager;
import com.rzy.dealt_force_skills.character.ntwo.NTwoStateManager;
import com.rzy.dealt_force_skills.stamina.StaminaManager;
import com.rzy.dealt_force_skills.effect.InjuryManager;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.S2C_HvkAsaraArenaStatus;
import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID)
public final class HvkAsaraArenaManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String INSTANCE_X = "HvkAsaraArenaX";
    private static final String INSTANCE_Z = "HvkAsaraArenaZ";
    private static final String INSTANCE_STATE = "HvkAsaraArenaState";
    private static final String ACTIVE_RETURN = "HvkAsaraArenaReturn";
    private static final String MOB_SESSION = "HvkAsaraArenaSession";
    private static final String SUPPORT_TRIGGERED = "HvkAsaraSupportTriggered";
    private static final String QUICK_STRIKE_USED = "HvkAsaraQuickStrikeUsed";
    private static final String LAST_STAND_AT = "HvkAsaraLastStandAt";
    private static final String ABSORPTION_AT = "HvkAsaraAbsorptionAt";
    private static final String BLINK_AT = "HvkAsaraBlinkAt";
    private static final ResourceLocation WOODLAND_MANSION = ResourceLocation.parse("minecraft:woodland_mansion");
    private static final ResourceLocation STRONGHOLD = ResourceLocation.parse("minecraft:stronghold");
    private static final ResourceLocation NETHER_FORTRESS = ResourceLocation.parse("minecraft:fortress");
    private static final ResourceLocation NETHER_EFFECTS = ResourceLocation.parse("minecraft:the_nether");
    private static final ResourceLocation END_EFFECTS = ResourceLocation.parse("minecraft:the_end");
    private static final ResourceKey<Level> OVERWORLD_ARENA = arenaKey("hvk_asara_arena_overworld");
    private static final ResourceKey<Level> NETHER_ARENA = arenaKey("hvk_asara_arena_nether");
    private static final ResourceKey<Level> END_ARENA = arenaKey("hvk_asara_arena_end");
    private static final ResourceLocation ARMOR_EFFECT_ID = ResourceLocation.fromNamespaceAndPath(
            DealtForceSkillsMod.MODID, "hvk_asara_armor_effect");
    private static final ResourceLocation BLADE_REACH_ID = ResourceLocation.fromNamespaceAndPath(
            DealtForceSkillsMod.MODID, "hvk_asara_blade_reach");
    private static final ResourceLocation ARMOR_FLOOR_ID = ResourceLocation.fromNamespaceAndPath(
            DealtForceSkillsMod.MODID, "hvk_asara_armor_floor");
    private static final ResourceLocation TOUGHNESS_FLOOR_ID = ResourceLocation.fromNamespaceAndPath(
            DealtForceSkillsMod.MODID, "hvk_asara_toughness_floor");
    private static final ResourceLocation MARCH_SPEED_ID = ResourceLocation.fromNamespaceAndPath(
            DealtForceSkillsMod.MODID, "hvk_asara_march_speed");
    private static final ResourceLocation GUARD_HEALTH_ID = ResourceLocation.fromNamespaceAndPath(
            DealtForceSkillsMod.MODID, "hvk_asara_guard_health");
    private static final ResourceLocation TRAINED_HEALTH_ID = ResourceLocation.fromNamespaceAndPath(
            DealtForceSkillsMod.MODID, "hvk_asara_trained_health");
    private static final ResourceLocation TRAINED_DAMAGE_ID = ResourceLocation.fromNamespaceAndPath(
            DealtForceSkillsMod.MODID, "hvk_asara_trained_damage");
    private static final ResourceLocation TRAINED_SPEED_ID = ResourceLocation.fromNamespaceAndPath(
            DealtForceSkillsMod.MODID, "hvk_asara_trained_speed");
    private static final int INSTANCE_RADIUS = 512;
    private static final int INSTANCE_SPACING = 1152;
    private static final int INSTANCE_Y = 80;
    private static final int ARRIVAL_DISTANCE = 64;
    private static final int ARRIVAL_HALF_SIZE = 6;
    private static final int ACCESS_HALF_WIDTH = 2;
    private static final int TERRAIN_MARGIN = 8;
    private static final int TERRAIN_DEPTH = 12;
    private static final int WATER_DEPTH = 12;
    private static final int MAX_STRUCTURE_CHUNKS = 4096;
    private static final int MAX_TERRAIN_BLOCKS = 6_000_000;
    private static final int MAX_RESET_BLOCKS = 16_000_000;
    private static final int STRUCTURE_PREVIEW_PROBES = 8_192;
    private static final int STRUCTURE_PREVIEW_RADIUS_CHUNKS = 2_048;
    private static final int STRUCTURE_PLACEMENT_CHECKS_PER_TICK = 8_192;
    private static final int SPAWN_FLOOR_PROBES = 96;
    private static final int MIN_CHALLENGE_HOSTILES = 20;
    private static final int MAX_CHALLENGE_HOSTILES = 40;
    private static final int NATURAL_SPAWN_TICKS = 30 * 20;
    private static final int SUCCESS_PACK_TICKS = 60 * 20;
    private static final int BUILD_BLOCKS_PER_TICK = 16_384;
    private static final int BUILD_CHUNKS_PER_TICK = 2;
    private static final int BUILD_ENTITY_CHUNKS_PER_TICK = 64;
    private static final long BUILD_TICK_BUDGET_NANOS = 4_000_000L;
    private static final int MAX_BUILD_TICKS = 10 * 60 * 20;
    private static final int HUD_SYNC_INTERVAL_TICKS = 10;
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();
    private static final Map<UUID, BuildJob> PENDING_BUILDS = new LinkedHashMap<>();
    private static final Map<UUID, ReturnPoint> PENDING_RETURNS = new HashMap<>();

    private HvkAsaraArenaManager() {
    }

    public static boolean start(ServerPlayer player, ResourceLocation structureId, long requestedMask) {
        try {
            return startInternal(player, structureId, requestedMask);
        } catch (RuntimeException exception) {
            LOGGER.error("Failed to prepare HVK Asara arena structure {} for {}",
                    structureId, player.getGameProfile().getName(), exception);
            player.displayClientMessage(Component.translatable(
                    "message.dealt_force_skills.hvk_asara_arena.place_failed", structureId.toString()), false);
            return false;
        }
    }

    private static boolean startInternal(ServerPlayer player, ResourceLocation structureId, long requestedMask) {
        if (SESSIONS.containsKey(player.getUUID()) || PENDING_BUILDS.containsKey(player.getUUID())) {
            player.displayClientMessage(Component.translatable(
                    "message.dealt_force_skills.hvk_asara_arena.already_active"), true);
            return false;
        }
        Registry<Structure> structureRegistry = player.server.registryAccess().registryOrThrow(Registries.STRUCTURE);
        Structure structure = structureRegistry.get(structureId);
        if (structure == null) {
            return false;
        }
        long mask = HvkAsaraArenaModifier.sanitize(requestedMask);
        BlockPos origin = instanceOrigin(player);
        ReturnPoint returnPoint = new ReturnPoint(player.level().dimension(), player.getX(), player.getY(), player.getZ(),
                player.getYRot(), player.getXRot());
        PENDING_BUILDS.put(player.getUUID(), new BuildJob(player.getUUID(), structureId, structure, mask,
                origin, returnPoint));
        player.displayClientMessage(Component.translatable(
                "message.dealt_force_skills.hvk_asara_arena.preparing"), false);
        return true;
    }

    public static boolean blocksExperienceGrowth(ServerPlayer player) {
        return player != null && SESSIONS.containsKey(player.getUUID());
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        Session session = SESSIONS.get(player.getUUID());
        if (session == null) {
            return;
        }
        ServerLevel arena = player.server.getLevel(session.arenaDimension);
        if (arena == null || player.level() != arena) {
            fail(player, session, "message.dealt_force_skills.hvk_asara_arena.failed");
            return;
        }
        long now = arena.getGameTime();
        if (!inside(session, player)) {
            player.teleportTo(arena, session.arrival.getX() + 0.5D, session.arrival.getY() + 0.1D,
                    session.arrival.getZ() + 0.5D, session.arrivalYaw, 0.0F);
        }
        applyPlayerModifiers(player, session, now);
        spawnExpectedStructureHostiles(arena, session, now);
        spawnNaturalHostiles(arena, session, now);
        List<LivingEntity> living = arena.getEntitiesOfClass(LivingEntity.class, bounds(session), LivingEntity::isAlive);
        for (LivingEntity entity : living) {
            entity.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40, 0, false, false, false));
            if (entity != player) {
                markAndApplyMob(entity, session);
            }
        }
        List<LivingEntity> activeHostiles = living.stream().filter(entity -> entity != player && isSessionHostile(entity, session))
                .sorted(java.util.Comparator.comparingDouble(player::distanceToSqr)).toList();
        syncArenaStatus(player, session, activeHostiles, now);

        if (session.successReturnAt > 0L) {
            if (now >= session.successReturnAt) {
                finishReturn(player, session);
            }
            return;
        }
        if (now >= session.spawnEndAt && !session.challengeEligibilityReported) {
            session.challengeEligibilityReported = true;
            session.challengeEligible = session.challengeHostiles.size() >= MIN_CHALLENGE_HOSTILES;
            if (!session.challengeEligible) {
                player.displayClientMessage(Component.translatable(
                        "message.dealt_force_skills.hvk_asara_arena.not_challenge",
                        session.challengeHostiles.size(), MIN_CHALLENGE_HOSTILES), false);
            }
        }
        if (now >= session.timeoutAt) {
            if (session.challengeEligibilityReported && !session.challengeEligible) {
                complete(arena, player, session, now);
            } else {
                fail(player, session, "message.dealt_force_skills.hvk_asara_arena.timeout");
            }
            return;
        }
        if (now >= session.spawnEndAt && activeHostiles.isEmpty()) {
            complete(arena, player, session, now);
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (event.getServer() != null) tickPendingBuilds(event.getServer());
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel) || !(event.getEntity() instanceof LivingEntity living)) {
            return;
        }
        Session session = sessionAt(living);
        if (session != null && !living.getUUID().equals(session.playerId)) {
            if (living instanceof Mob && (living.level().getGameTime() >= session.spawnEndAt
                    || isHostile(living) && session.challengeHostiles.size() >= MAX_CHALLENGE_HOSTILES)
                    && (!living.getPersistentData().hasUUID(MOB_SESSION)
                    || !living.getPersistentData().getUUID(MOB_SESSION).equals(session.id))) {
                event.setCanceled(true);
                return;
            }
            markAndApplyMob(living, session);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingHurt(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        Session session = sessionAt(target);
        if (session == null) {
            return;
        }
        Set<HvkAsaraArenaModifier> selected = session.modifiers;
        Entity attacker = event.getSource().getEntity();
        boolean targetIsPlayer = target.getUUID().equals(session.playerId);

        if (targetIsPlayer) {
            if (selected.contains(HvkAsaraArenaModifier.FROZEN_WORLD)) {
                NTwoStateManager.addAmbientCold((ServerPlayer) target, 8);
            }
            int brokenArmor = HvkAsaraArenaModifier.tier(selected, "broken_armor");
            if (brokenArmor > 0) {
                event.setAmount(event.getAmount() * (1.0F + brokenArmor * 0.3F));
            }
            if (selected.contains(HvkAsaraArenaModifier.HIDDEN_EXTREME) && event.getAmount() > 0.0F) {
                event.setAmount(Float.MAX_VALUE);
            }
            return;
        }

        if (selected.contains(HvkAsaraArenaModifier.REFLECTION_FIELD)
                && !isAllowedReflectionDamage(event.getSource())) {
            event.setCanceled(true);
            event.setAmount(0.0F);
            return;
        }
        int march = HvkAsaraArenaModifier.tier(selected, "march");
        float dodgeChance = march == 1 ? 0.10F : march == 2 ? 0.15F : march == 3 ? 0.30F : 0.0F;
        if (dodgeChance > 0.0F && target.getRandom().nextFloat() < dodgeChance) {
            event.setCanceled(true);
            event.setAmount(0.0F);
            return;
        }
        if (selected.contains(HvkAsaraArenaModifier.FORGED_ARMOR) && hasEquippedArmor(target)) {
            event.setAmount(event.getAmount() * 0.5F);
        }
        int guard = HvkAsaraArenaModifier.tier(selected, "guard");
        if (guard > 0) {
            event.setAmount(event.getAmount() * (guard == 1 ? 0.925F : guard == 2 ? 0.85F : 0.775F));
        }
        long now = target.level().getGameTime();
        CompoundTag data = target.getPersistentData();
        if (attacker instanceof ServerPlayer player
                && selected.contains(HvkAsaraArenaModifier.FROZEN_WORLD)) {
            NTwoStateManager.addAmbientCold(player, 8);
        }
        int strongBow = HvkAsaraArenaModifier.tier(selected, "strong_bow");
        if (strongBow > 0 && isBowOrCrossbowUser(target)
                && attacker instanceof ServerPlayer player && !isMeleeDamage(event.getSource())) {
            float ratio = strongBow == 1 ? 0.125F : strongBow == 2 ? 0.25F : 0.50F;
            player.hurt(target.damageSources().thorns(target), event.getAmount() * ratio);
        }
        if (selected.contains(HvkAsaraArenaModifier.UNBREAKABLE)
                && now >= data.getLong(ABSORPTION_AT)) {
            target.setAbsorptionAmount(Math.max(target.getAbsorptionAmount(), target.getMaxHealth() * 0.5F));
            data.putLong(ABSORPTION_AT, now + 60L * 20L);
        }
        if (selected.contains(HvkAsaraArenaModifier.LAST_STAND)
                && event.getAmount() >= target.getHealth() && now >= data.getLong(LAST_STAND_AT)) {
            event.setCanceled(true);
            event.setAmount(0.0F);
            target.setHealth(target.getMaxHealth());
            data.putLong(LAST_STAND_AT, now + 600L * 20L);
            return;
        }
        if (selected.contains(HvkAsaraArenaModifier.BLINK) && attacker instanceof LivingEntity livingAttacker
                && now >= data.getLong(BLINK_AT)) {
            teleportNear(target, livingAttacker);
            data.putLong(BLINK_AT, now + 15L * 20L);
        }
        int support = HvkAsaraArenaModifier.tier(selected, "support");
        if (support > 0 && target instanceof Mob mob && !data.getBoolean(SUPPORT_TRIGGERED)) {
            data.putBoolean(SUPPORT_TRIGGERED, true);
            spawnCopies(mob, session, support == 1 ? 1 : 3);
            if (attacker instanceof LivingEntity livingAttacker) {
                forceTarget(target, livingAttacker, support == 1 ? 30.0D : 60.0D);
            }
        }
        if (selected.contains(HvkAsaraArenaModifier.TRAINED) && attacker instanceof LivingEntity livingAttacker) {
            forceTarget(target, livingAttacker, 75.0D);
        }
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        Session session = sessionAt(event.getEntity());
        if (session == null || event.getEntity().getUUID().equals(session.playerId)) {
            return;
        }
        int copies = lootCopies(session.score);
        if (copies <= 0) {
            return;
        }
        List<ItemEntity> extra = new ArrayList<>();
        for (ItemEntity drop : event.getDrops()) {
            for (int i = 0; i < copies; i++) {
                extra.add(new ItemEntity(drop.level(), drop.getX(), drop.getY(), drop.getZ(), drop.getItem().copy()));
            }
        }
        event.getDrops().addAll(extra);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        Session session = SESSIONS.get(player.getUUID());
        if (session != null) {
            player.level().playSound(null, player.blockPosition(), ModSounds.HVK_ASARA_ARENA_FAILURE.get(),
                    SoundSource.BLOCKS, 1.0F, 1.0F);
            SESSIONS.remove(player.getUUID());
            PENDING_RETURNS.put(player.getUUID(), session.returnPoint);
            NetworkHandler.sendToPlayer(S2C_HvkAsaraArenaStatus.inactive(), player);
            clearNegativeEffects(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ReturnPoint point = PENDING_RETURNS.remove(player.getUUID());
        if (point == null) {
            return;
        }
        ServerLevel returnLevel = player.server.getLevel(point.dimension);
        if (returnLevel == null) {
            returnLevel = player.server.overworld();
        }
        player.teleportTo(returnLevel, point.x, point.y, point.z, point.yRot, point.xRot);
        player.getPersistentData().remove(ACTIVE_RETURN);
        player.displayClientMessage(Component.translatable(
                "message.dealt_force_skills.hvk_asara_arena.failed"), false);
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Session session = SESSIONS.get(player.getUUID());
        if (session != null) {
            fail(player, session, "message.dealt_force_skills.hvk_asara_arena.failed");
            player.getPersistentData().put(ACTIVE_RETURN, session.returnPoint.save());
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ReturnPoint point = loadActiveReturn(player);
        if (point == null) return;
        teleportToReturn(player, point);
        player.getPersistentData().remove(ACTIVE_RETURN);
        player.displayClientMessage(Component.translatable(
                "message.dealt_force_skills.hvk_asara_arena.failed"), false);
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        SESSIONS.clear();
        PENDING_BUILDS.clear();
        PENDING_RETURNS.clear();
    }

    private static void tickPendingBuilds(MinecraftServer server) {
        if (PENDING_BUILDS.isEmpty()) return;
        BuildJob job = PENDING_BUILDS.values().iterator().next();
        ServerPlayer player = server.getPlayerList().getPlayer(job.playerId);
        if (player == null) job.cancel();
        try {
            if (job.tick(server, player)) PENDING_BUILDS.remove(job.playerId);
        } catch (RuntimeException exception) {
            LOGGER.error("Failed to build HVK Asara arena structure {} for {}",
                    job.structureId, player == null ? job.playerId : player.getGameProfile().getName(), exception);
            job.rollbackPlayer(server, player);
            if (player != null) player.displayClientMessage(Component.translatable(
                    "message.dealt_force_skills.hvk_asara_arena.place_failed", job.structureId.toString()), false);
            if (!job.beginFailureCleanup()) PENDING_BUILDS.remove(job.playerId);
        }
    }

    private static void markAndApplyMob(LivingEntity entity, Session session) {
        CompoundTag data = entity.getPersistentData();
        if (data.hasUUID(MOB_SESSION) && data.getUUID(MOB_SESSION).equals(session.id)) {
            return;
        }
        data.putUUID(MOB_SESSION, session.id);
        if (entity.level().getGameTime() <= session.spawnEndAt && isHostile(entity)) {
            session.challengeHostiles.add(entity.getUUID());
        }
        Set<HvkAsaraArenaModifier> selected = session.modifiers;
        int armor = HvkAsaraArenaModifier.tier(selected, "armor");
        if (armor > 0) {
            equipArmor(entity, armor);
            applyAttributeFloorModifier(entity.getAttribute(Attributes.ARMOR), ARMOR_FLOOR_ID,
                    6.0D + armor * 2.0D);
            applyAttributeFloorModifier(entity.getAttribute(Attributes.ARMOR_TOUGHNESS), TOUGHNESS_FLOOR_ID,
                    armor * 2.0D);
            applyAttributeModifier(entity.getAttribute(Attributes.ARMOR), ARMOR_EFFECT_ID,
                    "hvk_asara_armor_effect", armor * 0.5D);
        }
        int blade = HvkAsaraArenaModifier.tier(selected, "blade");
        if (blade > 0 && !isBowOrCrossbowUser(entity)) {
            entity.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(blade == 1 ? Items.IRON_AXE
                    : blade == 2 ? Items.DIAMOND_AXE : Items.NETHERITE_AXE));
        }
        if (blade > 0) {
            applyAttributeModifier(entity.getAttribute(Attributes.ENTITY_INTERACTION_RANGE), BLADE_REACH_ID,
                    "hvk_asara_blade_reach", blade);
        }
        int march = HvkAsaraArenaModifier.tier(selected, "march");
        if (march > 0) {
            applyAttributeModifier(entity.getAttribute(Attributes.MOVEMENT_SPEED), MARCH_SPEED_ID,
                    "hvk_asara_march_speed", march * 0.25D);
        }
        int guard = HvkAsaraArenaModifier.tier(selected, "guard");
        if (guard > 0) {
            double multiplier = guard == 1 ? 2.0D : guard == 2 ? 3.5D : 7.0D;
            applyAttributeModifier(entity.getAttribute(Attributes.MAX_HEALTH), GUARD_HEALTH_ID,
                    "hvk_asara_guard_health", multiplier - 1.0D);
            entity.setHealth(entity.getMaxHealth());
        }
        if (selected.contains(HvkAsaraArenaModifier.TRAINED)) {
            applyAttributeModifier(entity.getAttribute(Attributes.MAX_HEALTH), TRAINED_HEALTH_ID,
                    "hvk_asara_trained_health", 0.25D);
            applyAttributeModifier(entity.getAttribute(Attributes.ATTACK_DAMAGE), TRAINED_DAMAGE_ID,
                    "hvk_asara_trained_damage", 0.25D);
            applyAttributeModifier(entity.getAttribute(Attributes.MOVEMENT_SPEED), TRAINED_SPEED_ID,
                    "hvk_asara_trained_speed", 0.25D);
            entity.setHealth(entity.getMaxHealth());
        }
        if (selected.contains(HvkAsaraArenaModifier.INVISIBLE)) {
            entity.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, Integer.MAX_VALUE, 0, false, false, false));
        }
        int magic = HvkAsaraArenaModifier.tier(selected, "magic");
        if (magic > 0) {
            enchantEquipment(entity, magic == 1 ? 1 : magic == 2 ? 4 : 7,
                    selected.contains(HvkAsaraArenaModifier.FULLY_ARMED) ? 6 : magic == 1 ? 1 : magic == 2 ? 3 : 5);
        }
    }

    public static float outgoingDamageMultiplier(LivingEntity attacker, LivingEntity target, DamageSource source) {
        Session session = sessionAt(attacker);
        if (session == null || attacker.getUUID().equals(session.playerId)) {
            return playerOutgoingMultiplier(attacker, target, session);
        }
        float multiplier = 1.0F;
        int strongBow = HvkAsaraArenaModifier.tier(session.modifiers, "strong_bow");
        if (strongBow > 0 && isBowOrCrossbowUser(attacker) && !isMeleeDamage(source)) {
            multiplier *= 1.0F + strongBow * 0.5F;
        }
        if (session.modifiers.contains(HvkAsaraArenaModifier.WEAPON_FORGING)) {
            multiplier *= 3.25F;
        }
        if (session.modifiers.contains(HvkAsaraArenaModifier.TRAINED)) {
            multiplier *= 1.25F;
        }
        CompoundTag data = attacker.getPersistentData();
        if (session.modifiers.contains(HvkAsaraArenaModifier.QUICK_STRIKE) && !data.getBoolean(QUICK_STRIKE_USED)) {
            data.putBoolean(QUICK_STRIKE_USED, true);
            multiplier *= 5.5F;
        }
        if (session.modifiers.contains(HvkAsaraArenaModifier.FULLY_ARMED) && target != null) {
            float minimum = target.getMaxHealth() * 0.25F;
            AttributeInstance attackDamage = attacker.getAttribute(Attributes.ATTACK_DAMAGE);
            float baseDamage = attackDamage == null ? 1.0F : (float) attackDamage.getValue();
            return Math.max(multiplier, minimum / Math.max(0.001F, baseDamage));
        }
        return multiplier;
    }

    private static float playerOutgoingMultiplier(LivingEntity attacker, LivingEntity target, Session session) {
        if (session == null || !attacker.getUUID().equals(session.playerId)) {
            return 1.0F;
        }
        int tier = HvkAsaraArenaModifier.tier(session.modifiers, "blunt_weapons");
        return tier == 1 ? 0.75F : tier == 2 ? 0.50F : tier == 3 ? 0.25F : 1.0F;
    }

    public static float capPlayerDamage(LivingEntity attacker, LivingEntity target, DamageSource source, float amount) {
        Session session = sessionAt(attacker);
        if (session == null || !attacker.getUUID().equals(session.playerId)) {
            return amount;
        }
        if (session.modifiers.contains(HvkAsaraArenaModifier.HIDDEN_EXTREME) && !isMeleeDamage(source)) {
            return 0.0F;
        }
        if (session.modifiers.contains(HvkAsaraArenaModifier.FINAL_LINE) && target != null) {
            return Math.min(amount, target.getMaxHealth() * 0.10F);
        }
        return amount;
    }

    public static double cooldownMultiplier(Player player) {
        Session session = player == null ? null : SESSIONS.get(player.getUUID());
        if (session == null) {
            return 1.0D;
        }
        int tier = HvkAsaraArenaModifier.tier(session.modifiers, "system_failure");
        double multiplier = tier == 1 ? 1.30D : tier == 2 ? 1.60D : tier == 3 ? 2.50D : 1.0D;
        if (session.modifiers.contains(HvkAsaraArenaModifier.FINAL_LINE)) {
            multiplier *= 1.50D;
        }
        return multiplier;
    }

    private static void applyPlayerModifiers(ServerPlayer player, Session session, long now) {
        Set<HvkAsaraArenaModifier> selected = session.modifiers;
        int heart = HvkAsaraArenaModifier.tier(selected, "heart_failure");
        float maxHealth = selected.contains(HvkAsaraArenaModifier.FINAL_LINE)
                ? 1.0F : player.getAttribute(Attributes.MAX_HEALTH) == null ? player.getMaxHealth()
                : (float) player.getAttribute(Attributes.MAX_HEALTH).getBaseValue() * (1.0F - heart * 0.25F);
        if (player.getHealth() > maxHealth) {
            player.setHealth(Math.max(1.0F, maxHealth));
        }
        if (selected.contains(HvkAsaraArenaModifier.EXHAUSTED)) {
            player.setSprinting(false);
            if (player.getDeltaMovement().y > 0.0D && !player.onGround()) {
                player.setDeltaMovement(player.getDeltaMovement().x, Math.min(0.0D, player.getDeltaMovement().y),
                        player.getDeltaMovement().z);
            }
        }
        if (selected.contains(HvkAsaraArenaModifier.DARKNESS)) {
            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0, false, false, false));
        }
        if (selected.contains(HvkAsaraArenaModifier.MAGNETIC_DISORDER)) {
            player.addEffect(new MobEffectInstance(ModEffects.RAPTOR_ELECTROMAGNETIC_INTERFERENCE,
                    40, 0, false, false, true));
        }
        if (selected.contains(HvkAsaraArenaModifier.FROZEN_WORLD)) {
            double moved = player.distanceToSqr(session.lastX, session.lastY, session.lastZ);
            if (moved > 0.0025D && now >= session.nextMovementColdAt) {
                NTwoStateManager.addAmbientCold(player, 4);
                session.nextMovementColdAt = now + 20L;
            }
            session.lastX = player.getX();
            session.lastY = player.getY();
            session.lastZ = player.getZ();
        }
        if (selected.contains(HvkAsaraArenaModifier.STAMINA_FAILURE)) {
            StaminaManager.empty(player);
        }
        if (selected.contains(HvkAsaraArenaModifier.DISEASED)) {
            for (InjuryManager.Type type : InjuryManager.Type.values()) {
                InjuryManager.applyForced(player, type);
            }
        }
    }

    private static void collectLoot(ServerLevel level, ServerPlayer player, Session session) {
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, bounds(session), Entity::isAlive)) {
            give(player, item.getItem().copy());
            item.discard();
        }
        AABB lootBounds = session.structureBounds.inflate(8.0D);
        int minChunkX = SectionPos.blockToSectionCoord((int) Math.floor(lootBounds.minX));
        int maxChunkX = SectionPos.blockToSectionCoord((int) Math.floor(lootBounds.maxX));
        int minChunkZ = SectionPos.blockToSectionCoord((int) Math.floor(lootBounds.minZ));
        int maxChunkZ = SectionPos.blockToSectionCoord((int) Math.floor(lootBounds.maxZ));
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                for (BlockEntity blockEntity : level.getChunk(chunkX, chunkZ).getBlockEntities().values()) {
                    if (!lootBounds.contains(blockEntity.getBlockPos().getCenter())
                            || !(blockEntity instanceof Container container)) {
                        continue;
                    }
                    if (blockEntity instanceof RandomizableContainerBlockEntity lootable) {
                        lootable.unpackLootTable(player);
                    }
                    for (int slot = 0; slot < container.getContainerSize(); slot++) {
                        ItemStack stack = container.removeItemNoUpdate(slot);
                        if (!stack.isEmpty()) {
                            give(player, stack);
                        }
                    }
                    blockEntity.setChanged();
                }
            }
        }
    }

    private static void complete(ServerLevel level, ServerPlayer player, Session session, long now) {
        collectLoot(level, player, session);
        session.successReturnAt = now + SUCCESS_PACK_TICKS;
        if (session.challengeEligible) {
            player.displayClientMessage(Component.translatable(
                    "message.dealt_force_skills.hvk_asara_arena.success", session.score), false);
            level.playSound(null, player.blockPosition(), ModSounds.HVK_ASARA_ARENA_SUCCESS.get(),
                    SoundSource.BLOCKS, 1.0F, 1.0F);
        } else {
            player.displayClientMessage(Component.translatable(
                    "message.dealt_force_skills.hvk_asara_arena.exploration_complete", session.score), false);
        }
    }

    private static void give(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    private static void fail(ServerPlayer player, Session session, String messageKey) {
        clearNegativeEffects(player);
        player.displayClientMessage(Component.translatable(messageKey), false);
        player.level().playSound(null, player.blockPosition(), ModSounds.HVK_ASARA_ARENA_FAILURE.get(),
                SoundSource.BLOCKS, 1.0F, 1.0F);
        finishReturn(player, session);
    }

    private static void finishReturn(ServerPlayer player, Session session) {
        SESSIONS.remove(player.getUUID());
        NetworkHandler.sendToPlayer(S2C_HvkAsaraArenaStatus.inactive(), player);
        player.removeEffect(MobEffects.GLOWING);
        clearNegativeEffects(player);
        InjuryManager.clearAll(player);
        NTwoStateManager.clearCold(player);
        teleportToReturn(player, session.returnPoint);
        player.getPersistentData().remove(ACTIVE_RETURN);
    }

    private static void teleportToReturn(ServerPlayer player, ReturnPoint point) {
        ServerLevel returnLevel = player.server.getLevel(point.dimension);
        if (returnLevel == null) returnLevel = player.server.overworld();
        player.teleportTo(returnLevel, point.x, point.y, point.z, point.yRot, point.xRot);
    }

    private static ReturnPoint loadActiveReturn(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        return data.contains(ACTIVE_RETURN) ? ReturnPoint.load(data.getCompound(ACTIVE_RETURN)) : null;
    }

    private static void clearNegativeEffects(LivingEntity entity) {
        List<net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect>> remove = entity.getActiveEffects().stream()
                .map(MobEffectInstance::getEffect)
                .filter(effect -> effect.value().getCategory() == net.minecraft.world.effect.MobEffectCategory.HARMFUL)
                .toList();
        remove.forEach(entity::removeEffect);
    }

    private static Session sessionAt(Entity entity) {
        if (entity == null || !isArenaDimension(entity.level().dimension())) {
            return null;
        }
        for (Session session : SESSIONS.values()) {
            if (session.arenaDimension.equals(entity.level().dimension()) && inside(session, entity)) {
                return session;
            }
        }
        return null;
    }

    private static boolean inside(Session session, Entity entity) {
        return bounds(session).contains(entity.position());
    }

    private static AABB bounds(Session session) {
        return session.bounds;
    }

    private static boolean isHostile(LivingEntity entity) {
        return entity instanceof Enemy || entity instanceof Mob mob && mob.getTarget() instanceof ServerPlayer;
    }

    private static boolean isSessionHostile(LivingEntity entity, Session session) {
        return session.challengeHostiles.contains(entity.getUUID()) || isHostile(entity);
    }

    private static void syncArenaStatus(ServerPlayer player, Session session, List<LivingEntity> activeHostiles, long now) {
        if (now < session.nextHudSyncAt) return;
        session.nextHudSyncAt = now + HUD_SYNC_INTERVAL_TICKS;
        List<BlockPos> positions = activeHostiles.size() < 8 ? activeHostiles.stream().map(Entity::blockPosition).toList() : List.of();
        NetworkHandler.sendToPlayer(new S2C_HvkAsaraArenaStatus(activeHostiles.size(), positions), player);
    }

    private static boolean isAllowedReflectionDamage(DamageSource source) {
        String path = source.type().msgId().toLowerCase(java.util.Locale.ROOT);
        return source.getDirectEntity() instanceof net.minecraft.world.entity.projectile.Projectile
                || source.getEntity() instanceof LivingEntity
                || path.contains("bullet") || path.contains("gun") || path.contains("projectile") || path.contains("mob");
    }

    private static boolean isMeleeDamage(DamageSource source) {
        return source != null && source.getEntity() instanceof LivingEntity attacker
                && source.getDirectEntity() == attacker;
    }

    private static boolean isBowOrCrossbowUser(LivingEntity entity) {
        return isBowOrCrossbow(entity.getMainHandItem()) || isBowOrCrossbow(entity.getOffhandItem());
    }

    private static boolean isBowOrCrossbow(ItemStack stack) {
        return stack.getItem() instanceof BowItem || stack.getItem() instanceof CrossbowItem;
    }

    private static boolean hasEquippedArmor(LivingEntity entity) {
        for (ItemStack stack : entity.getArmorSlots()) {
            if (stack.getItem() instanceof ArmorItem) {
                return true;
            }
        }
        return false;
    }

    private static void equipArmor(LivingEntity entity, int tier) {
        entity.setItemSlot(EquipmentSlot.HEAD, new ItemStack(tier == 1 ? Items.IRON_HELMET
                : tier == 2 ? Items.DIAMOND_HELMET : Items.NETHERITE_HELMET));
        entity.setItemSlot(EquipmentSlot.CHEST, new ItemStack(tier == 1 ? Items.IRON_CHESTPLATE
                : tier == 2 ? Items.DIAMOND_CHESTPLATE : Items.NETHERITE_CHESTPLATE));
        entity.setItemSlot(EquipmentSlot.LEGS, new ItemStack(tier == 1 ? Items.IRON_LEGGINGS
                : tier == 2 ? Items.DIAMOND_LEGGINGS : Items.NETHERITE_LEGGINGS));
        entity.setItemSlot(EquipmentSlot.FEET, new ItemStack(tier == 1 ? Items.IRON_BOOTS
                : tier == 2 ? Items.DIAMOND_BOOTS : Items.NETHERITE_BOOTS));
    }

    private static void applyAttributeModifier(AttributeInstance attribute, ResourceLocation id, String name, double amount) {
        if (attribute == null) {
            return;
        }
        if (attribute.getModifier(id) != null) {
            attribute.removeModifier(id);
        }
        attribute.addPermanentModifier(new AttributeModifier(id, amount,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    private static void applyAttributeFloorModifier(AttributeInstance attribute, ResourceLocation id, double floor) {
        if (attribute == null || !Double.isFinite(floor)) {
            return;
        }
        if (attribute.getModifier(id) != null) {
            attribute.removeModifier(id);
        }
        double amount = Math.max(0.0D, floor - attribute.getValue());
        if (amount > 0.0D) {
            attribute.addPermanentModifier(new AttributeModifier(id, amount,
                    AttributeModifier.Operation.ADD_VALUE));
        }
    }

    private static void enchantEquipment(LivingEntity entity, int count, int maxLevel) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            Registry<Enchantment> registry = entity.registryAccess().registryOrThrow(Registries.ENCHANTMENT);
            List<Holder.Reference<Enchantment>> candidates = registry.holders()
                    .filter(stack::supportsEnchantment)
                    .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
            List<Holder.Reference<Enchantment>> selected = new ArrayList<>();
            List<Integer> levels = new ArrayList<>();
            for (int index = 0; index < count && !candidates.isEmpty(); index++) {
                Holder.Reference<Enchantment> enchantment = candidates.remove(entity.getRandom().nextInt(candidates.size()));
                int level = maxLevel == 6 ? 6 : 1 + entity.getRandom().nextInt(Math.max(1, maxLevel));
                selected.add(enchantment);
                levels.add(level);
            }
            EnchantmentHelper.updateEnchantments(stack, enchantments -> {
                for (int index = 0; index < selected.size(); index++) {
                    enchantments.set(selected.get(index), levels.get(index));
                }
            });
        }
    }

    private static void forceTarget(LivingEntity center, LivingEntity attacker, double radius) {
        for (Mob mob : center.level().getEntitiesOfClass(Mob.class,
                center.getBoundingBox().inflate(radius), Mob::isAlive)) {
            mob.setTarget(attacker);
        }
    }

    private static void teleportNear(LivingEntity entity, LivingEntity target) {
        for (int i = 0; i < 12; i++) {
            double x = target.getX() + (entity.getRandom().nextDouble() - 0.5D) * 14.0D;
            double z = target.getZ() + (entity.getRandom().nextDouble() - 0.5D) * 14.0D;
            double y = target.getY();
            if (entity.randomTeleport(x, y, z, true)) {
                return;
            }
        }
    }

    private static void spawnCopies(Mob source, Session session, int count) {
        for (int i = 0; i < count; i++) {
            Entity created = source.getType().create(source.level());
            if (!(created instanceof Mob copy)) {
                continue;
            }
            copy.load(source.saveWithoutId(new CompoundTag()));
            copy.moveTo(source.getX() + (i + 1) * 0.6D, source.getY(), source.getZ() + (i + 1) * 0.6D,
                    source.getYRot(), source.getXRot());
            copy.setUUID(UUID.randomUUID());
            copy.getPersistentData().putUUID(MOB_SESSION, session.id);
            copy.getPersistentData().putBoolean(SUPPORT_TRIGGERED, true);
            source.level().addFreshEntity(copy);
        }
    }

    private static int lootCopies(int score) {
        int copies = score / 100;
        if (score > 550) {
            copies += (score - 550) / 50;
        }
        return Math.max(0, copies);
    }

    private static int challengeTicks(long mask) {
        int tier = HvkAsaraArenaModifier.tier(HvkAsaraArenaModifier.fromMask(mask), "race_time");
        return (tier == 1 ? 900 : tier == 2 ? 600 : tier == 3 ? 420 : 1800) * 20;
    }

    private static ResourceKey<Level> arenaKey(String path) {
        return ResourceKey.create(Registries.DIMENSION,
                ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, path));
    }

    private static boolean isArenaDimension(ResourceKey<Level> dimension) {
        return OVERWORLD_ARENA.equals(dimension) || NETHER_ARENA.equals(dimension) || END_ARENA.equals(dimension);
    }

    private static ResourceKey<Level> arenaDimensionFor(ServerLevel source) {
        ResourceLocation effects = source.dimensionType().effectsLocation();
        if (Level.NETHER.equals(source.dimension()) || source.dimensionType().piglinSafe()
                || NETHER_EFFECTS.equals(effects)) {
            return NETHER_ARENA;
        }
        if (Level.END.equals(source.dimension()) || END_EFFECTS.equals(effects)) {
            return END_ARENA;
        }
        return OVERWORLD_ARENA;
    }

    private static BlockPos instanceOrigin(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        if (data.contains(INSTANCE_X) && data.contains(INSTANCE_Z)) {
            BlockPos stored = new BlockPos(data.getInt(INSTANCE_X), INSTANCE_Y, data.getInt(INSTANCE_Z));
            if (!instanceOccupied(stored, player.getUUID())) return stored;
        }
        UUID playerId = player.getUUID();
        long mixed = playerId.getMostSignificantBits() ^ Long.rotateLeft(playerId.getLeastSignificantBits(), 21);
        int gridX = (int) Math.floorMod(mixed, 24001L) - 12000;
        int gridZ = (int) Math.floorMod(Long.rotateLeft(mixed, 29), 24001L) - 12000;
        int baseX = gridX * INSTANCE_SPACING;
        int baseZ = gridZ * INSTANCE_SPACING;
        for (int probe = 0; probe <= SESSIONS.size(); probe++) {
            BlockPos candidate = new BlockPos(baseX + probe * INSTANCE_SPACING, INSTANCE_Y, baseZ);
            if (!instanceOccupied(candidate, playerId)) {
                data.putInt(INSTANCE_X, candidate.getX());
                data.putInt(INSTANCE_Z, candidate.getZ());
                return candidate;
            }
        }
        throw new IllegalStateException("Unable to allocate an isolated HVK Asara arena instance");
    }

    private static boolean instanceOccupied(BlockPos candidate, UUID playerId) {
        return SESSIONS.values().stream().anyMatch(session -> !session.playerId.equals(playerId)
                && Math.abs(session.origin.getX() - candidate.getX()) < INSTANCE_RADIUS * 2 + 64
                && Math.abs(session.origin.getZ() - candidate.getZ()) < INSTANCE_RADIUS * 2 + 64);
    }

    private static List<ServerLevel> sourceLevels(MinecraftServer server) {
        List<ServerLevel> result = new ArrayList<>();
        Set<ResourceKey<Level>> seen = new HashSet<>();
        ServerLevel[] preferred = {server.overworld(), server.getLevel(Level.NETHER), server.getLevel(Level.END)};
        for (ServerLevel level : preferred) {
            if (level != null && seen.add(level.dimension())) result.add(level);
        }
        for (ServerLevel level : server.getAllLevels()) {
            if (!GamblerArenaManager.ARENA_LEVEL.equals(level.dimension())
                    && !isArenaDimension(level.dimension()) && seen.add(level.dimension())) {
                result.add(level);
            }
        }
        return result;
    }

    private static boolean supportsStructure(ServerLevel level, Structure structure) {
        return level.getChunkSource().getGenerator().getBiomeSource().possibleBiomes().stream().anyMatch(structure.biomes()::contains);
    }

    private static ChunkPos previewProbeChunk(ChunkPos center, long probe) {
        if (probe == 0L) return center;
        int radius = (int) Math.ceil((Math.sqrt(probe + 1.0D) - 1.0D) * 0.5D);
        long ringStart = (long) (radius * 2 - 1) * (radius * 2 - 1);
        int offset = (int) (probe - ringStart);
        int side = radius * 2;
        if (offset < side) return new ChunkPos(center.x - radius + offset, center.z - radius);
        if (offset < side * 2) return new ChunkPos(center.x + radius, center.z - radius + offset - side);
        if (offset < side * 3) return new ChunkPos(center.x + radius - offset + side * 2, center.z + radius);
        return new ChunkPos(center.x - radius, center.z + radius - offset + side * 3);
    }

    private static long previewChunkCount() {
        long diameter = STRUCTURE_PREVIEW_RADIUS_CHUNKS * 2L + 1L;
        return diameter * diameter;
    }

    private static boolean usesKnownPlacement(List<StructurePlacement> placements) {
        return placements.stream().anyMatch(placement -> placement instanceof RandomSpreadStructurePlacement
                || placement instanceof ConcentricRingsStructurePlacement);
    }

    private static List<ChunkPos> placementCandidates(ServerLevel source, List<StructurePlacement> placements,
                                                       ChunkPos center) {
        var state = source.getChunkSource().getGeneratorState();
        LinkedHashSet<ChunkPos> candidates = new LinkedHashSet<>();
        for (StructurePlacement placement : placements) {
            if (placement instanceof ConcentricRingsStructurePlacement rings) {
                List<ChunkPos> positions = state.getRingPositionsFor(rings);
                if (positions != null) {
                    positions.stream().filter(pos -> withinPreviewRadius(center, pos))
                            .sorted(java.util.Comparator.comparingLong(pos -> chunkDistanceSqr(center, pos)))
                            .limit(STRUCTURE_PREVIEW_PROBES).forEach(candidates::add);
                }
            } else if (placement instanceof RandomSpreadStructurePlacement randomSpread) {
                int spacing = Math.max(1, randomSpread.spacing());
                int maxRing = (STRUCTURE_PREVIEW_RADIUS_CHUNKS + spacing - 1) / spacing + 1;
                for (int ring = 0; ring <= maxRing && candidates.size() < STRUCTURE_PREVIEW_PROBES; ring++) {
                    for (int dx = -ring; dx <= ring && candidates.size() < STRUCTURE_PREVIEW_PROBES; dx++) {
                        for (int dz = -ring; dz <= ring && candidates.size() < STRUCTURE_PREVIEW_PROBES; dz++) {
                            if (Math.abs(dx) != ring && Math.abs(dz) != ring) continue;
                            ChunkPos candidate = randomSpread.getPotentialStructureChunk(state.getLevelSeed(),
                                    center.x + spacing * dx, center.z + spacing * dz);
                            if (withinPreviewRadius(center, candidate)) candidates.add(candidate);
                        }
                    }
                }
            }
            if (candidates.size() >= STRUCTURE_PREVIEW_PROBES) break;
        }
        return List.copyOf(candidates);
    }

    private static boolean withinPreviewRadius(ChunkPos center, ChunkPos candidate) {
        return Math.abs(candidate.x - center.x) <= STRUCTURE_PREVIEW_RADIUS_CHUNKS
                && Math.abs(candidate.z - center.z) <= STRUCTURE_PREVIEW_RADIUS_CHUNKS;
    }

    private static long chunkDistanceSqr(ChunkPos first, ChunkPos second) {
        long dx = (long) first.x - second.x;
        long dz = (long) first.z - second.z;
        return dx * dx + dz * dz;
    }

    private static StructureStart generateStructureAt(ServerLevel source, Structure structure, ChunkPos chunkPos) {
        var chunkSource = source.getChunkSource();
        return structure.generate(source.registryAccess(), chunkSource.getGenerator(),
                chunkSource.getGenerator().getBiomeSource(), chunkSource.randomState(), source.getStructureManager(),
                source.getSeed(), chunkPos, 0, source, structure.biomes()::contains);
    }

    private static StructurePreview relocatePreview(StructureStart start, Structure structure, ServerLevel source,
                                                     ChunkPos targetChunk) {
        BoundingBox sourceBox = copyBox(start.getBoundingBox());
        StructureStart relocated = relocateStructure(start, structure, targetChunk);
        return new StructurePreview(relocated, source, sourceBox, arenaDimensionFor(source));
    }

    private static BoundingBox copyBox(BoundingBox box) {
        return new BoundingBox(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ());
    }

    private static StructureStart relocateStructure(StructureStart start, Structure structure, ChunkPos targetChunk) {
        ChunkPos sourceChunk = start.getChunkPos();
        int offsetX = (targetChunk.x - sourceChunk.x) * 16;
        int offsetZ = (targetChunk.z - sourceChunk.z) * 16;
        if (offsetX != 0 || offsetZ != 0) {
            for (var piece : start.getPieces()) piece.move(offsetX, 0, offsetZ);
        }
        return new StructureStart(structure, targetChunk, start.getReferences(),
                new PiecesContainer(List.copyOf(start.getPieces())));
    }

    private static boolean moveStructureToArenaHeight(ServerLevel level, StructureStart start) {
        if (start.getPieces().isEmpty()) {
            return false;
        }
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (var piece : start.getPieces()) {
            minY = Math.min(minY, piece.getBoundingBox().minY());
            maxY = Math.max(maxY, piece.getBoundingBox().maxY());
        }
        int height = maxY - minY + 1;
        int lowest = level.getMinBuildHeight() + 4;
        int highest = level.getMaxBuildHeight() - 4;
        if (height <= 0 || height > highest - lowest + 1) {
            return false;
        }
        int targetMinY = Math.max(lowest, Math.min(INSTANCE_Y, highest - height + 1));
        int offsetY = targetMinY - minY;
        for (var piece : start.getPieces()) {
            piece.move(0, offsetY, 0);
        }
        return true;
    }

    private static AABB structureBounds(BoundingBox box) {
        return new AABB(box.minX(), box.minY(), box.minZ(),
                box.maxX() + 1.0D, box.maxY() + 1.0D, box.maxZ() + 1.0D);
    }

    private static List<BoundingBox> pieceBounds(StructureStart start) {
        return start.getPieces().stream().map(piece -> copyBox(piece.getBoundingBox())).toList();
    }

    private static BlockPos arrivalPoint(ServerLevel level, BlockPos origin, AABB structureBounds) {
        int centerX = (int) Math.floor((structureBounds.minX + structureBounds.maxX) * 0.5D);
        int centerZ = (int) Math.floor((structureBounds.minZ + structureBounds.maxZ) * 0.5D);
        int y = (int) Math.floor(structureBounds.minY);
        y = Math.max(level.getMinBuildHeight() + 2, Math.min(level.getMaxBuildHeight() - 3, y));
        BlockPos[] candidates = {
                new BlockPos((int) Math.floor(structureBounds.minX) - ARRIVAL_DISTANCE, y, centerZ),
                new BlockPos((int) Math.ceil(structureBounds.maxX) + ARRIVAL_DISTANCE, y, centerZ),
                new BlockPos(centerX, y, (int) Math.floor(structureBounds.minZ) - ARRIVAL_DISTANCE),
                new BlockPos(centerX, y, (int) Math.ceil(structureBounds.maxZ) + ARRIVAL_DISTANCE)
        };
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (BlockPos candidate : candidates) {
            if (Math.abs(candidate.getX() - origin.getX()) > INSTANCE_RADIUS - 8
                    || Math.abs(candidate.getZ() - origin.getZ()) > INSTANCE_RADIUS - 8) {
                continue;
            }
            double distance = candidate.distSqr(origin);
            if (distance < bestDistance) {
                best = candidate;
                bestDistance = distance;
            }
        }
        return best;
    }

    private static float lookYaw(BlockPos arrival, AABB structureBounds) {
        double dx = (structureBounds.minX + structureBounds.maxX) * 0.5D - arrival.getX();
        double dz = (structureBounds.minZ + structureBounds.maxZ) * 0.5D - arrival.getZ();
        return (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0D);
    }

    private static boolean canPrepareTerrain(ServerLevel level, BoundingBox structureBox, BlockPos arrival) {
        int minX = Math.min(structureBox.minX() - TERRAIN_MARGIN, arrival.getX() - ARRIVAL_HALF_SIZE);
        int maxX = Math.max(structureBox.maxX() + TERRAIN_MARGIN, arrival.getX() + ARRIVAL_HALF_SIZE);
        int minZ = Math.min(structureBox.minZ() - TERRAIN_MARGIN, arrival.getZ() - ARRIVAL_HALF_SIZE);
        int maxZ = Math.max(structureBox.maxZ() + TERRAIN_MARGIN, arrival.getZ() + ARRIVAL_HALF_SIZE);
        long area = (long) (maxX - minX + 1) * (maxZ - minZ + 1);
        ResetBounds bounds = resetBounds(level, structureBox, arrival);
        return area > 0L && area * (TERRAIN_DEPTH + WATER_DEPTH + 1L) <= MAX_TERRAIN_BLOCKS
                && bounds.volume() <= MAX_RESET_BLOCKS;
    }

    private static ResetBounds resetBounds(ServerLevel level, BoundingBox structureBox, BlockPos arrival) {
        int minX = Math.min(structureBox.minX() - TERRAIN_MARGIN, arrival.getX() - ARRIVAL_HALF_SIZE);
        int maxX = Math.max(structureBox.maxX() + TERRAIN_MARGIN, arrival.getX() + ARRIVAL_HALF_SIZE);
        int minZ = Math.min(structureBox.minZ() - TERRAIN_MARGIN, arrival.getZ() - ARRIVAL_HALF_SIZE);
        int maxZ = Math.max(structureBox.maxZ() + TERRAIN_MARGIN, arrival.getZ() + ARRIVAL_HALF_SIZE);
        int surfaceY = structureBox.minY() - 1;
        int minY = Math.max(level.getMinBuildHeight(), surfaceY - TERRAIN_DEPTH);
        int maxY = Math.min(level.getMaxBuildHeight() - 1,
                Math.max(structureBox.maxY(), Math.max(surfaceY + WATER_DEPTH, arrival.getY() + 3)));
        return new ResetBounds(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private static ServerLevel savedArenaLevel(MinecraftServer server, CompoundTag tag) {
        ResourceLocation id = ResourceLocation.tryParse(tag.getString("ArenaDimension"));
        ResourceKey<Level> key = id == null ? GamblerArenaManager.ARENA_LEVEL
                : ResourceKey.create(Registries.DIMENSION, id);
        return server.getLevel(key);
    }

    private static BlockPos savedOrigin(CompoundTag tag, BlockPos fallback) {
        return tag.contains("OriginX") && tag.contains("OriginZ")
                ? new BlockPos(tag.getInt("OriginX"), INSTANCE_Y, tag.getInt("OriginZ")) : fallback;
    }

    private static void saveInstance(ServerPlayer player, ServerLevel arena, ServerLevel source, ResetBounds bounds,
                                     ResourceLocation structureId, ChunkPos startChunk, BoundingBox structureBox,
                                     BlockPos origin) {
        CompoundTag tag = bounds.save();
        tag.putString("ArenaDimension", arena.dimension().location().toString());
        tag.putString("SourceDimension", source.dimension().location().toString());
        tag.putInt("OriginX", origin.getX());
        tag.putInt("OriginZ", origin.getZ());
        tag.putString("Structure", structureId.toString());
        tag.putInt("StartChunkX", startChunk.x);
        tag.putInt("StartChunkZ", startChunk.z);
        tag.putInt("MinChunkX", SectionPos.blockToSectionCoord(structureBox.minX()));
        tag.putInt("MaxChunkX", SectionPos.blockToSectionCoord(structureBox.maxX()));
        tag.putInt("MinChunkZ", SectionPos.blockToSectionCoord(structureBox.minZ()));
        tag.putInt("MaxChunkZ", SectionPos.blockToSectionCoord(structureBox.maxZ()));
        player.getPersistentData().put(INSTANCE_STATE, tag);
    }

    private static int naturalSurfaceY(ServerLevel source, BoundingBox sourceBox, ChunkGenerator generator,
                                       RandomState randomState) {
        int[] sampleX = {sourceBox.minX(), (sourceBox.minX() + sourceBox.maxX()) / 2, sourceBox.maxX()};
        int[] sampleZ = {sourceBox.minZ(), (sourceBox.minZ() + sourceBox.maxZ()) / 2, sourceBox.maxZ()};
        int surface = source.getMinBuildHeight();
        boolean netherLike = Level.NETHER.equals(source.dimension()) || source.dimensionType().piglinSafe()
                || NETHER_EFFECTS.equals(source.dimensionType().effectsLocation());
        for (int x : sampleX) {
            for (int z : sampleZ) {
                NoiseColumn column = generator.getBaseColumn(x, z, source, randomState);
                if (netherLike) {
                    boolean solid = false;
                    for (int y = source.getMinBuildHeight(); y <= source.getMaxBuildHeight() / 2; y++) {
                        if (!column.getBlock(y).isAir()) {
                            surface = Math.max(surface, y);
                            solid = true;
                        } else if (solid && y > source.getMinBuildHeight() + 4) {
                            break;
                        }
                    }
                } else {
                    for (int y = source.getMaxBuildHeight() - 1; y >= source.getMinBuildHeight(); y--) {
                        if (!column.getBlock(y).isAir()) {
                            surface = Math.max(surface, y);
                            break;
                        }
                    }
                }
            }
        }
        return surface;
    }

    private static BlockState arenaFloor(ResourceKey<Level> arenaDimension) {
        if (NETHER_ARENA.equals(arenaDimension)) return net.minecraft.world.level.block.Blocks.NETHERRACK.defaultBlockState();
        if (END_ARENA.equals(arenaDimension)) return net.minecraft.world.level.block.Blocks.END_STONE.defaultBlockState();
        return net.minecraft.world.level.block.Blocks.GRASS_BLOCK.defaultBlockState();
    }

    private static void prepareArrivalAccess(ServerLevel level, BoundingBox structureBox, BlockPos arrival,
                                             BlockState floor) {
        int deckY = structureBox.minY() - 1;
        fillDeck(level, arrival.getX() - ARRIVAL_HALF_SIZE, arrival.getX() + ARRIVAL_HALF_SIZE,
                arrival.getZ() - ARRIVAL_HALF_SIZE, arrival.getZ() + ARRIVAL_HALF_SIZE, deckY, floor);
        if (arrival.getX() < structureBox.minX()) {
            fillDeck(level, arrival.getX(), structureBox.minX() - 1,
                    arrival.getZ() - ACCESS_HALF_WIDTH, arrival.getZ() + ACCESS_HALF_WIDTH, deckY, floor);
        } else if (arrival.getX() > structureBox.maxX()) {
            fillDeck(level, structureBox.maxX() + 1, arrival.getX(),
                    arrival.getZ() - ACCESS_HALF_WIDTH, arrival.getZ() + ACCESS_HALF_WIDTH, deckY, floor);
        } else if (arrival.getZ() < structureBox.minZ()) {
            fillDeck(level, arrival.getX() - ACCESS_HALF_WIDTH, arrival.getX() + ACCESS_HALF_WIDTH,
                    arrival.getZ(), structureBox.minZ() - 1, deckY, floor);
        } else {
            fillDeck(level, arrival.getX() - ACCESS_HALF_WIDTH, arrival.getX() + ACCESS_HALF_WIDTH,
                    structureBox.maxZ() + 1, arrival.getZ(), deckY, floor);
        }
    }

    private static void fillDeck(ServerLevel level, int minX, int maxX, int minZ, int maxZ, int deckY,
                                 BlockState floor) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                level.setBlock(cursor.set(x, deckY, z), floor, 2);
                level.setBlock(cursor.set(x, deckY + 1, z),
                        net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
                level.setBlock(cursor.set(x, deckY + 2, z),
                        net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
                level.setBlock(cursor.set(x, deckY + 3, z),
                        net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
            }
        }
    }

    private static void spawnNaturalHostiles(ServerLevel level, Session session, long now) {
        if (now >= session.spawnEndAt || now % 10L != 0L || session.challengeHostiles.size() >= MAX_CHALLENGE_HOSTILES
                || level.getDifficulty() == Difficulty.PEACEFUL || !level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)) return;
        for (int attempt = 0; attempt < 2 && session.challengeHostiles.size() < MAX_CHALLENGE_HOSTILES; attempt++) {
            BlockPos spawnPos = findStructureFloor(level, session);
            if (spawnPos != null) spawnStructureMonster(level, session, spawnPos);
        }
    }

    private static boolean spawnStructureMonster(ServerLevel level, Session session, BlockPos spawnPos) {
        Structure structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(session.structureId);
        var override = structure == null ? null : structure.spawnOverrides().get(MobCategory.MONSTER);
        var possibleSpawns = override == null || override.spawns().isEmpty()
                ? level.getChunkSource().getGenerator().getMobsAt(
                level.getBiome(spawnPos), level.structureManager(), MobCategory.MONSTER, spawnPos)
                : override.spawns();
        var spawnData = possibleSpawns.getRandom(level.random).orElse(null);
        if (spawnData == null) return false;
        Entity created = spawnData.type.create(level);
        if (!(created instanceof Mob mob)) {
            if (created != null) created.discard();
            return false;
        }
        mob.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D,
                level.random.nextFloat() * 360.0F, 0.0F);
        if (!mob.checkSpawnObstruction(level)) {
            mob.discard();
            return false;
        }
        mob.setPersistenceRequired();
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos), MobSpawnType.STRUCTURE, null);
        if (!level.addFreshEntity(mob)) return false;
        markAndApplyMob(mob, session);
        return true;
    }

    private static void spawnExpectedStructureHostiles(ServerLevel level, Session session, long now) {
        if (now >= session.spawnEndAt || now % 20L != 0L || session.challengeHostiles.size() >= MAX_CHALLENGE_HOSTILES
                || level.getDifficulty() == Difficulty.PEACEFUL || !level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)
                || expectedStructureMonster(session) == null) return;
        for (int count = 0; count < 2 && session.challengeHostiles.size() < MAX_CHALLENGE_HOSTILES; count++) {
            BlockPos spawnPos = findStructureFloor(level, session);
            EntityType<?> type = expectedStructureMonster(session);
            session.structureSpawnCount++;
            Entity created = spawnPos == null ? null : type.create(level);
            if (!(created instanceof Mob mob)) continue;
            mob.moveTo(spawnPos, level.random.nextFloat() * 360.0F, 0.0F);
            if (!mob.checkSpawnObstruction(level)) {
                mob.discard();
                continue;
            }
            mob.setPersistenceRequired();
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos), MobSpawnType.STRUCTURE, null);
            if (level.addFreshEntity(mob)) {
                markAndApplyMob(mob, session);
            }
        }
    }

    private static EntityType<?> expectedStructureMonster(Session session) {
        int index = session.structureSpawnCount;
        if (session.structureId.equals(WOODLAND_MANSION)) {
            return index % 5 == 4 ? EntityType.EVOKER : EntityType.VINDICATOR;
        }
        if (session.structureId.equals(STRONGHOLD)) {
            EntityType<?>[] types = {EntityType.ZOMBIE, EntityType.SKELETON, EntityType.CREEPER,
                    EntityType.ENDERMAN, EntityType.SILVERFISH};
            return types[index % types.length];
        }
        if (session.structureId.equals(NETHER_FORTRESS)) {
            EntityType<?>[] types = {EntityType.WITHER_SKELETON, EntityType.BLAZE,
                    EntityType.MAGMA_CUBE, EntityType.ZOMBIFIED_PIGLIN};
            return types[index % types.length];
        }
        return null;
    }

    private static BlockPos findStructureFloor(ServerLevel level, Session session) {
        BlockPos covered = null;
        BlockPos exposed = null;
        int coveredCount = 0;
        int exposedCount = 0;
        for (int attempt = 0; attempt < SPAWN_FLOOR_PROBES; attempt++) {
            BoundingBox box = session.structurePieces.isEmpty()
                    ? new BoundingBox((int) session.structureBounds.minX, (int) session.structureBounds.minY,
                    (int) session.structureBounds.minZ, (int) session.structureBounds.maxX - 1,
                    (int) session.structureBounds.maxY, (int) session.structureBounds.maxZ - 1)
                    : session.structurePieces.get(level.random.nextInt(session.structurePieces.size()));
            int x = level.random.nextInt(box.minX(), box.maxX() + 1);
            int z = level.random.nextInt(box.minZ(), box.maxZ() + 1);
            for (int y = box.maxY(); y > box.minY(); y--) {
                BlockPos floor = new BlockPos(x, y - 1, z);
                BlockPos feet = floor.above();
                if (level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)
                        && level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                        && level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty()
                        && level.getBlockState(feet.above(2)).getCollisionShape(level, feet.above(2)).isEmpty()
                        && level.getFluidState(feet).isEmpty() && level.getFluidState(feet.above()).isEmpty()
                        && level.getFluidState(feet.above(2)).isEmpty()) {
                    if (level.canSeeSky(feet)) {
                        exposedCount++;
                        if (level.random.nextInt(exposedCount) == 0) exposed = feet;
                    } else {
                        coveredCount++;
                        if (level.random.nextInt(coveredCount) == 0) covered = feet;
                    }
                }
            }
        }
        return covered != null ? covered : exposed;
    }

    private record ResetBounds(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        private long volume() {
            return (long) (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
        }

        private CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putInt("MinX", minX); tag.putInt("MinY", minY); tag.putInt("MinZ", minZ);
            tag.putInt("MaxX", maxX); tag.putInt("MaxY", maxY); tag.putInt("MaxZ", maxZ);
            return tag;
        }

        private static ResetBounds load(CompoundTag tag) {
            ResetBounds bounds = new ResetBounds(tag.getInt("MinX"), tag.getInt("MinY"), tag.getInt("MinZ"),
                    tag.getInt("MaxX"), tag.getInt("MaxY"), tag.getInt("MaxZ"));
            return bounds.maxX >= bounds.minX && bounds.maxY >= bounds.minY && bounds.maxZ >= bounds.minZ ? bounds : null;
        }
    }

    private record StructurePreview(StructureStart start, ServerLevel source, BoundingBox sourceBox,
                                    ResourceKey<Level> arenaDimension) {
    }

    private enum BuildStage {
        PREVIEW, PREPARE, UNREGISTER, ENTITIES, CLEAR, TERRAIN, BIOMES, STRUCTURE, REGISTER, COMPLETE, CANCELLED
    }

    private record ClearTarget(ServerLevel level, ResetBounds bounds) {
    }

    private record EntityTarget(ServerLevel level, BlockPos origin) {
    }

    private record UnregisterTarget(ServerLevel level, Structure structure, ChunkPos startChunk,
                                    int minChunkX, int maxChunkX, int minChunkZ, int maxChunkZ) {
    }

    private static final class BuildJob {
        private final UUID playerId;
        private final ResourceLocation structureId;
        private final Structure structure;
        private final long mask;
        private final BlockPos origin;
        private final ReturnPoint returnPoint;
        private final List<ClearTarget> clearTargets = new ArrayList<>();
        private final List<EntityTarget> entityTargets = new ArrayList<>();
        private final List<UnregisterTarget> unregisterTargets = new ArrayList<>();
        private BuildStage stage = BuildStage.PREVIEW;
        private List<ServerLevel> previewSources;
        private int previewSourceIndex;
        private long previewProbeIndex;
        private int previewGenerationAttempts;
        private int previewPlacementSourceIndex = -1;
        private List<StructurePlacement> previewPlacements = List.of();
        private List<ChunkPos> previewCandidates = List.of();
        private boolean previewUsesKnownPlacement;
        private StructurePreview sourcePreview;
        private StructureStart preview;
        private ServerLevel arena;
        private BoundingBox structureBox;
        private AABB structureBounds;
        private BlockPos arrival;
        private ResetBounds resetBounds;
        private int clearTargetIndex;
        private long clearBlockIndex;
        private ServerLevel clearLoadedLevel;
        private int clearLoadedChunkX = Integer.MIN_VALUE;
        private int clearLoadedChunkZ = Integer.MIN_VALUE;
        private int unregisterTargetIndex;
        private int unregisterChunkIndex;
        private int entityTargetIndex;
        private int entityChunkIndex;
        private long terrainBlockIndex;
        private NoiseColumn terrainColumn;
        private int terrainColumnX = Integer.MIN_VALUE;
        private int terrainColumnZ = Integer.MIN_VALUE;
        private int offsetX;
        private int offsetY;
        private int offsetZ;
        private int biomeChunkIndex;
        private int structureChunkIndex;
        private int referenceChunkIndex;
        private boolean startRegistered;
        private boolean cleanupOnly;
        private RandomSource structureRandom;
        private int activeTicks;
        private int lastReportedProgress = -10;
        private int nextProgressReminderTick;

        private BuildJob(UUID playerId, ResourceLocation structureId, Structure structure, long mask,
                         BlockPos origin, ReturnPoint returnPoint) {
            this.playerId = playerId;
            this.structureId = structureId;
            this.structure = structure;
            this.mask = mask;
            this.origin = origin;
            this.returnPoint = returnPoint;
        }

        private boolean tick(MinecraftServer server, ServerPlayer player) {
            if (!cleanupOnly && ++activeTicks > MAX_BUILD_TICKS) {
                rollbackPlayer(server, player);
                if (player != null) player.displayClientMessage(Component.translatable(
                        "message.dealt_force_skills.hvk_asara_arena.place_failed", structureId.toString()), false);
                return !beginFailureCleanup();
            }
            long deadline = System.nanoTime() + BUILD_TICK_BUDGET_NANOS;
            boolean finished = switch (stage) {
                case PREVIEW -> processPreview(server, player, deadline);
                case PREPARE -> prepare(player);
                case UNREGISTER -> processUnregistration(deadline);
                case ENTITIES -> processEntities(deadline);
                case CLEAR -> processClear(deadline);
                case TERRAIN -> processTerrain(deadline);
                case BIOMES -> processBiomes(deadline);
                case STRUCTURE -> processStructure(deadline);
                case REGISTER -> processRegistration(deadline);
                case COMPLETE -> complete(player);
                case CANCELLED -> true;
            };
            if (!finished && !cleanupOnly && player != null) reportProgress(player);
            return finished;
        }

        private void reportProgress(ServerPlayer player) {
            int progress = progressPercent();
            int reportedProgress = progress >= 100 ? 100 : progress / 10 * 10;
            if (reportedProgress <= lastReportedProgress && activeTicks < nextProgressReminderTick) return;
            lastReportedProgress = Math.max(lastReportedProgress, reportedProgress);
            nextProgressReminderTick = activeTicks + 40;
            player.displayClientMessage(Component.translatable(
                    "message.dealt_force_skills.hvk_asara_arena.build_progress", reportedProgress), true);
        }

        private int progressPercent() {
            return switch (stage) {
                case PREVIEW -> 0;
                case PREPARE -> 10;
                case UNREGISTER -> phaseProgress(10, 5, unregisterProgress(), unregisterTotal());
                case ENTITIES -> phaseProgress(15, 5, entityProgress(), entityTotal());
                case CLEAR -> phaseProgress(20, 25, clearProgress(), clearTotal());
                case TERRAIN -> phaseProgress(45, 35, terrainBlockIndex,
                        resetBounds == null ? 0L : resetBounds.volume());
                case BIOMES -> phaseProgress(80, 5, biomeChunkIndex,
                        resetBounds == null ? 0L : resetChunkCount());
                case STRUCTURE -> phaseProgress(85, 12, structureChunkIndex,
                        structureBox == null ? 0L : structureChunkCount(structureBox));
                case REGISTER -> phaseProgress(97, 2, referenceChunkIndex,
                        structureBox == null ? 0L : structureChunkCount(structureBox));
                case COMPLETE -> 100;
                case CANCELLED -> lastReportedProgress;
            };
        }

        private static int phaseProgress(int start, int span, long completed, long total) {
            if (total <= 0L) return start + span;
            double fraction = Math.min(1.0D, Math.max(0.0D, (double) completed / total));
            return start + (int) Math.floor(span * fraction);
        }

        private long unregisterTotal() {
            long total = 0L;
            for (UnregisterTarget target : unregisterTargets) {
                total += (long) (target.maxChunkX - target.minChunkX + 1)
                        * (target.maxChunkZ - target.minChunkZ + 1);
            }
            return total;
        }

        private long unregisterProgress() {
            long completed = 0L;
            for (int index = 0; index < unregisterTargets.size(); index++) {
                UnregisterTarget target = unregisterTargets.get(index);
                long targetTotal = (long) (target.maxChunkX - target.minChunkX + 1)
                        * (target.maxChunkZ - target.minChunkZ + 1);
                if (index < unregisterTargetIndex) completed += targetTotal;
                else if (index == unregisterTargetIndex) completed += Math.min(targetTotal, unregisterChunkIndex);
            }
            return completed;
        }

        private long entityTotal() {
            long total = 0L;
            for (EntityTarget target : entityTargets) total += entityChunkCount(target);
            return total;
        }

        private long entityProgress() {
            long completed = 0L;
            for (int index = 0; index < entityTargets.size(); index++) {
                long targetTotal = entityChunkCount(entityTargets.get(index));
                if (index < entityTargetIndex) completed += targetTotal;
                else if (index == entityTargetIndex) completed += Math.min(targetTotal, entityChunkIndex);
            }
            return completed;
        }

        private long entityChunkCount(EntityTarget target) {
            long width = SectionPos.blockToSectionCoord(target.origin.getX() + INSTANCE_RADIUS)
                    - SectionPos.blockToSectionCoord(target.origin.getX() - INSTANCE_RADIUS) + 1L;
            long depth = SectionPos.blockToSectionCoord(target.origin.getZ() + INSTANCE_RADIUS)
                    - SectionPos.blockToSectionCoord(target.origin.getZ() - INSTANCE_RADIUS) + 1L;
            return width * depth;
        }

        private long clearTotal() {
            long total = 0L;
            for (ClearTarget target : clearTargets) total += target.bounds.volume();
            return total;
        }

        private long clearProgress() {
            long completed = 0L;
            for (int index = 0; index < clearTargets.size(); index++) {
                long targetTotal = clearTargets.get(index).bounds.volume();
                if (index < clearTargetIndex) completed += targetTotal;
                else if (index == clearTargetIndex) completed += Math.min(targetTotal, clearBlockIndex);
            }
            return completed;
        }

        private long resetChunkCount() {
            long width = SectionPos.blockToSectionCoord(resetBounds.maxX)
                    - SectionPos.blockToSectionCoord(resetBounds.minX) + 1L;
            long depth = SectionPos.blockToSectionCoord(resetBounds.maxZ)
                    - SectionPos.blockToSectionCoord(resetBounds.minZ) + 1L;
            return width * depth;
        }

        private boolean processPreview(MinecraftServer server, ServerPlayer player, long deadline) {
            if (player == null) return true;
        if (previewSources == null) previewSources = sourceLevels(server).stream().filter(level -> supportsStructure(level, structure)).toList();
            ChunkPos targetChunk = new ChunkPos(origin);
            int checked = 0;
            int generated = 0;
            while (previewSourceIndex < previewSources.size()
                    && checked < STRUCTURE_PLACEMENT_CHECKS_PER_TICK && generated < 1
                    && (checked == 0 || System.nanoTime() < deadline)) {
                ServerLevel source = previewSources.get(previewSourceIndex);
                if (previewPlacementSourceIndex != previewSourceIndex) {
                    Registry<Structure> registry = server.registryAccess().registryOrThrow(Registries.STRUCTURE);
                    previewPlacements = source.getChunkSource().getGeneratorState()
                            .getPlacementsForStructure(registry.wrapAsHolder(structure));
                    BlockPos centerPos = Level.OVERWORLD.equals(source.dimension())
                            ? source.getSharedSpawnPos() : BlockPos.ZERO;
                    previewUsesKnownPlacement = usesKnownPlacement(previewPlacements);
                    previewCandidates = previewUsesKnownPlacement
                            ? placementCandidates(source, previewPlacements, new ChunkPos(centerPos)) : List.of();
                    previewPlacementSourceIndex = previewSourceIndex;
                    previewProbeIndex = 0L;
                    previewGenerationAttempts = 0;
                }
                BlockPos centerPos = Level.OVERWORLD.equals(source.dimension()) ? source.getSharedSpawnPos() : BlockPos.ZERO;
                ChunkPos center = new ChunkPos(centerPos);
                long candidateCount = previewUsesKnownPlacement ? previewCandidates.size() : previewChunkCount();
                if (previewProbeIndex >= candidateCount
                        || previewGenerationAttempts >= STRUCTURE_PREVIEW_PROBES) {
                    previewSourceIndex++;
                    continue;
                }
                ChunkPos candidateChunk = previewUsesKnownPlacement
                        ? previewCandidates.get((int) previewProbeIndex++)
                        : previewProbeChunk(center, previewProbeIndex++);
                checked++;
                try {
                    boolean legal = previewPlacements.isEmpty()
                            || previewPlacements.stream().anyMatch(placement -> placement.isStructureChunk(
                            source.getChunkSource().getGeneratorState(), candidateChunk.x, candidateChunk.z));
                    if (!legal) continue;
                    previewGenerationAttempts++;
                    generated++;
                    StructureStart candidate = generateStructureAt(source, structure, candidateChunk);
                    if (candidate.isValid()) {
                        sourcePreview = relocatePreview(candidate, structure, source, targetChunk);
                        stage = BuildStage.PREPARE;
                        return false;
                    }
                } catch (RuntimeException exception) {
                    LOGGER.debug("Unable to preview structure {} in {} at chunk {}",
                            structureId, source.dimension().location(), candidateChunk, exception);
                }
            }
            return previewSourceIndex >= previewSources.size()
                    && abort(player, "message.dealt_force_skills.hvk_asara_arena.place_failed");
        }

        private boolean prepare(ServerPlayer player) {
            preview = sourcePreview.start;
            arena = player.server.getLevel(sourcePreview.arenaDimension);
            if (arena == null) {
                return abort(player, "message.dealt_force_skills.hvk_asara_arena.missing_dimension");
            }
            if (!preview.isValid() || !moveStructureToArenaHeight(arena, preview)) {
                return abort(player, "message.dealt_force_skills.hvk_asara_arena.place_failed");
            }
            structureBox = preview.getBoundingBox();
            if (structureChunkCount(structureBox) > MAX_STRUCTURE_CHUNKS) {
                return abort(player, "message.dealt_force_skills.hvk_asara_arena.place_failed");
            }
            structureBounds = structureBounds(structureBox);
            arrival = arrivalPoint(arena, origin, structureBounds);
            if (arrival == null || !canPrepareTerrain(arena, structureBox, arrival)) {
                return abort(player, "message.dealt_force_skills.hvk_asara_arena.place_failed");
            }
            resetBounds = resetBounds(arena, structureBox, arrival);
            CompoundTag data = player.getPersistentData();
            if (data.contains(INSTANCE_STATE)) {
                CompoundTag previousTag = data.getCompound(INSTANCE_STATE);
                ServerLevel previousLevel = savedArenaLevel(player.server, previousTag);
                ResetBounds previous = ResetBounds.load(previousTag);
                if (previousLevel != null) {
                    addUnregisterTarget(player.server, previousLevel, previousTag);
                    addEntityTarget(previousLevel, savedOrigin(previousTag, origin));
                    if (previous != null && previous.volume() <= MAX_RESET_BLOCKS) {
                        clearTargets.add(new ClearTarget(previousLevel, previous));
                    }
                }
            }
            if (clearTargets.stream().noneMatch(target -> target.level == arena && target.bounds.equals(resetBounds))) {
                clearTargets.add(new ClearTarget(arena, resetBounds));
            }
            addEntityTarget(arena, origin);
            var generator = sourcePreview.source.getChunkSource().getGenerator();
            var randomState = sourcePreview.source.getChunkSource().randomState();
            offsetX = structureBox.minX() - sourcePreview.sourceBox.minX();
            offsetZ = structureBox.minZ() - sourcePreview.sourceBox.minZ();
            offsetY = structureBox.minY() - 1
                    - naturalSurfaceY(sourcePreview.source, sourcePreview.sourceBox, generator, randomState);
            structureRandom = RandomSource.create(arena.getSeed() ^ preview.getChunkPos().toLong());
            stage = BuildStage.UNREGISTER;
            return false;
        }

        private void addUnregisterTarget(MinecraftServer server, ServerLevel level, CompoundTag tag) {
            ResourceLocation oldStructureId = ResourceLocation.tryParse(tag.getString("Structure"));
            if (oldStructureId == null) return;
            Structure oldStructure = server.registryAccess().registryOrThrow(Registries.STRUCTURE).get(oldStructureId);
            int minChunkX = tag.getInt("MinChunkX");
            int maxChunkX = tag.getInt("MaxChunkX");
            int minChunkZ = tag.getInt("MinChunkZ");
            int maxChunkZ = tag.getInt("MaxChunkZ");
            long chunkCount = (long) (maxChunkX - minChunkX + 1) * (maxChunkZ - minChunkZ + 1);
            if (oldStructure != null && chunkCount > 0L && chunkCount <= MAX_STRUCTURE_CHUNKS) {
                unregisterTargets.add(new UnregisterTarget(level, oldStructure,
                        new ChunkPos(tag.getInt("StartChunkX"), tag.getInt("StartChunkZ")),
                        minChunkX, maxChunkX, minChunkZ, maxChunkZ));
            }
        }

        private void addEntityTarget(ServerLevel level, BlockPos targetOrigin) {
            if (entityTargets.stream().noneMatch(target -> target.level == level && target.origin.equals(targetOrigin))) {
                entityTargets.add(new EntityTarget(level, targetOrigin));
            }
        }

        private boolean processUnregistration(long deadline) {
            int processed = 0;
            while (unregisterTargetIndex < unregisterTargets.size() && processed < BUILD_CHUNKS_PER_TICK
                    && (processed == 0 || System.nanoTime() < deadline)) {
                UnregisterTarget target = unregisterTargets.get(unregisterTargetIndex);
                int width = target.maxChunkX - target.minChunkX + 1;
                int depth = target.maxChunkZ - target.minChunkZ + 1;
                int total = Math.max(0, width * depth);
                if (unregisterChunkIndex == 0) {
                    LevelChunk startChunk = target.level.getChunk(target.startChunk.x, target.startChunk.z);
                    Map<Structure, StructureStart> starts = new HashMap<>(startChunk.getAllStarts());
                    if (starts.remove(target.structure) != null) {
                        startChunk.setAllStarts(starts);
                        startChunk.setUnsaved(true);
                    }
                }
                if (unregisterChunkIndex >= total) {
                    unregisterTargetIndex++;
                    unregisterChunkIndex = 0;
                    continue;
                }
                int chunkX = target.minChunkX + unregisterChunkIndex / depth;
                int chunkZ = target.minChunkZ + unregisterChunkIndex % depth;
                unregisterChunkIndex++;
                processed++;
                LevelChunk chunk = target.level.getChunk(chunkX, chunkZ);
                var references = chunk.getAllReferences().get(target.structure);
                long reference = target.startChunk.toLong();
                if (references != null && references.contains(reference)) {
                    var referencesByStructure = new HashMap<>(chunk.getAllReferences());
                    var updatedReferences = new LongOpenHashSet(references);
                    updatedReferences.remove(reference);
                    if (updatedReferences.isEmpty()) referencesByStructure.remove(target.structure);
                    else referencesByStructure.put(target.structure, updatedReferences);
                    chunk.setAllReferences(referencesByStructure);
                    chunk.setUnsaved(true);
                }
            }
            if (unregisterTargetIndex >= unregisterTargets.size()) stage = BuildStage.ENTITIES;
            return false;
        }

        private boolean processEntities(long deadline) {
            int processed = 0;
            while (entityTargetIndex < entityTargets.size() && processed < BUILD_ENTITY_CHUNKS_PER_TICK
                    && (processed == 0 || System.nanoTime() < deadline)) {
                EntityTarget target = entityTargets.get(entityTargetIndex);
                int minX = target.origin.getX() - INSTANCE_RADIUS;
                int minZ = target.origin.getZ() - INSTANCE_RADIUS;
                int maxX = target.origin.getX() + INSTANCE_RADIUS;
                int maxZ = target.origin.getZ() + INSTANCE_RADIUS;
                int minChunkX = SectionPos.blockToSectionCoord(minX);
                int minChunkZ = SectionPos.blockToSectionCoord(minZ);
                int width = SectionPos.blockToSectionCoord(maxX) - minChunkX + 1;
                int depth = SectionPos.blockToSectionCoord(maxZ) - minChunkZ + 1;
                int total = width * depth;
                if (entityChunkIndex >= total) {
                    entityTargetIndex++;
                    entityChunkIndex = 0;
                    continue;
                }
                int chunkX = minChunkX + entityChunkIndex / depth;
                int chunkZ = minChunkZ + entityChunkIndex % depth;
                entityChunkIndex++;
                processed++;
        AABB area = new AABB(Math.max(minX, chunkX << 4), target.level.getMinBuildHeight(),
                Math.max(minZ, chunkZ << 4), Math.min(maxX + 1.0D, (chunkX << 4) + 16.0D),
                target.level.getMaxBuildHeight(), Math.min(maxZ + 1.0D, (chunkZ << 4) + 16.0D));
        target.level.getChunk(chunkX, chunkZ);
        for (Entity entity : target.level.getEntities((Entity) null, area, entity -> !(entity instanceof Player))) {
                    entity.discard();
                }
            }
            if (entityTargetIndex >= entityTargets.size()) stage = BuildStage.CLEAR;
            return false;
        }

        private boolean processClear(long deadline) {
            int processed = 0;
            while (clearTargetIndex < clearTargets.size() && processed < BUILD_BLOCKS_PER_TICK
                    && (processed == 0 || System.nanoTime() < deadline)) {
                ClearTarget target = clearTargets.get(clearTargetIndex);
                long volume = target.bounds.volume();
                if (clearBlockIndex >= volume) {
                    clearTargetIndex++;
                    clearBlockIndex = 0L;
                    clearLoadedLevel = null;
                    continue;
                }
                BlockPos pos = positionAt(target.bounds, clearBlockIndex++);
                int chunkX = pos.getX() >> 4;
                int chunkZ = pos.getZ() >> 4;
                if (clearLoadedLevel != target.level || clearLoadedChunkX != chunkX || clearLoadedChunkZ != chunkZ) {
                    target.level.getChunk(chunkX, chunkZ);
                    clearLoadedLevel = target.level;
                    clearLoadedChunkX = chunkX;
                    clearLoadedChunkZ = chunkZ;
                }
                if (!target.level.getBlockState(pos).isAir()) {
                    target.level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 18);
                }
                processed++;
            }
            if (clearTargetIndex >= clearTargets.size()) {
                stage = cleanupOnly ? BuildStage.CANCELLED : BuildStage.TERRAIN;
            }
            return false;
        }

        private boolean processTerrain(long deadline) {
            int processed = 0;
            int height = resetBounds.maxY - resetBounds.minY + 1;
            int depth = resetBounds.maxZ - resetBounds.minZ + 1;
            long volume = resetBounds.volume();
            var source = sourcePreview.source;
            var generator = source.getChunkSource().getGenerator();
            var randomState = source.getChunkSource().randomState();
            while (terrainBlockIndex < volume && processed < BUILD_BLOCKS_PER_TICK
                    && (processed == 0 || System.nanoTime() < deadline)) {
                long columnIndex = terrainBlockIndex / height;
                int x = resetBounds.minX + (int) (columnIndex / depth);
                int z = resetBounds.minZ + (int) (columnIndex % depth);
                int y = resetBounds.minY + (int) (terrainBlockIndex % height);
                terrainBlockIndex++;
                int sourceX = x - offsetX;
                int sourceZ = z - offsetZ;
                if (terrainColumn == null || terrainColumnX != sourceX || terrainColumnZ != sourceZ) {
                    terrainColumn = generator.getBaseColumn(sourceX, sourceZ, source, randomState);
                    terrainColumnX = sourceX;
                    terrainColumnZ = sourceZ;
                }
                int sourceY = y - offsetY;
                if (sourceY >= source.getMinBuildHeight() && sourceY < source.getMaxBuildHeight()) {
                    BlockState state = terrainColumn.getBlock(sourceY);
                    if (!state.isAir()) {
                        arena.setBlock(new BlockPos(x, y, z), state, 18);
                    }
                }
                processed++;
            }
            if (terrainBlockIndex >= volume) {
                stage = BuildStage.BIOMES;
            }
            return false;
        }

        private boolean processBiomes(long deadline) {
            int minChunkX = SectionPos.blockToSectionCoord(resetBounds.minX);
            int minChunkZ = SectionPos.blockToSectionCoord(resetBounds.minZ);
            int width = SectionPos.blockToSectionCoord(resetBounds.maxX) - minChunkX + 1;
            int depth = SectionPos.blockToSectionCoord(resetBounds.maxZ) - minChunkZ + 1;
            int total = width * depth;
            var sampler = arena.getChunkSource().randomState().sampler();
            for (int count = 0; count < BUILD_CHUNKS_PER_TICK && biomeChunkIndex < total
                    && (count == 0 || System.nanoTime() < deadline); count++) {
                int chunkX = minChunkX + biomeChunkIndex / depth;
                int chunkZ = minChunkZ + biomeChunkIndex % depth;
                biomeChunkIndex++;
                LevelChunk chunk = arena.getChunk(chunkX, chunkZ);
                chunk.fillBiomesFromNoise((quartX, quartY, quartZ, ignored) -> sourcePreview.source.getNoiseBiome(
                        QuartPos.fromBlock(QuartPos.toBlock(quartX) - offsetX),
                        QuartPos.fromBlock(QuartPos.toBlock(quartY) - offsetY),
                        QuartPos.fromBlock(QuartPos.toBlock(quartZ) - offsetZ)), sampler);
                chunk.setUnsaved(true);
            }
            if (biomeChunkIndex >= total) {
                stage = BuildStage.STRUCTURE;
            }
            return false;
        }

        private boolean processStructure(long deadline) {
            int minChunkX = SectionPos.blockToSectionCoord(structureBox.minX());
            int minChunkZ = SectionPos.blockToSectionCoord(structureBox.minZ());
            int width = SectionPos.blockToSectionCoord(structureBox.maxX()) - minChunkX + 1;
            int depth = SectionPos.blockToSectionCoord(structureBox.maxZ()) - minChunkZ + 1;
            int total = width * depth;
            for (int count = 0; count < BUILD_CHUNKS_PER_TICK && structureChunkIndex < total
                    && (count == 0 || System.nanoTime() < deadline); count++) {
                int chunkX = minChunkX + structureChunkIndex / depth;
                int chunkZ = minChunkZ + structureChunkIndex % depth;
                structureChunkIndex++;
                ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
                BoundingBox chunkBox = new BoundingBox(chunkPos.getMinBlockX(), arena.getMinBuildHeight(),
                        chunkPos.getMinBlockZ(), chunkPos.getMaxBlockX(), arena.getMaxBuildHeight() - 1,
                        chunkPos.getMaxBlockZ());
                preview.placeInChunk(arena, arena.structureManager(), arena.getChunkSource().getGenerator(),
                        structureRandom, chunkBox, chunkPos);
            }
            if (structureChunkIndex >= total) {
                stage = BuildStage.REGISTER;
            }
            return false;
        }

        private boolean processRegistration(long deadline) {
            ChunkPos startChunkPos = preview.getChunkPos();
            if (!startRegistered) {
                LevelChunk startChunk = arena.getChunk(startChunkPos.x, startChunkPos.z);
                arena.structureManager().setStartForStructure(SectionPos.of(startChunkPos, arena.getMinSection()),
                        structure, preview, startChunk);
                startRegistered = true;
            }
            int minChunkX = SectionPos.blockToSectionCoord(structureBox.minX());
            int minChunkZ = SectionPos.blockToSectionCoord(structureBox.minZ());
            int width = SectionPos.blockToSectionCoord(structureBox.maxX()) - minChunkX + 1;
            int depth = SectionPos.blockToSectionCoord(structureBox.maxZ()) - minChunkZ + 1;
            int total = width * depth;
            long reference = startChunkPos.toLong();
            for (int count = 0; count < BUILD_CHUNKS_PER_TICK && referenceChunkIndex < total
                    && (count == 0 || System.nanoTime() < deadline); count++) {
                int chunkX = minChunkX + referenceChunkIndex / depth;
                int chunkZ = minChunkZ + referenceChunkIndex % depth;
                referenceChunkIndex++;
                ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
                LevelChunk chunk = arena.getChunk(chunkX, chunkZ);
                arena.structureManager().addReferenceForStructure(SectionPos.of(chunkPos, arena.getMinSection()),
                        structure, reference, chunk);
            }
            if (referenceChunkIndex >= total) {
                stage = BuildStage.COMPLETE;
            }
            return false;
        }

        private boolean complete(ServerPlayer player) {
            prepareArrivalAccess(arena, structureBox, arrival, arenaFloor(sourcePreview.arenaDimension));
            saveInstance(player, arena, sourcePreview.source, resetBounds, structureId, preview.getChunkPos(),
                    structureBox, origin);
            long now = arena.getGameTime();
            Session session = new Session(UUID.randomUUID(), playerId, arena.dimension(), origin, arrival,
                    structureBounds, pieceBounds(preview), structureId, lookYaw(arrival, structureBounds), returnPoint,
                    mask, HvkAsaraArenaModifier.score(mask), now, now + NATURAL_SPAWN_TICKS,
                    now + challengeTicks(mask), 0L);
            SESSIONS.put(playerId, session);
            player.getPersistentData().put(ACTIVE_RETURN, returnPoint.save());
            player.teleportTo(arena, arrival.getX() + 0.5D, arrival.getY() + 0.1D, arrival.getZ() + 0.5D,
                    session.arrivalYaw, 0.0F);
            player.addEffect(new MobEffectInstance(MobEffects.GLOWING, Integer.MAX_VALUE, 0, false, false, true));
            player.displayClientMessage(Component.translatable(
                    "message.dealt_force_skills.hvk_asara_arena.started", session.score), false);
            arena.playSound(null, player.blockPosition(), ModSounds.HVK_ASARA_ARENA_START.get(),
                    SoundSource.BLOCKS, 1.0F, 1.0F);
            return true;
        }

        private void rollbackPlayer(MinecraftServer server, ServerPlayer player) {
            SESSIONS.remove(playerId);
            if (player == null) return;
            if (arena != null && resetBounds != null) player.getPersistentData().remove(INSTANCE_STATE);
            player.removeEffect(MobEffects.GLOWING);
            ServerLevel returnLevel = server.getLevel(returnPoint.dimension);
            if (returnLevel == null) returnLevel = server.overworld();
            player.teleportTo(returnLevel, returnPoint.x, returnPoint.y, returnPoint.z,
                    returnPoint.yRot, returnPoint.xRot);
        }

        private boolean abort(ServerPlayer player, String messageKey) {
            player.displayClientMessage(Component.translatable(messageKey, structureId.toString()), false);
            return true;
        }

        private void cancel() {
            if (cleanupOnly) return;
            if (!beginFailureCleanup()) stage = BuildStage.CANCELLED;
        }

        private boolean beginFailureCleanup() {
            if (cleanupOnly) {
                skipFailedCleanupUnit();
                return true;
            }
            cleanupOnly = true;
            if (arena == null || resetBounds == null) {
                stage = BuildStage.CANCELLED;
                return false;
            }
            if (startRegistered) {
                unregisterTargets.add(new UnregisterTarget(arena, structure, preview.getChunkPos(),
                        SectionPos.blockToSectionCoord(structureBox.minX()),
                        SectionPos.blockToSectionCoord(structureBox.maxX()),
                        SectionPos.blockToSectionCoord(structureBox.minZ()),
                        SectionPos.blockToSectionCoord(structureBox.maxZ())));
            }
            if (clearTargets.stream().noneMatch(target -> target.level == arena && target.bounds.equals(resetBounds))) {
                clearTargets.add(new ClearTarget(arena, resetBounds));
            }
            addEntityTarget(arena, origin);
            unregisterTargetIndex = unregisterChunkIndex = entityTargetIndex = entityChunkIndex = clearTargetIndex = 0;
            clearBlockIndex = 0L;
            clearLoadedLevel = null;
            stage = BuildStage.UNREGISTER;
            return true;
        }

        private void skipFailedCleanupUnit() {
            switch (stage) {
                case UNREGISTER -> unregisterChunkIndex++;
                case ENTITIES -> entityChunkIndex++;
                case CLEAR -> clearBlockIndex++;
                default -> stage = BuildStage.CANCELLED;
            }
        }

        private static long structureChunkCount(BoundingBox box) {
            return (long) (SectionPos.blockToSectionCoord(box.maxX())
                    - SectionPos.blockToSectionCoord(box.minX()) + 1)
                    * (SectionPos.blockToSectionCoord(box.maxZ())
                    - SectionPos.blockToSectionCoord(box.minZ()) + 1);
        }

        private static BlockPos positionAt(ResetBounds bounds, long index) {
            int height = bounds.maxY - bounds.minY + 1;
            int depth = bounds.maxZ - bounds.minZ + 1;
            long column = index / height;
            return new BlockPos(bounds.minX + (int) (column / depth),
                    bounds.minY + (int) (index % height),
                    bounds.minZ + (int) (column % depth));
        }
    }


    private static final class Session {
        private final UUID id;
        private final UUID playerId;
        private final ResourceKey<Level> arenaDimension;
        private final BlockPos origin;
        private final BlockPos arrival;
        private final AABB structureBounds;
        private final List<BoundingBox> structurePieces;
        private final ResourceLocation structureId;
        private final AABB bounds;
        private final float arrivalYaw;
        private final ReturnPoint returnPoint;
        private final Set<HvkAsaraArenaModifier> modifiers;
        private final Set<UUID> challengeHostiles = new HashSet<>();
        private final int score;
        private final long startedAt;
        private final long spawnEndAt;
        private final long timeoutAt;
        private long successReturnAt;
        private boolean challengeEligibilityReported;
        private boolean challengeEligible;
        private double lastX;
        private double lastY;
        private double lastZ;
        private long nextMovementColdAt;
    private int structureSpawnCount;
    private long nextHudSyncAt;

        private Session(UUID id, UUID playerId, ResourceKey<Level> arenaDimension, BlockPos origin,
                        BlockPos arrival, AABB structureBounds, List<BoundingBox> structurePieces,
                        ResourceLocation structureId,
                        float arrivalYaw, ReturnPoint returnPoint, long mask, int score,
                        long startedAt, long spawnEndAt, long timeoutAt, long successReturnAt) {
            this.id = id;
            this.playerId = playerId;
            this.arenaDimension = arenaDimension;
            this.origin = origin;
            this.arrival = arrival;
            this.structureBounds = structureBounds;
            this.structurePieces = List.copyOf(structurePieces);
            this.structureId = structureId;
            this.bounds = new AABB(origin.getX() - INSTANCE_RADIUS,
                    Math.min(origin.getY() - 96.0D, structureBounds.minY - 96.0D),
                    origin.getZ() - INSTANCE_RADIUS, origin.getX() + INSTANCE_RADIUS + 1.0D,
                    Math.max(origin.getY() + 96.0D, structureBounds.maxY + 96.0D),
                    origin.getZ() + INSTANCE_RADIUS + 1.0D);
            this.arrivalYaw = arrivalYaw;
            this.returnPoint = returnPoint;
            this.modifiers = EnumSet.copyOf(HvkAsaraArenaModifier.fromMask(mask));
            this.score = score;
            this.startedAt = startedAt;
            this.spawnEndAt = spawnEndAt;
            this.timeoutAt = timeoutAt;
            this.successReturnAt = successReturnAt;
            this.lastX = arrival.getX() + 0.5D;
            this.lastY = arrival.getY() + 0.1D;
            this.lastZ = arrival.getZ() + 0.5D;
        }
    }

    private record ReturnPoint(ResourceKey<Level> dimension, double x, double y, double z, float yRot, float xRot) {
        private CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putString("Dimension", dimension.location().toString());
            tag.putDouble("X", x);
            tag.putDouble("Y", y);
            tag.putDouble("Z", z);
            tag.putFloat("YRot", yRot);
            tag.putFloat("XRot", xRot);
            return tag;
        }

        private static ReturnPoint load(CompoundTag tag) {
            ResourceLocation id = ResourceLocation.tryParse(tag.getString("Dimension"));
            if (id == null) return null;
            return new ReturnPoint(ResourceKey.create(Registries.DIMENSION, id), tag.getDouble("X"),
                    tag.getDouble("Y"), tag.getDouble("Z"), tag.getFloat("YRot"), tag.getFloat("XRot"));
        }
    }
}
