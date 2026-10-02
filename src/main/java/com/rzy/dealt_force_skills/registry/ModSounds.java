package com.rzy.dealt_force_skills.registry;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.lexninjia.LexNinjiaArt;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, DealtForceSkillsMod.MODID);

    public static final Map<String, DeferredHolder<SoundEvent, SoundEvent>> LEX_NINJIA_SOUNDS = registerLexNinjiaSounds();
    public static final Map<String, DeferredHolder<SoundEvent, SoundEvent>> GAMBLER_POWER_SOUNDS = registerGamblerPowerSounds();

    public static final DeferredHolder<SoundEvent, SoundEvent> ROVER_SMOKE_EQUIP = reg("rover.smoke_equip");
    public static final DeferredHolder<SoundEvent, SoundEvent> ROVER_SMOKE_STOW = reg("rover.smoke_stow");
    public static final DeferredHolder<SoundEvent, SoundEvent> ROVER_SMOKE_THROW = reg("rover.smoke_throw");
    public static final DeferredHolder<SoundEvent, SoundEvent> ROVER_SMOKE_BOUNCE = reg("rover.smoke_bounce");
    public static final DeferredHolder<SoundEvent, SoundEvent> ROVER_SMOKE_LAND = reg("rover.smoke_land");
    public static final DeferredHolder<SoundEvent, SoundEvent> ROVER_SMOKE_BURST = reg("rover.smoke_burst");
    public static final DeferredHolder<SoundEvent, SoundEvent> ROVER_INJECTOR_EQUIP = reg("rover.injector_equip");
    public static final DeferredHolder<SoundEvent, SoundEvent> ROVER_INJECTOR_MODE = reg("rover.injector_mode");
    public static final DeferredHolder<SoundEvent, SoundEvent> ROVER_INJECTOR_FIRE = reg("rover.injector_fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> ROVER_INJECTOR_BURST = reg("rover.injector_burst");
    public static final DeferredHolder<SoundEvent, SoundEvent> ROVER_PURIFIED = reg("rover.purified");
    public static final DeferredHolder<SoundEvent, SoundEvent> ROVER_CLEANSE = reg("rover.cleanse");
    public static final DeferredHolder<SoundEvent, SoundEvent> ROVER_DAMAGE = reg("rover.damage");
    public static final DeferredHolder<SoundEvent, SoundEvent> ROVER_DOG_SUMMON = reg("rover.dog_summon");
    public static final DeferredHolder<SoundEvent, SoundEvent> ROVER_DOG_SPAWN = reg("rover.dog_spawn");
    public static final DeferredHolder<SoundEvent, SoundEvent> ROVER_DOG_COMMAND = reg("rover.dog_command");
    public static final DeferredHolder<SoundEvent, SoundEvent> ROVER_DOG_RECALL = reg("rover.dog_recall");
    public static final DeferredHolder<SoundEvent, SoundEvent> ROVER_DOG_TARGET = reg("rover.dog_target");
    public static final DeferredHolder<SoundEvent, SoundEvent> ROVER_DOG_ATTACK = reg("rover.dog_attack");
    public static final DeferredHolder<SoundEvent, SoundEvent> ROVER_DOG_RETREAT = reg("rover.dog_retreat");
    public static final DeferredHolder<SoundEvent, SoundEvent> ROVER_DOG_KILL_OWNER = reg("rover.dog_kill_owner");
    public static final DeferredHolder<SoundEvent, SoundEvent> ROVER_DOG_KILL_VICTIM = reg("rover.dog_kill_victim");

    public static final DeferredHolder<SoundEvent, SoundEvent> SKILLS_CD = reg("skills_cd");
    public static final DeferredHolder<SoundEvent, SoundEvent> SKILLS_READY = reg("skills_ready");
    public static final DeferredHolder<SoundEvent, SoundEvent> GAMBLER_DRAW = reg("gambler.draw");
    public static final DeferredHolder<SoundEvent, SoundEvent> GAMBLER_SHIELD_BLOCK = reg("gambler.shield_block");
    public static final DeferredHolder<SoundEvent, SoundEvent> GAMBLER_HAKKO_ICHIU_BGM = reg("gambler.hakko_ichiu_bgm");
    public static final DeferredHolder<SoundEvent, SoundEvent> GAMBLER_ARENA_BGM = reg("gambler.arena_bgm");
    public static final DeferredHolder<SoundEvent, SoundEvent> GAMBLER_CORE_JACKPOT_BGM = reg("gambler.core_jackpot_bgm");

    public static final DeferredHolder<SoundEvent, SoundEvent> SHIELD_BASH = reg("shield_bash");

    public static final DeferredHolder<SoundEvent, SoundEvent> GRAPPLE_FIRE = reg("grapple_fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> GRAPPLE_HIT = reg("grapple_hit");

    public static final DeferredHolder<SoundEvent, SoundEvent> WIRE_PLACE = reg("wire_place");
    public static final DeferredHolder<SoundEvent, SoundEvent> WIRE_STEP = reg("wire_step");
    public static final DeferredHolder<SoundEvent, SoundEvent> HVK_CONSTRUCTOR_CRAFT_START =
            reg("hvk_constructor_craft_start");
    public static final DeferredHolder<SoundEvent, SoundEvent> HVK_CONSTRUCTOR_CRAFT_FINISH =
            reg("hvk_constructor_craft_finish");
    public static final DeferredHolder<SoundEvent, SoundEvent> HVK_ASARA_ARENA_OPEN = reg("hvk_asara_arena_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> HVK_ASARA_ARENA_START = reg("hvk_asara_arena_start");
    public static final DeferredHolder<SoundEvent, SoundEvent> HVK_ASARA_ARENA_FAILURE = reg("hvk_asara_arena_failure");
    public static final DeferredHolder<SoundEvent, SoundEvent> HVK_ASARA_ARENA_SUCCESS = reg("hvk_asara_arena_success");
    public static final DeferredHolder<SoundEvent, SoundEvent> HVK_ENCHANT_INFUSION_UPGRADE =
            reg("hvk_enchant_infusion_upgrade");
    public static final DeferredHolder<SoundEvent, SoundEvent> HVK_ENCHANT_INFUSION_DOWNGRADE_OR_CLEAR =
            reg("hvk_enchant_infusion_downgrade_or_clear");
    public static final DeferredHolder<SoundEvent, SoundEvent> HVK_DISASSEMBLER_USE =
            reg("hvk_disassembler_use");
    public static final DeferredHolder<SoundEvent, SoundEvent> HVK_DISASSEMBLER_REPEL =
            reg("hvk_disassembler_repel");

    public static final DeferredHolder<SoundEvent, SoundEvent> BOMB_SUIT_EQUIP = reg("bomb_suit_equip");
    public static final DeferredHolder<SoundEvent, SoundEvent> SINEVA_SHIELD_BASH = reg("sineva.shield_bash");
    public static final DeferredHolder<SoundEvent, SoundEvent> SINEVA_SHIELD_BASH_CHARGE = reg("sineva.shield_bash_charge");
    public static final DeferredHolder<SoundEvent, SoundEvent> SINEVA_SHIELD_HIT = reg("sineva.shield_hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> SINEVA_PROJECTILE_REFLECT = reg("sineva.projectile_reflect");
    public static final DeferredHolder<SoundEvent, SoundEvent> SINEVA_SHIELD_BASH_CONTROL_VOICE = reg("sineva.shield_bash_control_voice");
    public static final DeferredHolder<SoundEvent, SoundEvent> SINEVA_SHIELD_TOGGLE = reg("sineva.shield_toggle");
    public static final DeferredHolder<SoundEvent, SoundEvent> SINEVA_GRAPPLE_FIRE = reg("sineva.grapple_fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> SINEVA_RIOT_SUIT_PREPARE = reg("sineva.riot_suit_prepare");
    public static final DeferredHolder<SoundEvent, SoundEvent> SINEVA_RIOT_SUIT_WALK = reg("sineva.riot_suit_walk");
    public static final DeferredHolder<SoundEvent, SoundEvent> SINEVA_GRAPPLE_HIT = reg("sineva.grapple_hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> SINEVA_GRAPPLE_HIT_VOICE = reg("sineva.grapple_hit_voice");
    public static final DeferredHolder<SoundEvent, SoundEvent> SINEVA_GRAPPLE_DRAG = reg("sineva.grapple_drag");
    public static final DeferredHolder<SoundEvent, SoundEvent> SINEVA_GRAPPLE_BREAK_VOICE = reg("sineva.grapple_break_voice");
    public static final DeferredHolder<SoundEvent, SoundEvent> SINEVA_GRAPPLE_CHARGE_READY = reg("sineva.grapple_charge_ready");
    public static final DeferredHolder<SoundEvent, SoundEvent> SINEVA_VIEWPORT_BREAK = reg("sineva.viewport_break");
    public static final DeferredHolder<SoundEvent, SoundEvent> SINEVA_VIEWPORT_BREAK_VOICE = reg("sineva.viewport_break_voice");
    public static final DeferredHolder<SoundEvent, SoundEvent> SINEVA_BARBED_WIRE_THROW = reg("sineva.barbed_wire_throw");
    public static final DeferredHolder<SoundEvent, SoundEvent> SINEVA_GRAPPLE_RETRACT = reg("sineva.grapple_retract");
    public static final DeferredHolder<SoundEvent, SoundEvent> SINEVA_RIOT_SUIT_VOICE = reg("sineva.riot_suit_voice");
    public static final DeferredHolder<SoundEvent, SoundEvent> SINEVA_BARBED_WIRE_LAND = reg("sineva.barbed_wire_land");
    public static final DeferredHolder<SoundEvent, SoundEvent> SINEVA_GRAPPLE_CHARGE = reg("sineva.grapple_charge");
    public static final DeferredHolder<SoundEvent, SoundEvent> SINEVA_WALK_ON_BARBED_WIRE = reg("sineva.walk_on_barbed_wire");

    public static final DeferredHolder<SoundEvent, SoundEvent> INCENDIARY_READY = reg("incendiary_ready");
    public static final DeferredHolder<SoundEvent, SoundEvent> INCENDIARY_THROW = reg("incendiary_throw");
    public static final DeferredHolder<SoundEvent, SoundEvent> INCENDIARY_EXPLODE = reg("incendiary_explode");

    public static final DeferredHolder<SoundEvent, SoundEvent> QUICK_COVER_READY = reg("quick_cover_ready");
    public static final DeferredHolder<SoundEvent, SoundEvent> QUICK_COVER_THROW = reg("quick_cover_throw");
    public static final DeferredHolder<SoundEvent, SoundEvent> QUICK_COVER_DEPLOY = reg("quick_cover_deploy");

    public static final DeferredHolder<SoundEvent, SoundEvent> MISSILE_FIRE = reg("missile_fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> MISSILE_FLY = regFixed("missile_fly", 64.0F);
    public static final DeferredHolder<SoundEvent, SoundEvent> MISSILE_EXPLODE = reg("missile_explode");
    public static final DeferredHolder<SoundEvent, SoundEvent> BOMBLET_EXPLODE = reg("bomblet_explode");

    public static final DeferredHolder<SoundEvent, SoundEvent> D_WOLF_HAND_CANNON_EQUIP = reg("d_wolf_hand_cannon_equip");
    public static final DeferredHolder<SoundEvent, SoundEvent> D_WOLF_HAND_CANNON_FIRE = reg("d_wolf_hand_cannon_fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> D_WOLF_HAND_CANNON_FUSE = reg("d_wolf_hand_cannon_fuse");
    public static final DeferredHolder<SoundEvent, SoundEvent> D_WOLF_HAND_CANNON_IMPACT = reg("d_wolf_hand_cannon_impact");
    public static final DeferredHolder<SoundEvent, SoundEvent> D_WOLF_HAND_CANNON_EXPLODE = reg("d_wolf_hand_cannon_explode");
    public static final DeferredHolder<SoundEvent, SoundEvent> D_WOLF_SMOKE_EQUIP = reg("d_wolf_smoke_equip");
    public static final DeferredHolder<SoundEvent, SoundEvent> D_WOLF_SMOKE_THROW = reg("d_wolf_smoke_throw");
    public static final DeferredHolder<SoundEvent, SoundEvent> D_WOLF_SMOKE_BURST = reg("d_wolf_smoke_burst");
    public static final DeferredHolder<SoundEvent, SoundEvent> D_WOLF_OVERLOAD_START = reg("d_wolf_overload_start");
    public static final DeferredHolder<SoundEvent, SoundEvent> D_WOLF_OVERLOAD_ACTIVE = reg("d_wolf_overload_active");
    public static final DeferredHolder<SoundEvent, SoundEvent> D_WOLF_OVERLOAD_KILL_EXTENSION = reg("d_wolf_overload_kill_extension");
    public static final DeferredHolder<SoundEvent, SoundEvent> D_WOLF_OVERLOAD_STEP = reg("d_wolf_overload_step");
    public static final DeferredHolder<SoundEvent, SoundEvent> D_WOLF_OVERLOAD_END = reg("d_wolf_overload_end");

    public static final DeferredHolder<SoundEvent, SoundEvent> GIZMO_SMOKE_TRAP_EQUIP = reg("gizmo_smoke_trap_equip");
    public static final DeferredHolder<SoundEvent, SoundEvent> GIZMO_SMOKE_TRAP_DEPLOY = reg("gizmo_smoke_trap_deploy");
    public static final DeferredHolder<SoundEvent, SoundEvent> GIZMO_SMOKE_TRAP_READY = reg("gizmo_smoke_trap_ready");
    public static final DeferredHolder<SoundEvent, SoundEvent> GIZMO_SMOKE_TRAP_TRIGGER = reg("gizmo_smoke_trap_trigger");
    public static final DeferredHolder<SoundEvent, SoundEvent> GIZMO_SPIDER_NEST_EQUIP = reg("gizmo_spider_nest_equip");
    public static final DeferredHolder<SoundEvent, SoundEvent> GIZMO_SPIDER_NEST_DEPLOY = reg("gizmo_spider_nest_deploy");
    public static final DeferredHolder<SoundEvent, SoundEvent> GIZMO_SPIDER_NEST_READY = reg("gizmo_spider_nest_ready");
    public static final DeferredHolder<SoundEvent, SoundEvent> GIZMO_SPIDER_NEST_TRIGGER = reg("gizmo_spider_nest_trigger");
    public static final DeferredHolder<SoundEvent, SoundEvent> GIZMO_SPIDER_HIT_VOICE_1 = reg("gizmo_spider_hit_voice_1");
    public static final DeferredHolder<SoundEvent, SoundEvent> GIZMO_SPIDER_HIT_VOICE_2 = reg("gizmo_spider_hit_voice_2");
    public static final DeferredHolder<SoundEvent, SoundEvent> GIZMO_SPIDER_HIT_VOICE_3 = reg("gizmo_spider_hit_voice_3");
    public static final DeferredHolder<SoundEvent, SoundEvent> GIZMO_SPIDERLING_CRAWL = reg("gizmo_spiderling_crawl");
    public static final DeferredHolder<SoundEvent, SoundEvent> GIZMO_SPIDERLING_EXPLODE = reg("gizmo_spiderling_explode");
    public static final DeferredHolder<SoundEvent, SoundEvent> GIZMO_T_BOY_EQUIP = reg("gizmo_t_boy_equip");
    public static final DeferredHolder<SoundEvent, SoundEvent> GIZMO_T_BOY_DEPLOY = reg("gizmo_t_boy_deploy");
    public static final DeferredHolder<SoundEvent, SoundEvent> GIZMO_T_BOY_HIT_VOICE_1 = reg("gizmo_t_boy_hit_voice_1");
    public static final DeferredHolder<SoundEvent, SoundEvent> GIZMO_T_BOY_HIT_VOICE_2 = reg("gizmo_t_boy_hit_voice_2");
    public static final DeferredHolder<SoundEvent, SoundEvent> GIZMO_T_BOY_HIT_VOICE_3 = reg("gizmo_t_boy_hit_voice_3");
    public static final DeferredHolder<SoundEvent, SoundEvent> GIZMO_T_BOY_TRIGGER = reg("gizmo_t_boy_trigger");
    public static final DeferredHolder<SoundEvent, SoundEvent> GIZMO_T_BOY_CRAWL = reg("gizmo_t_boy_crawl");

    public static final DeferredHolder<SoundEvent, SoundEvent> SHEPHERD_SONIC_TRAP_EQUIP = reg("shepherd_sonic_trap_equip");
    public static final DeferredHolder<SoundEvent, SoundEvent> SHEPHERD_SONIC_TRAP_DEPLOY = reg("shepherd_sonic_trap_deploy");
    public static final DeferredHolder<SoundEvent, SoundEvent> SHEPHERD_SONIC_TRAP_READY = reg("shepherd_sonic_trap_ready");
    public static final DeferredHolder<SoundEvent, SoundEvent> SHEPHERD_SONIC_TRAP_TRIGGER = reg("shepherd_sonic_trap_trigger");
    public static final DeferredHolder<SoundEvent, SoundEvent> SHEPHERD_SONIC_TRAP_EXPLODE = reg("shepherd_sonic_trap_explode");
    public static final DeferredHolder<SoundEvent, SoundEvent> SHEPHERD_SONIC_SHOCK_HIT = reg("shepherd_sonic_shock_hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> SHEPHERD_FRAG_GRENADE_EQUIP = reg("shepherd_frag_grenade_equip");
    public static final DeferredHolder<SoundEvent, SoundEvent> SHEPHERD_FRAG_GRENADE_PIN = reg("shepherd_frag_grenade_pin");
    public static final DeferredHolder<SoundEvent, SoundEvent> SHEPHERD_FRAG_GRENADE_THROW = reg("shepherd_frag_grenade_throw");
    public static final DeferredHolder<SoundEvent, SoundEvent> SHEPHERD_FRAG_GRENADE_BOUNCE = reg("shepherd_frag_grenade_bounce");
    public static final DeferredHolder<SoundEvent, SoundEvent> SHEPHERD_FRAG_GRENADE_EXPLODE = reg("shepherd_frag_grenade_explode");
    public static final DeferredHolder<SoundEvent, SoundEvent> SHEPHERD_FRAG_GRENADE_HIT = reg("shepherd_frag_grenade_hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> SHEPHERD_ARMOR_BREAK = reg("shepherd_armor_break");
    public static final DeferredHolder<SoundEvent, SoundEvent> SHEPHERD_DRONE_TAKEOFF = reg("shepherd_drone_takeoff");
    public static final DeferredHolder<SoundEvent, SoundEvent> SHEPHERD_DRONE_FLY = reg("shepherd_drone_fly");
    public static final DeferredHolder<SoundEvent, SoundEvent> SHEPHERD_DRONE_COUNTDOWN = reg("shepherd_drone_countdown");
    public static final DeferredHolder<SoundEvent, SoundEvent> SHEPHERD_DRONE_STUN = reg("shepherd_drone_stun");

    public static final DeferredHolder<SoundEvent, SoundEvent> LUNA_BOW_EQUIP = reg("luna_bow_equip");
    public static final DeferredHolder<SoundEvent, SoundEvent> LUNA_BOW_CHARGE = reg("luna_bow_charge");
    public static final DeferredHolder<SoundEvent, SoundEvent> LUNA_ARROW_RELEASE = reg("luna_arrow_release");
    public static final DeferredHolder<SoundEvent, SoundEvent> LUNA_SHOCK_ARROW_PULSE = reg("luna_shock_arrow_pulse");
    public static final DeferredHolder<SoundEvent, SoundEvent> LUNA_GRENADE_EQUIP = reg("luna_grenade_equip");
    public static final DeferredHolder<SoundEvent, SoundEvent> LUNA_GRENADE_PIN = reg("luna_grenade_pin");
    public static final DeferredHolder<SoundEvent, SoundEvent> LUNA_GRENADE_THROW = reg("luna_grenade_throw");
    public static final DeferredHolder<SoundEvent, SoundEvent> LUNA_GRENADE_BOUNCE = reg("luna_grenade_bounce");
    public static final DeferredHolder<SoundEvent, SoundEvent> LUNA_GRENADE_EXPLODE = reg("luna_grenade_explode");
    public static final DeferredHolder<SoundEvent, SoundEvent> LUNA_GRENADE_HIT = reg("luna_grenade_hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> LUNA_ARMOR_BREAK = reg("luna_armor_break");
    public static final DeferredHolder<SoundEvent, SoundEvent> LUNA_RECON_ARROW_FLY = regFixed("luna_recon_arrow_fly", 40.0F);
    public static final DeferredHolder<SoundEvent, SoundEvent> LUNA_RECON_DETECTION = regFixed("luna_recon_detection", 40.0F);
    public static final DeferredHolder<SoundEvent, SoundEvent> LUNA_POSITION_REVEAL = reg("luna_position_reveal");
    public static final DeferredHolder<SoundEvent, SoundEvent> LUNA_REVEAL_VOICE = reg("luna_reveal_voice");

    public static final DeferredHolder<SoundEvent, SoundEvent> HACKCLAW_KNIFE_EQUIP = reg("hackclaw_knife_equip");
    public static final DeferredHolder<SoundEvent, SoundEvent> HACKCLAW_KNIFE_THROW = reg("hackclaw_knife_throw");
    public static final DeferredHolder<SoundEvent, SoundEvent> HACKCLAW_INTERFERENCE_DEPLOY = reg("hackclaw_interference_deploy");
    public static final DeferredHolder<SoundEvent, SoundEvent> HACKCLAW_HACK_SUCCESS = reg("hackclaw_hack_success");
    public static final DeferredHolder<SoundEvent, SoundEvent> HACKCLAW_ELECTRONIC_DEVICE_DESTROYED = reg("hackclaw_electronic_device_destroyed");
    public static final DeferredHolder<SoundEvent, SoundEvent> HACKCLAW_FLASH_DRONE_THROW = reg("hackclaw_flash_drone_throw");
    public static final DeferredHolder<SoundEvent, SoundEvent> HACKCLAW_FLASH_DRONE_FLASH = reg("hackclaw_flash_drone_flash");
    public static final DeferredHolder<SoundEvent, SoundEvent> HACKCLAW_FLASH_DRONE_DESTROYED = reg("hackclaw_flash_drone_destroyed");

    public static final DeferredHolder<SoundEvent, SoundEvent> VYRON_POWERED_LAND = reg("vyron_powered_land");
    public static final DeferredHolder<SoundEvent, SoundEvent> VYRON_POWERED_ACTIVATE = reg("vyron_powered_activate");
    public static final DeferredHolder<SoundEvent, SoundEvent> VYRON_DASH = reg("vyron_dash");
    public static final DeferredHolder<SoundEvent, SoundEvent> VYRON_FALLING_LOOP = reg("vyron_falling_loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> VYRON_MAGNETIC_BOMB_EQUIP = reg("vyron_magnetic_bomb_equip");
    public static final DeferredHolder<SoundEvent, SoundEvent> VYRON_MAGNETIC_BOMB_STOW = reg("vyron_magnetic_bomb_stow");
    public static final DeferredHolder<SoundEvent, SoundEvent> VYRON_MAGNETIC_BOMB_THROW = reg("vyron_magnetic_bomb_throw");
    public static final DeferredHolder<SoundEvent, SoundEvent> VYRON_MAGNETIC_BOMB_ATTACH = reg("vyron_magnetic_bomb_attach");
    public static final DeferredHolder<SoundEvent, SoundEvent> VYRON_MAGNETIC_BOMB_COUNTDOWN = reg("vyron_magnetic_bomb_countdown");
    public static final DeferredHolder<SoundEvent, SoundEvent> VYRON_MAGNETIC_BOMB_EXPLODE = reg("vyron_magnetic_bomb_explode");
    public static final DeferredHolder<SoundEvent, SoundEvent> VYRON_POWERED_ACTIVE = reg("vyron_powered_active");
    public static final DeferredHolder<SoundEvent, SoundEvent> VYRON_TIGER_CANNON_EQUIP = reg("vyron_tiger_cannon_equip");
    public static final DeferredHolder<SoundEvent, SoundEvent> VYRON_TIGER_CANNON_STOW = reg("vyron_tiger_cannon_stow");
    public static final DeferredHolder<SoundEvent, SoundEvent> VYRON_TIGER_CANNON_READY = reg("vyron_tiger_cannon_ready");
    public static final DeferredHolder<SoundEvent, SoundEvent> VYRON_TIGER_CANNON_FIRE = reg("vyron_tiger_cannon_fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> VYRON_TIGER_CANNON_BOUNCE = reg("vyron_tiger_cannon_bounce");
    public static final DeferredHolder<SoundEvent, SoundEvent> VYRON_TIGER_CANNON_EXPLODE = reg("vyron_tiger_cannon_explode");
    public static final DeferredHolder<SoundEvent, SoundEvent> VYRON_TIGER_CANNON_HIT_FEEDBACK = reg("vyron_tiger_cannon_hit_feedback");

    public static final DeferredHolder<SoundEvent, SoundEvent> NOX_ROTOR_EQUIP = reg("nox_rotor_equip");
    public static final DeferredHolder<SoundEvent, SoundEvent> NOX_ROTOR_STOW = reg("nox_rotor_stow");
    public static final DeferredHolder<SoundEvent, SoundEvent> NOX_ROTOR_LOCK = reg("nox_rotor_lock");
    public static final DeferredHolder<SoundEvent, SoundEvent> NOX_ROTOR_THROW = reg("nox_rotor_throw");
    public static final DeferredHolder<SoundEvent, SoundEvent> NOX_ROTOR_IDLE_LOOP = reg("nox_rotor_idle_loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> NOX_ROTOR_START_FLY = reg("nox_rotor_start_fly");
    public static final DeferredHolder<SoundEvent, SoundEvent> NOX_ROTOR_FLY_LOOP = reg("nox_rotor_fly_loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> NOX_ROTOR_FLY_END = reg("nox_rotor_fly_end");
    public static final DeferredHolder<SoundEvent, SoundEvent> NOX_ROTOR_DESTROYED = reg("nox_rotor_destroyed");
    public static final DeferredHolder<SoundEvent, SoundEvent> NOX_ROTOR_BOUNCE = reg("nox_rotor_bounce");
    public static final DeferredHolder<SoundEvent, SoundEvent> NOX_ROTOR_EXPLODE = reg("nox_rotor_explode");
    public static final DeferredHolder<SoundEvent, SoundEvent> NOX_FLASH_EQUIP = reg("nox_flash_equip");
    public static final DeferredHolder<SoundEvent, SoundEvent> NOX_FLASH_PIN = reg("nox_flash_pin");
    public static final DeferredHolder<SoundEvent, SoundEvent> NOX_FLASH_THROW = reg("nox_flash_throw");
    public static final DeferredHolder<SoundEvent, SoundEvent> NOX_FLASH_BOUNCE = reg("nox_flash_bounce");
    public static final DeferredHolder<SoundEvent, SoundEvent> NOX_FLASH_EXPLODE = reg("nox_flash_explode");
    public static final DeferredHolder<SoundEvent, SoundEvent> NOX_FLASH_HIT = reg("nox_flash_hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> NOX_FLASH_HIT_FULL = reg("nox_flash_hit_full");
    public static final DeferredHolder<SoundEvent, SoundEvent> NOX_FLASH_HIT_PARTIAL = reg("nox_flash_hit_partial");
    public static final DeferredHolder<SoundEvent, SoundEvent> NOX_STEALTH_START = reg("nox_stealth_start");
    public static final DeferredHolder<SoundEvent, SoundEvent> NOX_STEALTH_ACTIVE_LOOP = reg("nox_stealth_active_loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> NOX_STEALTH_WARNING = reg("nox_stealth_warning");
    public static final DeferredHolder<SoundEvent, SoundEvent> NOX_DECOY_BREAK = reg("nox_decoy_break");

    public static final DeferredHolder<SoundEvent, SoundEvent> MANBA_ELBOW_START = reg("manba_elbow_start");
    public static final DeferredHolder<SoundEvent, SoundEvent> MANBA_ELBOW_HIT = reg("manba_elbow_hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> MANBA_FLASHLIGHT_BLIND = reg("manba_flashlight_blind");
    public static final DeferredHolder<SoundEvent, SoundEvent> MANBA_DUEL_BGM = reg("manba_duel_bgm");
    public static final DeferredHolder<SoundEvent, SoundEvent> MANBA_DUEL_CONTROL_1 = reg("manba_duel_control_1");
    public static final DeferredHolder<SoundEvent, SoundEvent> MANBA_DUEL_CONTROL_2 = reg("manba_duel_control_2");
    public static final DeferredHolder<SoundEvent, SoundEvent> MANBA_DUEL_CONTROL_3 = reg("manba_duel_control_3");
    public static final DeferredHolder<SoundEvent, SoundEvent> MANBA_DUEL_CONTROL_4 = reg("manba_duel_control_4");
    public static final DeferredHolder<SoundEvent, SoundEvent> MANBA_DUEL_REVIVE = reg("manba_duel_revive");
    public static final DeferredHolder<SoundEvent, SoundEvent> MANBA_DUEL_EXECUTE = reg("manba_duel_execute");

    public static final DeferredHolder<SoundEvent, SoundEvent> STINGER_SMOKE_EQUIP = reg("stinger_smoke_equip");
    public static final DeferredHolder<SoundEvent, SoundEvent> STINGER_SMOKE_THROW = reg("stinger_smoke_throw");
    public static final DeferredHolder<SoundEvent, SoundEvent> STINGER_SMOKE_BURST = reg("stinger_smoke_burst");
    public static final DeferredHolder<SoundEvent, SoundEvent> STINGER_DRONE_EQUIP = reg("stinger_drone_equip");
    public static final DeferredHolder<SoundEvent, SoundEvent> STINGER_DRONE_LAUNCH = reg("stinger_drone_launch");
    public static final DeferredHolder<SoundEvent, SoundEvent> STINGER_DRONE_FLY = reg("stinger_drone_fly");
    public static final DeferredHolder<SoundEvent, SoundEvent> STINGER_STIM_EQUIP = reg("stinger_stim_equip");
    public static final DeferredHolder<SoundEvent, SoundEvent> STINGER_STIM_MODE = reg("stinger_stim_mode");
    public static final DeferredHolder<SoundEvent, SoundEvent> STINGER_STIM_FIRE = reg("stinger_stim_fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> STINGER_STIM_HIT = reg("stinger_stim_hit");

    // Planned sound hooks reserved from characters_records/需要完成的角色.
    public static final DeferredHolder<SoundEvent, SoundEvent> HACKCLAW_FLASH_DRONE_EQUIP = reg("hackclaw_flash_drone_equip");
    public static final DeferredHolder<SoundEvent, SoundEvent> HACKCLAW_FLASH_DRONE_CHARGE = reg("hackclaw_flash_drone_charge");
    public static final DeferredHolder<SoundEvent, SoundEvent> HACKCLAW_FLASH_DRONE_PATH_LOCK = reg("hackclaw_flash_drone_path_lock");
    public static final DeferredHolder<SoundEvent, SoundEvent> HACKCLAW_FLASH_DRONE_WARNING = reg("hackclaw_flash_drone_warning");
    public static final DeferredHolder<SoundEvent, SoundEvent> HACKCLAW_ADVANCED_HACK_CHANNEL_START = reg("hackclaw_advanced_hack_channel_start");
    public static final DeferredHolder<SoundEvent, SoundEvent> HACKCLAW_ADVANCED_HACK_SCAN_START = reg("hackclaw_advanced_hack_scan_start");
    public static final DeferredHolder<SoundEvent, SoundEvent> HACKCLAW_ADVANCED_HACK_SCAN_END = reg("hackclaw_advanced_hack_scan_end");
    public static final DeferredHolder<SoundEvent, SoundEvent> HACKCLAW_ADVANCED_HACK_TARGET_FOUND = reg("hackclaw_advanced_hack_target_found");
    public static final DeferredHolder<SoundEvent, SoundEvent> HACKCLAW_ADVANCED_HACK_EQUIPMENT_REPORT = reg("hackclaw_advanced_hack_equipment_report");
    public static final DeferredHolder<SoundEvent, SoundEvent> MORSE_ALERT_HEARING_PING = reg("morse_alert_hearing_ping");
    public static final DeferredHolder<SoundEvent, SoundEvent> MORSE_SOUND_ENHANCE = reg("morse_sound_enhance");
    public static final DeferredHolder<SoundEvent, SoundEvent> MORSE_SHOCK_ORB_EQUIP = reg("morse_shock_orb_equip");
    public static final DeferredHolder<SoundEvent, SoundEvent> MORSE_SHOCK_ORB_PREVIEW = reg("morse_shock_orb_preview");
    public static final DeferredHolder<SoundEvent, SoundEvent> MORSE_SHOCK_ORB_THROW = reg("morse_shock_orb_throw");
    public static final DeferredHolder<SoundEvent, SoundEvent> MORSE_SHOCK_ORB_BURST = reg("morse_shock_orb_burst");
    public static final DeferredHolder<SoundEvent, SoundEvent> MORSE_STRONG_SHOCK_HIT = reg("morse_strong_shock_hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> MORSE_DEAFENED = reg("morse_deafened");
    public static final DeferredHolder<SoundEvent, SoundEvent> MORSE_AI_DISABLED = reg("morse_ai_disabled");
    public static final DeferredHolder<SoundEvent, SoundEvent> MORSE_FLASH_EQUIP = reg("morse_flash_equip");
    public static final DeferredHolder<SoundEvent, SoundEvent> MORSE_FLASH_THROW = reg("morse_flash_throw");
    public static final DeferredHolder<SoundEvent, SoundEvent> MORSE_FLASH_BOUNCE = reg("morse_flash_bounce");
    public static final DeferredHolder<SoundEvent, SoundEvent> MORSE_FLASH_EXPLODE = reg("morse_flash_explode");
    public static final DeferredHolder<SoundEvent, SoundEvent> MORSE_FLASH_HIT = reg("morse_flash_hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> MORSE_TINNITUS = reg("morse_tinnitus");
    public static final DeferredHolder<SoundEvent, SoundEvent> MORSE_SONAR_EQUIP = reg("morse_sonar_equip");
    public static final DeferredHolder<SoundEvent, SoundEvent> MORSE_SONAR_DEPLOYING = reg("morse_sonar_deploying");
    public static final DeferredHolder<SoundEvent, SoundEvent> MORSE_SONAR_DEPLOY_COMPLETE = reg("morse_sonar_deploy_complete");
    public static final DeferredHolder<SoundEvent, SoundEvent> MORSE_SONAR_SCAN_START = reg("morse_sonar_scan_start");
    public static final DeferredHolder<SoundEvent, SoundEvent> MORSE_SONAR_IDLE_TICK = reg("morse_sonar_idle_tick");
    public static final DeferredHolder<SoundEvent, SoundEvent> MORSE_SONAR_TARGET_FOUND_VOICE = reg("morse_sonar_target_found_voice");
    public static final DeferredHolder<SoundEvent, SoundEvent> MORSE_SONAR_DESTROYED = reg("morse_sonar_destroyed");
    public static final DeferredHolder<SoundEvent, SoundEvent> MORSE_SONAR_ACTION_REVEAL = reg("morse_sonar_action_reveal");
    public static final DeferredHolder<SoundEvent, SoundEvent> RAPTOR_FOOTPRINT_SPAWN = reg("raptor_footprint_spawn");
    public static final DeferredHolder<SoundEvent, SoundEvent> RAPTOR_FOOTPRINT_FADE = reg("raptor_footprint_fade");
    public static final DeferredHolder<SoundEvent, SoundEvent> RAPTOR_FOOTPRINT_READ_START = reg("raptor_footprint_read_start");
    public static final DeferredHolder<SoundEvent, SoundEvent> RAPTOR_FOOTPRINT_INFO = reg("raptor_footprint_info");
    public static final DeferredHolder<SoundEvent, SoundEvent> RAPTOR_FALCON_EQUIP = reg("raptor_falcon_equip");
    public static final DeferredHolder<SoundEvent, SoundEvent> RAPTOR_FALCON_STOW = reg("raptor_falcon_stow");
    public static final DeferredHolder<SoundEvent, SoundEvent> RAPTOR_FALCON_LAUNCH = reg("raptor_falcon_launch");
    public static final DeferredHolder<SoundEvent, SoundEvent> RAPTOR_FALCON_FLY = reg("raptor_falcon_fly");
    public static final DeferredHolder<SoundEvent, SoundEvent> RAPTOR_FALCON_TARGET_FOUND = reg("raptor_falcon_target_found");
    public static final DeferredHolder<SoundEvent, SoundEvent> RAPTOR_FALCON_POSITION_REVEAL = regFixed("raptor_falcon_position_reveal", 32.0F);
    public static final DeferredHolder<SoundEvent, SoundEvent> RAPTOR_FALCON_PULSE_THROW = reg("raptor_falcon_pulse_throw");
    public static final DeferredHolder<SoundEvent, SoundEvent> RAPTOR_FALCON_SELF_DESTRUCT_START = reg("raptor_falcon_self_destruct_start");
    public static final DeferredHolder<SoundEvent, SoundEvent> RAPTOR_FALCON_SELF_DESTRUCT_EXPLODE = reg("raptor_falcon_self_destruct_explode");
    public static final DeferredHolder<SoundEvent, SoundEvent> RAPTOR_PULSE_GRENADE_EQUIP = reg("raptor_pulse_grenade_equip");
    public static final DeferredHolder<SoundEvent, SoundEvent> RAPTOR_PULSE_GRENADE_STOW = reg("raptor_pulse_grenade_stow");
    public static final DeferredHolder<SoundEvent, SoundEvent> RAPTOR_PULSE_GRENADE_PIN = reg("raptor_pulse_grenade_pin");
    public static final DeferredHolder<SoundEvent, SoundEvent> RAPTOR_PULSE_GRENADE_THROW = reg("raptor_pulse_grenade_throw");
    public static final DeferredHolder<SoundEvent, SoundEvent> RAPTOR_PULSE_GRENADE_BOUNCE = reg("raptor_pulse_grenade_bounce");
    public static final DeferredHolder<SoundEvent, SoundEvent> RAPTOR_PULSE_GRENADE_COUNTDOWN = reg("raptor_pulse_grenade_countdown");
    public static final DeferredHolder<SoundEvent, SoundEvent> RAPTOR_PULSE_GRENADE_EXPLODE = reg("raptor_pulse_grenade_explode");
    public static final DeferredHolder<SoundEvent, SoundEvent> RAPTOR_PULSE_STAGGER_HIT = reg("raptor_pulse_stagger_hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> RAPTOR_HUMMINGBIRD_TARGET_AVAILABLE = reg("raptor_hummingbird_target_available");
    public static final DeferredHolder<SoundEvent, SoundEvent> RAPTOR_HUMMINGBIRD_ATTACH = reg("raptor_hummingbird_attach");
    public static final DeferredHolder<SoundEvent, SoundEvent> RAPTOR_HUMMINGBIRD_SUCCESS = reg("raptor_hummingbird_success");
    public static final DeferredHolder<SoundEvent, SoundEvent> RAPTOR_HUMMINGBIRD_ACTIVE_LOOP = reg("raptor_hummingbird_active_loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> RAPTOR_HUMMINGBIRD_REVEAL = reg("raptor_hummingbird_reveal");
    public static final DeferredHolder<SoundEvent, SoundEvent> RAPTOR_HUMMINGBIRD_VIEW_ENTER = reg("raptor_hummingbird_view_enter");
    public static final DeferredHolder<SoundEvent, SoundEvent> RAPTOR_HUMMINGBIRD_VIEW_EXIT = reg("raptor_hummingbird_view_exit");
    public static final DeferredHolder<SoundEvent, SoundEvent> RAPTOR_HUMMINGBIRD_LOST = reg("raptor_hummingbird_lost");
    public static final DeferredHolder<SoundEvent, SoundEvent> RAPTOR_HUMMINGBIRD_END = reg("raptor_hummingbird_end");
    public static final DeferredHolder<SoundEvent, SoundEvent> RAPTOR_HUMMINGBIRD_DESTROYED = reg("raptor_hummingbird_destroyed");
    public static final DeferredHolder<SoundEvent, SoundEvent> TEMPEST_EXPLOSIVE_SPINE_ACTIVATE = reg("tempest_explosive_spine_activate");
    public static final DeferredHolder<SoundEvent, SoundEvent> TEMPEST_DANGER_NEAR_MISS = reg("tempest_danger_near_miss");
    public static final DeferredHolder<SoundEvent, SoundEvent> TEMPEST_EXPLOSION_TRIGGER = reg("tempest_explosion_trigger");
    public static final DeferredHolder<SoundEvent, SoundEvent> TEMPEST_SPEED_REFRESH = reg("tempest_speed_refresh");
    public static final DeferredHolder<SoundEvent, SoundEvent> TEMPEST_TACTICAL_ROLL_START = reg("tempest_tactical_roll_start");
    public static final DeferredHolder<SoundEvent, SoundEvent> TEMPEST_TACTICAL_ROLL_KILL_RESET = reg("tempest_tactical_roll_kill_reset");
    public static final DeferredHolder<SoundEvent, SoundEvent> TEMPEST_WALL_DRILL_EQUIP = reg("tempest_wall_drill_equip");
    public static final DeferredHolder<SoundEvent, SoundEvent> TEMPEST_WALL_DRILL_THROW = reg("tempest_wall_drill_throw");
    public static final DeferredHolder<SoundEvent, SoundEvent> TEMPEST_WALL_DRILL_ATTACH = reg("tempest_wall_drill_attach");
    public static final DeferredHolder<SoundEvent, SoundEvent> TEMPEST_WALL_DRILL_CHARGE_START = reg("tempest_wall_drill_charge_start");
    public static final DeferredHolder<SoundEvent, SoundEvent> TEMPEST_WALL_DRILL_CHARGE_TICK = reg("tempest_wall_drill_charge_tick");
    public static final DeferredHolder<SoundEvent, SoundEvent> TEMPEST_WALL_DRILL_RELEASE = reg("tempest_wall_drill_release");
    public static final DeferredHolder<SoundEvent, SoundEvent> TEMPEST_WALL_DRILL_DISARM_HIT = reg("tempest_wall_drill_disarm_hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> TEMPEST_WALL_DRILL_DESTROYED = reg("tempest_wall_drill_destroyed");
    public static final DeferredHolder<SoundEvent, SoundEvent> TEMPEST_RECALL_ANCHOR_PLACE = reg("tempest_recall_anchor_place");
    public static final DeferredHolder<SoundEvent, SoundEvent> TEMPEST_RECALL_ROPE_WARNING = reg("tempest_recall_rope_warning");
    public static final DeferredHolder<SoundEvent, SoundEvent> TEMPEST_RECALL_ACTIVE = reg("tempest_recall_active");
    public static final DeferredHolder<SoundEvent, SoundEvent> TEMPEST_RECALL_LETHAL_TRIGGER = reg("tempest_recall_lethal_trigger");
    public static final DeferredHolder<SoundEvent, SoundEvent> TEMPEST_RECALL_PULL_LOOP = reg("tempest_recall_pull_loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> TEMPEST_RECALL_DOWNED = reg("tempest_recall_downed");
    public static final DeferredHolder<SoundEvent, SoundEvent> TEMPEST_RECALL_SELF_RESCUE_START = reg("tempest_recall_self_rescue_start");
    public static final DeferredHolder<SoundEvent, SoundEvent> TEMPEST_RECALL_SELF_RESCUE_COMPLETE = reg("tempest_recall_self_rescue_complete");
    public static final DeferredHolder<SoundEvent, SoundEvent> TEMPEST_RECALL_ROPE_SUSPEND = reg("tempest_recall_rope_suspend");
    public static final DeferredHolder<SoundEvent, SoundEvent> TEMPEST_RECALL_END = reg("tempest_recall_end");
    public static final DeferredHolder<SoundEvent, SoundEvent> TOXIK_ADRENALINE_RELEASE = reg("toxik_adrenaline_release");
    public static final DeferredHolder<SoundEvent, SoundEvent> TOXIK_ADRENALINE_END = reg("toxik_adrenaline_end");
    public static final DeferredHolder<SoundEvent, SoundEvent> TOXIK_TEAR_GAS_EQUIP = reg("toxik_tear_gas_equip");
    public static final DeferredHolder<SoundEvent, SoundEvent> TOXIK_TEAR_GAS_THROW = reg("toxik_tear_gas_throw");
    public static final DeferredHolder<SoundEvent, SoundEvent> TOXIK_TEAR_GAS_BURST = reg("toxik_tear_gas_burst");
    public static final DeferredHolder<SoundEvent, SoundEvent> TOXIK_FIREFLY_EQUIP = reg("toxik_firefly_equip");
    public static final DeferredHolder<SoundEvent, SoundEvent> TOXIK_FIREFLY_STOW = reg("toxik_firefly_stow");
    public static final DeferredHolder<SoundEvent, SoundEvent> TOXIK_FIREFLY_MODE_SWITCH = reg("toxik_firefly_mode_switch");
    public static final DeferredHolder<SoundEvent, SoundEvent> TOXIK_FIREFLY_RELEASE = reg("toxik_firefly_release");
    public static final DeferredHolder<SoundEvent, SoundEvent> TOXIK_FIREFLY_HIT = reg("toxik_firefly_hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> TOXIK_FIREFLY_PULLOUT_START = reg("toxik_firefly_pullout_start");
    public static final DeferredHolder<SoundEvent, SoundEvent> TOXIK_FIREFLY_PULLOUT_COMPLETE = reg("toxik_firefly_pullout_complete");
    public static final DeferredHolder<SoundEvent, SoundEvent> VLINDER_DOWNED_TRIGGER = reg("vlinder_downed_trigger");
    public static final DeferredHolder<SoundEvent, SoundEvent> VLINDER_DOWNED_MARKER = reg("vlinder_downed_marker");
    public static final DeferredHolder<SoundEvent, SoundEvent> VLINDER_RESCUE_START = reg("vlinder_rescue_start");
    public static final DeferredHolder<SoundEvent, SoundEvent> VLINDER_RESCUE_COMPLETE = reg("vlinder_rescue_complete");
    public static final DeferredHolder<SoundEvent, SoundEvent> VLINDER_SELF_RESCUE_START = reg("vlinder_self_rescue_start");
    public static final DeferredHolder<SoundEvent, SoundEvent> VLINDER_SELF_RESCUE_COMPLETE = reg("vlinder_self_rescue_complete");
    public static final DeferredHolder<SoundEvent, SoundEvent> VLINDER_MEDICAL_DRONE_EQUIP = reg("vlinder_medical_drone_equip");
    public static final DeferredHolder<SoundEvent, SoundEvent> VLINDER_MEDICAL_DRONE_STOW = reg("vlinder_medical_drone_stow");
    public static final DeferredHolder<SoundEvent, SoundEvent> VLINDER_MEDICAL_DRONE_LOCK = reg("vlinder_medical_drone_lock");
    public static final DeferredHolder<SoundEvent, SoundEvent> VLINDER_MEDICAL_DRONE_MODE_SWITCH = reg("vlinder_medical_drone_mode_switch");
    public static final DeferredHolder<SoundEvent, SoundEvent> VLINDER_MEDICAL_DRONE_LAUNCH = reg("vlinder_medical_drone_launch");
    public static final DeferredHolder<SoundEvent, SoundEvent> VLINDER_MEDICAL_DRONE_HIT = reg("vlinder_medical_drone_hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> VLINDER_HEALING_DUST_APPLY = reg("vlinder_healing_dust_apply");
    public static final DeferredHolder<SoundEvent, SoundEvent> VLINDER_MEDICAL_WASTE_APPLY = reg("vlinder_medical_waste_apply");
    public static final DeferredHolder<SoundEvent, SoundEvent> VLINDER_MEDICAL_DRONE_DESTROYED = reg("vlinder_medical_drone_destroyed");
    public static final DeferredHolder<SoundEvent, SoundEvent> VLINDER_REMOTE_SMOKE_THROW = reg("vlinder_remote_smoke_throw");
    public static final DeferredHolder<SoundEvent, SoundEvent> VLINDER_REMOTE_SMOKE_BURST = reg("vlinder_remote_smoke_burst");
    public static final DeferredHolder<SoundEvent, SoundEvent> VLINDER_ACTIVE_DEFENSE_SUMMON = reg("vlinder_active_defense_summon");
    public static final DeferredHolder<SoundEvent, SoundEvent> VLINDER_ABSORPTION_REFRESH = reg("vlinder_absorption_refresh");
    public static final DeferredHolder<SoundEvent, SoundEvent> VLINDER_DOWNED_PLAYER_FOUND = reg("vlinder_downed_player_found");
    public static final DeferredHolder<SoundEvent, SoundEvent> VLINDER_PLASMA_INJECTION_START = reg("vlinder_plasma_injection_start");
    public static final DeferredHolder<SoundEvent, SoundEvent> VLINDER_PLASMA_INJECTION_COMPLETE = reg("vlinder_plasma_injection_complete");
    public static final DeferredHolder<SoundEvent, SoundEvent> VLINDER_QUICK_RESCUE = reg("vlinder_quick_rescue");
    public static final DeferredHolder<SoundEvent, SoundEvent> VLINDER_ACTIVE_DEFENSE_DESTROYED = reg("vlinder_active_defense_destroyed");
    public static final DeferredHolder<SoundEvent, SoundEvent> NIKAIDOU_RIFT_REFRESH = reg("nikaidou_rift_refresh");
    public static final DeferredHolder<SoundEvent, SoundEvent> NIKAIDOU_CORRECTION_START = reg("nikaidou_correction_start");
    public static final DeferredHolder<SoundEvent, SoundEvent> NIKAIDOU_CORRECTION_REFRESH = reg("nikaidou_correction_refresh");
    public static final DeferredHolder<SoundEvent, SoundEvent> NIKAIDOU_HOT_IRON_TOGGLE = reg("nikaidou_hot_iron_toggle");
    public static final DeferredHolder<SoundEvent, SoundEvent> NIKAIDOU_WEAPON_SWING = reg("nikaidou_weapon_swing");
    public static final DeferredHolder<SoundEvent, SoundEvent> NIKAIDOU_HOT_IRON_HIT = reg("nikaidou_hot_iron_hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> NIKAIDOU_CORE_START = reg("nikaidou_core_start");
    public static final DeferredHolder<SoundEvent, SoundEvent> NIKAIDOU_RITUAL_SWORD_HIT = reg("nikaidou_ritual_sword_hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> NIKAIDOU_DOOM_TRIGGER = reg("nikaidou_doom_trigger");
    public static final DeferredHolder<SoundEvent, SoundEvent> NIKAIDOU_DOOM_DEATH = reg("nikaidou_doom_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> NIKAIDOU_WITCH_REWIND_REBIRTH_1 = reg("nikaidou_witch_rewind_rebirth_1");
    public static final DeferredHolder<SoundEvent, SoundEvent> NIKAIDOU_WITCH_REWIND_REBIRTH_2 = reg("nikaidou_witch_rewind_rebirth_2");
    public static final DeferredHolder<SoundEvent, SoundEvent> NIKAIDOU_WITCH_REWIND_REBIRTH_3 = reg("nikaidou_witch_rewind_rebirth_3");
    public static final DeferredHolder<SoundEvent, SoundEvent> NIKAIDOU_WITCH_ERROR_CAST = reg("nikaidou_witch_error_cast");
    public static final DeferredHolder<SoundEvent, SoundEvent> NIKAIDOU_WITCH_KILL_1 = reg("nikaidou_witch_kill_1");
    public static final DeferredHolder<SoundEvent, SoundEvent> NIKAIDOU_WITCH_KILL_2 = reg("nikaidou_witch_kill_2");
    public static final DeferredHolder<SoundEvent, SoundEvent> NIKAIDOU_WITCH_CORE_CAST = reg("nikaidou_witch_core_cast");
    public static final DeferredHolder<SoundEvent, SoundEvent> NIKAIDOU_WITCH_REMNANT_LOCK = reg("nikaidou_witch_remnant_lock");
    public static final DeferredHolder<SoundEvent, SoundEvent> NIKAIDOU_WITCH_REMNANT_ENTER = reg("nikaidou_witch_remnant_enter");
    public static final DeferredHolder<SoundEvent, SoundEvent> NIKAIDOU_WITCH_FINAL_ENTER = reg("nikaidou_witch_final_enter");
    public static final DeferredHolder<SoundEvent, SoundEvent> CATDAD_REFLECT = reg("catdad_reflect");
    public static final DeferredHolder<SoundEvent, SoundEvent> CATDAD_HISS = reg("catdad_hiss");
    public static final DeferredHolder<SoundEvent, SoundEvent> CATDAD_POWER_STRIKE = reg("catdad_power_strike");
    public static final DeferredHolder<SoundEvent, SoundEvent> CATDAD_BLOCK_START = reg("catdad_block_start");
    public static final DeferredHolder<SoundEvent, SoundEvent> CATDAD_BLOCK_SUCCESS = reg("catdad_block_success");
    public static final DeferredHolder<SoundEvent, SoundEvent> CATDAD_ROAD_START = reg("catdad_road_start");
    public static final DeferredHolder<SoundEvent, SoundEvent> CATDAD_ROAD_WARNING = reg("catdad_road_warning");
    public static final DeferredHolder<SoundEvent, SoundEvent> CATDAD_TRUCK_HIT = reg("catdad_truck_hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> CATDAD_TRUCK_SELF_DOWNED = reg("catdad_truck_self_downed");
    public static final DeferredHolder<SoundEvent, SoundEvent> CATDAD_SELF_RESCUE_START = reg("catdad_self_rescue_start");
    public static final DeferredHolder<SoundEvent, SoundEvent> CATDAD_SELF_RESCUE_COMPLETE = reg("catdad_self_rescue_complete");
    public static final DeferredHolder<SoundEvent, SoundEvent> DEPARTMENT_LASER_SHOOT = reg("department_laser_shoot");
    public static final DeferredHolder<SoundEvent, SoundEvent> DEPARTMENT_LASER_FLY = reg("department_laser_fly");
    public static final DeferredHolder<SoundEvent, SoundEvent> DEPARTMENT_LASER_HIT = reg("department_laser_hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> DEPARTMENT_TRAP_EQUIP = reg("department_trap_equip");
    public static final DeferredHolder<SoundEvent, SoundEvent> DEPARTMENT_TRAP_DEPLOY = reg("department_trap_deploy");
    public static final DeferredHolder<SoundEvent, SoundEvent> DEPARTMENT_TRAP_READY = reg("department_trap_ready");
    public static final DeferredHolder<SoundEvent, SoundEvent> DEPARTMENT_TRAP_TRIGGER = reg("department_trap_trigger");
    public static final DeferredHolder<SoundEvent, SoundEvent> DEPARTMENT_TRAP_EXPLODE = reg("department_trap_explode");
    public static final DeferredHolder<SoundEvent, SoundEvent> DEPARTMENT_CHARGED_CREEPER_EJECT = reg("department_charged_creeper_eject");
    public static final DeferredHolder<SoundEvent, SoundEvent> DEPARTMENT_BABY_ZOMBIE_EJECT = reg("department_baby_zombie_eject");
    public static final DeferredHolder<SoundEvent, SoundEvent> DEPARTMENT_CORE_CAST = reg("department_core_cast");
    public static final DeferredHolder<SoundEvent, SoundEvent> DEPARTMENT_CORE_COUNTDOWN = reg("department_core_countdown");
    public static final DeferredHolder<SoundEvent, SoundEvent> DEPARTMENT_CORE_RELEASE = reg("department_core_release");
    public static final DeferredHolder<SoundEvent, SoundEvent> DEPARTMENT_PASSIVE_FIRST_FATAL = reg("department_passive_first_fatal");
    public static final DeferredHolder<SoundEvent, SoundEvent> DEPARTMENT_PASSIVE_SECOND_FATAL = reg("department_passive_second_fatal");
    public static final DeferredHolder<SoundEvent, SoundEvent> SAEED_ROLL_START = reg("saeed_roll_start");
    public static final DeferredHolder<SoundEvent, SoundEvent> SAEED_ROLL_END = reg("saeed_roll_end");
    public static final DeferredHolder<SoundEvent, SoundEvent> SAEED_CROSSBOW_DRAW = reg("saeed_crossbow_draw");
    public static final DeferredHolder<SoundEvent, SoundEvent> SAEED_CROSSBOW_RELOAD = reg("saeed_crossbow_reload");
    public static final DeferredHolder<SoundEvent, SoundEvent> SAEED_CROSSBOW_FIRE = reg("saeed_crossbow_fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> SAEED_FIRE_ARROW_EXPLODE = reg("saeed_fire_arrow_explode");
    public static final DeferredHolder<SoundEvent, SoundEvent> SAEED_FIRE_ARROW_HIT = reg("saeed_fire_arrow_hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> SAEED_COMMAND_CALL = reg("saeed_command_call");
    public static final DeferredHolder<SoundEvent, SoundEvent> SAEED_COMMAND_VOICE = reg("saeed_command_voice");
    public static final DeferredHolder<SoundEvent, SoundEvent> SAEED_COMMAND_END = reg("saeed_command_end");
    public static final DeferredHolder<SoundEvent, SoundEvent> SAEED_PRESTIGE_UP = reg("saeed_prestige_up");
    public static final DeferredHolder<SoundEvent, SoundEvent> SAEED_GUARD_WITHDRAW = reg("saeed_guard_withdraw");
    public static final DeferredHolder<SoundEvent, SoundEvent> SAEED_GUARD_SKILL = reg("saeed_guard_skill");
    public static final DeferredHolder<SoundEvent, SoundEvent> SAEED_THUNDER_ENTER = reg("saeed_thunder_enter");
    public static final DeferredHolder<SoundEvent, SoundEvent> SAEED_IRON_RAIN_ENTER = reg("saeed_iron_rain_enter");
    public static final DeferredHolder<SoundEvent, SoundEvent> SAEED_FIREEYE_ENTER = reg("saeed_fireeye_enter");
    public static final DeferredHolder<SoundEvent, SoundEvent> SAEED_SHARP_EAGLE_ENTER = reg("saeed_sharp_eagle_enter");
    public static final DeferredHolder<SoundEvent, SoundEvent> SAEED_HAKIM_ENTER = reg("saeed_hakim_enter");
    public static final DeferredHolder<SoundEvent, SoundEvent> SAEED_KARIM_ENTER = reg("saeed_karim_enter");
    public static final DeferredHolder<SoundEvent, SoundEvent> SAEED_IRON_RAIN_SKILL = reg("saeed_iron_rain_skill");
    public static final DeferredHolder<SoundEvent, SoundEvent> SAEED_FIREEYE_SKILL = reg("saeed_fireeye_skill");
    public static final DeferredHolder<SoundEvent, SoundEvent> SAEED_SHARP_EAGLE_SKILL = reg("saeed_sharp_eagle_skill");
    public static final DeferredHolder<SoundEvent, SoundEvent> SAEED_HAKIM_SKILL = reg("saeed_hakim_skill");
    public static final DeferredHolder<SoundEvent, SoundEvent> SAEED_KARIM_SKILL = reg("saeed_karim_skill");
    public static final DeferredHolder<SoundEvent, SoundEvent> SAEED_FIREEYE_ATTACK = reg("saeed_fireeye_attack");
    public static final DeferredHolder<SoundEvent, SoundEvent> SAEED_KARIM_HIT = reg("saeed_karim_hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> SAEED_HAKIM_HIT = reg("saeed_hakim_hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> SAEED_KARIM_KILL = reg("saeed_karim_kill");
    public static final DeferredHolder<SoundEvent, SoundEvent> SAEED_HAKIM_KILL = reg("saeed_hakim_kill");
    public static final DeferredHolder<SoundEvent, SoundEvent> SAEED_FIREEYE_DETONATE = reg("saeed_fireeye_detonate");
    public static final DeferredHolder<SoundEvent, SoundEvent> GHROTH_STARS = reg("ghroth_stars");
    public static final DeferredHolder<SoundEvent, SoundEvent> GHROTH_JUSTICE = reg("ghroth_justice");
    public static final DeferredHolder<SoundEvent, SoundEvent> GHROTH_NOON = reg("ghroth_noon");
    public static final DeferredHolder<SoundEvent, SoundEvent> GHROTH_PURCHASE_SUCCESS_1 = reg("ghroth_purchase_success_1");
    public static final DeferredHolder<SoundEvent, SoundEvent> GHROTH_PURCHASE_SUCCESS_2 = reg("ghroth_purchase_success_2");
    public static final DeferredHolder<SoundEvent, SoundEvent> GHROTH_PURCHASE_SUCCESS_3 = reg("ghroth_purchase_success_3");
    public static final DeferredHolder<SoundEvent, SoundEvent> GHROTH_PURCHASE_SUCCESS_4 = reg("ghroth_purchase_success_4");
    public static final DeferredHolder<SoundEvent, SoundEvent> GHROTH_PURCHASE_SUCCESS_5 = reg("ghroth_purchase_success_5");
    public static final DeferredHolder<SoundEvent, SoundEvent> GHROTH_PURCHASE_SUCCESS_6 = reg("ghroth_purchase_success_6");
    public static final DeferredHolder<SoundEvent, SoundEvent> GHROTH_PURCHASE_SUCCESS_7 = reg("ghroth_purchase_success_7");

    public static final DeferredHolder<SoundEvent, SoundEvent> CHAMBER_PASSIVE_FUNDS = reg("chamber_passive_funds");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHAMBER_TELEPORT_CARD = reg("chamber_teleport_card");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHAMBER_TRAP_CARD = reg("chamber_trap_card");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHAMBER_HEADHUNTER = reg("chamber_headhunter");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHAMBER_TOUR_DE_FORCE = reg("chamber_tour_de_force");
    public static final DeferredHolder<SoundEvent, SoundEvent> N_TWO_TRACKING_STUN = reg("n_two_tracking_stun");
    public static final DeferredHolder<SoundEvent, SoundEvent> N_TWO_TRACKING_STUN_THROW = reg("n_two_tracking_stun_throw");
    public static final DeferredHolder<SoundEvent, SoundEvent> N_TWO_TRACKING_STUN_BURST = reg("n_two_tracking_stun_burst");
    public static final DeferredHolder<SoundEvent, SoundEvent> N_TWO_TRACKING_STUN_LOCK = reg("n_two_tracking_stun_lock");
    public static final DeferredHolder<SoundEvent, SoundEvent> N_TWO_DEWAR_CANISTER = reg("n_two_dewar_canister");
    public static final DeferredHolder<SoundEvent, SoundEvent> N_TWO_DEWAR_STOW = reg("n_two_dewar_stow");
    public static final DeferredHolder<SoundEvent, SoundEvent> N_TWO_DEWAR_PIN = reg("n_two_dewar_pin");
    public static final DeferredHolder<SoundEvent, SoundEvent> N_TWO_DEWAR_THROW = reg("n_two_dewar_throw");
    public static final DeferredHolder<SoundEvent, SoundEvent> N_TWO_DEWAR_FIELD = reg("n_two_dewar_field");
    public static final DeferredHolder<SoundEvent, SoundEvent> N_TWO_ICE_FIELD = reg("n_two_ice_field");
    public static final DeferredHolder<SoundEvent, SoundEvent> N_TWO_CONDENSED_LAUNCHER = reg("n_two_condensed_launcher");
    public static final DeferredHolder<SoundEvent, SoundEvent> N_TWO_CONDENSED_CHARGE = reg("n_two_condensed_charge");
    public static final DeferredHolder<SoundEvent, SoundEvent> N_TWO_CONDENSED_STOW = reg("n_two_condensed_stow");
    public static final DeferredHolder<SoundEvent, SoundEvent> N_TWO_CONDENSED_FIRE = reg("n_two_condensed_fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> N_TWO_CONDENSED_MECHANICAL = reg("n_two_condensed_mechanical");
    public static final DeferredHolder<SoundEvent, SoundEvent> N_TWO_CONDENSED_BOUNCE = reg("n_two_condensed_bounce");
    public static final DeferredHolder<SoundEvent, SoundEvent> N_TWO_CONDENSED_COUNTDOWN = reg("n_two_condensed_countdown");
    public static final DeferredHolder<SoundEvent, SoundEvent> N_TWO_CONDENSED_HIT = reg("n_two_condensed_hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> N_TWO_CONDENSED_EXPLODE = reg("n_two_condensed_explode");
    public static final DeferredHolder<SoundEvent, SoundEvent> N_TWO_CONDENSED_FIELD = reg("n_two_condensed_field");
    public static final DeferredHolder<SoundEvent, SoundEvent> N_TWO_FREEZE = reg("n_two_freeze");
    public static final DeferredHolder<SoundEvent, SoundEvent> CORPS_WARM_ENFORCEMENT = reg("corps_warm_enforcement");
    public static final DeferredHolder<SoundEvent, SoundEvent> CORPS_FORCEFUL_BATON = reg("corps_forceful_baton");
    public static final DeferredHolder<SoundEvent, SoundEvent> CORPS_CORE_CAST = reg("corps_core_cast");
    public static final DeferredHolder<SoundEvent, SoundEvent> CORPS_DUEL_MUSIC = reg("corps_duel_music");

    public static final DeferredHolder<SoundEvent, SoundEvent> HVK_CLONE_PROTOTYPE_USE_1 = reg("hvk_clone_prototype_use_1");
    public static final DeferredHolder<SoundEvent, SoundEvent> HVK_CLONE_PROTOTYPE_USE_2 = reg("hvk_clone_prototype_use_2");
    public static final DeferredHolder<SoundEvent, SoundEvent> HVK_CLONE_PROTOTYPE_USE_3 = reg("hvk_clone_prototype_use_3");
    public static final DeferredHolder<SoundEvent, SoundEvent> HVK_CLONE_PROTOTYPE_USE_4 = reg("hvk_clone_prototype_use_4");
    public static final DeferredHolder<SoundEvent, SoundEvent> HVK_CLONE_PROTOTYPE_USE_5 = reg("hvk_clone_prototype_use_5");
    public static final DeferredHolder<SoundEvent, SoundEvent> HVK_ADVANCED_TREASURE_COMPASS_SUCCESS =
            reg("hvk_advanced_treasure_compass_success");
    public static final DeferredHolder<SoundEvent, SoundEvent> ETERNAL_LOVE_BLESSING_RECORD =
            reg("eternal_love_blessing_record");
    public static final DeferredHolder<SoundEvent, SoundEvent> ETERNAL_LOVE_BLESSING_OBTAIN =
            reg("eternal_love_blessing_obtain");
    public static final DeferredHolder<SoundEvent, SoundEvent> INTERDIMENSIONAL_BLOCK_USE_1 = reg("interdimensional_block_use_1");
    public static final DeferredHolder<SoundEvent, SoundEvent> INTERDIMENSIONAL_BLOCK_USE_2 = reg("interdimensional_block_use_2");
    public static final DeferredHolder<SoundEvent, SoundEvent> INTERDIMENSIONAL_BLOCK_USE_3 = reg("interdimensional_block_use_3");
    public static final DeferredHolder<SoundEvent, SoundEvent> INTERDIMENSIONAL_BLOCK_USE_4 = reg("interdimensional_block_use_4");
    public static final DeferredHolder<SoundEvent, SoundEvent> INTERDIMENSIONAL_BLOCK_USE_5 = reg("interdimensional_block_use_5");
    public static final DeferredHolder<SoundEvent, SoundEvent> INTERDIMENSIONAL_BLOCK_USE_6 = reg("interdimensional_block_use_6");
    public static final DeferredHolder<SoundEvent, SoundEvent> INTERDIMENSIONAL_BLOCK_USE_7 = reg("interdimensional_block_use_7");
    public static final DeferredHolder<SoundEvent, SoundEvent> INTERDIMENSIONAL_BLOCK_USE_8 = reg("interdimensional_block_use_8");
    public static final DeferredHolder<SoundEvent, SoundEvent> INTERDIMENSIONAL_BLOCK_USE_9 = reg("interdimensional_block_use_9");
    public static final DeferredHolder<SoundEvent, SoundEvent> INTERDIMENSIONAL_BLOCK_USE_10 = reg("interdimensional_block_use_10");
    public static final DeferredHolder<SoundEvent, SoundEvent> INTERDIMENSIONAL_BLOCK_BURIED = reg("interdimensional_block_buried");

    public static final DeferredHolder<SoundEvent, SoundEvent> ITEM_ARMOR_REPAIR_START = reg("item_armor_repair_start");
    public static final DeferredHolder<SoundEvent, SoundEvent> ITEM_ARMOR_REPAIR_WORK = reg("item_armor_repair_work");
    public static final DeferredHolder<SoundEvent, SoundEvent> ITEM_ARMOR_REPAIR_FINISH = reg("item_armor_repair_finish");
    public static final DeferredHolder<SoundEvent, SoundEvent> ITEM_HELMET_REPAIR_START = reg("item_helmet_repair_start");
    public static final DeferredHolder<SoundEvent, SoundEvent> ITEM_HELMET_REPAIR_WORK = reg("item_helmet_repair_work");
    public static final DeferredHolder<SoundEvent, SoundEvent> ITEM_HELMET_REPAIR_FINISH = reg("item_helmet_repair_finish");
    public static final DeferredHolder<SoundEvent, SoundEvent> ITEM_HEMOSTATIC_START = reg("item_hemostatic_start");
    public static final DeferredHolder<SoundEvent, SoundEvent> ITEM_HEMOSTATIC_FINISH = reg("item_hemostatic_finish");
    public static final DeferredHolder<SoundEvent, SoundEvent> ITEM_PAINKILLER_START = reg("item_painkiller_start");
    public static final DeferredHolder<SoundEvent, SoundEvent> ITEM_PAINKILLER_FINISH = reg("item_painkiller_finish");
    public static final DeferredHolder<SoundEvent, SoundEvent> ITEM_SUSTAINED_RELEASE_PAINKILLER_START = reg("item_sustained_release_painkiller_start");
    public static final DeferredHolder<SoundEvent, SoundEvent> ITEM_SUSTAINED_RELEASE_PAINKILLER_FINISH = reg("item_sustained_release_painkiller_finish");
    public static final DeferredHolder<SoundEvent, SoundEvent> ITEM_SURGICAL_START = reg("item_surgical_start");
    public static final DeferredHolder<SoundEvent, SoundEvent> ITEM_SURGICAL_FINISH = reg("item_surgical_finish");
    public static final DeferredHolder<SoundEvent, SoundEvent> ITEM_INJECTION_START = reg("item_injection_start");
    public static final DeferredHolder<SoundEvent, SoundEvent> ITEM_INJECTION_FINISH = reg("item_injection_finish");
    public static final DeferredHolder<SoundEvent, SoundEvent> ITEM_BEVERAGE_START = reg("item_beverage_start");
    public static final DeferredHolder<SoundEvent, SoundEvent> ITEM_BEVERAGE_FINISH = reg("item_beverage_finish");
    public static final DeferredHolder<SoundEvent, SoundEvent> ITEM_MEDICINE_START = reg("item_medicine_start");
    public static final DeferredHolder<SoundEvent, SoundEvent> ITEM_MEDICINE_WORK = reg("item_medicine_work");
    public static final DeferredHolder<SoundEvent, SoundEvent> ITEM_MEDICINE_FINISH = reg("item_medicine_finish");
    public static final DeferredHolder<SoundEvent, SoundEvent> ITEM_SPECIAL_SUPPLY_START = reg("item_special_supply_start");
    public static final DeferredHolder<SoundEvent, SoundEvent> ITEM_SPECIAL_SUPPLY_FINISH = reg("item_special_supply_finish");
    public static final DeferredHolder<SoundEvent, SoundEvent> ACCELERATED_TROOPS_WARNING_1 = reg("accelerated_troops_warning_1");
    public static final DeferredHolder<SoundEvent, SoundEvent> ACCELERATED_TROOPS_WARNING_2 = reg("accelerated_troops_warning_2");
    public static final DeferredHolder<SoundEvent, SoundEvent> ACCELERATED_TROOPS_WARNING_3_1 = reg("accelerated_troops_warning_3_1");
    public static final DeferredHolder<SoundEvent, SoundEvent> ACCELERATED_TROOPS_WARNING_3_2 = reg("accelerated_troops_warning_3_2");
    public static final DeferredHolder<SoundEvent, SoundEvent> ACCELERATED_TROOPS_COMBAT_1 = reg("accelerated_troops_combat_1");
    public static final DeferredHolder<SoundEvent, SoundEvent> ACCELERATED_TROOPS_COMBAT_2 = reg("accelerated_troops_combat_2");
    public static final DeferredHolder<SoundEvent, SoundEvent> ACCELERATED_TROOPS_COMBAT_3 = reg("accelerated_troops_combat_3");
    public static final DeferredHolder<SoundEvent, SoundEvent> ACCELERATED_TROOPS_COMBAT_4 = reg("accelerated_troops_combat_4");
    public static final DeferredHolder<SoundEvent, SoundEvent> ACCELERATED_TROOPS_BATTLE_MUSIC = reg("accelerated_troops_battle_music");
    public static final DeferredHolder<SoundEvent, SoundEvent> HOOK_AUTHOR_EQUIP = reg("hook_author_equip");
    public static final DeferredHolder<SoundEvent, SoundEvent> HOOK_AUTHOR_UNEQUIP = reg("hook_author_unequip");
    public static final DeferredHolder<SoundEvent, SoundEvent> HOOK_AUTHOR_CHARGE_STEP = reg("hook_author_charge_step");
    public static final DeferredHolder<SoundEvent, SoundEvent> HOOK_AUTHOR_RELEASE = reg("hook_author_release");
    public static final DeferredHolder<SoundEvent, SoundEvent> HOOK_AUTHOR_RELEASE_EXTRA = reg("hook_author_release_extra");
    public static final DeferredHolder<SoundEvent, SoundEvent> HOOK_AUTHOR_HISS = reg("hook_author_hiss");
    public static final DeferredHolder<SoundEvent, SoundEvent> HOOK_AUTHOR_USE = reg("hook_author_use");
    public static final DeferredHolder<SoundEvent, SoundEvent> HOOK_AUTHOR_LOCK = reg("hook_author_lock");
    public static final DeferredHolder<SoundEvent, SoundEvent> HOOK_AUTHOR_EXECUTE = reg("hook_author_execute");
    public static final java.util.List<DeferredHolder<SoundEvent, SoundEvent>> HOOK_AUTHOR_78 = registerIndexed("hook_author_78_", 15);

    public static final DeferredHolder<SoundEvent, SoundEvent> COMMANDER_MARK_1 = reg("commander_mark_1");
    public static final DeferredHolder<SoundEvent, SoundEvent> COMMANDER_MARK_2 = reg("commander_mark_2");
    public static final DeferredHolder<SoundEvent, SoundEvent> COMMANDER_SMOKE_1 = reg("commander_smoke_1");
    public static final DeferredHolder<SoundEvent, SoundEvent> COMMANDER_SMOKE_2 = reg("commander_smoke_2");
    public static final DeferredHolder<SoundEvent, SoundEvent> COMMANDER_LIGHTNING = reg("commander_lightning");
    public static final DeferredHolder<SoundEvent, SoundEvent> COMMANDER_MEDICAL_1 = reg("commander_medical_1");
    public static final DeferredHolder<SoundEvent, SoundEvent> COMMANDER_MEDICAL_2 = reg("commander_medical_2");
    public static final DeferredHolder<SoundEvent, SoundEvent> COMMANDER_QUICK_REVIVE_1 = reg("commander_quick_revive_1");
    public static final DeferredHolder<SoundEvent, SoundEvent> COMMANDER_QUICK_REVIVE_2 = reg("commander_quick_revive_2");
    public static final DeferredHolder<SoundEvent, SoundEvent> COMMANDER_ARTILLERY_1 = reg("commander_artillery_1");
    public static final DeferredHolder<SoundEvent, SoundEvent> COMMANDER_ARTILLERY_2 = reg("commander_artillery_2");
    public static final DeferredHolder<SoundEvent, SoundEvent> COMMANDER_VEHICLE_1 = reg("commander_vehicle_1");
    public static final DeferredHolder<SoundEvent, SoundEvent> COMMANDER_VEHICLE_2 = reg("commander_vehicle_2");
    public static final DeferredHolder<SoundEvent, SoundEvent> COMMANDER_EQUIPMENT_1 = reg("commander_equipment_1");
    public static final DeferredHolder<SoundEvent, SoundEvent> COMMANDER_EQUIPMENT_2 = reg("commander_equipment_2");
    public static final DeferredHolder<SoundEvent, SoundEvent> COMMANDER_DRONE_START = reg("commander_drone_start");
    public static final DeferredHolder<SoundEvent, SoundEvent> COMMANDER_DRONE_LAND = reg("commander_drone_land");
    public static final DeferredHolder<SoundEvent, SoundEvent> COMMANDER_BEACON_1 = reg("commander_beacon_1");
    public static final DeferredHolder<SoundEvent, SoundEvent> COMMANDER_BEACON_2 = reg("commander_beacon_2");
    public static final DeferredHolder<SoundEvent, SoundEvent> COMMANDER_MISSILE_FLY = reg("commander_missile_fly");
    public static final DeferredHolder<SoundEvent, SoundEvent> COMMANDER_MISSILE_1 = reg("commander_missile_1");
    public static final DeferredHolder<SoundEvent, SoundEvent> COMMANDER_MISSILE_2 = reg("commander_missile_2");
    public static final DeferredHolder<SoundEvent, SoundEvent> COMMANDER_EXPLOSION_1 = reg("commander_explosion_1");
    public static final DeferredHolder<SoundEvent, SoundEvent> COMMANDER_EXPLOSION_2 = reg("commander_explosion_2");
    public static final DeferredHolder<SoundEvent, SoundEvent> COMMANDER_EXPLOSION_3 = reg("commander_explosion_3");
    public static final DeferredHolder<SoundEvent, SoundEvent> COMMANDER_EXPLOSION_4 = reg("commander_explosion_4");
    public static final DeferredHolder<SoundEvent, SoundEvent> COMMANDER_ALARM = reg("commander_alarm");
    public static final DeferredHolder<SoundEvent, SoundEvent> COMMANDER_MISSILE_READY = reg("commander_missile_ready");
    public static final DeferredHolder<SoundEvent, SoundEvent> COMMANDER_SMOKE_READY = reg("commander_smoke_ready");
    public static final DeferredHolder<SoundEvent, SoundEvent> COMMANDER_NUCLEAR_RELEASE = reg("commander_nuclear_release");
    public static final DeferredHolder<SoundEvent, SoundEvent> COMMANDER_MARK_KILL_1 = reg("commander_mark_kill_1");
    public static final DeferredHolder<SoundEvent, SoundEvent> COMMANDER_MARK_KILL_2 = reg("commander_mark_kill_2");
    public static final DeferredHolder<SoundEvent, SoundEvent> COMMANDER_MARK_KILL_3 = reg("commander_mark_kill_3");
    public static final DeferredHolder<SoundEvent, SoundEvent> COMMANDER_ARTILLERY_READY = reg("commander_artillery_ready");
    public static final DeferredHolder<SoundEvent, SoundEvent> COMMANDER_VEHICLE_READY = reg("commander_vehicle_ready");
    public static final DeferredHolder<SoundEvent, SoundEvent> COMMANDER_BEACON_READY = reg("commander_beacon_ready");

    private static DeferredHolder<SoundEvent, SoundEvent> reg(String id) {
        return SOUNDS.register(id, () -> SoundEvent.createVariableRangeEvent(
                ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, id)));
    }

    private static DeferredHolder<SoundEvent, SoundEvent> regFixed(String id, float range) {
        return SOUNDS.register(id, () -> SoundEvent.createFixedRangeEvent(
                ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, id), range));
    }

    private static Map<String, DeferredHolder<SoundEvent, SoundEvent>> registerLexNinjiaSounds() {
        Map<String, DeferredHolder<SoundEvent, SoundEvent>> sounds = new LinkedHashMap<>();
        for (LexNinjiaArt art : LexNinjiaArt.values()) {
            sounds.put(art.soundId(), reg(art.soundId()));
        }
        sounds.put("lex_ninjia_ham_echo", reg("lex_ninjia_ham_echo"));
        return Collections.unmodifiableMap(sounds);
    }

    private static java.util.List<DeferredHolder<SoundEvent, SoundEvent>> registerIndexed(String prefix, int count) {
        java.util.List<DeferredHolder<SoundEvent, SoundEvent>> result = new java.util.ArrayList<>();
        for (int i = 1; i <= count; i++) result.add(reg(prefix + i));
        return java.util.List.copyOf(result);
    }

    public static SoundEvent lexNinjiaSound(String id) {
        DeferredHolder<SoundEvent, SoundEvent> sound = LEX_NINJIA_SOUNDS.get(id);
        return sound == null ? null : sound.get();
    }

    private static Map<String, DeferredHolder<SoundEvent, SoundEvent>> registerGamblerPowerSounds() {
        Map<String, DeferredHolder<SoundEvent, SoundEvent>> sounds = new LinkedHashMap<>();
        for (String id : new String[] {
                "ride_the_wind",
                "earthshaking_slam",
                "triple_threat",
                "shrink_ray",
                "vitality_burst",
                "frozen_tundra",
                "inspiration",
                "witch_familiar",
                "missile_barrage",
                "octo_thrower",
                "scary_shaper_1000",
                "precision_strike",
                "solar_burn",
                "unbreakable",
                "devour",
                "throw_potato",
                "orange_peel_shield",
                "power_punch",
                "inflating_mushroom",
                "sheepify",
                "blazing_bark",
                "gene_amplification"
        }) {
            sounds.put(id, reg("gambler.power." + id));
        }
        return Collections.unmodifiableMap(sounds);
    }

    public static SoundEvent gamblerPowerSound(String id) {
        DeferredHolder<SoundEvent, SoundEvent> sound = GAMBLER_POWER_SOUNDS.get(id);
        return sound == null ? null : sound.get();
    }
}
