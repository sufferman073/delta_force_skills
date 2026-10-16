package com.rzy.dealt_force_skills.compat;

import com.rzy.dealt_force_skills.block.HvkAdvancedDisassemblyBeamEmitterBlockEntity;
import com.rzy.dealt_force_skills.block.HvkAdvancedStandardTemplateConstructorBlockEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import java.util.List;

/** A server-owned snapshot, published only by the local integrated server's JEI plugin. */
public final class HvkJeiRecipeCatalog {
    public record Input(List<ItemStack> choices, int count) {
        public Input { choices = choices.stream().map(ItemStack::copy).toList(); }
    }
    public record Entry(ResourceLocation id, ItemStack output, List<Input> inputs) {
        public Entry { output = output.copy(); inputs = List.copyOf(inputs); }
    }
    private static MinecraftServer owner;
    private static List<Entry> entries = List.of();
    private HvkJeiRecipeCatalog() { }

    public static List<Entry> entries(Level level) {
        return level.getServer() == owner ? entries : List.of();
    }

    public static void replace(MinecraftServer server, List<Entry> snapshot) {
        if (server == null || server.isDedicatedServer()) return;
        owner = server;
        entries = List.copyOf(snapshot);
        HvkAdvancedStandardTemplateConstructorBlockEntity.clearRecipeCatalog(server);
        HvkAdvancedDisassemblyBeamEmitterBlockEntity.clearRecipeCatalog(server);
        if (server.overworld() != null) {
            HvkAdvancedStandardTemplateConstructorBlockEntity.warmRecipeCatalog(server.overworld());
            HvkAdvancedDisassemblyBeamEmitterBlockEntity.warmRecipeCatalog(server.overworld());
        }
    }

    public static void clear(MinecraftServer server) {
        if (owner == server) { owner = null; entries = List.of(); }
    }
}
