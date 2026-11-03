package com.mervyn.createpneumatics;

import net.createmod.catnip.data.Couple;
import net.minecraft.core.Direction;

// One face of a pneumatic tube. Pressure is stored as Create's PipeConnection stores it:
// an inbound and an outward component, so a run that is fed from both ends keeps a direction.
//
// v1 writes the same magnitude to every face of a tube; that makes the gauge work wherever it is
// mounted and leaves the signed gradient across neighbouring tubes, which is what a payload will
// follow once transit lands. Per-face asymmetry can come later without changing this class.
public class TubeConnection {
    public final Direction side;
    private final Couple<Float> pressure = Couple.create(0f, 0f);

    public TubeConnection(Direction side) {
        this.side = side;
    }

    public void addPressure(boolean inbound, float amount) {
        if (inbound)
            pressure.setFirst(pressure.getFirst() + amount);
        else
            pressure.setSecond(pressure.getSecond() + amount);
    }

    public void scale(float factor) {
        pressure.setFirst(pressure.getFirst() * factor);
        pressure.setSecond(pressure.getSecond() * factor);
    }

    public float pressure() {
        return pressure.getFirst() - pressure.getSecond();
    }

    public float comparePressure() {
        return pressure.getSecond() - pressure.getFirst();
    }

    public boolean hasPressure() {
        return pressure.getFirst() != 0 || pressure.getSecond() != 0;
    }

    public void wipePressure() {
        pressure.setFirst(0f);
        pressure.setSecond(0f);
    }
}
