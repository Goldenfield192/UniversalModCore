package cam72cam.mod.advancement;

import cam72cam.mod.entity.Player;
import cam72cam.mod.item.Fuzzy;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.world.World;
import net.minecraft.advancements.Criterion;

import java.util.function.Function;

public class Trigger {
    private Function<Context, Boolean> tester;

    private Trigger(Function<Context, Boolean> tester) {
        this.tester = tester;
    }

    public static Trigger hasItem(Fuzzy... item) {
        return new Trigger(context -> {
            for (int i = 0; i < context.player.getInventory().getSlotCount(); i++) {
                ItemStack stack = context.player.getInventory().get(i);
                for (Fuzzy test : item) {
                    if (stack.is(test)) {
                        return true;
                    }
                }
            }
            return false;
        });
    }

    public Trigger and(Trigger another) {
        return new Trigger(context -> this.tester.apply(context) && another.tester.apply(context));
    }

    public Trigger or(Trigger another) {
        return new Trigger(context -> this.tester.apply(context) || another.tester.apply(context));
    }

    class CustomCriteria extends Criterion {

    }

    public static class Context {
        public final Player player;
        public final World world;

        public Context(Player player, World world) {
            this.player = player;
            this.world = world;
        }
    }
}
