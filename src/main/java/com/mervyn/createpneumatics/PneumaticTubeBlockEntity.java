package com.mervyn.createpneumatics;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.kinetics.fan.AirCurrent;
import com.simibubi.create.content.kinetics.fan.IAirCurrentSource;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessingType;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class PneumaticTubeBlockEntity extends SmartBlockEntity implements IAirCurrentSource, IHaveGoggleInformation {
    // a fan at this rpm blows as hard as a tube at full pressure
    public static final float RPM_SCALE = 128f;

    TubeTransportBehaviour transport;
    private final AirCurrent airCurrent = new AirCurrent(this) {
        @Nullable
        @Override
        public FanProcessingType getTypeAt(float offset) {
            return Config.VENT_RUNS_PROCESSING.get() ? super.getTypeAt(offset) : null;
        }
    };
    @Nullable
    private Direction vent;
    private float ventSpeed;
    private boolean rebuildAir;
    private int entitySearch;

    public PneumaticTubeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        transport = new TubeTransportBehaviour(this);
        behaviours.add(transport);
    }

    @Override
    public void initialize() {
        super.initialize();
        if (!transport.hasPressure())
            PneumaticPropagator.queue(level, worldPosition);
    }

    void pressureChanged() {
        Direction open = null;
        for (Direction side : Direction.values())
            if (PneumaticTubeBlock.isOpenEnd(level, worldPosition, getBlockState(), side)) {
                open = side;
                break;
            }
        // rounded so a tiny wobble far down the line does not resend every tube
        float speed = open == null ? 0 : Math.round(transport.get(open).pressure() * RPM_SCALE / 4f) * 4f;
        if (open == vent && speed == ventSpeed)
            return;
        vent = open;
        ventSpeed = speed;
        rebuildAir = true;
        sendData();
    }

    @Override
    public void tick() {
        super.tick();
        if (rebuildAir) {
            rebuildAir = false;
            airCurrent.rebuild();
        }
        if (vent == null || ventSpeed == 0)
            return;
        if (entitySearch-- <= 0) {
            entitySearch = 5;
            airCurrent.findEntities();
        }
        airCurrent.tick();
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        if (!clientPacket)
            return;
        if (vent != null)
            tag.putInt("Vent", vent.get3DDataValue());
        tag.putFloat("VentSpeed", ventSpeed);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        if (!clientPacket)
            return;
        vent = tag.contains("Vent") ? Direction.from3DDataValue(tag.getInt("Vent")) : null;
        ventSpeed = tag.getFloat("VentSpeed");
        rebuildAir = true;
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        if (vent == null || ventSpeed == 0)
            return false;
        tooltip.add(Component.literal("    ").append(Component.translatable(ventSpeed > 0
                ? "createpneumatics.venting" : "createpneumatics.drawing")));
        return true;
    }

    @Override
    public AirCurrent getAirCurrent() {
        return airCurrent;
    }

    @Nullable
    @Override
    public Level getAirCurrentWorld() {
        return level;
    }

    @Override
    public BlockPos getAirCurrentPos() {
        return worldPosition;
    }

    @Override
    public float getSpeed() {
        return vent == null ? 0 : Math.abs(ventSpeed);
    }

    @Override
    public Direction getAirflowOriginSide() {
        return vent == null ? Direction.UP : vent;
    }

    @Nullable
    @Override
    public Direction getAirFlowDirection() {
        if (vent == null || ventSpeed == 0)
            return null;
        return ventSpeed > 0 ? vent : vent.getOpposite();
    }

    @Override
    public boolean isSourceRemoved() {
        return isRemoved();
    }
}
