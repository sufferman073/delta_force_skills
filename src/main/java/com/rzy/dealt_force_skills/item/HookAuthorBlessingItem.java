package com.rzy.dealt_force_skills.item;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.advancement.DfsAchievements;
import com.rzy.dealt_force_skills.character.CharacterAvailability;
import com.rzy.dealt_force_skills.character.CharacterSelectionManager;
import com.rzy.dealt_force_skills.character.sineva.SinevaStateManager;
import com.rzy.dealt_force_skills.effect.ModItemEffectHelper;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.S2C_OpenCharacterSelection;
import com.rzy.dealt_force_skills.registry.ModItems;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.shop.DealtCurrencyProvider;
import com.rzy.dealt_force_skills.shop.DealtCurrencyRegistry;
import com.rzy.dealt_force_skills.shop.SpecialOperationsBaseManager;
import com.rzy.dealt_force_skills.skill.SkillDamageHelper;
import com.rzy.dealt_force_skills.util.RangedSoundHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class HookAuthorBlessingItem extends QualityTooltipItem {
    public static final int BUTTON_LEFT = 0;
    public static final int BUTTON_RIGHT = 1;
    public static final int PHASE_HOLD = 0;
    public static final int PHASE_RELEASE = 1;
    private static final int LONG_PRESS_TICKS = 10;
    private static final int MAX_SIGNAL_GAP_TICKS = 20;
    private static final int MAX_RANGE = 1024;
    private static final double MAX_CHARGE_SPEED = 24.0D;
    private static final double MAX_CHARGE_PITCH = 20.0D;
    // charge_step.ogg is 1.021625 seconds at 48 kHz; schedule to the nearest game tick.
    private static final double CHARGE_STEP_BASE_TICKS = 1.021625D * 20.0D;
    private static final String EXTRA_DROPS = "HookAuthorBlessingExtraDrops";
    private static final Map<UUID, HoldState> HOLDS = new HashMap<>();
    private static final Map<UUID, BlastState> BLASTS = new HashMap<>();
    private static final Map<UUID, Boolean> HELD_LAST_TICK = new HashMap<>();

    public HookAuthorBlessingItem(Properties properties) {
        super(properties.stacksTo(1), DfsItemQuality.COLORFUL, "tooltip.dealt_force_skills.hook_author_blessing");
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, net.minecraft.world.entity.player.Player player,
                                                   InteractionHand hand) {
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    public static void input(ServerPlayer player, int button, int phase, boolean sneaking, int heldTicks) {
        if (!player.isAlive() || player.isSpectator()
                || !player.getMainHandItem().is(ModItems.HOOK_AUTHOR_BLESSING.get())
                || button < BUTTON_LEFT || button > BUTTON_RIGHT) {
            cancel(player);
            return;
        }
        int safeHeld = Math.max(0, Math.min(20 * 60 * 60, heldTicks));
        if (phase == PHASE_HOLD) {
            onHold(player, button, sneaking, safeHeld);
        } else if (phase == PHASE_RELEASE) {
            onRelease(player, button, sneaking, safeHeld);
        }
    }

    private static void onHold(ServerPlayer player, int button, boolean sneaking, int heldTicks) {
        long now = player.level().getGameTime();
        HoldState state = HOLDS.get(player.getUUID());
        if (state == null || state.button != button || state.sneaking != sneaking || heldTicks < state.heldTicks) {
            state = new HoldState(button, sneaking, now, now, 0, now + LONG_PRESS_TICKS, now + LONG_PRESS_TICKS);
            HOLDS.put(player.getUUID(), state);
        } else {
            state.lastSignalTick = now;
            state.heldTicks = heldTicks;
        }
        if (heldTicks == LONG_PRESS_TICKS && (button != BUTTON_LEFT || sneaking)) {
            play(player, ModSounds.HOOK_AUTHOR_USE.get(), 0.85F, 1.0F);
        }
        if (button == BUTTON_RIGHT && sneaking && heldTicks >= LONG_PRESS_TICKS
                && state.lockedTarget == null) {
            LivingEntity target = findTarget(player, 128.0D);
            if (target != null) {
                state.lockedTarget = target.getUUID();
                play(player, ModSounds.HOOK_AUTHOR_LOCK.get(), 1.0F, 1.0F);
                feedback(player, "lock_target", target.getDisplayName());
            } else {
                feedback(player, "lock_missing");
            }
        }
    }

    private static void onRelease(ServerPlayer player, int button, boolean sneaking, int heldTicks) {
        HoldState state = HOLDS.remove(player.getUUID());
        long serverHeldTicks = state == null
                ? Math.max(0, heldTicks)
                : Math.max(0L, player.level().getGameTime() - state.startedTick);
        if (serverHeldTicks < LONG_PRESS_TICKS) {
            if (button == BUTTON_LEFT) {
                if (sneaking) { DfsAchievements.awardAll(player); feedback(player, "achievements"); }
                else { reselect(player); feedback(player, "reselect"); }
            } else {
                if (sneaking) copyTargetBlock(player); else { refreshSkills(player); feedback(player, "skills_refreshed"); }
            }
            return;
        }
        if (button == BUTTON_LEFT) {
            if (!sneaking) releaseSonic(player, state == null ? serverHeldTicks / 20L : state.chargeStage);
        } else if (sneaking) {
            executeLocked(player, state == null ? null : state.lockedTarget);
        } else {
            grantCurrencies(player);
            play(player, ModSounds.HOOK_AUTHOR_USE.get(), 1.0F, 0.8F);
            feedback(player, "currencies_granted");
        }
    }

    public static void tick(ServerPlayer player) {
        boolean held = player.getMainHandItem().is(ModItems.HOOK_AUTHOR_BLESSING.get());
        boolean wasHeld = HELD_LAST_TICK.getOrDefault(player.getUUID(), false);
        if (held != wasHeld) {
            play(player, held ? ModSounds.HOOK_AUTHOR_EQUIP.get() : ModSounds.HOOK_AUTHOR_UNEQUIP.get(),
                    0.9F, 1.0F);
            HELD_LAST_TICK.put(player.getUUID(), held);
        }
        HoldState state = HOLDS.get(player.getUUID());
        if (state != null) {
            long now = player.level().getGameTime();
            if (!held || now - state.lastSignalTick > MAX_SIGNAL_GAP_TICKS) {
                HOLDS.remove(player.getUUID());
            } else if (state.button == BUTTON_LEFT && !state.sneaking) {
                long elapsed = Math.max(0L, now - state.startedTick);
                if (elapsed >= LONG_PRESS_TICKS) {
                    while (now >= state.nextChargeUpgradeTick) { state.chargeStage++; state.nextChargeUpgradeTick += chargeUpgradeDelay(chargeSpeed(state.chargeStage)); }
                    if (now >= state.nextChargeSoundTick) { double speed = chargeSpeed(state.chargeStage); float playbackRate = chargePlaybackRate(speed); play(player, ModSounds.HOOK_AUTHOR_CHARGE_STEP.get(), 1.0F, playbackRate); state.nextChargeSoundTick = now + chargeStepDelay(speed); }
                    renderChargeParticles((ServerLevel) player.level(), player, elapsed);
                    if (elapsed % 5L == 0L) feedback(player, "sonic_charge", Math.round(sonicDamage(state.chargeStage)), String.format(java.util.Locale.ROOT, "%.1f", sonicRange(state.chargeStage)));
                }
            } else if (state.button == BUTTON_LEFT && state.sneaking) {
                long elapsed = Math.max(0L, now - state.startedTick);
                if (elapsed >= 20L && elapsed % 20L == 0L) {
                    player.heal(player.getMaxHealth() * 0.5F);
                    player.setAbsorptionAmount(player.getMaxHealth());
                    feedback(player, "restored");
                }
                if (elapsed > 180L) {
                    feedback(player, "healing_overload");
                    play(player, ModSounds.HOOK_AUTHOR_HISS.get(), 1.0F, 1.0F);
                    player.hurt(player.damageSources().genericKill(), Float.MAX_VALUE);
                    HOLDS.remove(player.getUUID());
                }
            } else if (state.button == BUTTON_RIGHT && state.sneaking) {
                showLockIndicator(player, state);
            }
        }
        tickBlast(player);
    }

    private static void releaseSonic(ServerPlayer player, long heldTicks) {
        float damage = sonicDamage(heldTicks);
        double range = sonicRange(heldTicks);
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        ServerLevel level = (ServerLevel) player.level();
        renderSonicBeam(level, eye, look, range);
        List<UUID> targets = new ArrayList<>();
        for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(range), entity -> entity != player && entity.isAlive())) {
            Vec3 delta = target.getBoundingBox().getCenter().subtract(eye);
            if (delta.lengthSqr() <= range * range && delta.normalize().dot(look) >= 0.5D) {
                targets.add(target.getUUID());
            }
        }
        if (!targets.isEmpty()) {
            BLASTS.put(player.getUUID(), new BlastState(targets,
                    SkillDamageHelper.allTypes((ServerLevel) player.level()), damage));
        }
        Vec3 origin = player.getEyePosition();
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.SONIC_BOOM, origin.x, origin.y, origin.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        for (UUID targetId : targets) {
            Entity entity = level.getEntity(targetId);
            if (entity instanceof LivingEntity target) {
                Vec3 center = target.getBoundingBox().getCenter();
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.SONIC_BOOM, center.x, center.y, center.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK, center.x, center.y, center.z, 18, 0.35D, 0.45D, 0.35D, 0.08D);
            }
        }
        play(player, ModSounds.HOOK_AUTHOR_RELEASE.get(), 1.0F, 1.0F);
        playInterruptingReleaseExtra(player);
        play(player, ModSounds.SHEPHERD_SONIC_TRAP_EXPLODE.get(), 0.85F, 1.0F);
        play(player, ModSounds.SHEPHERD_SONIC_SHOCK_HIT.get(), 0.8F, 1.0F);
        feedback(player, "sonic_released", targets.size(), Math.round(damage), String.format(java.util.Locale.ROOT, "%.1f", range));
    }

    private static void tickBlast(ServerPlayer player) {
        BlastState blast = BLASTS.get(player.getUUID());
        if (blast == null || blast.damageIndex >= blast.types.size()) return;
        var type = blast.types.get(blast.damageIndex++);
        for (UUID targetId : blast.targets) {
            Entity entity = ((ServerLevel) player.level()).getEntity(targetId);
            if (entity instanceof LivingEntity target && target.isAlive()) {
                target.invulnerableTime = 0;
                SkillDamageHelper.hurtUnscaled(target,
                        SkillDamageHelper.source((ServerLevel) player.level(), type, player, player), blast.damage);
            }
        }
        if (blast.damageIndex >= blast.types.size()) BLASTS.remove(player.getUUID());
    }

    private static void reselect(ServerPlayer player) {
        if (!CharacterSelectionManager.hasSelectedCharacter(player)) {
            if (CharacterSelectionManager.isNormalPlayer(player)) {
                CharacterSelectionManager.clearNormalPlayer(player);
            }
            CharacterAvailability.syncToClient(player);
            NetworkHandler.sendToPlayer(new S2C_OpenCharacterSelection(), player);
            return;
        }
        CharacterSelectionManager.grantCharacterReselection(player);
        CharacterAvailability.syncToClient(player);
        NetworkHandler.sendToPlayer(new S2C_OpenCharacterSelection(true, true), player);
    }

    private static void refreshSkills(ServerPlayer player) {
        ModItemEffectHelper.refreshSkillCooldowns(player, true);
        clearCooldowns(player.getPersistentData());
        SinevaStateManager.refreshBladeWireCharges(player);
        for (Item item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
            player.getCooldowns().removeCooldown(item);
        }
        for (var sound : ModSounds.HOOK_AUTHOR_78) {
            player.connection.send(new ClientboundStopSoundPacket(sound.get().getLocation(), SoundSource.PLAYERS));
        }
        SoundEvent sound = ModSounds.HOOK_AUTHOR_78.get(player.getRandom().nextInt(ModSounds.HOOK_AUTHOR_78.size())).get();
        player.playNotifySound(sound, SoundSource.PLAYERS, 0.8F, 0.9F + player.getRandom().nextFloat() * 0.2F);
    }

    private static void clearCooldowns(CompoundTag tag) {
        for (String key : new ArrayList<>(tag.getAllKeys())) {
            Tag value = tag.get(key);
            String normalized = key.toLowerCase(java.util.Locale.ROOT);
            if (normalized.contains("cooldown") || normalized.contains("recharge")
                    || normalized.contains("ready_at") || normalized.contains("readyat")) {
                tag.remove(key);
            } else if (value instanceof CompoundTag child) {
                clearCooldowns(child);
            }
        }
    }

    private static void grantCurrencies(ServerPlayer player) {
        SpecialOperationsBaseManager.grantHavocCoins(player, 1_919_810L);
        for (DealtCurrencyProvider provider : DealtCurrencyRegistry.providers()) {
            if (!provider.id().equals("havoc_coin")) provider.grant(player, 114_514L);
        }
    }

    private static void copyTargetBlock(ServerPlayer player) {
        HitResult hit = player.pick(64.0D, 1.0F, false);
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) { feedback(player, "copy_missing"); return; }
        BlockPos pos = blockHit.getBlockPos();
        var state = player.level().getBlockState(pos);
        if (state.isAir()) { feedback(player, "copy_missing"); return; }
        ItemStack copy = new ItemStack(state.getBlock().asItem());
        if (copy.isEmpty()) { feedback(player, "copy_missing"); return; }
        player.getInventory().placeItemBackInInventory(copy); feedback(player, "block_copied", copy.getHoverName());
    }

    private static LivingEntity findTarget(ServerPlayer player, double range) {
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getLookAngle().scale(range));
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, start, end,
                player.getBoundingBox().expandTowards(player.getLookAngle().scale(range)).inflate(1.0D),
                entity -> entity instanceof LivingEntity && !(entity instanceof Player)
                        && entity != player && entity.isAlive(), range * range);
        return hit != null && hit.getEntity() instanceof LivingEntity living ? living : null;
    }

    private static void executeLocked(ServerPlayer player, UUID targetId) {
        if (targetId == null) { feedback(player, "lock_missing"); return; }
        Entity entity = ((ServerLevel) player.level()).getEntity(targetId);
        if (!(entity instanceof LivingEntity target) || target instanceof Player || !target.isAlive()) { feedback(player, "lock_missing"); return; }
        target.getPersistentData().putInt(EXTRA_DROPS, 19);
        player.awardStat(Stats.ENTITY_KILLED.get(target.getType()), 114_513);
        target.invulnerableTime = 0;
        SkillDamageHelper.hurtUnscaled(target,
                SkillDamageHelper.source((ServerLevel) player.level(), SkillDamageHelper.TRUE_SKILL_DAMAGE, player, player),
                Float.MAX_VALUE);
        if (target.isAlive()) target.kill();
        play(player, ModSounds.HOOK_AUTHOR_EXECUTE.get(), 1.0F, 1.0F);
        feedback(player, "executed", target.getDisplayName());
    }

    private static float sonicDamage(long heldTicks) { long stage = Math.max(0L, heldTicks); return (float) Math.min(Float.MAX_VALUE, 20.0D * (1.0D + stage * 0.10D)); }
    private static double sonicRange(long heldTicks) { long stage = Math.max(0L, heldTicks); return Math.min(MAX_RANGE, 6.0D + stage / 3.0D); }
    private static double chargeSpeed(long stage) { return Math.min(MAX_CHARGE_SPEED, 1.0D + Math.max(0L, stage) * 0.10D); }
    private static float chargePlaybackRate(double speed) { double normalized = (Math.max(1.0D, Math.min(MAX_CHARGE_SPEED, speed)) - 1.0D) / (MAX_CHARGE_SPEED - 1.0D); return (float) (1.0D + normalized * (MAX_CHARGE_PITCH - 1.0D)); }
    private static long chargeStepDelay(double speed) { return Math.max(1L, (long) Math.ceil(CHARGE_STEP_BASE_TICKS / speed)); }
    private static long chargeUpgradeDelay(double speed) { return Math.max(1L, (long) Math.ceil(20.0D / speed)); }
    private static void renderChargeParticles(ServerLevel level, ServerPlayer player, long elapsed) { if (elapsed % 5L != 0L) return; Vec3 point = player.getEyePosition().add(player.getLookAngle().scale(1.0D)).add(0.0D, -0.3D, 0.0D); level.sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK, point.x, point.y, point.z, 3, 0.12D, 0.12D, 0.12D, 0.015D); }
    private static void renderSonicBeam(ServerLevel level, Vec3 origin, Vec3 look, double range) { for (double distance = 1.25D; distance <= Math.min(range, 128.0D); distance += 2.0D) { Vec3 point = origin.add(look.scale(distance)); level.sendParticles(net.minecraft.core.particles.ParticleTypes.SONIC_BOOM, point.x, point.y, point.z, 1, 0.0D, 0.0D, 0.0D, 0.0D); } }
    private static void showLockIndicator(ServerPlayer player, HoldState state) {
        if (state.lockedTarget == null || player.level().getGameTime() % 5L != 0L) return;
        Entity entity = ((ServerLevel) player.level()).getEntity(state.lockedTarget);
        if (!(entity instanceof LivingEntity target) || !target.isAlive()) { state.lockedTarget = null; feedback(player, "lock_missing"); return; }
        target.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.GLOWING, 10, 0, false, false, true));
        Vec3 center = target.getBoundingBox().getCenter();
        ((ServerLevel) player.level()).sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD, center.x, center.y, center.z, 8, 0.35D, target.getBbHeight() * 0.5D, 0.35D, 0.01D);
        feedback(player, "lock_target", target.getDisplayName());
    }
    private static void feedback(ServerPlayer player, String key, Object... args) { player.displayClientMessage(Component.translatable("message.dealt_force_skills.hook_author_blessing." + key, args), true); }

    private static void play(ServerPlayer player, SoundEvent sound, float volume, float pitch) {
        player.level().playSound(null, player.blockPosition(), sound, SoundSource.PLAYERS, volume, pitch);
    }

    private static void playInterruptingReleaseExtra(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Vec3 position = player.position();
        SoundEvent sound = ModSounds.HOOK_AUTHOR_RELEASE_EXTRA.get();
        // Prevent overlapping release-extra clips for nearby listeners.
        RangedSoundHelper.stop(level, position, sound, SoundSource.PLAYERS, 32.0D);
        float pitch = 0.95F + player.getRandom().nextFloat() * 0.10F;
        RangedSoundHelper.play(level, position, sound, SoundSource.PLAYERS, 1.0F, pitch, 32.0D);
    }

    private static void cancel(ServerPlayer player) {
        HOLDS.remove(player.getUUID());
        BLASTS.remove(player.getUUID());
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.dealt_force_skills.hook_author_blessing.controls")
                .withStyle(ChatFormatting.GRAY));
    }

    @EventBusSubscriber(modid = DealtForceSkillsMod.MODID)
    public static final class Events {
        @SubscribeEvent
        public static void onTick(PlayerTickEvent.Post event) {
            if (event.getEntity() instanceof ServerPlayer player) tick(player);
        }

        @SubscribeEvent
        public static void onDrops(LivingDropsEvent event) {
            int copies = event.getEntity().getPersistentData().getInt(EXTRA_DROPS);
            if (copies <= 0) return;
            List<net.minecraft.world.entity.item.ItemEntity> extra = new ArrayList<>();
            for (var drop : event.getDrops()) {
                for (int i = 0; i < copies; i++) {
                    extra.add(new net.minecraft.world.entity.item.ItemEntity(drop.level(), drop.getX(), drop.getY(),
                            drop.getZ(), drop.getItem().copy()));
                }
            }
            event.getDrops().addAll(extra);
            event.getEntity().getPersistentData().remove(EXTRA_DROPS);
        }

        @SubscribeEvent
        public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
            UUID id = event.getEntity().getUUID();
            HOLDS.remove(id);
            BLASTS.remove(id);
            HELD_LAST_TICK.remove(id);
        }

        @SubscribeEvent
        public static void onStop(ServerStoppingEvent event) {
            HOLDS.clear();
            BLASTS.clear();
            HELD_LAST_TICK.clear();
        }
    }

    private static final class HoldState {
        private final int button;
        private final boolean sneaking;
        private final long startedTick;
        private long lastSignalTick;
        private int heldTicks;
        private UUID lockedTarget;
        private long nextChargeSoundTick;
        private long chargeStage;
        private long nextChargeUpgradeTick;

        private HoldState(int button, boolean sneaking, long startedTick, long lastSignalTick, int heldTicks, long nextChargeSoundTick, long nextChargeUpgradeTick) {
            this.button = button;
            this.sneaking = sneaking;
            this.startedTick = startedTick;
            this.lastSignalTick = lastSignalTick;
            this.heldTicks = heldTicks;
            this.nextChargeSoundTick = nextChargeSoundTick;
            this.nextChargeUpgradeTick = nextChargeUpgradeTick;
        }
    }

    private static final class BlastState {
        private final List<UUID> targets;
        private final List<net.minecraft.resources.ResourceKey<net.minecraft.world.damagesource.DamageType>> types;
        private final float damage;
        private int damageIndex;

        private BlastState(List<UUID> targets,
                           List<net.minecraft.resources.ResourceKey<net.minecraft.world.damagesource.DamageType>> types,
                           float damage) {
            this.targets = List.copyOf(targets);
            this.types = List.copyOf(types);
            this.damage = damage;
        }
    }
}
