package com.rzy.dealt_force_skills.home;

import com.rzy.dealt_force_skills.registry.ModBlockEntities;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.*;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.energy.IEnergyStorage;
import java.util.*;

public final class HomeDevice extends BlockEntity {
    int radius, speed;
    UUID payer;
    boolean energy;
    String energyId = HomeEnergy.FE;
    Fluid selected = Fluids.WATER;
    private final List<BlockPos> machines = new ArrayList<>();
    private long lastScan = Long.MIN_VALUE;
    public HomeDevice(BlockPos pos, BlockState state) { super(ModBlockEntities.PERSONAL_DIMENSION.get(), pos, state); }
    int tier() { return ((PersonalDimensionBlock)getBlockState().getBlock()).tier; }
    int maxRadius() { return tier() == 0 ? 0 : tier() < 4 ? 16 : 25; }
    int maxSpeed() { return tier() == 0 ? 0 : 1 << tier(); }
    void configure(int range, int rate) { radius = Math.max(0, Math.min(range, maxRadius())); speed = Math.max(0, Math.min(rate, maxSpeed())); lastScan = Long.MIN_VALUE; setChanged(); }
    @Override protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries); tag.putInt("Radius", radius); tag.putInt("Speed", speed);
        if (payer != null) tag.putUUID("Payer", payer);
        tag.putBoolean("Energy", energy);
        tag.putString("EnergyType", energyId);
        tag.putString("Fluid", BuiltInRegistries.FLUID.getKey(selected).toString());
    }
    @Override protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries); radius = Math.max(0, Math.min(tag.getInt("Radius"), maxRadius()));
        speed = Math.max(0, Math.min(tag.getInt("Speed"), maxSpeed())); payer = tier() >= 4 && tag.hasUUID("Payer") ? tag.getUUID("Payer") : null;
        energy = tier() == 5 && tag.getBoolean("Energy");
        energyId = tag.contains("EnergyType", 8) ? tag.getString("EnergyType") : HomeEnergy.FE;
        ResourceLocation key = ResourceLocation.tryParse(tag.getString("Fluid"));
        selected = key == null ? Fluids.EMPTY : BuiltInRegistries.FLUID.get(key);
        if (selected == Fluids.EMPTY) selected = Fluids.WATER;
        lastScan = Long.MIN_VALUE;
    }
    static final class FluidsList {
        static final List<Fluid> ALL = BuiltInRegistries.FLUID.stream().filter(fluid -> fluid != Fluids.EMPTY && (!(fluid instanceof FlowingFluid flowing) || flowing.getSource() == fluid))
                .sorted(java.util.Comparator.comparing(fluid -> BuiltInRegistries.FLUID.getKey(fluid).toString())).toList();
    }
    static void tick(Level world, BlockPos origin, BlockState state, HomeDevice device) {
        if (!(world instanceof ServerLevel level) || device.radius == 0 || device.speed < 2 && device.payer == null) return;
        long now = world.getGameTime();
        if (device.lastScan == Long.MIN_VALUE || now - device.lastScan >= 20) device.scan(level, origin, now);
        var user = device.payer == null || now % 5 != 0 ? null : level.getServer().getPlayerList().getPlayer(device.payer);
        for (BlockPos pos : device.machines) {
            if (device.isRemoved()) return;
            if (!level.hasChunkAt(pos) || !level.isPositionEntityTicking(pos)) continue;
            BlockEntity machine = level.getBlockEntity(pos);
            if (machine == null || machine instanceof HomeDevice || machine.isRemoved()) continue;
            if (user != null) device.fill(level, pos, machine, user);
            if (device.speed > 1) HomeAcceleration.request(level, pos, machine, device.speed);
        }
        if (!device.isRemoved() && device.speed > 1) device.randomTicks(level, origin);
    }
    private void scan(ServerLevel level, BlockPos origin, long now) {
        machines.clear(); lastScan = now;
        for (int chunkX = (origin.getX()-radius)>>4; chunkX <= (origin.getX()+radius)>>4; chunkX++)
            for (int chunkZ = (origin.getZ()-radius)>>4; chunkZ <= (origin.getZ()+radius)>>4; chunkZ++) {
                var chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
                if (chunk != null) for (BlockPos pos : chunk.getBlockEntitiesPos())
                    if (!pos.equals(origin) && pos.distSqr(origin) <= radius * radius) machines.add(pos);
            }
    }
    private void randomTicks(ServerLevel level, BlockPos origin) {
        int width = 2 * radius + 1;
        double expected = (double)width * width * width * (speed - 1) * level.getGameRules().getInt(GameRules.RULE_RANDOMTICKING) / 4096.0;
        int samples = (int)Math.min(4096, expected + level.random.nextDouble());
        for (int step = 0; step < samples; step++) {
            BlockPos pos = origin.offset(level.random.nextInt(width)-radius, level.random.nextInt(width)-radius, level.random.nextInt(width)-radius);
            if (pos.distSqr(origin) > radius * radius || !level.hasChunkAt(pos) || !level.isPositionEntityTicking(pos)) continue;
            var state = level.getBlockState(pos);
            if (!state.hasBlockEntity() && state.isRandomlyTicking()) state.randomTick(level, pos, level.random);
        }
    }
    private static IFluidHandler handler(ServerLevel level, BlockPos pos, BlockEntity machine, Direction side) {
        return level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, pos, side);
    }
    private void fill(ServerLevel level, BlockPos pos, BlockEntity machine, ServerPlayer user) {
        if (tier() == 5 && energy) {
            HomeEnergy.supply(energyId, level, pos, machine, user);
            return;
        }
        Set<IFluidHandler> ports = Collections.newSetFromMap(new IdentityHashMap<>());
        for (int face = -1; face < 6; face++) {
            var port = handler(level, pos, machine, face < 0 ? null : Direction.from3DDataValue(face));
            if (port != null) ports.add(port);
        }
        fillPorts(ports, user);
    }
    static void fillFE(ServerLevel level, BlockPos pos, BlockEntity machine, ServerPlayer user) {
        Set<IEnergyStorage> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (int face = -1; face < 6; face++) {
            Direction side = face < 0 ? null : Direction.from3DDataValue(face);
            var port = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK, pos, side);
            if (port != null && seen.add(port) && supplyEnergy(port, user)) return;
        }
    }
    static boolean supplyEnergy(IEnergyStorage port, ServerPlayer user) {
        if (!port.canReceive()) return false;
        long capacity = port.getMaxEnergyStored();
        int offer = (int)Math.min(Math.max(0L, capacity - Math.max(0, port.getEnergyStored())), HomeFillBudget.offer(capacity, user));
        if (offer == 0) return false;
        int sent = port.receiveEnergy(offer, false);
        return HomeFillBudget.bill(capacity, offer, sent, user);
    }
    void fillPorts(Set<IFluidHandler> ports, ServerPlayer user) {
        if (ports.isEmpty()) return;
        Fluid fluid = selected;
        if (tier() == 4) {
            fluid = null;
            for (Fluid candidate : FluidsList.ALL) {
                boolean accepts = false;
                for (var port : ports) if (port.fill(new FluidStack(candidate, 1000), IFluidHandler.FluidAction.SIMULATE) > 0) { accepts = true; break; }
                if (accepts) { if (fluid != null) return; fluid = candidate; }
            }
            if (fluid == null) return;
        }
        FluidStack probe = new FluidStack(fluid, 1);
        for (var port : ports) {
            long capacity = 0, space = 0;
            for (int tank = 0; tank < port.getTanks(); tank++) {
                FluidStack stored = port.getFluidInTank(tank);
                if (!port.isFluidValid(tank, probe) || !stored.isEmpty() && stored.getFluid() != fluid) continue;
                int maximum = Math.max(0, port.getTankCapacity(tank));
                capacity += maximum;
                space += Math.max(0, maximum - Math.max(0, stored.getAmount()));
            }
            int offer = (int)Math.min(Integer.MAX_VALUE, Math.min(space, HomeFillBudget.offer(capacity, user)));
            if (offer <= 0) continue;
            int accepted = Math.min(offer, port.fill(new FluidStack(fluid, offer), IFluidHandler.FluidAction.SIMULATE));
            if (accepted > 0) {
                int sent = port.fill(new FluidStack(fluid, accepted), IFluidHandler.FluidAction.EXECUTE);
                HomeFillBudget.bill(capacity, accepted, sent, user);
            }
        }
    }
}
