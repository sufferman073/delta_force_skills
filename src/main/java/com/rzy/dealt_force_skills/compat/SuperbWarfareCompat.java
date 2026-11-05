package com.rzy.dealt_force_skills.compat;

import com.google.gson.JsonElement;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.nbt.CompoundTag;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Array;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class SuperbWarfareCompat {
    private static final Gson GSON = new Gson();
    private static final String MOD_ID = "superbwarfare";
    private static final String VEHICLE_ENTITY_CLASS = "com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity";
    private static final float MAGNETIC_BOMB_MAX_HEALTH_FRACTION = 0.25F;

    private static volatile boolean vehicleClassResolved;
    private static volatile Class<?> vehicleClass;
    private static volatile Method getMaxHealthMethod;
    private static volatile Method getHealthMethod;
    private static volatile Method setHealthMethod;
    private static final int COMMANDER_VEHICLE_VALUE = 1_000_000;
    private static final double COMMANDER_WEAPON_SPEED = 3.0D;
    private static final int COMMANDER_FEED_AMMO_LIMIT = 1_000;
    private static final String AMMO_TUNED = "CommanderAmmoTuned";
    private static final String AMMO_PROFILE = "CommanderWeaponAmmoProfile";
    private static final String AMMO_DATA_PROFILE = "CommanderVehicleAmmoDataProfile";
    private static final String FORCED_CANNOT_FIRE = "CommanderCompatForcedCannotFire";
    private static final String FORCED_GUN_STOP = "CommanderCompatForcedGunStop";
    private static final String GUN_DATA_TAG = "GunData";
    private static final String GUN_OVERRIDE_TAG = "Override";
    private static final String DFS_OVERRIDE_TAG = "DFSWeaponOverride";
    private static final String DFS_OVERRIDE_ACTIVE = "DFSWeaponOverrideActive";
    private static final String DFS_OVERRIDE_BASE = "DFSWeaponOverrideBase";
    private static final String DFS_OVERRIDE_APPLIED = "DFSWeaponOverrideApplied";
    private static final String DFS_OVERRIDE_PROFILE = "DFSWeaponOverrideProfile";
    private static final String COMMANDER_VEHICLE_TAG = "dealt_force_skills.commander_vehicle";
    private static final List<String> GUN_DURATION_PROPERTIES = List.of(
            "SHOOT_DELAY", "SHOOT_DELAY_TIME", "BURST_COOLDOWN",
            "NORMAL_RELOAD_TIME", "EMPTY_RELOAD_TIME",
            "PREPARE_TIME", "PREPARE_LOAD_TIME", "PREPARE_AMMO_LOAD_TIME",
            "PREPARE_EMPTY_TIME", "ITERATIVE_TIME", "ITERATIVE_AMMO_LOAD_TIME",
            "FINISH_TIME", "BOLT_ACTION_TIME", "SEEK_TIME"
    );
    private static final Set<String> VEHICLE_TIMER_ACCESSORS = Set.of(
            "FIRE_TIME", "CANNON_FIRE_TIME", "GUN_FIRE_TIME", "COOL_DOWN", "CHARGING_TIME"
    );
    private static final Set<String> VEHICLE_AMMO_ACCESSORS = Set.of(
            "AMMO", "AMMO_COUNT", "MG_AMMO", "MAX_AMMO", "MISSILE_COUNT",
            "LOADED_ROCKET", "LOADED_BOMB", "LOADED_MISSILE", "LOADED_DRONE"
    );
    private static final Set<String> VEHICLE_HEAT_ACCESSORS = Set.of("HEAT", "COAX_HEAT");
    private static final Map<Class<?>, List<Field>> VEHICLE_INSTANCE_TIMERS = new ConcurrentHashMap<>();
    private static final Map<Class<?>, List<Field>> VEHICLE_DATA_TIMERS = new ConcurrentHashMap<>();
    private static final Map<Class<?>, List<Field>> VEHICLE_AMMO_DATA = new ConcurrentHashMap<>();
    private static final Map<Class<?>, List<Field>> VEHICLE_HEAT_DATA = new ConcurrentHashMap<>();
    private SuperbWarfareCompat() {
    }

    public static void damageVehicles(ServerLevel level, Vec3 center, double radius, DamageSource source,
                                      Entity excludedEntity, float magneticBombEquivalents,
                                      boolean requireLineOfSight) {
        damageVehicles(level, center, radius, source, excludedEntity, magneticBombEquivalents,
                requireLineOfSight, entity -> true);
    }

    public static void damageVehicles(ServerLevel level, Vec3 center, double radius, DamageSource source,
                                      Entity excludedEntity, float magneticBombEquivalents,
                                      boolean requireLineOfSight, java.util.function.Predicate<Entity> areaFilter) {
        if (level == null || center == null || source == null || radius <= 0.0D
                || magneticBombEquivalents <= 0.0F || !isLoaded()) {
            return;
        }

        AABB searchBox = new AABB(center, center).inflate(radius);
        for (Entity entity : level.getEntities((Entity) null, searchBox,
                entity -> entity != excludedEntity && isVehicle(entity) && !entity.isRemoved() && areaFilter.test(entity))) {
            AABB bounds = entity.getBoundingBox();
            Vec3 hitPoint = closestPoint(center, bounds);
            double distance = hitPoint.distanceTo(center);
            if (distance > radius) {
                continue;
            }
            if (requireLineOfSight && !hasLineOfSight(level, center, hitPoint, entity)) {
                continue;
            }

            float maxHealth = maxHealth(entity);
            if (maxHealth <= 0.0F) {
                continue;
            }
            float falloff = Math.max(0.0F, 1.0F - (float) (distance / radius));
            float damage = maxHealth * MAGNETIC_BOMB_MAX_HEALTH_FRACTION * magneticBombEquivalents * falloff;
            if (damage <= 0.0F) {
                continue;
            }
            entity.hurt(source, damage);
            entity.hurtMarked = true;
        }
    }

    public static void removeVehicles(List<Entity> entities) {
        if (entities == null || entities.isEmpty() || !isLoaded()) {
            return;
        }
        entities.removeIf(SuperbWarfareCompat::isVehicle);
    }

    public static boolean isVehicle(Entity entity) {
        Class<?> type = vehicleClass();
        return entity != null && type != null && type.isInstance(entity);
    }

    public static List<net.minecraft.resources.ResourceLocation> registeredVehicleIds(Level level) {
        if (!isLoaded() || level == null) {
            return List.of();
        }
        return ForgeRegistries.ENTITY_TYPES.getValues().stream()
                .map(ForgeRegistries.ENTITY_TYPES::getKey)
                .filter(id -> id != null && MOD_ID.equals(id.getNamespace()))
                .filter(id -> {
                    EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(id);
                    Entity candidate = type == null ? null : type.create(level);
                    boolean vehicle = candidate != null && isVehicle(candidate);
                    if (candidate != null) {
                        candidate.discard();
                    }
                    return vehicle;
                })
                .sorted(Comparator.comparing(net.minecraft.resources.ResourceLocation::toString))
                .toList();
    }

    public static EntityType<?> registeredVehicleType(ServerLevel level) {
        List<net.minecraft.resources.ResourceLocation> ids = registeredVehicleIds(level);
        return ids.isEmpty() ? null : registeredVehicleType(level, ids.get(0).toString());
    }

    public static EntityType<?> registeredVehicleType(ServerLevel level, String requestedId) {
        if (level == null || !isLoaded()) {
            return null;
        }
        if (requestedId == null || requestedId.isBlank()) {
            return null;
        }
        net.minecraft.resources.ResourceLocation id;
        try {
            id = net.minecraft.resources.ResourceLocation.parse(requestedId);
        } catch (RuntimeException ignored) {
            return null;
        }
        if (!MOD_ID.equals(id.getNamespace())) {
            return null;
        }
        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(id);
        if (type == null) {
            return null;
        }
        Entity candidate = type.create(level);
        boolean vehicle = candidate != null && isVehicle(candidate);
        if (candidate != null) {
            candidate.discard();
        }
        return vehicle ? type : null;
    }

    /** Marks a spawned Commander vehicle for the generic protection/maintenance hooks. */
    public static void configureCommanderVehicle(Entity entity, String markerTag) {
        if (!isVehicle(entity) || entity == null) {
            return;
        }
        entity.getPersistentData().putBoolean(markerTag, true);
        if (entity instanceof net.minecraft.world.entity.LivingEntity living) {
            living.setHealth(living.getMaxHealth());
        }
        setEnergy(entity, COMMANDER_VEHICLE_VALUE);
        maintainCommanderWeapons(entity);
    }

    public static boolean isCommanderVehicle(Entity entity, String markerTag) {
        return entity != null && isVehicle(entity) && entity.getPersistentData().getBoolean(markerTag);
    }

    public static boolean isCommanderVehicle(Entity entity) {
        return isCommanderVehicle(entity, COMMANDER_VEHICLE_TAG);
    }

    /** Applies the same live fire/reload multipliers used by the TaCZ bridge to mounted vehicles. */
    public static void applyWeaponSpeedModifiers(Entity entity, double fireRate, double reloadRate,
                                                 double aimRate, double aimPenalty, double boltRate) {
        if (!isVehicle(entity)) {
            return;
        }
        double effectiveFireRate = isCommanderVehicle(entity)
                ? Math.max(COMMANDER_WEAPON_SPEED, positiveMultiplier(fireRate))
                : positiveMultiplier(fireRate);
        boolean autoRefill = positiveMultiplier(reloadRate) > 1.0001D;
        boolean legacyWeapons = maintainLegacyVehicleWeapons(entity, effectiveFireRate, 1.0D, aimRate, boltRate,
                false, autoRefill);
        boolean modernWeapons = maintainGunDataVehicleWeapons(entity, effectiveFireRate, 1.0D,
                positiveMultiplier(aimRate) / positiveMultiplier(aimPenalty), boltRate,
                false, autoRefill);
        if (!legacyWeapons && !modernWeapons) {
            accelerateVehicleTimers(entity, effectiveFireRate, 1.0D);
        }
    }

    /** Applies TaCZ-equivalent gun property, timer, and disable state to SuperbWarfare handguns. */
    public static void applyPlayerWeaponModifiers(Player player, double fireRate, double reloadRate,
                                                  double aimRate, double aimPenalty, double boltRate,
                                                  boolean disabled) {
        if (player == null || !isLoaded()) {
            return;
        }
        double effectiveAimRate = positiveMultiplier(aimRate) / positiveMultiplier(aimPenalty);
        applyGunDataModifiers(player, player.getMainHandItem(), fireRate, reloadRate,
                effectiveAimRate, boltRate, disabled);
        if (player.getOffhandItem() != player.getMainHandItem()) {
            applyGunDataModifiers(player, player.getOffhandItem(), fireRate, reloadRate,
                    effectiveAimRate, boltRate, disabled);
        }
    }

    /** Advances SuperbWarfare's client-only ADS and mounted-fire gates. */
    public static void applyClientWeaponModifiers(Player player, double fireRate, double aimRate,
                                                  double aimPenalty, boolean commanderVehicle,
                                                  boolean disabled) {
        if (player == null || !isLoaded()) {
            return;
        }
        if (disabled) {
            player.stopUsingItem();
        }
        adjustClientAim(player.getMainHandItem(), aimRate, aimPenalty);
        adjustClientGunTimer(player.getMainHandItem(), fireRate);
        Entity vehicle = player.getVehicle();
        if (isVehicle(vehicle)) {
            double effectiveFireRate = commanderVehicle
                    ? Math.max(COMMANDER_WEAPON_SPEED, positiveMultiplier(fireRate))
                    : positiveMultiplier(fireRate);
            if (!hasStructuredVehicleWeapons(vehicle)) {
                adjustClientVehicleTimer(effectiveFireRate);
            }
        }
    }

    /** Returns whether the local SuperbWarfare weapon or vehicle is currently scoped. */
    public static boolean isClientAiming(Player player) {
        if (player == null || !isLoaded()) {
            return false;
        }
        try {
            Class<?> handler = clientEventHandlerClass();
            if (handler == null) {
                return false;
            }
            if (isVehicle(player.getVehicle()) && readStaticBoolean(handler, "zoomVehicle")) {
                return true;
            }
            Class<?> gunItemClass = Class.forName("com.atsuishio.superbwarfare.item.gun.GunItem");
            return gunItemClass.isInstance(player.getMainHandItem().getItem())
                    && readStaticBoolean(handler, "zoom");
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            return false;
        }
    }

    /**
     * Supplies the character auto-aim target to SuperbWarfare's client seeker state.
     * Native seeker input remains responsible for turning this candidate into a firing lock.
     */
    public static void applyClientScopedTargetLock(Player player, Entity target) {
        if (player == null || target == null || !target.isAlive() || !isClientAiming(player)) {
            return;
        }
        try {
            Class<?> handler = clientEventHandlerClass();
            if (handler == null) {
                return;
            }
            String suffix = isVehicle(player.getVehicle()) ? "Vehicle" : "";
            setClientHandlerEntity(handler, "nearestEntity" + suffix, target);
            setClientHandlerEntity(handler, "seekingEntity" + suffix, target);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            // Target-state fields are optional and differ between SuperbWarfare releases.
        }
    }

    public static void setVehicleWeaponDisabled(Entity entity, boolean disabled) {
        if (!isVehicle(entity)) {
            return;
        }
        CompoundTag data = entity.getPersistentData();
        if (disabled) {
            boolean changed = writeBooleanField(entity, "cannotFire", true);
            changed |= writeBooleanField(entity, "cannotFireCoax", true);
            if (changed) {
                data.putBoolean(FORCED_CANNOT_FIRE, true);
            }
        } else if (data.getBoolean(FORCED_CANNOT_FIRE)) {
            writeBooleanField(entity, "cannotFire", false);
            writeBooleanField(entity, "cannotFireCoax", false);
            data.remove(FORCED_CANNOT_FIRE);
        }
    }

    public static void maintainCommanderVehicle(Entity entity, String markerTag) {
        if (!isCommanderVehicle(entity, markerTag)) {
            return;
        }
        if (entity.tickCount % 20 == 0) {
            repairVehicle(entity);
        }
        setEnergy(entity, COMMANDER_VEHICLE_VALUE);
        maintainCommanderWeapons(entity);
    }

    /**
     * Uses SuperbWarfare's own container block so its normal opening animation and
     * delayed vehicle deployment remain intact. Reflection keeps this optional.
     */
    public static boolean spawnVehicleContainer(ServerLevel level, BlockPos pos, Direction facing,
                                                 EntityType<?> type, String markerTag) {
        if (level == null || pos == null || type == null || !isLoaded()) {
            return false;
        }
        Block block = ForgeRegistries.BLOCKS.getValue(
                new ResourceLocation(MOD_ID, "container"));
        if (block == null || !level.setBlock(pos, block.defaultBlockState(), 3)) {
            return false;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null || !writeContainerPayload(blockEntity, level, type, markerTag)) {
            level.removeBlock(pos, false);
            return false;
        }

        BlockState state = level.getBlockState(pos);
        for (Property<?> property : state.getProperties()) {
            if ("facing".equals(property.getName()) && facing != null) {
                state = setProperty(state, property, facing);
            } else if ("opened".equals(property.getName())) {
                state = setProperty(state, property, Boolean.TRUE);
            }
        }
        level.setBlock(pos, state, 3);
        blockEntity.setChanged();
        level.sendBlockUpdated(pos, state, state, 3);
        return true;
    }

    private static boolean writeContainerPayload(BlockEntity blockEntity, ServerLevel level,
                                                  EntityType<?> type, String markerTag) {
        Entity vehicle = type.create(level);
        if (vehicle == null || !isVehicle(vehicle)) {
            return false;
        }
        configureCommanderVehicle(vehicle, markerTag);
        CompoundTag entityTag = new CompoundTag();
        vehicle.saveWithoutId(entityTag);
        vehicle.discard();
        try {
            Field entityType = blockEntity.getClass().getField("entityType");
            Field payload = blockEntity.getClass().getField("entityTag");
            entityType.set(blockEntity, type);
            payload.set(blockEntity, entityTag);
            return true;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return false;
        }
    }

    private static BlockState setProperty(BlockState state, Property<?> property, Object value) {
        try {
            if (property instanceof DirectionProperty directionProperty && value instanceof Direction direction) {
                return state.setValue(directionProperty, direction);
            }
            if (property instanceof BooleanProperty booleanProperty && value instanceof Boolean flag) {
                return state.setValue(booleanProperty, flag);
            }
            return state;
        } catch (IllegalArgumentException ignored) {
            return state;
        }
    }

    private static void invokeNumeric(Entity entity, String name, int value) {
        invokeNumeric(entity, name, (double) value);
    }

    private static void invokeNumeric(Entity entity, String name, double value) {
        for (Method method : entity.getClass().getMethods()) {
            if (!method.getName().equals(name) || method.getParameterCount() != 1) {
                continue;
            }
            Class<?> parameter = method.getParameterTypes()[0];
            Object converted;
            if (parameter == int.class || parameter == Integer.class) converted = (int) Math.round(value);
            else if (parameter == long.class || parameter == Long.class) converted = (long) value;
            else if (parameter == float.class || parameter == Float.class) converted = (float) value;
            else if (parameter == double.class || parameter == Double.class) converted = value;
            else continue;
            try {
                method.invoke(entity, converted);
            } catch (IllegalAccessException | InvocationTargetException | RuntimeException ignored) {
                // Optional SuperbWarfare versions expose different public setter signatures.
            }
        }
    }

    private static void setEnergy(Entity entity, int requested) {
        try {
            Method storage = entity.getClass().getMethod("getEnergyStorage");
            Object energy = storage.invoke(entity);
            if (energy != null) {
                Method receive = energy.getClass().getMethod("receiveEnergy", int.class, boolean.class);
                int max = Math.max(0, ((Number) entity.getClass().getMethod("getMaxEnergy").invoke(entity)).intValue());
                receive.invoke(energy, Math.min(requested, max), false);
                return;
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }
        try {
            Class<?> current = entity.getClass();
            while (current != null) {
                try {
                    Method setter = current.getDeclaredMethod("setEnergy", int.class);
                    setter.setAccessible(true);
                    int max = ((Number) entity.getClass().getMethod("getMaxEnergy").invoke(entity)).intValue();
                    setter.invoke(entity, Math.min(requested, Math.max(0, max)));
                    return;
                } catch (NoSuchMethodException ignored) {
                    current = current.getSuperclass();
                }
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }
    }

    private static void maintainCommanderWeapons(Entity entity) {
        boolean legacyWeapons = maintainLegacyVehicleWeapons(entity, COMMANDER_WEAPON_SPEED, 1.0D,
                COMMANDER_WEAPON_SPEED, COMMANDER_WEAPON_SPEED, true, true);
        boolean modernWeapons = maintainGunDataVehicleWeapons(entity,
                COMMANDER_WEAPON_SPEED, 1.0D,
                COMMANDER_WEAPON_SPEED, COMMANDER_WEAPON_SPEED, true, true);
        refillVehicleAmmoData(entity, entity.getPersistentData());
        entity.getPersistentData().putBoolean(AMMO_TUNED, true);
        clearHeat(entity);
        if (!legacyWeapons && !modernWeapons) {
            accelerateVehicleTimers(entity, COMMANDER_WEAPON_SPEED, 1.0D);
        }
    }

    private static boolean maintainLegacyVehicleWeapons(Entity entity, double fireRate,
                                                         double reloadRate, double aimRate,
                                                         double boltRate, boolean commanderSupply,
                                                         boolean autoRefill) {
        CompoundTag persistent = entity.getPersistentData();
        Object weapons = invokeNoArg(entity, "getAllWeapons");
        if (weapons == null) {
            weapons = readField(entity, "availableWeapons");
        }
        if (weapons != null && weapons.getClass().isArray() && Array.getLength(weapons) > 0) {
            CompoundTag profile = persistent.getCompound(AMMO_PROFILE);
            tuneWeaponArray(weapons, profile, new int[] {0}, new IdentityHashMap<>(),
                    positiveMultiplier(fireRate), positiveMultiplier(reloadRate),
                    positiveMultiplier(aimRate), positiveMultiplier(boltRate),
                    commanderSupply, autoRefill);
            persistent.put(AMMO_PROFILE, profile);
            return true;
        }
        return false;
    }

    private static boolean maintainGunDataVehicleWeapons(Entity entity, double fireRate,
                                                         double reloadRate, double aimRate,
                                                         double boltRate, boolean commanderSupply,
                                                         boolean autoRefill) {
        Object rawMap = invokeNoArg(entity, "getGunDataMap");
        if (!(rawMap instanceof Map<?, ?> source) || source.isEmpty()) {
            return false;
        }
        CompoundTag profile = entity.getPersistentData().getCompound(AMMO_PROFILE);
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            Object gunData = entry.getValue();
            if (gunData == null) {
                continue;
            }
            GunModifierState state = new GunModifierState();
            state.fireRate = positiveMultiplier(fireRate);
            state.reloadRate = positiveMultiplier(reloadRate);
            state.aimRate = positiveMultiplier(aimRate);
            state.boltRate = positiveMultiplier(boltRate);
            state.autoRefill = autoRefill;
            int desiredAmmo = 0;
            if (commanderSupply) {
                String key = "gun_data_" + String.valueOf(entry.getKey());
                int currentMagazine = readGunIntProperty(gunData, "MAGAZINE");
                if (currentMagazine <= 0) {
                    // A zero magazine is SuperbWarfare's direct-feed mode. Keep it
                    // zero and supply a bounded virtual belt instead of creating a magazine.
                    profile.remove(key);
                    desiredAmmo = COMMANDER_FEED_AMMO_LIMIT;
                } else {
                    int base = profile.contains(key, net.minecraft.nbt.Tag.TAG_INT)
                            ? Math.max(1, profile.getInt(key)) : currentMagazine;
                    if (!profile.contains(key, net.minecraft.nbt.Tag.TAG_INT) || profile.getInt(key) <= 0) {
                        profile.putInt(key, base);
                    }
                    desiredAmmo = scaleAmmo(base);
                    state.magazineRate = 10.0D;
                }
                refillGunNumber(gunData, "virtualAmmo", desiredAmmo);
                refillGunNumber(gunData, "backupAmmoCount", desiredAmmo);
                if (numberAboveZero(readGunNumberValue(gunData, "heat"))) {
                    setGunNumberValue(gunData, "heat", 0.0D);
                }
                if (Boolean.TRUE.equals(readGunBooleanValue(gunData, "overHeat"))) {
                    setGunBooleanValue(gunData, "overHeat", false);
                }
            }
            applyGunPropertyModifiers(gunData, state, false);
            if (autoRefill) {
                if (!commanderSupply) {
                    desiredAmmo = readGunIntProperty(gunData, "MAGAZINE");
                }
                if (readGunIntProperty(gunData, "MAGAZINE") > 0) {
                    refillCurrentGunAmmo(gunData, desiredAmmo);
                }
            }
        }
        // VehicleEntity exposes a rebuilt map on every getter. Persist the mutated
        // GunData stack as one map after the pass, without replacing it piecemeal.
        invokeMapSetter(entity, "setGunDataMap", new LinkedHashMap<>(source));
        if (commanderSupply) {
            entity.getPersistentData().put(AMMO_PROFILE, profile);
        }
        return true;
    }

    private static int readGunIntProperty(Object gunData, String propertyName) {
        try {
            Class<?> propClass = Class.forName("com.atsuishio.superbwarfare.data.gun.GunProp");
            Object property = propClass.getField(propertyName).get(null);
            Object value = gunData.getClass().getMethod("get", propClass).invoke(gunData, property);
            return value instanceof Number number ? number.intValue() : 0;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            return 0;
        }
    }

    private static void tuneWeaponArray(Object array, CompoundTag profile,
                                        int[] index, IdentityHashMap<Object, Boolean> visited,
                                        double fireRate, double reloadRate, double aimRate,
                                        double boltRate, boolean commanderSupply,
                                        boolean autoRefill) {
        int length = Array.getLength(array);
        for (int i = 0; i < length; i++) {
            Object value = Array.get(array, i);
            if (value == null) continue;
            if (value.getClass().isArray()) {
                tuneWeaponArray(value, profile, index, visited, fireRate, reloadRate,
                        aimRate, boltRate, commanderSupply, autoRefill);
                continue;
            }
            if (visited.put(value, Boolean.TRUE) != null) {
                continue;
            }
            String key = "weapon_" + index[0]++;
            int currentMax = readIntField(value, "maxAmmo");
            if (commanderSupply) {
                boolean firstProfile = !profile.contains(key, net.minecraft.nbt.Tag.TAG_INT);
                String defaultLimitKey = key + "_default_limit";
                boolean missingLimit = profile.getBoolean(defaultLimitKey) || currentMax <= 0
                        || (!firstProfile && profile.getInt(key) == 1 && currentMax == scaleAmmo(1));
                int desired;
                if (missingLimit) {
                    profile.remove(key);
                    profile.putBoolean(defaultLimitKey, true);
                    desired = COMMANDER_FEED_AMMO_LIMIT;
                } else {
                    profile.remove(defaultLimitKey);
                    int base = firstProfile ? currentMax : Math.max(1, profile.getInt(key));
                    if (firstProfile || profile.getInt(key) <= 0) {
                        profile.putInt(key, base);
                    }
                    desired = scaleAmmo(base);
                }
                if (desired > 0) {
                    if (currentMax < desired) {
                        writeIntField(value, "maxAmmo", desired);
                    }
                    writeIntField(value, "backupAmmo", desired);
                }
            }

            int effectiveMax = readIntField(value, "maxAmmo");
            if (autoRefill && effectiveMax > 0
                    && readIntField(value, "currentAmmo") != effectiveMax) {
                writeIntField(value, "currentAmmo", effectiveMax);
            }

            String fireTimeKey = key + "_fire_time";
            int currentFireTime = readIntField(value, "fireTime");
            if (currentFireTime > 0 || profile.contains(fireTimeKey, net.minecraft.nbt.Tag.TAG_INT)) {
                int baseFireTime = profile.contains(fireTimeKey, net.minecraft.nbt.Tag.TAG_INT)
                        ? profile.getInt(fireTimeKey) : currentFireTime;
                profile.putInt(fireTimeKey, baseFireTime);
                writeIntField(value, "fireTime", scaledDuration(baseFireTime, fireRate));
            }
            scaleLegacyDurationFields(value, profile, key, fireRate, reloadRate, aimRate, boltRate);
        }
    }

    private static void scaleLegacyDurationFields(Object weapon, CompoundTag profile, String key,
                                                   double fireRate, double reloadRate,
                                                   double aimRate, double boltRate) {
        for (Class<?> type = weapon.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (field.getType() != int.class || Modifier.isStatic(field.getModifiers())
                        || !field.trySetAccessible()) {
                    continue;
                }
                String name = field.getName().toLowerCase(java.util.Locale.ROOT);
                if (!isLegacyDurationField(name) || "firetime".equals(name)) {
                    continue;
                }
                String fieldKey = key + "_" + name;
                int current;
                try {
                    current = field.getInt(weapon);
                } catch (IllegalAccessException ignored) {
                    continue;
                }
                if (current <= 0 && !profile.contains(fieldKey, net.minecraft.nbt.Tag.TAG_INT)) {
                    continue;
                }
                int base = profile.contains(fieldKey, net.minecraft.nbt.Tag.TAG_INT)
                        ? profile.getInt(fieldKey) : current;
                profile.putInt(fieldKey, base);
                double multiplier = name.contains("reload") || name.contains("prepare")
                        ? reloadRate
                        : name.contains("bolt") ? boltRate
                        : name.contains("aim") || name.contains("seek") ? aimRate : fireRate;
                try {
                    field.setInt(weapon, scaledDuration(base, multiplier));
                } catch (IllegalAccessException ignored) {
                }
            }
        }
    }

    private static boolean isLegacyDurationField(String name) {
        return name.contains("time") || name.contains("tick") || name.contains("cooldown")
                || name.contains("delay");
    }

    private static int readIntField(Object object, String name) {
        try {
            Field field = object.getClass().getField(name);
            return field.getType() == int.class ? field.getInt(object) : 0;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return 0;
        }
    }

    private static boolean writeIntField(Object object, String name, int value) {
        try {
            Field field = object.getClass().getField(name);
            if (field.getType() != int.class) {
                return false;
            }
            field.setInt(object, value);
            return true;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return false;
        }
    }

    private static boolean writeBooleanField(Object object, String name, boolean value) {
        Class<?> type = object.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(name);
                if (field.getType() != boolean.class || !field.trySetAccessible()) {
                    return false;
                }
                field.setBoolean(object, value);
                return true;
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                return false;
            }
        }
        return false;
    }

    private static Object readField(Object object, String name) {
        for (Class<?> type = object.getClass(); type != null; type = type.getSuperclass()) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(object);
            } catch (NoSuchFieldException ignored) {
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                return null;
            }
        }
        return null;
    }

    private static Object invokeNoArg(Object object, String name) {
        try {
            return object.getClass().getMethod(name).invoke(object);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    private static void clearHeat(Entity entity) {
        Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Field field : VEHICLE_HEAT_DATA.computeIfAbsent(entity.getClass(),
                type -> discoverDataAccessors(type, VEHICLE_HEAT_ACCESSORS))) {
            try {
                Object accessor = field.get(null);
                if (accessor != null && visited.add(accessor)) {
                    writeSyncedInt(entity, accessor, 0);
                }
            } catch (IllegalAccessException | RuntimeException ignored) {
            }
        }
    }

    private static void refillVehicleAmmoData(Entity entity, CompoundTag persistent) {
        CompoundTag profile = persistent.getCompound(AMMO_DATA_PROFILE);
        Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Field field : VEHICLE_AMMO_DATA.computeIfAbsent(entity.getClass(),
                type -> discoverDataAccessors(type, VEHICLE_AMMO_ACCESSORS))) {
            try {
                Object accessor = field.get(null);
                if (accessor == null || !visited.add(accessor)) {
                    continue;
                }
                Integer current = readSyncedInt(entity, accessor);
                if (current == null) {
                    continue;
                }
                String key = field.getDeclaringClass().getSimpleName() + "_" + field.getName();
                String defaultLimitKey = key + "_default_limit";
                boolean missingLimit = profile.getBoolean(defaultLimitKey) || current <= 0;
                int desired;
                if (missingLimit) {
                    profile.remove(key);
                    profile.putBoolean(defaultLimitKey, true);
                    desired = COMMANDER_FEED_AMMO_LIMIT;
                } else {
                    profile.remove(defaultLimitKey);
                    int base = profile.contains(key, net.minecraft.nbt.Tag.TAG_INT)
                            ? Math.max(1, profile.getInt(key)) : current;
                    if (!profile.contains(key, net.minecraft.nbt.Tag.TAG_INT) || profile.getInt(key) <= 0) {
                        profile.putInt(key, base);
                    }
                    desired = scaleAmmo(base);
                }
                if (desired > 0 && current != desired) {
                    writeSyncedInt(entity, accessor, desired);
                }
            } catch (IllegalAccessException | RuntimeException ignored) {
            }
        }
        persistent.put(AMMO_DATA_PROFILE, profile);
    }

    private static int scaleAmmo(int base) {
        return base > Integer.MAX_VALUE / 10 ? Integer.MAX_VALUE : Math.max(0, base * 10);
    }

    private static void accelerateVehicleTimers(Entity entity, double fireRate, double reloadRate) {
        if (positiveMultiplier(fireRate) <= 1.0001D && positiveMultiplier(reloadRate) <= 1.0001D) {
            return;
        }
        for (Field field : VEHICLE_INSTANCE_TIMERS.computeIfAbsent(entity.getClass(),
                SuperbWarfareCompat::discoverInstanceTimers)) {
            try {
                int extraTicks = timerExtraTicks(field.getName(), fireRate, reloadRate);
                int current = field.getInt(entity);
                if (extraTicks > 0 && current > 0) {
                    field.setInt(entity, Math.max(0, current - extraTicks));
                }
            } catch (IllegalAccessException | RuntimeException ignored) {
            }
        }
        Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Field field : VEHICLE_DATA_TIMERS.computeIfAbsent(entity.getClass(),
                type -> discoverDataAccessors(type, VEHICLE_TIMER_ACCESSORS))) {
            try {
                Object accessor = field.get(null);
                if (accessor == null || !visited.add(accessor)) {
                    continue;
                }
                int extraTicks = timerExtraTicks(field.getName(), fireRate, reloadRate);
                Integer current = readSyncedInt(entity, accessor);
                if (extraTicks > 0 && current != null && current > 0) {
                    writeSyncedInt(entity, accessor, Math.max(0, current - extraTicks));
                }
            } catch (IllegalAccessException | RuntimeException ignored) {
            }
        }
    }

    private static int timerExtraTicks(String fieldName, double fireRate, double reloadRate) {
        String normalized = fieldName.toLowerCase(java.util.Locale.ROOT);
        double multiplier = normalized.contains("reload") || normalized.contains("handbusy")
                ? reloadRate
                : normalized.contains("fire") || normalized.contains("shoot") || normalized.equals("cooldown")
                ? fireRate
                : Math.max(fireRate, reloadRate);
        return extraTicks(multiplier);
    }

    private static List<Field> discoverInstanceTimers(Class<?> initial) {
        java.util.ArrayList<Field> result = new java.util.ArrayList<>();
        for (Class<?> type = initial; type != null && type != Entity.class; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (field.getType() != int.class || Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                String name = field.getName().toLowerCase(java.util.Locale.ROOT);
                boolean timer = name.equals("cooldown")
                        || name.equals("handbusytime")
                        || name.contains("reloadcooldown")
                        || name.contains("firecooldown");
                if (timer && field.trySetAccessible()) {
                    result.add(field);
                }
            }
        }
        return List.copyOf(result);
    }

    private static List<Field> discoverDataAccessors(Class<?> initial, Set<String> names) {
        java.util.ArrayList<Field> result = new java.util.ArrayList<>();
        for (Class<?> type = initial; type != null && type != Entity.class; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) && names.contains(field.getName())
                        && field.getType().getName().equals("net.minecraft.network.syncher.EntityDataAccessor")
                        && field.trySetAccessible()) {
                    result.add(field);
                }
            }
        }
        return List.copyOf(result);
    }

    @SuppressWarnings("unchecked")
    private static Integer readSyncedInt(Entity entity, Object rawAccessor) {
        try {
            net.minecraft.network.syncher.EntityDataAccessor<Object> accessor =
                    (net.minecraft.network.syncher.EntityDataAccessor<Object>) rawAccessor;
            Object value = entity.getEntityData().get(accessor);
            return value instanceof Integer integer ? integer : null;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static void writeSyncedInt(Entity entity, Object rawAccessor, int value) {
        try {
            net.minecraft.network.syncher.EntityDataAccessor<Object> accessor =
                    (net.minecraft.network.syncher.EntityDataAccessor<Object>) rawAccessor;
            entity.getEntityData().set(accessor, value);
        } catch (RuntimeException ignored) {
        }
    }

    private static void applyGunDataModifiers(Player player, ItemStack stack, double fireRate, double reloadRate,
                                              double aimRate, double boltRate, boolean disabled) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        try {
            Class<?> gunItemClass = Class.forName("com.atsuishio.superbwarfare.item.gun.GunItem");
            if (!gunItemClass.isInstance(stack.getItem())) {
                return;
            }
            Class<?> gunDataClass = Class.forName("com.atsuishio.superbwarfare.data.gun.GunData");
            Object gunData = gunDataClass.getMethod("from", ItemStack.class).invoke(null, stack);
            GunModifierState state = new GunModifierState();
            state.fireRate = positiveMultiplier(fireRate);
            state.reloadRate = 1.0D;
            state.aimRate = positiveMultiplier(aimRate);
            state.boltRate = positiveMultiplier(boltRate);
            state.autoRefill = positiveMultiplier(reloadRate) > 1.0001D;
            applyGunPropertyModifiers(gunData, state);
            if (state.autoRefill) {
                refillCurrentGunAmmo(gunData, readGunIntProperty(gunData, "MAGAZINE"));
            }
            updateGunDisabledState(player, stack, gunData, disabled);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            // SuperbWarfare is optional and its gun API differs between releases.
        }
    }

    private static void applyGunPropertyModifiers(Object gunData, GunModifierState state) {
        applyGunPropertyModifiers(gunData, state, true);
    }

    private static void applyGunPropertyModifiers(Object gunData, GunModifierState state,
                                                  boolean reduceLiveTimers) {
        if (!applyGunOverride(gunData, state)) {
            applyLegacyGunPropertyModifiers(gunData, state);
        }
        if (reduceLiveTimers) {
            reduceGunTimers(gunData, state);
        }
    }

    private static boolean applyLegacyGunPropertyModifiers(Object gunData, GunModifierState state) {
        boolean applied = false;
        for (String property : gunProperties(state)) {
            try {
                setGunPropertyModifier(gunData, property, state);
                applied = true;
            } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
                // Bind every property independently so a renamed optional property
                // cannot disable the remaining SuperbWarfare properties.
            }
        }
        return applied;
    }

    private static List<String> gunProperties(GunModifierState state) {
        java.util.ArrayList<String> properties = new java.util.ArrayList<>();
        properties.add("RPM");
        properties.addAll(GUN_DURATION_PROPERTIES);
        if (state.magazineRate > 1.0001D) {
            properties.add("MAGAZINE");
        }
        return properties;
    }

    private static void setGunPropertyModifier(Object gunData, String propertyName, GunModifierState state)
            throws ReflectiveOperationException {
        Class<?> propClass = Class.forName("com.atsuishio.superbwarfare.data.gun.GunProp");
        Class<?> contextClass = Class.forName("com.atsuishio.superbwarfare.data.Prop$PropModifyContext");
        Object property = propClass.getField(propertyName).get(null);
        Object context = Proxy.newProxyInstance(contextClass.getClassLoader(), new Class<?>[] {contextClass},
                (proxy, method, args) -> {
                    if ("apply".equals(method.getName()) && args != null && args.length == 3
                            && args[2] instanceof Number number) {
                        return state.modify(propertyName, number.intValue());
                    }
                    if ("toString".equals(method.getName())) return "DFS SuperbWarfare " + propertyName;
                    if ("hashCode".equals(method.getName())) return System.identityHashCode(proxy);
                    if ("equals".equals(method.getName())) return args != null && args.length == 1 && proxy == args[0];
                    return null;
                });
        gunData.getClass().getMethod("setTempProperty", propClass, contextClass)
                .invoke(gunData, property, context);
    }

    private static void reduceGunTimers(Object gunData, GunModifierState state) {
        reducePublicTimers(readField(gunData, "bolt"), extraTicks(state.boltRate), 2);
        reducePublicTimers(readField(gunData, "shoot"), extraTicks(state.fireRate));
        reducePublicTimers(readField(gunData, "fire"), extraTicks(state.fireRate));
    }

    private static void reducePublicTimers(Object owner, int extraTicks) {
        reducePublicTimers(owner, extraTicks, 0);
    }

    private static void reducePublicTimers(Object owner, int extraTicks, int minimumBeforeNativeTick) {
        if (owner == null || extraTicks <= 0) {
            return;
        }
        for (Class<?> type = owner.getClass(); type != null; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (!field.getType().getName().endsWith(".Timer") || !field.trySetAccessible()) {
                    continue;
                }
                try {
                    Object timer = field.get(owner);
                    int current = ((Number) timer.getClass().getMethod("get").invoke(timer)).intValue();
                    if (current > 0) {
                        timer.getClass().getMethod("set", int.class)
                                .invoke(timer, Math.max(minimumBeforeNativeTick, current - extraTicks));
                    }
                } catch (ReflectiveOperationException | RuntimeException ignored) {
                }
            }
        }
    }

    private static boolean applyGunOverride(Object gunData, GunModifierState state) {
        ItemStack stack = gunStack(gunData);
        CompoundTag gunTag = gunDataTag(gunData);
        if (stack == null || gunTag == null) {
            return false;
        }

        CompoundTag root = stack.getOrCreateTag();
        CompoundTag compat = root.getCompound(DFS_OVERRIDE_TAG);
        if (!state.hasChange()) {
            restoreGunOverride(root, gunTag, compat);
            return true;
        }

        String current = gunTag.getString(GUN_OVERRIDE_TAG);
        String applied = compat.getString(DFS_OVERRIDE_APPLIED);
        if (!compat.getBoolean(DFS_OVERRIDE_ACTIVE)) {
            compat.putBoolean(DFS_OVERRIDE_ACTIVE, true);
            compat.putString(DFS_OVERRIDE_BASE, current);
        } else if (!current.equals(applied)) {
            // Preserve a third-party/user override that changed while DFS was active.
            compat.putString(DFS_OVERRIDE_BASE, current);
        }

        String profile = state.profile();
        if (profile.equals(compat.getString(DFS_OVERRIDE_PROFILE)) && current.equals(applied)) {
            return true;
        }

        String base = compat.getString(DFS_OVERRIDE_BASE);
        writeGunOverride(gunTag, base);
        JsonObject merged = parseOverride(base);
        putScaledGunProperty(merged, gunData, "RPM", state.fireRate, true);
        for (String property : GUN_DURATION_PROPERTIES) {
            putScaledGunProperty(merged, gunData, property, state.multiplier(property), false);
        }
        if (state.magazineRate > 1.0001D) {
            putScaledGunProperty(merged, gunData, "MAGAZINE", state.magazineRate, true);
        }
        putScaledSeekWeaponInfo(merged, gunData, state.aimRate);

        String updated = GSON.toJson(merged);
        writeGunOverride(gunTag, updated);
        compat.putString(DFS_OVERRIDE_APPLIED, updated);
        compat.putString(DFS_OVERRIDE_PROFILE, profile);
        root.put(DFS_OVERRIDE_TAG, compat);
        return true;
    }

    private static void putScaledGunProperty(JsonObject override, Object gunData, String propertyName,
                                             double multiplier, boolean scaleUp) {
        Number current = readGunNumberProperty(gunData, propertyName);
        if (current == null) {
            return;
        }
        int value = current.intValue();
        int scaled = scaleUp ? GunModifierState.scaledUp(value, multiplier)
                : scaledDuration(value, multiplier);
        override.addProperty(gunSerializationName(propertyName), scaled);
    }

    private static void putScaledSeekWeaponInfo(JsonObject override, Object gunData, double aimRate) {
        if (positiveMultiplier(aimRate) <= 1.0001D) {
            return;
        }
        Object info = readGunProperty(gunData, "SEEK_WEAPON_INFO");
        if (info == null) {
            return;
        }
        JsonElement serialized = GSON.toJsonTree(info);
        if (!serialized.isJsonObject()) {
            return;
        }
        JsonObject object = serialized.getAsJsonObject();
        JsonElement seekTime = object.get("SeekTime");
        if (seekTime == null || !seekTime.isJsonPrimitive() || !seekTime.getAsJsonPrimitive().isNumber()) {
            return;
        }
        object.addProperty("SeekTime", scaledDuration(seekTime.getAsInt(), aimRate));
        override.add("SeekWeaponInfo", object);
    }

    private static Number readGunNumberProperty(Object gunData, String propertyName) {
        Object value = readGunProperty(gunData, propertyName);
        return value instanceof Number number ? number : null;
    }

    private static Object readGunProperty(Object gunData, String propertyName) {
        try {
            Class<?> propClass = Class.forName("com.atsuishio.superbwarfare.data.gun.GunProp");
            Object property = propClass.getField(propertyName).get(null);
            return gunData.getClass().getMethod("get", propClass).invoke(gunData, property);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            return null;
        }
    }

    private static String gunSerializationName(String propertyName) {
        if ("RPM".equals(propertyName)) {
            return "RPM";
        }
        StringBuilder result = new StringBuilder();
        for (String part : propertyName.toLowerCase(java.util.Locale.ROOT).split("_")) {
            if (!part.isEmpty()) {
                result.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
            }
        }
        return result.toString();
    }

    private static JsonObject parseOverride(String raw) {
        if (raw == null || raw.isBlank()) {
            return new JsonObject();
        }
        try {
            JsonElement parsed = JsonParser.parseString(raw);
            return parsed.isJsonObject() ? parsed.getAsJsonObject() : new JsonObject();
        } catch (RuntimeException ignored) {
            return new JsonObject();
        }
    }

    private static ItemStack gunStack(Object gunData) {
        Object value = invokeNoArg(gunData, "stack");
        if (!(value instanceof ItemStack)) {
            value = invokeNoArg(gunData, "getStack");
        }
        if (!(value instanceof ItemStack)) {
            value = readField(gunData, "stack");
        }
        return value instanceof ItemStack stack ? stack : null;
    }

    private static CompoundTag gunDataTag(Object gunData) {
        Object value = invokeNoArg(gunData, "data");
        if (!(value instanceof CompoundTag)) {
            value = readField(gunData, "gunDataTag");
        }
        if (!(value instanceof CompoundTag)) {
            ItemStack stack = gunStack(gunData);
            CompoundTag root = stack == null ? null : stack.getTag();
            value = root == null ? null : root.getCompound(GUN_DATA_TAG);
        }
        return value instanceof CompoundTag tag ? tag : null;
    }

    private static void restoreGunOverride(Object gunData) {
        ItemStack stack = gunStack(gunData);
        CompoundTag gunTag = gunDataTag(gunData);
        if (stack == null || gunTag == null || stack.getTag() == null) {
            return;
        }
        CompoundTag root = stack.getTag();
        restoreGunOverride(root, gunTag, root.getCompound(DFS_OVERRIDE_TAG));
    }

    private static void restoreGunOverride(CompoundTag root, CompoundTag gunTag, CompoundTag compat) {
        if (!compat.getBoolean(DFS_OVERRIDE_ACTIVE)) {
            return;
        }
        String current = gunTag.getString(GUN_OVERRIDE_TAG);
        if (current.equals(compat.getString(DFS_OVERRIDE_APPLIED))) {
            writeGunOverride(gunTag, compat.getString(DFS_OVERRIDE_BASE));
        }
        root.remove(DFS_OVERRIDE_TAG);
    }

    private static void writeGunOverride(CompoundTag gunTag, String value) {
        if (value == null || value.isBlank()) {
            gunTag.remove(GUN_OVERRIDE_TAG);
        } else {
            gunTag.putString(GUN_OVERRIDE_TAG, value);
        }
    }

    private static void invokeMapSetter(Object object, String name, Map<?, ?> value) {
        try {
            object.getClass().getMethod(name, Map.class).invoke(object, value);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }
    }

    private static void clearLegacyGunStop(ItemStack stack, Object gunData) {
        CompoundTag stackTag = stack.getTag();
        if (stackTag != null && stackTag.getBoolean(FORCED_GUN_STOP)) {
            setGunBooleanValue(gunData, "stopped", false);
            setGunBooleanValue(gunData, "forceStop", false);
            stackTag.remove(FORCED_GUN_STOP);
        }
    }

    private static void updateGunDisabledState(Player player, ItemStack stack, Object gunData, boolean disabled) {
        clearLegacyGunStop(stack, gunData);
        if (disabled) {
            player.getCooldowns().addCooldown(stack.getItem(), 2);
            player.stopUsingItem();
        }
    }

    private static void setGunBooleanValue(Object gunData, String fieldName, boolean value) {
        try {
            Object holder = gunData.getClass().getField(fieldName).get(gunData);
            holder.getClass().getMethod("set", boolean.class).invoke(holder, value);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }
    }

    private static Boolean readGunBooleanValue(Object gunData, String fieldName) {
        try {
            Object holder = gunData.getClass().getField(fieldName).get(gunData);
            Object value = holder.getClass().getMethod("get").invoke(holder);
            return value instanceof Boolean bool ? bool : null;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    private static Number readGunNumberValue(Object gunData, String fieldName) {
        try {
            Object holder = gunData.getClass().getField(fieldName).get(gunData);
            Object value = holder.getClass().getMethod("get").invoke(holder);
            return value instanceof Number number ? number : null;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    private static boolean numberAboveZero(Number value) {
        return value != null && value.doubleValue() > 0.0D;
    }

    private static void refillGunNumber(Object gunData, String fieldName, int desired) {
        Number currentValue = readGunNumberValue(gunData, fieldName);
        if (currentValue == null || desired <= 0 || currentValue.longValue() == desired) {
            return;
        }
        setGunNumberValue(gunData, fieldName, desired);
    }

    private static void refillCurrentGunAmmo(Object gunData, int desired) {
        if (desired <= 0) {
            return;
        }
        Number current = readGunNumberValue(gunData, "ammo");
        boolean changed = current != null && current.intValue() != desired;
        if (changed) {
            setGunNumberValue(gunData, "ammo", desired);
        }
        boolean reloading = Boolean.TRUE.equals(invokeNoArg(gunData, "reloading"));
        if (reloading) {
            invokeNoArg(gunData, "resetStatus");
        }
        if (changed || reloading) {
            invokeNoArg(gunData, "save");
        }
    }

    private static void setGunNumberValue(Object gunData, String fieldName, Number value) {
        try {
            Object holder = gunData.getClass().getField(fieldName).get(gunData);
            for (Method method : holder.getClass().getMethods()) {
                if (!"set".equals(method.getName()) || method.getParameterCount() != 1) {
                    continue;
                }
                Class<?> parameter = method.getParameterTypes()[0];
                Object converted;
                if (parameter == int.class || parameter == Integer.class) converted = value.intValue();
                else if (parameter == long.class || parameter == Long.class) converted = value.longValue();
                else if (parameter == float.class || parameter == Float.class) converted = value.floatValue();
                else if (parameter == double.class || parameter == Double.class) converted = value.doubleValue();
                else continue;
                method.invoke(holder, converted);
                return;
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }
    }

    private static boolean isGun(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !isLoaded()) {
            return false;
        }
        try {
            return Class.forName("com.atsuishio.superbwarfare.item.gun.GunItem")
                    .isInstance(stack.getItem());
        } catch (ClassNotFoundException | RuntimeException | LinkageError ignored) {
            return false;
        }
    }

    private static boolean hasStructuredVehicleWeapons(Entity entity) {
        Object rawMap = invokeNoArg(entity, "getGunDataMap");
        if (rawMap instanceof Map<?, ?> map && !map.isEmpty()) {
            return true;
        }
        Object weapons = invokeNoArg(entity, "getAllWeapons");
        if (weapons == null) {
            weapons = readField(entity, "availableWeapons");
        }
        return weapons != null && weapons.getClass().isArray() && Array.getLength(weapons) > 0;
    }

    private static void adjustClientAim(ItemStack stack, double aimRate, double aimPenalty) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        try {
            Class<?> gunItemClass = Class.forName("com.atsuishio.superbwarfare.item.gun.GunItem");
            if (!gunItemClass.isInstance(stack.getItem())) {
                return;
            }
            Class<?> handler = clientEventHandlerClass();
            if (handler == null || !readStaticBoolean(handler, "zoom")) {
                return;
            }
            Field zoomTime = findField(handler, "zoomTime");
            if (zoomTime == null) {
                return;
            }
            double current = readNumericField(zoomTime, null);
            Object gunData = Class.forName("com.atsuishio.superbwarfare.data.gun.GunData")
                    .getMethod("from", ItemStack.class).invoke(null, stack);
            Number weightValue = readGunNumberProperty(gunData, "WEIGHT");
            double weight = weightValue == null ? 1.0D : Math.max(0.0D, weightValue.doubleValue());
            double normalAdvance = 0.03D * 7.0D / (weight + 2.0D);
            double effectiveAimRate = positiveMultiplier(aimRate) / positiveMultiplier(aimPenalty);
            double adjustment = normalAdvance * (effectiveAimRate - 1.0D);
            writeNumericField(zoomTime, null, Math.max(0.0D, Math.min(1.0D, current + adjustment)));
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            // SuperbWarfare is optional and moved its client event class between releases.
        }
    }

    private static void adjustClientVehicleTimer(double fireRate) {
        try {
            Class<?> handler = clientEventHandlerClass();
            if (handler == null) {
                return;
            }
            Field field = findField(handler, "clientTimerVehicle");
            if (field != null) {
                advanceClientTimer(field.get(null), fireRate);
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            // Client-only timer fields differ between optional SuperbWarfare versions.
        }
    }

    private static void adjustClientGunTimer(ItemStack stack, double fireRate) {
        if (stack == null || stack.isEmpty() || positiveMultiplier(fireRate) <= 1.0001D) {
            return;
        }
        try {
            Class<?> gunItemClass = Class.forName("com.atsuishio.superbwarfare.item.gun.GunItem");
            Class<?> handler = clientEventHandlerClass();
            if (!gunItemClass.isInstance(stack.getItem()) || handler == null) {
                return;
            }
            for (String name : List.of("clientTimer", "clientTimerGun")) {
                Field field = findField(handler, name);
                if (field != null) {
                    advanceClientTimer(field.get(null), fireRate);
                    return;
                }
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            // Optional releases may keep the handheld fire gate entirely server-side.
        }
    }

    private static void advanceClientTimer(Object timer, double fireRate) throws ReflectiveOperationException {
        if (timer == null || !((Boolean) timer.getClass().getMethod("started").invoke(timer))) {
            return;
        }
        long current = ((Number) timer.getClass().getMethod("getProgress").invoke(timer)).longValue();
        long adjustment = Math.round(50.0D * (positiveMultiplier(fireRate) - 1.0D));
        timer.getClass().getMethod("setProgress", long.class)
                .invoke(timer, Math.max(0L, current + adjustment));
    }

    private static Class<?> clientEventHandlerClass() {
        for (String name : List.of(
                "com.atsuishio.superbwarfare.event.ClientEventHandler",
                "com.atsuishio.superbwarfare.client.ClientEventHandler",
                "com.atsuishio.superbwarfare.client.event.ClientEventHandler")) {
            try {
                return Class.forName(name);
            } catch (ClassNotFoundException | LinkageError ignored) {
            }
        }
        return null;
    }

    private static void setClientHandlerEntity(Class<?> handler, String fieldName, Entity value)
            throws ReflectiveOperationException {
        Field field = findField(handler, fieldName);
        if (field == null) {
            return;
        }
        field.setAccessible(true);
        if (field.getType().isAssignableFrom(value.getClass())) {
            field.set(null, value);
        }
    }

    private static Field findField(Class<?> type, String name) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            try {
                Field field = current.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {
            }
        }
        return null;
    }

    private static boolean readStaticBoolean(Class<?> type, String name) throws ReflectiveOperationException {
        Field field = findField(type, name);
        Object value = field == null ? null : field.get(null);
        return value instanceof Boolean flag && flag;
    }

    private static double readNumericField(Field field, Object owner) throws IllegalAccessException {
        Object value = field.get(owner);
        return value instanceof Number number ? number.doubleValue() : 0.0D;
    }

    private static void writeNumericField(Field field, Object owner, double value) throws IllegalAccessException {
        Class<?> type = field.getType();
        if (type == float.class || type == Float.class) field.set(owner, (float) value);
        else if (type == int.class || type == Integer.class) field.set(owner, (int) Math.round(value));
        else if (type == long.class || type == Long.class) field.set(owner, Math.round(value));
        else field.set(owner, value);
    }

    private static int extraTicks(double multiplier) {
        return Math.max(0, (int) Math.ceil(positiveMultiplier(multiplier)) - 1);
    }

    private static double positiveMultiplier(double multiplier) {
        return Double.isFinite(multiplier) && multiplier > 0.0D ? multiplier : 1.0D;
    }

    private static final class GunModifierState {
        private volatile double fireRate = 1.0D;
        private volatile double reloadRate = 1.0D;
        private volatile double aimRate = 1.0D;
        private volatile double boltRate = 1.0D;
        private volatile double magazineRate = 1.0D;
        private volatile boolean autoRefill;

        private int modify(String propertyName, int value) {
            if ("RPM".equals(propertyName)) {
                return scaledUp(value, fireRate);
            }
            if ("MAGAZINE".equals(propertyName)) {
                return scaledUp(value, magazineRate);
            }
            return scaledDuration(value, multiplier(propertyName));
        }

        private double multiplier(String propertyName) {
            if ("SHOOT_DELAY".equals(propertyName) || "SHOOT_DELAY_TIME".equals(propertyName)
                    || "BURST_COOLDOWN".equals(propertyName)) {
                return fireRate;
            }
            if ("BOLT_ACTION_TIME".equals(propertyName)) {
                return boltRate;
            }
            if ("SEEK_TIME".equals(propertyName)) {
                return aimRate;
            }
            return reloadRate;
        }

        private boolean hasChange() {
            return fireRate > 1.0001D || reloadRate > 1.0001D || aimRate > 1.0001D
                    || boltRate > 1.0001D || magazineRate > 1.0001D || autoRefill;
        }

        private String profile() {
            return fireRate + ":" + reloadRate + ":" + aimRate + ":" + boltRate + ":"
                    + magazineRate + ":" + autoRefill;
        }

        private static int scaledUp(int value, double multiplier) {
            if (value <= 0) {
                return value;
            }
            return (int) Math.min(Integer.MAX_VALUE, Math.max(1.0D, Math.ceil(value * multiplier)));
        }
    }

    private static int scaledDuration(int value, double multiplier) {
        return value <= 0 ? value : Math.max(1, (int) Math.ceil(value / positiveMultiplier(multiplier)));
    }

    public static boolean isLoaded() {
        return ModList.get().isLoaded(MOD_ID);
    }

    private static Class<?> vehicleClass() {
        if (!isLoaded()) {
            return null;
        }
        if (!vehicleClassResolved) {
            synchronized (SuperbWarfareCompat.class) {
                if (!vehicleClassResolved) {
                    try {
                        vehicleClass = Class.forName(VEHICLE_ENTITY_CLASS);
                        getMaxHealthMethod = vehicleClass.getMethod("getMaxHealth");
                        getHealthMethod = vehicleClass.getMethod("getHealth");
                        setHealthMethod = vehicleClass.getMethod("setHealth", float.class);
                    } catch (ReflectiveOperationException | LinkageError ignored) {
                        vehicleClass = null;
                        getMaxHealthMethod = null;
                        getHealthMethod = null;
                        setHealthMethod = null;
                    }
                    vehicleClassResolved = true;
                }
            }
        }
        return vehicleClass;
    }

    private static float maxHealth(Entity entity) {
        Method method = getMaxHealthMethod;
        if (method == null || !isVehicle(entity)) {
            return 0.0F;
        }
        try {
            Object value = method.invoke(entity);
            return value instanceof Number number ? number.floatValue() : 0.0F;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return 0.0F;
        }
    }

    private static void repairVehicle(Entity entity) {
        if (!isVehicle(entity) || getHealthMethod == null || setHealthMethod == null) {
            return;
        }
        try {
            Object maxValue = getMaxHealthMethod.invoke(entity);
            Object currentValue = getHealthMethod.invoke(entity);
            if (!(maxValue instanceof Number maxNumber) || !(currentValue instanceof Number currentNumber)) {
                return;
            }
            float max = maxNumber.floatValue();
            float current = currentNumber.floatValue();
            if (!Float.isFinite(max) || !Float.isFinite(current) || max <= 0.0F || current >= max) {
                return;
            }
            setHealthMethod.invoke(entity, Math.min(max, current + max * 0.04F));
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            // VehicleEntity is optional and its health API may differ between releases.
        }
    }

    private static Vec3 closestPoint(Vec3 point, AABB bounds) {
        double x = Math.max(bounds.minX, Math.min(point.x, bounds.maxX));
        double y = Math.max(bounds.minY, Math.min(point.y, bounds.maxY));
        double z = Math.max(bounds.minZ, Math.min(point.z, bounds.maxZ));
        return new Vec3(x, y, z);
    }

    private static boolean hasLineOfSight(ServerLevel level, Vec3 center, Vec3 target, Entity context) {
        if (center.distanceToSqr(target) < 1.0E-4D) {
            return true;
        }
        HitResult result = level.clip(new ClipContext(center, target, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, context));
        return result.getType() == HitResult.Type.MISS || result.getLocation().distanceToSqr(target) < 0.25D;
    }
}
