package com.rzy.dealt_force_skills.department;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Captures identity before the last item is consumed, and reports only successful use. */
public final class DepartmentConsumption {
    private DepartmentConsumption() { }
    public record Observation(ItemStack item,float health,java.util.Map<String,Integer> harmful,boolean painRelief,double stamina,double maxStamina) { }
    public static Observation before(ServerPlayer player,ItemStack item) {
        java.util.Map<String,Integer> effects=new java.util.HashMap<>();
        for(var effect:player.getActiveEffects())if(effect.getEffect().value().getCategory()==net.minecraft.world.effect.MobEffectCategory.HARMFUL)
            effects.put(BuiltInRegistries.MOB_EFFECT.getKey(effect.getEffect().value()).toString(),effect.getAmplifier());
        return new Observation(item.copy(),player.getHealth(),effects,
                player.hasEffect(com.rzy.dealt_force_skills.registry.ModEffects.PAIN_RELIEF),com.rzy.dealt_force_skills.stamina.StaminaManager.currentValue(player),com.rzy.dealt_force_skills.stamina.StaminaManager.maximum(player));
    }
    public static void used(ServerPlayer player,ItemStack item) {
        if(item.isEmpty())return;
        DepartmentTasks.tracker().fail(player,"consume");
        DepartmentTasks.tracker().record(player,"consume",BuiltInRegistries.ITEM.getKey(item.getItem()).toString(),1,"");
    }
    public static void healed(ServerPlayer player,ItemStack item,float before) {
        float restored=Math.max(0,player.getHealth()-before);
        DepartmentTasks.signal(player,"consumable.healing",restored);
        if(item.is(com.rzy.dealt_force_skills.registry.ModItems.BATTLEFIELD_MEDICAL_KIT.get()))
            DepartmentTasks.signal(player,"battlefield_medical_box.healing",restored);
    }
    public static void finished(ServerPlayer player,Observation before) {
        ItemStack item=before.item();
        used(player,item);
        if(item.is(com.rzy.dealt_force_skills.registry.ModItems.OE2_COMBAT_STIMULANT.get())&&before.stamina()<before.maxStamina())
            DepartmentTasks.tracker().arm(player,"stamina_injection.moved",true);
        // Channelled medicine is measured once per successful pulse, including interrupted uses.
        if(!(item.getItem() instanceof com.rzy.dealt_force_skills.item.HealingMedicineItem))healed(player,item,before.health());
        java.util.Set<String> remaining=new java.util.HashSet<>();
        for(var effect:player.getActiveEffects())remaining.add(BuiltInRegistries.MOB_EFFECT.getKey(effect.getEffect().value()).toString());
        String id=BuiltInRegistries.ITEM.getKey(item.getItem()).getPath();
        before.harmful().forEach((effect,amplifier)->{
            if(remaining.contains(effect))return;
            DepartmentTasks.signal(player,"consumable.cleansed");
            if(amplifier>=2)DepartmentTasks.signal(player,"consumable.high_cleansed");
            if(id.equals("elastic_bandage")||id.equals("cat_tourniquet"))DepartmentTasks.signal(player,"bandage.cleansed");
        });
        if(id.equals("battlefield_medical_kit")&&!before.painRelief()
                &&player.hasEffect(com.rzy.dealt_force_skills.registry.ModEffects.PAIN_RELIEF))
            DepartmentTasks.signal(player,"battlefield_medical_box.painkiller");
    }
    public static void repaired(ServerPlayer player,ItemStack kit,ItemStack target,int restored,net.minecraft.world.entity.EquipmentSlot slot) {
        if(restored<=0)return;
        DepartmentTasks.tracker().fail(player,"consume");
        String id=BuiltInRegistries.ITEM.getKey(kit.getItem()).getPath();
        String part=slot==net.minecraft.world.entity.EquipmentSlot.HEAD?"head":"chest";
        if(id.startsWith("standard_"))DepartmentTasks.signal(player,"repair.standard."+part,restored);
        if(id.startsWith("precision_")&&com.rzy.dealt_force_skills.item.DfsItemQuality.of(target).tier()>3)
            DepartmentTasks.signal(player,"repair.precise.high_quality_"+part);
    }
}
