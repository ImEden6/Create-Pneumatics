package com.mervyn.createpneumatics.client;

import com.mervyn.createpneumatics.CreatePneumatics;
import com.mervyn.createpneumatics.Pneumatics;

import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.resources.ResourceLocation;

public class PneumaticsPonder implements PonderPlugin {
    @Override
    public String getModId() {
        return CreatePneumatics.MODID;
    }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        helper.forComponents(Pneumatics.BLOWER.getId(), Pneumatics.TUBE.getId(), Pneumatics.GLASS_TUBE.getId(), Pneumatics.GAUGE.getId())
                .addStoryBoard("roots_blower", PneumaticScenes::blower);
        helper.forComponents(Pneumatics.VALVE.getId(), Pneumatics.GAUGE.getId())
                .addStoryBoard("relief_valve", PneumaticScenes::valve);
        helper.forComponents(Pneumatics.JAR.getId())
                .addStoryBoard("bell_jar", PneumaticScenes::jar);
        helper.forComponents(Pneumatics.TERMINAL.getId(), Pneumatics.DIVERTER.getId(), Pneumatics.GLASS_TUBE.getId())
                .addStoryBoard("transit", PneumaticScenes::transit);
        helper.forComponents(Pneumatics.PAD.getId(), Pneumatics.TANK.getId(), Pneumatics.INTERFACE.getId())
                .addStoryBoard("vacuum_pad", PneumaticScenes::pad);
    }
}
