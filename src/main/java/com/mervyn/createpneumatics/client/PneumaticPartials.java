package com.mervyn.createpneumatics.client;

import com.mervyn.createpneumatics.CreatePneumatics;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.resources.ResourceLocation;

public final class PneumaticPartials {
    public static final PartialModel LOBE = of("blower_lobe");
    public static final PartialModel NEEDLE = of("gauge_needle");

    private PneumaticPartials() {}

    private static PartialModel of(String name) {
        return PartialModel.of(ResourceLocation.fromNamespaceAndPath(CreatePneumatics.MODID, "block/partial/" + name));
    }

    public static void init() {}
}
