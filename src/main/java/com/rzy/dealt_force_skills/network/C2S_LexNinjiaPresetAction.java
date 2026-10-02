package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.character.lexninjia.LexNinjiaComboInput;
import com.rzy.dealt_force_skills.character.lexninjia.LexNinjiaPreset;
import com.rzy.dealt_force_skills.shop.LexNinjiaShopManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record C2S_LexNinjiaPresetAction(
        Action action,
        int slot,
        String name,
        List<LexNinjiaComboInput> inputs
) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<C2S_LexNinjiaPresetAction> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "c2s_lex_ninjia_preset_action"));

    public static final StreamCodec<FriendlyByteBuf, C2S_LexNinjiaPresetAction> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    C2S_LexNinjiaPresetAction::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static final int MAX_NETWORK_INPUTS = 12;

    public C2S_LexNinjiaPresetAction {
        action = action == null ? Action.OPEN_MENU : action;
        name = name == null ? "" : name;
        inputs = inputs == null ? List.of() : List.copyOf(inputs);
    }

    public static C2S_LexNinjiaPresetAction openMenu() {
        return new C2S_LexNinjiaPresetAction(Action.OPEN_MENU, -1, "", List.of());
    }

    public static C2S_LexNinjiaPresetAction openConfig() {
        return new C2S_LexNinjiaPresetAction(Action.OPEN_CONFIG, -1, "", List.of());
    }

    public static C2S_LexNinjiaPresetAction execute(int slot) {
        return new C2S_LexNinjiaPresetAction(Action.EXECUTE, slot, "", List.of());
    }

    public static C2S_LexNinjiaPresetAction save(int slot, String name, List<LexNinjiaComboInput> inputs) {
        return new C2S_LexNinjiaPresetAction(Action.SAVE, slot, name, inputs);
    }

    public static void encode(C2S_LexNinjiaPresetAction msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.action.ordinal());
        buf.writeVarInt(msg.slot);
        buf.writeUtf(msg.name, LexNinjiaPreset.MAX_NAME_LENGTH);
        int count = Math.min(MAX_NETWORK_INPUTS, msg.inputs.size());
        buf.writeVarInt(count);
        for (int i = 0; i < count; i++) {
            buf.writeVarInt(msg.inputs.get(i).ordinal());
        }
    }

    public static C2S_LexNinjiaPresetAction decode(FriendlyByteBuf buf) {
        int actionOrdinal = buf.readVarInt();
        Action[] actions = Action.values();
        Action action = actionOrdinal >= 0 && actionOrdinal < actions.length
                ? actions[actionOrdinal]
                : Action.OPEN_MENU;
        int slot = buf.readVarInt();
        String name = buf.readUtf(LexNinjiaPreset.MAX_NAME_LENGTH);
        int count = buf.readVarInt();
        if (count < 0 || count > MAX_NETWORK_INPUTS) {
            throw new IllegalArgumentException("Invalid Lex Ninjia preset input count: " + count);
        }
        LexNinjiaComboInput[] values = LexNinjiaComboInput.values();
        List<LexNinjiaComboInput> inputs = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            int ordinal = buf.readVarInt();
            if (ordinal >= 0 && ordinal < values.length) {
                inputs.add(values[ordinal]);
            }
        }
        return new C2S_LexNinjiaPresetAction(action, slot, name, inputs);
    }

    public static void handle(C2S_LexNinjiaPresetAction msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player != null) {
                LexNinjiaShopManager.handlePresetAction(player, msg);
            }
        });
    }

    public enum Action {
        OPEN_MENU,
        OPEN_CONFIG,
        SAVE,
        EXECUTE
    }
}
