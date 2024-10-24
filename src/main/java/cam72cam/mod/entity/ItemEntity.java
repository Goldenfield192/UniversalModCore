package cam72cam.mod.entity;

import cam72cam.mod.item.ItemStack;

import java.util.UUID;

/**
 * Wrapper around ItemEntity
 */
public class ItemEntity extends Entity {
    public net.minecraft.entity.item.ItemEntity internal;

    public ItemEntity(net.minecraft.entity.item.ItemEntity entity) {
        super(entity);
        this.internal = entity;
    }

    public ItemStack getContent() {
        return new ItemStack(internal.getItem());
    }

    public UUID getOwner() {
        return internal.getOwnerId();
    }

    public UUID getThrower() {
        return internal.getThrowerId();
    }

    public void setDefaultPickupDelay() {
        internal.setDefaultPickupDelay();
    }

    public void setNoPickupDelay() {
        internal.setNoPickupDelay();
    }

    public void setInfinitePickupDelay() {
        internal.setInfinitePickupDelay();
    }

    public void setPickupDelay(int ticks) {
        internal.setPickupDelay(ticks);
    }

    public void setNoDespawn() {
        internal.setNoDespawn();
    }
}