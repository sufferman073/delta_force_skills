package com.rzy.dealt_force_skills.home;

import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import java.lang.reflect.*;
import java.util.*;

public final class HomeEnergy {
    public static final String FE = "dealt_force_skills:fe";
    @FunctionalInterface public interface Receiver {
        int insert(ServerLevel level, BlockPos pos, BlockEntity machine, Direction side, int offer) throws ReflectiveOperationException;
    }
    @FunctionalInterface public interface Capacity {
        long maximum(ServerLevel level, BlockPos pos, BlockEntity machine, Direction side) throws ReflectiveOperationException;
    }
    @FunctionalInterface public interface LongReceiver {
        long insert(ServerLevel level, BlockPos pos, BlockEntity machine, Direction side, long offer) throws ReflectiveOperationException;
    }
    @FunctionalInterface interface Transfer {
        void supply(ServerLevel level, BlockPos pos, BlockEntity machine, ServerPlayer user);
    }
    record Type(String id, Component name, Transfer transfer) { }
    private static final Map<String, Type> types = new TreeMap<>();
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("dealt_force_skills/home_energy");
    private static final Set<String> warnings = new HashSet<>();
    private static boolean initialized;

    public static synchronized void register(ResourceLocation id, Component name, Receiver receiver) {
        Objects.requireNonNull(receiver);
        if (!(receiver instanceof Capacity capacity)) {
            logger.warn("Home energy provider {} has no capacity; percentage charging is unavailable", id);
            return;
        }
        register(id, name, capacity, (level, pos, machine, side, offer) -> receiver.insert(level, pos, machine, side, (int)Math.min(Integer.MAX_VALUE, offer)));
    }
    public static synchronized void register(ResourceLocation id, Component name, Capacity capacity, LongReceiver receiver) {
        Objects.requireNonNull(capacity); Objects.requireNonNull(receiver);
        registerType(id, name, (level, pos, machine, user) -> supplyPorts(id.toString(), capacity, receiver, level, pos, machine, user));
    }
    private static void registerType(ResourceLocation id, Component name, Transfer transfer) {
        if (types.putIfAbsent(id.toString(), new Type(id.toString(), Objects.requireNonNull(name), transfer)) != null)
            throw new IllegalArgumentException("Duplicate home energy type: " + id);
    }
    private static synchronized void initialize() {
        if (initialized) return;
        registerType(ResourceLocation.tryParse(FE), Component.translatable("home.dealt_force_skills.energy"), HomeDevice::fillFE);
        for (String mod : List.of("mekanism", "ae2")) if (net.neoforged.fml.ModList.get().isLoaded(mod)) {
            try { if (mod.equals("mekanism")) registerMekanism(); else registerAE(); }
            catch (ReflectiveOperationException | LinkageError problem) { logger.warn("Unavailable home energy integration: " + mod, problem); }
        }
        initialized = true;
    }
    static List<Type> available() { initialize(); synchronized (HomeEnergy.class) { return List.copyOf(types.values()); } }
    static Type find(String id) { initialize(); synchronized (HomeEnergy.class) { return types.get(id); } }

    static void supply(String id, ServerLevel level, BlockPos pos, BlockEntity machine, ServerPlayer user) {
        Type type = find(id);
        if (type != null) type.transfer.supply(level, pos, machine, user);
    }
    private static void supplyPorts(String id, Capacity capacity, LongReceiver receiver, ServerLevel level, BlockPos pos, BlockEntity machine, ServerPlayer user) {
        for (int face = 0; face <= 6; face++) {
            Direction side = face == 6 ? null : Direction.from3DDataValue(face);
            try {
                long maximum = capacity.maximum(level, pos, machine, side);
                long offer = HomeFillBudget.offer(maximum, user);
                if (offer > 0 && HomeFillBudget.bill(maximum, offer, receiver.insert(level, pos, machine, side, offer), user)) return;
            } catch (ReflectiveOperationException | RuntimeException | LinkageError problem) {
                if (warnings.add(id)) logger.warn("Could not supply " + id + " to " + pos, problem);
                return;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void registerMekanism() throws ReflectiveOperationException {
        var caps = Class.forName("mekanism.common.capabilities.Capabilities").getField("STRICT_ENERGY").get(null);
        var cap = (net.neoforged.neoforge.capabilities.BlockCapability<?, Direction>)caps.getClass().getMethod("block").invoke(caps);
        var action = Class.forName("mekanism.api.Action");
        Object execute = action.getField("EXECUTE").get(null);
        var handler = Class.forName("mekanism.api.energy.IStrictEnergyHandler");
        var insert = handler.getMethod("insertEnergy", long.class, action);
        var count = handler.getMethod("getEnergyContainerCount");
        var maximum = handler.getMethod("getMaxEnergy", int.class);
        Capacity capacity = (level, pos, machine, side) -> {
            if (side == null) return 0;
            var port = level.getCapability(cap, pos, side);
            if (port == null) return 0;
            long total = 0; int size = (int)count.invoke(port);
            for (int index = 0; index < size; index++) {
                long limit = (long)maximum.invoke(port, index);
                if (limit > Long.MAX_VALUE - total) return Long.MAX_VALUE;
                total += Math.max(0L, limit);
            }
            return total;
        };
        register(ResourceLocation.tryParse("mekanism:joules"), Component.translatable("home.dealt_force_skills.joules"), capacity, (level, pos, machine, side, offer) -> {
            if (side == null) return 0;
            var port = level.getCapability(cap, pos, side);
            if (port == null) return 0;
            return offer - (long)insert.invoke(port, offer, execute);
        });
    }
    private static long wholeAccepted(long offered, Object remaining) {
        return Math.max(0, Math.min(offered, (long)Math.floor(offered - ((Number)remaining).doubleValue())));
    }
    private static void registerAE() throws ReflectiveOperationException {
        var storage = Class.forName("appeng.api.networking.energy.IAEPowerStorage");
        var action = Class.forName("appeng.api.config.Actionable");
        var allowed = Class.forName("appeng.api.config.AccessRestriction").getMethod("isAllowInsertion");
        var flow = storage.getMethod("getPowerFlow");
        var accessible = storage.getMethod("isAEPublicPowerStorage");
        var insert = storage.getMethod("injectAEPower", double.class, action);
        Object simulate = action.getField("SIMULATE").get(null), execute = action.getField("MODULATE").get(null);
        var maxPower = storage.getMethod("getAEMaxPower");
        Capacity capacity = (level, pos, machine, side) -> side == null && storage.isInstance(machine)
                && (boolean)accessible.invoke(machine) && (boolean)allowed.invoke(flow.invoke(machine))
                ? (long)(double)maxPower.invoke(machine) : 0;
        register(ResourceLocation.tryParse("ae2:ae"), Component.translatable("home.dealt_force_skills.ae"), capacity, (level, pos, machine, side, offer) -> {
            if (side != null || !storage.isInstance(machine) || !(boolean)accessible.invoke(machine) || !(boolean)allowed.invoke(flow.invoke(machine))) return 0;
            long whole = wholeAccepted(offer, insert.invoke(machine, (double)offer, simulate));
            return whole == 0 ? 0 : wholeAccepted(whole, insert.invoke(machine, (double)whole, execute));
        });
    }
}
