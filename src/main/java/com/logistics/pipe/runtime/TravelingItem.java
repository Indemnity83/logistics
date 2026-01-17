package com.logistics.pipe.runtime;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Direction;

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
                    Codec.INT
                            .fieldOf("direction")
                            .xmap(Direction::byIndex, Direction::getIndex)
                            .forGetter(t -> t.direction),
                    Codec.FLOAT
                            .optionalFieldOf("speed", PipeConfig.ITEM_MIN_SPEED)
                            .forGetter(t -> t.speed),
                    Codec.FLOAT.optionalFieldOf("progress", 0.0f).forGetter(t -> t.progress),
                    Codec.BOOL.optionalFieldOf("routed", false).forGetter(t -> t.routed))
            .apply(instance, TravelingItem::fromCodec));

    private static TravelingItem fromCodec(
            ItemStack stack, Direction direction, float speed, float progress, boolean routed) {
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
     * Applies acceleration or drag, and if above max speed, decelerates to hit max at the segment exit.
     * @param accelerationRate How quickly to adjust speed (positive or negative)
     * @param dragCoefficient Fraction of speed lost per tick when not accelerating
     * @param maxSpeed Maximum allowed speed
     * @return true if item reached the end of this pipe segment
     */
    public boolean tick(float accelerationRate, float dragCoefficient, float maxSpeed) {
        boolean deceleratingToMax = speed > maxSpeed;
        if (deceleratingToMax) {
            float remaining = Math.max(1.0e-4f, 1.0f - progress);
            float targetSquared = maxSpeed * maxSpeed;
            float currentSquared = speed * speed;
            float decel = (targetSquared - currentSquared) / (2.0f * remaining);
            speed += decel;
            if (speed < maxSpeed) {
                speed = maxSpeed;
            }
        } else if (accelerationRate != 0f) {
            speed += accelerationRate;
        } else if (dragCoefficient != 0f) {
            speed -= speed * dragCoefficient;
        }

        if (speed < PipeConfig.ITEM_MIN_SPEED) {
            speed = PipeConfig.ITEM_MIN_SPEED;
        } else if (!deceleratingToMax && speed > maxSpeed) {
            speed = maxSpeed;
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
