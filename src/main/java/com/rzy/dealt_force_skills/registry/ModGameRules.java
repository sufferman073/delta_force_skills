package com.rzy.dealt_force_skills.registry;

import com.rzy.dealt_force_skills.character.CharacterSkinSync;
import com.rzy.dealt_force_skills.config.DealtItemsHavocConfig;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.S2C_SyncModGameRules;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;

public final class ModGameRules {
    private static volatile boolean clientArmorSpecialsEnabled = true;
    private static volatile boolean clientRebuyEnabled = true;
    private static volatile boolean clientHavocValueVisible = true;
    public static final GameRules.Key<GameRules.BooleanValue> DEALT_UNDEAD_STEAL =
            GameRules.register("dealtundeadsteal", GameRules.Category.PLAYER, GameRules.BooleanValue.create(true));
    public static final GameRules.Key<GameRules.BooleanValue> DEALT_BOSSES_ENABLE =
            GameRules.register("dealtbossesenable", GameRules.Category.PLAYER, GameRules.BooleanValue.create(true));
    public static final GameRules.Key<GameRules.BooleanValue> DEALT_AGREEXP_UPGRADE =
            GameRules.register("dealtagreexpupgrade", GameRules.Category.PLAYER, GameRules.BooleanValue.create(true));
    public static final GameRules.Key<GameRules.BooleanValue> DEALT_UNDEAD_EXTRA_DROP =
            GameRules.register("dealtundeadextradrop", GameRules.Category.PLAYER, GameRules.BooleanValue.create(true));
    public static final GameRules.Key<GameRules.BooleanValue> DELTA_SKINS =
            GameRules.register("deltaskins", GameRules.Category.PLAYER,
                    GameRules.BooleanValue.create(true, (server, value) -> CharacterSkinSync.syncAll(server)));
    public static final GameRules.Key<GameRules.BooleanValue> DEALT_ARMOR_SPECIAL =
            GameRules.register("dealtarmorspecial", GameRules.Category.PLAYER,
                    GameRules.BooleanValue.create(true, (server, value) ->
                            NetworkHandler.sendToAll(syncPacket(server))));
    public static final GameRules.Key<GameRules.BooleanValue> DEALT_WOUND =
            GameRules.register("dealtwound", GameRules.Category.PLAYER,
                    GameRules.BooleanValue.create(false));
    public static final GameRules.Key<GameRules.BooleanValue> DEALT_LIMBS_DAMAGE =
            GameRules.register("dealtlimbsdamage", GameRules.Category.PLAYER,
                    GameRules.BooleanValue.create(true));
    public static final GameRules.Key<GameRules.BooleanValue> DEALT_GAMBLER =
            GameRules.register("dealtgambler", GameRules.Category.MOBS,
                    GameRules.BooleanValue.create(false));
    public static final GameRules.Key<GameRules.BooleanValue> DEALT_BREAKABLE =
            GameRules.register("dealtbreakable", GameRules.Category.PLAYER,
                    GameRules.BooleanValue.create(true));
    public static final GameRules.Key<GameRules.BooleanValue> DEALT_REBUY =
            GameRules.register("dealtrebuy", GameRules.Category.PLAYER,
                    GameRules.BooleanValue.create(true, (server, value) ->
                            NetworkHandler.sendToAll(syncPacket(server))));
    public static final GameRules.Key<GameRules.BooleanValue> DEALT_HAVOC_VISIBLE =
            GameRules.register("dealthavocvisible", GameRules.Category.PLAYER,
                    GameRules.BooleanValue.create(true, (server, value) ->
                            NetworkHandler.sendToAll(syncPacket(server))));
    public static final GameRules.Key<GameRules.BooleanValue> DEALT_CHEST_DROPABLE =
            GameRules.register("dealtchestdropable", GameRules.Category.PLAYER,
                    GameRules.BooleanValue.create(false));

    private ModGameRules() {
    }

    private static S2C_SyncModGameRules syncPacket(net.minecraft.server.MinecraftServer server) {
        return new S2C_SyncModGameRules(
                server.getGameRules().getBoolean(DEALT_ARMOR_SPECIAL),
                server.getGameRules().getBoolean(DEALT_REBUY),
                server.getGameRules().getBoolean(DEALT_HAVOC_VISIBLE),
                DealtItemsHavocConfig.priceSnapshot(),
                DealtItemsHavocConfig.variantPriceSnapshot(server));
    }

    public static void register() {
        // Forces static initialization so vanilla /gamerule exposes both mod rules.
    }

    public static boolean isExperienceUpgradeEnabled(Player player) {
        return player == null || player.level().getGameRules().getBoolean(DEALT_AGREEXP_UPGRADE);
    }

    public static int effectiveExperienceLevel(Player player) {
        if (player == null) {
            return 0;
        }
        return isExperienceUpgradeEnabled(player) ? Math.max(0, player.experienceLevel) : 0;
    }

    public static boolean areCharacterSkinsEnabled(Player player) {
        return player == null || player.level().getGameRules().getBoolean(DELTA_SKINS);
    }

    public static boolean isNoxInvisibilityEnabled(Player player) {
        return true;
    }

    public static boolean areArmorSpecialsEnabled(Player player) {
        if (player == null) {
            return true;
        }
        return player.level().isClientSide
                ? clientArmorSpecialsEnabled
                : player.level().getGameRules().getBoolean(DEALT_ARMOR_SPECIAL);
    }

    public static void syncToClient(ServerPlayer player) {
        NetworkHandler.sendToPlayer(syncPacket(player.getServer()), player);
    }

    public static void syncToAll(net.minecraft.server.MinecraftServer server) {
        NetworkHandler.sendToAll(syncPacket(server));
    }

    public static void acceptClientSync(boolean armorSpecialsEnabled, boolean rebuyEnabled,
                                        boolean havocValueVisible) {
        clientArmorSpecialsEnabled = armorSpecialsEnabled;
        clientRebuyEnabled = rebuyEnabled;
        clientHavocValueVisible = havocValueVisible;
    }

    public static boolean areWoundsEnabled(Player player) {
        return player != null && player.level().getGameRules().getBoolean(DEALT_WOUND);
    }

    public static boolean areLimbsDamageReductionsEnabled(Player player) {
        return player == null || player.level().getGameRules().getBoolean(DEALT_LIMBS_DAMAGE);
    }

    public static boolean areSkillBlockBreaksEnabled(Level level) {
        return level == null || level.getGameRules().getBoolean(DEALT_BREAKABLE);
    }

    public static boolean isRebuyEnabled(Level level) {
        return level == null || (level.isClientSide
                ? clientRebuyEnabled
                : level.getGameRules().getBoolean(DEALT_REBUY));
    }

    public static boolean isHavocValueVisible(Level level) {
        return level == null || (level.isClientSide
                ? clientHavocValueVisible
                : level.getGameRules().getBoolean(DEALT_HAVOC_VISIBLE));
    }
}
