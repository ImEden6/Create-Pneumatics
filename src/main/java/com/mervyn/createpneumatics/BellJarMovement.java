package com.mervyn.createpneumatics;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.neoforged.neoforge.network.PacketDistributor;

// The jar riding on a contraption. It runs the same BellJarCycle the block entity does, kept in
// the jar's own block entity tag on the contraption, so saving the contraption saves the batch and
// taking it apart puts the jar down mid-cycle exactly where it was. No tube reaches it while moving;
// a Vacuum Tank on board keeps pumping it instead, so a batch started on the ground finishes on the
// move. Starting a new batch still needs the jar to be standing on its basin in the world.
public class BellJarMovement implements MovementBehaviour {
    // pumping a jar down costs a tank this share of the vacuum the jar gained, so one full tank is
    // good for a few jars
    private static final float TANK_SHARE = 0.25f;

    @Override
    public void tick(MovementContext context) {
        if (context.world.isClientSide || stacked(context))
            return;
        var registries = context.world.registryAccess();
        BellJarCycle cycle = BellJarCycle.read(context.blockEntityData.getCompound("Cycle"), registries);
        if (!cycle.busy() && cycle.pressure == 0)
            return;

        float before = cycle.pressure;
        StructureBlockInfo tank = tank(context, -before);
        cycle.tickPressure(tank == null ? Float.NaN : -tank.nbt().getFloat("Charge"), height(context));
        if (tank != null && cycle.pressure < before) {
            float charge = tank.nbt().getFloat("Charge");
            tank.nbt().putFloat("Charge", Math.max(0, charge - (before - cycle.pressure) * TANK_SHARE));
        }
        cycle.tick(recipe(context, cycle.recipe), context.world.random);
        CompoundTag tag = cycle.write(registries);
        context.blockEntityData.put("Cycle", tag);
        sync(context, cycle, tag);
    }

    // put down with a batch still in it: vent, keep the certain outputs, lose the bonus
    @Override
    public void stopMoving(MovementContext context) {
        if (context.world.isClientSide)
            return;
        var registries = context.world.registryAccess();
        BellJarCycle cycle = BellJarCycle.read(context.blockEntityData.getCompound("Cycle"), registries);
        if (!cycle.busy())
            return;
        cycle.vent(recipe(context, cycle.recipe), context.world.random);
        context.blockEntityData.put("Cycle", cycle.write(registries));
    }

    private static boolean stacked(MovementContext context) {
        StructureBlockInfo below = context.contraption.getBlocks().get(context.localPos.below());
        return below != null && below.state().getBlock() instanceof BellJarBlock;
    }

    private static int height(MovementContext context) {
        int height = 1;
        while (height < BellJarBlockEntity.MAX_HEIGHT) {
            StructureBlockInfo above = context.contraption.getBlocks().get(context.localPos.above(height));
            if (above == null || !(above.state().getBlock() instanceof BellJarBlock))
                break;
            height++;
        }
        return height;
    }

    // the fullest tank, but only one that can still pull harder than the jar already is; a weaker
    // tank would let air back in
    @Nullable
    private static StructureBlockInfo tank(MovementContext context, float vacuum) {
        StructureBlockInfo best = null;
        for (StructureBlockInfo info : VacuumTankBlock.onBoard(context))
            if (info.nbt().getFloat("Charge") > vacuum && (best == null || info.nbt().getFloat("Charge") > best.nbt().getFloat("Charge")))
                best = info;
        return best;
    }

    private static void sync(MovementContext context, BellJarCycle cycle, CompoundTag tag) {
        if (context.contraption.entity == null)
            return;
        String phase = cycle.phase.name();
        if (phase.equals(context.data.getString("SentPhase")) && Math.abs(cycle.pressure - context.data.getFloat("SentPressure")) <= 0.02f)
            return;
        context.data.putString("SentPhase", phase);
        context.data.putFloat("SentPressure", cycle.pressure);
        PacketDistributor.sendToPlayersTrackingEntity(context.contraption.entity,
                new JarCycleSync(context.contraption.entity.getId(), context.localPos, tag));
    }

    private static VacuumRecipe recipe(MovementContext context, ResourceLocation id) {
        if (id == null)
            return null;
        return context.world.getRecipeManager().byKey(id)
                .map(RecipeHolder::value)
                .filter(VacuumRecipe.class::isInstance)
                .map(VacuumRecipe.class::cast)
                .orElse(null);
    }
}
