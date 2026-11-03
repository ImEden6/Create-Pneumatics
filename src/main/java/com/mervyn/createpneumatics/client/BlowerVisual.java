package com.mervyn.createpneumatics.client;

import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import com.mervyn.createpneumatics.RootsBlowerBlock;
import com.mervyn.createpneumatics.RootsBlowerBlockEntity;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual;
import com.simibubi.create.foundation.render.AllInstanceTypes;

import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.model.Models;
import com.simibubi.create.AllPartialModels;
import net.minecraft.core.Direction;

// the shaft, plus two lobes turning against each other under the window in the top plate
public class BlowerVisual extends SingleAxisRotatingVisual<RootsBlowerBlockEntity> {
    // lobes sit this far either side of the shaft line along the flanges, and this far up
    static final float SPREAD = 2.25f / 16, LIFT = 2f / 16;
    // the lobes run geared up from the shaft
    static final float GEARING = 2;

    private final RotatingInstance front;
    private final RotatingInstance back;

    public BlowerVisual(VisualizationContext ctx, RootsBlowerBlockEntity be, float partialTick) {
        super(ctx, be, partialTick, Models.partial(AllPartialModels.SHAFT));
        Direction facing = blockState.getValue(RootsBlowerBlock.HORIZONTAL_FACING);
        front = lobe(facing, 1, 0);
        back = lobe(facing, -1, 90);
    }

    private RotatingInstance lobe(Direction facing, int side, float offset) {
        Direction.Axis axis = rotationAxis();
        RotatingInstance lobe = instancerProvider().instancer(AllInstanceTypes.ROTATING, Models.partial(PneumaticPartials.LOBE)).createInstance();
        if (axis == Direction.Axis.Z)
            lobe.rotateTo(1, 0, 0, 0, 0, 1);
        lobe.setRotationAxis(axis)
                .setRotationalSpeed(side * blockEntity.getSpeed() * GEARING * RotatingInstance.SPEED_MULTIPLIER)
                .setRotationOffset(offset)
                .setPosition(getVisualPosition())
                .nudge(facing.getStepX() * SPREAD * side, LIFT, facing.getStepZ() * SPREAD * side)
                .setChanged();
        return lobe;
    }

    @Override
    public void update(float partialTick) {
        super.update(partialTick);
        float speed = blockEntity.getSpeed() * GEARING * RotatingInstance.SPEED_MULTIPLIER;
        front.setRotationalSpeed(speed).setChanged();
        back.setRotationalSpeed(-speed).setChanged();
    }

    @Override
    public void updateLight(float partialTick) {
        super.updateLight(partialTick);
        relight(front, back);
    }

    @Override
    public void collectCrumblingInstances(Consumer<@Nullable Instance> consumer) {
        super.collectCrumblingInstances(consumer);
        consumer.accept(front);
        consumer.accept(back);
    }

    @Override
    protected void _delete() {
        super._delete();
        front.delete();
        back.delete();
    }
}
