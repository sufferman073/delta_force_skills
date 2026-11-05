package com.rzy.dealt_force_skills.ai.operative;

import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.character.SkillSlot;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import com.rzy.dealt_force_skills.character.commander.CommanderSkillChoice;
import com.rzy.dealt_force_skills.character.commander.CommanderStateManager;

/** Inherited loadout with all bounded Commander growth applied. */
public final class NativeCommanderSkills implements OperativeSkills {
    private final NativeDWolfSkills wolf = new NativeDWolfSkills(OperativeGrowth.commanderEffect());
    private final NativeLunaSkills luna = new NativeLunaSkills(OperativeGrowth.commanderEffect());
    private final NativeStingerSkills stinger = new NativeStingerSkills(OperativeGrowth.commanderEffect());
    private final NativeGizmoSkills gizmo = new NativeGizmoSkills(OperativeGrowth.commanderEffect());
    private final NativeUluruSkills uluru = new NativeUluruSkills(OperativeGrowth.commanderEffect());
    private final Map<String, OperativeSkills> sources = new LinkedHashMap<>();
    private final Map<SkillSlot, String> lastSources = new java.util.EnumMap<>(SkillSlot.class);
    private final List<OperativeSkills> modules;

    public NativeCommanderSkills() {
        sources.put(ModCharacters.D_WOLF_ID, wolf); sources.put(ModCharacters.LUNA_ID, luna);
        sources.put(ModCharacters.STINGER_ID, stinger); sources.put(ModCharacters.GIZMO_ID, gizmo);
        sources.put(ModCharacters.ULURU_ID, uluru);
        for (var choice : CommanderSkillChoice.values())
            for (var definition : CommanderStateManager.availableSources(choice))
                if (NativeOperativeSkills.supports(definition.id()))
                    sources.computeIfAbsent(definition.id(), NativeOperativeSkills::create);
        modules = List.copyOf(sources.values());
    }

    /** Projectiles and event passives resolve the same persistent module as the brain. */
    public static OperativeSkills inherited(OperativeSkills skills, Class<? extends OperativeSkills> type) {
        if (type.isInstance(skills)) return skills;
        if (skills instanceof NativeCommanderSkills commander)
            for (var module : commander.modules) if (type.isInstance(module)) return module;
        return null;
    }

    @Override public void tick(OperativeHost host) {
        OperativeGrowth.applyCommanderGrowth(host.body());
        if(host.body().tickCount%20==0)host.body().heal(host.body().getMaxHealth()*.005F*OperativeGrowth.gene());
        if (!NativeRaptorSkills.available(host) || OperativeSupport.downed(host.body())) { cancel(host); return; }
        if (host.body().tickCount % 10 == 0)
            for (var target : host.serverLevel().getEntitiesOfClass(LivingEntity.class,
                    host.body().getBoundingBox().inflate(50), host::isEnemy)) {
                target.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.GLOWING, 20), host.body());
                OperativeRecon.reveal(host, target, 20);
            }
        // Tick committed actions independently; rescue does not cancel other modules.
        stinger.tick(host);
        wolf.tick(host, modules.stream().noneMatch(module -> module != wolf && module.busy()));
        for (var module : modules) if (module != stinger && module != wolf) module.tick(host);
    }

    private OperativeSkills selected(OperativeHost host, SkillSlot slot, LivingEntity target) {
        // An active anchor owns its second core press until recall finishes.
        if (slot == SkillSlot.CORE && sources.get(ModCharacters.TEMPEST_ID) instanceof NativeTempestSkills tempest
                && tempest.ropeActive()) return tempest;
        CommanderSkillChoice choice = switch (slot) {
            case ACTIVE_1 -> CommanderSkillChoice.ACTIVE_1;
            case ACTIVE_2 -> CommanderSkillChoice.ACTIVE_2;
            case CORE -> CommanderSkillChoice.CORE_1;
            default -> null;
        };
        if (choice == null) return null;
        OperativeSkills best = null;
        double bestScore = 0;
        for (var definition : CommanderStateManager.availableSources(choice)) {
            var module = sources.get(definition.id());
            if (module == null) continue;
            double score = module.score(host, slot, target);
            // Prefer an equally useful alternative without overriding emergency utility.
            if (definition.id().equals(lastSources.get(slot))) score *= .9;
            if (score > bestScore) { best = module; bestScore = score; }
        }
        return best;
    }

    @Override public double score(OperativeHost host, SkillSlot slot, LivingEntity target) {
        if (busy() || !NativeRaptorSkills.available(host) || OperativeSupport.downed(host.body())
                || OperativeElectronics.blocked(host, ModCharacters.COMMANDER_ID, slot)) return 0;
        var module = selected(host, slot, target);
        return module == null ? 0 : module.score(host, slot, target);
    }

    @Override public boolean start(OperativeHost host, SkillSlot slot, LivingEntity target) {
        if (score(host, slot, target) <= 0) return false;
        var module = selected(host, slot, target);
        if (module == null || !module.start(host, slot, target)) return false;
        sources.forEach((id, candidate) -> { if (candidate == module) lastSources.put(slot, id); });
        return true;
    }

    @Override public void remember(LivingEntity target, int ticks) { modules.forEach(module -> module.remember(target, ticks)); }
    @Override public boolean isRevealed(LivingEntity target) { return modules.stream().anyMatch(module -> module.isRevealed(target)); }
    @Override public double meleeIntervalTicks(OperativeHost host) { return gizmo.meleeIntervalTicks(host); }
    @Override public boolean locksMovement() { return modules.stream().anyMatch(OperativeSkills::locksMovement); }
    @Override public boolean locksWeapon() { return modules.stream().anyMatch(OperativeSkills::locksWeapon); }
    @Override public boolean busy() { return modules.stream().anyMatch(OperativeSkills::busy); }
    @Override public void cancel(OperativeHost host) { modules.forEach(module -> module.cancel(host)); }

    @Override public void refresh(OperativeHost h) { modules.forEach(module -> module.refresh(h)); }
    @Override public CompoundTag save() {
        var tag = new CompoundTag();
        tag.put("Wolf", wolf.save()); tag.put("Luna", luna.save()); tag.put("Stinger", stinger.save());
        tag.put("Gizmo", gizmo.save()); tag.put("Uluru", uluru.save());
        var additional = new CompoundTag();
        sources.forEach((id, module) -> additional.put(id, module.save()));
        tag.put("Sources", additional);
        lastSources.forEach((slot, id) -> tag.putString("LastSource" + slot.name(), id));
        return tag;
    }
    @Override public void load(CompoundTag tag) {
        if (tag.contains("Wolf", 10)) {
            var selected = tag.getCompound("Wolf").copy();
            wolf.load(selected);
        }
        if (tag.contains("Luna", 10)) luna.load(tag.getCompound("Luna"));
        if (tag.contains("Stinger", 10)) stinger.load(tag.getCompound("Stinger"));
        if (tag.contains("Gizmo", 10)) gizmo.load(tag.getCompound("Gizmo"));
        if (tag.contains("Uluru", 10)) uluru.load(tag.getCompound("Uluru"));
        var additional = tag.getCompound("Sources");
        sources.forEach((id, module) -> {
            if (module != wolf && module != luna && module != stinger && module != gizmo && module != uluru
                    && additional.contains(id, 10)) module.load(additional.getCompound(id));
        });
        lastSources.clear();
        for (var slot : SkillSlot.values()) {
            String id = tag.getString("LastSource" + slot.name());
            if (sources.containsKey(id)) lastSources.put(slot, id);
        }
    }
    @Override public String describe() { return "commander{" + wolf.describe() + ";" + luna.describe()
            + ";" + stinger.describe() + ";" + uluru.describe() + "}"; }
}
