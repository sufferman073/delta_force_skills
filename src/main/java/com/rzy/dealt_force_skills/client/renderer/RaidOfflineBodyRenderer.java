package com.rzy.dealt_force_skills.client.renderer;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.raid.RaidActorTypes;
import com.rzy.dealt_force_skills.raid.RaidOfflineBodyEntity;
import net.minecraft.client.model.HumanoidArmorModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DealtForceSkillsMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class RaidOfflineBodyRenderer extends MobRenderer<RaidOfflineBodyEntity, PlayerModel<RaidOfflineBodyEntity>> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("minecraft", "textures/entity/player/wide/steve.png");
    public RaidOfflineBodyRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.45F);
        addLayer(new HumanoidArmorLayer<>(this, new HumanoidArmorModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidArmorModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)), context.getModelManager()));
        addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
    }
    @Override public ResourceLocation getTextureLocation(RaidOfflineBodyEntity body) { return TEXTURE; }
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(RaidActorTypes.OFFLINE_BODY, RaidOfflineBodyRenderer::new);
    }
}
