package com.mervyn.createpneumatics;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;

// Every face of a tube gets a connection, even the ones pointing at air: those are the faces that
// can be an open end, so the propagator needs to be able to zero them too.
public class TubeTransportBehaviour extends BlockEntityBehaviour {
    public static final BehaviourType<TubeTransportBehaviour> TYPE = new BehaviourType<>();

    private final Map<Direction, TubeConnection> interfaces = new EnumMap<>(Direction.class);

    public TubeTransportBehaviour(SmartBlockEntity be) {
        super(be);
        for (Direction side : Direction.values())
            interfaces.put(side, new TubeConnection(side));
    }

    public Collection<TubeConnection> connections() {
        return interfaces.values();
    }

    public TubeConnection get(Direction side) {
        return interfaces.get(side);
    }

    public float pressure() {
        float strongest = 0;
        for (TubeConnection connection : interfaces.values())
            if (Math.abs(connection.pressure()) > Math.abs(strongest))
                strongest = connection.pressure();
        return strongest;
    }

    public boolean hasPressure() {
        for (TubeConnection connection : interfaces.values())
            if (connection.hasPressure())
                return true;
        return false;
    }

    public void wipePressure() {
        for (TubeConnection connection : interfaces.values())
            connection.wipePressure();
    }

    public void addPressure(boolean inbound, float amount) {
        for (TubeConnection connection : interfaces.values())
            connection.addPressure(inbound, amount);
    }

    public void scalePressure(float factor) {
        for (TubeConnection connection : interfaces.values())
            connection.scale(factor);
    }


    public static TubeTransportBehaviour get(BlockGetter level, BlockPos pos) {
        return BlockEntityBehaviour.get(level, pos, TYPE);
    }

    @Override
    public BehaviourType<?> getType() {
        return TYPE;
    }
}
