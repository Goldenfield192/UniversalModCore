package cam72cam.mod.item;

import cam72cam.mod.ModCore;
import cam72cam.mod.event.CommonEvents;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.oredict.OreIngredient;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Recipe registration */
public class Recipes {
    public static RecipeBuilder shapedRecipe(CustomItem item, int width, Fuzzy... ingredients) {
        return new ShapedRecipeBuilder(new ItemStack(item, 1), width, ingredients);
    }

    public static RecipeBuilder shapedRecipe(ItemStack item, int width, Fuzzy... ingredients) {
        return new ShapedRecipeBuilder(item, width, ingredients);
    }

    public static RecipeBuilder shapelessRecipe(CustomItem item, Fuzzy... ingredients) {
        return new ShapelessRecipeBuilder(new ItemStack(item, 1), ingredients);
    }

    public static RecipeBuilder shapelessRecipe(ItemStack item, Fuzzy... ingredients) {
        return new ShapelessRecipeBuilder(item, ingredients);
    }

    public static abstract class RecipeBuilder {
        private List<Fuzzy> dependencies = new ArrayList<>();
        private List<Fuzzy> conflicts = new ArrayList<>();

        public boolean checkCondition() {
            for (Fuzzy dependency : dependencies) {
                if (dependency.enumerate().isEmpty()) {
                    // Don't register recipe
                    return false;
                }
            }
            for (Fuzzy conflict : conflicts) {
                if (!conflict.enumerate().isEmpty()) {
                    // Don't register recipe
                    return false;
                }
            }
            return true;
        }

        public RecipeBuilder require(Fuzzy ...dependencies) {
            this.dependencies.addAll(Arrays.asList(dependencies));
            return this;
        }

        public RecipeBuilder conflicts(Fuzzy ...conflicts) {
            this.conflicts.addAll(Arrays.asList(conflicts));
            return this;
        }
    }

    public static class ShapedRecipeBuilder extends RecipeBuilder{
        private ShapedRecipeBuilder(ItemStack item, int width, Fuzzy... ingredients) {
            CommonEvents.Recipe.REGISTER.subscribe(() -> {
                if (!checkCondition()) {
                    return;
                }

                CraftingHelper.ShapedPrimer primer = new CraftingHelper.ShapedPrimer();
                primer.width = width;
                primer.height = ingredients.length / width;
                primer.mirrored = false;
                primer.input = NonNullList.withSize(primer.width * primer.height, Ingredient.EMPTY);

                for (int i = 0; i < ingredients.length; i++) {
                    if (ingredients[i] != null) {
                        primer.input.set(i, new OreIngredient(ingredients[i].toString()));
                    }
                }
                ShapedOreRecipe sor = new ShapedOreRecipe(new ResourceLocation(ModCore.MODID, "recipes"), item.internal, primer);
                sor.setRegistryName(item.internal.getItem().getRegistryName());
                ForgeRegistries.RECIPES.register(sor);
            });
        }
    }

    public static class ShapelessRecipeBuilder extends RecipeBuilder{
        private ShapelessRecipeBuilder(ItemStack item, Fuzzy... ingredients) {
            CommonEvents.Recipe.REGISTER.subscribe(() -> {
                CraftingHelper.ShapedPrimer primer = new CraftingHelper.ShapedPrimer();
                primer.mirrored = false;
                primer.input = NonNullList.withSize(primer.width * primer.height, Ingredient.EMPTY);

                for (int i = 0; i < ingredients.length; i++) {
                    if (ingredients[i] != null) {
                        primer.input.set(i, new OreIngredient(ingredients[i].toString()));
                    }
                }
                ShapelessOreRecipe sor = new ShapelessOreRecipe(new ResourceLocation(ModCore.MODID, "recipes"), item.internal, primer);
                sor.setRegistryName(item.internal.getItem().getRegistryName());
                ForgeRegistries.RECIPES.register(sor);
            });
        }
    }
}