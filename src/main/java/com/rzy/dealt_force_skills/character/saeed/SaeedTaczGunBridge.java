package com.rzy.dealt_force_skills.character.saeed;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.entity.ShootResult;
import com.tacz.guns.resource.index.CommonGunIndex;
import com.tacz.guns.resource.modifier.AttachmentCacheProperty;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public final class SaeedTaczGunBridge {
    private SaeedTaczGunBridge() {
    }

    public static boolean shoot(LivingEntity shooter, SaeedGuardType type, float pitch, float yaw) {
        ItemStack gun = shooter.getMainHandItem();
        if (!(shooter instanceof IGunOperator operator) || !SaeedTaczEquipment.isTaczGun(gun)) {
            return false;
        }
        try {
            boolean nativeGuard = shooter instanceof com.rzy.dealt_force_skills.ai.operative.NativeSaeedGuardEntity;
            if (!nativeGuard) SaeedTaczEquipment.primeForShot(type, gun);
            prepareOperator(operator, shooter, gun);
            operator.aim(true);
            ShootResult result = operator.shoot(() -> pitch, () -> yaw);
            if (result == ShootResult.SUCCESS) {
                return true;
            }
            if (nativeGuard && result == ShootResult.NOT_DRAW) operator.draw(shooter::getMainHandItem);
            if (result == ShootResult.NO_AMMO) {
                if (nativeGuard) {
                    if (!operator.getSynReloadState().getStateType().isReloading()) operator.reload();
                    return false;
                }
                SaeedTaczEquipment.primeForShot(type, gun);
                result = operator.shoot(() -> pitch, () -> yaw);
                return result == ShootResult.SUCCESS;
            }
            if (result == ShootResult.NEED_BOLT) {
                operator.bolt();
            }
            return false;
        } catch (LinkageError | RuntimeException ignored) {
            return false;
        }
    }

    private static void prepareOperator(IGunOperator operator, LivingEntity shooter, ItemStack gun) {
        if (operator.getDataHolder().currentGunItem == null) {
            operator.initialData();
            operator.getDataHolder().currentGunItem = shooter::getMainHandItem;
        }

        ResourceLocation gunId = SaeedTaczEquipment.gunId(gun);
        Optional<CommonGunIndex> gunIndex = gunId == null ? Optional.empty() : TimelessAPI.getCommonGunIndex(gunId);
        if (gunIndex.isPresent()) {
            AttachmentCacheProperty cacheProperty = new AttachmentCacheProperty();
            cacheProperty.eval(gun, gunIndex.get().getGunData());
            operator.updateCacheProperty(cacheProperty);
        }
    }
}
