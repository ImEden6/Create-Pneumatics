package com.mervyn.createpneumatics.client;

import com.mervyn.createpneumatics.RootsBlowerBlock;
import com.mervyn.createpneumatics.RootsBlowerBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

// only used when Flywheel can't run; BlowerVisual draws the same thing otherwise
public class BlowerRenderer extends KineticBlockEntityRenderer<RootsBlowerBlockEntity> {
    public BlowerRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(RootsBlowerBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        if (VisualizationManager.supportsVisualization(be.getLevel()))
            return;
        super.renderSafe(be, partialTicks, ms, buffer, light, overlay);

        BlockState state = be.getBlockState();
        Direction facing = state.getValue(RootsBlowerBlock.HORIZONTAL_FACING);
        Direction.Axis axis = getRotationAxisOf(be);
        float time = AnimationTickHolder.getRenderTime(be.getLevel());
        float angle = (float) Math.toRadians(time * be.getSpeed() * BlowerVisual.GEARING * 3f / 10 % 360);
        for (int side : new int[] {1, -1}) {
            SuperByteBuffer lobe = CachedBuffers.partial(PneumaticPartials.LOBE, state);
            lobe.translate(facing.getStepX() * BlowerVisual.SPREAD * side, BlowerVisual.LIFT, facing.getStepZ() * BlowerVisual.SPREAD * side)
                    .center();
            if (axis == Direction.Axis.Z)
                lobe.rotateYDegrees(-90);
            lobe.rotateXDegrees((float) Math.toDegrees(angle * side) + (side < 0 ? 90 : 0))
                    .uncenter()
                    .light(light)
                    .renderInto(ms, buffer.getBuffer(RenderType.solid()));
        }
    }

    @Override
    protected BlockState getRenderedBlockState(RootsBlowerBlockEntity be) {
        return shaft(getRotationAxisOf(be));
    }
}
