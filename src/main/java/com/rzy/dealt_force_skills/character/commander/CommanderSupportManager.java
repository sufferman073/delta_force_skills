package com.rzy.dealt_force_skills.character.commander;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.config.DealtItemsHavocConfig;
import com.rzy.dealt_force_skills.compat.SuperbWarfareCompat;
import com.rzy.dealt_force_skills.compat.TrainingTargetCompat;
import com.rzy.dealt_force_skills.entity.ToxikTearGasCloudEntity;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.S2C_CommanderVehicleState;
import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.registry.ModEntities;
import com.rzy.dealt_force_skills.registry.ModItems;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.skill.SkillDamageHelper;
import com.rzy.dealt_force_skills.team.DealtTeamManager;
import com.rzy.dealt_force_skills.util.TargetingUtil;
import com.rzy.dealt_force_skills.util.RangedSoundHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class CommanderSupportManager {
    private static final int MISSILE_WARNING_TICKS = 12 * 20;
    private static final long RADIATION_DURATION_TICKS = 10L * 60L * 20L;
    private static final int COMMANDER_SMOKE_RING_CLOUDS = 8;
    private static final double COMMANDER_SMOKE_RING_RADIUS = 8.0D;
    private static final String MARK_UNTIL = DealtForceSkillsMod.MODID + ".commander_mark_until";
    private static final String MARK_STACKS = DealtForceSkillsMod.MODID + ".commander_mark_stacks";
    private static final String MARK_OWNER = DealtForceSkillsMod.MODID + ".commander_mark_owner";
    private static final String VEHICLE_TAG = DealtForceSkillsMod.MODID + ".commander_vehicle";
    private static final String FULL_INVISIBILITY_UNTIL = "CommanderFullInvisibilityUntil";
    private static final String FULL_INVISIBILITY_RUNTIME = "CommanderFullInvisibilityRuntime";
    private static final Map<UUID, List<Zone>> ZONES = new HashMap<>();
    private static final List<Impact> IMPACTS = new ArrayList<>();
    private static final List<RadiationZone> RADIATION = new ArrayList<>();
    private static final Map<ServerLevel, java.util.Set<BlockPos>> SUPPLY_CHESTS = new HashMap<>();
    private static MinecraftServer loadedServer;
    private static CommanderSupportSavedData savedData;

    private CommanderSupportManager() {
    }

    public static boolean use(ServerPlayer player, CommanderSupport support, Vec3 position, int targetId) {
        return use(player, support, position, targetId, "");
    }

    public static boolean use(ServerPlayer player, CommanderSupport support, Vec3 position, int targetId,
                              String vehicleId) {
        if (!CommanderStateManager.isCommander(player)) {
            return false;
        }
        CommanderStateManager.initializeIfNeeded(player);
        ensureLoaded(player.server);
        if (CommanderStateManager.supportCooldownTicks(player, support) > 0) {
            player.displayClientMessage(Component.translatable(
                    "message.dealt_force_skills.commander.support_cooldown",
                    supportCooldownSeconds(player, support)), true);
            return false;
        }
        if (support == CommanderSupport.VEHICLE_AIRDROP && !SuperbWarfareCompat.isLoaded()) {
            player.displayClientMessage(Component.translatable(
                    "message.dealt_force_skills.commander.vehicle_unavailable"), true);
            return false;
        }
        ServerLevel level = player.serverLevel();
        Vec3 target = support.targeted() ? validateTarget(player, position) : player.position();
        if (support.targeted() && target == null) {
            player.displayClientMessage(Component.translatable(
                    "message.dealt_force_skills.commander.support_mark_invalid"), true);
            return false;
        }
        if (!canExecute(player, support, targetId, target, vehicleId)) {
            sendSupportFailed(player, support);
            return false;
        }
        if (!CommanderStateManager.beginSupportCooldown(player, support)) {
            return false;
        }
        if (!CommanderStateManager.spendTacticalPoints(player, support.cost())) {
            CommanderStateManager.cancelSupportCooldown(player, support);
            player.displayClientMessage(Component.translatable(
                    "message.dealt_force_skills.commander.points_missing", support.cost()), true);
            return false;
        }
        boolean applied;
        switch (support) {
            case HIGH_THREAT_MARK -> applied = markTarget(player, targetId);
            case AREA_SMOKE -> {
                deployToxikSmoke(player, target);
                applied = true;
            }
            case MEDICAL_SUPPORT -> {
                applyMedicalSupport(player);
                applied = true;
            }
            case ARTILLERY_POSITION -> {
                addZone(player, ZoneType.ARTILLERY, target, 60 * 20L);
                applied = true;
            }
            case MANNED_MEDICAL_DRONE -> {
                deployMedicalDrone(player, target);
                applied = true;
            }
            case VEHICLE_AIRDROP -> applied = dropVehicle(player, target, vehicleId);
            case TACTICAL_RESPAWN_BEACON -> {
                deployBeacon(player, target);
                applied = true;
            }
            case ADS_FIELD_COVER -> {
                CommanderStateManager.setAdsCoverUntil(player, now(player) + 60 * 20L);
                applied = true;
            }
            case EQUIPMENT_SUPPLY -> applied = dropEquipmentSupply(player, target);
            case LIGHTNING_STORM -> {
                addZone(player, ZoneType.LIGHTNING, target, 90 * 20L);
                applied = true;
            }
            case QUICK_REVIVE -> {
                CommanderStateManager.setQuickReviveUntil(player, now(player) + 60 * 20L);
                applied = true;
            }
            case GUIDED_MISSILE -> {
                scheduleMissile(player, target, false);
                applied = true;
            }
            case TACTICAL_NUCLEAR_STRIKE -> {
                scheduleMissile(player, target, true);
                applied = true;
            }
            default -> applied = true;
        }
        if (!applied) {
            CommanderStateManager.cancelSupportCooldown(player, support);
            CommanderStateManager.refundTacticalPoints(player, support.cost());
            sendSupportFailed(player, support);
            return false;
        }
        playSupportSound(player, support);
        emitSupportArrival(player, support, target == null ? player.position() : target);
        announceSupportApplied(player, support);
        CommanderStateManager.syncToClient(player);
        persist(player.server);
        return true;
    }

    private static void sendSupportFailed(ServerPlayer player, CommanderSupport support) {
        player.displayClientMessage(Component.translatable(
                "message.dealt_force_skills.commander.support_failed",
                Component.translatable(support.translationKey(), support.cost(), support.cooldownTicks() / 20)), true);
    }

    public static void tick(ServerPlayer player) {
        long now = now(player);
        long medicalUntil = CommanderStateManager.medicalSupportUntil(player);
        if (medicalUntil > now) {
            cleanse(player);
        }
        maintainFullInvisibility(player, now);
        if (player.tickCount % 5 == 0) {
            CommanderStateManager.syncToClient(player);
        }
    }

    public static void tickServer(net.minecraft.server.MinecraftServer server) {
        ensureLoaded(server);
        long now = server.overworld().getGameTime();
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                SuperbWarfareCompat.maintainCommanderVehicle(entity, VEHICLE_TAG);
                if (entity.tickCount % 10 == 0
                        && SuperbWarfareCompat.isCommanderVehicle(entity, VEHICLE_TAG)) {
                    NetworkHandler.sendToTrackingAndSelf(new S2C_CommanderVehicleState(
                            entity.getId(), entity.getUUID(), true), entity);
                }
            }
            if (now % 20L == 0L) {
                java.util.Set<BlockPos> chests = SUPPLY_CHESTS.get(level);
                if (chests != null) {
                    for (Iterator<BlockPos> iterator = chests.iterator(); iterator.hasNext();) {
                        BlockPos chestPos = iterator.next();
                        if (!(level.getBlockEntity(chestPos) instanceof ChestBlockEntity chest)
                                || chest.isEmpty()) {
                            if (level.getBlockState(chestPos).is(Blocks.CHEST)) {
                                level.removeBlock(chestPos, false);
                            }
                            iterator.remove();
                        }
                    }
                    if (chests.isEmpty()) {
                        SUPPLY_CHESTS.remove(level);
                    }
                }
            }
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            tick(player);
            if (now % 20L == 0L) {
                maintainMarkedTargets(player);
                showPersonalSupportIndicators(player, now);
            }
        }
        for (List<Zone> zones : ZONES.values()) {
            for (Iterator<Zone> iterator = zones.iterator(); iterator.hasNext();) {
                Zone zone = iterator.next();
                if (zone.endsAt() <= now) {
                    iterator.remove();
                    continue;
                }
                ServerPlayer owner = server.getPlayerList().getPlayer(zone.ownerId());
                if (owner == null) {
                    continue;
                }
                ServerLevel level = owner.serverLevel();
                if (!level.dimension().location().equals(zone.dimension())) {
                    continue;
                }
                if (now % 20L == 0L) {
                    maintainZone(owner, level, zone);
                }
                if (zone.type() == ZoneType.ARTILLERY && now >= zone.nextActionAt()) {
                    artilleryPulse(owner, level, zone);
                    zone.nextActionAt(now + 120L);
                } else if (zone.type() == ZoneType.LIGHTNING && now % 60L == 0L) {
                    lightningPulse(owner, level, zone.center());
                }
            }
        }
        for (Iterator<Impact> iterator = IMPACTS.iterator(); iterator.hasNext();) {
            Impact impact = iterator.next();
            if (impact.at() > now) {
                if (now % 20L == 0L) {
                    renderPendingImpact(server, impact);
                }
                continue;
            }
            ServerPlayer owner = server.getPlayerList().getPlayer(impact.ownerId());
            if (owner != null && owner.serverLevel().dimension().location().equals(impact.dimension())) {
                if (impact.type() == ImpactType.ARTILLERY) {
                    detonateArtillery(owner, owner.serverLevel(), impact.position());
                } else {
                    detonateMissile(owner, owner.serverLevel(), impact.position(),
                            impact.type() == ImpactType.NUCLEAR);
                }
            }
            iterator.remove();
        }
        tickRadiation(server, now);
        if (now % 20L == 0L) {
            persist(server);
        }
    }

    public static void syncTrackedVehicle(ServerPlayer watcher, Entity target) {
        if (watcher != null && SuperbWarfareCompat.isCommanderVehicle(target, VEHICLE_TAG)) {
            NetworkHandler.sendToPlayer(new S2C_CommanderVehicleState(
                    target.getId(), target.getUUID(), true), watcher);
        }
    }

    public static boolean isMarked(LivingEntity entity) {
        return entity.getPersistentData().getLong(MARK_UNTIL) > now(entity);
    }

    public static float markedDamageMultiplier(LivingEntity target) {
        if (!isMarked(target)) {
            return 1.0F;
        }
        int stacks = Math.max(0, target.getPersistentData().getInt(MARK_STACKS));
        target.getPersistentData().putInt(MARK_STACKS, stacks + 1);
        return 1.0F + (stacks + 1) * 0.10F;
    }

    public static UUID markedOwner(LivingEntity target) {
        return target.getPersistentData().hasUUID(MARK_OWNER)
                ? target.getPersistentData().getUUID(MARK_OWNER) : null;
    }

    public static boolean isAdsProtected(ServerPlayer player, Entity attacker, net.minecraft.world.damagesource.DamageSource source) {
        if (CommanderStateManager.adsCoverUntil(player) <= now(player)) {
            return false;
        }
        if (attacker != null && attacker.getClass().getName().startsWith("com.tacz.guns")) {
            return false;
        }
        return source == null || !source.getMsgId().contains("player");
    }

    public static boolean isAdsProtectedFor(ServerPlayer target, Entity attacker,
                                             net.minecraft.world.damagesource.DamageSource source) {
        if (target == null || target.server == null) {
            return false;
        }
        for (ServerPlayer owner : target.server.getPlayerList().getPlayers()) {
            if (!CommanderStateManager.isCommander(owner)
                    || (owner != target && !DealtTeamManager.areTeammates(owner, target))
                    || owner.distanceToSqr(target) > 15.0D * 15.0D) {
                continue;
            }
            if (isAdsProtected(owner, attacker, source)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isAdsEffectProtected(ServerPlayer target) {
        return isAdsProtectedFor(target, null, null);
    }

    public static boolean tryPreventFatal(ServerPlayer target,
                                          net.minecraft.world.damagesource.DamageSource source,
                                          float amount) {
        if (target == null || amount < target.getHealth()) {
            return false;
        }
        for (ServerPlayer owner : target.server.getPlayerList().getPlayers()) {
            if (!CommanderStateManager.isCommander(owner)
                    || (owner != target && !DealtTeamManager.areTeammates(owner, target))) {
                continue;
            }
            long now = now(owner);
            if (CommanderStateManager.quickReviveUntil(owner) > now
                    && (owner == target || owner.distanceToSqr(target) <= 15.0D * 15.0D)) {
                restorePlayer(target, null, 20, owner);
                return true;
            }
            var data = owner.getPersistentData();
            if (data.getLong("CommanderBeaconUntil") <= now
                    || data.getInt("CommanderBeaconUses") <= 0
                    || !owner.serverLevel().dimension().location().toString()
                    .equals(data.getString("CommanderBeaconDimension"))
                    || !target.serverLevel().dimension().location().toString()
                    .equals(data.getString("CommanderBeaconDimension"))) {
                continue;
            }
            BlockPos beacon = BlockPos.of(data.getLong("CommanderBeaconPosition"));
            data.putInt("CommanderBeaconUses", data.getInt("CommanderBeaconUses") - 1);
            if (data.getInt("CommanderBeaconUses") <= 0) {
                data.remove("CommanderBeaconUntil");
                data.remove("CommanderBeaconPosition");
                data.remove("CommanderBeaconDimension");
                data.remove("CommanderBeaconUses");
            }
            restorePlayer(target, beacon, 60, owner);
            return true;
        }
        return false;
    }

    public static void onCommanderKill(ServerPlayer killer, LivingEntity victim) {
        if (!CommanderStateManager.isCommander(killer)) {
            return;
        }
        if (CommanderStateManager.medicalSupportUntil(killer) > now(killer)) {
            killer.heal(killer.getMaxHealth() * 0.30F);
            killer.getFoodData().setFoodLevel(Math.min(20, killer.getFoodData().getFoodLevel() + 6));
            killer.getFoodData().setSaturation(Math.min(20.0F, killer.getFoodData().getSaturationLevel() + 6.0F));
        }
        if (markedOwner(victim) != null && killer.getUUID().equals(markedOwner(victim))) {
            net.minecraft.sounds.SoundEvent sound = switch (killer.getRandom().nextInt(3)) {
                case 1 -> ModSounds.COMMANDER_MARK_KILL_2.get();
                case 2 -> ModSounds.COMMANDER_MARK_KILL_3.get();
                default -> ModSounds.COMMANDER_MARK_KILL_1.get();
            };
            play(killer, sound, 1.0F, 1.0F);
        }
    }

    public static boolean isRadiated(Level level, BlockPos position) {
        ResourceLocation dimension = level.dimension().location();
        return RADIATION.stream().anyMatch(zone -> zone.dimension().equals(dimension)
                && zone.endsAt() > level.getGameTime()
                && zone.center().distanceToSqr(position.getCenter()) <= zone.radius() * zone.radius());
    }

    public static boolean shouldPreventSpawn(Level level, BlockPos position) {
        return isRadiated(level, position);
    }

    private static boolean markTarget(ServerPlayer owner, int targetId) {
        LivingEntity target = resolveTarget(owner, targetId);
        if (target == null) {
            owner.displayClientMessage(Component.translatable(
                    "message.dealt_force_skills.commander.target_missing"), true);
            return false;
        }
        target.getPersistentData().putLong(MARK_UNTIL, now(owner) + 600 * 20L);
        target.getPersistentData().putInt(MARK_STACKS, 0);
        target.getPersistentData().putUUID(MARK_OWNER, owner.getUUID());
        addSupportEffect(owner, target,
                new MobEffectInstance(MobEffects.GLOWING, 600 * 20, 0, false, true, true));
        addSupportEffect(owner, target,
                new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 600 * 20, 2, false, true, true));
        return true;
    }

    private static void deployToxikSmoke(ServerPlayer owner, Vec3 center) {
        spawnCommanderSmoke(owner, center);
        for (int i = 0; i < COMMANDER_SMOKE_RING_CLOUDS; i++) {
            double angle = Math.PI * 2.0D * i / COMMANDER_SMOKE_RING_CLOUDS;
            spawnCommanderSmoke(owner, center.add(Math.cos(angle) * COMMANDER_SMOKE_RING_RADIUS,
                    0.0D, Math.sin(angle) * COMMANDER_SMOKE_RING_RADIUS));
        }
    }

    private static void spawnCommanderSmoke(ServerPlayer owner, Vec3 center) {
        ToxikTearGasCloudEntity cloud = new ToxikTearGasCloudEntity(
                ModEntities.TOXIK_TEAR_GAS_CLOUD.get(), owner.serverLevel(), owner.getUUID(), 30 * 20);
        cloud.getPersistentData().putBoolean(ToxikTearGasCloudEntity.COMMANDER_REVEAL_TAG, true);
        cloud.setPos(center.x, center.y, center.z);
        owner.serverLevel().addFreshEntity(cloud);
    }

    private static void artilleryPulse(ServerPlayer owner, ServerLevel level, Zone zone) {
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class,
                new AABB(zone.center(), zone.center()).inflate(100.0D),
                entity -> TargetingUtil.isHostileLivingFor(owner, entity));
        if (targets.isEmpty()) {
            return;
        }
        int start = level.random.nextInt(targets.size());
        for (int i = 0; i < 18; i++) {
            LivingEntity target = targets.get((start + i) % targets.size());
            addSupportEffect(owner, target,
                    new MobEffectInstance(MobEffects.GLOWING, 140, 0, false, true, true));
            IMPACTS.add(new Impact(owner.getUUID(), level.dimension().location(), target.position(),
                    now(owner) + 30L, ImpactType.ARTILLERY));
        }
    }

    private static void detonateArtillery(ServerPlayer owner, ServerLevel level, Vec3 center) {
        sendParticlesLongDistance(level, center, 256.0D,
                net.minecraft.core.particles.ParticleTypes.FLASH, 2, 0.3D, 0.3D, 0.3D, 0.0D);
        sendParticlesLongDistance(level, center, 256.0D,
                net.minecraft.core.particles.ParticleTypes.EXPLOSION, 10, 1.8D, 1.2D, 1.8D, 0.06D);
        sendParticlesLongDistance(level, center, 256.0D,
                net.minecraft.core.particles.ParticleTypes.CLOUD, 28, 2.5D, 1.5D, 2.5D, 0.10D);
        sendParticlesLongDistance(level, center, 256.0D,
                net.minecraft.core.particles.ParticleTypes.FLAME, 36, 3.0D, 1.8D, 3.0D, 0.14D);
        sendParticlesLongDistance(level, center, 256.0D,
                net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER, 1,
                0.0D, 0.0D, 0.0D, 0.0D);
        sendRing(level, center, 6.0D, net.minecraft.core.particles.ParticleTypes.FLAME, 28);
        RangedSoundHelper.play(level, center, owner.getRandom().nextBoolean()
                        ? ModSounds.COMMANDER_EXPLOSION_1.get() : ModSounds.COMMANDER_EXPLOSION_2.get(),
                net.minecraft.sounds.SoundSource.PLAYERS, 1.5F, 1.0F, 256.0D);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(center, center).inflate(6.0D), entity -> TargetingUtil.isHostileLivingFor(owner, entity))) {
            float damage = Math.max(0.0F, 100.0F - (float) target.position().distanceTo(center) * 10.0F);
            if (damage > 0.0F) {
                target.hurt(level.damageSources().explosion(owner, owner), damage);
            }
        }
    }

    private static void lightningPulse(ServerPlayer owner, ServerLevel level, Vec3 center) {
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(center, center).inflate(200.0D), entity -> TargetingUtil.isHostileLivingFor(owner, entity))) {
            BlockPos strikePos = target.blockPosition();
            net.minecraft.world.entity.LightningBolt bolt = net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.create(level);
            if (bolt == null) continue;
            bolt.moveTo(strikePos.getX() + 0.5D, strikePos.getY(), strikePos.getZ() + 0.5D);
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
            addSupportEffect(owner, target,
                    new MobEffectInstance(ModEffects.NOX_DELAYED_WOUND, 10 * 20, 0));
            addSupportEffect(owner, target,
                    new MobEffectInstance(ModEffects.CORROSION, 10 * 20, 0));
            addSupportEffect(owner, target,
                    new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10 * 20, 1));
            addSupportEffect(owner, target,
                    new MobEffectInstance(MobEffects.WEAKNESS, 10 * 20, 1));
            target.hurt(SkillDamageHelper.source(level, DamageTypes.LIGHTNING_BOLT, owner, owner), 16.0F);
            target.hurt(SkillDamageHelper.source(level, DamageTypes.ON_FIRE, owner, owner), 16.0F);
        }
        sendParticlesLongDistance(level, center, 512.0D,
                net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK,
                80, 200.0D, 8.0D, 200.0D, 0.1D);
    }

    private static void deployMedicalDrone(ServerPlayer owner, Vec3 target) {
        List<ServerPlayer> team = team(owner);
        for (ServerPlayer member : team) {
            if (member.distanceToSqr(owner) > 4.0D * 4.0D && member != owner) continue;
            teleport(member, target);
            grantFullInvisibility(member, 10 * 20, owner);
            applyMedical(member);
            CommanderStateManager.setMedicalSupportUntil(member, now(owner) + 120L * 20L);
        }
        RangedSoundHelper.stopForPlayer(owner, ModSounds.COMMANDER_DRONE_START.get(),
                net.minecraft.sounds.SoundSource.PLAYERS);
        RangedSoundHelper.playForPlayer(owner, ModSounds.COMMANDER_DRONE_LAND.get(),
                net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private static boolean dropVehicle(ServerPlayer owner, Vec3 target, String vehicleId) {
        EntityType<?> type = SuperbWarfareCompat.registeredVehicleType(owner.serverLevel(), vehicleId);
        if (type == null) {
            owner.displayClientMessage(Component.translatable(
                    "message.dealt_force_skills.commander.vehicle_unavailable"), true);
            return false;
        }
        BlockPos containerPos = findPlace(owner.serverLevel(), BlockPos.containing(target));
        if (SuperbWarfareCompat.spawnVehicleContainer(owner.serverLevel(), containerPos,
                net.minecraft.core.Direction.fromYRot(owner.getYRot()), type, VEHICLE_TAG)) {
            owner.serverLevel().sendParticles(net.minecraft.core.particles.ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    containerPos.getX() + 0.5D, containerPos.getY() + 0.4D, containerPos.getZ() + 0.5D,
                    42, 1.4D, 0.35D, 1.4D, 0.04D);
            owner.serverLevel().sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                    containerPos.getX() + 0.5D, containerPos.getY() + 1.0D, containerPos.getZ() + 0.5D,
                    24, 0.8D, 1.4D, 0.8D, 0.02D);
            owner.serverLevel().playSound(null, containerPos, ModSounds.COMMANDER_VEHICLE_READY.get(),
                    net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
            return true;
        }
        Entity vehicle = type.create(owner.serverLevel());
        if (vehicle == null) return false;
        vehicle.moveTo(target.x, target.y, target.z, owner.getYRot(), 0.0F);
        SuperbWarfareCompat.configureCommanderVehicle(vehicle, VEHICLE_TAG);
        owner.serverLevel().sendParticles(net.minecraft.core.particles.ParticleTypes.CAMPFIRE_COSY_SMOKE,
                target.x, target.y + 0.4D, target.z, 42, 1.4D, 0.35D, 1.4D, 0.04D);
        owner.serverLevel().sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                target.x, target.y + 1.0D, target.z, 24, 0.8D, 1.4D, 0.8D, 0.02D);
        owner.serverLevel().playSound(null, BlockPos.containing(target), ModSounds.COMMANDER_VEHICLE_READY.get(),
                net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
        owner.serverLevel().addFreshEntity(vehicle);
        return true;
    }

    private static void deployBeacon(ServerPlayer owner, Vec3 target) {
        for (ServerPlayer member : team(owner)) {
            clearBeaconData(member);
        }
        var data = owner.getPersistentData();
        data.putString("CommanderBeaconDimension", owner.serverLevel().dimension().location().toString());
        data.putLong("CommanderBeaconPosition", BlockPos.containing(target).asLong());
        data.putLong("CommanderBeaconUntil", now(owner) + 60 * 20L);
        data.putInt("CommanderBeaconUses", 100);
        sendParticlesLongDistance(owner.serverLevel(), target.add(0.0D, 1.0D, 0.0D), 512.0D,
                net.minecraft.core.particles.ParticleTypes.END_ROD,
                40, 1.0D, 1.8D, 1.0D, 0.05D);
    }

    private static void clearBeaconData(ServerPlayer player) {
        var data = player.getPersistentData();
        data.remove("CommanderBeaconUntil");
        data.remove("CommanderBeaconPosition");
        data.remove("CommanderBeaconDimension");
        data.remove("CommanderBeaconUses");
    }

    private static boolean dropEquipmentSupply(ServerPlayer owner, Vec3 target) {
        ServerLevel level = owner.serverLevel();
        BlockPos pos = findPlace(level, BlockPos.containing(target));
        net.minecraft.util.RandomSource random = owner.getRandom();
        List<Item> consumables = List.of(
                ModItems.SPECIAL_EQUIPMENT_SUPPLY.get(), ModItems.PROTOTYPE_MADNESS_COMPOUND.get(),
                ModItems.CRANBERRY_SPECIAL.get(), ModItems.MIST_MORNING.get(), ModItems.ASARA_STYLE.get(),
                ModItems.REIS_DANCE.get());
        List<Item> medicines = List.of(
                ModItems.CAR_FIRST_AID_KIT.get(), ModItems.FIELD_FIRST_AID_KIT.get(),
                ModItems.SIMPLE_INJECTOR.get(), ModItems.STRONG_INJECTOR.get(), ModItems.OUTDOOR_MEDICAL_KIT.get(),
                ModItems.BATTLEFIELD_MEDICAL_KIT.get(), ModItems.CAT_TOURNIQUET.get(),
                ModItems.DVE_PAINKILLER.get(), ModItems.DEK_FIELD_SURGICAL_PACK.get());
        List<Item> materials = List.of(
                ModItems.BALLISTIC_CERAMIC.get(), ModItems.SPECIAL_STEEL.get(), ModItems.TITANIUM_ALLOY.get(),
                ModItems.ANIMAL_GLAND.get(), ModItems.RAW_WOOD_PLANK.get(), ModItems.PH_REGULATOR.get(),
                ModItems.GLASS_SYRINGE.get(), ModItems.ANTIOXIDANT_STABILIZER.get(), ModItems.ELECTRIC_MOTOR.get(),
                ModItems.POLYETHYLENE_FIBER.get(), ModItems.ARAMID_FIBER.get(), ModItems.POLYMER_FABRIC.get(),
                ModItems.DIGITAL_CALIPER.get(), ModItems.HIGH_OUTPUT_CRUSHING_PLIERS.get());
        List<Item> helmets = List.of(
                ModItems.H01_TACTICAL_HELMET.get(), ModItems.MC_BALLISTIC_HELMET.get(), ModItems.DAS_BALLISTIC_HELMET.get(),
                ModItems.H07_TACTICAL_HELMET.get(), ModItems.MC201_BALLISTIC_HELMET.get(), ModItems.RIOT_HELMET.get(),
                ModItems.DICH1_TACTICAL_HELMET.get(), ModItems.GN_HEAVY_HELMET.get(), ModItems.DICH9_HEAVY_HELMET.get());
        List<Item> armor = List.of(
                ModItems.NYLON_BODY_ARMOR.get(), ModItems.LIGHT_BODY_ARMOR.get(), ModItems.STANDARD_BALLISTIC_VEST.get(),
                ModItems.HVK_QUICK_RELEASE_BODY_ARMOR.get(), ModItems.ASSAULT_BALLISTIC_VEST.get(),
                ModItems.ELITE_BALLISTIC_VEST.get(), ModItems.HVK2_BODY_ARMOR.get(), ModItems.HA2_HEAVY_BODY_ARMOR.get());
        List<List<Item>> fillerCategories = List.of(consumables, medicines, materials);
        List<Item> equipment = new ArrayList<>(helmets.size() + armor.size());
        equipment.addAll(helmets);
        equipment.addAll(armor);
        long value = 0L;
        int count = 0;
        List<ItemStack> drops = new ArrayList<>();

        Item guaranteedMedicine = medicines.get(random.nextInt(medicines.size()));
        ItemStack medicineStack = new ItemStack(guaranteedMedicine,
                Math.max(1, guaranteedMedicine.getDefaultInstance().getMaxStackSize()));
        drops.add(medicineStack);
        count += medicineStack.getCount();
        value += Math.max(0L, DealtItemsHavocConfig.price(guaranteedMedicine)) * medicineStack.getCount();

        int equipmentCount = 1 + random.nextInt(2);
        for (int i = 0; i < equipmentCount; i++) {
            Item item = equipment.get(random.nextInt(equipment.size()));
            ItemStack stack = new ItemStack(item, 1);
            drops.add(stack);
            count++;
            value += Math.max(0L, DealtItemsHavocConfig.price(item));
        }

        Item fallback = fillerCategories.stream()
                .flatMap(List::stream)
                .max(java.util.Comparator.comparingLong(DealtItemsHavocConfig::price))
                .orElse(guaranteedMedicine);
        while (drops.size() < 27) {
            boolean needsFallback = drops.size() >= 20 && (count < 128 || value < 3_000_000L);
            List<Item> category = fillerCategories.get(random.nextInt(fillerCategories.size()));
            Item item = needsFallback ? fallback : category.get(random.nextInt(category.size()));
            int amount = 1 + random.nextInt(Math.max(1, item.getDefaultInstance().getMaxStackSize()));
            if (needsFallback) {
                amount = Math.max(1, item.getDefaultInstance().getMaxStackSize());
            }
            ItemStack stack = new ItemStack(item, amount);
            drops.add(stack);
            count += amount;
            value += Math.max(0L, DealtItemsHavocConfig.price(item)) * amount;
        }
        if (!level.setBlock(pos, Blocks.CHEST.defaultBlockState(), 3)
                || !(level.getBlockEntity(pos) instanceof ChestBlockEntity chest)) {
            return false;
        }
        for (int i = 0; i < drops.size() && i < chest.getContainerSize(); i++) {
            chest.setItem(i, drops.get(i));
        }
        chest.setChanged();
        SUPPLY_CHESTS.computeIfAbsent(level, ignored -> new java.util.HashSet<>()).add(pos.immutable());
        Vec3 center = Vec3.atCenterOf(pos);
        sendParticlesLongDistance(level, center, 512.0D,
                net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,
                40, 0.8D, 0.8D, 0.8D, 0.04D);
        sendRing(level, center, 3.0D, net.minecraft.core.particles.ParticleTypes.END_ROD, 24);
        owner.displayClientMessage(Component.translatable(
                "message.dealt_force_skills.commander.supply_arrived"), false);
        return true;
    }

    private static void scheduleMissile(ServerPlayer owner, Vec3 target, boolean nuclear) {
        announceWarning(owner, nuclear);
        play(owner, ModSounds.COMMANDER_MISSILE_FLY.get(), 1.0F, 1.0F);
        IMPACTS.add(new Impact(owner.getUUID(), owner.serverLevel().dimension().location(), target,
                now(owner) + MISSILE_WARNING_TICKS, nuclear ? ImpactType.NUCLEAR : ImpactType.GUIDED_MISSILE));
    }

    private static void detonateMissile(ServerPlayer owner, ServerLevel level, Vec3 center, boolean nuclear) {
        double radius = nuclear ? 256.0D : 400.0D;
        float damage = 114_514.0F;
        double visualRange = radius + 512.0D;
        sendParticlesLongDistance(level, center, visualRange,
                net.minecraft.core.particles.ParticleTypes.FLASH, nuclear ? 12 : 6,
                4.0D, 4.0D, 4.0D, 0.0D);
        sendParticlesLongDistance(level, center, visualRange,
                net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER, nuclear ? 20 : 8,
                8.0D, 5.0D, 8.0D, 0.0D);
        sendParticlesLongDistance(level, center, visualRange,
                net.minecraft.core.particles.ParticleTypes.EXPLOSION, nuclear ? 80 : 40,
                nuclear ? 18.0D : 10.0D, 6.0D, nuclear ? 18.0D : 10.0D, 0.12D);
        sendParticlesLongDistance(level, center.add(0.0D, 10.0D, 0.0D), visualRange,
                net.minecraft.core.particles.ParticleTypes.CAMPFIRE_COSY_SMOKE, nuclear ? 180 : 80,
                nuclear ? 16.0D : 8.0D, nuclear ? 28.0D : 14.0D,
                nuclear ? 16.0D : 8.0D, 0.05D);
        sendParticlesLongDistance(level, center, visualRange,
                nuclear ? net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME
                        : net.minecraft.core.particles.ParticleTypes.FLAME,
                nuclear ? 220 : 100, nuclear ? 20.0D : 12.0D, 8.0D,
                nuclear ? 20.0D : 12.0D, 0.16D);
        sendRing(level, center, nuclear ? 32.0D : 16.0D,
                net.minecraft.core.particles.ParticleTypes.EXPLOSION, nuclear ? 96 : 56);
        sendRing(level, center, nuclear ? 64.0D : 32.0D,
                net.minecraft.core.particles.ParticleTypes.FLAME, nuclear ? 128 : 72);
        net.minecraft.sounds.SoundEvent explosion = switch (owner.getRandom().nextInt(4)) {
            case 1 -> ModSounds.COMMANDER_EXPLOSION_2.get();
            case 2 -> ModSounds.COMMANDER_EXPLOSION_3.get();
            case 3 -> ModSounds.COMMANDER_EXPLOSION_4.get();
            default -> ModSounds.COMMANDER_EXPLOSION_1.get();
        };
        RangedSoundHelper.play(level, center, explosion,
                net.minecraft.sounds.SoundSource.PLAYERS, 4.0F, 0.8F, visualRange);
        owner.server.getPlayerList().broadcastSystemMessage(Component.translatable(nuclear
                ? "message.dealt_force_skills.commander.nuclear_impact"
                : "message.dealt_force_skills.commander.missile_impact"), false);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(center, center).inflate(radius), entity -> TargetingUtil.isHostileLivingFor(owner, entity))) {
            SkillDamageHelper.hurt(target, SkillDamageHelper.trueDamage(level, owner, owner), owner, damage);
            forceSupportKill(owner, target);
        }
        if (nuclear) {
            RADIATION.add(new RadiationZone(owner.getUUID(), level.dimension().location(), center, 256.0D,
                    now(owner) + RADIATION_DURATION_TICKS));
        }
    }

    private static void addZone(ServerPlayer owner, ZoneType type, Vec3 center, long duration) {
        long startAt = now(owner);
        ZONES.computeIfAbsent(owner.getUUID(), ignored -> new ArrayList<>())
                .add(new Zone(owner.getUUID(), owner.serverLevel().dimension().location(), type, center,
                        startAt + duration, type == ZoneType.ARTILLERY ? startAt + 100L : startAt));
        if (type == ZoneType.LIGHTNING) {
            owner.serverLevel().setWeatherParameters(0, 90 * 20, true, true);
        }
    }

    private static void applyMedicalSupport(ServerPlayer owner) {
        for (ServerPlayer member : team(owner)) {
            applyMedical(member);
        }
        long until = now(owner) + 120 * 20L;
        for (ServerPlayer member : team(owner)) CommanderStateManager.setMedicalSupportUntil(member, until);
    }

    private static void applyMedical(ServerPlayer player) {
        cleanse(player);
        player.setHealth(player.getMaxHealth());
        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(20.0F);
        for (ItemStack stack : player.getInventory().items) {
            if (stack.isDamageableItem()) stack.setDamageValue(0);
        }
        for (ItemStack stack : player.getInventory().armor) {
            if (stack.isDamageableItem()) stack.setDamageValue(0);
        }
    }

    private static void cleanse(ServerPlayer player) {
        for (MobEffectInstance effect : List.copyOf(player.getActiveEffects())) {
            if (effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
                player.removeEffect(effect.getEffect());
            }
        }
    }

    private static void restorePlayer(ServerPlayer player, BlockPos teleportTarget, int invisibilityTicks,
                                      ServerPlayer source) {
        if (teleportTarget != null) {
            BlockPos safe = findPlace(player.serverLevel(), teleportTarget);
            player.teleportTo(player.serverLevel(), safe.getX() + 0.5D, safe.getY(), safe.getZ() + 0.5D,
                    player.getYRot(), player.getXRot());
        }
        player.setHealth(player.getMaxHealth());
        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(20.0F);
        for (ItemStack stack : player.getInventory().items) {
            if (stack.isDamageableItem()) stack.setDamageValue(0);
        }
        for (ItemStack stack : player.getInventory().armor) {
            if (stack.isDamageableItem()) stack.setDamageValue(0);
        }
        grantFullInvisibility(player, invisibilityTicks, source);
    }

    private static void grantFullInvisibility(ServerPlayer player, int ticks, ServerPlayer source) {
        int duration = Math.max(1, ticks);
        CompoundTag data = player.getPersistentData();
        data.putLong(FULL_INVISIBILITY_UNTIL,
                Math.max(data.getLong(FULL_INVISIBILITY_UNTIL), now(player) + duration));
        data.putBoolean(FULL_INVISIBILITY_RUNTIME, true);
        MobEffectInstance effect = new MobEffectInstance(ModEffects.UNDEAD_TRUE_INVISIBILITY,
                duration, 0, false, false, false);
        if (source == null) {
            player.addEffect(effect);
        } else {
            player.addEffect(effect, source);
        }
        player.setInvisible(true);
        clearMobTargets(player);
    }

    private static void maintainFullInvisibility(ServerPlayer player, long now) {
        CompoundTag data = player.getPersistentData();
        if (data.getLong(FULL_INVISIBILITY_UNTIL) > now) {
            data.putBoolean(FULL_INVISIBILITY_RUNTIME, true);
            player.setInvisible(true);
            clearMobTargets(player);
            return;
        }
        data.remove(FULL_INVISIBILITY_UNTIL);
        if (!data.getBoolean(FULL_INVISIBILITY_RUNTIME)) {
            return;
        }
        data.remove(FULL_INVISIBILITY_RUNTIME);
        if (!player.hasEffect(ModEffects.UNDEAD_TRUE_INVISIBILITY)
                && !player.hasEffect(MobEffects.INVISIBILITY)
                && !player.hasEffect(ModEffects.DEPARTMENT_CONCEALMENT)
                && !player.hasEffect(ModEffects.NOX_STEALTH)) {
            player.setInvisible(false);
        }
    }

    private static void clearMobTargets(ServerPlayer player) {
        for (Mob mob : player.serverLevel().getEntitiesOfClass(
                Mob.class, player.getBoundingBox().inflate(64.0D), Mob::isAlive)) {
            if (mob.getTarget() == player) {
                mob.setTarget(null);
            }
        }
    }

    private static List<ServerPlayer> team(ServerPlayer owner) {
        List<ServerPlayer> result = new ArrayList<>();
        result.add(owner);
        result.addAll(DealtTeamManager.onlineTeammates(owner));
        return result;
    }

    private static void teleport(ServerPlayer player, Vec3 target) {
        BlockPos pos = findPlace(player.serverLevel(), BlockPos.containing(target));
        player.teleportTo(player.serverLevel(), pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D,
                player.getYRot(), player.getXRot());
    }

    private static BlockPos findPlace(ServerLevel level, BlockPos requested) {
        BlockPos pos = requested;
        for (int i = 0; i < 12; i++) {
            if (level.getBlockState(pos).canBeReplaced()) {
                return pos;
            }
            pos = pos.above();
        }
        return pos;
    }

    private static Vec3 validateTarget(ServerPlayer player, Vec3 requested) {
        if (requested == null || !Double.isFinite(requested.x) || !Double.isFinite(requested.y)
                || !Double.isFinite(requested.z)
                || requested.distanceToSqr(player.position()) > 256.0D * 256.0D) {
            return null;
        }
        BlockPos position = BlockPos.containing(requested);
        if (position.getY() < player.serverLevel().getMinBuildHeight() - 64
                || position.getY() > player.serverLevel().getMaxBuildHeight() + 64
                || !player.serverLevel().getWorldBorder().isWithinBounds(position)) {
            return null;
        }
        return requested;
    }

    private static void playSupportSound(ServerPlayer player, CommanderSupport support) {
        net.minecraft.sounds.SoundEvent sound = switch (support) {
            case HIGH_THREAT_MARK -> player.getRandom().nextBoolean()
                    ? ModSounds.COMMANDER_MARK_1.get() : ModSounds.COMMANDER_MARK_2.get();
            case AREA_SMOKE -> player.getRandom().nextBoolean()
                    ? ModSounds.COMMANDER_SMOKE_1.get() : ModSounds.COMMANDER_SMOKE_2.get();
            case MEDICAL_SUPPORT -> player.getRandom().nextBoolean()
                    ? ModSounds.COMMANDER_MEDICAL_1.get() : ModSounds.COMMANDER_MEDICAL_2.get();
            case ARTILLERY_POSITION -> player.getRandom().nextBoolean()
                    ? ModSounds.COMMANDER_ARTILLERY_1.get() : ModSounds.COMMANDER_ARTILLERY_2.get();
            case MANNED_MEDICAL_DRONE -> ModSounds.COMMANDER_DRONE_LAND.get();
            case VEHICLE_AIRDROP -> player.getRandom().nextBoolean()
                    ? ModSounds.COMMANDER_VEHICLE_1.get() : ModSounds.COMMANDER_VEHICLE_2.get();
            case TACTICAL_RESPAWN_BEACON -> player.getRandom().nextBoolean()
                    ? ModSounds.COMMANDER_BEACON_1.get() : ModSounds.COMMANDER_BEACON_2.get();
            case ADS_FIELD_COVER -> ModSounds.COMMANDER_QUICK_REVIVE_1.get();
            case EQUIPMENT_SUPPLY -> player.getRandom().nextBoolean()
                    ? ModSounds.COMMANDER_EQUIPMENT_1.get() : ModSounds.COMMANDER_EQUIPMENT_2.get();
            case LIGHTNING_STORM -> ModSounds.COMMANDER_LIGHTNING.get();
            case QUICK_REVIVE -> player.getRandom().nextBoolean()
                    ? ModSounds.COMMANDER_QUICK_REVIVE_1.get() : ModSounds.COMMANDER_QUICK_REVIVE_2.get();
            case GUIDED_MISSILE -> player.getRandom().nextBoolean()
                    ? ModSounds.COMMANDER_MISSILE_1.get() : ModSounds.COMMANDER_MISSILE_2.get();
            case TACTICAL_NUCLEAR_STRIKE -> ModSounds.COMMANDER_NUCLEAR_RELEASE.get();
        };
        play(player, sound, 1.0F, 1.0F);
    }

    private static void announceWarning(ServerPlayer owner, boolean nuclear) {
        Component text = Component.translatable(nuclear
                ? "message.dealt_force_skills.commander.nuclear_warning"
                : "message.dealt_force_skills.commander.missile_warning", owner.getDisplayName());
        owner.server.getPlayerList().broadcastSystemMessage(text, false);
        for (ServerPlayer player : owner.server.getPlayerList().getPlayers()) {
            play(player, ModSounds.COMMANDER_ALARM.get(), 1.0F, 1.0F);
        }
    }

    private static void play(ServerPlayer player, net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
        player.serverLevel().playSound(null, player.blockPosition(), sound,
                net.minecraft.sounds.SoundSource.PLAYERS, volume, pitch);
    }

    private static void emitSupportArrival(ServerPlayer owner, CommanderSupport support, Vec3 center) {
        ServerLevel level = owner.serverLevel();
        ParticleOptions particle = switch (support) {
            case HIGH_THREAT_MARK -> net.minecraft.core.particles.ParticleTypes.ENCHANT;
            case AREA_SMOKE, ARTILLERY_POSITION, VEHICLE_AIRDROP ->
                    net.minecraft.core.particles.ParticleTypes.CAMPFIRE_COSY_SMOKE;
            case MEDICAL_SUPPORT -> net.minecraft.core.particles.ParticleTypes.HEART;
            case MANNED_MEDICAL_DRONE, TACTICAL_RESPAWN_BEACON, EQUIPMENT_SUPPLY ->
                    net.minecraft.core.particles.ParticleTypes.END_ROD;
            case ADS_FIELD_COVER -> net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL;
            case LIGHTNING_STORM -> net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK;
            case QUICK_REVIVE -> net.minecraft.core.particles.ParticleTypes.TOTEM_OF_UNDYING;
            case GUIDED_MISSILE, TACTICAL_NUCLEAR_STRIKE -> net.minecraft.core.particles.ParticleTypes.SMOKE;
        };
        int count = switch (support) {
            case GUIDED_MISSILE, TACTICAL_NUCLEAR_STRIKE -> 64;
            case ARTILLERY_POSITION, LIGHTNING_STORM -> 48;
            default -> 28;
        };
        double spread = switch (support) {
            case GUIDED_MISSILE, TACTICAL_NUCLEAR_STRIKE -> 3.0D;
            case ARTILLERY_POSITION, LIGHTNING_STORM -> 2.0D;
            default -> 0.8D;
        };
        sendParticlesLongDistance(level, center.add(0.0D, 0.8D, 0.0D), 512.0D,
                particle, count, spread, 1.2D, spread, 0.04D);
        if (support.targeted()) {
            sendRing(level, center, support == CommanderSupport.TACTICAL_NUCLEAR_STRIKE ? 8.0D : 4.0D,
                    particle, support == CommanderSupport.TACTICAL_NUCLEAR_STRIKE ? 48 : 24);
        }
    }

    private static void announceSupportApplied(ServerPlayer owner, CommanderSupport support) {
        Component supportName = Component.translatable(support.translationKey(),
                support.cost(), support.cooldownTicks() / 20);
        Component message = Component.translatable(
                "message.dealt_force_skills.commander.support_applied", supportName);
        for (ServerPlayer member : team(owner)) {
            member.sendSystemMessage(message);
        }
    }

    private static void maintainMarkedTargets(ServerPlayer owner) {
        if (!CommanderStateManager.isCommander(owner)) {
            return;
        }
        long now = now(owner);
        for (Entity entity : owner.serverLevel().getAllEntities()) {
            if (!(entity instanceof LivingEntity target)
                    || target.getPersistentData().getLong(MARK_UNTIL) <= now
                    || !target.getPersistentData().hasUUID(MARK_OWNER)
                    || !owner.getUUID().equals(target.getPersistentData().getUUID(MARK_OWNER))) {
                continue;
            }
            addSupportEffect(owner, target,
                    new MobEffectInstance(MobEffects.GLOWING, 40, 0, false, true, true));
            owner.serverLevel().sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT,
                    target.getX(), target.getY() + target.getBbHeight() + 0.3D, target.getZ(),
                    6, 0.35D, 0.2D, 0.35D, 0.02D);
        }
    }

    private static void showPersonalSupportIndicators(ServerPlayer player, long now) {
        ServerLevel level = player.serverLevel();
        if (CommanderStateManager.medicalSupportUntil(player) > now) {
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.HEART,
                    player.getX(), player.getY() + 1.2D, player.getZ(), 2,
                    0.45D, 0.35D, 0.45D, 0.0D);
        }
        if (!CommanderStateManager.isCommander(player)) {
            return;
        }
        if (CommanderStateManager.adsCoverUntil(player) > now) {
            sendRing(level, player.position(), 15.0D,
                    net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL, 32);
        }
        if (CommanderStateManager.quickReviveUntil(player) > now) {
            sendRing(level, player.position(), 15.0D,
                    net.minecraft.core.particles.ParticleTypes.TOTEM_OF_UNDYING, 32);
        }
        CompoundTag data = player.getPersistentData();
        if (data.getLong("CommanderBeaconUntil") > now
                && level.dimension().location().toString().equals(data.getString("CommanderBeaconDimension"))) {
            Vec3 center = Vec3.atCenterOf(BlockPos.of(data.getLong("CommanderBeaconPosition")));
            sendRing(level, center, 4.0D, net.minecraft.core.particles.ParticleTypes.END_ROD, 24);
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                    center.x, center.y + 1.5D, center.z, 8, 0.2D, 1.4D, 0.2D, 0.01D);
        }
    }

    private static void maintainZone(ServerPlayer owner, ServerLevel level, Zone zone) {
        double radius = zone.type() == ZoneType.ARTILLERY ? 100.0D : 200.0D;
        ParticleOptions particle = zone.type() == ZoneType.ARTILLERY
                ? net.minecraft.core.particles.ParticleTypes.CAMPFIRE_COSY_SMOKE
                : net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK;
        sendRing(level, zone.center(), radius, particle, 64);
        level.sendParticles(particle, zone.center().x, zone.center().y + 1.0D, zone.center().z,
                8, 1.0D, 0.8D, 1.0D, 0.02D);
        if (zone.type() == ZoneType.ARTILLERY) {
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                    new AABB(zone.center(), zone.center()).inflate(radius),
                    entity -> TargetingUtil.isHostileLivingFor(owner, entity))) {
                addSupportEffect(owner, target,
                        new MobEffectInstance(MobEffects.GLOWING, 40, 0, false, true, true));
            }
        }
    }

    private static void renderPendingImpact(MinecraftServer server, Impact impact) {
        ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, impact.dimension()));
        if (level == null) {
            return;
        }
        if (impact.type() == ImpactType.ARTILLERY) {
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    impact.position().x, impact.position().y + 0.2D, impact.position().z,
                    4, 0.35D, 0.1D, 0.35D, 0.01D);
            sendRing(level, impact.position(), 6.0D,
                    net.minecraft.core.particles.ParticleTypes.SMOKE, 20);
            return;
        }
        double radius = impact.type() == ImpactType.NUCLEAR ? 256.0D : 400.0D;
        sendRing(level, impact.position(), radius,
                net.minecraft.core.particles.ParticleTypes.END_ROD, 64);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE,
                impact.position().x, impact.position().y + 1.0D, impact.position().z,
                20, 1.5D, 1.0D, 1.5D, 0.02D);
    }

    private static void sendRing(ServerLevel level, Vec3 center, double radius,
                                 ParticleOptions particle, int points) {
        int count = Math.max(8, points);
        double observerRange = radius + 512.0D;
        for (int i = 0; i < count; i++) {
            double angle = Math.PI * 2.0D * i / count;
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            for (ServerPlayer observer : level.players()) {
                if (observer.distanceToSqr(center) <= observerRange * observerRange) {
                    level.sendParticles(observer, particle, true,
                            x, center.y + 0.15D, z,
                            1, 0.0D, 0.0D, 0.0D, 0.0D);
                }
            }
        }
    }

    private static void sendParticlesLongDistance(ServerLevel level, Vec3 center, double observerRange,
                                                  ParticleOptions particle, int count,
                                                  double dx, double dy, double dz, double speed) {
        double rangeSqr = observerRange * observerRange;
        for (ServerPlayer observer : level.players()) {
            if (observer.distanceToSqr(center) <= rangeSqr) {
                level.sendParticles(observer, particle, true,
                        center.x, center.y, center.z,
                        count, dx, dy, dz, speed);
            }
        }
    }

    private static boolean canExecute(ServerPlayer owner, CommanderSupport support, int targetId, Vec3 target,
                                      String vehicleId) {
        return switch (support) {
            case HIGH_THREAT_MARK -> {
                LivingEntity marked = resolveTarget(owner, targetId);
                yield marked != null && target != null && marked.position().distanceToSqr(target) <= 9.0D;
            }
            case VEHICLE_AIRDROP -> target != null
                    && SuperbWarfareCompat.registeredVehicleType(owner.serverLevel(), vehicleId) != null;
            case EQUIPMENT_SUPPLY -> target != null;
            default -> !support.targeted() || target != null;
        };
    }

    private static LivingEntity resolveTarget(ServerPlayer owner, int targetId) {
        Entity entity = targetId < 0 ? null : owner.serverLevel().getEntity(targetId);
        LivingEntity target = entity instanceof LivingEntity living
                && TargetingUtil.isHostileLivingFor(owner, living)
                && !isMarked(living)
                && living.distanceToSqr(owner) <= 256.0D * 256.0D
                ? living : null;
        return target;
    }

    private static int supportCooldownSeconds(ServerPlayer player, CommanderSupport support) {
        return (CommanderStateManager.supportCooldownTicks(player, support) + 19) / 20;
    }

    private static long now(Entity entity) {
        return com.rzy.dealt_force_skills.skill.SkillCooldownHelper.now(entity);
    }

    private static void ensureLoaded(MinecraftServer server) {
        if (loadedServer == server && savedData != null) {
            return;
        }
        ZONES.clear();
        IMPACTS.clear();
        RADIATION.clear();
        loadedServer = server;
        savedData = server.overworld().getDataStorage()
                .computeIfAbsent(new SavedData.Factory<>(CommanderSupportSavedData::new, CommanderSupportSavedData::load),
                        DealtForceSkillsMod.MODID + "_commander_support");
        savedData.restoreRuntimeState(server.overworld().getGameTime());
    }

    private static void persist(MinecraftServer server) {
        ensureLoaded(server);
        savedData.captureRuntimeState();
        savedData.setDirty();
    }

    private static void tickRadiation(net.minecraft.server.MinecraftServer server, long now) {
        for (Iterator<RadiationZone> iterator = RADIATION.iterator(); iterator.hasNext();) {
            RadiationZone zone = iterator.next();
            if (zone.endsAt() <= now) {
                iterator.remove();
                continue;
            }
            ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, zone.dimension()));
            if (level == null) continue;
            ServerPlayer owner = zone.ownerId() == null ? null
                    : server.getPlayerList().getPlayer(zone.ownerId());
            if (now % 20L == 0L) {
                sendRing(level, zone.center(), zone.radius(),
                        net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME, 64);
            }
            for (ServerPlayer player : level.players()) {
                if (zone.contains(player.position())) {
                    MobEffectInstance wither = new MobEffectInstance(MobEffects.WITHER, 40, 9,
                            false, true, true);
                    MobEffectInstance hunger = new MobEffectInstance(MobEffects.HUNGER, 40, 254,
                            false, true, true);
                    if (owner == null) {
                        player.addEffect(wither);
                        player.addEffect(hunger);
                    } else {
                        addSupportEffect(owner, player, wither);
                        addSupportEffect(owner, player, hunger);
                    }
                }
            }
            for (Mob mob : level.getEntitiesOfClass(Mob.class,
                    new AABB(zone.center(), zone.center()).inflate(zone.radius()), Mob::isAlive)) {
                if (!TrainingTargetCompat.recordsDamageWithoutDying(mob)) {
                    if (owner == null) {
                        mob.kill();
                    } else {
                        forceSupportKill(owner, mob);
                    }
                }
            }
        }
    }

    private static void addSupportEffect(ServerPlayer owner, LivingEntity target, MobEffectInstance effect) {
        target.addEffect(effect, owner);
    }

    private static void forceSupportKill(ServerPlayer owner, LivingEntity target) {
        if (!target.isAlive() || TrainingTargetCompat.recordsDamageWithoutDying(target)) {
            return;
        }
        if (!(target.level() instanceof ServerLevel targetLevel)) {
            return;
        }
        target.invulnerableTime = 0;
        target.hurt(SkillDamageHelper.source(targetLevel, DamageTypes.PLAYER_ATTACK, owner, owner),
                Float.MAX_VALUE);
    }

    private enum ZoneType { ARTILLERY, LIGHTNING }

    private static final class Zone {
        private final UUID ownerId;
        private final ResourceLocation dimension;
        private final ZoneType type;
        private final Vec3 center;
        private final long endsAt;
        private long nextActionAt;

        private Zone(UUID ownerId, ResourceLocation dimension, ZoneType type, Vec3 center,
                     long endsAt, long nextActionAt) {
            this.ownerId = ownerId;
            this.dimension = dimension;
            this.type = type;
            this.center = center;
            this.endsAt = endsAt;
            this.nextActionAt = nextActionAt;
        }

        public UUID ownerId() { return ownerId; }
        public ResourceLocation dimension() { return dimension; }
        public ZoneType type() { return type; }
        public Vec3 center() { return center; }
        public long endsAt() { return endsAt; }
        public long nextActionAt() { return nextActionAt; }
        public void nextActionAt(long value) { nextActionAt = value; }
    }

    private enum ImpactType { ARTILLERY, GUIDED_MISSILE, NUCLEAR }

    private record Impact(UUID ownerId, ResourceLocation dimension, Vec3 position, long at, ImpactType type) {
    }

    private record RadiationZone(UUID ownerId, ResourceLocation dimension, Vec3 center,
                                 double radius, long endsAt) {
        boolean contains(Vec3 position) {
            return center.distanceToSqr(position) <= radius * radius;
        }
    }

    private static final class CommanderSupportSavedData extends SavedData {
        private CompoundTag state = new CompoundTag();

        private static CommanderSupportSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
            CommanderSupportSavedData data = new CommanderSupportSavedData();
            data.state = tag.copy();
            return data;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            tag.merge(state);
            return tag;
        }

        private void restoreRuntimeState(long now) {
            ListTag zones = state.getList("Zones", Tag.TAG_COMPOUND);
            for (int i = 0; i < zones.size(); i++) {
                CompoundTag entry = zones.getCompound(i);
                ResourceLocation dimension = parseDimension(entry);
                if (dimension == null || !entry.hasUUID("Owner")) {
                    continue;
                }
                Zone zone = new Zone(entry.getUUID("Owner"), dimension,
                        ZoneType.values()[Math.max(0, Math.min(ZoneType.values().length - 1,
                                entry.getInt("Type")))], new Vec3(entry.getDouble("X"), entry.getDouble("Y"),
                        entry.getDouble("Z")), entry.getLong("EndsAt"), entry.getLong("NextAt"));
                ZONES.computeIfAbsent(zone.ownerId(), ignored -> new ArrayList<>()).add(zone);
            }
            ListTag impacts = state.getList("Impacts", Tag.TAG_COMPOUND);
            for (int i = 0; i < impacts.size(); i++) {
                CompoundTag entry = impacts.getCompound(i);
                ResourceLocation dimension = parseDimension(entry);
                if (dimension == null || !entry.hasUUID("Owner")) {
                    continue;
                }
                int typeIndex = entry.contains("Type", Tag.TAG_INT)
                        ? entry.getInt("Type")
                        : entry.getBoolean("Nuclear") ? ImpactType.NUCLEAR.ordinal()
                        : ImpactType.GUIDED_MISSILE.ordinal();
                ImpactType type = ImpactType.values()[Math.max(0,
                        Math.min(ImpactType.values().length - 1, typeIndex))];
                IMPACTS.add(new Impact(entry.getUUID("Owner"), dimension,
                        new Vec3(entry.getDouble("X"), entry.getDouble("Y"), entry.getDouble("Z")),
                        entry.getLong("At"), type));
            }
            ListTag radiation = state.getList("Radiation", Tag.TAG_COMPOUND);
            for (int i = 0; i < radiation.size(); i++) {
                CompoundTag entry = radiation.getCompound(i);
                ResourceLocation dimension = parseDimension(entry);
                if (dimension != null) {
                    long endsAt = entry.contains("EndsAt", Tag.TAG_LONG)
                            ? entry.getLong("EndsAt") : now + RADIATION_DURATION_TICKS;
                    if (endsAt <= now) {
                        continue;
                    }
                    UUID ownerId = entry.hasUUID("Owner") ? entry.getUUID("Owner") : null;
                    RADIATION.add(new RadiationZone(ownerId, dimension,
                            new Vec3(entry.getDouble("X"), entry.getDouble("Y"), entry.getDouble("Z")),
                            entry.getDouble("Radius"), endsAt));
                }
            }
        }

        private static ResourceLocation parseDimension(CompoundTag entry) {
            String value = entry.getString("Dimension");
            if (value.isBlank()) {
                return null;
            }
            try {
                return ResourceLocation.parse(value);
            } catch (RuntimeException ignored) {
                return null;
            }
        }

        private void captureRuntimeState() {
            CompoundTag next = new CompoundTag();
            ListTag zones = new ListTag();
            for (List<Zone> entries : ZONES.values()) {
                for (Zone zone : entries) {
                    CompoundTag entry = new CompoundTag();
                    entry.putUUID("Owner", zone.ownerId());
                    entry.putString("Dimension", zone.dimension().toString());
                    entry.putInt("Type", zone.type().ordinal());
                    entry.putDouble("X", zone.center().x);
                    entry.putDouble("Y", zone.center().y);
                    entry.putDouble("Z", zone.center().z);
                    entry.putLong("EndsAt", zone.endsAt());
                    entry.putLong("NextAt", zone.nextActionAt());
                    zones.add(entry);
                }
            }
            next.put("Zones", zones);
            ListTag impacts = new ListTag();
            for (Impact impact : IMPACTS) {
                CompoundTag entry = new CompoundTag();
                entry.putUUID("Owner", impact.ownerId());
                entry.putString("Dimension", impact.dimension().toString());
                entry.putDouble("X", impact.position().x);
                entry.putDouble("Y", impact.position().y);
                entry.putDouble("Z", impact.position().z);
                entry.putLong("At", impact.at());
                entry.putInt("Type", impact.type().ordinal());
                impacts.add(entry);
            }
            next.put("Impacts", impacts);
            ListTag radiation = new ListTag();
            for (RadiationZone zone : RADIATION) {
                CompoundTag entry = new CompoundTag();
                if (zone.ownerId() != null) {
                    entry.putUUID("Owner", zone.ownerId());
                }
                entry.putString("Dimension", zone.dimension().toString());
                entry.putDouble("X", zone.center().x);
                entry.putDouble("Y", zone.center().y);
                entry.putDouble("Z", zone.center().z);
                entry.putDouble("Radius", zone.radius());
                entry.putLong("EndsAt", zone.endsAt());
                radiation.add(entry);
            }
            next.put("Radiation", radiation);
            state = next;
        }
    }
}