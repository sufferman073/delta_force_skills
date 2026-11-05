package com.rzy.dealt_force_skills.department;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import java.util.ArrayList;
import java.util.List;

/** Freeze dynamic requirements at acceptance; progress resets together with the task's Goals. */
public final class DepartmentChecklists {
    private DepartmentChecklists() { }
    public static void initialize(ServerPlayer player,DepartmentCatalog.Definition task,CompoundTag state) {
        CompoundTag lists=new CompoundTag();
        for(var goal:task.leaves())if(goal.kind().equals("checklist")) {
            CompoundTag entries=new CompoundTag();
            for(String key:required(player,goal.key()))entries.putBoolean(key,false);
            lists.put(goal.key(),entries);
        }
        state.getCompound("Goals").put("Checklists",lists);
    }
    private static List<String> required(ServerPlayer player,String key) {
        if(key.equals("interdimensional_foods"))return BuiltInRegistries.ITEM.stream()
                .filter(com.rzy.dealt_force_skills.block.InterdimensionalBlockEntity::isCraftableFood)
                .map(item->BuiltInRegistries.ITEM.getKey(item).toString()).toList();
        if(key.equals("monster_hunter")) {
            var advancement=player.server.getAdvancements().getAdvancement(ResourceLocation.parse("minecraft:adventure/kill_all_mobs"));
            return advancement==null?List.of():List.copyOf(advancement.getCriteria().keySet());
        }
        return List.of();
    }
    public static boolean accepts(CompoundTag state,String kind,String key) {
        String list=kind.equals("consume")?"interdimensional_foods":kind.equals("kill")?"monster_hunter":"";
        return !list.isEmpty()&&state.getCompound("Goals").getCompound("Checklists").getCompound(list).contains(key,Tag.TAG_BYTE);
    }
    public static boolean record(CompoundTag state,String kind,String key) {
        String list=kind.equals("consume")?"interdimensional_foods":kind.equals("kill")?"monster_hunter":"";
        if(list.isEmpty())return false;
        CompoundTag entries=state.getCompound("Goals").getCompound("Checklists").getCompound(list);
        if(!entries.contains(key,Tag.TAG_BYTE)||entries.getBoolean(key))return false;
        entries.putBoolean(key,true);return true;
    }
    public static double progress(CompoundTag state,String key) {
        CompoundTag entries=state.getCompound("Goals").getCompound("Checklists").getCompound(key);
        if(entries.isEmpty())return 0;
        return (double)entries.getAllKeys().stream().filter(entries::getBoolean).count()/entries.size();
    }
}
