package com.rzy.dealt_force_skills.department;

import com.rzy.dealt_force_skills.mail.MailboxManager;
import com.rzy.dealt_force_skills.item.DfsEquipmentItem;
import com.rzy.dealt_force_skills.shop.SpecialOperationsBaseManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Definitions are immutable; all mutable state is owned by the requesting player's save. */
public final class DepartmentTracker {
    public static final String KEY = "dealt_force_skills.departments";
    public static final int PAGE_SIZE = 64;
    private final Map<String, DepartmentCatalog.Definition> catalog;
    private final Map<String, List<DepartmentCatalog.Objective>> leaves;

    public record Row(String id, String chapter, String title, String status) { }
    public record GoalView(Component text, double current, double target, List<TaskDisplay.Item> items) {
        public GoalView(Component text,double current,double target){this(text,current,target,List.of());}
        public GoalView(String text, double current, double target) { this(Component.literal(text), current, target); }
    }
    public record Detail(String id, String title, String brief, Component reward, List<String> constraints,
                         String status, long retryTicks, boolean repeatable, List<GoalView> goals,
                         List<Row> prerequisites, boolean offerLocked, long refreshTicks, TaskDisplay.Rewards preview) {
        public Detail(String id,String title,String brief,Component reward,List<String> constraints,String status,long retryTicks,boolean repeatable,List<GoalView> goals,List<Row> prerequisites,boolean offerLocked,long refreshTicks) {
            this(id,title,brief,reward,constraints,status,retryTicks,repeatable,goals,prerequisites,offerLocked,refreshTicks,TaskDisplay.Rewards.EMPTY);
        }
    }
    public record Page(String department, int page, int pages, List<Row> rows, Detail detail) { }

    public DepartmentTracker(Map<String, DepartmentCatalog.Definition> definitions) {
        this.catalog = java.util.Collections.unmodifiableMap(new java.util.LinkedHashMap<>(definitions));
        Map<String, List<DepartmentCatalog.Objective>> flat = new java.util.HashMap<>();
        definitions.forEach((id, task) -> flat.put(id, task.leaves()));
        this.leaves = Map.copyOf(flat);
    }

    public Map<String, DepartmentCatalog.Definition> definitions() { return catalog; }
    private static long now(ServerPlayer player) { return player.server.overworld().getGameTime(); }
    private static CompoundTag root(Player player) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(KEY, Tag.TAG_COMPOUND)) data.put(KEY, new CompoundTag());
        return data.getCompound(KEY);
    }
    private static CompoundTag state(Player player, String id) { return root(player).getCompound(id); }

    public static void copy(Player old, Player replacement) {
        if(old.getPersistentData().contains(CollectorBoard.KEY,Tag.TAG_COMPOUND))
            replacement.getPersistentData().put(CollectorBoard.KEY,old.getPersistentData().getCompound(CollectorBoard.KEY).copy());
        if (old.getPersistentData().contains(KEY, Tag.TAG_COMPOUND))
            replacement.getPersistentData().put(KEY, old.getPersistentData().getCompound(KEY).copy());
    }

    public boolean visible(ServerPlayer player, String id) {
        var task = catalog.get(id);
        return task != null && !state(player, id).getBoolean("Deleted");
    }

    public boolean eligible(ServerPlayer player, String id) {
        var task = catalog.get(id);
        return visible(player, id) && task.prerequisites().stream().allMatch(parent ->
                state(player, parent).getBoolean("Completed") || state(player, parent).getBoolean("Deleted"));
    }

    private String status(ServerPlayer player, String id, long tick) {
        if (!eligible(player, id)) return "LOCKED";
        return TaskProgress.status(state(player, id), tick).name();
    }

    public List<Row> prerequisites(ServerPlayer player, String id) {
        var task=catalog.get(id);
        if(task==null)return List.of();
        return task.prerequisites().stream()
                .filter(parent -> !state(player,parent).getBoolean("Completed") && !state(player,parent).getBoolean("Deleted"))
                .map(catalog::get).map(parent -> new Row(parent.id(),parent.department(),parent.title(),status(player,parent.id(),now(player))))
                .toList();
    }

    public boolean administer(ServerPlayer player, String id, DepartmentTaskAdminManager.Action action) {
        var task = catalog.get(id);
        if (task == null) return false;
        long tick = now(player);
        CompoundTag playerTasks = root(player);
        CompoundTag data = state(player, id);

        switch (action) {
            case FINISH -> {
                if (task.repeatable()) data.putBoolean("AdminFinish", true);
                data.putBoolean("Active", false);
                data.putBoolean("Completed", true);
                data.putBoolean("Deleted", false);
                data.putLong("CompletedAt", tick);
                data.putLong("RetryAt", 0);
                playerTasks.put(id, data);
                deliverReward(player, task, data);
            }
            case RESET -> {
                long run = Math.max(0, data.getLong("Run"));
                long revision = data.getLong("ServerRevision");
                if (task.repeatable() && data.getBoolean("Completed")) {
                    if (run == Long.MAX_VALUE) return false;
                    run++;
                }
                CompoundTag reset = new CompoundTag();
                if (run > 0) reset.putLong("Run", run);
                if (revision > 0) reset.putLong("ServerRevision", revision);
                playerTasks.put(id, reset);
            }
            case DELETE -> {
                data.putBoolean("Active", false);
                data.putBoolean("Deleted", true);
                data.putLong("RetryAt", 0);
                playerTasks.put(id, data);
            }
        }
        DepartmentTaskConditions.refresh(player, this);
        return true;
    }

    public boolean accept(ServerPlayer player, String id) {
        var task=catalog.get(id);
        if(task!=null && task.repeatable())return false;
        return start(player,id,true);
    }

    private boolean start(ServerPlayer player, String id, boolean resume) {
        if (!player.isAlive() || player.isSpectator() || !eligible(player, id)) return false;
        var task = catalog.get(id);
        if (!equipmentAllowed(player, task)) return false;
        CompoundTag data = state(player, id);
        if (task.repeatable() && data.getBoolean("Completed") && data.getBoolean("RewardSent")) {
            long run = data.getLong("Run");
            if(run==Long.MAX_VALUE)return false;
            long revision = data.getLong("ServerRevision");
            data = new CompoundTag();data.putLong("Run",run+1);
            if (revision > 0) data.putLong("ServerRevision", revision);
        }
        if(data.getBoolean("Abandoned")) {
            if(!resume)return false;
            data.putBoolean("Abandoned",false);
        }
        if(TaskProgress.status(data,now(player))!=TaskProgress.Status.AVAILABLE)return false;
        if(task.repeatable()) {
            CollectorOffer offer=CollectorGenerator.generate(player,task.goals().get(0).key());
            if(offer==null)return false;
            data.put("Offer",offer.save(player));
        }
        if (!TaskProgress.accept(data, now(player))) return false;
        DepartmentChecklists.initialize(player,task,data);
        root(player).put(id, data);
        DepartmentTaskConditions.refresh(player, this);
        finishIfSatisfied(player, task, data, now(player));
        return true;
    }

    public boolean abandon(ServerPlayer player, String id) {
        if(!player.isAlive() || !visible(player,id))return false;
        var data=state(player,id);
        if(TaskProgress.status(data,now(player))!=TaskProgress.Status.ACTIVE)return false;
        TaskProgress.reset(data,now(player),0,false);
        data.putBoolean("Abandoned",true);
        DepartmentTaskConditions.refresh(player,this);
        return true;
    }

    /** No packet exposes this API. Call only at the authoritative outcome of an action. */
    /** Arm only currently accepted objectives; reset clears these markers with all other goal state. */
    public void arm(ServerPlayer player,String key,boolean armed) {
        long tick=now(player);
        for(var task:catalog.values()) {
            var data=state(player,task.id());
            if(TaskProgress.status(data,tick)!=TaskProgress.Status.ACTIVE||!equipmentAllowed(player,task)
                    ||leaves.get(task.id()).stream().noneMatch(g->g.goal().key().equals("signal:"+key)))continue;
            var goals=data.getCompound("Goals");
            if(!goals.contains("Sequences"))goals.put("Sequences",new CompoundTag());
            goals.getCompound("Sequences").putBoolean(key,armed);
        }
    }
    public void recordArmed(ServerPlayer player,String key) {
        recordWhen(player,"signal",key,1,"",(task,data)->{
            var sequences=data.getCompound("Goals").getCompound("Sequences");
            boolean armed=sequences.getBoolean(key);sequences.remove(key);return armed;
        });
    }
    public void recordAfter(ServerPlayer player,String prerequisite,String key) {
        recordWhen(player,"signal",key,1,"",(task,data)->{
            var flat=leaves.get(task.id());
            for(int i=0;i<flat.size();i++)if(flat.get(i).goal().key().equals("signal:"+prerequisite)
                    &&current(player,flat.get(i),data,i,now(player))>=flat.get(i).target())return true;
            return false;
        });
    }

    public void record(ServerPlayer player, String kind, String key, double amount, String token) {
        recordWhen(player,kind,key,amount,token,(task,data)->true);
    }
    /** Keep the last successful skill with the accepted task, so retries cannot inherit it. */
    public void recordAlternating(ServerPlayer player,String key,String skill) {
        if(skill==null||skill.isEmpty()||skill.length()>160)return;
        recordWhen(player,"signal",key,1,"",(task,data)->{
            if(leaves.get(task.id()).stream().noneMatch(g->g.goal().key().equals("signal:"+key)))return false;
            var goals=data.getCompound("Goals");
            if(!goals.contains("LastSkills"))goals.put("LastSkills",new CompoundTag());
            var last=goals.getCompound("LastSkills");
            if(skill.equals(last.getString(key)))return false;
            last.putString(key,skill);return true;
        });
    }
    private void recordWhen(ServerPlayer player,String kind,String key,double amount,String token,
                            java.util.function.BiPredicate<DepartmentCatalog.Definition,CompoundTag> permitted) {
        recordWhen(player,kind,key,amount,token,permitted,false);
    }
    /** The self-death objective is the sole outcome allowed after confirmed death. */
    void recordNikaidouCoreDeath(ServerPlayer player) {
        recordWhen(player,"signal","nikaidou.core.death",1,"",(task,data)->true,true);
    }
    private void recordWhen(ServerPlayer player,String kind,String key,double amount,String token,
                            java.util.function.BiPredicate<DepartmentCatalog.Definition,CompoundTag> permitted,boolean confirmedDeath) {
        if (!Double.isFinite(amount) || amount <= 0 || (!confirmedDeath&&!player.isAlive()) || player.isSpectator()) return;
        long tick = now(player);
        String event = kind + ":" + key;
        for (var task : catalog.values()) {
            CompoundTag data = state(player, task.id());
            if (TaskProgress.status(data, tick) != TaskProgress.Status.ACTIVE) continue;
            List<DepartmentCatalog.Objective> flat = leaves.get(task.id());
            // An unrelated outcome must not invalidate another outcome still being settled.
            if(flat.stream().noneMatch(g->g.goal().key().equals(event))&&!DepartmentChecklists.accepts(data,kind,key))continue;
            if (!equipmentAllowed(player, task)) {
                TaskProgress.reset(data, tick, 0, true);
                DepartmentChecklists.initialize(player,task,data);
                continue;
            }
            if(!permitted.test(task,data))continue;
            boolean changed = DepartmentChecklists.record(data,kind,key);
            for (int i = 0; i < flat.size(); i++) {
                var goal = flat.get(i).goal();
                if (goal.key().equals(event)) changed |= TaskProgress.record(data, i, goal, event, amount, token, tick);
            }
            if (changed) finishIfSatisfied(player, task, data, tick);
        }
    }

    /** Freeze only a real destruction outcome while its required armor is still worn. */
    Runnable captureEquipmentDestruction(ServerPlayer player) {
        long tick=now(player);
        Map<String,CompoundTag> acceptedGoals=new java.util.LinkedHashMap<>();
        for(var task:catalog.values()) {
            var data=state(player,task.id());
            if(TaskProgress.status(data,tick)==TaskProgress.Status.ACTIVE&&equipmentAllowed(player,task)
                    &&leaves.get(task.id()).stream().anyMatch(g->g.goal().key().equals("signal:armor.gti.destroyed_absorbing")))
                acceptedGoals.put(task.id(),data.getCompound("Goals"));
        }
        return ()->{
            if(!player.isAlive()||player.isSpectator())return;
            long at=now(player);
            acceptedGoals.forEach((id,goals)->{
                var data=state(player,id);var task=catalog.get(id);
                // Reset replaces Goals even within the same tick; stale outcomes cannot cross a retry.
                if(TaskProgress.status(data,at)!=TaskProgress.Status.ACTIVE||data.getCompound("Goals")!=goals)return;
                boolean changed=false;var flat=leaves.get(id);
                for(int i=0;i<flat.size();i++)if(flat.get(i).goal().key().equals("signal:armor.gti.destroyed_absorbing"))
                    changed|=TaskProgress.record(data,i,flat.get(i).goal(),"signal:armor.gti.destroyed_absorbing",1,"",at);
                if(changed)finishIfSatisfied(player,task,data,at);
            });
        };
    }

    private double current(ServerPlayer player, DepartmentCatalog.Objective goal, CompoundTag data, int index, long tick) {
        return switch (goal.kind()) {
            case "signal" -> switch(goal.key()) {
                case "advancement.mod.normal" -> com.rzy.dealt_force_skills.advancement.DfsAchievements.completedForDepartment(player,false);
                case "advancement.mod.challenge" -> com.rzy.dealt_force_skills.advancement.DfsAchievements.completedForDepartment(player,true);
                case "commander.gene.level" -> com.rzy.dealt_force_skills.character.commander.CommanderStateManager.geneLevel(player);
                default -> TaskProgress.progress(data,index,goal.goal(),tick);
            };
            case "inventory" -> inventoryCount(player, goal.key());
            case "advancement" -> advancementDone(player, goal.key()) ? 1 : 0;
            case "facility" -> {
                var facility = SpecialOperationsBaseManager.Facility.byId(goal.key());
                yield facility == null ? 0 : SpecialOperationsBaseManager.level(player, facility);
            }
            case "equipment" -> equipped(player, goal.key()) ? 1 : 0;
            case "character" -> com.rzy.dealt_force_skills.character.CharacterSelectionManager.getSelectedCharacterId(player)
                    .filter(id -> goal.key().equals("any") || id.startsWith("dealt_force_skills:"+goal.key()+"/")
                            || id.equals(goal.key())).isPresent() ? 1 : 0;
            case "collector" -> data.getBoolean("Submitted") ? 1 : 0;
            case "checklist" -> DepartmentChecklists.progress(data,goal.key());
            default -> TaskProgress.progress(data, index, goal.goal(), tick);
        };
    }

    public void recordScoped(ServerPlayer player,String key,double amount,String scope,String target,long lifetimeTicks) {
        if(!player.isAlive()||player.isSpectator()||!player.getPersistentData().contains(KEY))return;
        long tick=now(player);
        for(var task:catalog.values()) {
            CompoundTag data=state(player,task.id());
            if(TaskProgress.status(data,tick)!=TaskProgress.Status.ACTIVE)continue;
            var flat=leaves.get(task.id());
            if(flat.stream().noneMatch(g->g.goal().key().equals("signal:"+key)))continue;
            if(!equipmentAllowed(player,task)) {
                TaskProgress.reset(data,tick,0,true);DepartmentChecklists.initialize(player,task,data);continue;
            }
            boolean changed=false;
            for(int i=0;i<flat.size();i++) {
                var goal=flat.get(i).goal();
                if(goal.key().equals("signal:"+key))changed|=TaskProgress.recordScoped(data,i,goal,goal.key(),amount,scope,target,tick,lifetimeTicks);
            }
            if(changed)finishIfSatisfied(player,task,data,tick);
        }
    }

    private static int inventoryCount(ServerPlayer player, String id) {
        int count = 0;
        for (ItemStack stack : player.getInventory().items)
            if (BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(id)) count += stack.getCount();
        return count;
    }

    private static boolean advancementDone(ServerPlayer player, String key) {
        ResourceLocation id = ResourceLocation.tryParse(key);
        if (id == null) return false;
        var advancement = player.server.getAdvancements().get(id);
        return advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone();
    }

    private static boolean equipped(ServerPlayer player, String key) {
        var head = DfsEquipmentItem.profile(player.getItemBySlot(EquipmentSlot.HEAD));
        var chest = DfsEquipmentItem.profile(player.getItemBySlot(EquipmentSlot.CHEST));
        if (key.equals("any")) return head != null || chest != null;
        if (key.equals("set")) return head != null && chest != null && head.faction() == chest.faction()
                && com.rzy.dealt_force_skills.registry.ModGameRules.areArmorSpecialsEnabled(player);
        return key.startsWith("set:") && head != null && chest != null && head.faction() == chest.faction()
                && head.faction().key().equals(key.substring(4))
                && com.rzy.dealt_force_skills.registry.ModGameRules.areArmorSpecialsEnabled(player);
    }

    private static boolean equipmentAllowed(ServerPlayer player, DepartmentCatalog.Definition task) {
        for (String policy : task.policies()) if (policy.startsWith("set:") && !equipped(player, policy)) return false;
        return true;
    }

    private boolean satisfied(ServerPlayer player, DepartmentCatalog.Objective goal,
                              List<DepartmentCatalog.Objective> flat, CompoundTag data, long tick) {
        if (goal.kind().equals("any")) return goal.children().stream().anyMatch(child -> satisfied(player, child, flat, data, tick));
        if (goal.kind().equals("all")) return goal.children().stream().allMatch(child -> satisfied(player, child, flat, data, tick));
        return current(player, goal, data, flat.indexOf(goal), tick) >= goal.target();
    }

    private static boolean hasWindow(DepartmentCatalog.Objective goal) {
        return goal.window()>0||goal.children().stream().anyMatch(DepartmentTracker::hasWindow);
    }
    private boolean rootSatisfied(ServerPlayer player,DepartmentCatalog.Definition task,int index,CompoundTag data,long tick) {
        var goal=task.goals().get(index);
        if(data.getCompound("Goals").getCompound("CompletedWindows").getBoolean(Integer.toString(index)))return true;
        return satisfied(player,goal,leaves.get(task.id()),data,tick);
    }
    private void rememberCompletedWindows(ServerPlayer player,DepartmentCatalog.Definition task,CompoundTag data,long tick) {
        var goals=data.getCompound("Goals");
        if(!goals.contains("CompletedWindows"))goals.put("CompletedWindows",new CompoundTag());
        var done=goals.getCompound("CompletedWindows");
        for(int i=0;i<task.goals().size();i++)if(hasWindow(task.goals().get(i))&&rootSatisfied(player,task,i,data,tick))
            done.putBoolean(Integer.toString(i),true);
    }

    private void finishIfSatisfied(ServerPlayer player, DepartmentCatalog.Definition task, CompoundTag data, long tick) {
        if (TaskProgress.status(data, tick) != TaskProgress.Status.ACTIVE) return;
        List<DepartmentCatalog.Objective> flat = leaves.get(task.id());
        rememberCompletedWindows(player,task,data,tick);
        for(int i=0;i<task.goals().size();i++)if(!rootSatisfied(player,task,i,data,tick))return;
        // Completion and pending delivery live in the same player save; failed deliveries retry on tick/login.
        data.putBoolean("Active", false);
        data.putBoolean("Completed", true);
        data.putLong("CompletedAt", tick);
        deliverReward(player, task, data);
        player.sendSystemMessage(Component.translatable("message.dealt_force_skills.department.completed", task.title()));
        DepartmentTaskConditions.refresh(player, this);
    }

    private void deliverReward(ServerPlayer player, DepartmentCatalog.Definition task, CompoundTag data) {
        // Also migrate completed tasks whose unlock is still sitting in an unclaimed old mail.
        boolean unlocked=false;
        for(String id:task.reward().unlocks())
            unlocked |= com.rzy.dealt_force_skills.character.TaskCharacterUnlocks.unlock(player,id);
        if(unlocked)com.rzy.dealt_force_skills.character.CharacterAvailability.syncToClient(player);
        if (data.getBoolean("RewardSent")) return;
        if(task.repeatable()) {
            CollectorOffer offer=CollectorOffer.load(player,data.getCompound("Offer"));
            if (offer == null && data.getBoolean("AdminFinish")) {
                offer = CollectorGenerator.generate(player, task.goals().get(0).key());
                if (offer != null) data.put("Offer", offer.save(player));
            }
            if(offer==null)return;
            MailboxManager.sendRewardOnce(player,"department/"+task.id()+"/"+data.getLong("Run"),task.title(),
                    offer.rewards().stream().map(CollectorOffer.Part::stack).toList(),offer.coins(),offer.experience(),List.of());
            data.putBoolean("RewardSent",true);return;
        }
        List<ItemStack> items = new ArrayList<>();
        for (var reward : task.reward().items()) {
            // Missing optional-mod rewards wait for the mod to be available, never turn into air.
            ItemStack stack = DepartmentRewards.create(reward.item(), reward.count());
            if (stack.isEmpty()) return;
            items.add(stack);
        }
        MailboxManager.sendRewardOnce(player, "department/" + task.id() + "/" + data.getLong("Run"),
                task.title(), items, task.reward().coins(), task.reward().experience(), List.of());
        data.putBoolean("RewardSent", true);
    }

    public void poll(ServerPlayer player) {
        if(!player.isAlive() || player.isSpectator())return;
        long tick = now(player);
        for (var task : catalog.values()) {
            CompoundTag data = state(player, task.id());
            if (data.getBoolean("Completed")) { deliverReward(player, task, data); continue; }
            // Gameplay penalties require an explicit choice by the player.
            if(!task.repeatable() && !task.policies().contains("contact_effects") && !task.policies().contains("idle_pain")
                    && TaskProgress.status(data,tick)==TaskProgress.Status.AVAILABLE
                    && start(player,task.id(),false)) data=state(player,task.id());
            if (TaskProgress.status(data, tick) != TaskProgress.Status.ACTIVE || !eligible(player,task.id())) continue;
            if (!equipmentAllowed(player, task)) {
                TaskProgress.reset(data, tick, 0, true);
                DepartmentChecklists.initialize(player,task,data);
            }
            else finishIfSatisfied(player, task, data, tick);
        }
        DepartmentTaskConditions.refresh(player,this);
        CollectorBoard.ensure(player,this);
    }

    public boolean submit(ServerPlayer player,String id) {
        if(!catalog.containsKey(id))return CollectorBoard.submit(player,this,id);
        var task=catalog.get(id);long tick=now(player);
        if(task==null||!task.repeatable()||!player.isAlive()||player.isSpectator())return false;
        CompoundTag data=state(player,id);
        if(TaskProgress.status(data,tick)!=TaskProgress.Status.ACTIVE||data.getBoolean("Submitted"))return false;
        CollectorOffer offer=CollectorOffer.load(player,data.getCompound("Offer"));
        if(offer==null||!offer.consume(player))return false;
        data.putBoolean("Submitted",true);
        finishIfSatisfied(player,task,data,tick);
        return true;
    }

    /** Death, actual healing and successful consumable use are independent failure reasons. */
    public void fail(ServerPlayer player, String reason) {
        long tick = now(player);
        for (var task : catalog.values()) {
            CompoundTag data = state(player, task.id());
            if (TaskProgress.status(data, tick) != TaskProgress.Status.ACTIVE) continue;
            boolean reset = reason.equals("death") && task.policies().contains("death_reset");
            boolean cooldown = reason.equals("death") && task.policies().contains("death_cooldown")
                    || (reason.equals("heal") || reason.equals("consume")) && task.policies().contains("no_heal_or_consume");
            if (reset || cooldown) {
                TaskProgress.reset(data, tick, cooldown ? 12000 : 0, !cooldown);
                if(!cooldown)DepartmentChecklists.initialize(player,task,data);
                DepartmentTaskConditions.refresh(player,this);
                player.sendSystemMessage(Component.translatable("message.dealt_force_skills.department.failed", task.title()));
            }
        }
    }

    public boolean hasPolicy(ServerPlayer player, String policy) {
        long tick = now(player);
        return catalog.values().stream().anyMatch(task -> task.policies().contains(policy)
                && eligible(player,task.id()) && TaskProgress.status(state(player, task.id()), tick) == TaskProgress.Status.ACTIVE);
    }

    public Page page(ServerPlayer player, String department, int requested, String selected) {
        if(department.equals("收集者"))return CollectorBoard.page(player,this,department,selected);
        long tick = now(player);
        List<DepartmentCatalog.Definition> visible = catalog.values().stream()
                .filter(task -> task.department().equals(department) && visible(player, task.id())).toList();
        int pages = Math.max(1, (visible.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        int page = Math.max(0, Math.min(pages - 1, requested));
        if(requested<0)for(int i=0;i<visible.size();i++)if(visible.get(i).id().equals(selected)){page=i/PAGE_SIZE;break;}
        List<Row> rows = visible.stream().skip((long) page * PAGE_SIZE).limit(PAGE_SIZE).map(task ->
                new Row(task.id(), task.chapter(), task.title(), status(player,task.id(),tick))).toList();
        var task = catalog.get(selected);
        if(task==null&&!rows.isEmpty())task=catalog.get(rows.get(0).id());
        Detail detail = null;
        if (task != null && visible.contains(task)) {
            CompoundTag data = state(player, task.id());
            List<GoalView> goals = new ArrayList<>();
            List<DepartmentCatalog.Objective> flat = leaves.get(task.id());
            for (int i = 0; i < task.goals().size(); i++) {
                var goal = task.goals().get(i);
                boolean done = data.getBoolean("Completed") || rootSatisfied(player,task,i,data,tick);
                goals.add(new GoalView(Component.literal(goal.description()), goal.group() ? (done ? 1 : 0)
                        : done ? goal.target() : current(player, goal, data, flat.indexOf(goal), tick), goal.group() ? 1 : goal.target(),TaskDisplay.objective(goal)));
            }
            Component rewardText=Component.literal(task.rewardDescription());
            CollectorOffer offer=task.repeatable()?CollectorOffer.load(player,data.getCompound("Offer")):null;
            if(offer!=null) {
                goals.clear();
                for(var part:offer.requirements()) {
                    int held=0;for(var stack:player.getInventory().items)if(CollectorOffer.matches(player,stack,part))held+=stack.getCount();
                    Component text=DepartmentPlatform.itemName(player,part.template());
                    goals.add(new GoalView(text,Math.min(part.count(),held),part.count(),List.of(new TaskDisplay.Item(part.template(),part.count()))));
                }
                var reward=Component.translatable("screen.dealt_force_skills.department.exchange_reward",offer.coins(),offer.experience());
                for(var part:offer.rewards())reward.append("\n").append(DepartmentPlatform.itemName(player,part.template())).append(" ×"+part.count());
                rewardText=reward;
            }
            detail = new Detail(task.id(), task.title(), task.brief(), rewardText, task.constraints(),
                    status(player,task.id(),tick), Math.max(0, data.getLong("RetryAt") - tick), task.repeatable(),List.copyOf(goals),
                    prerequisites(player,task.id()),false,0,offer==null?TaskDisplay.reward(task.reward()):TaskDisplay.offer(offer));
        }
        return new Page(department, page, pages, rows, detail);
    }
}
