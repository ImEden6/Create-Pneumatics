package com.mervyn.createpneumatics;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;

import dev.engine_room.flywheel.lib.transform.TransformStack;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

// Grabs on assembly, lets go through the pad's block entity on disassembly. Cargo sits in
// blockEntityData (what disassembly writes back) and its state in data (what the client gets),
// and neither changes while moving, so no packet is needed.
public class VacuumPadMovement implements MovementBehaviour {
    static float lift() {
        return Config.PAD_LIFT.get().floatValue();
    }

    @Override
    public void startMoving(MovementContext context) {
        Level level = context.world;
        if (level.isClientSide || context.blockEntityData.contains("Cargo") || VacuumPadBlockEntity.grip(context.blockEntityData) != VacuumPadBlockEntity.Grip.ON_START)
            return;
        Direction facing = context.state.getValue(VacuumPadBlock.FACING);
        BlockPos pad = context.contraption.anchor.offset(context.localPos);
        BlockPos front = pad.relative(facing);
        if (context.contraption.getBlocks().containsKey(context.localPos.relative(facing)) || !VacuumCargo.canLift(level, front))
            return;
        if (!docked(level, pad, facing) && !drawFromTank(context))
            return;
        grab(context, front);
        sync(context, context.data.getCompound("State"));
    }

    @Override
    public void tick(MovementContext context) {
        if (context.world.isClientSide || context.position == null || !context.blockEntityData.contains("Cargo"))
            return;
        float hold = context.blockEntityData.getFloat("Hold") - Config.PAD_LEAK.get().floatValue();
        for (StructureBlockInfo info : VacuumTankBlock.onBoard(context)) {
            if (hold >= 1)
                break;
            float charge = info.nbt().getFloat("Charge");
            float taken = Math.min(charge, 1 - hold);
            info.nbt().putFloat("Charge", charge - taken);
            hold += taken;
        }
        context.blockEntityData.putFloat("Hold", hold);
        if (hold < lift())
            release(context);
    }

    // vacuum's gone mid-move: the block comes off where the cup is now, turned the way the
    // contraption is turned, or falls out as an item if there's no room
    private static void release(MovementContext context) {
        Level level = context.world;
        CompoundTag cargo = context.blockEntityData.getCompound("Cargo");
        Vec3 normal = context.rotation.apply(Vec3.atLowerCornerOf(context.state.getValue(VacuumPadBlock.FACING).getNormal()));
        BlockPos front = BlockPos.containing(context.position.add(normal));
        Vec3 north = context.rotation.apply(new Vec3(0, 0, -1));
        Rotation turn = switch (Direction.getNearest(north.x, 0, north.z)) {
            case EAST -> Rotation.CLOCKWISE_90;
            case SOUTH -> Rotation.CLOCKWISE_180;
            case WEST -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
        cargo.put("State", NbtUtils.writeBlockState(VacuumCargo.state(level, cargo).rotate(turn)));
        if (VacuumCargo.place(level, front, cargo))
            level.playSound(null, front, SoundEvents.CANDLE_EXTINGUISH, SoundSource.BLOCKS, 1, 0.6f);
        else
            VacuumCargo.drop(level, front, cargo);
        context.blockEntityData.remove("Cargo");
        context.blockEntityData.remove("Hold");
        context.data.remove("State");
        sync(context, new CompoundTag());
    }

    private static void sync(MovementContext context, CompoundTag state) {
        if (context.contraption.entity != null)
            PacketDistributor.sendToPlayersTrackingEntity(context.contraption.entity, new PadCargo(context.contraption.entity.getId(), context.localPos, state));
    }

    // in contact mode an empty cup takes whatever it runs up against while moving
    @Override
    public void visitNewPosition(MovementContext context, BlockPos pos) {
        Level level = context.world;
        if (level.isClientSide || context.blockEntityData.contains("Cargo") || VacuumPadBlockEntity.grip(context.blockEntityData) != VacuumPadBlockEntity.Grip.ON_CONTACT)
            return;
        Vec3 normal = context.rotation.apply(Vec3.atLowerCornerOf(context.state.getValue(VacuumPadBlock.FACING).getNormal()));
        Direction facing = Direction.getNearest(normal.x, normal.y, normal.z);
        BlockPos front = pos.relative(facing);
        if (!VacuumCargo.canLift(level, front) || !docked(level, pos, facing) && !drawFromTank(context))
            return;
        grab(context, front);
        sync(context, context.data.getCompound("State"));
    }

    private static void grab(MovementContext context, BlockPos front) {
        Level level = context.world;
        CompoundTag cargo = VacuumCargo.capture(level, front);
        context.blockEntityData.put("Cargo", cargo);
        context.blockEntityData.putFloat("Hold", 1);
        context.data.put("State", cargo.getCompound("State"));
        level.playSound(null, front, SoundEvents.SLIME_BLOCK_PLACE, SoundSource.BLOCKS, 1, 0.5f);
    }

    // a tube against the pad when it sets off is as good as a tank
    private static boolean docked(Level level, BlockPos pad, Direction facing) {
        for (Direction side : Direction.values()) {
            if (side == facing)
                continue;
            BlockPos at = pad.relative(side);
            TubeTransportBehaviour tube = TubeTransportBehaviour.get(level, at);
            if (tube != null && PneumaticTubeBlock.isOpenAt(level.getBlockState(at), side.getOpposite())
                    && tube.get(side.getOpposite()).pressure() < -lift())
                return true;
        }
        return false;
    }

    private static boolean drawFromTank(MovementContext context) {
        CompoundTag best = null;
        for (StructureBlockInfo info : VacuumTankBlock.onBoard(context))
            if (info.nbt().getFloat("Charge") >= lift() && (best == null || info.nbt().getFloat("Charge") > best.getFloat("Charge")))
                best = info.nbt();
        if (best == null)
            return false;
        best.putFloat("Charge", best.getFloat("Charge") - Config.PAD_GRAB_COST.get().floatValue());
        return true;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void renderInContraption(MovementContext context, VirtualRenderWorld renderWorld, ContraptionMatrices matrices, MultiBufferSource buffer) {
        if (!context.data.contains("State"))
            return;
        BlockState cargo = VacuumCargo.state(context.world, context.data);
        BlockPos at = context.localPos.relative(context.state.getValue(VacuumPadBlock.FACING));
        PoseStack ms = matrices.getModelViewProjection();
        ms.pushPose();
        TransformStack.of(ms).translate(at);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(cargo, ms, buffer,
                context.position == null ? LevelRenderer.getLightColor(renderWorld, at)
                        : LevelRenderer.getLightColor(context.world, BlockPos.containing(context.position)), OverlayTexture.NO_OVERLAY);
        ms.popPose();
    }
}
