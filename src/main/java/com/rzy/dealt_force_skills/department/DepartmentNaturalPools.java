package com.rzy.dealt_force_skills.department;

import com.rzy.dealt_force_skills.block.HvkAdvancedTreasureCompassBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import java.util.ArrayList;
import java.util.List;

/** Bounded sampling of actual loot tables, never spawning or killing the sampled entity. */
public final class DepartmentNaturalPools {
    private DepartmentNaturalPools() { }
    private static final ThreadLocal<java.util.Map<String,List<List<ItemStack>>>> BATCH=new ThreadLocal<>();
    public static <T> T inBatch(java.util.function.Supplier<T> action) {
        var previous=BATCH.get();BATCH.set(new java.util.HashMap<>());
        try{return action.get();}finally{if(previous==null)BATCH.remove();else BATCH.set(previous);}
    }
    public static List<List<ItemStack>> pools(ServerPlayer player,String kind,int needed) {
        var cache=BATCH.get();
        return cache==null?sample(player,kind,needed):cache.computeIfAbsent(kind,ignored->sample(player,kind,4));
    }
    private static List<List<ItemStack>> sample(ServerPlayer player,String kind,int needed) {
        if(kind.equals("plant"))return plants();
        List<List<ItemStack>> result=new ArrayList<>();
        var random=player.getRandom();
        if(kind.equals("chest")) {
            List<ResourceLocation> ids=new ArrayList<>(player.server.reloadableRegistries().getKeys(Registries.LOOT_TABLE).stream()
                    .filter(id->id.getPath().startsWith("chests/")).toList());
            for(int attempt=0;attempt<24&&!ids.isEmpty()&&result.size()<needed+3;attempt++) {
                var id=ids.remove(random.nextInt(ids.size()));
                List<ItemStack> sample=HvkAdvancedTreasureCompassBlockEntity.chestLoot(player,player.serverLevel(),player.position(),id,24);
                var unique=unique(sample);if(unique.size()>=2)result.add(unique);
            }
        } else if(kind.equals("entity")) {
            List<EntityType<?>> types=new ArrayList<>(BuiltInRegistries.ENTITY_TYPE.stream()
                    .filter(type->type!=EntityType.PLAYER&&type.canSummon()).toList());
            List<EntityType<?>> fallback=List.of(EntityType.SKELETON,EntityType.COW,EntityType.CHICKEN,
                    EntityType.RABBIT,EntityType.SPIDER,EntityType.ZOMBIE);
            for(int attempt=0;attempt<40+fallback.size()&&!types.isEmpty()&&result.size()<needed+3;attempt++) {
                EntityType<?> type;
                if(attempt<40)type=types.remove(random.nextInt(types.size()));
                else {
                    if(result.size()>=needed)break;
                    // Sparse modded registries must not randomly remove this entire offer category.
                    // Sample actual loaded loot tables; never invent rewards or sample a species twice.
                    type=fallback.get(attempt-40);if(!types.remove(type))continue;
                }
                try {
                    var entity=type.create(player.serverLevel());
                    if(!(entity instanceof LivingEntity living))continue;
                    living.setPos(player.position());
                    var table=player.server.reloadableRegistries().getLootTable(living.getLootTable());
                    var params=new LootParams.Builder(player.serverLevel())
                            .withParameter(LootContextParams.THIS_ENTITY,living)
                            .withParameter(LootContextParams.ORIGIN,player.position())
                            .withParameter(LootContextParams.DAMAGE_SOURCE,player.damageSources().playerAttack(player))
                            .withOptionalParameter(LootContextParams.ATTACKING_ENTITY,player)
                            .withOptionalParameter(LootContextParams.DIRECT_ATTACKING_ENTITY,player)
                            .withOptionalParameter(LootContextParams.LAST_DAMAGE_PLAYER,player)
                            .withLuck(player.getLuck()).create(LootContextParamSets.ENTITY);
                    List<ItemStack> sample=new ArrayList<>();
                    for(int roll=0;roll<32&&sample.size()<16;roll++) {
                        for(var stack:table.getRandomItems(params)) {
                            if(!stack.isEmpty()&&sample.stream().noneMatch(old->ItemStack.isSameItemSameComponents(old,stack)))
                                sample.add(stack.copyWithCount(1));
                            if(sample.size()>=32)break;
                        }
                    }
                    var unique=unique(sample);if(unique.size()>=2)result.add(unique);
                } catch(RuntimeException ignored) { /* Incompatible optional loot context is not offered. */ }
            }
        }
        return result;
    }
    private static List<ItemStack> unique(List<ItemStack> stacks) {
        List<ItemStack> result=new ArrayList<>();
        for(var stack:stacks)if(!stack.isEmpty()&&result.stream().noneMatch(old->ItemStack.isSameItemSameComponents(old,stack)))
            result.add(stack.copyWithCount(1));
        return result;
    }
    private static List<List<ItemStack>> plants() {
        List<List<ItemStack>> pools=new ArrayList<>();
        for(String[] family:new String[][]{{"wheat","wheat_seeds"},{"beetroot","beetroot_seeds"},{"pumpkin","pumpkin_seeds"},
                {"melon_slice","melon_seeds"},{"carrot"},{"potato"},{"sweet_berries"},{"glow_berries"},{"cocoa_beans"},
                {"nether_wart"},{"sugar_cane"},{"bamboo"},{"cactus"},{"kelp"}}) {
            List<ItemStack> items=new ArrayList<>();
            for(String id:family)items.add(new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("minecraft:"+id))));
            pools.add(items);
        }
        // Include modded crop seeds/produce grouped by their planting block when the public API exposes it.
        for(var item:BuiltInRegistries.ITEM)if(item instanceof net.minecraft.world.item.BlockItem blockItem
                && blockItem.getBlock() instanceof net.minecraft.world.level.block.CropBlock
                && !BuiltInRegistries.ITEM.getKey(item).getNamespace().equals("minecraft"))pools.add(List.of(new ItemStack(item)));
        return pools;
    }
}
