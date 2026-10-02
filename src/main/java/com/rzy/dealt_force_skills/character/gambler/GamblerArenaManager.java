package com.rzy.dealt_force_skills.character.gambler;

import com.rzy.dealt_force_skills.skill.SkillCooldownHelper;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.config.GamblerArenaConfig;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.S2C_OpenGamblerDuelInvite;
import com.rzy.dealt_force_skills.registry.ModGameRules;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.util.RangedSoundHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Unit;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.StringJoiner;
import java.util.UUID;

public final class GamblerArenaManager {
    public static final ResourceKey<Level> ARENA_LEVEL = ResourceKey.create(
            Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "gambler_arena"));

    private static final int ARENA_Y = 80;
    private static final int ARENA_RADIUS = 30;
    private static final int ARENA_RAIL_THICKNESS = 5;
    private static final int ARENA_CLEAR_RADIUS = 48;
    private static final int ARENA_ENTITY_CLEANUP_RADIUS = 128;
    private static final int ARENA_RAIL_TOP_Y = ARENA_Y + 4;
    private static final int ARENA_PULLBACK_MIN_Y = ARENA_Y - 1;
    private static final int ARENA_PULLBACK_MAX_Y = ARENA_RAIL_TOP_Y + 5;
    private static final int PREVIEW_MOB_TICKS = 12 * 20;
    private static final int PREVIEW_PVP_TICKS = 6 * 20;
    private static final int DRAIN_START_TICKS = 2 * 60 * 20;
    private static final int HARD_SETTLE_TICKS = 5 * 60 * 20;
    private static final int MOB_STALEMATE_TICKS = 2 * 60 * 20;
    private static final int MOB_RETARGET_INTERVAL_TICKS = 10;
    private static final int ARMY_POWER_BUDGET = 60;
    private static final int ARMY_ELITE_POWER_CAP = 18;
    private static final int MAX_MOBS_PER_SIDE = 72;
    private static final int DYNAMIC_SELECTION_ATTEMPTS = 64;
    private static final int VILLAGER_CONVERSION_TRACK_TICKS = 12 * 20;
    private static final double VILLAGER_CONVERSION_MATCH_RADIUS = 5.0D;
    private static final int BGM_REPLAY_TICKS = 111 * 20;
    private static final double SELECTION_RANGE = 30.0D;
    private static final double ARENA_BGM_RADIUS = 96.0D;
    private static final int SPECTATOR_FLOOR_Y = ARENA_Y + 9;
    private static final int SPECTATOR_Z = ARENA_RADIUS + ARENA_RAIL_THICKNESS + 5;
    private static final int SPECTATOR_HALF_X = 12;
    private static final int SPECTATOR_HALF_Z = 5;

    private static final Map<UUID, ArenaSession> SESSIONS = new HashMap<>();
    private static final Map<UUID, UUID> PLAYER_TO_SESSION = new HashMap<>();
    private static final Map<UUID, UUID> ENTITY_TO_SESSION = new HashMap<>();
    private static final Map<UUID, PendingVillagerConversion> PENDING_VILLAGER_CONVERSIONS = new HashMap<>();
    private static final Set<UUID> FORCED_PLAYER_DEATHS = new HashSet<>();

    private GamblerArenaManager() {
    }

    public static boolean start(ServerPlayer gambler, boolean alternate) {
        ArenaSession existing = sessionForPlayer(gambler);
        if (existing != null) {
            if (tryLeaveSoloPreview(gambler, existing, alternate)) {
                return true;
            }
            gambler.displayClientMessage(Component.translatable("message.dealt_force_skills.gambler.arena.already_active"), true);
            return true;
        }
        ServerLevel arena = gambler.server.getLevel(ARENA_LEVEL);
        if (arena == null) {
            gambler.displayClientMessage(Component.translatable("message.dealt_force_skills.gambler.arena.missing_dimension"), false);
            return true;
        }
        if (alternate) {
            NetworkHandler.sendToPlayer(new S2C_OpenGamblerDuelInvite(), gambler);
            return true;
        }
        return beginSession(gambler, arena, selectedPlayers(gambler, false), false);
    }

    public static boolean startWithInvited(ServerPlayer gambler, List<UUID> invitedIds) {
        if (sessionForPlayer(gambler) != null) {
            gambler.displayClientMessage(Component.translatable("message.dealt_force_skills.gambler.arena.already_active"), true);
            return true;
        }
        ServerLevel arena = gambler.server.getLevel(ARENA_LEVEL);
        if (arena == null) {
            gambler.displayClientMessage(Component.translatable("message.dealt_force_skills.gambler.arena.missing_dimension"), false);
            return true;
        }
        List<ServerPlayer> players = new ArrayList<>();
        players.add(gambler);
        Set<UUID> seen = new HashSet<>();
        seen.add(gambler.getUUID());
        for (UUID invitedId : invitedIds) {
            if (invitedId == null || !seen.add(invitedId)) {
                continue;
            }
            ServerPlayer invited = playerById(gambler.server, invitedId);
            if (invited != null && invited.isAlive() && !invited.isSpectator()) {
                players.add(invited);
            }
        }
        if (players.size() < 2) {
            gambler.displayClientMessage(Component.translatable("message.dealt_force_skills.gambler.arena.invite_none"), true);
            return true;
        }
        return beginSession(gambler, arena, players, true);
    }

    private static boolean beginSession(ServerPlayer gambler, ServerLevel arena, List<ServerPlayer> players,
                                        boolean cooldownOnEnd) {
        if (!SESSIONS.isEmpty() || players.stream().anyMatch(player -> PLAYER_TO_SESSION.containsKey(player.getUUID()))) {
            gambler.displayClientMessage(Component.translatable("message.dealt_force_skills.gambler.arena.target_busy"), true);
            return true;
        }

        long now = arena.getGameTime();
        ArenaSession session = new ArenaSession(UUID.randomUUID(), gambler.getUUID(), now, players,
                cooldownOnEnd && players.size() > 1);
        configureSession(session, players, gambler.getRandom().nextLong());
        prepareArena(arena);
        SESSIONS.put(session.id, session);
        for (ServerPlayer player : players) {
            PLAYER_TO_SESSION.put(player.getUUID(), session.id);
            session.snapshots.put(player.getUUID(), ParticipantSnapshot.capture(player));
            movePlayerToArena(player, arena, session.spawnFor(player.getUUID()));
        }
        if (session.isMobMode()) {
            spawnArmies(session, arena, gambler.getRandom().nextLong());
        }
        applyModes(session, gambler.server);
        broadcastStart(session, gambler.server);
        playArenaBgm(session, gambler.server, now);
        return true;
    }

    private static boolean tryLeaveSoloPreview(ServerPlayer gambler, ArenaSession session, boolean alternate) {
        if (alternate
                || session.mode != ArenaMode.SOLO_MOB
                || session.phase != ArenaPhase.PREVIEW
                || !session.gamblerId.equals(gambler.getUUID())) {
            return false;
        }
        endSession(session, gambler.server, ArenaSide.LEFT, "message.dealt_force_skills.gambler.arena.solo_leave", false);
        return true;
    }

    public static void tickPlayer(ServerPlayer player) {
        ArenaSession session = sessionForPlayer(player);
        if (session == null) {
            return;
        }
        long now = SkillCooldownHelper.now(player);
        if (session.lastTick == now) {
            return;
        }
        session.lastTick = now;
        tickSession(session, player.server, now);
    }

    public static boolean blocksSkillUse(ServerPlayer player) {
        ArenaSession session = sessionForPlayer(player);
        return session != null && !session.gamblerId.equals(player.getUUID());
    }

    public static boolean blocksInteraction(ServerPlayer player) {
        ArenaSession session = sessionForPlayer(player);
        if (session == null) {
            return false;
        }
        return !session.isPvpCombatant(player.getUUID()) || session.phase != ArenaPhase.FIGHTING;
    }

    public static boolean protectsArenaBlocks(Level level) {
        return level != null && ARENA_LEVEL.equals(level.dimension());
    }

    public static boolean blocksAttackEntity(ServerPlayer player, Entity target) {
        ArenaSession session = sessionForPlayer(player);
        if (session == null) {
            return false;
        }
        if (session.phase != ArenaPhase.FIGHTING || !session.isPvpCombatant(player.getUUID())) {
            return true;
        }
        ArenaSide attackerSide = session.teams.get(player.getUUID());
        ArenaSide targetSide = sideFor(session, target);
        return attackerSide == null || targetSide == null || attackerSide == targetSide;
    }

    public static boolean shouldCancelAttack(LivingEntity target, DamageSource source) {
        if (target instanceof ServerPlayer player && FORCED_PLAYER_DEATHS.contains(player.getUUID())) {
            return false;
        }
        ArenaSession session = sessionForEntity(target);
        Entity attacker = rootAttacker(source);
        if (session == null && attacker != null) {
            session = sessionForEntity(attacker);
        }
        if (session == null) {
            return false;
        }
        if (session.phase != ArenaPhase.FIGHTING) {
            return true;
        }
        ArenaSide targetSide = sideFor(session, target);
        ArenaSide attackerSide = sideFor(session, attacker);
        if (targetSide == null || attackerSide == null) {
            return true;
        }
        return targetSide == attackerSide;
    }

    public static boolean handleLivingHurt(LivingEntity target, DamageSource source, float amount) {
        if (target instanceof ServerPlayer player && FORCED_PLAYER_DEATHS.contains(player.getUUID())) {
            return false;
        }
        ArenaSession session = sessionForEntity(target);
        if (session == null || session.phase != ArenaPhase.FIGHTING) {
            return false;
        }
        if (shouldCancelAttack(target, source)) {
            return true;
        }
        trackVillagerConversionCandidate(session, target, source);
        if (target instanceof ServerPlayer player
                && session.isPvpMode()
                && amount >= player.getHealth()) {
            eliminatePlayer(session, player, false);
            return true;
        }
        return false;
    }

    public static boolean handleLivingDeath(LivingEntity entity) {
        if (entity instanceof ServerPlayer player && FORCED_PLAYER_DEATHS.contains(player.getUUID())) {
            return false;
        }
        ArenaSession session = sessionForEntity(entity);
        if (session == null) {
            return false;
        }
        if (entity instanceof ServerPlayer player && session.isPvpMode()) {
            eliminatePlayer(session, player, false);
            return true;
        }
        if (session.mobTeams.containsKey(entity.getUUID())) {
            session.deadMobs.add(entity.getUUID());
            return false;
        }
        return false;
    }

    public static boolean placeBet(ServerPlayer player, String sideId) {
        ArenaSession session = sessionForPlayer(player);
        if (session == null) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.gambler.arena.no_session"), true);
            return false;
        }
        if (session.phase != ArenaPhase.PREVIEW) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.gambler.arena.bet_closed"), true);
            return false;
        }
        ArenaSide side = ArenaSide.from(sideId);
        if (side == null) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.gambler.arena.bet_invalid"), true);
            return false;
        }
        if (!session.canBet(player.getUUID())) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.gambler.arena.bet_not_allowed"), true);
            return false;
        }
        ArenaSide previous = session.bets.put(player.getUUID(), side);
        String messageKey = previous == null
                ? "message.dealt_force_skills.gambler.arena.bet_placed"
                : "message.dealt_force_skills.gambler.arena.bet_changed";
        player.displayClientMessage(Component.translatable(messageKey,
                sideName(side)).withStyle(side.format), false);
        return true;
    }

    public static void clearForPlayer(ServerPlayer player) {
        ArenaSession session = sessionForPlayer(player);
        if (session != null) {
            endSession(session, player.server, ArenaSide.LEFT, "message.dealt_force_skills.gambler.arena.cancelled", false);
        }
    }

    public static void clearRuntimeCaches() {
        SESSIONS.clear();
        PLAYER_TO_SESSION.clear();
        ENTITY_TO_SESSION.clear();
        PENDING_VILLAGER_CONVERSIONS.clear();
        FORCED_PLAYER_DEATHS.clear();
    }

    private static void tickSession(ArenaSession session, MinecraftServer server, long now) {
        ServerLevel arena = server.getLevel(ARENA_LEVEL);
        if (arena == null) {
            endSession(session, server, ArenaSide.LEFT, "message.dealt_force_skills.gambler.arena.cancelled", false);
            return;
        }
        if (session.arenaBgmPlaying && now >= session.nextBgmTick) {
            playArenaBgm(session, server, now);
        }
        enforceArenaBounds(session, server, arena);
        if (session.phase == ArenaPhase.PREVIEW && now >= session.fightStartTick) {
            beginFight(session, server);
        }
        if (session.phase != ArenaPhase.FIGHTING) {
            return;
        }
        if (session.isPvpMode()) {
            applyPvpDrain(session, server, now);
            Optional<ArenaSide> winner = pvpWinner(session, server);
            if (winner.isPresent()) {
                settle(session, server, winner.get());
            } else if (now - session.fightStartTick >= HARD_SETTLE_TICKS) {
                settle(session, server, pvpHealthWinner(session, server));
            }
        } else {
            claimConvertedZombieVillagers(session, arena, now);
            retargetMobArmies(session, server, now);
            Optional<ArenaSide> winner = mobWinner(session, server);
            if (winner.isPresent()) {
                settle(session, server, winner.get());
            } else if (now - session.fightStartTick >= MOB_STALEMATE_TICKS) {
                settle(session, server, mobPowerWinner(session, server));
            }
        }
    }

    private static void configureSession(ArenaSession session, List<ServerPlayer> players, long seed) {
        Random random = new Random(seed);
        int count = players.size();
        if (count <= 1) {
            session.mode = ArenaMode.SOLO_MOB;
            session.phase = ArenaPhase.PREVIEW;
            session.fightStartTick = session.createdTick + PREVIEW_MOB_TICKS;
            session.spectators.add(session.gamblerId);
            session.leftArmy = randomArmyName(random);
            session.rightArmy = differentArmy(random, session.leftArmy);
            return;
        }
        if (count == 2) {
            session.mode = ArenaMode.DUEL_MOB;
            session.phase = ArenaPhase.PREVIEW;
            session.fightStartTick = session.createdTick + PREVIEW_MOB_TICKS;
            players.forEach(player -> session.spectators.add(player.getUUID()));
            session.leftArmy = randomArmyName(random);
            session.rightArmy = differentArmy(random, session.leftArmy);
            return;
        }

        session.mode = count % 2 == 0 ? ArenaMode.PVP_EVEN : ArenaMode.PVP_ODD;
        session.phase = ArenaPhase.PREVIEW;
        session.fightStartTick = session.createdTick + PREVIEW_PVP_TICKS;
        session.spectators.add(session.gamblerId);
        List<UUID> fighters = new ArrayList<>();
        for (ServerPlayer player : players) {
            if (!player.getUUID().equals(session.gamblerId)) {
                fighters.add(player.getUUID());
            }
        }
        if (session.mode == ArenaMode.PVP_EVEN && !fighters.isEmpty()) {
            UUID secondSpectator = fighters.remove(random.nextInt(fighters.size()));
            session.spectators.add(secondSpectator);
        }
        Collections.shuffle(fighters, random);
        for (int i = 0; i < fighters.size(); i++) {
            session.teams.put(fighters.get(i), i % 2 == 0 ? ArenaSide.LEFT : ArenaSide.RIGHT);
        }
    }

    private static void applyModes(ArenaSession session, MinecraftServer server) {
        for (UUID playerId : session.players) {
            ServerPlayer player = playerById(server, playerId);
            if (player == null) {
                continue;
            }
            player.setGameMode(GameType.ADVENTURE);
        }
    }

    private static void beginFight(ArenaSession session, MinecraftServer server) {
        assignMissingBets(session, server);
        session.phase = ArenaPhase.FIGHTING;
        session.fightStartTick = server.overworld().getGameTime();
        if (session.isMobMode()) {
            for (UUID mobId : session.mobTeams.keySet()) {
                Entity entity = entityById(server, mobId);
                if (entity instanceof Mob mob) {
                    mob.setNoAi(false);
                    retargetMob(session, server, mob);
                }
            }
            retargetMobArmies(session, server, server.overworld().getGameTime());
        }
        for (UUID playerId : session.players) {
            ServerPlayer player = playerById(server, playerId);
            if (player != null) {
                player.displayClientMessage(Component.translatable("message.dealt_force_skills.gambler.arena.fight_started")
                        .withStyle(ChatFormatting.RED), false);
            }
        }
        broadcastSupporters(session, server);
    }

    private static void settle(ArenaSession session, MinecraftServer server, ArenaSide winner) {
        session.phase = ArenaPhase.SETTLED;
        ServerPlayer gambler = playerById(server, session.gamblerId);
        ArenaSide gamblerBet = session.bets.get(session.gamblerId);
        boolean gamblerCorrect = gamblerBet == winner;
        if (session.mode == ArenaMode.SOLO_MOB && gambler != null) {
            settleSoloMob(session, gambler, gamblerCorrect);
            broadcastResult(session, server, winner);
            startNextSoloRound(session, server);
            return;
        } else if (session.mode == ArenaMode.DUEL_MOB) {
            settleDuelMob(session, server, winner);
        } else if (session.mode == ArenaMode.PVP_ODD) {
            settlePvpOdd(session, server, winner);
        } else if (session.mode == ArenaMode.PVP_EVEN) {
            settlePvpEven(session, server, winner);
        }
        broadcastResult(session, server, winner);
        endSession(session, server, winner, "message.dealt_force_skills.gambler.arena.finished", true);
    }

    private static void settleSoloMob(ArenaSession session, ServerPlayer gambler, boolean gamblerCorrect) {
        if (gamblerCorrect) {
            session.soloWinStreak = Math.min(20, session.soloWinStreak + 1);
            int reward = soloStreakReward(session.soloWinStreak);
            GamblerStateManager.grantChips(gambler, reward);
            gambler.displayClientMessage(Component.translatable(
                    "message.dealt_force_skills.gambler.final_bet_solo_win",
                    session.soloWinStreak, reward).withStyle(ChatFormatting.GOLD), false);
            return;
        }
        int brokenStreak = session.soloWinStreak;
        int loss;
        if (brokenStreak > 0) {
            int percent = Math.min(100, brokenStreak * 10);
            loss = Mth.ceil(GamblerStateManager.chips(gambler) * (percent / 100.0F));
            GamblerStateManager.spendChipsUpTo(gambler, loss);
        } else {
            loss = Math.min(5, GamblerStateManager.chips(gambler));
            GamblerStateManager.spendChipsUpTo(gambler, 5);
        }
        session.soloWinStreak = 0;
        gambler.addEffect(new MobEffectInstance(MobEffects.UNLUCK, 30 * 20, 0));
        gambler.displayClientMessage(Component.translatable(
                "message.dealt_force_skills.gambler.final_bet_solo_lose",
                brokenStreak, loss).withStyle(ChatFormatting.GRAY), false);
    }

    private static int soloStreakReward(int streak) {
        int multiplier = 1 << Math.min(10, Math.max(0, streak));
        return 10 * multiplier;
    }

    private static void startNextSoloRound(ArenaSession session, MinecraftServer server) {
        ServerLevel arena = server.getLevel(ARENA_LEVEL);
        ServerPlayer gambler = playerById(server, session.gamblerId);
        if (arena == null || gambler == null || !gambler.isAlive()) {
            endSession(session, server, ArenaSide.LEFT, "message.dealt_force_skills.gambler.arena.cancelled", false);
            return;
        }
        long now = arena.getGameTime();
        clearSessionMobs(session, arena);
        session.phase = ArenaPhase.PREVIEW;
        session.fightStartTick = now + PREVIEW_MOB_TICKS;
        session.lastTick = Long.MIN_VALUE;
        session.bets.clear();
        session.teams.clear();
        session.eliminatedPlayers.clear();
        session.deadMobs.clear();
        session.spectators.clear();
        session.spectators.add(session.gamblerId);
        Random random = new Random(now ^ session.id.getMostSignificantBits() ^ (long) session.soloWinStreak << 32);
        session.leftArmy = randomArmyName(random);
        session.rightArmy = differentArmy(random, session.leftArmy);
        prepareArena(arena);
        movePlayerToArena(gambler, arena, session.spawnFor(gambler.getUUID()));
        applyModes(session, server);
        spawnArmies(session, arena, random.nextLong());
        broadcastStart(session, server);
        gambler.displayClientMessage(Component.translatable(
                "message.dealt_force_skills.gambler.arena.solo_next").withStyle(ChatFormatting.AQUA), false);
        if (!session.arenaBgmPlaying) {
            playArenaBgm(session, server, now);
        }
    }

    private static void settleDuelMob(ArenaSession session, MinecraftServer server, ArenaSide winner) {
        ServerPlayer gambler = playerById(server, session.gamblerId);
        ServerPlayer opponent = firstOtherPlayer(session, server, session.gamblerId);
        boolean gamblerCorrect = session.bets.get(session.gamblerId) == winner;
        boolean opponentCorrect = opponent != null && session.bets.get(opponent.getUUID()) == winner;
        if (gambler != null && gamblerCorrect && !opponentCorrect) {
            GamblerStateManager.grantChips(gambler, 20);
            if (opponent != null) {
                forceKillPlayer(opponent);
            }
        } else if (gambler != null && gamblerCorrect) {
            gambler.addEffect(new MobEffectInstance(MobEffects.LUCK, 30 * 60 * 20, 0));
        } else if (gambler != null && !gamblerCorrect && opponentCorrect) {
            GamblerStateManager.spendChipsUpTo(gambler, Math.max(1, GamblerStateManager.chips(gambler) / 2));
            gambler.addEffect(new MobEffectInstance(MobEffects.UNLUCK, 30 * 60 * 20, 0));
        }
    }

    private static void settlePvpOdd(ArenaSession session, MinecraftServer server, ArenaSide winner) {
        ServerPlayer gambler = playerById(server, session.gamblerId);
        if (gambler == null) {
            return;
        }
        if (session.bets.get(session.gamblerId) == winner) {
            GamblerStateManager.setChipsAmount(gambler, (GamblerStateManager.chips(gambler) + 10) * 2);
            gambler.addEffect(new MobEffectInstance(MobEffects.LUCK, 30 * 60 * 20, 1));
        } else {
            GamblerStateManager.setChipsAmount(gambler, 0);
            gambler.addEffect(new MobEffectInstance(MobEffects.UNLUCK, 30 * 60 * 20, 1));
        }
    }

    private static void settlePvpEven(ArenaSession session, MinecraftServer server, ArenaSide winner) {
        ServerPlayer gambler = playerById(server, session.gamblerId);
        ServerPlayer otherSpectator = session.spectators.stream()
                .filter(id -> !id.equals(session.gamblerId))
                .map(id -> playerById(server, id))
                .filter(player -> player != null)
                .findFirst()
                .orElse(null);
        boolean gamblerCorrect = gambler != null && session.bets.get(session.gamblerId) == winner;
        boolean otherCorrect = otherSpectator != null && session.bets.get(otherSpectator.getUUID()) == winner;
        if (gambler != null && gamblerCorrect && !otherCorrect) {
            int reward = otherSpectator == null ? 20 : Math.max(1, Mth.ceil(otherSpectator.getHealth()));
            GamblerStateManager.grantChips(gambler, reward);
            gambler.addEffect(new MobEffectInstance(MobEffects.LUCK, 30 * 60 * 20, 1));
            if (otherSpectator != null) {
                forceKillPlayer(otherSpectator);
            }
        } else if (gambler != null && !gamblerCorrect && otherCorrect) {
            GamblerStateManager.setChipsAmount(gambler, 0);
            gambler.addEffect(new MobEffectInstance(MobEffects.UNLUCK, 30 * 60 * 20, 1));
            otherSpectator.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 30 * 60 * 20, 1));
            otherSpectator.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 30 * 60 * 20, 1));
            otherSpectator.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 30 * 60 * 20, 1));
        }
    }

    private static void endSession(ArenaSession session, MinecraftServer server, ArenaSide winner, String messageKey, boolean startCooldown) {
        ServerLevel arena = server.getLevel(ARENA_LEVEL);
        if (arena != null) {
            for (UUID mobId : new ArrayList<>(session.mobTeams.keySet())) {
                Entity entity = arena.getEntity(mobId);
                if (entity != null) {
                    entity.discard();
                }
            }
        }
        for (UUID playerId : new ArrayList<>(session.players)) {
            ServerPlayer player = playerById(server, playerId);
            PLAYER_TO_SESSION.remove(playerId);
            if (player == null) {
                continue;
            }
            stopArenaBgm(player);
            ParticipantSnapshot snapshot = session.snapshots.get(playerId);
            if (snapshot != null && player.isAlive()) {
                restorePlayer(player, snapshot);
            }
            player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
            player.displayClientMessage(Component.translatable(messageKey, sideName(winner)).withStyle(ChatFormatting.GOLD), false);
        }
        session.arenaBgmPlaying = false;
        for (UUID mobId : session.mobTeams.keySet()) {
            ENTITY_TO_SESSION.remove(mobId);
        }
        SESSIONS.remove(session.id);
        ServerPlayer gambler = playerById(server, session.gamblerId);
        if (startCooldown && session.cooldownOnEnd && gambler != null) {
            GamblerStateManager.beginFinalBetCooldown(gambler);
        }
        PENDING_VILLAGER_CONVERSIONS.entrySet().removeIf(entry -> entry.getValue().sessionId.equals(session.id));
    }

    private static void prepareArena(ServerLevel arena) {
        GamblerArenaCleanupEvents.beginReset();
        try {
        loadArenaCleanupChunks(arena);
        discardNonPlayerArenaEntities(arena);
        clearArenaTerrain(arena);
        PENDING_VILLAGER_CONVERSIONS.clear();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = -ARENA_CLEAR_RADIUS; x <= ARENA_CLEAR_RADIUS; x++) {
            for (int z = -ARENA_CLEAR_RADIUS; z <= ARENA_CLEAR_RADIUS; z++) {
                double dist = Math.sqrt(x * x + z * z);
                for (int y = ARENA_Y; y <= ARENA_Y + 12; y++) {
                    arena.setBlock(pos.set(x, y, z), Blocks.AIR.defaultBlockState(), 3);
                }
                if (dist <= ARENA_RADIUS) {
                    arena.setBlock(pos.set(x, ARENA_Y - 1, z), Blocks.SMOOTH_STONE.defaultBlockState(), 3);
                } else if (dist <= ARENA_RADIUS + ARENA_RAIL_THICKNESS) {
                    arena.setBlock(pos.set(x, ARENA_Y - 1, z), Blocks.BLACKSTONE.defaultBlockState(), 3);
                    for (int y = ARENA_Y; y <= ARENA_Y + 2; y++) {
                        arena.setBlock(pos.set(x, y, z), Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState(), 3);
                    }
                    for (int y = ARENA_Y + 3; y <= ARENA_RAIL_TOP_Y; y++) {
                        arena.setBlock(pos.set(x, y, z), Blocks.IRON_BARS.defaultBlockState(), 3);
                    }
                } else {
                    arena.setBlock(pos.set(x, ARENA_Y - 1, z), Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
        for (int z = -3; z <= 3; z++) {
            arena.setBlock(new BlockPos(-22, ARENA_Y, z), Blocks.RED_WOOL.defaultBlockState(), 3);
            arena.setBlock(new BlockPos(22, ARENA_Y, z), Blocks.BLUE_WOOL.defaultBlockState(), 3);
        }
        for (int x = -SPECTATOR_HALF_X; x <= SPECTATOR_HALF_X; x++) {
            for (int z = SPECTATOR_Z - SPECTATOR_HALF_Z; z <= SPECTATOR_Z + SPECTATOR_HALF_Z; z++) {
                arena.setBlock(new BlockPos(x, SPECTATOR_FLOOR_Y, z), Blocks.BARRIER.defaultBlockState(), 3);
            }
        }
        discardNonPlayerArenaEntities(arena);
        } finally {
            GamblerArenaCleanupEvents.endReset();
        }
    }

    // Only this dimension's arena footprint belongs to the minigame. Clear occupied sections
    // at every height, including blocks placed above spectators or below the original floor.
    private static void clearArenaTerrain(ServerLevel arena) {
        var area = new net.minecraft.world.level.levelgen.structure.BoundingBox(
                -ARENA_CLEAR_RADIUS, arena.getMinBuildHeight(), -ARENA_CLEAR_RADIUS,
                ARENA_CLEAR_RADIUS, arena.getMaxBuildHeight() - 1, ARENA_CLEAR_RADIUS);
        arena.getBlockTicks().clearArea(area);
        arena.getFluidTicks().clearArea(area);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int minChunk = Math.floorDiv(-ARENA_CLEAR_RADIUS, 16);
        int maxChunk = Math.floorDiv(ARENA_CLEAR_RADIUS, 16);
        for (int cx = minChunk; cx <= maxChunk; cx++) {
            for (int cz = minChunk; cz <= maxChunk; cz++) {
                var chunk = arena.getChunk(cx, cz);
                var sections = chunk.getSections();
                for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {
                    var section = sections[sectionIndex];
                    if (section.hasOnlyAir()) continue;
                    int bottom = (chunk.getMinSection() + sectionIndex) * 16;
                    for (int x = Math.max(-ARENA_CLEAR_RADIUS, cx * 16); x <= Math.min(ARENA_CLEAR_RADIUS, cx * 16 + 15); x++) {
                        for (int z = Math.max(-ARENA_CLEAR_RADIUS, cz * 16); z <= Math.min(ARENA_CLEAR_RADIUS, cz * 16 + 15); z++) {
                            for (int y = 0; y < 16; y++) {
                                if (section.getBlockState(x & 15, y, z & 15).isAir()) continue;
                                pos.set(x, bottom + y, z);
                                if (arena.getBlockEntity(pos) instanceof net.minecraft.world.Container container) {
                                    container.clearContent();
                                }
                                arena.setBlock(pos, Blocks.AIR.defaultBlockState(), 2 | 16 | 32);
                            }
                        }
                    }
                }
            }
        }
        arena.getBlockTicks().clearArea(area);
        arena.getFluidTicks().clearArea(area);
    }

    private static void loadArenaCleanupChunks(ServerLevel arena) {
        int minChunk = Math.floorDiv(-ARENA_ENTITY_CLEANUP_RADIUS, 16);
        int maxChunk = Math.floorDiv(ARENA_ENTITY_CLEANUP_RADIUS, 16);
        for (int chunkX = minChunk; chunkX <= maxChunk; chunkX++) {
            for (int chunkZ = minChunk; chunkZ <= maxChunk; chunkZ++) {
                arena.getChunk(chunkX, chunkZ);
            }
        }
    }

    private static void discardNonPlayerArenaEntities(ServerLevel arena) {
        List<Entity> existingEntities = new ArrayList<>();
        for (Entity entity : arena.getAllEntities()) {
            existingEntities.add(entity);
        }
        for (Entity entity : existingEntities) {
            if (entity instanceof Player) {
                continue;
            }
            entity.discard();
        }
    }

    private static void clearSessionMobs(ArenaSession session, ServerLevel arena) {
        for (UUID mobId : new ArrayList<>(session.mobTeams.keySet())) {
            Entity entity = arena.getEntity(mobId);
            if (entity != null) {
                entity.discard();
            }
            ENTITY_TO_SESSION.remove(mobId);
        }
        session.mobTeams.clear();
        session.mobPowers.clear();
        session.deadMobs.clear();
        PENDING_VILLAGER_CONVERSIONS.entrySet().removeIf(entry -> entry.getValue().sessionId.equals(session.id));
    }

    private static void clearSideMobs(ArenaSession session, ServerLevel arena, ArenaSide side) {
        List<UUID> mobIds = new ArrayList<>();
        for (Map.Entry<UUID, ArenaSide> entry : session.mobTeams.entrySet()) {
            if (entry.getValue() == side) {
                mobIds.add(entry.getKey());
            }
        }
        for (UUID mobId : mobIds) {
            Entity entity = arena.getEntity(mobId);
            if (entity != null) {
                entity.discard();
            }
            session.mobTeams.remove(mobId);
            session.mobPowers.remove(mobId);
            session.deadMobs.remove(mobId);
            ENTITY_TO_SESSION.remove(mobId);
        }
    }

    private static void spawnArmies(ArenaSession session, ServerLevel arena, long seed) {
        clearSessionMobs(session, arena);
        discardNonPlayerArenaEntities(arena);
        Random random = new Random(seed);
        spawnArmySafely(session, arena, ArenaSide.LEFT, session.leftArmy, -16, random);
        spawnArmySafely(session, arena, ArenaSide.RIGHT, session.rightArmy, 16, random);
    }

    private static void spawnArmySafely(ArenaSession session, ServerLevel arena, ArenaSide side, String army,
                                        int xBase, Random random) {
        try {
            spawnArmy(session, arena, side, army, xBase, random);
        } catch (RuntimeException ignored) {
            clearSideMobs(session, arena, side);
            spawnEmergencyArmy(session, arena, side, xBase, random);
        }
    }

    private static void spawnArmy(ArenaSession session, ServerLevel arena, ArenaSide side, String army, int xBase, Random random) {
        if (arena.getGameRules().getBoolean(ModGameRules.DEALT_GAMBLER)) {
            try {
                if (spawnDynamicArmy(session, arena, side, army, xBase, random)) {
                    return;
                }
            } catch (RuntimeException ignored) {
                clearSideMobs(session, arena, side);
            }
        }
        spawnConfiguredArmy(session, arena, side, army, xBase, random);
    }

    private static void spawnConfiguredArmy(ArenaSession session, ServerLevel arena, ArenaSide side, String army,
                                            int xBase, Random random) {
        ArmyRoster roster = armyRoster(army);
        int remainingPower = roster.totalValue();
        int elitePowerCap = roster.eliteValueCap();
        int maxMobs = roster.maxMobs();
        int elitePower = 0;
        int spawned = 0;
        for (ArmyUnit unit : roster.mandatory()) {
            if (spawned >= maxMobs) {
                break;
            }
            Mob mob = spawnArmyUnit(session, arena, side, unit, xBase, spawned, random);
            if (mob != null) {
                spawned++;
            }
            remainingPower = Math.max(0, remainingPower - unit.power());
            if (roster.elite().contains(unit)) {
                elitePower += unit.power();
            }
        }
        while (remainingPower > 0 && spawned < maxMobs) {
            ArmyUnit unit = selectArmyUnit(roster, remainingPower, spawned, elitePower, elitePowerCap, random);
            if (unit == null) {
                break;
            }
            Mob mob = spawnArmyUnit(session, arena, side, unit, xBase, spawned, random);
            if (mob == null) {
                remainingPower -= unit.power();
                continue;
            }
            remainingPower -= unit.power();
            if (roster.elite().contains(unit)) {
                elitePower += unit.power();
            }
            spawned++;
        }
    }

    private static void spawnEmergencyArmy(ArenaSession session, ServerLevel arena, ArenaSide side, int xBase, Random random) {
        ArmyUnit[] fallback = { unit(EntityType.ZOMBIE, 1), unit(EntityType.SKELETON, 1) };
        for (int spawned = 0; spawned < 12; spawned++) {
            try {
                spawnArmyUnit(session, arena, side, fallback[spawned % fallback.length], xBase, spawned, random);
            } catch (RuntimeException ignored) {
                // Keep arena startup alive even if an unexpected entity creation hook fails.
            }
        }
    }

    private static boolean spawnDynamicArmy(ArenaSession session, ServerLevel arena, ArenaSide side, String army,
                                           int xBase, Random random) {
        List<DynamicArmyUnit> candidates = dynamicArmyCandidates(arena);
        if (candidates.isEmpty()) {
            return false;
        }
        GamblerArenaConfig.DynamicArmySettings settings = GamblerArenaConfig.dynamicArmySettings();
        int target = randomBetween(settings.minTotalValue(), settings.maxTotalValue(), random);
        DynamicArmyPlan plan = buildDynamicArmyPlan(candidates, settings, target, random);
        if (plan.entries().isEmpty()) {
            return false;
        }
        int spawned = 0;
        for (DynamicArmyEntry entry : plan.entries()) {
            for (int i = 0; i < entry.count() && spawned < plan.maxMobs(); i++) {
                Mob mob;
                try {
                    mob = spawnArmyUnit(session, arena, side, entry.unit().unit(), xBase, spawned, random);
                } catch (RuntimeException ignored) {
                    continue;
                }
                if (mob != null) {
                    session.mobPowers.put(mob.getUUID(), entry.unit().value());
                    spawned++;
                }
            }
        }
        return spawned > 0;
    }

    private static List<DynamicArmyUnit> dynamicArmyCandidates(ServerLevel arena) {
        Map<EntityType<?>, DynamicArmyUnit> candidates = new HashMap<>();
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            addDynamicCandidate(candidates, unit(type, 1), arena);
        }
        return new ArrayList<>(candidates.values());
    }

    private static void addDynamicCandidate(Map<EntityType<?>, DynamicArmyUnit> candidates,
                                            ArmyUnit unit, ServerLevel arena) {
        if (unit.type() == EntityType.PLAYER) {
            return;
        }
        int value = dynamicMobValue(arena, unit);
        if (value <= 0) {
            return;
        }
        candidates.put(unit.type(), new DynamicArmyUnit(unit, value));
    }

    private static int dynamicMobValue(ServerLevel arena, ArmyUnit unit) {
        Entity entity = null;
        try {
            entity = unit.type().create(arena);
            if (!(entity instanceof Mob mob)) {
                return 0;
            }
            equipMob(mob, unit, new Random(0L));
            var attackAttribute = mob.getAttribute(Attributes.ATTACK_DAMAGE);
            double attack = attackAttribute == null ? 0.0D : Math.max(0.0D, attackAttribute.getValue());
            return Math.max(1, (int) Math.ceil(mob.getMaxHealth() + attack));
        } catch (RuntimeException ignored) {
            return 0;
        } finally {
            if (entity != null) {
                entity.discard();
            }
        }
    }

    private static DynamicArmyPlan buildDynamicArmyPlan(List<DynamicArmyUnit> candidates,
                                                       GamblerArenaConfig.DynamicArmySettings settings,
                                                       int target, Random random) {
        DynamicArmyPlan best = null;
        for (int attempt = 0; attempt < DYNAMIC_SELECTION_ATTEMPTS; attempt++) {
            DynamicArmyPlan plan = dynamicArmyPlanForSelection(
                    randomDynamicSelection(candidates, settings.maxMobTypes(), random),
                    settings, target, random);
            if (best == null || plan.score(settings) < best.score(settings)) {
                best = plan;
            }
            if (plan.isWithinValueRange(settings)) {
                return plan;
            }
        }
        DynamicArmyPlan strongest = dynamicArmyPlanForSelection(
                strongestDynamicSelection(candidates, settings.maxMobTypes()),
                settings, target, random);
        if (best == null || strongest.score(settings) < best.score(settings)) {
            best = strongest;
        }
        return best == null || best.entries().isEmpty() ? fallbackDynamicArmyPlan(candidates, settings) : best;
    }

    private static DynamicArmyPlan fallbackDynamicArmyPlan(List<DynamicArmyUnit> candidates,
                                                          GamblerArenaConfig.DynamicArmySettings settings) {
        if (candidates.isEmpty()) {
            return new DynamicArmyPlan(List.of(), 0, 0, settings.maxMobs());
        }
        DynamicArmyUnit unit = lowestDynamicUnit(candidates);
        return new DynamicArmyPlan(List.of(new DynamicArmyEntry(unit, 1)), unit.value(), 1, settings.maxMobs());
    }

    private static List<DynamicArmyUnit> randomDynamicSelection(List<DynamicArmyUnit> candidates,
                                                               int maxTypes, Random random) {
        List<DynamicArmyUnit> shuffled = new ArrayList<>(candidates);
        Collections.shuffle(shuffled, random);
        return new ArrayList<>(shuffled.subList(0, Math.min(Math.max(1, maxTypes), shuffled.size())));
    }

    private static List<DynamicArmyUnit> strongestDynamicSelection(List<DynamicArmyUnit> candidates, int maxTypes) {
        List<DynamicArmyUnit> sorted = new ArrayList<>(candidates);
        sorted.sort((left, right) -> Integer.compare(right.value(), left.value()));
        return new ArrayList<>(sorted.subList(0, Math.min(Math.max(1, maxTypes), sorted.size())));
    }

    private static DynamicArmyPlan dynamicArmyPlanForSelection(List<DynamicArmyUnit> selected,
                                                              GamblerArenaConfig.DynamicArmySettings settings,
                                                              int target, Random random) {
        if (selected.isEmpty()) {
            return new DynamicArmyPlan(List.of(), 0, 0, settings.maxMobs());
        }
        int maxMobs = settings.maxMobs();
        if (dynamicMaxPotential(selected, maxMobs) < settings.minTotalValue()) {
            maxMobs *= 2;
        }
        Map<DynamicArmyUnit, Integer> counts = new HashMap<>();
        int totalValue = 0;
        int totalCount = 0;
        while (totalCount < maxMobs && totalValue < target) {
            int remaining = target - totalValue;
            List<DynamicArmyUnit> source = affordableDynamic(selected, remaining);
            DynamicArmyUnit unit;
            if (source.isEmpty()) {
                if (totalValue >= settings.minTotalValue()) {
                    break;
                }
                unit = lowestDynamicUnit(selected);
            } else {
                unit = selectDynamicUnit(source, random);
            }
            totalValue += unit.value();
            totalCount++;
            counts.merge(unit, 1, Integer::sum);
            if (totalValue >= settings.minTotalValue()
                    && totalValue <= settings.maxTotalValue()
                    && random.nextFloat() < 0.18F) {
                break;
            }
        }
        if (totalValue < settings.minTotalValue() && totalCount < maxMobs) {
            DynamicArmyUnit strongest = strongestDynamicUnit(selected);
            while (totalValue < settings.minTotalValue() && totalCount < maxMobs) {
                totalValue += strongest.value();
                totalCount++;
                counts.merge(strongest, 1, Integer::sum);
            }
        }
        List<DynamicArmyEntry> entries = new ArrayList<>();
        for (Map.Entry<DynamicArmyUnit, Integer> entry : counts.entrySet()) {
            entries.add(new DynamicArmyEntry(entry.getKey(), entry.getValue()));
        }
        Collections.shuffle(entries, random);
        return new DynamicArmyPlan(List.copyOf(entries), totalValue, totalCount, maxMobs);
    }

    private static int dynamicMaxPotential(List<DynamicArmyUnit> selected, int maxMobs) {
        return strongestDynamicUnit(selected).value() * maxMobs;
    }

    private static List<DynamicArmyUnit> affordableDynamic(List<DynamicArmyUnit> units, int remainingValue) {
        List<DynamicArmyUnit> result = new ArrayList<>();
        for (DynamicArmyUnit unit : units) {
            if (unit.value() <= remainingValue) {
                result.add(unit);
            }
        }
        return result;
    }

    private static DynamicArmyUnit selectDynamicUnit(List<DynamicArmyUnit> source, Random random) {
        long totalWeight = 0L;
        for (DynamicArmyUnit unit : source) {
            totalWeight += Math.max(1, unit.value());
        }
        long roll = Math.floorMod(random.nextLong(), Math.max(1L, totalWeight));
        for (DynamicArmyUnit unit : source) {
            roll -= Math.max(1, unit.value());
            if (roll < 0L) {
                return unit;
            }
        }
        return source.get(source.size() - 1);
    }

    private static DynamicArmyUnit strongestDynamicUnit(List<DynamicArmyUnit> units) {
        return units.stream()
                .max(Comparator.comparingInt(DynamicArmyUnit::value))
                .orElseThrow();
    }

    private static DynamicArmyUnit lowestDynamicUnit(List<DynamicArmyUnit> units) {
        return units.stream()
                .min(Comparator.comparingInt(DynamicArmyUnit::value))
                .orElseThrow();
    }

    private static int randomBetween(int min, int max, Random random) {
        if (max <= min) {
            return min;
        }
        return min + random.nextInt(max - min + 1);
    }

    private static Mob spawnArmyUnit(ArenaSession session, ServerLevel arena, ArenaSide side, ArmyUnit unit,
                                    int xBase, int spawned, Random random) {
        Entity entity = unit.type().create(arena);
        if (!(entity instanceof Mob mob)) {
            if (entity != null) {
                entity.discard();
            }
            return null;
        }
        int column = spawned % 8;
        int row = (spawned / 8) % 16;
        double z = -21.0D + row * 2.8D;
        double xOffset = (column - 3.5D) * 1.25D;
        mob.moveTo(xBase + xOffset + random.nextDouble() * 2.0D - 1.0D, ARENA_Y, z,
                side == ArenaSide.LEFT ? -90.0F : 90.0F, 0.0F);
        equipMob(mob, unit, random);
        mob.setPersistenceRequired();
        mob.setNoAi(true);
        arena.addFreshEntity(mob);
        session.mobTeams.put(mob.getUUID(), side);
        session.mobPowers.put(mob.getUUID(), unit.power());
        ENTITY_TO_SESSION.put(mob.getUUID(), session.id);
        return mob;
    }

    private static ArmyRoster armyRoster(String army) {
        GamblerArenaConfig.ArmyDefinition definition = GamblerArenaConfig.army(army);
        List<ArmyUnit> basic = unitsFromConfig(definition.basic());
        List<ArmyUnit> elite = unitsFromConfig(definition.elite());
        List<ArmyUnit> mandatory = unitsFromConfig(definition.mandatory());
        if (basic.isEmpty() && elite.isEmpty()) {
            basic = List.of(unit(EntityType.ZOMBIE, 1), unit(EntityType.SKELETON, 1), unit(EntityType.HUSK, 1));
            elite = List.of(unit(EntityType.PILLAGER, 2), unit(EntityType.VINDICATOR, 3));
        }
        return new ArmyRoster(basic, elite, mandatory,
                Math.max(1, definition.totalValue()),
                Math.max(0, definition.eliteValueCap()),
                Math.max(1, definition.maxMobs()));
    }

    private static List<ArmyUnit> unitsFromConfig(List<GamblerArenaConfig.UnitDefinition> definitions) {
        List<ArmyUnit> units = new ArrayList<>();
        for (GamblerArenaConfig.UnitDefinition definition : definitions) {
            resolveMobType(definition.mobId()).ifPresent(type ->
                    units.add(new ArmyUnit(type, Math.max(1, definition.value()),
                            Math.max(1, definition.weight()), definition.equipment())));
        }
        return units;
    }

    private static Optional<EntityType<?>> resolveMobType(String id) {
        ResourceLocation location = ResourceLocation.tryParse(id);
        if (location == null) {
            return Optional.empty();
        }
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(location);
        if (type == null || type == EntityType.PLAYER) {
            return Optional.empty();
        }
        return Optional.of(type);
    }

    private static ArmyUnit unit(EntityType<?> type, int power) {
        return new ArmyUnit(type, power, 1, Map.of());
    }

    private static ArmyUnit selectArmyUnit(ArmyRoster roster, int remainingPower, int spawned, int elitePower,
                                          int elitePowerCap, Random random) {
        List<ArmyUnit> affordableBasic = affordable(roster.basic(), remainingPower);
        List<ArmyUnit> affordableElite = affordable(roster.elite(),
                Math.min(remainingPower, Math.max(0, elitePowerCap - elitePower)));
        boolean allowElite = spawned >= 12 && !affordableElite.isEmpty() && random.nextFloat() < 0.07F;
        List<ArmyUnit> source = allowElite ? affordableElite : affordableBasic;
        if (source.isEmpty()) {
            source = affordableBasic.isEmpty() ? affordableElite : affordableBasic;
        }
        if (source.isEmpty()) {
            return null;
        }
        int totalWeight = 0;
        for (ArmyUnit unit : source) {
            totalWeight += Math.max(1, unit.weight());
        }
        int roll = random.nextInt(Math.max(1, totalWeight));
        for (ArmyUnit unit : source) {
            roll -= Math.max(1, unit.weight());
            if (roll < 0) {
                return unit;
            }
        }
        return source.get(source.size() - 1);
    }

    private static List<ArmyUnit> affordable(List<ArmyUnit> units, int remainingPower) {
        List<ArmyUnit> result = new ArrayList<>();
        for (ArmyUnit unit : units) {
            if (unit.power() <= remainingPower) {
                result.add(unit);
            }
        }
        return result;
    }

    private static void equipMob(Mob mob, ArmyUnit unit, Random random) {
        EntityType<?> type = unit.type();
        if (!unit.equipment().isEmpty()) {
            equipConfigured(mob, unit.equipment());
            return;
        }
        if (type == EntityType.SKELETON || type == EntityType.STRAY) {
            equip(mob, EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
        } else if (type == EntityType.PILLAGER) {
            equip(mob, EquipmentSlot.MAINHAND, new ItemStack(Items.CROSSBOW));
        } else if (type == EntityType.VINDICATOR) {
            equip(mob, EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_AXE));
        } else if (type == EntityType.WITHER_SKELETON) {
            equip(mob, EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        } else if (type == EntityType.DROWNED) {
            equip(mob, EquipmentSlot.MAINHAND, new ItemStack(random.nextBoolean() ? Items.TRIDENT : Items.IRON_SWORD));
        } else if (type == EntityType.ZOMBIE || type == EntityType.HUSK || type == EntityType.ZOMBIFIED_PIGLIN) {
            equip(mob, EquipmentSlot.MAINHAND, new ItemStack(random.nextBoolean() ? Items.IRON_SWORD : Items.IRON_AXE));
        }

        if (canWearArmor(type) && random.nextFloat() < 0.75F) {
            ItemStack[] armor = armorSet(random);
            equip(mob, EquipmentSlot.HEAD, armor[0]);
            equip(mob, EquipmentSlot.CHEST, armor[1]);
            equip(mob, EquipmentSlot.LEGS, armor[2]);
            equip(mob, EquipmentSlot.FEET, armor[3]);
        }
    }

    private static void equipConfigured(Mob mob, Map<String, GamblerArenaConfig.EquipmentSpec> equipment) {
        equipConfigured(mob, EquipmentSlot.MAINHAND, equipment.get("mainhand"));
        equipConfigured(mob, EquipmentSlot.OFFHAND, equipment.get("offhand"));
        equipConfigured(mob, EquipmentSlot.HEAD, equipment.get("head"));
        equipConfigured(mob, EquipmentSlot.CHEST, equipment.get("chest"));
        equipConfigured(mob, EquipmentSlot.LEGS, equipment.get("legs"));
        equipConfigured(mob, EquipmentSlot.FEET, equipment.get("feet"));
    }

    private static void equipConfigured(Mob mob, EquipmentSlot slot, GamblerArenaConfig.EquipmentSpec spec) {
        if (spec == null || spec.itemId().isBlank()) {
            return;
        }
        ItemStack stack = configuredStack(spec);
        if (!stack.isEmpty()) {
            equip(mob, slot, stack);
        }
    }

    private static ItemStack configuredStack(GamblerArenaConfig.EquipmentSpec spec) {
        ResourceLocation id = ResourceLocation.tryParse(spec.itemId());
        if (id == null) {
            return ItemStack.EMPTY;
        }
        Item item = BuiltInRegistries.ITEM.get(id);
        if (item == null || item == Items.AIR) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = new ItemStack(item);
        if (!spec.nbt().isBlank()) {
            try {
                CompoundTag tag = TagParser.parseTag(spec.nbt());
                stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            } catch (CommandSyntaxException ignored) {
                return stack;
            }
        }
        return stack;
    }

    private static void trackVillagerConversionCandidate(ArenaSession session, LivingEntity target, DamageSource source) {
        if (target.getType() != EntityType.VILLAGER || !session.mobTeams.containsKey(target.getUUID())) {
            return;
        }
        Entity attacker = rootAttacker(source);
        if (!(attacker instanceof Mob mob) || !isZombieConversionSource(mob)) {
            return;
        }
        ArenaSide attackerSide = session.mobTeams.get(mob.getUUID());
        if (attackerSide == null) {
            return;
        }
        int power = Math.max(1, session.mobPowers.getOrDefault(mob.getUUID(), 1));
        long expires = SkillCooldownHelper.now(target) + VILLAGER_CONVERSION_TRACK_TICKS;
        PENDING_VILLAGER_CONVERSIONS.put(target.getUUID(), new PendingVillagerConversion(session.id, attackerSide,
                power, target.getX(), target.getY(), target.getZ(), expires));
    }

    private static boolean isZombieConversionSource(Mob mob) {
        EntityType<?> type = mob.getType();
        return type == EntityType.ZOMBIE || type == EntityType.ZOMBIE_VILLAGER;
    }

    private static void claimConvertedZombieVillagers(ArenaSession session, ServerLevel arena, long now) {
        PENDING_VILLAGER_CONVERSIONS.entrySet().removeIf(entry ->
                now > entry.getValue().expiresTick || !SESSIONS.containsKey(entry.getValue().sessionId));
        if (PENDING_VILLAGER_CONVERSIONS.isEmpty()) {
            return;
        }
        AABB scanBox = new AABB(-ARENA_CLEAR_RADIUS, ARENA_Y - 8, -ARENA_CLEAR_RADIUS,
                ARENA_CLEAR_RADIUS, ARENA_Y + 24, ARENA_CLEAR_RADIUS);
        for (Mob mob : arena.getEntitiesOfClass(Mob.class, scanBox,
                mob -> mob.getType() == EntityType.ZOMBIE_VILLAGER && !session.mobTeams.containsKey(mob.getUUID()))) {
            Map.Entry<UUID, PendingVillagerConversion> match = nearestConversion(session, mob, now);
            if (match == null) {
                continue;
            }
            PendingVillagerConversion conversion = match.getValue();
            session.mobTeams.put(mob.getUUID(), conversion.side);
            session.mobPowers.put(mob.getUUID(), conversion.power);
            ENTITY_TO_SESSION.put(mob.getUUID(), session.id);
            mob.setPersistenceRequired();
            mob.setNoAi(false);
            retargetMob(session, arena.getServer(), mob);
            PENDING_VILLAGER_CONVERSIONS.remove(match.getKey());
        }
    }

    private static Map.Entry<UUID, PendingVillagerConversion> nearestConversion(ArenaSession session, Mob mob, long now) {
        double bestDistance = VILLAGER_CONVERSION_MATCH_RADIUS * VILLAGER_CONVERSION_MATCH_RADIUS;
        Map.Entry<UUID, PendingVillagerConversion> best = null;
        for (Map.Entry<UUID, PendingVillagerConversion> entry : PENDING_VILLAGER_CONVERSIONS.entrySet()) {
            PendingVillagerConversion conversion = entry.getValue();
            if (!conversion.sessionId.equals(session.id) || now > conversion.expiresTick) {
                continue;
            }
            double distance = mob.distanceToSqr(conversion.x, conversion.y, conversion.z);
            if (distance <= bestDistance) {
                bestDistance = distance;
                best = entry;
            }
        }
        return best;
    }

    private static boolean canWearArmor(EntityType<?> type) {
        return type == EntityType.ZOMBIE
                || type == EntityType.HUSK
                || type == EntityType.DROWNED
                || type == EntityType.SKELETON
                || type == EntityType.STRAY
                || type == EntityType.PILLAGER
                || type == EntityType.VINDICATOR
                || type == EntityType.EVOKER
                || type == EntityType.ZOMBIFIED_PIGLIN
                || type == EntityType.WITHER_SKELETON;
    }

    private static ItemStack[] armorSet(Random random) {
        int roll = random.nextInt(100);
        if (roll < 12) {
            return new ItemStack[] {
                    new ItemStack(Items.DIAMOND_HELMET),
                    new ItemStack(Items.DIAMOND_CHESTPLATE),
                    new ItemStack(Items.DIAMOND_LEGGINGS),
                    new ItemStack(Items.DIAMOND_BOOTS)
            };
        }
        if (roll < 65) {
            return new ItemStack[] {
                    new ItemStack(Items.IRON_HELMET),
                    new ItemStack(Items.IRON_CHESTPLATE),
                    new ItemStack(Items.IRON_LEGGINGS),
                    new ItemStack(Items.IRON_BOOTS)
            };
        }
        if (roll < 85) {
            return new ItemStack[] {
                    new ItemStack(Items.CHAINMAIL_HELMET),
                    new ItemStack(Items.CHAINMAIL_CHESTPLATE),
                    new ItemStack(Items.CHAINMAIL_LEGGINGS),
                    new ItemStack(Items.CHAINMAIL_BOOTS)
            };
        }
        return new ItemStack[] {
                new ItemStack(Items.GOLDEN_HELMET),
                new ItemStack(Items.GOLDEN_CHESTPLATE),
                new ItemStack(Items.GOLDEN_LEGGINGS),
                new ItemStack(Items.GOLDEN_BOOTS)
        };
    }

    private static void equip(Mob mob, EquipmentSlot slot, ItemStack stack) {
        mob.setItemSlot(slot, stack);
        mob.setDropChance(slot, 0.0F);
    }

    private static void retargetMob(ArenaSession session, MinecraftServer server, Mob mob) {
        ArenaSide ownSide = session.mobTeams.get(mob.getUUID());
        if (ownSide == null) {
            return;
        }
        LivingEntity target = session.mobTeams.entrySet().stream()
                .filter(entry -> entry.getValue() != ownSide && !session.deadMobs.contains(entry.getKey()))
                .map(entry -> entityById(server, entry.getKey()))
                .filter(entity -> entity instanceof LivingEntity living && living.isAlive())
                .map(entity -> (LivingEntity) entity)
                .min(Comparator.comparingDouble(mob::distanceToSqr))
                .orElse(null);
        mob.setTarget(target);
        if (target != null) {
            forceArenaCombatTarget(mob, target);
            mob.getNavigation().moveTo(target, 1.15D);
        }
    }

    private static void forceArenaCombatTarget(Mob mob, LivingEntity target) {
        if (mob instanceof Warden warden) {
            warden.increaseAngerAt(target, 150, false);
            warden.setAttackTarget(target);
            warden.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, target);
            warden.getBrain().eraseMemory(MemoryModuleType.ROAR_TARGET);
            warden.getBrain().setMemoryWithExpiry(MemoryModuleType.DIG_COOLDOWN, Unit.INSTANCE, 30 * 20L);
            warden.getBrain().eraseMemory(MemoryModuleType.DISTURBANCE_LOCATION);
            warden.getBrain().eraseMemory(MemoryModuleType.IS_EMERGING);
            warden.getBrain().setActiveActivityIfPossible(Activity.FIGHT);
        }
    }

    private static void retargetMobArmies(ArenaSession session, MinecraftServer server, long now) {
        if (now % MOB_RETARGET_INTERVAL_TICKS != 0L) {
            return;
        }
        for (UUID mobId : session.mobTeams.keySet()) {
            Entity entity = entityById(server, mobId);
            if (entity instanceof Mob mob && mob.isAlive() && !session.deadMobs.contains(mobId)) {
                retargetMob(session, server, mob);
            }
        }
    }

    private static Optional<ArenaSide> mobWinner(ArenaSession session, MinecraftServer server) {
        boolean leftAlive = hasLivingMob(session, server, ArenaSide.LEFT);
        boolean rightAlive = hasLivingMob(session, server, ArenaSide.RIGHT);
        if (leftAlive == rightAlive) {
            return Optional.empty();
        }
        return Optional.of(leftAlive ? ArenaSide.LEFT : ArenaSide.RIGHT);
    }

    private static ArenaSide mobPowerWinner(ArenaSession session, MinecraftServer server) {
        float left = mobCombatPower(session, server, ArenaSide.LEFT);
        float right = mobCombatPower(session, server, ArenaSide.RIGHT);
        return left >= right ? ArenaSide.LEFT : ArenaSide.RIGHT;
    }

    private static boolean hasLivingMob(ArenaSession session, MinecraftServer server, ArenaSide side) {
        return session.mobTeams.entrySet().stream()
                .anyMatch(entry -> entry.getValue() == side
                        && !session.deadMobs.contains(entry.getKey())
                        && entityById(server, entry.getKey()) instanceof LivingEntity living
                        && living.isAlive());
    }

    private static float mobCombatPower(ArenaSession session, MinecraftServer server, ArenaSide side) {
        float total = 0.0F;
        for (Map.Entry<UUID, ArenaSide> entry : session.mobTeams.entrySet()) {
            if (entry.getValue() != side || session.deadMobs.contains(entry.getKey())) {
                continue;
            }
            if (entityById(server, entry.getKey()) instanceof LivingEntity living && living.isAlive()) {
                int basePower = session.mobPowers.getOrDefault(entry.getKey(), 1);
                float healthRatio = living.getMaxHealth() <= 0.0F ? 0.0F : living.getHealth() / living.getMaxHealth();
                total += basePower * Math.max(0.0F, healthRatio);
            }
        }
        return total;
    }

    private static void applyPvpDrain(ArenaSession session, MinecraftServer server, long now) {
        long elapsed = now - session.fightStartTick;
        if (elapsed < DRAIN_START_TICKS || now % 20L != 0L) {
            return;
        }
        int extraSteps = (int) ((elapsed - DRAIN_START_TICKS) / (10 * 20L));
        float percent = Math.min(0.50F, 0.01F + extraSteps * 0.01F);
        for (UUID fighterId : session.teams.keySet()) {
            if (session.eliminatedPlayers.contains(fighterId)) {
                continue;
            }
            ServerPlayer fighter = playerById(server, fighterId);
            if (fighter != null && fighter.isAlive()) {
                float amount = fighter.getMaxHealth() * percent;
                if (amount >= fighter.getHealth()) {
                    eliminatePlayer(session, fighter, false);
                } else {
                    fighter.setHealth(Math.max(1.0F, fighter.getHealth() - amount));
                }
            }
        }
    }

    private static Optional<ArenaSide> pvpWinner(ArenaSession session, MinecraftServer server) {
        boolean leftAlive = hasLivingFighter(session, server, ArenaSide.LEFT);
        boolean rightAlive = hasLivingFighter(session, server, ArenaSide.RIGHT);
        if (leftAlive == rightAlive) {
            return Optional.empty();
        }
        return Optional.of(leftAlive ? ArenaSide.LEFT : ArenaSide.RIGHT);
    }

    private static ArenaSide pvpHealthWinner(ArenaSession session, MinecraftServer server) {
        float left = fighterHealth(session, server, ArenaSide.LEFT);
        float right = fighterHealth(session, server, ArenaSide.RIGHT);
        return left >= right ? ArenaSide.LEFT : ArenaSide.RIGHT;
    }

    private static boolean hasLivingFighter(ArenaSession session, MinecraftServer server, ArenaSide side) {
        return session.teams.entrySet().stream()
                .anyMatch(entry -> entry.getValue() == side
                        && !session.eliminatedPlayers.contains(entry.getKey())
                        && playerById(server, entry.getKey()) != null
                        && playerById(server, entry.getKey()).isAlive());
    }

    private static float fighterHealth(ArenaSession session, MinecraftServer server, ArenaSide side) {
        float total = 0.0F;
        for (Map.Entry<UUID, ArenaSide> entry : session.teams.entrySet()) {
            if (entry.getValue() != side || session.eliminatedPlayers.contains(entry.getKey())) {
                continue;
            }
            ServerPlayer player = playerById(server, entry.getKey());
            if (player != null && player.isAlive()) {
                total += player.getHealth();
            }
        }
        return total;
    }

    private static void eliminatePlayer(ArenaSession session, ServerPlayer player, boolean kill) {
        session.eliminatedPlayers.add(player.getUUID());
        player.setHealth(Math.max(1.0F, player.getHealth()));
        if (kill) {
            forceKillPlayer(player);
            return;
        }
        ServerLevel arena = player.server.getLevel(ARENA_LEVEL);
        if (arena != null) {
            movePlayerToArena(player, arena, session.spawnFor(player.getUUID()));
        }
        player.displayClientMessage(Component.translatable("message.dealt_force_skills.gambler.arena.eliminated")
                .withStyle(ChatFormatting.GRAY), false);
    }

    private static void forceKillPlayer(ServerPlayer player) {
        FORCED_PLAYER_DEATHS.add(player.getUUID());
        player.hurt(player.damageSources().genericKill(), Float.MAX_VALUE);
        FORCED_PLAYER_DEATHS.remove(player.getUUID());
    }

    private static void enforceArenaBounds(ArenaSession session, MinecraftServer server, ServerLevel arena) {
        for (UUID playerId : session.players) {
            ServerPlayer player = playerById(server, playerId);
            if (player == null || !player.isAlive()) {
                continue;
            }
            if (player.serverLevel() != arena) {
                continue;
            }
            if (session.isObservationOnly(playerId)) {
                if (!isInsideObservationArea(player)) {
                    movePlayerToArena(player, arena, session.spawnFor(playerId));
                }
                continue;
            }
            if (session.phase == ArenaPhase.PREVIEW && session.isPvpCombatant(playerId)) {
                ArenaSide side = session.teams.get(playerId);
                if (!isInsideStagingArea(player, side)) {
                    movePlayerToArena(player, arena, session.spawnFor(playerId));
                }
                continue;
            }
            if (!isInsideCombatArena(player)) {
                movePlayerToArena(player, arena, session.spawnFor(playerId));
            }
        }
        if (session.isMobMode()) {
            enforceMobArenaBounds(session, server, arena);
        }
    }

    private static void enforceMobArenaBounds(ArenaSession session, MinecraftServer server, ServerLevel arena) {
        for (Map.Entry<UUID, ArenaSide> entry : session.mobTeams.entrySet()) {
            if (session.deadMobs.contains(entry.getKey())) {
                continue;
            }
            Entity entity = entityById(server, entry.getKey());
            if (!(entity instanceof Mob mob) || !mob.isAlive() || mob.level() != arena) {
                continue;
            }
            if (!isInsideCombatArena(mob)) {
                moveMobToSideSpawn(mob, entry.getValue());
                if (session.phase == ArenaPhase.FIGHTING) {
                    retargetMob(session, server, mob);
                }
            }
        }
    }

    private static boolean isInsideObservationArea(ServerPlayer player) {
        return Math.abs(player.getX()) <= SPECTATOR_HALF_X - 1
                && Math.abs(player.getZ() - SPECTATOR_Z) <= SPECTATOR_HALF_Z - 1
                && player.getY() >= SPECTATOR_FLOOR_Y
                && player.getY() <= SPECTATOR_FLOOR_Y + 5;
    }

    private static boolean isInsideStagingArea(ServerPlayer player, ArenaSide side) {
        if (side == null || Math.abs(player.getZ()) > ARENA_RADIUS - 7 || player.getY() < ARENA_PULLBACK_MIN_Y) {
            return false;
        }
        if (side == ArenaSide.LEFT) {
            return player.getX() >= -ARENA_RADIUS + 4 && player.getX() <= -4.0D;
        }
        return player.getX() <= ARENA_RADIUS - 4 && player.getX() >= 4.0D;
    }

    private static boolean isInsideCombatArena(ServerPlayer player) {
        return isInsideCombatArena((Entity) player);
    }

    private static boolean isInsideCombatArena(Entity entity) {
        double horizontalDistanceSqr = entity.getX() * entity.getX() + entity.getZ() * entity.getZ();
        double limit = ARENA_RADIUS - 1.0D;
        return horizontalDistanceSqr < limit * limit
                && entity.getY() >= ARENA_PULLBACK_MIN_Y
                && entity.getY() <= ARENA_PULLBACK_MAX_Y;
    }

    private static void movePlayerToArena(ServerPlayer player, ServerLevel arena, ArenaSpawn spawn) {
        player.teleportTo(arena, spawn.x, spawn.y, spawn.z, spawn.yRot, 0.0F);
    }

    private static void moveMobToSideSpawn(Mob mob, ArenaSide side) {
        double x = side == ArenaSide.LEFT ? -18.5D : 18.5D;
        mob.teleportTo(x, ARENA_Y, 0.5D);
        mob.setDeltaMovement(Vec3.ZERO);
        mob.fallDistance = 0.0F;
    }

    private static void restorePlayer(ServerPlayer player, ParticipantSnapshot snapshot) {
        ServerLevel level = player.server.getLevel(snapshot.level);
        if (level == null) {
            return;
        }
        player.teleportTo(level, snapshot.x, snapshot.y, snapshot.z, snapshot.yRot, snapshot.xRot);
        player.setGameMode(snapshot.gameType);
    }

    private static void broadcastStart(ArenaSession session, MinecraftServer server) {
        MutableComponent left = sideButton(ArenaSide.LEFT);
        MutableComponent right = sideButton(ArenaSide.RIGHT);
        for (UUID playerId : session.players) {
            ServerPlayer player = playerById(server, playerId);
            if (player == null) {
                continue;
            }
            Component leftName = session.isMobMode()
                    ? armyName(session.leftArmy)
                    : sideName(ArenaSide.LEFT);
            Component rightName = session.isMobMode()
                    ? armyName(session.rightArmy)
                    : sideName(ArenaSide.RIGHT);
            player.sendSystemMessage(Component.translatable("message.dealt_force_skills.gambler.arena.started",
                    leftName, rightName));
            if (session.canBet(playerId)) {
                player.sendSystemMessage(Component.translatable("message.dealt_force_skills.gambler.arena.bet_prompt", left, right));
            }
        }
    }

    private static void broadcastResult(ArenaSession session, MinecraftServer server, ArenaSide winner) {
        for (UUID playerId : session.players) {
            ServerPlayer player = playerById(server, playerId);
            if (player != null) {
                player.sendSystemMessage(Component.translatable("message.dealt_force_skills.gambler.arena.result", sideName(winner))
                        .withStyle(winner.format));
            }
        }
    }

    private static void assignMissingBets(ArenaSession session, MinecraftServer server) {
        Random random = new Random(session.id.getMostSignificantBits() ^ session.fightStartTick);
        for (UUID playerId : session.spectators) {
            if (session.bets.containsKey(playerId)) {
                continue;
            }
            ArenaSide side = random.nextBoolean() ? ArenaSide.LEFT : ArenaSide.RIGHT;
            session.bets.put(playerId, side);
            ServerPlayer player = playerById(server, playerId);
            if (player != null) {
                player.displayClientMessage(Component.translatable(
                        "message.dealt_force_skills.gambler.arena.bet_assigned", sideName(side)).withStyle(side.format), true);
            }
        }
    }

    private static void broadcastSupporters(ArenaSession session, MinecraftServer server) {
        Component left = supporterNames(session, server, ArenaSide.LEFT);
        Component right = supporterNames(session, server, ArenaSide.RIGHT);
        for (UUID playerId : session.players) {
            ServerPlayer player = playerById(server, playerId);
            if (player != null) {
                player.sendSystemMessage(Component.translatable("message.dealt_force_skills.gambler.arena.supporters",
                        sideName(ArenaSide.LEFT), left, sideName(ArenaSide.RIGHT), right));
            }
        }
    }

    private static Component supporterNames(ArenaSession session, MinecraftServer server, ArenaSide side) {
        StringJoiner joiner = new StringJoiner(", ");
        for (Map.Entry<UUID, ArenaSide> entry : session.bets.entrySet()) {
            if (entry.getValue() != side) {
                continue;
            }
            ServerPlayer player = playerById(server, entry.getKey());
            if (player != null) {
                joiner.add(player.getGameProfile().getName());
            }
        }
        String text = joiner.toString();
        return text.isEmpty()
                ? Component.translatable("message.dealt_force_skills.gambler.arena.supporters_empty")
                : Component.literal(text);
    }

    private static MutableComponent sideButton(ArenaSide side) {
        return Component.literal("[").append(sideName(side)).append("]")
                .withStyle(side.format)
                .withStyle(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                        "/gamblerarena bet " + side.id)));
    }

    private static Component sideName(ArenaSide side) {
        return Component.translatable("message.dealt_force_skills.gambler.arena.side." + side.id);
    }

    private static Component armyName(String army) {
        return switch (army) {
            case "undead", "arthropod", "illager", "atlantis", "coalition", "ancient", "nether", "ender" ->
                    Component.translatable("message.dealt_force_skills.gambler.army." + army);
            default -> Component.literal(army);
        };
    }

    private static void playArenaBgm(ArenaSession session, MinecraftServer server, long now) {
        for (UUID playerId : session.players) {
            ServerPlayer player = playerById(server, playerId);
            if (player != null) {
                RangedSoundHelper.playFollowingPlayer(player, ModSounds.GAMBLER_ARENA_BGM.get(),
                        SoundSource.PLAYERS, 0.78F, 1.0F, ARENA_BGM_RADIUS);
            }
        }
        session.arenaBgmPlaying = true;
        session.nextBgmTick = now + BGM_REPLAY_TICKS;
    }

    private static void stopArenaBgm(ServerPlayer player) {
        RangedSoundHelper.stop(player.serverLevel(), player.position(), ModSounds.GAMBLER_ARENA_BGM.get(),
                SoundSource.PLAYERS, ARENA_BGM_RADIUS + 16.0D);
    }

    private static List<ServerPlayer> selectedPlayers(ServerPlayer gambler, boolean alternate) {
        List<ServerPlayer> players = new ArrayList<>();
        players.add(gambler);
        if (alternate) {
            gambler.server.getPlayerList().getPlayers().stream()
                    .filter(player -> player != gambler
                            && player.serverLevel() == gambler.serverLevel()
                            && player.isAlive()
                            && !player.isSpectator()
                            && player.distanceToSqr(gambler) <= SELECTION_RANGE * SELECTION_RANGE)
                    .sorted(Comparator.comparingDouble(gambler::distanceToSqr))
                    .forEach(players::add);
        } else {
            ServerPlayer target = lookTarget(gambler).orElse(null);
            if (target != null) {
                players.add(target);
            }
        }
        return players;
    }

    private static Optional<ServerPlayer> lookTarget(ServerPlayer player) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        return player.serverLevel().getEntitiesOfClass(ServerPlayer.class,
                        player.getBoundingBox().inflate(SELECTION_RANGE),
                        target -> target != player && target.isAlive() && !target.isSpectator() && player.hasLineOfSight(target))
                .stream()
                .map(target -> new TargetCandidate(target, alignmentScore(eye, look, target)))
                .filter(candidate -> candidate.score >= 0.0D)
                .max(Comparator.comparingDouble(candidate -> candidate.score))
                .map(candidate -> candidate.player);
    }

    private static double alignmentScore(Vec3 eye, Vec3 look, LivingEntity target) {
        Vec3 center = target.position().add(0.0D, target.getBbHeight() * 0.55D, 0.0D);
        Vec3 toTarget = center.subtract(eye);
        double distance = toTarget.length();
        if (distance <= 0.001D || distance > SELECTION_RANGE) {
            return -1.0D;
        }
        double dot = look.dot(toTarget.scale(1.0D / distance));
        if (dot < 0.965D) {
            return -1.0D;
        }
        return dot * 1000.0D - distance;
    }

    private static String differentArmy(Random random, String first) {
        String second = randomArmyName(random);
        while (second.equals(first)) {
            second = randomArmyName(random);
        }
        return second;
    }

    private static String randomArmyName(Random random) {
        return GamblerArenaConfig.randomArmyId(random);
    }

    private static ArenaSession sessionForPlayer(ServerPlayer player) {
        UUID sessionId = PLAYER_TO_SESSION.get(player.getUUID());
        return sessionId == null ? null : SESSIONS.get(sessionId);
    }

    private static ArenaSession sessionForEntity(Entity entity) {
        if (entity == null) {
            return null;
        }
        if (entity instanceof ServerPlayer player) {
            return sessionForPlayer(player);
        }
        UUID sessionId = ENTITY_TO_SESSION.get(entity.getUUID());
        return sessionId == null ? null : SESSIONS.get(sessionId);
    }

    private static ArenaSide sideFor(ArenaSession session, Entity entity) {
        if (entity == null) {
            return null;
        }
        if (entity instanceof ServerPlayer player) {
            if (session.eliminatedPlayers.contains(player.getUUID())) {
                return null;
            }
            return session.teams.get(player.getUUID());
        }
        return session.mobTeams.get(entity.getUUID());
    }

    private static Entity rootAttacker(DamageSource source) {
        if (source == null) {
            return null;
        }
        Entity attacker = source.getEntity();
        return attacker != null ? attacker : source.getDirectEntity();
    }

    private static ServerPlayer firstOtherPlayer(ArenaSession session, MinecraftServer server, UUID excluded) {
        for (UUID playerId : session.players) {
            if (!playerId.equals(excluded)) {
                ServerPlayer player = playerById(server, playerId);
                if (player != null) {
                    return player;
                }
            }
        }
        return null;
    }

    private static ServerPlayer playerById(MinecraftServer server, UUID playerId) {
        return server.getPlayerList().getPlayer(playerId);
    }

    private static Entity entityById(MinecraftServer server, UUID entityId) {
        for (ServerLevel level : server.getAllLevels()) {
            Entity entity = level.getEntity(entityId);
            if (entity != null) {
                return entity;
            }
        }
        return null;
    }

    private enum ArenaMode {
        SOLO_MOB,
        DUEL_MOB,
        PVP_ODD,
        PVP_EVEN
    }

    private enum ArenaPhase {
        PREVIEW,
        FIGHTING,
        SETTLED
    }

    private enum ArenaSide {
        LEFT("left", ChatFormatting.RED),
        RIGHT("right", ChatFormatting.BLUE);

        private final String id;
        private final ChatFormatting format;

        ArenaSide(String id, ChatFormatting format) {
            this.id = id;
            this.format = format;
        }

        private static ArenaSide from(String id) {
            for (ArenaSide side : values()) {
                if (side.id.equalsIgnoreCase(id)) {
                    return side;
                }
            }
            return null;
        }
    }

    private static final class ArenaSession {
        private final UUID id;
        private final UUID gamblerId;
        private final long createdTick;
        private final boolean cooldownOnEnd;
        private final List<UUID> players;
        private final Map<UUID, ParticipantSnapshot> snapshots = new HashMap<>();
        private final Map<UUID, ArenaSide> bets = new HashMap<>();
        private final Map<UUID, ArenaSide> teams = new HashMap<>();
        private final Map<UUID, ArenaSide> mobTeams = new EnumMapBackedUuidMap();
        private final Map<UUID, Integer> mobPowers = new HashMap<>();
        private final Set<UUID> spectators = new HashSet<>();
        private final Set<UUID> eliminatedPlayers = new HashSet<>();
        private final Set<UUID> deadMobs = new HashSet<>();
        private ArenaMode mode = ArenaMode.SOLO_MOB;
        private ArenaPhase phase = ArenaPhase.PREVIEW;
        private long fightStartTick;
        private long nextBgmTick;
        private long lastTick = Long.MIN_VALUE;
        private int soloWinStreak;
        private boolean arenaBgmPlaying;
        private String leftArmy = "undead";
        private String rightArmy = "illager";

        private ArenaSession(UUID id, UUID gamblerId, long createdTick, List<ServerPlayer> players, boolean cooldownOnEnd) {
            this.id = id;
            this.gamblerId = gamblerId;
            this.createdTick = createdTick;
            this.cooldownOnEnd = cooldownOnEnd;
            this.players = players.stream().map(ServerPlayer::getUUID).toList();
        }

        private boolean isMobMode() {
            return mode == ArenaMode.SOLO_MOB || mode == ArenaMode.DUEL_MOB;
        }

        private boolean isSoloMobMode() {
            return mode == ArenaMode.SOLO_MOB;
        }

        private boolean isPvpMode() {
            return mode == ArenaMode.PVP_ODD || mode == ArenaMode.PVP_EVEN;
        }

        private boolean isPvpCombatant(UUID playerId) {
            return teams.containsKey(playerId) && !eliminatedPlayers.contains(playerId);
        }

        private boolean isObservationOnly(UUID playerId) {
            return spectators.contains(playerId) || eliminatedPlayers.contains(playerId);
        }

        private boolean canBet(UUID playerId) {
            return spectators.contains(playerId);
        }

        private ArenaSpawn spawnFor(UUID playerId) {
            if (isObservationOnly(playerId)) {
                return new ArenaSpawn(0.5D, SPECTATOR_FLOOR_Y + 1.0D, SPECTATOR_Z + 0.5D, 180.0F);
            }
            ArenaSide side = teams.getOrDefault(playerId, ArenaSide.LEFT);
            if (side == ArenaSide.LEFT) {
                return new ArenaSpawn(-18.5D, ARENA_Y, 0.5D, -90.0F);
            }
            return new ArenaSpawn(18.5D, ARENA_Y, 0.5D, 90.0F);
        }
    }

    private static final class EnumMapBackedUuidMap extends HashMap<UUID, ArenaSide> {
    }

    private record ParticipantSnapshot(
            ResourceKey<Level> level,
            double x,
            double y,
            double z,
            float yRot,
            float xRot,
            GameType gameType
    ) {
        private static ParticipantSnapshot capture(ServerPlayer player) {
            return new ParticipantSnapshot(player.level().dimension(),
                    player.getX(), player.getY(), player.getZ(),
                    player.getYRot(), player.getXRot(),
                    player.gameMode.getGameModeForPlayer());
        }
    }

    private record ArenaSpawn(double x, double y, double z, float yRot) {
    }

    private record TargetCandidate(ServerPlayer player, double score) {
    }

    private record ArmyRoster(List<ArmyUnit> basic, List<ArmyUnit> elite, List<ArmyUnit> mandatory,
                              int totalValue, int eliteValueCap, int maxMobs) {
    }

    private record ArmyUnit(EntityType<?> type, int power, int weight,
                            Map<String, GamblerArenaConfig.EquipmentSpec> equipment) {
    }

    private record DynamicArmyUnit(ArmyUnit unit, int value) {
    }

    private record DynamicArmyEntry(DynamicArmyUnit unit, int count) {
    }

    private record DynamicArmyPlan(List<DynamicArmyEntry> entries, int totalValue, int totalCount, int maxMobs) {
        private boolean isWithinValueRange(GamblerArenaConfig.DynamicArmySettings settings) {
            return totalCount <= maxMobs
                    && totalValue >= settings.minTotalValue()
                    && totalValue <= settings.maxTotalValue();
        }

        private int score(GamblerArenaConfig.DynamicArmySettings settings) {
            int distance;
            if (totalValue < settings.minTotalValue()) {
                distance = settings.minTotalValue() - totalValue;
            } else if (totalValue > settings.maxTotalValue()) {
                distance = totalValue - settings.maxTotalValue();
            } else {
                distance = 0;
            }
            return distance + Math.max(0, totalCount - maxMobs) * 1000;
        }
    }

    private record PendingVillagerConversion(UUID sessionId, ArenaSide side, int power,
                                             double x, double y, double z, long expiresTick) {
    }
}
