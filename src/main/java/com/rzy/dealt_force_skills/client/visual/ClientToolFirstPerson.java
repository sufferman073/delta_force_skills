package com.rzy.dealt_force_skills.client.visual;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.client.character.ClientCharacterSkinState;
import com.rzy.dealt_force_skills.client.character.ClientLunaBowVisualState;
import com.rzy.dealt_force_skills.client.character.ClientLunaHudState;
import com.rzy.dealt_force_skills.client.character.ClientUluruHudState;
import com.rzy.dealt_force_skills.client.character.ClientShepherdHudState;
import com.rzy.dealt_force_skills.character.luna.LunaTool;
import com.rzy.dealt_force_skills.client.renderer.BlockbenchAnimatedModelRenderer;
import com.rzy.dealt_force_skills.skill.ToolHandCue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.client.event.RenderHandEvent;
import com.rzy.dealt_force_skills.character.stinger.StingerStimMount;
import org.joml.Vector3f;
import java.util.EnumMap;
import java.util.Set;

final class ClientToolFirstPerson {
    private ClientToolFirstPerson() {}
    private static final EnumMap<ToolHandCue, ResourceLocation> MODELS = new EnumMap<>(ToolHandCue.class);
    static { for (ToolHandCue cue : ToolHandCue.values()) MODELS.put(cue,new ResourceLocation(DealtForceSkillsMod.MODID, cue.model)); }
    private static final Vector3f SIGHT_PICTURE_ANCHOR = new Vector3f(-1.8F,4.9F,-4.75F);
    private record ItemPose(float x,float y,float z,float yaw,float pitch,float roll,float size,
                            float gripX,float gripY,float gripZ) {
        void apply(PoseStack pose, boolean reflect) {
            pose.translate(x,y,z);
            pose.mulPose(Axis.YP.rotationDegrees(yaw));
            pose.mulPose(Axis.XP.rotationDegrees(pitch));
            pose.mulPose(Axis.ZP.rotationDegrees(roll));
            pose.scale(reflect ? -size : size,size,size);
            pose.translate(-gripX/16,-gripY/16,-gripZ/16);
        }
    }

    private static ItemPose placement(ClientToolHandMotion.Frame frame, int side, boolean aiming) {
        ToolHandCue cue = frame.cue(); float draw = frame.draw(), wind = frame.wind();
        float release = Math.max(0,frame.release());
        float snap = Mth.sin(Mth.PI * ClientToolHandMotion.smooth(release));
        float cast = ClientToolHandMotion.smooth(release/.42F), settle = ClientToolHandMotion.smooth((release-.48F)/.52F);
        float x=.34F*side,y=-.29F-(1-draw)*.32F,z=-.60F+(1-draw)*.17F;
        float yaw=0,pitch=0,roll=0,size=.72F,gx=0,gy=0,gz=0;
        if (cue == ToolHandCue.ULURU_MISSILE && aiming) {
            float scale = .91F;
            return new ItemPose(-SIGHT_PICTURE_ANCHOR.z*scale/16, -(SIGHT_PICTURE_ANCHOR.y+4.44F)*scale/16, -.81F,
                    90,0,0,scale,-1.53F,-4.44F,0);
        }
        if (cue == ToolHandCue.N_TWO_CONDENSER_LAUNCHER)
            return new ItemPose(.32F,-.46F-.10F*(1-draw),-1.04F+.035F*snap,90,-3,0,.70F,
                    ThirdPersonToolGrip.TOP_GRIP_X,ThirdPersonToolGrip.TOP_GRIP_Y,0);
        if (cue == ToolHandCue.ULURU_COVER)
            return new ItemPose(side*(.34F+.08F*wind-.21F*snap),y+.12F*wind+.09F*snap-.13F*settle,
                    -.65F+.12F*wind-.33F*snap,side*(-12+18*snap),-18+26*snap,side*(12+22*wind-38*snap),.53F,side*4,-2,2);
        switch(cue.style) {
            case THROW, KNIFE -> { x += side*(.09F*wind-.24F*snap); y += .11F*wind+.11F*snap-.2F*settle; z += .13F*wind-.32F*snap; roll = side*(12+32*wind-42*snap); if (cue.style == ToolHandCue.Style.KNIFE) { y += .065F; pitch = 22; gy=-9.5F; } }
            case DISC, CARD -> { x += side*(-.31F*snap+.10F*wind); z -= .27F*snap; y -= .12F*settle; yaw=side*(-25+105*cast); pitch=cue.style == ToolHandCue.Style.DISC ? 0 : -22; roll=side*(4-14*snap); size=.95F; if(cue.style==ToolHandCue.Style.DISC){ x-=side*.12F; y+=.14F; z-=.25F; pitch=12-8*snap; gz=3.2F; } }
            case PLACE -> { x=side*.30F; y -= .27F*Math.max(wind,snap); z -= .18F*Math.max(wind,snap); size=cue==ToolHandCue.MORSE_SONAR_DETECTOR ? .75F : .68F; gx=side*6.4F; gy=-2.3F; if(cue==ToolHandCue.MORSE_SONAR_DETECTOR){ x=side*.23F; y=-.24F-(1-draw)*.25F-.16F*Math.max(wind,snap); z=-.89F-.14F*wind; gx=side*5.3F; gy=.2F; gz=2.8F; } }
            case PALM -> { x-=side*.06F*snap; y+=.09F*snap; z-=.25F*snap; gy=cue==ToolHandCue.STINGER_SMOKE_DRONE ? -2.0F : -3.2F; gz=cue==ToolHandCue.STINGER_SMOKE_DRONE ? 2.1F : 0; size=.62F; }
            case CANNON, SHOULDER -> { x=side*(cue.style==ToolHandCue.Style.SHOULDER?.34F:.31F); y+=cue.style==ToolHandCue.Style.SHOULDER?.03F:0; z=-.82F+.09F*snap; yaw=side>0?82:98; pitch=0; roll=3+7*snap; size=cue.style==ToolHandCue.Style.SHOULDER?.91F:.80F; gx=cue==ToolHandCue.VYRON_TIGER_CANNON?1.225F:-1.53F; gy=cue==ToolHandCue.VYRON_TIGER_CANNON?-3.5F:-4.44F; }
            case GUN, GAUNTLET, HOOK -> { if(cue.style==ToolHandCue.Style.HOOK) x=-side*.35F; z += .11F*snap; pitch=4+13*snap; if(cue.style!=ToolHandCue.Style.GUN) yaw=90; if(cue==ToolHandCue.STINGER_STIM_GUN || cue==ToolHandCue.ROVER_AEROSOL_INJECTOR) {gy=-3.4F;gz=3.9F;} if(cue==ToolHandCue.SINEVA_HOOK){gx=-5.7F;gy=-4.2F;} }
            case SIDE -> { x=-.59F+(1-draw)*-.20F; y=-.33F; z=-.6F+.13F*snap; yaw=-6; pitch=3+9*snap; size=.85F; }
            case WHISTLE -> { x=side*(.24F-.19F*draw); y=-.43F+.28F*draw; z=-.47F+.22F*draw; pitch=34*draw; roll=side*(18-13*draw); size=.51F; gy=4.15F; }
            case BOW -> { x=.36F; y=-.20F-(1-draw)*.16F; z=-1.02F; yaw=-7; pitch=0; roll=-7; size=.63F; gx=.68F; }
            case TERMINAL -> { x=.28F*side; y=-.36F+.10F*draw; z=-.73F; pitch=-12; size=.73F; gx=5.4F*side; gy=-2; }
        }
        if (cue == ToolHandCue.STINGER_STIM_GUN) {
            x = side * StingerStimMount.HAND_SIDE;
            y = StingerStimMount.HAND_Y - (1-draw)*.32F;
            z = StingerStimMount.HAND_Z + (1-draw)*.17F + .11F*snap;
            pitch = StingerStimMount.PITCH + 13*snap;
            size = StingerStimMount.SIZE;
            gy = StingerStimMount.GRIP_Y;
            gz = StingerStimMount.GRIP_Z;
        }
        return new ItemPose(x,y,z,yaw,pitch,roll,size,gx,gy,gz);
    }

    static void draw(RenderHandEvent event, AbstractClientPlayer player, ClientToolHandMotion.Frame frame) {
        ToolHandCue cue = frame.cue();
        boolean right = player.getMainArm() == HumanoidArm.RIGHT;
        int side = right ? 1 : -1;
        if (cue == ToolHandCue.SINEVA_HOOK) {
            SinevaPlaceholderVisuals.drawGrapple(event, frame.clip(), frame.seconds());
            return;
        }
        boolean grenade = cue == ToolHandCue.SHEPHERD_GRENADE || cue == ToolHandCue.LUNA_COMPOSITE_GRENADE;
        boolean aiming = cue == ToolHandCue.ULURU_MISSILE && ClientUluruHudState.missileAiming();
        boolean bow = cue == ToolHandCue.LUNA_BOW;
        ItemPose placement = placement(frame,side,aiming);
        boolean mirrorPlacement = !right && (aiming || cue == ToolHandCue.N_TWO_CONDENSER_LAUNCHER);
        boolean reflect = bow && right || grenade && right;
        PoseStack local = new PoseStack();
        if (mirrorPlacement) local.scale(-1,1,1);
        placement.apply(local,reflect);
        PoseStack pose = event.getPoseStack();
        if (!frame.hidden()) {
            pose.pushPose();
            if (mirrorPlacement) pose.scale(-1,1,1);
            placement.apply(pose,reflect);
            ResourceLocation model = MODELS.get(cue);
            if (cue == ToolHandCue.LUNA_BOW) {
                LunaTool arrow = ClientLunaBowVisualState.isActive(player) ? ClientLunaBowVisualState.tool(player) : ClientLunaHudState.equippedTool();
                drawBow(arrow,frame.clip(),frame.seconds(),pose,event.getMultiBufferSource(),event.getPackedLight());
            } else if (grenade) {
                drawGrenade(event,model,frame,pose);
            } else if (cue == ToolHandCue.HACKCLAW_HACKING_KNIFE) {
                BlockbenchAnimatedModelRenderer.render(model,"idle_charge",frame.seconds(),pose,event.getMultiBufferSource(),event.getPackedLight());
            } else if (cue.style == ToolHandCue.Style.CARD) {
                Minecraft.getInstance().getItemRenderer().renderStatic(player,new ItemStack(cue == ToolHandCue.CHAMBER_TRAP_CARD ? Items.PAPER : Items.MAP),ItemDisplayContext.FIXED,false,pose,event.getMultiBufferSource(),player.level(),event.getPackedLight(),OverlayTexture.NO_OVERLAY,0);
            } else BlockbenchAnimatedModelRenderer.render(model,frame.clip(),frame.seconds(),pose,event.getMultiBufferSource(),event.getPackedLight());
            pose.popPose();
        }
        boolean mainRight = switch (cue.style) {
            case SIDE -> false;
            default -> right;
        };
        if(cue.style==ToolHandCue.Style.SIDE) { hand(event,player,true,new Vector3f(-.14F,-.40F,-.48F),50); return; }
        Vector3f grip = new Vector3f(placement.x,placement.y,placement.z);
        if (mirrorPlacement) grip.x = -grip.x;
        if (grenade) {
            drawGrenadeHands(event, player, frame, local, right);
            return;
        }
        if (bow) {
            Vector3f string = local.last().pose().transformPosition(ThirdPersonToolGrip.bowDrawHand(frame.clip(), frame.seconds()).div(16));
            fixedArm(event, !right, grip, new Vector3f(-side*.47F,-.42F,-.09F));
            fixedArm(event, right, string, new Vector3f(side*.49F,-.39F,-.08F));
            return;
        }
        if (cue == ToolHandCue.N_TWO_CONDENSER_LAUNCHER || aiming) {
            Vector3f socket = aiming ? new Vector3f(6,-4.4F,0) : ThirdPersonToolGrip.launcherSideGrip();
            Vector3f support = local.last().pose().transformPosition(socket.div(16));
            int rearSide = mainRight ? 1 : -1;
            fixedArm(event,mainRight,grip,new Vector3f(rearSide*.53F,-.45F,-.12F));
            fixedArm(event,!mainRight,support,new Vector3f(-rearSide*.49F,-.44F,-.12F));
            return;
        }
        hand(event,player,mainRight,grip,cue.style == ToolHandCue.Style.WHISTLE ? 42 : 66);
        if (cue == ToolHandCue.ULURU_COVER || cue == ToolHandCue.HACKCLAW_HACKING_KNIFE) return;
        Vector3f support = switch(cue.style) {
            case CANNON, SHOULDER -> new Vector3f(6,-4.4F,0);
            case PLACE, TERMINAL -> new Vector3f(-placement.gripX,placement.gripY,placement.gripZ);
            case THROW, KNIFE -> frame.wind() > .04F && frame.release() < 0 ? new Vector3f(-side*1.6F,3F,0) : null;
            default -> null;
        };
        if (support != null) {
            support.div(16); local.last().pose().transformPosition(support);
            hand(event,player,!mainRight,support,cue.style == ToolHandCue.Style.BOW ? 64 : 72);
        }
    }

    private static float pinTravel(ClientToolHandMotion.Frame frame) {
        return frame.release() >= 0 ? 1 : ClientToolHandMotion.smooth((frame.wind()-.30F)/.36F);
    }

    private static void drawGrenade(RenderHandEvent event, ResourceLocation model, ClientToolHandMotion.Frame frame, PoseStack pose) {
        var buffers = event.getMultiBufferSource(); int light = event.getPackedLight();
        BlockbenchAnimatedModelRenderer.render(model,null,0,pose,buffers,light,Set.of("safety_pin","pull_ring"),0xFFFFFFFF);
        boolean cooked = frame.cue() == ToolHandCue.SHEPHERD_GRENADE ? ClientShepherdHudState.grenadeCookTicks() > 0
                : ClientLunaHudState.grenadeCookTicks() > 0;
        if (frame.release() >= 0 || frame.wind() >= .88F || cooked && !"prime_release".equals(frame.clip())) return;
        pose.pushPose();
        pose.translate(7*pinTravel(frame)/16,0,0);
        BlockbenchAnimatedModelRenderer.render(model,null,0,pose,buffers,light,
                Set.of("static_body","safety_lever","fuse_striker"),0xFFFFFFFF);
        pose.popPose();
    }

    private static void drawGrenadeHands(RenderHandEvent event, AbstractClientPlayer player, ClientToolHandMotion.Frame frame,
                                         PoseStack local, boolean right) {
        float side = right ? 1 : -1;
        Vector3f palm = local.last().pose().transformPosition(-2.5F/16,-.8F/16,1.8F/16,new Vector3f());
        fixedArm(event, right, palm, new Vector3f(side*.52F,-.54F,-.10F));
        if (frame.release() >= 0 || frame.wind() <= 0 || frame.wind() >= .88F) return;
        float approach = ClientToolHandMotion.smooth(frame.wind()/.30F);
        float withdraw = ClientToolHandMotion.smooth((frame.wind()-.67F)/.21F);
        Vector3f ring = local.last().pose().transformPosition((4.6F+7*pinTravel(frame))/16,4.45F/16,1.2F/16,new Vector3f());
        Vector3f low = new Vector3f(-side*.45F,-.69F,-.39F);
        Vector3f finger = new Vector3f(low).lerp(ring,approach).lerp(low,withdraw);
        fixedArm(event,!right,finger,new Vector3f(-side*.52F,-.47F,-.12F));
    }

    static void fixedArm(RenderHandEvent event, boolean right, Vector3f grip, Vector3f shoulder) {
        armBetween(Minecraft.getInstance().player, right, grip, shoulder, event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight());
    }

    static void armBetween(AbstractClientPlayer player, boolean right, Vector3f grip, Vector3f shoulder,
                           PoseStack pose, MultiBufferSource buffer, int light) {
        if (player == null || player.isInvisible() || player.isSpectator()
                || !(Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player) instanceof PlayerRenderer renderer)) return;
        var skin = ClientCharacterSkinState.skinFor(player).orElseGet(() -> renderer.getTextureLocation(player));
        var model = renderer.getModel();
        ModelPart limb = right ? model.rightArm : model.leftArm;
        Vector3f axis = new Vector3f(shoulder).sub(grip);
        float length = axis.length();
        if (length < .001F) return;
        limb.visit(new PoseStack(), (unused, path, index, cube) -> {
            if (!path.isEmpty() || index != 0) return;
            pose.pushPose();
            pose.translate(grip.x,grip.y,grip.z);
            pose.mulPose(new org.joml.Quaternionf().rotationTo(new Vector3f(0,-1,0),axis.normalize()));
            pose.scale(.61F,16*length/(cube.maxY-cube.minY),.61F);
            pose.translate(-(cube.minX+cube.maxX)/32F,-cube.maxY/16F,-(cube.minZ+cube.maxZ)/32F);
            arm(limb,pose,buffer,RenderType.entitySolid(skin),light);
            if (player.isModelPartShown(right ? PlayerModelPart.RIGHT_SLEEVE : PlayerModelPart.LEFT_SLEEVE))
                arm(right ? model.rightSleeve : model.leftSleeve,pose,buffer,RenderType.entityTranslucent(skin),light);
            pose.popPose();
        });
    }

    static void drawBow(LunaTool arrow, String clip, float seconds, PoseStack pose, MultiBufferSource buffer, int light) {
        var bow = MODELS.get(ToolHandCue.LUNA_BOW);
        if ("release".equals(clip)) { BlockbenchAnimatedModelRenderer.render(bow,clip,seconds,pose,buffer,light); return; }
        BlockbenchAnimatedModelRenderer.renderWithGroupPlacement(bow,clip,seconds,pose,buffer,light,"nocking_point",() -> {
            pose.pushPose(); pose.translate(0,0,-5.25/16);
            BlockbenchAnimatedModelRenderer.render(arrow==LunaTool.RECON_BOW ? RECON : SHOCK,null,0,pose,buffer,light);
            pose.popPose();
        });
    }
    private static final ResourceLocation RECON = new ResourceLocation(DealtForceSkillsMod.MODID, "luna_recon_arrow");
    private static final ResourceLocation SHOCK = new ResourceLocation(DealtForceSkillsMod.MODID, "luna_shock_arrow");

    static void attachedHand(RenderHandEvent event, boolean right, float x, float y, float z) {
        var player = Minecraft.getInstance().player;
        if (player != null) hand(event,player,right,new Vector3f(x/16,y/16,z/16),62);
    }

    static void hand(RenderHandEvent event, AbstractClientPlayer player, boolean right, Vector3f at, float pitch) {
        skinHand(event.getPoseStack(),event.getMultiBufferSource(),event.getPackedLight(),player,right,at,pitch);
    }
    static void skinHand(PoseStack pose, MultiBufferSource buffer, int light, AbstractClientPlayer player, boolean right, Vector3f at, float pitch) {
        if (player.isInvisible() || player.isSpectator() || !(Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player) instanceof PlayerRenderer renderer)) return;
        var model = renderer.getModel(); int sign = right ? 1 : -1;
        var skin = ClientCharacterSkinState.skinFor(player).orElseGet(() -> renderer.getTextureLocation(player));
        pose.pushPose(); pose.translate(at.x,at.y,at.z); pose.mulPose(Axis.ZP.rotationDegrees(sign*18));
        pose.mulPose(Axis.YP.rotationDegrees(sign*20)); pose.mulPose(Axis.XP.rotationDegrees(-pitch));
        pose.scale(.73F,.73F,.73F); pose.translate(sign*.055,-.59,0);
        arm(right ? model.rightArm : model.leftArm,pose,buffer,RenderType.entitySolid(skin),light);
        if (player.isModelPartShown(right ? PlayerModelPart.RIGHT_SLEEVE : PlayerModelPart.LEFT_SLEEVE))
            arm(right ? model.rightSleeve : model.leftSleeve,pose,buffer,RenderType.entityTranslucent(skin),light);
        pose.popPose();
    }
    private static final ModelPart.Cube[] SKIN_SECTIONS = new ModelPart.Cube[32];

    private static ModelPart.Cube skinSection(boolean right, boolean slim, boolean sleeve, int section) {
        int index = (right ? 16 : 0) + (slim ? 8 : 0) + (sleeve ? 4 : 0) + section;
        if (SKIN_SECTIONS[index] != null) return SKIN_SECTIONS[index];
        int width = slim ? 3 : 4;
        int textureU = right ? 40 : sleeve ? 48 : 32;
        int textureV = (right ? sleeve ? 32 : 16 : 48) + (section == 1 ? 6 : 0);
        float pad = sleeve ? .25F : 0;
        var faces = section == 0 ? java.util.EnumSet.allOf(net.minecraft.core.Direction.class)
                : section == 1 ? java.util.EnumSet.of(net.minecraft.core.Direction.NORTH,net.minecraft.core.Direction.SOUTH,
                        net.minecraft.core.Direction.EAST,net.minecraft.core.Direction.WEST)
                : java.util.EnumSet.of(net.minecraft.core.Direction.UP,net.minecraft.core.Direction.DOWN);
        return SKIN_SECTIONS[index] = new ModelPart.Cube(textureU,textureV,-width/2F,0,-2,width,6,4,pad,pad,pad,false,64,64,faces);
    }

    static void foldedArm(AbstractClientPlayer player, boolean right, Vector3f shoulder, Vector3f elbow, Vector3f fist,
                          PoseStack pose, MultiBufferSource buffers, int light, float thickness) {
        if (player == null || player.isInvisible() || player.isSpectator()
                || !(Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player) instanceof PlayerRenderer renderer)) return;
        var skin = ClientCharacterSkinState.skinFor(player).orElseGet(() -> renderer.getTextureLocation(player));
        ModelPart limb = right ? renderer.getModel().rightArm : renderer.getModel().leftArm;
        limb.visit(new PoseStack(), (unused,path,index,cube) -> {
            if (!path.isEmpty() || index != 0) return;
            boolean slim = cube.maxX-cube.minX < 3.5F;
            for (int segment=0;segment<2;segment++) {
                Vector3f start = segment == 0 ? shoulder : elbow;
                Vector3f axis = new Vector3f(segment == 0 ? elbow : fist).sub(start);
                float length = axis.length();
                pose.pushPose(); pose.translate(start.x,start.y,start.z);
                pose.mulPose(new org.joml.Quaternionf().rotationTo(new Vector3f(0,1,0),axis.normalize()));
                pose.scale(thickness,length*16/6,thickness);
                for (int layer=0;layer<2;layer++) {
                    boolean sleeve = layer != 0;
                    if (sleeve && !player.isModelPartShown(right ? PlayerModelPart.RIGHT_SLEEVE : PlayerModelPart.LEFT_SLEEVE)) continue;
                    var consumer = buffers.getBuffer(sleeve ? RenderType.entityTranslucent(skin) : RenderType.entitySolid(skin));
                    for (int section=segment;section<=(segment == 0 ? 0 : 2);section++)
                        skinSection(right,slim,sleeve,section).compile(pose.last(),consumer,light,OverlayTexture.NO_OVERLAY,1,1,1,1);
                }
                pose.popPose();
            }
        });
    }

    private static void arm(ModelPart part, PoseStack pose, MultiBufferSource buffer, RenderType type, int light) {
        var saved=part.storePose(); boolean visible=part.visible;
        try { part.resetPose(); part.setPos(0,0,0); part.setRotation(0,0,0); part.visible=true;
            part.render(pose,buffer.getBuffer(type),light,OverlayTexture.NO_OVERLAY);
        } finally { part.loadPose(saved); part.visible=visible; }
    }
}
