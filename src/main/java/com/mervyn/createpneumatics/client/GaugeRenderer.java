package com.mervyn.createpneumatics.client;

import com.mervyn.createpneumatics.PressureGaugeBlockEntity;
import com.mervyn.createpneumatics.TubeMountBlock;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public class GaugeRenderer extends SafeBlockEntityRenderer<PressureGaugeBlockEntity> {
    // full pressure swings the needle this far from straight up, matching the dial's 270 degree sweep
    private static final float SWEEP = 135;

    public GaugeRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    protected void renderSafe(PressureGaugeBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        BlockState state = be.getBlockState();
        Direction facing = state.getValue(TubeMountBlock.FACING);
        // the same turn the blockstate gives the model, which is drawn facing north
        float yaw = switch (facing) {
            case EAST -> 90;
            case SOUTH -> 180;
            case WEST -> 270;
            default -> 0;
        };
        float pitch = facing == Direction.UP ? 270 : facing == Direction.DOWN ? 90 : 0;
        CachedBuffers.partial(PneumaticPartials.NEEDLE, state)
                .center()
                .rotateYDegrees(-yaw)
                .rotateXDegrees(-pitch)
                .rotateZDegrees(-be.needle(partialTicks) * SWEEP)
                .uncenter()
                .light(light)
                .renderInto(ms, buffer.getBuffer(RenderType.cutout()));
    }
}
