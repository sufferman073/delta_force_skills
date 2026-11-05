package com.rzy.dealt_force_skills.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.rzy.dealt_force_skills.character.*;
import com.rzy.dealt_force_skills.character.wildfire.WildfireConfig;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.client.character.ClientCharacterSelectionState;
import com.rzy.dealt_force_skills.network.*;
import net.minecraft.client.*;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import org.lwjgl.glfw.GLFW;
import java.util.*;

@EventBusSubscriber(modid="dealt_force_skills",value=Dist.CLIENT)
public final class WildfireClient {
    private static final Map<Integer,CompoundTag> STATES=new HashMap<>();
    private static final Map<Integer,Loop> LOOPS=new HashMap<>();
    private static final Map<Integer,CoreSound> CORE_SOUNDS=new HashMap<>();
    private static final boolean[] DOWN=new boolean[2],EQUIPPED=new boolean[2];
    private static final int[] HELD=new int[2];
    private static Object level;
    private WildfireClient(){}
    public static boolean selected(){return ClientCharacterSelectionState.isSelectedCharacter(ModCharacters.WILDFIRE_ID);}
    public static CompoundTag local(){var p=Minecraft.getInstance().player;return p==null?new CompoundTag():STATES.getOrDefault(p.getId(),new CompoundTag());}
    public static int tool(){return selected()?local().getInt("Tool"):0;}
    public static boolean blocksItems(){return selected()&&(local().getInt("Tool")!=0||local().getInt("Phase")!=0);}
    public static void accept(int id,CompoundTag state){
        Minecraft mc=Minecraft.getInstance();if(mc.level==null)return;
        ensureLevel(mc);if(state.isEmpty())STATES.remove(id);else STATES.put(id,state.copy());
        var entity=mc.level.getEntity(id);if(entity!=null)entity.getPersistentData().putBoolean("dfs.wildfire.passive",state.getBoolean("Passive"));
        int phase=state.getInt("Phase");
        if(phase!=0&&(!LOOPS.containsKey(id)||LOOPS.get(id).isStopped())){Loop loop=new Loop(id);LOOPS.put(id,loop);mc.getSoundManager().play(loop);}
        if(phase==0){Loop old=LOOPS.remove(id);if(old!=null)mc.getSoundManager().stop(old);}
        updateCoreSound(mc,id,state);
    }
    private static void ensureLevel(Minecraft mc){if(level!=mc.level){for(Loop loop:LOOPS.values())mc.getSoundManager().stop(loop);LOOPS.clear();stopCoreSounds(mc);STATES.clear();level=mc.level;resetKeys();}}
    public static void reset(){
        var mc=Minecraft.getInstance();for(var loop:LOOPS.values())mc.getSoundManager().stop(loop);
        LOOPS.clear();stopCoreSounds(mc);STATES.clear();level=null;resetKeys();
    }
    private static void resetKeys(){Arrays.fill(DOWN,false);Arrays.fill(EQUIPPED,false);Arrays.fill(HELD,0);}
    public static void tick(Minecraft mc){
        ensureLevel(mc);
        LOOPS.entrySet().removeIf(entry->{var entity=mc.level==null?null:mc.level.getEntity(entry.getKey());if(entity==null||!entity.isAlive()){mc.getSoundManager().stop(entry.getValue());return true;}return false;});
        CORE_SOUNDS.entrySet().removeIf(entry->{
            var entity=mc.level==null?null:mc.level.getEntity(entry.getKey());
            if(entity==null||!entity.isAlive()||entity.isSpectator()){
                mc.getSoundManager().stop(entry.getValue());return true;
            }
            return false;
        });
        if(mc.player==null||!mc.player.isAlive()||mc.player.isSpectator()||!selected()){resetKeys();return;}
        if(mc.screen!=null){resetKeys();return;}
        handleKey(0,KeybindRegister.ACTIVE_SKILL_1,SkillSlot.ACTIVE_1);
        handleKey(1,KeybindRegister.ACTIVE_SKILL_2,SkillSlot.ACTIVE_2);
        if(KeybindRegister.CORE_SKILL!=null)while(KeybindRegister.CORE_SKILL.consumeClick())ClientCharacterSelectionState.useSkill(SkillSlot.CORE);
        if(blocksItems())for(KeyMapping key:List.of(mc.options.keyAttack,mc.options.keyUse,mc.options.keyPickItem,mc.options.keyDrop,mc.options.keySwapOffhand)){
            key.setDown(false);while(key.consumeClick()){}
        }
        if(local().getInt("Phase")>=2){
            double speed=local().getDouble("Speed");Vec3 v=mc.player.getLookAngle().multiply(1,0,1).normalize().scale(speed);
            mc.player.setDeltaMovement(v.x,mc.player.getDeltaMovement().y,v.z);mc.options.keyJump.setDown(false);
        }
    }
    private static void handleKey(int i,KeyMapping key,SkillSlot slot){
        if(key==null)return;boolean clicked=false;while(key.consumeClick())clicked=true;
        if(key.isDown()){
            DOWN[i]=true;HELD[i]++;
            if(HELD[i]>=WildfireConfig.ticks("input.equip_hold_ticks",8)&&!EQUIPPED[i]){EQUIPPED[i]=true;ClientCharacterSelectionState.useSkill(slot,true);}
        }else{
            if((DOWN[i]||clicked)&&!EQUIPPED[i]){
                if(local().getInt("Tool")!=0)NetworkHandler.sendToServer(new C2S_WildfireAction(0));else ClientCharacterSelectionState.useSkill(slot);
            }
            DOWN[i]=false;HELD[i]=0;EQUIPPED[i]=false;
        }
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void mouse(InputEvent.MouseButton.Pre event){
        if(!blocksItems()||Minecraft.getInstance().screen!=null||(event.getButton()!=0&&event.getButton()!=1))return;
        event.setCanceled(true);if(event.getAction()==GLFW.GLFW_PRESS)action(event.getButton()==0);
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void interaction(InputEvent.InteractionKeyMappingTriggered event){
        if(!blocksItems())return;event.setCanceled(true);event.setSwingHand(false);
        // Mouse events own physical buttons; this covers keyboard-bound attack/use mappings.
        if(event.isAttack())action(true);else if(event.isUseItem())action(false);
    }
    private static void action(boolean left){int phase=local().getInt("Phase");if(phase==1&&left)NetworkHandler.sendToServer(new C2S_WildfireAction(2));else if(phase==0)NetworkHandler.sendToServer(new C2S_WildfireAction(left?1:0));}
    public static String detail(SkillSlot slot){
        CompoundTag s=local();if(slot==SkillSlot.CORE){int phase=s.getInt("Phase");return phase==1?String.format(Locale.ROOT,"%.1fs",s.getInt("Prepare")/20D):phase>=2?"∞":s.getInt("Cooldown")>0?(int)Math.ceil(s.getInt("Cooldown")/20D)+"s":"✓";}
        String key=slot==SkillSlot.ACTIVE_1?"Smoke":"Grenade";return s.getInt(key)+"/"+s.getInt(key+"Max");
    }

    public static void renderToolHud(net.minecraft.client.gui.GuiGraphics graphics){
        Minecraft mc=Minecraft.getInstance();if(mc.player==null||mc.screen!=null||!selected())return;
        int tool=tool(),phase=local().getInt("Phase");
        if(tool==0&&phase==0)return;
        var font=mc.font;int center=graphics.guiWidth()/2,bottom=graphics.guiHeight();
        String hint=net.minecraft.network.chat.Component.translatable(phase==1?
                "hud.dealt_force_skills.wildfire.cancel_prepare":phase>=2?
                "hud.dealt_force_skills.wildfire.leap_hint":"hud.dealt_force_skills.wildfire.tool_hint",
                phase>=2&&KeybindRegister.CORE_SKILL!=null?KeybindRegister.CORE_SKILL.getTranslatedKeyMessage():
                        mc.options.keyAttack.getTranslatedKeyMessage(),mc.options.keyUse.getTranslatedKeyMessage()).getString();
        HudTextHelper.drawCenteredFitted(graphics,font,hint,center,bottom-78,Math.max(80,graphics.guiWidth()-32),0xFFFFFFFF);
        if(tool==0)return;
        var slot=tool==1?SkillSlot.ACTIVE_1:SkillSlot.ACTIVE_2;
        ClientCharacterSelectionState.catalogCharacter(ModCharacters.WILDFIRE_ID).flatMap(c->c.skill(slot)).ifPresent(skill->
                graphics.drawCenteredString(font,skill.displayName(),center,bottom-65,0xFFFFFFFF));
        // Replace only the selected slot's picture; the player's actual stack remains server-owned.
        int x=center-90+mc.player.getInventory().selected*20,y=bottom-19;
        graphics.fill(x,y,x+16,y+16,0xFF171A20);
        graphics.pose().pushPose();graphics.pose().translate(x+8,y+14,160);graphics.pose().scale(12,-12,12);
        graphics.pose().mulPose(Axis.XP.rotationDegrees(20));graphics.pose().mulPose(Axis.YP.rotationDegrees(145));
        com.rzy.dealt_force_skills.client.renderer.BlockbenchAnimatedModelRenderer.render(toolModel(tool),"idle",0,
                graphics.pose(),graphics.bufferSource(),15728880);
        graphics.flush();graphics.pose().popPose();
    }
    private static ResourceLocation toolModel(int tool){
        return ResourceLocation.fromNamespaceAndPath("dealt_force_skills",tool==1?"stinger_smoke_grenade":"shared_hand_grenade");
    }

    @SubscribeEvent public static void renderShield(RenderLevelStageEvent event){
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_ENTITIES)return;
        Minecraft mc=Minecraft.getInstance();if(mc.level==null)return;
        var buffer=mc.renderBuffers().bufferSource();Vec3 camera=event.getCamera().getPosition();
        for(var entry:STATES.entrySet()){
            if(entry.getValue().getInt("Phase")<2||!(mc.level.getEntity(entry.getKey()) instanceof Player player)||!player.isAlive())continue;
            float partial=event.getPartialTick().getGameTimeDeltaPartialTick(false);Vec3 pos=player.getPosition(partial);Vec3 forward=player.getLookAngle().multiply(1,0,1).normalize();
            PoseStack pose=event.getPoseStack();pose.pushPose();pose.translate(pos.x-camera.x+forward.x*.8,pos.y-camera.y+1,pos.z-camera.z+forward.z*.8);
            pose.mulPose(Axis.YP.rotationDegrees(-player.getYRot()));
            var pane=net.minecraft.world.level.block.Blocks.GLASS_PANE.defaultBlockState()
                    .setValue(net.minecraft.world.level.block.IronBarsBlock.EAST,true)
                    .setValue(net.minecraft.world.level.block.IronBarsBlock.WEST,true);
            // One native pane covers the former 2 x 2 area without internal tile borders.
            pose.translate(-1,-.55,-.5);
            pose.scale(2,2,1);
            mc.getBlockRenderer().renderSingleBlock(pane,pose,buffer,15728880,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
            pose.popPose();
        }
    }
    private static void stopCoreSounds(Minecraft mc){
        for(CoreSound sound:CORE_SOUNDS.values())mc.getSoundManager().stop(sound);
        CORE_SOUNDS.clear();
    }
    private static void updateCoreSound(Minecraft mc,int id,CompoundTag state){
        CoreSound old=CORE_SOUNDS.get(id);
        if(state.getInt("Phase")<2){
            if(old!=null){mc.getSoundManager().stop(old);CORE_SOUNDS.remove(id);}
            return;
        }
        boolean sustain=state.getInt("CoreAge")>=Math.max(1,state.getInt("AccelerationTicks"));
        if(old==null||old.sustain!=sustain||old.isStopped()){
            if(old!=null)mc.getSoundManager().stop(old);
            CoreSound sound=new CoreSound(id,sustain,state);
            CORE_SOUNDS.put(id,sound);mc.getSoundManager().play(sound);
        }
    }
    private static final class CoreSound extends AbstractTickableSoundInstance {
        private final int id;
        private final boolean sustain;
        CoreSound(int id,boolean sustain,CompoundTag state){
            super((sustain?ModSounds.WILDFIRE_CORE_SUSTAIN:ModSounds.WILDFIRE_CORE_ACCELERATE).get(),SoundSource.PLAYERS,RandomSource.create());
            this.id=id;this.sustain=sustain;looping=sustain;delay=0;
            volume=(float)WildfireConfig.number("audio.loop_volume",.85);
            // Source duration from ffprobe; fit the one-shot to the remaining server acceleration.
            if(!sustain)pitch=(float)(9.014354*20/Math.max(1,state.getInt("AccelerationTicks")-state.getInt("CoreAge")));
            tick();
        }
        @Override public boolean canStartSilent(){return true;}
        @Override public void tick(){
            Minecraft mc=Minecraft.getInstance();var p=mc.level==null?null:mc.level.getEntity(id);
            if(p==null||!p.isAlive()||p.isSpectator()||STATES.getOrDefault(id,new CompoundTag()).getInt("Phase")<2){stop();return;}
            x=p.getX();y=p.getY();z=p.getZ();
        }
    }
    private static final class Loop extends AbstractTickableSoundInstance {
        private final int id;
        Loop(int id){super(SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("dealt_force_skills","wildfire_core_loop")),SoundSource.PLAYERS,RandomSource.create());this.id=id;looping=true;delay=0;volume=(float)WildfireConfig.number("audio.loop_volume",.85);tick();}
        @Override public boolean canStartSilent(){return true;}
        @Override public void tick(){Minecraft mc=Minecraft.getInstance();var p=mc.level==null?null:mc.level.getEntity(id);if(p==null||!p.isAlive()||STATES.getOrDefault(id,new CompoundTag()).getInt("Phase")==0){stop();return;}x=p.getX();y=p.getY();z=p.getZ();}
    }
}
