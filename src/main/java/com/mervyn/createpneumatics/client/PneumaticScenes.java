package com.mervyn.createpneumatics.client;

import java.util.List;

import com.mervyn.createpneumatics.BellJarBlockEntity;
import com.mervyn.createpneumatics.BellJarCycle;
import com.mervyn.createpneumatics.Pneumatics;
import com.mervyn.createpneumatics.PressureGaugeBlockEntity;
import com.mervyn.createpneumatics.ReliefValveBlock;
import com.mervyn.createpneumatics.TubePayloadEntity;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;

import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.EntityElement;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.phys.Vec3;

public class PneumaticScenes {
    public static void blower(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("roots_blower", "Moving air with the Roots Blower");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(5);

        BlockPos blower = util.grid().at(2, 1, 2);
        Selection kinetics = util.select().fromTo(2, 1, 1, 2, 1, 2);
        Selection front = util.select().position(4, 2, 2), back = util.select().position(0, 2, 2);
        scene.world().showSection(kinetics, Direction.DOWN);
        scene.idle(10);
        scene.overlay().showText(70)
                .text("The Roots Blower takes rotation and moves air through whatever is connected to its two flanges")
                .attachKeyFrame()
                .pointAt(util.vector().topOf(blower))
                .placeNearTarget();
        scene.idle(80);

        scene.world().showSection(util.select().fromTo(3, 1, 2, 4, 2, 2), Direction.WEST);
        scene.world().showSection(util.select().fromTo(0, 1, 2, 1, 2, 2), Direction.EAST);
        scene.idle(15);
        scene.world().setKineticSpeed(kinetics, 64);
        gauge(scene, front, .5f);
        gauge(scene, back, -.5f);
        scene.idle(20);

        scene.overlay().showText(70)
                .text("The front flange pushes air in, so pressure in those tubes goes up")
                .attachKeyFrame()
                .pointAt(util.vector().topOf(4, 2, 2))
                .placeNearTarget();
        scene.idle(80);
        scene.overlay().showText(70)
                .text("The back flange pulls air out and drops the pressure below zero")
                .pointAt(util.vector().topOf(0, 2, 2))
                .placeNearTarget();
        scene.idle(80);
        scene.overlay().showText(70)
                .text("Pressure Gauges show the reading of the tube they're stuck to")
                .pointAt(util.vector().topOf(0, 2, 2))
                .placeNearTarget();
        scene.idle(80);

        scene.world().setKineticSpeed(kinetics, -64);
        gauge(scene, front, -.5f);
        gauge(scene, back, .5f);
        scene.overlay().showText(70)
                .text("Reverse the rotation and the two sides swap")
                .attachKeyFrame()
                .pointAt(util.vector().topOf(blower))
                .placeNearTarget();
        scene.idle(80);

        scene.world().setKineticSpeed(kinetics, -128);
        gauge(scene, front, -1);
        gauge(scene, back, 1);
        scene.overlay().showText(70)
                .text("Spin it faster for stronger pressure")
                .pointAt(util.vector().topOf(blower))
                .placeNearTarget();
        scene.idle(80);
        scene.markAsFinished();
    }

    public static void valve(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("relief_valve", "Venting a network with the Relief Valve");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(5);

        BlockPos valve = util.grid().at(3, 2, 2), lever = util.grid().at(4, 2, 2);
        Selection kinetics = util.select().fromTo(1, 1, 1, 1, 1, 2), meter = util.select().position(2, 2, 2);
        scene.world().showSection(kinetics, Direction.DOWN);
        scene.idle(5);
        scene.world().showSection(util.select().fromTo(2, 1, 2, 3, 1, 2).add(meter), Direction.WEST);
        scene.idle(10);
        scene.world().setKineticSpeed(kinetics, 64);
        gauge(scene, meter, .6f);
        scene.idle(10);
        scene.overlay().showText(60)
                .text("This network is sealed, so the blower keeps it under pressure")
                .pointAt(util.vector().topOf(2, 2, 2))
                .placeNearTarget();
        scene.idle(70);

        scene.world().showSection(util.select().position(valve), Direction.DOWN);
        scene.idle(10);
        scene.overlay().showControls(util.vector().topOf(valve), Pointing.DOWN, 30).rightClick();
        scene.idle(7);
        scene.world().modifyBlock(valve, s -> s.setValue(ReliefValveBlock.OPEN, true), false);
        gauge(scene, meter, 0);
        scene.overlay().showText(80)
                .text("Right-click a Relief Valve to open it. The whole network loses its air while it stays open")
                .attachKeyFrame()
                .pointAt(util.vector().topOf(valve))
                .placeNearTarget();
        scene.idle(90);
        scene.overlay().showControls(util.vector().topOf(valve), Pointing.DOWN, 30).rightClick();
        scene.idle(7);
        scene.world().modifyBlock(valve, s -> s.setValue(ReliefValveBlock.OPEN, false), false);
        gauge(scene, meter, .6f);
        scene.idle(30);

        scene.world().showSection(util.select().fromTo(4, 1, 2, 4, 2, 2), Direction.DOWN);
        scene.idle(15);
        scene.world().modifyBlock(lever, s -> s.setValue(LeverBlock.POWERED, true), false);
        scene.world().modifyBlock(valve, s -> s.setValue(ReliefValveBlock.OPEN, true).setValue(ReliefValveBlock.POWERED, true), false);
        gauge(scene, meter, 0);
        scene.overlay().showText(70)
                .text("A redstone signal holds it open until the signal stops")
                .attachKeyFrame()
                .pointAt(util.vector().topOf(lever))
                .placeNearTarget();
        scene.idle(80);
        scene.world().modifyBlock(lever, s -> s.setValue(LeverBlock.POWERED, false), false);
        scene.world().modifyBlock(valve, s -> s.setValue(ReliefValveBlock.OPEN, false).setValue(ReliefValveBlock.POWERED, false), false);
        gauge(scene, meter, .6f);
        scene.idle(30);
        scene.markAsFinished();
    }

    public static void jar(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("bell_jar", "Processing under vacuum with the Bell Jar");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(5);

        BlockPos depot = util.grid().at(1, 1, 2), jar = util.grid().at(1, 2, 2);
        Selection kinetics = util.select().fromTo(4, 1, 1, 4, 1, 2), meter = util.select().position(2, 3, 2);
        ItemStack stew = new ItemStack(Items.MUSHROOM_STEW);
        scene.world().showSection(util.select().position(depot), Direction.DOWN);
        scene.idle(5);
        scene.world().showSection(util.select().position(jar), Direction.DOWN);
        scene.idle(10);
        scene.overlay().showText(70)
                .text("A Bell Jar sits on a Depot or Basin and works on whatever is inside it")
                .attachKeyFrame()
                .pointAt(util.vector().blockSurface(jar, Direction.WEST))
                .placeNearTarget();
        scene.idle(80);
        scene.overlay().showControls(util.vector().blockSurface(depot, Direction.WEST), Pointing.RIGHT, 30).rightClick().withItem(stew);
        scene.idle(7);
        scene.world().createItemOnBeltLike(depot, Direction.UP, stew);
        scene.idle(30);

        scene.world().showSection(util.select().fromTo(2, 1, 2, 3, 3, 2), Direction.WEST);
        scene.idle(5);
        scene.world().showSection(kinetics, Direction.DOWN);
        scene.idle(10);
        scene.world().setKineticSpeed(kinetics, 64);
        gauge(scene, meter, -.8f);
        scene.world().modifyBlockEntity(jar, BellJarBlockEntity.class, be -> be.cycle().pressure = -.8f);
        scene.overlay().showText(80)
                .text("Hook it up to a tube that pulls air out. Nothing happens until the jar is down to a vacuum")
                .attachKeyFrame()
                .pointAt(util.vector().topOf(2, 3, 2))
                .placeNearTarget();
        scene.idle(90);

        scene.world().removeItemsFromBelt(depot);
        scene.world().modifyBlockEntity(jar, BellJarBlockEntity.class, be -> {
            be.cycle().phase = BellJarCycle.Phase.RUNNING;
            be.cycle().batch.add(stew.copy());
        });
        scene.overlay().showText(70)
                .text("Then it pulls the items in and gets to work")
                .pointAt(util.vector().blockSurface(jar, Direction.WEST))
                .placeNearTarget();
        scene.idle(100);

        scene.world().modifyBlockEntity(jar, BellJarBlockEntity.class, be -> {
            be.cycle().phase = BellJarCycle.Phase.IDLE;
            be.cycle().batch.clear();
        });
        scene.world().createItemOnBeltLike(depot, Direction.UP, new ItemStack(Pneumatics.RATION_PACK.get()));
        scene.idle(10);
        scene.overlay().showText(70)
                .text("Mushroom Stew comes out as a Ration Pack, and you get the bowl back")
                .attachKeyFrame()
                .pointAt(util.vector().blockSurface(depot, Direction.WEST))
                .placeNearTarget();
        scene.idle(80);
        scene.overlay().showText(80)
                .text("Some recipes need heat. Put those on a Basin over a Blaze Burner")
                .pointAt(util.vector().blockSurface(depot, Direction.WEST))
                .placeNearTarget();
        scene.idle(90);
        scene.markAsFinished();
    }

    public static void transit(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("transit", "Sending items with Pneumatic Terminals");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(5);

        BlockPos sender = util.grid().at(0, 1, 2), diverter = util.grid().at(2, 1, 2);
        Selection kinetics = util.select().fromTo(0, 1, 4, 1, 1, 4);
        ItemStack cobble = new ItemStack(Items.COBBLESTONE, 16), diamond = new ItemStack(Items.DIAMOND, 4);
        scene.world().showSection(util.select().position(sender), Direction.DOWN);
        scene.idle(5);
        scene.world().showSection(util.select().fromTo(1, 1, 2, 4, 1, 2), Direction.WEST);
        scene.idle(5);
        scene.world().showSection(util.select().fromTo(0, 1, 3, 1, 1, 4), Direction.NORTH);
        scene.idle(10);
        scene.world().setKineticSpeed(kinetics, 64);
        scene.overlay().showText(70)
                .text("Pneumatic Terminals load items into tubes and catch them at the other end")
                .attachKeyFrame()
                .pointAt(util.vector().topOf(sender))
                .placeNearTarget();
        scene.idle(80);

        send(scene, util, sender, cobble);
        ride(scene, cobble, util.grid().at(1, 1, 2), diverter, util.grid().at(3, 1, 2), util.grid().at(4, 1, 2));
        scene.overlay().showText(80)
                .text("Items always ride toward lower pressure, away from a blower that pushes or toward one that pulls")
                .pointAt(util.vector().topOf(1, 1, 4))
                .placeNearTarget();
        scene.idle(90);

        scene.world().showSection(util.select().fromTo(2, 1, 0, 2, 1, 1), Direction.SOUTH);
        scene.idle(10);
        scene.overlay().showText(70)
                .text("An Air Diverter sends anything matching its filter out of its side")
                .attachKeyFrame()
                .pointAt(util.vector().topOf(diverter))
                .placeNearTarget();
        scene.idle(80);
        send(scene, util, sender, diamond);
        ride(scene, diamond, util.grid().at(1, 1, 2), diverter, util.grid().at(2, 1, 1), util.grid().at(2, 1, 0));
        scene.idle(10);
        send(scene, util, sender, cobble);
        ride(scene, cobble, util.grid().at(1, 1, 2), diverter, util.grid().at(3, 1, 2), util.grid().at(4, 1, 2));
        scene.overlay().showText(70)
                .text("Everything else goes straight through. A redstone signal switches the diverter off")
                .pointAt(util.vector().topOf(diverter))
                .placeNearTarget();
        scene.idle(80);
        scene.markAsFinished();
    }

    public static void pad(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("vacuum_pad", "Picking up blocks with the Vacuum Pad");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(5);

        BlockPos pad = util.grid().at(1, 2, 2);
        ElementLink<WorldSectionElement> cargo = scene.world().showIndependentSection(util.select().position(1, 1, 2), Direction.DOWN);
        scene.idle(5);
        ElementLink<WorldSectionElement> rig = scene.world().showIndependentSection(util.select().fromTo(1, 2, 2, 2, 3, 2), Direction.DOWN);
        scene.idle(10);
        scene.overlay().showText(80)
                .text("Vacuum Pads go on contraptions. When the contraption starts moving, the pad grabs the block it faces")
                .attachKeyFrame()
                .pointAt(util.vector().blockSurface(pad, Direction.WEST))
                .placeNearTarget();
        scene.idle(90);

        for (ElementLink<WorldSectionElement> part : List.of(cargo, rig))
            scene.world().moveSection(part, new Vec3(0, 1, 0), 15);
        scene.idle(15);
        for (ElementLink<WorldSectionElement> part : List.of(cargo, rig))
            scene.world().moveSection(part, new Vec3(0, 0, 2), 30);
        scene.idle(30);
        for (ElementLink<WorldSectionElement> part : List.of(cargo, rig))
            scene.world().moveSection(part, new Vec3(0, -1, 0), 15);
        scene.idle(20);
        scene.overlay().showText(60)
                .text("When it stops, the block gets put back down")
                .pointAt(util.vector().blockSurface(pad.south(2), Direction.WEST))
                .placeNearTarget();
        scene.idle(70);

        scene.world().moveSection(rig, new Vec3(0, 1, 0), 15);
        scene.idle(15);
        scene.world().moveSection(rig, new Vec3(0, 0, -2), 30);
        scene.idle(30);
        scene.world().moveSection(rig, new Vec3(0, -1, 0), 15);
        scene.idle(20);

        scene.overlay().showText(70)
                .text("Every grab uses up vacuum from a Vacuum Tank on the same contraption")
                .attachKeyFrame()
                .pointAt(util.vector().blockSurface(util.grid().at(1, 3, 2), Direction.WEST))
                .placeNearTarget();
        scene.idle(80);
        scene.world().showSection(util.select().fromTo(3, 1, 2, 4, 3, 3), Direction.DOWN);
        scene.idle(10);
        scene.world().setKineticSpeed(util.select().fromTo(3, 1, 3, 4, 1, 3), 64);
        scene.overlay().showText(90)
                .text("Park the contraption with a Pneumatic Interface facing one that a blower pulls on, and the tank fills back up while it waits")
                .attachKeyFrame()
                .pointAt(util.vector().topOf(util.grid().at(3, 3, 2)))
                .placeNearTarget();
        scene.idle(100);
        scene.markAsFinished();
    }

    private static void gauge(CreateSceneBuilder scene, Selection at, float pressure) {
        scene.world().modifyBlockEntityNBT(at, PressureGaugeBlockEntity.class, nbt -> nbt.putFloat("Pressure", pressure));
    }

    private static void send(CreateSceneBuilder scene, SceneBuildingUtil util, BlockPos terminal, ItemStack stack) {
        scene.overlay().showControls(util.vector().topOf(terminal), Pointing.DOWN, 20).rightClick().withItem(stack);
        scene.idle(25);
    }

    // payloads in a ponder level never tick their server side, so the scene walks them tube to tube itself
    private static void ride(CreateSceneBuilder scene, ItemStack stack, BlockPos... path) {
        ElementLink<EntityElement> payload = scene.world().createEntity(level -> {
            TubePayloadEntity entity = new TubePayloadEntity(Pneumatics.PAYLOAD.get(), level);
            entity.setItem(stack.copy());
            entity.setPos(Vec3.atCenterOf(path[0]));
            entity.setOldPosAndRot();
            return entity;
        });
        for (int i = 1; i < path.length; i++)
            for (int step = 1; step <= 4; step++) {
                Vec3 at = Vec3.atCenterOf(path[i - 1]).lerp(Vec3.atCenterOf(path[i]), step / 4.0);
                scene.world().modifyEntity(payload, entity -> entity.setPos(at));
                scene.idle(1);
            }
        scene.world().modifyEntity(payload, Entity::discard);
    }
}
