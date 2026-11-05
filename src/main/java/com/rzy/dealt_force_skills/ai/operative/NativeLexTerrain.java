package com.rzy.dealt_force_skills.ai.operative;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import java.util.ArrayList;
import java.util.UUID;

/** Temporary walls survive server saves and never load chunks or replace occupied blocks. */
@EventBusSubscriber(modid="dealt_force_skills")
public final class NativeLexTerrain {
    private static final String KEY="dealt_force_skills_native_lex_walls";
    private record Cell(BlockPos pos,UUID caster,long expires) { }
    private static Data data(ServerLevel level){return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(Data::new,(tag,provider)->Data.load(tag),null),KEY);}
    static void create(OperativeHost h){
        var level=h.serverLevel();var data=data(level);var forward=h.body().getDirection();var side=forward.getClockWise();
        var center=h.body().blockPosition().relative(forward,4);
        for(int x=-2;x<=2;x++)for(int y=0;y<5;y++){
            var pos=center.relative(side,x).above(y);
            if(data.cells.size()>=10000 || !level.hasChunkAt(pos) || !h.withinOrders(net.minecraft.world.phys.Vec3.atCenterOf(pos)) || !level.getBlockState(pos).isAir()
                    || !level.getEntities(null,new net.minecraft.world.phys.AABB(pos)).isEmpty())continue;
            if(level.setBlock(pos,Blocks.SANDSTONE.defaultBlockState(),3)){data.cells.add(new Cell(pos,h.body().getUUID(),level.getGameTime()+600));data.setDirty();}
        }
    }
    static void expire(OperativeHost h){var d=data(h.serverLevel());for(int i=0;i<d.cells.size();i++){var c=d.cells.get(i);if(c.caster.equals(h.body().getUUID())){d.cells.set(i,new Cell(c.pos,c.caster,0));d.setDirty();}}tick(h.serverLevel());}
    private static void tick(ServerLevel level){var d=data(level);boolean changed=false;
        for(var it=d.cells.iterator();it.hasNext();){var c=it.next();if(c.expires>level.getGameTime() || !level.hasChunkAt(c.pos))continue;
            if(level.getBlockState(c.pos).is(Blocks.SANDSTONE))level.removeBlock(c.pos,false);it.remove();changed=true;}
        if(changed)d.setDirty();
    }
    @SubscribeEvent public static void levelTick(LevelTickEvent.Post e){if(e.getLevel() instanceof ServerLevel level && level.getGameTime()%20==0)tick(level);}
    public static final class Data extends SavedData {
        private final ArrayList<Cell> cells=new ArrayList<>();
        static Data load(CompoundTag tag){var d=new Data();var list=tag.getList("Cells",10);for(int i=0;i<list.size() && i<10000;i++){var t=list.getCompound(i);if(t.hasUUID("Caster"))d.cells.add(new Cell(BlockPos.of(t.getLong("Pos")),t.getUUID("Caster"),t.getLong("Expires")));}return d;}
        @Override public CompoundTag save(CompoundTag tag,net.minecraft.core.HolderLookup.Provider provider){var list=new ListTag();for(var c:cells){var t=new CompoundTag();t.putLong("Pos",c.pos.asLong());t.putUUID("Caster",c.caster);t.putLong("Expires",c.expires);list.add(t);}tag.put("Cells",list);return tag;}
    }
}
