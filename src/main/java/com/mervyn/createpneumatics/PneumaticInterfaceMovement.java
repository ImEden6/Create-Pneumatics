package com.mervyn.createpneumatics;

import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.phys.Vec3;

// The moving half. Facing a stationary interface that has vacuum behind it, it holds the
// contraption still and fills every tank on board, and lets it go once the tanks stop gaining.
public class PneumaticInterfaceMovement implements MovementBehaviour {
    private static final int SETTLE = 20;

    @Override
    public void tick(MovementContext context) {
        if (context.world.isClientSide || context.position == null)
            return;
        Direction local = context.state.getValue(PneumaticInterfaceBlock.FACING);
        Vec3 normal = context.rotation.apply(Vec3.atLowerCornerOf(local.getNormal()));
        Direction facing = Direction.getNearest(normal.x, normal.y, normal.z);
        BlockPos front = BlockPos.containing(context.position.add(normal));
        BlockState there = context.world.getBlockState(front);
        if (!(there.getBlock() instanceof PneumaticInterfaceBlock) || there.getValue(PneumaticInterfaceBlock.FACING) != facing.getOpposite()) {
            context.data.remove("Idle");
            context.stall = false;
            return;
        }
        float pull = PneumaticInterfaceBlock.pull(context.world, front, facing.getOpposite());
        boolean filling = false;
        for (StructureBlockInfo info : VacuumTankBlock.onBoard(context)) {
            CompoundTag tank = info.nbt();
            float charge = tank.getFloat("Charge");
            if (charge < pull - 0.01f) {
                tank.putFloat("Charge", Math.min(pull, charge + Config.INTERFACE_RATE.get().floatValue()));
                filling = true;
            }
        }
        int idle = filling ? 0 : context.data.getInt("Idle") + 1;
        context.data.putInt("Idle", idle);
        context.stall = idle < SETTLE;
    }
}
