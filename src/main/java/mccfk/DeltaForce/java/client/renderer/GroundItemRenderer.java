package mccfk.DeltaForce.java.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import mccfk.DeltaForce.java.entity.GroundItemEntity;
import mccfk.DeltaForce.java.item.DeltaForceItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GroundItemRenderer extends EntityRenderer<GroundItemEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace("textures/item/placeholder.png");
    private final ItemEntityRenderer itemEntityRenderer;

    public GroundItemRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.15F;
        this.shadowStrength = 0.75F;
        this.itemEntityRenderer = new ItemEntityRenderer(context);
    }

    @Override
    public void render(GroundItemEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                      MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();

        ItemStack itemStack = entity.getItem();
        if (itemStack.isEmpty()) {
            poseStack.popPose();
            return;
        }

        renderItemModel(itemStack, entity, poseStack, bufferSource, packedLight);

        poseStack.popPose();

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    private void renderItemModel(ItemStack itemStack, GroundItemEntity entity, PoseStack poseStack,
                                  MultiBufferSource bufferSource, int packedLight) {
        poseStack.translate(0.0D, 0.05D, -0.125D);
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        float scale = 1.0F;
        poseStack.scale(scale, scale, scale);

        Minecraft minecraft = Minecraft.getInstance();
        var itemRenderer = minecraft.getItemRenderer();

        ItemStack renderStack = itemStack;
        if (DeltaForceItems.isRawMaterial(itemStack)) {
            renderStack = new ItemStack(DeltaForceItems.TEMP_DF.get());
        } else if (itemStack.is(DeltaForceItems.HIGH_SPEED_DISK_ARRAY.get())) {
            renderStack = itemStack.copy();
            renderStack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(1));
        }
        itemRenderer.renderStatic(renderStack, ItemDisplayContext.GROUND, packedLight, OverlayTexture.NO_OVERLAY,
            poseStack, bufferSource, entity.level(), 0);
    }

    @Override
    public ResourceLocation getTextureLocation(GroundItemEntity entity) {
        return TEXTURE;
    }
}