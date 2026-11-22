package com.rzy.dealt_force_skills.raid;

import com.rzy.dealt_force_skills.character.catdad.CatDadStateManager;
import com.rzy.dealt_force_skills.character.corps.CorpsStateManager;
import com.rzy.dealt_force_skills.character.manba.ManbaStateManager;
import com.rzy.dealt_force_skills.character.rover.RoverStateManager;
import com.rzy.dealt_force_skills.character.stinger.StingerStateManager;
import com.rzy.dealt_force_skills.character.tempest.TempestStateManager;
import com.rzy.dealt_force_skills.character.vlinder.VlinderStateManager;
import com.rzy.dealt_force_skills.compat.PlayerReviveCompat;
import com.rzy.dealt_force_skills.raid.ai.RaidAiAffinity;
import com.rzy.dealt_force_skills.raid.assets.RaidAssetService;
import com.rzy.dealt_force_skills.registry.ModEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static com.rzy.dealt_force_skills.raid.RaidMatchLedger.*;

/** Participant-to-actor ownership, disconnect bodies and the last-priority generic revive fallback. */
public final class RaidActors {
    private static final TicketType<UUID> BODY_TICKET = TicketType.create("dfs_raid_offline_body", Comparator.comparing(UUID::toString));
    private static final String STINGER_DATA = "dealt_force_skills.stinger";
    private RaidActors() { }

    public static LivingEntity entity(MinecraftServer server, Participant p) {
        ServerLevel level = server.getLevel(RaidInstances.DIMENSION);
        if (p.body != null && level != null) {
            Entity found = level.getEntity(p.body);
            if (found instanceof RaidOfflineBodyEntity body && p.id.equals(body.playerId())) return body;
            return null; // PlayerList still contains a disconnecting/reconnecting player during transfer.
        }
        return server.getPlayerList().getPlayer(p.id);
    }
    public static boolean loading(Participant p) { return p.bodyState.getBoolean("LoadingBody"); }
    public static boolean loading(MinecraftServer server, Participant p) { return loading(p); }
    public static void disconnect(ServerPlayer player, Match match, Participant p) {
        if (!p.eligible()) return;
        if (p.body != null) {
            // A failed/incomplete reconnect never transfers ownership back to the stale player object.
            LivingEntity actor = entity(player.server, p);
            if (actor == null) { ensureBody(player.server, match, p); actor = entity(player.server, p); }
            if (actor instanceof RaidOfflineBodyEntity body) {
                p.actual = body.actual(); p.bodyState = body.bodyState();
                p.position = body.position(); p.yaw = body.getYRot(); p.pitch = body.getXRot();
                RaidAssetService.checkpoint(player.server, p.id, p.actual);
            }
            RaidMatchLedger.save(player.server, match);
            return;
        }
        p.actual = RaidAssetService.captureActual(player); p.position = player.position(); p.yaw = player.getYRot(); p.pitch = player.getXRot();
        CompoundTag state = captureLife(player);
        state.putInt("Selected", player.getInventory().selected);
        CompoundTag food = new CompoundTag(); player.getFoodData().addAdditionalSaveData(food); state.put("Food", food);
        String kind = downKind(player); state.putString("DownKind", kind);
        int remaining = downRemaining(player);
        if (!kind.isEmpty()) {
            p.downedUntil = now(player.server) + Math.max(1, remaining);
            state.putBoolean("GenericDowned", true);
        } else p.downedUntil = 0;
        p.bodyState = state;
        if (p.body == null) p.body = UUID.randomUUID();
        RaidMatchLedger.save(player.server, match); // Body identity exists durably before entity creation.
        ensureBody(player.server, match, p);
    }
    public static void ensureBody(MinecraftServer server, Match match, Participant p) {
        if (!p.eligible()) return;
        if (p.body == null && server.getPlayerList().getPlayer(p.id) != null) return;
        ServerLevel level = server.getLevel(RaidInstances.DIMENSION);
        if (level == null || p.bodyState.isEmpty() || p.actual.isEmpty()) throw new IllegalStateException("Missing offline participant snapshot");
        keepOfflineActorsTicking(level, match, p);
        if (p.body == null) { p.body = UUID.randomUUID(); RaidMatchLedger.save(server, match); }
        ChunkPos chunk = new ChunkPos(BlockPos.containing(p.position));
        level.getChunkSource().addRegionTicket(BODY_TICKET, chunk, 2, p.id);
        level.getChunk(chunk.x, chunk.z);
        if (!level.areEntitiesLoaded(chunk.toLong())) {
            if (!loading(p)) { p.bodyState.putBoolean("LoadingBody", true); p.bodyState.putLong("LoadStarted", now(server)); }
            if (now(server) - p.bodyState.getLong("LoadStarted") > 200) throw new IllegalStateException("Offline body entity chunk did not finish loading");
            return;
        }
        p.bodyState.remove("LoadingBody"); p.bodyState.remove("LoadStarted");
        Entity existing = level.getEntity(p.body);
        if (existing != null) {
            if (!(existing instanceof RaidOfflineBodyEntity body) || !p.id.equals(body.playerId()) || !match.id.equals(body.instanceId()))
                throw new IllegalStateException("Offline body identity collision");
            return;
        }
        RaidOfflineBodyEntity body = RaidActorTypes.OFFLINE_BODY.create(level);
        if (body == null) throw new IllegalStateException("Cannot create offline raid body");
        body.setUUID(p.body); body.initialize(match.id, p);
        var instance = RaidInstanceData.get(server).instance(match.id);
        if (instance == null) throw new IllegalStateException("Offline body has no live instance");
        RaidAiAffinity.bindParticipant(body, match.id, p.team, instance.origin(), instance.sizeX, instance.sizeY, instance.sizeZ);
        if (!level.addFreshEntity(body)) throw new IllegalStateException("Offline raid body insertion failed");
        RaidMatchLedger.save(server, match);
    }
    public static void reconnect(ServerPlayer player, Match match, Participant p) {
        if (p.body != null) {
            ensureBody(player.server, match, p);
            LivingEntity actor = entity(player.server, p);
            if (!(actor instanceof RaidOfflineBodyEntity body)) throw new IllegalStateException("Offline body is still loading; reconnect recovery must retry");
            p.actual = body.actual(); p.bodyState = body.bodyState(); p.position = body.position(); p.yaw = body.getYRot(); p.pitch = body.getXRot();
            RaidMatchLedger.save(player.server, match);
        }
        if (!RaidAssetService.resumeActual(player, match.id, p.actual)) throw new IllegalStateException("Offline asset ownership could not resume");
        if (!RaidInstances.transfer(player, player.server.getLevel(RaidInstances.DIMENSION), p.position, p.yaw, p.pitch))
            throw new IllegalStateException("Offline participant return teleport rejected");
        if (!p.bodyState.isEmpty()) {
            restoreLife(player, p.bodyState);
            player.getFoodData().readAdditionalSaveData(p.bodyState.getCompound("Food"));
            player.getInventory().selected = Math.max(0, Math.min(8, p.bodyState.getInt("Selected")));
        }
        if (p.downedUntil > 0) {
            int remaining = (int) Math.max(1, p.downedUntil - now(player.server));
            String kind = p.bodyState.getString("DownKind");
            if (kind.equals("playerrevive") && PlayerReviveCompat.isLoaded()) {
                RaidReviveBridge.start(player, player.damageSources().generic());
                p.bodyState.putLong("ResumeBleedUntil", p.downedUntil);
                p.bodyState.putInt("LastNativeRemaining", PlayerReviveCompat.downedRemainingTicks(player));
            }
            else if (kind.equals("stinger") || kind.equals("vlinder") || kind.equals("catdad") || kind.equals("tempest") || kind.equals("rover")) {
                player.getPersistentData().getCompound("dealt_force_skills." + kind).putLong("DownedUntil", p.downedUntil);
            } else {
                StingerStateManager.enterRaidDowned(player, remaining); p.bodyState.putString("DownKind", "generic");
            }
        } else clearDowned(player);
        player.server.getPlayerList().saveAll();
        removeBody(player.server, p);
        RaidMatchLedger.save(player.server, match);
    }
    public static CompoundTag actual(MinecraftServer server, Participant p) {
        LivingEntity actor = entity(server, p);
        if (actor instanceof ServerPlayer player) return RaidAssetService.captureActual(player);
        if (actor instanceof RaidOfflineBodyEntity body) { p.bodyState = body.bodyState(); return body.actual(); }
        return p.actual.isEmpty() ? null : p.actual.copy();
    }
    public static void removeBody(MinecraftServer server, Participant p) {
        ServerLevel level = server.getLevel(RaidInstances.DIMENSION);
        if (level != null && p.body != null) {
            Entity body = level.getEntity(p.body);
            if (body instanceof RaidOfflineBodyEntity offline && p.id.equals(offline.playerId())) body.discard();
            level.getChunkSource().removeRegionTicket(BODY_TICKET, new ChunkPos(BlockPos.containing(p.position)), 2, p.id);
        }
        p.body = null; p.bodyState.remove("LoadingBody");
    }

    public static boolean isGenericDowned(net.minecraft.world.entity.player.Player player) {
        return player.getPersistentData().getCompound(STINGER_DATA).getBoolean("RaidGenericDowned");
    }
    public static boolean defersDeath(LivingEntity entity) {
        return entity instanceof RaidOfflineBodyEntity
                || entity instanceof ServerPlayer player && ownsDeath(player);
    }
    private static boolean ownsDeath(ServerPlayer player) {
        Participant participant = RaidMatches.participant(player);
        return participant != null && participant.hasAssets();
    }
    public static boolean staleOrBodyDeath(LivingEntity entity) {
        if (entity instanceof RaidOfflineBodyEntity) return true;
        if (!(entity instanceof ServerPlayer player)) return false;
        Participant p = RaidMatches.participant(player);
        return p != null && p.hasAssets() && (p.body != null || !p.eligible());
    }
    public static void recordFinalDeathSource(Participant p, DamageSource source) {
        if (source == null || p.bodyState.contains("FinalDeathSource")) return;
        p.bodyState.putString("FinalDeathSource", source.typeHolder().unwrapKey().map(key -> key.location().toString()).orElse(source.getMsgId()));
        if (source.getEntity() != null) p.bodyState.putUUID("FinalDeathAttacker", source.getEntity().getUUID());
    }
    /** Claim once before legacy rewards/cleanup; terminal asset restoration must run afterwards. */
    public static void confirmFinalDeath(MinecraftServer server, Match match, Participant p, DamageSource source) {
        if (p.status != Status.DEAD || p.bodyState.getBoolean("FinalDeathEffectsClaimed")) return;
        LivingEntity actor = entity(server, p);
        if (source == null && actor != null) {
            source = actor.damageSources().genericKill();
            var id = net.minecraft.resources.ResourceLocation.tryParse(p.bodyState.getString("FinalDeathSource"));
            if (id != null) {
                var key = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE, id);
                var type = actor.level().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE).getHolder(key);
                Entity attacker = p.bodyState.hasUUID("FinalDeathAttacker") && actor.level() instanceof ServerLevel level
                        ? level.getEntity(p.bodyState.getUUID("FinalDeathAttacker")) : null;
                if (type.isPresent()) source = new DamageSource(type.get(), attacker);
            }
        }
        recordFinalDeathSource(p, source);
        p.bodyState.putBoolean("FinalDeathEffectsClaimed", true);
        try { RaidMatchLedger.save(server, match); }
        catch (RuntimeException failure) { p.bodyState.remove("FinalDeathEffectsClaimed"); throw failure; }
        if (actor instanceof RaidOfflineBodyEntity body) body.remember(p.bodyState);
        if (actor != null && !p.assetApplied) com.rzy.dealt_force_skills.event.CommonEvents.confirmRaidDeath(actor, source);
    }

    /** Invoke only after the project's native fatal defenses and non-cancelled native PR handling. */
    public static boolean tryFallbackDown(ServerPlayer player, Participant p, DamageSource source) {
        if (!p.eligible() || finalDamage(player, source)) return false;
        String nativeKind = downKind(player);
        if (!nativeKind.isEmpty()) return false; // A second lethal hit finishes an already-downed actor.
        if (p.downedUntil > 0) return false;
        RaidReviveBridge.Settings settings = RaidReviveBridge.settings();
        if (PlayerReviveCompat.isLoaded()) {
            RaidReviveBridge.start(player, source);
            p.bodyState.putString("DownKind", "playerrevive");
            p.downedUntil = now(player.server) + Math.max(1, PlayerReviveCompat.downedRemainingTicks(player));
        } else {
            StingerStateManager.enterRaidDowned(player, settings.bleedTicks());
            player.setHealth(Math.min(player.getMaxHealth(), settings.bleedingHealth()));
            p.bodyState.putString("DownKind", "generic"); p.bodyState.putBoolean("GenericDowned", true);
            p.downedUntil = now(player.server) + settings.bleedTicks();
        }
        p.bodyState.putFloat("ReviveProgress", 0);
        Match match = RaidMatches.match(player); if (match != null) RaidMatchLedger.save(player.server, match);
        return true;
    }
    public static boolean tryBodyDown(RaidOfflineBodyEntity body, DamageSource source) {
        if (!(body.level() instanceof ServerLevel level)) return false;
        Match match = RaidMatches.match(level.getServer(), body.instanceId());
        Participant p = match == null ? null : match.players.get(body.playerId());
        if (p == null || !p.eligible() || p.downedUntil > 0 || finalDamage(body, source)) return false;
        var settings = RaidReviveBridge.settings();
        p.downedUntil = now(level.getServer()) + settings.bleedTicks();
        p.bodyState = body.bodyState(); p.bodyState.putString("DownKind", "generic");
        p.bodyState.putBoolean("GenericDowned", true); p.bodyState.putFloat("ReviveProgress", 0);
        body.setHealth(Math.min(body.getMaxHealth(), settings.bleedingHealth())); body.clearFire();
        body.addEffect(new MobEffectInstance(ModEffects.STINGER_DOWNED.get(), settings.bleedTicks(), 0, false, true, true));
        body.remember(p.bodyState); RaidMatchLedger.save(level.getServer(), match); return true;
    }
    private static boolean finalDamage(LivingEntity actor, DamageSource source) {
        return source == null || source.is(DamageTypes.GENERIC_KILL) || source.is(DamageTypes.FELL_OUT_OF_WORLD)
                || source.getMsgId().contains("bled_to_death") || RoverStateManager.executingDeath(actor)
                || CorpsStateManager.isExecutingLoyaltyTarget(actor) || ManbaStateManager.isExecutingDuelTarget(actor)
                || actor instanceof ServerPlayer player && (StingerStateManager.isExecutingDownedDeath(player) || VlinderStateManager.isExecutingDownedDeath(player));
    }
    public static boolean help(ServerPlayer helper, LivingEntity target) {
        if (!RaidMatches.participating(helper) || !RaidMatches.sameTeam(helper, target)) return false;
        helper.displayClientMessage(Component.literal("靠近倒地队友并持续潜行即可救援。"), true); return true;
    }
    public static void tick(MinecraftServer server, Match match, Participant p) {
        LivingEntity actor = entity(server, p); if (actor == null) return;
        if (actor instanceof RaidOfflineBodyEntity && actor.level() instanceof ServerLevel level) keepOfflineActorsTicking(level, match, p);
        if (actor instanceof ServerPlayer player) {
            if (p.downedUntil > 0 && p.bodyState.getString("DownKind").equals("generic") && now(server) >= p.downedUntil) {
                RaidMatches.terminal(server, match, p, Status.DEAD, true); return;
            }
            String nativeKind = downKind(player);
            if (!nativeKind.isEmpty() && !nativeKind.equals("generic")) {
                p.bodyState.putString("DownKind", nativeKind); p.downedUntil = now(server) + Math.max(1, downRemaining(player));
                if (nativeKind.equals("playerrevive") && p.bodyState.contains("ResumeBleedUntil")) {
                    int remaining = PlayerReviveCompat.downedRemainingTicks(player), previous = p.bodyState.getInt("LastNativeRemaining");
                    long cap = p.bodyState.getLong("ResumeBleedUntil");
                    if (remaining >= previous) cap += 1L + remaining - previous;
                    p.bodyState.putInt("LastNativeRemaining", remaining); p.bodyState.putLong("ResumeBleedUntil", cap);
                    p.downedUntil = Math.min(p.downedUntil, cap);
                    if (now(server) >= p.downedUntil) { RaidMatches.terminal(server, match, p, Status.DEAD, true); return; }
                }
                // PlayerRevive and character-specific rescues retain their existing timing/bonuses.
                return;
            }
            if (p.downedUntil > 0 && nativeKind.isEmpty()) { clearDownRecord(p); return; }
            if (nativeKind.equals("generic")) StingerStateManager.tickDowned(player);
        }
        if (p.downedUntil <= 0 || !p.eligible()) return;
        var settings = RaidReviveBridge.settings();
        if (now(server) >= p.downedUntil) { RaidMatches.terminal(server, match, p, Status.DEAD, true); return; }
        List<ServerPlayer> helpers = new ArrayList<>();
        for (Participant teammate : match.players.values()) {
            if (teammate == p || !teammate.eligible() || !teammate.team.equals(p.team)) continue;
            ServerPlayer helper = server.getPlayerList().getPlayer(teammate.id);
            if (helper != null && helper.isAlive() && !helper.isSpectator() && helper.isShiftKeyDown()
                    && helper.level() == actor.level() && downKind(helper).isEmpty()
                    && helper.distanceToSqr(actor) <= settings.distance() * settings.distance() && helper.hasLineOfSight(actor)) helpers.add(helper);
        }
        if (helpers.isEmpty()) p.bodyState.putFloat("ReviveProgress", 0);
        else {
            float speed = 0;
            for (ServerPlayer helper : helpers) {
                float contribution = settings.progressPerPlayer();
                if (StingerStateManager.isStinger(helper) && StingerStateManager.rescueCooldownReady(helper))
                    contribution = Math.max(contribution, (float) settings.progress() / Math.max(1, StingerStateManager.REVIVE_OTHER_TICKS));
                speed += contribution;
            }
            float progress = p.bodyState.getFloat("ReviveProgress") + speed; p.bodyState.putFloat("ReviveProgress", progress);
            if (settings.pauseWhileHelped()) {
                p.downedUntil++;
                if (actor instanceof ServerPlayer player) player.getPersistentData().getCompound(STINGER_DATA).putLong("DownedUntil", p.downedUntil);
            }
            for (ServerPlayer helper : helpers) helper.displayClientMessage(Component.literal("救援 " + p.name + "：" + Math.min(100, (int) (100 * progress / settings.progress())) + "%"), true);
            if (progress >= settings.progress()) {
                if (actor instanceof ServerPlayer player) StingerStateManager.clearDowned(player, false);
                actor.removeEffect(ModEffects.STINGER_DOWNED.get()); actor.removeEffect(ModEffects.VLINDER_VITAL_DOWNED.get());
                actor.removeEffect(ModEffects.CATDAD_DOWNED.get()); actor.removeEffect(ModEffects.ROVER_DOWNED.get());
                actor.removeEffect(ModEffects.TEMPEST_EMERGENCY_DOWNED.get());
                actor.setHealth(Math.min(actor.getMaxHealth(), settings.restoredHealth()));
                clearDownRecord(p);
                for (ServerPlayer helper : helpers) if (StingerStateManager.isStinger(helper)) StingerStateManager.markRescueCooldown(helper);
                RaidMatchLedger.save(server, match);
            }
        }
        if (actor instanceof RaidOfflineBodyEntity body) body.remember(p.bodyState);
    }
    private static void keepOfflineActorsTicking(ServerLevel level, Match match, Participant p) {
        if (match.phase != Phase.ACTIVE || !p.eligible() || !level.dimension().equals(RaidInstances.DIMENSION)) return;
        var instance = com.rzy.dealt_force_skills.raid.RaidInstanceData.get(level.getServer()).instance(match.id);
        if (instance != null && instance.phase == com.rzy.dealt_force_skills.raid.RaidInstanceData.Phase.READY && instance.error.isEmpty())
            level.resetEmptyTime(); // No persistent forced chunk; removeBody/ending stops this naturally.
    }
    private static void clearDownRecord(Participant p) {
        p.downedUntil = 0;
        for (String key : new String[]{"DownKind", "GenericDowned", "ReviveProgress", "ResumeBleedUntil", "LastNativeRemaining"}) p.bodyState.remove(key);
    }
    private static String downKind(ServerPlayer player) {
        if (PlayerReviveCompat.isBleeding(player)) return "playerrevive";
        if (RoverStateManager.isDowned(player)) return "rover";
        if (StingerStateManager.isDowned(player)) return player.getPersistentData().getCompound(STINGER_DATA).getBoolean("RaidGenericDowned") ? "generic" : "stinger";
        if (VlinderStateManager.isDowned(player)) return "vlinder";
        if (CatDadStateManager.isDowned(player)) return "catdad";
        if (TempestStateManager.isDowned(player)) return "tempest";
        return "";
    }
    private static int downRemaining(ServerPlayer player) {
        if (PlayerReviveCompat.isBleeding(player)) return PlayerReviveCompat.downedRemainingTicks(player);
        if (StingerStateManager.isDowned(player)) return StingerStateManager.downedRemainingTicks(player);
        if (VlinderStateManager.isDowned(player)) return VlinderStateManager.downedRemainingTicks(player);
        if (CatDadStateManager.isDowned(player)) return CatDadStateManager.downedRemainingTicks(player);
        String kind = downKind(player);
        long until = player.getPersistentData().getCompound("dealt_force_skills." + kind).getLong("DownedUntil");
        return (int) Math.max(1, Math.min(Integer.MAX_VALUE, until - now(player.server)));
    }
    public static void clearDowned(ServerPlayer player) {
        if (PlayerReviveCompat.isBleeding(player)) PlayerReviveCompat.revive(player, false);
        StingerStateManager.clearDownedOnDeath(player); VlinderStateManager.clearDownedOnDeath(player);
        if (RoverStateManager.isDowned(player)) RoverStateManager.clearDowned(player, false);
        player.getPersistentData().getCompound("dealt_force_skills.catdad").putLong("DownedUntil", 0);
        player.getPersistentData().getCompound("dealt_force_skills.tempest").putLong("DownedUntil", 0);
        player.removeEffect(ModEffects.CATDAD_DOWNED.get()); player.removeEffect(ModEffects.TEMPEST_EMERGENCY_DOWNED.get());
        PlayerReviveCompat.clearTempestSelfRescue(player);
    }
    public static CompoundTag captureOriginal(ServerPlayer player) {
        CompoundTag tag = captureLife(player), food = new CompoundTag();
        player.getFoodData().addAdditionalSaveData(food); tag.put("Food", food); return tag;
    }
    public static void restoreOriginal(ServerPlayer player, CompoundTag original) {
        if (original.isEmpty()) return;
        clearDowned(player); restoreLife(player, original);
        player.getFoodData().readAdditionalSaveData(original.getCompound("Food"));
    }
    public static void prepareEntry(ServerPlayer player) {
        clearDowned(player); player.removeAllEffects(); player.clearFire(); player.setAbsorptionAmount(0);
        player.setHealth(player.getMaxHealth()); player.getFoodData().setFoodLevel(20); player.getFoodData().setSaturation(5);
    }
    static CompoundTag captureLife(LivingEntity actor) {
        CompoundTag result = new CompoundTag();
        result.put("Attributes", actor.getAttributes().save());
        result.put("BodyAttributes", bodyAttributes(actor)); result.putFloat("Health", actor.getHealth());
        result.putFloat("Absorption", actor.getAbsorptionAmount()); result.putInt("Fire", actor.getRemainingFireTicks()); result.putInt("Air", actor.getAirSupply());
        ListTag effects = new ListTag(); for (MobEffectInstance effect : actor.getActiveEffects()) effects.add(effect.save(new CompoundTag()));
        result.put("Effects", effects); return result;
    }
    static void restoreLife(LivingEntity actor, CompoundTag snapshot) {
        actor.removeAllEffects();
        String attributes = actor instanceof RaidOfflineBodyEntity && snapshot.contains("BodyAttributes") ? "BodyAttributes" : "Attributes";
        actor.getAttributes().load(snapshot.getList(attributes, Tag.TAG_COMPOUND));
        for (Tag tag : snapshot.getList("Effects", Tag.TAG_COMPOUND)) {
            MobEffectInstance effect = MobEffectInstance.load((CompoundTag) tag); if (effect != null) actor.addEffect(effect);
        }
        actor.setHealth(Math.max(1, Math.min(actor.getMaxHealth(), snapshot.getFloat("Health"))));
        actor.setAbsorptionAmount(snapshot.getFloat("Absorption")); actor.setRemainingFireTicks(snapshot.getInt("Fire")); actor.setAirSupply(snapshot.getInt("Air"));
    }
    private static ListTag bodyAttributes(LivingEntity actor) {
        ListTag attributes = actor.getAttributes().save();
        for (Tag raw : attributes) {
            CompoundTag saved = (CompoundTag) raw;
            var id = net.minecraft.resources.ResourceLocation.tryParse(saved.getString("Name"));
            if (id == null) continue;
            var type = net.minecraft.core.registries.BuiltInRegistries.ATTRIBUTE.get(id);
            if (type == null) continue;
            var instance = actor.getAttribute(type);
            if (instance == null) continue;
            ListTag modifiers = new ListTag();
            for (var modifier : instance.getModifiers()) modifiers.add(modifier.save());
            saved.put("Modifiers", modifiers);
        }
        return attributes;
    }
    private static long now(MinecraftServer server) { return server.overworld().getGameTime(); }
}
