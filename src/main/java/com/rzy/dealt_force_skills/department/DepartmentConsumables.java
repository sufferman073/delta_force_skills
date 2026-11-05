package com.rzy.dealt_force_skills.department;

import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.stamina.StaminaManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** Measurements of the existing medicine mechanics, with exact active-effect provenance. */
public final class DepartmentConsumables {
    public record Source(java.util.UUID consumer,String item,double staminaBaseline) { }
    private static final ThreadLocal<Source> USE=new ThreadLocal<>();
    private static final String SAVE="dealt_force_skills:consumable_sources";
    private DepartmentConsumables() { }
    public static boolean observeUse(ServerPlayer player,ItemStack item,java.util.function.BooleanSupplier action) {
        Source previous=USE.get();
        Source used=new Source(player.getUUID(),BuiltInRegistries.ITEM.getKey(item.getItem()).toString(),StaminaManager.maximum(player));USE.set(used);
        try {
            boolean applied=action.getAsBoolean();
            if(applied&&used.item().equals("dealt_force_skills:cranberry_special")
                    &&player.getActiveEffects().stream().anyMatch(effect->((DepartmentEffectSource)effect).dfs$consumableSource()==used
                    &&effect.getAmplifier()>0&&effect.getEffect().isBeneficial()))
                DepartmentTasks.signal(player,"drink.cranberry.enhanced");
            return applied;
        } finally {if(previous==null)USE.remove();else USE.set(previous);}
    }
    public static void addEffect(ServerPlayer player,MobEffectInstance effect) {
        Source source=USE.get();
        if(source!=null&&source.consumer().equals(player.getUUID()))((DepartmentEffectSource)effect).dfs$consumableSource(source);
        long harmful=player.getActiveEffects().stream().filter(e->e.getEffect().getCategory()==net.minecraft.world.effect.MobEffectCategory.HARMFUL).count();
        player.addEffect(effect,player);
        var active=player.getEffect(effect.getEffect());
        if(source!=null&&source.item().equals("dealt_force_skills:mist_morning")
                &&effect.getEffect()==ModEffects.SEDATION.get()&&active!=null&&((DepartmentEffectSource)active).dfs$consumableSource()==source)
            DepartmentTasks.signal(player,"drink.mist.suppressed",harmful);
    }
    private static MobEffectInstance current(ServerPlayer player,MobEffect effect) { return player.getEffect(effect); }
    public static Source source(ServerPlayer player,MobEffect effect) {
        var active=current(player,effect);if(active==null)return null;
        var source=((DepartmentEffectSource)active).dfs$consumableSource();
        return source!=null&&source.consumer().equals(player.getUUID())?source:null;
    }
    public static boolean from(ServerPlayer player,MobEffect effect,String item) {
        var source=source(player,effect);return source!=null&&source.item().equals("dealt_force_skills:"+item);
    }
    private static MobEffectInstance hidden(MobEffectInstance effect) {
        return ((com.rzy.dealt_force_skills.mixin.OperativeEffectAccessor)effect).dfs$hiddenEffect();
    }
    public static void save(MobEffectInstance effect,CompoundTag tag) {
        var entries=new net.minecraft.nbt.ListTag();boolean any=false;
        for(int i=0;effect!=null&&i<64;i++,effect=hidden(effect)) {
            var row=new CompoundTag();var source=((DepartmentEffectSource)effect).dfs$consumableSource();
            if(source!=null){row.putUUID("Consumer",source.consumer());row.putString("Item",source.item());row.putDouble("Stamina",source.staminaBaseline());any=true;}
            entries.add(row);
        }
        if(any)tag.put(SAVE,entries);else tag.remove(SAVE);
    }
    public static void load(MobEffectInstance effect,CompoundTag tag) {
        var entries=tag.getList(SAVE,net.minecraft.nbt.Tag.TAG_COMPOUND);
        for(int i=0;effect!=null&&i<Math.min(64,entries.size());i++,effect=hidden(effect)) {
            var row=entries.getCompound(i);String item=row.getString("Item");double baseline=row.getDouble("Stamina");
            if(row.hasUUID("Consumer")&&item.length()<=160&&item.startsWith("dealt_force_skills:")&&Double.isFinite(baseline)&&baseline>0)
                ((DepartmentEffectSource)effect).dfs$consumableSource(new Source(row.getUUID("Consumer"),item,baseline));
        }
    }
    public static void tick(ServerPlayer player) {
        DepartmentGunReload.tick(player);
        String tickKey="dealt_force_skills.department_consumables_tick";long now=player.level().getGameTime();
        var data=player.getPersistentData();if(data.contains(tickKey)&&data.getLong(tickKey)==now)return;data.putLong(tickKey,now);
        tulipArmor(player);
        long harmful=player.getActiveEffects().stream().filter(e->e.getEffect().getCategory()==net.minecraft.world.effect.MobEffectCategory.HARMFUL).count();
        if(harmful>0&&source(player,ModEffects.PAIN_RELIEF.get())!=null) {
            DepartmentTasks.signal(player,"painkiller.suppressed");DepartmentTasks.signal(player,"painkiller.suppressed_ticks");
        }
        if(from(player,ModEffects.SEDATION.get(),"mist_morning"))DepartmentTasks.signal(player,"drink.mist.suppressed",harmful);
        if(from(player,ModEffects.MEDIUM_RELOAD_SPEED.get(),"sky_island")||from(player,ModEffects.SMALL_AIM_SPEED.get(),"sky_island")
                ||from(player,ModEffects.SMALL_ITEM_USE_SPEED.get(),"sky_island"))
            DepartmentTasks.signal(player,"drink.sky.positive_effects",player.getActiveEffects().stream().filter(e->e.getEffect().isBeneficial()).count());
    }
    public static void healed(LivingEntity entity,float restored) {
        if(!(entity instanceof ServerPlayer player)||restored<=0)return;
        if(from(player,ModEffects.CONTINUOUS_HEALING.get(),"asara_style"))DepartmentTasks.signal(player,"drink.asara.healing",restored);
        if(from(player,ModEffects.CONTINUOUS_HEALING.get(),"reis_dance"))DepartmentTasks.signal(player,"drink.reis.healing",restored);
    }
    private static void tulipArmor(ServerPlayer player) {
        if(!from(player,ModEffects.CARRY_BOOST.get(),"tulip"))return;
        var head=com.rzy.dealt_force_skills.item.DfsEquipmentItem.profile(player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD));
        var chest=com.rzy.dealt_force_skills.item.DfsEquipmentItem.profile(player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST));
        if(head==null||chest==null)return;
        boolean hvk=com.rzy.dealt_force_skills.registry.ModGameRules.areArmorSpecialsEnabled(player)
                &&head.faction()==com.rzy.dealt_force_skills.item.DfsEquipmentItem.Faction.HVK
                &&chest.faction()==com.rzy.dealt_force_skills.item.DfsEquipmentItem.Faction.HVK;
        double movement=(1+head.lightweight()-head.movementLimit())*(1+chest.lightweight()-chest.movementLimit());
        double movementFree=(1+head.lightweight())*(1+chest.lightweight());
        double action=Math.max(0.1,head.actionSpeedMultiplier(hvk)*chest.actionSpeedMultiplier(hvk));
        double actionFree=(1+head.lightweight()*(hvk?1.5:1))*(1+chest.lightweight()*(hvk?1.5:1));
        var effect=current(player,ModEffects.CARRY_BOOST.get());
        double boost=0.25*Math.max(1,Math.min(100,effect.getAmplifier()+1.0));
        double ratio=1;boolean hasPenalty=false;
        if(movementFree>movement){ratio=Math.min(ratio,movement*boost/(movementFree-movement));hasPenalty=true;}
        if(actionFree>action){ratio=Math.min(ratio,action*boost/(actionFree-action));hasPenalty=true;}
        if(hasPenalty)DepartmentTasks.signal(player,"drink.tulip.armor_penalty_reduction",Math.max(0,Math.min(1,ratio)));
    }
    public static void capacityRecovered(net.minecraft.world.entity.player.Player player,double restored) {
        if(player instanceof ServerPlayer server&&from(server,ModEffects.STAMINA_CAPACITY.get(),"norepinephrine"))
            DepartmentTasks.signal(server,"norepinephrine.stamina",restored);
    }
    public static void recovered(ServerPlayer player,double restored) {
        if(from(player,ModEffects.STAMINA_BOOST.get(),"mountain_rain"))DepartmentTasks.signal(player,"drink.rain.stamina",restored);
    }
    public static void spent(net.minecraft.world.entity.player.Player owner,double amount) {
        if(!(owner instanceof ServerPlayer player)||player.getAbilities().instabuild||amount<=0||!Double.isFinite(amount))return;
        var source=source(player,ModEffects.BURNING_HEART.get());
        if(source!=null&&source.item().equals("dealt_force_skills:long_goodbye"))
            DepartmentTasks.signal(player,"drink.farewell.stamina_ratio",amount/source.staminaBaseline());
    }
    public static void moved(ServerPlayer player,double distance) {
        if(player.isSprinting()&&from(player,ModEffects.FATIGUE_REMOVAL.get(),"highball"))DepartmentTasks.signal(player,"drink.highball.sprint",distance);
        if(player.isSprinting()||player.isSwimming())DepartmentTasks.tracker().recordArmed(player,"stamina_injection.moved");
    }
}
