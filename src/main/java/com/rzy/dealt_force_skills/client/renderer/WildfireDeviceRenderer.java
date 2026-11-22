package com.rzy.dealt_force_skills.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.rzy.dealt_force_skills.entity.WildfireDeviceEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;

public final class WildfireDeviceRenderer extends EntityRenderer<WildfireDeviceEntity>{
    private static final ResourceLocation MODEL=new ResourceLocation("dealt_force_skills","generic_gray_smoke");
    private final SmokeCloudRenderer<WildfireDeviceEntity> smoke;
    private final BlockbenchProjectileRenderer<WildfireDeviceEntity> ringPod;
    private final BlockbenchProjectileRenderer<WildfireDeviceEntity> hePod;
    public WildfireDeviceRenderer(EntityRendererProvider.Context context){super(context);smoke=new SmokeCloudRenderer<>(context,MODEL,1);
        ringPod=new BlockbenchProjectileRenderer<>(context,com.rzy.dealt_force_skills.client.WildfireClient.toolModel(1),null,"flight",0.85F,BlockbenchProjectileRenderer.Alignment.NONE);
        hePod=new BlockbenchProjectileRenderer<>(context,com.rzy.dealt_force_skills.client.WildfireClient.toolModel(2),null,"flight",0.8F,BlockbenchProjectileRenderer.Alignment.NONE);
    }
    @Override public void render(WildfireDeviceEntity entity,float yaw,float partial,PoseStack pose,MultiBufferSource buffer,int light){
        if(entity.mode()<WildfireDeviceEntity.CLOUD){(entity.mode()==WildfireDeviceEntity.HE_GRENADE?hePod:ringPod).render(entity,yaw,partial,pose,buffer,light);return;}
        smoke.render(entity,yaw,partial,pose,buffer,light);
    }
    public SmokeCloudRenderer<WildfireDeviceEntity> smokeRenderer(){return smoke;}
    @Override public ResourceLocation getTextureLocation(WildfireDeviceEntity entity){return smoke.getTextureLocation(entity);}
}
