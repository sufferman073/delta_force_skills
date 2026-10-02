package com.rzy.dealt_force_skills.client.model;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.entity.BeaconBossEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

public final class BeaconBossModel extends PlayerModel<BeaconBossEntity> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "beacon_boss"), "main");

    public BeaconBossModel(ModelPart root) {
        super(root, false);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = PlayerModel.createMesh(CubeDeformation.NONE, false);
        PartDefinition head = mesh.getRoot().getChild("head");
        head.addOrReplaceChild("beacon_detail_1",
                CubeListBuilder.create().texOffs(2, 1)
                        .addBox(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F),
                PartPose.offsetAndRotation(-4.0F, -5.7228F, -0.6036F,
                        0.0366F, 0.6099F, 1.6347F));
        head.addOrReplaceChild("beacon_detail_2",
                CubeListBuilder.create().texOffs(1, 0)
                        .addBox(0.0F, 0.0F, -1.0F, 1.0F, 1.0F, 2.0F),
                PartPose.offsetAndRotation(-4.3F, -5.0F, -1.3F,
                        -0.0524F, 0.0F, 0.0F));
        head.addOrReplaceChild("beacon_detail_3",
                CubeListBuilder.create().texOffs(2, 1)
                        .addBox(0.0F, 0.0F, -1.0F, 1.0F, 1.0F, 1.0F),
                PartPose.offsetAndRotation(-4.3F, -5.5F, -1.4F,
                        -0.1047F, 0.0F, 0.0F));
        head.addOrReplaceChild("beacon_detail_4",
                CubeListBuilder.create().texOffs(1, 0)
                        .addBox(0.0F, -1.0F, -1.0F, 1.0F, 2.0F, 2.0F),
                PartPose.offsetAndRotation(-4.3F, -5.5F, -0.5F,
                        -0.6109F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }
}
