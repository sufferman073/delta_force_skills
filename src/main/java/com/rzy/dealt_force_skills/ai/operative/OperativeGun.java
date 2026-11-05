package com.rzy.dealt_force_skills.ai.operative;

import com.rzy.dealt_force_skills.entity.OperativeEntity;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.entity.ShootResult;
import com.tacz.guns.api.item.IGun;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Uses TaCZ's living operator; AI reload supply belongs to the entity; item magazines still spend rounds. */
public final class OperativeGun {
    private ItemStack drawn = ItemStack.EMPTY;
    private ShootResult lastResult = ShootResult.NOT_GUN;
    private int reloadTicks, nextReloadAttempt;
    private boolean preferHead;
    private java.util.UUID aimTarget;

    public static boolean equipped(OperativeEntity host) {
        return IGun.getIGunOrNull(host.getMainHandItem()) != null
                && !(host.skills() instanceof NativeUndeadSkills || host.skills() instanceof NativeLexSkills)
                && !(host.skills() instanceof NativeNikaidouSkills hiro && hiro.weaponActive())
                && !(host.skills() instanceof NativeWitchSkills witch && witch.weaponActive())
                && !(com.rzy.dealt_force_skills.ai.operative.NativeCommanderSkills.inherited(host.skills(), NativeSinevaSkills.class) instanceof NativeSinevaSkills sineva && sineva.shieldFront());
    }

    public void tick(OperativeEntity host) {
        IGunOperator operator = IGunOperator.fromLivingEntity(host);
        ItemStack stack = host.getMainHandItem();
        if (!equipped(host)) { stop(operator); drawn = ItemStack.EMPTY; return; }
        IGun gun = IGun.getIGunOrNull(stack);
        if (TimelessAPI.getCommonGunIndex(gun.getGunId(stack)).isEmpty()) {
            stop(operator); lastResult = ShootResult.ID_NOT_EXIST; return;
        }
        if (drawn != stack) {
            stop(operator);
            operator.draw(host::getMainHandItem);
            drawn = stack;
            reloadTicks = nextReloadAttempt = 0;
        }
        boolean available = NativeRaptorSkills.available(host) && !OperativeSupport.downed(host)
                && !host.skills().locksWeapon();
        if (!available) { operator.aim(false); return; }
        supplyReserve(stack);
        com.rzy.dealt_force_skills.character.ghroth.GhrothTaczEnhancement.tickEnhancedGunRuntime(host);
        // Reload is maintenance, and must continue when retreating behind cover or losing a target.
        if (operator.getSynReloadState().getStateType().isReloading()) {
            reloadTicks++;
            double speed = com.rzy.dealt_force_skills.event.CommonEvents.effectiveTaczReloadMultiplier(host);
            var data = operator.getDataHolder();
            if (data.reloadTimestamp >= 0 && Double.isFinite(speed) && speed > 1)
                data.reloadTimestamp -= Math.round(50 * (speed - 1));
            if (reloadTicks > 600) { operator.cancelReload(); reloadTicks = 0; nextReloadAttempt = host.tickCount + 10; }
            return;
        }
        reloadTicks = 0;
        if (gun.getCurrentAmmoCount(stack) <= 0 && !gun.hasBulletInBarrel(stack)
                && reserve(host, gun, stack) && host.tickCount >= nextReloadAttempt) {
            operator.reload(); nextReloadAttempt = host.tickCount + 10;
        }
        LivingEntity target = host.getTarget();
        if (target == null || !host.isEnemy(target) || !host.withinCombatRange(target.position())
                || host.distanceToSqr(target) > Math.pow(effectiveRange(host), 2) || !clearShot(host, target)) {
            operator.aim(false); return;
        }
        if (!target.getUUID().equals(aimTarget)) { aimTarget = target.getUUID(); preferHead = host.getRandom().nextBoolean(); }
        Vec3 aim = firingPoint(host, host.getEyePosition(), target, preferHead);
        if (aim == null) { operator.aim(false); return; }
        var offset = aim.subtract(host.getEyePosition());
        float yaw = (float) (Math.toDegrees(Math.atan2(offset.z, offset.x)) - 90);
        float pitch = (float) -Math.toDegrees(Math.atan2(offset.y, offset.horizontalDistance()));
        // Navigation must retain its movement yaw so a gunner can turn and retreat.
        // Aim the visible body/head and the bullet independently from that walking direction.
        if (host.getNavigation().isDone()) host.setYRot(yaw);
        host.yBodyRot = yaw;
        host.setYHeadRot(yaw);
        host.setXRot(pitch);
        operator.aim(true);
        if (operator.getSynReloadState().getStateType().isReloading()) return;
        lastResult = operator.shoot(() -> pitch, () -> yaw);
        if (lastResult == ShootResult.SUCCESS) { NativeLexEvents.action(host); preferHead = host.getRandom().nextBoolean(); }
        if (lastResult == ShootResult.NEED_BOLT) operator.bolt();
        else if (lastResult == ShootResult.NO_AMMO && reserve(host, gun, stack)
                && host.tickCount >= nextReloadAttempt) { operator.reload(); nextReloadAttempt = host.tickCount + 10; }
        else if (lastResult == ShootResult.NOT_DRAW) operator.draw(host::getMainHandItem);
    }

    public void cancel(OperativeEntity host) { stop(IGunOperator.fromLivingEntity(host)); }
    private static void stop(IGunOperator operator) {
        operator.aim(false);
        if (operator.getSynReloadState().getStateType().isReloading()) operator.cancelReload();
    }
    public ShootResult lastResult() { return lastResult; }
    public boolean reloading(OperativeEntity host) { return IGunOperator.fromLivingEntity(host).getSynReloadState().getStateType().isReloading(); }
    public boolean hasAmmunition(OperativeEntity host) {
        ItemStack stack = host.getMainHandItem();
        IGun gun = IGun.getIGunOrNull(stack);
        return gun != null && TimelessAPI.getCommonGunIndex(gun.getGunId(stack)).isPresent();
    }

    /** Remove legacy dummy reserves so transferred guns use ordinary inventory ammunition. */
    public static void supplyReserve(ItemStack stack) {
        IGun gun = IGun.getIGunOrNull(stack);
        if (gun == null) return;
        var tag = stack.getTag();
        if (tag != null) { tag.remove("DummyAmmo"); tag.remove("MaxDummyAmmo"); }
    }

    public static void preparePlayerTransfer(ItemStack stack) {
        IGun gun = IGun.getIGunOrNull(stack);
        if (gun == null) return;
        var tag = stack.getTag();
        if (tag != null) { tag.remove("DummyAmmo"); tag.remove("MaxDummyAmmo"); }
    }

    private static boolean reserve(OperativeEntity host, IGun gun, ItemStack stack) {
        return host.isAlive();
    }

    public static double effectiveRange(OperativeEntity host) { return acquisitionRange(host) + 40; }

    public static double acquisitionRange(OperativeEntity host) { return Math.max(64, weaponRange(host)); }

    private static double weaponRange(LivingEntity host) {
        IGun gun = IGun.getIGunOrNull(host.getMainHandItem());
        String type = gun == null ? "" : TimelessAPI.getCommonGunIndex(gun.getGunId(host.getMainHandItem()))
                .map(index -> index.getType()).orElse("").toLowerCase(java.util.Locale.ROOT);
        if (type.contains("shotgun")) return 18;
        if (type.contains("sniper") || type.equals("mg") || type.contains("machine_gun")
                || type.contains("heavy") || type.contains("rpg") || type.contains("launcher")) return 80;
        if (type.contains("pistol")) return 40;
        return 56;
    }

    public static double preferredRange(LivingEntity host) {
        double range = weaponRange(host);
        return range <= 18 ? 15 : range >= 80 ? 58 : range <= 40 ? 30 : 42;
    }

    public static boolean clearShot(OperativeEntity host, LivingEntity target) {
        return attackPoint(host, host.getEyePosition(), target, true) != null;
    }

    /** Detection and actual bullets share this point; exposed legs are valid targets. */
    public static Vec3 attackPoint(OperativeEntity host, Vec3 start, LivingEntity target, boolean allies) {
        return exposedPoint(host, start, target, allies, new double[]{.85, .55, .22});
    }

    public static Vec3 firingPoint(OperativeEntity host, Vec3 start, LivingEntity target, boolean headFirst) {
        return exposedPoint(host, start, target, true, headFirst ? new double[]{.85, .55, .22} : new double[]{.55, .85, .22});
    }

    private static Vec3 exposedPoint(OperativeEntity host, Vec3 start, LivingEntity target, boolean allies, double[] heights) {
        var box = target.getBoundingBox();
        var points = new java.util.ArrayList<Vec3>();
        for (double height : heights) points.add(new Vec3(box.getCenter().x, box.minY + box.getYsize() * height, box.getCenter().z));
        double yaw = Math.toRadians(target.yBodyRot), side = Math.min(box.getXsize(), box.getZsize()) * .4;
        for (double height : new double[]{.6, .25}) for (int sign : new int[]{-1, 1})
            points.add(new Vec3(box.getCenter().x + Math.cos(yaw) * side * sign,
                    box.minY + box.getYsize() * height, box.getCenter().z + Math.sin(yaw) * side * sign));
        for (Vec3 end : points) {
            if (!unobstructed(host, start, end)) continue;
            boolean safe = true;
            if (allies) for (LivingEntity other : host.level().getEntitiesOfClass(LivingEntity.class,
                    new AABB(start, end).inflate(.35), e -> e != host && e != target && e.isAlive())) {
                if (!host.isEnemy(other) && (other.getBoundingBox().inflate(.35).contains(start)
                        || other.getBoundingBox().inflate(.35).clip(start, end).isPresent())) { safe = false; break; }
            }
            if (safe) return end;
        }
        return null;
    }

    public static boolean unobstructed(OperativeEntity host, Vec3 start, Vec3 end) {
        return host.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, host)).getType() == HitResult.Type.MISS;
    }
}
