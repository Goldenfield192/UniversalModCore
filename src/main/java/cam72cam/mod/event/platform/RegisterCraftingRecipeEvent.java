package cam72cam.mod.event.platform;

import cam72cam.mod.ModCore;
import cam72cam.mod.event.CommonEvents;
import cam72cam.mod.item.Fuzzy;
import cam72cam.mod.item.ItemStack;
import com.google.common.collect.ImmutableMap;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.*;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.event.IModBusEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

//Only support full height recipe for now
public class RegisterCraftingRecipeEvent extends Event implements IModBusEvent {
    Map<RecipeType<?>, ImmutableMap.Builder<ResourceLocation, Recipe<?>>> map;
    ImmutableMap.Builder<ResourceLocation, Recipe<?>> builder;

    public RegisterCraftingRecipeEvent(Map<RecipeType<?>, ImmutableMap.Builder<ResourceLocation, Recipe<?>>> map, ImmutableMap.Builder<ResourceLocation, Recipe<?>> builder) {
        this.map = map;
        this.builder = builder;
    }

    public void register(ItemStack target, int width, List<Fuzzy> ingredients, List<Fuzzy> dependencies, List<Fuzzy> conflicts) {
        ResourceLocation itemName = ForgeRegistries.ITEMS.getKey(target.internal().getItem());
        ResourceLocation name = ResourceLocation.fromNamespaceAndPath(itemName.getNamespace(),
                                                                      itemName.getPath() + "w" + ingredients.hashCode() + "w" + dependencies.hashCode() + "w" + conflicts.hashCode());

        boolean dependencyNotMet = dependencies.stream().anyMatch(f -> {
            Set<String> strings = Fuzzy.lookup.get(f.getTag().location());
            return (!f.isEmpty() && f.enumerate().stream().noneMatch(item ->
                                                            ForgeRegistries.ITEMS.containsKey(ForgeRegistries.ITEMS.getKey(item.internal().getItem()))))
                    || strings == null || strings.isEmpty();
        });
        boolean hasConflict = conflicts.stream().anyMatch(f -> {
            Set<String> strings = Fuzzy.lookup.get(f.getTag().location());
            return (!f.isEmpty() && f.enumerate().stream().anyMatch(item ->
                                                           ForgeRegistries.ITEMS.containsKey(ForgeRegistries.ITEMS.getKey(item.internal().getItem()))))
                    || (strings != null && !strings.isEmpty());
        });

        if (dependencyNotMet || hasConflict) {
            ModCore.info("Requirements not met, skipping UMC recipe %s", name.toString());
            return;
        }

        List<Ingredient> n = new ArrayList<>();
        for (Fuzzy ingredient : ingredients) {
            if ((ingredient == null || Fuzzy.lookup.get(ingredient.getTag().location()) == null) || (ingredient.isEmpty() && Fuzzy.lookup.get(ingredient.getTag().location()).isEmpty())) {
                n.add(Ingredient.EMPTY);
            } else {
                n.add(new Ingredient(Stream.of(new Ingredient.TagValue(ingredient.getTag()))));
            }
        }
        NonNullList<Ingredient> ingredient = NonNullList.create();
        ingredient.addAll(n);

        ShapedRecipe recipe = new ShapedRecipe(name, "", CraftingBookCategory.MISC, width, 3, ingredient, target.internal());

        CommonEvents.Recipe.RECIPE_LISTENER.get().add(event -> {
            ResourceLocation ad = ResourceLocation.fromNamespaceAndPath(name.getNamespace(), "unlock" + name.getPath());
            event.registerRecipeTrigger(ad, name, ingredients.toArray(new Fuzzy[0]));
        });
        map.getOrDefault(RecipeType.CRAFTING, ImmutableMap.builder()).put(name, recipe);
        builder.put(name, recipe);
    }
}
