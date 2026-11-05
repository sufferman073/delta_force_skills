package com.rzy.dealt_force_skills.ai.operative;

import com.rzy.dealt_force_skills.entity.OperativeEntity;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** One sparse impact cue per victim, shared by every operative firing at it. */
public final class OperativeHitFeedback {
    private static final String LAST = "dealt_force_skills.operative_last_impact";
    public enum Region { HEAD, TORSO, LIMBS }
    private OperativeHitFeedback() { }

    public static Region region(LivingEntity target, Vec3 impact, boolean headshot) {
        if (headshot) return Region.HEAD;
        var box = target.getBoundingBox();
        double height = (impact.y - box.minY) / Math.max(.01, box.getYsize());
        double yaw = Math.toRadians(target.yBodyRot);
        double side = Math.abs((impact.x - box.getCenter().x) * Math.cos(yaw)
                + (impact.z - box.getCenter().z) * Math.sin(yaw)) / Math.max(.01, box.getXsize() * .5);
        return height < .4 || height < .75 && side > .65 ? Region.LIMBS : Region.TORSO;
    }

    public static boolean emit(OperativeEntity attacker, LivingEntity target, Entity bullet, boolean headshot) {
        if (attacker.level().isClientSide) return false;
        var allegiance = new OperativeAllegiance(); allegiance.capture(attacker);
        if (allegiance.isAlly(attacker, target)) return false;
        long now = attacker.serverLevel().getGameTime();
        var tag = target.getPersistentData();
        if (tag.contains(LAST) && now >= tag.getLong(LAST) && now - tag.getLong(LAST) < 4) return false;
        Vec3 velocity = bullet.getDeltaMovement();
        var box = target.getBoundingBox();
        Vec3 impact = box.clip(bullet.position().subtract(velocity), bullet.position().add(velocity))
                .orElseGet(() -> new Vec3(
                        net.minecraft.util.Mth.clamp(bullet.getX(), box.minX, box.maxX),
                        net.minecraft.util.Mth.clamp(bullet.getY(), box.minY, box.maxY),
                        net.minecraft.util.Mth.clamp(bullet.getZ(), box.minZ, box.maxZ)));
        Region region = region(target, impact, headshot);
        int rgb = switch (region) { case HEAD -> 0xF33131; case TORSO -> 0xFF8C26; case LIMBS -> 0xFFE34A; };
        var color = new Vector3f(((rgb >> 16) & 255) / 255F, ((rgb >> 8) & 255) / 255F, (rgb & 255) / 255F);
        attacker.serverLevel().sendParticles(new DustParticleOptions(color, .65F), impact.x, impact.y, impact.z, 3, .035, .035, .035, 0);
        tag.putLong(LAST, now);
        return true;
    }
}
