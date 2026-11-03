package com.mervyn.createpneumatics;

import java.util.List;

import com.simibubi.create.api.contraption.transformable.TransformableBlockEntity;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import com.simibubi.create.foundation.gui.AllIcons;

import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

// Only holds cargo for the moment between a contraption coming apart and the block going back
// down. The contraption writes the cargo into this tag, so it arrives here on disassembly.
public class VacuumPadBlockEntity extends SmartBlockEntity implements TransformableBlockEntity {
    private static final int GIVE_UP = 100;

    private CompoundTag cargo = new CompoundTag();
    ScrollOptionBehaviour<Grip> grip;
    private int tries;

    public VacuumPadBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        grip = new ScrollOptionBehaviour<>(Grip.class, Component.translatable("createpneumatics.vacuum_pad.grip"), this, new Back());
        behaviours.add(grip);
    }

    // the scroll option's value as the contraption captured it
    static Grip grip(CompoundTag data) {
        Grip[] grips = Grip.values();
        return grips[Math.clamp(data.getInt("ScrollValue"), 0, grips.length - 1)];
    }

    enum Grip implements INamedIconOptions {
        ON_START(AllIcons.I_PLAY),
        ON_CONTACT(AllIcons.I_TARGET);

        private final AllIcons icon;

        Grip(AllIcons icon) {
            this.icon = icon;
        }

        @Override
        public AllIcons getIcon() {
            return icon;
        }

        @Override
        public String getTranslationKey() {
            return "createpneumatics.vacuum_pad.grip." + name().toLowerCase();
        }
    }

    // on the back plate, the one face that never has the cup on it
    private static class Back extends ValueBoxTransform.Sided {
        @Override
        protected Vec3 getSouthLocation() {
            return VecHelper.voxelSpace(8, 8, 16.05);
        }

        @Override
        protected boolean isSideActive(BlockState state, Direction direction) {
            return direction == state.getValue(VacuumPadBlock.FACING).getOpposite();
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level.isClientSide || cargo.isEmpty())
            return;
        BlockPos target = worldPosition.relative(getBlockState().getValue(VacuumPadBlock.FACING));
        if (VacuumCargo.place(level, target, cargo)) {
            level.playSound(null, target, Pneumatics.PAD_RELEASE.get(), SoundSource.BLOCKS, 1, 1);
        } else if (++tries < GIVE_UP) {
            return;
        } else {
            VacuumCargo.drop(level, target, cargo);
        }
        cargo = new CompoundTag();
        tries = 0;
        setChanged();
    }

    // the contraption turned while carrying it, so the cargo turns too
    @Override
    public void transform(BlockEntity be, StructureTransform transform) {
        if (cargo.isEmpty())
            return;
        cargo.put("State", NbtUtils.writeBlockState(transform.apply(VacuumCargo.state(level, cargo))));
        setChanged();
    }

    void dropCargo() {
        if (cargo.isEmpty())
            return;
        VacuumCargo.drop(level, worldPosition, cargo);
        cargo = new CompoundTag();
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        if (!cargo.isEmpty())
            tag.put("Cargo", cargo);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        cargo = tag.getCompound("Cargo");
    }
}
