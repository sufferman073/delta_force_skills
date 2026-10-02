package com.rzy.dealt_force_skills.stamina;

import com.rzy.dealt_force_skills.character.stinger.StingerStateManager;
import com.rzy.dealt_force_skills.entity.UluruLoiteringMissileEntity;
import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.team.RoundStartFreezeManager;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.minecraft.resources.ResourceLocation;

public final class StaminaMovement {
    private static final ResourceLocation SPRINT_ID = ResourceLocation.fromNamespaceAndPath("dealt_force_skills", "stamina_sprint");
    private static final ResourceLocation SWIM_ID = ResourceLocation.fromNamespaceAndPath("dealt_force_skills", "stamina_swim");

    private StaminaMovement() { }

    public static void reset(net.minecraft.world.entity.player.Player player) {
        CompoundTag state = StaminaManager.data(player);
        state.putInt("VaultTicks", 0);
        state.remove("SprintBoostTicks");
        state.remove("SwimBoostTicks");
        state.remove("LastX");
        state.remove("LastY");
        state.remove("LastZ");
        player.setForcedPose(null);
        modifier(player.getAttribute(Attributes.MOVEMENT_SPEED), SPRINT_ID, 0.0D);
        modifier(player.getAttribute(NeoForgeMod.SWIM_SPEED), SWIM_ID, 0.0D);
    }

    public static boolean canMove(ServerPlayer player) {
        return player != null && player.isAlive() && !player.isSpectator() && !player.isPassenger()
                && !player.getAbilities().flying && !player.isFallFlying() && !player.isSleeping()
                && !player.hasEffect(ModEffects.STUN) && !player.hasEffect(ModEffects.WEBBED)
                && !StingerStateManager.isDowned(player) && !RoundStartFreezeManager.isFrozen(player)
                && !UluruLoiteringMissileEntity.isPlayerControlling(player);
    }

    public static void tick(ServerPlayer player) {
        CompoundTag state = StaminaManager.data(player);
        tickVault(player, state);
        double dx = player.getX() - state.getDouble("LastX");
        double dz = player.getZ() - state.getDouble("LastZ");
        double dy = player.getY() - state.getDouble("LastY");
        boolean moving = state.contains("LastX") && dx * dx + dz * dz
                + (player.isSwimming() ? dy * dy : 0.0D) > 1.0E-6D;
        state.putDouble("LastY", player.getY());
        state.putDouble("LastX", player.getX());
        state.putDouble("LastZ", player.getZ());
        double value = StaminaManager.currentValue(player);
        boolean exhausted = state.getBoolean("BoostExhausted");
        if (StaminaManager.isFree(player) || value >= 2.0D) exhausted = false;
        else if (value <= 1.0D) exhausted = true;
        state.putBoolean("BoostExhausted", exhausted);
        boolean eligible = canMove(player) && state.getInt("VaultTicks") == 0;
        boolean swim = boost(player, state, "SwimBoostTicks", eligible && player.isInWater(),
                moving && player.isSwimming() && !exhausted, StaminaRules.SWIM_COST_PER_TICK);
        boolean sprint = boost(player, state, "SprintBoostTicks", eligible && !player.isInWater(),
                moving && player.isSprinting() && !exhausted, StaminaRules.SPRINT_COST_PER_TICK);
        double multiplier = StaminaManager.actionMultiplier(player);
        modifier(player.getAttribute(Attributes.MOVEMENT_SPEED), SPRINT_ID, sprint ? 0.50D * multiplier : 0.0D);
        modifier(player.getAttribute(NeoForgeMod.SWIM_SPEED), SWIM_ID, swim ? 1.0D * multiplier : 0.0D);
    }

    private static boolean boost(ServerPlayer player, CompoundTag state, String key,
                                 boolean allowed, boolean active, double cost) {
        int ticks = allowed ? Math.max(0, state.getInt(key) - 1) : 0;
        // Missing movement packets or a brief flag drop must not toggle the attribute every tick.
        if (allowed && active && StaminaManager.consume(player, cost) == StaminaManager.ConsumeResult.SUCCESS) {
            ticks = StaminaRules.BOOST_GRACE_TICKS;
        }
        state.putInt(key, ticks);
        return ticks > 0;
    }

    private static void modifier(AttributeInstance attribute, ResourceLocation id, double amount) {
        if (attribute == null) return;
        AttributeModifier old = attribute.getModifier(id);
        if (old != null && Math.abs(old.amount() - amount) < 1.0E-9D) return;
        attribute.removeModifier(id);
        if (amount > 0.0D) attribute.addTransientModifier(new AttributeModifier(id,
                amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    public static void tryVault(ServerPlayer player) {
        if (!canMove(player)) return;
        CompoundTag state = StaminaManager.data(player);
        long now = player.level().getGameTime();
        if (state.getInt("VaultTicks") > 0 || now < state.getLong("NextVault")) return;
        state.putLong("NextVault", now + 4);
        if (StaminaManager.canConsume(player, StaminaRules.VAULT_COST) != StaminaManager.ConsumeResult.SUCCESS) return;
        Vec3 look = player.getLookAngle();
        Vec3 forward = new Vec3(look.x, 0.0D, look.z).normalize();
        if (forward.lengthSqr() < 0.5D) return;
        Vec3 target = findTop(player, forward);
        if (target == null) return;
        if (canJumpOnto(player, target)) return;
        Vec3 raised = new Vec3(player.getX(), target.y, player.getZ());
        if (!clearPath(player, player.position(), raised) || !clearPath(player, raised, target)) return;
        if (StaminaManager.consume(player, StaminaRules.VAULT_COST) != StaminaManager.ConsumeResult.SUCCESS) return;
        int ticks = Math.max(2, (int) Math.ceil(5.0D / StaminaManager.actionMultiplier(player)));
        state.putInt("VaultTicks", ticks);
        state.putInt("VaultTotal", ticks);
        state.putDouble("VaultStartX", player.getX());
        state.putDouble("VaultStartY", player.getY());
        state.putDouble("VaultStartZ", player.getZ());
        state.putDouble("VaultX", target.x);
        state.putDouble("VaultY", target.y);
        state.putDouble("VaultZ", target.z);
        player.swing(InteractionHand.MAIN_HAND, true);
    }

    private static boolean canJumpOnto(ServerPlayer player, Vec3 target) {
        if (player.isInWater() || player.isInLava()) return false;
        boolean grounded = player.onGround() || !player.level().noCollision(player,
                standingBox(player, player.position().add(0.0D, -0.05D, 0.0D)));
        double velocity = Math.max(0.0D, player.getDeltaMovement().y);
        if (grounded) {
            float factor = player.level().getBlockState(player.blockPosition()).getBlock().getJumpFactor();
            if (factor == 1.0F) {
                factor = player.level().getBlockState(BlockPos.containing(player.getX(),
                        player.getY() - 0.500001D, player.getZ())).getBlock().getJumpFactor();
            }
            velocity = player.getAttributeValue(Attributes.JUMP_STRENGTH) * factor + player.getJumpBoostPower();
        }
        double gravity = player.getAttributeValue(Attributes.GRAVITY);
        if (target.y - player.getY() > StaminaRules.jumpRise(velocity, gravity) + 0.001D) return false;
        Vec3 raised = new Vec3(player.getX(), target.y, player.getZ());
        return clearPath(player, player.position(), raised) && clearPath(player, raised, target);
    }

    private static Vec3 findTop(ServerPlayer player, Vec3 forward) {
        for (double distance = 0.4D; distance <= 1.41D; distance += 0.1D) {
            Vec3 ahead = player.position().add(forward.scale(distance));
            for (int y = (int) Math.floor(player.getY()); y <= Math.floor(player.getY() + 2.5D); y++) {
                BlockPos pos = BlockPos.containing(ahead.x, y, ahead.z);
                if (!player.level().hasChunkAt(pos)) continue;
                for (AABB box : player.level().getBlockState(pos).getCollisionShape(player.level(), pos).toAabbs()) {
                    AABB world = box.move(pos);
                    if (ahead.x < world.minX || ahead.x > world.maxX || ahead.z < world.minZ || ahead.z > world.maxZ) continue;
                    double rise = world.maxY - player.getY();
                    if (rise <= 0.05D || rise > 2.5D + 1.0E-7D) continue;
                    Vec3 top = new Vec3(ahead.x, world.maxY + 0.001D, ahead.z);
                    if (player.level().getWorldBorder().isWithinBounds(standingBox(player, top))
                            && player.level().noCollision(player, standingBox(player, top))) return top;
                }
            }
        }
        return null;
    }

    private static AABB standingBox(ServerPlayer player, Vec3 at) {
        double half = player.getDimensions(Pose.STANDING).width() / 2.0D;
        return new AABB(at.x - half, at.y, at.z - half,
                at.x + half, at.y + player.getDimensions(Pose.STANDING).height(), at.z + half).deflate(1.0E-5D);
    }

    private static boolean clearPath(ServerPlayer player, Vec3 from, Vec3 to) {
        int steps = Math.max(1, (int) Math.ceil(from.distanceTo(to) / 0.1D));
        for (int i = 1; i <= steps; i++) {
            Vec3 at = from.lerp(to, i / (double) steps);
            if (!player.level().hasChunkAt(BlockPos.containing(at))
                    || !player.level().getWorldBorder().isWithinBounds(standingBox(player, at))
                    || !player.level().noCollision(player, standingBox(player, at))) return false;
        }
        return true;
    }

    private static void tickVault(ServerPlayer player, CompoundTag state) {
        int remaining = state.getInt("VaultTicks");
        if (remaining <= 0) return;
        Vec3 start = new Vec3(state.getDouble("VaultStartX"), state.getDouble("VaultStartY"), state.getDouble("VaultStartZ"));
        Vec3 target = new Vec3(state.getDouble("VaultX"), state.getDouble("VaultY"), state.getDouble("VaultZ"));
        int total = Math.max(1, state.getInt("VaultTotal"));
        Vec3 previous = player.position();
        Vec3 next = previous;
        Vec3 raised = new Vec3(start.x, target.y, start.z);
        boolean clear = canMove(player) && previous.distanceTo(start) <= 4.0D;
        // Three swept movement judgments per tick; rendering is interpolated independently.
        for (int substep = 1; clear && substep <= 3; substep++) {
            double progress = (total - remaining + substep / 3.0D) / total;
            Vec3 candidate = progress <= 0.6D ? start.lerp(raised, progress / 0.6D)
                    : raised.lerp(target, (progress - 0.6D) / 0.4D);
            boolean crossesCorner = progress > 0.6D && next.y < target.y - 0.0001D;
            clear = crossesCorner ? clearPath(player, next, raised) && clearPath(player, raised, candidate)
                    : clearPath(player, next, candidate);
            next = candidate;
        }
        if (!clear) {
            state.putInt("VaultTicks", 0);
            player.setForcedPose(null);
            return;
        }
        com.rzy.dealt_force_skills.network.NetworkHandler.sendToPlayer(
                new com.rzy.dealt_force_skills.network.S2C_VaultMotion(previous.x, previous.y, previous.z,
                        next.x, next.y, next.z), player);
        player.setForcedPose(Pose.CROUCHING);
        player.teleportTo(next.x, next.y, next.z);
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0.0F;
        state.putInt("VaultTicks", remaining - 1);
        if (remaining == 1) player.setForcedPose(null);
    }
}
