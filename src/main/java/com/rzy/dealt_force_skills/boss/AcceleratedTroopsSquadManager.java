package com.rzy.dealt_force_skills.boss;

import com.rzy.dealt_force_skills.character.ghroth.GhrothStateManager;
import com.rzy.dealt_force_skills.config.DealtBossesConfig;
import com.rzy.dealt_force_skills.entity.AcceleratedTroopsEntity;
import com.rzy.dealt_force_skills.registry.ModEntities;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.util.RangedSoundHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.function.Supplier;

public final class AcceleratedTroopsSquadManager {
    private static final String DATA_NAME = "dealt_force_accelerated_troops";
    private static final Map<ServerLevel, Map<UUID, Squad>> SQUADS = new WeakHashMap<>();

    private AcceleratedTroopsSquadManager() {
    }

    public static UUID spawnSquad(ServerLevel level, Vec3 center) {
        if (level == null || center == null || hasActiveSquad(level.getServer())) {
            return null;
        }
        UUID squadId = UUID.randomUUID();
        int spawned = 0;
        for (int i = 0; i < squadSize(); i++) {
            AcceleratedTroopsEntity member = ModEntities.ACCELERATED_TROOPS.get().create(level);
            if (member == null) {
                continue;
            }
            double angle = Math.PI * 2.0D * i / Math.max(1, squadSize());
            double radius = i == 0 ? 0.0D : 2.4D;
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    Mth.floor(x), Mth.floor(z));
            member.setSquadId(squadId);
            member.setSquadSlot(i);
            member.moveTo(x, Math.max(center.y, y), z, level.random.nextFloat() * 360.0F, 0.0F);
            if (level.addFreshEntity(member)) {
                register(member);
                spawned++;
            }
        }
        if (spawned <= 0) {
            return null;
        }
        WorldData data = data(level.getServer());
        data.activeSquad = squadId;
        data.setDirty();
        return squadId;
    }

    public static boolean trySpawnAfterBeaconKill(ServerPlayer killer) {
        return unlockAfterBeaconKill(killer == null ? null : killer.server);
    }

    /** Marks persistent post-beacon eligibility; the actual spawn is attempted by the server tick. */
    public static boolean unlockAfterBeaconKill(MinecraftServer server) {
        if (server == null) {
            return false;
        }
        WorldData data = data(server);
        long now = server.overworld().getGameTime();
        data.unlocked = true;
        if (data.nextAttemptTick <= 0L || data.nextAttemptTick > now) {
            data.nextAttemptTick = now;
        }
        data.setDirty();
        return true;
    }

    public static void tickServer(MinecraftServer server) {
        if (server == null) {
            return;
        }
        WorldData data = data(server);
        long now = server.overworld().getGameTime();
        if (!data.unlocked || data.activeSquad != null || hasRuntimeSquad(server)
                || now < data.lockoutUntil || now < data.nextAttemptTick) {
            return;
        }
        data.nextAttemptTick = now + 20L;
        data.setDirty();
        int min = Math.max(1, DealtBossesConfig.intValue(
                "accelerated_troops.spawn_min_distance", 128));
        int max = Math.max(min + 2, DealtBossesConfig.intValue(
                "accelerated_troops.spawn_max_distance", 256));
        int chance = Mth.clamp(DealtBossesConfig.intValue(
                "accelerated_troops.spawn_chance_percent", 25), 0, 100);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!isNaturalSpawnPlayer(player) || player.getRandom().nextInt(100) >= chance) {
                continue;
            }
            ServerLevel level = player.serverLevel();
            BlockPos origin = player.blockPosition();
            for (int attempt = 0; attempt < 32; attempt++) {
                double angle = player.getRandom().nextDouble() * Math.PI * 2.0D;
                double radius = min + player.getRandom().nextDouble() * (max - min);
                int x = origin.getX() + Mth.floor(Math.cos(angle) * radius);
                int z = origin.getZ() + Mth.floor(Math.sin(angle) * radius);
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                BlockPos feet = new BlockPos(x, y, z);
                if (!safeSpawn(level, feet)) {
                    continue;
                }
                if (spawnSquad(level, Vec3.atBottomCenterOf(feet)) != null) {
                    Component warning = Component.translatable(
                            "message.dealt_force_skills.accelerated_troops.approaching").withStyle(ChatFormatting.GREEN);
                    server.getPlayerList().getPlayers().forEach(viewer -> viewer.displayClientMessage(warning, false));
                    return;
                }
            }
        }
    }

    private static boolean isNaturalSpawnPlayer(ServerPlayer player) {
        return player != null && player.isAlive() && !player.isCreative() && !player.isSpectator()
                && !GhrothStateManager.isGhroth(player);
    }

    public static void tickMember(AcceleratedTroopsEntity member) {
        if (!(member.level() instanceof ServerLevel level) || !member.isAlive()) {
            return;
        }
        Squad squad = register(member);
        long now = level.getGameTime();
        if (squad.lastTick == now) {
            return;
        }
        squad.lastTick = now;
        squad.tick(level);
    }

    public static void onAttacked(AcceleratedTroopsEntity member, Entity attacker) {
        if (!(member.level() instanceof ServerLevel level) || attacker == null) {
            return;
        }
        if (attacker instanceof Player player && GhrothStateManager.isGhroth(player)) {
            return;
        }
        Squad squad = register(member);
        if (attacker instanceof Player player) {
            squad.enterCombat(level, player);
        } else if (squad.state != State.COMBAT
                && attacker instanceof LivingEntity living && member.isValidEnemy(living)) {
            squad.commandTarget(living);
        }
    }

    public static void onMemberDeath(AcceleratedTroopsEntity member) {
        if (!(member.level() instanceof ServerLevel level)) {
            return;
        }
        Map<UUID, Squad> byId = SQUADS.get(level);
        if (byId == null) {
            return;
        }
        Squad squad = byId.get(member.squadId());
        if (squad == null) {
            return;
        }
        squad.members.remove(member.getUUID());
        squad.updateBossBar();
        if (!squad.members.isEmpty()) {
            return;
        }
        squad.close(level);
        byId.remove(member.squadId());
        WorldData data = data(level.getServer());
        if (member.squadId().equals(data.activeSquad)) {
            data.activeSquad = null;
            data.lockoutUntil = level.getServer().overworld().getGameTime()
                    + Math.max(1, DealtBossesConfig.intValue(
                    "accelerated_troops.respawn_lockout_ticks", 30 * 60 * 20));
            data.setDirty();
        }
    }

    /** Peaceful cleanup discards entities without invoking their death callback. */
    public static void onMemberDiscarded(AcceleratedTroopsEntity member) {
        if (!(member.level() instanceof ServerLevel level)) {
            return;
        }
        Map<UUID, Squad> byId = SQUADS.get(level);
        if (byId != null) {
            Squad squad = byId.get(member.squadId());
            if (squad != null) {
                squad.members.remove(member.getUUID());
                if (squad.members.isEmpty()) {
                    squad.close(level);
                    byId.remove(member.squadId());
                }
            }
        }
        WorldData data = data(level.getServer());
        if (member.squadId().equals(data.activeSquad)) {
            data.activeSquad = null;
            data.nextAttemptTick = Math.min(data.nextAttemptTick, level.getServer().overworld().getGameTime());
            data.setDirty();
        }
    }

    public static boolean canTarget(AcceleratedTroopsEntity member, LivingEntity target) {
        if (!(member.level() instanceof ServerLevel level) || target == null || !target.isAlive()) {
            return false;
        }
        Squad squad = squad(level, member.squadId());
        if (squad == null || !member.isValidEnemy(target)) {
            return false;
        }
        if (target instanceof Player) {
            return squad.state == State.COMBAT && target.getUUID().equals(squad.primaryTarget);
        }
        return true;
    }

    public static boolean retainTarget(AcceleratedTroopsEntity member, LivingEntity target) {
        if (!(member.level() instanceof ServerLevel level) || target == null || !target.isAlive()) {
            return false;
        }
        Squad squad = squad(level, member.squadId());
        if (squad == null || !member.isValidEnemy(target)) {
            return false;
        }
        if (target instanceof Player) {
            return squad.state == State.COMBAT && target.getUUID().equals(squad.primaryTarget);
        }
        return squad.state != State.COMBAT;
    }

    public static boolean isCombat(AcceleratedTroopsEntity member) {
        if (!(member.level() instanceof ServerLevel level)) {
            return false;
        }
        Squad squad = squad(level, member.squadId());
        LivingEntity target = member.getTarget();
        return squad != null && (squad.state == State.COMBAT
                || target != null && !(target instanceof Player) && member.isValidEnemy(target));
    }

    public static boolean isPlayerCombatTarget(AcceleratedTroopsEntity member, LivingEntity target) {
        if (!(member.level() instanceof ServerLevel level) || !(target instanceof Player)) {
            return false;
        }
        Squad squad = squad(level, member.squadId());
        return squad != null && squad.state == State.COMBAT
                && target.isAlive() && target.getUUID().equals(squad.primaryTarget);
    }

    public static long reserveUtilityUse(AcceleratedTroopsEntity member, long earliestTick) {
        if (!(member.level() instanceof ServerLevel level)) {
            return earliestTick;
        }
        Squad squad = squad(level, member.squadId());
        return squad == null ? earliestTick : squad.reserveUtilityUse(earliestTick);
    }

    public static boolean hasActiveSquad(MinecraftServer server) {
        if (server == null) {
            return false;
        }
        WorldData worldData = data(server);
        if (worldData.activeSquad != null && !hasLiveSquad(server, worldData.activeSquad)) {
            worldData.activeSquad = null;
            worldData.setDirty();
        }
        return worldData.activeSquad != null || hasRuntimeSquad(server);
    }

    private static boolean hasLiveSquad(MinecraftServer server, UUID squadId) {
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getEntities().getAll()) {
                if (entity instanceof AcceleratedTroopsEntity troop
                        && squadId.equals(troop.squadId()) && troop.isAlive() && !troop.isRemoved()) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasRuntimeSquad(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            Map<UUID, Squad> byId = SQUADS.get(level);
            if (byId != null && byId.values().stream()
                    .anyMatch(squad -> squad.members.values().stream().anyMatch(AcceleratedTroopsEntity::isAlive))) {
                return true;
            }
        }
        return false;
    }

    private static boolean safeSpawn(ServerLevel level, BlockPos feet) {
        level.getChunkAt(feet);
        return level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                && level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty()
                && !level.getBlockState(feet.below()).getCollisionShape(level, feet.below()).isEmpty();
    }

    private static Squad register(AcceleratedTroopsEntity member) {
        ServerLevel level = (ServerLevel) member.level();
        Map<UUID, Squad> byId = SQUADS.computeIfAbsent(level, ignored -> new HashMap<>());
        Squad squad = byId.computeIfAbsent(member.squadId(), Squad::new);
        squad.members.put(member.getUUID(), member);
        WorldData data = data(level.getServer());
        if (data.activeSquad == null) {
            data.activeSquad = member.squadId();
            data.setDirty();
        }
        return squad;
    }

    private static Squad squad(ServerLevel level, UUID id) {
        Map<UUID, Squad> byId = SQUADS.get(level);
        return byId == null ? null : byId.get(id);
    }

    private static int squadSize() {
        return Math.max(1, DealtBossesConfig.intValue("accelerated_troops.squad_size", 6));
    }

    private static WorldData data(MinecraftServer server) {
        return server.overworld().getDataStorage()
                .computeIfAbsent(new SavedData.Factory<>(WorldData::new, WorldData::load), DATA_NAME);
    }

    private enum State {
        IDLE,
        WARNING,
        COMBAT
    }

    private static final class Squad {
        private final UUID id;
        private final Map<UUID, AcceleratedTroopsEntity> members = new HashMap<>();
        private final Set<UUID> bossBarPlayers = new HashSet<>();
        private final Set<UUID> audioPlayers = new HashSet<>();
        private final Map<UUID, Long> musicStartedAt = new HashMap<>();
        private final ServerBossEvent bossEvent = new ServerBossEvent(
                Component.translatable("entity.dealt_force_skills.accelerated_troops"),
                BossEvent.BossBarColor.RED,
                BossEvent.BossBarOverlay.PROGRESS);
        private State state = State.IDLE;
        private int warningTicks;
        private UUID primaryTarget;
        private long lastTick = Long.MIN_VALUE;
        private long nextUtilityUseAt;
        private boolean warningThirdPlayed;
        private boolean closed;

        private Squad(UUID id) {
            this.id = id;
        }

        private void tick(ServerLevel level) {
            if (closed) {
                return;
            }
            members.values().removeIf(entity -> entity == null || entity.isRemoved() || !entity.isAlive());
            if (members.isEmpty()) {
                close(level);
                Map<UUID, Squad> byId = SQUADS.get(level);
                if (byId != null) {
                    byId.remove(id);
                }
                return;
            }

            updateBossBar();
            Vec3 center = center();
            List<ServerPlayer> nearbyPlayers = level.players().stream()
                    .filter(this::validPlayer)
                    .filter(player -> player.distanceToSqr(center) <= warningRange() * warningRange())
                    .toList();

            if (state == State.COMBAT) {
                LivingEntity target = resolvePrimaryTarget(level);
                if (target == null || !target.isAlive()) {
                    resetEncounter(level);
                } else {
                    commandTarget(target);
                }
            } else {
                if (nearbyPlayers.isEmpty()) {
                    resetEncounter(level);
                } else {
                    if (state == State.IDLE) {
                        state = State.WARNING;
                        warningTicks = 0;
                        warningThirdPlayed = false;
                    }
                    ServerPlayer nearest = nearbyPlayers.stream()
                            .min(Comparator.comparingDouble(player -> player.distanceToSqr(center)))
                            .orElse(null);
                    primaryTarget = nearest == null ? null : nearest.getUUID();
                    warningTicks++;
                    if (warningTicks == warningSegmentTicks()) {
                        stopForListeners(level, ModSounds.ACCELERATED_TROOPS_WARNING_1.get());
                        playToListeners(level, ModSounds.ACCELERATED_TROOPS_WARNING_2);
                    } else if (warningTicks == warningSegmentTicks() * 2) {
                        stopForListeners(level, ModSounds.ACCELERATED_TROOPS_WARNING_2.get());
                        playRandomThird(level);
                        warningThirdPlayed = true;
                    }
                    if (warningTicks >= warningDurationTicks()) {
                        enterCombat(level, nearest);
                    }
                }
                if (state != State.COMBAT) {
                    LivingEntity directTarget = resolveDirectTarget(level);
                    if (directTarget != null) {
                        commandTarget(directTarget);
                    }
                }
            }

            List<ServerPlayer> viewers = new ArrayList<>(nearbyPlayers);
            Entity combatTarget = primaryTarget == null ? null : level.getEntity(primaryTarget);
            if (state == State.COMBAT && combatTarget instanceof ServerPlayer player
                    && player.isAlive() && !viewers.contains(player)) {
                viewers.add(player);
            }
            updateBossBarPlayers(viewers);
            syncEncounterAudio(level, viewers);
            enforceFormation(center);
        }

        private long reserveUtilityUse(long earliestTick) {
            long scheduled = Math.max(earliestTick, nextUtilityUseAt);
            nextUtilityUseAt = scheduled + Math.max(1, DealtBossesConfig.intValue(
                    "accelerated_troops.utility_squad_stagger_ticks", 16));
            return scheduled;
        }

        private void enterCombat(ServerLevel level, LivingEntity target) {
            if (!(target instanceof Player)) {
                if (target != null && target.isAlive()) {
                    commandTarget(target);
                }
                return;
            }
            boolean newlyCombat = state != State.COMBAT;
            state = State.COMBAT;
            warningTicks = warningDurationTicks();
            if (target instanceof ServerPlayer player) {
                ensureAudioPlayer(level, player);
            }
            if (newlyCombat) {
                stopWarningLead(level);
                if (!warningThirdPlayed) {
                    playRandomThird(level);
                }
                warningThirdPlayed = true;
                playCombatCue(level);
            }
            if (target != null && target.isAlive()) {
                primaryTarget = target.getUUID();
                commandTarget(target);
            }
        }

        private void commandTarget(LivingEntity target) {
            for (AcceleratedTroopsEntity member : members.values()) {
                if (member.isAlive() && member.isValidEnemy(target)) {
                    member.setTarget(target);
                }
            }
        }

        private LivingEntity resolvePrimaryTarget(ServerLevel level) {
            if (primaryTarget == null) {
                return null;
            }
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(primaryTarget);
            return player != null && player.level() == level && validPlayer(player) ? player : null;
        }

        private LivingEntity resolveDirectTarget(ServerLevel level) {
            Vec3 center = center();
            return level.getEntitiesOfClass(LivingEntity.class,
                            members.values().iterator().next().getBoundingBox().inflate(warningRange()),
                            living -> !(living instanceof Player)
                                    && members.values().stream().anyMatch(member -> member.isValidEnemy(living)))
                    .stream()
                    .filter(living -> living.distanceToSqr(center) <= warningRange() * warningRange())
                    .min(Comparator.comparingDouble(living -> living.distanceToSqr(center)))
                    .orElse(null);
        }

        private void enforceFormation(Vec3 center) {
            AcceleratedTroopsEntity leader = members.values().stream()
                    .filter(AcceleratedTroopsEntity::isAlive)
                    .min(Comparator.comparing(entity -> entity.getUUID().toString()))
                    .orElse(null);
            if (leader == null) {
                return;
            }
            double maxDistance = Math.max(1.0D,
                    DealtBossesConfig.doubleValue("accelerated_troops.formation_max_distance", 6.0D));
            for (AcceleratedTroopsEntity member : members.values()) {
                if (!member.isAlive() || member == leader
                        || member.distanceToSqr(center) <= maxDistance * maxDistance) {
                    continue;
                }
                member.getNavigation().moveTo(leader, 1.2D);
                if (member.distanceToSqr(leader) > maxDistance * maxDistance * 4.0D) {
                    Vec3 offset = new Vec3(member.getRandom().nextDouble() - 0.5D, 0.0D,
                            member.getRandom().nextDouble() - 0.5D).normalize().scale(2.0D);
                    member.teleportTo(leader.getX() + offset.x, leader.getY(), leader.getZ() + offset.z);
                }
            }
        }

        private Vec3 center() {
            double x = 0.0D;
            double y = 0.0D;
            double z = 0.0D;
            int count = 0;
            for (AcceleratedTroopsEntity member : members.values()) {
                if (!member.isAlive()) {
                    continue;
                }
                x += member.getX();
                y += member.getY();
                z += member.getZ();
                count++;
            }
            return count == 0 ? Vec3.ZERO : new Vec3(x / count, y / count, z / count);
        }

        private void updateBossBar() {
            float health = 0.0F;
            float memberMaxHealth = 0.0F;
            for (AcceleratedTroopsEntity member : members.values()) {
                if (member.isAlive()) {
                    health += Math.max(0.0F, member.getHealth());
                }
                memberMaxHealth = Math.max(memberMaxHealth, member.getMaxHealth());
            }
            float maxHealth = Math.max(1.0F, memberMaxHealth) * squadSize();
            bossEvent.setProgress(Mth.clamp(health / maxHealth, 0.0F, 1.0F));
            bossEvent.setVisible(!members.isEmpty());
        }

        private void updateBossBarPlayers(List<ServerPlayer> viewers) {
            Set<UUID> current = new HashSet<>();
            for (ServerPlayer player : viewers) {
                current.add(player.getUUID());
                if (bossBarPlayers.add(player.getUUID())) {
                    bossEvent.addPlayer(player);
                }
            }
            for (UUID id : new ArrayList<>(bossBarPlayers)) {
                if (current.contains(id)) {
                    continue;
                }
                bossBarPlayers.remove(id);
                bossEvent.getPlayers().stream()
                        .filter(player -> player.getUUID().equals(id))
                        .findFirst()
                        .ifPresent(bossEvent::removePlayer);
            }
        }

        private boolean validPlayer(ServerPlayer player) {
            return player != null && player.isAlive() && !player.isCreative() && !player.isSpectator()
                    && !GhrothStateManager.isGhroth(player);
        }

        private void resetEncounter(ServerLevel level) {
            if (state == State.IDLE && audioPlayers.isEmpty()) {
                return;
            }
            state = State.IDLE;
            warningTicks = 0;
            primaryTarget = null;
            warningThirdPlayed = false;
            members.values().forEach(member -> {
                if (member.getTarget() instanceof Player) {
                    member.setTarget(null);
                }
                member.restoreAfterEncounter();
            });
            stopAllAudio(level);
        }

        private void close(ServerLevel level) {
            closed = true;
            bossEvent.setVisible(false);
            for (ServerPlayer player : new ArrayList<>(bossEvent.getPlayers())) {
                bossEvent.removePlayer(player);
            }
            bossBarPlayers.clear();
            stopAllAudio(level);
            for (SoundEvent sound : encounterSounds()) {
                RangedSoundHelper.stop(level, sound, SoundSource.HOSTILE);
            }
        }

        private void syncEncounterAudio(ServerLevel level, List<ServerPlayer> listeners) {
            if (closed) {
                return;
            }
            Set<UUID> desired = new HashSet<>();
            for (ServerPlayer player : listeners) {
                desired.add(player.getUUID());
                ensureAudioPlayer(level, player);
            }
            for (UUID id : new ArrayList<>(audioPlayers)) {
                if (desired.contains(id)) {
                    ServerPlayer player = level.getServer().getPlayerList().getPlayer(id);
                    if (player != null && level.getGameTime() - musicStartedAt.getOrDefault(id, 0L)
                            >= musicRepeatTicks()) {
                        play(player, ModSounds.ACCELERATED_TROOPS_BATTLE_MUSIC.get(), 0.75F);
                        musicStartedAt.put(id, level.getGameTime());
                    }
                    continue;
                }
                ServerPlayer player = level.getServer().getPlayerList().getPlayer(id);
                if (player != null) {
                    stopAudio(player);
                }
                audioPlayers.remove(id);
                musicStartedAt.remove(id);
            }
        }

        private void ensureAudioPlayer(ServerLevel level, ServerPlayer player) {
            if (closed) {
                return;
            }
            if (!audioPlayers.add(player.getUUID())) {
                return;
            }
            play(player, ModSounds.ACCELERATED_TROOPS_BATTLE_MUSIC.get(), 0.75F);
            musicStartedAt.put(player.getUUID(), level.getGameTime());
            if (state != State.WARNING) {
                return;
            }
            Supplier<SoundEvent> current = warningTicks < warningSegmentTicks()
                    ? ModSounds.ACCELERATED_TROOPS_WARNING_1
                    : warningTicks < warningSegmentTicks() * 2
                    ? ModSounds.ACCELERATED_TROOPS_WARNING_2
                    : randomThird(level);
            play(player, current.get(), 1.0F);
        }

        private void playToListeners(ServerLevel level, Supplier<SoundEvent> sound) {
            for (UUID id : audioPlayers) {
                ServerPlayer player = level.getServer().getPlayerList().getPlayer(id);
                if (player != null) {
                    play(player, sound.get(), 1.0F);
                }
            }
        }

        private void playRandomThird(ServerLevel level) {
            playToListeners(level, randomThird(level));
        }

        private Supplier<SoundEvent> randomThird(ServerLevel level) {
            return level.getRandom().nextBoolean()
                    ? ModSounds.ACCELERATED_TROOPS_WARNING_3_1
                    : ModSounds.ACCELERATED_TROOPS_WARNING_3_2;
        }

        private void playCombatCue(ServerLevel level) {
            List<Supplier<SoundEvent>> cues = List.of(
                    ModSounds.ACCELERATED_TROOPS_COMBAT_1,
                    ModSounds.ACCELERATED_TROOPS_COMBAT_2,
                    ModSounds.ACCELERATED_TROOPS_COMBAT_3,
                    ModSounds.ACCELERATED_TROOPS_COMBAT_4);
            playToListeners(level, cues.get(level.getRandom().nextInt(cues.size())));
        }

        private void stopWarningLead(ServerLevel level) {
            for (UUID id : audioPlayers) {
                ServerPlayer player = level.getServer().getPlayerList().getPlayer(id);
                if (player != null) {
                    RangedSoundHelper.stopForPlayer(player,
                            ModSounds.ACCELERATED_TROOPS_WARNING_1.get(), SoundSource.HOSTILE);
                    RangedSoundHelper.stopForPlayer(player,
                            ModSounds.ACCELERATED_TROOPS_WARNING_2.get(), SoundSource.HOSTILE);
                }
            }
        }

        private void stopForListeners(ServerLevel level, SoundEvent sound) {
            for (UUID id : audioPlayers) {
                ServerPlayer player = level.getServer().getPlayerList().getPlayer(id);
                if (player != null) {
                    RangedSoundHelper.stopForPlayer(player, sound, SoundSource.HOSTILE);
                }
            }
        }

        private void stopAllAudio(ServerLevel level) {
            for (UUID id : new ArrayList<>(audioPlayers)) {
                ServerPlayer player = level.getServer().getPlayerList().getPlayer(id);
                if (player != null) {
                    stopAudio(player);
                }
            }
            audioPlayers.clear();
            musicStartedAt.clear();
        }

        private void stopAudio(ServerPlayer player) {
            for (SoundEvent sound : encounterSounds()) {
                RangedSoundHelper.stopForPlayer(player, sound, SoundSource.HOSTILE);
            }
        }

        private List<SoundEvent> encounterSounds() {
            return List.of(
                    ModSounds.ACCELERATED_TROOPS_WARNING_1.get(),
                    ModSounds.ACCELERATED_TROOPS_WARNING_2.get(),
                    ModSounds.ACCELERATED_TROOPS_WARNING_3_1.get(),
                    ModSounds.ACCELERATED_TROOPS_WARNING_3_2.get(),
                    ModSounds.ACCELERATED_TROOPS_COMBAT_1.get(),
                    ModSounds.ACCELERATED_TROOPS_COMBAT_2.get(),
                    ModSounds.ACCELERATED_TROOPS_COMBAT_3.get(),
                    ModSounds.ACCELERATED_TROOPS_COMBAT_4.get(),
                    ModSounds.ACCELERATED_TROOPS_BATTLE_MUSIC.get());
        }

        private void play(ServerPlayer player, SoundEvent sound, float volume) {
            RangedSoundHelper.playForPlayer(player, sound, SoundSource.HOSTILE, volume, 1.0F);
        }

        private double warningRange() {
            return Math.max(1.0D,
                    DealtBossesConfig.doubleValue("accelerated_troops.follow_range", 75.0D));
        }

        private int warningDurationTicks() {
            return Math.max(1,
                    DealtBossesConfig.intValue("accelerated_troops.warning_duration_ticks", 20 * 20));
        }

        private int warningSegmentTicks() {
            return Math.max(1,
                    DealtBossesConfig.intValue("accelerated_troops.warning_segment_ticks", 8 * 20));
        }

        private int musicRepeatTicks() {
            return Math.max(20,
                    DealtBossesConfig.intValue("accelerated_troops.music_repeat_ticks", 121 * 20));
        }
    }

    private static final class WorldData extends SavedData {
        private UUID activeSquad;
        private long lockoutUntil;
        private boolean unlocked;
        private long nextAttemptTick;

        private static WorldData load(CompoundTag tag, HolderLookup.Provider provider) {
            WorldData data = new WorldData();
            data.activeSquad = tag.hasUUID("ActiveSquad") ? tag.getUUID("ActiveSquad") : null;
            data.lockoutUntil = tag.getLong("LockoutUntil");
            data.unlocked = tag.getBoolean("Unlocked");
            data.nextAttemptTick = tag.getLong("NextAttemptTick");
            return data;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
            if (activeSquad != null) {
                tag.putUUID("ActiveSquad", activeSquad);
            }
            tag.putLong("LockoutUntil", lockoutUntil);
            tag.putBoolean("Unlocked", unlocked);
            tag.putLong("NextAttemptTick", nextAttemptTick);
            return tag;
        }
    }
}
