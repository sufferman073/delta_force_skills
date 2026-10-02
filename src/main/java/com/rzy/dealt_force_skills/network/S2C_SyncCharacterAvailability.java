package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.character.CharacterBranch;
import com.rzy.dealt_force_skills.character.CharacterBranchPackManager;
import com.rzy.dealt_force_skills.character.CharacterDefinition;
import com.rzy.dealt_force_skills.character.CharacterRole;
import com.rzy.dealt_force_skills.character.SkillDefinition;
import com.rzy.dealt_force_skills.character.SkillSlot;
import net.minecraft.network.FriendlyByteBuf;

import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class S2C_SyncCharacterAvailability implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<S2C_SyncCharacterAvailability> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "s2c_sync_character_availability"));

    public static final StreamCodec<FriendlyByteBuf, S2C_SyncCharacterAvailability> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    S2C_SyncCharacterAvailability::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final Map<String, String> unavailableReasons;
    private final List<CharacterBranch> branches;

    public S2C_SyncCharacterAvailability(Map<String, String> unavailableReasons) {
        this(unavailableReasons, CharacterBranchPackManager.currentBranches());
    }

    public S2C_SyncCharacterAvailability(Map<String, String> unavailableReasons, List<CharacterBranch> branches) {
        this.unavailableReasons = Map.copyOf(unavailableReasons);
        this.branches = branches == null ? List.of() : List.copyOf(branches);
    }

    public static void encode(S2C_SyncCharacterAvailability msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.unavailableReasons.size());
        msg.unavailableReasons.forEach((characterId, reasonKey) -> {
            buf.writeUtf(characterId);
            buf.writeUtf(reasonKey);
        });
        buf.writeVarInt(msg.branches.size());
        for (CharacterBranch branch : msg.branches) {
            buf.writeUtf(branch.id());
            buf.writeUtf(branch.name());
            buf.writeBoolean(branch.nameTranslationKey());
            buf.writeVarInt(branch.characters().size());
            for (CharacterDefinition character : branch.characters()) {
                writeCharacter(buf, character);
            }
        }
    }

    public static S2C_SyncCharacterAvailability decode(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        Map<String, String> unavailableReasons = new LinkedHashMap<>();
        for (int i = 0; i < size; i++) {
            unavailableReasons.put(buf.readUtf(256), buf.readUtf(256));
        }
        int branchCount = buf.readVarInt();
        List<CharacterBranch> branches = new ArrayList<>(branchCount);
        for (int i = 0; i < branchCount; i++) {
            String id = buf.readUtf(256);
            String name = buf.readUtf(512);
            boolean nameTranslationKey = buf.readBoolean();
            int characterCount = buf.readVarInt();
            List<CharacterDefinition> characters = new ArrayList<>(characterCount);
            for (int characterIndex = 0; characterIndex < characterCount; characterIndex++) {
                characters.add(readCharacter(buf));
            }
            branches.add(new CharacterBranch(id, name, nameTranslationKey, characters));
        }
        return new S2C_SyncCharacterAvailability(unavailableReasons, branches);
    }

    public static void handle(S2C_SyncCharacterAvailability msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
                    ClientCharacterSelectionState.syncSelectionCatalog(msg.branches);
                    ClientCharacterSelectionState.syncCharacterAvailability(msg.unavailableReasons);
                });
    }

    private static void writeCharacter(FriendlyByteBuf buf, CharacterDefinition character) {
        buf.writeUtf(character.id());
        buf.writeUtf(character.nameTranslationKey());
        buf.writeBoolean(character.nameIsTranslationKey());
        buf.writeVarInt(character.role().ordinal());
        buf.writeUtf(character.sourcePath());
        buf.writeBoolean(character.selectable());
        buf.writeVarInt(character.skills().size());
        for (SkillDefinition skill : character.skills()) {
            buf.writeVarInt(skill.slot().ordinal());
            buf.writeUtf(skill.translationKey());
            buf.writeBoolean(skill.translationIsKey());
            buf.writeUtf(skill.descriptionTranslationKey());
            buf.writeBoolean(skill.descriptionIsKey());
            buf.writeVarInt(Math.max(0, skill.cooldownTicks()));
            buf.writeBoolean(skill.implemented());
        }
    }

    private static CharacterDefinition readCharacter(FriendlyByteBuf buf) {
        String id = buf.readUtf(256);
        String name = buf.readUtf(512);
        boolean nameIsTranslationKey = buf.readBoolean();
        CharacterRole role = roleByOrdinal(buf.readVarInt());
        String sourcePath = buf.readUtf(512);
        boolean selectable = buf.readBoolean();
        int skillCount = buf.readVarInt();
        List<SkillDefinition> skills = new ArrayList<>(skillCount);
        for (int i = 0; i < skillCount; i++) {
            SkillSlot slot = skillByOrdinal(buf.readVarInt());
            String skillName = buf.readUtf(512);
            boolean skillNameIsKey = buf.readBoolean();
            String skillDescription = buf.readUtf(1024);
            boolean skillDescriptionIsKey = buf.readBoolean();
            int cooldownTicks = buf.readVarInt();
            boolean implemented = buf.readBoolean();
            skills.add(new SkillDefinition(slot, skillName, skillNameIsKey, skillDescription, skillDescriptionIsKey,
                    cooldownTicks, implemented));
        }
        return new CharacterDefinition(id, name, nameIsTranslationKey, role, sourcePath, selectable, skills);
    }

    private static CharacterRole roleByOrdinal(int ordinal) {
        CharacterRole[] values = CharacterRole.values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : CharacterRole.SPECIAL;
    }

    private static SkillSlot skillByOrdinal(int ordinal) {
        SkillSlot[] values = SkillSlot.values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : SkillSlot.PASSIVE;
    }
}
