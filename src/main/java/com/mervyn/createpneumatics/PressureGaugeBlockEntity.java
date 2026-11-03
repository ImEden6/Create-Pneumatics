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
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class PressureGaugeBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
    private float pressure;
    private float needle, previousNeedle, velocity;

    public PressureGaugeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setLazyTickRate(5);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

    public float pressure() {
        return pressure;
    }

    public float needle(float partialTicks) {
        return Mth.lerp(partialTicks, previousNeedle, needle);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level.isClientSide)
            return;
        previousNeedle = needle;
        float target = Mth.clamp(pressure, -1f, 1f);
        // a little overshoot so it swings like a real needle instead of sliding
        velocity = velocity * 0.7f + (target - needle) * 0.25f;
        needle += velocity;
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        if (level.isClientSide)
            return;
        Direction facing = getBlockState().getValue(TubeMountBlock.FACING);
        TubeTransportBehaviour tube = TubeTransportBehaviour.get(level, worldPosition.relative(facing));
        float reading = tube == null ? 0 : tube.get(facing.getOpposite()).pressure();
        if (Math.abs(reading - pressure) < 0.005f)
            return;
        pressure = reading;
        sendData();
        level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
    }

    public MutableComponent reading() {
        return Component.translatable("createpneumatics.gauge.reading", Math.round(pressure * 100));
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        tooltip.add(Component.literal("    ").append(Component.translatable("createpneumatics.gauge")));
        tooltip.add(Component.literal("    ").append(reading().withStyle(pressure < 0 ? ChatFormatting.AQUA : ChatFormatting.GOLD)));
        return true;
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        if (clientPacket)
            tag.putFloat("Pressure", pressure);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        if (clientPacket)
            pressure = tag.getFloat("Pressure");
    }
}
