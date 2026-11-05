package com.rzy.dealt_force_skills.character.vyron;

import com.rzy.dealt_force_skills.entity.VyronMagneticBombEntity;
import com.rzy.dealt_force_skills.entity.VyronTigerCannonEntity;
import com.rzy.dealt_force_skills.registry.ModEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Shared launch geometry for player skills and native living hosts. */
public final class VyronProjectiles {
    private VyronProjectiles() { }

    public static boolean throwMagneticBomb(ServerLevel level, LivingEntity owner, boolean highThrow) {
        VyronMagneticBombEntity bomb = new VyronMagneticBombEntity(ModEntities.VYRON_MAGNETIC_BOMB.get(), level, owner);
        Vec3 look = owner.getLookAngle().normalize();
        Vec3 start = owner.getEyePosition().add(look.scale(0.62D));
        bomb.setPos(start.x, start.y - 0.1D, start.z);
        bomb.setDeltaMovement(look.scale(highThrow ? 1.75D : 1.08D).add(0.0D, highThrow ? 0.32D : 0.06D, 0.0D));
        bomb.setYRot(owner.getYRot());
        bomb.setXRot(owner.getXRot());
        return level.addFreshEntity(bomb);
    }

    public static boolean fireTigerCannon(ServerLevel level, LivingEntity owner) {
        VyronTigerCannonEntity shell = new VyronTigerCannonEntity(ModEntities.VYRON_TIGER_CANNON.get(), level, owner);
        Vec3 look = owner.getLookAngle().normalize();
        Vec3 start = owner.getEyePosition().add(look.scale(0.8D));
        shell.setPos(start.x, start.y - 0.08D, start.z);
        shell.setDeltaMovement(look.scale(1.9D).add(0.0D, 0.08D, 0.0D));
        shell.setYRot(owner.getYRot());
        shell.setXRot(owner.getXRot());
        return level.addFreshEntity(shell);
    }
}
