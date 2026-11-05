package com.rzy.dealt_force_skills.department;

import com.rzy.dealt_force_skills.character.chamber.ChamberTaczEnhancement;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;

/** Optional TaCZ public API only. Attribution is captured at firing and travels with each bullet. */
public final class DepartmentGunCombat {
    private static final String SHOT="dealt_force_skills.department_shot";
    private DepartmentGunCombat() { }
    private static boolean bullet(Entity entity) {
        if(entity==null)return false;
        for(Class<?> type=entity.getClass();type!=null;type=type.getSuperclass())
            if(type.getName().equals("com.tacz.guns.entity.EntityKineticBullet"))return true;
        return false;
    }
    private static ResourceLocation gunId(Object gun) throws ReflectiveOperationException {
        if(gun instanceof ItemStack stack) {
            Class<?> api=Class.forName("com.tacz.guns.api.item.IGun");
            Object item=api.getMethod("getIGunOrNull",ItemStack.class).invoke(null,stack);
            return item==null?null:(ResourceLocation)api.getMethod("getGunId",ItemStack.class).invoke(item,stack);
        }
        return (ResourceLocation)gun.getClass().getMethod("getGunId").invoke(gun);
    }
    private static String type(ResourceLocation id) throws ReflectiveOperationException {
        if(id==null)return "";
        var index=(java.util.Optional<?>)Class.forName("com.tacz.guns.api.TimelessAPI").getMethod("getCommonGunIndex",ResourceLocation.class).invoke(null,id);
        if(index.isEmpty())return "";
        String type=String.valueOf(index.get().getClass().getMethod("getType").invoke(index.get())).toLowerCase(java.util.Locale.ROOT);
        return switch(type) {
            case "pistol","rifle","smg","shotgun","sniper" -> type;
            case "mg","rpg","heavy" -> "heavy";
            default -> "";
        };
    }
    /** Called for the server's firing event. A cancelled shot cannot spawn a credited bullet. */
    public static void firing(Object event) {
        try {
            if(!(event.getClass().getMethod("getShooter").invoke(event) instanceof ServerPlayer player)
                    ||!(event.getClass().getMethod("getGunItemStack").invoke(event) instanceof ItemStack stack))return;
            var id=gunId(stack);if(id==null)return;
            var data=new CompoundTag();data.putString("Gun",id.toString());data.putString("Cast",java.util.UUID.randomUUID().toString());
            data.putString("Kind",ChamberTaczEnhancement.kind(stack).name());
            data.putLong("At",player.server.overworld().getGameTime());
            data.putString("Dimension",player.level().dimension().location().toString());
            player.getPersistentData().put(SHOT,data);
        }catch(ReflectiveOperationException|LinkageError ignored){ }
    }
    public static void spawned(Entity entity) {
        if(!bullet(entity)||entity.level().isClientSide||!(entity instanceof Projectile projectile)
                ||!(projectile.getOwner() instanceof ServerPlayer owner))return;
        try {
            var id=gunId(entity);if(id==null)return;
            var data=new CompoundTag();data.putString("Gun",id.toString());data.putString("Type",type(id));
            data.putString("Cast",entity.getUUID().toString());data.putUUID("Owner",owner.getUUID());
            data.putBoolean("Starlight",DepartmentConsumables.from(owner,com.rzy.dealt_force_skills.registry.ModEffects.LARGE_FIRE_RATE.get(),"starlight_glitter"));
            var shot=owner.getPersistentData().getCompound(SHOT);
            if(shot.getLong("At")==owner.server.overworld().getGameTime()&&shot.getString("Gun").equals(id.toString())
                    &&shot.getString("Dimension").equals(owner.level().dimension().location().toString())) {
                data.putString("Cast",shot.getString("Cast"));data.putString("Kind",shot.getString("Kind"));
            }
            entity.getPersistentData().put(SHOT,data);
        }catch(ReflectiveOperationException|LinkageError ignored){ }
    }
    private static CompoundTag shot(DamageSource source,ServerPlayer owner) {
        Entity direct=source.getDirectEntity();
        if(!bullet(direct))return new CompoundTag();
        var data=direct.getPersistentData().getCompound(SHOT);
        return data.hasUUID("Owner")&&data.getUUID("Owner").equals(owner.getUUID())?data:new CompoundTag();
    }
    public static void damaged(ServerPlayer owner,DamageSource source,float amount) {
        var data=shot(source,owner);
        if(data.getBoolean("Starlight"))DepartmentTasks.signal(owner,"drink.starlight.gun_damage",amount);
        if(data.getString("Type").equals("heavy"))
            DepartmentTasks.signal(owner,"gun.heavy.damage_ratio",amount/Math.max(1.0F,owner.getMaxHealth()));
    }
    public static void killed(ServerPlayer owner,LivingEntity victim,DamageSource source) {
        var data=shot(source,owner);String type=data.getString("Type");
        if(type.isEmpty())return;
        String species=net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(victim.getType()).toString();
        if(type.equals("pistol"))DepartmentTasks.signal(owner,"gun.kill.pistol");
        if(type.equals("smg"))DepartmentTasks.signal(owner,"gun.kill.smg");
        if(type.equals("rifle"))DepartmentTasks.signal(owner,"gun.kill.rifle.species",1,species);
        if(type.equals("sniper")&&victim.getMaxHealth()>=300)DepartmentTasks.signal(owner,"gun.kill.sniper.health300");
        if(type.equals("shotgun"))DepartmentTasks.scoped(owner,"gun.shotgun.shot_kills",1,data.getString("Cast"),victim.getUUID().toString(),24000);
        String dim=victim.level().dimension().location().toString();
        if(dim.equals("minecraft:overworld")) {
            if(type.equals("pistol")&&victim.getMaxHealth()>=100)DepartmentTasks.signal(owner,"gun.kill.pistol.overworld.100");
            if(type.equals("rifle")&&victim.getMaxHealth()>=500)DepartmentTasks.signal(owner,"gun.kill.rifle.overworld.500");
            if(type.equals("smg")&&victim.getMaxHealth()>=300)DepartmentTasks.signal(owner,"gun.kill.smg.overworld.300");
        }else if(dim.equals("minecraft:the_nether")) {
            if(type.equals("rifle")&&victim.getMaxHealth()>=300)DepartmentTasks.signal(owner,"gun.kill.rifle.the_nether.300");
            if(type.equals("shotgun")&&victim.getMaxHealth()>=50)DepartmentTasks.signal(owner,"gun.kill.shotgun.the_nether.50");
        }else if(dim.equals("minecraft:the_end")) {
            if(type.equals("heavy")&&victim.getMaxHealth()>=300)DepartmentTasks.signal(owner,"gun.kill.heavy.the_end.300");
            if(type.equals("sniper")&&victim.getMaxHealth()>=200)DepartmentTasks.signal(owner,"gun.kill.sniper.the_end.200");
        }
        if(data.getString("Kind").equals("HEADHUNTER"))DepartmentTasks.signal(owner,"chamber.pistol.kill");
        if(data.getString("Kind").equals("TOUR_DE_FORCE"))DepartmentTasks.signal(owner,"chamber.sniper.kill");
    }
}
