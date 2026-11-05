package com.rzy.dealt_force_skills.character.wildfire;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;

public final class WildfireDamageSource extends DamageSource {
    private final boolean core;
    public WildfireDamageSource(ServerLevel level,Entity direct,Entity owner,boolean core) {
        super(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(
                ResourceKey.create(Registries.DAMAGE_TYPE,new ResourceLocation("dealt_force_skills",core?"wildfire_impact":"wildfire_grenade"))),direct,owner);
        this.core=core;
    }
    public boolean core() {return core;}
    public double armorPenetration() {return core?1:WildfireConfig.fraction("grenade.armor_penetration",.5);}
    public double protectionPenetration() {return core?0:WildfireConfig.fraction("grenade.protection_penetration",.5);}
    public double equipmentPenetration() {return core?1:WildfireConfig.fraction("grenade.equipment_penetration",.5);}
}
