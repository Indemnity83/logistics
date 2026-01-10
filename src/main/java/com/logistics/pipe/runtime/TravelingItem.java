package com.logistics.pipe.runtime;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.Direction;

/**
 * Represents an item traveling through the pipe network.
 * Items move along pipe edges from one connection point to another.
 */
public class TravelingItem {
    private ItemStack stack;
    private float progress; // 0.0 = entering pipe, 1.0 = leaving pipe
    private Direction direction; // Direction of travel through current pipe
    private float speed; // Blocks per tick (varies by pipe material)
    private boolean routed; // True once the item has been routed at the center

    public TravelingItem(ItemStack stack, Direction direction, float speed) {
        this.stack = stack.copy();
        this.progress = 0.0f;
        this.direction = direction;
        this.speed = speed;
        this.routed = false;
    }

    /**
     * Update the item's position along the pipe.
     * Gradually adjusts speed toward the target speed.
     * @param targetSpeed The pipe's target speed
     * @param accelerationRate How quickly to adjust speed
     * @param canAccelerate Whether the pipe can accelerate items (or only decelerate)
     * @return true if item reached the end of this pipe segment
     */
    public boolean tick(float targetSpeed, float accelerationRate, boolean canAccelerate) {
        // Adjust speed toward target
        if (speed < targetSpeed) {
            // Only accelerate if allowed
            if (canAccelerate) {
                speed = Math.min(speed + accelerationRate, targetSpeed);
            }
            // Otherwise maintain current speed (no acceleration)
        } else if (speed > targetSpeed) {
            // Always decelerate (drag)
            speed = Math.max(speed - accelerationRate, targetSpeed);
        }

        progress += speed;
        return progress >= 1.0f;
    }

    /**
     * Get the current position along the pipe (0.0 to 1.0)
     */
    public float getProgress() {
        return progress;
    }

    /**
     * Get the direction this item is traveling
     */
    public Direction getDirection() {
        return direction;
    }

    /**
     * Update direction without resetting progress (used for mid-pipe direction changes).
     */
    public void setDirection(Direction direction) {
        this.direction = direction;
        this.routed = true;
    }

    /**
     * Get the ItemStack being transported
     */
    public ItemStack getStack() {
        return stack;
    }

    /**
     * Get the movement speed (blocks per tick)
     */
    public float getSpeed() {
        return speed;
    }

    /**
     * Set the movement speed (when entering different pipe types)
     */
    public void setSpeed(float speed) {
        this.speed = speed;
    }

    /**
     * Set the current progress along the pipe (0.0 to 1.0).
     */
    public void setProgress(float progress) {
        this.progress = progress;
    }

    public boolean isRouted() {
        return routed;
    }

    public void setRouted(boolean routed) {
        this.routed = routed;
    }

    /**
     * Save to NBT
     */
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        nbt.put("Item", stack.toNbt(registryLookup));
        nbt.putFloat("Progress", progress);
        nbt.putInt("Direction", direction.getId());
        nbt.putFloat("Speed", speed);
        nbt.putBoolean("Routed", routed);
        return nbt;
    }

    /**
     * Load from NBT
     */
    public static TravelingItem fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        ItemStack stack = ItemStack.fromNbt(registryLookup, nbt.getCompound("Item")).orElse(ItemStack.EMPTY);
        Direction direction = Direction.byId(nbt.getInt("Direction"));
        float speed = nbt.getFloat("Speed");

        TravelingItem item = new TravelingItem(stack, direction, speed);
        item.progress = nbt.getFloat("Progress");
        if (nbt.contains("Routed", 99)) {
            item.routed = nbt.getBoolean("Routed");
        }
        return item;
    }
}
