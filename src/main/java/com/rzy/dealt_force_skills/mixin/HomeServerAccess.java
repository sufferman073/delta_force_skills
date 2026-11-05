package com.rzy.dealt_force_skills.mixin;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.progress.ChunkProgressListenerFactory;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import java.util.concurrent.Executor;

/** Access needed to construct additional levels using the server's own services. */
@Mixin(MinecraftServer.class)
public interface HomeServerAccess {
    @Accessor("executor") Executor homeExecutor();
    @Accessor("storageSource") LevelStorageSource.LevelStorageAccess homeStorage();
    @Accessor("progressListenerFactory") ChunkProgressListenerFactory homeProgress();
}
