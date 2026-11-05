package com.rzy.dealt_force_skills.compat.maid;

import com.rzy.dealt_force_skills.ai.operative.NativeOperativeSkills;
import com.rzy.dealt_force_skills.character.CharacterAvailability;
import com.rzy.dealt_force_skills.character.CharacterBranchPackManager;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.S2C_OpenMaidSelection;
import com.rzy.dealt_force_skills.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import java.util.List;
import java.util.UUID;

/** Server-authoritative, single-use selection grants bound to one owner, world and maid UUID. */
public final class MaidSelection {
    private static final String SESSION = "dealt_force_skills.MaidSelection";
    private MaidSelection() { }
    public static boolean owned(ServerPlayer player, Entity entity) {
        return !player.isSpectator() && player.isAlive() && entity instanceof MaidSkillHost host
                && host.dealtForceMaidSkills().supported() && !host.dealtForceMaidSkills().failed()
                && entity instanceof TamableAnimal maid && maid.isAlive()
                && player.getUUID().equals(maid.getOwnerUUID()) && entity.level() == player.level()
                && player.distanceToSqr(entity) <= 64;
    }
    public static List<String> available(ServerPlayer player) {
        return NativeOperativeSkills.characterIds().stream().filter(id -> CharacterBranchPackManager.findCharacter(id)
                .filter(c -> CharacterAvailability.selectionBlockedMessageKey(player, c).isEmpty()).isPresent())
                .sorted().toList();
    }
    public static void open(ServerPlayer player, Entity entity) {
        if (!owned(player, entity) || !player.getMainHandItem().is(ModItems.DRIFTWOOD.get())) return;
        UUID token = UUID.randomUUID();
        CompoundTag session = new CompoundTag();
        session.putUUID("Token", token); session.putUUID("Maid", entity.getUUID());
        session.putString("Dimension", player.level().dimension().location().toString());
        session.putLong("Expires", player.serverLevel().getGameTime() + 2400);
        player.getPersistentData().put(SESSION, session);
        var host = (MaidSkillHost)entity;
        NetworkHandler.sendToPlayer(new S2C_OpenMaidSelection(token, host.dealtForceMaidSkills().characterId(), available(player)), player);
    }
    public static boolean submit(ServerPlayer player, UUID token, String id) {
        CompoundTag session = player.getPersistentData().getCompound(SESSION);
        if (!session.hasUUID("Token") || !token.equals(session.getUUID("Token"))) return false;
        player.getPersistentData().remove(SESSION);
        if (!session.hasUUID("Maid") || player.serverLevel().getGameTime() > session.getLong("Expires")
                || !player.level().dimension().location().toString().equals(session.getString("Dimension"))
                || !player.getMainHandItem().is(ModItems.DRIFTWOOD.get())) return false;
        Entity entity = player.serverLevel().getEntity(session.getUUID("Maid"));
        if (!owned(player, entity) || !available(player).contains(id)) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.maid.selection_rejected"), false);
            return false;
        }
        if (!((MaidSkillHost)entity).dealtForceMaidSkills().select(id)) return false;
        if (!player.getAbilities().instabuild) player.getMainHandItem().shrink(1);
        player.displayClientMessage(Component.translatable("message.dealt_force_skills.maid.selected", entity.getDisplayName(),
                CharacterBranchPackManager.findCharacter(id).orElseThrow().displayName()), true);
        return true;
    }
    public static boolean clear(ServerPlayer player, Entity entity) {
        if (!owned(player, entity) || !player.getMainHandItem().isEmpty()) return false;
        var controller = ((MaidSkillHost)entity).dealtForceMaidSkills();
        if (controller.characterId().isEmpty() || !controller.clear()) return false;
        player.getPersistentData().remove(SESSION);
        player.displayClientMessage(Component.translatable("message.dealt_force_skills.maid.cleared", entity.getDisplayName()), true);
        return true;
    }
}
