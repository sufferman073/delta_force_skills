package com.rzy.dealt_force_skills.department;

import com.rzy.dealt_force_skills.character.nikaidou.NikaidouHiroStateManager;
import com.rzy.dealt_force_skills.character.nikaidou.NikaidouHiroWitchificationStateManager;
import com.rzy.dealt_force_skills.character.nikaidou.NikaidouHiroTool;
import com.rzy.dealt_force_skills.skill.SkillDamageHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/** Called only at the accepted tail of die, after all death-event listeners. */
public final class DepartmentNikaidou {
    private DepartmentNikaidou() { }
    private static final java.util.Set<LivingEntity> DEATHS=java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>());
    public static final String CORE_CAST="DepartmentCoreCast";
    public static void died(LivingEntity target,DamageSource source) {
        if(target.level().isClientSide||target.isAlive()||!DEATHS.add(target))return;
        if(target instanceof ServerPlayer player&&NikaidouHiroStateManager.isNikaidouHiro(player)
                &&source.is(SkillDamageHelper.NIKAIDOU_DECAY)&&source.getEntity()==player)
            DepartmentTasks.tracker().recordNikaidouCoreDeath(player);
        if(!(source.getEntity() instanceof ServerPlayer killer)||killer==target)return;
        if(NikaidouHiroStateManager.isNikaidouHiro(killer)) {
            if(NikaidouHiroStateManager.MAX_RIFT_STACKS>0
                    &&NikaidouHiroStateManager.riftStacks(killer)>=NikaidouHiroStateManager.MAX_RIFT_STACKS)
                DepartmentTasks.signal(killer,"nikaidou.gap.max");
            if(source.is(SkillDamageHelper.NIKAIDOU_WEAPON)) {
                var tool=NikaidouHiroStateManager.equippedTool(killer);
                if(tool==NikaidouHiroTool.HOT_IRON)DepartmentTasks.signal(killer,"nikaidou.fire_stick.kill");
                else if(tool==NikaidouHiroTool.RITUAL_SWORD)DepartmentTasks.signal(killer,"nikaidou.ritual_sword.kill");
            }
        }
        if(NikaidouHiroWitchificationStateManager.isNikaidouHiroWitchification(killer)
                &&NikaidouHiroWitchificationStateManager.isCoreActive(killer)&&source.is(SkillDamageHelper.NIKAIDOU_WEAPON))
            DepartmentTasks.scoped(killer,"witch.core.cast_kills",1,
                    killer.getPersistentData().getCompound("dealt_force_skills.nikaidou_hiro_witchification").getString(CORE_CAST),
                    target.getUUID().toString(),Long.MAX_VALUE);
    }
}
