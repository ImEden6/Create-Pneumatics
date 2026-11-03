package com.mervyn.createpneumatics.client;

import com.mervyn.createpneumatics.TubePayloadEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class PayloadRenderer extends EntityRenderer<TubePayloadEntity> {
    private final ItemRenderer items;

    public PayloadRenderer(EntityRendererProvider.Context context) {
        super(context);
        items = context.getItemRenderer();
        shadowRadius = 0;
    }

    @Override
    public void render(TubePayloadEntity payload, float yaw, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light) {
        ItemStack stack = payload.getItem();
        if (stack.isEmpty())
            return;
        ms.pushPose();
        ms.translate(0, 0.25, 0);
        ms.mulPose(Axis.YP.rotationDegrees((payload.tickCount + partialTicks) * 20));
        ms.scale(0.35f, 0.35f, 0.35f);
        items.renderStatic(stack, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, ms, buffer, payload.level(), payload.getId());
        ms.popPose();
        super.render(payload, yaw, partialTicks, ms, buffer, light);
    }

    @Override
    public ResourceLocation getTextureLocation(TubePayloadEntity payload) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
