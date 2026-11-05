package com.rzy.dealt_force_skills.department;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Immutable task definitions, compiled from the maintained design catalog. */
public final class DepartmentCatalog {
    public record Objective(String kind,String key,double target,long window,boolean unique,boolean maximum,
                            String description,List<Objective> children) {
        public Objective { children=List.copyOf(children); }
        public boolean group(){return kind.equals("any")||kind.equals("all");}
        public TaskProgress.Goal goal(){return new TaskProgress.Goal(kind+":"+key,target,window,unique,maximum);}
    }
    public record ItemReward(String item,int count) { }
    public record Reward(long coins,int experience,List<ItemReward> items,List<String> unlocks) {
        public Reward {items=List.copyOf(items);unlocks=List.copyOf(unlocks);}
    }
    public record Definition(String id,String source,String department,String chapter,String title,String brief,
                             List<String> prerequisites,List<Objective> goals,List<String> constraints,List<String> policies,
                             Reward reward,String rewardDescription,boolean repeatable) {
        public Definition {prerequisites=List.copyOf(prerequisites);goals=List.copyOf(goals);constraints=List.copyOf(constraints);policies=List.copyOf(policies);}
        public List<Objective> leaves(){List<Objective> result=new ArrayList<>();goals.forEach(g->flatten(g,result));return List.copyOf(result);}
    }
    private static final String RESOURCE="/data/dealt_force_skills/department/tasks.json";
    private DepartmentCatalog() { }

    private static void flatten(Objective goal,List<Objective> output){if(goal.group())goal.children.forEach(c->flatten(c,output));else output.add(goal);}
    public static Map<String,Definition> read(JsonArray array) {
        Map<String,Definition> result=new LinkedHashMap<>();
        if(array.size()>512)throw new IllegalArgumentException("Too many department tasks");
        for(JsonElement element:array) {
            JsonObject o=element.getAsJsonObject();JsonObject reward=o.getAsJsonObject("reward");
            List<ItemReward> items=new ArrayList<>();
            for(JsonElement entry:reward.getAsJsonArray("items")) {
                var item=entry.getAsJsonObject();int count=item.get("count").getAsInt();
                if(count<1||count>65536)throw new IllegalArgumentException("Invalid task reward count");
                items.add(new ItemReward(str(item,"item"),count));
            }
            List<Objective> goals=new ArrayList<>();for(JsonElement goal:o.getAsJsonArray("goals"))goals.add(objective(goal.getAsJsonObject()));
            Definition task=new Definition(str(o,"id"),str(o,"source"),str(o,"department"),str(o,"chapter"),str(o,"title"),str(o,"brief"),
                    strings(o,"prerequisites"),goals,strings(o,"constraints"),strings(o,"policies"),new Reward(reward.get("coins").getAsLong(),reward.get("experience").getAsInt(),items,strings(reward,"unlocks")),
                    str(o,"rewardDescription"),o.get("repeatable").getAsBoolean());
            if(!task.id.matches("[a-z0-9_]{1,64}")||task.goals.isEmpty()||task.leaves().size()>128
                    ||task.reward.coins<0||task.reward.experience<0
                    ||result.putIfAbsent(task.id,task)!=null)throw new IllegalArgumentException("Invalid/duplicate task: "+task.id);
            for(String policy:task.policies)if(!java.util.Set.of("contact_effects","death_cooldown","death_reset","no_heal_or_consume","idle_pain","set:gti","set:hvk","set:asara").contains(policy))
                throw new IllegalArgumentException("Unknown task policy: "+policy);
        }
        for(Definition task:result.values())for(String id:task.prerequisites)if(!result.containsKey(id))throw new IllegalArgumentException("Unknown prerequisite: "+id);
        for(String id:result.keySet())checkCycles(id,result,new java.util.HashSet<>(),new java.util.HashSet<>());
        return java.util.Collections.unmodifiableMap(result);
    }

    private static void checkCycles(String id,Map<String,Definition> tasks,java.util.Set<String> visiting,java.util.Set<String> visited){
        if(visited.contains(id))return;
        if(!visiting.add(id))throw new IllegalArgumentException("Cyclic prerequisite: "+id);
        for(String parent:tasks.get(id).prerequisites)checkCycles(parent,tasks,visiting,visited);
        visiting.remove(id);visited.add(id);
    }

    public static Map<String,Definition> load() {
        try(var stream=DepartmentCatalog.class.getResourceAsStream(RESOURCE)) {
            if(stream==null)throw new IllegalStateException("Department catalog missing");
            return read(new com.google.gson.JsonParser().parse(new InputStreamReader(stream,StandardCharsets.UTF_8)).getAsJsonArray());
        } catch(java.io.IOException e){throw new IllegalStateException("Cannot read department catalog",e);}
    }

    private static Objective objective(JsonObject o) {
        String kind=str(o,"kind");List<Objective> children=new ArrayList<>();
        if(kind.equals("any")||kind.equals("all"))for(JsonElement c:o.getAsJsonArray("children"))children.add(objective(c.getAsJsonObject()));
        if(!java.util.Set.of("any","all","signal","craft","consume","inventory","advancement","facility","equipment","character","collector","checklist").contains(kind))
            throw new IllegalArgumentException("Unknown goal kind: "+kind);
        var result=new Objective(kind,str(o,"key"),o.has("target")?o.get("target").getAsDouble():1,
                o.has("window")?o.get("window").getAsLong():0,o.has("unique")&&o.get("unique").getAsBoolean(),o.has("maximum")&&o.get("maximum").getAsBoolean(),str(o,"description"),children);
        if(result.group()){if(children.isEmpty())throw new IllegalArgumentException("Empty goal group");}else result.goal();
        return result;
    }

    private static String str(JsonObject object,String key){return object.has(key)?object.get(key).getAsString():"";}
    private static List<String> strings(JsonObject o,String key){List<String> values=new ArrayList<>();if(o.has(key))for(JsonElement e:o.getAsJsonArray(key))values.add(e.getAsString());return List.copyOf(values);}
}
