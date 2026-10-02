package com.rzy.dealt_force_skills.registry;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.SimpleParticleType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, DealtForceSkillsMod.MODID);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> D_WOLF_LARGE_SMOKE =
            PARTICLES.register("d_wolf_large_smoke", () -> new SimpleParticleType(false));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> GIZMO_LARGE_SMOKE =
            PARTICLES.register("gizmo_large_smoke", () -> new SimpleParticleType(false));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> STINGER_LARGE_SMOKE =
            PARTICLES.register("stinger_large_smoke", () -> new SimpleParticleType(false));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> STINGER_HEALING_SMOKE =
            PARTICLES.register("stinger_healing_smoke", () -> new SimpleParticleType(false));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> TOXIK_LARGE_SMOKE =
            PARTICLES.register("toxik_large_smoke", () -> new SimpleParticleType(false));
}
