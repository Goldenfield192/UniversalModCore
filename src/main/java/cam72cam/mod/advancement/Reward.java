package cam72cam.mod.advancement;

import cam72cam.mod.entity.Player;
import cam72cam.mod.world.World;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.command.FunctionObject;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ResourceLocation;

import java.util.function.Consumer;

public class Reward {
    private final Consumer<Player> handler;

    private Reward(Consumer<Player> handler) {
        this.handler = handler;
    }

    public static Reward addExperience(int num) {
        return new Reward(player -> player.internal.addExperience(num));
    }

    public static Reward custom(Consumer<Player> consumer) {
        return new Reward(consumer);
    }

    public AdvancementRewards internal() {
        return new Internal();
    }

    class Internal extends AdvancementRewards {
        public Internal() {
            super(0, new ResourceLocation[0], new ResourceLocation[0], FunctionObject.CacheableFunction.EMPTY);
        }

        @Override
        public void apply(EntityPlayerMP player) {
            handler.accept((Player) World.get(player.world).getEntity(player));
        }
    }
}
