package cam72cam.mod.advancement;

import cam72cam.mod.item.Fuzzy;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.resource.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Advancement {
    public static List<Builder> builders = new ArrayList<>();

    public static class Builder {
        public Identifier ident;
        public boolean visible;
        public ItemStack logo;
        public AdvancementGroup group;
        public String translation;
        public String description;
        public Trigger trigger;
        public Reward reward;
        public Advancement parent;

        public static Builder of(Identifier identifier) {
            Builder builder = new Builder();
            builder.ident = identifier;
            return builder;
        }

        public Builder icon(Fuzzy fuzzy) {
            if (fuzzy.isEmpty()) {
                this.visible = false;
            } else {
                this.visible = true;
                this.logo = fuzzy.example();
            }
            return this;
        }

        public Builder name(String translationKey) {
            this.translation = translationKey;
            return this;
        }

        public Builder description(String des) {
            this.description = des;
            return this;
        }

        public Builder reward(Reward reward) {
            this.reward = reward;
            return this;
        }

        public Builder group(AdvancementGroup group) {
            this.group = group;
            return this;
        }

        public Builder trigger(Trigger trigger) {
            this.trigger = trigger;
            return this;
        }

        public Builder parent(Advancement parent) {
            this.parent = parent;
            return this;
        }

        public void build() {
            //TODO
            builders.add(this);
        }
    }
}
