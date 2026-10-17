package com.rzy.dealt_force_skills.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Explicit-name accessors for the packet listener fields touched by {@link HvkZiplineConnectionMixin}.
 * Explicit accessor names are remapped in the mixin refmap, which makes the production (reobfuscated)
 * build resolve the SRG field names correctly.
 */
@Mixin(value = ServerGamePacketListenerImpl.class, remap = false)
public interface HvkZiplineConnectionAccess {
    @Accessor("player")
    ServerPlayer dfs$player();

    @Accessor("clientIsFloating")
    void dfs$setClientIsFloating(boolean floating);
}
