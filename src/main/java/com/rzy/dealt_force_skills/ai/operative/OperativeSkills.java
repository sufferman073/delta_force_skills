package com.rzy.dealt_force_skills.ai.operative;

import com.rzy.dealt_force_skills.character.SkillSlot;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;

/** A start result means an action really began, rather than a player request was handled. */
public interface OperativeSkills {
    default net.minecraft.world.phys.Vec3 investigation() { return null; }

    default void remember(LivingEntity target, int ticks) { }
    default boolean isRevealed(LivingEntity target) { return false; }

    default double meleeIntervalTicks(OperativeHost host) { return 20; }

    void tick(OperativeHost host);

    double score(OperativeHost host, SkillSlot slot, LivingEntity target);

    boolean start(OperativeHost host, SkillSlot slot, LivingEntity target);

    boolean busy();

    /** Busy prevents another skill; only these locks suspend ordinary combat controls. */
    default boolean locksMovement() { return busy(); }
    default boolean locksWeapon() { return locksMovement(); }

    /** Server action identity; rendering never infers a roll from collision pose. */
    default com.rzy.dealt_force_skills.motion.TacticalMotion motion() { return com.rzy.dealt_force_skills.motion.TacticalMotion.NONE; }
    default int motionDuration() { return 10; }
    default net.minecraft.world.phys.Vec3 motionDirection() { return net.minecraft.world.phys.Vec3.ZERO; }

    default void refresh(OperativeHost host) { }

    void cancel(OperativeHost host);

    CompoundTag save();

    void load(CompoundTag tag);

    String describe();
}
