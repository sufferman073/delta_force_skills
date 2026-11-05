package com.rzy.dealt_force_skills.department;

import com.rzy.dealt_force_skills.item.DfsEquipmentItem;
import com.rzy.dealt_force_skills.shop.TaczShopCatalog;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Generates paired requirement/reward variants from the collector design sheets. */
public final class CollectorGenerator {
    private CollectorGenerator() { }
    public static int variants(String kind) {
        return switch(kind) { case "basic" -> 10; case "mineral" -> 9; case "equipment" -> 16; case "enchant" -> 3; default -> 1; };
    }
    public static CollectorOffer generate(ServerPlayer player, String kind) {
        return generate(player, kind, player.getRandom().nextInt(variants(kind)) + 1);
    }
    public static CollectorOffer generate(ServerPlayer player, String kind, int variant) {
        if(variant<1||variant>variants(kind))return null;
        Builder b=new Builder(player);
        try {
            switch(kind) {
                case "basic" -> basic(b,variant);
                case "mineral" -> mineral(b,variant);
                case "equipment" -> equipment(b,variant);
                case "enchant" -> enchanting(b,variant);
                case "plant", "entity", "chest" -> natural(b,kind);
                default -> { return null; }
            }
            return new CollectorOffer(variant,b.required,b.rewards,b.coins,b.experience);
        } catch (UnavailablePool error) { return null; }
    }
    private static void basic(Builder b,int v) {
        switch(v) {
            case 1 -> { b.req("logs",8,20);b.out("planks",30,64);b.pay(200000,10); }
            case 2 -> { b.req("planks",32,128);b.out("sticks",64,128);b.out("minecraft:torch",16,32);b.out("fuel",8,12);b.pay(0,10); }
            case 3 -> { b.req("sticks",30,160);b.out("logs",4,8);b.pay(200000,10); }
            case 4 -> { b.req("stone",40,80);b.out("cobblestone",30,64);b.pay(200000,10); }
            case 5 -> { b.req("cobblestone",64,128);b.out("ores",8,16);b.pay(0,10); }
            case 6 -> { b.req("sand_gravel",32,128);b.out("glass",30,64);b.out("minecraft:lava_bucket",1,1);b.pay(0,35); }
            case 7 -> { b.req("logs",4,10);b.req("cobblestone",25,50);b.out("ores",10,20);b.pay(0,20); }
            case 8 -> { b.req("planks",16,64);b.req("stone",12,30);b.out("ores",10,20);b.pay(0,20); }
            case 9 -> {
                ItemStack log=b.pick(woodFamilies());ResourceLocation id=BuiltInRegistries.ITEM.getKey(log.getItem());
                String family=id.getPath().replaceFirst("^(stripped_)","").replaceFirst("_(log|wood|stem|hyphae)$","");
                b.req(log,2,8);b.req(item(id.getNamespace()+":"+family+"_planks"),8,16);
                ItemStack sticks=itemOrEmpty(id.getNamespace()+":"+family+"_stick");
                b.req(sticks.isEmpty()?new ItemStack(Items.STICK):sticks,8,16);b.out("logs",64,64);
            }
            case 10 -> { b.req("stone",15,25);b.req("cobblestone",32,64);b.req("sand_gravel",16,64);b.pay(780000,0); }
        }
    }
    private static void mineral(Builder b,int v) {
        switch(v) {
            case 1 -> { b.req("cobblestone",128,256);b.out(b.random.nextBoolean()?"ores":"raw",8,16);b.pay(100000,40); }
            case 2 -> { b.req("ores",20,45);b.out("fuel",64,128);b.pay(0,300); }
            case 3 -> { b.req("ingots_gems",10,35);b.out("ingots_gems",8,25);b.pay(0,300); }
            case 4 -> { b.req("raw",25,60);b.pay(780000,0); }
            case 5 -> { b.req("nuggets",64,128);b.out("mineral_blocks",2,3);b.pay(0,220); }
            case 6 -> { b.req("mineral_blocks",2,4);b.out("ingots",36,48); }
            case 7 -> { b.req("ores",16,40);b.req("raw",25,60);b.out("ingots_gems",30,80);b.out("mineral_blocks",3,6); }
            case 8 -> {
                ItemStack ingot=b.pick(metalFamilies());String id=BuiltInRegistries.ITEM.getKey(ingot.getItem()).toString();
                String base=id.substring(0,id.length()-"_ingot".length());
                b.req(item(base+"_nugget"),32,64);b.req(ingot,20,45);b.req(item(base+"_block"),1,2);
                b.out(b.random.nextBoolean()?"ores":"raw",60,120);b.pay(0,100);
            }
            case 9 -> { b.req("cobblestone",64,128);b.req("ingots",15,45);b.out("mineral_blocks",8,16);b.out("ores",30,60);b.pay(0,60); }
        }
    }
    private static void equipment(Builder b,int v) {
        if(v<=6) { b.req(b.armor(v,EquipmentSlot.HEAD),1,1);b.req(b.armor(v,EquipmentSlot.CHEST),1,1); }
        if(v>=7&&v<=10) {
            int head=v==7?1:v==8?3:4;int chest=v==7?2:v==8?4:v==9?5:6;
            b.req(b.armor(head,EquipmentSlot.HEAD),1,1);b.req(b.armor(chest,EquipmentSlot.CHEST),1,1);
        }
        switch(v) {
            case 1,2,3,4 -> { b.out(b.armor(v+1,b.random.nextBoolean()?EquipmentSlot.HEAD:EquipmentSlot.CHEST),1,1);b.pay(50000L<<(v-1),0); }
            case 5 -> { b.pay(7800000,0);b.out("dealt_force_skills:premium_coffee_beans",1,2); }
            case 6 -> b.out(b.random.nextBoolean()?"dealt_force_skills:hvk_advanced_standard_template_constructor":"dealt_force_skills:hvk_advanced_disassembly_beam_emitter",1,1);
            case 7 -> b.pay(187500,200);
            case 8 -> b.pay(450000,300);
            case 9 -> b.pay(780000,450);
            case 10 -> { b.pay(9178,1145);b.out("dealt_force_skills:driftwood",1,1); }
            case 11 -> {
                List<ItemStack> ammo=ammoWithGuns();ItemStack selected=b.pick(ammo);b.req(selected,32,64);
                String ammoId=taczAmmoId(selected);b.out(b.pick(guns().stream().filter(g -> gunAmmoId(g).equals(ammoId)).toList()),1,1);
            }
            case 12 -> { ItemStack gun=b.pick(guns());b.req(gun,1,1);b.out(ammoFor(gun),40,80); }
            case 13 -> { b.attachments(true,1,3);b.out(b.pick(ammoWithGuns()),15,25);b.out(b.pick(guns()),1,1); }
            case 14,15,16 -> {
                int head=v==14?1:v==15?3:4, chest=v==14?2:v==15?4:5;
                b.req(b.armor(head,EquipmentSlot.HEAD),1,1);b.req(b.armor(chest,EquipmentSlot.CHEST),1,1);
                List<ItemStack> pool=new ArrayList<>(guns());
                for(int i=0;i<(v==16?2:1);i++) {
                    ItemStack gun=b.take(pool);b.req(gun,1,1);b.req(ammoFor(gun),v==14?20:v==15?60:30,v==14?40:v==15?120:60);
                }
                if(v>=15)b.attachments(true,v==15?1:4,v==15?2:6);
                b.pay(v==14?415000:v==15?900000:1560000,v==14?400:v==15?600:900);
                if(v==16)b.out("dealt_force_skills:fox_music_player",1,1);
            }
        }
    }
    private static void enchanting(Builder b,int v) {
        String kind=v==1?"normal":v==2?"treasure":"curse";
        b.books(true,kind,1,v==1?4:2);
        int reward=b.random.nextInt(v==3?2:3);
        if(v==1)b.books(false,reward==0?"normal":reward==1?"treasure":"curse",1,reward==0?4:1);
        else if(v==2) {
            if(reward==2)b.out("dealt_force_skills:hvk_enchant_infusion",1,1);
            else b.books(false,reward==0?"treasure":"normal",reward==0?1:4,reward==0?2:6);
        } else b.books(false,reward==0?"normal":"curse",reward==0?4:1,reward==0?8:6);
    }
    private static void natural(Builder b,String kind) {
        int reqKinds=b.count(1,kind.equals("plant")?4:3), outKinds=b.count(1,kind.equals("plant")?3:2);
        List<List<ItemStack>> pools=DepartmentNaturalPools.pools(b.player,kind,Math.max(reqKinds,outKinds));
        if(pools.size()<Math.max(reqKinds,outKinds))throw new UnavailablePool();
        for(int direction=0;direction<2;direction++) {
            List<List<ItemStack>> remaining=new ArrayList<>(pools);
            for(int i=0;i<(direction==0?reqKinds:outKinds);i++) {
                List<ItemStack> items=new ArrayList<>(remaining.remove(b.random.nextInt(remaining.size())));
                int min=kind.equals("plant")?1:direction==0?1:2;
                int max=kind.equals("plant")?1:direction==0?2:4;
                if(items.size()<min)throw new UnavailablePool();
                int number=b.count(min,Math.min(max,items.size()));
                for(int j=0;j<number;j++) {
                    ItemStack stack=b.take(items);
                    if(direction==0)b.req(stack,1,kind.equals("plant")?8:32);
                    else b.out(stack,2,kind.equals("plant")?6:kind.equals("chest")?20:12);
                }
            }
        }
    }
    private static List<ItemStack> woodFamilies() {
        return pool("logs").stream().filter(stack -> {
            ResourceLocation id=BuiltInRegistries.ITEM.getKey(stack.getItem());
            String family=id.getPath().replaceFirst("^(stripped_)","").replaceFirst("_(log|wood|stem|hyphae)$","");
            return !itemOrEmpty(id.getNamespace()+":"+family+"_planks").isEmpty();
        }).toList();
    }
    private static List<ItemStack> metalFamilies() {
        return pool("ingots").stream().filter(stack -> {
            String id=BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            if(!id.endsWith("_ingot"))return false;String base=id.substring(0,id.length()-6);
            return !itemOrEmpty(base+"_nugget").isEmpty()&&!itemOrEmpty(base+"_block").isEmpty();
        }).toList();
    }
    private static ItemStack itemOrEmpty(String id) {
        ResourceLocation key=ResourceLocation.tryParse(id);
        return key!=null&&BuiltInRegistries.ITEM.containsKey(key)?new ItemStack(BuiltInRegistries.ITEM.get(key)):ItemStack.EMPTY;
    }
    private static ItemStack item(String id) { ItemStack stack=itemOrEmpty(id);if(stack.isEmpty())throw new UnavailablePool();return stack; }
    private static List<ItemStack> pool(String name) {
        if(name.indexOf(':')>=0)return List.of(item(name));
        List<ItemStack> result=new ArrayList<>();
        for(Item item:BuiltInRegistries.ITEM) {
            ItemStack stack=new ItemStack(item);if(stack.isEmpty())continue;
            var tags=stack.getTags().map(tag->tag.location().getPath()).toList();
            boolean match=switch(name) {
                case "logs" -> tags.contains("logs"); case "planks" -> tags.contains("planks");
                case "sticks" -> item==Items.STICK||tags.contains("rods/wooden");
                case "stone" -> item==Items.STONE||tags.contains("stone")||tags.contains("stones");
                case "cobblestone" -> item==Items.COBBLESTONE||tags.contains("cobblestone")||tags.contains("cobblestones");
                case "sand_gravel" -> item==Items.GRAVEL||tags.contains("sand")||tags.contains("sands");
                case "glass" -> item==Items.GLASS||tags.contains("glass")||tags.contains("glass_blocks");
                case "ores" -> tags.stream().anyMatch(t->t.equals("ores")||t.startsWith("ores/"));
                case "ingots" -> tags.contains("ingots");
                case "ingots_gems" -> tags.contains("ingots")||tags.contains("gems");
                case "raw" -> tags.stream().anyMatch(t->t.equals("raw_materials")||t.startsWith("raw_materials/"));
                case "nuggets" -> tags.contains("nuggets");
                case "mineral_blocks" -> tags.stream().anyMatch(t->t.startsWith("storage_blocks/"))
                        && !tags.contains("storage_blocks/slime")&&!tags.contains("storage_blocks/bone_meal")&&!tags.contains("storage_blocks/wheat");
                case "fuel" -> net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity.isFuel(stack);
                default -> false;
            };
            if(match)result.add(stack);
        }
        return result;
    }
    private static List<ItemStack> tacz(TaczShopCatalog.Type type) {
        return TaczShopCatalog.entries().stream().filter(entry->entry.type()==type).map(entry->entry.preview().copyWithCount(1)).toList();
    }
    private static List<ItemStack> guns() { return tacz(TaczShopCatalog.Type.GUN).stream().filter(g->!ammoFor(g).isEmpty()).toList(); }
    private static List<ItemStack> ammoWithGuns() { var guns=guns();return tacz(TaczShopCatalog.Type.AMMO).stream().filter(a->guns.stream().anyMatch(g->gunAmmoId(g).equals(taczAmmoId(a)))).toList(); }
    private static String taczAmmoId(ItemStack ammo) { return TaczShopCatalog.find(ammo).map(entry->entry.id().toString()).orElse(""); }
    private static String gunAmmoId(ItemStack gun) {
        try {
            var entry=TaczShopCatalog.find(gun);if(entry.isEmpty())return "";
            Object optional=Class.forName("com.tacz.guns.api.TimelessAPI").getMethod("getCommonGunIndex",ResourceLocation.class).invoke(null,entry.get().id());
            if(!(optional instanceof java.util.Optional<?> index)||index.isEmpty())return "";
            Object data=index.get().getClass().getMethod("getGunData").invoke(index.get());
            return data.getClass().getMethod("getAmmoId").invoke(data).toString();
        }catch(ReflectiveOperationException|LinkageError error){return "";}
    }
    private static ItemStack ammoFor(ItemStack gun) {
        String id=gunAmmoId(gun);
        return TaczShopCatalog.entries().stream().filter(entry->entry.type()==TaczShopCatalog.Type.AMMO&&entry.id().toString().equals(id))
                .findFirst().map(entry->entry.preview().copyWithCount(1)).orElse(ItemStack.EMPTY);
    }
    private static final class UnavailablePool extends RuntimeException { }
    private static final class Builder {
        final ServerPlayer player;final RandomSource random;
        final List<CollectorOffer.Part> required=new ArrayList<>(),rewards=new ArrayList<>();long coins;int experience;
        Builder(ServerPlayer player){this.player=player;random=player.getRandom();}
        int count(int min,int max){return min+random.nextInt(max-min+1);}
        ItemStack pick(List<ItemStack> pool){if(pool.isEmpty())throw new UnavailablePool();return pool.get(random.nextInt(pool.size())).copy();}
        ItemStack take(List<ItemStack> pool){if(pool.isEmpty())throw new UnavailablePool();return pool.remove(random.nextInt(pool.size())).copy();}
        void req(String pool,int min,int max){req(pick(pool(pool)),min,max);} void out(String pool,int min,int max){out(pick(pool(pool)),min,max);}
        void req(ItemStack stack,int min,int max){required.add(new CollectorOffer.Part(stack,count(min,max)));}
        void out(ItemStack stack,int min,int max){if(stack.isEmpty())throw new UnavailablePool();rewards.add(new CollectorOffer.Part(stack,count(min,max)));}
        void pay(long coins,int xp){this.coins=coins;experience=xp;}
        ItemStack armor(int quality,EquipmentSlot slot){
            return pick(BuiltInRegistries.ITEM.stream().filter(item->item instanceof DfsEquipmentItem equipment
                    && equipment.profile().slot()==slot && (equipment.profile().quality().tier()==quality
                    ||quality==6&&equipment.profile().quality()==com.rzy.dealt_force_skills.item.DfsItemQuality.RED_7)).map(ItemStack::new).toList());
        }
        void attachments(boolean request,int min,int max){
            List<ItemStack> pool=new ArrayList<>(tacz(TaczShopCatalog.Type.ATTACHMENT));
            if(pool.size()<min)throw new UnavailablePool();int n=count(min,Math.min(max,pool.size()));
            for(int i=0;i<n;i++){ItemStack stack=take(pool);if(request)req(stack,1,1);else out(stack,1,1);}
        }
        void books(boolean request,String kind,int min,int max){
            var pool=new ArrayList<>(DepartmentPlatform.enchantments(player).stream().filter(e->e.kind().equals(kind)).toList());
            if(pool.isEmpty())throw new UnavailablePool();
            Map<String,Map<String,Integer>> candidates=new LinkedHashMap<>();
            for(var e:pool)for(int level=1;level<=e.maximum();level++) {
                Map<String,Integer> single=Map.of(e.id(),level);candidates.put(single.toString(),single);
            }
            for(int attempt=0;!request&&attempt<64;attempt++) {
                Map<String,Integer> enchants=new java.util.TreeMap<>();int types=count(1,Math.min(3,pool.size()));
                for(int j=0;j<types;j++) {
                    List<DepartmentPlatform.EnchantmentSpec> choices=pool.stream().filter(e->!enchants.containsKey(e.id())).toList();
                    var e=choices.get(random.nextInt(choices.size()));enchants.put(e.id(),count(1,e.maximum()));
                }
                candidates.put(enchants.toString(),enchants);
            }
            if(candidates.size()<min)throw new UnavailablePool();
            List<Map<String,Integer>> choices=new ArrayList<>(candidates.values());
            int n=count(min,Math.min(max,choices.size()));
            for(int i=0;i<n;i++) {
                ItemStack stack=DepartmentPlatform.book(player,choices.remove(random.nextInt(choices.size())));
                if(request)required.add(new CollectorOffer.Part(stack,1,true));else out(stack,1,1);
            }
        }
    }
}
