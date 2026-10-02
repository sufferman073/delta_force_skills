package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.block.BladeWireBlockEntity;
import com.rzy.dealt_force_skills.compat.LesRaisinsTacticalCompat;
import com.rzy.dealt_force_skills.util.MeleeWeaponCompat;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class C2S_MeleeWireCut implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<C2S_MeleeWireCut> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "c2s_melee_wire_cut"));

    public static final StreamCodec<FriendlyByteBuf, C2S_MeleeWireCut> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    C2S_MeleeWireCut::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static final double DIRECT_TARGET_RANGE = 5.0D;
    private static final String LAST_SUCCESS_TICK = "dealt_force_skills.melee_wire_cut_packet_tick";

    private final boolean specialAttack;

    public C2S_MeleeWireCut(boolean specialAttack) {
        this.specialAttack = specialAttack;
    }

    public static void encode(C2S_MeleeWireCut msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.specialAttack);
    }

    public static C2S_MeleeWireCut decode(FriendlyByteBuf buf) {
        return new C2S_MeleeWireCut(buf.readBoolean());
    }

    public static void handle(C2S_MeleeWireCut msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player == null || player.isSpectator() || !player.isAlive()
                    || !(player.level() instanceof ServerLevel level)) {
                return;
            }
            long now = level.getGameTime();
            if (player.getPersistentData().getLong(LAST_SUCCESS_TICK) == now) {
                return;
            }

            ItemStack weapon = player.getMainHandItem();
            if (!MeleeWeaponCompat.isMeleeWeapon(weapon)) {
                return;
            }

            boolean cut = false;
            if (MeleeWeaponCompat.isLesRaisinsMelee(weapon)) {
                LesRaisinsTacticalCompat.MeleeCutShape shape =
                        LesRaisinsTacticalCompat.inputAttackShape(player, msg.specialAttack);
                cut = BladeWireBlockEntity.destroyWiresInMeleeArc(player, shape.range(), shape.halfWidth(), shape.height());
                if (!cut) {
                    cut = BladeWireBlockEntity.destroyTargetedWire(player, DIRECT_TARGET_RANGE);
                }
            } else if (!msg.specialAttack) {
                cut = BladeWireBlockEntity.destroyTargetedWire(player, DIRECT_TARGET_RANGE);
            }

            if (cut) {
                player.getPersistentData().putLong(LAST_SUCCESS_TICK, now);
                damageCuttingWeapon(player);
            }
        });
    }

    private static void damageCuttingWeapon(ServerPlayer player) {
        ItemStack weapon = player.getMainHandItem();
        if (!weapon.isEmpty() && !player.getAbilities().instabuild) {
            weapon.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
        }
    }
}
