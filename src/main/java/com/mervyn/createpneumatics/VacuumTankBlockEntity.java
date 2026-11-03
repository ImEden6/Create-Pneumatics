package com.mervyn.createpneumatics;

import java.util.List;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

// Stores vacuum for pads on a contraption. Fills from any tube that pulls on it while it sits in
// the world; on a contraption the pads draw Charge straight out of its captured tag.
public class VacuumTankBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
    private static final float FILL_RATE = 0.01f;

    float charge;
    private float sentCharge;

    public VacuumTankBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

    @Override
    public void tick() {
        super.tick();
        if (level.isClientSide)
            return;
        float pull = 0;
        for (Direction side : Direction.values()) {
            BlockPos at = worldPosition.relative(side);
            TubeTransportBehaviour tube = TubeTransportBehaviour.get(level, at);
            if (tube != null && PneumaticTubeBlock.isOpenAt(level.getBlockState(at), side.getOpposite()))
                pull = Math.max(pull, -tube.get(side.getOpposite()).pressure());
        }
        if (pull > charge)
            charge = Math.min(pull, charge + FILL_RATE);
        if (Math.abs(charge - sentCharge) > 0.02f) {
            sentCharge = charge;
            sendData();
            setChanged();
        }
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        tooltip.add(Component.literal("    ").append(Component.translatable("block.createpneumatics.vacuum_tank")));
        tooltip.add(Component.literal("    ").append(Component.translatable("createpneumatics.tank.charge", Math.round(charge * 100))
                .withStyle(ChatFormatting.AQUA)));
        return true;
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putFloat("Charge", charge);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        charge = tag.getFloat("Charge");
    }
}
