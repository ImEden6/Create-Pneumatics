package com.mervyn.createpneumatics.client;

import com.mervyn.createpneumatics.BellJarBlock;
import com.mervyn.createpneumatics.BellJarBlockEntity;
import com.mervyn.createpneumatics.BellJarCycle;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

// the needle on the jar's dial, and whatever is sealed inside turning slowly while it's worked
public class BellJarRenderer extends SafeBlockEntityRenderer<BellJarBlockEntity> {
    private static final float SWEEP = 135;

    public BellJarRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    protected void renderSafe(BellJarBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        if (be.stacked())
            return;
        BlockState state = be.getBlockState();
        float yaw = BellJarBlock.facing(state).toYRot() - 180;
        // the needle partial is drawn for a gauge facing north, pivoting at the block's middle; here it
        // moves down and forward onto the dial on the jar's base
        CachedBuffers.partial(PneumaticPartials.NEEDLE, state)
                .center()
                .rotateYDegrees(-yaw)
                .translate(0, -1.5f / 16, -5.4f / 16)
                .rotateZDegrees(be.needle(partialTicks) * SWEEP)
                .uncenter()
                .light(light)
                .renderInto(ms, buffer.getBuffer(RenderType.cutout()));

        BellJarCycle cycle = be.cycle();
        ItemStack shown = !cycle.batch.isEmpty() ? cycle.batch.get(0) : !cycle.outputs.isEmpty() ? cycle.outputs.get(0) : ItemStack.EMPTY;
        if (shown.isEmpty())
            return;
        float time = AnimationTickHolder.getRenderTime(be.getLevel());
        float spin = cycle.phase == BellJarCycle.Phase.RUNNING ? time * 3 : 0;
        ms.pushPose();
        ms.translate(0.5, 0.45 + Math.sin(time / 12) * 0.02, 0.5);
        ms.mulPose(Axis.YP.rotationDegrees(spin));
        ms.scale(0.5f, 0.5f, 0.5f);
        Minecraft.getInstance().getItemRenderer().renderStatic(shown, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, ms, buffer, be.getLevel(), 0);
        ms.popPose();
    }
}
