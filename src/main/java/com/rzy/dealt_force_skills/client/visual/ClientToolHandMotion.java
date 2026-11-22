package com.rzy.dealt_force_skills.client.visual;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.client.DWolfInputHandler;
import com.rzy.dealt_force_skills.client.RoverClient;
import com.rzy.dealt_force_skills.client.WildfireClient;
import com.rzy.dealt_force_skills.client.UluruMissileController;
import com.rzy.dealt_force_skills.client.character.*;
import com.rzy.dealt_force_skills.skill.ToolHandCue;
import com.rzy.dealt_force_skills.skill.HeldToolVisual;
import com.rzy.dealt_force_skills.skill.SkillModelVisual;
import static com.rzy.dealt_force_skills.skill.ToolHandCue.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.util.Mth;
import java.util.Map;
import java.util.WeakHashMap;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT)
public final class ClientToolHandMotion {
    private ClientToolHandMotion() {}
    private record Pulse(ToolHandCue cue, int tick) {}
    private static final Map<Player,Pulse> RELEASES = new WeakHashMap<>();
    private static final Map<Player,Pulse> DRAWS = new WeakHashMap<>();
    private static final ClientToolReleaseAction.Action[] ACTIONS = ClientToolReleaseAction.Action.values();
    record Frame(ToolHandCue cue, float draw, float wind, float release, String clip, float seconds) {
        boolean hidden() { return cue.leavesHand() && release >= .38F; }
    }

    private record Prep(ToolHandCue cue, String key, net.minecraft.resources.ResourceLocation model) {}
    private static Prep prep;

    static void preparing(String key, net.minecraft.resources.ResourceLocation model) {
        ToolHandCue cue = switch (key) {
            case "d_wolf_hand_cannon" -> D_WOLF_HAND_CANNON;
            case "d_wolf_smoke_grenade" -> D_WOLF_SMOKE;
            case "vyron_release" -> model.getPath().equals("vyron_magnetic_bomb") ? VYRON_MAGNETIC_BOMB : VYRON_TIGER_CANNON;
            case "uluru_incendiary" -> ULURU_INCENDIARY;
            case "uluru_cover" -> ULURU_COVER;
            case "uluru_missile" -> ULURU_MISSILE;
            case "sineva_grapple_gun" -> SINEVA_HOOK;
            default -> null;
        };
        if (cue != null) prep = new Prep(cue,key,model);
    }

    public static void acceptRelease(int entityId, ToolHandCue cue) {
        var level = Minecraft.getInstance().level;
        if (level != null && level.getEntity(entityId) instanceof Player player)
            RELEASES.put(player,new Pulse(cue,player.tickCount));
    }

    private static ToolHandCue localHeld() {
        if (DWolfInputHandler.isSmokeHeldForVisual()) return D_WOLF_SMOKE;
        if (ClientDWolfHudState.hasHandCannonEquipped()) return D_WOLF_HAND_CANNON;
        if (com.rzy.dealt_force_skills.client.SinevaInputHandler.isGrappleCharging()) return SINEVA_HOOK;
        if (com.rzy.dealt_force_skills.client.SinevaInputHandler.isBladeWireHeld()) return SINEVA_WIRE;
        if (SaeedPlaceholderVisuals.crossbowClip() != null) return SAEED_CROSSBOW;
        if (ClientTempestHudState.hasEquippedTool()) return TEMPEST_WALL_DRILL_STINGER;
        if (ClientDepartmentHudState.hasEquippedTool()) return DEPARTMENT_EXPLOSIVE_TRAP;
        if (RoverClient.tool() == 1) return ROVER_IRRITANT_SMOKE;
        if (RoverClient.tool() == 2) return ROVER_AEROSOL_INJECTOR;
        if (ClientStingerHudState.hasEquippedTool()) return switch (ClientStingerHudState.equippedTool()) {
            case SMOKE_GRENADE -> STINGER_SMOKE_GRENADE;
            case SMOKE_DRONE -> STINGER_SMOKE_DRONE;
            case STIM_GUN -> STINGER_STIM_GUN;
            default -> null;
        };
        if (ClientToxikHudState.hasEquippedTool()) return switch (ClientToxikHudState.equippedTool()) {
            case TEAR_GAS -> TOXIK_TEAR_GAS;
            case FIREFLY_SWARM -> TOXIK_FIREFLY_SWARM;
            default -> null;
        };
        if (ClientNTwoHudState.hasEquippedTool()) return switch (ClientNTwoHudState.equippedTool()) {
            case DEWAR_CANISTER -> N_TWO_DEWAR_CANISTER;
            case CONDENSER_LAUNCHER -> N_TWO_CONDENSER_LAUNCHER;
            default -> null;
        };
        if (ClientNoxHudState.hasEquippedTool()) return switch (ClientNoxHudState.equippedTool()) {
            case ROTOR -> NOX_ROTOR;
            case FLASH_GRENADE -> NOX_FLASH_GRENADE;
            default -> null;
        };
        if (ClientGizmoHudState.hasEquippedTool()) return switch (ClientGizmoHudState.equippedTool()) {
            case SMOKE_TRAP -> GIZMO_SMOKE_TRAP;
            case SPIDER_NEST -> GIZMO_SPIDER_NEST;
            case T_BOY -> GIZMO_T_BOY;
            default -> null;
        };
        if (ClientUluruHudState.hasEquippedTool()) return switch (ClientUluruHudState.equippedTool()) {
            case INCENDIARY -> ULURU_INCENDIARY;
            case COVER -> ULURU_COVER;
            case MISSILE -> ULURU_MISSILE;
            default -> null;
        };
        if (ClientChamberHudState.hasEquippedTool()) return switch (ClientChamberHudState.equippedTool()) {
            case TELEPORT_CARD -> CHAMBER_TELEPORT_CARD;
            case TRAP_CARD -> CHAMBER_TRAP_CARD;
            default -> null;
        };
        if (ClientMorseHudState.hasEquippedTool()) return switch (ClientMorseHudState.equippedTool()) {
            case SHOCK_ORB -> MORSE_SHOCK_ORB;
            case FLASH_GRENADE -> MORSE_FLASH_GRENADE;
            case SONAR_DETECTOR -> MORSE_SONAR_DETECTOR;
            default -> null;
        };
        if (ClientHackclawHudState.hasEquippedTool()) return switch (ClientHackclawHudState.equippedTool()) {
            case HACKING_KNIFE -> HACKCLAW_HACKING_KNIFE;
            case FLASH_DRONE -> HACKCLAW_FLASH_DRONE;
            default -> null;
        };
        if (ClientLunaHudState.hasEquippedTool()) return switch (ClientLunaHudState.equippedTool()) {
            case SHOCK_BOW -> LUNA_BOW;
            case COMPOSITE_GRENADE -> LUNA_COMPOSITE_GRENADE;
            case RECON_BOW -> LUNA_BOW;
            default -> null;
        };
        if (ClientShepherdHudState.hasEquippedTool()) return switch (ClientShepherdHudState.equippedTool()) {
            case SONIC_TRAP -> SHEPHERD_SONIC_TRAP;
            case FRAG_GRENADE -> SHEPHERD_GRENADE;
            default -> null;
        };
        if (ClientRaptorHudState.hasEquippedTool()) return switch (ClientRaptorHudState.equippedTool()) {
            case FALCON_DRONE -> RAPTOR_FALCON_DRONE;
            case PULSE_GRENADE -> RAPTOR_PULSE_GRENADE;
            default -> null;
        };
        if (ClientVyronHudState.hasEquippedTool()) return switch (ClientVyronHudState.equippedTool()) {
            case MAGNETIC_BOMB -> VYRON_MAGNETIC_BOMB;
            case TIGER_CANNON -> VYRON_TIGER_CANNON;
            default -> null;
        };
        if (ClientVlinderHudState.hasEquippedTool()) return switch (ClientVlinderHudState.equippedTool()) {
            case MEDICAL_DRONE -> VLINDER_MEDICAL_DRONE;
            default -> null;
        };
        return null;
    }

    static boolean commandOwnsHands(Player player) {
        return ClientHeldToolVisualState.commandActive(player) || player == Minecraft.getInstance().player
                && ClientSaeedHudState.shouldRender() && ClientSaeedHudState.commandVisualTicks() > 0;
    }

    static Frame frame(Player player, float partial) {
        if (player == null || !player.isAlive() || player.isSpectator()) return null;
        if (commandOwnsHands(player)) {
            RELEASES.remove(player);
            return null;
        }
        boolean local = player == Minecraft.getInstance().player;
        if (local && (UluruMissileController.isControlling() || ClientSinevaRenderState.isShieldDeployed(player))) return null;
        if (player.hasEffect(com.rzy.dealt_force_skills.registry.ModEffects.NOX_STEALTH) || player.hasEffect(com.rzy.dealt_force_skills.registry.ModEffects.UNDEAD_TRUE_INVISIBILITY) || player.hasEffect(com.rzy.dealt_force_skills.registry.ModEffects.DEPARTMENT_CONCEALMENT)) return null;
        Pulse pulse = RELEASES.get(player);
        float release = pulse == null ? -1 : (player.tickCount - pulse.tick + partial) / 9F;
        if (release >= 1 || release < 0) { pulse = null; release = -1; }
        ToolHandCue cue = pulse == null ? null : pulse.cue;
        String clip = null;
        float seconds = (player.tickCount + partial) / 20F, wind = 0;
        HeldToolVisual held = ClientHeldToolVisualState.visual(player);
        if (cue == null && local) {
            for (var action : ACTIONS) if (ClientToolReleaseAction.isActive(action)) {
                cue = switch (action) {
                    case HACKCLAW_KNIFE -> HACKCLAW_HACKING_KNIFE;
                    case HACKCLAW_FLASH_DRONE -> HACKCLAW_FLASH_DRONE;
                    case NOX_FLASH_GRENADE -> NOX_FLASH_GRENADE;
                    case MORSE_FLASH_GRENADE -> MORSE_FLASH_GRENADE;
                    case SHEPHERD_GRENADE -> SHEPHERD_GRENADE;
                    case LUNA_GRENADE -> LUNA_COMPOSITE_GRENADE;
                    case STINGER_SMOKE_GRENADE -> STINGER_SMOKE_GRENADE;
                    case STINGER_STIM_PRIME -> STINGER_STIM_GUN;
                    case STINGER_STIM_FIRE -> STINGER_STIM_GUN;
                    case TOXIK_TEAR_GAS -> TOXIK_TEAR_GAS;
                    case TOXIK_FIREFLY -> TOXIK_FIREFLY_SWARM;
                    case RAPTOR_PULSE_GRENADE -> RAPTOR_PULSE_GRENADE;
                };
                var active = ClientToolReleaseAction.frame(action,action.animation(),0);
                clip = active.animation(); seconds = active.seconds(); wind = Mth.clamp(seconds / action.fallbackLength(),0,1);
                break;
            }
        }
        if (cue == null && local && prep != null && ClientToolModelAnimationState.isActive(prep.key)) {
            cue = prep.cue;
            var active = ClientToolModelAnimationState.frame(prep.key,prep.model,null,0);
            clip = active.animation(); seconds = active.seconds(); wind = smooth(seconds/.65F);
        }
        if (cue == null && HackclawTerminalVisuals.visible(player)) cue = HACKCLAW_TERMINAL;
        if (cue == null && ClientLunaBowVisualState.isActive(player)) cue = LUNA_BOW;
        if (cue == null && held == HeldToolVisual.ROVER_DOG_WHISTLE) cue = ROVER_DOG_WHISTLE;
        int wildfire = WildfireClient.trackedTool(player.getId());
        if (cue == null && wildfire != 0) cue = wildfire == 1 ? WILDFIRE_SMOKE : WILDFIRE_HE;
        if (cue == null && local) cue = localHeld();
        if (cue == null) cue = ToolHandCue.held(held);
        if (cue == null || CharacterSkillModelVisuals.replacesHand(player)) return null;
        Pulse drawing = DRAWS.get(player);
        if (drawing == null || drawing.cue != cue) drawing = new Pulse(cue,player.tickCount);
        float draw = smooth((player.tickCount - drawing.tick + partial) / (cue == ROVER_DOG_WHISTLE ? 3F : 4F));
        if (pulse != null) { seconds = release * .45F; clip = cue.release; draw = 1; }
        else if (cue == LUNA_BOW && ClientLunaBowVisualState.isActive(player)) {
            clip = ClientLunaBowVisualState.phase(player) == ClientLunaBowVisualState.PHASE_RELEASE ? "release" : "draw";
            seconds = ClientLunaBowVisualState.animationSeconds(player,partial);
            if (!local) draw = "release".equals(clip) ? 1 : Math.max(draw, smooth(seconds / .23F));
            wind = "release".equals(clip) ? 1 - smooth(seconds / .35F) : Mth.clamp(seconds / .8F,0,1);
        } else if (cue == HACKCLAW_TERMINAL) {
            seconds = (ClientHackclawCoreVisualState.CHANNEL_TICKS - ClientHackclawCoreVisualState.remainingTicks(player) + partial) / 20F;
            if (ClientHackclawCoreVisualState.phase(player) == ClientHackclawCoreVisualState.PHASE_SCAN) { clip = "operate_terminal"; seconds = partial / 20F; wind = 1; }
            else { clip = "deploy_front"; wind = smooth(seconds / .6F); }
        } else if (cue == MORSE_SONAR_DETECTOR) {
            wind = smooth(ClientHeldToolVisualState.deployProgress(player, partial));
        } else if (cue == SAEED_CROSSBOW) {
            clip = local ? SaeedPlaceholderVisuals.crossbowClip() : held == HeldToolVisual.SAEED_CROSSBOW_SPENT ? "spent"
                    : held == HeldToolVisual.SAEED_CROSSBOW_RICOCHET ? "ricochet" : "direct";
        }
        if (cue == VYRON_TIGER_CANNON && clip == null) {
            seconds = Math.max(0, (player.tickCount - drawing.tick + partial) / 20F)
                    * ClientToolModelAnimationState.FAST_PLAYBACK_SPEED;
        }
        return new Frame(cue,draw,wind,release,clip != null ? clip : cue.idle,seconds);
    }

    @SubscribeEvent
    public static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event) {
        var world = Minecraft.getInstance().level;
        if (world == null) { DRAWS.clear(); RELEASES.clear(); prep = null; return; }
        for (Player player : world.players()) {
            Frame sample = frame(player,0);
            Pulse old = DRAWS.get(player);
            if (sample == null) DRAWS.remove(player);
            else if (old == null || old.cue != sample.cue) DRAWS.put(player,new Pulse(sample.cue,player.tickCount));
        }
    }

    static float smooth(float value) { float t = Mth.clamp(value,0,1); return t*t*(3-2*t); }
    public static boolean releasedModel(Player player, float partial) {
        Frame frame = frame(player,partial); return frame != null && frame.hidden();
    }
    public static boolean ownsFirstPerson(float partial) { return frame(Minecraft.getInstance().player,partial) != null; }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onHand(RenderHandEvent event) {
        var player = Minecraft.getInstance().player;
        Frame frame = frame(player,event.getPartialTick());
        if (frame == null) return;
        event.setCanceled(true);
        if (event.getHand() == InteractionHand.MAIN_HAND) ClientToolFirstPerson.draw(event,player,frame);
    }

    public static boolean poseThirdPerson(Player player, HumanoidModel<?> model, float partial) {
        Frame frame = frame(player,partial);
        boolean right = player.getMainArm() == HumanoidArm.RIGHT;
        var main = right ? model.rightArm : model.leftArm;
        var off = right ? model.leftArm : model.rightArm;
        int sign = right ? 1 : -1;
        if (frame == null) {
            SkillModelVisual skill = ClientSkillModelVisualState.visual(player);
            if (skill == null || !CharacterSkillModelVisuals.replacesHand(player)) return false;
            float phase = ClientSkillModelVisualState.ageSeconds(player,partial) * 20F / Math.max(1,skill.durationTicks());
            float arc = Mth.sin(Mth.PI * Mth.clamp(phase,0,1));
            main.setRotation(-1.1F - .75F*arc,sign*(.10F-.52F*arc),sign*(-.17F+.31F*arc));
            return true;
        }
        float strike = frame.release < 0 ? 0 : Mth.sin(Mth.PI*smooth(frame.release));
        float lift = frame.draw, wind = frame.wind;
        switch(frame.cue.style) {
            case WHISTLE -> main.setRotation(-1.87F*lift,-sign*.62F*lift,sign*.32F);
            case DISC, CARD -> main.setRotation(-.98F-.5F*strike,sign*(.53F-.98F*strike),sign*(.6F-.65F*strike));
            case BOW -> { main.setRotation(-1.37F,-sign*(.64F-.44F*wind),sign*.16F); off.setRotation(-1.54F,sign*.29F,-sign*.08F); }
            case TERMINAL, PLACE -> { main.setRotation(-1.08F+.48F*wind,sign*(-.27F-.10F*strike),.08F*sign); off.setRotation(-1.10F+.48F*wind,sign*.28F,-.08F*sign); }
            case SIDE -> model.leftArm.setRotation(-.75F+strike*.16F,-.26F,-.1F);
            case HOOK -> off.setRotation(-1.38F-strike*.19F,-sign*.06F,-sign*.04F);
            case CANNON, SHOULDER -> { main.setRotation(-1.48F-strike*.23F,-sign*.09F,-sign*.06F); off.setRotation(-1.40F-strike*.16F,sign*.54F,-sign*.12F); }
            case PALM -> main.setRotation(-1.1F-.49F*strike,sign*.12F,sign*.27F);
            case GUN, GAUNTLET -> main.setRotation(-1.43F-.22F*strike,-sign*.09F,-sign*.04F);
            default -> main.setRotation(-.89F-.62F*wind-.8F*strike,sign*(.15F-.3F*strike),sign*(.15F+.36F*wind));
        }
        return true;
    }
}
