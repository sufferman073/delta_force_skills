package com.rzy.dealt_force_skills.raid.rules;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Instance-local admission; caller supplies requests in server acceptance order. */
public final class RaidSpawnRules {
    public static final int MAX_TEAM_SIZE = 3;
    private RaidSpawnRules() { }

    public enum SpawnKind { PLAYER, AI }
    public enum AdmissionStatus { ACCEPTED, FULL, ALREADY_ADMITTED, CLOSED }

    public record SpawnPoint(String id, SpawnKind kind) {
        public SpawnPoint {
            if (id == null || id.isBlank()) throw new IllegalArgumentException("Empty spawn ID");
            Objects.requireNonNull(kind);
        }
    }

    public record HumanTeam(String id, List<UUID> players) {
        public HumanTeam {
            if (id == null || id.isBlank()) throw new IllegalArgumentException("Empty team ID");
            players = List.copyOf(players);
            if (players.isEmpty() || players.size() > MAX_TEAM_SIZE
                    || new HashSet<>(players).size() != players.size()) {
                throw new IllegalArgumentException("A human team needs 1-3 distinct players");
            }
        }
    }

    public record HumanAssignment(HumanTeam team, SpawnPoint spawn) { }
    public record AiAssignment(SpawnPoint spawn, int members) { }
    public record Admission(AdmissionStatus status, HumanAssignment assignment) {
        public boolean accepted() { return status == AdmissionStatus.ACCEPTED; }
    }
    public record Plan(List<HumanAssignment> humans, List<AiAssignment> ai, int targetPopulation) {
        public Plan { humans = List.copyOf(humans); ai = List.copyOf(ai); }
        public int humanCount() { return humans.stream().mapToInt(a -> a.team().players().size()).sum(); }
        public int aiCount() { return ai.stream().mapToInt(AiAssignment::members).sum(); }
    }

    public static final class Lobby {
        private final int targetPopulation;
        private final LinkedHashMap<String, SpawnPoint> freeSpawns = new LinkedHashMap<>();
        private final List<HumanAssignment> humans = new ArrayList<>();
        private final Set<String> teamIds = new HashSet<>();
        private final Set<UUID> admittedPlayers = new HashSet<>();
        private int humanCount;
        private Plan finalPlan;

        public Lobby(int targetPopulation, List<SpawnPoint> spawns) {
            if (targetPopulation < 1) throw new IllegalArgumentException("Population must be positive");
            this.targetPopulation = targetPopulation;
            for (SpawnPoint spawn : spawns) {
                if (freeSpawns.putIfAbsent(spawn.id(), spawn) != null)
                    throw new IllegalArgumentException("Duplicate spawn: " + spawn.id());
            }
        }

        /** Rejection is atomic: neither capacity nor a spawn is reserved for a rejected team. */
        public synchronized Admission accept(HumanTeam team) {
            Objects.requireNonNull(team);
            if (finalPlan != null) return new Admission(AdmissionStatus.CLOSED, null);
            if (teamIds.contains(team.id()) || team.players().stream().anyMatch(admittedPlayers::contains))
                return new Admission(AdmissionStatus.ALREADY_ADMITTED, null);
            List<SpawnPoint> available = freeSpawns.values().stream().filter(p -> p.kind() == SpawnKind.PLAYER).toList();
            SpawnPoint spawn = available.isEmpty() ? null : available.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(available.size()));
            if (team.players().size() > targetPopulation - humanCount || spawn == null)
                return new Admission(AdmissionStatus.FULL, null);
            HumanAssignment assignment = new HumanAssignment(team, spawn);
            freeSpawns.remove(spawn.id());
            teamIds.add(team.id());
            admittedPlayers.addAll(team.players());
            humans.add(assignment);
            humanCount += team.players().size();
            return new Admission(AdmissionStatus.ACCEPTED, assignment);
        }

        /** Idempotently freezes human admission before filling independent AI teams. */
        public synchronized Plan finish() {
            if (finalPlan != null) return finalPlan;
            List<SpawnPoint> points = List.copyOf(freeSpawns.values());
            int count = (int) Math.min(targetPopulation - humanCount, (long) points.size() * MAX_TEAM_SIZE);
            List<AiAssignment> assignments = new ArrayList<>();
            if (!points.isEmpty()) {
                int perPoint = count / points.size();
                int remainder = count % points.size();
                for (int i = 0; i < points.size(); i++) {
                    int members = perPoint + (i < remainder ? 1 : 0);
                    if (members > 0) assignments.add(new AiAssignment(points.get(i), members));
                }
            }
            return finalPlan = new Plan(humans, assignments, targetPopulation);
        }
    }
}
