package cam72cam.mod.item;


import net.minecraft.advancements.critereon.*;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.crafting.ConditionalRecipe;
import net.minecraftforge.common.crafting.conditions.ItemExistsCondition;
import net.minecraftforge.common.crafting.conditions.NotCondition;
import net.minecraftforge.common.crafting.conditions.TagEmptyCondition;
import net.minecraftforge.registries.ForgeRegistries;
import cam72cam.mod.event.CommonEvents;
import cam72cam.mod.event.platform.RegisterCraftingRecipeEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Recipe registration */
public class Recipes {
    public static ShapedRecipeBuilder shapedRecipe(CustomItem item, int width, Fuzzy... ingredients) {
        return new ShapedRecipeBuilder(new ItemStack(item, 1), width, ingredients);
    }

    public static ShapedRecipeBuilder shapedRecipe(ItemStack item, int width, Fuzzy... ingredients) {
        return new ShapedRecipeBuilder(item, width, ingredients);
    }

    public static class ShapedRecipeBuilder {
        private final List<Fuzzy> dependencies = new ArrayList<>();
        private final List<Fuzzy> conflicts = new ArrayList<>();

        private final ItemStack target;
        private final int width;
        private final List<Fuzzy> ingredients;

        private ShapedRecipeBuilder(ItemStack item, int width, Fuzzy... ingredients) {
            this.target = item;
            this.width = width;
            this.ingredients = new ArrayList<>(Arrays.asList(ingredients));

            CommonEvents.Recipe.REGISTER.subscribe(this::register);
        }

        public ShapedRecipeBuilder require(Fuzzy ...dependencies) {
            this.dependencies.addAll(Arrays.asList(dependencies));
            return this;
        }

        public ShapedRecipeBuilder conflicts(Fuzzy ...conflicts) {
            this.conflicts.addAll(Arrays.asList(conflicts));
            return this;
        }

        private void register(RegisterCraftingRecipeEvent event) {
            event.register(target, width, ingredients, dependencies, conflicts);
        }
    }
}