package com.rzy.dealt_force_skills.feedback;

/** Shared wire identifiers and presentation metadata, without client class references. */
public record SkillNotice(String id, String character, String icon, int color, int interval) {
    private static final java.util.Map<String, SkillNotice> ALL = new java.util.HashMap<>();
    static {
        add("self_rescue", "stinger", "stinger.skill.professional_rescue", 0xFFFF5959, 60);
        add("tiger", "vyron", "vyron.skill.tiger_cannon", 0xFFFF5959, 60);
        add("wound", "nox", "nox.skill.delayed_wound", 0xFFFF5959, 60);
        add("stealth", "nox", "nox.skill.silent_step", 0xFFFF5959, 100);
        add("disarmed", "tempest", "tempest.skill.wall_drill_stinger", 0xFFFF5959, 60);
        add("stim", "stinger", "stinger.skill.stim_gun", 0xFFFF5959, 60);
        add("firefly", "toxik", "toxik.skill.firefly_swarm", 0xFFFF5959, 60);
        add("dust", "vlinder", "vlinder.skill.medical_drone", 0xFFFF5959, 60);
        add("grapple", "sineva", "sineva.skill.grapple", 0xFFFF5959, 60);
        add("missile", "uluru", "uluru.skill.loitering_missile", 0xFFFF5959, 60);
        add("smoke_trap", "gizmo", "gizmo.skill.smoke_trap", 0xFFFF5959, 60);
        add("spider_nest", "gizmo", "gizmo.skill.spider_nest", 0xFFFF5959, 60);
        add("gizmo_reveal", "gizmo", "gizmo.skill.smoke_trap", 0xFFFF5959, 60);
        add("sonic_trap", "shepherd", "shepherd.skill.sonic_trap", 0xFFFF5959, 60);
        add("explosive_trap", "department", "department.skill.armor", 0xFFFF5959, 60);
        add("tracking", "ntwo", "ntwo.skill.tracking_stun", 0xFFFF5959, 60);
        add("frozen", "ntwo", "ntwo.skill.condensed_launcher", 0xFFFF5959, 60);
        add("luna_passive", "luna", "luna.skill.target_trace", 0xFFFF5959, 60);
        add("recon", "luna", "luna.skill.recon_arrow", 0xFFFF5959, 20);
        add("recon_scan", "luna", "luna.skill.recon_arrow", 0xFFF0B443, 60);
        add("hack", "hackclaw", "hackclaw.skill.advanced_hack", 0xFFFF5959, 60);
        add("sonar_scan", "morse", "morse.skill.sonar_detector", 0xFFF0B443, 10);
        add("sonar", "morse", "morse.skill.sonar_detector", 0xFFFF5959, 60);
        add("falcon", "raptor", "raptor.skill.falcon_drone", 0xFFFF5959, 60);
        add("hummingbird", "raptor", "raptor.skill.hummingbird_camera", 0xFFFF5959, 60);
        add("guard", "saeed", "saeed.skill.command_order", 0xFFFF5959, 60);
        add("locked", "ghroth", "ghroth.skill.demigod_brain", 0xFFFF5959, 60);
        add("duel", "manba", "manba.skill.la_legend", 0xFFFF5959, 60);
        add("counter", "catdad", "catdad.skill.bully", 0xFFFF5959, 60);
        add("truck", "catdad", "catdad.skill.highway", 0xFFFF5959, 60);
        add("witch", "nikaidou_hiro_witchification", "nikaidou_hiro_witchification.skill.erase_error", 0xFFFF5959, 60);
        add("calibration", "department", "department.skill.traffic_booth", 0xFFFF5959, 60);
        add("exploration", "undead", "undead.explorer.skill.active1", 0xFFFF5959, 60);
        add("bleeding", "undead", "undead.scholar.skill.active1", 0xFFFF5959, 60);
        add("parried", "undead", "undead.knight.skill.active2", 0xFFFF5959, 60);
        add("knocked", "undead", "undead.rogue.skill.active2", 0xFFFF5959, 60);
        add("stolen", "undead", "undead.rogue.skill.core", 0xFFFF5959, 60);
        add("pursuit", "corps", "corps.skill.unrestricted_fighting_tournament", 0xFFFF5959, 60);
        add("backlash", "lex_ninjia", "lex_ninjia.skill.hand", 0xFFFF5959, 60);
        add("shield", "gambler", "gambler.skill.super_shield", 0xFFFF5959, 60);
        add("trademark", "chamber", "chamber.skill.trademark", 0xFFFF5959, 60);
    }
    private static void add(String id, String character, String icon, int color, int interval) {
        ALL.put(id, new SkillNotice(id, character, icon, color, interval));
    }
    public static SkillNotice find(String id) { return ALL.get(id); }
    public String translationKey() { return "notification.dealt_force_skills." + id; }
}
