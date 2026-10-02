package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.item.DfsEquipmentItem;
import com.rzy.dealt_force_skills.item.DfsEquipmentItem.Profile;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class C2S_ToggleHelmetVision implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<C2S_ToggleHelmetVision> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "c2s_toggle_helmet_vision"));

    public static final StreamCodec<FriendlyByteBuf, C2S_ToggleHelmetVision> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    C2S_ToggleHelmetVision::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final boolean cycle;

    public C2S_ToggleHelmetVision(boolean cycle) {
        this.cycle = cycle;
    }

    public static void encode(C2S_ToggleHelmetVision msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.cycle);
    }

    public static C2S_ToggleHelmetVision decode(FriendlyByteBuf buf) {
        return new C2S_ToggleHelmetVision(buf.readBoolean());
    }

    public static void handle(C2S_ToggleHelmetVision msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player == null || player.isSpectator()) {
                return;
            }

            ItemStack head = player.getMainHandItem();
            Profile profile = DfsEquipmentItem.profile(head);
            if (profile == null || !profile.hasHelmetOptics()) {
                return;
            }

            int current = storedToggleVisionMode(head);
            int next = msg.cycle
                    ? nextSneakingVisionMode(profile, current)
                    : nextNightVisionMode(profile, current);

            DfsEquipmentItem.setVisionMode(head, next);
            player.getInventory().setChanged();
            player.inventoryMenu.broadcastChanges();
            player.displayClientMessage(message(profile, current, next), true);
        });
    }

    private static int storedToggleVisionMode(ItemStack stack) {
        CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = cd != null ? cd.copyTag() : null;
        if (tag == null || !tag.contains(DfsEquipmentItem.TAG_VISION_MODE)) {
            return DfsEquipmentItem.VISION_OFF;
        }
        return DfsEquipmentItem.storedVisionMode(stack);
    }

    private static int nextNightVisionMode(Profile profile, int currentMode) {
        int current = DfsEquipmentItem.sanitizeVisionMode(profile, currentMode);
        if (profile.nightVision()) {
            return current == DfsEquipmentItem.VISION_NIGHT ? DfsEquipmentItem.VISION_OFF : DfsEquipmentItem.VISION_NIGHT;
        }
        if (profile.thermalVision()) {
            return current == DfsEquipmentItem.VISION_THERMAL ? DfsEquipmentItem.VISION_OFF : DfsEquipmentItem.VISION_THERMAL;
        }
        return DfsEquipmentItem.VISION_OFF;
    }

    private static int nextSneakingVisionMode(Profile profile, int currentMode) {
        int current = DfsEquipmentItem.sanitizeVisionMode(profile, currentMode);
        if (profile.thermalVision() && profile.nightVision()) {
            return switch (current) {
                case DfsEquipmentItem.VISION_THERMAL -> DfsEquipmentItem.VISION_NIGHT;
                case DfsEquipmentItem.VISION_NIGHT -> DfsEquipmentItem.VISION_OFF;
                default -> DfsEquipmentItem.VISION_THERMAL;
            };
        }
        return DfsEquipmentItem.nextVisionMode(profile, current);
    }

    private static Component message(Profile profile, int previousMode, int nextMode) {
        if (nextMode == DfsEquipmentItem.VISION_NIGHT) {
            return Component.literal("夜视仪：开启");
        }
        if (nextMode == DfsEquipmentItem.VISION_THERMAL) {
            return Component.literal("热成像：开启");
        }
        if (previousMode == DfsEquipmentItem.VISION_THERMAL || (!profile.nightVision() && profile.thermalVision())) {
            return Component.literal("热成像：关闭");
        }
        return Component.literal("夜视仪：关闭");
    }
}
