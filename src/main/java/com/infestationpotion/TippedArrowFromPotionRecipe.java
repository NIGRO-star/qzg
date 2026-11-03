package com.infestationpotion;

import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.SpecialRecipe;
import net.minecraft.potion.PotionUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

/**
 * Crafting table: 1 regular (drinkable) potion + any number of arrows (1 to 8) = the same number of
 * tipped arrows with that potion's effect. No lingering potion / dragon's breath needed.
 * Works with every potion that has an effect (vanilla and modded), including Infestation.
 */
public class TippedArrowFromPotionRecipe extends SpecialRecipe {

    public TippedArrowFromPotionRecipe(ResourceLocation id) {
        super(id);
    }

    @Override
    public boolean matches(CraftingInventory inv, World world) {
        int arrows = 0;
        int potions = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.getItem() == Items.ARROW) {
                arrows++;
            } else if (stack.getItem() == Items.POTION && !PotionUtils.getMobEffects(stack).isEmpty()) {
                potions++;
            } else {
                return false; // anything else in the grid cancels the recipe
            }
        }
        return potions == 1 && arrows >= 1;
    }

    @Override
    public ItemStack assemble(CraftingInventory inv) {
        int arrows = 0;
        ItemStack potion = ItemStack.EMPTY;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.getItem() == Items.ARROW) {
                arrows++;
            } else if (stack.getItem() == Items.POTION) {
                potion = stack;
            }
        }
        if (potion.isEmpty() || arrows == 0) {
            return ItemStack.EMPTY;
        }
        ItemStack result = new ItemStack(Items.TIPPED_ARROW, arrows);
        PotionUtils.setPotion(result, PotionUtils.getPotion(potion));
        PotionUtils.setCustomEffects(result, PotionUtils.getCustomEffects(potion));
        return result;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public IRecipeSerializer<?> getSerializer() {
        return ModRecipes.TIPPED_ARROW.get();
    }
}
