package com.mervyn.createpneumatics.compat;

import com.mervyn.createpneumatics.CreatePneumatics;
import com.mervyn.createpneumatics.BellJarBlock;
import com.mervyn.createpneumatics.Pneumatics;
import com.mervyn.createpneumatics.VacuumRecipe;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.compat.jei.category.CreateRecipeCategory;
import com.simibubi.create.compat.jei.category.animations.AnimatedKinetics;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.recipe.HeatCondition;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.foundation.gui.AllGuiTextures;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

// laid out like Create's basin and fan pages: inputs left, the jar on its basin in the middle, results right
public class VacuumCategory extends AbstractRecipeCategory<RecipeHolder<VacuumRecipe>> {
    public static final RecipeType<RecipeHolder<VacuumRecipe>> TYPE = RecipeType.createRecipeHolderType(
            ResourceLocation.fromNamespaceAndPath(CreatePneumatics.MODID, "vacuum_processing"));
    private static final int SCALE = 22;

    public VacuumCategory(IGuiHelper gui) {
        super(TYPE, Component.translatable("createpneumatics.jei.vacuum"), gui.createDrawableItemLike(Pneumatics.JAR_ITEM.get()), 177, 113);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<VacuumRecipe> holder, IFocusGroup focuses) {
        VacuumRecipe recipe = holder.value();
        int slot = 0;
        for (Ingredient ingredient : recipe.getIngredients())
            builder.addSlot(RecipeIngredientRole.INPUT, x(slot, 4), y(slot++))
                    .setBackground(CreateRecipeCategory.getRenderedSlot(), -1, -1)
                    .addIngredients(ingredient);
        for (SizedFluidIngredient fluid : recipe.getFluidIngredients())
            CreateRecipeCategory.addFluidSlot(builder, x(slot, 4), y(slot++), fluid);

        int out = 0;
        for (ProcessingOutput output : recipe.getRollableResults()) {
            float chance = output.getChance();
            builder.addSlot(RecipeIngredientRole.OUTPUT, x(out, 116), y(out++))
                    .setBackground(CreateRecipeCategory.getRenderedSlot(output), -1, -1)
                    .addItemStack(output.getStack())
                    .addRichTooltipCallback((view, tooltip) -> {
                        if (chance < 1)
                            tooltip.add(Component.translatable("createpneumatics.jei.vacuum.chance", chance < 0.01 ? "<1" : Math.round(chance * 100))
                                    .withStyle(ChatFormatting.GOLD));
                    });
        }
        for (FluidStack fluid : recipe.getFluidResults())
            CreateRecipeCategory.addFluidSlot(builder, x(out, 116), y(out++), fluid);
    }

    private static int x(int slot, int left) {
        return left + 1 + slot % 3 * 19;
    }

    private static int y(int slot) {
        return 67 - slot / 3 * 19;
    }

    @Override
    public void draw(RecipeHolder<VacuumRecipe> holder, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        HeatCondition heat = holder.value().getRequiredHeat();
        boolean heated = heat != HeatCondition.NONE;

        (heated ? AllGuiTextures.JEI_LIGHT : AllGuiTextures.JEI_SHADOW).render(graphics, 62, 59);
        AllGuiTextures.JEI_LONG_ARROW.render(graphics, 42, 71);

        PoseStack ms = graphics.pose();
        ms.pushPose();
        ms.translate(74, 18, 200);
        ms.mulPose(Axis.XP.rotationDegrees(-15.5f));
        ms.mulPose(Axis.YP.rotationDegrees(22.5f));
        AnimatedKinetics.defaultBlockElement(Pneumatics.JAR.get().defaultBlockState().setValue(BellJarBlock.FACING, Direction.SOUTH))
                .scale(SCALE)
                .render(graphics);
        AnimatedKinetics.defaultBlockElement(AllBlocks.BASIN.getDefaultState())
                .atLocal(0, 1, 0)
                .scale(SCALE)
                .render(graphics);
        if (heated)
            AnimatedKinetics.defaultBlockElement(AllBlocks.BLAZE_BURNER.getDefaultState()
                            .setValue(BlazeBurnerBlock.HEAT_LEVEL, heat.visualizeAsBlazeBurner()))
                    .atLocal(0, 2, 0)
                    .scale(SCALE)
                    .render(graphics);
        ms.popPose();

        (heated ? AllGuiTextures.JEI_HEAT_BAR : AllGuiTextures.JEI_NO_HEAT_BAR).render(graphics, 4, 91);
        Component line = Component.translatable("createpneumatics.jei.vacuum.needs").append(" ")
                .append(Component.translatable("createpneumatics.jei.vacuum." + (heated ? heat.getSerializedName() : "cold")));
        graphics.drawString(Minecraft.getInstance().font, line, 9, 97, heated ? heat.getColor() : 0x4A6E8A, false);
    }
}
