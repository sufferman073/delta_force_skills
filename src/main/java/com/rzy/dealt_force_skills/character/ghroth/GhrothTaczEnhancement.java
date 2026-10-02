package com.rzy.dealt_force_skills.character.ghroth;

import com.rzy.dealt_force_skills.skill.SkillCooldownHelper;

import com.rzy.dealt_force_skills.registry.ModGameRules;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Optional;

public final class GhrothTaczEnhancement {
    public static final String DISPLAY_SUFFIX = "Hvk军工制造";
    public static final String ENHANCED_TAG = "dealt_force_skills.ghroth_hvk_manufactured";
    private static final String ENHANCED_KIND = "dealt_force_skills.ghroth_hvk_kind";
    private static final String TACZ_GUN_ID_TAG = "GunId";
    private static final String TACZ_GUN_CURRENT_AMMO_COUNT_TAG = "GunCurrentAmmoCount";
    private static final String TACZ_GUN_HAS_BULLET_IN_BARREL_TAG = "HasBulletInBarrel";
    private static final String TACZ_GUN_ATTACHMENT_LOCK_TAG = "AttachmentLock";
    private static final String LAST_ENHANCED_AMMO_TAG = "dealt_force_skills.ghroth_last_enhanced_ammo";
    private static final String LAST_CACHE_REFRESH_TAG = "dealt_force_skills.ghroth_last_cache_refresh";
    private static final int CACHE_REFRESH_INTERVAL_TICKS = 10;
    public static volatile double ENHANCED_DAMAGE_MULTIPLIER = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("ENHANCED_DAMAGE_MULTIPLIER", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue(
      "characters.ghroth.ghroth_tacz_enhancement.enhanced_damage_multiplier", 2.75
   ));
    public static volatile double ENHANCED_SNIPER_DAMAGE_MULTIPLIER = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("ENHANCED_SNIPER_DAMAGE_MULTIPLIER", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue(
      "characters.ghroth.ghroth_tacz_enhancement.enhanced_sniper_damage_multiplier", 11.0
   ));
    public static volatile double ENHANCED_HEADSHOT_MULTIPLIER = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("ENHANCED_HEADSHOT_MULTIPLIER", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue(
      "characters.ghroth.ghroth_tacz_enhancement.enhanced_headshot_multiplier", 3.0
   ));
    public static volatile double ENHANCED_ATTACHMENT_PROPERTY_MULTIPLIER = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("ENHANCED_ATTACHMENT_PROPERTY_MULTIPLIER", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue(
      "characters.ghroth.ghroth_tacz_enhancement.enhanced_attachment_property_multiplier", 2.5
   ));
    public static volatile double ENHANCED_DAMAGE_LIMIT = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("ENHANCED_DAMAGE_LIMIT", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("characters.ghroth.ghroth_tacz_enhancement.enhanced_damage_limit", 1.0E9));
    public static volatile int ENHANCED_MAGAZINE_MULTIPLIER = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("ENHANCED_MAGAZINE_MULTIPLIER", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.ghroth.ghroth_tacz_enhancement.enhanced_magazine_multiplier", 6));
    private GhrothTaczEnhancement() {
    }

    public static ItemStack markArmoryStack(ItemStack stack, String kind) {
        if (stack.isEmpty()) {
            return stack;
        }
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putBoolean(ENHANCED_TAG, true);
        tag.putString(ENHANCED_KIND, kind == null ? "" : kind);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        String name = stack.getHoverName().getString();
        if (!name.contains(DISPLAY_SUFFIX)) {
            stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, Component.literal(name + " " + DISPLAY_SUFFIX).withStyle(ChatFormatting.GOLD));
        }
        if ("gun".equals(kind)) {
            enhanceArmoryGunNbt(stack);
        }
        return stack;
    }

    public static void tickEnhancedGunRuntime(ServerPlayer player) {
        if (!GhrothStateManager.isGhroth(player)) {
            return;
        }
        ItemStack gun = player.getMainHandItem();
        if (!isEnhancedGun(gun)) {
            return;
        }
        CompoundTag tag = gun.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putBoolean(TACZ_GUN_ATTACHMENT_LOCK_TAG, false);
        refreshEnhancedCache(player, gun, tag);
        int vanillaMagazine = vanillaMagazineSize(gun);
        int enhancedMagazine = enhancedMagazineCapacity(vanillaMagazine);
        if (enhancedMagazine <= 0) {
            gun.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            return;
        }
        int current = Math.max(0, tag.getInt(TACZ_GUN_CURRENT_AMMO_COUNT_TAG));
        boolean hadLast = tag.contains(LAST_ENHANCED_AMMO_TAG);
        int last = tag.getInt(LAST_ENHANCED_AMMO_TAG);
        if (current > enhancedMagazine) {
            current = enhancedMagazine;
        } else if (vanillaMagazine > 0 && current == vanillaMagazine && current < enhancedMagazine
                && (!hadLast || last < current)) {
            current = enhancedMagazine;
        }
        tag.putInt(TACZ_GUN_CURRENT_AMMO_COUNT_TAG, current);
        tag.putBoolean(TACZ_GUN_HAS_BULLET_IN_BARREL_TAG, current > 0);
        tag.putInt(LAST_ENHANCED_AMMO_TAG, current);
        gun.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static boolean isEnhanced(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBoolean(ENHANCED_TAG);
    }

    public static boolean isEnhancedGun(ItemStack stack) {
        return isEnhanced(stack) && "gun".equals(stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString(ENHANCED_KIND));
    }

    public static boolean isEnhancedAttachment(ItemStack stack) {
        return isEnhanced(stack) && "attachment".equals(stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString(ENHANCED_KIND));
    }

    public static boolean isTaczGunStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = customData != null ? customData.copyTag() : null;
        if (tag != null && tag.contains(TACZ_GUN_ID_TAG)) {
            return true;
        }
        return stack.getItem().getClass().getName().startsWith("com.tacz.guns.");
    }

    public static Optional<ResourceLocation> gunId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return Optional.empty();
        }
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = customData != null ? customData.copyTag() : null;
        if (tag == null || !tag.contains(TACZ_GUN_ID_TAG)) {
            return Optional.empty();
        }
        ResourceLocation id = ResourceLocation.tryParse(tag.getString(TACZ_GUN_ID_TAG));
        return Optional.ofNullable(id);
    }

    public static double outgoingGunDamageMultiplier(ServerPlayer player, ItemStack gun, ResourceLocation eventGunId) {
        return levelDamageMultiplier(player) * enhancedGunDamageMultiplier(gun, eventGunId);
    }

    public static double levelDamageMultiplier(ServerPlayer player) {
        double growth = com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue(
                "experience_growth.ghroth.gun_damage_per_level", 0.01D);
        return 1.0D + ModGameRules.effectiveExperienceLevel(player) * growth;
    }

    public static double enhancedGunDamageMultiplier(ItemStack gun, ResourceLocation eventGunId) {
        if (!isEnhancedGun(gun)) {
            return 1.0D;
        }
        double multiplier = ENHANCED_DAMAGE_MULTIPLIER;
        ResourceLocation gunId = eventGunId != null ? eventGunId : gunId(gun).orElse(null);
        if (isLikelySniper(gunId)) {
            multiplier *= ENHANCED_SNIPER_DAMAGE_MULTIPLIER;
        }
        return multiplier;
    }

    public static float headshotMultiplier(float original, ItemStack gun) {
        return headshotMultiplier(original, gun, null);
    }

    public static float headshotMultiplier(float original, ItemStack gun, ResourceLocation eventGunId) {
        return original * (float) enhancedHeadshotMultiplier(gun, eventGunId);
    }

    public static double enhancedHeadshotMultiplier(ItemStack gun, ResourceLocation eventGunId) {
        return isEnhancedGun(gun) ? ENHANCED_HEADSHOT_MULTIPLIER : 1.0D;
    }

    public static double fireRateMultiplier(LivingEntity entity) {
        if (!(entity instanceof ServerPlayer player) || !GhrothStateManager.isGhroth(player)) {
            return 1.0D;
        }
        return isEnhancedGun(player.getMainHandItem()) ? 2.0D : 1.0D;
    }

    public static double reloadMultiplier(LivingEntity entity) {
        if (!(entity instanceof ServerPlayer player) || !GhrothStateManager.isGhroth(player)) {
            return 1.0D;
        }
        return isEnhancedGun(player.getMainHandItem()) ? 2.0D : 1.0D;
    }

    public static double aimMultiplier(LivingEntity entity) {
        if (!(entity instanceof ServerPlayer player) || !GhrothStateManager.isGhroth(player)) {
            return 1.0D;
        }
        return isEnhancedGun(player.getMainHandItem()) ? 16.0D : 1.0D;
    }

    public static double boltMultiplier(LivingEntity entity) {
        if (!(entity instanceof ServerPlayer player) || !GhrothStateManager.isGhroth(player)) {
            return 1.0D;
        }
        return isEnhancedGun(player.getMainHandItem()) ? 4.0D : 1.0D;
    }

    public static float estimatedShotDamage(ServerPlayer player, ItemStack gun) {
        ResourceLocation gunId = gunId(gun).orElse(null);
        return estimatedShotDamage(player, gun, gunId);
    }

    public static float estimatedShotDamage(ServerPlayer player, ItemStack gun, ResourceLocation gunId) {
        double base = Math.max(1.0D, reflectedBaseDamage(gunId));
        return clampEnhancedDamage(base * outgoingGunDamageMultiplier(player, gun, gunId));
    }

    public static float clampEnhancedDamage(double amount) {
        if (!Double.isFinite(amount)) {
            return (float) ENHANCED_DAMAGE_LIMIT;
        }
        return (float) Math.max(0.0D, Math.min(ENHANCED_DAMAGE_LIMIT, amount));
    }

    public static int enhancedMagazineCapacity(ItemStack gun, int baseCapacity) {
        if (!isEnhancedGun(gun)) {
            return Math.max(0, baseCapacity);
        }
        return enhancedMagazineCapacity(baseCapacity);
    }

    public static boolean shouldForceAllowAttachment(ItemStack gun, ItemStack attachmentItem) {
        return isEnhancedGun(gun) && isTaczAttachmentStack(attachmentItem);
    }

    public static boolean shouldForceAllowAttachmentType(ItemStack gun, Object type) {
        return isEnhancedGun(gun) && type != null && !"NONE".equals(type.toString());
    }

    public static boolean hasEnhancedAttachmentInstalled(ItemStack gun) {
        return enhancedAttachmentCountInstalled(gun) > 0;
    }

    public static int enhancedAttachmentCountInstalled(ItemStack gun) {
        if (gun == null || gun.isEmpty()) {
            return 0;
        }
        CustomData customData = gun.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = customData != null ? customData.copyTag() : null;
        if (tag == null) {
            return 0;
        }
        int count = 0;
        for (String key : tag.getAllKeys()) {
            if (!key.startsWith("Attachment") || !tag.contains(key, 10)) {
                continue;
            }
            if (compoundHasEnhancedAttachment(tag.getCompound(key))) {
                count++;
            }
        }
        return count;
    }

    public static double enhancedAttachmentPropertyMultiplier(int enhancedAttachmentCount) {
        double multiplier = 1.0D;
        for (int i = 0; i < Math.max(0, enhancedAttachmentCount); i++) {
            multiplier = Math.min(1_000_000.0D, multiplier * ENHANCED_ATTACHMENT_PROPERTY_MULTIPLIER);
        }
        return multiplier;
    }

    public static boolean isLikelySniper(ResourceLocation gunId) {
        if (gunId == null) {
            return false;
        }
        String id = (gunId.getNamespace() + ":" + gunId.getPath()).toLowerCase(Locale.ROOT);
        return id.contains("sniper")
                || id.contains("m200")
                || id.contains("m82")
                || id.contains("awm")
                || id.contains("awp")
                || id.contains("barrett")
                || id.contains("tac50")
                || id.contains("m24")
                || id.contains("m700")
                || id.contains("svd")
                || id.contains("kar98")
                || id.contains("mosin");
    }

    private static double reflectedBaseDamage(ResourceLocation gunId) {
        if (gunId == null) {
            return 6.0D;
        }
        try {
            Class<?> timelessApi = Class.forName("com.tacz.guns.api.TimelessAPI");
            Object optional = timelessApi.getMethod("getCommonGunIndex", ResourceLocation.class).invoke(null, gunId);
            if (!(optional instanceof Optional<?> opt) || opt.isEmpty()) {
                return 6.0D;
            }
            Object index = opt.get();
            Object bulletData = index.getClass().getMethod("getBulletData").invoke(index);
            Method damage = bulletData.getClass().getMethod("getDamageAmount");
            Object value = damage.invoke(bulletData);
            return value instanceof Number number ? number.doubleValue() : 6.0D;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return 6.0D;
        }
    }

    private static void enhanceArmoryGunNbt(ItemStack gun) {
        CompoundTag tag = gun.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putBoolean(TACZ_GUN_ATTACHMENT_LOCK_TAG, false);
        int enhancedMagazine = enhancedMagazineCapacity(vanillaMagazineSize(gun));
        if (enhancedMagazine <= 0) {
            gun.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            return;
        }
        int current = Math.max(0, tag.getInt(TACZ_GUN_CURRENT_AMMO_COUNT_TAG));
        if (current < enhancedMagazine) {
            current = enhancedMagazine;
        }
        tag.putInt(TACZ_GUN_CURRENT_AMMO_COUNT_TAG, current);
        tag.putBoolean(TACZ_GUN_HAS_BULLET_IN_BARREL_TAG, current > 0);
        tag.putInt(LAST_ENHANCED_AMMO_TAG, current);
        gun.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private static int enhancedMagazineCapacity(int baseCapacity) {
        if (baseCapacity <= 0) {
            return 0;
        }
        long enhanced = (long) baseCapacity * ENHANCED_MAGAZINE_MULTIPLIER;
        return enhanced > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) enhanced;
    }

    private static int vanillaMagazineSize(ItemStack gun) {
        ResourceLocation gunId = gunId(gun).orElse(null);
        if (gunId == null) {
            return 0;
        }
        try {
            Class<?> timelessApi = Class.forName("com.tacz.guns.api.TimelessAPI");
            Object optional = timelessApi.getMethod("getCommonGunIndex", ResourceLocation.class).invoke(null, gunId);
            if (!(optional instanceof Optional<?> opt) || opt.isEmpty()) {
                return 0;
            }
            Object index = opt.get();
            Object gunData = index.getClass().getMethod("getGunData").invoke(index);
            Object ammo = gunData.getClass().getMethod("getAmmoAmount").invoke(gunData);
            int baseAmmo = ammo instanceof Number number ? Math.max(0, number.intValue()) : 0;
            int magLevel = reflectedMagExtendLevel(gun, gunData);
            if (magLevel <= 0) {
                return baseAmmo;
            }
            Object extended = gunData.getClass().getMethod("getExtendedMagAmmoAmount").invoke(gunData);
            if (!(extended instanceof int[] amounts) || magLevel > amounts.length) {
                return baseAmmo;
            }
            return Math.max(baseAmmo, Math.max(0, amounts[magLevel - 1]));
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return 0;
        }
    }

    private static int reflectedMagExtendLevel(ItemStack gun, Object gunData) {
        try {
            Class<?> utils = Class.forName("com.tacz.guns.util.AttachmentDataUtils");
            for (Method method : utils.getMethods()) {
                if (!"getMagExtendLevel".equals(method.getName()) || method.getParameterCount() != 2) {
                    continue;
                }
                Class<?>[] parameterTypes = method.getParameterTypes();
                if (!ItemStack.class.isAssignableFrom(parameterTypes[0])
                        || !parameterTypes[1].isAssignableFrom(gunData.getClass())) {
                    continue;
                }
                Object value = method.invoke(null, gun, gunData);
                if (value instanceof Number number) {
                    return Math.max(0, number.intValue());
                }
            }
        } catch (ReflectiveOperationException | LinkageError ignored) {
            // Fall back to the gun's own magazine size when TaCZ changes this helper.
        }
        return 0;
    }

    private static void refreshEnhancedCache(ServerPlayer player, ItemStack gun, CompoundTag tag) {
        long now = SkillCooldownHelper.now(player);
        long last = tag.getLong(LAST_CACHE_REFRESH_TAG);
        if (last > 0L && now - last < CACHE_REFRESH_INTERVAL_TICKS) {
            return;
        }
        tag.putLong(LAST_CACHE_REFRESH_TAG, now);
        try {
            Class<?> manager = Class.forName("com.tacz.guns.resource.modifier.AttachmentPropertyManager");
            for (Method method : manager.getMethods()) {
                if (!"postChangeEvent".equals(method.getName()) || method.getParameterCount() != 2) {
                    continue;
                }
                Class<?>[] parameterTypes = method.getParameterTypes();
                if (!parameterTypes[0].isAssignableFrom(player.getClass())
                        || !ItemStack.class.isAssignableFrom(parameterTypes[1])) {
                    continue;
                }
                method.invoke(null, player, gun);
                return;
            }
        } catch (ReflectiveOperationException | LinkageError ignored) {
            // The Mixin-backed ammo/refit hooks still apply if TaCZ changes its cache refresh API.
        }
    }

    private static boolean isTaczAttachmentStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = customData != null ? customData.copyTag() : null;
        if (tag != null && tag.contains("AttachmentId")) {
            return true;
        }
        String className = stack.getItem().getClass().getName();
        return className.startsWith("com.tacz.guns.") && className.toLowerCase(Locale.ROOT).contains("attachment");
    }

    private static boolean compoundHasEnhancedAttachment(CompoundTag compound) {
        if (compound == null || compound.isEmpty()) {
            return false;
        }
        if (compound.getBoolean(ENHANCED_TAG) && "attachment".equals(compound.getString(ENHANCED_KIND))) {
            return true;
        }
        if (compound.contains("tag", 10)) {
            CompoundTag nested = compound.getCompound("tag");
            if (nested.getBoolean(ENHANCED_TAG) && "attachment".equals(nested.getString(ENHANCED_KIND))) {
                return true;
            }
        }
        return false;
    }
}
