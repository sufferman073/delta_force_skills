package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.character.ghroth.GhrothStateManager;
import com.rzy.dealt_force_skills.character.lexninjia.LexNinjiaStateManager;
import com.rzy.dealt_force_skills.character.saeed.SaeedStateManager;
import com.rzy.dealt_force_skills.character.undead.UndeadStateManager;
import com.rzy.dealt_force_skills.character.undead.UndeadUpgradeManager;
import com.rzy.dealt_force_skills.shop.GhrothArmoryCatalog;
import com.rzy.dealt_force_skills.shop.HaffCoinManager;
import com.rzy.dealt_force_skills.shop.LexNinjiaCurrencyManager;
import com.rzy.dealt_force_skills.shop.UndeadSoulManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 客户端请求打开"自身短按特殊面板"（角色专属商店）。
 * 服务端根据当前角色打开对应的特殊商店，不依赖外部区域/潜行状态判断。
 */
public class C2S_OpenOwnShop implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<C2S_OpenOwnShop> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "c2s_open_own_shop"));

    public static final StreamCodec<FriendlyByteBuf, C2S_OpenOwnShop> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    C2S_OpenOwnShop::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(C2S_OpenOwnShop msg, FriendlyByteBuf buf) {
    }

    public static C2S_OpenOwnShop decode(FriendlyByteBuf buf) {
        return new C2S_OpenOwnShop();
    }

    public static C2S_OpenOwnShop open() {
        return new C2S_OpenOwnShop();
    }

    public static void handle(C2S_OpenOwnShop msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player == null || player.isSpectator()) {
                return;
            }
            if (UndeadStateManager.isUndead(player)) {
                NetworkHandler.sendToPlayer(new S2C_OpenUndeadShop(
                        UndeadSoulManager.get(player),
                        UndeadStateManager.profession(player).ordinal(),
                        UndeadUpgradeManager.snapshot(player)), player);
            } else if (LexNinjiaStateManager.isLexNinjia(player)) {
                LexNinjiaStateManager.initializeIfNeeded(player);
                NetworkHandler.sendToPlayer(new S2C_OpenLexNinjiaShop(
                        LexNinjiaCurrencyManager.get(player),
                        LexNinjiaStateManager.shopData(player)), player);
            } else if (SaeedStateManager.isSaeed(player)) {
                SaeedStateManager.initializeIfNeeded(player);
                NetworkHandler.sendToPlayer(new S2C_OpenSaeedRecruitScreen(
                        SaeedStateManager.needsInitialGuardChoice(player)
                                ? SaeedStateManager.initialGuardData(player)
                                : SaeedStateManager.shopData(player)), player);
            } else if (GhrothStateManager.isGhroth(player)) {
                NetworkHandler.sendToPlayer(new S2C_OpenGhrothArmory(
                        HaffCoinManager.get(player),
                        GhrothArmoryCatalog.entries()), player);
            }
        });
    }
}
