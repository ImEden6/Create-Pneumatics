package com.mervyn.createpneumatics;

import java.util.ArrayList;
import java.util.List;

import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;

public class VacuumTankBlock extends Block implements IBE<VacuumTankBlockEntity> {
    public VacuumTankBlock(Properties properties) {
        super(properties);
    }

    // a contraption's blocks don't change while it moves, so each actor looks its tanks up once
    // and keeps them in its own temporaryData instead of walking every block each tick
    @SuppressWarnings("unchecked")
    static List<StructureBlockInfo> onBoard(MovementContext context) {
        if (context.temporaryData instanceof List<?> cached)
            return (List<StructureBlockInfo>) cached;
        List<StructureBlockInfo> tanks = new ArrayList<>();
        for (StructureBlockInfo info : context.contraption.getBlocks().values())
            if (info.state().getBlock() instanceof VacuumTankBlock && info.nbt() != null)
                tanks.add(info);
        context.temporaryData = tanks;
        return tanks;
    }

    @Override
    public Class<VacuumTankBlockEntity> getBlockEntityClass() {
        return VacuumTankBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends VacuumTankBlockEntity> getBlockEntityType() {
        return Pneumatics.TANK_BE.get();
    }
}
