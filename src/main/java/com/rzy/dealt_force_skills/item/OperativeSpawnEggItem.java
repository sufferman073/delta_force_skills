package com.rzy.dealt_force_skills.item;

import com.rzy.dealt_force_skills.ai.operative.OperativeLoadout;
import com.rzy.dealt_force_skills.entity.OperativeEntity;
import com.rzy.dealt_force_skills.registry.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import java.util.List;

/** Sneaking defers placement until release; a completed charge creates one hostile operative. */
public final class OperativeSpawnEggItem extends Item {
    private static final String SAVED_OPERATIVE = "SavedOperative";
    private static final String PENDING = "OperativeEggUse";
    public static final int HOSTILE_CHARGE_TICKS = 40;
    private final String character;
    public OperativeSpawnEggItem(String character, Properties properties) { super(properties); this.character = character; }
    private static CompoundTag itemData(ItemStack stack) { return stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag(); }
    /** Called by the attack event before friendly damage cancellation. */
    public static boolean collect(Player player, OperativeEntity bot) {
        if (!player.isShiftKeyDown() || !player.getUUID().equals(bot.getOwnerUUID()) || !bot.isAlive()
                || bot.isPassenger() || bot.isVehicle()) return false;
        if (player.level().isClientSide) return true;
        var egg = com.rzy.dealt_force_skills.registry.ModItems.OPERATIVE_SPAWN_EGGS.get(bot.characterId());
        if (egg == null) return false;
        if (!player.getMainHandItem().isEmpty() && player.getInventory().getFreeSlot() < 0) return false;
        // Interrupt transient casts/reloads, then retain health, gear, effects, orders and skill cooldowns.
        // Existing deployed entities are recalled by the normal destructive removal lifecycle.
        bot.skills().cancel(bot); bot.gun().cancel(bot);
        CompoundTag saved = new CompoundTag(); bot.saveWithoutId(saved);
        ItemStack stack = new ItemStack(egg.get()); CompoundTag data = itemData(stack);
        data.put(SAVED_OPERATIVE, saved); stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(data));
        if (player.getMainHandItem().isEmpty()) player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        else if (!player.getInventory().add(stack)) return false;
        bot.discard();
        return true;
    }
    private boolean restore(ServerLevel level, Player player, ItemStack stack, Vec3 point) {
        CompoundTag saved = itemData(stack).getCompound(SAVED_OPERATIVE).copy();
        if (!saved.hasUUID("UUID") || !saved.hasUUID("OperativeOwner")
                || !player.getUUID().equals(saved.getUUID("OperativeOwner"))
                || !character.equals(saved.getString("OperativeCharacter"))) return false;
        for (ServerLevel other : level.getServer().getAllLevels())
            if (other.getEntity(saved.getUUID("UUID")) != null) return false;
        OperativeEntity bot = ModEntities.OPERATIVE.get().create(level);
        if (bot == null) return false;
        saved.putDouble("OperativePostX", point.x); saved.putDouble("OperativePostY", point.y); saved.putDouble("OperativePostZ", point.z);
        bot.load(saved); bot.moveTo(point.x, point.y, point.z, player.getYRot(), 0);
        bot.setDeltaMovement(Vec3.ZERO); bot.fallDistance = 0;
        if (!level.noCollision(bot, bot.getBoundingBox()) || !level.addFreshEntity(bot)) return false;
        stack.shrink(1); // Stateful eggs are one entity even in creative.
        player.getCooldowns().addCooldown(this, 10);
        return true;
    }
    public String characterId() { return character; }
    @Override public int getUseDuration(ItemStack stack, LivingEntity user) { return HOSTILE_CHARGE_TICKS; }
    @Override public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.BOW; }

    @Override public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        Level level = context.getLevel();
        var block = context.getClickedPos();
        if (!level.mayInteract(player, block) || !player.mayUseItemAt(block, context.getClickedFace(), context.getItemInHand())) return InteractionResult.FAIL;
        if (!level.getBlockState(block).getCollisionShape(level, block).isEmpty()) block = block.relative(context.getClickedFace());
        Vec3 point = Vec3.atBottomCenterOf(block);
        if (itemData(context.getItemInHand()).contains(SAVED_OPERATIVE, 10)) {
            if (level instanceof ServerLevel server && !spawn(server, player, context.getItemInHand(), point, false, false)) return InteractionResult.FAIL;
        } else if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                CompoundTag pending = new CompoundTag();
                pending.putString("Role", character);
                pending.putString("Dimension", level.dimension().location().toString());
                pending.putDouble("X", point.x); pending.putDouble("Y", point.y); pending.putDouble("Z", point.z);
                pending.putLong("Start", level.getGameTime());
                player.getPersistentData().put(PENDING, pending);
            }
            player.startUsingItem(context.getHand());
        } else if (level instanceof ServerLevel server) {
            player.getPersistentData().remove(PENDING);
            if (!spawn(server, player, context.getItemInHand(), point, false, false)) return InteractionResult.FAIL;
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        BlockHitResult hit = getPlayerPOVHitResult(level, player, net.minecraft.world.level.ClipContext.Fluid.SOURCE_ONLY);
        if (hit.getType() != HitResult.Type.BLOCK) return InteractionResultHolder.pass(player.getItemInHand(hand));
        return new InteractionResultHolder<>(useOn(new UseOnContext(player, hand, hit)), player.getItemInHand(hand));
    }
    @Override public void releaseUsing(ItemStack stack, Level level, LivingEntity user, int remaining) {
        if (user instanceof Player player && !level.isClientSide) finish(stack, (ServerLevel) level, player, false);
    }
    @Override public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
        if (user instanceof Player player && !level.isClientSide) finish(stack, (ServerLevel) level, player, true);
        return stack;
    }
    private void finish(ItemStack stack, ServerLevel level, Player player, boolean charged) {
        CompoundTag pending = player.getPersistentData().getCompound(PENDING);
        player.getPersistentData().remove(PENDING);
        if (!character.equals(pending.getString("Role")) || !level.dimension().location().toString().equals(pending.getString("Dimension"))) return;
        long elapsed = level.getGameTime() - pending.getLong("Start");
        if (elapsed < 0 || elapsed > HOSTILE_CHARGE_TICKS + 40 || !player.isShiftKeyDown()) return;
        Vec3 point = new Vec3(pending.getDouble("X"), pending.getDouble("Y"), pending.getDouble("Z"));
        spawn(level, player, stack, point, true, charged && elapsed >= HOSTILE_CHARGE_TICKS);
    }
    public boolean spawn(ServerLevel level, Player player, ItemStack stack, Vec3 point, boolean neutral, boolean hostile) {
        if (!Double.isFinite(point.x) || !Double.isFinite(point.y) || !Double.isFinite(point.z)
                || player.distanceToSqr(point) > 64 || !level.hasChunkAt(net.minecraft.core.BlockPos.containing(point))
                || !level.getWorldBorder().isWithinBounds(net.minecraft.core.BlockPos.containing(point))
                || !level.mayInteract(player, net.minecraft.core.BlockPos.containing(point))
                || !(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)
                || com.rzy.dealt_force_skills.character.ModCharacters.get(character).isEmpty()
                || com.rzy.dealt_force_skills.character.CharacterAvailability.selectionBlockedMessageKey(serverPlayer,
                        com.rzy.dealt_force_skills.character.ModCharacters.get(character).orElseThrow()).isPresent()) return false;
        if (itemData(stack).contains(SAVED_OPERATIVE, 10)) return restore(level, player, stack, point);
        OperativeEntity bot = ModEntities.OPERATIVE.get().create(level);
        if (bot == null || !bot.setCharacter(character)) return false;
        bot.moveTo(point.x, point.y, point.z, player.getYRot(), 0);
        if (!level.noCollision(bot, bot.getBoundingBox())) return false;
        bot.setOwnerUUID(neutral ? null : player.getUUID());
        if (hostile) bot.setHostileTo(player.getUUID());
        OperativeLoadout.equip(bot);
        if (!level.addFreshEntity(bot)) return false;
        if (!player.getAbilities().instabuild) stack.shrink(1);
        player.getCooldowns().addCooldown(this, 10);
        return true;
    }
    @Override public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.dealt_force_skills.operative_spawn_egg"));
    }
}
