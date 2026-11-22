package com.rzy.dealt_force_skills.client.visual;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import java.util.IdentityHashMap;

final class ConsumableBody {
    private record State(float x, float y, float z, float rx, float ry, float rz, float sx, float sy, float sz) {
        State(ModelPart part) { this(part.x,part.y,part.z,part.xRot,part.yRot,part.zRot,part.xScale,part.yScale,part.zScale); }
        void restore(ModelPart part) {
            part.setPos(x,y,z); part.setRotation(rx,ry,rz); part.xScale=sx; part.yScale=sy; part.zScale=sz;
        }
    }
    private record Pass(Player player, HumanoidArm holder, State left, State right, State ls, State rs) {}
    private static final IdentityHashMap<PlayerModel<?>,Pass> PASSES = new IdentityHashMap<>();

    static boolean owns(PlayerModel<?> model, Player player, HumanoidArm arm) {
        Pass pass = PASSES.get(model);
        return pass != null && pass.player == player && pass.holder == arm;
    }

    static void finish(PlayerModel<?> model, Player player) {
        Pass pass = PASSES.get(model);
        if (pass == null || pass.player != player) return;
        PASSES.remove(model);
        pass.left.restore(model.leftArm); pass.right.restore(model.rightArm);
        pass.ls.restore(model.leftSleeve); pass.rs.restore(model.rightSleeve);
    }

    private static ModelPart.Cube cube(ModelPart part) {
        ModelPart.Cube[] result = {null};
        part.visit(new PoseStack(), (frame, path, index, cube) -> { if (path.isEmpty() && index == 0) result[0] = cube; });
        return result[0];
    }

    private static Vector3f cap(ModelPart arm, ModelPart.Cube cube) {
        return new Vector3f(arm.x+(cube.minX+cube.maxX)*.5F, arm.y+cube.minY, arm.z+(cube.minZ+cube.maxZ)*.5F).div(16);
    }

    private static void place(ModelPart limb, ModelPart sleeve, ModelPart.Cube cube, Matrix4f body, Matrix4f frame, float along) {
        Matrix4f skin = new Matrix4f(body).mul(frame);
        if (skin.determinant3x3()<0) skin.scale(-1,1,1);
        skin.rotateX(-(float)Math.PI/2).scale(ConsumableContact.ARM_SCALE);
        Vector3f origin = skin.transformPosition(-(cube.minX+cube.maxX)/32, -(cube.minY+along*(cube.maxY-cube.minY))/16,
                -(cube.minZ+cube.maxZ)/32, new Vector3f()).mul(16);
        Vector3f scale = skin.getScale(new Vector3f());
        float horizontal = (float)Math.hypot(skin.m00(), skin.m01());
        float yaw = (float)Math.atan2(-skin.m02(), horizontal);
        boolean vertical = horizontal < 1e-6F*scale.x;
        float pitch = vertical ? (float)Math.atan2(-skin.m21()/scale.z, skin.m11()/scale.y)
                : (float)Math.atan2(skin.m12()/scale.y, skin.m22()/scale.z);
        float roll = vertical ? 0 : (float)Math.atan2(skin.m01(), skin.m00());
        limb.setPos(origin.x,origin.y,origin.z); limb.setRotation(pitch,yaw,roll);
        limb.xScale=scale.x; limb.yScale=scale.y; limb.zScale=scale.z; sleeve.copyFrom(limb);
    }

    static boolean draw(PlayerModel<?> model, Player player, ConsumableMotion action, ItemStack stack, HumanoidArm holder,
                        float phase, float ticks, boolean assist, PoseStack pose, MultiBufferSource buffers, int light) {
        if (player.isSpectator() || player.isInvisible() || PASSES.containsKey(model)) return false;
        ModelPart.Cube right = cube(model.rightArm), left = cube(model.leftArm);
        if (right == null || left == null) return false;
        Vector3f r = cap(model.rightArm,right), l = cap(model.leftArm,left);
        Vector3f center = new Vector3f(r).lerp(l,.5F);
        float scale = r.distance(l)/(.235F*2);
        if (scale < .2F) return false;
        Vector3f lateral = new Vector3f(r).sub(l).normalize();
        Vector3f forward = lateral.cross(new Vector3f(0,-1,0),new Vector3f()).normalize();
        Vector3f down = forward.cross(lateral,new Vector3f());
        Matrix4f body = new Matrix4f().set(lateral.x,lateral.y,lateral.z,0, down.x,down.y,down.z,0,
                forward.x,forward.y,forward.z,0, center.x,center.y,center.z,1).scale(scale).translate(0,.22F,-.065F);
        Matrix4f inv = new Matrix4f(body).invert();
        PoseStack joint = new PoseStack(); model.head.translateAndRotate(joint);
        Vector3f mouth = inv.transformPosition(joint.last().pose().transformPosition(0,-.135F,-.26F,new Vector3f()));
        joint = new PoseStack(); model.body.translateAndRotate(joint);
        Vector3f chest = inv.transformPosition(joint.last().pose().transformPosition(0,.245F,-.153F,new Vector3f()));
        int side = holder == HumanoidArm.RIGHT ? 1 : -1;
        mouth.x*=side; chest.x*=side;
        Pass pass = new Pass(player,holder,new State(model.leftArm),new State(model.rightArm),new State(model.leftSleeve),new State(model.rightSleeve));
        pose.pushPose();
        boolean drew;
        try {
            ConsumableContact.apply(pose,body);
            drew = ConsumableUseRig.scene(action,stack,player,holder,phase,ticks,assist,0,pose,buffers,light,mouth,chest,
                    (arm,frame,along) -> {
                        if (arm == HumanoidArm.RIGHT) place(model.rightArm,model.rightSleeve,right,body,frame,along);
                        else place(model.leftArm,model.leftSleeve,left,body,frame,along);
                    });
        } finally { pose.popPose(); }
        if (drew) PASSES.put(model,pass);
        return drew;
    }
    private ConsumableBody() {}
}
