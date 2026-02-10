package cam72cam.mod.event.platform;

import cam72cam.mod.item.Fuzzy;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.event.IModBusEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RegisterAdvancementEvent extends Event implements IModBusEvent {
    private static final ResourceLocation RECIPE = new ResourceLocation("minecraft:recipes/root");
    private final Map<ResourceLocation, Advancement.Builder> map;

    public RegisterAdvancementEvent(Map<ResourceLocation, Advancement.Builder> map) {
        this.map = map;
    }

    public void registerRecipeTrigger(ResourceLocation ident, ResourceLocation recipe, Fuzzy... trigger) {
        Advancement.Builder builder = Advancement.Builder.recipeAdvancement();

        List<String> stringList = new ArrayList<>();
        for (int i = 0; i < trigger.length; i++) {
            Fuzzy ingredient = trigger[i];
            if (ingredient == null || ingredient.getTag() == null) continue;

            Criterion hasItem = new Criterion(InventoryChangeTrigger.TriggerInstance.hasItems(
                    ItemPredicate.Builder.item().of(ingredient.getTag()).build()));
            String e = "has" + ingredient + i;
            builder.addCriterion(e, hasItem);
            stringList.add(e);
        }
        builder.requirements(new String[][]{stringList.toArray(new String[0])});
        builder.rewards(AdvancementRewards.Builder.recipe(recipe));
        builder.parent(RECIPE);

        map.put(ident, builder);
    }
}
