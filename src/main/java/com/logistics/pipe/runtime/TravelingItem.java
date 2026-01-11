package com.logistics.pipe.runtime;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.Direction;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Represents an item traveling through the pipe network.
 * Items move along pipe edges from one connection point to another.
 */
public class TravelingItem {
    /**
     * Codec used for saving/loading TravelingItem via ReadView/WriteView (1.21.8+).
     */
    public static final Codec<TravelingItem> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        ItemStack.CODEC.fieldOf("item").forGetter(t -> t.stack),
        Codec.INT.fieldOf("direction").xmap(Direction::byIndex, Direction::getIndex).forGetter(t -> t.direction),
        Codec.FLOAT.optionalFieldOf("speed", PipeConfig.BASE_PIPE_SPEED).forGetter(t -> t.speed),
        Codec.FLOAT.optionalFieldOf("progress", 0.0f).forGetter(t -> t.progress),
        Codec.BOOL.optionalFieldOf("routed", false).forGetter(t -> t.routed)
    ).apply(instance, TravelingItem::fromCodec));

    private static TravelingItem fromCodec(ItemStack stack, Direction direction, float speed, float progress, boolean routed) {
        TravelingItem item = new TravelingItem(stack, direction, speed);
        item.progress = progress;
        item.routed = routed;
        return item;
    }

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
}
