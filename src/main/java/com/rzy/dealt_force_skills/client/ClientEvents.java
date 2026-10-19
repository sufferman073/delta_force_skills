package com.rzy.dealt_force_skills.client;

import com.rzy.dealt_force_skills.config.DealtForceConfig;
import com.rzy.dealt_force_skills.config.DealtItemsHavocConfig;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.character.SkillSlot;
import com.rzy.dealt_force_skills.character.commander.CommanderStateManager;
import com.rzy.dealt_force_skills.character.ghroth.GhrothTaczEnhancement;
import com.rzy.dealt_force_skills.compat.SuperbWarfareCompat;
import com.rzy.dealt_force_skills.client.character.ClientCharacterSelectionState;
import com.rzy.dealt_force_skills.client.character.ClientCatDadHudState;
import com.rzy.dealt_force_skills.client.character.ClientChamberHudState;
import com.rzy.dealt_force_skills.client.character.ClientCommanderState;
import com.rzy.dealt_force_skills.client.character.ClientCommanderVehicleState;
import com.rzy.dealt_force_skills.client.character.ClientCorpsHudState;
import com.rzy.dealt_force_skills.client.character.ClientDWolfHudState;
import com.rzy.dealt_force_skills.client.character.ClientDepartmentHudState;
import com.rzy.dealt_force_skills.client.character.ClientGamblerHudState;
import com.rzy.dealt_force_skills.client.character.ClientGamblerTargetingState;
import com.rzy.dealt_force_skills.client.character.ClientGizmoHudState;
import com.rzy.dealt_force_skills.client.character.ClientHackclawHudState;
import com.rzy.dealt_force_skills.client.character.ClientHackclawCoreVisualState;
import com.rzy.dealt_force_skills.client.character.ClientHeldToolVisualState;
import com.rzy.dealt_force_skills.client.character.ClientLunaHudState;
import com.rzy.dealt_force_skills.client.character.ClientLunaBowVisualState;
import com.rzy.dealt_force_skills.client.character.ClientLexNinjiaHudState;
import com.rzy.dealt_force_skills.client.character.ClientManbaHudState;
import com.rzy.dealt_force_skills.client.character.ClientMorseHudState;
import com.rzy.dealt_force_skills.client.character.ClientNikaidouHiroHudState;
import com.rzy.dealt_force_skills.client.character.ClientNoxHudState;
import com.rzy.dealt_force_skills.client.character.ClientNTwoHudState;
import com.rzy.dealt_force_skills.client.character.ClientRaptorHudState;
import com.rzy.dealt_force_skills.client.character.ClientSaeedHudState;
import com.rzy.dealt_force_skills.client.character.ClientShepherdHudState;
import com.rzy.dealt_force_skills.client.character.ClientSinevaHudState;
import com.rzy.dealt_force_skills.client.character.ClientCommanderState;
import com.rzy.dealt_force_skills.client.character.ClientSinevaRenderState;
import com.rzy.dealt_force_skills.client.character.ClientSkillModelVisualState;
import com.rzy.dealt_force_skills.client.character.ClientStingerHudState;
import com.rzy.dealt_force_skills.client.character.ClientTempestHudState;
import com.rzy.dealt_force_skills.client.character.ClientToxikHudState;
import com.rzy.dealt_force_skills.client.character.ClientUluruHudState;
import com.rzy.dealt_force_skills.client.character.ClientUndeadHudState;
import com.rzy.dealt_force_skills.client.character.ClientVlinderHudState;
import com.rzy.dealt_force_skills.client.character.ClientVyronHudState;
import com.rzy.dealt_force_skills.client.particle.GizmoLargeSmokeParticle;
import com.rzy.dealt_force_skills.client.particle.LargeSmokeParticle;
import com.rzy.dealt_force_skills.client.particle.StingerHealingSmokeParticle;
import com.rzy.dealt_force_skills.client.particle.StingerLargeSmokeParticle;
import com.rzy.dealt_force_skills.client.particle.ToxikLargeSmokeParticle;
import com.rzy.dealt_force_skills.client.model.BeaconBossModel;
import com.rzy.dealt_force_skills.client.renderer.CatDadRoadTruckRenderer;
import com.rzy.dealt_force_skills.client.renderer.BladeWireBlockEntityRenderer;
import com.rzy.dealt_force_skills.client.renderer.BlockbenchProjectileRenderer;
import com.rzy.dealt_force_skills.client.renderer.DWolfHandCannonGrenadeRenderer;
import com.rzy.dealt_force_skills.client.renderer.GrappleHookRenderer;
import com.rzy.dealt_force_skills.client.renderer.BeaconBossRenderer;
import com.rzy.dealt_force_skills.client.renderer.NoxDecoyRenderer;
import com.rzy.dealt_force_skills.client.renderer.SaeedGuardRenderer;
import com.rzy.dealt_force_skills.client.renderer.SmokeCloudRenderer;
import com.rzy.dealt_force_skills.client.renderer.TempestRecallAnchorRenderer;
import com.rzy.dealt_force_skills.client.renderer.DeathCrateRenderer;
import com.rzy.dealt_force_skills.client.renderer.VyronMagneticBombRenderer;
import com.rzy.dealt_force_skills.client.renderer.VyronTigerCannonProjectileRenderer;
import com.rzy.dealt_force_skills.client.visual.ClientSinevaKnockdownState;
import com.rzy.dealt_force_skills.client.visual.ClientSinevaVisualState;
import com.rzy.dealt_force_skills.client.visual.ClientToolReleaseAction;
import com.rzy.dealt_force_skills.client.visual.DfsEquipmentModelVisuals;
import com.rzy.dealt_force_skills.client.visual.DWolfSlideVisuals;
import com.rzy.dealt_force_skills.client.visual.ManbaFlashlightBeamRenderer;
import com.rzy.dealt_force_skills.client.visual.SinevaPlaceholderVisuals;
import com.rzy.dealt_force_skills.character.sineva.SinevaStateManager;
import com.rzy.dealt_force_skills.character.toxik.ToxikStateManager;
import com.rzy.dealt_force_skills.effect.ModItemEffectHelper;
import com.rzy.dealt_force_skills.item.DfsEquipmentItem;
import com.rzy.dealt_force_skills.item.DfsEquipmentItem.Profile;
import com.rzy.dealt_force_skills.item.DfsEquipmentItem.SpecialAbility;
import com.rzy.dealt_force_skills.util.TargetingUtil;
import com.rzy.dealt_force_skills.network.C2S_OpenSelectionOrShop;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.registry.ModEntities;
import com.rzy.dealt_force_skills.registry.ModBlockEntities;
import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.registry.ModGameRules;
import com.rzy.dealt_force_skills.registry.ModParticles;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import com.rzy.dealt_force_skills.util.ItemStackTagReads;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

@Mod.EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT)
public class ClientEvents {
    private static volatile int CORE_LONG_HOLD_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("CORE_LONG_HOLD_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("client.client_events.core_long_hold_ticks", 15));
    private static volatile int SHEPHERD_CORE_LONG_HOLD_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("SHEPHERD_CORE_LONG_HOLD_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("client.client_events.shepherd_core_long_hold_ticks", 15));
    private static int coreKeyHeldTicks = 0;
    private static boolean coreKeyWasDown = false;
    /** Keeps stale pseudo-tool handlers from swallowing vanilla screens after local death. */
    private static boolean localDeathInputOverride;
    private static int suppressHurtAnimationTicks;
    private static boolean forcedCharacterSpinActive;
    private static float forcedCharacterSpinYaw;
    private static Field minecraftRightClickDelayField;
    private static boolean minecraftRightClickDelayFieldChecked;
    private static final String TACZ_CLIENT_GUN_OPERATOR = "com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator";
    private static final String MHS_FURY_UNTIL = "dealt_force_skills.mhs_fury_until";
    private static final String TACZ_GUN_ID_TAG = "GunId";
    private static final String TRICK_STACKS = "dealt_force_skills.trick_stacks";
    private static final String TRICK_UNTIL = "dealt_force_skills.trick_until";
    private static final String DICH9_STACKS = "dealt_force_skills.dich9_stacks";
    private static final String DICH9_UNTIL = "dealt_force_skills.dich9_until";
    private static volatile double MASK1_LOCK_RANGE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("MASK1_LOCK_RANGE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("client.client_events.mask1_lock_range", 64.0));
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onClientTickTaczAdrenaline(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            ClientCommanderVehicleState.reset();
            return;
        }
        tickSuppressHurtAnimation(mc.player);
        CommanderStateManager.ensureVanillaStepHeight(mc.player);
        SinevaStateManager.updateBombSuitStepHeight(mc.player,
                (ClientSinevaHudState.shouldRender() || ClientCommanderState.inherits(
                        com.rzy.dealt_force_skills.character.ModCharacters.SINEVA_ID))
                        && ClientSinevaHudState.bombSuitActive(), ClientCommanderState.shouldRender());
        accelerateTaczAdrenalineClient(mc.player);
        TaczSpeedMultipliers multipliers = taczSpeedMultipliers(mc.player);
        boolean shockBlocked = superbWarfareShockBlocked(mc.player);
        SuperbWarfareCompat.applyClientWeaponModifiers(mc.player,
                multipliers.fireRate(), multipliers.aim(), multipliers.aimPenalty(),
                ClientCommanderVehicleState.isCommanderVehicle(mc.player.getVehicle()), shockBlocked);
        delayTaczMorseShockClient(mc.player);
    }

    @SubscribeEvent
    public static void onSinevaBombSuitLocalJump(LivingEvent.LivingJumpEvent event) {
        if (!(event.getEntity() instanceof LocalPlayer player)
                || !ClientSinevaHudState.shouldRender()
                || !ClientSinevaHudState.bombSuitActive()) {
            return;
        }
        Vec3 movement = player.getDeltaMovement();
        double multiplier = SinevaStateManager.bombSuitJumpVelocityMultiplier();
        if (movement.y > 0.0D && multiplier < 1.0D) {
            player.setDeltaMovement(movement.x, movement.y * multiplier, movement.z);
        }
    }

    @SubscribeEvent
    public static void onGhrothHvkTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (GhrothTaczEnhancement.isEnhancedGun(stack)) {
            event.getToolTip().add(Component.translatable("tooltip.dealt_force_skills.ghroth.hvk_gun_core")
                    .withStyle(ChatFormatting.GOLD));
            if (GhrothTaczEnhancement.gunId(stack).map(GhrothTaczEnhancement::isLikelySniper).orElse(false)) {
                event.getToolTip().add(Component.translatable("tooltip.dealt_force_skills.ghroth.hvk_gun_sniper")
                        .withStyle(ChatFormatting.GOLD));
            }
            event.getToolTip().add(Component.translatable("tooltip.dealt_force_skills.ghroth.hvk_gun_ballistics")
                    .withStyle(ChatFormatting.AQUA));
            event.getToolTip().add(Component.translatable("tooltip.dealt_force_skills.ghroth.hvk_gun_handling")
                    .withStyle(ChatFormatting.AQUA));
            event.getToolTip().add(Component.translatable("tooltip.dealt_force_skills.ghroth.hvk_gun_refit")
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        } else if (GhrothTaczEnhancement.isEnhancedAttachment(stack)) {
            event.getToolTip().add(Component.translatable("tooltip.dealt_force_skills.ghroth.hvk_attachment")
                    .withStyle(ChatFormatting.GOLD));
        }
        if (!stack.isEmpty() && ModGameRules.isHavocValueVisible(Minecraft.getInstance().level)) {
            event.getToolTip().add(Component.translatable("tooltip.dealt_force_skills.havoc_value",
                    DealtItemsHavocConfig.tooltipPrice(stack)).withStyle(ChatFormatting.GOLD));
        }
    }

    public static void suppressLocalHurtAnimation(int ticks) {
        suppressHurtAnimationTicks = Math.max(suppressHurtAnimationTicks, Math.max(1, ticks));
    }

    private static void tickSuppressHurtAnimation(net.minecraft.client.player.LocalPlayer player) {
        if (suppressHurtAnimationTicks <= 0) {
            return;
        }
        player.hurtTime = 0;
        player.hurtDuration = 0;
        suppressHurtAnimationTicks--;
    }

    private static boolean superbWarfareShockBlocked(LocalPlayer player) {
        MobEffectInstance sonic = player.getEffect(ModEffects.SONIC_SHOCK.get());
        if (sonic != null && Math.floorMod(player.tickCount, sonic.getAmplifier() >= 1 ? 4 : 2) != 0) {
            return true;
        }
        MobEffectInstance morse = player.getEffect(ModEffects.MORSE_STRONG_SHOCK.get());
        return morse != null && Math.floorMod(player.tickCount, morse.getAmplifier() >= 1 ? 4 : 2) != 0;
    }

    private static void accelerateTaczAdrenalineClient(net.minecraft.client.player.LocalPlayer player) {
        TaczSpeedMultipliers multipliers = taczSpeedMultipliers(player);
        if (!multipliers.hasChange()) {
            return;
        }
        try {
            Class<?> clientOperatorClass = Class.forName(TACZ_CLIENT_GUN_OPERATOR);
            Method fromLocalPlayer = clientOperatorClass.getMethod("fromLocalPlayer", net.minecraft.client.player.LocalPlayer.class);
            Object operator = fromLocalPlayer.invoke(null, player);
            Object dataHolder = clientOperatorClass.getMethod("getDataHolder").invoke(operator);
            long fireExtraMillis = extraTaczMillis(multipliers.fireRate());
            long reloadExtraMillis = extraTaczMillis(multipliers.reload());
            long aimExtraMillis = extraTaczMillis(multipliers.aim());
            long boltExtraMillis = extraTaczMillis(multipliers.bolt());
            long aimPenaltyMillis = extraTaczMillis(multipliers.aimPenalty());
            shiftLongFieldIfNonNegative(dataHolder, "clientShootTimestamp", -fireExtraMillis);
            shiftLongFieldIfNonNegative(dataHolder, "clientLastShootTimestamp", -fireExtraMillis);
            shiftLongFieldIfNonNegative(dataHolder, "lockTimestamp", -Math.max(fireExtraMillis, Math.max(reloadExtraMillis, boltExtraMillis)));
            if (booleanField(dataHolder, "isBolting")) {
                shiftLongFieldIfNonNegative(dataHolder, "boltTimestamp", -boltExtraMillis);
            }
            if (booleanField(dataHolder, "clientIsAiming")) {
                shiftLongFieldIfNonNegative(dataHolder, "clientAimingTimestamp", -aimExtraMillis + aimPenaltyMillis);
                boostFloatField(dataHolder, "clientAimingProgress", adrenalineAimProgressBoost(multipliers.aim()), 0.0F, 1.0F);
                reduceFloatField(dataHolder, "clientAimingProgress", taczAimPenaltyProgress(multipliers.aimPenalty()), 0.0F, 1.0F);
            }
            if (booleanField(dataHolder, "isCharging")) {
                boostFloatField(dataHolder, "chargeProgress", adrenalineChargeProgressBoost(Math.max(multipliers.fireRate(), multipliers.aim())), 0.0F, Float.MAX_VALUE);
            }
            setBooleanFieldIfPresent(dataHolder, "isShootRecorded", true);
        } catch (ReflectiveOperationException | LinkageError ignored) {
            // TaCZ is optional; keep the bridge best-effort and non-fatal.
        }
    }

    private static float adrenalineAimProgressBoost(double multiplier) {
        return (float) Math.min(0.5D, Math.max(0.0D, 0.08D * (multiplier - 1.0D)));
    }

    private static float adrenalineChargeProgressBoost(double multiplier) {
        return (float) Math.max(0.0D, 0.2D * (multiplier - 1.0D));
    }

    private static TaczSpeedMultipliers taczSpeedMultipliers(net.minecraft.client.player.LocalPlayer player) {
        double adrenaline = ToxikStateManager.adrenalineSpeedMultiplier(player);
        double adrenalineFireRate = ToxikStateManager.adrenalineFireRateSpeedMultiplier(player);
        double equipment = equipmentTaczAssaultMultiplier(player);
        double ghrothFireRate = ghrothClientFireRateMultiplier(player);
        double ghrothReload = ghrothClientReloadMultiplier(player);
        double ghrothAim = ghrothClientAimMultiplier(player);
        double ghrothBolt = ghrothClientBoltMultiplier(player);
        return new TaczSpeedMultipliers(
                adrenalineFireRate * equipment * ghrothFireRate * ModItemEffectHelper.medicineTaczFireRateMultiplier(player),
                adrenaline * ghrothReload * ModItemEffectHelper.medicineTaczReloadMultiplier(player),
                adrenaline * equipment * ghrothAim * ModItemEffectHelper.medicineTaczAimSpeedMultiplier(player),
                adrenaline * ghrothBolt,
                ModItemEffectHelper.medicineTaczAimPenaltyMultiplier(player)
        );
    }

    public static double effectiveTaczClientFireRateMultiplier(LocalPlayer player) {
        return taczSpeedMultipliers(player).fireRate();
    }

    public static double effectiveTaczClientAimMultiplier(LocalPlayer player) {
        TaczSpeedMultipliers multipliers = taczSpeedMultipliers(player);
        return multipliers.aim() / multipliers.aimPenalty();
    }

    private static double ghrothClientFireRateMultiplier(net.minecraft.client.player.LocalPlayer player) {
        if (!ModCharacters.GHROTH_ID.equals(ClientCharacterSelectionState.selectedCharacterId())) {
            return 1.0D;
        }
        return isGhrothEnhancedGun(player.getMainHandItem()) ? 2.0D : 1.0D;
    }

    private static double ghrothClientReloadMultiplier(net.minecraft.client.player.LocalPlayer player) {
        if (!ModCharacters.GHROTH_ID.equals(ClientCharacterSelectionState.selectedCharacterId())) {
            return 1.0D;
        }
        return isGhrothEnhancedGun(player.getMainHandItem()) ? 2.0D : 1.0D;
    }

    private static double ghrothClientAimMultiplier(net.minecraft.client.player.LocalPlayer player) {
        if (!ModCharacters.GHROTH_ID.equals(ClientCharacterSelectionState.selectedCharacterId())) {
            return 1.0D;
        }
        return isGhrothEnhancedGun(player.getMainHandItem()) ? 16.0D : 1.0D;
    }

    private static double ghrothClientBoltMultiplier(net.minecraft.client.player.LocalPlayer player) {
        if (!ModCharacters.GHROTH_ID.equals(ClientCharacterSelectionState.selectedCharacterId())) {
            return 1.0D;
        }
        return isGhrothEnhancedGun(player.getMainHandItem()) ? 4.0D : 1.0D;
    }

    private static boolean isGhrothEnhancedGun(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.getBoolean(GhrothTaczEnhancement.ENHANCED_TAG);
    }

    private static double equipmentTaczAssaultMultiplier(net.minecraft.client.player.LocalPlayer player) {
        if (!ModGameRules.areArmorSpecialsEnabled(player)) {
            return 1.0D;
        }
        double multiplier = 1.0D;
        long now = player.level().getGameTime();
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        Profile chestProfile = DfsEquipmentItem.profile(chest);
        if (chestProfile != null && chestProfile.ability() == SpecialAbility.TRICK_ASSAULT
                && now <= ItemStackTagReads.getLong(chest, TRICK_UNTIL)) {
            multiplier *= 1.0D + ItemStackTagReads.getInt(chest, TRICK_STACKS) * 0.04D;
        }
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        Profile headProfile = DfsEquipmentItem.profile(head);
        if (headProfile != null && headProfile.ability() == SpecialAbility.DICH9_ASSAULT
                && now <= ItemStackTagReads.getLong(head, DICH9_UNTIL)) {
            multiplier *= 1.0D + ItemStackTagReads.getInt(head, DICH9_STACKS) * 0.06D;
        }
        if (isMhsFuryActive(player)) {
            multiplier *= 2.0D;
        }
        return multiplier;
    }

    private static long extraTaczMillis(double multiplier) {
        return multiplier <= 1.0001D ? 0L : Math.max(1L, Math.round(50.0D * (multiplier - 1.0D)));
    }

    private static float taczAimPenaltyProgress(double multiplier) {
        return multiplier <= 1.0001D ? 0.0F : (float) Math.min(0.8D, 0.08D * (multiplier - 1.0D));
    }

    private record TaczSpeedMultipliers(double fireRate, double reload, double aim, double bolt, double aimPenalty) {
        private TaczSpeedMultipliers {
            fireRate = validMultiplier(fireRate);
            reload = validMultiplier(reload);
            aim = validMultiplier(aim);
            bolt = validMultiplier(bolt);
            aimPenalty = validMultiplier(aimPenalty);
        }

        private static double validMultiplier(double value) {
            return Double.isFinite(value) && value >= 1.0D ? value : 1.0D;
        }

        boolean hasChange() {
            return fireRate > 1.0001D || reload > 1.0001D || aim > 1.0001D || bolt > 1.0001D || aimPenalty > 1.0001D;
        }
    }

    private static void delayTaczMorseShockClient(net.minecraft.client.player.LocalPlayer player) {
        MobEffectInstance shock = player.getEffect(ModEffects.MORSE_STRONG_SHOCK.get());
        if (shock == null) {
            return;
        }
        long extraMillis = 35L * (shock.getAmplifier() + 1L);
        try {
            Class<?> clientOperatorClass = Class.forName(TACZ_CLIENT_GUN_OPERATOR);
            Method fromLocalPlayer = clientOperatorClass.getMethod("fromLocalPlayer", net.minecraft.client.player.LocalPlayer.class);
            Object operator = fromLocalPlayer.invoke(null, player);
            Object dataHolder = clientOperatorClass.getMethod("getDataHolder").invoke(operator);
            shiftLongFieldIfNonNegative(dataHolder, "clientShootTimestamp", extraMillis);
            shiftLongFieldIfNonNegative(dataHolder, "clientLastShootTimestamp", extraMillis);
            shiftLongFieldIfNonNegative(dataHolder, "lockTimestamp", extraMillis);
            if (booleanField(dataHolder, "clientIsAiming")) {
                shiftLongFieldIfNonNegative(dataHolder, "clientAimingTimestamp", extraMillis);
                reduceFloatField(dataHolder, "clientAimingProgress", 0.04F * (shock.getAmplifier() + 1), 0.0F, 1.0F);
            }
        } catch (ReflectiveOperationException | LinkageError ignored) {
            // TaCZ is optional; strong shock still applies its overlay and server movement penalties.
        }
    }

    private static void shiftLongFieldIfNonNegative(Object target, String fieldName, long delta) throws ReflectiveOperationException {
        if (target == null) {
            return;
        }
        Field field = fieldIfPresent(target, fieldName);
        if (field == null || field.getType() != long.class) {
            return;
        }
        long value = field.getLong(target);
        if (value >= 0L) {
            field.setLong(target, value + delta);
        }
    }

    private static boolean booleanField(Object target, String fieldName) throws ReflectiveOperationException {
        if (target == null) {
            return false;
        }
        Field field = fieldIfPresent(target, fieldName);
        return field != null && field.getType() == boolean.class && field.getBoolean(target);
    }

    private static float floatField(Object target, String fieldName) throws ReflectiveOperationException {
        if (target == null) {
            return 0.0F;
        }
        Field field = fieldIfPresent(target, fieldName);
        return field != null && field.getType() == float.class ? field.getFloat(target) : 0.0F;
    }

    private static void setBooleanFieldIfPresent(Object target, String fieldName, boolean value) throws ReflectiveOperationException {
        if (target == null) {
            return;
        }
        Field field = fieldIfPresent(target, fieldName);
        if (field != null && field.getType() == boolean.class) {
            field.setBoolean(target, value);
        }
    }

    private static void boostFloatField(Object target, String fieldName, float delta, float min, float max) throws ReflectiveOperationException {
        if (target == null || delta <= 0.0F) {
            return;
        }
        Field field = fieldIfPresent(target, fieldName);
        if (field == null || field.getType() != float.class) {
            return;
        }
        float value = field.getFloat(target);
        if (!Float.isFinite(value)) {
            return;
        }
        field.setFloat(target, Math.max(min, Math.min(max, value + delta)));
    }

    private static void reduceFloatField(Object target, String fieldName, float delta, float min, float max) throws ReflectiveOperationException {
        if (target == null || delta <= 0.0F) {
            return;
        }
        Field field = fieldIfPresent(target, fieldName);
        if (field == null || field.getType() != float.class) {
            return;
        }
        float value = field.getFloat(target);
        if (!Float.isFinite(value)) {
            return;
        }
        field.setFloat(target, Math.max(min, Math.min(max, value - delta)));
    }

    private static Field fieldIfPresent(Object target, String fieldName) {
        Class<?> type = target.getClass();
        while (type != null) {
            try {
                Field field = type.getField(fieldName);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            }
        }
        return null;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            localDeathInputOverride = true;
            forcedCharacterSpinActive = false;
            ClientToolReleaseAction.reset();
            ClientCharacterSelectionState.resetSession();
            SinevaPlaceholderVisuals.resetTransientState();
            com.rzy.dealt_force_skills.client.visual.UndeadPlayerVisuals.resetTransientState();
            DWolfSlideVisuals.reset();
            return;
        }
        localDeathInputOverride = !mc.player.isAlive();
        if (localDeathInputOverride) {
            resetGameplayInputAfterDeath(mc);
            return;
        }
        localDeathInputOverride = false;
        if (mc.player.isSpectator()) {
            forcedCharacterSpinActive = false;
            ClientToolReleaseAction.reset();
            UluruMissileController.stop(false);
            RaptorFalconController.stop(false);
            SaeedGuardViewController.stop(false);
            return;
        }

        ClientCharacterSelectionState.openInitialSelectionIfNeeded();
        ClientToolReleaseAction.tick();
        reduceAdrenalinePlacementDelay(mc);
        tickMask1LockOn(mc);
        tickGhrothLockOn(mc);
        tickForcedCharacterSpin(mc.player);
        ClientSinevaHudState.tick();
        ClientUluruHudState.tick();
        ClientDWolfHudState.tick();
        ClientGizmoHudState.tick();
        ClientChamberHudState.tick();
        ClientShepherdHudState.tick();
        ClientNTwoHudState.tick();
        ClientLunaHudState.tick();
        ClientHackclawHudState.tick();
        ClientVyronHudState.tick();
        ClientStingerHudState.tick();
        ClientNoxHudState.tick();
        ClientManbaHudState.tick();
        ClientNikaidouHiroHudState.tick();
        ClientCatDadHudState.tick();
        ClientCorpsHudState.tick();
        ClientDepartmentHudState.tick();
        ClientUndeadHudState.tick();
        ClientLexNinjiaHudState.tick();
        ClientGamblerHudState.tick();
        ClientMorseHudState.tick();
        ClientToxikHudState.tick();
        ClientRaptorHudState.tick();
        ClientVlinderHudState.tick();
        ClientTempestHudState.tick();
        ClientSaeedHudState.tick();
        ManbaFlashlightBeamRenderer.tick(mc);
        ClientSinevaRenderState.tick(mc);
        ClientHackclawCoreVisualState.tick(mc);
        ClientHeldToolVisualState.tick(mc);
        ClientLunaBowVisualState.tick(mc);
        ClientSkillModelVisualState.tick(mc);
        ClientSinevaVisualState.tick(mc);
        ClientSinevaKnockdownState.tick(mc);
        SinevaShieldInputHandler.tick(mc);
        SinevaInputHandler.tick(mc);
        UluruInputHandler.tick(mc);
        DWolfInputHandler.tick(mc);
        GizmoInputHandler.tick(mc);
        ChamberInputHandler.tick(mc);
        CorpsInputHandler.tick(mc);
        ShepherdInputHandler.tick(mc);
        NTwoInputHandler.tick(mc);
        LunaInputHandler.tick(mc);
        HackclawInputHandler.tick(mc);
        VyronInputHandler.tick(mc);
        StingerInputHandler.tick(mc);
        RoverClient.tick(mc);
        NoxInputHandler.tick(mc);
        ManbaInputHandler.tick(mc);
        NikaidouHiroInputHandler.tick(mc);
        DepartmentInputHandler.tick(mc);
        UndeadInputHandler.tick(mc);
        LexNinjiaInputHandler.tick(mc);
        GamblerInputHandler.tick(mc);
        MorseInputHandler.tick(mc);
        ToxikInputHandler.tick(mc);
        RaptorInputHandler.tick(mc);
        VlinderInputHandler.tick(mc);
        TempestInputHandler.tick(mc);
        SaeedInputHandler.tick(mc);
        GhrothInputHandler.tick(mc);
        CommanderInputHandler.tick(mc);
        SpecialOperationsBaseInputHandler.tick(mc);
        SaeedGuardViewController.tick(mc);
        UluruMissileController.tick(mc);
        RaptorFalconController.tick(mc);
        UluruGhostEntityManager.tick(mc);
        DWolfSlideVisuals.tick();
        if (mc.player.hasEffect(ModEffects.ROVER_DOWNED.get()) || mc.player.hasEffect(ModEffects.STINGER_DOWNED.get())
                || mc.player.hasEffect(ModEffects.VLINDER_VITAL_DOWNED.get())
                || mc.player.hasEffect(ModEffects.TEMPEST_EMERGENCY_DOWNED.get())
                || mc.player.hasEffect(ModEffects.TEMPEST_DISARMED.get())
                || ClientCatDadHudState.actionLocked()
                || ClientTempestHudState.recalling()) {
            drainDownedLockedKeys(mc);
            return;
        }
        if (UluruMissileController.isControlling()) {
            drainMissileControlLockedKeys(mc);
            return;
        }
        if (RaptorFalconController.isControlling()) {
            drainMissileControlLockedKeys(mc);
            return;
        }
        if (SaeedGuardViewController.isControlling()) {
            drainMissileControlLockedKeys(mc);
            return;
        }

        // Unified tool-equipped key drain: when any character has a pseudo-tool active,
        // suppress vanilla attack/use/pick to prevent accidental interactions leaking through.
        if (isAnyClientToolActive()) {
            drainKey(mc.options.keyAttack);
            drainKey(mc.options.keyUse);
            drainKey(mc.options.keyPickItem);
            drainKey(mc.options.keyDrop);
            drainKey(mc.options.keySwapOffhand);
        }

        while (!UndeadInputHandler.ownsSkillKeys()
                && !LexNinjiaInputHandler.ownsSkillKeys()
                && !GamblerInputHandler.ownsSkillKeys()
                && KeybindRegister.ACTIVE_SKILL_1 != null
                && KeybindRegister.ACTIVE_SKILL_1.consumeClick()) {
            // Sineva handles ACTIVE_1 exclusively in SinevaInputHandler; other characters use this.
            if (!ClientSinevaHudState.shouldRender() && !ClientStingerHudState.shouldRender()
                    && !ClientNoxHudState.shouldRender() && !ClientManbaHudState.shouldRender()
                    && !ClientHackclawHudState.shouldRender() && !ClientMorseHudState.shouldRender()
                    && !ClientToxikHudState.shouldRender()
                    && !ClientRaptorHudState.shouldRender()
                    && !ClientVlinderHudState.shouldRender()
                    && !ClientTempestHudState.shouldRender()) {
                ClientCharacterSelectionState.useSkill(SkillSlot.ACTIVE_1);
            }
        }

        while (!UndeadInputHandler.ownsSkillKeys()
                && !LexNinjiaInputHandler.ownsSkillKeys()
                && KeybindRegister.ACTIVE_SKILL_2 != null
                && KeybindRegister.ACTIVE_SKILL_2.consumeClick()) {
            if (!ClientVyronHudState.shouldRender() && !ClientStingerHudState.shouldRender()
                    && !ClientNoxHudState.shouldRender() && !ClientManbaHudState.shouldRender()
                    && !ClientHackclawHudState.shouldRender() && !ClientMorseHudState.shouldRender()
                    && !ClientToxikHudState.shouldRender()
                    && !ClientRaptorHudState.shouldRender()
                    && !ClientVlinderHudState.shouldRender()
                    && !ClientTempestHudState.shouldRender()) {
                if (ClientSinevaHudState.shouldRender()) {
                    SinevaInputHandler.startGrappleUse();
                } else {
                    ClientCharacterSelectionState.useSkill(SkillSlot.ACTIVE_2);
                }
            }
        }

        if (UndeadInputHandler.ownsSkillKeys() || LexNinjiaInputHandler.ownsSkillKeys()
                || RoverClient.ownsCoreSkill() || DWolfInputHandler.ownsCoreSkill() || StingerInputHandler.ownsCoreSkill()
                || NoxInputHandler.ownsCoreSkill() || ManbaInputHandler.ownsCoreSkill()
                || CorpsInputHandler.ownsCoreSkill()
                || MorseInputHandler.ownsCoreSkill()
                || ToxikInputHandler.ownsCoreSkill() || RaptorInputHandler.ownsCoreSkill()
                || VlinderInputHandler.ownsCoreSkill() || TempestInputHandler.ownsCoreSkill()
                || CommanderInputHandler.ownsCoreSkill()) {
            resetCoreSkillKey();
        } else {
            handleCoreSkillKey();
        }
    }

    /**
     * Pseudo-tools are normally allowed to own gameplay input, but never while the
     * local player is dead. Keep their transient camera/control state from ticking
     * and drain custom mappings so the death/title screens receive vanilla input.
     */
    private static void resetGameplayInputAfterDeath(Minecraft minecraft) {
        ClientToolReleaseAction.reset();
        ClientGamblerTargetingState.clearAll();
        UluruMissileController.stop(false);
        RaptorFalconController.stop(false);
        SaeedGuardViewController.stop(false);
        resetCoreSkillKey();
        drainKey(KeybindRegister.CHARACTER_SELECT);
        drainKey(KeybindRegister.ACTIVE_SKILL_1);
        drainKey(KeybindRegister.ACTIVE_SKILL_2);
        drainKey(KeybindRegister.CORE_SKILL);
    }

    private static boolean localDeathInputIsOverridden() {
        Minecraft minecraft = Minecraft.getInstance();
        return localDeathInputOverride
                || (minecraft.player != null && !minecraft.player.isAlive());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void restoreVanillaInputAfterDeath(InputEvent.MouseButton.Pre event) {
        if (localDeathInputIsOverridden()) {
            event.setCanceled(false);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void restoreVanillaInputAfterDeath(InputEvent.MouseScrollingEvent event) {
        if (localDeathInputIsOverridden()) {
            event.setCanceled(false);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void restoreVanillaInputAfterDeath(InputEvent.InteractionKeyMappingTriggered event) {
        if (localDeathInputIsOverridden()) {
            event.setCanceled(false);
            event.setSwingHand(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void restoreVanillaScreenAfterDeath(ScreenEvent.Opening event) {
        if (localDeathInputIsOverridden()) {
            event.setCanceled(false);
        }
    }

    private static void tickForcedCharacterSpin(LocalPlayer player) {
        float degrees = 0.0F;
        if (ClientDepartmentHudState.shouldRender()) {
            if (ClientDepartmentHudState.coreCountdownTicks() > 0) {
                degrees = 18.0F;
            } else if (ClientDepartmentHudState.coreAscendTicks() > 0) {
                degrees = 22.0F;
            }
        } else if (ClientUndeadHudState.shouldRender() && ClientUndeadHudState.hunterScatter()) {
            degrees = 18.0F;
        }

        if (degrees <= 0.0F) {
            forcedCharacterSpinActive = false;
            return;
        }
        if (!forcedCharacterSpinActive) {
            forcedCharacterSpinYaw = player.getYRot();
            forcedCharacterSpinActive = true;
        }
        forcedCharacterSpinYaw = Mth.wrapDegrees(forcedCharacterSpinYaw + degrees);
        player.setYHeadRot(forcedCharacterSpinYaw);
        player.setYBodyRot(forcedCharacterSpinYaw);
    }

    private static void tickMask1LockOn(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || player.isSpectator()
                || !ModGameRules.areArmorSpecialsEnabled(player)) {
            return;
        }
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        Profile headProfile = DfsEquipmentItem.profile(head);
        if (headProfile == null || headProfile.ability() != SpecialAbility.MASK_LOCK_ON) {
            return;
        }
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        Profile chestProfile = DfsEquipmentItem.profile(chest);
        if (chestProfile == null || !chestProfile.id().equals("heavy_assault_vest")) {
            return;
        }
        if (!isClientScopedWeapon(player)) {
            return;
        }
        LivingEntity target = nearestMask1LockTarget(player);
        if (target != null) {
            SuperbWarfareCompat.applyClientScopedTargetLock(player, target);
            aimLocalPlayerAt(player, target.getEyePosition());
        }
    }

    private static void tickGhrothLockOn(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || player.isSpectator()
                || !ModCharacters.GHROTH_ID.equals(ClientCharacterSelectionState.selectedCharacterId())) {
            return;
        }
        if (!isClientScopedWeapon(player)) {
            return;
        }
        LivingEntity target = player.isShiftKeyDown() ? nearestGhrothPlayerTarget(player) : nearestGhrothNonPlayerTarget(player);
        if (target != null) {
            SuperbWarfareCompat.applyClientScopedTargetLock(player, target);
            aimLocalPlayerAt(player, target.getEyePosition());
        }
    }

    private static boolean isClientScopedWeapon(LocalPlayer player) {
        return (isTaczGunStack(player.getMainHandItem()) && isTaczClientAiming(player))
                || SuperbWarfareCompat.isClientAiming(player);
    }

    private static LivingEntity nearestMask1LockTarget(LocalPlayer player) {
        return player.isShiftKeyDown() ? nearestMask1AttackableTarget(player) : nearestMask1PlayerTarget(player);
    }

    private static Player nearestGhrothPlayerTarget(LocalPlayer player) {
        return nearestMask1PlayerTarget(player);
    }

    private static LivingEntity nearestGhrothNonPlayerTarget(LocalPlayer player) {
        LivingEntity nearest = null;
        double bestDistance = MASK1_LOCK_RANGE * MASK1_LOCK_RANGE;
        for (LivingEntity candidate : player.level().getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(MASK1_LOCK_RANGE),
                entity -> entity != player && !(entity instanceof Player) && entity.isAlive() && !entity.isSpectator()
                        && entity.isAttackable() && entity.attackable())) {
            double distance = candidate.distanceToSqr(player);
            if (distance < bestDistance) {
                nearest = candidate;
                bestDistance = distance;
            }
        }
        return nearest;
    }

    private static Player nearestMask1PlayerTarget(LocalPlayer player) {
        Player nearest = null;
        double bestDistance = MASK1_LOCK_RANGE * MASK1_LOCK_RANGE;
        for (Player candidate : player.level().players()) {
            if (candidate == player || !candidate.isAlive() || candidate.isSpectator()) {
                continue;
            }
            double distance = candidate.distanceToSqr(player);
            if (distance < bestDistance) {
                nearest = candidate;
                bestDistance = distance;
            }
        }
        return nearest;
    }

    private static LivingEntity nearestMask1AttackableTarget(LocalPlayer player) {
        LivingEntity nearest = null;
        double bestDistance = MASK1_LOCK_RANGE * MASK1_LOCK_RANGE;
        for (LivingEntity candidate : player.level().getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(MASK1_LOCK_RANGE),
                entity -> entity != player && !TargetingUtil.isPlayerOwnedSummonOf(player, entity)
                        && entity.isAlive() && !entity.isSpectator()
                        && entity.isAttackable() && entity.attackable())) {
            double distance = candidate.distanceToSqr(player);
            if (distance < bestDistance) {
                nearest = candidate;
                bestDistance = distance;
            }
        }
        return nearest;
    }

    private static void aimLocalPlayerAt(LocalPlayer player, Vec3 target) {
        Vec3 eye = player.getEyePosition();
        double dx = target.x - eye.x;
        double dy = target.y - eye.y;
        double dz = target.z - eye.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        if (horizontal < 0.0001D) {
            return;
        }
        float yaw = (float) (Mth.atan2(dz, dx) * (180F / Math.PI)) - 90.0F;
        float pitch = (float) -(Mth.atan2(dy, horizontal) * (180F / Math.PI));
        float smoothedYaw = Mth.rotLerp(0.65F, player.getYRot(), yaw);
        float smoothedPitch = Mth.lerp(0.65F, player.getXRot(), Mth.clamp(pitch, -89.0F, 89.0F));
        player.setYRot(smoothedYaw);
        player.setXRot(smoothedPitch);
        player.setYHeadRot(smoothedYaw);
        player.setYBodyRot(smoothedYaw);
    }

    private static boolean isTaczClientAiming(LocalPlayer player) {
        try {
            Class<?> clientOperatorClass = Class.forName(TACZ_CLIENT_GUN_OPERATOR);
            Method fromLocalPlayer = clientOperatorClass.getMethod("fromLocalPlayer", LocalPlayer.class);
            Object operator = fromLocalPlayer.invoke(null, player);
            try {
                Object result = clientOperatorClass.getMethod("isAim").invoke(operator);
                if (result instanceof Boolean aiming && aiming) {
                    return true;
                }
            } catch (ReflectiveOperationException ignored) {
                // Older or changed TACZ builds may expose only data-holder fields.
            }
            Object dataHolder = clientOperatorClass.getMethod("getDataHolder").invoke(operator);
            return booleanField(dataHolder, "clientIsAiming")
                    || booleanField(dataHolder, "isAiming")
                    || floatField(dataHolder, "clientAimingProgress") > 0.15F
                    || floatField(dataHolder, "aimingProgress") > 0.15F;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return false;
        }
    }

    private static boolean isMhsFuryActive(LocalPlayer player) {
        if (!ModGameRules.areArmorSpecialsEnabled(player)) {
            return false;
        }
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        Profile headProfile = DfsEquipmentItem.profile(head);
        CompoundTag tag = head.getTag();
        return headProfile != null && headProfile.ability() == SpecialAbility.MHS_FURY
                && tag != null && player.level().getGameTime() <= tag.getLong(MHS_FURY_UNTIL);
    }

    private static boolean isTaczGunStack(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(TACZ_GUN_ID_TAG)) {
            return true;
        }
        return stack.getItem().getClass().getName().startsWith("com.tacz.guns.");
    }


    private static void reduceAdrenalinePlacementDelay(Minecraft minecraft) {
        if (minecraft.player == null || !minecraft.player.hasEffect(ModEffects.TOXIK_ADRENALINE.get())) {
            return;
        }
        Field field = rightClickDelayField();
        if (field == null) {
            return;
        }
        try {
            field.setInt(minecraft, 0);
        } catch (IllegalAccessException ignored) {
        }
    }

    private static Field rightClickDelayField() {
        if (minecraftRightClickDelayFieldChecked) {
            return minecraftRightClickDelayField;
        }
        minecraftRightClickDelayFieldChecked = true;
        for (String name : new String[]{"rightClickDelay", "rightClickDelayTicks", "f_91011_"}) {
            try {
                Field field = Minecraft.class.getDeclaredField(name);
                field.setAccessible(true);
                minecraftRightClickDelayField = field;
                return field;
            } catch (ReflectiveOperationException ignored) {
            }
        }
        return null;
    }

    private static void handleCoreSkillKey() {
        if (KeybindRegister.CORE_SKILL == null) {
            return;
        }

        boolean isDown = KeybindRegister.CORE_SKILL.isDown();
        if (isDown) {
            coreKeyHeldTicks++;
            coreKeyWasDown = true;
            return;
        }

        if (!coreKeyWasDown) {
            return;
        }

        int requiredHoldTicks = ClientShepherdHudState.shouldRender()
                ? SHEPHERD_CORE_LONG_HOLD_TICKS
                : CORE_LONG_HOLD_TICKS;
        boolean longHold = coreKeyHeldTicks >= requiredHoldTicks;
        ClientCharacterSelectionState.useSkill(SkillSlot.CORE, longHold);
        coreKeyHeldTicks = 0;
        coreKeyWasDown = false;
    }

    private static void resetCoreSkillKey() {
        coreKeyHeldTicks = 0;
        coreKeyWasDown = false;
    }

    private static boolean isAnyClientToolActive() {
        if (RoverClient.tool() > 0) return true;
        // Sineva: shield deployed or blade wire held
        if ((ClientSinevaHudState.shouldRender() || ClientCommanderState.inherits(ModCharacters.SINEVA_ID))
                && ClientSinevaHudState.shieldDeployed()) {
            return true;
        }
        if (SinevaInputHandler.isBladeWireHeld()) {
            return true;
        }
        if (SinevaInputHandler.isGrappleCharging()) {
            return true;
        }
        // Uluru: any tool equipped
        if (ClientUluruHudState.hasEquippedTool()) {
            return true;
        }
        // D-Wolf: hand cannon equipped
        if (ClientDWolfHudState.hasHandCannonEquipped()) {
            return true;
        }
        // Gizmo: any trap/T-boy pseudo-tool equipped
        if (ClientGizmoHudState.hasEquippedTool()) {
            return true;
        }
        if (ClientChamberHudState.hasEquippedTool()) {
            return true;
        }
        // Shepherd: sonic trap or frag grenade pseudo-tool equipped
        if (ClientShepherdHudState.hasEquippedTool()) {
            return true;
        }
        if (ClientNTwoHudState.hasEquippedTool()) {
            return true;
        }
        if (ClientLunaHudState.hasEquippedTool()) {
            return true;
        }
        if (ClientHackclawHudState.hasEquippedTool()) {
            return true;
        }
        if (ClientVyronHudState.hasEquippedTool()) {
            return true;
        }
        if (ClientStingerHudState.hasEquippedTool()) {
            return true;
        }
        if (ClientNoxHudState.hasEquippedTool()) {
            return true;
        }
        if (ClientManbaHudState.flashlightActive()) {
            return true;
        }
        if (ClientNikaidouHiroHudState.hasEquippedTool()) {
            return true;
        }
        if (ClientCatDadHudState.actionLocked()) {
            return true;
        }
        if (ClientDepartmentHudState.hasEquippedTool()) {
            return true;
        }
        if (ClientMorseHudState.hasEquippedTool()) {
            return true;
        }
        if (ClientToxikHudState.hasEquippedTool()) {
            return true;
        }
        if (ClientRaptorHudState.hasEquippedTool()) {
            return true;
        }
        if (ClientVlinderHudState.hasEquippedTool()) {
            return true;
        }
        if (ClientTempestHudState.hasEquippedTool()) {
            return true;
        }
        if (ClientSaeedHudState.hasCrossbowEquipped()) {
            return true;
        }
        return false;
    }

    private static void drainDownedLockedKeys(Minecraft minecraft) {
        drainKey(KeybindRegister.CHARACTER_SELECT);
        drainKey(KeybindRegister.ACTIVE_SKILL_1);
        drainKey(KeybindRegister.ACTIVE_SKILL_2);
        drainKey(KeybindRegister.CORE_SKILL);
        drainKey(minecraft.options.keyAttack);
        drainKey(minecraft.options.keyUse);
        drainKey(minecraft.options.keyPickItem);
        drainKey(minecraft.options.keyDrop);
        drainKey(minecraft.options.keySwapOffhand);
        drainKey(minecraft.options.keyInventory);
        drainKey(minecraft.options.keyJump);
        coreKeyHeldTicks = 0;
        coreKeyWasDown = false;
    }

    private static void drainMissileControlLockedKeys(Minecraft minecraft) {
        drainKey(KeybindRegister.CHARACTER_SELECT);
        drainKey(KeybindRegister.ACTIVE_SKILL_1);
        drainKey(KeybindRegister.ACTIVE_SKILL_2);
        drainKey(KeybindRegister.CORE_SKILL);
        drainKey(minecraft.options.keyAttack);
        drainKey(minecraft.options.keyUse);
        drainKey(minecraft.options.keyPickItem);
        drainKey(minecraft.options.keyDrop);
        drainKey(minecraft.options.keySwapOffhand);
        drainKey(minecraft.options.keyInventory);
        coreKeyHeldTicks = 0;
        coreKeyWasDown = false;
    }

    private static void drainKey(KeyMapping keyMapping) {
        if (keyMapping == null) {
            return;
        }
        keyMapping.setDown(false);
        while (keyMapping.consumeClick()) {
            // Suppress all gameplay actions while the guided missile owns control.
        }
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        UluruGhostEntityManager.render(event);
    }

    @Mod.EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModBusClient {
        @SubscribeEvent
        public static void registerParticleProviders(RegisterParticleProvidersEvent e) {
            e.registerSpriteSet(ModParticles.D_WOLF_LARGE_SMOKE.get(),
                    sprites -> (type, level, x, y, z, xs, ys, zs) ->
                            new LargeSmokeParticle(level, x, y, z, xs, ys, zs, sprites));
            e.registerSpriteSet(ModParticles.GIZMO_LARGE_SMOKE.get(),
                    sprites -> (type, level, x, y, z, xs, ys, zs) ->
                            new GizmoLargeSmokeParticle(level, x, y, z, xs, ys, zs, sprites));
            e.registerSpriteSet(ModParticles.STINGER_LARGE_SMOKE.get(),
                    sprites -> (type, level, x, y, z, xs, ys, zs) ->
                            new StingerLargeSmokeParticle(level, x, y, z, xs, ys, zs, sprites));
            e.registerSpriteSet(ModParticles.STINGER_HEALING_SMOKE.get(),
                    sprites -> (type, level, x, y, z, xs, ys, zs) ->
                            new StingerHealingSmokeParticle(level, x, y, z, xs, ys, zs, sprites));
            e.registerSpriteSet(ModParticles.TOXIK_LARGE_SMOKE.get(),
                    sprites -> (type, level, x, y, z, xs, ys, zs) ->
                            new ToxikLargeSmokeParticle(level, x, y, z, xs, ys, zs, sprites));
        }

        @SubscribeEvent
        public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions e) {
            e.registerLayerDefinition(BeaconBossModel.LAYER_LOCATION, BeaconBossModel::createBodyLayer);
        }

        @SubscribeEvent
        public static void registerRenderers(EntityRenderersEvent.RegisterRenderers e) {
            e.registerEntityRenderer(ModEntities.HVK_ZIPLINE_SEAT.get(), net.minecraft.client.renderer.entity.NoopRenderer::new);
            e.registerBlockEntityRenderer(ModBlockEntities.BLADE_WIRE.get(), BladeWireBlockEntityRenderer::new);
            e.registerEntityRenderer(ModEntities.GRAPPLE_HOOK.get(), GrappleHookRenderer::new);
            e.registerEntityRenderer(ModEntities.BLADE_WIRE_PROJECTILE.get(), ctx -> new BlockbenchProjectileRenderer<>(
                    ctx, model("sineva_blade_wire_core"), null, null, 0.8F,
                    BlockbenchProjectileRenderer.Alignment.NONE));
            e.registerEntityRenderer(ModEntities.ULURU_INCENDIARY_GRENADE.get(), ctx -> new BlockbenchProjectileRenderer<>(
                    ctx, model("uluru_incendiary_grenade"), "arm", null, 0.75F,
                    BlockbenchProjectileRenderer.Alignment.MOTION_X));
            e.registerEntityRenderer(ModEntities.ULURU_FIRE_FIELD.get(), ctx -> new BlockbenchProjectileRenderer<>(
                    ctx, model("uluru_incendiary_grenade"), null, null, 0.75F,
                    BlockbenchProjectileRenderer.Alignment.NONE));
            e.registerEntityRenderer(ModEntities.ULURU_QUICK_COVER_PACKAGE.get(), ctx -> new BlockbenchProjectileRenderer<>(
                    ctx, model("uluru_quick_cover_package"), "arm", null, 0.75F,
                    BlockbenchProjectileRenderer.Alignment.NONE));
            e.registerEntityRenderer(ModEntities.ULURU_LOITERING_MISSILE.get(), ctx -> new BlockbenchProjectileRenderer<>(
                    ctx, model("uluru_loitering_missile"), "deploy", "flight", 0.5F,
                    BlockbenchProjectileRenderer.Alignment.MOTION_X));
            e.registerEntityRenderer(ModEntities.ULURU_BOMBLET.get(), ctx -> new BlockbenchProjectileRenderer<>(
                    ctx, model("uluru_bomblet"), "arm", "flight", 0.75F,
                    BlockbenchProjectileRenderer.Alignment.MOTION_X));
            e.registerEntityRenderer(ModEntities.D_WOLF_HAND_CANNON_GRENADE.get(), DWolfHandCannonGrenadeRenderer::new);
            e.registerEntityRenderer(ModEntities.D_WOLF_SMOKE_GRENADE.get(), ctx -> new BlockbenchProjectileRenderer<>(
                    ctx, model("d_wolf_smoke_grenade"), null, "flight", 0.85F,
                    BlockbenchProjectileRenderer.Alignment.NONE));
            e.registerEntityRenderer(ModEntities.D_WOLF_SMOKE_CLOUD.get(), ctx -> new SmokeCloudRenderer<>(
                    ctx, model("generic_gray_smoke"), 1.0F));
            e.registerEntityRenderer(ModEntities.GIZMO_SMOKE_TRAP.get(), ctx -> new BlockbenchProjectileRenderer<>(
                    ctx, model("gizmo_smoke_trap"), null, "idle", 0.38F,
                    BlockbenchProjectileRenderer.Alignment.NONE));
            e.registerEntityRenderer(ModEntities.GIZMO_SMOKE_CLOUD.get(), ctx -> new SmokeCloudRenderer<>(
                    ctx, model("generic_gray_smoke"), 1.0F));
            e.registerEntityRenderer(ModEntities.GIZMO_SPIDER_NEST_TRAP.get(), ctx -> new BlockbenchProjectileRenderer<>(
                    ctx, model("gizmo_spider_nest_trap"), null, "idle", 0.72F,
                    BlockbenchProjectileRenderer.Alignment.NONE));
            e.registerEntityRenderer(ModEntities.GIZMO_SPIDERLING.get(), ctx -> new BlockbenchProjectileRenderer<>(
                    ctx, model("gizmo_spiderling"), null, "crawl", 0.5F,
                    BlockbenchProjectileRenderer.Alignment.MOTION_Z));
            e.registerEntityRenderer(ModEntities.GIZMO_T_BOY.get(), ctx -> new BlockbenchProjectileRenderer<>(
                    ctx, model("gizmo_t_boy"), null, "walk", 1.0F,
                    BlockbenchProjectileRenderer.Alignment.MOTION_Z));
            e.registerEntityRenderer(ModEntities.CHAMBER_CARD.get(), ctx -> new ThrownItemRenderer<>(ctx));
            e.registerEntityRenderer(ModEntities.CHAMBER_TELEPORT_ANCHOR.get(), ctx -> new ThrownItemRenderer<>(ctx));
            e.registerEntityRenderer(ModEntities.CHAMBER_TRAP.get(), ctx -> new ThrownItemRenderer<>(ctx));
            e.registerEntityRenderer(ModEntities.CHAMBER_SLOW_FIELD.get(), ctx -> new ThrownItemRenderer<>(ctx));
            e.registerEntityRenderer(ModEntities.SHEPHERD_SONIC_TRAP.get(), ctx -> new BlockbenchProjectileRenderer<>(
                    ctx, model("shepherd_sonic_trap"), null, "idle", 0.38F,
                    BlockbenchProjectileRenderer.Alignment.NONE));
            e.registerEntityRenderer(ModEntities.SHEPHERD_FRAG_GRENADE.get(), ctx -> new BlockbenchProjectileRenderer<>(
                    ctx, model("shared_hand_grenade"), null, "flight_spin", 0.55F,
                    BlockbenchProjectileRenderer.Alignment.NONE));
            e.registerEntityRenderer(ModEntities.SHEPHERD_DRONE.get(), ctx -> new BlockbenchProjectileRenderer<>(
                    ctx, model("shepherd_drone"), null, "idle", 0.9F,
                    BlockbenchProjectileRenderer.Alignment.NONE));
            e.registerEntityRenderer(ModEntities.LUNA_SHOCK_ARROW.get(), ctx -> new BlockbenchProjectileRenderer<>(
                    ctx, model("luna_shock_arrow"), null, "flight_charge", 0.75F,
                    BlockbenchProjectileRenderer.Alignment.MOTION_Z));
            e.registerEntityRenderer(ModEntities.LUNA_COMPOSITE_GRENADE.get(), ctx -> new BlockbenchProjectileRenderer<>(
                    ctx, model("shared_hand_grenade"), null, "flight_spin", 0.55F,
                    BlockbenchProjectileRenderer.Alignment.NONE));
            e.registerEntityRenderer(ModEntities.LUNA_RECON_ARROW.get(), ctx -> new BlockbenchProjectileRenderer<>(
                    ctx, model("luna_recon_arrow"), null, "flight_scan", 0.75F,
                    BlockbenchProjectileRenderer.Alignment.MOTION_Z));
            e.registerEntityRenderer(ModEntities.HACKCLAW_KNIFE.get(), ctx -> new BlockbenchProjectileRenderer<>(
                    ctx, model("hackclaw_knife"), null, "flight_spin", 0.38F,
                    BlockbenchProjectileRenderer.Alignment.MOTION_X));
            e.registerEntityRenderer(ModEntities.HACKCLAW_INTERFERENCE_FIELD.get(), ctx -> new BlockbenchProjectileRenderer<>(
                    ctx, model("hackclaw_knife"), null, "idle_charge", 0.85F,
                    BlockbenchProjectileRenderer.Alignment.NONE));
            e.registerEntityRenderer(ModEntities.HACKCLAW_FLASH_DRONE.get(), ctx -> new BlockbenchProjectileRenderer<>(
                    ctx, model("hackclaw_flash_drone"), "deploy_ready", "flight_ready", 0.85F,
                    BlockbenchProjectileRenderer.Alignment.MOTION_Z));
            e.registerEntityRenderer(ModEntities.VYRON_MAGNETIC_BOMB.get(), VyronMagneticBombRenderer::new);
            e.registerEntityRenderer(ModEntities.VYRON_TIGER_CANNON.get(), VyronTigerCannonProjectileRenderer::new);
            e.registerEntityRenderer(ModEntities.ROVER_PROJECTILE.get(), ctx -> new ThrownItemRenderer<>(ctx));
            e.registerEntityRenderer(ModEntities.ROVER_FIELD.get(), com.rzy.dealt_force_skills.client.renderer.RoverFieldRenderer::new);
            e.registerEntityRenderer(ModEntities.ROVER_WOLF.get(), com.rzy.dealt_force_skills.client.renderer.RoverWolfRenderer::new);
            e.registerEntityRenderer(ModEntities.STINGER_SMOKE_GRENADE.get(), ctx -> new BlockbenchProjectileRenderer<>(
                    ctx, model("stinger_smoke_grenade"), "prime", "idle", 0.85F,
                    BlockbenchProjectileRenderer.Alignment.NONE));
            e.registerEntityRenderer(ModEntities.STINGER_SMOKE_CLOUD.get(), ctx -> new SmokeCloudRenderer<>(
                    ctx, model("generic_gray_smoke"), 1.0F));
            e.registerEntityRenderer(ModEntities.STINGER_SMOKE_DRONE.get(), ctx -> new BlockbenchProjectileRenderer<>(
                    ctx, model("stinger_smoke_drone"), null, "idle", 0.9F,
                    BlockbenchProjectileRenderer.Alignment.NONE));
            e.registerEntityRenderer(ModEntities.STINGER_STIM_PROJECTILE.get(), ctx -> new BlockbenchProjectileRenderer<>(
                    ctx, model("stinger_stim_projectile"), null, "flight_streak", 0.75F,
                    BlockbenchProjectileRenderer.Alignment.MOTION_Z));
            e.registerEntityRenderer(ModEntities.NOX_ROTOR_DRONE.get(), ctx -> new BlockbenchProjectileRenderer<>(
                    ctx, model("nox_rotor_drone"), null, "idle", 0.85F,
                    BlockbenchProjectileRenderer.Alignment.MOTION_Z));
            e.registerEntityRenderer(ModEntities.NOX_FLASH_GRENADE.get(), ctx -> new BlockbenchProjectileRenderer<>(
                    ctx, model("nox_flash_grenade"), "prime_flash", "idle", 0.85F,
                    BlockbenchProjectileRenderer.Alignment.NONE));
            e.registerEntityRenderer(ModEntities.NOX_DECOY.get(), NoxDecoyRenderer::new);
            e.registerEntityRenderer(ModEntities.MORSE_SHOCK_ORB.get(), ctx -> new BlockbenchProjectileRenderer<>(
                    ctx, model("morse_shock_orb"), "throw_spin", "idle_hold", 0.8F,
                    BlockbenchProjectileRenderer.Alignment.NONE));
            e.registerEntityRenderer(ModEntities.MORSE_FLASH_GRENADE.get(), ctx -> new BlockbenchProjectileRenderer<>(
                    ctx, model("morse_flash_grenade"), null, "flight_spin", 0.65F,
                    BlockbenchProjectileRenderer.Alignment.NONE));
            e.registerEntityRenderer(ModEntities.MORSE_SONAR_DETECTOR.get(), ctx -> new BlockbenchProjectileRenderer<>(
                    ctx, model("morse_sonar_detector"), "deploy_ready", "scan_sweep", 1.0F,
                    BlockbenchProjectileRenderer.Alignment.MOTION_Z));
            e.registerEntityRenderer(ModEntities.TOXIK_TEAR_GAS_GRENADE.get(), ctx -> new BlockbenchProjectileRenderer<>(
                    ctx, model("toxik_tear_gas_grenade"), "prime_release", "idle", 0.85F,
                    BlockbenchProjectileRenderer.Alignment.NONE));
            e.registerEntityRenderer(ModEntities.TOXIK_TEAR_GAS_CLOUD.get(), ctx -> new SmokeCloudRenderer<>(
                    ctx, model("generic_blind_smoke"), 1.0F));
            e.registerEntityRenderer(ModEntities.VISUAL_SMOKE_CLOUD.get(), ctx -> new SmokeCloudRenderer<>(
                    ctx, model("generic_gray_smoke"), 1.0F));
            e.registerEntityRenderer(ModEntities.TOXIK_FIREFLY.get(), ctx -> new BlockbenchProjectileRenderer<>(
                    ctx, model("toxik_firefly_swarm"), null, "idle_hover", 0.16F,
                    BlockbenchProjectileRenderer.Alignment.MOTION_Z));
            e.registerEntityRenderer(ModEntities.RAPTOR_PULSE_GRENADE.get(), ctx -> new BlockbenchProjectileRenderer<>(
                    ctx, model("raptor_pulse_grenade"), "throw_spin", "idle_hold", 0.85F,
                    BlockbenchProjectileRenderer.Alignment.NONE));
            e.registerEntityRenderer(ModEntities.RAPTOR_FALCON_DRONE.get(), ctx -> new BlockbenchProjectileRenderer<>(
                    ctx, model("raptor_falcon_drone"), "launch_deploy", "idle_flight", 0.9F,
                    BlockbenchProjectileRenderer.Alignment.MOTION_Z));
            e.registerEntityRenderer(ModEntities.VLINDER_MEDICAL_DRONE.get(), ctx -> new BlockbenchProjectileRenderer<>(
                    ctx, model("vlinder_medical_drone"), null, "idle_hover", 0.9F,
                    BlockbenchProjectileRenderer.Alignment.NONE));
            e.registerEntityRenderer(ModEntities.VLINDER_REMOTE_SMOKE_ROUND.get(), ctx -> new BlockbenchProjectileRenderer<>(
                    ctx, model("vlinder_remote_smoke_round"), "activate", "idle", 0.8F,
                    BlockbenchProjectileRenderer.Alignment.MOTION_X));
            e.registerEntityRenderer(ModEntities.VLINDER_ACTIVE_DEFENSE_DRONE.get(), ctx -> new BlockbenchProjectileRenderer<>(
                    ctx, model("vlinder_active_defense_drone"), "deploy_shield_wings", "idle_hover", 0.9F,
                    BlockbenchProjectileRenderer.Alignment.NONE));
            e.registerEntityRenderer(ModEntities.TEMPEST_WALL_DRILL_STINGER.get(), ctx -> new BlockbenchProjectileRenderer<>(
                    ctx, model("tempest_wall_drill_stinger"), null, "idle", 0.85F,
                    BlockbenchProjectileRenderer.Alignment.MOTION_Z));
            e.registerEntityRenderer(ModEntities.TEMPEST_RECALL_ANCHOR.get(), TempestRecallAnchorRenderer::new);
            e.registerEntityRenderer(ModEntities.CATDAD_ROAD_TRUCK.get(), CatDadRoadTruckRenderer::new);
            e.registerEntityRenderer(ModEntities.DEPARTMENT_OVERHEAT_LASER.get(), ctx -> new BlockbenchProjectileRenderer<>(
                    ctx, model("department_overheat_laser"), null, "beam_pulse", 0.75F,
                    BlockbenchProjectileRenderer.Alignment.MOTION_Z));
            e.registerEntityRenderer(ModEntities.DEPARTMENT_EXPLOSIVE_TRAP.get(), ctx -> new BlockbenchProjectileRenderer<>(
                    ctx, model("department_explosive_trap"), "flip_open", "idle_hold", 1.2F,
                    BlockbenchProjectileRenderer.Alignment.NONE));
            e.registerEntityRenderer(ModEntities.SAEED_GUARD.get(), SaeedGuardRenderer::new);
            e.registerEntityRenderer(ModEntities.ASARA_FLAMETHROWER.get(), SaeedGuardRenderer::new);
            e.registerEntityRenderer(ModEntities.ASARA_MACHINE_GUNNER.get(), SaeedGuardRenderer::new);
            e.registerEntityRenderer(ModEntities.ASARA_SHIELD_TROOPER.get(), SaeedGuardRenderer::new);
            e.registerEntityRenderer(ModEntities.ASARA_ROCKET_TROOPER.get(), SaeedGuardRenderer::new);
            e.registerEntityRenderer(ModEntities.ASARA_SNIPER.get(), SaeedGuardRenderer::new);
            e.registerEntityRenderer(ModEntities.ASARA_SENTRY_SNIPER.get(), SaeedGuardRenderer::new);
            e.registerEntityRenderer(ModEntities.ASARA_GUARD_SOLDIER.get(), SaeedGuardRenderer::new);
            e.registerEntityRenderer(ModEntities.ACCELERATED_TROOPS.get(), SaeedGuardRenderer::new);
            e.registerEntityRenderer(ModEntities.ACCELERATED_TROOPS_PAYLOAD.get(), ctx -> new ThrownItemRenderer<>(ctx));
            e.registerEntityRenderer(ModEntities.RESPAWN_BEACON_MARKER.get(), ctx -> new ThrownItemRenderer<>(ctx));
            e.registerEntityRenderer(ModEntities.DEATH_CRATE.get(), DeathCrateRenderer::new);
            e.registerEntityRenderer(ModEntities.BEACON_BOSS.get(), BeaconBossRenderer::new);
            e.registerEntityRenderer(ModEntities.SAEED_FIRE_ARROW.get(), ctx -> new ThrownItemRenderer<>(ctx));
            e.registerEntityRenderer(ModEntities.SAEED_HAKIM_MISSILE.get(), ctx -> new ThrownItemRenderer<>(ctx));
            e.registerEntityRenderer(ModEntities.SAEED_FIRE_FIELD.get(), ctx -> new ThrownItemRenderer<>(ctx));
            e.registerEntityRenderer(ModEntities.N_TWO_TRACKING_GRENADE.get(), ctx -> new ThrownItemRenderer<>(ctx));
            e.registerEntityRenderer(ModEntities.N_TWO_DEWAR_CANISTER.get(), ctx -> new ThrownItemRenderer<>(ctx));
            e.registerEntityRenderer(ModEntities.N_TWO_CONDENSED_GRENADE.get(), ctx -> new ThrownItemRenderer<>(ctx));
            e.registerEntityRenderer(ModEntities.N_TWO_ICE_FIELD.get(), ctx -> new ThrownItemRenderer<>(ctx));
        }

        @SubscribeEvent
        public static void addLayers(EntityRenderersEvent.AddLayers e) {
            DfsEquipmentModelVisuals.registerLayers(e);
        }
    }

    private static ResourceLocation model(String path) {
        return new ResourceLocation(DealtForceSkillsMod.MODID, path);
    }
}