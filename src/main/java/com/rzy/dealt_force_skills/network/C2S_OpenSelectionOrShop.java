package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.character.CharacterAvailability;
import com.rzy.dealt_force_skills.character.CharacterSelectionManager;
import com.rzy.dealt_force_skills.character.saeed.SaeedStateManager;
import com.rzy.dealt_force_skills.shop.GhrothArmoryManager;
import com.rzy.dealt_force_skills.shop.HaffShopManager;
import com.rzy.dealt_force_skills.shop.LexNinjiaShopManager;
import com.rzy.dealt_force_skills.shop.SaeedRecruitManager;
import com.rzy.dealt_force_skills.shop.UndeadShopManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class C2S_OpenSelectionOrShop implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<C2S_OpenSelectionOrShop> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "c2s_open_selection_or_shop"));

    public static final StreamCodec<FriendlyByteBuf, C2S_OpenSelectionOrShop> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    C2S_OpenSelectionOrShop::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(C2S_OpenSelectionOrShop msg, FriendlyByteBuf buf) {
    }

    public static C2S_OpenSelectionOrShop decode(FriendlyByteBuf buf) {
        return new C2S_OpenSelectionOrShop();
    }

    public static void handle(C2S_OpenSelectionOrShop msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player == null || player.isSpectator()) {
                return;
            }
            if (!player.isCreative() && player.isShiftKeyDown()
                    && com.rzy.dealt_force_skills.character.commander.CommanderStateManager.isCommander(player)) {
                if (HaffShopManager.shouldOpenShop(player, true)) {
                    HaffShopManager.openShop(player, true);
                }
                return;
            }
            if (CharacterSelectionManager.hasCharacterReselection(player)) {
                CharacterAvailability.syncToClient(player);
                NetworkHandler.sendToPlayer(new S2C_OpenCharacterSelection(true), player);
            } else if (player.isShiftKeyDown() && HaffShopManager.shouldOpenShop(player, true)) {
                HaffShopManager.openShop(player, true);
            } else if (LexNinjiaShopManager.shouldOpen(player)) {
                LexNinjiaShopManager.open(player);
            } else if (UndeadShopManager.shouldOpen(player)) {
                UndeadShopManager.open(player);
            } else if (SaeedRecruitManager.shouldOpen(player)) {
                SaeedRecruitManager.open(player);
            } else if (GhrothArmoryManager.shouldOpen(player)) {
                GhrothArmoryManager.open(player);
            } else if (HaffShopManager.shouldOpenShop(player)) {
                HaffShopManager.openShop(player);
            } else if (CharacterSelectionManager.isNormalPlayer(player)) {
                return;
            } else {
                CharacterAvailability.syncToClient(player);
                NetworkHandler.sendToPlayer(new S2C_OpenCharacterSelection(), player);
            }
        });
    }
}
