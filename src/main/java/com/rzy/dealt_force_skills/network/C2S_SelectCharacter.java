package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.CharacterAvailability;
import com.rzy.dealt_force_skills.character.CharacterBranchPackManager;
import com.rzy.dealt_force_skills.character.CharacterSelectionManager;
import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.character.stinger.StingerStateManager;
import com.rzy.dealt_force_skills.entity.RaptorFalconDroneEntity;
import com.rzy.dealt_force_skills.entity.UluruLoiteringMissileEntity;
import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.registry.ModItems;
import com.rzy.dealt_force_skills.shop.SaeedRecruitManager;
import com.rzy.dealt_force_skills.skill.SkillDispatcher;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record C2S_SelectCharacter(String characterId) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<C2S_SelectCharacter> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "c2s_select_character"));

    public static final StreamCodec<FriendlyByteBuf, C2S_SelectCharacter> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> buf.writeUtf(msg.characterId),
                    buf -> new C2S_SelectCharacter(buf.readUtf(256))
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(C2S_SelectCharacter msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player.isSpectator()) return;

            var currentCharacterId = CharacterSelectionManager.getSelectedCharacterId(player);
            if (player.hasEffect(ModEffects.STUN) || player.hasEffect(ModEffects.WEBBED) || StingerStateManager.isDowned(player)) {
                NetworkHandler.sendToPlayer(new S2C_SyncSelectedCharacter(currentCharacterId.orElse("")), player);
                player.displayClientMessage(Component.translatable("message.dealt_force_skills.stunned"), true);
                return;
            }
            if (UluruLoiteringMissileEntity.isPlayerControlling(player)
                    || RaptorFalconDroneEntity.isPlayerControlling(player)) {
                NetworkHandler.sendToPlayer(new S2C_SyncSelectedCharacter(currentCharacterId.orElse("")), player);
                return;
            }

            if (currentCharacterId.isPresent()) {
                // 已合并 DeltaForce 干员选择界面，允许自由替换角色（不再锁定非创造玩家）
                var requestedCharacter = CharacterBranchPackManager.findCharacter(msg.characterId);
                if (requestedCharacter.isEmpty()) {
                    NetworkHandler.sendToPlayer(new S2C_SyncSelectedCharacter(currentCharacterId.get()), player);
                    player.displayClientMessage(Component.translatable("message.dealt_force_skills.invalid_character"), false);
                    return;
                }
                var blockedMessageKey = CharacterAvailability.selectionBlockedMessageKey(player, requestedCharacter.get());
                if (blockedMessageKey.isPresent()) {
                    NetworkHandler.sendToPlayer(new S2C_SyncSelectedCharacter(currentCharacterId.get()), player);
                    player.displayClientMessage(Component.translatable(blockedMessageKey.get()), false);
                    return;
                }
                if (currentCharacterId.get().equals(msg.characterId)) {
                    NetworkHandler.sendToPlayer(new S2C_SyncSelectedCharacter(currentCharacterId.get()), player);
                    return;
                }
                boolean freeReselection = player.getAbilities().instabuild
                        || CharacterSelectionManager.hasCharacterReselection(player);
                ItemStack driftwood = freeReselection
                        ? ItemStack.EMPTY : findDriftwood(player);
                if (!freeReselection && driftwood.isEmpty()) {
                    NetworkHandler.sendToPlayer(new S2C_SyncSelectedCharacter(currentCharacterId.get()), player);
                    player.displayClientMessage(Component.translatable("message.dealt_force_skills.driftwood_missing"), false);
                    return;
                }

                CharacterSelectionManager.replaceCharacter(player, msg.characterId).ifPresentOrElse(
                        character -> {
                            if (!driftwood.isEmpty()) {
                                driftwood.shrink(1);
                            }
                            CharacterSelectionManager.consumeCharacterReselection(player);
                            SkillDispatcher.onCharacterSelected(player, character);
                            NetworkHandler.sendToPlayer(new S2C_SyncSelectedCharacter(character.id()), player);
                            player.displayClientMessage(Component.translatable("message.dealt_force_skills.character_selected",
                                    character.displayName()), true);
                            if (ModCharacters.SAEED_ID.equals(character.id())) {
                                SaeedRecruitManager.openInitialChoiceIfNeeded(player);
                            }
                        },
                        () -> {
                            NetworkHandler.sendToPlayer(new S2C_SyncSelectedCharacter(currentCharacterId.get()), player);
                            player.displayClientMessage(Component.translatable("message.dealt_force_skills.invalid_character"), false);
                        }
                );
                return;
            }

            var requestedCharacter = CharacterBranchPackManager.findCharacter(msg.characterId);
            if (requestedCharacter.isEmpty()) {
                NetworkHandler.sendToPlayer(new S2C_SyncSelectedCharacter(""), player);
                player.displayClientMessage(Component.translatable("message.dealt_force_skills.invalid_character"), false);
                return;
            }
            var blockedMessageKey = CharacterAvailability.selectionBlockedMessageKey(player, requestedCharacter.get());
            if (blockedMessageKey.isPresent()) {
                NetworkHandler.sendToPlayer(new S2C_SyncSelectedCharacter(""), player);
                player.displayClientMessage(Component.translatable(blockedMessageKey.get()), false);
                return;
            }
            CharacterSelectionManager.selectCharacter(player, msg.characterId).ifPresentOrElse(
                    character -> {
                        CharacterSelectionManager.consumeCharacterReselection(player);
                        SkillDispatcher.onCharacterSelected(player, character);
                        NetworkHandler.sendToPlayer(new S2C_SyncSelectedCharacter(character.id()), player);
                        player.displayClientMessage(Component.translatable("message.dealt_force_skills.character_selected",
                                character.displayName()), true);
                        if (ModCharacters.SAEED_ID.equals(character.id())) {
                            SaeedRecruitManager.openInitialChoiceIfNeeded(player);
                        }
                    },
                    () -> {
                        NetworkHandler.sendToPlayer(new S2C_SyncSelectedCharacter(""), player);
                        player.displayClientMessage(Component.translatable("message.dealt_force_skills.invalid_character"), false);
                    }
            );
        });
    }

    private static ItemStack findDriftwood(ServerPlayer player) {
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(ModItems.DRIFTWOOD.get()) && !stack.isEmpty()) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

}
