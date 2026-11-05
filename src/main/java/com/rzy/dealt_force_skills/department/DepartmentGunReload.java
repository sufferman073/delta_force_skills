package com.rzy.dealt_force_skills.department;

import com.rzy.dealt_force_skills.registry.ModEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Observe real magazine transfer through public TaCZ reload state; no client completion packet is trusted. */
public final class DepartmentGunReload {
    private static final class Reload {
        final ItemStack item;
        final long started;
        final int ammo;
        Reload(ItemStack item,long started,int ammo){this.item=item;this.started=started;this.ammo=ammo;}
    }
    private static final java.util.Map<ServerPlayer,Reload> PENDING=new java.util.WeakHashMap<>();
    private DepartmentGunReload() { }
    private static int ammo(ItemStack stack) throws ReflectiveOperationException {
        Class<?> api=Class.forName("com.tacz.guns.api.item.IGun");
        Object gun=api.getMethod("getIGunOrNull",ItemStack.class).invoke(null,stack);
        if(gun==null)return -1;
        int magazine=(Integer)api.getMethod("getCurrentAmmoCount",ItemStack.class).invoke(gun,stack);
        return Math.max(0,magazine)+(Boolean.TRUE.equals(api.getMethod("hasBulletInBarrel",ItemStack.class).invoke(gun,stack))?1:0);
    }
    public static void event(Object event) {
        String name=event.getClass().getName();
        if(!name.equals("com.tacz.guns.api.event.common.GunReloadEvent"))return;
        try {
            if(!"SERVER".equals(String.valueOf(event.getClass().getMethod("getLogicalSide").invoke(event)))
                    ||!(event.getClass().getMethod("getGunItemStack").invoke(event) instanceof ItemStack item))return;
            if(!(event.getClass().getMethod("getEntity").invoke(event) instanceof ServerPlayer player))return;
            PENDING.remove(player);
            if(player.getMainHandItem()!=item||!DepartmentConsumables.from(player,ModEffects.LARGE_RELOAD_SPEED.get(),"heartbeat"))return;
            int count=ammo(item);if(count<0)return;
            PENDING.put(player,new Reload(item,player.level().getGameTime(),count));
        }catch(ReflectiveOperationException|LinkageError ignored){ }
    }
    public static void tick(ServerPlayer player) {
        Reload reload=PENDING.get(player);if(reload==null)return;
        long now=player.level().getGameTime();
        if(!player.isAlive()||player.getMainHandItem()!=reload.item||now<reload.started||now-reload.started>1200
                ||!DepartmentConsumables.from(player,ModEffects.LARGE_RELOAD_SPEED.get(),"heartbeat")) {
            PENDING.remove(player);return;
        }
        try {
            Class<?> api=Class.forName("com.tacz.guns.api.entity.IGunOperator");
            Object operator=api.getMethod("fromLivingEntity",net.minecraft.world.entity.LivingEntity.class).invoke(null,player);
            Object state=api.getMethod("getSynReloadState").invoke(operator);
            Object type=state.getClass().getMethod("getStateType").invoke(state);
            boolean finishing=Boolean.TRUE.equals(type.getClass().getMethod("isReloadFinishing").invoke(type));
            boolean reloading=Boolean.TRUE.equals(type.getClass().getMethod("isReloading").invoke(type));
            if(finishing&&ammo(reload.item)>reload.ammo) {
                PENDING.remove(player);DepartmentTasks.signal(player,"drink.heart.reload");
            }else if(!reloading&&now>reload.started)PENDING.remove(player);
        }catch(ReflectiveOperationException|LinkageError ignored){PENDING.remove(player);}
    }
}
