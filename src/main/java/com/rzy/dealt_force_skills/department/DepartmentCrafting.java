package com.rzy.dealt_force_skills.department;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class DepartmentCrafting {
    private DepartmentCrafting() { }
    private static final java.util.Set<String> DRINKS=java.util.Set.of("guanshan_nanyue","highball","heartbeat",
            "starlight_glitter","long_goodbye","mountain_rain","sky_island","mist_morning","tulip","wild_rose","asara_style","reis_dance","cranberry_special");
    public static void crafted(Player owner,ItemStack result) {
        if(!(owner instanceof ServerPlayer player)||result.isEmpty())return;
        var key=BuiltInRegistries.ITEM.getKey(result.getItem());
        DepartmentTasks.tracker().record(player,"craft",key.toString(),result.getCount(),"");
        if(key.getNamespace().equals("dealt_force_skills")&&DRINKS.contains(key.getPath()))DepartmentTasks.signal(player,"craft.drink",result.getCount());
    }
}
