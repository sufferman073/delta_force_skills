package com.rzy.dealt_force_skills.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.rzy.dealt_force_skills.entity.WildfireDeviceEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;

public final class WildfireDeviceRenderer extends EntityRenderer<WildfireDeviceEntity>{
    private static final ResourceLocation MODEL=new ResourceLocation("dealt_force_skills","generic_gray_smoke");
    private final SmokeCloudRenderer<WildfireDeviceEntity> smoke;
    private final ThrownItemRenderer<WildfireDeviceEntity> grenade;
    public WildfireDeviceRenderer(EntityRendererProvider.Context context){super(context);smoke=new SmokeCloudRenderer<>(context,MODEL,1);grenade=new ThrownItemRenderer<>(context);}
    @Override public void render(WildfireDeviceEntity entity,float yaw,float partial,PoseStack pose,MultiBufferSource buffer,int light){
        if(entity.mode()<WildfireDeviceEntity.CLOUD){grenade.render(entity,yaw,partial,pose,buffer,light);return;}
        smoke.render(entity,yaw,partial,pose,buffer,light);
    }
    @Override public ResourceLocation getTextureLocation(WildfireDeviceEntity entity){return smoke.getTextureLocation(entity);}
}
