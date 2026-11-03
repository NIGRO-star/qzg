package com.infestationpotion;

import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.SpecialRecipeSerializer;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModRecipes {
    public static final DeferredRegister<IRecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, InfestationPotionMod.MODID);

    public static final RegistryObject<SpecialRecipeSerializer<TippedArrowFromPotionRecipe>> TIPPED_ARROW =
            SERIALIZERS.register("tipped_arrow_from_potion",
                    () -> new SpecialRecipeSerializer<>(TippedArrowFromPotionRecipe::new));
}
